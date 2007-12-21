/**
*******************************************************************************
* Copyright (C) 2007, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
*******************************************************************************
*/ 
package com.ibm.icu.charset;

import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

class CharsetUTF16BE extends CharsetUTF16 {
    public CharsetUTF16BE(String icuCanonicalName, String javaCanonicalName, String[] aliases) {
        super(icuCanonicalName, javaCanonicalName, aliases, true, true);
    }

    class CharsetDecoderUTF16BE extends CharsetDecoderUTF16 {
        public CharsetDecoderUTF16BE(CharsetICU cs) {
            super(cs);
        }
    }

    class CharsetEncoderUTF16BE extends CharsetEncoderUTF16 {
        public CharsetEncoderUTF16BE(CharsetICU cs) {
            super(cs);
        }
    }

    public CharsetDecoder newDecoder() {
        return new CharsetDecoderUTF16BE(this);
    }

    public CharsetEncoder newEncoder() {
        return new CharsetEncoderUTF16BE(this);
    }
}
