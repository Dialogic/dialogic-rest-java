/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dialogic.XMSClientLibrary;


public class XMSPlayRecordOptions {

    XMSMediaType m_mediaType;

    String  m_terminateDigits;
    String  m_offset;
    String  m_delay;
    String  m_repeat;
    boolean m_isBargeEnable;
    boolean m_clearDB;
    boolean m_isBeepEnabled;
    String  m_timeoutValue; 
    String  m_maxTime;
    
    
    /**
     * This will Instantiate and Reset all the values to their defaults
     */
    public XMSPlayRecordOptions(){
        Reset();
    }
    /**
     * Resets all the contents back to default                          <br><br>
     *  terminateDigits="#"                                             <br><br>
        offset="0s"                                                     <br><br>
        delay="0s"                                                      <br><br>
        repeat="0"                                                      <br><br>
        mediaType=XMSMediaType.AUDIO                                    <br><br>
        BargeEnable=true                                                <br><br>
        clearDB=true                                                    <br><br>
        BeepEnabled = true                                              <br><br>
        timeoutValue=""                                                 <br><br>
      * maxTime="30s"                                                 <br><br>
     */
    public void Reset(){

        m_terminateDigits   = "#";
        m_offset            = "0s";
        m_delay             = "0s";
        m_repeat            = "0";
        m_mediaType         = XMSMediaType.AUDIO;
        m_isBargeEnable     = false;
        m_clearDB           = true;
        m_isBeepEnabled     = true;
        m_timeoutValue      = "";
        m_maxTime           = "30s";
    
    } // end reset
    
    /**
     * Set max_time.<br><br>
     * 
     * The maximum length of time to record.
     * 
     * @param a_maxTime - Terminating digits
     */
    public void SetMaxTime(String a_maxTime){
        m_maxTime=a_maxTime;
    }
       
    
    
    /**
     * Set Terminating Digits.<br><br>
     * 
     * The digit or digits [0-9,*,#] used to terminate the play_record.
     * 
     * @param a_digitValue - Terminating digits
     */
    public void SetTerminateDigits(String a_digitValue){
        m_terminateDigits=a_digitValue;
    }
// Note I commented these out so that users don't set them as they are not Applicable on a Record because they are play options
    /**
     * Set Offset.
     * 
     * Specifies the time offset from where the play should start (.wav files 
     * only). 
     * 
     * NOTE: Applies only to the initial play.
     * 
     * @param a_offsetValue - Offset time in seconds.
     */
    public void SetOffset(String a_offsetValue){
        m_offset=a_offsetValue+"s"; // Add 's' at the end of the string for seconds
    }

    /**
     * Set Delay.<br><br>
     * 
     * Specifies the time delay between repeated plays. The default value is 
     * one (1) second.
     * 
     * @param a_delayValue - Delay time in seconds
     */
    public void SetDelay(String a_delayValue){
        m_delay=a_delayValue+"s"; // Add 's' at the end of the string for seconds
    }

    /**
     * Set Repeat.<br><br>
     * 
     * Specifies the number of times to repeat the play.
     * 
     * @param a_repeatValue - How many times do you want to repeat the play
     */
    public void SetRepeat(String a_repeatValue){
        m_repeat=a_repeatValue;
    }

    /**
     * Set Media Type should be enabled on the outbound call.
     * 
     * Sets the media type supported by the call. Valid values are Audio or 
     * Audiovideo. The default is Audio.
     * 
     * @param a_mediaType - AUDIO or VIDEO
     */
    public void SetMediaType(XMSMediaType a_mediaType){
        m_mediaType=a_mediaType;
    }
    /**
    * Specifies whether DTMF digit input will barge the prompt and force 
    * transition to the record phase. 
    * 
    * Valid values are Yes or No. 
    * 
    * NOTE: If barge is set to No, the cleardigits parameter implicitly 
    * has the value Yes.
    * 
    * @param a_isBargeEnable 
    */
        //TODO do we want to support this as is there something on other technologies.
   public void EnableBarge(boolean a_isBargeEnable){
       m_isBargeEnable=a_isBargeEnable;
   }
       
/**
 * Set if want to cleardigits.
 * <br> <br>
 * Specifies whether previous input should be considered or ignored for the 
 * purpose of barge-in. Valid values are Yes or No. 
 * <br><br>
 * When set to Yes, any previously buffered digits are discarded. 
 * <br><br>
 * When set to No, previously buffered digits will be considered. When set 
 * to No, with the barge parameter set to Yes, previously buffered digits will 
 * result in the recording phase starting immediately, and the prompt will 
 * not be played.
 * <br><br>
 * 
 * @param a_clearDBon 
 */
  public void EnableClearDigitBufferOnExecute(boolean a_clearDBon){
        m_clearDB=a_clearDBon;
    }
/**
 * Sett if there should be a beep at the start of the recording
 * @param a_beepon 
 */
   public void EnableBeepOnExecute(boolean a_beepon){
       m_isBeepEnabled=a_beepon;
   }
      /**
     * Set Timeout.  <br><br>
     * 
     * Specifies the maximum length of time to wait for digits or tones. 
     * This time begins when the prompt phase ends. 
     * The default value is infinite.<br><br>
     * 
     * TODO docs indicate that this is for play_collect.  Not sure if valid
     * for PlayRecord
     * 
     * @param a_timeoutValue - Timeout in seconds.
     */
    public void SetTimeout(int a_timeoutValue){
        m_timeoutValue=a_timeoutValue+"s"; // Append 's' at the end of the string for seconds
    }

        
    @Override
    public String toString()
    {
        String RtnStr;

        /**
         * NEED TO DISCUSS THIS SOME MORE
         */
        RtnStr = "RecordOptions: terminateDigits="+m_terminateDigits+
                " ClearDigitBufferOnExecute="+m_clearDB+
                " isBeepEnabled="+m_isBeepEnabled+
                " TimeoutValue="+m_timeoutValue+
                " isBargeEnable="+m_isBargeEnable+
                " offset="+m_offset+
                " delay="+m_delay+
                " repeat="+m_repeat+
                " mediaType="+m_mediaType+
                "";
        return RtnStr;
    }
}
