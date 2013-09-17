/*
 *******************************************************************************
 * Copyright (C) 2013, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.util;

/**
 * General purpose formatting width enum.
 * @internal
 * @deprecated This API is ICU internal only.
 */
public enum FormatWidth {
    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    WIDE("units"), 
    
    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    SHORT("unitsShort"), 
    
    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
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