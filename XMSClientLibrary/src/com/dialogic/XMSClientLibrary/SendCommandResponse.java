/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package  com.dialogic.XMSClientLibrary;

/**
 * This is used as a basic class to get get the return and respond from
 * the XMSRestConn.SendCommand()
 * 
 * @author dslopres
 */
//TODO Need to update this interface to make it less "REST"y
public class SendCommandResponse {

    private int     scr_status_code;
    private String  scr_return_xml_payload;
    private String  scr_identifier;
    private String  scr_transaction_id;
    private String  src_source;
    
    /**
     * Set the transaction ID.  transaction_id A unique ID that is assigned by 
     * the PowerMedia XMS RESTful web service for the the specific
     * play, playrecord,play_collect>, or overlay action.
     *
     * @param a_scr_transaction_id 
     */
    void set_scr_transaction_id(String a_scr_transaction_id){

        scr_transaction_id = a_scr_transaction_id;

    }

    /**
     * Returns srouce_uri
     * 
     * @return srouce_uri
     */
    public String get_scr_source(){

        return src_source;

    }

    /**
     * Set the source URI.  The address of the connected server.
     * 
     * @param a_source
     */
    void set_scr_source(String a_source){

        src_source = a_source;

    }

    /**
     * Returns transaction ID from execution of an operation.
     * 
     * @return scr_transaction_id
     */
    public String get_scr_transaction_id(){

        return scr_transaction_id;

    }

    /**
     * Set the identifier.  Identifier is a unique ID of a call resource.
     * 
     * @param a_identifier 
     */
    void set_scr_identifier(String a_identifier){

        scr_identifier = a_identifier;

    }

    /**
     * Return the Send Command Response identifier.  This is returned
     * from the XMS server when operation is done.
     * 
     * @return scr_identifier
     */
    public String get_scr_identifier(){

        return scr_identifier;

    }



    /**
     * Set the HTTP status code after execution of http command.
     * @param a_status_code 
     */
    void set_scr_status_code(int a_status_code){  //changed a_return_code to a_status_code

        scr_status_code = a_status_code;

    }
    /**
     * Returns the status code after execution of http code.  Typically will be
     * a 2xx or 4xx code. 
     * 
     * @return scr_status_code
     */
    public int get_scr_status_code(){

        return scr_status_code;

    }

    /**
     * Returns the XMS payload after execution of a http commands.
     * 
     * @return scr_return_xml_payload
     */
    public String get_scr_return_xml_payload(){

        return scr_return_xml_payload ;
        
    }

    /**
     * Set the the member variable with the XML payload obtained after execution
     * of http command.
     * 
     * @param a_return_xml_payload 
     */
    void set_scr_return_xml_payload(String a_return_xml_payload){

        scr_return_xml_payload = a_return_xml_payload;
        
    }

@Override
public String toString(){
    String RetStr = "scr_status_code="+scr_status_code+
    " scr_identifier="+scr_identifier+
    " scr_transaction_id="+scr_transaction_id+
    " src_source="+src_source+
    " scr_return_xml_payload="+scr_return_xml_payload;
    
    return RetStr;
}



}
