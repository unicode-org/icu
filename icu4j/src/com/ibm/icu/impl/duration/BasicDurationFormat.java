/*
 *******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.duration;

import java.text.FieldPosition;
import java.util.Date;

import com.ibm.icu.text.DurationFormat;
import com.ibm.icu.util.ULocale;

/**
 * @author srl
 * @internal
 * @deprecated this API is for ICU internal use only
 */
public class BasicDurationFormat extends DurationFormat {
    
    /**
     * 
     */
    private static final long serialVersionUID = -3146984141909457700L;
    
    transient DurationFormatter formatter;
    transient PeriodFormatter pformatter;
    transient PeriodFormatterService pfs = null;

    public static DurationFormat getInstance(ULocale locale) {
        return new BasicDurationFormat(locale);
    }
    
    public StringBuffer format(Object object, StringBuffer toAppend, FieldPosition pos) {
        if(object instanceof Long) {
            String res = formatDurationFromNow(((Long)object).longValue());
            return toAppend.append(res);
        } else if(object instanceof Date) {
            String res = formatDurationFromNowTo(((Date)object));
            return toAppend.append(res);
         // BEGIN JDK>1.5
        } else if(object instanceof javax.xml.datatype.Duration) {
            String res = formatDuration(object);
            return toAppend.append(res);
         // END JDK>1.5
        } else {
            throw new IllegalArgumentException("Cannot format given Object as a Duration");
        }
    }
    
    public BasicDurationFormat() {
        pfs = BasicPeriodFormatterService.getInstance();
        formatter = pfs.newDurationFormatterFactory().getFormatter();
        pformatter = pfs.newPeriodFormatterFactory().setDisplayPastFuture(false).getFormatter();
    }
    /**
     * 
     */
    public BasicDurationFormat(ULocale locale) {
        super(locale);
        pfs = BasicPeriodFormatterService.getInstance();
        formatter = pfs.newDurationFormatterFactory().setLocale(locale.getName()).getFormatter();
        pformatter = pfs.newPeriodFormatterFactory().setDisplayPastFuture(false).setLocale(locale.getName()).getFormatter();
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

 // BEGIN JDK>1.5
    private static final javax.xml.datatype.DatatypeConstants.Field inFields[] = { 
        javax.xml.datatype.DatatypeConstants.YEARS,
        javax.xml.datatype.DatatypeConstants.MONTHS,
        javax.xml.datatype.DatatypeConstants.DAYS,
        javax.xml.datatype.DatatypeConstants.HOURS,
        javax.xml.datatype.DatatypeConstants.MINUTES,
        javax.xml.datatype.DatatypeConstants.SECONDS,
    };
    private static final TimeUnit outFields[] = { 
        TimeUnit.YEAR,
        TimeUnit.MONTH,
        TimeUnit.DAY,
        TimeUnit.HOUR,
        TimeUnit.MINUTE,
        TimeUnit.SECOND,
    };
    /** 
     *  JDK 1.5+ only
     * @param o
     * @return
     * @see http://java.sun.com/j2se/1.5.0/docs/api/javax/xml/datatype/Duration.html
     */
    public String formatDuration(Object obj) {
        javax.xml.datatype.Duration inDuration = (javax.xml.datatype.Duration)obj;
        Period p = null;
        javax.xml.datatype.Duration duration = inDuration;
        boolean inPast = false;
        if(inDuration.getSign()<0) {
            duration = inDuration.negate();
            inPast = true;
        }
        // convert a Duration to a Period
        for(int i=0;i<inFields.length;i++) {
            if(duration.isSet(inFields[i])) {
                Number n = duration.getField(inFields[i]);
                if(p == null) {
                    p = Period.at(n.floatValue(), outFields[i]);
                } else {
                    p = p.and(n.floatValue(), outFields[i]);
                }
            }
        }
        
        if(p == null) {
            // no fields set  = 0 seconds
            return formatDurationFromNow(0);
        } else {
            if(inPast) {// was negated, above.
                p = p.inPast();
            } else {
                p = p.inFuture();
            }
        }
        
        // now, format it.
        return pformatter.format(p);
    }
 // END JDK>1.5

}
