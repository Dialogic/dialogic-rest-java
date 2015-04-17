/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dialogic.XMSClientLibrary;

/**
 *
 * @author dwolansk
 */
public class XMSSendInfoOptions {
    String m_contentType;
    /**
     * This will Instantiate and Reset all the values to their defaults.
     */
    public XMSSendInfoOptions(){
        Reset();
    }
    /**
     * Resets all the contents back to default.
     * 
     * Default is to set the mediatype to audio.
     */
    public void Reset(){
        m_contentType="text/plain";
    }
    /**
     * Set the Content type for the Send message
     * @param a_type - Default set to "text/plain"
     */
    public void SetContentType(String a_type){
        m_contentType=a_type; 
    }
     
    @Override
    public String toString()
    {
        return "SendInfoOptions: m_contenttype="+m_contentType;
    }
}
