/*
*******************************************************************************
*
*   Copyright (C) 2002, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  uenumimp.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:2
*
*   created on: 2002jul08
*   created by: Vladimir Weinstein
*/

#ifndef __UENUMIMP_H
#define __UENUMIMP_H

#include "unicode/uenum.h"

/**
 * Function type declaration for uenum_close().
 *
 * This function should cleanup the enumerator object
 *
 * @param en enumeration to be closed
 */
typedef void U_CALLCONV
UEnumClose(UEnumeration *en);

/**
 * Function type declaration for uenum_count().
 *
 * This function should count the number of elements
 * in this enumeration
 *
 * @param en enumeration to be counted
 * @param status pointer to UErrorCode variable
 * @return number of elements in enumeration
 */
typedef int32_t U_CALLCONV
UEnumCount(UEnumeration *en, UErrorCode *status);

/**
 * Function type declaration for uenum_unext().
 *
 * This function should return the next element
 * as a UChar *
 *
 * @param en enumeration 
 * @param resultLength pointer to result length
 * @param status pointer to UErrorCode variable
 * @return next element as UChar *
 */
typedef const UChar* U_CALLCONV 
UEnumUNext(UEnumeration* en,
            int32_t* resultLength,
            UErrorCode* status);

/**
 * Function type declaration for uenum_next().
 *
 * This function should return the next element
 * as a char *
 *
 * @param en enumeration 
 * @param resultLength pointer to result length
 * @param status pointer to UErrorCode variable
 * @return next element as char *
 */
typedef const char* U_CALLCONV 
UEnumNext(UEnumeration* en,
           int32_t* resultLength,
           UErrorCode* status);

/**
 * Function type declaration for uenum_reset().
 *
 * This function should reset the enumeration 
 * object
 *
 * @param en enumeration 
 * @param status pointer to UErrorCode variable
 */
typedef void U_CALLCONV 
UEnumReset(UEnumeration* en, 
            UErrorCode* status);


struct UEnumeration {
  UChar *currentUChar;
  char  *currentChar;

  void *context1;
  void *context2;

  int32_t int1;
  int32_t int2;

  /* these are functions that will 
   * be used for APIs
   */
  /* called from uenum_close */
  UEnumClose *close;
  /* called from uenum_count */
  UEnumCount *count;
  /* called from uenum_unext */
  UEnumUNext *uNext;
  /* called from uenum_next */
  UEnumNext  *next;
  /* called from uenum_reset */
  UEnumReset *reset;
};

#endif
