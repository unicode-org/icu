/*
*******************************************************************************
*
*   Copyright (C) 1998-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File rblist.c
*
* Modification History:
*
*   Date        Name        Description
*   06/01/99    stephen     Creation.
*******************************************************************************
*/

#include "rblist.h"
#include "ustr.h"
#include "unicode/ustring.h"
#include "cmemory.h"

/* Protos */
static void rblist_grow(struct SRBItemList *list, UErrorCode *status);

struct SRBItem* 
make_rbitem(const UChar *tag, 
	    const struct SList *data, 
	    UErrorCode *status)
{
  struct SRBItem *item;
  UChar *s;

  if(U_FAILURE(*status)) return 0;
  
  item = (struct SRBItem*) uprv_malloc(sizeof(struct SRBItem));
  if(item == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return 0;
  }

  s = (UChar*) uprv_malloc(sizeof(UChar) * (u_strlen(tag) + 1));
  if(s == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return 0;
  }
  u_strcpy(s, tag);

  item->fTag = s;
  item->fData = (struct SList*) data;

  return item;
}

struct SRBItemList*
rblist_open(UErrorCode *status)
{
  struct SRBItemList *list;
  
  if(U_FAILURE(*status)) return 0;
  
  list = (struct SRBItemList*) uprv_malloc(sizeof(struct SRBItemList));
  if(list == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return 0;
  }

  list->fLocale = 0;

  list->fData = 0;
  list->fCount = 0;
  list->fCapacity = 32;

  rblist_grow(list, status);

  return list;
}

void rblist_close(struct SRBItemList *list,
		 UErrorCode *status)
{
  int32_t i;

  if(U_FAILURE(*status)) return;
  
  /* deallocate each list */
  for(i = 0; i < list->fCount; ++i) {
    
    switch(list->fData[i]->fData->fType) {
    case eStringList:
      strlist_close(list->fData[i]->fData, status);
      break;
      
    case eStringList2d:
      strlist2d_close(list->fData[i]->fData, status);
      break;
      
    case eTaggedList:
      taglist_close(list->fData[i]->fData, status);
      break;

    case eEmpty:
      break;
    }
  }
  uprv_free(list->fData);
  uprv_free(list->fLocale);

  uprv_free(list);
}

void rblist_setlocale(struct SRBItemList *list, 
		      const UChar *locale, 
		      UErrorCode *status)
{
  if(U_FAILURE(*status)) return;

  /* Allocate enough space */
  list->fLocale = (UChar*) uprv_realloc(list->fLocale, 
				       sizeof(UChar) * (u_strlen(locale) + 1));
  if(list->fLocale == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return;
  }

  u_strcpy(list->fLocale, locale);
}

void rblist_add(struct SRBItemList *list,
		struct SRBItem *s,
		UErrorCode *status)
{
  int32_t index;
  
  if(U_FAILURE(*status)) return;
  
  index = list->fCount;
  
  if(list->fCount == list->fCapacity) 
    rblist_grow(list, status);
  
  list->fData[index] = s;
  ++(list->fCount);
}

static void 
rblist_grow(struct SRBItemList *list,
	    UErrorCode *status)
{
  int32_t i;
  int32_t newCapacity;
  struct SRBItem **newData;
  
  if(U_FAILURE(*status)) return;
  
  newCapacity = list->fCapacity << 1; 
  
  /* allocate space for the array of SRBItems */
  newData = (struct SRBItem**) 
    uprv_malloc(sizeof(struct SRBItem*) * newCapacity);
  if(newData == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return;
  }

  /* copy each item */
  for(i = 0; i < list->fCount; ++i) {
    newData[i] = list->fData[i];
  }
  
  uprv_free(list->fData);
  list->fData = newData;
  list->fCapacity = newCapacity;
}
