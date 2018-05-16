// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License

// created: 2018may04 Markus W. Scherer

package com.ibm.icu.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

import com.ibm.icu.impl.Normalizer2Impl.UTF16Plus;

/**
 * Immutable Unicode code point trie.
 * Fast, reasonably compact, map from Unicode code points (U+0000..U+10FFFF) to integer values.
 * For details see http://site.icu-project.org/design/struct/utrie
 *
 * <p>This class is not intended for public subclassing.
 *
 * @see MutableCodePointTrie
 * @draft ICU 63
 * @provisional This API might change or be removed in a future release.
 */
public abstract class CodePointTrie extends CodePointMap {
    enum Type {
        FAST,
        SMALL
    }

    enum ValueWidth {
        BITS_16,
        BITS_32,
        BITS_8
    }

    /** @internal */
    protected int[] ascii;

    /** @internal */
    protected int[] index;

    // type can be null for "any"
    // valueBits can be null for "any"
    public static CodePointTrie fromBinary(Type type, ValueWidth valueWidth, ByteBuffer bytes)
            throws IOException {
        return null;
    }

    public abstract Type getType();
    public abstract ValueWidth getValueWidth();

    // with range check
    @Override
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

    @Override
    public boolean getRange(int start, HandleValue handleValue, Range range) {
        range.start = range.end = start;
        range.value = 0;
        return true;
    }

    // @return number of bytes written
    public int toBinary(OutputStream os) throws IOException {
        return 0;
    }

    public static abstract class Fast extends CodePointTrie {
        public static Fast fromBinary(ValueWidth valueWidth, ByteBuffer bytes) throws IOException {
            return (Fast) CodePointTrie.fromBinary(Type.FAST, valueWidth, bytes);
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
        public static Small fromBinary(ValueWidth valueWidth, ByteBuffer bytes) throws IOException {
            return (Small) CodePointTrie.fromBinary(Type.SMALL, valueWidth, bytes);
        }

        @Override
        public final Type getType() { return Type.SMALL; }

        /** @internal */
        @Override
        protected final int cpIndex(int c) {
            return 0;
        }
    }

    public static final class Fast16 extends Fast {
        public static Fast16 fromBinary(ByteBuffer bytes) throws IOException {
            return (Fast16) CodePointTrie.fromBinary(Type.FAST, ValueWidth.BITS_16, bytes);
        }

        @Override
        public final ValueWidth getValueWidth() { return ValueWidth.BITS_16; }

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

    public static final class Fast32 extends Fast {
        @Override
        public final ValueWidth getValueWidth() { return ValueWidth.BITS_32; }

        /** @internal */
        @Override
        protected final int getFromIndex(int index) {
            return 0;
        }
    }

    public static final class Fast8 extends Fast {
        @Override
        public final ValueWidth getValueWidth() { return ValueWidth.BITS_8; }

        /** @internal */
        @Override
        protected final int getFromIndex(int index) {
            return 0;
        }
    }

    public static final class Small16 extends Small {
        public static Small16 fromBinary(ByteBuffer bytes) throws IOException {
            return (Small16) CodePointTrie.fromBinary(Type.SMALL, ValueWidth.BITS_16, bytes);
        }

        @Override
        public final ValueWidth getValueWidth() { return ValueWidth.BITS_16; }

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

    public static final class Small32 extends Small {
        @Override
        public final ValueWidth getValueWidth() { return ValueWidth.BITS_32; }

        /** @internal */
        @Override
        protected final int getFromIndex(int index) {
            return 0;
        }
    }

    public static final class Small8 extends Small {
        @Override
        public final ValueWidth getValueWidth() { return ValueWidth.BITS_8; }

        /** @internal */
        @Override
        protected final int getFromIndex(int index) {
            return 0;
        }
    }
}
