/*
 * @(#)GlyphSubstitutionTables.h	1.9 00/03/15
 *
 * (C) Copyright IBM Corp. 1998-2003 - All Rights Reserved
 *
 */

#ifndef __GLYPHSUBSTITUTIONTABLES_H
#define __GLYPHSUBSTITUTIONTABLES_H

/**
 * \file
 * \internal
 */

#include "LETypes.h"
#include "LEGlyphFilter.h"
#include "OpenTypeTables.h"
#include "Lookups.h"
#include "GlyphLookupTables.h"
#include "GlyphDefinitionTables.h"
#include "GlyphPositionAdjustments.h"

U_NAMESPACE_BEGIN

struct GlyphSubstitutionTableHeader : public GlyphLookupTableHeader
{
    le_int32    process(LEGlyphID *&glyphs, const LETag **&glyphTags, le_int32 *&charIndices, le_int32 glyphCount,
                 le_bool rightToLeft, LETag scriptTag, LETag languageTag,
                 const GlyphDefinitionTableHeader *glyphDefinitionTableHeader,
                 const LEGlyphFilter *filter = NULL, const LETag *featureOrder = NULL) const;
};

enum GlyphSubstitutionSubtableTypes
{
    gsstSingle          = 1,
    gsstMultiple        = 2,
    gsstAlternate       = 3,
    gsstLigature        = 4,
    gsstContext         = 5,
    gsstChainingContext = 6,
    gsstExtension       = 7,
    gsstReverseChaining = 8
};

typedef LookupSubtable GlyphSubstitutionSubtable;

U_NAMESPACE_END
#endif
