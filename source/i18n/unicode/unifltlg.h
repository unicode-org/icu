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

#include "unicode/utypes.h"

class UnicodeFilter;

/**
 * <code>UnicodeFilterLogic</code> provides logical operators on
 * {@link UnicodeFilter} objects.  This class cannot be instantiated;
 * it consists only of static methods.  The static methods return
 * filter objects that perform logical inversion (<tt>not</tt>),
 * intersection (<tt>and</tt>), or union (<tt>or</tt>) of the given
 * filter objects.
 *
 * If a UnicodeFilter* f is passed in, where f == NULL, then that
 * is treated as a filter that contains all Unicode characters.
 * Therefore, createNot(NULL) returns a filter that contains no
 * Unicode characters.  Likewise, createAnd(g, NULL) returns g->clone(),
 * and createAnd(NULL, NULL) returns NULL.
 */
class U_I18N_API UnicodeFilterLogic {

public:

    /**
     * Returns a <tt>UnicodeFilter</tt> that implements the inverse of
     * the given filter.
     * @param f may be NULL
     * @result always non-NULL
     */
    static UnicodeFilter* createNot(const UnicodeFilter* f);

    /**
     * Returns a <tt>UnicodeFilter</tt> that implements a short
     * circuit AND of the result of the two given filters.  That is,
     * if <tt>f.contains()</tt> is <tt>false</tt>, then <tt>g.contains()</tt>
     * is not called, and <tt>contains()</tt> returns <tt>false</tt>.
     * @param f may be NULL
     * @param g may be NULL
     * @result will be NULL if and only if f == g == NULL
     */
    static UnicodeFilter* createAnd(const UnicodeFilter* f,
                                    const UnicodeFilter* g);

    /**
     * Returns a <tt>UnicodeFilter</tt> that implements a short
     * circuit OR of the result of the two given filters.  That is, if
     * <tt>f.contains()</tt> is <tt>true</tt>, then <tt>g.contains()</tt> is
     * not called, and <tt>contains()</tt> returns <tt>true</tt>.
     * @param f may be NULL
     * @param g may be NULL
     * @result will be NULL if and only if f == g == NULL
     */
    static UnicodeFilter* createOr(const UnicodeFilter* f,
                                   const UnicodeFilter* g);

private:
    // Disallow instantiation
    UnicodeFilterLogic();
};

inline UnicodeFilterLogic::UnicodeFilterLogic() {}

#endif
