/*
 * %W% %E%
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#ifndef __GLYPHSUBSTITUTIONLOOKUPPROCESSOR_H
#define __GLYPHSUBSTITUTIONLOOKUPPROCESSOR_H

#include "LETypes.h"
#include "LEGlyphFilter.h"
#include "LEFontInstance.h"
#include "OpenTypeTables.h"
#include "Lookups.h"
#include "Features.h"
#include "GlyphDefinitionTables.h"
#include "GlyphSubstitutionTables.h"
#include "GlyphIterator.h"
#include "LookupProcessor.h"

class GlyphSubstitutionLookupProcessor : public LookupProcessor
{
public:
    GlyphSubstitutionLookupProcessor(GlyphSubstitutionTableHeader *glyphSubstitutionTableHeader,
        LETag scriptTag, LETag languageTag, LEGlyphFilter *filter = NULL);

    virtual ~GlyphSubstitutionLookupProcessor();

    virtual le_uint32 applySubtable(LookupSubtable *lookupSubtable, le_uint16 lookupType, GlyphIterator *glyphIterator,
        const LEFontInstance *fontInstance);

protected:
    GlyphSubstitutionLookupProcessor();

private:
    LEGlyphFilter *fFilter;
};

#endif
