// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.segmenter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.segmenter.LocalizedSegmenter;
import com.ibm.icu.segmenter.LocalizedSegmenter.SegmentationType;
import com.ibm.icu.segmenter.Segment;
import com.ibm.icu.segmenter.Segmenter;
import com.ibm.icu.segmenter.Segments;
import com.ibm.icu.util.ULocale;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SegmentsTest extends CoreTestFmwk {

    @Test
    public void testSegments() {
        Segmenter enWordSegmenter =
                LocalizedSegmenter.builder()
                        .setLocale(ULocale.ENGLISH)
                        .setSegmentationType(SegmentationType.WORD)
                        .build();

        String source1 = "The quick brown fox jumped over the lazy dog.";

        // Create new Segments for source1
        Segments segments1 = enWordSegmenter.segment(source1);

        List<Segment> segments = segments1.segments().collect(Collectors.toList());

        assertEquals("first segment start", 0, segments.get(0).start);
        assertEquals("first segment limit", 3, segments.get(0).limit);

        assertEquals("second segment start", 3, segments.get(1).start);
        assertEquals("second segment limit", 4, segments.get(1).limit);
    }

    @Test
    public void testMultipleSegmentObjectsFromSegmenter() {
        Segmenter enWordSegmenter =
                LocalizedSegmenter.builder()
                        .setLocale(ULocale.ENGLISH)
                        .setSegmentationType(SegmentationType.WORD)
                        .build();

        String source1 = "The quick brown fox jumped over the lazy dog.";
        String source2 = "Sphinx of black quartz, judge my vow.";
        String source3 = "How vexingly quick daft zebras jump!";

        List<CharSequence> exp1 =
                Arrays.asList(
                        "The", " ", "quick", " ", "brown", " ", "fox", " ", "jumped", " ", "over",
                        " ", "the", " ", "lazy", " ", "dog", ".");
        List<CharSequence> exp2 =
                Arrays.asList(
                        "Sphinx", " ", "of", " ", "black", " ", "quartz", ",", " ", "judge", " ",
                        "my", " ", "vow", ".");
        List<CharSequence> exp3 =
                Arrays.asList(
                        "How",
                        " ",
                        "vexingly",
                        " ",
                        "quick",
                        " ",
                        "daft",
                        " ",
                        "zebras",
                        " ",
                        "jump",
                        "!");

        // Create new Segments for source1
        Segments segments1 = enWordSegmenter.segment(source1);
        List<CharSequence> act1 = segments1.subSequences().collect(Collectors.toList());
        assertThat(act1, is(exp1));

        // Create new Segments for source2
        Segments segments2 = enWordSegmenter.segment(source2);
        List<CharSequence> act2 = segments2.subSequences().collect(Collectors.toList());
        assertThat(act2, is(exp2));

        // Check that Segments for source1 is unaffected
        act1 = segments1.subSequences().collect(Collectors.toList());
        assertThat(act1, is(exp1));

        // Create new Segments for source3
        Segments segments3 = enWordSegmenter.segment(source3);
        List<CharSequence> act3 = segments3.subSequences().collect(Collectors.toList());
        assertThat(act3, is(exp3));

        // Check that Segments for source1 is unaffected
        act1 = segments1.subSequences().collect(Collectors.toList());
        assertThat(act1, is(exp1));

        // Check that Segments for source2 is unaffected
        act2 = segments2.subSequences().collect(Collectors.toList());
        assertThat(act2, is(exp2));
    }

    @Test
    public void testIsBoundary() {
        Segmenter enWordSegmenter =
                LocalizedSegmenter.builder()
                        .setLocale(ULocale.ENGLISH)
                        .setSegmentationType(LocalizedSegmenter.SegmentationType.WORD)
                        .build();

        String source1 = "The quick brown fox jumped over the lazy dog.";

        // Create new Segments for source1
        Segments segments1 = enWordSegmenter.segment(source1);
        List<Segment> segments = segments1.segments().collect(Collectors.toList());

        // Make space for n+1 possible boundary positions for an input of length n
        BitSet actBoundaries = new BitSet(source1.length() + 1);
        for (Segment seg : segments) {
            actBoundaries.set(seg.start);
            actBoundaries.set(seg.limit);
        }

        // since we are iterating over boundaries (not chars), we go from 0..n (not 0..n-1)
        IntStream.rangeClosed(0, source1.length())
                .forEach(
                        i -> {
                            if (actBoundaries.get(i)) {
                                assertTrue(
                                        "Detected " + i + " as boundary", segments1.isBoundary(i));
                            } else {
                                assertFalse(
                                        "Detected " + i + " as not a boundary",
                                        segments1.isBoundary(i));
                            }
                        });
    }

    @Test
    public void testSegmentsFrom_middleOfSegment() {
        Segmenter enWordSegmenter =
                LocalizedSegmenter.builder()
                        .setLocale(ULocale.ENGLISH)
                        .setSegmentationType(LocalizedSegmenter.SegmentationType.WORD)
                        .build();

        String source1 = "The quick brown fox jumped over the lazy dog.";
        int startIdx = 1;

        // Create new Segments for source1
        Segments segments1 = enWordSegmenter.segment(source1);

        List<Segment> segments = segments1.segmentsFrom(startIdx).collect(Collectors.toList());

        assertEquals("first segment start", 0, segments.get(0).start);
        assertEquals("first segment limit", 3, segments.get(0).limit);

        assertEquals("second segment start", 3, segments.get(1).start);
        assertEquals("second segment limit", 4, segments.get(1).limit);
    }

    @Test
    public void testSegmentsFrom_onBoundary() {
        Segmenter enWordSegmenter =
                LocalizedSegmenter.builder()
                        .setLocale(ULocale.ENGLISH)
                        .setSegmentationType(LocalizedSegmenter.SegmentationType.WORD)
                        .build();

        String source1 = "The quick brown fox jumped over the lazy dog.";
        int startIdx = 3;

        // Create new Segments for source1
        Segments segments1 = enWordSegmenter.segment(source1);

        List<Segment> segments = segments1.segmentsFrom(startIdx).collect(Collectors.toList());

        assertEquals("first segment start", 3, segments.get(0).start);
        assertEquals("first segment limit", 4, segments.get(0).limit);

        assertEquals("second segment start", 4, segments.get(1).start);
        assertEquals("second segment limit", 9, segments.get(1).limit);
    }

    @Test
    public void testSegmentsBefore_middleOfSegment() {
        Segmenter enWordSegmenter =
                LocalizedSegmenter.builder()
                        .setLocale(ULocale.ENGLISH)
                        .setSegmentationType(LocalizedSegmenter.SegmentationType.WORD)
                        .build();

        String source1 = "The quick brown fox jumped over the lazy dog.";
        int startIdx = 8;

        // Create new Segments for source1
        Segments segments1 = enWordSegmenter.segment(source1);

        List<Segment> segments = segments1.segmentsBefore(startIdx).collect(Collectors.toList());

        assertEquals("first segment start", 3, segments.get(0).start);
        assertEquals("first segment limit", 4, segments.get(0).limit);

        assertEquals("second segment start", 0, segments.get(1).start);
        assertEquals("second segment limit", 3, segments.get(1).limit);
    }

    @Test
    public void testSegmentsBefore_onBoundary() {
        Segmenter enWordSegmenter =
                LocalizedSegmenter.builder()
                        .setLocale(ULocale.ENGLISH)
                        .setSegmentationType(LocalizedSegmenter.SegmentationType.WORD)
                        .build();

        String source1 = "The quick brown fox jumped over the lazy dog.";
        int startIdx = 9;

        // Create new Segments for source1
        Segments segments1 = enWordSegmenter.segment(source1);

        List<Segment> segments = segments1.segmentsBefore(startIdx).collect(Collectors.toList());

        assertEquals("first segment start", 4, segments.get(0).start);
        assertEquals("first segment limit", 9, segments.get(0).limit);

        assertEquals("second segment start", 3, segments.get(1).start);
        assertEquals("second segment limit", 4, segments.get(1).limit);
    }

    @Test
    public void testBoundaries() {
        Segmenter enWordSegmenter =
                LocalizedSegmenter.builder()
                        .setLocale(ULocale.ENGLISH)
                        .setSegmentationType(SegmentationType.WORD)
                        .build();

        String source = "The quick brown fox jumped over the lazy dog.";

        // Create new Segments for source
        Segments segments = enWordSegmenter.segment(source);

        int[] exp = {0, 3, 4, 9, 10, 15, 16, 19, 20, 26, 27, 31, 32, 35, 36, 40, 41, 44, 45};

        int[] act = segments.boundaries().toArray();

        assertThat(act, is(exp));
    }

    @Test
    public void testBoundariesAfter() {
        Segmenter enWordSegmenter =
                LocalizedSegmenter.builder()
                        .setLocale(ULocale.ENGLISH)
                        .setSegmentationType(SegmentationType.WORD)
                        .build();

        String source = "The quick brown fox jumped over the lazy dog.";
        int TAKE_LIMIT = 5;

        // Create new Segments for source
        Segments segments = enWordSegmenter.segment(source);

        Object[][] casesData = {
            {"first " + TAKE_LIMIT + " before beginning", -2, new int[] {0, 3, 4, 9, 10}},
            {
                "first " + TAKE_LIMIT + " in the middle of the third segment",
                5,
                new int[] {9, 10, 15, 16, 19}
            },
            {
                "first " + TAKE_LIMIT + " on the limit of the third segment",
                9,
                new int[] {10, 15, 16, 19, 20}
            },
            {"first " + TAKE_LIMIT + " at the end", source.length(), new int[0]},
            {"first " + TAKE_LIMIT + " after the end", source.length() + 1, new int[0]},
        };

        for (Object[] caseDatum : casesData) {
            String desc = (String) caseDatum[0];
            int startIdx = (int) caseDatum[1];
            int[] exp = (int[]) caseDatum[2];

            int[] act = segments.boundariesAfter(startIdx).limit(TAKE_LIMIT).toArray();

            assertThat(desc, act, is(exp));
        }
    }

    @Test
    public void testBoundariesBackFrom() {
        Segmenter enWordSegmenter =
                LocalizedSegmenter.builder()
                        .setLocale(ULocale.ENGLISH)
                        .setSegmentationType(SegmentationType.WORD)
                        .build();

        String source = "The quick brown fox jumped over the lazy dog.";
        int TAKE_LIMIT = 5;

        // Create new Segments for source
        Segments segments = enWordSegmenter.segment(source);

        Object[][] casesData = {
            {"first " + TAKE_LIMIT + " before beginning", -2, new int[0]},
            {"first " + TAKE_LIMIT + " at the beginning", 0, new int[] {0}},
            {
                "first " + TAKE_LIMIT + " from the start of the 2nd to last segment",
                41,
                new int[] {41, 40, 36, 35, 32}
            },
            {
                "first " + TAKE_LIMIT + " in the middle of the 2nd to last segment",
                42,
                new int[] {41, 40, 36, 35, 32}
            },
            {
                "first " + TAKE_LIMIT + " at the end",
                source.length(),
                new int[] {45, 44, 41, 40, 36}
            },
            {
                "first " + TAKE_LIMIT + " after the end",
                source.length() + 1,
                new int[] {45, 44, 41, 40, 36}
            },
        };

        for (Object[] caseDatum : casesData) {
            String desc = (String) caseDatum[0];
            int startIdx = (int) caseDatum[1];
            int[] exp = (int[]) caseDatum[2];

            int[] act = segments.boundariesBackFrom(startIdx).limit(TAKE_LIMIT).toArray();

            assertThat(desc, act, is(exp));
        }
    }

    @Test
    public void testSegmentAt() {
        Segmenter enWordSegmenter =
                LocalizedSegmenter.builder()
                        .setLocale(ULocale.ENGLISH)
                        .setSegmentationType(SegmentationType.WORD)
                        .build();

        String source = "The quick brown fox jumped over the lazy dog.";

        // Create new Segments for source
        Segments segments1 = enWordSegmenter.segment(source);

        Object[][] casesData = {
            {"index before beginning", -2, null, null},
            {"index at beginning", 0, 0, 3},
            {"index in the middle of the first segment", 2, 0, 3},
            {"index in the middle of the third segment", 5, 4, 9},
            {"index right before the end", source.length() - 1, 44, 45},
            {"index at the end", source.length(), null, null},
            {"index after the end", source.length() + 1, null, null},
        };

        for (Object[] caseDatum : casesData) {
            String desc = (String) caseDatum[0];
            int startIdx = (int) caseDatum[1];
            Integer expStart = (Integer) caseDatum[2];
            Integer expLimit = (Integer) caseDatum[3];

            if (expStart == null) {
                try {
                    // this should throw an exception because the index is out of bounds
                    Segment segment = segments1.segmentAt(startIdx);
                    // if an exception isn't thrown, then we have a test failure
                    fail("segmentAt should throw for an out of bounds index");
                } catch (IndexOutOfBoundsException ioobe) {
                    // test throws expected exception, so continue
                }
            } else {
                Segment segment = segments1.segmentAt(startIdx);

                assertEquals(desc + ", start", (long) expStart.intValue(), (long) segment.start);
                assertEquals(desc + ", limit", (long) expLimit.intValue(), (long) segment.limit);
            }
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testSegmentAt_throwsIndexOutOfBoundsException() {
        Segmenter enWordSegmenter =
                LocalizedSegmenter.builder()
                        .setLocale(ULocale.ENGLISH)
                        .setSegmentationType(SegmentationType.WORD)
                        .build();

        String source = "The quick brown fox jumped over the lazy dog.";

        Segments segments = enWordSegmenter.segment(source);

        segments.segmentAt(-1);
    }
}
