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
using icu::StringSegment;

Precision parseSkeletonToPrecision(icu::UnicodeString precisionSkeleton, UErrorCode status) {
    if (U_FAILURE(status)) {
        return Precision::bogus();
    }
    constexpr int32_t kSkelPrefixLen = 20;
    if (!precisionSkeleton.startsWith(UNICODE_STRING_SIMPLE("precision-increment/"))) {
        status = U_INVALID_FORMAT_ERROR;
        return Precision::bogus();
    }
    U_ASSERT(precisionSkeleton[kSkelPrefixLen - 1] == u'/');
    StringSegment segment(precisionSkeleton, false);
    segment.adjustOffset(kSkelPrefixLen);
    MacroProps macros;
    blueprint_helpers::parseIncrementOption(segment, macros, status);
    return macros.precision;
}

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
    if (micros.rounder.fPrecision.isDefault()) {
        if (precisionSkeleton.length() > 0) {
            micros.rounder.fPrecision = parseSkeletonToPrecision(precisionSkeleton, status);
        } else {
            // TODO: some disgreement as to whether to do this override. The default
            // is maxFraction(6), which I find inappropriate for human-friendly
            // usage-based unit formatting? We should probably specify a "default
            // expectation when skeleton isn't given in unitPreferences", primarily
            // so we don't have to add that to every preference.
            micros.rounder.fPrecision = Precision::integer().withMinDigits(2);
        }
    }
}

#endif /* #if !UCONFIG_NO_FORMATTING */
