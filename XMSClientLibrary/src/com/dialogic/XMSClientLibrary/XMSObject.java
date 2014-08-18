/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dialogic.XMSClientLibrary;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;  /* this is Event Handler */

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
public abstract class XMSObject extends Observable implements Observer{


    /* Logger information */
     private static Logger m_logger = Logger.getLogger(XMSObject.class.getName());
     protected String m_callIdentifier;


     protected static int m_objectcounter=1;  // Used to provide a unique name
     protected String m_Name;
   //  protected XMSCallState m_state;
     protected XMSConnector m_connector;
     protected String m_type; // Technology type IE REST, MSML etc

     private boolean m_isSyncMode;
     private boolean m_isBlocked;
     private final Object m_synclock = new Object();
     private String m_connectionaddress = null;
     private XMSEvent m_lastevt;
     protected EnumMap<XMSEventType,XMSEventCallback> m_eventcallbackmap;
     
     /**
      * CTor for the Object.  Default takes no parms
      */
     public XMSObject(){
        m_Name = "XMSObject:"+m_objectcounter;
        PropertyConfigurator.configure("log4j.properties");
        //m_logger.setLevel(Level.ALL);
        m_logger.info("Creating " + m_Name);
        m_logger.info("Object is in SYNC mode");
        m_isSyncMode = false;
        m_isBlocked = false;
        m_eventcallbackmap = new EnumMap<XMSEventType,XMSEventCallback>(XMSEventType.class);
        
     }
   
       /**
      * CTor for the Object.  Default takes no parms
      */
     public XMSObject(XMSConnector a_conn){
        m_Name = "XMSObject:"+m_objectcounter;
        PropertyConfigurator.configure("log4j.properties");
        //m_logger.setLevel(Level.ALL);
        m_logger.info("Creating " + m_Name);
        m_logger.info("Object is in SYNC mode");
        m_isSyncMode = false;
        m_isBlocked = false;
        m_eventcallbackmap = new EnumMap<XMSEventType,XMSEventCallback>(XMSEventType.class);
        m_logger.info("Setting connector to "+a_conn);
        m_connector=a_conn;
     }
   
     

    /**
     * The Call Identifier for the object
     * @return
     */
    public String getCallIdentifier()
    {
        return m_callIdentifier;
    }
    /**
     * Used by the Connector to set the getCallIdentifier
     * @param a_id 
     */
    protected void SetCallIdentifier(String a_id){
        FunctionLogger logger=new FunctionLogger("SetCallIdentifier",this,m_logger);
        logger.info("Setting CallIdentifier to "+a_id);
        m_callIdentifier=a_id;
    }
 
    /**
     *
     * @return
     */
    public String getType(){

        return m_type;
    }
    /**
     *
     * @return
     */
    public String Name()
    {
        return toString();
    }


    @Override
    public String toString()
    {
        return m_Name+"("+m_callIdentifier+")";
    }
    /**
     * Initialize the XMSConnector
     * @param a_conn
     */
    public void Initialize(){
        FunctionLogger logger=new FunctionLogger("Initialize",this,m_logger);
         /*
          * logger.info("Setting connector to " + a_conn.toString());
         m_connector=a_conn;
        */
     }
    /**
     * Used to post the Notify to the observers by another class
     * @param a_obj 
     */
    public void UpdateAndNotify(Object a_obj){
        FunctionLogger logger=new FunctionLogger("Initialize",this,m_logger);
        logger.args(toString());
        logger.info("Entering UpdateAndNotify");
        setChanged();
        notifyObservers(a_obj);
        
    }
    /**
     * IsSyncMode will return if the object is in sync mode or
     * if the app callbacks should be used  When in sync mode 
     * each of the XMSCall objects will not return until the response
     * is returned from the connector.
     * @return 
     */
    public boolean isSyncMode(){
        return m_isSyncMode;
    }
    /**
     * Used to tell is the device is blocking waiting on 
     * and event to be returned
     * @return 
     */
    public boolean isBlocked(){
        
        return m_isBlocked;
    }
    /**
     * Block the execution thread if needed for sync mode or if there
     * are no events registered for the condition
     */
    protected void BlockIfNeeded() throws InterruptedException{
        FunctionLogger logger=new FunctionLogger("BlockIfNeeded",this,m_logger);
        if(!isSyncMode()){
            logger.info("Blocking is NOT needed, resuming");
        } else {
            logger.info("Blocking waiting on Notify");
            m_isBlocked=true;
            synchronized(m_synclock){
                while(isBlocked()){
                    m_synclock.wait();
                }
            }
            
        }
    }
       /**
     * Block the execution thread if needed for sync mode or if there
     * are no events registered for the condition
     */
    protected void BlockIfNeeded(XMSEventType a_event) throws InterruptedException{
        FunctionLogger logger=new FunctionLogger("BlockIfNeeded",this,m_logger);
        logger.args("Waiting for "+a_event);
        
        if(!isSyncMode()){
            if(m_eventcallbackmap.containsKey(a_event)){
                logger.info("Callback is registered for "+a_event+" - NOT Blocking ");
                return;
            }  else {
                logger.info("No Callback registered for event, running in sync mode");
            }
        }
        // if is Sync mode or if there is no callback resistered    
         logger.info("Blocking waiting on Notify");
            m_isBlocked=true;
            synchronized(m_synclock){
                while(isBlocked()){
                    m_synclock.wait();
                }
            }
            
        
    }
    /**
     * Block the execution thread if needed for sync mode or if there
     * are no events registered for the condition
     */
    protected void UnblockIfNeeded(){
        FunctionLogger logger=new FunctionLogger("UnblockIfNeeded",this,m_logger);
        if(isBlocked()){
            synchronized(m_synclock){
                //set ready flag to true (so isBlocked returns false)
                m_isBlocked = false;
                logger.info("Sending Notify and Unblocking Object");
                m_synclock.notifyAll();
            } 
        }else{
                logger.info("Object is NOT blocked, no Notify Needed");
            }
            
       
        
    }
    
    /**
     * THis function will return the last event that was 
     * dispatched by the call object.  
     * @return 
     */
    public XMSEvent getLastEvent(){
        return m_lastevt;
    }
    /**
     * THis function will sets the last event that was 
     * dispatched by the call object.  
     * @return 
     */
    public void setLastEvent(XMSEvent a_evt){
        m_lastevt=a_evt;
    }
        /**
     * Block the execution thread if needed for sync mode or if there
     * are no events registered for the condition
     */
    protected void UnblockIfNeeded(XMSEvent a_evt){
        
        FunctionLogger logger=new FunctionLogger("UnblockIfNeeded",this,m_logger);
        logger.args(a_evt);
        m_lastevt=a_evt;
        if(isBlocked()){
            synchronized(m_synclock){
                //set ready flag to true (so isBlocked returns false)
                m_isBlocked = false;
                logger.info("Sending Notify and Unblocking Object");
                m_synclock.notifyAll();
            } 
        }else{
                logger.info("Object is NOT blocked, posting senting event to EventDistributor");
                m_connector.PostCallEvent(a_evt);
            }
            
       
        
    }
    protected void DispatchXMSEvent(XMSEvent a_evt){
        FunctionLogger logger=new FunctionLogger("DispatchXMSEvent",this,m_logger);
        //TODO: We perhaps should have some event distribution thread rather then calling callback directly
        XMSEventCallback l_callback = m_eventcallbackmap.get(a_evt.getEventType());
        if(l_callback != null){
            logger.info("Calling Callback for "+a_evt);
            l_callback.ProcessEvent(a_evt);
        } else {
            logger.info("No Callback for this event, just destroying the event");
           //do we need to clean up?
        }
    }
     
    /**
     * This is used as the Callback for the Observer to the Connection to hand back events
     * @param obj
     * @param arg 
     */ 
    public void update(Observable obj, Object arg) {
        FunctionLogger logger=new FunctionLogger("update",this,m_logger);
        logger.info("update has been called Arg="+arg.toString());
    }
    /**
     * This enables the Application Callback for the specified event
     * @param a_event
     * @param a_callback 
     */
    public void EnableEvent(XMSEventType a_event, XMSEventCallback a_callback){
        FunctionLogger logger=new FunctionLogger("EnableEvent",this,m_logger);
        logger.args(a_event,a_callback);
        
        if(m_eventcallbackmap.containsKey(a_event)){
            logger.info("Map already had value for "+a_event+"("+m_eventcallbackmap.get(a_event)+"), replacing it with new callback "+a_callback);
            m_eventcallbackmap.put(a_event,a_callback);
        } else {
            logger.info("Adding new Callback for "+a_event+" CallbackObject="+a_callback);
            m_eventcallbackmap.put(a_event,a_callback);
        }
    }
    /**
     * Disable the Callback for the specified event
     * @param a_event 
     */
    public void DisableEvent(XMSEventType a_event){
        FunctionLogger logger=new FunctionLogger("DisableEvent",this,m_logger);
        logger.args(a_event);
        
        if(m_eventcallbackmap.containsKey(a_event)){
            logger.info("Removing Callback("+m_eventcallbackmap.get(a_event)+") for "+a_event+" from the callback list" );
            m_eventcallbackmap.remove(a_event);
        } else {
            logger.info("No Callback for "+a_event+" is currently in the callback list");
            
        }
    }
    /**
     * This enables the Application Callback for the specified event
     * @param a_event
     * @param a_callback 
     */
    public void EnableAllEvents(XMSEventCallback a_callback){
          FunctionLogger logger=new FunctionLogger("EnableAllEvents",this,m_logger);
          logger.args(a_callback);
         for (XMSEventType e : XMSEventType.values()) {
             EnableEvent(e,a_callback);
        }
    }
    /**
     * Disable the Callback for the specified event
     * @param a_event 
     */
    public void DisableAllEvents(){
        FunctionLogger logger=new FunctionLogger("DisableAllEvents",this,m_logger);
      for (XMSEventType e : XMSEventType.values()) {
                   
             DisableEvent(e);
        }
    }
    
    private Object m_userobject=null;
    public void setUserObject(Object a_userobject){
        FunctionLogger logger=new FunctionLogger("setUserObject",this,m_logger);
        logger.args(a_userobject);
        m_userobject=a_userobject;
    }
    public Object getUserObject(){
        return m_userobject;
    }
}
