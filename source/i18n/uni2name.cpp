/*
**********************************************************************
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   06/06/01    aliu        Creation.
**********************************************************************
*/

#include "unicode/uni2name.h"
#include "unicode/unifilt.h"

U_NAMESPACE_BEGIN

const char* UnicodeNameTransliterator::_ID = "Any-Name";

/**
 * Constructs a transliterator.
 */
UnicodeNameTransliterator::UnicodeNameTransliterator(
                                 UChar32 openDelim, UChar32 closeDelim,
                                 UnicodeFilter* adoptedFilter) :
    Transliterator(_ID, adoptedFilter),
    openDelimiter(openDelim),
    closeDelimiter(closeDelim) {
}

/**
 * Constructs a transliterator with the default delimiters '{' and
 * '}'.
 */
UnicodeNameTransliterator::UnicodeNameTransliterator(UnicodeFilter* adoptedFilter) :
    Transliterator(_ID, adoptedFilter),
    openDelimiter((UChar) 0x007B /*{*/),
    closeDelimiter((UChar) 0x007D /*}*/) {
}

/**
 * Destructor.
 */
UnicodeNameTransliterator::~UnicodeNameTransliterator() {}

/**
 * Copy constructor.
 */
UnicodeNameTransliterator::UnicodeNameTransliterator(const UnicodeNameTransliterator& o) :
    Transliterator(o),
    openDelimiter(o.openDelimiter),
    closeDelimiter(o.closeDelimiter) {}

/**
 * Assignment operator.
 */
UnicodeNameTransliterator& UnicodeNameTransliterator::operator=(
                             const UnicodeNameTransliterator& o) {
    Transliterator::operator=(o);
    openDelimiter = o.openDelimiter;
    closeDelimiter = o.closeDelimiter;
    return *this;
}

/**
 * Transliterator API.
 */
Transliterator* UnicodeNameTransliterator::clone(void) const {
    return new UnicodeNameTransliterator(*this);
}

/**
 * Implements {@link Transliterator#handleTransliterate}.
 */
void UnicodeNameTransliterator::handleTransliterate(Replaceable& text, UTransPosition& offsets,
                                                    UBool isIncremental) const {
    // As of Unicode 3.0.0, the longest name is 83 characters long.
    // Adjust this buffer size as needed.
    char buf[128];
    
    int32_t cursor = offsets.start;
    int32_t limit = offsets.limit;

    UnicodeString str(openDelimiter);
    UErrorCode status;
    UTextOffset len;

    while (cursor < limit) {
        status = U_ZERO_ERROR;
        UChar c = text.charAt(cursor);
        if ((len=u_charName(c, U_UNICODE_CHAR_NAME, buf, sizeof(buf), &status)) > 0 &&
            !U_FAILURE(status)) {
            
            str.truncate(1);
            str.append(UnicodeString(buf, len, "")).append(closeDelimiter);
            
            text.handleReplaceBetween(cursor, cursor+1, str);
            len += 2; // adjust for delimiters
            cursor += len; // advance cursor by 1 and adjust for new text
            limit += len-1; // change in length is (len - 1)
        } else {
            ++cursor;
        }
    }

    offsets.contextLimit += limit - offsets.limit;
    offsets.limit = limit;
    offsets.start = cursor;
}

U_NAMESPACE_END

