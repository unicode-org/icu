/*
 * %W% %E%
 *
 * (C) Copyright IBM Corp. 1998-2004 - All Rights Reserved
 *
 */

#ifndef __LOOKUPPROCESSOR_H
#define __LOOKUPPROCESSOR_H

/**
 * \file
 * \internal
 */

#include "LETypes.h"
#include "LEFontInstance.h"
#include "OpenTypeTables.h"
#include "Lookups.h"
#include "Features.h"
#include "GlyphDefinitionTables.h"
#include "GlyphPositionAdjustments.h"
#include "GlyphIterator.h"

U_NAMESPACE_BEGIN

class LEGlyphStorage;

class LookupProcessor : public UMemory {
public:
    static const LETag notSelected;
    static const LETag defaultFeature;

    le_int32 process(LEGlyphStorage &glyphStorage, GlyphPositionAdjustment *glyphPositionAdjustments,
                 le_bool rightToLeft, const GlyphDefinitionTableHeader *glyphDefinitionTableHeader, const LEFontInstance *fontInstance) const;

    le_uint32 applyLookupTable(const LookupTable *lookupTable, GlyphIterator *glyphIterator, const LEFontInstance *fontInstance) const;

    le_uint32 applySingleLookup(le_uint16 lookupTableIndex, GlyphIterator *glyphIterator, const LEFontInstance *fontInstance) const;

    virtual le_uint32 applySubtable(const LookupSubtable *lookupSubtable, le_uint16 subtableType,
        GlyphIterator *glyphIterator, const LEFontInstance *fontInstance) const = 0;

    virtual ~LookupProcessor();

protected:
    LookupProcessor(const char *baseAddress,
        Offset scriptListOffset, Offset featureListOffset, Offset lookupListOffset,
        LETag scriptTag, LETag languageTag, const LETag *featureOrder);

    LookupProcessor();

    le_int32 selectLookups(const FeatureTable *featureTable, LETag featureTag, le_int32 order);

    const LookupListTable   *lookupListTable;
    const FeatureListTable  *featureListTable;

    LETag                   *lookupSelectArray;
    LETag                   requiredFeatureTag;

    le_uint16               *lookupOrderArray;
    le_uint32               lookupOrderCount;

private:

    LookupProcessor(const LookupProcessor &other); // forbid copying of this class
    LookupProcessor &operator=(const LookupProcessor &other); // forbid copying of this class
};

U_NAMESPACE_END
#endif
