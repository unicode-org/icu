/*
**********************************************************************
*   Copyright (C) 1999-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*
*
*  ucnv_imp.h:
*  Contains all internal and external data structure definitions
* Created & Maitained by Bertrand A. Damiba
*
*
*
* ATTENTION:
* ---------
* Although the data structures in this file are open and stack allocatable
* we reserve the right to hide them in further releases.
*/

#ifndef UCNV_IMP_H
#define UCNV_IMP_H

#include "unicode/utypes.h"
#include "ucnv_bld.h"

/* figures out if we need to go to file to read in the data tables.
 * @param converterName The name of the converter
 * @param err The error code
 * @return the newly created converter
 */
UConverter *ucnv_createConverter (UConverter *myUConverter, const char *converterName, UErrorCode * err);

/*
 * Open a purely algorithmic converter, specified by a type constant.
 * @param myUConverter  NULL, or pre-allocated UConverter structure to avoid
 *                      a memory allocation
 * @param type          requested converter type
 * @param locale        locale parameter, or ""
 * @param options       converter options bit set (default 0)
 * @param err           ICU error code, not tested for U_FAILURE on input
 *                      because this is an internal function
 * @internal
 */
U_CFUNC UConverter *
ucnv_createAlgorithmicConverter(UConverter *myUConverter,
                                UConverterType type,
                                const char *locale, uint32_t options,
                                UErrorCode *err);

/* Creates a converter from shared data 
 */
UConverter*
ucnv_createConverterFromSharedData(UConverter *myUConverter, UConverterSharedData *mySharedConverterData, const char *realName, const char *locale, uint32_t options, UErrorCode *err);

UConverter* ucnv_createConverterFromPackage(const char *packageName, const char *converterName,  
                                            UErrorCode *err);

/* Stores the shared data in the SHARED_DATA_HASHTABLE
 * @param data The shared data
 */
void ucnv_shareConverterData (UConverterSharedData * data);

/* gets the shared data from the SHARED_DATA_HASHTABLE (might return NULL if it isn't there)
 * @param name The name of the shared data
 * @return the shared data from the SHARED_DATA_HASHTABLE
 */
UConverterSharedData *ucnv_getSharedConverterData (const char *name);

/* Deletes (frees) the Shared data it's passed. first it checks the referenceCounter to
 * see if anyone is using it, if not it frees all the memory stemming from sharedConverterData and
 * returns TRUE,
 * otherwise returns FALSE
 * @param sharedConverterData The shared data
 * @return if not it frees all the memory stemming from sharedConverterData and
 * returns TRUE, otherwise returns FALSE
 */
UBool ucnv_deleteSharedConverterData (UConverterSharedData * sharedConverterData);

/* returns true if "name" is in algorithmicConverterNames
 * @param name The converter name.
 * @return TRUE  if "name" is in algorithmicConverterNames.
 */
UBool ucnv_isDataBasedConverter (const char *name);

/* Copy the string that is represented by the UConverterPlatform enum
 * @param platformString An output buffer
 * @param platform An enum representing a platform
 * @return the length of the copied string.
 */
int32_t ucnv_copyPlatformString(char *platformString, UConverterPlatform platform);


#endif /* _UCNV_IMP */
