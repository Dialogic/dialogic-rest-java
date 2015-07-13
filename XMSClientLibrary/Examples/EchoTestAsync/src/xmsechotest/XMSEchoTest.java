/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package xmsechotest;

import com.dialogic.XMSClientLibrary.*;

import java.util.logging.Level;
import java.util.logging.Logger;

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
        XMSConnector myConnector = myFactory.CreateConnector();
        XMSCall myCall = myFactory.CreateCall(myConnector);
        MyCallbacks myCallback = new MyCallbacks();

        //Enable all events to go back to my event handler
        myCall.EnableAllEvents(myCallback);

        //Wait for an inbound call and start the state machine
        myCall.Waitcall();

        //At this point event handler thread will process all events so this thread just waits to complete
        while (!myCallback.isDone) {
            Sleep(1000);
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
