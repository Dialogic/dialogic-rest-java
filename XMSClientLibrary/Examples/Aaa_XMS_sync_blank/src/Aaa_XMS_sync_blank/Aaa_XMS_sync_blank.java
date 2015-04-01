/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Aaa_XMS_sync_blank;
import com.dialogic.XMSClientLibrary.*;

/**
 *
 * @author dwolansk
 */
public class Aaa_XMS_sync_blank {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        // TODO code application logic here
        XMSObjectFactory myFactory = new XMSObjectFactory();
        XMSConnector myConnector = myFactory.CreateConnector("XMSConnectorConfig.xml");
        
        //XMSCall myCall = myFactory.CreateCall(myConnector);
        //XMSConference myConf = myFactory.CreateConference(myConnector);
        
    }
}
