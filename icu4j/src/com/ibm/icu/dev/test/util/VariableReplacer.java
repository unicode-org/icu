/*
 *******************************************************************************
 * Copyright (C) 2002-2008, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class VariableReplacer {
    // simple implementation for now
    private Map m = new TreeMap(Collections.reverseOrder());

    // TODO - fix to do streams also, clean up implementation

    public VariableReplacer add(String variable, String value) {
        m.put(variable, value);
        return this;
    }
    public String replace(String source) {
        String oldSource;
        do {
            oldSource = source;
            for (Iterator it = m.keySet().iterator(); it.hasNext();) {
                String variable = (String) it.next();
                String value = (String) m.get(variable);
                source = replaceAll(source, variable, value);
            }
        } while (!source.equals(oldSource));
        return source;
    }
    public String replaceAll(String source, String key, String value) {
        while (true) {
            int pos = source.indexOf(key);
            if (pos < 0) return source;
            source = source.substring(0,pos) + value + source.substring(pos+key.length());
        }
    }
}

