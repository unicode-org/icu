/*
**********************************************************************
*   Copyright (c) 2000-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   01/11/2000  aliu        Creation.
**********************************************************************
*/
#ifndef NULTRANS_H
#define NULTRANS_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_TRANSLITERATION

#include "unicode/translit.h"

U_NAMESPACE_BEGIN

/**
 * A transliterator that leaves text unchanged.
 * @author Alan Liu
 * @internal Use transliterator factory methods instead since this class will be removed in that release.
 */
class U_I18N_API NullTransliterator : public Transliterator {

public:

    /**
     * ID for this transliterator.
     * @internal Use transliterator factory methods instead since this class will be removed in that release.
     */
    static const UChar ID[]; // public for Transliterator

    /**
     * ID for this transliterator.
     * @internal Use transliterator factory methods instead since this class will be removed in that release.
     */
    static const UChar SHORT_ID[]; // public for Transliterator

    /**
     * Constructs a transliterator.
     * @internal Use transliterator factory methods instead since this class will be removed in that release.
     */
    NullTransliterator();

    /**
     * Destructor.
     * @internal Use transliterator factory methods instead since this class will be removed in that release.
     */
    virtual ~NullTransliterator();

    /**
     * Transliterator API.
     * @internal Use transliterator factory methods instead since this class will be removed in that release.
     */
    Transliterator* clone(void) const;

    /**
     * Implements {@link Transliterator#handleTransliterate}.
     * @internal Use transliterator factory methods instead since this class will be removed in that release.
     */
    virtual void handleTransliterate(Replaceable& text, UTransPosition& offset,
                                     UBool isIncremental) const;

    /**
     * ICU "poor man's RTTI", returns a UClassID for the actual class.
     *
     * @draft ICU 2.2
     */
    virtual inline UClassID getDynamicClassID() const;

    /**
     * ICU "poor man's RTTI", returns a UClassID for this class.
     *
     * @draft ICU 2.2
     */
    static inline UClassID getStaticClassID();

private:

    /**
     * The address of this static class variable serves as this class's ID
     * for ICU "poor man's RTTI".
     */
    static const char fgClassID;
};

inline NullTransliterator::NullTransliterator() : Transliterator(ID, 0) {}

inline NullTransliterator::~NullTransliterator() {}

inline UClassID
NullTransliterator::getStaticClassID()
{ return (UClassID)&fgClassID; }

inline UClassID
NullTransliterator::getDynamicClassID() const
{ return NullTransliterator::getStaticClassID(); }

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_TRANSLITERATION */

#endif
