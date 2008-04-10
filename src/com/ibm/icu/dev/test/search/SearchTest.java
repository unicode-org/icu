/*
 *******************************************************************************
 * Copyright (C) 2000-2007, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

/**
 * Port From:   ICU4C v2.1 : collate/StringSearchTest
 * Source File: $ICU4CRoot/source/test/intltest/srchtest.cpp
 **/

package com.ibm.icu.dev.test.search;

import java.util.Locale;
import java.text.StringCharacterIterator;
import com.ibm.icu.dev.test.*;
import com.ibm.icu.text.*;

public class SearchTest extends TestFmwk {

    //inner class
    static class SearchData {
        SearchData(String text, String pattern, String coll, int strength, String breaker,
                   int[] offset, int[] size) {
            this.text = text;
            this.pattern = pattern;
            this.collator = coll;
            this.strength = strength;
            this.breaker = breaker;
            this.offset = offset;
            this.size = size;
        }
        String              text;
        String              pattern;
        String              collator;
        int                 strength;
        String              breaker;
        int[]               offset;
        int[]               size;
    }

    RuleBasedCollator m_en_us_;
    RuleBasedCollator m_fr_fr_;
    RuleBasedCollator m_de_;
    RuleBasedCollator m_es_;
    BreakIterator     m_en_wordbreaker_;
    BreakIterator     m_en_characterbreaker_;

    static SearchData[] BASIC = {
        new SearchData("xxxxxxxxxxxxxxxxxxxx",          "fisher",       null, Collator.TERTIARY, null, new int[] {-1},            new int[]{0}),
        new SearchData("silly spring string",           "string",       null, Collator.TERTIARY, null, new int[]{13, -1},         new int[]{6}),
        new SearchData("silly spring string string",    "string",       null, Collator.TERTIARY, null, new int[]{13, 20, -1},     new int[]{6, 6}),
        new SearchData("silly string spring string",    "string",       null, Collator.TERTIARY, null, new int[]{6, 20, -1},      new int[]{6, 6}),
        new SearchData("string spring string",          "string",       null, Collator.TERTIARY, null, new int[]{0, 14, -1},      new int[]{6, 6}),
        new SearchData("Scott Ganyo",                   "c",            null, Collator.TERTIARY, null, new int[]{1, -1},          new int[]{1}),
        new SearchData("Scott Ganyo",                   " ",            null, Collator.TERTIARY, null, new int[]{5, -1},          new int[]{1}),
        new SearchData("\u0300\u0325",                  "\u0300",       null, Collator.TERTIARY, null, new int[]{-1},             new int[]{0}),
        new SearchData("a\u0300\u0325",                 "\u0300",       null, Collator.TERTIARY, null, new int[]{-1},             new int[]{0}),
        new SearchData("a\u0300\u0325",                 "\u0300\u0325", null, Collator.TERTIARY, null, new int[]{1, -1},          new int[]{2}),
        new SearchData("a\u0300b",                      "\u0300",       null, Collator.TERTIARY, null, new int[]{1, -1},          new int[]{1}),
        new SearchData("\u00c9",                        "e",            null, Collator.PRIMARY,  null, new int[]{0, -1},          new int[]{1}),
        new SearchData(null,                            null,           null, Collator.TERTIARY, null, new int[]{-1},             new int[]{0})
    };

    SearchData BREAKITERATOREXACT[] = {
        new SearchData("foxy fox", "fox", null, Collator.TERTIARY, "characterbreaker", new int[] {0, 5, -1}, new int[] {3, 3}),
        new SearchData("foxy fox", "fox", null, Collator.TERTIARY, "wordbreaker", new int[] {5, -1}, new int[] {3}),
        new SearchData("This is a toe T\u00F6ne", "toe", "de", Collator.PRIMARY, "characterbreaker", new int[] {10, 14, -1}, new int[] {3, 2}),
        new SearchData("This is a toe T\u00F6ne", "toe", "de", Collator.PRIMARY, "wordbreaker", new int[] {10, -1}, new int[] {3}),
        new SearchData("Channel, another channel, more channels, and one last Channel", "Channel", "es", Collator.TERTIARY,
             "wordbreaker", new int[] {0, 54, -1}, new int[] {7, 7}),
        /* jitterbug 1745 */
        new SearchData("testing that \u00e9 does not match e", "e", null, Collator.TERTIARY,
            "characterbreaker", new int[] {1, 17, 30, -1}, new int[] {1, 1, 1}),
        new SearchData("testing that string ab\u00e9cd does not match e", "e", null, Collator.TERTIARY,
            "characterbreaker", new int[] {1, 28, 41, -1}, new int[] {1, 1, 1}),
        new SearchData("\u00c9", "e", "fr", Collator.PRIMARY,  "characterbreaker", new int[]{0, -1}, new int[]{1}),
        new SearchData(null, null, null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0})
    };

    SearchData BREAKITERATORCANONICAL[] = {
        new SearchData("foxy fox", "fox", null, Collator.TERTIARY, "characterbreaker", new int[] {0, 5, -1}, new int[] {3, 3}),
        new SearchData("foxy fox", "fox", null, Collator.TERTIARY, "wordbreaker", new int[] {5, -1}, new int[] {3}),
        new SearchData("This is a toe T\u00F6ne", "toe", "de", Collator.PRIMARY, "characterbreaker", new int[] {10, 14, -1}, new int[] {3, 2}),
        new SearchData("This is a toe T\u00F6ne", "toe", "de", Collator.PRIMARY, "wordbreaker", new int[] {10, -1}, new int[] {3}),
        new SearchData("Channel, another channel, more channels, and one last Channel", "Channel", "es", Collator.TERTIARY, "wordbreaker",
                       new int[] {0, 54, -1}, new int[] {7, 7}),
        /* jitterbug 1745 */
        new SearchData("testing that \u00e9 does not match e", "e", null, Collator.TERTIARY,
            "characterbreaker", new int[] {1, 17, 30, -1}, new int[] {1, 1, 1}),
        new SearchData("testing that string ab\u00e9cd does not match e", "e", null,
             Collator.TERTIARY, "characterbreaker", new int[] {1, 28, 41, -1}, new int[] {1, 1, 1}),
        new SearchData("\u00c9", "e", "fr", Collator.PRIMARY,  "characterbreaker", new int[]{0, -1}, new int[]{1}),
        new SearchData(null, null, null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0})
    };

    SearchData BASICCANONICAL[] = {
        new SearchData("xxxxxxxxxxxxxxxxxxxx", "fisher", null, Collator.TERTIARY, null, new int[] {-1}, new int [] {0}),
        new SearchData("silly spring string", "string", null, Collator.TERTIARY, null, new int[] {13, -1}, new int[] {6}),
        new SearchData("silly spring string string", "string", null, Collator.TERTIARY, null, new int[] {13, 20, -1}, new int[] {6, 6}),
        new SearchData("silly string spring string", "string", null, Collator.TERTIARY, null, new int[] {6, 20, -1}, new int[] {6, 6}),
        new SearchData("string spring string", "string", null, Collator.TERTIARY, null, new int[] {0, 14, -1}, new int[] {6, 6}),
        new SearchData("Scott Ganyo", "c", null, Collator.TERTIARY, null, new int[] {1, -1}, new int[] {1}),
        new SearchData("Scott Ganyo", " ", null, Collator.TERTIARY, null, new int[] {5, -1}, new int[] {1}),
        new SearchData("\u0300\u0325", "\u0300", null, Collator.TERTIARY, null, new int [] {0, -1}, new int[] {2}),
        new SearchData("a\u0300\u0325", "\u0300", null, Collator.TERTIARY, null, new int [] {1, -1}, new int[] {2}),
        new SearchData("a\u0300\u0325", "\u0300\u0325", null, Collator.TERTIARY, null, new int[] {1, -1}, new int[]{2}),
        new SearchData("a\u0300b", "\u0300", null, Collator.TERTIARY, null, new int[]{1, -1}, new int[] {1}),
        new SearchData("a\u0300\u0325b", "\u0300b", null, Collator.TERTIARY, null, new int[] {1, -1}, new int[] {3}),
        new SearchData("\u0325\u0300A\u0325\u0300", "\u0300A\u0300", null, Collator.TERTIARY, null, new int[] {0, -1}, new int[] {5}),
        new SearchData("\u0325\u0300A\u0325\u0300", "\u0325A\u0325", null, Collator.TERTIARY, null, new int[] {0, -1}, new int[] {5}),
        new SearchData("a\u0300\u0325b\u0300\u0325c \u0325b\u0300 \u0300b\u0325", "\u0300b\u0325", null, Collator.TERTIARY, null,
            new int[] {1, 12, -1}, new int[] {5, 3}),
        new SearchData("\u00c4\u0323", "A\u0323\u0308", null, Collator.TERTIARY, null, new int[] {0, -1}, new int[] {2}),
        new SearchData("\u0308\u0323", "\u0323\u0308", null, Collator.TERTIARY, null, new int[] {0, -1}, new int[] {2}),
        new SearchData(null, null, null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0})
    };

    SearchData COLLATOR[] = {
        /* english */
        new SearchData("fox fpx", "fox", null, Collator.TERTIARY, null, new int[] {0, -1}, new int[] {3}),
        /* tailored */
        new SearchData("fox fpx", "fox", null, Collator.PRIMARY, null, new int[] {0, 4, -1}, new int[] {3, 3}),
        new SearchData(null, null, null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0})
    };

    String TESTCOLLATORRULE = "& o,O ; p,P";
    String EXTRACOLLATIONRULE = " & ae ; \u00e4 & AE ; \u00c4 & oe ; \u00f6 & OE ; \u00d6 & ue ; \u00fc & UE ; \u00dc";


    SearchData COLLATORCANONICAL[] = {
        /* english */
        new SearchData("fox fpx", "fox", null, Collator.TERTIARY, null, new int[] {0, -1}, new int[] {3}),
        /* tailored */
        new SearchData("fox fpx", "fox", null, Collator.PRIMARY, null, new int[] {0, 4, -1}, new int[] {3, 3}),
        new SearchData(null, null, null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0})
    };

    SearchData COMPOSITEBOUNDARIES[] = {
        new SearchData("\u00C0", "A", null, Collator.TERTIARY, null, new int[] {0, -1}, new int[] {1}),
        new SearchData("A\u00C0C", "A", null, Collator.TERTIARY, null, new int[]  {0, 1, -1}, new int[]  {1, 1}),
        new SearchData("\u00C0A", "A", null, Collator.TERTIARY, null, new int[] {0, 1, -1}, new int[] {1, 1}),
        new SearchData("B\u00C0", "A", null, Collator.TERTIARY, null, new int[] {1, -1}, new int[] {1}),
        new SearchData("\u00C0B", "A", null, Collator.TERTIARY, null, new int[] {0, -1}, new int[] {1}),
        new SearchData("\u00C0", "\u0300", null, Collator.TERTIARY, null, new int[] {0, -1}, new int[] {1}),
        new SearchData("\u0300\u00C0", "\u0300", null, Collator.TERTIARY, null, new int[] {0, 1, -1}, new int[] {1, 1}),
        new SearchData("\u00C0\u0300", "\u0300", null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0}),
        /* A + 030A + 0301 */
        new SearchData("\u01FA", "\u01FA", null, Collator.TERTIARY, null, new int[] {0, -1}, new int[] {1}),
        new SearchData("\u01FA", "\u030A", null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0}),
        new SearchData("\u01FA", "A\u030A", null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0}),
        new SearchData("\u01FA", "\u030AA", null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0}),
        new SearchData("\u01FA", "\u0301", null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0}),
        new SearchData("\u01FA", "A\u0301", null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0}),
        new SearchData("\u01FA", "\u0301A", null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0}),
        new SearchData("\u01FA", "\u030A\u0301", null, Collator.TERTIARY, null, new int[] {0, -1}, new int[] {1}),
        new SearchData("A\u01FA", "A\u030A", null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0}),
        new SearchData("\u01FAA", "\u0301A", null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0}),
        new SearchData("\u0F73", "\u0F73", null, Collator.TERTIARY, null, new int[] {0, -1}, new int[] {1}),
        new SearchData("\u0F73", "\u0F71", null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0}),
        new SearchData("\u0F73", "\u0F72", null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0}),
        new SearchData("\u0F73", "\u0F71\u0F72", null, Collator.TERTIARY, null, new int[] {0, -1}, new int[] {1}),
        new SearchData("A\u0F73", "A\u0F71", null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0}),
        new SearchData("\u0F73A", "\u0F72A", null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0}),
        new SearchData(null, null, null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0})
    };

    SearchData COMPOSITEBOUNDARIESCANONICAL[] = {
        new SearchData("\u00C0", "A", null, Collator.TERTIARY, null, new int[] {0, -1}, new int[] {1}),
        new SearchData("A\u00C0C", "A", null, Collator.TERTIARY, null, new int[] {0, 1, -1}, new int[] {1, 1}),
        new SearchData("\u00C0A", "A", null, Collator.TERTIARY, null, new int[] {0, 1, -1}, new int[] {1, 1}),
        new SearchData("B\u00C0", "A", null, Collator.TERTIARY, null, new int[] {1, -1}, new int[] {1}),
        new SearchData("\u00C0B", "A", null, Collator.TERTIARY, null, new int[] {0, -1}, new int[] {1}),
        new SearchData("\u00C0", "\u0300", null, Collator.TERTIARY, null, new int[] {0, -1}, new int[] {1}),
        new SearchData("\u0300\u00C0", "\u0300", null, Collator.TERTIARY, null, new int[] {0, 1, -1}, new int[] {1, 1}),
        /* \u0300 blocked by \u0300 */
        new SearchData("\u00C0\u0300", "\u0300", null, Collator.TERTIARY, null, new int[] {0, -1}, new int[] {2}),
        /* A + 030A + 0301 */
        new SearchData("\u01FA", "\u01FA", null, Collator.TERTIARY, null, new int[] {0, -1}, new int[] {1}),
        new SearchData("\u01FA", "\u030A", null, Collator.TERTIARY, null, new int[] {0, -1}, new int[] {1}),
        new SearchData("\u01FA", "A\u030A", null, Collator.TERTIARY, null, new int[] {0, -1}, new int[] {1}),
        new SearchData("\u01FA", "\u030AA", null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0}),
        new SearchData("\u01FA", "\u0301", null, Collator.TERTIARY, null, new int[] {0, -1}, new int[] {1}),
        /* blocked accent */
        new SearchData("\u01FA", "A\u0301", null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0}),
        new SearchData("\u01FA", "\u0301A", null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0}),
        new SearchData("\u01FA", "\u030A\u0301", null, Collator.TERTIARY, null, new int[] {0, -1}, new int[] {1}),
        new SearchData("A\u01FA", "A\u030A", null, Collator.TERTIARY, null, new int[] {1, -1}, new int[] {1}),
        new SearchData("\u01FAA", "\u0301A", null, Collator.TERTIARY, null, new int[] {0, -1}, new int[] {2}),
        new SearchData("\u0F73", "\u0F73", null, Collator.TERTIARY, null, new int[] {0, -1}, new int[] {1}),
        new SearchData("\u0F73", "\u0F71", null, Collator.TERTIARY, null, new int[] {0, -1}, new int[] {1}),
        new SearchData("\u0F73", "\u0F72", null, Collator.TERTIARY, null, new int[] {0, -1}, new int[] {1}),
        new SearchData("\u0F73", "\u0F71\u0F72", null, Collator.TERTIARY, null, new int[] {0, -1}, new int[] {1}),
        new SearchData("A\u0F73", "A\u0F71", null, Collator.TERTIARY, null, new int[] {0, -1}, new int[] {2}),
        new SearchData("\u0F73A", "\u0F72A", null, Collator.TERTIARY, null, new int[] {0, -1}, new int[] {2}),
        new SearchData("\u01FA A\u0301\u030A A\u030A\u0301 A\u030A \u01FA", "A\u030A",
            null, Collator.TERTIARY, null, new int[] {0, 6, 10, 13, -1}, new int[] {1, 3, 2, 1}),
        new SearchData(null, null, null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0})
    };

    SearchData SUPPLEMENTARY[] = {
        /* 012345678901234567890123456789012345678901234567890012345678901234567890123456789012345678901234567890012345678901234567890123456789 */
        new SearchData("abc \uD800\uDC00 \uD800\uDC01 \uD801\uDC00 \uD800\uDC00abc abc\uD800\uDC00 \uD800\uD800\uDC00 \uD800\uDC00\uDC00",
            "\uD800\uDC00", null, Collator.TERTIARY, null, 
            new int[] {4, 13, 22, 26, 29, -1}, new int[] {2, 2, 2, 2, 2}),
        new SearchData("and\uD834\uDDB9this sentence", "\uD834\uDDB9", null, 
                       Collator.TERTIARY, null, new int[] {3, -1}, 
                       new int[] {2}),
        new SearchData("and \uD834\uDDB9 this sentence", " \uD834\uDDB9 ", 
                       null, Collator.TERTIARY, null, new int[] {3, -1}, 
                       new int[] {4}),
        new SearchData("and-\uD834\uDDB9-this sentence", "-\uD834\uDDB9-", 
                       null, Collator.TERTIARY, null, new int[] {3, -1}, 
                       new int[] {4}),
        new SearchData("and,\uD834\uDDB9,this sentence", ",\uD834\uDDB9,", 
                       null, Collator.TERTIARY, null, new int[] {3, -1}, 
                       new int[] {4}),
        new SearchData("and?\uD834\uDDB9?this sentence", "?\uD834\uDDB9?", 
                       null, Collator.TERTIARY, null, new int[] {3, -1}, 
                       new int[] {4}),
        new SearchData(null, null, null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0})
    };

    String CONTRACTIONRULE = "&z = ab/c < AB < X\u0300 < ABC < X\u0300\u0315";

    SearchData CONTRACTION[] = {
        /* common discontiguous */
        new SearchData("A\u0300\u0315", "\u0300", null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0}),
        new SearchData("A\u0300\u0315", "\u0300\u0315", null, Collator.TERTIARY, null, new int[] {1, -1}, new int[] {2}),
        /* contraction prefix */
        new SearchData("AB\u0315C", "A", null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0}),
        new SearchData("AB\u0315C", "AB", null, Collator.TERTIARY, null, new int[] {0, -1}, new int[] {2}),
        new SearchData("AB\u0315C", "\u0315", null, Collator.TERTIARY, null, new int[] {2, -1}, new int[] {1}),
        /* discontiguous problem here for backwards iteration.
        accents not found because discontiguous stores all information */
        new SearchData("X\u0300\u0319\u0315", "\u0319", null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0}),
         /* ends not with a contraction character */
        new SearchData("X\u0315\u0300D", "\u0300\u0315", null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0}),
        new SearchData("X\u0315\u0300D", "X\u0300\u0315", null, Collator.TERTIARY, null, new int[] {0, -1}, new int[] {3}),
        new SearchData("X\u0300\u031A\u0315D", "X\u0300", null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0}),
        /* blocked discontiguous */
        new SearchData("X\u0300\u031A\u0315D", "\u031A\u0315D", null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0}),
        new SearchData("ab", "z", null, Collator.TERTIARY, null, new int[] {0, -1}, new int[] {2}),
        new SearchData(null, null, null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0})
    };

    SearchData CONTRACTIONCANONICAL[] = {
        /* common discontiguous */
        new SearchData("A\u0300\u0315", "\u0300", null, Collator.TERTIARY, null, new int[] {1, -1}, new int[] {2}),
        new SearchData("A\u0300\u0315", "\u0300\u0315", null, Collator.TERTIARY, null, new int[] {1, -1}, new int[] {2}),
        /* contraction prefix */
        new SearchData("AB\u0315C", "A", null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0}),
        new SearchData("AB\u0315C", "AB", null, Collator.TERTIARY, null, new int[] {0, -1}, new int[] {2}),
        new SearchData("AB\u0315C", "\u0315", null, Collator.TERTIARY, null, new int[] {2, -1}, new int[] {1}),
        /* discontiguous problem here for backwards iteration.
        forwards gives 0, 4 but backwards give 1, 3 */
        /* {"X\u0300\u0319\u0315", "\u0319", null, Collator.TERTIARY, null, {0, -1},
        {4}}, */

         /* ends not with a contraction character */
        new SearchData("X\u0315\u0300D", "\u0300\u0315", null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0}),
        new SearchData("X\u0315\u0300D", "X\u0300\u0315", null, Collator.TERTIARY, null,
            new int[] {0, -1}, new int[] {3}),
        new SearchData("X\u0300\u031A\u0315D", "X\u0300", null, Collator.TERTIARY, null,
            new int[] {0, -1}, new int[] {4}),
        /* blocked discontiguous */
        new SearchData("X\u0300\u031A\u0315D", "\u031A\u0315D", null, Collator.TERTIARY, null,
            new int[] {1, -1}, new int[] {4}),
        new SearchData("ab", "z", null, Collator.TERTIARY, null, new int[] {0, -1}, new int[] {2}),
        new SearchData(null, null, null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0})
    };

    SearchData MATCH[] = {
        new SearchData("a busy bee is a very busy beeee", "bee", null, Collator.TERTIARY, null,
        new int[] {7, 26, -1}, new int[] {3, 3}),
        /* 012345678901234567890123456789012345678901234567890 */
        new SearchData("a busy bee is a very busy beeee with no bee life", "bee", null,
            Collator.TERTIARY, null, new int[] {7, 26, 40, -1}, new int[] {3, 3, 3}),
        new SearchData(null, null, null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0})
    };

    String IGNORABLERULE = "&a = \u0300";

    SearchData IGNORABLE[] = {
        new SearchData("\u0300\u0315 \u0300\u0315 ", "\u0300", null, Collator.PRIMARY, null,
            new int[] {0, 3, -1}, new int[] {2, 2}),
        new SearchData(null, null, null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0})
    };
    
    SearchData DIACTRICMATCH[] = {
        new SearchData("\u0061\u0061\u00E1", "\u0061\u00E1", null, Collator.SECONDARY, null,
            new int[] {1, -1}, new int[] {2}),   
        new SearchData("\u0020\u00C2\u0303\u0020\u0041\u0061\u1EAA\u0041\u0302\u0303\u00C2\u0303\u1EAB\u0061\u0302\u0303\u00E2\u0303\uD806\uDC01\u0300\u0020",
            "\u00C2\u0303", null, Collator.PRIMARY, null, new int[] {1, 4, 5, 6, 7, 10, 12, 13, 16,-1}, new int[] {2, 1, 1, 1, 3, 2, 1, 3, 2}),
        new SearchData("\u03BA\u03B1\u03B9\u0300\u0020\u03BA\u03B1\u1F76", "\u03BA\u03B1\u03B9", null, Collator.PRIMARY, null,
                new int[] {0, 5, -1}, new int[] {4, 3}),   
        new SearchData(null, null, null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0})
    };

    SearchData NORMCANONICAL[] = {
        new SearchData("\u0300\u0325", "\u0300", null, Collator.TERTIARY, null, new int[] {0, -1}, new int[] {2}),
        new SearchData("\u0300\u0325", "\u0325", null, Collator.TERTIARY, null, new int[] {0, -1}, new int[] {2}),
        new SearchData("a\u0300\u0325", "\u0325\u0300", null, Collator.TERTIARY, null, new int[] {1, -1},
            new int[] {2}),
        new SearchData("a\u0300\u0325", "\u0300\u0325", null, Collator.TERTIARY, null, new int[] {1, -1},
            new int[] {2}),
        new SearchData("a\u0300\u0325", "\u0325", null, Collator.TERTIARY, null, new int[] {1, -1}, new int[] {2}),
        new SearchData("a\u0300\u0325", "\u0300", null, Collator.TERTIARY, null, new int[] {1, -1}, new int[] {2}),
        new SearchData(null, null, null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0})
    };

    SearchData NORMEXACT[] = {
        new SearchData("a\u0300\u0325", "\u0325\u0300", null, Collator.TERTIARY, null, new int[] {1, -1}, new int[] {2}),
        new SearchData(null, null, null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0})
    };

    SearchData NONNORMEXACT[] = {
        new SearchData("a\u0300\u0325", "\u0325\u0300", null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0}),
        new SearchData(null, null, null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0})
    };

    SearchData OVERLAP[] = {
        new SearchData("abababab", "abab", null, Collator.TERTIARY, null, new int[] {0, 2, 4, -1}, new int[] {4, 4, 4}),
        new SearchData(null, null, null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0})
    };

    SearchData NONOVERLAP[] = {
        new SearchData("abababab", "abab", null, Collator.TERTIARY, null, new int[] {0, 4, -1}, new int[] {4, 4}),
        new SearchData(null, null, null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0})
    };

    SearchData OVERLAPCANONICAL[] = {
        new SearchData("abababab", "abab", null, Collator.TERTIARY, null, new int[] {0, 2, 4, -1},
                        new int[] {4, 4, 4}),
        new SearchData(null, null, null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0})
    };

    SearchData NONOVERLAPCANONICAL[] = {
        new SearchData("abababab", "abab", null, Collator.TERTIARY, null, new int[] {0, 4, -1}, new int[] {4, 4}),
        new SearchData(null, null, null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0})
    };

    SearchData PATTERNCANONICAL[] = {
        new SearchData("The quick brown fox jumps over the lazy foxes", "the", null,
                       Collator.PRIMARY, null, new int[] {0, 31, -1}, new int[] {3, 3}),
        new SearchData("The quick brown fox jumps over the lazy foxes", "fox", null,
                       Collator.PRIMARY, null, new int[] {16, 40, -1}, new int[] {3, 3}),
        new SearchData(null, null, null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0})
    };

    SearchData PATTERN[] = {
        new SearchData("The quick brown fox jumps over the lazy foxes", "the", null,
                       Collator.PRIMARY, null, new int[] {0, 31, -1}, new int[] {3, 3}),
        new SearchData("The quick brown fox jumps over the lazy foxes", "fox", null,
                       Collator.PRIMARY, null, new int[] {16, 40, -1}, new int[] {3, 3}),
        new SearchData(null, null, null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0})
    };

    SearchData STRENGTH[] = {
        /*012345678901234567890123456789012345678901234567890123456789*/
        new SearchData("The quick brown fox jumps over the lazy foxes", "fox", "en",
                       Collator.PRIMARY, null, new int[] {16, 40, -1}, new int[] {3, 3}),
        new SearchData("The quick brown fox jumps over the lazy foxes", "fox", "en",
                       Collator.PRIMARY, "wordbreaker", new int[] {16, -1}, new int[] {3}),
        new SearchData("blackbirds Pat p\u00E9ch\u00E9 p\u00EAche p\u00E9cher p\u00EAcher Tod T\u00F6ne black Tofu blackbirds Ton PAT toehold blackbird black-bird pat toe big Toe",
                       "peche", "fr", Collator.PRIMARY, null, new int[] {15, 21, 27, 34, -1}, new int[] {5, 5, 5, 5}),
        new SearchData("This is a toe T\u00F6ne", "toe", "de", Collator.PRIMARY, null,
                        new int[] {10, 14, -1}, new int[] {3, 2}),
        new SearchData("A channel, another CHANNEL, more Channels, and one last channel...", "channel", "es",
                        Collator.PRIMARY, null, new int[] {2, 19, 33, 56, -1}, new int[] {7, 7, 7, 7}),
        new SearchData(null, null, null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0})
    };

    SearchData STRENGTHCANONICAL[] = {
        /*012345678901234567890123456789012345678901234567890123456789 */
        new SearchData("The quick brown fox jumps over the lazy foxes", "fox", "en",
                       Collator.PRIMARY, null, new int[] {16, 40, -1}, new int[] {3, 3}),
        new SearchData("The quick brown fox jumps over the lazy foxes", "fox", "en",
                       Collator.PRIMARY, "wordbreaker", new int[] {16, -1}, new int[] {3}),
        new SearchData("blackbirds Pat p\u00E9ch\u00E9 p\u00EAche p\u00E9cher p\u00EAcher Tod T\u00F6ne black Tofu blackbirds Ton PAT toehold blackbird black-bird pat toe big Toe",
                       "peche", "fr", Collator.PRIMARY, null, new int[] {15, 21, 27, 34, -1}, new int[] {5, 5, 5, 5}),
        new SearchData("This is a toe T\u00F6ne", "toe", "de", Collator.PRIMARY, null,
                       new int[] {10, 14, -1}, new int[] {3, 2}),
        new SearchData("A channel, another CHANNEL, more Channels, and one last channel...", "channel", "es",
                       Collator.PRIMARY, null, new int[]{2, 19, 33, 56, -1}, new int[] {7, 7, 7, 7}),
        new SearchData(null, null, null, Collator.TERTIARY, null, new int[] {-1}, new int[]{0})
    };

    SearchData SUPPLEMENTARYCANONICAL[] = {
        /*012345678901234567890123456789012345678901234567890012345678901234567890123456789012345678901234567890012345678901234567890123456789 */
        new SearchData("abc \uD800\uDC00 \uD800\uDC01 \uD801\uDC00 \uD800\uDC00abc abc\uD800\uDC00 \uD800\uD800\uDC00 \uD800\uDC00\uDC00",
                       "\uD800\uDC00", null, Collator.TERTIARY, null, new int[] {4, 13, 22, 26, 29, -1},
                       new int[] {2, 2, 2, 2, 2}),
        new SearchData("and\uD834\uDDB9this sentence", "\uD834\uDDB9", null, 
                       Collator.TERTIARY, null, new int[] {3, -1}, 
                       new int[] {2}),
        new SearchData("and \uD834\uDDB9 this sentence", " \uD834\uDDB9 ", 
                       null, Collator.TERTIARY, null, new int[] {3, -1}, 
                       new int[] {4}),
        new SearchData("and-\uD834\uDDB9-this sentence", "-\uD834\uDDB9-", 
                       null, Collator.TERTIARY, null, new int[] {3, -1}, 
                       new int[] {4}),
        new SearchData("and,\uD834\uDDB9,this sentence", ",\uD834\uDDB9,", 
                       null, Collator.TERTIARY, null, new int[] {3, -1}, 
                       new int[] {4}),
        new SearchData("and?\uD834\uDDB9?this sentence", "?\uD834\uDDB9?", 
                       null, Collator.TERTIARY, null, new int[] {3, -1}, 
                       new int[] {4}),
        new SearchData(null, null, null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0})
    };

    static SearchData VARIABLE[] = {
        /*012345678901234567890123456789012345678901234567890123456789*/
        new SearchData("blackbirds black blackbirds blackbird black-bird", "blackbird", null, Collator.TERTIARY,   null,
        new int[] {0, 17, 28, 38, -1}, new int[] {9, 9, 9, 10}),

        /* to see that it doesn't go into an infinite loop if the start of text
        is a ignorable character */
        new SearchData(" on",                                              "go",        null, Collator.TERTIARY,   null,
                       new int[] {-1}, new int[]{0}),
        new SearchData("abcdefghijklmnopqrstuvwxyz",                       "   ",       null, Collator.PRIMARY,    null,
                        new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1},
                        new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),

        /* testing tightest match */
        new SearchData(" abc  a bc   ab c    a  bc     ab  c",             "abc",       null, Collator.QUATERNARY, null,
                       new int[]{1, -1}, new int[] {3}),
        /*012345678901234567890123456789012345678901234567890123456789 */
        new SearchData(" abc  a bc   ab c    a  bc     ab  c",             "abc",       null, Collator.SECONDARY,  null,
                       new int[] {1, 6, 13, 21, 31, -1}, new int[] {3, 4, 4, 5, 5}),

        /* totally ignorable text */
        new SearchData("           ---------------",                       "abc",       null, Collator.SECONDARY,  null,
                       new int[] {-1}, new int[] {0}),
        new SearchData(null, null, null, Collator.TERTIARY, null, new int[] {-1}, new int[] {0})
    };

    static SearchData TEXTCANONICAL[] = {
        new SearchData("the foxy brown fox",                               "fox",       null, Collator.TERTIARY,   null,
                       new int[] {4, 15, -1}, new int[] {3, 3}),
        new SearchData("the quick brown fox",                              "fox",       null, Collator.TERTIARY,   null,
                       new int[] {16, -1}, new int[]{3}),
        new SearchData(null, null, null, Collator.TERTIARY,null, new int[] {-1}, new int[] {0})
    };

    /**
     * Constructor
     */
    public SearchTest()
    {

    }

    protected void init()throws Exception{
        m_en_us_ = (RuleBasedCollator)Collator.getInstance(Locale.US);
        m_fr_fr_ = (RuleBasedCollator)Collator.getInstance(Locale.FRANCE);
        m_de_ = (RuleBasedCollator)Collator.getInstance(new Locale("de", "DE"));
        m_es_ = (RuleBasedCollator)Collator.getInstance(new Locale("es", "ES"));
        m_en_wordbreaker_ = BreakIterator.getWordInstance();
        m_en_characterbreaker_ = BreakIterator.getCharacterInstance();
        String rules = m_de_.getRules() + EXTRACOLLATIONRULE;
        m_de_ = new RuleBasedCollator(rules);
        rules = m_es_.getRules() + EXTRACOLLATIONRULE;
        m_es_ = new RuleBasedCollator(rules);

    }
    public static void main(String[] args) throws Exception {
        new SearchTest().run(args);
        // new SearchTest().TestContraction();
    }

    RuleBasedCollator getCollator(String collator) {
        if (collator == null) {
            return m_en_us_;
        } if (collator.equals("fr")) {
            return m_fr_fr_;
        } else if (collator.equals("de")) {
            return m_de_;
        } else if (collator.equals("es")) {
            return m_es_;
        } else {
            return m_en_us_;
        }
    }

    BreakIterator getBreakIterator(String breaker) {
        if (breaker == null) {
            return null;
        } if (breaker.equals("wordbreaker")) {
            return m_en_wordbreaker_;
        } else {
            return m_en_characterbreaker_;
        }
    }

    boolean assertCanonicalEqual(SearchData search) {
        Collator      collator = getCollator(search.collator);
        BreakIterator breaker  = getBreakIterator(search.breaker);
        StringSearch  strsrch;

        String text = search.text;
        String  pattern = search.pattern;

        if (breaker != null) {
            breaker.setText(text);
        }
        collator.setStrength(search.strength);
        try {
            strsrch = new StringSearch(pattern, new StringCharacterIterator(text), (RuleBasedCollator)collator, breaker);
            strsrch.setCanonical(true);
        } catch (Exception e) {
            errln("Error opening string search" + e.getMessage());
            return false;
        }

        if (!assertEqualWithStringSearch(strsrch, search)) {
            collator.setStrength(Collator.TERTIARY);
            return false;
        }
        collator.setStrength(Collator.TERTIARY);
        return true;
    }

    boolean assertEqual(SearchData search) {
        Collator      collator = getCollator(search.collator);
        BreakIterator breaker  = getBreakIterator(search.breaker);
        StringSearch  strsrch;

        String text = search.text;
        String  pattern = search.pattern;

        if (breaker != null) {
            breaker.setText(text);
        }
        collator.setStrength(search.strength);
        try {
            strsrch = new StringSearch(pattern, new StringCharacterIterator(text), (RuleBasedCollator)collator, breaker);
        } catch (Exception e) {
            errln("Error opening string search " + e.getMessage());
            return false;
        }

        if (!assertEqualWithStringSearch(strsrch, search)) {
            collator.setStrength(Collator.TERTIARY);
            return false;
        }
        collator.setStrength(Collator.TERTIARY);
        return true;
    }

    boolean assertEqualWithAttribute(SearchData search, boolean canonical, boolean overlap) {
        Collator      collator = getCollator(search.collator);
        BreakIterator breaker  = getBreakIterator(search.breaker);
        StringSearch  strsrch;

        String text = search.text;
        String  pattern = search.pattern;

        if (breaker != null) {
            breaker.setText(text);
        }
        collator.setStrength(search.strength);
        try {
            strsrch = new StringSearch(pattern, new StringCharacterIterator(text), (RuleBasedCollator)collator, breaker);
            strsrch.setCanonical(canonical);
            strsrch.setOverlapping(overlap);
        } catch (Exception e) {
            errln("Error opening string search " + e.getMessage());
            return false;
        }

        if (!assertEqualWithStringSearch(strsrch, search)) {
            collator.setStrength(Collator.TERTIARY);
            return false;
        }
        collator.setStrength(Collator.TERTIARY);
        return true;
    }

    boolean assertEqualWithStringSearch(StringSearch strsrch, SearchData search) {
        int           count       = 0;
        int   matchindex  = search.offset[count];
        String matchtext;

        if (strsrch.getMatchStart() != SearchIterator.DONE ||
            strsrch.getMatchLength() != 0) {
            errln("Error with the initialization of match start and length");
        }
        // start of following matches
        while (matchindex >= 0) {
            int matchlength = search.size[count];
            strsrch.next();
            //int x = strsrch.getMatchStart();
            if (matchindex != strsrch.getMatchStart() ||
                matchlength != strsrch.getMatchLength()) {
                errln("Text: " + search.text);
                errln("Pattern: " + strsrch.getPattern());
                errln("Error following match found at " + strsrch.getMatchStart() + ", " + strsrch.getMatchLength());
                return false;
            }
            count ++;

            matchtext = strsrch.getMatchedText();
            String targetText = search.text;
            if (matchlength > 0 &&
                targetText.substring(matchindex, matchindex + matchlength).compareTo(matchtext) != 0) {
                errln("Error getting following matched text");
            }

            matchindex = search.offset[count];
        }
        strsrch.next();
        if (strsrch.getMatchStart() != SearchIterator.DONE ||
            strsrch.getMatchLength() != 0) {
                errln("Text: " + search.text);
                errln("Pattern: " + strsrch.getPattern());
                errln("Error following match found at " + strsrch.getMatchStart() + ", " + strsrch.getMatchLength());
                return false;
        }
        // start of preceding matches
        count = count == 0 ? 0 : count - 1;
        matchindex = search.offset[count];
        while (matchindex >= 0) {
            int matchlength = search.size[count];
            strsrch.previous();
            if (matchindex != strsrch.getMatchStart() ||
                matchlength != strsrch.getMatchLength()) {
                errln("Text: " + search.text);
                errln("Pattern: " + strsrch.getPattern());
                errln("Error following match found at " + strsrch.getMatchStart() + ", " + strsrch.getMatchLength());
                return false;
            }

            matchtext = strsrch.getMatchedText();
            String targetText = search.text;
            if (matchlength > 0 &&
                targetText.substring(matchindex, matchindex + matchlength).compareTo(matchtext) != 0) {
                errln("Error getting following matched text");
            }

            matchindex = count > 0 ? search.offset[count - 1] : -1;
            count --;
        }
        strsrch.previous();
        if (strsrch.getMatchStart() != SearchIterator.DONE ||
            strsrch.getMatchLength() != 0) {
                errln("Text: " + search.text);
                errln("Pattern: " + strsrch.getPattern());
                errln("Error following match found at " + strsrch.getMatchStart() + ", " + strsrch.getMatchLength());
                return false;
        }
        return true;
    }

    public void TestConstructor()
    {
        String pattern = "pattern";
        String text = "text";
        StringCharacterIterator textiter = new StringCharacterIterator(text);
        Collator defaultcollator = Collator.getInstance();
        BreakIterator breaker = BreakIterator.getCharacterInstance();
        breaker.setText(text);
        StringSearch search = new StringSearch(pattern, text);
        if (!search.getPattern().equals(pattern)
            || !search.getTarget().equals(textiter)
            || !search.getCollator().equals(defaultcollator)
            /*|| !search.getBreakIterator().equals(breaker)*/) {
            errln("StringSearch(String, String) error");
        }
        search = new StringSearch(pattern, textiter, m_fr_fr_);
        if (!search.getPattern().equals(pattern)
            || !search.getTarget().equals(textiter)
            || !search.getCollator().equals(m_fr_fr_)
            /*|| !search.getBreakIterator().equals(breaker)*/) {
            errln("StringSearch(String, StringCharacterIterator, "
                  + "RuleBasedCollator) error");
        }
        Locale de = new Locale("de", "DE");
        breaker = BreakIterator.getCharacterInstance(de);
        breaker.setText(text);
        search = new StringSearch(pattern, textiter, de);
        if (!search.getPattern().equals(pattern)
            || !search.getTarget().equals(textiter)
            || !search.getCollator().equals(Collator.getInstance(de))
            /*|| !search.getBreakIterator().equals(breaker)*/) {
            errln("StringSearch(String, StringCharacterIterator, Locale) "
                  + "error");
        }

        search = new StringSearch(pattern, textiter, m_fr_fr_,
                                  m_en_wordbreaker_);
        if (!search.getPattern().equals(pattern)
            || !search.getTarget().equals(textiter)
            || !search.getCollator().equals(m_fr_fr_)
            || !search.getBreakIterator().equals(m_en_wordbreaker_)) {
            errln("StringSearch(String, StringCharacterIterator, Locale) "
                  + "error");
        }
    }

    public void TestBasic() {
        int count = 0;
        while (BASIC[count].text != null) {
            if (!assertEqual(BASIC[count])) {
                errln("Error at test number " + count);
            }
            count ++;
        }
    }

    public void TestBreakIterator() {

        String text = BREAKITERATOREXACT[0].text;
        String pattern = BREAKITERATOREXACT[0].pattern;
        StringSearch strsrch = null;
        try {
            strsrch = new StringSearch(pattern, new StringCharacterIterator(text), m_en_us_, null);
        } catch (Exception e) {
            errln("Error opening string search");
            return;
        }

        strsrch.setBreakIterator(null);
        if (strsrch.getBreakIterator() != null) {
            errln("Error usearch_getBreakIterator returned wrong object");
        }

        strsrch.setBreakIterator(m_en_characterbreaker_);
        if (!strsrch.getBreakIterator().equals(m_en_characterbreaker_)) {
            errln("Error usearch_getBreakIterator returned wrong object");
        }

        strsrch.setBreakIterator(m_en_wordbreaker_);
        if (!strsrch.getBreakIterator().equals(m_en_wordbreaker_)) {
            errln("Error usearch_getBreakIterator returned wrong object");
        }

        int count = 0;
        while (count < 4) {
            // special purposes for tests numbers 0-3
            SearchData        search   = BREAKITERATOREXACT[count];
            RuleBasedCollator collator = getCollator(search.collator);
            BreakIterator     breaker  = getBreakIterator(search.breaker);
                  //StringSearch      strsrch;

            text = search.text;
            pattern = search.pattern;
            if (breaker != null) {
                breaker.setText(text);
            }
            collator.setStrength(search.strength);
            strsrch = new StringSearch(pattern, new StringCharacterIterator(text), collator, breaker);
            if (strsrch.getBreakIterator() != breaker) {
                errln("Error setting break iterator");
            }
            if (!assertEqualWithStringSearch(strsrch, search)) {
                collator.setStrength(Collator.TERTIARY);
            }
            search   = BREAKITERATOREXACT[count + 1];
            breaker  = getBreakIterator(search.breaker);
            if (breaker != null) {
                breaker.setText(text);
            }
            strsrch.setBreakIterator(breaker);
            if (strsrch.getBreakIterator() != breaker) {
                errln("Error setting break iterator");
            }
            strsrch.reset();
            if (!assertEqualWithStringSearch(strsrch, search)) {
                 errln("Error at test number " + count);
            }
            count += 2;
        }
        count = 0;
        while (BREAKITERATOREXACT[count].text != null) {
            if (!assertEqual(BREAKITERATOREXACT[count])) {
                errln("Error at test number " + count);
            }
             count++;
        }
    }

    public void TestBreakIteratorCanonical() {
        int        count  = 0;
        while (count < 4) {
            // special purposes for tests numbers 0-3
            SearchData     search   = BREAKITERATORCANONICAL[count];

            String text = search.text;
            String pattern = search.pattern;
            RuleBasedCollator collator = getCollator(search.collator);
            collator.setStrength(search.strength);

            BreakIterator breaker = getBreakIterator(search.breaker);
            StringSearch  strsrch = null;
            try {
                strsrch = new StringSearch(pattern, new StringCharacterIterator(text), collator, breaker);
            } catch (Exception e) {
                errln("Error creating string search data");
                return;
            }
            strsrch.setCanonical(true);
            if (!strsrch.getBreakIterator().equals(breaker)) {
                errln("Error setting break iterator");
                return;
            }
            if (!assertEqualWithStringSearch(strsrch, search)) {
                collator.setStrength(Collator.TERTIARY);
                return;
            }
            search  = BREAKITERATOREXACT[count + 1];
            breaker = getBreakIterator(search.breaker);
            breaker.setText(strsrch.getTarget());
            strsrch.setBreakIterator(breaker);
            if (!strsrch.getBreakIterator().equals(breaker)) {
                errln("Error setting break iterator");
                return;
            }
            strsrch.reset();
            strsrch.setCanonical(true);
            if (!assertEqualWithStringSearch(strsrch, search)) {
                 errln("Error at test number " + count);
                 return;
            }
            count += 2;
        }
        count = 0;
        while (BREAKITERATORCANONICAL[count].text != null) {
             if (!assertEqual(BREAKITERATORCANONICAL[count])) {
                 errln("Error at test number " + count);
                 return;
             }
             count++;
        }
    }

    public void TestCanonical() {
        int count = 0;
        while (BASICCANONICAL[count].text != null) {
            if (!assertCanonicalEqual(BASICCANONICAL[count])) {
                errln("Error at test number " + count);
            }
            count ++;
        }
    }

    public void TestCollator() {
        // test collator that thinks "o" and "p" are the same thing
        String text = COLLATOR[0].text;
        String pattern  = COLLATOR[0].pattern;
        StringSearch strsrch = null;
        try {
            strsrch = new StringSearch(pattern, new StringCharacterIterator(text), m_en_us_, null);
        } catch (Exception e) {
            errln("Error opening string search ");
            return;
        }
        if (!assertEqualWithStringSearch(strsrch, COLLATOR[0])) {
            return;
        }
        String rules = TESTCOLLATORRULE;
        RuleBasedCollator tailored = null;
        try {
            tailored = new RuleBasedCollator(rules);
            tailored.setStrength(COLLATOR[1].strength);
        } catch (Exception e) {
            errln("Error opening rule based collator ");
            return;
        }

        strsrch.setCollator(tailored);
        if (!strsrch.getCollator().equals(tailored)) {
            errln("Error setting rule based collator");
        }
        strsrch.reset();
        if (!assertEqualWithStringSearch(strsrch, COLLATOR[1])) {
            return;
        }
        strsrch.setCollator(m_en_us_);
        strsrch.reset();
        if (!strsrch.getCollator().equals(m_en_us_)) {
            errln("Error setting rule based collator");
        }
        if (!assertEqualWithStringSearch(strsrch, COLLATOR[0])) {
           errln("Error searching collator test");
        }
    }

    public void TestCollatorCanonical() {
        /* test collator that thinks "o" and "p" are the same thing */
        String text = COLLATORCANONICAL[0].text;
        String pattern = COLLATORCANONICAL[0].pattern;

        StringSearch strsrch = null;
        try {
            strsrch = new StringSearch(pattern, new StringCharacterIterator(text), m_en_us_, null);
            strsrch.setCanonical(true);
        } catch (Exception e) {
            errln("Error opening string search ");
        }

        if (!assertEqualWithStringSearch(strsrch, COLLATORCANONICAL[0])) {
            return;
        }

        String rules = TESTCOLLATORRULE;
        RuleBasedCollator tailored = null;
        try {
            tailored = new RuleBasedCollator(rules);
            tailored.setStrength(COLLATORCANONICAL[1].strength);
            tailored.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        } catch (Exception e) {
            errln("Error opening rule based collator ");
        }

        strsrch.setCollator(tailored);
        if (!strsrch.getCollator().equals(tailored)) {
            errln("Error setting rule based collator");
        }
        strsrch.reset();
        strsrch.setCanonical(true);
        if (!assertEqualWithStringSearch(strsrch, COLLATORCANONICAL[1])) {
        	logln("COLLATORCANONICAL[1] failed");  // Error should already be reported.
        }
        strsrch.setCollator(m_en_us_);
        strsrch.reset();
        if (!strsrch.getCollator().equals(m_en_us_)) {
            errln("Error setting rule based collator");
        }
        if (!assertEqualWithStringSearch(strsrch, COLLATORCANONICAL[0])) {
        	logln("COLLATORCANONICAL[0] failed");  // Error should already be reported.
        }
    }

    public void TestCompositeBoundaries() {
        int count = 0;
        while (COMPOSITEBOUNDARIES[count].text != null) {
            // logln("composite " + count);
            if (!assertEqual(COMPOSITEBOUNDARIES[count])) {
                errln("Error at test number " + count);
            }
            count++;
        }
    }

    public void TestCompositeBoundariesCanonical() {
        int count = 0;
        while (COMPOSITEBOUNDARIESCANONICAL[count].text != null) {
            // logln("composite " + count);
            if (!assertCanonicalEqual(COMPOSITEBOUNDARIESCANONICAL[count])) {
                errln("Error at test number " + count);
            }
            count++;
        }
    }

    public void TestContraction() {
        String rules = CONTRACTIONRULE;
        RuleBasedCollator collator = null;
        try {
            collator = new RuleBasedCollator(rules);
            collator.setStrength(Collator.TERTIARY);
            collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        } catch (Exception e) {
            errln("Error opening collator ");
        }
        String text = "text";
        String pattern = "pattern";
        StringSearch strsrch = null;
        try {
            strsrch = new StringSearch(pattern, new StringCharacterIterator(text), collator, null);
        } catch (Exception e) {
            errln("Error opening string search ");
        }

        int count = 0;
        while (CONTRACTION[count].text != null) {
            text = CONTRACTION[count].text;
            pattern = CONTRACTION[count].pattern;
            strsrch.setTarget(new StringCharacterIterator(text));
            strsrch.setPattern(pattern);
            if (!assertEqualWithStringSearch(strsrch, CONTRACTION[count])) {
                errln("Error at test number " + count);
            }
            count++;
        }
    }

    public void TestContractionCanonical() {
        String rules = CONTRACTIONRULE;
        RuleBasedCollator collator = null;
        try {
            collator = new RuleBasedCollator(rules);
            collator.setStrength(Collator.TERTIARY);
            collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        } catch (Exception e) {
            errln("Error opening collator ");
        }
        String text = "text";
        String pattern = "pattern";
        StringSearch strsrch = null;
        try {
            strsrch = new StringSearch(pattern, new StringCharacterIterator(text), collator, null);
            strsrch.setCanonical(true);
        } catch (Exception e) {
            errln("Error opening string search");
        }

        int count = 0;
        while (CONTRACTIONCANONICAL[count].text != null) {
            text = CONTRACTIONCANONICAL[count].text;
            pattern = CONTRACTIONCANONICAL[count].pattern;
            strsrch.setTarget(new StringCharacterIterator(text));
            strsrch.setPattern(pattern);
            if (!assertEqualWithStringSearch(strsrch, CONTRACTIONCANONICAL[count])) {
                errln("Error at test number " + count);
            }
            count++;
        }
    }

    public void TestGetMatch() {
        SearchData search = MATCH[0];
        String text = search.text;
        String pattern = search.pattern;

        StringSearch strsrch = null;
        try {
            strsrch = new StringSearch(pattern, new StringCharacterIterator(text), m_en_us_, null);
        } catch (Exception e) {
            errln("Error opening string search ");
            return;
        }

        int           count      = 0;
        int   matchindex = search.offset[count];
        String matchtext;
        while (matchindex >= 0) {
            int matchlength = search.size[count];
            strsrch.next();
            if (matchindex != strsrch.getMatchStart() ||
                matchlength != strsrch.getMatchLength()) {
                errln("Text: " + search.text);
                errln("Pattern: " + strsrch.getPattern());
                errln("Error match found at " + strsrch.getMatchStart() + ", " + strsrch.getMatchLength());
                return;
            }
            count++;

            matchtext = strsrch.getMatchedText();
            if (matchtext.length() != matchlength){
                errln("Error getting match text");
            }
            matchindex = search.offset[count];
        }
        strsrch.next();
        if (strsrch.getMatchStart()  != StringSearch.DONE ||
            strsrch.getMatchLength() != 0) {
            errln("Error end of match not found");
        }
        matchtext = strsrch.getMatchedText();
        if (matchtext != null) {
            errln("Error getting null matches");
        }
    }

    public void TestGetSetAttribute() {
        String  pattern = "pattern";
        String  text = "text";
        StringSearch  strsrch = null;
        try {
            strsrch = new StringSearch(pattern, new StringCharacterIterator(text), m_en_us_, null);
        } catch (Exception e) {
            errln("Error opening search");
            return;
        }

        if (strsrch.isOverlapping()) {
            errln("Error default overlaping should be false");
        }
        strsrch.setOverlapping(true);
        if (!strsrch.isOverlapping()) {
            errln("Error setting overlap true");
        }
        strsrch.setOverlapping(false);
        if (strsrch.isOverlapping()) {
            errln("Error setting overlap false");
        }

        strsrch.setCanonical(true);
        if (!strsrch.isCanonical()) {
            errln("Error setting canonical match true");
        }
        strsrch.setCanonical(false);
        if (strsrch.isCanonical()) {
            errln("Error setting canonical match false");
        }

    }

    public void TestGetSetOffset() {
        String  pattern = "1234567890123456";
        String  text  = "12345678901234567890123456789012";
        StringSearch  strsrch = null;
        try {
            strsrch = new StringSearch(pattern, new StringCharacterIterator(text), m_en_us_, null);
        } catch (Exception e) {
            errln("Error opening search");

            return;
        }

        /* testing out of bounds error */
        try {
            strsrch.setIndex(-1);
            errln("Error expecting set offset error");
        } catch (IndexOutOfBoundsException e) {
        	logln("PASS: strsrch.setIndex(-1) failed as expected");
       	}

        try {
            strsrch.setIndex(128);
            errln("Error expecting set offset error");
        } catch (IndexOutOfBoundsException e) {
        	logln("PASS: strsrch.setIndex(128) failed as expected");
       	}

        int index   = 0;
        while (BASIC[index].text != null) {
            SearchData  search      = BASIC[index ++];

            text =search.text;
            pattern = search.pattern;
            strsrch.setTarget(new StringCharacterIterator(text));
            strsrch.setPattern(pattern);
            strsrch.getCollator().setStrength(search.strength);
            strsrch.reset();

            int count = 0;
            int matchindex  = search.offset[count];

            while (matchindex >= 0) {
                int matchlength = search.size[count];
                strsrch.next();
                if (matchindex != strsrch.getMatchStart() ||
                    matchlength != strsrch.getMatchLength()) {
                    errln("Text: " + text);
                    errln("Pattern: " + strsrch.getPattern());
                    errln("Error match found at " + strsrch.getMatchStart() + ", " + strsrch.getMatchLength());
                    return;
                }
                matchindex = search.offset[count + 1] == -1 ? -1 :
                             search.offset[count + 2];
                if (search.offset[count + 1] != -1) {
                    strsrch.setIndex(search.offset[count + 1] + 1);
                    if (strsrch.getIndex() != search.offset[count + 1] + 1) {
                        errln("Error setting offset\n");
                        return;
                    }
                }

                count += 2;
            }
            strsrch.next();
            if (strsrch.getMatchStart() != StringSearch.DONE) {
                errln("Text: " + text);
                errln("Pattern: " + strsrch.getPattern());
                errln("Error match found at " + strsrch.getMatchStart() + ", " + strsrch.getMatchLength());
                return;
            }
        }
        strsrch.getCollator().setStrength(Collator.TERTIARY);
    }

    public void TestGetSetOffsetCanonical() {

        String  text = "text";
        String  pattern = "pattern";
        StringSearch  strsrch = null;
        try {
            strsrch = new StringSearch(pattern, new StringCharacterIterator(text), m_en_us_, null);
        } catch (Exception e) {
            errln("Fail to open StringSearch!");
            return;
        }
        strsrch.setCanonical(true);
        /* testing out of bounds error */
        try {
            strsrch.setIndex(-1);
            errln("Error expecting set offset error");
        } catch (IndexOutOfBoundsException e) {
        	logln("PASS: strsrch.setIndex(-1) failed as expected");
       	}
        try {
            strsrch.setIndex(128);
            errln("Error expecting set offset error");
        } catch (IndexOutOfBoundsException e) {
        	logln("PASS: strsrch.setIndex(128) failed as expected");
       	}

        int   index   = 0;
        while (BASICCANONICAL[index].text != null) {
            SearchData  search      = BASICCANONICAL[index ++];
            if (BASICCANONICAL[index].text == null) {
                // skip the last one
                break;
            }

            text = search.text;
            pattern = search.pattern;
            strsrch.setTarget(new StringCharacterIterator(text));
            strsrch.setPattern(pattern);
            int         count       = 0;
            int matchindex  = search.offset[count];
            while (matchindex >= 0) {
                int matchlength = search.size[count];
                strsrch.next();
                if (matchindex != strsrch.getMatchStart() ||
                    matchlength != strsrch.getMatchLength()) {
                    errln("Text: " + text);
                    errln("Pattern: " + strsrch.getPattern());
                    errln("Error match found at " + strsrch.getMatchStart() + ", " + strsrch.getMatchLength());
                    return;
                }
                matchindex = search.offset[count + 1] == -1 ? -1 :
                             search.offset[count + 2];
                if (search.offset[count + 1] != -1) {
                    strsrch.setIndex(search.offset[count + 1] + 1);
                    if (strsrch.getIndex() != search.offset[count + 1] + 1) {
                        errln("Error setting offset");
                        return;
                    }
                }

                count += 2;
            }
            strsrch.next();
            if (strsrch.getMatchStart() != StringSearch.DONE) {
                errln("Text: " + text);
                errln("Pattern: %s" + strsrch.getPattern());
                errln("Error match found at " + strsrch.getMatchStart() + ", " + strsrch.getMatchLength());
                return;
            }
        }
        strsrch.getCollator().setStrength(Collator.TERTIARY);
    }

    public void TestIgnorable() {
        String rules = IGNORABLERULE;
        int        count  = 0;
        RuleBasedCollator collator = null;
        try {
            collator = new RuleBasedCollator(rules);
            collator.setStrength(IGNORABLE[count].strength);
            collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        } catch (Exception e) {
            errln("Error opening collator ");
            return;
        }
        String pattern = "pattern";
        String text = "text";
        StringSearch strsrch = null;
        try {
            strsrch = new StringSearch(pattern, new StringCharacterIterator(text), collator, null);
        } catch (Exception e) {
            errln("Error opening string search ");
            return;
        }

        while (IGNORABLE[count].text != null) {
            text = IGNORABLE[count].text;
            pattern = IGNORABLE[count].pattern;
            strsrch.setTarget(new StringCharacterIterator(text));
            strsrch.setPattern(pattern);
            if (!assertEqualWithStringSearch(strsrch, IGNORABLE[count])) {
                errln("Error at test number " + count);
            }
            count++;
        }
    }

    public void TestInitialization() {
        String  pattern;
        String  text;
        String  temp = "a";
        StringSearch  result;

        /* simple test on the pattern ce construction */
        pattern = temp + temp;
        text = temp + temp + temp;
        try {
            result = new StringSearch(pattern, new StringCharacterIterator(text), m_en_us_, null);
        } catch (Exception e) {
            errln("Error opening search ");
            return;
        }

        /* testing if an extremely large pattern will fail the initialization */
        pattern = "";
        for (int count = 0; count < 512; count ++) {
            pattern += temp;
        }
        try {
            result = new StringSearch(pattern, new StringCharacterIterator(text), m_en_us_, null);
            logln("pattern:" + result.getPattern());
        } catch (Exception e) {
            errln("Fail: an extremely large pattern will fail the initialization");
            return;
        }
        if (result != result) {
            errln("Error: string search object expected to match itself");
        }

    }

    public void TestNormCanonical() {
        m_en_us_.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        int count = 0;
        while (NORMCANONICAL[count].text != null) {
            if (!assertCanonicalEqual(NORMCANONICAL[count])) {
                errln("Error at test number " + count);
            }
            count++;
        }
        m_en_us_.setDecomposition(Collator.NO_DECOMPOSITION);
    }

    public void TestNormExact() {
        int count = 0;
        m_en_us_.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        while (BASIC[count].text != null) {
            if (!assertEqual(BASIC[count])) {
                errln("Error at test number " + count);
            }
            count++;
        }
        count = 0;
        while (NORMEXACT[count].text != null) {
            if (!assertEqual(NORMEXACT[count])) {
                errln("Error at test number " + count);
            }
            count++;
        }
        m_en_us_.setDecomposition(Collator.NO_DECOMPOSITION);
        count = 0;
        while (NONNORMEXACT[count].text != null) {
            if (!assertEqual(NONNORMEXACT[count])) {
                errln("Error at test number " + count);
            }
            count++;
        }
    }

    public void TestOpenClose() {
        StringSearch            result;
        BreakIterator           breakiter = m_en_wordbreaker_;
        String           pattern = "";
        String           text = "";
        String           temp  = "a";
        StringCharacterIterator  chariter= new StringCharacterIterator(text);

        /* testing null arguments */
        try {
            result = new StringSearch(pattern, new StringCharacterIterator(text), null, null);
            errln("Error: null arguments should produce an error");
        } catch (Exception e) {
        	logln("PASS: null arguments failed as expected");
       	}

        chariter.setText(text);
        try {
            result = new StringSearch(pattern, chariter, null, null);
            errln("Error: null arguments should produce an error");
        } catch (Exception e) {
        	logln("PASS: null arguments failed as expected");
       	}

        text  = String.valueOf(0x1);
        try {
            result = new StringSearch(pattern, new StringCharacterIterator(text), null, null);
            errln("Error: Empty pattern should produce an error");
        } catch (Exception e) {
        	logln("PASS: Empty pattern failed as expected");
        }

        chariter.setText(text);
        try {
            result = new StringSearch(pattern, chariter, null, null);
            errln("Error: Empty pattern should produce an error");
        } catch (Exception e) {
        	logln("PASS: Empty pattern failed as expected");
        }

        text = "";
        pattern =temp;
        try {
            result = new StringSearch(pattern, new StringCharacterIterator(text), null, null);
            errln("Error: Empty text should produce an error");
        } catch (Exception e) {
        	logln("PASS: Empty text failed as expected");
        }

        chariter.setText(text);
        try {
            result = new StringSearch(pattern, chariter, null, null);
            errln("Error: Empty text should produce an error");
        } catch (Exception e) {
        	logln("PASS: Empty text failed as expected");
    	}

        text += temp;
        try {
            result = new StringSearch(pattern, new StringCharacterIterator(text), null, null);
            errln("Error: null arguments should produce an error");
        } catch (Exception e) {
        	logln("PASS: null arguments failed as expected");
       	}

        chariter.setText(text);
        try {
            result = new StringSearch(pattern, chariter, null, null);
            errln("Error: null arguments should produce an error");
        } catch (Exception e) {
        	logln("PASS: null arguments failed as expected");
       	}

        try {
            result = new StringSearch(pattern, new StringCharacterIterator(text), m_en_us_, null);
        } catch (Exception e) {
            errln("Error: null break iterator is valid for opening search");
        }

        try {
            result = new StringSearch(pattern, chariter, m_en_us_, null);
        } catch (Exception e) {
            errln("Error: null break iterator is valid for opening search");
        }

        try {
            result = new StringSearch(pattern, new StringCharacterIterator(text), Locale.ENGLISH);
        } catch (Exception e) {
            errln("Error: null break iterator is valid for opening search");
        }

        try {
            result = new StringSearch(pattern, chariter, Locale.ENGLISH);
        } catch (Exception e) {
            errln("Error: null break iterator is valid for opening search");
        }

        try {
            result = new StringSearch(pattern, new StringCharacterIterator(text), m_en_us_, breakiter);
        } catch (Exception e) {
            errln("Error: Break iterator is valid for opening search");
        }

        try {
            result = new StringSearch(pattern, chariter, m_en_us_, null);
            logln("pattern:" + result.getPattern());
        } catch (Exception e) {
            errln("Error: Break iterator is valid for opening search");
        }
    }

    public void TestOverlap() {
        int count = 0;
        while (OVERLAP[count].text != null) {
            if (!assertEqualWithAttribute(OVERLAP[count], false, true)) {
                errln("Error at overlap test number " + count);
            }
            count++;
        }
        count = 0;
        while (NONOVERLAP[count].text != null) {
            if (!assertEqual(NONOVERLAP[count])) {
                errln("Error at non overlap test number " + count);
            }
            count++;
        }

        count = 0;
        while (count < 1) {
            SearchData search = (OVERLAP[count]);
            String text = search.text;
            String pattern = search.pattern;

            RuleBasedCollator collator = getCollator(search.collator);
            StringSearch strsrch = null;
            try {
                strsrch  = new StringSearch(pattern, new StringCharacterIterator(text), collator, null);
            } catch (Exception e) {
                errln("error open StringSearch");
                return;
            }

            strsrch.setOverlapping(true);
            if (!strsrch.isOverlapping()) {
                errln("Error setting overlap option");
            }
            if (!assertEqualWithStringSearch(strsrch, search)) {
                return;
            }

            search = NONOVERLAP[count];
            strsrch.setOverlapping(false);
            if (strsrch.isOverlapping()) {
                errln("Error setting overlap option");
            }
            strsrch.reset();
            if (!assertEqualWithStringSearch(strsrch, search)) {
                errln("Error at test number " + count);
             }
            count ++;
        }
    }

    public void TestOverlapCanonical() {
        int count = 0;
        while (OVERLAPCANONICAL[count].text != null) {
            if (!assertEqualWithAttribute(OVERLAPCANONICAL[count], true,
                                          true)) {
                errln("Error at overlap test number %d" + count);
            }
            count ++;
        }
        count = 0;
        while (NONOVERLAP[count].text != null) {
            if (!assertCanonicalEqual(NONOVERLAPCANONICAL[count])) {
                errln("Error at non overlap test number %d" + count);
            }
            count ++;
        }

        count = 0;
        while (count < 1) {
                 /* UChar       temp[128];
            const SearchData *search = &(OVERLAPCANONICAL[count]);
                  UErrorCode  status = U_ZERO_ERROR;*/
            SearchData search = OVERLAPCANONICAL[count];

            /*u_unescape(search.text, temp, 128);
            UnicodeString text;
            text.setTo(temp, u_strlen(temp));
            u_unescape(search.pattern, temp, 128);
            UnicodeString pattern;
            pattern.setTo(temp, u_strlen(temp));*/
            RuleBasedCollator collator = getCollator(search.collator);
            StringSearch strsrch = new StringSearch(search.pattern, new StringCharacterIterator(search.text), collator, null);
            strsrch.setCanonical(true);
            strsrch.setOverlapping(true);
            if (strsrch.isOverlapping() != true) {
                errln("Error setting overlap option");
            }
            if (!assertEqualWithStringSearch(strsrch, search)) {
                strsrch = null;
                return;
            }
            search = NONOVERLAPCANONICAL[count];
            strsrch.setOverlapping(false);
            if (strsrch.isOverlapping() != false) {
                errln("Error setting overlap option");
            }
            strsrch.reset();
            if (!assertEqualWithStringSearch(strsrch, search)) {
                strsrch = null;
                errln("Error at test number %d" + count);
             }

            count ++;
            strsrch = null;
        }
    }

    public void TestPattern() {
        m_en_us_.setStrength(PATTERN[0].strength);
        StringSearch strsrch = new StringSearch(PATTERN[0].pattern, new StringCharacterIterator(PATTERN[0].text), m_en_us_, null);

        /*if (U_FAILURE(status)) {
            errln("Error opening string search %s", u_errorName(status));
            m_en_us_.setStrength(getECollationStrength(UCOL_TERTIARY));
            if (strsrch != NULL) {
                delete strsrch;
            }
            return;
        }*/

        if (strsrch.getPattern() != PATTERN[0].pattern) {
            errln("Error setting pattern");
        }
        if (!assertEqualWithStringSearch(strsrch, PATTERN[0])) {
            m_en_us_.setStrength(Collator.TERTIARY);
            if (strsrch != null) {
                strsrch = null;
            }
            return;
        }

        strsrch.setPattern(PATTERN[1].pattern);
        if (PATTERN[1].pattern != strsrch.getPattern()) {
            errln("Error setting pattern");
            m_en_us_.setStrength(Collator.TERTIARY);
            if (strsrch != null) {
                strsrch = null;
            }
            return;
        }
        strsrch.reset();

        if (!assertEqualWithStringSearch(strsrch, PATTERN[1])) {
            m_en_us_.setStrength(Collator.TERTIARY);
            if (strsrch != null) {
                strsrch = null;
            }
            return;
        }

        strsrch.setPattern(PATTERN[0].pattern);
        if (PATTERN[0].pattern != strsrch.getPattern()) {
            errln("Error setting pattern");
            m_en_us_.setStrength(Collator.TERTIARY);
            if (strsrch != null) {
                strsrch = null;
            }
            return;
        }
            strsrch.reset();

        if (!assertEqualWithStringSearch(strsrch, PATTERN[0])) {
            m_en_us_.setStrength(Collator.TERTIARY);
            if (strsrch != null) {
                strsrch = null;
            }
            return;
        }
        /* enormous pattern size to see if this crashes */
        String pattern = "";
        for (int templength = 0; templength != 512; templength ++) {
            pattern += 0x61;
        }
        try{
            strsrch.setPattern(pattern);
        }catch(Exception e) {
            errln("Error setting pattern with size 512");
        }

        m_en_us_.setStrength(Collator.TERTIARY);
        if (strsrch != null) {
            strsrch = null;
        }
    }

    public void TestPatternCanonical() {
        //StringCharacterIterator text = new StringCharacterIterator(PATTERNCANONICAL[0].text);
        m_en_us_.setStrength(PATTERNCANONICAL[0].strength);
        StringSearch strsrch = new StringSearch(PATTERNCANONICAL[0].pattern, new StringCharacterIterator(PATTERNCANONICAL[0].text),
                                                m_en_us_, null);
        strsrch.setCanonical(true);

        if (PATTERNCANONICAL[0].pattern != strsrch.getPattern()) {
            errln("Error setting pattern");
        }
        if (!assertEqualWithStringSearch(strsrch, PATTERNCANONICAL[0])) {
            m_en_us_.setStrength(Collator.TERTIARY);
            strsrch = null;
            return;
        }

        strsrch.setPattern(PATTERNCANONICAL[1].pattern);
        if (PATTERNCANONICAL[1].pattern != strsrch.getPattern()) {
            errln("Error setting pattern");
            m_en_us_.setStrength(Collator.TERTIARY);
            strsrch = null;
            return;
        }
        strsrch.reset();
        strsrch.setCanonical(true);

        if (!assertEqualWithStringSearch(strsrch, PATTERNCANONICAL[1])) {
            m_en_us_.setStrength(Collator.TERTIARY);
            strsrch = null;
            return;
        }

        strsrch.setPattern(PATTERNCANONICAL[0].pattern);
        if (PATTERNCANONICAL[0].pattern != strsrch.getPattern()) {
            errln("Error setting pattern");
            m_en_us_.setStrength(Collator.TERTIARY);
            strsrch = null;
            return;
        }

        strsrch.reset();
        strsrch.setCanonical(true);
        if (!assertEqualWithStringSearch(strsrch, PATTERNCANONICAL[0])) {
            m_en_us_.setStrength(Collator.TERTIARY);
            strsrch = null;
            return;
        }
    }

    public void TestReset() {
        StringCharacterIterator text = new StringCharacterIterator("fish fish");
        String pattern = "s";

        StringSearch  strsrch = new StringSearch(pattern, text, m_en_us_, null);
        strsrch.setOverlapping(true);
        strsrch.setCanonical(true);
        strsrch.setIndex(9);
        strsrch.reset();
        if (strsrch.isCanonical() || strsrch.isOverlapping() ||
            strsrch.getIndex() != 0 || strsrch.getMatchLength() != 0 ||
            strsrch.getMatchStart() != SearchIterator.DONE) {
                errln("Error resetting string search");
        }

        strsrch.previous();
        if (strsrch.getMatchStart() != 7 || strsrch.getMatchLength() != 1) {
            errln("Error resetting string search\n");
        }
    }

    public void TestSetMatch() {
        int count = 0;
        while (MATCH[count].text != null) {
            SearchData     search = MATCH[count];
            StringSearch strsrch = new StringSearch(search.pattern, new StringCharacterIterator(search.text),
                                                    m_en_us_, null);

            int size = 0;
            while (search.offset[size] != -1) {
                size ++;
            }

            if (strsrch.first() != search.offset[0]) {
                errln("Error getting first match");
            }
            if (strsrch.last() != search.offset[size -1]) {
                errln("Error getting last match");
            }

            int index = 0;
            while (index < size) {
                if (index + 2 < size) {
                    if (strsrch.following(search.offset[index + 2] - 1) != search.offset[index + 2]) {
                        errln("Error getting following match at index " + (search.offset[index + 2]-1));
                    }
                }
                if (index + 1 < size) {
                    if (strsrch.preceding(search.offset[index + 1] + search.size[index + 1] + 1) != search.offset[index + 1]) {
                        errln("Error getting preceeding match at index " + (search.offset[index + 1] + 1));
                    }
                }
                index += 2;
            }

            if (strsrch.following(search.text.length()) != SearchIterator.DONE) {
                errln("Error expecting out of bounds match");
            }
            if (strsrch.preceding(0) != SearchIterator.DONE) {
                errln("Error expecting out of bounds match");
            }
            count ++;
            strsrch = null;
        }
    }

    public void TestStrength() {
        int count = 0;
        while (STRENGTH[count].text != null) {
            if (count == 3) count ++;
            if (!assertEqual(STRENGTH[count])) {
                errln("Error at test number " + count);
            }
            count ++;
        }
    }

    public void TestStrengthCanonical() {
        int count = 0;
        while (STRENGTHCANONICAL[count].text != null) {
            if (count == 3) count ++;
            if (!assertCanonicalEqual(STRENGTHCANONICAL[count])) {
                errln("Error at test number" + count);
            }
            count ++;
        }
    }

    public void TestSupplementary() {
        int count = 0;
        while (SUPPLEMENTARY[count].text != null) {
            if (!assertEqual(SUPPLEMENTARY[count])) {
                errln("Error at test number " + count);
            }
            count ++;
        }
    }

    public void TestSupplementaryCanonical() {
        int count = 0;
        while (SUPPLEMENTARYCANONICAL[count].text != null) {
            if (!assertCanonicalEqual(SUPPLEMENTARYCANONICAL[count])) {
                errln("Error at test number" + count);
            }
            count ++;
        }
    }

    public void TestText() {
        SearchData TEXT[] = {
            new SearchData("the foxy brown fox", "fox", null, Collator.TERTIARY, null, new int[] {4, 15, -1}, new int[] {3, 3}),
            new SearchData("the quick brown fox", "fox", null, Collator.TERTIARY, null, new int[] {16, -1}, new int[] {3}),
            new SearchData(null, null, null, Collator.TERTIARY, null, new int[] {-1}, new int[]{0})
        };
        StringCharacterIterator t = new StringCharacterIterator(TEXT[0].text);
        StringSearch strsrch = new StringSearch(TEXT[0].pattern, t, m_en_us_, null);

        if (!t.equals(strsrch.getTarget())) {
            errln("Error setting text");
        }
        if (!assertEqualWithStringSearch(strsrch, TEXT[0])) {
            errln("Error at assertEqualWithStringSearch");
            return;
        }

        t = new StringCharacterIterator(TEXT[1].text);
        strsrch.setTarget(t);
        if (!t.equals(strsrch.getTarget())) {
            errln("Error setting text");
            return;
        }

        if (!assertEqualWithStringSearch(strsrch, TEXT[1])) {
            errln("Error at assertEqualWithStringSearch");
            return;
        }
    }

    public void TestTextCanonical() {
        StringCharacterIterator t = new StringCharacterIterator(TEXTCANONICAL[0].text);
        StringSearch strsrch = new StringSearch(TEXTCANONICAL[0].pattern, t, m_en_us_, null);
        strsrch.setCanonical(true);

        if (!t.equals(strsrch.getTarget())) {
            errln("Error setting text");
        }
        if (!assertEqualWithStringSearch(strsrch, TEXTCANONICAL[0])) {
            strsrch = null;
            return;
        }

        t = new StringCharacterIterator(TEXTCANONICAL[1].text);
        strsrch.setTarget(t);
        if (!t.equals(strsrch.getTarget())) {
            errln("Error setting text");
            strsrch = null;
            return;
        }

        if (!assertEqualWithStringSearch(strsrch, TEXTCANONICAL[1])) {
            strsrch = null;
            return;
        }

        t = new StringCharacterIterator(TEXTCANONICAL[0].text);
        strsrch.setTarget(t);
        if (!t.equals(strsrch.getTarget())) {
            errln("Error setting text");
            strsrch = null;
            return;
        }

        if (!assertEqualWithStringSearch(strsrch, TEXTCANONICAL[0])) {
            errln("Error at assertEqualWithStringSearch");
            strsrch = null;
            return;
        }
    }

    public void TestVariable() {
        int count = 0;
        m_en_us_.setAlternateHandlingShifted(true);
        while (VARIABLE[count].text != null) {
            // logln("variable" + count);
            if (!assertEqual(VARIABLE[count])) {
                errln("Error at test number " + count);
            }
            count ++;
        }
        m_en_us_.setAlternateHandlingShifted(false);
    }

    public void TestVariableCanonical() {
        int count = 0;
        m_en_us_.setAlternateHandlingShifted(true);
        while (VARIABLE[count].text != null) {
            // logln("variable " + count);
            if (!assertCanonicalEqual(VARIABLE[count])) {
                errln("Error at test number " + count);
            }
            count ++;
        }
        m_en_us_.setAlternateHandlingShifted(false);
    }

    public void TestSubClass()
    {
        class TestSearch extends SearchIterator
        {
            String pattern;
            String text;

            TestSearch(StringCharacterIterator target, BreakIterator breaker,
                       String pattern)
            {
                super(target, breaker);
                this.pattern = pattern;
                StringBuffer buffer = new StringBuffer();
                while (targetText.getIndex() != targetText.getEndIndex()) {
                    buffer.append(targetText.current());
                    targetText.next();
                }
                text = buffer.toString();
                targetText.setIndex(targetText.getBeginIndex());
            }
            protected int handleNext(int start)
            {
                int match = text.indexOf(pattern, start);
                if (match < 0) {
                    targetText.last();
                    return DONE;
                }
                targetText.setIndex(match);
                setMatchLength(pattern.length());
                return match;
            }
            protected int handlePrevious(int start)
            {
                int match = text.lastIndexOf(pattern, start - 1);
                if (match < 0) {
                    targetText.setIndex(0);
                    return DONE;
                }
                targetText.setIndex(match);
                setMatchLength(pattern.length());
                return match;
            }

            public int getIndex()
            {
                int result = targetText.getIndex();
                if (result < 0 || result >= text.length()) {
                    return DONE;
                }
                return result;
            }
        }

        TestSearch search = new TestSearch(
                            new StringCharacterIterator("abc abcd abc"),
                            null, "abc");
        int expected[] = {0, 4, 9};
        for (int i = 0; i < expected.length; i ++) {
            if (search.next() != expected[i]) {
                errln("Error getting next match");
            }
            if (search.getMatchLength() != search.pattern.length()) {
                errln("Error getting next match length");
            }
        }
        if (search.next() != SearchIterator.DONE) {
            errln("Error should have reached the end of the iteration");
        }
        for (int i = expected.length - 1; i >= 0; i --) {
            if (search.previous() != expected[i]) {
                errln("Error getting next match");
            }
            if (search.getMatchLength() != search.pattern.length()) {
                errln("Error getting next match length");
            }
        }
        if (search.previous() != SearchIterator.DONE) {
            errln("Error should have reached the start of the iteration");
        }
    }
    
    //Test for ticket 5024
    public void TestDiactricMatch() {
        String pattern = "pattern";
        String text = "text";
        StringSearch strsrch = null;
        int count = 0;
        try {
            strsrch = new StringSearch(pattern, text);
        } catch (Exception e) {
            errln("Error opening string search ");
            return;
        }

        while (DIACTRICMATCH[count].text != null) {
            strsrch.setCollator(getCollator(DIACTRICMATCH[count].collator));
            strsrch.getCollator().setStrength(DIACTRICMATCH[count].strength);
            strsrch.setBreakIterator(getBreakIterator(DIACTRICMATCH[count].breaker));
            strsrch.reset();
            text = DIACTRICMATCH[count].text;
            pattern = DIACTRICMATCH[count].pattern;
            strsrch.setTarget(new StringCharacterIterator(text));
            strsrch.setPattern(pattern);
            if (!assertEqualWithStringSearch(strsrch, DIACTRICMATCH[count])) {
                errln("Error at test number " + count);
            }
            count++;
        }
    }
}