/*
*******************************************************************************
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*   Date        Name        Description
*   03/22/00    aliu        Creation.
*******************************************************************************
*/

#include "cintltst.h"
#include "uhash.h"
#include "unicode/ctest.h"
#include "cstring.h"

/**********************************************************************
 * Prototypes
 *********************************************************************/

void TestBasic(void);

int32_t hashChars(const void* key);

UBool isEqualChars(const void* key1, const void* key2);

static void _put(UHashtable* hash,
                 const char* key,
                 int32_t value,
                 int32_t expectedOldValue);

void _get(UHashtable* hash,
          const char* key,
          int32_t expectedValue);

void _remove(UHashtable* hash,
             const char* key,
             int32_t expectedValue);

/**********************************************************************
 * FW Registration
 *********************************************************************/

void addHashtableTest(TestNode** root) {
    addTest(root, &TestBasic, "tsutil/chashtst/TestBasic");
}

/**********************************************************************
 * Test Functions
 *********************************************************************/

void TestBasic(void) {
    UErrorCode status = U_ZERO_ERROR;
    UHashtable *hash;

    hash = uhash_open(hashChars, isEqualChars,  &status);
    if (U_FAILURE(status)) {
        log_err("FAIL: uhash_open failed with %s and returned 0x%08x\n",
                u_errorName(status), hash);
        return;
    }
    if (hash == NULL) {
        log_err("FAIL: uhash_open returned NULL\n");
        return;
    }
    log_verbose("Ok: uhash_open returned 0x%08X\n", hash);

    _put(hash, "one", 1, 0);
    _put(hash, "omega", 24, 0);
    _put(hash, "two", 2, 0);
    _put(hash, "three", 3, 0);
    _put(hash, "one", -1, 1);
    _put(hash, "two", -2, 2);
    _put(hash, "omega", 48, 24);
    _put(hash, "one", 100, -1);
    _get(hash, "three", 3);
    _remove(hash, "two", -2);
    _get(hash, "two", 0);
    _get(hash, "one", 100);
    _put(hash, "two", 200, 0);
    _get(hash, "omega", 48);
    _get(hash, "two", 200);

    uhash_close(hash);
}

/**********************************************************************
 * uhash Callbacks
 *********************************************************************/

/**
 * This hash function is designed to collide a lot to test key equality
 * resolution.  It only uses the first char.
 */
int32_t hashChars(const void* key) {
    return *(const char*) key;
}

UBool isEqualChars(const void* key1, const void* key2) {
    return (key1 != NULL) &&
        (key2 != NULL) &&
        (uprv_strcmp(key1, key2) == 0);
}

/**********************************************************************
 * Wrapper Functions
 *********************************************************************/

void _put(UHashtable* hash,
          const char* key,
          int32_t value,
          int32_t expectedOldValue) {
    UErrorCode status = U_ZERO_ERROR;
    int32_t oldValue = (int32_t)
        uhash_put(hash, (void*) key, (void*) value, &status);
    if (U_FAILURE(status)) {
        log_err("FAIL: uhash_put(%s) failed with %s and returned %ld\n",
                key, u_errorName(status), oldValue);
    } else if (oldValue != expectedOldValue) {
        log_err("FAIL: uhash_put(%s) returned old value %ld; expected %ld\n",
                key, oldValue, expectedOldValue);
    } else {
        log_verbose("Ok: uhash_put(%s, %d) returned old value %ld\n",
                    key, value, oldValue);
    }
}

void _get(UHashtable* hash,
          const char* key,
          int32_t expectedValue) {
    UErrorCode status = U_ZERO_ERROR;
    int32_t value = (int32_t) uhash_get(hash, key);
    if (U_FAILURE(status)) {
        log_err("FAIL: uhash_get(%s) failed with %s and returned %ld\n",
                key, u_errorName(status), value);
    } else if (value != expectedValue) {
        log_err("FAIL: uhash_get(%s) returned %ld; expected %ld\n",
                key, value, expectedValue);
    } else {
        log_verbose("Ok: uhash_get(%s) returned value %ld\n",
                    key, value);
    }
}

void _remove(UHashtable* hash,
             const char* key,
             int32_t expectedValue) {
    int32_t value = (int32_t) uhash_remove(hash, key);
    if (value != expectedValue) {
        log_err("FAIL: uhash_remove(%s) returned %ld; expected %ld\n",
                key, value, expectedValue);
    } else {
        log_verbose("Ok: uhash_remove(%s) returned old value %ld\n",
                    key, value);
    }
}
