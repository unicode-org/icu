/**
 *******************************************************************************
 * Copyright (C) 2001-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.data;

import com.ibm.icu.impl.ICUListResourceBundle;
/**
 * @author Ram
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class TestDataElements_testcircularalias  extends ICUListResourceBundle {
    public TestDataElements_testcircularalias() {
          super.contents = data;
    }
    private static Object[][] data = new Object[][] { 
                {
                    "aaa",
                    new ICUListResourceBundle.Alias("testcircularalias/aab"),
                },
                {
                    "aab",
                    new ICUListResourceBundle.Alias("testcircularalias/aac"),
                },
                {
                    "aac",
                    new ICUListResourceBundle.Alias("testcircularalias/aaa"),
                } 
    };
}

