// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT

// Allow implicit conversion from char16_t* to UnicodeString for this file:
// Helpful in toString methods and elsewhere.
#define UNISTR_FROM_STRING_EXPLICIT

#include "unicode/numberrangeformatter.h"
#include "numrange_impl.h"
#include "uresimp.h"
#include "util.h"

using namespace icu;
using namespace icu::number;
using namespace icu::number::impl;

namespace {

// Helper function for 2-dimensional switch statement
constexpr int8_t identity2d(UNumberIdentityFallback a, UNumberRangeIdentityResult b) {
    return static_cast<int8_t>(a) | (static_cast<int8_t>(b) << 4);
}


class NumberRangeDataSink : public ResourceSink {
  public:
    NumberRangeDataSink(NumberRangeData& data) : fData(data) {}

    void put(const char* key, ResourceValue& value, UBool /*noFallback*/, UErrorCode& status) U_OVERRIDE {
        ResourceTable miscTable = value.getTable(status);
        if (U_FAILURE(status)) { return; }
        for (int i = 0; miscTable.getKeyAndValue(i, key, value); i++) {
            if (uprv_strcmp(key, "range") == 0) {
                if (fData.rangePattern.getArgumentLimit() == 0) {
                    continue; // have already seen this pattern
                }
                fData.rangePattern = {value.getUnicodeString(status), status};
            } else if (uprv_strcmp(key, "approximately") == 0) {
                if (fData.approximatelyPattern.getArgumentLimit() == 0) {
                    continue; // have already seen this pattern
                }
                fData.approximatelyPattern = {value.getUnicodeString(status), status};
            }
        }
    }

  private:
    NumberRangeData& fData;
};

void getNumberRangeData(const char* localeName, const char* nsName, NumberRangeData& data, UErrorCode& status) {
    if (U_FAILURE(status)) { return; }
    LocalUResourceBundlePointer rb(ures_open(NULL, localeName, &status));
    if (U_FAILURE(status)) { return; }
    NumberRangeDataSink sink(data);

    CharString dataPath;
    dataPath.append("NumberElements/", -1, status);
    dataPath.append(nsName, -1, status);
    dataPath.append("/miscPatterns", -1, status);
    ures_getAllItemsWithFallback(rb.getAlias(), dataPath.data(), sink, status);
    if (U_FAILURE(status)) { return; }

    if (uprv_strcmp(nsName, "latn") != 0 && (data.rangePattern.getArgumentLimit() == 0
            || data.approximatelyPattern.getArgumentLimit() == 0)) {
        // fall back to Latin data
        ures_getAllItemsWithFallback(rb.getAlias(), "NumberElements/latn/miscPatterns", sink, status);
        if (U_FAILURE(status)) { return; }
    }

    if (data.rangePattern.getArgumentLimit() == 0) {
        // No data!
        data.rangePattern = {u"{0} --- {1}", status};
    }
    if (data.approximatelyPattern.getArgumentLimit() == 0) {
        // No data!
        data.approximatelyPattern = {u"~{0}", status};
    }
}

} // namespace


NumberRangeFormatterImpl::NumberRangeFormatterImpl(const RangeMacroProps& macros, UErrorCode& status)
    : formatterImpl1(macros.formatter1.fMacros, status),
      formatterImpl2(macros.formatter2.fMacros, status),
      fSameFormatters(true), // FIXME
      fCollapse(macros.collapse),
      fIdentityFallback(macros.identityFallback) {
    // TODO: get local ns
    NumberRangeData data;
    getNumberRangeData(macros.locale.getName(), "latn", data, status);
    if (U_FAILURE(status)) { return; }
    fRangeFormatter = data.rangePattern;
    fApproximatelyModifier = {data.approximatelyPattern, UNUM_FIELD_COUNT, false};
}

void NumberRangeFormatterImpl::format(UFormattedNumberRangeData& data, bool equalBeforeRounding, UErrorCode& status) const {
    if (U_FAILURE(status)) {
        return;
    }

    // Identity case 1: equal before rounding
    if (equalBeforeRounding) {
    }

    MicroProps micros1;
    MicroProps micros2;
    formatterImpl1.preProcess(data.quantity1, micros1, status);
    formatterImpl2.preProcess(data.quantity2, micros2, status);
    if (U_FAILURE(status)) {
        return;
    }

    // Check for identity
    if (equalBeforeRounding) {
        data.identityResult = UNUM_IDENTITY_RESULT_EQUAL_BEFORE_ROUNDING;
    } else if (data.quantity1 == data.quantity2) {
        data.identityResult = UNUM_IDENTITY_RESULT_EQUAL_AFTER_ROUNDING;
    } else {
        data.identityResult = UNUM_IDENTITY_RESULT_NOT_EQUAL;
    }

    switch (identity2d(fIdentityFallback, data.identityResult)) {
        case identity2d(UNUM_IDENTITY_FALLBACK_RANGE,
                        UNUM_IDENTITY_RESULT_NOT_EQUAL):
        case identity2d(UNUM_IDENTITY_FALLBACK_RANGE,
                        UNUM_IDENTITY_RESULT_EQUAL_AFTER_ROUNDING):
        case identity2d(UNUM_IDENTITY_FALLBACK_RANGE,
                        UNUM_IDENTITY_RESULT_EQUAL_BEFORE_ROUNDING):
        case identity2d(UNUM_IDENTITY_FALLBACK_APPROXIMATELY,
                        UNUM_IDENTITY_RESULT_NOT_EQUAL):
        case identity2d(UNUM_IDENTITY_FALLBACK_APPROXIMATELY_OR_SINGLE_VALUE,
                        UNUM_IDENTITY_RESULT_NOT_EQUAL):
        case identity2d(UNUM_IDENTITY_FALLBACK_SINGLE_VALUE,
                        UNUM_IDENTITY_RESULT_NOT_EQUAL):
            formatRange(data, micros1, micros2, status);
            break;

        case identity2d(UNUM_IDENTITY_FALLBACK_APPROXIMATELY,
                        UNUM_IDENTITY_RESULT_EQUAL_AFTER_ROUNDING):
        case identity2d(UNUM_IDENTITY_FALLBACK_APPROXIMATELY,
                        UNUM_IDENTITY_RESULT_EQUAL_BEFORE_ROUNDING):
        case identity2d(UNUM_IDENTITY_FALLBACK_APPROXIMATELY_OR_SINGLE_VALUE,
                        UNUM_IDENTITY_RESULT_EQUAL_AFTER_ROUNDING):
            formatApproximately(data, micros1, micros2, status);
            break;

        case identity2d(UNUM_IDENTITY_FALLBACK_APPROXIMATELY_OR_SINGLE_VALUE,
                        UNUM_IDENTITY_RESULT_EQUAL_BEFORE_ROUNDING):
        case identity2d(UNUM_IDENTITY_FALLBACK_SINGLE_VALUE,
                        UNUM_IDENTITY_RESULT_EQUAL_AFTER_ROUNDING):
        case identity2d(UNUM_IDENTITY_FALLBACK_SINGLE_VALUE,
                        UNUM_IDENTITY_RESULT_EQUAL_BEFORE_ROUNDING):
            formatSingleValue(data, micros1, micros2, status);
            break;

        default:
            U_ASSERT(false);
            break;
    }
}


void NumberRangeFormatterImpl::formatSingleValue(UFormattedNumberRangeData& data,
                                                 MicroProps& micros1, MicroProps& micros2,
                                                 UErrorCode& status) const {
    if (fSameFormatters) {
        formatterImpl1.format(data.quantity1, data.string, status);
    } else {
        formatRange(data, micros1, micros2, status);
    }
}


void NumberRangeFormatterImpl::formatApproximately (UFormattedNumberRangeData& data,
                                                    MicroProps& micros1, MicroProps& micros2,
                                                    UErrorCode& status) const {
    if (fSameFormatters) {
        int32_t length = formatterImpl1.format(data.quantity1, data.string, status);
        fApproximatelyModifier.apply(data.string, 0, length, status);
    } else {
        formatRange(data, micros1, micros2, status);
    }
}


void NumberRangeFormatterImpl::formatRange(UFormattedNumberRangeData& data,
                                           MicroProps& micros1, MicroProps& micros2,
                                           UErrorCode& status) const {

    // modInner is always notation (scientific); collapsable in ALL.
    // modOuter is always units; collapsable in ALL, AUTO, and UNIT.
    // modMiddle could be either; collapsable in ALL and sometimes AUTO and UNIT.
    // Never collapse an outer mod but not an inner mod.
    bool collapseOuter, collapseMiddle, collapseInner;
    switch (fCollapse) {
        case UNUM_RANGE_COLLAPSE_ALL:
        case UNUM_RANGE_COLLAPSE_AUTO:
        case UNUM_RANGE_COLLAPSE_UNIT:
        {
            // OUTER MODIFIER
            collapseOuter = *micros1.modOuter == *micros2.modOuter;

            if (!collapseOuter) {
                // Never collapse inner mods if outer mods are not collapsable
                collapseMiddle = false;
                collapseInner = false;
                break;
            }

            // MIDDLE MODIFIER
            collapseMiddle = *micros1.modMiddle == *micros2.modMiddle;

            if (!collapseMiddle) {
                // Never collapse inner mods if outer mods are not collapsable
                collapseInner = false;
                break;
            }

            // MIDDLE MODIFIER HEURISTICS
            // (could disable collapsing of the middle modifier)
            // The modifiers are equal by this point, so we can look at just one of them.
            const Modifier* mm = micros1.modMiddle;
            if (mm == nullptr) {
                // pass
            } else if (fCollapse == UNUM_RANGE_COLLAPSE_UNIT) {
                // Only collapse if the modifier is a unit.
                // TODO: Make a better way to check for a unit?
                // TODO: Handle case where the modifier has both notation and unit (compact currency)?
                if (!mm->containsField(UNUM_CURRENCY_FIELD) && !mm->containsField(UNUM_PERCENT_FIELD)) {
                    collapseMiddle = false;
                }
            } else if (fCollapse == UNUM_RANGE_COLLAPSE_AUTO) {
                // Heuristic as of ICU 63: collapse only if the modifier is exactly one code point.
                if (mm->getCodePointCount() != 1) {
                    collapseMiddle = false;
                }
            }

            if (!collapseMiddle || fCollapse != UNUM_RANGE_COLLAPSE_ALL) {
                collapseInner = false;
                break;
            }

            // INNER MODIFIER
            collapseInner = *micros1.modInner == *micros2.modInner;

            // All done checking for collapsability.
            break;
        }

        default:
            collapseOuter = false;
            collapseMiddle = false;
            collapseInner = false;
            break;
    }

    NumberStringBuilder& string = data.string;
    int32_t lengthPrefix = 0;
    int32_t length1 = 0;
    int32_t lengthInfix = 0;
    int32_t length2 = 0;
    int32_t lengthSuffix = 0;
    #define UPRV_INDEX_0 (lengthPrefix)
    #define UPRV_INDEX_1 (lengthPrefix + length1)
    #define UPRV_INDEX_2 (lengthPrefix + length1 + lengthInfix)
    #define UPRV_INDEX_3 (lengthPrefix + length1 + lengthInfix + length2)

    int32_t lengthRange = SimpleModifier::formatTwoArgPattern(
        fRangeFormatter,
        string,
        0,
        &lengthPrefix,
        &lengthSuffix,
        UNUM_FIELD_COUNT,
        status);
    if (U_FAILURE(status)) { return; }
    lengthInfix = lengthRange - lengthPrefix - lengthSuffix;

    length1 += NumberFormatterImpl::writeNumber(micros1, data.quantity1, string, UPRV_INDEX_0, status);
    length2 += NumberFormatterImpl::writeNumber(micros2, data.quantity2, string, UPRV_INDEX_2, status);

    // TODO: Support padding?

    if (collapseInner) {
        lengthInfix += micros1.modInner->apply(string, UPRV_INDEX_0, UPRV_INDEX_3, status);
    } else {
        length1 += micros1.modInner->apply(string, UPRV_INDEX_0, UPRV_INDEX_1, status);
        length2 += micros1.modInner->apply(string, UPRV_INDEX_2, UPRV_INDEX_3, status);
    }

    if (collapseMiddle) {
        lengthInfix += micros1.modMiddle->apply(string, UPRV_INDEX_0, UPRV_INDEX_3, status);
    } else {
        length1 += micros1.modMiddle->apply(string, UPRV_INDEX_0, UPRV_INDEX_1, status);
        length2 += micros1.modMiddle->apply(string, UPRV_INDEX_2, UPRV_INDEX_3, status);
    }

    if (collapseOuter) {
        micros1.modOuter->apply(string, UPRV_INDEX_0, UPRV_INDEX_3, status);
    } else {
        micros1.modOuter->apply(string, UPRV_INDEX_0, UPRV_INDEX_1, status);
        micros1.modOuter->apply(string, UPRV_INDEX_2, UPRV_INDEX_3, status);
    }
}



#endif /* #if !UCONFIG_NO_FORMATTING */
