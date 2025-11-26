// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*******************************************************************************
* Copyright (C) 2015, International Business Machines Corporation and         *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/
#ifndef _NUMBER_FORMAT_TEST_TUPLE
#define _NUMBER_FORMAT_TEST_TUPLE

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/decimfmt.h"
#include "unicode/ucurr.h"

#define NFTT_GET_FIELD(tuple, fieldName, defaultValue) ((tuple).fieldName##Flag ? (tuple).fieldName : (defaultValue))

U_NAMESPACE_USE

enum ENumberFormatTestTupleField {
    kLocale,
    kCurrency,
    kPattern,
    kFormat,
    kOutput,
    kComment,
    kMinIntegerDigits,
    kMaxIntegerDigits,
    kMinFractionDigits,
    kMaxFractionDigits,
    kMinGroupingDigits,
    kBreaks,
    kUseSigDigits,
    kMinSigDigits,
    kMaxSigDigits,
    kUseGrouping,
    kMultiplier,
    kRoundingIncrement,
    kFormatWidth,
    kPadCharacter,
    kUseScientific,
    kGrouping,
    kGrouping2,
    kRoundingMode,
    kCurrencyUsage,
    kMinimumExponentDigits,
    kExponentSignAlwaysShown,
    kDecimalSeparatorAlwaysShown,
    kPadPosition,
    kPositivePrefix,
    kPositiveSuffix,
    kNegativePrefix,
    kNegativeSuffix,
    kSignAlwaysShown,
    kLocalizedPattern,
    kToPattern,
    kToLocalizedPattern,
    kStyle,
    kParse,
    kLenient,
    kPlural,
    kParseIntegerOnly,
    kDecimalPatternMatchRequired,
    kParseCaseSensitive,
    kParseNoExponent,
    kOutputCurrency,
    kNumberFormatTestTupleFieldCount
};

/**
 * NumberFormatTestTuple represents the data for a single data driven test.
 * It consist of named fields each of which may or may not be set. Each field
 * has a particular meaning in the test. For more information on what each
 * field means and how the data drive tests work, please see
 * https://docs.google.com/document/d/1T2P0p953_Lh1pRwo-5CuPVrHlIBa_wcXElG-Hhg_WHM/edit?usp=sharing
 * Each field is optional. That is, a certain field may be unset for a given
 * test. The UBool fields ending in "Flag" indicate whether the corresponding
 * field is set or not. true means set; false means unset. An unset field
 * generally means that the corresponding setter method is not called on
 * the NumberFormat object.
 */

class NumberFormatTestTuple final {
public:
    Locale locale;
    UnicodeString currency;
    UnicodeString pattern;
    UnicodeString format;
    UnicodeString output;
    UnicodeString comment;
    int32_t minIntegerDigits;
    int32_t maxIntegerDigits;
    int32_t minFractionDigits;
    int32_t maxFractionDigits;
    int32_t minGroupingDigits;
    UnicodeString breaks;
    int32_t useSigDigits;
    int32_t minSigDigits;
    int32_t maxSigDigits;
    int32_t useGrouping;
    int32_t multiplier;
    double roundingIncrement;
    int32_t formatWidth;
    UnicodeString padCharacter;
    int32_t useScientific;
    int32_t grouping;
    int32_t grouping2;
    DecimalFormat::ERoundingMode roundingMode;
    UCurrencyUsage currencyUsage;
    int32_t minimumExponentDigits;
    int32_t exponentSignAlwaysShown;
    int32_t decimalSeparatorAlwaysShown;
    DecimalFormat::EPadPosition padPosition;
    UnicodeString positivePrefix;
    UnicodeString positiveSuffix;
    UnicodeString negativePrefix;
    UnicodeString negativeSuffix;
    int32_t signAlwaysShown;
    UnicodeString localizedPattern;
    UnicodeString toPattern;
    UnicodeString toLocalizedPattern;
    UNumberFormatStyle style;
    UnicodeString parse;
    int32_t lenient;
    UnicodeString plural;
    int32_t parseIntegerOnly;
    int32_t decimalPatternMatchRequired;
    int32_t parseNoExponent;
    int32_t parseCaseSensitive;
    UnicodeString outputCurrency;

    UBool localeFlag;
    UBool currencyFlag;
    UBool patternFlag;
    UBool formatFlag;
    UBool outputFlag;
    UBool commentFlag;
    UBool minIntegerDigitsFlag;
    UBool maxIntegerDigitsFlag;
    UBool minFractionDigitsFlag;
    UBool maxFractionDigitsFlag;
    UBool minGroupingDigitsFlag;
    UBool breaksFlag;
    UBool useSigDigitsFlag;
    UBool minSigDigitsFlag;
    UBool maxSigDigitsFlag;
    UBool useGroupingFlag;
    UBool multiplierFlag;
    UBool roundingIncrementFlag;
    UBool formatWidthFlag;
    UBool padCharacterFlag;
    UBool useScientificFlag;
    UBool groupingFlag;
    UBool grouping2Flag;
    UBool roundingModeFlag;
    UBool currencyUsageFlag;
    UBool minimumExponentDigitsFlag;
    UBool exponentSignAlwaysShownFlag;
    UBool decimalSeparatorAlwaysShownFlag;
    UBool padPositionFlag;
    UBool positivePrefixFlag;
    UBool positiveSuffixFlag;
    UBool negativePrefixFlag;
    UBool negativeSuffixFlag;
    UBool signAlwaysShownFlag;
    UBool localizedPatternFlag;
    UBool toPatternFlag;
    UBool toLocalizedPatternFlag;
    UBool styleFlag;
    UBool parseFlag;
    UBool lenientFlag;
    UBool pluralFlag;
    UBool parseIntegerOnlyFlag;
    UBool decimalPatternMatchRequiredFlag;
    UBool parseNoExponentFlag;
    UBool parseCaseSensitiveFlag;
    UBool outputCurrencyFlag;

public:
    struct Numberformattesttuple_EnumConversion {
        const char *str;
        int32_t value;
    };
private:
    static int32_t toEnum(
            const Numberformattesttuple_EnumConversion *table,
            int32_t tableLength,
            const UnicodeString &str,
            UErrorCode &status);

    static void fromEnum(
            const Numberformattesttuple_EnumConversion *table,
            int32_t tableLength,
            int32_t val,
            UnicodeString &appendTo);

    static void identVal(
            const UnicodeString &str, void *strPtr, UErrorCode & /*status*/);

    static void identStr(
            const void *strPtr, UnicodeString &appendTo);

    static void strToLocale(
            const UnicodeString &str, void *localePtr, UErrorCode &status);

    static void localeToStr(
            const void *localePtr, UnicodeString &appendTo);

    static void strToInt(
            const UnicodeString &str, void *intPtr, UErrorCode &status);

    static void intToStr(
            const void *intPtr, UnicodeString &appendTo);

    static void strToDouble(
            const UnicodeString &str, void *doublePtr, UErrorCode &status);

    static void doubleToStr(
            const void *doublePtr, UnicodeString &appendTo);

    static void strToERounding(
            const UnicodeString &str, void *roundPtr, UErrorCode &status);

    static void eRoundingToStr(
            const void *roundPtr, UnicodeString &appendTo);

    static void strToCurrencyUsage(
            const UnicodeString &str, void *currencyUsagePtr, UErrorCode &status);

    static void currencyUsageToStr(
            const void *currencyUsagePtr, UnicodeString &appendTo);

    static void strToEPadPosition(
            const UnicodeString &str, void *padPositionPtr, UErrorCode &status);

    static void ePadPositionToStr(
            const void *padPositionPtr, UnicodeString &appendTo);

    static void strToFormatStyle(
            const UnicodeString &str, void *formatStylePtr, UErrorCode &status);

    static void formatStyleToStr(
            const void *formatStylePtr, UnicodeString &appendTo);

    struct NumberFormatTestTupleFieldOps {
        void (*toValue)(const UnicodeString &str, void *valPtr, UErrorCode &);
        void (*toString)(const void *valPtr, UnicodeString &appendTo);
    };

    const NumberFormatTestTupleFieldOps gStrOps = {identVal, identStr};
    const NumberFormatTestTupleFieldOps gIntOps = {strToInt, intToStr};
    const NumberFormatTestTupleFieldOps gLocaleOps = {strToLocale, localeToStr};
    const NumberFormatTestTupleFieldOps gDoubleOps = {strToDouble, doubleToStr};
    const NumberFormatTestTupleFieldOps gERoundingOps = {strToERounding, eRoundingToStr};
    const NumberFormatTestTupleFieldOps gCurrencyUsageOps = {strToCurrencyUsage, currencyUsageToStr};
    const NumberFormatTestTupleFieldOps gEPadPositionOps = {strToEPadPosition, ePadPositionToStr};
    const NumberFormatTestTupleFieldOps gFormatStyleOps = {strToFormatStyle, formatStyleToStr};

#define FIELD_INIT(fieldName, fieldType) {#fieldName, &fieldName, fieldName##Flag, fieldType}

    struct NumberFormatTestTupleFieldData {
        const char *name;
        void* fieldPtr;
        UBool& flag;
        const NumberFormatTestTupleFieldOps *ops;
    };

    // Order must correspond to ENumberFormatTestTupleField
    const NumberFormatTestTupleFieldData gFieldData[kNumberFormatTestTupleFieldCount] = {
        FIELD_INIT(locale, &gLocaleOps),
        FIELD_INIT(currency, &gStrOps),
        FIELD_INIT(pattern, &gStrOps),
        FIELD_INIT(format, &gStrOps),
        FIELD_INIT(output, &gStrOps),
        FIELD_INIT(comment, &gStrOps),
        FIELD_INIT(minIntegerDigits, &gIntOps),
        FIELD_INIT(maxIntegerDigits, &gIntOps),
        FIELD_INIT(minFractionDigits, &gIntOps),
        FIELD_INIT(maxFractionDigits, &gIntOps),
        FIELD_INIT(minGroupingDigits, &gIntOps),
        FIELD_INIT(breaks, &gStrOps),
        FIELD_INIT(useSigDigits, &gIntOps),
        FIELD_INIT(minSigDigits, &gIntOps),
        FIELD_INIT(maxSigDigits, &gIntOps),
        FIELD_INIT(useGrouping, &gIntOps),
        FIELD_INIT(multiplier, &gIntOps),
        FIELD_INIT(roundingIncrement, &gDoubleOps),
        FIELD_INIT(formatWidth, &gIntOps),
        FIELD_INIT(padCharacter, &gStrOps),
        FIELD_INIT(useScientific, &gIntOps),
        FIELD_INIT(grouping, &gIntOps),
        FIELD_INIT(grouping2, &gIntOps),
        FIELD_INIT(roundingMode, &gERoundingOps),
        FIELD_INIT(currencyUsage, &gCurrencyUsageOps),
        FIELD_INIT(minimumExponentDigits, &gIntOps),
        FIELD_INIT(exponentSignAlwaysShown, &gIntOps),
        FIELD_INIT(decimalSeparatorAlwaysShown, &gIntOps),
        FIELD_INIT(padPosition, &gEPadPositionOps),
        FIELD_INIT(positivePrefix, &gStrOps),
        FIELD_INIT(positiveSuffix, &gStrOps),
        FIELD_INIT(negativePrefix, &gStrOps),
        FIELD_INIT(negativeSuffix, &gStrOps),
        FIELD_INIT(signAlwaysShown, &gIntOps),
        FIELD_INIT(localizedPattern, &gStrOps),
        FIELD_INIT(toPattern, &gStrOps),
        FIELD_INIT(toLocalizedPattern, &gStrOps),
        FIELD_INIT(style, &gFormatStyleOps),
        FIELD_INIT(parse, &gStrOps),
        FIELD_INIT(lenient, &gIntOps),
        FIELD_INIT(plural, &gStrOps),
        FIELD_INIT(parseIntegerOnly, &gIntOps),
        FIELD_INIT(decimalPatternMatchRequired, &gIntOps),
        FIELD_INIT(parseNoExponent, &gIntOps),
        FIELD_INIT(parseCaseSensitive, &gIntOps),
        FIELD_INIT(outputCurrency, &gStrOps)
    };
public:

    NumberFormatTestTuple() {
        clear();
    }

    /**
     * Sets a particular field using the string representation of that field.
     * @param field the field to set.
     * @param fieldValue the string representation of the field value.
     * @param status error returned here such as when the string representation
     *  of the field value cannot be parsed.
     * @return true on success or false if an error was set in status.
     */
    UBool setField(
            ENumberFormatTestTupleField field,
            const UnicodeString &fieldValue,
            UErrorCode &status);
    /**
     * Clears a particular field.
     * @param field the field to clear.
     * @param status error set here.
     * @return true on success or false if error was set.
     */
    UBool clearField(
            ENumberFormatTestTupleField field,
            UErrorCode &status);
    /**
     * Clears every field.
     */
    void clear();

    /**
     * Returns the string representation of the test case this object
     * currently represents.
     * @param appendTo the result appended here.
     * @return appendTo
     */
    UnicodeString &toString(UnicodeString &appendTo) const;

    /**
     * Converts the name of a field to the corresponding enum value.
     * @param name the name of the field as a string.
     * @return the corresponding enum value or kNumberFormatTestFieldCount
     *   if name does not map to any recognized field name.
     */
    ENumberFormatTestTupleField getFieldByName(const UnicodeString &name) const;
private:
    const void *getFieldAddress(int32_t fieldId) const;
    void *getMutableFieldAddress(int32_t fieldId);
    void setFlag(int32_t fieldId, UBool value);
    UBool isFlag(int32_t fieldId) const;
};

#endif /* !UCONFIG_NO_FORMATTING */
#endif
