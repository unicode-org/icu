package com.ibm.icu.text.segmenter;

import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.segmenter.Segments.BoundaryIterable;
import com.ibm.icu.text.segmenter.Segments.IterationDirection;
import com.ibm.icu.text.segmenter.Segments.Segment;
import com.ibm.icu.text.segmenter.Segments.SegmentIterable;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


// Global TODO: make initialization of breakIterator a prerequisite
public class SegmentsImplUtils {

  public static boolean isBoundary(BreakIterator breakIter, CharSequence source, int i) {
    breakIter.setText(source);

    return breakIter.isBoundary(i);
  }

  public static Stream<CharSequence> subSequences(BreakIterator breakIter, CharSequence sourceSequence) {
    return segments(breakIter, sourceSequence).map(segmentToSequenceFn(sourceSequence));
  }

  public static Segment segmentAt(BreakIterator breakIter, CharSequence sourceSequence, int i) {
    // TODO: make initialization of breakIterator a prerequisite
    breakIter.setText(sourceSequence);

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
  }

  public static Stream<Segment> segments(BreakIterator breakIter, CharSequence sourceSequence) {
    return segmentsFrom(breakIter, sourceSequence, 0);
  }

  public static Stream<Segment> segmentsFrom(BreakIterator breakIter, CharSequence sourceSequence, int i) {
    breakIter.setText(sourceSequence);

    // create a Stream from a Spliterator of an Iterable so that the Stream can be lazy, not eager
    SegmentIterable iterable = new SegmentIterable(breakIter, IterationDirection.FORWARDS, i, sourceSequence);
    return StreamSupport.stream(iterable.spliterator(), false);
  }

  public static Stream<Segment> segmentsBefore(BreakIterator breakIter, CharSequence sourceSequence, int i) {
     breakIter.setText(sourceSequence);

    // create a Stream from a Spliterator of an Iterable so that the Stream can be lazy, not eager
    SegmentIterable iterable = new SegmentIterable(breakIter, IterationDirection.BACKWARDS, i, sourceSequence);
    return StreamSupport.stream(iterable.spliterator(), false);
  }

  public static Segment segmentAfterIndex(BreakIterator breakIter, CharSequence sourceSequence, int i) {
    breakIter.setText(sourceSequence);

    int start = breakIter.following(i);
    if (start == BreakIterator.DONE) {
      return null;
    }

    int limit = breakIter.next();
    if (limit == BreakIterator.DONE) {
      return null;
    }

    return new Segment(start, limit, sourceSequence);
  }

  public static Segment segmentBeforeIndex(BreakIterator breakIter, CharSequence sourceSequence, int i) {
    breakIter.setText(sourceSequence);


    // TODO(ICU-22987): Remove after fixing preceding(int) to return `DONE` for negative inputs
    if (i < 0) {
      // return the same thing as we would if preceding() returned DONE
      return null;
    }

    int start = breakIter.preceding(i);
    int limit = breakIter.previous();

    if (start == BreakIterator.DONE || limit == BreakIterator.DONE) {
      return null;
    }

    assert limit <= start;

    return new Segment(limit, start, sourceSequence);
  }

  public static Function<Segment, CharSequence> segmentToSequenceFn(CharSequence sourceSequence) {
    return segment -> sourceSequence.subSequence(segment.start, segment.limit);
  }

  public static IntStream boundaries(BreakIterator breakIter, CharSequence sourceSequence) {
    return boundariesAfter(breakIter, sourceSequence, -1);
  }

  public static IntStream boundariesAfter(BreakIterator breakIter, CharSequence sourceSequence, int i) {
    breakIter.setText(sourceSequence);

    // create a Stream from a Spliterator of an Iterable so that the Stream can be lazy, not eager
    // TODO: optimize IntStream creation to avoid autoboxing
    BoundaryIterable iterable = new BoundaryIterable(breakIter, IterationDirection.FORWARDS, i);
    Stream<Integer> boundariesAsIntegers =  StreamSupport.stream(iterable.spliterator(), false);
    return boundariesAsIntegers.mapToInt(Integer::intValue);
  }

  public static IntStream boundariesBackFrom(BreakIterator breakIter, CharSequence sourceSequence, int i) {
    // TODO: make initialization of breakIterator a prerequisite
    breakIter.setText(sourceSequence);

    int sourceLength = sourceSequence.length();
    if (i < 0) {
      return IntStream.empty();
    }

    boolean isOnBoundary = i <= sourceLength && isBoundary(breakIter, sourceSequence, i);
    int backFromIdx = isOnBoundary ? i + 1 : i;

    // create a Stream from a Spliterator of an Iterable so that the Stream can be lazy, not eager
    // TODO: optimize IntStream creation to avoid autoboxing
    BoundaryIterable iterable = new BoundaryIterable(breakIter, IterationDirection.BACKWARDS, backFromIdx);
    Stream<Integer> boundariesAsIntegers =  StreamSupport.stream(iterable.spliterator(), false);
    return boundariesAsIntegers.mapToInt(Integer::intValue);
  }

}
