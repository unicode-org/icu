/*
 *******************************************************************************
 * Copyright (C) 2006, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
*/
package com.ibm.icu.dev.test.util;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.LRUMap;

public class LRUMapTest extends TestFmwk {
    public static void main(String[] args) throws Exception {
        LRUMapTest test = new LRUMapTest();
        test.run(args);
    }

    public void TestLRUMap() {
        // Default size - max 64
        logln("Testing LRUMap with the default size");
        LRUMap map = new LRUMap();
        execute(map, 64);

        // max size - 16
        logln("Testing LRUMap with initial/max size - 4/16");
        map = new LRUMap(4, 16);
        execute(map, 16);
    }

    private void execute(LRUMap map, int maxSize /* maxSize > 6 */) {
        Integer num;
        String numStr;

        for (int i = 0; i <= maxSize; i++) {
            num = new Integer(i);
            numStr = num.toString();
            map.put(numStr, num);
        }
        // The first key/value should be removed, because
        // we already put 65 entries
        num = (Integer)map.get("0");
        if (num == null) {
            logln("OK: The entry '0' was removed.");
        }
        else {
            errln("The entry '0' is still available.");
        }
        // Access the second entry '1', which is currently
        // the eldest.
        num = (Integer)map.get("1");
        if (num == null) {
            errln("The eldest entry '1' was removed.");
        }
        else {
            logln("OK: The eldest entry '1' is available.");
        }
        // Put another entry - because the entry '1' was
        // accessed above, the entry '2' is the eldest and
        // putting new entry should remove the entry.
        num = new Integer(maxSize + 1);
        map.put(num.toString(), num);
        num = (Integer)map.get("2");
        if (num == null) {
            logln("OK: The entry '2' was removed.");
        }
        else {
            errln("The entry '2' is still available.");
        }
        // The entry '3' is the eldest for now.
        boolean b = map.containsKey("3");
        if (b) {
            logln("OK: The eldest entry '3' is available.");
        }
        else {
            errln("The eldest entry '3' was removed.");
        }
        // contansKey should not affect the access order
        num = new Integer(maxSize + 2);
        map.put(num.toString(), num);
        num = (Integer)map.get("3");
        if (num == null) {
            logln("OK: The entry '3' was removed.");
        }
        else {
            errln("The entry '3' is still available.");
        }
        // Putting existing entry with new value
        num = (Integer)map.put("4", new Integer(-4));
        if (num == null) {
            errln("The entry '4' no longer exists");
        }
        if (num.intValue() != 4) {
            errln("The value for '4' was not 4");
        }
        // The entry '5' is the eldest for now, because
        // the entry '4' was updated above.
        num = new Integer(maxSize + 3);
        map.put(num.toString(), num);
        num = (Integer)map.get("5");
        if (num == null) {
            logln("OK: The entry '5' was removed.");
        }
        else {
            errln("The entry '5' is still available.");
        }
        // Clear the map
        map.clear();
        num = (Integer)map.get("6");
        if (num == null) {
            logln("OK: The entry '6' was removed.");
        }
        else {
            errln("The entry '6' is still available.");
        }        
    }
}
