/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dialogic.XMSClientLibrary;

/**
 *
 * @author Kids
 */
public enum XMSEventType {
    
    CALL_CONNECTED,  /// Call is connected via Makecall or WaitCall
    CALL_DISCONNECTED, // Call is Disconnected
    CALL_OFFERED, // Call is Offered
    CALL_PLAY_END,
    CALL_COLLECTDIGITS_END,
    CALL_PLAYCOLLECT_END,
    CALL_PLAYRECORD_END,
    CALL_RECORD_END,
    CALL_UPDATED,
    CALL_ALARM,
    CALL_MESSAGE ,
    CALL_SENDMESSAGE_END
       
}
