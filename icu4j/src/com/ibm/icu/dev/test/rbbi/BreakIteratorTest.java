/*
 *******************************************************************************
 * Copyright (C) 1996-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.rbbi;

import com.ibm.icu.dev.test.*;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.RuleBasedBreakIterator_Old;
import java.text.StringCharacterIterator;
import java.util.Locale;
import java.util.Vector;

public class BreakIteratorTest extends TestFmwk
{
    private BreakIterator characterBreak;
    private BreakIterator wordBreak;
    private BreakIterator lineBreak;
    private BreakIterator sentenceBreak;
    private BreakIterator titleBreak;

    public static void main(String[] args) throws Exception {
        new BreakIteratorTest().run(args);
    }

    public BreakIteratorTest()
    {
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

    private void generalIteratorTest(BreakIterator bi, Vector expectedResult) {
        StringBuffer buffer = new StringBuffer();
        String text;
        for (int i = 0; i < expectedResult.size(); i++) {
            text = (String)expectedResult.elementAt(i);
            buffer.append(text);
        }
        text = buffer.toString();

        bi.setText(text);

        Vector nextResults = _testFirstAndNext(bi, text);
        Vector previousResults = _testLastAndPrevious(bi, text);

        logln("comparing forward and backward...");
        int errs = getErrorCount();
        compareFragmentLists("forward iteration", "backward iteration", nextResults,
                        previousResults);
        if (getErrorCount() == errs) {
            logln("comparing expected and actual...");
            compareFragmentLists("expected result", "actual result", expectedResult,
                            nextResults);
        }

        int[] boundaries = new int[expectedResult.size() + 3];
        boundaries[0] = BreakIterator.DONE;
        boundaries[1] = 0;
        for (int i = 0; i < expectedResult.size(); i++)
            boundaries[i + 2] = boundaries[i + 1] + ((String)expectedResult.elementAt(i)).
                            length();
        boundaries[boundaries.length - 1] = BreakIterator.DONE;

        _testFollowing(bi, text, boundaries);
        _testPreceding(bi, text, boundaries);
        _testIsBoundary(bi, text, boundaries);

        doMultipleSelectionTest(bi, text);
    }

    private Vector _testFirstAndNext(BreakIterator bi, String text) {
        int p = bi.first();
        int lastP = p;
        Vector result = new Vector();

        if (p != 0)
            errln("first() returned " + p + " instead of 0");
        while (p != BreakIterator.DONE) {
            p = bi.next();
            if (p != BreakIterator.DONE) {
                if (p <= lastP)
                    errln("next() failed to move forward: next() on position "
                                    + lastP + " yielded " + p);

                result.addElement(text.substring(lastP, p));
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

    private Vector _testLastAndPrevious(BreakIterator bi, String text) {
        int p = bi.last();
        int lastP = p;
        Vector result = new Vector();

        if (p != text.length())
            errln("last() returned " + p + " instead of " + text.length());
        while (p != BreakIterator.DONE) {
            p = bi.previous();
            if (p != BreakIterator.DONE) {
                if (p >= lastP)
                    errln("previous() failed to move backward: previous() on position "
                                    + lastP + " yielded " + p);

                result.insertElementAt(text.substring(p, lastP), 0);
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

    private void compareFragmentLists(String f1Name, String f2Name, Vector f1, Vector f2) {
        int p1 = 0;
        int p2 = 0;
        String s1;
        String s2;
        int t1 = 0;
        int t2 = 0;

        while (p1 < f1.size() && p2 < f2.size()) {
            s1 = (String)f1.elementAt(p1);
            s2 = (String)f2.elementAt(p2);
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
                        tempT1 += ((String)f1.elementAt(tempP1)).length();
                        ++tempP1;
                    }
                    while (tempT2 < tempT1 && tempP2 < f2.size()) {
                        tempT2 += ((String)f2.elementAt(tempP2)).length();
                        ++tempP2;
                    }
                }
                logln("*** " + f1Name + " has:");
                while (p1 <= tempP1 && p1 < f1.size()) {
                    s1 = (String)f1.elementAt(p1);
                    t1 += s1.length();
                    debugLogln(" *** >" + s1 + "<");
                    ++p1;
                }
                logln("***** " + f2Name + " has:");
                while (p2 <= tempP2 && p2 < f2.size()) {
                    s2 = (String)f2.elementAt(p2);
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

    private void doMultipleSelectionTest(BreakIterator iterator, String testText)
    {
        logln("Multiple selection test...");
        BreakIterator testIterator = (BreakIterator)iterator.clone();
        int offset = iterator.first();
        int testOffset;
        int count = 0;

        do {
            testOffset = testIterator.first();
            testOffset = testIterator.next(count);
            logln("next(" + count + ") -> " + testOffset);
            if (offset != testOffset)
                errln("next(n) and next() not returning consistent results: for step " + count + ", next(n) returned " + testOffset + " and next() had " + offset);

            if (offset != BreakIterator.DONE) {
                count++;
                offset = iterator.next();
            }
        } while (offset != BreakIterator.DONE);

        // now do it backwards...
        offset = iterator.last();
        count = 0;

        do {
            testOffset = testIterator.last();
            testOffset = testIterator.next(count);
            logln("next(" + count + ") -> " + testOffset);
            if (offset != testOffset)
                errln("next(n) and next() not returning consistent results: for step " + count + ", next(n) returned " + testOffset + " and next() had " + offset);

            if (offset != BreakIterator.DONE) {
                count--;
                offset = iterator.previous();
            }
        } while (offset != BreakIterator.DONE);
    }

    private void doBreakInvariantTest(BreakIterator tb, String testChars)
    {
        StringBuffer work = new StringBuffer("aaa");
//        int errorCount = 0;

        // a break should always occur after CR (unless followed by LF), LF, PS, and LS,
        // unless they're followed by a non-spacing mark or a format character
        String breaks = "\r\n\u2029\u2028";

        for (int i = 0; i < breaks.length(); i++) {
            work.setCharAt(1, breaks.charAt(i));
            for (int j = 0; j < testChars.length(); j++) {
                work.setCharAt(0, testChars.charAt(j));
                for (int k = 0; k < testChars.length(); k++) {
                    char c = testChars.charAt(k);

                    // if a cr is followed by lf, ps, ls or etx, don't do the check (that's
                    // not supposed to work)
                    if (work.charAt(1) == '\r' && (c == '\n' || c == '\u2029'
                            || c == '\u2028' || c == '\u0003'))
                        continue;

                    work.setCharAt(2, c);
                    tb.setText(work.toString());
                    boolean seen2 = false;
                    for (int l = tb.first(); l != BreakIterator.DONE; l = tb.next()) {
                        if (l == 2)
                            seen2 = true;
                    }
                    if (!seen2) {
                        errln("No break between U+" + Integer.toHexString((int)(work.charAt(1)))
                                    + " and U+" + Integer.toHexString((int)(work.charAt(2))));
                    }
                }
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
                                (int)(work.charAt(0))) + ", U+d U+a U+" + Integer.toHexString(
                                (int)(work.charAt(3))));
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
                        errln("Break between U+" + Integer.toHexString((int)(work.charAt(1)))
                                + " and U+" + Integer.toHexString((int)(work.charAt(2))));
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
                temp = Integer.toHexString((int)c);
                out.append(zeros.substring(0, 4 - temp.length()));
                out.append(temp);
            }
        }
        logln(out.toString());
    }

    //=========================================================================
    // tests
    //=========================================================================

    public void TestWordBreak() {
        try {
            RuleBasedBreakIterator_Old olb = (RuleBasedBreakIterator_Old)wordBreak;
            Vector wordSelectionData = new Vector();
            
            wordSelectionData.addElement("12,34");
            
            wordSelectionData.addElement(" ");
            wordSelectionData.addElement("\u00A2"); //cent sign
            wordSelectionData.addElement("\u00A3"); //pound sign
            wordSelectionData.addElement("\u00A4"); //currency sign
            wordSelectionData.addElement("\u00A5"); //yen sign
            wordSelectionData.addElement("alpha-beta-gamma");
            wordSelectionData.addElement(".");
            wordSelectionData.addElement(" ");
            wordSelectionData.addElement("Badges");
            wordSelectionData.addElement("?");
            wordSelectionData.addElement(" ");
            wordSelectionData.addElement("BADGES");
            wordSelectionData.addElement("!");
            wordSelectionData.addElement("?");
            wordSelectionData.addElement("!");
            wordSelectionData.addElement(" ");
            wordSelectionData.addElement("We");
            wordSelectionData.addElement(" ");
            wordSelectionData.addElement("don't");
            wordSelectionData.addElement(" ");
            wordSelectionData.addElement("need");
            wordSelectionData.addElement(" ");
            wordSelectionData.addElement("no");
            wordSelectionData.addElement(" ");
            wordSelectionData.addElement("STINKING");
            wordSelectionData.addElement(" ");
            wordSelectionData.addElement("BADGES");
            wordSelectionData.addElement("!");
            wordSelectionData.addElement("!");
            wordSelectionData.addElement("!");
            
            wordSelectionData.addElement("012.566,5");
            wordSelectionData.addElement(" ");
            wordSelectionData.addElement("123.3434,900");
            wordSelectionData.addElement(" ");
            wordSelectionData.addElement("1000,233,456.000");
            wordSelectionData.addElement(" ");
            wordSelectionData.addElement("1,23.322%");
            wordSelectionData.addElement(" ");
            wordSelectionData.addElement("123.1222");
            
            wordSelectionData.addElement(" ");
            wordSelectionData.addElement("\u0024123,000.20");
            
            wordSelectionData.addElement(" ");
            wordSelectionData.addElement("179.01\u0025");
            
            wordSelectionData.addElement("Hello");
            wordSelectionData.addElement(",");
            wordSelectionData.addElement(" ");
            wordSelectionData.addElement("how");
            wordSelectionData.addElement(" ");
            wordSelectionData.addElement("are");
            wordSelectionData.addElement(" ");
            wordSelectionData.addElement("you");
            wordSelectionData.addElement(" ");
            wordSelectionData.addElement("X");
            wordSelectionData.addElement(" ");
            
            wordSelectionData.addElement("Now");
            wordSelectionData.addElement("\r");
            wordSelectionData.addElement("is");
            wordSelectionData.addElement("\n");
            wordSelectionData.addElement("the");
            wordSelectionData.addElement("\r\n");
            wordSelectionData.addElement("time");
            wordSelectionData.addElement("\n");
            wordSelectionData.addElement("\r");
            wordSelectionData.addElement("for");
            wordSelectionData.addElement("\r");
            wordSelectionData.addElement("\r");
            wordSelectionData.addElement("all");
            wordSelectionData.addElement(" ");
            
            generalIteratorTest(wordBreak, wordSelectionData);
        }
        catch (ClassCastException e) {
            logln("New Break Iterator, skipping old test");
        }   
    }

    /**
     * @bug 4097779
     */
    public void TestBug4097779() {
        Vector wordSelectionData = new Vector();

        wordSelectionData.addElement("aa\u0300a");
        wordSelectionData.addElement(" ");

        generalIteratorTest(wordBreak, wordSelectionData);
    }

    /**
     * @bug 4098467
     */
    public void TestBug4098467Words() {
        Vector wordSelectionData = new Vector();

        // What follows is a string of Korean characters (I found it in the Yellow Pages
        // ad for the Korean Presbyterian Church of San Francisco, and I hope I transcribed
        // it correctly), first as precomposed syllables, and then as conjoining jamo.
        // Both sequences should be semantically identical and break the same way.
        // precomposed syllables...
        wordSelectionData.addElement("\uc0c1\ud56d");
        wordSelectionData.addElement(" ");
        wordSelectionData.addElement("\ud55c\uc778");
        wordSelectionData.addElement(" ");
        wordSelectionData.addElement("\uc5f0\ud569");
        wordSelectionData.addElement(" ");
        wordSelectionData.addElement("\uc7a5\ub85c\uad50\ud68c");
        wordSelectionData.addElement(" ");
        // conjoining jamo...
        wordSelectionData.addElement("\u1109\u1161\u11bc\u1112\u1161\u11bc");
        wordSelectionData.addElement(" ");
        wordSelectionData.addElement("\u1112\u1161\u11ab\u110b\u1175\u11ab");
        wordSelectionData.addElement(" ");
        wordSelectionData.addElement("\u110b\u1167\u11ab\u1112\u1161\u11b8");
        wordSelectionData.addElement(" ");
        wordSelectionData.addElement("\u110c\u1161\u11bc\u1105\u1169\u1100\u116d\u1112\u116c");
        wordSelectionData.addElement(" ");

        generalIteratorTest(wordBreak, wordSelectionData);
    }

    /**
     * @bug 4117554
     */
    public void TestBug4117554Words() {
        try {
            RuleBasedBreakIterator_Old olb = (RuleBasedBreakIterator_Old)wordBreak;
            Vector wordSelectionData = new Vector();
            
            // this is a test for bug #4117554: the ideographic iteration mark (U+3005) should
            // count as a Kanji character for the purposes of word breaking
            wordSelectionData.addElement("abc");
            wordSelectionData.addElement("\u4e01\u4e02\u3005\u4e03\u4e03");
            wordSelectionData.addElement("abc");
            
            generalIteratorTest(wordBreak, wordSelectionData);
        }
        catch (ClassCastException e) {
            logln("New Break Iterator, skipping old test");
        }   
    }

    public void TestSentenceBreak() {
        try {
            RuleBasedBreakIterator_Old olb = (RuleBasedBreakIterator_Old)sentenceBreak;
            Vector sentenceSelectionData = new Vector();
            
            sentenceSelectionData.addElement("This is a simple sample sentence. ");
            sentenceSelectionData.addElement("(This is it.) ");
            sentenceSelectionData.addElement("This is a simple sample sentence. ");
            sentenceSelectionData.addElement("\"This isn\'t it.\" ");
            sentenceSelectionData.addElement("Hi! ");
            sentenceSelectionData.addElement("This is a simple sample sentence. ");
            sentenceSelectionData.addElement("It does not have to make any sense as you can see. ");
            sentenceSelectionData.addElement("Nel mezzo del cammin di nostra vita, mi ritrovai in una selva oscura. ");
            sentenceSelectionData.addElement("Che la dritta via aveo smarrita. ");
            sentenceSelectionData.addElement("He said, that I said, that you said!! ");
            
            sentenceSelectionData.addElement("Don't rock the boat.\u2029");
            
            sentenceSelectionData.addElement("Because I am the daddy, that is why. ");
            sentenceSelectionData.addElement("Not on my time (el timo.)! ");
            
            sentenceSelectionData.addElement("So what!!\u2029");
            
            sentenceSelectionData.addElement("\"But now,\" he said, \"I know!\" ");
            sentenceSelectionData.addElement("Harris thumbed down several, including \"Away We Go\" (which became the huge success Oklahoma!). ");
            sentenceSelectionData.addElement("One species, B. anthracis, is highly virulent.\n");
            sentenceSelectionData.addElement("Wolf said about Sounder:\"Beautifully thought-out and directed.\" ");
            sentenceSelectionData.addElement("Have you ever said, \"This is where \tI shall live\"? ");
            sentenceSelectionData.addElement("He answered, \"You may not!\" ");
            sentenceSelectionData.addElement("Another popular saying is: \"How do you do?\". ");
            sentenceSelectionData.addElement("Yet another popular saying is: \'I\'m fine thanks.\' ");
            sentenceSelectionData.addElement("What is the proper use of the abbreviation pp.? ");
            sentenceSelectionData.addElement("Yes, I am definatelly 12\" tall!!");
            
            generalIteratorTest(sentenceBreak, sentenceSelectionData);
        }
        catch (ClassCastException e) {
            logln("New Break Iterator, skipping old test");
        }   
}

    /**
     * @bug 4113835
     */
    public void TestBug4113835() {
        try {
            RuleBasedBreakIterator_Old olb = (RuleBasedBreakIterator_Old)sentenceBreak;
            
            Vector sentenceSelectionData = new Vector();
            
            // test for bug #4113835: \n and \r count as spaces, not as paragraph breaks
            sentenceSelectionData.addElement("Now\ris\nthe\r\ntime\n\rfor\r\rall\u2029");
            
            generalIteratorTest(sentenceBreak, sentenceSelectionData);
        }
        catch (ClassCastException e) {
            logln("New Break Iterator, skipping old test");
        }   
    }

    /**
     * @bug 4111338
     */
    public void TestBug4111338() {
        Vector sentenceSelectionData = new Vector();

        // test for bug #4111338: Don't break sentences at the boundary between CJK
        // and other letters
        sentenceSelectionData.addElement("\u5487\u67ff\ue591\u5017\u61b3\u60a1\u9510\u8165:\"JAVA\u821c"
                + "\u8165\u7fc8\u51ce\u306d,\u2494\u56d8\u4ec0\u60b1\u8560\u51ba"
                + "\u611d\u57b6\u2510\u5d46\".\u2029");
        sentenceSelectionData.addElement("\u5487\u67ff\ue591\u5017\u61b3\u60a1\u9510\u8165\u9de8"
                + "\u97e4JAVA\u821c\u8165\u7fc8\u51ce\u306d\ue30b\u2494\u56d8\u4ec0"
                + "\u60b1\u8560\u51ba\u611d\u57b6\u2510\u5d46\u97e5\u7751\u2029");
        sentenceSelectionData.addElement("\u5487\u67ff\ue591\u5017\u61b3\u60a1\u9510\u8165\u9de8\u97e4"
                + "\u6470\u8790JAVA\u821c\u8165\u7fc8\u51ce\u306d\ue30b\u2494\u56d8"
                + "\u4ec0\u60b1\u8560\u51ba\u611d\u57b6\u2510\u5d46\u97e5\u7751\u2029");
        sentenceSelectionData.addElement("He said, \"I can go there.\"\u2029");

        generalIteratorTest(sentenceBreak, sentenceSelectionData);
    }

    /**
     * @bug 4117554
     */
    public void TestBug4117554Sentences() {
        try {
            RuleBasedBreakIterator_Old olb = (RuleBasedBreakIterator_Old)sentenceBreak;
            Vector sentenceSelectionData = new Vector();
            
            // Treat fullwidth variants of .!? the same as their
            // normal counterparts
            sentenceSelectionData.addElement("I know I'm right\uff0e ");
            sentenceSelectionData.addElement("Right\uff1f ");
            sentenceSelectionData.addElement("Right\uff01 ");
            
            // Don't break sentences at boundary between CJK and digits
            sentenceSelectionData.addElement("\u5487\u67ff\ue591\u5017\u61b3\u60a1\u9510\u8165\u9de8"
                    + "\u97e48888\u821c\u8165\u7fc8\u51ce\u306d\ue30b\u2494\u56d8\u4ec0"
                    + "\u60b1\u8560\u51ba\u611d\u57b6\u2510\u5d46\u97e5\u7751\u2029");
            
            // Break sentence between a sentence terminator and
            // opening punctuation
            sentenceSelectionData.addElement("no?");
            sentenceSelectionData.addElement("(yes)");
            
            generalIteratorTest(sentenceBreak, sentenceSelectionData);
        }
        catch (ClassCastException e) {
            logln("New Break Iterator, skipping old test");
        }  
    }

    /**
     * @bug 4158381
     */
    public void TestBug4158381() {
        try {
            RuleBasedBreakIterator_Old olb = (RuleBasedBreakIterator_Old)sentenceBreak;
            Vector sentenceSelectionData = new Vector();
            
            // Don't break sentence after period if it isn't followed by a space
            sentenceSelectionData.addElement("Test <code>Flags.Flag</code> class.  ");
            sentenceSelectionData.addElement("Another test.\u2029");
            
            // No breaks when there are no terminators around
            sentenceSelectionData.addElement("<P>Provides a set of "
                    + "&quot;lightweight&quot; (all-java<FONT SIZE=\"-2\"><SUP>TM"
                    + "</SUP></FONT> language) components that, "
                    + "to the maximum degree possible, work the same on all platforms.  ");
            sentenceSelectionData.addElement("Another test.\u2029");
            
            generalIteratorTest(sentenceBreak, sentenceSelectionData);
        }
        catch (ClassCastException e) {
            logln("New Break Iterator, skipping old test");
        }   
}

    /**
     * @bug 4143071
     */
    public void TestBug4143071() {
        Vector sentenceSelectionData = new Vector();

        // Make sure sentences that end with digits work right
        sentenceSelectionData.addElement("Today is the 27th of May, 1998.  ");
        sentenceSelectionData.addElement("Tomorrow will be 28 May 1998.  ");
        sentenceSelectionData.addElement("The day after will be the 30th.\u2029");

        generalIteratorTest(sentenceBreak, sentenceSelectionData);
    }

    /**
     * @bug 4152416
     */
    public void TestBug4152416() {
        Vector sentenceSelectionData = new Vector();

        // Make sure sentences ending with a capital letter are treated correctly
        sentenceSelectionData.addElement("The type of all primitive "
                + "<code>boolean</code> values accessed in the target VM.  ");
        sentenceSelectionData.addElement("Calls to xxx will return an "
                + "implementor of this interface.\u2029");

        generalIteratorTest(sentenceBreak, sentenceSelectionData);
    }

    /**
     * @bug 4152117
     */
    public void TestBug4152117() {
        Vector sentenceSelectionData = new Vector();

        // Make sure sentence breaking is handling punctuation correctly
        // [COULD NOT REPRODUCE THIS BUG, BUT TEST IS HERE TO MAKE SURE
        // IT DOESN'T CROP UP]
        sentenceSelectionData.addElement("Constructs a randomly generated "
                + "BigInteger, uniformly distributed over the range <tt>0</tt> "
                + "to <tt>(2<sup>numBits</sup> - 1)</tt>, inclusive.  ");
        sentenceSelectionData.addElement("The uniformity of the distribution "
                + "assumes that a fair source of random bits is provided in "
                + "<tt>rnd</tt>.  ");
        sentenceSelectionData.addElement("Note that this constructor always "
                + "constructs a non-negative BigInteger.\u2029");

        generalIteratorTest(sentenceBreak, sentenceSelectionData);
    }

    public void TestLineBreak() {
        Vector lineSelectionData = new Vector();

        lineSelectionData.addElement("Multi-");
        lineSelectionData.addElement("Level ");
        lineSelectionData.addElement("example ");
        lineSelectionData.addElement("of ");
        lineSelectionData.addElement("a ");
        lineSelectionData.addElement("semi-");
        lineSelectionData.addElement("idiotic ");
        lineSelectionData.addElement("non-");
        lineSelectionData.addElement("sensical ");
        lineSelectionData.addElement("(non-");
        lineSelectionData.addElement("important) ");
        lineSelectionData.addElement("sentence. ");

        lineSelectionData.addElement("Hi  ");
        lineSelectionData.addElement("Hello ");
        lineSelectionData.addElement("How\n");
        lineSelectionData.addElement("are\r");
        lineSelectionData.addElement("you\u2028");
        lineSelectionData.addElement("fine.\t");
        lineSelectionData.addElement("good.  ");

        lineSelectionData.addElement("Now\r");
        lineSelectionData.addElement("is\n");
        lineSelectionData.addElement("the\r\n");
        lineSelectionData.addElement("time\n");
        lineSelectionData.addElement("\r");
        lineSelectionData.addElement("for\r");
        lineSelectionData.addElement("\r");
        lineSelectionData.addElement("all");

        generalIteratorTest(lineBreak, lineSelectionData);
    }

    /**
     * @bug 4068133
     */
    public void TestBug4068133() {
        Vector lineSelectionData = new Vector();

        lineSelectionData.addElement("\u96f6");
        lineSelectionData.addElement("\u4e00\u3002");
        lineSelectionData.addElement("\u4e8c\u3001");
        lineSelectionData.addElement("\u4e09\u3002\u3001");
        lineSelectionData.addElement("\u56db\u3001\u3002\u3001");
        lineSelectionData.addElement("\u4e94,");
        lineSelectionData.addElement("\u516d.");
        lineSelectionData.addElement("\u4e03.\u3001,\u3002");
        lineSelectionData.addElement("\u516b");

        generalIteratorTest(lineBreak, lineSelectionData);
    }

    /**
     * @bug 4086052
     */
    public void TestBug4086052() {
        Vector lineSelectionData = new Vector();

        lineSelectionData.addElement("foo\u00a0bar ");
//        lineSelectionData.addElement("foo\ufeffbar");

        generalIteratorTest(lineBreak, lineSelectionData);
    }

    /**
     * @bug 4097920
     */
    public void TestBug4097920() {
        Vector lineSelectionData = new Vector();

        lineSelectionData.addElement("dog,cat,mouse ");
        lineSelectionData.addElement("(one)");
        lineSelectionData.addElement("(two)\n");
        generalIteratorTest(lineBreak, lineSelectionData);
    }

    /**
     * @bug 4035266
     */
    public void TestBug4035266() {
        try {
            RuleBasedBreakIterator_Old olb = (RuleBasedBreakIterator_Old)lineBreak;
            Vector lineSelectionData = new Vector();
            
            lineSelectionData.addElement("The ");
            lineSelectionData.addElement("balance ");
            lineSelectionData.addElement("is ");
            lineSelectionData.addElement("$-23,456.78, ");
            lineSelectionData.addElement("not ");
            lineSelectionData.addElement("-$32,456.78!\n");
            
            generalIteratorTest(lineBreak, lineSelectionData);
        }
        catch (ClassCastException e) {
            logln("New Break Iterator, skipping old test");
        }   
}

    /**
     * @bug 4098467
     */
    public void TestBug4098467Lines() {
        try {
            RuleBasedBreakIterator_Old olb = (RuleBasedBreakIterator_Old)lineBreak;
            Vector lineSelectionData = new Vector();
            
            // What follows is a string of Korean characters (I found it in the Yellow Pages
            // ad for the Korean Presbyterian Church of San Francisco, and I hope I transcribed
            // it correctly), first as precomposed syllables, and then as conjoining jamo.
            // Both sequences should be semantically identical and break the same way.
            // precomposed syllables...
            lineSelectionData.addElement("\uc0c1\ud56d ");
            lineSelectionData.addElement("\ud55c\uc778 ");
            lineSelectionData.addElement("\uc5f0\ud569 ");
            lineSelectionData.addElement("\uc7a5\ub85c\uad50\ud68c ");
            // conjoining jamo...
            lineSelectionData.addElement("\u1109\u1161\u11bc\u1112\u1161\u11bc ");
            lineSelectionData.addElement("\u1112\u1161\u11ab\u110b\u1175\u11ab ");
            lineSelectionData.addElement("\u110b\u1167\u11ab\u1112\u1161\u11b8 ");
            lineSelectionData.addElement("\u110c\u1161\u11bc\u1105\u1169\u1100\u116d\u1112\u116c");
            
            generalIteratorTest(lineBreak, lineSelectionData);
        }
        catch (ClassCastException e) {
            logln("New Break Iterator, skipping old test");
        }   
    }

    public void TestThaiLineBreak() {
        Vector lineSelectionData = new Vector();

        // \u0e2f-- the Thai paiyannoi character-- isn't a letter.  It's a symbol that
        // represents elided letters at the end of a long word.  It should be bound to
        // the end of the word and not treated as an independent punctuation mark.
        lineSelectionData.addElement("\u0e2a\u0e16\u0e32\u0e19\u0e35\u0e2f");
        lineSelectionData.addElement("\u0e08\u0e30");
        lineSelectionData.addElement("\u0e23\u0e30\u0e14\u0e21");
        lineSelectionData.addElement("\u0e40\u0e08\u0e49\u0e32");
//        lineSelectionData.addElement("\u0e2b\u0e19\u0e49\u0e32");
//        lineSelectionData.addElement("\u0e17\u0e35\u0e48");
// I think the above two lines are the preferred reading of this text, but our current
// dictionary yields the following:
lineSelectionData.addElement("\u0e2b\u0e16\u0e49\u0e32\u0e17\u0e35\u0e48");
        lineSelectionData.addElement("\u0e2d\u0e2d\u0e01");
        lineSelectionData.addElement("\u0e21\u0e32");
        lineSelectionData.addElement("\u0e40\u0e23\u0e48\u0e07");
        lineSelectionData.addElement("\u0e23\u0e30\u0e1a\u0e32\u0e22");
        lineSelectionData.addElement("\u0e2d\u0e22\u0e48\u0e32\u0e07");
        lineSelectionData.addElement("\u0e40\u0e15\u0e47\u0e21");

        // the one time where the paiyannoi occurs somewhere other than at the end
        // of a word is in the Thai abbrevation for "etc.", which both begins and
        // ends with a paiyannoi
        lineSelectionData.addElement("\u0e2f\u0e25\u0e2f");
        lineSelectionData.addElement("\u0e17\u0e35\u0e48");
        lineSelectionData.addElement("\u0e19\u0e31\u0e49\u0e19");

        generalIteratorTest(BreakIterator.getLineInstance(new Locale("th", "", "")),
                lineSelectionData);
    }

    public void TestMixedThaiLineBreak() {
        Vector lineSelectionData = new Vector();

        // Arabic numerals should always be separated from surrounding Thai text
/*
        lineSelectionData.addElement("\u0e04\u0e48\u0e32");
        lineSelectionData.addElement("\u0e40\u0e07\u0e34\u0e19");
        lineSelectionData.addElement("\u0e1a\u0e32\u0e17");
        lineSelectionData.addElement("\u0e41\u0e15\u0e30");
        lineSelectionData.addElement("\u0e23\u0e30\u0e14\u0e31\u0e1a");
        lineSelectionData.addElement("39");
        lineSelectionData.addElement("\u0e1a\u0e32\u0e17 ");

        // words in non-Thai scripts should always be separated from surrounding Thai text
        lineSelectionData.addElement("\u0e17\u0e14");
        lineSelectionData.addElement("\u0e2a\u0e2d\u0e1a");
        lineSelectionData.addElement("Java");
        lineSelectionData.addElement("\u0e1a\u0e19");
        lineSelectionData.addElement("\u0e40\u0e04\u0e23\u0e37\u0e48\u0e2d\u0e07");
        lineSelectionData.addElement("\u0e44\u0e2d\u0e1a\u0e35\u0e40\u0e2d\u0e47\u0e21 ");

        // Thai numerals should always be separated from the text surrounding them
        lineSelectionData.addElement("\u0e04\u0e48\u0e32");
        lineSelectionData.addElement("\u0e40\u0e07\u0e34\u0e19");
        lineSelectionData.addElement("\u0e1a\u0e32\u0e17");
        lineSelectionData.addElement("\u0e41\u0e15\u0e30");
        lineSelectionData.addElement("\u0e23\u0e30\u0e14\u0e31\u0e1a");
        lineSelectionData.addElement("\u0e53\u0e59");
        lineSelectionData.addElement("\u0e1a\u0e32\u0e17 ");

        // Thai text should interact correctly with punctuation and symbols
        lineSelectionData.addElement("\u0e44\u0e2d\u0e1a\u0e35\u0e40\u0e2d\u0e47\u0e21");
//        lineSelectionData.addElement("(\u0e1b\u0e23\u0e30\u0e40\u0e17\u0e28");
//        lineSelectionData.addElement("\u0e44\u0e17\u0e22)");
// I think the above lines represent the preferred reading for this text, but our current
// dictionary file yields the following:
lineSelectionData.addElement("(\u0e1b\u0e23\u0e30\u0e40\u0e17\u0e28\u0e44\u0e17\u0e22)");
        lineSelectionData.addElement("\u0e08\u0e33\u0e01\u0e31\u0e14");
        lineSelectionData.addElement("\u0e40\u0e1b\u0e34\u0e14");
        lineSelectionData.addElement("\u0e15\u0e31\u0e27\"");
*/
        lineSelectionData.addElement("\u0e2e\u0e32\u0e23\u0e4c\u0e14\u0e14\u0e34\u0e2a\u0e01\u0e4c\"");
        lineSelectionData.addElement("\u0e23\u0e38\u0e48\u0e19");
        lineSelectionData.addElement("\u0e43\u0e2b\u0e21\u0e48");
        lineSelectionData.addElement("\u0e40\u0e14\u0e37\u0e2d\u0e19\u0e21\u0e34.");
        lineSelectionData.addElement("\u0e22.");
        lineSelectionData.addElement("\u0e19\u0e35\u0e49");
        lineSelectionData.addElement("\u0e23\u0e32\u0e04\u0e32");
        lineSelectionData.addElement("$200");
        lineSelectionData.addElement("\u0e40\u0e17\u0e48\u0e32");
        lineSelectionData.addElement("\u0e19\u0e31\u0e49\u0e19 ");
        lineSelectionData.addElement("(\"\u0e2e\u0e32\u0e23\u0e4c\u0e14\u0e14\u0e34\u0e2a\u0e01\u0e4c\").");

        generalIteratorTest(BreakIterator.getLineInstance(new Locale("th", "", "")),
                lineSelectionData);
    }

    public void TestMaiyamok() {
        Vector lineSelectionData = new Vector();

        // the Thai maiyamok character is a shorthand symbol that means "repeat the previous
        // word".  Instead of appearing as a word unto itself, however, it's kept together
        // with the word before it
        lineSelectionData.addElement("\u0e44\u0e1b\u0e46");
        lineSelectionData.addElement("\u0e21\u0e32\u0e46");
        lineSelectionData.addElement("\u0e23\u0e30\u0e2b\u0e27\u0e48\u0e32\u0e07");
        lineSelectionData.addElement("\u0e01\u0e23\u0e38\u0e07\u0e40\u0e17\u0e1e");
        lineSelectionData.addElement("\u0e41\u0e25\u0e30");
        lineSelectionData.addElement("\u0e40\u0e03\u0e35\u0e22\u0e07");
        lineSelectionData.addElement("\u0e43\u0e2b\u0e21\u0e48");

        Locale loc = new Locale("th", "", "");
        BreakIterator bi = BreakIterator.getLineInstance(loc);      
        generalIteratorTest(bi, lineSelectionData);
    }

    /**
     * @bug 4117554
     */
    public void TestBug4117554Lines() {
        Vector lineSelectionData = new Vector();

        // Fullwidth .!? should be treated as postJwrd
        lineSelectionData.addElement("\u4e01\uff0e");
        lineSelectionData.addElement("\u4e02\uff01");
        lineSelectionData.addElement("\u4e03\uff1f");

        generalIteratorTest(lineBreak, lineSelectionData);
    }

    public void TestLettersAndDigits() {
        // a character sequence such as "X11" or "30F3" or "native2ascii" should
        // be kept together as a single word
        Vector lineSelectionData = new Vector();

        lineSelectionData.addElement("X11 ");
        lineSelectionData.addElement("30F3 ");
        lineSelectionData.addElement("native2ascii");

        generalIteratorTest(lineBreak, lineSelectionData);
    }

    /**
     * @bug 4217703
     */
    public void TestBug4217703() {
        try {
            RuleBasedBreakIterator_Old olb = (RuleBasedBreakIterator_Old)lineBreak;
            Vector lineSelectionData = new Vector();
            
            // There shouldn't be a line break between sentence-ending punctuation
            // and a closing quote
            lineSelectionData.addElement("He ");
            lineSelectionData.addElement("said ");
            lineSelectionData.addElement("\"Go!\"  ");
            lineSelectionData.addElement("I ");
            lineSelectionData.addElement("went.  ");
            
            lineSelectionData.addElement("Hashtable$Enumeration ");
            lineSelectionData.addElement("getText().");
            lineSelectionData.addElement("getIndex()");
            
            generalIteratorTest(lineBreak, lineSelectionData);
        }
        catch (ClassCastException e) {
            logln("New Break Iterator, skipping old test");
        }   
}

    private static final String graveS = "S\u0300";
    private static final String acuteBelowI = "i\u0317";
    private static final String acuteE = "e\u0301";
    private static final String circumflexA = "a\u0302";
    private static final String tildeE = "e\u0303";

    public void TestCharacterBreak() {
        Vector characterSelectionData = new Vector();

        characterSelectionData.addElement(graveS);
        characterSelectionData.addElement(acuteBelowI);
        characterSelectionData.addElement("m");
        characterSelectionData.addElement("p");
        characterSelectionData.addElement("l");
        characterSelectionData.addElement(acuteE);
        characterSelectionData.addElement(" ");
        characterSelectionData.addElement("s");
        characterSelectionData.addElement(circumflexA);
        characterSelectionData.addElement("m");
        characterSelectionData.addElement("p");
        characterSelectionData.addElement("l");
        characterSelectionData.addElement(tildeE);
        characterSelectionData.addElement(".");
        characterSelectionData.addElement("w");
        characterSelectionData.addElement(circumflexA);
        characterSelectionData.addElement("w");
        characterSelectionData.addElement("a");
        characterSelectionData.addElement("f");
        characterSelectionData.addElement("q");
        characterSelectionData.addElement("\n");
        characterSelectionData.addElement("\r");
        characterSelectionData.addElement("\r\n");
        characterSelectionData.addElement("\n");

        generalIteratorTest(characterBreak, characterSelectionData);
    }

    /**
     * @bug 4098467
     */
    public void TestBug4098467Characters() {
        Vector characterSelectionData = new Vector();

        // What follows is a string of Korean characters (I found it in the Yellow Pages
        // ad for the Korean Presbyterian Church of San Francisco, and I hope I transcribed
        // it correctly), first as precomposed syllables, and then as conjoining jamo.
        // Both sequences should be semantically identical and break the same way.
        // precomposed syllables...
        characterSelectionData.addElement("\uc0c1");
        characterSelectionData.addElement("\ud56d");
        characterSelectionData.addElement(" ");
        characterSelectionData.addElement("\ud55c");
        characterSelectionData.addElement("\uc778");
        characterSelectionData.addElement(" ");
        characterSelectionData.addElement("\uc5f0");
        characterSelectionData.addElement("\ud569");
        characterSelectionData.addElement(" ");
        characterSelectionData.addElement("\uc7a5");
        characterSelectionData.addElement("\ub85c");
        characterSelectionData.addElement("\uad50");
        characterSelectionData.addElement("\ud68c");
        characterSelectionData.addElement(" ");
        // conjoining jamo...
        characterSelectionData.addElement("\u1109\u1161\u11bc");
        characterSelectionData.addElement("\u1112\u1161\u11bc");
        characterSelectionData.addElement(" ");
        characterSelectionData.addElement("\u1112\u1161\u11ab");
        characterSelectionData.addElement("\u110b\u1175\u11ab");
        characterSelectionData.addElement(" ");
        characterSelectionData.addElement("\u110b\u1167\u11ab");
        characterSelectionData.addElement("\u1112\u1161\u11b8");
        characterSelectionData.addElement(" ");
        characterSelectionData.addElement("\u110c\u1161\u11bc");
        characterSelectionData.addElement("\u1105\u1169");
        characterSelectionData.addElement("\u1100\u116d");
        characterSelectionData.addElement("\u1112\u116c");

        generalIteratorTest(characterBreak, characterSelectionData);
    }

    public void TestTitleBreak()
    {
        Vector titleData = new Vector();
        titleData.addElement("   ");
        titleData.addElement("This ");
        titleData.addElement("is ");
        titleData.addElement("a ");
        titleData.addElement("simple ");
        titleData.addElement("sample ");
        titleData.addElement("sentence. ");
        titleData.addElement("This ");

        generalIteratorTest(titleBreak, titleData);
    }



    /*
     * @bug 4153072
     */
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

    public void TestBug4146175Sentences() {
        try {
            RuleBasedBreakIterator_Old olb = (RuleBasedBreakIterator_Old)lineBreak;
            Vector sentenceSelectionData = new Vector();
            
            // break between periods and opening punctuation even when there's no
            // intervening space
            sentenceSelectionData.addElement("end.");
            sentenceSelectionData.addElement("(This is\u2029");
            
            // treat the fullwidth period as an unambiguous sentence terminator
            sentenceSelectionData.addElement("\u7d42\u308f\u308a\uff0e");
            sentenceSelectionData.addElement("\u300c\u3053\u308c\u306f");
            
            generalIteratorTest(sentenceBreak, sentenceSelectionData);
        }
        catch (ClassCastException e) {
            logln("New Break Iterator, skipping old test");
        }   
    }

    public void TestBug4146175Lines() {
        Vector lineSelectionData = new Vector();

        // the fullwidth comma should stick to the preceding Japanese character
        lineSelectionData.addElement("\u7d42\uff0c");
        lineSelectionData.addElement("\u308f");

        generalIteratorTest(lineBreak, lineSelectionData);
    }

    public void TestBug4214367() {
        try {
            RuleBasedBreakIterator_Old olb = (RuleBasedBreakIterator_Old)wordBreak;
            Vector wordSelectionData = new Vector();
            
            // the hiragana and katakana iteration marks and the long vowel mark
            // are not being treated correctly by the word-break iterator
            wordSelectionData.addElement("\u3042\u3044\u309d\u3042\u309e\u3042\u30fc\u3042");
            wordSelectionData.addElement("\u30a2\u30a4\u30fd\u30a2\u30fe\u30a2\u30fc\u30a2");
            
            generalIteratorTest(wordBreak, wordSelectionData);
        }
        catch (ClassCastException e) {
            logln("New Break Iterator, skipping old test");
        }   
    }

    private static final String cannedTestChars
        = "\u0000\u0001\u0002\u0003\u0004 !\"#$%&()+-01234<=>ABCDE[]^_`abcde{}|\u00a0\u00a2"
        + "\u00a3\u00a4\u00a5\u00a6\u00a7\u00a8\u00a9\u00ab\u00ad\u00ae\u00af\u00b0\u00b2\u00b3"
        + "\u00b4\u00b9\u00bb\u00bc\u00bd\u02b0\u02b1\u02b2\u02b3\u02b4\u0300\u0301\u0302\u0303"
        + "\u0304\u05d0\u05d1\u05d2\u05d3\u05d4\u0903\u093e\u093f\u0940\u0949\u0f3a\u0f3b\u2000"
        + "\u2001\u2002\u200c\u200d\u200e\u200f\u2010\u2011\u2012\u2028\u2029\u202a\u203e\u203f"
        + "\u2040\u20dd\u20de\u20df\u20e0\u2160\u2161\u2162\u2163\u2164";

    public void TestSentenceInvariants()
    {
        BreakIterator e = BreakIterator.getSentenceInstance();
        doOtherInvariantTest(e, cannedTestChars + ".,\u3001\u3002\u3041\u3042\u3043\ufeff");
    }

    public void TestWordInvariants()
    {
        BreakIterator e = BreakIterator.getWordInstance();
        try {
            RuleBasedBreakIterator_Old olb = (RuleBasedBreakIterator_Old)e;
            doBreakInvariantTest(e, cannedTestChars + "\',.\u3041\u3042\u3043\u309b\u309c\u30a1\u30a2"
                    + "\u30a3\u4e00\u4e01\u4e02");
            doOtherInvariantTest(e, cannedTestChars + "\',.\u3041\u3042\u3043\u309b\u309c\u30a1\u30a2"
                    + "\u30a3\u4e00\u4e01\u4e02");
        }
        catch (ClassCastException ex) {
            logln("New Break Iterator, skipping old test");
        }   
    }

    public void TestLineInvariants()
    {
        BreakIterator e = BreakIterator.getLineInstance();
        try {
            RuleBasedBreakIterator_Old olb = (RuleBasedBreakIterator_Old)e;
            String testChars = cannedTestChars + ".,;:\u3001\u3002\u3041\u3042\u3043\u3044\u3045"
            + "\u30a3\u4e00\u4e01\u4e02";
            doBreakInvariantTest(e, testChars);
            doOtherInvariantTest(e, testChars);
            
            int errorCount = 0;
            
            // in addition to the other invariants, a line-break iterator should make sure that:
            // it doesn't break around the non-breaking characters
            String noBreak = "\u00a0\u2007\u2011\ufeff";
            StringBuffer work = new StringBuffer("aaa");
            for (int i = 0; i < testChars.length(); i++) {
                char c = testChars.charAt(i);
                if (c == '\r' || c == '\n' || c == '\u2029' || c == '\u2028' || c == '\u0003')
                    continue;
                work.setCharAt(0, c);
                for (int j = 0; j < noBreak.length(); j++) {
                    work.setCharAt(1, noBreak.charAt(j));
                    for (int k = 0; k < testChars.length(); k++) {
                        work.setCharAt(2, testChars.charAt(k));
                        e.setText(work.toString());
                        for (int l = e.first(); l != BreakIterator.DONE; l = e.next())
                            if (l == 1 || l == 2) {
                                errln("Got break between U+" + Integer.toHexString((int)
                                        (work.charAt(l - 1))) + " and U+" + Integer.toHexString(
                                                (int)(work.charAt(l))));
                                errorCount++;
                                if (errorCount >= 75)
                                    return;
                            }
                    }
                }
            }
            
            // it does break after dashes (unless they're followed by a digit, a non-spacing mark,
            // a currency symbol, a space, a format-control character, a regular control character,
            // a line or paragraph separator, or another dash)
            String dashes = "-\u00ad\u2010\u2012\u2013\u2014";
            for (int i = 0; i < testChars.length(); i++) {
                work.setCharAt(0, testChars.charAt(i));
                for (int j = 0; j < dashes.length(); j++) {
                    work.setCharAt(1, dashes.charAt(j));
                    for (int k = 0; k < testChars.length(); k++) {
                        char c = testChars.charAt(k);
                        if (Character.getType(c) == Character.DECIMAL_DIGIT_NUMBER ||
                                Character.getType(c) == Character.OTHER_NUMBER ||
                                Character.getType(c) == Character.NON_SPACING_MARK ||
                                Character.getType(c) == Character.ENCLOSING_MARK ||
                                Character.getType(c) == Character.CURRENCY_SYMBOL ||
                                Character.getType(c) == Character.DASH_PUNCTUATION ||
                                Character.getType(c) == Character.SPACE_SEPARATOR ||
                                Character.getType(c) == Character.FORMAT ||
                                Character.getType(c) == Character.CONTROL ||
                                c == '\n' || c == '\r' || c == '\u2028' || c == '\u2029' ||
                                c == '\u0003' || c == '\u2007' || c == '\u2011' ||
                                c == '\ufeff')
                            continue;
                        work.setCharAt(2, c);
                        e.setText(work.toString());
                        boolean saw2 = false;
                        for (int l = e.first(); l != BreakIterator.DONE; l = e.next())
                            if (l == 2)
                                saw2 = true;
                        if (!saw2) {
                            errln("Didn't get break between U+" + Integer.toHexString((int)
                                    (work.charAt(1))) + " and U+" + Integer.toHexString(
                                            (int)(work.charAt(2))));
                            errorCount++;
                            if (errorCount >= 75)
                                return;
                        }
                    }
                }
            }
        }
        catch (ClassCastException ex) {
            logln("New Break Iterator, skipping old test");
        }   
    }

    public void TestCharacterInvariants()
        {
            BreakIterator e = BreakIterator.getCharacterInstance();
            try {
                RuleBasedBreakIterator_Old olb = (RuleBasedBreakIterator_Old)e;
                doBreakInvariantTest(e, cannedTestChars + "\u1100\u1101\u1102\u1160\u1161\u1162\u11a8"
                        + "\u11a9\u11aa");
                doOtherInvariantTest(e, cannedTestChars + "\u1100\u1101\u1102\u1160\u1161\u1162\u11a8"
                        + "\u11a9\u11aa");
            }
            catch (ClassCastException ex) {
                logln("New Break Iterator, skipping old test");
            }   
        }

    public void TestEmptyString()
    {
        String text = "";
        Vector x = new Vector();
        x.addElement(text);

        generalIteratorTest(lineBreak, x);
    }

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
     * @bug 4095322
     */
    public void TestJapaneseLineBreak()
    {
        StringBuffer testString = new StringBuffer("\u4e00x\u4e8c");
        String precedingChars = "([{\u00ab$\u00a5\u00a3\u00a4\u2018\u201a\u201c\u201e\u201b\u201f";
        String followingChars = ")]}\u00bb!%,.\u3001\u3002\u3063\u3083\u3085\u3087\u30c3\u30e3\u30e5\u30e7\u30fc:;\u309b\u309c\u3005\u309d\u309e\u30fd\u30fe\u2019\u201d\u00b0\u2032\u2033\u2034\u2030\u2031\u2103\u2109\u00a2\u0300\u0301\u0302";
        BreakIterator iter = BreakIterator.getLineInstance(Locale.JAPAN);
        try {
            RuleBasedBreakIterator_Old olb = (RuleBasedBreakIterator_Old)iter;
            
            for (int i = 0; i < precedingChars.length(); i++) {
                testString.setCharAt(1, precedingChars.charAt(i));
                iter.setText(testString.toString());
                int j = iter.first();
                if (j != 0)
                    errln("ja line break failure: failed to start at 0");
                j = iter.next();
                if (j != 1)
                    errln("ja line break failure: failed to stop before '" + precedingChars.charAt(i)
                            + "' (" + ((int)(precedingChars.charAt(i))) + ")");
                j = iter.next();
                if (j != 3)
                    errln("ja line break failure: failed to skip position after '" + precedingChars.charAt(i)
                            + "' (" + ((int)(precedingChars.charAt(i))) + ")");
            }
            
            for (int i = 0; i < followingChars.length(); i++) {
                testString.setCharAt(1, followingChars.charAt(i));
                iter.setText(testString.toString());
                int j = iter.first();
                if (j != 0)
                    errln("ja line break failure: failed to start at 0");
                j = iter.next();
                if (j != 2)
                    errln("ja line break failure: failed to skip position before '" + followingChars.charAt(i)
                            + "' (" + ((int)(followingChars.charAt(i))) + ")");
                j = iter.next();
                if (j != 3)
                    errln("ja line break failure: failed to stop after '" + followingChars.charAt(i)
                            + "' (" + ((int)(followingChars.charAt(i))) + ")");
            }
        }
        catch (ClassCastException e) {
            logln("New Break Iterator, skipping old test");
        }   
    }

    /**
     * Bug 4638433
     */
        public void TestLineBreakBasedOnUnicode3_0_0() {
            BreakIterator iter;
            int i;
            
            /* Latin Extend-B characters
             * 0x0218-0x0233 which have been added since Unicode 3.0.0.
             */
            iter = BreakIterator.getWordInstance(Locale.US);
            try {
                RuleBasedBreakIterator_Old olb = (RuleBasedBreakIterator_Old)iter;
                iter.setText("\u0216\u0217\u0218\u0219\u021A");
                i = iter.first();
                i = iter.next();
                if (i != 5) {   
                    errln("Word break failure: failed to stop at 5 and bounded at " + i);
                }
                
                
                iter = BreakIterator.getLineInstance(Locale.US);
                
                /* <Three(Nd)><Two(Nd)><Low Double Prime Quotation Mark(Pe)><One(Nd)>
                 * \u301f has changed its category from Ps to Pe since Unicode 2.1.
                 */
                iter.setText("32\u301f1");
                i = iter.first();
                i = iter.next();
                if (i != 3) {   
                    errln("Line break failure: failed to skip before \\u301F(Pe) at 3 and bounded at " + i);
                }
                
                /* Mongolian <Letter A(Lo)><Todo Soft Hyphen(Pd)><Letter E(Lo)>
                 * which have been added since Unicode 3.0.0.
                 */
                iter.setText("\u1820\u1806\u1821");
                i = iter.first();
                i = iter.next();
                if (i != 2) {   
                    errln("Mongolian line break failure: failed to skip position before \\u1806(Pd) at 2 and bounded at " + i);
                }
                
                /* Khmer <ZERO(Nd)><Currency Symbol(Sc)><ONE(Nd)> which have
                 * been added since Unicode 3.0.0.
                 */
                /*
                 * Richard: fail to pass, refer to #3550
                 iter.setText("\u17E0\u17DB\u17E1");
                 i = iter.first();
                 i = iter.next();
                 if (i != 1) {   
                 errln("Khmer line break failure: failed to stop before \\u17DB(Sc) at 1 and bounded at " + i);
                 }
                 i = iter.next();
                 if (i != 3) {   
                 errln("Khmer line break failure: failed to skip position after \\u17DB(Sc) at 3 and bounded at " + i);
                 }*/
                
                /* Ogham <Letter UR(Lo)><Space Mark(Zs)><Letter OR(Lo)> which have
                 * been added since Unicode 3.0.0.
                 */
                iter.setText("\u1692\u1680\u1696");
                i = iter.first();
                i = iter.next();
                if (i != 2) {   
                    errln("Ogham line break failure: failed to skip postion before \\u1680(Zs) at 2 and bounded at " + i);
                }
                
                
                // Confirm changes in BreakIteratorRules_th.java have been reflected.
                iter = BreakIterator.getLineInstance(new Locale("th", ""));
                
                /* Thai <Seven(Nd)>
                 *      <Left Double Quotation Mark(Pi)>
                 *      <Five(Nd)>
                 *      <Right Double Quotation Mark(Pf)>
                 *      <Three(Nd)>
                 */
                iter.setText("\u0E57\u201C\u0E55\u201D\u0E53");
                i = iter.first();
                i = iter.next();
                if (i != 1) {   
                    errln("Thai line break failure: failed to stop before \\u201C(Pi) at 1 and bounded at " + i);
                }
                i = iter.next();
                if (i != 4) {   
                    errln("Thai line break failure: failed to stop after \\u201D(Pf) at 4 and bounded at " + i);
                }
            }
            catch (ClassCastException e) {
                logln("New Break Iterator, skipping old test");
            }   
        }
    
    /**
     * @bug 4068137
     */
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
     * Test Thai word break using generalIteratorTest()
     **/
    public void TestThaiWordBreak() {
        Vector thaiWordSelection = new Vector();

        thaiWordSelection.addElement("\u0E1A\u0E17"); //2
        thaiWordSelection.addElement("\u0E17\u0E35\u0E48"); //5
        thaiWordSelection.addElement("\u0E51"); //6
        thaiWordSelection.addElement("\u0E1E\u0E32\u0E22\u0E38"); //10
        thaiWordSelection.addElement("\u0E44\u0E0B\u0E42\u0E04\u0E25\u0E19"); //16
        thaiWordSelection.addElement("\r\n"); //18

        // This is the correct result
        //thaiWordSelection.addElement(("\u0E42\u0E14\u0E42\u0E23\u0E18\u0E35")); //24
        //thaiWordSelection.addElement(("\u0E2D\u0E32\u0E28\u0E31\u0E22")); //29

        // and this is what the dictionary does...
        thaiWordSelection.addElement("\u0E42\u0E14"); // 20
        thaiWordSelection.addElement("\u0E42\u0E23\u0E18\u0E35\u0E2D\u0E32\u0E28\u0E31\u0E22"); //29

        thaiWordSelection.addElement("\u0E2D\u0E22\u0E39\u0E48"); //33

        // This is the correct result
        //thaiWordSelection.addElement("\u0E17\u0E48\u0E32\u0E21"); //37
        //thaiWordSelection.addElement("\u0E01\u0E25\u0E32\u0E07"); //41

        // and this is what the dictionary does
        thaiWordSelection.addElement("\u0E17\u0E48\u0E32\u0E21\u0E01\u0E25\u0E32\u0E07"); //41

        thaiWordSelection.addElement("\u0E17\u0E38\u0E48\u0E07"); //45
        thaiWordSelection.addElement("\u0E43\u0E2B\u0E0D\u0E48"); //49
        thaiWordSelection.addElement("\u0E43\u0E19"); //51

        // This is the correct result
        //thaiWordSelection.addElement("\u0E41\u0E04\u0E19\u0E0B\u0E31\u0E2A"); //57
        //thaiWordSelection.addElement("\u0E01\u0E31\u0E1A"); //60

        // and this is what the dictionary does
        thaiWordSelection.addElement("\u0E41\u0E04\u0E19"); // 54
        thaiWordSelection.addElement("\u0E0B\u0E31\u0E2A\u0E01\u0E31\u0E1A"); //60

        thaiWordSelection.addElement("\u0E25\u0E38\u0E07"); //63

        // This is the correct result
        //thaiWordSelection.addElement("\u0E40\u0E2E\u0E19\u0E23\u0E35"); //68
        //thaiWordSelection.addElement("\u0E0A\u0E32\u0E27"); //71
        //thaiWordSelection.addElement("\u0E44\u0E23\u0E48"); //74
        //thaiWordSelection.addElement("\u0E41\u0E25\u0E30"); //77

        // and this is what the dictionary does
        thaiWordSelection.addElement("\u0E40\u0E2E"); // 65
        thaiWordSelection.addElement("\u0E19\u0E23\u0E35\u0E0A\u0E32\u0E27\u0E44\u0E23\u0E48\u0E41\u0E25\u0E30"); //77

        BreakIterator e = BreakIterator.getWordInstance(new Locale("th","",""));

        generalIteratorTest(e, thaiWordSelection);
    }
    
    /**
     * Bug 4450804
     */
    public void TestLineBreakContractions() {
        Vector expected = new Vector();
        expected.add("These ");
        expected.add("are ");
        expected.add("'foobles'. ");
        expected.add("Don't ");
        expected.add("you ");
        expected.add("like ");
        expected.add("them?");
        generalIteratorTest(lineBreak, expected);
    }
}

