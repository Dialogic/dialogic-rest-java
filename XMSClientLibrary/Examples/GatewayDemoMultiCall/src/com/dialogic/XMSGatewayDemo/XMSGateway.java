package com.dialogic.XMSGatewayDemo;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import com.dialogic.XMSClientLibrary.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

enum InboundCallStates{
    UNKNOWN,                //Initialized state
    WAITCALL,               //Waiting for an inbound call
    ACCEPTCALL,             //Accepted the call
    ANSWERCALL,             //Answer the Call
    CALLCONNECTED,          //Call is connected
    PLAYRINGBACK,           //Playback Ringing to the Party
    JOINED,                 //Connected to Outbound call
    DISCONNECTING,          //Call in process of disconnected
    DISCONNECTED            //Call disconnected waiting for far end to finish to go back into wait
}
enum OutboundCallStates{
    UNKNOWN,                //Initialized state
    IDLE,                   //Call allocated but IDLE
    MAKECALL,               //Making an outbound call
    CALLCONNECTED,          //Call connected
    JOINED,                 //Connected to Inbound Call
    DISCONNECTING,          //Call in process of disconnected
    DISCONNECTED            //Call Disconnected waitting for far end to finish
}

enum GatewayStates{
    UNINITIALIZED,          //Initial State where the device is unilitialize or not started
    WAITING,                //Devices allocated and INBOUND call waiting on inbound call
    ESTABLISHING,           //Establishing calls (answering inbound and making outbound) 
    UNJOINED,               //Calls are connected but not Joined together
    CONNECTED,              //Calls are connected and Joined together
    DISCONNECTING,          //Disconnecting the calls
    CLEANUP                 //Any post processing cleanup that needs to be done before returning to WAIT

}

/**
 *
 * @author dwolansk
 */
public class XMSGateway implements XMSEventCallback {
    
    XMSCall myInboundCall;
    XMSCall myOutboundCall;
    XMSRestConnector restcon; 
    
    InboundCallStates myInboundCallState = InboundCallStates.UNKNOWN;
    OutboundCallStates myOutboundCallState = OutboundCallStates.UNKNOWN;
    GatewayStates myState = GatewayStates.UNINITIALIZED;
    
     protected static int m_objectcounter=1;  // Used to provide a unique name
     protected String myName;
     private static Logger myLogger = Logger.getLogger(XMSGateway.class.getName());
     int CallNumber=0;
     
    @Override
    public String toString()
    {
        return myName+"("+myState+")";
    }
    /**
     * Default Constructor
     */
    public XMSGateway(){
        myName = "XMSGateway:"+m_objectcounter++;

        PropertyConfigurator.configure("log4j.properties");
        myLogger.setLevel(Level.ALL);
        myLogger.info("Creating " + myName);
        
    }
    /**
     * Constructor that intializes the Calls
     * @param in
     * @param out 
     */
    public XMSGateway(XMSCall in, XMSCall out, XMSRestConnector myRestcon){
        myName = "XMSGateway:"+m_objectcounter++;

        PropertyConfigurator.configure("log4j.properties");
        myLogger.setLevel(Level.ALL);
        myLogger.info("Creating " + myName);
        
        restcon = myRestcon; 
        
        SetInboundCall(in);
        SetOutboundCall(out);
           
    }
    boolean ConnectInboundFlag=true;
    /**
     * Set if the gateway should connect the inbound call prior to making the outbound call
     * If this is set to false the gateway will only accept the call and will not answer until
     * outbound party answers
     * @param option (Default=true)
     */
    public void SetConnectInbound(boolean option){
        FunctionLogger logger=new FunctionLogger("SetConnectInbound",this,myLogger);
        logger.args(option);
        ConnectInboundFlag=option;
    }
    
    boolean PlayRingbackFlag = false;
    /**
     * Set if the gateway should playback ringback to the inbound caller while 
     * trying to connect the outbound caller. Note that this parm will only be in effect
     * if the SetConnectInbound is set to true
     * @param option (Default = true)
     */
    public void SetPlayRingback(boolean option){
         FunctionLogger logger=new FunctionLogger("SetPlayRingback",this,myLogger);
        logger.args(option);
        PlayRingbackFlag=option;
    }
    
    String RingbackFile="Ringback";
    /**
     * Set the filename to be used for Ringback
     * @param filename 
     */
    public void SetRingbackFile(String filename){
        FunctionLogger logger=new FunctionLogger("SetRingbackFile",this,myLogger);
        logger.args(filename);
        
        RingbackFile = filename;
    }
    
    boolean RecordCallFlag = false;
    /**
     * Set if the gateway should playback ringback to the inbound caller while 
     * trying to connect the outbound caller. Note that this parm will only be in effect
     * if the SetConnectInbound is set to true
     * @param option (Default = false)
     */
    public void SetCallRecording(boolean option){
        FunctionLogger logger=new FunctionLogger("SetCallRecording",this,myLogger);
        logger.args(option);
        PlayRingbackFlag=option;
    }
     String myRtcRegistrarAddress=null;
     public void SetRtcRegistrarAddress(String addr){
         FunctionLogger logger=new FunctionLogger("SetRtcRegistrarAddress",this,myLogger);
        logger.args(addr);
        
        myRtcRegistrarAddress=addr;
     }
     String mySipRegistrarAddress=null;   
     
     public void SetSipRegistrarAddress(String addr){
         FunctionLogger logger=new FunctionLogger("SetSipRegistrarAddress",this,myLogger);
        logger.args(addr);
        
        mySipRegistrarAddress=addr;
     }
    private void WaitForNewCall(){
         FunctionLogger logger=new FunctionLogger("WaitForNewCall",this,myLogger);
        myInboundCall.WaitcallOptions.EnableAutoConnect(ConnectInboundFlag);
        myInboundCall.WaitcallOptions.SetMediaType(XMSMediaType.AUDIO);
        SetInboundCallState(InboundCallStates.WAITCALL);
        
        SetGatewayState(GatewayStates.WAITING);
        myInboundCall.Waitcall();
    }
    public void Start(){
        SetOutboundCallState(OutboundCallStates.IDLE);
        FunctionLogger logger=new FunctionLogger("Start",this,myLogger);
        WaitForNewCall();
        
    }
    public void Stop(){
        FunctionLogger logger=new FunctionLogger("Stop",this,myLogger);
        myInboundCall.Dropcall();
        myOutboundCall.Dropcall();
    }
    /**
     * Set the inbound Call object
     * @param in 
     */
    public void SetInboundCall(XMSCall in){
        FunctionLogger logger=new FunctionLogger("SetInboundCall",this,myLogger);
        logger.args(in);
        myInboundCall = in;
        myInboundCall.EnableAllEvents(this);
    }
    /**
     * Set the outbound Call object
     * @param out
     */
    public void SetOutboundCall(XMSCall out){
        FunctionLogger logger=new FunctionLogger("SetOutboundCall",this,myLogger);
        logger.args(out);
        myOutboundCall = out;
        myOutboundCall.EnableAllEvents(this);
    }
    
    private void SetGatewayState(GatewayStates newstate){
        FunctionLogger logger=new FunctionLogger("SetGatewayState",this,myLogger);
        logger.args(newstate);
        
        myState=newstate;
            
    }
    private void SetInboundCallState(InboundCallStates newstate){
        FunctionLogger logger=new FunctionLogger("SetInboundCallState",this,myLogger);
        logger.args(newstate);
        
        myInboundCallState=newstate;
            
    }
    private void SetOutboundCallState(OutboundCallStates newstate){
        FunctionLogger logger=new FunctionLogger("SetOutboundCallState",this,myLogger);
        logger.args(newstate);
        
        myOutboundCallState=newstate;
            
    }
    /**
     * Connect the Two inbound calls
     */
    private void ConnectCalls(){
        FunctionLogger logger=new FunctionLogger("SetOutboundCallState",this,myLogger);
        
        //I think if we want to support the Transaction logging we need to throw them in the conf here
        // for now lets just join them
        if(PlayRingbackFlag){
            myInboundCall.Stop();
        } 
        SetInboundCallState(InboundCallStates.JOINED);
        SetOutboundCallState(OutboundCallStates.JOINED);
        SetGatewayState(GatewayStates.CONNECTED);
        myInboundCall.Join(myOutboundCall);
                
    }
        private void UnJoinCalls(){
        FunctionLogger logger=new FunctionLogger("UnJoin calls",this,myLogger);
        
        //I think if we want to support the Transaction logging we need to throw them in the conf here
        // for now lets just join them
        SetInboundCallState(InboundCallStates.CALLCONNECTED);
        SetOutboundCallState(OutboundCallStates.CALLCONNECTED);
        SetGatewayState(GatewayStates.CONNECTED);
        myInboundCall.UnJoin();
                
    }
        
        public void SendDTMF(String message){
        
            myOutboundCall.SendDtmf(message);
        
    }   
    
    private void DisconnectCalls(){
                System.out.println("*****   DisconnectCalls   *****");

        if(myOutboundCallState != OutboundCallStates.IDLE){
            SetOutboundCallState(OutboundCallStates.DISCONNECTING);
            myOutboundCall.Dropcall();
            SetOutboundCallState(OutboundCallStates.DISCONNECTED);
            
            SetOutboundCallState(OutboundCallStates.IDLE);
        }
        if(myInboundCallState != InboundCallStates.WAITCALL){
            SetInboundCallState(InboundCallStates.DISCONNECTING);
            myInboundCall.Dropcall();
            SetInboundCallState(InboundCallStates.DISCONNECTED);
            WaitForNewCall();
        }
    }
    /**
     * This is the default Event Handler for the XMSEventCallback Implementation
     * @param evt XMSEvent to be processed
     */
    @Override
    public void ProcessEvent(XMSEvent evt){
        FunctionLogger logger=new FunctionLogger("ProcessEvent",this,myLogger);
        logger.args(evt);
        if(evt.getCall() == myInboundCall){
            ProcessInboundEvent(evt);
        } else if(evt.getCall() == myOutboundCall){
            ProcessOutboundEvent(evt);
        } else{
            logger.error("Event Received for an unknown Call.  Ignoring");
        }
        
    }
    /**
     * Process function for all Events on the Outbound device
     * @param evt 
     */
    protected void ProcessOutboundEvent(XMSEvent evt){
        switch(evt.getEventType()){
            case CALL_CONNECTED:
                System.out.println("*****  Outbound call connected   *****");
                SetOutboundCallState(OutboundCallStates.CALLCONNECTED);
                ConnectCalls();
                break;
            case CALL_RECORD_END:
                
                break;       
            case CALL_SENDDTMF_END:
                System.out.println("*****  END_DTMF   *****");
                System.out.println("*****  ReJoin Calls  *****");
                ConnectCalls();
                    
                break;    
            case CALL_DTMF:
                    
                System.out.println("*****  outbound CALL_DTMF   *****");
                System.out.println("data=" + evt.getData());
                myInboundCall.SendInfo("DTMF_" + evt.getData());
                          
                break;   
             case CALL_INFO:
                     
                System.out.println("*****   Outbound CALL_INFO   *****");
                System.out.println("data=" + evt.getData());
                myInboundCall.SendInfo(evt.getData());

                break;
            case CALL_PLAY_END:
                //May need to do something here
                break;
            case CALL_DISCONNECTED:  // The far end hung up will simply wait for the media
                System.out.println("*****  outbound CALL_Disconnected   *****");
                DisconnectCalls();
                break;
            default:
                System.out.println("Unknown Event Type!!");
        }
    }
    /**
     * Process Function for all the Events on the Inbound device
     * @param evt 
     */
    protected void ProcessInboundEvent(XMSEvent evt){
        
            FunctionLogger logger=new FunctionLogger("ProcessInboundEvent",this,myLogger);
            logger.args(evt);
            try{
            switch(evt.getEventType()){
                case CALL_OFFERED:
                    System.out.println("*****   Inbound CALL_Offered   *****");
                    SetGatewayState(GatewayStates.ESTABLISHING);
                    SetInboundCallState(InboundCallStates.ACCEPTCALL);
                    //Should there ba an accept Call

                    StartOutboundCall(evt.getCall().getCalledAddress(),evt.getCall().getConnectionAddress());
             
                    break;
                case CALL_CONNECTED:
                    System.out.println("*****   Inbound CALL_Connected   *****");
                    SetInboundCallState(InboundCallStates.CALLCONNECTED);
                    StartOutboundCall(evt.getCall().getCalledAddress(),evt.getCall().getConnectionAddress());
                    if(PlayRingbackFlag ){
                        myInboundCall.PlayOptions.SetMediaType(XMSMediaType.VIDEO);
                        myInboundCall.Play(RingbackFile);
                    } 
                    break;
                    
                case CALL_INFO:
                     
                    System.out.println("*****   Inbound CALL_INFO   *****");
                    System.out.println("data=" + evt.getData());
                    myOutboundCall.SendInfo(evt.getData());
                    
                    break;
                case CALL_DTMF:
                    
                    System.out.println("*****  inbound CALL_DTMF   *****");
                    System.out.println("data=" + evt.getData());
                    
                    UnJoinCalls(); 
                    SendDTMF(evt.getData()); 
                    
                    break;                    
                case CALL_SENDDTMF_END:
                    
                    System.out.println("*****  END_DTMF   *****");
                    System.out.println("*****  ReJoin Calls  *****");
                    ConnectCalls();
                    
                    break;
                    
                case CALL_RECORD_END:
                    break;                
                case CALL_PLAY_END:
                    break;
                case CALL_DISCONNECTED:  
                    System.out.println("*****  inbound CALL_Disconnected   *****");
                    DisconnectCalls();
                    break;
                default:
                    System.out.println("Unknown Event Type!!");
            }
        }catch(XMSGatewayOutboundCallException e){
            logger.info("Error is received",e);
        }
    }

    protected XMSReturnCode StartOutboundCall(String inString, String CallingID) throws XMSGatewayOutboundCallException{
        FunctionLogger logger=new FunctionLogger("StartOutboundCall",this,myLogger);
        logger.args(inString);
        
            String OutboundAddress = BuildOutboundAddress(inString); 
            String OutboundCallingAddress = BuildOutboundCallingAddress(CallingID);
          
            
            myOutboundCallState=OutboundCallStates.MAKECALL;
            myOutboundCall.MakecallOptions.SetMediaType(XMSMediaType.AUDIO);
            myOutboundCall.MakecallOptions.setSourceAddress(OutboundCallingAddress);
            myOutboundCall.Makecall(OutboundAddress);
            
            return XMSReturnCode.SUCCESS;        
            
    }
    protected String BuildOutboundAddress(String in){
         FunctionLogger logger=new FunctionLogger("BuildOutboundAddress",this,myLogger);
        String OutboundName=GetOutboundNameFromCalledAddress(in);
        String sipServerIP;
        String retstring; 
        
        if(myInboundCall.getCallType()==XMSCallType.SIP){
            int colon=OutboundName.lastIndexOf(":");
            String name;
            if(colon>-1){
                name=OutboundName.substring(colon);
            } else {
                name=OutboundName;
            }
            name=name.trim();
            name=name.replaceAll("^\"|\"$", "").replaceAll("^'|'$", "");
            retstring="rtc"+name;
            
            if(myRtcRegistrarAddress!=null){
                retstring+="@"+myRtcRegistrarAddress;
            }
        } else {
            int atsign=in.indexOf("@");
            if(atsign == -1){
                           sipServerIP=mySipRegistrarAddress;
            }else{
              sipServerIP=in.substring(atsign+1);
              sipServerIP=sipServerIP.replaceAll("\"","");
            }
          
            int colon=OutboundName.lastIndexOf(":");
            String name;
            if(colon>-1){
                name=OutboundName.substring(colon);
            } else {
                name=OutboundName;
            }
            name=name.trim();
            name=name.replaceAll("^\"|\"$", "").replaceAll("^'|'$", "");
            retstring="sip"+name;
            
            if(sipServerIP!=null){
                retstring+="@"+sipServerIP;
            }
        }
        logger.info("Return String="+retstring);
        return retstring;
    }
        protected String BuildOutboundCallingAddress(String in){
         FunctionLogger logger=new FunctionLogger("BuildOutboundCallingAddress",this,myLogger);
        String OutboundCallingName=GetOutboundCallingNameFromCallingAddress(in);
        String retstring; 
        

            int colon=OutboundCallingName.lastIndexOf(":");
            String name;
            if(colon>-1){
                name=OutboundCallingName.substring(colon);
            } else {
                name=OutboundCallingName;
            }
          
            name=name.trim();
            name=name.replaceAll("^\"|\"$", "").replaceAll("^'|'$", "");
            retstring=name;

            
       
        logger.info("Return String="+retstring);
        return retstring;
    }
    
    protected XMSCallType GetCallTypeFromOutboundName(String name){
        if(name.contains("rtc:")){
            return XMSCallType.WEBRTC;
        } else {
            return XMSCallType.SIP;
        }
    }
    protected String GetOutboundNameFromCalledAddress(String calledAddress){
        String name;
          int atsign=calledAddress.indexOf("@");
          if(atsign == -1){
                           name=calledAddress;
            }else{
              name=calledAddress.substring(0,atsign);
          }
          
          int colon=name.lastIndexOf(":");
          
          if (colon >= 3 ){
              return name.substring(colon-3);
          } else {
                  return name;
          }
        
    }
     protected String GetOutboundCallingNameFromCallingAddress(String callingAddress){
        String name;
                    System.out.println("*****  Get Calling Name   *****");
                    System.out.println("CallingAddress = " + callingAddress);        
          int atsign=callingAddress.indexOf("@");
                   System.out.println("atsign = " + atsign);        
          if(atsign == -1){
                           name=callingAddress;
            }else{
              name=callingAddress.substring(0,atsign);
          }
          
          int colon=name.indexOf(":");
                 System.out.println("colon = " + colon);        
          if (colon == -1){
              return name;
          } else {
                  return name.substring(colon+1);
          }
        
    }
    
}
    

