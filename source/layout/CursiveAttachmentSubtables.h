/*
 * %W% %E%
 *
 * (C) Copyright IBM Corp. 1998-2003 - All Rights Reserved
 *
 */

#ifndef __CURSIVEATTACHMENTSUBTABLES_H
#define __CURSIVEATTACHMENTSUBTABLES_H

/**
 * \file
 * \internal
 */

#include "LETypes.h"
#include "LEFontInstance.h"
#include "OpenTypeTables.h"
#include "GlyphPositioningTables.h"
#include "GlyphIterator.h"

U_NAMESPACE_BEGIN

struct EntryExitRecord
{
    Offset entryAnchor;
    Offset exitAnchor;
};

struct CursiveAttachmentSubtable : GlyphPositioningSubtable
{
    le_uint16 entryExitCount;
    EntryExitRecord entryExitRecords[ANY_NUMBER];

    le_uint32  process(GlyphIterator *glyphIterator, const LEFontInstance *fontInstance) const;
};

U_NAMESPACE_END
#endif


