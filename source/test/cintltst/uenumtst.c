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

void chArrayClose(UEnumeration *en) {
  if(en->currentUChar != NULL) {
    uprv_free(en->currentUChar);
  }
  uprv_free(en);
}

int32_t chArrayCount(UEnumeration *en, UErrorCode *status) {
  if(U_FAILURE(*status)) {
    return 0;
  }
  return en->int1;
}

const UChar* chArrayUNext(UEnumeration *en, int32_t *resultLength, UErrorCode *status) {

  if(U_FAILURE(*status)) {
    return NULL;
  }

  if(en->int2 >= en->int1) {
    return NULL;
  }
  
  if(en->currentUChar == NULL) {
    en->currentUChar = (UChar *)uprv_malloc(1024*sizeof(UChar));
  }

  *resultLength = uprv_strlen(en->currentChar);
  en->currentChar = ((char **)en->context1)[en->int2];
  u_charsToUChars(en->currentChar, en->currentUChar, *resultLength);
  en->int2++;
  return en->currentUChar;
}

const char* chArrayNext(UEnumeration *en, int32_t *resultLength, UErrorCode *status) {
  if(U_FAILURE(*status)) {
    return NULL;
  }

  if(en->int2 >= en->int1) {
    return NULL;
  }
  
  en->currentChar = ((char **)en->context1)[en->int2];
  en->int2++;
  *resultLength = uprv_strlen(en->currentChar);
  return en->currentChar;
}

void chArrayReset(UEnumeration *en, UErrorCode *status) {
  if(U_FAILURE(*status)) {
    return;
  }
  en->int2 = 0;
}

UEnumeration chEnum = {
    NULL, NULL,
    NULL, NULL,
    0, 0,
    chArrayClose,
    chArrayCount,
    chArrayUNext,
    chArrayNext,
    chArrayReset   
};

static UEnumeration *getchArrayEnum(const char** source, int32_t size) {
  UEnumeration *result = (UEnumeration *)uprv_malloc(sizeof(UEnumeration));
  memcpy(result, &chEnum, sizeof(UEnumeration));
  result->context1 = (void *)source;
  result->int1 = size;
  return result;
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