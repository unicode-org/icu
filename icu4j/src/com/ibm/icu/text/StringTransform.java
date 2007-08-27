/*
 *******************************************************************************
 * Copyright (C) 1996-2007, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

/**
 * Provide a base class for Transforms that focuses just on the transformation of the text. APIs that take Transliterator, but only depend on the text transformation should use this interface in the API instead.
 *
 * @draft ICU 3.8
 * @provisional This API might change or be removed in a future release.
 * @author markdavis
 *
 */
public interface StringTransform {
    /**
     * Transform the text in some way, to be determined by the subclass.
     * @param source text to be transformed (eg lowercased)
     * @return result
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public String transform(String source);
}