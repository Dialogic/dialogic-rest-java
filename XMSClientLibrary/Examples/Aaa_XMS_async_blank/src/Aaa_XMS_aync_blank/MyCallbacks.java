/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Aaa_XMS_aync_blank;
import com.dialogic.XMSClientLibrary.*;

/**
 *
 * @author dwolansk
 */
enum AppState{
    NULL,  
    MAKECALL, //outbound calls
    CONNECTED, // Call is active for either Inbound or outbound
    PLAY,
    PLAYCOLLECT,
    COLLECTDIGITS,
    PLAYRECORD,
    RECORD,
    JOINING,
    WAITCALL, // Waiting for an inbound call
    OFFERED, // inbound calls
    DISCONNECTED, 
    REJECTED
}
public class MyCallbacks implements XMSEventCallback{
    
    AppState myState=AppState.WAITCALL;
    boolean isDone = false;
   
    @Override
    public void ProcessEvent(XMSEvent a_event) {
              
            XMSCall myCall=a_event.getCall();
        
            switch(a_event.getEventType()){
                     
                    //CALL_CONNECTED - call is connected via Makecall or WaitCall.
                    case CALL_CONNECTED:
                        System.out.println("***** STATE: " + myState.toString() + " / EVENT - CALL_CONNECTED / Reason: " + a_event.getReason() + " *****");
                        
                     break;
                     
                     //CALL_PLAY_END – play has finished, either play complete or play stopped
                    case CALL_PLAY_END:
                        System.out.println("*****  STATE: " + myState.toString() + " / EVENT - CALL_PLAY_END / Reason: " + a_event.getReason() + "  *****");    
                                                      
                         break;
                     
                    //CALL_OFFERED - newly arrived inbound call is offered to the user application.
                    case CALL_OFFERED:
                        System.out.println("*****  STATE: " + myState.toString() + " / EVENT - CALL_OFFERED / Reason: " + a_event.getReason() + "  *****");  

                        break;

                     //CALL_COLLECTDIGITS_END - digit collection has completed due one of the following reasons; maxdigits, timeone, tone, or action stopped        
                    case CALL_COLLECTDIGITS_END:
                        System.out.println("*****  STATE: " + myState.toString() + " / EVENT - CALL_COLLECTDIGITS_END / Reason: " + a_event.getReason() + "  *****");  
                                                              
                        break;

                     //CALL_PLAYCOLLECT_END - play completed due one of the following reasons; maxdigits, timeone, tone, or action stopped CALL_PLAYRECORD_END -
                    case CALL_PLAYCOLLECT_END:
                        System.out.println("*****  STATE: " + myState.toString() + " / EVENT - CALL_PLAYCOLLECT_END / Reason: " + a_event.getReason() + "  *****");  

                        break;

                    //    
                    case CALL_PLAYRECORD_END:
                        System.out.println("*****  STATE: " + myState.toString() + " / EVENT - CALL_PLAYRECORD_END / Reason: " + a_event.getReason() + "  *****");  
                               
                        break; 

                    //CALL_DISCONNECTED – call has been disconnected, i.e. terminated.    
                    case CALL_DISCONNECTED:  
                        System.out.println("*****  STATE: " + myState.toString() + " / EVENT - CALL_DISCONNECTED / Reason: " + a_event.getReason() + "  *****");  
                     
                        break;

                    // CALL_RECORD_END – record completed due one of the following reasons; maxdigits, timeone, tone, or action stopped   
                    case CALL_RECORD_END:
                        System.out.println("*****  STATE: " + myState.toString() + " / EVENT - CALL_RECORD_END / Reason: " + a_event.getReason() + "  *****");   
                        
                        break;

                    default:
                        System.out.println("Unknown Event Type!!");
        }//switch
    }
}
