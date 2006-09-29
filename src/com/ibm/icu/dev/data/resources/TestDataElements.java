 /*
  *******************************************************************************
  * Copyright (C) 2005, International Business Machines Corporation and    *
  * others. All Rights Reserved.                                                *
  *******************************************************************************
  */
package com.ibm.icu.dev.data.resources;

import java.util.ListResourceBundle;

public class TestDataElements extends ListResourceBundle {    
    private static Object[][] data = new Object[][] { 
        {    
            "from_root",
    		"This data comes from root"
    	},
        {
            "from_en",
            "In root should be overridden"
        },
    	{ 
    		"from_en_Latn",
    		"In root should be overridden"
    	},
    	{
    		"from_en_Latn_US",
    		"In root should be overridden"
    	}
    	
    };
	protected Object[][] getContents() {
		return data;
	}
}
