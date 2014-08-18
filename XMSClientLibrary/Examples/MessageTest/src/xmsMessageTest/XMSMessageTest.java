/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package xmsMessageTest;
import com.dialogic.XMSClientLibrary.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 *
 * @author dwolansk
 */
public class XMSMessageTest {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        XMSObjectFactory myFactory = new XMSObjectFactory();
        XMSConnector myConnector = myFactory.CreateConnector("XMSConnectorConfig.xml");
        XMSCall myCall = myFactory.CreateCall(myConnector);
         MyCallbacks myCallback = new MyCallbacks();
        
        //Enable all events to go back to my event handler
        myCall.EnableAllEvents(myCallback);
        
        //Wait for an inbound call and start the state machine
        myCall.WaitcallOptions.SetMediaType(XMSMediaType.MESSAGE);
        myCall.Waitcall();  
  /*      
        XMSCall myCall2 = myFactory.CreateCall(myConnector);
        myCall2.MakecallOptions.SetMediaType(XMSMediaType.MESSAGE);
        myCall2.MakecallOptions.setSourceAddress("sip:OutboundMessage@192.168.1.6");
        myCall2.Makecall("sip:app@192.168.1.110");
        
        Sleep(5000);
        myCall2.SendMessage("Can I hang up yet?");
        Sleep(10000);
        myCall2.SendMessage("It is later - Can I hang up yet?");
        Sleep(5000);
        //Hangup the call
        myCall2.Dropcall();
    */
        while(true){
            Sleep(1000);
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
