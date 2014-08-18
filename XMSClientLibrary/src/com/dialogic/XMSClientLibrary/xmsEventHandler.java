package com.dialogic.XMSClientLibrary;
//import XMSClientLibraryTester.Main; // 22-Jun-2012 dsl : this flags error when importing into project.
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.logging.Level;
import javax.jws.WebService;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import com.dialogic.xms.*;
import com.dialogic.xms.CallDocument.Call;
import com.dialogic.xms.EventhandlerResponseDocument.EventhandlerResponse;
import com.dialogic.xms.EventType;

import java.util.Observable;
import java.util.Observer;  /* this is Event Handler */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author dslopres
 */
public class xmsEventHandler implements Runnable {

    private static Logger m_logger = Logger.getLogger(xmsEventHandler.class.getName());
    private static Appender myAppender;
    
    
    /*
            isr = new InputStreamReader(instream);
            reader = new BufferedReader(isr);
     */

    private InputStream         m_instream          = null;
    private InputStreamReader   m_InputStreamReader = null;
    private BufferedReader      m_BufferedReader    = null;

    private String  m_EvtHdlrXMLPayload = null;
    private String m_Address;
    private String m_AppID;
    private String m_URL;
    private XMSConnector m_connector;
    private int ReturnCode ;

    private HttpResponse response = null;

    private StringEntity se = null;

    private HttpEntity m_HttpEntity = null;
    private String localeventhandler_id = null;
    
//TODO do all these need to be members or can they be locals

    /**
     * Default constructor for the class.  Requires information on the XMS server
     *
     * @param a_Address     base address of the XMS server
     * @param a_AppID       AppID being used.
     */

    public xmsEventHandler(XMSConnector a_conn,String a_Address, String a_AppID) {
        m_logger.info("xmsEventHandler thread created args=XMSConnector="+a_conn+",Addr="+a_Address+",AppID="+a_AppID);
        m_Address = a_Address;
        m_AppID = a_AppID;
        m_connector= a_conn;
    }//constructor


    /**
     * This method is called when the thread runs
     */
    public void run() {
        FunctionLogger logger=new FunctionLogger("run",this,m_logger);
        logger.info("xmsEventHandler Thread started.");
        monitorEvents();
    }

/**
 * This method will be used to initialize the connection to the server;
 * @return 
 */    
    public XMSReturnCode ConnectToServer(){
        FunctionLogger logger=new FunctionLogger("ConnectToServer",this,m_logger);
        /**
         * Let's go ahead and register a generic event handler
         * with the XMS server
         */

            m_EvtHdlrXMLPayload = "<web_service version=\"1.0\"> <eventhandler>   <eventsubscribe action=\"add\"       type=\"any\" resource_id=\"any\"       resource_type=\"any\"/> </eventhandler></web_service>";
            logger.info("XML Payload:" +  m_EvtHdlrXMLPayload);

        /**
         * Create the event handler to grab all events.  POST
         */

           logger.info("Creating httpclient");
           HttpClient httpclient = new DefaultHttpClient();

           m_URL = m_Address + "eventhandlers?appid=" + m_AppID;

           logger.info("Setting up httppost");
           logger.info("m_URL: " + m_URL);

           HttpPost httppost = new HttpPost(m_URL);

       /*
        * Load up the XML payload with the generic event handler.
        */
           try {
                se = new StringEntity(m_EvtHdlrXMLPayload, HTTP.UTF_8);
           } catch (UnsupportedEncodingException ex) {
                logger.info("se exception" + ex);
           }

        // SETUP HTTPPOST WITH ENTITY INFORMATION
            httppost.setHeader("Content-Type","text/xml;charset=UTF-8");
            httppost.setEntity(se);


            logger.info("Attempting to execute the post for generic event handler.");
            try {
                response = httpclient.execute(httppost);
            } catch (IOException ex) {
                logger.error("EXITING ... Exception in executing post for generic event handler:" + ex);
                //TODO: We likely should not be exiting insdie our library and will yank covers off the app level.  Proper way would be to throw an exception that
                // app can handle and/or process. http://stackoverflow.com/questions/1369204/how-to-throw-a-checked-exception-from-a-java-thread
                return XMSReturnCode.FAILURE;
                //System.exit(1);              // 
            } // end try

            logger.info("STATUS LINE: " + response.getStatusLine().toString());

            m_HttpEntity = response.getEntity();
            String xmlString=null;

            if (response.getStatusLine().getStatusCode() == 201){

                logger.info("EventHandler Registeration Succesful!");

                /**
                 * Go grab the XML payload
                 */

                try {
                    xmlString = EntityUtils.toString(m_HttpEntity);
                } catch (IOException ex) {
                    logger.error("EntityUtils.toString IOException: "+ ex);
                } // end try
                logger.info("== BEGIN RAW RESPONSE OUTPUT ==\n\n" + xmlString);
                logger.info("== END RAW RESPONSE OUTPUT ==");

                /**
                 * Now let's use the XML Beans classes to go ahead and parse
                 * out the event handler response.
                 */

                WebServiceDocument myWebServiceDoc = null;
                EventhandlerResponse ehr = null;
                EventDataDocument edd = null;


                /*****************************************************************
                 * Now parse the EventhandlerResponse
                 ****************************************************************/
                try {
                    myWebServiceDoc = WebServiceDocument.Factory.parse(xmlString);

                } catch (XmlException ex) {
                    logger.info("Exception in parsing EventhandlerResponse: " + ex);
                } // end try

                logger.info("WebServiceDoc parsed.");

                /*****************************************************************
                 * Get information about the request
                 ****************************************************************/

                WebServiceDocument.WebService webservice  = myWebServiceDoc.getWebService();
                ehr = webservice.getEventhandlerResponse();

                logger.info("eventhandler information");
                logger.info("getAppid: " + ehr.getAppid());
                logger.info("Href: " + ehr.getHref());
                logger.info("Identifier: " + ehr.getIdentifier());
                localeventhandler_id = ehr.getIdentifier(); // store for later usage


            } else { // Registering of event handler failed.

               logger.error("Eventhandler registration failed. EXITING!!");
               return XMSReturnCode.FAILURE;
               
            } // end if


            /******************************************************************
            * Build new URL to check for events
            *******************************************************************/
                    
                String UrlExt = "eventhandlers/" + localeventhandler_id + "?appid=" + m_AppID;
                logger.info("UrlExt: " + UrlExt);

                String URL = m_Address + UrlExt;
                logger.info("URL: "+ URL);
                //InputStream instream = null;
                InputStreamReader isr = null;
                BufferedReader reader;
                String line;


                HttpGet httpget = new HttpGet(URL);

                try {
                    response = httpclient.execute(httpget);
                }   catch (IOException ex) {

                    logger.error("EXITING .. EXCEPTION Executing httpget: " + ex);
                    return XMSReturnCode.FAILURE;
                    

                }
                logger.info("HTTPOST EXECUTED TO REGISTER EVENTHANDLER");
                //logger.info("== PROTOCOL VERSION: " + response.getProtocolVersion());
                //logger.info("== STATUS CODE: " + response.getStatusLine().getStatusCode());
                //logger.info("== STATUS LINE: " + response.getStatusLine().getReasonPhrase());
                logger.info("== STATUS LINE: " + response.getStatusLine().toString());

                m_HttpEntity = response.getEntity();
                //logger.info("CONTENT TYPE: " + m_HttpEntity.getContentType() );
                //logger.info("CONTENT LENGTH: " + m_HttpEntity.getContentLength() );


                try {
                        logger.debug("Using InputStream to grab data.");
                        m_instream = m_HttpEntity.getContent();
                    } catch (IOException ex) {
                        logger.info("EXITING ... IOException", ex);
                        return XMSReturnCode.FAILURE;                       
                    } catch (IllegalStateException ex) {
                        logger.info("EXITING ... Illegal State Exception", ex);
                        return XMSReturnCode.FAILURE;
                        
                 }

                
        return XMSReturnCode.SUCCESS;
        
    }
    /**
     * Monitors the asynch events.
     * 
     * Telephony applications must handle unsolicited events such as digit detection 
     * and play completion events. Unsolicited events or client notifications do not 
     * fit well into the HTTP request / response model. 
     * However HTTP does support other mechanisms that enable this to be supported.
     * The concept is called Comet or HTTP event streaming. 
     */
   // TODO Try to compartmentialize this function a little so it isn't so monolithic
    private void monitorEvents(){
        FunctionLogger logger=new FunctionLogger("monitorEvents",this,m_logger);
        
                //InputStream instream = null;
                InputStreamReader isr;
                BufferedReader reader;
                String line;
            String xmlString=null;


        /**
                 * Setting up input stream to read from the socket connection.
                 */
                isr = new InputStreamReader(m_instream);
                reader = new BufferedReader(isr);
                int i=0;
                xmlString = "";
                WebServiceDocument wsd = null;

                
        logger.info("Executing go()");
                /********************************************************************
                 * The HTTP Connection remains open.
                 * Use a BufferedReader to grab data as it comes across the wire
                 ********************************************************************/
                String xmlStringPrime = null;
                String xmlStringCleaned;
                logger.info("====== WAITING FOR EVENT==========");
                try {
                    while ((line = reader.readLine()) != null) {

                        xmlString = xmlString+line;

                            /************************************************************
                             * Check to see if the XML data is complete
                             ************************************************************/
                            if (line.equals("</web_service>") ){
                                XMSRestEvent l_evt=new XMSRestEvent();
                                l_evt.rawstring=xmlString;
                                logger.info("====== NEW MESSAGE HAS BEEN RECEIVED ==========");
                                    //logger.debug("RAW XML': " + xmlString);
                                    /*****************************************************
                                     * Let's clean up the XML string
                                     * Leading characters encountered in the buffer
                                     *****************************************************/
                                // TODO Getting Exceptions on below code sometimes.
                                if(xmlString != null){
                                    int startPosition = xmlString.indexOf("<web_service version=\"1.0\">") + "<web_service version=\"1.0\">".length();
                                    int endPosition = xmlString.indexOf("</web_service>", startPosition);
                                    xmlStringCleaned = xmlString.substring(startPosition, endPosition);
                                    xmlStringCleaned = "<web_service version=\"1.0\">" + xmlStringCleaned + "</web_service>";
                                    logger.info("xmlStringCleaned: " + xmlStringCleaned);
                                    //logger.info("WebSequence {{{XMS->App: EVENT "+ xmlStringCleaned+" }}}");
                                } else {
                                    logger.info("XMLString is null");
                                    xmlStringCleaned = xmlString;
                                }
                                    /******************************************************
                                     * Now that we have a clean XML Payload, using
                                     * the XMLBeans java classes to parse through the
                                     * data completely.
                                     ******************************************************/
                                    String resourceId = "";
                                    String eventType = "";
                                    String resourceType = "";
                                     
                                    try {


                                        wsd = WebServiceDocument.Factory.parse(xmlStringCleaned);


                                    } catch (XmlException ex) {
                                        logger.info("exception: " + ex);
                                     }

                                  /**
                                   * Let's go figure out what type of events we got
                                   */
                                  WebServiceDocument.WebService webservice  = wsd.getWebService();
                                  EventDocument.Event event = webservice.getEvent();
                                  if (event == null) {
                                        logger.error("Can not find event node");
                                        return;
                                    } 
                                    l_evt.event = event;
                                    eventType       = event.getType().toString();
                                    // Check to see if is just a keepalive
                                    if(eventType.contains("keepalive")){
                                        
                                        logger.info("keepalive received");
                                    } else {
                                        
                                        resourceId      = event.getResourceId();
                                        eventType       = event.getType().toString();
                                        resourceType    = event.getResourceType();
                                        l_evt.resourceId = resourceId;
                                        l_evt.eventType = eventType;
                                        l_evt.resourceType = resourceType;
                                        
                                        logger.info("Resource Type  = " + resourceType);
                                        logger.info("Resource Id    = " + resourceId);
                                        logger.info("Event Type     = " + eventType);

                                        l_evt.connector=m_connector;
                                        if(eventType.contains("incoming")){
                                                logger.info("Processing incoming call");
                                                XMSCall l_call=m_connector.GetCallFromWaitCallList();
                                                if(l_call != null){
                                                    l_call.SetCallIdentifier(resourceId);
                                                     m_connector.AddCallToActiveCallList(resourceId, l_call);
                                                    l_evt.call=l_call;
                                                    l_call.UpdateAndNotify(l_evt);
                                            } else {
                                                logger.error("No calls in Waitcall state to process the incomming call");
                                            }
                                        }else {
                                            //TODO Check this logic on what to do if getID is not present
                                            XMSObject l_tmp=m_connector.GetCallFromId(resourceId);
                                            if(l_tmp instanceof XMSCall){
                                                XMSCall l_call=(XMSCall)l_tmp;
                                                if(l_call !=null){
                                                    l_evt.call=l_call;
                                                    l_call.UpdateAndNotify(l_evt);
                                                } else {

                                                    logger.error("No call for ID:"+resourceId+" found");
                                                }
                                            
                                            } else if(l_tmp instanceof XMSConference){
                                                l_evt.call=null;
                                                XMSConference l_conf=(XMSConference)l_tmp;
                                                l_conf.UpdateAndNotify(l_evt);
                                            } else {
                                                logger.info("Unable to detect Object Type");
                                                l_tmp=null;
                                            }
                                        }
                                    }
                                    xmlString = "";
                                    logger.info("====== MESSAGE COMPLETE ==========");
                                    logger.info("====== WAITING FOR EVENT==========");
                            } // end if



                        } // end while
                    } catch (IOException ex) {
                        logger.error("EXCEPTION: "+ ex); // 18-Jun-2012 dsl
                    }



            /******************************************************************
             * END OF PROCESSING BLOCK
             ******************************************************************/


    } // end monitor Events


} // end xmsEventHandler Class
