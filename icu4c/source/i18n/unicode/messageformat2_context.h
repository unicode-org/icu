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

// TODO: doc comments
// Represents the arguments to a message
class U_I18N_API MessageArguments : public UMemory {
  public:
    class Builder {
    public:
        Builder& add(const UnicodeString&, const UnicodeString&, UErrorCode&);
        Builder& addDouble(const UnicodeString&, double, UErrorCode&);
        Builder& addInt64(const UnicodeString&, int64_t, UErrorCode&);
        Builder& addLong(const UnicodeString&, long, UErrorCode&);
        Builder& addDate(const UnicodeString&, UDate, UErrorCode&);
        // Adds an array of strings
        // Adopts its argument
        Builder& add(const UnicodeString&, const UnicodeString*, size_t, UErrorCode&);
        // Does not adopt its UObject argument. Argument must be non-null
        Builder& addObject(const UnicodeString&, UObject*, UErrorCode&);
        // Does not invalidate the builder
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
