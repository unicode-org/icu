package com.ibm.icu.text.segmenter;

import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.RuleBasedBreakIterator;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class RuleBasedSegmenter implements Segmenter {

  private String rules;

  @Override
  public Segments segment(CharSequence s) {
    return new RuleBasedSegments(s, this);
  }

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

  public static class Builder {

    String rules;

    Builder() { }

    public Builder setRules(String rules) {
      this.rules = rules;
      return this;
    }

    public RuleBasedSegmenter build() {
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
    public Stream<CharSequence> subSequences() {
      return SegmentsImplUtils.subSequences(this.breakIter, this.source);
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
    public Function<Segment, CharSequence> segmentToSequenceFn() {
      return SegmentsImplUtils.segmentToSequenceFn(this.source);
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
