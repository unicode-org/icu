/*
*******************************************************************************
*
*   Copyright (C) 2009-2011, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*/

#ifndef _OICU
#define _OICU

/**
   uclean.h
*/
U_STABLE void U_EXPORT2
OICU_u_init(UErrorCode *status);

/**
 ucol.h
*/
U_STABLE UCollator* U_EXPORT2 
OICU_ucol_open(const char *loc, UErrorCode& status);

U_STABLE int32_t U_EXPORT2
OICU_ucol_getShortDefinitionString(const UCollator *coll,
                              const char *locale,
                              char *buffer,
                              int32_t capacity,
                              UErrorCode *status);


U_STABLE void U_EXPORT2 
OICU_ucol_close(UCollator*);

U_STABLE UCollationResult OICU_ucol_strcoll	(	const UCollator * 	coll,
const UChar * 	source,
int32_t 	sourceLength,
const UChar * 	target,
int32_t 	targetLength	 
);

U_STABLE int32_t U_EXPORT2 
OICU_ucol_countAvailable();

U_STABLE void U_EXPORT2 
OICU_ucol_setStrength(const UCollator *, UCollationStrength );


#ifndef OICU_ucol_getAvailable
#error OICU_ucol_getAvailable not found - urename symbol mismatch?
#endif

U_STABLE const char * U_EXPORT2 
OICU_ucol_getAvailable(int32_t i);

U_STABLE UCollationStrength U_EXPORT2 
OICU_ucol_getStrength(UCollator *col);

U_STABLE int32_t U_EXPORT2 
OICU_ucol_getSortKey(const    UCollator    *coll,
        const    UChar        *source,
        int32_t        sourceLength,
        uint8_t        *result,
        int32_t        resultLength);


U_STABLE UCollator* U_EXPORT2 
OICU_ucol_safeClone(const UCollator *coll,
               void            *stackBuffer,
               int32_t         *pBufferSize,
               UErrorCode      *status);


/**
 end ucol.h
*/

// define version
GLUE_VER( ICUGLUE_VER )
#endif
