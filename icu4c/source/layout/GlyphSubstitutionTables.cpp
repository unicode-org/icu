/*
 * @(#)GlyphSubstitutionTables.cpp	1.9 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "LEGlyphFilter.h"
#include "OpenTypeTables.h"
#include "Lookups.h"
#include "GlyphDefinitionTables.h"
#include "GlyphPositionAdjustments.h"
#include "GlyphSubstitutionTables.h"
#include "GlyphSubstLookupProc.h"
#include "ScriptAndLanguage.h"
#include "LESwaps.h"

void GlyphSubstitutionTableHeader::process(LEGlyphID *glyphs, const LETag **glyphTags, le_int32 glyphCount,
                                           le_bool rightToLeft, LETag scriptTag, LETag languageTag,
                                           const GlyphDefinitionTableHeader *glyphDefinitionTableHeader,
                                           const LEGlyphFilter *filter) const
{
    GlyphSubstitutionLookupProcessor processor(this, scriptTag, languageTag, filter);

    processor.process(glyphs, NULL, glyphTags, glyphCount, rightToLeft, glyphDefinitionTableHeader, NULL);
}

le_bool GlyphSubstitutionTableHeader::coversScript(LETag scriptTag) const
{
    const ScriptListTable *scriptListTable = (const ScriptListTable *) ((char *)this + SWAPW(scriptListOffset));

    return scriptListTable->findScript(scriptTag) != NULL;
}

