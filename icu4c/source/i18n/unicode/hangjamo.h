/*
**********************************************************************
*   Copyright (c) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   01/17/2000  aliu        Ported from Java.
**********************************************************************
*/
#ifndef HANGJAMO_H
#define HANGJAMO_H

#include "unicode/translit.h"

/**
 * A transliterator that converts Hangul to Jamo.
 *
 * @author Mark Davis
 * @version $RCSfile: hangjamo.h,v $ $Revision: 1.3 $ $Date: 2000/01/19 19:02:10 $
 */
class U_I18N_API HangulJamoTransliterator : public Transliterator {

    /**
     * ID for this transliterator.
     */
    static const char* _ID;

public:

    /**
     * Constructs a transliterator.
     */
    HangulJamoTransliterator(UnicodeFilter* adoptedFilter = 0);

    /**
     * Destructor.
     */
    virtual ~HangulJamoTransliterator();

    /**
     * Copy constructor.
     */
    HangulJamoTransliterator(const HangulJamoTransliterator&);

    /**
     * Assignment operator.
     */
    HangulJamoTransliterator& operator=(const HangulJamoTransliterator&);

    /**
     * Transliterator API.
     */
    Transliterator* clone(void) const;

    /**
     * Implements {@link Transliterator#handleTransliterate}.
     */
    virtual void handleTransliterate(Replaceable& text, Position& offsets,
                                     bool_t isIncremental) const;

private:

    static bool_t decomposeHangul(UChar s, UnicodeString& result);
};

inline HangulJamoTransliterator::~HangulJamoTransliterator() {}

#endif
