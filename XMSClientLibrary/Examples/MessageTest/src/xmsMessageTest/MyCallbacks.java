/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package xmsMessageTest;
import com.dialogic.XMSClientLibrary.*;
/**
 *
 * @author dwolansk
 */


public class MyCallbacks implements XMSEventCallback{

    XMSRestConnector restcon;
    
     public MyCallbacks(XMSRestConnector myRestcon){

        restcon = myRestcon; 
    }
    
    @Override
    public void ProcessEvent(XMSEvent a_event) {
        //throw new UnsupportedOperationException("Not supported yet.");
        
        
       
            XMSCall myCall=a_event.getCall();
        switch(a_event.getEventType()){
            case CALL_CONNECTED:
             //  myCall.SendMessage("Please Ask me your question?");
                break;
            case CALL_SENDMESSAGE_END:
                System.out.println("\n\nMessage Status !!"+a_event.getReason());
                break;
            case CALL_MESSAGE:
                if(a_event.getData()==null || a_event.getData().isEmpty()){
                    System.out.println("\n\nMessage is emplty, will not echo");
                }
                else if(a_event.getData().contains("isComposing") || a_event.getData().contains("iscomposing")){
                    System.out.println("\n\nMessage is a \"Composing\" message, will not echo");
                }else {
                    String contents = a_event.getData(); 
                    System.out.println("\n\n Message is "+contents+"\n\n");
                    
                    
                    String startIndex = "text-indent:0px;\">"; 
                                  
                    int position = contents.indexOf(startIndex);
                    
                    contents = contents.substring(position+startIndex.length(), contents.length()-1);
                   
                    myCall.SendMessageOptions.SetMessageMode("msrp");
                    String endIndex = "<";
                    position = contents.indexOf(endIndex);
                    contents = contents.substring(0, position);             

                    int randomNum = (int) (Math.random() * 6);

                    switch (randomNum) {
                       case 0:  
                                myCall.SendMessage("Your Question: " + contents.toString() + " My Answer: It is certain");
                                break;
                       case 1:  
                                myCall.SendMessage("Your Question: " + contents.toString() + " My Answer: It is decidedly so");
                                break;
                        case 2:  
                                myCall.SendMessage("Your Question: " + contents.toString() + " My Answer: you may rely on it");
                                break;
                        case 3: 
                                myCall.SendMessage("Your Question: " + contents.toString() + " My Answer: Most likely");
                                break;     
                        case 4: 
                                myCall.SendMessage("Your Question: " + contents.toString() + " My Answer: Reply hazy try again");
                                break;               
                        case 5: 
                                myCall.SendMessage("Your Question: " + contents.toString() + " My Answer: Better not tell you now");
                                break; 
                         case 6: 
                                myCall.SendMessage("Your Question: " + contents.toString() + " My Answer: Cannot predict now");
                                break;                 
                         case 7: 
                                myCall.SendMessage("Your Question: " + contents.toString() + " My Answer: Don't count on it");
                                break;                                                           
                         case 8: 
                                myCall.SendMessage("Your Question: " + contents.toString() + " My Answer: My sources say no");
                                break;                                                                                            
                         case 9: 
                                myCall.SendMessage("Your Question: " + contents.toString() + " My Answer: Yes!");
                                break;                                                                                                                                
                          case 10: 
                                myCall.SendMessage("Your Question: " + contents.toString() + " My Answer: Very doubtful");
                                break; 
                    } 
                            
                }
                break;
            case CALL_DISCONNECTED: 
                myCall.Dropcall();
                myCall.Waitcall();
                break;
            default:
                System.out.println("Unknown Event Type!!");
        }
    
    }
    
}
