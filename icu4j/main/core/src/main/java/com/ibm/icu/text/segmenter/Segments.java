package com.ibm.icu.text.segmenter;

import com.ibm.icu.text.BreakIterator;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public interface Segments {
  
  Stream<CharSequence> subSequences();

  /**
   * Return the segment that contains index {@code i}. Containment is inclusive of the start index
   * and exclusive of the limit index.
   *
   * <p>Specifically, the containing segment is defined as the segment with start {@code s} and limit
   * {@code  l} such that {@code  s ≤ i < l}.</p>
   * @param i index in the input {@code CharSequence} to the {@code Segmenter}
   * @throws IllegalArgumentException if {@code i} is less than 0 or greater than the length of the
   *    input {@code CharSequence} to the {@code Segmenter}
   * @return A segment that either starts at or contains index {@code i}
   */
  Segment segmentAt(int i);

  /**
   * Return a {@code Stream} of all {@code Segment}s in the source sequence. Start with the first
   * and iterate forwards until the end of the sequence.
   *
   * <p>This is equivalent to {@code segmentsFrom(0)}.</p>
   * @return a {@code Stream} of all {@code Segments} in the source sequence.
   */
  Stream<Segment> segments();

  /**
   * Return a {@code Stream} of all {@code Segment}s in the source sequence where all segment limits
   * {@code  l} satisfy {@code i < l}.  Iteration moves forwards.
   *
   * <p>This means that the first segment in the stream is the same
   * as what is returned by {@code segmentAt(i)}.</p>
   *
   * <p>The word "from" is used here to mean "at or after", with the semantics of "at" for a
   * {@code Segment} defined by {@link #segmentAt(int)}}. We cannot describe the segments all as
   * being "after" since the first segment might contain {@code i} in the middle, meaning that
   * in the forward direction, its start position precedes {@code i}.</p>
   *
   * <p>{@code segmentsFrom} and {@link #segmentsBefore(int)} create a partitioning of the space of
   * all {@code Segment}s.</p>
   * @param i index in the input {@code CharSequence} to the {@code Segmenter}
   * @return a {@code Stream} of all {@code Segment}s at or after {@code i}
   */
  Stream<Segment> segmentsFrom(int i);

  /**
   * Return a {@code Stream} of all {@code Segment}s in the source sequence where all segment limits
   * {@code  l} satisfy {@code l ≤ i}. Iteration moves backwards.
   *
   * <p>This means that the all segments in the stream come before the one that
   * is returned by {@code segmentAt(i)}. A segment is not considered to contain index {@code i} if
   * {code i} is equal to limit {@code l}. Thus, "before" encapsulates the invariant
   * {@code l ≤ i}.</p>
   * @param i index in the input {@code CharSequence} to the {@code Segmenter}
   * @return a {@code Stream} of all {@code Segment}s before {@code i}
   */
  Stream<Segment> segmentsBefore(int i);

  Function<Segment, CharSequence> segmentToSequenceFn();

  /**
   * Returns whether offset {@code i} is a segmentation boundary. Throws an exception when
   * {@code i} is not a valid index position for the source sequence.
   * @param i index in the input {@code CharSequence} to the {@code Segmenter}
   * @throws IllegalArgumentException if {@code i} is less than 0 or greater than the length of the
   *     input {@code CharSequence} to the {@code Segmenter}
   * @return Returns whether offset {@code i} is a segmentation boundary.
   */
  boolean isBoundary(int i);

  /**
   * Return all segmentation boundaries, starting from the beginning and moving forwards.
   *
   * <p><b>Note:</b> {@code boundaries() != boundariesAfter(0)}.
   * This difference naturally results from the strict inequality condition in boundariesAfter,
   * and the fact that 0 is the first boundary returned from the start of an input sequence.</p>
   * @return An {@code IntStream} of all segmentation boundaries, starting at the first
   * boundary with index 0, and moving forwards in the input sequence.
   */
  IntStream boundaries();

  /**
   * Return all segmentation boundaries after the provided index.  Iteration moves forwards.
   * @param i index in the input {@code CharSequence} to the {@code Segmenter}
   * @return An {@code IntStream} of all boundaries {@code b} such that {@code b > i}
   */
  IntStream boundariesAfter(int i);

  /**
   * Return all segmentation boundaries on or before the provided index. Iteration moves backwards.
   *
   * <p>The phrase "back from" is used to indicate both that: 1) boundaries are "on or before" the
   * input index; 2) the direction of iteration is backwards (towards the beginning).
   * "on or before" indicates that the result set is {@code b} where {@code b ≤ i}, which is a weak
   * inequality, while "before" might suggest the strict inequality {@code b < i}.</p>
   *
   * <p>{@code boundariesBackFrom} and {@link #boundariesAfter(int)} create a partitioning of the
   *     space of all boundaries.</p>
   * @param i index in the input {@code CharSequence} to the {@code Segmenter}
   * @return An {@code IntStream} of all boundaries {@code b} such that {@code b ≤ i}
   */
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
  //   3) do not link the multiple Segment objects and the single Segments object via a field, and
  //      instead provide a function on Segments that can convert each Segment into a CharSequence
  class Segment {
    public final int start;
    public final int limit;
    public final int ruleStatus = 0;
    private final CharSequence source;

    public Segment(int start, int limit, CharSequence source) {
      this.start = start;
      this.limit = limit;
      this.source = source;
    }

    /**
     * Return the subsequence represented by this {@code Segment}
     * @return a new {@code CharSequence} object that is the subsequence represented by this
     * {@code Segment}.
     */
    public CharSequence getSubSequence() {
      return source.subSequence(this.start, this.limit);
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

  class BoundaryIteratorOfInts {
    BreakIterator breakIter;
    IterationDirection direction;
    int currIdx;

    BoundaryIteratorOfInts(BreakIterator breakIter, IterationDirection direction, int startIdx) {
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

    public boolean hasNext() {
      return this.currIdx != BreakIterator.DONE;
    }

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

  class SegmentSpliterator implements Spliterator.OfInt {

    private final BoundaryIteratorOfInts iter;

    SegmentSpliterator(BreakIterator breakIter, IterationDirection direction, int startIdx) {
      iter = new BoundaryIteratorOfInts(breakIter, direction, startIdx);
    }

    @Override
    public OfInt trySplit() {
      // The elements of the Stream represent an iteration through a string, and is thus inherently
      // stateful. Therefore, splitting this Stream does not make sense. Ex: splitting the Stream
      // is tantamount to discarding the segment subtended by the end value (index into the input
      // string) of one substream and the beginning value of the next substream.
      return null;
    }

    @Override
    public long estimateSize() {
      // The number of segments per input size depends on language, script, and
      // the content of the input string, and thus is hard to estimate without
      // sacrificing performance. Thus, returning `Long.MAX_VALUE`, according
      // to the API, to mean "unknown, or too expensive to compute".
      return Long.MAX_VALUE;
    }

    @Override
    public int characteristics() {
      return Spliterator.DISTINCT  // BreakIterator always advances
          | Spliterator.IMMUTABLE // design of Segmenter API is to provide an immutable view of
                                  // segmentation by preventing the input string from mutating
                                  // in the underlying BreakIterator
          | Spliterator.NONNULL   // primtive int is non-null
          | Spliterator.ORDERED   // BreakIterator always advances, and in a single direction
          ;
    }

    @Override
    public boolean tryAdvance(IntConsumer action) {
      if (action == null) {
        throw new NullPointerException();
      }
      if (iter.hasNext()) {
        action.accept(iter.next());
        return true;
      } else {
        return false;
      }
    }
  }

}
