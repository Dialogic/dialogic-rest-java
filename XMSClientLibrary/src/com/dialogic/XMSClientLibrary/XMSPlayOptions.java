/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dialogic.XMSClientLibrary;

/**
 *
 * @author rdmoses
 */
public class XMSPlayOptions {

    public XMSMediaType m_mediaType;

    public String m_terminateDigits;
    public String m_offset;
    public String m_delay;
    public String m_repeat;

    /**
     * This will Instantiate and Reset all the values to their defaults
     */
    public XMSPlayOptions() {
        Reset();
    }

    /**
     * Resets all the contents back to default
     */
    public void Reset() {

        m_terminateDigits = "#";
        m_offset = "0s";
        m_delay = "0s";
        m_repeat = "0";
        m_mediaType = XMSMediaType.AUDIO;
    }

    /**
     * Set Terminating Digits
     *
     * @param a_digitValue - Terminating digits
     */
    public void SetTerminateDigits(String a_digitValue) {
        m_terminateDigits = a_digitValue;
    }

    /**
     * Set Offset
     *
     * @param a_offsetValue - Offset time in seconds.
     */
    public void SetOffset(String a_offsetValue) {
        m_offset = a_offsetValue + "s"; // Add 's' at the end of the string for seconds
    }

    /**
     * Set Delay
     *
     * @param a_delayValue - Delay time in seconds
     */
    public void SetDelay(String a_delayValue) {
        m_delay = a_delayValue + "s"; // Add 's' at the end of the string for seconds
    }

    /**
     * Set Repeat
     *
     * @param a_repeatValue - How many times do you want to repeat the play
     */
    public void SetRepeat(String a_repeatValue) {
        m_repeat = a_repeatValue;
    }

    /**
     * Set Media Type should be enabled on the outbound call
     *
     * @param a_mediaType - AUDIO or VIDEO
     */
    public void SetMediaType(XMSMediaType a_mediaType) {
        m_mediaType = a_mediaType;
    }

//TODO Add in Barge Enable
//  /**
// * 
// * @param a_isBargeEnable 
// */
//    //TODO do we want to support this as is there something on other technologies.
//   public void EnableBarge(boolean a_isBargeEnable){
//       m_isBargeEnable=a_isBargeEnable;
//   }
    @Override
    public String toString() {
        String RtnStr;

        /**
         * NEED TO DISCUSS THIS SOME MORE
         */
        RtnStr = "PlayOptions: terminateDigits=" + m_terminateDigits
                + " m_offset=" + m_offset
                + " m_delay=" + m_delay
                + " m_repeat=" + m_repeat
                + " mediaType=" + m_mediaType;
        return RtnStr;
    }
}
