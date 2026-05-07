// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.segmenter;

import com.ibm.icu.segmenter.Segments.IterationDirection;
import com.ibm.icu.text.BreakIterator;
import java.util.Iterator;

/**
 * This {@code Iterable} exists to enable the creation of a {@code Spliterator} that in turn enables
 * the creation of a lazy {@code Stream}.
 */
class SegmentIterable implements Iterable<Segment> {
    private BreakIterator breakIter;
    private final IterationDirection direction;
    private int startIdx;
    private final CharSequence source;

    SegmentIterable(
            BreakIterator breakIter,
            IterationDirection direction,
            int startIdx,
            CharSequence source) {
        this.breakIter = breakIter;
        this.direction = direction;
        this.startIdx = startIdx;
        this.source = source;
    }

    @Override
    public Iterator<Segment> iterator() {
        return new SegmentIterator(breakIter, direction, startIdx, source);
    }
}
