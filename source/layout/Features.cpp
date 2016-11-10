/*
 * @(#)Features.cpp 1.4 00/03/15
 *
 * Copyright (C) 2016 and later: Unicode, Inc. and others. License & terms of use: http://www.unicode.org/copyright.html
 *
 */

#include "LETypes.h"
#include "OpenTypeUtilities.h"
#include "OpenTypeTables.h"
#include "ICUFeatures.h"
#include "LESwaps.h"

U_NAMESPACE_BEGIN

LEReferenceTo<FeatureTable> FeatureListTable::getFeatureTable(const LETableReference &base, le_uint16 featureIndex, LETag *featureTag, LEErrorCode &success) const
{
    LEReferenceToArrayOf<FeatureRecord>
        featureRecordArrayRef(base, success, featureRecordArray, featureIndex+1);

  if (featureIndex >= SWAPW(featureCount) || LE_FAILURE(success)) {
    return LEReferenceTo<FeatureTable>();
  }

    Offset featureTableOffset = featureRecordArray[featureIndex].featureTableOffset;

    *featureTag = SWAPT(featureRecordArray[featureIndex].featureTag);

    return LEReferenceTo<FeatureTable>(base, success, SWAPW(featureTableOffset));
}

#if 0
/*
 * Note: according to the OpenType Spec. v 1.4, the entries in the Feature
 * List Table are sorted alphabetically by feature tag; however, there seem
 * to be some fonts which have an unsorted list; that's why the binary search
 * is #if 0'd out and replaced by a linear search.
 *
 * Also note: as of ICU 2.6, this method isn't called anyhow...
 */
const FeatureTable *FeatureListTable::getFeatureTable(LETag featureTag) const
{
#if 0
    Offset featureTableOffset =
        OpenTypeUtilities::getTagOffset(featureTag, (TagAndOffsetRecord *) featureRecordArray, SWAPW(featureCount));

    if (featureTableOffset == 0) {
        return 0;
    }

    return (const FeatureTable *) ((char *) this + SWAPW(featureTableOffset));
#else
    int count = SWAPW(featureCount);
    
    for (int i = 0; i < count; i += 1) {
        if (SWAPT(featureRecordArray[i].featureTag) == featureTag) {
            return (const FeatureTable *) ((char *) this + SWAPW(featureRecordArray[i].featureTableOffset));
        }
    }

    return 0;
#endif
}
#endif

U_NAMESPACE_END
