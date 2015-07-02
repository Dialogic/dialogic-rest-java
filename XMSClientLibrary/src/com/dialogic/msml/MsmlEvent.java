/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dialogic.msml;

import javax.sip.message.Request;
import javax.sip.message.Response;

/**
 *
 * @author ssatyana
 */
public class MsmlEvent {

    private MsmlEventType type;
    private long contentLength;
    private String content;
    private Request req;
    private Response res;
    private XMSSipCall call;

    public MsmlEvent() {
        this.type = MsmlEventType.IDLE;
        this.contentLength = 0;
        this.content = "";
        this.req = null;
        this.res = null;
        this.call = null;
    }

    /**
     * @return the type
     */
    public MsmlEventType getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(MsmlEventType type) {
        this.type = type;
    }

    /**
     * @return the length
     */
    public long getContentLength() {
        return contentLength;
    }

    /**
     * @param length the length to set
     */
    public void setContentLength(long length) {
        this.contentLength = length;
    }

    /**
     * @return the content
     */
    public String getContent() {
        return content;
    }

    /**
     * @param content the content to set
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * @return the req
     */
    public Request getReq() {
        return req;
    }

    /**
     * @param req the req to set
     */
    public void setReq(Request req) {
        this.req = req;
    }

    /**
     * @return the res
     */
    public Response getRes() {
        return res;
    }

    /**
     * @param res the res to set
     */
    public void setRes(Response res) {
        this.res = res;
    }

    /**
     * @return the call
     */
    public XMSSipCall getCall() {
        return call;
    }

    /**
     * @param call the call to set
     */
    public void setCall(XMSSipCall call) {
        this.call = call;
    }
}
