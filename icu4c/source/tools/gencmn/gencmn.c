/*
*******************************************************************************
*
*   Copyright (C) 1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
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
#include "unicode/utypes.h"
#include "cmemory.h"
#include "cstring.h"
#include "filestrm.h"
#include "toolutil.h"
#include "unewdata.h"
#include "uoptions.h"

#define STRING_STORE_SIZE 100000
#define MAX_FILE_COUNT 2000

#define COMMON_DATA_NAME "icudata"
#define DATA_TYPE "dat"

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
addFile(const char *filename, UBool sourceTOC);

static char *
allocString(uint32_t length);

static int
compareFiles(const void *file1, const void *file2);

/* -------------------------------------------------------------------------- */

static UOption options[]={
    UOPTION_HELP_H,
    UOPTION_HELP_QUESTION_MARK,
    UOPTION_VERBOSE,
    UOPTION_COPYRIGHT,
    UOPTION_DESTDIR,
    UOPTION_DEF( "comment", 'C', UOPT_REQUIRES_ARG),
    UOPTION_DEF( "name", 'n', UOPT_REQUIRES_ARG),
    UOPTION_DEF( "type", 't', UOPT_REQUIRES_ARG),
    UOPTION_DEF( "source", 'S', UOPT_NO_ARG)
};

extern int
main(int argc, const char *argv[]) {
    static uint8_t buffer[4096];
    char line[512];
    const char *destdir = 0;
    FileStream *in, *file;
    UNewDataMemory *out;
    char *s;
    UErrorCode errorCode=U_ZERO_ERROR;
    uint32_t i, fileOffset, basenameOffset, length;
    UBool sourceTOC;

    /* preset then read command line options */
    options[4].value=u_getDataDirectory();
    options[6].value=COMMON_DATA_NAME;
    options[7].value=DATA_TYPE;
    argc=u_parseArgs(argc, argv, sizeof(options)/sizeof(options[0]), options);

    /* error handling, printing usage message */
    if(argc<0) {
        fprintf(stderr,
            "error in command line argument \"%s\"\n",
            argv[-argc]);
    } else if(argc<2) {
        argc=-1;
    }
    if(argc<0 || options[0].doesOccur || options[1].doesOccur) {
        fprintf(stderr,
            "usage: %s [-options] maxsize [list-filename]\n"
            "\tread the list file (default: stdin) and \n"
            "\tcreate a common data file from all the files listed but each not larger than maxsize\n"
            "\toptions:\n"
            "\t\t-h or -? or --help  this usage text\n"
            "\t\t-v or --verbose     verbose output\n"
            "\t\t-c or --copyright   include the ICU copyright notice\n"
            "\t\t-C or --comment     include a comment string\n"
            "\t\t-d or --destdir     destination directory, followed by the path\n"
            "\t\t-n or --name        name of the destination file, defaults to " COMMON_DATA_NAME "\n"
            "\t\t-t or --type        type of the destination file, defaults to " DATA_TYPE "\n"
            "\t\t-S or --source      write a .c source file with the table of contents\n",
            argv[0]);
        return argc<0 ? U_ILLEGAL_ARGUMENT_ERROR : U_ZERO_ERROR;
    }

    sourceTOC=options[8].doesOccur;

    maxSize=uprv_strtoul(argv[1], NULL, 0);
    if(maxSize==0 && !sourceTOC) {
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

    if(sourceTOC) {
        printf("Generating %s_%s.c Table of Contents source file\n", options[6].value, options[7].value);
    } else {
        printf("Generating %s.%s common data file with Table of Contents\n", options[6].value, options[7].value);
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

        addFile(getLongPathname(line), sourceTOC);
    }

    if(in!=T_FileStream_stdin()) {
        T_FileStream_close(in);
    }

    if(fileCount==0) {
        printf("gencmn: no files listed in %s\n", argc==2 ? "<stdin>" : argv[2]);
        return 0;
    }

    /* sort the files by basename */
    qsort(files, fileCount, sizeof(File), compareFiles);

    if(!sourceTOC) {
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
        out=udata_create(options[4].value, options[7].value, options[6].value,
                         &dataInfo,
                         options[3].doesOccur ? U_COPYRIGHT_STRING : options[5].value,
                         &errorCode);
        if(U_FAILURE(errorCode)) {
            fprintf(stderr, "gencmn: udata_create(-d %s -n %s -t %s) failed - %s\n",
                options[4].value, options[6].value, options[7].value,
                u_errorName(errorCode));
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
                udata_writePadding(out, 16-length);
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
            fprintf(stderr, "gencmn: udata_finish() failed - %s\n", u_errorName(errorCode));
            exit(errorCode);
        }
    } else {
        /* write a .c source file with the table of contents */
        char buffer[1000];
        char *filename, *s;
        FileStream *out;

        /* create the output filename */
        filename=s=buffer;
        uprv_strcpy(filename, options[4].value);
        s=filename+uprv_strlen(filename);
        if(s>filename && *(s-1)!=U_FILE_SEP_CHAR) {
            *s++=U_FILE_SEP_CHAR;
        }
        uprv_strcpy(s, options[6].value);
        if(*(options[7].value)!=0) {
            s+=uprv_strlen(s);
            *s++='_';
            uprv_strcpy(s, options[7].value);
        }
        s+=uprv_strlen(s);
        uprv_strcpy(s, ".c");

        /* open the output file */
        out=T_FileStream_open(filename, "w");
        if(out==NULL) {
            fprintf(stderr, "gencmn: unable to open .c output file %s\n", filename);
            exit(U_FILE_ACCESS_ERROR);
        }

        /* write the source file */
        sprintf(buffer,
            "/*\n"
            " * ICU common data table of contents for %s.%s ,\n"
            " * Automatically generated by icu/source/tools/gencmn/gencmn .\n"
            " */\n\n"
            "#include \"unicode/utypes.h\"\n"
            "#include \"unicode/udata.h\"\n"
            "\n"
            "/* external symbol declarations for data */\n",
            options[6].value, options[7].value);
        T_FileStream_writeLine(out, buffer);

        sprintf(buffer, "extern const char\n    %s[]", files[0].pathname);
        T_FileStream_writeLine(out, buffer);
        for(i=1; i<fileCount; ++i) {
            sprintf(buffer, ",\n    %s[]", files[i].pathname);
            T_FileStream_writeLine(out, buffer);
        }
        T_FileStream_writeLine(out, ";\n\n");

        sprintf(
            buffer,
            "U_CAPI const struct {\n"
            "    uint16_t headerSize;\n"
            "    uint8_t magic1, magic2;\n"
            "    UDataInfo info;\n"
            "    char padding[%lu];\n"
            "    uint32_t count, reserved;\n"
            "    struct {\n"
            "        const char *name;\n"
            "        const void *data;\n"
            "    } toc[%lu];\n"
            "} U_EXPORT2 icudata_dat = {\n"
            "    32, 0xda, 0x27, {\n"
            "        %lu, 0, \n"
            "        %u, %u, %u, 0, \n"
            "        0x54, 0x6f, 0x43, 0x50, \n"
            "        1, 0, 0, 0,\n"
            "        0, 0, 0, 0\n"
            "    },\n"
            "    \"\", %lu, 0, {\n",
            32-4-sizeof(UDataInfo),
            fileCount,
            sizeof(UDataInfo),
            U_IS_BIG_ENDIAN,
            U_CHARSET_FAMILY,
            U_SIZEOF_UCHAR,
            fileCount
        );
        T_FileStream_writeLine(out, buffer);

        sprintf(buffer, "        { \"%s\", %s }", files[0].basename, files[0].pathname);
        T_FileStream_writeLine(out, buffer);
        for(i=1; i<fileCount; ++i) {
            sprintf(buffer, ",\n        { \"%s\", %s }", files[i].basename, files[i].pathname);
            T_FileStream_writeLine(out, buffer);
        }
      
        T_FileStream_writeLine(out, "\n    }\n};\n");
        T_FileStream_close(out);
    }

    return 0;
}

static void
addFile(const char *filename, UBool sourceTOC) {
    char *s;
    uint32_t length;

    if(fileCount==MAX_FILE_COUNT) {
        fprintf(stderr, "gencmn: too many files, maximum is %d\n", MAX_FILE_COUNT);
        exit(U_BUFFER_OVERFLOW_ERROR);
    }

    if(!sourceTOC) {
        FileStream *file;

        /* store the pathname */
        length=uprv_strlen(filename)+1;
        s=allocString(length);
        uprv_memcpy(s, filename, length);
        files[fileCount].pathname=s;

        /* get the basename */
        s=(char *)findBasename(s);
        files[fileCount].basename=s;
        length=uprv_strlen(s)+1;
        files[fileCount].basenameLength=length;
        basenameTotal+=length;

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
    } else {
        char *t;

        /* get and store the basename */
        filename=findBasename(filename);
        length=uprv_strlen(filename)+1;
        s=allocString(length);
        uprv_memcpy(s, filename, length);
        files[fileCount].basename=s;

        /* turn the basename into an entry point name and store in the pathname field */
        t=files[fileCount].pathname=allocString(length);
        while(--length>0) {
            if(*s=='.' || *s=='-') {
                *t='_';
            } else {
                *t=*s;
            }
            ++s;
            ++t;
        }
        *t=0;
    }

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
    return uprv_strcmp(((File *)file1)->basename, ((File *)file2)->basename);
}

/*
 * Hey, Emacs, please set the following:
 *
 * Local Variables:
 * indent-tabs-mode: nil
 * End:
 *
 */
