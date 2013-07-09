/*
 *******************************************************************************
 * Copyright (C) 2013, International Business Machines Corporation and         *
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
public class UFieldPosition extends FieldPosition {
    private int countVisibleFractionDigits = -1;
    private long fractionDigits = 0;

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public UFieldPosition() {
        super(-1);
    }

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public UFieldPosition(int field) {
        super(field);
    }

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public UFieldPosition(Field attribute, int fieldID) {
        super(attribute, fieldID);
    }

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public UFieldPosition(Field attribute) {
        super(attribute);
    }

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public void setFractionDigits(int countVisibleFractionDigits, long fractionDigits ) {
        this.countVisibleFractionDigits = countVisibleFractionDigits;
        this.fractionDigits = fractionDigits;
    }

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public int getCountVisibleFractionDigits() {
        return countVisibleFractionDigits;
    }

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public long getFractionDigits() {
        return fractionDigits;
    }
}
