/*
**********************************************************************
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   05/24/01    aliu        Creation.
**********************************************************************
*/
#ifndef TITLETRN_H
#define TITLETRN_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_TRANSLITERATION

#include "unicode/translit.h"
#include "unicode/locid.h"

U_NAMESPACE_BEGIN

/**
 * A transliterator that converts all letters (as defined by
 * <code>UCharacter.isLetter()</code>) to lower case, except for those
 * letters preceded by non-letters.  The latter are converted to title
 * case using <code>u_totitle()</code>.
 * @author Alan Liu
 */
class U_I18N_API TitlecaseTransliterator : public Transliterator {

    /**
     * ID for this transliterator.
     */
    static const char _ID[];

 public:

    /**
     * Constructs a transliterator.
     * @param loc the given locale.
     */
    TitlecaseTransliterator(const Locale& loc = Locale::getDefault());

    /**
     * Destructor.
     */
    virtual ~TitlecaseTransliterator();

    /**
     * Copy constructor.
     */
    TitlecaseTransliterator(const TitlecaseTransliterator&);

    /**
     * Assignment operator.
     */
    TitlecaseTransliterator& operator=(const TitlecaseTransliterator&);

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
    virtual inline UClassID getDynamicClassID() const { return getStaticClassID(); }

    /**
     * ICU "poor man's RTTI", returns a UClassID for this class.
     *
     * @draft ICU 2.2
     */
    static inline UClassID getStaticClassID() { return (UClassID)&fgClassID; }

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
    void handleTransliterate(Replaceable& text, UTransPosition& offset,
                             UBool isIncremental) const;

 public:

    /**
     * Static memory cleanup function.  FOR INTERNAL USE ONLY; DO NOT
     * CALL.
     */
    static void cleanup();
 private:
    Locale loc;
    UChar* buffer;

    /**
     * The address of this static class variable serves as this class's ID
     * for ICU "poor man's RTTI".
     */
    static const char fgClassID;
};

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_TRANSLITERATION */

#endif
