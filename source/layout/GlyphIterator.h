/*
 * @(#)GlyphIterator.h	1.9 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#ifndef __GLYPHITERATOR_H
#define __GLYPHITERATOR_H

#include "LETypes.h"
#include "OpenTypeTables.h"
#include "GlyphDefinitionTables.h"
#include "GlyphPositionAdjustments.h"

class GlyphIterator
{
public:
    GlyphIterator(LEGlyphID *theGlyphs, GlyphPositionAdjustment *theGlyphPositionAdjustments, le_int32 theGlyphCount,
        le_bool rightToLeft, le_uint16 theLookupFlags, LETag theFeatureTag, const LETag *theGlyphTags[],
        GlyphDefinitionTableHeader *theGlyphDefinitionTableHeader);

    GlyphIterator(GlyphIterator &that);

    GlyphIterator(GlyphIterator &that, le_uint16 newLookupFlags);

    ~GlyphIterator();

    le_bool next(le_uint32 delta = 1);
    le_bool prev(le_uint32 delta = 1);
    le_bool findFeatureTag();

    le_bool isRightToLeft();

    LEGlyphID getCurrGlyphID();
    le_int32  getCurrStreamPosition();
    void   getCurrGlyphPositionAdjustment(GlyphPositionAdjustment& adjustment);

    le_int32  getMarkComponent(le_int32 markPosition);

    void setCurrGlyphID(LEGlyphID glyphID);
    void setCurrStreamPosition(le_int32 position);
    void setCurrGlyphPositionAdjustment(const GlyphPositionAdjustment *adjustment);
    void adjustCurrGlyphPositionAdjustment(float xPlacmentAdjust, float yPlacementAdjust,
                                           float xAdvanceAdjust, float yAdvanceAdjust);

private:
    GlyphIterator();
    le_bool filterGlyph(le_uint32 index);
    le_bool hasFeatureTag();
    le_bool nextInternal(le_uint32 delta = 1);
    le_bool prevInternal(le_uint32 delta = 1);

    le_int32  direction;
    le_int32  position;
    le_int32  nextLimit;
    le_int32  prevLimit;
    LEGlyphID *glyphs;
    GlyphPositionAdjustment *glyphPositionAdjustments;
    le_uint16 lookupFlags;
    LETag    featureTag;
    const LETag **glyphTags;
    GlyphClassDefinitionTable *glyphClassDefinitionTable;
    MarkAttachClassDefinitionTable *markAttachClassDefinitionTable;
};

#endif
