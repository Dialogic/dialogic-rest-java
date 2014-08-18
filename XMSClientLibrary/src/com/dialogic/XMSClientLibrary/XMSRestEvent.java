/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dialogic.XMSClientLibrary;

import com.dialogic.xms.EventDocument;

/**
 *
 * @author Kids
 */
public class XMSRestEvent {
    String rawstring = "";
    String resourceId = "";
    String eventType = "";
    String resourceType = "";
    
    EventDocument.Event event;
    
    XMSCall call;
    XMSConnector connector;
    
    public XMSRestEvent(){
        rawstring = "";
        resourceId = "";
        eventType = "";
        resourceType = "";

        call=null;
        connector=null;
        event=null;

    }
    @Override
    public String toString(){
        
        return "XMSRestEvt- ID="+resourceId+" Type="+eventType+" ResourceType="+resourceType+" event="+event;
    }
}
