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

class U_I18N_API UVector;

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
 * @version $RCSfile: cpdtrans.h,v $ $Revision: 1.18 $ $Date: 2001/09/22 03:36:34 $
 * @draft
 */
class U_I18N_API CompoundTransliterator : public Transliterator {

    Transliterator** trans;

    int32_t count;

    /**
     * For compound RBTs (those with an ::id block before and/or after
     * the main rule block) we record the index of the RBT here.
     * Otherwise, this should have a value of -1.  We need this
     * information to implement toRules().
     */
    int32_t compoundRBTIndex;

public:

    /**
     * Constructs a new compound transliterator given an array of
     * transliterators.  The array of transliterators may be of any
     * length, including zero or one, however, useful compound
     * transliterators have at least two components.
     * @param transliterators array of <code>Transliterator</code>
     * objects
     * @param transliteratorCount The number of
     * <code>Transliterator</code> objects in transliterators.
     * @param filter the filter.  Any character for which
     * <tt>filter.contains()</tt> returns <tt>false</tt> will not be
     * altered by this transliterator.  If <tt>filter</tt> is
     * <tt>null</tt> then no filtering is applied.
     * @draft
     */
    CompoundTransliterator(Transliterator* const transliterators[],
                           int32_t transliteratorCount,
                           UnicodeFilter* adoptedFilter = 0);

    /**
     * Constructs a new compound transliterator.
     * @param filter the filter.  Any character for which
     * <tt>filter.isIn()</tt> returns <tt>false</tt> will not be
     * altered by this transliterator.  If <tt>filter</tt> is
     * <tt>null</tt> then no filtering is applied.
     * @draft
     */
    CompoundTransliterator(const UnicodeString& id,
                           UTransDirection dir,
                           UnicodeFilter* adoptedFilter,
                           UParseError& parseError,
                           UErrorCode& status);

    /**
     * Constructs a new compound transliterator in the FORWARD
     * direction with a NULL filter.
     * @draft
     */
    CompoundTransliterator(const UnicodeString& id,
                           UParseError& parseError,
                           UErrorCode& status);

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
     * Override Transliterator:
     * Create a rule string that can be passed to createFromRules()
     * to recreate this transliterator.
     * @param result the string to receive the rules.  Previous
     * contents will be deleted.
     * @param escapeUnprintable if TRUE then convert unprintable
     * character to their hex escape representations, \uxxxx or
     * \Uxxxxxxxx.  Unprintable characters are those other than
     * U+000A, U+0020..U+007E.
     */
    virtual UnicodeString& toRules(UnicodeString& result,
                                   UBool escapeUnprintable) const;

    /**
     * Implements {@link Transliterator#handleTransliterate}.
     * @draft
     */
    virtual void handleTransliterate(Replaceable& text, UTransPosition& index,
                                     UBool incremental) const;

private:

    friend class Transliterator;
    friend class TransliteratorRegistry; // to access private ct

    /**
     * Private constructor for compound RBTs.  Construct a compound
     * transliterator using the given idBlock, with the adoptedTrans
     * inserted at the idSplitPoint.
     */
    CompoundTransliterator(const UnicodeString& ID,
                           const UnicodeString& idBlock,
                           int32_t idSplitPoint,
                           Transliterator *adoptedTrans,
                           UParseError& parseError,
                           UErrorCode& status);
                           
    /**
     * Private constructor for Transliterator.
     */
    CompoundTransliterator(UTransDirection dir,
                           UVector& list,
                           UErrorCode& status);

    void init(const UnicodeString& id,
              UTransDirection direction,
              int32_t idSplitPoint,
              Transliterator *adoptedRbt,
              UBool fixReverseID,
              UParseError& parseError,
              UErrorCode& status);

    void init(UVector& list,
              UTransDirection direction,
              UBool fixReverseID,
              UErrorCode& status);

    /**
     * Return the IDs of the given list of transliterators, concatenated
     * with ';' delimiting them.  Equivalent to the perlish expression
     * join(';', map($_.getID(), transliterators).
     */
    UnicodeString joinIDs(Transliterator* const transliterators[],
                          int32_t transCount);

    void freeTransliterators(void);

    void computeMaximumContextLength(void);

    
#ifdef U_USE_DEPRECATED_TRANSLITERATOR_API

public:

    /**
     * Constructs a new compound transliterator.
     * @param filter the filter.  Any character for which
     * <tt>filter.isIn()</tt> returns <tt>false</tt> will not be
     * altered by this transliterator.  If <tt>filter</tt> is
     * <tt>null</tt> then no filtering is applied.
     * @deprecated Remove after Aug 2002. Use the constructor that takes
     * UParseError as one of the paramerters.
     */
    CompoundTransliterator(const UnicodeString& id,
                           UTransDirection dir,
                           UnicodeFilter* adoptedFilter,
                           UErrorCode& status);

    /**
     * Constructs a new compound transliterator in the FORWARD
     * direction with a NULL filter.
     * @deprecated Remove after Aug 2002. Use the constructor that takes
     * UParseError as one of the parmeters.
     */
    CompoundTransliterator(const UnicodeString& id,
                           UErrorCode& status);

#endif
};

/**
 * Definitions for deprecated API
 * @deprecated Remove after Aug 2002
 */

#ifdef U_USE_DEPRECATED_TRANSLITERATOR_API

inline CompoundTransliterator::CompoundTransliterator( const UnicodeString& id,
                                                       UTransDirection dir,
                                                       UnicodeFilter* adoptedFilter,
                                                       UErrorCode& status):
                                            Transliterator(id, adoptedFilter),
                                            trans(0), compoundRBTIndex(-1) {
    UParseError parseError;
    init(id, dir, -1, 0, TRUE,parseError,status);
}

inline CompoundTransliterator::CompoundTransliterator(const UnicodeString& id,
                                                      UErrorCode& status) :
                                            Transliterator(id, 0), // set filter to 0 here!
                                            trans(0), compoundRBTIndex(-1) {
    UParseError parseError;
    init(id, UTRANS_FORWARD, -1, 0, TRUE,parseError,status);       
}

#endif

#endif
