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
 * @version $RCSfile: jamohang.h,v $ $Revision: 1.2 $ $Date: 2000/01/18 18:27:27 $
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
    virtual void handleTransliterate(Replaceable& text,
                                     int32_t offsets[3]) const;

private:

    /**
     * Return composed character (if it composes)
     * 0 otherwise
     */
    static UChar composeHangul(UChar last, UChar ch);
};

inline JamoHangulTransliterator::~JamoHangulTransliterator() {}

#endif
