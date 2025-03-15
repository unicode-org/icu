package com.ibm.icu.text.segmenter;

import com.ibm.icu.text.BreakIterator;
import java.util.Iterator;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public interface Segments {
  
  Stream<CharSequence> subSequences();

  Segment segmentAt(int i);

  Stream<Segment> segments();

  Stream<Segment> segmentsFrom(int i);

  Stream<Segment> segmentsBefore(int i);

  Function<Segment, CharSequence> segmentToSequenceFn();

  /**
   * Returns whether offset {@code i} is a segmentation boundary. Throws an exception when
   * {@code i} is not a valid boundary position for the source sequence.
   * @param i
   * @return
   */
  boolean isBoundary(int i);

  IntStream boundaries();

  IntStream boundariesAfter(int i);

  IntStream boundariesBackFrom(int i);

  //
  // Inner enums/classes in common for other inner classes
  //

  enum IterationDirection {
    FORWARDS,
    BACKWARDS,
  }

  //
  // Inner classes for Segment, SegmentIterable, and SegmentIterator
  //

  // TODO: consider options in design for potential memory usage optimization:
  //   1) keep simple class with public fields, but requires field per Segment to point to source
  //   2) make Segment an interface (getSource, getStart, getLimit, getRuleStatus, newSegment), and
  //      maybe an abstract class that implements the interface, maybe with a default method impl
  //      for convenience for getting (allocating & returning) the subsequence
  class Segment {
    public final int start;
    public final int limit;
    public final int ruleStatus = 0;
    public final CharSequence source;

    public Segment(int start, int limit, CharSequence source) {
      this.start = start;
      this.limit = limit;
      this.source = source;
    }
  }

  /**
   * This {@code Iterable} exists to enable the creation of a {@code Spliterator} that in turn
   * enables the creation of a lazy {@code Stream}.
   */
  class SegmentIterable implements Iterable<Segment> {
    BreakIterator breakIter;
    final IterationDirection direction;
    int startIdx;
    final CharSequence source;

    SegmentIterable(BreakIterator breakIter, IterationDirection direction, int startIdx, CharSequence source) {
      this.breakIter = breakIter;
      this.direction = direction;
      this.startIdx = startIdx;
      this.source = source;
    }

    @Override
    public Iterator<Segment> iterator() {
      return new SegmentIterator(this.breakIter, this.direction, this.startIdx, this.source);
    }
  }

  class SegmentIterator implements Iterator<Segment> {
    BreakIterator breakIter;
    final IterationDirection direction;
    int start;
    int limit;
    final CharSequence source;

    SegmentIterator(BreakIterator breakIter, IterationDirection direction, int startIdx, CharSequence source) {
      this.breakIter = breakIter;
      this.direction = direction;
      this.source = source;

      Segment segmentAtIdx = SegmentsImplUtils.segmentAt(breakIter, source, startIdx);

      if (segmentAtIdx == null) {
        this.start = BreakIterator.DONE;
      } else if (direction == IterationDirection.FORWARDS) {
        this.start = segmentAtIdx.start;
        this.limit = breakIter.following(this.start);
      } else {
        assert direction == IterationDirection.BACKWARDS;
        if (breakIter.isBoundary(startIdx)) {
          // Note: breakIter::isBoundary is a stateful operation. It resets the position in the
          // BreakIterator, which we want to ensure that the position is where we think it is.
          this.start = startIdx;
        } else {
          // Since we already called BreakIterator.isBoundary() which mutates the BreakIterator
          // position to increment forwards when the return value is false, we should call
          // BreakIterator.previous() to update the iterator position while getting the start value
          // of the segment at startIdx
          this.start = breakIter.previous();
        }
        this.limit = getDirectionBasedNextIdx();
      }
    }

    int getDirectionBasedNextIdx() {
      if (direction == IterationDirection.FORWARDS) {
        return breakIter.next();
      } else {
        assert direction == IterationDirection.BACKWARDS;
        return breakIter.previous();
      }
    }

    @Override
    public boolean hasNext() {
      return this.limit != BreakIterator.DONE;
    }

    @Override
    public Segment next() {
      Segment result;
      if (this.limit < this.start) {
        result = new Segment(this.limit, this.start, this.source);
      } else {
        result = new Segment(this.start, this.limit, this.source);
      }

      this.start = this.limit;
      this.limit = getDirectionBasedNextIdx();

      return result;
    }
  }

  //
  // Inner classes for BoundaryIterable and BoundaryIterator
  //

  class BoundaryIterable implements Iterable<Integer> {
    BreakIterator breakIter;
    IterationDirection direction;
    int startIdx;

    BoundaryIterable(BreakIterator breakIter, IterationDirection direction, int startIdx) {
      this.breakIter = breakIter;
      this.direction = direction;
      this.startIdx = startIdx;
    }

    @Override
    public Iterator<Integer> iterator() {
      return new BoundaryIterator(this.breakIter, this.direction, this.startIdx);
    }
  }

  class BoundaryIterator implements Iterator<Integer> {
    BreakIterator breakIter;
    IterationDirection direction;
    int currIdx;

    BoundaryIterator(BreakIterator breakIter, IterationDirection direction, int startIdx) {
      this.breakIter = breakIter;
      this.direction = direction;

      // TODO(ICU-22987): Remove after fixing preceding(int) to return `DONE` for negative inputs
      if (startIdx < 0 && direction == IterationDirection.BACKWARDS) {
        this.currIdx = BreakIterator.DONE;
        return;
      }

      if (direction == IterationDirection.FORWARDS) {
        this.currIdx = breakIter.following(startIdx);
      } else {
        assert direction == IterationDirection.BACKWARDS;
        this.currIdx = breakIter.preceding(startIdx);
      }
    }

    @Override
    public boolean hasNext() {
      return this.currIdx != BreakIterator.DONE;
    }

    @Override
    public Integer next() {
      int result = this.currIdx;

      if (direction == IterationDirection.FORWARDS) {
        this.currIdx = breakIter.next();
      } else {
        assert direction == IterationDirection.BACKWARDS;
        this.currIdx = breakIter.previous();
      }

      return result;
    }
  }

}
