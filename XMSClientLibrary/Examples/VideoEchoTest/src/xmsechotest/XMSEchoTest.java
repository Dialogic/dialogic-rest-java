/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package xmsechotest;
import com.dialogic.XMSClientLibrary.*;

/**
 *
 * @author dwolansk
 */
public class XMSEchoTest {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        XMSObjectFactory myFactory = new XMSObjectFactory();
        XMSConnector myConnector = myFactory.CreateConnector("XMSConnectorConfig.xml");
        XMSCall myCall = myFactory.CreateCall(myConnector);
        
        //Wait for an inbound call
       while(true){
        myCall.WaitcallOptions.SetMediaType(XMSMediaType.VIDEO);
        myCall.Waitcall();  
        
        //Save the connection address for later
        String addr = myCall.getConnectionAddress();
        
        //Set the Record options to only record for 10seconds
        myCall.RecordOptions.SetMediaType(XMSMediaType.VIDEO);
        myCall.RecordOptions.SetMaxTime(10);
        
        //Record a file
        myCall.Record("echotest3.vid");
        
        //Hangup the call
        myCall.Dropcall();
        
        //Make an outbound call to the same address that you just received a call from
        myCall.MakecallOptions.SetMediaType(XMSMediaType.VIDEO);
        myCall.Makecall(addr);
        
        //Playback the file recorded
        myCall.PlayOptions.SetMediaType(XMSMediaType.VIDEO);
        myCall.Play("echotest3.vid");
        
        //Hangup the call
        myCall.Dropcall();
       }
    }
}
