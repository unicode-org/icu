/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/resources/Attic/NumberFormatRules_ja.java,v $ 
 * $Date: 2000/03/10 04:07:28 $ 
 * $Revision: 1.2 $
 *
 *****************************************************************************************
 */

package com.ibm.text.resources;

import java.util.ListResourceBundle;

/**
 * RuleBasedNumberFormat data for Japanese
 *
 * @author Richard Gillam
 * @version $Version$ $Date: 2000/03/10 04:07:28 $
 */
public class NumberFormatRules_ja extends ListResourceBundle {
    /**
     * Puts a copyright in the .class file
     */
    private static final String copyrightNotice
        = "Copyright \u00a91997-1998 IBM Corp.  All rights reserved.";

    public Object[][] getContents() {
        return contents;
    }

    Object[][] contents = {
        /**
         * Spellout rules for Japanese.  In Japanese, there really isn't any
         * distinction between a number written out in digits and a number
         * written out in words: the ideographic characters are both digits
         * and words.  This rule set provides two variants:  %traditional
         * uses the traditional CJK numerals (which are also used in China
         * and Korea).  %financial uses alternate ideographs for many numbers
         * that are harder to alter than the traditional numerals (one could
         * fairly easily change a one to
         * a three just by adding two strokes, for example).  This is also done in
         * the other countries using Chinese idographs, but different ideographs
         * are used in those places.
         */
        { "SpelloutRules",
            "%financial:\n"
            + "    \u96f6; \u58f1; \u5f10; \u53c2; \u56db; \u4f0d; \u516d; \u4e03; \u516b; \u4e5d;\n"
            + "    \u62fe[>>];\n"
            + "    20: <<\u62fe[>>];\n"
            + "    100: <<\u767e[>>];\n"
            + "    1000: <<\u5343[>>];\n"
            + "    10,000: <<\u4e07[>>];\n"
            + "    100,000,000: <<\u5104[>>];\n"
            + "    1,000,000,000,000: <<\u5146[>>];\n"
            + "    10,000,000,000,000,000: =#,##0=;\n"
            + "%traditional:\n"
            + "    \u96f6; \u4e00; \u4e8c; \u4e09; \u56db; \u4e94; \u516d; \u4e03; \u516b; \u4e5d;\n"
            + "    \u5341[>>];\n"
            + "    20: <<\u5341[>>];\n"
            + "    100: <<\u767e[>>];\n"
            + "    1000: <<\u5343[>>];\n"
            + "    10,000: <<\u4e07[>>];\n"
            + "    100,000,000: <<\u5104[>>];\n"
            + "    1,000,000,000,000: <<\u5146[>>];\n"
            + "    10,000,000,000,000,000: =#,##0=;" }
        // Can someone supply me with the right fraud-proof ideographs for
        // Simplified and Traditional Chinese, and for Korean?  Can someone
        // supply me with information on negatives and decimals?
    };
}
