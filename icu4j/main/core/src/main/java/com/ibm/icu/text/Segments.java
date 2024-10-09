package com.ibm.icu.text;

import java.util.stream.Stream;

public interface Segments {

  String getSourceString();

  @Deprecated
  Segmenter getSegmenter();

  @Deprecated
  BreakIterator getInstanceBreakIterator();

  default Stream<CharSequence> subSequences() {
    return ranges().map((range) -> getSourceString().subSequence(range.getStart(), range.getLimit()));
  }

  default Stream<SegmentRange> ranges() {
    BreakIterator breakIter = getInstanceBreakIterator();
    breakIter.setText(getSourceString());

    int start = breakIter.first();
    int limit = breakIter.next();
    if (limit == BreakIterator.DONE) {
      return Stream.empty();
    } else {
      Stream.Builder<SegmentRange> streamBuilder = Stream.builder();
      while (limit != BreakIterator.DONE) {
        SegmentRange range = new SegmentRange(start, limit);
        streamBuilder.add(range);
        start = limit;
        limit = breakIter.next();
      }
      return streamBuilder.build();
    }
  };

  class SegmentRange {
    int start;
    int limit;

    public SegmentRange(int start, int limit) {
      this.start = start;
      this.limit = limit;
    }

    public int getStart() {
      return start;
    }

    public int getLimit(){
      return limit;
    }
  }

}
