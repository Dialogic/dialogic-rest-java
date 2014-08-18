/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dialogic.XMSGatewayDemo;
import com.dialogic.XMSClientLibrary.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
/**
 *
 * @author dwolansk
 */
public class XMSGatewayDemo {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
         Properties prop = new Properties();
         String SipRegistrarAddress=null;
         String RtcRegistrarAddress=null;
    	try {
               //load a properties file
    		prop.load(new FileInputStream("config.properties"));
 
                SipRegistrarAddress=prop.getProperty("SipRegistrarAddress");
                RtcRegistrarAddress=prop.getProperty("RtcRegistrarAddress");
 
    	} catch (IOException ex) {
    		ex.printStackTrace();
        }
        //Initalize the Factory that is used for future creation
        XMSObjectFactory myFactory = new XMSObjectFactory();
        
        //Initialize the connection to the server
        XMSConnector myConnector = myFactory.CreateConnector("XMSConnectorConfig.xml");
        XMSCall call1=myFactory.CreateCall(myConnector);
        XMSCall call2=myFactory.CreateCall(myConnector);
        
        XMSGateway myGw=new XMSGateway(call1,call2);
        
        myGw.SetRtcRegistrarAddress(RtcRegistrarAddress);
        myGw.SetSipRegistrarAddress(SipRegistrarAddress);
        myGw.Start();
        while(true){
           // System.out.println("hi");
            try{
                Thread.currentThread().sleep(1000);
            }catch(Exception ex){ }
            
        }
        
    }
}
