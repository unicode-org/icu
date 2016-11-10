/*
 *
 * Copyright (C) 2016 and later: Unicode, Inc. and others. License & terms of use: http://www.unicode.org/copyright.html
 *
 */

#ifndef __ALTERNATESUBSTITUTIONSUBTABLES_H
#define __ALTERNATESUBSTITUTIONSUBTABLES_H

/**
 * \file
 * \internal
 */

#include "LETypes.h"
#include "LEGlyphFilter.h"
#include "OpenTypeTables.h"
#include "GlyphSubstitutionTables.h"
#include "GlyphIterator.h"

U_NAMESPACE_BEGIN

struct AlternateSetTable
{
    le_uint16 glyphCount;
    TTGlyphID alternateArray[ANY_NUMBER];
};

LE_VAR_ARRAY(AlternateSetTable, alternateArray)

struct AlternateSubstitutionSubtable : GlyphSubstitutionSubtable
{
    le_uint16 alternateSetCount;
    Offset    alternateSetTableOffsetArray[ANY_NUMBER];

    le_uint32 process(const LEReferenceTo<AlternateSubstitutionSubtable> &base, GlyphIterator *glyphIterator, LEErrorCode &success, const LEGlyphFilter *filter = NULL) const;
};

LE_VAR_ARRAY(AlternateSubstitutionSubtable, alternateSetTableOffsetArray)

U_NAMESPACE_END
#endif
