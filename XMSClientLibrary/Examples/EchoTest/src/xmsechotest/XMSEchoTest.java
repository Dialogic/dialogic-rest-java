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
        // REST
        //XMSConnector myConnector = myFactory.CreateConnector("XMSConnectorConfig.xml");
        // MSML
        XMSConnector myConnector = myFactory.CreateConnector("ConnectorConfig.xml");
        XMSCall myCall = myFactory.CreateCall(myConnector);
        
        //Wait for an inbound call
        myCall.Waitcall();  
        
        //Save the connection address for later
        String addr = myCall.getConnectionAddress();
        
        //Set the Record options to only record for 10seconds
        myCall.RecordOptions.SetMaxTime(10);
        myCall.RecordOptions.SetTerminateDigits("#");
        //Record a file
        //myCall.Record("echotest.wav");
        myCall.Record("file://recorded/Test.wav");
        
        //Hangup the call
        myCall.Dropcall();
        
        //Make an outbound call to the same address that you just received a call from
        myCall.Makecall(addr);
        
        //Playback the file recorded
        myCall.Play("file://recorded/Test.wav");
        
        //Hangup the call
        myCall.Dropcall();
        
    }
}
