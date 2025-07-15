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

  private BreakIterator breakIterPrototype;

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
   * @return a builder for constructing {@code LocalizedSegmenter}
   * @draft ICU 78
   */
  public static Builder builder() {
    return new Builder();
  }

  private LocalizedSegmenter(ULocale locale, SegmentationType segmentationType) {
    switch (segmentationType) {
      case LINE:
        breakIterPrototype = BreakIterator.getLineInstance(locale);
        break;
      case SENTENCE:
        breakIterPrototype = BreakIterator.getSentenceInstance(locale);
        break;
      case WORD:
        breakIterPrototype = BreakIterator.getWordInstance(locale);
        break;
      case GRAPHEME_CLUSTER:
        breakIterPrototype = BreakIterator.getCharacterInstance(locale);
        break;
    }
  }

  /**
   * The type of segmentation to be performed. See the ICU User Guide page
   * <a
   * href="https://unicode-org.github.io/icu/userguide/boundaryanalysis/#four-types-of-breakiterator">Boundary Analysis</a>
   * for further details.
   * @draft ICU 78
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

    private SegmentationType segmentationType = null;

    private Builder() { }

    /**
     * Set the locale for which segmentation rules will be loaded
     * @param locale an ICU locale object
     * @draft ICU 78
     */
    public Builder setLocale(ULocale locale) {
      if (locale == null) {
        throw new IllegalArgumentException("locale cannot be set to null.");
      }
      this.locale = locale;
      return this;
    }

    /**
     * Set the locale for which segmentation rules will be loaded
     * @param locale a Java locale object
     * @draft ICU 78
     */
    public Builder setLocale(Locale locale) {
      if (locale == null) {
        throw new IllegalArgumentException("locale cannot be set to null.");
      }
      this.locale = ULocale.forLocale(locale);
      return this;
    }

    /**
     * Set the segmentation type to be performed.
     * @param segmentationType
     * @draft ICU 78
     */
    public Builder setSegmentationType(SegmentationType segmentationType) {
      if (segmentationType == null) {
        throw new IllegalArgumentException("segmentationType cannot be set to null.");
      }
      this.segmentationType = segmentationType;
      return this;
    }

    /**
     * Builds the {@code Segmenter}
     * @return the constructed {@code Segmenter} instance
     * @draft ICU 78
     */
    public Segmenter build() {
      if (segmentationType == null) {
        throw new IllegalArgumentException("segmentationType is null and must be set to a specific value.");
      }
      return new LocalizedSegmenter(locale, segmentationType);
    }

  }
}
