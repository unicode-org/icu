/*
*******************************************************************************
*
*   Copyright (C) 1999-2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  toolutil.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 1999nov19
*   created by: Markus W. Scherer
*
*   This file defines utility functions for ICU tools like genccode.
*/

#ifndef __TOOLUTIL_H__
#define __TOOLUTIL_H__

#include "unicode/utypes.h"



/*
 * For Windows, a path/filename may be the short (8.3) version
 * of the "real", long one. In this case, the short one
 * is abbreviated and contains a tilde etc.
 * This function returns a pointer to the original pathname
 * if it is the "real" one itself, and a pointer to a static
 * buffer (not thread-safe) containing the long version
 * if the pathname is indeed abbreviated.
 *
 * On platforms other than Windows, this function always returns
 * the input pathname pointer.
 *
 * This function is especially useful in tools that are called
 * by a batch file for loop, which yields short pathnames on Win9x.
 */
U_CAPI const char * U_EXPORT2
getLongPathname(const char *pathname);

/*
 * Find the basename at the end of a pathname, i.e., the part
 * after the last file separator, and return a pointer
 * to this part of the pathname.
 * If the pathname only contains a basename and no file separator,
 * then the pathname pointer itself is returned.
 */
U_CAPI const char * U_EXPORT2
findBasename(const char *filename);

#endif
