/*
******************************************************************************
*
*   Copyright (C) 1999-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************


/*----------------------------------------------------------------------------------
 *                           
 *       Memory mapped file wrappers for use by the ICU Data Implementation
 *       All of the platform-specific implementation for mapping data files
 *         is here.  The rest of the ICU Data implementation uses only the
 *         wrapper functions.
 *
 *----------------------------------------------------------------------------------*/
#include "unicode/utypes.h"


#include "udatamem.h"
#include "umapfile.h"


/* memory-mapping base definitions ------------------------------------------ */


#define MAP_WIN32       1
#define MAP_POSIX       2
#define MAP_FILE_STREAM 3
#define MAP_390DLL      4

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

#elif OS390
    /*   No memory mapping for 390 batch mode.  Fake it using dll loading.  */
#           include <dll.h>
#   define MAP_IMPLEMENTATION MAP_390DLL

#else /* unknown platform, no memory map implementation: use FileStream/uprv_malloc() instead */

#   include "filestrm.h"

    typedef void *MemoryMap;

#   define IS_MAP(map) ((map)!=NULL)

#   define MAP_IMPLEMENTATION MAP_FILE_STREAM

#endif




/*----------------------------------------------------------------------------------*
 *                                                                                  *
 *   Memory Mapped File support.  Platform dependent implementation of functions    *
 *                                used by the rest of the implementation.           *
 *                                                                                  *
 *----------------------------------------------------------------------------------*/
#if MAP_IMPLEMENTATION==MAP_WIN32
    UBool
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


    void
    uprv_unmapFile(UDataMemory *pData) {
        if(pData!=NULL && pData->map!=NULL) {
            UnmapViewOfFile(pData->pHeader);
            CloseHandle(pData->map);
            pData->pHeader=NULL;
            pData->map=NULL;
        }
    }



#elif MAP_IMPLEMENTATION==MAP_POSIX
    UBool
    uprv_mapFile(UDataMemory *pData, const char *path) {
        int fd;
        int length;
        struct stat mystat;
        void *data;

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
            return FALSE;
        }

		pData->map = (char *)data + length;
        pData->pHeader=(const DataHeader *)data;
        pData->mapAddr = data;
        return TRUE;
    }

	
	
    void
    uprv_unmapFile(UDataMemory *pData) {
        if(pData!=NULL && pData->map!=NULL) {
			size_t dataLen = (char *)pData->map - (char *)pData->mapAddr;
            if(munmap(pData->mapAddr, dataLen)==-1) {
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

        /* allocate the memory to hold the file data */
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


#elif MAP_IMPLEMENTATION==MAP_390DLL
    /*  390 specific Library Loading.
     *  This is the only platform left that dynamically loads an ICU Data Library.
     *  All other platforms use .data files when dynamic loading is required, but
     *  this turn out to be awkward to support in 390 batch mode.
     *
     *  The idea here is to hide the fact that 390 is using dll loading from the
     *   rest of ICU, and make it look like there is file loading happening.
     *
     */

    uprv_mapFile(UDataMemory *pData, const char *path) {
        /*  TODO:  turn the path into a dll name, try to load it,
         *         if successful, set the the pHeader field of the UDataMemory
         *         to the data address.  
         *
         *         (If path has a ".dat" extension, take it off and add on whatever
         *          the standard shared library extension is?)
         */
    }



    void    uprv_unmapFile(UDataMemory *pData) {
        if(pData!=NULL && pData->map!=NULL) {
            /*  TODO:  whatever.  Doesn't really need to do anything.  */
        }
    }

#           define  RTLD_LAZY 0
#           define  RTLD_GLOBAL 0

static void *dlopen(const char *filename, int flag) {
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

static void *dlsym(void *h, const char *symbol) {
                void *val=0;
                val=dllqueryvar((dllhandle*)h,symbol);
#               ifdef UDATA_DEBUG
                    fprintf(stderr, "dllqueryvar(%08X, %s) -> %08X\n", h, symbol, val);
#               endif
                return val;
            }

static int dlclose(void *handle) {
#               ifdef UDATA_DEBUG
                    fprintf(stderr, "dllfree: %08X\n", handle);
#               endif
                return dllfree((dllhandle*)handle);
            }



    /* TODO:  the following code is just a mish-mash of pieces from the
     *        previous OS390 data library loading code that might be useful
     *        in putting together something that works.
     */

#if 0
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
#endif 

#else
#   error MAP_IMPLEMENTATION is set incorrectly
#endif


