/*
 * @(#)GlyphPositioningTables.h	1.7 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000, 2001 - All Rights Reserved
 *
 */

#ifndef __GLYPHPOSITIONINGTABLES_H
#define __GLYPHPOSITIONINGTABLES_H

#include "LETypes.h"
#include "LEFontInstance.h"
#include "OpenTypeTables.h"
#include "Lookups.h"
#include "GlyphDefinitionTables.h"
#include "GlyphPositionAdjustments.h"

U_NAMESPACE_BEGIN

struct GlyphPositioningTableHeader
{
    fixed32 version;
    Offset  scriptListOffset;
    Offset  featureListOffset;
    Offset  lookupListOffset;

    void    process(LEUnicode *glyphs, GlyphPositionAdjustment *glyphPositionAdjustments,
                const LETag **glyphTags, le_int32 glyphCount,
                le_bool rightToLeft, LETag scriptTag, LETag languageTag,
                const GlyphDefinitionTableHeader *glyphDefinitionTableHeader,
                const LEFontInstance *fontInstance) const;
};

enum GlyphPositioningSubtableTypes
{
    gpstSingle          = 1,
    gpstPair            = 2,
    gpstCursive         = 3,
    gpstMarkToBase      = 4,
    gpstMarkToLigature  = 5,
    gpstMarkToMark      = 6,
    gpstContext         = 7,
    gpstChainedContext  = 8
};

typedef LookupSubtable GlyphPositioningSubtable;

U_NAMESPACE_END
#endif
