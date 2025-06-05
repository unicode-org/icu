// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.translit;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.Transliterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/***********************************************************************
 *
 * This test class uses JUnit parametrization to iterate over all
 * transliterators and to execute a sample operation.
 *
 ***********************************************************************/

@RunWith(Parameterized.class)
public class TransliteratorInstantiateAllTest extends TestFmwk {
    private String testTransliteratorID;

    public TransliteratorInstantiateAllTest /*InstantiationTest*/(String t) {
        this.testTransliteratorID = t;
    }

    @Parameterized.Parameters
    public static Collection<String> testData() {
        ArrayList<String> allTranslitIDs = new ArrayList<String>();

        for (Enumeration<String> e = Transliterator.getAvailableIDs(); e.hasMoreElements(); ) {
            String id = e.nextElement();
            allTranslitIDs.add(id);
        }

        return allTranslitIDs;
    }

    @Test
    public void TestInstantiation() {
        Transliterator t = null;

        try {
            t = Transliterator.getInstance(testTransliteratorID);
        } catch (IllegalArgumentException ex) {
            errln("FAIL: " + testTransliteratorID);
            throw ex;
        }

        if (t != null) {
            // Test toRules
            String rules = null;
            try {
                rules = t.toRules(true);
                Transliterator.createFromRules("x", rules, Transliterator.FORWARD);
            } catch (IllegalArgumentException ex2) {
                errln("FAIL: " + "ID" + ".toRules() => bad rules: " + rules);
                throw ex2;
            }
        }
    }
}
