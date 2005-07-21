/*
**********************************************************************
*   Copyright (C) 2001-2004, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   05/24/01    aliu        Creation.
**********************************************************************
*/
#ifndef TOLOWTRN_H
#define TOLOWTRN_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_TRANSLITERATION

#include "unicode/translit.h"
#include "unicode/locid.h"
#include "casetrn.h"

U_NAMESPACE_BEGIN

/**
 * A transliterator that performs locale-sensitive toLower()
 * case mapping.
 * @author Alan Liu
 */
class U_I18N_API LowercaseTransliterator : public CaseMapTransliterator {

 public:

    /**
     * Constructs a transliterator.
     * @param loc the given locale.
     */
    LowercaseTransliterator(const Locale& loc = Locale::getDefault());

    /**
     * Destructor.
     */
    virtual ~LowercaseTransliterator();

    /**
     * Copy constructor.
     */
    LowercaseTransliterator(const LowercaseTransliterator&);

    /**
     * Assignment operator.
     */
    LowercaseTransliterator& operator=(const LowercaseTransliterator&);

    /**
     * Transliterator API.
     * @return a copy of the object.
     */
    virtual Transliterator* clone(void) const;

    /**
     * ICU "poor man's RTTI", returns a UClassID for the actual class.
     *
     * @draft ICU 2.2
     */
    virtual UClassID getDynamicClassID() const;

    /**
     * ICU "poor man's RTTI", returns a UClassID for this class.
     *
     * @draft ICU 2.2
     */
    static UClassID U_EXPORT2 getStaticClassID();
};

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_TRANSLITERATION */

#endif
