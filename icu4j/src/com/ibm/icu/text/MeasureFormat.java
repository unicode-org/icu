/*
**********************************************************************
* Copyright (c) 2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Alan Liu
* Created: April 20, 2004
* Since: ICU 3.0
**********************************************************************
*/
package com.ibm.icu.text;

import com.ibm.icu.util.ULocale;

/**
 * A formatter for Measure objects.  This is an abstract base class.
 *
 * <p>To format or parse a Measure object, first create a formatter
 * object using a MeasureFormat factory method.  Then use that
 * object's format and parse methods.
 *
 * @see com.ibm.icu.text.UFormat
 * @author Alan Liu
 * @draft ICU 3.0
 * @deprecated This is a draft API and might change in a future release of ICU.
 */
public abstract class MeasureFormat extends UFormat {

    /**
     * @internal
     */
    protected MeasureFormat() {};
    
    /**
     * Return a formatter for CurrencyAmount objects in the given
     * locale.
     * @param locale desired locale
     * @return a formatter object
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static MeasureFormat getCurrencyFormat(ULocale locale) {
        return new CurrencyFormat(locale);
    }

    /**
     * Return a formatter for CurrencyAmount objects in the default
     * locale.
     * @return a formatter object
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static MeasureFormat getCurrencyFormat() {
        return getCurrencyFormat(ULocale.getDefault());
    }
}
