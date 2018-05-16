// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License

// created: 2018may10 Markus W. Scherer

package com.ibm.icu.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Abstract map from Unicode code points (U+0000..U+10FFFF) to integer values.
 *
 * @draft ICU 63
 * @provisional This API might change or be removed in a future release.
 */
public abstract class CodePointMap implements Iterable<CodePointMap.Range> {
    // For getRange() & Iterator.
    public interface HandleValue {
        public int apply(int value);
    }

    // For getRange() & Iterator.
    public static final class Range {
        int start;
        int end;
        int value;

        public Range() {
            start = end = -1;
            value = 0;
        }

        public int getStart() { return start; }
        public int getEnd() { return end; }
        public int getValue() { return value; }
        public void set(int start, int end, int value) {
            this.start = start;
            this.end = end;
            this.value = value;
        }
    }

    private final class RangeIterator implements Iterator<Range> {
        private Range range = new Range();

        @Override
        public boolean hasNext() {
            return -1 <= range.end && range.end < 0x10ffff;
        }

        @Override
        public Range next() {
            if (getRange(range.end + 1, null, range)) {
                return range;
            } else {
                throw new NoSuchElementException();
            }
        }

        @Override
        public final void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Iterates over code points of a string and fetches trie values.
     *
     * <p>This class is not intended for public subclassing.
     */
    public class StringIterator implements Iterator<StringIterator> {
        /** @internal */
        protected CharSequence s;
        /** @internal */
        protected int sIndex;
        /** @internal */
        protected int c;
        /** @internal */
        protected int value;

        /** @internal */
        protected StringIterator(CharSequence s, int sIndex) {
            this.s = s;
            this.sIndex = sIndex;
            c = -1;
            value = 0;
        }

        public void reset(CharSequence s, int sIndex) {
            this.s = s;
            this.sIndex = sIndex;
            c = -1;
            value = 0;
        }

        @Override
        public final boolean hasNext() {
            return sIndex < s.length();
        }

        public final boolean hasPrevious() {
            return sIndex > 0;
        }

        @Override
        public StringIterator next() {
            if (sIndex >= s.length()) {
                throw new NoSuchElementException();
            }
            c = Character.codePointAt(s, sIndex);
            sIndex += Character.charCount(c);
            value = get(c);
            return this;
        }

        public StringIterator previous() {
            if (sIndex <= 0) {
                throw new NoSuchElementException();
            }
            c = Character.codePointBefore(s, sIndex);
            sIndex -= Character.charCount(c);
            value = get(c);
            return this;
        }
        public final int getIndex() { return sIndex; }
        public final int getCodePoint() { return c; }
        public final int getValue() { return value; }

        /**
         * Not implemented.
         *
         * @throws UnsupportedOperationException because there is nothing to remove.
         */
        @Override
        public final void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public abstract int get(int c);

    public abstract boolean getRange(int start, HandleValue handleValue, Range range);

    public boolean getRangeFixedSurr(int start, boolean allSurr, int surrValue,
            HandleValue handleValue, Range range) {
        if (!getRange(start, handleValue, range)) {
            return false;
        }
        int surrEnd = allSurr ? 0xdfff : 0xdbff;
        int end = range.end;
        if (end < 0xd7ff || start > surrEnd) {
            return true;
        }
        // The range overlaps with surrogates, or ends just before the first one.
        if (range.value == surrValue) {
            if (end >= surrEnd) {
                // Surrogates followed by a non-surrValue range,
                // or surrogates are part of a larger surrValue range.
                return true;
            }
        } else {
            if (start <= 0xd7ff) {
                range.end = 0xd7ff;  // Non-surrValue range ends before surrValue surrogates.
                return true;
            }
            // Start is a surrogate with a non-surrValue code *unit* value.
            // Return a surrValue code *point* range.
            range.value = surrValue;
            if (end > surrEnd) {
                range.end = surrEnd;  // Inert surrogate range ends before non-surrValue rest of range.
                return true;
            }
        }
        // See if the surrValue surrogate range can be merged with
        // an immediately following range.
        if (getRange(surrEnd + 1, handleValue, range) && range.value == surrValue) {
            range.start = start;
            return true;
        }
        range.start = start;
        range.end = surrEnd;
        range.value = surrValue;
        return true;
    }

    @Override
    public Iterator<Range> iterator() {
        return new RangeIterator();
    }

    public StringIterator stringIterator(CharSequence s, int sIndex) {
        return new StringIterator(s, sIndex);
    }
}
