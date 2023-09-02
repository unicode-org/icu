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

// Intermediate classes used internally in the formatter
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

class Error : public UMemory {
    public:
    enum Type {
        DuplicateOptionName,
        UnresolvedVariable,
        FormattingWarning,
        MissingSelectorAnnotation,
        NonexhaustivePattern,
        ReservedError,
        SelectorError,
        SyntaxError,
        UnknownFunction,
        VariantKeyMismatchWarning
    };
    Error(Type ty) : type(ty) {}
    Error(Type ty, const Text& t) : type(ty), contents(t.toString()) {} 
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
    bool formattingWarning;
    bool missingSelectorAnnotationError;
    bool selectorError;
    bool syntaxError;
    bool unknownFunctionError;
    Errors(UErrorCode& errorCode);

    public:
    static Errors* create(UErrorCode&);

    size_t count() const;
    void addNonexhaustivePattern();
    void addDuplicateOption();
    void addSelectorError();
    void addMissingSelectorAnnotation();
    void addUnresolvedVariable(const VariableName&);
    void addVariantKeyMismatchWarning();
    void addSyntaxError(UErrorCode&);
    void addUnknownFunction(const FunctionName&);
    void addVariantKeyMismatch();
    void addFormattingError();
    bool hasDataModelError() const { return dataModelError; }
    bool hasFormattingWarning() const { return formattingWarning; }
    bool hasSelectorError() const { return selectorError; }
    bool hasSyntaxError() const { return syntaxError; }
    bool hasUnknownFunctionError() const { return unknownFunctionError; }
    bool hasMissingSelectorAnnotationError() const { return missingSelectorAnnotationError; }
    void addError(Error, UErrorCode&);
    void checkErrors(UErrorCode&);
    void clearResolutionAndFormattingErrors();

    virtual ~Errors();
}; // class Errors

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
         *
         * @internal ICU 74.0 technology preview
         * @deprecated This API is for technology preview only.
         */
        Builder& addDouble(const UnicodeString&, double, UErrorCode&);
        Builder& addInt64(const UnicodeString&, int64_t, UErrorCode&);
        Builder& addLong(const UnicodeString&, long, UErrorCode&);
        /**
         * Adds an argument of type `UDate`.
         *
         * @param key The name of the argument.
         * @param value The value of the argument.
         * @param status    Input/output error code.
         *
         * @internal ICU 74.0 technology preview
         * @deprecated This API is for technology preview only.
         */
        Builder& addDate(const UnicodeString&, UDate, UErrorCode&);
        /**
         * Adds an argument of type UnicodeString[]. Adopts `value`.
         *
         * @param key The name of the argument.
         * @param value The value of the argument, interpreted as an array of strings.
         * @param length The length of the array.
         * @param status  Input/output error code.
         *
         * @internal ICU 74.0 technology preview
         * @deprecated This API is for technology preview only.
         */
        Builder& add(const UnicodeString& key, const UnicodeString* value, size_t length, UErrorCode& status);
        /**
         * Adds an argument of type UObject*, which must be non-null. Does not
         * adopt this argument. `value` is not declared as const, but is treated
         * as if it was const.
         *
         * @param key The name of the argument.
         * @param value The value of the argument.
         * @param status  Input/output error code.
         *
         * @internal ICU 74.0 technology preview
         * @deprecated This API is for technology preview only.
         */
        Builder& addObject(const UnicodeString& key, UObject* value, UErrorCode& status);
        /**
         * Creates an immutable `MessageArguments` object with the argument names
         * and values that were added by previous calls. The builder can still be used
         * after this call.
         *
         * @param status  Input/output error code.
         *
         * @internal ICU 74.0 technology preview
         * @deprecated This API is for technology preview only.
         */
        MessageArguments* build(UErrorCode&) const;
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
    static Builder* builder(UErrorCode&);
  private:
    friend class Context;
    friend class MessageFormatter;

    bool has(const VariableName&) const;
    const Formattable& get(const VariableName&) const;

    MessageArguments& add(const UnicodeString&, Formattable*, UErrorCode&);
    MessageArguments(Hashtable* c, Hashtable* o) : contents(c), objectContents(o) {}
    LocalPointer<Hashtable> contents;
    // Keep a separate hash table for objects, which does not
    // own the values
    LocalPointer<Hashtable> objectContents;
}; // class MessageArguments

// Map from expression pointers to Formatters
class CachedFormatters : public UMemory {
private:
    friend class Context;
    friend class MessageFormatter;
    
    LocalPointer<Hashtable> cache;
    
    const Formatter* getFormatter(const FunctionName&);
    void setFormatter(const FunctionName&, Formatter*, UErrorCode& errorCode);
    CachedFormatters(UErrorCode&);
};

// Context needed for formatting an expression
class MessageFormatter;

class Context : public UMemory {
public:
    bool hasVar(const VariableName&) const;
    const Formattable& getVar(const VariableName& var) const;
    
    static Context* create(const MessageFormatter& mf, const MessageArguments& args, Errors& errors, UErrorCode& errorCode);

    void setFormattingWarning(const FunctionName&, UErrorCode&);
    void setSelectorError(const FunctionName&, UErrorCode&);
    void setUnresolvedVariableWarning(const VariableName&, UErrorCode&);
    void setUnknownFunctionWarning(const FunctionName&, UErrorCode&);
    // If any errors were set, update `status` accordingly
    void checkErrors(UErrorCode& status) const;
    Errors& getErrors() { return errors; }
    bool hasParseError() const;
    bool hasDataModelError() const;
    bool hasMissingSelectorAnnotationError() const;
    bool hasUnknownFunctionError() const;
    bool hasFormattingWarning() const;
    bool hasSelectorError() const;
    bool hasError() const;
    void addError(Error, UErrorCode&);

    virtual ~Context();
    
private:
    Context(const MessageFormatter&, const MessageArguments&, Errors&);
    FormatterFactory* lookupFormatterFactory(const FunctionName&, UErrorCode&);

    const MessageFormatter& parent;
    const MessageArguments& arguments; // External message arguments
    // Errors accumulated during parsing/formatting
    Errors& errors;
}; // class Context

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // MESSAGEFORMAT2_CONTEXT_H

#endif // U_HIDE_DEPRECATED_API
// eof
