/*
 * Copyright (C) 1996-2000, International Business Machines Corporation and
 * others. All Rights Reserved.
 */
package com.ibm.text.resources;

import java.util.ListResourceBundle;

public class LocaleElements_xx_IN extends ListResourceBundle {
    /**
     * Overrides ListResourceBundle
     */
    public Object[][] getContents() {
        return new Object[][] {
            // The purpose of this overlay is to specify the secondary
            // grouping separator for Indian numbers.
            { "NumberPatterns", 
                new String[] { 
                    "#,##,##0.###",
                    "\u00A4 #,##,##0.00;-\u00A4 #,##,##0.00",
                    "#,##,##0%"
                }
            },
        };
    }
}
