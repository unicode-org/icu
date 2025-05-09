// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.segmenter;

import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.util.ULocale;
import java.util.Locale;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Performs segmentation according to the rules defined for the locale.
 */
public class LocalizedSegmenter implements Segmenter {

  private ULocale locale;

  private SegmentationType segmentationType;

  @Override
  public Segments segment(CharSequence s) {
    return new LocalizedSegments(s, this);
  }

  /**
   * @return a builder for constructing {@code LocalizedSegmenter}
   * @draft ICU 78
   */
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

  /**
   * The type of segmentation to be performed. See the ICU User Guide page
   * <a
   * href="https://unicode-org.github.io/icu/userguide/boundaryanalysis/#four-types-of-breakiterator">Boundary Analysis</a>
   * for further details.
   */
  public enum SegmentationType {
    GRAPHEME_CLUSTER,
    WORD,
    LINE,
    SENTENCE,
  }

  /**
   * Builder for {@link LocalizedSegmenter}
   * @draft ICU 78
   */
  public static class Builder {

    private ULocale locale = ULocale.ROOT;

    private SegmentationType segmentationType = SegmentationType.GRAPHEME_CLUSTER;

    Builder() { }

    /**
     * Set the locale for which segmentation rules will be loaded
     * @param locale an ICU locale object
     * @draft ICU 78
     */
    public Builder setLocale(ULocale locale) {
      this.locale = locale;
      return this;
    }

    /**
     * Set the locale for which segmentation rules will be loaded
     * @param locale a Java locale object
     * @draft ICU 78
     */
    public Builder setLocale(Locale locale) {
      this.locale = ULocale.forLocale(locale);
      return this;
    }

    /**
     * Set the segmentation type to be performed.
     * @param segmentationType
     * @draft ICU 78
     */
    public Builder setSegmentationType(SegmentationType segmentationType) {
      this.segmentationType = segmentationType;
      return this;
    }

    /**
     * Builds the {@code Segmenter}
     * @return the constructed {@code Segmenter} instance
     * @draft ICU 78
     */
    public Segmenter build() {
      return new LocalizedSegmenter(this.locale, this.segmentationType);
    }

  }

  class LocalizedSegments implements Segments {

    private CharSequence source;

    private LocalizedSegmenter segmenter;

    private BreakIterator breakIter;

    private LocalizedSegments(CharSequence source, LocalizedSegmenter segmenter) {
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
