/*
**********************************************************************
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   05/24/01    aliu        Creation.
**********************************************************************
*/
#ifndef TOUPPTRN_H
#define TOUPPTRN_H

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
     */
    virtual void handleTransliterate(Replaceable& text,
                                     UTransPosition& offsets, 
                                     UBool isIncremental) const;

 private:

    Locale loc;
    UChar* buffer;
    static const char _ID[];

    /**
     * The address of this static class variable serves as this class's ID
     * for ICU "poor man's RTTI".
     */
    static const char fgClassID;
};

U_NAMESPACE_END

#endif
