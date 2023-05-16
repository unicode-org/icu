// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef U_HIDE_DEPRECATED_API

#ifndef MESSAGEFORMAT_DATA_MODEL_H
#define MESSAGEFORMAT_DATA_MODEL_H

#if U_SHOW_CPLUSPLUS_API

#if !UCONFIG_NO_FORMATTING

#include "unicode/unistr.h"
#include "unicode/utypes.h"
#include "uhash.h"
#include "uvector.h"

U_NAMESPACE_BEGIN  namespace message2 {

// TBD
using FunctionName   = UnicodeString;
// TBD
using VariableName   = UnicodeString;
// TBD
using String         = UnicodeString;

// -----------------------------------------------------------------------
// Utilities (not public)

class Operator;
template <typename T>
class List : public UMemory {
    // Provides a wrapper around a vector
  public:
    List(); // Creates an empty list
    bool isEmpty();
    size_t length();
    // Out-of-bounds is an internal error
    void get(size_t, T &);
    // adding *copies* the thing being added
    void add(T, UErrorCode errorCode);
  private:
    friend class Operator;
    // Only used by move assignment operator for Operator
    void clear() { contents = nullptr; }
    UVector* contents;
};

template <typename T>
class Optional : public UMemory {
  public:
    Optional(); // "None" constructor
    Optional(T &); // "Some constructor
    bool isNone() { return contents == nullptr; }
    bool isSome() { return !isNone(); }
    void get(T &); // internal error if None
  private:
    T* contents;
};


// -----------------------------------------------------------------------
// Helpers (not public)

class Expression;

// For now, represent variable names as strings
#define hashVariableName    uhash_hashUnicodeString
#define compareVariableName uhash_compareUnicodeString

class Environment : public UMemory {
  // Provides a wrapper around a hash table
  public:
    // Looks up a variable, returning true iff it's found in the environment.
    // The UErrorCode is used for signaling other errors, e.g. `bindings` being
    // null.
    // TODO ^ That should probably be an assert. would we want to use the error
    // code here at all?
    bool lookup(const VariableName &, UErrorCode &, Expression &result);

    // Precondition: variable name is not already defined
    // The environment takes ownership of the expression.
    // The error code is used to signal a memory allocation error.
    void define(const VariableName &, const Expression &, UErrorCode &);

    // Precondition: variable name is already defined
    void redefine(const VariableName &, const Expression &, UErrorCode &);

    // Creates an empty environment
    Environment(UErrorCode &errorCode) {
        if (U_FAILURE(errorCode)) {
            // Won't be valid if there already was an error.
            bindings = nullptr;
            return;
        }
        // No value comparator needed
        bindings = uhash_open(hashVariableName, compareVariableName, nullptr, &errorCode);
    }
    // Delete default constructor - need an error code
    Environment() = delete;
    ~Environment() { uhash_close(bindings); }
  private:
    UHashtable* bindings;
};

class Operand : public UMemory {
    // An operand can either be an uninterpreted string,
    // or a variable reference.
    // TODO: do we want to distinguish literals from strings?
    // (Compare other implementations)
  public:
    Operand(bool b, String s) : isVariableReference(b), string(s) {}
    // TODO: leaving this in for convenience for now, but it would be
    // better for all types to be immutable
    Operand() : isVariableReference(false), string("") {}

  private:
    // TODO
    /* const */ bool isVariableReference;
    /* const */ String string;
};

class Option : public UMemory {
    // Represents a single name-value pair
  public:
    Option(String s, Operand v) : name(s), value(v) {}
  private:
    const String name;
    const Operand value;
};

using OptionList = List<Option>;
class Operator : public UMemory {
    // An operator represents a function name together with
    // a list of options, which may be empty.
  public:
    Operator(FunctionName f, OptionList l) : functionName(f), options(l) {}
    // TODO: leaving this in for convenience for now, but it would be
    // better for all types to be immutable
    Operator() : functionName(""), options(OptionList()) {}

  private:
    // TODO
    /* const */ FunctionName functionName;
    // TODO: should probably be a hash
    /* const */ OptionList options;
};

class Expression : public UMemory {
    /*
      An expression is the application of an optional operator to an optional operand.
      For example (using a made-up quasi-s-expression notation):

      { |42| :fun opt=value } => Expression(operator=Some(fun, {opt: value}),
                                            operand=Some(Literal(42)))
      abcd                    => Expression(operator=None, operand=Some(String("abcd")))
      { : fun opt=value }     => Expression(operator=Some(fun, {opt: value}),
                                            operand=None)

      An expression where both operand and operator are None can't be constructed.
    */
  public:
    Expression(Operator rAtor, Operand rAnd)
        : rator(Optional<Operator>(rAtor)), rand(Optional<Operand>(rAnd)) {}
    Expression(Operator rAtor)
        : rator(Optional<Operator>(rAtor)), rand(Optional<Operand>()) {}
    Expression(Operand rAnd)
        : rator(Optional<Operator>()), rand(Optional<Operand>(rAnd)) {}
    
    // TODO: leaving this in for convenience for now, but it would be
    // better for all types to be immutable
    Expression() : rator(Optional<Operator>()), rand(Optional<Operand>()) {}

  private:
    // TODO
    /* const */ Optional<Operator> rator;
    /* const */ Optional<Operand> rand;
};

class Key : public UMemory {
    // A key is either a string or the "wildcard" symbol.
    // TODO: same question, distinguish literals from strings?
  public:
    Key();           // wildcard constructor
    Key(String &s);  // concrete key constructor
    bool isWildcard();
    void getString(String&); // internal error if this is a wildcard
  private:
    // TODO
    /* const */ bool wildcard; // True if this represents the wildcard "*"
    /* const */ String contents;
};

using KeyList = List<Key>;

class Variant : public UMemory {
    /*
      A variant represents a single `when`-clause in a selectors.
      Unlike in the grammar, the key list can be empty. This lets us
      desugar `pattern`s into a `selectors`.
     */
  public:
    // Special case used to represent patterns: creates a `when` with
    // no keys
    Variant(Expression &expr) : keys(KeyList()), expression(expr) {}
    // Usual case; takes ownership of `keys`
    Variant(KeyList ks, Expression &expr) : keys(ks), expression(expr) {}
  private:
    
    const KeyList keys;
    // TODO: this subsumes `text` under `expression`, which the grammar doesn't do,
    // by using `expression` to represent a pattern; I think that's okay to do, though
    const Expression expression;
};

using ExpressionList = List<Expression>;
using VariantList    = List<Variant>;

class MessageBody : public UMemory {
  public:
    // Constructs a body out of a single Expression, which represents
    // a `pattern` in the grammar
    MessageBody(Expression &expr, UErrorCode &errorCode)
        : scrutinees(ExpressionList()), variants(VariantList()) {
        variants.add(Variant(expr), errorCode);
    }
    // Constructs a body out of a list of scrutinees (expressions) and
    // a list of variants, which represents the `(selectors 1*([s] variant))`
    // alternative in the grammar
    MessageBody(ExpressionList es, VariantList vs) : scrutinees(es), variants(vs) {}


    // TODO: leave this in for convenience; would be better for this to be
    // immutable
    MessageBody() {}

  private:
    friend class MessageFormatDataModel;
    
    /*
      A message body is a `selectors` construct as in the grammar.
      A bare pattern is represented as a `selectors` with no scrutinees
      and a single `when`-clause with empty keys.
     */

    // The expressions that are being matched on. May be empty.
    // (If it's empty, there must be exactly one `when`-clause with empty
    // keys.)
    ExpressionList scrutinees;

    // The list of `when` clauses (case arms).
    VariantList variants;
};

// -----------------------------------------------------------------------
// Public MessageFormatDataModel class

/**
 * <p>MessageFormat2 is a Technical Preview API implementing MessageFormat 2.0.
 * Since it is not final, documentation has not yet been added everywhere.
 *
 * <p>See <a target="github"
href="https://github.com/unicode-org/message-format-wg/blob/main/spec/syntax.md">the
 * description of the syntax with examples and use cases</a> and the corresponding
 * <a target="github"
href="https://github.com/unicode-org/message-format-wg/blob/main/spec/message.abnf">ABNF</a> grammar.</p>
 *
 * The `MessageFormatDataModel` class describes a parsed representation of the text of a message.
 * This representation is public as higher-level APIs for messages will need to know its public
 * interface: for example, to re-instantiate a parsed message with different values for imported
variables.
 *
 * @internal ICU 74.0 technology preview
 * @deprecated This API is for technology preview only.
 */
class MessageFormat2;

class U_I18N_API MessageFormatDataModel : public UMemory {
  friend MessageFormat2;

  public:
    // TODO: the public operations will be things like changing/adding bindings.
    // Only a MessageFormat2 itself can call the constructor

    // TODO: this is left here for convenience; the MessageFormat2 constructor calls parse(),
    // which initializes the data model, and that doesn't work unless there's a default
    // constructor. Is there a better way?
    // Needs a UErrorCode because it initializes its environment
    MessageFormatDataModel(UErrorCode &status) : env(status) {}

    virtual ~MessageFormatDataModel();

  private:

    /*
      A parsed message consists of an environment and a body.
      Initially, the environment contains bindings for local variables
      (those declared with `let` in the message). API calls can extend
      the environment with new bindings or change the values of existing ones.

      Once the data model is constructed, only the environment can be mutated.
      (It's constructed bottom-up.)
    */
    Environment env;

    /*
      See the `MessageBody` class.
     */
    // TODO
 /*   const */ MessageBody body;

    // TODO: Maybe not needed
    // Takes ownership of the environment and body
//    MessageFormatDataModel(Environment& env, const MessageBody& body, UErrorCode &status);
    
    // Do not define default assignment operator
    const MessageFormatDataModel &operator=(const MessageFormatDataModel &) = delete;
    
}; // class MessageFormatDataModel

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // MESSAGEFORMAT_DATA_MODEL_H

#endif // U_HIDE_DEPRECATED_API
// eof

