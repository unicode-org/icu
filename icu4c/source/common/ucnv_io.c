/*
*******************************************************************************
*
*   Copyright (C) 1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
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

#include "unicode/utypes.h"
#include "umutex.h"
#include "cstring.h"
#include "cmemory.h"
#include "ucnv_io.h"
#include "unicode/udata.h"

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
 * More formal file data structure (data format 2.1):
 *
 * uint16_t aliasCount;
 * uint16_t aliasOffsets[aliasCount];
 * uint16_t converterIndexes[aliasCount];
 *
 * uint16_t converterCount;
 * struct {
 *     uint16_t converterOffset;
 *     uint16_t aliasCount;
 * } converters[converterCount];
 *
 * uint16_t tagCount;
 * uint16_t taggedAliasesOffsets[tagCount][converterCount];
 * char tags[] = { "Tag0\Tag1\0..." };
 *
 * char strings[]={
 *     "Converter0\0Alias1\0Alias2\0...Converter1\0Converter2\0Alias0\Alias1\0..."
 * };
 *
 * The code included here can read versions 2 and 2.1 of the data format.
 * Version 2 does not have tag information, but since the code never refers
 * to strings[] by its base offset, it's okay.
 *
 */

#define DATA_NAME "cnvalias"
#define DATA_TYPE "dat"

static UDataMemory *aliasData=NULL;
static const uint16_t *aliasTable=NULL;

static const uint16_t *converterTable = NULL;
static const uint16_t *tagTable = NULL;

static UBool
isAcceptable(void *context,
             const char *type, const char *name,
             const UDataInfo *pInfo) {
    return (UBool)(
        pInfo->size>=20 &&
        pInfo->isBigEndian==U_IS_BIG_ENDIAN &&
        pInfo->charsetFamily==U_CHARSET_FAMILY &&
        pInfo->dataFormat[0]==0x43 &&   /* dataFormat="CvAl" */
        pInfo->dataFormat[1]==0x76 &&
        pInfo->dataFormat[2]==0x41 &&
        pInfo->dataFormat[3]==0x6c &&
        pInfo->formatVersion[0]>1);
}

static UBool
haveAliasData(UErrorCode *pErrorCode) {
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return FALSE;
    }

    /* load converter alias data from file if necessary */
    if(aliasData==NULL) {
        UDataMemory *data;
        UDataInfo info;
        const uint16_t *table=NULL;

        /* open the data outside the mutex block */
        data=udata_openChoice(NULL, DATA_TYPE, DATA_NAME, isAcceptable, NULL, pErrorCode);
        if(U_FAILURE(*pErrorCode)) {
            return FALSE;
        }

        table=(const uint16_t *)udata_getMemory(data);
        udata_getInfo(data, &info);

        /* in the mutex block, set the data for this process */
        umtx_lock(NULL);
        if(aliasData==NULL) {
            aliasData=data;
            data=NULL;
            aliasTable=table;
            table=NULL;
            converterTable = aliasTable + 1 + 2 * *aliasTable;

            if (info.formatVersion[0] > 2) {
                tagTable = converterTable + 1 + 2 * *converterTable;
            }
        }
        umtx_unlock(NULL);

        /* if a different thread set it first, then close the extra data */
        if(data!=NULL) {
            udata_close(data); /* NULL if it was set correctly */
        }
    }

    return TRUE;
}

static UBool
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

static int16_t getTagNumber(const char *tagname) {
    if (tagTable) {
        int16_t tag, count = (int16_t) *tagTable;
        const char *tags = (const char *) (tagTable + 1 + count * *converterTable);

        char name[100];
        int i;

        /* convert the tag name to lowercase to do case-insensitive comparisons */
        for(i = 0; i < sizeof(name) - 1 && *tagname; ++i) {
            name[i] = (char)uprv_tolower(*tagname++);
        }
        name[i] = 0;

        for (tag = 0; count--; ++tag) {
            if (!charsetNameCmp(name, tags)) {
                return tag;
            }
            tags += strlen(tags);
        }
    }

    return -1;
}

/**
 * Do a fuzzy compare of a two converter/alias names.  The comparison
 * is case-insensitive.  It also ignores the characters '-', '_', and
 * ' ' (dash, underscore, and space).  Thus the strings "UTF-8",
 * "utf_8", and "Utf 8" are exactly equivalent.
 * 
 * This is a symmetrical (commutative) operation; order of arguments
 * is insignificant.  This is an important property for sorting the
 * list (when the list is preprocessed into binary form) and for
 * performing binary searches on it at run time.
 * 
 * @param name1 a converter name or alias, zero-terminated
 * @param name2 a converter name or alias, zero-terminated
 * @return 0 if the names match, or a negative value if the name1
 * lexically precedes name2, or a positive value if the name1
 * lexically follows name2.
 */
U_CFUNC int
charsetNameCmp(const char *name1, const char *name2) {
    int rc;
    unsigned char c1, c2;

    for (;;) {
        /* Ignore delimiters '-', '_', and ' ' */
        while ((c1 = (unsigned char)*name1) == '-'
               || c1 == '_' || c1 == ' ') ++name1;
        while ((c2 = (unsigned char)*name2) == '-'
               || c2 == '_' || c2 == ' ') ++name2;

        /* If we reach the ends of both strings then they match */
        if ((c1|c2)==0) {
            return 0;
        }
        
        /* Case-insensitive comparison */
        rc = (int)(unsigned char)uprv_tolower(c1) -
             (int)(unsigned char)uprv_tolower(c2);
        if (rc!=0) {
            return rc;
        }
        ++name1;
        ++name2;
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
        name[i]=(char)uprv_tolower(*alias++);
    }
    name[i]=0;

    /* do a binary search for the alias */
    start=0;
    while(start<limit-1) {
        i=(uint16_t)((start+limit)/2);
        if(charsetNameCmp(name, (const char *)aliasTable+p[i])<0) {
            limit=i;
        } else {
            start=i;
        }
    }

    /* did we really find it? */
    if(charsetNameCmp(name, (const char *)aliasTable+p[start])==0) {
        limit=*(p-1);       /* aliasCount */
        p+=limit;           /* advance to the second column of the alias table */
        i=p[start];         /* converter index */
        return
            p+limit+        /* beginning of converter table */
            1+              /* skip its count */
            2*i;            /* go to this converter's entry and return a pointer to it */
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
ucnv_io_getAlias(const char *alias, uint16_t n, UErrorCode *pErrorCode) {
    if(haveAliasData(pErrorCode) && isAlias(alias, pErrorCode)) {
        const uint16_t *p=findAlias(alias);
        if(p!=NULL) {
            uint16_t count=*(p+1);
            if(n<count) {
                const char *aliases=(const char *)aliasTable+*p;
                while(n>0) {
                    /* skip a name, first the canonical converter name */
                    aliases+=uprv_strlen(aliases)+1;
                    --n;
                }
                return aliases;
            }
        }
    }
    return NULL;
}

U_CFUNC uint16_t
ucnv_io_countStandards(UErrorCode *pErrorCode) {
    if (haveAliasData(pErrorCode)) {
        if (!tagTable) {
            *pErrorCode = U_INVALID_FORMAT_ERROR;
            return 0;
        }

        return *tagTable;
    }

    return 0;
}

U_CFUNC const char *
ucnv_io_getStandard(uint16_t n, UErrorCode *pErrorCode) {
    if (haveAliasData(pErrorCode) && tagTable) {
        const char *p = (const char *) tagTable + 1 + *tagTable * *converterTable;
        int16_t count = (int16_t) *tagTable;

        while (n-- && count--) {
            p += strlen(p);
        }

        return count >= 0 ? p : NULL;
    }

    return NULL;
}

U_CFUNC const char *
ucnv_io_getStandardName(const char *alias, const char *standard, UErrorCode *pErrorCode) {
    if (haveAliasData(pErrorCode) && isAlias(alias, pErrorCode)) {
        const uint16_t *p = findAlias(alias);
        if(p != NULL) {
            int16_t tag = getTagNumber(standard);

            if (tag > -1) {
                uint16_t offset = tagTable[1 + tag * *converterTable + (p - converterTable) / 2];
                return offset ? (const char *) aliasTable + offset : NULL;
            }
        }
    }

   return NULL;
}

U_CFUNC uint16_t
ucnv_io_countAvailableConverters(UErrorCode *pErrorCode) {
    if(haveAliasData(pErrorCode)) {
        return *converterTable;
    }
    return 0;
}

U_CFUNC const char *
ucnv_io_getAvailableConverter(uint16_t n, UErrorCode *pErrorCode) {
    if(haveAliasData(pErrorCode)) {
        const uint16_t *p=converterTable;
        if(n<*p) {
            return (const char *)aliasTable+p[1+2*n];
        }
    }
    return NULL;
}

U_CFUNC void
ucnv_io_fillAvailableConverters(const char **aliases, UErrorCode *pErrorCode) {
    if(haveAliasData(pErrorCode)) {
        const uint16_t *p=converterTable;
        uint16_t count=*p++;
        while(count>0) {
            *aliases++=(const char *)aliasTable+*p;
            p+=2;
            --count;
        }
    }
}

U_CFUNC uint16_t
ucnv_io_countAvailableAliases(UErrorCode *pErrorCode) {
    if(haveAliasData(pErrorCode)) {
        return *aliasTable;
    }
    return 0;
}

#if 0
/*
 * We are not currently using these functions, so I am commenting them out
 * to reduce the binary file size and improve the code coverage;
 * I do not currently want to remove this entirely because it may be useful
 * in the future and also serves to some degree as another piece of
 * documentation of the data structure.
 */
U_CFUNC const char *
ucnv_io_getAvailableAlias(uint16_t n, UErrorCode *pErrorCode) {
    if(haveAliasData(pErrorCode) && n<*aliasTable) {
        return (const char *)aliasTable+*(aliasTable+1+n);
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
            ++p;
            --count;
        }
    }
}
#endif

/* default converter name --------------------------------------------------- */

/*
 * In order to be really thread-safe, the get function would have to take
 * a buffer parameter and copy the current string inside a mutex block.
 * This implementation only tries to be really thread-safe while
 * setting the name.
 * It assumes that setting a pointer is atomic.
 */

static char defaultConverterNameBuffer[100];
static const char *defaultConverterName = NULL;

U_CFUNC const char *
ucnv_io_getDefaultConverterName() {
    /* local variable to be thread-safe */
    const char *name=defaultConverterName;
    if(name==NULL) {
        const char *codepage=0;
        umtx_lock(NULL);        
        codepage = uprv_getDefaultCodepage();
        umtx_unlock(NULL);
        if(codepage!=NULL) {
            UErrorCode errorCode=U_ZERO_ERROR;
            name=ucnv_io_getConverterName(codepage, &errorCode);
            if(U_FAILURE(errorCode) || name==NULL) {
                name=codepage;
            }
            defaultConverterName=name;
        }
    }
    return name;
}

U_CFUNC void
ucnv_io_setDefaultConverterName(const char *converterName) {
    if(converterName==NULL) {
        /* reset to the default codepage */
        defaultConverterName=NULL;
    } else {
        UErrorCode errorCode=U_ZERO_ERROR;
        const char *name=ucnv_io_getConverterName(converterName, &errorCode);
        if(U_SUCCESS(errorCode) && name!=NULL) {
            defaultConverterName=name;
        } else {
            /* do not set the name if the alias lookup failed and it is too long */
            int32_t length=uprv_strlen(converterName);
            if(length<sizeof(defaultConverterNameBuffer)) {
                /* it was not found as an alias, so copy it - accept an empty name */
                UBool didLock;
                if(defaultConverterName==defaultConverterNameBuffer) {
                    umtx_lock(NULL);
                    didLock=TRUE;
                } else {
                    didLock=FALSE;
                }
                uprv_memcpy(defaultConverterNameBuffer, converterName, length);
                defaultConverterNameBuffer[length]=0;
                defaultConverterName=defaultConverterNameBuffer;
                if(didLock) {
                    umtx_unlock(NULL);
                }
            }
        }
    }
}

/*
 * Hey, Emacs, please set the following:
 *
 * Local Variables:
 * indent-tabs-mode: nil
 * End:
 *
 */

