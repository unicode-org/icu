/*
******************************************************************************
*
*   Copyright (C) 1999-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*   file name:  udata.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 1999oct25
*   created by: Markus W. Scherer
*/

#include "unicode/utypes.h"
#include "unicode/putil.h"
#include "umutex.h"
#include "cmemory.h"
#include "cstring.h"
#include "unicode/udata.h"
#include "unicode/uversion.h"
#include "uhash.h"
#include "ucln_cmn.h"

#include "udatamem.h"
#include "umapfile.h"
#include "ucmndata.h"

/***********************************************************************
*
*   Notes on the organization of the ICU data implementation
*
*      All of the public API is defined in udata.h
*
*      The implementation is split into several files...
*
*         - udata.c  (this file) contains higher level code that knows about
*                     the search paths for locating data, caching opened data, etc.
*
*         - umapfile.c  contains the low level platform-specific code for actually loading
*                     (memory mapping, file reading, whatever) data into memory.
*
*         - ucmndata.c  deals with the tables of contents of ICU data items within
*                     an ICU common format data file.  The implementation includes
*                     an abstract interface and support for multiple TOC formats.
*                     All knowledge of any specific TOC format is encapsulated here.
*
*         - udatamem.c has code for managing UDataMemory structs.  These are little
*                     descriptor objects for blocks of memory holding ICU data of
*                     various types.
*/

/* configuration ---------------------------------------------------------- */

/* If you are excruciatingly bored turn this on .. */
/* #define UDATA_DEBUG 1 */

#if defined(UDATA_DEBUG)
#   include <stdio.h>
#endif




/***********************************************************************
*
*    static (Global) data
*
************************************************************************/
static UDataMemory *gCommonICUData = NULL;    /* Pointer to the common ICU data.           */
                                              /*   May be updated once, if we started with */
                                              /*   a stub or subset library.               */

static UDataMemory *gStubICUData   = NULL;    /* If gCommonICUData does get updated, remember */
                                              /*   the original one so that it can be cleaned */
                                              /*   up when ICU is shut down.                  */

static UHashtable  *gCommonDataCache = NULL;  /* Global hash table of opened ICU data files.  */


UBool
udata_cleanup()
{
    if (gCommonDataCache) {              /* Delete the cache of user data mappings.  */
        uhash_close(gCommonDataCache);   /*   Table owns the contents, and will delete them. */
        gCommonDataCache = 0;            /*   Cleanup is not thread safe.                */
    }

    if (gCommonICUData != NULL) {
    udata_close(gCommonICUData);         /* Clean up common ICU Data             */
    gCommonICUData = NULL;
    }

    if (gStubICUData != NULL) {
        udata_close(gStubICUData);     /* Clean up the stub ICU Data             */
        gStubICUData = NULL;
    }


    return TRUE;                   /* Everything was cleaned up */
}




/*
 * setCommonICUData.   Set a UDataMemory to be the global ICU Data
 */
static void
setCommonICUData(UDataMemory *pData,     /*  The new common data.  Belongs to caller, we copy it. */
                 UDataMemory *oldData,   /*  Old ICUData ptr.  Overwrite of this value is ok,     */
                                         /*     of any others is not.                             */
                 UBool       warn,       /*  If true, set USING_DEFAULT warning if ICUData was    */
                                         /*    changed by another thread before we got to it.     */
                 UErrorCode *pErr)
{
    UDataMemory  *newCommonData = UDataMemory_createNewInstance(pErr);
    if (U_FAILURE(*pErr)) {
        return;
    }

    /*  For the assignment, other threads must cleanly see either the old            */
    /*    or the new, not some partially initialized new.  The old can not be        */
    /*    deleted - someone may still have a pointer to it lying around in           */
    /*    their locals.                                                              */
    UDatamemory_assign(newCommonData, pData);
    umtx_lock(NULL);
    if (gCommonICUData==oldData) {
        gStubICUData   = gCommonICUData;   /* remember the old Common Data, so it can be cleaned up. */
        gCommonICUData = newCommonData;
    }
    else {
        if  (warn==TRUE) {
            *pErr = U_USING_DEFAULT_WARNING;
        }
        uprv_free(newCommonData);
    }
    umtx_unlock(NULL);
    return;
}



static char *
strcpy_returnEnd(char *dest, const char *src) {
    while((*dest=*src)!=0) {
        ++dest;
        ++src;
    }
    return dest;
}

/*------------------------------------------------------------------------------*
 *                                                                              *
 *  setPathGetBasename   given a (possibly partial) path of an item             *
 *                       to be opened, compute a full directory path and leave  *
 *                       it in pathBuffer.  Returns a pointer to the null at    *
 *                       the end of the computed path.                          *
 *                       Overwrites any contents in the output pathBuffer       *
 *                                                                              *
 *------------------------------------------------------------------------------*/
static char *
setPathGetBasename(const char *path, char *pathBuffer) {
    if(path==NULL) {
        /* copy the ICU_DATA path to the path buffer */
        path=u_getDataDirectory();
        if(path!=NULL && *path!=0) {
            return strcpy_returnEnd(pathBuffer, path);
        } else {
            /* there is no path */
            return pathBuffer;
        }
    } else {
        /* find the last file sepator in the input path */
        char *basename=uprv_strrchr(path, U_FILE_SEP_CHAR);
        if(basename==NULL) {
            /* copy the ICU_DATA path to the path buffer */
            path=u_getDataDirectory();
            if(path!=NULL && *path!=0) {
                return strcpy_returnEnd(pathBuffer, path);
            } else {
                /* there is no path */
                return pathBuffer;
            }
        } else {
            /* copy the path to the path buffer */
            ++basename;
            uprv_memcpy(pathBuffer, path, basename-path);
            basename=pathBuffer+(basename-path);
            *basename=0;
            return basename;
        }
    }
}


static const char *
findBasename(const char *path) {
    const char *basename=uprv_strrchr(path, U_FILE_SEP_CHAR);
    if(basename==NULL) {
        return path;
    } else {
        return basename+1;
    }
}


/*----------------------------------------------------------------------*
 *                                                                      *
 *   Cache for common data                                              *
 *      Functions for looking up or adding entries to a cache of        *
 *      data that has been previously opened.  Avoids a potentially     *
 *      expensive operation of re-opening the data for subsequent       *
 *      uses.                                                           *
 *                                                                      *
 *      Data remains cached for the duration of the process.            *
 *                                                                      *
 *----------------------------------------------------------------------*/

typedef struct DataCacheElement {
    char          *name;
    UDataMemory   *item;
} DataCacheElement;



/*
 * Deleter function for DataCacheElements.
 *         udata cleanup function closes the hash table; hash table in turn calls back to
 *         here for each entry.
 */
static void  U_CALLCONV DataCacheElement_deleter(void *pDCEl) {
    DataCacheElement *p = (DataCacheElement *)pDCEl;
    udata_close(p->item);              /* unmaps storage */
    uprv_free(p->name);                /* delete the hash key string. */
    uprv_free(pDCEl);                  /* delete 'this'          */
}


 /*   udata_getCacheHashTable()
 *     Get the hash table used to store the data cache entries.
 *     Lazy create it if it doesn't yet exist.
 */
static UHashtable *udata_getHashTable() {
    UErrorCode err = U_ZERO_ERROR;

    if (gCommonDataCache != NULL) {
        return gCommonDataCache;
    }
    umtx_lock(NULL);
    if (gCommonDataCache == NULL) {
        gCommonDataCache = uhash_open(uhash_hashChars, uhash_compareChars, &err);
        uhash_setValueDeleter(gCommonDataCache, DataCacheElement_deleter);
    }
    umtx_unlock(NULL);

    if (U_FAILURE(err)) {
        return NULL;      /* TODO:  handle this error better.  */
    }
    return gCommonDataCache;
}



static UDataMemory *udata_findCachedData(const char *path)
{
    UHashtable        *htable;
    UDataMemory       *retVal = NULL;
    DataCacheElement  *el;
    const char        *baseName;

    baseName = findBasename(path);   /* Cache remembers only the base name, not the full path. */
    htable = udata_getHashTable();
    umtx_lock(NULL);
    el = (DataCacheElement *)uhash_get(htable, baseName);
    umtx_unlock(NULL);
    if (el != NULL) {
        retVal = el->item;
    }
    return retVal;
}


static UDataMemory *udata_cacheDataItem(const char *path, UDataMemory *item, UErrorCode *pErr) {
    DataCacheElement *newElement;
    const char       *baseName;
    int               nameLen;
    UHashtable       *htable;
    UDataMemory      *oldValue = NULL;

    if (U_FAILURE(*pErr)) {
        return NULL;
    }

    /* Create a new DataCacheElement - the thingy we store in the hash table -
     * and copy the supplied path and UDataMemoryItems into it.
     */
    newElement = uprv_malloc(sizeof(DataCacheElement));
    if (newElement == NULL) {
        *pErr = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }
    newElement->item = UDataMemory_createNewInstance(pErr);
    if (U_FAILURE(*pErr)) {
        return NULL;
    }
    UDatamemory_assign(newElement->item, item);

    baseName = findBasename(path);
    nameLen = uprv_strlen(baseName);
    newElement->name = uprv_malloc(nameLen+1);
    if (newElement->name == NULL) {
        *pErr = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }
    uprv_strcpy(newElement->name, baseName);

    /* Stick the new DataCacheElement into the hash table.
    */
    htable = udata_getHashTable();
    umtx_lock(NULL);
    oldValue = uhash_get(htable, path);
    if (oldValue != NULL) {
        *pErr = U_USING_DEFAULT_WARNING; }
    else {
        uhash_put(
            htable,
            newElement->name,               /* Key   */
            newElement,                     /* Value */
            pErr);
    }
    umtx_unlock(NULL);

    if (*pErr == U_USING_DEFAULT_WARNING || U_FAILURE(*pErr)) {
        /*  TODO  - get rid of newElement->item too. */
        uprv_free(newElement->name);
        uprv_free(newElement);
        return oldValue;
    }

    return newElement->item;
}




/*----------------------------------------------------------------------*
 *                                                                      *
 *  Add a static reference to the common data  library                  *
 *   Unless overridden by an explicit u_setCommonData, this will be     *
 *      our common data.                                                *
 *                                                                      *
 *----------------------------------------------------------------------*/
extern  const DataHeader U_IMPORT U_ICUDATA_ENTRY_POINT;


/*----------------------------------------------------------------------*
 *                                                                      *
 *   openCommonData   Attempt to open a common format (.dat) file       *
 *                    Map it into memory (if it's not there already)    *
 *                    and return a UDataMemory object for it.           *
 *                                                                      *
 *                    If the requested data is already open and cached  *
 *                       just return the cached UDataMem object.        *
 *                                                                      *
 *----------------------------------------------------------------------*/
static UDataMemory *
openCommonData(
               const char *path,          /*  Path from OpenCHoice?          */
               UBool isICUData,           /*  ICU Data true if path == NULL  */
               UErrorCode *pErrorCode)
{
    const char *inBasename;
    char *basename, *suffix;
    char pathBuffer[1024];
    UDataMemory   tData;

    if (U_FAILURE(*pErrorCode)) {
        return NULL;
    }

    UDataMemory_init(&tData);

    if (isICUData) {
        /* "mini-cache" for common ICU data */
        if(gCommonICUData != NULL) {
            return gCommonICUData;
        }

        tData.pHeader = &U_ICUDATA_ENTRY_POINT;
        udata_checkCommonData(&tData, pErrorCode);
        setCommonICUData(&tData, NULL, FALSE, pErrorCode);
        return gCommonICUData;
    }


    /* request is NOT for ICU Data.
    * Is the requested data already cached?  */
    inBasename=findBasename(path);
    {
        UDataMemory  *dataToReturn = udata_findCachedData(inBasename);
        if (dataToReturn != NULL) {
            return dataToReturn;
        }
    }

    /* Requested item is not in the cache.
     * Hunt it down, trying all the fall back locations.
     */
    basename=setPathGetBasename(path, pathBuffer);
    if(*inBasename==0) {
        /* no basename, no common data */
        *pErrorCode=U_FILE_ACCESS_ERROR;
        return NULL;
    }


    /* set up the file name */
    suffix=strcpy_returnEnd(basename, inBasename);
    uprv_strcpy(suffix, ".dat");

    /* try path/basename first, then basename only */
    uprv_mapFile(&tData, pathBuffer);

    if (!UDataMemory_isLoaded(&tData)) {
        if (basename!=pathBuffer) {
            uprv_mapFile(&tData, basename);
        }
    }

    if (!UDataMemory_isLoaded(&tData)) {
        /* no common data */
        *pErrorCode=U_FILE_ACCESS_ERROR;
        return NULL;
    }

    /* we have mapped a file, check its header */
    udata_checkCommonData(&tData, pErrorCode);


    /* Cache the UDataMemory struct for this .dat file,
     *   so we won't need to hunt it down and map it again next time
     *   something is needed from it.                */
    return udata_cacheDataItem(inBasename, &tData, pErrorCode);
}




/*----------------------------------------------------------------------*
 *                                                                      *
 *   extendICUData   If the full set of ICU data was not loaded at      *
 *                   program startup, load it now.  This function will  *
 *                   be called when the lookup of an ICU data item in   *
 *                   the common ICU data fails.                         *
 *                                                                      *
 *                   The parameter is the UDataMemory in which the      *
 *                   search for a requested item failed.                *
 *                                                                      *
 *                   return true if new data is loaded, false otherwise.*
 *                                                                      *
 *----------------------------------------------------------------------*/
static UBool extendICUData(UDataMemory *failedData, UErrorCode *pErr)
{
    /*  If the data library that we are running with turns out to be the
     *   stub library (or, on the 390, the subset library), we will try to
     *   load a .dat file instead.  The stub library has no entries in its
     *   TOC, which is how we identify it here.
     */
    UDataMemory   *pData;

    if (failedData->vFuncs->NumEntries(failedData) > 0) {
        /*  Not the stub.  We can't extend.  */
        return FALSE;
    }

    /* See if we can explicitly open a .dat file for the ICUData. */
    pData = openCommonData(
               U_ICUDATA_NAME,            /*  "icudt20l" , for example.          */
               FALSE,                     /*  Pretend we're not opening ICUData  */
               pErr);

    /* How about if there is no pData, eh... */

    if(pData != NULL) {
      
      pData->map = 0;                 /* The mapping for this data is owned by the hash table */
      pData->mapAddr = 0;             /*   which will unmap it when ICU is shut down.         */
                                      /* CommonICUData is also unmapped when ICU is shut down.*/
                                      /* To avoid unmapping the data twice, zero out the map  */
                                      /*   fields in the UDataMemory that we're assigning     */
                                      /*   to CommonICUData.                                  */
    }
    

    setCommonICUData(pData,         /*  The new common data.                              */
                 failedData,        /*  Old ICUData ptr.  Overwrite of this value is ok,  */
                 FALSE,             /*  No warnings if write didn't happen                */
                 pErr);             /*  setCommonICUData honors errors; NOP if error set  */

    return gCommonICUData != failedData;   /* Return true if ICUData pointer was updated.   */
                                    /*   (Could potentialy have been done by another thread racing */
                                    /*   us through here, but that's fine, we still return true    */
                                    /*   so that current thread will also examine extended data.   */
}




/*----------------------------------------------------------------------*
 *                                                                      *
 *   udata_setCommonData                                                *
 *                                                                      *
 *----------------------------------------------------------------------*/
U_CAPI void U_EXPORT2
udata_setCommonData(const void *data, UErrorCode *pErrorCode) {
    UDataMemory dataMemory;

    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return;
    }

    if(data==NULL) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    /* do we already have common ICU data set? */
    if(gCommonICUData != NULL) {
        *pErrorCode=U_USING_DEFAULT_ERROR;
        return;
    }

    /* set the data pointer and test for validity */
    UDataMemory_init(&dataMemory);
    UDataMemory_setData(&dataMemory, data);
    udata_checkCommonData(&dataMemory, pErrorCode);
    if (U_FAILURE(*pErrorCode)) {return;}

    /* we have good data */
    /* Set it up as the ICU Common Data.  */
    setCommonICUData(&dataMemory, NULL, TRUE, pErrorCode);
}




/*---------------------------------------------------------------------------
 *
 *  udata_setAppData
 *
 *---------------------------------------------------------------------------- */
U_CAPI void U_EXPORT2
udata_setAppData(const char *path, const void *data, UErrorCode *err)
{
    UDataMemory     udm;

    if(err==NULL || U_FAILURE(*err)) {
        return;
    }
    if(data==NULL) {
        *err=U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    UDataMemory_init(&udm);
    udm.pHeader = data;
    udata_checkCommonData(&udm, err);
    udata_cacheDataItem(path, &udm, err);
}

/*----------------------------------------------------------------------------*
 *                                                                            *
 *  checkDataItem     Given a freshly located/loaded data item, either        *
 *                    an entry in a common file or a separately loaded file,  *
 *                    sanity check its header, and see if the data is         *
 *                    acceptable to the app.                                  *
 *                    If the data is good, create and return a UDataMemory    *
 *                    object that can be returned to the application.         *
 *                    Return NULL on any sort of failure.                     *
 *                                                                            *
 *----------------------------------------------------------------------------*/
static UDataMemory *
checkDataItem
(
 const DataHeader         *pHeader,         /* The data item to be checked.                */
 UDataMemoryIsAcceptable  *isAcceptable,    /* App's call-back function                    */
 void                     *context,         /*   pass-thru param for above.                */
 const char               *type,            /*   pass-thru param for above.                */
 const char               *name,            /*   pass-thru param for above.                */
 UErrorCode               *nonFatalErr,     /* Error code if this data was not acceptable  */
                                            /*   but openChoice should continue with       */
                                            /*   trying to get data from fallback path.    */
 UErrorCode               *fatalErr         /* Bad error, caller should return immediately */
 )
{
    UDataMemory  *rDataMem = NULL;          /* the new UDataMemory, to be returned.        */

    if (U_FAILURE(*fatalErr)) {
        return NULL;
    }

    if(pHeader->dataHeader.magic1==0xda &&
        pHeader->dataHeader.magic2==0x27 &&
        pHeader->info.isBigEndian==U_IS_BIG_ENDIAN &&
        (isAcceptable==NULL || isAcceptable(context, type, name, &pHeader->info))
        ) {
        rDataMem=UDataMemory_createNewInstance(fatalErr);
        if (U_FAILURE(*fatalErr)) {
            return NULL;
        }
        rDataMem->pHeader = pHeader;
    } else {
        /* the data is not acceptable, look further */
        /* If we eventually find something good, this errorcode will be */
        /*    cleared out.                                              */
        *nonFatalErr=U_INVALID_FORMAT_ERROR;
    }
    return rDataMem;
}




/*
 *  A note on the ownership of Mapped Memory
 *
 *  For common format files, ownership resides with the UDataMemory object
 *    that lives in the cache of opened common data.  These UDataMemorys are private
 *    to the udata implementation, and are never seen directly by users.
 *
 *    The UDataMemory objects returned to users will have the address of some desired
 *    data within the mapped region, but they wont have the mapping info itself, and thus
 *    won't cause anything to be removed from memory when they are closed.
 *
 *  For individual data files, the UDataMemory returned to the user holds the
 *  information necessary to unmap the data on close.  If the user independently
 *  opens the same data file twice, two completely independent mappings will be made.
 *  (There is no cache of opened data items from individual files, only a cache of
 *   opened Common Data files, that is, files containing a collection of data items.)
 *
 *  For common data passed in from the user via udata_setAppData() or
 *  udata_setCommonData(), ownership remains with the user.
 *
 *  UDataMemory objects themselves, as opposed to the memory they describe,
 *  can be anywhere - heap, stack/local or global.
 *  They have a flag to indicate when they're heap allocated and thus
 *  must be deleted when closed.
 */


/*----------------------------------------------------------------------------*
 *                                                                            *
 * main data loading functions                                                *
 *                                                                            *
 *----------------------------------------------------------------------------*/
static UDataMemory *
doOpenChoice(const char *path, const char *type, const char *name,
             UDataMemoryIsAcceptable *isAcceptable, void *context,
             UErrorCode *pErrorCode)
{
    char                pathBuffer[1024];
    char                tocEntryName[100];
    UDataMemory         dataMemory;
    UDataMemory        *pCommonData;
    UDataMemory        *pEntryData;
    const DataHeader   *pHeader;
    const char         *inBasename;
    char               *basename;
    char               *suffix;
    UErrorCode          errorCode=U_ZERO_ERROR;
    UBool               isICUData= (UBool)(path==NULL);


    /* Make up a full mame by appending the type to the supplied
     *  name, assuming that a type was supplied.
     */
    uprv_strcpy(tocEntryName, name);
    if(type!=NULL && *type!=0) {
        uprv_strcat(tocEntryName, ".");
        uprv_strcat(tocEntryName, type);
    }

    /* try to get common data.  The loop is for platforms such as the 390 that do
     *  not initially load the full set of ICU data.  If the lookup of an ICU data item
     *  fails, the full (but slower to load) set is loaded, the and the loop repeats,
     *  trying the lookup again.  Once the full set of ICU data is loaded, the loop wont
     *  repeat because the full set will be checked the first time through.
     *
     *  The loop also handles the fallback to a .dat file if the application linked
     *   to the stub data library rather than a real library.
     */
    for (;;) {
        pCommonData=openCommonData(path, isICUData, &errorCode);

        if(U_SUCCESS(errorCode)) {
            /* look up the data piece in the common data */
            pHeader=pCommonData->vFuncs->Lookup(pCommonData, tocEntryName, &errorCode);
            if(pHeader!=NULL) {
                pEntryData = checkDataItem(pHeader, isAcceptable, context, type, name, &errorCode, pErrorCode);
                if (U_FAILURE(*pErrorCode)) {
                    return NULL;
                }
                if (pEntryData != NULL) {
                    return pEntryData;
                }
            }
        }
        /* Data wasn't found.  If we were looking for an ICUData item and there is
         * more data available, load it and try again,
         * otherwise break out of this loop. */
        if (!(isICUData && extendICUData(pCommonData, &errorCode))) {
            break;
        }
    };


    /* the data was not found in the common data,  look further, */
    /* try to get an individual data file */
    basename=setPathGetBasename(path, pathBuffer);
    if(isICUData) {
        inBasename=COMMON_DATA_NAME;
    } else {
        inBasename=findBasename(path);
    }

#ifdef UDATA_DEBUG
    fprintf(stderr, "looking for ind. file\n");
#endif

    /* try path+basename+"_"+entryName first */
    if(*inBasename!=0) {
        suffix=strcpy_returnEnd(basename, inBasename);
        *suffix++='_';
        uprv_strcpy(suffix, tocEntryName);

        if( uprv_mapFile(&dataMemory, pathBuffer) ||
            (basename!=pathBuffer && uprv_mapFile(&dataMemory, basename)))
        {
            /* We mapped a file.  Check out its contents.   */
            pEntryData = checkDataItem(dataMemory.pHeader, isAcceptable, context, type, name, &errorCode, pErrorCode);
            if (pEntryData != NULL)
            {
               /* Got good data.
                *  Hand off ownership of the backing memory to the user's UDataMemory.
                *  and return it. */
                pEntryData->mapAddr = dataMemory.mapAddr;
                pEntryData->map     = dataMemory.map;
                return pEntryData;
            }

            /* the data is not acceptable, or some error occured.  Either way, unmap the memory */
            udata_close(&dataMemory);

            /* If we had a nasty error, bail out completely.  */
            if (U_FAILURE(*pErrorCode)) {
                return NULL;
            }

            /* Otherwise remember that we found data but didn't like it for some reason,
            *  and continue looking
            */
            errorCode=U_INVALID_FORMAT_ERROR;
        }
    }

    /* try path+entryName next */
    uprv_strcpy(basename, tocEntryName);
    if( uprv_mapFile(&dataMemory, pathBuffer) ||
        (basename!=pathBuffer && uprv_mapFile(&dataMemory, basename)))
    {
        pEntryData = checkDataItem(dataMemory.pHeader, isAcceptable, context, type, name, &errorCode, pErrorCode);
        if (pEntryData != NULL) {
           /* Data is good.
            *  Hand off ownership of the backing memory to the user's UDataMemory.
            *  and return it.   */
            pEntryData->mapAddr = dataMemory.mapAddr;
            pEntryData->map     = dataMemory.map;
            return pEntryData;
        }

        /* the data is not acceptable, or some error occured.  Either way, unmap the memory */
        udata_close(&dataMemory);

        /* If we had a nasty error, bail out completely.  */
        if (U_FAILURE(*pErrorCode)) {
            return NULL;
        }

        /* Otherwise remember that we found data but didn't like it for some reason  */
        errorCode=U_INVALID_FORMAT_ERROR;
    }

    /* data not found */
    if(U_SUCCESS(*pErrorCode)) {
        if(U_SUCCESS(errorCode)) {
            /* file not found */
            *pErrorCode=U_FILE_ACCESS_ERROR;
        } else {
            /* entry point not found or rejected */
            *pErrorCode=errorCode;
        }
    }
    return NULL;
}



/* API ---------------------------------------------------------------------- */

U_CAPI UDataMemory * U_EXPORT2
udata_open(const char *path, const char *type, const char *name,
           UErrorCode *pErrorCode) {
#ifdef UDATA_DEBUG
    fprintf(stderr, "udata_open(): Opening: %s . %s\n", name, type);
    fflush(stderr);
#endif

    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return NULL;
    } else if(name==NULL || *name==0) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    } else {
        return doOpenChoice(path, type, name, NULL, NULL, pErrorCode);
    }
}



U_CAPI UDataMemory * U_EXPORT2
udata_openChoice(const char *path, const char *type, const char *name,
                 UDataMemoryIsAcceptable *isAcceptable, void *context,
                 UErrorCode *pErrorCode) {
#ifdef UDATA_DEBUG
  fprintf(stderr, "udata_openChoice(): Opening: %s . %s\n", name, type);fflush(stderr);
#endif

    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return NULL;
    } else if(name==NULL || *name==0 || isAcceptable==NULL) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    } else {
        return doOpenChoice(path, type, name, isAcceptable, context, pErrorCode);
    }
}



U_CAPI void U_EXPORT2
udata_getInfo(UDataMemory *pData, UDataInfo *pInfo) {
    if(pInfo!=NULL) {
        if(pData!=NULL && pData->pHeader!=NULL) {
            const UDataInfo *info=&pData->pHeader->info;
            if(pInfo->size>info->size) {
                pInfo->size=info->size;
            }
            uprv_memcpy((uint16_t *)pInfo+1, (uint16_t *)info+1, pInfo->size-2);
        } else {
            pInfo->size=0;
        }
    }
}
