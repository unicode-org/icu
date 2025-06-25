// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.segmenter;

import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.RuleBasedBreakIterator;
import java.io.InputStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Performs segmentation according to the provided rule string. The rule string must follow the
 * same guidelines as for {@link RuleBasedBreakIterator#RuleBasedBreakIterator(String)}.
 * @draft ICU 78
 */
public class RuleBasedSegmenter implements Segmenter {

  private final BreakIterator breakIterPrototype;

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
  @Override
  public Segments segment(CharSequence s) {
    return new SegmentsImpl(breakIterPrototype, s);
  }

  /**
   * @return a builder for constructing {@code RuleBasedSegmenter}
   * @draft ICU 78
   */
  public static Builder builder() {
    return new Builder();
  }

  private RuleBasedSegmenter(BreakIterator breakIter) {
    breakIterPrototype = breakIter;
  }

  /**
   * Builder for {@link RuleBasedSegmenter}
   * @draft ICU 78
   */
  public static class Builder {

    private BreakIterator breakIter = null;

    private Builder() { }

    /**
     * Sets the rule string for segmentation.
     * @param rules rule string.  The rule string must follow the same guidelines as for
     *     {@link RuleBasedBreakIterator#getInstanceFromCompiledRules(InputStream)}.
     * @draft ICU 78
     */
    public Builder setRules(String rules) {
      if (rules == null) {
        throw new IllegalArgumentException("rules cannot be set to null.");
      }
      try {
        breakIter = new RuleBasedBreakIterator(rules);
        return this;
      } catch (RuntimeException rte) {
        throw new IllegalArgumentException("The provided rule string is invalid"
            + " or there was an error in creating the RuleBasedSegmenter.", rte);
      }
    }

    /**
     * Builds the {@code Segmenter}
     * @return the constructed {@code Segmenter} instance
     * @draft ICU 78
     */
    public Segmenter build() {
      if (breakIter == null) {
        throw new IllegalArgumentException("A rule string must be set.");
      } else {
        return new RuleBasedSegmenter(breakIter);
      }
    }
  }
}
