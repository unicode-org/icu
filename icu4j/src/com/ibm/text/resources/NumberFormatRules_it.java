/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/resources/Attic/NumberFormatRules_it.java,v $ 
 * $Date: 2000/03/10 04:07:28 $ 
 * $Revision: 1.2 $
 *
 *****************************************************************************************
 */

package com.ibm.text.resources;

import java.util.ListResourceBundle;

/**
 * RuleBasedNumberFormat data for Italian
 *
 * @author Richard Gillam
 * @version $Version$ $Date: 2000/03/10 04:07:28 $
 */
public class NumberFormatRules_it extends ListResourceBundle {
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
         * Spellout rules for Italian.  Like German, most Italian numbers are
         * written as single words.  What makes these rules complicated is the rule
         * that says that when a word ending in a vowel and a word beginning with
         * a vowel are combined into a compound, the vowel is dropped from the
         * end of the first word: 180 is "centottanta," not "centoottanta."
         * The complexity of this rule set is to produce this behavior.
         */
        { "SpelloutRules",
            // main rule set.  Follows the patterns of the preceding rule sets,
            // except that the final vowel is omitted from words ending in
            // vowels when they are followed by another word; instead, we have
            // separate rule sets that are identical to this one, except that
            // all the words that don't begin with a vowel have a vowel tacked
            // onto them at the front.  A word ending in a vowel calls a
            // substitution that will supply that vowel, unless that vowel is to
            // be elided.
            "%main:\n"
            + "    -x: meno >>;\n"
            + "    x.x: << virgola >>;\n"
            + "    zero; uno; due; tre; quattro; cinque; sei; sette; otto;\n"
            + "        nove;\n"
            + "    dieci; undici; dodici; tredici; quattordici; quindici; sedici;\n"
            + "        diciasette; diciotto; diciannove;\n"
            + "    20: venti; vent>%%with-i>;\n"
            + "    30: trenta; trent>%%with-i>;\n"
            + "    40: quaranta; quarant>%%with-a>;\n"
            + "    50: cinquanta; cinquant>%%with-a>;\n"
            + "    60: sessanta; sessant>%%with-a>;\n"
            + "    70: settanta; settant>%%with-a>;\n"
            + "    80: ottanta; ottant>%%with-a>;\n"
            + "    90: novanta; novant>%%with-a>;\n"
            + "    100: cento; cent[>%%with-o>];\n"
            + "    200: <<cento; <<cent[>%%with-o>];\n"
            + "    1000: mille; mill[>%%with-i>];\n"
            + "    2000: <<mila; <<mil[>%%with-a>];\n"
            + "    100,000>>: <<mila[ >>];\n"
            + "    1,000,000: =#,##0= (incomplete data);\n"
            + "%%with-a:\n"
            + "    azero; uno; adue; atre; aquattro; acinque; asei; asette; otto;\n"
            + "        anove;\n"
            + "    adieci; undici; adodici; atredici; aquattordici; aquindici; asedici;\n"
            + "        adiciasette; adiciotto; adiciannove;\n"
            + "    20: aventi; avent>%%with-i>;\n"
            + "    30: atrenta; atrent>%%with-i>;\n"
            + "    40: aquaranta; aquarant>%%with-a>;\n"
            + "    50: acinquanta; acinquant>%%with-a>;\n"
            + "    60: asessanta; asessant>%%with-a>;\n"
            + "    70: asettanta; asettant>%%with-a>;\n"
            + "    80: ottanta; ottant>%%with-a>;\n"
            + "    90: anovanta; anovant>%%with-a>;\n"
            + "    100: acento; acent[>%%with-o>];\n"
            + "    200: <%%with-a<cento; <%%with-a<cent[>%%with-o>];\n"
            + "    1000: amille; amill[>%%with-i>];\n"
            + "    2000: <%%with-a<mila; <%%with-a<mil[>%%with-a>];\n"
            + "    100,000: =%main=;\n"
            + "%%with-i:\n"
            + "    izero; uno; idue; itre; iquattro; icinque; isei; isette; otto;\n"
            + "        inove;\n"
            + "    idieci; undici; idodici; itredici; iquattordici; iquindici; isedici;\n"
            + "        idiciasette; idiciotto; idiciannove;\n"
            + "    20: iventi; ivent>%%with-i>;\n"
            + "    30: itrenta; itrent>%%with-i>;\n"
            + "    40: iquaranta; iquarant>%%with-a>;\n"
            + "    50: icinquanta; icinquant>%%with-a>;\n"
            + "    60: isessanta; isessant>%%with-a>;\n"
            + "    70: isettanta; isettant>%%with-a>;\n"
            + "    80: ottanta; ottant>%%with-a>;\n"
            + "    90: inovanta; inovant>%%with-a>;\n"
            + "    100: icento; icent[>%%with-o>];\n"
            + "    200: <%%with-i<cento; <%%with-i<cent[>%%with-o>];\n"
            + "    1000: imille; imill[>%%with-i>];\n"
            + "    2000: <%%with-i<mila; <%%with-i<mil[>%%with-a>];\n"
            + "    100,000: =%main=;\n"
            + "%%with-o:\n"
            + "    ozero; uno; odue; otre; oquattro; ocinque; osei; osette; otto;\n"
            + "        onove;\n"
            + "    odieci; undici; ododici; otredici; oquattordici; oquindici; osedici;\n"
            + "        odiciasette; odiciotto; odiciannove;\n"
            + "    20: oventi; ovent>%%with-i>;\n"
            + "    30: otrenta; otrent>%%with-i>;\n"
            + "    40: oquaranta; oquarant>%%with-a>;\n"
            + "    50: ocinquanta; ocinquant>%%with-a>;\n"
            + "    60: osessanta; osessant>%%with-a>;\n"
            + "    70: osettanta; osettant>%%with-a>;\n"
            + "    80: ottanta; ottant>%%with-a>;\n"
            + "    90: onovanta; onovant>%%with-a>;\n"
            + "    100: ocento; ocent[>%%with-o>];\n"
            + "    200: <%%with-o<cento; <%%with-o<cent[>%%with-o>];\n"
            + "    1000: omille; omill[>%%with-i>];\n"
            + "    2000: <%%with-o<mila; <%%with-o<mil[>%%with-a>];\n"
            + "    100,000: =%main=;\n" }
        // Can someone confirm that I did the vowel-eliding thing right?  I'm
        // not 100% sure I'm doing it in all the right places, or completely
        // correctly.  Also, I don't have information for negatives and decimals,
        // and I lack words fror values from 1,000,000 on up.
    };
}
