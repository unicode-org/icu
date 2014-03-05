/*
*******************************************************************************
* Copyright (C) 2014, International Business Machines Corporation and
* others. All Rights Reserved.
*******************************************************************************
*/

#include "unicode/filteredbrk.h"

U_NAMESPACE_BEGIN

FilteredBreakIteratorBuilder::FilteredBreakIteratorBuilder() {
}

FilteredBreakIteratorBuilder::~FilteredBreakIteratorBuilder() {
}

FilteredBreakIteratorBuilder *
FilteredBreakIteratorBuilder::createInstance(const Locale& /*where*/, UErrorCode& status) {
  if (U_FAILURE(status)) return NULL;

  status = U_UNSUPPORTED_ERROR;
  return NULL;
}


FilteredBreakIteratorBuilder *
FilteredBreakIteratorBuilder::createInstance(UErrorCode& status) {
  status = U_UNSUPPORTED_ERROR;
  return NULL;
}

U_NAMESPACE_END
