/*
 *
 * (C) Copyright IBM Corp. 1998-2004 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "LEFontInstance.h"
#include "OpenTypeTables.h"
#include "Lookups.h"
#include "GlyphDefinitionTables.h"
#include "GlyphPositionAdjustments.h"
#include "GlyphPositioningTables.h"
#include "GlyphPosnLookupProc.h"
#include "LEGlyphStorage.h"

U_NAMESPACE_BEGIN

void GlyphPositioningTableHeader::process(LEGlyphStorage &glyphStorage, GlyphPositionAdjustment *glyphPositionAdjustments, le_bool rightToLeft,
                                          LETag scriptTag, LETag languageTag,
                                          const GlyphDefinitionTableHeader *glyphDefinitionTableHeader,
                                          const LEFontInstance *fontInstance, const LETag *featureOrder) const
{
    GlyphPositioningLookupProcessor processor(this, scriptTag, languageTag, featureOrder);

    processor.process(glyphStorage, glyphPositionAdjustments, rightToLeft, glyphDefinitionTableHeader, fontInstance);
}

U_NAMESPACE_END
