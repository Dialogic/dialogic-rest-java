/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package xmsjointest;
import com.dialogic.XMSClientLibrary.*;

/**
 *
 * @author dwolansk
 */
public class XMSJoinTest {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        XMSObjectFactory myFactory = new XMSObjectFactory();
        XMSConnector myConnector = myFactory.CreateConnector();
        XMSCall myCall1 = myFactory.CreateCall(myConnector);
        XMSCall myCall2 = myFactory.CreateCall(myConnector);
        
        while(true){
        //Wait for an inbound call
            myCall1.WaitcallOptions.SetMediaType(XMSMediaType.VIDEO);
            myCall1.Waitcall();  
            
            myCall2.WaitcallOptions.SetMediaType(XMSMediaType.VIDEO);
            myCall2.Waitcall();  
                
            myCall2.Join(myCall1);
            // or you could call myCall1.Join(myCall2); 
            // both will do the same thing and it is only
            // on one device to establish a full duplex connection
            
           Sleep(15000);
           
           myCall1.Dropcall();
           myCall2.Dropcall();
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
