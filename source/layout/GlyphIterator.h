/*
 * %W% %E%
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000, 2001 - All Rights Reserved
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
        const GlyphDefinitionTableHeader *theGlyphDefinitionTableHeader);

    GlyphIterator(GlyphIterator &that);

    GlyphIterator(GlyphIterator &that, le_uint16 newLookupFlags);

    ~GlyphIterator();

    le_bool next(le_uint32 delta = 1);
    le_bool prev(le_uint32 delta = 1);
    le_bool findFeatureTag();

    le_bool isRightToLeft() const;
    le_bool ignoresMarks() const;

    LEGlyphID getCurrGlyphID() const;
    le_int32  getCurrStreamPosition() const;
    void   getCurrGlyphPositionAdjustment(GlyphPositionAdjustment& adjustment) const;

    le_int32  getMarkComponent(le_int32 markPosition) const;

    void setCurrGlyphID(LEGlyphID glyphID);
    void setCurrStreamPosition(le_int32 position);
    void setCurrGlyphPositionAdjustment(const GlyphPositionAdjustment *adjustment);
    void adjustCurrGlyphPositionAdjustment(float xPlacmentAdjust, float yPlacementAdjust,
                                           float xAdvanceAdjust, float yAdvanceAdjust);

private:
    GlyphIterator();
    le_bool filterGlyph(le_uint32 index) const;
    le_bool hasFeatureTag() const;
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
    const GlyphClassDefinitionTable *glyphClassDefinitionTable;
    const MarkAttachClassDefinitionTable *markAttachClassDefinitionTable;
};

#endif
