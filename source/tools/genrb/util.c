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


/* Platform-specific directory separator */
#ifdef WIN32
# define DIR_SEP '\\'
# define CUR_DIR ".\\"
#else
# define DIR_SEP '/'
# define CUR_DIR "./"
#endif /* WIN32 */

/* go from "/usr/local/include/curses.h" to "/usr/local/include" */
void
get_dirname(char *dirname,
	    const char *filename)
{
  const char *slash;
  const char *lastSlash;

  /* if the first character isn't DIR_SEP or '.', return CUR_DIR */
  if(*filename != DIR_SEP && *filename != '.') {
    icu_strcpy(dirname, CUR_DIR);
    return;
  }

  /* strip off and copy any leading directory portions */
  slash = lastSlash = filename;
  for(;;) {
    slash = icu_strchr(slash, DIR_SEP);
    if(slash == 0) {
      slash = lastSlash;
      break;
    }
    ++slash;
    lastSlash = slash;
  }
  
  *dirname = '\0';
  icu_strncat(dirname, filename, (lastSlash - filename));
}

/* go from "/usr/local/include/curses.h" to "curses" */
void
get_basename(char *basename,
	     const char *filename)
{
  const char *slash;
  const char *lastSlash;
  char *dot;
  char *lastDot;

  /* strip off any leading directory portions */
  slash = lastSlash = filename;
  for(;;) {
    slash = icu_strchr(slash, DIR_SEP);
    if(slash == 0) {
      slash = lastSlash;
      break;
    }
    ++slash;
    lastSlash = slash;
  }
  
  icu_strcpy(basename, slash);

  /* strip off any suffix */
  dot = basename;
  lastDot = 0;
  for(;;) {
    dot = icu_strchr(dot, '.');
    if(dot == 0) {
      if(lastDot != 0)
	*lastDot = '\0';
      break;
    }
    lastDot = dot;
    ++dot;
  }
}
