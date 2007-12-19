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

class CharsetUTF16BE extends CharsetUTF16 {
    public CharsetUTF16BE(String icuCanonicalName, String javaCanonicalName, String[] aliases){
        super(icuCanonicalName, javaCanonicalName, aliases);
    }
    class CharsetDecoderUTF16BE extends CharsetDecoderUTF16{
        
        public CharsetDecoderUTF16BE(CharsetICU cs) {
            super(cs);
        }
        protected CoderResult decodeLoopImpl(ByteBuffer source, CharBuffer target, IntBuffer offsets, boolean flush){
            return decodeLoopUTF16BE(source, target, offsets, flush);
        }
    }
    public CharsetDecoder newDecoder() {
        return new CharsetDecoderUTF16BE(this);
    }
    class CharsetEncoderUTF16BE extends CharsetEncoderUTF16{

        public CharsetEncoderUTF16BE(CharsetICU cs) {
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
        return new CharsetEncoderUTF16BE(this);
    }
}
