/*
 *******************************************************************************
 * Copyright (C) 2009, Google, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.translit;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

/**
 * @author markdavis
 *
 */
public class AnyScriptTest extends TestFmwk {
    public static void main(String[] args) throws Exception {
        new AnyScriptTest().run(args);
    }
    
    public void TestContext() {
        Transliterator t = Transliterator.createFromRules("foo", "::[bc]; a{b}d > B;", Transliterator.FORWARD);
        String sample = "abd abc b";
        assertEquals("context works", "aBd abc b", t.transform(sample));
    }

    public void TestScripts(){
        // get a couple of characters of each script for testing
        
        StringBuffer testBuffer = new StringBuffer();
        for (int script = 0; script < UScript.CODE_LIMIT; ++script) {
            UnicodeSet test = new UnicodeSet().applyPropertyAlias("script", UScript.getName(script));
            int count = Math.min(20, test.size());
            for (int i = 0; i < count; ++i){
                testBuffer.append(UTF16.valueOf(test.charAt(i)));
            }
        }
        String test = testBuffer.toString();
        logln("Test line: " + test);
        
        int inclusion = getInclusion();
        boolean testedUnavailableScript = false;
        
        for (int script = 0; script < UScript.CODE_LIMIT; ++script) {
            if (script == UScript.COMMON || script == UScript.INHERITED) {
                continue;
            }
            // if the inclusion rate is not 10, skip all but a small number of items.
            // Make sure, however, that we test at least one unavailable script
            if (inclusion < 10 && script != UScript.LATIN
                    && script != UScript.HAN 
                    && script != UScript.HIRAGANA
                    && testedUnavailableScript
                    ) {
                continue;
            }
            
            String scriptName = UScript.getName(script);
            Transliterator t;
            try {
                t = Transliterator.getInstance("any-" + scriptName);
            } catch (Exception e) {
                testedUnavailableScript = true;
                logln("Skipping unavailable: " + scriptName);
                continue; // we don't handle all scripts
            }
            logln("Checking: " + scriptName);
            if (t != null) {
                t.transform(test); // just verify we don't crash
            }
            scriptName = UScript.getShortName(script);
            t = Transliterator.getInstance("any-" + scriptName);
            t.transform(test); // just verify we don't crash
        }
    }
    
    /**
     * Check to make sure that wide characters are converted when going to narrow scripts.
     */
    public void TestForWidth(){
        Transliterator widen = Transliterator.getInstance("halfwidth-fullwidth");
        Transliterator narrow = Transliterator.getInstance("fullwidth-halfwidth");
        UnicodeSet ASCII = new UnicodeSet("[:ascii:]");
        String lettersAndSpace = "abc def";
        final String punctOnly = "( )";
        
        String wideLettersAndSpace = widen.transform(lettersAndSpace);
        String widePunctOnly = widen.transform(punctOnly);
        assertContainsNone("Should be wide", ASCII, wideLettersAndSpace);
        assertContainsNone("Should be wide", ASCII, widePunctOnly);
        
        String back;
        back = narrow.transform(wideLettersAndSpace);
        assertEquals("Should be narrow", lettersAndSpace, back);
        back = narrow.transform(widePunctOnly);
        assertEquals("Should be narrow", punctOnly, back);
        
        Transliterator latin = Transliterator.getInstance("any-Latn");
        back = latin.transform(wideLettersAndSpace);
        assertEquals("Should be ascii", lettersAndSpace, back);
        
        back = latin.transform(widePunctOnly);
        assertEquals("Should be ascii", punctOnly, back);
       
        Transliterator t2 = Transliterator.getInstance("any-Han");
        back = t2.transform(widePunctOnly);
        assertEquals("Should be same", widePunctOnly, back);


    }
    
    public void TestCommonDigits() {
        UnicodeSet westernDigitSet = new UnicodeSet("[0-9]");
        UnicodeSet westernDigitSetAndMarks = new UnicodeSet("[[0-9][:Mn:]]");
        UnicodeSet arabicDigitSet = new UnicodeSet("[[:Nd:]&[:block=Arabic:]]");
        Transliterator latin = Transliterator.getInstance("Any-Latn");
        Transliterator arabic = Transliterator.getInstance("Any-Arabic");
        String westernDigits = getList(westernDigitSet);
        String arabicDigits = getList(arabicDigitSet);

        String fromArabic = latin.transform(arabicDigits);
        assertContainsAll("Any-Latin transforms Arabic digits", westernDigitSetAndMarks, fromArabic);
        if (false) { // we don't require conversion to Arabic digits
            String fromLatin = arabic.transform(westernDigits);
            assertContainsAll("Any-Arabic transforms Western digits", arabicDigitSet, fromLatin);
        }
    }

    // might want to add to TestFmwk
    private void assertContainsAll(String message, UnicodeSet set, String string) {
        handleAssert(set.containsAll(string), message, set, string, "contains all of", false);
    }

    private void assertContainsNone(String message, UnicodeSet set, String string) {
        handleAssert(set.containsNone(string), message, set, string, "contains none of", false);
    }

    // might want to add to UnicodeSet
    private String getList(UnicodeSet set) {
        StringBuffer result = new StringBuffer();
        for (UnicodeSetIterator it = new UnicodeSetIterator(set); it.next();) {
            result.append(it.getString());
        }
        return result.toString();
    }
}
