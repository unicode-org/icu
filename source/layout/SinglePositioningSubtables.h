/*
 * @(#)SinglePositioningSubtables.h	1.6 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#ifndef __SINGLEPOSITIONINGSUBTABLES_H
#define __SINGLEPOSITIONINGSUBTABLES_H

#include "LETypes.h"
#include "LEFontInstance.h"
#include "OpenTypeTables.h"
#include "GlyphPositioningTables.h"
#include "ValueRecords.h"
#include "GlyphIterator.h"

struct SinglePositioningSubtable : GlyphPositioningSubtable
{
    le_uint32  process(GlyphIterator *glyphIterator, LEFontInstance *fontInstance);
};

struct SinglePositioningFormat1Subtable : SinglePositioningSubtable
{
    ValueFormat valueFormat;
    ValueRecord valueRecord;

    le_uint32  process(GlyphIterator *glyphIterator, LEFontInstance *fontInstance);
};

struct SinglePositioningFormat2Subtable : SinglePositioningSubtable
{
    ValueFormat valueFormat;
    le_uint16      valueCount;
    ValueRecord valueRecordArray[ANY_NUMBER];

    le_uint32  process(GlyphIterator *glyphIterator, LEFontInstance *fontInstance);
};

#endif


