/*
**********************************************************************
*   Copyright (C) 2001-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   07/03/01    aliu        Creation.
**********************************************************************
*/
#ifndef NORTRANS_H
#define NORTRANS_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_TRANSLITERATION

#include "unicode/translit.h"
#include "unicode/normlzr.h"

U_NAMESPACE_BEGIN

/**
 * A transliterator that performs normalization.
 * @author Alan Liu
 */
class U_I18N_API NormalizationTransliterator : public Transliterator {

    /**
     * The normalization mode of this transliterator.
     */
    UNormalizationMode fMode;

    /**
     * Normalization options for this transliterator.
     */
    int32_t options;

 public:

    /**
     * Destructor.
     */
    virtual ~NormalizationTransliterator();

    /**
     * Copy constructor.
     */
    NormalizationTransliterator(const NormalizationTransliterator&);

    /**
     * Assignment operator.
     */
    NormalizationTransliterator& operator=(const NormalizationTransliterator&);

    /**
     * Transliterator API.
     * @return    A copy of the object.
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
     * @param text          the buffer holding transliterated and
     *                      untransliterated text
     * @param offset        the start and limit of the text, the position
     *                      of the cursor, and the start and limit of transliteration.
     * @param incremental   if true, assume more text may be coming after
     *                      pos.contextLimit. Otherwise, assume the text is complete.
     */
    void handleTransliterate(Replaceable& text, UTransPosition& offset,
                             UBool isIncremental) const;
 public:

    /**
     * System registration hook.  Public to Transliterator only.
     */
    static void registerIDs();

 private:

    // Transliterator::Factory methods
    static Transliterator* _create(const UnicodeString& ID,
                                   Token context);

    /**
     * Constructs a transliterator.  This method is private.
     * Public users must use the factory method createInstance().
     */
    NormalizationTransliterator(const UnicodeString& id,
                                UNormalizationMode mode, int32_t opt);
};

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_TRANSLITERATION */

#endif
