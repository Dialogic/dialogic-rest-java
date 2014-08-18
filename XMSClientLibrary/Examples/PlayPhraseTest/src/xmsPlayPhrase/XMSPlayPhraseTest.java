/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package xmsPlayPhrase;
import com.dialogic.XMSClientLibrary.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 *
 * @author dwolansk
 */
public class XMSPlayPhraseTest {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        XMSObjectFactory myFactory = new XMSObjectFactory();
        XMSConnector myConnector = myFactory.CreateConnector("XMSConnectorConfig.xml");
        XMSCall myCall = myFactory.CreateCall(myConnector);
        
             
        myCall.Waitcall();
        
        myCall.PlaySilence(1000);
        myCall.PlayPhrase(XMSPlayPhraseType.DATE, "20140314");
        myCall.PlaySilence(1000);
        myCall.PlayPhrase(XMSPlayPhraseType.DIGITS,"1234567890");
        myCall.PlaySilence(1000);
        myCall.PlayPhrase(XMSPlayPhraseType.DURATION, "3661");
        myCall.PlaySilence(1000);
        myCall.PlayPhrase(XMSPlayPhraseType.MONEY, "1025");
        myCall.PlaySilence(1000);
        myCall.PlayPhrase(XMSPlayPhraseType.MONTH, "03");
        myCall.PlaySilence(1000);
        myCall.PlayPhrase(XMSPlayPhraseType.NUMBER, "421");
        myCall.PlaySilence(1000);
        myCall.PlayPhrase(XMSPlayPhraseType.TIME, "1750");
        myCall.PlaySilence(1000);
        myCall.PlayPhrase(XMSPlayPhraseType.WEEKDAY,"4");
        myCall.PlaySilence(1000);
        myCall.PlayPhrase(XMSPlayPhraseType.STRING,"a34bc");
        
        //Hangup the call
        myCall.Dropcall();
        
    }
    public static void Sleep(int time){
            try {
                
                Thread.sleep(time);
            } catch (InterruptedException ex) {
                System.out.print(ex);
            }
                   
        }
}
