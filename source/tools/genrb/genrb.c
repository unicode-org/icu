/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright International Business Machines Corporation, 1998, 1999     *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
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

#include "utypes.h"
#include "cmemory.h"
#include "cstring.h"
#include "filestrm.h"
#include "error.h"
#include "parse.h"
#include "write.h"
#include "util.h"


/* Protos */
static void usage();
static void version();
static void processFile(const char *filename, UErrorCode *status);
static char* make_res_filename(const char *filename, UErrorCode *status);
static char* make_col_filename(const char *filename, UErrorCode *status);
static const char* errorName(UErrorCode status);
int main(int argc, char **argv);

/* File suffixes */
#define RES_SUFFIX ".res"
#define COL_SUFFIX ".col"

/* The version of genrb */
#define GENRB_VERSION "1.0"


int
main(int argc,
     char **argv)
{
  int printUsage = 0;
  int printVersion = 0;
  int optind = 1;
  int i;
  char *arg;
  UErrorCode status;


  /* parse the options */
  for(optind = 1; optind < argc; ++optind) {
    arg = argv[optind];
    
    /* version info */
    if(icu_strcmp(arg, "-v") == 0 || icu_strcmp(arg, "--version") == 0) {
      printVersion = 1;
    }
    /* usage info */
    else if(icu_strcmp(arg, "-h") == 0 || icu_strcmp(arg, "--help") == 0) {
      printUsage = 1;
    }
    /* POSIX.1 says all arguments after -- are not options */
    else if(icu_strcmp(arg, "--") == 0) {
      /* skip the -- */
      ++optind;
      break;
    }
    /* unrecognized option */
    else if(icu_strncmp(arg, "-", icu_strlen("-")) == 0) {
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
    status = ZERO_ERROR;
    processFile(argv[i], &status);
    if(FAILURE(status)) {
      printf("genrb: %s processing file \"%s\"\n", errorName(status), argv[i]);
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
  puts("  -h, --help        Print this message and exit.");
  puts("  -v, --version     Print the version number of genrb and exit.");
}

/* Version information */
static void
version()
{
  printf("genrb version %s (ICU version %s), by Stephen F. Booth.\n", 
	 GENRB_VERSION, ICU_VERSION);
  puts("(C) Copyright International Business Machines Corporation, 1998, 1999");
  puts("Licensed Material - Program-Property of IBM - All Rights Reserved.");
  puts("US Government Users Restricted Rights - Use, duplication, or disclosure");
  puts("restricted by GSA ADP Schedule Contract with IBM Corp.");
}

/* Process a file */
static void
processFile(const char *filename,
	    UErrorCode *status)
{
  FileStream *in;
  FileStream *rb_out;
  struct SRBItemList *data;
  char *rbname;

  if(FAILURE(*status)) return;

  /* Setup */
  in = rb_out = 0;

  /* Open the input file for reading */
  in = T_FileStream_open(filename, "r");
  if(in == 0) {
    *status = FILE_ACCESS_ERROR;
    setErrorText("File not found");
    return;
  }

  /* Parse the data into an SRBItemList */
  data = parse(in, status);

  /* Determine the target rb filename */
  rbname = make_res_filename(filename, status);
  if(FAILURE(*status)) {
    goto finish;
  }

  /* Open the target file  for writing */
  rb_out = T_FileStream_open(rbname, "wb");
  if(rb_out == 0 || T_FileStream_error(rb_out) != 0) {
    *status = FILE_ACCESS_ERROR;
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

  icu_free(rbname);
}

/* Generate the target .res file name from the input file name */
static char*
make_res_filename(const char *filename,
		 UErrorCode *status)
{
  char *basename;
  char *dirname;
  char *resName;

  if(FAILURE(*status)) return 0;

  /* setup */
  basename = dirname = resName = 0;

  /* determine basename, and compiled file names */
  basename = (char*) icu_malloc(sizeof(char) * (icu_strlen(filename) + 1));
  if(basename == 0) {
    *status = MEMORY_ALLOCATION_ERROR;
    goto finish;
  }
  get_basename(basename, filename);
  
  dirname = (char*) icu_malloc(sizeof(char) * (icu_strlen(filename) + 1));
  if(dirname == 0) {
    *status = MEMORY_ALLOCATION_ERROR;
    goto finish;
  }
  get_dirname(dirname, filename);

  resName = (char*) icu_malloc(sizeof(char) * (icu_strlen(dirname)
					       + icu_strlen(basename) 
					       + icu_strlen(RES_SUFFIX) + 1));
  if(resName == 0) {
    *status = MEMORY_ALLOCATION_ERROR;
    goto finish;
  }
  icu_strcpy(resName, dirname);
  icu_strcat(resName, basename);
  icu_strcat(resName, RES_SUFFIX);

 finish:
  icu_free(basename);
  icu_free(dirname);

  return resName;
}

/* Generate the target .col file name from the input file name */
static char*
make_col_filename(const char *filename,
		 UErrorCode *status)
{
  char *basename;
  char *dirname;
  char *colName;

  if(FAILURE(*status)) return 0;

  /* setup */
  basename = dirname = colName = 0;

  /* determine basename, and compiled file names */
  basename = (char*) icu_malloc(sizeof(char) * (icu_strlen(filename) + 1));
  if(basename == 0) {
    *status = MEMORY_ALLOCATION_ERROR;
    goto finish;
  }
  get_basename(basename, filename);
  
  dirname = (char*) icu_malloc(sizeof(char) * (icu_strlen(filename) + 1));
  if(dirname == 0) {
    *status = MEMORY_ALLOCATION_ERROR;
    goto finish;
  }
  get_dirname(dirname, filename);

  colName = (char*) icu_malloc(sizeof(char) * (icu_strlen(dirname)
					       + icu_strlen(basename) 
					       + icu_strlen(COL_SUFFIX) + 1));
  if(colName == 0) {
    *status = MEMORY_ALLOCATION_ERROR;
    goto finish;
  }
  icu_strcpy(colName, dirname);
  icu_strcat(colName, basename);
  icu_strcat(colName, COL_SUFFIX);

 finish:
  icu_free(basename);
  icu_free(dirname);

  return colName;
}

/* Get the error message for an error code */
static const char* 
errorName(UErrorCode status)
{
  switch(status) {
  case ZERO_ERROR: return "ZERO_ERROR";
  case ILLEGAL_ARGUMENT_ERROR: return "ILLEGAL_ARGUMENT_ERROR";
  case MISSING_RESOURCE_ERROR: return "MISSING_RESOURCE_ERROR";
  case INVALID_FORMAT_ERROR: return "INVALID_FORMAT_ERROR";
  case FILE_ACCESS_ERROR: return "FILE_ACCESS_ERROR";
  case INTERNAL_PROGRAM_ERROR: return "INTERNAL_PROGRAM_ERROR";
  case MESSAGE_PARSE_ERROR: return "MESSAGE_PARSE_ERROR";
  case MEMORY_ALLOCATION_ERROR: return "MEMORY_ALLOCATION_ERROR";
  case INDEX_OUTOFBOUNDS_ERROR: return "INDEX_OUTOFBOUNDS_ERROR";
  case PARSE_ERROR: return "PARSE_ERROR";
  case INVALID_CHAR_FOUND: return "INVALID_CHAR_FOUND";
  case TRUNCATED_CHAR_FOUND: return "TRUNCATED_CHAR_FOUND";
  case ILLEGAL_CHAR_FOUND: return "ILLEGAL_CHAR_FOUND";
  case INVALID_TABLE_FORMAT: return "INVALID_TABLE_FORMAT";
  case INVALID_TABLE_FILE: return "INVALID_TABLE_FILE";
  case BUFFER_OVERFLOW_ERROR: return "BUFFER_OVERFLOW_ERROR";
  case UNSUPPORTED_ERROR: return "UNSUPPORTED_ERROR";
  case USING_FALLBACK_ERROR: return "USING_FALLBACK_ERROR";
  case USING_DEFAULT_ERROR: return "USING_DEFAULT_ERROR";
  default: return "[BOGUS UErrorCode]";
  }
}




