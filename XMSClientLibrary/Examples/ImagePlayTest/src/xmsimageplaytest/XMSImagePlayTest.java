/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package xmsimageplaytest;
import com.dialogic.XMSClientLibrary.*;

/**
 *
 * @author dwolansk
 */
public class XMSImagePlayTest {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        XMSObjectFactory myFactory = new XMSObjectFactory();
        XMSConnector myConnector = myFactory.CreateConnector("XMSConnectorConfig.xml");
        XMSCall myCall = myFactory.CreateCall(myConnector);
        
        //Wait for an inbound call
        myCall.WaitcallOptions.SetMediaType(XMSMediaType.VIDEO);
        myCall.Waitcall();  
        
        myCall.PlayOptions.SetMediaType(XMSMediaType.IMAGE);
        myCall.Play("id=menu&header=IVVR Demo&items=1 Play&items=2 Record&items=3 Playback&items=4 Conf&footer=Dialogic");
        
        //Hangup the call
        myCall.Dropcall();
        
    }
}
