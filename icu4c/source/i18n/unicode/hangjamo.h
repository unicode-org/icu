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
 * @version $RCSfile: hangjamo.h,v $ $Revision: 1.1 $ $Date: 2000/01/18 01:55:52 $
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
     * Transliterates a segment of a string.  <code>Transliterator</code> API.
     * @param text the string to be transliterated
     * @param start the beginning index, inclusive; <code>0 <= start
     * <= limit</code>.
     * @param limit the ending index, exclusive; <code>start <= limit
     * <= text.length()</code>.
     * @return the new limit index
     */
    virtual int32_t transliterate(Replaceable &text,
                                  int32_t start, int32_t limit) const;

    /**
     * Implements {@link Transliterator#handleKeyboardTransliterate}.
     */
    virtual void handleKeyboardTransliterate(Replaceable& text,
                                             int32_t offsets[3]) const;

    /**
     * Return the length of the longest context required by this transliterator.
     * This is <em>preceding</em> context.
     * @param direction either <code>FORWARD</code> or <code>REVERSE</code>
     * @return maximum number of preceding context characters this
     * transliterator needs to examine
     */
    virtual int32_t getMaximumContextLength(void) const;

private:

    static bool_t decomposeHangul(UChar s, UnicodeString& result);

    UChar filteredCharAt(Replaceable& text, int32_t i) const;
};

inline HangulJamoTransliterator::~HangulJamoTransliterator() {}

#endif
