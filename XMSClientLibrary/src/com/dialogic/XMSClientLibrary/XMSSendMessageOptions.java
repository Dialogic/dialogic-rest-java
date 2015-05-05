/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dialogic.XMSClientLibrary;

/**
 *
 * @author dwolansk
 */
public class XMSSendMessageOptions {
    String m_contentType;
    String m_messageMode;
    String m_id;
    /**
     * This will Instantiate and Reset all the values to their defaults.
     */
    public XMSSendMessageOptions(){
        Reset();
    }
    /**
     * Resets all the contents back to default.
     * 
     * Default is to set the mediatype to audio.
     */
    public void Reset(){
        m_contentType="text/plain";
        m_messageMode="signalling";
        m_id="id1";
    }
    /**
     * Set the Content type for the Send message
     * @param a_type - Default set to "text/plain"
     */
    public void SetContentType(String a_type){
        m_contentType=a_type; 
    }
        /**
     * Set the message mode for the Send message
     * @param a_mode - Default set to "signalling"
     * options are: signalling, msrp or rfc5547
     */
    public void SetMessageMode(String a_mode){
        m_messageMode=a_mode; 
    }
    /**
     * Set the id for the Send message
     * @param a_id - Default set to "id1"
     */
    public void SetId(String a_id){
        m_id=a_id; 
    } 
    @Override
    public String toString()
    {
        return "SendMessageOptions:"+
                " m_messagemode="+m_messageMode+
                " m_contenttype="+m_contentType+
                " m_id="+m_id;
    }
}
