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

ResolvedFunctionOption::ResolvedFunctionOption(ResolvedFunctionOption&& other) {
    name = std::move(other.name);
    value = std::move(other.value);
}

ResolvedFunctionOption::~ResolvedFunctionOption() {}

/* static */ ResolvedFunctionOption* FunctionOptions::getResolvedFunctionOptions(
    FunctionOptions&& opts, int32_t& len) {
    len = opts.functionOptionsLen;
    U_ASSERT(len == 0 || opts.options != nullptr);
    return std::move(opts).options;
}

FunctionOptions::FunctionOptions(UVector&& optionsVector, UErrorCode& status) {
    CHECK_ERROR(status);

    functionOptionsLen = optionsVector.size();
    options = moveVectorToArray<ResolvedFunctionOption>(optionsVector, status);
}

const FormattedPlaceholder* FunctionOptions::getFunctionOption(const UnicodeString& key,
                                                               UErrorCode& status) const {
    NULL_ON_ERROR(status);
    if (options != nullptr) {
        for (int32_t i = 0; i < functionOptionsLen; i++) {
            const ResolvedFunctionOption& opt = options[i];
            if (opt.getName() == key) {
                return opt.getValue();
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
    const FormattedPlaceholder* option = getFunctionOption(key, localErrorCode);
    if (U_SUCCESS(localErrorCode)) {
        Formattable opt = option->asFormattable();
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

FunctionOptions& FunctionOptions::operator=(FunctionOptions&& other) noexcept {
    functionOptionsLen = other.functionOptionsLen;
    options = other.options;
    other.functionOptionsLen = 0;
    other.options = nullptr;
    return *this;
}

FunctionOptions::FunctionOptions(FunctionOptions&& other) {
    *this = std::move(other);
}

FunctionOptions::~FunctionOptions() {
    if (options != nullptr) {
        delete[] options;
    }
}
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
