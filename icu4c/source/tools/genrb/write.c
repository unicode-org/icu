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
* File write.c
*
* Modification History:
*
*   Date        Name        Description
*   06/01/99    stephen     Creation.
*******************************************************************************
*/

#include <stdio.h>
#include "write.h"
#include "cmemory.h"
#include "cstring.h"
#include "filestrm.h"
#include "ustring.h"
#include "error.h"
#include "list.h"


/* Protos */
static void write_ustring(FileStream *rb, const UChar *data);
static void write_strlist(FileStream *rb, const UChar *name, 
			  const struct SStringList *list);
static void write_strlist2d(FileStream *rb, const UChar *name,
			    const struct SStringList2d *list);
static void write_taglist(FileStream *rb, const UChar *name,
			  const struct STaggedList *list);

/* Special values */
static const int32_t sBOM = 0x021C;

static const int32_t sEOF = -1;

static const int32_t sSTRINGLIST = 1;
static const int32_t sSTRINGLIST2D = 2;
static const int32_t sTAGGEDLIST = 3;

static const UChar gCollationElementsTag [] = {
  /* "CollationElements" */
  0x0043, 0x006f, 0x006c, 0x006c, 0x0061, 0x0074, 0x0069, 0x006f, 0x006e,
  0x0045, 0x006c, 0x0065, 0x006d, 0x0065, 0x006e, 0x0074, 0x0073, 0x0000
};

/* Write a null-terminated UChar array */
static void
write_ustring(FileStream *rb, 
	      const UChar *data)
{
  int32_t len;

  len = u_strlen(data);

  /* Write the string's length */
  T_FileStream_write(rb, &len, sizeof(len));

  /* Write the string's data */
  T_FileStream_write(rb, data, sizeof(UChar) * len);
}

/* Write a string list */
static void
write_strlist(FileStream *rb, 
	      const UChar *name,
	      const struct SStringList *list)
{
  int32_t i;

  /* Write out the value indicating this is a string list */
  T_FileStream_write(rb, &sSTRINGLIST, sizeof(sSTRINGLIST));

  /* Write the name of this string list */
  write_ustring(rb, name);

  /* Write the item count */
  T_FileStream_write(rb, &list->fCount, sizeof(list->fCount));

  /* Write each string in the list */
  for(i = 0; i < list->fCount; ++i) {
    write_ustring(rb, list->fData[i]);
  }
}

/* Write a 2-d string list */
static void 
write_strlist2d(FileStream *rb,
		const UChar *name,
		const struct SStringList2d *list)
{
  int32_t i, j;
  int32_t itemcount;

  /* Write out the value indicating this is a 2-d string list */
  T_FileStream_write(rb, &sSTRINGLIST2D, sizeof(sSTRINGLIST2D));

  /* Write the name of this 2-d string list */
  write_ustring(rb, name);

  /* Write the row count */
  T_FileStream_write(rb, &list->fRowCount, sizeof(list->fRowCount));

  /* Write out each row */
  for(i = 0; i < list->fRowCount; ++i) {
    itemcount = (i == list->fRowCount - 1 ? list->fCount: list->fRows[i+1])
      - list->fRows[i];

    /* Write out the count of strings in this row */
    T_FileStream_write(rb, &itemcount, sizeof(itemcount));

    /* Write out each string in the row */
    for(j = 0; j < itemcount; ++j) {
      write_ustring(rb, list->fData[list->fRows[i] + j]);
    }
  }
}

/* Write a tagged list */
static void 
write_taglist(FileStream *rb, 
	      const UChar *name,
	      const struct STaggedList *list)
{
  int32_t i;

  /* Write out the value indicating this is a tagged list */
  T_FileStream_write(rb, &sTAGGEDLIST, sizeof(sTAGGEDLIST));

  /* Write the name of this tagged list */
  write_ustring(rb, name);

  /* Write the item count */
  T_FileStream_write(rb, &list->fCount, sizeof(list->fCount));

  /* Write out each key/value pair */
  for(i = 0; i < list->fCount; ++i) {
    printf("     ");
    write_ustring(rb, list->fData[i].fKey);
    write_ustring(rb, list->fData[i].fValue);
  }
}

/* Write a parsed SRBItemList to a file */
void
rb_write(FileStream *f, 
	 struct SRBItemList *data, 
	 UErrorCode *status)
{
  int32_t i;
  struct SRBItem *item;

  if(FAILURE(*status)) return;

  /* Write the byte order mark to the file */
  T_FileStream_write(f, &sBOM, sizeof(sBOM));

  /* Write the locale name to the file */
  write_ustring(f, data->fLocale);

  /* Successively write each list item */
  for(i = 0; i < data->fCount; ++i) {

    item = data->fData[i];

    switch(item->fData->fType) {
    case eStringList:
      /*if(u_strcmp(item->fTag, gCollationElementsTag) == 0)
	puts("got CollationElements");*/
      write_strlist(f, item->fTag, &item->fData->u.fStringList);
      break;

    case eStringList2d:
      write_strlist2d(f, item->fTag, &item->fData->u.fStringList2d);
      break;

    case eTaggedList:
      write_taglist(f, item->fTag, &item->fData->u.fTaggedList);
      break;

    case eEmpty:
      *status = U_INTERNAL_PROGRAM_ERROR;
      setErrorText("Unexpected empty item found");
      goto finish;
      /*break;*/
    }
  }

  /* Indicate the end of the data */
  T_FileStream_write(f, &sEOF, sizeof(sEOF));

  /* Check if any errors occurred during writing */
  if(T_FileStream_error(f) != 0) {
    *status = U_FILE_ACCESS_ERROR;
  }

 finish:
  ;
  
  /* clean up */
}
