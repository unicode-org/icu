/*
 **************************************************************************
 * Copyright (C) 2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                           *
 **************************************************************************
 *
 */

package com.ibm.icu.util;

import com.ibm.icu.math.BigDecimal;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.SimpleTimeZone;
import java.lang.IllegalArgumentException;
import java.util.Locale;

/** 
 * There are quite a few different conventions for binary datetime, depending on different
 * platforms and protocols. Some of these have severe drawbacks. For example, people using
 * Unix time (seconds since Jan 1, 1970) think that they are safe until near the year 2038.
 * But cases can and do arise where arithmetic manipulations causes serious problems. Consider
 * the computation of the average of two datetimes, for example: if one calculates them with
 * <code>averageTime = (time1 + time2)/2</code>, there will be overflow even with dates
 * around the present. Moreover, even if these problems don't occur, there is the issue of
 * conversion back and forth between different systems.
 *
 * <p>
 * Binary datetimes differ in a number of ways: the datatype, the unit,
 * and the epoch (origin). We'll refer to these as time scales. For example:
 *
 * <table border="1" cellspacing="0" cellpadding="4">
 *  <caption>
 *    <h3>Table 1: Binary Time Scales</h3>
 *
 *  </caption>
 *  <tr>
 *    <th align="left">Source</th>
 *    <th align="left">Datatype</th>
 *    <th align="left">Unit</th>
 *    <th align="left">Epoch</th>
 *  </tr>
 *
 *  <tr>
 *    <td>JAVA_TIME</td>
 *    <td>long</td>
 *    <td>milliseconds</td>
 *    <td>Jan 1, 1970</td>
 *  </tr>
 *  <tr>
 *
 *    <td>UNIX_TIME</td>
 *    <td>int or long</td>
 *    <td>seconds</td>
 *    <td>Jan 1, 1970</td>
 *  </tr>
 *  <tr>
 *    <td>ICU4C</td>
 *
 *    <td>double</td>
 *    <td>milliseconds</td>
 *    <td>Jan 1, 1970</td>
 *  </tr>
 *  <tr>
 *    <td>WINDOWS_FILE_TIME</td>
 *    <td>long</td>
 *
 *    <td>ticks (100 nanoseconds)</td>
 *    <td>Jan 1, 1601</td>
 *  </tr>
 *  <tr>
 *    <td>WINDOWS_DATE_TIME</td>
 *    <td>long</td>
 *    <td>ticks (100 nanoseconds)</td>
 *
 *    <td>Jan 1, 0001</td>
 *  </tr>
 *  <tr>
 *    <td>MAC_OLD_TIME</td>
 *    <td>int</td>
 *    <td>seconds</td>
 *    <td>Jan 1, 1904</td>
 *
 *  </tr>
 *  <tr>
 *    <td>MAC_TIME</td>
 *    <td>double</td>
 *    <td>seconds</td>
 *    <td>Jan 1, 2001</td>
 *  </tr>
 *
 *  <tr>
 *    <td>EXCEL_TIME</td>
 *    <td>?</td>
 *    <td>days</td>
 *    <td>Dec 31, 1899</td>
 *  </tr>
 *  <tr>
 *
 *    <td>DB2_TIME</td>
 *    <td>?</td>
 *    <td>days</td>
 *    <td>Dec 31, 1899</td>
 *  </tr>
 * </table>
 *
 * <p>
 * All of the epochs start at 00:00 am (the earliest possible time on the day in question),
 * and are assumed to be UTC.
 *
 * <p>
 * The ranges for different datatypes are given in the following table (all values in years).
 * The range of years includes the entire range expressible with positive and negative
 * values of the datatype. The range of years for double is the range that would be allowed
 * without losing precision to the corresponding unit.
 *
 * <table border="1" cellspacing="0" cellpadding="4">
 *  <tr>
 *    <th align="left">Units</th>
 *    <th align="left">long</th>
 *    <th align="left">double</th>
 *    <th align="left">int</th>
 *  </tr>
 *
 *  <tr>
 *    <td>1 sec</td>
 *    <td align="right">5.84542×10¹¹</td>
 *    <td align="right">285,420,920.94</td>
 *    <td align="right">136.10</td>
 *  </tr>
 *  <tr>
 *
 *    <td>1 millisecond</td>
 *    <td align="right">584,542,046.09</td>
 *    <td align="right">285,420.92</td>
 *    <td align="right">0.14</td>
 *  </tr>
 *  <tr>
 *    <td>1 microsecond</td>
 *
 *    <td align="right">584,542.05</td>
 *    <td align="right">285.42</td>
 *    <td align="right">0.00</td>
 *  </tr>
 *  <tr>
 *    <td>100 nanoseconds (tick)</td>
 *    <td align="right">58,454.20</td>
 *    <td align="right">28.54</td>
 *    <td align="right">0.00</td>
 *  </tr>
 *  <tr>
 *    <td>1 nanosecond</td>
 *    <td align="right">584.5420461</td>
 *    <td align="right">0.2854</td>
 *    <td align="right">0.00</td>
 *  </tr>
 * </table>
 *
 * <p>
 * This class implements a universal time scale which can be used as a 'pivot',
 * and provide conversion functions to and from all other major time scales.
 * This datetimes to be converted to the pivot time, safely manipulated,
 * and converted back to any other datetime time scale.
 *
 *<p>
 * So what to use for this pivot? Java time has plenty of range, but cannot represent
 * Windows datetimes without severe loss of precision. ICU4C time addresses this by using a
 * <code>double</code> that is otherwise equivalent to the Java time. However, there are disadvantages
 * with <code>doubles</code>. They provide for much more graceful degradation in arithmetic operations.
 * But they only have 53 bits of accuracy, which means that they will lose precision when
 * converting back and forth to ticks. What would really be nice would be a
 * <code>long double</code> (80 bits -- 64 bit mantissa), but that is not supported on most systems.
 *
 *<p>
 * The Unix extended time uses a structure with two components: time in seconds and a
 * fractional field (microseconds). However, this is clumsy, slow, and
 * prone to error (you always have to keep track of overflow and underflow in the
 * fractional field). <code>BigDecimal</code> would allow for arbitrary precision and arbitrary range,
 * but we would not want to use this as the normal type, because it is slow and does not
 * have a fixed size.
 *
 *<p>
 * Because of these issues, we ended up concluding that the Windows datetime would be the
 * best pivot. However, we use the full range allowed by the datatype, allowing for
 * datetimes back to 29,000 BC and up to 29,000 AD. This time scale is very fine grained,
 * does not lose precision, and covers a range that will meet almost all requirements.
 * It will not handle the range that Java times would, but frankly, being able to handle dates
 * before 29,000 BC or after 29,000 AD is of very limited interest. However, for those cases,
 * we also allow conversion to an optional <code>BigDecimal</code> format that would have arbitrary
 * precision and range.
 *
 */

public final class UniversalTimeScale
{
    /**
     * Used in the JDK. Data is a <code>long</code>. Value
     * is milliseconds since January 1, 1970.
     *
     * @draft ICU 3.2
     */
    static final public int JAVA_TIME = 0;

    /**
     * Used in Unix systems. Data is an <code>int> or a <code>long</code>. Value
     * is seconds since January 1, 1970.
     *
     * @draft ICU 3.2
     */
    static final public int UNIX_TIME = 1;

    /**
     * Used in the ICU4C. Data is a <code>double</code>. Value
     * is milliseconds since January 1, 1970.
     *
     * @draft ICU 3.2
     */
    static final public int ICU4C_TIME = 2;

    /**
     * Used in Windows for file times. Data is a <code>long</code>. Value
     * is ticks (1 tick == 100 nanoseconds) since January 1, 1601.
     *
     * @draft ICU 3.2
     */
    static final public int WINDOWS_FILE_TIME = 3;

    /**
     * Used in Windows for date time (?). Data is a <code>long</code>. Value
     * is ticks (1 tick == 100 nanoseconds) since January 1, 0001.
     *
     * @draft ICU 3.2
     */
    static final public int WINDOWS_DATE_TIME = 4;

    /**
     * Used in older Macintosh systems. Data is an <code>int</code>. Value
     * is seconds since January 1, 1904.
     *
     * @draft ICU 3.2
     */
    static final public int MAC_OLD_TIME = 5;

    /**
     * Used in the JDK. Data is a <code>double</code>. Value
     * is milliseconds since January 1, 2001.
     *
     * @draft ICU 3.2
     */
    static final public int MAC_TIME = 6;

    /**
     * Used in Excel. Data is a <code>?unknown?</code>. Value
     * is days since December 31, 1899.
     *
     * @draft ICU 3.2
     */
    static final public int EXCEL_TIME = 7;

    /**
     * Used in DB2. Data is a <code>?unknown?</code>. Value
     * is days since December 31, 1899.
     *
     * @draft ICU 3.2
     */
    static final public int DB2_TIME = 8;
    
    /**
     * This is the first unused time scale value.
     *
     * @draft ICU 3.2
     */
    static final public int MAX_SCALE = 9;
    
    private static final long ticks        = 1;
    private static final long microseconds = ticks * 10;
    private static final long milliseconds = microseconds * 1000;
    private static final long seconds      = milliseconds * 1000;
    private static final long minutes      = seconds * 60;
    private static final long hours        = minutes * 60;
    private static final long days         = hours * 24;
    
    /**
     * This class holds the data that describes a particular
     * time scale.
     *
     * @draft ICU 3.2
     */
    public static final class TimeScaleData
    {
        TimeScaleData(long theUnits, long theEpochOffset,
                       long theToMin, long theToMax,
                       long theFromMin, long theFromMax)
        {
            units      = theUnits;
            unitsRound = theUnits / 2;
            
            minRound = Long.MIN_VALUE + unitsRound;
            maxRound = Long.MAX_VALUE - unitsRound;
                        
            epochOffset   = theEpochOffset / theUnits;
            
            if (theUnits == 1) {
                epochOffsetP1 = epochOffsetM1 = epochOffset;
            } else {
                epochOffsetP1 = epochOffset + 1;
                epochOffsetM1 = epochOffset - 1;
            }
            
            toMin = theToMin;
            toMax = theToMax;
            
            fromMin = theFromMin;
            fromMax = theFromMax;
        }
        
        /**
         * The units of the time scale, expressed in ticks.
         * 
         * @draft ICU 3.2
         */
        public long units;
        
        /**
         * The distance from the Universal Time Scale's epoch to the
         * time scale's epoch expressed in the time scale's units.
         * 
         * @draft ICU 3.2
         */
        public long epochOffset;
        
        /**
         * The minimum time scale value that can be conveted
         * to the Universal Time Scale without underflowing.
         * 
         * @draft ICU 3.2
         */
        public long fromMin;
        
        /**
         * The maximum time scale value that can be conveted
         * to the Universal Time Scale without overflowing.
         * 
         * @draft ICU 3.2
         */
        public long fromMax;
        
        /**
         * The minimum Universal Time Scale value that can
         * be converted to the time scale without underflowing.
         * 
         * @draft ICU 3.2
         */
        public long toMin;
        
        /**
         * The maximum Universal Time Scale value that can
         * be converted to the time scale without overflowing.
         * 
         * @draft ICU 3.2
         */
        public long toMax;
        
        long epochOffsetP1;
        long epochOffsetM1;
        long unitsRound;
        long minRound;
        long maxRound;
    }
    
    private static final TimeScaleData[] timeScaleTable = {
            new TimeScaleData(milliseconds, 621357696000000000L, -9223372036854774999L, 9223372036854774999L, -984472973285477L,         860201434085477L), // JAVA_TIME
            new TimeScaleData(seconds,      621357696000000000L, -9223372036854775808L, 9223372036854775807L, -984472973285L,               860201434085L), // UNIX_TIME
            new TimeScaleData(milliseconds, 621357696000000000L, -9223372036854774999L, 9223372036854774999L, -984472973285477L,         860201434085477L), // ICU4C_TIME
            new TimeScaleData(ticks,        504912960000000000L, -8718459076854775808L, 9223372036854775807L, -9223372036854775808L, 8718459076854775807L), // WINDOWS_FILE_TIME
            new TimeScaleData(ticks,        000000000000000000L, -9223372036854775808L, 9223372036854775807L, -9223372036854775808L, 9223372036854775807L), // WINDOWS_DATE_TIME
            new TimeScaleData(seconds,      600529248000000000L, -9223372036854775808L, 9223372036854775807L, -982390128485L,               862284278885L), // MAC_OLD_TIME
            new TimeScaleData(seconds,      631140768000000000L, -9223372036854775808L, 9223372036854775807L, -985451280485L,               859223126885L), // MAC_TIME
            new TimeScaleData(days,         599266944000000000L, -9223372036854775808L, 9223372036854775807L, -11368795L,                        9981603L), // EXCEL_TIME
            new TimeScaleData(days,         599266944000000000L, -9223372036854775808L, 9223372036854775807L, -11368795L,                        9981603L)  // DB2_TIME
    };
    
    /**
     * Convert a <code>double</code> datetime from the given time scale to the universal time scale.
     *
     * @param otherTime The <code>double</code> datetime
     * @param timeScale The time scale to convert from
     * 
     * @return The datetime converted to the universal time scale
     *
     * @draft ICU 3.2
     */
    static public long from(double otherTime, int timeScale)
    {
        TimeScaleData data = fromRangeCheck(otherTime, timeScale);
        
        return ((long)otherTime + data.epochOffset) * data.units;
    }

    /**
     * Convert a <code>long</code> datetime from the given time scale to the universal time scale.
     *
     * @param otherTime The <code>long</code> datetime
     * @param timeScale The time scale to convert from
     * 
     * @return The datetime converted to the universal time scale
     *
     * @draft ICU 3.2
     */
    static public long from(long otherTime, int timeScale)
    {
        TimeScaleData data = fromRangeCheck(otherTime, timeScale);
                
        return (otherTime + data.epochOffset) * data.units;
    }

    /**
     * Convert a <code>double</code> datetime from the given time scale to the universal time scale.
     * All calculations are done using <code>BigDecimal</code> to guarantee that the value
     * does not go out of range.
     *
     * @param otherTime The <code>double</code> datetime
     * @param timeScale The time scale to convert from
     * 
     * @return The datetime converted to the universal time scale
     *
     * @draft ICU 3.2
     */
    static public BigDecimal bigDecimalFrom(double otherTime, int timeScale)
    {
        TimeScaleData data     = getTimeScaleData(timeScale);
        BigDecimal other       = new BigDecimal(otherTime);
        BigDecimal units       = new BigDecimal(data.units);
        BigDecimal epochOffset = new BigDecimal(data.epochOffset);
        
        return other.add(epochOffset).multiply(units);
    }

    /**
     * Convert a <code>long</code> datetime from the given time scale to the universal time scale.
     * All calculations are done using <code>BigDecimal</code> to guarantee that the value
     * does not go out of range.
     *
     * @param otherTime The <code>long</code> datetime
     * @param timeScale The time scale to convert from
     * 
     * @return The datetime converted to the universal time scale
     *
     * @draft ICU 3.2
     */
    static public BigDecimal bigDecimalFrom(long otherTime, int timeScale)
    {
        TimeScaleData data     = getTimeScaleData(timeScale);
        BigDecimal other       = new BigDecimal(otherTime);
        BigDecimal units       = new BigDecimal(data.units);
        BigDecimal epochOffset = new BigDecimal(data.epochOffset);
        
        return other.add(epochOffset).multiply(units);
    }

    /**
     * Convert a <code>BigDecimal</code> datetime from the given time scale to the universal time scale.
     * All calculations are done using <code>BigDecimal</code> to guarantee that the value
     * does not go out of range.
     *
     * @param otherTime The <code>BigDecimal</code> datetime
     * @param timeScale The time scale to convert from
     * 
     * @return The datetime converted to the universal time scale
     *
     * @draft ICU 3.2
     */
    static public BigDecimal bigDecimalFrom(BigDecimal otherTime, int timeScale)
    {
        TimeScaleData data = getTimeScaleData(timeScale);
        
        BigDecimal units = new BigDecimal(data.units);
        BigDecimal epochOffset = new BigDecimal(data.epochOffset);
        
        return otherTime.add(epochOffset).multiply(units);
    }

    /**
     * Convert a datetime from the universal time scale to a <code>double</code> in the given
     * time scale.
     * 
     * Since this calculation requires a divide, we must round. The straight forward
     * way to round by adding half of the divisor will push the sum out of range for values
     * within half the divisor of the limits of the precision of a <code>long</code>. To get around this, we do
     * the rounding like this:
     * 
     * <p><code>
     * (universalTime - units + units/2) / units + 1
     * </code>
     * 
     * <p>
     * (i.e. we subtract units first to guarantee that we'll still be in range when we
     * add <code>units/2</code>. We then need to add one to the quotent to make up for the extra subtraction.
     * This simplifies to:
     * 
     * <p><code>
     * (universalTime - units/2) / units - 1
     * </code>
     * 
     * <p>
     * For negative values to round away from zero, we need to flip the signs:
     * 
     * <p><code>
     * (universalTime + units/2) / units + 1
     * </code>
     * 
     * <p>
     * Since we also need to subtract the epochOffset, we fold the <code>+/- 1</code>
     * into the offset value. (i.e. <code>epochOffsetP1</code>, <code>epochOffsetM1</code>.)
     *
     * @param universal The datetime in the universal time scale
     * @param timeScale The time scale to convert to
     * 
     * @return The datetime converted to the given time scale
     *
     * @draft ICU 3.2
     */
    static public double toDouble(long universalTime, int timeScale)
    {
        TimeScaleData data = toRangeCheck(universalTime, timeScale);
        
        if (universalTime < 0) {
            if (universalTime < data.minRound) {
                return (universalTime + data.unitsRound) / data.units - data.epochOffsetP1;
            }
            
            return (universalTime - data.unitsRound) / data.units - data.epochOffset;
        }
        
        if (universalTime > data.maxRound) {
            return (universalTime - data.unitsRound) / data.units - data.epochOffsetM1;
        }
        
        return (universalTime + data.unitsRound) / data.units - data.epochOffset;
    }
    
    /**
     * Convert a datetime from the universal time scale stored as a <code>BigDecimal</code> to a
     * <code>double</code> in the given time scale.
     *
     * @param universal The datetime in the universal time scale
     * @param timeScale The time scale to convert to
     * 
     * @return The datetime converted to the given time scale
     *
     * @draft ICU 3.2
     */
    static private double toDouble(BigDecimal universalTime, int timeScale)
    {
        TimeScaleData data = getTimeScaleData(timeScale);
        BigDecimal units = new BigDecimal(data.units);
        BigDecimal epochOffset = new BigDecimal(data.epochOffset);
        
        return universalTime.divide(units, BigDecimal.ROUND_HALF_UP).subtract(epochOffset).doubleValue();
    }

    /**
     * Convert a datetime from the universal time scale stored as a <code>BigDecimal</code> to a
     * <code>long</code> in the given time scale.
     *
     * Since this calculation requires a divide, we must round. The straight forward
     * way to round by adding half of the divisor will push the sum out of range for values
     * within have the divisor of the limits of the precision of a <code>long</code>. To get around this, we do
     * the rounding like this:
     * 
     * <p><code>
     * (universalTime - units + units/2) / units + 1
     * </code>
     * 
     * <p>
     * (i.e. we subtract units first to guarantee that we'll still be in range when we
     * add <code>units/2</code>. We then need to add one to the quotent to make up for the extra subtraction.
     * This simplifies to:
     * 
     * <p><code>
     * (universalTime - units/2) / units - 1
     * </code>
     * 
     * <p>
     * For negative values to round away from zero, we need to flip the signs:
     * 
     * <p><code>
     * (universalTime + units/2) / units + 1
     * </code>
     * 
     * <p>
     * Since we also need to subtract the epochOffset, we fold the <code>+/- 1</code>
     * into the offset value. (i.e. <code>epochOffsetP1</code>, <code>epochOffsetM1</code>.)
     * 
     * @param universal The datetime in the universal time scale
     * @param timeScale The time scale to convert to
     * 
     * @return The datetime converted to the given time scale
     *
     * @draft ICU 3.2
     */
    static public long toLong(long universalTime, int timeScale)
    {
        TimeScaleData data = toRangeCheck(universalTime, timeScale);
        
        if (universalTime < 0) {
            if (universalTime < data.minRound) {
                return (universalTime + data.unitsRound) / data.units - data.epochOffsetP1;
            }
            
            return (universalTime - data.unitsRound) / data.units - data.epochOffset;
        }
        
        if (universalTime > data.maxRound) {
            return (universalTime - data.unitsRound) / data.units - data.epochOffsetM1;
        }
        
        return (universalTime + data.unitsRound) / data.units - data.epochOffset;
    }
    
    /**
     * Convert a datetime from the universal time scale to a <code>long</code> in the given time scale.
     *
     * @param universal The datetime in the universal time scale
     * @param timeScale The time scale to convert to
     * 
     * @return The datetime converted to the given time scale
     *
     * @draft ICU 3.2
     */
    static private long toLong(BigDecimal universalTime, int timeScale)
    {
        TimeScaleData data     = getTimeScaleData(timeScale);
        BigDecimal units       = new BigDecimal(data.units);
        BigDecimal epochOffset = new BigDecimal(data.epochOffset);
        
        return universalTime.divide(units, BigDecimal.ROUND_HALF_UP).subtract(epochOffset).longValue();
    }
    
    /**
     * Convert a datetime from the universal time scale to a <code>BigDecimal</code> in the given time scale.
     *
     * @param universal The datetime in the universal time scale
     * @param timeScale The time scale to convert to
     * 
     * @return The datetime converted to the given time scale
     *
     * @draft ICU 3.2
     */
    static public BigDecimal toBigDecimal(long universalTime, int timeScale)
    {
        TimeScaleData data     = getTimeScaleData(timeScale);
        BigDecimal universal   = new BigDecimal(universalTime);
        BigDecimal units       = new BigDecimal(data.units);
        BigDecimal epochOffset = new BigDecimal(data.epochOffset);
        
        return universal.divide(units, BigDecimal.ROUND_HALF_UP).subtract(epochOffset);
    }
    
    /**
     * Convert a datetime from the universal time scale to a <code>BigDecimal</code> in the given time scale.
     *
     * @param universal The datetime in the universal time scale
     * @param timeScale The time scale to convert to
     * 
     * @return The datetime converted to the given time scale
     *
     * @draft ICU 3.2
     */
    static public BigDecimal toBigDecimal(BigDecimal universalTime, int timeScale)
    {
        TimeScaleData data     = getTimeScaleData(timeScale);
        BigDecimal units       = new BigDecimal(data.units);
        BigDecimal epochOffset = new BigDecimal(data.epochOffset);
        
        return universalTime.divide(units, BigDecimal.ROUND_HALF_UP).subtract(epochOffset);
    }
    
    /**
     * Return the <code>TimeScaleData</code> object for the given time
     * scale.
     * 
     * @param scale - the time scale
     * 
     * @return the <code>TimeScaleData</code> object for the given time scale
     * 
     * @draft ICU 3.2
     */
    static public TimeScaleData getTimeScaleData(int scale)
    {
        if (scale < 0 || scale >= MAX_SCALE) {
            throw new IllegalArgumentException("scale out of range: " + scale);
        }
        
        return timeScaleTable[scale];
    }
    
    private static TimeScaleData toRangeCheck(long universalTime, int scale)
    {
        TimeScaleData data = getTimeScaleData(scale);
          
        if (universalTime >= data.toMin && universalTime <= data.toMax) {
            return data;
        }
        
        throw new IllegalArgumentException("universalTime out of range:" + universalTime);
    }
    
    private static TimeScaleData fromRangeCheck(long otherTime, int scale)
    {
        TimeScaleData data = getTimeScaleData(scale);
          
        if (otherTime >= data.fromMin && otherTime <= data.fromMax) {
            return data;
        }
        
        throw new IllegalArgumentException("otherTime out of range:" + otherTime);
    }
    
    private static TimeScaleData fromRangeCheck(double otherTime, int scale)
    {
        TimeScaleData data = getTimeScaleData(scale);
          
        if (otherTime >= data.fromMin && otherTime <= data.fromMax) {
            return data;
        }
        
        throw new IllegalArgumentException("otherTime out of range:" + otherTime);
    }
    
    private static BigDecimal toBigDecimalTrunc(BigDecimal universalTime, int timeScale)
    {
        TimeScaleData data = getTimeScaleData(timeScale);
        BigDecimal units = new BigDecimal(data.units);
        BigDecimal epochOffset = new BigDecimal(data.epochOffset);
        
        return universalTime.divide(units, BigDecimal.ROUND_DOWN).subtract(epochOffset);
    }
    
    private static int[][] epochDates = {
            {   1, Calendar.JANUARY,   1},
            {1970, Calendar.JANUARY,   1},
            {1601, Calendar.JANUARY,   1},
            {1904, Calendar.JANUARY,   1},
            {2001, Calendar.JANUARY,   1},
            {1899, Calendar.DECEMBER, 31},
            {1900, Calendar.MARCH,     1}
    };
    
    public static void main(String[] args)
    {
        TimeZone utc = new SimpleTimeZone(0, "UTC");
        Calendar cal = Calendar.getInstance(utc, Locale.ENGLISH);
        MessageFormat fmt = new MessageFormat("{0, date, full} {0, time, full} = {1}");
        Object arguments[] = {cal, null};
        
        System.out.println("Epoch offsets:");
        
        // January 1, 0001 00:00:00 is the universal epoch date...
        cal.set(1, Calendar.JANUARY, 1, 0, 0, 0);
        
        long universalEpoch = cal.getTimeInMillis();
        
        for (int i = 0; i < epochDates.length; i += 1) {
            int[] date = epochDates[i];
            
            cal.set(date[0], date[1], date[2]);
            
            long millis = cal.getTimeInMillis();
            
            arguments[1] = Long.toString((millis - universalEpoch) * milliseconds);
            
            System.out.println(fmt.format(arguments));
         }

        BigDecimal universalMin = new BigDecimal(Long.MIN_VALUE);
        BigDecimal universalMax = new BigDecimal(Long.MAX_VALUE);
        Object limitArgs[] = {null, null, null, null};
        
        fmt = new MessageFormat("{0}L, {1}L, {2}L, {3}L");
        
        System.out.println("\nTo, From limits:");
        
        // from limits
        for(int scale = 0; scale < MAX_SCALE; scale += 1) {
            BigDecimal min = toBigDecimalTrunc(universalMin, scale).max(universalMin);
            BigDecimal max = toBigDecimalTrunc(universalMax, scale).min(universalMax);
            long minLong   = min.longValue();
            long maxLong   = max.longValue();
            
            limitArgs[2] = min.toString();
            limitArgs[3] = max.toString();

            // to limits
            BigDecimal minTrunc   = bigDecimalFrom(min, scale);
            BigDecimal maxTrunc   = bigDecimalFrom(max, scale);
            BigDecimal minResidue = minTrunc.subtract(universalMin);
            BigDecimal maxResidue = universalMax.subtract(maxTrunc);
            TimeScaleData data    = getTimeScaleData(scale);
            long units            = data.units;
            BigDecimal half       = new BigDecimal(units == 1? 0: units / 2 - 1);
            
            min = minTrunc.subtract(minResidue.min(half));
            max = maxTrunc.add(maxResidue.min(half));
            limitArgs[0] = min.toString();
            limitArgs[1] = max.toString();
            
            System.out.println(fmt.format(limitArgs));
            
            // round-trip test the from limits
            if(toLong(from(minLong, scale), scale) != minLong) {
                System.out.println("OOPS: min didn't round trip!");
            }
            
            if(toLong(from(maxLong, scale), scale) != maxLong) {
                System.out.println("OOPS: max didn't round trip!");
            }
            
            // make sure that the to limits convert to the from limits
            if(toLong(min.longValue(), scale) != minLong) {
                System.out.println("OOPS: toLong(toMin) != fromMin");
            }
            
            if(toLong(max.longValue(), scale) != maxLong) {
                System.out.println("OOPS: toLong(toMax) != fromMax");
            }
        }
        
        arguments[0] = cal;
        fmt = new MessageFormat("{1} = {0, date, full} {0, time, full}");
        
        System.out.println("\nJava test:");
        cal.setTimeInMillis(toLong(from(0, JAVA_TIME), ICU4C_TIME));
        arguments[1] = " 000000000000000";
        System.out.println(fmt.format(arguments));
        
        cal.setTimeInMillis(toLong(from(-62164684800000L, JAVA_TIME), ICU4C_TIME));
        arguments[1] = "-62164684800000L";
        System.out.println(fmt.format(arguments));
        
        cal.setTimeInMillis(toLong(from(-62135769600000L, JAVA_TIME), ICU4C_TIME));
        arguments[1] = "-62135769600000L";
        System.out.println(fmt.format(arguments));
        
        System.out.println("\nUnix test:");
        
        cal.setTimeInMillis(toLong(from(0x80000000, UNIX_TIME), ICU4C_TIME));
        arguments[1] = "0x80000000";
        System.out.println(fmt.format(arguments));
        
        cal.setTimeInMillis(toLong(from(0, UNIX_TIME), ICU4C_TIME));
        arguments[1] = "0x00000000";
        System.out.println(fmt.format(arguments));
        
        cal.setTimeInMillis(toLong(from(0x7FFFFFFF, UNIX_TIME), ICU4C_TIME));
        arguments[1] = "0x7FFFFFFF";
        System.out.println(fmt.format(arguments));
    }
}
