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


/**
 * The `ImmutableVector` class represents a polymorphic immutable list,
 * constructed using the builder pattern. It's used to represent
 * various nodes in the MessageFormat data model that may have a
 * variable number of components.
 *
 * @internal ICU 74.0 technology preview
 * @deprecated This API is for technology preview only.
 */
template<typename T>
class U_I18N_API ImmutableVector : public UMemory {

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
    /**
     * Size accessor.
     *
     * @return   The number of elements in this list.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    int32_t length() const;

    /**
     * Element accessor.
     * Precondition: i < length()
     *
     * @param i The index to access.
     * @return   The list element at `i`
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    const T* get(int32_t i) const;

    /**
     * Checks for the existence of an element.
     *
     * @param element The item to search for.
     * @return        True if and only if `element` occurs in this list.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    UBool contains(const T& element) const;

    /**
     * Finds the index of an element.
     *
     * @param element The item to search for.
     * @param index   A mutable reference that is set to the first index
     *                where `element` occurs in this list, if it occurs.
     * @return        True if and only if `element` occurs in this list.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    UBool find(const T& element, int32_t& index) const;

    /**
     * Copy constructor. Performs a deep copy (`T` must have
     * a copy constructor.)
     *
     * @param other   The ImmutableVector to copy.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    ImmutableVector(const ImmutableVector<T>& other);

    /**
     * The mutable `ImmutableVector::Builder` class allows the list to be constructed
     * one element at a time.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    class U_I18N_API Builder : public UMemory {
    public:
        /**
         * Adds to the list. Adopts `element`.
         *
         * @param element The element to be added.
         * @param status Input/output error code.
         * @return A reference to the builder.
         *
         * @internal ICU 74.0 technology preview
         * @deprecated This API is for technology preview only.
         */
        Builder& add(T *element, UErrorCode& status);
        /**
         * Constructs a new `ImmutableVector` using the list of elements
         * set with previous `add()` calls.
         *
         * The builder object (`this`) can still be used after calling `build()`.
         *
         * @param status    Input/output error code.
         * @return          The new ImmutableVector, which is non-null if U_SUCCESS(status).
         *
         * @internal ICU 74.0 technology preview
         * @deprecated This API is for technology preview only.
         */
        ImmutableVector<T>* build(UErrorCode &errorCode) const;

    private:
        friend class ImmutableVector;
        LocalPointer<UVector> contents;
        Builder(UErrorCode& errorCode);
    }; // class ImmutableVector::Builder

    /**
     * Returns a new `ImmutableVector::Builder` object.
     *
     * @param status  Input/output error code.
     * @return The new Builder object, which is non-null if U_SUCCESS(status).
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
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


/**
 * The `OrderedMap` class represents a polymorphic hash table with string
 * keys, constructed using the builder pattern. It's used to represent
 * various nodes in the MessageFormat data model that may have a
 * variable number of named components. The map records the order in which
 * keys were added and iterates over its elements in that order.
 *
 * @internal ICU 74.0 technology preview
 * @deprecated This API is for technology preview only.
 */
template<typename V>
class U_I18N_API OrderedMap : public UMemory {
class MessageFormatDataModel {
    class Operator;
    class VariantMap;
};

private:
    // See comments under `ImmutableVector::isBogus()`
    bool isBogus() const { return (!contents.isValid() || !keys.isValid()); }

public:
    /**
     * Used with `next()`.
     *
     * The initial iterator position for `next()`.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    static constexpr int32_t FIRST = 0;
    /**
     * Iterates over all keys in the order in which they were added.
     *
     * @param pos A mutable reference to the current iterator position. Should be set to
     *            `FIRST` before the first call to `next()`.
     * @param k   A mutable reference that is set to the name of the next key
     *            if the return value is true.
     * @param v   A mutable reference to a pointer to an element of the map's value type,
     *            which is non-null if the return value is true.
     * @return True if and only if there are elements starting at `pos`.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    UBool next(int32_t &pos, UnicodeString& k, const V*& v) const;
    /**
     * Size accessor.
     *
     * @return   The number of elements in this map.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    int32_t size() const;

    /**
     * Copy constructor. Performs a deep copy (`V` must have
     * a copy constructor.)
     *
     * @param other   The OrderedMap to copy.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    OrderedMap<V>(const OrderedMap<V>& other);

    /**
     * The mutable `OrderedMap::Builder` class allows the map to be constructed
     * one key/value pair at a time.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    class U_I18N_API Builder : public UMemory {
    public:
        /**
         * Adds to the map. Adopts `value`.
         * Precondition: !has(key)
         *
         * @param key    The name to be added. It is an internal error to
         *               call `add()` with a key that has already been added.
         * @param value  The value to be associated with the name.
         * @param status Input/output error code.
         * @return A reference to the builder.
         *
         * @internal ICU 74.0 technology preview
         * @deprecated This API is for technology preview only.
         */
        Builder& add(const UnicodeString& key, V* value, UErrorCode& status);
        /**
         * Checks if a key is in the map.
         *
         * @param key Reference to a (string) key.
         * @return    True if and only if `key` is mapped to a value in the map.
         *
         * @internal ICU 74.0 technology preview
         * @deprecated This API is for technology preview only.
         */
        UBool has(const UnicodeString& key) const;
        /**
         * Constructs a new `OrderedMap` using the keys and values
         * set with previous `add()` calls.
         *
         * The builder object (`this`) can still be used after calling `build()`.
         *
         * @param status    Input/output error code.
         * @return          The new OrderedMap, which is non-null if U_SUCCESS(status).
         *
         * @internal ICU 74.0 technology preview
         * @deprecated This API is for technology preview only.
         */
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

