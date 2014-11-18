/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dialogic.XMSClientLibrary;

/**
 *
 * @author dwolansk
 */
public class XMSAddOptions {
    XMSMediaDirection m_videodirection;
    XMSMediaDirection m_audiodirection;
    String m_caption="";
    /**
     * This will Instantiate and Reset all the values to their defaults.
     */
    public XMSAddOptions(){
        Reset();
    }
    /**
     * Resets all the contents back to default.
     * 
     * Default is to set the mediatype to audio.
     */
    public void Reset(){
        m_videodirection = XMSMediaDirection.AUTOMATIC;
        m_audiodirection = XMSMediaDirection.AUTOMATIC;
        m_caption = "";
    }
    /**
     * Set the attribute for the party audio component (INACTIVE,RECVONLY,SENDONLY,SENDRECEIVE or AUTOMATIC)
     * @param a_direction 
     */
    public void SetAudioDirection(XMSMediaDirection a_direction){
        m_audiodirection=a_direction; 
    }
    /**
     * Set the attribute for the party video component (INACTIVE,RECVONLY,SENDONLY,SENDRECEIVE or AUTOMATIC)
     * @param a_direction 
     */
    public void SetVideoDirection(XMSMediaDirection a_direction){
        m_videodirection=a_direction; 
    }
    /**
     * Set the caption to be displayed.  If this is empty ie "" then the Calling party address will be used
     * @param a_caption
     */
    public void SetCaption(String a_caption){
        m_caption=a_caption; 
    }
 //   public String GetMediaTypeAsString(){
 //       
 //       return m_mediatype.toString().toLowerCase();
 //       
 //   }
    
    @Override
    public String toString()
    {
        return "AddOptions: audiodirection="+m_audiodirection+" videodirection="+m_videodirection+" caption="+m_caption;
    }
}
