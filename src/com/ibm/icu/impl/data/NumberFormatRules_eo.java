/*
 *******************************************************************************
 * Copyright (C) 2001, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.impl.data;

import java.util.ListResourceBundle;

/**
 * RuleBasedNumberFormat data for Esperanto
 *
 * @author Doug Felt
 * @version %W% %E%
 */
public class NumberFormatRules_eo extends ListResourceBundle {
    /**
     * Puts a copyright in the .class file
     */
    private static final String copyrightNotice
        = "Copyright \u00a92001 IBM Corp.  All rights reserved.";

    public Object[][] getContents() {
        return contents;
    }

    Object[][] contents = {
        /**
         * Spellout rules for Esperanto.
         * data from 'Esperanto-programita 1' courtesy of Markus Scherer
         */
        { "SpelloutRules",
          "-x: minus >>;\n" +
          "x.x: << komo >>;\n" +
          "nulo; unu; du; tri; kvar; kvin; ses; sep; ok; na\u016d;\n" +
          "10: dek[ >>];\n" +
          "20: <<dek[ >>];\n" +
          "100: cent[ >>];\n" +
          "200: <<cent[ >>];\n" +
          "1000: mil[ >>];\n" +
          "2000: <<mil[ >>];\n" +
          "10000: dekmil[ >>];\n" +
          "11000>: << mil[ >>];\n" +
          "1,000,000: miliono[ >>];\n" +
          "2,000,000: << milionoj[ >>];\n" +
          "1,000,000,000: miliardo[ >>];\n" +
          "2,000,000,000: << miliardoj[ >>];\n" +
          "1,000,000,000,000: biliono[ >>];\n" +
          "2,000,000,000,000: << bilionoj[ >>];\n" +
          "1,000,000,000,000,000: =#,##0=;\n"
        }
    };
}

