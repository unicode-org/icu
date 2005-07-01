/*
 *******************************************************************************
 * Copyright (C) 2002-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.localeconverter;

import java.io.*;
import java.util.*;

/**

The ConvertJavaLocale application converts java locales to 
Java and ICU Locale files.  It's usage is as follows

    ConvertJavaLocale [-11] [-12] [-icu] [-package] locale...

Usage

-11
    If this option is specified, data is output in 
    Java 1.1.x locale format.
    
-12
    If this option is specified, data is output in
    Java 1.2.x locale format.  If an output format
    is not specified, -12 is the default.
    
-icu
    If this option is specified, data is output in
    ICU locale format.

locale
    The locale to convert


*/

public class ConvertJavaLocale {
    private static final byte OPT_11 = (byte)0x01;
    private static final byte OPT_12 = (byte)0x02;
    private static final byte OPT_ICU = (byte)0x04;
    private static final byte OPT_PACKAGE = (byte)0x08;
    private static final byte OPT_UNKNOWN = (byte)0x80;
    private static final String USER_OPTIONS[] = {
        "-11", 
        "-12", 
        "-icu",
        "-package"
    };
    
    private static final String[] tags = {
        "LocaleString",
        "LocaleID",
        "ShortLanguage",
        "ShortCountry",
        "Languages",
        "Countries",
        "MonthNames",
        "MonthAbbreviations",
        "DayNames",
        "DayAbbreviations",
        "AmPmMarkers",
        "Eras",
        "NumberPatterns",
        "NumberElements",
        "CurrencyElements",
        "DateTimePatterns",
        "DateTimeElements",   
        "collations",
        "zoneStrings",
        "localPatternChars",
    };



    public static void main(String args[]) {
        try {
            new ConvertJavaLocale(args, System.out);
        } catch (Throwable t) {
            System.err.println("Unknown error: "+t);
        }
    }
    
    public ConvertJavaLocale(String args[], PrintStream out) {
        process(args, out);
    }
    
    public void process(String args[], PrintStream out) {
        short options = identifyOptions(args);
        if ((args.length < 1) || ((options & OPT_UNKNOWN) != 0)) {
            printUsage();
        } else {
            String localeName = null;
            String packagename = null;
            for (int i = 0; i < args.length; i++) {
                if(args[i].equalsIgnoreCase("-package")){
                   i++;
                   packagename = args[i];
                }else if(args[i].equalsIgnoreCase("-icu")){
                }else if (!args[i].startsWith("-")) {
                    localeName = args[i];
                }
            }
            final Hashtable data = new Hashtable();
            final String localeElements;
            final String dateFormatZoneData;
            
            if(localeName != null){
                if(!localeName.equals("root")){
                    localeElements = packagename+".LocaleElements" +"_"+localeName;
                    dateFormatZoneData = packagename+".DateFormatZoneData" + "_"+localeName;
                }else{
                    localeElements = packagename+".LocaleElements";
                    dateFormatZoneData = packagename+".DateFormatZoneData";
                }
            }else{
                printUsage();
                return;
            }
            addLocaleData(localeElements, data);
            addLocaleData(dateFormatZoneData, data);
            final Locale locale = localeFromString(localeName);
            if ((options & OPT_11) != 0) {
                new Java1LocaleWriter(out, System.err).write(locale, data);
            }
            if ((options & OPT_12) != 0) {
                new JavaLocaleWriter(out, System.err).write(locale, data);
            }
            if ((options & OPT_ICU) != 0) {
                new ICULocaleWriter(out, System.err).write(locale, data);
            }
        }
    }

    private void addLocaleData(final String bundleClassName, final Hashtable data) {
        try {
            final Class bundleClass = Class.forName(bundleClassName);
            final ResourceBundle bundle = (ResourceBundle)bundleClass.newInstance();
            for (int i = 0; i < tags.length; i++) {
                try {
                    final Object resource = bundle.getObject(tags[i]);
                    data.put(tags[i], resource);
                } catch (MissingResourceException e) {
                }               
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Could not find bundle class for bundle: "+bundleClassName);
        } catch (InstantiationException e) {
            System.err.println("Could not create bundle instance for bundle: "+bundleClassName);
        } catch (IllegalAccessException e) {
            System.err.println("Could not create bundle instance for bundle: "+bundleClassName);
        }
    }
    
    private void printUsage() {
        System.err.println("Usage: ConvertJavaLocale [-11] [-12] [-icu] [-package] <package name> localeName");
    }
    
    private short identifyOptions(String[] options) {
        short result = 0;
        for (int j = 0; j < options.length; j++) {
            String option = options[j];
            if (option.startsWith("-")) {
                boolean optionRecognized = false;
                for (short i = 0; i < USER_OPTIONS.length; i++) {
                    if (USER_OPTIONS[i].equals(option)) {
                        result |= (short)(1 << i);
                        optionRecognized = true;
                        break;
                    }
                }
                if (!optionRecognized) {
                    result |= OPT_UNKNOWN;
                }
            }
        }
        return result;
    }
    
    private Locale localeFromString(final String localeName) {
        if (localeName == null) return new Locale("", "", "");
        String language = localeName;
        String country = "";
        String variant = "";
        
        int ndx = language.indexOf('_');
        if (ndx >= 0) {
            country = language.substring(ndx+1);
            language = language.substring(0, ndx);
        }
        ndx = country.indexOf('_');
        if (ndx >= 0) {
            variant = country.substring(ndx+1);
            country = country.substring(0, ndx);
        }
        return new Locale(language, country, variant);
    }
}
