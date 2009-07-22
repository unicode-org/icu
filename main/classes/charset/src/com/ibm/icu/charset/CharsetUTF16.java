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
class CharsetUTF16 extends CharsetICU {

    private static final int SIGNATURE_LENGTH = 2;
    private static final byte[] fromUSubstitution_BE = { (byte) 0xff, (byte) 0xfd };
    private static final byte[] fromUSubstitution_LE = { (byte) 0xfd, (byte) 0xff };
    private static final byte[] BOM_BE = { (byte) 0xfe, (byte) 0xff };
    private static final byte[] BOM_LE = { (byte) 0xff, (byte) 0xfe };
    private static final int ENDIAN_XOR_BE = 0;
    private static final int ENDIAN_XOR_LE = 1;
    private static final int NEED_TO_WRITE_BOM = 1;

    private boolean isEndianSpecified;
    private boolean isBigEndian;
    private int endianXOR;
    private byte[] bom;
    private byte[] fromUSubstitution;

    public CharsetUTF16(String icuCanonicalName, String javaCanonicalName, String[] aliases) {
        super(icuCanonicalName, javaCanonicalName, aliases);

        this.isEndianSpecified = (this instanceof CharsetUTF16BE || this instanceof CharsetUTF16LE);
        this.isBigEndian = !(this instanceof CharsetUTF16LE);

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
        minBytesPerChar = 2;
        maxCharsPerByte = 1;
    }

    class CharsetDecoderUTF16 extends CharsetDecoderICU {
        
        private boolean isBOMReadYet;
        private int actualEndianXOR;
        private byte[] actualBOM;

        public CharsetDecoderUTF16(CharsetICU cs) {
            super(cs);
        }

        protected void implReset() {
            super.implReset();
            isBOMReadYet = false;
            actualBOM = null;
        }

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

            // if we have unfinished business
            if (toUnicodeStatus != 0) {
                CoderResult cr = decodeTrail(source, target, offsets, (char) toUnicodeStatus);
                if (cr != null)
                    return cr;
            }

            char char16;

            while (true) {
                while (toULength < 2) {
                    if (!source.hasRemaining())
                        return CoderResult.UNDERFLOW;
                    toUBytesArray[toULength++] = source.get();
                }

                if (!target.hasRemaining())
                    return CoderResult.OVERFLOW;

                char16 = (char) (((toUBytesArray[0 ^ actualEndianXOR] & UConverterConstants.UNSIGNED_BYTE_MASK) << 8) | ((toUBytesArray[1 ^ actualEndianXOR] & UConverterConstants.UNSIGNED_BYTE_MASK)));

                if (!UTF16.isSurrogate(char16)) {
                    toULength = 0;
                    target.put(char16);
                } else {
                    CoderResult cr = decodeTrail(source, target, offsets, char16);
                    if (cr != null)
                        return cr;
                }
            }
        }

        private final CoderResult decodeTrail(ByteBuffer source, CharBuffer target, IntBuffer offsets, char lead) {
            if (!UTF16.isLeadSurrogate(lead)) {
                // 2 bytes, lead malformed
                toUnicodeStatus = 0;
                return CoderResult.malformedForLength(2);
            }

            while (toULength < 4) {
                if (!source.hasRemaining()) {
                    // let this be unfinished business
                    toUnicodeStatus = lead;
                    return CoderResult.UNDERFLOW;
                }
                toUBytesArray[toULength++] = source.get();
            }

            char trail = (char) (((toUBytesArray[2 ^ actualEndianXOR] & UConverterConstants.UNSIGNED_BYTE_MASK) << 8) | ((toUBytesArray[3 ^ actualEndianXOR] & UConverterConstants.UNSIGNED_BYTE_MASK)));

            if (!UTF16.isTrailSurrogate(trail)) {
                // pretend like we didnt read the last 2 bytes
                toULength = 2;
                source.position(source.position() - 2);

                // 2 bytes, lead malformed
                toUnicodeStatus = 0;
                return CoderResult.malformedForLength(2);
            }

            toUnicodeStatus = 0;
            toULength = 0;

            target.put(lead);

            if (target.hasRemaining()) {
                target.put(trail);
                return null;
            } else {
                /* Put in overflow buffer (not handled here) */
                charErrorBufferArray[0] = trail;
                charErrorBufferLength = 1;
                return CoderResult.OVERFLOW;
            }
        }
    }

    class CharsetEncoderUTF16 extends CharsetEncoderICU {
        private final byte[] temp = new byte[4];

        public CharsetEncoderUTF16(CharsetICU cs) {
            super(cs, fromUSubstitution);
            fromUnicodeStatus = isEndianSpecified ? 0 : NEED_TO_WRITE_BOM;
        }

        protected void implReset() {
            super.implReset();
            fromUnicodeStatus = isEndianSpecified ? 0 : NEED_TO_WRITE_BOM;
        }

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

            if (UTF16.isSurrogate(ch)) {
                cr = handleSurrogates(source, ch);
                if (cr != null)
                    return cr;

                char trail = UTF16.getTrailSurrogate(fromUChar32);
                fromUChar32 = 0;

                // 4 bytes
                temp[0 ^ endianXOR] = (byte) (ch >>> 8);
                temp[1 ^ endianXOR] = (byte) (ch);
                temp[2 ^ endianXOR] = (byte) (trail >>> 8);
                temp[3 ^ endianXOR] = (byte) (trail);
                cr = fromUWriteBytes(this, temp, 0, 4, target, offsets, sourceIndex);
            } else {
                // 2 bytes
                temp[0 ^ endianXOR] = (byte) (ch >>> 8);
                temp[1 ^ endianXOR] = (byte) (ch);
                cr = fromUWriteBytes(this, temp, 0, 2, target, offsets, sourceIndex);
            }
            return (cr.isUnderflow() ? null : cr);
        }
    }

    public CharsetDecoder newDecoder() {
        return new CharsetDecoderUTF16(this);
    }

    public CharsetEncoder newEncoder() {
        return new CharsetEncoderUTF16(this);
    }
    
    void getUnicodeSetImpl( UnicodeSet setFillIn, int which){
        getNonSurrogateUnicodeSet(setFillIn);            
    }
}
