/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dialogic.XMSClientLibrary;

/**
 *
 * @author dwolansk
 */
public class XMSAnswercallOptions {
    XMSMediaType m_mediatype;
    /**
     * This will Instantiate and Reset all the values to their defaults.
     */
    public XMSAnswercallOptions(){
        Reset();
    }
    /**
     * Resets all the contents back to default.
     * 
     * Default is to set the mediatype to audio.
     */
    public void Reset(){
        m_mediatype = XMSMediaType.AUDIO;
    }
    /**
     * Set if CPA should be enabled on the outbound call
     * TODO this comment seems wrong.
     * @param a_isEnabled - true or false if it should be enabled
     */
    public void SetMediaType(XMSMediaType a_type){
        m_mediatype=a_type; 
    }
 //   public String GetMediaTypeAsString(){
 //       
 //       return m_mediatype.toString().toLowerCase();
 //       
 //   }
    
    @Override
    public String toString()
    {
        return "AnswercallOptions: m_calltype="+m_mediatype;
    }
}
