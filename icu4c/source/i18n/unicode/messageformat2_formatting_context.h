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

class Selector;
class SelectorFactory;

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
    virtual void setFormattingError(const UnicodeString&, UErrorCode&) = 0;
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
    virtual UBool hasFormattableInput() const = 0;
    /**
     * Accesses the function's argument, assuming it has type  `Formattable`.
     * It is an internal error to call this method if `!hasFormattableInput()`.
     * In particular, if the argument passed in is a UObject*, it is an internal
     * error to call `getFormattableInput()` (`getObjectInput()` must be called instead.)
     *
     * @return A reference to the argument to this function.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */ 
    virtual const Formattable& getFormattableInput() const = 0;
    /**
     * Determines the type of input to this function.
     *
     * @return True if and only if a `UObject*` argument was supplied to this
     *         function.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    virtual UBool hasObjectInput() const = 0;
    /**
     * Accesses the function's argument, assuming it has type `UObject`.
     * It is an internal error to call this method if `!hasObjectInput()`.
     *
     * @return A reference to the argument to this function.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    virtual const UObject& getObjectInput() const = 0;
    /**
     * Checks if the argument being passed in already has a formatted
     * result that is a string. This formatted result may be treated as the input
     * to this formatter, or may be overwritten with the result of formatting the
     * original input differently.
     *
     * @return True if and only if formatted string output is present.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    virtual UBool hasStringOutput() const = 0;
    /**
     * Checks if the argument being passed in already has a formatted
     * result that is a number. This formatted result may be treated as the input
     * to this formatter, or may be overwritten with the result of formatting the
     * original input differently.
     *
     * @return True if and only if formatted number output is present.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    virtual UBool hasNumberOutput() const = 0;
    /**
     * Accesses the existing formatted output of this argument as a string.
     * It is an internal error to call this method if `!hasStringOutput()`.
     *
     * @return A reference to the existing formatted string output.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    virtual const UnicodeString& getStringOutput() const = 0;
    /**
     * Accesses the existing formatted output of this argument as a number.
     * It is an internal error to call this method if `!hasNumberOutput()`.
     *
     * @return A reference to the existing formatted number output.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    virtual const number::FormattedNumber& getNumberOutput() const = 0;
    /**
     * Looks up the value of a named string option.
     *
     * @param optionName The name of the option.
     * @param optionValue A mutable reference that is set to the string value of
     *        the option if the named option exists.
     * @return True if and only if a string-typed option named `optionName` exists.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    virtual UBool getStringOption(const UnicodeString& optionName, UnicodeString& optionValue) const = 0;
    /**
     * Looks up the value of a named numeric option of type `double`.
     * The return value is true if and only if there is a `double`-typed option
     * named `optionName`
     *
     * @param optionName The name of the option.
     * @param optionValue A mutable reference that is set to the `double` value of
     *        the option if the named option exists.
     * @return True if and only if a double-typed option named `optionName` exists.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    virtual UBool getDoubleOption(const UnicodeString& optionName, double& optionValue) const = 0;
    /**
     * Looks up the value of a named numeric option of type `int64_t`.
     * The return value is true if and only if there is a `int64_t`-typed option
     * named `optionName`
     *
     * @param optionName The name of the option.
     * @param optionValue A mutable reference that is set to the `double` value of
     *        the option if the named option exists.
     * @return True if and only if a int64-typed option named `optionName` exists.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    virtual UBool getInt64Option(const UnicodeString& optionName, int64_t& optionValue) const = 0;
    /**
     * Checks for a named object option.
     *
     * @param optionName The name of the option.
     * @return True if and only if an object-typed option named `optionName` exists.
     **
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    virtual UBool hasObjectOption(const UnicodeString& optionName) const = 0;
    /**
     * Accesses a named object option.
     * Precondition: the option must exist.
     *
     * @param optionName The name of the option.
     * @return           A reference to the object value of the option.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    virtual const UObject& getObjectOption(const UnicodeString& optionName) const = 0;
    /**
     * Iterates over all options. The order in which the options are returned is unspecified.
     *
     * @param pos A mutable reference to the current iterator position. Should be set to
     *            `firstOption()` before the first call to `nextOption()`.
     * @param optionName A mutable reference that is set to the name of the next option
     *        if the return value is non-null.
     * @return A pointer to a `Formattable`  (whose value will be string, double, date, or int64;
     *         other types are not used). The pointer is null if there are no further options
     *         from `pos`.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    virtual const Formattable* nextOption(int32_t& pos, UnicodeString& optionName) const = 0;
    /**
     * Used with `nextOption()`.
     *
     * @return The initial iterator position for `nextOption()`.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    virtual int32_t firstOption() const = 0;
    /**
     * Gets the number of options.
     *
     * @return The number of named options.
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

// The ExpressionContext contains everything needed to format a specific operand
// or expression.
class ExpressionContext : public FormattingContext {
    private:

    // The input state tracks whether the formatter has a Formattable
    // or object input; represents an absent operand; or is in an error state.
    enum InputState {
        FALLBACK,
        NO_OPERAND, // Used when the argument is absent, but there are no errors
        OBJECT_INPUT,
        FORMATTABLE_INPUT
    };

    // The output state tracks whether (formatted) numeric or string output
    // has been generated.
    enum OutputState {
        NONE,
        NUMBER,
        STRING
    };

    void clearInput();
    void clearOutput();

    bool hasFunctionName() const;
    const FunctionName& getFunctionName();
    void clearFunctionName();
    // Precondition: hasSelector()
    Selector* getSelector(UErrorCode&) const;
    // Precondition: hasFormatter()
    const Formatter* getFormatter(UErrorCode&);

    void initFunctionOptions(UErrorCode&);
    void addFunctionOption(const UnicodeString&, Formattable*, UErrorCode&);
    void clearFunctionOptions();
    Formattable* getOption(const UnicodeString&, Formattable::Type) const;
    bool tryStringAsNumberOption(const UnicodeString&, double&) const;
    Formattable* getNumericOption(const UnicodeString&) const;

    void doFormattingCall();
    void doSelectorCall(const UnicodeString[], int32_t, UnicodeString[], int32_t&, UErrorCode&);
    void returnFromFunction();

    void enterState(InputState s);
    void enterState(OutputState s);
    void promoteFallbackToOutput();
    void formatInputWithDefaults(const Locale&, UErrorCode&);

    ExpressionContext(MessageContext&, UErrorCode&);

    friend class MessageArguments;
    friend class MessageFormatter;

    MessageContext& context;

    InputState inState;
    OutputState outState;

    // Function name that has been set but not yet invoked on an argument
    LocalPointer<FunctionName> pendingFunctionName;

    // Fallback string to use in case of errors
    UnicodeString fallback;

    // Input arises from literals or a message argument
    // Invariant: input.getType != kObject (object Formattables can't be copied)
    Formattable input;
    // (An object input can only originate from a message argument)
    // Invariant: ((isObject && objectInput != nullptr) || (!isObject && objectInput == nullptr)
    const UObject* objectInput;
    const UObject* getObjectInputPointer() const;
 
    // Output is returned by a formatting function
    UnicodeString stringOutput;
    number::FormattedNumber numberOutput;

    // Named options passed to functions
    LocalPointer<Hashtable> functionOptions;

    // Creates a new context with the given `MessageContext` as its parent
    static ExpressionContext* create(MessageContext&, UErrorCode&);
    // Creates a new context sharing this's context and parent
    ExpressionContext* create(UErrorCode&);

    const MessageContext& messageContext() const { return context; }

    // Resets input and output and uses existing fallback
    void setFallback();
    // Sets fallback string
    void setFallbackTo(const FunctionName&);
    void setFallbackTo(const VariableName&);
    void setFallbackTo(const MessageFormatDataModel::Literal&);
    // Sets the fallback string as input and exits the error state
    void promoteFallbackToInput();

    void setFunctionName(const FunctionName&, UErrorCode&);
    // Function name must be set; clears it
    void resolveSelector(Selector*);

    void setStringOption(const UnicodeString&, const UnicodeString&, UErrorCode&);
    void setDateOption(const UnicodeString&, UDate, UErrorCode&);
    void setNumericOption(const UnicodeString&, double, UErrorCode&);
    void setObjectOption(const UnicodeString&, const UObject*, UErrorCode&);

    void setNoOperand();
    void setInput(const UObject*);
    void setInput(const Formattable&);
    void setInput(const UnicodeString&);
    void setObjectInput(UObject*);
    void setOutput(const UnicodeString&);
    void setOutput(number::FormattedNumber&&);

    // If there is a function name, clear it and
    // call the function, setting the input and/or output appropriately
    // Precondition: hasFormatter()
    void evalFormatterCall(const FunctionName&, UErrorCode&);
    // If there is a function name, clear it and
    // call the function, setting the input and/or output appropriately
    // Precondition: hasSelector()
    void evalPendingSelectorCall(const UnicodeString[], int32_t, UnicodeString[], int32_t&, UErrorCode&);

    static Formattable* createFormattable(const UnicodeString&, UErrorCode&);
    static Formattable* createFormattable(double, UErrorCode&);
    static Formattable* createFormattable(int64_t, UErrorCode&);
    static Formattable* createFormattableDate(UDate, UErrorCode&);
    static Formattable* createFormattableDecimal(StringPiece, UErrorCode&);
    static Formattable* createFormattable(const UnicodeString*, int32_t, UErrorCode&);
    static Formattable* createFormattable(const UObject*, UErrorCode&);

    public:

    // Precondition: pending function name is set
    bool hasSelector() const;
    // Precondition: pending function name is set
    bool hasFormatter() const;

    bool isFallback() const;

    bool hasInput() const { return hasFormattableInput() || hasObjectInput(); }
    UBool hasFormattableInput() const;
    UBool hasObjectInput() const;
    const Formattable& getFormattableInput() const;
    const UObject& getObjectInput() const;

    UBool hasStringOutput() const;
    UBool hasNumberOutput() const;
    bool hasOutput() { return (hasStringOutput() || hasNumberOutput()); }
    // Just gets existing output, doesn't force evaluation
    const UnicodeString& getStringOutput() const;
    const number::FormattedNumber& getNumberOutput() const;
    // Forces evaluation
    void formatToString(const Locale&, UErrorCode&);

    UBool getStringOption(const UnicodeString&, UnicodeString&) const;
    UBool getDoubleOption(const UnicodeString&, double&) const;
    UBool getInt64Option(const UnicodeString&, int64_t&) const;
    UBool hasObjectOption(const UnicodeString&) const;
    const UObject& getObjectOption(const UnicodeString&) const;
    // Function options iterator
    int32_t firstOption() const;
    int32_t optionsCount() const;
    const Formattable* nextOption(int32_t&, UnicodeString&) const;

    void setSelectorError(const UnicodeString&, UErrorCode&);
    void setFormattingError(const UnicodeString&, UErrorCode&);

    virtual ~ExpressionContext();
};

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // MESSAGEFORMAT2_FORMATTING_CONTEXT_H

#endif // U_HIDE_DEPRECATED_API
// eof
