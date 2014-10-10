/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package xmsMultiConfDemoAsync;

import com.dialogic.XMSClientLibrary.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dwolansk
 */
public class XMSMultiConfDemoAsync implements XMSEventCallback{
    boolean isRunning=true; //Set this to fales to get it to exit.
    
    XMSObjectFactory myFactory = new XMSObjectFactory();
    XMSConnector myConnector = myFactory.CreateConnector("XMSConnectorConfig.xml");
    
    
    List<XMSCall> myCallList=new ArrayList<XMSCall>();
    Map<String,XMSConference> myConfMap = new HashMap<String,XMSConference>();
    
    
    int myCallCount=4;
    void Initialize(int callcount){
        myCallCount=callcount;
        
        for(int x=0;x<myCallCount;x++){
            myCallList.add(myFactory.CreateCall(myConnector));
        }
        
    }
    
    void AddToConf(XMSCall myCall){
        String ConfURI=myCall.getCalledAddress();
        
        if(myConfMap.containsKey(ConfURI)){
                XMSConference myConf=myConfMap.get(ConfURI);
              myConf.ConferenceOptions.SetLayout(0);
                myConf.ConferenceOptions.SetMediaType(XMSMediaType.VIDEO);
                myConf.AddOptions.SetAudioDirection(XMSMediaDirection.SENDRECV);
                myConf.AddOptions.SetVideoDirection(XMSMediaDirection.SENDRECV);
                myConf.Add(myCall);
            
        } else{
            XMSConference myConf = myFactory.CreateConference(myConnector);
            myConfMap.put(ConfURI,myConf);
            myConf.Add(myCall);
        }
    }
    void RemoveFromConf(XMSCall myCall){
        String ConfURI=myCall.getCalledAddress();
        
        if(myConfMap.containsKey(ConfURI)){
                XMSConference myConf=myConfMap.get(ConfURI);
                myConf.Remove(myCall);
                if(myConf.GetPartyCount()==0){
                    myConfMap.remove(ConfURI);
                }            
        } 
    }
     @Override
    public void ProcessEvent(XMSEvent a_event) {
        //throw new UnsupportedOperationException("Not supported yet.");
            XMSCall myCall=a_event.getCall();
        switch(a_event.getEventType()){
            case CALL_CONNECTED:
                AddToConf(myCall);
                 
                break;
            
            case CALL_DISCONNECTED:  // The far end hung up will simply wait for the media
                RemoveFromConf(myCall);
                myCall.WaitcallOptions.SetMediaType(XMSMediaType.VIDEO);
                myCall.Waitcall();
                break;
            default:
                System.out.println("Unknown Event Type!!");
        }
     }
        public void RunDemo(){
        
        
            for(XMSCall myCall: myCallList){
                //Enable all events to go back to my event handler
                myCall.EnableAllEvents(this);
            //Set to a Video Call
                myCall.WaitcallOptions.SetMediaType(XMSMediaType.VIDEO);
            //Wait for an inbound call and start the state machine
                myCall.Waitcall();  
            }
        
       
        }
}
