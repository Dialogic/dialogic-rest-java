/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package xmsConfDemoAsync;

import com.dialogic.XMSClientLibrary.*;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dwolansk
 */
public class XMSConfDemoAsync implements XMSEventCallback{
    boolean isRunning=true; //Set this to fales to get it to exit.
    
    XMSObjectFactory myFactory = new XMSObjectFactory();
    XMSConnector myConnector = myFactory.CreateConnector("XMSConnectorConfig.xml");
    XMSCall myCall1 = myFactory.CreateCall(myConnector);
    XMSCall myCall2 = myFactory.CreateCall(myConnector);
    XMSCall myCall3 = myFactory.CreateCall(myConnector);
    XMSCall myCall4 = myFactory.CreateCall(myConnector);
    XMSConference myConf = myFactory.CreateConference(myConnector);
    
     @Override
    public void ProcessEvent(XMSEvent a_event) {
        //throw new UnsupportedOperationException("Not supported yet.");
            XMSCall myCall=a_event.getCall();
        switch(a_event.getEventType()){
            case CALL_CONNECTED:
                myConf.ConferenceOptions.SetLayout(Layout.AUTO);
                myConf.ConferenceOptions.SetMediaType(XMSMediaType.VIDEO);
                myConf.AddOptions.SetAudioDirection(XMSMediaDirection.SENDRECV);
                myConf.AddOptions.SetVideoDirection(XMSMediaDirection.SENDRECV);
                myConf.Add(myCall);
                break;
            
            case CALL_DISCONNECTED:  // The far end hung up will simply wait for the media
                myConf.Remove(myCall);
                myCall.WaitcallOptions.SetMediaType(XMSMediaType.VIDEO);
                myCall.Waitcall();
                break;
            default:
                System.out.println("Unknown Event Type!!");
        }
     }
        public void RunDemo(){
        
        
        //Enable all events to go back to my event handler
            myCall1.EnableAllEvents(this);
        //Set to a Video Call
            myCall1.WaitcallOptions.SetMediaType(XMSMediaType.VIDEO);
        //Wait for an inbound call and start the state machine
            myCall1.Waitcall();  
       
        //Enable all events to go back to my event handler
            myCall2.EnableAllEvents(this);
        //Set to a Video Call
            myCall1.WaitcallOptions.SetMediaType(XMSMediaType.VIDEO);
        //Wait for an inbound call and start the state machine
            myCall2.Waitcall();  
       
        //Enable all events to go back to my event handler
            myCall3.EnableAllEvents(this);
        //Set to a Video Call
            myCall1.WaitcallOptions.SetMediaType(XMSMediaType.VIDEO);
        //Wait for an inbound call and start the state machine
         myCall3.Waitcall();  
       
        //Enable all events to go back to my event handler
            myCall4.EnableAllEvents(this);
        //Set to a Video Call
            myCall1.WaitcallOptions.SetMediaType(XMSMediaType.VIDEO);
        //Wait for an inbound call and start the state machine
            myCall4.Waitcall();  
       
        }
}
