/*
 * %W% %E%
 *
 * (C) Copyright IBM Corp. 1998-2003 - All Rights Reserved
 *
 */

#ifndef __GLYPHITERATOR_H
#define __GLYPHITERATOR_H

/**
 * \file
 * \internal
 */

#include "LETypes.h"
#include "OpenTypeTables.h"
#include "GlyphDefinitionTables.h"
#include "GlyphPositionAdjustments.h"

U_NAMESPACE_BEGIN

class GlyphIterator : public UMemory {
public:
    GlyphIterator(LEGlyphID *theGlyphs, GlyphPositionAdjustment *theGlyphPositionAdjustments, le_int32 theGlyphCount,
        le_bool rightToLeft, le_uint16 theLookupFlags, LETag theFeatureTag, const LETag *theGlyphTags[],
        const GlyphDefinitionTableHeader *theGlyphDefinitionTableHeader);

    GlyphIterator(GlyphIterator &that);

    GlyphIterator(GlyphIterator &that, LETag newFeatureTag);

    GlyphIterator(GlyphIterator &that, le_uint16 newLookupFlags);

    ~GlyphIterator();

    le_bool next(le_uint32 delta = 1);
    le_bool prev(le_uint32 delta = 1);
    le_bool findFeatureTag();

    le_bool isRightToLeft() const;
    le_bool ignoresMarks() const;

    le_bool baselineIsLogicalEnd() const;
    le_bool hasCursiveFirstExitPoint() const;
    le_bool hasCursiveLastExitPoint() const;

    LEGlyphID getCurrGlyphID() const;
    le_int32  getCurrStreamPosition() const;
    void   getCurrGlyphPositionAdjustment(GlyphPositionAdjustment &adjustment) const;

    le_int32  getMarkComponent(le_int32 markPosition) const;
    le_bool   findMark2Glyph();

    void getCursiveLastExitPoint(LEPoint &exitPoint) const;
    LEGlyphID getCursiveLastGlyphID() const;
    float getCursiveBaselineAdjustment() const;
    void getCursiveLastPositionAdjustment(GlyphPositionAdjustment &adjustment) const;

    void setCurrGlyphID(TTGlyphID glyphID);
    void setCurrStreamPosition(le_int32 position);
    void setCurrGlyphPositionAdjustment(const GlyphPositionAdjustment *adjustment);
    void setCurrGlyphBaseOffset(le_int32 baseOffset);
    void adjustCurrGlyphPositionAdjustment(float xPlacmentAdjust, float yPlacementAdjust,
                                           float xAdvanceAdjust, float yAdvanceAdjust);

    void setCursiveFirstExitPoint();
    void resetCursiveLastExitPoint();
    void setCursiveLastExitPoint(LEPoint &exitPoint);
    void setCursiveBaselineAdjustment(float adjustment);
    void adjustCursiveLastGlyphPositionAdjustment(float xPlacmentAdjust, float yPlacementAdjust,
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
    le_int32  cursiveFirstPosition;
    le_int32  cursiveLastPosition;
    float     cursiveBaselineAdjustment;
    LEPoint   cursiveLastExitPoint;
    LEGlyphID *glyphs;
    GlyphPositionAdjustment *glyphPositionAdjustments;
    le_uint16 lookupFlags;
    LETag    featureTag;
    const LETag **glyphTags;
    const GlyphClassDefinitionTable *glyphClassDefinitionTable;
    const MarkAttachClassDefinitionTable *markAttachClassDefinitionTable;

    GlyphIterator &operator=(const GlyphIterator &other); // forbid copying of this class
};

U_NAMESPACE_END
#endif
