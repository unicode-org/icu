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
#include "unicode/udata.h"
#include "uresdata.h"

/*
 * Resource access helpers
 */

/* get a const char* pointer to the key with the keyOffset byte offset from pRoot */
#define RES_GET_KEY(pRoot, keyOffset) ((const char *)(pRoot)+(keyOffset))

/* get signed and unsigned integer values directly from the Resource handle */
#define RES_GET_INT(res) (((int32_t)((res)<<4L))>>4L)
#define RES_GET_UINT(res) ((res)&0xfffffff)

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
        int32_t *p=(int32_t *)(pRoot+res);
        *pLength=*p++;
        return (UChar *)p;
    }
}

/*
 * Array functions
 */
static Resource
_res_getArrayItem(Resource *pRoot, Resource res, int32_t index) {
    int32_t *p=(int32_t *)RES_GET_POINTER(pRoot, res);
    if(index<*p) {
        return ((Resource *)(p))[1+index];
    } else {
        return RES_BOGUS;   /* index>itemCount */
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
_res_getTableKey(Resource *pRoot, Resource res, uint16_t index) {
    uint16_t *p=(uint16_t *)RES_GET_POINTER(pRoot, res);
    if(index<*p) {
        return RES_GET_KEY(pRoot, p[index+1]);
    } else {
        return NULL;    /* index>itemCount */
    }
}

static Resource
_res_getTableItem(Resource *pRoot, Resource res, uint16_t index) {
    uint16_t *p=(uint16_t *)RES_GET_POINTER(pRoot, res);
    uint16_t count=*p;
    if(index<count) {
        return ((Resource *)(p+1+count+(~count&1)))[index];
    } else {
        return RES_BOGUS;   /* index>itemCount */
    }
}

static Resource
_res_findTableItem(Resource *pRoot, Resource res, const char *key) {
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

/* helper for res_load() ---------------------------------------------------- */

static bool_t
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

U_CFUNC bool_t
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
res_getString(ResourceData *pResData, const char *key, int32_t *pLength) {
    Resource res=_res_findTableItem(pResData->pRoot, pResData->rootRes, key);
    if(res!=RES_BOGUS && RES_GET_TYPE(res)==RES_STRING) {
        return _res_getString(pResData->pRoot, res, pLength);
    } else {
        *pLength=0;
        return NULL;
    }
}

U_CFUNC Resource
res_getStringArray(ResourceData *pResData, const char *key, int32_t *pCount) {
    Resource res=_res_findTableItem(pResData->pRoot, pResData->rootRes, key);
    if(res!=RES_BOGUS && RES_GET_TYPE(res)==RES_ARRAY) {
        Resource *p=RES_GET_POINTER(pResData->pRoot, res);
        int32_t count=*(int32_t *)p;
        *pCount=count;
        
        /* check to make sure all items are strings */
        while(count>0) {
            if(RES_GET_TYPE(*++p)!=RES_STRING) {
                *pCount=0;
                return RES_BOGUS;
            }
            --count;
        }
        return res;
    } else {
        *pCount=0;
        return RES_BOGUS;
    }
}

U_CFUNC const UChar *
res_getStringArrayItem(ResourceData *pResData,
                       Resource arrayRes, int32_t index, int32_t *pLength) {
    return _res_getString(pResData->pRoot,
                          _res_getArrayItem(pResData->pRoot, arrayRes, index),
                          pLength);
}
