/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright International Business Machines Corporation, 1998, 1999     *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
*******************************************************************************
*
* File error.c
*
* Modification History:
*
*   Date        Name        Description
*   05/28/99    stephen     Creation.
*******************************************************************************
*/

#include "error.h"

/* This is incredibly non thread-safe, but it doesn't matter for this util */
static const char *gErrorText = 0;

void 
setErrorText(const char *s)
{ 
  gErrorText = s; 
}

const char* 
getErrorText()
{
  return gErrorText;
}
