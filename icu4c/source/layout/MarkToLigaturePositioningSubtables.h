/*
 * @(#)MarkToLigaturePositioningSubtables.h	1.5 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#ifndef __MARKTOLIGATUREPOSITIONINGSUBTABLES_H
#define __MARKTOLIGATUREPOSITIONINGSUBTABLES_H

#include "LETypes.h"
#include "LEFontInstance.h"
#include "OpenTypeTables.h"
#include "GlyphPositioningTables.h"
#include "AttachmentPositioningSubtables.h"
#include "GlyphIterator.h"

struct MarkToLigaturePositioningSubtable : AttachmentPositioningSubtable
{
    le_int32   process(GlyphIterator *glyphIterator, const LEFontInstance *fontInstance);
    LEGlyphID  findLigatureGlyph(GlyphIterator *glyphIterator);
};

struct ComponentRecord
{
    Offset ligatureAnchorTableOffsetArray[ANY_NUMBER];
};

struct LigatureAttachTable
{
    le_uint16 componentCount;
    ComponentRecord componentRecordArray[ANY_NUMBER];
};

struct LigatureArray
{
    le_uint16 ligatureCount;
    Offset ligatureAttachTableOffsetArray[ANY_NUMBER];
};

#endif

