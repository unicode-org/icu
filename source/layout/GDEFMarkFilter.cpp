/*
 *
 * Copyright (C) 2016 and later: Unicode, Inc. and others. License & terms of use: http://www.unicode.org/copyright.html
 *
 */

#include "LETypes.h"
#include "LEGlyphFilter.h"
#include "GDEFMarkFilter.h"
#include "GlyphDefinitionTables.h"

U_NAMESPACE_BEGIN

GDEFMarkFilter::GDEFMarkFilter(const LEReferenceTo<GlyphDefinitionTableHeader> &gdefTable, LEErrorCode &success)
  : classDefTable(gdefTable->getGlyphClassDefinitionTable(gdefTable, success))
{
  if(!classDefTable.isValid()) {
    success = LE_INTERNAL_ERROR;
  }
}

GDEFMarkFilter::~GDEFMarkFilter()
{
    // nothing to do?
}

le_bool GDEFMarkFilter::accept(LEGlyphID glyph) const
{
    le_int32 glyphClass = classDefTable->getGlyphClass(glyph);

    return glyphClass == gcdMarkGlyph;
}

U_NAMESPACE_END
