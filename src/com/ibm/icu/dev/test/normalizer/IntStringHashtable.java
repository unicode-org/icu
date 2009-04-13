package com.ibm.icu.dev.test.normalizer;

import java.util.Hashtable;

/**
 *******************************************************************************
 * Copyright (C) 1998-2007, International Business Machines Corporation and    *
 * Unicode, Inc. All Rights Reserved.                                          *
 *******************************************************************************
 *
 * Integer-String hash table. Uses Java Hashtable for now.
 * @author Mark Davis
 */
 
public class IntStringHashtable {
//    static final String copyright = "Copyright (C) 1998-2003 International Business Machines Corporation and Unicode, Inc.";
    
    public IntStringHashtable (String defaultValue) {
        this.defaultValue = defaultValue;
    }
    
    public void put(int key, String value) {
        if (value == defaultValue) {
            table.remove(new Integer(key));
        } else {
            table.put(new Integer(key), value);
        }
    }
    
    public String get(int key) {
        Object value = table.get(new Integer(key));
        if (value == null) return defaultValue;
        return (String)value;
    }
    
    private String defaultValue;
    private Hashtable table = new Hashtable();
}