/*
*******************************************************************************
*
*   Copyright (C) 1998-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
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
static void usage(void);
static void version(void);
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
  coll = ucol_open("default", &status);
  ucol_close(coll);
  for(i = 0; i < uloc_countAvailable(); ++i) {
    status = U_ZERO_ERROR;
    loc = uloc_getAvailable(i);
    printf("gencol: Creating collation data for locale \"%s\"\n", loc);
    coll = ucol_open(loc, &status);
    if(U_FAILURE(status)) {
      printf("gencol: %s for locale \"%s\"", u_errorName(status), loc);
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
  printf("gencol version %s (ICU version %s).\n", 
	 GENCOL_VERSION, ICU_VERSION);
  puts(U_COPYRIGHT_STRING);
}
