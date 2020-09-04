// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/**
 *******************************************************************************
 * Copyright (C) 2006-2011, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.charset;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

import com.ibm.icu.text.UnicodeSet;

class Charset88591 extends CharsetASCII {
    public Charset88591(String icuCanonicalName, String javaCanonicalName, String[] aliases) {
        super(icuCanonicalName, javaCanonicalName, aliases);
    }

    class CharsetDecoder88591 extends CharsetDecoderASCII {
        public CharsetDecoder88591(CharsetICU cs) {
            super(cs);
        }

        @Override
        protected CoderResult decodeLoopCoreOptimized(ByteBuffer source, CharBuffer target,
                byte[] sourceArray, char[] targetArray, int oldSource, int offset, int limit) {

            /*
             * perform 88591 conversion from the source array to the target array. no range check is
             * necessary.
             */
            for (int i = oldSource; i < limit; i++)
                targetArray[i + offset] = (char) (sourceArray[i] & 0xff);

            return null;
        }

        @Override
        protected CoderResult decodeLoopCoreUnoptimized(ByteBuffer source, CharBuffer target) {
            byte ch;
            /*
             * perform 88591 conversion from the source buffer to the target buffer. no range check
             * is necessary.
             */
            while (source.hasRemaining()) {
                ch = source.get();
                if (target.hasRemaining()) {
                    target.put((char) (ch & 0xff));
                } else {
                    return CoderResult.OVERFLOW;
                }
            }

            return CoderResult.UNDERFLOW;
        }
    }

    class CharsetEncoder88591 extends CharsetEncoderASCII {
        public CharsetEncoder88591(CharsetICU cs) {
            super(cs);
        }

        @Override
        protected final CoderResult encodeLoopCoreOptimized(CharBuffer source, ByteBuffer target,
                char[] sourceArray, byte[] targetArray, int oldSource, int offset, int limit,
                boolean flush) {
            int i, ch = 0;

            /*
             * perform 88591 conversion from the source array to the target array, making sure each
             * char in the source is within the correct range
             */
            for (i = oldSource; i < limit; i++) {
                ch = sourceArray[i];
                if ((ch & 0xff00) == 0) {
                    targetArray[i + offset] = (byte) ch;
                } else {
                    break;
                }
            }

            /*
             * if some byte was not in the correct range, we need to deal with this byte by calling
             * encodeMalformedOrUnmappable and move the source and target positions to reflect the
             * early termination of the loop
             */
            if ((ch & 0xff00) != 0) {
                source.position((i + 1) - source.arrayOffset());
                target.position(i + offset);
                return encodeMalformedOrUnmappable(source, ch, flush);
            } else
                return null;
        }

        @Override
        protected final CoderResult encodeLoopCoreUnoptimized(CharBuffer source, ByteBuffer target, boolean flush) {
            int ch;

            /*
             * perform 88591 conversion from the source buffer to the target buffer, making sure
             * each char in the source is within the correct range
             */

            while (source.hasRemaining()) {
                ch = source.get();
                if ((ch & 0xff00) == 0) {
                    if (target.hasRemaining()) {
                        target.put((byte) ch);
                    } else {
                        return CoderResult.OVERFLOW;
                    }
                } else {
                    /*
                     * if we reach here, it's because a character was not in the correct range, and we need
                     * to deal with this by calling encodeMalformedOrUnmappable.
                     */
                    return encodeMalformedOrUnmappable(source, ch, flush);
                }
            }

            return CoderResult.UNDERFLOW;
        }

    }

    @Override
    public CharsetDecoder newDecoder() {
        return new CharsetDecoder88591(this);
    }

    @Override
    public CharsetEncoder newEncoder() {
        return new CharsetEncoder88591(this);
    }

    @Override
    void getUnicodeSetImpl( UnicodeSet setFillIn, int which){
        setFillIn.add(0,0xff);
     }
}
