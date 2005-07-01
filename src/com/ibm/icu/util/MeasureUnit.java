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
package com.ibm.icu.util;

/**
 * A unit such as length, mass, volume, currency, etc.  A unit is
 * coupled with a numeric amount to produce a Measure.
 *
 * @see com.ibm.icu.util.Measure
 * @author Alan Liu
 * @draft ICU 3.0
 * @deprecated This is a draft API and might change in a future release of ICU.
 */
public abstract class MeasureUnit {
    /**
     * @internal
     */
    protected MeasureUnit() {};
}
