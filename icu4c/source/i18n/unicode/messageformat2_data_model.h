// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef U_HIDE_DEPRECATED_API

#ifndef MESSAGEFORMAT_DATA_MODEL_H
#define MESSAGEFORMAT_DATA_MODEL_H

#if U_SHOW_CPLUSPLUS_API

#if !UCONFIG_NO_FORMATTING

#include "unicode/unistr.h"
#include "unicode/utypes.h"
#include "hash.h"
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

template <typename T>
class List;
// Mutable list: used for building up results in the parser
template <typename T>
class ListBuilder : public UMemory {
    // Provides a wrapper around a vector
  public:
    // Creates an empty list
    ListBuilder() {
        // Initially set to null (so the empty list constructor
        // doesn't need to take an error code)
        contents = nullptr;
    }
    // adding adopts the thing being added
    void add(T* element, UErrorCode errorCode) {
        if (U_FAILURE(errorCode)) {
            return;
        }
        // If this is the first element being added,
        // initialize `contents`
        if (contents == nullptr) {
            contents = new UVector(errorCode);
        }
        // If creating the new vector succeeded, add the
        // element
        if (U_SUCCESS(errorCode)) {
            contents->adoptElement(element, errorCode);
        }
    }
  private:
    friend class List<T>;
    UVector* contents;
};

// Immutable list
template <typename T>
class List : public UMemory {
    // Provides a wrapper around a vector
  public:
    // Adopts the contents of `builder`
    List(ListBuilder<T> *builder) {
        // Precondition: builder's contents is non-null
        U_ASSERT(builder->contents != nullptr);
        // Copy the pointer to `contents`
        contents = builder->contents;
        // Null out builder's pointer to its content
        // and delete it
        builder->contents = nullptr;
        delete builder;
    }
    // Initializes to an immutable singleton list
    // (Adopts its argument)
    List(T *item, UErrorCode errorCode) {
        LocalPointer<T> adoptedItem(item);
        if (U_FAILURE(errorCode)) {
            // The adoptedItem destructor deletes `item`
        } else {
            contents = new UVector(errorCode);
            if (U_FAILURE(errorCode)) {
                // The adoptedItem destructor deletes `item`
            } else {
                contents->adoptElement(adoptedItem.orphan(), errorCode);
            }
        }
    }
    // Initializes to an immutable empty list
    List() : contents(nullptr) {}
    bool isEmpty() const { return (contents == nullptr); }
    size_t length() const { return (contents == nullptr ? 0 : contents->size()); }
    // Out-of-bounds is an internal error
    void get(size_t i, T & result) const {
        U_ASSERT(!(isEmpty() || i >= length()));
        result = *(static_cast<T*>(contents->elementAt(i)));
    }
  private:
    // Not marked const, so that the singleton constructor works.
    // But there are no mutator methods.
    /* const */ UVector* contents;
};

// -----------------------------------------------------------------------
// Helpers (not public)

class Expression;

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

    bool isVariable() const { return isVariableReference; }
    VariableName asVariable() const {
        U_ASSERT(isVariable());
        return string;
    }
    bool isLiteral() const { return !isVariable(); }
    String asLiteral() const {
        U_ASSERT(isLiteral());
        return string;
    }
  private:
    const bool isVariableReference;
    const String string;
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
    // Adopts `l`
    Operator(FunctionName f, OptionList* l) : functionName(f), options(l) {}

    FunctionName getFunctionName() const { return functionName; }
    const OptionList& getOptions() const { return *options; }

  private:
    const FunctionName functionName;
    const OptionList* options;
};

class PatternPart : public UMemory {
    // Either Expression or TextPart can show up in a pattern
};

class Expression : public PatternPart {
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
    // All constructors adopt their arguments
    // Both operator and operands must be non-null
    Expression(Operator *rAtor, Operand *rAnd)
        : rator(rAtor), rand(rAnd) {  U_ASSERT(rAtor != nullptr && rAnd != nullptr); }
    // Operator must be non-null
    Expression(Operator *rAtor) : rator(rAtor) { U_ASSERT(rAtor != nullptr); }
    // Operand must be non-null
    Expression(Operand *rAnd) : rand(rAnd) { U_ASSERT(rAnd != nullptr); }
    
    // TODO: leaving this in for convenience for now, but it would be
    // better for all types to be immutable
    Expression() : rator(nullptr), rand(nullptr) {}


    bool isStandaloneAnnotation() const { return (rand == nullptr); }
    bool isFunctionCall() const         { return (rator != nullptr && rand != nullptr); }
    /*
      TODO: I thought about using an Optional class here instead of nullable
      pointers. But that doesn't really work since we can't use the STL and therefore
      can't use `std::move` and therefore can't use move constructors/move assignment
      operators.
     */
  private:
    const Operator* rator;
    const Operand* rand;
};

class Key : public UMemory {
    // A key is either a string or the "wildcard" symbol.
    // TODO: same question, distinguish literals from strings?
  public:
    // wildcard constructor
    Key() : wildcard(true) {}
    // concrete key constructor
    Key(String &s) : wildcard(false), contents(s) {}
    bool isWildcard() const { return wildcard; }
    // internal error if this is a wildcard
    String getString() const {
        U_ASSERT(!isWildcard());
        return contents;
    }
  private:
    const bool wildcard; // True if this represents the wildcard "*"
    const String contents;
};

class Text : public PatternPart {
  public:
    Text(String &s) : text(s) {}
  private:
    const String text;
};

// using PatternPartList = List<PatternPart>;
using ExpressionList = List<Expression>;

class Pattern : public UMemory {
 public:
    // Takes ownership of `ps`
    Pattern(ExpressionList *ps) : parts(ps) {
        U_ASSERT(ps != nullptr);
    }
    size_t numParts() const { return parts->length(); }
    // Precondition: i < numParts()
    void getPart(size_t i, Expression& result) const {
        U_ASSERT(i < numParts());
        parts->get(i, result);
    }
 private:
    // Possibly-empty list of parts
    // Note: a "text" thing is represented like a literal, so that's an expression too.
    // TODO: compare and see how other implementations distinguish text / literal / nmtoken
    const ExpressionList* parts;
};

using KeyList = List<Key>;

class Variant : public UMemory {
    /*
      A variant represents a single `when`-clause in a selectors.
      Unlike in the grammar, the key list can be empty. This lets us
      desugar `pattern`s into a `selectors`.
     */
  public:
    // Special case used to represent top-level `pattern` bodies:
    // creates a `when` with no keys
    // Adopts `p`
    Variant(Pattern *p, UErrorCode &errorCode) {
        LocalPointer<Pattern> adoptedPattern(p);
        if (U_FAILURE(errorCode)) {
            // the adoptedPattern destructor deletes the pattern
        } else {
            keys = new KeyList();
            if (keys == nullptr) {
                errorCode = U_MEMORY_ALLOCATION_ERROR;
                // the adoptedPattern destructor deletes the pattern
            } else {
                pattern = adoptedPattern.orphan();
            }
        }
    }
    // Usual case; takes ownership of `keys` and `p`
    Variant(KeyList* ks, Pattern* p) : keys(ks), pattern(p) {}
  private:
    
    const KeyList* keys;
    const Pattern* pattern;
};

using VariantList    = List<Variant>;

class MessageBody : public UMemory {
  public:
    // Constructs a body out of a single Pattern
    // (body -> pattern alternative in the grammar)
    // a `pattern` in the grammar
    MessageBody(Pattern *pattern, UErrorCode &errorCode) {
        LocalPointer<Pattern> adoptedPattern(pattern);
        if (U_FAILURE(errorCode)) {
            // The adoptedPattern destructor deletes the pattern
        } else {
            LocalPointer<Variant> patternVariant(new Variant(pattern, errorCode));
            if (!patternVariant.isValid()) {
                errorCode = U_MEMORY_ALLOCATION_ERROR;
                // The adoptedPattern destructor deletes the pattern
            } else {
                variants = new VariantList(patternVariant.orphan(), errorCode);
                if (variants == nullptr) {
                    errorCode = U_MEMORY_ALLOCATION_ERROR;
                    // The adoptedPattern destructor deletes the pattern
                } else if (U_SUCCESS(errorCode)) {
                    // `variants` adopted the pattern
                    LocalPointer<ExpressionList> adoptedScrutinees(new ExpressionList());
                    if (!adoptedScrutinees.isValid()) {
                        errorCode = U_MEMORY_ALLOCATION_ERROR;
                    } else {
                        adoptedPattern.orphan();
                        scrutinees = adoptedScrutinees.orphan();
                    }
                }
            }
        }
    }
    // Constructs a body out of a list of scrutinees (expressions) and
    // a list of variants, which represents the `(selectors 1*([s] variant))`
    // alternative in the grammar
    // Adopts its arguments
    MessageBody(ExpressionList *es, VariantList *vs) : scrutinees(es), variants(vs) {}

    // TODO: see comment on MessageFormatDataModel constructor
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
    ExpressionList* scrutinees;

    // The list of `when` clauses (case arms).
    VariantList* variants;
};

// For now, represent variable names as strings
#define compareVariableName uhash_compareUnicodeString

class Environment : public UMemory {
  // Provides a wrapper around a hash table
  public:
    // Returns true iff the variable is defined
    bool defined(const VariableName &v) {
        U_ASSERT(bindings != nullptr);
        return bindings->containsKey(v);
    }
    // Looks up a variable, returning true iff it's found in the environment.
    bool lookup(const VariableName &v, Expression &result) {
        void* maybeExpr = bindings->get(v);
        if (maybeExpr == nullptr) {
            return false;
        }
        result = *(static_cast<Expression*>(maybeExpr));
        return true;
    }

    // Precondition: variable name is not already defined
    // The environment takes ownership of the expression.
    // The error code is used to signal a memory allocation error.
    void define(const VariableName &v, Expression* e, UErrorCode &errorCode) {
        if (U_FAILURE(errorCode)) {
            return;
        }
        // The assert ensures that the variable name was not already defined
        U_ASSERT(bindings->put(v, e, errorCode) == nullptr);
    }

    // Precondition: variable name is already defined
    void redefine(const VariableName &v, Expression* e, UErrorCode &errorCode) {
        if (U_FAILURE(errorCode)) {
            return;
        }
        // The assert ensures that the variable name was already defined
        U_ASSERT(bindings->put(v, e, errorCode) != nullptr);
    }

    // Creates an empty environment
    Environment(UErrorCode &errorCode) {
        if (U_FAILURE(errorCode)) {
            // Won't be valid if there already was an error.
            bindings = nullptr;
            return;
        }
        // No value comparator needed
        LocalPointer<Hashtable> e(new Hashtable(compareVariableName, nullptr, errorCode));
        if (U_FAILURE(errorCode)) {
            bindings = nullptr;
            return;
        }
        bindings = e.orphan();
        // The environment owns the values
        bindings->setValueDeleter(uprv_deleteUObject);
    }
    // Delete default constructor - need an error code
    Environment() = delete;
  private:
    Hashtable* bindings;
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
    MessageFormatDataModel(UErrorCode &status) {
        if (U_FAILURE(status)) {
            return;
        }
        LocalPointer<Environment> envLocal(new Environment(status));
        if (U_FAILURE(status)) {
            return;
        }
        env = envLocal.orphan();
    }
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
    Environment* env;

    /*
      See the `MessageBody` class.
     */
    const MessageBody* body;

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

