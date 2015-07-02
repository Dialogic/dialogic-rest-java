/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dialogic.msml;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Random;
import java.util.regex.Pattern;
import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.AllowHeader;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.Header;
import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.SupportedHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The call class maintaining the call functionalities.
 *
 * @author ssatyana
 */
public class XMSSipCall extends Observable {

    static final Logger logger = LogManager.getLogger(XMSSipCall.class.getName());

    Dialog dialog;
    private ServerTransaction servertransaction;
    private ClientTransaction clienttransaction;
    private Request inviteRequest = null;
    public String localSdp;
    public String remoteSdp;
    public String remoteTag;
    String content;
    String callId;
    XMSMsmlConnector sipConnector;
    private String fromAddr;
    private String fromUser;
    private String toAddr;
    private String toUser;
    static XMSSipCall xmsCall;
    static boolean isACKOn200 = true;
    static boolean isOKOnInfo = true;
    static boolean isInvite = false;
    private Map<String, Object> headers = new HashMap<>();
    static private Map<String, FromHeader> fromHeadersMap = new HashMap<>();
    static private Map<String, ToHeader> toHeadersMap = new HashMap<>();
    static private Map<Integer, ViaHeader> viaHeadersMap = new HashMap<>();
    static private Map<Integer, ContactHeader> contactHeadersMap = new HashMap<>();
    static private Map<String, CSeqHeader> cSeqHeadersMap = new HashMap<>();
    static private Map<String, ContentTypeHeader> contentTypeHeaderMap = new HashMap<>();
    private List<XMSSipCall> pendingCallList = new ArrayList();

    public XMSSipCall(XMSMsmlConnector connector) {
        this.dialog = null;
        this.servertransaction = null;
        this.clienttransaction = null;
        this.localSdp = null;
        this.inviteRequest = null;
        this.content = null;
        this.callId = null;
        this.sipConnector = connector;
        this.fromUser = "Test";
    }

    /**
     * This method handles the requests for the call.
     *
     * @param requestEvent.
     */
    public void handleStackRequest(RequestEvent requestEvent) {
        Request request = requestEvent.getRequest();
        switch (request.getMethod()) {
            case Request.INVITE:
                if (isInvite && this.getRemoteSdp() != null) {
                    doReInviteOk(request);
                } else {
                    this.createTryingResponse(request);
                    if (request.getRawContent() != null) {
                        this.setRemoteSdp(new String(request.getRawContent()));
                    }
                    MsmlEvent invite = createRequestEvent(request, MsmlEventType.INCOMING);
                    setValue(invite);
                    isInvite = true;
                }
                break;
            case Request.OPTIONS:
                this.createOptionsResponse(request);
                break;
            case Request.INFO:
                if (isOKOnInfo) {
                    createInfoResponse(request);
                }
                MsmlEvent info = createRequestEvent(request, MsmlEventType.INFOREQUEST);
                setValue(info);
                break;
            case Request.ACK:
                MsmlEvent ack = createRequestEvent(request, MsmlEventType.CONNECTED);
                setValue(ack);
                break;
            case Request.CANCEL:
                MsmlEvent cancel = createRequestEvent(request, MsmlEventType.CANCEL);
                setValue(cancel);
                break;
            case Request.BYE:
                MsmlEvent bye = createRequestEvent(request, MsmlEventType.DISCONNECTED);
                setValue(bye);
                break;
        }
    }

    /**
     * This method handles all the responses for the call.
     *
     * @param response. The response received.
     * @param cSeq. The cSeq to get the type of method.
     * @param dialog. The dialog related to this response. incoming call(bridge
     * mode). This is required to handle the request/response on the App.
     */
    public void handleStackResponse(Response response, CSeqHeader cSeq, Dialog dialog) {
        switch (response.getStatusCode()) {
            case Response.OK:
                switch (cSeq.getMethod()) {
                    case Request.INVITE:
                        if (pendingCallList.contains(this)) {
                            System.out.println("repeated request");
                            // request repeated do not create an event
                        } else {
                            if (isACKOn200) {
                                createAckRequest(response);
                            }
                            pendingCallList.add(this);
                            this.setRemoteSdp(new String(response.getRawContent()));
                            MsmlEvent oK = createResponseEvent(response, MsmlEventType.CONNECTING);
                            setValue(oK);
                        }
                        break;
                    case Request.INFO:
                        MsmlEvent info = createResponseEvent(response, MsmlEventType.INFORESPONSE);
                        setValue(info);
                        break;
                    case Request.BYE:
                        MsmlEvent bye = createResponseEvent(response, MsmlEventType.DISCONNECTED);
                        setValue(bye);
                        break;
                }
                break;
            case Response.TRYING:
                break;
            case Response.RINGING:
                MsmlEvent eve = createResponseEvent(response, MsmlEventType.RINGING);
                setValue(eve);
                break;
            case Response.ACCEPTED:
                break;
            case Response.REQUEST_TERMINATED:
                break;
        }
    }

    public MsmlEvent createResponseEvent(Response response, MsmlEventType type) {
        MsmlEvent event = new MsmlEvent();
        event.setType(type);
        event.setRes(response);
        event.setCall(this);

        return event;

    }

    public MsmlEvent createRequestEvent(Request request, MsmlEventType type) {
        MsmlEvent event = new MsmlEvent();
        event.setType(type);
        event.setReq(request);
        event.setCall(this);

        return event;
    }

    /**
     * This method creates an invite request with necessary headers.
     *
     * @param toUserId
     * @param toAdr
     */
    public void createInviteRequest(String toUserId, String toAdr) {
        AddressFactory addressFactory = sipConnector.getAddressFactory();
        HeaderFactory headerFactory = sipConnector.getHeaderFactory();

        try {
            SipURI requestUri = addressFactory.createSipURI(toUserId, toAdr);
            FromHeader fromHeader;
            if (this.getFromHeader(this.getFromAddress()) != null) {
                fromHeader = this.getFromHeader(this.getFromAddress());
            } else {
                SipURI fromUri = addressFactory.createSipURI(this.getFromUser(), this.getFromAddress());
                Address fromAddress = addressFactory.createAddress(fromUri);
                fromHeader = headerFactory.createFromHeader(fromAddress, tagGenerator());
                fromHeadersMap.put(this.getFromAddress(), fromHeader);
            }
            ToHeader toHeader;
            if (this.getToHeader(toAdr) != null) {
                toHeader = this.getToHeader(toAdr);
            } else {
                SipURI toUri = addressFactory.createSipURI(toUserId, toAdr);
                Address toAddress = addressFactory.createAddress(toUri);
                toHeader = headerFactory.createToHeader(toAddress, null);
            }

            ArrayList<ViaHeader> viaHeaders = new ArrayList<>();
            ViaHeader viaHeader;
            if (this.getViaHeader(sipConnector.sipProvider.getListeningPoint("udp").getPort()) != null) {
                viaHeader = this.getViaHeader(sipConnector.sipProvider.getListeningPoint("udp").getPort());
            } else {
                viaHeader = headerFactory.createViaHeader(this.getFromAddress(),
                        sipConnector.sipProvider.getListeningPoint("udp").getPort(), "udp", null);
                viaHeadersMap.put(sipConnector.sipProvider.getListeningPoint("udp").getPort(), viaHeader);
            }
            viaHeaders.add(viaHeader);

            CallIdHeader callIdHeader = sipConnector.sipProvider.getNewCallId();
            this.setCallId(callIdHeader.getCallId());

            CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1L, Request.INVITE);
            headers.put("CSeq", cSeqHeader);

            ContentTypeHeader contTypeHeader = headerFactory.createContentTypeHeader("application", "sdp");
            MaxForwardsHeader maxForwardsHeader = headerFactory.createMaxForwardsHeader(70);

            Request request = null;
            if (this.getLocalSdp() != null) {
                request = sipConnector.getMessageFactory().createRequest(requestUri, Request.INVITE,
                        callIdHeader, cSeqHeader, fromHeader, toHeader, viaHeaders, maxForwardsHeader,
                        contTypeHeader, this.getLocalSdp().getBytes());
            } else {
                this.setACKOn200(Boolean.FALSE);
                request = sipConnector.getMessageFactory().createRequest(requestUri, Request.INVITE,
                        callIdHeader, cSeqHeader, fromHeader, toHeader, viaHeaders, maxForwardsHeader);
            }

//            Header supportedHeader = headerFactory.createHeader("Supported", "timer");
//            Header requireHeader = headerFactory.createHeader("Require", "timer");
//            Header sessionExpires = headerFactory.createHeader("Session-Expires", "90;refresher=uas");
//            Header minSE = headerFactory.createHeader("Min-SE", "90");
//            request.addHeader(supportedHeader);
//            request.addHeader(requireHeader);
//            request.addHeader(sessionExpires);
//            request.addHeader(minSE);
            ContactHeader contactHeader;
            if (this.getContactHeader(sipConnector.sipProvider.getListeningPoint("udp").getPort()) != null) {
                contactHeader = this.getContactHeader(sipConnector.sipProvider.getListeningPoint("udp").getPort());
            } else {
                SipURI contactUri = addressFactory.createSipURI(this.getFromUser(), this.getFromAddress());
                contactUri.setPort(sipConnector.sipProvider.getListeningPoint("udp").getPort());
                Address contactAddress = addressFactory.createAddress(contactUri);
                contactHeader = headerFactory.createContactHeader(contactAddress);
                contactHeadersMap.put(sipConnector.sipProvider.getListeningPoint("udp").getPort(), contactHeader);
            }
            request.addHeader(contactHeader);

            sipConnector.register(this);
            sipConnector.addToActiveMap(this.getCallId(), this);
            logger.info("CREATING AN INVITE REQUEST");
            sipConnector.sendRequest(request, this);
        } catch (Exception ex) {
            logger.fatal(ex.getMessage(), ex);
        }
    }

    /**
     * Creates an ACK request.
     *
     * @param response
     */
    public void createAckRequest(Response response) {
        logger.info("CREATING ACK REQUEST ");
        Request ackRequest;
        try {
            if (this.getDialog() != null) {
                ackRequest = this.getDialog().createAck(((CSeqHeader) response.getHeader(CSeqHeader.NAME)).getSeqNumber());
                if (!isACKOn200 && this.getLocalSdp() == null) {
                    HeaderFactory headerFactory = sipConnector.getHeaderFactory();
                    ContentTypeHeader contTypeHeader = headerFactory.createContentTypeHeader("application", "sdp");
                    ackRequest.setContent(response.getRawContent(), contTypeHeader);
                }
                sipConnector.sendAck(ackRequest, this.getDialog());
                MsmlEvent eve = createResponseEvent(response, MsmlEventType.CONNECTED);
                setValue(eve);
            }
        } catch (InvalidArgumentException | SipException | ParseException ex) {
            logger.fatal(ex.getMessage(), ex);
        }
    }

    /**
     * Creates an INFO request.
     *
     * @param msml
     */
    private void createInfoRequest(String msml) {
        HeaderFactory headerFactory = sipConnector.getHeaderFactory();
        try {
            Request infoRequest = this.getDialog().createRequest(Request.INFO);
            ContentTypeHeader contTypeHeader = headerFactory.createContentTypeHeader("application", "xml");
            infoRequest.addHeader(contTypeHeader);

            logger.info("REMOTE TAG -> " + this.getDialog().getRemoteTag());
            msml = msml.replaceAll("conn:.*?\\\"", "conn:" + this.getDialog().getRemoteTag() + "\"");
            infoRequest.setContent(msml, contTypeHeader);
            logger.info("CREATING INFO REQUEST TO XMS");
            sipConnector.sendRequest(infoRequest, this);

        } catch (SipException | ParseException ex) {
            logger.fatal(ex.getMessage(), ex);
        }
    }

    private void createInfoRequestWithoutConn(String msml) {
        HeaderFactory headerFactory = sipConnector.getHeaderFactory();
        try {
            Request infoRequest = this.getDialog().createRequest(Request.INFO);
            ContentTypeHeader contTypeHeader = headerFactory.createContentTypeHeader("application", "xml");
            infoRequest.addHeader(contTypeHeader);

            infoRequest.setContent(msml, contTypeHeader);
            sipConnector.sendRequest(infoRequest, this);

        } catch (SipException | ParseException ex) {
            logger.fatal(ex.getMessage(), ex);
        }
    }

    public void createDialogEndRequest(String msml) {
        HeaderFactory headerFactory = sipConnector.getHeaderFactory();
        try {
            Request infoRequest = this.getDialog().createRequest(Request.INFO);
            ContentTypeHeader contTypeHeader = headerFactory.createContentTypeHeader("application", "xml");
            infoRequest.addHeader(contTypeHeader);

            logger.info("REMOTE TAG -> " + this.getDialog().getRemoteTag());
            msml = msml.replaceAll("conn:.*?\\/", "conn:" + this.getDialog().getRemoteTag() + "\\/");
            infoRequest.setContent(msml, contTypeHeader);
            logger.info("CREATING INFO REQUEST TO XMS");
            sipConnector.sendRequest(infoRequest, this);
        } catch (SipException | ParseException ex) {
            logger.fatal(ex.getMessage(), ex);
        }
    }

    /**
     * Creates an INFO response.
     *
     * @param request
     */
    public void createInfoResponse(Request request) {
        MessageFactory messageFactory = sipConnector.getMessageFactory();
        HeaderFactory headerFactory = sipConnector.getHeaderFactory();
        AddressFactory addressFactory = sipConnector.getAddressFactory();
        try {
            Response infoOkResponse = messageFactory.createResponse(Response.OK, request);
            SipURI contactUri = addressFactory.createSipURI(this.getFromUser(), this.getFromAddress());
            contactUri.setPort(sipConnector.sipProvider.getListeningPoint("udp").getPort());
            Address contactAddress = addressFactory.createAddress(contactUri);
            ContactHeader contactHeader = headerFactory.createContactHeader(contactAddress);
            infoOkResponse.addHeader(contactHeader);
            logger.info("CREATING INFO RESPONSE TO XMS");
            sipConnector.sendResponse(infoOkResponse, this);
        } catch (Exception ex) {
            logger.fatal(ex.getMessage(), ex);
        }
    }

    /**
     * Creates an OPTIONS response.
     *
     * @param request
     */
    public void createOptionsResponse(Request request) {
        logger.info("CREATING OPTIONS RESPONSE");
        try {
            MessageFactory messageFactory = sipConnector.getMessageFactory();
            Response optionsResponse = messageFactory.createResponse(Response.OK, request);
            ToHeader toHeader = (ToHeader) optionsResponse.getHeader(ToHeader.NAME);
            toHeader.setTag(Integer.toHexString(new Random().nextInt(0xffffff) + 0xffffff));
            sipConnector.sendResponse(optionsResponse, this);
            //this.setSdp(new String(request.getRawContent()));
        } catch (Exception ex) {
            logger.fatal(ex.getMessage(), ex);
        }

    }

    /**
     * Creates a TRYING response.
     *
     * @param request
     */
    public void createTryingResponse(Request request) {
        logger.info("CREATING 100 TRYING RESPONSE");
        try {
            MessageFactory messageFactory = sipConnector.getMessageFactory();
            Response tryingResponse = messageFactory.createResponse(Response.TRYING, request);
            ToHeader toHeader = (ToHeader) tryingResponse.getHeader(ToHeader.NAME);
            toHeader.setTag(Integer.toHexString(new Random().nextInt(0xffffff) + 0xffffff));
            sipConnector.sendResponse(tryingResponse, this);
        } catch (Exception ex) {
            logger.fatal(ex.getMessage(), ex);
        }
    }

    /**
     * Creates a RINGING response.
     *
     * @param request
     */
    public void createRingingResponse(Request request) {
        logger.info("CREATING 180 RINGING RESPONSE");
        try {
            MessageFactory messageFactory = sipConnector.getMessageFactory();
            Response ringingResponse = messageFactory.createResponse(Response.RINGING, request);
            ToHeader toHeader = (ToHeader) ringingResponse.getHeader(ToHeader.NAME);
            toHeader.setTag(Integer.toHexString(new Random().nextInt(0xffffff) + 0xffffff));
            sipConnector.sendResponse(ringingResponse, this);
        } catch (Exception ex) {
            logger.fatal(ex.getMessage(), ex);
        }
    }

    /**
     * Creates an INVITE OK response.
     *
     * @param request
     */
    public void createInviteOk(Request request) {
        logger.info("CREATING 200OK FOR INVITE");
        MessageFactory messageFactory = sipConnector.getMessageFactory();
        AddressFactory addressFactory = sipConnector.getAddressFactory();
        HeaderFactory headerFactory = sipConnector.getHeaderFactory();

        ToHeader toHeader = (ToHeader) request.getHeader("To");
        Address reqToAddress = toHeader.getAddress();
        try {
            Response okResponse = messageFactory.createResponse(Response.OK, this.getServerTransaction().getRequest());

            Address contactAddress = addressFactory.createAddress(reqToAddress.toString());
            ContactHeader contactHeader = headerFactory.createContactHeader(contactAddress);
            okResponse.addHeader(contactHeader);
            MaxForwardsHeader maxForwardsHeader = headerFactory.createMaxForwardsHeader(70);
            okResponse.addHeader(maxForwardsHeader);

            AllowHeader allowHeader = headerFactory.createAllowHeader("INVITE, BYE, ACK, CANCEL, OPTIONS, INFO");
            okResponse.addHeader(allowHeader);
            ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader("application", "sdp");

//            if (request.getContent() != null) {
//                okResponse.setContent(request.getContent(), contentTypeHeader);
//            } else {
            okResponse.setContent(this.getLocalSdp(), contentTypeHeader);
            //}
            sipConnector.sendResponse(okResponse, this);
        } catch (ParseException | InvalidArgumentException e) {
            logger.fatal(e.getMessage(), e);
        }
    }

    /**
     * Creates a BYE request.
     */
    public void createBye() {
        try {
            Request byeRequest = this.getDialog().createRequest(Request.BYE);
            logger.info("CREATE BYE REQUEST");
            logger.debug("CREATE BYE REQUEST \n" + byeRequest);
            sipConnector.sendRequest(byeRequest, this);
        } catch (SipException ex) {
            logger.fatal(ex.getMessage(), ex);
        }
    }

    /**
     * Creates a BYE OK response.
     *
     * @param request
     */
    public void doByeOk(Request request) {
        MessageFactory messageFactory = sipConnector.getMessageFactory();
        try {
            Response okResponse = messageFactory.createResponse(Response.OK, request);
            sipConnector.sendResponse(okResponse, this);
        } catch (ParseException ex) {
            logger.fatal(ex.getMessage(), ex);
        }
    }

    public void doReInviteOk(Request request) {
        MessageFactory messageFactory = sipConnector.getMessageFactory();
        AddressFactory addressFactory = sipConnector.getAddressFactory();
        HeaderFactory headerFactory = sipConnector.getHeaderFactory();

        ToHeader toHeader = (ToHeader) request.getHeader("To");
        Address reqToAddress = toHeader.getAddress();
        try {
            Response okResponse = messageFactory.createResponse(Response.OK, request);

            SupportedHeader supportedHeader = headerFactory.createSupportedHeader("timer");
            okResponse.addHeader(supportedHeader);
            Header sessionExpiresHeader = request.getHeader("Session-Expires");
            okResponse.addHeader(sessionExpiresHeader);
            Address contactAddress = addressFactory.createAddress(reqToAddress.toString());
            ContactHeader contactHeader = headerFactory.createContactHeader(contactAddress);
            okResponse.addHeader(contactHeader);

            MaxForwardsHeader maxForwardsHeader = headerFactory.createMaxForwardsHeader(70);
            okResponse.addHeader(maxForwardsHeader);

            AllowHeader allowHeader = headerFactory.createAllowHeader("INVITE, BYE, ACK, CANCEL, OPTIONS, INFO");
            okResponse.addHeader(allowHeader);
            ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader("application", "sdp");

            okResponse.setContent(request.getContent(), contentTypeHeader);

            sipConnector.sendResponse(okResponse, this);
        } catch (ParseException | InvalidArgumentException ex) {
            logger.fatal(ex.getMessage(), ex);
        }
    }

    /**
     * Creates a CANCEL request.
     */
    public void createCancelRequest() {
        logger.info("CREATING CANCEL REQUEST");
        try {
            Request cancelRequest = this.getClientTransaction().createCancel();
            sipConnector.sendRequest(cancelRequest, this);
        } catch (SipException ex) {
            logger.fatal(ex.getMessage(), ex);
        }
    }

    /**
     * Creates a CANCEL response.
     *
     * @param request
     */
    public void createCancelResponse(Request request) {
        MessageFactory messageFactory = sipConnector.getMessageFactory();
        Response okResponse;
        try {
            okResponse = messageFactory.createResponse(Response.OK, request);
            sipConnector.sendResponse(okResponse, this);
            // send request terminate
            //Response requestTerminatedResponse = messageFactory.createResponse(Response.REQUEST_TERMINATED, call.getInviteRequest());
            //sipConnector.sendResponse(requestTerminatedResponse, this);
        } catch (ParseException ex) {
            logger.fatal(ex.getMessage(), ex);
        }
    }

    /**
     * Creates a request terminated ACK.
     *
     * @param response
     */
    public void createRequestTerminated(Response response) {
        Request ackRequest;
        try {
            ackRequest = dialog.createAck(((CSeqHeader) response.getHeader(CSeqHeader.NAME)).getSeqNumber());
            sipConnector.sendTerminationAck(ackRequest, dialog);
        } catch (InvalidArgumentException | SipException ex) {
            logger.fatal(ex.getMessage(), ex);
        }
    }

    public void createResponsePayload(Response response) {
        MsmlEvent eve = createResponseEvent(response, MsmlEventType.INFORESPONSE);
        setValue(eve);
    }

    public void setValue(MsmlEvent value) {
        setChanged();
        notifyObservers(value);
    }

    public String getLocalSdp() {
        return this.localSdp;
    }

    public void setLocalSdp(String content) {
        this.localSdp = content;
    }

    public String getRemoteSdp() {
        return this.remoteSdp;
    }

    public void setRemoteSdp(String content) {
        this.remoteSdp = content;
    }

    public void sendInfo(String content) {
        createInfoRequest(content);
    }

    public void sendInfoWithoutConn(String content) {
        createInfoRequestWithoutConn(content);
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getCallId() {
        return this.callId;
    }

    public String getRemoteTag() {
        return this.remoteTag;
    }

    public void setRemoteTag(String tag) {
        remoteTag = tag;
    }

    public String getFromAddress() {
        return this.fromAddr;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddr = fromAddress;
    }

    public String getToAddress() {
        return this.toAddr;
    }

    public void setToAddress(String toAdr) {
        this.toAddr = toAdr;
    }

    public String getStringMessage() {
        String contentString = "v=0\r\n"
                + "o=MsmlTool 575 654 IN IP4 10.20.120.24\r\n"
                + "s=Talk\r\n"
                + "c=IN IP4 10.20.120.24\r\n"
                + "t=0 0\r\n"
                + "m=audio 7078 RTP/AVP 0 8 18 101\r\n"
                + "a=rtpmap:0 PCMU/8000\r\n"
                + "a=rtpmap:101 telephone-event/8000\r\n"
                + "a=fmtp:101 0-11\r\n"
                + "a=sendrecv\r\n\r\n";
        return contentString;
    }

    /**
     * @return the tag.
     */
    private String tagGenerator() {
        return Integer.toHexString(new Random().nextInt(0xffffff) + 0xffffff);
    }

    /**
     * @param d, the dialog.
     */
    public void setDialog(Dialog d) {
        this.dialog = d;
    }

    /**
     * @return the dialog.
     */
    public Dialog getDialog() {
        return this.dialog;
    }

    /**
     * @return the server transaction.
     */
    public ServerTransaction getServerTransaction() {
        return this.servertransaction;
    }

    /**
     * @param s, the server transaction.
     */
    public void setServerTransaction(ServerTransaction s) {
        this.servertransaction = s;
    }

    /**
     * @return the client transaction.
     */
    public ClientTransaction getClientTransaction() {
        return this.clienttransaction;
    }

    /**
     * @param c, the client transaction.
     */
    public void setClientTransaction(ClientTransaction c) {
        this.clienttransaction = c;
    }

    /**
     * @param r, the invite request.
     */
    public void setInviteRequest(Request r) {
        this.inviteRequest = r;
    }

    /**
     * @return the inviteRequest.
     */
    public Request getInviteRequest() {
        return this.inviteRequest;
    }

    /**
     * @return the fromUser
     */
    public String getFromUser() {
        return this.fromUser;
    }

    /**
     * @param fromUser the fromUser to set
     */
    public void setFromUser(String fromUser) {
        this.fromUser = fromUser;
    }

    /**
     * @return the toUser
     */
    public String getToUser() {
        return this.toUser;
    }

    /**
     * @param toUser the toUser to set
     */
    public void setToUser(String toUser) {
        this.toUser = toUser;
    }

    public FromHeader getFromHeader(String adr) {
        return fromHeadersMap.get(adr);
    }

    public void setFromHeader(String user, String address) {
        AddressFactory addressFactory = sipConnector.getAddressFactory();
        HeaderFactory headerFactory = sipConnector.getHeaderFactory();
        try {
            SipURI fromUri = addressFactory.createSipURI(user, address);
            Address fromAddress = addressFactory.createAddress(fromUri);
            FromHeader frmHeader = headerFactory.createFromHeader(fromAddress, tagGenerator());
            fromHeadersMap.put(address, frmHeader);
        } catch (ParseException ex) {
            logger.fatal(ex.getMessage(), ex);
        }
    }

    public ToHeader getToHeader(String adr) {
        return toHeadersMap.get(adr);
    }

    public void setToHeader(String user, String address) {
        AddressFactory addressFactory = sipConnector.getAddressFactory();
        HeaderFactory headerFactory = sipConnector.getHeaderFactory();
        try {
            SipURI toUri = addressFactory.createSipURI(user, address);
            Address toAddress = addressFactory.createAddress(toUri);
            ToHeader toHeader = headerFactory.createToHeader(toAddress, null);
            toHeadersMap.put(address, toHeader);
        } catch (ParseException ex) {
            logger.fatal(ex.getMessage(), ex);
        }
    }

    public ContactHeader getContactHeader(Integer adr) {
        return contactHeadersMap.get(adr);
    }

    public void setContactHeader(String user, String adr, int port) {
        AddressFactory addressFactory = sipConnector.getAddressFactory();
        HeaderFactory headerFactory = sipConnector.getHeaderFactory();
        try {
            SipURI contactUri = addressFactory.createSipURI(user, adr);
            contactUri.setPort(port);
            Address contactAddress = addressFactory.createAddress(contactUri);
            ContactHeader contactHeader = headerFactory.createContactHeader(contactAddress);
            contactHeadersMap.put(port, contactHeader);
        } catch (ParseException ex) {
            logger.fatal(ex.getMessage(), ex);
        }
    }

    public ViaHeader getViaHeader(int adr) {
        return viaHeadersMap.get(adr);
    }

    public void setViaHeader(String host, int port, String transport, String branch) {
        HeaderFactory headerFactory = sipConnector.getHeaderFactory();
        try {
            ViaHeader viaHeader = headerFactory.createViaHeader(host, port, transport, branch);
            viaHeadersMap.put(port, viaHeader);
        } catch (Exception ex) {
            logger.fatal(ex.getMessage(), ex);
        }
    }

    public ContentTypeHeader getContentTypeHeader(String contentSubType) {
        return contentTypeHeaderMap.get(contentSubType);
    }

    public void setContentTypeHeader(String contentType, String contentSubType) {
        HeaderFactory headerFactory = sipConnector.getHeaderFactory();
        try {
            ContentTypeHeader header = headerFactory.createContentTypeHeader(contentType, contentSubType);
            contentTypeHeaderMap.put(contentSubType, header);
        } catch (Exception ex) {
            logger.fatal(ex.getMessage(), ex);
        }

    }

    public void setACKOn200(boolean b) {
        isACKOn200 = b;
    }

    public void setOKOnInfo(boolean b) {
        isOKOnInfo = b;
    }

    public void ackCall(Response response) {
        createAckRequest(response);
    }

    public CSeqHeader getCSeqHeader(String method) {
        return cSeqHeadersMap.get(method);
    }

    public void setCSeqHeader(long sequenceNumber, String method) {
        HeaderFactory headerFactory = sipConnector.getHeaderFactory();
        try {
            CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(sequenceNumber, method);
            cSeqHeadersMap.put(method, cSeqHeader);
        } catch (Exception ex) {
            logger.fatal(ex.getMessage(), ex);
        }
    }

    public void addToWaitList() {
        sipConnector.addToWaitList(this);
    }
}
