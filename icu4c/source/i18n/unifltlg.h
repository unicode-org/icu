/*
**********************************************************************
* Copyright © {1999}, International Business Machines Corporation and others. All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/17/99    aliu        Creation.
**********************************************************************
*/
#ifndef UNIFLTLG_H
#define UNIFLTLG_H

#include "utypes.h"

class UnicodeFilter;

/**
 * <code>UnicodeFilterLogic</code> provides logical operators on
 * {@link UnicodeFilter} objects.  This class cannot be instantiated;
 * it consists only of static methods.  The static methods return
 * filter objects that perform logical inversion (<tt>not</tt>),
 * intersection (<tt>and</tt>), or union (<tt>or</tt>) of the given
 * filter objects.
 */
class U_I18N_API UnicodeFilterLogic {

public:

    /**
     * Returns a <tt>UnicodeFilter</tt> that implements the inverse of
     * the given filter.
     */
    static UnicodeFilter* createNot(const UnicodeFilter& f);

    /**
     * Returns a <tt>UnicodeFilter</tt> that implements a short
     * circuit AND of the result of the two given filters.  That is,
     * if <tt>f.isIn()</tt> is <tt>false</tt>, then <tt>g.isIn()</tt>
     * is not called, and <tt>isIn()</tt> returns <tt>false</tt>.
     *
     * <p>Either <tt>f</tt> or <tt>g</tt> must be non-null.
     */
    static UnicodeFilter* createAnd(const UnicodeFilter& f,
                                    const UnicodeFilter& g);

    /**
     * Returns a <tt>UnicodeFilter</tt> that implements a short
     * circuit AND of the result of the given filters.  That is, if
     * <tt>f[i].isIn()</tt> is <tt>false</tt>, then
     * <tt>f[j].isIn()</tt> is not called, where <tt>j > i</tt>, and
     * <tt>isIn()</tt> returns <tt>false</tt>.
     */
    // static UnicodeFilter* and(const UnicodeFilter** f);

    /**
     * Returns a <tt>UnicodeFilter</tt> that implements a short
     * circuit OR of the result of the two given filters.  That is, if
     * <tt>f.isIn()</tt> is <tt>true</tt>, then <tt>g.isIn()</tt> is
     * not called, and <tt>isIn()</tt> returns <tt>true</tt>.
     *
     * <p>Either <tt>f</tt> or <tt>g</tt> must be non-null.
     */
    static UnicodeFilter* createOr(const UnicodeFilter& f,
                                   const UnicodeFilter& g);

    /**
     * Returns a <tt>UnicodeFilter</tt> that implements a short
     * circuit OR of the result of the given filters.  That is, if
     * <tt>f[i].isIn()</tt> is <tt>false</tt>, then
     * <tt>f[j].isIn()</tt> is not called, where <tt>j > i</tt>, and
     * <tt>isIn()</tt> returns <tt>true</tt>.
     */
    // static UnicodeFilter* or(const UnicodeFilter** f);

    // TODO: Add nand() & nor() for convenience, if needed.

private:
    // Disallow instantiation
    UnicodeFilterLogic();
};

inline UnicodeFilterLogic::UnicodeFilterLogic() {}

#endif
