/*
**********************************************************************
*   Copyright (C) 1999, International Business Machines
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

#ifndef UHASH_H
typedef struct _UHashtable UHashtable;
#endif

/*Hashtable used to store UConverterSharedData objects supporting
 *the Caching mechanism
 */
extern UHashtable *SHARED_DATA_HASHTABLE;


/* figures out if we need to go to file to read in the data tables.
 */
UConverter *createConverter (const char *converterName, UErrorCode * err);

/* Stores the shared data in the SHARED_DATA_HASHTABLE
 */
void shareConverterData (UConverterSharedData * data);

/* gets the shared data from the SHARED_DATA_HASHTABLE (might return NULL if it isn't there)
 */
UConverterSharedData *getSharedConverterData (const char *name);

/* Deletes (frees) the Shared data it's passed. first it checks the referenceCounter to
 * see if anyone is using it, if not it frees all the memory stemming from sharedConverterData and
 * returns TRUE,
 * otherwise returns FALSE
 */
bool_t deleteSharedConverterData (UConverterSharedData * sharedConverterData);

/* returns true if "name" is in algorithmicConverterNames
 */
bool_t isDataBasedConverter (const char *name);

void copyPlatformString (char *platformString, UConverterPlatform pltfrm);


#endif /* _UCNV_IMP */
