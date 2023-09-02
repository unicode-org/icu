// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef U_HIDE_DEPRECATED_API

#ifndef MESSAGEFORMAT_UTILS_H
#define MESSAGEFORMAT_UTILS_H

#if U_SHOW_CPLUSPLUS_API

#if !UCONFIG_NO_FORMATTING

#include "unicode/messageformat2_macros.h"
#include "unicode/unistr.h"
#include "unicode/utypes.h"
#include "hash.h"
#include "uvector.h"

U_NAMESPACE_BEGIN namespace message2 {

// Defined for convenience, in case we end up using a different
// representation in the data model for variable references and/or
// variable definitions
static UBool compareVariableName(const UElement e1, const UElement e2) {
    return uhash_compareUnicodeString(e1, e2);
}

// Polymorphic immutable list class (constructed using the builder pattern),
// that uses a UVector as its underlying representation
template<typename T>
class ImmutableVector : public UMemory {

private:
    // If a copy constructor fails, the list is left in an inconsistent state,
    // because copying has to allocate a new vector.
    // Copy constructors can't take error codes as arguments. So we have to
    // resort to this, and all methods must check the invariant and signal an
    // error if it's false. The error should be U_MEMORY_ALLOCATION_ERROR,
    // since isBogus iff an allocation failed.
    // For classes that contain a ImmutableVector member, there is no guarantee that
    // the list will be non-bogus. ImmutableVector operations use assertions to detect
    // this condition as early as possible.
    bool isBogus() const { return !contents.isValid(); }

public:
    int32_t length() const;

    // Precondition: i < length()
    // Because UVector::element() returns a void*,
    // to avoid either copying the result or returning a reference
    // to a temporary value, get() returns a T* 
    const T* get(int32_t i) const;

    // Returns true iff this contains `element`
    UBool contains(const T& element) const;

    // Returns true iff this contains `element` and returns
    // its first index in `index`. Returns false otherwise
    UBool find(const T& element, int32_t& index) const;

    // Copy constructor
    ImmutableVector(const ImmutableVector<T>& other);

    class Builder : public UMemory {
    public:
        // Adopts its argument
        Builder& add(T *element, UErrorCode &errorCode);
        // Postcondition: U_FAILURE(errorCode) or returns a list such that isBogus() = false
        ImmutableVector<T>* build(UErrorCode &errorCode) const;

    private:
        friend class ImmutableVector;
        LocalPointer<UVector> contents;
        Builder(UErrorCode& errorCode);
    }; // class ImmutableVector::Builder

    static Builder* builder(UErrorCode &errorCode);

private:
    friend class Builder; // NOTE: Builder should only call buildList(); not the constructors

    // Helper functions for vector copying
    // T1 must have a copy constructor
    // This may leave dst->pointer == nullptr, which is handled by the UVector assign() method
    template <typename T1>
    static void copyElements(UElement *dst, UElement *src);

    // Copies the contents of `builder`
    // This won't compile unless T is a type that has a copy assignment operator
    static ImmutableVector<T>* buildList(const Builder &builder, UErrorCode &errorCode);

    // Adopts `contents`
    ImmutableVector(UVector* things) : contents(things) { U_ASSERT(things != nullptr); }

    // Used as const, but since UVector doesn't have a copy constructor,
    // writing the copy constructor for ImmutableVector requires `contents` to be non-const
    LocalPointer<UVector> contents;
}; // class ImmutableVector

// Immutable polymorphic ordered map from strings to V*
// Preserves the order in which keys are added.
template<typename V>
class OrderedMap : public UMemory {
class MessageFormatDataModel {
    class Operator;
    class VariantMap;
};

private:
    // See comments under `ImmutableVector::isBogus()`
    bool isBogus() const { return (!contents.isValid() || !keys.isValid()); }

public:
    static constexpr int32_t FIRST = 0;
    // Iterates over keys in the order in which they were added.
    // Returns true iff `pos` indicates that there are elements
    // remaining
    UBool next(int32_t &pos, UnicodeString& k, const V*& v) const;
    int32_t size() const;

    // Copy constructor
    OrderedMap<V>(const OrderedMap<V>& other);

    class Builder : public UMemory {
    public:
        // Adopts `value`
        // Precondition: `key` is not already in the map. (The caller must
        // check this)
        Builder& add(const UnicodeString& key, V* value, UErrorCode& errorCode);
        // This is provided so that builders can check for duplicate keys
        // (for example, adding duplicate options is an error)
        UBool has(const UnicodeString& key) const;
        // Copying `build()` (leaves `this` valid)
        OrderedMap<V>* build(UErrorCode& errorCode) const;
    private:
        friend class OrderedMap;
        
        // Only called by builder()
        Builder(UErrorCode& errorCode);

        // Hashtable representing the underlying map
        LocalPointer<Hashtable> contents;
        // Maintain a list of keys that encodes the order in which
        // keys are added. This wastes some space, but allows us to
        // re-use ICU4C's Hashtable abstraction without re-implementing
        // an ordered version of it.
        LocalPointer<UVector> keys;
    }; // class OrderedMap<V>::Builder

    static Builder* builder(UErrorCode &errorCode);

private:

    // Helper methods for copy constructor
    static void copyStrings(UElement *dst, UElement *src);
    static UVector* copyStringVector(const UVector& other);
    // Postcondition: U_FAILURE(errorCode) || !((return value).isBogus())
    static OrderedMap<V>* create(Hashtable* c, UVector* k, UErrorCode& errorCode);
    static Hashtable* copyHashtable(const Hashtable& other);
    OrderedMap<V>(Hashtable* c, UVector* k);
    // Hashtable representing the underlying map
    const LocalPointer<Hashtable> contents;
    // List of keys
    const LocalPointer<UVector> keys;
}; // class OrderedMap<V>

#include "messageformat2_utils_impl.h"

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // MESSAGEFORMAT_UTILS_H

#endif // U_HIDE_DEPRECATED_API
// eof

