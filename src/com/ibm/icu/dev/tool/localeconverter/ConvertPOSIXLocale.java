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

The ConvertPOSIXLocale application converts POSIX locale files to 
Java and ICU Locale files.  It's usage is as follows

    ConvertPOSIXLocale 
        [-LC_CTYPE]
        [-LC_TIME]
        [-LC_NUMERIC] 
        [-LC_MONETARY] 
        [-LC_MESSAGES] 
        [-11] 
        [-12] 
        [-icu]
        [-icu2]
        localeName
        localeDataFile 
        [charMapFile ...]
        
The application is invoked with options specifying the format(s) of
the locale file(s) to generate as well as the POSIX locale file and
character mapping files.

Usage

-LC_CTYPE
    If the -LC_CTYPE option is specified, the
    following items are added to the locale if they
    are present in the source: upper, lower, alpha, digit, 
    space, cntrl, punct, graph, print, xdigit, blank, 
    toupper, tolower.
    
<CODE>-LC_TIME
    If the -LC_TIME option is specified, the following
    items will be included if they are present in the POSIX source:
    abday, day, abmon, mon, d_t_fmt, d_ftm, t_fmt, am_pm,
    t_fmt_ampm, era, era_year, era_d_fmt, alt_digits.
    
-LC_NUMERIC
    If the -LC_NUMERIC option is specified, the following
    items will be included if they are present in the source:
    decimal_point, thousands_sep, grouping

-LC_MONETARY
    If the -LC_MONETARY option is specified, the following
    items will be included if they are present in the source:
    int_curr_symbol, currency_symbol, mon_decimal_point,
    mon_thousands_sep, mon_grouping, positive_sign, 
    negative_sign, int_frac_digits, frac_digits, p_cs_precedes,
    p_sep_by_space, n_cs_precedes, n_sep_by_space, p_sign_posn.

-LC_MESSAGES
    If the -LC_MESSAGES option is specified, the
    following items are added to the locale if they
    are present in the source: yesexpr, noexpr

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

localeName
    The name of the locale in the localeDataFile.  Ex.  en_US.

localeDataFile
    The localeDataFile path is required and specifies the path 
    to the locale data file.  If a "copy" directive is encountered 
    while processing the localeDataFile, ConvertPOSIXLocale will look
    in the same directory as the localeDataFile for additional
    POSIX locale data files.  Files must be in the POSIX format
    specified in ISO/IEC 9945- with exceptions noted below.  Exactly
    one localeDataFile must be specified.
    
charMapFile
    Zero or more character mapping files may be specified.  charMapFiles are used
    to map symbols in the localeDataFile to Unicode values.  They are processed
    as follows.  ConvertPOSIXLocale searchs for a line containing only the
    word "CHARMAP" and reads symbol mappings until it reaches a line
    containing only the words "END CHARMAP".  Symbol mappings have
    the form "<SYMBOL> <Uhhhh>" where "<SYMBOL>" is any symbol valid
    in a localeDataFile and "hhhh" is four hexidecimal digits representing
    the Unicode value for that symbol.  Surrogate pairs are not supported
    in charMapFiles. An example charMapFile might contain the following:

        CHARMAP
        <START_OF_TEXT>       <U0002>
        <E>                   <U0045>
        <q>                   <U0071>
        END CHARMAP

    specifying that the symbol <START_OF_TEXT> should be replaced by 
    the Unicode value of 0x0002 wherever it occurs.
    
    When multiple charMapFiles are specified, mappings in files listed 
    later take precedence over earlier ones.


Conversion to ICU and Java:

collations
    Converted from the LC_COLLATE section.  The "..." directive is ignored.
    The "replace-after" directive is ignored.

CurrencyElements
    element 0 is set to currency_symbol
    element 1 is set to int_curr_symbol
    element 2 is set to mon_decimal_point
    All other elements default.
    
NumberElements
    element 0 is set to decimal_point
    element 1 is set to thousands_sep

MonthNames is set to mon

MonthAbbreviations is set to abmon

DayNames is set to day

DayAbbreviations is set to abday

AmPmMarkers is set to am_pm

DateTimePatterns
    elements 0 through 3 are set to t_fmt_ampm with the patterns converted
    elements 4 through 7 are set to d_fmt with the patterns converted


Adition POSIX data may be included in the Locale as follows:

LC_TYPE 
    This section is ignored unless the -LC_CTYPE option is
    specified.  If the -LC_CTYPE option is specified, the
    following items are added to the locale if they
    are present in the source: upper, lower, alpha, digit, 
    space, cntrl, punct, graph, print, xdigit, blank, 
    toupper, tolower.

LC_MESSAGES
    
LC_MONETARY
    
LC_NUMERIC

LC_TIME
    If the -LC_TIME option is specified, the following
    items will be included if they are present in the source:
    abday, day, abmon, mon, d_t_fmt, d_ftm, t_fmt, am_pm,
    t_fmt_ampm, era, era_year, era_d_fmt, alt_digits.

LC_COLLATE
    Converted to collations in the resource file.
    
    
*/

public class ConvertPOSIXLocale {
    private static final short OPT_LC_CTYPE = 0x001;
    private static final short OPT_LC_TIME = 0x002;
    private static final short OPT_LC_NUMERIC = 0x004;
    private static final short OPT_LC_MONETARY = 0x008;
    private static final short OPT_LC_MESSAGES = 0x010;
    private static final short OPT_11 = 0x020;
    private static final short OPT_12 = 0x040;
    private static final short OPT_ICU = 0x080;
    private static final short OPT_ICU2 = 0x100;
    private static final short OPT_RAW = 0x200;
    private static final short OPT_UNKNOWN = 0x4000;
    private static final String USER_OPTIONS[] = {
        "-LC_CTYPE", 
        "-LC_TIME", 
        "-LC_NUMERIC", 
        "-LC_MONETARY",
        "-LC_MESSAGES",
        "-11", 
        "-12", 
        "-icu", 
        "-icu2", 
        "-RAW",
        "-enc",
    };
    private static final short OPT_CONVERT = (short)(OPT_LC_CTYPE | OPT_LC_TIME 
            | OPT_LC_NUMERIC | OPT_LC_MONETARY | OPT_LC_MESSAGES);
    
    private Hashtable data;

    public static void main(String args[]) {
        try {
            new ConvertPOSIXLocale(args);
        } catch (Throwable t) {
            t.printStackTrace();
            System.err.println("Unknown error: "+t);
        }
    }
    
    public ConvertPOSIXLocale(String args[]) {
        process(args);
            //{{INIT_CONTROLS
                //}}
    }
    
    public void process(String args[]) {
        short options = identifyOptions(args);
        String enc=null;
        if ((args.length < 2) || ((options & OPT_UNKNOWN) != 0)) {
            printUsage();
        } else {
            Vector mapFiles = new Vector();
            Locale locale = null;
            String fileName = null;
            for (int i = 0; i < args.length; i++) {
                final String thisArg = args[i];
                if (thisArg.startsWith("-")) {
                    if(thisArg.startsWith("-enc")){
                        enc = args[++i];
                    }
                } else if (locale == null) {
                    locale = localeFromString(thisArg);
                } else if (fileName == null) {
                    fileName = thisArg;
                } else {
                    mapFiles.addElement(thisArg);
                }
                
            }
            if(enc==null){
                enc="Default";
            }
            if ((fileName == null) || (locale == null) || (options == 0)) {
                printUsage();
            } else {
                PosixCharMap map = new PosixCharMap();
                Enumeration enum = mapFiles.elements();
                while (enum.hasMoreElements()) {
                    String mapFile = (String)enum.nextElement();
                    System.err.println("Locale: "+locale);
                    System.err.println("Loading character map file: "+mapFile);
                    try {
                        map.load(new File(mapFile),enc);
                    } catch (IOException e) {
                        System.err.println("Error loading map file: "+mapFile+"  "+e);
                        System.err.println("File skipped");
                    }
                }
                SymbolTransition.setCharMap(map);
                File dataFile = new File(fileName);
                System.err.println("Locale directory: "+dataFile.getParent());
                POSIXLocaleReader reader = new POSIXLocaleReader(dataFile.getParent(), locale);
                System.err.println("Parsing file: "+dataFile.getName());
                try {
                    data = reader.parse(dataFile.getName(), (byte)(options & OPT_CONVERT));
                    System.err.println("Converting....");
                    if ((options & OPT_11) != 0) {
                        new Java1LocaleWriter(System.out, System.err).write(locale, data);
                    }
                    if ((options & OPT_12) != 0) {
                        new JavaLocaleWriter(System.out, System.err).write(locale, data);
                    }
                    if ((options & OPT_ICU) != 0) {
                        new ICULocaleWriter(System.out, System.err).write(locale, data);
                    }
                    if ((options & OPT_ICU2) != 0) {
                        new ICU2LocaleWriter(System.out, System.err).write(locale, data);
                    }
                    if ((options & OPT_RAW) != 0) {
                        new ICULocaleWriter(System.out, System.err).write(locale, data);
                    }
                } catch (IOException e) {
                    System.err.println(e);
                }
            }
        }
    }

    private void printUsage() {
        System.err.println("Usage: ConvertPOSIXLocale [-LC_CTYPE] [-LC_TIME]"+
            " [-LC_NUMERIC] [-LC_MONETARY] [-LC_MESSAGES] [-11] [-12] [-icu]"+ 
            " localeName localeDataFile [charMapFile ...]");
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
        ndx = country.indexOf('@');
        if(ndx>0){
            variant = country.substring(ndx+1);
            country = country.substring(0,ndx);
        }
        return new Locale(language, country, variant);
    }
    //{{DECLARE_CONTROLS
        //}}
}
