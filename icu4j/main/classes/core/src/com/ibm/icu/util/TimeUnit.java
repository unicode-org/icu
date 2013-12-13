/*
 **************************************************************************
 * Copyright (C) 2008-2013, Google, International Business Machines
 * Corporation and others. All Rights Reserved.
 **************************************************************************
 */
package com.ibm.icu.util;

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;


/**
 * Measurement unit for time units.
 * @see TimeUnitAmount
 * @see TimeUnit
 * @author markdavis
 * @stable ICU 4.0
 */
public class TimeUnit extends MeasureUnit {
    private static final long serialVersionUID = -2839973855554750484L;
    
    /**
     * Here for serialization backward compatibility only.
     */
    private final int index;

    TimeUnit(String type, String code) {
        super(type, code);
        index = 0;
    }

    /**
     * @return the available values
     * @stable ICU 4.0
     */
    public static TimeUnit[] values() {
        return new TimeUnit[] {
                (TimeUnit) SECOND,
                (TimeUnit) MINUTE,
                (TimeUnit) HOUR,
                (TimeUnit) DAY,
                (TimeUnit) WEEK,
                (TimeUnit) MONTH,
                (TimeUnit) YEAR};
    }
    
    private Object writeReplace() throws ObjectStreamException {
        return new MeasureUnitProxy(type, code);
    }
    
    // For backward compatibility only
    private Object readResolve() throws ObjectStreamException {
        // The old index field used to uniquely identify the time unit.
        switch (index) {
        case 6:
            return SECOND;
        case 5:
            return MINUTE;
        case 4:
            return HOUR;
        case 3:
            return DAY;
        case 2:
            return WEEK;
        case 1:
            return MONTH;
        case 0:
            return YEAR;
        default:
            throw new InvalidObjectException("Bad index: " + index);
        }
    }
}
