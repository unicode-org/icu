// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef U_HIDE_DEPRECATED_API

#ifndef MESSAGEFORMAT_UTILS_IMPL_H
#define MESSAGEFORMAT_UTILS_IMPL_H

#if U_SHOW_CPLUSPLUS_API

#if !UCONFIG_NO_FORMATTING

template<typename T>
int32_t ImmutableVector<T>::length() const {
    U_ASSERT(!isBogus());
    return contents->size();
}

template<typename T>
const T* ImmutableVector<T>::get(int32_t i) const {
    // Because UVector::element() returns a void*,
    // to avoid either copying the result or returning a reference
    // to a temporary value, get() returns a T*
    U_ASSERT(!isBogus());
    U_ASSERT(i < length());
    return static_cast<const T *>(contents->elementAt(i));
}

// Returns true iff this contains `element`
template<typename T>
UBool ImmutableVector<T>::contains(const T& element) const {
    U_ASSERT(!isBogus());

    int32_t index;
    return find(element, index);
}

// Returns true iff this contains `element` and returns
// its first index in `index`. Returns false otherwise
template<typename T>
UBool ImmutableVector<T>::find(const T& element, int32_t& index) const {
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
template<typename T>
ImmutableVector<T>::ImmutableVector(const ImmutableVector<T>& other) {
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

// Adopts its argument
template<typename T>
typename ImmutableVector<T>::Builder& ImmutableVector<T>::Builder::add(T *element, UErrorCode &errorCode) {
    THIS_ON_ERROR(errorCode);
    U_ASSERT(contents != nullptr);
    contents->adoptElement(element, errorCode);
    return *this;
}

// Postcondition: U_FAILURE(errorCode) or returns a list such that isBogus() = false
template<typename T>
ImmutableVector<T>* ImmutableVector<T>::Builder::build(UErrorCode &errorCode) const {
    NULL_ON_ERROR(errorCode);
    LocalPointer<ImmutableVector<T>> adopted(buildList(*this, errorCode));
    if (!adopted.isValid() || adopted->isBogus()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return adopted.orphan();
}

template<typename T>
ImmutableVector<T>::Builder::Builder(UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);
    contents.adoptInstead(new UVector(errorCode));
    CHECK_ERROR(errorCode);
    contents->setDeleter(uprv_deleteUObject);
}

template<typename T>
/* static */ typename ImmutableVector<T>::Builder* ImmutableVector<T>::builder(UErrorCode &errorCode) {
    NULL_ON_ERROR(errorCode);
    LocalPointer<Builder> result(new Builder(errorCode));
    NULL_ON_ERROR(errorCode);
    return result.orphan();
}

// Helper functions for vector copying
// T1 must have a copy constructor
// This may leave dst->pointer == nullptr, which is handled by the UVector assign() method
template<typename T>
template<typename T1>
/* static */ void ImmutableVector<T>::copyElements(UElement *dst, UElement *src) {
    dst->pointer = new T1(*(static_cast<T1 *>(src->pointer)));
}

 // Copies the contents of `builder`
// This won't compile unless T is a type that has a copy assignment operator
template<typename T>
/* static */ ImmutableVector<T>* ImmutableVector<T>::buildList(const Builder &builder, UErrorCode &errorCode) {
    NULL_ON_ERROR(errorCode);
    ImmutableVector<T>* result;
    U_ASSERT(builder.contents != nullptr);

    LocalPointer<UVector> adoptedContents(new UVector(builder.contents->size(), errorCode));
    adoptedContents->assign(*builder.contents, &copyElements<T>, errorCode);
    NULL_ON_ERROR(errorCode);
    result = new ImmutableVector<T>(adoptedContents.orphan());

    // Finally, check for null
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
} 

// Iterates over keys in the order in which they were added.
// Returns true iff `pos` indicates that there are elements
 // remaining
template<typename V>
UBool OrderedMap<V>::next(int32_t &pos, UnicodeString& k, const V*& v) const {
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

template<typename V>
int32_t OrderedMap<V>::size() const {
    U_ASSERT(!isBogus());
    return keys->size();
}

// Copy constructor
template<typename V>
OrderedMap<V>::OrderedMap(const OrderedMap<V>& other) : contents(copyHashtable(*other.contents)), keys(copyStringVector(*other.keys)) {
    U_ASSERT(!other.isBogus());
}

// Adopts `value`
// Precondition: `key` is not already in the map. (The caller must
// check this)
template<typename V>
typename OrderedMap<V>::Builder& OrderedMap<V>::Builder::add(const UnicodeString& key, V* value, UErrorCode& errorCode) {
    THIS_ON_ERROR(errorCode);
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
template<typename V>
UBool OrderedMap<V>::Builder::has(const UnicodeString& key) const {
    return contents->containsKey(key);
}

// Copying `build()` (leaves `this` valid)
template<typename V>
OrderedMap<V>* OrderedMap<V>::Builder::build(UErrorCode& errorCode) const {
    NULL_ON_ERROR(errorCode);

    LocalPointer<Hashtable> adoptedContents(copyHashtable(*contents));
    LocalPointer<UVector> adoptedKeys(copyStringVector(*keys));

    if (!adoptedContents.isValid() || !adoptedKeys.isValid()) {
        return nullptr;
    }
    LocalPointer<OrderedMap<V>> result(
        OrderedMap<V>::create(adoptedContents.orphan(),
                              adoptedKeys.orphan(),
                              errorCode));
    NULL_ON_ERROR(errorCode);
    return result.orphan();
}

// Only called by builder()
template<typename V>
OrderedMap<V>::Builder::Builder(UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);
    // initialize `keys`
    keys.adoptInstead(new UVector(errorCode));
    CHECK_ERROR(errorCode);
    keys->setDeleter(uprv_deleteUObject);
 
    // initialize `contents`
    // No value comparator needed
    contents.adoptInstead(new Hashtable(compareVariableName, nullptr, errorCode));
    CHECK_ERROR(errorCode);
    // The `contents` hashtable owns the values, but does not own the keys
    contents->setValueDeleter(uprv_deleteUObject);
}

template<typename V>
/* static */ typename OrderedMap<V>::Builder* OrderedMap<V>::builder(UErrorCode &errorCode) {
    NULL_ON_ERROR(errorCode);
    LocalPointer<Builder> result(new Builder(errorCode));
    NULL_ON_ERROR(errorCode);
    return result.orphan();
}

// Helper methods for copy constructor
template<typename V>
/* static */ void OrderedMap<V>::copyStrings(UElement *dst, UElement *src) {
    dst->pointer = new UnicodeString(*(static_cast<UnicodeString *>(src->pointer)));
}

template<typename V>
/* static */ UVector* OrderedMap<V>::copyStringVector(const UVector& other) {
    UErrorCode errorCode = U_ZERO_ERROR;
    LocalPointer<UVector> adoptedKeys(new UVector(other.size(), errorCode));
    NULL_ON_ERROR(errorCode);
    adoptedKeys->assign(other, &copyStrings, errorCode);
    NULL_ON_ERROR(errorCode);
    return adoptedKeys.orphan();
}

// Postcondition: U_FAILURE(errorCode) || !((return value).isBogus())
template<typename V>
/* static */ OrderedMap<V>* OrderedMap<V>::create(Hashtable* c, UVector* k, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    LocalPointer<OrderedMap<V>> result(new OrderedMap<V>(c, k));
    if (result == nullptr || result->isBogus()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return result.orphan();
}

template<typename V>
/* static */ Hashtable* OrderedMap<V>::copyHashtable(const Hashtable& other) {
    UErrorCode errorCode = U_ZERO_ERROR;
    // No value comparator needed
    LocalPointer<Hashtable> adoptedContents(new Hashtable(compareVariableName, nullptr, errorCode));
    NULL_ON_ERROR(errorCode);
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
    }
    NULL_ON_ERROR(errorCode);
    return adoptedContents.orphan();
}

template<typename V>
OrderedMap<V>::OrderedMap(Hashtable* c, UVector* k) : contents(c), keys(k) {
        // It would be an error if `c` and `k` had different sizes
        U_ASSERT(c->count() == k->size());
}

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // MESSAGEFORMAT_UTILS_IMPL_H

#endif // U_HIDE_DEPRECATED_API
// eof

