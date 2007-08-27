/**
 *******************************************************************************
 * Copyright (C) 2006-2007, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 *******************************************************************************
 */

package com.ibm.icu.charset;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UTF16;

/**
 * @author Niti Hantaweepant
 */
class CharsetUTF8 extends CharsetICU {

    private static final byte[] fromUSubstitution = new byte[] { (byte) 0xef, (byte) 0xbf, (byte) 0xbd };

    public CharsetUTF8(String icuCanonicalName, String javaCanonicalName, String[] aliases) {
        super(icuCanonicalName, javaCanonicalName, aliases);
        /* max 3 bytes per code unit from UTF-8 (4 bytes from surrogate _pair_) */
        maxBytesPerChar = 3;
        minBytesPerChar = 1;
        maxCharsPerByte = 1;
    }

    private static final int BITMASK_FROM_UTF8[] = { -1, 0x7f, 0x1f, 0xf, 0x7, 0x3, 0x1 };

    private static final byte BYTES_FROM_UTF8[] = {
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
        3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 0, 0
    };

    /*
     * Starting with Unicode 3.0.1: UTF-8 byte sequences of length N _must_ encode code points of or
     * above utf8_minChar32[N]; byte sequences with more than 4 bytes are illegal in UTF-8, which is
     * tested with impossible values for them
     */
    private static final int UTF8_MIN_CHAR32[] = { 0, 0, 0x80, 0x800, 0x10000,
            Integer.MAX_VALUE, Integer.MAX_VALUE };

    private final boolean isCESU8 = this instanceof CharsetCESU8;

    class CharsetDecoderUTF8 extends CharsetDecoderICU {

        public CharsetDecoderUTF8(CharsetICU cs) {
            super(cs);
        }

        protected CoderResult decodeLoop(ByteBuffer source, CharBuffer target, IntBuffer offsets,
                boolean flush) {
            if (!source.hasRemaining()) {
                /* no input, nothing to do */
                return CoderResult.UNDERFLOW;
            }
            if (!target.hasRemaining()) {
                /* no output available, can't do anything */
                return CoderResult.OVERFLOW;
            }

            if (source.hasArray() && target.hasArray()) {
                /* source and target are backed by arrays, so use the arrays for optimal performance */
                byte[] sourceArray = source.array();
                int sourceIndex = source.arrayOffset() + source.position();
                int sourceLimit = source.arrayOffset() + source.limit();
                char[] targetArray = target.array();
                int targetIndex = target.arrayOffset() + target.position();
                int targetLimit = target.arrayOffset() + target.limit();

                byte ch;
                int char32, bytes, i;
                CoderResult cr;

                if (mode == 0) {
                    /* nothing is stored in toUnicodeStatus, read a byte as input */
                    char32 = sourceArray[sourceIndex++] & 0xff;
                    bytes = BYTES_FROM_UTF8[char32];
                    char32 &= BITMASK_FROM_UTF8[bytes];
                    i = 1;
                } else {
                    /* a partially or fully built code point is stored in toUnicodeStatus */
                    char32 = toUnicodeStatus;
                    bytes = mode;
                    i = toULength;

                    toUnicodeStatus = 0;
                    mode = 0;
                    toULength = 0;
                }

                outer: while (true) {
                    if (i < bytes) {
                        /* read a trail byte and insert its relevant bits into char32 */
                        if (sourceIndex >= sourceLimit) {
                            /* no source left, save the state for later and break out of the loop */
                            toUnicodeStatus = char32;
                            mode = bytes;
                            toULength = i;
                            cr = (flush)
                                    ? CoderResult.malformedForLength(i)
                                    : CoderResult.UNDERFLOW;
                            break;
                        }
                        if (((ch = sourceArray[sourceIndex++]) & 0xc0) != 0x80) {
                            /* not a trail byte (is not of the form 10xxxxxx) */
                            sourceIndex--;
                            cr = CoderResult.malformedForLength(toULength = i);
                            break;
                        }
                        char32 = (char32 << 6) | (ch & 0x3f);
                        i++;
                    } else if (i == bytes && UTF8_MIN_CHAR32[bytes] <= char32 && char32 <= 0x10ffff
                            && (isCESU8 ? bytes <= 3 : !UTF16.isSurrogate((char) char32))) {
                        /*
                         * char32 is a valid code point and is composed of the correct number of
                         * bytes ... we now need to output it in UTF-16
                         */

                        if (char32 <= UConverterConstants.MAXIMUM_UCS2) {
                            /* fits in 16 bits */
                            targetArray[targetIndex++] = (char) char32;
                        } else {
                            /* fit char32 into 20 bits */
                            char32 -= UConverterConstants.HALF_BASE;

                            /* write out the surrogates */
                            targetArray[targetIndex++] = (char) ((char32 >>> UConverterConstants.HALF_SHIFT) + UConverterConstants.SURROGATE_HIGH_START);

                            if (targetIndex >= targetLimit) {
                                /* put in overflow buffer (not handled here) */
                                charErrorBufferArray[charErrorBufferBegin++] = (char) char32;
                                cr = CoderResult.OVERFLOW;
                                break;
                            }
                            targetArray[targetIndex++] = (char) ((char32 & UConverterConstants.HALF_MASK) + UConverterConstants.SURROGATE_LOW_START);
                        }

                        /*
                         * we're finished outputing, so now we need to read in the first byte of the
                         * next byte sequence that could form a code point
                         */

                        if (sourceIndex >= sourceLimit) {
                            cr = CoderResult.UNDERFLOW;
                            break;
                        }
                        if (targetIndex >= targetLimit) {
                            cr = CoderResult.OVERFLOW;
                            break;
                        }

                        /* keep reading the next input (and writing it) while bytes == 1 */
                        while ((bytes = BYTES_FROM_UTF8[char32 = sourceArray[sourceIndex++] & 0xff]) == 1) {
                            targetArray[targetIndex++] = (char) char32;
                            if (sourceIndex >= sourceLimit) {
                                cr = CoderResult.UNDERFLOW;
                                break outer;
                            }
                            if (targetIndex >= targetLimit) {
                                cr = CoderResult.OVERFLOW;
                                break outer;
                            }
                        }

                        /* remove the bits that indicate the number of bytes */
                        char32 &= BITMASK_FROM_UTF8[bytes];
                        i = 1;
                    } else {
                        /*
                         * either the lead byte in the code sequence is invalid (bytes == 0) or the
                         * lead byte combined with all the trail chars does not form a valid code
                         * point
                         */
                        cr = CoderResult.malformedForLength(toULength = i);
                        break;
                    }
                }

                source.position(sourceIndex - source.arrayOffset());
                target.position(targetIndex - target.arrayOffset());
                return cr;

            } else {

                int sourceIndex = source.position();
                int sourceLimit = source.limit();
                int targetIndex = target.position();
                int targetLimit = target.limit();

                byte ch;
                int char32, bytes, i;
                CoderResult cr;

                if (mode == 0) {
                    /* nothing is stored in toUnicodeStatus, read a byte as input */
                    char32 = source.get(sourceIndex++) & 0xff;
                    bytes = BYTES_FROM_UTF8[char32];
                    char32 &= BITMASK_FROM_UTF8[bytes];
                    i = 1;
                } else {
                    /* a partially or fully built code point is stored in toUnicodeStatus */
                    char32 = toUnicodeStatus;
                    bytes = mode;
                    i = toULength;

                    toUnicodeStatus = 0;
                    mode = 0;
                    toULength = 0;
                }

                outer: while (true) {
                    if (i < bytes) {
                        /* read a trail byte and insert its relevant bits into char32 */
                        if (sourceIndex >= sourceLimit) {
                            /* no source left, save the state for later and break out of the loop */
                            toUnicodeStatus = char32;
                            mode = bytes;
                            toULength = i;
                            cr = (flush)
                                    ? CoderResult.malformedForLength(i)
                                    : CoderResult.UNDERFLOW;
                            break;
                        }
                        if (((ch = source.get(sourceIndex++)) & 0xc0) != 0x80) {
                            /* not a trail byte (is not of the form 10xxxxxx) */
                            sourceIndex--;
                            cr = CoderResult.malformedForLength(toULength = i);
                            break;
                        }
                        char32 = (char32 << 6) | (ch & 0x3f);
                        i++;
                    }
                    /*
                     * Legal UTF-8 byte sequences in Unicode 3.0.1 and up:
                     * - use only trail bytes after a lead byte (checked above)
                     * - use the right number of trail bytes for a given lead byte
                     * - encode a code point <= U+10ffff
                     * - use the fewest possible number of bytes for their code points
                     * - use at most 4 bytes (for i>=5 it is 0x10ffff<utf8_minChar32[])
                     *
                     * Starting with Unicode 3.2, surrogate code points must not be encoded in UTF-8.
                     * There are no irregular sequences any more.
                     * In CESU-8, only surrogates, not supplementary code points, are encoded directly.
                     */
                    else if (i == bytes && UTF8_MIN_CHAR32[bytes] <= char32 && char32 <= 0x10ffff
                            && (isCESU8 ? bytes <= 3 : !UTF16.isSurrogate((char) char32))) {
                        /*
                         * char32 is a valid code point and is composed of the correct number of
                         * bytes ... we now need to output it in UTF-16
                         */

                        if (char32 <= UConverterConstants.MAXIMUM_UCS2) {
                            /* fits in 16 bits */
                            target.put(targetIndex++, (char) char32);
                        } else {
                            /* fit char32 into 20 bits */
                            char32 -= UConverterConstants.HALF_BASE;

                            /* write out the surrogates */
                            target.put(
                                    targetIndex++,
                                    (char) ((char32 >>> UConverterConstants.HALF_SHIFT) + UConverterConstants.SURROGATE_HIGH_START));

                            if (targetIndex >= targetLimit) {
                                /* put in overflow buffer (not handled here) */
                                charErrorBufferArray[charErrorBufferBegin++] = (char) char32;
                                cr = CoderResult.OVERFLOW;
                                break;
                            }
                            target.put(
                                    targetIndex++,
                                    (char) ((char32 & UConverterConstants.HALF_MASK) + UConverterConstants.SURROGATE_LOW_START));
                        }

                        /*
                         * we're finished outputing, so now we need to read in the first byte of the
                         * next byte sequence that could form a code point
                         */

                        if (sourceIndex >= sourceLimit) {
                            cr = CoderResult.UNDERFLOW;
                            break;
                        }
                        if (targetIndex >= targetLimit) {
                            cr = CoderResult.OVERFLOW;
                            break;
                        }

                        /* keep reading the next input (and writing it) while bytes == 1 */
                        while ((bytes = BYTES_FROM_UTF8[char32 = source.get(sourceIndex++) & 0xff]) == 1) {
                            target.put(targetIndex++, (char) char32);
                            if (sourceIndex >= sourceLimit) {
                                cr = CoderResult.UNDERFLOW;
                                break outer;
                            }
                            if (targetIndex >= targetLimit) {
                                cr = CoderResult.OVERFLOW;
                                break outer;
                            }
                        }

                        /* remove the bits that indicate the number of bytes */
                        char32 &= BITMASK_FROM_UTF8[bytes];
                        i = 1;
                    } else {
                        /*
                         * either the lead byte in the code sequence is invalid (bytes == 0) or the
                         * lead byte combined with all the trail chars does not form a valid code
                         * point
                         */
                        cr = CoderResult.malformedForLength(toULength = i);
                        break;
                    }
                }

                source.position(sourceIndex);
                target.position(targetIndex);
                return cr;
            }
        }

    }

    class CharsetEncoderUTF8 extends CharsetEncoderICU {

        public CharsetEncoderUTF8(CharsetICU cs) {
            super(cs, fromUSubstitution);
            implReset();
        }

        protected void implReset() {
            super.implReset();
        }

        protected CoderResult encodeLoop(CharBuffer source, ByteBuffer target, IntBuffer offsets,
                boolean flush) {
            if (!source.hasRemaining()) {
                /* no input, nothing to do */
                fromUChar32 = 0;
                return CoderResult.UNDERFLOW;
            }
            if (!target.hasRemaining()) {
                /* no output available, can't do anything */
                return CoderResult.OVERFLOW;
            }

            if (source.hasArray() && target.hasArray()) {
                /* source and target are backed by arrays, so use the arrays for optimal performance */
                char[] sourceArray = source.array();
                int sourceIndex = source.arrayOffset() + source.position();
                int sourceLimit = source.arrayOffset() + source.limit();
                byte[] targetArray = target.array();
                int targetIndex = target.arrayOffset() + target.position();
                int targetLimit = target.arrayOffset() + target.limit();

                int char32;
                CoderResult cr;

                /* take care of the special condition of fromUChar32 not being 0 (it is a surrogate) */
                if (fromUChar32 != 0) {
                    char32 = fromUChar32;
                    fromUChar32 = 0;

                    /* 4 bytes to encode from char32 and a following char in source */

                    this.sourceIndex = sourceIndex;
                    this.targetIndex = targetIndex;
                    cr = encodeFourBytes(sourceArray, targetArray, sourceLimit, targetLimit,
                            char32, flush);
                    sourceIndex = this.sourceIndex;
                    targetIndex = this.targetIndex;
                    if (cr != null) {
                        source.position(sourceIndex - source.arrayOffset());
                        target.position(targetIndex - target.arrayOffset());
                        return cr;
                    }
                }

                while (true) {
                    if (sourceIndex >= sourceLimit) {
                        /* nothing left to read */
                        cr = CoderResult.UNDERFLOW;
                        break;
                    }
                    if (targetIndex >= targetLimit) {
                        /* no space left to write */
                        cr = CoderResult.OVERFLOW;
                        break;
                    }

                    /* reach the next char into char32 */
                    char32 = sourceArray[sourceIndex++] & 0xffff;

                    if (char32 <= 0x7f) {
                        /* 1 byte to encode from char32 */

                        targetArray[targetIndex++] = encodeHeadOf1(char32);

                    } else if (char32 <= 0x7ff) {
                        /* 2 bytes to encode from char32 */

                        targetArray[targetIndex++] = encodeHeadOf2(char32);

                        if (targetIndex >= targetLimit) {
                            errorBuffer[errorBufferLength++] = encodeLastTail(char32);
                            cr = CoderResult.OVERFLOW;
                            break;
                        }
                        targetArray[targetIndex++] = encodeLastTail(char32);

                    } else if (!UTF16.isSurrogate((char) char32) || isCESU8) {
                        /* 3 bytes to encode from char32 */

                        targetArray[targetIndex++] = encodeHeadOf3(char32);

                        if (targetIndex >= targetLimit) {
                            errorBuffer[errorBufferLength++] = encodeSecondToLastTail(char32);
                            errorBuffer[errorBufferLength++] = encodeLastTail(char32);
                            cr = CoderResult.OVERFLOW;
                            break;
                        }
                        targetArray[targetIndex++] = encodeSecondToLastTail(char32);

                        if (targetIndex >= targetLimit) {
                            errorBuffer[errorBufferLength++] = encodeLastTail(char32);
                            cr = CoderResult.OVERFLOW;
                            break;
                        }
                        targetArray[targetIndex++] = encodeLastTail(char32);

                    } else {
                        /* 4 bytes to encode from char32 and a following char in source */

                        this.sourceIndex = sourceIndex;
                        this.targetIndex = targetIndex;
                        cr = encodeFourBytes(sourceArray, targetArray, sourceLimit, targetLimit,
                                char32, flush);
                        sourceIndex = this.sourceIndex;
                        targetIndex = this.targetIndex;
                        if (cr != null)
                            break;
                    }
                }

                /* set the new source and target positions and return the CoderResult stored in cr */
                source.position(sourceIndex - source.arrayOffset());
                target.position(targetIndex - target.arrayOffset());
                return cr;

            } else {
                int sourceIndex = source.position();
                int sourceLimit = source.limit();
                int targetIndex = target.position();
                int targetLimit = target.limit();

                int char32;
                CoderResult cr;

                /* take care of the special condition of fromUChar32 not being 0 (it is a surrogate) */
                if (fromUChar32 != 0) {
                    char32 = fromUChar32;
                    fromUChar32 = 0;

                    /* 4 bytes to encode from char32 and a following char in source */

                    this.sourceIndex = sourceIndex;
                    this.targetIndex = targetIndex;
                    cr = encodeFourBytes(source, target, sourceLimit, targetLimit, char32, flush);
                    sourceIndex = this.sourceIndex;
                    targetIndex = this.targetIndex;
                    if (cr != null) {
                        source.position(sourceIndex);
                        target.position(targetIndex);
                        return cr;
                    }
                }

                while (true) {
                    if (sourceIndex >= sourceLimit) {
                        /* nothing left to read */
                        cr = CoderResult.UNDERFLOW;
                        break;
                    }
                    if (targetIndex >= targetLimit) {
                        /* no space left to write */
                        cr = CoderResult.OVERFLOW;
                        break;
                    }

                    /* reach the next char into char32 */
                    char32 = source.get(sourceIndex++) & 0xffff;

                    if (char32 <= 0x7f) {
                        /* 1 byte to encode from char32 */

                        target.put(targetIndex++, encodeHeadOf1(char32));

                    } else if (char32 <= 0x7ff) {
                        /* 2 bytes to encode from char32 */

                        target.put(targetIndex++, encodeHeadOf2(char32));

                        if (targetIndex >= targetLimit) {
                            errorBuffer[errorBufferLength++] = encodeLastTail(char32);
                            cr = CoderResult.OVERFLOW;
                            break;
                        }
                        target.put(targetIndex++, encodeLastTail(char32));

                    } else if (!UTF16.isSurrogate((char) char32) || isCESU8) {
                        /* 3 bytes to encode from char32 */

                        target.put(targetIndex++, encodeHeadOf3(char32));

                        if (targetIndex >= targetLimit) {
                            errorBuffer[errorBufferLength++] = encodeSecondToLastTail(char32);
                            errorBuffer[errorBufferLength++] = encodeLastTail(char32);
                            cr = CoderResult.OVERFLOW;
                            break;
                        }
                        target.put(targetIndex++, encodeSecondToLastTail(char32));

                        if (targetIndex >= targetLimit) {
                            errorBuffer[errorBufferLength++] = encodeLastTail(char32);
                            cr = CoderResult.OVERFLOW;
                            break;
                        }
                        target.put(targetIndex++, encodeLastTail(char32));

                    } else {
                        /* 4 bytes to encode from char32 and a following char in source */

                        this.sourceIndex = sourceIndex;
                        this.targetIndex = targetIndex;
                        cr = encodeFourBytes(source, target, sourceLimit, targetLimit, char32,
                                flush);
                        sourceIndex = this.sourceIndex;
                        targetIndex = this.targetIndex;
                        if (cr != null)
                            break;
                    }
                }

                /* set the new source and target positions and return the CoderResult stored in cr */
                source.position(sourceIndex);
                target.position(targetIndex);
                return cr;
            }
        }

        private final CoderResult encodeFourBytes(char[] sourceArray, byte[] targetArray,
                int sourceLimit, int targetLimit, int char32, boolean flush) {

            /* we need to read another char to match up the surrogate stored in char32 */
            if (sourceIndex >= sourceLimit) {
                fromUChar32 = char32;
                return (flush) ? CoderResult.malformedForLength(1) : CoderResult.UNDERFLOW;
            }

            try {
                /* combine char32 and the next char into a code point if possible */
                char32 = UCharacter.getCodePoint((char) char32, sourceArray[sourceIndex++]);
            } catch (IllegalArgumentException ex) {
                /* it was not possible */
                fromUChar32 = char32;
                sourceIndex--;
                return CoderResult.malformedForLength(1);
            }

            /* the rest is routine -- encode four bytes, stopping on overflow */

            targetArray[targetIndex++] = encodeHeadOf4(char32);

            if (targetIndex >= targetLimit) {
                errorBuffer[errorBufferLength++] = encodeThirdToLastTail(char32);
                errorBuffer[errorBufferLength++] = encodeSecondToLastTail(char32);
                errorBuffer[errorBufferLength++] = encodeLastTail(char32);
                return CoderResult.OVERFLOW;
            }
            targetArray[targetIndex++] = encodeThirdToLastTail(char32);

            if (targetIndex >= targetLimit) {
                errorBuffer[errorBufferLength++] = encodeSecondToLastTail(char32);
                errorBuffer[errorBufferLength++] = encodeLastTail(char32);
                return CoderResult.OVERFLOW;
            }
            targetArray[targetIndex++] = encodeSecondToLastTail(char32);

            if (targetIndex >= targetLimit) {
                errorBuffer[errorBufferLength++] = encodeLastTail(char32);
                return CoderResult.OVERFLOW;
            }
            targetArray[targetIndex++] = encodeLastTail(char32);

            /* return null for success */
            return null;
        }

        private final CoderResult encodeFourBytes(CharBuffer source, ByteBuffer target,
                int sourceLimit, int targetLimit, int char32, boolean flush) {

            /* we need to read another char to match up the surrogate stored in char32 */
            if (sourceIndex >= sourceLimit) {
                fromUChar32 = char32;
                return (flush) ? CoderResult.malformedForLength(1) : CoderResult.UNDERFLOW;
            }

            try {
                /* combine char32 and the next char into a code point if possible */
                char32 = UCharacter.getCodePoint((char) char32, source.get(sourceIndex++));
            } catch (IllegalArgumentException ex) {
                /* it was not possible */
                fromUChar32 = char32;
                sourceIndex--;
                return CoderResult.malformedForLength(1);
            }

            /* the rest is routine -- encode four bytes, stopping on overflow */

            target.put(targetIndex++, encodeHeadOf4(char32));

            if (targetIndex >= targetLimit) {
                errorBuffer[errorBufferLength++] = encodeThirdToLastTail(char32);
                errorBuffer[errorBufferLength++] = encodeSecondToLastTail(char32);
                errorBuffer[errorBufferLength++] = encodeLastTail(char32);
                return CoderResult.OVERFLOW;
            }
            target.put(targetIndex++, encodeThirdToLastTail(char32));

            if (targetIndex >= targetLimit) {
                errorBuffer[errorBufferLength++] = encodeSecondToLastTail(char32);
                errorBuffer[errorBufferLength++] = encodeLastTail(char32);
                return CoderResult.OVERFLOW;
            }
            target.put(targetIndex++, encodeSecondToLastTail(char32));

            if (targetIndex >= targetLimit) {
                errorBuffer[errorBufferLength++] = encodeLastTail(char32);
                return CoderResult.OVERFLOW;
            }
            target.put(targetIndex++, encodeLastTail(char32));

            /* return null for success */
            return null;
        }

        private int sourceIndex;

        private int targetIndex;

    }

    private static final byte encodeHeadOf1(int char32) {
        return (byte) char32;
    }

    private static final byte encodeHeadOf2(int char32) {
        return (byte) (0xc0 | (char32 >>> 6));
    }

    private static final byte encodeHeadOf3(int char32) {
        return (byte) (0xe0 | ((char32 >>> 12)));
    }

    private static final byte encodeHeadOf4(int char32) {
        return (byte) (0xf0 | ((char32 >>> 18)));
    }

    private static final byte encodeThirdToLastTail(int char32) {
        return (byte) (0x80 | ((char32 >>> 12) & 0x3f));
    }

    private static final byte encodeSecondToLastTail(int char32) {
        return (byte) (0x80 | ((char32 >>> 6) & 0x3f));
    }

    private static final byte encodeLastTail(int char32) {
        return (byte) (0x80 | (char32 & 0x3f));
    }

    /* single-code point definitions -------------------------------------------- */

    /*
     * Does this code unit (byte) encode a code point by itself (US-ASCII 0..0x7f)? @param c 8-bit
     * code unit (byte) @return TRUE or FALSE @draft ICU 3.6
     */
    // static final boolean isSingle(byte c) {return (((c)&0x80)==0);}
    /*
     * Is this code unit (byte) a UTF-8 lead byte? @param c 8-bit code unit (byte) @return TRUE or
     * FALSE @draft ICU 3.6
     */
    // static final boolean isLead(byte c) {return ((((c)-0xc0) &
    // UConverterConstants.UNSIGNED_BYTE_MASK)<0x3e);}
    /*
     * Is this code unit (byte) a UTF-8 trail byte?
     * 
     * @param c
     *            8-bit code unit (byte)
     * @return TRUE or FALSE
     * @draft ICU 3.6
     */
    /*private static final boolean isTrail(byte c) {
        return (((c) & 0xc0) == 0x80);
    }*/

    public CharsetDecoder newDecoder() {
        return new CharsetDecoderUTF8(this);
    }

    public CharsetEncoder newEncoder() {
        return new CharsetEncoderUTF8(this);
    }
}

/*
 * The purpose of this class is to set isCESU8 to true in the super class, and to allow the Charset
 * framework to open the variant UTF-8 converter without extra setup work. CESU-8 encodes/decodes
 * supplementary characters as 6 bytes instead of the proper 4 bytes.
 */
class CharsetCESU8 extends CharsetUTF8 {
    public CharsetCESU8(String icuCanonicalName, String javaCanonicalName, String[] aliases) {
        super(icuCanonicalName, javaCanonicalName, aliases);
    }
}
