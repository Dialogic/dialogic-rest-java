/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package xmsInboundPlay;
import com.dialogic.XMSClientLibrary.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 *
 * @author dwolansk
 */
public class XMSInboundPlay {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        XMSObjectFactory myFactory = new XMSObjectFactory();
        // REST
        //XMSConnector myConnector = myFactory.CreateConnector("XMSConnectorConfig.xml");
        //MSML changed the config file
        XMSConnector myConnector = myFactory.CreateConnector("ConnectorConfig.xml");
        XMSCall myCall = myFactory.CreateCall(myConnector);
        
            
    Properties prop = new Properties();
         String PlayFile=null;
         boolean isVideo=true;
        
        
         
    	try {
               
               //prop.setProperty("PlayFile","./verification/Dialogic_NetworkFuel" );
               //prop.setProperty("isVideo","true" );
               // prop.store(new FileOutputStream("config.properties"), null);
                //load a properties file
                
    		prop.load(new FileInputStream("config.properties"));
 
                PlayFile=prop.getProperty("PlayFile");
                if(prop.getProperty("isVideo").contains("false")){
                    isVideo=false;
                } 
 
    	} catch (IOException ex) {
    		ex.printStackTrace();
        }
        
        
        //Make an outbound call to the same address that you just received a call from
        if(isVideo)
            myCall.WaitcallOptions.SetMediaType(XMSMediaType.VIDEO);
        myCall.Waitcall();
        
        if(myCall.getState()== XMSCallState.CONNECTED){
        //Playback the file recorded
            if(isVideo)
                myCall.PlayOptions.SetMediaType(XMSMediaType.VIDEO);
//            myCall.Play("image:id=menu&header=IVVR Demo&items=1 Play&items=2 Record&items=3 Playback&items=4 Conf&footer=Dialogic");
            myCall.Play(PlayFile);
        }
        Sleep(5000);
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
