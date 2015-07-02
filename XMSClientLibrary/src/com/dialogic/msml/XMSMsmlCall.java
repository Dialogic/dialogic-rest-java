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
import com.dialogic.xms.msml.Play.Media;
import com.dialogic.xms.msml.Record;
import com.dialogic.xms.msml.Send;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.UnknownHostException;
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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import org.xml.sax.InputSource;
import org.apache.commons.io.FilenameUtils;

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
    private static MsmlCallMode callMode = MsmlCallMode.OUTBOUND;
    String callerToUserId = null;
    String callerToAdr = null;
    private final Object m_synclock = new Object();
    static boolean isBlocked = false;
    static boolean isACKOn200;
    static boolean isDropCall = false;
    static int mediaStatusCode;
    private static String connectionAddress;
    static XMSEvent xmsEvent;
    static ObjectFactory objectFactory = new ObjectFactory();

    public XMSMsmlCall(XMSMsmlConnector connector) {
        try {
            this.connector = connector;
            m_connector = connector;
            this.setFromAddress(Inet4Address.getLocalHost().getHostAddress());
            setState(XMSCallState.NULL);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    @Override
    public XMSReturnCode Waitcall() {
        try {
            caller = new XMSSipCall(this.connector);
            caller.addObserver(this);
            callMode = MsmlCallMode.INBOUND;
            setState(XMSCallState.WAITCALL);
            caller.addToWaitList();
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

    public XMSReturnCode waitCallAsyn() {
        caller = new XMSSipCall(this.connector);
        caller.addObserver(this);
        callMode = MsmlCallMode.INBOUND;
        m_state = XMSCallState.WAITCALL;
        caller.addToWaitList();
        return XMSReturnCode.SUCCESS;
    }

    @Override
    public XMSReturnCode Makecall(String dest) {
        try {
            setState(XMSCallState.MAKECALL);
            MakecallOptions.setCalledAddress(dest);
            if (!dest.isEmpty()) {
                String[] params = dest.split("@");
                if (params.length != 0) {
                    callerToUserId = params[0];
                    callerToAdr = params[1];
                }
            }

            if (isDropCall) {
                // reset                 
                this.callMode = MsmlCallMode.OUTBOUND;
                msmlSip = null;
                caller = null;
                MakecallOptions.EnableACKOn200(false);
                MakecallOptions.EnableOKOnInfo(false);
                isDropCall = false;
            }
            if (msmlSip == null) {
                msmlSip = new XMSSipCall(this.connector);
                msmlSip.addObserver(this);

                // get XMS ip address and user from config file
                setXMSInfo(msmlSip);
                msmlSip.setFromAddress(Inet4Address.getLocalHost().getHostAddress());
                if (caller != null && caller.getRemoteSdp() != null) {
                    msmlSip.setLocalSdp(caller.getRemoteSdp());
                }
                if (!MakecallOptions.m_ACKOn200Enabled) {
                    msmlSip.setACKOn200(Boolean.FALSE);
                }
                if (!MakecallOptions.m_OKOnInfoEnabled) {
                    msmlSip.setOKOnInfo(Boolean.FALSE);
                }
                if (!MakecallOptions.m_sdp.isEmpty()) {
                    msmlSip.setLocalSdp(MakecallOptions.m_sdp);
                }
                msmlSip.createInviteRequest(msmlSip.getToUser(), msmlSip.getToAddress());

                if (caller == null) {
                    BlockIfNeeded(XMSEventType.CALL_CONNECTED);
                }
            } else if (caller == null && msmlSip != null) {
                Thread.sleep(500);
                caller = new XMSSipCall(this.connector);
                caller.addObserver(this);
                caller.setFromAddress(Inet4Address.getLocalHost().getHostAddress());
                caller.setLocalSdp(msmlSip.getRemoteSdp());
                caller.createInviteRequest(callerToUserId, callerToAdr);
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return XMSReturnCode.SUCCESS;
    }

    @Override
    public XMSReturnCode Acceptcall() {
        try {
            if (caller != null) {
                caller.createRingingResponse(caller.getInviteRequest());
                if (!WaitcallOptions.m_autoConnectEnabled) {
                    BlockIfNeeded(XMSEventType.CALL_UPDATED);
                }
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return XMSReturnCode.SUCCESS;
    }

    @Override
    public XMSReturnCode Answercall() {
        try {
            if (caller != null) {
                caller.createInviteOk(caller.getInviteRequest());
                if (!WaitcallOptions.m_autoConnectEnabled) {
                    BlockIfNeeded(XMSEventType.CALL_CONNECTED);
                }
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return XMSReturnCode.SUCCESS;
    }

    @Override
    public XMSReturnCode Dropcall() {
        try {
            if (caller != null && msmlSip != null) {
                isDropCall = true;
                caller.createBye();
                msmlSip.createBye();
                BlockIfNeeded(XMSEventType.CALL_DISCONNECTED);
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return XMSReturnCode.SUCCESS;
    }

    @Override
    public XMSReturnCode Play(String filename) {
        try {
            if (msmlSip != null && filename != null) {
                msmlSip.sendInfo(buildPlayMsml(filename));
                setState(XMSCallState.PLAY);
                BlockIfNeeded(XMSEventType.CALL_INFO);
                if (mediaStatusCode == 200) {
                    BlockIfNeeded(XMSEventType.CALL_PLAY_END);
                }
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return XMSReturnCode.SUCCESS;
    }

    @Override
    public XMSReturnCode Record(String filename) {
        try {
            if (msmlSip != null && filename != null) {
                msmlSip.sendInfo(buildRecordMsml(filename));
                setState(XMSCallState.RECORD);
                BlockIfNeeded(XMSEventType.CALL_INFO);
                if (mediaStatusCode == 200) {
                    BlockIfNeeded(XMSEventType.CALL_RECORD_END);
                }
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return XMSReturnCode.SUCCESS;
    }

    @Override
    public XMSReturnCode CollectDigits() {
        try {
            if (msmlSip != null) {
                msmlSip.sendInfo(buildCollectMsml());
                setState(XMSCallState.COLLECTDIGITS);
                BlockIfNeeded(XMSEventType.CALL_INFO);
                if (mediaStatusCode == 200) {
                    BlockIfNeeded(XMSEventType.CALL_COLLECTDIGITS_END);
                }
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return XMSReturnCode.SUCCESS;
    }

    @Override
    public XMSReturnCode PlayCollect(String filename) {
        try {
            if (msmlSip != null && filename != null) {
                msmlSip.sendInfo(buildPlayCollectMsml(filename));
                setState(XMSCallState.PLAYCOLLECT);
                synchronized (m_synclock) {
                    while (!isBlocked) {
                        m_synclock.wait();
                    }
                    isBlocked = false;
                    // waiting for digits
                    synchronized (m_synclock) {
                        while (!isBlocked) {
                            m_synclock.wait();
                        }
                        isBlocked = false;
                    }
                }
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return XMSReturnCode.SUCCESS;
    }

    @Override
    public XMSReturnCode SendInfo(String msml) {
        try {
            if (msmlSip != null && msml != null) {
                m_state = XMSCallState.CUSTOM;
                msmlSip.sendInfo(msml);
                BlockIfNeeded(XMSEventType.CALL_INFO);
                if (mediaStatusCode == 200) {
                    BlockIfNeeded(XMSEventType.CALL_INFO);
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

    @Override
    public void update(Observable o, Object o1) {
        MsmlEvent e = (MsmlEvent) o1;
        if (e.getType().equals(MsmlEventType.INCOMING)) {
            if (this.callMode == MsmlCallMode.INBOUND) {

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
            } else if (this.callMode == MsmlCallMode.OUTBOUND) {
            }
        } else if (e.getType().equals(MsmlEventType.RINGING)) {
            if (this.callMode == MsmlCallMode.INBOUND) {
                if (WaitcallOptions.m_autoConnectEnabled) {
                    Acceptcall();
                } else {
                    setState(XMSCallState.OFFERED);
                    XMSEvent xmsEvent = new XMSEvent();
                    xmsEvent.CreateEvent(XMSEventType.CALL_OFFERED, this, "", "", "");
                    UnblockIfNeeded(xmsEvent);
                }

            } else if (this.callMode == MsmlCallMode.OUTBOUND) {
            }
        } else if (e.getType().equals(MsmlEventType.CONNECTING)) {
            if (this.callMode == MsmlCallMode.INBOUND) {
                if (WaitcallOptions.m_autoConnectEnabled) {
                    if (caller.getLocalSdp() == null) {
                        caller.setLocalSdp(msmlSip.getRemoteSdp());
                    }
                    Answercall();
                } else {
                    if (caller.getLocalSdp() == null) {
                        caller.setLocalSdp(msmlSip.getRemoteSdp());
                    }
                    setState(XMSCallState.ACCEPTED);
                    XMSEvent xmsEvent = new XMSEvent();
                    xmsEvent.CreateEvent(XMSEventType.CALL_UPDATED, this, "", "", "");
                    UnblockIfNeeded(xmsEvent);
                }
            } else if (this.callMode == MsmlCallMode.OUTBOUND) {
                if (e.getCall() == caller) {
                    msmlSip.createAckRequest(e.getRes());
                    caller.createAckRequest(e.getRes());
                } else if (caller == null) {
                    if (callerToAdr != msmlSip.getToAddress()) {
                        Makecall(callerToUserId + "@" + callerToAdr);
                    }
                }
            } else {
                synchronized (m_synclock) {
                    isBlocked = true;
                    m_synclock.notifyAll();
                }
            }
        } else if (e.getType().equals(MsmlEventType.CONNECTED)) {
            // recieved ack from inbound call and its connected
            if (this.callMode == MsmlCallMode.INBOUND) {
                if (e.getCall() == caller) {
                    setState(XMSCallState.CONNECTED);
                    XMSEvent xmsEvent = new XMSEvent();
                    xmsEvent.CreateEvent(XMSEventType.CALL_CONNECTED, this, "", "", "");
                    UnblockIfNeeded(xmsEvent);
                }
            } else if (this.callMode == MsmlCallMode.OUTBOUND) {
                if (e.getCall() == caller) {
                    setState(XMSCallState.CONNECTED);
                    XMSEvent xmsEvent = new XMSEvent();
                    xmsEvent.CreateEvent(XMSEventType.CALL_CONNECTED, this, "", "", "");
                    UnblockIfNeeded(xmsEvent);
                }
            }
        } else if (e.getType().equals(MsmlEventType.INFORESPONSE)) {
            if (e.getCall() == msmlSip) {
                String reponseMessage = new String(e.getRes().getRawContent());
                if (getState() == XMSCallState.CUSTOM) {
                    XMSEvent xmsEvent = new XMSEvent();
                    xmsEvent.CreateEvent(XMSEventType.CALL_INFO, this, "", "", reponseMessage);
                    setLastEvent(xmsEvent);
                } else if (getState() != XMSCallState.DISCONNECTED) {
                    if (e.getRes().getRawContent() != null) {
                        Msml msml = unmarshalObject(new ByteArrayInputStream((byte[]) e.getRes().getRawContent()));
                        Msml.Result result = msml.getResult();
                        if (result.getResponse().equalsIgnoreCase("200")) {
                            mediaStatusCode = Integer.parseInt(result.getResponse());
                            System.out.println("jaxb" + result.getResponse());
                            XMSEvent xmsEvent = new XMSEvent();
                            xmsEvent.CreateEvent(XMSEventType.CALL_INFO, this, result.getResponse(), "", reponseMessage);
                            UnblockIfNeeded(xmsEvent);
                        } else if (result.getResponse().equalsIgnoreCase("400")) {
                            System.out.println("Response 400 received");
                            mediaStatusCode = Integer.parseInt(result.getResponse());
                        }
                    }
                }
            } else if (e.getCall() == caller) {

            }
        } else if (e.getType().equals(MsmlEventType.INFOREQUEST)) {
            if (!MakecallOptions.m_OKOnInfoEnabled) {
                msmlSip.createInfoResponse(e.getReq());
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
                } else if (mediaControl != null) {
                    if (caller != null) {
                        caller.sendInfoWithoutConn(info);
                    }
                    xmsEvent = new XMSEvent();
                    xmsEvent.CreateEvent(XMSEventType.CALL_INFO, this, "", "", info);
                    setLastEvent(xmsEvent);

                } else {
                    Msml msml = unmarshalObject(new ByteArrayInputStream((byte[]) e.getReq().getRawContent()));
                    Msml.Event event = msml.getEvent();
                    String eventName = event.getName();
                    //get the id
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
                        // check if the event if for play                        
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
                            UnblockIfNeeded(xmsEvent);
                        } else {
                            msmlSip.createBye();
                        }
                    }
                }
            }
        } else if (e.getType().equals(MsmlEventType.DISCONNECTED)) {
            if (e.getCall() == msmlSip) {
                if (isDropCall) {
                    isDropCall = false;
                    this.callMode = MsmlCallMode.OUTBOUND;
                    msmlSip = null;
                    caller = null;
                    MakecallOptions.EnableACKOn200(false);
                    MakecallOptions.EnableOKOnInfo(false);
                    setState(XMSCallState.DISCONNECTED);
                    XMSEvent xmsEvent = new XMSEvent();
                    xmsEvent.CreateEvent(XMSEventType.CALL_DISCONNECTED, this, "", "", "");
                    UnblockIfNeeded(xmsEvent);
                } else if (getState() != XMSCallState.DISCONNECTED) {
                    if (e.getReq() != null) {
                        setState(XMSCallState.DISCONNECTED);
                        msmlSip.doByeOk(e.getReq());
                        caller.createBye();
                    }
                }
            } else if (e.getCall() == caller) {
                if (getState() != XMSCallState.DISCONNECTED) {
                    if (getState() == XMSCallState.PLAY) {
                        //media active, send dialog exit before bye
                        msmlSip.createDialogEndRequest(buildDialogExit("Play"));
                        setState(XMSCallState.DISCONNECTED);
                        caller.doByeOk(e.getReq());
                    } else if (getState() == XMSCallState.RECORD) {
                        msmlSip.createDialogEndRequest(buildDialogExit("Record"));
                        setState(XMSCallState.DISCONNECTED);
                        caller.doByeOk(e.getReq());
                    } else {
                        if (e.getReq() != null) {
                            setState(XMSCallState.DISCONNECTED);
                            caller.doByeOk(e.getReq());
                            msmlSip.createBye();
                        }
                    }
                }
            }
        } else if (e.getType().equals(MsmlEventType.CANCEL)) {
            if (e.getCall() == caller) {
                msmlSip.createCancelRequest();
                caller.createCancelResponse(e.getReq());
            }

        }
    }

    // change to xml beans/jaxb objects
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
        audio.setUri(fileName + ".wav");

        audio.setFormat("audio/wav;codec=L16");
        audio.setAudiosamplerate(BigInteger.valueOf(16000));
        audio.setAudiosamplesize(BigInteger.valueOf(16));
        media.getAudioOrVideo().add(audio);

        Play.Media.Video video = null;
        if (PlayOptions.m_mediaType == XMSMediaType.VIDEO) {
            video = objectFactory.createPlayMediaVideo();
            video.setUri(fileName + ".vid");
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
            record.setVideodest(fileName + ".vid");
            record.setFormat("video/x-vid");
        }

        if (fileName != null && !fileName.isEmpty()) {
            record.setAudiodest(fileName + ".wav");
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

    private static String buildPlayCollectMsml(String filename) {
        String msml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                + "<msml version=\"1.1\">\n"
                + "<dialogstart target=\"conn:1234\" type=\"application/moml+xml\">\n"
                + "<group topology=\"parallel\">\n"
                + "<play barge=\"true\" cleardb=\"true\">\n"
                + "	<audio uri=\"" + filename + "\"/>\n"
                + " <playexit>\n"
                + "	 <send target=\"collect\" event=\"starttimer\"/>\n"
                + " </playexit>\n"
                + "</play>\n"
                + "<collect fdt=\"3s\" idt=\"10s\">\n"
                + "	<pattern digits=\"x\">\n"
                + "		<send target=\"source\" event=\"match\" namelist=\"dtmf.end dtmf.digits\"/>\n"
                + "	</pattern>\n"
                + "	<detect>\n"
                + "		<send target=\"source\" event=\"detect\" namelist=\"dtmf.end dtmf.digits\"/>\n"
                + "	</detect>\n"
                + "	<noinput>\n"
                + "		<send target=\"source\" event=\"noinput\" namelist=\"dtmf.end dtmf.digits\"/>\n"
                + "	</noinput>\n"
                + "	<nomatch>\n"
                + "		<send target=\"source\" event=\"nomatch\" namelist=\"dtmf.end dtmf.digits\"/>	\n"
                + "	</nomatch>\n"
                + "</collect>\n"
                + "</group>\n"
                + "</dialogstart>\n"
                + "</msml>";
        return msml;
    }

    private void setXMSInfo(XMSSipCall c) {
        try {
            FileInputStream xmlFile = new FileInputStream("ConnectorConfig.xml");
            Document doc = new Builder().build(xmlFile);
            Element root = doc.getRootElement();
            Elements entries = root.getChildElements();
            for (int x = 0; x < entries.size(); x++) {
                Element element = entries.get(x);
                if (element.getLocalName().equals("user")) {
                    c.setToUser(element.getValue());
                }
                if (element.getLocalName().equals("xmsAddress")) {
                    c.setToAddress(element.getValue());

                }
            }
        } catch (Exception ex) {
            Logger.getLogger(XMSSipCall.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void setAckOn200(Boolean value) {
        this.isACKOn200 = value;
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
}
