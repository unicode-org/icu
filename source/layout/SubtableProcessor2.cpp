/*
 *
 * Copyright (C) 2016 and later: Unicode, Inc. and others. License & terms of use: http://www.unicode.org/copyright.html
 *
 */

#include "LETypes.h"
#include "MorphTables.h"
#include "SubtableProcessor2.h"
#include "LESwaps.h"

U_NAMESPACE_BEGIN

SubtableProcessor2::SubtableProcessor2()
{
}

SubtableProcessor2::SubtableProcessor2(const LEReferenceTo<MorphSubtableHeader2> &morphSubtableHeader, LEErrorCode &success)
  : length(0), coverage(0), subtableFeatures(0L), subtableHeader(morphSubtableHeader, success)
{
  if(LE_FAILURE(success)) return;

  length = SWAPL(subtableHeader->length);
  coverage = SWAPL(subtableHeader->coverage);
  subtableFeatures = SWAPL(subtableHeader->subtableFeatures);
}

SubtableProcessor2::~SubtableProcessor2()
{
}

U_NAMESPACE_END
