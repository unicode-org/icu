// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT

// Allow implicit conversion from char16_t* to UnicodeString for this file:
// Helpful in toString methods and elsewhere.
#define UNISTR_FROM_STRING_EXPLICIT

#include "number_types.h"
#include "number_multiplier.h"

using namespace icu;
using namespace icu::number;
using namespace icu::number::impl;


Multiplier::Multiplier(int32_t magnitudeMultiplier, int32_t multiplier)
        : magnitudeMultiplier(magnitudeMultiplier), multiplier(multiplier) {}

Multiplier Multiplier::magnitude(int32_t magnitudeMultiplier) {
    return {magnitudeMultiplier, 1};
}

Multiplier Multiplier::integer(int32_t multiplier) {
    return {0, multiplier};
}


void MultiplierChain::setAndChain(const Multiplier& multiplier, const MicroPropsGenerator* parent) {
    this->multiplier = multiplier;
    this->parent = parent;
}

void
MultiplierChain::processQuantity(DecimalQuantity& quantity, MicroProps& micros, UErrorCode& status) const {
    parent->processQuantity(quantity, micros, status);
    quantity.adjustMagnitude(multiplier.magnitudeMultiplier);
    if (multiplier.multiplier != 1) {
        quantity.multiplyBy(multiplier.multiplier);
    }
}


#endif /* #if !UCONFIG_NO_FORMATTING */
