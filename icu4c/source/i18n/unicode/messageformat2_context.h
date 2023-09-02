// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef U_HIDE_DEPRECATED_API

#ifndef MESSAGEFORMAT2_CONTEXT_H
#define MESSAGEFORMAT2_CONTEXT_H

#if U_SHOW_CPLUSPLUS_API

/**
 * \file
 * \brief C++ API: Formats messages using the draft MessageFormat 2.0.
 */

#if !UCONFIG_NO_FORMATTING

#include "unicode/messageformat2_data_model.h"
#include "unicode/utypes.h"

U_NAMESPACE_BEGIN namespace message2 {

class Formatter;
class FormatterFactory;
class SelectorFactory;

using FunctionName = MessageFormatDataModel::FunctionName;
using VariableName = MessageFormatDataModel::VariableName;

// Intermediate classes used internally in the formatter

// Closures and environments
// -------------------------

class Environment;

// A closure represents the right-hand side of a variable
// declaration, along with an environment giving values
// to its free variables
class Closure : public UMemory {
    using Expression = MessageFormatDataModel::Expression;
public:
    static Closure* create(const Expression&, const Environment&, UErrorCode&);
    const Expression& getExpr() const {
        return expr;
    }
    const Environment& getEnv() const {
        return env;
    }
    virtual ~Closure();
private:
    Closure(const Expression& expression, const Environment& environment) : expr(expression), env(environment) {}

    // An unevaluated expression
    const Expression& expr;
    // The environment mapping names used in this
    // expression to other expressions
    const Environment& env;
};

// An environment is represented as a linked chain of
// non-empty environments, terminating at an empty environment.
// It's searched using linear search.
class Environment : public UMemory {
public:
    virtual const Closure* lookup(const VariableName&) const = 0;
    static Environment* create(UErrorCode&);
    static Environment* create(const VariableName&, Closure*, const Environment&, UErrorCode&);
    virtual ~Environment();
};

class NonEmptyEnvironment;
class EmptyEnvironment : public Environment {
private:
    friend class Environment;

    const Closure* lookup(const VariableName&) const;
    static EmptyEnvironment* create(UErrorCode&);
    virtual ~EmptyEnvironment();
    // Adopts its closure argument
    static NonEmptyEnvironment* create(const VariableName&, Closure*, const Environment&, UErrorCode&);

    EmptyEnvironment() {}
};

class NonEmptyEnvironment : public Environment {
private:
    friend class Environment;
    const Closure* lookup(const VariableName&) const;
    // Adopts its closure argument
    static NonEmptyEnvironment* create(const VariableName&, Closure*, const Environment&, UErrorCode&);
    virtual ~NonEmptyEnvironment();
private:
    friend class Environment;

    NonEmptyEnvironment(const VariableName& v, Closure* c, const Environment& e) : var(v), rhs(c), parent(e) {}

    // Maps VariableName onto Closure*
    // Chain of linked environments
    VariableName var;
    const LocalPointer<Closure> rhs; // should be valid
    const Environment& parent;
};

// Errors
// ----------

class Error : public UMemory {
    public:
    enum Type {
        DuplicateOptionName,
        UnresolvedVariable,
        FormattingError,
        MissingSelectorAnnotation,
        NonexhaustivePattern,
        ReservedError,
        SelectorError,
        SyntaxError,
        UnknownFunction,
        VariantKeyMismatchError
    };
    Error(Type ty) : type(ty) {}
    Error(Type ty, const UnicodeString& s) : type(ty), contents(s) {}
    virtual ~Error();
    private:
    friend class Errors;

    Type type;
    UnicodeString contents;
}; // class Error

class Errors : public UMemory {
    private:
    LocalPointer<UVector> syntaxAndDataModelErrors;
    LocalPointer<UVector> resolutionAndFormattingErrors;
    bool dataModelError;
    bool formattingError;
    bool missingSelectorAnnotationError;
    bool selectorError;
    bool syntaxError;
    bool unknownFunctionError;
    bool unresolvedVariableError;
    Errors(UErrorCode& errorCode);

    public:
    static Errors* create(UErrorCode&);

    int32_t count() const;
    void setSelectorError(const FunctionName&, UErrorCode&);
    void setReservedError(UErrorCode&);
    void setMissingSelectorAnnotation(UErrorCode&);
    void setUnresolvedVariable(const VariableName&, UErrorCode&);
    void addSyntaxError(UErrorCode&);
    void setUnknownFunction(const FunctionName&, UErrorCode&);
    void setFormattingError(const FunctionName&, UErrorCode&);
    bool hasDataModelError() const { return dataModelError; }
    bool hasFormattingError() const { return formattingError; }
    bool hasSelectorError() const { return selectorError; }
    bool hasSyntaxError() const { return syntaxError; }
    bool hasUnknownFunctionError() const { return unknownFunctionError; }
    bool hasMissingSelectorAnnotationError() const { return missingSelectorAnnotationError; }
    bool hasUnresolvedVariableError() const { return unresolvedVariableError; }
    void addError(Error, UErrorCode&);
    void checkErrors(UErrorCode&);
    void clearResolutionAndFormattingErrors();
    bool hasError() const;

    virtual ~Errors();
}; // class Errors

// Arguments
// ----------

/**
 * <p>MessageFormatter is a Technical Preview API implementing MessageFormat 2.0.
 * Since it is not final, documentation has not yet been added everywhere.
 *
 * The following class represents the named arguments to a message.
 *
 * @internal ICU 74.0 technology preview
 * @deprecated This API is for technology preview only.
 */

class U_I18N_API MessageArguments : public UMemory {
  public:
    /**
     * The mutable Builder class allows each message argument to be initialized
     * separately; calling its `build()` method yields an immutable MessageArguments.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    class Builder {
    public:
        /**
         * Adds an argument of type `UnicodeString`.
         *
         * @param key The name of the argument.
         * @param value The value of the argument.
         * @param status    Input/output error code.
         * @return          A reference to the builder.
         *
         * @internal ICU 74.0 technology preview
         * @deprecated This API is for technology preview only.
         */
        Builder& add(const UnicodeString& key, const UnicodeString& value, UErrorCode& status);
        /**
         * Adds an argument of type `double`.
         *
         * @param key The name of the argument.
         * @param value The value of the argument.
         * @param status    Input/output error code.
         * @return          A reference to the builder.
         *
         * @internal ICU 74.0 technology preview
         * @deprecated This API is for technology preview only.
         */
        Builder& addDouble(const UnicodeString& key, double value, UErrorCode& status);
        /**
         * Adds an argument of type `int64_t`.
         *
         * @param key The name of the argument.
         * @param value The value of the argument.
         * @param status    Input/output error code.
         * @return          A reference to the builder.
         *
         * @internal ICU 74.0 technology preview
         * @deprecated This API is for technology preview only.
         */
        Builder& addInt64(const UnicodeString& key, int64_t value, UErrorCode& status);
        /**
         * Adds an argument of type `UDate`.
         *
         * @param key The name of the argument.
         * @param value The value of the argument.
         * @param status    Input/output error code.
         * @return          A reference to the builder.
         *
         * @internal ICU 74.0 technology preview
         * @deprecated This API is for technology preview only.
         */
        Builder& addDate(const UnicodeString& key, UDate value, UErrorCode& status);
        /**
         * Adds an argument of type `StringPiece`, representing a
         * decimal number.
         *
         * @param key The name of the argument.
         * @param value The value of the argument.
         * @param status    Input/output error code.
         * @return          A reference to the builder.
         *
         * @internal ICU 74.0 technology preview
         * @deprecated This API is for technology preview only.
         */
        Builder& addDecimal(const UnicodeString& key, StringPiece value, UErrorCode& status);
        /**
         * Adds an argument of type UnicodeString[]. Adopts `value`.
         *
         * @param key The name of the argument.
         * @param value The value of the argument, interpreted as an array of strings.
         * @param length The length of the array.
         * @param status  Input/output error code.
         * @return        A reference to the builder.
         *
         * @internal ICU 74.0 technology preview
         * @deprecated This API is for technology preview only.
         */
        Builder& add(const UnicodeString& key, const UnicodeString* value, int32_t length, UErrorCode& status);
        /**
         * Adds an argument of type UObject*, which must be non-null. Does not
         * adopt this argument.
         *
         * @param key The name of the argument.
         * @param value The value of the argument.
         * @param status  Input/output error code.
         * @return        A reference to the builder.
         *
         * @internal ICU 74.0 technology preview
         * @deprecated This API is for technology preview only.
         */
        Builder& addObject(const UnicodeString& key, const UObject* value, UErrorCode& status);
        /**
         * Creates an immutable `MessageArguments` object with the argument names
         * and values that were added by previous calls. The builder can still be used
         * after this call.
         *
         * @param status  Input/output error code.
         * @return        The new MessageArguments object, which is non-null if U_SUCCESS(status).
         *
         * @internal ICU 74.0 technology preview
         * @deprecated This API is for technology preview only.
         */
        MessageArguments* build(UErrorCode& status) const;
    private:
        friend class MessageArguments;
        Builder(UErrorCode&);
        Builder& add(const UnicodeString&, Formattable*, UErrorCode&);
        LocalPointer<Hashtable> contents;
        // Keep a separate hash table for objects, which does not
        // own the values
        // This is because a Formattable that wraps an object can't
        // be copied
        LocalPointer<Hashtable> objectContents;
    }; // class MessageArguments::Builder

    /**
     * Returns a new `MessageArguments::Builder` object.
     *
     * @param status  Input/output error code.
     * @return        The new builder, which is non-null if U_SUCCESS(status).
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    static Builder* builder(UErrorCode& status);
  private:
    friend class MessageContext;

    bool has(const VariableName&) const;
    const Formattable& get(const VariableName&) const;

    MessageArguments& add(const UnicodeString&, Formattable*, UErrorCode&);
    MessageArguments(Hashtable* c, Hashtable* o) : contents(c), objectContents(o) {}
    LocalPointer<Hashtable> contents;
    // Keep a separate hash table for objects, which does not
    // own the values
    LocalPointer<Hashtable> objectContents;
}; // class MessageArguments

// Formatter cache
// --------------

// Map from expression pointers to Formatters
class CachedFormatters : public UMemory {
private:
    friend class MessageFormatter;
    
    LocalPointer<Hashtable> cache;
    CachedFormatters(UErrorCode&);

public:
    const Formatter* getFormatter(const FunctionName&);
    void setFormatter(const FunctionName&, Formatter*, UErrorCode& errorCode);
};

// The context contains all the information needed to process
// an entire message: arguments, formatter cache, and error list

class MessageFormatter;

class MessageContext : public UMemory {
public:
    bool hasVar(const VariableName&) const;
    const Formattable& getVar(const VariableName& var) const;
    
    static MessageContext* create(const MessageFormatter& mf, const MessageArguments& args, Errors& errors, UErrorCode& errorCode);

    bool isCustomFormatter(const FunctionName&) const;
    const Formatter* maybeCachedFormatter(const FunctionName&, UErrorCode&);
    const SelectorFactory* lookupSelectorFactory(const FunctionName&, UErrorCode& status) const;
    bool isSelector(const FunctionName& fn) const { return isBuiltInSelector(fn) || isCustomSelector(fn); }
    bool isFormatter(const FunctionName& fn) const { return isBuiltInFormatter(fn) || isCustomFormatter(fn); }

    bool hasGlobal(const VariableName& v) const { return hasGlobalAsFormattable(v) || hasGlobalAsObject(v); }
    bool hasGlobalAsFormattable(const VariableName&) const;
    bool hasGlobalAsObject(const VariableName&) const;
    const Formattable& getGlobalAsFormattable(const VariableName&) const;
    const UObject* getGlobalAsObject(const VariableName&) const;

    // If any errors were set, update `status` accordingly
    void checkErrors(UErrorCode& status) const;
    Errors& getErrors() const { return errors; }

    const MessageFormatter& messageFormatter() const { return parent; }

    virtual ~MessageContext();
    
private:
    MessageContext(const MessageFormatter&, const MessageArguments&, Errors&);

    FormatterFactory* lookupFormatterFactory(const FunctionName&, UErrorCode&) const;
    bool isBuiltInSelector(const FunctionName&) const;
    bool isBuiltInFormatter(const FunctionName&) const;
    bool isCustomSelector(const FunctionName&) const;

    const MessageFormatter& parent;
    const MessageArguments& arguments; // External message arguments
    // Errors accumulated during parsing/formatting
    Errors& errors;
}; // class MessageContext

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // MESSAGEFORMAT2_CONTEXT_H

#endif // U_HIDE_DEPRECATED_API
// eof
