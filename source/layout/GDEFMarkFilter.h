/*
 *
 * Copyright (C) 2016 and later: Unicode, Inc. and others. License & terms of use: http://www.unicode.org/copyright.html
 *
 */

#ifndef __GDEFMARKFILTER__H
#define __GDEFMARKFILTER__H

/**
 * \file
 * \internal
 */

#include "LETypes.h"
#include "LEGlyphFilter.h"
#include "GlyphDefinitionTables.h"

U_NAMESPACE_BEGIN

class GDEFMarkFilter : public UMemory, public LEGlyphFilter
{
private:
    const LEReferenceTo<GlyphClassDefinitionTable> classDefTable;

    GDEFMarkFilter(const GDEFMarkFilter &other); // forbid copying of this class
    GDEFMarkFilter &operator=(const GDEFMarkFilter &other); // forbid copying of this class

public:
    GDEFMarkFilter(const LEReferenceTo<GlyphDefinitionTableHeader> &gdefTable, LEErrorCode &success);
    virtual ~GDEFMarkFilter();

    virtual le_bool accept(LEGlyphID glyph) const;
};

U_NAMESPACE_END
#endif
