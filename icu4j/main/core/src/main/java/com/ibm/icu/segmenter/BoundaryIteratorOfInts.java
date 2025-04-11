// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.segmenter;

import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.segmenter.Segments.IterationDirection;

class BoundaryIteratorOfInts {
  BreakIterator breakIter;
  IterationDirection direction;
  int currIdx;

  BoundaryIteratorOfInts(BreakIterator breakIter, IterationDirection direction, int startIdx) {
    this.breakIter = breakIter;
    this.direction = direction;

    // TODO(ICU-22987): Remove after fixing preceding(int) to return `DONE` for negative inputs
    if (startIdx < 0 && direction == IterationDirection.BACKWARDS) {
      this.currIdx = BreakIterator.DONE;
      return;
    }

    if (direction == IterationDirection.FORWARDS) {
      this.currIdx = breakIter.following(startIdx);
    } else {
      assert direction == IterationDirection.BACKWARDS;
      this.currIdx = breakIter.preceding(startIdx);
    }
  }

  public boolean hasNext() {
    return this.currIdx != BreakIterator.DONE;
  }

  public Integer next() {
    int result = this.currIdx;

    if (direction == IterationDirection.FORWARDS) {
      this.currIdx = breakIter.next();
    } else {
      assert direction == IterationDirection.BACKWARDS;
      this.currIdx = breakIter.previous();
    }

    return result;
  }
}
