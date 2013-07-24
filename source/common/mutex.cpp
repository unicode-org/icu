/*
*******************************************************************************
*
*   Copyright (C) 2008-2013, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  mutex.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*/

#include "unicode/utypes.h"
#include "mutex.h"
#include "uassert.h"

U_NAMESPACE_BEGIN

void *SimpleSingleton::getInstance(InstantiatorFn *instantiator, const void *context,
                                   UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) {
        return NULL;
    }
    if (umtx_loadAcquire(fInitOnce.fState) == 2) {
        return fInstance;
    }
    if (umtx_initImplPreInit(fInitOnce)) {
        fInstance = instantiator(context, errorCode);
        umtx_initImplPostInit(fInitOnce, fInstance != NULL);
    }
    return fInstance;
}


/*
 * Three states:
 *
 * Initial state: Instance creation not attempted yet.
 * fInstance=NULL && U_SUCCESS(fErrorCode)
 *
 * Instance creation run & succeeded:
 * fInstance!=NULL && U_SUCCESS(fErrorCode)
 *
 * Instance creation & failed:
 * fInstance=NULL && U_FAILURE(fErrorCode)
 * We will not attempt again to create the instance.
 *
 * The instantiator function will be called only once, whether it succeeds or fails.
 * The controlling state is maintained by the UInitOnce object, not by
 *    fInstance and fErrorCode.
 * The values of fInstance and fErrorCode must only be set between pre and post init(),
 *    where they are in a controlled memory environment.
 */
void *TriStateSingleton::getInstance(InstantiatorFn *instantiator, const void *context,
                                     UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) {
        return NULL;
    }
    if (umtx_loadAcquire(fInitOnce.fState) == 2) {
        errorCode = fErrorCode;
        return fInstance;
    }
    if (umtx_initImplPreInit(fInitOnce)) {
        errorCode = U_ZERO_ERROR;
        fInstance = instantiator(context, errorCode);
        fErrorCode = errorCode;
        umtx_initImplPostInit(fInitOnce, TRUE);
    }
    return fInstance;
}


void TriStateSingleton::reset() {
    fInstance=NULL;
    fErrorCode=U_ZERO_ERROR;
    fInitOnce.reset();
}

#if UCONFIG_NO_SERVICE

/* If UCONFIG_NO_SERVICE, then there is no invocation of Mutex elsewhere in
   common, so add one here to force an export */
static Mutex *aMutex = 0;

/* UCONFIG_NO_SERVICE */
#endif

U_NAMESPACE_END
