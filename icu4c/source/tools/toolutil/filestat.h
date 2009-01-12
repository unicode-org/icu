/*
*******************************************************************************
*
*   Copyright (C) 2009, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  flagparser.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2009jan09
*   created by: Michael Ow
*
* Compares modification times on specified files using ICU and intended for use in ICU tests and in build tools.
* Not suitable for production use. Not supported.
* Not conformant. Not efficient.
* But very small.
*/

#ifndef __FILESTAT_H__
#define __FILESTAT_H__

#include "unicode/utypes.h"

U_CAPI UBool U_EXPORT2
isFileModTimeLater(const char *filePath, const char *checkAgainst, UBool isDir=FALSE);

#endif
