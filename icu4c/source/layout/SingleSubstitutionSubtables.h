/*
 * @(#)SingleSubstitutionSubtables.h	1.6 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000, 2001 - All Rights Reserved
 *
 */

#ifndef __SINGLESUBSTITUTIONSUBTABLES_H
#define __SINGLESUBSTITUTIONSUBTABLES_H

#include "LETypes.h"
#include "LEGlyphFilter.h"
#include "OpenTypeTables.h"
#include "GlyphSubstitutionTables.h"
#include "GlyphIterator.h"

struct SingleSubstitutionSubtable : GlyphSubstitutionSubtable
{
    le_uint32  process(GlyphIterator *glyphIterator, const LEGlyphFilter *filter = NULL) const;
};

struct SingleSubstitutionFormat1Subtable : SingleSubstitutionSubtable
{
    le_int16   deltaGlyphID;

    le_uint32  process(GlyphIterator *glyphIterator, const LEGlyphFilter *filter = NULL) const;
};

struct SingleSubstitutionFormat2Subtable : SingleSubstitutionSubtable
{
    le_uint16  glyphCount;
    LEGlyphID  substituteArray[ANY_NUMBER];

    le_uint32  process(GlyphIterator *glyphIterator, const LEGlyphFilter *filter = NULL) const;
};

#endif


