/*
 *
 * (C) Copyright IBM Corp. 1998-2004 - All Rights Reserved
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
#include "LEGlyphStorage.h"
#include "LESwaps.h"

U_NAMESPACE_BEGIN

const LETag LookupProcessor::notSelected    = 0x00000000;
const LETag LookupProcessor::defaultFeature = 0xFFFFFFFF;

const LETag emptyTag = 0x00000000;


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

le_int32 LookupProcessor::process(LEGlyphStorage &glyphStorage, GlyphPositionAdjustment *glyphPositionAdjustments,
                              le_bool rightToLeft, const GlyphDefinitionTableHeader *glyphDefinitionTableHeader,
                              const LEFontInstance *fontInstance) const
{
    le_int32 glyphCount = glyphStorage.getGlyphCount();

    if (lookupSelectArray == NULL) {
        return glyphCount;
    }

    GlyphIterator glyphIterator(glyphStorage, glyphPositionAdjustments,
                                rightToLeft, 0, 0, glyphDefinitionTableHeader);
    le_int32 newGlyphCount = glyphCount;

    for (le_uint16 order = 0; order < lookupOrderCount; order += 1) {
        le_uint16 lookup = lookupOrderArray[order];
        LETag selectTag = lookupSelectArray[lookup];

        if (selectTag != notSelected) {
            const LookupTable *lookupTable = lookupListTable->getLookupTable(lookup);
            le_uint16 lookupFlags = SWAPW(lookupTable->lookupFlags);
            
            glyphIterator.reset(lookupFlags, selectTag);

            while (glyphIterator.findFeatureTag()) {
                le_uint32 delta = 1;

                while (glyphIterator.next(delta)) {
                    delta = applyLookupTable(lookupTable, &glyphIterator, fontInstance);
                }
            }

            newGlyphCount = glyphIterator.applyInsertions();
        }
    }

    return newGlyphCount;
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

le_int32 LookupProcessor::selectLookups(const FeatureTable *featureTable, LETag featureTag, le_int32 order)
{
    le_uint16 lookupCount = featureTable? SWAPW(featureTable->lookupCount) : 0;

    for (le_uint16 lookup = 0; lookup < lookupCount; lookup += 1) {
        le_uint16 lookupListIndex = SWAPW(featureTable->lookupListIndexArray[lookup]);

        lookupSelectArray[lookupListIndex] = featureTag;
        lookupOrderArray[order + lookup]   = lookupListIndex;
    }

    return lookupCount;
}

LookupProcessor::LookupProcessor(const char *baseAddress,
        Offset scriptListOffset, Offset featureListOffset, Offset lookupListOffset,
        LETag scriptTag, LETag languageTag, const LETag *featureOrder)
    : lookupListTable(NULL), featureListTable(NULL), lookupSelectArray(NULL),
      requiredFeatureTag(notSelected), lookupOrderArray(NULL), lookupOrderCount(0)
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

    lookupSelectArray = LE_NEW_ARRAY(LETag, lookupListCount);

    for (int i = 0; i < lookupListCount; i += 1) {
        lookupSelectArray[i] = notSelected;
    }

    le_int32 count, order = 0;
    const FeatureTable *featureTable = 0;
    LETag featureTag;

    lookupOrderArray = LE_NEW_ARRAY(le_uint16, lookupListCount);

    if (requiredFeatureIndex != 0xFFFF) {
        featureTable = featureListTable->getFeatureTable(requiredFeatureIndex, &featureTag);
        order += selectLookups(featureTable, defaultFeature, order);
    }

    if (featureOrder != NULL) {
        if (order > 1) {
            OpenTypeUtilities::sort(lookupOrderArray, order);
        }

        for (le_int32 tag = 0; featureOrder[tag] != emptyTag; tag += 1) {
            featureTag = featureOrder[tag];
            count = 0;

            for (le_uint16 feature = 0; feature < featureCount; feature += 1) {
                le_uint16 featureIndex = SWAPW(langSysTable->featureIndexArray[feature]);

                featureTable = featureListTable->getFeatureTable(featureIndex, &featureTag);

                if (featureTag == featureOrder[tag]) {
                    count += selectLookups(featureTable, featureTag, order + count);
                }
            }

            if (count > 1) {
                OpenTypeUtilities::sort(&lookupOrderArray[order], count);
            }

            order += count;
        }
    } else {
        for (le_uint16 feature = 0; feature < featureCount; feature += 1) {
            le_uint16 featureIndex = SWAPW(langSysTable->featureIndexArray[feature]);
 
            featureTable = featureListTable->getFeatureTable(featureIndex, &featureTag);
            count = selectLookups(featureTable, featureTag, order);
            order += count;
        }

        if (order > 1) {
            OpenTypeUtilities::sort(lookupOrderArray, order);
        }
    }

    lookupOrderCount = order;
}

LookupProcessor::LookupProcessor()
{
}

LookupProcessor::~LookupProcessor()
{
    LE_DELETE_ARRAY(lookupOrderArray);
    LE_DELETE_ARRAY(lookupSelectArray);
};

U_NAMESPACE_END
