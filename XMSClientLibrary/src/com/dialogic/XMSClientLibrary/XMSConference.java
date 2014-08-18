/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dialogic.XMSClientLibrary;

import java.util.ArrayList;

/**
 * Logging utility.
 */
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
/**
 *
 * @author dslopresti
 * @author dwolanski
 * @author chinck
 */
public abstract class XMSConference extends XMSObject{


    /* Logger information */
     private static Logger m_logger = Logger.getLogger(XMSConference.class.getName());
     
     List<XMSCall> m_partylist=new ArrayList<XMSCall>();
     
     public XMSConferenceOptions ConferenceOptions = new XMSConferenceOptions();
     public XMSPlayOptions PlayOptions = new XMSPlayOptions();
     public XMSAddOptions AddOptions = new XMSAddOptions();
     /**
      * CTor for the Object.  Default takes no parms
      */
     public XMSConference(){
        m_Name = "XMSConference:"+m_objectcounter;
        PropertyConfigurator.configure("log4j.properties");
        //m_logger.setLevel(Level.ALL);
        m_logger.info("Creating " + m_Name);
        m_partylist.clear();
     }
     
     /**
      * Add the Call to the conference
      * @param a_call
      * @return 
      */
     public XMSReturnCode Add( XMSCall a_call){
         return XMSReturnCode.NOT_IMPLEMENTED;
     }
     /**
      * Remove the Call from the conference
      * @param a_call
      * @return 
      */
     public XMSReturnCode Remove( XMSCall a_call){
         return XMSReturnCode.NOT_IMPLEMENTED;
     }
      
     /** 
      * Returns the number of calls in the conference.
      * @return 
      */
     public int GetPartyCount(){
         return m_partylist.size();
     }
     /**
      * Returns the list of the parties currently inside the list
      * @return 
      */
     public List<XMSCall> GetPartyList(){
         return m_partylist;
     }
     /**
      * Plays out a file to the parties currently in the conference
      * @param a_playfile
      * @return 
      */
     //TODO Get Conference play working.  Will need to add all the enablement for the events as well
     public XMSReturnCode Play(String a_playfile){
         return XMSReturnCode.NOT_IMPLEMENTED;
     }
}
