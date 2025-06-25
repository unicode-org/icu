// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.segmenter;

import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * An interface that represents the segmentation results, including the APIs for iteration therein,
 * that are yielded from passing an input {@code CharSequence} to a {@code Segmenter}.
 *
 * <p>The segmentation results can be provided either as the segmentation boundary indices
 * ({code int}s) or as segments, which are represented by the {@link Segment} class. In turn, the
 * {@code Segment} object can also provide the subsequence of the original input that it
 * represents.
 *
 * <p>Example:
 *
 * <blockquote><pre>
 * Segmenter wordSeg =
 *     LocalizedSegmenter.builder()
 *         .setLocale(ULocale.forLanguageTag("de"))
 *         .setSegmentationType(SegmentationType.WORD)
 *         .build();
 *
 * Segments segments = wordSeg.segment("Das 21ste Jahrh. ist das beste.");
 *
 * List<CharSequence> words = segments.subSequences().collect(Collectors.toList());
 * </pre></blockquote>
 *
 * @see Segmenter
 * @see Segment
 * @draft ICU 78
 */
public interface Segments {

  /**
   * Returns a {@code Stream} of the {@code CharSequence}s for all of the segments in the source
   * sequence. Start from the beginning of the sequence and iterate forwards until the end.
   * @return a {@code Stream} of all {@code Segments} in the source sequence.
   * @draft ICU 78
   */
  default Stream<CharSequence> subSequences() {
    return segments().map(Segment::getSubSequence);
  }

  /**
   * Returns the segment that contains index {@code i}. Containment is inclusive of the start index
   * and exclusive of the limit index.
   *
   * <p>Specifically, the containing segment is defined as the segment with start {@code s} and
   * limit {@code  l} such that {@code  s ≤ i < l}.</p>
   * @param i index in the input {@code CharSequence} to the {@code Segmenter}
   * @throws IndexOutOfBoundsException if {@code i} is less than 0 or greater than or equal to the
   *     length of the input {@code CharSequence} to the {@code Segmenter}
   * @return A segment that either starts at or contains index {@code i}
   * @draft ICU 78
   */
  Segment segmentAt(int i);

  /**
   * Returns a {@code Stream} of all {@code Segment}s in the source sequence. Start with the first
   * and iterate forwards until the end of the sequence.
   *
   * <p>This is equivalent to {@code segmentsFrom(0)}.</p>
   * @return a {@code Stream} of all {@code Segments} in the source sequence.
   * @draft ICU 78
   */
  default Stream<Segment> segments() {
    return segmentsFrom(0);
  }

  /**
   * Returns a {@code Stream} of all {@code Segment}s in the source sequence where all segment limits
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
   * @draft ICU 78
   */
  Stream<Segment> segmentsFrom(int i);

  /**
   * Returns a {@code Stream} of all {@code Segment}s in the source sequence where all segment
   * limits {@code  l} satisfy {@code l ≤ i}. Iteration moves backwards.
   *
   * <p>This means that the all segments in the stream come before the one that
   * is returned by {@code segmentAt(i)}. A segment is not considered to contain index {@code i} if
   * {code i} is equal to limit {@code l}. Thus, "before" encapsulates the invariant
   * {@code l ≤ i}.</p>
   * @param i index in the input {@code CharSequence} to the {@code Segmenter}
   * @return a {@code Stream} of all {@code Segment}s before {@code i}
   * @draft ICU 78
   */
  Stream<Segment> segmentsBefore(int i);

  /**
   * Returns whether offset {@code i} is a segmentation boundary. Throws an exception when
   * {@code i} is not a valid index position for the source sequence.
   * @param i index in the input {@code CharSequence} to the {@code Segmenter}
   * @throws IllegalArgumentException if {@code i} is less than 0 or greater than the length of the
   *     input {@code CharSequence} to the {@code Segmenter}
   * @return Returns whether offset {@code i} is a segmentation boundary.
   * @draft ICU 78
   */
  boolean isBoundary(int i);

  /**
   * Returns all segmentation boundaries, starting from the beginning and moving forwards.
   *
   * <p><b>Note:</b> {@code boundaries() != boundariesAfter(0)}.
   * This difference naturally results from the strict inequality condition in boundariesAfter,
   * and the fact that 0 is the first boundary returned from the start of an input sequence.</p>
   * @return An {@code IntStream} of all segmentation boundaries, starting at the first
   * boundary with index 0, and moving forwards in the input sequence.
   * @draft ICU 78
   */
  default IntStream boundaries() {
    return boundariesAfter(-1);
  }

  /**
   * Returns all segmentation boundaries after the provided index.  Iteration moves forwards.
   * @param i index in the input {@code CharSequence} to the {@code Segmenter}
   * @return An {@code IntStream} of all boundaries {@code b} such that {@code b > i}
   * @draft ICU 78
   */
  IntStream boundariesAfter(int i);

  /**
   * Returns all segmentation boundaries on or before the provided index. Iteration moves backwards.
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
   * @draft ICU 78
   */
  IntStream boundariesBackFrom(int i);

  //
  // Inner enums/classes in common for other inner classes
  //

  enum IterationDirection {
    FORWARDS,
    BACKWARDS,
  }
}
