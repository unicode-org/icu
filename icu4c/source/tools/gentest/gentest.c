/*
*******************************************************************************
*
*   Copyright (C) 1999-2004, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  gentest.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000mar03
*   created by: Madhu Katragadda
*
*   This program writes a little data file for testing the udata API.
*/

#include <stdio.h>
#include <stdlib.h>
#include "unicode/utypes.h"
#include "unicode/putil.h"
#include "unicode/uclean.h"
#include "unicode/udata.h"
#include "unewdata.h"
#include "cmemory.h"
#include "cstring.h"
#include "uoptions.h"
#include "gentest.h"

#define DATA_PKG  "testdata"
#define DATA_NAME "test"
#define DATA_TYPE "icu"

/* UDataInfo cf. udata.h */
static const UDataInfo dataInfo={
    sizeof(UDataInfo),
    0,

    U_IS_BIG_ENDIAN,
    U_CHARSET_FAMILY,
    sizeof(UChar),
    0,

    {0x54, 0x65, 0x73, 0x74},     /* dataFormat="Test" */
    {1, 0, 0, 0},                 /* formatVersion */
    {1, 0, 0, 0}                  /* dataVersion */
};

static void createData(const char*, UErrorCode *);

static UOption options[]={
  /*0*/ UOPTION_HELP_H,
  /*1*/ UOPTION_HELP_QUESTION_MARK,
  /*2*/ UOPTION_DESTDIR,
  /*3*/ UOPTION_DEF("genres", 'r', UOPT_NO_ARG)
};

extern int
main(int argc, char* argv[]) {
    UErrorCode errorCode = U_ZERO_ERROR;

    /* preset then read command line options */
    options[2].value=u_getDataDirectory();
    argc=u_parseArgs(argc, argv, sizeof(options)/sizeof(options[0]), options);

    /* error handling, printing usage message */
    if(argc<0) {
        fprintf(stderr,
            "error in command line argument \"%s\"\n",
            argv[-argc]);
    }
    if(argc<0 || options[0].doesOccur || options[1].doesOccur) {
        fprintf(stderr,
            "usage: %s [-options]\n"
            "\tcreate the test file " DATA_PKG "_" DATA_NAME "." DATA_TYPE " unless the -r option is given.\n"
            "\toptions:\n"
            "\t\t-h or -? or --help  this usage text\n"
            "\t\t-d or --destdir     destination directory, followed by the path\n"
            "\t\t-r or --genres      generate resource file testtable32.txt instead of UData test \n",
            argv[0]);
        return argc<0 ? U_ILLEGAL_ARGUMENT_ERROR : U_ZERO_ERROR;
    }

    if ( options[3].doesOccur ) {
        return genres32( argv[0], options[2].value );
    } else { 
        /* printf("Generating the test memory mapped file\n"); */
        createData(options[2].value, &errorCode);
    }
    return U_FAILURE(errorCode);
}

/* Create data file ----------------------------------------------------- */
static void
createData(const char* outputDirectory, UErrorCode *errorCode) {
    UNewDataMemory *pData;
    char stringValue[]={'Y', 'E', 'A', 'R', '\0'};
    uint16_t intValue=2000;

    long dataLength;
    uint32_t size;

    pData=udata_create(outputDirectory, DATA_TYPE, DATA_PKG "_" DATA_NAME, &dataInfo,
                       U_COPYRIGHT_STRING, errorCode);
    if(U_FAILURE(*errorCode)) {
        fprintf(stderr, "gentest: unable to create data memory, error %d\n", *errorCode);
        exit(*errorCode);
    }

    /* write the data to the file */
    /* a 16 bit value  and a String*/
    udata_write16(pData, intValue);
    udata_writeString(pData, stringValue, sizeof(stringValue));

    /* finish up */
    dataLength=udata_finish(pData, errorCode);
    if(U_FAILURE(*errorCode)) {
        fprintf(stderr, "gentest: error %d writing the output file\n", *errorCode);
        exit(*errorCode);
    }
    size=sizeof(stringValue) + sizeof(intValue);


    if(dataLength!=(long)size) {
        fprintf(stderr, "gentest: data length %ld != calculated size %lu\n",
            dataLength, (unsigned long)size);
        exit(U_INTERNAL_PROGRAM_ERROR);
    }
}
