/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/impl/data/Attic/NumberFormatRules.java,v $ 
 * $Date: 2002/02/16 03:05:52 $ 
 * $Revision: 1.4 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.impl.data;

import java.util.ListResourceBundle;

/**
 * Base resource for RuleBasedNumberFormat data.  Each resource contains
 * rule sets for three uses: spelled-out numerical values, ordinal
 * abbreviations, and durations in seconds.  This resource contains the
 * U.S. English data.
 *
 * @author Richard Gillam
 * @version $Version$ $Date: 2002/02/16 03:05:52 $
 */
public class NumberFormatRules extends ListResourceBundle {
    /**
     * Puts a copyright in the .class file
     */
    private static final String copyrightNotice
        = "Copyright \u00a91997-1998 IBM Corp.  All rights reserved.";

    public Object[][] getContents() {
        return contents;
    }

    Object[][] contents = {
        /*
         * Default used to be English (US) rules, but now default just formats
         * like DecimalFormat.  The former default rules are now the _en rules.
         */
        { "SpelloutRules",
          "=#,##0.######=;\n"
        },
        { "OrdinalRules",
          "=#,##0=;\n"
        },
        { "DurationRules",
          "=#,##0=;\n"
        }
    };
}
