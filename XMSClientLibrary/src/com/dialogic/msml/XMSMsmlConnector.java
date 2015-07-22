/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dialogic.msml;

import com.dialogic.XMSClientLibrary.RESTOPERATION;
import com.dialogic.XMSClientLibrary.SendCommandResponse;
import com.dialogic.XMSClientLibrary.XMSConnector;
import com.dialogic.XMSClientLibrary.XMSObject;
import com.dialogic.XMSClientLibrary.XMSReturnCode;
import gov.nist.javax.sip.header.CSeq;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TooManyListenersException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.InvalidArgumentException;
import javax.sip.SipFactory;
import javax.sip.SipStack;
import javax.sip.PeerUnavailableException;
import javax.sip.SipProvider;
import javax.sip.ListeningPoint;
import javax.sip.ObjectInUseException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.address.AddressFactory;
import javax.sip.SipListener;
import javax.sip.Timeout;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionAlreadyExistsException;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.TransactionUnavailableException;
import javax.sip.TransportNotSupportedException;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Connector that creates a sip stack to handle requests/responses. Implements
 * the SipListener interface to interact with the SIP stack
 *
 * @author ssatyana
 */
public class XMSMsmlConnector extends XMSConnector implements SipListener, Runnable {

    static final Logger logger = LogManager.getLogger(XMSMsmlConnector.class.getName());

    // Objects used to communicate to the JAIN SIP API.
    SipFactory sipFactory;          // Used to access the SIP API.
    static SipStack sipStack;              // The SIP stack.
    static SipProvider sipProvider;        // Used to send SIP messages.
    MessageFactory messageFactory;  // Used to create SIP message factory.
    HeaderFactory headerFactory;    // Used to create SIP headers.
    AddressFactory addressFactory;  // Used to create SIP address factory.
    ListeningPoint listeningPoint;  // SIP listening IP address/port.
    Properties properties;
    static SipListener sipListener;
    //Objects keeping local configuration.
    String protocol = "udp";        // The local protocol (UDP).
    static private Map<String, XMSSipCall> callMap = new HashMap<>();
    ClientTransaction clientTransaction = null;
    public static String responseMessage;
    static private List<XMSSipCall> waitCallList = new ArrayList();
    static private Map<String, XMSSipCall> activeCallMap = new HashMap<>();
    private final Object m_synclock = new Object();
    ExecutorService executor = Executors.newFixedThreadPool(25);
    private Response response;

    /**
     * Creates the sip stack, sip provider and factories for
     * address,header,messages.
     *
     * @param myIpAddress
     * @param myPort
     */
    public XMSMsmlConnector(String filename, String myIpAddress, int myPort) {
        m_type = "MSML";
        m_ConfigFileName = filename;
        if (sipFactory == null) {
            sipFactory = SipFactory.getInstance();
            sipFactory.setPathName("gov.nist"); // denotes the SIP stack

            properties = new Properties();
            properties.setProperty("javax.sip.STACK_NAME", "SipCall");
            properties.setProperty("javax.sip.IP_ADDRESS", myIpAddress);
            //properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "32");

            //properties.setProperty("gov.nist.javax.sip.SERVER_LOG", "SipCall.txt");
            //properties.setProperty("gov.nist.javax.sip.DEBUG_LOG", "SipCall.log");
        }
        try {
            if (sipStack == null) {
                sipStack = sipFactory.createSipStack(properties);
                logger.info("SipStack Created {}", sipStack);
            }

            // Sip provider with listening point
            ListeningPoint lp = sipStack.createListeningPoint(myIpAddress, myPort, "udp");
            // TODO if multiple providers then have a flag and modify code, make stack/provider non-static.
            if (sipProvider == null) {
                sipProvider = sipStack.createSipProvider(lp);
            }
            logger.info("SipProvider Created {}", sipProvider);

            int counter = 0;
            Iterator it = sipStack.getSipProviders();
            while (it.hasNext()) {
                Object e = it.next();
                counter++;
            }

            if (counter == 1 && sipListener == null) {
                sipListener = this;
            }

            sipProvider.addSipListener(sipListener);
            this.headerFactory = sipFactory.createHeaderFactory();
            this.messageFactory = sipFactory.createMessageFactory();
            this.addressFactory = sipFactory.createAddressFactory();
        } catch (PeerUnavailableException | TransportNotSupportedException | InvalidArgumentException | ObjectInUseException | TooManyListenersException ex) {
            logger.fatal(ex.getMessage(), ex);
        }
    }

    private String timeStamp() {
        return new SimpleDateFormat("[HH:mm:ss.SSS] ").format(Calendar.getInstance().getTime());
    }

    /**
     * Processes a Request received on a SipProvider upon which this SipListener
     * is registered.
     *
     * @param requestEvent
     */
    @Override
    public void processRequest(RequestEvent requestEvent) {
        Request request = requestEvent.getRequest();
        ServerTransaction serverTransaction = requestEvent.getServerTransaction();
        try {
            if (serverTransaction == null) {
                serverTransaction = sipProvider.getNewServerTransaction(request);
            }
        } catch (TransactionAlreadyExistsException | TransactionUnavailableException e) {
            logger.fatal(e.getMessage(), e);
        }
        XMSSipCall call;
        String CallId = ((CallIdHeader) request.getHeader(CallIdHeader.NAME)).getCallId();
        String contactAddress = "";
        if (request.getHeader(ContactHeader.NAME) != null) {
            contactAddress = ((ContactHeader) request.getHeader(ContactHeader.NAME))
                    .getAddress().toString();
        }
        switch (request.getMethod()) {
            case Request.INVITE:
//                logger.info("INVITE RECIEVED: " + contactAddress);
//                logger.debug("INVITE RECIEVED \n" + request);

                System.out.println(timeStamp() + "INVITE RECIEVED -> " + request);

                call = activeCallMap.get(CallId);
                if (call != null) {
                    logger.info("REINVITE RECIEVED: " + contactAddress);
                    call.setServerTransaction(serverTransaction);
                    call.handleStackRequest(requestEvent);
                } else {
                    if (waitCallList.size() > 0) {
                        XMSSipCall c1 = waitCallList.get(0);
                        if (!activeCallMap.containsValue(c1)) {
                            c1.setInviteRequest(request);
                            c1.setServerTransaction(serverTransaction);
                            activeCallMap.put(CallId, c1);
                            waitCallList.remove(c1);
                            executor.execute(new RequestProcessingThread(requestEvent, c1));
                            //c1.handleStackRequest(requestEvent);
                        }
                    } else {
                        // send 486, no call available
                        logger.info("NO CALLS IN THE WAITING LIST");
                    }
                }

                break;
            case Request.OPTIONS:
//                logger.info("OPTIONS RECIEVED");
//                logger.debug("OPTIONS RECIEVED \n" + request);

                System.out.println(timeStamp() + "OPTIONS RECIEVED -> " + request);
                if (waitCallList.size() > 0) {
                    XMSSipCall c1 = waitCallList.get(0);
                    c1.setServerTransaction(serverTransaction);
                    c1.handleStackRequest(requestEvent);
                }
                break;
            case Request.INFO:
//                logger.info("INFO RECIEVED: " + contactAddress);
//                logger.debug("INFO RECIEVED \n" + request);

                System.out.println(timeStamp() + "INFO RECIEVED -> " + request);
                call = activeCallMap.get(requestEvent.getDialog().getCallId().getCallId());
                call.setServerTransaction(serverTransaction);
                call.handleStackRequest(requestEvent);
                break;
            case Request.ACK:
//                logger.info("ACK RECIEVED: " + contactAddress);
//                logger.debug("ACK RECIEVED \n" + request);

                System.out.println(timeStamp() + "ACK RECIEVED -> " + request);
                call = activeCallMap.get(requestEvent.getDialog().getCallId().getCallId());
                call.setDialog(requestEvent.getDialog());
                call.setServerTransaction(serverTransaction);
                call.handleStackRequest(requestEvent);
                break;
            case Request.BYE:
//                logger.info("BYE RECIEVED: " + contactAddress);
//                logger.debug("BYE RECIEVED \n" + request);

                System.out.println(timeStamp() + "BYE RECIEVED -> " + request);
                call = activeCallMap.get(requestEvent.getDialog().getCallId().getCallId());
                if (call != null) {
                    call.setServerTransaction(serverTransaction);
                    call.setCallId(requestEvent.getDialog().getCallId().getCallId());
                    call.handleStackRequest(requestEvent);
                }
                break;
            case Request.CANCEL:
//                logger.info("CANCEL RECIEVED: " + contactAddress);
//                logger.debug("CANCEL RECIEVED \n" + request);

                System.out.println(timeStamp() + "CANCEL RECIEVED -> " + request);
                call = activeCallMap.get(requestEvent.getDialog().getCallId().getCallId());

                call.setServerTransaction(serverTransaction);
                call.handleStackRequest(requestEvent);
                break;
        }
//        executor.shutdown();
//        while (!executor.isTerminated()) {
//        }
//        System.out.println("Finished all the threads from the EXEC!!");
    }

    /**
     * Processes a Response received on a SipProvider upon which this
     * SipListener is registered.
     *
     * @param responseEvent
     */
    @Override
    public void processResponse(ResponseEvent responseEvent) {
        Response response = responseEvent.getResponse();
        CSeqHeader cSeq = (CSeqHeader) response.getHeader(CSeq.NAME);
        Dialog dialog = responseEvent.getDialog();
        XMSSipCall call = activeCallMap.get(dialog.getCallId().getCallId());
        call.setClientTransaction(responseEvent.getClientTransaction());
        String contactAddress = "";
        if (response.getHeader(ContactHeader.NAME) != null) {
            contactAddress = ((ContactHeader) response.getHeader(ContactHeader.NAME))
                    .getAddress().toString();
        }
        switch (response.getStatusCode()) {
            case Response.OK:
                switch (cSeq.getMethod()) {
                    case Request.INVITE:
//                        logger.info("RESPONSE 200OK FOR INVITE RECIEVED: " + contactAddress);
//                        logger.debug("RESPONSE 200OK FOR INVITE RECIEVED \n" + response);

                        System.out.println(timeStamp() + "RESPONSE 200OK FOR INVITE -> " + responseEvent.getResponse());
                        call.setDialog(dialog);
                        call = activeCallMap.get(dialog.getCallId().getCallId());
                        if (call != null) {
                            call.setDialog(dialog);
                            call.handleStackResponse(response, cSeq, dialog);
                        }
                        break;
                    case Request.OPTIONS:
                        break;
                    case Request.INFO:
//                        logger.info("RESPONSE 200OK FOR INFO RECIEVED: " + contactAddress);
//                        logger.debug("RESPONSE 200OK FOR INFO RECIEVED \n" + response);

                        System.out.println(timeStamp() + "RESPONSE 200OK FOR INFO -> " + responseEvent.getResponse());
                        call = activeCallMap.get(dialog.getCallId().getCallId());
                        if (call != null) {
                            call.handleStackResponse(response, cSeq, dialog);

                        }
                        break;
                    case Request.BYE:
//                        logger.info("RESPONSE 200OK FOR BYE RECIEVED: " + contactAddress);
//                        logger.debug("RESPONSE 200OK FOR BYE RECIEVED \n" + response);

                        System.out.println(timeStamp() + "RESPONSE 200OK FOR BYE -> " + responseEvent.getResponse());
                        call = activeCallMap.get(dialog.getCallId().getCallId());
                        if (call != null) {
                            call.handleStackResponse(response, cSeq, dialog);

                        }
                        activeCallMap.remove(dialog.getCallId().getCallId());
                        logger.info("HASHMAP SIZE: " + activeCallMap.size());
                        break;
                    case Request.CANCEL:
//                        logger.info("RESPONSE 200OK FOR CANCEL RECIEVED: " + contactAddress);
//                        logger.debug("RESPONSE 200OK FOR CANCEL RECIEVED \n" + response);

                        System.out.println(timeStamp() + "RESPONSE 200OK FOR CANCEL -> " + responseEvent.getResponse());
                        activeCallMap.remove(dialog.getCallId().getCallId());
                        logger.info("HASHMAP SIZE: " + activeCallMap.size());
                        break;
                }
                break;
            case Response.TRYING:
//                logger.info("RESPONSE 100 TRYING RECIEVED: " + contactAddress);
//                logger.debug("RESPONSE 100 TRYING RECIEVED \n" + response);

                System.out.println(timeStamp() + "RESPONSE 100 TRYING RECIEVED -> " + responseEvent.getResponse());
                call = activeCallMap.get(dialog.getCallId().getCallId());
                call.handleStackResponse(response, cSeq, dialog);
                break;
            case Response.RINGING:
//                logger.info("RESPONSE 180 RINGING RECIEVED: " + contactAddress);
//                logger.debug("RESPONSE 180 RINGING RECIEVED \n" + response);

                System.out.println(timeStamp() + "RESPONSE 180 RINGING RECIEVED -> " + responseEvent.getResponse());
                call = activeCallMap.get(dialog.getCallId().getCallId());
                if (call != null) {
                    call.handleStackResponse(response, cSeq, dialog);
                }
                break;
            case Response.BUSY_HERE:
                logger.info("RESPONSE BUSY HERE RECIEVED: " + contactAddress);
                logger.debug("RESPONSE BUSY HERE RECIEVED \n" + response);
                break;
            case Response.DECLINE:
//                logger.info("RESPONSE DECLINE RECIEVED: " + contactAddress);
//                logger.debug("RESPONSE DECLINE RECIEVED \n" + response);
                System.out.println("RESPONSE DECLINE RECIEVED -> " + response);
                call = activeCallMap.get(dialog.getCallId().getCallId());
                if (call != null) {
                    call.handleStackResponse(response, cSeq, dialog);
                }
                activeCallMap.remove(dialog.getCallId().getCallId());
                logger.info("HASHMAP SIZE: " + activeCallMap.size());
                break;
            case Response.REQUEST_TERMINATED:
                logger.info("RESPONSE REQUEST TERMINATED RECIEVED: " + contactAddress);
                logger.debug("RESPONSE REQUEST TERMINATED RECIEVED \n" + response);
                call.handleStackResponse(response, cSeq, dialog);
                break;
        }
    }

    /**
     * Registers the created call. Maintains a map to track all the calls.
     *
     * @param call
     */
    public void register(XMSSipCall call) {
        if (call != null) {
            callMap.put(call.getCallId(), call);
            System.out.println("Registered: Contents of the call map" + callMap);
        }
    }

    /**
     * Methods used to send the requests. Ex: INVITE,INFO,BYE,CANCEL, etc.
     *
     * @param request
     * @param call
     */
    public void sendRequest(Request request, XMSSipCall call) {
//        logger.info("SEND " + request.getMethod() + " REQUEST");
//        logger.debug("SEND " + request.getMethod() + " REQUEST -> " + request);

        System.out.println("SEND " + request.getMethod() + " REQUEST -> " + request);
        Map<Request, XMSSipCall> queueMap = new HashMap<>();
        try {
            Dialog dialog = call.getDialog();
            clientTransaction = sipProvider.getNewClientTransaction(request);
            call.setClientTransaction(clientTransaction);
            if (dialog != null) {
                dialog.sendRequest(clientTransaction);
            } else {
                clientTransaction.sendRequest();
            }
        } catch (Exception ex) {
            if (ex.getMessage().equalsIgnoreCase("Transaction already exists!")) {
                queueMap.put(request, call);
                try {
                    synchronized (m_synclock) {
                        logger.debug("WAIT FOR TRANSACTION TO COMPLETE");
                        m_synclock.wait(500);
                        logger.debug("WAIT OVER");
                        if (queueMap.size() > 0) {
                            Iterator it = queueMap.entrySet().iterator();
                            while (it.hasNext()) {
                                Map.Entry item = (Map.Entry) it.next();
                                sendRequest((Request) item.getKey(), (XMSSipCall) item.getValue());
                                it.remove();
                            }
                        }
                    }
                } catch (InterruptedException ex1) {
                    logger.fatal(ex1.getMessage(), ex1);
                }
            }
        }
    }

    /**
     * Sends an ACK request.
     *
     * @param ackRequest
     * @param dialog
     */
    public void sendAck(Request ackRequest, Dialog dialog) {
        XMSSipCall call = activeCallMap.get(dialog.getCallId().getCallId());
        try {
            dialog.sendAck(ackRequest);
//            logger.info("SEND ACK REQUEST");
//            logger.debug("SEND ACK REQUEST \n" + ackRequest);

            System.out.println("SEND ACK REQUEST -> " + ackRequest);
        } catch (SipException ex) {
            logger.fatal(ex.getMessage(), ex);
        }
        call.setRemoteTag(call.getDialog().getRemoteTag());
        logger.debug("REMOTE TAG \n" + call.getDialog().getRemoteTag());
    }

    /**
     * Method used to send responses. Ex: 200OK,TRYING,RINGING, etc.
     *
     * @param response
     * @param call
     */
    public void sendResponse(Response response, XMSSipCall call) {
        ServerTransaction st = call.getServerTransaction();
        CSeqHeader cSeq = (CSeqHeader) response.getHeader(CSeq.NAME);
        if (st != null) {
            try {
                switch (response.getStatusCode()) {
                    case Response.OK:
                        switch (cSeq.getMethod()) {
                            case Request.BYE:
//                                logger.info("SEND OK FOR BYE");
//                                logger.debug("SEND 200 OK for BYE REQUEST \n" + response);

                                System.out.println(timeStamp() + "Received Bye, sending OK");
                                st.sendResponse(response);
                                activeCallMap.remove(call.getCallId());
                                break;
                            case Request.INFO:
//                                logger.info("SEND OK FOR INFO");
//                                logger.debug("SEND 200 OK for INFO REQUEST \n" + response);

                                System.out.println(timeStamp() + "Received Info, sending OK");
                                setLastResponse(response);
                                st.sendResponse(response);
                                break;
                            case Request.CANCEL:
                                logger.info("SEND OK FOR CANCEL");
                                logger.debug("SEND 200 OK for CANCEL REQUEST \n" + response);
                                st.sendResponse(response);
                                break;
                            case Request.INVITE:
//                                logger.info("SEND OK FOR INVITE");
//                                logger.debug("SEND 200 OK for INVITE REQUEST \n" + response);

                                System.out.println("200 OK for INVITE REQUEST -> " + response);
                                setLastResponse(response);
                                st.sendResponse(response);
                                break;
                            case Request.OPTIONS:
//                                logger.info("SEND OK FOR OPTIONS");
//                                logger.debug("SEND 200 OK for OPTIONS REQUEST \n" + response);

                                System.out.println("200 OK for OPTIONS REQUEST -> " + response);
                                st.sendResponse(response);
                                break;
                        }
                        break;
                    case Response.TRYING:
//                        logger.info("SEND 100 TRYING");
//                        logger.debug("SEND 100 TRYING RESPONSE \n" + response);

                        System.out.println("SENT 100 TRYING RESPONSE -> " + response);
                        setLastResponse(response);
                        st.sendResponse(response);
                        break;
                    case Response.RINGING:
//                        logger.info("SEND 180 RINGING");
//                        logger.debug("SEND 180 RINGING RESPONSE \n" + response);

                        System.out.println("SENT 180 RINGING RESPONSE -> " + response);
                        setLastResponse(response);
                        st.sendResponse(response);
                        break;
                }
            } catch (SipException | InvalidArgumentException ex) {
                logger.fatal(ex.getMessage(), ex);
            }
        }
    }

    /**
     * Sends an ACK for request termination.
     *
     * @param ackRequest
     * @param dialog
     */
    public void sendTerminationAck(Request ackRequest, Dialog dialog) {
        try {
            dialog.sendAck(ackRequest);
            logger.info("TERMINATION ACK ");
            logger.debug("TERMINATION ACK  \n" + ackRequest);
        } catch (SipException ex) {
            logger.fatal(ex.getMessage(), ex);
        }
        activeCallMap.remove(dialog.getCallId().getCallId());
        logger.info("HASHMAP SIZE: " + activeCallMap.size());
    }

    /**
     * Processes a retransmit or expiration Timeout of an underlying Transaction
     * handled by this SipListener.
     * <p>
     * This Event notifies the application that a retransmission or transaction
     * Timer expired in the SipProvider's transaction state machine. The
     * TimeoutEvent encapsulates the specific timeout type and the transaction
     * identifier either client or server upon which the timeout occurred.
     * </p>
     *
     * @param te. The timeout event.
     */
    @Override
    public void processTimeout(TimeoutEvent te) {
        logger.info(te.getTimeout());
        try {
            if (te.getTimeout() == Timeout.TRANSACTION) {
                if (te.getServerTransaction() != null) {
                    te.getServerTransaction().sendResponse(getLastResponse());
                }
            }
        } catch (Exception ex) {
            logger.fatal(ex.getMessage(), ex);
        }
    }

    /**
     * Process an asynchronously reported IO Exception.
     * <p>
     * Asynchronous IO Exceptions may occur as a result of errors during
     * retransmission of requests. The transaction state machine requires to
     * report IO Exceptions to the application immediately (according to RFC
     * 3261). This method enables an implementation to propagate the
     * asynchronous handling of IO Exceptions to the application.
     * </p>
     *
     * @param ioee. The IOException event.
     */
    @Override
    public void processIOException(IOExceptionEvent ioee) {
        logger.info("IO Exception");
    }

    /**
     * Process an asynchronously reported TransactionTerminatedEvent.
     * <p>
     * When a transaction transitions to the Terminated state, the stack keeps
     * no further records of the transaction. This notification can be used by
     * applications to clean up any auxiliary data that is being maintained for
     * the given transaction.
     * </p>
     *
     * @param tte. The transaction terminated event.
     */
    @Override
    public void processTransactionTerminated(TransactionTerminatedEvent tte) {
        logger.info("Transaction Terminated");
    }

    /**
     * Process an asynchronously reported DialogTerminatedEvent.
     * <p>
     * When a dialog transitions to the Terminated state, the stack keeps no
     * further records of the dialog. This notification can be used by
     * applications to clean up any auxiliary data that is being maintained for
     * the given dialog. A dialog transitions to the "terminated" state when it
     * is completed and ready for garbage collection.
     * </p>
     *
     * @param dte. The dialog terminated event.
     */
    @Override
    public void processDialogTerminated(DialogTerminatedEvent dte) {
        logger.info("Dialog Terminated");
    }

    /**
     * Returns the address factory created using sipFactory.
     *
     * @return addressFactory.
     */
    public AddressFactory getAddressFactory() {
        return this.addressFactory;
    }

    /**
     * Returns the message factory created using sipFactory.
     *
     * @return messageFactory.
     */
    public MessageFactory getMessageFactory() {
        return this.messageFactory;
    }

    /**
     * Returns the header factory created using sipFactory.
     *
     * @return headerFactory.
     */
    public HeaderFactory getHeaderFactory() {
        return this.headerFactory;
    }

    public void addToWaitList(XMSSipCall call) {
        waitCallList.add(call);
        System.out.println("Calls waiting" + waitCallList);
    }

    public void addToActiveMap(String id, XMSSipCall call) {
        activeCallMap.put(id, call);
    }

    @Override
    public void run() {
        //do nothing
    }

    @Override
    public XMSReturnCode Initialize() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected SendCommandResponse SendCommand(XMSObject a_call, RESTOPERATION a_RESTOPERATION, String a_urlextension, String a_xmlPayload) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void setLastResponse(Response res) {
        response = res;
    }

    private Response getLastResponse() {
        return response;
    }

}
