/*
 **********************************************************************
 *   Copyright (C) 1997-1999, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 **********************************************************************
*/

#include "umutex.h"

int GlobalMutexInitialize()
{
  umtx_init( NULL );
  return 0;
}
static int initializesGlobalMutex = GlobalMutexInitialize();



