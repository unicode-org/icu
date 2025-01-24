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
 * Initiates the conversion process between different measurement units.
 *
 * @draft ICU 77
 */
public class UnitsConverter {
    /**
     * Creates a UnitsConverterWithFromCompoundUnit object using the specified unit.
     * 
     * @param from The unit to convert from.
     * @return A `UnitsConverterWithFromCompoundUnit` object.
     * @draft ICU 77
     */
    static public UnitsConverterWithFromCompoundUnit fromCompoundUnit(MeasureUnit from) {
        return new UnitsConverterWithFromCompoundUnit(from);
    }
}
