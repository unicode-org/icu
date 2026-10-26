// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1998-2010, International Business Machines Corporation and    *
 * Unicode, Inc. All Rights Reserved.                                          *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.normalizer;

import java.util.HashMap;
import java.util.Map;

/**
 * Integer-String hash table. Uses Java Hashtable for now.
 *
 * @author Mark Davis
 */
public class IntStringHashtable {

    public IntStringHashtable(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    @SuppressWarnings("ReferenceEquality")
    public void put(int key, String value) {
        if (value == defaultValue) {
            table.remove(key);
        } else {
            table.put(key, value);
        }
    }

    public String get(int key) {
        return table.getOrDefault(key, defaultValue);
    }

    private final String defaultValue;
    private final Map<Integer, String> table = new HashMap<>();
}
