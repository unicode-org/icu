/*
 *******************************************************************************
 * Copyright (C) 2006, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.tests;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Locale;

import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.util.ULocale;

public class BreakIteratorTest extends ICUTestCase {
    // ICU behaves a bit differently with this text, but the tested values aren't
    // affected.  If Java changes behavior they might need to change.
    private static final String text = "Mr. and Mrs. Mumblety-Peg paid $35.97 for a new 12\" cockatoo. " +
        "When they got home they both cooed \"Isn't it lovely?\" and sighed softly. " +
        "\"Let's name it u\u0308\u5098!\" they said with glee.";
    private static int pos = text.indexOf("sn't");
    private static BreakIterator cbr;
    private static BreakIterator wbr;
    private static BreakIterator lbr;
    private static BreakIterator sbr;
        
    static {
        cbr = BreakIterator.getCharacterInstance();
        cbr.setText(text);
        wbr = BreakIterator.getWordInstance();
        wbr.setText(text);
        lbr = BreakIterator.getLineInstance();
        lbr.setText(text);
        sbr = BreakIterator.getSentenceInstance();
        sbr.setText(text);
                
        // diagnostic
        //              dump(cbr);
        //              dump(wbr);
        //              dump(lbr);
        //              dump(sbr);
    }
        
    //      private static void dump(BreakIterator bi) {
    //              for (int ix = bi.first(), lim = text.length(); ix != lim;) {
    //                      int nx = bi.next();
    //                      if (nx < 0) nx = lim;
    //                      System.out.println(Integer.toString(ix) + ": " + text.substring(ix, nx));
    //                      ix = nx;
    //              }
    //      }
        
    /*
     * Test method for 'com.ibm.icu.text.BreakIterator.hashCode()'
     */
    public void testHashCode() {
        BreakIterator br = BreakIterator.getWordInstance();
        br.setText(text);
        BreakIterator brne = BreakIterator.getWordInstance();
        brne.setText(text + "X");
        wbr.first();
        testEHCS(br, wbr, brne);
    }

    /*
     * Test method for 'com.ibm.icu.text.BreakIterator.BreakIterator(BreakIterator)'
     */
    public void testBreakIterator() {
        // implicitly tested everywhere
    }

    /*
     * Test method for 'com.ibm.icu.text.BreakIterator.first()'
     */
    public void testFirst() {
        assertEquals(0, cbr.first());
        assertEquals(0, wbr.first());
        assertEquals(0, lbr.first());
        assertEquals(0, sbr.first());
    }

    /*
     * Test method for 'com.ibm.icu.text.BreakIterator.last()'
     */
    public void testLast() {
        assertEquals(text.length(), cbr.last());
        assertEquals(text.length(), wbr.last());
        assertEquals(text.length(), lbr.last());
        assertEquals(text.length(), sbr.last());
    }

    /*
     * Test method for 'com.ibm.icu.text.BreakIterator.next(int)'
     */
    public void testNextInt() {
        cbr.first();
        wbr.first();
        lbr.first();
        sbr.first();
        assertEquals(2, cbr.next(2));
        assertEquals(3, wbr.next(2));
        assertEquals(8, lbr.next(2));
        assertEquals(62, sbr.next(2));
                
        cbr.last();
        wbr.last();
        lbr.last();
        sbr.last();
        assertEquals(174, cbr.next(-2));
        assertEquals(171, wbr.next(-2));
        assertEquals(166, lbr.next(-2));
        assertEquals(135, sbr.next(-2));
    }

    /*
     * Test method for 'com.ibm.icu.text.BreakIterator.next()'
     */
    public void testNext() {
        cbr.first();
        wbr.first();
        lbr.first();
        sbr.first();
        assertEquals(1, cbr.next());
        assertEquals(2, wbr.next());
        assertEquals(4, lbr.next());
        assertEquals(13, sbr.next());
    }

    /*
     * Test method for 'com.ibm.icu.text.BreakIterator.previous()'
     */
    public void testPrevious() {
        cbr.last();
        wbr.last();
        lbr.last();
        sbr.last();
        assertEquals(175, cbr.previous());
        assertEquals(175, wbr.previous());
        assertEquals(171, lbr.previous());
        assertEquals(156, sbr.previous());
    }

    /*
     * Test method for 'com.ibm.icu.text.BreakIterator.following(int)'
     */
    public void testFollowing() {
        assertEquals(100, cbr.following(pos));
        assertEquals(103, wbr.following(pos));
        assertEquals(104, lbr.following(pos));
        assertEquals(116, sbr.following(pos));
    }

    /*
     * Test method for 'com.ibm.icu.text.BreakIterator.preceding(int)'
     */
    public void testPreceding() {
        assertEquals(98, cbr.preceding(pos));
        assertEquals(98, wbr.preceding(pos));
        assertEquals(97, lbr.preceding(pos));
        assertEquals(62, sbr.preceding(pos));
    }

    /*
     * Test method for 'com.ibm.icu.text.BreakIterator.isBoundary(int)'
     */
    public void testIsBoundary() {
        assertTrue(cbr.isBoundary(pos));
        assertFalse(wbr.isBoundary(pos));
        assertFalse(lbr.isBoundary(pos));
        assertFalse(sbr.isBoundary(pos));

    }

    /*
     * Test method for 'com.ibm.icu.text.BreakIterator.current()'
     */
    public void testCurrent() {
        cbr.following(pos);
        wbr.following(pos);
        lbr.following(pos);
        sbr.following(pos);
        assertEquals(100, cbr.current());
        assertEquals(103, wbr.current());
        assertEquals(104, lbr.current());
        assertEquals(116, sbr.current());
    }

    /*
     * Test method for 'com.ibm.icu.text.BreakIterator.getText()'
     */
    public void testGetText() {
        CharacterIterator ci = cbr.getText();
        StringBuffer buf = new StringBuffer(ci.getEndIndex() - ci.getBeginIndex());
        for (char c = ci.first(); c != CharacterIterator.DONE; c = ci.next()) {
            buf.append(c);
        }
        String result = buf.toString();
        assertEquals(text, result);
    }

    /*
     * Test method for 'com.ibm.icu.text.BreakIterator.setText(String)'
     */
    public void testSetTextString() {
        // implicitly tested
    }

    /*
     * Test method for 'com.ibm.icu.text.BreakIterator.setText(CharacterIterator)'
     */
    public void testSetTextCharacterIterator() {
        CharacterIterator ci = new StringCharacterIterator(text, pos);
        BreakIterator bi = BreakIterator.getWordInstance();
        bi.setText(ci);
        assertEquals(2, bi.next());
    }

    /*
     * Test method for 'com.ibm.icu.text.BreakIterator.getWordInstance()'
     */
    public void testGetWordInstance() {
        // implicitly tested
    }

    /*
     * Test method for 'com.ibm.icu.text.BreakIterator.getWordInstance(Locale)'
     */
    public void testGetWordInstanceLocale() {
        assertNotNull(BreakIterator.getWordInstance(Locale.JAPAN));
    }

    /*
     * Test method for 'com.ibm.icu.text.BreakIterator.getWordInstance(ULocale)'
     */
    public void testGetWordInstanceULocale() {
        assertNotNull(BreakIterator.getWordInstance(ULocale.JAPAN));
    }

    /*
     * Test method for 'com.ibm.icu.text.BreakIterator.getLineInstance()'
     */
    public void testGetLineInstance() {
        // implicitly tested
    }

    /*
     * Test method for 'com.ibm.icu.text.BreakIterator.getLineInstance(Locale)'
     */
    public void testGetLineInstanceLocale() {
        assertNotNull(BreakIterator.getLineInstance(Locale.JAPAN));
    }

    /*
     * Test method for 'com.ibm.icu.text.BreakIterator.getLineInstance(ULocale)'
     */
    public void testGetLineInstanceULocale() {
        assertNotNull(BreakIterator.getLineInstance(ULocale.JAPAN));
    }

    /*
     * Test method for 'com.ibm.icu.text.BreakIterator.getCharacterInstance()'
     */
    public void testGetCharacterInstance() {
        // implicitly tested
    }

    /*
     * Test method for 'com.ibm.icu.text.BreakIterator.getCharacterInstance(Locale)'
     */
    public void testGetCharacterInstanceLocale() {
        assertNotNull(BreakIterator.getCharacterInstance(Locale.JAPAN));
    }

    /*
     * Test method for 'com.ibm.icu.text.BreakIterator.getCharacterInstance(ULocale)'
     */
    public void testGetCharacterInstanceULocale() {
        assertNotNull(BreakIterator.getCharacterInstance(ULocale.JAPAN));
    }

    /*
     * Test method for 'com.ibm.icu.text.BreakIterator.getSentenceInstance()'
     */
    public void testGetSentenceInstance() {
        // implicitly tested
    }

    /*
     * Test method for 'com.ibm.icu.text.BreakIterator.getSentenceInstance(Locale)'
     */
    public void testGetSentenceInstanceLocale() {
        assertNotNull(BreakIterator.getSentenceInstance(Locale.JAPAN));
    }

    /*
     * Test method for 'com.ibm.icu.text.BreakIterator.getSentenceInstance(ULocale)'
     */
    public void testGetSentenceInstanceULocale() {
        assertNotNull(BreakIterator.getSentenceInstance(ULocale.JAPAN));
    }

    /*
     * Test method for 'com.ibm.icu.text.BreakIterator.getTitleInstance()'
     */
    public void testGetTitleInstance() {
        // not implemented
    }

    /*
     * Test method for 'com.ibm.icu.text.BreakIterator.getTitleInstance(Locale)'
     */
    public void testGetTitleInstanceLocale() {
        // not implemented
    }

    /*
     * Test method for 'com.ibm.icu.text.BreakIterator.getTitleInstance(ULocale)'
     */
    public void testGetTitleInstanceULocale() {
        // not implemented
    }

    /*
     * Test method for 'com.ibm.icu.text.BreakIterator.getAvailableLocales()'
     */
    public void testGetAvailableLocales() {
        assertNotNull(BreakIterator.getAvailableLocales());
    }

    /*
     * Test method for 'com.ibm.icu.text.BreakIterator.getAvailableULocales()'
     */
    public void testGetAvailableULocales() {
        assertNotNull(BreakIterator.getAvailableULocales());
    }

    /*
     * Test method for 'com.ibm.icu.text.BreakIterator.toString()'
     */
    public void testToString() {
        assertNotNull(cbr.toString());
    }

    /*
     * Test method for 'com.ibm.icu.text.BreakIterator.clone()'
     */
    public void testClone() {
        // see testHashCode
    }

    /*
     * Test method for 'com.ibm.icu.text.BreakIterator.equals(Object)'
     */
    public void testEqualsObject() {
        // see testHashCode
    }
}
