// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

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

using namespace icu;
using namespace icu::number;
using namespace icu::number::impl;
using namespace icu::numparse;
using namespace icu::numparse::impl;
using ERoundingMode = icu::DecimalFormat::ERoundingMode;
using EPadPosition = icu::DecimalFormat::EPadPosition;


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
    refreshFormatter(status);
}

DecimalFormat::DecimalFormat(const DecimalFormatSymbols* symbolsToAdopt, UErrorCode& status) {
    properties = new DecimalFormatProperties();
    exportedProperties = new DecimalFormatProperties();
    if (symbolsToAdopt == nullptr) {
        symbols = new DecimalFormatSymbols(status);
    } else {
        symbols = symbolsToAdopt;
    }
    if (properties == nullptr || exportedProperties == nullptr || symbols == nullptr) {
        status = U_MEMORY_ALLOCATION_ERROR;
    }
}

#if UCONFIG_HAVE_PARSEALLINPUT

void DecimalFormat::setParseAllInput(UNumberFormatAttributeValue value) {
    properties->parseAllInput = value;
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
            return getParseNoExponent();

        case UNUM_PARSE_DECIMAL_MARK_REQUIRED:
            return isDecimalPatternMatchRequired();

        case UNUM_CURRENCY_USAGE:
            return getCurrencyUsage();

        case UNUM_MINIMUM_GROUPING_DIGITS:
            return getMinimumGroupingDigits();

        default:
            status = U_UNSUPPORTED_ERROR;
            break;
    }
    // TODO: UNUM_FORMAT_FAIL_IF_MORE_THAN_MAX_DIGITS?
    // TODO: UNUM_SCALE?

    return -1; /* undefined */
}

void DecimalFormat::setGroupingUsed(UBool enabled) {
    if (enabled) {
        // Set to a reasonable default value
        properties->groupingSize = 3;
        properties->secondaryGroupingSize = 3;
    } else {
        properties->groupingSize = 0;
        properties->secondaryGroupingSize = 0;
    }
    refreshFormatterNoError();
}

void DecimalFormat::setParseIntegerOnly(UBool value) {
    properties->parseIntegerOnly = value;
    refreshFormatterNoError();
}

DecimalFormat::DecimalFormat(const UnicodeString& pattern, DecimalFormatSymbols* symbolsToAdopt,
                             UParseError& parseError, UErrorCode& status)
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
    properties = new DecimalFormatProperties();
    exportedProperties = new DecimalFormatProperties();
    symbols = new DecimalFormatSymbols(*source.symbols);
    if (properties == nullptr || exportedProperties == nullptr || symbols == nullptr) {
        return;
    }
    refreshFormatterNoError();
}

DecimalFormat& DecimalFormat::operator=(const DecimalFormat& rhs) {
    *properties = *rhs.properties;
    exportedProperties->clear();
    symbols = new DecimalFormatSymbols(*rhs.symbols);
    refreshFormatterNoError();
}

DecimalFormat::~DecimalFormat() = default;

Format* DecimalFormat::clone() const {
    return new DecimalFormat(*this);
}

UBool DecimalFormat::operator==(const Format& other) const {
    const DecimalFormat* otherDF = dynamic_cast<const DecimalFormat*>(&other);
    if (otherDF == nullptr) {
        return false;
    }
    return *properties == *otherDF->properties && *symbols == *otherDF->symbols;
}

UnicodeString& DecimalFormat::format(double number, UnicodeString& appendTo, FieldPosition& pos) const {
    ErrorCode localStatus;
    FormattedNumber output = formatter->formatDouble(number, localStatus);
    output.populateFieldPosition(pos, localStatus);
    auto appendable = UnicodeStringAppendable(appendTo);
    output.appendTo(appendable);
    return appendTo;
}

UnicodeString& DecimalFormat::format(double number, UnicodeString& appendTo, FieldPosition& pos,
                                     UErrorCode& status) const {
    FormattedNumber output = formatter->formatDouble(number, status);
    output.populateFieldPosition(pos, status);
    auto appendable = UnicodeStringAppendable(appendTo);
    output.appendTo(appendable);
    return appendTo;
}

UnicodeString&
DecimalFormat::format(double number, UnicodeString& appendTo, FieldPositionIterator* posIter,
                      UErrorCode& status) const {
    FormattedNumber output = formatter->formatDouble(number, status);
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
    FormattedNumber output = formatter->formatInt(number, localStatus);
    output.populateFieldPosition(pos, localStatus);
    auto appendable = UnicodeStringAppendable(appendTo);
    output.appendTo(appendable);
    return appendTo;
}

UnicodeString& DecimalFormat::format(int64_t number, UnicodeString& appendTo, FieldPosition& pos,
                                     UErrorCode& status) const {
    FormattedNumber output = formatter->formatInt(number, status);
    output.populateFieldPosition(pos, status);
    auto appendable = UnicodeStringAppendable(appendTo);
    output.appendTo(appendable);
    return appendTo;
}

UnicodeString&
DecimalFormat::format(int64_t number, UnicodeString& appendTo, FieldPositionIterator* posIter,
                      UErrorCode& status) const {
    FormattedNumber output = formatter->formatInt(number, status);
    output.populateFieldPositionIterator(*posIter, status);
    auto appendable = UnicodeStringAppendable(appendTo);
    output.appendTo(appendable);
    return appendTo;
}

UnicodeString&
DecimalFormat::format(StringPiece number, UnicodeString& appendTo, FieldPositionIterator* posIter,
                      UErrorCode& status) const {
    ErrorCode localStatus;
    FormattedNumber output = formatter->formatDecimal(number, localStatus);
    output.populateFieldPositionIterator(*posIter, status);
    auto appendable = UnicodeStringAppendable(appendTo);
    output.appendTo(appendable);
    return appendTo;
}

UnicodeString& DecimalFormat::format(const DecimalQuantity& number, UnicodeString& appendTo,
                                     FieldPositionIterator* posIter, UErrorCode& status) const {
    FormattedNumber output = formatter->formatDecimalQuantity(number, status);
    output.populateFieldPositionIterator(*posIter, status);
    auto appendable = UnicodeStringAppendable(appendTo);
    output.appendTo(appendable);
    return appendTo;
}

UnicodeString&
DecimalFormat::format(const DecimalQuantity& number, UnicodeString& appendTo, FieldPosition& pos,
                      UErrorCode& status) const {
    FormattedNumber output = formatter->formatDecimalQuantity(number, status);
    output.populateFieldPosition(pos, status);
    auto appendable = UnicodeStringAppendable(appendTo);
    output.appendTo(appendable);
    return appendTo;
}

void
DecimalFormat::parse(const UnicodeString& text, Formattable& result, ParsePosition& parsePosition) const {
    // FIXME
}

CurrencyAmount* DecimalFormat::parseCurrency(const UnicodeString& text, ParsePosition& pos) const {
    // FIXME
}

const DecimalFormatSymbols* DecimalFormat::getDecimalFormatSymbols(void) const {}

void DecimalFormat::adoptDecimalFormatSymbols(DecimalFormatSymbols* symbolsToAdopt) {}

void DecimalFormat::setDecimalFormatSymbols(const DecimalFormatSymbols& symbols) {}

const CurrencyPluralInfo* DecimalFormat::getCurrencyPluralInfo(void) const {}

void DecimalFormat::adoptCurrencyPluralInfo(CurrencyPluralInfo* toAdopt) {}

void DecimalFormat::setCurrencyPluralInfo(const CurrencyPluralInfo& info) {}

UnicodeString& DecimalFormat::getPositivePrefix(UnicodeString& result) const {}

void DecimalFormat::setPositivePrefix(const UnicodeString& newValue) {}

UnicodeString& DecimalFormat::getNegativePrefix(UnicodeString& result) const {}

void DecimalFormat::setNegativePrefix(const UnicodeString& newValue) {}

UnicodeString& DecimalFormat::getPositiveSuffix(UnicodeString& result) const {}

void DecimalFormat::setPositiveSuffix(const UnicodeString& newValue) {}

UnicodeString& DecimalFormat::getNegativeSuffix(UnicodeString& result) const {}

void DecimalFormat::setNegativeSuffix(const UnicodeString& newValue) {}

int32_t DecimalFormat::getMultiplier(void) const {}

void DecimalFormat::setMultiplier(int32_t newValue) {}

double DecimalFormat::getRoundingIncrement(void) const {}

void DecimalFormat::setRoundingIncrement(double newValue) {}

ERoundingMode DecimalFormat::getRoundingMode(void) const {}

void DecimalFormat::setRoundingMode(ERoundingMode roundingMode) {}

int32_t DecimalFormat::getFormatWidth(void) const {}

void DecimalFormat::setFormatWidth(int32_t width) {}

UnicodeString DecimalFormat::getPadCharacterString() const {}

void DecimalFormat::setPadCharacter(const UnicodeString& padChar) {}

EPadPosition DecimalFormat::getPadPosition(void) const {}

void DecimalFormat::setPadPosition(EPadPosition padPos) {}

UBool DecimalFormat::isScientificNotation(void) const {}

void DecimalFormat::setScientificNotation(UBool useScientific) {}

int8_t DecimalFormat::getMinimumExponentDigits(void) const {}

void DecimalFormat::setMinimumExponentDigits(int8_t minExpDig) {}

UBool DecimalFormat::isExponentSignAlwaysShown(void) const {}

void DecimalFormat::setExponentSignAlwaysShown(UBool expSignAlways) {}

int32_t DecimalFormat::getGroupingSize(void) const {}

void DecimalFormat::setGroupingSize(int32_t newValue) {}

int32_t DecimalFormat::getSecondaryGroupingSize(void) const {}

void DecimalFormat::setSecondaryGroupingSize(int32_t newValue) {}

int32_t DecimalFormat::getMinimumGroupingDigits() const {}

void DecimalFormat::setMinimumGroupingDigits(int32_t newValue) {}

UBool DecimalFormat::isDecimalSeparatorAlwaysShown(void) const {}

void DecimalFormat::setDecimalSeparatorAlwaysShown(UBool newValue) {}

UBool DecimalFormat::isDecimalPatternMatchRequired(void) const {}

void DecimalFormat::setDecimalPatternMatchRequired(UBool newValue) {}

UnicodeString& DecimalFormat::toPattern(UnicodeString& result) const {}

UnicodeString& DecimalFormat::toLocalizedPattern(UnicodeString& result) const {}

void
DecimalFormat::applyPattern(const UnicodeString& pattern, UParseError& parseError, UErrorCode& status) {}

void DecimalFormat::applyPattern(const UnicodeString& pattern, UErrorCode& status) {}

void DecimalFormat::applyLocalizedPattern(const UnicodeString& pattern, UParseError& parseError,
                                          UErrorCode& status) {}

void DecimalFormat::applyLocalizedPattern(const UnicodeString& pattern, UErrorCode& status) {}

void DecimalFormat::setMaximumIntegerDigits(int32_t newValue) {}

void DecimalFormat::setMinimumIntegerDigits(int32_t newValue) {}

void DecimalFormat::setMaximumFractionDigits(int32_t newValue) {}

void DecimalFormat::setMinimumFractionDigits(int32_t newValue) {}

int32_t DecimalFormat::getMinimumSignificantDigits() const {}

int32_t DecimalFormat::getMaximumSignificantDigits() const {}

void DecimalFormat::setMinimumSignificantDigits(int32_t min) {}

void DecimalFormat::setMaximumSignificantDigits(int32_t max) {}

UBool DecimalFormat::areSignificantDigitsUsed() const {}

void DecimalFormat::setSignificantDigitsUsed(UBool useSignificantDigits) {}

void DecimalFormat::setCurrency(const char16_t* theCurrency, UErrorCode& ec) {}

void DecimalFormat::setCurrency(const char16_t* theCurrency) {}

void DecimalFormat::setCurrencyUsage(UCurrencyUsage newUsage, UErrorCode* ec) {}

UCurrencyUsage DecimalFormat::getCurrencyUsage() const {}

void
DecimalFormat::formatToDecimalQuantity(double number, DecimalQuantity& output, UErrorCode& status) const {}

void DecimalFormat::formatToDecimalQuantity(const Formattable& number, DecimalQuantity& output,
                                            UErrorCode& status) const {}

number::LocalizedNumberFormatter DecimalFormat::toNumberFormatter() const {}

UClassID DecimalFormat::getStaticClassID() {}

UClassID DecimalFormat::getDynamicClassID() const {}

/** Rebuilds the formatter object from the property bag. */
void DecimalFormat::refreshFormatter(UErrorCode& status) {
    if (exportedProperties == nullptr) {
        // exportedProperties is null only when the formatter is not ready yet.
        // The only time when this happens is during legacy deserialization.
        return;
    }
    Locale locale = getLocale(ULOC_ACTUAL_LOCALE, status);
    if (U_FAILURE(status)) {
        // Constructor
        locale = symbols->getLocale(ULOC_ACTUAL_LOCALE, status);
    }
    if (U_FAILURE(status)) {
        // Deserialization
        locale = symbols->getLocale();
    }
    if (U_FAILURE(status)) {
        return;
    }

    *formatter = NumberPropertyMapper::create(*properties, *symbols, *exportedProperties, status).locale(
            locale);
    parser = NumberParserImpl::createParserFromProperties(*properties, *symbols, false, false, status);
    parserWithCurrency = NumberParserImpl::createParserFromProperties(
            *properties, *symbols, true, false, status);
}

void DecimalFormat::refreshFormatterNoError() {
    ErrorCode localStatus;
    refreshFormatter(localStatus);
}

void DecimalFormat::setPropertiesFromPattern(const UnicodeString& pattern, int32_t ignoreRounding,
                                             UErrorCode& status) {
    // Cast workaround to get around putting the enum in the public header file
    auto actualIgnoreRounding = static_cast<IgnoreRounding>(ignoreRounding);
    PatternParser::parseToExistingProperties(pattern, *properties, actualIgnoreRounding, status);
}


#endif /* #if !UCONFIG_NO_FORMATTING */
