/*
*******************************************************************************
*
*   Copyright (C) 1998-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File parse.h
*
* Modification History:
*
*   Date        Name        Description
*   05/26/99    stephen     Creation.
*******************************************************************************
*/

#ifndef PARSE_H
#define PARSE_H 1

#include "utypes.h"
#include "filestrm.h"
#include "rblist.h"

/* Parse a ResourceBundle text file */
struct SRBItemList* parse(FileStream *f, const char *cp, UErrorCode *status);

#endif
