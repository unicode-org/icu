// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2006-2011, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.tests;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Date;
import java.util.Locale;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.ULocale;

public class MessageFormatTest extends ICUTestCase {
    private final String pattern = "Deleted {0,number} files at {1,time,short} on {1,date}.";
    private final String altPattern = "Deleted {0,  number } files at {1, time, short} on {1, date}.";
    private final Date date = new Date(716698890835L);
    private final Number num = new Long(3456);
    private final Object[] args = { num, date };
    private final Date dateOnly = new Date(716626800000L);
    private final String englishTarget = "Deleted 3,456 files at 8:01 PM on Sep 16, 1992.";
    private final String germanTarget = "Deleted 3.456 files at 20:01 on 16.09.1992.";
    private final String modifiedTarget = "Deleted 3,456 files at 8:01:30 PM PDT on Sep 16, 1992.";
    
    /*
     * Test method for 'com.ibm.icu.text.MessageFormat.hashCode()'
     */
    public void testHashCode() {
        MessageFormat mf = new MessageFormat(pattern);
        MessageFormat eq = new MessageFormat(altPattern);
        MessageFormat ne = new MessageFormat("Deleted (0, number, currency} files at {1, time} on {1, date}.");
        testEHCS(mf, eq, ne);
    }

    /*
     * Test method for 'com.ibm.icu.text.MessageFormat.MessageFormat(MessageFormat)'
     */
    public void testMessageFormatMessageFormat() {
        // implicitly tested everywhere
    }

    /*
     * Test method for 'com.ibm.icu.text.MessageFormat.MessageFormat(String)'
     */
    public void testMessageFormatString() {
        MessageFormat mf = new MessageFormat(pattern);
        assertEquals(englishTarget, mf.format(args));
    }

    /*
     * Test method for 'com.ibm.icu.text.MessageFormat.MessageFormat(String, Locale)'
     */
    public void testMessageFormatStringLocale() {
        MessageFormat mf = new MessageFormat(pattern, Locale.US);
        assertEquals(englishTarget, mf.format(args));
    }

    /*
     * Test method for 'com.ibm.icu.text.MessageFormat.MessageFormat(String, ULocale)'
     */
    public void testMessageFormatStringULocale() {
        MessageFormat mf = new MessageFormat(pattern, ULocale.US);
        assertEquals(englishTarget, mf.format(args));
    }

    /*
     * Test method for 'com.ibm.icu.text.MessageFormat.setLocale(Locale)'
     */
    public void testSetLocaleLocale() {
        MessageFormat mf = new MessageFormat(pattern, Locale.US);
        mf.setLocale(Locale.GERMANY);
        mf.applyPattern(pattern);
        assertEquals(germanTarget, mf.format(args));
    }

    /*
     * Test method for 'com.ibm.icu.text.MessageFormat.setLocale(ULocale)'
     */
    public void testSetLocaleULocale() {
        MessageFormat mf = new MessageFormat(pattern, Locale.US);
        mf.setLocale(ULocale.GERMANY);
        mf.applyPattern(pattern);
        assertEquals(germanTarget, mf.format(args));
    }

    /*
     * Test method for 'com.ibm.icu.text.MessageFormat.getLocale()'
     */
    public void testGetLocale() {
        MessageFormat mf = new MessageFormat(pattern, Locale.US);
        mf.setLocale(Locale.GERMANY);
        assertEquals(Locale.GERMANY, mf.getLocale());
    }

    /*
     * Test method for 'com.ibm.icu.text.MessageFormat.getULocale()'
     */
    public void testGetULocale() {
        MessageFormat mf = new MessageFormat(pattern, Locale.US);
        mf.setLocale(ULocale.GERMANY);
        assertEquals(ULocale.GERMANY, mf.getULocale());
    }

    /*
     * Test method for 'com.ibm.icu.text.MessageFormat.applyPattern(String)'
     */
    public void testApplyPattern() {
        MessageFormat mf = new MessageFormat("foo");
        mf.applyPattern(pattern);
        assertEquals(englishTarget, mf.format(args));
    }

    /*
     * Test method for 'com.ibm.icu.text.MessageFormat.toPattern()'
     */
    public void testToPattern() {
        MessageFormat mf = new MessageFormat(altPattern);
        assertEquals(pattern, mf.toPattern());
    }

    /*
     * Test method for 'com.ibm.icu.text.MessageFormat.setFormatsByArgumentIndex(Format[])'
    public void testSetFormatsByArgumentIndex() {
        // this api is broken.  if the same argument is used twice with two different
        // formats, this can't be used, since it sets only one format per argument.
        MessageFormat mf = new MessageFormat(pattern, Locale.US);
        Format[] formats = {
            NumberFormat.getIntegerInstance(),
            DateFormat.getTimeInstance(DateFormat.SHORT),
            DateFormat.getDateInstance(),
        };
        mf.setFormatsByArgumentIndex(formats);
        assertEquals(brokenButConformantTarget, mf.format(args));
    }
     */

    /*
     * Test method for 'com.ibm.icu.text.MessageFormat.setFormats(Format[])'
     */
    public void testSetFormats() {
        // this api, while it has the problem that the order of formats depends
        // on the order in the string, at least lets you set all the formats.
        
        MessageFormat mf = new MessageFormat(pattern, Locale.US);
        Format[] formats = {
            NumberFormat.getIntegerInstance(),
            DateFormat.getTimeInstance(DateFormat.SHORT),
            DateFormat.getDateInstance(),
        };
        mf.setFormats(formats);
        assertEquals(englishTarget, mf.format(args));
   }

    /*
     * Test method for 'com.ibm.icu.text.MessageFormat.setFormatByArgumentIndex(int, Format)'
     public void testSetFormatByArgumentIndex() {
        // same problem, once you set a format for an argument, you've set all of them
        
        MessageFormat mf = new MessageFormat(pattern, Locale.US);
        mf.setFormatByArgumentIndex(1, DateFormat.getTimeInstance(DateFormat.SHORT));
        assertEquals(brokenButConformantTarget, mf.format(args));

    }
    */

    /*
     * Test method for 'com.ibm.icu.text.MessageFormat.setFormat(int, Format)'
     */
    public void testSetFormat() {
        // and ok again        
        MessageFormat mf = new MessageFormat(pattern, Locale.US);
        mf.setFormat(1, DateFormat.getTimeInstance(DateFormat.LONG));
        assertEquals(modifiedTarget, mf.format(args));
    }

    /*
     * Test method for 'com.ibm.icu.text.MessageFormat.getFormatsByArgumentIndex()'
    public void testGetFormatsByArgumentIndex() {
        MessageFormat mf = new MessageFormat(pattern, Locale.US);
        Format[] formats = mf.getFormatsByArgumentIndex();
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        assertEquals(formats[0], nf);
        DateFormat df = DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.US);
        assertEquals(formats[1], df);
    }
     */

    /*
     * Test method for 'com.ibm.icu.text.MessageFormat.getFormats()'
     */
    public void testGetFormats() {
        MessageFormat mf = new MessageFormat(pattern, Locale.US);
        Format[] formats = mf.getFormats();
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        assertEquals(formats[0], nf);
        DateFormat tf = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.US);
        assertEquals(formats[1], tf);
        DateFormat df = DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.US);
        assertEquals(formats[2], df);
    }

    /*
     * Test method for 'com.ibm.icu.text.MessageFormat.format(Object[], StringBuffer, FieldPosition)'
     */
    public void testFormatObjectArrayStringBufferFieldPosition() {
        MessageFormat mf = new MessageFormat(pattern, Locale.US);
        StringBuffer buf = new StringBuffer();
        FieldPosition fp = new FieldPosition(0);
        mf.format(args, buf, fp);
        assertEquals(englishTarget, buf.toString());
    }

    /*
     * Test method for 'com.ibm.icu.text.MessageFormat.format(String, Object[])'
     */
    public void testFormatStringObjectArray() {
        assertEquals(englishTarget, MessageFormat.format(pattern, args));
    }

    /*
     * Test method for 'com.ibm.icu.text.MessageFormat.format(Object, StringBuffer, FieldPosition)'
     */
    public void testFormatObjectStringBufferFieldPosition() {
        MessageFormat mf = new MessageFormat(pattern, Locale.US);
        StringBuffer buf = new StringBuffer();
        FieldPosition fp = new FieldPosition(0);
        mf.format((Object)args, buf, fp);
        assertEquals(englishTarget, buf.toString());
    }

    /*
     * Test method for 'com.ibm.icu.text.MessageFormat.parse(String, ParsePosition)'
     */
    public void testParseStringParsePosition() {
        MessageFormat mf = new MessageFormat(pattern, Locale.US);
        ParsePosition pp = new ParsePosition(1);
        Object[] result = mf.parse("!" + englishTarget, pp);
        assertEquals(num, result[0]);
        assertEquals(dateOnly, result[1]);
    }

    /*
     * Test method for 'com.ibm.icu.text.MessageFormat.parse(String)'
     */
    public void testParseString() {
        MessageFormat mf = new MessageFormat(pattern, Locale.US);
        try {
            Object[] result = mf.parse(englishTarget);
            assertEquals(num, result[0]);
            assertEquals(dateOnly, result[1]);
        }
        catch (ParseException e) {
            fail(e.getMessage());
        }
    }

    /*
     * Test method for 'com.ibm.icu.text.MessageFormat.parseObject(String, ParsePosition)'
     */
    public void testParseObjectStringParsePosition() {
        MessageFormat mf = new MessageFormat(pattern, Locale.US);
        ParsePosition pp = new ParsePosition(0);
        Object result = mf.parseObject(englishTarget, pp);
        assertEquals(num, ((Object[])result)[0]);
        assertEquals(dateOnly, ((Object[])result)[1]);
    }

    /*
     * Test method for 'com.ibm.icu.text.MessageFormat.autoQuoteApostrophe(String)'
     */
    public void testAutoQuoteApostrophe() {
        String str = "Let's meet at {1,time,h 'o'' clock'} at l'Orange Bleue";
        String pat = MessageFormat.autoQuoteApostrophe(str);
        MessageFormat mf = new MessageFormat(pat, Locale.US);
        String result = mf.format(args);
        assertEquals("Let's meet at 8 o' clock at l'Orange Bleue", result);
        assertEquals("Let''s meet at {1,time,h 'o'' clock'} at l''Orange Bleue", pat);
    }

    /*
     * Test method for 'com.ibm.icu.text.MessageFormat.clone()'
     */
    public void testClone() {
        // tested already in testHashcode
    }

    /*
     * Test method for 'com.ibm.icu.text.MessageFormat.equals(Object)'
     */
    public void testEqualsObject() {
        // tested already in testHashcode
    }

    /*
     * Test method for 'com.ibm.icu.text.MessageFormat.toString()'
     */
    public void testToString() {
        // no need to test
    }
}
