/*
 * @(#)GDEFMarkFilter.h	1.5 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000, 2001 - All Rights Reserved
 *
 */

#ifndef __GDEFMARKFILTER__H
#define __GDEFMARKFILTER__H

#include "LETypes.h"
#include "LEGlyphFilter.h"
#include "GlyphDefinitionTables.h"

class GDEFMarkFilter : public LEGlyphFilter
{
private:
    const GlyphClassDefinitionTable *classDefTable;

public:
    GDEFMarkFilter(const GlyphDefinitionTableHeader *gdefTable);
    virtual ~GDEFMarkFilter();

    virtual le_bool accept(LEGlyphID glyph) const;
};


#endif
