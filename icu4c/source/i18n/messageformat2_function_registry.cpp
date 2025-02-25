// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_NORMALIZATION

#if !UCONFIG_NO_FORMATTING

#if !UCONFIG_NO_MF2

#include <math.h>
#include <cmath>

#include "unicode/dtptngen.h"
#include "unicode/messageformat2.h"
#include "unicode/messageformat2_data_model_names.h"
#include "unicode/messageformat2_function_registry.h"
#include "unicode/normalizer2.h"
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
MFFunctionRegistry::Builder::adoptFunction(const FunctionName& functionName,
                                           Function* function,
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
Function* MFFunctionRegistry::getFunction(const FunctionName& functionName) const {
    U_ASSERT(functions != nullptr);
    return static_cast<Function*>(functions->get(functionName));
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
    checkFunction("test:function");
    checkFunction("test:format");
    checkFunction("test:select");
}

// Function/selector helpers

// Returns the NFC-normalized version of s, returning s itself
// if it's already normalized.
/* static */ UnicodeString StandardFunctions::normalizeNFC(const UnicodeString& s) {
    UErrorCode status = U_ZERO_ERROR;
    const Normalizer2* nfcNormalizer = Normalizer2::getNFCInstance(status);
    if (U_FAILURE(status)) {
        return s;
    }
    // Check if string is already normalized
    UNormalizationCheckResult result = nfcNormalizer->quickCheck(s, status);
    // If so, return it
    if (U_SUCCESS(status) && result == UNORM_YES) {
        return s;
    }
    // Otherwise, normalize it
    UnicodeString normalized = nfcNormalizer->normalize(s, status);
    if (U_FAILURE(status)) {
        return {};
    }
    return normalized;
}

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

/* static */ StandardFunctions::Number*
StandardFunctions::Number::integer(UErrorCode& success) {
    return create(true, success);
}

/* static */ StandardFunctions::Number*
StandardFunctions::Number::number(UErrorCode& success) {
    return create(false, success);
}

/* static */ StandardFunctions::Number*
StandardFunctions::Number::create(bool isInteger, UErrorCode& success) {
    NULL_ON_ERROR(success);

    LocalPointer<Number> result(new Number(isInteger));
    if (!result.isValid()) {
        success = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return result.orphan();
}

LocalPointer<FunctionValue> StandardFunctions::Number::call(const FunctionContext& context,
                                                            const FunctionValue& operand,
                                                            const FunctionOptions& options,
                                                            UErrorCode& errorCode) {
    if (U_FAILURE(errorCode)) {
        return LocalPointer<FunctionValue>();
    }
    LocalPointer<FunctionValue>
        val(new NumberValue(*this, context, operand, options, errorCode));
    if (!val.isValid()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return val;
}

/* static */ number::LocalizedNumberFormatter StandardFunctions::formatterForOptions(const Number& number,
                                                                                     const Locale& locale,
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
    return nf.locale(locale);
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

static UChar32 digitToChar(int32_t val, UErrorCode errorCode) {
    if (U_FAILURE(errorCode)) {
        return '0';
    }
    if (val < 0 || val > 9) {
        errorCode = U_ILLEGAL_ARGUMENT_ERROR;
    }
    switch(val) {
        case 0:
            return '0';
        case 1:
            return '1';
        case 2:
            return '2';
        case 3:
            return '3';
        case 4:
            return '4';
        case 5:
            return '5';
        case 6:
            return '6';
        case 7:
            return '7';
        case 8:
            return '8';
        case 9:
            return '9';
        default:
            errorCode = U_ILLEGAL_ARGUMENT_ERROR;
            return '0';
    }
    return '0';
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
            val = getInt64Value(Locale("en-US"), Formattable(formatted), localStatus);
        }
        if (U_FAILURE(localStatus)) {
            localStatus = U_ZERO_ERROR;
        }
        // Next try the operand
        val = getInt64Value(Locale("en-US"), opt->getOperand(), localStatus);
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
                                            const FunctionContext& context,
                                            const FunctionValue& arg,
                                            const FunctionOptions& options,
                                            UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);
    // Must have an argument
    if (arg.isNullOperand()) {
        errorCode = U_MF_OPERAND_MISMATCH_ERROR;
        return;
    }

    locale = context.getLocale();
    opts = options.mergeOptions(arg.getResolvedOptions(), errorCode);
    operand = arg.getOperand();
    functionName = UnicodeString(parent.isInteger ? "integer" : "number");

    number::LocalizedNumberFormatter realFormatter;
    realFormatter = formatterForOptions(parent, locale, opts, errorCode);

    int64_t integerValue = 0;

    if (U_SUCCESS(errorCode)) {
        switch (operand.getType()) {
        case UFMT_DOUBLE: {
            double d = operand.getDouble(errorCode);
            U_ASSERT(U_SUCCESS(errorCode));
            formattedNumber = realFormatter.formatDouble(d, errorCode);
            integerValue = static_cast<int64_t>(std::round(d));
            break;
        }
        case UFMT_LONG: {
            int32_t l = operand.getLong(errorCode);
            U_ASSERT(U_SUCCESS(errorCode));
            formattedNumber = realFormatter.formatInt(l, errorCode);
            integerValue = l;
            break;
        }
        case UFMT_INT64: {
            int64_t i = operand.getInt64(errorCode);
            U_ASSERT(U_SUCCESS(errorCode));
            formattedNumber = realFormatter.formatInt(i, errorCode);
            integerValue = i;
            break;
        }
        case UFMT_STRING: {
            // Try to parse the string as a number
            const UnicodeString& s = operand.getString(errorCode);
            U_ASSERT(U_SUCCESS(errorCode));
            double d = parseNumberLiteral(s, errorCode);
            if (U_FAILURE(errorCode))
                return;
            formattedNumber = realFormatter.formatDouble(d, errorCode);
            integerValue = static_cast<int64_t>(std::round(d));
            break;
        }
        default: {
            // Other types can't be parsed as a number
            errorCode = U_MF_OPERAND_MISMATCH_ERROR;
            break;
        }
        }
    }

    // Need to set the integer value if invoked as :integer
    if (parent.isInteger) {
        operand = Formattable(integerValue);
    }
}

UnicodeString StandardFunctions::NumberValue::formatToString(UErrorCode& errorCode) const {
    if (U_FAILURE(errorCode)) {
        return {};
    }

    return formattedNumber.toString(errorCode);
}

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
                                                int32_t* prefs,
                                                int32_t& prefsLen,
                                                UErrorCode& errorCode) const {
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
		prefs[prefsLen] = i;
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
            prefs[prefsLen] = i;
            prefsLen++;
        }
    }

    // Note: Step 5(iii) "Else, emit a Selection Error" is omitted in both loops

    // 6. Return a new list whose elements are the concatenation of the elements
    // (in order) of resultExact followed by the elements (in order) of resultKeyword.
    // (Implicit, since `prefs` is an out-parameter)
}

// --------- DateTime

/* static */ StandardFunctions::DateTime*
StandardFunctions::DateTime::date(UErrorCode& success) {
    return DateTime::create(DateTimeType::kDate, success);
}

/* static */ StandardFunctions::DateTime*
StandardFunctions::DateTime::time(UErrorCode& success) {
    return DateTime::create(DateTimeType::kTime, success);
}

/* static */ StandardFunctions::DateTime*
StandardFunctions::DateTime::dateTime(UErrorCode& success) {
    return DateTime::create(DateTimeType::kDateTime, success);
}

/* static */ StandardFunctions::DateTime*
StandardFunctions::DateTime::create(DateTime::DateTimeType type,
                                    UErrorCode& success) {
    NULL_ON_ERROR(success);

    LocalPointer<DateTime> result(new DateTime(type));
    if (!result.isValid()) {
        success = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return result.orphan();
}

LocalPointer<FunctionValue>
StandardFunctions::DateTime::call(const FunctionContext& context,
                                  const FunctionValue& val,
                                  const FunctionOptions& opts,
                                  UErrorCode& errorCode) {
    if (U_FAILURE(errorCode)) {
        return LocalPointer<FunctionValue>();
    }
    LocalPointer<FunctionValue>
        result(new DateTimeValue(type, context, val, opts, errorCode));
    if (!result.isValid()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

static DateFormat::EStyle stringToStyle(UnicodeString option, UErrorCode& errorCode) {
    if (U_SUCCESS(errorCode)) {
        UnicodeString upper = option.toUpper();
        if (upper.isEmpty()) {
            return DateFormat::EStyle::kShort;
        }
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
        if (upper == UnicodeString("DEFAULT")) {
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

StandardFunctions::DateTimeValue::DateTimeValue(DateTime::DateTimeType type,
                                                const FunctionContext& context,
                                                const FunctionValue& val,
                                                const FunctionOptions& options,
                                                UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);
    using DateTimeType = DateTime::DateTimeType;

    // Must have an argument
    if (val.isNullOperand()) {
        errorCode = U_MF_OPERAND_MISMATCH_ERROR;
        return;
    }

    const Locale& locale = context.getLocale();
    operand = val.getOperand();
    opts = options.mergeOptions(val.getResolvedOptions(), errorCode);
    switch(type) {
        case DateTimeType::kDateTime:
            functionName = UnicodeString("dateTime");
            break;
        case DateTimeType::kDate:
            functionName = UnicodeString("date");
            break;
        case DateTimeType::kTime:
            functionName = UnicodeString("time");
            break;
    }

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
    bool hasTimeStyleOption = timeStyleOption.length() > 0;
    bool noOptions = opts.optionsCount() == 0;

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

StandardFunctions::DateTime::~DateTime() {}
StandardFunctions::DateTimeValue::~DateTimeValue() {}

// --------- String

/* static */ StandardFunctions::String*
StandardFunctions::String::string(UErrorCode& success) {
    NULL_ON_ERROR(success);

    LocalPointer<String> result(new String());
    if (!result.isValid()) {
        success = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return result.orphan();
}

extern UnicodeString formattableToString(const Locale&,
                                         const UBiDiDirection,
                                         const Formattable&,
                                         UErrorCode&);

LocalPointer<FunctionValue>
StandardFunctions::String::call(const FunctionContext& context,
                                const FunctionValue& val,
                                const FunctionOptions& opts,
                                UErrorCode& errorCode) {
    if (U_FAILURE(errorCode)) {
        return LocalPointer<FunctionValue>();
    }
    LocalPointer<FunctionValue>
        result(new StringValue(context, val, opts, errorCode));
    if (!result.isValid()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

UnicodeString StandardFunctions::StringValue::formatToString(UErrorCode& errorCode) const {
    (void) errorCode;

    return formattedString;
}

StandardFunctions::StringValue::StringValue(const FunctionContext& context,
                                            const FunctionValue& val,
                                            const FunctionOptions&,
                                            UErrorCode& status) {
    CHECK_ERROR(status);
    operand = val.getOperand();
    functionName = UnicodeString("string");
    // No options
    // Convert to string
    formattedString = formattableToString(context.getLocale(), context.getDirection(), operand, status);
}

void StandardFunctions::StringValue::selectKeys(const UnicodeString* keys,
                                                int32_t keysLen,
                                                int32_t* prefs,
                                                int32_t& prefsLen,
                                                UErrorCode& errorCode) const {
    CHECK_ERROR(errorCode);

    // Just compares the key and value as strings

    prefsLen = 0;

    if (U_FAILURE(errorCode)) {
        return;
    }
    // Normalize result
    UnicodeString normalized = normalizeNFC(formattedString);

    for (int32_t i = 0; i < keysLen; i++) {
        if (keys[i] == normalized) {
	    prefs[0] = i;
            prefsLen = 1;
            break;
        }
    }
}

StandardFunctions::String::~String() {}
StandardFunctions::StringValue::~StringValue() {}

// ------------ TestFunction

StandardFunctions::TestFunction::~TestFunction() {}
StandardFunctions::TestFunctionValue::~TestFunctionValue() {}


LocalPointer<FunctionValue> StandardFunctions::TestFunction::call(const FunctionContext& context,
                                                                  const FunctionValue& operand,
                                                                  const FunctionOptions& options,
                                                                  UErrorCode& errorCode) {
    if (U_FAILURE(errorCode)) {
        return LocalPointer<FunctionValue>();
    }
    LocalPointer<FunctionValue>
        val(new TestFunctionValue(*this, context, operand, options, errorCode));
    if (!val.isValid()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return val;
}

// Extract numeric value from a Formattable or, if it's a string,
// parse it as a number according to the MF2 `number-literal` grammar production
double formattableToNumber(const Formattable& arg, UErrorCode& status) {
    if (U_FAILURE(status)) {
        return 0;
    }

    double result = 0;

    switch (arg.getType()) {
        case UFMT_DOUBLE: {
            result = arg.getDouble(status);
            U_ASSERT(U_SUCCESS(status));
            break;
        }
        case UFMT_LONG: {
            result = (double) arg.getLong(status);
            U_ASSERT(U_SUCCESS(status));
            break;
        }
        case UFMT_INT64: {
            result = (double) arg.getInt64(status);
            U_ASSERT(U_SUCCESS(status));
            break;
        }
        case UFMT_STRING: {
            // Try to parse the string as a number
            const UnicodeString& s = arg.getString(status);
            U_ASSERT(U_SUCCESS(status));
            result = parseNumberLiteral(s, status);
            if (U_FAILURE(status)) {
                status = U_MF_OPERAND_MISMATCH_ERROR;
            }
            break;
        }
        default: {
            // Other types can't be parsed as a number
            status = U_MF_OPERAND_MISMATCH_ERROR;
            break;
        }
        }
    return result;
}

static bool isTestFunction(const UnicodeString& s) {
    return (s == u"test:format"
            || s == u"test:select"
            || s == u"test:function");
}

static void setFailsFromFunctionValue(const FunctionValue& optionValue,
                                      bool& failsFormat,
                                      bool& failsSelect,
                                      UErrorCode& status) {
    UnicodeString failsString = optionValue.getOperand().getString(status);
    if (U_SUCCESS(status)) {
        // 9i. If its value resolves to the string 'always', then
        if (failsString == u"always") {
            // 9ia. Set FailsFormat to be true
            failsFormat = true;
            // 9ib. Set FailsSelect to be true.
            failsSelect = true;
        }
        // 9ii. Else if its value resolves to the string "format", then
        else if (failsString == u"format") {
            // 9ia. Set FailsFormat to be true
            failsFormat = true;
        }
        // 9iii. Else if its value resolves to the string "select", then
        else if (failsString == u"select") {
            // 9iiia. Set FailsSelect to be true.
            failsSelect = true;
        }
        // 9iv. Else if its value does not resolve to the string "never", then
        else if (failsString != u"never") {
            // 9iv(a). Emit "bad-option" Resolution Error.
            status = U_MF_BAD_OPTION;
        }
    } else {
        // 9iv. again
        status = U_MF_BAD_OPTION;
    }
}

/* static */ void StandardFunctions::TestFunction::testFunctionParameters(const FunctionValue& arg,
                                                                          const FunctionOptions& options,
                                                                          int32_t& decimalPlaces,
                                                                          bool& failsFormat,
                                                                          bool& failsSelect,
                                                                          double& input,
                                                                          UErrorCode& status) const {
    CHECK_ERROR(status);

    // 1. Let DecimalPlaces be 0.
    decimalPlaces = 0;

    // 2. Let FailsFormat be false.
    failsFormat = false;

    // 3. Let FailsSelect be false.
    failsSelect = false;

    // 4. Let arg be the resolved value of the expression operand.
    // (already true)

    // 5. If arg is the resolved value of an expression with a :test:function, :test:select, or :test:format annotation for which resolution has succeeded, then
    if (isTestFunction(arg.getFunctionName())) {
        // 5i. Let Input be the Input value of arg.
        input = formattableToNumber(arg.getOperand(), status);
        if (U_FAILURE(status)) {
            status = U_MF_OPERAND_MISMATCH_ERROR;
            return;
        }
        const FunctionOptions& opts = arg.getResolvedOptions();
        // 5ii. Set DecimalPlaces to be DecimalPlaces value of arg.
        const FunctionValue* decimalPlacesFunctionValue = opts.getFunctionOption(UnicodeString("decimalPlaces"), status);
        if (U_SUCCESS(status)) {
            decimalPlaces = formattableToNumber(decimalPlacesFunctionValue->getOperand(), status);
            if (U_FAILURE(status)) {
                status = U_MF_OPERAND_MISMATCH_ERROR;
                return;
            }
        } else {
            // Option was not provided -- not an error
            status = U_ZERO_ERROR;
        }
        // 5iii. Set FailsFormat to be FailsFormat value of arg.
        const FunctionValue* failsFormatFunctionValue = opts.getFunctionOption(UnicodeString("fails"), status);
        if (U_SUCCESS(status)) {
            setFailsFromFunctionValue(*failsFormatFunctionValue, failsFormat, failsSelect, status);
            if (U_FAILURE(status)) {
                status = U_MF_BAD_OPTION;
                return;
            }
        } else {
            // Option was not provided -- not an error
            status = U_ZERO_ERROR;
        }
        // 5iv. Set FailsSelect to be FailsSelect value of arg.
        // (Done in previous step)
    } else {
        // 6. Else if arg is a numerical value or a string matching the number-literal production, then
        input = formattableToNumber(arg.getOperand(), status);
        if (U_FAILURE(status)) {
            // 7. Else,
            // 7i. Emit "bad-input" Resolution Error.
            status = U_MF_OPERAND_MISMATCH_ERROR;
            // 7ii. Use a fallback value as the resolved value of the expression.
            // Further steps of this algorithm are not followed.
        }
    }

    const FunctionValue* decimalPlacesOpt = options.getFunctionOption(UnicodeString("decimalPlaces"), status);
    // 8. If the decimalPlaces option is set, then
    if (U_SUCCESS(status)) {
        // 8i. If its value resolves to a numerical integer value 0 or 1
        // or their corresponding string representations '0' or '1', then
        double decimalPlacesInput = formattableToNumber(decimalPlacesOpt->getOperand(), status);
        if (U_SUCCESS(status)) {
            if (decimalPlacesInput == 0 || decimalPlacesInput == 1) {
                // 8ia. Set DecimalPlaces to be the numerical value of the option.
                decimalPlaces = decimalPlacesInput;
            }
        }
        // 8ii. Else if its value is not an unresolved value set by option resolution,
        else {
            // 8iia. Emit "bad-option" Resolution Error.
            status = U_MF_BAD_OPTION;
            // 8iib. Use a fallback value as the resolved value of the expression.
        }
    } else {
        // Option was not provided -- not an error
        status = U_ZERO_ERROR;
    }

    const FunctionValue* failsOpt = options.getFunctionOption(UnicodeString("fails"), status);
    // 9. If the fails option is set, then
    if (U_SUCCESS(status)) {
        setFailsFromFunctionValue(*failsOpt, failsFormat, failsSelect, status);
    } else {
        // Option was not provided -- not an error
        status = U_ZERO_ERROR;
    }
}

StandardFunctions::TestFunctionValue::TestFunctionValue(const TestFunction& parent,
                                                        const FunctionContext&,
                                                        const FunctionValue& arg,
                                                        const FunctionOptions& options,
                                                        UErrorCode& status) {
    parent.testFunctionParameters(arg, options, decimalPlaces,
                                  failsFormat, failsSelect, input, status);
    opts = options.mergeOptions(arg.getResolvedOptions(), status);
    operand = arg.getOperand();
    canFormat = parent.canFormat;
    canSelect = parent.canSelect;
    functionName = UnicodeString(canFormat && canSelect ?
                                 "test:function"
                                 : canFormat ? "test:format"
                                 : "test:select");

    if (U_FAILURE(status)) {
        return;
    }

    // If FailsFormat is true, attempting to format the placeholder to any
    // formatting target will fail.
    if (failsFormat) {
        status = U_MF_FORMATTING_ERROR;
        return;
    }

    // When :test:function is used as a formatter, a placeholder resolving to a value
    // with a :test:function expression is formatted as a concatenation of the following parts:
    // 1. If Input is less than 0, the character - U+002D Hyphen-Minus.
    if (input < 0) {
        formattedString += HYPHEN;
    }
    // 2. The truncated absolute integer value of Input, i.e. floor(abs(Input)), formatted as a
    // sequence of decimal digit characters (U+0030...U+0039).
    char buffer[256];
    bool ignore;
    int ignoreLen;
    int ignorePoint;
    double_conversion::DoubleToStringConverter::DoubleToAscii(floor(abs(input)),
                                                              double_conversion::DoubleToStringConverter::DtoaMode::SHORTEST,
                                                              0,
                                                              buffer,
                                                              256,
                                                              &ignore,
                                                              &ignoreLen,
                                                              &ignorePoint);
    formattedString += UnicodeString(buffer);
    // 3. If DecimalPlaces is 1, then
    if (decimalPlaces == 1) {
        // 3i. The character . U+002E Full Stop.
        formattedString += u".";
        // 3ii. The single decimal digit character representing the value
        // floor((abs(Input) - floor(abs(Input))) * 10)
        int32_t val = floor((abs(input) - floor(abs(input)) * 10));
        formattedString += digitToChar(val, status);
        U_ASSERT(U_SUCCESS(status));
    }
}

UnicodeString StandardFunctions::TestFunctionValue::formatToString(UErrorCode& status) const {
    if (U_FAILURE(status)) {
        return {};
    }
    if (!canFormat || failsFormat) {
        status = U_MF_FORMATTING_ERROR;
        return {};
    }
    return formattedString;
}

void StandardFunctions::TestFunctionValue::selectKeys(const UnicodeString* keys,
                                                      int32_t keysLen,
                                                      int32_t* prefs,
                                                      int32_t& prefsLen,
                                                      UErrorCode& status) const {
    if (U_FAILURE(status)) {
        return;
    }

    if (!canSelect || failsSelect) {
        status = U_MF_SELECTOR_ERROR;
        return;
    }

    // If the Input is 1 and DecimalPlaces is 1, the method will return some slice
    // of the list « '1.0', '1' », depending on whether those values are included in keys.
    bool include1point0 = false;
    bool include1 = false;
    if (input == 1 && decimalPlaces == 1) {
        include1point0 = true;
        include1 = true;
    } else if (input == 1 && decimalPlaces == 0) {
        include1 = true;
    }

    // If the Input is 1 and DecimalPlaces is 0, the method will return the list « '1' » if
    // keys includes '1', or an empty list otherwise.
    // If the Input is any other value, the method will return an empty list.
    for (int32_t i = 0; i < keysLen; i++) {
        if ((keys[i] == u"1" && include1)
            || (keys[i] == u"1.0" && include1point0)) {
            prefs[prefsLen] = i;
            prefsLen++;
        }
    }
}

StandardFunctions::TestFunction::TestFunction(bool format, bool select) : canFormat(format), canSelect(select) {
    U_ASSERT(format || select);
}

/* static */ StandardFunctions::TestFunction* StandardFunctions::TestFunction::testFunction(UErrorCode& status) {
    return create<TestFunction>(TestFunction(true, true), status);
}

/* static */ StandardFunctions::TestFunction* StandardFunctions::TestFunction::testFormat(UErrorCode& status) {
    return create<TestFunction>(TestFunction(true, false), status);
}

/* static */ StandardFunctions::TestFunction* StandardFunctions::TestFunction::testSelect(UErrorCode& status) {
    return create<TestFunction>(TestFunction(false, true), status);
}

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_MF2 */

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif /* #if !UCONFIG_NO_NORMALIZATION */
