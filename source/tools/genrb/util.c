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
#if defined(WIN32) ||  defined(_WIN32) || defined(OS2) || defined(__OS2__)
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
  const char *lastSlash = icu_strrchr(filename, DIR_SEP) + 1;

  if(lastSlash>filename) {
    icu_strncpy(dirname, filename, (lastSlash - filename));
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
  const char *lastSlash = icu_strrchr(filename, DIR_SEP) + 1;
  char *lastDot;

  if(lastSlash>filename) {
    icu_strcpy(basename, lastSlash);
  } else {
    icu_strcpy(basename, filename);
  }

  /* strip off any suffix */
  lastDot = icu_strrchr(basename, '.');

  if(lastDot != NULL) {
    *lastDot = '\0';
  }
}
