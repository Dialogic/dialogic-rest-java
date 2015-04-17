/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dialogic.XMSGatewayDemo;
import com.dialogic.XMSClientLibrary.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.ArrayList;
import java.util.List;



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
         String myCallCountString=null;
         int myCallCount=2;
    	try {
               //load a properties file
    		prop.load(new FileInputStream("config.properties"));
 
                SipRegistrarAddress=prop.getProperty("SipRegistrarAddress");
                RtcRegistrarAddress=prop.getProperty("RtcRegistrarAddress");
                myCallCountString=prop.getProperty("myCallCount");
 
    	} catch (IOException ex) {
    		ex.printStackTrace();
        }
        //Initalize the Factory that is used for future creation
        XMSObjectFactory myFactory = new XMSObjectFactory();
        
        
        //Initialize the connection to the server
        XMSConnector myConnector = myFactory.CreateConnector("XMSConnectorConfig.xml");
        
        XMSRestConnector restcon = (XMSRestConnector)myConnector;
        
        ////    New code 
        
         myCallCount=Integer.parseInt(myCallCountString);
         
         List<XMSCall> myCallListA=new ArrayList<XMSCall>();
         List<XMSCall> myCallListB=new ArrayList<XMSCall>();
         List<XMSGateway> myGwList=new ArrayList<XMSGateway>();
         
         
         for(int x=0;x<myCallCount;x++){
            myCallListA.add(myFactory.CreateCall(myConnector));
            myCallListB.add(myFactory.CreateCall(myConnector));
            myGwList.add(new XMSGateway(myCallListA.get(x), myCallListB.get(x), restcon));
         };
         for(XMSGateway myGw: myGwList){
            myGw.SetRtcRegistrarAddress(RtcRegistrarAddress);
            myGw.SetSipRegistrarAddress(SipRegistrarAddress);
            myGw.Start();
            System.out.println("*****  Starting Gateway   *****");
         }
  
        while(true){
           // System.out.println("hi");
            try{
                Thread.currentThread().sleep(1000);
            }catch(Exception ex){ }
            
        }
        
    }
}
