// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef NUMBERFORMAT_PROPERTIES_H
#define NUMBERFORMAT_PROPERTIES_H

#include "unicode/unistr.h"
#include <cstdint>
#include <unicode/plurrule.h>
#include <unicode/currpinf.h>
#include "unicode/unum.h"
#include "number_types.h"

U_NAMESPACE_BEGIN
namespace number {
namespace impl {

struct DecimalFormatProperties {

  public:
    NullableValue<UNumberCompactStyle> compactStyle;
    NullableValue<CurrencyUnit> currency;
    CopyableLocalPointer <CurrencyPluralInfo> currencyPluralInfo;
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


#endif //NUMBERFORMAT_PROPERTIES_H
