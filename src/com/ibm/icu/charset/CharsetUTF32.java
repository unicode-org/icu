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
class CharsetUTF32 extends CharsetICU {
    
    protected byte[] fromUSubstitution = new byte[]{(byte)0, (byte)0, (byte)0xff, (byte)0xfd};
    protected int bom;
    
    public CharsetUTF32(String icuCanonicalName, String javaCanonicalName, String[] aliases){
        super(icuCanonicalName, javaCanonicalName, aliases);
        maxBytesPerChar = 4;
        minBytesPerChar = 4;
        maxCharsPerByte = 1;
        bom=0;
    }
    
    
    class CharsetDecoderUTF32 extends CharsetDecoderICU{
        boolean isFirstBuffer;
        final int SIGNATURE_LENGTH=4;
        public CharsetDecoderUTF32(CharsetICU cs) {
            super(cs);
            isFirstBuffer = true;
        }
        
        protected CoderResult decodeLoop(ByteBuffer source, CharBuffer target, IntBuffer offsets, boolean flush){
            int state, offsetDelta;
            int offsetsPos = (offsets==null)?0:offsets.position();
            CoderResult cr = CoderResult.UNDERFLOW;
            state=mode;

            /*
             * If we detect a BOM in this buffer, then we must add the BOM size to the
             * offsets because the actual converter function will not see and count the BOM.
             * offsetDelta will have the number of the BOM bytes that are in the current buffer.
             */
            offsetDelta=0;
            int pos = source.position(); 
            if(isFirstBuffer && toULength<SIGNATURE_LENGTH && source.remaining()>=SIGNATURE_LENGTH){
                while(pos < source.limit() && pos < SIGNATURE_LENGTH) {
                    toUBytesArray[toULength++] = source.get(pos++);
                }
                if(toULength==SIGNATURE_LENGTH){
                    if(toUBytesArray[0]==(byte)0x00 && toUBytesArray[1]==(byte)0x00 && toUBytesArray[2]==(byte)0xFE && toUBytesArray[3]==(byte)0xFF){
                        // may be BE
                        state = 1;
                        offsetDelta=4;
                    }else if(toUBytesArray[0]==(byte)0xFF && toUBytesArray[1]==(byte)0xFE && toUBytesArray[2]==(byte)0x00 && toUBytesArray[3]==(byte)0x00){
                        //may be LE
                        state = 2;
                        offsetDelta=4;
                    }else{
                        //default to the subclass charset
                        //state = 3;
                        toUnicodeStatus = getChar(toUBytesArray, toULength)+1;  
                    }
                    isFirstBuffer = false;
                }
            }

            /* add BOM size to offsets - see comment at offsetDelta declaration */
            if(offsets!=null && offsetDelta!=0) {
                int offsetsLimit=offsets.position();
                while(offsetsPos<offsetsLimit) {
                    int delta = offsetDelta + offsets.get(pos);
                    offsets.put(pos++, delta);
                }
            }
            
            mode=state;
            source.position(pos);
            if (!source.hasRemaining()) {
                return cr;
            }
            else if(!isFirstBuffer || bom!=0){
                cr = decodeLoopImpl(source, target, offsets, flush);
            } else {
                cr = CoderResult.malformedForLength(pos);
            }

            return cr;
        }
        protected int getChar(byte[] bytes, int length){
            return -1;
        }
        protected CoderResult decodeLoopImpl(ByteBuffer source, CharBuffer target, IntBuffer offsets, boolean flush){
            
            CoderResult cr = CoderResult.UNDERFLOW;
            if(mode==1){
                /* call UTF-32BE */
                cr = decodeLoopUTF32BE(source, target, offsets, flush);
            }else if(mode==2){
                /* call UTF-32LE */
                cr =decodeLoopUTF32LE(source, target, offsets, flush);
            }else{
                /* should not occur */
                cr = decodeLoopUTF32BE(source, target, offsets, flush);
            }
            return cr;
        }
        final CoderResult decodeLoopUTF32BE(ByteBuffer source, CharBuffer target, IntBuffer offsets, boolean flush){
            CoderResult cr = CoderResult.UNDERFLOW;
            
            int sourceArrayIndex = source.position();
            int ch, i;
            
            donefornow:
            {                    
                /* UTF-8 returns here for only non-offset, this needs to change.*/
                if (toUnicodeStatus != 0 && target.hasRemaining()) {
                    i = toULength;       /* restore # of bytes consumed */
            
                    ch = (int)(toUnicodeStatus - 1);/*Stores the previously calculated ch from a previous call*/
                    toUnicodeStatus = 0;
                    toULength =0;
                    
                    while (i < 4) {
                        if (sourceArrayIndex < source.limit()) {
                            ch = (ch << 8) | ((byte)(source.get(sourceArrayIndex)) & UConverterConstants.UNSIGNED_BYTE_MASK);
                            toUBytesArray[i++] = (byte) source.get(sourceArrayIndex++);
                        }
                        else {
                            /* stores a partially calculated target*/
                            /* + 1 to make 0 a valid character */
                            toUnicodeStatus = ch + 1;
                            toULength = (byte) i;
                            break donefornow;
                        }
                    }
            
                    if (ch <= UConverterConstants.MAXIMUM_UTF && !isSurrogate(ch)) {
                        /* Normal valid byte when the loop has not prematurely terminated (i < inBytes) */
                        if (ch <= UConverterConstants.MAXIMUM_UCS2) 
                        {
                            /* fits in 16 bits */
                            target.put((char)ch);
                        }
                        else {
                            /* write out the surrogates */
                            target.put(UTF16.getLeadSurrogate(ch));
                            ch = UTF16.getTrailSurrogate(ch);
                            if (target.hasRemaining()) {
                                target.put((char)ch);
                            }
                            else {
                                /* Put in overflow buffer (not handled here) */
                                charErrorBufferArray[0] = (char) ch;
                                charErrorBufferLength = 1;
                                cr = CoderResult.OVERFLOW;
                            }
                        }
                    }
                    else {
                        toULength = (byte)i;
                        cr = CoderResult.malformedForLength(sourceArrayIndex);
                        break donefornow;
                    }
                }
                
                while (sourceArrayIndex < source.limit() && target.hasRemaining()) {
                    i = 0;
                    ch = 0;
            
                    while (i < 4) {
                        if (sourceArrayIndex < source.limit()) {
                            ch = (ch << 8) | ((byte)(source.get(sourceArrayIndex)) & UConverterConstants.UNSIGNED_BYTE_MASK);
                            toUBytesArray[i++] = (byte) source.get(sourceArrayIndex++);
                        }
                        else {
                            /* stores a partially calculated target*/
                            /* + 1 to make 0 a valid character */
                            toUnicodeStatus = ch + 1;
                            toULength = (byte) i;
                            break donefornow;
                        }
                    }
            
                    if (ch <= UConverterConstants.MAXIMUM_UTF && !isSurrogate(ch)) {
                        /* Normal valid byte when the loop has not prematurely terminated (i < inBytes) */
                        if (ch <= UConverterConstants.MAXIMUM_UCS2) 
                        {
                            /* fits in 16 bits */
                            target.put((char) ch);
                        }
                        else {
                            /* write out the surrogates */
                            target.put(UTF16.getLeadSurrogate(ch));
                            ch = UTF16.getTrailSurrogate(ch);
                            if (target.hasRemaining()) {
                                target.put((char)ch);
                            }
                            else {
                                /* Put in overflow buffer (not handled here) */
                                charErrorBufferArray[0] = (char) ch;
                                charErrorBufferLength = 1;
                                cr = CoderResult.OVERFLOW;   
                                break;
                            }
                        }
                    }
                    else {
                        toULength = (byte)i;
                        cr = CoderResult.malformedForLength(sourceArrayIndex);
                        break;
                    }
                }
            }
            
            if (sourceArrayIndex < source.limit() && !target.hasRemaining()) {
                /* End of target buffer */
                cr = CoderResult.OVERFLOW;
            }                    
            
            source.position(sourceArrayIndex);
            return cr;
        }        

        final CoderResult decodeLoopUTF32LE(ByteBuffer source, CharBuffer target, IntBuffer offsets, boolean flush){
            CoderResult cr = CoderResult.UNDERFLOW;
            
            int sourceArrayIndex = source.position();
            int ch, i;

            donefornow:
            {                    
                /* UTF-8 returns here for only non-offset, this needs to change.*/
                if (toUnicodeStatus != 0 && target.hasRemaining()) {
                    i = toULength;       /* restore # of bytes consumed */
            
                    ch = (int)(toUnicodeStatus - 1);/*Stores the previously calculated ch from a previous call*/
                    toUnicodeStatus = 0;
                    toULength=0;
                    
                    while (i < 4) {
                        if (sourceArrayIndex < source.limit()) {
                            ch |= (source.get(sourceArrayIndex) & UConverterConstants.UNSIGNED_BYTE_MASK) << (i * 8);
                            toUBytesArray[i++] = (byte) source.get(sourceArrayIndex++);
                        }
                        else {
                            /* stores a partially calculated target*/
                            /* + 1 to make 0 a valid character */
                            toUnicodeStatus = ch + 1;
                            toULength = (byte) i;
                            break donefornow;
                        }
                    }
            
                    if (ch <= UConverterConstants.MAXIMUM_UTF && !isSurrogate(ch)) {
                        /* Normal valid byte when the loop has not prematurely terminated (i < inBytes) */
                        if (ch <= UConverterConstants.MAXIMUM_UCS2) 
                        {
                            /* fits in 16 bits */
                            target.put((char)ch);
                        }
                        else {
                            /* write out the surrogates */
                            target.put(UTF16.getLeadSurrogate(ch));
                            ch = UTF16.getTrailSurrogate(ch);
                            if (target.hasRemaining()) {
                                target.put((char)ch);
                            }
                            else {
                                /* Put in overflow buffer (not handled here) */
                                charErrorBufferArray[0] = (char) ch;
                                charErrorBufferLength = 1;
                                cr = CoderResult.OVERFLOW;
                            }
                        }
                    }
                    else {
                        toULength = (byte)i;
                        cr = CoderResult.malformedForLength(sourceArrayIndex);
                        break donefornow;
                    }
                }
                
                while (sourceArrayIndex < source.limit() && target.hasRemaining()) {
                    i = 0;
                    ch = 0;
            
                    while (i < 4) {
                        if (sourceArrayIndex < source.limit()) {
                            ch |= (source.get(sourceArrayIndex) & UConverterConstants.UNSIGNED_BYTE_MASK) << (i * 8);
                            toUBytesArray[i++] = (byte) source.get(sourceArrayIndex++);
                        }
                        else {
                            /* stores a partially calculated target*/
                            /* + 1 to make 0 a valid character */
                            toUnicodeStatus = ch + 1;
                            toULength = (byte) i;
                            break donefornow;
                        }
                    }
            
                    if (ch <= UConverterConstants.MAXIMUM_UTF && !isSurrogate(ch)) {
                        /* Normal valid byte when the loop has not prematurely terminated (i < inBytes) */
                        if (ch <= UConverterConstants.MAXIMUM_UCS2) 
                        {
                            /* fits in 16 bits */
                            target.put((char) ch);
                        }
                        else {
                            /* write out the surrogates */
                            target.put(UTF16.getLeadSurrogate(ch));
                            ch = UTF16.getTrailSurrogate(ch);
                            if (target.hasRemaining()) {
                                target.put((char)ch);
                            }
                            else {
                                /* Put in overflow buffer (not handled here) */
                                charErrorBufferArray[0] = (char) ch;
                                charErrorBufferLength = 1;
                                cr = CoderResult.OVERFLOW;                                    
                                break;
                            }
                        }
                    }
                    else {
                        toULength = (byte)i;
                        cr = CoderResult.malformedForLength(sourceArrayIndex);
                        break;
                    }
                }
            }
            
            if (sourceArrayIndex < source.limit() && !target.hasRemaining()) {
                /* End of target buffer */
                cr = CoderResult.OVERFLOW;
            }                    
            
            source.position(sourceArrayIndex);
            return cr;
        }  
        protected void implReset() {
            super.implReset();
            isFirstBuffer = true;
        }
    }
    
    class CharsetEncoderUTF32 extends CharsetEncoderICU{

        public CharsetEncoderUTF32(CharsetICU cs) {
            super(cs, fromUSubstitution);
            implReset();
            writeBOM = true;
        }

        protected void implReset() {
            super.implReset();
            fromUnicodeStatus = NEED_TO_WRITE_BOM;
        }
        
        protected CoderResult encodeLoop(CharBuffer source, ByteBuffer target, IntBuffer offsets, boolean flush){
            CoderResult cr = CoderResult.UNDERFLOW;
            if(!source.hasRemaining()) {
                /* no input, nothing to do */
                return cr;
            }
            
            /* write the BOM if necessary */
            if(fromUnicodeStatus==NEED_TO_WRITE_BOM && writeBOM) {
                byte[] bom={ 0, 0, (byte)0xfe, (byte)0xff };
                cr = fromUWriteBytes(this, bom, 0, bom.length, target, offsets, -1);
                if(cr.isError()  || cr.isOverflow()){
                    return cr;
                }
                fromUnicodeStatus=0;
            }
            
            int ch, ch2;
            int indexToWrite;
            byte temp[] = new byte[4];
            temp[0] = 0;
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
                temp[1] = (byte) (ch >>> 16 & 0x1F);
                temp[2] = (byte) (ch >>> 8);  /* unsigned cast implicitly does (ch & FF) */
                temp[3] = (byte) (ch);       /* unsigned cast implicitly does (ch & FF) */
        
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
                    temp[1] = (byte) (ch >>> 16 & 0x1F);
                    temp[2] = (byte) (ch >>> 8);  /* unsigned cast implicitly does (ch & FF) */
                    temp[3] = (byte) (ch);       /* unsigned cast implicitly does (ch & FF) */
            
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
        return new CharsetDecoderUTF32(this);
    }

    public CharsetEncoder newEncoder() {
        return new CharsetEncoderUTF32(this);
    }
}
