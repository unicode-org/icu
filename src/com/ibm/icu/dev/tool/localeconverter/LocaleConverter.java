/*
 *******************************************************************************
 * Copyright (C) 2002-2002, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/tool/localeconverter/LocaleConverter.java,v $ 
 * $Date: 2002/12/18 03:56:41 $ 
 * $Revision: 1.3 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.dev.tool.localeconverter;

import java.io.*;
import java.util.*;

public class LocaleConverter {
    public Hashtable convert(Hashtable table) throws ConversionError {
        Hashtable result = new Hashtable();
        convert(result, table);
        return result;
    }
    
    protected void convert(Hashtable result, Hashtable source) throws ConversionError {
        Enumeration enum = source.keys();
        while (enum.hasMoreElements()) {
            String key = (String)enum.nextElement();
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
