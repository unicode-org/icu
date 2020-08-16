// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#if !UCONFIG_NO_FORMATTING

#include "number_usageprefs.h"
#include "cstring.h"
#include "number_decimalquantity.h"
#include "number_microprops.h"
#include "number_roundingutils.h"
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

// Move constructor
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
    const MaybeStackVector<Measure>& routedUnits = routed.measures;
    micros.outputUnit = routed.outputUnit.copy(status).build(status);
    if (U_FAILURE(status)) {
        return;
    }

    micros.mixedMeasuresCount = routedUnits.length() - 1;
    if (micros.mixedMeasuresCount > 0) {
#ifdef U_DEBUG
        U_ASSERT(micros.outputUnit.getComplexity(status) == UMEASURE_UNIT_MIXED);
        U_ASSERT(U_SUCCESS(status));
        // Check that we received measurements with the expected MeasureUnits:
        int32_t singleUnitsCount;
        LocalArray<MeasureUnit> singleUnits =
            micros.outputUnit.splitToSingleUnits(singleUnitsCount, status);
        U_ASSERT(U_SUCCESS(status));
        U_ASSERT(routedUnits.length() == singleUnitsCount);
        for (int32_t i = 0; i < routedUnits.length(); i++) {
            U_ASSERT(routedUnits[i]->getUnit() == singleUnits[i]);
        }
#endif
        // Mixed units: except for the last value, we pass all values to the
        // LongNameHandler via micros.mixedMeasures.
        if (micros.mixedMeasures.getCapacity() < micros.mixedMeasuresCount) {
            if (micros.mixedMeasures.resize(micros.mixedMeasuresCount) == NULL) {
                status = U_MEMORY_ALLOCATION_ERROR;
                return;
            }
        }
        for (int32_t i = 0; i < micros.mixedMeasuresCount; i++) {
            micros.mixedMeasures[i] = routedUnits[i]->getNumber().getInt64();
        }
    } else {
        micros.mixedMeasuresCount = 0;
    }
    // The last value (potentially the only value) gets passed on via quantity.
    quantity.setToDouble(routedUnits[routedUnits.length() - 1]->getNumber().getDouble());

    // TODO(units): here we are always overriding Precision. (1) get precision
    // from fUnitsRouter, (2) ensure we use the UnitPreference skeleton's
    // precision only when there isn't an explicit override we prefer to use.
    // This needs to be handled within
    // NumberFormatterImpl::macrosToMicroGenerator in number_formatimpl.cpp
    // TODO: Use precision from `routed` result.
    Precision precision = Precision::integer().withMinDigits(2);
    UNumberFormatRoundingMode roundingMode;
    // Temporary until ICU 64?
    roundingMode = precision.fRoundingMode;
    CurrencyUnit currency(u"", status);
    micros.rounder = {precision, roundingMode, currency, status};
    if (U_FAILURE(status)) {
        return;
    }
}

#endif /* #if !UCONFIG_NO_FORMATTING */
