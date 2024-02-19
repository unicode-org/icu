// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ibm.icu.impl.StringSegment;

/**
 * @author sffc
 *
 */
public class StringSegmentTest {
    static final String SAMPLE_STRING = "📻 radio 📻";

    @Test
    public void testOffset() {
        StringSegment segment = new StringSegment(SAMPLE_STRING, false);
        assertEquals(0, segment.getOffset());
        segment.adjustOffsetByCodePoint();
        assertEquals(2, segment.getOffset());
        segment.adjustOffset(1);
        assertEquals(3, segment.getOffset());
        segment.adjustOffset(2);
        assertEquals(5, segment.getOffset());
        segment.setOffset(4);
        assertEquals(4, segment.getOffset());
    }

    @Test
    public void testLength() {
        StringSegment segment = new StringSegment(SAMPLE_STRING, false);
        assertEquals(11, segment.length());
        segment.adjustOffset(3);
        assertEquals(8, segment.length());
        segment.setLength(4);
        assertEquals(4, segment.length());
        segment.setOffset(5);
        assertEquals(2, segment.length());
        segment.resetLength();
        assertEquals(6, segment.length());
    }

    @Test
    public void testCharAt() {
        StringSegment segment = new StringSegment(SAMPLE_STRING, false);
        assertCharSequenceEquals(SAMPLE_STRING, segment);
        assertTrue(segment.contentEquals(SAMPLE_STRING));
        segment.adjustOffset(3);
        assertCharSequenceEquals("radio 📻", segment);
        assertTrue(segment.contentEquals("radio 📻"));
        assertFalse(segment.contentEquals(SAMPLE_STRING));
        segment.setLength(5);
        assertCharSequenceEquals("radio", segment);
        assertTrue(segment.contentEquals("radio"));
        assertFalse(segment.contentEquals(SAMPLE_STRING));
    }

    @Test
    public void testGetCodePoint() {
        StringSegment segment = new StringSegment(SAMPLE_STRING, false);
        assertEquals(0x1F4FB, segment.getCodePoint());
        segment.setLength(1);
        assertEquals(0xD83D, segment.getCodePoint());
        segment.resetLength();
        segment.adjustOffset(1);
        assertEquals(0xDCFB, segment.getCodePoint());
        segment.adjustOffset(1);
        assertEquals(0x20, segment.getCodePoint());
    }

    @Test
    public void testCommonPrefixLength() {
        StringSegment segment = new StringSegment(SAMPLE_STRING, true);
        assertEquals(11, segment.getCommonPrefixLength(SAMPLE_STRING));
        assertEquals(4, segment.getCommonPrefixLength("📻 r"));
        assertEquals(3, segment.getCommonPrefixLength("📻 x"));
        assertEquals(0, segment.getCommonPrefixLength("x"));
        segment.adjustOffset(3);
        assertEquals(5, segment.getCommonPrefixLength("raDio"));
        assertEquals(5, segment.getCommonPrefixLength("radio"));
        assertEquals(2, segment.getCommonPrefixLength("rafio"));
        assertEquals(0, segment.getCommonPrefixLength("fadio"));
        assertEquals(5, segment.getCaseSensitivePrefixLength("radio"));
        assertEquals(2, segment.getCaseSensitivePrefixLength("raDio"));
        segment.setLength(3);
        assertEquals(3, segment.getCommonPrefixLength("radio"));
        assertEquals(2, segment.getCommonPrefixLength("rafio"));
        assertEquals(0, segment.getCommonPrefixLength("fadio"));
        segment.resetLength();
        segment.setOffset(11); // end of string
        assertEquals(0, segment.getCommonPrefixLength("foo"));
    }

    private static void assertCharSequenceEquals(CharSequence a, CharSequence b) {
        assertEquals(a.length(), b.length());
        for (int i = 0; i < a.length(); i++) {
            assertEquals(a.charAt(i), b.charAt(i));
        }
    }
}
