// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#if !UCONFIG_NO_MF2

#include "messageformat2_allocation.h"
#include "messageformat2_evaluation.h"
#include "messageformat2_macros.h"
#include "uvector.h" // U_ASSERT

U_NAMESPACE_BEGIN

// Auxiliary data structures used during formatting a message

namespace message2 {

using namespace data_model;

// Functions
// -------------

ResolvedFunctionOption::ResolvedFunctionOption(const UnicodeString& n,
                                               const FormattableWithOptions& v) : name(n), value(v) {}

ResolvedFunctionOption::ResolvedFunctionOption(const ResolvedFunctionOption& other) {
    name = other.name;
    value = other.value;
}

ResolvedFunctionOption::~ResolvedFunctionOption() {}

/* static */ const ResolvedFunctionOption* FunctionOptions::getResolvedFunctionOptions(
    const FunctionOptions& opts, int32_t& len) {
    U_ASSERT(!opts.bogus);

    len = opts.functionOptionsLen;
    U_ASSERT(len == 0 || opts.options.isValid());
    return opts.options.getAlias();
}

FunctionOptions::FunctionOptions(UVector&& optionsVector, UErrorCode& status) {
    CHECK_ERROR(status);

    functionOptionsLen = optionsVector.size();
    options.adoptInstead(moveVectorToArray<ResolvedFunctionOption>(optionsVector, status));
}

FunctionOptions::FunctionOptions(ResolvedFunctionOption* opts, int32_t len) {
    U_ASSERT(opts != nullptr);
    options.adoptInstead(opts);
    functionOptionsLen = len;
}

FunctionOptions::FunctionOptions(const FunctionOptions& other) {
    U_ASSERT(!other.bogus);

    functionOptionsLen = other.functionOptionsLen;
    options = nullptr;
    UErrorCode localErrorCode = U_ZERO_ERROR;
    if (functionOptionsLen > 0) {
        options.adoptInstead(copyArray<ResolvedFunctionOption>(other.options.getAlias(),
                                                               functionOptionsLen,
                                                               localErrorCode));
    }
    if (U_FAILURE(localErrorCode)) {
        bogus = true;
    }
}

bool FunctionOptions::has(const UnicodeString& name) const {
    U_ASSERT(!bogus);
    if (options != nullptr) {
        for (int32_t i = 0; i < functionOptionsLen; i++) {
            if (options[i].getName() == name) {
                return true;
            }
        }
    } else {
        U_ASSERT(functionOptionsLen == 0);
    }
    return false;

}

const FormattableWithOptions* FunctionOptions::getFunctionOption(const UnicodeString& key,
                                                                 UErrorCode& status) const {
    NULL_ON_ERROR(status);
    U_ASSERT(!bogus);
    if (options != nullptr) {
        for (int32_t i = 0; i < functionOptionsLen; i++) {
            const ResolvedFunctionOption& opt = options[i];
            if (opt.getName() == key) {
                return &(opt.value);
            }
        }
    }
    else {
        U_ASSERT(functionOptionsLen == 0);
    }
    status = U_ILLEGAL_ARGUMENT_ERROR;
    return nullptr;
}

UnicodeString FunctionOptions::getStringFunctionOption(const UnicodeString& key) const {
    UErrorCode localErrorCode = U_ZERO_ERROR;
    U_ASSERT(!bogus);

    const FormattableWithOptions* option = getFunctionOption(key, localErrorCode);
    if (U_SUCCESS(localErrorCode)) {
        Formattable opt = option->getValue();
        if (opt.getType() == UFMT_STRING) {
            UnicodeString val = opt.getString(localErrorCode);
            U_ASSERT(U_SUCCESS(localErrorCode));
            return val;
        }
    }
    // For anything else, including non-string values, return "".
    // Alternately, could try to stringify the non-string option.
    // (Currently, no tests require that.)
    return {};
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

/*
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
*/

int32_t FunctionOptions::getIntFunctionOption(const UnicodeString& key, UErrorCode& errorCode) const {
    UErrorCode localErrorCode = U_ZERO_ERROR;
    U_ASSERT(!bogus);

    const FormattableWithOptions* option = getFunctionOption(key, localErrorCode);
    if (U_SUCCESS(localErrorCode)) {
        Formattable opt = option->getValue();
        switch (opt.getType()) {
            case UFMT_STRING: {
                int64_t result = tryStringAsNumber(Locale("en"), opt, localErrorCode);
                if (U_SUCCESS(localErrorCode)) {
                    return static_cast<int32_t>(result);
                }
                errorCode = U_ILLEGAL_ARGUMENT_ERROR;
                break;
            }
            case UFMT_DOUBLE: {
                return static_cast<int32_t>(opt.getDouble(localErrorCode));
            }
            case UFMT_LONG: {
                return opt.getLong(localErrorCode);
            }
            case UFMT_INT64: {
                return static_cast<int32_t>(opt.getInt64(localErrorCode));
            }
            default: {
                // Other types can't be parsed as a number
                errorCode = U_ILLEGAL_ARGUMENT_ERROR;
                break;
            }
        }
    }
    return -1; // Caller must check for this case
}

/* static */ int32_t FunctionOptions::countUniqueOptions(const FunctionOptions& one,
                                                         const FunctionOptions& other) {
    U_ASSERT(!one.bogus);
    U_ASSERT(!other.bogus);

    int32_t optionsCount = one.optionsCount();
    for (int32_t i = 0; i < other.optionsCount(); i++) {
        if (!one.has(other.options[i].name)) {
            optionsCount++;
        }
    }
    return optionsCount;
}

FunctionOptions FunctionOptions::mergeOptions(const FunctionOptions& other, UErrorCode& status) const {
    if (U_FAILURE(status)) {
        return {};
    }
    U_ASSERT(!bogus);
    U_ASSERT(!other.bogus);
    int32_t totalLen = optionsCount() + other.optionsCount();
    int32_t newLen = countUniqueOptions(*this, other);
    LocalArray<ResolvedFunctionOption> newOptions(new ResolvedFunctionOption[newLen]);
    if (!newOptions.isValid()) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return {};
    }
    for (int32_t i = 0; i < functionOptionsLen; i++) {
        newOptions[i] = options[i];
    }
    int oldIndex = 0;
    int newIndex = functionOptionsLen;
    for (int32_t i = functionOptionsLen; i < totalLen; i++) {
        const ResolvedFunctionOption& oldOption = other.options[oldIndex];
        if (!has(oldOption.name)) {
            newOptions[newIndex++] = oldOption;
        }
        oldIndex++;
    }
    U_ASSERT(newLen >= other.optionsCount());
    for (int32_t i = 0; i < newLen; i++) {
        U_ASSERT(newOptions[i].name.length() > 0);
    }
    return FunctionOptions(newOptions.orphan(), newLen);
}

FunctionOptions& FunctionOptions::operator=(FunctionOptions other) noexcept {
    U_ASSERT(!bogus);
    U_ASSERT(!other.bogus);
    swap(*this, other);
    return *this;
}

FunctionOptions::~FunctionOptions() {}

// ResolvedSelector
// ----------------

ResolvedSelector::ResolvedSelector(const FunctionName& fn,
                                   Selector* sel,
                                   FunctionOptions&& opts,
                                   FormattedPlaceholder&& val)
    : selectorName(fn), selector(sel), options(std::move(opts)), value(std::move(val))  {
    U_ASSERT(sel != nullptr);
}

ResolvedSelector::ResolvedSelector(FormattedPlaceholder&& val) : value(std::move(val)) {}

ResolvedSelector& ResolvedSelector::operator=(ResolvedSelector&& other) noexcept {
    selectorName = std::move(other.selectorName);
    selector.adoptInstead(other.selector.orphan());
    options = std::move(other.options);
    value = std::move(other.value);
    return *this;
}

ResolvedSelector::ResolvedSelector(ResolvedSelector&& other) {
    *this = std::move(other);
}

ResolvedSelector::~ResolvedSelector() {}

// PrioritizedVariant
// ------------------

UBool PrioritizedVariant::operator<(const PrioritizedVariant& other) const {
  if (priority < other.priority) {
      return true;
  }
  return false;
}

PrioritizedVariant::~PrioritizedVariant() {}

    // ---------------- Environments and closures

    Environment* Environment::create(const VariableName& var, Closure&& c, Environment* parent, UErrorCode& errorCode) {
        NULL_ON_ERROR(errorCode);
        Environment* result = new NonEmptyEnvironment(var, std::move(c), parent);
        if (result == nullptr) {
            errorCode = U_MEMORY_ALLOCATION_ERROR;
            return nullptr;
        }
        return result;
    }

    Environment* Environment::create(UErrorCode& errorCode) {
        NULL_ON_ERROR(errorCode);
        Environment* result = new EmptyEnvironment();
        if (result == nullptr) {
            errorCode = U_MEMORY_ALLOCATION_ERROR;
            return nullptr;
        }
        return result;
    }

    const Closure& EmptyEnvironment::lookup(const VariableName& v) const {
        (void) v;
        U_ASSERT(false);
        UPRV_UNREACHABLE_EXIT;
    }

    const Closure& NonEmptyEnvironment::lookup(const VariableName& v) const {
        if (v == var) {
            return rhs;
        }
        return parent->lookup(v);
    }

    bool EmptyEnvironment::has(const VariableName& v) const {
        (void) v;
        return false;
    }

    bool NonEmptyEnvironment::has(const VariableName& v) const {
        if (v == var) {
            return true;
        }
        return parent->has(v);
    }

    Environment::~Environment() {}
    NonEmptyEnvironment::~NonEmptyEnvironment() {}
    EmptyEnvironment::~EmptyEnvironment() {}

    Closure::~Closure() {}

    // MessageContext methods

    void MessageContext::checkErrors(UErrorCode& status) const {
        CHECK_ERROR(status);
        errors.checkErrors(status);
    }

    const Formattable* MessageContext::getGlobal(const VariableName& v, UErrorCode& errorCode) const {
       return arguments.getArgument(v, errorCode);
    }

    MessageContext::MessageContext(const MessageArguments& args,
                                   const StaticErrors& e,
                                   UErrorCode& status) : arguments(args), errors(e, status) {}
    MessageContext::~MessageContext() {}

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_MF2 */

#endif /* #if !UCONFIG_NO_FORMATTING */
