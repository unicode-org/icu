/*
 *******************************************************************************
 * Copyright (C) 2013, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.util;

/**
 * General purpose formatting width enum.
 */
public enum FormatWidth {
    WIDE("units"), 
    SHORT("unitsShort"), 
    NARROW("unitsNarrow");

    /**
     * @internal
     * @deprecated internal use
     */
    public final String resourceKey;

    private FormatWidth(String resourceKey) {
        this.resourceKey = resourceKey;
    }
}