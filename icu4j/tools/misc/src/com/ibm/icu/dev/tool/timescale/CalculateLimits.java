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

import com.ibm.icu.math.BigDecimal;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.util.UniversalTimeScale;

/**
 * This class calculates the minimum and maximum values which can be
 * used as arguments to <code>toLong</code> and <code>from</code>.
 * 
 * NOTE: If you change the way in which these values are calculated, it
 * may be necessary to disable to <code>toRangeCheck()</code> and
 * <code>fromRangeCheck()</code> methods in the <code>UniversalTimeScale</code>
 * for all of the calculations to run without throwing an error.
 * 
 * @see com.ibm.icu.util.UniversalTimeScale
 */
public class CalculateLimits {

    /**
     * The default constructor.
     */
    public CalculateLimits()
    {
    }

    /**
     * This method first calculates the <code>from</code> limits by
     * passing <code>Long.MIN_VALUE</code> and <code>Long.MAX_VALUE</code> to
     * the (internal) <code>toBigDecimalTrunc()</code> method. Any values outside
     * of the range of a <code>long</code> are pinned.
     * 
     * The mimimum and maximum values for <code>toLong</code> are calulated by passing
     * the min and max values calculated above to <code>BigDecimalFrom()</code>. Because
     * this method will round, the returned values are adjusted to take this into account.
     * 
     * @see com.ibm.icu.util.UniversalTimeScale
     * 
     * @param args - the command line arugments
     */
    public static void main(String[] args)
    {
        MessageFormat fmt = new MessageFormat("{0}L, {1}L, {2}L, {3}L");        
        BigDecimal universalMin = new BigDecimal(Long.MIN_VALUE);
        BigDecimal universalMax = new BigDecimal(Long.MAX_VALUE);
        Object limitArgs[] = {null, null, null, null};
        
        System.out.println("\nTo, From limits:");
        
        // from limits
        for(int scale = 0; scale < UniversalTimeScale.MAX_SCALE; scale += 1) {
            BigDecimal min = UniversalTimeScale.toBigDecimalTrunc(universalMin, scale).max(universalMin);
            BigDecimal max = UniversalTimeScale.toBigDecimalTrunc(universalMax, scale).min(universalMax);
            long minLong   = min.longValue();
            long maxLong   = max.longValue();
            
            limitArgs[2] = min.toString();
            limitArgs[3] = max.toString();

            // to limits
            BigDecimal minTrunc   = UniversalTimeScale.bigDecimalFrom(min, scale);
            BigDecimal maxTrunc   = UniversalTimeScale.bigDecimalFrom(max, scale);
            BigDecimal minResidue = minTrunc.subtract(universalMin);
            BigDecimal maxResidue = universalMax.subtract(maxTrunc);
            long units            = UniversalTimeScale.getTimeScaleValue(scale, UniversalTimeScale.UNITS_VALUE);
            BigDecimal half       = new BigDecimal(units == 1? 0: units / 2 - 1);
            
            min = minTrunc.subtract(minResidue.min(half));
            max = maxTrunc.add(maxResidue.min(half));
            limitArgs[0] = min.toString();
            limitArgs[1] = max.toString();
            
            System.out.println(fmt.format(limitArgs));
            
            // round-trip test the from limits
            if(UniversalTimeScale.toLong(UniversalTimeScale.from(minLong, scale), scale) != minLong) {
                System.out.println("OOPS: min didn't round trip!");
            }
            
            if(UniversalTimeScale.toLong(UniversalTimeScale.from(maxLong, scale), scale) != maxLong) {
                System.out.println("OOPS: max didn't round trip!");
            }
            
            // make sure that the to limits convert to the from limits
            if(UniversalTimeScale.toLong(min.longValue(), scale) != minLong) {
                System.out.println("OOPS: toLong(toMin) != fromMin");
            }
            
            if(UniversalTimeScale.toLong(max.longValue(), scale) != maxLong) {
                System.out.println("OOPS: toLong(toMax) != fromMax");
            }
        }
    }
}
