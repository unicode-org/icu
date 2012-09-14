/*
 *******************************************************************************
 * Copyright (C) 2011-2012, Google, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.util;

import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UTF16;

/**
 * Simple wrapping for normalizer that allows for both the standard ICU normalizer, and one built directly from the UCD.
 */
public abstract class UnicodeTransform implements Transform<String,String> {
    public enum Type {
        NFD, NFC, NFKD, NFKC, CASEFOLD
    }
    
    public interface Factory {
        public UnicodeTransform getInstance(Type type);
    }
    
    private static Factory factory = new IcuUnicodeNormalizerFactory();
    
    public static synchronized Factory getFactory() {
        return factory;
    }

    public static synchronized void setFactory(Factory factory) {
        UnicodeTransform.factory = factory;
    }

    public static synchronized UnicodeTransform getInstance(Type type) {
        return factory.getInstance(type);
    }
    
    public abstract String transform(String source);
    
    /**
     * Can be overridden for performance.
     */
    public boolean isTransformed(String source) {
        return source.equals(transform(source));
    }
    /**
     * Can be overridden for performance.
     */
    public String transform(int source) {
        return transform(UTF16.valueOf(source));
    }
    /**
     * Can be overridden for performance.
     */
    public boolean isTransformed(int source) {
        return isTransformed(UTF16.valueOf(source));
    }
}

