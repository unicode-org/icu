/*
 * @(#)AttachmentPositioningSubtables.h	1.6 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#ifndef __ATTACHMENTPOSITIONINGSUBTABLES_H
#define __ATTACHMENTPOSITIONINGSUBTABLES_H

#include "LETypes.h"
#include "OpenTypeTables.h"
#include "GlyphPositioningTables.h"
#include "ValueRecords.h"
#include "GlyphIterator.h"

struct AttachmentPositioningSubtable : GlyphPositioningSubtable
{
    Offset    baseCoverageTableOffset;
    le_uint16 classCount;
    Offset    markArrayOffset;
    Offset    baseArrayOffset;

    le_int32  getBaseCoverage(LEGlyphID baseGlyphId);
    le_uint32 process(GlyphIterator *glyphIterator);
};

inline le_int32 AttachmentPositioningSubtable::getBaseCoverage(LEGlyphID baseGlyphID)
{
    return getGlyphCoverage(baseCoverageTableOffset, baseGlyphID);
}

#endif

