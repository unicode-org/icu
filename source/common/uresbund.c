/*
*******************************************************************************
* Copyright (C) 1997-1999, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* File URESBUND.C
*
* Modification History:
*
*   Date        Name        Description
*   04/01/97    aliu        Creation.
*   06/14/99    stephen     Removed functions taking a filename suffix.
*   07/20/99    stephen     Changed for UResourceBundle typedef'd to void*
*   11/09/99    weiv            Added ures_getLocale()
*   March 2000  weiv        Total overhaul - using data in DLLs
*   06/20/2000  helena      OS/400 port changes; mostly typecast.
*******************************************************************************
*/

#include "uresimp.h"
#include <stdio.h>

/* this is just for internal purposes. DO NOT USE! */
static void entryCloseInt(UResourceDataEntry *resB);
void entryClose(UResourceDataEntry *resB);


/* Static cache for already opened resource bundles - mostly for keeping fallback info */
static UHashtable *cache = NULL;
static UBool isMutexInited = FALSE;
static UMTX resbMutex = NULL;

/* INTERNAL: hashes an entry  */
int32_t hashEntry(const void *parm) {
    UResourceDataEntry *b = (UResourceDataEntry *)parm;
    return uhash_hashChars(b->fName)+37*uhash_hashChars(b->fPath);
}

/* INTERNAL: compares two entries */
UBool compareEntries(const void *p1, const void *p2) {
    UResourceDataEntry *b1 = (UResourceDataEntry *)p1;
    UResourceDataEntry *b2 = (UResourceDataEntry *)p2;

    return (UBool)(uhash_compareChars(b1->fName, b2->fName) & 
        uhash_compareChars(b1->fPath, b2->fPath));
}


/**
 *  Internal function, gets parts of locale name according 
 *  to the position of '_' character
 */
UBool chopLocale(char *name) {
    char *i = uprv_strrchr(name, '_');

    if(i != NULL) {
        *i = '\0';
        return TRUE;
    }

    return FALSE;
}

void entryIncrease(UResourceDataEntry *entry) {
        umtx_lock(&resbMutex);
        entry->fCountExisting++;
        while(entry->fParent != NULL) {
          entry = entry->fParent;
          entry->fCountExisting++;
        }
        umtx_unlock(&resbMutex);
}

/**
 *  Internal function. Tries to find a resource in given Resource 
 *  Bundle, as well as in its parents
 */
const ResourceData *getFallbackData(const UResourceBundle* resBundle, const char* * resTag, UResourceDataEntry* *realData, Resource *res, UErrorCode *status) {
    UResourceDataEntry *resB = resBundle->fData;
    int32_t indexR = -1;
    int32_t i = 0;
    *res = RES_BOGUS;
    if(resB != NULL) {
        if(resB->fBogus == U_ZERO_ERROR) { /* if this resource is real, */
            *res = res_getTableItemByKey(&(resB->fData), resB->fData.rootRes, &indexR, resTag); /* try to get data from there */
            i++;
        }
        if(resBundle->fHasFallback == TRUE) {
            while(*res == RES_BOGUS && resB->fParent != NULL) { /* Otherwise, we'll look in parents */
                resB = resB->fParent;
                if(resB->fBogus == U_ZERO_ERROR) {
                    i++;
                    *res = res_getTableItemByKey(&(resB->fData), resB->fData.rootRes, &indexR, resTag);
                }
            }
        }

        if(*res != RES_BOGUS) { /* If the resource is found in parents, we need to adjust the error */
            if(i>1) {
                if(uprv_strcmp(resB->fName, uloc_getDefault())==0 || uprv_strcmp(resB->fName, kRootLocaleName)==0) {
                    *status = U_USING_DEFAULT_ERROR;
                } else {
                    *status = U_USING_FALLBACK_ERROR;
                }
            }
            *realData = resB;
            return (&(resB->fData));
        } else { /* If resource is not found, we need to give an error */
            *status = U_MISSING_RESOURCE_ERROR;
            return NULL;
        }
    } else {
            *status = U_MISSING_RESOURCE_ERROR;
            return NULL;
    }
}

/** INTERNAL: Initializes the cache for resources */
void initCache(UErrorCode *status) {
    if(isMutexInited == FALSE) {
        umtx_lock(NULL);
        if(isMutexInited == FALSE) {
          umtx_init(&resbMutex);
          isMutexInited = TRUE;
        }
        umtx_unlock(NULL);
    }
    if(cache == NULL) {
        UHashtable *newCache = uhash_open(hashEntry, compareEntries, status);
        if (U_FAILURE(*status)) {
            return;
        }
        umtx_lock(&resbMutex);
        if(cache == NULL) {
            cache = newCache;
            newCache = NULL;
        }
        umtx_unlock(&resbMutex);
        if(newCache != NULL) {
            uhash_close(newCache);
        }
    }
}

/** INTERNAL: sets the name (locale) of the resource bundle to given name */

void setEntryName(UResourceDataEntry *res, char *name, UErrorCode *status) {
    if(res->fName != NULL) {
        uprv_free(res->fName);
    }
    res->fName = (char *)uprv_malloc(sizeof(char)*uprv_strlen(name)+1);
    if(res->fName == NULL) {
        *status = U_MEMORY_ALLOCATION_ERROR;
    } else {
        uprv_strcpy(res->fName, name);
    }
}

/**
 *  INTERNAL: Inits and opens an entry from a data DLL.
 */
UResourceDataEntry *init_entry(const char *localeID, const char *path, UErrorCode *status) {
    UResourceDataEntry *r = NULL;
    UResourceDataEntry find;
    int32_t hashValue;
    char name[96];
    const char *myPath = NULL;

    if(U_FAILURE(*status)) {
        return NULL;
    }

    /* here we try to deduce the right locale name */
    if(localeID == NULL) { /* if localeID is NULL, we're trying to open default locale */
        uprv_strcpy(name, uloc_getDefault());
    } else if(uprv_strlen(localeID) == 0) { /* if localeID is "" then we try to open root locale */
        uprv_strcpy(name, kRootLocaleName);
    } else { /* otherwise, we'll open what we're given */
        uprv_strcpy(name, localeID);
    }

    if(path != NULL) { /* if we actually have path, we'll use it */
        if(uprv_strcmp(path, u_getDataDirectory()) != 0) { /* unless it is system default path */
            myPath = path;
        }
    }

    find.fName = name;
    find.fPath = (char *)myPath;

    /* calculate the hash value of the entry */
    hashValue = hashEntry((const void *)&find);

    /* check to see if we already have this entry */
    r = (UResourceDataEntry *)uhash_get(cache, &find);

    if(r != NULL) { /* if the entry is already in the hash table */
        r->fCountExisting++; /* we just increase it's reference count */
        *status = r->fBogus; /* and set returning status */
    } else { /* otherwise, we'll try to construct a new entry */
        UBool result = FALSE;

        r = (UResourceDataEntry *) uprv_malloc(sizeof(UResourceDataEntry));

        if(r == NULL) {
            *status = U_MEMORY_ALLOCATION_ERROR;
            return NULL;
        }
        r->fCountExisting = 1;

        r->fName = NULL;
        setEntryName(r, name, status);

        r->fPath = NULL;
        if(myPath != NULL && !U_FAILURE(*status)) {
            r->fPath = (char *)uprv_malloc(sizeof(char)*uprv_strlen(myPath)+1);
            if(r->fPath == NULL) {
                *status = U_MEMORY_ALLOCATION_ERROR;
            } else {
                uprv_strcpy(r->fPath, myPath);
            }
        }

        r->fHashKey = hashValue;
        r->fParent = NULL;
        r->fData.data = NULL;
        r->fData.pRoot = NULL;
        r->fData.rootRes = 0;
        r->fBogus = U_ZERO_ERROR;
        
        /* this is the acutal loading - returns bool true/false */
        result = res_load(&(r->fData), r->fPath, r->fName, status);

        if (result == FALSE || U_FAILURE(*status)) { 
            /* we have no such entry in dll, so it will always use fallback */
            *status = U_USING_FALLBACK_ERROR;
            r->fBogus = U_USING_FALLBACK_ERROR;
        } else { /* if we have a regular entry */
            /* handle the alias by trying to get out the %%Alias tag.*/
            char aliasName[100];
            int32_t aliasLen;
            /* We'll try to get alias string from the bundle */
            Resource aliasres = res_getResource(&(r->fData), "%%ALIAS");
            const UChar *alias = res_getString(&(r->fData), aliasres, &aliasLen);
            if(alias != NULL && aliasLen > 0) { /* if there is actual alias - unload and load new data */
                u_UCharsToChars(alias, aliasName, u_strlen(alias)+1);
                res_unload(&(r->fData));
                res_load(&(r->fData), r->fPath, aliasName, status);
            }
        }

        {
            UResourceDataEntry *oldR = NULL;
            if((oldR = (UResourceDataEntry *)uhash_get(cache, r)) == NULL) { /* if the data is not cached */
              /* just insert it in the cache */
                uhash_put(cache, (void *)r, r, status);
            } else {
              /* somebody have already inserted it while we were working, discard newly opened data */
              /* this part is probably obsolete since we check cache in locked state */
                uprv_free(r->fName);
                if(r->fPath != NULL) {
                    uprv_free(r->fPath);
                }
                uprv_free(r);
                r = oldR;
                r->fCountExisting++;
            }
        }
    }
    return r;
}

UResourceDataEntry *entryOpen(const char* path, const char* localeID, UErrorCode* status) {
    UErrorCode initstatus = U_ZERO_ERROR;
    UResourceDataEntry *r = NULL;
    UResourceDataEntry *t1 = NULL;
    UResourceDataEntry *t2 = NULL;
    UBool isDefault = FALSE;
    UBool isRoot = FALSE;
    UBool hasRealData = FALSE;
    UBool hasChopped = FALSE;
    char name[96];

    if(U_FAILURE(*status)) {
      return NULL;
    }

    initCache(status);

    umtx_lock(&resbMutex);
    r = init_entry(localeID, path, &initstatus);
    uprv_strcpy(name, r->fName);
    isDefault = (UBool)(uprv_strncmp(name, uloc_getDefault(), uprv_strlen(name)) == 0);
    hasRealData = (UBool)(r->fBogus == U_ZERO_ERROR);

    isRoot = (UBool)(uprv_strcmp(name, kRootLocaleName) == 0);

    /*Fallback data stuff*/
    hasChopped = chopLocale(name);
    t1 = r;

    while (hasChopped && !isRoot && t1->fParent == NULL) {
        /* insert regular parents */
        t2 = init_entry(name, r->fPath, status);
        hasRealData = (UBool)((t2->fBogus == U_ZERO_ERROR) | hasRealData);
        t1->fParent = t2;
        t1 = t2;
        hasChopped = chopLocale(name);
    }

    if(!hasRealData && !isDefault && !isRoot && t1->fParent == NULL) {
        /* insert default locale */
        uprv_strcpy(name, uloc_getDefault());
        t2 = init_entry(name, r->fPath, status);
        hasRealData = (UBool)((t2->fBogus == U_ZERO_ERROR) | hasRealData);
        r->fBogus = U_USING_DEFAULT_ERROR;
        isDefault = TRUE;
        t1->fParent = t2;
        t1 = t2;
        hasChopped = chopLocale(name);
        while (hasChopped && t1->fParent == NULL) {
            /* insert chopped defaults */
            t2 = init_entry(name, r->fPath, status);
            hasRealData = (UBool)((t2->fBogus == U_ZERO_ERROR) | hasRealData);
            t1->fParent = t2;
            t1 = t2;
            hasChopped = chopLocale(name);
        }
    }

    if(!isRoot && uprv_strcmp(t1->fName, kRootLocaleName) != 0 && t1->fParent == NULL) {
        /* insert root locale */
        t2 = init_entry(kRootLocaleName, r->fPath, status);
        if(!hasRealData) {
          r->fBogus = U_USING_DEFAULT_ERROR;
        }
        hasRealData = (UBool)((t2->fBogus == U_ZERO_ERROR) | hasRealData);
        t1->fParent = t2;
        t1 = t2;
    }

    while(!isRoot && t1->fParent != NULL) {
        t1->fParent->fCountExisting++;
        t1 = t1->fParent;
        hasRealData = (UBool)((t1->fBogus == U_ZERO_ERROR) | hasRealData);
    }

    if(!hasRealData) {
        entryCloseInt(r);
        *status = U_MISSING_RESOURCE_ERROR;
    }

    umtx_unlock(&resbMutex);

    if(U_SUCCESS(*status)) {
      *status = r->fBogus;
      return r;
    } else {
      return NULL;
    }
}


/**
 * Functions to create and destroy resource bundles.
 */

/**
 *  INTERNAL: This function is used to open a resource bundle 
 *  without initializing fallback data. It is exclusively used 
 *  for initing Collation data at this point.
 */
U_CFUNC UResourceBundle* ures_openNoFallback(const char* path, const char* localeID, UErrorCode* status) {
    UResourceBundle *r = (UResourceBundle *)uprv_malloc(sizeof(UResourceBundle));
    if(r == NULL) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }

    r->fHasFallback = FALSE;
    r->fIsTopLevel = TRUE;
    ures_setIsStackObject(r, FALSE);
    r->fIndex = -1;
    r->fData = entryOpen(path, localeID, status);
    if(U_FAILURE(*status)) {
        uprv_free(r);
        return NULL;
    }
    if(r->fData->fBogus != U_ZERO_ERROR) {
        entryClose(r->fData);
        uprv_free(r);
        *status = U_MISSING_RESOURCE_ERROR;
        return NULL;
    }

    r->fKey = NULL;
    r->fVersion = NULL;
    r->fResData.data = r->fData->fData.data;
    r->fResData.pRoot = r->fData->fData.pRoot;
    r->fResData.rootRes = r->fData->fData.rootRes;
    r->fRes = r->fResData.rootRes;
    r->fSize = res_countArrayItems(&(r->fResData), r->fRes);
    return r;
}

UResourceBundle *init_resb_result(const ResourceData *rdata, const Resource r, const char *key, UResourceDataEntry *realData, UResourceBundle *resB, UErrorCode *status) {
    if(status == NULL || U_FAILURE(*status)) {
        return resB;
    }
    if(resB == NULL) {
        resB = (UResourceBundle *)uprv_malloc(sizeof(UResourceBundle));
        ures_setIsStackObject(resB, FALSE);
    } else {
        if(ures_isStackObject(resB, status) != FALSE) {
            ures_setIsStackObject(resB, TRUE);
        }
    }
    resB->fData = realData;
    entryIncrease(resB->fData);
    resB->fHasFallback = FALSE;
    resB->fIsTopLevel = FALSE;
    resB->fIndex = -1;
    resB->fKey = key;
    resB->fVersion = NULL;
    resB->fRes = r;
    resB->fResData.data = rdata->data;
    resB->fResData.pRoot = rdata->pRoot;
    resB->fResData.rootRes = rdata->rootRes;
    resB->fSize = res_countArrayItems(&(resB->fResData), resB->fRes);
    return resB;
}

UResourceBundle *copyResb(UResourceBundle *r, const UResourceBundle *original, UErrorCode *status) {
    UBool isStackObject;
    if(U_FAILURE(*status) || r == original) {
        return r;
    }
    if(original != NULL) {
        if(r == NULL) {
            isStackObject = FALSE;
            r = (UResourceBundle *)uprv_malloc(sizeof(UResourceBundle));
        } else {
            isStackObject = ures_isStackObject(r, status);
            if(U_FAILURE(*status)) {
                return r;
            }
            if(isStackObject == FALSE) {
                ures_close(r);
                r = (UResourceBundle *)uprv_malloc(sizeof(UResourceBundle));
            }
        }
        uprv_memcpy(r, original, sizeof(UResourceBundle));
        ures_setIsStackObject(r, isStackObject);
        if(r->fData != NULL) {
          entryIncrease(r->fData);
        }
        return r;
    } else {
        return r;
    }
}

/**
 * Functions to retrieve data from resource bundles.
 */

U_CAPI const UChar* U_EXPORT2 ures_getString(const UResourceBundle* resB, int32_t* len, UErrorCode* status) {

        if (status==NULL || U_FAILURE(*status)) {
                return NULL;
        }
        if(resB == NULL) {
                *status = U_ILLEGAL_ARGUMENT_ERROR;
                return NULL;
        }

    switch(RES_GET_TYPE(resB->fRes)) {
        case RES_STRING:
            return res_getString(&(resB->fResData), resB->fRes, len);
            break;
        case RES_INT:
        case RES_INT_VECTOR:
        case RES_BINARY:
        case RES_ARRAY:
        case RES_TABLE:
        default:
            *status = U_RESOURCE_TYPE_MISMATCH;
            return NULL;
            break;
    }

	return NULL;
}

U_CAPI const uint8_t* U_EXPORT2 ures_getBinary(const UResourceBundle* resB, int32_t* len, 
                                               UErrorCode*               status) {
  
  if (status==NULL || U_FAILURE(*status)) {
    return NULL;
  }
  if(resB == NULL) {
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return NULL;
  }
  switch(RES_GET_TYPE(resB->fRes)) {
  case RES_BINARY:
    return res_getBinary(&(resB->fResData), resB->fRes, len);
    break;
  case RES_INT:
  case RES_STRING:
  case RES_INT_VECTOR:
  case RES_ARRAY:
  case RES_TABLE:
  default:
    *status = U_RESOURCE_TYPE_MISMATCH;
    return NULL;
    break;
  }

  return NULL;
}

U_CAPI uint32_t U_EXPORT2 ures_getInt(const UResourceBundle* resB, UErrorCode *status) {
  
  if (status==NULL || U_FAILURE(*status)) {
    return 0xffffffff;
  }
  if(resB == NULL) {
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return 0xffffffff;
  }
  return RES_GET_UINT(resB->fRes);
}


U_CAPI UResType U_EXPORT2 ures_getType(UResourceBundle *resB) {
  if(resB == NULL) {
    return (UResType) RES_BOGUS;
  }
  return (UResType) (RES_GET_TYPE(resB->fRes));
}

U_CAPI const char * U_EXPORT2 ures_getKey(UResourceBundle *resB) {
  if(resB == NULL) {
    return NULL;
  }
  
  return(resB->fKey);
}

U_CAPI int32_t U_EXPORT2 ures_getSize(UResourceBundle *resB) {
  if(resB == NULL) {
    return 0;
  }
  
  return resB->fSize;
}

U_CAPI void U_EXPORT2 ures_resetIterator(UResourceBundle *resB){
  if(resB == NULL) {
    return;
  }
  resB->fIndex = -1;
}

U_CAPI UBool U_EXPORT2 ures_hasNext(UResourceBundle *resB) {
  if(resB == NULL) {
    return FALSE;
  }
  return (UBool)(resB->fIndex < resB->fSize-1);
}

U_CAPI const UChar* U_EXPORT2 ures_getNextString(UResourceBundle *resB, int32_t* len, const char ** key, UErrorCode *status) {
  Resource r = RES_BOGUS;
  
  if (status==NULL || U_FAILURE(*status)) {
    return NULL;
  }
  if(resB == NULL) {
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return NULL;
  }
  
  if(resB->fIndex == resB->fSize-1) {
    *status = U_INDEX_OUTOFBOUNDS_ERROR;
        return NULL;
  } else {
    resB->fIndex++;
    switch(RES_GET_TYPE(resB->fRes)) {
    case RES_INT:
    case RES_BINARY:
    case RES_STRING:
      return res_getString(&(resB->fResData), resB->fRes, len); 
      break;
    case RES_TABLE:
      r = res_getTableItemByIndex(&(resB->fResData), resB->fRes, resB->fIndex, key);
      if(r == RES_BOGUS && resB->fHasFallback) {
        /* TODO: do the fallback */
      }
      return res_getString(&(resB->fResData), r, len); 
      break;
        case RES_ARRAY:
          r = res_getArrayItem(&(resB->fResData), resB->fRes, resB->fIndex);
          if(r == RES_BOGUS && resB->fHasFallback) {
            /* TODO: do the fallback */
          }
          return res_getString(&(resB->fResData), r, len);
          break;
    case RES_INT_VECTOR:
    default:
      return NULL;
      break;
    }

  }
  return NULL;
}

U_CAPI UResourceBundle* U_EXPORT2 ures_getNextResource(UResourceBundle *resB, UResourceBundle *fillIn, UErrorCode *status) {
    const char *key = NULL;
    Resource r = RES_BOGUS;

        if (status==NULL || U_FAILURE(*status)) {
                /*return NULL;*/
                return fillIn;
        }
        if(resB == NULL) {
                *status = U_ILLEGAL_ARGUMENT_ERROR;
                /*return NULL;*/
                return fillIn;
        }

    if(resB->fIndex == resB->fSize-1) {
      *status = U_INDEX_OUTOFBOUNDS_ERROR;
        /*return NULL;*/
        return fillIn;
    } else {
        resB->fIndex++;
        switch(RES_GET_TYPE(resB->fRes)) {
        case RES_INT:
        case RES_BINARY:
        case RES_STRING:
            return copyResb(fillIn, resB, status);
            break;
        case RES_TABLE:
            r = res_getTableItemByIndex(&(resB->fResData), resB->fRes, resB->fIndex, &key);
            if(r == RES_BOGUS && resB->fHasFallback) {
                /* TODO: do the fallback */
            }
            return init_resb_result(&(resB->fResData), r, key, resB->fData, fillIn, status);
            break;
        case RES_ARRAY:
            r = res_getArrayItem(&(resB->fResData), resB->fRes, resB->fIndex);
            if(r == RES_BOGUS && resB->fHasFallback) {
                /* TODO: do the fallback */
            }
            return init_resb_result(&(resB->fResData), r, key, resB->fData, fillIn, status);
            break;
        case RES_INT_VECTOR:
        default:
            /*return NULL;*/
            return fillIn;
            break;
        }
    }
    /*return NULL;*/
    return fillIn;
}

U_CAPI UResourceBundle* U_EXPORT2 ures_getByIndex(const UResourceBundle *resB, int32_t indexR, UResourceBundle *fillIn, UErrorCode *status) {
    const char* key = NULL;
    Resource r = RES_BOGUS;

        if (status==NULL || U_FAILURE(*status)) {
            /*return NULL;*/
            return fillIn;
        }
        if(resB == NULL) {
                *status = U_ILLEGAL_ARGUMENT_ERROR;
                /*return NULL;*/
                return fillIn;
        }

    if(indexR >= 0 && resB->fSize > indexR) {
        switch(RES_GET_TYPE(resB->fRes)) {
        case RES_INT:
        case RES_BINARY:
        case RES_STRING:
            return copyResb(fillIn, resB, status);
            break;
        case RES_TABLE:
            r = res_getTableItemByIndex(&(resB->fResData), resB->fRes, indexR, &key);
            if(r == RES_BOGUS && resB->fHasFallback) {
                /* TODO: do the fallback */
            }
            return init_resb_result(&(resB->fResData), r, key, resB->fData, fillIn, status);
            break;
        case RES_ARRAY:
            r = res_getArrayItem(&(resB->fResData), resB->fRes, indexR);
            if(r == RES_BOGUS && resB->fHasFallback) {
                /* TODO: do the fallback */
            }
            return init_resb_result(&(resB->fResData), r, key, resB->fData, fillIn, status);
            break;
        case RES_INT_VECTOR:
        default:
            /*return NULL;*/
            return fillIn;
            break;
        }
    } else {
        *status = U_MISSING_RESOURCE_ERROR;
        /*return NULL;*/
        return fillIn;
    }
    /*return NULL;*/
    return fillIn;
}

U_CAPI const UChar* U_EXPORT2 ures_getStringByIndex(const UResourceBundle *resB, int32_t indexS, int32_t* len, UErrorCode *status) {
    const char* key = NULL;
    Resource r = RES_BOGUS;

        if (status==NULL || U_FAILURE(*status)) {
                return NULL;
        }
        if(resB == NULL) {
                *status = U_ILLEGAL_ARGUMENT_ERROR;
                return NULL;
        }

    if(indexS >= 0 && resB->fSize > indexS) {
        switch(RES_GET_TYPE(resB->fRes)) {
        case RES_INT:
        case RES_BINARY:
        case RES_STRING:
            return res_getString(&(resB->fResData), resB->fRes, len);
            break;
        case RES_TABLE:
            r = res_getTableItemByIndex(&(resB->fResData), resB->fRes, indexS, &key);
            if(r == RES_BOGUS && resB->fHasFallback) {
                /* TODO: do the fallback */
            }
            return res_getString(&(resB->fResData), r, len);
            break;
        case RES_ARRAY:
            r = res_getArrayItem(&(resB->fResData), resB->fRes, indexS);
            if(r == RES_BOGUS && resB->fHasFallback) {
                /* TODO: do the fallback */
            }
            return res_getString(&(resB->fResData), r, len);
            break;
        case RES_INT_VECTOR:
        default:
            return NULL;
            break;
        }
    } else {
        *status = U_MISSING_RESOURCE_ERROR;
        return NULL;
    }
	return NULL;
}

U_CAPI UResourceBundle* U_EXPORT2 ures_getByKey(const UResourceBundle *resB, const char* inKey, UResourceBundle *fillIn, UErrorCode *status) {
    Resource res = RES_BOGUS;
    UResourceDataEntry *realData = NULL;
    const char *key = inKey;

        if (status==NULL || U_FAILURE(*status)) {
            /*return NULL;*/
            return fillIn;
        }
        if(resB == NULL) {
                *status = U_ILLEGAL_ARGUMENT_ERROR;
                /*return NULL;*/
                return fillIn;
        }

    if(RES_GET_TYPE(resB->fRes) == RES_TABLE) {
        int32_t t;
        res = res_getTableItemByKey(&(resB->fResData), resB->fRes, &t, &key);
        if(res == RES_BOGUS) {
          key = inKey;
                if(resB->fHasFallback == TRUE) {
                    const ResourceData *rd = getFallbackData(resB, &key, &realData, &res, status);
                    if(U_FAILURE(*status)) {
                        /*return NULL;*/
                        return fillIn;
                    } else {
                        return init_resb_result(rd, res, key, realData, fillIn, status);
                    }
                } else {
                    *status = U_MISSING_RESOURCE_ERROR;
                    /*return NULL;*/
                    return fillIn;
                }
        } else {
            return init_resb_result(&(resB->fResData), res, key, resB->fData, fillIn, status);
        }
    } else if(RES_GET_TYPE(resB->fRes) == RES_ARRAY && resB->fHasFallback == TRUE) {
        /* here should go a first attempt to locate the key using index table */
        const ResourceData *rd = getFallbackData(resB, &key, &realData, &res, status);
        if(U_FAILURE(*status)) {
            /*return NULL;*/
            return fillIn;
        } else {
            return init_resb_result(rd, res, key, realData, fillIn, status);
        }
    } else {
        *status = U_RESOURCE_TYPE_MISMATCH;
        /*return NULL;*/
        return fillIn;
    }
    /*return NULL;*/
    return fillIn;
}

U_CAPI const UChar* U_EXPORT2 ures_getStringByKey(const UResourceBundle *resB, const char* inKey, int32_t* len, UErrorCode *status) {
    Resource res = RES_BOGUS;
    UResourceDataEntry *realData = NULL;
    const char* key = inKey;

        if (status==NULL || U_FAILURE(*status)) {
                return NULL;
        }
        if(resB == NULL) {
                *status = U_ILLEGAL_ARGUMENT_ERROR;
                return NULL;
        }

    if(RES_GET_TYPE(resB->fRes) == RES_TABLE) {
        int32_t t=0;
        res = res_getTableItemByKey(&(resB->fResData), resB->fRes, &t, &key);
        if(res == RES_BOGUS) {
          key = inKey;
                if(resB->fHasFallback == TRUE) {
                    const ResourceData *rd = getFallbackData(resB, &key, &realData, &res, status);
                    if(U_FAILURE(*status)) {
                        *status = U_MISSING_RESOURCE_ERROR;
                        return NULL;
                    } else {
                        return res_getString(rd, res, len);
                    }
                } else {
                    *status = U_MISSING_RESOURCE_ERROR;
                    return NULL;
                }
        } else {
            return res_getString(&(resB->fResData), res, len);
        }
    } else if(RES_GET_TYPE(resB->fRes) == RES_ARRAY && resB->fHasFallback == TRUE) {
        /* here should go a first attempt to locate the key using index table */
        const ResourceData *rd = getFallbackData(resB, &key, &realData, &res, status);
        if(U_FAILURE(*status)) {
            *status = U_MISSING_RESOURCE_ERROR;
            return NULL;
        } else {
            return res_getString(rd, res, len);
        }
    } else {
        *status = U_RESOURCE_TYPE_MISMATCH;
        return NULL;
    }
	return NULL;
}


/* TODO: clean from here down */

/**
 *  INTERNAL: Get the name of the first real locale (not placeholder) 
 *  that has resource bundle data.
 */
U_CFUNC const char* ures_getRealLocale(const UResourceBundle* resourceBundle, UErrorCode* status)
{
    const UResourceDataEntry *resB = resourceBundle->fData;
        if (status==NULL || U_FAILURE(*status)) {
                return NULL;
        }
    if (!resourceBundle) {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }
    while(resB->fBogus != U_ZERO_ERROR && resB->fParent != NULL) {
        resB = resB->fParent;
    }
    if(resB->fBogus == U_ZERO_ERROR) {
        return resB->fName;
    } else {
        *status = U_INTERNAL_PROGRAM_ERROR;
        return NULL;
    }
	return NULL;
}

static void entryCloseInt(UResourceDataEntry *resB) {
    UResourceDataEntry *p = resB;

    while(resB != NULL) {
        p = resB->fParent;
        resB->fCountExisting--;

        if(resB->fCountExisting <= 0) {

          /* Entries are left in the cache. TODO: add ures_cacheFlush() to force a flush
             of the cache. */
/*
            uhash_remove(cache, resB);
            if(resB->fBogus == U_ZERO_ERROR) {
                res_unload(&(resB->fData));
            }
            if(resB->fName != NULL) {
                uprv_free(resB->fName);
            }
            if(resB->fPath != NULL) {
                uprv_free(resB->fPath);
            }
            uprv_free(resB);
*/
        }

        resB = p;
    }
}

/** 
 *  API: closes a resource bundle and cleans up.
 */

void entryClose(UResourceDataEntry *resB) {
  umtx_lock(&resbMutex);
  entryCloseInt(resB);
  umtx_unlock(&resbMutex);
}


U_CFUNC const char* ures_getName(const UResourceBundle* resB) {
    return resB->fData->fName;
}
U_CFUNC const char* ures_getPath(const UResourceBundle* resB) {
    return resB->fData->fPath;
}
U_CFUNC const char* ures_getTag(const UResourceBundle* resB) {
    return resB->fKey;
}
U_CFUNC const ResourceData * ures_getResData(const UResourceBundle* resB) {
    return &(resB->fData->fData);
}

/* OLD API implementation */

/**
 *  API: This function is used to open a resource bundle 
 *  proper fallback chaining is executed while initialization. 
 *  The result is stored in cache for later fallback search.
 */
U_CAPI void ures_openFillIn(UResourceBundle *r, const char* path,
                    const char* localeID, UErrorCode* status) {
    if(r == NULL) {
        *status = U_INTERNAL_PROGRAM_ERROR;
    } else {
        UResourceDataEntry *firstData;
        r->fHasFallback = TRUE;
        r->fIsTopLevel = TRUE;
        r->fKey = NULL;
        r->fVersion = NULL;
        r->fIndex = -1;
        r->fData = entryOpen(path, localeID, status);
        /* this is a quick fix to get regular data in bundle - until construction is cleaned up */
        firstData = r->fData;
        while(firstData->fBogus != U_ZERO_ERROR && firstData->fParent != NULL) {
            firstData = firstData->fParent;
        }
        r->fResData.data = firstData->fData.data;
        r->fResData.pRoot = firstData->fData.pRoot;
        r->fResData.rootRes = firstData->fData.rootRes;
        r->fRes = r->fResData.rootRes;
        r->fSize = res_countArrayItems(&(r->fResData), r->fRes);
    }
}
U_CAPI UResourceBundle* ures_open(const char* path,
                    const char* localeID,
                    UErrorCode* status)
{
    UResourceDataEntry *hasData = NULL;
    UResourceBundle *r = (UResourceBundle *)uprv_malloc(sizeof(UResourceBundle));
    if(r == NULL) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }

    r->fHasFallback = TRUE;
    r->fIsTopLevel = TRUE;
    ures_setIsStackObject(r, FALSE);
    r->fKey = NULL;
    r->fVersion = NULL;
    r->fIndex = -1;
    r->fData = entryOpen(path, localeID, status);
    if(U_FAILURE(*status)) {
        uprv_free(r);
        return NULL;
    }

    hasData = r->fData;
    while(hasData->fBogus != U_ZERO_ERROR) {
        hasData = hasData->fParent;
        if(hasData == NULL) {
            entryClose(r->fData);
            uprv_free(r);
            *status = U_MISSING_RESOURCE_ERROR;
            return NULL;
        }
    }

    r->fResData.data = hasData->fData.data;
    r->fResData.pRoot = hasData->fData.pRoot;
    r->fResData.rootRes = hasData->fData.rootRes;
    r->fRes = r->fResData.rootRes;
    r->fSize = res_countArrayItems(&(r->fResData), r->fRes);

    return r;
}

U_CAPI UResourceBundle* ures_openW(const wchar_t* myPath,
                    const char* localeID,
                    UErrorCode* status)
{
    char path[100];
    UResourceBundle *r;
    size_t tempSize = uprv_wcstombs(NULL, myPath, ((size_t)-1) >> 1);
    /*char *temp = new char[tempSize + 1];*/

    tempSize = uprv_wcstombs(path, myPath, tempSize);
    path[tempSize] = 0;

    /*u_UCharsToChars(myPath, path, uprv_wcslen(myPath)+1);*/

    r = ures_open(path, localeID, status);

    if (r == FALSE || U_FAILURE(*status)) {
        return NULL;
    }

    return r;
}


U_CAPI UResourceBundle* U_EXPORT2 ures_openU(const UChar* myPath, 
                  const char* localeID, 
                  UErrorCode* status)
{
    char path[100];
    UResourceBundle *r;
    int32_t pathlen = u_strlen(myPath);


    u_UCharsToChars(myPath, path, pathlen);
    path[pathlen] = 0;

    r = ures_open(path, localeID, status);

    if (r == FALSE || U_FAILURE(*status)) {
        return NULL;
    }

    return r;
}

U_CAPI void ures_setIsStackObject( UResourceBundle* resB, UBool state) {
    if(state == TRUE) {
        resB->fMagic1 = 0;
        resB->fMagic2 = 0;
    } else {
        resB->fMagic1 = MAGIC1;
        resB->fMagic2 = MAGIC2;
    }
}

U_CAPI UBool ures_isStackObject( UResourceBundle* resB, UErrorCode *status) {
    if(status == NULL || U_FAILURE(*status)) {
        return FALSE;
    }
    if(resB == NULL) {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        return TRUE;
    }
    if(resB->fMagic1 == MAGIC1 && resB->fMagic2 == MAGIC2) {
        return FALSE;
    } else {
        return TRUE;
    }
}


U_CAPI const UChar* ures_get(    const UResourceBundle*    resB,
                const char*              resourceTag,
                UErrorCode*               status) 
{
    int32_t len = 0;
        if(resB == NULL || U_FAILURE(*status)) {
                *status = U_ILLEGAL_ARGUMENT_ERROR;
                return NULL;
        }
    return ures_getStringByKey(resB, resourceTag, &len, status);
}

U_CAPI const UChar* ures_getArrayItem(const UResourceBundle*     resB,
                    const char*               resourceTag,
                    int32_t                   resourceIndex,
                    UErrorCode*                status)
{
    UResourceBundle res;
    ures_setIsStackObject(&res, TRUE);
        if (status==NULL || U_FAILURE(*status)) {
                return NULL;
        }
        if(resB == NULL) {
                *status = U_ILLEGAL_ARGUMENT_ERROR;
                return NULL;
        }
    ures_getByKey(resB, resourceTag, &res, status);
    if(U_SUCCESS(*status)) {
        int32_t len = 0;
        const UChar *r = ures_getStringByIndex(&res, resourceIndex, &len, status);
        ures_close(&res);
        return r;
    } else {
        return NULL;
    }
}

U_CAPI const UChar* ures_get2dArrayItem(const UResourceBundle*   resB,
                      const char*             resourceTag,
                      int32_t                 rowIndex,
                      int32_t                 columnIndex,
                      UErrorCode*              status)
{
    UResourceBundle res;
    ures_setIsStackObject(&res, TRUE);
        if (status==NULL || U_FAILURE(*status)) {
                return NULL;
        }
        if(resB == NULL) {
                *status = U_ILLEGAL_ARGUMENT_ERROR;
                return NULL;
        }
    ures_getByKey(resB, resourceTag, &res, status);
    if(U_SUCCESS(*status)) {
        UResourceBundle res2;
	ures_setIsStackObject(&res2, TRUE);
        ures_getByIndex(&res, rowIndex, &res2, status);
        ures_close(&res);
        if(U_SUCCESS(*status)) {
            int32_t len = 0;
            const UChar *r = ures_getStringByIndex(&res2, columnIndex, &len, status);
            ures_close(&res2);
            return r;
        } else {
            return NULL;
        }
    } else {
        return NULL;
    }
}

U_CAPI const UChar* ures_getTaggedArrayItem(const UResourceBundle*   resB,
                      const char*             resourceTag,
                      const char*             itemTag,
                      UErrorCode*              status)
{
    UResourceBundle res;
    ures_setIsStackObject(&res, TRUE);
        if (status==NULL || U_FAILURE(*status)) {
                return NULL;
        }
        if(resB == NULL) {
                *status = U_ILLEGAL_ARGUMENT_ERROR;
                return NULL;
        }
    ures_getByKey(resB, resourceTag, &res, status);
    if(U_SUCCESS(*status)) {
        int32_t len = 0;
        const UChar *r = ures_getStringByKey(&res, itemTag, &len, status);
        ures_close(&res);
        return r;
    } else {
        return NULL;
    }
}

/**
 *  API: Counts members. For arrays and tables, returns number of resources.
 *  For strings, returns 1.
 */
U_CAPI int32_t ures_countArrayItems(const UResourceBundle* resourceBundle,
                  const char* resourceKey,
                  UErrorCode* status)
{
    UResourceBundle resData;
    ures_setIsStackObject(&resData, TRUE);
        if (status==NULL || U_FAILURE(*status)) {
                return 0;
        }
        if(resourceBundle == NULL) {
                *status = U_ILLEGAL_ARGUMENT_ERROR;
                return 0;
        }
    ures_getByKey(resourceBundle, resourceKey, &resData, status);

    if(resData.fResData.data != NULL) {
      int32_t result = res_countArrayItems(&resData.fResData, resData.fRes);
      ures_close(&resData);
      return result;
    } else {
      *status = U_MISSING_RESOURCE_ERROR;
      ures_close(&resData);
      return 0;
    }
}

U_CAPI void ures_close(UResourceBundle*    resB)
{
    UErrorCode status = U_ZERO_ERROR;
    if(resB != NULL) {
        if(resB->fData != NULL) {
            entryClose(resB->fData);
        }
        /*
        if(resB->fKey != NULL) {
            uprv_free(resB->fKey);
        }
        */
        if(resB->fVersion != NULL) {
            uprv_free(resB->fVersion);
        }

        if(ures_isStackObject(resB, &status) == FALSE) {
            uprv_free(resB);
        }
    }
}

U_CAPI const char* ures_getVersionNumber(const UResourceBundle*   resourceBundle)
{
    if (!resourceBundle) return NULL;

    if(resourceBundle->fVersion == NULL) {

        /* If the version ID has not been built yet, then do so.  Retrieve */
        /* the minor version from the file. */
        UErrorCode status = U_ZERO_ERROR;
        int32_t minor_len = 0;

        const UChar* minor_version = ures_getStringByKey(resourceBundle, kVersionTag, &minor_len, &status);
    
        /* Determine the length of of the final version string.  This is */
        /* the length of the major part + the length of the separator */
        /* (==1) + the length of the minor part (+ 1 for the zero byte at */
        /* the end). */
        int32_t len = uprv_strlen(U_ICU_VERSION);
        /*
        if(U_SUCCESS(status) && minor_version.length() > 0) 
        {
          minor_len = u_strlen(minor_version);
        }
        */
        len += (minor_len > 0) ? minor_len : 1 /*==uprv_strlen(kDefaultMinorVersion)*/;
        ++len; /* Add length of separator */
    
        /* Allocate the string, and build it up. */
        /* + 1 for zero byte */


        ((UResourceBundle *)resourceBundle)->fVersion = (char *)uprv_malloc(1 + len); 
    
        uprv_strcpy(resourceBundle->fVersion, U_ICU_VERSION);
        uprv_strcat(resourceBundle->fVersion, kVersionSeparator);

        if(minor_len > 0) {
            u_UCharsToChars(minor_version, resourceBundle->fVersion + len - minor_len, minor_len);
          /*minor_version.extract(0, minor_len, fVersionID + len - minor_len, "");*/
            resourceBundle->fVersion[len] =  '\0';
        }
        else {
          uprv_strcat(resourceBundle->fVersion, kDefaultMinorVersion);
        }
    }

    return resourceBundle->fVersion;
}

U_CAPI void U_EXPORT2 ures_getVersion(const UResourceBundle* resB, UVersionInfo versionInfo) {
    if (!resB) return;
}

/**
 *  API: get the nominal name of resource bundle locale,
 *  regardless of wether resource bundle really exists 
 *  or not.
 */
U_CAPI const char* ures_getLocale(const UResourceBundle* resourceBundle, UErrorCode* status)
{
        if (status==NULL || U_FAILURE(*status)) {
                return NULL;
        }
  if (!resourceBundle)
    {
      *status = U_ILLEGAL_ARGUMENT_ERROR;
      return NULL;
    }
  return ures_getName(resourceBundle);
}


/* eof */
