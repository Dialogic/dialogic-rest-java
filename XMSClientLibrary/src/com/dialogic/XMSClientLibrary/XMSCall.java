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
/**
 *
 * @author dslopresti
 * @author dwolanski
 * @author chinck
 */
public abstract class XMSCall extends XMSObject{


    /* Logger information */
     private static Logger m_logger = Logger.getLogger(XMSCall.class.getName());
     
     protected XMSCallState m_state;
     
     private XMSCallType m_calltype = XMSCallType.UNKNOWN;
     private String m_connectionaddress = null;
     private String m_calledaddress = null;
     public XMSMakecallOptions MakecallOptions = new XMSMakecallOptions();
     public XMSUpdatecallOptions UpdatecallOptions= new XMSUpdatecallOptions();
     public XMSWaitcallOptions WaitcallOptions = new XMSWaitcallOptions();
     public XMSAnswercallOptions AnswercallOptions = WaitcallOptions;  
     public XMSAcceptcallOptions AcceptcallOptions = new XMSAcceptcallOptions();
     //public XMSAnswercallOptions AnswercallOptions = new XMSAnswercallOptions();  
     public XMSRecordOptions RecordOptions = new XMSRecordOptions();
     public XMSPlayOptions PlayOptions = new XMSPlayOptions();
     public XMSCollectDigitsOptions CollectDigitsOptions = new XMSCollectDigitsOptions();
     public XMSPlayCollectOptions PlayCollectOptions = new XMSPlayCollectOptions();
     public XMSPlayRecordOptions PlayRecordOptions = new XMSPlayRecordOptions();
     public XMSPlayPhraseOptions PlayPhraseOptions = new XMSPlayPhraseOptions();
     public XMSSendMessageOptions SendMessageOptions = new XMSSendMessageOptions();
     public XMSSendInfoOptions SendInfoOptions = new XMSSendInfoOptions();
     /**
      * CTor for the Object.  Default takes no parms
      */
     public XMSCall(){
        m_Name = "XMSCall:"+m_objectcounter;
        PropertyConfigurator.configure("log4j.properties");
        //m_logger.setLevel(Level.ALL);
        m_logger.info("Creating " + m_Name);
        m_state = XMSCallState.NULL;
        
     }
     /**
      * Returns the address of the other endpoint the call is connected to.  This will be the
      * address of the endpoint originating the call on a waitcall scenario or the destination address
      * in a Makecall scenario.  THis address is cleared when the call is disconnected
      * @return - ConnectionAddress
      */
     public String getConnectionAddress(){
         return m_connectionaddress;
     }
     protected  void setConnectionAddress(String a_addr){
         FunctionLogger logger=new FunctionLogger("setConnectionAddress",this,m_logger);
         logger.args(a_addr);
         m_connectionaddress = a_addr;
     }
     
     /**
      * Returns the address Called to establish the connection.  This will be the
      * address calledpartyURI on the waitcall  scenario or the destination address
      * in a Makecall scenario.  THis address is cleared when the call is disconnected
      * @return - ConnectionAddress
      */
     public String getCalledAddress(){
         return m_calledaddress;
     }
     protected  void setCalledAddress(String a_addr){
         FunctionLogger logger=new FunctionLogger("setCalledAddress",this,m_logger);
         logger.args(a_addr);
         m_calledaddress = a_addr;
     }
     
     /**
      * Returns the Current type of the call 
      * may be SIP, WEBRTC or UNKNOWN
      * @return
      */
     public XMSCallType getCallType()
    {
        return m_calltype;
    }
     protected void setCallType( XMSCallType a_type){
         FunctionLogger logger=new FunctionLogger("setCallType",this,m_logger);
         logger.args(a_type);
         m_calltype=a_type;
     }
     /**
      * Returns the Current state of the object
      * @return
      */
     public XMSCallState getState()
    {
        return m_state;
    }
/**
      * Sets the Current state of the object.
      * @param a_newState
      * @return
      * Warning: This should not be changed by external entities as it may
      * alter the state
      */
     // TODO Add in logic to check state.. ie can't Make a call while in a call etc
    protected XMSCallState setState(XMSCallState a_newState)
    {
        FunctionLogger logger=new FunctionLogger("setState",this,m_logger);
        logger.info("Setting state to "+a_newState +" (previous state = "+m_state+")");
        m_state = a_newState;
        return m_state;
    }


    public XMSReturnCode Makecall(String dest){
        //throw new UnsupportedOperationException("Not supported yet.");
        return XMSReturnCode.NOT_IMPLEMENTED;
    }
    /**
     * Join / Route 2 Calls together
     * @param a_othercall
     * @return 
     */
    public XMSReturnCode Join( XMSCall a_othercall){
        return XMSReturnCode.NOT_IMPLEMENTED;
    }
       /**
     * Join / Route 2 Calls together
     * @return 
     */
    public XMSReturnCode UnJoin( ){
        return XMSReturnCode.NOT_IMPLEMENTED;
    }
    /** Join / Route 2 Calls together
     * @return 
     */
    public XMSReturnCode UnRoute( ){
        return UnJoin();
    }
    /**
     * Stop an active media Call on a channel
     * @return 
     */
    public XMSReturnCode Stop(){
        return XMSReturnCode.NOT_IMPLEMENTED;
    }
     /**
     * Redirect an incoming call destination
     * NOTE To use this you need to turn off AUTO ANSWER
     * @param a_dest - URI for the destination
     * @return 
     */
    public XMSReturnCode Redirect( String a_dest){
        return XMSReturnCode.NOT_IMPLEMENTED;
    }
      /**
     * Unattended / Unsupervised transfer to destination
     * @param a_dest - URI for the destination
     * @return 
     */
    public XMSReturnCode Transfer( String a_dest){
        return XMSReturnCode.NOT_IMPLEMENTED;
    }
      /**
     * Supervised transfer to another connected call
     * @param a_call - XMSCall Object of a connectd call
     * @return 
     */
    public XMSReturnCode Transfer( XMSCall a_call){
        return XMSReturnCode.NOT_IMPLEMENTED;
    }
    /**
     * Join / Route 2 Calls together (note this simply a wrapper to Join() 
     * @param a_othercall
     * @return 
     */
    public XMSReturnCode Route( XMSCall a_othercall){
        return Join(a_othercall);
    }
    /**
     * Record a file
     * @param a_file
     * @return 
     */
     public XMSReturnCode Record(String a_file){
 
        return XMSReturnCode.NOT_IMPLEMENTED; 
    }
     /**
      * This file will start a Play and a record at the same time
      * @param a_playfile
      * @param recfile
      * @return 
      */
     public XMSReturnCode PlayRecord(String a_playfile,String recfile){
 
        return XMSReturnCode.NOT_IMPLEMENTED; 
    }
    /**
     * Play a file
     * @param a_file - File to be played
     * @return
     */
     public XMSReturnCode Play(String a_file){

        ArrayList<String> l_playlist = new ArrayList<String>();
        l_playlist.add(a_file);
        return PlayList(l_playlist);
    }
     /**
     * Play a file
     * @param a_file - File to be played
     * @return
     */
     public XMSReturnCode PlayList(ArrayList<String> a_playlist){

        return XMSReturnCode.NOT_IMPLEMENTED;
    }
    /**
     * This will terminate an call.  The call could be in progress or
     * in the middle of being established.
     *
     * @return
     */
     public XMSReturnCode Dropcall(){

        return XMSReturnCode.NOT_IMPLEMENTED;
    }

    /**
     * Puts the call in a state waiting for a new inbound call
     * @return 
     */
    public XMSReturnCode Waitcall(){

        return XMSReturnCode.NOT_IMPLEMENTED;
    }
    /**
     * This Answer an incoming call.  
     *
     * @return
     */
     public XMSReturnCode Answercall(){

        return XMSReturnCode.NOT_IMPLEMENTED;
    }
     /**
     * This Accept an incoming call, but not answer it
     *
     * @return
     */
     public XMSReturnCode Acceptcall(){

        return XMSReturnCode.NOT_IMPLEMENTED;
    }
     /**
      * Collect DTMF digits
      * @return 
      */
     public XMSReturnCode CollectDigits(){

        return XMSReturnCode.NOT_IMPLEMENTED;
    }
      /*
       * 
       */
     public XMSReturnCode PlayCollect(String a_playfile){
         //TODO Is this better called PlayAndCollect or PlayAndCollectDigits
        return XMSReturnCode.NOT_IMPLEMENTED;
    } 
     
     public XMSReturnCode Updatecall(){
         return XMSReturnCode.NOT_IMPLEMENTED;
     }
     /**
      * Playback phrases
      * @param a_type - The type of phrase to be spoken
      * @param a_phrase - The phrase to be spoken
      * @return 
      */
     public XMSReturnCode PlayPhrase(XMSPlayPhraseType a_type, String a_phrase){
         return XMSReturnCode.NOT_IMPLEMENTED;
     }
     /**
      * Play a period of silence 
      * @param a_duration - 0 – 36000 (in 100 ms units up to 1 hour)
      * @return 
      */
     public XMSReturnCode PlaySilence(int a_duration){
         return XMSReturnCode.NOT_IMPLEMENTED;
     }
     /**
      * Send out the message using MRCP 
      * @param a_message - String that is to be sent
      * @return 
      */
     public XMSReturnCode SendMessage(String a_message){
         return XMSReturnCode.NOT_IMPLEMENTED;
     }
     /**
      * Send out the DTMF string to the network 
      * Will terminate with a CALL_END_DTMF event
      * @param a_dialstring - String that is to be sent
      * @return 
      */
     public XMSReturnCode SendDtmf(String a_dialstring){
         return XMSReturnCode.NOT_IMPLEMENTED;
     }
    /**
      * Send out the message using MRCP 
      * @param a_message - String that is to be sent
      * @return 
      */
     public XMSReturnCode SendInfo(String a_message){
         return XMSReturnCode.NOT_IMPLEMENTED;
     }
}
