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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UTF16;

class CharsetASCII extends CharsetICU {
    protected byte[] fromUSubstitution = new byte[]{(byte)0x1a};
    public CharsetASCII(String icuCanonicalName, String javaCanonicalName, String[] aliases){
        super(icuCanonicalName, javaCanonicalName, aliases);
        maxBytesPerChar = 1;
        minBytesPerChar = 1;
        maxCharsPerByte = 1;
    }
    class CharsetDecoderASCII extends CharsetDecoderICU{

        public CharsetDecoderASCII(CharsetICU cs) {
            super(cs);
        }

        protected CoderResult decodeLoop(ByteBuffer source, CharBuffer target, IntBuffer offsets){
            CoderResult cr = CoderResult.UNDERFLOW;
            if(!source.hasRemaining() && toUnicodeStatus==0) {
                /* no input, nothing to do */
                return cr;
            }
            if(!target.hasRemaining()) {
                return CoderResult.OVERFLOW;
            }
        
            int sourceArrayIndex=source.position(), count=0;
            int sourceIndex = 0;
            char c=0;
            int oldTarget = target.position();
            try{
                /* conversion loop */
                c=0;
                while(sourceArrayIndex<source.limit()&&
                        (c=(char)source.get(sourceArrayIndex))<=0x7f){
                     target.put(c);
                     sourceArrayIndex++;
                }

                if(c>0x7f) {
                    /* callback(illegal); copy the current bytes to toUBytes[] */
                    toUBytesArray[0]=(byte)c;
                    toULength=1;
                    cr = CoderResult.malformedForLength(toULength);
                } else if(sourceArrayIndex<source.limit() && !target.hasRemaining()) {
                    /* target is full */
                    cr = CoderResult.OVERFLOW;
                }

                /* set offsets since the start */
                if(offsets!=null) {
                    count=target.position()-oldTarget;
                    while(count>0) {
                        offsets.put(sourceIndex++);
                        --count;
                    }
                }
            }catch(BufferOverflowException ex){
                cr = CoderResult.OVERFLOW;
            }
            source.position(sourceArrayIndex);
            return cr;
        }
        
    }
    class CharsetEncoderASCII extends CharsetEncoderICU{

        public CharsetEncoderASCII(CharsetICU cs) {
            super(cs, fromUSubstitution);
            implReset();
        }

        private final static int NEED_TO_WRITE_BOM = 1;
        
        protected void implReset() {
            super.implReset();
            fromUnicodeStatus = NEED_TO_WRITE_BOM;
        }
        
        protected CoderResult encodeLoop(CharBuffer source, ByteBuffer target, IntBuffer offsets){
            CoderResult cr = CoderResult.UNDERFLOW;
            if(!source.hasRemaining()) {
                /* no input, nothing to do */
                return cr;
            }
            
            if(!target.hasRemaining()) {
                return CoderResult.OVERFLOW;
            }
            
            int sourceArrayIndex=source.position(), count=0;
            int sourceIndex = 0;
            int ch=0;
            int oldTarget = target.position();
            boolean doloop = true;
            try{
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
                            /* callback(unassigned) */
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
                            target.put((byte)ch);
                        }else {
                            if (UTF16.isSurrogate((char)ch)) {
                                if (UTF16.isLeadSurrogate((char)ch)) {
                                    //lowsurogate:
                                    if (sourceArrayIndex < source.limit()) {
                                        ch2 = source.get(sourceArrayIndex);
                                        if (UTF16.isTrailSurrogate((char)ch2)) {
                                            ch = ((ch - UConverterSharedData.SURROGATE_HIGH_START) << UConverterSharedData.HALF_SHIFT) + ch2 + UConverterSharedData.SURROGATE_LOW_BASE;
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
               
            }catch(BufferOverflowException ex){
                cr = CoderResult.OVERFLOW;
            }
            source.position(sourceArrayIndex);
            return cr;
        }
    }
    public CharsetDecoder newDecoder() {
        return new CharsetDecoderASCII(this);
    }

    public CharsetEncoder newEncoder() {
        return new CharsetEncoderASCII(this);
    }
//#ifdef VERSION_1.5   
//    /**
//     * Implements compareTo method of Comparable interface
//     * @see java.lang.Comparable#compareTo(java.lang.Object)
//     */
//    public int compareTo(Object o) {
//        if(o instanceof Charset){
//            return super.compareTo((Charset)o);
//        }
//        return -1;
//    }
//#endif
}
