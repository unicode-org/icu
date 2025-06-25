// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.segmenter;

import com.ibm.icu.text.BreakIterator;

/**
 * An interface that defines APIs for segmentation in terms of segments and boundaries, and
 * enforces immutable stateless iteration over the segmentation result yielded from an input
 * {@code CharSequence}.
 *
 * <p>{@code Segmenter} is designed to be a followup to the {@code BreakIterator} in providing
 * segmentation functionality. {@code Segmenter} provides immutable iteration, higher level
 * constructs like {@code Segment}s and {@code CharSequence}s as return types, and Java programmer
 * conveniences like {@code Stream}s in its APIs.
 *
 * <p>Iteration over the input sequences is made immutable by separating the design into two parts,
 * each represented by an interface. The {@code Segmenter} interface represents the construction of
 * the object that encapsulates the segmentation logic. The {@link Segments} interface represents
 * the result of segmentation being performed for a specific given input {@code CharSequence}.
 * {@code Segments} APIs also provide {@code Stream}s to support iteration over the segmentation
 * results in a stateless manner.
 *
 * @see Segments
 * @see BreakIterator
 * @draft ICU 78
 */
public interface Segmenter {

  /**
   * Returns a {@link Segments} object that encapsulates the segmentation of the input
   * {@code CharSequence}. The {@code Segments} object, in turn, provides the main APIs to support
   * traversal over the resulting segments and boundaries via the Java {@code Stream} abstraction.
   * @param s input {@code CharSequence} on which segmentation is performed. The input must not be
   *     modified while using the resulting {@code Segments} object.
   * @return A {@code Segments} object with APIs to access the results of segmentation, including
   *     APIs that return {@code Stream}s of the segments and boundaries.
   * @draft ICU 78
   */
  Segments segment(CharSequence s);

}
