/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dialogic.XMSClientLibrary;

/**
 *
 * @author rdmoses
 */
public class XMSCollectDigitsOptions {        

    String m_terminateDigits;
    String m_timeoutValue;    
    String m_maxDigits;    
    boolean m_clearDB;

    /**
     * This will Instantiate and Reset all the values to their defaults
     */
    public XMSCollectDigitsOptions(){
        Reset();
    }
    /**
     * Resets all the contents back to default
     */
    public void Reset(){

        m_terminateDigits="#";
        m_timeoutValue="0s";
        m_maxDigits="1";        
        m_clearDB=true;

    }
    /**
     * Set Terminating Digits
     * @param a_digitValue - Terminating digit(s)
     */
    public void SetTerminateDigits(String a_digitValue){
        m_terminateDigits=a_digitValue;
    }

    /**
     * Set Timeout
     * @param a_timeoutValue - Timeout in seconds.
     */
    public void SetTimeout(int a_timeoutValue){
        m_timeoutValue=a_timeoutValue+"s"; // Append 's' at the end of the string for seconds
    }

    /**
     * Clear Digit Buffer
     * @param a_clearDB - boolean option (true=Yes, false=no)
     */
    public void EnableClearDigitBufferOnExecute(boolean a_clearDB){
        m_clearDB=a_clearDB;
    }

    /**
     * Set Max Digits
     * @param a_maxDigitValue - Maximum digit value
     */
    public void SetMaxDigits(int a_maxDigitValue){
        m_maxDigits=""+a_maxDigitValue;
    }





    @Override
    public String toString()
    {
        String RtnStr;

        /**
         * NEED TO DISCUSS THIS SOME MORE
         */
        RtnStr = "CollectOptions: terminateDigits="+m_terminateDigits+
                "m_timeoutValue="+m_timeoutValue+
                "m_maxDigits"+m_maxDigits+
                "m_clearDB"+m_clearDB;
        return RtnStr;
    }
}
