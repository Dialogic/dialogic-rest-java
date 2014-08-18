/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dialogic.XMSGatewayDemo;

/**
 *
 * @author dwolansk
 */
public class XMSGatewayInboundCallException extends Exception{
     public XMSGatewayInboundCallException ()
        {
        }

    public XMSGatewayInboundCallException (String message)
        {
        super (message);
        }

    public XMSGatewayInboundCallException (Throwable cause)
        {
        super (cause);
        }

    public XMSGatewayInboundCallException (String message, Throwable cause)
        {
        super (message, cause);
        }
}
