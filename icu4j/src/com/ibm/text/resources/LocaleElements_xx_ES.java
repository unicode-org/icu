/*
 * Copyright (C) 1996-2000, International Business Machines Corporation and
 * others. All Rights Reserved.
 */
package com.ibm.text.resources;

import java.util.ListResourceBundle;

public class LocaleElements_xx_ES extends ListResourceBundle {
    /**
     * Overrides ListResourceBundle
     */
    public Object[][] getContents() {
        return new Object[][] {
            // The purpose of this overlay is to make sure the number
            // of fractional digits for Pesetas is zero; it is
            // non-zero in some JDKs.
            { "NumberPatterns", 
                new String[] { 
                    "#,##0.###;-#,##0.###", // decimal pattern
                    "#,##0 \u00A4;-#,##0 \u00A4", // currency pattern
                    "#,##0%" // percent pattern
                }
            },
        };
    }
}
