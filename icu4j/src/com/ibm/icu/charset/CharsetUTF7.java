/*
 *******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and         *
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

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UTF16;
/**
 * @author Michael Ow
 *
 */
class CharsetUTF7 extends CharsetICU {
    protected byte[] fromUSubstitution = null;
   
    public CharsetUTF7(String icuCanonicalName, String javaCanonicalName, String[] aliases) {
        super(icuCanonicalName, javaCanonicalName, aliases);
    }
    
    private static boolean inSetD(char c) {
        return (
                (char)(c - 97) < 26 || (char)(c -65) < 26 || /* letters */
                (char)(c - 48) < 10 ||                        /* digits */
                (char)(c - 39) < 3 ||                          /* ' () */
                (char)(c - 44) < 4 ||                          /* ,-./ */
                (c == 58) || (c == 63)            /* :? */
                );
    }
    
    private static boolean inSetO(char c) {
        return (
                (char)(c - 33) < 6 ||                           /* !"#$%& */
                (char)(c - 59) < 4 ||                           /* ;<=> */
                (char)(c - 93) < 4 ||                           /* ]^_` */
                (char)(c - 123) < 3 ||                         /* {|} */
                (c == 58) || (c == 63)             /* *@[ */
                );
    }
    
    private static boolean isCRLFTAB(char c) {
        return (
                (c == 13) || (c == 10) || (c == 9)
                );
    }
    
    private static boolean isCRLFSPTAB(char c) {
        return (
                (c == 32) || (c == 13) || (c == 10) || (c == 9)
                );
    }
    
    private static final  byte PLUS = 43;
    private static final byte MINUS = 45;
    private static final byte BACKSLASH = 92;
    private static final byte TILDE = 126;
    
    private static boolean isLegalUTF7(char c) {
        return (
                ((char)(c - 32) < 94 && (c != BACKSLASH)) || isCRLFTAB(c)
                );
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
    
    private static final short TO_BASE_64[] =
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
        }
        
        protected CoderResult decodeLoop(ByteBuffer source, CharBuffer target, IntBuffer offsets, boolean flush) {
            CoderResult cr = CoderResult.UNDERFLOW;
            byte base64Value;
            byte base64Counter;
            byte inDirectMode;
            char bits;
            int byteIndex;
            int sourceIndex, nextSourceIndex;
            
            int length, targetCapacity;;
            
            char b;
            
            int sourceArrayIndex = source.position();
            
            //get the state of the machine state
            {
            int status = toUnicodeStatus;
            inDirectMode = (byte)((status>>24)&1);
            base64Counter = (byte)(status>>16);
            bits = (char)status;
            }
            byteIndex = toULength;
            /* sourceIndex = -1 if the current character began in the previous buffer */
            sourceIndex = byteIndex == 0 ? 0 : -1;
            nextSourceIndex = 0;
            
            directMode:  while(true) {
            if (inDirectMode != 0) {
                    /* 
                     * In Direct Mode, most US-ASCII characters are encoded directly, i.e.,
                     * with their US-ASCII byte values.
                     * Backslash and Tilde and most control characters are not alled in UTF-7.
                     * A plus sign starts Unicode (or "escape") Mode.
                     * 
                     * In Direct Mode, only the sourceIndex is used.
                     */
                    byteIndex = 0;
                    length = source.remaining();
                    targetCapacity = target.remaining();
                    if (length > targetCapacity) {
                        length = targetCapacity;
                    }
                    while (length > 0) {
                        b = (char)(source.get(sourceArrayIndex++) & UConverterConstants.UNSIGNED_BYTE_MASK);
                        if (!isLegalUTF7(b)) {
                            cr = CoderResult.malformedForLength(sourceArrayIndex);
                            break;
                        } else if (b != PLUS) {
                            target.put(b);
                            offsets.put(sourceIndex++);
                        } else { /* PLUS */
                            /* switch to Unicode mode */
                            nextSourceIndex = ++sourceIndex;
                            inDirectMode = 0;
                            byteIndex = 0;
                            bits = 0;
                            base64Counter = -1;
                            continue directMode;
                        }
                        --length;
                    }//end of while
                    if (source.hasRemaining() && target.position() >= target.limit()) {
                        /* target is full */
                        cr = CoderResult.OVERFLOW;
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
                        b = (char)(source.get(sourceArrayIndex++) & UConverterConstants.UNSIGNED_BYTE_MASK);
                        toUBytesArray[byteIndex++] = (byte)b;
                        if (b >= 126) {
                            /* illegal - test other illegal US-ASCII values by base64Value==-3 */
                            inDirectMode = 1;
                            cr = CoderResult.malformedForLength(sourceArrayIndex);
                            break directMode;
                        } else if ((base64Value = FROM_BASE_64[b]) >= 0) {
                            /* collect base64 bytes */
                            switch (base64Counter) {
                            case -1: /* -1 is immediately after the + */
                            case 0:
                                bits = (char)base64Value;
                                base64Counter = 1;
                                break;
                            case 1:
                            case 3:
                            case 4:
                            case 6:
                                bits = (char)((bits<<6) | base64Value);
                                ++base64Counter;
                                break;
                            case 2:
                                target.put((char)((bits<<4) | (base64Value>>2)));
                                if (offsets != null) {
                                    offsets.put(sourceIndex);
                                    sourceIndex = nextSourceIndex - 1;
                                }
                                toUBytesArray[0] = (byte)b; /* keep this byte in case an error occurs */
                                byteIndex = 1;
                                bits = (char)(base64Value&3);
                                base64Counter = 3;
                                break;
                            case 5:
                                target.put((char)((bits<<6) | base64Value));
                                if (offsets != null) {
                                    offsets.put(sourceIndex);
                                    sourceIndex = nextSourceIndex - 1;
                                }
                                toUBytesArray[0] = (byte)b; /* keep this byte in case an error occurs */
                                byteIndex = 1;
                                bits = (char)(base64Value&15);
                                base64Counter = 6;
                                break;
                            case 7:
                                target.put((char)((bits<<6) | base64Value));
                                if (offsets != null) {
                                    offsets.put(sourceIndex);
                                    sourceIndex = nextSourceIndex;
                                }
                                byteIndex = 0;
                                bits = 0;
                                base64Counter = 0;
                                break;
                            default:
                                /* will never occur */
                                break;                                                           
                            }//end of switch
                        } else if (base64Value == -2) {
                            /* minus sign terminates the base64 sequence */
                            inDirectMode = 1;
                            if (base64Counter == -1) {
                                /* +- i.e. a minus immediately following a plus */
                                target.put((char)PLUS);
                                if (offsets != null) {
                                    offsets.put(sourceIndex - 1);
                                }
                            } else {
                                /* absorb the minus and leave the Unicode Mode */
                                if (bits != 0) {
                                    /*bits are illegally left over, a unicode character is incomplete */
                                    cr = CoderResult.malformedForLength(sourceArrayIndex);
                                    break;
                                }
                            }
                            sourceIndex = nextSourceIndex;
                            continue directMode;
                        } else if (base64Value == -1) { /* for any legal character except base64 and minus sign */
                            /* leave the Unicode Mode */
                            inDirectMode = 1;
                            if (base64Counter == -1) {
                                /* illegal:  + immediately followed by something other than base64 minus sign */
                                /* include the plus sign in the reported sequence */
                                --sourceIndex;
                                toUBytesArray[0] = (byte)PLUS;
                                toUBytesArray[1] = (byte)b;
                                byteIndex = 2;
                                cr = CoderResult.malformedForLength(sourceArrayIndex);
                                break;
                            } else if (bits == 0) {
                                /* un-read the character in case it is a plus sign */
                                --sourceArrayIndex;
                                sourceIndex = nextSourceIndex - 1;
                                continue directMode;
                            } else {
                                /* bits are illegally left over, a unicode character is incomplete */
                                cr = CoderResult.malformedForLength(sourceArrayIndex);
                                break;
                            }
                        } else { /* base64Value == -3 for illegal characters */
                            /* illegal */
                            inDirectMode = 1;
                            cr = CoderResult.malformedForLength(sourceArrayIndex);
                            break;
                        }
                    } else {
                        /* target is full */
                        cr = CoderResult.OVERFLOW;
                        break;
                    }
                } //end of while
            }
            }//end of direct mode label
            
            //TODO:  need to find a better method than cr.isUnderflow() to check for error
            if (cr.isUnderflow() && flush && !source.hasRemaining() && bits  ==0) {
                /*
                 * if we are in Unicode Mode, then the byteIndex might not be 0,
                 * but that is ok if bits -- 0
                 * -> we set byteIndex = 0 at the end of the stream to avoid a truncated error 
                 * (not true for IMAP-mailbox-name where we must end in direct mode)
                 */
                byteIndex = 0;
            }
            /* set the converter state */
            toUnicodeStatus = ((int)inDirectMode<<24) | ((int)((char)base64Counter)<<16) | (int)((long)bits & UConverterConstants.UNSIGNED_INT_MASK);
            toULength = byteIndex;
   
            return cr;
        }
    }
    
    class CharsetEncoderUTF7 extends CharsetEncoderICU {
        public CharsetEncoderUTF7(CharsetICU cs) {
            super(cs, fromUSubstitution);
            implReset();
        }
        
        protected void implReset() {
            super.implReset();
        }
        
        protected CoderResult encodeLoop(CharBuffer source, ByteBuffer target, IntBuffer offsets, boolean flush) {
            CoderResult cr = CoderResult.UNDERFLOW;
            
            return cr;
        }
    }
    
    public CharsetDecoder newDecoder() {
        return new CharsetDecoderUTF7(this);
    }
    
    public CharsetEncoder newEncoder() {
        return new CharsetEncoderUTF7(this);
    }
}
