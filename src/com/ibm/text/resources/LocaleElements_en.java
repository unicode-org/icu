/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/resources/Attic/LocaleElements_en.java,v $ 
 * $Date: 2000/03/10 04:07:27 $ 
 * $Revision: 1.2 $
 *
 *****************************************************************************************
 */

// WARNING : the format of this file may change in the future!

package com.ibm.text.resources;

import java.util.ListResourceBundle;

public class LocaleElements_en extends ListResourceBundle {
    /**
     * Overrides ListResourceBundle
     */
    public Object[][] getContents() {
        return new Object[][] {
            { "TransliteratorNamePattern",
                /* Format for the display name of a Transliterator.
                 * This is the English form of this resource.
                 */
                "{0,choice,0#|1#{1}|2#{1} to {2}}"
            },

            // Transliterator display names
            { "%Translit%Hex", "Hex Escape" },
            { "%Translit%UnicodeName", "Unicode Name" },
            { "%Translit%UnicodeChar", "Unicode Character" },
        };
    }
}
