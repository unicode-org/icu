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
    const char *entryName;
    const DataHeader *pHeader;
} PointerTOCEntry;


/*----------------------------------------------------------------------------------*
 *                                                                                  *
 *    entry point lookup implementations                                            *
 *                                                                                  *
 *----------------------------------------------------------------------------------*/
static uint32_t offsetTOCEntryCount(const UDataMemory *pData) {
    uint32_t count = *(const uint32_t *)pData->toc;
    return count;
}


static const DataHeader *
offsetTOCLookupFn(const UDataMemory *pData,
                  const char *tocEntryName,
                  UErrorCode *pErrorCode) {

    if(pData->toc!=NULL) {
        const char *base=(const char *)pData->toc;
        uint32_t *toc=(uint32_t *)pData->toc;
        uint32_t start, limit, number;

        /* perform a binary search for the data in the common data's table of contents */
        start=0;
        limit=*toc++;   /* number of names in this table of contents */
        if (limit == 0) {         /* Stub common data library used during build is empty. */
            return NULL;
        }
        while(start<limit-1) {
            number=(start+limit)/2;
            if(uprv_strcmp(tocEntryName, base+toc[2*number])<0) {
                limit=number;
            } else {
                start=number;
            }
        }

        if(uprv_strcmp(tocEntryName, base+toc[2*start])==0) {
            /* found it */
#ifdef UDATA_DEBUG
      fprintf(stderr, "Found: %p\n",(base+toc[2*start+1]));
#endif
            return (const DataHeader *)(base+toc[2*start+1]);
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
    uint32_t count = *(const uint32_t *)pData->toc;
    return count;
}


static const DataHeader *pointerTOCLookupFn(const UDataMemory *pData,
                   const char *tocEntryName,
                   UErrorCode *pErrorCode) {
#ifdef UDATA_DEBUG
  fprintf(stderr, "ptrTOC[%p] looking for %s/%s\n",
      pData,
      tocEntryName,dllEntryName);
#endif
    if(pData->toc!=NULL) {
        const PointerTOCEntry *toc=(const PointerTOCEntry *)((const uint32_t *)pData->toc+2);
        uint32_t start, limit, number;

        /* perform a binary search for the data in the common data's table of contents */
        start=0;
        limit=*(const uint32_t *)pData->toc; /* number of names in this table of contents */

#ifdef UDATA_DEBUG
        fprintf(stderr, "  # of ents: %d\n", limit);
        fflush(stderr);
#endif

        if (limit == 0) {         /* Stub common data library used during build is empty. */
            return NULL;
        }

        while(start<limit-1) {
            number=(start+limit)/2;
            if(uprv_strcmp(tocEntryName, toc[number].entryName)<0) {
                limit=number;
            } else {
                start=number;
            }
        }

        if(uprv_strcmp(tocEntryName, toc[start].entryName)==0) {
            /* found it */
#ifdef UDATA_DEBUG
            fprintf(stderr, "FOUND: %p\n",
                normalizeDataPointer(toc[start].pHeader));
#endif

            return normalizeDataPointer(toc[start].pHeader);
        } else {
#ifdef UDATA_DEBUG
            fprintf(stderr, "NOT found\n");
#endif
            return NULL;
        }
    } else {
#ifdef UDATA_DEBUG
        fprintf(stderr, "Returning header\n");
#endif
        return pData->pHeader;
    }
}

static commonDataFuncs CmnDFuncs = {offsetTOCLookupFn,  offsetTOCEntryCount};
static commonDataFuncs ToCPFuncs = {pointerTOCLookupFn, pointerTOCEntryCount};



/*----------------------------------------------------------------------*
 *                                                                      *
 *  checkCommonData   Validate the format of a common data file.        *
 *                    Fill in the virtual function ptr based on TOC type  *
 *                    If the data is invalid, close the UDataMemory     *
 *                    and set the appropriate error code.               *
 *                                                                      *
 *----------------------------------------------------------------------*/
void checkCommonData(UDataMemory *udm, UErrorCode *err) {
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

