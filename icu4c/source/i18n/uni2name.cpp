/*
**********************************************************************
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   06/06/01    aliu        Creation.
**********************************************************************
*/

#include <stdio.h>

#include "cstring.h"
#include "uni2name.h"
#include "unicode/unifilt.h"
#include "unicode/uchar.h"

U_NAMESPACE_BEGIN

const char UnicodeNameTransliterator::fgClassID=0;

const char UnicodeNameTransliterator::_ID[] = "Any-Name";

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
 * Ignore isIncremental since we don't need the context, and
 * we work on codepoints.
 */
void UnicodeNameTransliterator::handleTransliterate(Replaceable& text, UTransPosition& offsets,
                                                    UBool /*isIncremental*/) const {
    // As of Unicode 3.0.0, the longest name is 83 characters long.
    // Adjust this buffer size as needed.

    char buf[128];
    
    int32_t cursor = offsets.start;
    int32_t limit = offsets.limit;

    UnicodeString str(openDelimiter);
    UErrorCode status;
    int32_t len;

    while (cursor < limit) {
        UChar32 c = text.char32At(cursor);
        int32_t clen = UTF_CHAR_LENGTH(c);
        status = U_ZERO_ERROR;
        if ((len = u_charName(c, U_EXTENDED_CHAR_NAME, buf, sizeof(buf), &status)) >0 && !U_FAILURE(status)) {
            str.truncate(1);
            str.append(UnicodeString(buf, len, "")).append(closeDelimiter);
            text.handleReplaceBetween(cursor, cursor+clen, str);
            len += 2; // adjust for delimiters
            cursor += len; // advance cursor and adjust for new text
            limit += len-clen; // change in length
        } else {
            cursor += clen;
        }
    }

    offsets.contextLimit += limit - offsets.limit;
    offsets.limit = limit;
    offsets.start = cursor;
}

U_NAMESPACE_END

