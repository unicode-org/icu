/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/Attic/UnicodeFilterLogic.java,v $ 
 * $Date: 2002/12/03 21:07:57 $ 
 * $Revision: 1.8 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.text;

// Disable coverage analysis for this file
///CLOVER:OFF

/**
 * <code>UnicodeFilterLogic</code> provides logical operators on
 * {@link UnicodeFilter} objects.  This class cannot be instantiated;
 * it consists only of static methods.  The static methods return
 * filter objects that perform logical inversion (<tt>not</tt>),
 * intersection (<tt>and</tt>), or union (<tt>or</tt>) of the given
 * filter objects.
 *
 * @deprecated ICU 2.4 This will be removed after 2003-Aug-28. Use UnicodeSet methods instead.
 */
public final class UnicodeFilterLogic {

    /**
     * UnicodeFilter subclass that stubs out methods we don't implement.
     */
    private static abstract class _UF extends UnicodeFilter {
        public abstract boolean contains(int c); // redeclare
        public String toPattern(boolean escapeUnprintable) {
            return "";
        }
        public boolean matchesIndexValue(int v) {
            return false;
        }
        public void addMatchSetTo(UnicodeSet toUnionTo) {}
    }

    /**
     * Returns a <tt>UnicodeFilter</tt> that implements the inverse of
     * the given filter.
     * @deprecated ICU 2.4 This will be removed after 2003-Aug-28. Use UnicodeSet methods instead.
     */
    public static UnicodeFilter not(final UnicodeFilter f) {
        return new _UF() {
            public boolean contains(int c) {
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
     * @deprecated ICU 2.4 This will be removed after 2003-Aug-28. Use UnicodeSet methods instead.
     */
    public static UnicodeFilter and(final UnicodeFilter f,
                                    final UnicodeFilter g) {
        if (f == null) {
            return g;
        }
        if (g == null) {
            return f;
        }
        return new _UF() {
            public boolean contains(int c) {
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
     * @deprecated ICU 2.4 This will be removed after 2003-Aug-28. Use UnicodeSet methods instead.
     */
    public static UnicodeFilter and(final UnicodeFilter[] f) {
        return new _UF() {
            public boolean contains(int c) {
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
     * @deprecated ICU 2.4 This will be removed after 2003-Aug-28. Use UnicodeSet methods instead.
     */
    public static UnicodeFilter or(final UnicodeFilter f,
                                   final UnicodeFilter g) {
        if (f == null) {
            return g;
        }
        if (g == null) {
            return f;
        }
        return new _UF() {
            public boolean contains(int c) {
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
     * @deprecated ICU 2.4 This will be removed after 2003-Aug-28. Use UnicodeSet methods instead.
     */
    public static UnicodeFilter or(final UnicodeFilter[] f) {
        return new _UF() {
            public boolean contains(int c) {
                for (int i=0; i<f.length; ++i) {
                    if (f[i].contains(c)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    // Disable instantiation and make CheckTags happy
    private UnicodeFilterLogic() {}
}
