/*
*******************************************************************************
*
*   Copyright (C) 1998-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File genrb.c
*
* Modification History:
*
*   Date        Name        Description
*   05/25/99    stephen     Creation.
*   5/10/01     Ram         removed ustdio dependency
*******************************************************************************
*/

#include <stdio.h>
#include "unicode/utypes.h"
#include "unicode/putil.h"
#include "cmemory.h"
#include "cstring.h"
#include "filestrm.h"


#include "ucbuf.h"
#include "error.h"
#include "parse.h"
#include "util.h"
#include "reslist.h"


#include "toolutil.h"
#include "uoptions.h"

#include "unicode/ucol.h"
#include "unicode/uloc.h"

/* Protos */
static void  processFile(const char *filename, const char* cp, const char *inputDir, const char *outputDir, UErrorCode *status);
static char *make_res_filename(const char *filename, const char *outputDir, UErrorCode *status);

/* File suffixes */
#define RES_SUFFIX ".res"
#define COL_SUFFIX ".col"

/* The version of genrb */
#define GENRB_VERSION "3.0"

const char *gCurrentFileName;

enum
{
    HELP1,
    HELP2,
    VERBOSE,
    VERSION,
    SOURCEDIR,
    DESTDIR,
    ENCODING,
    ICUDATADIR
};

UOption options[]={
                      UOPTION_HELP_H,
                      UOPTION_HELP_QUESTION_MARK,
                      UOPTION_VERBOSE,
                      UOPTION_VERSION,
                      UOPTION_SOURCEDIR,
                      UOPTION_DESTDIR,
                      UOPTION_ENCODING,
                      UOPTION_ICUDATADIR
                  };


#ifdef XP_MAC_CONSOLE
#include <console.h>
#endif

int
main(int argc,
     char* argv[]) {
    UErrorCode  status    = U_ZERO_ERROR;
    const char *arg       = NULL;
    const char *outputDir = NULL; /* NULL = no output directory, use current */
    const char *inputDir  = NULL;
    const char *encoding  = "";
    UBool       verbose;
    int         i;

#ifdef XP_MAC_CONSOLE

    argc = ccommand((char***)&argv);
#endif

    argc = u_parseArgs(argc, argv, (int32_t)(sizeof(options)/sizeof(options[0])), options);

    /* error handling, printing usage message */
    if(argc<0) {
        fprintf(stderr, "error in command line argument \"%s\"\n", argv[-argc]);
    } else if(argc<2) {
        argc = -1;
    }

    if(options[VERSION].doesOccur) {
        fprintf(stderr,
                "%s version %s (ICU version %s).\n"
                "%s\n",
                argv[0], GENRB_VERSION, U_ICU_VERSION, U_COPYRIGHT_STRING);
        return U_ZERO_ERROR;
    }

    if(argc<0 || options[HELP1].doesOccur || options[HELP2].doesOccur) {
        fprintf(stderr,
                "Usage: %s [OPTIONS] [FILES]\n"
                "\treads the list of resource bundle source files and creates\n"
                "\tbinary version of reosurce bundles (.res files)\n"
                "\tOptions:\n"
                "\t\t-h, -? or --help     this usage text\n"
                "\t\t-V or --version      prints out version number and exits\n"
                "\t\t-d of --destdir      destination directory, followed by the path, defaults to %s\n"
                "\t\t-v or --verbose      be verbose\n"
                "\t\t-e or --encoding     encoding of source files, leave empty for system default encoding\n"
                "\t\t                     NOTE: ICU must be completely built to use this option\n"
                "\t\t-s or --sourcedir    source directory for files followed by path, defaults to %s\n"
                "\t\t-i or --icudatadir   directory for locating any needed intermediate data files,\n"
                "\t\t                     followed by path, defaults to %s\n",
                argv[0], u_getDataDirectory(), u_getDataDirectory(),u_getDataDirectory());
        return argc < 0 ? U_ILLEGAL_ARGUMENT_ERROR : U_ZERO_ERROR;
    }

    if(options[VERBOSE].doesOccur) {
        verbose = TRUE;
    }

    if(options[SOURCEDIR].doesOccur) {
        inputDir = options[SOURCEDIR].value;
    }

    if(options[DESTDIR].doesOccur) {
        outputDir = options[DESTDIR].value;
    }

    if(options[ENCODING].doesOccur) {
        encoding = options[ENCODING].value;
    }

    if(options[ICUDATADIR].doesOccur) {
        u_setDataDirectory(options[ICUDATADIR].value);
    }

    initParser();

    /* generate the binary files */
    for(i = 1; i < argc; ++i) {
        status = U_ZERO_ERROR;
        arg    = getLongPathname(argv[i]);

        printf("genrb: processing file \"%s\"\n", arg);

        processFile(arg, encoding, inputDir, outputDir, &status);
    }

    return status;
}

/* Process a file */
static void
processFile(const char *filename, const char *cp, const char *inputDir, const char *outputDir, UErrorCode *status) {
    FileStream     *in           = NULL;
    struct SRBRoot *data         = NULL;
    UCHARBUF       *ucbuf        = NULL;
    char           *rbname       = NULL;
    char           *openFileName = NULL;

    if (U_FAILURE(*status)) {
        return;
    }

    /* Setup */
    in = 0;

    /* Open the input file for reading */
    if(inputDir == NULL) {
        in = T_FileStream_open(filename, "rb");
    } else {
        int32_t dirlen  = (int32_t)uprv_strlen(inputDir);
        int32_t filelen = (int32_t)uprv_strlen(filename);
        if(inputDir[dirlen-1] != U_FILE_SEP_CHAR) {
            openFileName = (char *) uprv_malloc(dirlen + filelen + 2);

            uprv_strcpy(openFileName, inputDir);
            openFileName[dirlen]     = U_FILE_SEP_CHAR;
            openFileName[dirlen + 1] = '\0';
            uprv_strcat(openFileName, filename);
        } else {
            openFileName = (char *) uprv_malloc(dirlen + filelen + 1);
            uprv_strcpy(openFileName, inputDir);
            uprv_strcat(openFileName, filename);
        }
        in = T_FileStream_open(openFileName, "rb");
    }

    if(in == 0) {
        *status = U_FILE_ACCESS_ERROR;
        fprintf(stderr, "Couldn't open file %s", openFileName == NULL ? filename : openFileName);
        goto finish;
    } else {
        /* auto detect popular encodings */
        if (ucbuf_autodetect(in, &cp)) {
            printf("Autodetected encoding %s\n", cp);
        }
    }

    ucbuf = ucbuf_open(in, status);

    if (ucbuf == NULL || U_FAILURE(*status)) {
        goto finish;
    }

    /* Parse the data into an SRBRoot */
    gCurrentFileName = filename;
    data = parse(ucbuf, inputDir, status);

    if (data == NULL || U_FAILURE(*status)) {
        goto finish;
    }

    /* Determine the target rb filename */
    rbname = make_res_filename(filename, outputDir, status);
    if(U_FAILURE(*status)) {
        goto finish;
    }

    /* Write the data to the file */
    bundle_write(data, outputDir, status);
    bundle_close(data, status);

finish:
    if (openFileName != NULL) {
        uprv_free(openFileName);
    }

    if(ucbuf) {
        ucbuf_close(ucbuf);
    }

    /* Clean up */
    T_FileStream_close(in);

    if (rbname) {
        uprv_free(rbname);
    }
}

/* Generate the target .res file name from the input file name */
static char*
make_res_filename(const char *filename,
                  const char *outputDir,
                  UErrorCode *status) {
    char *basename;
    char *dirname;
    char *resName;

    if (U_FAILURE(*status)) {
        return 0;
    }

    /* setup */
    basename = dirname = resName = 0;

    /* determine basename, and compiled file names */
    basename = (char*) uprv_malloc(sizeof(char) * (uprv_strlen(filename) + 1));
    if(basename == 0) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        goto finish;
    }

    get_basename(basename, filename);

    dirname = (char*) uprv_malloc(sizeof(char) * (uprv_strlen(filename) + 1));
    if(dirname == 0) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        goto finish;
    }

    get_dirname(dirname, filename);

    if (outputDir == NULL) {
        /* output in same dir as .txt */
        resName = (char*) uprv_malloc(sizeof(char) * (uprv_strlen(dirname)
                                      + uprv_strlen(basename)
                                      + uprv_strlen(RES_SUFFIX) + 1));
        if(resName == 0) {
            *status = U_MEMORY_ALLOCATION_ERROR;
            goto finish;
        }

        uprv_strcpy(resName, dirname);
        uprv_strcat(resName, basename);
    } else {
        int32_t dirlen      = (int32_t)uprv_strlen(outputDir);
        int32_t basenamelen = (int32_t)uprv_strlen(basename);

        resName = (char*) uprv_malloc(sizeof(char) * (dirlen + basenamelen + 2));

        if (resName == NULL) {
            *status = U_MEMORY_ALLOCATION_ERROR;
            goto finish;
        }

        uprv_strcpy(resName, outputDir);

        if(outputDir[dirlen] != U_FILE_SEP_CHAR) {
            resName[dirlen]     = U_FILE_SEP_CHAR;
            resName[dirlen + 1] = '\0';
        }

        uprv_strcat(resName, basename);
    }

finish:
    uprv_free(basename);
    uprv_free(dirname);

    return resName;
}

/*
 * Local Variables:
 * indent-tabs-mode: nil
 * End:
 */
