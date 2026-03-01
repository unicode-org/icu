// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl;

/**
 * Calendar type enum, moved from com.ibm.icu.util.Calendar.
 *
 * @author Yoshito Umaoka
 */
public enum CalType {
    GREGORIAN("gregorian"),
    ISO8601("iso8601"),

    BUDDHIST("buddhist"),
    CHINESE("chinese"),
    COPTIC("coptic"),
    DANGI("dangi"),
    ETHIOPIC("ethiopic"),
    ETHIOPIC_AMETE_ALEM("ethiopic-amete-alem"),
    HEBREW("hebrew"),
    INDIAN("indian"),
    ISLAMIC("islamic"),
    ISLAMIC_CIVIL("islamic-civil"),
    ISLAMIC_RGSA("islamic-rgsa"),
    ISLAMIC_TBLA("islamic-tbla"),
    ISLAMIC_UMALQURA("islamic-umalqura"),
    JAPANESE("japanese"),
    PERSIAN("persian"),
    ROC("roc"),

    UNKNOWN("unknown");

    String id;

    CalType(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
