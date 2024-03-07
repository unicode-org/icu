// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2011, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl;

import com.ibm.icu.text.TimeZoneNames;
import com.ibm.icu.text.TimeZoneNames.Factory;
import com.ibm.icu.util.ULocale;

/**
 * The implementation class of <code>TimeZoneNames.Factory</code>
 */
public class TimeZoneNamesFactoryImpl extends Factory {

    /* (non-Javadoc)
     * @see com.ibm.icu.text.TimeZoneNames.Factory#getTimeZoneNames(com.ibm.icu.util.ULocale)
     */
    @Override
    public TimeZoneNames getTimeZoneNames(ULocale locale) {
        return new TimeZoneNamesImpl(locale);
    }

}
