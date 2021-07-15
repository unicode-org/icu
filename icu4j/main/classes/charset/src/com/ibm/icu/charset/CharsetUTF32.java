// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/**
 *******************************************************************************
 * Copyright (C) 2006-2008, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
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

/**
 * @author Niti Hantaweepant
 */
class CharsetUTF32 extends CharsetICU {

    private static final int SIGNATURE_LENGTH = 4;
    private static final byte[] fromUSubstitution_BE = { (byte) 0, (byte) 0, (byte) 0xff, (byte) 0xfd };
    private static final byte[] fromUSubstitution_LE = { (byte) 0xfd, (byte) 0xff, (byte) 0, (byte) 0 };
    private static final byte[] BOM_BE = { 0, 0, (byte) 0xfe, (byte) 0xff };
    private static final byte[] BOM_LE = { (byte) 0xff, (byte) 0xfe, 0, 0 };
    private static final int ENDIAN_XOR_BE = 0;
    private static final int ENDIAN_XOR_LE = 3;
    private static final int NEED_TO_WRITE_BOM = 1;

    private boolean isEndianSpecified;
    private boolean isBigEndian;
    private int endianXOR;
    private byte[] bom;
    private byte[] fromUSubstitution;

    public CharsetUTF32(String icuCanonicalName, String javaCanonicalName, String[] aliases) {
        super(icuCanonicalName, javaCanonicalName, aliases);

        this.isEndianSpecified = (this instanceof CharsetUTF32BE || this instanceof CharsetUTF32LE);
        this.isBigEndian = !(this instanceof CharsetUTF32LE);

        if (isBigEndian) {
            this.bom = BOM_BE;
            this.fromUSubstitution = fromUSubstitution_BE;
            this.endianXOR = ENDIAN_XOR_BE;
        } else {
            this.bom = BOM_LE;
            this.fromUSubstitution = fromUSubstitution_LE;
            this.endianXOR = ENDIAN_XOR_LE;
        }

        maxBytesPerChar = 4;
        minBytesPerChar = 4;
        maxCharsPerByte = 1;
    }

    class CharsetDecoderUTF32 extends CharsetDecoderICU {

        private boolean isBOMReadYet;
        private int actualEndianXOR;
        private byte[] actualBOM;

        public CharsetDecoderUTF32(CharsetICU cs) {
            super(cs);
        }

        @Override
        protected void implReset() {
            super.implReset();
            isBOMReadYet = false;
            actualBOM = null;
        }

        @Override
        protected CoderResult decodeLoop(ByteBuffer source, CharBuffer target, IntBuffer offsets, boolean flush) {
            /*
             * If we detect a BOM in this buffer, then we must add the BOM size to the offsets because the actual
             * converter function will not see and count the BOM. offsetDelta will have the number of the BOM bytes that
             * are in the current buffer.
             */
            if (!isBOMReadYet) {
                while (true) {
                    if (!source.hasRemaining())
                        return CoderResult.UNDERFLOW;

                    toUBytesArray[toULength++] = source.get();

                    if (toULength == 1) {
                        // on the first byte, we haven't decided whether or not it's bigEndian yet
                        if ((!isEndianSpecified || isBigEndian)
                                && toUBytesArray[toULength - 1] == BOM_BE[toULength - 1]) {
                            actualBOM = BOM_BE;
                            actualEndianXOR = ENDIAN_XOR_BE;
                        } else if ((!isEndianSpecified || !isBigEndian)
                                && toUBytesArray[toULength - 1] == BOM_LE[toULength - 1]) {
                            actualBOM = BOM_LE;
                            actualEndianXOR = ENDIAN_XOR_LE;
                        } else {
                            // we do not have a BOM (and we have toULength==1 bytes)
                            actualBOM = null;
                            actualEndianXOR = endianXOR;
                            break;
                        }
                    } else if (toUBytesArray[toULength - 1] != actualBOM[toULength - 1]) {
                        // we do not have a BOM (and we have toULength bytes)
                        actualBOM = null;
                        actualEndianXOR = endianXOR;
                        break;
                    } else if (toULength == SIGNATURE_LENGTH) {
                        // we found a BOM! at last!
                        // too bad we have to get ignore it now (like it was unwanted or something)
                        toULength = 0;
                        break;
                    }
                }

                isBOMReadYet = true;
            }

            // now that we no longer need to look for a BOM, let's do some work
            int char32;

            while (true) {
                while (toULength < 4) {
                    if (!source.hasRemaining())
                        return CoderResult.UNDERFLOW;
                    toUBytesArray[toULength++] = source.get();
                }

                if (!target.hasRemaining())
                    return CoderResult.OVERFLOW;

                char32 = 0;
                for (int i = 0; i < 4; i++)
                    char32 = (char32 << 8)
                            | (toUBytesArray[i ^ actualEndianXOR] & UConverterConstants.UNSIGNED_BYTE_MASK);

                if (0 <= char32 && char32 <= UConverterConstants.MAXIMUM_UTF && !isSurrogate(char32)) {
                    toULength = 0;
                    if (char32 <= UConverterConstants.MAXIMUM_UCS2) {
                        /* fits in 16 bits */
                        target.put((char) char32);
                    } else {
                        /* write out the surrogates */
                        target.put(UTF16.getLeadSurrogate(char32));
                        char32 = UTF16.getTrailSurrogate(char32);
                        if (target.hasRemaining()) {
                            target.put((char) char32);
                        } else {
                            /* Put in overflow buffer (not handled here) */
                            charErrorBufferArray[0] = (char) char32;
                            charErrorBufferLength = 1;
                            return CoderResult.OVERFLOW;
                        }
                    }
                } else {
                    return CoderResult.malformedForLength(toULength);
                }
            }
        }
    }

    class CharsetEncoderUTF32 extends CharsetEncoderICU {
        private final byte[] temp = new byte[4];

        public CharsetEncoderUTF32(CharsetICU cs) {
            super(cs, fromUSubstitution);
            fromUnicodeStatus = isEndianSpecified ? 0 : NEED_TO_WRITE_BOM;
        }

        @Override
        protected void implReset() {
            super.implReset();
            fromUnicodeStatus = isEndianSpecified ? 0 : NEED_TO_WRITE_BOM;
        }

        @Override
        protected CoderResult encodeLoop(CharBuffer source, ByteBuffer target, IntBuffer offsets, boolean flush) {
            CoderResult cr;

            /* write the BOM if necessary */
            if (fromUnicodeStatus == NEED_TO_WRITE_BOM) {
                if (!target.hasRemaining())
                    return CoderResult.OVERFLOW;

                fromUnicodeStatus = 0;
                cr = fromUWriteBytes(this, bom, 0, bom.length, target, offsets, -1);
                if (cr.isOverflow())
                    return cr;
            }

            if (fromUChar32 != 0) {
                if (!target.hasRemaining())
                    return CoderResult.OVERFLOW;

                // a note: fromUChar32 will either be 0 or a lead surrogate
                cr = encodeChar(source, target, offsets, (char) fromUChar32);
                if (cr != null)
                    return cr;
            }

            while (true) {
                if (!source.hasRemaining())
                    return CoderResult.UNDERFLOW;
                if (!target.hasRemaining())
                    return CoderResult.OVERFLOW;

                cr = encodeChar(source, target, offsets, source.get());
                if (cr != null)
                    return cr;
            }
        }

        private final CoderResult encodeChar(CharBuffer source, ByteBuffer target, IntBuffer offsets, char ch) {
            int sourceIndex = source.position() - 1;
            CoderResult cr;
            int char32;

            if (UTF16.isSurrogate(ch)) {
                cr = handleSurrogates(source, ch);
                if (cr != null)
                    return cr;

                char32 = fromUChar32;
                fromUChar32 = 0;
            } else {
                char32 = ch;
            }

            /* We cannot get any larger than 10FFFF because we are coming from UTF-16 */
            // temp[0 ^ endianXOR] = (byte) (char32 >>> 24); // (always 0)
            temp[1 ^ endianXOR] = (byte) (char32 >>> 16); // same as (byte)((char32 >>> 16) & 0x1f)
            temp[2 ^ endianXOR] = (byte) (char32 >>> 8);
            temp[3 ^ endianXOR] = (byte) (char32);
            cr = fromUWriteBytes(this, temp, 0, 4, target, offsets, sourceIndex);
            return (cr.isUnderflow() ? null : cr);
        }
    }

    @Override
    public CharsetDecoder newDecoder() {
        return new CharsetDecoderUTF32(this);
    }

    @Override
    public CharsetEncoder newEncoder() {
        return new CharsetEncoderUTF32(this);
    }


    @Override
    void getUnicodeSetImpl( UnicodeSet setFillIn, int which){
        getNonSurrogateUnicodeSet(setFillIn);
    }
}
