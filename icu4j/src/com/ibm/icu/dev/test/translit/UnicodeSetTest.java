/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/translit/UnicodeSetTest.java,v $ 
 * $Date: 2002/03/18 01:08:46 $ 
 * $Revision: 1.27 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.dev.test.translit;
import com.ibm.icu.lang.*;
import com.ibm.icu.text.*;
import com.ibm.icu.dev.test.*;
import com.ibm.icu.impl.Utility;
import java.text.*;
import java.util.*;

/**
 * @test
 * @summary General test of UnicodeSet
 */
public class UnicodeSetTest extends TestFmwk {

    static final String NOT = "%%%%";

    public static void main(String[] args) throws Exception {
        new UnicodeSetTest().run(args);
    }

    /**
     * Test that toPattern() round trips with syntax characters and
     * whitespace.
     */
    public void TestToPattern() throws Exception {
        for (int i = 0; i < OTHER_TOPATTERN_TESTS.length; ++i) {
            checkPat(OTHER_TOPATTERN_TESTS[i], new UnicodeSet(OTHER_TOPATTERN_TESTS[i]));
        }
        for (int i = 0; i <= 0x10FFFF; ++i) {
            if ((i <= 0xFF && !UCharacter.isLetter(i)) || UCharacter.isWhitespace(i)) {
                // check various combinations to make sure they all work.
                if (i != 0 && !toPatternAux(i, i)) continue;
                if (!toPatternAux(0, i)) continue;
                if (!toPatternAux(i, 0xFFFF)) continue;
            }
        }
    }
    
    static String[] OTHER_TOPATTERN_TESTS = {
                "[[:latin:]&[:greek:]]", 
                "[[:latin:]-[:greek:]]",
                "[:nonspacing mark:]"
    };
    
    
    public boolean toPatternAux(int start, int end) {
        // use Integer.toString because Utility.hex doesn't handle ints
        String source = "0x" + Integer.toString(start,16).toUpperCase();
        if (start != end) source += "..0x" + Integer.toString(end,16).toUpperCase();
        UnicodeSet testSet = new UnicodeSet();
        testSet.add(start, end);
        return checkPat(source, testSet);
    }
    
    boolean checkPat (String source, UnicodeSet testSet) {
        String pat = "";
        try {
            // What we want to make sure of is that a pattern generated
            // by toPattern(), with or without escaped unprintables, can
            // be passed back into the UnicodeSet constructor.
            String pat0 = testSet.toPattern(true);
            if (!checkPat(source + " (escaped)", testSet, pat0)) return false;
            
            //String pat1 = unescapeLeniently(pat0);
            //if (!checkPat(source + " (in code)", testSet, pat1)) return false;
            
            String pat2 = testSet.toPattern(false);
            if (!checkPat(source, testSet, pat2)) return false;
            
            //String pat3 = unescapeLeniently(pat2);
            //if (!checkPat(source + " (in code)", testSet, pat3)) return false;
            
            //logln(source + " => " + pat0 + ", " + pat1 + ", " + pat2 + ", " + pat3);
            logln(source + " => " + pat0 + ", " + pat2);
        } catch (Exception e) {
            errln("EXCEPTION in toPattern: " + source + " => " + pat);
            return false;
        }
        return true;
    }
    
    boolean checkPat (String source, UnicodeSet testSet, String pat) {
        UnicodeSet testSet2 = new UnicodeSet(pat);
        if (!testSet2.equals(testSet)) {
            errln("Fail toPattern: " + source + " => " + pat);
            return false;
        }
        return true;
    }
    
    
    // NOTE: copied the following from Utility. There ought to be a version in there with a flag
    // that does the Java stuff
    
    public static int unescapeAt(String s, int[] offset16) {
        int c;
        int result = 0;
        int n = 0;
        int minDig = 0;
        int maxDig = 0;
        int bitsPerDigit = 4;
        int dig;
        int i;

        /* Check that offset is in range */
        int offset = offset16[0];
        int length = s.length();
        if (offset < 0 || offset >= length) {
            return -1;
        }

        /* Fetch first UChar after '\\' */
        c = UTF16.charAt(s, offset);
        offset += UTF16.getCharCount(c);

        /* Convert hexadecimal and octal escapes */
        switch (c) {
        case 'u':
            minDig = maxDig = 4;
            break;
        /*
        case 'U':
            minDig = maxDig = 8;
            break;
        case 'x':
            minDig = 1;
            maxDig = 2;
            break;
        */
        default:
            dig = UCharacter.digit(c, 8);
            if (dig >= 0) {
                minDig = 1;
                maxDig = 3;
                n = 1; /* Already have first octal digit */
                bitsPerDigit = 3;
                result = dig;
            }
            break;
        }
        if (minDig != 0) {
            while (offset < length && n < maxDig) {
                // TEMPORARY
                // TODO: Restore the char32-based code when UCharacter.digit
                // is working (Bug 66).

                //c = UTF16.charAt(s, offset);
                //dig = UCharacter.digit(c, (bitsPerDigit == 3) ? 8 : 16);
                c = s.charAt(offset);
                dig = Character.digit((char)c, (bitsPerDigit == 3) ? 8 : 16);
                if (dig < 0) {
                    break;
                }
                result = (result << bitsPerDigit) | dig;
                //offset += UTF16.getCharCount(c);
                ++offset;
                ++n;
            }
            if (n < minDig) {
                return -1;
            }
            offset16[0] = offset;
            return result;
        }

        /* Convert C-style escapes in table */
        for (i=0; i<UNESCAPE_MAP.length; i+=2) {
            if (c == UNESCAPE_MAP[i]) {
                offset16[0] = offset;
                return UNESCAPE_MAP[i+1];
            } else if (c < UNESCAPE_MAP[i]) {
                break;
            }
        }

        /* If no special forms are recognized, then consider
         * the backslash to generically escape the next character. */
        offset16[0] = offset;
        return c;
    }

    /* This map must be in ASCENDING ORDER OF THE ESCAPE CODE */
    static private final char[] UNESCAPE_MAP = {
        /*"   0x22, 0x22 */
        /*'   0x27, 0x27 */
        /*?   0x3F, 0x3F */
        /*\   0x5C, 0x5C */
        /*a*/ 0x61, 0x07,
        /*b*/ 0x62, 0x08,
        /*f*/ 0x66, 0x0c,
        /*n*/ 0x6E, 0x0a,
        /*r*/ 0x72, 0x0d,
        /*t*/ 0x74, 0x09,
        /*v*/ 0x76, 0x0b
    };

    /**
     * Convert all escapes in a given string using unescapeAt().
     * Leave invalid escape sequences unchanged.
     */
    public static String unescapeLeniently(String s) {
        StringBuffer buf = new StringBuffer();
        int[] pos = new int[1];
        for (int i=0; i<s.length(); ) {
            char c = s.charAt(i++);
            if (c == '\\') {
                pos[0] = i;
                int e = unescapeAt(s, pos);
                if (e < 0) {
                    buf.append(c);
                } else {
                    UTF16.append(buf, e);
                    i = pos[0];
                }
            } else {
                buf.append(c);
            }
        }
        return buf.toString();
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
        // not used int TOP = 0x200; // Don't need to go over the whole range:
        set = new UnicodeSet("[:L:]");
        for (int i=0; i<0x200; ++i) {
            boolean l = UCharacter.isLetter(i);
            if (l != set.contains((char)i)) {
                errln("FAIL: L contains " + (char)i + " = " + 
                      set.contains((char)i));
                if (++failures == 10) break;
            }
        }

        set = new UnicodeSet("[:Lu:]");
        for (int i=0; i<0x200; ++i) {
            boolean lu = (UCharacter.getType(i) == UCharacterCategory.UPPERCASE_LETTER);
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
            int a = set.getRangeStart(i);
            int b = set.getRangeEnd(i);
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
            errln(Utility.escape("FAIL: c.complement() = " + c + ", expect " + exp));
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
    
    public void TestStrings() {
        Object[][] testList = {
            {I_EQUALS,  UnicodeSet.fromAll("abc"),
                        new UnicodeSet("[a-c]")},
                        
            {I_EQUALS,  UnicodeSet.from("ch").add('a','z').add("ll"),
                        new UnicodeSet("[{ll}{ch}a-z]")},
                        
            {I_EQUALS,  UnicodeSet.from("ab}c"),  
                        new UnicodeSet("[{ab\\}c}]")},
                        
            {I_EQUALS,  new UnicodeSet('a','z').add('A', 'Z').retain('M','m').complement('X'), 
                        new UnicodeSet("[[a-zA-Z]&[M-m]-[X]]")},
        };
        
        for (int i = 0; i < testList.length; ++i) {
            expectRelation(testList[i][0], testList[i][1], testList[i][2], "(" + i + ")");
        }        
    }
    
    /**
     * Test pattern behavior of multicharacter strings.
     */
    public void TestStringPatterns() {
        UnicodeSet s = new UnicodeSet("[a-z {aa} {ab}]");
        expectToPattern(s, "[a-z{aa}{ab}]",
                        new String[] {"aa", "ab", NOT, "ac"});
        s.add("ac");
        expectToPattern(s, "[a-z{aa}{ab}{ac}]",
                        new String[] {"aa", "ab", "ac", NOT, "xy"});

        s.applyPattern("[a-z {\\{l} {r\\}}]");
        expectToPattern(s, "[a-z{\\{l}{r\\}}]",
                        new String[] {"{l", "r}", NOT, "xy"});
        s.add("[]");
        expectToPattern(s, "[a-z{\\[\\]}{r\\}}{\\{l}]",
                        new String[] {"{l", "r}", "[]", NOT, "xy"});

        s.applyPattern("[a-z {\u4E01\u4E02}{\\n\\r}]");
        expectToPattern(s, "[a-z{\\u4E01\\u4E02}{\\n\\r}]",
                        new String[] {"\u4E01\u4E02", "\n\r"});
    }

    static final Integer 
       I_ANY = new Integer(UnicodeSet.ANY),
       I_CONTAINS = new Integer(UnicodeSet.CONTAINS),
       I_DISJOINT = new Integer(UnicodeSet.DISJOINT),
       I_NO_B = new Integer(UnicodeSet.NO_B),
       I_ISCONTAINED = new Integer(UnicodeSet.ISCONTAINED),
       I_EQUALS = new Integer(UnicodeSet.EQUALS),
       I_NO_A = new Integer(UnicodeSet.NO_A),
       I_NONE = new Integer(UnicodeSet.NONE);
    
    public void TestSetRelation() {

        String[] choices = {"a", "b", "cd", "ef"};
        int limit = 1 << choices.length;
        
        SortedSet iset = new TreeSet();
        SortedSet jset = new TreeSet();
        
        for (int i = 0; i < limit; ++i) {
            pick(i, choices, iset);
            for (int j = 0; j < limit; ++j) {
                pick(j, choices, jset);
                checkSetRelation(iset, jset, "(" + i + ")");
            }
        }
    }
    
    public void TestSetSpeed() {
        // skip unless verbose
        if (!isVerbose()) return;
        
        SetSpeed2(100);
        SetSpeed2(1000);
    }
    
    public void SetSpeed2(int size) {
        
        SortedSet iset = new TreeSet();
        SortedSet jset = new TreeSet();
        
        for (int i = 0; i < size*2; i += 2) { // only even values
            iset.add(new Integer(i));
            jset.add(new Integer(i));
        }
        
        int iterations = 1000000 / size;
        
        logln("Timing comparison of Java vs Utility");
        logln("For about " + size + " objects that are almost all the same.");
        
        CheckSpeed(iset, jset, "when a = b", iterations);
        
        iset.add(new Integer(size + 1));    // add odd value in middle
        
        CheckSpeed(iset, jset, "when a contains b", iterations);        
        CheckSpeed(jset, iset, "when b contains a", iterations);
        
        jset.add(new Integer(size - 1));    // add different odd value in middle
        
        CheckSpeed(jset, iset, "when a, b are disjoint", iterations);        
    }
    
    void CheckSpeed(SortedSet iset, SortedSet jset, String message, int iterations) {
        CheckSpeed2(iset, jset, message, iterations);
        CheckSpeed3(iset, jset, message, iterations);
    }
    
    void CheckSpeed2(SortedSet iset, SortedSet jset, String message, int iterations) {
        boolean x;
        boolean y;
        
        // make sure code is loaded:
        x = iset.containsAll(jset);
        y = UnicodeSet.hasRelation(iset, UnicodeSet.CONTAINS, jset);
        if (x != y) errln("FAIL contains comparison");
        
        double start = System.currentTimeMillis();
        for (int i = 0; i < iterations; ++i) {
            x |= iset.containsAll(jset);
        }
        double middle = System.currentTimeMillis();
        for (int i = 0; i < iterations; ++i) {
            y |= UnicodeSet.hasRelation(iset, UnicodeSet.CONTAINS, jset);
        }
        double end = System.currentTimeMillis();
        
        double jtime = (middle - start)/iterations;
        double utime = (end - middle)/iterations;
        
        java.text.NumberFormat nf = java.text.NumberFormat.getPercentInstance();
        logln("Test contains: " + message + ": Java: " + jtime
            + ", Utility: " + utime + ", u:j: " + nf.format(utime/jtime));
    }
    
    void CheckSpeed3(SortedSet iset, SortedSet jset, String message, int iterations) {
        boolean x;
        boolean y;
        
        // make sure code is loaded:
        x = iset.equals(jset);
        y = UnicodeSet.hasRelation(iset, UnicodeSet.EQUALS, jset);
        if (x != y) errln("FAIL equality comparison");

        
        double start = System.currentTimeMillis();
        for (int i = 0; i < iterations; ++i) {
            x |= iset.equals(jset);
        }
        double middle = System.currentTimeMillis();
        for (int i = 0; i < iterations; ++i) {
            y |= UnicodeSet.hasRelation(iset, UnicodeSet.EQUALS, jset);
        }
        double end = System.currentTimeMillis();
        
        double jtime = (middle - start)/iterations;
        double utime = (end - middle)/iterations;
        
        java.text.NumberFormat nf = java.text.NumberFormat.getPercentInstance();
        logln("Test equals:   " + message + ": Java: " + jtime
            + ", Utility: " + utime + ", u:j: " + nf.format(utime/jtime));
    }
    
    void pick(int bits, Object[] examples, SortedSet output) {
        output.clear();
        for (int k = 0; k < 32; ++k) {
            if (((1<<k) & bits) != 0) output.add(examples[k]);
        }
    }
    
    public static final String[] RELATION_NAME = {
        "both-are-null",
        "a-is-null", 
        "equals", 
        "is-contained-in",
        "b-is-null",
        "is-disjoint_with",
        "contains", 
        "any", };
        
    boolean dumbHasRelation(Collection A, int filter, Collection B) {
        Collection ab = new TreeSet(A);
        ab.retainAll(B);
        if (ab.size() > 0 && (filter & UnicodeSet.A_AND_B) == 0) return false; 
        
        // A - B size == A.size - A&B.size
        if (A.size() > ab.size() && (filter & UnicodeSet.A_NOT_B) == 0) return false; 
        
        // B - A size == B.size - A&B.size
        if (B.size() > ab.size() && (filter & UnicodeSet.B_NOT_A) == 0) return false; 

        
        return true;
    }    
    
    void checkSetRelation(SortedSet a, SortedSet b, String message) {
        for (int i = 0; i < 8; ++i) {
            
            boolean hasRelation = UnicodeSet.hasRelation(a, i, b);
            boolean dumbHasRelation = dumbHasRelation(a, i, b);
            
            logln(message + " " + hasRelation + ":\t" + a + "\t" + RELATION_NAME[i] + "\t" + b);
            
            if (hasRelation != dumbHasRelation) {
                errln("FAIL: " + 
                    message + " " + dumbHasRelation + ":\t" + a + "\t" + RELATION_NAME[i] + "\t" + b);
            }
        }
        logln("");
    }
    
   /**
    * Test the [:Latin:] syntax.
    */
    public void TestScriptSet() {
        UnicodeSet set = new UnicodeSet("[:Latin:]");

        expectContainment(set, "aA", CharsToUnicodeString("\\u0391\\u03B1"));

        UnicodeSet set2 = new UnicodeSet("[:Greek:]");
        expectContainment(set2, CharsToUnicodeString("\\u0391\\u03B1"), "aA");
        
        /* Jitterbug 1423 */
        UnicodeSet set3 = new UnicodeSet("[[:Common:][:Inherited:]]");
        expectContainment(set3, CharsToUnicodeString("\\U00003099\\U0001D169\\u0000"), "aA");

    }
    
    /**
     * Test the [:Latin:] syntax.
     */
    public void TestPropertySet() {
        UnicodeSet set = new UnicodeSet("[:Latin:]");
        expectContainment(set, "aA", "\u0391\u03B1");
        set = new UnicodeSet("[\\p{Greek}]");
        expectContainment(set, "\u0391\u03B1", "aA");
        set = new UnicodeSet("\\P{ GENERAL Category = upper case letter }");
        expectContainment(set, "abc", "ABC");
    }

    /**
     * Test cloning of UnicodeSet
     */
    public void TestClone() {
        UnicodeSet s = new UnicodeSet("[abcxyz]");
        UnicodeSet t = (UnicodeSet) s.clone();
        expectContainment(t, "abc", "def");
    }

    /**
     * Test the indexOf() and charAt() methods.
     */
    public void TestIndexOf() {
        UnicodeSet set = new UnicodeSet("[a-cx-y3578]");
        for (int i=0; i<set.size(); ++i) {
            int c = set.charAt(i);
            if (set.indexOf(c) != i) {
                errln("FAIL: charAt(" + i + ") = " + c +
                      " => indexOf() => " + set.indexOf(c));
            }
        }
        int c = set.charAt(set.size());
        if (c != -1) {
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
            
        	// AS LONG AS WE ARE HERE, check roundtrip
        	checkRoundTrip(bitsToSet(i));
            
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
    UnicodeSet bitsToSet(int a) {
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
    
    /**
     * Test function. Make sure that the sets have the right relation
     */
     
    void expectRelation(Object relationObj, Object set1Obj, Object set2Obj, String message) {
        int relation = ((Integer) relationObj).intValue();
        UnicodeSet set1 = (UnicodeSet) set1Obj;
        UnicodeSet set2 = (UnicodeSet) set2Obj;
        
        // by-the-by, check the iterator
        checkRoundTrip(set1);
        checkRoundTrip(set2);
        
        boolean contains = set1.containsAll(set2);
        boolean isContained = set2.containsAll(set1);
        boolean disjoint = set1.containsNone(set2);
        boolean equals = set1.equals(set2);
        
        UnicodeSet intersection = new UnicodeSet(set1).retainAll(set2);
        UnicodeSet minus12 = new UnicodeSet(set1).removeAll(set2);
        UnicodeSet minus21 = new UnicodeSet(set2).removeAll(set1);
        
        // test basic properties
        
        if (contains != (intersection.size() == set2.size())) {
            errln("FAIL contains1" + set1.toPattern(true) + ", " + set2.toPattern(true));
        }
          
        if (contains != (intersection.equals(set2))) {
            errln("FAIL contains2" + set1.toPattern(true) + ", " + set2.toPattern(true));
        }
          
        if (isContained != (intersection.size() == set1.size())) {
            errln("FAIL isContained1" + set1.toPattern(true) + ", " + set2.toPattern(true));
        }
          
        if (isContained != (intersection.equals(set1))) {
            errln("FAIL isContained2" + set1.toPattern(true) + ", " + set2.toPattern(true));
        }
          
        if ((contains && isContained) != equals) {
            errln("FAIL equals" + set1.toPattern(true) + ", " + set2.toPattern(true));
        }
          
        if (disjoint != (intersection.size() == 0)) {
            errln("FAIL disjoint" + set1.toPattern(true) + ", " + set2.toPattern(true));
        }
        
        // Now see if the expected relation is true
        int status = (minus12.size() != 0 ? 4 : 0)
          | (intersection.size() != 0 ? 2 : 0)
          | (minus21.size() != 0 ? 1 : 0);
        
        if (status != relation) {
            errln("FAIL relation incorrect" + message
              + "; desired = " + RELATION_NAME[relation]
              + "; found = " + RELATION_NAME[status]
              + "; set1 = " + set1.toPattern(true)
              + "; set2 = " + set2.toPattern(true)
              );
        }
    }
    
    /**
     * Basic consistency check for a few items.
     * That the iterator works, and that we can create a pattern and
     * get the same thing back
     */
    
    void checkRoundTrip(UnicodeSet s) {
        String pat = s.toPattern(false);
        UnicodeSet t = copyWithIterator(s, false);
        checkEqual(s, t, "iterator roundtrip");

        t = copyWithIterator(s, true); // try range
        checkEqual(s, t, "iterator roundtrip");
        
        t = new UnicodeSet(pat);
        checkEqual(s, t, "toPattern(false)");
        
        pat = s.toPattern(true);
        t = new UnicodeSet(pat);
        checkEqual(s, t, "toPattern(true)");
    }
    
    UnicodeSet copyWithIterator(UnicodeSet s, boolean withRange) {
    	UnicodeSet t = new UnicodeSet();
    	UnicodeSetIterator it = new UnicodeSetIterator(s);
    	if (withRange) {
    		while (it.nextRange()) {
    			if (it.codepoint == UnicodeSetIterator.IS_STRING) {
    				t.add(it.string);
    			} else {
    				t.add(it.codepoint, it.codepointEnd);
    			}
    		}
    	} else {
    		while (it.next()) {
    			if (it.codepoint == UnicodeSetIterator.IS_STRING) {
    				t.add(it.string);
    			} else {
    				t.add(it.codepoint);
    			}
    		}
    	}
    	return t;
    }
    
    boolean checkEqual(UnicodeSet s, UnicodeSet t, String message) {
        if (!s.equals(t)) {
            errln("FAIL " + message
              + "; source = " + s.toPattern(true)
              + "; result = " + t.toPattern(true)
              );
        	return false;
        }
        return true;
    }
    		
    
    /**
     * Expect the given set to contain the characters in charsIn and
     * to not contain those in charsOut.
     */
    void expectContainment(UnicodeSet set, String charsIn, String charsOut) {
        StringBuffer bad = new StringBuffer();
        if (charsIn != null) {
            for (int i=0; i<charsIn.length(); ++i) {
                int c = UTF16.charAt(charsIn,i);
                if(c>0xffff) i++;
                if (!set.contains(c)) {
                    UTF16.append(bad,c);
                }
            }
            if (bad.length() > 0) {
                errln(Utility.escape("FAIL: set " + set + " does not contain " + bad +
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
                errln(Utility.escape("FAIL: set " + set + " contains " + bad +
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

    void expectToPattern(UnicodeSet set,
                         String expPat,
                         String[] expStrings) {
        String pat = set.toPattern(true);
        if (pat.equals(expPat)) {
            logln("Ok:   toPattern() => \"" + pat + "\"");
        } else {
            errln("FAIL: toPattern() => \"" + pat + "\", expected \"" + expPat + "\"");
            return;
        }
        boolean in = true;
        for (int i=0; i<expStrings.length; ++i) {
            if (expStrings[i] == NOT) { // sic; pointer comparison
                in = false;
                continue;
            }
            // TODO
        }
    }

    void expectPairs(UnicodeSet set, String expectedPairs) {
        if (!getPairs(set).equals(expectedPairs)) {
            errln("FAIL: Expected pair list \"" +
                  Utility.escape(expectedPairs) + "\", got \"" +
                  Utility.escape(getPairs(set)) + "\"");
        }
    }
    static final String CharsToUnicodeString(String s) {
        return Utility.unescape(s);
    }
}
