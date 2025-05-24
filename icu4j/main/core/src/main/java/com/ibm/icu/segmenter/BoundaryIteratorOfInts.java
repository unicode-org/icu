// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.segmenter;

import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.segmenter.Segments.IterationDirection;

/**
 * An iterator of segmentation boundaries that can operate in either the forwards or reverse
 * direction.
 *
 * <p>When constructed to operate in the forwards direction, the iterator will return all boundaries
 * that are strictly after the input index value provided to the constructor. However, when
 * constructed to operate in the backwards direction, if the input index is already a segmentation
 * boundary, then it will be included as the first value that the iterator returns as it iterates
 * backwards.
 */
class BoundaryIteratorOfInts {
  BreakIterator breakIter;
  IterationDirection direction;
  int currIdx;

  BoundaryIteratorOfInts(BreakIterator breakIter, CharSequence sourceSequence, IterationDirection direction, int startIdx) {
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

      // When iterating backwards over boundaries, adjust the start index forwards by 1 to
      // counteract the behavior from BreakIterator.preceding(), which we use to initialize the
      // BreakIterator state, that always moves backwards by at least 1. We want to support an
      // API that includes the input index when it is itself a boundary, unlike the behavior of
      // BreakIterator.preceding().
      int sourceLength = sourceSequence.length();
      boolean isOnBoundary = startIdx <= sourceLength && breakIter.isBoundary(startIdx);
      int backFromIdx = isOnBoundary ? startIdx + 1 : startIdx;

      this.currIdx = breakIter.preceding(backFromIdx);
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
