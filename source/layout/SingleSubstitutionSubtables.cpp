/*
 * @(#)SingleSubstitutionSubtables.cpp	1.6 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "LEGlyphFilter.h"
#include "OpenTypeTables.h"
#include "GlyphSubstitutionTables.h"
#include "SingleSubstitutionSubtables.h"
#include "GlyphIterator.h"
#include "LESwaps.h"

le_uint32 SingleSubstitutionSubtable::process(GlyphIterator *glyphIterator, LEGlyphFilter *filter)
{
    switch(SWAPW(subtableFormat))
    {
    case 0:
        return 0;

    case 1:
    {
        SingleSubstitutionFormat1Subtable *subtable = (SingleSubstitutionFormat1Subtable *) this;

        return subtable->process(glyphIterator, filter);
    }

    case 2:
    {
        SingleSubstitutionFormat2Subtable *subtable = (SingleSubstitutionFormat2Subtable *) this;

        return subtable->process(glyphIterator, filter);
    }

    default:
        return 0;
    }
}

le_uint32 SingleSubstitutionFormat1Subtable::process(GlyphIterator *glyphIterator, LEGlyphFilter *filter)
{
    LEGlyphID glyph = (LEGlyphID) glyphIterator->getCurrGlyphID();
    le_int32 coverageIndex = getGlyphCoverage(glyph);

    if (coverageIndex >= 0)
    {
        LEGlyphID substitute = glyph + SWAPW(deltaGlyphID);

        if (filter == NULL || filter->accept(substitute)) {
            glyphIterator->setCurrGlyphID(substitute);
        }

        return 1;
    }

    return 0;
}

le_uint32 SingleSubstitutionFormat2Subtable::process(GlyphIterator *glyphIterator, LEGlyphFilter *filter)
{
    LEGlyphID glyph = (LEGlyphID) glyphIterator->getCurrGlyphID();
    le_int32 coverageIndex = getGlyphCoverage(glyph);

    if (coverageIndex >= 0)
    {
        LEGlyphID substitute = SWAPW(substituteArray[coverageIndex]);

        if (filter == NULL || filter->accept(substitute)) {
            glyphIterator->setCurrGlyphID(substitute);
        }

        return 1;
    }

    return 0;
}

