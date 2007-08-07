/*
 *******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.duration;

import java.util.Date;

import com.ibm.icu.text.DurationFormat;
import com.ibm.icu.util.ULocale;

/**
 * @author srl
 *
 */
public class BasicDurationFormat extends DurationFormat {
    
    /**
     * 
     */
    private static final long serialVersionUID = -3146984141909457700L;
    
    transient DurationFormatter formatter;

    public static DurationFormat getInstance(ULocale locale) {
        return new BasicDurationFormat(locale);
    }
    
    
    
    public BasicDurationFormat() {
        formatter = BasicPeriodFormatterService.getInstance().newDurationFormatterFactory().getFormatter();
    }
    /**
     * 
     */
    public BasicDurationFormat(ULocale locale) {
        super(locale);
        formatter  = BasicPeriodFormatterService.getInstance().newDurationFormatterFactory().setLocale(locale.getName()).getFormatter();
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.text.DurationFormat#formatDurationFrom(long, long)
     */
    public String formatDurationFrom(long duration, long referenceDate) {
        return formatter.formatDurationFrom(duration, referenceDate);
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.text.DurationFormat#formatDurationFromNow(long)
     */
    public String formatDurationFromNow(long duration) {
        return formatter.formatDurationFromNow(duration);
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.text.DurationFormat#formatDurationFromNowTo(java.util.Date)
     */
    public String formatDurationFromNowTo(Date targetDate) {
        return formatter.formatDurationFromNowTo(targetDate);
    }

}
