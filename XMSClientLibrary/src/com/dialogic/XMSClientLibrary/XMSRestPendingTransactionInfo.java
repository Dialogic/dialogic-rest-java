/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dialogic.XMSClientLibrary;

/**
 *
 * @author dwolansk
 */
class XMSRestPendingTransactionInfo {

    private String m_description="";
    public void setDescription(String a_desc){
        m_description=a_desc;
    }
    public String getDescription(){
        return m_description;
    }
    
    private String m_transactionId="";
    public void setTransactionId(String a_id){
        m_transactionId=a_id;
    }
    public String getTransactionId(){
        return m_transactionId;
    }
    
    private SendCommandResponse m_scr= null;
    public void setResponseData(SendCommandResponse a_scr){
        m_scr=a_scr;
    }
    public SendCommandResponse getResponseData(){
        return m_scr;
    }
    public void Reset(){
        m_description=null;
        m_transactionId=null;
        m_scr=null;
    }
    @Override
    public String toString(){
        return "Transaction ID:"+m_transactionId+" Description:"+m_description;
    }
}
