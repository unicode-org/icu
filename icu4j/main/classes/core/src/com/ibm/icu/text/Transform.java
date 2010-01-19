/*
 ********************************************************************************
 * Copyright (C) 2009-2010, Google, International Business Machines Corporation *
 * and others. All Rights Reserved.                                             *
 ********************************************************************************
 */
package com.ibm.icu.text;

/**
 * Provide an interface for Transforms that focuses just on the transformation of the text.
 * APIs that take Transliterator or StringTransform, but only depend on the transformation should use this interface in the API instead.
 *
 * @author markdavis
 * @draft ICU 4.4
 * @provisional This API might change or be removed in a future release.
 *
 */

public interface Transform<S,D> {
    /**
     * Transform the input in some way, to be determined by the subclass.
     * @param source to be transformed (eg lowercased)
     * @return result
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public D transform(S source);
}
