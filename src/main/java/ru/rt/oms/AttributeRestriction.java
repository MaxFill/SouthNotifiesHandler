package ru.rt.oms;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "AttributeRestriction")
@XmlEnum
public enum AttributeRestriction {

    @XmlEnumValue("model")
    MODEL("model"),

    @XmlEnumValue("dynamic")
    DYNAMIC("dynamic");
    private final String value;

    AttributeRestriction(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static AttributeRestriction fromValue(String v) {
        for (AttributeRestriction c: AttributeRestriction.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
