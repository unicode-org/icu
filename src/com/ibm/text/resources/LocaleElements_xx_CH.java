/*
 * Copyright (C) 1996-2000, International Business Machines Corporation and
 * others. All Rights Reserved.
 */
package com.ibm.text.resources;

import java.util.ListResourceBundle;

public class LocaleElements_xx_CH extends ListResourceBundle {
    /**
     * Overrides ListResourceBundle
     */
    public Object[][] getContents() {
        return new Object[][] {
            // The purpose of this overlay is to implement nickel
            // rounding for Switzerland
            { "NumberPatterns", 
                new String[] { 
                    "#,##0.###;-#,##0.###",
                    "\u00A4 #,##0.05;\u00A4-#,##0.05",
                    "#,##0%",
                }
            },
        };
    }
}
