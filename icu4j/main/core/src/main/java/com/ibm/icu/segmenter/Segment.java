// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.segmenter;

/**
 * A simple struct to represent an element of the segmentation result. The {@code start} and
 * {@code limit} indices correspond to {@code source}, the input {@code CharSequence} that was
 * originally passed to the {@code Segmenter}. {@code start} and {@code limit} are inclusive and
 * exclusive boundaries, respectively.
 * @draft ICU 78
 */
public class Segment {

  /**
   * @draft ICU 78
   */
  public final int start;

  /**
   * @draft ICU 78
   */
  public final int limit;

  /**
   * @draft ICU 78
   */
  public final int ruleStatus = 0;

  private final CharSequence source;

  Segment(int start, int limit, CharSequence source) {
    this.start = start;
    this.limit = limit;
    this.source = source;
  }

  /**
   * Returns the subsequence represented by this {@code Segment}
   * @return a new {@code CharSequence} object that is the subsequence represented by this
   * {@code Segment}.
   * @draft ICU 78
   */
  public CharSequence getSubSequence() {
    return source.subSequence(start, limit);
  }
}
