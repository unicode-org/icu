/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/test/translit/Attic/UnicodeSetTest.java,v $ 
 * $Date: 2000/03/10 03:47:47 $ 
 * $Revision: 1.6 $
 *
 *****************************************************************************************
 */
package com.ibm.test.translit;
import com.ibm.text.*;
import com.ibm.test.*;
import java.text.*;
import java.util.*;

/**
 * @test
 * @summary General test of UnicodeSet
 */
public class UnicodeSetTest extends TestFmwk {

    public static void main(String[] args) throws Exception {
        new UnicodeSetTest().run(args);
    }

    public void TestPatterns() {
        UnicodeSet set = new UnicodeSet();
        expectPattern(set, "[[a-m]&[d-z]&[k-y]]",  "km");
        expectPattern(set, "[[a-z]-[m-y]-[d-r]]",  "aczz");
        expectPattern(set, "[a\\-z]",  "--aazz");
        expectPattern(set, "[-az]",  "--aazz");
        expectPattern(set, "[az-]",  "--aazz");
        expectPattern(set, "[[[a-z]-[aeiou]i]]", "bdfnptvz");

        // Throw in a test of complement
        set.complement();
        String exp = '\u0000' + "aeeoouu" + (char)('z'+1) + '\uFFFF';
        expectPairs(set, exp);
    }

    public void TestCategories() {
        UnicodeSet set = new UnicodeSet("[:Lu:]");
        expectContainment(set, "ABC", "abc");
    }

    public void TestAddRemove() {
        UnicodeSet set = new UnicodeSet();
        set.add('a', 'z');
        expectPairs(set, "az");
        set.remove('m', 'p');
        expectPairs(set, "alqz");
        set.remove('e', 'g');
        expectPairs(set, "adhlqz");
        set.remove('d', 'i');
        expectPairs(set, "acjlqz");
        set.remove('c', 'r');
        expectPairs(set, "absz");
        set.add('f', 'q');
        expectPairs(set, "abfqsz");
        set.remove('a', 'g');
        expectPairs(set, "hqsz");
        set.remove('a', 'z');
        expectPairs(set, "");

        // Try removing an entire set from another set
        expectPattern(set, "[c-x]", "cx");
        UnicodeSet set2 = new UnicodeSet();
        expectPattern(set2, "[f-ky-za-bc[vw]]", "acfkvwyz");
        set.removeAll(set2);
        expectPairs(set, "deluxx");

        // Try adding an entire set to another set
        expectPattern(set, "[jackiemclean]", "aacceein");
        expectPattern(set2, "[hitoshinamekatajamesanderson]", "aadehkmort");
        set.addAll(set2);
        expectPairs(set, "aacehort");

        // Test commutativity
        expectPattern(set, "[hitoshinamekatajamesanderson]", "aadehkmort");
        expectPattern(set2, "[jackiemclean]", "aacceein");
        set.addAll(set2);
        expectPairs(set, "aacehort");
    }

    void expectContainment(UnicodeSet set, String charsIn, String charsOut) {
        StringBuffer bad = new StringBuffer();
        if (charsIn != null) {
            for (int i=0; i<charsIn.length(); ++i) {
                char c = charsIn.charAt(i);
                if (!set.contains(c)) {
                    bad.append(c);
                }
            }
            if (bad.length() > 0) {
                logln("Fail: set " + set + " does not contain " + bad +
                      ", expected containment of " + charsIn);
            } else {
                logln("Ok: set " + set + " contains " + charsIn);
            }
        }
        if (charsOut != null) {
            bad.setLength(0);
            for (int i=0; i<charsOut.length(); ++i) {
                char c = charsOut.charAt(i);
                if (set.contains(c)) {
                    bad.append(c);
                }
            }
            if (bad.length() > 0) {
                logln("Fail: set " + set + " contains " + bad +
                      ", expected non-containment of " + charsOut);
            } else {
                logln("Ok: set " + set + " does not contain " + charsOut);
            }
        }
    }

    void expectPattern(UnicodeSet set,
                       String pattern,
                       String expectedPairs) {
        set.applyPattern(pattern);
        if (!set.getPairs().equals(expectedPairs)) {
            errln("FAIL: applyPattern(\"" + pattern +
                  "\") => pairs \"" +
                  escape(set.getPairs()) + "\", expected \"" +
                  escape(expectedPairs) + "\"");
        } else {
            logln("Ok:   applyPattern(\"" + pattern +
                  "\") => pairs \"" +
                  escape(set.getPairs()) + "\"");
        }
    }

    void expectPairs(UnicodeSet set, String expectedPairs) {
        if (!set.getPairs().equals(expectedPairs)) {
            errln("FAIL: Expected pair list \"" +
                  escape(expectedPairs) + "\", got \"" +
                  escape(set.getPairs()) + "\"");
        }
    }

    /**
     * Escape non-ASCII characters as Unicode.
     */
    static final String escape(String s) {
        StringBuffer buf = new StringBuffer();
        for (int i=0; i<s.length(); ++i) {
            char c = s.charAt(i);
            if (c >= ' ' && c <= 0x007F) {
                buf.append(c);
            } else {
                buf.append("\\u");
                if (c < 0x1000) {
                    buf.append('0');
                    if (c < 0x100) {
                        buf.append('0');
                        if (c < 0x10) {
                            buf.append('0');
                        }
                    }
                }
                buf.append(Integer.toHexString(c));
            }
        }
        return buf.toString();
    }
}
