/*
*******************************************************************************
*
*   Copyright (C) 1998-2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File read.h
*
* Modification History:
*
*   Date        Name        Description
*   05/26/99    stephen     Creation.
*   5/10/01     Ram         removed ustdio dependency
*******************************************************************************
*/

#ifndef PRSCMNTS_H
#define PRSCMNTS_H 1

U_CFUNC int32_t 
getNote(const UChar* source, int32_t srcLen,
        UChar** dest, int32_t destCapacity,
        UErrorCode* status);
U_CFUNC int32_t 
removeCmtText(UChar* source, int32_t srcLen, UErrorCode* status);

U_CFUNC int32_t
getDescription( const UChar* source, int32_t srcLen,
                UChar** dest, int32_t destCapacity,
                UErrorCode* status);
U_CFUNC int32_t
getTranslate( const UChar* source, int32_t srcLen,
              UChar** dest, int32_t destCapacity,
              UErrorCode* status);

#endif

