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
#include "util.h"
#include "reslist.h"

U_CDECL_END
#include "toolutil.h"
#include "uoptions.h"

#include "unicode/ucol.h"
#include "unicode/uloc.h"

/* Protos */
static void usage(void);
static void version(void);
static void processFile(const char *filename, const char* cp, const char *inputDir, const char *outputDir, UErrorCode *status);
static char* make_res_filename(const char *filename, const char *outputDir, UErrorCode *status);
static char* make_col_filename(const char *filename, const char *outputDir, UErrorCode *status);
static void make_col(const char *filename, UErrorCode *status);
int main(int argc, char **argv);

/* File suffixes */
#define RES_SUFFIX ".res"
#define COL_SUFFIX ".col"

/* The version of genrb */
#define GENRB_VERSION "2.0"


static enum {
    HELP1,
    HELP2,
    VERBOSE,
    VERSION,
    SOURCEDIR,
    DESTDIR,
    ENCODING
};

static UOption options[]={
    UOPTION_HELP_H,
    UOPTION_HELP_QUESTION_MARK,
    UOPTION_VERBOSE,
    UOPTION_VERSION,
    UOPTION_SOURCEDIR,
    UOPTION_DESTDIR,
    UOPTION_ENCODING
};



int
main(int argc,
     char **argv)
{
  int printUsage = 0;
  int printVersion = 0;
  int useConversionLibrary = 0;
  int optind = 1;
  int i;
  UErrorCode status;
  const char *arg = NULL;
  const char *outputDir = NULL; /* NULL = no output directory, use current */
  const char *inputDir = NULL;
  const char *encoding = ""; 
  bool_t verbose;

  argc = u_parseArgs(argc, argv, sizeof(options)/sizeof(options[0]), options);

  /* error handling, printing usage message */
  if(argc<0) {
      fprintf(stderr, "error in command line argument \"%s\"\n", argv[-argc]);
  } else if(argc<2) {
      argc=-1;
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
      "\t\t-s or --sourcedir    source directory for files followed by path, defaults to %s\n",
      argv[0], u_getDataDirectory(), u_getDataDirectory());
      return argc<0 ? U_ILLEGAL_ARGUMENT_ERROR : U_ZERO_ERROR;
  }

  if(options[VERSION].doesOccur) {
      fprintf(stderr, 
      "%s version %s (ICU version %s).\n"
      "%s\n", 
      argv[0], GENRB_VERSION, U_ICU_VERSION, U_COPYRIGHT_STRING);
      return U_ZERO_ERROR;
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

  /* generate the binary files */
  for(i = 1; i < argc; ++i) {
    status = U_ZERO_ERROR;
    arg = getLongPathname(argv[i]);
/*
    if(outputDir == NULL) {
        char *pathSepPosition = NULL;
        pathSepPosition = uprv_strrchr(arg, U_FILE_SEP_CHAR);
        if(pathSepPosition != NULL) {
            int32_t pathLen = pathSepPosition-arg+1;
            outputDir = (char *) uprv_malloc(sizeof(char)*(pathLen+1));
            uprv_strncpy(outputDir, arg, pathLen);
            outputDir[pathLen] = '\0';
        }
    }
*/
    printf("genrb: processing file \"%s\"\n", arg);
    processFile(arg, encoding, inputDir, outputDir, &status);
    /*make_col(arg, &status);*/
    if(U_FAILURE(status)) {
        printf("genrb: %s processing file \"%s\"\n", u_errorName(status), arg);
        if(getErrorText() != 0)
	    printf("       (%s)\n", getErrorText());
    }
  }

  return status;
}

/* Process a file */
static void
processFile(const char *filename, const char *cp, const char *inputDir, const char *outputDir, UErrorCode *status)
{
  FileStream *in;
  struct SRBRoot *data;
  char *rbname;

  if(U_FAILURE(*status)) return;

  /* Setup */
  in = 0;

  /* Open the input file for reading */
  if(inputDir == NULL) {
      in = T_FileStream_open(filename, "r");
  } else {
      char *openFileName = NULL;
      int32_t dirlen = uprv_strlen(inputDir);
      int32_t filelen = uprv_strlen(filename);
      if(inputDir[dirlen-1] != U_FILE_SEP_CHAR) {
          openFileName = (char *) uprv_malloc(dirlen+filelen+2);
          uprv_strcpy(openFileName, inputDir);
          openFileName[dirlen] = U_FILE_SEP_CHAR;
          openFileName[dirlen+1] = '\0';
          uprv_strcat(openFileName, filename);
      } else {
          openFileName = (char *) uprv_malloc(dirlen+filelen+1);
          uprv_strcpy(openFileName, inputDir);
          uprv_strcat(openFileName, filename);
      }
      in = T_FileStream_open(openFileName, "r");
      uprv_free(openFileName);
  }

  if(in == 0) {
    *status = U_FILE_ACCESS_ERROR;
    setErrorText("File not found");
    return;
  }

  /* Parse the data into an SRBRoot */
  data = parse(in, cp, status);

  /* Determine the target rb filename */
  rbname = make_res_filename(filename, outputDir,  status);
  if(U_FAILURE(*status)) {
    goto finish;
  }

  /* Open the target file  for writing */
  /* Write the data to the file */
  /*rb_write(rb_out, data, status);*/
  bundle_write(data, outputDir, status);
  /*bundle_write(data, outputDir, rbname, status);*/
  bundle_close(data, status);


 finish:

  /* Clean up */
  T_FileStream_close(in);

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
      /*uprv_strcat(resName, RES_SUFFIX);*/
  }
  else
  {
      int32_t dirlen = uprv_strlen(outputDir);
      int32_t dirnamelen = uprv_strlen(dirname);
      int32_t basenamelen = uprv_strlen(basename);
      resName = (char*) uprv_malloc(sizeof(char) * (dirlen + basenamelen + 2));
      /*resName = (char*) uprv_malloc(sizeof(char) * (dirnamelen + basenamelen + 1));*/
      if(resName == 0) {
          *status = U_MEMORY_ALLOCATION_ERROR;
          goto finish;
      }

      uprv_strcpy(resName, outputDir);

      if(outputDir[dirlen] != U_FILE_SEP_CHAR) {
          resName[dirlen] = U_FILE_SEP_CHAR;
          resName[dirlen+1] = '\0';
      } 
      /*uprv_strcat(resName, dirname);*/
      /*uprv_strcpy(resName, dirname);*/
      uprv_strcat(resName, basename);    
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

/*
 * Local Variables:
 * indent-tabs-mode: nil
 * End:
 */
