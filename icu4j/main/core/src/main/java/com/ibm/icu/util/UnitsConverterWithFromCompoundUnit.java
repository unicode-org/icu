// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1996-2014, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.util;

/**
 * Specifies the unit to convert to.
 * 
 * @draft ICU 77
 */
public class UnitsConverterWithFromCompoundUnit {
    private MeasureUnit from;

    protected UnitsConverterWithFromCompoundUnit(MeasureUnit from) {
        this.from = from;
    }

    /**
     * Sets the target unit for conversion.
     * 
     * @param to: The unit to convert to.
     * @return: A SingleUnitsConverterOptions object
     * @draft ICU 77
     */
    public CompoundUnitsConverterBuilder toCompoundUnit(MeasureUnit to) {
        return new CompoundUnitsConverterBuilder(from, to);
    }

}
