/*
**********************************************************************
*   Copyright (C) 2001-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   05/24/01    aliu        Creation.
**********************************************************************
*/

#include "unicode/utypes.h"

#if !UCONFIG_NO_TRANSLITERATION

#include "unicode/ustring.h"
#include "unicode/uchar.h"
#include "toupptrn.h"
#include "ustr_imp.h"
#include "cpputils.h"

U_NAMESPACE_BEGIN

UOBJECT_DEFINE_RTTI_IMPLEMENTATION(UppercaseTransliterator)

const char CURR_ID[] = "Any-Upper";

/**
 * Constructs a transliterator.
 */
UppercaseTransliterator::UppercaseTransliterator(const Locale& theLoc) :
    Transliterator(UnicodeString(CURR_ID, ""), 0),
    loc(theLoc), 
    buffer(0)
{
    buffer = (UChar *)uprv_malloc(u_getMaxCaseExpansion()*sizeof(buffer[0]));
}

/**
 * Destructor.
 */
UppercaseTransliterator::~UppercaseTransliterator() {
    uprv_free(buffer);
}

/**
 * Copy constructor.
 */
UppercaseTransliterator::UppercaseTransliterator(const UppercaseTransliterator& o) :
    Transliterator(o),
    loc(o.loc),
    buffer(0)
{
    buffer = (UChar *)uprv_malloc(u_getMaxCaseExpansion()*sizeof(buffer[0]));
    uprv_arrayCopy(o.buffer, 0, this->buffer, 0, u_getMaxCaseExpansion());
}

/**
 * Assignment operator.
 */
UppercaseTransliterator& UppercaseTransliterator::operator=(
                             const UppercaseTransliterator& o) {
    Transliterator::operator=(o);
    loc = o.loc;
    uprv_arrayCopy(o.buffer, 0, this->buffer, 0, u_getMaxCaseExpansion());
    return *this;
}

/**
 * Transliterator API.
 */
Transliterator* UppercaseTransliterator::clone(void) const {
    return new UppercaseTransliterator(*this);
}

/**
 * Implements {@link Transliterator#handleTransliterate}.
 */
void UppercaseTransliterator::handleTransliterate(Replaceable& text,
                                 UTransPosition& offsets, 
                                 UBool /*isIncremental*/) const
{
    /* TODO: Verify that isIncremental can be ignored */
    int32_t textPos = offsets.start;
    if (textPos >= offsets.limit)
        return;

    // get string for context
    
    UnicodeString original;
    text.extractBetween(offsets.contextStart, offsets.contextLimit, original);
    
    UCharIterator iter;
    uiter_setReplaceable(&iter, &text);
    iter.start = offsets.contextStart;
    iter.limit = offsets.contextLimit;
            
    // Walk through original string
    // If there is a case change, modify corresponding position in replaceable
    
    int32_t i = textPos - offsets.contextStart;
    int32_t limit = offsets.limit - offsets.contextStart;
    UChar32 cp;
    int32_t oldLen;
    
    for (; i < limit; ) {
        UTF_GET_CHAR(original.getBuffer(), 0, i, original.length(), cp);
        oldLen = UTF_CHAR_LENGTH(cp);
        i += oldLen;
        iter.index = i; // Point _past_ current char
        int32_t newLen = u_internalToUpper(cp, &iter, buffer, u_getMaxCaseExpansion(), loc.getName());
        if (newLen >= 0) {
            UnicodeString temp(buffer, newLen);
            text.handleReplaceBetween(textPos, textPos + oldLen, temp);
            if (newLen != oldLen) {
                textPos += newLen;
                offsets.limit += newLen - oldLen;
                offsets.contextLimit += newLen - oldLen;
                continue;
            }
        }
        textPos += oldLen;
    }
    offsets.start = offsets.limit;
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_TRANSLITERATION */
