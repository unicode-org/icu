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

U_NAMESPACE_BEGIN

class GDEFMarkFilter : public UObject, public LEGlyphFilter
{
private:
    const GlyphClassDefinitionTable *classDefTable;

    /**
     * The address of this static class variable serves as this class's ID
     * for ICU "poor man's RTTI".
     */
    static const char fgClassID;

public:
    GDEFMarkFilter(const GlyphDefinitionTableHeader *gdefTable);
    virtual ~GDEFMarkFilter();

    virtual le_bool accept(LEGlyphID glyph) const;

    /**
     * ICU "poor man's RTTI", returns a UClassID for the actual class.
     *
     * @draft ICU 2.2
     */
    virtual inline UClassID getDynamicClassID() const { return getStaticClassID(); }

    /**
     * ICU "poor man's RTTI", returns a UClassID for this class.
     *
     * @draft ICU 2.2
     */
    static inline UClassID getStaticClassID() { return (UClassID)&fgClassID; }
};

U_NAMESPACE_END
#endif
