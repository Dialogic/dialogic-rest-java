/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dialogic.XMSClientLibrary;

/**
 *
 * @author dwolansk
 */
public class XMSWaitcallOptions extends XMSAnswercallOptions{
    boolean m_autoConnectEnabled;
    /**
     * This will Instanciate and Reset all the values to thier defaults
     */
    public XMSWaitcallOptions(){
        Reset();
    }
    /**
     * Resets all the contents back to default
     */
    @Override
    public void Reset(){
        super.Reset();
        m_autoConnectEnabled = true;
    }
    /**
     * Set if Waitcall should return to app at offered or connected
     * @param m_autoConnectEnabled - true or false if it should be enabled
     */
    public void EnableAutoConnect(boolean a_isEnabled){
        m_autoConnectEnabled=a_isEnabled; 
    }

    public boolean isAutoConnect(){
        return m_autoConnectEnabled;
    
    }
    @Override
    public String toString()
    {
        return "WaitcallOptions: autoConnectEnabled="+m_autoConnectEnabled+" / "+super.toString();
    }
}
