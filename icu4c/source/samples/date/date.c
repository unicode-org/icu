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
* File date.c
*
* Modification History:
*
*   Date        Name        Description
*   06/11/99    stephen     Creation.
*   06/16/99    stephen     Modified to use uprint.
*******************************************************************************
*/

#include <stdio.h>
#include <string.h>

#include "utypes.h"
#include "ustring.h"

#include "ucnv.h"
#include "udat.h"
#include "ucal.h"

#include "uprint.h"

/* Protos */
static void usage();
static void version();
static void date(const UChar *tz, UDateFormatStyle style, UErrorCode *status);
int main(int argc, char **argv);


/* The version of date */
#define DATE_VERSION "1.0"

/* "GMT" */
static const UChar GMT_ID [] = { 0x0047, 0x004d, 0x0054, 0x0000 };


int
main(int argc,
     char **argv)
{
  int printUsage = 0;
  int printVersion = 0;
  int optind = 1;
  char *arg;
  const UChar *tz = 0;
  UDateFormatStyle style = UDAT_DEFAULT;
  UErrorCode status = ZERO_ERROR;


  /* parse the options */
  for(optind = 1; optind < argc; ++optind) {
    arg = argv[optind];
    
    /* version info */
    if(strcmp(arg, "-v") == 0 || strcmp(arg, "--version") == 0) {
      printVersion = 1;
    }
    /* usage info */
    else if(strcmp(arg, "-h") == 0 || strcmp(arg, "--help") == 0) {
      printUsage = 1;
    }
    /* display date in gmt */
    else if(strcmp(arg, "-u") == 0 || strcmp(arg, "--gmt") == 0) {
      tz = GMT_ID;
    }
    /* display date in gmt */
    else if(strcmp(arg, "-f") == 0 || strcmp(arg, "--full") == 0) {
      style = UDAT_FULL;
    }
    /* display date in long format */
    else if(strcmp(arg, "-l") == 0 || strcmp(arg, "--long") == 0) {
      style = UDAT_LONG;
    }
    /* display date in medium format */
    else if(strcmp(arg, "-m") == 0 || strcmp(arg, "--medium") == 0) {
      style = UDAT_MEDIUM;
    }
    /* display date in short format */
    else if(strcmp(arg, "-s") == 0 || strcmp(arg, "--short") == 0) {
      style = UDAT_SHORT;
    }
    /* POSIX.1 says all arguments after -- are not options */
    else if(strcmp(arg, "--") == 0) {
      /* skip the -- */
      ++optind;
      break;
    }
    /* unrecognized option */
    else if(strncmp(arg, "-", strlen("-")) == 0) {
      printf("date: invalid option -- %s\n", arg+1);
      printUsage = 1;
    }
    /* done with options, display date */
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

  /* print the date */
  date(tz, style, &status);

  return (FAILURE(status) ? 1 : 0);
}

/* Usage information */
static void
usage()
{  
  puts("Usage: date [OPTIONS]");
  puts("Options:");
  puts("  -h, --help        Print this message and exit.");
  puts("  -v, --version     Print the version number of date and exit.");
  puts("  -u, --gmt         Display the date in Greenwich Mean Time.");
  puts("  -f, --full        Use full display format.");
  puts("  -l, --long        Use long display format.");
  puts("  -m, --medium      Use medium display format.");
  puts("  -s, --short       Use short display format.");
}

/* Version information */
static void
version()
{
  printf("date version %s (ICU version %s), by Stephen F. Booth.\n", 
	 DATE_VERSION, ICU_VERSION);
  puts("(C) Copyright International Business Machines Corporation, 1998, 1999");
  puts("Licensed Material - Program-Property of IBM - All Rights Reserved.");
  puts("US Government Users Restricted Rights - Use, duplication, or disclosure");
  puts("restricted by GSA ADP Schedule Contract with IBM Corp.");
}

/* Format the date */
static void
date(const UChar *tz,
     UDateFormatStyle style,
     UErrorCode *status)
{
  UChar *s = 0;
  int32_t len = 0;
  UDateFormat *fmt;

  fmt = udat_open(style, style, 0, tz, -1, status);
  len = udat_format(fmt, ucal_getNow(), 0, len, 0, status);
  if(*status == BUFFER_OVERFLOW_ERROR) {
    *status = ZERO_ERROR;
    s = (UChar*) malloc(sizeof(UChar) * (len+1));
    if(s == 0) goto finish;
    udat_format(fmt, ucal_getNow(), s, len + 1, 0, status);
    if(FAILURE(*status)) goto finish;
  }

  /* print the date string */
  uprint(s, stdout, status);

  /* print a trailing newline */
  printf("\n");

 finish:
  udat_close(fmt);
  free(s);
}
