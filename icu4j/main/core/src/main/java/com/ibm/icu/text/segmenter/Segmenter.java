package com.ibm.icu.text.segmenter;

import com.ibm.icu.text.BreakIterator;

public interface Segmenter {
  Segments segment(CharSequence s);

  /**
   * @internal
   * @deprecated This API is ICU internal only.
   */
  @Deprecated
  BreakIterator getNewBreakIterator();

}
