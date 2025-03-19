package com.ibm.icu.text.segmenter;

import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.util.ULocale;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class LocalizedSegmenter implements Segmenter {

  private ULocale locale;

  private SegmentationType segmentationType;

  @Override
  public Segments segment(CharSequence s) {
    return new LocalizedSegments(s, this);
  }

  public static Builder builder() {
    return new Builder();
  }

  LocalizedSegmenter(ULocale locale, SegmentationType segmentationType) {
    this.locale = locale;
    this.segmentationType = segmentationType;
  }

  /**
   * @internal
   * @deprecated This API is ICU internal only.
   */
  @Override
  @Deprecated
  public BreakIterator getNewBreakIterator() {
    BreakIterator breakIter;
    switch (this.segmentationType) {
      case LINE:
        breakIter = BreakIterator.getLineInstance(this.locale);
        break;
      case SENTENCE:
        breakIter = BreakIterator.getSentenceInstance(this.locale);
        break;
      case WORD:
        breakIter = BreakIterator.getWordInstance(this.locale);
        break;
      case GRAPHEME_CLUSTER:
      default:
        breakIter = BreakIterator.getCharacterInstance(this.locale);
        break;
    }
    return breakIter;
  }

  public enum SegmentationType {
    GRAPHEME_CLUSTER,
    WORD,
    LINE,
    SENTENCE,
  }

  public static class Builder {

    private ULocale locale = ULocale.ROOT;

    private SegmentationType segmentationType = SegmentationType.GRAPHEME_CLUSTER;

    Builder() { }

    public Builder setLocale(ULocale locale) {
      this.locale = locale;
      return this;
    }

    public Builder setSegmentationType(SegmentationType segmentationType) {
      this.segmentationType = segmentationType;
      return this;
    }

    public LocalizedSegmenter build() {
      return new LocalizedSegmenter(this.locale, this.segmentationType);
    }

  }

  public class LocalizedSegments implements Segments {

    private CharSequence source;

    private LocalizedSegmenter segmenter;

    private BreakIterator breakIter;

    private LocalizedSegments(CharSequence source, LocalizedSegmenter segmenter) {
      this.source = source;
      this.segmenter = segmenter;
      this.breakIter = this.segmenter.getNewBreakIterator();
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
