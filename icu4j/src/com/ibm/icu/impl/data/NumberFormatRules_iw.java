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
 * RuleBasedNumberFormat data for Hebrew
 *
 * @author Richard Gillam
 * @version $Version$ $Date: 2000/02/10 06:25:53 $
 */
public class NumberFormatRules_iw extends ListResourceBundle {
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
         * Spellout rules for Hebrew.  Hebrew actually has inflected forms for
         * most of the lower-order numbers.  The masculine forms are shown
         * here.
         */
        { "SpelloutRules",
            "zero (incomplete data); \u05d0\u05d4\u05d3; \u05e9\u05d2\u05d9\u05d9\u05dd; \u05e9\u05dc\u05d5\u05e9\u05d4;\n"
            + "4: \u05d0\u05d3\u05d1\u05e6\u05d4; \u05d7\u05d2\u05d5\u05d9\u05e9\u05d4; \u05e9\u05e9\u05d4;\n"
            + "7: \u05e9\u05d1\u05e6\u05d4; \u05e9\u05de\u05d5\u05d2\u05d4; \u05ea\u05e9\u05e6\u05d4;\n"
            + "10: \u05e6\u05e9\u05d3\u05d4[ >>];\n"
            + "20: \u05e6\u05e9\u05d3\u05d9\u05dd[ >>];\n"
            + "30: \u05e9\u05dc\u05d5\u05e9\u05d9\u05dd[ >>];\n"
            + "40: \u05d0\u05d3\u05d1\u05e6\u05d9\u05dd[ >>];\n"
            + "50: \u05d7\u05de\u05d9\u05e9\u05d9\u05dd[ >>];\n"
            + "60: \u05e9\u05e9\u05d9\u05dd[ >>];\n"
            + "70: \u05e9\u05d1\u05e6\u05d9\u05dd[ >>];\n"
            + "80: \u05e9\u05de\u05d5\u05d2\u05d9\u05dd[ >>];\n"
            + "90: \u05ea\u05e9\u05e6\u05d9\u05dd[ >>];\n"
            + "100: \u05de\u05d0\u05d4[ >>];\n"
            + "200: << \u05de\u05d0\u05d4[ >>];\n"
            + "1000: \u05d0\u05dc\u05e3[ >>];\n"
            + "2000: << \u05d0\u05dc\u05e3[ >>];\n"
            + "1,000,000: =#,##0= (incomplete data);" }
        // This data is woefully incomplete.  Can someone fill me in on the
        // various inflected forms of the numbers, which seem to be necessary
        // to do Hebrew correctly?  Can somone supply me with data for values
        // from 1,000,000 on up?  What about the word for zero?  What about
        // information on negatives and decimals?
    };
}
