// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef U_HIDE_DEPRECATED_API

#ifndef MESSAGEFORMAT2_FORMATTED_VALUE_H
#define MESSAGEFORMAT2_FORMATTED_VALUE_H

#if U_SHOW_CPLUSPLUS_API

/**
 * \file
 * \brief C++ API: Formats messages using the draft MessageFormat 2.0.
 */

#if !UCONFIG_NO_FORMATTING

#include "unicode/formattedvalue.h"
#include "unicode/messageformat2_data_model.h"
#include "unicode/messageformat2_macros.h"
#include "unicode/numberformatter.h"
#include "unicode/smpdtfmt.h"

U_NAMESPACE_BEGIN namespace message2 {

class FormattedString;
class FormattedNumber;
class FormattingInput;
class FullyFormatted;
class Selector;

extern void formatDateWithDefaults(const Locale& locale, UDate date, UnicodeString&, UErrorCode& errorCode);
extern number::FormattedNumber formatNumberWithDefaults(const Locale& locale, double toFormat, UErrorCode& errorCode);
extern number::FormattedNumber formatNumberWithDefaults(const Locale& locale, int32_t toFormat, UErrorCode& errorCode);
extern number::FormattedNumber formatNumberWithDefaults(const Locale& locale, int64_t toFormat, UErrorCode& errorCode);

/*
  TODO

  A FormattedPlaceholder can either be a fully formatted value,
  or a "Formattable" that could be formatted with a default formatter,
  but is available to be manipulated by formatting functions or deconstructed
  by selector functions.
*/


using Literal         = MessageFormatDataModel::Literal;

class Context;
class Formatter;
class FunctionRegistry;
class MessageFormatter;

class Error : public UMemory {
    public:
    enum Type {
        UnresolvedVariable,
        FormattingWarning,
        ReservedError,
        SelectorError
    };
    Error(Type ty) : type(ty) {
        U_ASSERT(ty == ReservedError);
    }
    Error(Type ty, const Text& t) : type(ty), contents(t.toString()) {} 
    Error(Type ty, const UnicodeString& s) : type(ty), contents(s) {}
    private:
    Type type;
    UnicodeString contents;
}; // class Error

class Errors : public UMemory {
    private:
    LocalPointer<UVector> errors;
    bool dataModelError;
    bool missingSelectorAnnotationError;
    bool selectorError;
    bool syntaxError;
    bool unknownFunctionError;
    bool warning;
    Errors(UErrorCode& errorCode);

    public:
    static Errors* create(UErrorCode&);

    size_t count() const;
    void addNonexhaustivePattern();
    void addDuplicateOption();
    void addSelectorError();
    void addMissingSelectorAnnotation();
    void addUnresolvedVariable(const VariableName&);
    void addSyntaxError();
    void addUnknownFunction(const FunctionName&);
    void addVariantKeyMismatch();
    void addFormattingError();
    bool hasDataModelError() const { return dataModelError; }
    bool hasSelectorError() const { return selectorError; }
    bool hasSyntaxError() const { return syntaxError; }
    bool hasUnknownFunctionError() const { return unknownFunctionError; }
    bool hasMissingSelectorAnnotationError() const { return missingSelectorAnnotationError; }
    bool hasWarning() const { return warning; }
    void addError(Error, UErrorCode&);
    void include(const Errors&, UErrorCode&);
}; // class Errors

// Interface that functions have access to
class State : public UMemory {
    public:

    virtual State& setOutput(const UnicodeString&) = 0;
    virtual State& setOutput(number::FormattedNumber&&) = 0;
    virtual State& setSelectorError(const UnicodeString&, UErrorCode&) = 0;
    virtual State& setFormattingWarning(const UnicodeString&, UErrorCode&) = 0;

    virtual bool hasFormattableInput() const = 0;
    virtual bool hasObjectInput() const = 0;
    virtual bool hasStringOutput() const = 0;
    virtual bool hasNumberOutput() const = 0;
    virtual const UnicodeString& getStringOutput() const = 0;
    virtual const number::FormattedNumber& getNumberOutput() const = 0;
    virtual const Formattable& getFormattableInput() const = 0;
    virtual const UObject& getObjectInput() const = 0;
    virtual bool getStringOption(const UnicodeString&, UnicodeString&) const = 0;
    virtual bool getDoubleOption(const UnicodeString&, double&) const = 0;
    virtual bool getInt64Option(const UnicodeString&, int64_t&) const = 0;
    // Arguments iterator
    virtual const Formattable* nextOption(int32_t&, UnicodeString&) const = 0;
    virtual int32_t firstOption() const = 0;
    virtual size_t optionsCount() const = 0;
    // sets output to string, even if current output is a number
    virtual void formatToString(const Locale&, UErrorCode&) = 0;
    static DateFormat* defaultDateTimeInstance(const Locale&, UErrorCode&);

    virtual ~State();
};

// This is per-operand/expression being formatted,
// so it's not part of the MessageFormatter object
class FormattedValueBuilder : public State {
    private:
    using Builder = FormattedValueBuilder;

    enum InputState {
        FALLBACK,
        OBJECT_INPUT,
        FORMATTABLE_INPUT
    };
    enum OutputState {
        NONE,
        NUMBER,
        STRING
    };

    void clearInput();
    void clearOutput();
    void addFunctionOption(const UnicodeString&, Formattable*, UErrorCode&);
    bool isBuiltInFormatter(const FunctionName&) const;
    bool isCustomFormatter(const FunctionName&) const;
    bool isBuiltInSelector(const FunctionName&) const;
    bool isCustomSelector(const FunctionName&) const;
    void doFormattingCall();
    bool hasFunctionName() const;
    void clearFunctionName();
    void clearFunctionOptions();
    // Precondition: hasSelector()
    Selector* getSelector(UErrorCode&) const;
    // Precondition: hasFormatter()
    const Formatter* getFormatter(UErrorCode&) const;
    void doSelectorCall(const UnicodeString[], size_t, UnicodeString[], size_t&, UErrorCode&);
    void returnFromFunction();
    const FunctionRegistry& customRegistry() const;
    void enterState(InputState s);
    void enterState(OutputState s);
    Builder& promoteFallbackToOutput();
    void formatInputWithDefaults(const Locale&, UErrorCode&);
    void initFunctionOptions(UErrorCode&);
    void initErrors(UErrorCode&);
    Formattable* getOption(const UnicodeString&, Formattable::Type) const;

    private:
    friend class MessageFormatter;

    FormattedValueBuilder(Context&, const MessageFormatter&, UErrorCode&);

    Context& context;
    const MessageFormatter& parent;

    InputState inState;
    OutputState outState;
    LocalPointer<FunctionName> pendingFunctionName;

    UnicodeString fallback; // Fallback string to use in case of errors
    Formattable input; // Invariant: input.getType != kObject (object Formattables can't be copied)
    const UObject* objectInput; // Invariant: ((isObject && objectInput != nullptr) || (!isObject && objectInput == nullptr)
    UnicodeString stringOutput;
    number::FormattedNumber numberOutput;

    LocalPointer<Hashtable> functionOptions;
    LocalPointer<Errors> errors;

    static FormattedValueBuilder* create(Context&, const MessageFormatter&, UErrorCode&);
    // Creates a new builder sharing this's context and parent
    FormattedValueBuilder* create(UErrorCode&);

    Builder& setParseError(uint32_t line, uint32_t offset);
    Builder& setUnresolvedVariable(const VariableName&, UErrorCode&);
    Builder& setUnknownFunction(const FunctionName&);
    Builder& setVariantKeyMismatch();
    Builder& setFormattingWarning(const UnicodeString&, UErrorCode&);
    Builder& setNonexhaustivePattern();
    Builder& setDuplicateOptionName();
    Builder& setReservedError(UErrorCode&);
    Builder& setSelectorError(const UnicodeString&, UErrorCode&);
    Builder& setMissingSelectorAnnotation();
    // Resets input and output and uses existing fallback
    Builder& setFallback();
    // Sets fallback string
    Builder& setFallback(const Text&);
    Builder& setFunctionName(const FunctionName&, UErrorCode&);
    // Function name must be set; clears it
    Builder& resolveSelector(Selector*);
    Builder& setStringOption(const UnicodeString&, const UnicodeString&, UErrorCode&);
    Builder& setDateOption(const UnicodeString&, UDate, UErrorCode&);
    Builder& setNumericOption(const UnicodeString&, double, UErrorCode&);
    Builder& setInput(const UObject*);
    Builder& setInput(const Formattable&);
    Builder& setInput(const UnicodeString&);
    Builder& setObjectInput(UObject*);
    Builder& setOutput(const UnicodeString&);
    Builder& setOutput(number::FormattedNumber&&);
    Builder& propagateErrors(const FormattedValueBuilder& localContext, UErrorCode&);
    // If any errors were set, update `status` accordingly
    Builder& checkErrors(UErrorCode& status);
    Builder& promoteFallbackToInput();
    // Doesn't change output if it already exists
    // Appends to `result`
    //void formatToString(const Locale& locale, UnicodeString& result, UErrorCode&) const;

    bool buildToFunctionCall();
    // If there is a formatter name, clear it and
    // call the function, setting the input and/or output appropriately
    // Precondition: hasFormatter()
    void evalPendingFormatterCall(UErrorCode&);
    void evalPendingSelectorCall(const UnicodeString[], size_t, UnicodeString[], size_t&, UErrorCode&);

    public:
    void formatWithDefaults(UErrorCode& errorCode);
    
    bool hasGlobal(const VariableName& v) const { return hasGlobalAsFormattable(v) || hasGlobalAsObject(v); }
    bool hasGlobalAsFormattable(const VariableName&) const;
    bool hasGlobalAsObject(const VariableName&) const;
    const Formattable& getGlobalAsFormattable(const VariableName&) const;
    const UObject* getGlobalAsObject(const VariableName&) const;
    bool hasOperand() const;
    bool hasSelector() const;
    // Precondition: pending function name is set
    bool hasFormatter() const;
    bool hasOperator() const { return (hasSelector() || hasFormatter()); }
    const FunctionName& getOperator() const;
    bool hasInput() const { return hasFormattableInput() || hasObjectInput(); }
    bool hasFormattableInput() const;
    bool hasObjectInput() const;
    const Formattable& getFormattableInput() const;
    const UObject& getObjectInput() const;
    bool hasStringOutput() const;
    bool hasNumberOutput() const;
    bool hasOutput() { return (hasStringOutput() || hasNumberOutput()); }
    // just gets existing output, doesn't force evaluation
    const UnicodeString& getStringOutput() const;
    const number::FormattedNumber& getNumberOutput() const;
    // forces evaluation
    void formatToString(const Locale&, UErrorCode&);
    bool getStringOption(const UnicodeString&, UnicodeString&) const;
    bool getDoubleOption(const UnicodeString&, double&) const;
    bool getInt64Option(const UnicodeString&, int64_t&) const;
    // Arguments iterator
    int32_t firstOption() const;
    size_t optionsCount() const;
    const Formattable* nextOption(int32_t&, UnicodeString&) const;
    bool isFallback() const;
    bool hasParseError() const;
    bool hasDataModelError() const;
    bool hasMissingSelectorAnnotationError() const;
    bool hasUnknownFunctionError() const;
    bool hasSelectorError() const;
    bool hasError() const;
    bool hasWarning() const;

    virtual ~FormattedValueBuilder();
};

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // MESSAGEFORMAT2_FORMATTED_VALUE_H

#endif // U_HIDE_DEPRECATED_API
// eof
