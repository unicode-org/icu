/*
*******************************************************************************
*
*   Copyright (C) 1997-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
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

#define uprv_malloc(size) malloc(size)
#define uprv_realloc(buffer, size) realloc(buffer, size)
#define uprv_free(buffer) free(buffer)
#define uprv_memcpy(dst, src, size) memcpy(dst, src, size)
#define uprv_memmove(dst, src, size) memmove(dst, src, size)
#define uprv_memset(buffer, mark, size) memset(buffer, mark, size)
#define uprv_memcmp(buffer1, buffer2, size) memcmp(buffer1, buffer2,size)

#endif
