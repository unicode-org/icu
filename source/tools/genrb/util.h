/*
*******************************************************************************
*
*   Copyright (C) 1998-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File util.h
*
* Modification History:
*
*   Date        Name        Description
*   06/10/99    stephen     Creation.
*******************************************************************************
*/

#ifndef UTIL_H
#define UTIL_H 1

U_CDECL_BEGIN

void get_dirname(char *dirname, const char *filename);
void get_basename(char *basename, const char *filename);

U_CDECL_END
#endif /* ! UTIL_H */

