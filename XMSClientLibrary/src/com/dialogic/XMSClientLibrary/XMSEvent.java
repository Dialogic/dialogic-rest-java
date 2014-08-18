/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dialogic.XMSClientLibrary;


/**
 *
 * @author dwolansk
 */
public class XMSEvent {
    public void CreateEvent(XMSEventType a_type, XMSObject a_obj, String a_data, String a_reason, String a_internal){
        m_evttype=a_type;
        if(a_obj instanceof XMSCall){
            m_call=(XMSCall)a_obj;
        }
        m_object = a_obj;
        m_internalData = a_internal;
        m_data=a_data;
        m_reason=a_reason;
  //      m_duration="";
    }
    private XMSEventType m_evttype;
    /**
     * Obtain the Event Type
     * @return 
     */
    public XMSEventType getEventType(){
        return m_evttype;
    }
     private XMSObject m_object;
    /**
     * Obtain the XMS Object for the event
     * @return 
     */
    public XMSObject getXMSObject(){
        return m_object;
    }   
    private XMSCall m_call;
    /**
     * Obtain the Call Object for the event
     * @return 
     */
    public XMSCall getCall(){
        return m_call;
    }
    
    private String m_reason;
    /**
     * This is to Obtain the reason for the event (ie max-digits, end etc)
     * @return 
     */
    public String getReason(){
        return m_reason;
    }
    public void setReason(String a_reason){
        m_reason = a_reason;
    }
//    private String m_duration;
//    /**
//     * Obtains the duration of the event in MS if available. 
//     * @return 
//     */
//    public String getDuration(){
//        return m_duration;
//    }
//    public void setDuration(String a_duration){
//        m_duration=a_duration;
//    }
//    
    private String m_data;
    /**
     * Obtains any event specific data such as the digit string 
     * @return 
     */
    public String getData(){
        return m_data;
    }
    public void setData(String a_data){
        m_data=a_data;
    }
    
    private String m_internalData;
    /**
     * Obtain any low level internal information or event contents
     * NOTE= This String will likely contain Technology SPecific data
     * @return 
     */
    public String getInternalData(){
        return m_internalData;
    }
    public void setInternalData(String a_data){
        m_internalData= a_data;
    }
    /**
     * 
     * @return 
     */
    @Override
    public String toString(){
        return "XMSEventType="+getEventType()+" XMSCall="+getCall()+" Data="+getData()+" Reason="+getReason();
    }
}
