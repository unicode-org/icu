// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.segmenter;

import com.ibm.icu.segmenter.Segments.IterationDirection;
import com.ibm.icu.text.BreakIterator;

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
    private BreakIterator breakIter;
    private IterationDirection direction;
    private int currIdx;

    BoundaryIteratorOfInts(
            BreakIterator breakIter,
            CharSequence sourceSequence,
            IterationDirection direction,
            int startIdx) {
        this.breakIter = breakIter;
        this.direction = direction;

        if (direction == IterationDirection.FORWARDS) {
            currIdx = breakIter.following(startIdx);
        } else {
            assert direction == IterationDirection.BACKWARDS;

            // When iterating backwards over boundaries, adjust the initial index to be the boundary
            // that is either startIdx or else the one right before startIdx.
            //
            // Note: we have to set the initial index indirectly because there is no way to
            // statelessly
            // query whether an index is on a boundary. Instead, BreakIterator.isBoundary() will
            // mutate
            // state when the input is not on a boundary, before it returns the value indicating a
            // boundary.
            int sourceLength = sourceSequence.length();
            if (startIdx == 0) {
                currIdx = breakIter.first();
            } else if (startIdx == sourceLength) {
                currIdx = breakIter.last();
            } else {
                boolean isOnBoundary =
                        0 <= startIdx && startIdx <= sourceLength && breakIter.isBoundary(startIdx);

                // The previous call to BreakIterator.isBoundary(startIdx) will have advanced
                // breakIter's
                // current position forwards to the next boundary if the argument, startIdx, is not
                // a
                // boundary. Therefore, in that case, we have to move back to the previous boundary.
                //
                // BreakIterator.isBoundary(startIdx) should have cached the surrounding 2
                // boundaries in the
                // BreakIterator, which means that BreakIterator.preceding(startIdx) shouldn't cost
                // significant extra time.
                //
                // BreakIterator.preceding(startIdx) is used in initialization instead of a simple
                // call to
                // BreakIterator.previous() since BreakIterator.preceding() can accept arguments
                // larger than
                // the last boundary and return the last boundary, whereas .previous() would return
                // DONE.
                // Thus, .preceding() provides symmetrical behavior to .following(), which we use in
                // the
                // forwards direction.
                currIdx = isOnBoundary ? startIdx : breakIter.preceding(startIdx);
            }
        }
    }

    public boolean hasNext() {
        return currIdx != BreakIterator.DONE;
    }

    public Integer next() {
        int result = currIdx;

        if (direction == IterationDirection.FORWARDS) {
            currIdx = breakIter.next();
        } else {
            assert direction == IterationDirection.BACKWARDS;
            currIdx = breakIter.previous();
        }

        return result;
    }
}
