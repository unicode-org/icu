/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/IntMap.java,v $
* $Date: 2003/03/18 00:10:47 $
* $Revision: 1.1 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;
import java.util.HashMap;

public class IntMap {
    int lowest = Integer.MAX_VALUE;
    int highest = Integer.MIN_VALUE;
    HashMap store = new HashMap();
    
    public Object get(int key) {
        if (key < lowest || key > highest) return null;
        return store.get(new Integer(key));
    }
    
    public void put(int key, Object value) {
        if (key < lowest) lowest = key;
        if (key > highest) highest = key;
        store.put(new Integer(key), value);
    }
    
    public int size() {
        return store.size();
    }
}
        