/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/translit/UnicodeSetTest.java,v $ 
 * $Date: 2001/04/04 22:44:49 $ 
 * $Revision: 1.10 $
 *
 *****************************************************************************************
 */
package com.ibm.test.translit;
import com.ibm.text.*;
import com.ibm.test.*;
import com.ibm.util.Utility;
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
        int failures = 0;
        UnicodeSet set = new UnicodeSet("[:Lu:]");
        expectContainment(set, "ABC", "abc");

        // Make sure generation of L doesn't pollute cached Lu set
        // First generate L, then Lu
        int TOP = 0x200; // Don't need to go over the whole range:
        set = new UnicodeSet("[:L:]");
        for (int i=0; i<0x200; ++i) {
            boolean l = Character.isLetter((char)i);
            if (l != set.contains((char)i)) {
                errln("FAIL: L contains " + (char)i + " = " + 
                      set.contains((char)i));
                if (++failures == 10) break;
            }
        }

        set = new UnicodeSet("[:Lu:]");
        for (int i=0; i<0x200; ++i) {
            boolean lu = (Character.getType((char)i) == Character.UPPERCASE_LETTER);
            if (lu != set.contains((char)i)) {
                errln("FAIL: Lu contains " + (char)i + " = " + 
                      set.contains((char)i));
                if (++failures == 20) break;
            }
        }
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

    /**
     * Make sure minimal representation is maintained.
     */
    public void TestMinimalRep() {
        // This is pretty thoroughly tested by checkCanonicalRep()
        // run against the exhaustive operation results.  Use the code
        // here for debugging specific spot problems.
       
        // 1 overlap against 2
        UnicodeSet set = new UnicodeSet("[h-km-q]");
        UnicodeSet set2 = new UnicodeSet("[i-o]");
        set.addAll(set2);
        expectPairs(set, "hq");
        // right
        set.applyPattern("[a-m]");
        set2.applyPattern("[e-o]");
        set.addAll(set2);
        expectPairs(set, "ao");
        // left
        set.applyPattern("[e-o]");
        set2.applyPattern("[a-m]");
        set.addAll(set2);
        expectPairs(set, "ao");
        // 1 overlap against 3
        set.applyPattern("[a-eg-mo-w]");
        set2.applyPattern("[d-q]");
        set.addAll(set2);
        expectPairs(set, "aw");
    }

    public void TestAPI() {
        // default ct
        UnicodeSet set = new UnicodeSet();
        if (!set.isEmpty() || set.getRangeCount() != 0) {
            errln("FAIL, set should be empty but isn't: " +
                  set);
        }

        // clear(), isEmpty()
        set.add('a');
        if (set.isEmpty()) {
            errln("FAIL, set shouldn't be empty but is: " +
                  set);
        }
        set.clear();
        if (!set.isEmpty()) {
            errln("FAIL, set should be empty but isn't: " +
                  set);
        }

        // size()
        set.clear();
        if (set.size() != 0) {
            errln("FAIL, size should be 0, but is " + set.size() +
                  ": " + set);
        }
        set.add('a');
        if (set.size() != 1) {
            errln("FAIL, size should be 1, but is " + set.size() +
                  ": " + set);
        }
        set.add('1', '9');
        if (set.size() != 10) {
            errln("FAIL, size should be 10, but is " + set.size() +
                  ": " + set);
        }

        // contains(first, last)
        set.clear();
        set.applyPattern("[A-Y 1-8 b-d l-y]");
        for (int i = 0; i<set.getRangeCount(); ++i) {
            char a = set.getRangeStart(i);
            char b = set.getRangeEnd(i);
            if (!set.contains(a, b)) {
                errln("FAIL, should contain " + (char)a + '-' + (char)b +
                      " but doesn't: " + set);
            }
            if (set.contains((char)(a-1), b)) {
                errln("FAIL, shouldn't contain " +
                      (char)(a-1) + '-' + (char)b +
                      " but does: " + set);
            }
            if (set.contains(a, (char)(b+1))) {
                errln("FAIL, shouldn't contain " +
                      (char)a + '-' + (char)(b+1) +
                      " but does: " + set);
            }
        }

        // Ported InversionList test.
        UnicodeSet a = new UnicodeSet((char)3,(char)10);
        UnicodeSet b = new UnicodeSet((char)7,(char)15);
        UnicodeSet c = new UnicodeSet();

        logln("a [3-10]: " + a);
        logln("b [7-15]: " + b);
        c.set(a); c.addAll(b);
        UnicodeSet exp = new UnicodeSet((char)3,(char)15);
        if (c.equals(exp)) {
            logln("c.set(a).add(b): " + c);
        } else {
            errln("FAIL: c.set(a).add(b) = " + c + ", expect " + exp);
        }
        c.complement();
        exp.set((char)0, (char)2);
        exp.add((char)16, UnicodeSet.MAX_VALUE);
        if (c.equals(exp)) {
            logln("c.complement(): " + c);
        } else {
            errln("FAIL: c.complement() = " + c + ", expect " + exp);
        }
        c.complement();
        exp.set((char)3, (char)15);
        if (c.equals(exp)) {
            logln("c.complement(): " + c);
        } else {
            errln("FAIL: c.complement() = " + c + ", expect " + exp);
        }
        c.set(a); c.complementAll(b);
        exp.set((char)3,(char)6);
        exp.add((char)11,(char) 15);
        if (c.equals(exp)) {
            logln("c.set(a).complement(b): " + c);
        } else {
            errln("FAIL: c.set(a).complement(b) = " + c + ", expect " + exp);
        }

        exp.set(c);
        c = bitsToSet(setToBits(c));
        if (c.equals(exp)) {
            logln("bitsToSet(setToBits(c)): " + c);
        } else {
            errln("FAIL: bitsToSet(setToBits(c)) = " + c + ", expect " + exp);
        }
    }

    /**
     * Test the indexOf() and charAt() methods.
     */
    public void TestIndexOf() {
        UnicodeSet set = new UnicodeSet("[a-cx-y3578]");
        for (int i=0; i<set.size(); ++i) {
            char c = set.charAt(i);
            if (set.indexOf(c) != i) {
                errln("FAIL: charAt(" + i + ") = " + c +
                      " => indexOf() => " + set.indexOf(c));
            }
        }
        char c = set.charAt(set.size());
        if (c != '\uFFFE') {
            errln("FAIL: charAt(<out of range>) = " +
                  Utility.escape(String.valueOf(c)));
        }
        int j = set.indexOf('q');
        if (j != -1) {
            errln("FAIL: indexOf('q') = " + j);
        }
    }

    public void TestExhaustive() {
        // exhaustive tests. Simulate UnicodeSets with integers.
        // That gives us very solid tests (except for large memory tests).

        char limit = (char)128;

        for (char i = 0; i < limit; ++i) {
            logln("Testing " + i + ", " + bitsToSet(i));
            _testComplement(i);
            for (char j = 0; j < limit; ++j) {
                _testAdd(i,j);
                _testXor(i,j);
                _testRetain(i,j);
                _testRemove(i,j);
            }
        }
    }
    
    void _testComplement(int a) {
        UnicodeSet x = bitsToSet(a);
        UnicodeSet z = bitsToSet(a);
        z.complement();
        int c = setToBits(z);
        if (c != (~a)) {
            errln("FAILED: add: ~" + x +  " != " + z);
            errln("FAILED: add: ~" + a + " != " + c);
        }
        checkCanonicalRep(z, "complement " + a);
    }

    void _testAdd(int a, int b) {
        UnicodeSet x = bitsToSet(a);
        UnicodeSet y = bitsToSet(b);
        UnicodeSet z = bitsToSet(a);
        z.addAll(y);
        int c = setToBits(z);
        if (c != (a | b)) {
            errln(Utility.escape("FAILED: add: " + x + " | " + y + " != " + z));
            errln("FAILED: add: " + a + " | " + b + " != " + c);
        }
        checkCanonicalRep(z, "add " + a + "," + b);
    }

    void _testRetain(int a, int b) {
        UnicodeSet x = bitsToSet(a);
        UnicodeSet y = bitsToSet(b);
        UnicodeSet z = bitsToSet(a);
        z.retainAll(y);
        int c = setToBits(z);
        if (c != (a & b)) {
            errln("FAILED: retain: " + x + " & " + y + " != " + z);
            errln("FAILED: retain: " + a + " & " + b + " != " + c);
        }
        checkCanonicalRep(z, "retain " + a + "," + b);
    }

    void _testRemove(int a, int b) {
        UnicodeSet x = bitsToSet(a);
        UnicodeSet y = bitsToSet(b);
        UnicodeSet z = bitsToSet(a);
        z.removeAll(y);
        int c = setToBits(z);
        if (c != (a &~ b)) {
            errln("FAILED: remove: " + x + " &~ " + y + " != " + z);
            errln("FAILED: remove: " + a + " &~ " + b + " != " + c);
        }
        checkCanonicalRep(z, "remove " + a + "," + b);
    }

    void _testXor(int a, int b) {
        UnicodeSet x = bitsToSet(a);
        UnicodeSet y = bitsToSet(b);
        UnicodeSet z = bitsToSet(a);
        z.complementAll(y);
        int c = setToBits(z);
        if (c != (a ^ b)) {
            errln("FAILED: complement: " + x + " ^ " + y + " != " + z);
            errln("FAILED: complement: " + a + " ^ " + b + " != " + c);
        }
        checkCanonicalRep(z, "complement " + a + "," + b);
    }
    
    /**
     * Check that ranges are monotonically increasing and non-
     * overlapping.
     */
    void checkCanonicalRep(UnicodeSet set, String msg) {
        int n = set.getRangeCount();
        if (n < 0) {
            errln("FAIL result of " + msg +
                  ": range count should be >= 0 but is " +
                  n + " for " + Utility.escape(set.toString()));
            return;
        }
        int last = 0;
        for (int i=0; i<n; ++i) {
            int start = set.getRangeStart(i);
            int end = set.getRangeEnd(i);
            if (start > end) {
                errln("FAIL result of " + msg +
                      ": range " + (i+1) +
                      " start > end: " + start + ", " + end +
                      " for " + Utility.escape(set.toString()));
            }
            if (i > 0 && start <= last) {
                errln("FAIL result of " + msg +
                      ": range " + (i+1) +
                      " overlaps previous range: " + start + ", " + end +
                      " for " + Utility.escape(set.toString()));
            }
            last = end;
        }
    }

    /**
     * Convert a bitmask to a UnicodeSet.
     */
    static UnicodeSet bitsToSet(int a) {
        UnicodeSet result = new UnicodeSet();
        for (int i = 0; i < 32; ++i) {
            if ((a & (1<<i)) != 0) {
                result.add((char)i,(char)i);
            }
        }
        return result;
    }
    
    /**
     * Convert a UnicodeSet to a bitmask.  Only the characters
     * U+0000 to U+0020 are represented in the bitmask.
     */
    static int setToBits(UnicodeSet x) {
        int result = 0;
        for (int i = 0; i < 32; ++i) {
            if (x.contains((char)i)) {
                result |= (1<<i);
            }
        }
        return result;
    }

    /**
     * Return the representation of an inversion list based UnicodeSet
     * as a pairs list.  Ranges are listed in ascending Unicode order.
     * For example, the set [a-zA-M3] is represented as "33AMaz".
     */
    static String getPairs(UnicodeSet set) {
        StringBuffer pairs = new StringBuffer();
        for (int i=0; i<set.getRangeCount(); ++i) {
            int start = set.getRangeStart(i);
            int end = set.getRangeEnd(i);
            if (end > 0xFFFF) {
                end = 0xFFFF;
                i = set.getRangeCount(); // Should be unnecessary
            }
            pairs.append((char)start).append((char)end);
        }
        return pairs.toString();
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
                logln(Utility.escape("Fail: set " + set + " does not contain " + bad +
                      ", expected containment of " + charsIn));
            } else {
                logln(Utility.escape("Ok: set " + set + " contains " + charsIn));
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
                logln(Utility.escape("Fail: set " + set + " contains " + bad +
                      ", expected non-containment of " + charsOut));
            } else {
                logln(Utility.escape("Ok: set " + set + " does not contain " + charsOut));
            }
        }
    }

    void expectPattern(UnicodeSet set,
                       String pattern,
                       String expectedPairs) {
        set.applyPattern(pattern);
        if (!getPairs(set).equals(expectedPairs)) {
            errln("FAIL: applyPattern(\"" + pattern +
                  "\") => pairs \"" +
                  Utility.escape(getPairs(set)) + "\", expected \"" +
                  Utility.escape(expectedPairs) + "\"");
        } else {
            logln("Ok:   applyPattern(\"" + pattern +
                  "\") => pairs \"" +
                  Utility.escape(getPairs(set)) + "\"");
        }
    }

    void expectPairs(UnicodeSet set, String expectedPairs) {
        if (!getPairs(set).equals(expectedPairs)) {
            errln("FAIL: Expected pair list \"" +
                  Utility.escape(expectedPairs) + "\", got \"" +
                  Utility.escape(getPairs(set)) + "\"");
        }
    }
}
