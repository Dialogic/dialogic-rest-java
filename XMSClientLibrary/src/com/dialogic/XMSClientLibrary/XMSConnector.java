package com.dialogic.XMSClientLibrary;

import java.io.*;
//import java.util.logging.Level;
//import java.util.logging.Logger;

/**
 * Used for parsing the XML config file
 */import java.lang.String;


/**
 * Logging utility.
 */
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
/**
 * Needed for the registration maps
 */
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Queue;
import java.util.LinkedList;
/**
 *
 * @author dslopres
 */
public abstract class XMSConnector {

    /**
     *  Set up the logging mechanism using the log4j library
     */
    static Logger m_logger = Logger.getLogger(XMSConnector.class.getName());
    static Appender myAppender;


    /**
     * !!todo: make variables private
     */

    protected String m_ID ;               /* Unique identifier for the xmsconnector */
    protected String m_Name ;             /* use to id connector in logs */
    protected String m_Address;           /* Used for Base URL to XMS Rest server */
    protected String m_ConfigFileName;    /* getName of config file */
    protected String m_State;             /* getState of connection, e.g
                                 * if you are connected to XMS server or not
                                 * connected.  Should be enum
                                 *  UNINITIALIZED
                                 *  READY
                                 */
  
    protected String m_type;
    
    protected Map<String,XMSObject> m_activecallmap;
    protected Queue<XMSCall> m_waitcalllist;
    
    protected Runnable m_CallEvtHandlerThreadJob;       /* event handler thread  */
    protected Thread m_CallEvtHandlerThread;  /* thread id */
    /**
     * FUNCTIONS
     */

    
    /**
     * Initial constructor for the connector.  It will do the following:
     * - parse data from the configuration file.
     * - through it into the member variables
     *
     * Assumptions:
     * - default config file name = xmsconn.xml
     *
     */

    public XMSConnector(){

        PropertyConfigurator.configure("log4j.properties");
        m_logger.info("Initialization of XMSRestConn");

        m_Name = "XMSRestConnector";

        m_activecallmap = new HashMap<String,XMSObject>();
        m_waitcalllist = new LinkedList<XMSCall>();
        
        
       m_CallEvtHandlerThreadJob = new XMSEventDistributor();
       m_CallEvtHandlerThread = new Thread(m_CallEvtHandlerThreadJob);
       m_CallEvtHandlerThread.start();
       m_logger.info("Call Event Handler Thread init completed");
    }
 
    /**
     * Constructor that will read in the configuration file specified and
     * bring in variables needed to establish connections with the XMS 
     * server.
     * 
     * This function uses XOM library for parsing the configuration file 
     * (http://www.xom.nu/)
     * 
     * 
     * @param a_ConfigFileName  The XMS Configuration file.
     */
    public  XMSConnector(String a_ConfigFileName){

        PropertyConfigurator.configure("log4j.properties");
        //m_logger.setLevel(Level.ALL);
        m_logger.info("XMSRestConn config");
        FileInputStream xmlFile = null;
        m_activecallmap = new HashMap<String,XMSObject>();
    
        
    }

@Override
    public String toString()
    {
        return m_Name;

    }
     /**
     * FUNCTION     : Initialize
     * INPUT        :
     * OUTPUT       :
     * DESCRIPTION  :
     * - creates the the eventhandler thread and request xms eventhandler
     */
    public abstract XMSReturnCode Initialize();

 /**
     * FUNCTION     :   Register
     *
     * DESCRIPTION  :   This is used by the XMSCall Object to register
     *                  For future callbacks
     *
     * PARAMETERS   :   XMSRestCall     - The object to register
     *
     * RETURN       :   Status Code for operations
     *
     * Author(s)    :   d.wolanski
     * Created      :   4-Jun-2012
     * Updated      :   
     *
     * !!todo       :
     *
     * HISTORY      :
     *************************************************************************/
    protected XMSReturnCode Register(XMSCall a_callobj){
        
        return XMSReturnCode.SUCCESS;
    
    }
    
    
     /**
     * FUNCTION     :   SendCommand
     *
     * DESCRIPTION  :   Sends actual HTTP request using approriate operations
     *
     * PARAMETERS   :   RESTOPERATION   - enum for POST, GET, DELETE, UPDATE
     *                  xmlPayload      - String for XMLPAYLOAD
     *
     * RETURN       :   Status Code for operations
     *
     * Author(s)    :   d.lopresti
     * Created      :   16-May-2012
     * Updated      :   18-May-2012
     *
     * !!todo       :
     *
     * HISTORY      :
     *************************************************************************/
//TODO This Send Interface is real REST specific.. should genricize it
    // possibly adding a XMSSendCommandArgument class that is extended for
    // each technology
    protected abstract SendCommandResponse SendCommand(XMSObject a_call,RESTOPERATION a_RESTOPERATION, String a_urlextension, String a_xmlPayload ) ;
    
/**
     * 
     * @param a_ID 
     */
    protected void setID(String a_ID){

        m_ID = a_ID;
    }
/**
     * 
     * @param a_Name 
     */
    void setName(String a_Name){
        m_Name = a_Name;
    }
/**
     * 
     * @param a_Address 
     */
    void setAddress(String a_Address){
        m_Address=a_Address;
    }
/**
     * 
     * @param a_ConfigFileName 
     */
    void setConfigFileName(String a_ConfigFileName) {
        m_ConfigFileName = a_ConfigFileName;
    }
/**
     * 
     * @param a_State 
     */
    protected void setState(String a_State){
        m_State = a_State;
    }

   /*********************/


    
/**
     * 
     * @return 
     */
    String getID(){
        
        return m_ID ;
    }
       /**
     * 
     * @return 
     */     
    String getName(){
        return m_Name;
    }             
    /**
     * 
     * @return 
     */
    String getAddress(){
        return m_Address;
    }           
    /**
     * 
     * @return 
     */
    String getConfigFileName() {
        return m_ConfigFileName ;
    }
/**
     * 
     * @return 
     */
    String getState(){
        return m_State ;
    }  
    /**
     * 
     * @return 
     */
    String getType(){
        return m_type ;
    } 
    /**
     * Add the call to the wait call list
     * @param a_call 
     */
    protected void AddCallToWaitCallList(XMSCall a_call){
        FunctionLogger logger=new FunctionLogger("AddCallToWaitCallList",this,m_logger);
        logger.info("Adding "+a_call+" to the WaitCallList");
        m_waitcalllist.add(a_call);    
    }
    /**
     * Used by the event thread to obtain the call object from
     * the WaitCallList
     * @return 
     */
    protected XMSCall GetCallFromWaitCallList(){
        FunctionLogger logger=new FunctionLogger("GetCallFromWaitCallList",this,m_logger);
        logger.info("Getting Call From WaitCallList- Waitcall list size="+m_waitcalllist.size());
        
        if( m_waitcalllist.isEmpty()){
            logger.error("No Calls in the waitcall list");
            return null;
        }else{
            XMSCall l_call=(XMSCall)m_waitcalllist.poll();
            logger.info(l_call + "Was popped from the WaitCallList");
            return l_call;
        }
    }
    /**
     * Add the new call getID into the active call list 
     * @param a_id
     * @param a_call 
     */
    protected void AddCallToActiveCallList(String a_id,XMSObject a_call){
        FunctionLogger logger=new FunctionLogger("AddCallToActiveCallList",this,m_logger);
        a_call.SetCallIdentifier(a_id);
        logger.info("Adding "+a_id+" / "+a_call.Name()+"to the ActiveCallList");
        m_activecallmap.put(a_id,a_call);
    }
    /**
     * Used to obtain the correct XMSCall Object from the getID
     * @param a_id
     * @return 
     */
    protected XMSObject GetCallFromId(String a_id){
        FunctionLogger logger=new FunctionLogger("GetCallFromId",this,m_logger);
        XMSObject l_obj = m_activecallmap.get(a_id);
        if(l_obj != null){
        logger.info("Obtained "+l_obj.Name() + " for id:"+a_id+" |");
        } else {
            
            logger.error("Unable to find call for id:"+a_id);    
        }
        return l_obj;
    }
    /**
     * Remove an id from the ActiveCallList map.
     * @param a_id 
     */
    protected void RemoveCallFromActiveCallList(String a_id){
        FunctionLogger logger=new FunctionLogger("RemoveCallFromActiveCallList",this,m_logger);
        logger.info("removing id:"+a_id+" from ActiveCallList");
        m_activecallmap.remove(a_id);
    }
    protected void PostCallEvent(XMSEvent a_evt){
        XMSEventDistributor l_distributor=(XMSEventDistributor)m_CallEvtHandlerThreadJob;
        l_distributor.AddEventToQueue(a_evt);
        
    }
}
