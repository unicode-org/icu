/*
 * (C) IBM Corp. 1997-1998.  All Rights Reserved.
 *
 * The program is provided "as is" without any warranty express or
 * implied, including the warranty of non-infringement and the implied
 * warranties of merchantibility and fitness for a particular purpose.
 * IBM will not be liable for any damages suffered by you as a result
 * of using the Program. In no event will IBM be liable for any
 * special, indirect or consequential damages or lost profits even if
 * IBM has been advised of the possibility of their occurrence. IBM
 * will not be liable for any third party claims against you.
 */
package com.ibm.text.resources;

import java.util.ListResourceBundle;

/**
 * Default RuleBasedNumberFormat data for English.  This resource
 * inherits everything from the default.
 *
 * @author Richard Gillam
 * @version $Version$ $Date: 2000/02/10 06:25:52 $
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
