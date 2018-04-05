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
#include "number_utils.h"
#include "decNumber.h"

using namespace icu;
using namespace icu::number;
using namespace icu::number::impl;
using namespace icu::numparse::impl;


Multiplier::Multiplier(int32_t magnitude, DecNum* arbitraryToAdopt)
        : fMagnitude(magnitude), fArbitrary(arbitraryToAdopt), fError(U_ZERO_ERROR) {
    if (fArbitrary != nullptr) {
        // Attempt to convert the DecNum to a magnitude multiplier.
        fArbitrary->normalize();
        if (fArbitrary->getRawDecNumber()->digits == 1 && fArbitrary->getRawDecNumber()->lsu[0] == 1 &&
            !fArbitrary->isNegative()) {
            // Success!
            fMagnitude = fArbitrary->getRawDecNumber()->exponent;
            delete fArbitrary;
            fArbitrary = nullptr;
        }
    }
}

Multiplier::Multiplier(const Multiplier& other)
        : fMagnitude(other.fMagnitude), fArbitrary(nullptr), fError(other.fError) {
    if (other.fArbitrary != nullptr) {
        UErrorCode localStatus = U_ZERO_ERROR;
        fArbitrary = new DecNum(*other.fArbitrary, localStatus);
    }
}

Multiplier& Multiplier::operator=(const Multiplier& other) {
    fMagnitude = other.fMagnitude;
    if (other.fArbitrary != nullptr) {
        UErrorCode localStatus = U_ZERO_ERROR;
        fArbitrary = new DecNum(*other.fArbitrary, localStatus);
    } else {
        fArbitrary = nullptr;
    }
    fError = other.fError;
    return *this;
}

Multiplier::Multiplier(Multiplier&& src) U_NOEXCEPT
        : fMagnitude(src.fMagnitude), fArbitrary(src.fArbitrary), fError(src.fError) {
    // Take ownership away from src if necessary
    src.fArbitrary = nullptr;
}

Multiplier& Multiplier::operator=(Multiplier&& src) U_NOEXCEPT {
    fMagnitude = src.fMagnitude;
    fArbitrary = src.fArbitrary;
    fError = src.fError;
    // Take ownership away from src if necessary
    src.fArbitrary = nullptr;
    return *this;
}

Multiplier::~Multiplier() {
    delete fArbitrary;
}


Multiplier Multiplier::none() {
    return {0, nullptr};
}

Multiplier Multiplier::powerOfTen(int32_t power) {
    return {power, nullptr};
}

Multiplier Multiplier::arbitraryDecimal(StringPiece multiplicand) {
    UErrorCode localError = U_ZERO_ERROR;
    LocalPointer<DecNum> decnum(new DecNum(), localError);
    if (U_FAILURE(localError)) {
        return {localError};
    }
    decnum->setTo(multiplicand, localError);
    if (U_FAILURE(localError)) {
        return {localError};
    }
    return {0, decnum.orphan()};
}

Multiplier Multiplier::arbitraryDouble(double multiplicand) {
    UErrorCode localError = U_ZERO_ERROR;
    LocalPointer<DecNum> decnum(new DecNum(), localError);
    if (U_FAILURE(localError)) {
        return {localError};
    }
    decnum->setTo(multiplicand, localError);
    if (U_FAILURE(localError)) {
        return {localError};
    }
    return {0, decnum.orphan()};
}

void Multiplier::applyTo(impl::DecimalQuantity& quantity) const {
    quantity.adjustMagnitude(fMagnitude);
    if (fArbitrary != nullptr) {
        UErrorCode localStatus = U_ZERO_ERROR;
        quantity.multiplyBy(*fArbitrary, localStatus);
    }
}

void Multiplier::applyReciprocalTo(impl::DecimalQuantity& quantity) const {
    quantity.adjustMagnitude(-fMagnitude);
    if (fArbitrary != nullptr) {
        UErrorCode localStatus = U_ZERO_ERROR;
        quantity.divideBy(*fArbitrary, localStatus);
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
        : fMultiplier(std::move(multiplier)) {}

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
