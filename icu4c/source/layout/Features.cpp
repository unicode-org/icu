/*
 * @(#)Features.cpp	1.4 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "OpenTypeTables.h"
#include "Features.h"
#include "LESwaps.h"

FeatureTable *FeatureListTable::getFeatureTable(le_uint16 featureIndex, LETag *featureTag)
{
    if (featureIndex >= SWAPW(featureCount))
    {
        return 0;
    }

    Offset featureTableOffset = featureRecordArray[featureIndex].featureTableOffset;

    *featureTag = SWAPT(featureRecordArray[featureIndex].featureTag);

    return (FeatureTable *) ((char *) this + SWAPW(featureTableOffset));
}

