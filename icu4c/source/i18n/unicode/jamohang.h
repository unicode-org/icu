/*
**********************************************************************
*   Copyright (c) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   01/17/2000  aliu        Ported from Java.
**********************************************************************
*/
#ifndef JAMOHANG_H
#define JAMOHANG_H

#include "unicode/translit.h"

/**
 * A transliterator that converts Jamo to Hangul.
 *
 * @author Mark Davis
 * @version $RCSfile: jamohang.h,v $ $Revision: 1.5 $ $Date: 2000/05/18 22:08:27 $
 */
class U_I18N_API JamoHangulTransliterator : public Transliterator {

    /**
     * ID for this transliterator.
     */
    static const char* _ID;

public:

    /**
     * Constructs a transliterator.
     */
    JamoHangulTransliterator(UnicodeFilter* adoptedFilter = 0);

    /**
     * Destructor.
     */
    virtual ~JamoHangulTransliterator();

    /**
     * Copy constructor.
     */
    JamoHangulTransliterator(const JamoHangulTransliterator&);

    /**
     * Assignment operator.
     */
    JamoHangulTransliterator& operator=(const JamoHangulTransliterator&);

    /**
     * Transliterator API.
     */
    Transliterator* clone(void) const;

    /**
     * Implements {@link Transliterator#handleTransliterate}.
     */
    virtual void handleTransliterate(Replaceable& text, Position& offsets,
                                     UBool isIncremental) const;

private:

    static UChar composeHangul(UChar last, UChar ch, int32_t& count);
};

inline JamoHangulTransliterator::~JamoHangulTransliterator() {}

#endif
