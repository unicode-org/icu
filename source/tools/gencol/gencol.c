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
* File gencol.c
*
* Modification History:
*
*   Date        Name        Description
*   06/09/99    stephen	    Creation.
*******************************************************************************
*/

#include <stdio.h>

#include "utypes.h"
#include "cmemory.h"
#include "cstring.h"

#include "uloc.h"
#include "ucol.h"


/* Protos */
static void usage();
static void version();
static const char* errorName(UErrorCode status);
int main(int argc, char **argv);


/* The version of gencol */
#define GENCOL_VERSION "1.0b"


int
main(int argc,
     char **argv)
{
  int printUsage = 0;
  int printVersion = 0;
  int optind;
  int i;
  char *arg;
  const char *loc;
  UCollator *coll;
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
      printf("gencol: invalid option -- %s\n", arg+1);
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

  /* generate the binary collation files */
  for(i = 0; i < uloc_countAvailable(); ++i) {
    status = ZERO_ERROR;
    loc = uloc_getAvailable(i);
    printf("gencol: Creating collation data for locale \"%s\"\n", loc);
    coll = ucol_open(loc, &status);
    if(FAILURE(status)) {
      printf("gencol: %s for locale \"%s\"", errorName(status), loc);
    }
    else {
      ucol_close(coll);
    }
  }
  
  return 0;
}

/* Usage information */
static void
usage()
{  
  puts("Usage: gencol [OPTIONS] [FILES]");
  puts("Options:");
  puts("  -h, --help        Print this message and exit.");
  puts("  -v, --version     Print the version number of gencol and exit.");
}

/* Version information */
static void
version()
{
  printf("gencol version %s (ICU version %s), by Stephen F. Booth.\n", 
	 GENCOL_VERSION, ICU_VERSION);
  puts("(C) Copyright International Business Machines Corporation, 1998, 1999");
  puts("Licensed Material - Program-Property of IBM - All Rights Reserved.");
  puts("US Government Users Restricted Rights - Use, duplication, or disclosure");
  puts("restricted by GSA ADP Schedule Contract with IBM Corp.");
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




