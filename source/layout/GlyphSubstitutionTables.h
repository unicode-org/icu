/*
 * @(#)GlyphSubstitutionTables.h	1.9 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000, 2001 - All Rights Reserved
 *
 */

#ifndef __GLYPHSUBSTITUTIONTABLES_H
#define __GLYPHSUBSTITUTIONTABLES_H

#include "LETypes.h"
#include "LEGlyphFilter.h"
#include "OpenTypeTables.h"
#include "Lookups.h"
#include "GlyphDefinitionTables.h"
#include "GlyphPositionAdjustments.h"

struct GlyphSubstitutionTableHeader
{
    fixed32 version;
    Offset  scriptListOffset;
    Offset  featureListOffset;
    Offset  lookupListOffset;

    void    process(LEGlyphID *glyphs, const LETag **glyphTags, le_int32 glyphCount,
                 le_bool rightToLeft, LETag scriptTag, LETag languageTag,
                 const GlyphDefinitionTableHeader *glyphDefinitionTableHeader,
                 const LEGlyphFilter *filter = NULL) const;

    le_bool coversScript(LETag scriptTag) const;
};

enum GlyphSubstitutionSubtableTypes
{
    gsstSingle          = 1,
    gsstMultiple        = 2,
    gsstAlternate       = 3,
    gsstLigature        = 4,
    gsstContext         = 5,
    gsstChainingContext = 6
};

typedef LookupSubtable GlyphSubstitutionSubtable;

#endif
