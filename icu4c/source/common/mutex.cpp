/*
 **********************************************************************
 *   Copyright (C) 1997-2001, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 **********************************************************************
*/

#include "unicode/utypes.h"
#include "unicode/uobject.h"
#include "mutex.h"
#include "umutex.h"

/* Initialize the global mutex only when we can use it. */
#if (ICU_USE_THREADS == 1)

static int GlobalMutexInitialize()
{
  umtx_init( NULL );
  return 0;
}

static int initializesGlobalMutex = GlobalMutexInitialize();


U_NAMESPACE_BEGIN

const char Mutex::fgClassID=0;

U_NAMESPACE_END

#endif /* ICU_USE_THREADS==1 */
