/*
******************************************************************************
*
*   Copyright (C) 1999-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************/


/*----------------------------------------------------------------------------------
 *
 *   UCommonData   An abstract interface for dealing with ICU Common Data Files.
 *                 ICU Common Data Files are a grouping of a number of individual
 *                 data items (resources, converters, tables, anything) into a
 *                 single file or dll.  The combined format includes a table of
 *                 contents for locating the individual items by name.
 *
 *                 Two formats for the table of contents are supported, which is
 *                 why there is an abstract inteface involved.
 *
 */               
 
#include "unicode/utypes.h"
#include "unicode/udata.h"
#include "cstring.h"
#include "ucmndata.h"
#include "udatamem.h"


/*----------------------------------------------------------------------------------*
 *                                                                                  *
 *  Pointer TOCs.   TODO: This form of table-of-contents should be removed because  *
 *                  DLLs must be relocated on loading to correct the pointer values *
 *                  and this operation makes shared memory mapping of the data      *
 *                  much less likely to work.                                       *
 *                                                                                  *
 *----------------------------------------------------------------------------------*/
typedef struct {
    const char       *entryName;
    const DataHeader *pHeader;
} PointerTOCEntry;


typedef struct  {
    uint32_t          count;
    uint32_t          reserved;
    PointerTOCEntry   entry[2];   /* Actual size is from count. */
}  PointerTOC;



typedef struct {
    int32_t           nameOffset;
    int32_t           dataOffset;
}  OffsetTOCEntry;


typedef struct {
    uint32_t          count;
    OffsetTOCEntry    entry[2];    /* Acutal size of array is from count. */
}  OffsetTOC;


/*----------------------------------------------------------------------------------*
 *                                                                                  *
 *    entry point lookup implementations                                            *
 *                                                                                  *
 *----------------------------------------------------------------------------------*/
static uint32_t offsetTOCEntryCount(const UDataMemory *pData) {
    int32_t          retVal=0;
    const OffsetTOC *toc = (OffsetTOC *)pData->toc;
    if (toc != NULL) {
        retVal = toc->count;
    } 
    return retVal;
}


static const DataHeader *
offsetTOCLookupFn(const UDataMemory *pData,
                  const char *tocEntryName,
                  UErrorCode *pErrorCode) {
    const OffsetTOC  *toc = (OffsetTOC *)pData->toc;
    if(toc!=NULL) {
        const char *base=(const char *)pData->toc;
        uint32_t start, limit, number;

        /* perform a binary search for the data in the common data's table of contents */
        start=0;
        limit=toc->count;         /* number of names in this table of contents */
        if (limit == 0) {         /* Stub common data library used during build is empty. */
            return NULL;
        }
        while(start<limit-1) {
            number=(start+limit)/2;
            if(uprv_strcmp(tocEntryName, &base[toc->entry[number].nameOffset])<0) {
                limit=number;
            } else {
                start=number;
            }
        }

        if(uprv_strcmp(tocEntryName, &base[toc->entry[start].nameOffset])==0) {
            /* found it */
#ifdef UDATA_DEBUG
/*      fprintf(stderr, "Found: %p\n",(base+toc[2*start+1])) */
            fprintf(stderr, "Found it\n");
#endif
            return (const DataHeader *)&base[toc->entry[start].dataOffset];
        } else {
#ifdef UDATA_DEBUG
      fprintf(stderr, "Not found.\n");
#endif
            return NULL;
        }
    } else {
#ifdef UDATA_DEBUG
        fprintf(stderr, "returning header\n");
#endif

        return pData->pHeader;
    }
}


static uint32_t pointerTOCEntryCount(const UDataMemory *pData) {
    const PointerTOC *toc = (PointerTOC *)pData->toc;
    if (toc != NULL) {
        return toc->count;
    } else {
        return 0;
    }
}


static const DataHeader *pointerTOCLookupFn(const UDataMemory *pData,
                   const char *name,
                   UErrorCode *pErrorCode) {
    if(pData->toc!=NULL) {
        const PointerTOC *toc = (PointerTOC *)pData->toc;
        uint32_t start, limit, number;

        /* perform a binary search for the data in the common data's table of contents */
        start=0;
        limit=toc->count;   

        if (limit == 0) {       /* Stub common data library used during build is empty. */
            return NULL;
        }

        while(start<limit-1) {
            number=(start+limit)/2;
            if(uprv_strcmp(name, toc->entry[number].entryName)<0) {
                limit=number;
            } else {
                start=number;
            }
        }

        if(uprv_strcmp(name, toc->entry[start].entryName)==0) {
            /* found it */
            return UDataMemory_normalizeDataPointer(toc->entry[start].pHeader);
        } else {
            return NULL;
        }
    } else {
        return pData->pHeader;
    }
}

static const commonDataFuncs CmnDFuncs = {offsetTOCLookupFn,  offsetTOCEntryCount};
static const commonDataFuncs ToCPFuncs = {pointerTOCLookupFn, pointerTOCEntryCount};



/*----------------------------------------------------------------------*
 *                                                                      *
 *  checkCommonData   Validate the format of a common data file.        *
 *                    Fill in the virtual function ptr based on TOC type  *
 *                    If the data is invalid, close the UDataMemory     *
 *                    and set the appropriate error code.               *
 *                                                                      *
 *----------------------------------------------------------------------*/
void udata_checkCommonData(UDataMemory *udm, UErrorCode *err) {
    if (U_FAILURE(*err)) {
        return;
    }

    if(!(udm->pHeader->dataHeader.magic1==0xda &&
        udm->pHeader->dataHeader.magic2==0x27 &&
        udm->pHeader->info.isBigEndian==U_IS_BIG_ENDIAN &&
        udm->pHeader->info.charsetFamily==U_CHARSET_FAMILY)
        ) {
        /* header not valid */
        *err=U_INVALID_FORMAT_ERROR;
    }
    else if (udm->pHeader->info.dataFormat[0]==0x43 &&
        udm->pHeader->info.dataFormat[1]==0x6d &&
        udm->pHeader->info.dataFormat[2]==0x6e &&
        udm->pHeader->info.dataFormat[3]==0x44 &&
        udm->pHeader->info.formatVersion[0]==1
        ) {
        /* dataFormat="CmnD" */
        udm->vFuncs = &CmnDFuncs;
        udm->toc=(const char *)udm->pHeader+udm->pHeader->dataHeader.headerSize;
    }
    else if(udm->pHeader->info.dataFormat[0]==0x54 &&
        udm->pHeader->info.dataFormat[1]==0x6f &&
        udm->pHeader->info.dataFormat[2]==0x43 &&
        udm->pHeader->info.dataFormat[3]==0x50 &&
        udm->pHeader->info.formatVersion[0]==1
        ) {
        /* dataFormat="ToCP" */
        udm->vFuncs = &ToCPFuncs;
        udm->toc=(const char *)udm->pHeader+udm->pHeader->dataHeader.headerSize;
    }
    else {
        /* dataFormat not recognized */
        *err=U_INVALID_FORMAT_ERROR;
    }

    if (U_FAILURE(*err)) {
        /* If the data is no good and we memory-mapped it ourselves,
         *  close the memory mapping so it doesn't leak.  Note that this has
         *  no effect on non-memory mapped data, other than clearing fields in udm.
         */
        udata_close(udm);
    }
}

