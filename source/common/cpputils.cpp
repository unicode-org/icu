#define  EXTENDED_FUNCTIONALITY
#include "cpputils.h"
#include "cstring.h"
#include "unicode/ustring.h"

/**********************************************************************
 *   Copyright (C) 1999, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 **********************************************************************/


/******************************************************
 * Simple utility to set output buffer parameters
 ******************************************************/
void T_fillOutputParams(const UnicodeString* temp,
                        UChar* result, 
                        const int32_t resultLength,
                        int32_t* resultLengthOut, 
                        UErrorCode* status) 
{
  int32_t actual = temp->length();

  if (resultLength > 0) {
    // copy the contents; extract() will check if it needs to copy anything at all
    temp->extract(0, resultLength - 1, result, 0);

    // zero-terminate the result buffer
    if (actual < resultLength) {
      result[actual] = 0;
    } else {
      result[resultLength - 1] = 0;
    }
  }

  // set the output length to the actual string length
  if (resultLengthOut != 0) {
    *resultLengthOut = actual;
  }

  // set the error code according to the necessary buffer length
  if (actual >= resultLength && U_SUCCESS(*status)) {
    *status = U_BUFFER_OVERFLOW_ERROR;
  }
}
