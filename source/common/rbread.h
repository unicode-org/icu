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
* File rbread.h
*
* Modification History:
*
*   Date        Name        Description
*   06/14/99    stephen     Creation.
*******************************************************************************
*/

#ifndef RBREAD_H
#define RBREAD_H 1

#include "utypes.h"
#include "filestrm.h"
#include "uhash.h"
#include "unistr.h"

/* Byte order mark for compiled resource bundle files */
static const int32_t sBOM          = 0x021C;

/* Type of resource indicators */
static const int32_t sSTRINGLIST   = 1;
static const int32_t sSTRINGLIST2D = 2;
static const int32_t sTAGGEDLIST   = 3;

/* EOF indicator */
static const int32_t sEOF          = -1;

/* Parse a compiled resource bundle into a hashtable and locale name */
UHashtable* rb_parse(FileStream *f, 
		     UnicodeString& localename, 
		     UErrorCode& status);

#endif /* ! RBREAD_H */
