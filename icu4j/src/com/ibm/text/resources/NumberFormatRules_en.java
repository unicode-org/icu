/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/resources/Attic/NumberFormatRules_en.java,v $ 
 * $Date: 2000/03/10 04:07:27 $ 
 * $Revision: 1.2 $
 *
 *****************************************************************************************
 */

package com.ibm.text.resources;

import java.util.ListResourceBundle;

/**
 * Default RuleBasedNumberFormat data for English.  This resource
 * inherits everything from the default.
 *
 * @author Richard Gillam
 * @version $Version$ $Date: 2000/03/10 04:07:27 $
 */
public class NumberFormatRules_en extends ListResourceBundle {
    /**
     * Puts a copyright in the .class file
     */
    private static final String copyrightNotice
        = "Copyright \u00a91997-1998 IBM Corp.  All rights reserved.";

    public Object[][] getContents() {
        return contents;
    }

    // this is exactly the same as SpelloutRules from the root resource
    // bundle
    Object[][] contents = {
        { "SpelloutRules",
            "%simplified:\n"
            + "    -x: minus >>;\n"
            + "    x.x: << point >>;\n"
            + "    zero; one; two; three; four; five; six; seven; eight; nine;\n"
            + "    ten; eleven; twelve; thirteen; fourteen; fifteen; sixteen;\n"
            + "        seventeen; eighteen; nineteen;\n"
            + "    20: twenty[->>];\n"
            + "    30: thirty[->>];\n"
            + "    40: forty[->>];\n"
            + "    50: fifty[->>];\n"
            + "    60: sixty[->>];\n"
            + "    70: seventy[->>];\n"
            + "    80: eighty[->>];\n"
            + "    90: ninety[->>];\n"
            + "    100: << hundred[ >>];\n"
            + "    1000: << thousand[ >>];\n"
            + "    1,000,000: << million[ >>];\n"
            + "    1,000,000,000: << billion[ >>];\n"
            + "    1,000,000,000,000: << trillion[ >>];\n"
            + "    1,000,000,000,000,000: =#,##0=;\n"
            + "%default:\n"
            + "    -x: minus >>;\n"
            + "    x.x: << point >>;\n"
            + "    =%simplified=;\n"
            + "    100: << hundred[ >%%and>];\n"
            + "    1000: << thousand[ >%%and>];\n"
            + "    100,000>>: << thousand[>%%commas>];\n"
            + "    1,000,000: << million[>%%commas>];\n"
            + "    1,000,000,000: << billion[>%%commas>];\n"
            + "    1,000,000,000,000: << trillion[>%%commas>];\n"
            + "    1,000,000,000,000,000: =#,##0=;\n"
            + "%%and:\n"
            + "    and =%default=;\n"
            + "    100: =%default=;\n"
            + "%%commas:\n"
            + "    ' and =%default=;\n"
            + "    100: , =%default=;\n"
            + "    1000: , <%default< thousand, >%default>;\n"
            + "    1,000,000: , =%default=;"
            + "%%lenient-parse:\n"
            + "    & ' ' , ',' ;\n" }
    };
}
