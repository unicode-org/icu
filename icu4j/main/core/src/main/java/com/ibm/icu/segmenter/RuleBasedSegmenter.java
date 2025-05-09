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
 * same guidelines as for {@link RuleBasedBreakIterator#getInstanceFromCompiledRules(InputStream)}.
 * @draft ICU 78
 */
public class RuleBasedSegmenter implements Segmenter {

  private String rules;

  /**
   * @draft ICU 78
   */
  @Override
  public Segments segment(CharSequence s) {
    return new RuleBasedSegments(s, this);
  }

  /**
   * @return a builder for constructing {@code RuleBasedSegmenter}
   * @draft ICU 78
   */
  public static Builder builder() {
    return new Builder();
  }

  RuleBasedSegmenter(String rules) {
    this.rules = rules;
  }

  /**
   * @internal
   * @deprecated This API is ICU internal only.
   */
  @Override
  @Deprecated
  public BreakIterator getNewBreakIterator() {
    return new RuleBasedBreakIterator(this.rules);
  }

  /**
   * Builder for {@link RuleBasedSegmenter}
   * @draft ICU 78
   */
  public static class Builder {

    String rules;

    Builder() { }

    /**
     * Sets the rule string for segmentation.
     * @param rules rule string.  The rule string must follow the same guidelines as for
     *     {@link RuleBasedBreakIterator#getInstanceFromCompiledRules(InputStream)}.
     * @draft ICU 78
     */
    public Builder setRules(String rules) {
      this.rules = rules;
      return this;
    }

    /**
     * Builds the {@code Segmenter}
     * @return the constructed {@code Segmenter} instance
     * @draft ICU 78
     */
    public Segmenter build() {
      return new RuleBasedSegmenter(this.rules);
    }
  }

  static class RuleBasedSegments implements Segments {
    private CharSequence source;

    private RuleBasedSegmenter segmenter;

    private BreakIterator breakIter;

    RuleBasedSegments(CharSequence source, RuleBasedSegmenter segmenter) {
      this.source = source;
      this.segmenter = segmenter;
      this.breakIter = this.segmenter.getNewBreakIterator();

      this.breakIter.setText(source);
    }

    @Override
    public Segment segmentAt(int i) {
      return SegmentsImplUtils.segmentAt(this.breakIter, this.source, i);
    }

    @Override
    public Stream<Segment> segments() {
      return SegmentsImplUtils.segments(this.breakIter, this.source);
    }

    @Override
    public boolean isBoundary(int i) {
      return SegmentsImplUtils.isBoundary(this.breakIter, this.source, i);
    }

    @Override
    public Stream<Segment> segmentsFrom(int i) {
      return SegmentsImplUtils.segmentsFrom(this.breakIter, this.source, i);
    }

    @Override
    public Stream<Segment> segmentsBefore(int i) {
      return SegmentsImplUtils.segmentsBefore(this.breakIter, this.source, i);
    }

    @Override
    public IntStream boundaries() {
      return SegmentsImplUtils.boundaries(this.breakIter, this.source);
    }

    @Override
    public IntStream boundariesAfter(int i) {
      return SegmentsImplUtils.boundariesAfter(this.breakIter, this.source, i);
    }

    @Override
    public IntStream boundariesBackFrom(int i) {
      return SegmentsImplUtils.boundariesBackFrom(this.breakIter, this.source, i);
    }
  }
}
