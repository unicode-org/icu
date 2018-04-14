// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT

// Allow implicit conversion from char16_t* to UnicodeString for this file:
// Helpful in toString methods and elsewhere.
#define UNISTR_FROM_STRING_EXPLICIT

#include "numparse_types.h"
#include "number_decimalquantity.h"
#include <cmath>

using namespace icu;
using namespace icu::number;
using namespace icu::number::impl;
using namespace icu::numparse;
using namespace icu::numparse::impl;


ParsedNumber::ParsedNumber() {
    clear();
}

void ParsedNumber::clear() {
    quantity.bogus = true;
    charEnd = 0;
    flags = 0;
    prefix.setToBogus();
    suffix.setToBogus();
    currencyCode[0] = 0;
}

void ParsedNumber::setCharsConsumed(const StringSegment& segment) {
    charEnd = segment.getOffset();
}

void ParsedNumber::postProcess() {
    if (!quantity.bogus && 0 != (flags & FLAG_NEGATIVE)) {
        quantity.negate();
    }
}

bool ParsedNumber::success() const {
    return charEnd > 0 && 0 == (flags & FLAG_FAIL);
}

bool ParsedNumber::seenNumber() const {
    return !quantity.bogus || 0 != (flags & FLAG_NAN) || 0 != (flags & FLAG_INFINITY);
}

double ParsedNumber::getDouble() const {
    bool sawNaN = 0 != (flags & FLAG_NAN);
    bool sawInfinity = 0 != (flags & FLAG_INFINITY);

    // Check for NaN, infinity, and -0.0
    if (sawNaN) {
        return NAN;
    }
    if (sawInfinity) {
        if (0 != (flags & FLAG_NEGATIVE)) {
            return -INFINITY;
        } else {
            return INFINITY;
        }
    }
    U_ASSERT(!quantity.bogus);
    if (quantity.isZero() && quantity.isNegative()) {
        return -0.0;
    }

    if (quantity.fitsInLong()) {
        return quantity.toLong();
    } else {
        return quantity.toDouble();
    }
}

void ParsedNumber::populateFormattable(Formattable& output) const {
    bool sawNaN = 0 != (flags & FLAG_NAN);
    bool sawInfinity = 0 != (flags & FLAG_INFINITY);

    // Check for NaN, infinity, and -0.0
    if (sawNaN) {
        output.setDouble(NAN);
        return;
    }
    if (sawInfinity) {
        if (0 != (flags & FLAG_NEGATIVE)) {
            output.setDouble(-INFINITY);
            return;
        } else {
            output.setDouble(INFINITY);
            return;
        }
    }
    U_ASSERT(!quantity.bogus);
    if (quantity.isZero() && quantity.isNegative()) {
        output.setDouble(-0.0);
        return;
    }

    // All other numbers
    output.adoptDecimalQuantity(new DecimalQuantity(quantity));
}

bool ParsedNumber::isBetterThan(const ParsedNumber& other) {
    // Favor results with strictly more characters consumed.
    return charEnd > other.charEnd;
}



#endif /* #if !UCONFIG_NO_FORMATTING */
