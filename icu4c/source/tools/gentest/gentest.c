/*
*******************************************************************************
*
*   Copyright (C) 1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  genprops.c
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
#include "unicode/udata.h"
#include "unewdata.h"
#include "cmemory.h"
#include "cstring.h"

#define DATA_NAME "test"
#define DATA_TYPE "dat"

/* UDataInfo cf. udata.h */
static const UDataInfo dataInfo={
    sizeof(UDataInfo),
    0,

    U_IS_BIG_ENDIAN,
    U_CHARSET_FAMILY,
    sizeof(UChar),
    0,

    0x54, 0x65, 0x73, 0x74,     /* dataFormat="Test" */
    1, 0, 0, 0,                 /* formatVersion */
    1, 0, 0, 0                  /* dataVersion */
};

static void usage();
static void createData();
static bool_t beVerbose=FALSE, haveCopyright=FALSE;

int
main(int argc, char *argv[]) {

  int printUsage = 0;
  int option = 1;
  char *arg;
  char *outputDir = NULL; /* NULL = no output directory, use the default one(data) */
  
  if(argc > 3)
      printUsage=1;
   
   /* parse the options */
   for(option = 1; option < argc; ++option) {
    arg = argv[option];
    
    /* usage info */
    if(uprv_strcmp(arg, "-h") == 0 || uprv_strcmp(arg, "--help") == 0) {
      printUsage = 1;
    }

    else if(uprv_strcmp(arg, "-D") == 0 || uprv_strcmp(arg, "--dir") == 0) {
        outputDir = argv[++option];
    }

    /* all arguments after -- are not options */
    else if(uprv_strcmp(arg, "--") == 0) {
      /* skip the -- */
      ++option;
      break;
    }
    /* unrecognized option */
    else if(uprv_strncmp(arg, "-", uprv_strlen("-")) == 0) {
      printf("genrb: invalid option -- %s\n", arg+1);
      printUsage = 1;
    }
    /* done with options, start Generating the memory mapped file */
    else {
      break;
    }
  }

  /* print usage info */
  if(printUsage) {
    usage();
    return 0;
  }
  printf("Generating the test memory mapped file"); 
  createData(outputDir);
  return 0;
}
/* Usage information */
static void
usage()
{  
  /*("Usage: gentest [OPTIONS] [DIR]");*/
  fprintf(stderr, "Usage: gentest [OPTIONS] [DIR]\n"
                  "\tCreates the memory mapped file \"" DATA_NAME "." DATA_TYPE "\" for testing purpose\n"
                  "Options: \n"
                  "\t-h, --help        Print this message and exit.\n"
                  "\t-D, --dir         Store the created memory mapped file under 'dir'.\n");

}

/* Create data file ----------------------------------------------------- */
static void
createData(const char* outputDirectory) {
    UNewDataMemory *pData;
    UErrorCode errorCode=U_ZERO_ERROR;
    char stringValue[]={'Y', 'E', 'A', 'R', '\0'};
    uint16_t intValue=2000;
    
    long dataLength;
    uint32_t size;

    pData=udata_create(outputDirectory, DATA_TYPE, DATA_NAME, &dataInfo,
                       haveCopyright ? U_COPYRIGHT_STRING : NULL, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "gentest: unable to create data memory, error %d\n", errorCode);
        exit(errorCode);
    }

    /* write the data to the file */
    /* a 16 bit value  and a String*/
    udata_write16(pData, intValue);
    udata_writeString(pData, stringValue, sizeof(stringValue));

    /* finish up */
    dataLength=udata_finish(pData, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "gentest: error %d writing the output file\n", errorCode);
        exit(errorCode);
    }
    size=sizeof(stringValue) + sizeof(intValue);


    if(dataLength!=(long)size) {
        fprintf(stderr, "gentest: data length %ld != calculated size %lu\n", dataLength, size);
        exit(U_INTERNAL_PROGRAM_ERROR);
    }
}
