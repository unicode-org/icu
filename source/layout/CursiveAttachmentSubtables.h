/*
 *
 * Copyright (C) 2016 and later: Unicode, Inc. and others. License & terms of use: http://www.unicode.org/copyright.html
 *
 */

#ifndef __CURSIVEATTACHMENTSUBTABLES_H
#define __CURSIVEATTACHMENTSUBTABLES_H

/**
 * \file
 * \internal
 */

#include "LETypes.h"
#include "OpenTypeTables.h"
#include "GlyphPositioningTables.h"

U_NAMESPACE_BEGIN

class LEFontInstance;
class GlyphIterator;

struct EntryExitRecord
{
    Offset entryAnchor;
    Offset exitAnchor;
};

struct CursiveAttachmentSubtable : GlyphPositioningSubtable
{
    le_uint16 entryExitCount;
    EntryExitRecord entryExitRecords[ANY_NUMBER];

    le_uint32  process(const LEReferenceTo<CursiveAttachmentSubtable> &base, GlyphIterator *glyphIterator, const LEFontInstance *fontInstance, LEErrorCode &success) const;
};
LE_VAR_ARRAY(CursiveAttachmentSubtable, entryExitRecords)

U_NAMESPACE_END
#endif


