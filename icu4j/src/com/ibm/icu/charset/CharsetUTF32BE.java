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

class CharsetUTF32BE extends CharsetUTF32 {
    public CharsetUTF32BE(String icuCanonicalName, String javaCanonicalName, String[] aliases) {
        super(icuCanonicalName, javaCanonicalName, aliases, true, true);
    }

    class CharsetDecoderUTF32BE extends CharsetDecoderUTF32 {
        public CharsetDecoderUTF32BE(CharsetICU cs) {
            super(cs);
        }
    }

    class CharsetEncoderUTF32BE extends CharsetEncoderUTF32 {
        public CharsetEncoderUTF32BE(CharsetICU cs) {
            super(cs);
        }
    }

    public CharsetDecoder newDecoder() {
        return new CharsetDecoderUTF32BE(this);
    }

    public CharsetEncoder newEncoder() {
        return new CharsetEncoderUTF32BE(this);
    }
}
