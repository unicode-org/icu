/**************************************************************************
*
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
***************************************************************************
*   file name:  pkgdata.c
*   encoding:   ANSI X3.4 (1968)
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000may16
*   created by: Steven \u24C7 Loomis
*
*  common types for pkgdata
*/

#ifndef _PKGTYPES
#define _PKGTYPES

/* headers */
#include "unicode/utypes.h"
#include "filestrm.h"

/* linked list */
struct _CharList;

typedef struct _CharList
{
  const char       *str;
  struct _CharList *next;
} CharList;


/* 
 * write CharList 'l' into stream 's' using deliminter 'delim' (delim can be NULL)
 */
const char *pkg_writeCharList(FileStream *s, CharList *l, const char *delim);

/*
 * Same, but use line breaks
 */
const char *pkg_writeCharListWrap(FileStream *s, CharList *l, const char *delim, const char *brk);

/*
 * Count items . 0 if null
 */
uint32_t pkg_countCharList(CharList *l);

/* 
 * Prepend string to CharList. Str is adopted!
 */
CharList *pkg_prependToList(CharList *l, const char *str);

/* 
 * append string to CharList. *end or even end can be null if you don't 
 * know it.[slow]
 * Str is adopted!
 */
CharList *pkg_appendToList(CharList *l, CharList** end, const char *str);

/*
 * does list contain string?  Returns: t/f
 */
bool_t  pkg_listContains(CharList *l, const char *str);

/*
 * Delete list 
 */
void pkg_deleteList(CharList *l);




/*
 * Mode package function
 */
struct UPKGOptions_;
typedef   void (UPKGMODE)(struct UPKGOptions_ *, FileStream *s, UErrorCode *status);

/* 
 * Options to be passed throughout the program
 */

typedef struct UPKGOptions_
{
  CharList   *fileListFiles; /* list of files containing files for inclusion in the package */
  CharList   *filePaths;     /* All the files, with long paths */
  CharList   *files;         /* All the files */
  CharList   *outFiles;      /* output files [full paths] */

  const char *shortName;   /* name of what we're building */
  const char *targetDir;
  const char *tmpDir;
  const char *srcDir;
  const char *options;     /* Options arg */
  const char *mode;        /* Mode of building */
  const char *comment;     /* comment string */
  const char *makeFile;    /* Makefile path */
  const char *install;     /* Where to install to (NULL = don't install) */
  bool_t     rebuild;
  bool_t     clean;
  bool_t     nooutput;
  bool_t     verbose;
  bool_t     hadStdin;     /* Stdin was a dependency - don't make anything depend on the file list coming in. */

  UPKGMODE  *fcn;          /* Handler function */
} UPKGOptions;


/* set up common defines for library naming */

#ifdef WIN32
# ifndef UDATA_SO_SUFFIX
#  define UDATA_SO_SUFFIX ".DLL"
# endif
# define LIB_PREFIX ""
# define OBJ_SUFFIX ".obj"

#else  /* POSIX? */
# define LIB_PREFIX "lib"
# define OBJ_SUFFIX ".o"
#endif 


/* defines for common file names */
#define UDATA_CMN_PREFIX ""
#define UDATA_CMN_SUFFIX ".dat"
#define UDATA_CMN_INTERMEDIATE_SUFFIX "_dat"


#endif
