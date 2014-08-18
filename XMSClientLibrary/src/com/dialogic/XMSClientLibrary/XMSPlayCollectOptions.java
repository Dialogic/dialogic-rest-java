/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dialogic.XMSClientLibrary;

public class XMSPlayCollectOptions {

    XMSMediaType m_mediaType;

    String  m_terminateDigits;
    String  m_offset;
    String  m_delay;
    String  m_repeat;
    boolean m_isBargeEnable;
    boolean m_clearDB;
    String  m_timeoutValue; 
    String  m_maxDigits; 
    boolean m_toneDetection;     // 15-Jun-2012 dsl 
       
    /**
     * This will Instantiate and Reset all the values to their defaults
     */
    public XMSPlayCollectOptions(){
        Reset();
    }
    /**
     * Resets all the contents back to default
     *     terminateDigits="#"
        offset="0s"
        delay="0s"
        repeat="0"
        mediaType=XMSMediaType.AUDIO
        BargeEnable=true
        clearDB=true
        timeoutValue=""
     */
    public void Reset(){

        m_terminateDigits   = "#";
        m_offset            = "0s";
        m_delay             = "0s";
        m_repeat            = "0";
        m_mediaType         = XMSMediaType.AUDIO;
        m_isBargeEnable     = true;
        m_clearDB           = true;
        m_maxDigits         = "1";   
        m_timeoutValue      = "";
        m_toneDetection     = false;  // 15-Jun-2012 dsl
    
    } // end Reset()
    
   
   /**
    * Sets tone detection. <play_collect>
    * 
    * Enable tone detection. Valid values are Yes or No. The default value is No.
    * 
    * @param a_toneDetection default value is No
    */
    public void SetToneDetection(boolean a_toneDetection){
        m_toneDetection=a_toneDetection;
    } // End SetToneDetection()
    
    
    
    /**
     * Set Terminating Digits.  The digit or digits [0-9,*,#] used to terminate 
     * the play. Valid values are 0-9, * or #. The default value is #.
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
     * only). The offset is applied to the initial play only. The default 
     * is zero (0) seconds.
     * 
     * @param a_offsetValue - Offset time in seconds.
     */
    public void SetOffset(String a_offsetValue){
        m_offset=a_offsetValue+"s"; // Add 's' at the end of the string for seconds
    }
    /**
     * Set Max Digits.  
     * 
     * Specifies the maximum number of digits to collect.
     * 
     * @param a_maxDigitValue - Maximum digit value
     */
    public void SetMaxDigits(int a_maxDigitValue){
        m_maxDigits=""+a_maxDigitValue;
    }

    /**
     * Set Delay.  Time delay between repeated plays. The default is one (1) second.
     * 
     * @param a_delayValue - Delay time in seconds
     */
    public void SetDelay(String a_delayValue){
        m_delay=a_delayValue+"s"; // Add 's' at the end of the string for seconds
    }

    /**
     * Set Repeat.  
     * 
     * Number of times to repeat the play. Use "infinite" to repeat 
     * indefinitely. "file://" URIs only. 
     * 
     * The default is zero (0) seconds
     * 
     * @param a_repeatValue - How many times do you want to repeat the play
     */
    public void SetRepeat(String a_repeatValue){
        m_repeat=a_repeatValue;
    }

    /**
     * Set Media Type should be enabled on the outbound call.
     * 
     * Specifies the media type supported by the call. Valid values are 
     * Audio or Audiovideo.
     * 
     * @param a_mediaType - AUDIO or VIDEO
     */
    public void SetMediaType(XMSMediaType a_mediaType){
        m_mediaType=a_mediaType;
    }
    /**
 * 
 * @param a_isBargeEnable 
 */
    //TODO do we want to support this as is there something on other technologies.
   public void EnableBarge(boolean a_isBargeEnable){
       m_isBargeEnable=a_isBargeEnable;
   }
       
/**
 * Set if there was a specific DTMF that will terminate (ex #)
 * @param a_clearDBon 
 */
  public void EnableClearDigitBufferOnExecute(boolean a_clearDBon){
        m_clearDB=a_clearDBon;
    }
      /**
     * Set Timeout.
     * 
     * Specifies the maximum length of time to wait for digits or tones. This 
     * time begins when the prompt phase ends. The default value is infinite.
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
        RtnStr = "PlayCollectOptions: terminateDigits="+m_terminateDigits+
                " ClearDigitBufferOnExecute="+m_clearDB+
                " TimeoutValue="+m_timeoutValue+
                " MaxDigit="+m_maxDigits+
                " isBargeEnable="+m_isBargeEnable+
                " offset="+m_offset+
                " delay="+m_delay+
                " repeat="+m_repeat+
                " mediaType="+m_mediaType+
                "";
        return RtnStr;
    }
}
