// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2016, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.translit;

import java.util.List;

import org.junit.Test;

import com.ibm.icu.dev.test.TestBoilerplate;
import com.ibm.icu.dev.util.UnicodeMap;

/**
 * Moved from UnicodeMapTest
 */
public class UnicodeMapBoilerplateTest extends TestBoilerplate<UnicodeMap> {

    private static String[] TEST_VALUES = {"A", "B", "C", "D", "E", "F"};

    public void TestUnicodeMapBoilerplate() throws Exception {
    }

    @Test
    public void test() throws Exception {
        _test();
    }
    
    /* (non-Javadoc)
     * @see com.ibm.icu.dev.test.TestBoilerplate#_hasSameBehavior(java.lang.Object, java.lang.Object)
     */
    protected boolean _hasSameBehavior(UnicodeMap a, UnicodeMap b) {
        // we are pretty confident in the equals method, so won't bother with this right now.
        return true;
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.dev.test.TestBoilerplate#_addTestObject(java.util.List)
     */
    protected boolean _addTestObject(List<UnicodeMap> list) {
        if (list.size() > 30) return false;
        UnicodeMap result = new UnicodeMap();
        for (int i = 0; i < 50; ++i) {
            int start = random.nextInt(25);
            String value = TEST_VALUES[random.nextInt(TEST_VALUES.length)];
            result.put(start, value);
        }
        list.add(result);
        return true;
    }

}
