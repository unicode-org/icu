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

void uenum_close(UEnumeration* en) {
  en->close(en);
}

int32_t uenum_count(UEnumeration* en, UErrorCode* status) {
  return en->count(en, status);
}

const UChar* uenum_unext(UEnumeration* en,
                        int32_t* resultLength,
                        UErrorCode* status) {
  return en->uNext(en, resultLength, status);
}

const char* uenum_next(UEnumeration* en,
                      int32_t* resultLength,
                      UErrorCode* status) {
  return en->next(en, resultLength, status);
}

void uenum_reset(UEnumeration* en, UErrorCode* status) {
  en->reset(en, status);
}
