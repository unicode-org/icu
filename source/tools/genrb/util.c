/*
*******************************************************************************
*
*   Copyright (C) 1998-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File util.c
*
* Modification History:
*
*   Date        Name        Description
*   06/10/99    stephen     Creation.
*******************************************************************************
*/

#include "util.h"
#include "cmemory.h"
#include "cstring.h"
#include "unicode/putil.h"


/* go from "/usr/local/include/curses.h" to "/usr/local/include" */
void
get_dirname(char *dirname,
	    const char *filename)
{
  const char *lastSlash = uprv_strrchr(filename, U_FILE_SEP_CHAR) + 1;

  if(lastSlash>filename) {
    uprv_strncpy(dirname, filename, (lastSlash - filename));
    *(dirname + (lastSlash - filename)) = '\0';
  } else {
    *dirname = '\0';
  }
}

/* go from "/usr/local/include/curses.h" to "curses" */
void
get_basename(char *basename,
	     const char *filename)
{
  /* strip off any leading directory portions */
  const char *lastSlash = uprv_strrchr(filename, U_FILE_SEP_CHAR) + 1;
  char *lastDot;

  if(lastSlash>filename) {
    uprv_strcpy(basename, lastSlash);
  } else {
    uprv_strcpy(basename, filename);
  }

  /* strip off any suffix */
  lastDot = uprv_strrchr(basename, '.');

  if(lastDot != NULL) {
    *lastDot = '\0';
  }
}
