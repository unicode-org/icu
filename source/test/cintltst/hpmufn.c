/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 2003, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/*
* File hpmufn.c
*
*/

#include "unicode/utypes.h"
#include "unicode/uclean.h"
#include "unicode/uchar.h"
#include "unicode/ures.h"
#include "cintltst.h"

#include <stdlib.h>


static void TestHeapFunctions(void);
static void TestMutexFunctions(void);

void addHeapMutexTest(TestNode **root);


void
addHeapMutexTest(TestNode** root)
{
    addTest(root, &TestHeapFunctions,       "tsutil/TestHeapFunctions"      );
    addTest(root, &TestMutexFunctions,      "tsutil/TestMutexFunctions");
}


#define TEST_STATUS(status, expected) \
if (status != expected) { \
log_err("FAIL at  %s:%d. Actual status = \"%s\";  Expected status = \"%s\"\n", \
__FILE__, __LINE__, u_errorName(status), u_errorName(expected)); }


#define TEST_ASSERT(expr) \
if (!(expr)) { \
    log_err("FAILED Assertion \"" #expr "\" at  %s:%d.\n", __FILE__, __LINE__); \
}
    
/*
 *  Test Heap Functions.
 *    Implemented on top of the standard malloc heap.
 *    All blocks increased in size by 8 bytes, and the poiner returned to ICU is
 *       offset up by 8, which should cause a good heap corruption if one of our "blocks"
 *       ends up being freed directly, without coming through us.
 *    Allocations are counted, to check that ICU actually does call back to us.
 */
int    gBlockCount = 0;
const void  *gContext;

void   *myMemAlloc(const void *context, size_t size) {
    char *retPtr = (char *)malloc(size+8);
    if (retPtr != NULL) {
        retPtr += 8;
    }
    gBlockCount ++;
    return retPtr;
}

void  myMemFree(const void *context, void *mem) {
    char *freePtr = (char *)mem;
    if (freePtr != NULL) {
        freePtr -= 8;
    }
    free(freePtr);
}


    
void  *myMemRealloc(const void *context, void *mem, size_t size) {
    char *p = (char *)mem;
    char *retPtr;

    if (p!=NULL) {
        p -= 8;
    }
    retPtr = realloc(p, size+8);
    if (retPtr != NULL) {
        p += 8;
    }
    return retPtr;
}


static void TestHeapFunctions() {
    UErrorCode       status = U_ZERO_ERROR;
    UResourceBundle *rb     = NULL;


    /* Can not set memory functions if ICU is already initialized */
    u_setMemoryFunctions(&gContext, myMemAlloc, myMemRealloc, myMemFree, &status);
    TEST_STATUS(status, U_INVALID_STATE_ERROR);

    /* Un-initialize ICU */
    u_cleanup();

    /* Can not set memory functions with NULL values */
    status = U_ZERO_ERROR;
    u_setMemoryFunctions(&gContext, NULL, myMemRealloc, myMemFree, &status);
    TEST_STATUS(status, U_ILLEGAL_ARGUMENT_ERROR);
    status = U_ZERO_ERROR;
    u_setMemoryFunctions(&gContext, myMemAlloc, NULL, myMemFree, &status);
    TEST_STATUS(status, U_ILLEGAL_ARGUMENT_ERROR);
    status = U_ZERO_ERROR;
    u_setMemoryFunctions(&gContext, myMemAlloc, myMemRealloc, NULL, &status);
    TEST_STATUS(status, U_ILLEGAL_ARGUMENT_ERROR);

    /* u_setMemoryFunctions() should work with null or non-null context pointer */
    status = U_ZERO_ERROR;
    u_setMemoryFunctions(NULL, myMemAlloc, myMemRealloc, myMemFree, &status);
    TEST_STATUS(status, U_ZERO_ERROR);
    u_setMemoryFunctions(&gContext, myMemAlloc, myMemRealloc, myMemFree, &status);
    TEST_STATUS(status, U_ZERO_ERROR);


    /* After reinitializing ICU, we should not be able to set the memory funcs again. */
    status = U_ZERO_ERROR;
    u_init(&status);
    TEST_STATUS(status, U_ZERO_ERROR);
    u_setMemoryFunctions(NULL, myMemAlloc, myMemRealloc, myMemFree, &status);
    TEST_STATUS(status, U_INVALID_STATE_ERROR);

    /* Doing ICU operations should cause allocations to come through our test heap */
    gBlockCount = 0;
    status = U_ZERO_ERROR;
    rb = ures_open(NULL, "es", &status);
    TEST_STATUS(status, U_ZERO_ERROR);
    if (gBlockCount == 0) {
        log_err("Heap functions are not being called from ICU.\n");
    }
    ures_close(rb);

    /* Cleanup should put the heap back to its default implementation. */
    u_cleanup();
    status = U_ZERO_ERROR;
    u_init(&status);
    TEST_STATUS(status, U_ZERO_ERROR);

    /* ICU operations should no longer cause allocations to come through our test heap */
    gBlockCount = 0;
    status = U_ZERO_ERROR;
    rb = ures_open(NULL, "fr", &status);
    TEST_STATUS(status, U_ZERO_ERROR);
    if (gBlockCount != 0) {
        log_err("Heap functions did not reset after u_cleanup.\n");
    }
    ures_close(rb);
}


/*
 *  Test u_setMutexFunctions()
 */

int         gTotalMutexesInitialized = 0;         /* Total number of mutexes created */
int         gTotalMutexesActive      = 0;         /* Total mutexes created, but not destroyed  */
int         gAccumulatedLocks        = 0;
const void *gMutexContext;

typedef struct DummyMutex {
    int  fLockCount;
    int  fMagic;
} DummyMutex;


void myMutexInit(const void *context, UMTX *mutex, UErrorCode *status) {
    DummyMutex *theMutex;

    TEST_STATUS(*status, U_ZERO_ERROR);
    theMutex = (DummyMutex *)malloc(sizeof(DummyMutex));
    theMutex->fLockCount = 0;
    theMutex->fMagic     = 123456;
    gTotalMutexesInitialized++;
    gTotalMutexesActive++;
    gMutexContext = context;
    *mutex = theMutex;
}


void myMutexDestroy(const void *context, UMTX  *mutex) {
    DummyMutex *This = *(DummyMutex **)mutex;

    gTotalMutexesActive--;
    TEST_ASSERT(This->fLockCount == 0);
    TEST_ASSERT(This->fMagic == 123456);
    This->fMagic = 0;
    This->fLockCount = 0;
    free(This);
}

void  myMutexLock(const void *context, UMTX *mutex) {
    DummyMutex *This = *(DummyMutex **)mutex;

    TEST_ASSERT(This->fMagic == 123456);
    This->fLockCount++;
    gAccumulatedLocks++;
}

void  myMutexUnlock(const void *context, UMTX *mutex) {
    DummyMutex *This = *(DummyMutex **)mutex;

    TEST_ASSERT(This->fMagic == 123456);
    This->fLockCount--;
    TEST_ASSERT(This->fLockCount >= 0);
}



static void TestMutexFunctions() {
    UErrorCode       status = U_ZERO_ERROR;
    UResourceBundle *rb     = NULL;


    /* Can not set mutex functions if ICU is already initialized */
    u_setMutexFunctions(&gContext, myMutexInit, myMutexDestroy, myMutexLock, myMutexUnlock, &status);
    TEST_STATUS(status, U_INVALID_STATE_ERROR);

    /* Un-initialize ICU */
    u_cleanup();

    /* Can not set Mutex functions with NULL values */
    status = U_ZERO_ERROR;
    u_setMutexFunctions(&gContext, NULL, myMutexDestroy, myMutexLock, myMutexUnlock, &status);
    TEST_STATUS(status, U_ILLEGAL_ARGUMENT_ERROR);
    status = U_ZERO_ERROR;
    u_setMutexFunctions(&gContext, myMutexInit, NULL, myMutexLock, myMutexUnlock, &status);
    TEST_STATUS(status, U_ILLEGAL_ARGUMENT_ERROR);
    status = U_ZERO_ERROR;
    u_setMutexFunctions(&gContext, myMutexInit, myMutexDestroy, NULL, myMutexUnlock, &status);
    TEST_STATUS(status, U_ILLEGAL_ARGUMENT_ERROR);
    status = U_ZERO_ERROR;
    u_setMutexFunctions(&gContext, myMutexInit, myMutexDestroy, myMutexLock, NULL, &status);
    TEST_STATUS(status, U_ILLEGAL_ARGUMENT_ERROR);

    /* u_setMutexFunctions() should work with null or non-null context pointer */
    status = U_ZERO_ERROR;
    u_setMutexFunctions(&gContext, myMutexInit, myMutexDestroy, myMutexLock, myMutexUnlock, &status);
    TEST_STATUS(status, U_ZERO_ERROR);
    u_setMutexFunctions(&gContext, myMutexInit, myMutexDestroy, myMutexLock, myMutexUnlock, &status);
    TEST_STATUS(status, U_ZERO_ERROR);


    /* After reinitializing ICU, we should not be able to set the mutex funcs again. */
    status = U_ZERO_ERROR;
    u_init(&status);
    TEST_STATUS(status, U_ZERO_ERROR);
    u_setMutexFunctions(&gContext, myMutexInit, myMutexDestroy, myMutexLock, myMutexUnlock, &status);
    TEST_STATUS(status, U_INVALID_STATE_ERROR);

    /* Doing ICU operations should cause allocations to come through our test mutexes */
    gBlockCount = 0;
    status = U_ZERO_ERROR;
    rb = ures_open(NULL, "es", &status);
    TEST_STATUS(status, U_ZERO_ERROR);
    TEST_ASSERT(gTotalMutexesInitialized > 0);
    TEST_ASSERT(gTotalMutexesActive > 0);

    ures_close(rb);

    /* Cleanup should destroy all of the mutexes. */
    u_cleanup();
    status = U_ZERO_ERROR;
    TEST_ASSERT(gTotalMutexesInitialized > 0);
    TEST_ASSERT(gTotalMutexesActive == 0);


    /* Additional ICU operations should no longer use our dummy test mutexes */
    gTotalMutexesInitialized = 0;
    gTotalMutexesActive      = 0;
    u_init(&status);
    TEST_STATUS(status, U_ZERO_ERROR);

    status = U_ZERO_ERROR;
    rb = ures_open(NULL, "fr", &status);
    TEST_STATUS(status, U_ZERO_ERROR);
    TEST_ASSERT(gTotalMutexesInitialized == 0);
    TEST_ASSERT(gTotalMutexesActive == 0);

    ures_close(rb);
}

