/*
 *******************************************************************************
 * Copyright (C) 1996-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.rbbi;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.StringCharacterIterator;
import java.util.Locale;
import java.util.Vector;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.DictionaryBasedBreakIterator;

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

    }
    protected void init(){
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


    public void TestBug4146175Lines() {
        Vector lineSelectionData = new Vector();

        // the fullwidth comma should stick to the preceding Japanese character
        lineSelectionData.addElement("\u7d42\uff0c");
        lineSelectionData.addElement("\u308f");

        generalIteratorTest(lineBreak, lineSelectionData);
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

    /**
     * Ticket#5615
     */
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
    
    /*
     * Tests the constructors public DictionaryBasedBreakIterator(String rules, ... public
     * DictionaryBasedBreakIterator(InputStream compiledRules, ...
     */
    public void TestDictionaryBasedBreakIterator() throws IOException {
        // The following class allows the testing of the constructor
        // public DictionaryBasedBreakIterator(String rules, ...
        class TestDictionaryBasedBreakIterator extends DictionaryBasedBreakIterator {
            public TestDictionaryBasedBreakIterator(InputStream is) throws IOException {
                super("", is);
            }
        }
        try {
            @SuppressWarnings("unused")
            TestDictionaryBasedBreakIterator td = new TestDictionaryBasedBreakIterator(null);
            errln("DictionaryBasedBreakIterator constructor is suppose to return an "
                    + "exception for an empty string.");
        } catch (Exception e) {
        }
        
        try {
            File file = File.createTempFile("dummy", "");
            FileInputStream fis = new FileInputStream(file);
            DataInputStream dis = new DataInputStream(fis);
            @SuppressWarnings("unused")
            TestDictionaryBasedBreakIterator td = new TestDictionaryBasedBreakIterator(dis);
            errln("DictionaryBasedBreakIterator constructor is suppose to return an "
                    + "exception for a temporary file with EOF.");
        } catch (Exception e) {
        }
        
        // The following class allows the testing of the constructor
        // public DictionaryBasedBreakIterator(InputStream compiledRules, ...
        class TestDictionaryBasedBreakIterator1 extends DictionaryBasedBreakIterator {
            public TestDictionaryBasedBreakIterator1() throws IOException {
                super((InputStream) null, (InputStream) null);
            }

        }
        try {
            @SuppressWarnings("unused")
            TestDictionaryBasedBreakIterator1 td1 = new TestDictionaryBasedBreakIterator1();
            errln("DictionaryBasedBreakIterator constructor is suppose to return an "
                    + "exception for an null input stream.");
        } catch (Exception e) {
        }
    }   
}