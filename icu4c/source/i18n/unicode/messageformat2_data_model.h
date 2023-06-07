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
// Helpers (not public)

class Operand : public UMemory {
    // An operand can either be an uninterpreted string,
    // or a variable reference.
    // TODO: do we want to distinguish literals from strings?
    // (Compare other implementations)
  public:
    static Operand* create(bool b, String s, UErrorCode& errorCode) {
        if (U_FAILURE(errorCode)) {
            return nullptr;
        }
        Operand* result = new Operand(b, s);
        if (result == nullptr) {
            errorCode = U_MEMORY_ALLOCATION_ERROR;
        }
        return result;
    }

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
    static Option* create(String s, Operand v, UErrorCode& errorCode) {
        if (U_FAILURE(errorCode)) {
            return nullptr;
        }
        Option* result = new Option(s, v);
        if (result == nullptr) {
            errorCode = U_MEMORY_ALLOCATION_ERROR;
        }
        return result;
    }
  private:
    Option(String s, Operand v) : name(s), value(v) {}
    const String name;
    const Operand value;
};

class PatternPart : public UMemory {
    // Either Expression or TextPart can show up in a pattern
};

class Key : public UMemory {
    // A key is either a string or the "wildcard" symbol.
    // TODO: same question, distinguish literals from strings?
  public:
    bool isWildcard() const { return wildcard; }
    // internal error if this is a wildcard
    String getString() const {
        U_ASSERT(!isWildcard());
        return contents;
    }
    Key(const Key& other) : wildcard(other.wildcard), contents(other.contents) {};

    static Key* create(UErrorCode& errorCode) {
        if (U_FAILURE(errorCode)) {
            return nullptr;
        }
        Key* k = new Key();
        if (k == nullptr) {
            errorCode = U_MEMORY_ALLOCATION_ERROR;
        }
        return k;
    }

    static Key* create(String& s, UErrorCode& errorCode) {
        if (U_FAILURE(errorCode)) {
            return nullptr;
        }
        Key* k = new Key(s);
        if (k == nullptr) {
            errorCode = U_MEMORY_ALLOCATION_ERROR;
        }
        return k;
    }

  private:
    // wildcard constructor
    Key() : wildcard(true) {}
    // concrete key constructor
    Key(String &s) : wildcard(false), contents(s) {}

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


// For now, represent variable names as strings
#define compareVariableName uhash_compareUnicodeString


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

// TODO: this is hurting my brain -- think about whether this is necessary
/*
  This exists because even though AST nodes are immutable, it's
  possible someone could use build() to create data models that have
  different lifetimes
*/
template <typename T1>
static void copyElements(UElement *dst, UElement *src) {
    dst->pointer = new T1(*(static_cast<T1 *>(src->pointer)));
}

class U_I18N_API MessageFormatDataModel : public UMemory {
    friend class MessageFormatter;

  // TODO: this is subject to change; it's not clear yet how much of the structure
  // of the AST will be encoded into the API
  public:
    // Immutable list
    template<typename T>
    class List : public UMemory {
        // Provides a wrapper around a vector
      private:
         const UVector *contents;

      public:
        size_t length() const { return (contents == nullptr ? 0 : contents->size()); }

        // Out-of-bounds is an internal error
        void get(size_t i, T &result) const {
            U_ASSERT(!(length() <= 0 || i >= length()));
            result = *(static_cast<T *>(contents->elementAt(i)));
        }

        class Builder : public UMemory {
           // Provides a wrapper around a vector
         public:
           // adding adopts the thing being added
           void add(T *element, UErrorCode errorCode) {
               if (U_FAILURE(errorCode)) {
                   return;
               }
               U_ASSERT(contents != nullptr);
               contents->adoptElement(element, errorCode);
           }
           List<T>* build(UErrorCode &errorCode) const {
               if (U_FAILURE(errorCode)) {
                   return nullptr;
               }
               LocalPointer<List<T>> adopted(buildList(*this, errorCode));
               if (U_FAILURE(errorCode)) {
                   return nullptr;
               }
               return adopted.orphan();
           }

         private:
           friend class List;

           UVector *contents;

           // Creates an empty list
           Builder(UErrorCode& errorCode) {
               if (U_FAILURE(errorCode)) {
                   return;
               }
               // initialize `contents`
               LocalPointer<UVector> adoptedContents(new UVector(errorCode));
               if (U_FAILURE(errorCode)) {
                   return;
               }
               adoptedContents->setDeleter(uprv_deleteUObject);
               contents = adoptedContents.orphan();
           }
        };
        static Builder* builder(UErrorCode &errorCode) {
            if (U_FAILURE(errorCode)) {
                return nullptr;
            }
            LocalPointer<Builder> result(new Builder(errorCode));
            if (U_FAILURE(errorCode)) {
                return nullptr;
            }
            return result.orphan();
        }
     private:
        friend class Builder; // NOTE: Builder should only call buildList(); not the constructors

        // Copies the contents of `builder`
        // This won't compile unless T is a type that has a copy assignment operator
        static List<T>* buildList(const Builder &builder, UErrorCode &errorCode) {
            if (U_FAILURE(errorCode)) {
                return nullptr;
            }
            List<T>* result;
            U_ASSERT(builder.contents != nullptr);

            LocalPointer<UVector> adoptedContents(new UVector(builder.contents->size(), errorCode));
            adoptedContents->assign(*builder.contents, &copyElements<T>, errorCode);
            if (U_FAILURE(errorCode)) {
                return nullptr;
            }
            result = new List<T>(adoptedContents.orphan());

            // Finally, check for null
            if (result == nullptr) {
                errorCode = U_MEMORY_ALLOCATION_ERROR;
            }
            return result;
        }

        // Adopts `contents`
        List(UVector* things) : contents(things) {}
  };

  using OptionList = List<Option>;

  // TODO: This class should really be private. left public for the convenience of the parser
  class Operator : public UMemory {
         // An operator represents either a function name together with
         // a list of options, which may be empty;
         // or a reserved sequence (which has no meaning and may result
         // in a formatting error).
       public:


         // copy constructor is used so that builders work properly -- see comment under copyElements()
         Operator(const Operator &other)
             : isReservedSequence(other.isReservedSequence), functionName(other.functionName),
               options(other.options == nullptr ? nullptr : new OptionList(*other.options)) {}

         FunctionName getFunctionName() const {
             U_ASSERT(!isReserved());
             return functionName;
         }
         String asReserved() const {
             U_ASSERT(isReserved());
             return functionName;
         }
         const OptionList &getOptions() const {
             U_ASSERT(!isReserved());
             return *options;
         }
         bool isReserved() const { return isReservedSequence; }

         static Operator* create(UnicodeString& r, UErrorCode& errorCode) {
             if (U_FAILURE(errorCode)) {
                 return nullptr;
             }
             Operator* result = new Operator(r);
             if (result == nullptr) {
                 errorCode = U_MEMORY_ALLOCATION_ERROR;
             }
             return result;
         }

         static Operator* create(FunctionName f, OptionList* l, UErrorCode& errorCode) {
             if (U_FAILURE(errorCode)) {
                 return nullptr;
             }
             LocalPointer<OptionList> adoptedOptions(l);
             U_ASSERT(adoptedOptions.isValid());
             Operator* result = new Operator(f, adoptedOptions.orphan());
             if (result == nullptr) {
                 errorCode = U_MEMORY_ALLOCATION_ERROR;
             }
             return result;
         }

       private:
         // Function call constructor; adopts `l`
         Operator(FunctionName f, OptionList *l)
             : isReservedSequence(false), functionName(f), options(l) {}

       // Reserved sequence constructor
         Operator(UnicodeString &r) : isReservedSequence(true), functionName(r), options(nullptr) {}

         const bool isReservedSequence;
         const FunctionName functionName;
         const OptionList *options;
     };

 public:
     using KeyList = List<Key>;

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

        // copy constructor is used so that builders work properly -- see comment under copyElements()
        Expression(const Expression &other) { *this = other; }

        // The following three methods should be private and the parser should be a friend class,
        // once the parser becomes its own class
        // TODO
        static Expression* create(Operand *rAnd, UErrorCode &errorCode) {
            if (U_FAILURE(errorCode)) {
                return nullptr;
            }
            Expression* result = new Expression(rAnd);
            if (result == nullptr) {
                errorCode = U_MEMORY_ALLOCATION_ERROR;
            }
            return result;
        }

        static Expression* create(Operator *rAtor, UErrorCode &errorCode) {
            if (U_FAILURE(errorCode)) {
                return nullptr;
            }
            Expression* result = new Expression(rAtor);
            if (result == nullptr) {
                errorCode = U_MEMORY_ALLOCATION_ERROR;
            }
            return result;
        }

        static Expression* create(Operator *rAtor, Operand *rAnd, UErrorCode &errorCode) {
            if (U_FAILURE(errorCode)) {
                return nullptr;
            }
            Expression* result = new Expression(rAtor, rAnd);
            if (result == nullptr) {
                errorCode = U_MEMORY_ALLOCATION_ERROR;
            }
            return result;
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
        bool isFunctionCall() const { return (rator != nullptr && rand != nullptr); }

        Expression getOperand() const;
        UnicodeString getFunctionName() const;
        Hashtable &getOptions();

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
            Builder setFunctionName(const UnicodeString &functionName);
            Builder addOption(const UnicodeString &key, Expression *value);
            Expression *build(UErrorCode& errorCode);
        };

      private:
        // All constructors adopt their arguments
        // Both operator and operands must be non-null
        Expression(Operator *rAtor, Operand *rAnd) : rator(rAtor), rand(rAnd) {
            U_ASSERT(rAtor != nullptr && rAnd != nullptr);
        }

        // Operand must be non-null
        Expression(Operand *rAnd) : rand(rAnd) { U_ASSERT(rAnd != nullptr); }

        // Operator must be non-null
        Expression(Operator *rAtor) : rator(rAtor) { U_ASSERT(rAtor != nullptr); }

        const Operator *rator;
        const Operand *rand;
    };

    using ExpressionList = List<Expression>;
    class Variant;
    using VariantList    = List<Variant>;

    class Pattern : public UMemory {
      public:
        size_t numParts() const { return parts->length(); }
        // Precondition: i < numParts()
        void getPart(size_t i, Expression &result) const {
            U_ASSERT(i < numParts());
            parts->get(i, result);
        }

        class Builder {
          private:
            friend class Pattern;

            Builder(UErrorCode &errorCode) {
                if (U_FAILURE(errorCode)) {
                    return;
                }
                parts = ExpressionList::builder(errorCode);
            }
            typename ExpressionList::Builder* parts;

          public:
            // Takes ownership of `part`
            void add(Expression *part, UErrorCode &errorCode);
            // TODO: is addAll() necessary?
            Pattern *build(UErrorCode &errorCode);
        };

        static Builder *builder(UErrorCode &errorCode);

      private:
        // Possibly-empty list of parts
        // Note: a "text" thing is represented like a literal, so that's an expression too.
        // TODO: compare and see how other implementations distinguish text / literal
        const ExpressionList *parts;

        // Can only be called by Builder
        // Takes ownership of `ps`
        Pattern(ExpressionList *ps) : parts(ps) { U_ASSERT(ps != nullptr); }

        // Copy constructor -- used so that builders work
        Pattern(const Pattern &other) : parts(new ExpressionList(*other.parts)) {
            U_ASSERT(other.parts != nullptr);
        }
    };

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
        Variant(Pattern * p, UErrorCode & errorCode) {
            if (U_FAILURE(errorCode)) {
                return;
            }
            LocalPointer<Pattern> adoptedPattern(p);
            KeyList::Builder* keysBuilder = KeyList::builder(errorCode);
            keys = keysBuilder->build(errorCode);
            if (U_SUCCESS(errorCode)) {
                pattern = adoptedPattern.orphan();
            }
        }

        static Variant* create(KeyList* ks, Pattern* p, UErrorCode& errorCode) {
            if(U_FAILURE(errorCode)) {
                return nullptr;
            }
            U_ASSERT(ks != nullptr && p != nullptr);
            LocalPointer<Pattern> adoptedPattern(p);
            LocalPointer<KeyList> adoptedKeys(ks);
            Variant* result = new Variant(adoptedKeys.orphan(), adoptedPattern.orphan());
            if (result == nullptr) {
                errorCode = U_MEMORY_ALLOCATION_ERROR;
            }
            return result;
        }
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
        // Usual case; takes ownership of `keys` and `p`
        Variant(KeyList * ks, Pattern * p) : keys(ks), pattern(p) {}

        const KeyList *keys;
        const Pattern *pattern;
    };


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
                    keys = KeyList::builder(errorCode);
                }
                List<Key>::Builder* keys;
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

     // Immutable hash table
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
             void *maybeExpr = bindings->get(v);
             if (maybeExpr == nullptr) {
                 return false;
             }
             result = *(static_cast<Expression *>(maybeExpr));
             return true;
         }

         // Precondition: variable name is not already defined
         // The environment takes ownership of the expression.
         // The error code is used to signal a memory allocation error.
         void define(const VariableName &v, Expression *e, UErrorCode &errorCode) {
             if (U_FAILURE(errorCode)) {
                 return;
             }
             // The assert ensures that the variable name was not already defined
             U_ASSERT(bindings->put(v, e, errorCode) == nullptr);
         }

         // Precondition: variable name is already defined
         void redefine(const VariableName &v, Expression *e, UErrorCode &errorCode) {
             if (U_FAILURE(errorCode)) {
                 return;
             }
             // The assert ensures that the variable name was already defined
             U_ASSERT(bindings->put(v, e, errorCode) != nullptr);
         }

         static Hashtable *initBindings(UErrorCode &errorCode) {
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
         // Delete assignment operator -- need an error code
         const Environment &operator=(const Environment &) = delete;
         // Delete default constructor - need an error code
         Environment() = delete;

         // Returns an environment; if U_SUCCESS(errorCode) on exit, then the result is non-null
         static Environment* create(UErrorCode& errorCode) {
             if (U_FAILURE(errorCode)) {
                 return nullptr;
             }
             LocalPointer<Environment> result(new Environment(errorCode));
             return result.orphan();
         }
         static Environment* create(const Environment& other, UErrorCode& errorCode) {
             if (U_FAILURE(errorCode)) {
                 return nullptr;
             }
             LocalPointer<Environment> result(new Environment(other, errorCode));
             return result.orphan();
         }
       private:
         
         // Creates an empty environment
         Environment(UErrorCode &errorCode) : bindings(initBindings(errorCode)) {}

         // Copy constructor -- this is used so that builders work
         Environment(const Environment &other, UErrorCode &errorCode)
             : bindings(initBindings(errorCode)) {
             const UHashElement *e;
             int32_t pos = UHASH_FIRST;
             Expression *expr;
             while ((e = other.bindings->nextElement(pos)) != nullptr) {
                 expr = new Expression(*(static_cast<Expression *>(e->value.pointer)));
                 if (expr == nullptr) {
                     errorCode = U_MEMORY_ALLOCATION_ERROR;
                     return;
                 }
                 UnicodeString *s = static_cast<UnicodeString *>(e->key.pointer);
                 bindings->put(*s, expr, errorCode);
                 if (U_FAILURE(errorCode)) {
                     return;
                 }
             }
         }

         Hashtable *bindings;
     };


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
             selectors = ExpressionList::builder(errorCode);
             variants = VariantList::builder(errorCode);
             locals = Environment::create(errorCode);
         }

         // Parser class (private)
         class Parser : UMemory {
           public:
             virtual ~Parser();
           private:
             friend class MessageFormatDataModel::Builder;

             Parser(const UnicodeString &input, MessageFormatDataModel::Builder& dataModelBuilder)
             : source(input), index(0), dataModel(dataModelBuilder) {
                 parseError.line = 0;
                 parseError.offset = 0;
                 parseError.lengthBeforeCurrentLine = 0;
                 parseError.preContext[0] = '\0';
                 parseError.postContext[0] = '\0';
             }

             // Used so `parseEscapeSequence()` can handle all types of escape sequences
             // (literal, text, and reserved)
             typedef enum { LITERAL, TEXT, RESERVED } EscapeKind;

             // The parser validates the message and builds the data model
             // from it.
             void parse(UParseError &, UErrorCode &);
             void parseBody(UErrorCode &);
             void parseDeclarations(UErrorCode &);
             void parseSelectors(UErrorCode &);

             void parseWhitespaceMaybeRequired(bool, UErrorCode &);
             void parseRequiredWhitespace(UErrorCode &);
             void parseOptionalWhitespace(UErrorCode &);
             void parseToken(UChar32, UErrorCode &);
             void parseTokenWithWhitespace(UChar32, UErrorCode &);
             template <size_t N>
             void parseToken(const UChar32 (&)[N], UErrorCode &);
             template <size_t N>
             void parseTokenWithWhitespace(const UChar32 (&)[N], UErrorCode &);
             void parseNmtoken(UErrorCode&, VariableName&);
             void parseName(UErrorCode&, VariableName&);
             void parseVariableName(UErrorCode&, VariableName&);
             void parseFunction(UErrorCode&, FunctionName&);
             void parseEscapeSequence(EscapeKind, UErrorCode &, String&);
             void parseLiteralEscape(UErrorCode &, String&);
             void parseLiteral(UErrorCode &, String&);
             void parseOption(UErrorCode &, OptionList::Builder&);
             OptionList* parseOptions(UErrorCode &);
             void parseReservedEscape(UErrorCode &, String &);
             void parseReservedChunk(UErrorCode &, String &);
             Operator* parseReserved(UErrorCode &);
             Operator* parseAnnotation(UErrorCode &);
             Expression* parseLiteralOrVariableWithAnnotation(bool, UErrorCode &);
             Expression* parseExpression(UErrorCode &);
             void parseTextEscape(UErrorCode&, String&);
             void parseText(UErrorCode&, String&);
             Key* parseKey(UErrorCode&);
             SelectorKeys* parseNonEmptyKeys(UErrorCode&);
             Pattern* parsePattern(UErrorCode&);

             // The input string
             const UnicodeString &source;
             // The current position within the input string
             uint32_t index;
             // Represents the current line (and when an error is indicated),
             // character offset within the line of the parse error
             MessageParseError parseError;

             // The parent builder
             MessageFormatDataModel::Builder &dataModel;
         }; // class Parser

         Pattern* pattern;
         ExpressionList::Builder* selectors;
         VariantList::Builder* variants;
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

     class MessageBody : public UMemory {
       public:
         // Constructs a body out of a list of scrutinees (expressions) and
         // a list of variants, which represents the `(selectors 1*([s] variant))`
         // alternative in the grammar
         // Adopts its arguments; takes an error code for consistency
         MessageBody(ExpressionList *es, VariantList *vs, UErrorCode& errorCode) {
             if (U_FAILURE(errorCode)) {
                 return;
             }
             U_ASSERT(es != nullptr && vs != nullptr);
             scrutinees = es;
             variants = vs;
         }

         // Constructs a body out of a single Pattern
         // (body -> pattern alternative in the grammar)
         // a `pattern` in the grammar
         MessageBody(Pattern *pattern, UErrorCode &errorCode) {
             LocalPointer<Pattern> adoptedPattern(pattern);
             if (U_FAILURE(errorCode)) {
                 // The adoptedPattern destructor deletes the pattern
             } else {
                 LocalPointer<Variant> patternVariant(new Variant(adoptedPattern.orphan(), errorCode));
                 LocalPointer<VariantList::Builder> variantListBuilder(VariantList::builder(errorCode));
                 variantListBuilder->add(patternVariant.orphan(), errorCode);
                 // If success, then `variantListBuilder` adopted the pattern
                 LocalPointer<VariantList> adoptedVariants(variantListBuilder->build(errorCode));
                 LocalPointer<ExpressionList::Builder> expressionListBuilder(ExpressionList::builder(errorCode));
                 LocalPointer<ExpressionList> adoptedScrutinees(expressionListBuilder->build(errorCode));

                 if (U_FAILURE(errorCode)) {
                     return;
                 }

                 // Everything succeeded - finally, initialize the members
                 variants = adoptedVariants.orphan();
                 scrutinees = adoptedScrutinees.orphan();
             }
         }
       private:

         /*
           A message body is a `selectors` construct as in the grammar.
           A bare pattern is represented as a `selectors` with no scrutinees
           and a single `when`-clause with empty keys.
          */

         // The expressions that are being matched on. May be empty.
         // (If it's empty, there must be exactly one `when`-clause with empty
         // keys.)
         ExpressionList *scrutinees;

         // The list of `when` clauses (case arms).
         VariantList *variants;
     };


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
