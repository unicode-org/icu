/*
*******************************************************************************
*
*   Copyright (C) 2002, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  uenum.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:2
*
*   created on: 2002jul08
*   created by: Vladimir Weinstein
*/

#include "uenumimp.h"
#include "cmemory.h"

static const UEnumeration nullEnumeration = {
    NULL, /* context */
    NULL, /* close */
    NULL, /* count */
    NULL, /* uNext */
    NULL, /* next */
    NULL  /* reset */
};

void uenum_close(UEnumeration* en) {
  if(en->close != NULL) {
    en->close(en);
  } else { // this seems dangerous, but better kill the object
    uprv_free(en);
  }
}

int32_t uenum_count(UEnumeration* en, UErrorCode* status) {
  if(U_FAILURE(*status)) {
    return -1;
  }
  if(en->count != NULL) {
    return en->count(en, status);
  } else {
    *status = U_UNSUPPORTED_ERROR;
    return -1;
  }
}

const UChar* uenum_unext(UEnumeration* en,
                        int32_t* resultLength,
                        UErrorCode* status) {
  if(U_FAILURE(*status)) {
    return NULL;
  }
  if(en->uNext != NULL) {
    return en->uNext(en, resultLength, status);
  } else {
    *status = U_UNSUPPORTED_ERROR;
    return NULL;
  }
}

const char* uenum_next(UEnumeration* en,
                      int32_t* resultLength,
                      UErrorCode* status) {
  if(U_FAILURE(*status)) {
    return NULL;
  }
  if(en->next != NULL) {
    return en->next(en, resultLength, status);
  } else {
    *status = U_UNSUPPORTED_ERROR;
    return NULL;
  }
}

void uenum_reset(UEnumeration* en, UErrorCode* status) {
  if(U_FAILURE(*status)) {
    return;
  }
  if(en->reset != NULL) {
    en->reset(en, status);
  } else {
    *status = U_UNSUPPORTED_ERROR;
  }
}
