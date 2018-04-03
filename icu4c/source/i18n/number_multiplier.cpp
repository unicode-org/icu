// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT

// Allow implicit conversion from char16_t* to UnicodeString for this file:
// Helpful in toString methods and elsewhere.
#define UNISTR_FROM_STRING_EXPLICIT

#include "number_types.h"
#include "number_multiplier.h"
#include "numparse_validators.h"

using namespace icu;
using namespace icu::number;
using namespace icu::number::impl;
using namespace icu::numparse::impl;


Multiplier::Multiplier(int32_t magnitude, double arbitrary)
        : fMagnitude(magnitude), fArbitrary(arbitrary) {}

Multiplier Multiplier::none() {
    return {0, 1};
}

Multiplier Multiplier::powerOfTen(int32_t power) {
    return {power, 1};
}

Multiplier Multiplier::arbitraryDecimal(StringPiece multiplicand) {
    // TODO: Fix this hack
    UErrorCode localError = U_ZERO_ERROR;
    DecimalQuantity dq;
    dq.setToDecNumber(multiplicand, localError);
    return {0, dq.toDouble()};
}

Multiplier Multiplier::arbitraryDouble(double multiplicand) {
    return {0, multiplicand};
}

void Multiplier::applyTo(impl::DecimalQuantity& quantity) const {
    quantity.adjustMagnitude(fMagnitude);
    quantity.multiplyBy(fArbitrary);
}

void Multiplier::applyReciprocalTo(impl::DecimalQuantity& quantity) const {
    quantity.adjustMagnitude(-fMagnitude);
    if (fArbitrary != 0) {
        quantity.multiplyBy(1 / fArbitrary);
    }
}


void
MultiplierFormatHandler::setAndChain(const Multiplier& multiplier, const MicroPropsGenerator* parent) {
    this->multiplier = multiplier;
    this->parent = parent;
}

void MultiplierFormatHandler::processQuantity(DecimalQuantity& quantity, MicroProps& micros,
                                              UErrorCode& status) const {
    parent->processQuantity(quantity, micros, status);
    multiplier.applyTo(quantity);
}


// NOTE: MultiplierParseHandler is declared in the header numparse_validators.h
MultiplierParseHandler::MultiplierParseHandler(::icu::number::Multiplier multiplier)
        : fMultiplier(multiplier) {}

void MultiplierParseHandler::postProcess(ParsedNumber& result) const {
    if (!result.quantity.bogus) {
        fMultiplier.applyReciprocalTo(result.quantity);
        // NOTE: It is okay if the multiplier was negative.
    }
}

UnicodeString MultiplierParseHandler::toString() const {
    return u"<Multiplier>";
}


#endif /* #if !UCONFIG_NO_FORMATTING */
