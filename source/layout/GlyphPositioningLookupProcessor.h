/*
 * @(#)GlyphPositioningLookupProcessor.h	1.7 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
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

class GlyphPositioningLookupProcessor : public LookupProcessor
{
public:
    GlyphPositioningLookupProcessor(GlyphPositioningTableHeader *glyphPositioningTableHeader,
        LETag scriptTag, LETag languageTag);

    virtual ~GlyphPositioningLookupProcessor();

    virtual le_uint32 applySubtable(LookupSubtable *lookupSubtable, le_uint16 lookupType, GlyphIterator *glyphIterator,
        LEFontInstance *fontInstance);

protected:
    GlyphPositioningLookupProcessor();
};

#endif
