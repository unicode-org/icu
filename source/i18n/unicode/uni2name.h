/*
**********************************************************************
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   06/06/01    aliu        Creation.
**********************************************************************
*/
#ifndef UNI2NAME_H
#define UNI2NAME_H

#include "unicode/translit.h"

U_NAMESPACE_BEGIN

/**
 * A transliterator that performs character to name mapping.
 * @author Alan Liu
 */
class U_I18N_API UnicodeNameTransliterator : public Transliterator {

    UChar32 openDelimiter;
    UChar32 closeDelimiter;

 public:

    /**
     * Constructs a transliterator.
     */
    UnicodeNameTransliterator(UChar32 openDelimiter, UChar32 closeDelimiter,
                                UnicodeFilter* adoptedFilter = 0);

    /**
     * Constructs a transliterator with the default delimiters '{' and
     * '}'.
     */
    UnicodeNameTransliterator(UnicodeFilter* adoptedFilter = 0);

    /**
     * Destructor.
     */
    virtual ~UnicodeNameTransliterator();

    /**
     * Copy constructor.
     */
    UnicodeNameTransliterator(const UnicodeNameTransliterator&);

    /**
     * Assignment operator.
     */
    UnicodeNameTransliterator& operator=(const UnicodeNameTransliterator&);

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

    static const char* _ID;
};

U_NAMESPACE_END

#endif
