// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1996-2016, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.dev.test.rbbi;

//Regression testing of RuleBasedBreakIterator
//
//  TODO:  These tests should be mostly retired.
//          Much of the test data that was originally here was removed when the RBBI rules
//            were updated to match the Unicode boundary TRs, and the data was found to be invalid.
//          Much of the remaining data has been moved into the rbbitst.txt test data file,
//            which is common between ICU4C and ICU4J.  The remaining test data should also be moved,
//            or simply retired if it is no longer interesting.
import java.text.CharacterIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.RBBIDataWrapper;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.RuleBasedBreakIterator;
import com.ibm.icu.util.CodePointTrie;
import com.ibm.icu.util.ULocale;


@RunWith(JUnit4.class)
public class RBBITest extends TestFmwk {
    public RBBITest() {
    }



    @Test
   public void TestThaiDictionaryBreakIterator() {
       int position;
       int index;
       int result[] = { 1, 2, 5, 10, 11, 12, 11, 10, 5, 2, 1, 0 };
       char ctext[] = {
               0x0041, 0x0020,
               0x0E01, 0x0E32, 0x0E23, 0x0E17, 0x0E14, 0x0E25, 0x0E2D, 0x0E07,
               0x0020, 0x0041
               };
       String text = new String(ctext);

       ULocale locale = ULocale.createCanonical("th");
       BreakIterator b = BreakIterator.getWordInstance(locale);

       b.setText(text);

       index = 0;
       // Test forward iteration
       while ((position = b.next())!= BreakIterator.DONE) {
           if (position != result[index++]) {
               errln("Error with ThaiDictionaryBreakIterator forward iteration test at " + position + ".\nShould have been " + result[index-1]);
           }
       }

       // Test backward iteration
       while ((position = b.previous())!= BreakIterator.DONE) {
           if (position != result[index++]) {
               errln("Error with ThaiDictionaryBreakIterator backward iteration test at " + position + ".\nShould have been " + result[index-1]);
           }
       }

       //Test invalid sequence and spaces
       char text2[] = {
               0x0E01, 0x0E39, 0x0020, 0x0E01, 0x0E34, 0x0E19, 0x0E01, 0x0E38, 0x0E49, 0x0E07, 0x0020, 0x0E1B,
               0x0E34, 0x0E49, 0x0E48, 0x0E07, 0x0E2D, 0x0E22, 0x0E39, 0x0E48, 0x0E43, 0x0E19,
               0x0E16, 0x0E49, 0x0E33
       };
       int expectedWordResult[] = {
               2, 3, 6, 10, 11, 15, 17, 20, 22
       };
       int expectedLineResult[] = {
               3, 6, 11, 15, 17, 20, 22
       };
       BreakIterator brk = BreakIterator.getWordInstance(new ULocale("th"));
       brk.setText(new String(text2));
       position = index = 0;
       while ((position = brk.next()) != BreakIterator.DONE && position < text2.length) {
           if (position != expectedWordResult[index++]) {
               errln("Incorrect break given by thai word break iterator. Expected: " + expectedWordResult[index-1] + " Got: " + position);
           }
       }

       brk = BreakIterator.getLineInstance(new ULocale("th"));
       brk.setText(new String(text2));
       position = index = 0;
       while ((position = brk.next()) != BreakIterator.DONE && position < text2.length) {
           if (position != expectedLineResult[index++]) {
               errln("Incorrect break given by thai line break iterator. Expected: " + expectedLineResult[index-1] + " Got: " + position);
           }
       }
       // Improve code coverage
       if (brk.preceding(expectedLineResult[1]) != expectedLineResult[0]) {
           errln("Incorrect preceding position.");
       }
       if (brk.following(expectedLineResult[1]) != expectedLineResult[2]) {
           errln("Incorrect following position.");
       }
       int []fillInArray = new int[2];
       if (((RuleBasedBreakIterator)brk).getRuleStatusVec(fillInArray) != 1 || fillInArray[0] != 0) {
           errln("Error: Since getRuleStatusVec is not supported in DictionaryBasedBreakIterator, it should return 1 and fillInArray[0] == 0.");
       }
   }


   // TODO: Move these test cases to rbbitst.txt if they aren't there already, then remove this test. It is redundant.
    @Test
    public void TestTailoredBreaks() {
        class TBItem {
            private int     type;
            private ULocale locale;
            private String  text;
            private int[]   expectOffsets;
            TBItem(int typ, ULocale loc, String txt, int[] eOffs) {
                type          = typ;
                locale        = loc;
                text          = txt;
                expectOffsets = eOffs;
            }
            private static final int maxOffsetCount = 128;
            private boolean offsetsMatchExpected(int[] foundOffsets, int foundOffsetsLength) {
                if ( foundOffsetsLength != expectOffsets.length ) {
                    return false;
                }
                for (int i = 0; i < foundOffsetsLength; i++) {
                    if ( foundOffsets[i] != expectOffsets[i] ) {
                        return false;
                    }
                }
                return true;
            }
            private String formatOffsets(int[] offsets, int length) {
                StringBuffer buildString = new StringBuffer(4*maxOffsetCount);
                for (int i = 0; i < length; i++) {
                    buildString.append(" " + offsets[i]);
                }
                return buildString.toString();
            }

            public void doTest() {
                BreakIterator brkIter;
                switch( type ) {
                    case BreakIterator.KIND_CHARACTER: brkIter = BreakIterator.getCharacterInstance(locale); break;
                    case BreakIterator.KIND_WORD:      brkIter = BreakIterator.getWordInstance(locale); break;
                    case BreakIterator.KIND_LINE:      brkIter = BreakIterator.getLineInstance(locale); break;
                    case BreakIterator.KIND_SENTENCE:  brkIter = BreakIterator.getSentenceInstance(locale); break;
                    default: errln("Unsupported break iterator type " + type); return;
                }
                brkIter.setText(text);
                int[] foundOffsets = new int[maxOffsetCount];
                int offset, foundOffsetsCount = 0;
                // do forwards iteration test
                while ( foundOffsetsCount < maxOffsetCount && (offset = brkIter.next()) != BreakIterator.DONE ) {
                    foundOffsets[foundOffsetsCount++] = offset;
                }
                if ( !offsetsMatchExpected(foundOffsets, foundOffsetsCount) ) {
                    // log error for forwards test
                    String textToDisplay = (text.length() <= 16)? text: text.substring(0,16);
                    errln("For type " + type + " " + locale + ", text \"" + textToDisplay + "...\"" +
                            "; expect " + expectOffsets.length + " offsets:" + formatOffsets(expectOffsets, expectOffsets.length) +
                            "; found " + foundOffsetsCount + " offsets fwd:" + formatOffsets(foundOffsets, foundOffsetsCount) );
                } else {
                    // do backwards iteration test
                    --foundOffsetsCount; // back off one from the end offset
                    while ( foundOffsetsCount > 0 ) {
                        offset = brkIter.previous();
                        if ( offset != foundOffsets[--foundOffsetsCount] ) {
                            // log error for backwards test
                            String textToDisplay = (text.length() <= 16)? text: text.substring(0,16);
                            errln("For type " + type + " " + locale + ", text \"" + textToDisplay + "...\"" +
                                    "; expect " + expectOffsets.length + " offsets:" + formatOffsets(expectOffsets, expectOffsets.length) +
                                    "; found rev offset " + offset + " where expect " + foundOffsets[foundOffsetsCount] );
                            break;
                        }
                    }
                }
            }
        }
        // KIND_SENTENCE "el"
        final String elSentText     = "\u0391\u03B2, \u03B3\u03B4; \u0395 \u03B6\u03B7\u037E \u0398 \u03B9\u03BA. " +
                                      "\u039B\u03BC \u03BD\u03BE! \u039F\u03C0, \u03A1\u03C2? \u03A3";
        final int[]  elSentTOffsets = { 8, 14, 20, 27, 35, 36 };
        final int[]  elSentROffsets = {        20, 27, 35, 36 };
        // KIND_CHARACTER "th"
        final String thCharText     = "\u0E01\u0E23\u0E30\u0E17\u0E48\u0E2D\u0E21\u0E23\u0E08\u0E19\u0E32 " +
                                      "(\u0E2A\u0E38\u0E0A\u0E32\u0E15\u0E34-\u0E08\u0E38\u0E11\u0E32\u0E21\u0E32\u0E28) " +
                                      "\u0E40\u0E14\u0E47\u0E01\u0E21\u0E35\u0E1B\u0E31\u0E0D\u0E2B\u0E32 ";
        final int[]  thCharTOffsets = { 1, 2, 3, 5, 6, 7, 8, 9, 10, 11,
                                        12, 13, 15, 16, 17, 19, 20, 22, 23, 24, 25, 26, 27, 28,
                                        29, 30, 32, 33, 35, 37, 38, 39, 40, 41 };
        //starting in Unicode 6.1, root behavior should be the same as Thai above
        //final int[]  thCharROffsets = { 1,    3, 5, 6, 7, 8, 9,     11,
        //                                12, 13, 15,     17, 19, 20, 22,     24,     26, 27, 28,
        //                                29,     32, 33, 35, 37, 38,     40, 41 };

        final TBItem[] tests = {
            new TBItem( BreakIterator.KIND_SENTENCE,  new ULocale("el"),          elSentText,   elSentTOffsets   ),
            new TBItem( BreakIterator.KIND_SENTENCE,  ULocale.ROOT,               elSentText,   elSentROffsets   ),
            new TBItem( BreakIterator.KIND_CHARACTER, new ULocale("th"),          thCharText,   thCharTOffsets   ),
            new TBItem( BreakIterator.KIND_CHARACTER, ULocale.ROOT,               thCharText,   thCharTOffsets   ),
        };
        for (int iTest = 0; iTest < tests.length; iTest++) {
            tests[iTest].doTest();
        }
    }

    /* Tests the method public Object clone() */
    @Test
    public void TestClone() {
        RuleBasedBreakIterator rbbi = new RuleBasedBreakIterator(".;");
        try {
            rbbi.setText((CharacterIterator) null);
            if (((RuleBasedBreakIterator) rbbi.clone()).getText() != null)
                errln("RuleBasedBreakIterator.clone() was suppose to return "
                        + "the same object because fText is set to null.");
        } catch (Exception e) {
            errln("RuleBasedBreakIterator.clone() was not suppose to return " + "an exception.");
        }
    }

    /*
     * Tests the method public boolean equals(Object that)
     */
    @Test
    public void TestEquals() {
        RuleBasedBreakIterator rbbi = new RuleBasedBreakIterator(".;");
        RuleBasedBreakIterator rbbi1 = new RuleBasedBreakIterator(".;");

        // TODO: Tests when "if (fRData != other.fRData && (fRData == null || other.fRData == null))" is true

        // Tests when "if (fText == null || other.fText == null)" is true
        rbbi.setText((CharacterIterator) null);
        if (rbbi.equals(rbbi1)) {
            errln("RuleBasedBreakIterator.equals(Object) was not suppose to return "
                    + "true when the other object has a null fText.");
        }

        // Tests when "if (fText == null && other.fText == null)" is true
        rbbi1.setText((CharacterIterator) null);
        if (!rbbi.equals(rbbi1)) {
            errln("RuleBasedBreakIterator.equals(Object) was not suppose to return "
                    + "false when both objects has a null fText.");
        }

        // Tests when an exception occurs
        if (rbbi.equals(0)) {
            errln("RuleBasedBreakIterator.equals(Object) was suppose to return " + "false when comparing to integer 0.");
        }
        if (rbbi.equals(0.0)) {
            errln("RuleBasedBreakIterator.equals(Object) was suppose to return " + "false when comparing to float 0.0.");
        }
        if (rbbi.equals("0")) {
            errln("RuleBasedBreakIterator.equals(Object) was suppose to return "
                    + "false when comparing to string '0'.");
        }
    }

    /*
     * Tests the method public int first()
     */
    @Test
    public void TestFirst() {
        RuleBasedBreakIterator rbbi = new RuleBasedBreakIterator(".;");
        // Tests when "if (fText == null)" is true
        rbbi.setText((CharacterIterator) null);
        assertEquals("RuleBasedBreakIterator.first()", BreakIterator.DONE, rbbi.first());

        rbbi.setText("abc");
        assertEquals("RuleBasedBreakIterator.first()", 0, rbbi.first());
        assertEquals("RuleBasedBreakIterator.next()", 1, rbbi.next());
    }

    /*
     * Tests the method public int last()
     */
    @Test
    public void TestLast() {
        RuleBasedBreakIterator rbbi = new RuleBasedBreakIterator(".;");
        // Tests when "if (fText == null)" is true
        rbbi.setText((CharacterIterator) null);
        if (rbbi.last() != BreakIterator.DONE) {
            errln("RuleBasedBreakIterator.last() was suppose to return "
                    + "BreakIterator.DONE when the object has a null fText.");
        }
    }

    /*
     * Tests the method public int following(int offset)
     */
    @Test
    public void TestFollowing() {
        RuleBasedBreakIterator rbbi = new RuleBasedBreakIterator(".;");
        // Tests when "else if (offset < fText.getBeginIndex())" is true
        rbbi.setText("dummy");
        if (rbbi.following(-1) != 0) {
            errln("RuleBasedBreakIterator.following(-1) was suppose to return "
                    + "0 when the object has a fText of dummy.");
        }
    }

    /*
     * Tests the method public int preceding(int offset)
     */
    @Test
    public void TestPreceding() {
        RuleBasedBreakIterator rbbi = new RuleBasedBreakIterator(".;");
        // Tests when "if (fText == null || offset > fText.getEndIndex())" is true
        rbbi.setText((CharacterIterator)null);
        if (rbbi.preceding(-1) != BreakIterator.DONE) {
            errln("RuleBasedBreakIterator.preceding(-1) was suppose to return "
                    + "0 when the object has a fText of null.");
        }

        // Tests when "else if (offset < fText.getBeginIndex())" is true
        rbbi.setText("dummy");
        if (rbbi.preceding(-1) != 0) {
            errln("RuleBasedBreakIterator.preceding(-1) was suppose to return "
                    + "0 when the object has a fText of dummy.");
        }
    }

    /* Tests the method public int current() */
    @Test
    public void TestCurrent(){
        RuleBasedBreakIterator rbbi = new RuleBasedBreakIterator(".;");
        // Tests when "(fText != null) ? fText.getIndex() : BreakIterator.DONE" is true and false
        rbbi.setText((CharacterIterator)null);
        if(rbbi.current() != BreakIterator.DONE){
            errln("RuleBasedBreakIterator.current() was suppose to return "
                    + "BreakIterator.DONE when the object has a fText of null.");
        }
        rbbi.setText("dummy");
        if(rbbi.current() != 0){
            errln("RuleBasedBreakIterator.current() was suppose to return "
                    + "0 when the object has a fText of dummy.");
        }
    }

    @Test
    public void TestBug7547() {
        try {
            new RuleBasedBreakIterator("");
            fail("TestBug7547: RuleBasedBreakIterator constructor failed to throw an exception with empty rules.");
        }
        catch (IllegalArgumentException e) {
            // expected exception with empty rules.
        }
        catch (Exception e) {
            fail("TestBug7547: Unexpected exception while creating RuleBasedBreakIterator: " + e);
        }
    }

    @Test
    public void TestBug12797() {
        String rules = "!!chain; !!forward; $v=b c; a b; $v; !!reverse; .*;";
        RuleBasedBreakIterator bi = new RuleBasedBreakIterator(rules);

        bi.setText("abc");
        bi.first();
        assertEquals("Rule chaining test", 3,  bi.next());
    }


    @Test
    public void TestBug12873() {
        // Bug with RuleBasedBreakIterator's internal structure for recording potential look-ahead
        // matches not being cloned when a break iterator is cloned. This resulted in usage
        // collisions if the original break iterator and its clone were used concurrently.

        // The Line Break rules for Regional Indicators make use of look-ahead rules, and
        // show the bug. 1F1E6 = \uD83C\uDDE6 = REGIONAL INDICATOR SYMBOL LETTER A
        // Regional indicators group into pairs, expect breaks after two code points, which
        // is after four 16 bit code units.

        final String dataToBreak = "\uD83C\uDDE6\uD83C\uDDE6\uD83C\uDDE6\uD83C\uDDE6\uD83C\uDDE6\uD83C\uDDE6";
        final RuleBasedBreakIterator bi = (RuleBasedBreakIterator)BreakIterator.getLineInstance();
        final AssertionError[] assertErr = new AssertionError[1];  // saves an error found from within a thread

        class WorkerThread implements Runnable {
            @Override
            public void run() {
                try {
                    RuleBasedBreakIterator localBI = (RuleBasedBreakIterator)bi.clone();
                    localBI.setText(dataToBreak);
                    for (int loop=0; loop<100; loop++) {
                        int nextExpectedBreak = 0;
                        for (int actualBreak = localBI.first(); actualBreak != BreakIterator.DONE;
                                actualBreak = localBI.next(), nextExpectedBreak+= 4) {
                            assertEquals("", nextExpectedBreak, actualBreak);
                        }
                        assertEquals("", dataToBreak.length()+4, nextExpectedBreak);
                    }
                } catch (AssertionError e) {
                    assertErr[0] = e;
                }
            }
        }

        List<Thread> threads = new ArrayList<>();
        for (int n = 0; n<4; ++n) {
            threads.add(new Thread(new WorkerThread()));
        }
        for (Thread thread: threads) {
            thread.start();
        }
        for (Thread thread: threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                fail(e.toString());
            }
        }

        // JUnit wont see failures from within the worker threads, so
        // check again if one occurred.
        if (assertErr[0] != null) {
            throw assertErr[0];
        }
    }

    @Test
    public void TestBreakAllChars() {
        // Make a "word" from each code point, separated by spaces.
        // For dictionary based breaking, runs the start-of-range
        // logic with all possible dictionary characters.
        StringBuilder sb = new StringBuilder();
        for (int c=0; c<0x110000; ++c) {
            sb.appendCodePoint(c);
            sb.appendCodePoint(c);
            sb.appendCodePoint(c);
            sb.appendCodePoint(c);
            sb.append(' ');
        }
        String s = sb.toString();

        for (int breakKind=BreakIterator.KIND_CHARACTER; breakKind<=BreakIterator.KIND_TITLE; ++breakKind) {
            RuleBasedBreakIterator bi =
                    (RuleBasedBreakIterator)BreakIterator.getBreakInstance(ULocale.ENGLISH, breakKind);
            bi.setText(s);
            int lastb = -1;
            for (int b = bi.first(); b != BreakIterator.DONE; b = bi.next()) {
                assertTrue("(lastb < b) : (" + lastb + " < " + b + ")", lastb < b);
            }
        }
    }

    @Test
    public void TestBug12918() {
        // This test triggered an assertion failure in ICU4C, in dictbe.cpp
        // The equivalent code in ICU4J is structured slightly differently,
        // and does not appear vulnerable to the same issue.
        //
        // \u3325 decomposes with normalization, then the CJK dictionary
        // finds a break within the decomposition.

        String crasherString = "\u3325\u4a16";
        BreakIterator iter = BreakIterator.getWordInstance(ULocale.ENGLISH);
        iter.setText(crasherString);
        iter.first();
        int pos = 0;
        int lastPos = -1;
        while((pos = iter.next()) != BreakIterator.DONE) {
            assertTrue("", pos > lastPos);
        }
    }

    @Test
    public void TestBug12519() {
        RuleBasedBreakIterator biEn = (RuleBasedBreakIterator)BreakIterator.getWordInstance(ULocale.ENGLISH);
        RuleBasedBreakIterator biFr = (RuleBasedBreakIterator)BreakIterator.getWordInstance(ULocale.FRANCE);
        assertEquals("", ULocale.ENGLISH, biEn.getLocale(ULocale.VALID_LOCALE));
        assertEquals("", ULocale.FRENCH, biFr.getLocale(ULocale.VALID_LOCALE));
        assertEquals("Locales do not participate in BreakIterator equality.", biEn, biFr);

        RuleBasedBreakIterator cloneEn = (RuleBasedBreakIterator)biEn.clone();
        assertEquals("", biEn, cloneEn);
        assertEquals("", ULocale.ENGLISH, cloneEn.getLocale(ULocale.VALID_LOCALE));

        RuleBasedBreakIterator cloneFr = (RuleBasedBreakIterator)biFr.clone();
        assertEquals("", biFr, cloneFr);
        assertEquals("", ULocale.FRENCH, cloneFr.getLocale(ULocale.VALID_LOCALE));
    }

    static class T13512Thread extends Thread {
        private String fText;
        public List fBoundaries;
        public List fExpectedBoundaries;

        T13512Thread(String text) {
            fText = text;
            fExpectedBoundaries = getBoundary(fText);
        }
        @Override
        public void run() {
            for (int i= 0; i<10000; ++i) {
                fBoundaries = getBoundary(fText);
                if (!fBoundaries.equals(fExpectedBoundaries)) {
                    break;
                }
            }
        }
        private static final BreakIterator BREAK_ITERATOR_CACHE = BreakIterator.getWordInstance(ULocale.ROOT);
        public static List<Integer> getBoundary(String toParse) {
            List<Integer> retVal = new ArrayList<>();
            BreakIterator bi = (BreakIterator) BREAK_ITERATOR_CACHE.clone();
            bi.setText(toParse);
            for (int boundary=bi.first(); boundary != BreakIterator.DONE; boundary = bi.next()) {
                retVal.add(boundary);
            }
            return retVal;
        }
    }

    @Test
    public void TestBug13512() {
        String japanese = "コンピューターは、本質的には数字しか扱うことができません。コンピューターは、文字や記号などのそれぞれに番号を割り振る"
                + "ことによって扱えるようにします。ユニコードが出来るまでは、これらの番号を割り振る仕組みが何百種類も存在しました。どの一つをとっても、十分な"
                + "文字を含んではいませんでした。例えば、欧州連合一つを見ても、そのすべての言語をカバーするためには、いくつかの異なる符号化の仕"
                + "組みが必要でした。英語のような一つの言語に限っても、一つだけの符号化の仕組みでは、一般的に使われるすべての文字、句読点、技術"
                + "的な記号などを扱うには不十分でした。";

        String thai = "โดยพื้นฐานแล้ว, คอมพิวเตอร์จะเกี่ยวข้องกับเรื่องของตัวเลข. คอมพิวเตอร์จัดเก็บตัวอักษรและอักขระอื่นๆ"
                + " โดยการกำหนดหมายเลขให้สำหรับแต่ละตัว. ก่อนหน้าที่๊ Unicode จะถูกสร้างขึ้น, ได้มีระบบ encoding "
                + "อยู่หลายร้อยระบบสำหรับการกำหนดหมายเลขเหล่านี้. ไม่มี encoding ใดที่มีจำนวนตัวอักขระมากเพียงพอ: ยกตัวอย่างเช่น, "
                + "เฉพาะในกลุ่มสหภาพยุโรปเพียงแห่งเดียว ก็ต้องการหลาย encoding ในการครอบคลุมทุกภาษาในกลุ่ม. "
                + "หรือแม้แต่ในภาษาเดี่ยว เช่น ภาษาอังกฤษ ก็ไม่มี encoding ใดที่เพียงพอสำหรับทุกตัวอักษร, "
                + "เครื่องหมายวรรคตอน และสัญลักษณ์ทางเทคนิคที่ใช้กันอยู่ทั่วไป.\n" +
                "ระบบ encoding เหล่านี้ยังขัดแย้งซึ่งกันและกัน. นั่นก็คือ, ในสอง encoding สามารถใช้หมายเลขเดียวกันสำหรับตัวอักขระสองตัวที่แตกต่างกัน,"
                + "หรือใช้หมายเลขต่างกันสำหรับอักขระตัวเดียวกัน. ในระบบคอมพิวเตอร์ (โดยเฉพาะเซิร์ฟเวอร์) ต้องมีการสนับสนุนหลาย"
                + " encoding; และเมื่อข้อมูลที่ผ่านไปมาระหว่างการเข้ารหัสหรือแพล็ตฟอร์มที่ต่างกัน, ข้อมูลนั้นจะเสี่ยงต่อการผิดพลาดเสียหาย.";

        T13512Thread t1 = new T13512Thread(thai);
        T13512Thread t2 = new T13512Thread(japanese);
        try {
            t1.start(); t2.start();
            t1.join(); t2.join();
        } catch (Exception e) {
            fail(e.toString());
        }
        assertEquals("", t1.fExpectedBoundaries, t1.fBoundaries);
        assertEquals("", t2.fExpectedBoundaries, t2.fBoundaries);
    }

    @Test
    public void TestBug12677() {
        // Check that stripping of comments from rules for getRules() is not confused by
        // the presence of '#' characters in the rules that do not introduce comments.
        String rules = "!!forward; \n"
                     + "$x = [ab#];  # a set with a # literal. \n"
                     + " # .;        # a comment that looks sort of like a rule.   \n"
                     + " '#' '?';    # a rule with a quoted #   \n";

        RuleBasedBreakIterator bi  = new RuleBasedBreakIterator(rules);
        String rtRules = bi.toString();        // getRules() in C++
        assertEquals("Break Iterator rule stripping test", "!!forward;$x=[ab#];'#''?';",  rtRules);
    }

    @Test
    public void TestTableRedundancies() {
        RuleBasedBreakIterator bi = (RuleBasedBreakIterator)BreakIterator.getLineInstance(Locale.ENGLISH);
        String rules = bi.toString();
        bi = new RuleBasedBreakIterator(rules);
        // Build a break iterator from source rules.
        // Want to check the rule builder in Java, not the pre-built rules that are imported from ICU4C.
        RBBIDataWrapper dw = bi.fRData;
        RBBIDataWrapper.RBBIStateTable fwtbl = dw.fFTable;
        int numCharClasses = dw.fHeader.fCatCount;

        // Check for duplicate columns (character categories)
        List<String> columns = new ArrayList<>();
        for (int column=0; column<numCharClasses; column++) {
            StringBuilder s = new StringBuilder();
            for (int r = 1; r < fwtbl.fNumStates; r++) {
                int row = dw.getRowIndex(r);
                char tableVal = fwtbl.fTable[row + RBBIDataWrapper.NEXTSTATES + column];
                s.append(tableVal);
            }
            columns.add(s.toString());
        }
        // Ignore column (char class) 0 while checking; it's special, and may have duplicates.
        for (int c1=1; c1<numCharClasses; c1++) {
            int limit = c1 < fwtbl.fDictCategoriesStart ? fwtbl.fDictCategoriesStart : numCharClasses;
            for (int c2 = c1+1; c2 < limit; c2++) {
                assertFalse(String.format("Duplicate columns (%d, %d)", c1, c2), columns.get(c1).equals(columns.get(c2)));
                // if (columns.get(c1).equals(columns.get(c2))) {
                //    System.out.printf("Duplicate columns (%d, %d)\n", c1, c2);
                // }
            }
        }

        // Check for duplicate states.
        List<String> rows = new ArrayList<>();
        for (int r=0; r<fwtbl.fNumStates; r++) {
            StringBuilder s = new StringBuilder();
            int row = dw.getRowIndex(r);
            s.append(fwtbl.fTable[row + RBBIDataWrapper.ACCEPTING]);
            s.append(fwtbl.fTable[row + RBBIDataWrapper.LOOKAHEAD]);
            s.append(fwtbl.fTable[row + RBBIDataWrapper.TAGSIDX]);
            for (int column=0; column<numCharClasses; column++) {
                char tableVal = fwtbl.fTable[row + RBBIDataWrapper.NEXTSTATES + column];
                s.append(tableVal);
            }
            rows.add(s.toString());
        }

        for (int r1=0; r1 < fwtbl.fNumStates; r1++) {
            for (int r2= r1+1; r2 < fwtbl.fNumStates; r2++) {
                assertFalse(String.format("Duplicate states (%d, %d)", r1, r2), rows.get(r1).equals(rows.get(r2)));
                // if (rows.get(r1).equals(rows.get(r2))) {
                //     System.out.printf("Duplicate states (%d, %d)\n", r1, r2);
                // }
            }
        }
    }

    @Test
    public void TestBug13447() {
        // Bug 13447: verify that getRuleStatus() returns the value corresponding to current(),
        //  even after next() has returned DONE.
       RuleBasedBreakIterator bi =
                (RuleBasedBreakIterator)BreakIterator.getWordInstance(Locale.ENGLISH);
        bi.setText("1234");
        assertEquals("", BreakIterator.WORD_NONE, bi.getRuleStatus());
        assertEquals("", 4, bi.next());
        assertEquals("", BreakIterator.WORD_NUMBER, bi.getRuleStatus());
        assertEquals("", BreakIterator.DONE, bi.next());
        assertEquals("", 4, bi.current());
        assertEquals("", BreakIterator.WORD_NUMBER, bi.getRuleStatus());
    }

    @Test
    public void TestTableRebuild() {
        // Test to verify that rebuilding the state tables from rule source for the standard
        // break iterator types yields the same tables as are imported from ICU4C as part of the default data.
        List<RuleBasedBreakIterator> breakIterators = new ArrayList<>();
        breakIterators.add((RuleBasedBreakIterator)BreakIterator.getCharacterInstance(Locale.ENGLISH));
        breakIterators.add((RuleBasedBreakIterator)BreakIterator.getWordInstance(Locale.ENGLISH));
        breakIterators.add((RuleBasedBreakIterator)BreakIterator.getSentenceInstance(Locale.ENGLISH));
        breakIterators.add((RuleBasedBreakIterator)BreakIterator.getLineInstance(Locale.ENGLISH));

        for (RuleBasedBreakIterator bi: breakIterators) {
            String rules = bi.toString();
            RuleBasedBreakIterator bi2 = new RuleBasedBreakIterator(rules);
            assertTrue("Forward Table",      RBBIDataWrapper.equals(bi.fRData.fFTable, bi2.fRData.fFTable));
            assertTrue("Reverse Table",      RBBIDataWrapper.equals(bi.fRData.fRTable, bi2.fRData.fRTable));
        }
    }

    // Helper function to test 8/16 bits of trie and 8/16 bits of state table.
    private void testTrieStateTable(int numChar, boolean expectUCPTrieValueWidthIn8Bits,
        boolean expectStateRowIn8Bits) {
        // Text are duplicate characters from U+4E00 to U+4FFF
        StringBuilder builder = new StringBuilder(2 * (0x5000 - 0x4e00));
        for (char c = 0x4e00; c < 0x5000; c++) {
            builder.append(c).append(c);
        }
        String text = builder.toString();

        // Generate rule which will caused length+4 character classes and
        // length+3 states

        builder = new StringBuilder(100 + 6 * numChar);
        builder.append("!!quoted_literals_only;");
        for (char c = 0x4e00; c < 0x4e00 + numChar; c++) {
            builder.append("\'").append(c).append(c).append("';");
        }
        builder.append(".;");
        String rules = builder.toString();

        RuleBasedBreakIterator bi = new RuleBasedBreakIterator(rules);

        RBBIDataWrapper dw = bi.fRData;
        RBBIDataWrapper.RBBIStateTable fwtbl = dw.fFTable;
        RBBIDataWrapper.RBBIStateTable rvtbl = dw.fRTable;

        boolean has8BitRowDataForwardTable = (fwtbl.fFlags & RBBIDataWrapper.RBBI_8BITS_ROWS) != 0;
        boolean has8BitRowDataReverseTable = (rvtbl.fFlags & RBBIDataWrapper.RBBI_8BITS_ROWS) != 0;
        boolean has8BitsTrie = dw.fTrie.getValueWidth() == CodePointTrie.ValueWidth.BITS_8;

        assertEquals("Number of char classes mismatch numChar=" + numChar, numChar + 4, dw.fHeader.fCatCount);
        assertEquals("Number of states in Forward Table mismatch numChar=" + numChar, numChar + 3, fwtbl.fNumStates);
        assertEquals("Number of states in Reverse Table mismatch numChar=" + numChar, numChar + 3, rvtbl.fNumStates);
        assertEquals("Trie width mismatch numChar=" + numChar, expectUCPTrieValueWidthIn8Bits, has8BitsTrie);
        assertEquals("Bits of Forward State table mismatch numChar=" + numChar,
                     expectStateRowIn8Bits, has8BitRowDataForwardTable);
        assertEquals("Bits of Reverse State table mismatch numChar=" + numChar,
                     expectStateRowIn8Bits, has8BitRowDataReverseTable);

        bi.setText(text);

        int pos;
        int i = 0;
        while ((pos = bi.next()) > 0) {
            // The first numChar should not break between the pair
            if (i++ < numChar) {
                assertEquals("next() mismatch numChar=" + numChar, i * 2, pos);
            } else {
                // After the first numChar next(), break on each character.
                assertEquals("next() mismatch numChar=" + numChar, i + numChar, pos);
            }
        }
        while ((pos = bi.previous()) > 0) {
            // The first numChar should not break between the pair
            if (--i < numChar) {
                assertEquals("previous() mismatch numChar=" + numChar, i * 2, pos);
            } else {
                // After the first numChar next(), break on each character.
                assertEquals("previous() mismatch numChar=" + numChar, i + numChar, pos);
            }
        }
    }

    @Test
    public void Test8BitsTrieWith8BitStateTable() {
        testTrieStateTable(251,  true /* expectUCPTrieValueWidthIn8Bits */,  true /* expectStateRowIn8Bits */);
    }

    @Test
    public void Test16BitsTrieWith8BitStateTable() {
        testTrieStateTable(252, false /* expectUCPTrieValueWidthIn8Bits */,  true /* expectStateRowIn8Bits */);
    }

    @Test
    public void Test16BitsTrieWith16BitStateTable() {
        testTrieStateTable(253, false /* expectUCPTrieValueWidthIn8Bits */, false /* expectStateRowIn8Bits */);
    }

    @Test
    public void Test8BitsTrieWith16BitStateTable() {
        // Test UCPTRIE_VALUE_BITS_8 with 16 bits rows. Use a different approach to
        // create state table in 16 bits.

        // Generate 510 'a' as text
        StringBuilder builder = new StringBuilder(510);
        for (int i = 0; i < 510; i++) {
            builder.append('a');
        }
        String text = builder.toString();

        builder = new StringBuilder(550);
        builder.append("!!quoted_literals_only;'");
        // 254 'a' in the rule will cause 256 states
        for (int i = 0; i < 254; i++) {
            builder.append('a');
        }
        builder.append("';.;");
        String rules = builder.toString();

        RuleBasedBreakIterator bi = new RuleBasedBreakIterator(rules);

        RBBIDataWrapper dw = bi.fRData;
        RBBIDataWrapper.RBBIStateTable fwtbl = dw.fFTable;

        boolean has8BitRowData = (fwtbl.fFlags & RBBIDataWrapper.RBBI_8BITS_ROWS) != 0;
        boolean has8BitsTrie = dw.fTrie.getValueWidth() == CodePointTrie.ValueWidth.BITS_8;
        assertFalse("State table should be in 16 bits", has8BitRowData);
        assertTrue("Trie should be in 8 bits", has8BitsTrie);

        bi.setText(text);

        // break positions:
        // 254, 508, 509, 510
        assertEquals("next()", 254, bi.next());
        int i = 0;
        int pos;
        while ((pos = bi.next()) > 0) {
            assertEquals("next()", 508 + i , pos);
            i++;
        }
        i = 0;
        while ((pos = bi.previous()) > 0) {
             i++;
            if (pos >= 508) {
                assertEquals("previous()", 510 - i , pos);
            } else {
                assertEquals("previous()", 254 , pos);
            }
        }
    }

    /**
     * Test that both compact (8 bit) and full sized (16 bit) rbbi tables work, and
     * that there are no problems with rules at the size that transitions between the two.
     *
     * A rule that matches a literal string, like 'abcdefghij', will require one state and
     * one character class per character in the string. So we can make a rule to tickle the
     * boundaries by using literal strings of various lengths.
     *
     * For both the number of states and the number of character classes, the eight bit format
     * only has 7 bits available, allowing for 128 values. For both, a few values are reserved,
     * leaving 120 something available. This test runs the string over the range of 120 - 130,
     * which allows some margin for changes to the number of values reserved by the rule builder
     * without breaking the test.
     */
    @Test
    public void TestTable_8_16_Bits() {
        // testStr serves as both the source of the rule string (truncated to the desired length)
        // and as test data to check matching behavior. A break rule consisting of the first 120
        // characters of testStr will match the first 120 chars of the full-length testStr.
        StringBuilder builder = new StringBuilder(0x200);
        for (char c=0x3000; c<0x3200; ++c) {
            builder.append(c);
        }
        String testStr = builder.toString();

        int startLength = 120;   // The shortest rule string to test.
        int endLength = 260;     // The longest rule string to test
        int increment = 1;
        for (int ruleLen=startLength; ruleLen <= endLength; ruleLen += increment) {
            String ruleString = (new String("!!quoted_literals_only; '#';"))
                .replace("#", testStr.substring(0, ruleLen));
            RuleBasedBreakIterator bi = new RuleBasedBreakIterator(ruleString);

            // Verify that the break iterator is functioning - that the first boundary found
            // in testStr is at the length of the rule string.
            bi.setText(testStr);
            assertEquals("The first boundary found in testStr should be at the length of the rule string",
                ruleLen, bi.next());

            // Reverse iteration. Do a setText() first, to flush the break iterator's internal cache
            // of previously detected boundaries, thus forcing the engine to run the safe reverse rules.
            bi.setText(testStr);
            int result = bi.preceding(ruleLen);
            assertEquals("Reverse iteration should find the boundary at 0", 0, result);

            // Verify that the range of rule lengths being tested cover the transations
            // from 8 to 16 bit data.
            RBBIDataWrapper dw = bi.fRData;
            RBBIDataWrapper.RBBIStateTable fwtbl = dw.fFTable;

            boolean has8BitRowData = (fwtbl.fFlags & RBBIDataWrapper.RBBI_8BITS_ROWS) != 0;
            boolean has8BitsTrie = dw.fTrie.getValueWidth() == CodePointTrie.ValueWidth.BITS_8;
            if (ruleLen == startLength) {
                assertTrue("State table should be in 8 bits", has8BitRowData);
                assertTrue("Trie should be in 8 bits", has8BitsTrie);
            }
            if (ruleLen == endLength) {
                assertFalse("State table should be in 16 bits", has8BitRowData);
                assertFalse("Trie should be in 16 bits", has8BitsTrie);
            }
        }
    }

    /* Test handling of a large number of look-ahead rules.
     * The number of rules in the test exceeds the implementation limits prior to the
     * improvements introduced with #13590.
     *
     * The test look-ahead rules have the form "AB / CE"; "CD / EG"; ...
     * The text being matched is sequential, "ABCDEFGHI..."
     *
     * The upshot is that the look-ahead rules all match on their preceding context,
     * and consequently must save a potential result, but then fail to match on their
     * trailing context, so that they don't actually cause a boundary.
     *
     * Additionally, add a ".*" rule, so there are no boundaries unless a
     * look-ahead hard-break rule forces one.
     */
    @Test
    public void TestBug13590() {
        StringBuilder rules = new StringBuilder("!!quoted_literals_only; !!chain; .*;\n");

        int NUM_LOOKAHEAD_RULES = 50;
        char STARTING_CHAR = '\u5000';
        char firstChar = 0;
        for (int ruleNum = 0; ruleNum < NUM_LOOKAHEAD_RULES; ++ruleNum) {
            firstChar = (char) (STARTING_CHAR + ruleNum*2);
            rules.append('\'') .append(firstChar) .append((char)(firstChar+1)) .append('\'')
                 .append(' ') .append('/') .append(' ')
                 .append('\'') .append((char)(firstChar+2)) .append((char)(firstChar+4)) .append('\'')
                 .append(';') .append('\n');
        }

        // Change the last rule added from the form "UV / WY" to "UV / WX".
        // Changes the rule so that it will match - all 4 chars are in ascending sequence.
        String rulesStr = rules.toString().replace((char)(firstChar+4), (char)(firstChar+3));

        RuleBasedBreakIterator bi = new RuleBasedBreakIterator(rulesStr);
        // bi.dump(System.out);

        StringBuilder testString = new StringBuilder();
        for (char c = (char) (STARTING_CHAR-200); c < STARTING_CHAR + NUM_LOOKAHEAD_RULES*4; ++c) {
            testString.append(c);
        }
        bi.setText(testString);

        int breaksFound = 0;
        while (bi.next() != BreakIterator.DONE) {
            ++breaksFound;
        }

        // Two matches are expected, one from the last rule that was explicitly modified,
        // and one at the end of the text.
        assertEquals("Wrong number of breaks found", 2, breaksFound);
    }

    /* Test handling of unpair surrogate.
     */
    @Test
    public void TestUnpairedSurrogate() {
        // make sure the simple one work first.
        String rules = "ab;";
        RuleBasedBreakIterator bi = new RuleBasedBreakIterator(rules);
        assertEquals("Rules does not match", rules, bi.toString());

        try {
            new RuleBasedBreakIterator("a\ud800b;");
            fail("TestUnpairedSurrogate: RuleBasedBreakIterator() failed to throw an exception with unpair low surrogate.");
        }
        catch (IllegalArgumentException e) {
            // expected exception with unpair surrogate.
        }
        catch (Exception e) {
            fail("TestUnpairedSurrogate: Unexpected exception while new RuleBasedBreakIterator() with unpair low surrogate: " + e);
        }

        try {
            new RuleBasedBreakIterator("a\ude00b;");
            fail("TestUnpairedSurrogate: RuleBasedBreakIterator() failed to throw an exception with unpair high surrogate.");
        }
        catch (IllegalArgumentException e) {
            // expected exception with unpair surrogate.
        }
        catch (Exception e) {
            fail("TestUnpairedSurrogate: Unexpected exception while new RuleBasedBreakIterator() with unpair high surrogate: " + e);
        }


        // make sure the surrogate one work too.
        rules = "a😀b;";
        bi = new RuleBasedBreakIterator(rules);
        assertEquals("Rules does not match", rules, bi.toString());
    }
}
