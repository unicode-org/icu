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
 */
public class UFieldPosition extends FieldPosition {
    private int countVisibleFractionDigits = -1;
    private long fractionDigits = 0;

    public UFieldPosition() {
        super(-1);
    }
    public UFieldPosition(int field) {
        super(field);
    }
    public UFieldPosition(Field attribute, int fieldID) {
        super(attribute, fieldID);
    }
    public UFieldPosition(Field attribute) {
        super(attribute);
    }
    public void setFractionDigits(int countVisibleFractionDigits, long fractionDigits ) {
        this.countVisibleFractionDigits = countVisibleFractionDigits;
        this.fractionDigits = fractionDigits;
    }
    public int getCountVisibleFractionDigits() {
        return countVisibleFractionDigits;
    }
    public long getFractionDigits() {
        return fractionDigits;
    }
}
