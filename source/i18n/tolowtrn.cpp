/*
**********************************************************************
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   05/24/01    aliu        Creation.
**********************************************************************
*/

#include "tolowtrn.h"
#include "unicode/ustring.h"
#include "ustr_imp.h"
#include "cpputils.h"
#include "unicode/uchar.h"

U_NAMESPACE_BEGIN

const char LowercaseTransliterator::_ID[] = "Any-Lower";

/**
 * Constructs a transliterator.
 */
LowercaseTransliterator::LowercaseTransliterator(const Locale& theLoc) : Transliterator(_ID, 0),
    loc(theLoc) , buffer(0) {
    buffer = new UChar[u_getMaxCaseExpansion()];
}

/**
 * Destructor.
 */
LowercaseTransliterator::~LowercaseTransliterator() {
    delete [] buffer;
}

/**
 * Copy constructor.
 */
LowercaseTransliterator::LowercaseTransliterator(const LowercaseTransliterator& o) :
    Transliterator(o),
    loc(o.loc), buffer(0) {
    buffer = new UChar[u_getMaxCaseExpansion()];
}

/**
 * Assignment operator.
 */
LowercaseTransliterator& LowercaseTransliterator::operator=(
                             const LowercaseTransliterator& o) {
    Transliterator::operator=(o);
    loc = o.loc;
    uprv_arrayCopy((const UChar*)o.buffer, 0, this->buffer, 0, u_getMaxCaseExpansion());
    return *this;
}

/**
 * Transliterator API.
 */
Transliterator* LowercaseTransliterator::clone(void) const {
    return new LowercaseTransliterator(*this);
}

/**
 * Implements {@link Transliterator#handleTransliterate}.
 */
void LowercaseTransliterator::handleTransliterate(Replaceable& text,
                                 UTransPosition& offsets, 
                                 UBool isIncremental) const
{
    int32_t textPos = offsets.start;
    int32_t loop;
    if (textPos >= offsets.limit) return;

    // get string for context
    // TODO: add convenience method to do this, since we do it all over
    
    UnicodeString original;
    /*UChar *original = new UChar[offsets.contextLimit - offsets.contextStart+1];*/ // get whole context
    /* Extract the characters from Replaceable */
    for (loop = offsets.contextStart; loop < offsets.contextLimit; loop++) {
        original.append(text.charAt(loop));
    }
    
    // Walk through original string
    // If there is a case change, modify corresponding position in replaceable
    
    int32_t i = textPos - offsets.contextStart;
    int32_t limit = offsets.limit - offsets.contextStart;
    UChar32 cp, bufferCH;
    int32_t oldLen;
    
    for (; i < limit; ) { 
        UErrorCode status = U_ZERO_ERROR;
        int32_t s = i;
        bufferCH = original.char32At(s);

        UTF_GET_CHAR(original.getBuffer(), 0, i, original.length(), cp);
        oldLen = UTF_CHAR_LENGTH(cp);
        i += oldLen;
        int32_t len = u_strToLower(buffer, u_getMaxCaseExpansion(), original.getBuffer()+s, i-s, loc.getName(), &status);
        /* Skip checking of status code here because the buffer should not have overflowed. */
        UTF_GET_CHAR(buffer, 0, 0, len, cp);
        if ( bufferCH != cp ) {
            UnicodeString temp(buffer);
            text.handleReplaceBetween(textPos, textPos + oldLen, temp);
            if (len != oldLen) {
                textPos += len;
                offsets.limit += len - oldLen;
                offsets.contextLimit += len - oldLen;
                continue;
            }
        }
        textPos += oldLen;
    }
    offsets.start = offsets.limit;
}
U_NAMESPACE_END

