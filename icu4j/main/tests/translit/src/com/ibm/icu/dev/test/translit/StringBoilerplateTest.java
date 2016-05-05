/*
 *******************************************************************************
 * Copyright (C) 2016, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.translit;

import java.util.List;

import com.ibm.icu.dev.test.TestBoilerplate;

/**
 * Moved from UnicodeMapTest
 */
public class StringBoilerplateTest extends TestBoilerplate<String> {

    public static void main(String[] args) throws Exception {
        new StringBoilerplateTest().run(args);
    }

    public void TestStringBoilerplate() throws Exception {
        _test();
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.dev.test.TestBoilerplate#_hasSameBehavior(java.lang.Object, java.lang.Object)
     */
    @Override
    protected boolean _hasSameBehavior(String a, String b) {
        // we are pretty confident in the equals method, so won't bother with this right now.
        return true;
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.dev.test.TestBoilerplate#_addTestObject(java.util.List)
     */
    @Override
    protected boolean _addTestObject(List<String> list) {
        if (list.size() > 31) return false;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < 10; ++i) {
            result.append((char)random.nextInt(0xFF));
        }
        list.add(result.toString());
        return true;
    }
}
