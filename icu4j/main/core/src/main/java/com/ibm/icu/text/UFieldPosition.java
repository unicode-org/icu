// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2013-2014, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.text.FieldPosition;
import java.text.Format.Field;

/**
 * Adds the ability to get the decimal digits
 * @internal
 * @deprecated This API is ICU internal only.
 */
@Deprecated
public class UFieldPosition extends FieldPosition {
    private int countVisibleFractionDigits = -1;
    private long fractionDigits = 0;

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public UFieldPosition() {
        super(-1);
    }

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public UFieldPosition(int field) {
        super(field);
    }

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public UFieldPosition(Field attribute, int fieldID) {
        super(attribute, fieldID);
    }

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public UFieldPosition(Field attribute) {
        super(attribute);
    }

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public void setFractionDigits(int countVisibleFractionDigits, long fractionDigits ) {
        this.countVisibleFractionDigits = countVisibleFractionDigits;
        this.fractionDigits = fractionDigits;
    }

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public int getCountVisibleFractionDigits() {
        return countVisibleFractionDigits;
    }

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public long getFractionDigits() {
        return fractionDigits;
    }
}
