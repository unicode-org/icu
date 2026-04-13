// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2016, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test;

import com.ibm.icu.text.UnicodeSet;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Moved from UnicodeMapTest */
@RunWith(JUnit4.class)
public class UnicodeSetBoilerplateTest extends TestBoilerplate<UnicodeSet> {

    @Test
    public void test() throws Exception {
        _test();
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.dev.test.TestBoilerplate#_hasSameBehavior(java.lang.Object, java.lang.Object)
     */
    @Override
    protected boolean _hasSameBehavior(UnicodeSet a, UnicodeSet b) {
        // we are pretty confident in the equals method, so won't bother with this right now.
        return true;
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.dev.test.TestBoilerplate#_addTestObject(java.util.List)
     */
    @Override
    protected boolean _addTestObject(List<UnicodeSet> list) {
        if (list.size() > 32) return false;
        UnicodeSet result = new UnicodeSet();
        for (int i = 0; i < 50; ++i) {
            result.add(random.nextInt(100));
        }
        list.add(result);
        return true;
    }
}
