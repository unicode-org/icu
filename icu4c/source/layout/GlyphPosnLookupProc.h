/*
 * @(#)GlyphPosnLookupProc.h	1.7 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000, 2001 - All Rights Reserved
 *
 */

#ifndef __GLYPHPOSITIONINGLOOKUPPROCESSOR_H
#define __GLYPHPOSITIONINGLOOKUPPROCESSOR_H

#include "LETypes.h"
#include "LEFontInstance.h"
#include "OpenTypeTables.h"
#include "Lookups.h"
#include "Features.h"
#include "GlyphDefinitionTables.h"
#include "GlyphPositioningTables.h"
#include "GlyphIterator.h"
#include "LookupProcessor.h"

U_NAMESPACE_BEGIN

class GlyphPositioningLookupProcessor : public LookupProcessor
{
public:
    GlyphPositioningLookupProcessor(const GlyphPositioningTableHeader *glyphPositioningTableHeader,
        LETag scriptTag, LETag languageTag, const LETag *featureOrder);

    virtual ~GlyphPositioningLookupProcessor();

    virtual le_uint32 applySubtable(const LookupSubtable *lookupSubtable, le_uint16 lookupType, GlyphIterator *glyphIterator,
        const LEFontInstance *fontInstance) const;

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

protected:
    GlyphPositioningLookupProcessor();

private:

    /**
     * The address of this static class variable serves as this class's ID
     * for ICU "poor man's RTTI".
     */
    static const char fgClassID;
};

U_NAMESPACE_END
#endif
