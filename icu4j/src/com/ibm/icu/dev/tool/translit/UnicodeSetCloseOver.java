/*
**********************************************************************
* Copyright (c) 2003, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Alan Liu
* Created: February 11 2003
* Since: ICU 2.6
**********************************************************************
*/
package com.ibm.icu.dev.tools.translit;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.*;
import com.ibm.icu.impl.Utility;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

/**
 * This class produces the data tables used by the closeOver() method
 * of UnicodeSet.
 *
 * Whenever the Unicode database changes, this tool must be re-run
 * (AFTER the data file(s) underlying ICU4J are udpated).
 *
 * The output of this tool should then be pasted into the appropriate
 * files:
 *
 * ICU4J: com.ibm.icu.text.UnicodeSet.java
 * ICU4C: /icu/source/common/uniset.cpp
 */
class UnicodeSetCloseOver {

    public static void main(String[] args) {
        generateCaseData();
    }

    // Name of this class
    static final String ME = UnicodeSetCloseOver.class.getName();

    // Source code "do not edit" warning
    static final String WARNING = "MACHINE-GENERATED: Do not edit (see " + ME + ")";

    // Case folding options flag.  This must correspond to the options
    // used in UnicodeSet.closeOver() in Java and C++.
    static final boolean DEFAULT_CASE_MAP = true; // false for Turkish

    /**
     * Create a map of String => Set.  The String in this case is a
     * folded string for which
     * UCharacter.foldCase(folded. DEFAULT_CASE_MAP).equals(folded).
     * The Set contains all single-character strings x for which
     * UCharacter.foldCase(x, DEFAULT_CASE_MAP).equals(folded), as
     * well as folded itself.
     */
    static Map createCaseFoldEquivalencyClasses() {
        Map equivClasses = new HashMap();
        for (int i = 0; i <= 0x10FFFF; ++i) {
            int cat = UCharacter.getType(i);
            if (cat == Character.UNASSIGNED || cat == Character.PRIVATE_USE)
                continue;

            String cp = UTF16.valueOf(i);
            String folded = UCharacter.foldCase(cp, DEFAULT_CASE_MAP);
            if (folded.equals(cp)) continue;

            // At this point, have different case folding.  Add
            // the code point and its folded equivalent into the
            // equivalency class.
            TreeSet s = (TreeSet) equivClasses.get(folded);
            if (s == null) {
                s = new TreeSet();
                s.add(folded); // add the case fold result itself
                equivClasses.put(folded, s);
            }
            s.add(cp);
        }
        return equivClasses;
    }

    /**
     * Analyze the case fold equivalency classes.  Break them into two
     * groups: 'pairs', and 'nonpairs'.  Create a tally of the length
     * configurations of the nonpairs.
     *
     * Length configurations of equivalency classes, as of Unicode
     * 3.2.  Most of the classes (83%) have two single codepoints.
     * Here "112:28" means there are 28 equivalency classes with 2
     * single codepoints and one string of length 2.
     *
     * 11:656
     * 111:16
     * 1111:3
     * 112:28
     * 113:2
     * 12:31
     * 13:12
     * 22:38
     *
     * Note: This method does not count the frequencies of the
     * different length configurations (as shown above after ':'); it
     * merely records which configurations occur.
     *
     * @param pairs Accumulate equivalency classes that consist of
     * exactly two codepoints here.  This is 83+% of the classes.
     * E.g., {"a", "A"}.
     * @param nonpairs Accumulate other equivalency classes here, as
     * lists of strings.  E,g, {"st", "\uFB05", "\uFB06"}.
     * @param lengths Accumulate a list of unique length structures,
     * not including pairs.  Each length structure is represented by a
     * string of digits.  The digit string "12" means the equivalency
     * class contains a single code point and a string of length 2.
     * Typical contents of 'lengths': { "111", "1111", "112",
     * "113", "12", "13", "22" }.  Note the absence of "11".
     */
    static void analyzeCaseData(Map equivClasses,
                                StringBuffer pairs,
                                Vector nonpairs,
                                Vector lengths) {
        Iterator i = new TreeSet(equivClasses.keySet()).iterator();
        StringBuffer buf = new StringBuffer();
        while (i.hasNext()) {
            Object key = i.next();
            Vector v = new Vector((Set) equivClasses.get(key));
            if (v.size() == 2) {
                String a = (String) v.elementAt(0);
                String b = (String) v.elementAt(1);
                if (a.length() == 1 && b.length() == 1) {
                    pairs.append(a).append(b);
                    continue;
                    // Note that pairs are included in 'lengths'
                }
            }
            String[] a = new String[v.size()];
            v.toArray(a);
            nonpairs.add(a);
            int singleCount = 0;
            int stringCount = 0;
            // Make a string of the lengths, e.g., "111" means 3
            // single code points; "13" means a single code point
            // and a string of length 3.
            v.clear();
            for (int j=0; j<a.length; ++j) {
                v.add(new Integer(a[j].length()));
            }
            Collections.sort(v);
            buf.setLength(0);
            for (int j=0; j<v.size(); ++j) {
                buf.append(String.valueOf(v.elementAt(j)));
            }
            if (!lengths.contains(buf.toString())) {
                lengths.add(buf.toString());
            }
        }
    }

    static void generateCaseData() {

        Map equivClasses = createCaseFoldEquivalencyClasses();

        // Accumulate equivalency classes that consist of exactly
        // two codepoints here.  This is 83+% of the classes.
        // E.g., {"a", "A"}.
        StringBuffer pairs = new StringBuffer();

        // Accumulate other equivalency classes here, as lists
        // of strings.  E,g, {"st", "\uFB05", "\uFB06"}.
        Vector nonpairs = new Vector(); // contains String[]
        Vector lengths = new Vector(); // "111", "12", "22", etc.

        analyzeCaseData(equivClasses, pairs, nonpairs, lengths);

        //-------------------------------------------------------------
        // Emit Java source
        System.out.println("\n    // " + WARNING);
        System.out.println("    private static final String CASE_PAIRS =\n" +
                           Utility.formatForSource(pairs.toString()) +
                           ";\n");

        System.out.println("    // " + WARNING);
        System.out.println("    private static final String[][] CASE_NONPAIRS = {");
        for (int j=0; j<nonpairs.size(); ++j) {
            String[] a = (String[]) nonpairs.elementAt(j);
            System.out.print("        {");
            for (int k=0; k<a.length; ++k) {
                if (k != 0) System.out.print(", ");
                System.out.print(Utility.format1ForSource(a[k]));
            }
            System.out.println("},");
        }
        System.out.println("    };");

        //-------------------------------------------------------------
        // Emit C++ source

        // In C++, the pairs are again emitted in an array, but this
        // array is the final representation form -- it will not be
        // reprocessed into a hash.  It will be binary searched by
        // looking at the even elements [0], [2], [4], etc., and
        // ignoring the odd elements.  The even elements must contain
        // the folded members of the pairs.  That is, in the pair
        // {'A', 'a'}, the even element must be 'a', not 'A'.  Then a
        // code point to be located is first folded ('Y' => 'y') then
        // it binary searched against [0]='A', [2]='B', etc.  When a
        // match is found at k, the pair is [k], [k+1].

        // Sort the pairs.  They must be ordered by the folded element.
        // Store these as two-character strings, with charAt(0) being
        // the folded member of the pair.
        TreeSet sortPairs = new TreeSet(new Comparator() {
            public int compare(Object a, Object b) {
                return ((int) ((String) a).charAt(0)) -
                       ((int) ((String) b).charAt(0));
            }
            public boolean equals(Object obj) {
                return false;
            }
        });
        for (int i=0; i<pairs.length(); i+=2) {
            String a = String.valueOf(pairs.charAt(i));
            String b = String.valueOf(pairs.charAt(i+1));
            String folded = UCharacter.foldCase(a, DEFAULT_CASE_MAP);
            if (a.equals(folded)) {
                sortPairs.add(a + b);
            } else {
                sortPairs.add(b + a);
            }
        }

        // Emit the pairs
        System.out.println("\n// " + WARNING);
        System.out.println("static const UChar CASE_PAIRS[] = {");
        Iterator it = sortPairs.iterator();
        while (it.hasNext()) {
            System.out.print("    ");
            int n = 0;
            while (n++ < 5 && it.hasNext()) {
                String s = (String) it.next();
                //System.out.print((int) s.charAt(0) + "," +
                //                 (int) s.charAt(1) + ",");
                System.out.print("0x" + Utility.hex(s.charAt(0)) + ",0x" +
                                 Utility.hex(s.charAt(1)) + ",");
            }
            System.out.println();
        }
        System.out.println("};\n");

        // The non-pairs are encoded in the following way.  All the
        // single codepoints in each class are grouped together
        // followed by a zero.  Then each multi-character string is
        // added, followed by a zero.  Finally, another zero is added.
        // Some examples:
        //  {"iQ", "R"}           =>  [ 'R', 0, 'i', 'Q', 0, 0 ]
        //  {"S", "D", "F", "G"}  =>  [ 'S', 'D', 'F', 'G', 0, 0 ]
        //  {"jW", "jY"}          =>  [ 0, 'j', 'W', 0, 'j', 'Y', 0, 0 ]
        // The end-result is a short, flat array of UChar values that
        // can be used to initialize a UChar[] array in C.
        
        int maxLen = 0; // Maximum encoded length of any class, including zeros
        System.out.println("// " + WARNING);
        System.out.println("static const CaseEquivClass CASE_NONPAIRS[] = {");
        for (int j=0; j<nonpairs.size(); ++j) {
            int len = 0;
            String[] a = (String[]) nonpairs.elementAt(j);
            System.out.print("    {");
            // Emit single code points
            for (int k=0; k<a.length; ++k) {
                if (a[k].length() != 1) continue;
                //System.out.print((int) a[k].charAt(0) + ",");
                System.out.print("0x"+Utility.hex(a[k].charAt(0)) + ",");
                ++len;
            }
            System.out.print("0,  "); // End of single code points
            ++len;
            // Emit multi-character strings
            for (int k=0; k<a.length; ++k) {
                if (a[k].length() == 1) continue;
                for (int m=0; m<a[k].length(); ++m) {
                    //System.out.print((int) a[k].charAt(m) + ",");
                    System.out.print("0x"+Utility.hex(a[k].charAt(m)) + ",");
                    ++len;
                }
                System.out.print("0, "); // End of string
                ++len;
            }
            System.out.println("0},"); // End of equivalency class
            ++len;
            if (len > maxLen) maxLen = len;
        }
        System.out.println("};");

        // Make sure the CaseEquivClass data can fit.
        if (maxLen > 8) {
            throw new RuntimeException("Must adjust CaseEquivClass to accomodate " + maxLen + " UChars");
        }

        // Also make sure that we can map into this array using a
        // CompactByteArray.  We could do this check above, but we
        // keep it here, adjacent to the maxLen check.  We use one
        // value (-1 == 255) to indicate "no value."
        if (nonpairs.size() > 255) {
            throw new RuntimeException("Too many CASE_NONPAIRS array elements to be indexed by a CompactByteArray");
        }

        //-------------------------------------------------------------
        // Case-unique set:  All characters c for which closeOver(c)==c.

        UnicodeSet caseUnique = new UnicodeSet();
        for (int i = 0; i <= 0x10FFFF; ++i) {
            String cp = UTF16.valueOf(i);
            if (equivClasses.get(UCharacter.foldCase(cp, DEFAULT_CASE_MAP)) == null) {
                caseUnique.add(i);
            }
        }
        System.out.println("caseUnique = " + caseUnique.toPattern(true));
    }
}
