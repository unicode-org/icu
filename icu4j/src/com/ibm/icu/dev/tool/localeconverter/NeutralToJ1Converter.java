/*
 *******************************************************************************
 * Copyright (C) 2002-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.localeconverter;

import java.util.*;

public class NeutralToJ1Converter extends LocaleConverter {
    private static class Conversion {
        private String propName;
        private int ndx;
        protected Conversion() {
        }
        public Conversion(String propName) {
            this.propName = propName;
            this.ndx = -1;
        }
        public Conversion(String propName, int ndx) {
            this.propName = propName;
            this.ndx = ndx;
        }
        public String getPropName() {
            return propName;
        }
        public String convert(Hashtable source) throws ConversionError {
            Object sourceData = source.get(propName);
            if (sourceData == null) {
                return null;
            }
            if (ndx >= 0) {
                if (sourceData instanceof String[]) {
                    sourceData = ((String[])sourceData)[ndx];
                } else if (sourceData instanceof String[][]) {
                    sourceData = ((String[][])sourceData)[ndx];
                }
            }
            if (sourceData instanceof String) {
                return (String)sourceData;
            } else if (sourceData instanceof String[]) {
                String[] data = (String[])sourceData;
                StringBuffer result = new StringBuffer();
                for (int i = 0; i < data.length; i++) {
                    if (i > 0) result.append(';');
                    result.append(data[i]);
                }
                return result.toString();
            } else {
                throw new ConversionError("could not convert tag: "+propName);
            }
        }
    }

    private static class CollationConversion extends Conversion {
        public String convert(Hashtable source) throws ConversionError {
            Object[][] elements = (Object[][])source.get("collations");
            CollationItem[] items = (CollationItem[])elements[2][1];
            if (items == null) {
                return "";
            } else {
                StringBuffer result = new StringBuffer();
                for (int i = 0; i < items.length; i++) {
                    if(items[i]!=null){
                        result.append(items[i].toString());
                    }
                }
                return result.toString();
            }
        }
    }

    private static final Conversion[] conversions = {
        new Conversion("LocaleString"), /*locale id based on iso codes*/
        new Conversion("LocaleID"), /*Windows id*/
        new Conversion("ShortLanguage"), /*iso-3 abbrev lang name*/
        new Conversion("ShortCountry"), /*iso-3 abbrev country name*/
        new Conversion("Languages"), /*language names*/
        new Conversion("Countries"), /*country names*/
        new Conversion("MonthNames",0), /*january*/
        new Conversion("MonthNames",1), /*february*/
        new Conversion("MonthNames",2), /*march*/
        new Conversion("MonthNames",3), /*april*/
        new Conversion("MonthNames",4), /*may*/
        new Conversion("MonthNames",5), /*june*/
        new Conversion("MonthNames",6), /*july*/
        new Conversion("MonthNames",7), /*august*/
        new Conversion("MonthNames",8), /*september*/
        new Conversion("MonthNames",9), /*october*/
        new Conversion("MonthNames",10), /*november*/
        new Conversion("MonthNames",11), /*december*/
        new Conversion("MonthNames",12), /*month 13 if applicable*/
        new Conversion("MonthAbbreviations",0), /*abb january*/
        new Conversion("MonthAbbreviations",1), /*abb february*/
        new Conversion("MonthAbbreviations",2), /*abb march*/
        new Conversion("MonthAbbreviations",3), /*abb april*/
        new Conversion("MonthAbbreviations",4), /*abb may*/
        new Conversion("MonthAbbreviations",5), /*abb june*/
        new Conversion("MonthAbbreviations",6), /*abb july*/
        new Conversion("MonthAbbreviations",7), /*abb august*/
        new Conversion("MonthAbbreviations",8), /*abb september*/
        new Conversion("MonthAbbreviations",9), /*abb october*/
        new Conversion("MonthAbbreviations",10), /*abb november*/
        new Conversion("MonthAbbreviations",11), /*abb december*/
        new Conversion("MonthAbbreviations",12), /*abb month 13 if applicable*/
        new Conversion("DayNames",0), /*Monday*/
        new Conversion("DayNames",1), /*Tuesday*/
        new Conversion("DayNames",2), /*Wednesday*/
        new Conversion("DayNames",3), /*Thursday*/
        new Conversion("DayNames",4), /*Friday*/
        new Conversion("DayNames",5), /*Saturday*/
        new Conversion("DayNames",6), /*Sunday*/
        new Conversion("DayAbbreviations",0), /*abb Monday*/
        new Conversion("DayAbbreviations",1), /*abb Tuesday*/
        new Conversion("DayAbbreviations",2), /*abb Wednesday*/
        new Conversion("DayAbbreviations",3), /*abb Thursday*/
        new Conversion("DayAbbreviations",4), /*abb Friday*/
        new Conversion("DayAbbreviations",5), /*abb Saturday*/
        new Conversion("DayAbbreviations",6), /*abb Sunday*/
        new Conversion("AmPmMarkers",0), /*am marker*/
        new Conversion("AmPmMarkers",1), /*pm marker*/
        new Conversion("Eras"),/*era strings*/
        new Conversion("NumberPatterns",0), /*decimal pattern*/
        new Conversion("NumberPatterns",1), /*currency pattern*/
        new Conversion("NumberPatterns",2), /*percent pattern*/
        new Conversion("NumberElements",0), /*decimal separator*/
        new Conversion("NumberElements",1), /*group (thousands) separator*/
        new Conversion("NumberElements",2), /*list separator*/
        new Conversion("NumberElements",3), /*percent sign*/
        new Conversion("NumberElements",4), /*native 0 digit*/
        new Conversion("NumberElements",5), /*pattern digit*/
        new Conversion("NumberElements",6), /*minus sign*/
        new Conversion("NumberElements",7), /*exponential*/
        new Conversion("CurrencyElements",0), /*local currency symbol*/
        new Conversion("CurrencyElements",1), /*intl currency symbol*/
        new Conversion("CurrencyElements",2), /*monetary decimal separator*/
        new Conversion("DateTimePatterns",0), /*full time pattern*/
        new Conversion("DateTimePatterns",1), /*long time pattern*/
        new Conversion("DateTimePatterns",2), /*medium time pattern*/
        new Conversion("DateTimePatterns",3), /*short time pattern*/
        new Conversion("DateTimePatterns",4), /*full date pattern*/
        new Conversion("DateTimePatterns",5), /*long date pattern*/
        new Conversion("DateTimePatterns",6), /*medium date pattern*/
        new Conversion("DateTimePatterns",7), /*short date pattern*/
        new Conversion("DateTimePatterns",8), /*date-time pattern*/
        new Conversion("DateTimeElements",9), /*first day of week*/
        new Conversion("DateTimeElements",10), /*min days in first week*/
        new CollationConversion(), /*collation order*/
    };
    private Locale locale;
    private Locale parentLocale;
    private ResourceBundle defaultData;

    public NeutralToJ1Converter(Locale locale) {
        this.locale = locale;
        String language = locale.toString();
        String country = "";
        String variant = "";
        
        int ndx = language.indexOf('_');
        if (ndx >= 0) {
            country = language.substring(ndx+1);
            language = language.substring(0, ndx);
        }
        ndx = country.indexOf('_');
        if (ndx >= 0) {
            variant = country.substring(ndx);
            country = country.substring(0, ndx);
        }
        
        if ("".equals(country)) {
            language = "";
            variant = "";
        } else if ("".equals(variant)) {
            country = "";
        }
        
        parentLocale = new Locale(language, country, variant);
        defaultData =
            ResourceBundle.getBundle("com.ibm.icu.dev.tool.localeconverter.myLocaleElements", parentLocale);
            //{{INIT_CONTROLS
//}}
}
    
    /** convert the source table to the result */
    protected void convert(Hashtable result, Hashtable source) throws ConversionError {
        Vector localeElements = new Vector();
        for (int i = 0; i < conversions.length; i++) {
            final Conversion conv = conversions[i];
            final String newValue = conv.convert(source);
            if (newValue != null) {
                localeElements.addElement(newValue);
            } else {
                localeElements.addElement(defaultData.getObject(conv.getPropName()));
            }
        }
        result.put("LocaleElements", localeElements);
    }
    //{{DECLARE_CONTROLS
//}}
}