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
#include "unicode/udata.h"
#include "cmemory.h"
#include "cstring.h"
#include "uarrsort.h"
#include "udataswp.h"
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

/* resource bundle swapping ------------------------------------------------- */

/*
 * Need to always enumerate the entire item tree,
 * track the lowest address of any item to use as the limit for char keys[],
 * track the highest address of any item to return the size of the data.
 *
 * We should have thought of storing those in the data...
 * It is possible to extend the data structure by putting additional values
 * in places that are inaccessible by ordinary enumeration of the item tree.
 * For example, additional integers could be stored at the beginning or
 * end of the key strings; this could be indicated by a minor version number,
 * and the data swapping would have to know about these values.
 *
 * The data structure does not forbid keys to be shared, so we must swap
 * all keys once instead of each key when it is referenced.
 *
 * These swapping functions assume that a resource bundle always has a length
 * that is a multiple of 4 bytes.
 * Currently, this is trivially true because genrb writes bundle tree leaves
 * physically first, before their branches, so that the root table with its
 * array of resource items (uint32_t values) is always last.
 */

/* definitions for table sorting ------------------------ */

/*
 * row of a temporary array
 *
 * gets platform-endian key string indexes and sorting indexes;
 * after sorting this array by keys, the actual key/value arrays are permutated
 * according to the sorting indexes
 *
 * TODO if and when we add another table type with 32-bit key string indexes,
 * widen both values here to int32_t's
 */
typedef struct Row {
    uint16_t keyIndex, sortIndex;
} Row;

static int32_t
ures_compareRows(const void *context, const void *left, const void *right) {
    const char *keyChars=(const char *)context;
    return (int32_t)uprv_strcmp(keyChars+((const Row *)left)->keyIndex,
                                keyChars+((const Row *)right)->keyIndex);
}

typedef struct TempTable {
    const char *keyChars;
    Row *rows;
    int32_t *resort;
} TempTable;

enum {
    STACK_ROW_CAPACITY=200
};

/*
 * preflight one resource item and set bottom and top values;
 * length, bottom, and top count Resource item offsets (4 bytes each), not bytes
 */
static void
ures_preflightResource(const UDataSwapper *ds,
                       const Resource *inBundle, int32_t length,
                       Resource res,
                       int32_t *pBottom, int32_t *pTop, int32_t *pMaxTableLength,
                       UErrorCode *pErrorCode) {
    const Resource *p;
    int32_t offset;

    if(res==0 || RES_GET_TYPE(res)==URES_INT) {
        /* empty string or integer, nothing to do */
        return;
    }

    /* all other types use an offset to point to their data */
    offset=(int32_t)RES_GET_OFFSET(res);
    if(0<=length && length<=offset) {
        udata_printError(ds, "ures_preflightResource(res=%08x) resource offset exceeds bundle length %d\n",
                         res, length);
        *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
        return;
    } else if(offset<*pBottom) {
        *pBottom=offset;
    }
    p=inBundle+offset;

    switch(RES_GET_TYPE(res)) {
    case URES_ALIAS:
        /* physically same value layout as string, fall through */
    case URES_STRING:
        /* top=offset+1+(string length +1)/2 rounded up */
        offset+=1+((udata_readInt32(ds, (int32_t)*p)+1)+1)/2;
        break;
    case URES_BINARY:
        /* top=offset+1+(binary length)/4 rounded up */
        offset+=1+(udata_readInt32(ds, (int32_t)*p)+3)/4;
        break;
    case URES_TABLE:
        {
            const uint16_t *pKey;
            Resource item;
            int32_t i, count;

            /* get table item count */
            pKey=(const uint16_t *)p;
            count=ds->readUInt16(*pKey++);
            if(count>*pMaxTableLength) {
                *pMaxTableLength=count;
            }

            /* top=((1+ table item count)/2 rounded up)+(table item count) */
            offset+=((1+count)+1)/2;
            p=inBundle+offset; /* pointer to table resources */
            offset+=count;

            /* recurse */
            if(offset<=length) {
                for(i=0; i<count; ++i) {
                    item=ds->readUInt32(*p++);
                    ures_preflightResource(ds, inBundle, length, item,
                                           pBottom, pTop, pMaxTableLength,
                                           pErrorCode);
                    if(U_FAILURE(*pErrorCode)) {
                        udata_printError(ds, "ures_preflightResource(table res=%08x)[%d].recurse(%08x) failed - %s\n",
                                         res, i, item, u_errorName(*pErrorCode));
                        break;
                    }
                }
            }
        }
        break;
    case URES_ARRAY:
        {
            Resource item;
            int32_t i, count;

            /* top=offset+1+(array length) */
            count=udata_readInt32(ds, (int32_t)*p++);
            offset+=1+count;

            /* recurse */
            if(offset<=length) {
                for(i=0; i<count; ++i) {
                    item=ds->readUInt32(*p++);
                    ures_preflightResource(ds, inBundle, length, item,
                                           pBottom, pTop, pMaxTableLength,
                                           pErrorCode);
                    if(U_FAILURE(*pErrorCode)) {
                        udata_printError(ds, "ures_preflightResource(array res=%08x)[%d].recurse(%08x) failed - %s\n",
                                         res, i, item, u_errorName(*pErrorCode));
                        break;
                    }
                }
            }
        }
        break;
    case URES_INT_VECTOR:
        /* top=offset+1+(vector length) */
        offset+=1+udata_readInt32(ds, (int32_t)*p);
        break;
    default:
        /* also catches RES_BOGUS */
        udata_printError(ds, "ures_preflightResource(res=%08x) unknown resource type\n", res);
        *pErrorCode=U_UNSUPPORTED_ERROR;
        break;
    }

    if(U_FAILURE(*pErrorCode)) {
        /* nothing to do */
    } else if(0<=length && length<offset) {
        udata_printError(ds, "ures_preflightResource(res=%08x) resource limit exceeds bundle length %d\n",
                         res, length);
        *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
    } else if(offset>*pTop) {
        *pTop=offset;
    }
}

/*
 * swap one resource item
 * since preflighting succeeded, we need not check offsets against length any more
 */
static void
ures_swapResource(const UDataSwapper *ds,
                  const Resource *inBundle, Resource *outBundle,
                  Resource res, /* caller swaps res itself */
                  TempTable *pTempTable,
                  UErrorCode *pErrorCode) {
    const Resource *p;
    Resource *q;
    int32_t offset, count;

    if(res==0 || RES_GET_TYPE(res)==URES_INT) {
        /* empty string or integer, nothing to do */
        return;
    }

    /* all other types use an offset to point to their data */
    offset=(int32_t)RES_GET_OFFSET(res);
    p=inBundle+offset;
    q=outBundle+offset;

    switch(RES_GET_TYPE(res)) {
    case URES_ALIAS:
        /* physically same value layout as string, fall through */
    case URES_STRING:
        count=udata_readInt32(ds, (int32_t)*p);
        /* swap length */
        ds->swapArray32(ds, p, 4, q, pErrorCode);
        /* swap each UChar (the terminating NUL would not change) */
        ds->swapArray16(ds, p+1, 2*count, q+1, pErrorCode);
        break;
    case URES_BINARY:
        /* swap length */
        ds->swapArray32(ds, p, 4, q, pErrorCode);
        /* no need to swap or copy bytes - ures_swap() copied them all */
        break;
    case URES_TABLE:
        {
            const uint16_t *pKey;
            uint16_t *qKey;
            Resource item;
            int32_t i, oldIndex;

            /* get table item count */
            pKey=(const uint16_t *)p;
            qKey=(uint16_t *)q;
            count=ds->readUInt16(*pKey);

            /* swap count */
            ds->swapArray16(ds, pKey++, 2, qKey++, pErrorCode);
            if(count==0) {
                break;
            }

            offset+=((1+count)+1)/2;
            p=inBundle+offset; /* pointer to table resources */
            q=outBundle+offset;

            /* recurse */
            for(i=0; i<count; ++i) {
                item=ds->readUInt32(p[i]);
                /*
                 * ### TODO detect a collation binary that is to be swapped via
                 * ds->compareInvChars(ds, keyChars+readUInt16(pKey[i]), "CollationElements")
                 * etc.
                 *
                 * use some UDataSwapFn pointer from somewhere for collation swapping
                 * because the common library cannot directly call into the i18n library
                 */
                ures_swapResource(ds, inBundle, outBundle, item, pTempTable, pErrorCode);
                if(U_FAILURE(*pErrorCode)) {
                    udata_printError(ds, "ures_swapResource(table res=%08x)[%d].recurse(%08x) failed - %s\n",
                                     res, i, item, u_errorName(*pErrorCode));
                    return;
                }
            }

            /*
             * ### TODO optimize
             * After some testing, add a test
             * if(inCharset==outCharset) { only swap keys and items, do not sort; }
             * else { sort/copy/swap/permutate as below; }
             */

            /*
             * We need to sort tables by outCharset key strings because they
             * sort differently for different charset families.
             * ures_swap() already set pTempTable->keyChars appropriately.
             * First we set up a temporary table with the key indexes and
             * sorting indexes and sort that.
             * Then we permutate and copy/swap the actual values.
             */
            for(i=0; i<count; ++i) {
                pTempTable->rows[i].keyIndex=ds->readUInt16(pKey[i]);
                pTempTable->rows[i].sortIndex=(uint16_t)i;
            }
            uprv_sortArray(pTempTable->rows, count, sizeof(Row),
                           ures_compareRows, pTempTable->keyChars,
                           FALSE, pErrorCode);
            if(U_FAILURE(*pErrorCode)) {
                udata_printError(ds, "ures_swapResource(table res=%08x).uprv_sortArray(%d items) failed - %s\n",
                                 res, count, u_errorName(*pErrorCode));
                return;
            }

            /* copy/swap/permutate items */
            if(pKey!=qKey) {
                for(i=0; i<count; ++i) {
                    oldIndex=pTempTable->rows[i].sortIndex;
                    ds->swapArray16(ds, pKey+oldIndex, 2, qKey+i, pErrorCode);
                    ds->swapArray32(ds, p+oldIndex, 4, q+i, pErrorCode);
                }
            } else {
                /*
                 * If we swap in-place, then the permutation must use another
                 * temporary array (pTempTable->resort)
                 * before the results are copied to the outBundle.
                 */
                int32_t *r=pTempTable->resort;
                uint16_t *rKey=(uint16_t *)r;

                for(i=0; i<count; ++i) {
                    oldIndex=pTempTable->rows[i].sortIndex;
                    ds->swapArray16(ds, pKey+oldIndex, 2, rKey+i, pErrorCode);
                }
                uprv_memcpy(qKey, rKey, 2*count);

                for(i=0; i<count; ++i) {
                    oldIndex=pTempTable->rows[i].sortIndex;
                    ds->swapArray32(ds, p+oldIndex, 4, r+i, pErrorCode);
                }
                uprv_memcpy(q, r, 4*count);
            }
        }
        break;
    case URES_ARRAY:
        {
            Resource item;
            int32_t i;

            count=udata_readInt32(ds, (int32_t)*p);
            /* swap length */
            ds->swapArray32(ds, p++, 4, q++, pErrorCode);

            /* recurse */
            for(i=0; i<count; ++i) {
                item=ds->readUInt32(p[i]);
                ures_swapResource(ds, inBundle, outBundle, item, pTempTable, pErrorCode);
                if(U_FAILURE(*pErrorCode)) {
                    udata_printError(ds, "ures_swapResource(array res=%08x)[%d].recurse(%08x) failed - %s\n",
                                     res, i, item, u_errorName(*pErrorCode));
                    return;
                }
            }

            /* swap items */
            ds->swapArray32(ds, p, 4*count, q, pErrorCode);
        }
        break;
    case URES_INT_VECTOR:
        count=udata_readInt32(ds, (int32_t)*p);
        /* swap length and each integer */
        ds->swapArray32(ds, p, 4*(1+count), q, pErrorCode);
        break;
    default:
        /* also catches RES_BOGUS */
        *pErrorCode=U_UNSUPPORTED_ERROR;
        break;
    }
}

U_CAPI int32_t U_EXPORT2
ures_swap(const UDataSwapper *ds,
          const void *inData, int32_t length, void *outData,
          UErrorCode *pErrorCode) {
    const UDataInfo *pInfo;
    const Resource *inBundle;
    Resource rootRes;
    int32_t headerSize, maxTableLength;

    Row rows[STACK_ROW_CAPACITY];
    int32_t resort[STACK_ROW_CAPACITY];
    TempTable tempTable;

    /* the following integers count Resource item offsets (4 bytes each), not bytes */
    int32_t bundleLength, bottom, top;

    /* udata_swapDataHeader checks the arguments */
    headerSize=udata_swapDataHeader(ds, inData, length, outData, pErrorCode);
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return 0;
    }

    /* check data format and format version */
    pInfo=(const UDataInfo *)((const char *)inData+4);
    if(!(
        pInfo->dataFormat[0]==0x52 &&   /* dataFormat="ResB" */
        pInfo->dataFormat[1]==0x65 &&
        pInfo->dataFormat[2]==0x73 &&
        pInfo->dataFormat[3]==0x42 &&
        pInfo->formatVersion[0]==1
    )) {
        udata_printError(ds, "ures_swap(): data format %02x.%02x.%02x.%02x (format version %02x) is not a resource bundle\n",
                         pInfo->dataFormat[0], pInfo->dataFormat[1],
                         pInfo->dataFormat[2], pInfo->dataFormat[3],
                         pInfo->formatVersion[0]);
        *pErrorCode=U_UNSUPPORTED_ERROR;
        return 0;
    }

    /* a resource bundle must contain at least one resource item */
    if(length<0) {
        bundleLength=-1;
    } else {
        bundleLength=(length-headerSize)/4;
        if(bundleLength<1) {
            udata_printError(ds, "ures_swap(): too few bytes (%d after header) for a resource bundle\n",
                             length-headerSize);
            *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
            return 0;
        }
    }

    /* preflight to get the bottom, top and maxTableLength values */
    inBundle=(const Resource *)((const char *)inData+headerSize);
    bottom=0x7fffffff;
    top=maxTableLength=0;
    rootRes=*inBundle;
    ures_preflightResource(ds, inBundle, bundleLength, rootRes,
                           &bottom, &top, &maxTableLength,
                           pErrorCode);
    if(U_FAILURE(*pErrorCode)) {
        udata_printError(ds, "ures_preflightResource(root res=%08x) failed - %s\n",
                         rootRes, u_errorName(*pErrorCode));
        return 0;
    }

    if(length>=0) {
        Resource *outBundle=(Resource *)((char *)outData+headerSize);

        /* copy the bundle for binary and inaccessible data */
        if(inData!=outData) {
            uprv_memcpy(outBundle, inBundle, 4*top);
        }

        /* swap the key strings */
        ds->swapInvChars(ds, inBundle+1, 4*(bottom-1), outBundle+1, pErrorCode);

        /* allocate the temporary table for sorting resource tables */
        tempTable.keyChars=(const char *)outBundle; /* sort by outCharset */
        if(maxTableLength<=STACK_ROW_CAPACITY) {
            tempTable.rows=rows;
            tempTable.resort=resort;
        } else {
            tempTable.rows=(Row *)uprv_malloc(maxTableLength*sizeof(Row)+maxTableLength*4);
            if(tempTable.rows==NULL) {
                udata_printError(ds, "ures_swap(): unable to allocate memory for sorting tables (max length: %d)\n",
                                 maxTableLength);
                *pErrorCode=U_MEMORY_ALLOCATION_ERROR;
                return 0;
            }
            tempTable.resort=(int32_t *)(tempTable.rows+maxTableLength);
        }

        /* swap the resources */
        ures_swapResource(ds, inBundle, outBundle, rootRes, &tempTable, pErrorCode);
        if(U_FAILURE(*pErrorCode)) {
            udata_printError(ds, "ures_swapResource(root res=%08x) failed - %s\n",
                             rootRes, u_errorName(*pErrorCode));
        }

        if(tempTable.rows!=rows) {
            uprv_free(tempTable.rows);
        }

        /* swap the root resource */
        ds->swapArray32(ds, inBundle, 4, outBundle, pErrorCode);
    }

    return headerSize+4*top;
}
