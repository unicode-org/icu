/*
******************************************************************************
*   Copyright (C) 1997-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
******************************************************************************
*   Date        Name        Description
*   03/28/00    aliu        Creation.
******************************************************************************
*/

#ifndef HASH_H
#define HASH_H

#include "uhash.h"
#include "unicode/unistr.h"

U_NAMESPACE_BEGIN

/**
 * Hashtable is a thin C++ wrapper around UHashtable, a general-purpose void*
 * hashtable implemented in C.  Hashtable is designed to be idiomatic and
 * easy-to-use in C++.
 *
 * Hashtable is an INTERNAL CLASS.
 */
class Hashtable {
    UHashtable* hash;

public:
    Hashtable(UBool ignoreKeyCase, UErrorCode& status);

    /**
     * Construct a hashtable, _disregarding any error_.  Use this constructor
     * with caution.
     * @param ignoreKeyCase if TRUE, keys are case insensitive
     */
    Hashtable(UBool ignoreKeyCase = FALSE);

    /**
     * Non-virtual destructor; make this virtual if Hashtable is subclassed
     * in the future.
     */
    ~Hashtable();

    UObjectDeleter setValueDeleter(UObjectDeleter fn);

    int32_t count() const;

    void* put(const UnicodeString& key, void* value, UErrorCode& status);

    int32_t puti(const UnicodeString& key, int32_t value, UErrorCode& status);

    void* get(const UnicodeString& key) const;
    
    int32_t geti(const UnicodeString& key) const;
    
    void* remove(const UnicodeString& key);

    int32_t removei(const UnicodeString& key);

    const UHashElement* find(const UnicodeString& key) const;

    const UHashElement* nextElement(int32_t& pos) const;
};

/*********************************************************************
 * Implementation
 ********************************************************************/

inline Hashtable::Hashtable(UBool ignoreKeyCase, UErrorCode& status) :
    hash(0) {
    if (U_FAILURE(status)) {
        return;
    }
    hash = uhash_open(ignoreKeyCase ? uhash_hashCaselessUnicodeString
                                    : uhash_hashUnicodeString,
                      ignoreKeyCase ? uhash_compareCaselessUnicodeString
                                    : uhash_compareUnicodeString,
                      &status);
    if (U_SUCCESS(status)) {
        uhash_setKeyDeleter(hash, uhash_deleteUnicodeString);
    }
}

inline Hashtable::Hashtable(UBool ignoreKeyCase) : hash(0) {
    UErrorCode status = U_ZERO_ERROR;
    hash = uhash_open(ignoreKeyCase ? uhash_hashCaselessUnicodeString
                                    : uhash_hashUnicodeString,
                      ignoreKeyCase ? uhash_compareCaselessUnicodeString
                                    : uhash_compareUnicodeString,
                      &status);
    if (U_SUCCESS(status)) {
        uhash_setKeyDeleter(hash, uhash_deleteUnicodeString);
    }
}

inline Hashtable::~Hashtable() {
    if (hash != 0) {
        uhash_close(hash);
        hash = 0;
    }
}

inline UObjectDeleter Hashtable::setValueDeleter(UObjectDeleter fn) {
    return uhash_setValueDeleter(hash, fn);
}

inline int32_t Hashtable::count() const {
    return uhash_count(hash);
}

inline void* Hashtable::put(const UnicodeString& key, void* value, UErrorCode& status) {
    return uhash_put(hash, new UnicodeString(key), value, &status);
}

inline int32_t Hashtable::puti(const UnicodeString& key, int32_t value, UErrorCode& status) {
    return uhash_puti(hash, new UnicodeString(key), value, &status);
}

inline void* Hashtable::get(const UnicodeString& key) const {
    return uhash_get(hash, &key);
}

inline int32_t Hashtable::geti(const UnicodeString& key) const {
    return uhash_geti(hash, &key);
}

inline void* Hashtable::remove(const UnicodeString& key) {
    return uhash_remove(hash, &key);
}

inline int32_t Hashtable::removei(const UnicodeString& key) {
    return uhash_removei(hash, &key);
}

inline const UHashElement* Hashtable::find(const UnicodeString& key) const {
    return uhash_find(hash, &key);
}

inline const UHashElement* Hashtable::nextElement(int32_t& pos) const {
    return uhash_nextElement(hash, &pos);
}

U_NAMESPACE_END

#endif
