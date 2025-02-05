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
 * Sets additional options for the unit conversion.
 * 
 * @draft ICU 77
 */
public class CompoundUnitsConverterBuilder {
    private MeasureUnit from;
    private MeasureUnit to;

    protected CompoundUnitsConverterBuilder(MeasureUnit from, MeasureUnit to) {
        this.from = from;
        this.to = to;
    }

    /**
     * Builds the CompoundUnitsConverter with the specified options.
     * 
     * @return A CompoundUnitsConverter object.
     * @throws IllegalArgumentException if any errors such as non-convertible units
     *                                  are encountered.
     * @draft ICU 77
     */
    public CompoundUnitsConverter build() {
        return new CompoundUnitsConverter(this.from, this.to);
    }
}
