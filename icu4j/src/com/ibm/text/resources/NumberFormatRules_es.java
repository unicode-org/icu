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
 * RuleBasedNumberFormat data for Spanish.
 *
 * @author Richard Gillam
 * @version $Version$ $Date: 2000/02/10 06:25:52 $
 */
public class NumberFormatRules_es extends ListResourceBundle {
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
         * Spellout rules for Spanish.  The Spanish rules are quite similar to
         * the English rules, but there are some important differences:
         * First, we have to provide separate rules for most of the twenties
         * because the ones digit frequently picks up an accent mark that it
         * doesn't have when standing alone.  Second, each multiple of 100 has
         * to be specified separately because the multiplier on 100 very often
         * changes form in the contraction: 500 is "quinientos," not
         * "cincocientos."  In addition, the word for 100 is "cien" when
         * standing alone, but changes to "ciento" when followed by more digits.
         * There also some other differences.
         */
        { "SpelloutRules",
            // negative-number and fraction rules
            "-x: menos >>;\n"
            + "x.x: << punto >>;\n"
            // words for values from 0 to 19
            + "cero; uno; dos; tres; cuatro; cinco; seis; siete; ocho; nueve;\n"
            + "diez; once; doce; trece; catorce; quince; diecis\u00e9is;\n"
            + "    diecisiete; dieciocho; diecinueve;\n"
            // words for values from 20 to 29 (necessary because the ones digit
            // often picks up an accent mark it doesn't have when standing alone)
            + "veinte; veintiuno; veintid\u00f3s; veintitr\u00e9s; veinticuatro;\n"
            + "    veinticinco; veintis\u00e9is; veintisiete; veintiocho;\n"
            + "    veintinueve;\n"
            // words for multiples of 10 (notice that the tens digit is separated
            // from the ones digit by the word "y".)
            + "30: treinta[ y >>];\n"
            + "40: cuarenta[ y >>];\n"
            + "50: cincuenta[ y >>];\n"
            + "60: sesenta[ y >>];\n"
            + "70: setenta[ y >>];\n"
            + "80: ochenta[ y >>];\n"
            + "90: noventa[ y >>];\n"
            // 100 by itself is "cien," but 100 followed by something is "cineto"
            + "100: cien;\n"
            + "101: ciento >>;\n"
            // words for multiples of 100 (must be stated because they're
            // rarely simple concatenations)
            + "200: doscientos[ >>];\n"
            + "300: trescientos[ >>];\n"
            + "400: cuatrocientos[ >>];\n"
            + "500: quinientos[ >>];\n"
            + "600: seiscientos[ >>];\n"
            + "700: setecientos[ >>];\n"
            + "800: ochocientos[ >>];\n"
            + "900: novecientos[ >>];\n"
            // for 1,000, the multiplier on "mil" is omitted: 2,000 is "dos mil,"
            // but 1,000 is just "mil."
            + "1000: mil[ >>];\n"
            + "2000: << mil[ >>];\n"
            // 1,000,000 is "un millon," not "uno millon"
            + "1,000,000: un mill\u00f3n[ >>];\n"
            + "2,000,000: << mill\u00f3n[ >>];\n"
            // overflow rule
            + "1,000,000,000: =#,##0= (incomplete data);" }
        // The Spanish rules are incomplete.  I'm missing information on negative
        // numbers and numbers with fractional parts.  I also don't have
        // information on numbers higher than the millions.
    };
}
