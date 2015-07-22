/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dialogic.msml;

import com.dialogic.XMSClientLibrary.Layout;
import com.dialogic.XMSClientLibrary.XMSCall;
import com.dialogic.XMSClientLibrary.XMSCallState;
import com.dialogic.XMSClientLibrary.XMSConference;
import com.dialogic.XMSClientLibrary.XMSConferenceOptions;
import com.dialogic.XMSClientLibrary.XMSConnector;
import com.dialogic.XMSClientLibrary.XMSEvent;
import com.dialogic.XMSClientLibrary.XMSEventType;
import com.dialogic.XMSClientLibrary.XMSMediaType;
import com.dialogic.XMSClientLibrary.XMSReturnCode;
import com.dialogic.xms.msml.AudioMixType;
import com.dialogic.xms.msml.BasicAudioMixType;
import com.dialogic.xms.msml.BooleanDatatype;
import com.dialogic.xms.msml.Msml;
import com.dialogic.xms.msml.ObjectFactory;
import com.dialogic.xms.msml.RootType;
import com.dialogic.xms.msml.StreamType;
import com.dialogic.xms.msml.VideoLayoutType;
import com.dialogic.xms.msml.VideoLayoutType.Region;
import static com.dialogic.msml.XMSMsmlCall.logger;
import static com.dialogic.msml.XMSMsmlCall.objectFactory;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.StringWriter;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 *
 * @author ssatyana
 */
public class XMSMsmlConference extends XMSConference implements Observer {

    private static Logger m_logger = Logger.getLogger(XMSMsmlConference.class.getName());

    private XMSMsmlConnector connector;
    XMSSipCall conf;
    static int counter = 0;
    static XMSCallState m_state = XMSCallState.NULL;
    static ObjectFactory objectFactory = new ObjectFactory();
    static int mark = 1;
    static int display = 1;
    String filename;

    public XMSMsmlConference() {
        m_type = "MSML";
        m_Name = "XMSMsmlConference_" + m_objectcounter++;

        PropertyConfigurator.configure("log4j.properties");
        //m_logger.setLevel(Level.ALL);
        m_logger.info("Creating " + m_Name);

    }

    public XMSMsmlConference(XMSConnector a_connector) {
        m_callIdentifier = null;
        m_Name = "XMSConference_" + m_objectcounter++;
        PropertyConfigurator.configure("log4j.properties");
        //m_logger.setLevel(Level.ALL);
        m_logger.info("Creating " + m_Name);
        m_state = XMSCallState.CONF;

        m_type = a_connector.getType();
        connector = (XMSMsmlConnector) a_connector;
        m_connector = a_connector;
        this.filename = connector.getConfigFileName();
        createConf(m_Name);

    }

    public XMSReturnCode createConf(String name) {
        try {

            conf = new XMSSipCall(connector);
            conf.addObserver(this);
            setXMSInfo(conf);
            conf.setFromAddress(Inet4Address.getLocalHost().getHostAddress());
            conf.setLocalSdp(getNullSdp());
            conf.createInviteRequest(conf.getToUser(), conf.getToAddress());
            BlockIfNeeded(XMSEventType.CALL_CONNECTED);
            conf.sendInfo(buildConfMsml(name));
            BlockIfNeeded(XMSEventType.CALL_INFO);
            counter++;
        } catch (Exception ex) {
            m_logger.error(ex.getMessage(), ex);
        }
        return XMSReturnCode.SUCCESS;
    }

    @Override
    public XMSReturnCode Add(XMSCall call) {
        m_partylist.add(call);
        XMSMsmlCall msmlCall = (XMSMsmlCall) call;
        try {
            if (msmlCall != null && msmlCall.msmlSip != null) {
                //msmlCall.msmlSip.sendInfoWithoutConn(buildJoinConfVideoMsml(m_Name));
                msmlCall.msmlSip.sendInfo(buildJoinConfVideoMsml(m_Name));
                counter++;
            }
        } catch (Exception ex) {
            m_logger.error(ex.getMessage(), ex);
        }
        return XMSReturnCode.SUCCESS;
    }

    @Override
    public XMSReturnCode Remove(XMSCall call) {
        m_partylist.remove(call);
        XMSMsmlCall msmlCall = (XMSMsmlCall) call;
        try {
            if (msmlCall != null && msmlCall.msmlSip != null) {
                msmlCall.msmlSip.sendInfo(buildUnJoinMsml(m_Name));
                counter++;
            }
        } catch (Exception ex) {
            m_logger.error(ex.getMessage(), ex);
        }
        return XMSReturnCode.SUCCESS;
    }

    @Override
    public void update(Observable o, Object o1) {
        MsmlEvent e = (MsmlEvent) o1;
        if (e.getType().equals(MsmlEventType.RINGING)) {

        } else if (e.getType().equals(MsmlEventType.CONNECTING)) {

        } else if (e.getType().equals(MsmlEventType.CONNECTED)) {
            XMSEvent xmsEvent = new XMSEvent();
            xmsEvent.CreateEvent(XMSEventType.CALL_CONNECTED, this, "", "", "");
            setLastEvent(xmsEvent);
            UnblockIfNeeded(xmsEvent);
        } else if (e.getType().equals(MsmlEventType.INFORESPONSE)) {
            String reponseMessage = new String(e.getRes().getRawContent());

            if (e.getRes().getRawContent() != null) {
                Msml msml = unmarshalObject(new ByteArrayInputStream((byte[]) e.getRes().getRawContent()));
                Msml.Result result = msml.getResult();
                if (result.getResponse().equalsIgnoreCase("200")) {
                    System.out.println("Response received" + result.getResponse());
                    XMSEvent xmsEvent = new XMSEvent();
                    xmsEvent.CreateEvent(XMSEventType.CALL_INFO, this, result.getResponse(), "", reponseMessage);
                    setLastEvent(xmsEvent);
                    UnblockIfNeeded(xmsEvent);
                } else if (result.getResponse().equalsIgnoreCase("400")) {
                    System.out.println("Response 400 received");
                }
            }
        } else if (e.getType().equals(MsmlEventType.INFOREQUEST)) {
            String info = new String(e.getReq().getRawContent());

            Msml msml = unmarshalObject(new ByteArrayInputStream((byte[]) e.getReq().getRawContent()));
            Msml.Event event = msml.getEvent();
            String eventName = event.getName();
            if (eventName != null && eventName.equalsIgnoreCase("msml.conf.nomedia")) {
                conf.sendInfo(buildDestroyConfMsml(m_Name));
                conf.createBye();
                XMSEvent xmsEvent = new XMSEvent();
                xmsEvent.CreateEvent(XMSEventType.CALL_DISCONNECTED, this, "", "", info);
                setLastEvent(xmsEvent);
            }

        } else if (e.getType().equals(MsmlEventType.DISCONNECTED)) {
            if (counter > 1) {
                counter--;
            }
            if (counter == 1 && (getLastEvent().getEventType() != XMSEventType.CALL_DISCONNECTED)) {
                conf.sendInfo(buildDestroyConfMsml(m_Name));
                conf.createBye();
            }
        }
    }

    private void setXMSInfo(XMSSipCall c) {
        try {
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
            m_logger.error(ex.getMessage(), ex);
        }
    }

    private String getNullSdp() {
        String sdp = "v=0 \n"
                + "o=test 53655765 2353687637 IN IP4 0.0.0.0 \n"
                + "s=test Server \n"
                + "c=IN IP4 0.0.0.0 \n"
                + "t=0 0";
        return sdp;
    }

    // to do other options in msml eg text overlay, caption, beep
    private String buildConfMsml(String name) {
        java.io.StringWriter sw = new StringWriter();

        Msml msml = objectFactory.createMsml();
        msml.setVersion("1.1");

        Msml.Createconference createConf = objectFactory.createMsmlCreateconference();
        createConf.setName(name);
        createConf.setDeletewhen("nomedia");
        createConf.setMark("1");
        createConf.setTerm(BooleanDatatype.TRUE);

        VideoLayoutType videoLayout = objectFactory.createVideoLayoutType();

        RootType rootType = objectFactory.createRootType();
        rootType.setSize("VGA");
        videoLayout.setRoot(rootType);

        videoLayout.getRegion().add(buildRegion("1", "0", "0", "1"));
        createConf.setVideolayout(videoLayout);

        msml.getMsmlRequest().add(createConf);
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Msml.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(msml, sw);

        } catch (JAXBException ex) {
            m_logger.error(ex.getMessage(), ex);
        }

        System.out.println("MSML CONF -> " + sw.toString());
        return sw.toString();
    }

    private String buildJoinConfMsml(String name) {
        java.io.StringWriter sw = new StringWriter();

        Msml msml = objectFactory.createMsml();
        msml.setVersion("1.1");

        Msml.Join join = objectFactory.createMsmlJoin();
        join.setId1("conf:" + name);
        join.setId2("conn:1234");
        join.setMark("2");

        StreamType streamType = objectFactory.createStreamType();
        streamType.setMedia("audio");

        join.getStream().add(streamType);
        msml.getMsmlRequest().add(join);

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Msml.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(msml, sw);

        } catch (JAXBException ex) {
            m_logger.error(ex.getMessage(), ex);
        }

        System.out.println("MSML JOIN -> " + sw.toString());
        return sw.toString();
    }

    private String buildJoinConfVideoMsml(String name) {
        VideoLayoutType videolayout;
        if (ConferenceOptions.m_Layout == Layout.AUTO) {
            List<XMSCall> videoList = new ArrayList<XMSCall>();
            for (XMSCall c : m_partylist) {
                if (c.WaitcallOptions.m_mediatype == XMSMediaType.VIDEO) {
                    videoList.add(c);
                }
            }
            videolayout = getLayout(videoList.size());
        } else {
            videolayout = getLayout(ConferenceOptions.m_Layout.getValue());
        }
        java.io.StringWriter sw = new StringWriter();

        Msml msml = objectFactory.createMsml();
        msml.setVersion("1.1");

        if (videolayout != null) {
            Msml.Modifyconference modifyConf = objectFactory.createMsmlModifyconference();
            modifyConf.setId("conf:" + name);
            modifyConf.setVideolayout(videolayout);

            msml.getMsmlRequest().add(modifyConf);

            msml.getMsmlRequest().add(buildJoin("1234", name, Integer.toString(mark++), Integer.toString(display++)));
        } else {
            msml.getMsmlRequest().add(buildJoinAudio("1234", name, Integer.toString(mark++), Integer.toString(display++)));
        }
        //        int mark = 1;
//        int display = 1;
        //        for (XMSCall c : m_partylist) {
        //            if (c.WaitcallOptions.m_mediatype == XMSMediaType.VIDEO) {
        //                MsmlCall msmlCall = (MsmlCall) c;
        //                msml.getMsmlRequest().add(
        //                        buildJoin(msmlCall.msmlSip.getRemoteTag(), name, Integer.toString(mark++),
        //                                Integer.toString(display++)));
        //            }
        //        }
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Msml.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(msml, sw);

        } catch (JAXBException ex) {
            m_logger.error(ex.getMessage(), ex);
        }

        System.out.println("MSML JOIN -> " + sw.toString());
        return sw.toString();
    }

    private String buildUnJoinMsml(String name) {
        List<XMSCall> videoList = new ArrayList<XMSCall>();
        for (XMSCall c : m_partylist) {
            if (c.WaitcallOptions.m_mediatype == XMSMediaType.VIDEO) {
                videoList.add(c);
            }
        }
        VideoLayoutType videolayout = getLayout(videoList.size());

        java.io.StringWriter sw = new StringWriter();
        Msml msml = objectFactory.createMsml();
        msml.setVersion("1.1");

        Msml.Modifyconference modifyConf = objectFactory.createMsmlModifyconference();
        modifyConf.setId("conf:" + name);
        modifyConf.setVideolayout(videolayout);

        msml.getMsmlRequest().add(modifyConf);

        Msml.Unjoin unjoin = objectFactory.createMsmlUnjoin();
        unjoin.setId1("conn:1234");
        unjoin.setId2("conf:" + name);

        msml.getMsmlRequest().add(unjoin);

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Msml.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(msml, sw);

        } catch (JAXBException ex) {
            m_logger.error(ex.getMessage(), ex);
        }

        System.out.println("MSML UNJOIN -> " + sw.toString());
        return sw.toString();
    }

    private String buildDestroyConfMsml(String name) {
        java.io.StringWriter sw = new StringWriter();

        Msml msml = objectFactory.createMsml();
        msml.setVersion("1.1");

        Msml.Destroyconference destroyConf = objectFactory.createMsmlDestroyconference();
        destroyConf.setId("conf:" + name);
        destroyConf.setMark("1");

        msml.getMsmlRequest().add(destroyConf);
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Msml.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(msml, sw);

        } catch (JAXBException ex) {
            m_logger.error(ex.getMessage(), ex);
        }

        System.out.println("MSML DESTROY CONF -> " + sw.toString());
        return sw.toString();
    }

    public VideoLayoutType getLayout(int num) {
        if (num > 0) {
            VideoLayoutType videoLayout = objectFactory.createVideoLayoutType();

            RootType rootType = objectFactory.createRootType();
            rootType.setSize("VGA");
            videoLayout.setRoot(rootType);
            if (num == 1) {
                videoLayout.getRegion().add(buildRegion("1", "0", "0", "1"));
            } else if (num == 2) {
                videoLayout.getRegion().add(buildRegion("1", "0", "0", "1/2"));
                videoLayout.getRegion().add(buildRegion("2", "50%", "0", "1/2"));
            } else if (num <= 4) {
                videoLayout.getRegion().add(buildRegion("1", "0", "0", "1/2"));
                videoLayout.getRegion().add(buildRegion("2", "50%", "0", "1/2"));
                videoLayout.getRegion().add(buildRegion("3", "0", "50%", "1/2"));
                videoLayout.getRegion().add(buildRegion("4", "50%", "50%", "1/2"));
            } else if (num <= 6) {
                videoLayout.getRegion().add(buildRegion("1", "0", "0", "2/3"));
                videoLayout.getRegion().add(buildRegion("2", "66.666%", "0", "1/3"));
                videoLayout.getRegion().add(buildRegion("3", "66.666%", "33.333%", "1/3"));
                videoLayout.getRegion().add(buildRegion("4", "66.666%", "66.666%", "1/3"));
                videoLayout.getRegion().add(buildRegion("5", "33.333%", "66.666%", "1/3"));
                videoLayout.getRegion().add(buildRegion("6", "0", "66.666%", "1/3"));
            } else if (num <= 9 || num > 9) {
                videoLayout.getRegion().add(buildRegion("1", "0", "0", "1/3"));
                videoLayout.getRegion().add(buildRegion("2", "33.333%", "0", "1/3"));
                videoLayout.getRegion().add(buildRegion("3", "66.666%", "0", "1/3"));
                videoLayout.getRegion().add(buildRegion("4", "0", "33.333%", "1/3"));
                videoLayout.getRegion().add(buildRegion("5", "33.333%", "33.333%", "1/3"));
                videoLayout.getRegion().add(buildRegion("6", "66.666%", "33.333%", "1/3"));
                videoLayout.getRegion().add(buildRegion("7", "0", "66.666%", "1/3"));
                videoLayout.getRegion().add(buildRegion("8", "33.333%", "66.666%", "1/3"));
                videoLayout.getRegion().add(buildRegion("9", "66.666%", "66.666%", "1/3"));
            }
            return videoLayout;
        }
        return null;
    }

    public Region buildRegion(String id, String left, String top, String relativeSize) {
        VideoLayoutType.Region region = objectFactory.createVideoLayoutTypeRegion();
        region.setId(id);
        region.setLeft(left);
        region.setTop(top);
        region.setRelativesize(relativeSize);

        return region;
    }

    public Msml.Join buildJoin(String conn, String name, String mark, String display) {
        Msml.Join join = objectFactory.createMsmlJoin();
        join.setId1("conn:" + conn);
        join.setId2("conf:" + name);
        join.setMark(mark);

        StreamType streamType1 = objectFactory.createStreamType();
        streamType1.setMedia("audio");

        StreamType streamType2 = objectFactory.createStreamType();
        streamType2.setMedia("video");
        streamType2.setDir("from-id1");
        streamType2.setDisplay(display);

        StreamType streamType3 = objectFactory.createStreamType();
        streamType3.setMedia("video");
        streamType3.setDir("to-id1");

        join.getStream().add(streamType1);
        join.getStream().add(streamType2);
        join.getStream().add(streamType3);

        return join;
    }

    public Msml.Join buildJoinAudio(String conn, String name, String mark, String display) {
        Msml.Join join = objectFactory.createMsmlJoin();
        join.setId1("conn:" + conn);
        join.setId2("conf:" + name);
        join.setMark(mark);

        StreamType streamType2 = objectFactory.createStreamType();
        streamType2.setMedia("audio");
        streamType2.setDir("from-id1");
        //streamType2.setEchoCancel("enable");

        StreamType streamType3 = objectFactory.createStreamType();
        streamType3.setMedia("audio");
        streamType3.setDir("to-id1");

        join.getStream().add(streamType2);
        join.getStream().add(streamType3);

        return join;
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
