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

void DecimalFormat::setParseAllInput(UNumberFormatAttributeValue value) {}

DecimalFormat&
DecimalFormat::setAttribute(UNumberFormatAttribute attr, int32_t newvalue, UErrorCode& status) {}

int32_t DecimalFormat::getAttribute(UNumberFormatAttribute attr, UErrorCode& status) const {}

void DecimalFormat::setGroupingUsed(UBool newValue) {}

void DecimalFormat::setParseIntegerOnly(UBool value) {}

void DecimalFormat::setContext(UDisplayContext value, UErrorCode& status) {}

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
    ErrorCode localStatus;
    refreshFormatter(localStatus);
}

DecimalFormat& DecimalFormat::operator=(const DecimalFormat& rhs) {}

DecimalFormat::~DecimalFormat() = default;

Format* DecimalFormat::clone() const {}

UBool DecimalFormat::operator==(const Format& other) const {}

UnicodeString& DecimalFormat::format(double number, UnicodeString& appendTo, FieldPosition& pos) const {}

UnicodeString& DecimalFormat::format(double number, UnicodeString& appendTo, FieldPosition& pos,
                                     UErrorCode& status) const {}

UnicodeString&
DecimalFormat::format(double number, UnicodeString& appendTo, FieldPositionIterator* posIter,
                      UErrorCode& status) const {}

UnicodeString& DecimalFormat::format(int32_t number, UnicodeString& appendTo, FieldPosition& pos) const {}

UnicodeString& DecimalFormat::format(int32_t number, UnicodeString& appendTo, FieldPosition& pos,
                                     UErrorCode& status) const {}

UnicodeString&
DecimalFormat::format(int32_t number, UnicodeString& appendTo, FieldPositionIterator* posIter,
                      UErrorCode& status) const {}

UnicodeString& DecimalFormat::format(int64_t number, UnicodeString& appendTo, FieldPosition& pos) const {}

UnicodeString& DecimalFormat::format(int64_t number, UnicodeString& appendTo, FieldPosition& pos,
                                     UErrorCode& status) const {}

UnicodeString&
DecimalFormat::format(int64_t number, UnicodeString& appendTo, FieldPositionIterator* posIter,
                      UErrorCode& status) const {}

UnicodeString&
DecimalFormat::format(StringPiece number, UnicodeString& appendTo, FieldPositionIterator* posIter,
                      UErrorCode& status) const {}

UnicodeString& DecimalFormat::format(const DecimalQuantity& number, UnicodeString& appendTo,
                                     FieldPositionIterator* posIter, UErrorCode& status) const {}

UnicodeString&
DecimalFormat::format(const DecimalQuantity& number, UnicodeString& appendTo, FieldPosition& pos,
                      UErrorCode& status) const {}

void
DecimalFormat::parse(const UnicodeString& text, Formattable& result, ParsePosition& parsePosition) const {}

CurrencyAmount* DecimalFormat::parseCurrency(const UnicodeString& text, ParsePosition& pos) const {}

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

void DecimalFormat::setPropertiesFromPattern(const UnicodeString& pattern, int32_t ignoreRounding,
                                             UErrorCode& status) {
    // Cast workaround to get around putting the enum in the public header file
    auto actualIgnoreRounding = static_cast<IgnoreRounding>(ignoreRounding);
    PatternParser::parseToExistingProperties(pattern, *properties, actualIgnoreRounding, status);
}


#endif /* #if !UCONFIG_NO_FORMATTING */
