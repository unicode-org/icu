/*
 * %W% %E%
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000, 2001 - All Rights Reserved
 *
 */

#ifndef __LOOKUPPROCESSOR_H
#define __LOOKUPPROCESSOR_H

#include "LETypes.h"
#include "LEFontInstance.h"
#include "OpenTypeTables.h"
#include "Lookups.h"
#include "Features.h"
#include "GlyphDefinitionTables.h"
#include "GlyphPositionAdjustments.h"
#include "GlyphIterator.h"

class LookupProcessor
{
public:
    static LETag notSelected;
    static LETag defaultFeature;

    void process(LEGlyphID *glyphs, GlyphPositionAdjustment *glyphPositionAdjustments, const LETag **glyphTags, le_int32 glyphCount,
                 le_bool rightToLeft, const GlyphDefinitionTableHeader *glyphDefinitionTableHeader, const LEFontInstance *fontInstance) const;

    le_uint32 applyLookupTable(const LookupTable *lookupTable, GlyphIterator *glyphIterator, const LEFontInstance *fontInstance) const;

    le_uint32 applySingleLookup(le_uint16 lookupTableIndex, GlyphIterator *glyphIterator, const LEFontInstance *fontInstance) const;

    virtual le_uint32 applySubtable(const LookupSubtable *lookupSubtable, le_uint16 subtableType,
        GlyphIterator *glyphIterator, const LEFontInstance *fontInstance) const = 0;

    virtual ~LookupProcessor();

protected:
    LookupProcessor(const char *baseAddress,
        Offset scriptListOffset, Offset featureListOffset, Offset lookupListOffset,
        LETag scriptTag, LETag languageTag);

    LookupProcessor();

    LETag selectFeature(le_uint16 featureIndex, LETag tagOverride = notSelected) const;

    const LookupListTable	*lookupListTable;
    const FeatureListTable	*featureListTable;

    LETag					*lookupSelectArray;
    LETag					requiredFeatureTag;
};

#endif
