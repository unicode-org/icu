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
* File list.h
*
* Modification History:
*
*   Date        Name        Description
*   06/01/99    stephen     Creation.
*******************************************************************************
*/

#ifndef LIST_H
#define LIST_H 1

#include "utypes.h"

/* A string list */
struct SStringList {
  UChar **fData;
  int32_t fCount;
  int32_t fCapacity;
};

struct SList* strlist_open(UErrorCode *status);
void strlist_close(struct SList *list, UErrorCode *status);
void strlist_add(struct SList *list, const UChar *s, UErrorCode *status);

/* A two-dimensional string list */
struct SStringList2d {
  UChar **fData;
  int32_t fCount;
  int32_t fCapacity;
  
  int32_t *fRows;
  int32_t fRowCount;
  int32_t fRowCapacity;
};

struct SList* strlist2d_open(UErrorCode *status);
void strlist2d_close(struct SList *list, UErrorCode *status);
void strlist2d_newRow(struct SList *list, UErrorCode *status);
void strlist2d_add(struct SList *list, const UChar *s, UErrorCode *status);

/* A name/value pair for a tagged list */
struct SStringPair {
  UChar *fKey;
  UChar *fValue;
};

/* A tagged list */
struct STaggedList {
  struct SStringPair *fData;
  int32_t fCount;
  int32_t fCapacity;
};

struct SList* taglist_open(UErrorCode *status);
void taglist_close(struct SList *list, UErrorCode *status);
void taglist_add(struct SList *list, const UChar *tag, 
		 const UChar *data, UErrorCode *status);
const UChar* taglist_get(const struct SList *list, const UChar *tag, 
			 UErrorCode *status);

/* Types of lists */
enum EListType {
  eEmpty,
  eStringList,
  eStringList2d,
  eTaggedList
};

/* A generic list container */
struct SList {
  enum EListType fType; /* type of element in union */
  
  union {
    struct SStringList fStringList;
    struct SStringList2d fStringList2d;
    struct STaggedList fTaggedList;
  } u;
};

#endif
