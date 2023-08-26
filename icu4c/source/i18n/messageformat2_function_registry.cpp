// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/dtptngen.h"
#include "unicode/messageformat2.h"
#include "unicode/messageformat2_formatted_value.h"
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

// ------------------------------------------------------
// Options

using Option = FunctionRegistry::Option;
using Options = FunctionRegistry::Options;

bool Options::getStringOption(const UnicodeString& key, UnicodeString& value) const {
    U_ASSERT(contents.isValid());

    const Option* optionValue = static_cast<const Option*>(contents->get(key));
    if (optionValue != nullptr) {
        switch (optionValue->getType()) {
            case Option::Type::STRING: {
                value = optionValue->getString();
                return true;
            }
            default: {
                break;
            }
        }
    }
    // In all other cases, there is no string option with this name
    return false;
}

static bool tryStringToNumber(const UnicodeString& s, int64_t& result) {
    UErrorCode localErrorCode = U_ZERO_ERROR;
    // Try to parse string as int

    LocalPointer<NumberFormat> numberFormat(NumberFormat::createInstance(localErrorCode));
    if (U_FAILURE(localErrorCode)) {
        return false;
    }
    numberFormat->setParseIntegerOnly(true);
    Formattable asNumber;
    numberFormat->parse(s, asNumber, localErrorCode);
    if (U_SUCCESS(localErrorCode)) {
        result = asNumber.getInt64(localErrorCode);
        if (U_SUCCESS(localErrorCode)) {
            return true;
        }
    }
    return false;
}

bool tryFormattableAsNumber(const Formattable& optionValue, int64_t& result) {
    UErrorCode localErrorCode = U_ZERO_ERROR;
    if (optionValue.isNumeric()) {
        result = optionValue.getInt64(localErrorCode);
        if (U_SUCCESS(localErrorCode)) {
            return true;
        }
    } else {
        if (tryStringToNumber(optionValue.getString(), result)) {
            return true;
        }
    }
    return false;
}

int64_t Options::getIntOption(const UnicodeString& key, int64_t defaultVal) const {
    U_ASSERT(contents.isValid());

    const Option* optionValue = static_cast<const Option*>(contents->get(key));
    int64_t result = defaultVal;
    if (optionValue != nullptr) {
        switch (optionValue->getType()) {
            case Option::Type::STRING: {
                // Try to parse string as int
                if (tryStringToNumber(optionValue->getString(), result)) {
                    return result;
                }
                // Try input
                if (tryFormattableAsNumber(optionValue->getString(), result)) {
                    return result;
                }
                break;
            }
            case Option::Type::DOUBLE: {
                return optionValue->getDouble();
                break;
            }
            case Option::Type::LONG: {
                return optionValue->getLong();
                break;
            }
            case Option::Type::INT64: {
                return optionValue->getInt64();
                break;
            }
            case Option::Type::DATE: {
                // Not a number
                break;
            }
        }
    }
    // Value was either not in the options, or was a string not parsable as a number,
    // or overflow occurred while parsing the string,
    // or it was a date, Return the default value that was provided
    return defaultVal;
}

const Option* Options::nextElement(int32_t& pos, UnicodeString& key) const {
    U_ASSERT(contents.isValid());

    const UHashElement* e = contents->nextElement(pos);
    if (e == nullptr) {
        return nullptr;
    }
    key = *(static_cast<UnicodeString*> (e->key.pointer));
    return static_cast<const Option*> (e->value.pointer);
}

// Adopts its argument
void Options::add(const UnicodeString& name, Option* value, UErrorCode& errorCode) {
    U_ASSERT(contents.isValid());
    contents->put(name, value, errorCode);
}

/* static */ Option* Option::createDouble(double val, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    Option* result = new Option(val);
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

/* static */ Option* Option::createInt64(int64_t val, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    Option* result = new Option(val, Option::Type::INT64);
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

/* static */ Option* Option::createLong(long val, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    Option* result = new Option(val);
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

/* static */ Option* Option::createDate(UDate val, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    Option* result = new Option(val, Option::Type::DATE);
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

/* static */ Option* Option::createString(const UnicodeString& val, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    Option* result = new Option(val);
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

bool Options::empty() const {
    U_ASSERT(contents.isValid());
    return contents->count() == 0;
}

Options::Options(UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    contents.adoptInstead(new Hashtable(compareVariableName, nullptr, errorCode));
    CHECK_ERROR(errorCode);
    // The `contents` hashtable owns the values, but does not own the keys
    contents->setValueDeleter(uprv_deleteUObject);
}

// Formatter/selector helpers

static void strToDouble(const UnicodeString& s, Locale loc, double& result, UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    LocalPointer<NumberFormat> numberFormat(NumberFormat::createInstance(loc, errorCode));
    CHECK_ERROR(errorCode);
    Formattable asNumber;
    numberFormat->parse(s, asNumber, errorCode);
    CHECK_ERROR(errorCode);
    result = asNumber.getDouble(errorCode);
}

Option::~Option() {}

// Specific formatter implementations

// --------- Number

number::LocalizedNumberFormatter* formatterForOptions(Locale locale, const State& context, UErrorCode& status) {
    NULL_ON_ERROR(status);

    number::UnlocalizedNumberFormatter nf;
    UnicodeString skeleton;
    if (context.getStringOption(UnicodeString("skeleton"), skeleton)) {
        nf = number::NumberFormatter::forSkeleton(skeleton, status);
    } else {
        int64_t minFractionDigits = 0;
        context.getInt64Option(UnicodeString("minimumFractionDigits"), minFractionDigits);
        nf = number::NumberFormatter::with().precision(number::Precision::minFraction(minFractionDigits));
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

static void notANumber(State& context) {
    context.setOutput(UnicodeString("NaN"));
}

static void stringAsNumber(Locale locale, const number::LocalizedNumberFormatter nf, State& context, UnicodeString s, int64_t offset, UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    double numberValue;
    UErrorCode localErrorCode = U_ZERO_ERROR;
    strToDouble(s, locale, numberValue, localErrorCode);
    if (U_FAILURE(localErrorCode)) {
        notANumber(context);
        return;
    }
    UErrorCode savedStatus = errorCode;
    number::FormattedNumber result = nf.formatDouble(numberValue - offset, errorCode);
    // Ignore U_USING_DEFAULT_WARNING
    if (errorCode == U_USING_DEFAULT_WARNING) {
        errorCode = savedStatus;
    }
    context.setOutput(std::move(result));
}

void StandardFunctions::Number::format(State& context, UErrorCode& errorCode) const {
    CHECK_ERROR(errorCode);

    // No argument => return "NaN"
    if (!context.hasFormattableInput()) {
        return notANumber(context);
    }

    int64_t offset = 0;
    context.getInt64Option(UnicodeString("offset"), offset);

    LocalPointer<number::LocalizedNumberFormatter> realFormatter;
    if (context.optionsCount() == 0) {
        realFormatter.adoptInstead(new number::LocalizedNumberFormatter(icuFormatter));
        if (!realFormatter.isValid()) {
            errorCode = U_MEMORY_ALLOCATION_ERROR;
        }
    } else {
        realFormatter.adoptInstead(formatterForOptions(locale, context, errorCode));
    }
    CHECK_ERROR(errorCode);

    if (context.hasStringOutput()) {
        stringAsNumber(locale, *realFormatter, context, context.getStringOutput(), offset, errorCode);
        return;
    } else if (context.hasNumberOutput()) {
        // Nothing to do
        return;
    }

    number::FormattedNumber numberResult;
    // Already checked that input is present
    const Formattable& toFormat = context.getFormattableInput(); 
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
        stringAsNumber(locale, *realFormatter, context, toFormat.getString(), offset, errorCode);
        return;
    }
    default: {
        // Other types can't be parsed as a number
        notANumber(context);
        return;
    }
    }
    
    CHECK_ERROR(errorCode);
    context.setOutput(std::move(numberResult));
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
            return;
        }
        default: {
            noMatch = true;
            return;
        }
    }
    noMatch = false;
}

void StandardFunctions::Plural::selectKey(State& context, const UnicodeString* keys/*[]*/, size_t numKeys,  UnicodeString* prefs/*[]*/, size_t& numMatching, UErrorCode& errorCode) const {
    CHECK_ERROR(errorCode);

    // No argument => return "NaN"
    if (!context.hasFormattableInput()) {
        context.setSelectorError(UnicodeString("plural"), errorCode);
        return;
    }

    int64_t offset = 0;
    context.getInt64Option(UnicodeString("offset"), offset);

    // Only doubles and integers can match
    double valToCheck;
    bool noMatch = true;

    bool isFormattedNumber = context.hasNumberOutput();
    bool isFormattedString = context.hasStringOutput();

    if (isFormattedString) {
        // Formatted string: try parsing it as a number
        tryAsString(locale, context.getStringOutput(), valToCheck, noMatch);
    } else {
        // Already checked that input is present
        tryWithFormattable(locale, context.getFormattableInput(), valToCheck, noMatch);
    }

    if (noMatch) {
        // Non-number => selector error
        context.setSelectorError(UnicodeString("plural"), errorCode);
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
    UnicodeString match;
    if (isFormattedNumber) {
        match = rules->select(context.getNumberOutput(), errorCode);
    } else {
        match = rules->select(valToCheck - offset);
    }
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

void StandardFunctions::DateTime::format(State& context, UErrorCode& errorCode) const {
    CHECK_ERROR(errorCode);

    // Argument must be present
    if (!context.hasFormattableInput()) {
        context.setFormattingWarning(UnicodeString("datetime"), errorCode);
        return;
    }

    LocalPointer<DateFormat> df;

    UnicodeString opt;
    if (context.getStringOption(UnicodeString("skeleton"), opt)) {
        // Same as getInstanceForSkeleton(), see ICU 9029
        // Based on test/intltest/dtfmttst.cpp - TestPatterns()
        LocalPointer<DateTimePatternGenerator> generator(DateTimePatternGenerator::createInstance(locale, errorCode));
        UnicodeString pattern = generator->getBestPattern(opt, errorCode);
        df.adoptInstead(new SimpleDateFormat(pattern, locale, errorCode));
    } else {
        if (context.getStringOption(UnicodeString("pattern"), opt)) {
            df.adoptInstead(new SimpleDateFormat(opt, locale, errorCode));
        } else {
            DateFormat::EStyle dateStyle = DateFormat::NONE;
            if (context.getStringOption(UnicodeString("datestyle"), opt)) {
                dateStyle = stringToStyle(opt, errorCode);
            }
            DateFormat::EStyle timeStyle = DateFormat::NONE;
            if (context.getStringOption(UnicodeString("timestyle"), opt)) {
                timeStyle = stringToStyle(opt, errorCode);
            }
            if (dateStyle == DateFormat::NONE && timeStyle == DateFormat::NONE) {
                df.adoptInstead(State::defaultDateTimeInstance(locale, errorCode));
            } else {
                df.adoptInstead(DateFormat::createDateTimeInstance(dateStyle, timeStyle, locale));
                if (!df.isValid()) {
                    errorCode = U_MEMORY_ALLOCATION_ERROR;
                    return;
                }
            }
        }
    }

    CHECK_ERROR(errorCode);

    UnicodeString result;

    df->format(context.getFormattableInput(), result, 0, errorCode);
    context.setOutput(result);
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

void StandardFunctions::TextSelector::selectKey(State& context, const UnicodeString* keys/*[]*/, size_t numKeys, UnicodeString* prefs/*[]*/, size_t& numMatching, UErrorCode& errorCode) const {
    CHECK_ERROR(errorCode);

    // Just compares the key and value as strings

    // Argument must be present
    if (!context.hasFormattableInput()) {
        context.setSelectorError(UnicodeString("select"), errorCode);
        return;
    }

    U_ASSERT(prefs != nullptr);
    numMatching = 0;

    // Convert to string
    context.formatToString(locale, errorCode);
    CHECK_ERROR(errorCode);
    if (!context.hasStringOutput()) {
        numMatching = 0;
        return;
    }

    const UnicodeString& formattedValue = context.getStringOutput();

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
    Formatter* result = new Identity(locale);
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return result;

}

void StandardFunctions::Identity::format(State& context, UErrorCode& errorCode) const {
    CHECK_ERROR(errorCode);

    // Argument must be present
    if (!context.hasFormattableInput()) {
        context.setFormattingWarning(UnicodeString("text"), errorCode);
        return;
    }

    // Just returns the input value as a string
    context.formatToString(locale, errorCode);
}

StandardFunctions::Identity::~Identity() {}

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

