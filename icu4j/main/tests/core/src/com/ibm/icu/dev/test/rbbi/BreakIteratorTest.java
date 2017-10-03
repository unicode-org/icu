// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
/*
 *******************************************************************************
 * Copyright (C) 1996-2016, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.rbbi;

import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.FilteredBreakIteratorBuilder;
import com.ibm.icu.util.ULocale;

@SuppressWarnings("unused")
@RunWith(JUnit4.class)
public class BreakIteratorTest extends TestFmwk
{
    private BreakIterator characterBreak;
    private BreakIterator wordBreak;
    private BreakIterator lineBreak;
    private BreakIterator sentenceBreak;
    private BreakIterator titleBreak;

    public BreakIteratorTest()
    {

    }

    @Before
    public void init(){
        characterBreak = BreakIterator.getCharacterInstance();
        wordBreak = BreakIterator.getWordInstance();
        lineBreak = BreakIterator.getLineInstance();
        //logln("Creating sentence iterator...");
        sentenceBreak = BreakIterator.getSentenceInstance();
        //logln("Finished creating sentence iterator...");
        titleBreak = BreakIterator.getTitleInstance();
    }
    //=========================================================================
    // general test subroutines
    //=========================================================================

    private List<String> _testFirstAndNext(BreakIterator bi, String text) {
        int p = bi.first();
        int lastP = p;
        List<String> result = new ArrayList<String>();

        if (p != 0)
            errln("first() returned " + p + " instead of 0");
        while (p != BreakIterator.DONE) {
            p = bi.next();
            if (p != BreakIterator.DONE) {
                if (p <= lastP)
                    errln("next() failed to move forward: next() on position "
                                    + lastP + " yielded " + p);

                result.add(text.substring(lastP, p));
            }
            else {
                if (lastP != text.length())
                    errln("next() returned DONE prematurely: offset was "
                                    + lastP + " instead of " + text.length());
            }
            lastP = p;
        }
        return result;
    }

    private List<String> _testLastAndPrevious(BreakIterator bi, String text) {
        int p = bi.last();
        int lastP = p;
        List<String> result = new ArrayList<String>();

        if (p != text.length())
            errln("last() returned " + p + " instead of " + text.length());
        while (p != BreakIterator.DONE) {
            p = bi.previous();
            if (p != BreakIterator.DONE) {
                if (p >= lastP)
                    errln("previous() failed to move backward: previous() on position "
                                    + lastP + " yielded " + p);

                result.add(0, text.substring(p, lastP));
            }
            else {
                if (lastP != 0)
                    errln("previous() returned DONE prematurely: offset was "
                                    + lastP + " instead of 0");
            }
            lastP = p;
        }
        return result;
    }

    private void compareFragmentLists(String f1Name, String f2Name, List<String> f1, List<String> f2) {
        int p1 = 0;
        int p2 = 0;
        String s1;
        String s2;
        int t1 = 0;
        int t2 = 0;

        while (p1 < f1.size() && p2 < f2.size()) {
            s1 = f1.get(p1);
            s2 = f2.get(p2);
            t1 += s1.length();
            t2 += s2.length();

            if (s1.equals(s2)) {
                debugLogln("   >" + s1 + "<");
                ++p1;
                ++p2;
            }
            else {
                int tempT1 = t1;
                int tempT2 = t2;
                int tempP1 = p1;
                int tempP2 = p2;

                while (tempT1 != tempT2 && tempP1 < f1.size() && tempP2 < f2.size()) {
                    while (tempT1 < tempT2 && tempP1 < f1.size()) {
                        tempT1 += (f1.get(tempP1)).length();
                        ++tempP1;
                    }
                    while (tempT2 < tempT1 && tempP2 < f2.size()) {
                        tempT2 += (f2.get(tempP2)).length();
                        ++tempP2;
                    }
                }
                logln("*** " + f1Name + " has:");
                while (p1 <= tempP1 && p1 < f1.size()) {
                    s1 = f1.get(p1);
                    t1 += s1.length();
                    debugLogln(" *** >" + s1 + "<");
                    ++p1;
                }
                logln("***** " + f2Name + " has:");
                while (p2 <= tempP2 && p2 < f2.size()) {
                    s2 = f2.get(p2);
                    t2 += s2.length();
                    debugLogln(" ***** >" + s2 + "<");
                    ++p2;
                }
                errln("Discrepancy between " + f1Name + " and " + f2Name);
            }
        }
    }

    private void _testFollowing(BreakIterator bi, String text, int[] boundaries) {
        logln("testFollowing():");
        int p = 2;
        for (int i = 0; i <= text.length(); i++) {
            if (i == boundaries[p])
                ++p;

            int b = bi.following(i);
            logln("bi.following(" + i + ") -> " + b);
            if (b != boundaries[p])
                errln("Wrong result from following() for " + i + ": expected " + boundaries[p]
                                + ", got " + b);
        }
    }

    private void _testPreceding(BreakIterator bi, String text, int[] boundaries) {
        logln("testPreceding():");
        int p = 0;
        for (int i = 0; i <= text.length(); i++) {
            int b = bi.preceding(i);
            logln("bi.preceding(" + i + ") -> " + b);
            if (b != boundaries[p])
                errln("Wrong result from preceding() for " + i + ": expected " + boundaries[p]
                                + ", got " + b);

            if (i == boundaries[p + 1])
                ++p;
        }
    }

    private void _testIsBoundary(BreakIterator bi, String text, int[] boundaries) {
        logln("testIsBoundary():");
        int p = 1;
        boolean isB;
        for (int i = 0; i <= text.length(); i++) {
            isB = bi.isBoundary(i);
            logln("bi.isBoundary(" + i + ") -> " + isB);

            if (i == boundaries[p]) {
                if (!isB)
                    errln("Wrong result from isBoundary() for " + i + ": expected true, got false");
                ++p;
            }
            else {
                if (isB)
                    errln("Wrong result from isBoundary() for " + i + ": expected false, got true");
            }
        }
    }

    private void doOtherInvariantTest(BreakIterator tb, String testChars)
    {
        StringBuffer work = new StringBuffer("a\r\na");
        int errorCount = 0;

        // a break should never occur between CR and LF
        for (int i = 0; i < testChars.length(); i++) {
            work.setCharAt(0, testChars.charAt(i));
            for (int j = 0; j < testChars.length(); j++) {
                work.setCharAt(3, testChars.charAt(j));
                tb.setText(work.toString());
                for (int k = tb.first(); k != BreakIterator.DONE; k = tb.next())
                    if (k == 2) {
                        errln("Break between CR and LF in string U+" + Integer.toHexString(
                                (work.charAt(0))) + ", U+d U+a U+" + Integer.toHexString(
                                (work.charAt(3))));
                        errorCount++;
                        if (errorCount >= 75)
                            return;
                    }
            }
        }

        // a break should never occur before a non-spacing mark, unless it's preceded
        // by a line terminator
        work.setLength(0);
        work.append("aaaa");
        for (int i = 0; i < testChars.length(); i++) {
            char c = testChars.charAt(i);
            if (c == '\n' || c == '\r' || c == '\u2029' || c == '\u2028' || c == '\u0003')
                continue;
            work.setCharAt(1, c);
            for (int j = 0; j < testChars.length(); j++) {
                c = testChars.charAt(j);
                if (Character.getType(c) != Character.NON_SPACING_MARK && Character.getType(c)
                        != Character.ENCLOSING_MARK)
                    continue;
                work.setCharAt(2, c);
                tb.setText(work.toString());
                for (int k = tb.first(); k != BreakIterator.DONE; k = tb.next())
                    if (k == 2) {
                        errln("Break between U+" + Integer.toHexString((work.charAt(1)))
                                + " and U+" + Integer.toHexString((work.charAt(2))));
                        errorCount++;
                        if (errorCount >= 75)
                            return;
                    }
            }
        }
    }

    public void debugLogln(String s) {
        final String zeros = "0000";
        String temp;
        StringBuffer out = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= ' ' && c < '\u007f')
                out.append(c);
            else {
                out.append("\\u");
                temp = Integer.toHexString(c);
                out.append(zeros.substring(0, 4 - temp.length()));
                out.append(temp);
            }
        }
        logln(out.toString());
    }

    //=========================================================================
    // tests
    //=========================================================================


    /*
     * @bug 4153072
     */
    @Test
    public void TestBug4153072() {
        BreakIterator iter = BreakIterator.getWordInstance();
        String str = "...Hello, World!...";
        int begin = 3;
        int end = str.length() - 3;
        // not used boolean gotException = false;


        iter.setText(new StringCharacterIterator(str, begin, end, begin));
        for (int index = -1; index < begin + 1; ++index) {
            try {
                iter.isBoundary(index);
                if (index < begin)
                    errln("Didn't get exception with offset = " + index +
                                    " and begin index = " + begin);
            }
            catch (IllegalArgumentException e) {
                if (index >= begin)
                    errln("Got exception with offset = " + index +
                                    " and begin index = " + begin);
            }
        }
    }


    private static final String cannedTestChars
        = "\u0000\u0001\u0002\u0003\u0004 !\"#$%&()+-01234<=>ABCDE[]^_`abcde{}|\u00a0\u00a2"
        + "\u00a3\u00a4\u00a5\u00a6\u00a7\u00a8\u00a9\u00ab\u00ad\u00ae\u00af\u00b0\u00b2\u00b3"
        + "\u00b4\u00b9\u00bb\u00bc\u00bd\u02b0\u02b1\u02b2\u02b3\u02b4\u0300\u0301\u0302\u0303"
        + "\u0304\u05d0\u05d1\u05d2\u05d3\u05d4\u0903\u093e\u093f\u0940\u0949\u0f3a\u0f3b\u2000"
        + "\u2001\u2002\u200c\u200d\u200e\u200f\u2010\u2011\u2012\u2028\u2029\u202a\u203e\u203f"
        + "\u2040\u20dd\u20de\u20df\u20e0\u2160\u2161\u2162\u2163\u2164";

    @Test
    public void TestSentenceInvariants()
    {
        BreakIterator e = BreakIterator.getSentenceInstance();
        doOtherInvariantTest(e, cannedTestChars + ".,\u3001\u3002\u3041\u3042\u3043\ufeff");
    }

    @Test
    public void TestGetAvailableLocales()
    {
        Locale[] locList = BreakIterator.getAvailableLocales();

        if (locList.length == 0)
            errln("getAvailableLocales() returned an empty list!");
        // I have no idea how to test this function...

        com.ibm.icu.util.ULocale[] ulocList = BreakIterator.getAvailableULocales();
        if (ulocList.length == 0) {
            errln("getAvailableULocales() returned an empty list!");
        } else {
            logln("getAvailableULocales() returned " + ulocList.length + " locales");
        }
    }


    /**
     * @bug 4068137
     */
    @Test
    public void TestEndBehavior()
    {
        String testString = "boo.";
        BreakIterator wb = BreakIterator.getWordInstance();
        wb.setText(testString);

        if (wb.first() != 0)
            errln("Didn't get break at beginning of string.");
        if (wb.next() != 3)
            errln("Didn't get break before period in \"boo.\"");
        if (wb.current() != 4 && wb.next() != 4)
            errln("Didn't get break at end of string.");
    }

    // The Following two tests are ported from ICU4C 1.8.1 [Richard/GCL]
    /**
     * Port From:   ICU4C v1.8.1 : textbounds : IntlTestTextBoundary
     * Source File: $ICU4CRoot/source/test/intltest/ittxtbd.cpp
     **/
    /**
     * test methods preceding, following and isBoundary
     **/
    @Test
    public void TestPreceding() {
        String words3 = "aaa bbb ccc";
        BreakIterator e = BreakIterator.getWordInstance(Locale.getDefault());
        e.setText( words3 );
        e.first();
        int p1 = e.next();
        int p2 = e.next();
        int p3 = e.next();
        int p4 = e.next();

        int f = e.following(p2+1);
        int p = e.preceding(p2+1);
        if (f!=p3)
            errln("IntlTestTextBoundary::TestPreceding: f!=p3");
        if (p!=p2)
            errln("IntlTestTextBoundary::TestPreceding: p!=p2");

        if (p1+1!=p2)
            errln("IntlTestTextBoundary::TestPreceding: p1+1!=p2");

        if (p3+1!=p4)
            errln("IntlTestTextBoundary::TestPreceding: p3+1!=p4");

        if (!e.isBoundary(p2) || e.isBoundary(p2+1) || !e.isBoundary(p3))
        {
            errln("IntlTestTextBoundary::TestPreceding: isBoundary err");
        }
    }

    /**
     * Ticket#5615
     */
    @Test
    public void TestT5615() {
        com.ibm.icu.util.ULocale[] ulocales = BreakIterator.getAvailableULocales();
        int type = 0;
        com.ibm.icu.util.ULocale loc = null;
        try {
            for (int i = 0; i < ulocales.length; i++) {
                loc = ulocales[i];
                for (type = 0; type < 5 /* 5 = BreakIterator.KIND_COUNT */; ++type) {
                    BreakIterator brk = BreakIterator.getBreakInstance(loc, type);
                    if (brk == null) {
                        errln("ERR: Failed to create an instance type: " + type + " / locale: " + loc);
                    }
                }
            }
        } catch (Exception e) {
            errln("ERR: Failed to create an instance type: " + type + " / locale: " + loc + " / exception: " + e.getMessage());
        }
    }

    /**
     * At present, Japanese doesn't have exceptions.
     * However, this still should not fail.
     */
    @Test
    public void TestFilteredJapanese() {
        ULocale loc = ULocale.JAPANESE;
        BreakIterator brk = FilteredBreakIteratorBuilder
                .getInstance(loc)
                .wrapIteratorWithFilter(BreakIterator.getSentenceInstance(loc));
        brk.setText("ＯＫです。");
        assertEquals("Starting point", 0, brk.current());
        assertEquals("Next point", 5, brk.next());
        assertEquals("Last point", BreakIterator.DONE, brk.next());
    }

    /*
     * Test case for Ticket#10721. BreakIterator factory method should throw NPE
     * when specified locale is null.
     */
    @Test
    public void TestNullLocale() {
        Locale loc = null;
        ULocale uloc = null;

        @SuppressWarnings("unused")
        BreakIterator brk;

        // Character
        try {
            brk = BreakIterator.getCharacterInstance(loc);
            errln("getCharacterInstance((Locale)null) did not throw NPE.");
        } catch (NullPointerException e) { /* OK */ }
        try {
            brk = BreakIterator.getCharacterInstance(uloc);
            errln("getCharacterInstance((ULocale)null) did not throw NPE.");
        } catch (NullPointerException e) { /* OK */ }

        // Line
        try {
            brk = BreakIterator.getLineInstance(loc);
            errln("getLineInstance((Locale)null) did not throw NPE.");
        } catch (NullPointerException e) { /* OK */ }
        try {
            brk = BreakIterator.getLineInstance(uloc);
            errln("getLineInstance((ULocale)null) did not throw NPE.");
        } catch (NullPointerException e) { /* OK */ }

        // Sentence
        try {
            brk = BreakIterator.getSentenceInstance(loc);
            errln("getSentenceInstance((Locale)null) did not throw NPE.");
        } catch (NullPointerException e) { /* OK */ }
        try {
            brk = BreakIterator.getSentenceInstance(uloc);
            errln("getSentenceInstance((ULocale)null) did not throw NPE.");
        } catch (NullPointerException e) { /* OK */ }

        // Title
        try {
            brk = BreakIterator.getTitleInstance(loc);
            errln("getTitleInstance((Locale)null) did not throw NPE.");
        } catch (NullPointerException e) { /* OK */ }
        try {
            brk = BreakIterator.getTitleInstance(uloc);
            errln("getTitleInstance((ULocale)null) did not throw NPE.");
        } catch (NullPointerException e) { /* OK */ }

        // Word
        try {
            brk = BreakIterator.getWordInstance(loc);
            errln("getWordInstance((Locale)null) did not throw NPE.");
        } catch (NullPointerException e) { /* OK */ }
        try {
            brk = BreakIterator.getWordInstance(uloc);
            errln("getWordInstance((ULocale)null) did not throw NPE.");
        } catch (NullPointerException e) { /* OK */ }
    }

    /**
     * Test FilteredBreakIteratorBuilder newly introduced
     */
    @Test
    public void TestFilteredBreakIteratorBuilder() {
        FilteredBreakIteratorBuilder builder;
        BreakIterator baseBI;
        BreakIterator filteredBI;

        String text = "In the meantime Mr. Weston arrived with his small ship, which he had now recovered. Capt. Gorges, who informed the Sgt. here that one purpose of his going east was to meet with Mr. Weston, took this opportunity to call him to account for some abuses he had to lay to his charge."; // (William Bradford, public domain. http://catalog.hathitrust.org/Record/008651224 ) - edited.
        String ABBR_MR = "Mr.";
        String ABBR_CAPT = "Capt.";

        {
            logln("Constructing empty builder\n");
            builder = FilteredBreakIteratorBuilder.getEmptyInstance();

            logln("Constructing base BI\n");
            baseBI = BreakIterator.getSentenceInstance(Locale.ENGLISH);

            logln("Building new BI\n");
            filteredBI = builder.wrapIteratorWithFilter(baseBI);

            assertDefaultBreakBehavior(filteredBI, text);
        }

        {
            logln("Constructing empty builder\n");
            builder = FilteredBreakIteratorBuilder.getEmptyInstance();

            logln("Adding Mr. as an exception\n");

            assertEquals("2.1 suppressBreakAfter", true, builder.suppressBreakAfter(ABBR_MR));
            assertEquals("2.2 suppressBreakAfter", false, builder.suppressBreakAfter(ABBR_MR));
            assertEquals("2.3 unsuppressBreakAfter", true, builder.unsuppressBreakAfter(ABBR_MR));
            assertEquals("2.4 unsuppressBreakAfter", false, builder.unsuppressBreakAfter(ABBR_MR));
            assertEquals("2.5 suppressBreakAfter", true, builder.suppressBreakAfter(ABBR_MR));

            logln("Constructing base BI\n");
            baseBI = BreakIterator.getSentenceInstance(Locale.ENGLISH);

            logln("Building new BI\n");
            filteredBI = builder.wrapIteratorWithFilter(baseBI);

            logln("Testing:");
            filteredBI.setText(text);
            assertEquals("2nd next", 84, filteredBI.next());
            assertEquals("2nd next", 90, filteredBI.next());
            assertEquals("2nd next", 278, filteredBI.next());
            filteredBI.first();
        }


        {
          logln("Constructing empty builder\n");
          builder = FilteredBreakIteratorBuilder.getEmptyInstance();

          logln("Adding Mr. and Capt as an exception\n");
          assertEquals("3.1 suppressBreakAfter", true, builder.suppressBreakAfter(ABBR_MR));
          assertEquals("3.2 suppressBreakAfter", true, builder.suppressBreakAfter(ABBR_CAPT));

          logln("Constructing base BI\n");
          baseBI = BreakIterator.getSentenceInstance(Locale.ENGLISH);

          logln("Building new BI\n");
          filteredBI = builder.wrapIteratorWithFilter(baseBI);

          logln("Testing:");
          filteredBI.setText(text);
          assertEquals("3rd next", 84, filteredBI.next());
          assertEquals("3rd next", 278, filteredBI.next());
          filteredBI.first();
        }

        {
          logln("Constructing English builder\n");
          builder = FilteredBreakIteratorBuilder.getInstance(ULocale.ENGLISH);

          logln("Constructing base BI\n");
          baseBI = BreakIterator.getSentenceInstance(Locale.ENGLISH);

          logln("unsuppressing 'Capt'");
          assertEquals("1st suppressBreakAfter", true, builder.unsuppressBreakAfter(ABBR_CAPT));

          logln("Building new BI\n");
          filteredBI = builder.wrapIteratorWithFilter(baseBI);

          if(filteredBI != null) {
            logln("Testing:");
            filteredBI.setText(text);
            assertEquals("4th next", 84, filteredBI.next());
            assertEquals("4th next", 90, filteredBI.next());
            assertEquals("4th next", 278, filteredBI.next());
            filteredBI.first();
          }
        }

        {
          logln("Constructing English builder\n");
          builder = FilteredBreakIteratorBuilder.getInstance(ULocale.ENGLISH);

          logln("Constructing base BI\n");
          baseBI = BreakIterator.getSentenceInstance(Locale.ENGLISH);

          logln("Building new BI\n");
          filteredBI = builder.wrapIteratorWithFilter(baseBI);

          if(filteredBI != null) {
            assertEnglishBreakBehavior(filteredBI, text);
          }
        }

        {
            logln("Constructing English @ss=standard\n");
            filteredBI = BreakIterator.getSentenceInstance(ULocale.forLanguageTag("en-US-u-ss-standard"));

            if(filteredBI != null) {
              assertEnglishBreakBehavior(filteredBI, text);
            }
        }

        {
            logln("Constructing Afrikaans @ss=standard - should be == default\n");
            filteredBI = BreakIterator.getSentenceInstance(ULocale.forLanguageTag("af-u-ss-standard"));

            assertDefaultBreakBehavior(filteredBI, text);
        }

        {
            logln("Constructing Japanese @ss=standard - should be == default\n");
            filteredBI = BreakIterator.getSentenceInstance(ULocale.forLanguageTag("ja-u-ss-standard"));

            assertDefaultBreakBehavior(filteredBI, text);
        }
        {
            logln("Constructing tfg @ss=standard - should be == default\n");
            filteredBI = BreakIterator.getSentenceInstance(ULocale.forLanguageTag("tfg-u-ss-standard"));

            assertDefaultBreakBehavior(filteredBI, text);
        }

        {
          logln("Constructing French builder");
          builder = FilteredBreakIteratorBuilder.getInstance(ULocale.FRENCH);

          logln("Constructing base BI\n");
          baseBI = BreakIterator.getSentenceInstance(Locale.FRENCH);

          logln("Building new BI\n");
          filteredBI = builder.wrapIteratorWithFilter(baseBI);

          if(filteredBI != null) {
            assertFrenchBreakBehavior(filteredBI, text);
          }
        }
    }

    /**
     * @param filteredBI
     * @param text
     */
    private void assertFrenchBreakBehavior(BreakIterator filteredBI, String text) {
        logln("Testing French behavior:");
        filteredBI.setText(text);
        assertEquals("6th next", 20, filteredBI.next());
        assertEquals("6th next", 84, filteredBI.next());
        filteredBI.first();
    }

    /**
     * @param filteredBI
     * @param text
     */
    private void assertEnglishBreakBehavior(BreakIterator filteredBI, String text) {
        logln("Testing English filtered behavior:");
          filteredBI.setText(text);

          assertEquals("5th next", 84, filteredBI.next());
          assertEquals("5th next", 278, filteredBI.next());
          filteredBI.first();
    }

    /**
     * @param filteredBI
     * @param text
     */
    private void assertDefaultBreakBehavior(BreakIterator filteredBI, String text) {
        logln("Testing Default Behavior:");
        filteredBI.setText(text);
        assertEquals("1st next", 20, filteredBI.next());
        assertEquals("1st next", 84, filteredBI.next());
        assertEquals("1st next", 90, filteredBI.next());
        assertEquals("1st next", 181, filteredBI.next());
        assertEquals("1st next", 278, filteredBI.next());
        filteredBI.first();
    }
}
