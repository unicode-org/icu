/*
*******************************************************************************
*
*   Copyright (C) 1998-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File genrb.c
*
* Modification History:
*
*   Date        Name        Description
*   05/25/99    stephen	    Creation.
*******************************************************************************
*/

#include <stdio.h>

#include "unicode/utypes.h"
#include "cmemory.h"
#include "cstring.h"
#include "filestrm.h"

U_CDECL_BEGIN

#include "error.h"
#include "parse.h"
#include "write.h"
#include "util.h"

U_CDECL_END
#include "toolutil.h"

#include "unicode/ucol.h"
#include "unicode/uloc.h"

/* Protos */
static void usage(void);
static void version(void);
static void processFile(const char *filename, const char* cp, const char *outputDir, UErrorCode *status);
static char* make_res_filename(const char *filename, const char *outputDir, UErrorCode *status);
static char* make_col_filename(const char *filename, const char *outputDir, UErrorCode *status);
static void make_col(const char *filename, UErrorCode *status);
int main(int argc, char **argv);

/* File suffixes */
#define RES_SUFFIX ".res"
#define COL_SUFFIX ".col"

/* The version of genrb */
#define GENRB_VERSION "1.0"

  char *encoding = ""; 


int
main(int argc,
     char **argv)
{
  int printUsage = 0;
  int printVersion = 0;
  int useConversionLibrary = 0;
  int optind = 1;
  int i;
  char *arg;
  UErrorCode status;
  char *outputDir = NULL; /* NULL = no output directory, use current */


  if(argc == 1)
      printUsage = 1;


  /* parse the options */
  for(optind = 1; optind < argc; ++optind) {
    arg = argv[optind];
    
    /* version info */
    if(uprv_strcmp(arg, "-v") == 0 || uprv_strcmp(arg, "--version") == 0) {
      printVersion = 1;
    }
    /* usage info */
    else if(uprv_strcmp(arg, "-h") == 0 || uprv_strcmp(arg, "--help") == 0) {
      printUsage = 1;
    }

    else if(uprv_strncmp(arg, "-e", 2) == 0) {
        useConversionLibrary = 1;
        if(uprv_strlen(arg) > uprv_strlen("-e")) {
            encoding = arg+2;
        } else {
            encoding = 0;
        }

    }
    else if(uprv_strncmp(arg, "-D", 2) == 0) {
        outputDir = arg+2;
    }

    /* POSIX.1 says all arguments after -- are not options */
    else if(uprv_strcmp(arg, "--") == 0) {
      /* skip the -- */
      ++optind;
      break;
    }
    /* unrecognized option */
    else if(uprv_strncmp(arg, "-", uprv_strlen("-")) == 0) {
      printf("genrb: invalid option -- %s\n", arg+1);
      printUsage = 1;
    }
    /* done with options, start file processing */
    else {
      break;
    }
  }

  /* print usage info */
  if(printUsage) {
    usage();
    return 0;
  }

  /* print version info */
  if(printVersion) {
    version();
    return 0;
  }

  /* generate the binary files */
  for(i = optind; i < argc; ++i) {
    status = U_ZERO_ERROR;
    arg = getLongPathname(argv[i]);
    processFile(arg, encoding, outputDir, &status);
    make_col(arg, &status);
    if(U_FAILURE(status)) {
      printf("genrb: %s processing file \"%s\"\n", u_errorName(status), arg);
      if(getErrorText() != 0)
	printf("       (%s)\n", getErrorText());
    }
  }

  return 0;
}

/* Usage information */
static void
usage()
{  
  puts("Usage: genrb [OPTIONS] [FILES]");
  puts("Options:");
  puts("  -e                Resource bundle is encoded with system default encoding");
  puts("  -eEncoding        Resource bundle uses specified Encoding");
  puts("  -h, --help        Print this message and exit.");
  puts("  -v, --version     Print the version number of genrb and exit.");
  puts("  -Ddir             Store ALL output files under 'dir'.");
  encoding!=NULL?puts(encoding):puts("encoding is NULL");
}

/* Version information */
static void
version()
{
  printf("genrb version %s (ICU version %s).\n", 
	 GENRB_VERSION, U_ICU_VERSION);
  puts(U_COPYRIGHT_STRING);
}

/* Process a file */
static void
processFile(const char *filename, const char *cp, const char *outputDir,
	    UErrorCode *status)
{
  FileStream *in;
  FileStream *rb_out;
  struct SRBItemList *data;
  char *rbname;

  if(U_FAILURE(*status)) return;

  /* Setup */
  in = rb_out = 0;

  /* Open the input file for reading */
  in = T_FileStream_open(filename, "r");
  if(in == 0) {
    *status = U_FILE_ACCESS_ERROR;
    setErrorText("File not found");
    return;
  }

  /* Parse the data into an SRBItemList */
  data = parse(in, cp, status);

  /* Determine the target rb filename */
  rbname = make_res_filename(filename, outputDir,  status);
  if(U_FAILURE(*status)) {
    goto finish;
  }

  /* Open the target file  for writing */
  rb_out = T_FileStream_open(rbname, "wb");
  if(rb_out == 0 || T_FileStream_error(rb_out) != 0) {
    *status = U_FILE_ACCESS_ERROR;
    setErrorText("Could not open file for writing");
    goto finish;
  }

  /* Write the data to the file */
  rb_write(rb_out, data, status);

 finish:

  /* Clean up */
  rblist_close(data, status);
  T_FileStream_close(in);
  T_FileStream_close(rb_out);

  uprv_free(rbname);
}

/* Generate the target .res file name from the input file name */
static char*
make_res_filename(const char *filename,
                  const char *outputDir,
		 UErrorCode *status)
{
  char *basename;
  char *dirname;
  char *resName;

  if(U_FAILURE(*status)) return 0;

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

  if ( outputDir == NULL )
  {
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
      uprv_strcat(resName, RES_SUFFIX);
  }
  else
  {
      /* output in 'outputDir'  */
      resName = (char*) uprv_malloc(sizeof(char) * (uprv_strlen(outputDir)
                                                    + uprv_strlen(basename) 
                                                    + uprv_strlen(RES_SUFFIX) + 1));
      if(resName == 0) {
          *status = U_MEMORY_ALLOCATION_ERROR;
          goto finish;
      }
      uprv_strcpy(resName, outputDir);
      uprv_strcat(resName, basename);
      uprv_strcat(resName, RES_SUFFIX);
      
  }

 finish:
  uprv_free(basename);
  uprv_free(dirname);

  return resName;
}

/* Generate the target .col file name from the input file name */
static char*
make_col_filename(const char *filename,
                  const char *outputDir,
		 UErrorCode *status)
{
  char *basename;
  char *dirname;
  char *colName;

  if(U_FAILURE(*status)) return 0;

  /* setup */
  basename = dirname = colName = 0;

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

  if(outputDir == NULL)
  {
      
      get_dirname(dirname, filename);
      
      colName = (char*) uprv_malloc(sizeof(char) * (uprv_strlen(dirname)
                                                    + uprv_strlen(basename) 
                                                    + uprv_strlen(COL_SUFFIX) + 1));
      if(colName == 0) {
          *status = U_MEMORY_ALLOCATION_ERROR;
          goto finish;
      }
      uprv_strcpy(colName, dirname);
      uprv_strcat(colName, basename);
      uprv_strcat(colName, COL_SUFFIX);

  }
  else
  {
      
      get_dirname(dirname, filename);
      
      colName = (char*) uprv_malloc(sizeof(char) * (uprv_strlen(outputDir)
                                                    + uprv_strlen(basename) 
                                                    + uprv_strlen(COL_SUFFIX) + 1));
      if(colName == 0) {
          *status = U_MEMORY_ALLOCATION_ERROR;
          goto finish;
      }
      uprv_strcpy(colName, outputDir);
      uprv_strcat(colName, basename);
      uprv_strcat(colName, COL_SUFFIX);
  }

 finish:
  uprv_free(basename);
  uprv_free(dirname);

  return colName;
}

static void make_col(const char *filename, UErrorCode *status)
{
    char *basename;
    UCollator *coll;

    basename = (char*) uprv_malloc(sizeof(char) * (uprv_strlen(filename) + 1));
    if(basename == 0) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
    get_basename(basename, filename);
    
    coll = ucol_open(basename, status);
    if(U_FAILURE(*status)) {
      printf("gencol: %s for locale \"%s\"", u_errorName(*status), basename);
    }
    else {
      ucol_close(coll);
    }
}
