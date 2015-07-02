/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dialogic.XMSClientLibrary;

/**
 *
 * @author rdmoses
 */
public class XMSRecordOptions {

    public XMSMediaType m_mediaType;

    public String m_terminateDigits;
    public String m_offset;
    public String m_delay;
    public String m_repeat;
    public boolean m_isBargeEnable;
    public boolean m_clearDB;
    public boolean m_isBeepEnabled;
    public String m_timeoutValue;
    public String m_maxTime;

    /**
     * This will Instantiate and Reset all the values to their defaults
     */
    public XMSRecordOptions() {
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
        m_isBargeEnable = false;
        m_clearDB = true;
        m_isBeepEnabled = true;
        m_timeoutValue = "";
        m_maxTime = "30s";

    }

    /**
     * Set Max Time.
     *
     * The maximum length of time to record.
     *
     *
     * @param a_maxTime - seconds
     */
    public void SetMaxTime(int a_maxTime) {
        m_maxTime = a_maxTime + "s";
    }

    /**
     * Set Terminating Digits. The digit or digits [0-9,*,#] used to terminate
     * the play_record.
     *
     * @param a_digitValue - Terminating digits
     */
    public void SetTerminateDigits(String a_digitValue) {
        m_terminateDigits = a_digitValue;
    }
// Note I commented these out so that users don't set them as they are 
//  not Applicable on a Record because they are play options
//    /**
//     * Set Offset
//     * @param a_offsetValue - Offset time in seconds.
//     */
//    public void SetOffset(String a_offsetValue){
//        m_offset=a_offsetValue+"s"; // Add 's' at the end of the string for seconds
//    }
//
//    /**
//     * Set Delay
//     * @param a_delayValue - Delay time in seconds
//     */
//    public void SetDelay(String a_delayValue){
//        m_delay=a_delayValue+"s"; // Add 's' at the end of the string for seconds
//    }
//
//    /**
//     * Set Repeat
//     * @param a_repeatValue - How many times do you want to repeat the play
//     */
//    public void SetRepeat(String a_repeatValue){
//        m_repeat=a_repeatValue;
//    }
//

    /**
     * Set Media Type should be enabled on the outbound call
     *
     * @param a_mediaType - AUDIO or VIDEO
     */
    public void SetMediaType(XMSMediaType a_mediaType) {
        m_mediaType = a_mediaType;
    }
//    /**
// * 
// * @param a_isBargeEnable 
// */
//    //TODO do we want to support this as is there something on other technologies.
//   public void EnableBarge(boolean a_isBargeEnable){
//       m_isBargeEnable=a_isBargeEnable;
//   }

    /**
     * Set if there was a specific DTMF that will terminate (ex #) Specifies
     * whether previous input should be considered or ignored for the purpose of
     * barge-in.
     *
     * Valid values are Yes or No.
     *
     * When set to Yes, any previously buffered digits are discarded.
     *
     * When set to No, previously buffered digits will be considered. When set
     * to No, with the barge parameter set to Yes, previously buffered digits
     * will result in the recording phase starting immediately, and the prompt
     * will not be played.
     *
     *
     * @param a_clearDBon
     */
    public void EnableClearDigitBufferOnExecute(boolean a_clearDBon) {
        m_clearDB = a_clearDBon;
    }

    /**
     * Set if there should be a beep at the start of the recording.
     *
     * Specifies whether to play a tone when starting to record.
     *
     * Valid values are Yes or No.
     *
     * @param a_beepon
     */
    public void EnableBeepOnExecute(boolean a_beepon) {
        m_isBeepEnabled = a_beepon;
    }

    /**
     * Set Timeout
     *
     * @param a_timeoutValue - Timeout in seconds.
     *
     * TODO - not clear if this applies to records.
     */
    public void SetTimeout(int a_timeoutValue) {
        m_timeoutValue = a_timeoutValue + "s"; // Append 's' at the end of the string for seconds
    }

    @Override
    public String toString() {
        String RtnStr;

        /**
         * NEED TO DISCUSS THIS SOME MORE
         */
        RtnStr = "RecordOptions: terminateDigits=" + m_terminateDigits
                + " ClearDigitBufferOnExecute=" + m_clearDB
                + " isBeepEnabled=" + m_isBeepEnabled
                + " TimeoutValue=" + m_timeoutValue
                + //                " isBargeEnable"+m_isBargeEnable+
                //                " offset="+m_offset+
                //                " delay"+m_delay+
                //                " repeat"+m_repeat+
                " mediaType=" + m_mediaType
                + "";
        return RtnStr;
    }
}
