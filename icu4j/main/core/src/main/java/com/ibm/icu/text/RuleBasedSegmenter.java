package com.ibm.icu.text;

import java.util.stream.Stream;

public class RuleBasedSegmenter implements Segmenter {

  private String rules;


  @Override
  public Segments segment(String s) {
    return new RuleBasedSegments(s, this);
  }

  public String getRules() {
    return this.rules;
  }

  public static Builder builder() {
    return new Builder();
  }

  RuleBasedSegmenter(String rules) {
    this.rules = rules;
  }

  @Override
  public RuleBasedBreakIterator getNewBreakIterator() {
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

  public static class RuleBasedSegments implements Segments {
    private String source;

    private RuleBasedSegmenter segmenter;

    private BreakIterator breakIter;

    RuleBasedSegments(String source, RuleBasedSegmenter segmenter) {
      this.source = source;
      this.segmenter = segmenter;
      this.breakIter = this.segmenter.getNewBreakIterator();
    }

    @Override
    public String getSourceString() {
      return this.source;
    }

    @Override
    public Segmenter getSegmenter() {
      return segmenter;
    }

    @Override
    public BreakIterator getInstanceBreakIterator() {
      return this.breakIter;
    }
  }
}
