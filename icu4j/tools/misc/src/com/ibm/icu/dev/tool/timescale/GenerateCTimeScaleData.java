// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1996-2008, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 */

package com.ibm.icu.dev.tool.timescale;

import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.util.UniversalTimeScale;

/**
 * This class prints out the initializers needed to initialize
 * the time scale data in the C version of <code>UniversalTimeScale</code>.
 * 
 * It just calls <code>getTimeScaleValue()</code> for all fields and prints
 * the initializers. Because some C compilers can't compile a literal constant for
 * the minimum and / or maximum values of an <code>int64_t</code>, this code will
 * print <code>U_INT64_MIN</code> or <code>U_INT64_MAX</code> for these values.
 * 
 * @see com.ibm.icu.util.UniversalTimeScale
 */
public class GenerateCTimeScaleData
{

    /**
     * The default constructor.
     */
    public GenerateCTimeScaleData()
    {
    }

    private static final long ticks        = 1;
    private static final long microseconds = ticks * 10;
    private static final long milliseconds = microseconds * 1000;
    private static final long seconds      = milliseconds * 1000;
    private static final long minutes      = seconds * 60;
    private static final long hours        = minutes * 60;
    private static final long days         = hours * 24;

    /*
     * Returns <code>String</code> that is a literal representation of the given value.
     * This will either be a call to the <code>INT64_C()</code> macro, or the constant
     * <code>U_INT64_MIN</code> or <U_INT64_MAX>.
     */
    private static String minMaxFilter(long value)
    {
        if (value == Long.MIN_VALUE) {
            return "U_INT64_MIN";
        } else if (value == Long.MAX_VALUE) {
            return "U_INT64_MAX";
        }
        
        return "INT64_C(" + Long.toString(value) + ")";
    }
    
    /**
     * This method prints the C initializers for the time scale data.
     * 
     * @param args - the command line arguments
     * 
     * @see com.ibm.icu.util.UniversalTimeScale
     */
    public static void main(String[] args)
    {
        MessageFormat fmt = new MessageFormat("'{'{0}, {1}, {2}, {3}, {4}, {5}, {6}, {7}, {8}, {9}, {10}},");
        Object cargs[] = {null, null, null, null, null, null, null, null, null, null, null};
        
        System.out.println("\nC data:");
        
        for (int scale = 0; scale < UniversalTimeScale.MAX_SCALE; scale += 1) {
            long units = UniversalTimeScale.getTimeScaleValue(scale, UniversalTimeScale.UNITS_VALUE);
            
            if (units == ticks) {
                cargs[0] = "ticks";
            } else if (units == microseconds) {
                cargs[0] = "microseconds";
            } else if (units == milliseconds) {
                cargs[0] = "milliseconds";
            } else if (units == seconds) {
                cargs[0] = "seconds";
            } else if (units == minutes) {
                cargs[0] = "minutes";
            } else if (units == hours) {
                cargs[0] = "hours";
            } else if (units == days) {
                cargs[0] = "days";
            } else {
                cargs[0] = "INT64_C(" + Long.toString(units) + ")";
            }
            
            cargs[1]  = minMaxFilter(UniversalTimeScale.getTimeScaleValue(scale, UniversalTimeScale.EPOCH_OFFSET_VALUE));
            cargs[2]  = minMaxFilter(UniversalTimeScale.getTimeScaleValue(scale, UniversalTimeScale.FROM_MIN_VALUE));
            cargs[3]  = minMaxFilter(UniversalTimeScale.getTimeScaleValue(scale, UniversalTimeScale.FROM_MAX_VALUE));
            cargs[4]  = minMaxFilter(UniversalTimeScale.getTimeScaleValue(scale, UniversalTimeScale.TO_MIN_VALUE));
            cargs[5]  = minMaxFilter(UniversalTimeScale.getTimeScaleValue(scale, UniversalTimeScale.TO_MAX_VALUE));
            cargs[6]  = minMaxFilter(UniversalTimeScale.getTimeScaleValue(scale, UniversalTimeScale.EPOCH_OFFSET_PLUS_1_VALUE));
            cargs[7]  = minMaxFilter(UniversalTimeScale.getTimeScaleValue(scale, UniversalTimeScale.EPOCH_OFFSET_MINUS_1_VALUE));
            cargs[8]  = minMaxFilter(UniversalTimeScale.getTimeScaleValue(scale, UniversalTimeScale.UNITS_ROUND_VALUE));
            cargs[9]  = minMaxFilter(UniversalTimeScale.getTimeScaleValue(scale, UniversalTimeScale.MIN_ROUND_VALUE));
            cargs[10] = minMaxFilter(UniversalTimeScale.getTimeScaleValue(scale, UniversalTimeScale.MAX_ROUND_VALUE));
            
            System.out.println(fmt.format(cargs));
        }
    }
}
