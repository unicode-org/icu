/* *******************************************************************************
 * Copyright (C) 2011, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import com.ibm.icu.dev.test.util.UnicodeTransform.Type;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.Normalizer2.Mode;

/**
 * @author markdavis
 *
 */
public class IcuUnicodeNormalizerFactory implements UnicodeTransform.Factory {

    public UnicodeTransform getInstance(Type type) {
        switch (type) {
        case NFC: case NFKC:
            return new IcuUnicodeNormalizer(Normalizer2.getInstance(null, type.toString(), Mode.COMPOSE));
        case NFD: case NFKD:
            return new IcuUnicodeNormalizer(Normalizer2.getInstance(null, type == Type.NFD ? "NFC" : "NFKC", Mode.DECOMPOSE));
        case CASEFOLD:
            return new CaseFolder();
        default:
            throw new IllegalArgumentException();
        }
    }

    private static class CaseFolder extends UnicodeTransform {
        @Override
        public String transform(String source) {
            return UCharacter.foldCase(source.toString(), true);
        }
    }

    private static class IcuUnicodeNormalizer extends UnicodeTransform {
        private Normalizer2 normalizer;

        private IcuUnicodeNormalizer(Normalizer2 normalizer) {
            this.normalizer = normalizer;
        }

        public String transform(String src) {
            return normalizer.normalize(src);
        }

        public boolean isTransformed(String s) {
            return normalizer.isNormalized(s);
        }
    }
}
