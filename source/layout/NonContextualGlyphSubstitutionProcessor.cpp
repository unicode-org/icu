/*
 * @(#)NonContextualGlyphSubstitutionProcessor.cpp	1.5 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "MorphTables.h"
#include "SubtableProcessor.h"
#include "NonContextualGlyphSubstitution.h"
#include "NonContextualGlyphSubstitutionProcessor.h"
#include "SimpleArrayProcessor.h"
#include "SegmentSingleProcessor.h"
#include "SegmentArrayProcessor.h"
#include "SingleTableProcessor.h"
#include "TrimmedArrayProcessor.h"
#include "LESwaps.h"

NonContextualGlyphSubstitutionProcessor::NonContextualGlyphSubstitutionProcessor()
{
}

NonContextualGlyphSubstitutionProcessor::NonContextualGlyphSubstitutionProcessor(MorphSubtableHeader *morphSubtableHeader)
	: SubtableProcessor(morphSubtableHeader)
{
}

NonContextualGlyphSubstitutionProcessor::~NonContextualGlyphSubstitutionProcessor()
{
}

SubtableProcessor *NonContextualGlyphSubstitutionProcessor::createInstance(MorphSubtableHeader *morphSubtableHeader)
{
    NonContextualGlyphSubstitutionHeader *header = (NonContextualGlyphSubstitutionHeader *) morphSubtableHeader;

    switch (SWAPW(header->table.format))
    {
    case ltfSimpleArray:
        return new SimpleArrayProcessor(morphSubtableHeader);

    case ltfSegmentSingle:
        return new SegmentSingleProcessor(morphSubtableHeader);

    case ltfSegmentArray:
        return new SegmentArrayProcessor(morphSubtableHeader);

    case ltfSingleTable:
        return new SingleTableProcessor(morphSubtableHeader);

    case ltfTrimmedArray:
        return new TrimmedArrayProcessor(morphSubtableHeader);

    default:
        return NULL;
    }
}
