/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package xmstransfertest;
import com.dialogic.XMSClientLibrary.*;

/**
 *
 * @author dwolansk
 */
public class XMSAttendedTransferTest {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        XMSObjectFactory myFactory = new XMSObjectFactory();
        XMSConnector myConnector = myFactory.CreateConnector("XMSConnectorConfig.xml");
        XMSCall myCall = myFactory.CreateCall(myConnector);
           XMSCall myCall2 = myFactory.CreateCall(myConnector);
        
        //Wait for an inbound call
        myCall.WaitcallOptions.SetMediaType(XMSMediaType.VIDEO);
        myCall.Waitcall();  
        
        //TODO: May do some play here telling them they are being transfered
        
   //   myCall.Transfer("sip:Dan@10.20.105.43:5070");
        
        myCall2.MakecallOptions.SetMediaType(XMSMediaType.VIDEO);
        //myCall2.Makecall("sip:Dan@10.20.105.43:5070");
        myCall2.Makecall("rtc:lab");
            
        myCall.Transfer(myCall2);
        
        Sleep(1000);
    }
    public static void Sleep(int time){
            try {
                
                Thread.sleep(time);
            } catch (InterruptedException ex) {
                System.out.print(ex);
            }
                   
        }
}
