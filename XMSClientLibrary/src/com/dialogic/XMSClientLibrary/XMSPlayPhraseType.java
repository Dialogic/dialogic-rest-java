/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dialogic.XMSClientLibrary;

/**
 *
 * @author dwolansk
 */
/*

/**
 * This enum will contain the Call states for an XMSCall
 * @author dwolansk
 */
public enum XMSPlayPhraseType {
    /** The value is spoken as a date in the form specified by the subtype. <br> <br>
     *  DefaultFormat = mdy<br> 
     * The value is always specified as YYYYMMDD (per ISO 8601, International Date and Time Notation)<br> 
     * Example:<br>
     * YYYY: 1900-2999 <br>
     * MM: 01-12<br>
     * DD: 01-31 <br><br><br>
     * AllowedFormats = mdy, dmy, ymd
     */
    DATE,  
     /** The value is spoken as a string of digits, one at a time, in the form specified by the subtype. . <br> <br>
     *  DefaultFormat = gen   Digits are spoken as generic digits, one at a time (one, five, zero) with no pause.<br>  <br>
     * The value numbers from 0-9 <br><br>
     * AllowedFormats <br>
     * <u>ndn</u> (Digits are spoken with North American dialing phone number phrasing (NPA-NXX-XXXX) with appropriate pauses. )<br><br>
     * <u>gen</u>   Digits are spoken as generic digits, one at a time (one, five, zero) with no pause.<br><br>
     */
    DIGITS,  
    /** Duration is specified in seconds and is spoken in one or more units of time as specified by the subtype <br><br>
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
    DURATION, 
    /** Money is specified in the smallest unit of currency for the indicated subtype. 
     * The value is converted and spoken, per the subtype, as large units of currency 
     * followed by the remainder in smaller units of currency (for example, dollars and cents).
     * <br><br>
     * DefaultFormat = usd
     * AllowedFormats <br>
     * <u>usd</u> - US dollar (cents) (format: $$¢¢) <br>
     * <i>Example: 1000 is spoken as "ten dollars" 1025 is spoken as "ten dollars and twenty five cents" 25 is spoken as "twenty five cents"</i> <br><br>
     * <u>cny</u> - Chinese yuan (fen) (format: $$¢¢) <br>
     * <i>Example: 1255050 is spoken as "one wan, two thousand, five hundred, five shi, dollar, and fifty cents"</i> <br><br>
     * */
    MONEY, 
    /** The value is spoken as a month and is specified in the MM format, with 01 denoting January, 02 denoting February, 10 denoting October, and so forth. <br>
     * DefaultFormat = Null <br>
     
    */
    MONTH, 
    /** The value is a number in cardinal or ordinal form as specified by the subtype.<br><br>
     * DefaultFormat = crd <br><br>
     * AllowedFormats <br>
     * <u>crd</u> - Cardinal 1 is spoken as "one" 5,111 is spoken as "five thousand, one hundred and eleven" 421 is spoken as "four hundred and twenty one"<br>
     * <u>ord</u> - Ordinal 1 is spoken as "first" 5,111 is spoken as "five thousand, one hundred and eleventh" 421 is spoken as "four hundred and twenty-first"
     * 
     */
    NUMBER, 
    /** The value is spoken as a time of day in either twelve or twenty-four hour HHMM 
     * format according to ISO 8601, International Data and Time, as specified by the subtype.
     * DefaultFormat - t12<br><br>
     * Value- The value is always specified as HHMM (per ISO 8601, International Date and Time Notation). 
     * HH: 00-24 refers to a zero padded hour, 2400(HHMM) denotes midnight at the end of the calendar day, 
     * MM: 00-59 refers to a minute.  <br><br>
     * AllowedFormats <br>
     * <u>t12</u> - 12 hour format <br>
     * <i> Example: 1730 is spoken as "five thirty p.m." 0530 is spoken as "five thirty a.m." 
     * 0030 is spoken as "twelve thirty a.m." 1230 is spoken as "twelve thirty p.m." </i> <br><br>
     * <u>T24</u> - 24 hour format <br>
     * <i>Example: 1700 is spoken as "seventeen hundred hours" 2400 is spoken as "twenty four hundred hours" </i>
     * */
    TIME, 
    /** The value is spoken as the day of the week. Days are specified as single digits, with 
     * 1 denoting Sunday, 2 denoting Monday, and so forth.  <br> 
     * DefaultFormat - Null<br>
     * Value - 1 – 7 <br>
1 = Sunday <br>
2 = Monday <br>
3 = Tuesday <br>
4 = Wednesday <br>
5 = Thursday <br>
6 = Friday <br>
7 = Saturday <br>
        */
    WEEKDAY,
    /** The value is a string of characters spoken as each individual character in the string. <br>
     * DefaultFormat - Null <br>
     * Value - <br>
     * a-Z, A-Z, 0-9, #, and * <br>
     * <i>Example: "a34bc" is spoken as "A, three, four, B, C" </i>

     */
    STRING ,
    /** Plays a period of silence.
     * 0 – 36000 (in 100 ms units up to 1 hour) 
     */
    SILENCE
    
}

