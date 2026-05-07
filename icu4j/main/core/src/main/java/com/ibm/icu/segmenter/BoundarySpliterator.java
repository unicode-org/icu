// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.segmenter;

import com.ibm.icu.segmenter.Segments.IterationDirection;
import com.ibm.icu.text.BreakIterator;
import java.util.Spliterator;
import java.util.function.IntConsumer;

class BoundarySpliterator implements Spliterator.OfInt {

    private final BoundaryIteratorOfInts iter;

    BoundarySpliterator(
            BreakIterator breakIter,
            CharSequence sourceSequence,
            IterationDirection direction,
            int startIdx) {
        iter = new BoundaryIteratorOfInts(breakIter, sourceSequence, direction, startIdx);
    }

    @Override
    public OfInt trySplit() {
        // The elements of the Stream represent an iteration through a string, and is thus
        // inherently
        // stateful. Therefore, splitting this Stream does not make sense. Ex: splitting the Stream
        // is tantamount to discarding the segment subtended by the end value (index into the input
        // string) of one substream and the beginning value of the next substream.
        return null;
    }

    @Override
    public long estimateSize() {
        // The number of segments per input size depends on language, script, and
        // the content of the input string, and thus is hard to estimate without
        // sacrificing performance. Thus, returning `Long.MAX_VALUE`, according
        // to the API, to mean "unknown, or too expensive to compute".
        return Long.MAX_VALUE;
    }

    @Override
    public int characteristics() {
        return
        // BreakIterator always advances
        Spliterator.DISTINCT
                // The design of the Segmenter API is to provide an immutable view of
                // segmentation by preventing the input string from mutating
                // in the underlying BreakIterator.
                | Spliterator.IMMUTABLE
                // primitive int is non-null
                | Spliterator.NONNULL
                // BreakIterator always advances, and in a single direction.
                | Spliterator.ORDERED;
    }

    @Override
    public boolean tryAdvance(IntConsumer action) {
        if (action == null) {
            throw new NullPointerException();
        }
        if (iter.hasNext()) {
            action.accept(iter.next());
            return true;
        } else {
            return false;
        }
    }
}
