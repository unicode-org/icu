/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/Attic/UnicodeFilterLogic.java,v $ 
 * $Date: 2000/03/10 04:07:25 $ 
 * $Revision: 1.3 $
 *
 *****************************************************************************************
 */
package com.ibm.text;

/**
 * <code>UnicodeFilterLogic</code> provides logical operators on
 * {@link UnicodeFilter} objects.  This class cannot be instantiated;
 * it consists only of static methods.  The static methods return
 * filter objects that perform logical inversion (<tt>not</tt>),
 * intersection (<tt>and</tt>), or union (<tt>or</tt>) of the given
 * filter objects.
 */
public final class UnicodeFilterLogic {

    /**
     * Returns a <tt>UnicodeFilter</tt> that implements the inverse of
     * the given filter.
     */
    public static UnicodeFilter not(final UnicodeFilter f) {
        return new UnicodeFilter() {
            public boolean contains(char c) {
                return !f.contains(c);
            }
        };
    }

    /**
     * Returns a <tt>UnicodeFilter</tt> that implements a short
     * circuit AND of the result of the two given filters.  That is,
     * if <tt>f.contains()</tt> is <tt>false</tt>, then <tt>g.contains()</tt>
     * is not called, and <tt>contains()</tt> returns <tt>false</tt>.
     *
     * <p>Either <tt>f</tt> or <tt>g</tt> must be non-null.
     */
    public static UnicodeFilter and(final UnicodeFilter f,
                                    final UnicodeFilter g) {
        if (f == null) {
            return g;
        }
        if (g == null) {
            return f;
        }
        return new UnicodeFilter() {
            public boolean contains(char c) {
                return f.contains(c) && g.contains(c);
            }
        };
    }

    /**
     * Returns a <tt>UnicodeFilter</tt> that implements a short
     * circuit AND of the result of the given filters.  That is, if
     * <tt>f[i].contains()</tt> is <tt>false</tt>, then
     * <tt>f[j].contains()</tt> is not called, where <tt>j > i</tt>, and
     * <tt>contains()</tt> returns <tt>false</tt>.
     */
    public static UnicodeFilter and(final UnicodeFilter[] f) {
        return new UnicodeFilter() {
            public boolean contains(char c) {
                for (int i=0; i<f.length; ++i) {
                    if (!f[i].contains(c)) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    /**
     * Returns a <tt>UnicodeFilter</tt> that implements a short
     * circuit OR of the result of the two given filters.  That is, if
     * <tt>f.contains()</tt> is <tt>true</tt>, then <tt>g.contains()</tt> is
     * not called, and <tt>contains()</tt> returns <tt>true</tt>.
     *
     * <p>Either <tt>f</tt> or <tt>g</tt> must be non-null.
     */
    public static UnicodeFilter or(final UnicodeFilter f,
                                   final UnicodeFilter g) {
        if (f == null) {
            return g;
        }
        if (g == null) {
            return f;
        }
        return new UnicodeFilter() {
            public boolean contains(char c) {
                return f.contains(c) || g.contains(c);
            }
        };
    }

    /**
     * Returns a <tt>UnicodeFilter</tt> that implements a short
     * circuit OR of the result of the given filters.  That is, if
     * <tt>f[i].contains()</tt> is <tt>false</tt>, then
     * <tt>f[j].contains()</tt> is not called, where <tt>j > i</tt>, and
     * <tt>contains()</tt> returns <tt>true</tt>.
     */
    public static UnicodeFilter or(final UnicodeFilter[] f) {
        return new UnicodeFilter() {
            public boolean contains(char c) {
                for (int i=0; i<f.length; ++i) {
                    if (f[i].contains(c)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    // TODO: Add nand() & nor() for convenience, if needed.
}
