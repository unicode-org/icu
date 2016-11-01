/*
 *******************************************************************************
 * Copyright (C) 2011-2012, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.util;

import com.ibm.icu.dev.util.UnicodeTransform.Type;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Normalizer2;

/**
 * @author markdavis
 *
 */
public class IcuUnicodeNormalizerFactory implements UnicodeTransform.Factory {

    public UnicodeTransform getInstance(Type type) {
        switch (type) {
        case NFC:
            return new IcuUnicodeNormalizer(Normalizer2.getNFCInstance());
        case NFKC:
            return new IcuUnicodeNormalizer(Normalizer2.getNFKCInstance());
        case NFD:
            return new IcuUnicodeNormalizer(Normalizer2.getNFDInstance());
        case NFKD:
            return new IcuUnicodeNormalizer(Normalizer2.getNFKDInstance());
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
