// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/messageformat2_formatting_context.h"
#include "unicode/messageformat2_function_registry.h"
#include "unicode/messageformat2_data_model.h"
#include "unicode/messageformat2.h"
#include "uvector.h" // U_ASSERT

U_NAMESPACE_BEGIN namespace message2 {

// Context that's specific to formatting a single expression

// Constructors
// ------------

/* static */ ExpressionContext* ExpressionContext::create(MessageContext& globalContext, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    LocalPointer<ExpressionContext> result(new ExpressionContext(globalContext, errorCode));
    NULL_ON_ERROR(errorCode);
    return result.orphan();
}

ExpressionContext* ExpressionContext::create(UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    LocalPointer<ExpressionContext> result(new ExpressionContext(context, errorCode));
    NULL_ON_ERROR(errorCode);
    return result.orphan();
}

ExpressionContext::ExpressionContext(MessageContext& c, UErrorCode& errorCode) : context(c), inState(FALLBACK), outState(NONE) {
    CHECK_ERROR(errorCode);

    initFunctionOptions(errorCode);
}

void ExpressionContext::initFunctionOptions(UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);
    functionOptions.adoptInstead(new Hashtable(compareVariableName, nullptr, errorCode));
    CHECK_ERROR(errorCode);
    // `functionOptions` owns its values
    functionOptions->setValueDeleter(uprv_deleteUObject);
}

// State
// ---------

void ExpressionContext::enterState(InputState s) {
    // If we're entering an error state, clear the output
    if (s == InputState::FALLBACK) {
        enterState(OutputState::NONE);
    }
    inState = s;
    
}

void ExpressionContext::enterState(OutputState s) {
    // Input must exist if output exists
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

// Fallback values are enclosed in curly braces;
// see https://github.com/unicode-org/message-format-wg/blob/main/spec/formatting.md#formatting-fallback-values
void fallbackToString(const Text& t, UnicodeString& result) {
    result += LEFT_CURLY_BRACE;
    result += t.toString();
    result += RIGHT_CURLY_BRACE;
}

void ExpressionContext::setFallback(const Text& t) {
    fallback.remove();
    fallbackToString(t, fallback);
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

// Used when handling function calls with no argument
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

UBool ExpressionContext::hasFormattableInput() const {
    return (inState == InputState::FORMATTABLE_INPUT);
}

UBool ExpressionContext::hasObjectInput() const {
    return (inState == InputState::OBJECT_INPUT);
}

const UObject& ExpressionContext::getObjectInput() const {
    U_ASSERT(hasObjectInput());
    return *objectInput;
}

const UObject* ExpressionContext::getObjectInputPointer() const {
    U_ASSERT(hasObjectInput());
    return objectInput;
}

const Formattable& ExpressionContext::getFormattableInput() const {
    U_ASSERT(hasFormattableInput());
    return input;
}

const number::FormattedNumber& ExpressionContext::getNumberOutput() const {
    U_ASSERT(hasNumberOutput());
    return numberOutput;
}

UBool ExpressionContext::hasStringOutput() const {
    return (inState > FALLBACK && outState == OutputState::STRING);
}

UBool ExpressionContext::hasNumberOutput() const {
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

// Called when output is required and no output is present;
// formats the input to a string with defaults, for inputs that can be
// formatted with a default formatter
void ExpressionContext::formatInputWithDefaults(const Locale& locale, UErrorCode& status) {
    CHECK_ERROR(status);

    U_ASSERT(hasFormattableInput());
    U_ASSERT(!hasOutput());

    // Try as decimal number first
    if (input.isNumeric()) {
        StringPiece asDecimal = input.getDecimalNumber(status);
        CHECK_ERROR(status);
        if (asDecimal != nullptr) {
            setOutput(formatNumberWithDefaults(locale, asDecimal, status));
            return;
        }
    }

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

// Called when string output is required; forces output to be produced
// if none is present (including formatting number output as a string)
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

// Helper functions for function options
// -------------------------------------

/* static */ Formattable* ExpressionContext::createFormattable(const UnicodeString& v, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    Formattable* result = new Formattable(v);
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

/* static */  Formattable* ExpressionContext::createFormattable(double v, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    Formattable* result = new Formattable(v);
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

/* static */  Formattable* ExpressionContext::createFormattable(int64_t v, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    Formattable* result = new Formattable(v);
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

/* static */ Formattable* ExpressionContext::createFormattable(const UObject* v, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    // This object will only be accessed through getObjectOption(), which returns
    // a const reference
    Formattable* result = new Formattable(const_cast<UObject*>(v));
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

/* static */ Formattable* ExpressionContext::createFormattable(const UnicodeString* in, int32_t count, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    LocalArray<Formattable> arr(new Formattable[count]);
    if (!arr.isValid()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }

    LocalPointer<Formattable> val;
    for (int32_t i = 0; i < count; i++) {
        // TODO
        // Without this explicit cast, `val` is treated as if it's
        // an object when it's assigned into `arr[i]`. I don't know why.
        val.adoptInstead(new Formattable((const UnicodeString&) in[i]));
        if (!val.isValid()) {
            errorCode = U_MEMORY_ALLOCATION_ERROR;
            return nullptr;
        }
        arr[i] = *val;
    }

    Formattable* result(new Formattable(arr.orphan(), count));
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

/* static */ Formattable* ExpressionContext::createFormattableDate(UDate v, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    Formattable* result = new Formattable(v, Formattable::kIsDate);
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

/* static */ Formattable* ExpressionContext::createFormattableDecimal(StringPiece val, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    Formattable* result = new Formattable(val, errorCode);
    if (U_FAILURE(errorCode)) {
        return nullptr;
    }
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

// Function options iterator
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


// Function options
// ----------------

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

void ExpressionContext::setObjectOption(const UnicodeString& key, const UObject* value, UErrorCode& errorCode) {
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

UBool ExpressionContext::getStringOption(const UnicodeString& key, UnicodeString& value) const {
    Formattable* result = getOption(key, Formattable::Type::kString);
    if (result == nullptr) {
        return false;
    }
    value = result->getString();
    return true;
}

const UObject& ExpressionContext::getObjectOption(const UnicodeString& key) const {
    Formattable* result = getOption(key, Formattable::Type::kObject);
    U_ASSERT(result != nullptr);
    const UObject* value = result->getObject();
    U_ASSERT(value != nullptr);
    return *value;
}

UBool ExpressionContext::hasObjectOption(const UnicodeString& key) const {
    Formattable* result = getOption(key, Formattable::Type::kObject);
    return (result != nullptr);
}

bool ExpressionContext::tryStringAsNumberOption(const UnicodeString& key, double& value) const {
    // Check for a string option, try to parse it as a number if present
    UnicodeString tempValue;
    if (!getStringOption(key, tempValue)) {
        return false;
    }
    UErrorCode localErrorCode = U_ZERO_ERROR;
    LocalPointer<NumberFormat> numberFormat(NumberFormat::createInstance(context.messageFormatter().locale, localErrorCode));
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

UBool ExpressionContext::getInt64Option(const UnicodeString& key, int64_t& value) const {
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

UBool ExpressionContext::getDoubleOption(const UnicodeString& key, double& value) const {
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

// Precondition: pending function name is set and selector is defined
// Postcondition: selector != nullptr
Selector* ExpressionContext::getSelector(UErrorCode& status) const {
    NULL_ON_ERROR(status);

    U_ASSERT(pendingFunctionName.isValid());
    const FunctionName& functionName = *pendingFunctionName;
    const SelectorFactory* selectorFactory = context.lookupSelectorFactory(functionName, status);
    NULL_ON_ERROR(status);
    // Create a specific instance of the selector
    LocalPointer<Selector> result(selectorFactory->createSelector(context.messageFormatter().locale, status));
    NULL_ON_ERROR(status);
    return result.orphan();
}

// Precondition: pending function name is set and formatter is defined
// Postcondition: formatter != nullptr
const Formatter* ExpressionContext::getFormatter(UErrorCode& status) {
    NULL_ON_ERROR(status);

    U_ASSERT(pendingFunctionName.isValid());
    U_ASSERT(hasFormatter());
    return context.maybeCachedFormatter(*pendingFunctionName, status);
}

bool ExpressionContext::hasFormatter() const {
    U_ASSERT(pendingFunctionName.isValid());
    return context.isFormatter(*pendingFunctionName);
}

bool ExpressionContext::hasSelector() const {
    if (!pendingFunctionName.isValid()) {
        return false;
    }
    return context.isSelector(*pendingFunctionName);
}

// Calls the pending selector
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

// Calls the pending formatter
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
                // Convey any error generated by the formatter
                // as a formatting error
                setFallback();
                status = U_ZERO_ERROR;
                setFormattingError(functionName.name(), status);
            } else {
                // Ignore warnings
                status = savedStatus;
            }
        }
        // Ignore the output if any errors occurred
        if (context.getErrors().hasFormattingError()) {
            clearOutput();
        }
        returnFromFunction();
        if (savedFunctionName != nullptr) {
            setFunctionName(*savedFunctionName, status);
        }
        return;
    }
    // No formatter with this name -- set error
    if (context.isSelector(functionName)) {
        setFormattingError(functionName.name(), status);
    } else {
        context.getErrors().setUnknownFunction(functionName, status);
    }
    setFallback();
}

// Default formatters
// ------------------

number::FormattedNumber formatNumberWithDefaults(const Locale& locale, double toFormat, UErrorCode& errorCode) {
    return number::NumberFormatter::withLocale(locale).formatDouble(toFormat, errorCode);
}

number::FormattedNumber formatNumberWithDefaults(const Locale& locale, int32_t toFormat, UErrorCode& errorCode) {
    return number::NumberFormatter::withLocale(locale).formatInt(toFormat, errorCode);
}

number::FormattedNumber formatNumberWithDefaults(const Locale& locale, int64_t toFormat, UErrorCode& errorCode) {
    return number::NumberFormatter::withLocale(locale).formatInt(toFormat, errorCode);
}

number::FormattedNumber formatNumberWithDefaults(const Locale& locale, StringPiece toFormat, UErrorCode& errorCode) {
    return number::NumberFormatter::withLocale(locale).formatDecimal(toFormat, errorCode);
}

DateFormat* defaultDateTimeInstance(const Locale& locale, UErrorCode& errorCode) {
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

    LocalPointer<DateFormat> df(defaultDateTimeInstance(locale, errorCode));
    CHECK_ERROR(errorCode);
    df->format(date, result, 0, errorCode);
}

// Errors
// -------

void ExpressionContext::setFormattingError(const UnicodeString& formatterName, UErrorCode& status) {
    CHECK_ERROR(status);

    context.getErrors().setFormattingError(formatterName, status);
}

void ExpressionContext::setSelectorError(const UnicodeString& selectorName, UErrorCode& status) {
    CHECK_ERROR(status);

    context.getErrors().setSelectorError(selectorName, status);
}

ExpressionContext::~ExpressionContext() {}
FormattingContext::~FormattingContext() {}

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */
