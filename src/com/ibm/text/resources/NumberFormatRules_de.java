/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/resources/Attic/NumberFormatRules_de.java,v $ 
 * $Date: 2000/03/10 04:07:27 $ 
 * $Revision: 1.3 $
 *
 *****************************************************************************************
 */
package com.ibm.text.resources;

import java.util.ListResourceBundle;

/**
 * RuleBasedNumberFormat data for German
 *
 * @author Richard Gillam
 * @version $Version$ $Date: 2000/03/10 04:07:27 $
 */
public class NumberFormatRules_de extends ListResourceBundle {
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
         * Spellout rules for German.  German also adds some interesting
         * characteristics.  For values below 1,000,000, numbers are customarily
         * written out as a single word.  And the ones digit PRECEDES the tens
         * digit (e.g., 23 is "dreiundzwanzig," not "zwanzigunddrei").
         */
        { "SpelloutRules",
            // 1 is "eins" when by itself, but turns into "ein" in most
            // combinations
            "%alt-ones:\n"
            + "    null; eins; =%%main=;\n"
            + "%%main:\n"
                   // words for numbers from 0 to 12.  Notice that the values
                   // from 13 to 19 can derived algorithmically, unlike in most
                   // other languages
            + "    null; ein; zwei; drei; vier; f\u00fcnf; sechs; sieben; acht; neun;\n"
            + "    zehn; elf; zw\u00f6lf; >>zehn;\n"
                   // rules for the multiples of 10.  Notice that the ones digit
                   // goes on the front
            + "    20: [>>und]zwanzig;\n"
            + "    30: [>>und]drei\u00dfig;\n"
            + "    40: [>>und]vierzig;\n"
            + "    50: [>>und]f\u00fcnfzig;\n"
            + "    60: [>>und]sechzig;\n"
            + "    70: [>>und]siebzig;\n"
            + "    80: [>>und]achtzig;\n"
            + "    90: [>>und]neunzig;\n"
            + "    100: hundert[>%alt-ones>];\n"
            + "    200: <<hundert[>%alt-ones>];\n"
            + "    1000: tausend[>%alt-ones>];\n"
            + "    2000: <<tausend[>%alt-ones>];\n"
            + "    1,000,000: eine Million[ >%alt-ones>];\n"
            + "    2,000,000: << Millionen[ >%alt-ones>];\n"
            + "    1,000,000,000: eine Milliarde[ >%alt-ones>];\n"
            + "    2,000,000,000: << Milliarden[ >%alt-ones>];\n"
            + "    1,000,000,000,000: eine Billion[ >%alt-ones>];\n"
            + "    2,000,000,000,000: << Billionen[ >%alt-ones>];\n"
            + "    1,000,000,000,000,000: =#,##0=;"
            + "%%lenient-parse:\n"
            + "    & ae , \u00e4 & ae , \u00c4\n"
            + "    & oe , \u00f6 & oe , \u00d6\n"
            + "    & ue , \u00fc & ue , \u00dc\n"
        }
            
        // again, I'm not 100% sure of these rules.  I think both "hundert" and
        // "einhundert" are correct or 100, but I'm not sure which is preferable
        // in situations where this framework is likely to be used.  Also, is it
        // really true that numbers are run together into compound words all the
        // time?  And again, I'm missing information on negative numbers and
        // decimals.
    };
}

