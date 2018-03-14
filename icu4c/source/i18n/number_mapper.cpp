// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT

// Allow implicit conversion from char16_t* to UnicodeString for this file:
// Helpful in toString methods and elsewhere.
#define UNISTR_FROM_STRING_EXPLICIT

#include "number_mapper.h"

using namespace icu;
using namespace icu::number;
using namespace icu::number::impl;


UnlocalizedNumberFormatter NumberPropertyMapper::create(const DecimalFormatProperties& properties,
                                                        const DecimalFormatSymbols& symbols,
                                                        DecimalFormatProperties& exportedProperties,
                                                        UErrorCode& status) {
    // TODO
    status = U_UNSUPPORTED_ERROR;
    return NumberFormatter::with();
}



#endif /* #if !UCONFIG_NO_FORMATTING */
