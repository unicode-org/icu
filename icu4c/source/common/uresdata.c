/*
*******************************************************************************
*                                                                             *
* Copyright (C) 1999-2003, International Business Machines Corporation        *
*               and others. All Rights Reserved.                              *
*                                                                             *
*******************************************************************************
*   file name:  uresdata.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 1999dec08
*   created by: Markus W. Scherer
* Modification History:
*
*   Date        Name        Description
*   06/20/2000  helena      OS/400 port changes; mostly typecast.
*   06/24/02    weiv        Added support for resource sharing
*/

#include "unicode/utypes.h"
#include "cstring.h"
#include "unicode/udata.h"
#include "uresdata.h"
#include "uresimp.h"

/*
 * Resource access helpers
 */

/* get a const char* pointer to the key with the keyOffset byte offset from pRoot */
#define RES_GET_KEY(pRoot, keyOffset) ((const char *)(pRoot)+(keyOffset))
#define URESDATA_ITEM_NOT_FOUND 0xFFFF

/*
 * All the type-access functions assume that
 * the resource is of the expected type.
 */


/*
 * Array functions
 */
static Resource
_res_getArrayItem(Resource *pRoot, Resource res, int32_t indexR) {
    int32_t *p=(int32_t *)RES_GET_POINTER(pRoot, res);
    if(indexR<*p) {
        return ((Resource *)(p))[1+indexR];
    } else {
        return RES_BOGUS;   /* indexR>itemCount */
    }
}

/*
 * Table functions
 *
 * Important: the key offsets are 16-bit byte offsets from pRoot,
 * and the itemCount is one more 16-bit, too.
 * Thus, there are (count+1) uint16_t values.
 * In order to 4-align the Resource item values, there is a padding
 * word if count is even, i.e., there is exactly (~count&1)
 * 16-bit padding words.
 */
static const char *
_res_getTableKey(const Resource *pRoot, const Resource res, uint16_t indexS) {
    uint16_t *p=(uint16_t *)RES_GET_POINTER(pRoot, res);
    if(indexS<*p) {
        return RES_GET_KEY(pRoot, p[indexS+1]);
    } else {
        return NULL;    /* indexS>itemCount */
    }
}

static Resource
_res_getTableItem(const Resource *pRoot, const Resource res, uint16_t indexR) {
    uint16_t *p=(uint16_t *)RES_GET_POINTER(pRoot, res);
    uint16_t count=*p;
    if(indexR<count) {
        return ((Resource *)(p+1+count+(~count&1)))[indexR];
    } else {
        return RES_BOGUS;   /* indexR>itemCount */
    }
}

static Resource
_res_findTableItem(const Resource *pRoot, const Resource res, const char *key) {
    uint16_t *p=(uint16_t *)RES_GET_POINTER(pRoot, res);
    uint16_t i, start, limit;

    limit=*p++; /* number of entries */

    if(limit == 0) { /* this table is empty */
      return RES_BOGUS;
    }

    /* do a binary search for the key */
    start=0;
    while(start<limit-1) {
        i=(uint16_t)((start+limit)/2);
        if(uprv_strcmp(key, RES_GET_KEY(pRoot, p[i]))<0) {
            limit=i;
        } else {
            start=i;
        }
    }

    /* did we really find it? */
    if(uprv_strcmp(key, RES_GET_KEY(pRoot, p[start]))==0) {
        limit=*(p-1);   /* itemCount */
        return ((Resource *)(p+limit+(~limit&1)))[start];
    } else {
        return RES_BOGUS;   /* not found */
    }
}

static Resource
_res_findTableItemN(const Resource *pRoot, const Resource res, const char *key, int32_t keyLen, const char **realKey) {
    uint16_t *p=(uint16_t *)RES_GET_POINTER(pRoot, res);
    uint16_t i, start, limit;

    limit=*p++; /* number of entries */

    if(limit == 0) { /* this table is empty */
      return RES_BOGUS;
    }

    /* do a binary search for the key */
    start=0;
    while(start<limit-1) {
        i=(uint16_t)((start+limit)/2);
        if(uprv_strncmp(key, RES_GET_KEY(pRoot, p[i]), keyLen)<0) { 
            limit=i;
        } else {
            start=i;
        }
    }

    /* did we really find it? */
    if(uprv_strncmp(key, RES_GET_KEY(pRoot, p[start]), keyLen)==0) {
        *realKey = RES_GET_KEY(pRoot, p[start]);
        limit=*(p-1);   /* itemCount */
        return ((Resource *)(p+limit+(~limit&1)))[start];
    } else {
        *realKey = NULL;
        return RES_BOGUS;   /* not found */
    }
}

static uint16_t
_res_findTableIndex(const Resource *pRoot, const Resource res, const char *key) {
    uint16_t *p=(uint16_t *)RES_GET_POINTER(pRoot, res);
    uint16_t i, start, limit;

    limit=*p++; /* number of entries */

    if(limit == 0) { /* this table is empty */
      return URESDATA_ITEM_NOT_FOUND;
    }

    /* do a binary search for the key */
    start=0;
    while(start<limit-1) {
        i=(uint16_t)((start+limit)/2);
        if(uprv_strcmp(key, RES_GET_KEY(pRoot, p[i]))<0) {
            limit=i;
        } else {
            start=i;
        }
    }
    
    /* did we really find it? */
    if(uprv_strcmp(key, RES_GET_KEY(pRoot, p[start]))==0) {
        limit=*(p-1);   /* itemCount */
        return start;
    } else {
        return URESDATA_ITEM_NOT_FOUND;   /* not found */
    }
}

/* helper for res_load() ---------------------------------------------------- */

static UBool U_CALLCONV
isAcceptable(void *context,
             const char *type, const char *name,
             const UDataInfo *pInfo) {
    return (UBool)(
        pInfo->size>=20 &&
        pInfo->isBigEndian==U_IS_BIG_ENDIAN &&
        pInfo->charsetFamily==U_CHARSET_FAMILY &&
        pInfo->sizeofUChar==U_SIZEOF_UCHAR &&
        pInfo->dataFormat[0]==0x52 &&   /* dataFormat="ResB" */
        pInfo->dataFormat[1]==0x65 &&
        pInfo->dataFormat[2]==0x73 &&
        pInfo->dataFormat[3]==0x42 &&
        pInfo->formatVersion[0]==1);
}

/* semi-public functions ---------------------------------------------------- */

U_CFUNC UBool
res_load(ResourceData *pResData,
         const char *path, const char *name, UErrorCode *errorCode) {
    /* load the ResourceBundle file */
    pResData->data=udata_openChoice(path, "res", name, isAcceptable, NULL, errorCode);
    if(U_FAILURE(*errorCode)) {
        return FALSE;
    }

    /* get its memory and root resource */
    pResData->pRoot=(Resource *)udata_getMemory(pResData->data);
    pResData->rootRes=*pResData->pRoot;

    /* currently, we accept only resources that have a Table as their roots */
    if(RES_GET_TYPE(pResData->rootRes)!=URES_TABLE) {
        udata_close(pResData->data);
        pResData->data=NULL; 
        return FALSE;
    }

    return TRUE;
}

U_CFUNC void
res_unload(ResourceData *pResData) {
    if(pResData->data!=NULL) {
        udata_close(pResData->data);
        pResData->data=NULL;
    }
}

U_CFUNC const UChar *
res_getString(const ResourceData *pResData, const Resource res, int32_t *pLength) {
    if(res!=RES_BOGUS && RES_GET_TYPE(res)==URES_STRING) {
        int32_t *p=(int32_t *)RES_GET_POINTER(pResData->pRoot, res);
        if (pLength) {
            *pLength=*p;
        }
        return (UChar *)++p;
    } else {
        if (pLength) {
            *pLength=0;
        }
        return NULL;
    }
}

U_CFUNC const UChar *
res_getAlias(const ResourceData *pResData, const Resource res, int32_t *pLength) {
    if(res!=RES_BOGUS && RES_GET_TYPE(res)==URES_ALIAS) {
        int32_t *p=(int32_t *)RES_GET_POINTER(pResData->pRoot, res);
        if (pLength) {
            *pLength=*p;
        }
        return (UChar *)++p;
    } else {
        if (pLength) {
            *pLength=0;
        }
        return NULL;
    }
}

U_CFUNC const uint8_t *
res_getBinary(const ResourceData *pResData, const Resource res, int32_t *pLength) {
    if(res!=RES_BOGUS) {
        int32_t *p=(int32_t *)RES_GET_POINTER(pResData->pRoot, res);
        *pLength=*p++;
        if (*pLength == 0) {
            p = NULL;
        }
        return (uint8_t *)p;
    } else {
        *pLength=0;
        return NULL;
    }
}


U_CFUNC const int32_t *
res_getIntVector(const ResourceData *pResData, const Resource res, int32_t *pLength) {
    if(res!=RES_BOGUS && RES_GET_TYPE(res)==URES_INT_VECTOR) {
        int32_t *p=(int32_t *)RES_GET_POINTER(pResData->pRoot, res);
        *pLength=*p++;
        if (*pLength == 0) {
            p = NULL;
        }
        return (const int32_t *)p;
    } else {
        *pLength=0;
        return NULL;
    }
}

U_CFUNC int32_t
res_countArrayItems(const ResourceData *pResData, const Resource res) {
    if(res!=RES_BOGUS) {
        if(RES_GET_TYPE(res)==URES_STRING) {
            return 1;
        } else if(RES_GET_TYPE(res)==URES_ARRAY) {
            Resource *p=RES_GET_POINTER(pResData->pRoot, res);
            int32_t count=*(int32_t *)p;   
            return count;
        } else if(RES_GET_TYPE(res)==URES_TABLE) {
            return res_getTableSize(pResData, res);
        }
    } 
    return 0;
}

U_CFUNC Resource
res_getResource(const ResourceData *pResData, const char *key) {
    return _res_findTableItem(pResData->pRoot, pResData->rootRes, key);
}

U_CFUNC Resource 
res_getArrayItem(const ResourceData *pResData, Resource array, const int32_t indexR) {
    return _res_getArrayItem(pResData->pRoot, array, indexR);
}

U_CFUNC Resource
res_findResource(const ResourceData *pResData, Resource r, const char** path, const char** key) {
  /* we pass in a path. CollationElements/Sequence or zoneStrings/3/2 etc. 
   * iterates over a path and stops when a scalar resource is found. This  
   * CAN be an alias. Path gets set to the part that has not yet been processed. 
   */

  const char *pathP = *path, *nextSepP = *path;
  char *closeIndex = NULL;
  Resource t1 = r;
  Resource t2;
  int32_t indexR = 0, keyLen = 0;
  UResType type = RES_GET_TYPE(t1);
  
  while(nextSepP && *pathP && t1 != RES_BOGUS && (type == URES_TABLE || type == URES_ARRAY)) { 
    /* Iteration stops if: the path has been consumed, we found a non-existing
     * resource (t1 == RES_BOGUS) or we found a scalar resource (including alias)
     */
    nextSepP = uprv_strchr(pathP, RES_PATH_SEPARATOR);
    /* if there are more separators, terminate string 
     * and set path to the remaining part of the string
     */
    if(nextSepP != NULL) {
      keyLen = nextSepP-pathP;
      *path = nextSepP+1;
    } else {
      keyLen = uprv_strlen(pathP);
      *path += keyLen;
    }

    /* if the resource is a table */
    /* try the key based access */
    if(type == URES_TABLE) {
      t2 = _res_findTableItemN(pResData->pRoot, t1, pathP, keyLen, key);
      if(t2 == RES_BOGUS) { 
        /* if we fail to get the resource by key, maybe we got an index */
        indexR = uprv_strtol(pathP, &closeIndex, 10);
        if(closeIndex != pathP) {
          /* if we indeed have an index, try to get the item by index */
          t2 = res_getTableItemByIndex(pResData, t1, indexR, key);
        }
      }
    } else if(type == URES_ARRAY) {
      indexR = uprv_strtol(pathP, &closeIndex, 10);
      if(closeIndex != pathP) {
        t2 = _res_getArrayItem(pResData->pRoot, t1, indexR);
      } else {
        t2 = RES_BOGUS; /* have an array, but don't have a valid index */
      }
      *key = NULL;
    } else { /* can't do much here, except setting t2 to bogus */
      t2 = RES_BOGUS;
    }
    t1 = t2;
    type = RES_GET_TYPE(t1);
    /* position pathP to next resource key/index */
    pathP += keyLen+1;
  }

  return t1;
}

U_CFUNC Resource 
res_getTableItemByKey(const ResourceData *pResData, Resource table, int32_t* indexR, const char* *  key) {
    uint16_t tempIndex;
    if(key != NULL) {
        tempIndex  = _res_findTableIndex(pResData->pRoot, table, *key);
        if(tempIndex != URESDATA_ITEM_NOT_FOUND) {
            *key = _res_getTableKey(pResData->pRoot, table, tempIndex);
            *indexR = tempIndex;
            return _res_getTableItem(pResData->pRoot, table, tempIndex);
        } else {
            return RES_BOGUS;
        }
    } else {
        return RES_BOGUS;
    }
}

U_CFUNC Resource 
res_getTableItemByIndex(const ResourceData *pResData, Resource table, int32_t indexR, const char * * key) {
    if(indexR>-1) {
        if(key != NULL) {
            *key = _res_getTableKey(pResData->pRoot, table, (uint16_t)indexR);
        }
        return _res_getTableItem(pResData->pRoot, table, (uint16_t)indexR);
    } else {
        return RES_BOGUS;
    }
}

U_CFUNC int32_t 
res_getTableSize(const ResourceData *pResData, Resource table) {
    uint16_t *p=(uint16_t *)RES_GET_POINTER(pResData->pRoot, table);
    return *p;
}
