/*
*******************************************************************************
*
*   Copyright (C) 1998-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
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
