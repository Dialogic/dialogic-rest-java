/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package xmsDigitDetect;
import com.dialogic.XMSClientLibrary.*;

/**
 *
 * @author dwolansk
 */
public class XMSDigitDetect {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        XMSObjectFactory myFactory = new XMSObjectFactory();
        XMSConnector myConnector = myFactory.CreateConnector("XMSConnectorConfig.xml");
        //XMSConnector myConnector = myFactory.CreateConnector("ConnectorConfig.xml");
        XMSCall myCall = myFactory.CreateCall(myConnector);
        
            
    
       
        //Make an outbound call to the same address that you just received a call from
        
        myCall.Waitcall();
        
        //Playback the file recorded
        
       
        
        
        while(true){
            myCall.CollectDigitsOptions.SetMaxDigits(1);
            myCall.CollectDigitsOptions.SetTimeout(20);
            myCall.CollectDigits();
            System.out.println("Last Event -  "+myCall.getLastEvent());

            if(myCall.getLastEvent().getReason().contains("term-digit")){
                System.out.println("Term Digit of # detected.... hanging up");
                break;
            }
            if(myCall.getLastEvent().getReason().contains("max-digits")  ){
                if(myCall.getLastEvent().getData().contentEquals("*")){
                    System.out.println("We have a star here.... do something fun");
                    myCall.Play("verification/video_clip_nascar");
                } else {
                    System.out.println("Digit "+myCall.getLastEvent().getData()+" was detected");
                    continue;
                }
            }
        }
        //Hangup the call
        myCall.Dropcall();
        
    }
}
