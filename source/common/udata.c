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

#ifdef OS390
#include <stdlib.h>
#endif

/* configuration ---------------------------------------------------------- */


#define COMMON_DATA_NAME U_ICUDATA_NAME
#define COMMON_DATA_NAME_LENGTH 8
/* Tests must verify that it remains 8 characters. */

#ifdef OS390
#define COMMON_DATA1_NAME U_ICUDATA_NAME"_390"
#define COMMON_DATA1_NAME_LENGTH (COMMON_DATA_NAME_LENGTH + 4)
static UBool s390dll = TRUE;
#endif

#define DATA_TYPE "dat"

/* If you are excruciatingly bored turn this on .. */
/* #define UDATA_DEBUG 1 */

#if defined(UDATA_DEBUG)
#   include <stdio.h>
#endif

/* DLL/shared library base functions ----------------------------------------   */
/*   TODO:  Dynamic loading of DLLs is no longer supported.                     */
/*          390 is a special case, since it can not support file loading.       */
/*          Need to abstract 390 library loading so that it appears to the rest */
/*          ICU as file loading.                                                */


#ifdef OS390
#           include <dll.h>

#           define  RTLD_LAZY 0
#           define  RTLD_GLOBAL 0

            void *dlopen(const char *filename, int flag) {
                dllhandle *handle;

#               ifdef UDATA_DEBUG
                    fprintf(stderr, "dllload: %s ", filename);
#               endif
                handle=dllload(filename);
#               ifdef UDATA_DEBUG
                    fprintf(stderr, " -> %08X\n", handle );
#               endif
                    return handle;
            }

            void *dlsym(void *h, const char *symbol) {
                void *val=0;
                val=dllqueryvar((dllhandle*)h,symbol);
#               ifdef UDATA_DEBUG
                    fprintf(stderr, "dllqueryvar(%08X, %s) -> %08X\n", h, symbol, val);
#               endif
                return val;
            }

            int dlclose(void *handle) {
#               ifdef UDATA_DEBUG
                    fprintf(stderr, "dllfree: %08X\n", handle);
#               endif
                return dllfree((dllhandle*)handle);
            }
#endif /*  OS390:    */




/* memory-mapping base definitions ------------------------------------------ */

/* we need these definitions before the common ones because
   MemoryMap is a field of UDataMemory;
   however, the mapping functions use UDataMemory,
   therefore they are defined later
 */

#define MAP_WIN32       1
#define MAP_POSIX       2
#define MAP_FILE_STREAM 3

#ifdef WIN32
#   define WIN32_LEAN_AND_MEAN
#   define NOGDI
#   define NOUSER
#   define NOSERVICE
#   define NOIME
#   define NOMCX
#   include <windows.h>

    typedef HANDLE MemoryMap;

#   define IS_MAP(map) ((map)!=NULL)

#   define MAP_IMPLEMENTATION MAP_WIN32

/* ### Todo: auto detect mmap(). Until then, just add your platform here. */
#elif HAVE_MMAP || defined(U_LINUX) || defined(POSIX) || defined(U_SOLARIS) || defined(AIX) || defined(HPUX) || defined(OS390) || defined(PTX)
    typedef size_t MemoryMap;

#   define IS_MAP(map) ((map)!=0)

#   include <unistd.h>
#   include <sys/mman.h>
#   include <sys/stat.h>
#   include <fcntl.h>

#   ifndef MAP_FAILED
#       define MAP_FAILED ((void*)-1)
#   endif

#   define MAP_IMPLEMENTATION MAP_POSIX

#else /* unknown platform, no memory map implementation: use FileStream/uprv_malloc() instead */

#   include "filestrm.h"

    typedef void *MemoryMap;

#   define IS_MAP(map) ((map)!=NULL)

#   define MAP_IMPLEMENTATION MAP_FILE_STREAM

#endif

/* common definitions ------------------------------------------------------- */



typedef struct  {
    uint16_t    headerSize;
    uint8_t     magic1;
    uint8_t     magic2;
} MappedData;

typedef struct  {
    MappedData  dataHeader;
    UDataInfo   info;
} DataHeader;

typedef const DataHeader *
LookupFn(const UDataMemory *pData,
         const char *tocEntryName,
         const char *dllEntryName,
         UErrorCode *pErrorCode);

/*----------------------------------------------------------------------------------*
 *                                                                                  *
 *  UDataMemory     Very Important Struct.  Pointers to these are returned          *
 *                  to callers from the various data open functions.                *
 *                  These keep track of everything about the memeory                *
 *                                                                                  *
 *----------------------------------------------------------------------------------*/
struct UDataMemory {
    MemoryMap         map;         /* Handle, or whatever.  OS dependent.             */
                                   /* Only set if a close operation should unmap the  */
                                   /*  associated data.                               */
    LookupFn         *lookupFn;
    const void       *toc;         /* For common memory, to find pieces within.       */
    const DataHeader *pHeader;     /* Header.  For common data, header is at top of file */
    const void       *mapAddr;     /* For mapped or allocated memory, the start addr. */
                                   /*   Will be above pHeader in some cases.          */
                                   /*   Needed to allow unmapping.                    */
    uint32_t          flags;       /* Memory format, TOC type, Allocation type, etc.  */
};

/* constants for UDataMemory flags        */
#define MALLOCED_UDATAMEMORY_FLAG  0x80000000   /* Set flag if UDataMemory object itself
                                                 *  is on heap, and must be freed when
                                                 *  it is closed.
                                                 */

#define TOC_HAS_CONTENTS_FLAG      0x40000000   /* Flag set if UDataMemory is for a
                                                 * something with a non-empty TOC.
                                                 * An empty TOC means this is the stub
                                                 *  library.
                                                 */

#define IS_DATA_MEMORY_LOADED(pData) ((pData)->pHeader!=NULL)


static void UDataMemory_init(UDataMemory *This) {
    uprv_memset(This, 0, sizeof(UDataMemory));
}


static void UDatamemory_assign(UDataMemory *dest, UDataMemory *source) {
    /* UDataMemory Assignment.  Destination UDataMemory must be initialized first.
     *                          Malloced flag of the destination is preserved,
     *                          since it indicates where the UDataMemory struct itself
     *                          is allocated. */
    uint32_t  dest_MALLOCED_UDATAMEMORY_FLAG = dest->flags & MALLOCED_UDATAMEMORY_FLAG;
    uprv_memcpy(dest, source, sizeof(UDataMemory));
    dest->flags &= ~MALLOCED_UDATAMEMORY_FLAG;
    dest->flags |= dest_MALLOCED_UDATAMEMORY_FLAG;
}

static UDataMemory *UDataMemory_createNewInstance(UErrorCode *pErr) {
    UDataMemory *This;

    if (U_FAILURE(*pErr)) {
        return NULL;
    }
    This = uprv_malloc(sizeof(UDataMemory));
    if (This == NULL) {
        *pErr = U_MEMORY_ALLOCATION_ERROR; }
    else {
        UDataMemory_init(This);
        This->flags |= MALLOCED_UDATAMEMORY_FLAG;
    }
    return This;
}

/*----------------------------------------------------------------------------------*
 *                                                                                  *
 *  Pointer TOCs.   This form of table-of-contents should be removed because        *
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
 *   Memory Mapped File support.  Platform dependent implementation of functions    *
 *                                used by the rest of the implementation.           *
 *                                                                                  *
 *----------------------------------------------------------------------------------*/
#if MAP_IMPLEMENTATION==MAP_WIN32
    static UBool
    uprv_mapFile(
         UDataMemory *pData,    /* Fill in with info on the result doing the mapping. */
                                /*   Output only; any original contents are cleared.  */
         const char *path       /* File path to be opened/mapped                      */
         )
    {
        HANDLE map;
        HANDLE file;

        UDataMemory_init(pData); /* Clear the output struct.        */

        /* open the input file */
        file=CreateFile(path, GENERIC_READ, FILE_SHARE_READ, NULL,
            OPEN_EXISTING,
            FILE_ATTRIBUTE_NORMAL|FILE_FLAG_RANDOM_ACCESS, NULL);
        if(file==INVALID_HANDLE_VALUE) {
            return FALSE;
        }

        /* create an unnamed Windows file-mapping object for the specified file */
        map=CreateFileMapping(file, NULL, PAGE_READONLY, 0, 0, NULL);
        CloseHandle(file);
        if(map==NULL) {
            return FALSE;
        }

        /* map a view of the file into our address space */
        pData->pHeader=(const DataHeader *)MapViewOfFile(map, FILE_MAP_READ, 0, 0, 0);
        if(pData->pHeader==NULL) {
            CloseHandle(map);
            return FALSE;
        }
        pData->map=map;
        return TRUE;
    }


    static void
    uprv_unmapFile(UDataMemory *pData) {
        if(pData!=NULL && pData->map!=NULL) {
            UnmapViewOfFile(pData->pHeader);
            CloseHandle(pData->map);
            pData->pHeader=NULL;
            pData->map=NULL;
        }
    }

#elif MAP_IMPLEMENTATION==MAP_POSIX
    static UBool
    uprv_mapFile(UDataMemory *pData, const char *path) {
        int fd;
        int length;
        struct stat mystat;
        const void *data;

        UDataMemory_init(pData); /* Clear the output struct.        */

        /* determine the length of the file */
        if(stat(path, &mystat)!=0 || mystat.st_size<=0) {
            return FALSE;
        }
        length=mystat.st_size;

        /* open the file */
        fd=open(path, O_RDONLY);
        if(fd==-1) {
            return FALSE;
        }

        /* get a view of the mapping */
#ifndef HPUX
        data=mmap(0, length, PROT_READ, MAP_SHARED,  fd, 0);
#else
        data=mmap(0, length, PROT_READ, MAP_PRIVATE, fd, 0);
#endif
        close(fd); /* no longer needed */
        if(data==MAP_FAILED) {

#       ifdef UDATA_DEBUG
              perror("mmap");
#       endif

            return FALSE;
        }

#       ifdef UDATA_DEBUG
            fprintf(stderr, "mmap of %s [%d bytes] succeeded, -> 0x%X\n", path, length, data);
            fflush(stderr);
#       endif

        pData->map=length;
        pData->pHeader=(const DataHeader *)data;
        pData->mapAddr = data;
        return TRUE;
    }

    static void
    uprv_unmapFile(UDataMemory *pData) {
        if(pData!=NULL && pData->map>0) {
            if(munmap((void *)pData->mapAddr, pData->map)==-1) {
#               ifdef UDATA_DEBUG
                    perror("munmap");
#               endif
            }
            pData->pHeader=NULL;
            pData->map=0;
            pData->mapAddr=NULL;
        }
    }

#elif MAP_IMPLEMENTATION==MAP_FILE_STREAM
    static UBool
    uprv_mapFile(UDataMemory *pData, const char *path) {
        FileStream *file;
        int32_t fileLength;
        void *p;

        UDataMemory_init(pData); /* Clear the output struct.        */
        /* open the input file */
        file=T_FileStream_open(path, "rb");
        if(file==NULL) {
            return FALSE;
        }

        /* get the file length */
        fileLength=T_FileStream_size(file);
        if(T_FileStream_error(file) || fileLength<=20) {
            T_FileStream_close(file);
            return FALSE;
        }

        /* allocate the data structure */
        p=uprv_malloc(fileLength);
        if(p==NULL) {
            T_FileStream_close(file);
            return FALSE;
        }

        /* read the file */
        if(fileLength!=T_FileStream_read(file, p, fileLength)) {
            uprv_free(p);
            T_FileStream_close(file);
            return FALSE;
        }

        T_FileStream_close(file);
        pData->map=p;
        pData->pHeader=(const DataHeader *)p;
        pData->mapAddr=p;
        return TRUE;
    }

    static void
    uprv_unmapFile(UDataMemory *pData) {
        if(pData!=NULL && pData->map!=NULL) {
            uprv_free(pData->map);
            pData->map     = NULL;
            pData->mapAddr = NULL;
            pData->pHeader = NULL;
        }
    }

#else
#   error MAP_IMPLEMENTATION is set incorrectly
#endif



/*----------------------------------------------------------------------------------*
 *                                                                                  *
 *    entry point lookup implementations                                            *
 *                                                                                  *
 *----------------------------------------------------------------------------------*/
static const DataHeader *
normalizeDataPointer(const DataHeader *p) {
    /* allow the data to be optionally prepended with an alignment-forcing double value */
    if(p==NULL || (p->dataHeader.magic1==0xda && p->dataHeader.magic2==0x27)) {
        return p;
    } else {
        return (const DataHeader *)((const double *)p+1);
    }
}

static const DataHeader *
offsetTOCLookupFn(const UDataMemory *pData,
                  const char *tocEntryName,
                  const char *dllEntryName,
                  UErrorCode *pErrorCode) {

#ifdef UDATA_DEBUG
  fprintf(stderr, "offsetTOC[%p] looking for %s/%s\n",
      pData,
      tocEntryName,dllEntryName);
#endif

    if(pData->toc!=NULL) {
        const char *base=(const char *)pData->toc;
        uint32_t *toc=(uint32_t *)pData->toc;
        uint32_t start, limit, number;

        /* perform a binary search for the data in the common data's table of contents */
        start=0;
        limit=*toc++;   /* number of names in this table of contents */
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

static const DataHeader *
pointerTOCLookupFn(const UDataMemory *pData,
                   const char *tocEntryName,
                   const char *dllEntryName,
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


/* common library functions ------------------------------------------------- */

static UDataMemory *commonICUData = NULL;

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
    if (commonICUData==oldData) {
        commonICUData = newCommonData;
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
    UDataMemory    item;
} DataCacheElement;

static UHashtable *gHashTable = NULL;


/*
 * Deleter function for DataCacheElements.
 *         udata cleanup function closes the hash table; hash table in turn calls back to
 *         here for each entry.
 */
static void  U_CALLCONV DataCacheElement_deleter(void *pDCEl) {
    DataCacheElement *p = (DataCacheElement *)pDCEl;
    udata_close(&p->item);             /* unmaps storage */
    uprv_free(p->name);                /* delete the hash key string. */
    uprv_free(pDCEl);                  /* delete 'this'          */
}


 /*   udata_getCacheHashTable()
 *     Get the hash table used to store the data cache entries.
 *     Lazy create it if it doesn't yet exist.
 */
static UHashtable *udata_getHashTable() {
    UErrorCode err = U_ZERO_ERROR;

    if (gHashTable != NULL) {
        return gHashTable;
    }
    umtx_lock(NULL);
    if (gHashTable == NULL) {
        gHashTable = uhash_open(uhash_hashChars, uhash_compareChars, &err);
        uhash_setValueDeleter(gHashTable, DataCacheElement_deleter);
    }
    umtx_unlock(NULL);

    if (U_FAILURE(err)) {
        return NULL;      /* TODO:  handle this error better.  */
    }
    return gHashTable;
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
        retVal = &el->item;
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
    UDataMemory_init(&newElement->item);         /*Need separte init + copy to get flags right. */
    UDatamemory_assign(&newElement->item, item);  /*  They're not all copied.                    */

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
        uprv_free(newElement->name);
        uprv_free(newElement);
        return oldValue;
    }

    return &newElement->item;
}


/*----------------------------------------------------------------------*
 *                                                                      *
 *  checkCommonData   Validate the format of a common data file.        *
 *                    Fill in the TOC type in the UDataMemory           *
 *                    If the data is invalid, close the UDataMemory     *
 *                    and set the appropriate error code.               *
 *                                                                      *
 *----------------------------------------------------------------------*/
static void checkCommonData(UDataMemory *udm, UErrorCode *err) {
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
        udm->lookupFn=offsetTOCLookupFn;
        udm->toc=(const char *)udm->pHeader+udm->pHeader->dataHeader.headerSize;
        if (*(const uint32_t *)udm->toc > 0) {
            udm->flags |= TOC_HAS_CONTENTS_FLAG;
        }
    }
    else if(udm->pHeader->info.dataFormat[0]==0x54 &&
        udm->pHeader->info.dataFormat[1]==0x6f &&
        udm->pHeader->info.dataFormat[2]==0x43 &&
        udm->pHeader->info.dataFormat[3]==0x50 &&
        udm->pHeader->info.formatVersion[0]==1
        ) {
        /* dataFormat="ToCP" */
        udm->lookupFn=pointerTOCLookupFn;
        udm->toc=(const char *)udm->pHeader+udm->pHeader->dataHeader.headerSize;
        if (*(const uint32_t *)udm->toc > 0) {
            udm->flags |= TOC_HAS_CONTENTS_FLAG;
        }
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
        uprv_unmapFile(udm);
    }
}


UBool
udata_cleanup()
{
    if (gHashTable) {              /* Delete the cache of user data mappings.  */
        uhash_close(gHashTable);   /*   Table owns the contents, and will delete them. */
        gHashTable = 0;            /*   Cleanup is not thread safe.                */
    }

    udata_close(commonICUData);    /* Clean up common ICU Data             */
    commonICUData = NULL;

    return TRUE;                   /* Everything was cleaned up */
}


/*                                                                     */
/*  Add a static reference to the common data from a library if the    */
/*      build options are set to request it.                           */
/*   Unless overridden by an explicit u_setCommonData, this will be    */
/*      our common data.                                               */
#if defined(UDATA_STATIC_LIB) || defined(UDATA_DLL)
extern  const DataHeader U_IMPORT U_ICUDATA_ENTRY_POINT;
#endif


/*----------------------------------------------------------------------*
 *                                                                      *
 *   openCommonData   Attempt to open a common format (.dat) file       *
 *                    Map it into memory (if it's not there already)    *
 *                    and return a UDataMemory object for it.           *
 *                    The UDataMemory object will either be heap or     *
 *                    global - in either case, it is permanent and can  *
 *                    be safely passed back the chain of callers.       *
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
        if(commonICUData != NULL) {
            return commonICUData;
        }

        tData.pHeader = &U_ICUDATA_ENTRY_POINT;
        checkCommonData(&tData, pErrorCode);
        setCommonICUData(&tData, NULL, FALSE, pErrorCode);
        return commonICUData;
    }


    /* request is NOT for ICU Data.
    * Is the requested data already cached?  */
    {
        UDataMemory  *dataToReturn = udata_findCachedData(path);
        if (dataToReturn != NULL) {
            return dataToReturn;
        }
    }

    /* Requested item is not in the cache.
     * Hunt it down, trying all the fall back locations.
     */
    basename=setPathGetBasename(path, pathBuffer);
    inBasename=findBasename(path);
    if(*inBasename==0) {
        /* no basename, no common data */
        *pErrorCode=U_FILE_ACCESS_ERROR;
        return NULL;
    }


    /* set up the file name */
    suffix=strcpy_returnEnd(basename, inBasename);
    uprv_strcpy(suffix, "." DATA_TYPE);      /*  DATA_TYPE is ".dat" */

    /* try path/basename first, then basename only */
    uprv_mapFile(&tData, pathBuffer);

    if (!IS_DATA_MEMORY_LOADED(&tData)) {
        if (basename!=pathBuffer) {
            uprv_mapFile(&tData, basename);
        }
    }

    if (!IS_DATA_MEMORY_LOADED(&tData)) {
        /* no common data */
        *pErrorCode=U_FILE_ACCESS_ERROR;
        return NULL;
    }

    /* we have mapped a file, check its header */
    tData.pHeader=tData.pHeader;
    checkCommonData(&tData, pErrorCode);


    /* Cache the UDataMemory struct for this .dat file,
     *   so we won't need to hunt it down and map it again next time
     *   something is needed from it.                */
    return udata_cacheDataItem(path, &tData, pErrorCode);
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
#ifndef OS390
    /*  For most platforms (all except 390), if the data library that
     *   we are running with turned out to be the stub library, we will try to
     *   load a .dat file instead.  The stub library has no entries in its
     *   TOC, which is how we identify it here.
     */
    UDataMemory   *pData;

    if (failedData->flags & TOC_HAS_CONTENTS_FLAG) {
        /*  Not the stub.  We can't extend.  */
        return FALSE;
    }

    /* See if we can explicitly open a .dat file for the ICUData. */
    pData = openCommonData(
               U_ICUDATA_NAME,            /*  "icudt20l" , for example.          */
               FALSE,                     /*  Pretend we're not opening ICUData  */
               pErr);


    setCommonICUData(pData,         /*  The new common data.                              */
                 failedData,        /*  Old ICUData ptr.  Overwrite of this value is ok,  */
                 FALSE,             /*  No warnings if write didn't happen                */
                 pErr);             /*  setCommonICUData honors errors; NOP if error set  */

    return commonICUData != failedData;   /* Return true if ICUData pointer was updated.   */
                                    /*   (Could potentialy have been done by another thread racing */
                                    /*   us through here, but that's fine, we still return true    */
                                    /*   so that current thread will also examine extended data.   */
#else

    /*  390 specific Library Loading.
     *  This is the only platform left that dynamically loads an ICU Data Library.
     *  All other platforms use .data files when dynamic loading is required, but
     *  this turn out to be awkward to support in 390 batch mode.
     */
    static UBool isLibLoaded;

    if (isLibLoaded == TRUE) {
        /* We've already been through here once and loaded the full ICU Data library.
        *  Nothing more left to load.    */
        return false;
    }


    /* Need to do loading in a mutex-protected section of code because we
     *  don't want to load it twice because of a race.
     */
    umtx_lock(NULL);
    if (isLibLoaded) {
        return FALSE;  /* Watch that unlock  */
    }

    /* TODO:  the following code is just a mish-mash of pieces from the
     *        previous data library loading code that might be useful
     *        in putting together something that works.
     */

    Library lib;
    inBasename=U_ICUDATA_NAME"_390";
    suffix=strcpy_returnEnd(basename, inBasename);
    uprv_strcpy(suffix, LIB_SUFFIX);

    if (uprv_isOS390BatchMode()) {
    /* ### hack: we still need to get u_getDataDirectory() fixed
    for OS/390 (batch mode - always return "//"? )
    and this here straightened out with LIB_PREFIX and LIB_SUFFIX (both empty?!)
    This is probably due to the strange file system on OS/390.  It's more like
        a database with short entry names than a typical file system. */
        if (s390dll) {
            lib=LOAD_LIBRARY("//IXMICUD1", "//IXMICUD1");
        }
        else {
            /* U_ICUDATA_NAME should always have the correct name */
            /* 390port: BUT FOR BATCH MODE IT IS AN EXCEPTION ... */
            /* 390port: THE NEXT LINE OF CODE WILL NOT WORK !!!!! */
            /*lib=LOAD_LIBRARY("//" U_ICUDATA_NAME, "//" U_ICUDATA_NAME);*/
            lib=LOAD_LIBRARY("//IXMICUDA", "//IXMICUDA"); /*390port*/
        }
    }

    lib=LOAD_LIBRARY(pathBuffer, basename);
    if(!IS_LIBRARY(lib) && basename!=pathBuffer) {
        /* try basename only next */
        lib=LOAD_LIBRARY(basename, basename);
    }

    if(IS_LIBRARY(lib)) {
        /* we have a data DLL - what kind of lookup do we need here? */
        char entryName[100];
        const DataHeader *pHeader;
        *basename=0;
    }

    checkCommonData(&tData, pErrorCode);
    if (U_SUCCESS(*pErrorCode)) {
        /* Don't close the old data - someone might be using it
         * May need to change the global to be a pointer rather than a static struct
         * to get a clean switch-over.
         */
        setCommonICUData(&tData);

    }

    umtx_unlock(NULL);
    return TRUE;  /* SUCCESS? */
#endif  /* OS390 */
}




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
    if(commonICUData != NULL) {
        *pErrorCode=U_USING_DEFAULT_ERROR;
        return;
    }

    /* normalize the data pointer and test for validity */
    UDataMemory_init(&dataMemory);
    dataMemory.pHeader = normalizeDataPointer((const DataHeader *)data);
    checkCommonData(&dataMemory, pErrorCode);
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
    checkCommonData(&udm, err);
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
/*      // rDataMem is already initialized
        UDataMemory_init(rDataMem);
*/
        rDataMem->pHeader = pHeader;
    } else {
        /* the data is not acceptable, look further */
        /* If we eventually find something good, this errorcode will be */
        /*    cleared out.                                              */
        *nonFatalErr=U_INVALID_FORMAT_ERROR;
    }
    return rDataMem;
}



/*------------------------------------------------------------------------------*
 *                                                                              *
 *   setEntryNames    Files Names and DLL entry point names have different      *
 *                    rules for what's valid.   For a DLL entry point name,     *
 "                    change all '.' or '-'s to '_'.   For both, append         *
 *                    '.typeName'  (or _typeName) to the name                   *
 *                                                                              *
 *------------------------------------------------------------------------------*/
static void
setEntryNames(const char *type, const char *name,
              char *tocEntryName, char *dllEntryName) {
    while(*name!=0) {
        *tocEntryName=*name;
        if(*name=='.' || *name=='-') {
            *dllEntryName='_';
        } else {
            *dllEntryName=*name;
        }
        ++tocEntryName;
        ++dllEntryName;
        ++name;
    }

    if(type!=NULL && *type!=0) {
        *tocEntryName++='.';
        *dllEntryName++='_';
        do {
            *tocEntryName++=*dllEntryName++=*type++;
        } while(*type!=0);
    }

    *tocEntryName=*dllEntryName=0;
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
 *
 *  For common data passed in from the user via udata_setAppData() or
 *  udata_setCommonData(), ownership remains with the user.
 *
 *  UDataMemory objects themselves, as opposed to the memory they describe,
 *  can be anywhere - heap, stack/local or global.
 *  They have a flag bit to indicate when they're heap allocated and thus
 *  must be deleted when closed.
 */


/* main data loading function ----------------------------------------------- */

static UDataMemory *
doOpenChoice(const char *path, const char *type, const char *name,
             UDataMemoryIsAcceptable *isAcceptable, void *context,
             UErrorCode *pErrorCode)
{
    char                pathBuffer[1024];
    char                tocEntryName[100];
    char                dllEntryName[100];
    UDataMemory         dataMemory;
    UDataMemory        *pCommonData;
    UDataMemory        *pEntryData;
    const DataHeader   *pHeader;
    const char         *inBasename;
    char               *basename;
    char               *suffix;
    UErrorCode          errorCode=U_ZERO_ERROR;
    UBool               isICUData= (UBool)(path==NULL);

    /* set up the ToC names for DLL and offset-ToC lookups */
    setEntryNames(type, name, tocEntryName, dllEntryName);


    /* try to get common data.  The loop is for platforms such as the 390 that do
     *  not initially load the full set of ICU data.  If the lookup of an ICU data item
     *  fails, the full (but slower to load) set is loaded, the and the loop repeats,
     *  trying the lookup again.  Once the full set of ICU data is loaded, the loop wont
     *  repeat because the full set will be checked the first time through.  */
    for (;;) {
        pCommonData=openCommonData(path, isICUData, &errorCode);
#ifdef UDATA_DEBUG
        fprintf(stderr, "commonData;%p\n", pCommonData);
        fflush(stderr);
#endif

        if(U_SUCCESS(errorCode)) {
            /* look up the data piece in the common data */
            pHeader=pCommonData->lookupFn(pCommonData, tocEntryName, dllEntryName, &errorCode);
#ifdef UDATA_DEBUG
            fprintf(stderr, "Common found: %p\n", pHeader);
#endif
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


    /* the data was not found in the common data,  look further */
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
            uprv_unmapFile(&dataMemory);

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
        uprv_unmapFile(&dataMemory);

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

static void
unloadDataMemory(UDataMemory *pData) {
     uprv_unmapFile(pData);
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
udata_close(UDataMemory *pData) {
#ifdef UDATA_DEBUG
    fprintf(stderr, "udata_close()\n");fflush(stderr);
#endif

    if(pData!=NULL) {
        unloadDataMemory(pData);
        if(pData->flags & MALLOCED_UDATAMEMORY_FLAG ) {
            uprv_free(pData);
        } else {
            UDataMemory_init(pData);
        }
    }
}

U_CAPI const void * U_EXPORT2
udata_getMemory(UDataMemory *pData) {
    if(pData!=NULL && pData->pHeader!=NULL) {
        return (char *)(pData->pHeader)+pData->pHeader->dataHeader.headerSize;
    } else {
        return NULL;
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
