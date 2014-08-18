package com.dialogic.XMSClientLibrary;

//import XMSClientLibraryTester.Main;// 22-Jun-2012 dsl : this flags error when importing into project.
import java.io.*;
//import java.util.logging.Level;
//import java.util.logging.Logger;

/**
 * Used for parsing the XML config file
 */import java.lang.String;
import java.util.concurrent.TimeUnit;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.ParsingException;

/**
 * Apach http client.  Used for htpp commands
 */
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;




        
/**
 * Logging utility.
 */
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 *
 * @author dslopres
 */
public class XMSRestConnector extends XMSConnector {

    /**
     *  Set up the logging mechanism using the log4j library
     */
    static Logger m_logger = Logger.getLogger(XMSRestConnector.class.getName());
    static Appender myAppender;


  
    /**
     * !!tod0: define what this object looks like
     */
    //List<XMSObj> m_XMSObjList ;


    /**
     * RESTFUL SPECIFIC MEMBER VARIABLES>
     */
//TODO should all these status codes be members or locals in event function?
    String m_AppID ;            /* For Restful XMS interface */
    String m_StatusCode ;       /* from http command */
    String m_ReasonPhrase;      /* Full text explanation */
    String m_uri;               /* Full URI */
    String m_xmlPayload;        /* XML Payload */
    String m_ReturnPayload;     /* XML Return Payload after executing commend */
    Runnable m_threadJob;       /* event handler thread  */
    Thread m_EvtHandlerThread;  /* thread id */
    private String m_configfilename;
    /**
     * Set up the variables used for various operations for Apache httpclient
     */
    HttpClient      m_httpclient    = null;
    HttpGet         m_httpget       = null;
    HttpPost        m_httppost       = null;
    HttpPut         m_httpput       = null;
    HttpDelete      m_httpdelete    = null;
    HttpResponse    m_httpresponseh = null;

    
    /* Map<String,XMSRestCall> m_CallMap; */
    
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

    public XMSRestConnector(){

        m_type = "REST";
        PropertyConfigurator.configure("log4j.properties");
        m_logger.info("Initialization of XMSRestConn");

        m_Name = "XMSRestConnector";

        
    }
 
    /**
     * Constructor that will read in the configuration file specified and
     * bring in variables needed to establish connections with the XMS Restful
     * server.
     * 
     * This function uses XOM library for parsing the configuration file 
     * (http://www.xom.nu/)
     * 
     * 
     * @param a_ConfigFileName  The XMS Configuration file.
     */
    public  XMSRestConnector(String a_ConfigFileName){
        m_type = "REST";
        PropertyConfigurator.configure("log4j.properties");
        //m_logger.setLevel(Level.ALL);
        m_logger.info("XMSRestConn config");
        

         m_Name = "XMSRestConnector";

         m_type = "REST"; // need to be used

         m_configfilename=a_ConfigFileName;
         Initialize();

    }

@Override
    public String toString()
    {
        return m_Name;

    }
     /**
     * FUNCTION     : Initialize
     * INPUT        : XMSReturn code
     * OUTPUT       :
     * DESCRIPTION  :
     * - creates the the eventhandler thread and request xms eventhandler
     */
public XMSReturnCode Initialize(String a_configfile){
    m_configfilename = a_configfile;
    return Initialize();
    
}
     /**
     * FUNCTION     : Initialize
     * INPUT        : XMSReturn code
     * OUTPUT       :
     * DESCRIPTION  :
     * - creates the the eventhandler thread and request xms eventhandler
     */
    public XMSReturnCode Initialize(){
        
           FunctionLogger logger=new FunctionLogger("Initialize",this,m_logger);
          
         FileInputStream xmlFile = null;
        /**
         * grab the config file
         */
        try {
            xmlFile = new FileInputStream(m_configfilename);
        } catch (FileNotFoundException ex) {
            m_logger.error(ex);
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
            m_logger.error(ex);
        } catch (IOException ex) {
            m_logger.error(ex);
        }

        Element root = doc.getRootElement();

        Elements entries = root.getChildElements();

        /**
         * Parse the XMS config file.
         */
        for (int x = 0; x < entries.size(); x++) {

            Element element = entries.get(x);
            if (element.getLocalName().equals("baseurl") ) {
                m_Address = element.getValue();
                m_logger.info("m_Address:" + m_Address);
            }  else if (element.getLocalName().equals("appid")){
                m_AppID = element.getValue();
                m_logger.info("m_AppID:" + m_AppID);
            } // end if
            
            

            
        }
        
           logger.info("Begin initialization");
           logger.info("Allocate Apache http client");
           //SchemeRegistry schemeRegistry = new SchemeRegistry();                                    // dsl 27-Jul-2012
           //schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));  // dsl 27-Jul-2012
           //ClientConnectionManager cm = new PoolingClientConnectionManager(schemeRegistry);         // dsl 27-Jul-2012
           m_httpclient = new DefaultHttpClient();
           
           
           logger.info("HttpClient allocated");
       
           xmsEventHandler l_eh=new xmsEventHandler(this,m_Address,m_AppID);
           m_threadJob = l_eh;
           m_EvtHandlerThread = new Thread(m_threadJob);
           if(l_eh.ConnectToServer() == XMSReturnCode.FAILURE){
               return XMSReturnCode.FAILURE;
           }
            
           m_EvtHandlerThread.start();
           logger.info("init completed");
           return XMSReturnCode.SUCCESS;
           


    }

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
   /*
     * public int Register(XMSRestCall a_callobj){
        
        
    
    }
    */
    
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
    public synchronized SendCommandResponse SendCommand(XMSObject a_call,RESTOPERATION a_RESTOPERATION, String a_urlextension, String a_xmlPayload ) {
        FunctionLogger logger=new FunctionLogger("SendCommand",this,m_logger);
        logger.args(a_call,a_RESTOPERATION,a_urlextension,a_xmlPayload);  //Commented to reduce log spam as most of parms are printed below in prinouts.
        StringEntity se=null; // variable for xml payload
        HttpEntity l_HttpEntity = null; // used for processing return payload
        SendCommandResponse l_scr = null; // used for command response that is returned
        l_scr = new SendCommandResponse();
        

        logger.info("WebSequence: "+a_call+" {{{App->XMS: "+a_RESTOPERATION+" "+a_xmlPayload+" }}}" );
        m_uri = m_Address + a_urlextension + "?appid=" + m_AppID;

        //TODO When a function fails next function after that gets a
//        Exception in thread "Thread-3" java.lang.IllegalStateException: Invalid use of SingleClientConnManager: connection still allocated.
//        Make sure to release the connection before allocating another one.
        
        logger.info("m_uri: "+ m_uri);
        
        if (a_xmlPayload != null ) {
            logger.info("a_xmlPayload: " + a_xmlPayload);
        }
      
        /**
         * Load the XML payload into
         * Place the XML payload such that it can be used int the httpclient
         */

        logger.info("Setting up the string entity if not null.");

        if (a_xmlPayload != null ) {

            try {
                se = new StringEntity(a_xmlPayload, HTTP.UTF_8);
            } catch (UnsupportedEncodingException ex) {
                logger.error("Exception occurred executioning StringEntity: " + ex); // 18-Jun-2012 dsl - replaced with logger.
             } catch (Exception ex){
                 logger.error("Exception occurred executioning StringEntity:: " + ex ); // 27-Jul-2012 dsl - replaced with logger.
             }

            logger.info("StringEntity set up complete."); // 27-Jul-2012 should be inside here.
        } else {
            logger.info("String Entity IS NULL.");

        }
        
        

        switch (a_RESTOPERATION){

               case GET:
                   logger.info("Attempted execution of : " + a_RESTOPERATION);
                   //!!todo:
                    //  1) Add StringEntity loading code for XML Payload
                    //  2) Normalize the URI being passed in
                   //   3) Further unit testing
                   m_httpget = new HttpGet(m_uri);
                    try {
                        m_httpresponseh = m_httpclient.execute(m_httpget);
                    } catch (IOException ex) {
                                                
                        logger.error("EXCEPTION [,"+a_urlextension+"]"+"["+a_RESTOPERATION+"] Exception-> " + ex);
                                                
                    }

                   /**
                    * reduced debug output to 1 line that can represent response
                    */
                   logger.info(a_RESTOPERATION + " STATUS LINE : " + m_httpresponseh.getStatusLine().toString());
                   //logger.info("WebSequence: "+a_call+" {{{XMS->App: "+a_RESTOPERATION + " STATUS LINE : " + m_httpresponseh.getStatusLine().toString()+" }}}" );
                   break; // end GET

                case POST:
                    
                   logger.info("Attempted execution of : " + a_RESTOPERATION);

                    m_httppost = new HttpPost(m_uri);

                    m_httppost.setHeader("Content-Type","text/xml;charset=UTF-8");
                    m_httppost.setEntity(se);

                    try {
                        m_httpresponseh = m_httpclient.execute(m_httppost);
                    } catch (IOException ex) {
                        logger.error("EXCEPTION [,"+a_urlextension+"]"+"["+a_RESTOPERATION+"] Exception-> " + ex);
                    }
                    logger.info("Completed execution of : " + a_RESTOPERATION);
                    /**
                    * reduced debug output to 1 line that can represent response
                    */
                    
                   logger.info(a_RESTOPERATION + " STATUS LINE : " + m_httpresponseh.getStatusLine().toString());
                   //logger.info("WebSequence: "+a_call+" {{{XMS->App: "+a_RESTOPERATION + " STATUS LINE : " + m_httpresponseh.getStatusLine().toString()+" }}}" );
                   break; // end POST

                case PUT:
                    logger.info("Attempted execution of : " + a_RESTOPERATION);
                    m_httpput = new HttpPut(m_uri);
                    m_httpput.setHeader("Content-Type","text/xml;charset=UTF-8");
                    m_httpput.setEntity(se);
                    try {
                        m_httpresponseh = m_httpclient.execute(m_httpput);
                    } catch (IOException ex) {
                        logger.error("EXCEPTION [,"+a_urlextension+"]"+"["+a_RESTOPERATION+"] Exception-> " + ex);
                                            }
                    logger.info("Completed execution of : " + a_RESTOPERATION);
                    /**
                    * reduced debug output to 1 line that can represent response
                    */
                    //logger.info("WebSequence: "+a_call+" {{{XMS->App: "+a_RESTOPERATION + " STATUS LINE : " + m_httpresponseh.getStatusLine().toString()+" }}}" );
                   logger.info(a_RESTOPERATION + " STATUS LINE : " + m_httpresponseh.getStatusLine().toString());
                  
                    break; // end PUT

                case DELETE:
                    //!!todo:
                    //  1) Add StringEntity loading code for XML Payload
                    //  2) Normalize the URI being passed in

                    logger.info("Attempted execution of : " + a_RESTOPERATION);
                    m_httpdelete = new HttpDelete(m_uri);
                    m_httpdelete.setHeader("Content-Type","text/xml;charset=UTF-8");
                    
                    try {
                        m_httpresponseh = m_httpclient.execute(m_httpdelete);
                    } catch (Exception ex) {
                        logger.error("EXCEPTION [,"+a_urlextension+"]"+"["+a_RESTOPERATION+"] Exception-> " + ex);
                     
                    }
                    logger.info("Completed execution of : " + a_RESTOPERATION);

                    /**
                    * reduced debug output to 1 line that can represent response
                    */
                    //logger.info("WebSequence: "+a_call+" {{{XMS->App: "+a_RESTOPERATION + " STATUS LINE : " + m_httpresponseh.getStatusLine().toString()+" }}}" );
                   logger.info(a_RESTOPERATION + " STATUS LINE : " + m_httpresponseh.getStatusLine().toString());

                    break; // end DELETE

        }// end switch

        if (a_RESTOPERATION != RESTOPERATION.DELETE){
            l_HttpEntity = m_httpresponseh.getEntity();
        } // end if
        logger.info("m_httpresponseh.getStatusLine().getStatusCode():" + m_httpresponseh.getStatusLine().getStatusCode());

          if ((m_httpresponseh.getStatusLine().getStatusCode() >= 200)&& (m_httpresponseh.getStatusLine().getStatusCode() < 300) ){

                logger.info("Send Command Succesful, operation:" + a_RESTOPERATION + " ,urlext:"  + a_urlextension);
                 if (a_RESTOPERATION != RESTOPERATION.DELETE){
                    /**
                     * Go grab the XML payload
                     */

                    try {
                        m_ReturnPayload = EntityUtils.toString(l_HttpEntity);
                    } catch (IOException ex) {
                        logger.error("EntityUtils.toString IOException: "+ ex);
                    } // end try
                    logger.debug("== BEGIN RETURN PAYLOAD RESPONSE OUTPUT RAW==\n\n" + m_ReturnPayload);
                    logger.debug("== END RETURN PAYLOAD RAW RESPONSE OUTPUT ==");
                    //logger.info("WebSequence: "+a_call+" {{{XMS->App: "+a_RESTOPERATION + " STATUS LINE : " + m_httpresponseh.getStatusLine().toString()+" "+m_ReturnPayload+" }}}" );
        //TODO move the the XMLbeans
                    String delims = "[ ]+";
                    String[] tokens = m_ReturnPayload.split(delims);
                    for (int i = 0; i < tokens.length; i++){
                        logger.debug("RETURN PAYLOAD TOKENS: " + tokens[i]);

                        if (tokens[i].startsWith("identifier=")){

                            String tempString = tokens[i];
                            String[] items = tempString.split("\"");

                            l_scr.set_scr_identifier(items[1]);
                            logger.info("scr_identifier: " + l_scr.get_scr_identifier());
                            AddCallToActiveCallList(l_scr.get_scr_identifier(),a_call);

                        }
                        if (tokens[i].startsWith("transaction_id=")){

                            String tempString = tokens[i];
                            String[] items = tempString.split("\"");

                            l_scr.set_scr_transaction_id(items[1]);
                            logger.info("scr_transaction_id: " + l_scr.get_scr_transaction_id());
                          }
                        if (tokens[i].startsWith("source_uri=")){

                            String tempString = tokens[i];
                            String[] items = tempString.split("\"");

                            l_scr.set_scr_source(items[1]);
                            logger.info("src_source: " + l_scr.get_scr_source());
                          }
                        
                     } // end if

                } // end for

            } else { // Registering of event handler failed.

               //TODO
               logger.error("Send Command failed!! DUMP XTRA INFO");
                try {
                        m_ReturnPayload = EntityUtils.toString(l_HttpEntity);
                    } catch (IOException ex) {
                        logger.error("EntityUtils.toString IOException: "+ ex);
                    } // end try
               logger.debug("DUMPINFO:== BEGIN RETURN PAYLOAD RESPONSE OUTPUT RAW==\n\n" + m_ReturnPayload);
               logger.debug("DUMPINFO:== END RETURN PAYLOAD RAW RESPONSE OUTPUT ==");
               logger.error("DUMPINFO: Call             =" + a_call);
               logger.error("DUMPINFO: StatusCode       =" + m_httpresponseh.getStatusLine().getStatusCode());
               logger.error("DUMPINFO: a_RESTOPERATION  =" + a_RESTOPERATION);
               logger.error("DUMPINFO: a_urlextension   =" + a_urlextension);
               logger.error("DUMPINFO: a_xmlPayload     =" + a_xmlPayload);
               logger.error("DUMP XTRA INFO COMPLETE");
               //System.exit(1); // exiting is too harsh [6-Jun-2012 dsl]

            } // end if

          
        l_scr.set_scr_status_code( m_httpresponseh.getStatusLine().getStatusCode());
        l_scr.set_scr_return_xml_payload(m_ReturnPayload);
        logger.info("WebSequence: "+a_call+" {{{XMS->App: "+a_RESTOPERATION + " STATUS LINE : " + l_scr.get_scr_status_code() +" "+l_scr.get_scr_return_xml_payload() +" }}}" );
        return l_scr;

      

    } // end SendCommand

    
    

    void setAppID(String a_AppID){
        m_AppID=a_AppID;
    }

    void setStatusCode(String a_StatusCode){

        m_StatusCode=a_StatusCode;
    }

     void setReasonPhrase(String a_ReasonPhrase){

        m_ReasonPhrase=a_ReasonPhrase;
    }

    void setUri(String a_uri){

        m_uri=a_uri;
    }

    void setXmlPayload(String a_xmlPayload){

        m_xmlPayload=a_xmlPayload;
    }

   /*********************/


    


    String getAppID(){
        return m_AppID;
    }            
    //TODO what is the use of the status code get?
    String getStatusCode(){
    
        return m_StatusCode;
    }
    
     String getReasonPhrase(){
    
        return m_ReasonPhrase;
    }
    
    String getUri(){
    
        return m_uri;
    }
    
    String getXmlPayload(){
    
        return m_xmlPayload;
    }

    
}
