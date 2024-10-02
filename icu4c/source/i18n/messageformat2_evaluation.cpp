// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_NORMALIZATION

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

ResolvedFunctionOption::ResolvedFunctionOption(const UnicodeString& n,
                                               FormattedPlaceholder&& v,
                                               UErrorCode& status) {
    CHECK_ERROR(status);

    name = n;
    LocalPointer<FormattedPlaceholder>
        temp(create<FormattedPlaceholder>(std::move(v), status));
    if (U_SUCCESS(status)) {
        value.adoptInstead(temp.orphan());
    }
}

ResolvedFunctionOption::~ResolvedFunctionOption() {}


const ResolvedFunctionOption* FunctionOptions::getResolvedFunctionOptions(int32_t& len) const {
    len = functionOptionsLen;
    U_ASSERT(len == 0 || options != nullptr);
    return options;
}

FunctionOptions::FunctionOptions(UVector&& optionsVector, UErrorCode& status) {
    CHECK_ERROR(status);

    functionOptionsLen = optionsVector.size();
    options = moveVectorToArray<ResolvedFunctionOption>(optionsVector, status);
}

const FormattedPlaceholder*
FunctionOptions::getFunctionOption(const UnicodeString& key,
                                   UErrorCode& status) const {
    if (options == nullptr) {
        U_ASSERT(functionOptionsLen == 0);
    }
    for (int32_t i = 0; i < functionOptionsLen; i++) {
        const ResolvedFunctionOption& opt = options[i];
        if (opt.getName() == key) {
            return opt.getValue();
        }
    }
    status = U_ILLEGAL_ARGUMENT_ERROR;
    return nullptr;
}

UnicodeString FunctionOptions::getStringFunctionOption(const UnicodeString& key) const {
    UErrorCode localStatus = U_ZERO_ERROR;
    const FormattedPlaceholder* option = getFunctionOption(key, localStatus);
    if (U_SUCCESS(localStatus)) {
        const Formattable* source = option->getSource(localStatus);
        // Null operand should never appear as an option value
        U_ASSERT(U_SUCCESS(localStatus));
        UnicodeString val = source->getString(localStatus);
        if (U_SUCCESS(localStatus)) {
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
        options = nullptr;
    }
}

// InternalValue
// -------------


InternalValue::~InternalValue() {}
InternalValue& InternalValue::operator=(InternalValue&& other) {
    fallbackString = other.fallbackString;
    functionName = other.functionName;
    resolvedOptions = std::move(other.resolvedOptions);
    operand = std::move(other.operand);
    return *this;
}

InternalValue::InternalValue(InternalValue&& other) {
    *this = std::move(other);
}

InternalValue::InternalValue(const FunctionName& name,
                             FunctionOptions&& options,
                             FormattedPlaceholder&& rand)
    : fallbackString(""), functionName(name),
      resolvedOptions(std::move(options)), operand(std::move(rand)) {}

FormattedPlaceholder InternalValue::takeValue(UErrorCode& status) {
    if (U_FAILURE(status)) {
        return {};
    }
    if (!functionName.isEmpty() || !fallbackString.isEmpty()) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return {};
    }
    return std::move(operand);
}
// Only works if not fully evaluated
FormattedPlaceholder InternalValue::takeOperand(UErrorCode& status) {
    if (U_FAILURE(status)) {
        return {};
    }
    if (functionName.isEmpty()) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return {};
    }
    return std::move(operand);
}
// Only works if not fully evaluated
FunctionOptions InternalValue::takeOptions(UErrorCode& status) {
    if (U_FAILURE(status)) {
        return {};
    }
    if (!isSuspension()) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return {};
    }
    return std::move(resolvedOptions);
}
// Only works if not fully evaluated
FunctionName InternalValue::getFunctionName(UErrorCode& status) const {
    if (U_FAILURE(status)) {
        return {};
    }
    if (functionName.isEmpty()) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return {};
    }
    return functionName;
}

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

    const Formattable* MessageContext::getGlobal(const VariableName& v,
                                                 UErrorCode& errorCode) const {
       return arguments.getArgument(v, errorCode);
    }

    MessageContext::MessageContext(const MessageArguments& args,
                                   const StaticErrors& e,
                                   UErrorCode& status) : arguments(args), errors(e, status) {}

    MessageContext::~MessageContext() {}

    // InternalValue
    // -------------

    bool InternalValue::isFallback() const {
        return std::holds_alternative<FormattedPlaceholder>(argument)
            && std::get_if<FormattedPlaceholder>(&argument)->isFallback();
    }

    bool InternalValue::hasNullOperand() const {
        return std::holds_alternative<FormattedPlaceholder>(argument)
            && std::get_if<FormattedPlaceholder>(&argument)->isNullOperand();
    }

    FormattedPlaceholder InternalValue::takeArgument(UErrorCode& errorCode) {
        if (U_FAILURE(errorCode)) {
            return {};
        }

        if (std::holds_alternative<FormattedPlaceholder>(argument)) {
            return std::move(*std::get_if<FormattedPlaceholder>(&argument));
        }
        errorCode = U_ILLEGAL_ARGUMENT_ERROR;
        return {};
    }

    const UnicodeString& InternalValue::getFallback() const {
        if (std::holds_alternative<FormattedPlaceholder>(argument)) {
            return std::get_if<FormattedPlaceholder>(&argument)->getFallback();
        }
        return (*std::get_if<InternalValue*>(&argument))->getFallback();
    }

    const Selector* InternalValue::getSelector(UErrorCode& errorCode) const {
        if (U_FAILURE(errorCode)) {
            return nullptr;
        }

        if (selector == nullptr) {
            errorCode = U_ILLEGAL_ARGUMENT_ERROR;
        }
        return selector;
    }

    InternalValue::InternalValue(FormattedPlaceholder&& arg) {
        argument = std::move(arg);
        selector = nullptr;
        formatter = nullptr;
    }

    InternalValue::InternalValue(InternalValue* operand,
                                 FunctionOptions&& opts,
                                 const FunctionName& functionName,
                                 const Formatter* f,
                                 const Selector* s) {
        argument = operand;
        options = std::move(opts);
        name = functionName;
        selector = s;
        formatter = f;
        U_ASSERT(selector != nullptr || formatter != nullptr);
    }

    // `this` cannot be used after calling this method
    void InternalValue::forceSelection(DynamicErrors& errs,
                                       const UnicodeString* keys,
                                       int32_t keysLen,
                                       UnicodeString* prefs,
                                       int32_t& prefsLen,
                                       UErrorCode& errorCode) {
        if (U_FAILURE(errorCode)) {
            return;
        }

        if (!canSelect()) {
            errorCode = U_ILLEGAL_ARGUMENT_ERROR;
            return;
        }
        // Find the argument and complete set of options by traversing `argument`
        FunctionOptions opts;
        InternalValue* p = this;
        FunctionName selectorName = name;
        while (std::holds_alternative<InternalValue*>(p->argument)) {
            if (p->name != selectorName) {
                // Can only compose calls to the same selector
                errorCode = U_ILLEGAL_ARGUMENT_ERROR;
                return;
            }
            // First argument to mergeOptions takes precedence
            opts = opts.mergeOptions(std::move(p->options), errorCode);
            if (U_FAILURE(errorCode)) {
                return;
            }
            InternalValue* next = *std::get_if<InternalValue*>(&p->argument);
            p = next;
        }
        FormattedPlaceholder arg = std::move(*std::get_if<FormattedPlaceholder>(&p->argument));

        selector->selectKey(std::move(arg), std::move(opts),
                            keys, keysLen,
                            prefs, prefsLen, errorCode);
        if (U_FAILURE(errorCode)) {
            errorCode = U_ZERO_ERROR;
            errs.setSelectorError(selectorName, errorCode);
        }
    }

    FormattedPlaceholder InternalValue::forceFormatting(DynamicErrors& errs, UErrorCode& errorCode) {
        if (U_FAILURE(errorCode)) {
            return {};
        }

        if (formatter == nullptr && selector == nullptr) {
            U_ASSERT(std::holds_alternative<FormattedPlaceholder>(argument));
            return std::move(*std::get_if<FormattedPlaceholder>(&argument));
        }
        if (formatter == nullptr) {
            errorCode = U_ILLEGAL_ARGUMENT_ERROR;
            return {};
        }

        FormattedPlaceholder arg;

        if (std::holds_alternative<FormattedPlaceholder>(argument)) {
            arg = std::move(*std::get_if<FormattedPlaceholder>(&argument));
        } else {
            arg = (*std::get_if<InternalValue*>(&argument))->forceFormatting(errs,
                                                                             errorCode);
        }

        if (U_FAILURE(errorCode)) {
            return {};
        }

        if (arg.isFallback()) {
            return arg;
        }

        // The fallback for a nullary function call is the function name
        UnicodeString fallback;
        if (arg.isNullOperand()) {
            fallback = u":";
            fallback += name;
        } else {
            fallback = arg.getFallback();
        }

        // Call the function with the argument
        FormattedPlaceholder result = formatter->format(std::move(arg), std::move(options), errorCode);
        if (U_FAILURE(errorCode)) {
            if (errorCode == U_MF_OPERAND_MISMATCH_ERROR) {
                errorCode = U_ZERO_ERROR;
                errs.setOperandMismatchError(name, errorCode);
            } else {
                errorCode = U_ZERO_ERROR;
                // Convey any error generated by the formatter
                // as a formatting error, except for operand mismatch errors
                errs.setFormattingError(name, errorCode);
            }
        }
        // Ignore the output if any error occurred
        if (errs.hasFormattingError()) {
            return FormattedPlaceholder(fallback);
        }

        return result;
    }

    InternalValue& InternalValue::operator=(InternalValue&& other) noexcept {
        argument = std::move(other.argument);
        other.argument = nullptr;
        options = std::move(other.options);
        name = other.name;
        selector = other.selector;
        formatter = other.formatter;
        other.selector = nullptr;
        other.formatter = nullptr;

        return *this;
    }

    InternalValue::~InternalValue() {
        delete selector;
        selector = nullptr;
        delete formatter;
        formatter = nullptr;
        if (std::holds_alternative<InternalValue*>(argument)) {
            delete *std::get_if<InternalValue*>(&argument);
            argument = nullptr;
        }
    }

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_MF2 */

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif /* #if !UCONFIG_NO_NORMALIZATION */
