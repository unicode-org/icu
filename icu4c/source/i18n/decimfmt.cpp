// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include <cmath>
#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT

// Allow implicit conversion from char16_t* to UnicodeString for this file:
// Helpful in toString methods and elsewhere.
#define UNISTR_FROM_STRING_EXPLICIT

#include "unicode/errorcode.h"
#include "unicode/decimfmt.h"
#include "number_decimalquantity.h"
#include "number_types.h"
#include "numparse_impl.h"
#include "number_mapper.h"
#include "number_patternstring.h"
#include "putilimp.h"
#include "number_utils.h"

using namespace icu;
using namespace icu::number;
using namespace icu::number::impl;
using namespace icu::numparse;
using namespace icu::numparse::impl;
using ERoundingMode = icu::DecimalFormat::ERoundingMode;
using EPadPosition = icu::DecimalFormat::EPadPosition;


UOBJECT_DEFINE_RTTI_IMPLEMENTATION(DecimalFormat)


DecimalFormat::DecimalFormat(UErrorCode& status)
        : DecimalFormat(nullptr, status) {
}

DecimalFormat::DecimalFormat(const UnicodeString& pattern, UErrorCode& status)
        : DecimalFormat(nullptr, status) {
    setPropertiesFromPattern(pattern, IGNORE_ROUNDING_IF_CURRENCY, status);
    refreshFormatter(status);
}

DecimalFormat::DecimalFormat(const UnicodeString& pattern, DecimalFormatSymbols* symbolsToAdopt,
                             UErrorCode& status)
        : DecimalFormat(symbolsToAdopt, status) {
    setPropertiesFromPattern(pattern, IGNORE_ROUNDING_IF_CURRENCY, status);
    refreshFormatter(status);
}

DecimalFormat::DecimalFormat(const UnicodeString& pattern, DecimalFormatSymbols* symbolsToAdopt,
                             UNumberFormatStyle style, UErrorCode& status)
        : DecimalFormat(symbolsToAdopt, status) {
    // If choice is a currency type, ignore the rounding information.
    if (style == UNumberFormatStyle::UNUM_CURRENCY || style == UNumberFormatStyle::UNUM_CURRENCY_ISO ||
        style == UNumberFormatStyle::UNUM_CURRENCY_ACCOUNTING ||
        style == UNumberFormatStyle::UNUM_CASH_CURRENCY ||
        style == UNumberFormatStyle::UNUM_CURRENCY_STANDARD ||
        style == UNumberFormatStyle::UNUM_CURRENCY_PLURAL) {
        setPropertiesFromPattern(pattern, IGNORE_ROUNDING_ALWAYS, status);
    } else {
        setPropertiesFromPattern(pattern, IGNORE_ROUNDING_IF_CURRENCY, status);
    }
    // Note: in Java, CurrencyPluralInfo is set in NumberFormat.java, but in C++, it is not set there,
    // so we have to set it here.
    if (style == UNumberFormatStyle::UNUM_CURRENCY_PLURAL) {
        LocalPointer<CurrencyPluralInfo> cpi(
                new CurrencyPluralInfo(fSymbols->getLocale(), status),
                status);
        if (U_FAILURE(status)) { return; }
        fProperties->currencyPluralInfo.fPtr.adoptInstead(cpi.orphan());
    }
    refreshFormatter(status);
}

DecimalFormat::DecimalFormat(const DecimalFormatSymbols* symbolsToAdopt, UErrorCode& status) {
    fProperties.adoptInsteadAndCheckErrorCode(new DecimalFormatProperties(), status);
    fExportedProperties.adoptInsteadAndCheckErrorCode(new DecimalFormatProperties(), status);
    fWarehouse.adoptInsteadAndCheckErrorCode(new DecimalFormatWarehouse(), status);
    if (symbolsToAdopt == nullptr) {
        fSymbols.adoptInsteadAndCheckErrorCode(new DecimalFormatSymbols(status), status);
    } else {
        fSymbols.adoptInsteadAndCheckErrorCode(symbolsToAdopt, status);
    }
}

#if UCONFIG_HAVE_PARSEALLINPUT

void DecimalFormat::setParseAllInput(UNumberFormatAttributeValue value) {
    fProperties->parseAllInput = value;
}

#endif

DecimalFormat&
DecimalFormat::setAttribute(UNumberFormatAttribute attr, int32_t newValue, UErrorCode& status) {
    if (U_FAILURE(status)) { return *this; }

    switch (attr) {
        case UNUM_LENIENT_PARSE:
            setLenient(newValue != 0);
            break;

        case UNUM_PARSE_INT_ONLY:
            setParseIntegerOnly(newValue != 0);
            break;

        case UNUM_GROUPING_USED:
            setGroupingUsed(newValue != 0);
            break;

        case UNUM_DECIMAL_ALWAYS_SHOWN:
            setDecimalSeparatorAlwaysShown(newValue != 0);
            break;

        case UNUM_MAX_INTEGER_DIGITS:
            setMaximumIntegerDigits(newValue);
            break;

        case UNUM_MIN_INTEGER_DIGITS:
            setMinimumIntegerDigits(newValue);
            break;

        case UNUM_INTEGER_DIGITS:
            setMinimumIntegerDigits(newValue);
            setMaximumIntegerDigits(newValue);
            break;

        case UNUM_MAX_FRACTION_DIGITS:
            setMaximumFractionDigits(newValue);
            break;

        case UNUM_MIN_FRACTION_DIGITS:
            setMinimumFractionDigits(newValue);
            break;

        case UNUM_FRACTION_DIGITS:
            setMinimumFractionDigits(newValue);
            setMaximumFractionDigits(newValue);
            break;

        case UNUM_SIGNIFICANT_DIGITS_USED:
            setSignificantDigitsUsed(newValue != 0);
            break;

        case UNUM_MAX_SIGNIFICANT_DIGITS:
            setMaximumSignificantDigits(newValue);
            break;

        case UNUM_MIN_SIGNIFICANT_DIGITS:
            setMinimumSignificantDigits(newValue);
            break;

        case UNUM_MULTIPLIER:
            setMultiplier(newValue);
            break;

        case UNUM_GROUPING_SIZE:
            setGroupingSize(newValue);
            break;

        case UNUM_ROUNDING_MODE:
            setRoundingMode((DecimalFormat::ERoundingMode) newValue);
            break;

        case UNUM_FORMAT_WIDTH:
            setFormatWidth(newValue);
            break;

        case UNUM_PADDING_POSITION:
            /** The position at which padding will take place. */
            setPadPosition((DecimalFormat::EPadPosition) newValue);
            break;

        case UNUM_SECONDARY_GROUPING_SIZE:
            setSecondaryGroupingSize(newValue);
            break;

#if UCONFIG_HAVE_PARSEALLINPUT
        case UNUM_PARSE_ALL_INPUT:
            setParseAllInput((UNumberFormatAttributeValue) newValue);
            break;
#endif

        case UNUM_PARSE_NO_EXPONENT:
            setParseNoExponent((UBool) newValue);
            break;

        case UNUM_PARSE_DECIMAL_MARK_REQUIRED:
            setDecimalPatternMatchRequired((UBool) newValue);
            break;

        case UNUM_CURRENCY_USAGE:
            setCurrencyUsage((UCurrencyUsage) newValue, &status);
            break;

        case UNUM_MINIMUM_GROUPING_DIGITS:
            setMinimumGroupingDigits(newValue);
            break;

        case UNUM_PARSE_CASE_SENSITIVE:
            setParseCaseSensitive(static_cast<UBool>(newValue));
            break;

        case UNUM_SIGN_ALWAYS_SHOWN:
            setSignAlwaysShown(static_cast<UBool>(newValue));
            break;

        default:
            status = U_UNSUPPORTED_ERROR;
            break;
    }
    // TODO: UNUM_SCALE?
    // TODO: UNUM_FORMAT_FAIL_IF_MORE_THAN_MAX_DIGITS?
    return *this;
}

int32_t DecimalFormat::getAttribute(UNumberFormatAttribute attr, UErrorCode& status) const {
    if (U_FAILURE(status)) { return -1; }
    switch (attr) {
        case UNUM_LENIENT_PARSE:
            return isLenient();

        case UNUM_PARSE_INT_ONLY:
            return isParseIntegerOnly();

        case UNUM_GROUPING_USED:
            return isGroupingUsed();

        case UNUM_DECIMAL_ALWAYS_SHOWN:
            return isDecimalSeparatorAlwaysShown();

        case UNUM_MAX_INTEGER_DIGITS:
            return getMaximumIntegerDigits();

        case UNUM_MIN_INTEGER_DIGITS:
            return getMinimumIntegerDigits();

        case UNUM_INTEGER_DIGITS:
            // TBD: what should this return?
            return getMinimumIntegerDigits();

        case UNUM_MAX_FRACTION_DIGITS:
            return getMaximumFractionDigits();

        case UNUM_MIN_FRACTION_DIGITS:
            return getMinimumFractionDigits();

        case UNUM_FRACTION_DIGITS:
            // TBD: what should this return?
            return getMinimumFractionDigits();

        case UNUM_SIGNIFICANT_DIGITS_USED:
            return areSignificantDigitsUsed();

        case UNUM_MAX_SIGNIFICANT_DIGITS:
            return getMaximumSignificantDigits();

        case UNUM_MIN_SIGNIFICANT_DIGITS:
            return getMinimumSignificantDigits();

        case UNUM_MULTIPLIER:
            return getMultiplier();

        case UNUM_GROUPING_SIZE:
            return getGroupingSize();

        case UNUM_ROUNDING_MODE:
            return getRoundingMode();

        case UNUM_FORMAT_WIDTH:
            return getFormatWidth();

        case UNUM_PADDING_POSITION:
            return getPadPosition();

        case UNUM_SECONDARY_GROUPING_SIZE:
            return getSecondaryGroupingSize();

        case UNUM_PARSE_NO_EXPONENT:
            return isParseNoExponent();

        case UNUM_PARSE_DECIMAL_MARK_REQUIRED:
            return isDecimalPatternMatchRequired();

        case UNUM_CURRENCY_USAGE:
            return getCurrencyUsage();

        case UNUM_MINIMUM_GROUPING_DIGITS:
            return getMinimumGroupingDigits();

        case UNUM_PARSE_CASE_SENSITIVE:
            return isParseCaseSensitive();

        case UNUM_SIGN_ALWAYS_SHOWN:
            return isSignAlwaysShown();

        default:
            status = U_UNSUPPORTED_ERROR;
            break;
    }
    // TODO: UNUM_FORMAT_FAIL_IF_MORE_THAN_MAX_DIGITS?
    // TODO: UNUM_SCALE?

    return -1; /* undefined */
}

void DecimalFormat::setGroupingUsed(UBool enabled) {
    NumberFormat::setGroupingUsed(enabled); // to set field for compatibility
    if (enabled) {
        // Set to a reasonable default value
        fProperties->groupingSize = 3;
        fProperties->secondaryGroupingSize = -1;
    } else {
        fProperties->groupingSize = 0;
        fProperties->secondaryGroupingSize = 0;
    }
    refreshFormatterNoError();
}

void DecimalFormat::setParseIntegerOnly(UBool value) {
    NumberFormat::setParseIntegerOnly(value); // to set field for compatibility
    fProperties->parseIntegerOnly = value;
    refreshFormatterNoError();
}

void DecimalFormat::setLenient(UBool enable) {
    NumberFormat::setLenient(enable); // to set field for compatibility
    fProperties->parseMode = enable ? PARSE_MODE_LENIENT : PARSE_MODE_STRICT;
    refreshFormatterNoError();
}

DecimalFormat::DecimalFormat(const UnicodeString& pattern, DecimalFormatSymbols* symbolsToAdopt,
                             UParseError&, UErrorCode& status)
        : DecimalFormat(symbolsToAdopt, status) {
    // TODO: What is parseError for?
    setPropertiesFromPattern(pattern, IGNORE_ROUNDING_IF_CURRENCY, status);
    refreshFormatter(status);
}

DecimalFormat::DecimalFormat(const UnicodeString& pattern, const DecimalFormatSymbols& symbols,
                             UErrorCode& status)
        : DecimalFormat(new DecimalFormatSymbols(symbols), status) {
    setPropertiesFromPattern(pattern, IGNORE_ROUNDING_IF_CURRENCY, status);
    refreshFormatter(status);
}

DecimalFormat::DecimalFormat(const DecimalFormat& source) {
    fProperties.adoptInstead(new DecimalFormatProperties(*source.fProperties));
    fExportedProperties.adoptInstead(new DecimalFormatProperties());
    fWarehouse.adoptInstead(new DecimalFormatWarehouse());
    fSymbols.adoptInstead(new DecimalFormatSymbols(*source.fSymbols));
    if (fProperties == nullptr || fExportedProperties == nullptr || fWarehouse == nullptr ||
        fSymbols == nullptr) {
        return;
    }
    refreshFormatterNoError();
}

DecimalFormat& DecimalFormat::operator=(const DecimalFormat& rhs) {
    *fProperties = *rhs.fProperties;
    fExportedProperties->clear();
    fSymbols.adoptInstead(new DecimalFormatSymbols(*rhs.fSymbols));
    refreshFormatterNoError();
    return *this;
}

DecimalFormat::~DecimalFormat() = default;

Format* DecimalFormat::clone() const {
    return new DecimalFormat(*this);
}

UBool DecimalFormat::operator==(const Format& other) const {
    auto* otherDF = dynamic_cast<const DecimalFormat*>(&other);
    if (otherDF == nullptr) {
        return false;
    }
    return *fProperties == *otherDF->fProperties && *fSymbols == *otherDF->fSymbols;
}

UnicodeString& DecimalFormat::format(double number, UnicodeString& appendTo, FieldPosition& pos) const {
    ErrorCode localStatus;
    FormattedNumber output = fFormatter->formatDouble(number, localStatus);
    output.populateFieldPosition(pos, localStatus);
    auto appendable = UnicodeStringAppendable(appendTo);
    output.appendTo(appendable);
    return appendTo;
}

UnicodeString& DecimalFormat::format(double number, UnicodeString& appendTo, FieldPosition& pos,
                                     UErrorCode& status) const {
    FormattedNumber output = fFormatter->formatDouble(number, status);
    output.populateFieldPosition(pos, status);
    auto appendable = UnicodeStringAppendable(appendTo);
    output.appendTo(appendable);
    return appendTo;
}

UnicodeString&
DecimalFormat::format(double number, UnicodeString& appendTo, FieldPositionIterator* posIter,
                      UErrorCode& status) const {
    FormattedNumber output = fFormatter->formatDouble(number, status);
    output.populateFieldPositionIterator(*posIter, status);
    auto appendable = UnicodeStringAppendable(appendTo);
    output.appendTo(appendable);
    return appendTo;
}

UnicodeString& DecimalFormat::format(int32_t number, UnicodeString& appendTo, FieldPosition& pos) const {
    return format(static_cast<int64_t> (number), appendTo, pos);
}

UnicodeString& DecimalFormat::format(int32_t number, UnicodeString& appendTo, FieldPosition& pos,
                                     UErrorCode& status) const {
    return format(static_cast<int64_t> (number), appendTo, pos, status);
}

UnicodeString&
DecimalFormat::format(int32_t number, UnicodeString& appendTo, FieldPositionIterator* posIter,
                      UErrorCode& status) const {
    return format(static_cast<int64_t> (number), appendTo, posIter, status);
}

UnicodeString& DecimalFormat::format(int64_t number, UnicodeString& appendTo, FieldPosition& pos) const {
    ErrorCode localStatus;
    FormattedNumber output = fFormatter->formatInt(number, localStatus);
    output.populateFieldPosition(pos, localStatus);
    auto appendable = UnicodeStringAppendable(appendTo);
    output.appendTo(appendable);
    return appendTo;
}

UnicodeString& DecimalFormat::format(int64_t number, UnicodeString& appendTo, FieldPosition& pos,
                                     UErrorCode& status) const {
    FormattedNumber output = fFormatter->formatInt(number, status);
    output.populateFieldPosition(pos, status);
    auto appendable = UnicodeStringAppendable(appendTo);
    output.appendTo(appendable);
    return appendTo;
}

UnicodeString&
DecimalFormat::format(int64_t number, UnicodeString& appendTo, FieldPositionIterator* posIter,
                      UErrorCode& status) const {
    FormattedNumber output = fFormatter->formatInt(number, status);
    output.populateFieldPositionIterator(*posIter, status);
    auto appendable = UnicodeStringAppendable(appendTo);
    output.appendTo(appendable);
    return appendTo;
}

UnicodeString&
DecimalFormat::format(StringPiece number, UnicodeString& appendTo, FieldPositionIterator* posIter,
                      UErrorCode& status) const {
    ErrorCode localStatus;
    FormattedNumber output = fFormatter->formatDecimal(number, localStatus);
    output.populateFieldPositionIterator(*posIter, status);
    auto appendable = UnicodeStringAppendable(appendTo);
    output.appendTo(appendable);
    return appendTo;
}

UnicodeString& DecimalFormat::format(const DecimalQuantity& number, UnicodeString& appendTo,
                                     FieldPositionIterator* posIter, UErrorCode& status) const {
    FormattedNumber output = fFormatter->formatDecimalQuantity(number, status);
    output.populateFieldPositionIterator(*posIter, status);
    auto appendable = UnicodeStringAppendable(appendTo);
    output.appendTo(appendable);
    return appendTo;
}

UnicodeString&
DecimalFormat::format(const DecimalQuantity& number, UnicodeString& appendTo, FieldPosition& pos,
                      UErrorCode& status) const {
    FormattedNumber output = fFormatter->formatDecimalQuantity(number, status);
    output.populateFieldPosition(pos, status);
    auto appendable = UnicodeStringAppendable(appendTo);
    output.appendTo(appendable);
    return appendTo;
}

void DecimalFormat::parse(const UnicodeString& text, Formattable& output,
                          ParsePosition& parsePosition) const {
    if (parsePosition.getIndex() < 0 || parsePosition.getIndex() >= text.length()) {
        return;
    }

    ErrorCode status;
    ParsedNumber result;
    // Note: if this is a currency instance, currencies will be matched despite the fact that we are not in the
    // parseCurrency method (backwards compatibility)
    int32_t startIndex = parsePosition.getIndex();
    fParser->parse(text, startIndex, true, result, status);
    if (result.success()) {
        parsePosition.setIndex(result.charEnd);
        result.populateFormattable(output);
    } else {
        parsePosition.setErrorIndex(startIndex + result.charEnd);
    }
}

CurrencyAmount* DecimalFormat::parseCurrency(const UnicodeString& text, ParsePosition& parsePosition) const {
    if (parsePosition.getIndex() < 0 || parsePosition.getIndex() >= text.length()) {
        return nullptr;
    }

    ErrorCode status;
    ParsedNumber result;
    // Note: if this is a currency instance, currencies will be matched despite the fact that we are not in the
    // parseCurrency method (backwards compatibility)
    int32_t startIndex = parsePosition.getIndex();
    fParserWithCurrency->parse(text, startIndex, true, result, status);
    if (result.success()) {
        parsePosition.setIndex(result.charEnd);
        Formattable formattable;
        result.populateFormattable(formattable);
        return new CurrencyAmount(formattable, result.currencyCode, status);
    } else {
        parsePosition.setErrorIndex(startIndex + result.charEnd);
        return nullptr;
    }
}

const DecimalFormatSymbols* DecimalFormat::getDecimalFormatSymbols(void) const {
    return fSymbols.getAlias();
}

void DecimalFormat::adoptDecimalFormatSymbols(DecimalFormatSymbols* symbolsToAdopt) {
    if (symbolsToAdopt == nullptr) {
        return; // do not allow caller to set fSymbols to NULL
    }
    fSymbols.adoptInstead(symbolsToAdopt);
    refreshFormatterNoError();
}

void DecimalFormat::setDecimalFormatSymbols(const DecimalFormatSymbols& symbols) {
    fSymbols.adoptInstead(new DecimalFormatSymbols(symbols));
    refreshFormatterNoError();
}

const CurrencyPluralInfo* DecimalFormat::getCurrencyPluralInfo(void) const {
    return fProperties->currencyPluralInfo.fPtr.getAlias();
}

void DecimalFormat::adoptCurrencyPluralInfo(CurrencyPluralInfo* toAdopt) {
    fProperties->currencyPluralInfo.fPtr.adoptInstead(toAdopt);
    refreshFormatterNoError();
}

void DecimalFormat::setCurrencyPluralInfo(const CurrencyPluralInfo& info) {
    *fProperties->currencyPluralInfo.fPtr = info; // copy-assignment operator
    refreshFormatterNoError();
}

UnicodeString& DecimalFormat::getPositivePrefix(UnicodeString& result) const {
    ErrorCode localStatus;
    result = fFormatter->formatInt(1, localStatus).getPrefix(localStatus);
    return result;
}

void DecimalFormat::setPositivePrefix(const UnicodeString& newValue) {
    fProperties->positivePrefix = newValue;
    refreshFormatterNoError();
}

UnicodeString& DecimalFormat::getNegativePrefix(UnicodeString& result) const {
    ErrorCode localStatus;
    result = fFormatter->formatInt(-1, localStatus).getPrefix(localStatus);
    return result;
}

void DecimalFormat::setNegativePrefix(const UnicodeString& newValue) {
    fProperties->negativePrefix = newValue;
    refreshFormatterNoError();
}

UnicodeString& DecimalFormat::getPositiveSuffix(UnicodeString& result) const {
    ErrorCode localStatus;
    result = fFormatter->formatInt(1, localStatus).getSuffix(localStatus);
    return result;
}

void DecimalFormat::setPositiveSuffix(const UnicodeString& newValue) {
    fProperties->positiveSuffix = newValue;
    refreshFormatterNoError();
}

UnicodeString& DecimalFormat::getNegativeSuffix(UnicodeString& result) const {
    ErrorCode localStatus;
    result = fFormatter->formatInt(-1, localStatus).getSuffix(localStatus);
    return result;
}

void DecimalFormat::setNegativeSuffix(const UnicodeString& newValue) {
    fProperties->negativeSuffix = newValue;
    refreshFormatterNoError();
}

UBool DecimalFormat::isSignAlwaysShown() const {
    return fProperties->signAlwaysShown;
}

void DecimalFormat::setSignAlwaysShown(UBool value) {
    fProperties->signAlwaysShown = value;
    refreshFormatterNoError();
}

int32_t DecimalFormat::getMultiplier(void) const {
    if (fProperties->multiplier != 1) {
        return fProperties->multiplier;
    } else if (fProperties->magnitudeMultiplier != 0) {
        return static_cast<int32_t>(uprv_pow10(fProperties->magnitudeMultiplier));
    } else {
        return 1;
    }
}

void DecimalFormat::setMultiplier(int32_t multiplier) {
    if (multiplier == 0) {
        multiplier = 1;     // one being the benign default value for a multiplier.
    }

    // Try to convert to a magnitude multiplier first
    int delta = 0;
    int value = multiplier;
    while (value != 1) {
        delta++;
        int temp = value / 10;
        if (temp * 10 != value) {
            delta = -1;
            break;
        }
        value = temp;
    }
    if (delta != -1) {
        fProperties->magnitudeMultiplier = delta;
        fProperties->multiplier = 1;
    } else {
        fProperties->magnitudeMultiplier = 0;
        fProperties->multiplier = multiplier;
    }
    refreshFormatterNoError();
}

double DecimalFormat::getRoundingIncrement(void) const {
    return fExportedProperties->roundingIncrement;
}

void DecimalFormat::setRoundingIncrement(double newValue) {
    fProperties->roundingIncrement = newValue;
    refreshFormatterNoError();
}

ERoundingMode DecimalFormat::getRoundingMode(void) const {
    // UNumberFormatRoundingMode and ERoundingMode have the same values.
    return static_cast<ERoundingMode>(fExportedProperties->roundingMode.getNoError());
}

void DecimalFormat::setRoundingMode(ERoundingMode roundingMode) {
    NumberFormat::setMaximumIntegerDigits(roundingMode); // to set field for compatibility
    fProperties->roundingMode = static_cast<UNumberFormatRoundingMode>(roundingMode);
    refreshFormatterNoError();
}

int32_t DecimalFormat::getFormatWidth(void) const {
    return fProperties->formatWidth;
}

void DecimalFormat::setFormatWidth(int32_t width) {
    fProperties->formatWidth = width;
    refreshFormatterNoError();
}

UnicodeString DecimalFormat::getPadCharacterString() const {
    return fProperties->padString;
}

void DecimalFormat::setPadCharacter(const UnicodeString& padChar) {
    if (padChar.length() > 0) {
        fProperties->padString = UnicodeString(padChar.char32At(0));
    } else {
        fProperties->padString.setToBogus();
    }
    refreshFormatterNoError();
}

EPadPosition DecimalFormat::getPadPosition(void) const {
    if (fProperties->padPosition.isNull()) {
        return EPadPosition::kPadBeforePrefix;
    } else {
        // UNumberFormatPadPosition and EPadPosition have the same values.
        return static_cast<EPadPosition>(fProperties->padPosition.getNoError());
    }
}

void DecimalFormat::setPadPosition(EPadPosition padPos) {
    fProperties->padPosition = static_cast<UNumberFormatPadPosition>(padPos);
    refreshFormatterNoError();
}

UBool DecimalFormat::isScientificNotation(void) const {
    return fProperties->minimumExponentDigits != -1;
}

void DecimalFormat::setScientificNotation(UBool useScientific) {
    if (useScientific) {
        fProperties->minimumExponentDigits = 1;
    } else {
        fProperties->minimumExponentDigits = -1;
    }
    refreshFormatterNoError();
}

int8_t DecimalFormat::getMinimumExponentDigits(void) const {
    return static_cast<int8_t>(fProperties->minimumExponentDigits);
}

void DecimalFormat::setMinimumExponentDigits(int8_t minExpDig) {
    fProperties->minimumExponentDigits = minExpDig;
    refreshFormatterNoError();
}

UBool DecimalFormat::isExponentSignAlwaysShown(void) const {
    return fProperties->exponentSignAlwaysShown;
}

void DecimalFormat::setExponentSignAlwaysShown(UBool expSignAlways) {
    fProperties->exponentSignAlwaysShown = expSignAlways;
    refreshFormatterNoError();
}

int32_t DecimalFormat::getGroupingSize(void) const {
    return fProperties->groupingSize;
}

void DecimalFormat::setGroupingSize(int32_t newValue) {
    fProperties->groupingSize = newValue;
    refreshFormatterNoError();
}

int32_t DecimalFormat::getSecondaryGroupingSize(void) const {
    int grouping1 = fProperties->groupingSize;
    int grouping2 = fProperties->secondaryGroupingSize;
    if (grouping1 == grouping2 || grouping2 < 0) {
        return 0;
    }
    return grouping2;
}

void DecimalFormat::setSecondaryGroupingSize(int32_t newValue) {
    fProperties->secondaryGroupingSize = newValue;
    refreshFormatterNoError();
}

int32_t DecimalFormat::getMinimumGroupingDigits() const {
    return fProperties->minimumGroupingDigits;
}

void DecimalFormat::setMinimumGroupingDigits(int32_t newValue) {
    fProperties->minimumGroupingDigits = newValue;
    refreshFormatterNoError();
}

UBool DecimalFormat::isDecimalSeparatorAlwaysShown(void) const {
    return fProperties->decimalSeparatorAlwaysShown;
}

void DecimalFormat::setDecimalSeparatorAlwaysShown(UBool newValue) {
    fProperties->decimalSeparatorAlwaysShown = newValue;
    refreshFormatterNoError();
}

UBool DecimalFormat::isDecimalPatternMatchRequired(void) const {
    return fProperties->decimalPatternMatchRequired;
}

void DecimalFormat::setDecimalPatternMatchRequired(UBool newValue) {
    fProperties->decimalPatternMatchRequired = newValue;
    refreshFormatterNoError();
}

UBool DecimalFormat::isParseNoExponent() const {
    return fProperties->parseNoExponent;
}

void DecimalFormat::setParseNoExponent(UBool value) {
    fProperties->parseNoExponent = value;
    refreshFormatterNoError();
}

UBool DecimalFormat::isParseCaseSensitive() const {
    return fProperties->parseCaseSensitive;
}

void DecimalFormat::setParseCaseSensitive(UBool value) {
    fProperties->parseCaseSensitive = value;
    refreshFormatterNoError();
}

UnicodeString& DecimalFormat::toPattern(UnicodeString& result) const {
    // Pull some properties from exportedProperties and others from properties
    // to keep affix patterns intact.  In particular, pull rounding properties
    // so that CurrencyUsage is reflected properly.
    // TODO: Consider putting this logic in number_patternstring.cpp instead.
    ErrorCode localStatus;
    DecimalFormatProperties tprops(*fProperties);
    bool useCurrency = ((!tprops.currency.isNull()) || !tprops.currencyPluralInfo.fPtr.isNull() ||
                        !tprops.currencyUsage.isNull() || AffixUtils::hasCurrencySymbols(
            UnicodeStringCharSequence(tprops.positivePrefixPattern), localStatus) ||
                        AffixUtils::hasCurrencySymbols(
                                UnicodeStringCharSequence(tprops.positiveSuffixPattern), localStatus) ||
                        AffixUtils::hasCurrencySymbols(
                                UnicodeStringCharSequence(tprops.negativePrefixPattern), localStatus) ||
                        AffixUtils::hasCurrencySymbols(
                                UnicodeStringCharSequence(tprops.negativeSuffixPattern), localStatus));
    if (useCurrency) {
        tprops.minimumFractionDigits = fExportedProperties->minimumFractionDigits;
        tprops.maximumFractionDigits = fExportedProperties->maximumFractionDigits;
        tprops.roundingIncrement = fExportedProperties->roundingIncrement;
    }
    result = PatternStringUtils::propertiesToPatternString(tprops, localStatus);
    return result;
}

UnicodeString& DecimalFormat::toLocalizedPattern(UnicodeString& result) const {
    ErrorCode localStatus;
    result = toPattern(result);
    result = PatternStringUtils::convertLocalized(result, *fSymbols, true, localStatus);
    return result;
}

void DecimalFormat::applyPattern(const UnicodeString& pattern, UParseError&, UErrorCode& status) {
    // TODO: What is parseError for?
    applyPattern(pattern, status);
}

void DecimalFormat::applyPattern(const UnicodeString& pattern, UErrorCode& status) {
    setPropertiesFromPattern(pattern, IGNORE_ROUNDING_NEVER, status);
    refreshFormatter(status);
}

void DecimalFormat::applyLocalizedPattern(const UnicodeString& localizedPattern, UParseError&,
                                          UErrorCode& status) {
    // TODO: What is parseError for?
    applyLocalizedPattern(localizedPattern, status);
}

void DecimalFormat::applyLocalizedPattern(const UnicodeString& localizedPattern, UErrorCode& status) {
    UnicodeString pattern = PatternStringUtils::convertLocalized(
            localizedPattern, *fSymbols, false, status);
    applyPattern(pattern, status);
}

void DecimalFormat::setMaximumIntegerDigits(int32_t newValue) {
    fProperties->maximumIntegerDigits = newValue;
    refreshFormatterNoError();
}

void DecimalFormat::setMinimumIntegerDigits(int32_t newValue) {
    fProperties->minimumIntegerDigits = newValue;
    refreshFormatterNoError();
}

void DecimalFormat::setMaximumFractionDigits(int32_t newValue) {
    fProperties->maximumFractionDigits = newValue;
    refreshFormatterNoError();
}

void DecimalFormat::setMinimumFractionDigits(int32_t newValue) {
    fProperties->minimumFractionDigits = newValue;
    refreshFormatterNoError();
}

int32_t DecimalFormat::getMinimumSignificantDigits() const {
    return fExportedProperties->minimumSignificantDigits;
}

int32_t DecimalFormat::getMaximumSignificantDigits() const {
    return fExportedProperties->maximumSignificantDigits;
}

void DecimalFormat::setMinimumSignificantDigits(int32_t value) {
    int32_t max = fProperties->maximumSignificantDigits;
    if (max >= 0 && max < value) {
        fProperties->maximumSignificantDigits = value;
    }
    fProperties->minimumSignificantDigits = value;
    refreshFormatterNoError();
}

void DecimalFormat::setMaximumSignificantDigits(int32_t value) {
    int32_t min = fProperties->minimumSignificantDigits;
    if (min >= 0 && min > value) {
        fProperties->minimumSignificantDigits = value;
    }
    fProperties->maximumSignificantDigits = value;
    refreshFormatterNoError();
}

UBool DecimalFormat::areSignificantDigitsUsed() const {
    return fProperties->minimumSignificantDigits != -1 || fProperties->maximumSignificantDigits != -1;
}

void DecimalFormat::setSignificantDigitsUsed(UBool useSignificantDigits) {
    if (useSignificantDigits) {
        // These are the default values from the old implementation.
        fProperties->minimumSignificantDigits = 1;
        fProperties->maximumSignificantDigits = 6;
    } else {
        fProperties->minimumSignificantDigits = -1;
        fProperties->maximumSignificantDigits = -1;
    }
    refreshFormatterNoError();
}

void DecimalFormat::setCurrency(const char16_t* theCurrency, UErrorCode& ec) {
    fProperties->currency = CurrencyUnit(theCurrency, ec);
    // TODO: Set values in fSymbols, too?
    refreshFormatterNoError();
}

void DecimalFormat::setCurrency(const char16_t* theCurrency) {
    ErrorCode localStatus;
    setCurrency(theCurrency, localStatus);
}

void DecimalFormat::setCurrencyUsage(UCurrencyUsage newUsage, UErrorCode* ec) {
    fProperties->currencyUsage = newUsage;
    refreshFormatter(*ec);
}

UCurrencyUsage DecimalFormat::getCurrencyUsage() const {
    // CurrencyUsage is not exported, so we have to get it from the input property bag.
    // TODO: Should we export CurrencyUsage instead?
    if (fProperties->currencyUsage.isNull()) {
        return UCURR_USAGE_STANDARD;
    }
    return fProperties->currencyUsage.getNoError();
}

void
DecimalFormat::formatToDecimalQuantity(double number, DecimalQuantity& output, UErrorCode& status) const {
    fFormatter->formatDouble(number, status).getDecimalQuantity(output, status);
}

void DecimalFormat::formatToDecimalQuantity(const Formattable& number, DecimalQuantity& output,
                                            UErrorCode& status) const {
    // Check if the Formattable is a DecimalQuantity
    DecimalQuantity* dq = number.getDecimalQuantity();
    if (dq != nullptr) {
        fFormatter->formatDecimalQuantity(*dq, status).getDecimalQuantity(output, status);
        return;
    }

    // If not, it must be Double, Long (int32_t), or Int64:
    switch (number.getType()) {
        case Formattable::kDouble:
            fFormatter->formatDouble(number.getDouble(), status).getDecimalQuantity(output, status);
            break;
        case Formattable::kLong:
            fFormatter->formatInt(number.getLong(), status).getDecimalQuantity(output, status);
            break;
        case Formattable::kInt64:
        default:
            fFormatter->formatInt(number.getInt64(), status).getDecimalQuantity(output, status);
    }
}

const number::LocalizedNumberFormatter& DecimalFormat::toNumberFormatter() const {
    return *fFormatter;
}

/** Rebuilds the formatter object from the property bag. */
void DecimalFormat::refreshFormatter(UErrorCode& status) {
    if (fExportedProperties == nullptr) {
        // fExportedProperties is null only when the formatter is not ready yet.
        // The only time when this happens is during legacy deserialization.
        return;
    }

    // In C++, fSymbols is the source of truth for the locale.
    Locale locale = fSymbols->getLocale();

    fFormatter.adoptInsteadAndCheckErrorCode(
            new LocalizedNumberFormatter(
                    NumberPropertyMapper::create(
                            *fProperties, *fSymbols, *fWarehouse, *fExportedProperties, status).locale(
                            locale)), status);

    fParser.adoptInsteadAndCheckErrorCode(
            NumberParserImpl::createParserFromProperties(
                    *fProperties, *fSymbols, false, status), status);

    fParserWithCurrency.adoptInsteadAndCheckErrorCode(
            NumberParserImpl::createParserFromProperties(
                    *fProperties, *fSymbols, true, status), status);

    // In order for the getters to work, we need to populate some fields in NumberFormat.
    NumberFormat::setMaximumIntegerDigits(fExportedProperties->maximumIntegerDigits);
    NumberFormat::setMinimumIntegerDigits(fExportedProperties->minimumIntegerDigits);
    NumberFormat::setMaximumFractionDigits(fExportedProperties->maximumFractionDigits);
    NumberFormat::setMinimumFractionDigits(fExportedProperties->minimumFractionDigits);
}

void DecimalFormat::refreshFormatterNoError() {
    ErrorCode localStatus;
    refreshFormatter(localStatus);
}

void DecimalFormat::setPropertiesFromPattern(const UnicodeString& pattern, int32_t ignoreRounding,
                                             UErrorCode& status) {
    // Cast workaround to get around putting the enum in the public header file
    auto actualIgnoreRounding = static_cast<IgnoreRounding>(ignoreRounding);
    PatternParser::parseToExistingProperties(pattern, *fProperties, actualIgnoreRounding, status);
}


#endif /* #if !UCONFIG_NO_FORMATTING */
