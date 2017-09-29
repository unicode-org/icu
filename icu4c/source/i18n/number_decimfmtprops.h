// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#if !UCONFIG_NO_FORMATTING
#ifndef __NUMBER_DECIMFMTPROPS_H__
#define __NUMBER_DECIMFMTPROPS_H__

#include "unicode/unistr.h"
#include <cstdint>
#include "unicode/plurrule.h"
#include "unicode/currpinf.h"
#include "unicode/unum.h"
#include "unicode/localpointer.h"
#include "number_types.h"

U_NAMESPACE_BEGIN
namespace number {
namespace impl {

// TODO: Figure out a nicer way to deal with CurrencyPluralInfo.
// Exported as U_I18N_API because it is a public member field of exported DecimalFormatProperties
struct U_I18N_API CurrencyPluralInfoWrapper {
	UPRV_SUPPRESS_DLL_INTERFACE_WARNING  // LocalPointer is defined in a header file; why does Visual Studio complain?
    LocalPointer<CurrencyPluralInfo> fPtr;

    CurrencyPluralInfoWrapper() {}
    CurrencyPluralInfoWrapper(const CurrencyPluralInfoWrapper& other) {
        if (!other.fPtr.isNull()) {
            fPtr.adoptInstead(new CurrencyPluralInfo(*other.fPtr));
        }
    }
};

// Exported as U_I18N_API because it is needed for the unit test PatternStringTest
struct U_I18N_API DecimalFormatProperties {

  public:
    NullableValue<UNumberCompactStyle> compactStyle;
    NullableValue<CurrencyUnit> currency;
    CurrencyPluralInfoWrapper currencyPluralInfo;
    NullableValue<UCurrencyUsage> currencyUsage;
    bool decimalPatternMatchRequired;
    bool decimalSeparatorAlwaysShown;
    bool exponentSignAlwaysShown;
    int32_t formatWidth;
    int32_t groupingSize;
    int32_t magnitudeMultiplier;
    int32_t maximumFractionDigits;
    int32_t maximumIntegerDigits;
    int32_t maximumSignificantDigits;
    int32_t minimumExponentDigits;
    int32_t minimumFractionDigits;
    int32_t minimumGroupingDigits;
    int32_t minimumIntegerDigits;
    int32_t minimumSignificantDigits;
    int32_t multiplier;
    UnicodeString negativePrefix;
    UnicodeString negativePrefixPattern;
    UnicodeString negativeSuffix;
    UnicodeString negativeSuffixPattern;
    NullableValue<PadPosition> padPosition;
    UnicodeString padString;
    bool parseCaseSensitive;
    bool parseIntegerOnly;
    bool parseLenient;
    bool parseNoExponent;
    bool parseToBigDecimal;
    //PluralRules pluralRules;
    UnicodeString positivePrefix;
    UnicodeString positivePrefixPattern;
    UnicodeString positiveSuffix;
    UnicodeString positiveSuffixPattern;
    double roundingIncrement;
    NullableValue<RoundingMode> roundingMode;
    int32_t secondaryGroupingSize;
    bool signAlwaysShown;

    DecimalFormatProperties();

    //DecimalFormatProperties(const DecimalFormatProperties &other) = default;

    DecimalFormatProperties &operator=(const DecimalFormatProperties &other) = default;

    bool operator==(const DecimalFormatProperties &other) const;

    void clear();
};

} // namespace impl
} // namespace number
U_NAMESPACE_END


#endif //__NUMBER_DECIMFMTPROPS_H__

#endif /* #if !UCONFIG_NO_FORMATTING */
