// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.segmenter;

import com.ibm.icu.segmenter.Segments.IterationDirection;
import com.ibm.icu.text.BreakIterator;
import java.util.Iterator;

class SegmentIterator implements Iterator<Segment> {
    private BreakIterator breakIter;
    private final IterationDirection direction;
    private int start;
    private int limit;
    private final CharSequence source;

    SegmentIterator(
            BreakIterator breakIter,
            IterationDirection direction,
            int startIdx,
            CharSequence source) {
        this.breakIter = breakIter;
        this.direction = direction;
        this.source = source;

        // Note: BreakIterator.isBoundary() is a stateful operation. It resets the position in the
        // BreakIterator, and thus doesn't just return whether the input is on a boundary.
        boolean startIdxIsBoundary = breakIter.isBoundary(startIdx);

        if (direction == IterationDirection.FORWARDS) {
            if (startIdxIsBoundary) {
                start = startIdx;
                limit = breakIter.next();
            } else {
                // if startIdx wasn't on a boundary, then the call to isBoundary will have advanced
                // it to
                // the next boundary, which is the limit of the segment
                limit = breakIter.current();
                // go back to get the start of the segment
                start = breakIter.previous();
                // reset current position of BreakIterator to be limit of segment
                breakIter.isBoundary(limit);
            }
        } else {
            assert direction == IterationDirection.BACKWARDS;
            if (startIdxIsBoundary) {
                limit = breakIter.current();
            } else {
                // if startIdx was not on boundary, then the breakIter state moved forward past
                // startIdx
                // after the call to BreakIterator.current(), so we need to move to the previous
                // boundary
                // before startIdx to start the iteration
                limit = breakIter.previous();
            }
            start = breakIter.previous();
        }
    }

    @Override
    public boolean hasNext() {
        if (direction == IterationDirection.FORWARDS) {
            return limit != BreakIterator.DONE;
        } else {
            return start != BreakIterator.DONE;
        }
    }

    @Override
    public Segment next() {
        Segment result = new Segment(start, limit, source);

        if (direction == IterationDirection.FORWARDS) {
            start = limit;
            limit = breakIter.next();
        } else {
            assert direction == IterationDirection.BACKWARDS;
            limit = start;
            start = breakIter.previous();
        }

        return result;
    }
}
