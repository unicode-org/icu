/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright International Business Machines Corporation, 1999           *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
*******************************************************************************
*   file name:  udata.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 1999oct25
*   created by: Markus W. Scherer
*/

#include "utypes.h"
#include "putil.h"
#include "umutex.h"
#include "cmemory.h"
#include "cstring.h"
#include "filestrm.h"
#include "udata.h"

#if !defined(HAVE_DLOPEN)
# define HAVE_DLOPEN 0
#endif
 
#if !defined(UDATA_DLL) && !defined(UDATA_MAP)
#   define UDATA_DLL
#endif

#define COMMON_DATA_NAME "icudata"
#define COMMON_DATA_NAME_LENGTH 7
#define DATA_TYPE "dat"

static UDataMemory *
doOpenChoice(const char *path, const char *type, const char *name,
             UDataMemoryIsAcceptable *isAcceptable, void *context,
             UErrorCode *pErrorCode);

U_CAPI UDataMemory * U_EXPORT2
udata_open(const char *path, const char *type, const char *name,
           UErrorCode *pErrorCode) {
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
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return NULL;
    } else if(name==NULL || *name==0 || isAcceptable==NULL) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    } else {
        return doOpenChoice(path, type, name, isAcceptable, context, pErrorCode);
    }
}

/* platform-specific implementation ----------------------------------------- */

/*
 * Most implementations define a MappedData struct
 * and have a MappedData *p; in UDataMemory.
 * They share the source code for some functions.
 * Other implementations need to #undef the following #define.
 * See after the platform-specific code.
 */
#define UDATA_INDIRECT

static bool_t
isCommonDataAcceptable(void *context,
                       const char *type, const char *name,
                       UDataInfo *pInfo);

#if defined(WIN32) /* Win32 implementations --------------------------------- */

#include <windows.h>

typedef struct {
    uint16_t headerSize;
    uint8_t magic1, magic2;
} MappedData;

#   if defined(UDATA_DLL) /* Win32 dll implementation ----------------------- */

struct UDataMemory {
    HINSTANCE lib;
    MappedData *p;
};

typedef HINSTANCE Library;

static MappedData *
getChoice(Library lib, const char *entry,
          const char *type, const char *name,
          UDataMemoryIsAcceptable *isAcceptable, void *context,
          UErrorCode *pErrorCode);

#define LIB_SUFFIX ".dll"

#define GET_ENTRY(lib, entryName) (MappedData *)GetProcAddress(lib, entryName)

#define NO_LIBRARY NULL
#define IS_LIBRARY(lib) ((lib)!=NULL)
#define LOAD_LIBRARY(path, basename, isCommon) LoadLibrary(path);
#define UNLOAD_LIBRARY(lib) FreeLibrary(lib)

#   else /* Win32 memory map implementation --------------------------------- */

struct UDataMemory {
    HANDLE map;
    MappedData *p;
};

typedef UDataMemory *Library;

static MappedData *
getChoice(Library lib, const char *entry,
          const char *type, const char *name,
          UDataMemoryIsAcceptable *isAcceptable, void *context,
          UErrorCode *pErrorCode);

#define LIB_SUFFIX ".dat"

#define GET_ENTRY(lib, entryName) getCommonMapData(lib, entryName)

#define NO_LIBRARY NULL
#define IS_LIBRARY(lib) ((lib)!=NULL)
#define UNLOAD_LIBRARY(lib) udata_close((UDataMemory *)(lib))

static Library
LOAD_LIBRARY(const char *path, const char *basename, bool_t isCommon) {
    char buffer[40];
    UDataMemory *pData;
    MappedData *p;
    HANDLE map;
    UErrorCode errorCode=U_ZERO_ERROR;

    /* set up the mapping name and the filename */
    icu_strcpy(buffer, "icu ");
    icu_strcat(buffer, basename);

    /* open the mapping */
    map=OpenFileMapping(FILE_MAP_READ, FALSE, buffer);
    if(map==NULL) {
        /* the mapping has not been created */
        HANDLE file;

        /* open the input file */
        file=CreateFile(path, GENERIC_READ, FILE_SHARE_READ, NULL,
                        OPEN_EXISTING,
                        FILE_ATTRIBUTE_NORMAL|FILE_FLAG_RANDOM_ACCESS, NULL);
        if(file==INVALID_HANDLE_VALUE) {
            return NULL;
        }

        /* create the mapping */
        map=CreateFileMapping(file, NULL, PAGE_READONLY, 0, 0, buffer);
        CloseHandle(file);
        if(map==NULL) {
            return NULL;
        }
    }

    /* get a view of the mapping */
    p=(MappedData *)MapViewOfFile(map, FILE_MAP_READ, 0, 0, 0);
    if(p==NULL) {
        CloseHandle(map);
        return NULL;
    }

    /* allocate the data structure */
    pData=(UDataMemory *)icu_malloc(sizeof(UDataMemory));
    if(pData==NULL) {
        UnmapViewOfFile(pData->p);
        CloseHandle(map);
        return NULL;
    }

    pData->map=map;
    pData->p=p;

    /* is it acceptable? */
    if(NULL==getChoice(pData, NULL, DATA_TYPE, COMMON_DATA_NAME, isCommonDataAcceptable, NULL, &errorCode)) {
        udata_close(pData);
        return NULL;
    }

    return (Library)pData;
}

U_CAPI void U_EXPORT2
udata_close(UDataMemory *pData) {
    if(pData!=NULL) {
        if(pData->map!=NULL) {
            UnmapViewOfFile(pData->p);
            CloseHandle(pData->map);
        }
        icu_free(pData);
    }
}

#   endif

/* POSIX implementations ---------------------------------------------------- */

#elif defined (LINUX)||defined(POSIX)||defined(SOLARIS)||defined(AIX)||defined(HPUX)

typedef struct {
    uint16_t headerSize;
    uint8_t magic1, magic2;
} MappedData;

#if defined(UDATA_DEBUG)
#include <stdio.h>
#endif

/* add more to this list as more platform's dll support is written */  
#   if defined(UDATA_DLL) && (HAVE_DLOPEN)

struct UDataMemory {
    void *lib;
    MappedData *p;
};

#ifndef UDATA_SO_SUFFIX
#   error Please define UDATA_SO_SUFFIX to the shlib suffix (i.e. '.so' )
#endif

#define LIB_PREFIX "lib"
#define LIB_PREFIX_LENGTH 3
#define LIB_SUFFIX UDATA_SO_SUFFIX

/* Do we need to check the platform here? */
#include <dlfcn.h>

typedef void *Library;

static MappedData *
getChoice(Library lib, const char *entry,
          const char *type, const char *name,
          UDataMemoryIsAcceptable *isAcceptable, void *context,
          UErrorCode *pErrorCode);

#define GET_ENTRY(lib, entryName) (MappedData *)dlsym(lib, entryName)

#define NO_LIBRARY NULL
#define IS_LIBRARY(lib) ((lib)!=NULL)
#define LOAD_LIBRARY(path, basename, isCommon) dlopen(path, RTLD_LAZY|RTLD_GLOBAL);
#define UNLOAD_LIBRARY(lib) dlclose(lib)

#   else /* POSIX memory map implementation --------------------------------- */
#ifdef UDATA_DLL
#undef UDATA_DLL
#endif

#ifndef UDATA_MAP
#define UDATA_MAP
#endif

#include <unistd.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/fcntl.h>

struct UDataMemory {
  size_t length;
  MappedData *p;
};

typedef UDataMemory *Library;

static MappedData *
getChoice(Library lib, const char *entry,
          const char *type, const char *name,
          UDataMemoryIsAcceptable *isAcceptable, void *context,
          UErrorCode *pErrorCode);

#define LIB_SUFFIX ".dat"

#define GET_ENTRY(lib, entryName) getCommonMapData(lib, entryName)

#define NO_LIBRARY NULL
#define IS_LIBRARY(lib) ((lib)!=NULL)
#define UNLOAD_LIBRARY(lib) udata_close((UDataMemory *)(lib))

static Library
LOAD_LIBRARY(const char *path, const char *basename, bool_t isCommon) {
    UDataMemory *pData;
    UDataInfo *info;
    int fd;
    int length;
    const char *dataDir;
    struct stat mystat;
    void *data;
    UErrorCode errorCode = U_ZERO_ERROR;

    /* determine the length of the file */
    if(stat(path, &mystat))
      {
        return NULL;
      }

    length = mystat.st_size;

    fd = open(path, O_RDONLY);

    if(fd == -1)
      {
        return NULL;
      }

    /* get a view of the mapping */
    data = mmap(0, length, PROT_READ, MAP_SHARED, fd, 0);

    close(fd); /* no longer needed */

    if(data == MAP_FAILED)
      {
        perror("mmap");
        return NULL;
      }

#ifdef UDATA_DEBUG
    fprintf(stderr, "mmap of %s [%d bytes] succeeded, -> 0x%X\n",
            path, length, data);
    fflush(stderr);
#endif
    
    /* allocate the data structure */
    pData=(UDataMemory *)icu_malloc(sizeof(UDataMemory));
    if(pData==NULL) {
        munmap(data, length);
        return NULL;
    }

    pData->length = length;
    pData->p =(MappedData *)data;

    /* is it acceptable? */
    if(NULL==getChoice(pData, NULL, DATA_TYPE, COMMON_DATA_NAME, isCommonDataAcceptable, NULL, &errorCode)) {
        udata_close(pData);
        return NULL;
    }

    return pData;
}

U_CAPI void U_EXPORT2
udata_close(UDataMemory *pData) {
    if(pData!=NULL) {
        if(pData->length!=0 && munmap(pData->p, pData->length)==-1) {
            perror("munmap");
        }
        icu_free(pData);
    }
}
#   endif 

#else /* unknown platform - stdio fopen()/fread() implementation ------------ */

#include <stdio.h>

#undef UDATA_INDIRECT
#undef UDATA_DLL
#ifndef UDATA_MAP
#   define UDATA_MAP
#endif

struct UDataMemory {
    uint16_t headerSize;
    uint8_t magic1, magic2;
};

typedef UDataMemory MappedData;
typedef UDataMemory *Library;

static MappedData *
getChoice(Library lib, const char *entry,
          const char *type, const char *name,
          UDataMemoryIsAcceptable *isAcceptable, void *context,
          UErrorCode *pErrorCode);

#define GET_ENTRY(lib, entryName) (lib)

#define NO_LIBRARY NULL
#define IS_LIBRARY(lib) ((lib)!=NULL)
#define UNLOAD_LIBRARY(lib) icu_free(lib)

static Library
LOAD_LIBRARY(const char *path, const char *basename, bool_t isCommon) {
    FileStream *file;
    UDataMemory *pData;
    int32_t fileLength;
    
    /* open the input file */
    file=T_FileStream_open(path, "rb");
    if(file==NULL) {
        return NULL;
    }

    /* get the file length */
    fileLength=T_FileStream_size(file);
    if(T_FileStream_error(file) || fileLength<=20) {
        T_FileStream_close(file);
        return NULL;
    }

    /* allocate the data structure */
    pData=(UDataMemory *)icu_malloc(fileLength);
    if(pData==NULL) {
        T_FileStream_close(file);
        return NULL;
    }

    /* read the file */
    if(fileLength!=T_FileStream_read(file, pData, fileLength)) {
        icu_free(pData);
        T_FileStream_close(file);
        return NULL;
    }

    T_FileStream_close(file);

    return pData;
}

U_CAPI void U_EXPORT2
udata_close(UDataMemory *pData) {
    if(pData!=NULL) {
        icu_free(pData);
    }
}

U_CAPI const void * U_EXPORT2
udata_getMemory(UDataMemory *pData) {
    if(pData!=NULL) {
        return (char *)pData+pData->headerSize;
    } else {
        return NULL;
    }
}

U_CAPI void U_EXPORT2
udata_getInfo(UDataMemory *pData, UDataInfo *pInfo) {
    if(pInfo!=NULL) {
        if(pData!=NULL) {
            UDataInfo *info=(UDataInfo *)(pData+1);
            uint16_t size=pInfo->size;
            if(size>info->size) {
                pInfo->size=info->size;
            }
            icu_memcpy((uint16_t *)pInfo+1, (uint16_t *)info+1, size-2);
        } else {
            pInfo->size=0;
        }
    }
}
#endif

/* common function implementations ------------------------------------------ */

#ifdef UDATA_INDIRECT

#   ifdef UDATA_DLL

/* common DLL implementations */

U_CAPI void U_EXPORT2
udata_close(UDataMemory *pData) {
    if(pData!=NULL) {
        if(IS_LIBRARY(pData->lib)) {
            UNLOAD_LIBRARY(pData->lib);
        }
        icu_free(pData);
    }
}

#   else

/* common implementation of use of common memory map */

/* this is the memory-map version of GET_ENTRY(), used by getChoice() */
static MappedData *
getCommonMapData(const UDataMemory *data, const char *dataName) {
    /* dataName==NULL if no lookup in a table of contents is necessary */
    if(dataName!=NULL) {
        const char *base=(const char *)(data->p)+data->p->headerSize;
        uint32_t *toc=(uint32_t *)base;
        uint32_t start, limit, number;

        /* perform a binary search for the data in the common data's table of contents */
        start=0;
        limit=*toc++;   /* number of names in this table of contents */
        while(start<limit-1) {
            number=(start+limit)/2;
            if(icu_strcmp(dataName, (const char *)(base+toc[2*number]))<0) {
                limit=number;
            } else {
                start=number;
            }
        }

        if(icu_strcmp(dataName, (const char *)(base+toc[2*start]))==0) {
            /* found it */
            return (MappedData *)(base+toc[2*start+1]);
        } else {
            return NULL;
        }
    } else {
        return data->p;
    }
}

static bool_t
isCommonDataAcceptable(void *context,
                       const char *type, const char *name,
                       UDataInfo *pInfo) {
    return
        pInfo->size>=20 &&
        pInfo->isBigEndian==U_IS_BIG_ENDIAN &&
        pInfo->charsetFamily==U_CHARSET_FAMILY &&
        pInfo->sizeofUChar==sizeof(UChar) &&
        pInfo->dataFormat[0]==0x43 &&   /* dataFormat="CmnD" */
        pInfo->dataFormat[1]==0x6d &&
        pInfo->dataFormat[2]==0x6e &&
        pInfo->dataFormat[3]==0x44 &&
        pInfo->formatVersion[0]==1;
}

#   endif

/* common implementations of other functions for indirect mappings */

U_CAPI const void * U_EXPORT2
udata_getMemory(UDataMemory *pData) {
    if(pData!=NULL) {
        return (char *)(pData->p)+pData->p->headerSize;
    } else {
        return NULL;
    }
}

U_CAPI void U_EXPORT2
udata_getInfo(UDataMemory *pData, UDataInfo *pInfo) {
    if(pInfo!=NULL) {
        if(pData!=NULL) {
            UDataInfo *info=(UDataInfo *)(pData->p+1);
            uint16_t size=pInfo->size;
            if(size>info->size) {
                pInfo->size=info->size;
            }
            icu_memcpy((uint16_t *)pInfo+1, (uint16_t *)info+1, size-2);
        } else {
            pInfo->size=0;
        }
    }
}

#endif

/* function implementations for all platforms ------------------------------- */

static Library commonLib=NO_LIBRARY;

static const char *strcpy_dllentry(char *target, const char *src)
{
    int i, length;

    icu_strcpy(target,src);
    length = icu_strlen(target);
    for(i=0;i<length;i++)
    {
         if(target[i] == '-')
         {
             target[i] = '_';
         }
    }
    return target;
}

static const char *strcat_dllentry(char *target, const char *src)
{
    int i, length;

    i = icu_strlen(target); /* original size */

    icu_strcat(target,src);

    length = i + icu_strlen(src);

    for(;i<length;i++)
    {
         if(target[i] == '-')
         {
             target[i] = '_';
         }
    }
    return target;
}

static UDataMemory *
doOpenChoice(const char *path, const char *type, const char *name,
             UDataMemoryIsAcceptable *isAcceptable, void *context,
             UErrorCode *pErrorCode) {
    char pathBuffer[512];
    char entryNameBuffer[40];
    char *basename, *suffix;
    const char *entryName;
    bool_t isICUData, hasPath, hasBasename;

    Library lib;
    MappedData *p;
    UErrorCode errorCode=U_ZERO_ERROR;

    /* set up path and basename */
    if(path==NULL) {
        isICUData=TRUE;
        basename=pathBuffer;

        /* copy the path to the path buffer */
        path=u_getDataDirectory();
        if(path!=NULL && *path!=0) {
            int length=icu_strlen(path);
            icu_memcpy(pathBuffer, path, length);
            basename+=length;
            hasPath=TRUE;
        } else {
            hasPath=FALSE;
        }

        /* add (prefix and) basename */
#       ifndef LIB_PREFIX
            icu_strcpy(basename, COMMON_DATA_NAME);
            suffix=basename+COMMON_DATA_NAME_LENGTH;
#       else
            icu_memcpy(basename, LIB_PREFIX, LIB_PREFIX_LENGTH);
            icu_strcpy(basename+LIB_PREFIX_LENGTH, COMMON_DATA_NAME);
            suffix=basename+LIB_PREFIX_LENGTH+COMMON_DATA_NAME_LENGTH;
#       endif
        hasBasename=TRUE;
    } else {
        char *basename2;

        isICUData=FALSE;

        /* find the last file sepator */
        basename=icu_strrchr(path, '/');
        if(basename==NULL) {
            basename=(char *)path;
        } else {
            ++basename;
        }

        basename2=icu_strrchr(basename, '\\');
        if(basename2!=NULL) {
            basename=basename2+1;
        }

        if(path!=basename) {
#           ifndef LIB_PREFIX
                /* copy the path/basename to the path buffer */
                icu_strcpy(pathBuffer, path);
                basename=pathBuffer+(basename-path);
#           else
                /* copy the path to the path buffer */
                icu_memcpy(pathBuffer, path, basename-path);

                /* add prefix and basename */
                suffix=pathBuffer+(basename-path);
                icu_memcpy(suffix, LIB_PREFIX, LIB_PREFIX_LENGTH);
                icu_strcpy(suffix+LIB_PREFIX_LENGTH, basename);
                basename=suffix;
#           endif
            hasPath=TRUE;
        } else {
            /* copy the path to the path buffer */
            path=u_getDataDirectory();
            if(path!=NULL && *path!=0) {
                int length=icu_strlen(path);
                icu_memcpy(pathBuffer, path, length);
                suffix=pathBuffer+length;
                hasPath=TRUE;
            } else {
                suffix=pathBuffer;
                hasPath=FALSE;
            }

            /* add (prefix and) basename */
#           ifndef LIB_PREFIX
                icu_strcpy(suffix, basename);
#           else
                icu_memcpy(suffix, LIB_PREFIX, LIB_PREFIX_LENGTH);
                icu_strcpy(suffix+LIB_PREFIX_LENGTH, basename);
#           endif
            basename=suffix;
        }
        hasBasename= *basename!=0;
        if(hasBasename) {
            suffix=basename+icu_strlen(basename);
        }
    }
    path=pathBuffer;

    /* set up the entry point name */
    if(type!=NULL && *type!=0) {
#ifdef UDATA_DLL
        strcpy_dllentry(entryNameBuffer, name);
#else
        icu_strcpy(entryNameBuffer, name);
#endif

#       ifdef UDATA_DLL
            icu_strcat(entryNameBuffer, "_");
#       else
            icu_strcat(entryNameBuffer, ".");
#       endif

#ifdef UDATA_DLL
        strcat_dllentry(entryNameBuffer, type);
#else
        icu_strcat(entryNameBuffer, type);
#endif

        entryName=entryNameBuffer;
    } else {
#ifdef UDATA_DLL
        strcpy_dllentry(entryNameBuffer, name);
        entryName=entryNameBuffer;
#else
        entryName=name;
#endif
    }

    /* try the common data first */
    p=NULL;

#   ifdef UDATA_INDIRECT
        if(hasBasename) {
            /* get the common data */
            /* do we have it cached? */
            if(isICUData) {
                lib=commonLib;
            } else {
                lib=NO_LIBRARY;
            }

            /* load the common data if neccessary */
            if(!IS_LIBRARY(lib)) {
                /* try path/basename first */
                icu_strcpy(suffix, LIB_SUFFIX);
                lib=LOAD_LIBRARY(path, basename, TRUE);
                if(!IS_LIBRARY(lib)) {
                    /* try basename only next */
                    lib=LOAD_LIBRARY(basename, basename, TRUE);
                }

                /* set the cache if appropriate */
                if(isICUData && IS_LIBRARY(lib)) {
                    bool_t setThisLib=FALSE;

                    /* in the mutex block, set the common library for this process */
                    umtx_lock(NULL);
                    if(!IS_LIBRARY(commonLib)) {
                        commonLib=lib;
                        setThisLib=TRUE;
                    }
                    umtx_unlock(NULL);

                    /* if a different thread set it first, then free the extra library instance */
                    if(!setThisLib) {
                        UNLOAD_LIBRARY(lib);
                        lib=commonLib;
                    }
                }
            }

            if(IS_LIBRARY(lib)) {
                /* look for the entry point in this common data */
                p=getChoice(lib, entryName, type, name, isAcceptable, context, &errorCode);
                if(p!=NULL) {
                    if(isICUData) {
                        lib=NO_LIBRARY;
                    }
                } else {
                    if(!isICUData) {
                        UNLOAD_LIBRARY(lib);
                    }
                }
            }
        }
#   endif

    /* if the data is not found in the common data, then look for a separate library */

    /* try basename+"_"+entryName[+LIB_SUFFIX] first */
    if(p==NULL && hasBasename) {
        *suffix='_';
        icu_strcpy(suffix+1, entryName);
#       ifdef UDATA_DLL
            icu_strcat(suffix+1, LIB_SUFFIX);
#       endif

        /* try path/basename first */
        lib=LOAD_LIBRARY(path, basename, FALSE);
        if(!IS_LIBRARY(lib)) {
            /* try basename only next */
            lib=LOAD_LIBRARY(basename, basename, FALSE);
        }

        if(IS_LIBRARY(lib)) {
            /* look for the entry point */
            p=getChoice(lib, entryName, type, name, isAcceptable, context, &errorCode);
            if(p==NULL) {
                UNLOAD_LIBRARY(lib);
            }
        }
    }

    /* try entryName[+LIB_SUFFIX] next */
    if(p==NULL) {
#       ifndef LIB_PREFIX
            icu_strcpy(basename, entryName);
#       else
            icu_strcpy(basename+LIB_PREFIX_LENGTH, entryName);
#       endif
#       ifdef UDATA_DLL
            icu_strcat(basename, LIB_SUFFIX);
#       endif

        /* try path/basename first */
        lib=LOAD_LIBRARY(path, basename, FALSE);
        if(!IS_LIBRARY(lib)) {
            /* try basename only next */
            lib=LOAD_LIBRARY(basename, basename, FALSE);
        }

        if(IS_LIBRARY(lib)) {
            /* look for the entry point */
            p=getChoice(lib, entryName, type, name, isAcceptable, context, &errorCode);
            if(p==NULL) {
                UNLOAD_LIBRARY(lib);
            }
        }
    }

    /* return the data if found */
    if(p!=NULL) {
#       ifndef UDATA_INDIRECT
            /* for direct mappings, Library==UDataMemory==MappedData */
            return (UDataMemory *)lib;
#       else
            UDataMemory *pData;

#           ifdef UDATA_MAP
                if(IS_LIBRARY(lib)) {
                    /* for mapped files, Library==UDataMemory */
                    pData=(UDataMemory *)lib;
                    pData->p=p;
                    return pData;
                }
#           endif

            /* allocate the data structure */
            pData=(UDataMemory *)icu_malloc(sizeof(UDataMemory));
            if(pData==NULL) {
                if(IS_LIBRARY(lib)) {
                    UNLOAD_LIBRARY(lib);
                }
                *pErrorCode=U_MEMORY_ALLOCATION_ERROR;
                return NULL;
            }

#           ifdef UDATA_DLL
                pData->lib=lib;
#           else
                /* defined(UDATA_MAP) && !IS_LIBRARY(lib) */
                icu_memset(pData, 0, sizeof(pData));
#           endif

            pData->p=p;
            return pData;
#       endif
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

static MappedData *
getChoice(Library lib, const char *entry,
          const char *type, const char *name,
          UDataMemoryIsAcceptable *isAcceptable, void *context,
          UErrorCode *pErrorCode) {
    MappedData *p;
    UDataInfo *info;

    /* get the data pointer */
    p=GET_ENTRY(lib, entry);
    if(p==NULL) {
        *pErrorCode=U_FILE_ACCESS_ERROR;
        return NULL;
    }
    info=(UDataInfo *)(p+1);

    /* check magic1 & magic2 */
    /* check for the byte ordering */
    /* is this acceptable? */
    if( p->magic1!=0xda || p->magic2!=0x27 ||
        info->isBigEndian!=U_IS_BIG_ENDIAN ||
        isAcceptable!=NULL && !isAcceptable(context, type, name, info)
    ) {
        *pErrorCode=U_INVALID_FORMAT_ERROR;
        return NULL;
    }

    return p;
}
