/*
 *
 * (C) Copyright IBM Corp.  and others 1998-2013 - All Rights Reserved
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

SubtableProcessor2::SubtableProcessor2(const MorphSubtableHeader2 *morphSubtableHeader)
{
    subtableHeader = morphSubtableHeader;
    
    length = SWAPL(subtableHeader->length);
    coverage = SWAPL(subtableHeader->coverage);
    subtableFeatures = SWAPL(subtableHeader->subtableFeatures);
}

SubtableProcessor2::~SubtableProcessor2()
{
}

U_NAMESPACE_END
