/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/resources/Attic/NumberFormatRules_el.java,v $ 
 * $Date: 2000/03/10 04:07:27 $ 
 * $Revision: 1.2 $
 *
 *****************************************************************************************
 */

package com.ibm.text.resources;

import java.util.ListResourceBundle;

/**
 * RuleBasedNumberFormat data for Greek
 *
 * @author Richard Gillam
 * @version $Version$ $Date: 2000/03/10 04:07:27 $
 */
public class NumberFormatRules_el extends ListResourceBundle {
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
         * Spellout rules for Greek.  Again in Greek we have to supply the words
         * for the multiples of 100 because they can't be derived algorithmically.
         * Also, the tens dgit changes form when followed by a ones digit: an
         * accent mark disappears from the tens digit and moves to the ones digit.
         * Therefore, instead of using the [] notation, we actually have to use
         * two separate rules for each multiple of 10 to show the two forms of
         * the word.
         */
        { "SpelloutRules",
            "zero (incomplete data); \u03ad\u03bd\u03b1; \u03b4\u03cd\u03bf; \u03b4\u03c1\u03af\u03b1; "
            + "\u03c4\u03ad\u03c3\u03c3\u03b5\u03c1\u03b1; \u03c0\u03ad\u03bd\u03c4\u03b5; "
            + "\u03ad\u03be\u03b9; \u03b5\u03c0\u03c4\u03ac; \u03bf\u03ba\u03c4\u03ce; "
            + "\u03b5\u03bd\u03bd\u03ad\u03b1;\n"
            + "10: \u03b4\u03ad\u03ba\u03b1; "
            + "\u03ad\u03bd\u03b4\u03b5\u03ba\u03b1; \u03b4\u03ce\u03b4\u03b5\u03ba\u03b1; "
            + "\u03b4\u03b5\u03ba\u03b1>>;\n"
            + "20: \u03b5\u03af\u03ba\u03bf\u03c3\u03b9; \u03b5\u03b9\u03ba\u03bf\u03c3\u03b9>>;\n"
            + "30: \u03c4\u03c1\u03b9\u03ac\u03bd\u03c4\u03b1; \u03c4\u03c1\u03b9\u03b1\u03bd\u03c4\u03b1>>;\n"
            + "40: \u03c3\u03b1\u03c1\u03ac\u03bd\u03c4\u03b1; \u03c3\u03b1\u03c1\u03b1\u03bd\u03c4\u03b1>>;\n"
            + "50: \u03c0\u03b5\u03bd\u03ae\u03bd\u03c4\u03b1; \u03c0\u03b5\u03bd\u03b7\u03bd\u03c4\u03b1>>;\n"
            + "60: \u03b5\u03be\u03ae\u03bd\u03c4\u03b1; \u03b5\u03be\u03b7\u03bd\u03c4\u03b1>>;\n"
            + "70: \u03b5\u03b2\u03b4\u03bf\u03bc\u03ae\u03bd\u03c4\u03b1; "
            + "\u03b5\u03b2\u03b4\u03bf\u03bc\u03b7\u03bd\u03c4\u03b1>>;\n"
            + "80: \u03bf\u03b3\u03b4\u03cc\u03bd\u03c4\u03b1; \u03bf\u03b3\u03b4\u03bf\u03bd\u03c4\u03b1>>;\n"
            + "90: \u03b5\u03bd\u03bd\u03b5\u03bd\u03ae\u03bd\u03c4\u03b1; "
            + "\u03b5\u03bd\u03bd\u03b5\u03bd\u03b7\u03bd\u03c4\u03b1>>;\n"
            + "100: \u03b5\u03ba\u03b1\u03c4\u03cc[\u03bd >>];\n"
            + "200: \u03b4\u03b9\u03b1\u03ba\u03cc\u03c3\u03b9\u03b1[ >>];\n"
            + "300: \u03c4\u03c1\u03b9\u03b1\u03ba\u03cc\u03c3\u03b9\u03b1[ >>];\n"
            + "400: \u03c4\u03b5\u03c4\u03c1\u03b1\u03ba\u03cc\u03c3\u03b9\u03b1[ >>];\n"
            + "500: \u03c0\u03b5\u03bd\u03c4\u03b1\u03ba\u03cc\u03c3\u03b9\u03b1[ >>];\n"
            + "600: \u03b5\u03be\u03b1\u03ba\u03cc\u03c3\u03b9\u03b1[ >>];\n"
            + "700: \u03b5\u03c0\u03c4\u03b1\u03ba\u03cc\u03c3\u03b9\u03b1[ >>];\n"
            + "800: \u03bf\u03ba\u03c4\u03b1\u03ba\u03cc\u03c3\u03b9\u03b1[ >>];\n"
            + "900: \u03b5\u03bd\u03bd\u03b9\u03b1\u03ba\u03cc\u03c3\u03b9\u03b1[ >>];\n"
            + "1000: \u03c7\u03af\u03bb\u03b9\u03b1[ >>];\n"
            + "2000: << \u03c7\u03af\u03bb\u03b9\u03b1[ >>];\n"
            + "1,000,000: << \u03b5\u03ba\u03b1\u03c4\u03bf\u03bc\u03bc\u03b9\u03cc\u03c1\u03b9\u03bf[ >>];\n"
            + "1,000,000,000: << \u03b4\u03b9\u03c3\u03b5\u03ba\u03b1\u03c4\u03bf\u03bc\u03bc\u03b9\u03cc\u03c1\u03b9\u03bf[ >>];\n"
            + "1,000,000,000,000: =#,##0=" }
        // Can someone supply me with information on negatives and decimals?
        // I'm also missing the word for zero.  Can someone clue me in?
    };
}
