/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dialogic.msml;

import com.dialogic.XMSClientLibrary.XMSCall;
import com.dialogic.XMSClientLibrary.XMSCallState;
import com.dialogic.XMSClientLibrary.XMSEvent;
import com.dialogic.XMSClientLibrary.XMSEventType;
import com.dialogic.XMSClientLibrary.XMSMediaType;
import com.dialogic.XMSClientLibrary.XMSReturnCode;
import com.dialogic.xms.msml.BooleanDatatype;
import com.dialogic.xms.msml.Collect;
import com.dialogic.xms.msml.DialogLanguageDatatype;
import com.dialogic.xms.msml.ExitType;
import com.dialogic.xms.msml.Group;
import com.dialogic.xms.msml.IterateSendType;
import com.dialogic.xms.msml.Msml;
import com.dialogic.xms.msml.ObjectFactory;
import com.dialogic.xms.msml.Play;
import com.dialogic.xms.msml.Record;
import com.dialogic.xms.msml.Send;
import com.dialogic.xms.msml.StreamType;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sip.address.Address;
import javax.sip.header.FromHeader;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;

/**
 *
 * @author ssatyana
 */
public class XMSMsmlCall extends XMSCall implements Observer {

    static final Logger logger = Logger.getLogger(XMSMsmlCall.class.getName());

    XMSSipCall caller;
    XMSSipCall msmlSip;
    XMSMsmlConnector connector;
    private String fromAddr;
    private MsmlCallMode callMode = MsmlCallMode.OUTBOUND;
    String callerToUserId = null;
    String callerToAdr = null;
    private final Object m_synclock = new Object();
    static boolean isBlocked = false;
    static boolean isDropCall = false;
    private int mediaStatusCode;
    static boolean isRemoteDropCall = false;
    private static String connectionAddress;
    static XMSEvent xmsEvent;
    static ObjectFactory objectFactory = new ObjectFactory();
    String filename;

    public XMSMsmlCall(XMSMsmlConnector connector) {
        try {
            this.connector = connector;
            m_connector = connector;
            this.filename = connector.getConfigFileName();
            this.setFromAddress(Inet4Address.getLocalHost().getHostAddress());
            setState(XMSCallState.NULL);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    /**
     * Wait for a new MSML call.
     *
     * @return Event - This will return a CALL_CONNECTED if in AutoConnect mode,
     * CALL_OFFERED if not.
     */
    @Override
    public XMSReturnCode Waitcall() {
        try {
            this.caller = new XMSSipCall(this.connector);
            this.caller.addObserver(this);
            setCallMode(MsmlCallMode.INBOUND);
            setState(XMSCallState.WAITCALL);
            this.caller.addToWaitList();
            if (WaitcallOptions.isAutoConnect()) {
                BlockIfNeeded(XMSEventType.CALL_CONNECTED);
            } else {
                BlockIfNeeded(XMSEventType.CALL_OFFERED);
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return XMSReturnCode.SUCCESS;
    }

    /**
     * Make a MSML call to the specified destination
     *
     * @param dest
     * @return Event - This generates a CALL_CONNECTED event
     */
    @Override
    public XMSReturnCode Makecall(String dest) {
        try {
            setState(XMSCallState.MAKECALL);
            MakecallOptions.setCalledAddress(dest);
            if (!dest.isEmpty()) {
                String[] params = dest.split("@");
                if (params.length != 0) {
                    this.callerToUserId = params[0];
                    this.callerToAdr = params[1];
                }
            }

            if (isDropCall) {
                // reset                 
                this.setCallMode(MsmlCallMode.OUTBOUND);
                this.msmlSip = null;
                this.caller = null;
                MakecallOptions.EnableACKOn200(false);
                MakecallOptions.EnableOKOnInfo(false);
                isDropCall = false;
            }
            if (this.msmlSip == null) {
                this.msmlSip = new XMSSipCall(this.connector);
                this.msmlSip.addObserver(this);

                // get XMS ip address and user from config file(m_ConfigFileName)
                setXMSInfo(this.msmlSip);
                this.msmlSip.setFromAddress(Inet4Address.getLocalHost().getHostAddress());
                if (this.caller != null && this.caller.getRemoteSdp() != null) {
                    this.msmlSip.setLocalSdp(this.caller.getRemoteSdp());
                }
                if (!MakecallOptions.m_ACKOn200Enabled) {
                    this.msmlSip.setACKOn200(Boolean.FALSE);
                }
                if (!MakecallOptions.m_OKOnInfoEnabled) {
                    this.msmlSip.setOKOnInfo(Boolean.FALSE);
                }
                if (!MakecallOptions.m_sdp.isEmpty()) {
                    this.msmlSip.setLocalSdp(MakecallOptions.m_sdp);
                }
                this.msmlSip.createInviteRequest(this.msmlSip.getToUser(), this.msmlSip.getToAddress());

                if (this.caller == null) {
                    BlockIfNeeded(XMSEventType.CALL_CONNECTED);
                }
            } else if (this.caller == null && this.msmlSip != null) {
                Thread.sleep(500);
                this.caller = new XMSSipCall(this.connector);
                this.caller.addObserver(this);
                this.caller.setFromAddress(Inet4Address.getLocalHost().getHostAddress());
                this.caller.setLocalSdp(this.msmlSip.getRemoteSdp());
                this.caller.createInviteRequest(this.callerToUserId, this.callerToAdr);
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return XMSReturnCode.SUCCESS;
    }

    /**
     * Accept an incoming call.
     *
     * @return
     */
    @Override
    public XMSReturnCode Acceptcall() {
        try {
            if (this.caller != null) {
                this.caller.createRingingResponse(this.caller.getInviteRequest());
                if (!WaitcallOptions.m_autoConnectEnabled) {
                    BlockIfNeeded(XMSEventType.CALL_UPDATED);
                }
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return XMSReturnCode.SUCCESS;
    }

    /**
     * Answer an incoming call.
     *
     * @return
     */
    @Override
    public XMSReturnCode Answercall() {
        try {
            if (this.caller != null) {
                this.caller.createInviteOk(this.caller.getInviteRequest());
                if (!WaitcallOptions.m_autoConnectEnabled) {
                    BlockIfNeeded(XMSEventType.CALL_CONNECTED);
                }
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return XMSReturnCode.SUCCESS;
    }

    /**
     * Drops the call by sending messages to the XMS server.
     *
     * @return
     */
    @Override
    public XMSReturnCode Dropcall() {
        try {
            if (this.caller != null && this.msmlSip != null) {
                isDropCall = true;
                this.caller.createBye();
                this.msmlSip.createBye();
                BlockIfNeeded(XMSEventType.CALL_DISCONNECTED);
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return XMSReturnCode.SUCCESS;
    }

    /**
     * Play a file
     *
     * @param filename - File to be played
     * @return SUCCESS if play completed and the dialog exited normally, FAILURE
     * if error
     */
    @Override
    public XMSReturnCode Play(String filename) {
        try {
            if (this.msmlSip != null && filename != null) {
                this.msmlSip.sendInfo(buildPlayMsml(filename));
                setState(XMSCallState.PLAY);
                BlockIfNeeded(XMSEventType.CALL_INFO);
                if (this.getMediaStatusCode() == 200) {
                    BlockIfNeeded(XMSEventType.CALL_PLAY_END);
                } else {
                    return XMSReturnCode.FAILURE;
                }
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
//        if (isRemoteDropCall) {
//            return XMSReturnCode.FAILURE;
//        }
        return XMSReturnCode.SUCCESS;
    }

    /**
     * Record a file
     *
     * @param filename - Name of the file recorded to
     * @return SUCCESS if record completed and the dialog exited normally,
     * FAILURE if error
     */
    @Override
    public XMSReturnCode Record(String filename) {
        try {
            if (this.msmlSip != null && filename != null) {
                this.msmlSip.sendInfo(buildRecordMsml(filename));
                setState(XMSCallState.RECORD);
                BlockIfNeeded(XMSEventType.CALL_INFO);
                if (this.getMediaStatusCode() == 200) {
                    BlockIfNeeded(XMSEventType.CALL_RECORD_END);
                } else {
                    return XMSReturnCode.FAILURE;
                }
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
//        if (isRemoteDropCall) {
//            return XMSReturnCode.FAILURE;
//        }
        return XMSReturnCode.SUCCESS;
    }

    /**
     * Collect the DTMF Digit information
     *
     * @return SUCCESS if collect completed and the dialog exited normally,
     * FAILURE if error
     */
    @Override
    public XMSReturnCode CollectDigits() {
        try {
            if (this.msmlSip != null) {
                this.msmlSip.sendInfo(buildCollectMsml());
                setState(XMSCallState.COLLECTDIGITS);
                BlockIfNeeded(XMSEventType.CALL_INFO);
                if (this.getMediaStatusCode() == 200) {
                    BlockIfNeeded(XMSEventType.CALL_COLLECTDIGITS_END);
                } else {
                    return XMSReturnCode.FAILURE;
                }
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return XMSReturnCode.SUCCESS;
    }

    /**
     * Join / Route 2 Calls together
     *
     * @param call
     * @return SUCCESS if join completed, FAILURE if error
     */
    @Override
    public XMSReturnCode Join(XMSCall call) {
        try {
            XMSMsmlCall c = (XMSMsmlCall) call;
            if (c != null && c.msmlSip != null && this.msmlSip != null) {
                boolean v = false;
                if (this.WaitcallOptions.m_mediatype == XMSMediaType.VIDEO
                        && call.WaitcallOptions.m_mediatype == XMSMediaType.VIDEO) {
                    v = true;
                }
                msmlSip.sendInfoWithoutConn(buildJoinMsml(this.msmlSip, c.msmlSip, v));
                setState(XMSCallState.JOINING);
                BlockIfNeeded(XMSEventType.CALL_INFO);
                if (this.getMediaStatusCode() == 200) {
                    //BlockIfNeeded(XMSEventType.CALL_UPDATED);
                    return XMSReturnCode.SUCCESS;
                } else {
                    return XMSReturnCode.FAILURE;
                }
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return XMSReturnCode.FAILURE;
    }

    /**
     * Play prompt and Collect the DTMF Digit information
     *
     * @param filename - File to be played
     * @return SUCCESS if play collect completed, FAILURE if error
     */
    @Override
    public XMSReturnCode PlayCollect(String filename) {
        try {
            if (this.msmlSip != null && filename != null) {
                this.msmlSip.sendInfo(buildPlayCollectMsml(filename));
                setState(XMSCallState.PLAYCOLLECT);
                BlockIfNeeded(XMSEventType.CALL_INFO);
                if (this.getMediaStatusCode() == 200) {
                    BlockIfNeeded(XMSEventType.CALL_COLLECTDIGITS_END);
                } else {
                    return XMSReturnCode.FAILURE;
                }
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return XMSReturnCode.SUCCESS;
    }

    /**
     * Send custom MSML content
     *
     * @param msml - the MSML content
     * @return SUCCESS if INFO response is 200, FAILURE otherwise
     */
    @Override
    public XMSReturnCode SendInfo(String msml) {
        try {
            if (this.msmlSip != null && msml != null) {
                m_state = XMSCallState.CUSTOM;
                this.msmlSip.sendInfo(msml);
                BlockIfNeeded(XMSEventType.CALL_INFO);
                if (this.getMediaStatusCode() == 200) {
                    BlockIfNeeded(XMSEventType.CALL_INFO);
                } else {
                    return XMSReturnCode.FAILURE;
                }
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return XMSReturnCode.SUCCESS;
    }

    public String getFromAddress() {
        return this.fromAddr;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddr = fromAddress;
    }

    /**
     * This is the Notify handler that will be called by EventThread when new
     * events are created.
     *
     * @param o
     * @param o1
     */
    @Override
    public void update(Observable o, Object o1) {
        MsmlEvent e = (MsmlEvent) o1;
        if (e.getType().equals(MsmlEventType.INCOMING)) {
            if (this.getCallMode() == MsmlCallMode.INBOUND) {
                // incoming call, send INVITE request to XMS
                FromHeader fromHeader = (FromHeader) e.getReq().getHeader("From");
                Address reqToAddress = fromHeader.getAddress();
                String incomingAdr = reqToAddress.toString();
                Pattern pattern = Pattern.compile("sip:(.*?)>");
                Matcher m = pattern.matcher(incomingAdr);
                if (m.find()) {
                    String event = m.group(1);
                    setConnectionAddress(event);
                }
                Makecall("");
            } else if (this.getCallMode() == MsmlCallMode.OUTBOUND) {
            }
        } else if (e.getType().equals(MsmlEventType.RINGING)) {
            if (this.getCallMode() == MsmlCallMode.INBOUND) {
                if (WaitcallOptions.m_autoConnectEnabled) {
                    // if auto connect then accept the incoming call
                    Acceptcall();
                } else {
                    // if not then create an xms event and return
                    setState(XMSCallState.OFFERED);
                    XMSEvent xmsEvent = new XMSEvent();
                    xmsEvent.CreateEvent(XMSEventType.CALL_OFFERED, this, "", "",
                            e.getReq().toString());
                    UnblockIfNeeded(xmsEvent);
                }

            } else if (this.getCallMode() == MsmlCallMode.OUTBOUND) {
            }
        } else if (e.getType().equals(MsmlEventType.CONNECTING)) {
            if (this.getCallMode() == MsmlCallMode.INBOUND) {
                if (WaitcallOptions.m_autoConnectEnabled) {
                    if (this.caller.getLocalSdp() == null) {
                        this.caller.setLocalSdp(this.msmlSip.getRemoteSdp());
                    }
                    // send 200 ok for invite req
                    Answercall();
                } else {
                    if (this.caller.getLocalSdp() == null) {
                        this.caller.setLocalSdp(this.msmlSip.getRemoteSdp());
                    }
                    setState(XMSCallState.ACCEPTED);
                    XMSEvent xmsEvent = new XMSEvent();
                    xmsEvent.CreateEvent(XMSEventType.CALL_UPDATED, this, "", "",
                            e.getRes().toString());
                    UnblockIfNeeded(xmsEvent);
                }
            } else if (this.getCallMode() == MsmlCallMode.OUTBOUND) {
                if (e.getCall() == this.caller) {
                    this.msmlSip.createAckRequest(e.getRes());
                } else if (this.caller == null) {
                    if (this.callerToAdr != this.msmlSip.getToAddress()) {
                        // connected to XMS now make an outbound call tot he dest
                        Makecall(this.callerToUserId + "@" + this.callerToAdr);
                    }
                }
            } else {
                synchronized (m_synclock) {
                    isBlocked = true;
                    m_synclock.notifyAll();
                }
            }
        } else if (e.getType().equals(MsmlEventType.CONNECTED)) {
            if (this.getCallMode() == MsmlCallMode.INBOUND) {
                // recieved ACK from inbound call and its connected
                if (e.getCall() == this.caller) {
                    setState(XMSCallState.CONNECTED);
                    XMSEvent xmsEvent = new XMSEvent();
                    xmsEvent.CreateEvent(XMSEventType.CALL_CONNECTED, this, "", "",
                            e.getReq().toString());
                    UnblockIfNeeded(xmsEvent);
                }
            } else if (this.getCallMode() == MsmlCallMode.OUTBOUND) {
                if (e.getCall() == this.caller) {
                    setState(XMSCallState.CONNECTED);
                    XMSEvent xmsEvent = new XMSEvent();
                    xmsEvent.CreateEvent(XMSEventType.CALL_CONNECTED, this, "", "",
                            e.getRes().toString());
                    UnblockIfNeeded(xmsEvent);
                }
            }
        } else if (e.getType().equals(MsmlEventType.INFORESPONSE)) {
            // response to an INFO request
            if (e.getCall() == this.msmlSip) {
                String reponseMessage = new String(e.getRes().getRawContent());
                if (getState() == XMSCallState.CUSTOM) {
                    // if custom msml content then create an event and return
                    if (e.getRes().getRawContent() != null) {
                        Msml msml = unmarshalObject(new ByteArrayInputStream((byte[]) e.getRes().getRawContent()));
                        Msml.Result result = msml.getResult();
                        this.setMediaStatusCode(Integer.parseInt(result.getResponse()));
                    }
                    XMSEvent xmsEvent = new XMSEvent();
                    xmsEvent.CreateEvent(XMSEventType.CALL_INFO, this, "", "", reponseMessage);
                    setLastEvent(xmsEvent);
                    UnblockIfNeeded(xmsEvent);
                } else if (getState() != XMSCallState.DISCONNECTED) {
                    if (e.getRes().getRawContent() != null) {
                        Msml msml = unmarshalObject(new ByteArrayInputStream((byte[]) e.getRes().getRawContent()));
                        Msml.Result result = msml.getResult();
                        if (result.getResponse().equalsIgnoreCase("200")) {
                            this.setMediaStatusCode(Integer.parseInt(result.getResponse()));
                            XMSEvent xmsEvent = new XMSEvent();
                            xmsEvent.CreateEvent(XMSEventType.CALL_INFO, this, result.getResponse(), "", reponseMessage);
                            setLastEvent(xmsEvent);
                            UnblockIfNeeded(xmsEvent);
                        } else {
                            this.setMediaStatusCode(Integer.parseInt(result.getResponse()));
                            XMSEvent xmsEvent = new XMSEvent();
                            xmsEvent.CreateEvent(XMSEventType.CALL_INFO, this, result.getResponse(), "", reponseMessage);
                            setLastEvent(xmsEvent);
                            UnblockIfNeeded(xmsEvent);
                        }
                    }
                }
            } else if (e.getCall() == this.caller) {
                this.msmlSip.sendInfoResponse();
            }
        } else if (e.getType().equals(MsmlEventType.INFOREQUEST)) {
            // INFO request from XMS
            if (!MakecallOptions.m_OKOnInfoEnabled) {
                this.msmlSip.createInfoResponse(e.getReq());
            }
            if (e.getReq().getRawContent() != null) {
                String info = new String(e.getReq().getRawContent());

                Pattern mediaControlPattern = Pattern.compile("<media_control>(.+?)</media_control>");
                Matcher mediaControlMatcher = mediaControlPattern.matcher(info);
                String mediaControl = null;
                if (mediaControlMatcher.find()) {
                    mediaControl = mediaControlMatcher.group(1);
                }
                if (getState() == XMSCallState.CUSTOM) {
                    xmsEvent = new XMSEvent();
                    xmsEvent.CreateEvent(XMSEventType.CALL_INFO, this, "", "", info);
                    setLastEvent(xmsEvent);
                } else if (mediaControl != null && e.getCall() == this.msmlSip) {
                    if (this.caller != null) {
                        this.caller.sendInfoWithoutConn(info);
                    }
                    xmsEvent = new XMSEvent();
                    xmsEvent.CreateEvent(XMSEventType.CALL_INFO, this, "", "", info);
                    setLastEvent(xmsEvent);
                } else {
                    Msml msml = unmarshalObject(new ByteArrayInputStream((byte[]) e.getReq().getRawContent()));
                    Msml.Event event = msml.getEvent();
                    String eventName = event.getName();

                    Pattern dialogPattern = Pattern.compile("dialog:(.*)");
                    Matcher dialogMatcher = dialogPattern.matcher(event.getId());
                    String dialogType = null;
                    if (dialogMatcher.find()) {
                        dialogType = dialogMatcher.group(1);
                    }

                    if (eventName != null && eventName.equalsIgnoreCase("moml.exit")) {
                        List<JAXBElement<String>> eventNameValueList = event.getNameAndValue();
                        Map<String, String> events = new HashMap<>();
                        for (int i = 0, n = eventNameValueList.size(); i < n; i += 2) {
                            System.out.println("[i] -> " + eventNameValueList.get(i).getValue());
                            System.out.println("[i+1] -> " + eventNameValueList.get(i + 1).getValue());

                            events.put(eventNameValueList.get(i).getValue(),
                                    eventNameValueList.get(i + 1).getValue());
                        }
                        if (dialogType != null) {
                            xmsEvent = new XMSEvent();
                            xmsEvent.setInternalData(info);
                            switch (dialogType) {
                                case "Play":
                                    String amt = events.get("play.amt");
                                    String playReason = events.get("play.end");
                                    xmsEvent.CreateEvent(XMSEventType.CALL_PLAY_END, this, amt, playReason, info);
                                    xmsEvent.setReason(playReason);
                                    setLastEvent(xmsEvent);
                                    break;
                                case "Record":
                                    String len = events.get("record.len");
                                    String recordReason = events.get("record.end");
                                    xmsEvent.CreateEvent(XMSEventType.CALL_RECORD_END, this, len, recordReason, info);
                                    xmsEvent.setReason(recordReason);
                                    setLastEvent(xmsEvent);
                                    break;
                                default:
                                    xmsEvent.CreateEvent(XMSEventType.CALL_INFO, this, "", "", info);
                                    setLastEvent(xmsEvent);
                                    break;
                            }
                        }
                    } else if (eventName != null && eventName.equalsIgnoreCase("nomatch")
                            || eventName != null && eventName.equalsIgnoreCase("dtmfexit")
                            || eventName != null && eventName.equalsIgnoreCase("termkey")
                            || eventName != null && eventName.equalsIgnoreCase("noinput")) {
                        xmsEvent = new XMSEvent();
                        List<JAXBElement<String>> eventNameValueList = event.getNameAndValue();
                        Map<String, String> events = new HashMap<>();
                        for (int i = 0, n = eventNameValueList.size(); i < n; i += 2) {
                            System.out.println("[i] -> " + eventNameValueList.get(i).getValue());
                            System.out.println("[i+1] -> " + eventNameValueList.get(i + 1).getValue());

                            events.put(eventNameValueList.get(i).getValue(),
                                    eventNameValueList.get(i + 1).getValue());
                        }
                        String dtmfDigits = events.get("dtmf.digits");
                        String dtmfEnd = events.get("dtmf.end");
                        String dtmfLen = events.get("dtmf.len");
                        if (dtmfDigits.contains(CollectDigitsOptions.m_terminateDigits)) {
                            xmsEvent.CreateEvent(XMSEventType.CALL_COLLECTDIGITS_END, this,
                                    dtmfDigits, "term-digit", info);
                            xmsEvent.setReason("term-digit");
                        } else if (dtmfLen.equalsIgnoreCase(CollectDigitsOptions.m_maxDigits)) {
                            xmsEvent.CreateEvent(XMSEventType.CALL_COLLECTDIGITS_END, this,
                                    dtmfDigits, "max-digits", info);
                            xmsEvent.setReason("max-digits");
                        } else {
                            xmsEvent.CreateEvent(XMSEventType.CALL_COLLECTDIGITS_END, this,
                                    dtmfDigits, dtmfEnd, info);
                            xmsEvent.setReason(dtmfEnd);
                        }
                        setLastEvent(xmsEvent);
                    } else if (eventName != null && eventName.equalsIgnoreCase("msml.dialog.exit")) {
                        if (getState() != XMSCallState.DISCONNECTED) {
                            if (dialogType != null) {
                                switch (dialogType) {
                                    case "Play":
                                        setState(XMSCallState.PLAY_END);
                                        break;
                                    case "Record":
                                        setState(XMSCallState.RECORD_END);
                                        break;
                                }
                            }
                            if (xmsEvent == null) {
                                xmsEvent = new XMSEvent();
                                List<JAXBElement<String>> eventNameValueList = event.getNameAndValue();
                                Map<String, String> events = new HashMap<>();
                                for (int i = 0, n = eventNameValueList.size(); i < n; i += 2) {
                                    System.out.println("[i] -> " + eventNameValueList.get(i).getValue());
                                    System.out.println("[i+1] -> " + eventNameValueList.get(i + 1).getValue());

                                    events.put(eventNameValueList.get(i).getValue(),
                                            eventNameValueList.get(i + 1).getValue());
                                }
                                String dialogExitStatus = events.get("dialog.exit.status");
                                String dialogExitDesc = events.get("dialog.exit.description");
                                xmsEvent.CreateEvent(XMSEventType.CALL_INFO, this, dialogExitStatus, dialogExitDesc, info);
                                xmsEvent.setReason(dialogExitDesc);
                                setLastEvent(xmsEvent);
                            } else {
                                UnblockIfNeeded(xmsEvent);
                            }
                        } else {
                            isRemoteDropCall = true;
                            xmsEvent = new XMSEvent();
                            xmsEvent.CreateEvent(XMSEventType.CALL_DISCONNECTED, this,
                                    "", "Call Dropped", "Dialogend and close connection");
                            xmsEvent.setReason("Call Dropped");
                            setLastEvent(xmsEvent);
                            this.msmlSip.createBye();
                            //UnblockIfNeeded(xmsEvent);
                        }
                    }
                }
            }
        } else if (e.getType().equals(MsmlEventType.DISCONNECTED)) {
            xmsEvent = new XMSEvent();
            if (e.getCall() == this.msmlSip) {
                if (isDropCall) {
                    isDropCall = false;
                    this.setCallMode(MsmlCallMode.OUTBOUND);
                    this.msmlSip = null;
                    this.caller = null;
                    MakecallOptions.EnableACKOn200(false);
                    MakecallOptions.EnableOKOnInfo(false);
                    setState(XMSCallState.DISCONNECTED);
                    xmsEvent = new XMSEvent();
                    String data = null;
                    if (e.getReq() != null) {
                        data = e.getReq().toString();
                    } else if (e.getRes() != null) {
                        data = e.getRes().toString();
                    }
                    xmsEvent.CreateEvent(XMSEventType.CALL_DISCONNECTED, this, "", "", data);
                    setLastEvent(xmsEvent);
                    UnblockIfNeeded(xmsEvent);
                } else if (getState() != XMSCallState.DISCONNECTED) {
                    if (e.getReq() != null) {
                        setState(XMSCallState.DISCONNECTED);
                        xmsEvent.CreateEvent(XMSEventType.CALL_DISCONNECTED, this, "", "", e.getReq().toString());
                        setLastEvent(xmsEvent);
                        this.msmlSip.doByeOk(e.getReq());
                        this.caller.createBye();
                    }
                }
            } else if (e.getCall() == this.caller) {
                if (getState() != XMSCallState.DISCONNECTED) {
                    System.out.println(e.getReq().toString());
                    if (getState() == XMSCallState.PLAY) {
                        //media active, send dialog exit before bye
                        this.msmlSip.createDialogEndRequest(buildDialogExit("Play"));
                        setState(XMSCallState.DISCONNECTED);
                        xmsEvent.CreateEvent(XMSEventType.CALL_DISCONNECTED, this, "", "", e.getReq().toString());
                        setLastEvent(xmsEvent);
                        this.caller.doByeOk(e.getReq());
                    } else if (getState() == XMSCallState.RECORD) {
                        this.msmlSip.createDialogEndRequest(buildDialogExit("Record"));
                        setState(XMSCallState.DISCONNECTED);
                        xmsEvent.CreateEvent(XMSEventType.CALL_DISCONNECTED, this, "", "", e.getReq().toString());
                        setLastEvent(xmsEvent);
                        this.caller.doByeOk(e.getReq());
                    } else {
                        if (e.getReq() != null) {
                            setState(XMSCallState.DISCONNECTED);
                            xmsEvent.CreateEvent(XMSEventType.CALL_DISCONNECTED, this, "", "", e.getReq().toString());
                            setLastEvent(xmsEvent);
                            this.caller.doByeOk(e.getReq());
                            this.msmlSip.createBye();
                        }
                    }
                }
            }
        } else if (e.getType().equals(MsmlEventType.CANCEL)) {
            if (e.getCall() == this.caller) {
                xmsEvent = new XMSEvent();
                setState(XMSCallState.DISCONNECTED);
                xmsEvent.CreateEvent(XMSEventType.CALL_DISCONNECTED, this, "", "", e.getReq().toString());
                setLastEvent(xmsEvent);
                this.msmlSip.createCancelRequest();
                this.msmlSip.createBye();
                this.caller.createCancelResponse(e.getReq());
            }
        }
    }

    /**
     * Builds MSML play script
     *
     * @param fileName - File to be played.
     * @return MSML script.
     */
    private String buildPlayMsml(String fileName) {
        java.io.StringWriter sw = new StringWriter();

        Msml msml = objectFactory.createMsml();
        msml.setVersion("1.1");

        Msml.Dialogstart dialogstart = objectFactory.createMsmlDialogstart();
        dialogstart.setTarget("conn:1234");
        dialogstart.setType(DialogLanguageDatatype.APPLICATION_MOML_XML);
        dialogstart.setName("Play");

        Group group = objectFactory.createGroup();
        group.setTopology("parallel");

        Play play = objectFactory.createPlay();
        int repeat = Integer.parseInt(PlayOptions.m_repeat);
        if (repeat > 0) {
            play.setIterate(Integer.toString(repeat++));
            //TODO may need to trim "s" off the offset
            play.setInterval(PlayOptions.m_delay);
        }

        if (!PlayOptions.m_offset.equalsIgnoreCase("0s")) {
            if (PlayOptions.m_mediaType == XMSMediaType.AUDIO) {
                //TODO may need to trim "s" off the offset
                play.setOffset(PlayOptions.m_offset);
            } else {
                logger.warning("As per spec offset only supported for Audio, ignoring parameter");
            }
        }
        Play.Media media = objectFactory.createPlayMedia();
        Play.Media.Audio audio = objectFactory.createPlayMediaAudio();
        if (fileName.contains(".wav")) {
            audio.setUri("file://" + fileName);
        } else {
            audio.setUri("file://" + fileName + ".wav");
        }

        audio.setFormat("audio/wav;codec=L16");
        audio.setAudiosamplerate(BigInteger.valueOf(16000));
        audio.setAudiosamplesize(BigInteger.valueOf(16));
        media.getAudioOrVideo().add(audio);

        Play.Media.Video video = null;
        if (PlayOptions.m_mediaType == XMSMediaType.VIDEO) {
            video = objectFactory.createPlayMediaVideo();
            if (fileName.contains(".vid")) {
                video.setUri("file://" + fileName);
            } else {
                video.setUri("file://" + fileName + ".vid");
            }
            video.setFormat("video/x-vid;codec=h264");
            media.getAudioOrVideo().add(video);
        }

        Play.Playexit playexit = objectFactory.createPlayPlayexit();
        ExitType exitType = new ExitType();
        exitType.setNamelist("play.end play.amt");
        playexit.setExit(exitType);
        play.setPlayexit(playexit);

        play.getAudioOrVideoOrMedia().add(objectFactory.createPlayMedia(media));

        group.getPrimitive().add(objectFactory.createPlay(play));

        if (!PlayOptions.m_terminateDigits.isEmpty()) {
            Collect collect = objectFactory.createCollect();
            Collect.Pattern termDigPattern = objectFactory.createCollectPattern();
            termDigPattern.setDigits(PlayOptions.m_terminateDigits);
            Send sendDigit = objectFactory.createSend();
            sendDigit.setTarget("source");
            sendDigit.setEvent("TermkeyRecieved");
            sendDigit.setNamelist("dtmf.digits dtmf.len dtmf.last");
            termDigPattern.getSend().add(sendDigit);

            Send playTermSend = objectFactory.createSend();
            playTermSend.setTarget("play");
            playTermSend.setEvent("terminate");
            termDigPattern.getSend().add(playTermSend);

            collect.getPattern().add(termDigPattern);
            group.getPrimitive().add(objectFactory.createCollect(collect));
        }
        dialogstart.getMomlRequest().add(objectFactory.createGroup(group));
        msml.getMsmlRequest().add(dialogstart);

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Msml.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(msml, sw);

        } catch (JAXBException ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }

        System.out.println("MSML PLAY -> " + sw.toString());
        return sw.toString();
    }

    /**
     * Builds MSML record script
     *
     * @param fileName - File to be recorded to.
     * @return MSML script.
     */
    private String buildRecordMsml(String fileName) {
        java.io.StringWriter sw = new StringWriter();

        Msml msml = objectFactory.createMsml();
        msml.setVersion("1.1");

        Msml.Dialogstart dialogstart = objectFactory.createMsmlDialogstart();
        dialogstart.setTarget("conn:1234");
        dialogstart.setType(DialogLanguageDatatype.APPLICATION_MOML_XML);
        dialogstart.setName("Record");

        Group group = objectFactory.createGroup();
        group.setTopology("parallel");

        Record record = objectFactory.createRecord();
        record.setBeep(BooleanDatatype.TRUE);

        if (RecordOptions.m_mediaType == XMSMediaType.VIDEO) {
            if (fileName.contains(".vid")) {
                record.setVideodest("file://" + fileName);
            } else {
                record.setVideodest("file://" + fileName + ".vid");
            }
            record.setFormat("video/x-vid");
        }

        if (fileName.contains(".wav")) {
            record.setAudiodest("file://" + fileName);
        } else {
            record.setAudiodest("file://" + fileName + ".wav");
        }

        if (record.getFormat() != null) {
            record.setFormat("audio/wav;" + record.getFormat() + ";codec=L16,h264");
        } else {
            record.setFormat("audio/wav;codec=L16");
        }

        if (!RecordOptions.m_maxTime.isEmpty()) {
            record.setMaxtime(RecordOptions.m_maxTime);
        }

        record.setAudiosamplerate(BigInteger.valueOf(16000));
        record.setAudiosamplesize(BigInteger.valueOf(16));

        Record.Recordexit recordExit = objectFactory.createRecordRecordexit();
        ExitType exitType = new ExitType();
        exitType.setNamelist("record.end record.len");
        recordExit.setExit(exitType);
        record.setRecordexit(recordExit);

        group.getPrimitive().add(objectFactory.createRecord(record));

        if (!RecordOptions.m_terminateDigits.isEmpty()) {
            Collect collect = objectFactory.createCollect();
            Collect.Pattern termDigPattern = objectFactory.createCollectPattern();
            termDigPattern.setDigits(RecordOptions.m_terminateDigits);
            Send sendDigit = objectFactory.createSend();
            sendDigit.setTarget("source");
            sendDigit.setEvent("TermkeyRecieved");
            sendDigit.setNamelist("dtmf.digits dtmf.len dtmf.last");
            termDigPattern.getSend().add(sendDigit);

            Send recordTermSend = objectFactory.createSend();
            recordTermSend.setTarget("record");
            recordTermSend.setEvent("terminate");
            termDigPattern.getSend().add(recordTermSend);

            collect.getPattern().add(termDigPattern);
            group.getPrimitive().add(objectFactory.createCollect(collect));
        }
        dialogstart.getMomlRequest().add(objectFactory.createGroup(group));
        msml.getMsmlRequest().add(dialogstart);

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Msml.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(msml, sw);

        } catch (JAXBException ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }

        System.out.println("MSML RECORD -> " + sw.toString());
        return sw.toString();
    }

    /**
     * Builds MSML dialog end script
     *
     * @param type - Either play or record.
     * @return MSML script.
     */
    private static String buildDialogExit(String type) {
        java.io.StringWriter sw = new StringWriter();

        Msml msml = objectFactory.createMsml();
        msml.setVersion("1.1");

        Msml.Dialogend dialogEnd = objectFactory.createMsmlDialogend();
        dialogEnd.setId("conn:1234/dialog:" + type);
        msml.getMsmlRequest().add(dialogEnd);

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Msml.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(msml, sw);

        } catch (JAXBException ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
        System.out.println("DIALOG END -> " + sw.toString());
        return sw.toString();
    }

    /**
     * Builds MSML collect script
     *
     * @return MSML script.
     */
    private String buildCollectMsml() {
        java.io.StringWriter sw = new StringWriter();

        Msml msml = objectFactory.createMsml();
        msml.setVersion("1.1");

        Msml.Dialogstart dialogstart = objectFactory.createMsmlDialogstart();
        dialogstart.setTarget("conn:1234");
        dialogstart.setType(DialogLanguageDatatype.APPLICATION_MOML_XML);
        dialogstart.setName("Collect");

        Collect collect = objectFactory.createCollect();
        if (!CollectDigitsOptions.m_timeoutValue.equalsIgnoreCase("0s")) {
            collect.setFdt(CollectDigitsOptions.m_timeoutValue);
        } else {
            collect.setFdt("10s");
        }
        collect.setIdt("2s");
        collect.setStarttimer(BooleanDatatype.TRUE);
        collect.setCleardb(BooleanDatatype.TRUE);
        Collect.Pattern termDigPattern = objectFactory.createCollectPattern();
        termDigPattern.setDigits(CollectDigitsOptions.m_terminateDigits);

        Send sendDigit = objectFactory.createSend();
        sendDigit.setTarget("source");
        sendDigit.setEvent("termKey");
        sendDigit.setNamelist("dtmf.digits dtmf.len dtmf.end");
        termDigPattern.getSend().add(sendDigit);
        collect.getPattern().add(termDigPattern);

        Collect.Pattern digitsPattern = objectFactory.createCollectPattern();
        int length = Integer.parseInt(CollectDigitsOptions.m_maxDigits);
        StringBuilder stringBuilder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            stringBuilder.append("x");
        }
        digitsPattern.setDigits(stringBuilder.toString());
        collect.getPattern().add(digitsPattern);

        IterateSendType noinput = objectFactory.createIterateSendType();
        Send sendNoInput = objectFactory.createSend();
        sendNoInput.setTarget("source");
        sendNoInput.setEvent("noinput");
        sendNoInput.setNamelist("dtmf.digits dtmf.len dtmf.end");
        noinput.getSend().add(sendNoInput);
        collect.setNoinput(noinput);

        IterateSendType nomatch = objectFactory.createIterateSendType();
        Send sendNoMatch = objectFactory.createSend();
        sendNoMatch.setTarget("source");
        sendNoMatch.setEvent("nomatch");
        sendNoMatch.setNamelist("dtmf.digits dtmf.len dtmf.end");
        nomatch.getSend().add(sendNoMatch);
        collect.setNomatch(nomatch);

        Collect.Dtmfexit dtmfexit = objectFactory.createCollectDtmfexit();
        Send sendDtmf = objectFactory.createSend();
        sendDtmf.setTarget("source");
        sendDtmf.setEvent("dtmfexit");
        sendDtmf.setNamelist("dtmf.digits dtmf.len dtmf.end");
        dtmfexit.getSend().add(sendDtmf);
        collect.setDtmfexit(dtmfexit);

        dialogstart.getMomlRequest().add(objectFactory.createCollect(collect));
        msml.getMsmlRequest().add(dialogstart);
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Msml.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(msml, sw);

        } catch (JAXBException ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
        //System.out.println("MSML COLLECT DIGITS -> " + sw.toString());
        return sw.toString();
    }

    /**
     * Builds MSML join script
     *
     * @param c1 - Caller 1
     * @param c2 - Caller 2
     * @param isVideo - true if video call, false otherwise
     * @return MSML script.
     */
    private static String buildJoinMsml(XMSSipCall c1, XMSSipCall c2, boolean isVideo) {
        java.io.StringWriter sw = new StringWriter();

        Msml msml = objectFactory.createMsml();
        msml.setVersion("1.1");

        Msml.Join join = objectFactory.createMsmlJoin();
        join.setId1("conn:" + c1.getRemoteTag());
        join.setId2("conn:" + c2.getRemoteTag());
        join.setMark("1");

        if (isVideo) {
            StreamType streamType1 = objectFactory.createStreamType();
            streamType1.setMedia("audio");

            StreamType streamType2 = objectFactory.createStreamType();
            streamType2.setMedia("video");
            streamType2.setDir("from-id1");
            streamType2.setDisplay("1");

            StreamType streamType3 = objectFactory.createStreamType();
            streamType3.setMedia("video");
            streamType3.setDir("to-id1");

            join.getStream().add(streamType1);
            join.getStream().add(streamType2);
            join.getStream().add(streamType3);
        } else {
            StreamType streamType1 = objectFactory.createStreamType();
            streamType1.setMedia("audio");
            streamType1.setDir("from-id1");

            StreamType streamType2 = objectFactory.createStreamType();
            streamType2.setMedia("audio");
            streamType2.setDir("to-id1");

            join.getStream().add(streamType1);
            join.getStream().add(streamType2);
        }
        msml.getMsmlRequest().add(join);

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Msml.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(msml, sw);

        } catch (JAXBException ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }

        System.out.println("MSML JOIN -> " + sw.toString());
        return sw.toString();
    }

    /**
     * Builds MSML play collect script
     *
     * @param fileName - File to be played.
     * @return MSML script.
     */
    private String buildPlayCollectMsml(String fileName) {
        java.io.StringWriter sw = new StringWriter();

        Msml msml = objectFactory.createMsml();
        msml.setVersion("1.1");

        Msml.Dialogstart dialogstart = objectFactory.createMsmlDialogstart();
        dialogstart.setTarget("conn:1234");
        dialogstart.setType(DialogLanguageDatatype.APPLICATION_MOML_XML);
        dialogstart.setName("Collect");

        Group group = objectFactory.createGroup();
        group.setTopology("parallel");

        Play play = objectFactory.createPlay();
        play.setBarge(BooleanDatatype.TRUE);
        play.setCleardb(BooleanDatatype.TRUE);

        Play.Media media = objectFactory.createPlayMedia();
        Play.Media.Audio audio = objectFactory.createPlayMediaAudio();
        if (fileName.contains(".wav")) {
            audio.setUri("file://" + fileName);
        } else {
            audio.setUri("file://" + fileName + ".wav");
        }
        media.getAudioOrVideo().add(audio);
        play.getAudioOrVideoOrMedia().add(objectFactory.createPlayMedia(media));

        Play.Playexit playexit = objectFactory.createPlayPlayexit();
        Send sendPlay = objectFactory.createSend();
        sendPlay.setTarget("collect");
        sendPlay.setEvent("starttimer");
        playexit.getSend().add(sendPlay);
        play.setPlayexit(playexit);

        group.getPrimitive().add(objectFactory.createPlay(play));

        Collect collect = objectFactory.createCollect();
        if (!CollectDigitsOptions.m_timeoutValue.equalsIgnoreCase("0s")) {
            collect.setFdt(CollectDigitsOptions.m_timeoutValue);
        } else {
            collect.setFdt("10s");
        }
        collect.setIdt("2s");
        collect.setStarttimer(BooleanDatatype.TRUE);
        collect.setCleardb(BooleanDatatype.TRUE);
        Collect.Pattern termDigPattern = objectFactory.createCollectPattern();
        termDigPattern.setDigits(CollectDigitsOptions.m_terminateDigits);

        Send sendDigit = objectFactory.createSend();
        sendDigit.setTarget("source");
        sendDigit.setEvent("termKey");
        sendDigit.setNamelist("dtmf.digits dtmf.len dtmf.end");
        termDigPattern.getSend().add(sendDigit);
        collect.getPattern().add(termDigPattern);

        Collect.Pattern digitsPattern = objectFactory.createCollectPattern();
        int length = Integer.parseInt(CollectDigitsOptions.m_maxDigits);
        StringBuilder stringBuilder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            stringBuilder.append("x");
        }
        digitsPattern.setDigits(stringBuilder.toString());
        collect.getPattern().add(digitsPattern);

        IterateSendType noinput = objectFactory.createIterateSendType();
        Send sendNoInput = objectFactory.createSend();
        sendNoInput.setTarget("source");
        sendNoInput.setEvent("noinput");
        sendNoInput.setNamelist("dtmf.digits dtmf.len dtmf.end");
        noinput.getSend().add(sendNoInput);
        collect.setNoinput(noinput);

        IterateSendType nomatch = objectFactory.createIterateSendType();
        Send sendNoMatch = objectFactory.createSend();
        sendNoMatch.setTarget("source");
        sendNoMatch.setEvent("nomatch");
        sendNoMatch.setNamelist("dtmf.digits dtmf.len dtmf.end");
        nomatch.getSend().add(sendNoMatch);
        collect.setNomatch(nomatch);

        Collect.Dtmfexit dtmfexit = objectFactory.createCollectDtmfexit();
        Send sendDtmf = objectFactory.createSend();
        sendDtmf.setTarget("source");
        sendDtmf.setEvent("dtmfexit");
        sendDtmf.setNamelist("dtmf.digits dtmf.len dtmf.end");
        dtmfexit.getSend().add(sendDtmf);
        collect.setDtmfexit(dtmfexit);

        group.getPrimitive().add(objectFactory.createCollect(collect));

        dialogstart.getMomlRequest().add(objectFactory.createGroup(group));
        msml.getMsmlRequest().add(dialogstart);
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Msml.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(msml, sw);

        } catch (JAXBException ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
        //System.out.println("MSML COLLECT DIGITS -> " + sw.toString());
        return sw.toString();
    }

    /**
     * This function gets the XMS related data from the config file
     *
     * @param c
     */
    private void setXMSInfo(XMSSipCall c) {
        try {
            System.out.println("FILENAME! -> " + this.filename);
            FileInputStream xmlFile = new FileInputStream(this.filename);
            Document doc = new Builder().build(xmlFile);
            Element root = doc.getRootElement();
            Elements entries = root.getChildElements();
            for (int x = 0; x < entries.size(); x++) {
                Element element = entries.get(x);
                if (element.getLocalName().equals("appid")) {
                    c.setToUser(element.getValue());
                }
                if (element.getLocalName().equals("baseurl")) {
                    c.setToAddress(element.getValue());
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(XMSSipCall.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * @return the connectionAddress
     */
    public String getConnectionAddress() {
        return connectionAddress;
    }

    /**
     * @param aConnectionAddress the connectionAddress to set
     */
    public void setConnectionAddress(String aConnectionAddress) {
        connectionAddress = aConnectionAddress;
    }

    public Msml unmarshalObject(ByteArrayInputStream sdp) {
        Msml msml = objectFactory.createMsml();
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Msml.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            msml = (Msml) unmarshaller.unmarshal(sdp);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return msml;
    }

    /**
     * @return the mediaStatusCode
     */
    public int getMediaStatusCode() {
        return this.mediaStatusCode;
    }

    /**
     * @param mediaStatusCode the mediaStatusCode to set
     */
    public void setMediaStatusCode(int mediaStatusCode) {
        this.mediaStatusCode = mediaStatusCode;
    }

    /**
     * @return the callMode
     */
    public MsmlCallMode getCallMode() {
        return this.callMode;
    }

    /**
     * @param callMode the callMode to set
     */
    public void setCallMode(MsmlCallMode callMode) {
        this.callMode = callMode;
    }
}
