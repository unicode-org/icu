/*
**********************************************************************
*   Copyright (C) 1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/17/99    aliu        Creation.
**********************************************************************
*/
#ifndef CPDTRANS_H
#define CPDTRANS_H

#include "unicode/translit.h"

/**
 * A transliterator that is composed of two or more other
 * transliterator objects linked together.  For example, if one
 * transliterator transliterates from script A to script B, and
 * another transliterates from script B to script C, the two may be
 * combined to form a new transliterator from A to C.
 *
 * <p>Composed transliterators may not behave as expected.  For
 * example, inverses may not combine to form the identity
 * transliterator.  See the class documentation for {@link
 * Transliterator} for details.
 *
 * <p>If a non-<tt>null</tt> <tt>UnicodeFilter</tt> is applied to a
 * <tt>CompoundTransliterator</tt>, it has the effect of being
 * logically <b>and</b>ed with the filter of each transliterator in
 * the chain.
 *
 * <p>Copyright &copy; IBM Corporation 1999.  All rights reserved.
 *
 * @author Alan Liu
 * @version $RCSfile: cpdtrans.h,v $ $Revision: 1.7 $ $Date: 2000/03/22 19:19:33 $
 * @draft
 */
class U_I18N_API CompoundTransliterator : public Transliterator {

    Transliterator** trans;

    /**
     * Array of original filters associated with transliterators.
     */
    UnicodeFilter** filters;

    int32_t count;

public:

    /**
     * Constructs a new compound transliterator given an array of
     * transliterators.  The array of transliterators may be of any
     * length, including zero or one, however, useful compound
     * transliterators have at least two components.
     * @param transliterators array of <code>Transliterator</code>
     * objects
     * @param filter the filter.  Any character for which
     * <tt>filter.contains()</tt> returns <tt>false</tt> will not be
     * altered by this transliterator.  If <tt>filter</tt> is
     * <tt>null</tt> then no filtering is applied.
     * @draft
     */
    CompoundTransliterator(Transliterator* const transliterators[],
                           int32_t count,
                           UnicodeFilter* adoptedFilter = 0);

    /**
     * Constructs a new compound transliterator.
     * @param filter the filter.  Any character for which
     * <tt>filter.isIn()</tt> returns <tt>false</tt> will not be
     * altered by this transliterator.  If <tt>filter</tt> is
     * <tt>null</tt> then no filtering is applied.
     * @draft
     */
    CompoundTransliterator(const UnicodeString& ID,
                           Direction dir = FORWARD,
                           UnicodeFilter* adoptedFilter = 0);

    /**
     * Destructor.
     * @draft
     */
    virtual ~CompoundTransliterator();

    /**
     * Copy constructor.
     * @draft
     */
    CompoundTransliterator(const CompoundTransliterator&);

    /**
     * Assignment operator.
     * @draft
     */
    CompoundTransliterator& operator=(const CompoundTransliterator&);

    /**
     * Transliterator API.
     * @draft
     */
    Transliterator* clone(void) const;

    /**
     * Returns the number of transliterators in this chain.
     * @return number of transliterators in this chain.
     * @draft
     */
    virtual int32_t getCount(void) const;

    /**
     * Returns the transliterator at the given index in this chain.
     * @param index index into chain, from 0 to <code>getCount() - 1</code>
     * @return transliterator at the given index
     * @draft
     */
    virtual const Transliterator& getTransliterator(int32_t index) const;

    /**
     * @draft
     */
    void setTransliterators(Transliterator* const transliterators[],
                            int32_t count);

    /**
     * @draft
     */
    void adoptTransliterators(Transliterator* adoptedTransliterators[],
                              int32_t count);

    /**
     * Override Transliterator.  Modify the transliterators that make up
     * this compound transliterator so their filters are the logical AND
     * of this transliterator's filter and their own.  Original filters
     * are kept in the filters array.
     */
    virtual void adoptFilter(UnicodeFilter* f);

    /**
     * Implements {@link Transliterator#handleTransliterate}.
     * @draft
     */
    virtual void handleTransliterate(Replaceable& text, Position& index,
                                     bool_t incremental) const;

private:

    /**
     * Return the IDs of the given list of transliterators, concatenated
     * with ';' delimiting them.  Equivalent to the perlish expression
     * join(';', map($_.getID(), transliterators).
     */
    UnicodeString joinIDs(Transliterator* const transliterators[],
                          int32_t count);

    /**
     * Splits a string, as in JavaScript
     */
    UnicodeString* split(const UnicodeString& s,
                         UChar divider,
                         int32_t& count);

    void freeTransliterators(void);

    void computeMaximumContextLength(void);
};
#endif
