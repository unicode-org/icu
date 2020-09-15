// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2008-2015, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.charset;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.UnsupportedCharsetException;

import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

class CharsetHZ extends CharsetICU {

    private static final int UCNV_TILDE = 0x7E; /* ~ */
    private static final int UCNV_OPEN_BRACE = 0x7B; /* { */
    private static final int UCNV_CLOSE_BRACE = 0x7D; /* } */
    private static final byte[] SB_ESCAPE = new byte[] { 0x7E, 0x7D };
    private static final byte[] DB_ESCAPE = new byte[] { 0x7E, 0x7B };
    private static final byte[] TILDE_ESCAPE = new byte[] { 0x7E, 0x7E };
    private static final byte[] fromUSubstitution = new byte[] { (byte) 0x1A };

    private CharsetMBCS gbCharset;
    private boolean isEmptySegment;

    public CharsetHZ(String icuCanonicalName, String canonicalName, String[] aliases) {
        super(icuCanonicalName, canonicalName, aliases);
        gbCharset = (CharsetMBCS) new CharsetProviderICU().charsetForName("GBK");
        if (gbCharset == null) {
            throw new UnsupportedCharsetException("unable to open ICU GBK Charset, required for HZ");
        }

        maxBytesPerChar = 4;
        minBytesPerChar = 1;
        maxCharsPerByte = 1;

        isEmptySegment = false;
    }

    class CharsetDecoderHZ extends CharsetDecoderICU {
        CharsetMBCS.CharsetDecoderMBCS gbDecoder;
        boolean isStateDBCS = false;

        public CharsetDecoderHZ(CharsetICU cs) {
            super(cs);
            gbDecoder = (CharsetMBCS.CharsetDecoderMBCS) gbCharset.newDecoder();
        }

        @Override
        protected void implReset() {
            super.implReset();
            gbDecoder.implReset();

            isStateDBCS = false;
            isEmptySegment = false;
        }

        @Override
        protected CoderResult decodeLoop(ByteBuffer source, CharBuffer target, IntBuffer offsets, boolean flush) {
            CoderResult err = CoderResult.UNDERFLOW;
            byte[] tempBuf = new byte[2];
            int targetUniChar = 0;
            int mySourceChar = 0;

            if (!source.hasRemaining())
                return CoderResult.UNDERFLOW;
            else if (!target.hasRemaining())
                return CoderResult.OVERFLOW;

            while (source.hasRemaining()) {

                if (target.hasRemaining()) {

                    // get the byte as unsigned
                    mySourceChar = source.get() & 0xff;

                    if (mode == UCNV_TILDE) {
                        /* second byte after ~ */
                        mode = 0;
                        switch (mySourceChar) {
                        case 0x0A:
                            /* no output for ~\n (line-continuation marker) */
                            continue;
                        case UCNV_TILDE:
                            if (offsets != null) {
                                offsets.put(source.position() - 2);
                            }
                            target.put((char) mySourceChar);
                            continue;
                        case UCNV_OPEN_BRACE:
                        case UCNV_CLOSE_BRACE:
                            isStateDBCS = (mySourceChar == UCNV_OPEN_BRACE);
                            if (isEmptySegment) {
                                isEmptySegment = false; /* we are handling it, reset to avoid future spurious errors */
                                this.toUBytesArray[0] = UCNV_TILDE;
                                this.toUBytesArray[1] = (byte)mySourceChar;
                                this.toULength = 2;
                                return CoderResult.malformedForLength(1);
                            }
                            isEmptySegment = true;
                            continue;
                        default:
                            /*
                             * if the first byte is equal to TILDE and the trail byte is not a valid byte then it is an
                             * error condition
                             */
                            /*
                             * Ticket 5691: consistent illegal sequences:
                             * - We include at least the first byte in the illegal sequence.
                             * - If any of the non-initial bytes could be the start of a character,
                             *   we stop the illegal sequence before the first one of those.
                             */
                            isEmptySegment = false; /* different error here, reset this to avoid spurious furture error */
                            err = CoderResult.malformedForLength(1);
                            toUBytesArray[0] = UCNV_TILDE;
                            if (isStateDBCS ? (0x21 <= mySourceChar && mySourceChar <= 0x7e) : mySourceChar <= 0x7f) {
                                /* The current byte could be the start of a character: Back it out. */
                                toULength = 1;
                                source.position(source.position() - 1);
                            } else {
                                /* Include the current byte in the illegal sequence. */
                                toUBytesArray[1] = (byte)mySourceChar;
                                toULength = 2;
                            }
                            return err;
                        }
                    } else if (isStateDBCS) {
                        if (toUnicodeStatus == 0) {
                            /* lead byte */
                            if (mySourceChar == UCNV_TILDE) {
                                mode = UCNV_TILDE;
                            } else {
                                /*
                                 * add another bit to distinguish a 0 byte from not having seen a lead byte
                                 */
                                toUnicodeStatus = mySourceChar | 0x100;
                                isEmptySegment = false; /* the segment has something, either valid or will produce a different error, so reset this */
                            }
                            continue;
                        } else {
                            /* trail byte */
                            boolean leadIsOk, trailIsOk;
                            int leadByte = toUnicodeStatus & 0xff;
                            targetUniChar = 0xffff;
                            /*
                             * Ticket 5691: consistent illegal sequence
                             * - We include at least the first byte in the illegal sequence.
                             * - If any of the non-initial bytes could be the start of a character,
                             *   we stop the illegal sequence before the first one of those
                             *
                             * In HZ DBCS, if the second byte is in the 21..7e range,
                             * we report ony the first byte as the illegal sequence.
                             * Otherwise we convert of report the pair of bytes.
                             */
                            leadIsOk = (short)(UConverterConstants.UNSIGNED_BYTE_MASK & (leadByte - 0x21)) <= (0x7d - 0x21);
                            trailIsOk = (short)(UConverterConstants.UNSIGNED_BYTE_MASK & (mySourceChar - 0x21)) <= (0x7e - 0x21);
                            if (leadIsOk && trailIsOk) {
                                tempBuf[0] = (byte)(leadByte + 0x80);
                                tempBuf[1] = (byte)(mySourceChar + 0x80);
                                targetUniChar = gbDecoder.simpleGetNextUChar(ByteBuffer.wrap(tempBuf), super.isFallbackUsed());
                                mySourceChar = (leadByte << 8) | mySourceChar;
                            } else if (trailIsOk) {
                                /* report a single illegal byte and continue with the following DBCS starter byte */
                                source.position(source.position() - 1);
                                mySourceChar = leadByte;
                            } else {
                                /* report a pair of illegal bytes if the second byte is not a DBCS starter */
                                /* add another bit so that the code below writes 2 bytes in case of error */
                                mySourceChar = 0x10000 | (leadByte << 8) | mySourceChar;
                            }
                            toUnicodeStatus = 0x00;
                        }
                    } else {
                        if (mySourceChar == UCNV_TILDE) {
                            mode = UCNV_TILDE;
                            continue;
                        } else if (mySourceChar <= 0x7f) {
                            targetUniChar = mySourceChar; /* ASCII */
                            isEmptySegment = false; /* the segment has something valid */
                        } else {
                            targetUniChar = 0xffff;
                            isEmptySegment = false; /* different error here, reset this to avoid spurious future error */
                        }
                    }

                    if (targetUniChar < 0xfffe) {
                        if (offsets != null) {
                            offsets.put(source.position() - 1 - (isStateDBCS ? 1 : 0));
                        }

                        target.put((char) targetUniChar);
                    } else /* targetUniChar >= 0xfffe */{
                        if (mySourceChar > 0xff) {
                            toUBytesArray[toUBytesBegin + 0] = (byte) (mySourceChar >> 8);
                            toUBytesArray[toUBytesBegin + 1] = (byte) mySourceChar;
                            toULength = 2;
                        } else {
                            toUBytesArray[toUBytesBegin + 0] = (byte) mySourceChar;
                            toULength = 1;
                        }
                        if (targetUniChar == 0xfffe) {
                            return CoderResult.unmappableForLength(toULength);
                        } else {
                            return CoderResult.malformedForLength(toULength);
                        }
                    }
                } else {
                    return CoderResult.OVERFLOW;
                }
            }

            return err;
        }
    }

    class CharsetEncoderHZ extends CharsetEncoderICU {
        CharsetMBCS.CharsetEncoderMBCS gbEncoder;
        boolean isEscapeAppended = false;
        boolean isTargetUCharDBCS = false;

        public CharsetEncoderHZ(CharsetICU cs) {
            super(cs, fromUSubstitution);
            gbEncoder = (CharsetMBCS.CharsetEncoderMBCS) gbCharset.newEncoder();
        }

        @Override
        protected void implReset() {
            super.implReset();
            gbEncoder.implReset();

            isEscapeAppended = false;
            isTargetUCharDBCS = false;
        }

        @Override
        protected CoderResult encodeLoop(CharBuffer source, ByteBuffer target, IntBuffer offsets, boolean flush) {
            int length = 0;
            int[] targetUniChar = new int[] { 0 };
            int mySourceChar = 0;
            boolean oldIsTargetUCharDBCS = isTargetUCharDBCS;

            if (!source.hasRemaining())
                return CoderResult.UNDERFLOW;
            else if (!target.hasRemaining())
                return CoderResult.OVERFLOW;

            if (fromUChar32 != 0 && target.hasRemaining()) {
                CoderResult cr = handleSurrogates(source, (char) fromUChar32);
                return (cr != null) ? cr : CoderResult.unmappableForLength(2);
            }
            /* writing the char to the output stream */
            while (source.hasRemaining()) {
                targetUniChar[0] = MISSING_CHAR_MARKER;
                if (target.hasRemaining()) {

                    mySourceChar = source.get();

                    oldIsTargetUCharDBCS = isTargetUCharDBCS;
                    if (mySourceChar == UCNV_TILDE) {
                        /*
                         * concatEscape(args, &myTargetIndex, &targetLength,"\x7E\x7E",err,2,&mySourceIndex);
                         */
                        concatEscape(source, target, offsets, TILDE_ESCAPE);
                        continue;
                    } else if (mySourceChar <= 0x7f) {
                        length = 1;
                        targetUniChar[0] = mySourceChar;
                    } else {
                        length = gbEncoder.fromUChar32(mySourceChar, targetUniChar, super.isFallbackUsed());

                        /*
                         * we can only use lead bytes 21..7D and trail bytes 21..7E
                         */
                        if (length == 2 && 0xa1a1 <= targetUniChar[0] && targetUniChar[0] <= 0xfdfe
                                && 0xa1 <= (targetUniChar[0] & 0xff) && (targetUniChar[0] & 0xff) <= 0xfe) {
                            targetUniChar[0] -= 0x8080;
                        } else {
                            targetUniChar[0] = MISSING_CHAR_MARKER;
                        }
                    }
                    if (targetUniChar[0] != MISSING_CHAR_MARKER) {
                        isTargetUCharDBCS = (targetUniChar[0] > 0x00FF);
                        if (oldIsTargetUCharDBCS != isTargetUCharDBCS || !isEscapeAppended) {
                            /* Shifting from a double byte to single byte mode */
                            if (!isTargetUCharDBCS) {
                                concatEscape(source, target, offsets, SB_ESCAPE);
                                isEscapeAppended = true;
                            } else { /*
                                         * Shifting from a single byte to double byte mode
                                         */
                                concatEscape(source, target, offsets, DB_ESCAPE);
                                isEscapeAppended = true;

                            }
                        }

                        if (isTargetUCharDBCS) {
                            if (target.hasRemaining()) {
                                target.put((byte) (targetUniChar[0] >> 8));
                                if (offsets != null) {
                                    offsets.put(source.position() - 1);
                                }
                                if (target.hasRemaining()) {
                                    target.put((byte) targetUniChar[0]);
                                    if (offsets != null) {
                                        offsets.put(source.position() - 1);
                                    }
                                } else {
                                    errorBuffer[errorBufferLength++] = (byte) targetUniChar[0];
                                    // *err = U_BUFFER_OVERFLOW_ERROR;
                                }
                            } else {
                                errorBuffer[errorBufferLength++] = (byte) (targetUniChar[0] >> 8);
                                errorBuffer[errorBufferLength++] = (byte) targetUniChar[0];
                                // *err = U_BUFFER_OVERFLOW_ERROR;
                            }

                        } else {
                            if (target.hasRemaining()) {
                                target.put((byte) targetUniChar[0]);
                                if (offsets != null) {
                                    offsets.put(source.position() - 1);
                                }

                            } else {
                                errorBuffer[errorBufferLength++] = (byte) targetUniChar[0];
                                // *err = U_BUFFER_OVERFLOW_ERROR;
                            }
                        }

                    } else {
                        /* oops.. the code point is unassigned */
                        /* Handle surrogates */
                        /* check if the char is a First surrogate */

                        if (UTF16.isSurrogate((char) mySourceChar)) {
                            // use that handy handleSurrogates method everyone's been talking about!
                            CoderResult cr = handleSurrogates(source, (char) mySourceChar);
                            return (cr != null) ? cr : CoderResult.unmappableForLength(2);
                        } else {
                            /* callback(unassigned) for a BMP code point */
                            // *err = U_INVALID_CHAR_FOUND;
                            fromUChar32 = mySourceChar;
                            return CoderResult.unmappableForLength(1);
                        }
                    }
                } else {
                    // *err = U_BUFFER_OVERFLOW_ERROR;
                    return CoderResult.OVERFLOW;
                }
            }

            return CoderResult.UNDERFLOW;
        }

        private CoderResult concatEscape(CharBuffer source, ByteBuffer target, IntBuffer offsets, byte[] strToAppend) {
            CoderResult cr = null;
            for (int i=0; i<strToAppend.length; i++) {
                byte b = strToAppend[i];
                if (target.hasRemaining()) {
                    target.put(b);
                    if (offsets != null)
                        offsets.put(source.position() - 1);
                } else {
                    errorBuffer[errorBufferLength++] = b;
                    cr = CoderResult.OVERFLOW;
                }
            }
            return cr;
        }
    }

    @Override
    public CharsetDecoder newDecoder() {
        return new CharsetDecoderHZ(this);
    }

    @Override
    public CharsetEncoder newEncoder() {
        return new CharsetEncoderHZ(this);
    }

    @Override
    void getUnicodeSetImpl( UnicodeSet setFillIn, int which){
        setFillIn.add(0,0x7f);
       // CharsetMBCS mbcshz = (CharsetMBCS)CharsetICU.forNameICU("icu-internal-25546");
        gbCharset.MBCSGetFilteredUnicodeSetForUnicode(gbCharset.sharedData, setFillIn, which, CharsetMBCS.UCNV_SET_FILTER_HZ);
    }
}
