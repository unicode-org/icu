/*
*****************************************************************************************
*                                                                                       *
* COPYRIGHT:                                                                            *
*   (C) Copyright Taligent, Inc.,  1997                                                 *
*   (C) Copyright International Business Machines Corporation,  1997-1998               *
*   Licensed Material - Program-Property of IBM - All Rights Reserved                   *
*   US Government Users Restricted Rights - Use, duplication, or disclosure             *
*   restricted by GSA ADP Schedule Contract with IBM Corp                               *
*                                                                                       *
*****************************************************************************************
*/

#include "umutex.h"

int GlobalMutexInitialize()
{
  umtx_init( NULL );
  return 0;
}
static int initializesGlobalMutex = GlobalMutexInitialize();



