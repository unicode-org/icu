// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef U_HIDE_DEPRECATED_API

#ifndef MESSAGEFORMAT_DATA_MODEL_H
#define MESSAGEFORMAT_DATA_MODEL_H

#if U_SHOW_CPLUSPLUS_API

#if !UCONFIG_NO_FORMATTING

#include "unicode/messageformat2_macros.h"
#include "unicode/parseerr.h"
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
// Helpers (not public)

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

// Forward declarations
class MessageFormatter;

/*
TODO: explain in more detail why AST nodes need copy constructors:
 -> because temporary (mutable) lists within builders can contain immutable nodes
 -> though immutable, they must be copied when calling build() since the builder could go out of scope before the immutable result of the builder does, resulting in non-uniquely-owned pointers; or, further mutating operations could be called on the builder, resulting in unexpected mutation to the data model due to unexpected sharing 

the copy constructors do *deep* copies, e.g. copying an entire vector
*/

/*
TODO: Intermediate Builder->Builder& methods are currently not const,
because that seems like a lot of copying.
But maybe they should be?
*/
class U_I18N_API MessageFormatDataModel : public UMemory {

    friend class MessageFormatter;

public:
    // Forward declarations
    class Binding;
    class Expression;
    class Key;
    template<typename T>
    class List;
    class Operand;
    class Operator;
    template<typename V>
    class OrderedMap;
    class Pattern;
    class PatternPart;
    class Reserved;
    class SelectorKeys;
    class VariantMap;

    using Bindings = List<Binding>;
    using ExpressionList = List<Expression>;
    using KeyList = List<Key>;
    using OptionMap = OrderedMap<Operand>;

    // Immutable list; wraps a vector
    template<typename T>
    class List : public UMemory {
    private:
        // This should be const, but is non-const to make the copy constructor easier
        /* const */ LocalPointer<UVector> contents;

    public:
        size_t length() const {
            U_ASSERT(!isBogus());
            return contents->size();
        }

        // Out-of-bounds is an internal error
        // To avoid undue copying, get() returns a T* since UVector::get() returns a void*.
        const T* get(size_t i) const {
            U_ASSERT(!isBogus());
            U_ASSERT(!(length() <= 0 || i >= length()));
            return static_cast<const T *>(contents->elementAt(i));
        }
        
        class Builder : public UMemory {
            // Provides a wrapper around a vector
        public:
            // adding adopts the thing being added
            Builder& add(T *element, UErrorCode errorCode) {
                if (U_FAILURE(errorCode)) {
                    return *this;
                }
                U_ASSERT(contents != nullptr);
                contents->adoptElement(element, errorCode);
                return *this;
            }

            // Postcondition: U_FAILURE(errorCode) or returns a list such that isBogus() = false
            List<T>* build(UErrorCode &errorCode) const {
                if (U_FAILURE(errorCode)) {
                    return nullptr;
                }
                LocalPointer<List<T>> adopted(buildList(*this, errorCode));
                if (!adopted.isValid() || adopted->isBogus()) {
                    errorCode = U_MEMORY_ALLOCATION_ERROR;
                    return nullptr;
                }
                return adopted.orphan();
            }

        private:
            friend class List;
            LocalPointer<UVector> contents;
            Builder(UErrorCode& errorCode) {
                if (U_FAILURE(errorCode)) {
                    return;
                }
                // initialize `contents`
                contents.adoptInstead(new UVector(errorCode));
                if (U_FAILURE(errorCode)) {
                    return;
                }
                contents->setDeleter(uprv_deleteUObject);
            }
        }; // class List::Builder

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
        friend class Pattern;
        friend class Reserved;
        friend class SelectorKeys;

        // Helper functions for vector copying
        // T1 must have a copy constructor
        // This may leave dst->pointer == nullptr, which is handled by the UVector assign() method
        template <typename T1>
        static void copyElements(UElement *dst, UElement *src) {
            dst->pointer = new T1(*(static_cast<T1 *>(src->pointer)));
        }

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

        List(const List<T>& other) {
            UErrorCode errorCode = U_ZERO_ERROR;
            U_ASSERT(!other.isBogus());
            contents.adoptInstead(new UVector(other.length(), errorCode));
            if (U_FAILURE(errorCode)) {
                contents.adoptInstead(nullptr);
                return;
            }
            contents->assign(*other.contents, &copyElements<T>, errorCode);
            if (U_FAILURE(errorCode)) {
                contents.adoptInstead(nullptr);
            }
        }
        // Adopts `contents`
        List(UVector* things) : contents(things) {
            U_ASSERT(things != nullptr);
        }
        // If a copy constructor fails, the list is left in an inconsistent state,
        // because copying has to allocate a new vector.
        // Copy constructors can't take error codes as arguments. So we have to
        // resort to this, and all methods must check the invariant and signal an
        // error if it's false. The error should be U_MEMORY_ALLOCATION_ERROR,
        // since isBogus iff an allocation failed.
        // For classes that contain a List member, there is no guarantee that
        // the list will be non-bogus, but if it is, any operations on that list
        // will fail (assertion failure)
        bool isBogus() const { return !contents.isValid(); }
    }; // class List

    // Immutable ordered map from strings to pointers to arbitrary values
    template<typename V>
    class OrderedMap : public UMemory {
        // Provides an immutable wrapper around a hash table.
        // Also stores the order in which keys were added.

    private:
        size_t size() const {
            U_ASSERT(!isBogus());
            return keys->size();
        }

    public:
        static constexpr size_t FIRST = 0;
        // Returns true if there are elements remaining
        bool next(size_t &pos, UnicodeString& k, const V*& v) const {
            U_ASSERT(!isBogus());
            U_ASSERT(pos >= FIRST);
            if (pos >= size()) {
                return false;
            }
            k = *((UnicodeString*)keys->elementAt(pos));
            v = (V*) contents->get(k);
            pos = pos + 1;
            return true;
        }

        class Builder : public UMemory {
        public:
            // Adopts `value`
            Builder& add(const UnicodeString& key, V* value, UErrorCode& errorCode) {
                if (U_FAILURE(errorCode)) {
                    return *this;
                }
                // Copy `key` so it can be stored in the vector
                LocalPointer<UnicodeString> adoptedKey(new UnicodeString(key));
                if (!adoptedKey.isValid()) {
                    return *this;
                }
                UnicodeString* k = adoptedKey.orphan();
                keys->adoptElement(k, errorCode);
                contents->put(key, value, errorCode);
                return *this;
            }

            // Copying `build()` (leaves `this` valid)
            OrderedMap<V>* build(UErrorCode& errorCode) const {
                if (U_FAILURE(errorCode)) {
                    return nullptr;
                }

                LocalPointer<Hashtable> adoptedContents(copyHashtable(*contents));
                LocalPointer<UVector> adoptedKeys(copyStringVector(*keys));

                if (!adoptedContents.isValid() || !adoptedKeys.isValid()) {
                    return nullptr;
                }
                LocalPointer<OrderedMap<V>> result(
                    OrderedMap<V>::create(adoptedContents.orphan(),
                                          adoptedKeys.orphan(),
                                          errorCode));
                if (U_FAILURE(errorCode)) {
                    return nullptr;
                }
                return result.orphan();
            }
            // TODO
            // could add a non-copying / moving build() that invalidates `this`
            // The parser itself would be a good use case for this
            // OrderedMap<V>* build(UErrorCode& errorCode);
        private:
            friend class OrderedMap;
        
            // Only called by builder()
            Builder(UErrorCode& errorCode) {
                // initialize `keys`
                keys.adoptInstead(new UVector(errorCode));
                if (U_FAILURE(errorCode)) {
                    return;
                }
                keys->setDeleter(uprv_deleteUObject);
 
                // initialize `contents`
                // No value comparator needed
                contents.adoptInstead(new Hashtable(compareVariableName, nullptr, errorCode));
                if (U_FAILURE(errorCode)) {
                    return;
                }
                // The `contents` hashtable owns the values, but does not own the keys
                contents->setValueDeleter(uprv_deleteUObject);
            }
        
            // Hashtable representing the underlying map
            LocalPointer<Hashtable> contents;
            // Maintain a list of keys that encodes the order in which
            // keys are added. This wastes some space, but allows us to
            // re-use ICU4C's Hashtable abstraction without re-implementing
            // an ordered version of it.
            LocalPointer<UVector> keys;
        }; // class OrderedMap<V>::Builder

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
        friend class VariantMap;
        friend class Operator;
        // Helpers

        static void copyStrings(UElement *dst, UElement *src) {
            dst->pointer = new UnicodeString(*(static_cast<UnicodeString *>(src->pointer)));
        }

        static Hashtable* copyHashtable(const Hashtable& other) {
            UErrorCode errorCode = U_ZERO_ERROR;
            // No value comparator needed
            LocalPointer<Hashtable> adoptedContents(new Hashtable(compareVariableName, nullptr, errorCode));
            if (U_FAILURE(errorCode)) {
                return nullptr;
            }
            // The hashtable owns the values
            adoptedContents->setValueDeleter(uprv_deleteUObject);

            // Copy all the key/value bindings over
            const UHashElement *e;
            int32_t pos = UHASH_FIRST;
            V *val;
            while ((e = other.nextElement(pos)) != nullptr) {
                val = new V(*(static_cast<V *>(e->value.pointer)));
                if (val == nullptr) {
                    return nullptr;
                }
                UnicodeString *s = static_cast<UnicodeString *>(e->key.pointer);
                adoptedContents->put(*s, val, errorCode);
                if (U_FAILURE(errorCode)) {
                    return nullptr;
                }
            }

            return adoptedContents.orphan();
        }

        static UVector* copyStringVector(const UVector& other) {
            UErrorCode errorCode = U_ZERO_ERROR;
            LocalPointer<UVector> adoptedKeys(new UVector(other.size(), errorCode));
            if (U_FAILURE(errorCode)) {
                return nullptr;
            }
            adoptedKeys->assign(other, &copyStrings, errorCode);
            if (U_FAILURE(errorCode)) {
                return nullptr;
            }
            return adoptedKeys.orphan();

        }
        // See comments under `List`
        bool isBogus() const { return (!contents.isValid() || !keys.isValid()); }

        OrderedMap<V>(const OrderedMap<V>& other) : contents(copyHashtable(*other.contents)), keys(copyStringVector(*other.keys)) {
            U_ASSERT(!other.isBogus());
        }

        // Postcondition: U_FAILURE(errorCode) || !((return value).isBogus())
        static OrderedMap<V>* create(Hashtable* c, UVector* k, UErrorCode& errorCode) {
            if (U_FAILURE(errorCode)) {
                return nullptr;
            }
            LocalPointer<OrderedMap<V>> result(new OrderedMap<V>(c, k));
            if (result == nullptr) {
                errorCode = U_MEMORY_ALLOCATION_ERROR;
            } else if (result->isBogus()) {
                errorCode = U_MEMORY_ALLOCATION_ERROR;
                return nullptr;
            }
            return result.orphan();
        }
        OrderedMap<V>(Hashtable* c, UVector* k) : contents(c), keys(k) {
            // It would be an error if `c` and `k` had different sizes
            U_ASSERT(c->count() == k->size());
        }
        // Hashtable representing the underlying map
        const LocalPointer<Hashtable> contents;
        // List of keys
        const LocalPointer<UVector> keys;
    }; // class OrderedMap<V>

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
        // Makes it easier for new wildcard Keys to be initialized
        Literal() {}
    };
    
    class Operand : public UObject {
        // An operand can either be a variable reference or a literal.
        // There is a separate Literal class (which can be quoted or unquoted)
        // to make it easier to distinguish |x| from x when serializing the data model.
    public:
        // Variable
        static Operand* create(const VariableName& s, UErrorCode& errorCode);
        // Literal
        static Operand* create(Literal& lit, UErrorCode& errorCode);

        bool isVariable() const { return isVariableReference; }
        bool isLiteral() const { return !isVariable(); }
        VariableName asVariable() const;
        const Literal& asLiteral() const;
    private:
        friend class Expression;
        friend class OrderedMap<Operand>;

        // Represent variable names as unquoted literals
        Operand(const VariableName& var) : isVariableReference(true), string(Literal(false, var)) {}
        Operand(const Literal& l) : isVariableReference(false), string(l) {}

        // copy constructor is used so that builders work properly -- see comment under copyElements()
        Operand(const Operand& other) : isVariableReference(other.isVariableReference), string(other.string) {}

        const bool isVariableReference;
        const Literal string;
    }; // class Operand

  class Key : public UObject {
  // A key is either a literal or the "wildcard" symbol.
  public:
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

  // Represents the left-hand side of a `when` clause
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
          // TODO: is addAll() necessary?
          SelectorKeys* build(UErrorCode& errorCode) const;
      }; // class SelectorKeys::Builder
      static Builder* builder(UErrorCode& errorCode);

  private:
      friend class List<SelectorKeys>;
      friend class VariantMap;
      // SelectorKeys needs a copy constructor because VariantMap::Builder has a copying
      // build() method, which in turn is because we want MessageFormatDataModel::Builder to have a copying
      // build() method
      SelectorKeys(const SelectorKeys& other) : keys(new KeyList(*(other.keys))) {
          U_ASSERT(!other.isBogus());
      }

      const LocalPointer<KeyList> keys;
      bool isBogus() const { return !keys.isValid(); }
      // Adopts `keys`
      SelectorKeys(KeyList* ks) : keys(ks) {}
  }; // class SelectorKeys

  class VariantMap : public UMemory {
      // Wraps an OrderedMap<Pattern>
      // Has a different put() function since we want to stringify the key
      // list to hash it
      
  public:
      static constexpr size_t FIRST = OrderedMap<Pattern>::FIRST;
      // Passing a SelectorKeys*& (same for Pattern*&) is to avoid
      // copying, since List::get() returns a T*
      bool next(size_t &pos, const SelectorKeys*& k, const Pattern*& v) const;
      class Builder : public UMemory {
      public:
          Builder& add(SelectorKeys* key, Pattern* value, UErrorCode& errorCode);
          // this should be a *copying* build() (leaves `this` valid)
          // Needs to be const to enforce that when MessageFormatDataModel
          // constructor calls VariantMap::build(), it copies the variants
          // rather than moving them
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
          // it would be an error for `vs` and `ks` to have different sizes
          U_ASSERT(vs->contents->count() == ((int32_t) ks->length()));
      }
      const LocalPointer<OrderedMap<Pattern>> contents;
      // TODO: this is really annoying
      // however, it will be in the same order as the contents of the OrderedMap
      const LocalPointer<List<SelectorKeys>> keyLists;
  }; // class VariantMap

  // Matching the draft schema at https://github.com/unicode-org/message-format-wg/pull/393/ ,
  // `Reserved` is exposed
  // Corresponds to `reserved` in the grammar
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
          // TODO: is addAll() necessary?
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

  class Operator : public UMemory {
      // An operator represents either a function name together with
      // a list of options, which may be empty;
      // or a reserved sequence (which has no meaning and may result
      // in a formatting error).
  public:
      const FunctionName& getFunctionName() const;
      const Reserved& asReserved() const;
      const OptionMap &getOptions() const;
      bool isReserved() const { return isReservedSequence; }

      class Builder {
      private:
          friend class Operator;
          Builder() {}
          LocalPointer<Reserved> asReserved;
          LocalPointer<UnicodeString> functionName;
          LocalPointer<OptionMap::Builder> options;
      public:
            Builder& setReserved(Reserved* reserved);
            Builder& setFunctionName(UnicodeString* func);
            Builder& addOption(const UnicodeString &key, Operand* value, UErrorCode& errorCode);
            Operator* build(UErrorCode& errorCode) const;
        };

       static Builder* builder(UErrorCode& errorCode);
  private:
      friend class Expression; // makes copy constructor easier
      
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
      Operator(const Reserved& r) : isReservedSequence(true), functionName(""), options(nullptr), reserved(new Reserved(r)) {}

      // Operator needs a copy constructor in order to make Expression deeply copyable
      Operator(const Operator& other);

      bool isBogus() const;
      const bool isReservedSequence;
      const FunctionName functionName;
      // Non-const for copy constructors, effectively const
      /* const */ LocalPointer<OptionMap> options;
      /* const */ LocalPointer<Reserved> reserved;
  }; // class Operator
  
  class Expression : public UObject {
  public:

      // TODO: include these or not?
      bool isStandaloneAnnotation() const;
      // Returns true for function calls with operands as well as
      // standalone annotations.
      // Reserved sequences are not function calls
      bool isFunctionCall() const;
      bool isReserved() const;
      const Operator& getOperator() const;
      const Operand& getOperand() const;

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
      friend class PatternPart;      // makes copy constructors easier
      friend class Binding;          // makes copy constructors easier
      friend class List<Expression>; // makes copy constructors easier

        /*
          An expression is represented as the application of an optional operator to an optional operand.
          For example (using a made-up quasi-s-expression notation):

          { |42| :fun opt=value } => Expression(operator=Some(fun, {opt: value}),
                                                operand=Some(Literal(42)))
          abcd                    => Expression(operator=None, operand=Some(String("abcd")))
          { : fun opt=value }     => Expression(operator=Some(fun, {opt: value}),
                                                operand=None)

          An expression where both operand and operator are None can't be constructed.
        */

      // Expression needs a copy constructor in order to make Pattern deeply copyable
      Expression(const Expression& other);

      // Here, a separate variable isBogus tracks if any copies failed.
      // This is because rator = nullptr and rand = nullptr are semantic here,
      // so this can't just be a predicate that checks if those are null
      bool bogus = false; // copy constructors explicitly set this to true on failure

      bool isBogus() const;

      Expression(const Operator &rAtor, const Operand &rAnd) : rator(new Operator(rAtor)), rand(new Operand(rAnd)) {}
      Expression(const Operand &rAnd) : rator(nullptr), rand(new Operand(rAnd)){}
      Expression(const Operator &rAtor) : rator(new Operator(rAtor)), rand(nullptr) {}
      /*
        TODO: I thought about using an Optional class here instead of nullable
        pointers. But that doesn't really work since we can't use the STL and therefore
        can't use `std::move` and therefore can't use move constructors/move assignment
        operators.
      */

        // Non-const for copy constructors; effectively const
        /* const */ LocalPointer<Operator> rator;
        /* const */ LocalPointer<Operand> rand;
    }; // class Expression

    // TODO: No builder, for now, since this is just created in one step
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

      // Either Expression or TextPart can show up in a pattern
      // This class exists so Text can be distinguished from Expression
      // when serializing a Pattern
      const bool isRawText;
      // Not used if !isRawText
      const UnicodeString text;
      // null if isRawText
      const LocalPointer<Expression> expression;
      
      bool isBogus() const { return (!isRawText && !expression.isValid()); }
  }; // class PatternPart

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
          // TODO: is addAll() necessary?
          Pattern *build(UErrorCode &errorCode);
      }; // class Pattern::Builder

      static Builder *builder(UErrorCode &errorCode);
      
  private:
      friend class MessageFormatDataModel;
      friend class List<PatternPart>;
      friend class OrderedMap<Pattern>;

      // Possibly-empty list of parts
      const LocalPointer<List<PatternPart>> parts;
      
      bool isBogus() const { return !parts.isValid(); }
      // Can only be called by Builder
      // Takes ownership of `ps`
      Pattern(List<PatternPart> *ps) : parts(ps) { U_ASSERT(ps != nullptr); }

      // If the copy of the other list fails,
      // then isBogus() will be true for this Pattern
      // Pattern needs a copy constructor in order to make MessageFormatDataModel::build() be a copying rather than
      // moving build
      Pattern(const Pattern& other) : parts(new List<PatternPart>(*(other.parts))) { U_ASSERT(!other.isBogus()); }
    }; // class Pattern

  class Binding {
  public:
       static Binding* create(const UnicodeString& var, Expression* e, UErrorCode& errorCode);
       Binding(const UnicodeString& v, Expression* e) : var(v), value(e) {}
       // This needs a copy constructor so that `Bindings` is deeply-copyable,
       // which is in turn so that MessageFormatDataModel::build() can be copying
       // (it has to copy the builder's locals)
       Binding(const Binding& other) : var(other.var), value(new Expression(*other.value)) {
         U_ASSERT(!other.isBogus());
       }
         
       const UnicodeString var;
       // Postcondition: result is non-null
       const Expression* getValue() const {
           U_ASSERT(!isBogus());
           return value.getAlias();
       }
  private:
       const LocalPointer<Expression> value;
       bool isBogus() const { return !value.isValid(); }
  }; // class Binding

  const Bindings& getLocalVariables() const { return *bindings; }

  // This is so that we can avoid having getSelectors(), getVariants(),
  // and getPattern() take error codes / signal an error if you call the wrong one
  // TODO
  bool hasSelectors() const;
  const ExpressionList& getSelectors() const;
  const VariantMap& getVariants() const;
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

