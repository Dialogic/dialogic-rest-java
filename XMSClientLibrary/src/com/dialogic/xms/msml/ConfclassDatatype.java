//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.5-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.08.04 at 02:42:49 PM EDT 
//


package com.dialogic.xms.msml;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for confclass.datatype.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="confclass.datatype">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="standard"/>
 *     &lt;enumeration value="preferred"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "confclass.datatype")
@XmlEnum
public enum ConfclassDatatype {

    @XmlEnumValue("standard")
    STANDARD("standard"),
    @XmlEnumValue("preferred")
    PREFERRED("preferred");
    private final String value;

    ConfclassDatatype(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ConfclassDatatype fromValue(String v) {
        for (ConfclassDatatype c: ConfclassDatatype.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
