/*
 *******************************************************************************
 * Copyright (C) 2002-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
 
package com.ibm.icu.dev.tool.localeconverter;

/*

ConvertPOSIXLocale [-11] [-12] [-icu] [-icu2] localeDataFile [charMapFile ...]

*/

public class ConvertLocaleTest {
    public static void main(String args[]) {
/*
        ConvertPOSIXLocale.main( new String[] {
            "-12",
            "C:\\projects\\com\\taligent\\localeconverter\\collationTest.txt"
        } );    
 
        ConvertPOSIXLocale.main( new String[] {
            "-icu",
            "C:\\projects\\com\\taligent\\localeconverter\\collationTest2.txt",
            "C:\\projects\\com\\taligent\\localeconverter\\CharMaps\\ISO-8859-1",
            "C:\\projects\\com\\taligent\\localeconverter\\CharMaps\\mnemonic.ds",
        } );

        ConvertPOSIXLocale.main( new String[] {
            "-icu", "-LC_MESSAGES",
            "C:\\projects\\com\\taligent\\localeconverter\\Locales\\Vivnx43.ibm",
            "C:\\projects\\com\\taligent\\localeconverter\\CharMaps\\ibm1129-2-UniCode.chm",
        } );
/*      ConvertPOSIXLocale.main( new String[] {
            "-12",
            "C:\\projects\\com\\taligent\\localeconverter\\Locales\\POSIXLocales\\en_US",
            "C:\\projects\\com\\taligent\\localeconverter\\CharMaps\\ISO-8859-1",
            "C:\\projects\\com\\taligent\\localeconverter\\CharMaps\\mnemonic.ds",
        });
/*      ConvertPOSIXLocale.main( new String[] {
            "-12",
            "C:\\projects\\com\\taligent\\localeconverter\\Locales\\POSIXLocales\\fi_FI",
            "C:\\projects\\com\\taligent\\localeconverter\\CharMaps\\ISO-8859-1",
            "C:\\projects\\com\\taligent\\localeconverter\\CharMaps\\mnemonic.ds",
        });

        ConvertPOSIXLocale.main( new String[] {
            "-12", "en_BE", 
            "C:\\projects\\com\\taligent\\localeconverter\\Locales\\ENBEWIN.IBM",
            "C:\\projects\\com\\taligent\\localeconverter\\CharMaps\\Ibm1252.chm",
        });

        ConvertPOSIXLocale.main( new String[] {
            "-12", "vi_VN",
            "C:\\projects\\com\\taligent\\localeconverter\\Locales\\Vivnx43.ibm",
            "C:\\projects\\com\\taligent\\localeconverter\\CharMaps\\ibm1129-2-UniCode.chm",
        });

        ConvertPOSIXLocale.main( new String[] {
            "-icu", "fr_FR", 
            "C:\\projects\\com\\taligent\\localeconverter\\Locales\\POSIXLocales\\fr_FR",
            "C:\\projects\\com\\taligent\\localeconverter\\CharMaps\\ISO-8859-1",
            "C:\\projects\\com\\taligent\\localeconverter\\CharMaps\\mnemonic.ds",
        });

        ConvertPOSIXLocale.main( new String[] {
            "-icu", "fo_FO", 
            "C:\\projects\\com\\taligent\\localeconverter\\Locales\\POSIXLocales\\fo_FO",
            "C:\\projects\\com\\taligent\\localeconverter\\CharMaps\\ISO-8859-1",
            "C:\\projects\\com\\taligent\\localeconverter\\CharMaps\\mnemonic.ds",
        });
*/
        ConvertPOSIXLocale.main( new String[] {
            "-icu", "fr_LU", 
            "C:\\projects\\com\\taligent\\localeconverter\\Locales\\POSIXLocales\\fr_LU",
            "C:\\projects\\com\\taligent\\localeconverter\\CharMaps\\ISO-8859-1",
            "C:\\projects\\com\\taligent\\localeconverter\\CharMaps\\mnemonic.ds",
        });
/*
        ConvertPOSIXLocale.main( new String[] {
            "-icu", "de_LU", 
            "C:\\projects\\com\\taligent\\localeconverter\\Locales\\POSIXLocales\\de_LU",
            "C:\\projects\\com\\taligent\\localeconverter\\CharMaps\\ISO-8859-1",
            "C:\\projects\\com\\taligent\\localeconverter\\CharMaps\\mnemonic.ds",
        });
*/
    }
}