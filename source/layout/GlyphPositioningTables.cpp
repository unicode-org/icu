/*
 *
 * (C) Copyright IBM Corp. 1998-2005 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "LEFontInstance.h"
#include "OpenTypeTables.h"
#include "Lookups.h"
#include "GlyphDefinitionTables.h"
#include "GlyphPositioningTables.h"
#include "GlyphPosnLookupProc.h"
#include "CursiveAttachmentSubtables.h"
#include "LEGlyphStorage.h"
#include "GlyphPositionAdjustments.h"

U_NAMESPACE_BEGIN

void GlyphPositioningTableHeader::process(LEGlyphStorage &glyphStorage, GlyphPositionAdjustments *glyphPositionAdjustments, le_bool rightToLeft,
                                          LETag scriptTag, LETag languageTag,
                                          const GlyphDefinitionTableHeader *glyphDefinitionTableHeader,
                                          const LEFontInstance *fontInstance, const LETag *featureOrder) const
{
    GlyphPositioningLookupProcessor processor(this, scriptTag, languageTag, featureOrder);

    processor.process(glyphStorage, glyphPositionAdjustments, rightToLeft, glyphDefinitionTableHeader, fontInstance);

    glyphPositionAdjustments->applyCursiveAdjustments(glyphStorage, rightToLeft, fontInstance);
}

U_NAMESPACE_END
