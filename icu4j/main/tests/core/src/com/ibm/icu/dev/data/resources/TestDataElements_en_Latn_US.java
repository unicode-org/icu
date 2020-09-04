// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
 /*
  *******************************************************************************
  * Copyright (C) 2005-2008, International Business Machines Corporation and    *
  * others. All Rights Reserved.                                                *
  *******************************************************************************
  */
package com.ibm.icu.dev.data.resources;

import java.util.ListResourceBundle;

public class TestDataElements_en_Latn_US extends ListResourceBundle {    
    private static Object[][] data = new Object[][] { 
        {
            "from_en_Latn_US",
            "This data comes from en_Latn_US"
        }
    };
    protected Object[][] getContents() {
        return data;
    }
}
