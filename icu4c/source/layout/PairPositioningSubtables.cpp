/*
 * @(#)PairPositioningSubtables.cpp	1.7 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "LEFontInstance.h"
#include "OpenTypeTables.h"
#include "GlyphPositioningTables.h"
#include "PairPositioningSubtables.h"
#include "ValueRecords.h"
#include "GlyphIterator.h"
#include "GlyphPositionAdjustments.h"
#include "OpenTypeUtilities.h"
#include "LESwaps.h"

le_uint32 PairPositioningSubtable::process(GlyphIterator *glyphIterator, const LEFontInstance *fontInstance)
{
    switch(SWAPW(subtableFormat))
    {
    case 0:
        return 0;

    case 1:
    {
        PairPositioningFormat1Subtable *subtable = (PairPositioningFormat1Subtable *) this;

        return subtable->process(glyphIterator, fontInstance);
    }

    case 2:
    {
        PairPositioningFormat2Subtable *subtable = (PairPositioningFormat2Subtable *) this;

        return subtable->process(glyphIterator, fontInstance);
    }

    default:
        return 0;
    }
}

le_uint32 PairPositioningFormat1Subtable::process(GlyphIterator *glyphIterator, const LEFontInstance *fontInstance)
{
    LEGlyphID firstGlyph = (LEGlyphID) glyphIterator->getCurrGlyphID();
    le_int32 coverageIndex = getGlyphCoverage(firstGlyph);
    GlyphIterator tempIterator(*glyphIterator);

    if (coverageIndex >= 0 && glyphIterator->next()) {
        Offset pairSetTableOffset = SWAPW(pairSetTableOffsetArray[coverageIndex]);
        PairSetTable *pairSetTable = (PairSetTable *) ((char *) this + pairSetTableOffset);
        le_int16 valueRecord1Size = ValueRecord::getSize(SWAPW(valueFormat1));
        le_int16 valueRecord2Size = ValueRecord::getSize(SWAPW(valueFormat2));
        le_int16 recordSize = sizeof(PairValueRecord) - sizeof(ValueRecord) + valueRecord1Size + valueRecord2Size;
        LEGlyphID secondGlyph = (LEGlyphID) glyphIterator->getCurrGlyphID();
        PairValueRecord *pairValueRecord =
            findPairValueRecord(secondGlyph, pairSetTable->pairValueRecordArray, SWAPW(pairSetTable->pairValueCount), recordSize);

        if (pairValueRecord == NULL) {
            return 0;
        }

        if (valueFormat1 != 0) {
            GlyphPositionAdjustment adjustment;

            tempIterator.getCurrGlyphPositionAdjustment(adjustment);
            pairValueRecord->valueRecord1.adjustPosition(SWAPW(valueFormat1), (char *) this, adjustment, fontInstance);
            tempIterator.setCurrGlyphPositionAdjustment(&adjustment);
        }

        if (valueFormat2 != 0) {
            ValueRecord *valueRecord2 = (ValueRecord *) ((char *) &pairValueRecord->valueRecord1 + valueRecord1Size);
            GlyphPositionAdjustment adjustment;

            glyphIterator->getCurrGlyphPositionAdjustment(adjustment);
            valueRecord2->adjustPosition(SWAPW(valueFormat2), (char *) this, adjustment, fontInstance);
            glyphIterator->setCurrGlyphPositionAdjustment(&adjustment);
        }

        return 2;
    }

    return 0;
}

le_uint32 PairPositioningFormat2Subtable::process(GlyphIterator *glyphIterator, const LEFontInstance *fontInstance)
{
    LEGlyphID firstGlyph = (LEGlyphID) glyphIterator->getCurrGlyphID();
    le_int32 coverageIndex = getGlyphCoverage(firstGlyph);
    GlyphIterator tempIterator(*glyphIterator);

    if (coverageIndex >= 0 && glyphIterator->next()) {
        LEGlyphID secondGlyph = (LEGlyphID) glyphIterator->getCurrGlyphID();
        ClassDefinitionTable *classDef1 = (ClassDefinitionTable *) ((char *) this + SWAPW(classDef1Offset));
        ClassDefinitionTable *classDef2 = (ClassDefinitionTable *) ((char *) this + SWAPW(classDef2Offset));
        le_int32 class1 = classDef1->getGlyphClass(firstGlyph);
        le_int32 class2 = classDef2->getGlyphClass(secondGlyph);
        le_int16 valueRecord1Size = ValueRecord::getSize(SWAPW(valueFormat1));
        le_int16 valueRecord2Size = ValueRecord::getSize(SWAPW(valueFormat2));
        le_int16 class2RecordSize = valueRecord1Size + valueRecord2Size;
        le_int16 class1RecordSize = class2RecordSize * SWAPW(class2Count);
        Class1Record *class1Record = (Class1Record *) ((char *) class1RecordArray + (class1RecordSize * class1));
        Class2Record *class2Record = (Class2Record *) ((char *) class1Record->class2RecordArray + (class2RecordSize * class2));


        if (valueFormat1 != 0) {
            GlyphPositionAdjustment adjustment;

            tempIterator.getCurrGlyphPositionAdjustment(adjustment);
            class2Record->valueRecord1.adjustPosition(SWAPW(valueFormat1), (char *) this, adjustment, fontInstance);
            tempIterator.setCurrGlyphPositionAdjustment(&adjustment);
        }

        if (valueFormat2 != 0) {
            ValueRecord *valueRecord2 = (ValueRecord *) ((char *) &class2Record->valueRecord1 + valueRecord1Size);
            GlyphPositionAdjustment adjustment;

            glyphIterator->getCurrGlyphPositionAdjustment(adjustment);
            valueRecord2->adjustPosition(SWAPW(valueFormat2), (char *) this, adjustment, fontInstance);
            glyphIterator->setCurrGlyphPositionAdjustment(&adjustment);
        }

        return 2;
    }

    return 0;
}

PairValueRecord *PairPositioningFormat1Subtable::findPairValueRecord(LEGlyphID glyphID, PairValueRecord *records, le_uint16 recordCount, le_uint16 recordSize)
{
    le_uint8 bit = OpenTypeUtilities::highBit(recordCount);
    le_uint16 power = 1 << bit;
    le_uint16 extra = (recordCount - power) * recordSize;
    le_uint16 probe = power * recordSize;
    PairValueRecord *record = records;
    PairValueRecord *trial = (PairValueRecord *) ((char *) record + extra);

    if (SWAPW(trial->secondGlyph) <= glyphID) {
        record = trial;
    }

    while (probe > recordSize) {
        probe >>= 1;
        trial = (PairValueRecord *) ((char *) record + probe);

        if (SWAPW(trial->secondGlyph) <= glyphID) {
            record = trial;
        }
    }

    if (SWAPW(record->secondGlyph) == glyphID) {
        return record;
    }

    return NULL;
}
