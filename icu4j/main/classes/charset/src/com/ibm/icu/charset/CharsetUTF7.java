// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2007-2011, International Business Machines Corporation and         *
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

import com.ibm.icu.text.UnicodeSet;

/**
 * @author Michael Ow
 *
 */
class CharsetUTF7 extends CharsetICU {
    private final static String IMAP_NAME="IMAP-mailbox-name";
    private boolean useIMAP;
    protected byte[] fromUSubstitution=new byte[]{0x3F};

    public CharsetUTF7(String icuCanonicalName, String javaCanonicalName, String[] aliases) {
        super(icuCanonicalName, javaCanonicalName, aliases);
        maxBytesPerChar=5; /* max 3 bytes per code unit from UTF-7 (base64) plus SIN SOUT */
        minBytesPerChar=1;
        maxCharsPerByte=1;

        useIMAP=false;

        if (icuCanonicalName.equals(IMAP_NAME)) {
            useIMAP=true;
        }
    }

    //private static boolean inSetD(char c) {
    //    return (
    //            (char)(c - 97) < 26 || (char)(c - 65) < 26 || /* letters */
    //            (char)(c - 48) < 10 ||                        /* digits */
    //            (char)(c - 39) < 3 ||                          /* ' () */
    //            (char)(c - 44) < 4 ||                          /* ,-./ */
    //            (c==58) || (c==63)            /* :? */
    //            );
    //}

    //private static boolean inSetO(char c) {
    //    return (
    //            (char)(c - 33) < 6 ||                           /* !"#$%& */
    //            (char)(c - 59) < 4 ||                           /* ;<=> */
    //            (char)(c - 93) < 4 ||                           /* ]^_` */
    //            (char)(c - 123) < 3 ||                         /* {|} */
    //            (c==58) || (c==63)             /* *@[ */
    //            );
    //}

    private static boolean isCRLFTAB(char c) {
        return (
                (c==13) || (c==10) || (c==9)
                );
    }

    //private static boolean isCRLFSPTAB(char c) {
    //   return (
    //            (c==32) || (c==13) || (c==10) || (c==9)
    //            );
    //}

    private static final byte PLUS=43;
    private static final byte MINUS=45;
    private static final byte BACKSLASH=92;
    //private static final byte TILDE=126;
    private static final byte AMPERSAND=0x26;
    private static final byte COMMA=0x2c;
    private static final byte SLASH=0x2f;

    // legal byte values: all US-ASCII graphic characters 0x20..0x7e
    private static boolean isLegal(char c, boolean useIMAP) {
        if (useIMAP) {
            return (
                    (0x20 <= c) && (c <= 0x7e)
                    );
        } else {
            return (
                    ((char)(c - 32) < 94 && (c != BACKSLASH)) || isCRLFTAB(c)
                    );
        }
    }

    // directly encode all of printable ASCII 0x20..0x7e except '&' 0x26
    private static boolean inSetDIMAP(char c) {
        return (
                (isLegal(c, true) && c != AMPERSAND)
                );
    }

    private static byte TO_BASE64_IMAP(int n) {
        return (n < 63 ? TO_BASE_64[n] : COMMA);
    }

    private static byte FROM_BASE64_IMAP(char c) {
        return (c==COMMA ? 63 : c==SLASH ? -1 : FROM_BASE_64[c]);
    }

    /* encode directly sets D and O and CR LF SP TAB */
    private static final byte ENCODE_DIRECTLY_MAXIMUM[] =
    {
     /*0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f*/
        0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,

        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,

        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1,

        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0
    };

    /* encode directly set D and CR LF SP TAB but not set O */
    private static final byte ENCODE_DIRECTLY_RESTRICTED[] =
    {
     /*0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f*/
        0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,

        1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 1,

        0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0,

        0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0
    };

    private static final byte TO_BASE_64[] =
    {
       /* A-Z */
       65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77,
       78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90,
       /* a-z */
       97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109,
       110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122,
       /* 0-9 */
       48, 49, 50, 51, 52, 53, 54, 55, 56, 57,
       /* +/ */
       43, 47
    };

    private static final byte FROM_BASE_64[] =
    {
       /* C0 controls, -1 for legal ones (CR LF TAB), -3 for illegal ones */
       -3, -3, -3, -3, -3, -3, -3, -3, -3, -1, -1, -3, -3, -1, -3, -3,
       -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3,
       /* general punctuation with + and / and a special value (-2) for - */
       -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -2, -1, 63,
       /* digits */
       52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1, -1,
       /* A-Z */
       -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
       15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -3, -1, -1, -1,
       /* a-z*/
       -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
       41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, -1, -1, -1, -3, -3
    };

    class CharsetDecoderUTF7 extends CharsetDecoderICU {
        public CharsetDecoderUTF7(CharsetICU cs) {
            super(cs);
            implReset();
        }

        @Override
        protected void implReset() {
            super.implReset();
            toUnicodeStatus=(toUnicodeStatus & 0xf0000000) | 0x1000000;
        }

        @Override
        protected CoderResult decodeLoop(ByteBuffer source, CharBuffer target, IntBuffer offsets, boolean flush) {
            CoderResult cr=CoderResult.UNDERFLOW;
            byte base64Value;
            byte base64Counter;
            byte inDirectMode;
            char bits;
            int byteIndex;
            int sourceIndex, nextSourceIndex;

            int length;

            char b;
            char c;

            int sourceArrayIndex=source.position();

            //get the state of the machine state
            {
            int status=toUnicodeStatus;
            inDirectMode=(byte)((status >> 24) & 1);
            base64Counter=(byte)(status >> 16);
            bits=(char)status;
            }
            byteIndex=toULength;
            /* sourceIndex=-1 if the current character began in the previous buffer */
            sourceIndex=byteIndex==0 ? 0 : -1;
            nextSourceIndex=0;

            directMode:  while (true) {
                if (inDirectMode==1) {
                    /*
                     * In Direct Mode, most US-ASCII characters are encoded directly, i.e.,
                     * with their US-ASCII byte values.
                     * Backslash and Tilde and most control characters are not called in UTF-7.
                     * A plus sign starts Unicode (or "escape") Mode.
                     * An ampersand starts Unicode Mode for IMAP.
                     *
                     * In Direct Mode, only the sourceIndex is used.
                     */
                    byteIndex=0;
                    length=source.remaining();
                    //targetCapacity=target.remaining();
                    //Commented out because length of source may be larger than target when it comes to bytes
                    /*if (useIMAP && length > targetCapacity) {
                        length=targetCapacity;
                    }*/
                    while (length > 0) {
                        b=(char)(source.get());
                        sourceArrayIndex++;
                        if (!isLegal(b, useIMAP)) {
                            toUBytesArray[0]=(byte)b;
                            byteIndex=1;
                            cr=CoderResult.malformedForLength(sourceArrayIndex);
                            break;
                        } else if ((!useIMAP && b!=PLUS) || (useIMAP && b!=AMPERSAND)) {
                            // write directly encoded character
                            if (target.hasRemaining()) { // Check to make sure that there is room in target.
                                target.put(b);
                                if (offsets!= null) {
                                    offsets.put(sourceIndex++);
                                }
                            } else {  // Get out and set the CoderResult.
                                charErrorBufferArray[charErrorBufferLength++] = b;
                                cr = CoderResult.OVERFLOW;
                                break;
                            }
                        } else { /* PLUS or (AMPERSAND in IMAP)*/
                            /* switch to Unicode mode */
                            nextSourceIndex=++sourceIndex;
                            inDirectMode=0;
                            byteIndex=0;
                            bits=0;
                            base64Counter=-1;
                            continue directMode;
                        }
                        --length;
                    }//end of while
                    if (source.hasRemaining() && target.position() >= target.limit()) {
                        /* target is full */
                        cr=CoderResult.OVERFLOW;
                    }
                    break directMode;
                } else { /* Unicode Mode*/
                    /*
                     * In Unicode Mode, UTF-16BE is base64-encoded.
                     * The base64 sequence ends with any character that is not in the base64 alphabet.
                     * A terminating minus sign is consumed.
                     *
                     * In Unicode Mode, the sourceIndex has the index to the start of the current
                     * base64 bytes, while nextSourceIndex is precisely parallel to source,
                     * keeping the index to the following byte.
                     */
                    while(source.hasRemaining()) {
                        if (target.hasRemaining()) {
                            b=(char)source.get();
                            sourceArrayIndex++;
                            toUBytesArray[byteIndex++]=(byte)b;
                            base64Value = -3; /* initialize as illegal */
                            if ((!useIMAP && (b>=126 || (base64Value=FROM_BASE_64[b])==-3 || base64Value==-1)) || (useIMAP && b>0x7e)) {
                                /* either
                                 * base64Value==-1 for any legal character except base64 and minus sign, or
                                 * base64Value==-3 for illegal characters:
                                 * 1. In either case, leave Unicode mode.
                                 * 2.1. If we ended with an incomplete UChar or none after the +, then
                                 *      generate an error for the preceding erroneous sequence and deal with
                                 *      the current (possibly illegal) character next time through.
                                 * 2.2. Else the current char comes after a complete UChar, which was already
                                 *      pushed to the output buf, so:
                                 * 2.2.1. If the current char is legal, just save it for processing next time.
                                 *        It may be for example, a plus which we need to deal with in direct mode.
                                 * 2.2.2. Else if the current char is illegal, we might as well deal with it here.
                                 */
                                inDirectMode=1;

                                if(base64Counter==-1) {
                                    /* illegal: + immediately followed by something other than base64 or minus sign */
                                    /* include the plus sign in the reported sequence, but not the subsequent char */
                                    source.position(source.position() -1);
                                    toUBytesArray[0]=PLUS;
                                    byteIndex=1;
                                    cr=CoderResult.malformedForLength(sourceArrayIndex);
                                    break directMode;
                                } else if(bits!=0) {
                                    /* bits are illegally left over, a UChar is incomplete */
                                    /* don't include current char (legal or illegal) in error seq */
                                    source.position(source.position() -1);
                                    --byteIndex;
                                    cr=CoderResult.malformedForLength(sourceArrayIndex);
                                    break directMode;
                                } else {
                                    /* previous UChar was complete */
                                    if(base64Value==-3) {
                                        /* current character is illegal, deal with it here */
                                        cr=CoderResult.malformedForLength(sourceArrayIndex);
                                        break directMode;
                                    } else {
                                        /* un-read the current character in case it is a plus sign */
                                        source.position(source.position() -1);
                                        sourceIndex=nextSourceIndex-1;
                                        continue directMode;
                                    }
                                }
                            } else if ((!useIMAP && (base64Value=FROM_BASE_64[b])>=0) || (useIMAP && (base64Value=FROM_BASE64_IMAP(b))>=0)) {
                                /* collect base64 bytes */
                                switch (base64Counter) {
                                case -1: /* -1 is immediately after the + */
                                case 0:
                                    bits=(char)base64Value;
                                    base64Counter=1;
                                    break;
                                case 1:
                                case 3:
                                case 4:
                                case 6:
                                    bits=(char)((bits<<6) | base64Value);
                                    ++base64Counter;
                                    break;
                                case 2:
                                    c=(char)((bits<<4) | (base64Value>>2));
                                    if (useIMAP && isLegal(c, useIMAP)) {
                                        // illegal
                                        inDirectMode=1;
                                        cr=CoderResult.malformedForLength(sourceArrayIndex);
                                        // goto endloop;
                                        break directMode;
                                    }
                                    target.put(c);
                                    if (offsets != null) {
                                        offsets.put(sourceIndex);
                                        sourceIndex=nextSourceIndex - 1;
                                    }
                                    toUBytesArray[0]=(byte)b; /* keep this byte in case an error occurs */
                                    byteIndex=1;
                                    bits=(char)(base64Value&3);
                                    base64Counter=3;
                                    break;
                                case 5:
                                    c=(char)((bits<<2) | (base64Value>>4));
                                    if(useIMAP && isLegal(c, useIMAP)) {
                                        // illegal
                                        inDirectMode=1;
                                        cr=CoderResult.malformedForLength(sourceArrayIndex);
                                        // goto endloop;
                                        break directMode;
                                    }
                                    target.put(c);
                                    if (offsets != null) {
                                        offsets.put(sourceIndex);
                                        sourceIndex=nextSourceIndex - 1;
                                    }
                                    toUBytesArray[0]=(byte)b; /* keep this byte in case an error occurs */
                                    byteIndex=1;
                                    bits=(char)(base64Value&15);
                                    base64Counter=6;
                                    break;
                                case 7:
                                    c=(char)((bits<<6) | base64Value);
                                    if (useIMAP && isLegal(c, useIMAP)) {
                                        // illegal
                                        inDirectMode=1;
                                        cr=CoderResult.malformedForLength(sourceArrayIndex);
                                        // goto endloop;
                                        break directMode;
                                    }
                                    target.put(c);
                                    if (offsets != null) {
                                        offsets.put(sourceIndex);
                                        sourceIndex=nextSourceIndex;
                                    }
                                    byteIndex=0;
                                    bits=0;
                                    base64Counter=0;
                                    break;
                                //default:
                                    /* will never occur */
                                    //break;
                                }//end of switch
                            } else if (!useIMAP || (useIMAP && base64Value==-2)) {
                                /* minus sign terminates the base64 sequence */
                                inDirectMode=1;
                                if (base64Counter==-1) {
                                    /* +- i.e. a minus immediately following a plus */
                                    target.put(useIMAP ? (char)AMPERSAND : (char)PLUS);
                                    if (offsets != null) {
                                        offsets.put(sourceIndex - 1);
                                    }
                                } else {
                                    /* absorb the minus and leave the Unicode Mode */
                                    if (bits!=0 || (useIMAP && base64Counter!=0 && base64Counter!=3 && base64Counter!=6)) {
                                        /*bits are illegally left over, a unicode character is incomplete */
                                        cr=CoderResult.malformedForLength(sourceArrayIndex);
                                        break;
                                    }
                                }
                                sourceIndex=nextSourceIndex;
                                continue directMode;
                            } else if (useIMAP) {
                                if (base64Counter==-1) {
                                    // illegal: & immediately followed by something other than base64 or minus sign
                                    // include the ampersand in the reported sequence
                                    --sourceIndex;
                                    toUBytesArray[0]=AMPERSAND;
                                    toUBytesArray[1]=(byte)b;
                                    byteIndex=2;
                                }
                                /* base64Value==-3 for illegal characters */
                                /* illegal */
                                inDirectMode=1;
                                cr=CoderResult.malformedForLength(sourceArrayIndex);
                                break;
                            }
                        } else {
                            /* target is full */
                            cr=CoderResult.OVERFLOW;
                            break;
                        }
                    } //end of while
                    break directMode;
                }
            }//end of direct mode label
            if (useIMAP) {
                if (!cr.isError() && inDirectMode==0 && flush && byteIndex==0 && !source.hasRemaining()) {
                    if (base64Counter==-1) {
                        /* & at the very end of the input */
                        /* make the ampersand the reported sequence */
                        toUBytesArray[0]=AMPERSAND;
                        byteIndex=1;
                    }
                    /* else if (base64Counter!=-1) byteIndex remains 0 because there is no particular byte sequence */
                    inDirectMode=1;
                    cr=CoderResult.malformedForLength(sourceIndex);
                }

            } else {
                if (!cr.isError() && flush && !source.hasRemaining() && bits  ==0) {
                    /*
                     * if we are in Unicode Mode, then the byteIndex might not be 0,
                     * but that is ok if bits -- 0
                     * -> we set byteIndex=0 at the end of the stream to avoid a truncated error
                     * (not true for IMAP-mailbox-name where we must end in direct mode)
                     */
                    if (!cr.isOverflow()) {
                        byteIndex=0;
                    }
                }
            }
            /* set the converter state */
            toUnicodeStatus=(inDirectMode<<24 | ((base64Counter & UConverterConstants.UNSIGNED_BYTE_MASK)<<16) | bits);
            toULength=byteIndex;

            return cr;
        }
    }

    class CharsetEncoderUTF7 extends CharsetEncoderICU {
        public CharsetEncoderUTF7(CharsetICU cs) {
            super(cs, fromUSubstitution);
            implReset();
        }

        @Override
        protected void implReset() {
            super.implReset();
            fromUnicodeStatus=(fromUnicodeStatus & 0xf0000000) | 0x1000000;
        }

        @Override
        protected CoderResult encodeLoop(CharBuffer source, ByteBuffer target, IntBuffer offsets, boolean flush) {
            CoderResult cr=CoderResult.UNDERFLOW;
            byte inDirectMode;
            byte encodeDirectly[];
            int status;

            int length, targetCapacity, sourceIndex;

            byte base64Counter;
            char bits;
            char c;
            char b;
            /* get the state machine state */
            {
                status=fromUnicodeStatus;
                encodeDirectly=(((long)status) < 0x10000000) ? ENCODE_DIRECTLY_MAXIMUM : ENCODE_DIRECTLY_RESTRICTED;
                inDirectMode=(byte)((status >> 24) & 1);
                base64Counter=(byte)(status >> 16);
                bits=(char)((byte)status);
            }
            /* UTF-7 always encodes UTF-16 code units, therefore we need only a simple sourceIndex */
            sourceIndex=0;

            directMode: while(true) {
            if(inDirectMode==1) {
                length=source.remaining();
                targetCapacity=target.remaining();
                if(length > targetCapacity) {
                    length=targetCapacity;
                }
                while (length > 0) {
                    c=source.get();
                    /* UTF7: currently always encode CR LF SP TAB directly */
                    /* IMAP: encode 0x20..0x7e except '&' directly */
                    if ((!useIMAP && c<=127 && encodeDirectly[c]==1) || (useIMAP && inSetDIMAP(c))) {
                        /* encode directly */
                        target.put((byte)c);
                        if (offsets != null) {
                            offsets.put(sourceIndex++);
                        }
                    } else if ((!useIMAP && c==PLUS) || (useIMAP && c==AMPERSAND)) {
                        /* IMAP: output &- for & */
                        /* UTF-7: output +- for + */
                        target.put(useIMAP ? AMPERSAND : PLUS);
                        if (target.hasRemaining()) {
                            target.put(MINUS);
                            if (offsets != null) {
                                offsets.put(sourceIndex);
                                offsets.put(sourceIndex++);
                            }
                            /* realign length and targetCapacity */
                            continue directMode;
                        } else {
                            if (offsets != null) {
                                offsets.put(sourceIndex++);
                            }
                            errorBuffer[0]=MINUS;
                            errorBufferLength=1;
                            cr=CoderResult.OVERFLOW;
                            break;
                        }
                    } else {
                        /* un-read this character and switch to unicode mode */
                        source.position(source.position() - 1);
                        target.put(useIMAP ? AMPERSAND : PLUS);
                        if (offsets != null) {
                            offsets.put(sourceIndex);
                        }
                        inDirectMode=0;
                        base64Counter=0;
                        continue directMode;
                    }
                    --length;
                } //end of while
                if (source.hasRemaining() && !target.hasRemaining()) {
                    /* target is full */
                    cr=CoderResult.OVERFLOW;
                }
                break directMode;
            } else {
                /* Unicode Mode */
                while (source.hasRemaining()) {
                    if (target.hasRemaining()) {
                        c=source.get();
                        if ((!useIMAP && c<=127 && encodeDirectly[c]==1) || (useIMAP && isLegal(c, useIMAP))) {
                            /* encode directly */
                            inDirectMode=1;

                            /* trick: back out this character to make this easier */
                            source.position(source.position() - 1);

                            /* terminate the base64 sequence */
                            if (base64Counter!=0) {
                                /* write remaining bits for the previous character */
                                target.put(useIMAP ? TO_BASE64_IMAP(bits) : TO_BASE_64[bits]);
                                if (offsets!=null) {
                                    offsets.put(sourceIndex-1);
                                }
                            }
                            if (FROM_BASE_64[c]!=-1 || useIMAP) {
                                /* need to terminate with a minus */
                                if (target.hasRemaining()) {
                                    target.put(MINUS);
                                    if (offsets!=null) {
                                        offsets.put(sourceIndex-1);
                                    }
                                } else {
                                    errorBuffer[0]=MINUS;
                                    errorBufferLength=1;
                                    cr=CoderResult.OVERFLOW;
                                    break;
                                }
                            }
                            continue directMode;
                        } else {
                            /*
                             * base64 this character:
                             * Output 2 or 3 base64 bytres for the remaining bits of the previous character
                             * and the bits of this character, each implicitly in UTF-16BE.
                             *
                             * Here, bits is an 8-bit variable because only 6 bits need to be kept from one
                             * character to the next.  The actual 2 or 4 bits are shifted to the left edge
                             * of the 6-bits filed 5..0 to make the termination of the base64 sequence easier.
                             */
                            switch (base64Counter) {
                            case 0:
                                b=(char)(c>>10);
                                target.put(useIMAP ? TO_BASE64_IMAP(b) : TO_BASE_64[b]);
                                if (target.hasRemaining()) {
                                    b=(char)((c>>4)&0x3f);
                                    target.put(useIMAP ? TO_BASE64_IMAP(b) : TO_BASE_64[b]);
                                    if (offsets!=null) {
                                        offsets.put(sourceIndex);
                                        offsets.put(sourceIndex++);
                                    }
                                } else {
                                    if (offsets!=null) {
                                        offsets.put(sourceIndex++);
                                    }
                                    b=(char)((c>>4)&0x3f);
                                    errorBuffer[0]=useIMAP ? TO_BASE64_IMAP(b) : TO_BASE_64[b];
                                    errorBufferLength=1;
                                    cr=CoderResult.OVERFLOW;
                                }
                                bits=(char)((c&15)<<2);
                                base64Counter=1;
                                break;
                            case 1:
                                b=(char)(bits|(c>>14));
                                target.put(useIMAP ? TO_BASE64_IMAP(b) : TO_BASE_64[b]);
                                if (target.hasRemaining()) {
                                    b=(char)((c>>8)&0x3f);
                                    target.put(useIMAP ? TO_BASE64_IMAP(b) : TO_BASE_64[b]);
                                    if (target.hasRemaining()) {
                                        b=(char)((c>>2)&0x3f);
                                        target.put(useIMAP ? TO_BASE64_IMAP(b) : TO_BASE_64[b]);
                                        if (offsets!=null) {
                                            offsets.put(sourceIndex);
                                            offsets.put(sourceIndex);
                                            offsets.put(sourceIndex++);
                                        }
                                    } else {
                                        if (offsets!=null) {
                                            offsets.put(sourceIndex);
                                            offsets.put(sourceIndex++);
                                        }
                                        b=(char)((c>>2)&0x3f);
                                        errorBuffer[0]=useIMAP ? TO_BASE64_IMAP(b) : TO_BASE_64[b];
                                        errorBufferLength=1;
                                        cr=CoderResult.OVERFLOW;
                                    }
                                } else {
                                    if (offsets!=null) {
                                        offsets.put(sourceIndex++);
                                    }
                                    b=(char)((c>>8)&0x3f);
                                    errorBuffer[0]=useIMAP ? TO_BASE64_IMAP(b) : TO_BASE_64[b];
                                    b=(char)((c>>2)&0x3f);
                                    errorBuffer[1]=useIMAP ? TO_BASE64_IMAP(b) : TO_BASE_64[b];
                                    errorBufferLength=2;
                                    cr=CoderResult.OVERFLOW;
                                }
                                bits=(char)((c&3)<<4);
                                base64Counter=2;
                                break;
                            case 2:
                                b=(char)(bits|(c>>12));
                                target.put(useIMAP ? TO_BASE64_IMAP(b) : TO_BASE_64[b]);
                                if (target.hasRemaining()) {
                                    b=(char)((c>>6)&0x3f);
                                    target.put(useIMAP ? TO_BASE64_IMAP(b) : TO_BASE_64[b]);
                                    if (target.hasRemaining()) {
                                        b=(char)(c&0x3f);
                                        target.put(useIMAP ? TO_BASE64_IMAP(b) : TO_BASE_64[b]);
                                        if (offsets!=null) {
                                            offsets.put(sourceIndex);
                                            offsets.put(sourceIndex);
                                            offsets.put(sourceIndex++);
                                        }
                                    } else {
                                        if (offsets!=null) {
                                            offsets.put(sourceIndex);
                                            offsets.put(sourceIndex++);
                                        }
                                        b=(char)(c&0x3f);
                                        errorBuffer[0]=useIMAP ? TO_BASE64_IMAP(b) : TO_BASE_64[b];
                                        errorBufferLength=1;
                                        cr=CoderResult.OVERFLOW;
                                    }
                                } else {
                                    if (offsets!=null) {
                                        offsets.put(sourceIndex++);
                                    }
                                    b=(char)((c>>6)&0x3f);
                                    errorBuffer[0]=useIMAP ? TO_BASE64_IMAP(b) : TO_BASE_64[b];
                                    b=(char)(c&0x3f);
                                    errorBuffer[1]=useIMAP ? TO_BASE64_IMAP(b) : TO_BASE_64[b];
                                    errorBufferLength=2;
                                    cr=CoderResult.OVERFLOW;
                                }
                                bits=0;
                                base64Counter=0;
                                break;
                           //default:
                               /* will never occur */
                               //break;
                           } //end of switch
                        }
                    } else {
                        /* target is full */
                        cr=CoderResult.OVERFLOW;
                        break;
                    }
                } //end of while
                break directMode;
            }
            } //end of directMode label

            if (flush && !source.hasRemaining()) {
                /* flush remaining bits to the target */
                if (inDirectMode==0) {
                    if (base64Counter!=0) {
                        if (target.hasRemaining()) {
                            target.put(useIMAP ? TO_BASE64_IMAP(bits) : TO_BASE_64[bits]);
                            if (offsets!=null) {
                                offsets.put(sourceIndex - 1);
                            }
                        } else {
                            errorBuffer[errorBufferLength++]=useIMAP ? TO_BASE64_IMAP(bits) : TO_BASE_64[bits];
                            cr=CoderResult.OVERFLOW;
                        }
                    }

                    /* need to terminate with a minus */
                    if (target.hasRemaining()) {
                        target.put(MINUS);
                        if (offsets!=null) {
                            offsets.put(sourceIndex - 1);
                        }
                    } else {
                        errorBuffer[errorBufferLength++]=MINUS;
                        cr=CoderResult.OVERFLOW;
                    }
                }
                /*reset the state for the next conversion */
                fromUnicodeStatus=((status&0xf0000000) | 0x1000000); /* keep version, inDirectMode=true */
            } else {
                /* set the converter state back */
                fromUnicodeStatus=((status&0xf0000000) | (inDirectMode<<24) | ((base64Counter & UConverterConstants.UNSIGNED_BYTE_MASK)<<16) | (bits));
            }

            return cr;
        }
    }

    @Override
    public CharsetDecoder newDecoder() {
        return new CharsetDecoderUTF7(this);
    }

    @Override
    public CharsetEncoder newEncoder() {
        return new CharsetEncoderUTF7(this);
    }

    @Override
    void getUnicodeSetImpl( UnicodeSet setFillIn, int which){
        getCompleteUnicodeSet(setFillIn);
    }
}
