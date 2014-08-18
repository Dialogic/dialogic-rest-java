/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dialogic.XMSClientLibrary;


/**
 *
 * @author dwolansk
 *  Default values 
 * m_locale="en-US";
 * m_dateFormat="mdy";
 * m_digitFormat="gen";
 * m_durationFormat="min";
 * m_moneyFormat="usd";
 * m_numberFormat="crd";
 * m_timeFormat="t12";
 * m_terminateDigits="0";
 * m_voice="susan";
 */
public class XMSPlayPhraseOptions {

    
    String m_locale;
    String m_dateFormat;
    String m_digitFormat;
    String m_durationFormat;
    String m_moneyFormat;
    String m_numberFormat;
    String m_timeFormat;
    String m_terminateDigits;
    
    String m_voice;

    /**
     * This will Instantiate and Reset all the values to their defaults
     */
    public XMSPlayPhraseOptions(){
        Reset();
    }
    /**
     * Resets all the contents back to default
      *  Default values <br>
 * m_locale="en-US";<br>
 * m_dateFormat="mdy";<br>
 * m_digitFormat="gen";<br>
 * m_durationFormat="min";<br>
 * m_moneyFormat="usd";<br>
 * m_numberFormat="crd";<br>
 * m_timeFormat="t12";<br>
 * m_terminateDigits="#";<br>
 * m_voice="susan";<br>
 */
     
    public void Reset(){
        m_locale="en-US";
        m_dateFormat="mdy";
        m_digitFormat="gen";
        m_durationFormat="min";
        m_moneyFormat="usd";
        m_numberFormat="crd";
        m_timeFormat="t12";
        m_terminateDigits="#";
        m_voice="susan";
   

    }
    /**Return the correctly formated parm for a given type
     * 
     * @param a_type
     * @return 
     */
    public String GetSubtypeFormat(XMSPlayPhraseType a_type){
        switch(a_type){
            case  DATE:  //The value is spoken as a date in the form specified by the subtype. 
                return m_dateFormat;
              
            case DIGITS:  //The value is spoken as a string of digits, one at a time, in the form specified by the subtype. 
                return m_digitFormat;
              
            case DURATION: //Duration is specified in seconds and is spoken in one or more units of time as specified by the subtype
                return m_durationFormat;
              
            case MONEY: //Money is specified in the smallest unit of currency for the indicated subtype. The          
            //value is converted and spoken, per the subtype, as large units of currency followed by the 
            //remainder in smaller units of currency (for example, dollars and cents).
                return m_moneyFormat;
              
            case NUMBER: //The value is a number in cardinal or ordinal form as specified by the subtype.
                return m_numberFormat;
            case TIME: //The value is spoken as a time of day in either twelve or twenty-four hour HHMM 
            //format according to ISO 8601, International Data and Time, as specified by the subtype.
                return m_timeFormat;
              
    
            default:
                return "";
        }
    }

    /** Set the Locale for the lang that is used 
     * 
     * @param a_locale - default = en-US
     */
    public void SetLocale (String a_locale){
        m_locale=a_locale;
    }
    /** Set the Date Format
     * 
     * @param a_format - default = mdy
     * <br><br>The value is always specified as YYYYMMDD (per ISO 8601, International Date and Time Notation)<br> 
     * Example:<br>
     * YYYY: 1900-2999 <br>
     * MM: 01-12<br>
     * DD: 01-31 <br><br><br>
     * AllowedFormats = mdy, dmy, ymd
     */
    public void SetDateFormat (String a_format){
        m_dateFormat=a_format;
    }
    /** Set the Digit Format
     * 
     * @param a_format - default = gen<br>  <br>
     * The value numbers from 0-9 <br><br>
     * AllowedFormats <br>
     * <u>ndn</u> (Digits are spoken with North American dialing phone number phrasing (NPA-NXX-XXXX) with appropriate pauses. )<br><br>
     * <u>gen</u>   Digits are spoken as generic digits, one at a time (one, five, zero) with no pause.<br><br>
     */
    public void SetDigitFormat (String a_format){
        m_digitFormat=a_format;
    }
    /** Set the Duration Format
     * 
     * @param a_format - default = min <br><br>
     * DefaultFormat = min The value is converted and spoken as minutes and seconds.  <br><br>
     * The Value is 1 – 4,294,967,295 (>136 years) <br><br>
     * AllowedFormats <br>
     * yrs - The value is converted and spoken as years, days, hours, minutes, and seconds. <br>
     * <i>Example: 31626061 is spoken as "one year, one day, one hour, one minute, and one second"</i> <br><br>
     * <u>hrs</u> - The value is converted and spoken as hours, minutes, and seconds. <br>
     * <i>Example: 3600 is spoken as "one hour" 3661 is spoken as "one hour, one minute, and one second"</i> <br><br>
     * <u>min</u>=The value is converted and spoken as minutes and seconds. <br>
     * <i>Example: 60 is spoken as "one minute" 3661 is spoken as "sixty one minutes, and one second"</i> <br><br>
     */
    public void SetDurationFormat (String a_format){
        m_durationFormat=a_format;
    }    
   /**Set the Money Format
    *  
    * @param a_format - default usd 
    */
    public void SetMoneyFormat (String a_format){
        m_moneyFormat=a_format;
    }
    
   /**Set the number Format
    *  
    * @param a_format - default crd
    */     
    public void SetNumberFormat (String a_format){
        m_numberFormat=a_format;
    }   
   /**Set the Time Format
    *  
    * @param a_format - default t12
    */     
    public void SetTimeFormat (String a_format){
        m_timeFormat=a_format;
    }        
   
       
        
    
    
    /**Set the voice
    *  
    * @param a_voice - "susan"
    */     
    public void SetVoice (String a_voice){
        m_voice=a_voice;
    }        
        /**
     * Set Terminating Digits.  The digit or digits [0-9,*,#] used to terminate 
     * the play. Valid values are 0-9, * or #. The default value is #.
     * 
     * @param a_digitValue - Terminating digits
     */
    public void SetTerminateDigits(String a_digitValue){
        m_terminateDigits=a_digitValue;
    }
     
    @Override
    public String toString()
    {
        String RtnStr;

        /**
         * NEED TO DISCUSS THIS SOME MORE
         */
        RtnStr = "PlayPhraseOptions: "+
        " m_locale="+m_locale+
        " m_dateFormat="+m_dateFormat+
        " m_digitFormat="+m_digitFormat+
        " m_durationFormat="+m_durationFormat+
        " m_moneyFormat="+m_moneyFormat+
        " m_numberFormat="+m_numberFormat+
        " m_timeFormat="+m_timeFormat+
        " m_voice="+m_voice+
        " m_terminateDigits"+m_terminateDigits+
        "";
        
        return RtnStr;
    }
}
