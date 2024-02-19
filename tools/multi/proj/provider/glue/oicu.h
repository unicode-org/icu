// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*******************************************************************************
*
*   Copyright (C) 2009-2012, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*/

#ifndef _OICU
#define _OICU

#include "unicode/uclean.h"
/**
   uclean.h
*/
U_CAPI void U_EXPORT2
OICU_u_init(UErrorCode *status);

#include "unicode/ucol.h"
/**
 ucol.h
*/
U_CAPI UCollator* U_EXPORT2 
OICU_ucol_open(const char *loc, UErrorCode& status);

U_CAPI int32_t U_EXPORT2
OICU_ucol_getShortDefinitionString(const UCollator *coll,
                              const char *locale,
                              char *buffer,
                              int32_t capacity,
                              UErrorCode *status);


U_CAPI void U_EXPORT2 
OICU_ucol_close(UCollator*);

U_CAPI UCollationResult OICU_ucol_strcoll	(	const UCollator * 	coll,
const UChar * 	source,
int32_t 	sourceLength,
const UChar * 	target,
int32_t 	targetLength	 
);

U_CAPI void U_EXPORT2 
OICU_ucol_setStrength(const UCollator *, UCollationStrength );


#ifndef OICU_ucol_getAvailable
#error OICU_ucol_getAvailable not found - urename symbol mismatch?
#endif

U_CAPI const char * U_EXPORT2 
OICU_ucol_getAvailable(int32_t i);

U_CAPI int32_t U_EXPORT2 
OICU_ucol_countAvailable();


U_CAPI UCollationStrength U_EXPORT2 
OICU_ucol_getStrength(UCollator *col);

U_CAPI int32_t U_EXPORT2 
OICU_ucol_getSortKey(const    UCollator    *coll,
        const    UChar        *source,
        int32_t        sourceLength,
        uint8_t        *result,
        int32_t        resultLength);


U_CAPI UCollator* U_EXPORT2 
OICU_ucol_safeClone(const UCollator *coll,
               void            *stackBuffer,
               int32_t         *pBufferSize,
               UErrorCode      *status);

#include "unicode/udat.h"
U_CAPI UDateFormat* U_EXPORT2 
OICU_udat_open(UDateFormatStyle  timeStyle,
          UDateFormatStyle  dateStyle,
          const char        *locale,
          const UChar       *tzID,
          int32_t           tzIDLength,
          const UChar       *pattern,
          int32_t           patternLength,
          UErrorCode        *status);

U_CAPI const char * U_EXPORT2 
OICU_udat_getAvailable(int32_t i);

U_CAPI int32_t U_EXPORT2 
OICU_udat_countAvailable();


U_CAPI void U_EXPORT2 
OICU_udat_close(UDateFormat* format);

U_CAPI int32_t U_EXPORT2 
OICU_udat_format(    const    UDateFormat*    format,
                        UDate           dateToFormat,
                        UChar*          result,
                        int32_t         resultLength,
                        UFieldPosition* position,
                        UErrorCode*     status);



/**
 end ucol.h
*/

#include "unicode/ucal.h"

U_CAPI UCalendar* U_EXPORT2 
OICU_ucal_open(const UChar*   zoneID,
          int32_t        len,
          const char*    locale,
          UCalendarType  type,
          UErrorCode*    status);

/**
 * Close a UCalendar.
 * Once closed, a UCalendar may no longer be used.
 * @param cal The UCalendar to close.
 * @stable ICU 2.0
 */
U_CAPI void U_EXPORT2 
OICU_ucal_close(UCalendar *cal);


U_CAPI int32_t U_EXPORT2 
OICU_ucal_getAttribute(const UCalendar*    cal,
                  UCalendarAttribute  attr);


// define version

#endif
