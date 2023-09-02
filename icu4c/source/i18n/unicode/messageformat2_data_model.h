// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef U_HIDE_DEPRECATED_API

#ifndef MESSAGEFORMAT_DATA_MODEL_H
#define MESSAGEFORMAT_DATA_MODEL_H

#if U_SHOW_CPLUSPLUS_API

#if !UCONFIG_NO_FORMATTING

#include "unicode/messageformat2_macros.h"
#include "unicode/messageformat2_utils.h"
#include "unicode/unistr.h"
#include "unicode/utypes.h"

U_NAMESPACE_BEGIN namespace message2 {

// Defined for convenience, in case we end up using a different
// string representation in the data model
using String         = UnicodeString;
// Defined for convenience, in case we end up using a different
// representation in the data model for variable references and/or
// variable definitions
using VariableName   = UnicodeString;

// -----------------------------------------------------------------------
// Public MessageFormatDataModel class

/**
 * <p>MessageFormat2 is a Technical Preview API implementing MessageFormat 2.0.
 * Since it is not final, documentation has not yet been added everywhere.
 *
 * The `MessageFormatDataModel` class describes a parsed representation of the text of a message.
 * This representation is public as higher-level APIs for messages will need to know its public
 * interface: for example, to re-instantiate a parsed message with different values for imported
variables.
 *
 * The MessageFormatDataModel API implements <a target="github"
href="https://github.com/unicode-org/message-format-wg/blob/main/spec/data-model.md">the
 * specification of the abstract syntax (data model representation)</a> for MessageFormat.
 *
 * @internal ICU 74.0 technology preview
 * @deprecated This API is for technology preview only.
 */

/*
  Classes that represent nodes in the data model are nested inside the
  `MessageFormatDataModel` class.

  Classes such as `Expression`, `Pattern` and `VariantMap` are immutable and
  are constructed using the builder pattern.

  Most classes representing nodes have copy constructors. This is because builders
  contain immutable data that must be copied when calling `build()`, since the builder
  could go out of scope before the immutable result of the builder does. Copying is
  also necessary to prevent unexpected mutation if intermediate builders are saved
  and mutated again after calling `build()`.

  The copy constructors perform a deep copy, for example by copying the entire
  list of options for an `Operator` (and copying the entire underlying vector.)
  Some internal fields should be `const`, but are declared as non-`const` to make
  the copy constructor simpler to implement. (These are noted throughout.) In
  other words, those fields are `const` except during the execution of a copy
  constructor.

  On the other hand, intermediate `Builder` methods that return a `Builder&`
  mutate the state of the builder, so in code like:

  Expression::Builder& exprBuilder = Expression::builder()-> setOperand(foo);
  Expression::Builder& exprBuilder2 = exprBuilder.setOperator(bar);

  the call to `setOperator()` would mutate `exprBuilder`, since `exprBuilder`
  and `exprBuilder2` are references to the same object.

  An alternate choice would be to make `build()` destructive, so that copying would
  be unnecessary. Or, both copying and moving variants of `build()` could be
  provided. Copying variants of the intermediate `Builder` methods could be
  provided as well, if this proved useful.
*/
class U_I18N_API MessageFormatDataModel : public UMemory {

public:
    // Forward declarations
    class Binding;
    class Expression;
    class Key;
    class Operand;
    class Operator;
    class Pattern;
    class PatternPart;
    class Reserved;
    class SelectorKeys;
    class VariantMap;

    using Bindings = List<Binding>;
    using ExpressionList = List<Expression>;
    using KeyList = List<Key>;
    using OptionMap = OrderedMap<Operand>;

    // Corresponds to the `FunctionRef` interface defined in
    // https://github.com/unicode-org/message-format-wg/blob/main/spec/data-model.md#expressions
    class FunctionName : public UMemory {
        public:
        enum Sigil {
            OPEN,
            CLOSE,
            DEFAULT
        };
        const UnicodeString name;
        const Sigil sigil;
        FunctionName(UnicodeString s) : name(s), sigil(Sigil::DEFAULT) {}
        FunctionName(UnicodeString n, Sigil s) : name(n), sigil(s) {}

        UnicodeString toString() const;
        private:
        // Function names only need to be copied when copying an `Operator`
        friend class Operator;

        FunctionName(const FunctionName& other) : name(other.name), sigil(other.sigil) {}
    };

    // Corresponds to the `Literal` interface defined in
    // https://github.com/unicode-org/message-format-wg/blob/main/spec/data-model.md#expressions
    class Literal : public UObject {
    public:
        const bool isQuoted = false;
        const UnicodeString contents;

        Literal(bool q, const UnicodeString& s) : isQuoted(q), contents(s) {}

    private:
        friend class Key;
        friend class List<Literal>;
        friend class Operand;
        friend class Reserved;

        Literal(const Literal& other) : isQuoted(other.isQuoted), contents(other.contents) {}
        // Because Key uses `Literal` as its underlying representation,
        // this provides a default constructor for wildcard keys
        Literal() {}
    };
    
    // Represents a `Literal | VariableRef` -- see the `operand?` field of the `FunctionRef`
    // interface defined at:
    // https://github.com/unicode-org/message-format-wg/blob/main/spec/data-model.md#expressions
    class Operand : public UObject {
    public:
        // Variable
        static Operand* create(const VariableName& s, UErrorCode& errorCode);
        // Literal
        static Operand* create(const Literal& lit, UErrorCode& errorCode);

        bool isVariable() const { return isVariableReference; }
        bool isLiteral() const { return !isVariable(); }
        VariableName asVariable() const;
        const Literal& asLiteral() const;

        // Copy constructor
        Operand(const Operand& other) : isVariableReference(other.isVariableReference), string(other.string) {}

    private:
        // Represent variable names as unquoted literals
        Operand(const VariableName& var) : isVariableReference(true), string(Literal(false, var)) {}
        Operand(const Literal& l) : isVariableReference(false), string(l) {}

        const bool isVariableReference;
        const Literal string;
    }; // class Operand

    // Corresponds to the `Literal | CatchallKey` that is the
    // element type of the `keys` array in the `Variant` interface
    // defined in https://github.com/unicode-org/message-format-wg/blob/main/spec/data-model.md#messages
    class Key : public UObject {
    public:
        // A key is either a literal or the "wildcard" symbol.

        bool isWildcard() const { return wildcard; }
        // Precondition: !isWildcard()
        const Literal& asLiteral() const;
        static Key* create(UErrorCode& errorCode);
        static Key* create(const Literal& lit, UErrorCode& errorCode);

    private:
        friend class List<Key>;
        friend class VariantMap;

        Key(const Key& other) : wildcard(other.wildcard), contents(other.contents) {};
        void toString(UnicodeString& result) const;
    
        // wildcard constructor
        Key() : wildcard(true) {}
        // concrete key constructor
        Key(const Literal& lit) : wildcard(false), contents(lit) {}
        const bool wildcard; // True if this represents the wildcard "*"
        const Literal contents;
    }; // class Key

    // Corresponds to the `keys` array in the `Variant` interface
    // defined in https://github.com/unicode-org/message-format-wg/blob/main/spec/data-model.md#messages
    class SelectorKeys : public UObject {
    public:
        const KeyList& getKeys() const;
        class Builder {
        private:
            friend class SelectorKeys;
            Builder(UErrorCode& errorCode);
            LocalPointer<List<Key>::Builder> keys;
        public:
            Builder& add(Key* key, UErrorCode& errorCode);
            // Note: ICU4J has an `addAll()` method, which is omitted here.
            SelectorKeys* build(UErrorCode& errorCode) const;
        }; // class SelectorKeys::Builder
        static Builder* builder(UErrorCode& errorCode);

    private:
        friend class List<SelectorKeys>;
        friend class VariantMap;

        SelectorKeys(const SelectorKeys& other) : keys(new KeyList(*(other.keys))) {
            U_ASSERT(!other.isBogus());
        }

        const LocalPointer<KeyList> keys;
        bool isBogus() const { return !keys.isValid(); }
        // Adopts `keys`
        SelectorKeys(KeyList* ks) : keys(ks) {}
    }; // class SelectorKeys

    /*
      A `VariantMap` maps a list of keys onto a `Pattern`, following
      the `variant` production in the grammar:

      variant = when 1*(s key) [s] pattern

      https://github.com/unicode-org/message-format-wg/blob/main/spec/message.abnf#L9

      The map uses the `key` list as its key, and the `pattern` as the value.

      This representation mirrors the ICU4J API:
      public OrderedMap<SelectorKeys, Pattern> getVariants();

      Since the `OrderedMap` class defined above is not polymorphic on its key
      values, `VariantMap` is defined as a separate data type that wraps an
      `OrderedMap<Pattern>`.

      The `VariantMap::Builder::add()` method encodes its `SelectorKeys` as
      a string, and the VariantMap::next() method decodes it.
    */
    class VariantMap : public UMemory {
    public:
        static constexpr size_t FIRST = OrderedMap<Pattern>::FIRST;
        // Because List::get() returns a T*,
        // the out-parameters for `next()` are references to pointers
        // rather than references to a `SelectorKeys` or a `Pattern`,
        // in order to avoid either copying or creating a reference to
        // a temporary value.
        bool next(size_t &pos, const SelectorKeys*& k, const Pattern*& v) const;
        class Builder : public UMemory {
        public:
            Builder& add(SelectorKeys* key, Pattern* value, UErrorCode& errorCode);
            VariantMap* build(UErrorCode& errorCode) const;
        private:
            friend class VariantMap;
          
            static void concatenateKeys(const SelectorKeys& keys, UnicodeString& result);
            Builder(UErrorCode& errorCode);
            LocalPointer<OrderedMap<Pattern>::Builder> contents;
            LocalPointer<List<SelectorKeys>::Builder> keyLists;
        }; // class VariantMap::Builder

        static Builder* builder(UErrorCode& errorCode);
    private:
        friend class Builder;
        VariantMap(OrderedMap<Pattern>* vs, List<SelectorKeys>* ks) : contents(vs), keyLists(ks) {
            // Check invariant: `vs` and `ks` have the same size
            U_ASSERT(vs->size() == ks->length());
        }
        const LocalPointer<OrderedMap<Pattern>> contents;
        // See the method implementations for comments on
        // how `keyLists` is used.
        const LocalPointer<List<SelectorKeys>> keyLists;
    }; // class VariantMap

    // Corresponds to the `Reserved` interface
    // defined in
    // https://github.com/unicode-org/message-format-wg/blob/main/spec/data-model.md#expressions 
    class Reserved : public UMemory {
    public:
        size_t numParts() const;
        // Precondition: i < numParts()
        const Literal* getPart(size_t i) const;
        class Builder {
        private:
            friend class Reserved;
          
            Builder(UErrorCode &errorCode);
            LocalPointer<List<Literal>::Builder> parts;
          
        public:
            Builder& add(Literal& part, UErrorCode &errorCode);
            Reserved *build(UErrorCode &errorCode) const;
        }; // class Reserved::Builder

        static Builder *builder(UErrorCode &errorCode);
    private:
        friend class Operator;
      
        // See comments under SelectorKeys' copy constructor; this is analogous
        bool isBogus() const { return !parts.isValid(); }
      
        // Reserved needs a copy constructor in order to make Expression deeply copyable
        Reserved(const Reserved& other) : parts(new List<Literal>(*other.parts)) {
            U_ASSERT(!other.isBogus());
        }

        // Possibly-empty list of parts
        // `literal` reserved as a quoted literal; `reserved-char` / `reserved-escape`
        // strings represented as unquoted literals
        const LocalPointer<List<Literal>> parts;
      
        // Can only be called by Builder
        // Takes ownership of `ps`
        Reserved(List<Literal> *ps) : parts(ps) { U_ASSERT(ps != nullptr); }
    };

    // Corresponds to the `FunctionRef | Reserved` type in the
    // `Expression` interface defined in
    // https://github.com/unicode-org/message-format-wg/blob/main/spec/data-model.md#patterns
    class Operator : public UMemory {
        /*
          An operator represents either a function name together with
          a list of options, which may be empty;
          or a reserved sequence (which has no meaning and may result
          in a formatting error.
        */
    public:
        bool isReserved() const { return isReservedSequence; }
        // Precondition: !isReserved()
        const FunctionName& getFunctionName() const;
        // Precondition: isReserved()
        const Reserved& asReserved() const;
        // Precondition: isReserved()
        const OptionMap &getOptions() const;

        class Builder {
        private:
            friend class Operator;
            Builder() {}
            LocalPointer<Reserved> asReserved;
            LocalPointer<FunctionName> functionName;
            LocalPointer<OptionMap::Builder> options;
        public:
            Builder& setReserved(Reserved* reserved);
            Builder& setFunctionName(FunctionName* func);
            Builder& addOption(const UnicodeString &key, Operand* value, UErrorCode& errorCode);
            // Note: ICU4J has an `addAll()` method, which is omitted here.
            Operator* build(UErrorCode& errorCode) const;
        };

        static Builder* builder(UErrorCode& errorCode);
    private:
        friend class Expression;
      
        // Postcondition: if U_SUCCESS(errorCode), then return value is non-bogus
        static Operator* create(const Reserved& r, UErrorCode& errorCode);

        // Takes ownership of `opts`
        // Postcondition: if U_SUCCESS(errorCode), then return value is non-bogus
        static Operator* create(const FunctionName& f, OptionMap* opts, UErrorCode& errorCode);

        // Function call constructor; adopts `l`, which must be non-null
        Operator(FunctionName f, OptionMap *l)
            : isReservedSequence(false), functionName(f), options(l), reserved(nullptr) {
            U_ASSERT(l != nullptr);
        }

        // Reserved sequence constructor
        // Result is bogus if copy of `r` fails
        Operator(const Reserved& r) : isReservedSequence(true), functionName(FunctionName(UnicodeString(""))), options(nullptr), reserved(new Reserved(r)) {}

        // Copy constructor
        Operator(const Operator& other);

        bool isBogus() const;
        const bool isReservedSequence;
        const FunctionName functionName;
        /* const */ LocalPointer<OptionMap> options;
        /* const */ LocalPointer<Reserved> reserved;
    }; // class Operator
  
    // Corresponds to the `FunctionRef | Reserved` type in the
    // `Expression` interface defined in
    // https://github.com/unicode-org/message-format-wg/blob/main/spec/data-model.md#patterns
    class Expression : public UObject {
        /*
          An expression is represented as the application of an optional operator to an optional operand.

                                      Operator               | Operand
                                      --------------------------------
          { |42| :fun opt=value } =>  (FunctionName=fun,     | Literal(quoted=true, contents="42")
                                      options={opt: value})
          { abcd }                =>  null                   | Literal(quoted=false, contents="abcd")
          { : fun opt=value }     =>  (FunctionName=fun,
                                      options={opt: value})  | null

          An expression where both operand and operator are null can't be constructed using
          the builder interface.
        */
    public:

        // Returns true iff the `Operator` of `this`
        // is a function call with no argument.
        bool isStandaloneAnnotation() const;
        // Returns true iff the `Operator` of `this`
        // is a function call (with or without an operand).
        // Reserved sequences are not function calls
        bool isFunctionCall() const;
        // Returns true iff the `Operator` of `this`
        // is a reserved sequence
        bool isReserved() const;
        // Precondition: (isFunctionCall() || isReserved())
        const Operator& getOperator() const;
        // Precondition: !isStandaloneAnnotation()
        const Operand& getOperand() const;

        // Expression needs a copy constructor in order to make Pattern deeply copyable
        // (and for closures)
        Expression(const Expression& other);

        class Builder {
        private:
            friend class Expression;
            Builder() {}
            LocalPointer<Operand> rand;
            LocalPointer<Operator> rator;
        public:
            Builder& setOperand(Operand* rAnd);
            Builder& setOperator(Operator* rAtor);
            // Postcondition: U_FAILURE(errorCode) || (result != nullptr && !isBogus(result))
            Expression *build(UErrorCode& errorCode) const;
        }; // class Expression::Builder

        static Builder* builder(UErrorCode& errorCode);

    private:

        // Here, a separate variable isBogus tracks if any copies failed.
        // This is because rator = nullptr and rand = nullptr are semantic here,
        // so this can't just be a predicate that checks if those are null
        bool bogus = false; // copy constructors explicitly set this to true on failure

        bool isBogus() const;

        Expression(const Operator &rAtor, const Operand &rAnd) : rator(new Operator(rAtor)), rand(new Operand(rAnd)) {}
        Expression(const Operand &rAnd) : rator(nullptr), rand(new Operand(rAnd)){}
        Expression(const Operator &rAtor) : rator(new Operator(rAtor)), rand(nullptr) {}
        /* const */ LocalPointer<Operator> rator;
        /* const */ LocalPointer<Operand> rand;
    }; // class Expression

    // Represents the `body` field of the `Pattern` interface
    // defined in https://github.com/unicode-org/message-format-wg/blob/main/spec/data-model.md#patterns
    class PatternPart : public UObject {
    public:
        static PatternPart* create(const UnicodeString& t, UErrorCode& errorCode);
        // Takes ownership of `e`
        static PatternPart* create(Expression* e, UErrorCode& errorCode);
        bool isText() const { return isRawText; }
        // Precondition: !isText()
        const Expression& contents() const;
        // Precondition: isText();
        const UnicodeString& asText() const;

    private:
        friend class List<PatternPart>;
        friend class Pattern;
        // Text
        PatternPart(const UnicodeString& t) : isRawText(true), text(t), expression(nullptr) {}
        // Expression
        PatternPart(Expression* e) : isRawText(false), expression(e) {}

        // If !isRawText and the copy of the other expression fails,
        // then isBogus() will be true for this PatternPart
        // PatternPart needs a copy constructor in order to make Pattern deeply copyable
        PatternPart(const PatternPart& other) : isRawText(other.isText()), text(other.text), expression(isRawText ? nullptr : new Expression(other.contents()))  {
            U_ASSERT(!other.isBogus());
        }

        const bool isRawText;
        // Not used if !isRawText
        const UnicodeString text;
        // null if isRawText
        const LocalPointer<Expression> expression;
      
        bool isBogus() const { return (!isRawText && !expression.isValid()); }
    }; // class PatternPart

    // Represents the `Pattern` interface
    // defined in https://github.com/unicode-org/message-format-wg/blob/main/spec/data-model.md#patterns
    class Pattern : public UObject {
    public:
        size_t numParts() const { return parts->length(); }
        // Precondition: i < numParts()
        const PatternPart* getPart(size_t i) const;

        class Builder {
        private:
            friend class Pattern;
          
            Builder(UErrorCode &errorCode);
            // Note this is why PatternPart and all its enclosed classes need
            // copy constructors: when the build() method is called on `parts`,
            // it should copy `parts` rather than moving it
            LocalPointer<List<PatternPart>::Builder> parts;
          
        public:
            // Takes ownership of `part`
            Builder& add(PatternPart *part, UErrorCode &errorCode);
            // Note: ICU4J has an `addAll()` method, which is omitted here.
            Pattern *build(UErrorCode &errorCode);
        }; // class Pattern::Builder

        static Builder *builder(UErrorCode &errorCode);

        // If the copy of the other list fails,
        // then isBogus() will be true for this Pattern
        // Pattern needs a copy constructor in order to make MessageFormatDataModel::build() be a copying rather than
        // moving build
        Pattern(const Pattern& other) : parts(new List<PatternPart>(*(other.parts))) { U_ASSERT(!other.isBogus()); }
      
    private:
        friend class MessageFormatDataModel;

        // Possibly-empty list of parts
        const LocalPointer<List<PatternPart>> parts;
      
        bool isBogus() const { return !parts.isValid(); }
        // Can only be called by Builder
        // Takes ownership of `ps`
        Pattern(List<PatternPart> *ps) : parts(ps) { U_ASSERT(ps != nullptr); }

    }; // class Pattern

    // Represents the `Declaration` interface
    // defined in https://github.com/unicode-org/message-format-wg/blob/main/spec/data-model.md#messages
    class Binding {
    public:
        static Binding* create(const UnicodeString& var, Expression* e, UErrorCode& errorCode);
        const UnicodeString var;
        // Postcondition: result is non-null
        const Expression* getValue() const {
            U_ASSERT(!isBogus());
            return value.getAlias();
        }
    private:
        friend class List<Binding>;

        Binding(const UnicodeString& v, Expression* e) : var(v), value(e) {}
        // This needs a copy constructor so that `Bindings` is deeply-copyable,
        // which is in turn so that MessageFormatDataModel::build() can be copying
        // (it has to copy the builder's locals)
        Binding(const Binding& other) : var(other.var), value(new Expression(*other.value)) {
            U_ASSERT(!other.isBogus());
        }

        const LocalPointer<Expression> value;
        bool isBogus() const { return !value.isValid(); }
    }; // class Binding

    // Public MessageFormatDataModel methods

    const Bindings& getLocalVariables() const { return *bindings; }
    // The `hasSelectors()` method is provided so that `getSelectors()`,
    // `getVariants()` and `getPattern()` can rely on preconditions
    // rather than taking error codes as arguments.
    bool hasSelectors() const;
    // Precondition: hasSelectors()
    const ExpressionList& getSelectors() const;
    // Precondition: hasSelectors()
    const VariantMap& getVariants() const;
    // Precondition: !hasSelectors()
    const Pattern& getPattern() const;

    class Builder {
    private:
        friend class MessageFormatDataModel;
        Builder(UErrorCode& errorCode);
        void buildSelectorsMessage(UErrorCode& errorCode);
        LocalPointer<Pattern> pattern;
        LocalPointer<ExpressionList::Builder> selectors;
        LocalPointer<VariantMap::Builder> variants;
        LocalPointer<Bindings::Builder> locals;
      
    public:
        // Note that these methods are not const -- they mutate `this` and return a reference to `this`,
        // rather than a const reference to a new builder
      
        // Takes ownership of `expression`
        Builder& addLocalVariable(const UnicodeString& variableName, Expression* expression, UErrorCode &errorCode);
        // No addLocalVariables() yet
        // Takes ownership
        Builder& addSelector(Expression* selector, UErrorCode& errorCode);
        // No addSelectors() yet
        // Takes ownership
        Builder& addVariant(SelectorKeys* keys, Pattern* pattern, UErrorCode& errorCode);
        Builder& setPattern(Pattern* pattern);
        MessageFormatDataModel* build(UErrorCode& errorCode) const;
    }; // class MessageFormatDataModel::Builder

  static Builder* builder(UErrorCode& errorCode);
  
  virtual ~MessageFormatDataModel();

  private:
     // The expressions that are being matched on.
     // Null iff this is a `pattern` message.
     LocalPointer<ExpressionList> selectors;

     // The list of `when` clauses (case arms).
     // Null iff this is a `pattern` message.
     LocalPointer<VariantMap> variants;

     // The pattern forming the body of the message.
     // If this is non-null, then `variants` and `selectors` must be null.
     LocalPointer<Pattern> pattern;

     // Bindings for local variables
     LocalPointer<Bindings> bindings;

     // Normalized version of the input string (optional whitespace omitted)
     // Used for testing purposes
     LocalPointer<UnicodeString> normalizedInput;

    // Do not define default assignment operator
    const MessageFormatDataModel &operator=(const MessageFormatDataModel &) = delete;

    MessageFormatDataModel(const Builder& builder, UErrorCode &status);
}; // class MessageFormatDataModel

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // MESSAGEFORMAT_DATA_MODEL_H

#endif // U_HIDE_DEPRECATED_API
// eof

