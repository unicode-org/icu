/*
******************************************************************************
* Copyright (C) 2003, International Business Machines Corporation and        *
* others. All Rights Reserved.                                               *
******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/util/ULocale.java,v $
* $Date: 2004/01/08 22:27:14 $
* $Revision: 1.8 $
*
******************************************************************************
*/

package com.ibm.icu.util;

import java.util.Locale;
import java.io.Serializable;
import java.io.IOException;

/**
 * A class that extends {@link java.util.Locale} to provide additional
 * ICU protocol.  In ICU 3.0 this class will be enhanced to support
 * RFC 3066 language identifiers.
 *
 * <p>Many classes and services in ICU follow a factory idiom, in which a
 * factory method or object responds to a client request with an
 * object.  The request includes a locale (the <i>requested</i>
 * locale), and the returned object is constructed using data for that
 * locale.  The system may lack data for the requested locale, in
 * which case the locale fallback mechanism will be invoked until a
 * populated locale is found (the <i>valid</i> locale).  Furthermore,
 * even when a valid locale is found, further fallback may be required
 * to reach a locale containing the specific data required by the
 * service (the <i>actual</i> locale).
 *
 * <p>This class provides selectors {@link #VALID_LOCALE} and {@link
 * #ACTUAL_LOCALE} intended for use in methods named
 * <tt>getLocale()</tt>.  These methods exist in several ICU classes,
 * including {@link com.ibm.icu.util.Calendar}, {@link
 * com.ibm.icu.text.UFormat}, {@link com.ibm.icu.text.BreakIterator},
 * {@link com.ibm.icu.text.Collator}, {@link
 * com.ibm.icu.text.DateFormatSymbols}, and {@link
 * com.ibm.icu.text.DecimalFormatSymbols} and their subclasses, if
 * any.  Once an object of one of these classes has been created,
 * <tt>getLocale()</tt> may be called on it to determine the valid and
 * actual locale arrived at during the object's construction.
 *
 * @see java.util.Locale
 * @author weiv
 * @author Alan Liu
 * @draft ICU 2.8
 */
public final class ULocale implements Serializable {

    private transient Locale locale;

    private String locName;

    /**
     * The root ULocale.
     * @draft ICU 2.8
     */ 
    public static final ULocale ROOT = new ULocale("");

    /**
     * Construct a ULocale object from a {@link java.util.Locale}.
     * @param loc a JDK locale
     * @draft ICU 2.8
     */
    public ULocale(Locale loc) {
        this.locName = loc.toString();
        this.locale = loc;
    }
    
    /**
     * Construct a ULocale from a string of the form "no_NO_NY".
     * @param locName string representation of the locale, e.g:
     * "en_US", "sy-Cyrl-YU"
     * @draft ICU 2.8
     */ 
    public ULocale(String locName) {
        this.locName  = locName;
        this.locale = new Locale(locName, "");
    }

    /**
     * Convert this ULocale object to a {@link java.util.Locale}.
     * @return a JDK locale that either exactly represents this object
     * or is the closest approximation.
     * @draft ICU 2.8
     */
    public Locale toLocale() {
        return locale;
    }
    
    /**
     * Return the current default ULocale.
     * @draft ICU 2.8
     */ 
    public static ULocale getDefault() {
        return new ULocale(Locale.getDefault());
    }

    /** 
     * Selector for <tt>getLocale()</tt> indicating the locale of the
     * resource containing the data.  This is always at or above the
     * valid locale.  If the valid locale does not contain the
     * specific data being requested, then the actual locale will be
     * above the valid locale.  If the object was not constructed from
     * locale data, then the valid locale is <i>null</i>.
     *
     * <p>Note: The actual locale will be returned correctly in ICU
     * 3.0 or later.
     * @draft ICU 2.8
     */
    public static final Type ACTUAL_LOCALE = new Type(0);
 
    /** 
     * Selector for <tt>getLocale()</tt> indicating the most specific
     * locale for which any data exists.  This is always at or above
     * the requested locale, and at or below the actual locale.  If
     * the requested locale does not correspond to any resource data,
     * then the valid locale will be above the requested locale.  If
     * the object was not constructed from locale data, then the
     * actual locale is <i>null</i>.
     * @draft ICU 2.8
     */ 
    public static final Type VALID_LOCALE = new Type(1);
    
    /**
     * Opaque selector enum for <tt>getLocale()</tt>.
     * @see com.ibm.icu.util.ULocale
     * @see com.ibm.icu.util.ULocale#ACTUAL_LOCALE
     * @see com.ibm.icu.util.ULocale#VALID_LOCALE
     * @draft ICU 2.8
     */
    public static final class Type {
        private int localeType;
        private Type(int type) { localeType = type; }
    }
        
    private void writeObject(java.io.ObjectOutputStream out)
        throws IOException {
        out.writeObject(locName);
    }

    private void readObject(java.io.ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        locName = (String)in.readObject();
        locale = new Locale(locName, "");
    }
}
