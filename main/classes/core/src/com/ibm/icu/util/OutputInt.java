// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
/*
 *******************************************************************************
 * Copyright (C) 2014, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.util;

/**
 * Simple struct-like class for int output parameters.
 * Like <code>Output&lt;Integer&gt;</code> but without auto-boxing.
 *
 * @internal but could become public
 * @deprecated This API is ICU internal only.
 */
@Deprecated
public class OutputInt {
    /**
     * The value field.
     *
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public int value;

    /**
     * Constructs an <code>OutputInt</code> with value 0.
     *
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public OutputInt() {
    }

    /**
     * Constructs an <code>OutputInt</code> with the given value.
     *
     * @param value the initial value
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public OutputInt(int value) {
        this.value = value;
    }

    /**
     * {@inheritDoc}
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
