/*
 *******************************************************************************
 * Copyright (C) 2011, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.util;

/**
 * Simple struct-like class for output parameters.
 * @param <T> The type of the parameter.
 * @draft ICU 4.8
 * @provisional This API might change or be removed in a future release.
 */
public class Output<T> {
    /**
     * The value field
     * @draft ICU 4.8
     * @provisional This API might change or be removed in a future release.
     */
    public T value;

    /**
     * {@inheritDoc}
     * @draft ICU 4.8
     * @provisional This API might change or be removed in a future release.
     */
    public String toString() {
        return value == null ? "null" : value.toString();
    }

    /**
     * Constructs an empty <code>Output</code>
     * @draft ICU 4.8
     * @provisional This API might change or be removed in a future release.
     */
    public Output() {
        
    }

    /**
     * Constructs an <code>Output</code> withe the given value.
     * @param value the initial value
     * @draft ICU 4.8
     * @provisional This API might change or be removed in a future release.
     */
    public Output(T value) {
        this.value = value;
    }
}
