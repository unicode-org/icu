/*
 * @(#)MorphTables.cpp	1.6 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */


#include "LETypes.h"
#include "LayoutTables.h"
#include "MorphTables.h"
#include "SubtableProcessor.h"
#include "IndicRearrangementProcessor.h"
#include "ContextualGlyphSubstitutionProcessor.h"
#include "LigatureSubstitutionProcessor.h"
#include "NonContextualGlyphSubstitutionProcessor.h"
//#include "ContextualGlyphInsertionProcessor.h"
#include "LESwaps.h"

void MorphTableHeader::process(LEGlyphID *glyphs, le_int32 *glyphIndices, le_int32 glyphCount)
{
    ChainHeader *chainHeader = chains;
    le_uint32 chainCount = SWAPL(this->nChains);
    le_uint32 chain;

    for (chain = 0; chain < chainCount; chain += 1)
    {
        FeatureFlags defaultFlags = SWAPL(chainHeader->defaultFlags);
        le_uint32 chainLength = SWAPL(chainHeader->chainLength);
        le_int16 nFeatureEntries = SWAPW(chainHeader->nFeatureEntries);
        le_int16 nSubtables = SWAPW(chainHeader->nSubtables);
        MorphSubtableHeader *subtableHeader =
            (MorphSubtableHeader *)&chainHeader->featureTable[nFeatureEntries];
        le_int16 subtable;

        for (subtable = 0; subtable < nSubtables; subtable += 1)
        {
            le_int16 length = SWAPW(subtableHeader->length);
            SubtableCoverage coverage = SWAPW(subtableHeader->coverage);
            FeatureFlags subtableFeatures = SWAPL(subtableHeader->subtableFeatures);

            // should check coverage too, really...
            if ((subtableFeatures & defaultFlags) != 0)
            {
                subtableHeader->process(glyphs, glyphIndices, glyphCount);
            }

            subtableHeader = (MorphSubtableHeader *) ((char *)subtableHeader + length);
        }

        chainHeader = (ChainHeader *)((char *)chainHeader + chainLength);
    }
}

void MorphSubtableHeader::process(LEGlyphID *glyphs, le_int32 *glyphIndices, le_int32 glyphCount)
{
    SubtableProcessor *processor = NULL;

    switch (SWAPW(coverage) & scfTypeMask)
    {
    case mstIndicRearrangement:
        processor = new IndicRearrangementProcessor(this);
        break;

    case mstContextualGlyphSubstitution:
        processor = new ContextualGlyphSubstitutionProcessor(this);
        break;

    case mstLigatureSubstitution:
        processor = new LigatureSubstitutionProcessor(this);
        break;

    case mstReservedUnused:
        break;

    case mstNonContextualGlyphSubstitution:
        processor = NonContextualGlyphSubstitutionProcessor::createInstance(this);
        break;

    /*
    case mstContextualGlyphInsertion:
        processor = new ContextualGlyphInsertionProcessor(this);
        break;
    */

    default:
        break;
    }

    if (processor != NULL)
    {
        processor->process(glyphs, glyphIndices, glyphCount);
        delete processor;
    }
}

