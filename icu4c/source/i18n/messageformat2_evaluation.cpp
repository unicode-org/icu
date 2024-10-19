// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#if !UCONFIG_NO_MF2

#include "unicode/ubidi.h"
#include "messageformat2_allocation.h"
#include "messageformat2_evaluation.h"
#include "messageformat2_macros.h"
#include "uvector.h" // U_ASSERT

U_NAMESPACE_BEGIN

// Auxiliary data structures used during formatting a message

namespace message2 {

using namespace data_model;

// BaseValue
// ---------

BaseValue::BaseValue(const Locale& loc, const Formattable& source)
    : locale(loc) {
    operand = source;
}

/* static */ BaseValue* BaseValue::create(const Locale& locale,
                                          const Formattable& source,
                                          UErrorCode& errorCode) {
    return message2::create<BaseValue>(BaseValue(locale, source), errorCode);
}

extern UnicodeString formattableToString(const Locale&, const UBiDiDirection, const Formattable&, UErrorCode&);

UnicodeString BaseValue::formatToString(UErrorCode& errorCode) const {
    return formattableToString(locale,
                               UBIDI_NEUTRAL,
                               operand,
                               errorCode);
}

BaseValue& BaseValue::operator=(BaseValue&& other) noexcept {
    operand = std::move(other.operand);
    opts = std::move(other.opts);
    locale = other.locale;

    return *this;
}

BaseValue::BaseValue(BaseValue&& other) {
    *this = std::move(other);
}

// Functions
// -------------

ResolvedFunctionOption::ResolvedFunctionOption(ResolvedFunctionOption&& other) {
    *this = std::move(other);
}

ResolvedFunctionOption::ResolvedFunctionOption(const UnicodeString& n,
                                               FunctionValue* f) : name(n), value(f) {
    U_ASSERT(f != nullptr);
}

ResolvedFunctionOption::~ResolvedFunctionOption() {
    if (value != nullptr) {
        delete value;
        value = nullptr;
    }
}


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

const FunctionValue*
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


UnicodeString
FunctionOptions::getStringFunctionOption(const UnicodeString& k, UErrorCode& errorCode) const {
    const FunctionValue* option = getFunctionOption(k, errorCode);
    if (U_SUCCESS(errorCode)) {
        UnicodeString result = option->formatToString(errorCode);
        if (U_SUCCESS(errorCode)) {
            return result;
        }
    }
    return {};
}

UnicodeString FunctionOptions::getStringFunctionOption(const UnicodeString& key) const {
    UErrorCode localStatus = U_ZERO_ERROR;

    UnicodeString result = getStringFunctionOption(key, localStatus);
    if (U_FAILURE(localStatus)) {
        return {};
    }
    return result;
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

static bool containsOption(const UVector& opts, const ResolvedFunctionOption& opt) {
    for (int32_t i = 0; i < opts.size(); i++) {
        if (static_cast<ResolvedFunctionOption*>(opts[i])->getName()
            == opt.getName()) {
            return true;
        }
    }
    return false;
}

// Options in `this` take precedence
// `this` can't be used after mergeOptions is called
FunctionOptions FunctionOptions::mergeOptions(FunctionOptions&& other,
                                              UErrorCode& status) {
    UVector mergedOptions(status);
    mergedOptions.setDeleter(uprv_deleteUObject);

    if (U_FAILURE(status)) {
        return {};
    }

    // Create a new vector consisting of the options from this `FunctionOptions`
    for (int32_t i = 0; i < functionOptionsLen; i++) {
        mergedOptions.adoptElement(create<ResolvedFunctionOption>(std::move(options[i]), status),
                                 status);
    }

    // Add each option from `other` that doesn't appear in this `FunctionOptions`
    for (int i = 0; i < other.functionOptionsLen; i++) {
        // Note: this is quadratic in the length of `options`
        if (!containsOption(mergedOptions, other.options[i])) {
            mergedOptions.adoptElement(create<ResolvedFunctionOption>(std::move(other.options[i]),
                                                                    status),
                                     status);
        }
    }

    delete[] options;
    options = nullptr;
    functionOptionsLen = 0;

    return FunctionOptions(std::move(mergedOptions), status);
}

// InternalValue
// -------------


InternalValue::~InternalValue() {}

InternalValue& InternalValue::operator=(InternalValue&& other) {
    isFallbackValue = other.isFallbackValue;
    fallbackString = other.fallbackString;
    if (!isFallbackValue) {
        U_ASSERT(other.val.isValid());
        val.adoptInstead(other.val.orphan());
    }
    return *this;
}

InternalValue::InternalValue(InternalValue&& other) {
    *this = std::move(other);
}

InternalValue::InternalValue(UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    NullValue* nv = new NullValue();
    if (nv == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
    val.adoptInstead(nv);
}

InternalValue::InternalValue(FunctionValue* v, const UnicodeString& fb)
    : fallbackString(fb), val(v) {
    U_ASSERT(v != nullptr);
}

FunctionValue* InternalValue::takeValue(UErrorCode& status) {
    if (U_FAILURE(status)) {
        return {};
    }
    if (isFallback()) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return {};
    }
    U_ASSERT(val.isValid());
    return val.orphan();
}

const FunctionValue* InternalValue::getValue(UErrorCode& status) const {
    if (U_FAILURE(status)) {
        return {};
    }
    if (isFallback()) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return {};
    }
    U_ASSERT(val.isValid());
    return val.getAlias();
}

bool InternalValue::isSelectable() const {
    if (isFallbackValue) {
        return false;
    }
    return val->isSelectable();
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
