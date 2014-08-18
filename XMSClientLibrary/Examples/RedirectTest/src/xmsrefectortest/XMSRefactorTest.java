/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package xmsrefectortest;
import com.dialogic.XMSClientLibrary.*;

/**
 *
 * @author dwolansk
 */
public class XMSRefactorTest {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        XMSObjectFactory myFactory = new XMSObjectFactory();
        XMSConnector myConnector = myFactory.CreateConnector("XMSConnectorConfig.xml");
        XMSCall myCall = myFactory.CreateCall(myConnector);
      
        
        //Wait for an inbound call, but to do redirect you need
        // to disable the auto connect as redirect is only acceptable in
        // offered state
        myCall.WaitcallOptions.EnableAutoConnect(false);
        myCall.Waitcall();  
        
        //TODO: May do some play here telling them they are being redirected to
        myCall.Redirect("sip:Dan@10.20.105.43:5070");
        
        
        Sleep(10000);
    }
    public static void Sleep(int time){
            try {
                
                Thread.sleep(time);
            } catch (InterruptedException ex) {
                System.out.print(ex);
            }
                   
        }
}
