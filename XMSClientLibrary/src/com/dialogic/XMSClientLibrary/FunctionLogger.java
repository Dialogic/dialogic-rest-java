/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 * 
 */
package com.dialogic.XMSClientLibrary;
/**
 * Logging utility.
 */
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 *
 * @author dwolansk
 */
public class FunctionLogger {
     static Logger logger;
     String m_function;
     Object m_obj;
     
     public FunctionLogger(String a_func,Object a_obj,Logger a_log){
         m_function=a_func;
         if(a_obj !=null){
            m_obj=a_obj;
         } else {
            
           m_obj = new Object();
         }
          if(a_log !=null){
            logger=a_log;
         } else {
             logger = Logger.getLogger(XMSCall.class.getName());
             PropertyConfigurator.configure("log4j.properties");
             logger.setLevel(Level.ALL);
         }
        
          
        info("Entering " + m_function);
     }
     protected void finalize () throws Throwable {
         // TODO Leaving Not working on the FunctionLogger
           info("Leaving " + m_function);
           super.finalize();
     }
     
     
     public void info(String a_logentry){
         
         if(a_logentry.contains("WebSequence")){
             a_logentry=a_logentry.replaceAll("\r","");
             a_logentry=a_logentry.replaceAll("\n","\\\\n");
             
         }
         logger.info(m_obj.toString()+", "+m_function+", "+a_logentry);
     }
     public void error(String a_logentry){
         
         logger.info(m_obj.toString()+", "+m_function+", "+a_logentry);
     }
     public void debug(String a_logentry){
         
         logger.info(m_obj.toString()+", "+m_function+", "+a_logentry);
     }
     
     public void info(String a_logentry,Throwable t){
         
         logger.info(m_obj.toString()+", "+m_function+", "+a_logentry,t);
     }
     public void error(String a_logentry,Throwable t){
         
         logger.info(m_obj.toString()+", "+m_function+", "+a_logentry,t);
     }
     public void debug(String a_logentry,Throwable t){
         logger.info(m_obj.toString()+", "+m_function+", "+a_logentry,t);
     }
    
      public void error(Throwable t){
         
         logger.info(m_obj.toString()+", "+m_function+", ",t);
     }
     public void args(Object ... arglist){
         String l_args = "args=";
         
         for(Object arg: arglist){       
                l_args+= arg+", ";
             
         }
                 
         l_args+=")";
         info(l_args);
     }
    
}
