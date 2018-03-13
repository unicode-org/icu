// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT

// Allow implicit conversion from char16_t* to UnicodeString for this file:
// Helpful in toString methods and elsewhere.
#define UNISTR_FROM_STRING_EXPLICIT

#include "unicode/compactdecimalformat.h"

using namespace icu;


CompactDecimalFormat*
CompactDecimalFormat::createInstance(const Locale& inLocale, UNumberCompactStyle style,
                                     UErrorCode& status) {}

CompactDecimalFormat::CompactDecimalFormat(const CompactDecimalFormat& source) = default;

CompactDecimalFormat::~CompactDecimalFormat() = default;

CompactDecimalFormat& CompactDecimalFormat::operator=(const CompactDecimalFormat& rhs) {}

UClassID CompactDecimalFormat::getStaticClassID() {}

UClassID CompactDecimalFormat::getDynamicClassID() const {}


#endif /* #if !UCONFIG_NO_FORMATTING */
