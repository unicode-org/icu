/*
*******************************************************************************
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*   Date        Name        Description
*   03/22/00    aliu        Creation.
*   07/13/00    Madhu       Added more tests 
*******************************************************************************
*/

#include "cintltst.h"
#include "uhash.h"
#include "unicode/ctest.h"
#include "unicode/ustring.h"
#include "cstring.h"

/**********************************************************************
 * Prototypes
 *********************************************************************/

static void TestBasic(void);
static void TestOtherAPI(void);

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
   
    addTest(root, &TestBasic,   "tsutil/chashtst/TestBasic");
    addTest(root, &TestOtherAPI, "tsutil/chashtst/TestOtherAPI");
    
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

    if(uhash_compareChars((void*)"one", (void*)"three") == TRUE ||
        uhash_compareChars((void*)"one", (void*)"one") != TRUE ||
        uhash_compareChars((void*)"one", NULL) == TRUE  )  {
        log_err("FAIL: compareChars failed\n");
    }
    if(uhash_compareIChars((void*)"one", (void*)"three") == TRUE ||
        uhash_compareIChars((void*)"one", (void*)"one") != TRUE ||
        uhash_compareIChars((void*)"one", NULL) == TRUE  )  {
        log_err("FAIL: compareIChars failed\n");
    }
     
    uhash_close(hash);

}
void TestOtherAPI(void){
    
    UErrorCode status = U_ZERO_ERROR;
    UHashtable *hash;
    UChar key[10];
    

    hash = uhash_open(uhash_hashUChars, uhash_compareUChars,  &status);
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

    uhash_put(hash, (void*)u_uastrcpy(key, "one"), (void*)1, &status);
    if(uhash_count(hash) != 1){
         log_err("FAIL: uhas_count() failed. Expected: 1, Got: %d\n", uhash_count(hash));
     }
    uhash_put(hash, (void*)u_uastrcpy(key, "two"), (void*)2, &status);
    uhash_put(hash, (void*)u_uastrcpy(key, "three"), (void*)3, &status);
    uhash_put(hash, (void*)u_uastrcpy(key, "four"), (void*)4, &status);
    uhash_put(hash, (void*)u_uastrcpy(key, "five"), (void*)5, &status);
    
    if(uhash_count(hash) != 5){
        log_err("FAIL: uhas_count() failed. Expected: 5, Got: %d\n", uhash_count(hash));
    }
    
    if((int32_t)uhash_get(hash, (void*)u_uastrcpy(key, "two")) != 2){
        log_err("FAIL: uhash_get failed\n");
    }
    
    if((int32_t)uhash_remove(hash, (void*)u_uastrcpy(key, "five")) != 5){
        log_err("FAIL: uhash_remove() failed\n");
    }
    if(uhash_count(hash) != 4){
        log_err("FAIL: uhas_count() failed. Expected: 4, Got: %d\n", uhash_count(hash));
    }

    uhash_put(hash, (void*)u_uastrcpy(key, "one"), NULL, &status);
    if(uhash_count(hash) != 3){
        log_err("FAIL: uhash_put() with value=NULL didn't remove the key value pair\n");
    }
    status=U_ILLEGAL_ARGUMENT_ERROR;
    uhash_put(hash, (void*)u_uastrcpy(key, "one"), (void*)1, &status);
    if(uhash_count(hash) != 3){
        log_err("FAIL: uhash_put() with value=NULL should fail when status != U_ZERO_ERROR \n");
    }
    
    status=U_ZERO_ERROR;
    uhash_put(hash, (void*)u_uastrcpy(key, "one"), (void*)1, &status);
    if(uhash_compareUChars((void*)"one", (void*)"two") == TRUE ||
        uhash_compareUChars((void*)"one", (void*)"one") != TRUE ||
        uhash_compareUChars((void*)"one", NULL) == TRUE  )  {
        log_err("FAIL: compareUChars failed\n");
    }
   
    uhash_removeAll(hash);
    if(uhash_count(hash) != 0){
        log_err("FAIL: uhas_count() failed. Expected: 0, Got: %d\n", uhash_count(hash));
    }

    uhash_setKeyComparator(hash, uhash_compareLong);
    uhash_setKeyHasher(hash, uhash_hashLong);
    uhash_put(hash, (void*)1001, (void*)1, &status);
    uhash_put(hash, (void*)1002, (void*)2, &status);
    uhash_put(hash, (void*)1003, (void*)3, &status);
    if(uhash_compareLong((void*)1001, (void*)1002) == TRUE ||
        uhash_compareLong((void*)1001, (void*)1001) != TRUE ||
        uhash_compareLong((void*)1001, NULL) == TRUE  )  {
        log_err("FAIL: compareLong failed\n");
    }
    /*set the resize policy to just GROW and SHRINK*/
         /*how to test this??*/
    uhash_setResizePolicy(hash, U_GROW_AND_SHRINK);
    uhash_put(hash, (void*)1004, (void*)4, &status);
    uhash_put(hash, (void*)1005, (void*)5, &status);
    uhash_put(hash, (void*)1006, (void*)6, &status);
    if(uhash_count(hash) != 6){
        log_err("FAIL: uhash_count() failed. Expected: 6, Got: %d\n", uhash_count(hash));
    }
    if((int32_t)uhash_remove(hash, (void*)1004) != 4){
        log_err("FAIL: uhash_remove failed\n");
    }
    if((int32_t)uhash_remove(hash, (void*)1004) != 0){
        log_err("FAIL: uhash_remove failed\n");
    }
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
