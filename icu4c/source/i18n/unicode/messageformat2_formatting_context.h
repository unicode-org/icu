// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef U_HIDE_DEPRECATED_API

#ifndef MESSAGEFORMAT2_FORMATTING_CONTEXT_H
#define MESSAGEFORMAT2_FORMATTING_CONTEXT_H

#if U_SHOW_CPLUSPLUS_API

/**
 * \file
 * \brief C++ API: Formats messages using the draft MessageFormat 2.0.
 */

#if !UCONFIG_NO_FORMATTING

#include "unicode/formattedvalue.h"
#include "unicode/messageformat2_context.h"
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
extern number::FormattedNumber formatNumberWithDefaults(const Locale& locale, StringPiece toFormat, UErrorCode& errorCode);
extern DateFormat* defaultDateTimeInstance(const Locale&, UErrorCode&);

/**
 * <p>MessageFormatter is a Technical Preview API implementing MessageFormat 2.0.
 * Since it is not final, documentation has not yet been added everywhere.
 *
 * The following class represents the input to a custom function; it encapsulates
 * the function's (unnamed) argument and its named options, as well as providing
 * methods for the function to record its output.
 *
 * @internal ICU 74.0 technology preview
 * @deprecated This API is for technology preview only.
 */
class U_I18N_API FormattingContext : public UMemory {
    public:

    /**
     * Sets the function's output to a string value.
     *
     * @param output The value of the output.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    virtual void setOutput(const UnicodeString& output) = 0;
    /**
     * Sets the function's output to a `number::FormattedNumber` value
     *
     * @param output The value of the output, which is passed by move.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    virtual void setOutput(number::FormattedNumber&& output) = 0;
    /**
     * Indicates that an error occurred during selection, such as an
     * argument with a type that doesn't support selection. Errors are signaled
     * internally to the `FormattingContext` object and propagated at the end of
     * formatting, and are not signaled using the usual `UErrorCode` mechanism
     * (`UErrorCode`s are still used to indicate memory allocation errors and any
     * errors signaled by other ICU functions).
     *
     * @param name Any informative string (usually the name of the selector function).
     * @param status Input/output error code
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    virtual void setSelectorError(const UnicodeString& name, UErrorCode& status) = 0;
    /**
     * Indicates that an error occurred during formatting, such as an argument
     * having a type not supported by this formatter.
     *
     * @param name Any informative string (usually the name of the formatter function).
     * @param status Input/output error code
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    virtual void setFormattingWarning(const UnicodeString&, UErrorCode&) = 0;
    /**
     * Returns true if and only if a `Formattable` argument was supplied to this
     * function. (Object arguments must be checked for using `hasObjectinput()` and
     * are not treated as a `Formattable` wrapping an object.) Each function has
     * at most one argument, so if `hasFormattableInput()` is true,
     * `hasObjectInput()` is false, and vice versa.
     *     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    virtual bool hasFormattableInput() const = 0;
    /**
     * Returns a reference to this function's argument as a `Formattable`.
     * It is an internal error to call this method if `!hasFormattableInput()`.
     * In particular, if the argument passed in is a UObject*, it is an internal
     * error to call `getFormattableInput()` (`getObjectInput()` must be called instead.)
     *     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */ 
    virtual const Formattable& getFormattableInput() const = 0;
    /**
     * Returns true if and only if a `UObject*` argument was supplied to this
     * function.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    virtual bool hasObjectInput() const = 0;
    /**
     * Returns a reference to this function's `UObject*` argument.
     * It is an internal error to call this method if `!hasObjectInput()`.
     *     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */ 
    virtual const UObject& getObjectInput() const = 0;
    /**
     * Returns true if and only if the argument being passed in already has a formatted
     * result that is a string. This formatted result may be treated as the input
     * to this formatter, or may be overwritten with the result of formatting the
     * original input differently.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    virtual bool hasStringOutput() const = 0;
    /**
     * Returns true if and only if the argument being passed in already has a formatted
     * result that is a number. This formatted result may be treated as the input
     * to this formatter, or may be overwritten with the result of formatting the
     * original input differently.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    virtual bool hasNumberOutput() const = 0;
    /**
     * Returns a reference to the existing formatted output of this argument as a string.
     * It is an internal error to call this method if `!hasStringOutput()`.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    virtual const UnicodeString& getStringOutput() const = 0;
    /**
     * Returns a reference to the existing formatted output of this argument as a string.
     * It is an internal error to call this method if `!hasNumberOutput()`.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    virtual const number::FormattedNumber& getNumberOutput() const = 0;
    /**
     * Looks up the value of a named string option. The return value is true if
     * and only if there is a string-typed option named `optionName`
     *
     * @param optionName The name of the option.
     * @param optionValue A mutable reference that is set to the string value of
     *        the option if the named option exists.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    virtual bool getStringOption(const UnicodeString& optionName, UnicodeString& optionValue) const = 0;
    /**
     * Looks up the value of a named numeric option of type `double`.
     * The return value is true if and only if there is a `double`-typed option
     * named `optionName`
     *
     * @param optionName The name of the option.
     * @param optionValue A mutable reference that is set to the `double` value of
     *        the option if the named option exists.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    virtual bool getDoubleOption(const UnicodeString& optionName, double& optionValue) const = 0;
    /**
     * Looks up the value of a named numeric option of type `int64_t`.
     * The return value is true if and only if there is a `int64_t`-typed option
     * named `optionName`
     *
     * @param optionName The name of the option.
     * @param optionValue A mutable reference that is set to the `double` value of
     *        the option if the named option exists.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    virtual bool getInt64Option(const UnicodeString& optionName, int64_t& optionValue) const = 0;
    /**
     * Iterates over all options. The order in which the options are returned is unspecified.
     * The return value is null if there are no further options after `pos`, and is a pointer
     * to a `Formattable` (whose value will be string, double or int64_t; other types are not used)
     * otherwise.
     *
     * @param pos A mutable reference to the current iterator position. Should be set to
     *            `firstOption()` before the first call to `nextOption()`.
     * @param optionName A mutable reference that is set to the name of the next option
     *        if the return value is non-null.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    virtual const Formattable* nextOption(int32_t& pos, UnicodeString& optionName) const = 0;
    /**
     * Returns the initial iterator position to be used with `nextOption()`.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    virtual int32_t firstOption() const = 0;
    /**
     * Returns the number of named options.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    virtual int32_t optionsCount() const = 0;
    /**
     * Formats the current argument as a string, using defaults. If `hasNumberOutput()` is
     * true, then the string output is set to the result of formatting the number output,
     * and the number output is cleared. If the function's argument is either absent or is
     * a fallback value, the output is the result of formatting the fallback value (which
     * is the default fallback string if the argument is absent). If the function's argument
     * is object-typed, then the argument is treated as a fallback value, since there is
     * no default formatter for objects.
     *
     * @param locale The locale to use for formatting numbers or dates (does not affect
     *        the formatting of a pre-formatted number, if a number output is already present)
     * @param status Input/output error code
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    virtual void formatToString(const Locale& locale, UErrorCode& status) = 0;

    virtual ~FormattingContext();
};

class FunctionRegistry;

// This is per-operand/expression being formatted,
// so it's not part of the MessageFormatter object
class ExpressionContext : public FormattingContext {
    private:

    enum InputState {
        FALLBACK,
        NO_OPERAND, // Used when the argument is absent, but there are no errors
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
    const FunctionName& getFunctionName();
    void clearFunctionName();
    void clearFunctionOptions();
    // Precondition: hasSelector()
    Selector* getSelector(UErrorCode&) const;
    // Precondition: hasFormatter()
    const Formatter* getFormatter(UErrorCode&);
    void doSelectorCall(const UnicodeString[], int32_t, UnicodeString[], int32_t&, UErrorCode&);
    void returnFromFunction();
    const FunctionRegistry& customRegistry() const;
    bool hasCustomRegistry() const;
    void enterState(InputState s);
    void enterState(OutputState s);
    void promoteFallbackToOutput();
    void formatInputWithDefaults(const Locale&, UErrorCode&);
    void initFunctionOptions(UErrorCode&);
    Formattable* getOption(const UnicodeString&, Formattable::Type) const;
    void replaceFunctionName(const FunctionName&, UErrorCode&);
    bool tryStringAsNumberOption(const UnicodeString&, double&) const;
    Formattable* getNumericOption(const UnicodeString&) const;
    bool isSelector(const FunctionName&) const;

    private:
    friend class MessageFormatter;

    ExpressionContext(Context&, const MessageFormatter&, UErrorCode&);

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

    static ExpressionContext* create(Context&, const MessageFormatter&, UErrorCode&);
    // Creates a new builder sharing this's context and parent
    ExpressionContext* create(UErrorCode&);

    void setParseError(uint32_t line, uint32_t offset);
    void setUnresolvedVariable(const VariableName&, UErrorCode&);
    void setUnknownFunction(const FunctionName&, UErrorCode&);
    void setSelectorError(const FunctionName&, UErrorCode&);
    void setVariantKeyMismatch();
    void setFormattingWarning(const UnicodeString&, UErrorCode&);
    void setNonexhaustivePattern();
    void setDuplicateOptionName();
    void setReservedError(UErrorCode&);
    void setSelectorError(const UnicodeString&, UErrorCode&);
    void setMissingSelectorAnnotation(UErrorCode&);
    // Resets input and output and uses existing fallback
    void setFallback();
    // Sets fallback string
    void setFallback(const Text&);
    void setFunctionName(const FunctionName&, UErrorCode&);

    // Function name must be set; clears it
    void resolveSelector(Selector*);
    void setStringOption(const UnicodeString&, const UnicodeString&, UErrorCode&);
    void setDateOption(const UnicodeString&, UDate, UErrorCode&);
    void setNumericOption(const UnicodeString&, double, UErrorCode&);
    void setNoOperand();
    void setInput(const UObject*);
    void setInput(const Formattable&);
    void setInput(const UnicodeString&);
    void setObjectInput(UObject*);
    void setOutput(const UnicodeString&);
    void setOutput(number::FormattedNumber&&);
    void promoteFallbackToInput();

    bool buildToFunctionCall();
    void evalFormatterCall(const FunctionName&, UErrorCode&);
    // If there is a functionName name, clear it and
    // call the function, setting the input and/or output appropriately
    // Precondition: hasSelector()
    void evalPendingSelectorCall(const UnicodeString[], int32_t, UnicodeString[], int32_t&, UErrorCode&);

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
    // Just gets existing output, doesn't force evaluation
    const UnicodeString& getStringOutput() const;
    const number::FormattedNumber& getNumberOutput() const;
    // forces evaluation
    void formatToString(const Locale&, UErrorCode&);
    bool getStringOption(const UnicodeString&, UnicodeString&) const;
    bool getDoubleOption(const UnicodeString&, double&) const;
    bool getInt64Option(const UnicodeString&, int64_t&) const;
    // Arguments iterator
    int32_t firstOption() const;
    int32_t optionsCount() const;
    const Formattable* nextOption(int32_t&, UnicodeString&) const;
    bool isFallback() const;
    bool hasParseError() const;
    bool hasDataModelError() const;
    bool hasMissingSelectorAnnotationError() const;
    bool hasUnknownFunctionError() const;
    bool hasFormattingWarning() const;
    bool hasSelectorError() const;
    bool hasError() const;

    virtual ~ExpressionContext();
};

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // MESSAGEFORMAT2_FORMATTING_CONTEXT_H

#endif // U_HIDE_DEPRECATED_API
// eof
