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

#include "cstring.h"
#include "error.h"

/* This is incredibly non thread-safe, but it doesn't matter for this util */
static char gErrorText[200] = { "" };

void 
setErrorText(const char *s)
{
  uprv_strcpy(gErrorText, s);
}

const char* 
getErrorText()
{
  return gErrorText[0] != 0 ? gErrorText : NULL;
}
