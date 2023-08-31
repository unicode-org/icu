// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/messageformat2_formatted_value.h"
#include "unicode/messageformat2_function_registry.h"
#include "unicode/messageformat2_data_model.h"
#include "unicode/messageformat2.h"
#include "uvector.h" // U_ASSERT

U_NAMESPACE_BEGIN namespace message2 {

// Constructors
// ------------

/* static */ ExpressionContext* ExpressionContext::create(Context& globalContext, const MessageFormatter& parent, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    LocalPointer<ExpressionContext> result(new ExpressionContext(globalContext, parent, errorCode));
    NULL_ON_ERROR(errorCode);
    return result.orphan();
}

ExpressionContext* ExpressionContext::create(UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    LocalPointer<ExpressionContext> result(new ExpressionContext(context, parent, errorCode));
    NULL_ON_ERROR(errorCode);
    return result.orphan();
}

ExpressionContext::ExpressionContext(Context& c, const MessageFormatter& mf, UErrorCode& errorCode) : context(c), parent(mf), inState(FALLBACK), outState(NONE) {
    CHECK_ERROR(errorCode);

    initFunctionOptions(errorCode);
}

void ExpressionContext::initFunctionOptions(UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);
    functionOptions.adoptInstead(new Hashtable(compareVariableName, nullptr, errorCode));
    CHECK_ERROR(errorCode);
    functionOptions->setValueDeleter(uprv_deleteUObject);
}

Errors::Errors(UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);
    syntaxAndDataModelErrors.adoptInstead(new UVector(errorCode));
    resolutionAndFormattingErrors.adoptInstead(new UVector(errorCode));
    CHECK_ERROR(errorCode);
    syntaxAndDataModelErrors->setDeleter(uprv_deleteUObject);
    resolutionAndFormattingErrors->setDeleter(uprv_deleteUObject);
    dataModelError = false;
    formattingWarning = false;
    missingSelectorAnnotationError = false;
    selectorError = false;
    syntaxError = false;
    unknownFunctionError = false;
}

Errors::~Errors() {}
Error::~Error() {}
// State
// ---------

void ExpressionContext::enterState(InputState s) {
    if (s == InputState::FALLBACK) {
        enterState(OutputState::NONE);
    }
    inState = s;
    
}

void ExpressionContext::enterState(OutputState s) {
    if (s > OutputState::NONE) {
        U_ASSERT(hasInput());
    }
    outState = s;
}

bool ExpressionContext::isFallback() const {
    return (inState == InputState::FALLBACK);
}

void ExpressionContext::setFallback() {
    enterState(FALLBACK);
}

void fallbackToString(const Text& t, UnicodeString& result) {
    result += LEFT_CURLY_BRACE;
    result += t.toString();
    result += RIGHT_CURLY_BRACE;
}

void ExpressionContext::setFallback(const Text& t) {
    fallback.remove();
    fallbackToString(t, fallback);
}

static Formattable* createFormattable(const UnicodeString& v, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    
    Formattable* result = new Formattable(v);
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

static Formattable* createFormattable(double v, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    
    Formattable* result = new Formattable(v);
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

static Formattable* createFormattableDate(UDate v, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    
    Formattable* result = new Formattable(v, Formattable::kIsDate);
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

// Add the fallback string as the input string, and
// unset this as a fallback
void ExpressionContext::promoteFallbackToInput() {
    U_ASSERT(isFallback());
    return setInput(fallback);
}

// Add the fallback string as the output string
void ExpressionContext::promoteFallbackToOutput() {
    U_ASSERT(isFallback());
    return setOutput(fallback);
}

void ExpressionContext::setNoOperand() {
    U_ASSERT(isFallback());
    enterState(NO_OPERAND);
}

void ExpressionContext::setInput(const UnicodeString& s) {
    U_ASSERT(inState <= NO_OPERAND);
    enterState(FORMATTABLE_INPUT);
    input = Formattable(s);
}

void ExpressionContext::setInput(const Formattable& s) {
    U_ASSERT(isFallback());
    enterState(FORMATTABLE_INPUT);
    U_ASSERT(s.getType() != Formattable::Type::kObject);
    input = s;
}

bool ExpressionContext::hasFormattableInput() const {
    return (inState == InputState::FORMATTABLE_INPUT);
}

bool ExpressionContext::hasObjectInput() const {
    return (inState == InputState::OBJECT_INPUT);
}

const UObject& ExpressionContext::getObjectInput() const {
    U_ASSERT(hasObjectInput());
    return *objectInput;
}

const Formattable& ExpressionContext::getFormattableInput() const {
    U_ASSERT(hasFormattableInput());
    return input;
}

const number::FormattedNumber& ExpressionContext::getNumberOutput() const {
    U_ASSERT(hasNumberOutput());
    return numberOutput;
}

bool ExpressionContext::hasStringOutput() const {
    return (inState > FALLBACK && outState == OutputState::STRING);
}

bool ExpressionContext::hasNumberOutput() const {
    return (inState > FALLBACK && outState == OutputState::NUMBER);
}

const UnicodeString& ExpressionContext::getStringOutput() const {
    U_ASSERT(hasStringOutput());
    return stringOutput;
}

void ExpressionContext::setInput(const UObject* obj) {
    U_ASSERT(isFallback());
    U_ASSERT(obj != nullptr);
    enterState(OBJECT_INPUT);
    objectInput = obj;
}

void ExpressionContext::setOutput(const UnicodeString& s) {
    if (inState == InputState::NO_OPERAND) {
        // If setOutput() is called while the
        // operand is null, set the input to the
        // output string
        setInput(s);
    }
    U_ASSERT(hasInput());
    enterState(OutputState::STRING);
    stringOutput = s;
}

void ExpressionContext::setOutput(number::FormattedNumber&& num) {
    U_ASSERT(hasInput());
    enterState(OutputState::NUMBER);
    numberOutput = std::move(num);
}

void ExpressionContext::clearOutput() {
    stringOutput.remove();
    enterState(OutputState::NONE);
}

void ExpressionContext::formatInputWithDefaults(const Locale& locale, UErrorCode& status) {
    CHECK_ERROR(status);

    U_ASSERT(hasFormattableInput());
    U_ASSERT(!hasOutput());

    switch (input.getType()) {
    case Formattable::Type::kDate: {
        formatDateWithDefaults(locale, input.getDate(), stringOutput, status);
        enterState(OutputState::STRING);
        break;
    }
    case Formattable::Type::kDouble: {
        setOutput(formatNumberWithDefaults(locale, input.getDouble(), status));
        break;
    }
    case Formattable::Type::kLong: {
        setOutput(formatNumberWithDefaults(locale, input.getLong(), status));
        break;
    }
    case Formattable::Type::kInt64: {
        setOutput(formatNumberWithDefaults(locale, input.getInt64(), status));
        break;
    }
    case Formattable::Type::kString: {
        setOutput(input.getString());
        break;
    }
    default: {
        // No default formatters for other types; use fallback
        promoteFallbackToOutput();
    }
    }
}

// Forces evaluation
void ExpressionContext::formatToString(const Locale& locale, UErrorCode& status) {
    CHECK_ERROR(status);

    switch (outState) {
        case OutputState::STRING: {
            return; // Nothing to do
        }
        case OutputState::NUMBER: {
            setOutput(numberOutput.toString(status));
            return;
        }
        default: {
            break;
        }
    }
    switch (inState) {
        case InputState::FALLBACK: {
            setInput(fallback);
            setOutput(fallback);
            break;
        }
        case InputState::NO_OPERAND:
            // No operand and a function call hasn't cleared the state --
            // use fallback
        case InputState::OBJECT_INPUT: {
            setFallback();
            promoteFallbackToOutput();
            break;
        }
        case InputState::FORMATTABLE_INPUT: {
            formatInputWithDefaults(locale, status);
            // Force number to string, in case the result was a number
            formatToString(locale, status);
            break;
        }
    }
    CHECK_ERROR(status);
    U_ASSERT(hasStringOutput());
}

void ExpressionContext::clearFunctionName() {
    U_ASSERT(pendingFunctionName.isValid());
    pendingFunctionName.adoptInstead(nullptr);
}            

const FunctionName& ExpressionContext::getFunctionName() {
    U_ASSERT(pendingFunctionName.isValid());
    return *pendingFunctionName;
}            

// Message arguments
// -----------------

bool ExpressionContext::hasGlobalAsObject(const VariableName& v) const {
    if (!context.hasVar(v)) {
        return false;
    }
    switch (context.getVar(v).getType()) {
        case Formattable::Type::kObject: {
            return true;
        }
        default: {
            return false;
        }
    }
}

bool ExpressionContext::hasGlobalAsFormattable(const VariableName& v) const {
    if (!context.hasVar(v)) {
        return false;
    }
    switch (context.getVar(v).getType()) {
        case Formattable::Type::kObject: {
            return false;
        }
        default: {
            return true;
        }
    }
}

const UObject* ExpressionContext::getGlobalAsObject(const VariableName& v) const {
    U_ASSERT(hasGlobalAsObject(v));
    const Formattable& argValue = context.getVar(v);
    U_ASSERT(argValue.getType() == Formattable::Type::kObject);
    return argValue.getObject();
}

const Formattable& ExpressionContext::getGlobalAsFormattable(const VariableName& v) const {
    U_ASSERT(hasGlobalAsFormattable(v));
    const Formattable& argValue = context.getVar(v);
    U_ASSERT(argValue.getType() != Formattable::Type::kObject);
    return argValue;
}


// Function options
// ----------------

// Iterator
int32_t ExpressionContext::firstOption() const { return UHASH_FIRST; }

const Formattable* ExpressionContext::nextOption(int32_t& pos, UnicodeString& key) const {
    U_ASSERT(functionOptions.isValid());
    const UHashElement* next = functionOptions->nextElement(pos);
    if (next == nullptr) {
        return nullptr;
    }
    key = *((UnicodeString*) next->key.pointer);
    return (const Formattable*) next->value.pointer;
}

int32_t ExpressionContext::optionsCount() const {
    U_ASSERT(functionOptions.isValid());
    return functionOptions->count();
}

// Adopts `val`
void ExpressionContext::addFunctionOption(const UnicodeString& k, Formattable* val, UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);
    U_ASSERT(functionOptions.isValid());
    functionOptions->put(k, val, errorCode);
}

void ExpressionContext::setStringOption(const UnicodeString& key, const UnicodeString& value, UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    LocalPointer<Formattable> valuePtr(createFormattable(value, errorCode));
    CHECK_ERROR(errorCode);
    addFunctionOption(key, valuePtr.orphan(), errorCode);
}

void ExpressionContext::setDateOption(const UnicodeString& key, UDate date, UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    LocalPointer<Formattable> valuePtr(createFormattableDate(date, errorCode));
    CHECK_ERROR(errorCode);
    addFunctionOption(key, valuePtr.orphan(), errorCode);
}

void ExpressionContext::setNumericOption(const UnicodeString& key, double value, UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    LocalPointer<Formattable> valuePtr(createFormattable(value, errorCode));
    CHECK_ERROR(errorCode);
    addFunctionOption(key, valuePtr.orphan(), errorCode);
}

Formattable* ExpressionContext::getOption(const UnicodeString& key, Formattable::Type type) const {
    U_ASSERT(functionOptions.isValid());
    Formattable* result = (Formattable*) functionOptions->get(key);
    if (result == nullptr || result->getType() != type) {
        return nullptr;
    }
    return result;
}

Formattable* ExpressionContext::getNumericOption(const UnicodeString& key) const {
    U_ASSERT(functionOptions.isValid());
    Formattable* result = (Formattable*) functionOptions->get(key);
    if (result == nullptr || !result->isNumeric()) {
        return nullptr;
    }
    return result;
}

bool ExpressionContext::getStringOption(const UnicodeString& key, UnicodeString& value) const {
    Formattable* result = getOption(key, Formattable::Type::kString);
    if (result == nullptr) {
        return false;
    }
    value = result->getString();
    return true;
}

bool ExpressionContext::tryStringAsNumberOption(const UnicodeString& key, double& value) const {
    // Check for a string option, try to parse it as a number if present
    UnicodeString tempValue;
    if (!getStringOption(key, tempValue)) {
        return false;
    }
    UErrorCode localErrorCode = U_ZERO_ERROR;
    LocalPointer<NumberFormat> numberFormat(NumberFormat::createInstance(parent.locale, localErrorCode));
    if (U_FAILURE(localErrorCode)) {
        return false;
    }
    Formattable asNumber;
    numberFormat->parse(tempValue, asNumber, localErrorCode);
    if (U_FAILURE(localErrorCode)) {
        return false;
    }
    value = asNumber.getDouble(localErrorCode);
    if (U_FAILURE(localErrorCode)) {
        return false;
    }
    return true;
}

bool ExpressionContext::getInt64Option(const UnicodeString& key, int64_t& value) const {
    Formattable* result = getNumericOption(key);
    if (result == nullptr) {
        double doubleResult;
        if (tryStringAsNumberOption(key, doubleResult)) {
            value = (int64_t) doubleResult;
            return true;
        }
        return false;
    }
    UErrorCode localErrorCode = U_ZERO_ERROR;
    value = result->getInt64(localErrorCode);
    if (U_SUCCESS(localErrorCode)) {
        return true;
    }
    // Option was numeric but couldn't be converted to int64_t -- could be overflow
    return false;
}

bool ExpressionContext::getDoubleOption(const UnicodeString& key, double& value) const {
    Formattable* result = getNumericOption(key);
    if (result == nullptr) {
        return tryStringAsNumberOption(key, value);
    }
    UErrorCode localErrorCode = U_ZERO_ERROR;
    value = result->getDouble(localErrorCode);
    // The conversion must succeed, since the result is numeric
    U_ASSERT(U_SUCCESS(localErrorCode));
    return true;
}


// Functions
// -------------

void ExpressionContext::setFunctionName(const FunctionName& fn, UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    U_ASSERT(!hasFunctionName());
    pendingFunctionName.adoptInstead(new FunctionName(fn));
    if (!pendingFunctionName.isValid()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
}

bool ExpressionContext::hasFunctionName() const {
    return pendingFunctionName.isValid();
}

void ExpressionContext::returnFromFunction() {
    U_ASSERT(hasFunctionName());
    clearFunctionName();
    clearFunctionOptions();
}

void ExpressionContext::clearFunctionOptions() {
    U_ASSERT(functionOptions.isValid());
    functionOptions->removeAll();
}

const FunctionRegistry& ExpressionContext::customRegistry() const {
    U_ASSERT(hasCustomRegistry());
    return parent.getCustomFunctionRegistry();
}

bool ExpressionContext::hasCustomRegistry() const {
    return parent.hasCustomFunctionRegistry();
}

bool ExpressionContext::isBuiltInFormatter(const FunctionName& fn) const {
    return parent.isBuiltInFormatter(fn);
}

bool ExpressionContext::isCustomFormatter(const FunctionName& fn) const {
    return hasCustomRegistry() && customRegistry().getFormatter(fn) != nullptr;
}

bool ExpressionContext::isBuiltInSelector(const FunctionName& fn) const {
    return parent.isBuiltInSelector(fn);
}

bool ExpressionContext::isCustomSelector(const FunctionName& fn) const {
    return hasCustomRegistry() && customRegistry().getSelector(fn) != nullptr;
}

// Precondition: pending function name is set and selector is defined
// Postcondition: selector != nullptr
Selector* ExpressionContext::getSelector(UErrorCode& status) const {
    NULL_ON_ERROR(status);

    U_ASSERT(pendingFunctionName.isValid());
    const FunctionName& functionName = *pendingFunctionName;
    const SelectorFactory* selectorFactory = parent.lookupSelectorFactory(context, functionName, status);
    NULL_ON_ERROR(status);
    // Create a specific instance of the selector
    LocalPointer<Selector> result(selectorFactory->createSelector(parent.locale, status));
    NULL_ON_ERROR(status);
    return result.orphan();
}

// Precondition: pending function name is set and formatter is defined
// Postcondition: formatter != nullptr
const Formatter* ExpressionContext::getFormatter(UErrorCode& status) {
    NULL_ON_ERROR(status);

    U_ASSERT(pendingFunctionName.isValid());
    U_ASSERT(hasFormatter());
    return parent.maybeCachedFormatter(context, *pendingFunctionName, status);
}

bool ExpressionContext::hasFormatter() const {
    U_ASSERT(pendingFunctionName.isValid());
    const FunctionName& fn = *pendingFunctionName;
    return isBuiltInFormatter(fn) || isCustomFormatter(fn);
}

bool ExpressionContext::isSelector(const FunctionName& fn) const {
    return isBuiltInSelector(fn) || isCustomSelector(fn);
}

bool ExpressionContext::hasSelector() const {
    if (!pendingFunctionName.isValid()) {
        return false;
    }
    return isSelector(*pendingFunctionName);
}

void ExpressionContext::evalPendingSelectorCall(const UnicodeString keys[], int32_t numKeys, UnicodeString keysOut[], int32_t& numberMatching, UErrorCode& status) {
    CHECK_ERROR(status);

    U_ASSERT(pendingFunctionName.isValid());
    U_ASSERT(hasSelector());
    LocalPointer<Selector> selectorImpl(getSelector(status));
    CHECK_ERROR(status);
    UErrorCode savedStatus = status;
    selectorImpl->selectKey(*this, keys, numKeys, keysOut, numberMatching, status);
    // Update errors
    if (savedStatus != status) {
        if (U_FAILURE(status)) {
            setFallback();
            status = U_ZERO_ERROR;
            setSelectorError(pendingFunctionName->name(), status);
        } else {
            // Ignore warnings
            status = savedStatus;
        }
    }
    returnFromFunction();
}

void ExpressionContext::evalFormatterCall(const FunctionName& functionName, UErrorCode& status) {
    CHECK_ERROR(status);

    FunctionName* savedFunctionName = pendingFunctionName.isValid() ? pendingFunctionName.orphan() : nullptr;
    setFunctionName(functionName, status);
    CHECK_ERROR(status);
    if (hasFormatter()) {
        const Formatter* formatterImpl = getFormatter(status);
        CHECK_ERROR(status);
        UErrorCode savedStatus = status;
        formatterImpl->format(*this, status);
        // Update errors
        if (savedStatus != status) {
            if (U_FAILURE(status)) {
                setFallback();
                status = U_ZERO_ERROR;
                setFormattingWarning(functionName.name(), status);
                if (U_SUCCESS(status)) {
                    status = U_FORMATTING_WARNING;
                }
            } else {
                // Ignore warnings
                status = savedStatus;
            }
        }
        if (hasFormattingWarning()) {
            clearOutput();
        }
        returnFromFunction();
        if (savedFunctionName != nullptr) {
            setFunctionName(*savedFunctionName, status);
        }
        return;
    }
    // No formatter with this name -- set error
    if (isSelector(functionName)) {
        setFormattingWarning(functionName.name(), status);
    } else {
        setUnknownFunction(functionName, status);
    }
    setFallback();
}

// Errors
// -------


bool ExpressionContext::hasDataModelError() const {
    return context.hasDataModelError();
}

bool ExpressionContext::hasParseError() const {
    return context.hasParseError();
}

bool ExpressionContext::hasSelectorError() const {
    return context.hasSelectorError();
}

bool ExpressionContext::hasUnknownFunctionError() const {
    return context.hasUnknownFunctionError();
}

bool ExpressionContext::hasMissingSelectorAnnotationError() const {
    return context.hasMissingSelectorAnnotationError();
}

bool ExpressionContext::hasFormattingWarning() const {
    return context.hasFormattingWarning();
}

bool ExpressionContext::hasError() const {
    return context.hasError();
}

void ExpressionContext::setUnresolvedVariable(const VariableName& v, UErrorCode& status) {
    CHECK_ERROR(status);

    Error err(Error::Type::UnresolvedVariable, v);
    context.addError(err, status);
}

void ExpressionContext::setUnknownFunction(const FunctionName& fn, UErrorCode& status) {
    CHECK_ERROR(status);

    Error err(Error::Type::UnknownFunction, fn);
    context.addError(err, status);

}

void ExpressionContext::setMissingSelectorAnnotation(UErrorCode& status) {
    CHECK_ERROR(status);

    Error err(Error::Type::MissingSelectorAnnotation);
    context.addError(err, status);
}

void ExpressionContext::setFormattingWarning(const UnicodeString& formatterName, UErrorCode& status) {
    CHECK_ERROR(status);

    context.setFormattingWarning(formatterName, status);
}

void ExpressionContext::setSelectorError(const UnicodeString& selectorName, UErrorCode& status) {
    CHECK_ERROR(status);
    
    Error err(Error::Type::SelectorError, selectorName);
    context.addError(err, status);
}

void ExpressionContext::setSelectorError(const FunctionName& selectorName, UErrorCode& status) {
    CHECK_ERROR(status);
    
    Error err(Error::Type::SelectorError, selectorName.toString());
    context.addError(err, status);
}

void ExpressionContext::setReservedError(UErrorCode& status) {
    CHECK_ERROR(status);
    
    Error err(Error::Type::ReservedError);
    context.addError(err, status);
}

ExpressionContext::~ExpressionContext() {}
FormattingContext::~FormattingContext() {}

number::FormattedNumber formatNumberWithDefaults(const Locale& locale, double toFormat, UErrorCode& errorCode) {
    return number::NumberFormatter::withLocale(locale).formatDouble(toFormat, errorCode);
}

number::FormattedNumber formatNumberWithDefaults(const Locale& locale, int32_t toFormat, UErrorCode& errorCode) {
    return number::NumberFormatter::withLocale(locale).formatInt(toFormat, errorCode);
}

number::FormattedNumber formatNumberWithDefaults(const Locale& locale, int64_t toFormat, UErrorCode& errorCode) {
    return number::NumberFormatter::withLocale(locale).formatInt(toFormat, errorCode);
}

/* static */ DateFormat* FormattingContext::defaultDateTimeInstance(const Locale& locale, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    LocalPointer<DateFormat> df(DateFormat::createDateTimeInstance(DateFormat::SHORT, DateFormat::SHORT, locale));
    if (!df.isValid()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return df.orphan();
}

void formatDateWithDefaults(const Locale& locale, UDate date, UnicodeString& result, UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    LocalPointer<DateFormat> df(FormattingContext::defaultDateTimeInstance(locale, errorCode));
    CHECK_ERROR(errorCode);
    df->format(date, result, 0, errorCode);
}

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */
