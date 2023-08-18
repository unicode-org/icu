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
    if (opts.containsKey(key)) {
        FormattedPlaceholder* val = (FormattedPlaceholder*) opts.get(key);
        U_ASSERT(val != nullptr);
        if (val->getType() == FormattedPlaceholder::Type::STRING) {
            result = val->getString();
            exists = true;
            return;
        }
        if (val->getType() == FormattedPlaceholder::Type::DYNAMIC && val->getInput().getType() == Formattable::Type::kString) {
            result = val->getInput().getString();
            exists = true;
            return;
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
        if (val->getType() == FormattedPlaceholder::Type::STRING) {
            tryStringToNumber(val->getString(), result);
            return;
        }
        if (val->getType() == FormattedPlaceholder::Type::DYNAMIC) {
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
        // No case for FormattedNumber
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

    LocalPointer<Formattable> s(new Formattable("NaN"));
    if (!s.isValid()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return FormattedPlaceholder::create(s.orphan(), errorCode);
}

static FormattedPlaceholder* stringAsNumber(Locale locale, const number::LocalizedNumberFormatter nf, Formattable* fp, UnicodeString s, int64_t offset, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    U_ASSERT(fp != nullptr);

    double numberValue;
    UErrorCode localErrorCode = U_ZERO_ERROR;
    strToDouble(s, locale, numberValue, localErrorCode);
    if (U_FAILURE(localErrorCode)) {
        return notANumber(errorCode);
    }
    return FormattedPlaceholder::create(fp, nf.formatDouble(numberValue - offset, errorCode), errorCode);
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
    LocalPointer<Formattable> copiedInput(new Formattable(arg->getInput()));
    if (!copiedInput.isValid()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    switch (arg->getType()) {
        case FormattedPlaceholder::Type::STRING: {
            return stringAsNumber(locale, *realFormatter, copiedInput.orphan(), arg->getString(), offset, errorCode);
        }
        case FormattedPlaceholder::Type::NUMBER: {
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
                return stringAsNumber(locale, *realFormatter, copiedInput.orphan(), toFormat.getString(), offset, errorCode);
            }
            default: {
                // Other types can't be parsed as a number
                return notANumber(errorCode);
            }
            }
        }
    }
    
    NULL_ON_ERROR(errorCode);
    return FormattedPlaceholder::create(copiedInput.orphan(), std::move(numberResult), errorCode);
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
    const Formattable& value = valuePtr->getType() == FormattedPlaceholder::Type::STRING ? Formattable(valuePtr->getString()) : valuePtr->getInput();

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
            // Try parsing the scrutinee as a double
            strToDouble(value.getString(), locale, valToCheck, errorCode);
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

    UnicodeString match = valuePtr->getType() == FormattedPlaceholder::Type::NUMBER ? rules->select(valuePtr->getNumber(), errorCode)
        : rules->select(valToCheck - offset);
    CHECK_ERROR(errorCode);

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
        errorCode = U_FORMATTING_ERROR;
        return nullptr;
    }
    // TODO: is this correct if arg is a Number or formatted string?
    const Formattable& toFormat = arg->getInput();

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
                LocalPointer<Formattable> copy(new Formattable(toFormat));
                if (!copy.isValid()) {
                    errorCode = U_MEMORY_ALLOCATION_ERROR;
                    return nullptr;
                }
                return FormattedPlaceholder::formatDateWithDefaults(locale, copy.orphan(), errorCode);
            } else {
                df.adoptInstead(DateFormat::createDateTimeInstance(dateStyle, timeStyle, locale));
                if (!df.isValid()) {
                    errorCode = U_MEMORY_ALLOCATION_ERROR;
                    return nullptr;
                }
            }
        }
    }

    UnicodeString result;
    df->format(toFormat, result, 0, errorCode);
    return FormattedPlaceholder::create(result, errorCode);
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

