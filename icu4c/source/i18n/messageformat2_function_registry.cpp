// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#if !UCONFIG_NO_MF2

#include <math.h>

#include "unicode/dtptngen.h"
#include "unicode/messageformat2_data_model_names.h"
#include "unicode/messageformat2_function_registry.h"
#include "unicode/smpdtfmt.h"
#include "charstr.h"
#include "double-conversion.h"
#include "messageformat2_allocation.h"
#include "messageformat2_function_registry_internal.h"
#include "messageformat2_macros.h"
#include "hash.h"
#include "number_types.h"
#include "uvector.h" // U_ASSERT

// The C99 standard suggested that C++ implementations not define PRId64 etc. constants
// unless this macro is defined.
// See the Notes at https://en.cppreference.com/w/cpp/types/integer .
// Similar to defining __STDC_LIMIT_MACROS in unicode/ptypes.h .
#ifndef __STDC_FORMAT_MACROS
#   define __STDC_FORMAT_MACROS
#endif
#include <inttypes.h>
#include <math.h>

U_NAMESPACE_BEGIN

namespace message2 {

// Function registry implementation

FunctionFactory::~FunctionFactory() {}
Function::~Function() {}
FunctionValue::~FunctionValue() {}

MFFunctionRegistry MFFunctionRegistry::Builder::build() {
    U_ASSERT(functions != nullptr);
    U_ASSERT(formattersByType != nullptr);
    MFFunctionRegistry result = MFFunctionRegistry(functions, formattersByType);
    functions = nullptr;
    formattersByType = nullptr;
    return result;
}

MFFunctionRegistry::Builder&
MFFunctionRegistry::Builder::adoptFunctionFactory(const FunctionName& functionName,
                                                  FunctionFactory* function,
                                                  UErrorCode& errorCode) {
    if (U_SUCCESS(errorCode)) {
        U_ASSERT(functions != nullptr);
        functions->put(functionName, function, errorCode);
    }
    return *this;
}

MFFunctionRegistry::Builder&
MFFunctionRegistry::Builder::setDefaultFormatterNameByType(const UnicodeString& type,
                                                           const FunctionName& functionName,
                                                           UErrorCode& errorCode) {
    if (U_SUCCESS(errorCode)) {
        U_ASSERT(formattersByType != nullptr);
        FunctionName* f = create<FunctionName>(FunctionName(functionName), errorCode);
        formattersByType->put(type, f, errorCode);
     }
     return *this;
 }

MFFunctionRegistry::Builder::Builder(UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    functions = new Hashtable();
    formattersByType = new Hashtable();
    if (functions == nullptr || formattersByType == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }

    functions->setValueDeleter(uprv_deleteUObject);
    formattersByType->setValueDeleter(uprv_deleteUObject);
}

MFFunctionRegistry::Builder::~Builder() {
    if (functions != nullptr) {
        delete functions;
        functions = nullptr;
    }
    if (formattersByType != nullptr) {
        delete formattersByType;
        formattersByType = nullptr;
    }
}

// Returns non-owned pointer. Returns pointer rather than reference because it can fail.
// Returns non-const because Function is mutable.
FunctionFactory* MFFunctionRegistry::getFunction(const FunctionName& functionName) const {
    U_ASSERT(functions != nullptr);
    return static_cast<FunctionFactory*>(functions->get(functionName));
}

UBool MFFunctionRegistry::getDefaultFormatterNameByType(const UnicodeString& type, FunctionName& name) const {
    U_ASSERT(formattersByType != nullptr);
    const FunctionName* f = static_cast<FunctionName*>(formattersByType->get(type));
    if (f != nullptr) {
        name = *f;
        return true;
    }
    return false;
}

bool MFFunctionRegistry::hasFunction(const FunctionName& f) const {
    return getFunction(f) != nullptr;
}

void MFFunctionRegistry::checkFunction(const char* s) const {
#if U_DEBUG
    U_ASSERT(hasFunction(FunctionName(UnicodeString(s))));
#else
   (void) s;
#endif
}

// Debugging
void MFFunctionRegistry::checkStandard() const {
    checkFunction("datetime");
    checkFunction("date");
    checkFunction("time");
    checkFunction("number");
    checkFunction("integer");
    checkFunction("string");
}

// Function/selector helpers

// Converts `s` to a double, indicating failure via `errorCode`
static void strToDouble(const UnicodeString& s, double& result, UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    // Using en-US locale because it happens to correspond to the spec:
    // https://github.com/unicode-org/message-format-wg/blob/main/spec/registry.md#number-operands
    // Ideally, this should re-use the code for parsing number literals (Parser::parseUnquotedLiteral())
    // It's hard to reuse the same code because of how parse errors work.
    // TODO: Refactor
    LocalPointer<NumberFormat> numberFormat(NumberFormat::createInstance(Locale("en-US"), errorCode));
    CHECK_ERROR(errorCode);
    icu::Formattable asNumber;
    numberFormat->parse(s, asNumber, errorCode);
    CHECK_ERROR(errorCode);
    result = asNumber.getDouble(errorCode);
}

static double tryStringAsNumber(const Locale& locale, const Formattable& val, UErrorCode& errorCode) {
    // Check for a string option, try to parse it as a number if present
    UnicodeString tempString = val.getString(errorCode);
    LocalPointer<NumberFormat> numberFormat(NumberFormat::createInstance(locale, errorCode));
    if (U_SUCCESS(errorCode)) {
        icu::Formattable asNumber;
        numberFormat->parse(tempString, asNumber, errorCode);
        if (U_SUCCESS(errorCode)) {
            return asNumber.getDouble(errorCode);
        }
    }
    return 0;
}

static int64_t getInt64Value(const Locale& locale, const Formattable& value, UErrorCode& errorCode) {
    if (U_SUCCESS(errorCode)) {
        if (!value.isNumeric()) {
            double doubleResult = tryStringAsNumber(locale, value, errorCode);
            if (U_SUCCESS(errorCode)) {
                return static_cast<int64_t>(doubleResult);
            }
        }
        else {
            int64_t result = value.getInt64(errorCode);
            if (U_SUCCESS(errorCode)) {
                return result;
            }
        }
    }
    // Option was numeric but couldn't be converted to int64_t -- could be overflow
    return 0;
}

// Adopts its argument
MFFunctionRegistry::MFFunctionRegistry(FunctionMap* f, Hashtable* byType)
    : functions(f), formattersByType(byType) {
    U_ASSERT(f != nullptr);
    U_ASSERT(byType != nullptr);
}

MFFunctionRegistry& MFFunctionRegistry::operator=(MFFunctionRegistry&& other) noexcept {
    cleanup();

    functions = other.functions;
    other.functions = nullptr;
    formattersByType = other.formattersByType;
    other.formattersByType = nullptr;

    return *this;
}

void MFFunctionRegistry::cleanup() noexcept {
    if (functions != nullptr) {
        delete functions;
        functions = nullptr;
    }
    if (formattersByType != nullptr) {
        delete formattersByType;
        formattersByType = nullptr;
    }
}


MFFunctionRegistry::~MFFunctionRegistry() {
    cleanup();
}

// Specific function implementations

// --------- Number

/* static */ StandardFunctions::NumberFactory*
StandardFunctions::NumberFactory::integer(UErrorCode& success) {
    return NumberFactory::create(true, success);
}

/* static */ StandardFunctions::NumberFactory*
StandardFunctions::NumberFactory::number(UErrorCode& success) {
    return NumberFactory::create(false, success);
}

/* static */ StandardFunctions::NumberFactory*
StandardFunctions::NumberFactory::create(bool isInteger,
                                         UErrorCode& success) {
    NULL_ON_ERROR(success);

    LocalPointer<NumberFactory> result(new NumberFactory(isInteger));
    if (!result.isValid()) {
        success = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return result.orphan();
}

Function*
StandardFunctions::NumberFactory::createFunction(const Locale& locale, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    Number* result = new Number(locale, isInteger);
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

/* static */ StandardFunctions::Number*
StandardFunctions::Number::integer(const Locale& loc, UErrorCode& success) {
    return create(loc, true, success);
}

/* static */ StandardFunctions::Number*
StandardFunctions::Number::number(const Locale& loc, UErrorCode& success) {
    return create(loc, false, success);
}

/* static */ StandardFunctions::Number*
StandardFunctions::Number::create(const Locale& loc, bool isInteger, UErrorCode& success) {
    NULL_ON_ERROR(success);

    LocalPointer<Number> result(new Number(loc, isInteger));
    if (!result.isValid()) {
        success = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return result.orphan();
}

FunctionValue* StandardFunctions::Number::call(FunctionValue& operand,
                                               FunctionOptions&& options,
                                               UErrorCode& errorCode) {
    LocalPointer<NumberValue>
        val(new NumberValue(*this, operand, std::move(options), errorCode));
    if (val.isValid()) {
        return val.orphan();
    }
    errorCode = U_MEMORY_ALLOCATION_ERROR;
    return nullptr;
}

/* static */ number::LocalizedNumberFormatter StandardFunctions::formatterForOptions(const Number& number,
                                                                                     const FunctionOptions& opts,
                                                                                     UErrorCode& status) {
    number::UnlocalizedNumberFormatter nf;

    using namespace number;

    if (U_SUCCESS(status)) {
        Formattable opt;
        nf = NumberFormatter::with();
        bool isInteger = number.isInteger;

        if (isInteger) {
            nf = nf.precision(Precision::integer());
        }

        // Notation options
        if (!isInteger) {
            // These options only apply to `:number`

            // Default notation is simple
            Notation notation = Notation::simple();
            UnicodeString notationOpt = opts.getStringFunctionOption(UnicodeString("notation"));
            if (notationOpt == UnicodeString("scientific")) {
                notation = Notation::scientific();
            } else if (notationOpt == UnicodeString("engineering")) {
                notation = Notation::engineering();
            } else if (notationOpt == UnicodeString("compact")) {
                UnicodeString displayOpt = opts.getStringFunctionOption(UnicodeString("compactDisplay"));
                if (displayOpt == UnicodeString("long")) {
                    notation = Notation::compactLong();
                } else {
                    // Default is short
                    notation = Notation::compactShort();
                }
            } else {
                // Already set to default
            }
            nf = nf.notation(notation);
        }

        // Style options -- specific to `:number`
        if (!isInteger) {
            if (number.usePercent(opts)) {
                nf = nf.unit(NoUnit::percent()).scale(Scale::powerOfTen(2));
            }
        }

        int32_t maxSignificantDigits = number.maximumSignificantDigits(opts);
        if (!isInteger) {
            int32_t minFractionDigits = number.minimumFractionDigits(opts);
            int32_t maxFractionDigits = number.maximumFractionDigits(opts);
            int32_t minSignificantDigits = number.minimumSignificantDigits(opts);
            Precision p = Precision::unlimited();
            bool precisionOptions = false;

            // Returning -1 means the option wasn't provided
            if (maxFractionDigits != -1 && minFractionDigits != -1) {
                precisionOptions = true;
                p = Precision::minMaxFraction(minFractionDigits, maxFractionDigits);
            } else if (minFractionDigits != -1) {
                precisionOptions = true;
                p = Precision::minFraction(minFractionDigits);
            } else if (maxFractionDigits != -1) {
                precisionOptions = true;
                p = Precision::maxFraction(maxFractionDigits);
            }

            if (minSignificantDigits != -1) {
                precisionOptions = true;
                p = p.minSignificantDigits(minSignificantDigits);
            }
            if (maxSignificantDigits != -1) {
                precisionOptions = true;
                p = p.maxSignificantDigits(maxSignificantDigits);
            }
            if (precisionOptions) {
                nf = nf.precision(p);
            }
        } else {
            // maxSignificantDigits applies to `:integer`, but the other precision options don't
            Precision p = Precision::integer();
            if (maxSignificantDigits != -1) {
                p = p.maxSignificantDigits(maxSignificantDigits);
            }
            nf = nf.precision(p);
        }

        // All other options apply to both `:number` and `:integer`
        int32_t minIntegerDigits = number.minimumIntegerDigits(opts);
        if (minIntegerDigits != -1) {
            nf = nf.integerWidth(IntegerWidth::zeroFillTo(minIntegerDigits));
        }

        // signDisplay
        UnicodeString sd = opts.getStringFunctionOption(UnicodeString("signDisplay"));
        UNumberSignDisplay signDisplay;
        if (sd == UnicodeString("always")) {
            signDisplay = UNumberSignDisplay::UNUM_SIGN_ALWAYS;
        } else if (sd == UnicodeString("exceptZero")) {
            signDisplay = UNumberSignDisplay::UNUM_SIGN_EXCEPT_ZERO;
        } else if (sd == UnicodeString("negative")) {
            signDisplay = UNumberSignDisplay::UNUM_SIGN_NEGATIVE;
        } else if (sd == UnicodeString("never")) {
            signDisplay = UNumberSignDisplay::UNUM_SIGN_NEVER;
        } else {
            signDisplay = UNumberSignDisplay::UNUM_SIGN_AUTO;
        }
        nf = nf.sign(signDisplay);

        // useGrouping
        UnicodeString ug = opts.getStringFunctionOption(UnicodeString("useGrouping"));
        UNumberGroupingStrategy grp;
        if (ug == UnicodeString("always")) {
            grp = UNumberGroupingStrategy::UNUM_GROUPING_ON_ALIGNED;
        } else if (ug == UnicodeString("never")) {
            grp = UNumberGroupingStrategy::UNUM_GROUPING_OFF;
        } else if (ug == UnicodeString("min2")) {
            grp = UNumberGroupingStrategy::UNUM_GROUPING_MIN2;
        } else {
            // Default is "auto"
            grp = UNumberGroupingStrategy::UNUM_GROUPING_AUTO;
        }
        nf = nf.grouping(grp);

        // numberingSystem
        UnicodeString ns = opts.getStringFunctionOption(UnicodeString("numberingSystem"));
        if (ns.length() > 0) {
            ns = ns.toLower(Locale("en-US"));
            CharString buffer;
            // Ignore bad option values, so use a local status
            UErrorCode localStatus = U_ZERO_ERROR;
            // Copied from number_skeletons.cpp (helpers::parseNumberingSystemOption)
            buffer.appendInvariantChars({false, ns.getBuffer(), ns.length()}, localStatus);
            if (U_SUCCESS(localStatus)) {
                LocalPointer<NumberingSystem> symbols
                    (NumberingSystem::createInstanceByName(buffer.data(), localStatus));
                if (U_SUCCESS(localStatus)) {
                    nf = nf.adoptSymbols(symbols.orphan());
                }
            }
        }
    }
    return nf.locale(number.locale);
}

static double parseNumberLiteral(const UnicodeString& inputStr, UErrorCode& errorCode) {
    if (U_FAILURE(errorCode)) {
        return {};
    }

    // Hack: Check for cases that are forbidden by the MF2 grammar
    // but allowed by StringToDouble
    int32_t len = inputStr.length();

    if (len > 0 && ((inputStr[0] == '+')
                    || (inputStr[0] == '0' && len > 1 && inputStr[1] != '.')
                    || (inputStr[len - 1] == '.')
                    || (inputStr[0] == '.'))) {
        errorCode = U_MF_OPERAND_MISMATCH_ERROR;
        return 0;
    }

    // Otherwise, convert to double using double_conversion::StringToDoubleConverter
    using namespace double_conversion;
    int processedCharactersCount = 0;
    StringToDoubleConverter converter(0, 0, 0, "", "");
    double result =
        converter.StringToDouble(reinterpret_cast<const uint16_t*>(inputStr.getBuffer()),
                                 len,
                                 &processedCharactersCount);
    if (processedCharactersCount != len) {
        errorCode = U_MF_OPERAND_MISMATCH_ERROR;
    }
    return result;
}

static number::FormattedNumber tryParsingNumberLiteral(const number::LocalizedNumberFormatter& nf,
                                                       const UnicodeString& input,
                                                       UErrorCode& errorCode) {
    double numberValue = parseNumberLiteral(input, errorCode);
    if (U_FAILURE(errorCode)) {
        return {};
    }

    UErrorCode savedStatus = errorCode;
    number::FormattedNumber result = nf.formatDouble(numberValue, errorCode);
    // Ignore U_USING_DEFAULT_WARNING
    if (errorCode == U_USING_DEFAULT_WARNING) {
        errorCode = savedStatus;
    }
    return result;
}

int32_t StandardFunctions::Number::digitSizeOption(const FunctionOptions& opts,
                                                   const UnicodeString& k) const {
    UErrorCode localStatus = U_ZERO_ERROR;
    const FunctionValue* opt = opts.getFunctionOption(k,
                                                      localStatus);
    if (U_SUCCESS(localStatus)) {
        // First try the formatted value
        UnicodeString formatted = opt->formatToString(localStatus);
        int64_t val = 0;
        if (U_SUCCESS(localStatus)) {
            val = getInt64Value(locale, Formattable(formatted), localStatus);
        }
        if (U_FAILURE(localStatus)) {
            localStatus = U_ZERO_ERROR;
        }
        // Next try the operand
        val = getInt64Value(locale, opt->getOperand(), localStatus);
        if (U_SUCCESS(localStatus)) {
            return static_cast<int32_t>(val);
        }
    }
    // Returning -1 indicates that the option wasn't provided or was a non-integer.
    // The caller needs to check for that case, since passing -1 to Precision::maxFraction()
    // is an error.
    return -1;
}

int32_t StandardFunctions::Number::maximumFractionDigits(const FunctionOptions& opts) const {
    if (isInteger) {
        return 0;
    }

    return digitSizeOption(opts, UnicodeString("maximumFractionDigits"));
}

int32_t StandardFunctions::Number::minimumFractionDigits(const FunctionOptions& opts) const {
    Formattable opt;

    if (isInteger) {
        return -1;
    }
    return digitSizeOption(opts, UnicodeString("minimumFractionDigits"));
}

int32_t StandardFunctions::Number::minimumIntegerDigits(const FunctionOptions& opts) const {
    return digitSizeOption(opts, UnicodeString("minimumIntegerDigits"));
}

int32_t StandardFunctions::Number::minimumSignificantDigits(const FunctionOptions& opts) const {
    if (isInteger) {
        return -1;
    }
    return digitSizeOption(opts, UnicodeString("minimumSignificantDigits"));
}

int32_t StandardFunctions::Number::maximumSignificantDigits(const FunctionOptions& opts) const {
    return digitSizeOption(opts, UnicodeString("maximumSignificantDigits"));
}

bool StandardFunctions::Number::usePercent(const FunctionOptions& opts) const {
    const UnicodeString& style = opts.getStringFunctionOption(UnicodeString("style"));
    if (isInteger || style.length() == 0) {
        return false;
    }
    return (style == UnicodeString("percent"));
}

StandardFunctions::NumberValue::NumberValue(const Number& parent,
                                            FunctionValue& arg,
                                            FunctionOptions&& options,
                                            UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);
    // Must have an argument
    if (arg.isNullOperand()) {
        errorCode = U_MF_OPERAND_MISMATCH_ERROR;
        return;
    }

    locale = parent.locale;
    opts = options.mergeOptions(arg.getResolvedOptions(), errorCode);
    operand = arg.getOperand();

    number::LocalizedNumberFormatter realFormatter;
    realFormatter = formatterForOptions(parent, opts, errorCode);

    if (U_SUCCESS(errorCode)) {
        switch (operand.getType()) {
        case UFMT_DOUBLE: {
            double d = operand.getDouble(errorCode);
            U_ASSERT(U_SUCCESS(errorCode));
            formattedNumber = realFormatter.formatDouble(d, errorCode);
            break;
        }
        case UFMT_LONG: {
            int32_t l = operand.getLong(errorCode);
            U_ASSERT(U_SUCCESS(errorCode));
            formattedNumber = realFormatter.formatInt(l, errorCode);
            break;
        }
        case UFMT_INT64: {
            int64_t i = operand.getInt64(errorCode);
            U_ASSERT(U_SUCCESS(errorCode));
            formattedNumber = realFormatter.formatInt(i, errorCode);
            break;
        }
        case UFMT_STRING: {
            // Try to parse the string as a number
            formattedNumber = tryParsingNumberLiteral(realFormatter,
                                                   operand.getString(errorCode),
                                                   errorCode);
            break;
        }
        default: {
            // Other types can't be parsed as a number
            errorCode = U_MF_OPERAND_MISMATCH_ERROR;
            break;
        }
        }
    }
}

UnicodeString StandardFunctions::NumberValue::formatToString(UErrorCode& errorCode) const {
    if (U_FAILURE(errorCode)) {
        return {};
    }

    return formattedNumber.toString(errorCode);
}

StandardFunctions::NumberFactory::~NumberFactory() {}
StandardFunctions::Number::~Number() {}
StandardFunctions::NumberValue::~NumberValue() {}

/* static */ StandardFunctions::Number::PluralType
StandardFunctions::Number::pluralType(const FunctionOptions& opts) {
    const UnicodeString& select = opts.getStringFunctionOption(UnicodeString("select"));

    if (select.length() > 0) {
        if (select == UnicodeString("ordinal")) {
            return PluralType::PLURAL_ORDINAL;
        }
        if (select == UnicodeString("exact")) {
            return PluralType::PLURAL_EXACT;
        }
    }
    return PluralType::PLURAL_CARDINAL;
}

void StandardFunctions::NumberValue::selectKeys(const UnicodeString* keys,
                                                int32_t keysLen,
                                                UnicodeString* prefs,
                                                int32_t& prefsLen,
                                                UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    Number::PluralType type = Number::pluralType(opts);

    // (resolvedSelector is `this`)

    // See  https://github.com/unicode-org/message-format-wg/blob/main/spec/registry.md#number-selection
    // 1. Let exact be the JSON string representation of the numeric value of resolvedSelector
    UnicodeString exact = formattedNumber.toString(errorCode);

    if (U_FAILURE(errorCode)) {
        // Non-number => selector error
        errorCode = U_MF_SELECTOR_ERROR;
        return;
    }

    // Step 2. Let keyword be a string which is the result of rule selection on resolvedSelector.
    // If the option select is set to exact, rule-based selection is not used. Return the empty string.
    UnicodeString keyword;
    if (type != Number::PluralType::PLURAL_EXACT) {
        UPluralType t = type == Number::PluralType::PLURAL_ORDINAL ? UPLURAL_TYPE_ORDINAL : UPLURAL_TYPE_CARDINAL;
        // Look up plural rules by locale and type
        LocalPointer<PluralRules> rules(PluralRules::forLocale(locale, t, errorCode));
        CHECK_ERROR(errorCode);

        keyword = rules->select(formattedNumber, errorCode);
    }

    // Steps 3-4 elided:
    // 3. Let resultExact be a new empty list of strings.
    // 4. Let resultKeyword be a new empty list of strings.
    // Instead, we use `prefs` the concatenation of `resultExact`
    // and `resultKeyword`.

    prefsLen = 0;

    // 5. For each string key in keys:
    double keyAsDouble = 0;
    for (int32_t i = 0; i < keysLen; i++) {
        // Try parsing the key as a double
        UErrorCode localErrorCode = U_ZERO_ERROR;
        strToDouble(keys[i], keyAsDouble, localErrorCode);
        // 5i. If the value of key matches the production number-literal, then
        if (U_SUCCESS(localErrorCode)) {
            // 5i(a). If key and exact consist of the same sequence of Unicode code points, then
            if (exact == keys[i]) {
                // 5i(a)(a) Append key as the last element of the list resultExact.
		prefs[prefsLen] = keys[i];
                prefsLen++;
                break;
            }
        }
    }

    // Return immediately if exact matching was requested
    if (prefsLen == keysLen || type == Number::PluralType::PLURAL_EXACT) {
        return;
    }


    for (int32_t i = 0; i < keysLen; i ++) {
        if (prefsLen >= keysLen) {
            break;
        }
        // 5ii. Else if key is one of the keywords zero, one, two, few, many, or other, then
        // 5ii(a). If key and keyword consist of the same sequence of Unicode code points, then
        if (keyword == keys[i]) {
            // 5ii(a)(a) Append key as the last element of the list resultKeyword.
            prefs[prefsLen] = keys[i];
            prefsLen++;
        }
    }

    // Note: Step 5(iii) "Else, emit a Selection Error" is omitted in both loops

    // 6. Return a new list whose elements are the concatenation of the elements
    // (in order) of resultExact followed by the elements (in order) of resultKeyword.
    // (Implicit, since `prefs` is an out-parameter)
}

// --------- DateTime

/*
// Date/time options only
static UnicodeString defaultForOption(const UnicodeString& optionName) {
    if (optionName == UnicodeString("dateStyle")
        || optionName == UnicodeString("timeStyle")
        || optionName == UnicodeString("style")) {
        return UnicodeString("short");
    }
    return {}; // Empty string is default
}
*/

/* static */ StandardFunctions::DateTimeFactory*
StandardFunctions::DateTimeFactory::date(UErrorCode& success) {
    return DateTimeFactory::create(DateTimeType::kDate, success);
}

/* static */ StandardFunctions::DateTimeFactory*
StandardFunctions::DateTimeFactory::time(UErrorCode& success) {
    return DateTimeFactory::create(DateTimeType::kTime, success);
}

/* static */ StandardFunctions::DateTimeFactory*
StandardFunctions::DateTimeFactory::dateTime(UErrorCode& success) {
    return DateTimeFactory::create(DateTimeType::kDateTime, success);
}

/* static */ StandardFunctions::DateTimeFactory*
StandardFunctions::DateTimeFactory::create(DateTimeFactory::DateTimeType type,
                                           UErrorCode& success) {
    NULL_ON_ERROR(success);

    LocalPointer<DateTimeFactory> result(new DateTimeFactory(type));
    if (!result.isValid()) {
        success = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return result.orphan();
}

Function*
StandardFunctions::DateTimeFactory::createFunction(const Locale& locale, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    DateTime* result = new DateTime(locale, type);
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

/* static */ StandardFunctions::DateTime*
StandardFunctions::DateTime::create(const Locale& loc,
                                    DateTimeFactory::DateTimeType type,
                                    UErrorCode& success) {
    NULL_ON_ERROR(success);

    LocalPointer<DateTime> result(new DateTime(loc, type));
    if (!result.isValid()) {
        success = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return result.orphan();
}

FunctionValue*
StandardFunctions::DateTime::call(FunctionValue& val, FunctionOptions&& opts, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    auto result = new DateTimeValue(locale, type, val, std::move(opts), errorCode);
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

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

UnicodeString StandardFunctions::DateTimeValue::formatToString(UErrorCode& status) const {
    (void) status;

    return formattedDate;
}

StandardFunctions::DateTimeValue::DateTimeValue(const Locale& locale,
                                                DateTimeFactory::DateTimeType type,
                                                FunctionValue& val,
                                                FunctionOptions&& options,
                                                UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    // Must have an argument
    if (val.isNullOperand()) {
        errorCode = U_MF_OPERAND_MISMATCH_ERROR;
        return;
    }

    operand = val.getOperand();
    opts = options.mergeOptions(val.getResolvedOptions(), errorCode);

    const Formattable* source = &operand;

    LocalPointer<DateFormat> df;
    Formattable opt;

    DateFormat::EStyle dateStyle = DateFormat::kShort;
    DateFormat::EStyle timeStyle = DateFormat::kShort;

    UnicodeString dateStyleName("dateStyle");
    UnicodeString timeStyleName("timeStyle");
    UnicodeString styleName("style");

    UnicodeString dateStyleOption = opts.getStringFunctionOption(dateStyleName);
    UnicodeString timeStyleOption = opts.getStringFunctionOption(timeStyleName);
    bool hasDateStyleOption = dateStyleOption.length() > 0;
    bool hasTimeStyleOption = dateStyleOption.length() > 0;
    bool noOptions = opts.optionsCount() == 0;

    using DateTimeType = DateTimeFactory::DateTimeType;

    bool useStyle = (type == DateTimeType::kDateTime
                     && (hasDateStyleOption || hasTimeStyleOption
                         || noOptions))
        || (type != DateTimeType::kDateTime);

    bool useDate = type == DateTimeType::kDate
        || (type == DateTimeType::kDateTime
            && hasDateStyleOption);
    bool useTime = type == DateTimeType::kTime
        || (type == DateTimeType::kDateTime
            && hasTimeStyleOption);

    if (useStyle) {
        // Extract style options
        if (type == DateTimeType::kDateTime) {
            // Note that the options-getting has to be repeated across the three cases,
            // since `:datetime` uses "dateStyle"/"timeStyle" and `:date` and `:time`
            // use "style"
            dateStyle = stringToStyle(opts.getStringFunctionOption(dateStyleName), errorCode);
            timeStyle = stringToStyle(opts.getStringFunctionOption(timeStyleName), errorCode);

            if (useDate && !useTime) {
                df.adoptInstead(DateFormat::createDateInstance(dateStyle, locale));
            } else if (useTime && !useDate) {
                df.adoptInstead(DateFormat::createTimeInstance(timeStyle, locale));
            } else {
                df.adoptInstead(DateFormat::createDateTimeInstance(dateStyle, timeStyle, locale));
            }
        } else if (type == DateTimeType::kDate) {
            dateStyle = stringToStyle(opts.getStringFunctionOption(styleName), errorCode);
            df.adoptInstead(DateFormat::createDateInstance(dateStyle, locale));
        } else {
            // :time
            timeStyle = stringToStyle(opts.getStringFunctionOption(styleName), errorCode);
            df.adoptInstead(DateFormat::createTimeInstance(timeStyle, locale));
        }
    } else {
        // Build up a skeleton based on the field options, then use that to
        // create the date formatter

        UnicodeString skeleton;
        #define ADD_PATTERN(s) skeleton += UnicodeString(s)
        if (U_SUCCESS(errorCode)) {
            // Year
            UnicodeString year = opts.getStringFunctionOption(UnicodeString("year"), errorCode);
            if (U_FAILURE(errorCode)) {
                errorCode = U_ZERO_ERROR;
            } else {
                useDate = true;
                if (year == UnicodeString("2-digit")) {
                    ADD_PATTERN("YY");
                } else if (year == UnicodeString("numeric")) {
                    ADD_PATTERN("YYYY");
                }
            }
            // Month
            UnicodeString month = opts.getStringFunctionOption(UnicodeString("month"), errorCode);
            if (U_FAILURE(errorCode)) {
                errorCode = U_ZERO_ERROR;
            } else {
                useDate = true;
                /* numeric, 2-digit, long, short, narrow */
                if (month == UnicodeString("long")) {
                    ADD_PATTERN("MMMM");
                } else if (month == UnicodeString("short")) {
                    ADD_PATTERN("MMM");
                } else if (month == UnicodeString("narrow")) {
                    ADD_PATTERN("MMMMM");
                } else if (month == UnicodeString("numeric")) {
                    ADD_PATTERN("M");
                } else if (month == UnicodeString("2-digit")) {
                    ADD_PATTERN("MM");
                }
            }
            // Weekday
            UnicodeString weekday = opts.getStringFunctionOption(UnicodeString("weekday"), errorCode);
            if (U_FAILURE(errorCode)) {
                errorCode = U_ZERO_ERROR;
            } else {
                useDate = true;
                if (weekday == UnicodeString("long")) {
                    ADD_PATTERN("EEEE");
                } else if (weekday == UnicodeString("short")) {
                    ADD_PATTERN("EEEEE");
                } else if (weekday == UnicodeString("narrow")) {
                    ADD_PATTERN("EEEEE");
                }
            }
            // Day
            UnicodeString day = opts.getStringFunctionOption(UnicodeString("day"), errorCode);
            if (U_FAILURE(errorCode)) {
                errorCode = U_ZERO_ERROR;
            } else {
                useDate = true;
                if (day == UnicodeString("numeric")) {
                    ADD_PATTERN("d");
                } else if (day == UnicodeString("2-digit")) {
                    ADD_PATTERN("dd");
                }
            }
            // Hour
            UnicodeString hour = opts.getStringFunctionOption(UnicodeString("hour"), errorCode);
            if (U_FAILURE(errorCode)) {
                errorCode = U_ZERO_ERROR;
            } else {
                useTime = true;
                if (hour == UnicodeString("numeric")) {
                    ADD_PATTERN("h");
                } else if (hour == UnicodeString("2-digit")) {
                    ADD_PATTERN("hh");
                }
            }
            // Minute
            UnicodeString minute = opts.getStringFunctionOption(UnicodeString("minute"), errorCode);
            if (U_FAILURE(errorCode)) {
                errorCode = U_ZERO_ERROR;
            } else {
                useTime = true;
                if (minute == UnicodeString("numeric")) {
                    ADD_PATTERN("m");
                } else if (minute == UnicodeString("2-digit")) {
                    ADD_PATTERN("mm");
                }
            }
            // Second
            UnicodeString second = opts.getStringFunctionOption(UnicodeString("second"), errorCode);
            if (U_FAILURE(errorCode)) {
                errorCode = U_ZERO_ERROR;
            } else {
                useTime = true;
                if (second == UnicodeString("numeric")) {
                    ADD_PATTERN("s");
                } else if (second == UnicodeString("2-digit")) {
                    ADD_PATTERN("ss");
                }
            }
        }
        /*
          TODO
          fractionalSecondDigits
          hourCycle
          timeZoneName
          era
         */
        df.adoptInstead(DateFormat::createInstanceForSkeleton(skeleton, errorCode));
    }

    if (U_FAILURE(errorCode)) {
        return;
    }
    if (!df.isValid()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return;
    }

    UnicodeString result;
    switch (source->getType()) {
    case UFMT_STRING: {
        const UnicodeString& sourceStr = source->getString(errorCode);
        U_ASSERT(U_SUCCESS(errorCode));
        // Pattern for ISO 8601 format - datetime
        UnicodeString pattern("YYYY-MM-dd'T'HH:mm:ss");
        LocalPointer<DateFormat> dateParser(new SimpleDateFormat(pattern, errorCode));
        if (U_FAILURE(errorCode)) {
            errorCode = U_MF_FORMATTING_ERROR;
        } else {
            // Parse the date
            UDate d = dateParser->parse(sourceStr, errorCode);
            if (U_FAILURE(errorCode)) {
                // Pattern for ISO 8601 format - date
                UnicodeString pattern("YYYY-MM-dd");
                errorCode = U_ZERO_ERROR;
                dateParser.adoptInstead(new SimpleDateFormat(pattern, errorCode));
                if (U_FAILURE(errorCode)) {
                    errorCode = U_MF_FORMATTING_ERROR;
                } else {
                    d = dateParser->parse(sourceStr, errorCode);
                    if (U_FAILURE(errorCode)) {
                        errorCode = U_MF_OPERAND_MISMATCH_ERROR;
                    }
                }
            }
            // Use the parsed date as the source value
            // in the returned FormattedPlaceholder; this is necessary
            // so the date can be re-formatted
            operand = message2::Formattable::forDate(d);
            df->format(d, result, 0, errorCode);
        }
        break;
    }
    case UFMT_DATE: {
        df->format(source->asICUFormattable(errorCode), result, 0, errorCode);
        if (U_FAILURE(errorCode)) {
            if (errorCode == U_ILLEGAL_ARGUMENT_ERROR) {
                errorCode = U_MF_OPERAND_MISMATCH_ERROR;
            }
        }
        break;
    }
    // Any other cases are an error
    default: {
        errorCode = U_MF_OPERAND_MISMATCH_ERROR;
        break;
    }
    }
    if (U_FAILURE(errorCode)) {
        return;
    }
    // Ignore U_USING_DEFAULT_WARNING
    if (errorCode == U_USING_DEFAULT_WARNING) {
        errorCode = U_ZERO_ERROR;
    }
    formattedDate = result;
}

StandardFunctions::DateTimeFactory::~DateTimeFactory() {}
StandardFunctions::DateTime::~DateTime() {}
StandardFunctions::DateTimeValue::~DateTimeValue() {}

// --------- String

/* static */ StandardFunctions::StringFactory*
StandardFunctions::StringFactory::string(UErrorCode& success) {
    NULL_ON_ERROR(success);

    LocalPointer<StringFactory> result(new StringFactory());
    if (!result.isValid()) {
        success = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return result.orphan();
}

/* static */ StandardFunctions::String*
StandardFunctions::String::string(const Locale& loc, UErrorCode& success) {
    NULL_ON_ERROR(success);

    LocalPointer<String> result(new String(loc));
    if (!result.isValid()) {
        success = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return result.orphan();
}

Function*
StandardFunctions::StringFactory::createFunction(const Locale& locale, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    String* result = new String(locale);
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

extern UnicodeString formattableToString(const Locale&, const Formattable&, UErrorCode&);

FunctionValue*
StandardFunctions::String::call(FunctionValue& val, FunctionOptions&& opts, UErrorCode& errorCode) {
    return new StringValue(locale, val, std::move(opts), errorCode);
}

UnicodeString StandardFunctions::StringValue::formatToString(UErrorCode& errorCode) const {
    (void) errorCode;

    return formattedString;
}

StandardFunctions::StringValue::StringValue(const Locale& locale,
                                            FunctionValue& val,
                                            FunctionOptions&& options,
                                            UErrorCode& status) {
    CHECK_ERROR(status);
    operand = val.getOperand();
    opts = std::move(options); // No options
    // Convert to string
    formattedString = formattableToString(locale, operand, status);
}

void StandardFunctions::StringValue::selectKeys(const UnicodeString* keys,
                                                int32_t keysLen,
                                                UnicodeString* prefs,
                                                int32_t& prefsLen,
                                                UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    // Just compares the key and value as strings

    prefsLen = 0;

    if (U_FAILURE(errorCode)) {
        return;
    }

    for (int32_t i = 0; i < keysLen; i++) {
        if (keys[i] == formattedString) {
	    prefs[0] = keys[i];
            prefsLen = 1;
            break;
        }
    }
}

StandardFunctions::StringFactory::~StringFactory() {}
StandardFunctions::String::~String() {}
StandardFunctions::StringValue::~StringValue() {}

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_MF2 */

#endif /* #if !UCONFIG_NO_FORMATTING */

