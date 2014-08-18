/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dialogic.XMSClientLibrary;
/**
 * Logging utility.
 */
import java.util.Stack;
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 *
 * @author dwolansk
 */
public class XMSEventDistributor implements Runnable{
     
     static Logger m_logger = Logger.getLogger(xmsEventHandler.class.getName());
     protected Stack<XMSEvent> m_eventlist= new Stack<XMSEvent>();
     boolean m_keeplooping;
    public XMSEventDistributor() {
        m_logger.info("xmsEventHandler thread created");
     
        m_keeplooping=true;
    }//constructor
    
    public void stopWaitingForEvents(){
        m_keeplooping=false;
        synchronized(m_eventlist){
        	m_eventlist.notify();
        }
    }
    /**
     * This method is called when the thread runs
     */
    public void run() {
        FunctionLogger logger=new FunctionLogger("run",this,m_logger);
        logger.info("xmsEventHandler Thread started.");
        monitorEvents();
    }
    public void monitorEvents(){
        FunctionLogger logger=new FunctionLogger("monitorEvents",this,m_logger);
        while(m_keeplooping){
        	XMSEvent evt = null;
        	synchronized(m_eventlist){
        		while(m_eventlist.empty() && m_keeplooping){ // wait for event
        			try {
        				m_eventlist.wait();
        			} catch (InterruptedException e) {
        				// do nothing
        			}
        		}
        		if(!m_eventlist.empty() ){
        			evt = m_eventlist.pop();
        		}
        	}
            if(evt != null){
                evt.getCall().DispatchXMSEvent(evt);
            }
        }
        
    }
    public void AddEventToQueue(XMSEvent a_evt){
    	synchronized(m_eventlist){
    		m_eventlist.push(a_evt);
    		m_eventlist.notify();
    	}
    }
    
}
