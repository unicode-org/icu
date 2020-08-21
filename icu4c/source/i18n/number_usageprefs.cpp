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

MicroPropsGenerator::~MicroPropsGenerator() = default;

// SymbolsWrapper::SymbolsWrapper(const SymbolsWrapper& other) {
//     doCopyFrom(other);
// }

// SymbolsWrapper::SymbolsWrapper(SymbolsWrapper&& src) U_NOEXCEPT {
//     doMoveFrom(std::move(src));
// }

// SymbolsWrapper& SymbolsWrapper::operator=(const SymbolsWrapper& other) {
//     if (this == &other) {
//         return *this;
//     }
//     doCleanup();
//     doCopyFrom(other);
//     return *this;
// }

// SymbolsWrapper& SymbolsWrapper::operator=(SymbolsWrapper&& src) U_NOEXCEPT {
//     if (this == &src) {
//         return *this;
//     }
//     doCleanup();
//     doMoveFrom(std::move(src));
//     return *this;
// }

SymbolsWrapper::~SymbolsWrapper() {
    doCleanup();
}

// void SymbolsWrapper::setTo(const DecimalFormatSymbols& dfs) {
//     doCleanup();
//     fType = SYMPTR_DFS;
//     fPtr.dfs = new DecimalFormatSymbols(dfs);
// }

// void SymbolsWrapper::setTo(const NumberingSystem* ns) {
//     doCleanup();
//     fType = SYMPTR_NS;
//     fPtr.ns = ns;
// }

// void SymbolsWrapper::doCopyFrom(const SymbolsWrapper& other) {
//     fType = other.fType;
//     switch (fType) {
//         case SYMPTR_NONE:
//             // No action necessary
//             break;
//         case SYMPTR_DFS:
//             // Memory allocation failures are exposed in copyErrorTo()
//             if (other.fPtr.dfs != nullptr) {
//                 fPtr.dfs = new DecimalFormatSymbols(*other.fPtr.dfs);
//             } else {
//                 fPtr.dfs = nullptr;
//             }
//             break;
//         case SYMPTR_NS:
//             // Memory allocation failures are exposed in copyErrorTo()
//             if (other.fPtr.ns != nullptr) {
//                 fPtr.ns = new NumberingSystem(*other.fPtr.ns);
//             } else {
//                 fPtr.ns = nullptr;
//             }
//             break;
//     }
// }

// void SymbolsWrapper::doMoveFrom(SymbolsWrapper&& src) {
//     fType = src.fType;
//     switch (fType) {
//         case SYMPTR_NONE:
//             // No action necessary
//             break;
//         case SYMPTR_DFS:
//             fPtr.dfs = src.fPtr.dfs;
//             src.fPtr.dfs = nullptr;
//             break;
//         case SYMPTR_NS:
//             fPtr.ns = src.fPtr.ns;
//             src.fPtr.ns = nullptr;
//             break;
//     }
// }

void SymbolsWrapper::doCleanup() {
    switch (fType) {
        case SYMPTR_NONE:
            // No action necessary
            break;
        case SYMPTR_DFS:
            delete fPtr.dfs;
            break;
        case SYMPTR_NS:
            delete fPtr.ns;
            break;
    }
}

// bool SymbolsWrapper::isDecimalFormatSymbols() const {
//     return fType == SYMPTR_DFS;
// }

// bool SymbolsWrapper::isNumberingSystem() const {
//     return fType == SYMPTR_NS;
// }

Precision parseSkeletonToPrecision(icu::UnicodeString precisionSkeleton, UErrorCode status) {
    if (U_FAILURE(status)) {
        return {};
    }
    constexpr int32_t kSkelPrefixLen = 20;
    if (!precisionSkeleton.startsWith(UNICODE_STRING_SIMPLE("precision-increment/"))) {
        status = U_INVALID_FORMAT_ERROR;
        return {};
    }
    U_ASSERT(precisionSkeleton[kSkelPrefixLen-1] == u'/');
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
    // TODO: Is it okay if this is kDefaultMode?
    // Otherwise it was: unitPrefMacros.roundingMode or precision.fRoundingMode;
    UNumberFormatRoundingMode roundingMode = kDefaultMode;
    CurrencyUnit currency(u"", status);
    if (!micros.rounder.isPassThrough()) {
        // Do nothing: we already have a rounder, so we don't use
        // precisionSkeleton or a default "usage-appropriate" rounder.
    } else if (precisionSkeleton.length() > 0) {
        CharString csPrecisionSkeleton;
        UErrorCode csErrCode = U_ZERO_ERROR;
        csPrecisionSkeleton.appendInvariantChars(precisionSkeleton, csErrCode);

        // Parse skeleton, collect results
        // int32_t errOffset;
        // int32_t errOffset = 0;
        U_ASSERT(U_SUCCESS(status));
        Precision precision = parseSkeletonToPrecision(precisionSkeleton, status);
        micros.rounder = {precision, roundingMode, currency, status};
    } else {
        Precision precision = Precision::integer().withMinDigits(2);
        micros.rounder = {precision, roundingMode, currency, status};
    }
}

#endif /* #if !UCONFIG_NO_FORMATTING */
