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

// Shared by OrderedMap and Environment
template<typename V>
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
        val = V::create(*(static_cast<V *>(e->value.pointer)));
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
    int32_t length() const {
        U_ASSERT(!isBogus());
        return contents->size();
    }

    // Precondition: i < length()
    // Because UVector::element() returns a void*,
    // to avoid either copying the result or returning a reference
    // to a temporary value, get() returns a T* 
    const T* get(int32_t i) const {
        U_ASSERT(!isBogus());
        U_ASSERT(i < length());
        return static_cast<const T *>(contents->elementAt(i));
    }

    // Returns true iff this contains `element`
    bool contains(const T& element) const {
        U_ASSERT(!isBogus());

        int32_t index;
        return find(element, index);
    }

    // Returns true iff this contains `element` and returns
    // its first index in `index`. Returns false otherwise
    bool find(const T& element, int32_t& index) const {
        U_ASSERT(!isBogus());

        for (int32_t i = 0; i < length(); i++) {
            if (*(get(i)) == element) {
                index = i;
                return true;
            }
        }
        return false;
    }

    // Copy constructor
    ImmutableVector(const ImmutableVector<T>& other) {
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
        
    class Builder : public UMemory {
    public:
        // Adopts its argument
        Builder& add(T *element, UErrorCode &errorCode) {
            if (U_FAILURE(errorCode)) {
                return *this;
            }
            U_ASSERT(contents != nullptr);
            contents->adoptElement(element, errorCode);
            return *this;
        }
        // Postcondition: U_FAILURE(errorCode) or returns a list such that isBogus() = false
        ImmutableVector<T>* build(UErrorCode &errorCode) const {
            if (U_FAILURE(errorCode)) {
                return nullptr;
            }
            LocalPointer<ImmutableVector<T>> adopted(buildList(*this, errorCode));
            if (!adopted.isValid() || adopted->isBogus()) {
                errorCode = U_MEMORY_ALLOCATION_ERROR;
                return nullptr;
            }
            return adopted.orphan();
        }

    private:
        friend class ImmutableVector;
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
    }; // class ImmutableVector::Builder

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

    // Helper functions for vector copying
    // T1 must have a copy constructor
    // This may leave dst->pointer == nullptr, which is handled by the UVector assign() method
    template <typename T1>
    static void copyElements(UElement *dst, UElement *src) {
        dst->pointer = new T1(*(static_cast<T1 *>(src->pointer)));
    }

    // Copies the contents of `builder`
    // This won't compile unless T is a type that has a copy assignment operator
    static ImmutableVector<T>* buildList(const Builder &builder, UErrorCode &errorCode) {
        if (U_FAILURE(errorCode)) {
            return nullptr;
        }
        ImmutableVector<T>* result;
        U_ASSERT(builder.contents != nullptr);

        LocalPointer<UVector> adoptedContents(new UVector(builder.contents->size(), errorCode));
        adoptedContents->assign(*builder.contents, &copyElements<T>, errorCode);
        if (U_FAILURE(errorCode)) {
            return nullptr;
        }
        result = new ImmutableVector<T>(adoptedContents.orphan());

        // Finally, check for null
        if (result == nullptr) {
            errorCode = U_MEMORY_ALLOCATION_ERROR;
        }
        return result;
    } 

    // Adopts `contents`
    ImmutableVector(UVector* things) : contents(things) {
        U_ASSERT(things != nullptr);
    }

    /* const */ LocalPointer<UVector> contents;
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
    bool next(int32_t &pos, UnicodeString& k, const V*& v) const {
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

    int32_t size() const {
        U_ASSERT(!isBogus());
        return keys->size();
    }

    // Copy constructor
    OrderedMap<V>(const OrderedMap<V>& other) : contents(copyHashtable<V>(*other.contents)), keys(copyStringVector(*other.keys)) {
        U_ASSERT(!other.isBogus());
    }

    class Builder : public UMemory {
    public:
        // Adopts `value`
        // Precondition: `key` is not already in the map. (The caller must
        // check this)
        Builder& add(const UnicodeString& key, V* value, UErrorCode& errorCode) {
            if (U_FAILURE(errorCode)) {
                return *this;
            }
            // Check that the key is not already in the map.
            // (If not for this check, the invariant that keys->size()
            // == contents->count() could be violated.)
            U_ASSERT(!contents->containsKey(key));
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
        // This is provided so that builders can check for duplicate keys
        // (for example, adding duplicate options is an error)
        bool has(const UnicodeString& key) const {
            return contents->containsKey(key);
        }
        // Copying `build()` (leaves `this` valid)
        OrderedMap<V>* build(UErrorCode& errorCode) const {
            if (U_FAILURE(errorCode)) {
                return nullptr;
            }

            LocalPointer<Hashtable> adoptedContents(copyHashtable<V>(*contents));
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

    // Helper methods for copy constructor
    static void copyStrings(UElement *dst, UElement *src) {
        dst->pointer = new UnicodeString(*(static_cast<UnicodeString *>(src->pointer)));
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


} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // MESSAGEFORMAT_UTILS_H

#endif // U_HIDE_DEPRECATED_API
// eof

