/*
*******************************************************************************
*
*   Copyright (C) 2002, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  uenumtst.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:2
*
*   created on: 2002jul08
*   created by: Vladimir Weinstein
*/

#include "cintltst.h"
#include "uenumimp.h"
#include "cmemory.h"
#include "cstring.h"

static const char* test1[] = {
  "first",
    "second",
    "third",
    "fourth"
};

struct chArrayContext {
  int32_t currIndex;
  int32_t maxIndex;
  char *currChar;
  UChar *currUChar;
  char **array;
};

typedef struct chArrayContext chArrayContext;

#define cont ((chArrayContext *)en->context)

void U_CALLCONV
chArrayClose(UEnumeration *en) {
  if(cont->currUChar != NULL) {
    uprv_free(cont->currUChar);
  }
  uprv_free(en);
}

int32_t U_CALLCONV
chArrayCount(UEnumeration *en, UErrorCode *status) {
  return cont->maxIndex;
}

const UChar* U_CALLCONV 
chArrayUNext(UEnumeration *en, int32_t *resultLength, UErrorCode *status) {
  if(cont->currIndex >= cont->maxIndex) {
    return NULL;
  }
  
  if(cont->currUChar == NULL) {
    cont->currUChar = (UChar *)uprv_malloc(1024*sizeof(UChar));
  }

  cont->currChar = (cont->array)[cont->currIndex];
  *resultLength = uprv_strlen(cont->currChar);
  u_charsToUChars(cont->currChar, cont->currUChar, *resultLength);
  cont->currIndex++;
  return cont->currUChar;
}

const char* U_CALLCONV
chArrayNext(UEnumeration *en, int32_t *resultLength, UErrorCode *status) {
  if(cont->currIndex >= cont->maxIndex) {
    return NULL;
  }
  
  cont->currChar = (cont->array)[cont->currIndex];
  *resultLength = uprv_strlen(cont->currChar);
  cont->currIndex++;
  return cont->currChar;
}

void U_CALLCONV
chArrayReset(UEnumeration *en, UErrorCode *status) {
  cont->currIndex = 0;
}

chArrayContext myCont = {
  0, 0,
    NULL, NULL,
    NULL
};

UEnumeration chEnum = {
    &myCont,
    chArrayClose,
    chArrayCount,
    chArrayUNext,
    chArrayNext,
    chArrayReset   
};

static UEnumeration *getchArrayEnum(const char** source, int32_t size) {
  UEnumeration *en = (UEnumeration *)uprv_malloc(sizeof(UEnumeration));
  memcpy(en, &chEnum, sizeof(UEnumeration));
  cont->array = (char **)source;
  cont->maxIndex = size;
  return en;
}

static void EnumerationTest(void) {
  UErrorCode status = U_ZERO_ERROR;
  int32_t len = 0;
  UEnumeration *en = getchArrayEnum(test1, sizeof(test1)/sizeof(test1[0]));
  const char *string = NULL;
  const UChar *uString = NULL;
  while(string = uenum_next(en, &len, &status)) {
    log_verbose("read %s, length %i\n", string, len);
  }
  uenum_reset(en, &status);
  while(uString = uenum_unext(en, &len, &status)) {
    log_verbose("read uchar of len %i\n", len);
  }

  uenum_close(en);
}

void addEnumerationTest(TestNode** root)
{
    addTest(root, &EnumerationTest, "tsutil/uenumtst/EnumerationTest");
}