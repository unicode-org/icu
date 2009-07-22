/**
*******************************************************************************
* Copyright (C) 2008, International Business Machines Corporation.       *
* All Rights Reserved.                                                        *
*******************************************************************************
*/

#include "unicode/utypes.h"

#if UCONFIG_NO_SERVICE

/* If UCONFIG_NO_SERVICE, then there is no invocation of Mutex elsewhere in
   common, so add one here to force an export */
#include "mutex.h"
static Mutex *aMutex = 0;

/* UCONFIG_NO_SERVICE */
#endif
