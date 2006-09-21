 /*
  *******************************************************************************
  * Copyright (C) 2006, International Business Machines Corporation and    *
  * others. All Rights Reserved.                                                *
  *******************************************************************************
  */
package com.ibm.icu.dev.data.resources;

import java.util.ListResourceBundle;

public class TestDataElements_te extends ListResourceBundle {    
    private static Object[][] data = new Object[][] { 
    	{
    		"from_te",
    		"In root should be overridden"
    	}
    	
    };
	protected Object[][] getContents() {
		return data;
	}
}