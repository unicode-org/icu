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

#include "translit.h"

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
 * @version $RCSfile: cpdtrans.h,v $ $Revision: 1.2 $ $Date: 1999/12/22 22:52:18 $
 */
class U_I18N_API CompoundTransliterator : public Transliterator {

    Transliterator** trans;

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
     * <tt>filter.isIn()</tt> returns <tt>false</tt> will not be
     * altered by this transliterator.  If <tt>filter</tt> is
     * <tt>null</tt> then no filtering is applied.
     */
    CompoundTransliterator(const UnicodeString& ID,
                           Transliterator* const transliterators[],
                           int32_t count,
                           UnicodeFilter* adoptedFilter = 0);

    CompoundTransliterator(const UnicodeString& ID,
                           UnicodeFilter* adoptedFilter = 0);

    /**
     * Destructor.
     */
    virtual ~CompoundTransliterator();

    /**
     * Copy constructor.
     */
    CompoundTransliterator(const CompoundTransliterator&);

    /**
     * Assignment operator.
     */
    CompoundTransliterator& operator=(const CompoundTransliterator&);

    /**
     * Transliterator API.
     */
    Transliterator* clone(void) const;

    /**
     * Returns the number of transliterators in this chain.
     * @return number of transliterators in this chain.
     */
    virtual int32_t getCount(void) const;

    /**
     * Returns the transliterator at the given index in this chain.
     * @param index index into chain, from 0 to <code>getCount() - 1</code>
     * @return transliterator at the given index
     */
    virtual const Transliterator& getTransliterator(int32_t index) const;

    void setTransliterators(Transliterator* const transliterators[],
                            int32_t count);

    void adoptTransliterators(Transliterator* adoptedTransliterators[],
                              int32_t count);

    /**
     * Transliterates a segment of a string.  <code>Transliterator</code> API.
     * @param text the string to be transliterated
     * @param start the beginning index, inclusive; <code>0 <= start
     * <= limit</code>.
     * @param limit the ending index, exclusive; <code>start <= limit
     * <= text.length()</code>.
     * @return the new limit index
     */
    virtual int32_t transliterate(Replaceable& text, int32_t start, int32_t limit) const;

    /**
     * Implements {@link Transliterator#handleKeyboardTransliterate}.
     */
    virtual void handleKeyboardTransliterate(Replaceable& text,
                                             int32_t index[3]) const;

    /**
     * Returns the length of the longest context required by this transliterator.
     * This is <em>preceding</em> context.
     * @return maximum number of preceding context characters this
     * transliterator needs to examine
     */
    virtual int32_t getMaximumContextLength(void) const;

private:

    void freeTransliterators(void);
};
#endif
