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
 * RuleBasedNumberFormat data for Dutch
 *
 * @author Richard Gillam
 * @version $Version$ $Date: 2000/02/10 06:25:53 $
 */
class NumberFormatRules_nl extends ListResourceBundle {
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
         * Spellout rules for Swedish.
         */
        { "SpelloutRules",
            " -x: min >>;\n"
            + "x.x: << komma >>;\n"
            + "(zero?); een; twee; drie; vier; vijf; zes; zeven; acht; negen;\n"
            + "tien; elf; twaalf; dertien; veertien; vijftien; zestien;\n"
            + "zeventien; achtien; negentien;\n"
            + "20: [>> en ]twintig;\n"
            + "30: [>> en ]dertig;\n"
            + "40: [>> en ]veertig;\n"
            + "50: [>> en ]vijftig;\n"
            + "60: [>> en ]zestig;\n"
            + "70: [>> en ]zeventig;\n"
            + "80: [>> en ]tachtig;\n"
            + "90: [>> en ]negentig;\n"
            + "100: << honderd[ >>];\n"
            + "1000: << duizend[ >>];\n"
            + "1,000,000: << miljoen[ >>];\n"
            + "1,000,000,000: << biljoen[ >>];\n"
            + "1,000,000,000,000: =#,##0=" }
            // can someone supply me with information on negatives and decimals?
    };
}
