/*
*******************************************************************************
*                                                                             *
* Copyright (C) 1999, International Business Machines Corporation and others. *
*                     All Rights Reserved.                                    *
*                                                                             *
*******************************************************************************
*   file name:  uresdata.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 1999dec08
*   created by: Markus W. Scherer
*/

#include "unicode/utypes.h"
#include "cstring.h"
#include "cmemory.h"
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
 * Unicode String functions
 *
 * Note that the type value for strings is 0, therefore
 * res itself contains the offset value.
 */ 
static const UChar nulUChar=0;

static const UChar *
_res_getString(Resource *pRoot, Resource res, int32_t *pLength) {
    if(res==0) {
        *pLength=0;
        return &nulUChar;
    } else {
        int32_t *p=(int32_t *)RES_GET_POINTER(pRoot, res);
        *pLength=*p++;
        return (UChar *)p;
    }
}

static const uint8_t *
_res_getBinary(Resource *pRoot, Resource res, int32_t *pLength) {
    if(res==0) {
        *pLength=0;
        return NULL;
    } else {
        int32_t *p=(int32_t *)RES_GET_POINTER(pRoot, res);
        *pLength=*p++;
        return (uint8_t *)p;
    }
}

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

    /* do a binary search for the key */
    start=0;
    while(start<limit-1) {
        i=(start+limit)/2;
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

static uint16_t
_res_findTableIndex(const Resource *pRoot, const Resource res, const char *key) {
    uint16_t *p=(uint16_t *)RES_GET_POINTER(pRoot, res);
    uint16_t i, start, limit;

    limit=*p++; /* number of entries */

    /* do a binary search for the key */
    start=0;
    while(start<limit-1) {
        i=(start+limit)/2;
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

static UBool
_res_isStringArray(Resource *r) {
        int32_t count=*(int32_t *)r;
        
        /* check to make sure all items are strings */
        while(count>0) {
            if(RES_GET_TYPE(*++r)!=RES_STRING) {
                return FALSE;
            }
            --count;
        }
        return TRUE;
}

/* helper for res_load() ---------------------------------------------------- */

static UBool
isAcceptable(void *context,
             const char *type, const char *name,
             const UDataInfo *pInfo) {
    return
        pInfo->size>=20 &&
        pInfo->isBigEndian==U_IS_BIG_ENDIAN &&
        pInfo->charsetFamily==U_CHARSET_FAMILY &&
        pInfo->sizeofUChar==U_SIZEOF_UCHAR &&
        pInfo->dataFormat[0]==0x52 &&   /* dataFormat="ResB" */
        pInfo->dataFormat[1]==0x65 &&
        pInfo->dataFormat[2]==0x73 &&
        pInfo->dataFormat[3]==0x42 &&
        pInfo->formatVersion[0]==1;
}

/* semi-public functions ---------------------------------------------------- */

U_CFUNC UBool
res_load(ResourceData *pResData,
         const char *path, const char *name, UErrorCode *errorCode) {
    if(errorCode==NULL || U_FAILURE(*errorCode)) {
        return FALSE;
    }

    /* load the ResourceBundle file */
    pResData->data=udata_openChoice(path, "res", name, isAcceptable, NULL, errorCode);
    if(U_FAILURE(*errorCode)) {
        return FALSE;
    }

    /* get its memory and root resource */
    pResData->pRoot=(Resource *)udata_getMemory(pResData->data);
    pResData->rootRes=*pResData->pRoot;

    /* currently, we accept only resources that have a Table as their roots */
    if(RES_GET_TYPE(pResData->rootRes)!=RES_TABLE) {
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
/*res_getString(const ResourceData *pResData, const char *key, int32_t *pLength) {
  Resource res=_res_findTableItem(pResData->pRoot, pResData->rootRes, key);*/
res_getString(const ResourceData *pResData, const Resource res, int32_t *pLength) {
    if(res!=RES_BOGUS && RES_GET_TYPE(res)==RES_STRING) {
        return _res_getString(pResData->pRoot, res, pLength);
    } else {
        *pLength=0;
        return NULL;
    }
}

U_CFUNC const uint8_t *
res_getBinary(const ResourceData *pResData, const Resource res, int32_t *pLength) {
    if(res!=RES_BOGUS && RES_GET_TYPE(res)==RES_BINARY) {
        return _res_getBinary(pResData->pRoot, res, pLength);
    } else {
        *pLength=0;
        return NULL;
    }
}

U_CFUNC Resource
res_getStringArray(const ResourceData *pResData, const char *key, int32_t *pCount) {
    Resource res=_res_findTableItem(pResData->pRoot, pResData->rootRes, key);
    if(res!=RES_BOGUS && RES_GET_TYPE(res)==RES_ARRAY) {
        Resource *p=RES_GET_POINTER(pResData->pRoot, res);
        int32_t count=*(int32_t *)p;
        *pCount=count;
        
        /* check to make sure all items are strings */
        if(!_res_isStringArray(p)) {
                *pCount=0;
                return RES_BOGUS;
        }
        return res;
    } else {
        *pCount=0;
        return RES_BOGUS;
    }
}


U_CFUNC int32_t
res_countArrayItems(const ResourceData *pResData, const Resource res) {
  /*Resource res=_res_findTableItem(pResData->pRoot, pResData->rootRes, key);*/
    if(res!=RES_BOGUS) {
        if(RES_GET_TYPE(res)==RES_STRING) {
            return 1;
        } else if(RES_GET_TYPE(res)==RES_ARRAY) {
            Resource *p=RES_GET_POINTER(pResData->pRoot, res);
            int32_t count=*(int32_t *)p;   
            return count;
        } else if(RES_GET_TYPE(res)==RES_TABLE) {
            return res_getTableSize(pResData, res);
        }
    } 
    return 0;
}

U_CFUNC int32_t
res_count2dArrayCols(const ResourceData *pResData, const Resource res) {
  /*Resource res=_res_findTableItem(pResData->pRoot, pResData->rootRes, key);*/
    if(res!=RES_BOGUS) {
        if(RES_GET_TYPE(res)==RES_ARRAY) {
            Resource *p=RES_GET_POINTER(pResData->pRoot, res);
            int32_t count = *(int32_t *)RES_GET_POINTER(pResData->pRoot, *(p+1)); /*Number of columns in the first row*/
            return count;
        } 
    } 
    return 0;
}

U_CFUNC Resource
res_get2DStringArray(const ResourceData *pResData, const char *key, int32_t *pRows, int32_t *pCols) {
    Resource res=_res_findTableItem(pResData->pRoot, pResData->rootRes, key);
    if(res!=RES_BOGUS && RES_GET_TYPE(res)==RES_ARRAY) {
        Resource *p=RES_GET_POINTER(pResData->pRoot, res);
        Resource *row=NULL;
        int32_t row_count=*(int32_t *)p;
        *pRows = row_count;

        *pCols = *(int32_t *)RES_GET_POINTER(pResData->pRoot, *(p+1)); /*Number of columns in the first row*/
        
        /* check to make sure all items are strings */
        while(row_count>0) {
            row = RES_GET_POINTER(pResData->pRoot, *(++p));
            if(!_res_isStringArray(row) || RES_GET_TYPE(*p)!=RES_ARRAY) {
                *pRows=0;
                *pCols=0;
                return RES_BOGUS;
            } else {
                int32_t col_count=*(int32_t *)(row);
                if(*pCols != col_count) {
                    *pRows=0;
                    *pCols=0;
                    return RES_BOGUS;
                }
            }
            --row_count;
        }
        return res;
    } else {
        *pRows=0;
        *pCols=0;
        return RES_BOGUS;
    }
}

U_CFUNC Resource
res_getResource(const ResourceData *pResData, const char *key) {
    return _res_findTableItem(pResData->pRoot, pResData->rootRes, key);
}

U_CFUNC Resource
res_getTable(const ResourceData *pResData, const char *key) {
    return _res_findTableItem(pResData->pRoot, pResData->rootRes, key);
}

U_CFUNC Resource res_getArrayItem(const ResourceData *pResData, const Resource array, const int32_t indexR) {
    return _res_getArrayItem(pResData->pRoot, array, indexR);
}

U_CFUNC Resource res_getTableItemByKey(const ResourceData *pResData, const Resource table, int32_t* indexR, const char* *  key) {
  uint16_t tempIndex = 0;
    if(key != NULL) {
        tempIndex  = _res_findTableIndex(pResData->pRoot, table, *key);
	if(tempIndex != URESDATA_ITEM_NOT_FOUND) {
	  *key = _res_getTableKey(pResData->pRoot, table, tempIndex);
	  return _res_getTableItem(pResData->pRoot, table, tempIndex);
          *indexR = tempIndex;
	} else {
	  return RES_BOGUS;
	}
        /*return _res_findTableItem(pResData->pRoot, table, key);*/
    } else {
        return RES_BOGUS;
    }
}

U_CFUNC Resource res_getTableItemByIndex(const ResourceData *pResData, const Resource table, int32_t indexR, const char * * key) {
    if(indexR>-1) {
        if(key != NULL) {
            *key = _res_getTableKey(pResData->pRoot, table, (uint16_t)indexR);
        }
        return _res_getTableItem(pResData->pRoot, table, (uint16_t)indexR);
    } else {
        return RES_BOGUS;
    }
}

U_CFUNC int32_t res_getTableSize(const ResourceData *pResData, Resource table) {
    uint16_t *p=(uint16_t *)RES_GET_POINTER(pResData->pRoot, table);
    return *p;
}

U_CFUNC void
res_getNextStringTableItem(const ResourceData *pResData, Resource table, const UChar **value, const char **key, int32_t *len, int16_t *indexS) {
    Resource next;
    if(*indexS == -1) {
        *indexS = 0;
    }
    next = _res_getTableItem(pResData->pRoot, table, *indexS);
    if ((next == RES_BOGUS) || (RES_GET_TYPE(next) != RES_STRING)) {
        *key = NULL;
        *value = NULL;
        len = 0;
        return;
    }
    *key = _res_getTableKey(pResData->pRoot, table, *indexS);
    (*indexS)++;
    *value = _res_getString(pResData->pRoot, next, len);
}

U_CFUNC const UChar *
res_getStringTableItem(const ResourceData *pResData, Resource table, const char *key, int32_t *len) {
    Resource res = _res_findTableItem(pResData->pRoot, table, key);
    if(RES_GET_TYPE(res) != RES_STRING) {
        return NULL;
    }

    return _res_getString(pResData->pRoot, res, len);

}


U_CFUNC const UChar *
res_get2DStringArrayItem(const ResourceData *pResData,
                       Resource arrayRes, int32_t row, int32_t col, int32_t *pLength) {
    Resource res = _res_getArrayItem(pResData->pRoot, arrayRes, row);
    return _res_getString(pResData->pRoot,
                          _res_getArrayItem(pResData->pRoot, res, col),
                          pLength);
}

U_CFUNC const UChar *
res_getStringArrayItem(const ResourceData *pResData,
                       Resource arrayRes, int32_t indexS, int32_t *pLength) {
    return _res_getString(pResData->pRoot,
                          _res_getArrayItem(pResData->pRoot, arrayRes, indexS),
                          pLength);
}

U_CFUNC const char *
res_getVersion(const ResourceData *pResData) {
    UDataInfo *info;

    info = uprv_malloc(sizeof(UDataInfo));
    uprv_memset(info, 0, sizeof(UDataInfo));

    if(info == NULL) {
        return NULL;
    }

    udata_getInfo(pResData->data, info);

    uprv_free(info);

    return NULL;
}

