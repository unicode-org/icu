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

using Builder = FormattedValueBuilder;

// Constructors
// ------------

/* static */ Builder* Builder::create(Context& context, const MessageFormatter& parent, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    LocalPointer<Builder> result(new FormattedValueBuilder(context, parent, errorCode));
    NULL_ON_ERROR(errorCode);
    return result.orphan();
}

Builder* Builder::create(UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    LocalPointer<Builder> result(new FormattedValueBuilder(context, parent, errorCode));
    NULL_ON_ERROR(errorCode);
    return result.orphan();
}

Builder::FormattedValueBuilder(Context& c, const MessageFormatter& mf, UErrorCode& errorCode) : context(c), parent(mf), inState(FALLBACK), outState(NONE) {
    CHECK_ERROR(errorCode);

    initFunctionOptions(errorCode);
    initErrors(errorCode);
}

void Builder::initFunctionOptions(UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);
    functionOptions.adoptInstead(new Hashtable(compareVariableName, nullptr, errorCode));
    CHECK_ERROR(errorCode);
    functionOptions->setValueDeleter(uprv_deleteUObject);
}

void Builder::initErrors(UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);
    errors.adoptInstead(Errors::create(errorCode));
}

Errors::Errors(UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);
    errors.adoptInstead(new UVector(errorCode));
    CHECK_ERROR(errorCode);
    errors->setDeleter(uprv_deleteUObject);
    dataModelError = false;
    missingSelectorAnnotationError = false;
    selectorError = false;
    unknownFunctionError = false;
    warning = false;
}

// State
// ---------

void Builder::enterState(InputState s) {
    U_ASSERT(outState == OutputState::NONE);
    inState = s;
}

void Builder::enterState(OutputState s) {
    if (s > OutputState::NONE) {
        U_ASSERT(hasInput());
    }
    outState = s;
}

bool Builder::isFallback() const {
    return (inState == InputState::FALLBACK);
}

Builder& Builder::setFallback() {
    enterState(FALLBACK);
    return *this;
}

Builder& Builder::setFallback(const Text& t) {
    fallback = t.toString();
    return *this;
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
Builder& Builder::promoteFallbackToInput() {
    U_ASSERT(isFallback());
    return setInput(fallback);
}

// Add the fallback string as the output string
Builder& Builder::promoteFallbackToOutput() {
    U_ASSERT(isFallback());
    return setOutput(fallback);
}

Builder& Builder::setInput(const UnicodeString& s) {
    U_ASSERT(isFallback());
    enterState(FORMATTABLE_INPUT);
    input = Formattable(s);
    return *this;
}

Builder& Builder::setInput(const Formattable& s) {
    U_ASSERT(isFallback());
    enterState(FORMATTABLE_INPUT);
    U_ASSERT(s.getType() != Formattable::Type::kObject);
    input = s;
    return *this;
}

bool Builder::hasFormattableInput() const {
    return (inState == InputState::FORMATTABLE_INPUT);
}

bool Builder::hasObjectInput() const {
    return (inState == InputState::OBJECT_INPUT);
}

const UObject& Builder::getObjectInput() const {
    U_ASSERT(hasObjectInput());
    return *objectInput;
}

const Formattable& Builder::getFormattableInput() const {
    U_ASSERT(hasFormattableInput());
    return input;
}

const number::FormattedNumber& Builder::getNumberOutput() const {
    U_ASSERT(hasNumberOutput());
    return numberOutput;
}

bool Builder::hasStringOutput() const {
    return (inState > FALLBACK && outState == OutputState::STRING);
}

bool Builder::hasNumberOutput() const {
    return (inState > FALLBACK && outState == OutputState::NUMBER);
}

const UnicodeString& Builder::getStringOutput() const {
    U_ASSERT(hasStringOutput());
    return stringOutput;
}

Builder& Builder::setInput(const UObject* obj) {
    U_ASSERT(isFallback());
    U_ASSERT(obj != nullptr);
    enterState(OBJECT_INPUT);
    objectInput = obj;
    return *this;
}

Builder& Builder::setOutput(const UnicodeString& s) {
    U_ASSERT(hasInput());
    enterState(OutputState::STRING);
    stringOutput = s;
    return *this;
}

Builder& Builder::setOutput(number::FormattedNumber&& num) {
    U_ASSERT(hasInput());
    enterState(OutputState::NUMBER);
    numberOutput = std::move(num);
    return *this;
}

void Builder::formatInputWithDefaults(const Locale& locale, UErrorCode& status) {
    CHECK_ERROR(status);

    U_ASSERT(hasFormattableInput());
    U_ASSERT(!hasOutput());

    switch (input.getType()) {
    case Formattable::Type::kDate: {
        formatDateWithDefaults(locale, input.getDate(), stringOutput, status);
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
        // TODO: no default formatters for these. use fallback
        promoteFallbackToOutput();
    }
    }
}

// Forces evaluation
void Builder::formatToString(const Locale& locale, UErrorCode& status) {
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
        case InputState::OBJECT_INPUT: {
            // No default formatter -- use fallback
            setFallback();
            promoteFallbackToOutput();
            break;
        }
        case InputState::FORMATTABLE_INPUT: {
            formatInputWithDefaults(locale, status);
            break;
        }
    }
    CHECK_ERROR(status);
    U_ASSERT(hasStringOutput());
}

void Builder::clearFunctionName() {
    U_ASSERT(hasOutput());
    U_ASSERT(pendingFunctionName.isValid());
    pendingFunctionName.adoptInstead(nullptr);
}            

// Message arguments
// -----------------

bool Builder::hasGlobalAsObject(const VariableName& v) const {
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

bool Builder::hasGlobalAsFormattable(const VariableName& v) const {
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

const UObject* Builder::getGlobalAsObject(const VariableName& v) const {
    U_ASSERT(hasGlobalAsObject(v));
    const Formattable& argValue = context.getVar(v);
    U_ASSERT(argValue.getType() == Formattable::Type::kObject);
    return argValue.getObject();
}

const Formattable& Builder::getGlobalAsFormattable(const VariableName& v) const {
    U_ASSERT(hasGlobalAsFormattable(v));
    const Formattable& argValue = context.getVar(v);
    U_ASSERT(argValue.getType() != Formattable::Type::kObject);
    return argValue;
}


// Function options
// ----------------

// Iterator
int32_t Builder::firstOption() const { return UHASH_FIRST; }

const Formattable* Builder::nextOption(int32_t& pos, UnicodeString& key) const {
    U_ASSERT(functionOptions.isValid());
    const UHashElement* next = functionOptions->nextElement(pos);
    if (next == nullptr) {
        return nullptr;
    }
    key = *((UnicodeString*) next->key.pointer);
    return (const Formattable*) next->value.pointer;
}

size_t Builder::optionsCount() const {
    U_ASSERT(functionOptions.isValid());
    return functionOptions->count();
}

// Adopts `val`
void Builder::addFunctionOption(const UnicodeString& k, Formattable* val, UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);
    U_ASSERT(functionOptions.isValid());
    functionOptions->put(k, val, errorCode);
}

Builder& Builder::setStringOption(const UnicodeString& key, const UnicodeString& value, UErrorCode& errorCode) {
    THIS_ON_ERROR(errorCode);

    LocalPointer<Formattable> valuePtr(createFormattable(value, errorCode));
    THIS_ON_ERROR(errorCode);
    addFunctionOption(key, valuePtr.orphan(), errorCode);
    return *this;
}

Builder& Builder::setDateOption(const UnicodeString& key, UDate date, UErrorCode& errorCode) {
    THIS_ON_ERROR(errorCode);

    LocalPointer<Formattable> valuePtr(createFormattableDate(date, errorCode));
    THIS_ON_ERROR(errorCode);
    addFunctionOption(key, valuePtr.orphan(), errorCode);
    return *this;
}

Builder& Builder::setNumericOption(const UnicodeString& key, double value, UErrorCode& errorCode) {
    THIS_ON_ERROR(errorCode);

    LocalPointer<Formattable> valuePtr(createFormattable(value, errorCode));
    THIS_ON_ERROR(errorCode);
    addFunctionOption(key, valuePtr.orphan(), errorCode);
    return *this;
}

Formattable* Builder::getOption(const UnicodeString& key, Formattable::Type type) const {
    U_ASSERT(functionOptions.isValid());
    Formattable* result = (Formattable*) functionOptions->get(key);
    if (result == nullptr || result->getType() != type) {
        return nullptr;
    }
    return result;
}

bool Builder::getStringOption(const UnicodeString& key, UnicodeString& value) const {
    Formattable* result = getOption(key, Formattable::Type::kString);
    if (result == nullptr) {
        return false;
    }
    value = result->getString();
    return true;
}

bool Builder::getInt64Option(const UnicodeString& key, int64_t& value) const {
    Formattable* result = getOption(key, Formattable::Type::kInt64);
    if (result == nullptr) {
        return false;
    }
    value = result->getInt64();
    return true;
}

bool Builder::getDoubleOption(const UnicodeString& key, double& value) const {
    Formattable* result = getOption(key, Formattable::Type::kDouble);
    if (result == nullptr) {
        return false;
    }
    value = result->getDouble();
    return true;
}


// Functions
// -------------

Builder& Builder::setFunctionName(const FunctionName& fn, UErrorCode& errorCode) {
    THIS_ON_ERROR(errorCode);

    U_ASSERT(!hasFunctionName());
    pendingFunctionName.adoptInstead(new FunctionName(fn));
    if (!pendingFunctionName.isValid()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return *this;
}

bool Builder::hasFunctionName() const {
    return pendingFunctionName.isValid();
}

void Builder::returnFromFunction() {
    U_ASSERT(hasFunctionName());
    clearFunctionName();
    clearFunctionOptions();
}

void Builder::clearFunctionOptions() {
    U_ASSERT(functionOptions.isValid());
    functionOptions->removeAll();
}

const FunctionRegistry& Builder::customRegistry() const { return parent.getCustomFunctionRegistry(); }

bool Builder::isBuiltInFormatter(const FunctionName& fn) const {
    return parent.isBuiltInFormatter(fn);
}

bool Builder::isCustomFormatter(const FunctionName& fn) const {
    return customRegistry().getFormatter(fn) != nullptr;
}

bool Builder::isBuiltInSelector(const FunctionName& fn) const {
    return parent.isBuiltInSelector(fn);
}

bool Builder::isCustomSelector(const FunctionName& fn) const {
    return customRegistry().getSelector(fn) != nullptr;
}

// Precondition: pending function name is set and selector is defined
// Postcondition: selector != nullptr
Selector* Builder::getSelector(UErrorCode& status) const {
    NULL_ON_ERROR(status);

    U_ASSERT(pendingFunctionName.isValid());
    const FunctionName& functionName = *pendingFunctionName;
    const SelectorFactory* selectorFactory = parent.lookupSelectorFactory(functionName, status);
    NULL_ON_ERROR(status);
    // Create a specific instance of the selector
    LocalPointer<Selector> result(selectorFactory->createSelector(parent.getLocale(), status));
    NULL_ON_ERROR(status);
    return result.orphan();
}

// Precondition: pending function name is set and formatter is defined
// Postcondition: formatter != nullptr
const Formatter* Builder::getFormatter(UErrorCode& status) const {
    NULL_ON_ERROR(status);

    U_ASSERT(pendingFunctionName.isValid());
    U_ASSERT(hasFormatter());
    return context.maybeCachedFormatter(*pendingFunctionName, status);
}

bool Builder::hasFormatter() const {
    U_ASSERT(pendingFunctionName.isValid());
    const FunctionName& fn = *pendingFunctionName;
    return isBuiltInFormatter(fn) || isCustomFormatter(fn);
}

bool Builder::hasSelector() const {
    U_ASSERT(pendingFunctionName.isValid());
    const FunctionName& fn = *pendingFunctionName;
    return isBuiltInSelector(fn) || isCustomSelector(fn);
}

void Builder::evalPendingSelectorCall(const UnicodeString keys[], size_t numKeys, UnicodeString keysOut[], size_t& numberMatching, UErrorCode& status) {
    CHECK_ERROR(status);

    U_ASSERT(pendingFunctionName.isValid());
    U_ASSERT(hasSelector());
    LocalPointer<Selector> selectorImpl(getSelector(status));
    CHECK_ERROR(status);
    selectorImpl->selectKey(*this, keys, numKeys, keysOut, numberMatching, status);
    returnFromFunction();
}

void Builder::evalPendingFormatterCall(UErrorCode& status) {
    CHECK_ERROR(status);

    U_ASSERT(pendingFunctionName.isValid());
    U_ASSERT(hasFormatter());
    const Formatter* formatterImpl = getFormatter(status);
    CHECK_ERROR(status);
    formatterImpl->format(*this, status);
    returnFromFunction();
}

// Errors
// -------

Builder& Builder::propagateErrors(const FormattedValueBuilder& other, UErrorCode& errorCode) {
    THIS_ON_ERROR(errorCode);

    U_ASSERT(errors.isValid() && other.errors.isValid());
    errors->include(*other.errors, errorCode);
    return *this;
}

Builder& Builder::checkErrors(UErrorCode& status) {
    THIS_ON_ERROR(status);

    // TODO: figure out how to return a representation of the errors

    return *this;
}

bool Builder::hasDataModelError() const {
    U_ASSERT(errors.isValid());
    return errors->hasDataModelError();
}

bool Builder::hasParseError() const {
    U_ASSERT(errors.isValid());
    return errors->hasSyntaxError();
}

bool Builder::hasSelectorError() const {
    U_ASSERT(errors.isValid());
    return errors->hasSelectorError();
}

bool Builder::hasUnknownFunctionError() const {
    U_ASSERT(errors.isValid());
    return errors->hasUnknownFunctionError();
}

bool Builder::hasMissingSelectorAnnotationError() const {
    U_ASSERT(errors.isValid());
    return errors->hasMissingSelectorAnnotationError();
}

bool Builder::hasError() const {
    U_ASSERT(errors.isValid());
    return errors->count() > 0;
}

bool Builder::hasWarning() const {
    U_ASSERT(errors.isValid());
    return errors->hasWarning();
}

Builder& Builder::setUnresolvedVariable(const VariableName& v, UErrorCode& status) {
    THIS_ON_ERROR(status);

    U_ASSERT(errors.isValid());
    Error err(Error::Type::UnresolvedVariable, v);
    errors->addError(err, status);
    return *this;
}

Builder& Builder::setFormattingWarning(const UnicodeString& formatterName, UErrorCode& status) {
    THIS_ON_ERROR(status);
    
    U_ASSERT(errors.isValid());
    Error err(Error::Type::FormattingWarning, formatterName);
    errors->addError(err, status);
    return *this;
}

Builder& Builder::setSelectorError(const UnicodeString& selectorName, UErrorCode& status) {
    THIS_ON_ERROR(status);
    
    U_ASSERT(errors.isValid());
    Error err(Error::Type::SelectorError, selectorName);
    errors->addError(err, status);
    return *this;
}

Builder& Builder::setReservedError(UErrorCode& status) {
    THIS_ON_ERROR(status);
    
    U_ASSERT(errors.isValid());
    Error err(Error::Type::ReservedError);
    errors->addError(err, status);
    return *this;
}

Errors* Errors::create(UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    return new Errors(errorCode);
}

size_t Errors::count() const {
    U_ASSERT(errors.isValid());
    return ((size_t) errors->size());
}

void Errors::include(const Errors& other, UErrorCode& status) {
    CHECK_ERROR(status);

    dataModelError |= other.dataModelError;
    missingSelectorAnnotationError |= other.missingSelectorAnnotationError;
    selectorError |= other.selectorError;
    syntaxError |= other.syntaxError;
    unknownFunctionError |= other.unknownFunctionError;
    warning |= other.warning;

    LocalPointer<Error> err;
    for (size_t i = 0; ((int32_t) i) < other.errors->size(); i++) {
        const Error& otherErr = *((Error*) (*other.errors)[i]);
        err.adoptInstead(new Error(otherErr));
        if (!err.isValid()) {
            status = U_MEMORY_ALLOCATION_ERROR;
            return;
        }
        errors->adoptElement(err.orphan(), status);
    }
}

void Errors::addError(Error e, UErrorCode& status) {
    CHECK_ERROR(status);

    U_ASSERT(errors.isValid());
    Error* eP = new Error(e);
    if (eP == nullptr) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
    errors->adoptElement(eP, status);
}

Builder::~Builder() {}
State::~State() {}

number::FormattedNumber formatNumberWithDefaults(const Locale& locale, double toFormat, UErrorCode& errorCode) {
    return number::NumberFormatter::withLocale(locale).formatDouble(toFormat, errorCode);
}

number::FormattedNumber formatNumberWithDefaults(const Locale& locale, int32_t toFormat, UErrorCode& errorCode) {
    return number::NumberFormatter::withLocale(locale).formatInt(toFormat, errorCode);
}

number::FormattedNumber formatNumberWithDefaults(const Locale& locale, int64_t toFormat, UErrorCode& errorCode) {
    return number::NumberFormatter::withLocale(locale).formatInt(toFormat, errorCode);
}

/* static */ DateFormat* State::defaultDateTimeInstance(const Locale& locale, UErrorCode& errorCode) {
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

    LocalPointer<DateFormat> df(State::defaultDateTimeInstance(locale, errorCode));
    CHECK_ERROR(errorCode);
    df->format(date, result, 0, errorCode);
}

/*
UnicodeString Fallback::toString() const {
    UnicodeString result;
    result += LEFT_CURLY_BRACE;
    result += fallback.toString();
    result += RIGHT_CURLY_BRACE;
    return result;
}

UnicodeString Fallback::toString(Locale l, UErrorCode& s) const {
    (void) l;
    (void) s;
    return toString();
}
*/

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */
