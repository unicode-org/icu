/*
 * %W% %E%
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000, 2001 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "OpenTypeUtilities.h"
#include "LEFontInstance.h"
#include "OpenTypeTables.h"
#include "Features.h"
#include "Lookups.h"
#include "ScriptAndLanguage.h"
#include "GlyphDefinitionTables.h"
#include "GlyphPositionAdjustments.h"
#include "LookupProcessor.h"
#include "LESwaps.h"

U_NAMESPACE_BEGIN

LETag LookupProcessor::notSelected    = 0x00000000;
LETag LookupProcessor::defaultFeature = 0xFFFFFFFF;

LETag emptyTag = 0x00000000;


le_uint32 LookupProcessor::applyLookupTable(const LookupTable *lookupTable, GlyphIterator *glyphIterator,
                                         const LEFontInstance *fontInstance) const
{
    le_uint16 lookupType = SWAPW(lookupTable->lookupType);
    le_uint16 subtableCount = SWAPW(lookupTable->subTableCount);
    le_int32 startPosition = glyphIterator->getCurrStreamPosition();
    le_uint32 delta;

    for (le_uint16 subtable = 0; subtable < subtableCount; subtable += 1) {
        const LookupSubtable *lookupSubtable = lookupTable->getLookupSubtable(subtable);

        delta = applySubtable(lookupSubtable, lookupType, glyphIterator, fontInstance);

        if (delta > 0) {
            return 1;
        }

        glyphIterator->setCurrStreamPosition(startPosition);
    }

    return 1;
}

void LookupProcessor::process(LEGlyphID *glyphs, GlyphPositionAdjustment *glyphPositionAdjustments, const LETag **glyphTags, le_int32 glyphCount,
                              le_bool rightToLeft, const GlyphDefinitionTableHeader *glyphDefinitionTableHeader,
                              const LEFontInstance *fontInstance) const
{
    if (lookupSelectArray == NULL) {
        return;
    }

    for (le_uint16 order = 0; order < lookupOrderCount; order += 1) {
        le_uint16 lookup = lookupOrderArray[order];
        LETag selectTag = lookupSelectArray[lookup];

        if (selectTag != notSelected) {
            const LookupTable *lookupTable = lookupListTable->getLookupTable(lookup);
            le_uint16 lookupFlags = SWAPW(lookupTable->lookupFlags);
            GlyphIterator glyphIterator(glyphs, glyphPositionAdjustments, glyphCount,
                                  rightToLeft, lookupFlags, selectTag, glyphTags,
                                  glyphDefinitionTableHeader);

            while (glyphIterator.findFeatureTag()) {
                le_uint32 delta = 1;

                while (glyphIterator.next(delta)) {
                    delta = applyLookupTable(lookupTable, &glyphIterator, fontInstance);
                }
            }
        }
    }
}

le_uint32 LookupProcessor::applySingleLookup(le_uint16 lookupTableIndex, GlyphIterator *glyphIterator,
                                          const LEFontInstance *fontInstance) const
{
    const LookupTable *lookupTable = lookupListTable->getLookupTable(lookupTableIndex);
    le_uint16 lookupFlags = SWAPW(lookupTable->lookupFlags);
    GlyphIterator tempIterator(*glyphIterator, lookupFlags);
    le_uint32 delta = applyLookupTable(lookupTable, &tempIterator, fontInstance);

    return delta;
}

LETag LookupProcessor::selectFeature(le_uint16 featureIndex, LETag tagOverride) const
{
    LETag featureTag;
    const FeatureTable *featureTable = featureListTable->getFeatureTable(featureIndex, &featureTag);
    le_uint16 lookupCount = featureTable? SWAPW(featureTable->lookupCount) : 0;

    if (tagOverride != notSelected) {
        featureTag = tagOverride;
    }

    for (le_uint16 lookup = 0; lookup < lookupCount; lookup += 1) {
        le_uint16 lookupListIndex = SWAPW(featureTable->lookupListIndexArray[lookup]);

        lookupSelectArray[lookupListIndex] = featureTag;
    }

    return featureTag;
 }


LookupProcessor::LookupProcessor(const char *baseAddress,
        Offset scriptListOffset, Offset featureListOffset, Offset lookupListOffset,
        LETag scriptTag, LETag languageTag, const LETag *featureOrder)
    : lookupListTable(NULL), featureListTable(NULL), lookupSelectArray(NULL),
      requiredFeatureTag(notSelected)
{
    const ScriptListTable *scriptListTable = NULL;
    const LangSysTable *langSysTable = NULL;
    le_uint16 featureCount = 0;
    le_uint16 lookupListCount = 0;
    le_uint16 requiredFeatureIndex;

    if (scriptListOffset != 0) {
        scriptListTable = (const ScriptListTable *) (baseAddress + scriptListOffset);
        langSysTable = scriptListTable->findLanguage(scriptTag, languageTag);

		if (langSysTable != 0) {
			featureCount = SWAPW(langSysTable->featureCount);
		}
    }

    if (featureListOffset != 0) {
        featureListTable = (const FeatureListTable *) (baseAddress + featureListOffset);
    }

    if (lookupListOffset != 0) {
        lookupListTable = (const LookupListTable *) (baseAddress + lookupListOffset);
        lookupListCount = SWAPW(lookupListTable->lookupCount);
    }
    
    if (langSysTable == NULL || featureListTable == NULL || lookupListTable == NULL ||
        featureCount == 0 || lookupListCount == 0) {
        return;
    }
 
    requiredFeatureIndex = SWAPW(langSysTable->reqFeatureIndex);

    lookupSelectArray = new LETag[lookupListCount];

    for (int i = 0; i < lookupListCount; i += 1) {
        lookupSelectArray[i] = notSelected;
    }

    if (requiredFeatureIndex != 0xFFFF) {
        requiredFeatureTag = selectFeature(requiredFeatureIndex, defaultFeature);
    }

    for (le_uint16 feature = 0; feature < featureCount; feature += 1) {
        le_uint16 featureIndex = SWAPW(langSysTable->featureIndexArray[feature]);

        selectFeature(featureIndex);
    }

    int lookup;

    lookupOrderArray = new le_uint16[lookupListCount];

    // FIXME: selectFeature looks at all the lookups for a given feature, and
    // so does this code. It should be possible to combine the processing:
    // use a routine that takes a featureTable (or a feature index if we add
    // a routine to get the feature index from the tag) dumps the lookups into
    // lookupOrderArray starting at a given index, sets the lookupSelectArray,
    // and returns the number of lookups added. Then both branches below can
    // call the routine - the featureOrder branch will sort as it goes, and
    // the other branch will sort when it's done
    if (featureOrder != 0) {
        int tag, order = 0;

	// FIXME: figure out where the default feature goes in all of this...
	// (a hack that will work for Devamt.ttf is to just put it first)
        for (tag = 0; featureOrder[tag] != emptyTag; tag += 1) {
            const FeatureTable *featureTable = featureListTable->getFeatureTable(featureOrder[tag]);
            le_uint16 lookupCount = featureTable? SWAPW(featureTable->lookupCount) : 0;

            if (featureTable != 0) {
                le_uint16 lookupCount = SWAPW(featureTable->lookupCount);

                for (lookup = 0; lookup < lookupCount; lookup += 1) {
                    lookupOrderArray[order + lookup] = SWAPW(featureTable->lookupListIndexArray[lookup]);
                }

                if (lookupCount > 1) {
                    OpenTypeUtilities::sort(&lookupOrderArray[order], lookupCount);
                }

                order += lookupCount;
            }
        }

        lookupOrderCount = order;
    } else {
	// FIXME - lookup up the features from the featureListTable by feature index,
	// and add the lookups to the lookupOrderArray in the order they come. Sort the
	// whole array when finished.
        for (lookup = 0; lookup < lookupListCount; lookup += 1) {
            lookupOrderArray[lookup] = lookup;
        }

        lookupOrderCount = lookupListCount;
    }
}

LookupProcessor::LookupProcessor()
{
}

LookupProcessor::~LookupProcessor()
{
    delete[] lookupOrderArray;
    delete[] lookupSelectArray;
};

U_NAMESPACE_END
