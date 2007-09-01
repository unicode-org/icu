/**
*******************************************************************************
* Copyright (C) 2007, International Business Machines Corporation and    *
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

class CharsetUTF32BE extends CharsetUTF32 {
    public CharsetUTF32BE(String icuCanonicalName, String javaCanonicalName, String[] aliases){
        super(icuCanonicalName, javaCanonicalName, aliases);
    }
    class CharsetDecoderUTF32BE extends CharsetDecoderUTF32{
        
        public CharsetDecoderUTF32BE(CharsetICU cs) {
            super(cs);
            mode=1;
            bom=1;
        }
        protected CoderResult decodeLoopImpl(ByteBuffer source, CharBuffer target, IntBuffer offsets, boolean flush){
            return decodeLoopUTF32BE(source, target, offsets, flush);
        }
        protected int getChar(byte[] bytes, int length){
            int i=0, ch=0;
            while (i<length){
                ch |= (bytes[i] & UConverterConstants.UNSIGNED_BYTE_MASK) << ((3-i) * 8);
                i++;
            }
            return ch;
        }
    }
    public CharsetDecoder newDecoder() {
        return new CharsetDecoderUTF32BE(this);
    }
    class CharsetEncoderUTF32BE extends CharsetEncoderUTF32{

        public CharsetEncoderUTF32BE(CharsetICU cs) {
            super(cs);
            implReset();
        }

        protected void implReset() {
            super.implReset();
            fromUnicodeStatus = 0;
            writeBOM = false;
        }
    }
    public CharsetEncoder newEncoder() {
        return new CharsetEncoderUTF32BE(this);
    }
}
