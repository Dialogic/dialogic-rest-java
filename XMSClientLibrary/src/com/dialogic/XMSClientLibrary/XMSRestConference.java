/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dialogic.XMSClientLibrary;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Observable;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.xmlbeans.XmlNMTOKEN;

import com.dialogic.xms.*;
import com.dialogic.xms.EventDataDocument.*;


/**
 *
 * @author dwolansk
 */
public class XMSRestConference extends XMSConference{
    
     private static Logger m_logger = Logger.getLogger(XMSRestConference.class.getName());
    /* Logger information */
     
     
     private XMSRestPendingTransactionInfo m_pendingtransactionInfo=new XMSRestPendingTransactionInfo();
     
     private XMSRestConnector m_restconnector;
     
  
     public XMSRestConference(){
        m_type = "REST";
        m_Name = "XMSRestConference:"+m_objectcounter++;

        PropertyConfigurator.configure("log4j.properties");
        //m_logger.setLevel(Level.ALL);
        m_logger.info("Creating " + m_Name);
        
        
     }
  /**
      * CTor for the REST call object.  THis will both create and tie
      * the object into the XMSConnector
      * @param a_connector 
      */
    
  
           /**
      * CTor for the REST call object.  THis will both create and tie
      * the object into the XMSConnector
      * @param a_connector 
      */
    public XMSRestConference(XMSConnector a_connector){
        m_callIdentifier=null;
        m_Name = "XMSConference:"+m_objectcounter++;
        PropertyConfigurator.configure("log4j.properties");
        //m_logger.setLevel(Level.ALL);
        m_logger.info("Creating " + m_Name);
        
        m_type = a_connector.getType();
        m_restconnector = (XMSRestConnector)a_connector;
        m_connector = a_connector;
        Initialize();
        m_logger.info("Adding Myself as an Observer");
        this.addObserver(this);
        
     }  
    
    public XMSReturnCode Create(){
        FunctionLogger logger=new FunctionLogger("Create",this,m_logger);
        logger.args("ConferenceOptions="+ConferenceOptions); 
        
            logger.info("There is not CallIdentifier, makeing a new conference before proceeding");
            String XMLPAYLOAD;
            SendCommandResponse RC ;

            
            XMLPAYLOAD = buildCreateConferencePayload(); 
            
            
            /*
            XMLPAYLOAD = "<web_service version=\"1.0\"> "+
             " <conference "+
                    " layout=\"0\""+
            " >"+
            " </conference>"+
            " </web_service>";
            * 
            */
            RC = m_connector.SendCommand(this,RESTOPERATION.POST, "conferences", XMLPAYLOAD);
        
            if (RC.get_scr_status_code() == 201){
                logger.info("Conference Create Success, ID: " + RC.get_scr_identifier());
            } else {
                
                logger.info("Conference Create Failed, Status Code: " + RC.get_scr_status_code());
                return XMSReturnCode.FAILURE;

            }
        
        
        
        return XMSReturnCode.SUCCESS;
    }
        
    @Override
    public XMSReturnCode Add(XMSCall a_call){
        FunctionLogger logger=new FunctionLogger("Add",this,m_logger);
        logger.args("Call="+a_call+"  ConferenceOptions="+ConferenceOptions); 
        if(this.getCallIdentifier() == null){
            if(Create() == XMSReturnCode.FAILURE){
                return XMSReturnCode.FAILURE;
            }

        }
        if(a_call instanceof  XMSRestCall){
            if(((XMSRestCall)a_call).AddToConference(this) == XMSReturnCode.FAILURE){
                return XMSReturnCode.FAILURE;
            }
            m_partylist.add(a_call);
        } else {
            return XMSReturnCode.FAILURE;
        }
        return XMSReturnCode.SUCCESS;
    }
    
    @Override
    public XMSReturnCode Remove(XMSCall a_call){
      FunctionLogger logger=new FunctionLogger("Remove",this,m_logger);
        logger.args("Call="+a_call); 
        
        if(a_call instanceof  XMSRestCall){
            if(((XMSRestCall)a_call).RemoveFromConference(this) == XMSReturnCode.FAILURE){
                return XMSReturnCode.FAILURE;
            }
            m_partylist.remove(a_call);
        } else {
            return XMSReturnCode.FAILURE;
        }
        //TODO put in Remove when call list gets to 0 in conference
        return XMSReturnCode.SUCCESS;
    

    } // end remove

  /**
     * Play a file
     * @param a_filelist - A list of Files to be played
     * @return
     */
    
    @Override
     public XMSReturnCode Play(String a_file){
        FunctionLogger logger=new FunctionLogger("PlayList",this,m_logger);
        logger.args("Playfile "+a_file+" " + PlayOptions);
        String XMLPAYLOAD;
        SendCommandResponse RC ;

        
        String l_urlext;
        l_urlext = "conferences/" + m_callIdentifier;
        
       
        ArrayList<String> l_playlist = new ArrayList<String>();
        l_playlist.add(a_file);
        
        XMLPAYLOAD = buildPlayPayload(l_playlist); 

        //logger.info("Sending message ---->  " + XMLPAYLOAD);
        RC = m_connector.SendCommand(this,RESTOPERATION.PUT, l_urlext, XMLPAYLOAD);
        
         if (RC.get_scr_status_code() == 200){
            m_pendingtransactionInfo.setDescription("Play file(s)="+l_playlist);
            m_pendingtransactionInfo.setTransactionId(RC.get_scr_transaction_id());
            m_pendingtransactionInfo.setResponseData(RC);
            
            //setState(XMSCallState.PLAY);
                    try {
                        BlockIfNeeded(XMSEventType.CALL_PLAY_END);
                    } catch (InterruptedException ex) {
                        logger.error("Exception:"+ex);
                    }

                    return XMSReturnCode.SUCCESS;

        } else {

            logger.info("Play Failed, Status Code: " + RC.get_scr_status_code());
            
            //setState(XMSCallState.NULL);
            return XMSReturnCode.FAILURE;

        }
        
    } // end play
    
      /**
     * This is the Notify handler that will be called by EventThread when
     * new events are created.
     * @param obj
     * @param arg 
     */
    @Override
    public void update(Observable obj, Object arg) {
        FunctionLogger logger=new FunctionLogger("update",this,m_logger);
        logger.args("obj="+obj+" arg="+arg);
        XMSEvent l_callbackevt = new XMSEvent();
        if (arg instanceof XMSRestEvent){
            XMSRestEvent l_evt=(XMSRestEvent) arg;
            //TODO This Method is getting a little combersome, may want to extend out to private OnXXXXEvent functions to segment the readability.
            //TODO Should we support the Ringing event?
                if (l_evt.eventType.contentEquals("end_play")) {
                    logger.info("Processing play end event");
                   m_pendingtransactionInfo.Reset();
                    //setState(XMSCallState.CONNECTED);
                    EventData[] l_datalist=l_evt.event.getEventDataArray();
                        for(EventDataDocument.EventData ed: l_datalist){
                            if (ed.getName().contentEquals(EventDataName.REASON.toString())){
                                l_callbackevt.setReason(ed.getValue());
                            }
                        }
                    l_callbackevt.CreateEvent(XMSEventType.CALL_PLAY_END, this, "", "", l_evt.toString());
                    UnblockIfNeeded(l_callbackevt);
                    //end end_play
                
                }else {
                    logger.info("Unprocessed event type: " + l_evt.eventType);
                }
        }                     
    }

    
    // Building Methods
        /**
     * Build the payload string to make a new conference
     * 
     * 
     * @return Payload string  
     * 
     */
    
    private String buildCreateConferencePayload() {

        FunctionLogger logger=new FunctionLogger("buildCreateConferencePayload",this,m_logger);
        String l_rqStr = "";

        WebServiceDocument l_WMSdoc;
        WebServiceDocument.WebService l_WMS;
        XmlNMTOKEN  l_ver; 

        // Create a new Web Service Doc Instance
        l_WMSdoc = WebServiceDocument.Factory.newInstance();
        l_WMS = l_WMSdoc.addNewWebService();


        // Create a new XMLToken Instance
        l_ver = XmlNMTOKEN.Factory.newInstance();
        l_ver.setStringValue("1.0");
        l_WMS.xsetVersion(l_ver);
        
        // Create a conf instance
        ConferenceDocument.Conference l_conf;
        
        
        l_conf = l_WMS.addNewConference();
         
        l_conf.setMaxParties(ConferenceOptions.m_MaxParties);
    
        l_conf.setLayout(Integer.toString(ConferenceOptions.m_Layout.getValue()));
        
        if(ConferenceOptions.m_CaptionEnabled) {
            
            l_conf.setCaption(BooleanType.YES);
        } else {
            l_conf.setCaption(BooleanType.NO);
            
        } // end if
    //TODO get setCaptionDuration working
    l_conf.setCaptionDuration(""+ConferenceOptions.m_CaptionDuration);
    
     if(ConferenceOptions.m_BeepEnabled) {
            
            l_conf.setBeep(BooleanType.YES);
        } else {
            l_conf.setBeep(BooleanType.NO);
            
        } // end if
     if(ConferenceOptions.m_DigitClampingEnabled) {
            
            l_conf.setClampDtmf(BooleanType.YES);
        } else {
            l_conf.setClampDtmf(BooleanType.NO);
            
        } // end if
    
    
    
    
        if(ConferenceOptions.m_AGCEnabled) {
            
            l_conf.setAutoGainControl(BooleanType.YES);
        } else {
            l_conf.setAutoGainControl(BooleanType.NO);
            
        } // end if
        
        if(ConferenceOptions.m_ECEnabled) {
            
            l_conf.setEchoCancellation(BooleanType.YES);
        } else {
            l_conf.setEchoCancellation(BooleanType.NO);
            
        } // end if

        // Set Media getType parm
        switch (ConferenceOptions.m_MediaType) {
            case AUDIO:
                l_conf.setType(MediaType.AUDIO);
                //l_conf.setMedia(MediaType.AUDIO);
                break;
            case VIDEO:
                l_conf.setType(MediaType.AUDIOVIDEO);
              //  l_conf.setMedia(MediaType.AUDIOVIDEO);
                break;
            case UNKNOWN:
                l_conf.setType(MediaType.UNKNOWN);
                //l_conf.setMedia(MediaType.UNKNOWN);
        } // end switch


        //logger.debug("RAW REST generated...." + l_WMS.toString());
          ByteArrayOutputStream l_newDialog = new ByteArrayOutputStream();

        try {
            l_WMSdoc.save(l_newDialog);
            l_rqStr = l_WMSdoc.toString();

            } catch (IOException ex) {
            logger.error(ex);
        }

        //logger.debug ("Returning Payload:\n " + l_rqStr);
        return l_rqStr;  // Return the requested string...


    } // end buildMakecallPayload
    
      /**
     * CLASS TYPE   :   private
     * METHOD       :   buildPlayPayload
     *
     * DESCRIPTION  :   Build the payload string to play a file(s). Supports multiple files
     *
     * PARAMETERS   :   Array List <String> = playlist
     *
     *
     * RETURN       :   Payload string
     *
     * Author(s)    :   r.moses
     * Created      :   21-May-2012
     * Updated      :   31-May-2012
     *
     *
     * HISTORY      :
     *************************************************************************/
    private String buildPlayPayload(ArrayList <String> a_playlist) {
        FunctionLogger logger=new FunctionLogger("buildPlayPayload",this,m_logger);
        String l_rqStr = "";
        String uriString = "";

        WebServiceDocument l_WMSdoc;
        WebServiceDocument.WebService l_WMS;

        XmlNMTOKEN  l_ver;

        
        ConferenceDocument.Conference l_conf;
        ConfActionDocument.ConfAction l_confAction;
        //CallActionDocument.CallAction l_callAction; // Call Action instance

        PlayDocument.Play l_play;
        PlaySourceDocument.PlaySource l_playSource;

        // Create a new Web Service Doc Instance
        l_WMSdoc = WebServiceDocument.Factory.newInstance();
        l_WMS = l_WMSdoc.addNewWebService();

        // Create a new XMLToken Instance
        l_ver = XmlNMTOKEN.Factory.newInstance();
        l_ver.setStringValue("1.0");
        l_WMS.xsetVersion(l_ver);

        // add a new call
        l_conf = l_WMS.addNewConference();

        // Add a new Call Action to the call
        l_confAction = l_conf.addNewConfAction();

        // Add a new Play to the callAction
        l_play = l_confAction.addNewPlay();

        // Add the play properties
        // Hard code these for now..
        l_play.setOffset(PlayOptions.m_offset); // Hard code this for now..
        l_play.setDelay(PlayOptions.m_delay); // Hard code this for now..
        l_play.setRepeat(PlayOptions.m_repeat); // Hard code this for now..

        l_play.setTerminateDigits(PlayOptions.m_terminateDigits); // Hard code this for now..
        //l_play.setTransactionId(getCallIdentifier()+"_"+m_transactionId++); // Hard code this for now..
        
        // Setup your play list and put it in playsource
        for (int i = 0; i < a_playlist.size(); i++) {
//                logger.debug("playList entry["+i+"] - " + uriString);
                
                // Add a new playsource to the play
                l_playSource = l_play.addNewPlaySource();

                uriString = (a_playlist.get(i));
                 String l_uristring=uriString;
                 
                 if( this.PlayOptions.m_mediaType == XMSMediaType.IMAGE){
                    if(!l_uristring.startsWith("image:")){
                        l_uristring="image:"+l_uristring;
                    }
                    l_playSource.setLocation(l_uristring);
                }
                 
                 else{
                if(uriString.toLowerCase().endsWith(".wav") || uriString.toLowerCase().endsWith(".vid")){
                    l_uristring=uriString.substring(0, uriString.length()-4);
                }
                // TO DO: May need to append the MediaDefaultDirectory

                l_playSource.setLocation("file://"+l_uristring);
  //              logger.debug("Added [" + uriString + "]");
                 }
        }

        //logger.debug("RAW REST generated...." + l_WMS.toString());
        ByteArrayOutputStream l_newDialog = new ByteArrayOutputStream();

        try {
            l_WMSdoc.save(l_newDialog);
            l_rqStr = l_WMSdoc.toString();

            } catch (IOException ex) {
            logger.error(ex);
        }

        //logger.debug ("Returning Payload:\n " + l_rqStr);
        return l_rqStr;  // Return the requested string...
    } // end buildPlayPayload


}
