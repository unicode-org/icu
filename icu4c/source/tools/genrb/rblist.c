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
#include "cstring.h"


struct SRBItem* 
make_rbitem(const UChar *tag, 
        const struct SList *data, 
        UErrorCode *status)
{
  struct SRBItem *item;
  char *s;

  if(U_FAILURE(*status)) return 0;
  
  item = (struct SRBItem*) uprv_malloc(sizeof(struct SRBItem));
  if(item == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return 0;
  }

  //s = (UChar*) uprv_malloc(sizeof(UChar) * (u_strlen(tag) + 1));
  s = (char*) uprv_malloc(sizeof(char) * (u_strlen(tag) + 1));
  if(s == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return 0;
  }
  u_UCharsToChars(tag, s, u_strlen(tag)+1);
  //u_strcpy(s, tag);

  item->fTag = s;
  item->fData = (struct SList*) data;
  item->fNext = NULL;

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
  list->fFirst = NULL;

//  list->fData = 0; 
  list->fCount = 0;
  list->fCapacity = 32;
  list->fKeys = (char *) uprv_malloc(sizeof(char) * 65532);
  list->fKeyPoint = 0;

  return list;
}

void rblist_close(struct SRBItemList *list,
         UErrorCode *status)
{
//  int32_t i;
  struct SRBItem *current;
  struct SRBItem *prev = NULL;

  if(U_FAILURE(*status)) return;
  current = list->fFirst;
  /* deallocate each list */
//  for(i = 0; i < list->fCount; ++i) {
  while(current != NULL) { 
    
//    switch(list->fData[i]->fData->fType) {
    switch(current->fData->fType) {
        case eStringList:
          strlist_close(current->fData, status);
          break;
      
        case eStringList2d:
          strlist2d_close(current->fData, status);
          break;
      
        case eTaggedList:
          taglist_close(current->fData, status);
          break;

        case eEmpty:
          break;
    }
    prev = current;
    current=current->fNext;
    uprv_free(prev);
  }
//  uprv_free(list->fData);
  uprv_free(list->fLocale);
  uprv_free(list->fKeys);

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
//  int32_t index;

    struct SRBItem *current;
    struct SRBItem *prev = NULL;

    if(U_FAILURE(*status)) return;
    /* here we need to traverse the list */  

    ++(list->fCount);

    s->fStrKey = list->fKeyPoint;

    uprv_strcpy((list->fKeys)+list->fKeyPoint, s->fTag);

    list->fKeyPoint += uprv_strlen(s->fTag)+1;



    /* is list still empty? */
    if(list->fFirst == NULL) {
        list->fFirst = s;
        s->fNext = NULL;
        return;
    } else {
        current = list->fFirst;
    }

    while(current != NULL) {
        if(uprv_strcmp(current->fTag, s->fTag)<0) {
            prev = current;
            current = current->fNext;
        } else { /*we're either in front of list, or in middle*/
            if(prev == NULL) { /*front of the list*/
                list->fFirst = s;
            } else { /*middle of the list*/
                prev->fNext = s;
            }
            s->fNext = current;
            return;
        }
    }

    /* end of list */
    prev->fNext = s;
    s->fNext = NULL;
}

