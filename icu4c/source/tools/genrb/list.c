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
* File list.c
*
* Modification History:
*
*   Date        Name        Description
*   06/01/99    stephen     Creation.
*******************************************************************************
*/

#include "list.h"
#include "cmemory.h"
#include "ustring.h"

/* Protos */
static void strlist_grow(struct SList *list, UErrorCode *status);
static void strlist2d_grow(struct SList *list, UErrorCode *status);
static void strlist2d_growRows(struct SList *list, UErrorCode *status);
static void taglist_grow(struct SList *list, UErrorCode *status);

/* String list */

struct SList*
strlist_open(UErrorCode *status)
{
  struct SList *list;
  
  if(FAILURE(*status)) return 0;
  
  list = (struct SList*) icu_malloc(sizeof(struct SList));
  if(list == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return 0;
  }

  list->fType = eStringList;
  
  list->u.fStringList.fData = 0;
  list->u.fStringList.fCount = 0;
  list->u.fStringList.fCapacity = 32;

  strlist_grow(list, status);

  return list;
}

void
strlist_close(struct SList *list,
	      UErrorCode *status)
{
  int32_t i;

  if(FAILURE(*status)) return;

  if(list->fType != eStringList) {
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return;
  }
  
  /* deallocate each string */
  for(i = 0; i < list->u.fStringList.fCount; ++i) {
    icu_free(list->u.fStringList.fData[i]);
  }
  icu_free(list->u.fStringList.fData);
  
  list->fType = eEmpty;
  icu_free(list);
}

void
strlist_add(struct SList *list,
	    const UChar *s,
	    UErrorCode *status)
{
  int32_t index;
  
  if(FAILURE(*status)) return;
  
  if(list->fType != eStringList) {
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return;
  }
  
  index = list->u.fStringList.fCount;
  
  if(list->u.fStringList.fCount == list->u.fStringList.fCapacity) 
    strlist_grow(list, status);

  list->u.fStringList.fData[index] = (UChar*) 
    icu_malloc(sizeof(UChar) * (u_strlen(s) + 1));
  if(list->u.fStringList.fData[index] == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return;
  }

  u_strcpy(list->u.fStringList.fData[index], s);
  ++(list->u.fStringList.fCount);
}

static void 
strlist_grow(struct SList *list,
	     UErrorCode *status)
{
  int32_t i, j;
  int32_t newCapacity;
  UChar **newData;
  
  if(FAILURE(*status)) return;
  
  if(list->fType != eStringList) {
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return;
  }
  
  newCapacity = list->u.fStringList.fCapacity << 1; 
  
  /* allocate space for the array of strings */
  newData = (UChar**) icu_malloc(sizeof(UChar*) * newCapacity);
  if(newData == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return;
  }

  /* allocate and copy each string */
  for(i = 0; i < list->u.fStringList.fCount; ++i) {
    newData[i] = (UChar*) 
      icu_malloc(sizeof(UChar) * (u_strlen(list->u.fStringList.fData[i]) + 1));
    if(newData[i] == 0) {
      *status = U_MEMORY_ALLOCATION_ERROR;
      for(j = 0; j < i; ++j) 
	icu_free(newData[j]);
      icu_free(newData);
      return;
    }
    u_strcpy(newData[i], list->u.fStringList.fData[i]);
  }
  
  icu_free(list->u.fStringList.fData);
  list->u.fStringList.fData = newData;
  list->u.fStringList.fCapacity = newCapacity;
}

/* 2-d String list*/

struct SList*
strlist2d_open(UErrorCode *status)
{
  struct SList *list;
  
  if(FAILURE(*status)) return 0;

  list = (struct SList*) icu_malloc(sizeof(struct SList));
  if(list == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return 0;
  }

  list->fType = eStringList2d;
  
  list->u.fStringList2d.fData = 0;
  list->u.fStringList2d.fCount = 0;
  list->u.fStringList2d.fCapacity = 32;

  list->u.fStringList2d.fRows = 0;
  list->u.fStringList2d.fRowCount = 0;
  list->u.fStringList2d.fRowCapacity = 32;

  strlist2d_grow(list, status);
  strlist2d_growRows(list, status);

  if(SUCCESS(*status)) {
    list->u.fStringList2d.fRows[0] = 0;
    list->u.fStringList2d.fRowCount = 1;
  }

  return list;
}

void 
strlist2d_close(struct SList *list,
		UErrorCode *status)
{
  int32_t i;
  
  if(FAILURE(*status)) return;
  
  if(list->fType != eStringList2d) {
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return;
  }

  /* deallocate each string */
  for(i = 0; i < list->u.fStringList2d.fCount; ++i) {
    icu_free(list->u.fStringList2d.fData[i]);
  }
  icu_free(list->u.fStringList2d.fData);

  icu_free(list->u.fStringList2d.fRows);

  list->fType = eEmpty;
  icu_free(list);
}

void
strlist2d_newRow(struct SList *list,
		 UErrorCode *status)
{
  if(FAILURE(*status)) return;

  if(list->fType != eStringList2d) {
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return;
  }
  
  if(list->u.fStringList2d.fRowCount == list->u.fStringList2d.fRowCapacity)
    strlist2d_growRows(list, status);
  if(FAILURE(*status)) return;
  list->u.fStringList2d.fRows[(list->u.fStringList2d.fRowCount)++] = 
    list->u.fStringList2d.fCount;
}

void strlist2d_add(struct SList *list,
		 const UChar *s,
		 UErrorCode *status)
{
  int32_t index;
  
  if(FAILURE(*status)) return;

  if(list->fType != eStringList2d) {
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return;
  }

  index = list->u.fStringList2d.fCount;
  
  if(list->u.fStringList2d.fCount == list->u.fStringList2d.fCapacity) 
    strlist2d_grow(list, status);

  list->u.fStringList2d.fData[index] = (UChar*) 
    icu_malloc(sizeof(UChar) * (u_strlen(s) + 1));
  if(list->u.fStringList2d.fData[index] == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return;
  }

  u_strcpy(list->u.fStringList2d.fData[index], s);
  ++(list->u.fStringList2d.fCount);
}

static void
strlist2d_grow(struct SList *list,
	       UErrorCode *status)
{
  int32_t i, j;
  int32_t newCapacity;
  UChar **newData;
  
  if(FAILURE(*status)) return;

  if(list->fType != eStringList2d) {
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return;
  }
  
  newCapacity = list->u.fStringList2d.fCapacity << 1; 
  
  /* allocate space for the array of strings */
  newData = (UChar**) icu_malloc(sizeof(UChar*) * newCapacity);
  if(newData == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return;
  }

  /* allocate and copy each string */
  for(i = 0; i < list->u.fStringList2d.fCount; ++i) {
    newData[i] = (UChar*) 
      icu_malloc(sizeof(UChar) * (u_strlen(list->u.fStringList2d.fData[i]) + 1));
    if(newData[i] == 0) {
      *status = U_MEMORY_ALLOCATION_ERROR;
      for(j = 0; j < i; ++j) 
	icu_free(newData[j]);
      icu_free(newData);
      return;
    }
    u_strcpy(newData[i], list->u.fStringList2d.fData[i]);
  }
  
  icu_free(list->u.fStringList2d.fData);
  list->u.fStringList2d.fData = newData;
  list->u.fStringList2d.fCapacity = newCapacity;
}

static void 
strlist2d_growRows(struct SList *list,
		   UErrorCode *status)
{
  int32_t i;
  int32_t newCapacity;
  int32_t *newRows;

  if(FAILURE(*status)) return;

  if(list->fType != eStringList2d) {
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return;
  }

  newCapacity = list->u.fStringList2d.fRowCapacity << 1;
  
  /* allocate space for the array of ints */
  newRows = (int32_t*) icu_malloc(sizeof(int32_t) * newCapacity);
  if(newRows == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
  }
  
  /* copy each int */
  for(i = 0; i < list->u.fStringList2d.fRowCount; ++i) 
    newRows[i] = list->u.fStringList2d.fRows[i];
  
  /* clean up */
  icu_free(list->u.fStringList2d.fRows);
  list->u.fStringList2d.fRows = newRows;
  list->u.fStringList2d.fRowCapacity = newCapacity;
}

/* Tagged list */

struct SList*
taglist_open(UErrorCode *status)
{
  struct SList *list;
  
  if(FAILURE(*status)) return 0;
  
  list = (struct SList*) icu_malloc(sizeof(struct SList));
  if(list == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return 0;
  }

  list->fType = eTaggedList;
  
  list->u.fTaggedList.fData = 0;
  list->u.fTaggedList.fCount = 0;
  list->u.fTaggedList.fCapacity = 32;

  taglist_grow(list, status);
  
  return list;
}

void 
taglist_close(struct SList *list,
	      UErrorCode *status)
{
  if(FAILURE(*status)) return;

  if(list->fType != eTaggedList) {
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return;
  }
  
  icu_free(list->u.fTaggedList.fData);
  
  list->fType = eEmpty;
  icu_free(list);
}


static void 
taglist_grow(struct SList *list,
	     UErrorCode *status)
{
  int32_t i;
  int32_t newCapacity;
  struct SStringPair *newData;
  
  if(FAILURE(*status)) return;
  
  if(list->fType != eTaggedList) {
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return;
  }
  
  newCapacity = list->u.fTaggedList.fCapacity << 1; 
  
  /* allocate space for the array of string pairs */
  newData = (struct SStringPair*) 
    icu_malloc(sizeof(struct SStringPair) * newCapacity);
  if(newData == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return;
  }

  /* copy each string pair */
  for(i = 0; i < list->u.fTaggedList.fCount; ++i) {
    newData[i] = list->u.fTaggedList.fData[i];
  }
  
  icu_free(list->u.fTaggedList.fData);
  list->u.fTaggedList.fData = newData;
  list->u.fTaggedList.fCapacity = newCapacity;
}

void
taglist_add(struct SList *list,
	    const UChar *tag, 
	    const UChar *data,
	    UErrorCode *status)
{
  int32_t index;
  struct SStringPair pair;
  
  if(FAILURE(*status)) return;
  
  if(list->fType != eTaggedList) {
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return;
  }

  pair.fKey = (UChar*) icu_malloc(sizeof(UChar) * (u_strlen(tag) + 1));
  if(pair.fKey == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return;
  }

  pair.fValue = (UChar*) icu_malloc(sizeof(UChar) * (u_strlen(data) + 1));
  if(pair.fValue == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    icu_free(pair.fKey);
    return;
  }

  u_strcpy(pair.fKey, tag);
  u_strcpy(pair.fValue, data);
  
  index = list->u.fTaggedList.fCount;
  
  if(list->u.fTaggedList.fCount == list->u.fTaggedList.fCapacity)
    taglist_grow(list, status);
  
  list->u.fTaggedList.fData[index] = pair;
  ++(list->u.fTaggedList.fCount);
}

const UChar* 
taglist_get(const struct SList *list,
	    const UChar *tag,
	    UErrorCode *status)
{
  int32_t i;

  if(FAILURE(*status)) return 0;
  
  if(list->fType != eTaggedList) {
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return 0;
  }

  for(i = 0; i < list->u.fTaggedList.fCount; ++i) {
    if(u_strcmp(list->u.fTaggedList.fData[i].fKey, tag) == 0)
      return list->u.fTaggedList.fData[i].fValue;
  }

  return 0;
}
