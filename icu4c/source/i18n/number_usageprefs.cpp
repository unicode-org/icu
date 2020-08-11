// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

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
#include "units_data.h"

using namespace icu;
using namespace icu::number;
using namespace icu::number::impl;
using icu::StringSegment;
using icu::units::ConversionRates;

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

void mixedMeasuresToMicros(const MaybeStackVector<Measure> &measures, DecimalQuantity *quantity,
                           MicroProps *micros, UErrorCode status) {
    micros->mixedMeasuresCount = measures.length() - 1;
    if (micros->mixedMeasuresCount > 0) {
#ifdef U_DEBUG
        U_ASSERT(micros->outputUnit.getComplexity(status) == UMEASURE_UNIT_MIXED);
        U_ASSERT(U_SUCCESS(status));
        // Check that we received measurements with the expected MeasureUnits:
        int32_t singleUnitsCount;
        LocalArray<MeasureUnit> singleUnits =
            micros->outputUnit.splitToSingleUnits(singleUnitsCount, status);
        U_ASSERT(U_SUCCESS(status));
        U_ASSERT(measures.length() == singleUnitsCount);
        for (int32_t i = 0; i < measures.length(); i++) {
            U_ASSERT(measures[i]->getUnit() == singleUnits[i]);
        }
#endif
        // Mixed units: except for the last value, we pass all values to the
        // LongNameHandler via micros->mixedMeasures.
        if (micros->mixedMeasures.getCapacity() < micros->mixedMeasuresCount) {
            if (micros->mixedMeasures.resize(micros->mixedMeasuresCount) == nullptr) {
                status = U_MEMORY_ALLOCATION_ERROR;
                return;
            }
        }
        for (int32_t i = 0; i < micros->mixedMeasuresCount; i++) {
            micros->mixedMeasures[i] = measures[i]->getNumber().getInt64();
        }
    } else {
        micros->mixedMeasuresCount = 0;
    }
    // The last value (potentially the only value) gets passed on via quantity.
    quantity->setToDouble(measures[measures.length() - 1]->getNumber().getDouble());
}

UsagePrefsHandler::UsagePrefsHandler(const Locale &locale,
                                     const MeasureUnit &inputUnit,
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
    const MaybeStackVector<Measure>& routedUnits = routed.measures;
    micros.outputUnit = routed.outputUnit.copy(status).build(status);
    if (U_FAILURE(status)) {
        return;
    }

    mixedMeasuresToMicros(routedUnits, &quantity, &micros, status);

    UnicodeString precisionSkeleton = routed.precision;
    if (micros.rounder.fPrecision.isBogus()) {
        if (precisionSkeleton.length() > 0) {
            micros.rounder.fPrecision = parseSkeletonToPrecision(precisionSkeleton, status);
        } else {
            // We use the same rounding mode as COMPACT notation: known to be a
            // human-friendly rounding mode: integers, but add a decimal digit
            // as needed to ensure we have at least 2 significant digits.
            micros.rounder.fPrecision = Precision::integer().withMinDigits(2);
        }
    }
}

Precision UsagePrefsHandler::parseSkeletonToPrecision(icu::UnicodeString precisionSkeleton,
                                                      UErrorCode status) {
    if (U_FAILURE(status)) {
        // As a member of UsagePrefsHandler, which is a friend of Precision, we
        // get access to the default constructor.
        return {};
    }
    constexpr int32_t kSkelPrefixLen = 20;
    if (!precisionSkeleton.startsWith(UNICODE_STRING_SIMPLE("precision-increment/"))) {
        status = U_INVALID_FORMAT_ERROR;
        return {};
    }
    U_ASSERT(precisionSkeleton[kSkelPrefixLen - 1] == u'/');
    StringSegment segment(precisionSkeleton, false);
    segment.adjustOffset(kSkelPrefixLen);
    MacroProps macros;
    blueprint_helpers::parseIncrementOption(segment, macros, status);
    return macros.precision;
}

UnitConversionHandler::UnitConversionHandler(const MeasureUnit &unit, const MicroPropsGenerator *parent,
                                             UErrorCode &status)
    : fOutputUnit(unit), fParent(parent) {
    MeasureUnitImpl temp;
    const MeasureUnitImpl &outputUnit = MeasureUnitImpl::forMeasureUnit(unit, temp, status);
    const MeasureUnitImpl *inputUnit = &outputUnit;
    MaybeStackVector<MeasureUnitImpl> singleUnits;
    U_ASSERT(outputUnit.complexity == UMEASURE_UNIT_MIXED);
    // When we wish to support unit conversion, replace the above assert with this if:
    // if (outputUnit.complexity == UMEASURE_UNIT_MIXED) {
    {
        singleUnits = outputUnit.extractIndividualUnits(status);
        U_ASSERT(singleUnits.length() > 0);
        inputUnit = singleUnits[0];
    }
    // TODO: this should become an initOnce thing? Review with other
    // ConversionRates usages.
    ConversionRates conversionRates(status);
    if (U_FAILURE(status)) {
        return;
    }
    fUnitConverter.adoptInsteadAndCheckErrorCode(
        new ComplexUnitsConverter(*inputUnit, outputUnit, conversionRates, status), status);
}

void UnitConversionHandler::processQuantity(DecimalQuantity &quantity, MicroProps &micros,
                                            UErrorCode &status) const {
    fParent->processQuantity(quantity, micros, status);
    if (U_FAILURE(status)) {
        return;
    }
    quantity.roundToInfinity(); // Enables toDouble
    MaybeStackVector<Measure> measures = fUnitConverter->convert(quantity.toDouble(), status);
    micros.outputUnit = fOutputUnit;
    if (U_FAILURE(status)) {
        return;
    }

    mixedMeasuresToMicros(measures, &quantity, &micros, status);

    // TODO: add tests to explore behaviour that may suggest a more
    // human-centric default rounder?
    // if (micros.rounder.fPrecision.isBogus()) {
    //     micros.rounder.fPrecision = Precision::integer().withMinDigits(2);
    // }
}

#endif /* #if !UCONFIG_NO_FORMATTING */
