/*
*******************************************************************************
*   Copyright (C) 1997-2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*   Date        Name        Description
*   03/28/00    aliu        Creation.
*******************************************************************************
*/

#ifndef HASH_H
#define HASH_H

#include "uhash.h"
#include "unicode/unistr.h"

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
    Hashtable(UErrorCode& status);

    /**
     * Non-virtual destructor; make this virtual if Hashtable is subclassed
     * in the future.
     */
    ~Hashtable();

    UObjectDeleter setValueDeleter(UObjectDeleter fn);

    int32_t count() const;

    void* put(const UnicodeString& key, void* value, UErrorCode& status);

    void* get(const UnicodeString& key) const;
    
    void* remove(const UnicodeString& key);

    bool_t nextElement(const UnicodeString*& key, void*& value, int32_t& pos) const;
};

/*********************************************************************
 * Implementation
 ********************************************************************/

inline Hashtable::Hashtable(UErrorCode& status) : hash(0) {
    if (U_FAILURE(status)) {
        return;
    }
    hash = uhash_open(uhash_hashUnicodeString,
                      uhash_compareUnicodeString, &status);
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

inline void* Hashtable::get(const UnicodeString& key) const {
    return uhash_get(hash, &key);
}

inline void* Hashtable::remove(const UnicodeString& key) {
    return uhash_remove(hash, &key);
}

inline bool_t Hashtable::nextElement(const UnicodeString*& key, void*& value, int32_t& pos) const {
    UHashElement *e = uhash_nextElement(hash, &pos);
    if (e != 0) {
        key = (const UnicodeString*) e->key;
        value = e->value;
        return TRUE;
    } else {
        return FALSE;
    }
}

#endif
