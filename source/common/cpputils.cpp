/**********************************************************************
 *   Copyright (C) 1999-2001, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 **********************************************************************/

#include "unicode/utypes.h"
#include "unicode/unistr.h"
#include "cpputils.h"

/** Simple utility to fill a UChar array from a UnicodeString */
U_CAPI int32_t U_EXPORT2
uprv_fillOutputString(const UnicodeString &temp,
                      UChar *dest, 
                      int32_t destCapacity,
                      UErrorCode *status) {
  int32_t length = temp.length();

  if (destCapacity > 0) {
    // copy the contents; extract() will check if it needs to copy anything at all
    temp.extract(0, destCapacity, dest, 0);

    // zero-terminate the dest buffer if possible
    if (length < destCapacity) {
      dest[length] = 0;
    }
  }

  // set the error code according to the necessary buffer length
  if (length > destCapacity && U_SUCCESS(*status)) {
    *status = U_BUFFER_OVERFLOW_ERROR;
  }

  // return the full string length
  return length;
}
