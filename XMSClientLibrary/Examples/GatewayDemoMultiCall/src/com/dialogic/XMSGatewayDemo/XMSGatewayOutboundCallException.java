/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dialogic.XMSGatewayDemo;

/**
 *
 * @author dwolansk
 */
public class XMSGatewayOutboundCallException extends Exception{
     public XMSGatewayOutboundCallException ()
        {
        }

    public XMSGatewayOutboundCallException (String message)
        {
        super (message);
        }

    public XMSGatewayOutboundCallException (Throwable cause)
        {
        super (cause);
        }

    public XMSGatewayOutboundCallException (String message, Throwable cause)
        {
        super (message, cause);
        }
}
