// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef U_HIDE_DEPRECATED_API

#ifndef MESSAGEFORMAT_DATA_MODEL_H
#define MESSAGEFORMAT_DATA_MODEL_H

#if U_SHOW_CPLUSPLUS_API

#if !UCONFIG_NO_FORMATTING

#include "unicode/parseerr.h"
#include "unicode/unistr.h"
#include "unicode/utypes.h"
#include "hash.h"
#include "uvector.h"

U_NAMESPACE_BEGIN  namespace message2 {

/*
  Use an internal "parse error" structure to make it easier to translate
  absolute offsets to line offsets.
  This is translated back to a `UParseError` at the end of parsing.
*/
typedef struct MessageParseError {
    // The line on which the error occurred
    uint32_t line;
    // The offset, relative to the erroneous line, on which the error occurred
    uint32_t offset;
    // The total number of characters seen before advancing to the current line. It has a value of 0 if line == 0.
    // It includes newline characters, because the index does too.
    uint32_t lengthBeforeCurrentLine;

    // This parser doesn't yet use the last two fields.
    UChar   preContext[U_PARSE_CONTEXT_LEN];
    UChar   postContext[U_PARSE_CONTEXT_LEN];
} MessageParseError;

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
            contents->setDeleter(uprv_deleteUObject);
            contents->adoptElement(element, errorCode);
        }
    }
    bool isEmpty() const { return (contents == nullptr); }
  private:
    friend class List<T>;
    UVector* contents;
};

/*
This exists because even though AST nodes are immutable, it's
possible someone could use build() to create data models that have
different lifetimes
 */
template<typename T>
void copyElements(UElement *dst, UElement *src) {
    dst->pointer = new T(*(static_cast<T*>(src->pointer)));
}
 
// Immutable list
template <typename T>
class List : public UMemory {
    // Provides a wrapper around a vector
  public:
    // Copies the contents of `builder`
    // This won't compile unless T is a type that has a copy assignment operator
    List(const ListBuilder<T>& builder, UErrorCode& errorCode) {
        // builder->contents == null means the empty list
        if (builder.contents == nullptr) {
            contents = nullptr;
        } else {
            contents = new UVector(builder.contents->size(), errorCode);
            if (U_FAILURE(errorCode)) {
                return;
            }
            contents->assign(*builder.contents, &copyElements<T>, errorCode);
        }
    }
    
    // Adopts the contents of `builder`
    List(ListBuilder<T> *builder) {
        // builder->contents == null means the empty list
        if (builder->contents == nullptr) {
            contents = nullptr;
        } else {
            // Copy the pointer to `contents`
            contents = builder->contents;
            // Null out builder's pointer to its content
            // and delete it
            builder->contents = nullptr;
        }
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
                contents->setDeleter(uprv_deleteUObject);
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
    // copy constructor is used so that builders work properly -- see comment under copyElements()
    Operand(const Operand& other) : isVariableReference(other.isVariableReference), string(other.string) {}

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
    // An operator represents either a function name together with
    // a list of options, which may be empty;
    // or a reserved sequence (which has no meaning and may result
    // in a formatting error).
  public:
    // Function call constructor; adopts `l`
    Operator(FunctionName f, OptionList* l) : isReservedSequence(false), functionName(f), options(l) {}
    // Reserved sequence constructor
    Operator(UnicodeString &r) : isReservedSequence(true), functionName(r), options(nullptr) {}

    // copy constructor is used so that builders work properly -- see comment under copyElements()
    Operator(const Operator& other) : isReservedSequence(other.isReservedSequence),
        functionName(other.functionName),
        options(other.options == nullptr ? nullptr : new OptionList(*other.options)) {}
    
    FunctionName getFunctionName() const {
        U_ASSERT(!isReserved());
        return functionName;
    }
    String asReserved() const {
        U_ASSERT(isReserved());
        return functionName;
    }
    const OptionList& getOptions() const {
        U_ASSERT(!isReserved());
        return *options;
    }
    bool isReserved() const { return isReservedSequence; }

  private:
    const bool isReservedSequence;
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

    // copy constructor is used so that builders work properly -- see comment under copyElements()
    Expression(const Expression& other) {
        *this = other;
    }

    const Expression &operator=(const Expression &other) {
        if (other.rator == nullptr) {
            rator = nullptr;
        } else {
            rator = new Operator(*other.rator);
        }
        if (other.rand == nullptr) {
            rand = nullptr;
        } else {
            rand = new Operand(*other.rand);
        }
        return *this;
    }

    // TODO: include these or not?
    bool isStandaloneAnnotation() const { return (rand == nullptr); }
    bool isFunctionCall() const         { return (rator != nullptr && rand != nullptr); }

    Expression getOperand() const;
    UnicodeString getFunctionName() const;
    Hashtable& getOptions();

    /*
      TODO: I thought about using an Optional class here instead of nullable
      pointers. But that doesn't really work since we can't use the STL and therefore
      can't use `std::move` and therefore can't use move constructors/move assignment
      operators.
     */

    class Builder {
      private:
        Builder() {} // prevent direct construction
      public:
        Builder setOperand(Operand operand);
        Builder setFunctionName(const UnicodeString& functionName);
        Builder addOption(const UnicodeString& key, Expression* value);
        Expression* build();
    };
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
    Key(const Key& other) : wildcard(other.wildcard), contents(other.contents) {};
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
    size_t numParts() const { return parts->length(); }
    // Precondition: i < numParts()
    void getPart(size_t i, Expression& result) const {
        U_ASSERT(i < numParts());
        parts->get(i, result);
    }
    
    class Builder {
      private:
        friend class Pattern;

        Builder(UErrorCode& errorCode) {
            if (U_FAILURE(errorCode)) {
                return;
            }
            parts = new ListBuilder<Expression>();
            if (parts == nullptr) {
                errorCode = U_MEMORY_ALLOCATION_ERROR;
            }
        }
        ListBuilder<Expression>* parts;
      public:
        // Takes ownership of `part`
        void add(Expression* part, UErrorCode& errorCode);
        // TODO: is addAll() necessary?
        Pattern* build(UErrorCode& errorCode);
    };

    static Builder* builder(UErrorCode& errorCode);

 private:
    friend class Builder;
    friend class MessageFormatDataModel;

    // Possibly-empty list of parts
    // Note: a "text" thing is represented like a literal, so that's an expression too.
    // TODO: compare and see how other implementations distinguish text / literal / nmtoken
    const ExpressionList* parts;

    // Can only be called by Builder
    // Takes ownership of `ps`
    Pattern(ExpressionList *ps) : parts(ps) {
        U_ASSERT(ps != nullptr);
    }

    // Copy constructor -- used so that builders work
    Pattern(const Pattern& other) : parts(new ExpressionList(*other.parts)) {
        U_ASSERT(other.parts != nullptr);
    }

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

    /*
    // copy constructor is used so that builders work properly
    Variant(const Variant& other) {
        U_ASSERT(other.keys != nullptr);
        U_ASSERT(other.pattern != nullptr);

        // List is immutable; copy not needed
        keys = other.keys;
        pattern = *other.pattern;
    }
    */
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

    static Hashtable* initBindings(UErrorCode& errorCode) {
       if (U_FAILURE(errorCode)) {
            // Won't be valid if there already was an error.
            return nullptr;
        }
        // No value comparator needed
        LocalPointer<Hashtable> e(new Hashtable(compareVariableName, nullptr, errorCode));
        if (U_FAILURE(errorCode)) {
            return nullptr;
        }
        // The environment owns the values
        e->setValueDeleter(uprv_deleteUObject);
        return e.orphan();
    }
    
    // Creates an empty environment
    Environment(UErrorCode &errorCode) : bindings(initBindings(errorCode)) {}

    // Copy constructor -- this is used so that builders work
     Environment(const Environment& other, UErrorCode& errorCode) : bindings(initBindings(errorCode)) {
        const UHashElement* e;
        int32_t pos = UHASH_FIRST;
        Expression* expr;
        while ((e = other.bindings->nextElement(pos)) != nullptr) {
            expr = new Expression(*(static_cast<Expression*>(e->value.pointer)));
            if (expr == nullptr) {
                errorCode = U_MEMORY_ALLOCATION_ERROR;
                return;
            }
            UnicodeString* s = static_cast<UnicodeString*>(e->key.pointer);
            bindings->put(*s, expr, errorCode);
            if (U_FAILURE(errorCode)) {
                return;
            }
        }
    }
    // Delete assignment operator -- need an error code
    const Environment& operator=(const Environment&) = delete;
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
class MessageFormatter;

class U_I18N_API MessageFormatDataModel : public UMemory {
    friend class MessageFormatter;

  // TODO: this is subject to change; it's not clear yet how much of the structure
  // of the AST will be encoded into the API
  public:
    // Represents the left-hand side of a `when` clause
     class SelectorKeys {
       public:
         KeyList* getKeys() const;
         class Builder {
            private:
                friend class SelectorKeys;
                // prevent direct construction
                Builder(UErrorCode& errorCode) {
                    if (U_FAILURE(errorCode)) {
                        return;
                    }
                    keys = new ListBuilder<Key>();
                    if (keys == nullptr) {
                        errorCode = U_MEMORY_ALLOCATION_ERROR;
                    }
                }
                ListBuilder<Key>* keys;
            public:
                void add(Key* key, UErrorCode& errorCode);
                // TODO: is addAll() necessary?
                SelectorKeys* build(UErrorCode& errorCode);
         };
         static Builder* builder(UErrorCode& errorCode);
       private:
         KeyList* keys;
         // Adopts `keys`
         SelectorKeys(KeyList* ks) : keys(ks) {}
     };

     // class Pattern: see above
     // Corresponds to `pattern` in the grammar

     // Note: we don't have the `Part` class; use Expression instead
     // class Expression: see above
     // Corresponds to `expression` in the grammar


     // TODO: I'm calling the `Value` class "Operand"
     // and I don't have a separate Variable class

     // TODO: can Hashtable be used in the public API?

     Environment& getLocalVariables() const;
     ExpressionList& getSelectors() const;
     Hashtable& getVariants() const;
     Pattern& getPattern() const;

     class Builder {
       private:
         friend class MessageFormatDataModel;

         // prevent direct construction
         Builder(UErrorCode& errorCode) {
             if (U_FAILURE(errorCode)) {
                 return;
             }
             pattern = nullptr;
             selectors = ListBuilder<Expression>();
             variants = ListBuilder<Variant>();
             locals = new Environment(errorCode);
             if (locals == nullptr) {
                 errorCode = U_MEMORY_ALLOCATION_ERROR;
             }
         }
         // The parser validates the message and builds the data model
         // from it.
         MessageFormatDataModel* parse(const UnicodeString &, UParseError &, UErrorCode &);
         void parseBody(const UnicodeString &, uint32_t&, MessageParseError &, UErrorCode &);
         void parseDeclarations(const UnicodeString &, uint32_t&, MessageParseError &, UErrorCode &);
         void parseSelectors(const UnicodeString &, uint32_t&, MessageParseError &, UErrorCode &);

         Pattern* pattern;
         ListBuilder<Expression> selectors;
         ListBuilder<Variant> variants;
         Environment* locals;
       public:
         // Takes ownership of `expression`
         void addLocalVariable(const UnicodeString& variableName, Expression* expression, UErrorCode &errorCode);
         // No addLocalVariables() yet
         // Takes ownership
         void addSelector(Expression* selector, UErrorCode& errorCode);
         // No addSelectors() yet
         // Takes ownership
         void addVariant(SelectorKeys* keys, Pattern* pattern, UErrorCode& errorCode);
         void setPattern(Pattern* pattern);
         MessageFormatDataModel* build(const UnicodeString& source, UParseError &parseError, UErrorCode& errorCode);
     };

     static Builder* builder(UErrorCode& errorCode);

     virtual ~MessageFormatDataModel();

  private:
     friend class Builder;
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
    
    // Do not define default assignment operator
    const MessageFormatDataModel &operator=(const MessageFormatDataModel &) = delete;

    // This *copies* the contents of `builder`, so that it can be re-used / mutated
    // while preserving the immutability of this data model
    // TODO: add tests for this
    MessageFormatDataModel(const Builder& builder, UErrorCode &status);
}; // class MessageFormatDataModel

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // MESSAGEFORMAT_DATA_MODEL_H

#endif // U_HIDE_DEPRECATED_API
// eof

