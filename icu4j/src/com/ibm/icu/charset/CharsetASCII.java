/**
 *******************************************************************************
 * Copyright (C) 2006, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 *******************************************************************************
 */
package com.ibm.icu.charset;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UTF16;

class CharsetASCII extends CharsetICU {
    protected byte[] fromUSubstitution = new byte[] { (byte) 0x1a };

    public CharsetASCII(String icuCanonicalName, String javaCanonicalName,
            String[] aliases) {
        super(icuCanonicalName, javaCanonicalName, aliases);
        maxBytesPerChar = 1;
        minBytesPerChar = 1;
        maxCharsPerByte = 1;
    }

    class CharsetDecoderASCII extends CharsetDecoderICU {

        public CharsetDecoderASCII(CharsetICU cs) {
            super(cs);
        }

        protected CoderResult decodeLoop(ByteBuffer source, CharBuffer target,
                IntBuffer offsets, boolean flush) {
            if (!source.hasRemaining() && toUnicodeStatus == 0) {
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

                byte[] sourceArray = source.array();
                char[] targetArray = target.array();
                int offset = oldTarget - oldSource;
                int sourceLength = source.limit() - oldSource;
                int targetLength = target.limit() - oldTarget;
                int limit = ((sourceLength < targetLength) ? sourceLength
                        : targetLength)
                        + oldSource;

                if ((cr = decodeLoopCoreOptimized(source, target, sourceArray,
                        targetArray, oldSource, offset, limit)) == null) {
                    if (sourceLength <= targetLength) {
                        source.position(oldSource + sourceLength);
                        target.position(oldTarget + sourceLength);
                        cr = CoderResult.UNDERFLOW;
                    } else {
                        source.position(oldSource + targetLength + 1);
                        target.position(oldTarget + targetLength);
                        cr = CoderResult.OVERFLOW;
                    }
                }
            } else {
                /* unoptimized loop */

                try {
                    cr = decodeLoopCoreUnoptimized(source, target);

                } catch (BufferUnderflowException ex) {
                    /* all of the source has been read */
                    cr = CoderResult.UNDERFLOW;
                } catch (BufferOverflowException ex) {
                    /* the target is full */
                    cr = CoderResult.OVERFLOW;
                }
            }

            /* set offsets since the start */
            if (offsets != null) {
                int count = target.position() - oldTarget;
                int sourceIndex = -1;
                while (--count >= 0)
                    offsets.put(++sourceIndex);
            }

            return cr;
        }

        protected CoderResult decodeLoopCoreOptimized(ByteBuffer source,
                CharBuffer target, byte[] sourceArray, char[] targetArray,
                int oldSource, int offset, int limit) {
            int i, ch = 0;
            for (i = oldSource; i < limit
                    && (((ch = (sourceArray[i] & 0xff)) & 0x80) == 0); i++)
                targetArray[i + offset] = (char) ch;

            if ((ch & 0x80) != 0) {
                source.position(i + 1);
                target.position(i + offset);
                return decodeIllegal(ch);
            } else
                return null;
        }

        protected CoderResult decodeLoopCoreUnoptimized(ByteBuffer source,
                CharBuffer target) throws BufferUnderflowException,
                BufferOverflowException {
            int ch = 0;
            while (((ch = (source.get() & 0xff)) & 0x80) == 0)
                target.put((char) ch);

            return decodeIllegal(ch);
        }

        protected CoderResult decodeIllegal(int ch) {
            toUBytesArray[0] = (byte) ch;
            return CoderResult.malformedForLength(toULength = 1);
        }
    }

    class CharsetEncoderASCII extends CharsetEncoderICU {

        public CharsetEncoderASCII(CharsetICU cs) {
            super(cs, fromUSubstitution);
            implReset();
        }

        private final static int NEED_TO_WRITE_BOM = 1;

        protected void implReset() {
            super.implReset();
            fromUnicodeStatus = NEED_TO_WRITE_BOM;
        }

        protected CoderResult encodeLoop(CharBuffer source, ByteBuffer target,
                IntBuffer offsets, boolean flush) {
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
                cr = encodeTrail(source, (char) fromUChar32, flush);
            } else {
                int ch = 0;

                if (source.hasArray() && target.hasArray()) {
                    /* optimized loop */

                    char[] sourceArray = source.array();
                    byte[] targetArray = target.array();
                    int offset = oldTarget - oldSource;
                    int sourceLength = source.limit() - oldSource;
                    int targetLength = target.limit() - oldTarget;
                    int limit = ((sourceLength < targetLength) ? sourceLength
                            : targetLength)
                            + oldSource;

                    if ((cr = encodeLoopCoreOptimized(source, target,
                            sourceArray, targetArray, oldSource, offset, limit,
                            flush)) == null) {
                        if (sourceLength <= targetLength) {
                            source.position(oldSource + sourceLength);
                            target.position(oldTarget + sourceLength);
                            cr = CoderResult.UNDERFLOW;
                        } else {
                            source.position(oldSource + targetLength + 1);
                            target.position(oldTarget + targetLength);
                            cr = CoderResult.OVERFLOW;
                        }
                    }
                } else {
                    /* unoptimized loop */

                    try {
                        cr = encodeLoopCoreUnoptimized(source, target, flush);

                        cr = encodeIllegal(source, ch, flush);

                    } catch (BufferUnderflowException ex) {
                        cr = CoderResult.UNDERFLOW;
                    } catch (BufferOverflowException ex) {
                        cr = CoderResult.OVERFLOW;
                    }
                }
            }

            /* set offsets since the start */
            if (offsets != null) {
                int count = target.position() - oldTarget;
                int sourceIndex = -1;
                while (--count >= 0)
                    offsets.put(++sourceIndex);
            }

            return cr;
        }

        protected CoderResult encodeLoopCoreOptimized(CharBuffer source,
                ByteBuffer target, char[] sourceArray, byte[] targetArray,
                int oldSource, int offset, int limit, boolean flush) {
            int i, ch = 0;
            for (i = oldSource; i < limit
                    && (((ch = (int) sourceArray[i]) & 0xff80) == 0); i++)
                targetArray[i + offset] = (byte) ch;

            if ((ch & 0xff80) != 0) {
                source.position(i + 1);
                target.position(i + offset);
                return encodeIllegal(source, ch, flush);
            } else
                return null;
        }

        protected CoderResult encodeLoopCoreUnoptimized(CharBuffer source,
                ByteBuffer target, boolean flush)
                throws BufferUnderflowException, BufferOverflowException {
            int ch;
            while (((ch = (int) source.get()) & 0xff80) == 0)
                target.put((byte) ch);

            return encodeIllegal(source, ch, flush);
        }

        protected CoderResult encodeIllegal(CharBuffer source, int ch,
                boolean flush) {
            return (UTF16.isLeadSurrogate((char) ch)) ? encodeTrail(source,
                    (char) ch, flush) : CoderResult.unmappableForLength(1);
        }

        protected CoderResult encodeTrail(CharBuffer source, char lead,
                boolean flush) {
            if (source.hasRemaining()) {
                char trail = source.get();
                if (UTF16.isTrailSurrogate(trail)) {
                    fromUChar32 = UCharacter.getCodePoint(lead, trail);
                    return CoderResult.unmappableForLength(2); /* two chars */
                } else {
                    fromUChar32 = lead;
                    source.position(source.position() - 1); /* rewind by 1 */
                    return CoderResult.malformedForLength(1);
                }
            } else {
                fromUChar32 = lead;
                if (flush)
                    return CoderResult.malformedForLength(1);
                else
                    return CoderResult.UNDERFLOW;
            }
        }

    }

    public CharsetDecoder newDecoder() {
        return new CharsetDecoderASCII(this);
    }

    public CharsetEncoder newEncoder() {
        return new CharsetEncoderASCII(this);
    }

}
