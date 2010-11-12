 /*
  *******************************************************************************
  * Copyright (C) 2005, International Business Machines Corporation and    *
  * others. All Rights Reserved.                                                *
  *******************************************************************************
  */
package com.ibm.icu.dev.data.resources;

import java.util.ListResourceBundle;

public class TestDataElements_en_US extends ListResourceBundle {

    private static Object[][] data = new Object[][] { 
        {
            "from_en_US",
            "This data comes from en_US"
        }
        
    };
    protected Object[][] getContents() {
        return data;
    }
}
