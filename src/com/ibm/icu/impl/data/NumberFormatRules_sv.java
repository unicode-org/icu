/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/impl/data/Attic/NumberFormatRules_sv.java,v $ 
 * $Date: 2002/02/16 03:05:54 $ 
 * $Revision: 1.4 $
 *
 *****************************************************************************************
 */

package com.ibm.icu.impl.data;

import java.util.ListResourceBundle;

/**
 * RuleBasedNumberFormat data for Swedish
 *
 * @author Richard Gillam
 * @version $Version$ $Date: 2002/02/16 03:05:54 $
 */
public class NumberFormatRules_sv extends ListResourceBundle {
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
         * Thanks to Kent Karlsson for cleaning up these rules.
         */
        { "SpelloutRules",
          "%year:\n"
            + "=%neutrum=;\n"
            + "1000>: <%neutrum<hundra[\u00ad>>];\n"
            + "10,000: =%neutrum=;\n"
          // the same as default except we change the one's digit, wish there were a better way
          + "%neutrum:\n"
            + "-x: minus >>;\n"
            + "x.x: << komma >>;\n"
            + "noll; ett; tv\u00e5; tre; fyra; fem; sex; sju; \u00e5tta; nio;\n"
            + "tio; elva; tolv; tretton; fjorton; femton; sexton; sjutton; arton; nitton;\n"
            + "20: tjugo[>>];\n"
            + "30: trettio[>>];\n"
            + "40: fyrtio[>>];\n"
            + "50: femtio[>>];\n"
            + "60: sextio[>>];\n"
            + "70: sjuttio[>>];\n"
            + "80: \u00e5ttio[>>];\n"
            + "90: nittio[>>];\n"
            + "100: <<hundra[\u00ad>>];\n"
            + "1000: ettusen[ >>];\n"
            + "2000: <%default<\u00adtusen[ >>];\n"
            + "1,000,000: en miljon[ >>];\n"
            + "2,000,000: <%default< miljoner[ >>];\n"
            + "1,000,000,000: en miljard[ >>];\n"
            + "2,000,000,000: <%default< miljarder[ >>];\n"
            + "1,000,000,000,000: en biljon[ >>];\n"
            + "2,000,000,000,000: <%default< biljoner[ >>];\n"
            + "1,000,000,000,000,000: en triljon[ >>];\n"
            + "2,000,000,000,000,000: <%default< triljoner[ >>];\n"
            + "1,000,000,000,000,000,000: =#,##0=;\n"
          + "%default:\n"
            + "-x: minus >>;\n"
            + "x.x: << komma >>;\n"
            + "noll; en; tv\u00e5; tre; fyra; fem; sex; sju; \u00e5tta; nio;\n"
            + "tio; elva; tolv; tretton; fjorton; femton; sexton; sjutton; arton; nitton;\n"
            + "20: tjugo[>>];\n"
            + "30: trettio[>>];\n"
            + "40: fyrtio[>>];\n"
            + "50: femtio[>>];\n"
            + "60: sextio[>>];\n"
            + "70: sjuttio[>>];\n"
            + "80: \u00e5ttio[>>];\n"
            + "90: nittio[>>];\n"
            + "100: etthundra[\u00ad>>];\n"
            + "200: <<hundra[\u00ad>>];\n"
            + "1000: ettusen[ >>];\n"
            + "2000: <<\u00adtusen[ >>];\n"
            + "1,000,000: en miljon[ >>];\n"
            + "2,000,000: << miljoner[ >>];\n"
            + "1,000,000,000: en miljard[ >>];\n"
            + "2,000,000,000: << miljarder[ >>];\n"
            + "1,000,000,000,000: en biljon[ >>];\n"
            + "2,000,000,000,000: << biljoner[ >>];\n"
            + "1,000,000,000,000,000: en triljon[ >>];\n"
            + "2,000,000,000,000,000: << triljoner[ >>];\n"
            + "1,000,000,000,000,000,000: =#,##0=;\n"
/* 
 * Current implementation can't handle these magnitudes)
            + "1,000,000,000,000,000,000: en triljard[ >>];\n"
            + "2,000,000,000,000,000,000: << triljarder[ >>];\n"
            + "1,000,000,000,000,000,000,000: en kvartiljon[ >>];\n"
            + "2,000,000,000,000,000,000,000: << kvartiljoner[ >>];\n"
            + "1,000,000,000,000,000,000,000,000: en kvartiljard[ >>];\n"
            + "2,000,000,000,000,000,000,000,000: << kvartiljarder[ >>];\n"
            + "1,000,000,000,000,000,000,000,000,000: en kvintiljon[ >>];\n"
            + "2,000,000,000,000,000,000,000,000,000: << kvintiljoner[ >>];\n"
            + "1,000,000,000,000,000,000,000,000,000,000: en kvintiljard[ >>];\n"
            + "2,000,000,000,000,000,000,000,000,000,000: << kvintiljarder[ >>];\n"
            + "1,000,000,000,000,000,000,000,000,000,000,000: en sextiljon[ >>];\n"
            + "2,000,000,000,000,000,000,000,000,000,000,000: << sextiljoner[ >>];\n"
            + "1,000,000,000,000,000,000,000,000,000,000,000,000: en sextiljard[ >>];\n"
            + "2,000,000,000,000,000,000,000,000,000,000,000,000: << sextiljarder[ >>];\n"
            + "1,000,000,000,000,000,000,000,000,000,000,000,000,000: =#,##0="
*/
        }
    };
}
