/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/impl/data/Attic/LocaleElements.java,v $ 
 * $Date: 2002/02/16 03:05:50 $ 
 * $Revision: 1.6 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.impl.data;

import java.util.ListResourceBundle;

public class LocaleElements extends ListResourceBundle {
    /**
     * Overrides ListResourceBundle
     */
    public Object[][] getContents() {
        return new Object[][] {
            { "TransliteratorNamePattern",
                /* Format for the display name of a Transliterator.
                 * This is the language-neutral form of this resource.
                 */
                "{0,choice,0#|1#{1}|2#{1}-{2}}", // Display name
            },
        };
    }
}
