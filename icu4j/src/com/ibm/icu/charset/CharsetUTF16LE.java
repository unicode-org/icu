/**
*******************************************************************************
* Copyright (C) 2006-2007, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
*******************************************************************************
*/ 
package com.ibm.icu.charset;

import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

class CharsetUTF16LE extends CharsetUTF16 {
    public CharsetUTF16LE(String icuCanonicalName, String javaCanonicalName, String[] aliases) {
        super(icuCanonicalName, javaCanonicalName, aliases, false, true);
    }

    class CharsetDecoderUTF16LE extends CharsetDecoderUTF16 {
        public CharsetDecoderUTF16LE(CharsetICU cs) {
            super(cs);
        }
    }

    class CharsetEncoderUTF16LE extends CharsetEncoderUTF16 {
        public CharsetEncoderUTF16LE(CharsetICU cs) {
            super(cs);
        }
    }

    public CharsetDecoder newDecoder() {
        return new CharsetDecoderUTF16LE(this);
    }

    public CharsetEncoder newEncoder() {
        return new CharsetEncoderUTF16LE(this);
    }
}
