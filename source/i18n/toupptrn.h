/*
**********************************************************************
*   Copyright (C) 2001-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   05/24/01    aliu        Creation.
**********************************************************************
*/
#ifndef TOUPPTRN_H
#define TOUPPTRN_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_TRANSLITERATION

#include "unicode/translit.h"
#include "unicode/locid.h"

U_NAMESPACE_BEGIN

/**
 * A transliterator that performs locale-sensitive toUpper()
 * case mapping.
 * @author Alan Liu
 */
class U_I18N_API UppercaseTransliterator : public Transliterator {

 public:

    /**
     * Constructs a transliterator.
     * @param loc the given locale.
     */
    UppercaseTransliterator(const Locale& loc = Locale::getDefault());

    /**
     * Destructor.
     */
    virtual ~UppercaseTransliterator();

    /**
     * Copy constructor.
     */
    UppercaseTransliterator(const UppercaseTransliterator&);

    /**
     * Assignment operator.
     */
    UppercaseTransliterator& operator=(const UppercaseTransliterator&);

    /**
     * Transliterator API.
     * @return a copy of the object.
     */
    Transliterator* clone(void) const;

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
    static UClassID getStaticClassID();

 protected:


    /**
     * Implements {@link Transliterator#handleTransliterate}.
     * @param text        the buffer holding transliterated and
     *                    untransliterated text
     * @param offset      the start and limit of the text, the position
     *                    of the cursor, and the start and limit of transliteration.
     * @param incremental if true, assume more text may be coming after
     *                    pos.contextLimit.  Otherwise, assume the text is complete.
     */
    virtual void handleTransliterate(Replaceable& text,
                                     UTransPosition& offsets, 
                                     UBool isIncremental) const;

private:

    Locale loc;
    UChar* buffer;

};

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_TRANSLITERATION */

#endif
