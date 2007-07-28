/*
 *******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Date;

import com.ibm.icu.util.ULocale;

/**
 * This class implements a formatter over a duration in time
 * such as "2 days from now" or "3 hours ago".
 * @draft ICU 3.8
 */
public abstract class DurationFormat extends UFormat {
    
    /**
     * Construct a duration format for the specified locale
     * @draft ICU 3.8
     */
    public static DurationFormat getInstance(ULocale locale) {
        throw new UnsupportedOperationException();
    }
    

    /**
     * Subclass interface
     * @internal
     */
    protected DurationFormat() {
    }

    /**
     * Format an arbitrary object.
     * Defaults to a call to formatDurationFromNow() for either Long or Date objects.
     * @param object the object to format. Should be either a Long or Date object.
     * @param toAppend the buffer to append to
     * @param pos the field position, may contain additional error messages.
     * @return the toAppend buffer
     * @draft ICU 3.8
     */
    public StringBuffer format(Object object, StringBuffer toAppend,
            FieldPosition pos) {
        if(object instanceof Long) {
            String res = formatDurationFromNow(((Long)object).longValue());
            return toAppend.append(res);
        } else if(object instanceof Date) {
            String res = formatDurationFromNowTo(((Date)object));
            return toAppend.append(res);
        } else {
            throw new IllegalArgumentException("Cannot format given Object as a Duration");
        }
    }

    /**
     * DurationFormat cannot parse, by default. This method will throw an UnsupportedOperationException.
     * @draft ICU 3.8
     */
    public Object parseObject(String source, ParsePosition pos) {
       throw new UnsupportedOperationException();
    }

    /**
     * Formats the duration between now and a target date.
     * <p>
     * This is a convenience method that calls
     * formatDurationFrom(long, long) using now
     * as the reference date, and the difference between now and
     * <code>targetDate.getTime()</code> as the duration.
     * 
     * @param targetDate the ending date
     * @return the formatted time
     * @draft ICU 3.8
     */
    public abstract String formatDurationFromNowTo(Date targetDate);

    /**
     * Formats a duration expressed in milliseconds.
     * <p>
     * This is a convenience method that calls formatDurationFrom
     * using the current system time as the reference date.
     * 
     * @param duration the duration in milliseconds
     * @return the formatted time
     * @draft ICU 3.8
     */
    public abstract String formatDurationFromNow(long duration);

    /**
     * Formats a duration expressed in milliseconds from a reference date.
     * <p>
     * The reference date allows formatters to use actual durations of
     * variable-length periods (like months) if they wish.
     * <p>
     * The duration is expressed as the number of milliseconds in the
     * past (negative values) or future (positive values) with respect
     * to a reference date (expressed as milliseconds in epoch).
     * 
     * @param duration the duration in milliseconds
     * @param referenceDate the date from which to compute the duration
     * @return the formatted time
     * @draft ICU 3.8
     */
    public abstract String formatDurationFrom(long duration, long referenceDate);
    
    
}
