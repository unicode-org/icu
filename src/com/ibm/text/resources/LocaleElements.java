/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/resources/Attic/LocaleElements.java,v $ 
 * $Date: 2001/03/01 22:46:33 $ 
 * $Revision: 1.5 $
 *
 *****************************************************************************************
 */
package com.ibm.text.resources;

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
