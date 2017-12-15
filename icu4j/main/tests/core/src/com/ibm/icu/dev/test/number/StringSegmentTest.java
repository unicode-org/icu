// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.dev.test.number;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ibm.icu.impl.number.parse.StringSegment;

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
        segment.adjustOffset(3);
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
        segment.adjustOffset(3);
        assertCharSequenceEquals("radio 📻", segment);
        segment.setLength(5);
        assertCharSequenceEquals("radio", segment);
    }

    @Test
    public void testGetCodePoint() {
        StringSegment segment = new StringSegment(SAMPLE_STRING, false);
        assertEquals(0x1F4FB, segment.getCodePoint());
        segment.setLength(1);
        assertEquals(-1, segment.getCodePoint());
        segment.resetLength();
        segment.adjustOffset(1);
        assertEquals(-1, segment.getCodePoint());
        segment.adjustOffset(1);
        assertEquals(0x20, segment.getCodePoint());
    }

    @Test
    public void testIsLeadingSurrogate() {
        StringSegment segment = new StringSegment(SAMPLE_STRING, false);
        assertFalse(segment.isLeadingSurrogate());
        segment.setLength(1);
        assertTrue(segment.isLeadingSurrogate());
        segment.adjustOffset(1);
        segment.setLength(1);
        assertFalse(segment.isLeadingSurrogate()); // trail, not lead
    }

    @Test
    public void testCommonPrefixLength() {
        StringSegment segment = new StringSegment(SAMPLE_STRING, false);
        assertEquals(11, segment.getCommonPrefixLength(SAMPLE_STRING));
        assertEquals(4, segment.getCommonPrefixLength("📻 r"));
        assertEquals(3, segment.getCommonPrefixLength("📻 x"));
        assertEquals(0, segment.getCommonPrefixLength("x"));
        assertEquals(0, segment.getCommonPrefixLength(""));
        segment.adjustOffset(3);
        assertEquals(0, segment.getCommonPrefixLength("RADiO"));
        assertEquals(5, segment.getCommonPrefixLength("radio"));
        assertEquals(2, segment.getCommonPrefixLength("rafio"));
        assertEquals(0, segment.getCommonPrefixLength("fadio"));
        assertEquals(0, segment.getCommonPrefixLength(""));
        segment.setLength(3);
        assertEquals(3, segment.getCommonPrefixLength("radio"));
        assertEquals(2, segment.getCommonPrefixLength("rafio"));
        assertEquals(0, segment.getCommonPrefixLength("fadio"));
        assertEquals(0, segment.getCommonPrefixLength(""));
        segment.resetLength();
        segment.setOffset(11); // end of string
        assertEquals(0, segment.getCommonPrefixLength("foo"));
    }

    @Test
    public void testIgnoreCase() {
        StringSegment segment = new StringSegment(SAMPLE_STRING, true);
        assertEquals(11, segment.getCommonPrefixLength(SAMPLE_STRING));
        assertEquals(0, segment.getCommonPrefixLength("x"));
        segment.setOffset(3);
        assertEquals(5, segment.getCommonPrefixLength("RAdiO"));
    }

    private static void assertCharSequenceEquals(CharSequence a, CharSequence b) {
        assertEquals(a.length(), b.length());
        for (int i = 0; i < a.length(); i++) {
            assertEquals(a.charAt(i), b.charAt(i));
        }
    }
}
