/*
*******************************************************************************
*
*   Copyright (C) 1998-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File write.c
*
* Modification History:
*
*   Date        Name        Description
*   06/01/99    stephen     Creation.
*******************************************************************************
*/

#include "parse.h"
#include "cmemory.h"
#include "cstring.h"
#include "filestrm.h"
#include "ustring.h"
#include "uprint.h"

/* Protos */
static UChar* read_ustring(FileStream *rb, UErrorCode *status);
static void read_strlist(FileStream *rb, UErrorCode *status);
static void read_strlist2d(FileStream *rb, UErrorCode *status);
static void read_taglist(FileStream *rb, UErrorCode *status);

/* Special values */
static const int32_t sBOM = 0x021C;

#define STRINGLIST 1
#define STRINGLIST2D 2
#define TAGGEDLIST 3

static const int32_t sSTRINGLIST = 1;
static const int32_t sSTRINGLIST2D = 2;
static const int32_t sTAGGEDLIST = 3;

static const int32_t sEOF = -1;


/* Read a UChar array */
static UChar*
read_ustring(FileStream *rb, 
	     UErrorCode *status)
{
  int32_t len;
  UChar *s;

  if(U_FAILURE(*status)) return 0;

  /* Read the string's length */
  T_FileStream_read(rb, &len, sizeof(len));

  /* Allocate space for the string */
  s = (UChar*) icu_malloc(sizeof(UChar) * (len + 1));
  if(s == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return 0;
  }

  /*  Read the string's data */
  T_FileStream_read(rb, s, sizeof(UChar) * len);

  /* Add the null terminator */
  s[len] = 0x0000;

  return s;
}

/* Read a string list */
static void
read_strlist(FileStream *rb,
	     UErrorCode *status)
{
  int32_t i, count;
  UChar *name;
  UChar *s;

  if(U_FAILURE(*status)) return;

  /* Read the name of this string list */
  name = read_ustring(rb, status);

  /* Read the number of items in this string list */
  T_FileStream_read(rb, &count, sizeof(count));

  printf("  ");
  uprint(name, stdout, status);
  fputc('\n', stdout);

  /* Successively read strings in the list */
  for(i = 0; i < count; ++i) {
    s = read_ustring(rb, status);
    
    /* handle error */
    if(U_FAILURE(*status) || s == 0) {
      goto finish;
    }

    printf("    ");
    uprint(s, stdout, status);
    fputc('\n', stdout);
    icu_free(s);
  }

 finish:

  /* clean up */
  icu_free(name);
}

/* Read a 2-d string list */
static void 
read_strlist2d(FileStream *rb,
	       UErrorCode *status)
{
  int32_t i, j;
  int32_t rows, itemcount;
  UChar *name;
  UChar *s;

  if(U_FAILURE(*status)) return;

  /* Read the name of this 2-d string list */
  name = read_ustring(rb, status);

  /* Read the number of rows in this 2-d string list */
  T_FileStream_read(rb, &rows, sizeof(rows));

  printf("  ");
  uprint(name, stdout, status);
  fputc('\n', stdout);

  /* Read each row */
  for(i = 0; i < rows; ++i) {

    /* Read the number of items in this row */
    T_FileStream_read(rb, &itemcount, sizeof(itemcount));

    printf("    ");

    /* Read each item */
    for(j = 0; j < itemcount; ++j) {
      s = read_ustring(rb, status);
      
      /* handle error */
      if(U_FAILURE(*status) || s == 0) {
	goto finish;
      }
      
      uprint(s, stdout, status);
      if(j != itemcount - 1)
	fputc(',', stdout);

      icu_free(s);
    }

    puts("");
  }
  
 finish:

  /* clean up */
  icu_free(name);
}

/* Read a tagged list */
static void 
read_taglist(FileStream *rb, 
	     UErrorCode *status)
{
  int32_t i, count;
  UChar *name;
  UChar *tag, *value;

  if(U_FAILURE(*status)) return;

  /* Read the name of this tagged list */
  name = read_ustring(rb, status);

  /* Read the number of items in this tagged list */
  T_FileStream_read(rb, &count, sizeof(count));

  printf("  ");
  uprint(name, stdout, status);
  fputc('\n', stdout);

  /* Successively read strings in the list */
  for(i = 0; i < count; ++i) {
    tag = read_ustring(rb, status);
    value = read_ustring(rb, status);
    
    /* handle error */
    if(U_FAILURE(*status) || tag == 0 || value == 0) {
      goto finish;
    }

    printf("    ");
    uprint(tag, stdout, status);
    printf(": ");
    uprint(value, stdout, status);
    fputc('\n', stdout);
    
    icu_free(tag);
    icu_free(value);
  }

 finish:

  /* clean up */
  icu_free(name);
}

/* Parse a compiled rb file */
void
parse(FileStream *f, 
      UErrorCode *status)
{
  int32_t bom;
  int32_t itemtype;
  UChar *localename;

  if(U_FAILURE(*status)) return;

  /* Read the byte order mark from the file */
  T_FileStream_read(f, &bom, sizeof(bom));
  
  /* Verify the byte ordering matches */
  if(bom != sBOM) {
    *status = U_INVALID_FORMAT_ERROR;
    goto finish;
  }

  /* Read the locale name from the file */
  localename = read_ustring(f, status);

  uprint(localename, stdout, status);
  fputc('\n', stdout);

  if(U_FAILURE(*status)) {
    goto finish;
  }

  /* Successively read each list item */
  for(;;) {

    /* Read the next item type */
    T_FileStream_read(f, &itemtype, sizeof(itemtype));

    /* If we're at EOF, break */
    if(itemtype == sEOF) {
      goto finish;
    }
   
    /* Parse each item */
    switch(itemtype) {
    case STRINGLIST:
      read_strlist(f, status);
      if(U_FAILURE(*status)) {
	goto finish;
      }
      break;

    case STRINGLIST2D:
      read_strlist2d(f, status);
      if(U_FAILURE(*status)) {
	goto finish;
      }
      break;

    case TAGGEDLIST:
      read_taglist(f, status);
      if(U_FAILURE(*status)) {
	goto finish;
      }
      break;
    }
  }

  /* Check if any errors occurred during reading */
  if(T_FileStream_error(f) != 0) {
    *status = U_FILE_ACCESS_ERROR;
  }

 finish:
  
  /* clean up */
  icu_free(localename);
}
