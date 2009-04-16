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

/**
 * @author markdavis
 *
 */
public class AnyScriptTest extends TestFmwk {
    public static void main(String[] args) throws Exception {
        new AnyScriptTest().run(args);
    }

    public void TestScripts(){
        // get a couple of characters of each script for testing
        StringBuffer testBuffer = new StringBuffer();
        for (int script = 0; script < UScript.CODE_LIMIT; ++script) {
            UnicodeSet test = new UnicodeSet("[:script=" + UScript.getName(script) + ":]");
            int count = Math.min(3, test.size());
            for (int i = 0; i < count; ++i){
                testBuffer.append(UTF16.valueOf(test.charAt(i)));
            }
        }
        String test = testBuffer.toString();
        logln("Test line: " + test);
        
        for (int script = 0; script < UScript.CODE_LIMIT; ++script) {
            if (script == UScript.COMMON || script == UScript.INHERITED) {
                continue;
            }
            String scriptName = UScript.getName(script);
            Transliterator t;
            try {
                t = Transliterator.getInstance("any-" + scriptName);
            } catch (Exception e) {
                continue; // we don't handle all scripts
            }
            logln("Checking " + scriptName);
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
        UnicodeSet ASCII = new UnicodeSet("[:ascii:]");
        String lettersAndSpace = "abc def";
        final String punctOnly = "( )";
        String wideLettersAndSpace = widen.transform(lettersAndSpace);
        String widePunctOnly = widen.transform(punctOnly);
        assertTrue("Should be wide", ASCII.containsNone(wideLettersAndSpace));
        assertTrue("Should be wide", ASCII.containsNone(widePunctOnly));
        
        Transliterator latin = Transliterator.getInstance("any-Latn");
        String back = latin.transform(wideLettersAndSpace);
        assertEquals("Should be ascii", lettersAndSpace, back);
        
        back = latin.transform(widePunctOnly);
        assertEquals("Should be ascii", punctOnly, back);
       
        Transliterator t2 = Transliterator.getInstance("any-Han");
        back = t2.transform(widePunctOnly);
        assertEquals("Should be same", widePunctOnly, back);


    }
}
