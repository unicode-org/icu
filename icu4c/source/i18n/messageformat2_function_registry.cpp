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

FormatterFactory* FunctionRegistry::getFormatter(const FunctionName& formatterName) const {
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
            if (U_FAILURE(status)) {
                // option didn't parse as an int -- reset error and use default
                status= U_ZERO_ERROR;
            } else {
                nf = nf.precision(number::Precision::minFraction(minFractionDigitsInt));
            }
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

static void addAll(const Hashtable& source, Hashtable& dest, UErrorCode& errorCode) {
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

Formatter* StandardFunctions::NumberFactory::createFormatter(Locale locale, const Hashtable& fixedOptions, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    Formatter* result = new Number(locale, fixedOptions);
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return result;
}

static FormattedPlaceholder* notANumber(UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    LocalPointer<Formattable> s(new Formattable("NaN"));
    if (!s.isValid()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return FormattedPlaceholder::create(s.orphan(), errorCode);
}

// variableOptions = map of *resolved* options (strings)
FormattedPlaceholder* StandardFunctions::Number::format(const Formattable* arg, const Hashtable& variableOptions, UErrorCode& errorCode) const {
    NULL_ON_ERROR(errorCode);

    // Argument must be non-null
    if (arg == nullptr) {
        errorCode = U_FORMATTING_ERROR;
        return nullptr;
    }

    const Formattable& toFormat = *arg;

    LocalPointer<number::LocalizedNumberFormatter> realFormatter;
    if (variableOptions.count() == 0) {
        realFormatter.adoptInstead(new number::LocalizedNumberFormatter(icuFormatter));
        if (!realFormatter.isValid()) {
            errorCode = U_MEMORY_ALLOCATION_ERROR;
        }
    } else {
        // Create a new map and add both the arguments and variable options into it
        LocalPointer<Hashtable> mergedOptions(new Hashtable(compareVariableName, nullptr, errorCode));
        NULL_ON_ERROR(errorCode);
        addAll(fixedOptions, *mergedOptions, errorCode);
        addAll(variableOptions, *mergedOptions, errorCode);
        NULL_ON_ERROR(errorCode);
/*
  TODO: check options/ignore invalid here?
*/

        realFormatter.adoptInstead(formatterForOptions(locale, *mergedOptions, errorCode));
    }
    NULL_ON_ERROR(errorCode);

    number::FormattedNumber numberResult;
    switch (toFormat.getType()) {
        case Formattable::Type::kDouble: {
            numberResult = realFormatter->formatDouble(toFormat.getDouble(),
                                                       errorCode);
            break;
        }
        case Formattable::Type::kLong: {
            numberResult = realFormatter->formatInt(toFormat.getLong(),
                                                    errorCode);
            break;
        }
        case Formattable::Type::kInt64: {
            numberResult = realFormatter->formatInt(toFormat.getInt64(),
                                                    errorCode);
            break;
        }
        case Formattable::Type::kString: {
            // Try to parse the string as a number, as in the `else` case there
            // TODO: see if the behavior here matches the function registry spec
            double numberValue;
            UErrorCode localErrorCode;
            strToDouble(toFormat.getString(), locale, numberValue, localErrorCode);
            if (U_FAILURE(localErrorCode)) {
                return notANumber(errorCode);
            }
            break;
        }
        default: {
            // Other types can't be parsed as a number
            return notANumber(errorCode);
        }   
    }
    
    NULL_ON_ERROR(errorCode);
    LocalPointer<number::FormattedNumber> num(&numberResult);
    if (!num.isValid()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return FormattedPlaceholder::create(num.orphan(), errorCode);
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

void StandardFunctions::Plural::selectKey(const Formattable* valuePtr, const UnicodeString* keys/*[]*/, size_t numKeys, const Hashtable& variableOptions, UnicodeString* prefs/*[]*/, size_t& numMatching, UErrorCode& errorCode) const {
    CHECK_ERROR(errorCode);

    // Variable options not used
    (void) variableOptions;

    // Argument must be present
    if (valuePtr == nullptr) {
        errorCode = U_SELECTOR_ERROR;
        return;
    }

    // Only doubles and integers can match
    double valToCheck;
    switch (valuePtr->getType()) {
        case Formattable::Type::kDouble: {
            valToCheck = valuePtr->getDouble();
            break;
        }
        case Formattable::Type::kLong: {
            valToCheck = (double) valuePtr->getLong();
            break;
        }
        case Formattable::Type::kInt64: {
            valToCheck = (double) valuePtr->getInt64();
            break;
        }
        case Formattable::Type::kString: {
            // Try parsing the scrutinee as a double
            strToDouble(valuePtr->getString(), locale, valToCheck, errorCode);
            // Invalid format error => value is not a number; return a selector error
            if (errorCode == U_INVALID_FORMAT_ERROR) {
                errorCode = U_SELECTOR_ERROR;
                return;
            }
            CHECK_ERROR(errorCode);
            break;
        }
        default: {
            numMatching = 0;
            return;
        }
    }

    // Generate the matches
    // -----------------------

    U_ASSERT(keys != nullptr);
    // First, check for an exact match
    numMatching = 0;
    double keyAsDouble = 0;
    for (size_t i = 0; i < numKeys; i++) {
        // Try parsing the key as a double
        strToDouble(keys[i], locale, keyAsDouble, errorCode);
        if (U_SUCCESS(errorCode)) {
            if (valToCheck == keyAsDouble) {
                prefs[numMatching++] = keys[i];
                break;
            }
        }
        else {
            // We're going to try a different matching strategy,
            // so ignore the failure by resetting the error code
            errorCode = U_ZERO_ERROR;
        }
    }

    UnicodeString match = rules->select(valToCheck);
    
    // Next, check for a match based on the plural category
    for (size_t i = 0; i < numKeys; i ++) {
        if (match == keys[i]) {
            prefs[numMatching++] = keys[i];
            break;
        }
    }
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

Formatter* StandardFunctions::DateTimeFactory::createFormatter(Locale locale, const Hashtable& fixedOptions, UErrorCode& errorCode) {
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

FormattedPlaceholder* StandardFunctions::DateTime::format(const Formattable* arg, const Hashtable& variableOptions, UErrorCode& errorCode) const {
    NULL_ON_ERROR(errorCode);

    if (arg == nullptr) {
        errorCode = U_FORMATTING_ERROR;
        return nullptr;
    }
    const Formattable& toFormat = *arg;

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

    UnicodeString result;
    df->format(toFormat, result, 0, errorCode);
    return FormattedPlaceholder::create(result, errorCode);
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

void StandardFunctions::TextSelector::selectKey(const Formattable* value, const UnicodeString* keys/*[]*/, size_t numKeys, const Hashtable& options, UnicodeString* prefs/*[]*/, size_t& numMatching, UErrorCode& errorCode) const {
    CHECK_ERROR(errorCode);

    // Just compares the key and value as strings

    (void) options; // Unused parameter

    // Argument must be non-null
    if (value == nullptr) {
        errorCode = U_SELECTOR_ERROR;
        return;
    }

    U_ASSERT(prefs != nullptr);
    numMatching = 0;

    // Convert to string
    const UnicodeString& valueAsString = value->getString();

    for (size_t i = 0; i < numKeys; i++) {
        if (keys[i] == valueAsString) {
            numMatching++;
            prefs[0] = keys[i];
            break;
        }
    }
}

StandardFunctions::TextSelector::~TextSelector() {}

// --------- IdentityFactory

Formatter* StandardFunctions::IdentityFactory::createFormatter(Locale locale, const Hashtable& fixedOptions, UErrorCode& errorCode) {
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

FormattedPlaceholder* StandardFunctions::Identity::format(const Formattable* toFormat, const Hashtable& variableOptions, UErrorCode& errorCode) const {
    NULL_ON_ERROR(errorCode);

    (void) variableOptions; // unused parameter
    if (toFormat == nullptr) {
        errorCode = U_FORMATTING_ERROR;
        return nullptr;
    }
    // Just returns the input value as a string
    return FormattedPlaceholder::create(toFormat->getString(), errorCode);
}

StandardFunctions::Identity::~Identity() {}

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

