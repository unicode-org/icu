/*
 *
 * (C) Copyright IBM Corp. 1998-2004 - All Rights Reserved
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
#include "LEGlyphStorage.h"
#include "LESwaps.h"

U_NAMESPACE_BEGIN

le_int32 GlyphSubstitutionTableHeader::process(LEGlyphStorage &glyphStorage, le_bool rightToLeft, LETag scriptTag, LETag languageTag,
                                           const GlyphDefinitionTableHeader *glyphDefinitionTableHeader,
                                           const LEGlyphFilter *filter, const LETag *featureOrder) const
{
    GlyphSubstitutionLookupProcessor processor(this, scriptTag, languageTag, filter, featureOrder);

    return processor.process(glyphStorage, NULL, rightToLeft, glyphDefinitionTableHeader, NULL);
}

U_NAMESPACE_END
