/*
 * @(#)GlyphDefinitionTables.cpp	1.5 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "OpenTypeTables.h"
#include "GlyphDefinitionTables.h"
#include "LESwaps.h"

GlyphClassDefinitionTable *GlyphDefinitionTableHeader::getGlyphClassDefinitionTable()
{
    return (GlyphClassDefinitionTable *) ((char *) this + SWAPW(glyphClassDefOffset));
}

AttachmentListTable *GlyphDefinitionTableHeader::getAttachmentListTable()
{
    return (AttachmentListTable *) ((char *) this + SWAPW(attachListOffset));
}

LigatureCaretListTable *GlyphDefinitionTableHeader::getLigatureCaretListTable()
{
    return (LigatureCaretListTable *) ((char *) this + SWAPW(ligCaretListOffset));
}

MarkAttachClassDefinitionTable *GlyphDefinitionTableHeader::getMarkAttachClassDefinitionTable()
{
    return (MarkAttachClassDefinitionTable *) ((char *) this + SWAPW(MarkAttachClassDefOffset));
}

