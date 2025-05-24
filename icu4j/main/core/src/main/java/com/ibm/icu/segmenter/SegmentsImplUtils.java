// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.segmenter;

import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.segmenter.Segments.IterationDirection;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


class SegmentsImplUtils {

  static boolean isBoundary(BreakIterator breakIter, CharSequence source, int i) {
    return breakIter.isBoundary(i);
  }

  static Segment segmentAt(BreakIterator breakIter, CharSequence sourceSequence, int i) {
    try {
      int start;
      int limit;

      boolean isBoundary = breakIter.isBoundary(i);

      if (isBoundary) {
        start = i;
        limit = breakIter.next();
      } else {
        // BreakIterator::isBoundary(i) will advance forwards to the next boundary if the argument
        // is not a boundary.
        limit = breakIter.current();
        start = breakIter.previous();
      }

      if (start != BreakIterator.DONE && limit != BreakIterator.DONE) {
        return new Segment(start, limit, sourceSequence);
      } else {
        return null;
      }
    } catch (IllegalArgumentException iae) {
      // Catch the error that is thrown by the implementation helper method inside BreakIterator
      // (called checkOffset) whenever the index passed to `.isBoundary(int)` is out of bounds
      // in the original string. Since IndexOutOfBoundsException is more appropriate, throw that
      // instead.
      throw new IndexOutOfBoundsException(i);
    }
  }

  static Stream<Segment> segments(BreakIterator breakIter, CharSequence sourceSequence) {
    return segmentsFrom(breakIter, sourceSequence, 0);
  }

  static Stream<Segment> segmentsFrom(BreakIterator breakIter, CharSequence sourceSequence, int i) {
    breakIter.setText(sourceSequence);

    // create a Stream from a Spliterator of an Iterable so that the Stream can be lazy, not eager
    SegmentIterable iterable = new SegmentIterable(breakIter, IterationDirection.FORWARDS, i, sourceSequence);
    return StreamSupport.stream(iterable.spliterator(), false);
  }

  static Stream<Segment> segmentsBefore(BreakIterator breakIter, CharSequence sourceSequence, int i) {
     breakIter.setText(sourceSequence);

    // create a Stream from a Spliterator of an Iterable so that the Stream can be lazy, not eager
    SegmentIterable iterable = new SegmentIterable(breakIter, IterationDirection.BACKWARDS, i, sourceSequence);
    return StreamSupport.stream(iterable.spliterator(), false);
  }

  static Function<Segment, CharSequence> segmentToSequenceFn(CharSequence sourceSequence) {
    return segment -> sourceSequence.subSequence(segment.start, segment.limit);
  }

  static IntStream boundaries(BreakIterator breakIter, CharSequence sourceSequence) {
    return boundariesAfter(breakIter, sourceSequence, -1);
  }

  static IntStream boundariesAfter(BreakIterator breakIter, CharSequence sourceSequence, int i) {
    breakIter.setText(sourceSequence);

    // create a Stream from a Spliterator of an Iterable so that the Stream can be lazy, not eager
    return StreamSupport.intStream(new SegmentSpliterator(breakIter, sourceSequence, IterationDirection.FORWARDS, i), false);
  }

  static IntStream boundariesBackFrom(BreakIterator breakIter, CharSequence sourceSequence, int i) {
    // create a Stream from a Spliterator of an Iterable so that the Stream can be lazy, not eager
    return StreamSupport.intStream(new SegmentSpliterator(breakIter, sourceSequence, IterationDirection.BACKWARDS, i), false);
  }

}
