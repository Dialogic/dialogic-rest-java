/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dialogic.XMSClientLibrary;

/**
 * This enum will contain the Call states for an XMSCall
 * @author dwolansk
 */
public enum XMSCallState {
    NULL,  
    MAKECALL, //outbound calls
    ACCEPTED,
    CONNECTED, // Call is active for either Inbound or outbound
    PLAY,
    PLAYCOLLECT,
    COLLECTDIGITS,
    PLAYRECORD,
    RECORD,
    JOINING,
    WAITCALL, // Waiting for an inbound call
    OFFERED, // inbound calls
    UPDATECALL,
    DISCONNECTED, 
    REJECTED,
    SENDMESSAGE
}
