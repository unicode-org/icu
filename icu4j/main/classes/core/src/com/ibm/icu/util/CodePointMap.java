// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License

// created: 2018may10 Markus W. Scherer

package com.ibm.icu.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Abstract map from Unicode code points (U+0000..U+10FFFF) to integer values.
 * This does not implement java.util.Map.
 *
 * @draft ICU 63
 * @provisional This API might change or be removed in a future release.
 */
public abstract class CodePointMap implements Iterable<CodePointMap.Range> {
    /**
     * Selectors for how getRange() should report value ranges overlapping with surrogates.
     * Most users should use NORMAL.
     *
     * @see #getRange
     * @draft ICU 63
     * @provisional This API might change or be removed in a future release.
     */
    public enum RangeOption {
        /**
         * getRange() enumerates all same-value ranges as stored in the trie.
         * Most users should use this option.
         *
         * @draft ICU 63
         * @provisional This API might change or be removed in a future release.
         */
        NORMAL,
        /**
         * getRange() enumerates all same-value ranges as stored in the trie,
         * except that lead surrogates (U+D800..U+DBFF) are treated as having the
         * surrogateValue, which is passed to getRange() as a separate parameter.
         * The surrogateValue is not transformed via filter().
         * See {@link Character#isHighSurrogate}.
         *
         * <p>Most users should use NORMAL instead.
         *
         * <p>This option is useful for tries that map surrogate code *units* to
         * special values optimized for UTF-16 string processing
         * or for special error behavior for unpaired surrogates,
         * but those values are not to be associated with the lead surrogate code *points*.
         *
         * @draft ICU 63
         * @provisional This API might change or be removed in a future release.
         */
        FIXED_LEAD_SURROGATES,
        /**
         * getRange() enumerates all same-value ranges as stored in the trie,
         * except that all surrogates (U+D800..U+DFFF) are treated as having the
         * surrogateValue, which is passed to getRange() as a separate parameter.
         * The surrogateValue is not transformed via filter().
         * See {@link Character#isSurrogate}.
         *
         * <p>Most users should use NORMAL instead.
         *
         * <p>This option is useful for tries that map surrogate code *units* to
         * special values optimized for UTF-16 string processing
         * or for special error behavior for unpaired surrogates,
         * but those values are not to be associated with the lead surrogate code *points*.
         *
         * @draft ICU 63
         * @provisional This API might change or be removed in a future release.
         */
        FIXED_ALL_SURROGATES
    }

    // For getRange() & Iterator.
    public interface FilterValue {
        public int apply(int value);
    }

    // For getRange() & Iterator.
    public static final class Range {
        private int start;
        private int end;
        private int value;

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
     * This does not implement java.util.Iterator.
     *
     * <pre>
     * void onString(CodePointMap map, CharSequence s, int start) {
     *     CodePointMap.StringIterator iter = map.stringIterator(s, start);
     *     while (iter.next()) {
     *         int end = iter.getIndex();  // code point from between start and end
     *         useValue(s, start, end, iter.getCodePoint(), iter.getValue());
     *         start = end;
     *     }
     * }
     * </pre>
     *
     * <p>This class is not intended for public subclassing.
     *
     * @draft ICU 63
     * @provisional This API might change or be removed in a future release.
     */
    public class StringIterator {
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

        public boolean next() {
            if (sIndex >= s.length()) {
                return false;
            }
            c = Character.codePointAt(s, sIndex);
            sIndex += Character.charCount(c);
            value = get(c);
            return true;
        }

        public boolean previous() {
            if (sIndex <= 0) {
                return false;
            }
            c = Character.codePointBefore(s, sIndex);
            sIndex -= Character.charCount(c);
            value = get(c);
            return true;
        }
        public final int getIndex() { return sIndex; }
        public final int getCodePoint() { return c; }
        public final int getValue() { return value; }
    }

    public abstract int get(int c);

    public abstract boolean getRange(int start, FilterValue filter, Range range);

    public boolean getRange(int start, RangeOption option, int surrogateValue,
            FilterValue filter, Range range) {
        assert option != null;
        if (!getRange(start, filter, range)) {
            return false;
        }
        if (option == RangeOption.NORMAL) {
            return true;
        }
        int surrEnd = option == RangeOption.FIXED_ALL_SURROGATES ? 0xdfff : 0xdbff;
        int end = range.end;
        if (end < 0xd7ff || start > surrEnd) {
            return true;
        }
        // The range overlaps with surrogates, or ends just before the first one.
        if (range.value == surrogateValue) {
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
            range.value = surrogateValue;
            if (end > surrEnd) {
                range.end = surrEnd;  // Surrogate range ends before non-surrValue rest of range.
                return true;
            }
        }
        // See if the surrValue surrogate range can be merged with
        // an immediately following range.
        if (getRange(surrEnd + 1, filter, range) && range.value == surrogateValue) {
            range.start = start;
            return true;
        }
        range.start = start;
        range.end = surrEnd;
        range.value = surrogateValue;
        return true;
    }

    /**
     * Convenience iterator over same-trie-value code point ranges.
     * Same as looping over all ranges with getRange()
     * (simple overload or using {@value RangeOption#NORMAL}) without filtering.
     * Adjacent ranges have different trie values.
     *
     * <p>The iterator always returns the same Range object.
     *
     * @return a Range iterator
     * @draft ICU 63
     * @provisional This API might change or be removed in a future release.
     */
    @Override
    public Iterator<Range> iterator() {
        return new RangeIterator();
    }

    public StringIterator stringIterator(CharSequence s, int sIndex) {
        return new StringIterator(s, sIndex);
    }
}
