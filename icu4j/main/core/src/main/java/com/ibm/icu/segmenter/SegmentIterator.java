// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.segmenter;

import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.segmenter.Segments.IterationDirection;
import java.util.Iterator;

class SegmentIterator implements Iterator<Segment> {
  BreakIterator breakIter;
  final IterationDirection direction;
  int start;
  int limit;
  final CharSequence source;

  SegmentIterator(BreakIterator breakIter, IterationDirection direction, int startIdx, CharSequence source) {
    this.breakIter = breakIter;
    this.direction = direction;
    this.source = source;

    Segment segmentAtIdx = SegmentsImplUtils.segmentAt(breakIter, source, startIdx);

    if (segmentAtIdx == null) {
      this.start = BreakIterator.DONE;
    } else if (direction == IterationDirection.FORWARDS) {
      this.start = segmentAtIdx.start;
      this.limit = breakIter.following(this.start);
    } else {
      assert direction == IterationDirection.BACKWARDS;
      if (breakIter.isBoundary(startIdx)) {
        // Note: breakIter::isBoundary is a stateful operation. It resets the position in the
        // BreakIterator, which we want to ensure that the position is where we think it is.
        this.start = startIdx;
      } else {
        // Since we already called BreakIterator.isBoundary() which mutates the BreakIterator
        // position to increment forwards when the return value is false, we should call
        // BreakIterator.previous() to update the iterator position while getting the start value
        // of the segment at startIdx
        this.start = breakIter.previous();
      }
      this.limit = getDirectionBasedNextIdx();
    }
  }

  int getDirectionBasedNextIdx() {
    if (direction == IterationDirection.FORWARDS) {
      return breakIter.next();
    } else {
      assert direction == IterationDirection.BACKWARDS;
      return breakIter.previous();
    }
  }

  @Override
  public boolean hasNext() {
    return this.limit != BreakIterator.DONE;
  }

  @Override
  public Segment next() {
    Segment result;
    if (this.limit < this.start) {
      result = new Segment(this.limit, this.start, this.source);
    } else {
      result = new Segment(this.start, this.limit, this.source);
    }

    this.start = this.limit;
    this.limit = getDirectionBasedNextIdx();

    return result;
  }
}
