/**
*******************************************************************************
* Copyright (C) 2006-2007, International Business Machines Corporation and    *
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
/**
 * @author Niti Hantaweepant
 */
class CharsetUTF32LE extends CharsetUTF32 {
    
    protected byte[] fromUSubstitution = new byte[]{(byte)0xfd, (byte)0xff, (byte)0, (byte)0};
    
    public CharsetUTF32LE(String icuCanonicalName, String javaCanonicalName, String[] aliases){
        super(icuCanonicalName, javaCanonicalName, aliases);
        maxBytesPerChar = 4;
        minBytesPerChar = 4;
        maxCharsPerByte = 1;
    }
    class CharsetDecoderUTF32LE extends CharsetDecoderUTF32{
        
        public CharsetDecoderUTF32LE(CharsetICU cs) {
            super(cs);
            mode=2;
            bom=2;
        }
        protected CoderResult decodeLoopImpl(ByteBuffer source, CharBuffer target, IntBuffer offsets, boolean flush){
            return decodeLoopUTF32LE(source, target, offsets, flush);
        }
        protected int getChar(byte[] bytes, int length){
            int i=0;
            int ch=0;
            while(i<length){
                ch |= (bytes[i] & UConverterConstants.UNSIGNED_BYTE_MASK) << (i * 8);
                i++;
            }
            return ch;
        }
    }
    class CharsetEncoderUTF32LE extends CharsetEncoderICU{

        public CharsetEncoderUTF32LE(CharsetICU cs) {
            super(cs, fromUSubstitution);
            implReset();
        }
        
        protected void implReset() {
            super.implReset();
            fromUnicodeStatus = 0;
        }
        
        protected CoderResult encodeLoop(CharBuffer source, ByteBuffer target, IntBuffer offsets, boolean flush){
            CoderResult cr = CoderResult.UNDERFLOW;
            if(!source.hasRemaining()) {
                /* no input, nothing to do */
                return cr;
            }
            
            /* write the BOM if necessary */
            if(fromUnicodeStatus==NEED_TO_WRITE_BOM && writeBOM) {
                byte[] bom={ (byte)0xff, (byte)0xfe, 0, 0 };
                cr = fromUWriteBytes(this, bom, 0, bom.length, target, offsets, -1);
                if(cr.isError()){
                    return cr;
                }
                fromUnicodeStatus=0;
            }
             
            int ch, ch2;
            int indexToWrite;
            byte temp[] = new byte[4];
            temp[3] = 0;
            int sourceArrayIndex = source.position();
            
            boolean doloop = true;
            if (fromUChar32 != 0) {
                ch = fromUChar32;
                fromUChar32 = 0;
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
                        cr = CoderResult.malformedForLength(sourceArrayIndex);
                        doloop = false;
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
                    doloop = false;
                }
                
                /* We cannot get any larger than 10FFFF because we are coming from UTF-16 */
                temp[2] = (byte) (ch >>> 16 & 0x1F);
                temp[1] = (byte) (ch >>> 8);  /* unsigned cast implicitly does (ch & FF) */
                temp[0] = (byte) (ch);       /* unsigned cast implicitly does (ch & FF) */
        
                for (indexToWrite = 0; indexToWrite <= 3; indexToWrite++) {
                    if (target.hasRemaining()) {
                        target.put(temp[indexToWrite]);
                    }
                    else {
                        errorBuffer[errorBufferLength++] = temp[indexToWrite];
                        cr = CoderResult.OVERFLOW;
                    }
                }
            }
        
            if(doloop) {
                while (sourceArrayIndex < source.limit() && target.hasRemaining()) {
                    ch = source.get(sourceArrayIndex++);
            
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
                        else {
                            fromUChar32 = ch;
                            cr = CoderResult.malformedForLength(sourceArrayIndex);
                            break;
                        }
                    }
            
                    /* We cannot get any larger than 10FFFF because we are coming from UTF-16 */
                    temp[2] = (byte) (ch >>> 16 & 0x1F);
                    temp[1] = (byte) (ch >>> 8);  /* unsigned cast implicitly does (ch & FF) */
                    temp[0] = (byte) (ch);       /* unsigned cast implicitly does (ch & FF) */
            
                    for (indexToWrite = 0; indexToWrite <= 3; indexToWrite++) {
                        if (target.hasRemaining()) {
                            target.put(temp[indexToWrite]);
                        }
                        else {
                            errorBuffer[errorBufferLength++] = temp[indexToWrite];
                            cr = CoderResult.OVERFLOW;
                        }
                    }
                }
            }
        
            if (sourceArrayIndex < source.limit() && !target.hasRemaining()) {
                cr = CoderResult.OVERFLOW;
            }
            source.position(sourceArrayIndex);
            return cr;
        }
    }
    public CharsetDecoder newDecoder() {
        return new CharsetDecoderUTF32LE(this);
    }

    public CharsetEncoder newEncoder() {
        return new CharsetEncoderUTF32LE(this);
    }
}
