/*
*******************************************************************************
* Copyright (C) 1996-1999, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/
//  FILE NAME : unicode.cpp
//
//  CREATED
//      Wednesday, December 11, 1996
//  
//  CHANGES
//      Wednesday, February 4,  1998
//      Changed logic in toUpperCase and toLowerCase in order
//      to avoid 0xFFFF to be returned when receiving 
//      confusing Unichar  to lowercase or to uppercase
//      (e.g. Letterlike symbols)
//
//  CHANGES BY
//  Bertramd A. DAMIBA
//
//  CREATED BY
//      Helena Shih
//
//  CHANGES
//      Thursday, April 15, 1999
//      Modified the definitions of all the functions
//      C++ Wrappers for Unicode
//  CHANGES BY
//      Madhu Katragadda
//   5/20/99     Madhu		Added the function u_getVersion()
//  07/09/99     stephen        Added definition for {MIN,MAX}_VALUE
//  11/22/99     aliu       Added MIN_RADIX, MAX_RADIX, digit, forDigit
//********************************************************************************************

#include "unicode/utypes.h"
#include "unicode/uchar.h"
#include "unicode/unicode.h"

/*
 * Private (sort of) constructors etc., defined only to prevent
 * instantiation and subclassing. Therefore, empty.
 */
Unicode::Unicode() {}
Unicode::Unicode(const Unicode &) {}
Unicode::~Unicode() {}
const Unicode &
Unicode::operator=(const Unicode &) {
    return *this;
}
