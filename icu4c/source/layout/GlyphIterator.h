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

struct InsertionRecord
{
	InsertionRecord *next;
	le_int32 position;
	le_int32 count;
	LEGlyphID glyphs[ANY_NUMBER];
};

class InsertionCallback
{
public:
	virtual le_bool applyInsertion(le_int32 atPosition, le_int32 count, LEGlyphID newGlyphs[]) = 0;
};

class InsertionList : public UObject
{
public:
	InsertionList(le_bool rightToLeft);
	~InsertionList();

	LEGlyphID *insert(le_int32 position, le_int32 count);
	le_int32 getGrowAmount();

	le_bool applyInsertions(InsertionCallback *callback);

	void reset();

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

private:

    /**
     * The address of this static class variable serves as this class's ID
     * for ICU "poor man's RTTI".
     */
    static const char fgClassID;

	InsertionRecord *head;
	InsertionRecord *tail;

	le_int32 growAmount;
	le_bool  append;
};

class GlyphIterator : public UMemory, protected InsertionCallback {
public:
    GlyphIterator(LEGlyphID *&theGlyphs, GlyphPositionAdjustment *theGlyphPositionAdjustments, le_int32 *&theCharIndices, le_int32 theGlyphCount,
        le_bool rightToLeft, le_uint16 theLookupFlags, LETag theFeatureTag, const LETag **&theGlyphTags,
        const GlyphDefinitionTableHeader *theGlyphDefinitionTableHeader);

    GlyphIterator(GlyphIterator &that);

    GlyphIterator(GlyphIterator &that, LETag newFeatureTag);

    GlyphIterator(GlyphIterator &that, le_uint16 newLookupFlags);

    virtual ~GlyphIterator();

	void reset(le_uint16 newLookupFlags, LETag newFeatureTag);

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

	LEGlyphID *insertGlyphs(le_int32 count);
	le_int32 applyInsertions();

protected:
	virtual le_bool applyInsertion(le_int32 atPosition, le_int32 count, LEGlyphID newGlyphs[]);

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
	LEGlyphID **glyphsRef;
    LEGlyphID *glyphs;
    GlyphPositionAdjustment *glyphPositionAdjustments;
	le_int32 **charIndicesRef;
	le_int32  *charIndices;
	le_int32  glyphCount;
	InsertionList *insertionList;
	le_bool ownInsertionList;
	le_int32 srcIndex;
	le_int32 destIndex;
    le_uint16 lookupFlags;
    LETag    featureTag;
	const LETag ***glyphTagsRef;
    const LETag **glyphTags;
    const GlyphClassDefinitionTable *glyphClassDefinitionTable;
    const MarkAttachClassDefinitionTable *markAttachClassDefinitionTable;

    GlyphIterator &operator=(const GlyphIterator &other); // forbid copying of this class
};

U_NAMESPACE_END
#endif
