/**
<<<<<<< .mine
 *******************************************************************************
 * Copyright (C) 2006, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 *******************************************************************************
 */
=======
*******************************************************************************
* Copyright (C) 2006-2007, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
*******************************************************************************
*/ 
>>>>>>> .r21670
package com.ibm.icu.charset;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

class Charset88591 extends CharsetASCII {
    public Charset88591(String icuCanonicalName, String javaCanonicalName,
            String[] aliases) {
        super(icuCanonicalName, javaCanonicalName, aliases);
    }

    class CharsetDecoder88591 extends CharsetDecoderASCII {
        public CharsetDecoder88591(CharsetICU cs) {
            super(cs);
        }

        protected CoderResult decodeLoopCoreOptimized(ByteBuffer source,
                CharBuffer target, byte[] sourceArray, char[] targetArray,
                int oldSource, int offset, int limit) {

            for (int i = oldSource; i < limit; i++)
                targetArray[i + offset] = (char) (sourceArray[i] & 0xff);

            return null;
        }

        protected CoderResult decodeLoopCoreUnoptimized(ByteBuffer source,
                CharBuffer target) throws BufferUnderflowException,
                BufferOverflowException {
            while (true)
                target.put((char) (source.get() & 0xff));
        }
    }

    class CharsetEncoder88591 extends CharsetEncoderASCII {
        public CharsetEncoder88591(CharsetICU cs) {
            super(cs);
        }

        protected CoderResult encodeLoopCoreOptimized(CharBuffer source,
                ByteBuffer target, char[] sourceArray, byte[] targetArray,
                int oldSource, int offset, int limit, boolean flush) {
            int i, ch = 0;
            for (i = oldSource; i < limit
                    && (((ch = (int) sourceArray[i]) & 0xff00) == 0); i++)
                targetArray[i + offset] = (byte) ch;

            if ((ch & 0xff00) != 0) {
                source.position(i + 1);
                target.position(i + offset);
                return encodeIllegal(source, ch, flush);
            } else
                return null;
        }

<<<<<<< .mine
        protected CoderResult encodeLoopCoreUnoptimized(CharBuffer source,
                ByteBuffer target, boolean flush)
                throws BufferUnderflowException, BufferOverflowException {
            int ch;
            while (((ch = (int) source.get()) & 0xff00) == 0)
                target.put((byte) ch);

            return encodeIllegal(source, ch, flush);
=======
            if (fromUChar32 != 0 && target.hasRemaining()){
                ch = fromUChar32;
                fromUChar32 = 0;
                       
                if (sourceArrayIndex < source.limit()) {
                    /* test the following code unit */
                    char trail = source.get(sourceArrayIndex);
                    if(UTF16.isTrailSurrogate(trail)) {
                        ++sourceArrayIndex;
                        ch = UCharacter.getCodePoint((char)ch, trail);
                        /* convert this supplementary code point */
                        cr = CoderResult.unmappableForLength(sourceArrayIndex);
                        doloop = false;
                    } else {
                        /* this is an unmatched lead code unit (1st surrogate) */
                        /* callback(illegal) */
                        fromUChar32 = (int)ch;
                        cr = CoderResult.malformedForLength(sourceArrayIndex);
                        doloop = false;
                    }
                } else {
                    /* no more input */
                    fromUChar32 = (int)ch;
                    doloop = false;
                }                            
            }
            if(doloop){
                /* conversion loop */
                ch=0;
                int ch2=0;
                while(sourceArrayIndex<source.limit()){
                    ch=source.get(sourceArrayIndex++);
                    if(ch<=0xff) {
                        if( target.hasRemaining()){
                            target.put((byte)ch);
                        }else{
                            cr = CoderResult.OVERFLOW;
                            break;
                        }
                    }else {
                        if (UTF16.isSurrogate((char)ch)) {
                            if (UTF16.isLeadSurrogate((char)ch)) {
                                //lowsurogate:
                                if (sourceArrayIndex < source.limit()) {
                                    ch2 = source.get(sourceArrayIndex);
                                    if (UTF16.isTrailSurrogate((char)ch2)) {
                                        ch = ((ch - UConverterConstants.SURROGATE_HIGH_START) << UConverterConstants.HALF_SHIFT) + ch2 + UConverterConstants.SURROGATE_LOW_BASE;
                                        sourceArrayIndex++;
                                    }
                                    else {
                                        /* this is an unmatched trail code unit (2nd surrogate) */
                                        /* callback(illegal) */
                                        fromUChar32 = ch;
                                        cr = CoderResult.OVERFLOW;
                                        break;
                                    }
                                }
                                else {
                                    /* ran out of source */
                                    fromUChar32 = ch;
                                    if (flush) {
                                        /* this is an unmatched trail code unit (2nd surrogate) */
                                        /* callback(illegal) */
                                        cr = CoderResult.malformedForLength(sourceArrayIndex);
                                    }
                                    break;
                                }
                            }
                        }
                        fromUChar32 = ch;
                        cr = CoderResult.malformedForLength(sourceArrayIndex);
                        break;                            
                    }
                }
            }
            /* set offsets since the start */
            if(offsets!=null) {
                count=target.position()-oldTarget;
                while(count>0) {
                    offsets.put(sourceIndex++);
                    --count;
                }
            } 
               
            source.position(sourceArrayIndex);
            return cr;
>>>>>>> .r21670
        }

    }

    public CharsetDecoder newDecoder() {
        return new CharsetDecoder88591(this);
    }

    public CharsetEncoder newEncoder() {
        return new CharsetEncoder88591(this);
    }

}
