/*
*******************************************************************************
*
*   Copyright (C) 1998-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
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
#include "cstring.h"
#include "unicode/ustring.h"

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
  
  if(U_FAILURE(*status)) return 0;
  
  list = (struct SList*) uprv_malloc(sizeof(struct SList));
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

  if(U_FAILURE(*status)) return;

  if(list->fType != eStringList) {
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return;
  }
  
  /* deallocate each string */
  for(i = 0; i < list->u.fStringList.fCount; ++i) {
    uprv_free(list->u.fStringList.fData[i]);
  }
  uprv_free(list->u.fStringList.fData);
  
  list->fType = eEmpty;
  uprv_free(list);
}

void
strlist_add(struct SList *list,
	    const UChar *s,
	    UErrorCode *status)
{
  int32_t index;
  
  if(U_FAILURE(*status)) return;
  
  if(list->fType != eStringList) {
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return;
  }
  
  index = list->u.fStringList.fCount;
  
  if(list->u.fStringList.fCount == list->u.fStringList.fCapacity) 
    strlist_grow(list, status);

  list->u.fStringList.fData[index] = (UChar*) 
    uprv_malloc(sizeof(UChar) * (u_strlen(s) + 1));
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
  
  if(U_FAILURE(*status)) return;
  
  if(list->fType != eStringList) {
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return;
  }
  
  newCapacity = list->u.fStringList.fCapacity << 1; 
  
  /* allocate space for the array of strings */
  newData = (UChar**) uprv_malloc(sizeof(UChar*) * newCapacity);
  if(newData == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return;
  }

  /* allocate and copy each string */
  for(i = 0; i < list->u.fStringList.fCount; ++i) {
    newData[i] = (UChar*) 
      uprv_malloc(sizeof(UChar) * (u_strlen(list->u.fStringList.fData[i]) + 1));
    if(newData[i] == 0) {
      *status = U_MEMORY_ALLOCATION_ERROR;
      for(j = 0; j < i; ++j) 
	uprv_free(newData[j]);
      uprv_free(newData);
      return;
    }
    u_strcpy(newData[i], list->u.fStringList.fData[i]);
  }
  
  uprv_free(list->u.fStringList.fData);
  list->u.fStringList.fData = newData;
  list->u.fStringList.fCapacity = newCapacity;
}

/* 2-d String list*/

struct SList*
strlist2d_open(UErrorCode *status)
{
  struct SList *list;
  
  if(U_FAILURE(*status)) return 0;

  list = (struct SList*) uprv_malloc(sizeof(struct SList));
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

  if(U_SUCCESS(*status)) {
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
  
  if(U_FAILURE(*status)) return;
  
  if(list->fType != eStringList2d) {
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return;
  }

  /* deallocate each string */
  for(i = 0; i < list->u.fStringList2d.fCount; ++i) {
    uprv_free(list->u.fStringList2d.fData[i]);
  }
  uprv_free(list->u.fStringList2d.fData);

  uprv_free(list->u.fStringList2d.fRows);

  list->fType = eEmpty;
  uprv_free(list);
}

void
strlist2d_newRow(struct SList *list,
		 UErrorCode *status)
{
  if(U_FAILURE(*status)) return;

  if(list->fType != eStringList2d) {
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return;
  }
  
  if(list->u.fStringList2d.fRowCount == list->u.fStringList2d.fRowCapacity)
    strlist2d_growRows(list, status);
  if(U_FAILURE(*status)) return;
  list->u.fStringList2d.fRows[(list->u.fStringList2d.fRowCount)++] = 
    list->u.fStringList2d.fCount;
}

void strlist2d_add(struct SList *list,
		 const UChar *s,
		 UErrorCode *status)
{
  int32_t index;
  
  if(U_FAILURE(*status)) return;

  if(list->fType != eStringList2d) {
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return;
  }

  index = list->u.fStringList2d.fCount;
  
  if(list->u.fStringList2d.fCount == list->u.fStringList2d.fCapacity) 
    strlist2d_grow(list, status);

  list->u.fStringList2d.fData[index] = (UChar*) 
    uprv_malloc(sizeof(UChar) * (u_strlen(s) + 1));
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
  
  if(U_FAILURE(*status)) return;

  if(list->fType != eStringList2d) {
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return;
  }
  
  newCapacity = list->u.fStringList2d.fCapacity << 1; 
  
  /* allocate space for the array of strings */
  newData = (UChar**) uprv_malloc(sizeof(UChar*) * newCapacity);
  if(newData == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return;
  }

  /* allocate and copy each string */
  for(i = 0; i < list->u.fStringList2d.fCount; ++i) {
    newData[i] = (UChar*) 
      uprv_malloc(sizeof(UChar) * (u_strlen(list->u.fStringList2d.fData[i]) + 1));
    if(newData[i] == 0) {
      *status = U_MEMORY_ALLOCATION_ERROR;
      for(j = 0; j < i; ++j) 
	uprv_free(newData[j]);
      uprv_free(newData);
      return;
    }
    u_strcpy(newData[i], list->u.fStringList2d.fData[i]);
  }
  
  uprv_free(list->u.fStringList2d.fData);
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

  if(U_FAILURE(*status)) return;

  if(list->fType != eStringList2d) {
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return;
  }

  newCapacity = list->u.fStringList2d.fRowCapacity << 1;
  
  /* allocate space for the array of ints */
  newRows = (int32_t*) uprv_malloc(sizeof(int32_t) * newCapacity);
  if(newRows == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
  }
  
  /* copy each int */
  for(i = 0; i < list->u.fStringList2d.fRowCount; ++i) 
    newRows[i] = list->u.fStringList2d.fRows[i];
  
  /* clean up */
  uprv_free(list->u.fStringList2d.fRows);
  list->u.fStringList2d.fRows = newRows;
  list->u.fStringList2d.fRowCapacity = newCapacity;
}

/* Tagged list */

struct SList*
taglist_open(UErrorCode *status)
{
  struct SList *list;
  
  if(U_FAILURE(*status)) return 0;
  
  list = (struct SList*) uprv_malloc(sizeof(struct SList));
  if(list == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return 0;
  }

  list->fType = eTaggedList;
  
  /*list->u.fTaggedList.fData = 0;*/
  list->u.fTaggedList.fFirst = NULL;
  list->u.fTaggedList.fCount = 0;
  /*list->u.fTaggedList.fCapacity = 32;*/

  /*taglist_grow(list, status);*/
  
  return list;
}

void 
taglist_close(struct SList *list,
	      UErrorCode *status)
{
    struct SStringPair *current;
    struct SStringPair *prev;
    if(U_FAILURE(*status)) return;

  if(list->fType != eTaggedList) {
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return;
  }

    current = list->u.fTaggedList.fFirst;

    while(current != NULL) {
        prev = current;
        current = current->fNext;
        uprv_free(prev);
    }
 
  
    /*uprv_free(list->u.fTaggedList.fData);*/
  
  list->fType = eEmpty;
  uprv_free(list);
}


void
taglist_add(struct SList *list,
	    const UChar *tag, 
	    const UChar *data,
	    UErrorCode *status)
{
  /*int32_t index;*/
  struct SStringPair *pair = NULL;
  struct SStringPair *current = NULL;
  struct SStringPair *prev = NULL;
  
  if(U_FAILURE(*status)) return;

  if(list->fType != eTaggedList) {
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return;
  }
  
  pair = (struct SStringPair *) uprv_malloc(sizeof(struct SStringPair));
  if(pair->fKey == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return;
  }


  pair->fKey = (char*) uprv_malloc(sizeof(char) * (u_strlen(tag) + 1));
  if(pair->fKey == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    uprv_free(pair);
    return;
  }

  pair->fValue = (UChar*) uprv_malloc(sizeof(UChar) * (u_strlen(data) + 1));
  if(pair->fValue == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    uprv_free(pair->fKey);
    uprv_free(pair);
    return;
  }

  ++(list->u.fTaggedList.fCount);

  /*u_strcpy(pair.fKey, tag);*/
  u_UCharsToChars(tag, pair->fKey, u_strlen(tag)+1);
  u_strcpy(pair->fValue, data);

    /* is list still empty? */
    if(list->u.fTaggedList.fFirst == NULL) {
        list->u.fTaggedList.fFirst = pair;
        pair->fNext = NULL;
        return;
    } else {
        current = list->u.fTaggedList.fFirst;
    }

    while(current != NULL) {
        if(uprv_strcmp(current->fKey, pair->fKey)<0) {
            prev = current;
            current = current->fNext;
        } else { /*we're either in front of list, or in middle*/
            if(prev == NULL) { /*front of the list*/
                list->u.fTaggedList.fFirst = pair;
            } else { /*middle of the list*/
                prev->fNext = pair;
            }
            pair->fNext = current;
            return;
        }
    }

    /* end of list */
    prev->fNext = pair;
    pair->fNext = NULL;
  
    /*index = list->u.fTaggedList.fCount;*/
  
    /*if(list->u.fTaggedList.fCount == list->u.fTaggedList.fCapacity)*/
    /*taglist_grow(list, status);*/
  
    /*list->u.fTaggedList.fData[index] = pair;*/
}

const UChar* 
taglist_get(const struct SList *list,
	    const char *tag,
	    UErrorCode *status)
{
  /*int32_t i;*/
  struct SStringPair *current;

  if(U_FAILURE(*status)) return 0;
  
  if(list->fType != eTaggedList) {
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return 0;
  }

    /* is list still empty? */
    if(list->u.fTaggedList.fFirst == NULL) {
        return NULL;
    } else {
        current = list->u.fTaggedList.fFirst;
    }

    while(current != NULL) {
        if(uprv_strcmp(current->fKey, tag)!=0) {
            current = current->fNext;
        } else { /*we're either in front of list, or in middle*/
            return current->fValue;
        }
    }

  return NULL;
}
