/*
**********************************************************************
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   06/07/01    aliu        Creation.
**********************************************************************
*/
#ifndef NAME2UNI_H
#define NAME2UNI_H

#include "unicode/translit.h"

/**
 * A transliterator that performs name to character mapping.
 * @author Alan Liu
 */
class U_I18N_API NameUnicodeTransliterator : public Transliterator {

    UChar32 openDelimiter;
    UChar32 closeDelimiter;

 public:

    /**
     * Constructs a transliterator.
     */
    NameUnicodeTransliterator(UChar32 openDelimiter, UChar32 closeDelimiter,
                              UnicodeFilter* adoptedFilter = 0);

    /**
     * Constructs a transliterator with the default delimiters '{' and
     * '}'.
     */
    NameUnicodeTransliterator(UnicodeFilter* adoptedFilter = 0);

    /**
     * Destructor.
     */
    virtual ~NameUnicodeTransliterator();

    /**
     * Copy constructor.
     */
    NameUnicodeTransliterator(const NameUnicodeTransliterator&);

    /**
     * Assignment operator.
     */
    NameUnicodeTransliterator& operator=(const NameUnicodeTransliterator&);

    /**
     * Transliterator API.
     */
    Transliterator* clone(void) const;

 protected:

    /**
     * Implements {@link Transliterator#handleTransliterate}.
     * @draft
     */
    virtual void handleTransliterate(Replaceable& text, UTransPosition& offset,
                                     UBool isIncremental) const;

 private:

    static const char _ID[];
};
#endif
