/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dialogic.XMSClientLibrary;
/**
 * Logging utility.
 */
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

// Java Files..
import java.util.*;
import java.io.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import javax.xml.namespace.QName;
import java.util.Observable;
import java.util.Observer;  /* this is Event Handler */
import java.util.List;

//regex
import java.util.regex.*;

// XML Beans
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlNMTOKEN;
import org.apache.xmlbeans.XmlOptions;

// REST
import com.dialogic.xms.*;
import com.dialogic.xms.EventDocument.*;
import com.dialogic.xms.EventDataName.*;
import com.dialogic.xms.EventDataDocument.*;

import com.dialogic.xms.CallDocument.Call;
import com.dialogic.xms.CallResponseDocument.CallResponse;
import com.dialogic.xms.CallActionDocument.CallAction;

import com.dialogic.xms.PlaySourceDocument.PlaySource;
import com.dialogic.xms.PlayDocument.Play;
import com.dialogic.xms.PlaycollectDocument.Playcollect;
import com.dialogic.xms.PlayrecordDocument;

import com.dialogic.xms.JoinDocument.Join;
import com.dialogic.xms.UnjoinDocument.Unjoin;

import com.dialogic.xms.StopDocument.Stop;

import com.dialogic.xms.AddPartyDocument.AddParty;
import com.dialogic.xms.RemovePartyDocument.RemoveParty;
import com.dialogic.xms.MediaDirection;

import com.dialogic.xms.EventDocument;
import com.dialogic.xms.EventDataDocument;
import com.dialogic.xms.EventDataName;


/**
 *
 * @author dwolansk
 */
public class XMSRestCall extends XMSCall{
    
     private static Logger m_logger = Logger.getLogger(XMSRestCall.class.getName());

     private XMSRestPendingTransactionInfo m_pendingtransactionInfo=new XMSRestPendingTransactionInfo();
     //int m_transactionId=0;
     private XMSRestConnector m_restconnector;
     
  
     
     /**
      * Default Constructor.  
      * Warning, if you use this you will need to call Initialzie with the 
      * XMSConnectoer before using the object
      */
     public XMSRestCall(){
        m_type = "REST";
        m_Name = "XMSRestCall:"+m_objectcounter++;

        PropertyConfigurator.configure("log4j.properties");
       // m_logger.setLevel(Level.ALL);
        m_logger.info("Creating " + m_Name);
        m_state = XMSCallState.NULL;
     }
/**
      * CTor for the REST call object.  THis will both create and tie
      * the object into the XMSConnector
      * @param a_connector 
      */
    public XMSRestCall(XMSConnector a_connector){
        m_Name = "XMSCall:"+m_objectcounter++;
        PropertyConfigurator.configure("log4j.properties");
        //m_logger.setLevel(Level.ALL);
        m_logger.info("Creating " + m_Name);
        m_state = XMSCallState.NULL;
        m_type = a_connector.getType();
        m_restconnector = (XMSRestConnector)a_connector;
        m_connector = a_connector;
        Initialize();
        m_logger.info("Adding Myself as an Observer");
        this.addObserver(this);
        
     }
    private void SetPendingTransactionInfo(){
        
    }
    protected XMSCallType getCallTypeFromURI(String a_uri){
        FunctionLogger logger=new FunctionLogger("getCallTypeFromURI",this,m_logger);
        logger.args(a_uri);
        
        
        if(a_uri.contains("rtc:")){
            logger.info(a_uri + " contains rtc: setting call type to WEBRTC");
            return XMSCallType.WEBRTC;
        } else if (a_uri.contains("sip:")){
            logger.info(a_uri + " contains sip: setting call type to SIP");
            return XMSCallType.SIP;
        } else {
            logger.info(a_uri + " does not include sip: or rtc: defaulting to call type to SIP");
            return XMSCallType.SIP;
        }
    }
    /**
     * Wait for a new REST call 
     * @return 
     * Event - This will return a CALL_CONNECTED if in AutoConnect mode, CALL_OFFERED if not.
     */
    @Override
    public XMSReturnCode Waitcall(){
        FunctionLogger logger=new FunctionLogger("Waitcall",this,m_logger);
        logger.args(WaitcallOptions);
        
        m_connector.AddCallToWaitCallList(this);
        
        setState(XMSCallState.WAITCALL);
        try {
            if(WaitcallOptions.isAutoConnect()){
                BlockIfNeeded(XMSEventType.CALL_CONNECTED);
            } else {
                BlockIfNeeded(XMSEventType.CALL_OFFERED);
            }
            
        } catch (InterruptedException ex) {
            System.out.println("Got the Exception");
        }
        
        return XMSReturnCode.SUCCESS;
    }
    /**
     * Make a REST call to the specified destination
     * @param dest
     * @return 
     * Event - This generates a CALL_CONNECTED event
     */
    @Override
    public XMSReturnCode Makecall(String dest){
        FunctionLogger logger=new FunctionLogger("Makecall",this,m_logger);
        logger.args("Dest="+dest,MakecallOptions);
        String XMLPAYLOAD;
        SendCommandResponse RC ;
        
        //Set the call typeto webrtc based on if the URI has rtc:
        setCallType(getCallTypeFromURI(dest));
        
        // RDM: Build and return a makecall payload
        XMLPAYLOAD = buildMakecallPayload(dest); 

        //logger.info("Sending message ---->  " + XMLPAYLOAD);
        RC = m_connector.SendCommand(this,RESTOPERATION.POST, "calls", XMLPAYLOAD);
        
         if (RC.get_scr_status_code() == 201){
             if(MakecallOptions.m_signalingEnabled){
                m_pendingtransactionInfo.setDescription("Makecall Destination"+dest);
                m_pendingtransactionInfo.setTransactionId(RC.get_scr_transaction_id());
                m_pendingtransactionInfo.setResponseData(RC);
                setState(XMSCallState.MAKECALL);
                        try {
                            BlockIfNeeded(XMSEventType.CALL_CONNECTED);
                        } catch (InterruptedException ex) {
                            logger.error("Exception:"+ex);
                        }
                        setConnectionAddress(dest);
                        setCalledAddress(dest);
                    } else {
                        XMSEvent l_callbackevt = new XMSEvent();
                        l_callbackevt.CreateEvent(XMSEventType.CALL_CONNECTED, this, RC.get_scr_return_xml_payload(), "", RC.toString());
                        setLastEvent(l_callbackevt);
                        
                        setState(XMSCallState.CONNECTED);
                    }
                    return XMSReturnCode.SUCCESS;

        } else {

            logger.info("Call Failed, Status Code: " + RC.get_scr_status_code());
            setState(XMSCallState.NULL);
            return XMSReturnCode.FAILURE;

        }
        
    }
   
    /**
     * Send a XMS REST Message 
     * @return 
     * Event - NONE
     */
    public SendCommandResponse SendRestMessage(String XMLPAYLOAD){
        FunctionLogger logger=new FunctionLogger("SendRestMessage",this,m_logger);
        
        SendCommandResponse RC ;
        
        
        //logger.info("Sending message ---->  " + XMLPAYLOAD);
        RC = m_connector.SendCommand(this,RESTOPERATION.PUT, "calls", XMLPAYLOAD);
        
         if (RC.get_scr_status_code() == 201){
            return RC;

        } else {

            logger.info("Status Code: " + RC.get_scr_status_code());
           
            return RC;

        }
        
    }
     /**
     * Update a REST call 
     * @return 
     * Event - NONE
     */
    @Override
    public XMSReturnCode Updatecall(){
        FunctionLogger logger=new FunctionLogger("Updatecall",this,m_logger);
        logger.args(UpdatecallOptions);
       String l_urlext;
        SendCommandResponse RC ;
        //todo!!: create the payload and add the call id.
        l_urlext = "calls/" + m_callIdentifier;

        String XMLPAYLOAD;
       
        // RDM: Build and return a updatecall payload
        XMLPAYLOAD = buildUpdatecallPayload(); 

        //logger.info("Sending message ---->  " + XMLPAYLOAD);
        RC = m_connector.SendCommand(this,RESTOPERATION.PUT, l_urlext, XMLPAYLOAD);
        
         if (RC.get_scr_status_code() == 200){
                    XMSEvent l_callbackevt = new XMSEvent();
                    l_callbackevt.CreateEvent(XMSEventType.CALL_UPDATED, this, RC.get_scr_return_xml_payload(), "", RC.toString());
                    setLastEvent(l_callbackevt);
            return XMSReturnCode.SUCCESS;

        } else {

            logger.info("Update Call Failed, Status Code: " + RC.get_scr_status_code());
           
            return XMSReturnCode.FAILURE;

        }
        
    }
    /**
     *  Drops the call by sending messages to the XMS server.
     *
     *  Utilizes the delete function.  No XML payload.
     * @return
     */
     @Override
    public XMSReturnCode Dropcall(){
       FunctionLogger logger=new FunctionLogger("Dropcall",this,m_logger);

        String l_urlext;
        SendCommandResponse RC ;
        if(getState() == XMSCallState.NULL ){
            logger.info("Call is already in NULL state, not sending drop but instead just returning");
            //TODO figure out if this should return success or bad state.
            //15-Jun-2012 dsl - i think returning success is fine.
            return XMSReturnCode.SUCCESS;
        } else {
            if(getState() == XMSCallState.DISCONNECTED){
                logger.info("Call is already in DISCONNECTED state, just cleaning up internals");
                setConnectionAddress(null);
                setCalledAddress(null);
                m_connector.RemoveCallFromActiveCallList(m_callIdentifier);
                m_callIdentifier = null;
                setCallType(XMSCallType.UNKNOWN);
                setState(XMSCallState.NULL);
                return XMSReturnCode.SUCCESS;
  
            }
                else {
                l_urlext = "calls/" + m_callIdentifier ;

                RC = m_connector.SendCommand(this,RESTOPERATION.DELETE, l_urlext, null);
            }
            // Check if the delete call was OK.
        if (RC.get_scr_status_code() == 204){
                setConnectionAddress(null);
                setCalledAddress(null);
                m_connector.RemoveCallFromActiveCallList(m_callIdentifier);
                m_callIdentifier = null;
                setCallType(XMSCallType.UNKNOWN);
                setState(XMSCallState.NULL);
                return XMSReturnCode.SUCCESS;

            } else {  // delete call failed

                logger.info("Dropcall Failed, Status Code: " + RC.get_scr_status_code());
                setState(XMSCallState.NULL);
                return XMSReturnCode.FAILURE;

            } // end if
        }
    } // end drop call 
/**
     * Unattended / Unsupervised transfer to destination
     * @param a_dest - URI for the destination
     * @return 
     */
    @Override
    public XMSReturnCode Transfer( String a_dest){
        FunctionLogger logger=new FunctionLogger("Transfer",this,m_logger);
        logger.args(a_dest);
        
        String l_urlext;
        SendCommandResponse RC ;
        //todo!!: create the payload and add the call id.
        l_urlext = "calls/" + m_callIdentifier;

        String XMLPAYLOAD;
       
        // RDM: Build and return a updatecall payload
        XMLPAYLOAD = buildUnattendedTransferPayload(a_dest); 

        //logger.info("Sending message ---->  " + XMLPAYLOAD);
        RC = m_connector.SendCommand(this,RESTOPERATION.PUT, l_urlext, XMLPAYLOAD);
        
         if (RC.get_scr_status_code() >= 200 && RC.get_scr_status_code() <= 299){
                    XMSEvent l_callbackevt = new XMSEvent();
                    l_callbackevt.CreateEvent(XMSEventType.CALL_UPDATED, this, RC.get_scr_return_xml_payload(), "", RC.toString());
                    setLastEvent(l_callbackevt);
            return XMSReturnCode.SUCCESS;

        } else {

            logger.info("Transfer Call Failed, Status Code: " + RC.get_scr_status_code());
           
            return XMSReturnCode.FAILURE;

        }
    }
    
    /**
     * Redirect to destination
     * @param a_dest - URI for the destination
     * @return 
     */
    @Override
    public XMSReturnCode Redirect( String a_dest){
        FunctionLogger logger=new FunctionLogger("Redirect",this,m_logger);
        logger.args(a_dest);
        
        if(getState()!= XMSCallState.OFFERED){
            return XMSReturnCode.FAILURE;
        }
        
        String l_urlext;
        SendCommandResponse RC ;
        //todo!!: create the payload and add the call id.
        l_urlext = "calls/" + m_callIdentifier;

        String XMLPAYLOAD;
       
        // RDM: Build and return a updatecall payload
        XMLPAYLOAD = buildRedirectPayload(a_dest); 

        //logger.info("Sending message ---->  " + XMLPAYLOAD);
        RC = m_connector.SendCommand(this,RESTOPERATION.PUT, l_urlext, XMLPAYLOAD);
        
         if (RC.get_scr_status_code() >= 200 && RC.get_scr_status_code() <= 299){
                    XMSEvent l_callbackevt = new XMSEvent();
                    l_callbackevt.CreateEvent(XMSEventType.CALL_UPDATED, this, RC.get_scr_return_xml_payload(), "", RC.toString());
                    setLastEvent(l_callbackevt);
            return XMSReturnCode.SUCCESS;

        } else {

            logger.info("Redirect Call Failed, Status Code: " + RC.get_scr_status_code());
           
            return XMSReturnCode.FAILURE;

        }
    }

      /**
     * Supervised transfer to another connected call
     * @param a_call - XMSCall Object of a connectd call
     * @return 
     */
    @Override
    public XMSReturnCode Transfer( XMSCall a_call){
        FunctionLogger logger=new FunctionLogger("Transfer",this,m_logger);
        logger.args(a_call);
        
        String l_urlext;
        SendCommandResponse RC ;
        //todo!!: create the payload and add the call id.
        l_urlext = "calls/" + m_callIdentifier;

        String XMLPAYLOAD;
       
        // RDM: Build and return a updatecall payload
        XMLPAYLOAD = buildAttendedTransferPayload(a_call); 

        //logger.info("Sending message ---->  " + XMLPAYLOAD);
        RC = m_connector.SendCommand(this,RESTOPERATION.PUT, l_urlext, XMLPAYLOAD);
        
         if (RC.get_scr_status_code() == 200){
                    XMSEvent l_callbackevt = new XMSEvent();
                    l_callbackevt.CreateEvent(XMSEventType.CALL_UPDATED, this, RC.get_scr_return_xml_payload(), "", RC.toString());
                    setLastEvent(l_callbackevt);
            return XMSReturnCode.SUCCESS;

        } else {

            logger.info("Transfer Call Failed, Status Code: " + RC.get_scr_status_code());
           
            return XMSReturnCode.FAILURE;

        }
    }
    
     /**
      * Send out the message using MRCP 
      * @param a_message - String that is to be sent
      * @return 
      */
    @Override
     public XMSReturnCode SendMessage(String a_message){
         FunctionLogger logger=new FunctionLogger("SendMessage",this,m_logger);
        logger.args(a_message);
        
             
        String l_urlext;
        SendCommandResponse RC ;
        //todo!!: create the payload and add the call id.
        l_urlext = "calls/" + m_callIdentifier;

        String XMLPAYLOAD;
       
        // RDM: Build and return a updatecall payload
        XMLPAYLOAD = buildSendMessagePayload(a_message); 

      //  logger.info("Sending message ---->  " + XMLPAYLOAD);
        RC = m_connector.SendCommand(this,RESTOPERATION.PUT, l_urlext, XMLPAYLOAD);
          
         if (RC.get_scr_status_code() == 200){
            m_pendingtransactionInfo.setDescription("SendMessage="+a_message);
            m_pendingtransactionInfo.setTransactionId(RC.get_scr_transaction_id());
            m_pendingtransactionInfo.setResponseData(RC);
            
            setState(XMSCallState.SENDMESSAGE);
                    try {
                        BlockIfNeeded(XMSEventType.CALL_SENDMESSAGE_END);
                    } catch (InterruptedException ex) {
                        logger.error("Exception:"+ex);
                    }

                    return XMSReturnCode.SUCCESS;

        } else {

            logger.info("SendMessage Failed, Status Code: " + RC.get_scr_status_code());
            
            //setState(XMSCallState.NULL);
            return XMSReturnCode.FAILURE;

        }
         
     }
 String RestGetCalledAddress(String rawxml){
     FunctionLogger logger=new FunctionLogger("******RestGetCalledAddress",this,m_logger);
        logger.args(rawxml);
        
     
     return "NONE";
 }
   /**
     * This Answer an incoming call.  
     *
     * @return
     */
    @Override
     public XMSReturnCode Answercall(){
        FunctionLogger logger=new FunctionLogger("Answercall",this,m_logger);
        logger.args(AnswercallOptions);
         
        

        String l_urlext;
        SendCommandResponse RC ;
        //todo!!: create the payload and add the call id.
        l_urlext = "calls/" + m_callIdentifier;

        String XMLPAYLOAD;
        /*
        XMLPAYLOAD = "<web_service version=\"1.0\">"
                + "<call answer=\"yes\" media=\""+AnswercallOptions.GetCallTypeAsString()+"\"/>"
                + "</web_service>";
        */
        XMLPAYLOAD=buildAnswercallPayload();
       // logger.info("Sending message ---->  " + XMLPAYLOAD);
        RC = m_connector.SendCommand(this,RESTOPERATION.PUT, l_urlext, XMLPAYLOAD);
        
         if (RC.get_scr_status_code() == 200){
            setState(XMSCallState.CONNECTED);
            //TODO: Should likely make a way to obtain the connection info 
                   
                   setConnectionAddress(RC.get_scr_source());
         //          setCalledAddress(RestGetCalledAddress(RC.get_scr_return_xml_payload()));  
                    return XMSReturnCode.SUCCESS;

        } else {

            logger.info("Answer Call Failed, Status Code: " + RC.get_scr_status_code());
            setState(XMSCallState.NULL);
            return XMSReturnCode.FAILURE;

        }
    
    
    } // End Answercall
     /**
     * This Answer an incoming call.  
     *
     * @return
     */
    @Override
     public XMSReturnCode Acceptcall(){
        FunctionLogger logger=new FunctionLogger("Acceptcall",this,m_logger);
        //logger.args(AnswercallOptions);
         
        

        String l_urlext;
        SendCommandResponse RC ;
        //todo!!: create the payload and add the call id.
        l_urlext = "calls/" + m_callIdentifier;

        String XMLPAYLOAD;
        /*
        XMLPAYLOAD = "<web_service version=\"1.0\">"
                + "<call answer=\"yes\" media=\""+AnswercallOptions.GetCallTypeAsString()+"\"/>"
                + "</web_service>";
        */
        XMLPAYLOAD=buildAcceptcallPayload();
       // logger.info("Sending message ---->  " + XMLPAYLOAD);
        RC = m_connector.SendCommand(this,RESTOPERATION.PUT, l_urlext, XMLPAYLOAD);
        
         if (RC.get_scr_status_code() >= 200 && RC.get_scr_status_code() < 300){
            setState(XMSCallState.ACCEPTED);
            //TODO: Should likely make a way to obtain the connection info 
                   
                   //setConnectionAddress(RC.get_scr_source());
         //          setCalledAddress(RestGetCalledAddress(RC.get_scr_return_xml_payload()));  
                    return XMSReturnCode.SUCCESS;

        } else {

            logger.info("Accept Call Failed, Status Code: " + RC.get_scr_status_code());
            setState(XMSCallState.NULL);
            return XMSReturnCode.FAILURE;

        }
    
    
    } // End Answercall
     
     /**
     * Join / Route 2 Calls together
     * @param a_othercall
     * @return 
     */
     @Override
    public XMSReturnCode Join( XMSCall a_othercall){
         FunctionLogger logger=new FunctionLogger("Join",this,m_logger);
         
         if(this.getState() == XMSCallState.NULL || a_othercall.getState() == XMSCallState.NULL){
            logger.error("Unable to Join as one of the calls is in NULL state");
            return XMSReturnCode.INVALID_STATE;
            
         }
         
         
        String XMLPAYLOAD;
        SendCommandResponse RC ;

        String l_urlext;
        l_urlext = "calls/" + m_callIdentifier;
        
        ArrayList<String> l_playlist = new ArrayList<String>();
        
        
        XMLPAYLOAD = buildJoinPayload(a_othercall.getCallIdentifier()); 

        //logger.info("Sending message ---->  " + XMLPAYLOAD);
        RC = m_connector.SendCommand(this,RESTOPERATION.PUT, l_urlext, XMLPAYLOAD);
        
         if (RC.get_scr_status_code() == 200){
            
                    return XMSReturnCode.SUCCESS;

        } else {

            logger.info("Join Failed, Status Code: " + RC.get_scr_status_code());
            //setState(XMSCallState.NULL);
            return XMSReturnCode.FAILURE;

        }
        
    }// end Join
     
     /**
     * UnJoin / UnRoute 2 Calls together
     * @return 
     */
     @Override
    public XMSReturnCode UnJoin(){
         FunctionLogger logger=new FunctionLogger("UnJoin",this,m_logger);
         
         if(this.getState() == XMSCallState.NULL){
            logger.error("Unable to UnJoin as the call is in NULL state");
            return XMSReturnCode.INVALID_STATE;
         }
         
         
        String XMLPAYLOAD;
        SendCommandResponse RC ;

        String l_urlext;
        l_urlext = "calls/" + m_callIdentifier;
        
        ArrayList<String> l_playlist = new ArrayList<String>();
        
        
        XMLPAYLOAD = buildUnJoinPayload(); 

        //logger.info("Sending message ---->  " + XMLPAYLOAD);
        RC = m_connector.SendCommand(this,RESTOPERATION.PUT, l_urlext, XMLPAYLOAD);
        
         if (RC.get_scr_status_code() == 200){
            
                    return XMSReturnCode.SUCCESS;

        } else {

            logger.info("UnJoin Failed, Status Code: " + RC.get_scr_status_code());
            //setState(XMSCallState.NULL);
            return XMSReturnCode.FAILURE;

        }
        
    }// end UnJoin
     /**
     * UnJoin / UnRoute 2 Calls together
     * @return 
     */
     @Override
    public XMSReturnCode Stop(){
         FunctionLogger logger=new FunctionLogger("Stop",this,m_logger);
         
         if(this.getState() == XMSCallState.NULL){
            logger.error("Unable to Stop as the call is in NULL state");
            return XMSReturnCode.INVALID_STATE;
         }
         
         
        String XMLPAYLOAD;
        SendCommandResponse RC ;

        String l_urlext;
        l_urlext = "calls/" + m_callIdentifier;
        
        ArrayList<String> l_playlist = new ArrayList<String>();
        
        
        XMLPAYLOAD = buildStopPayload(); 

        //logger.info("Sending message ---->  " + XMLPAYLOAD);
        RC = m_connector.SendCommand(this,RESTOPERATION.PUT, l_urlext, XMLPAYLOAD);
        
         if (RC.get_scr_status_code() == 200){
            
                    return XMSReturnCode.SUCCESS;

        } else {

            logger.info("Stop Failed, Status Code: " + RC.get_scr_status_code());
            //setState(XMSCallState.NULL);
            return XMSReturnCode.FAILURE;

        }
        
    }// end Stop
    /**
     * Add in this call to the specified conference
     * @param a_conf
     * @return 
     */
    public XMSReturnCode AddToConference(XMSConference a_conf){
        FunctionLogger logger=new FunctionLogger("AddToConference",this,m_logger);
        logger.args("Conf "+a_conf);
        String XMLPAYLOAD;
        SendCommandResponse RC ;

        
        String l_urlext;
        l_urlext = "calls/" + m_callIdentifier;
                
        XMLPAYLOAD = buildAddPartyPayload(a_conf);                 
        // END WORKAROUND
        //XMLPAYLOAD = buildRecordPayload(a_recfile); 

        //logger.info("Sending message ---->  " + XMLPAYLOAD);
        RC = m_connector.SendCommand(this,RESTOPERATION.PUT, l_urlext, XMLPAYLOAD);
        
         if (RC.get_scr_status_code() == 200){
            logger.info("AddToConference was a success");
            return XMSReturnCode.SUCCESS;

        } else {

            logger.info("AddToConference Failed, Status Code: " + RC.get_scr_status_code());
            //setState(XMSCallState.NULL);
            return XMSReturnCode.FAILURE;

        }
        
        
    }
     /**
     * Add in this call to the specified conference
     * @param a_conf
     * @return 
     */
    public XMSReturnCode RemoveFromConference(XMSConference a_conf){
        FunctionLogger logger=new FunctionLogger("RemoveFromConference",this,m_logger);
        logger.args("Conf "+a_conf);
        String XMLPAYLOAD;
        SendCommandResponse RC ;

        
        String l_urlext;
        l_urlext = "calls/" + m_callIdentifier;
                
        XMLPAYLOAD = buildRemovePartyPayload(a_conf);                 
        // END WORKAROUND
        //XMLPAYLOAD = buildRecordPayload(a_recfile); 

        //logger.info("Sending message ---->  " + XMLPAYLOAD);
        RC = m_connector.SendCommand(this,RESTOPERATION.PUT, l_urlext, XMLPAYLOAD);
        
         if (RC.get_scr_status_code() == 200){
            logger.info("RemoveFromParty was a success");
            return XMSReturnCode.SUCCESS;

        } else {

            logger.info("RemoveFromParty Failed, Status Code: " + RC.get_scr_status_code());
            //setState(XMSCallState.NULL);
            return XMSReturnCode.FAILURE;

        }
        
        
    }
    /**
     * Play a file
     * @param a_filelist - A list of Files to be played
     * @return
     */
    @Override
     public XMSReturnCode PlayList(ArrayList<String> a_filelist){
        FunctionLogger logger=new FunctionLogger("PlayList",this,m_logger);
        logger.args("Playlist "+a_filelist+" " + PlayOptions);
        String XMLPAYLOAD;
        SendCommandResponse RC ;

        //TODO Playlist in REST only can be 1.  Need to under the hood handle this but for now will only support a list of size 1
        if(a_filelist.size() != 1){
            logger.error("XMSRestCall Only supports a single file at this time multiple play files are not implimented");
            return XMSReturnCode.NOT_IMPLEMENTED;
        }
        
        String l_urlext;
        l_urlext = "calls/" + m_callIdentifier;
        
       
        XMLPAYLOAD = buildPlayPayload(a_filelist); 

        //logger.info("Sending message ---->  " + XMLPAYLOAD);
        RC = m_connector.SendCommand(this,RESTOPERATION.PUT, l_urlext, XMLPAYLOAD);
        
         if (RC.get_scr_status_code() == 200){
            m_pendingtransactionInfo.setDescription("Play file(s)="+a_filelist);
            m_pendingtransactionInfo.setTransactionId(RC.get_scr_transaction_id());
            m_pendingtransactionInfo.setResponseData(RC);
            
            setState(XMSCallState.PLAY);
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
     * This will record the line data to the specified file
     * @param a_recfile - getName of the FIle to Record to
     * @return 
     */
        @Override
     public XMSReturnCode Record(String a_recfile){
        FunctionLogger logger=new FunctionLogger("Record",this,m_logger);
        logger.args("Record "+a_recfile+" " + RecordOptions);
        String XMLPAYLOAD;
        SendCommandResponse RC ;

        
        String l_urlext;
        l_urlext = "calls/" + m_callIdentifier;
        
        // TODO get this fixed and remove this dumb workaround
        // WORKAROUND
        String l_recfile=a_recfile;
        if(a_recfile.toLowerCase().endsWith(".wav") || a_recfile.toLowerCase().endsWith(".vid")){
            l_recfile=a_recfile.substring(0, a_recfile.length()-4);
        }
        XMLPAYLOAD = buildRecordPayload(l_recfile);                 
        // END WORKAROUND
        //XMLPAYLOAD = buildRecordPayload(a_recfile); 

        //logger.info("Sending message ---->  " + XMLPAYLOAD);
        RC = m_connector.SendCommand(this,RESTOPERATION.PUT, l_urlext, XMLPAYLOAD);
        
         if (RC.get_scr_status_code() == 200){
             m_pendingtransactionInfo.setDescription("Record file"+l_recfile);
            m_pendingtransactionInfo.setTransactionId(RC.get_scr_transaction_id());
            m_pendingtransactionInfo.setResponseData(RC);
            setState(XMSCallState.RECORD);
                    try {
                        BlockIfNeeded(XMSEventType.CALL_RECORD_END);
                    } catch (InterruptedException ex) {
                        logger.error("Exception:"+ex);
                    }

                    return XMSReturnCode.SUCCESS;

        } else {

            logger.info("Record Failed, Status Code: " + RC.get_scr_status_code());
            //setState(XMSCallState.NULL);
            return XMSReturnCode.FAILURE;

        }
        
    } // end record
           @Override
     public XMSReturnCode PlayRecord(String a_playfile,String a_recfile){
        FunctionLogger logger=new FunctionLogger("PlayRecord",this,m_logger);
        logger.args("Record playfile="+a_playfile+" recfile="+a_recfile+" " + PlayRecordOptions);
        
        String XMLPAYLOAD;
        SendCommandResponse RC ;

        
        String l_urlext;
        l_urlext = "calls/" + m_callIdentifier;
        
        // TODO get this fixed and remove this dumb workaround
        // WORKAROUND
        String l_recfile=a_recfile;
        if(a_recfile.toLowerCase().endsWith(".wav")||a_recfile.toLowerCase().endsWith(".vid")){
            l_recfile=a_recfile.substring(0, a_recfile.length()-4);
        }
        // END WORKAROUND
      // TODO get this fixed and remove this dumb workaround
        // WORKAROUND
        String l_playfile=a_playfile;
        if(a_playfile.toLowerCase().endsWith(".wav") || a_playfile.toLowerCase().endsWith(".vid")){
            l_playfile=a_playfile.substring(0, a_playfile.length()-4);
        }
        XMLPAYLOAD = buildPlayCollectPayload(l_playfile); 

        XMLPAYLOAD = buildPlayRecordPayload(l_playfile,l_recfile);                 
       
       
        //logger.info("Sending message ---->  " + XMLPAYLOAD);
        RC = m_connector.SendCommand(this,RESTOPERATION.PUT, l_urlext, XMLPAYLOAD);
        
         if (RC.get_scr_status_code() == 200){
            m_pendingtransactionInfo.setDescription("Playrecord playfile="+a_playfile+" recfile="+l_recfile);
            m_pendingtransactionInfo.setTransactionId(RC.get_scr_transaction_id());
            m_pendingtransactionInfo.setResponseData(RC);
            setState(XMSCallState.PLAYRECORD);
                    try {
                        BlockIfNeeded(XMSEventType.CALL_PLAYRECORD_END);
                    } catch (InterruptedException ex) {
                        logger.error("Exception:"+ex);
                    }

                    return XMSReturnCode.SUCCESS;

        } else {

            logger.info("PlayRecord Failed, Status Code: " + RC.get_scr_status_code());
            //setState(XMSCallState.NULL);
            return XMSReturnCode.FAILURE;

        }
        
    } // end playrecord
           
    /**
     * Collect the DTMF Digit information
     * @return 
     */
        @Override
      public XMSReturnCode CollectDigits(){
            
        FunctionLogger logger=new FunctionLogger("CollectDigits",this,m_logger);
        logger.args(CollectDigitsOptions);
        String XMLPAYLOAD;
        SendCommandResponse RC ;

     
        
        String l_urlext;
        l_urlext = "calls/" + m_callIdentifier;
        

        XMLPAYLOAD = buildCollectPayload(); 

        //logger.info("Sending message ---->  " + XMLPAYLOAD);
        RC = m_connector.SendCommand(this,RESTOPERATION.PUT, l_urlext, XMLPAYLOAD);
        
         if (RC.get_scr_status_code() == 200){
            m_pendingtransactionInfo.setDescription("CollectDigits");
            m_pendingtransactionInfo.setTransactionId(RC.get_scr_transaction_id());
            m_pendingtransactionInfo.setResponseData(RC);
            setState(XMSCallState.COLLECTDIGITS);
                    try {
                        BlockIfNeeded(XMSEventType.CALL_COLLECTDIGITS_END);
                    } catch (InterruptedException ex) {
                        logger.error("Exception:"+ex);
                    }

                    return XMSReturnCode.SUCCESS;

        } else {

            logger.info("Collect Digits Failed, Status Code: " + RC.get_scr_status_code());
            //setState(XMSCallState.CONNECTED);
            return XMSReturnCode.FAILURE;

        }
        
    } // end collect
    @Override
      public XMSReturnCode PlayCollect(String a_playfile){
        FunctionLogger logger=new FunctionLogger("PlayCollect",this,m_logger);
        logger.args(PlayCollectOptions);
        String XMLPAYLOAD;
        SendCommandResponse RC ;

     
        
        String l_urlext;
        l_urlext = "calls/" + m_callIdentifier;
        
// TODO get this fixed and remove this dumb workaround
        // WORKAROUND
        String l_playfile=a_playfile;
        if(a_playfile.toLowerCase().endsWith(".wav") || a_playfile.toLowerCase().endsWith(".vid")){
            l_playfile=a_playfile.substring(0, a_playfile.length()-4);
        }
        XMLPAYLOAD = buildPlayCollectPayload(l_playfile); 

        //logger.info("Sending message ---->  " + XMLPAYLOAD);
        RC = m_connector.SendCommand(this,RESTOPERATION.PUT, l_urlext, XMLPAYLOAD);
        
         if (RC.get_scr_status_code() == 200){
            m_pendingtransactionInfo.setDescription("PlayCollect playfile="+a_playfile);
            m_pendingtransactionInfo.setTransactionId(RC.get_scr_transaction_id());
            m_pendingtransactionInfo.setResponseData(RC);
            setState(XMSCallState.COLLECTDIGITS);
                    try {
                        BlockIfNeeded(XMSEventType.CALL_COLLECTDIGITS_END);
                    } catch (InterruptedException ex) {
                        logger.error("Exception:"+ex);
                    }

                    return XMSReturnCode.SUCCESS;

        } else {

            logger.info("Collect Digits Failed, Status Code: " + RC.get_scr_status_code());
            //setState(XMSCallState.CONNECTED);
            return XMSReturnCode.FAILURE;

        }
        
    } // end playcollect
    
         
      /**
     * This is the Notify handler that will be called by EventThread when
     * new events are created.
     * @param obj
     * @param arg 
     */
    //TODO Add in support for the REASON code for all events!!
    @Override
    public void update(Observable obj, Object arg) {
        FunctionLogger logger=new FunctionLogger("update",this,m_logger);
        logger.args("obj="+obj+" arg="+arg);
        XMSEvent l_callbackevt = new XMSEvent();
        if (arg instanceof XMSRestEvent){
            logger.info("WebSequence "+this.toString()+" {{{XMS->App: EVENT "+arg.toString()+" }}}");
            XMSRestEvent l_evt=(XMSRestEvent) arg;
            //TODO This Method is getting a little combersome, may want to extend out to private OnXXXXEvent functions to segment the readability.
            //TODO Should we support the Ringing event?
                if (l_evt.eventType.contentEquals("end_play")) {
                    logger.info("Processing play end event");
                   m_pendingtransactionInfo.Reset();
                    setState(XMSCallState.CONNECTED);
                    /*
                    List<EventDataDocument.EventData> l_datalist=l_evt.event.getEventDataList();
                        for(EventDataDocument.EventData ed: l_datalist){
                            if (ed.getName() == EventDataName.REASON){
                                l_callbackevt.setReason(ed.getValue());
                            }
                        }
                   //Above has been depricated, changing to getXXXAray
                   */
                    EventData[] l_datalist=l_evt.event.getEventDataArray();
                        for(EventDataDocument.EventData ed: l_datalist){
                            if (ed.getName().contentEquals( EventDataName.REASON.toString())){
                                l_callbackevt.setReason(ed.getValue());
                            }
                        }
                    l_callbackevt.CreateEvent(XMSEventType.CALL_PLAY_END, this, "", l_callbackevt.getReason(), l_evt.toString());//30-Jul-2012 dsl
                    UnblockIfNeeded(l_callbackevt);
                    //end end_play
                }  else if (l_evt.eventType.contentEquals("end_playcollect")) {
                    logger.info("Processing end_playcollect event");
                    m_pendingtransactionInfo.Reset();
                    if(getState()== XMSCallState.COLLECTDIGITS){
                        l_callbackevt.CreateEvent(XMSEventType.CALL_COLLECTDIGITS_END, this, "", "", l_evt.toString());
                       
                        
                    } else{
                        l_callbackevt.CreateEvent(XMSEventType.CALL_PLAYCOLLECT_END, this, "", "", l_evt.toString());
                    }
                     //List<EventDataDocument.EventData> l_datalist=l_evt.event.getEventDataList();
                    EventData[] l_datalist=l_evt.event.getEventDataArray();
                        for(EventDataDocument.EventData ed: l_datalist){
                            if(ed.getName().contentEquals(EventDataName.DIGITS.toString())){
                                l_callbackevt.setData(ed.getValue());
                                logger.info("Digits Collected: " + l_callbackevt.getData());
                            } else if (ed.getName().contentEquals(EventDataName.REASON.toString())){
                                l_callbackevt.setReason(ed.getValue());
                            }
                        }
                    setState(XMSCallState.CONNECTED);
                    UnblockIfNeeded(l_callbackevt);
                    //end end_playcollect
                }else if (l_evt.eventType.contentEquals("end_playrecord")) {
                    logger.info("Processing end_playrecord event");
                    m_pendingtransactionInfo.Reset();
                    if(getState()== XMSCallState.RECORD){
                        
                        //List<EventDataDocument.EventData> l_datalist=l_evt.event.getEventDataList(); // 30-Jul-2012 dsl
                        EventData[] l_datalist=l_evt.event.getEventDataArray(); // 30-Jul-2012 dsl
                        for(EventDataDocument.EventData ed: l_datalist){                            // 30-Jul-2012 dsl
                            if (ed.getName().contentEquals(EventDataName.REASON.toString())){                              // 30-Jul-2012 dsl
                                l_callbackevt.setReason(ed.getValue());                             // 30-Jul-2012 dsl
                            }                                                                       // 30-Jul-2012 dsl
                        }                                                                           // 30-Jul-2012 dsl
                        
                        l_callbackevt.CreateEvent(XMSEventType.CALL_RECORD_END, this, "", l_callbackevt.getReason(), l_evt.toString()); // 30-Jul-2012 dsl
                        
                        
                    } else{
                        
                        EventData[] l_datalist=l_evt.event.getEventDataArray(); // 30-Jul-2012 dsl
                        for(EventDataDocument.EventData ed: l_datalist){                            // 30-Jul-2012 dsl
                            if (ed.getName().contentEquals(EventDataName.REASON.toString())){                              // 30-Jul-2012 dsl
                                l_callbackevt.setReason(ed.getValue());                             // 30-Jul-2012 dsl
                            }                                                                       // 30-Jul-2012 dsl
                        }                                                                           // 30-Jul-2012 dsl
                        
                        l_callbackevt.CreateEvent(XMSEventType.CALL_PLAYCOLLECT_END, this, "", l_callbackevt.getReason(), l_evt.toString());
                    }
// 30-Jul-2012 DSL -- this patch of code seems like it does not due anything!
//                    List<EventDataDocument.EventData> l_datalist=l_evt.event.getEventDataList();
//                        for(EventDataDocument.EventData ed: l_datalist){
//                            if (ed.getName() == EventDataName.REASON){
//                                l_callbackevt.setReason(ed.getValue());
//                            } 
//                        }
                    setState(XMSCallState.CONNECTED);
                    UnblockIfNeeded(l_callbackevt);
                    //end end_playrecord
                }else if (l_evt.eventType.contentEquals("incoming")) {
                    
                    //Check the caller_uri to see if this is a RTC or SIP.  To do this will need to parst the raw string
                    logger.info("Processing incoming event");
                       int start=l_evt.rawstring.indexOf("name=\"caller_uri\" value=\"");
                    
                       int end=l_evt.rawstring.indexOf("/>", start);
                       if(start > -1 && end > -1){
                            String caller_uri = unescapeXML((String) l_evt.rawstring.subSequence(start, end));
                            
                            setCallType(getCallTypeFromURI(caller_uri));
                       } else{
                           logger.info("Can't detect call type from inboutn caller_uri, setting to SIP by default");
                           setCallType(XMSCallType.SIP);
                       }
                                    
                       //TODO Fixt this to something nicer
                    //Check the caller_uri to see if this is a RTC or SIP.  To do this will need to parst the raw string
                    logger.info("Processing incoming event");
                       start=l_evt.rawstring.indexOf("name=\"called_uri\" value=\"");
                    
                       end=l_evt.rawstring.indexOf("/>", start);
                       if(start > -1 && end > -1){
                            String called_uri = unescapeXML((String) l_evt.rawstring.subSequence(start, end));
                            
                            setCalledAddress(called_uri);   
                       } else{
                           logger.info("Can't detect called_uri in inbound message, setting to empty string");
                           //setCallType(XMSCallType.SIP);
                           setCalledAddress("");
                       }
                       //TODO fix this too
                       
                    if(WaitcallOptions.m_autoConnectEnabled){
                        Answercall();
                        l_callbackevt.CreateEvent(XMSEventType.CALL_CONNECTED, this, "", "", l_evt.toString());
                        UnblockIfNeeded(l_callbackevt);
                    } else {
                        setState(XMSCallState.OFFERED);
                        l_callbackevt.CreateEvent(XMSEventType.CALL_OFFERED, this, "", "", l_evt.toString());
                    UnblockIfNeeded(l_callbackevt);
                    }
                    //end incoming
                } else if (l_evt.eventType.contentEquals("stream")) {
                    logger.info("Processing state event");
                                        
                    EventData[] l_datalist=l_evt.event.getEventDataArray(); // 27-Jul-2012 dsl
                        for(EventDataDocument.EventData ed: l_datalist){                         // 27-Jul-2012 dsl
                            if (ed.getName().contentEquals(EventDataName.REASON.toString())){                           // 27-Jul-2012 dsl
                                l_callbackevt.setReason(ed.getValue());                          // 27-Jul-2012 dsl
                            } 
                        }
                    l_callbackevt.CreateEvent(XMSEventType.CALL_STATE, this, "", l_callbackevt.getReason(), l_evt.toString()); // 30-Jul-2012 dsl
                    //end stream state
                }else if (l_evt.eventType.contentEquals("connected")) {
                    logger.info("Processing connected event");
                    setState(XMSCallState.CONNECTED);
                    
                    EventData[] l_datalist=l_evt.event.getEventDataArray(); // 27-Jul-2012 dsl
                        for(EventDataDocument.EventData ed: l_datalist){                         // 27-Jul-2012 dsl
                            if (ed.getName().contentEquals(EventDataName.REASON.toString())){                           // 27-Jul-2012 dsl
                                l_callbackevt.setReason(ed.getValue());                          // 27-Jul-2012 dsl
                            } 
                        }
                    l_callbackevt.CreateEvent(XMSEventType.CALL_CONNECTED, this, "", l_callbackevt.getReason(), l_evt.toString()); // 30-Jul-2012 dsl
                    UnblockIfNeeded(l_callbackevt);
                    //end connected
                } else if (l_evt.eventType.contentEquals("hangup")) {
                    logger.info("Processing hangup event");
                    setState(XMSCallState.DISCONNECTED);
                    EventData[] l_datalist=l_evt.event.getEventDataArray();// 30-Jul-2012 dsl
                    for(EventDataDocument.EventData ed: l_datalist){                            // 30-Jul-2012 dsl
                        if (ed.getName().contentEquals(EventDataName.REASON.toString())){                              // 30-Jul-2012 dsl
                            l_callbackevt.setReason(ed.getValue());                             // 30-Jul-2012 dsl
                        } 
                    }
                    l_callbackevt.CreateEvent(XMSEventType.CALL_DISCONNECTED, this, "", l_callbackevt.getReason(), l_evt.toString()); // 30-Jul-2012 dsl
                    UnblockIfNeeded(l_callbackevt);
                    //end hangup
                }else if (l_evt.eventType.contentEquals("alarm")) {
                    logger.info("Processing alarm event");
                    EventData[] l_datalist=l_evt.event.getEventDataArray();// 30-Jul-2012 dsl
                    for(EventDataDocument.EventData ed: l_datalist){                            // 30-Jul-2012 dsl
                        if (ed.getName().contentEquals(EventDataName.REASON.toString())){                              // 30-Jul-2012 dsl
                            l_callbackevt.setReason(ed.getValue());                             // 30-Jul-2012 dsl
                        } 
                    }
                    l_callbackevt.CreateEvent(XMSEventType.CALL_ALARM, this, "", l_callbackevt.getReason(), l_evt.toString()); // 30-Jul-2012 dsl
                    
                    //end hangup
                } else if (l_evt.eventType.contentEquals("message") ) {
                    logger.info("Processing message event");
                    EventData[] l_datalist=l_evt.event.getEventDataArray();// 30-Jul-2012 dsl
                    for(EventDataDocument.EventData ed: l_datalist){                            // 30-Jul-2012 dsl
                        if (ed.getName().contentEquals(EventDataName.REASON.toString())){                              // 30-Jul-2012 dsl
                            l_callbackevt.setReason(ed.getValue());                             // 30-Jul-2012 dsl
                        } else if (ed.getName().contentEquals(EventDataName.CONTENT.toString())){                              // 30-Jul-2012 dsl
                            l_callbackevt.setData(ed.getValue());                             // 30-Jul-2012 dsl
                            logger.info("setting data content to "+l_callbackevt.getData());
                        }
                        
                    }
                    l_callbackevt.CreateEvent(XMSEventType.CALL_MESSAGE, this, l_callbackevt.getData(), l_callbackevt.getReason(), l_evt.toString()); // 30-Jul-2012 dsl
                    UnblockIfNeeded(l_callbackevt);
                    //end hangup
                }else if (l_evt.eventType.contentEquals("end_send_message") ) {
                    logger.info("Processing end_send_message event");
                    EventData[] l_datalist=l_evt.event.getEventDataArray();// 30-Jul-2012 dsl
                    for(EventDataDocument.EventData ed: l_datalist){                            // 30-Jul-2012 dsl
                        if (ed.getName().contentEquals(EventDataName.REASON.toString())){                              // 30-Jul-2012 dsl
                            l_callbackevt.setReason(ed.getValue());                             // 30-Jul-2012 dsl
                        } 
                    }
                    setState(XMSCallState.CONNECTED);
                    l_callbackevt.CreateEvent(XMSEventType.CALL_SENDMESSAGE_END, this, "", l_callbackevt.getReason(), l_evt.toString()); // 30-Jul-2012 dsl
                    UnblockIfNeeded(l_callbackevt);
                    //end hangup
                }else {
                    logger.info("Unprocessed event type: " + l_evt.eventType);
                }
        }                     
    }

    
    /**
     * Build the payload string to update a call
     * 
     * @param a_destination - where you want to call.
     * 
     * @return Payload string  
     * 
     */
    
    private String buildUpdatecallPayload() {
        
        FunctionLogger logger=new FunctionLogger("buildUpdatecallPayload",this,m_logger);
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
        
        // Create a call instance
        Call l_call;

        // add a new call
        l_call = l_WMS.addNewCall();

        
      
           // Set ICE enabled parm
        if(UpdatecallOptions.m_iceEnabled) {
            l_call.setIce(BooleanType.YES);
        } else {
            l_call.setIce(BooleanType.NO);
        } // end if
     
           // Set ICE enabled parm
        if(UpdatecallOptions.m_encryptionEnabled) {
            l_call.setEncryption(RtpEncryptionOption.DTLS);
        } else {
            l_call.setEncryption(RtpEncryptionOption.NONE);
        } // end if
     
     
        // Set Media getType parm
        switch (UpdatecallOptions.m_mediaType) {
            case AUDIO:
                l_call.setMedia(MediaType.AUDIO);
                break;
            case VIDEO:
                l_call.setMedia(MediaType.AUDIOVIDEO);
                break;
            case UNKNOWN:
                l_call.setMedia(MediaType.UNKNOWN);
        } // end switch
        
        if(UpdatecallOptions.m_signalingEnabled){
            // Set the call attributes.
        
            if(getCallType() == XMSCallType.WEBRTC){
                logger.info("WebRTC call detected, setting dtmfmode = OUTOFBAND, ice=YES and encryption=dtls");
                l_call.setIce(BooleanType.YES);
                l_call.setEncryption(RtpEncryptionOption.DTLS);
                l_call.setDtmfMode(DtmfModeOption.OUTOFBAND);
            }
        } else {
                logger.info("3pcc call detected - sd= "+UpdatecallOptions.m_sdp.length());
                if(UpdatecallOptions.m_sdp.length() > 0){
                    l_call.setSdp("SDPPPLACEHOLDER12345");
                } else {
                    l_call.setSdp("");
                }
    //             l_call.setSignaling(BooleanType.NO);
        }
        //logger.debug("RAW REST generated...." + l_WMS.toString());
          ByteArrayOutputStream l_newDialog = new ByteArrayOutputStream();

        try {
            l_WMSdoc.save(l_newDialog);
            l_rqStr = l_WMSdoc.toString();

            } catch (IOException ex) {
            logger.error(ex);
        }
        
            String tmp=UpdatecallOptions.m_sdp;
            tmp=tmp.replaceAll("\r","&#xD;");
            tmp=tmp.replaceAll( "\n","&#xA;");
            l_rqStr=l_rqStr.replaceAll("SDPPPLACEHOLDER12345", UpdatecallOptions.m_sdp);
           
        //logger.debug ("Returning Payload:\n " + l_rqStr);
        return l_rqStr;  // Return the requested string...


    } // end buildUpdatecallPayload
    
    /**
     * Build the payload string to make a call
     * 
     * @param a_destination - where you want to call.
     * 
     * @return Payload string  
     * 
     */
    
    private String buildMakecallPayload(String a_destination) {
        
        FunctionLogger logger=new FunctionLogger("buildMakecallPayload",this,m_logger);
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
        
        // Create a call instance
        Call l_call;

        // add a new call
        l_call = l_WMS.addNewCall();

        
        
        
        // Set Media getType parm
        switch (MakecallOptions.m_mediaType) {
            case AUDIO:
                l_call.setMedia(MediaType.AUDIO);
                break;
            case VIDEO:
                l_call.setMedia(MediaType.AUDIOVIDEO);
                break;
            case MESSAGE:
                l_call.setMedia(MediaType.MESSAGE);
                break;
            case UNKNOWN:
                l_call.setMedia(MediaType.UNKNOWN);
        } // end switch
        
        if(MakecallOptions.m_mediaType != XMSMediaType.MESSAGE){
            // Set CPA enabled parm
            if(MakecallOptions.m_cpaEnabled) {
                l_call.setCpa(BooleanType.YES);
            } else {
                l_call.setCpa(BooleanType.NO);
            } // end if

               // Set ICE enabled parm
            if(MakecallOptions.m_iceEnabled) {
                l_call.setIce(BooleanType.YES);
            } else {
                l_call.setIce(BooleanType.NO);
            } // end if

               // Set ICE enabled parm
            if(MakecallOptions.m_encryptionEnabled) {
                l_call.setEncryption(RtpEncryptionOption.DTLS);
            } else {
                l_call.setEncryption(RtpEncryptionOption.NONE);
            } // end if


            if(MakecallOptions.m_signalingEnabled){
                // Set the call attributes.
                l_call.setDestinationUri(a_destination); // passed in..

                if(getCallType() == XMSCallType.WEBRTC){
                    logger.info("WebRTC call detected, setting dtmfmode = OUTOFBAND, ice=YES and encryption=dtls");
                    l_call.setIce(BooleanType.YES);
                    l_call.setEncryption(RtpEncryptionOption.DTLS);
                    l_call.setDtmfMode(DtmfModeOption.OUTOFBAND);
                }
            } else {
                    l_call.setSignaling(BooleanType.NO);
                    logger.info("3pcc call detected");
                    //l_call.setSdp(MakecallOptions.m_sdp);
                    if(MakecallOptions.m_sdp.length()>0){
                     l_call.setSdp("SDPPPLACEHOLDER12345");
                    } else {
                        l_call.setSdp("");
                    }
            }
        }else{
             
             if(MakecallOptions.m_signalingEnabled){
                 l_call.setDestinationUri(a_destination); // passed in..
             }else{
                   l_call.setSignaling(BooleanType.NO);
                    logger.info("3pcc call detected");
                    //l_call.setSdp(MakecallOptions.m_sdp);
                    if(MakecallOptions.m_sdp.length()>0){
                     l_call.setSdp("SDPPPLACEHOLDER12345");
                    } else {
                        l_call.setSdp("");
                    }
                  
             }
        }
        if(MakecallOptions.m_calledAddress.length()>0){
                 l_call.setCalledUri(MakecallOptions.m_calledAddress);
          }
        
        if(MakecallOptions.m_sourceAddress.length()>0){
                 l_call.setSourceUri(MakecallOptions.m_sourceAddress);
          }
        
        if(MakecallOptions.m_displayName.length()>0){
                 l_call.setDisplayName(MakecallOptions.m_displayName);
          }
        //logger.debug("RAW REST generated...." + l_WMS.toString());
          ByteArrayOutputStream l_newDialog = new ByteArrayOutputStream();

        try {
            l_WMSdoc.save(l_newDialog);
            l_rqStr = l_WMSdoc.toString();

            } catch (IOException ex) {
            logger.error(ex);
        }
        
        
            String tmp=MakecallOptions.m_sdp;
            tmp=tmp.replaceAll("\r","&#xD;");
            tmp=tmp.replaceAll( "\n","&#xA;");
            l_rqStr=l_rqStr.replaceAll("SDPPPLACEHOLDER12345", tmp);
         
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

        Call l_call; // Create a call instance
        CallAction l_callAction; // Call Action instance

        Play l_play;
        PlaySource l_playSource;

        // Create a new Web Service Doc Instance
        l_WMSdoc = WebServiceDocument.Factory.newInstance();
        l_WMS = l_WMSdoc.addNewWebService();

        // Create a new XMLToken Instance
        l_ver = XmlNMTOKEN.Factory.newInstance();
        l_ver.setStringValue("1.0");
        l_WMS.xsetVersion(l_ver);

        // add a new call
        l_call = l_WMS.addNewCall();

        // Add a new Call Action to the call
        l_callAction = l_call.addNewCallAction();

        // Add a new Play to the callAction
        l_play = l_callAction.addNewPlay();

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
               
                // TO DO: May need to append the MediaDefaultDirectory
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
                    l_playSource.setLocation("file://"+l_uristring);
                }
  //              logger.debug("Added [" + uriString + "]");
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

    /**
     * CLASS TYPE   :   private
     * METHOD       :   buildRecordPayload
     *
     * DESCRIPTION  :   Build the payload string to record a file(s)
     *
     * PARAMETERS   :   @param a_recfile - The filename to record too
     *
     *
     * RETURN       :   Payload string
     *
     * Author(s)    :   d.wolanski
     * Created      :   12-Jun-2012
     *
     *
     * HISTORY      :
     *************************************************************************/
    private String buildRecordPayload(String a_recfile) {
        FunctionLogger logger=new FunctionLogger("buildRecordPayload",this,m_logger);
        String l_rqStr = "";
        String uriString = "";

        WebServiceDocument l_WMSdoc;
        WebServiceDocument.WebService l_WMS;

        XmlNMTOKEN  l_ver;

        Call l_call; // Create a call instance
        CallAction l_callAction; // Call Action instance

        PlayrecordDocument.Playrecord l_record;
        

        // Create a new Web Service Doc Instance
        l_WMSdoc = WebServiceDocument.Factory.newInstance();
        l_WMS = l_WMSdoc.addNewWebService();

        // Create a new XMLToken Instance
        l_ver = XmlNMTOKEN.Factory.newInstance();
        l_ver.setStringValue("1.0");
        l_WMS.xsetVersion(l_ver);

        // add a new call
        l_call = l_WMS.addNewCall();

        // Add a new Call Action to the call
        l_callAction = l_call.addNewCallAction();

        // Add a new Play to the callAction
        l_record = l_callAction.addNewPlayrecord();
        
        // Add the record properties
        l_record.setOffset(RecordOptions.m_offset); 
        l_record.setDelay(RecordOptions.m_delay); 
        l_record.setRepeat(RecordOptions.m_repeat); 
        l_record.setTerminateDigits(RecordOptions.m_terminateDigits); 
        l_record.setMaxTime(RecordOptions.m_maxTime); // 16-Jun-2012 dsl
        
        if(!RecordOptions.m_timeoutValue.isEmpty()){
            l_record.setMaxTime(RecordOptions.m_timeoutValue);
        }
        if(RecordOptions.m_isBargeEnable){
           l_record.setBarge(BooleanType.YES);
        }else {
            l_record.setBarge(BooleanType.NO);
        }
        if(RecordOptions.m_clearDB){
           l_record.setCleardigits(BooleanType.YES);
        }else {
            l_record.setCleardigits(BooleanType.NO);
        }
        if(RecordOptions.m_isBeepEnabled){
           l_record.setBeep(BooleanType.YES);
        }else {
            l_record.setBeep(BooleanType.NO);
        }
        
        //l_record.setTransactionId(getCallIdentifier()+"_"+m_transactionId++); // Hard code this for now..
         
        l_record.setRecordingUri("file://"+a_recfile);
        
      

         
        //logger.debug("RAW REST generated...." + l_WMS.toString());
        ByteArrayOutputStream l_newDialog = new ByteArrayOutputStream();

        try {
            l_WMSdoc.save(l_newDialog);
            l_rqStr = l_WMSdoc.toString();

            } catch (IOException ex) {
            logger.error(ex);
        }

      //  logger.debug ("Returning Payload:\n " + l_rqStr);
        return l_rqStr;  // Return the requested string...
    } // end buildRecordPayload
    
    /**
     * Builds the PlayRecord XML payload
     * @param a_playfile
     * @param a_recfile
     * @return 
     */
private String buildPlayRecordPayload(String a_playfile,String a_recfile) {
    
        FunctionLogger logger=new FunctionLogger("buildPlayRecordPayload",this,m_logger);
        String l_rqStr = "";
        String uriString = "";

        WebServiceDocument l_WMSdoc;
        WebServiceDocument.WebService l_WMS;

        XmlNMTOKEN  l_ver;

        Call l_call; // Create a call instance
        CallAction l_callAction; // Call Action instance

        PlayrecordDocument.Playrecord l_record;
        

        // Create a new Web Service Doc Instance
        l_WMSdoc = WebServiceDocument.Factory.newInstance();
        l_WMS = l_WMSdoc.addNewWebService();

        // Create a new XMLToken Instance
        l_ver = XmlNMTOKEN.Factory.newInstance();
        l_ver.setStringValue("1.0");
        l_WMS.xsetVersion(l_ver);

        // add a new call
        l_call = l_WMS.addNewCall();

        // Add a new Call Action to the call
        l_callAction = l_call.addNewCallAction();

        // Add a new Play to the callAction
        l_record = l_callAction.addNewPlayrecord();
        
        // Add the record properties
        l_record.setOffset(PlayRecordOptions.m_offset); 
        l_record.setDelay(PlayRecordOptions.m_delay); 
        l_record.setRepeat(PlayRecordOptions.m_repeat); 
        l_record.setTerminateDigits(PlayRecordOptions.m_terminateDigits); 
        l_record.setMaxTime(PlayRecordOptions.m_maxTime); // 16-Jun-2012 dsl
        if(!PlayRecordOptions.m_timeoutValue.isEmpty()){
            l_record.setMaxTime(PlayRecordOptions.m_timeoutValue);
        }
        if(PlayRecordOptions.m_isBargeEnable){
           l_record.setBarge(BooleanType.YES);
        }else {
            l_record.setBarge(BooleanType.NO);
        }
        if(PlayRecordOptions.m_clearDB){
           l_record.setCleardigits(BooleanType.YES);
        }else {
            l_record.setCleardigits(BooleanType.NO);
        }
        if(PlayRecordOptions.m_isBeepEnabled){
           l_record.setBeep(BooleanType.YES);
        }else {
            l_record.setBeep(BooleanType.NO);
        }
        
        //l_record.setTransactionId(getCallIdentifier()+"_"+m_transactionId++); 
         
        l_record.setRecordingUri("file://"+a_recfile);
        
         // Setup your play list and put it in playsource
         PlaySource l_playSource;
         l_playSource = l_record.addNewPlaySource();
         l_playSource.setLocation("file://"+a_playfile);
                
        
        //logger.debug("RAW REST generated...." + l_WMS.toString());
        ByteArrayOutputStream l_newDialog = new ByteArrayOutputStream();

        try {
            l_WMSdoc.save(l_newDialog);
            l_rqStr = l_WMSdoc.toString();

            } catch (IOException ex) {
            logger.error(ex);
        }

      //  logger.debug ("Returning Payload:\n " + l_rqStr);
        return l_rqStr;  // Return the requested string...
    } // end buildRecordPayload

    /**
     * CLASS TYPE   :   private
     * METHOD       :   buildAnswercallPayload
     *
     * DESCRIPTION  :   Builds AnswerCall Payload
     *
     * PARAMETERS   :   None
     *
     *
     * RETURN       :   Payload string
     *
     * Author(s)    :   Dan Wolanski
     * Created      :   6/7/2012
     * Updated      :   6/7/2012
     *
     *
     * HISTORY      :
     *************************************************************************/
    private String buildAnswercallPayload() {
        FunctionLogger logger=new FunctionLogger("buildAnswercallPayload",this,m_logger);
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
        
        // Create a call instance
        Call l_call;
        // add a new call
        l_call = l_WMS.addNewCall();
        

        // Set Media getType parm
        switch (AnswercallOptions.m_mediatype) {
            case AUDIO:
                l_call.setMedia(MediaType.AUDIO);
                break;
            case VIDEO:
                l_call.setMedia(MediaType.AUDIOVIDEO);
                break;
            case MESSAGE:
                l_call.setMedia(MediaType.MESSAGE);
                break;
            
            case UNKNOWN:
                l_call.setMedia(MediaType.UNKNOWN);
        } // end switch

        l_call.setAnswer(BooleanType.YES);
        // all that is supported for answer on message type is answer and media
        if(AnswercallOptions.m_mediatype != XMSMediaType.MESSAGE){
        //    logger.debug("RAW REST generated...."+l_WMS.toString());
             if(getCallType() == XMSCallType.WEBRTC){
                logger.info("WebRTC call detected, setting dtmfmode = OUTOFBAND, ice=YES and encryption=dtls");
                l_call.setIce(BooleanType.YES);
                l_call.setEncryption(RtpEncryptionOption.DTLS);
                l_call.setDtmfMode(DtmfModeOption.OUTOFBAND);
            } else {
                 l_call.setDtmfMode(DtmfModeOption.RFC_2833);

             }
             l_call.setAsyncDtmf(BooleanType.YES);
             l_call.setInfoAckMode(AckModeOption.AUTOMATIC);
             l_call.setSignaling(BooleanType.YES);
        } else {
            logger.info("Setting to MediaType Message, omitting other default parms");
        }
        ByteArrayOutputStream l_newDialog = new ByteArrayOutputStream();

        try {
            l_WMSdoc.save(l_newDialog);
            l_rqStr = l_WMSdoc.toString();

            } catch (IOException ex) {
            logger.error(ex);
        }

     //   logger.debug ("Returning Payload:\n " + l_rqStr);
        return l_rqStr;  // Return the requested string...


    } // end buildAnswercallPayload
     /**
     * CLASS TYPE   :   private
     * METHOD       :   buildAnswercallPayload
     *
     * DESCRIPTION  :   Builds AcceptCall Payload
     *
     * PARAMETERS   :   None
     *
     *
     * RETURN       :   Payload string
     *
     * Author(s)    :   Dan Wolanski
     * Created      :   11/19/2013
     * Updated      :   11/19/2013
     *
     *
     * HISTORY      :
     *************************************************************************/
    private String buildAcceptcallPayload() {
        FunctionLogger logger=new FunctionLogger("buildAcceptcallPayload",this,m_logger);
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
        
        // Create a call instance
        Call l_call;
        // add a new call
        l_call = l_WMS.addNewCall();
        
        l_call.setAccept(BooleanType.YES);
         // Set Media getType parm
        switch (AcceptcallOptions.m_mediatype) {
            case AUDIO:
                l_call.setMedia(MediaType.AUDIO);
                break;
            case VIDEO:
                l_call.setMedia(MediaType.AUDIOVIDEO);
                break;
            case MESSAGE:
                l_call.setMedia(MediaType.MESSAGE);
                break;
            case UNKNOWN:
                l_call.setMedia(MediaType.UNKNOWN);
        } // end switch

        l_call.setEarlyMedia(BooleanType.YES);
        ByteArrayOutputStream l_newDialog = new ByteArrayOutputStream();

        try {
            l_WMSdoc.save(l_newDialog);
            l_rqStr = l_WMSdoc.toString();

            } catch (IOException ex) {
            logger.error(ex);
        }

     //   logger.debug ("Returning Payload:\n " + l_rqStr);
        return l_rqStr;  // Return the requested string...


    } // end buildAcceptcallPayload
    /**
     * CLASS TYPE   :   private
     * METHOD       :   buildUnattendedTransferPayload
     *
     * DESCRIPTION  :   Builds Transfer Payload
     *
     * PARAMETERS   :   None
     *
     *
     * RETURN       :   Payload string
     *
     * Author(s)    :   Dan Wolanski
     * Created      :   11/6/2013
     * Updated      :   11/7/2013
     *
     *
     * HISTORY      :
     *************************************************************************/
    private String buildUnattendedTransferPayload(String a_uri) {
        FunctionLogger logger=new FunctionLogger("buildUnattendedTransferPayload",this,m_logger);
        String l_rqStr = "";

        WebServiceDocument l_WMSdoc;
        WebServiceDocument.WebService l_WMS;
        XmlNMTOKEN  l_ver; 

        // Create a new Web Service Doc Instance
        l_WMSdoc = WebServiceDocument.Factory.newInstance();
        l_WMS = l_WMSdoc.addNewWebService();

        Call l_call; // Create a call instance
        CallAction l_callAction; // Call Action instance

        //PlayrecordDocument.Playrecord l_record;
        TransferDocument.Transfer l_xfer;

        // Create a new Web Service Doc Instance
        l_WMSdoc = WebServiceDocument.Factory.newInstance();
        l_WMS = l_WMSdoc.addNewWebService();

        // Create a new XMLToken Instance
        l_ver = XmlNMTOKEN.Factory.newInstance();
        l_ver.setStringValue("1.0");
        l_WMS.xsetVersion(l_ver);

        // add a new call
        l_call = l_WMS.addNewCall();

        // Add a new Call Action to the call
        l_callAction = l_call.addNewCallAction();

        // Add a new Transfer to the callAction
        l_xfer = l_callAction.addNewTransfer();
        
        // Add the Transfer properties
        l_xfer.setUri(a_uri);
        
         
        ByteArrayOutputStream l_newDialog = new ByteArrayOutputStream();

        try {
            l_WMSdoc.save(l_newDialog);
            l_rqStr = l_WMSdoc.toString();

            } catch (IOException ex) {
            logger.error(ex);
        }

     //   logger.debug ("Returning Payload:\n " + l_rqStr);
        return l_rqStr;  // Return the requested string...


    } // end buildUnattendedTransfercallPayload
    /**
     * CLASS TYPE   :   private
     * METHOD       :   buildRedirectPayload
     *
     * DESCRIPTION  :   Builds Transfer Payload
     *
     * PARAMETERS   :   String to redirect too
     *
     *
     * RETURN       :   Payload string
     *
     * Author(s)    :   Dan Wolanski
     * Created      :   11/6/2013
     * Updated      :   11/7/2013
     *
     *
     * HISTORY      :
     *************************************************************************/
    private String buildRedirectPayload(String a_uri) {
        FunctionLogger logger=new FunctionLogger("buildRedirectPayload",this,m_logger);
        String l_rqStr = "";

        WebServiceDocument l_WMSdoc;
        WebServiceDocument.WebService l_WMS;
        XmlNMTOKEN  l_ver; 

        // Create a new Web Service Doc Instance
        l_WMSdoc = WebServiceDocument.Factory.newInstance();
        l_WMS = l_WMSdoc.addNewWebService();

        Call l_call; // Create a call instance
        CallAction l_callAction; // Call Action instance

        //PlayrecordDocument.Playrecord l_record;
        RedirectDocument.Redirect l_redirect;

        // Create a new Web Service Doc Instance
        l_WMSdoc = WebServiceDocument.Factory.newInstance();
        l_WMS = l_WMSdoc.addNewWebService();

        // Create a new XMLToken Instance
        l_ver = XmlNMTOKEN.Factory.newInstance();
        l_ver.setStringValue("1.0");
        l_WMS.xsetVersion(l_ver);

        // add a new call
        l_call = l_WMS.addNewCall();

        // Add a new Call Action to the call
        l_callAction = l_call.addNewCallAction();

        // Add a new Transfer to the callAction
        l_redirect = l_callAction.addNewRedirect();
        
        // Add the Transfer properties
        l_redirect.setUri(a_uri);
        
         
        ByteArrayOutputStream l_newDialog = new ByteArrayOutputStream();

        try {
            l_WMSdoc.save(l_newDialog);
            l_rqStr = l_WMSdoc.toString();

            } catch (IOException ex) {
            logger.error(ex);
        }

     //   logger.debug ("Returning Payload:\n " + l_rqStr);
        return l_rqStr;  // Return the requested string...


    } // end buildRedirectPayload
  /**
     * CLASS TYPE   :   private
     * METHOD       :   buildRedirectPayload
     *
     * DESCRIPTION  :   Builds Transfer Payload
     *
     * PARAMETERS   :   String to redirect too
     *
     *
     * RETURN       :   Payload string
     *
     * Author(s)    :   Dan Wolanski
     * Created      :   11/6/2013
     * Updated      :   11/7/2013
     *
     *
     * HISTORY      :
     *************************************************************************/
    private String buildSendMessagePayload(String a_message) {
        FunctionLogger logger=new FunctionLogger("buildSendMessagePayload",this,m_logger);
        String l_rqStr = "";

        WebServiceDocument l_WMSdoc;
        WebServiceDocument.WebService l_WMS;
        XmlNMTOKEN  l_ver; 

        // Create a new Web Service Doc Instance
        l_WMSdoc = WebServiceDocument.Factory.newInstance();
        l_WMS = l_WMSdoc.addNewWebService();

        Call l_call; // Create a call instance
        CallAction l_callAction; // Call Action instance

        //PlayrecordDocument.Playrecord l_record;
        SendMessageDocument.SendMessage l_sendmessage;

        // Create a new Web Service Doc Instance
        l_WMSdoc = WebServiceDocument.Factory.newInstance();
        l_WMS = l_WMSdoc.addNewWebService();

        // Create a new XMLToken Instance
        l_ver = XmlNMTOKEN.Factory.newInstance();
        l_ver.setStringValue("1.0");
        l_WMS.xsetVersion(l_ver);

        // add a new call
        l_call = l_WMS.addNewCall();

        // Add a new Call Action to the call
        l_callAction = l_call.addNewCallAction();

        // Add a new Transfer to the callAction
        l_sendmessage = l_callAction.addNewSendMessage();
        
        l_sendmessage.setContentType(SendMessageOptions.m_contentType);
        l_sendmessage.setReport(SendMessageDocument.SendMessage.Report.BOTH);
        l_sendmessage.setContent(a_message);
        ByteArrayOutputStream l_newDialog = new ByteArrayOutputStream();

        try {
            l_WMSdoc.save(l_newDialog);
            l_rqStr = l_WMSdoc.toString();

            } catch (IOException ex) {
            logger.error(ex);
        }

       // logger.debug ("Returning Payload:\n " + l_rqStr);
        return l_rqStr;  // Return the requested string...


    } // end buildSendMessage
          
  /**
     * CLASS TYPE   :   private
     * METHOD       :   buildAttendedTransferPayload
     *
     * DESCRIPTION  :   Builds Transfer Payload
     *
     * PARAMETERS   :   None
     *
     *
     * RETURN       :   Payload string
     *
     * Author(s)    :   Dan Wolanski
     * Created      :   11/6/2013
     * Updated      :   11/7/2013
     *
     *
     * HISTORY      :
     *************************************************************************/
    private String buildAttendedTransferPayload(XMSCall a_call) {
        FunctionLogger logger=new FunctionLogger("buildAttendedTransferPayload",this,m_logger);
        String l_rqStr = "";

        WebServiceDocument l_WMSdoc;
        WebServiceDocument.WebService l_WMS;
        XmlNMTOKEN  l_ver; 

        // Create a new Web Service Doc Instance
        l_WMSdoc = WebServiceDocument.Factory.newInstance();
        l_WMS = l_WMSdoc.addNewWebService();

        Call l_call; // Create a call instance
        CallAction l_callAction; // Call Action instance

        //PlayrecordDocument.Playrecord l_record;
        TransferDocument.Transfer l_xfer;

        // Create a new Web Service Doc Instance
        l_WMSdoc = WebServiceDocument.Factory.newInstance();
        l_WMS = l_WMSdoc.addNewWebService();

        // Create a new XMLToken Instance
        l_ver = XmlNMTOKEN.Factory.newInstance();
        l_ver.setStringValue("1.0");
        l_WMS.xsetVersion(l_ver);

        // add a new call
        l_call = l_WMS.addNewCall();

        // Add a new Call Action to the call
        l_callAction = l_call.addNewCallAction();

        // Add a new Transfer to the callAction
        l_xfer = l_callAction.addNewTransfer();
        
        // Add the Transfer properties
        l_xfer.setCallId(a_call.getCallIdentifier());
        
         
        ByteArrayOutputStream l_newDialog = new ByteArrayOutputStream();

        try {
            l_WMSdoc.save(l_newDialog);
            l_rqStr = l_WMSdoc.toString();

            } catch (IOException ex) {
            logger.error(ex);
        }

     //   logger.debug ("Returning Payload:\n " + l_rqStr);
        return l_rqStr;  // Return the requested string...


    } // end buildAttendedTransfercallPayload
      
    /**
     * CLASS TYPE   :   private
     * METHOD       :   buildJoinPayload
     *
     * DESCRIPTION  :   Builds Join Payload
     *
     * PARAMETERS   :   @param a_othercallid - Builds the other string
     *
     *
     * RETURN       :   Payload string
     *
     * Author(s)    :   Dan Wolanski
     * Created      :   6/8/2012
     * Updated      :   6/8/2012
     *
     *
     * HISTORY      :
     *************************************************************************/
    private String buildJoinPayload(String a_othercallid) {
        FunctionLogger logger=new FunctionLogger("buildJoinPayload",this,m_logger);
       
        String l_rqStr = "";


        WebServiceDocument l_WMSdoc;
        WebServiceDocument.WebService l_WMS;

        XmlNMTOKEN  l_ver;

        Call l_call; // Create a call instance
        CallAction l_callAction; // Call Action instance

        Join l_join;
        PlaySource l_playSource;

        // Create a new Web Service Doc Instance
        l_WMSdoc = WebServiceDocument.Factory.newInstance();
        l_WMS = l_WMSdoc.addNewWebService();

        // Create a new XMLToken Instance
        l_ver = XmlNMTOKEN.Factory.newInstance();
        l_ver.setStringValue("1.0");
        l_WMS.xsetVersion(l_ver);

        // add a new call
        l_call = l_WMS.addNewCall();

        
        // Add a new Call Action to the call
        l_callAction = l_call.addNewCallAction();

        // Add a new Play to the callAction
        l_join = l_callAction.addNewJoin();

        l_join.setCallId(a_othercallid);
       // l_join.setTransactionId(getCallIdentifier()+"_"+m_transactionId++); 
        //l_join.setTransactionId(""); // Hard code this for now..


       // logger.debug("RAW REST generated...." + l_WMS.toString());
        ByteArrayOutputStream l_newDialog = new ByteArrayOutputStream();

        try {
            l_WMSdoc.save(l_newDialog);
            l_rqStr = l_WMSdoc.toString();

            } catch (IOException ex) {
            logger.error(ex);
        }

      //  logger.debug ("Returning Payload:\n " + l_rqStr);
        return l_rqStr;  // Return the requested string...

    } // end buildJoinPayload
     /**
     * CLASS TYPE   :   private
     * METHOD       :   buildJoinPayload
     *
     * DESCRIPTION  :   Builds Join Payload
     *
     * PARAMETERS   :   @param a_othercallid - Builds the other string
     *
     *
     * RETURN       :   Payload string
     *
     * Author(s)    :   Dan Wolanski
     * Created      :   6/8/2012
     * Updated      :   6/8/2012
     *
     *
     * HISTORY      :
     *************************************************************************/
    private String buildUnJoinPayload() {
        FunctionLogger logger=new FunctionLogger("buildUnJoinPayload",this,m_logger);
       
        String l_rqStr = "";


        WebServiceDocument l_WMSdoc;
        WebServiceDocument.WebService l_WMS;

        XmlNMTOKEN  l_ver;

        Call l_call; // Create a call instance
        CallAction l_callAction; // Call Action instance

        Unjoin l_unjoin;
        
        // Create a new Web Service Doc Instance
        l_WMSdoc = WebServiceDocument.Factory.newInstance();
        l_WMS = l_WMSdoc.addNewWebService();

        // Create a new XMLToken Instance
        l_ver = XmlNMTOKEN.Factory.newInstance();
        l_ver.setStringValue("1.0");
        l_WMS.xsetVersion(l_ver);

        // add a new call
        l_call = l_WMS.addNewCall();

        
        // Add a new Call Action to the call
        l_callAction = l_call.addNewCallAction();

        // Add a new Play to the callAction
        l_unjoin = l_callAction.addNewUnjoin();
        l_unjoin.setCallId(getCallIdentifier());
       // l_join.setTransactionId(getCallIdentifier()+"_"+m_transactionId++); 
        //l_join.setTransactionId(""); // Hard code this for now..


       // logger.debug("RAW REST generated...." + l_WMS.toString());
        ByteArrayOutputStream l_newDialog = new ByteArrayOutputStream();

        try {
            l_WMSdoc.save(l_newDialog);
            l_rqStr = l_WMSdoc.toString();

            } catch (IOException ex) {
            logger.error(ex);
        }

      //  logger.debug ("Returning Payload:\n " + l_rqStr);
        return l_rqStr;  // Return the requested string...

    } // end buildUnJoinPayload

         /**
     * CLASS TYPE   :   private
     * METHOD       :   buildStopPayload
     *
     * DESCRIPTION  :   Builds Stop Payload
     *
     *
     *
     * RETURN       :   Payload string
     *
     * Author(s)    :   Dan Wolanski
     * Created      :   6/8/2012
     * Updated      :   6/8/2012
     *
     *
     * HISTORY      :
     *************************************************************************/
    private String buildStopPayload() {
        FunctionLogger logger=new FunctionLogger("buildStopPayload",this,m_logger);
       
        String l_rqStr = "";


        WebServiceDocument l_WMSdoc;
        WebServiceDocument.WebService l_WMS;

        XmlNMTOKEN  l_ver;

        Call l_call; // Create a call instance
        CallAction l_callAction; // Call Action instance

        Stop l_stop;
        
        // Create a new Web Service Doc Instance
        l_WMSdoc = WebServiceDocument.Factory.newInstance();
        l_WMS = l_WMSdoc.addNewWebService();

        // Create a new XMLToken Instance
        l_ver = XmlNMTOKEN.Factory.newInstance();
        l_ver.setStringValue("1.0");
        l_WMS.xsetVersion(l_ver);

        // add a new call
        l_call = l_WMS.addNewCall();

        
        // Add a new Call Action to the call
        l_callAction = l_call.addNewCallAction();

        // Add a new Play to the callAction
        l_stop = l_callAction.addNewStop();
        l_stop.setTransactionId(m_pendingtransactionInfo.getTransactionId()); 


       // logger.debug("RAW REST generated...." + l_WMS.toString());
        ByteArrayOutputStream l_newDialog = new ByteArrayOutputStream();

        try {
            l_WMSdoc.save(l_newDialog);
            l_rqStr = l_WMSdoc.toString();

            } catch (IOException ex) {
            logger.error(ex);
        }

      //  logger.debug ("Returning Payload:\n " + l_rqStr);
        return l_rqStr;  // Return the requested string...

    } // end buildStopPayload
   /**
     * CLASS TYPE   :   private
     * METHOD       :   buildAddPartyPayload
     *
     * DESCRIPTION  :   Builds Add Party Payload
     *
     *
     *
     * RETURN       :   Payload string
     *
     * Author(s)    :   Dan Wolanski
     * Created      :   6/8/2012
     * Updated      :   6/8/2012
     *
     *
     * HISTORY      :
     *************************************************************************/
    private String buildAddPartyPayload(XMSConference a_conf) {
        FunctionLogger logger=new FunctionLogger("buildAddPartyPayload",this,m_logger);
       
        String l_rqStr = "";


        WebServiceDocument l_WMSdoc;
        WebServiceDocument.WebService l_WMS;

        XmlNMTOKEN  l_ver;

        Call l_call; // Create a call instance
        CallAction l_callAction; // Call Action instance

        AddParty l_addparty;
        
        // Create a new Web Service Doc Instance
        l_WMSdoc = WebServiceDocument.Factory.newInstance();
        l_WMS = l_WMSdoc.addNewWebService();

        // Create a new XMLToken Instance
        l_ver = XmlNMTOKEN.Factory.newInstance();
        l_ver.setStringValue("1.0");
        l_WMS.xsetVersion(l_ver);

        // add a new call
        l_call = l_WMS.addNewCall();

        
        // Add a new Call Action to the call
        l_callAction = l_call.addNewCallAction();

        // Add a new Play to the callAction
        l_addparty = l_callAction.addNewAddParty();
        l_addparty.setConfId(a_conf.getCallIdentifier());
        //TODO change this to take real options
        if(a_conf.AddOptions.m_audiodirection == XMSMediaDirection.INACTIVE) l_addparty.setAudio(MediaDirection.INACTIVE);
        else if(a_conf.AddOptions.m_audiodirection == XMSMediaDirection.RECVONLY) l_addparty.setAudio(MediaDirection.RECVONLY);
        else if(a_conf.AddOptions.m_audiodirection == XMSMediaDirection.SENDONLY) l_addparty.setAudio(MediaDirection.SENDONLY);
        else if(a_conf.AddOptions.m_audiodirection == XMSMediaDirection.SENDRECV) l_addparty.setAudio(MediaDirection.SENDRECV);
        else if(a_conf.AddOptions.m_audiodirection == XMSMediaDirection.AUTOMATIC) l_addparty.setAudio(MediaDirection.SENDRECV);
        
        if(a_conf.AddOptions.m_videodirection == XMSMediaDirection.INACTIVE) l_addparty.setVideo(MediaDirection.INACTIVE);
        else if(a_conf.AddOptions.m_videodirection == XMSMediaDirection.RECVONLY) l_addparty.setVideo(MediaDirection.RECVONLY);
        else if(a_conf.AddOptions.m_videodirection == XMSMediaDirection.SENDONLY) l_addparty.setVideo(MediaDirection.SENDONLY);
        else if(a_conf.AddOptions.m_videodirection == XMSMediaDirection.SENDRECV) l_addparty.setVideo(MediaDirection.SENDRECV);
        //TODO need to get AUTOMATIC mode working by adding a check to see if the call is a video call or not, for not just setting it to SENDRECV by default
        else if(a_conf.AddOptions.m_videodirection == XMSMediaDirection.AUTOMATIC) l_addparty.setVideo(MediaDirection.SENDRECV);
        
        if(a_conf.AddOptions.m_caption==""){
            l_addparty.setCaption(getConnectionAddress());
        } else{
            l_addparty.setCaption(a_conf.AddOptions.m_caption);
        }
        
       //l_addparty.setVideo(MediaDirection.INACTIVE);
       // l_addparty.setVideo(MediaDirection.SENDRECV);
        


       // logger.debug("RAW REST generated...." + l_WMS.toString());
        ByteArrayOutputStream l_newDialog = new ByteArrayOutputStream();

        try {
            l_WMSdoc.save(l_newDialog);
            l_rqStr = l_WMSdoc.toString();

            } catch (IOException ex) {
            logger.error(ex);
        }

      //  logger.debug ("Returning Payload:\n " + l_rqStr);
        return l_rqStr;  // Return the requested string...

    } // end buildAddPartyPayload

    /**
     * CLASS TYPE   :   private
     * METHOD       :   buildRemovePartyPayload
     *
     * DESCRIPTION  :   Builds Remove Party Payload
     *
     *
     *
     * RETURN       :   Payload string
     *
     * Author(s)    :   Dan Wolanski
     * Created      :   6/8/2012
     * Updated      :   6/8/2012
     *
     *
     * HISTORY      :
     *************************************************************************/
    private String buildRemovePartyPayload(XMSConference a_conf) {
        FunctionLogger logger=new FunctionLogger("buildRemovePartyPayload",this,m_logger);
       
        String l_rqStr = "";


        WebServiceDocument l_WMSdoc;
        WebServiceDocument.WebService l_WMS;

        XmlNMTOKEN  l_ver;

        Call l_call; // Create a call instance
        CallAction l_callAction; // Call Action instance

        RemoveParty l_removeparty;
        
        // Create a new Web Service Doc Instance
        l_WMSdoc = WebServiceDocument.Factory.newInstance();
        l_WMS = l_WMSdoc.addNewWebService();

        // Create a new XMLToken Instance
        l_ver = XmlNMTOKEN.Factory.newInstance();
        l_ver.setStringValue("1.0");
        l_WMS.xsetVersion(l_ver);

        // add a new call
        l_call = l_WMS.addNewCall();

        
        // Add a new Call Action to the call
        l_callAction = l_call.addNewCallAction();

        // Add a new Play to the callAction
        l_removeparty = l_callAction.addNewRemoveParty();
        l_removeparty.setConfId(a_conf.getCallIdentifier());
        
       // logger.debug("RAW REST generated...." + l_WMS.toString());
        ByteArrayOutputStream l_newDialog = new ByteArrayOutputStream();

        try {
            l_WMSdoc.save(l_newDialog);
            l_rqStr = l_WMSdoc.toString();

            } catch (IOException ex) {
            logger.error(ex);
        }

      //  logger.debug ("Returning Payload:\n " + l_rqStr);
        return l_rqStr;  // Return the requested string...

    } // end buildAddPartyPayload
 
    /**
     * CLASS TYPE   :   private
     * METHOD       :   buildCollectPayload
     *
     * DESCRIPTION  :   Build the payload string to collect digit(s). Supports multiple files
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
    private String buildCollectPayload() {
        FunctionLogger logger=new FunctionLogger("buildCollectPayload",this,m_logger);
        
        String l_rqStr = "";
        String uriString = "";

        WebServiceDocument l_WMSdoc;
        WebServiceDocument.WebService l_WMS;

        XmlNMTOKEN  l_ver;

        Call l_call; // Create a call instance
        CallAction l_callAction; // Call Action instance             
        
        Playcollect l_playCollect;                

        // Create a new Web Service Doc Instance
        l_WMSdoc = WebServiceDocument.Factory.newInstance();
        l_WMS = l_WMSdoc.addNewWebService();

        // Create a new XMLToken Instance
        l_ver = XmlNMTOKEN.Factory.newInstance();
        l_ver.setStringValue("1.0");
        l_WMS.xsetVersion(l_ver);

        // add a new call
        l_call = l_WMS.addNewCall();

        // Add a new Call Action to the call
        l_callAction = l_call.addNewCallAction();
        
        // Add a new play collect to the call Action
        l_playCollect = l_callAction.addNewPlaycollect();


        // Should keep this hardcoded. Not sure if needed..
        l_playCollect.setBarge(BooleanType.YES); // hardcode this for now..
        l_playCollect.setOffset("0s"); // hardcode this for now..
        l_playCollect.setDelay("0s"); // hardcode this for now..
        l_playCollect.setRepeat("0"); // hardcode this for now..
        

        // Set the placycollect attributes
        if(CollectDigitsOptions.m_clearDB==true) {
            l_playCollect.setCleardigits(BooleanType.YES);            
        } else { 
            l_playCollect.setCleardigits(BooleanType.NO);            
        }
        
        //TODO test with not term digit or maxdig as terms when useing the options
        l_playCollect.setTerminateDigits(CollectDigitsOptions.m_terminateDigits); // hardcode this for now..
        l_playCollect.setMaxDigits(CollectDigitsOptions.m_maxDigits); // hardcode this for now..
        if(!RecordOptions.m_timeoutValue.isEmpty()){
            l_playCollect.setTimeout(CollectDigitsOptions.m_timeoutValue); // hardcode this for now..
        }
        //l_playCollect.setTransactionId(getCallIdentifier()+"_"+m_transactionId++); 
        //l_playCollect.setTransactionId(uriString); // hardcode this for now..
        

   
        // NOTE: This is a collect. No play added

       // logger.debug("RAW REST generated...." + l_WMS.toString());
        ByteArrayOutputStream l_newDialog = new ByteArrayOutputStream();

        try {
            l_WMSdoc.save(l_newDialog);
            l_rqStr = l_WMSdoc.toString();

            } catch (IOException ex) {
            logger.error(ex);
        }

       // logger.debug ("Returning Payload:\n " + l_rqStr);
        return l_rqStr;  // Return the requested string...
    } // end buildCollectPayload
    /**
     * 
     * @param a_playfile
     * @return 
     */
    private String buildPlayCollectPayload(String a_playfile) {
        FunctionLogger logger=new FunctionLogger("buildPlayCollectPayload",this,m_logger);
        
        String l_rqStr = "";
        String uriString = "";
        WebServiceDocument l_WMSdoc;
        WebServiceDocument.WebService l_WMS;

        XmlNMTOKEN  l_ver;

        Call l_call; // Create a call instance
        CallAction l_callAction; // Call Action instance             
        
        Playcollect l_playCollect;                

        // Create a new Web Service Doc Instance
        l_WMSdoc = WebServiceDocument.Factory.newInstance();
        l_WMS = l_WMSdoc.addNewWebService();

        // Create a new XMLToken Instance
        l_ver = XmlNMTOKEN.Factory.newInstance();
        l_ver.setStringValue("1.0");
        l_WMS.xsetVersion(l_ver);

        // add a new call
        l_call = l_WMS.addNewCall();

        // Add a new Call Action to the call
        l_callAction = l_call.addNewCallAction();
        
        // Add a new play collect to the call Action
        l_playCollect = l_callAction.addNewPlaycollect();


        l_playCollect.setOffset(PlayCollectOptions.m_offset); // hardcode this for now..
        l_playCollect.setDelay(PlayCollectOptions.m_delay); // hardcode this for now..
        l_playCollect.setRepeat(PlayCollectOptions.m_repeat); // hardcode this for now..
        

        // Set the placycollect attributes
        if(PlayCollectOptions.m_clearDB==true) {
            l_playCollect.setCleardigits(BooleanType.YES);            
        } else { 
            l_playCollect.setCleardigits(BooleanType.NO);            
        }
          // Set the PlayCollect attributes
        if(PlayCollectOptions.m_isBargeEnable==true) {
            l_playCollect.setBarge(BooleanType.YES);            
        } else { 
            l_playCollect.setBarge(BooleanType.NO);            
        }
        
        // Set the Tone Detection Options.   15-Jun-2012 dsl
        if(PlayCollectOptions.m_toneDetection==true) {
            l_playCollect.setToneDetection(BooleanType.YES) ;
        } else { 
            l_playCollect.setToneDetection(BooleanType.NO);            
        }

        
        l_playCollect.setTerminateDigits(PlayCollectOptions.m_terminateDigits); // hardcode this for now..
        l_playCollect.setMaxDigits(PlayCollectOptions.m_maxDigits); // hardcode this for now..
        if(!PlayCollectOptions.m_timeoutValue.isEmpty()){
            l_playCollect.setTimeout(PlayCollectOptions.m_timeoutValue); // hardcode this for now..
        }
        //l_playCollect.setTransactionId(getCallIdentifier()+"_"+m_transactionId++); 
        

   
           // Setup your play list and put it in playsource
         PlaySource l_playSource;
         l_playSource = l_playCollect.addNewPlaySource();
         l_playSource.setLocation("file://"+a_playfile);
                
        
       // logger.debug("RAW REST generated...." + l_WMS.toString());
        ByteArrayOutputStream l_newDialog = new ByteArrayOutputStream();

        try {
            l_WMSdoc.save(l_newDialog);
            l_rqStr = l_WMSdoc.toString();

            } catch (IOException ex) {
            logger.error(ex);
        }

       // logger.debug ("Returning Payload:\n " + l_rqStr);
        return l_rqStr;  // Return the requested string...
    } // end buildPlayCollectPayload
    
    
     /**
      * Playback phrases
      * @param a_type - The type of phrase to be spoken
      * @param a_phrase - The phrase to be spoken
      * @return 
      */
    @Override
     public XMSReturnCode PlayPhrase(XMSPlayPhraseType a_type, String a_phrase){
         //<play_source audio_uri=”var://locale=en-US;type=dig;subtype=ndn;value=12345678;”/> 
          FunctionLogger logger=new FunctionLogger("PlayPhrase",this,m_logger);
        logger.args("Type "+a_type+" " + "Phrase "+a_phrase+" "+PlayPhraseOptions);
        String XMLPAYLOAD;
        SendCommandResponse RC ;

        String l_urlext;
        l_urlext = "calls/" + m_callIdentifier;
        
       
        XMLPAYLOAD = buildPlayPhrasePayload(a_type,a_phrase); 

        //logger.info("Sending message ---->  " + XMLPAYLOAD);
        RC = m_connector.SendCommand(this,RESTOPERATION.PUT, l_urlext, XMLPAYLOAD);
        
         if (RC.get_scr_status_code() == 200){
            m_pendingtransactionInfo.setDescription("Play phrase="+a_phrase+" type="+a_type);
            m_pendingtransactionInfo.setTransactionId(RC.get_scr_transaction_id());
            m_pendingtransactionInfo.setResponseData(RC);
            
            setState(XMSCallState.PLAY);
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
        
     }
     private String buildPlayPhrasePayload(XMSPlayPhraseType a_type, String a_phrase){
        FunctionLogger logger=new FunctionLogger("buildPlayPhrase",this,m_logger);
        
        String l_rqStr = "";
        String uriString = "";

        WebServiceDocument l_WMSdoc;
        WebServiceDocument.WebService l_WMS;

        XmlNMTOKEN  l_ver;

        Call l_call; // Create a call instance
        CallAction l_callAction; // Call Action instance

        Play l_play;
       // Playcollect l_play;
        
        PlaySource l_playSource;

        // Create a new Web Service Doc Instance
        l_WMSdoc = WebServiceDocument.Factory.newInstance();
        l_WMS = l_WMSdoc.addNewWebService();

        // Create a new XMLToken Instance
        l_ver = XmlNMTOKEN.Factory.newInstance();
        l_ver.setStringValue("1.0");
        l_WMS.xsetVersion(l_ver);

        // add a new call
        l_call = l_WMS.addNewCall();

        // Add a new Call Action to the call
        l_callAction = l_call.addNewCallAction();

        // Add a new Play to the callAction
        //l_play = l_callAction.addNewPlaycollect();  
        l_play =l_callAction.addNewPlay();

       
        l_play.setTerminateDigits(PlayPhraseOptions.m_terminateDigits); 
      //  l_play.setMaxDigits(CollectDigitsOptions.m_maxDigits); // hardcode this for now..

        
        
//      
        l_playSource = l_play.addNewPlaySource();
        
        uriString="var://"+"locale="+PlayPhraseOptions.m_locale+
                ";voice="+PlayPhraseOptions.m_voice +
                ";type="+a_type.name().toLowerCase()+
                ";subtype="+PlayPhraseOptions.GetSubtypeFormat(a_type)+
                ";value="+a_phrase+
                ";";
        l_playSource.setLocation(uriString);
       
                
        

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

     }
     /**
      * Play a period of silence 
      * @param a_duration - 0 – 36000 (in 100 ms units up to 1 hour)
      * @return 
      */
     @Override
     public XMSReturnCode PlaySilence(int a_duration){
          //<play_source audio_uri=”var://locale=en-US;type=dig;subtype=ndn;value=12345678;”/> 
          FunctionLogger logger=new FunctionLogger("PlaySilence",this,m_logger);
        logger.args("Duration "+a_duration);
        String XMLPAYLOAD;
        SendCommandResponse RC ;

        String l_urlext;
        l_urlext = "calls/" + m_callIdentifier;
        
       
        XMLPAYLOAD = buildPlayPhrasePayload(XMSPlayPhraseType.SILENCE,Integer.toString(a_duration)); 

        //logger.info("Sending message ---->  " + XMLPAYLOAD);
        RC = m_connector.SendCommand(this,RESTOPERATION.PUT, l_urlext, XMLPAYLOAD);
        
         if (RC.get_scr_status_code() == 200){
            m_pendingtransactionInfo.setDescription("Play Silence duration="+a_duration);
            m_pendingtransactionInfo.setTransactionId(RC.get_scr_transaction_id());
            m_pendingtransactionInfo.setResponseData(RC);
            
            setState(XMSCallState.PLAY);
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
     }
    
public static String unescapeXML( final String xml )
{
    Pattern xmlEntityRegex = Pattern.compile( "&(#?)([^;]+);" );
    //Unfortunately, Matcher requires a StringBuffer instead of a StringBuilder
    StringBuffer unescapedOutput = new StringBuffer( xml.length() );

    Matcher m = xmlEntityRegex.matcher( xml );
    Map<String,String> builtinEntities = null;
    String entity;
    String hashmark;
    String ent;
    int code;
    while ( m.find() ) {
        ent = m.group(2);
        hashmark = m.group(1);
        if ( (hashmark != null) && (hashmark.length() > 0) ) {
            code = Integer.parseInt( ent );
            entity = Character.toString( (char) code );
        } else {
            //must be a non-numerical entity
            if ( builtinEntities == null ) {
                builtinEntities = buildBuiltinXMLEntityMap();
            }
            entity = builtinEntities.get( ent );
            if ( entity == null ) {
                //not a known entity - ignore it
                entity = "&" + ent + ';';
            }
        }
        m.appendReplacement( unescapedOutput, entity );
    }
    m.appendTail( unescapedOutput );

    return unescapedOutput.toString();
}

private static Map<String,String> buildBuiltinXMLEntityMap()
{
    Map<String,String> entities = new HashMap<String,String>(10);
    entities.put( "lt", "<" );
    entities.put( "gt", ">" );
    entities.put( "amp", "&" );
    entities.put( "apos", "'" );
    entities.put( "quot", "\"" );
    return entities;
}
}
