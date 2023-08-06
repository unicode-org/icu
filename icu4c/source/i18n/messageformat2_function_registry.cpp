// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/dtptngen.h"
#include "unicode/messageformat2.h"
#include "unicode/numberformatter.h"
#include "unicode/smpdtfmt.h"
#include "uvector.h" // U_ASSERT

U_NAMESPACE_BEGIN namespace message2 {

// Function registry implementation

Formatter::~Formatter() {}
Selector::~Selector() {}
FormatterFactory::~FormatterFactory() {}
SelectorFactory::~SelectorFactory() {}
StandardFunctions::PluralFactory::~PluralFactory() {}

FunctionRegistry* FunctionRegistry::Builder::build(UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    U_ASSERT(formatters.isValid() && selectors.isValid());
    LocalPointer<FunctionRegistry> result(new FunctionRegistry(formatters.orphan(), selectors.orphan()));
    if (!result.isValid()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return result.orphan();
}

/* static */ FunctionRegistry::Builder* FunctionRegistry::builder(UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    LocalPointer<FunctionRegistry::Builder> result(new FunctionRegistry::Builder(errorCode));
    if (U_FAILURE(errorCode)) {
        return nullptr;
    }
    return result.orphan();
}

FunctionRegistry::Builder::Builder(UErrorCode& errorCode)  {
    CHECK_ERROR(errorCode);

    formatters.adoptInstead(new Hashtable(compareVariableName, nullptr, errorCode));
    selectors.adoptInstead(new Hashtable(compareVariableName, nullptr, errorCode));
    if (U_FAILURE(errorCode)) {
        formatters.adoptInstead(nullptr);
        selectors.adoptInstead(nullptr);
        return;
    }
    // The hashtables own the values, but not the keys
    formatters->setValueDeleter(uprv_deleteUObject);
    selectors->setValueDeleter(uprv_deleteUObject);
}

FunctionRegistry::Builder& FunctionRegistry::Builder::setSelector(const FunctionName& selectorName, SelectorFactory* selectorFactory, UErrorCode& errorCode) {
    THIS_ON_ERROR(errorCode);
    if (selectorFactory == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return *this;
    }

    selectors->put(selectorName.toString(), selectorFactory, errorCode);
    return *this;
}

FunctionRegistry::Builder& FunctionRegistry::Builder::setFormatter(const FunctionName& formatterName, FormatterFactory* formatterFactory, UErrorCode& errorCode) {
    THIS_ON_ERROR(errorCode);
    if (formatterFactory == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return *this;
    }

    formatters->put(formatterName.toString(), formatterFactory, errorCode);
    return *this;
}

const FormatterFactory* FunctionRegistry::getFormatter(const FunctionName& formatterName) const {
    // Caller must check for null
    return ((FormatterFactory*) formatters->get(formatterName.toString()));
}

const SelectorFactory* FunctionRegistry::getSelector(const FunctionName& selectorName) const {
    // Caller must check for null
    return ((SelectorFactory*) selectors->get(selectorName.toString()));
}

void FunctionRegistry::checkFormatter(const char* s) const {
    U_ASSERT(hasFormatter(FunctionName(UnicodeString(s))));
}

void FunctionRegistry::checkSelector(const char* s) const {
    U_ASSERT(hasSelector(FunctionName(UnicodeString(s))));
}

// Debugging
void FunctionRegistry::checkStandard() const {
    checkFormatter("datetime");
    checkFormatter("number");
    checkFormatter("identity");
    checkSelector("plural");
    checkSelector("selectordinal");
    checkSelector("select");
    checkSelector("gender");
}

// Formatter/selector helpers


static void strToInt(const UnicodeString& s, int64_t& result, UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    LocalPointer<NumberFormat> numberFormat(NumberFormat::createInstance(errorCode));
    CHECK_ERROR(errorCode);
    numberFormat->setParseIntegerOnly(true);
    Formattable asNumber;
    numberFormat->parse(s, asNumber, errorCode);
    CHECK_ERROR(errorCode);
    result = asNumber.getInt64(errorCode);
}

static void strToDouble(const UnicodeString& s, Locale loc, double& result, UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    LocalPointer<NumberFormat> numberFormat(NumberFormat::createInstance(loc, errorCode));
    CHECK_ERROR(errorCode);
    Formattable asNumber;
    numberFormat->parse(s, asNumber, errorCode);
    CHECK_ERROR(errorCode);
    result = asNumber.getDouble(errorCode);
}

// Specific formatter implementations

// --------- Number

number::LocalizedNumberFormatter* formatterForOptions(Locale locale, const Hashtable& fixedOptions, UErrorCode& status) {
    NULL_ON_ERROR(status);

    number::UnlocalizedNumberFormatter nf;
    UnicodeString* skeleton = (UnicodeString*) (fixedOptions.get(UnicodeString("skeleton")));
    if (skeleton != nullptr) {
        nf = number::NumberFormatter::forSkeleton(*skeleton, status);
    } else {
        nf = number::NumberFormatter::with();
        UnicodeString* minFractionDigits = (UnicodeString*) fixedOptions.get(UnicodeString("minimumFractionDigits"));
        if (minFractionDigits != nullptr) {
            int64_t minFractionDigitsInt;
            strToInt(*minFractionDigits, minFractionDigitsInt, status);
            nf = nf.precision(number::Precision::minFraction(minFractionDigitsInt));
        }
    }
    NULL_ON_ERROR(status);
    LocalPointer<number::LocalizedNumberFormatter> result(new number::LocalizedNumberFormatter(nf.locale(locale)));
    if (!result.isValid()) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return result.orphan();
}

void addAll(const Hashtable& source, Hashtable& dest, UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    int32_t pos = UHASH_FIRST;
    while (true) {
        const UHashElement* element = source.nextElement(pos);
        if (element == nullptr) {
            break;
        }
        UnicodeString *key = static_cast<UnicodeString *>(element->key.pointer);
        UnicodeString* value = static_cast<UnicodeString*>(element->value.pointer);
        U_ASSERT(key != nullptr && value != nullptr);
        dest.put(*key, value, errorCode);
    }
}

Formatter* StandardFunctions::NumberFactory::createFormatter(Locale locale, const Hashtable& fixedOptions, UErrorCode& errorCode) const {
    NULL_ON_ERROR(errorCode);

    Formatter* result = new Number(locale, fixedOptions);
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return result;
}

// variableOptions = map of *resolved* options (strings)
void StandardFunctions::Number::format(const UnicodeString& toFormat, const Hashtable& variableOptions, UnicodeString& result, UErrorCode& errorCode) const {
    CHECK_ERROR(errorCode);

    LocalPointer<number::LocalizedNumberFormatter> realFormatter;
    if (variableOptions.count() == 0) {
        realFormatter.adoptInstead(new number::LocalizedNumberFormatter(icuFormatter));
        if (!realFormatter.isValid()) {
            errorCode = U_MEMORY_ALLOCATION_ERROR;
        }
    } else {
        // Create a new map and add both the arguments and variable options into it
        LocalPointer<Hashtable> mergedOptions(new Hashtable(compareVariableName, nullptr, errorCode));
        CHECK_ERROR(errorCode);
        addAll(fixedOptions, *mergedOptions, errorCode);
        addAll(variableOptions, *mergedOptions, errorCode);
        CHECK_ERROR(errorCode);
        realFormatter.adoptInstead(formatterForOptions(locale, *mergedOptions, errorCode));
    }
    CHECK_ERROR(errorCode);
    
    // TODO: NumberFormatterFactory.java dispatches on the type of `toFormat`,
    // but here the formatters only take strings.
    // Try to parse the string as a number, as in the `else` case there
    double numberValue;
    strToDouble(toFormat, locale, numberValue, errorCode);
    if (U_FAILURE(errorCode)) {
        errorCode = U_ZERO_ERROR;
        result += UnicodeString("NaN");
        return;
    }
    number::FormattedNumber numberResult = realFormatter->formatDouble(numberValue, errorCode);
    CHECK_ERROR(errorCode);
    result += numberResult.toString(errorCode);
}

// --------- PluralFactory

Selector* StandardFunctions::PluralFactory::createSelector(Locale locale, const Hashtable& fixedOptions, UErrorCode& errorCode) const {
    NULL_ON_ERROR(errorCode);

    // Look up plural rules by locale
    LocalPointer<PluralRules> rules(PluralRules::forLocale(locale, type, errorCode));
    NULL_ON_ERROR(errorCode);
    Selector* result = new Plural(locale, fixedOptions, type, rules.orphan());
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return result;
}

bool StandardFunctions::Plural::matches(const UnicodeString& value, const UnicodeString& key, const Hashtable& variableOptions, UErrorCode& errorCode) const {
    FALSE_ON_ERROR(errorCode);

    // This does not handle the '*' key, which is represented as a non-string
    // and thus shouldn't be passed to this method
    U_ASSERT(key != UnicodeString(ASTERISK));

    // Try parsing the scrutinee as a double
    double valToCheck;
    strToDouble(value, locale, valToCheck, errorCode);
    // Invalid format error => value is not a number; return a selector error
    if (errorCode == U_INVALID_FORMAT_ERROR) {
        errorCode = U_SELECTOR_ERROR;
    }
    FALSE_ON_ERROR(errorCode);

    // TODO: does the Integer case need to be there?
    // See PluralSelectoryFactory.java

    if (!fixedOptions.containsKey(UnicodeString("skeleton")) && !variableOptions.containsKey(UnicodeString("skeleton"))) {
        // Try parsing the key as a Double
        double keyAsDouble;
        strToDouble(key, locale, keyAsDouble, errorCode);
        if (U_SUCCESS(errorCode)) {
            if (valToCheck == keyAsDouble) {
                return true;
            }
        }
        else {
            // We're going to try a different matching strategy,
            // so ignore the failure by resetting the error code
            errorCode = U_ZERO_ERROR;
        }
    }

    UnicodeString match = rules->select(valToCheck);
    return (match == key);
}

StandardFunctions::Plural::~Plural() {}

// --------- DateTimeFactory


static DateFormat::EStyle stringToStyle(UnicodeString option, UErrorCode& errorCode) {
    if (U_SUCCESS(errorCode)) {
        UnicodeString upper = option.toUpper();
        if (upper == UnicodeString("FULL")) {
            return DateFormat::EStyle::kFull;
        }
        if (upper == UnicodeString("LONG")) {
            return DateFormat::EStyle::kLong;
        }
        if (upper == UnicodeString("MEDIUM")) {
            return DateFormat::EStyle::kMedium;
        }
        if (upper == UnicodeString("SHORT")) {
            return DateFormat::EStyle::kShort;
        }
        if (upper.isEmpty() || upper == UnicodeString("DEFAULT")) {
            return DateFormat::EStyle::kDefault;
        }
        errorCode = U_ILLEGAL_ARGUMENT_ERROR;
    }
    return DateFormat::EStyle::kNone;
}

Formatter* StandardFunctions::DateTimeFactory::createFormatter(Locale locale, const Hashtable& fixedOptions, UErrorCode& errorCode) const {
    NULL_ON_ERROR(errorCode);

    // Fixed options not used
    (void) fixedOptions;

    Formatter* result = new DateTime(locale);
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return result;
}

void StandardFunctions::DateTime::format(const UnicodeString& toFormat, const Hashtable& variableOptions, UnicodeString& result, UErrorCode& errorCode) const {
    CHECK_ERROR(errorCode);

    LocalPointer<DateFormat> df;

    UnicodeString* opt = (UnicodeString*) variableOptions.get(UnicodeString("skeleton"));
    if (opt != nullptr) {
        // Same as getInstanceForSkeleton(), see ICU 9029
        // Based on test/intltest/dtfmttst.cpp - TestPatterns()
        LocalPointer<DateTimePatternGenerator> generator(DateTimePatternGenerator::createInstance(locale, errorCode));
        UnicodeString pattern = generator->getBestPattern(*opt, errorCode);
        df.adoptInstead(new SimpleDateFormat(pattern, locale, errorCode));
    } else {
        opt = (UnicodeString*) variableOptions.get(UnicodeString("pattern"));
        if (opt != nullptr) {
            df.adoptInstead(new SimpleDateFormat(*opt, locale, errorCode));
        } else {
            opt = (UnicodeString*) variableOptions.get(UnicodeString("datestyle"));
            DateFormat::EStyle dateStyle = DateFormat::NONE;
            if (opt != nullptr) {
                dateStyle = stringToStyle(*opt, errorCode);
            }
            DateFormat::EStyle timeStyle = DateFormat::NONE;
            opt = (UnicodeString*) variableOptions.get(UnicodeString("timestyle"));
            if (opt != nullptr) {
                timeStyle = stringToStyle(*opt, errorCode);
            }
            if (dateStyle == DateFormat::NONE && timeStyle == DateFormat::NONE) {
                // Match the MessageFormat behavior
                dateStyle = DateFormat::SHORT;
                timeStyle = DateFormat::SHORT;
            }
            df.adoptInstead(DateFormat::createDateTimeInstance(dateStyle, timeStyle, locale));
        }
    }

    // Parse the given string as a date
    // TODO: this assumes the string is in "seconds" format;
    // since this doesn't take a UDate object, I'm not sure
    // what to assume
    //    UDate date = df->parse(toFormat, errorCode);
    double resultDate;
    strToDouble(toFormat, locale, resultDate, errorCode);
    CHECK_ERROR(errorCode);
    df->format(UDate(resultDate), result);
}

// --------- TextFactory

Selector* StandardFunctions::TextFactory::createSelector(Locale locale, const Hashtable& fixedOptions, UErrorCode& errorCode) const {
    // No state
    (void) locale;
    (void) fixedOptions;

    Selector* result = new TextSelector();
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return result;
}

bool StandardFunctions::TextSelector::matches(const UnicodeString& value, const UnicodeString& key, const Hashtable& variableOptions, UErrorCode& errorCode) const {
    // Just compares the key and value as strings
    FALSE_ON_ERROR(errorCode);

    (void) variableOptions; // Unused parameter

    return (key == value);
}

StandardFunctions::TextSelector::~TextSelector() {}

// --------- IdentityFactory

Formatter* StandardFunctions::IdentityFactory::createFormatter(Locale locale, const Hashtable& fixedOptions, UErrorCode& errorCode) const {
    // No state
    (void) locale;
    (void) fixedOptions;

    Formatter* result = new Identity();
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return result;

}

void StandardFunctions::Identity::format(const UnicodeString& toFormat, const Hashtable& variableOptions, UnicodeString& result, UErrorCode& errorCode) const {
    CHECK_ERROR(errorCode);

    (void) variableOptions; // unused parameter
    // Just returns the string
    result = toFormat;
}

StandardFunctions::Identity::~Identity() {}

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

