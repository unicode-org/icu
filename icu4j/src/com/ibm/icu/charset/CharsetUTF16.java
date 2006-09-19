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

import com.ibm.icu.text.UTF16;

class CharsetUTF16 extends CharsetICU {
    
    protected byte[] fromUSubstitution = new byte[]{(byte)0xff, (byte)0xfd};
   
    public CharsetUTF16(String icuCanonicalName, String javaCanonicalName, String[] aliases){
        super(icuCanonicalName, javaCanonicalName, aliases);
        maxBytesPerChar = 4;
        minBytesPerChar = 2;
        maxCharsPerByte = 1;
    }
    class CharsetDecoderUTF16 extends CharsetDecoderICU{

        public CharsetDecoderUTF16(CharsetICU cs) {
            super(cs);
        }

        protected CoderResult decodeLoop(ByteBuffer source, CharBuffer target, IntBuffer offsets, boolean flush){
            CoderResult cr = CoderResult.UNDERFLOW;
            if(!source.hasRemaining() && toUnicodeStatus==0) {
                /* no input, nothing to do */
                return cr;
            }
            if(!target.hasRemaining()) {
                return CoderResult.OVERFLOW;
            }
        
            int sourceIndex=0, count=0, length, sourceArrayIndex;
            char c=0, trail;
            length = source.remaining();
            sourceArrayIndex = source.position();
            try{
                /* complete a partial UChar or pair from the last call */
                if(toUnicodeStatus!=0) {
                    /*
                     * special case: single byte from a previous buffer,
                     * where the byte turned out not to belong to a trail surrogate
                     * and the preceding, unmatched lead surrogate was put into toUBytes[]
                     * for error handling
                     */
                    toUBytesArray[toUBytesBegin+0]=(byte)toUnicodeStatus;
                    toULength=1;
                    toUnicodeStatus=0;
                }
                if((count=toULength)!=0) {
                    byte[] pArray=toUBytesArray;
                    int pArrayIndex = toUBytesBegin;
                    do {
                        pArray[count++]=source.get(sourceArrayIndex++);
                        ++sourceIndex;
                        --length;
                        if(count==2) {
                            c=(char)(((pArray[pArrayIndex+0]&UConverterConstants.UNSIGNED_BYTE_MASK)<<8)|(pArray[pArrayIndex+1]&UConverterConstants.UNSIGNED_BYTE_MASK));
                            if(!UTF16.isSurrogate(c)) {
                                /* output the BMP code point */
                                target.put(c);
                                if(offsets!=null) {
                                    offsets.put(-1);
                                }
                                count=0;
                                c=0;
                                break;
                            } else if(UTF16.isLeadSurrogate(c)) {
                                /* continue collecting bytes for the trail surrogate */
                                c=0; /* avoid unnecessary surrogate handling below */
                            } else {
                                /* fall through to error handling for an unmatched trail surrogate */
                                break;
                            }
                        } else if(count==4) {
                            c=(char)(((pArray[pArrayIndex+0]&UConverterConstants.UNSIGNED_BYTE_MASK)<<8)|(pArray[pArrayIndex+1]&UConverterConstants.UNSIGNED_BYTE_MASK));
                            trail=(char)(((pArray[pArrayIndex+2]&UConverterConstants.UNSIGNED_BYTE_MASK)<<8)|(pArray[pArrayIndex+3]&UConverterConstants.UNSIGNED_BYTE_MASK));
                            if(UTF16.isTrailSurrogate(trail)) {
                                /* output the surrogate pair */
                                target.put(c);
                                if(target.remaining()>=1) {
                                    target.put(trail);
                                    if(offsets!=null) {
                                        offsets.put(-1);
                                        offsets.put(-1);
                                    }
                                } else /* targetCapacity==1 */ {
                                    charErrorBufferArray[charErrorBufferBegin+0]=trail;
                                    charErrorBufferLength=1;
                                    return CoderResult.OVERFLOW;
                                }
                                count=0;
                                c=0;
                                break;
                            } else {
                                /* unmatched lead surrogate, handle here for consistent toUBytes[] */
            
                                /* back out reading the code unit after it */
                                if((source.position()-sourceArrayIndex)>=2) {
                                    sourceArrayIndex-=2;
                                } else {
                                    /*
                                     * if the trail unit's first byte was in a previous buffer, then
                                     * we need to put it into a special place because toUBytes[] will be
                                     * used for the lead unit's bytes
                                     */
                                    toUnicodeStatus=0x100|pArray[pArrayIndex+2];
                                    --sourceArrayIndex;
                                }
                                toULength=2;
                                cr = CoderResult.malformedForLength(sourceArrayIndex);
                                break;
                            }
                        }
                    } while(length>0);
                    toULength=(byte)count;
                }
            
                /* copy an even number of bytes for complete UChars */
                count=2*target.remaining();
                if(count>length) {
                    count=length&~1;
                }
                if(c==0 && count>0) {
                    length-=count;
                    count>>=1;
                    //targetCapacity-=count;
                    if(offsets==null) {
                        do {
                            c=(char)(((source.get(sourceArrayIndex+0)&UConverterConstants.UNSIGNED_BYTE_MASK)<<8)|(source.get(sourceArrayIndex+1)&UConverterConstants.UNSIGNED_BYTE_MASK));
                            sourceArrayIndex+=2;
                            if(!UTF16.isSurrogate(c)) {
                                target.put(c);
                            } else if(UTF16.isLeadSurrogate(c) && count>=2 &&
                                      UTF16.isTrailSurrogate(trail=(char)(((source.get(sourceArrayIndex+0)&UConverterConstants.UNSIGNED_BYTE_MASK)<<8)|(source.get(sourceArrayIndex+1)&UConverterConstants.UNSIGNED_BYTE_MASK)))
                                     ) {
                                sourceArrayIndex+=2;
                                --count;
                                target.put(c);
                                target.put(trail);
                            } else {
                                break;
                            }
                        } while(--count>0);
                    } else {
                        do {
                            c=(char)(((source.get(sourceArrayIndex+0)&UConverterConstants.UNSIGNED_BYTE_MASK)<<8)|(source.get(sourceArrayIndex+1)&UConverterConstants.UNSIGNED_BYTE_MASK));
                            sourceArrayIndex+=2;
                            if(!UTF16.isSurrogate(c)) {
                                target.put(c);
                                offsets.put(sourceIndex);
                                sourceIndex+=2;
                            } else if(UTF16.isLeadSurrogate(c) && count>=2 &&
                                      UTF16.isTrailSurrogate(trail=(char)(((source.get(sourceArrayIndex+0)&UConverterConstants.UNSIGNED_BYTE_MASK)<<8)|(source.get(sourceArrayIndex+1)&UConverterConstants.UNSIGNED_BYTE_MASK)))
                            ) {
                                sourceArrayIndex+=2;
                                --count;
                                target.put(c);
                                target.put(trail);
                                offsets.put(sourceIndex);
                                offsets.put(sourceIndex);
                                sourceIndex+=4;
                            } else {
                                break;
                            }
                        } while(--count>0);
                    }
            
                    if(count==0) {
                        /* done with the loop for complete UChars */
                        c=0;
                    } else {
                        /* keep c for surrogate handling, trail will be set there */
                        length+=2*(count-1); /* one more byte pair was consumed than count decremented */
                    }
                }
            
                if(c!=0) {
                    /*
                     * c is a surrogate, and
                     * - source or target too short
                     * - or the surrogate is unmatched
                     */
                    toUBytesArray[toUBytesBegin+0]=(byte)(c>>>8);
                    toUBytesArray[toUBytesBegin+1]=(byte)c;
                    toULength=2;
            
                    if(UTF16.isLeadSurrogate(c)) {
                        if(length>=2) {
                            if(UTF16.isTrailSurrogate(trail=(char)(((source.get(sourceArrayIndex+0)&UConverterConstants.UNSIGNED_BYTE_MASK)<<8)|(source.get(sourceArrayIndex+1)&UConverterConstants.UNSIGNED_BYTE_MASK)))) {
                                /* output the surrogate pair, will overflow (see conditions comment above) */
                                sourceArrayIndex+=2;
                                length-=2;
                                target.put(c);
                                if(offsets!=null) {
                                    offsets.put(sourceIndex);
                                }
                                charErrorBufferArray[charErrorBufferBegin+0]=trail;
                                charErrorBufferLength=1;
                                toULength=0;
                                cr = CoderResult.OVERFLOW;
                            } else {
                                /* unmatched lead surrogate */
                                cr = CoderResult.malformedForLength(sourceArrayIndex);
                            }
                        } else {
                            /* see if the trail surrogate is in the next buffer */
                        }
                    } else {
                        /* unmatched trail surrogate */
                        cr = CoderResult.malformedForLength(sourceArrayIndex);
                    }
                }
            
               
                /* check for a remaining source byte */
                if(!cr.isError()){
                    if(length>0) {
                        if(!target.hasRemaining()) {
                            cr = CoderResult.OVERFLOW;
                        } else {
                            /* it must be length==1 because otherwise the above would have copied more */
                            toUBytesArray[toULength++]=source.get(sourceArrayIndex++);
                        }
                    }
                }
                source.position(sourceArrayIndex);
            }catch(BufferOverflowException ex){
                cr = CoderResult.OVERFLOW;
            }
            return cr;
        }
        
    }
    class CharsetEncoderUTF16 extends CharsetEncoderICU{

        public CharsetEncoderUTF16(CharsetICU cs) {
            super(cs, fromUSubstitution);
            implReset();
        }

        private final static int NEED_TO_WRITE_BOM = 1;
        
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
            char c;
            /* write the BOM if necessary */
            if(fromUnicodeStatus==NEED_TO_WRITE_BOM) {
                byte bom[]={ (byte)0xfe, (byte)0xff };
                cr = fromUWriteBytes(this,bom, 0, bom.length, target, offsets, -1);
                if(cr.isError()){
                    return cr;
                }
                fromUnicodeStatus=0;
            }
            
            if(!target.hasRemaining()) {
                return CoderResult.OVERFLOW;
            }
            
            int sourceIndex = 0;
            char trail = 0;
            int length = source.remaining();
            int sourceArrayIndex = source.position();
            
            try{
                /* c!=0 indicates in several places outside the main loops that a surrogate was found */
            
                if((c=(char)fromUChar32)!=0 && UTF16.isTrailSurrogate(trail=source.get(sourceArrayIndex)) && target.remaining()>=4) {
                    /* the last buffer ended with a lead surrogate, output the surrogate pair */
                    ++sourceArrayIndex;
                    --length;
                    target.put((byte)(c>>>8));
                    target.put((byte)c);
                    target.put((byte)(trail>>>8));
                    target.put((byte)trail);
                    if(offsets!=null && offsets.remaining()>=4) {
                        offsets.put(-1);
                        offsets.put(-1);
                        offsets.put(-1);
                        offsets.put(-1);
                    }
                    sourceIndex=1;
                    fromUChar32=c=0;
                }
                byte overflow[/*4*/] = new byte[4];
                
                if(c==0) {
                    /* copy an even number of bytes for complete UChars */
                    int count=2*length;
                    int targetCapacity = target.limit();
                    if(count>targetCapacity) {
                        count=targetCapacity&~1;
                    }           
                    /* count is even */
                    targetCapacity-=count;
                    count>>=1;
                    length-=count;
    
                    if(offsets==null) {
                        while(count>0) {
                            c= source.get(sourceArrayIndex++);
                            if(!UTF16.isSurrogate(c)) {
                                target.put((byte)(c>>>8));
                                target.put((byte)c);
                                
                            } else if(UTF16.isLeadSurrogate(c) && count>=2 && UTF16.isTrailSurrogate(trail=source.get(sourceArrayIndex))) {
                                ++sourceArrayIndex;
                                --count;
                                target.put((byte)(c>>>8));
                                target.put((byte)c);
                                target.put((byte)(trail>>>8));
                                target.put((byte)trail);
                            } else {
                                break;
                            }
                            --count;
                        }
                    } else {
                        while(count>0) {
                            c=source.get(sourceArrayIndex++);
                            if(!UTF16.isSurrogate(c)) {
                                target.put((byte)(c>>>8));
                                target.put((byte)c);
                                offsets.put(sourceIndex);
                                offsets.put(sourceIndex++);
                            } else if(UTF16.isLeadSurrogate(c) && count>=2 && UTF16.isTrailSurrogate(trail=source.get(sourceArrayIndex))) {
                                ++sourceArrayIndex;
                                --count;
                                target.put((byte)(c>>>8));
                                target.put((byte)c);
                                target.put((byte)(trail>>>8));
                                target.put((byte)trail);
                                offsets.put(sourceIndex);
                                offsets.put(sourceIndex);
                                offsets.put(sourceIndex);
                                offsets.put(sourceIndex);
                                sourceIndex+=2;
                            } else {
                                break;
                            }
                            --count;
                        }
                    }
            
                    if(count==0) {
                        /* done with the loop for complete UChars */
                        if(length>0 && targetCapacity>0) {
                            /*
                             * there is more input and some target capacity -
                             * it must be targetCapacity==1 because otherwise
                             * the above would have copied more;
                             * prepare for overflow output
                             */
                            if(!UTF16.isSurrogate(c=source.get(sourceArrayIndex++))) {
                                overflow[0]=(byte)(c>>>8);
                                overflow[1]=(byte)c;
                                length=2; /* 2 bytes to output */
                                c=0;
                            /* } else { keep c for surrogate handling, length will be set there */
                            }
                        } else {
                            length=0;
                            c=0;
                        }
                    } else {
                        /* keep c for surrogate handling, length will be set there */
                        targetCapacity+=2*count;
                    }
                } else {
                    length=0; /* from here on, length counts the bytes in overflow[] */
                }
                
                if(c!=0) {
                    /*
                     * c is a surrogate, and
                     * - source or target too short
                     * - or the surrogate is unmatched
                     */
                    length=0;
                    if(UTF16.isLeadSurrogate(c)) {
                        if(sourceArrayIndex<source.limit()) {
                            if(UTF16.isTrailSurrogate(trail=source.get(sourceArrayIndex))) {
                                /* output the surrogate pair, will overflow (see conditions comment above) */
                                ++sourceArrayIndex;
                                overflow[0]=(byte)(c>>>8);
                                overflow[1]=(byte)c;
                                overflow[2]=(byte)(trail>>>8);
                                overflow[3]=(byte)trail;
                                length=4; /* 4 bytes to output */
                                c=0;
                            } else {
                                /* unmatched lead surrogate */
                                //pErrorCode[0]=ErrorCode.U_ILLEGAL_CHAR_FOUND;
                                cr = CoderResult.malformedForLength(sourceArrayIndex);
                            }
                        } else {
                            /* see if the trail surrogate is in the next buffer */
                        }
                    } else {
                        /* unmatched trail surrogate */
                        cr = CoderResult.malformedForLength(sourceArrayIndex);
                    }
                    fromUChar32=c;
                }
                source.position(sourceArrayIndex);
                if(length>0) {
                    /* output length bytes with overflow (length>targetCapacity>0) */
                    fromUWriteBytes(this, overflow, 0, length, target, offsets, sourceIndex);
                }
            }catch(BufferOverflowException ex){
                cr = CoderResult.OVERFLOW;
            }
            return cr;
        }
    }
    public CharsetDecoder newDecoder() {
        return new CharsetDecoderUTF16(this);
    }

    public CharsetEncoder newEncoder() {
        return new CharsetEncoderUTF16(this);
    }

}
