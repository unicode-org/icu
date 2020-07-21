// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#if !UCONFIG_NO_FORMATTING

#include "number_usageprefs.h"
#include "cstring.h"
#include "number_decimalquantity.h"
#include "number_microprops.h"
#include "number_roundingutils.h"
#include "number_skeletons.h"
#include "unicode/char16ptr.h"
#include "unicode/currunit.h"
#include "unicode/fmtable.h"
#include "unicode/measure.h"
#include "unicode/numberformatter.h"
#include "unicode/platform.h"
#include "unicode/unum.h"
#include "unicode/urename.h"

using namespace icu::number;
using namespace icu::number::impl;
using icu::number::impl::skeleton::parseSkeleton;

// Copy constructor
Usage::Usage(const Usage &other) : fUsage(nullptr), fLength(other.fLength), fError(other.fError) {
    if (other.fUsage != nullptr) {
        fUsage = (char *)uprv_malloc(fLength + 1);
        uprv_strncpy(fUsage, other.fUsage, fLength + 1);
    }
}

// Copy assignment operator
Usage &Usage::operator=(const Usage &other) {
    fLength = other.fLength;
    if (other.fUsage != nullptr) {
        fUsage = (char *)uprv_malloc(fLength + 1);
        uprv_strncpy(fUsage, other.fUsage, fLength + 1);
    }
    fError = other.fError;
    return *this;
}

// Move constructor - can it be improved by taking over src's "this" instead of
// copying contents? Swapping pointers makes sense for heap objects but not for
// stack objects.
// *this = std::move(src);
Usage::Usage(Usage &&src) U_NOEXCEPT : fUsage(src.fUsage), fLength(src.fLength), fError(src.fError) {
    // Take ownership away from src if necessary
    src.fUsage = nullptr;
}

// Move assignment operator
Usage &Usage::operator=(Usage &&src) U_NOEXCEPT {
    if (this == &src) {
        return *this;
    }
    if (fUsage != nullptr) {
        uprv_free(fUsage);
    }
    fUsage = src.fUsage;
    fLength = src.fLength;
    fError = src.fError;
    // Take ownership away from src if necessary
    src.fUsage = nullptr;
    return *this;
}

Usage::~Usage() {
    if (fUsage != nullptr) {
        uprv_free(fUsage);
        fUsage = nullptr;
    }
}

void Usage::set(StringPiece value) {
    if (fUsage != nullptr) {
        uprv_free(fUsage);
        fUsage = nullptr;
    }
    fLength = value.length();
    fUsage = (char *)uprv_malloc(fLength + 1);
    uprv_strncpy(fUsage, value.data(), fLength);
    fUsage[fLength] = 0;
}

UsagePrefsHandler::UsagePrefsHandler(const Locale &locale,
                                     const MeasureUnit inputUnit,
                                     const StringPiece usage,
                                     const MicroPropsGenerator *parent,
                                     UErrorCode &status)
    : fUnitsRouter(inputUnit, StringPiece(locale.getCountry()), usage, status),
      fParent(parent) {
}

void UsagePrefsHandler::processQuantity(DecimalQuantity &quantity, MicroProps &micros,
                                        UErrorCode &status) const {
    fParent->processQuantity(quantity, micros, status);
    if (U_FAILURE(status)) {
        return;
    }

    quantity.roundToInfinity(); // Enables toDouble
    const auto routed = fUnitsRouter.route(quantity.toDouble(), status);
    if (U_FAILURE(status)) {
        return;
    }
    const auto& routedUnits = routed.measures;
    micros.outputUnit = routedUnits[0]->getUnit();
    quantity.setToDouble(routedUnits[0]->getNumber().getDouble());

    UnicodeString precisionSkeleton = routed.precision;
    // TODO(icu-units/icu#13): If the programmer specified a precision, use
    // that.
    if (precisionSkeleton.length() > 0) {
        CharString csPrecisionSkeleton;
        UErrorCode csErrCode = U_ZERO_ERROR;
        csPrecisionSkeleton.appendInvariantChars(precisionSkeleton, csErrCode);

        // Parse skeleton, collect results
        int32_t errOffset;
        // int32_t errOffset = 0;
        U_ASSERT(U_SUCCESS(status));
        MacroProps unitPrefMacros = parseSkeleton(precisionSkeleton, errOffset, status);
        Precision precision;
        UNumberFormatRoundingMode roundingMode;
        if (U_FAILURE(status)) {
            return;
        }
        precision = unitPrefMacros.precision;
        if (unitPrefMacros.roundingMode != kDefaultMode) {
            roundingMode = unitPrefMacros.roundingMode;
        } else {
            // TODO: "Temporary until ICU 64" in number_formatimpl.cpp? Clarify!
            roundingMode = precision.fRoundingMode;
        }
        CurrencyUnit currency(u"", status);
        micros.rounder = {precision, roundingMode, currency, status};
    } else {
        Precision precision = Precision::integer().withMinDigits(2);
        UNumberFormatRoundingMode roundingMode;
        // Temporary until ICU 64?
        roundingMode = precision.fRoundingMode;
        CurrencyUnit currency(u"", status);
        micros.rounder = {precision, roundingMode, currency, status};
    }
}

#endif /* #if !UCONFIG_NO_FORMATTING */
