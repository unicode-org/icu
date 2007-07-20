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
class CharsetUTF16LE extends CharsetUTF16 {
    
    public CharsetUTF16LE(String icuCanonicalName, String javaCanonicalName, String[] aliases){
        super(icuCanonicalName, javaCanonicalName, aliases);
        fromUSubstitution = new byte[]{(byte)0xfd, (byte)0xff};
    }

    class CharsetDecoderUTF16LE extends CharsetDecoderUTF16{
        
        public CharsetDecoderUTF16LE(CharsetICU cs) {
            super(cs);
        }
        protected CoderResult decodeLoopImpl(ByteBuffer source, CharBuffer target, IntBuffer offsets, boolean flush){
            return decodeLoopUTF16LE(source, target, offsets, flush);
        }
    }
    
    private static final byte UTF16LE_BOM[]={ (byte)0xff, (byte)0xfe };

    class CharsetEncoderUTF16LE extends CharsetEncoderICU{
        
        public CharsetEncoderUTF16LE(CharsetICU cs) {
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
            char c;
            /* write the BOM if necessary */
            if(fromUnicodeStatus==NEED_TO_WRITE_BOM && writeBOM) {
                cr = fromUWriteBytes(this, UTF16LE_BOM, 0, UTF16LE_BOM.length, target, offsets, -1);
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
            
            /* c!=0 indicates in several places outside the main loops that a surrogate was found */
        
            if((c=(char)fromUChar32)!=0 && UTF16.isTrailSurrogate(trail=source.get(sourceArrayIndex)) && target.remaining()>=4) {
                /* the last buffer ended with a lead surrogate, output the surrogate pair */
                ++sourceArrayIndex;
                --length;
                target.put((byte)c);
                target.put((byte)(c>>>8));
                target.put((byte)trail);
                target.put((byte)(trail>>>8));
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
                int targetCapacity = target.remaining();
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
                            target.put((byte)c);
                            target.put((byte)(c>>>8));
                            
                        } else if(UTF16.isLeadSurrogate(c) && count>=2 && UTF16.isTrailSurrogate(trail=source.get(sourceArrayIndex))) {
                            ++sourceArrayIndex;
                            --count;
                            target.put((byte)c);
                            target.put((byte)(c>>>8));
                            target.put((byte)trail);
                            target.put((byte)(trail>>>8));
                        } else {
                            break;
                        }
                        --count;
                    }
                } else {
                    while(count>0) {
                        c=source.get(sourceArrayIndex++);
                        if(!UTF16.isSurrogate(c)) {
                            target.put((byte)c);
                            target.put((byte)(c>>>8));
                            offsets.put(sourceIndex);
                            offsets.put(sourceIndex++);
                        } else if(UTF16.isLeadSurrogate(c) && count>=2 && UTF16.isTrailSurrogate(trail=source.get(sourceArrayIndex))) {
                            ++sourceArrayIndex;
                            --count;
                            target.put((byte)c);
                            target.put((byte)(c>>>8));
                            target.put((byte)trail);
                            target.put((byte)(trail>>>8));
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
                            overflow[0]=(byte)c;
                            overflow[1]=(byte)(c>>>8);
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
                            overflow[0]=(byte)c;
                            overflow[1]=(byte)(c>>>8);
                            overflow[2]=(byte)trail;
                            overflow[3]=(byte)(trail>>>8);
                            length=4; /* 4 bytes to output */
                            c=0;
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
                fromUChar32=c;
            }
            source.position(sourceArrayIndex);
            if(length>0) {
                /* output length bytes with overflow (length>targetCapacity>0) */
                cr = fromUWriteBytes(this, overflow, 0, length, target, offsets, sourceIndex);
            }
            return cr;
        }
    }
    public CharsetDecoder newDecoder() {
        return new CharsetDecoderUTF16LE(this);
    }

    public CharsetEncoder newEncoder() {
        return new CharsetEncoderUTF16LE(this);
    }

}
