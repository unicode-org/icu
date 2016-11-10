/*
 *
 * Copyright (C) 2016 and later: Unicode, Inc. and others. License & terms of use: http://www.unicode.org/copyright.html
 *
 */

#ifndef __LIGATURESUBSTITUTIONSUBTABLES_H
#define __LIGATURESUBSTITUTIONSUBTABLES_H

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

struct LigatureSetTable
{
    le_uint16 ligatureCount;
    Offset    ligatureTableOffsetArray[ANY_NUMBER];
};
LE_VAR_ARRAY(LigatureSetTable, ligatureTableOffsetArray)

struct LigatureTable
{
    TTGlyphID ligGlyph;
    le_uint16 compCount;
    TTGlyphID componentArray[ANY_NUMBER];
};
LE_VAR_ARRAY(LigatureTable, componentArray)

struct LigatureSubstitutionSubtable : GlyphSubstitutionSubtable
{
    le_uint16 ligSetCount;
    Offset    ligSetTableOffsetArray[ANY_NUMBER];

    le_uint32  process(const LETableReference &base, GlyphIterator *glyphIterator, LEErrorCode &success, const LEGlyphFilter *filter = NULL) const;
};
LE_VAR_ARRAY(LigatureSubstitutionSubtable, ligSetTableOffsetArray)

U_NAMESPACE_END
#endif
