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

static void getStringOpt(const Hashtable& opts, const UnicodeString& key, UnicodeString& result, bool& exists) {
    // Returns null if key is absent or is not a string
    // (including if the key is an explicit NULL argument)
    if (opts.containsKey(key)) {
        FormattedPlaceholder* val = (FormattedPlaceholder*) opts.get(key);
        U_ASSERT(val != nullptr);
        switch (val->getType()) {
            case FormattedPlaceholder::Type::STRING: {
                result = val->getString();
                exists = true;
                return;
            }
            case FormattedPlaceholder::Type::DYNAMIC: {
                if (val->getInput().getType() == Formattable::Type::kString) {
                    result = val->getInput().getString();
                    exists = true;
                    return;
                }
                break;
            }
           case FormattedPlaceholder::Type::NUMBER:
           case FormattedPlaceholder::Type::NULL_ARGUMENT: {
               // Not a string key
               break;
           }
        }
    }
    exists = false;
}

static void tryStringToNumber(const UnicodeString& s, int64_t& result) {
    UErrorCode localErrorCode = U_ZERO_ERROR;
    // Try to parse string as int
    int64_t tempResult;
    strToInt(s, tempResult, localErrorCode);
    if (U_SUCCESS(localErrorCode)) {
        result = tempResult;
    }
}

static void getIntOpt(const Hashtable& opts, const UnicodeString& key, int64_t& result) {
    // Doesn't modify `result` if `key` is absent or can't be coerced to a number
    if (opts.containsKey(key)) {
        FormattedPlaceholder* val = (FormattedPlaceholder*) opts.get(key);
        U_ASSERT(val != nullptr);
        switch (val->getType()) {
            case FormattedPlaceholder::Type::STRING: {
                tryStringToNumber(val->getString(), result);
                return;
            }
            case FormattedPlaceholder::Type::DYNAMIC: {
                switch (val->getInput().getType()) {
                case Formattable::Type::kDouble: {
                    result = (int64_t) val->getInput().getDouble();
                    return;
                }
                case Formattable::Type::kLong: {
                    result = (int64_t) val->getInput().getLong();
                    return;
                }
                case Formattable::Type::kInt64: {
                    result = val->getInput().getInt64();
                    return;
                }
                case Formattable::Type::kString: {
                    tryStringToNumber(val->getInput().getString(), result);
                    return;
                }
                default: {
                    // Can't be cast to number
                    return;
                }
                }
            }
            case FormattedPlaceholder::Type::NUMBER:
            case FormattedPlaceholder::Type::NULL_ARGUMENT: {
                // Can't be cast to number
                return;
            }
        }
    }
}

number::LocalizedNumberFormatter* formatterForOptions(Locale locale, const Hashtable& options, UErrorCode& status) {
    NULL_ON_ERROR(status);

    number::UnlocalizedNumberFormatter nf;
    bool hasSkeleton = false;
    UnicodeString skeleton;
    getStringOpt(options, UnicodeString("skeleton"), skeleton, hasSkeleton);
    if (hasSkeleton) {
        nf = number::NumberFormatter::forSkeleton(skeleton, status);
    } else {
        nf = number::NumberFormatter::with();
        bool hasMinFractionDigits = false;
        UnicodeString minFractionDigits;
        getStringOpt(options, UnicodeString("minimumFractionDigits"), minFractionDigits, hasMinFractionDigits);
        if (hasMinFractionDigits) {
            int64_t minFractionDigitsInt;
            strToInt(minFractionDigits, minFractionDigitsInt, status);
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

Formatter* StandardFunctions::NumberFactory::createFormatter(Locale locale, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    Formatter* result = new Number(locale);
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return result;
}

static FormattedPlaceholder* notANumber(UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    return FormattedPlaceholder::create(UnicodeString("NaN"), errorCode);
}

static FormattedPlaceholder* stringAsNumber(Locale locale, const number::LocalizedNumberFormatter nf, const Formattable* fp, UnicodeString s, int64_t offset, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    U_ASSERT(fp != nullptr);

    double numberValue;
    UErrorCode localErrorCode = U_ZERO_ERROR;
    strToDouble(s, locale, numberValue, localErrorCode);
    if (U_FAILURE(localErrorCode)) {
        return notANumber(errorCode);
    }
    UErrorCode savedStatus = errorCode;
    number::FormattedNumber result = nf.formatDouble(numberValue - offset, errorCode);
    // Ignore U_USING_DEFAULT_WARNING
    if (errorCode == U_USING_DEFAULT_WARNING) {
        errorCode = savedStatus;
    }
    return FormattedPlaceholder::create(fp, std::move(result), errorCode);
}

// variableOptions = map of *resolved* options (strings)
FormattedPlaceholder* StandardFunctions::Number::format(FormattedPlaceholder* arg, const Hashtable& variableOptions, UErrorCode& errorCode) const {
    NULL_ON_ERROR(errorCode);

    // No argument => return "NaN"
    if (arg == nullptr) {
        return notANumber(errorCode);
    }

    int64_t offset = 0;
    getIntOpt(variableOptions, UnicodeString("offset"), offset);

    LocalPointer<number::LocalizedNumberFormatter> realFormatter;
    if (variableOptions.count() == 0) {
        realFormatter.adoptInstead(new number::LocalizedNumberFormatter(icuFormatter));
        if (!realFormatter.isValid()) {
            errorCode = U_MEMORY_ALLOCATION_ERROR;
        }
    } else {
        realFormatter.adoptInstead(formatterForOptions(locale, variableOptions, errorCode));
    }
    NULL_ON_ERROR(errorCode);

    number::FormattedNumber numberResult;
    switch (arg->getType()) {
        case FormattedPlaceholder::Type::STRING: {
            return stringAsNumber(locale, *realFormatter, arg->aliasInput(), arg->getString(), offset, errorCode);
        }
        case FormattedPlaceholder::Type::NUMBER: {
/*
  This is why functions can't just take a `const FormattedPlaceholder*`.
  The number can't be copied, so we have to invalidate the input
  `FormattedPlaceholder*`. At the same time, we can't just return a `const FormattedPlaceholder*`
  since if we do allocate a new FormattedPlaceholder*, the caller has to take ownership
*/
            // TODO: passing in a number just returns the same number,
            // with options ignored. is that right?
            numberResult = arg->getNumber();
            break;
        }
        case FormattedPlaceholder::Type::DYNAMIC: {
            const Formattable& toFormat = arg->getInput(); 
            switch (toFormat.getType()) {
            case Formattable::Type::kDouble: {
                numberResult = realFormatter->formatDouble(toFormat.getDouble() - offset, errorCode);
                break;
            }
            case Formattable::Type::kLong: {
                numberResult = realFormatter->formatInt(toFormat.getLong() - offset, errorCode);
                break;
            }
            case Formattable::Type::kInt64: {
                numberResult = realFormatter->formatInt(toFormat.getInt64() - offset, errorCode);
                break;
            }
            case Formattable::Type::kString: {
                // Try to parse the string as a number, as in the `else` case there
                // TODO: see if the behavior here matches the function registry spec
                return stringAsNumber(locale, *realFormatter, arg->aliasInput(), toFormat.getString(), offset, errorCode);
            }
            default: {
                // Other types can't be parsed as a number
                return notANumber(errorCode);
            }
            }
            break;
        }
        case FormattedPlaceholder::Type::NULL_ARGUMENT: {
            return notANumber(errorCode);
        }
    }
    
    NULL_ON_ERROR(errorCode);
    return FormattedPlaceholder::create(arg->aliasInput(), std::move(numberResult), errorCode);
}

// --------- PluralFactory

Selector* StandardFunctions::PluralFactory::createSelector(Locale locale, UErrorCode& errorCode) const {
    NULL_ON_ERROR(errorCode);

    // Look up plural rules by locale
    LocalPointer<PluralRules> rules(PluralRules::forLocale(locale, type, errorCode));
    NULL_ON_ERROR(errorCode);
    Selector* result = new Plural(locale, type, rules.orphan());
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return result;
}

static void tryAsString(const Locale& locale, const UnicodeString& s, double& valToCheck, bool& noMatch) {
    // Try parsing the inputString as a double
    UErrorCode localErrorCode = U_ZERO_ERROR;
    strToDouble(s, locale, valToCheck, localErrorCode);
    // Invalid format error => value is not a number; no match
    if (U_FAILURE(localErrorCode)) {
        noMatch = true;
        return;
    }
    noMatch = false;
}

static void tryWithFormattable(const Locale& locale, const Formattable& value, double& valToCheck, bool& noMatch) {
    switch (value.getType()) {
        case Formattable::Type::kDouble: {
            valToCheck = value.getDouble();
            break;
        }
        case Formattable::Type::kLong: {
            valToCheck = (double) value.getLong();
            break;
        }
        case Formattable::Type::kInt64: {
            valToCheck = (double) value.getInt64();
            break;
        }
        case Formattable::Type::kString: {
            tryAsString(locale, value.getString(), valToCheck, noMatch);
            break;
        }
        default: {
            noMatch = true;
            return;
        }
    }
    noMatch = false;
}

void StandardFunctions::Plural::selectKey(const FormattedPlaceholder* valuePtr, const UnicodeString* keys/*[]*/, size_t numKeys, const Hashtable& variableOptions, UnicodeString* prefs/*[]*/, size_t& numMatching, UErrorCode& errorCode) const {
    CHECK_ERROR(errorCode);

    // Variable options not used
    (void) variableOptions;

    // Argument must be present
    if (valuePtr == nullptr) {
        errorCode = U_SELECTOR_ERROR;
        return;
    }

    int64_t offset = 0;
    getIntOpt(variableOptions, UnicodeString("offset"), offset);

    // Only doubles and integers can match
    double valToCheck;
    bool noMatch = true;

    switch (valuePtr->getType()) {
        // Formatted string: try parsing it as a number
        case FormattedPlaceholder::Type::STRING: {
            tryAsString(locale, valuePtr->getString(), valToCheck, noMatch);
            break;
        }
        // Number: use the original input and parse it as a number
        case FormattedPlaceholder::Type::NUMBER:
        // Formattable: check if it's a number or parseable as a number
        case FormattedPlaceholder::Type::DYNAMIC: {
            tryWithFormattable(locale, valuePtr->getInput(), valToCheck, noMatch);
        }
        // These values never match; noMatch is already true
        case FormattedPlaceholder::Type::NULL_ARGUMENT: {
            break;
        }
    }

    if (noMatch) {
        // Non-number => selector error
        errorCode = U_SELECTOR_ERROR;
        numMatching = 0;
        return;
    }

    // Generate the matches
    // -----------------------

    U_ASSERT(keys != nullptr);
    // First, check for an exact match
    numMatching = 0;
    double keyAsDouble = 0;
    for (size_t i = 0; i < numKeys; i++) {
        // Try parsing the key as a double
        UErrorCode localErrorCode = U_ZERO_ERROR;
        strToDouble(keys[i], locale, keyAsDouble, localErrorCode);
        if (U_SUCCESS(localErrorCode)) {
            if (valToCheck == keyAsDouble) {
                prefs[numMatching++] = keys[i];
                break;
            }
        }
    }
    if (numMatching > 0) {
        return;
    }

    // If there was no exact match, check for a match based on the plural category
    UnicodeString match = valuePtr->isFormattedNumber() ? rules->select(valuePtr->getNumber(), errorCode)
        : rules->select(valToCheck - offset);
    CHECK_ERROR(errorCode);

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

Formatter* StandardFunctions::DateTimeFactory::createFormatter(Locale locale, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    Formatter* result = new DateTime(locale);
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return result;
}

FormattedPlaceholder* StandardFunctions::DateTime::format(FormattedPlaceholder* arg, const Hashtable& variableOptions, UErrorCode& errorCode) const {
    NULL_ON_ERROR(errorCode);

    if (arg == nullptr) {
        errorCode = U_FORMATTING_WARNING;
        return nullptr;
    }

    LocalPointer<DateFormat> df;

    UnicodeString opt;
    bool hasSkeleton = false;
    getStringOpt(variableOptions, UnicodeString("skeleton"), opt, hasSkeleton);
    if (hasSkeleton) {
        // Same as getInstanceForSkeleton(), see ICU 9029
        // Based on test/intltest/dtfmttst.cpp - TestPatterns()
        LocalPointer<DateTimePatternGenerator> generator(DateTimePatternGenerator::createInstance(locale, errorCode));
        UnicodeString pattern = generator->getBestPattern(opt, errorCode);
        df.adoptInstead(new SimpleDateFormat(pattern, locale, errorCode));
    } else {
        bool hasPattern = false;
        getStringOpt(variableOptions, UnicodeString("pattern"), opt, hasPattern);
        if (hasPattern) {
            df.adoptInstead(new SimpleDateFormat(opt, locale, errorCode));
        } else {
            bool hasOpt = false;
            getStringOpt(variableOptions, UnicodeString("datestyle"), opt, hasOpt);
            DateFormat::EStyle dateStyle = DateFormat::NONE;
            if (hasOpt) {
                dateStyle = stringToStyle(opt, errorCode);
            }
            DateFormat::EStyle timeStyle = DateFormat::NONE;
            getStringOpt(variableOptions, UnicodeString("timestyle"), opt, hasOpt);
            if (hasOpt) {
                timeStyle = stringToStyle(opt, errorCode);
            }
            if (dateStyle == DateFormat::NONE && timeStyle == DateFormat::NONE) {
                df.adoptInstead(FormattedPlaceholder::defaultDateTimeInstance(locale, errorCode));
            } else {
                df.adoptInstead(DateFormat::createDateTimeInstance(dateStyle, timeStyle, locale));
                if (!df.isValid()) {
                    errorCode = U_MEMORY_ALLOCATION_ERROR;
                    return nullptr;
                }
            }
        }
    }

    NULL_ON_ERROR(errorCode);

    UnicodeString result;
    // TODO: is this correct if arg is a Number or formatted string?
    switch (arg->getType()) {
        case FormattedPlaceholder::Type::DYNAMIC: {
            df->format(arg->getInput(), result, 0, errorCode);
            break;
        }
        case FormattedPlaceholder::Type::STRING:
        case FormattedPlaceholder::Type::NUMBER: 
        case FormattedPlaceholder::Type::NULL_ARGUMENT: {
            errorCode = U_FORMATTING_WARNING;
            return nullptr;
        }
    }
    return FormattedPlaceholder::create(arg->aliasInput(), result, errorCode);
}

// --------- TextFactory

Selector* StandardFunctions::TextFactory::createSelector(Locale locale, UErrorCode& errorCode) const {
    Selector* result = new TextSelector(locale);
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return result;
}

void StandardFunctions::TextSelector::selectKey(const FormattedPlaceholder* value, const UnicodeString* keys/*[]*/, size_t numKeys, const Hashtable& options, UnicodeString* prefs/*[]*/, size_t& numMatching, UErrorCode& errorCode) const {
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
    UErrorCode localErrorCode = U_ZERO_ERROR;
    UnicodeString formattedValue = value->toString(locale, localErrorCode);
    if (U_FAILURE(localErrorCode)) {
        // Don't pass the error through, just return "no match"
        numMatching = 0;
        return;
    }

    for (size_t i = 0; i < numKeys; i++) {
        if (keys[i] == formattedValue) {
            numMatching++;
            prefs[0] = keys[i];
            break;
        }
    }
}

StandardFunctions::TextSelector::~TextSelector() {}

// --------- IdentityFactory

Formatter* StandardFunctions::IdentityFactory::createFormatter(Locale locale, UErrorCode& errorCode) {
    // Locale not used
    (void) locale;

    Formatter* result = new Identity();
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return result;

}

FormattedPlaceholder* StandardFunctions::Identity::format(FormattedPlaceholder* toFormat, const Hashtable& variableOptions, UErrorCode& errorCode) const {
    NULL_ON_ERROR(errorCode);

    (void) variableOptions; // unused parameter
    if (toFormat == nullptr) {
        errorCode = U_FORMATTING_WARNING;
        return nullptr;
    }
    // Just returns the input value as a string
    return FormattedPlaceholder::create(toFormat->aliasInput(), toFormat->getString(), errorCode);
}

StandardFunctions::Identity::~Identity() {}

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

