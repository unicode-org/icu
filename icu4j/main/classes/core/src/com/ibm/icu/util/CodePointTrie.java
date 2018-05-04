// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.ibm.icu.impl.Normalizer2Impl.UTF16Plus;

/**
 * Immutable Unicode code point trie structure.
 * Fast, reasonably compact, map from Unicode code points (U+0000..U+10FFFF) to integer values.
 * For details see http://site.icu-project.org/design/struct/utrie
 *
 * <p>This class is not intended for public subclassing.
 *
 * @see CodePointTrieBuilder
 * @internal ICU 62 technology preview
 * @provisional This API might change or be removed in a future release.
 */
public abstract class CodePointTrie implements Iterable<CodePointTrie.Range> {
    enum Type {
        FAST,
        SMALL
    }

    enum ValueBits {
        BITS_16,
        BITS_32,
        BITS_8
    }

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

        private StringIterator(CharSequence s, int sIndex) {
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

    /** @internal */
    protected int[] ascii;

    /** @internal */
    protected int[] index;

    // type can be null for "any"
    // valueBites can be null for "any"
    public static CodePointTrie fromBinary(Type type, ValueBits valueBits, ByteBuffer bytes)
            throws IOException {
        return null;
    }

    public abstract Type getType();
    public abstract ValueBits getValueBits();

    // with range check
    public int get(int c) {
        return getFromIndex(cpIndex(c));
    }

    public final int asciiGet(int c) {
        assert 0 <= c && c <= 0x7f;
        return ascii[c];
    }

    /** @internal */
    protected abstract int getFromIndex(int index);

    /** @internal */
    protected int fastIndex(int c) {
        return 0;
    }

    /** @internal */
    protected int smallIndex(int c) {
        return 0;
    }

    /** @internal */
    protected abstract int cpIndex(int c);

    /** @internal */
    protected int errorIndex() {
        return 0;  // dataLength - 1
    }

    /** @internal */
    protected int highValueIndex() {
        return 0;  // dataLength - 2
    }

    public boolean getRange(int start, HandleValue handleValue, Range range) {
        range.start = range.end = start;
        range.value = 0;
        return true;
    }

    public boolean getRangeFixedSurr(int start, boolean allSurr, int surrValue,
            HandleValue handleValue, Range range) {
        range.start = range.end = start;
        range.value = 0;
        return true;
    }

    @Override
    public Iterator<Range> iterator() {
        return null;
    }

    public Iterator<Range> iterator(int start, HandleValue handleValue) {
        return null;
    }

    public Iterator<Range> iteratorFixedSurr(int start, boolean allSurr, int surrValue,
            HandleValue handleValue) {
        return null;
    }

    public StringIterator stringIterator(CharSequence s, int sIndex) {
        return new StringIterator(s, sIndex);
    }

    // @return number of bytes written
    public int toBinary(OutputStream os) throws IOException {
        return 0;
    }

    public static abstract class Fast extends CodePointTrie {
        public static Fast fromBinary(ValueBits valueBits, ByteBuffer bytes) throws IOException {
            return (Fast) CodePointTrie.fromBinary(Type.FAST, valueBits, bytes);
        }

        @Override
        public final Type getType() { return Type.FAST; }

        @Override
        public int get(int c) {
            return getFromIndex(cpIndex(c));
        }

        public int bmpGet(int c) {
            assert 0 <= c && c <= 0xffff;
            return c & 3;
        }

        public int suppGet(int c) {
            assert 0x10000 <= c && c <= 0x10ffff;
            return c & 3;
        }

        /** @internal */
        @Override
        protected final int cpIndex(int c) {
            return 0;
        }

        @Override
        public StringIterator stringIterator(CharSequence s, int sIndex) {
            return new FastStringIterator(s, sIndex);
        }

        private final class FastStringIterator extends StringIterator {
            private FastStringIterator(CharSequence s, int sIndex) {
                super(s, sIndex);
            }

            @Override
            public StringIterator next() {
                if (sIndex >= s.length()) {
                    throw new NoSuchElementException();
                }
                char lead = s.charAt(sIndex++);
                c = lead;
                if (!Character.isSurrogate(lead)) {
                    value = bmpGet(c);
                } else {
                    char trail;
                    if (UTF16Plus.isSurrogateLead(lead) && sIndex < s.length() &&
                            Character.isLowSurrogate(trail = s.charAt(sIndex))) {
                        ++sIndex;
                        c = Character.toCodePoint(lead, trail);
                        value = suppGet(c);
                    } else {
                        value = getFromIndex(errorIndex());
                    }
                }
                return this;
            }

            @Override
            public StringIterator previous() {
                if (sIndex <= 0) {
                    throw new NoSuchElementException();
                }
                char trail = s.charAt(--sIndex);
                c = trail;
                if (!Character.isSurrogate(trail)) {
                    value = bmpGet(c);
                } else {
                    char lead;
                    if (!UTF16Plus.isSurrogateLead(trail) && sIndex > 0 &&
                            Character.isHighSurrogate(lead = s.charAt(sIndex - 1))) {
                        --sIndex;
                        c = Character.toCodePoint(lead, trail);
                        value = suppGet(c);
                    } else {
                        value = getFromIndex(errorIndex());
                    }
                }
                return this;
            }
        }
    }

    public static abstract class Small extends CodePointTrie {
        public static Small fromBinary(ValueBits valueBits, ByteBuffer bytes) throws IOException {
            return (Small) CodePointTrie.fromBinary(Type.SMALL, valueBits, bytes);
        }

        @Override
        public final Type getType() { return Type.SMALL; }

        /** @internal */
        @Override
        protected final int cpIndex(int c) {
            return 0;
        }
    }

    public static class Fast16 extends Fast {
        public static Fast16 fromBinary(ByteBuffer bytes) throws IOException {
            return (Fast16) CodePointTrie.fromBinary(Type.FAST, ValueBits.BITS_16, bytes);
        }

        @Override
        public final ValueBits getValueBits() { return ValueBits.BITS_16; }

        @Override
        public final int get(int c) {
            return getFromIndex(cpIndex(c));
        }

        @Override
        public final int bmpGet(int c) {
            assert 0 <= c && c <= 0xffff;
            return c & 3;
        }

        @Override
        public final int suppGet(int c) {
            assert 0x10000 <= c && c <= 0x10ffff;
            return c & 3;
        }

        /** @internal */
        @Override
        protected final int getFromIndex(int index) {
            return 0;
        }
    }

    public static class Fast32 extends Fast {
        @Override
        public final ValueBits getValueBits() { return ValueBits.BITS_32; }

        /** @internal */
        @Override
        protected final int getFromIndex(int index) {
            return 0;
        }
    }

    public static class Fast8 extends Fast {
        @Override
        public final ValueBits getValueBits() { return ValueBits.BITS_8; }

        /** @internal */
        @Override
        protected final int getFromIndex(int index) {
            return 0;
        }
    }

    public static class Small16 extends Small {
        public static Small16 fromBinary(ByteBuffer bytes) throws IOException {
            return (Small16) CodePointTrie.fromBinary(Type.SMALL, ValueBits.BITS_16, bytes);
        }

        @Override
        public final ValueBits getValueBits() { return ValueBits.BITS_16; }

        @Override
        public final int get(int c) {
            return getFromIndex(cpIndex(c));
        }

        /** @internal */
        @Override
        protected final int getFromIndex(int index) {
            return 0;
        }
    }

    public static class Small32 extends Small {
        @Override
        public final ValueBits getValueBits() { return ValueBits.BITS_32; }

        /** @internal */
        @Override
        protected final int getFromIndex(int index) {
            return 0;
        }
    }

    public static class Small8 extends Small {
        @Override
        public final ValueBits getValueBits() { return ValueBits.BITS_8; }

        /** @internal */
        @Override
        protected final int getFromIndex(int index) {
            return 0;
        }
    }
}
