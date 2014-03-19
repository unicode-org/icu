/*
 *******************************************************************************
 * Copyright (C) 2011-2012, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.util;

/**
 * Simple struct-like class for output parameters.
 * @param <T> The type of the parameter.
 * @stable ICU 4.8
 */
public class Output<T> {
    /**
     * The value field
     * @stable ICU 4.8
     */
    public T value;

    /**
     * {@inheritDoc}
     * @stable ICU 4.8
     */
    public String toString() {
        return value == null ? "null" : value.toString();
    }

    /**
     * Constructs an empty <code>Output</code>
     * @stable ICU 4.8
     */
    public Output() {
        
    }

    /**
     * Constructs an <code>Output</code> withe the given value.
     * @param value the initial value
     * @stable ICU 4.8
     */
    public Output(T value) {
        this.value = value;
    }
}
