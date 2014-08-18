/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dialogic.XMSClientLibrary;

/**
 * Enum that is used as input to the XMSRestconn.SendCommand method.
 * 
 * It indicates what type of http operation will be done, i.e. POST, 
 * GET, UPDATE, or DELETE.
 * 
 * @author dslopres
 */
public enum RESTOPERATION {

    /** 
     * *C*reate a new resource
     * 
     *  RESPONSE BODY:  Contents of newly created resource.
     */
    POST,   

    /**
     *  *R*etrieve information for all instances of a specific resource
     *  type, or retrieve information regarding a specific resource.
     * 
     *  RESPONSE BODY: Contents of resource information.This can be for
     *  all instances of a specific resource type or for an individual resource.
     */
    GET,    

    /**
     *  *U*pdate an existing resource
     *   
     *   RESPONSE BODY: Contents of updated resource.
     */
    PUT,    
    
    /**
     *  *D*elete an existing resource or all existing resources.
     *  
     *   RESPONSE BODY: N/A
     */
     DELETE  
}
