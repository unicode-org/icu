/*
 *******************************************************************************
 * Copyright (C) 2007-2009, International Business Machines Corporation and    *
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

    private static boolean checkXMLDuration = true; 

    public StringBuffer format(Object object, StringBuffer toAppend, FieldPosition pos) {
        if(object instanceof Long) {
            String res = formatDurationFromNow(((Long)object).longValue());
            return toAppend.append(res);
        } else if(object instanceof Date) {
            String res = formatDurationFromNowTo(((Date)object));
            return toAppend.append(res);
        }
        if(checkXMLDuration) try {
            if(object instanceof javax.xml.datatype.Duration) {
                String res = formatDuration(object);
                return toAppend.append(res);
            }
        } catch ( NoClassDefFoundError ncdfe ) {
            System.err.println("Skipping XML capability");
            checkXMLDuration = false; // don't try again
        }
        throw new IllegalArgumentException("Cannot format given Object as a Duration");

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

    /** 
     *  JDK 1.5+ only
     * @param o
     * @return
     * @see http://java.sun.com/j2se/1.5.0/docs/api/javax/xml/datatype/Duration.html
     */
    public String formatDuration(Object obj) {
        javax.xml.datatype.DatatypeConstants.Field inFields[] = { 
                javax.xml.datatype.DatatypeConstants.YEARS,
                javax.xml.datatype.DatatypeConstants.MONTHS,
                javax.xml.datatype.DatatypeConstants.DAYS,
                javax.xml.datatype.DatatypeConstants.HOURS,
                javax.xml.datatype.DatatypeConstants.MINUTES,
                javax.xml.datatype.DatatypeConstants.SECONDS,
            };
             TimeUnit outFields[] = { 
                TimeUnit.YEAR,
                TimeUnit.MONTH,
                TimeUnit.DAY,
                TimeUnit.HOUR,
                TimeUnit.MINUTE,
                TimeUnit.SECOND,
            };

        javax.xml.datatype.Duration inDuration = (javax.xml.datatype.Duration)obj;
        Period p = null;
        javax.xml.datatype.Duration duration = inDuration;
        boolean inPast = false;
        if(inDuration.getSign()<0) {
            duration = inDuration.negate();
            inPast = true;
        }
        // convert a Duration to a Period
        boolean sawNonZero = false; // did we have a set, non-zero field?
        for(int i=0;i<inFields.length;i++) {
            if(duration.isSet(inFields[i])) {
                Number n = duration.getField(inFields[i]);
                if(n.intValue() == 0 && !sawNonZero) {
                    continue; // ignore zero fields larger than the largest nonzero field
                } else {
                    sawNonZero = true;
                }
                float floatVal = n.floatValue();
                // is there a 'secondary' unit to set?
                TimeUnit alternateUnit = null;
                float alternateVal = 0;
                
                // see if there is a fractional part
                if(outFields[i]==TimeUnit.SECOND) {
                    double fullSeconds = floatVal;
                    double intSeconds = Math.floor(floatVal);
                    double millis = (fullSeconds-intSeconds)*1000.0;
                    if(millis > 0.0) {
                        alternateUnit = TimeUnit.MILLISECOND;
                        alternateVal=(float)millis;
                        floatVal=(float)intSeconds;
                    }
                }
                
                if(p == null) {
                    p = Period.at(floatVal, outFields[i]);
                } else {
                    p = p.and(floatVal, outFields[i]);
                }
                
                if(alternateUnit != null) {
                    p = p.and(alternateVal, alternateUnit); // add in MILLISECONDs
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
}
