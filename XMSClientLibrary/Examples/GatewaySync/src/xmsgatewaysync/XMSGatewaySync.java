/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package xmsgatewaysync;
import com.dialogic.XMSClientLibrary.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 *
 * @author dwolansk
 */
public class XMSGatewaySync {

       static String OutboundAddress=null;
       static String OutboundSipProxy=null;
       static boolean isVideo=true;
        
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        XMSObjectFactory myFactory = new XMSObjectFactory();
        XMSConnector myConnector = myFactory.CreateConnector("XMSConnectorConfig.xml");
        XMSCall myCall1 = myFactory.CreateCall(myConnector);
        XMSCall myCall2 = myFactory.CreateCall(myConnector);
        
         Properties prop = new Properties();
         
        
         
    	try {
               
               //prop.setProperty("OutboundAddress","rtc:Test" );
               //prop.setProperty("OutboundSipProxy","192.168.1.10" );
               //prop.setProperty("isVideo","true" );
               //prop.store(new FileOutputStream("config.properties"), null);
                //load a properties file
                
    		prop.load(new FileInputStream("config.properties"));
                OutboundSipProxy=prop.getProperty("OutboundSipProxy");
                OutboundAddress=prop.getProperty("OutboundAddress");
                if(prop.getProperty("isVideo").contains("false")){
                    isVideo=false;
                } 
 
    	} catch (IOException ex) {
    		ex.printStackTrace();
        }
        
        //Wait for an inbound call
            while(true){
                if(isVideo)myCall1.WaitcallOptions.SetMediaType(XMSMediaType.VIDEO);
                myCall1.Waitcall();  

                System.out.println("CalledAddress = "+myCall1.getCalledAddress());
                String outstring=GetOutString(myCall1.getCalledAddress());
                
                if(isVideo)myCall2.MakecallOptions.SetMediaType(XMSMediaType.VIDEO);
                
                //This will set the address to the address that initiated the call.
                myCall2.MakecallOptions.setSourceAddress(myCall1.getConnectionAddress());
                
                myCall2.Makecall(outstring);
                myCall2.Join(myCall1);
                // or you could call myCall1.Join(myCall2); 
                // both will do the same thing and it is only
                // on one device to establish a full duplex connection

               while( myCall1.getState()==XMSCallState.CONNECTED && myCall2.getState()==XMSCallState.CONNECTED){
                    Sleep(1000);
               }
               myCall1.Dropcall();
               myCall2.Dropcall();
            }
    }
    public static String GetOutString(String instring){
                String[] output =instring.split("call=");
                
                //First checking to is if the call= is used, otherwise just make call to the static IP in file. 
                // this is done to preserve functionality with previous version
                if(output.length > 1){
                    //this indicates that the call= detected, next we need to detect type
                    String outstring= output[1];
                    //This is a work around because the " at end is present
                    outstring=outstring.substring(0, outstring.length()-2);
                    System.out.println("detected call: in CalledAddress, using "+outstring);
                    
                    // This indicates an RTC call
                    if(outstring.startsWith("rtc:") ){
                        //need to strip out anything after the @ in case of sip->rtc
                        if( outstring.contains("@"))
                            outstring=outstring.substring(0, outstring.indexOf("@"));
                        System.out.println("detected rtc and @ in CalledAddress, set new string to "+outstring);
                        
                    } else if(outstring.startsWith("sip:") && outstring.contains("@")){
                        //Inticates that there is a SIP call
                        outstring=outstring.substring(0, outstring.indexOf("@"));
                        outstring=outstring+"@"+OutboundSipProxy;
                        System.out.println("detected sip and @ in CalledAddress, Appending OutboundSipProxy, set new string to "+outstring);
                        
                    } else if( outstring.contains("@")){
                        //Assuming that anything else is a sip destination
                        outstring=outstring.substring(0, outstring.indexOf("@"));
                        outstring="sip:"+outstring+"@"+OutboundSipProxy;
                        System.out.println("detected @ in CalledAddress, Appending OutboundSipProxy and prepending sip:, set new string to "+outstring);
                        
                    } else{
                        outstring="sip:"+outstring+"@"+OutboundSipProxy;
                        System.out.println("detected @ in CalledAddress, Appending OutboundSipProxy and prepending sip:, set new string to "+outstring);
                        System.out.println("NO RTC and NO @ in CalledAddress, Appending OutboundSipProxy and adding sip:, set new string to "+outstring);
                        
                    }
                    System.out.println("returning outsting="+outstring);
                    return outstring;  
                }
                else {
                    System.out.println("did not detect call: in CalledAddress, using "+OutboundAddress);
                    
                    return OutboundAddress;  
                }
    }
    public static void Sleep(int time){
            try {
                
                Thread.sleep(time);
            } catch (InterruptedException ex) {
                System.out.print(ex);
            }
                   
        }
     
}
