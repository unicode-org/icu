// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2003-2012, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.text.Format;

/**
 * An abstract class that extends {@link java.text.Format} to provide
 * additional ICU protocol, specifically, the <tt>getLocale()</tt>
 * API.  All ICU format classes are subclasses of this class.
 *
 * @see com.ibm.icu.util.ULocale
 * @author weiv
 * @author Alan Liu
 * @draft ICU 2.8 (retain)
 * @provisional This API might change or be removed in a future release.
 */
public abstract class UFormat extends Format {
    private static final long serialVersionUID = 1L;

    /**
     * @draft ICU 2.8 (retain)
     * @provisional This API might change or be removed in a future release.
     */
    public UFormat() {}

//    /**
//     * Return the locale that was used to create this object, or null.
//     * This may may differ from the locale requested at the time of
//     * this object's creation.  For example, if an object is created
//     * for locale <tt>en_US_CALIFORNIA</tt>, the actual data may be
//     * drawn from <tt>en</tt> (the <i>actual</i> locale), and
//     * <tt>en_US</tt> may be the most specific locale that exists (the
//     * <i>valid</i> locale).
//     *
//     * <p>Note: This method will be implemented in ICU 3.0; ICU 2.8
//     * contains a partial preview implementation.  The <i>actual</i>
//     * locale is returned correctly, but the <i>valid</i> locale is
//     * not, in most cases.
//     * @param type type of information requested, either {@link
//     * com.ibm.icu.util.ULocale#VALID_LOCALE} or {@link
//     * com.ibm.icu.util.ULocale#ACTUAL_LOCALE}.
//     * @return the information specified by <i>type</i>, or null if
//     * this object was not constructed from locale data.
//     * @see com.ibm.icu.util.ULocale
//     * @see com.ibm.icu.util.ULocale#VALID_LOCALE
//     * @see com.ibm.icu.util.ULocale#ACTUAL_LOCALE
//     * @draft ICU 2.8 (retain)
//     * @provisional This API might change or be removed in a future release.
//     */
//    public final ULocale getLocale(ULocale.Type type) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Set information about the locales that were used to create this
//     * object.  If the object was not constructed from locale data,
//     * both arguments should be set to null.  Otherwise, neither
//     * should be null.  The actual locale must be at the same level or
//     * less specific than the valid locale.  This method is intended
//     * for use by factories or other entities that create objects of
//     * this class.
//     * @param valid the most specific locale containing any resource
//     * data, or null
//     * @param actual the locale containing data used to construct this
//     * object, or null
//     * @see com.ibm.icu.util.ULocale
//     * @see com.ibm.icu.util.ULocale#VALID_LOCALE
//     * @see com.ibm.icu.util.ULocale#ACTUAL_LOCALE
//     */
//    final void setLocale(ULocale valid, ULocale actual) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }
}
