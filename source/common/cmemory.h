/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright Taligent, Inc.,  1997                                       *
*   (C) Copyright International Business Machines Corporation,  1997-1998     *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
*******************************************************************************
*
* File CMEMORY.H
*
*  Contains stdlib.h/string.h memory functions
*
* @author       Bertrand A. Damiba
*
* Modification History:
*
*   Date        Name        Description
*   6/20/98     Bertrand    Created.
*  05/03/99     stephen     Changed from functions to macros.
*
*******************************************************************************
*/

#ifndef CMEMORY_H
#define CMEMORY_H

#include <stdlib.h>
#include <string.h>

#define icu_malloc(size) malloc(size)
#define icu_realloc(buffer, size) realloc(buffer, size)
#define icu_free(buffer) free(buffer)
#define icu_memcpy(dst, src, size) memcpy(dst, src, size)
#define icu_memmove(dst, src, size) memmove(dst, src, size)
#define icu_memset(buffer, mark, size) memset(buffer, mark, size)
#define icu_memcmp(buffer1, buffer2, size) memcmp(buffer1, buffer2,size)

#endif
