/*
 **********************************************************************
 *   Copyright (C) 1997-2001, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 **********************************************************************
*/

#include "umutex.h"

/* Initialize the global mutex only when we can use it. */
#if (ICU_USE_THREADS == 1)

static int GlobalMutexInitialize()
{
  umtx_init( NULL );
  return 0;
}

static int initializesGlobalMutex = GlobalMutexInitialize();

#endif /* ICU_USE_THREADS==1 */
