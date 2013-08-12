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

U_NAMESPACE_BEGIN

#if UCONFIG_NO_SERVICE

/* If UCONFIG_NO_SERVICE, then there is no invocation of Mutex elsewhere in
   common, so add one here to force an export */
static Mutex *aMutex = 0;

/* UCONFIG_NO_SERVICE */
#endif

U_NAMESPACE_END
