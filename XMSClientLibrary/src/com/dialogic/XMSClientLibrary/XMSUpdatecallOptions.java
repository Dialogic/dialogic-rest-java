/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dialogic.XMSClientLibrary;

/**
 *
 * @author dwolansk
 */
public class XMSUpdatecallOptions extends XMSMakecallOptions{
    @Override
    public String toString()
    {
        String RtnStr;

        /**
         * NEED TO DISCUSS THIS SOME MORE
         */
        RtnStr = "XMSUpdateCallOptions: cpaEnabled="+m_cpaEnabled+
                " mediaType="+m_mediaType+
                " iceEnabled="+m_iceEnabled+
                " encryptionEnabled="+m_encryptionEnabled+
                " signalingEnabled="+m_signalingEnabled+
                " sdp="+m_sdp;
                

        return RtnStr;
    }
}
