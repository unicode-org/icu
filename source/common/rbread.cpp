/*
**********************************************************************
*   Copyright (C) 1998-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*
* File rbread.cpp
*
* Modification History:
*
*   Date        Name        Description
*   06/11/99    stephen     Creation.
*******************************************************************************
*/

#include <stdio.h>

#include "rbread.h"
#include "cmemory.h"
#include "cstring.h"
#include "filestrm.h"
#include "ustring.h"
#include "rbdata.h"

#include "unistr.h"
#include "rbdata.h"


/* Protos */
static void read_ustring(FileStream *rb, UnicodeString& val, 
			 UErrorCode& status);
static StringList* read_strlist(FileStream *rb, UnicodeString& listname, 
				UErrorCode& status);
static String2dList* read_strlist2d(FileStream *rb, UnicodeString& listname,
				    UErrorCode& status);
static TaggedList* read_taglist(FileStream *rb, UnicodeString& listname,
				UErrorCode& status);
static void U_CALLCONV RBHashtable_valueDeleter(void *value);


/* Read a string from a compiled resource file */
#define BUF_SIZE 128
void
read_ustring(FileStream *rb, 
	     UnicodeString& val,
	     UErrorCode& status)
{
  int32_t len = 0, readLen = 0, remain = 0;
  UChar buf [BUF_SIZE];

  if(U_FAILURE(status)) return;

  /* Read the string's length */
  T_FileStream_read(rb, &len, sizeof(len));

  /* Truncate the output string */
  val.remove();

  remain = len;

  /* Successively read the string's data from the file */
  while(remain != 0) {

    /* Read the next chunk of data */
    readLen = icu_min(BUF_SIZE, remain);
    icu_memset(buf, 0, readLen*sizeof(UChar));
    T_FileStream_read(rb, buf, sizeof(UChar) * readLen);

    /* Append the chunk to the string */
    val.append(buf, readLen);
    
    remain -= readLen;
  }

}

/* Read a string list */
StringList*
read_strlist(FileStream *rb,
	     UnicodeString& listname,
	     UErrorCode& status)
{
  int32_t i, count = 0;
  StringList *retval;

  if(U_FAILURE(status)) return 0;

  /* Setup the string list */
  retval = new StringList();
  if(retval == 0) {
    status = U_MEMORY_ALLOCATION_ERROR;
    return 0;
  }

  /* Read the name of this string list */
  read_ustring(rb, listname, status);
  if(U_FAILURE(status)) {
    delete retval;
    return 0;
  }

  /* Read the number of items in this string list */
  T_FileStream_read(rb, &count, sizeof(count));
  retval->fCount = count;

  /* Allocate space for the array of strings */
  retval->fStrings = new UnicodeString [ retval->fCount ];
  if(retval->fStrings == 0) {
    status = U_MEMORY_ALLOCATION_ERROR;
    delete retval;
    return 0;
  }

  /* Successively read strings in the list */
  for(i = 0; i < count; ++i) {
    read_ustring(rb, retval->fStrings[i], status);

    /* handle error */
    if(U_FAILURE(status)) {
      delete [] retval->fStrings;
      delete retval;
      return 0;
    }
  }
  
  return retval;
}

/* Read a 2-d string list */
String2dList*
read_strlist2d(FileStream *rb,
	       UnicodeString& listname,
	       UErrorCode& status)
{
  int32_t i, j;
  int32_t rows, itemcount;
  String2dList *retval;

  if(U_FAILURE(status)) return 0;
  
  /* Setup the 2-d string list */
  retval = new String2dList();
  if(retval == 0) {
    status = U_MEMORY_ALLOCATION_ERROR;
    return 0;
  }

  /* Read the name of this 2-d string list */
  read_ustring(rb, listname, status);
  if(U_FAILURE(status)) {
    delete retval;
    return 0;
  }

  /* Read the number of rows in this 2-d string list */
  T_FileStream_read(rb, &rows, sizeof(rows));
  retval->fRowCount = rows;

  /* Allocate space for the array of strings */
  retval->fStrings = new UnicodeString* [ retval->fRowCount ];
  if(retval->fStrings == 0) {
    status = U_MEMORY_ALLOCATION_ERROR;
    delete retval;
    return 0;
  }

  /* Read each row */
  for(i = 0; i < rows; ++i) {

    /* Read the number of items in this row */
    T_FileStream_read(rb, &itemcount, sizeof(itemcount));

    /* Hack for now - assume all rows are the same length */
    retval->fColCount = itemcount;

    /* Allocate enough space for each item */
    retval->fStrings[i] = new UnicodeString[itemcount];
    if(retval->fStrings[i] == 0) {
      status = U_MEMORY_ALLOCATION_ERROR;
      /* Complicated cleanup later */
      delete retval;
      return 0;
    }

    /* Read each item */
    for(j = 0; j < itemcount; ++j) {
      read_ustring(rb, retval->fStrings[i][j], status);
      
      /* handle error */
      if(U_FAILURE(status)) {
	/* Complicated cleanup later */
	delete retval;
	return 0;
      }
    }
  }

  return retval;
}

/* Read a tagged list */
TaggedList*
read_taglist(FileStream *rb, 
	     UnicodeString& listname,
	     UErrorCode& status)
{
  TaggedList *retval;
  int32_t i, count = 0;
  UnicodeString tag, value;

  if(U_FAILURE(status)) return 0;

  /* Setup the tagged list */
  retval = new TaggedList();
  if(retval == 0) {
    status = U_MEMORY_ALLOCATION_ERROR;
    return 0;
  }

  /* Read the name of this tagged list */
  read_ustring(rb, listname, status);
  if(U_FAILURE(status)) {
    delete retval;
    return 0;
  }

  /* Read the number of items in this tagged list */
  T_FileStream_read(rb, &count, sizeof(count));

  /* Successively read strings in the list */
  for(i = 0; i < count; ++i) {
    read_ustring(rb, tag, status);
    read_ustring(rb, value, status);
    
    /* handle error */
    if(U_FAILURE(status)) {
      delete retval;
      return 0;
    }

    /* put the tag/value in the list */
    retval->put(tag, value);
  }

  return retval;
}

/* Parse a compiled rb file */
UHashtable*
rb_parse(FileStream *f, 
	 UnicodeString& localename,
	 UErrorCode& status)
{
  UHashtable *retval;
  int32_t bom;
  int32_t itemtype;
  UnicodeString listname;
  StringList *strlist;
  String2dList *strlist2d;
  TaggedList *taglist;

  if(U_FAILURE(status)) return 0;

  /* Open the hashtable for saving data */
  retval = uhash_open((UHashFunction)uhash_hashUString, &status);
  if(retval == 0 || U_FAILURE(status)) {
    status = U_MEMORY_ALLOCATION_ERROR;
    return 0;
  }
  uhash_setValueDeleter(retval, RBHashtable_valueDeleter);

  /* Read the byte order mark from the file */
  T_FileStream_read(f, &bom, sizeof(bom));
  
  /* Verify the byte ordering matches */
  if(bom != sBOM) {
    uhash_close(retval);
    status = U_INVALID_FORMAT_ERROR;
    return 0;
  }

  /* Read the locale name from the file */
  read_ustring(f, localename, status);
  if(U_FAILURE(status)) {
    uhash_close(retval);
    return 0;
  }

  /* Successively read each list item */
  for(;;) {

    /* Read the next item type */
    T_FileStream_read(f, &itemtype, sizeof(itemtype));

    /* If we're at EOF, break */
    if(itemtype == sEOF) {
      break;
    }
   
    /* Parse each item and add it to the hashtable */
    switch(itemtype) {
    case sSTRINGLIST:
      strlist = read_strlist(f, listname, status);
      if (listname == "%%ALIAS") {
          localename = strlist->fStrings[0];
          uhash_close(retval);
          return 0;
      }
      uhash_putKey(retval, listname.hashCode() & 0x7FFFFFFF, 
		   strlist, &status);
      if(U_FAILURE(status)) {
	uhash_close(retval);
	return 0;
      }
      break;

    case sSTRINGLIST2D:
      strlist2d = read_strlist2d(f, listname, status);
      uhash_putKey(retval, listname.hashCode() & 0x7FFFFFFF, 
		   strlist2d, &status);
      if(U_FAILURE(status)) {
	uhash_close(retval);
	return 0;
      }
      break;

    case sTAGGEDLIST:
      taglist = read_taglist(f, listname, status);
      uhash_putKey(retval, listname.hashCode() & 0x7FFFFFFF, 
		   taglist, &status);
      if(U_FAILURE(status)) {
	uhash_close(retval);
	return 0;
      }
      break;
    }
  }

  /* Check if any errors occurred during reading */
  if(T_FileStream_error(f) != 0) {
    status = U_FILE_ACCESS_ERROR;
    delete retval;
    return 0;
  }

  return retval;
}

void 
RBHashtable_valueDeleter(void *value)
{
  delete (ResourceBundleData*)value;
}
