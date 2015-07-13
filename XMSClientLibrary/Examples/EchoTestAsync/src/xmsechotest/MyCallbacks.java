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
enum AppState {

    WAITCALL,
    RECORD,
    MAKECALL,
    PLAY
}

public class MyCallbacks implements XMSEventCallback {

    String addr = "";
    AppState myState = AppState.WAITCALL;
    boolean isDone = false;

    @Override
    public void ProcessEvent(XMSEvent a_event) {
        //throw new UnsupportedOperationException("Not supported yet.");
        XMSCall myCall = a_event.getCall();
        switch (a_event.getEventType()) {
            case CALL_CONNECTED:
                if (myState == AppState.MAKECALL) {
                    myCall.Play("echotest.wav");
                    //myCall.Play("file://recorded/Test");
                } else {  // is waitcall
                    //Save the connection address for later
                    addr = myCall.getConnectionAddress();
                    //Set the Record options to only record for 10seconds
                    myCall.RecordOptions.SetMaxTime(10);
                    //Record a file
                    myState = AppState.RECORD;
                    myCall.Record("echotest.wav");
                    //myCall.Record("file://recorded/Test");
                }
                break;
            case CALL_RECORD_END:
                myCall.Dropcall();
                myState = AppState.MAKECALL;
                myCall.Makecall(addr);
                break;
            case CALL_PLAY_END:
                myCall.Dropcall();
                isDone = true;
                break;
            case CALL_DISCONNECTED:  // The far end hung up will simply wait for the media

                break;
            case CALL_INFO:
                break;
            default:
                System.out.println("Unknown Event Type!!");
        }

    }

}
