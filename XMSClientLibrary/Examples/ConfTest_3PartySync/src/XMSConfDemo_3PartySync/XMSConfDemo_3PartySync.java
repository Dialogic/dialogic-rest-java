/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package XMSConfDemo_3PartySync;

import com.dialogic.XMSClientLibrary.*;
//import java.util.concurrent.*

/**
 *
 * @author dwolansk
 */
public class XMSConfDemo_3PartySync {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        XMSObjectFactory myFactory = new XMSObjectFactory();
        XMSConnector myConnector = myFactory.CreateConnector();
        XMSCall myCall1 = myFactory.CreateCall(myConnector);
        XMSCall myCall2 = myFactory.CreateCall(myConnector);
        XMSCall myCall3 = myFactory.CreateCall(myConnector);

        XMSConference myConf = myFactory.CreateConference(myConnector);

        while (true) {
            myCall1.WaitcallOptions.SetMediaType(XMSMediaType.VIDEO);
            myCall1.Waitcall();
            myConf.Add(myCall1);

            myCall2.WaitcallOptions.SetMediaType(XMSMediaType.VIDEO);
            myCall2.Waitcall();
            myConf.Add(myCall2);

            myCall3.WaitcallOptions.SetMediaType(XMSMediaType.VIDEO);
            myCall3.Waitcall();
            myConf.Add(myCall3);

            Sleep(30000);
            myCall1.Dropcall();
            myCall2.Dropcall();
            myCall3.Dropcall();
        }
    }

    public static void Sleep(int time) {
        try {

            Thread.sleep(time);
        } catch (InterruptedException ex) {
            System.out.print(ex);
        }

    }
}
