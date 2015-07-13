/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dialogic.XMSClientLibrary;

import com.dialogic.gui.SelectorForm;
import com.dialogic.msml.XMSMsmlCall;
import com.dialogic.msml.XMSMsmlConnector;
import java.io.*;
//import java.util.logging.Level;
//import java.util.logging.Logger;

/**
 * Used for parsing the XML config file
 */
import java.lang.String;
import java.net.Inet4Address;
import java.net.UnknownHostException;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.ParsingException;

/**
 * Logging utility.
 */
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

//TODO Add in a default connector so all you have to do is call the Create without having to pass it in each time
/**
 *
 * @author dwolansk
 */
public class XMSObjectFactory {

    static private Logger logger = Logger.getLogger(XMSObjectFactory.class.getName());
    private String m_Name;
    private static final Object m_synclock = new Object();
    static SelectorForm selector;

    /**
     * CTor for the XMSObjectFactory
     */
    public XMSObjectFactory(){
        m_Name = "XMSObjectfactory";
        PropertyConfigurator.configure("log4j.properties");
        //logger.setLevel(Level.ALL);
        logger.info("Creating " + m_Name);
    }

    /**
     * This function is used when there is no configuration file specified. This
     * creates an user interface to enter the configuration details used to
     * create a connector based on the technology type
     *
     * @return connector
     */
    public XMSConnector CreateConnector() {
        try {
            selector = new SelectorForm();
            synchronized (m_synclock) {
                while (selector.isVisible()) {
                    m_synclock.wait();
                }
            }
        } catch (InterruptedException ex) {
            logger.error(ex);
        }
        XMSConnector connector = CreateConnector("SelectorConfiguration.xml");
        return connector;
    }

    public static void unblock() {
        synchronized (m_synclock) {
            selector.setVisible(false);
            m_synclock.notifyAll();
        }
    }

    /**
     * This function is used to auto detect the technology type and make 
     * an object of the correct type
     *
     * @param a_ConfigFileName
     * @return
     */
    public XMSConnector CreateConnector(String a_ConfigFileName){

        FileInputStream xmlFile = null;
        /**
         * grab the config file
         */
        try {
            xmlFile = new FileInputStream(a_ConfigFileName);
        } catch (FileNotFoundException ex) {
            logger.error(ex);
        }

        /**
         * Set up the XOM library to begin parsing.
         */
        Builder builder = new Builder();
        Document doc=null;

        /**
         * parse the file.
         */
        try {
            doc = builder.build(xmlFile);
        } catch (ParsingException ex) {
            logger.error(ex);
        } catch (IOException ex) {
            logger.error(ex);
        }

        Element root = doc.getRootElement();

        Elements entries = root.getChildElements();

        /**
         * Parse the XMS config file.
         *
         * The routine will read in the confif file and using the keyword
         * "techtype" determine if a REST or MSML connector is needed. 
         * The specific type of connector object will be returned.
         *
         */
        for (int x = 0; x < entries.size(); x++) {

            Element element = entries.get(x);
            //System.out.println(element.getLocalName());
            if (element.getLocalName().equals("techtype") ) {
                String l_techtype = new String();
                l_techtype = element.getValue();
                logger.info("Tech Type is " + l_techtype);
                if (l_techtype.equals("REST")){
                    return new XMSRestConnector(a_ConfigFileName);
                    //return new XMSRestConnector();
                } else if (l_techtype.equals("MSML")){
                    try {
                        // this is local address and port, do we need a config file for this?
                        return new XMSMsmlConnector(a_ConfigFileName, Inet4Address.getLocalHost().getHostAddress(), 5070);
                    } catch (UnknownHostException ex) {
                        logger.fatal(ex.getMessage(), ex);
                    }
                }
            }

        }
        return null;
    }

    /**
     * This is used to create a new XMSCallObject of the correct type.
     * @param a_type
     * @return 
     * WARNING: When using the STRING version of this you must call the
     * XMSCall.Initialize() with the XMSConnector at a later time.
     */
    public XMSCall CreateCall(String a_type){

        if(a_type.equals("REST")){
            logger.info("Creating a REST call Object via String");
            XMSCall l_call= new XMSRestCall();
            return l_call;

        }
        return null;
    }

    /**
     * This is used to create a new XMSCallObject of the correct type and 
     * tie it into the connector
     * @param a_type
     * @return
     */
    public XMSCall CreateCall(XMSConnector a_conn){

        logger.info("a_conn.m_type" + a_conn.m_type);

        if (a_conn.getType().equals("REST")){
            logger.info("Creating a REST Call Object via XMSRestConnector");
            XMSCall l_call = new XMSRestCall(a_conn);
            return l_call;
        } else if (a_conn.getType().equals("MSML")) {
            XMSCall l_call = new XMSMsmlCall((XMSMsmlConnector) a_conn);
            return l_call;
        }
        logger.error("Error detecting the type of the passed XMSConnector");
        return null;
    }

    /**
     * This is used to create a new XMSConference Object of the correct type.
     *
     * @param a_type
     * @return 
     * WARNING: When using the STRING version of this you must call the
     * XMSCall.Initialize() with the XMSConnector at a later time.
     */
    public XMSConference CreateConference(String a_type){

        if(a_type.equals("REST")){
            logger.info("Creating a REST Conference Object via String");
            XMSConference l_conf= new XMSRestConference();
            return l_conf;

        }
        return null;
    }

    /**
     * This is used to create a new XMSCallObject of the correct type and 
     * tie it into the connector
     * @param a_type
     * @return
     */
    public XMSConference CreateConference(XMSConnector a_conn){

        logger.info("a_conn.m_type" + a_conn.m_type);

        if(a_conn.getType().equals("REST")){
            logger.info("Creating a REST Conference Object via XMSRestConnector");
            XMSConference l_conf = new XMSRestConference(a_conn);
            return l_conf;
        }
        logger.error("Error detecting the type of the passed XMSConnector");
        return null;
    }
}
