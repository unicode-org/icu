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

class CharsetUTF32LE extends CharsetUTF32 {
    public CharsetUTF32LE(String icuCanonicalName, String javaCanonicalName, String[] aliases) {
        super(icuCanonicalName, javaCanonicalName, aliases, false, true);
    }

    class CharsetDecoderUTF32LE extends CharsetDecoderUTF32 {
        public CharsetDecoderUTF32LE(CharsetICU cs) {
            super(cs);
        }
    }

    class CharsetEncoderUTF32LE extends CharsetEncoderUTF32 {
        public CharsetEncoderUTF32LE(CharsetICU cs) {
            super(cs);
        }
    }

    public CharsetDecoder newDecoder() {
        return new CharsetDecoderUTF32LE(this);
    }

    public CharsetEncoder newEncoder() {
        return new CharsetEncoderUTF32LE(this);
    }
}
