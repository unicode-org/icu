/*
********************************************************************************
*                                                                              *
* COPYRIGHT:                                                                   *
*   (C) Copyright International Business Machines Corporation, 1998, 1999      *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.         *
*   US Government Users Restricted Rights - Use, duplication, or disclosure    *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                     *
*                                                                              *
********************************************************************************
*
*
*  ucnv_io.c:
*  initializes global variables and defines functions pertaining to file access,
*  and name resolution aspect of the library.
*
*   new implementation:
*
*   created on: 1999nov22
*   created by: Markus W. Scherer
*
*   Use the binary cnvalias.dat (created from convrtrs.txt) to work
*   with aliases for converter names.
********************************************************************************
*/

#include "utypes.h"
#include "umutex.h"
#include "cstring.h"
#include "ucnv_io.h"
#include "udata.h"

/* Format of cnvalias.dat ------------------------------------------------------
 *
 * cnvalias.dat is a binary, memory-mappable form of convrtrs.txt .
 * It contains two sorted tables and a block of zero-terminated strings.
 * Each table is preceded by the number of table entries.
 *
 * The first table maps from aliases to converter indexes.
 * The converter names themselves are listed as aliases in this table.
 * Each entry in this table has an offset to the alias and
 * an index of the converter in the converter table.
 *
 * The second table lists only the converters themselves.
 * Each entry in this table has an offset to the converter name and
 * the number of aliases, including the converter itself.
 * A count of 1 means that there is no alias, only the converter name.
 *
 * In the block of strings after the tables, each converter name is directly
 * followed by its aliases. All offsets to strings are offsets from the
 * beginning of the data.
 *
 * More formal file data structure (data format 1.0):
 *
 * uint16_t aliasCount;
 * struct {
 *     uint16_t aliasOffset;
 *     uint16_t converterIndex;
 * } aliases[aliasCount];
 *
 * uint16_t converterCount;
 * struct {
 *     uint16_t converterOffset;
 *     uint16_t aliasCount;
 * } converters[converterCount];
 *
 * char strings[]={
 *     "Converter0\0Alias1\0Alias2\0...Converter1\0Converter2\0Alias0\Alias1\0..."
 * };
 */

#define DATA_NAME "cnvalias"
#define DATA_TYPE "dat"

static UDataMemory *aliasData=NULL;
static const uint16_t *aliasTable=NULL;

static bool_t
isAcceptable(void *context,
             const char *type, const char *name,
             UDataInfo *pInfo) {
    return
        pInfo->size>=20 &&
        pInfo->isBigEndian==U_IS_BIG_ENDIAN &&
        pInfo->charsetFamily==U_CHARSET_FAMILY &&
        pInfo->dataFormat[0]==0x43 &&   /* dataFormat="CvAl" */
        pInfo->dataFormat[1]==0x76 &&
        pInfo->dataFormat[2]==0x41 &&
        pInfo->dataFormat[3]==0x6c &&
        pInfo->formatVersion[0]==1;
}

static bool_t
haveAliasData(UErrorCode *pErrorCode) {
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return FALSE;
    }

    /* load converter alias data from file if necessary */
    if(aliasData==NULL) {
        UDataMemory *data;
        const uint16_t *table=NULL;

        /* open the data outside the mutex block */
        data=udata_openChoice(NULL, DATA_TYPE, DATA_NAME, isAcceptable, NULL, pErrorCode);
        if(U_FAILURE(*pErrorCode)) {
            return FALSE;
        }

        table=(const uint16_t *)udata_getMemory(data);

        /* in the mutex block, set the data for this process */
        umtx_lock(NULL);
        if(aliasData==NULL) {
            aliasData=data;
            data=NULL;
            aliasTable=table;
            table=NULL;
        }
        umtx_unlock(NULL);

        /* if a different thread set it first, then close the extra data */
        if(data!=NULL) {
            udata_close(data); /* NULL if it was set correctly */
        }
    }

    return TRUE;
}

static bool_t
isAlias(const char *alias, UErrorCode *pErrorCode) {
    if(alias==NULL) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return FALSE;
    } else if(*alias==0) {
        return FALSE;
    } else {
        return TRUE;
    }
}

/* compare lowercase str1 with mixed-case str2, ignoring case */
static int
strHalfCaseCmp(const char *str1, const char *str2) {
    /* compare non-NULL strings lexically with lowercase */
    int rc;
    unsigned char c1, c2;

    for(;;) {
        c1=(unsigned char)*str1;
        c2=(unsigned char)*str2;
        if(c1==0) {
            if(c2==0) {
                return 0;
            } else {
                return -1;
            }
        } else if(c2==0) {
            return 1;
        } else {
            /* compare non-zero characters with lowercase */
            rc=(int)c1-(int)(unsigned char)icu_tolower(c2);
            if(rc!=0) {
                return rc;
            }
        }
        ++str1;
        ++str2;
    }
}

/*
 * search for an alias
 * return NULL or a pointer to the converter table entry
 */
static const uint16_t *
findAlias(const char *alias) {
    char name[100];
    const uint16_t *p=aliasTable;
    uint16_t i, start, limit;

    limit=*p++;
    if(limit==0) {
        /* there are no aliases */
        return NULL;
    }

    /* convert the alias name to lowercase to do case-insensitive comparisons */
    for(i=0; i<sizeof(name)-1 && *alias!=0; ++i) {
        name[i]=icu_tolower(*alias++);
    }
    name[i]=0;

    /* do a binary search for the alias */
    start=0;
    while(start<limit-1) {
        i=(start+limit)/2;
        if(strHalfCaseCmp(name, (const char *)aliasTable+p[2*i])<0) {
            limit=i;
        } else {
            start=i;
        }
    }

    /* did we really find it? */
    if(icu_strcmp(name, (const char *)aliasTable+p[2*start])==0) {
        /*
         * turn p back to aliasTable and
         * skip the entire alias table, both table counts,
         * and point into the converter table
         */
        i=p[2*start+1]; /* converter index */
        --p;
        return p+2*(*p+1+i);
    } else {
        return NULL;
    }
}

U_CFUNC const char *
ucnv_io_getConverterName(const char *alias, UErrorCode *pErrorCode) {
    if(haveAliasData(pErrorCode) && isAlias(alias, pErrorCode)) {
        const uint16_t *p=findAlias(alias);
        if(p!=NULL) {
            return (const char *)aliasTable+*p;
        }
    }
    return NULL;
}

U_CFUNC uint16_t
ucnv_io_getAliases(const char *alias, const char **aliases, UErrorCode *pErrorCode) {
    if(haveAliasData(pErrorCode) && isAlias(alias, pErrorCode)) {
        const uint16_t *p=findAlias(alias);
        if(p!=NULL) {
            *aliases=(const char *)aliasTable+*p;
            return *(p+1);
        }
    }
    return 0;
}

U_CFUNC const char *
ucnv_io_getAlias(const char *alias, uint16_t index, UErrorCode *pErrorCode) {
    if(haveAliasData(pErrorCode) && isAlias(alias, pErrorCode)) {
        const uint16_t *p=findAlias(alias);
        if(p!=NULL) {
            uint16_t count=*(p+1);
            if(index<count) {
                const char *aliases=(const char *)aliasTable+*p;
                while(index>0) {
                    /* skip a name, first the canonical converter name */
                    aliases+=icu_strlen(aliases)+1;
                    --index;
                }
                return aliases;
            }
        }
    }
    return NULL;
}

U_CFUNC uint16_t
ucnv_io_countAvailableAliases(UErrorCode *pErrorCode) {
    if(haveAliasData(pErrorCode)) {
        return *aliasTable;
    }
    return 0;
}

U_CFUNC const char *
ucnv_io_getAvailableAlias(uint16_t index, UErrorCode *pErrorCode) {
    if(haveAliasData(pErrorCode) && index<*aliasTable) {
        return (const char *)aliasTable+*(aliasTable+1+2*index);
    }
    return NULL;
}

U_CFUNC void
ucnv_io_fillAvailableAliases(const char **aliases, UErrorCode *pErrorCode) {
    if(haveAliasData(pErrorCode)) {
        const uint16_t *p=aliasTable;
        uint16_t count=*p++;
        while(count>0) {
            *aliases++=(const char *)aliasTable+*p;
            p+=2;
            --count;
        }
    }
}
