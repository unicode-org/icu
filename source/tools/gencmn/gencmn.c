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
*   file name:  gencmn.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 1999nov01
*   created by: Markus W. Scherer
*
*   This program reads a list of data files and combines them
*   into one common, memory-mappable file.
*/

#include <stdio.h>
#include <stdlib.h>
#include "utypes.h"
#include "cmemory.h"
#include "cstring.h"
#include "filestrm.h"
#include "../toolutil/toolutil.h"
#include "../toolutil/unewdata.h"

#define STRING_STORE_SIZE 100000
#define MAX_FILE_COUNT 2000

#define COMMON_DATA_NAME "icudata"
#define DATA_TYPE "dat"

#define DATA_COPYRIGHT "\n" \
    "*******************************************************************************\n" \
    "* COPYRIGHT:                                                                  *\n" \
    "*   (C) Copyright International Business Machines Corporation, 1999           *\n" \
    "*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *\n" \
    "*   US Government Users Restricted Rights - Use, duplication, or disclosure   *\n" \
    "*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *\n" \
    "*******************************************************************************\n"

/* UDataInfo cf. udata.h */
static const UDataInfo dataInfo={
    sizeof(UDataInfo),
    0,

    U_IS_BIG_ENDIAN,
    U_CHARSET_FAMILY,
    sizeof(UChar),
    0,

    0x43, 0x6d, 0x6e, 0x44,     /* dataFormat="CmnD" */
    1, 0, 0, 0,                 /* formatVersion */
    3, 0, 0, 0                  /* dataVersion */
};

static uint32_t maxSize;

static char stringStore[STRING_STORE_SIZE];
static uint32_t stringTop=0, basenameTotal=0;

typedef struct {
    char *pathname, *basename;
    uint32_t basenameLength, basenameOffset, fileSize, fileOffset;
} File;

static File files[MAX_FILE_COUNT];
static uint32_t fileCount=0;

/* prototypes --------------------------------------------------------------- */

static void
addFile(const char *filename);

static char *
allocString(uint32_t length);

static int
compareFiles(const void *file1, const void *file2);

/* -------------------------------------------------------------------------- */

extern int
main(int argc, char *argv[]) {
    static uint8_t buffer[4096];
    char line[512];
    FileStream *in, *file;
    UNewDataMemory *out;
    char *s;
    UErrorCode errorCode=U_ZERO_ERROR;
    uint32_t i, fileOffset, basenameOffset, length;

    if(argc<=1) {
        fprintf(stderr,
            "usage: %s maxsize [list-filename]\n"
            "\tread the list file (default: stdin) and \n"
            "\tcreate " COMMON_DATA_NAME "." DATA_TYPE " from all the files listed but each not larger than maxsize\n",
            argv[0]);
    }

    if(argc<2) {
        return U_ILLEGAL_ARGUMENT_ERROR;
    }

    maxSize=icu_strtoul(argv[1], NULL, 0);
    if(maxSize==0) {
        fprintf(stderr, "gencmn: maxSize %s not valid\n", argv[1]);
        exit(U_ILLEGAL_ARGUMENT_ERROR);
    }

    if(argc==2) {
        in=T_FileStream_stdin();
    } else {
        in=T_FileStream_open(argv[2], "r");
        if(in==NULL) {
            fprintf(stderr, "gencmn: unable to open input file %s\n", argv[2]);
            exit(U_FILE_ACCESS_ERROR);
        }
    }

    /* read the list of files and get their lengths */
    while(T_FileStream_readLine(in, line, sizeof(line))!=NULL) {
        /* remove trailing newline characters */
        s=line;
        while(*s!=0) {
            if(*s=='\r' || *s=='\n') {
                *s=0;
                break;
            }
            ++s;
        }

        addFile(getLongPathname(line));
    }

    if(in!=T_FileStream_stdin()) {
        T_FileStream_close(in);
    }

    /* sort the files by basename */
    qsort(files, fileCount, sizeof(File), compareFiles);

    /* determine the offsets of all basenames and files in this common one */
    basenameOffset=4+8*fileCount;
    fileOffset=basenameOffset+(basenameTotal+15)&~0xf;
    for(i=0; i<fileCount; ++i) {
        files[i].fileOffset=fileOffset;
        fileOffset+=(files[i].fileSize+15)&~0xf;
        files[i].basenameOffset=basenameOffset;
        basenameOffset+=files[i].basenameLength;
    }

    /* create the output file */
    out=udata_create(DATA_TYPE, COMMON_DATA_NAME, &dataInfo, DATA_COPYRIGHT, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "gencmn: unable to open output file - error %s\n", errorName(errorCode));
        exit(errorCode);
    }

    /* write the table of contents */
    udata_write32(out, fileCount);
    for(i=0; i<fileCount; ++i) {
        udata_write32(out, files[i].basenameOffset);
        udata_write32(out, files[i].fileOffset);
    }

    /* write the basenames */
    for(i=0; i<fileCount; ++i) {
        udata_writeString(out, files[i].basename, files[i].basenameLength);
    }
    length=4+8*fileCount+basenameTotal;

    /* copy the files */
    for(i=0; i<fileCount; ++i) {
        /* pad to 16-align the next file */
        length&=0xf;
        if(length!=0) {
            length=16-length;
            icu_memset(buffer, 0, length);
            udata_writeBlock(out, buffer, length);
        }

        /* copy the next file */
        file=T_FileStream_open(files[i].pathname, "rb");
        if(file==NULL) {
            fprintf(stderr, "gencmn: unable to open listed file %s\n", files[i].pathname);
            exit(U_FILE_ACCESS_ERROR);
        }
        for(;;) {
            length=T_FileStream_read(file, buffer, sizeof(buffer));
            if(length==0) {
                break;
            }
            udata_writeBlock(out, buffer, length);
        }
        T_FileStream_close(file);
        length=files[i].fileSize;
    }

    /* finish */
    udata_finish(out, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "gencmn: error finishing output file - %s\n", errorName(errorCode));
        exit(errorCode);
    }

    return 0;
}

static void
addFile(const char *filename) {
    FileStream *file;
    char *s;
    uint32_t length;

    if(fileCount==MAX_FILE_COUNT) {
        fprintf(stderr, "gencmn: too many files\n");
        exit(U_BUFFER_OVERFLOW_ERROR);
    }

    /* try to open the file */
    file=T_FileStream_open(filename, "rb");
    if(file==NULL) {
        fprintf(stderr, "gencmn: unable to open listed file %s\n", filename);
        exit(U_FILE_ACCESS_ERROR);
    }

    /* get the file length */
    length=T_FileStream_size(file);
    if(T_FileStream_error(file) || length<=20) {
        fprintf(stderr, "gencmn: unable to get length of listed file %s\n", filename);
        exit(U_FILE_ACCESS_ERROR);
    }
    T_FileStream_close(file);

    /* do not add files that are longer than maxSize */
    if(length>maxSize) {
        return;
    }
    files[fileCount].fileSize=length;

    /* store the pathname */
    length=icu_strlen(filename)+1;
    s=allocString(length);
    icu_memcpy(s, filename, length);
    files[fileCount].pathname=s;

    /* get the basename */
    s=(char *)findBasename(s);
    files[fileCount].basename=s;
    length=icu_strlen(s)+1;
    files[fileCount].basenameLength=length;
    basenameTotal+=length;

    ++fileCount;
}

static char *
allocString(uint32_t length) {
    uint32_t top=stringTop+length;
    char *p;

    if(top>STRING_STORE_SIZE) {
        fprintf(stderr, "gencmn: out of memory\n");
        exit(U_MEMORY_ALLOCATION_ERROR);
    }
    p=stringStore+stringTop;
    stringTop=top;
    return p;
}

static int
compareFiles(const void *file1, const void *file2) {
    /* sort by basename */
    return icu_strcmp(((File *)file1)->basename, ((File *)file2)->basename);
}
