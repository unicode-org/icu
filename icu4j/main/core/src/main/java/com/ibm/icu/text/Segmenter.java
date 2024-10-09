package com.ibm.icu.text;

public interface Segmenter {
  Segments segment(String s);

  @Deprecated
  BreakIterator getNewBreakIterator();
}
