/*
* Copyright (C) 1999, International Business Machines Corporation and others. All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/17/99    aliu        Creation.
**********************************************************************
*/
#ifndef UNIFILT_H
#define UNIFILT_H

#include "unicode/unimatch.h"

U_NAMESPACE_BEGIN

/**
 * <code>UnicodeFilter</code> defines a protocol for selecting a
 * subset of the full range (U+0000 to U+FFFF) of Unicode characters.
 * Currently, filters are used in conjunction with classes like {@link
 * Transliterator} to only process selected characters through a
 * transformation.
 *
 * <p>Note: UnicodeFilter currently stubs out two pure virtual methods
 * of its base class, UnicodeMatcher.  These methods are toPattern()
 * and matchesIndexValue().  This is done so that filter classes that
 * are not actually used as matchers -- specifically, those in the
 * UnicodeFilterLogic component, and those in tests -- can continue to
 * work without defining these methods.  As long as a filter is not
 * used in an RBT during real transliteration, these methods will not
 * be called.  However, this breaks the UnicodeMatcher base class
 * protocol, and it is not a correct solution.
 *
 * <p>In the future we may revisit the UnicodeMatcher / UnicodeFilter
 * hierarchy and either redesign it, or simply remove the stubs in
 * UnicodeFilter and force subclasses to implement the full
 * UnicodeMatcher protocol.
 *
 * @see UnicodeFilterLogic
 * @draft
 */
class U_I18N_API UnicodeFilter : public UnicodeMatcher {

public:
    /**
     * Destructor
     * @draft */
    virtual ~UnicodeFilter();

    /**
     * Returns <tt>true</tt> for characters that are in the selected
     * subset.  In other words, if a character is <b>to be
     * filtered</b>, then <tt>contains()</tt> returns
     * <b><tt>false</tt></b>.
     * @draft
     */
    virtual UBool contains(UChar32 c) const = 0;

    /**
     * UnicodeMatcher API.  This class stubs this out.
     */
    UnicodeString& toPattern(UnicodeString& result,
                             UBool escapeUnprintable) const;

    /**
     * UnicodeMatcher API.  This class stubs this out.
     */
    UBool matchesIndexValue(uint8_t v) const;

    /**
     * Implement UnicodeMatcher API.
     */
    virtual UMatchDegree matches(const Replaceable& text,
                                 int32_t& offset,
                                 int32_t limit,
                                 UBool incremental) const;

protected:

    UnicodeFilter();
};

inline UnicodeFilter::UnicodeFilter() {}
inline UnicodeFilter::~UnicodeFilter() {}

U_NAMESPACE_END

#endif
