/*
 *******************************************************************************
 * Copyright (C) 2002-2005, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.localeconverter;

import java.util.*;

public class LocaleConverter {
    public Hashtable convert(Hashtable table) throws ConversionError {
        Hashtable result = new Hashtable();
        convert(result, table);
        return result;
    }
    
    protected void convert(Hashtable result, Hashtable source) throws ConversionError {
        Enumeration enumer = source.keys();
        while (enumer.hasMoreElements()) {
            String key = (String)enumer.nextElement();
            Object data = source.get(key);
            result.put(key, data);
        }
    }
    
    public static class ConversionError extends Exception {
        public ConversionError() {
        }
        
        public ConversionError(String reason) {
            super(reason);
        }
    }
}
