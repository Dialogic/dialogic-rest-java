/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dialogic.XMSClientLibrary;

/**
 *
 * @author dwolansk
 */
public class XMSConferenceOptions {
    
    String m_MaxParties = "9";
    XMSMediaType m_MediaType = XMSMediaType.VIDEO;
    String m_Layout = "0"; //TODO need to change this to a Enum with meaningful values
    boolean m_CaptionEnabled=true;
    int m_CaptionDuration=20;
    boolean m_BeepEnabled = true;
    boolean m_DigitClampingEnabled = true;
    boolean m_AGCEnabled = true;
    boolean m_ECEnabled = true;
    
   public XMSConferenceOptions(){
       Reset();
   }
   
   public void Reset(){
     m_MaxParties = "9";
     m_MediaType = XMSMediaType.VIDEO;
     m_Layout = "0"; //TODO need to change this to a Enum with meaningful values
     m_CaptionEnabled=true;
     m_CaptionDuration=20;
     m_BeepEnabled = true;
     m_DigitClampingEnabled = true;
     m_AGCEnabled = true;
     m_ECEnabled = true;
       
   }
   public void SetMaxParties(int a_maxparties){
     m_MaxParties = ""+a_maxparties;  
   }
   
     public void SetMediaType(XMSMediaType a_type){
         m_MediaType= a_type;
     }
     public void SetLayout(int a_layout){
         //TODO ENUM the layout for conferencing
         m_Layout = ""+a_layout;
     }
     public void EnableCaption(boolean a_isenabled){
         m_CaptionEnabled = a_isenabled;
    }
     public void SetCaptionDuration(int a_duration){
         m_CaptionDuration=a_duration;
     }
     public void EnableBeep(boolean a_isenabled){
         m_BeepEnabled = a_isenabled;
     }
     public void EnableDigitClamping(boolean a_isenabled){
         m_DigitClampingEnabled=a_isenabled;
     }
     public void EnableAutoGainControl(boolean a_isenabled){
         m_AGCEnabled=a_isenabled;
     }
     public void EnableEchoCancellation(boolean a_isenabled){
         m_ECEnabled=a_isenabled;
     }
       
      @Override
    public String toString()
    {
        return "m_MaxParties="+m_MaxParties+ 
    " m_MediaType=" + m_MediaType+
    " m_Layout=" + m_Layout+
    " m_CaptionEnabled=" + m_CaptionEnabled+
    " m_CaptionDuration="+ m_CaptionDuration+
    " m_BeepEnabled=" + m_BeepEnabled+
    " m_DigitClampingEnabled="+m_DigitClampingEnabled+
    " m_AGCEnabled="+m_AGCEnabled+
    " m_ECEnabled="+m_ECEnabled;
    }
     
}
