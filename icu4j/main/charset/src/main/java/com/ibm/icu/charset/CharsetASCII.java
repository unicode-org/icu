// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/**
 *******************************************************************************
 * Copyright (C) 2006-2011, International Business Machines Corporation and    *
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

import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

class CharsetASCII extends CharsetICU {
    protected byte[] fromUSubstitution = new byte[] { (byte) 0x1a };

    public CharsetASCII(String icuCanonicalName, String javaCanonicalName, String[] aliases) {
        super(icuCanonicalName, javaCanonicalName, aliases);
        maxBytesPerChar = 1;
        minBytesPerChar = 1;
        maxCharsPerByte = 1;
    }

    class CharsetDecoderASCII extends CharsetDecoderICU {

        public CharsetDecoderASCII(CharsetICU cs) {
            super(cs);
        }

        @Override
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

            CoderResult cr;
            int oldSource = source.position();
            int oldTarget = target.position();

            if (source.hasArray() && target.hasArray()) {
                /* optimized loop */

                /*
                 * extract arrays from the buffers and obtain various constant values that will be
                 * necessary in the core loop
                 */
                byte[] sourceArray = source.array();
                int sourceOffset = source.arrayOffset();
                int sourceIndex = oldSource + sourceOffset;
                int sourceLength = source.limit() - oldSource;

                char[] targetArray = target.array();
                int targetOffset = target.arrayOffset();
                int targetIndex = oldTarget + targetOffset;
                int targetLength = target.limit() - oldTarget;

                int limit = ((sourceLength < targetLength) ? sourceLength : targetLength)
                        + sourceIndex;
                int offset = targetIndex - sourceIndex;

                /*
                 * perform the core loop... if it returns null, it must be due to an overflow or
                 * underflow
                 */
                cr = decodeLoopCoreOptimized(source, target, sourceArray, targetArray, sourceIndex, offset, limit);
                if (cr == null) {
                    if (sourceLength <= targetLength) {
                        source.position(oldSource + sourceLength);
                        target.position(oldTarget + sourceLength);
                        cr = CoderResult.UNDERFLOW;
                    } else {
                        source.position(oldSource + targetLength);
                        target.position(oldTarget + targetLength);
                        cr = CoderResult.OVERFLOW;
                    }
                }
            } else {
                /* unoptimized loop */
                cr = decodeLoopCoreUnoptimized(source, target);
                if (cr == CoderResult.OVERFLOW) {
                    /* the target is full */
                    source.position(source.position() - 1); /* rewind by 1 */
                }
            }

            /* set offsets since the start */
            if (offsets != null) {
                int count = target.position() - oldTarget;
                int sourceIndex = -1;
                while (--count >= 0) offsets.put(++sourceIndex);
            }

            return cr;
        }

        protected CoderResult decodeLoopCoreOptimized(ByteBuffer source, CharBuffer target,
                byte[] sourceArray, char[] targetArray, int oldSource, int offset, int limit) {
            int i, ch = 0;

            /*
             * perform ascii conversion from the source array to the target array, making sure each
             * byte in the source is within the correct range
             */
            for (i = oldSource; i < limit && (((ch = (sourceArray[i] & 0xff)) & 0x80) == 0); i++)
                targetArray[i + offset] = (char) ch;

            /*
             * if some byte was not in the correct range, we need to deal with this byte by calling
             * decodeMalformedOrUnmappable and move the source and target positions to reflect the
             * early termination of the loop
             */
            if ((ch & 0x80) != 0) {
                source.position(i + 1);
                target.position(i + offset);
                return decodeMalformedOrUnmappable(ch);
            } else
                return null;
        }

        protected CoderResult decodeLoopCoreUnoptimized(ByteBuffer source, CharBuffer target) {
            int ch = 0;

            /*
             * perform ascii conversion from the source buffer to the target buffer, making sure
             * each byte in the source is within the correct range
             */
            while (source.hasRemaining()) {
                ch = source.get() & 0xff;

                if ((ch & 0x80) == 0) {
                    if (target.hasRemaining()) {
                        target.put((char)ch);
                    } else {
                        return CoderResult.OVERFLOW;
                    }
                } else {
                    /*
                     * if we reach here, it's because a character was not in the correct range, and we need
                     * to deak with this by calling decodeMalformedOrUnmappable
                     */
                    return decodeMalformedOrUnmappable(ch);
                }
            }

            return CoderResult.UNDERFLOW;
        }

        protected CoderResult decodeMalformedOrUnmappable(int ch) {
            /*
             * put the guilty character into toUBytesArray and return a message saying that the
             * character was malformed and of length 1.
             */
            toUBytesArray[0] = (byte) ch;
            toULength = 1;
            return CoderResult.malformedForLength(1);
        }
    }

    class CharsetEncoderASCII extends CharsetEncoderICU {

        public CharsetEncoderASCII(CharsetICU cs) {
            super(cs, fromUSubstitution);
            implReset();
        }

        private final static int NEED_TO_WRITE_BOM = 1;

        @Override
        protected void implReset() {
            super.implReset();
            fromUnicodeStatus = NEED_TO_WRITE_BOM;
        }

        @Override
        protected CoderResult encodeLoop(CharBuffer source, ByteBuffer target, IntBuffer offsets,
                boolean flush) {
            if (!source.hasRemaining()) {
                /* no input, nothing to do */
                return CoderResult.UNDERFLOW;
            }
            if (!target.hasRemaining()) {
                /* no output available, can't do anything */
                return CoderResult.OVERFLOW;
            }

            CoderResult cr;
            int oldSource = source.position();
            int oldTarget = target.position();

            if (fromUChar32 != 0) {
                /*
                 * if we have a leading character in fromUChar32 that needs to be dealt with, we
                 * need to check for a matching trail character and taking the appropriate action as
                 * dictated by encodeTrail.
                 */
                cr = encodeTrail(source, (char) fromUChar32, flush);
            } else {
                if (source.hasArray() && target.hasArray()) {
                    /* optimized loop */

                    /*
                     * extract arrays from the buffers and obtain various constant values that will
                     * be necessary in the core loop
                     */
                    char[] sourceArray = source.array();
                    int sourceOffset = source.arrayOffset();
                    int sourceIndex = oldSource + sourceOffset;
                    int sourceLength = source.limit() - oldSource;

                    byte[] targetArray = target.array();
                    int targetOffset = target.arrayOffset();
                    int targetIndex = oldTarget + targetOffset;
                    int targetLength = target.limit() - oldTarget;

                    int limit = ((sourceLength < targetLength) ? sourceLength : targetLength)
                            + sourceIndex;
                    int offset = targetIndex - sourceIndex;

                    /*
                     * perform the core loop... if it returns null, it must be due to an overflow or
                     * underflow
                     */
                    cr = encodeLoopCoreOptimized(source, target, sourceArray, targetArray, sourceIndex, offset, limit, flush);
                    if (cr == null) {
                        if (sourceLength <= targetLength) {
                            source.position(oldSource + sourceLength);
                            target.position(oldTarget + sourceLength);
                            cr = CoderResult.UNDERFLOW;
                        } else {
                            source.position(oldSource + targetLength);
                            target.position(oldTarget + targetLength);
                            cr = CoderResult.OVERFLOW;
                        }
                    }
                } else {
                    /* unoptimized loop */

                    cr = encodeLoopCoreUnoptimized(source, target, flush);

                    if (cr == CoderResult.OVERFLOW) {
                        source.position(source.position() - 1); /* rewind by 1 */
                    }
                }
            }

            /* set offsets since the start */
            if (offsets != null) {
                int count = target.position() - oldTarget;
                int sourceIndex = -1;
                while (--count >= 0) offsets.put(++sourceIndex);
            }

            return cr;
        }

        protected CoderResult encodeLoopCoreOptimized(CharBuffer source, ByteBuffer target,
                char[] sourceArray, byte[] targetArray, int oldSource, int offset, int limit,
                boolean flush) {
            int i, ch = 0;

            /*
             * perform ascii conversion from the source array to the target array, making sure each
             * char in the source is within the correct range
             */
            for (i = oldSource; i < limit && (((ch = sourceArray[i]) & 0xff80) == 0); i++)
                targetArray[i + offset] = (byte) ch;

            /*
             * if some byte was not in the correct range, we need to deal with this byte by calling
             * encodeMalformedOrUnmappable and move the source and target positions to reflect the
             * early termination of the loop
             */
            if ((ch & 0xff80) != 0) {
                source.position((i + 1) - source.arrayOffset());
                target.position(i + offset);
                return encodeMalformedOrUnmappable(source, ch, flush);
            } else
                return null;
        }

        protected CoderResult encodeLoopCoreUnoptimized(CharBuffer source, ByteBuffer target, boolean flush) {
            int ch;

            /*
             * perform ascii conversion from the source buffer to the target buffer, making sure
             * each char in the source is within the correct range
             */
            while (source.hasRemaining()) {
                ch = source.get();

                if ((ch & 0xff80) == 0) {
                    if (target.hasRemaining()) {
                        target.put((byte) ch);
                    } else {
                        return CoderResult.OVERFLOW;
                    }
                } else {
                    /*
                     * if we reach here, it's because a character was not in the correct range, and we need
                     * to deak with this by calling encodeMalformedOrUnmappable.
                     */
                    return encodeMalformedOrUnmappable(source, ch, flush);
                }
            }

            return CoderResult.UNDERFLOW;
        }

        protected final CoderResult encodeMalformedOrUnmappable(CharBuffer source, int ch, boolean flush) {
            /*
             * if the character is a lead surrogate, we need to call encodeTrail to attempt to match
             * it up with a trail surrogate. if not, the character is unmappable.
             */
            return (UTF16.isSurrogate(ch))
                    ? encodeTrail(source, (char) ch, flush)
                    : CoderResult.unmappableForLength(1);
        }

        private final CoderResult encodeTrail(CharBuffer source, char lead, boolean flush) {
            /*
             * ASCII doesn't support characters in the BMP, so if handleSurrogates returns null,
             * we leave fromUChar32 alone (it should store a new codepoint) and call it unmappable.
             */
            CoderResult cr = handleSurrogates(source, lead);
            if (cr != null) {
                return cr;
            } else {
                //source.position(source.position() - 2);
                return CoderResult.unmappableForLength(2);
            }
        }

    }

    @Override
    public CharsetDecoder newDecoder() {
        return new CharsetDecoderASCII(this);
    }

    @Override
    public CharsetEncoder newEncoder() {
        return new CharsetEncoderASCII(this);
    }

    @Override
    void getUnicodeSetImpl( UnicodeSet setFillIn, int which){
        setFillIn.add(0,0x7f);
     }
}
