/*
 * %W% %E%
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000, 2001 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "OpenTypeTables.h"
#include "GlyphDefinitionTables.h"
#include "GlyphPositionAdjustments.h"
#include "GlyphIterator.h"
#include "Lookups.h"
#include "LESwaps.h"

U_NAMESPACE_BEGIN

GlyphIterator::GlyphIterator(LEGlyphID *theGlyphs, GlyphPositionAdjustment *theGlyphPositionAdjustments, le_int32 theGlyphCount,
    le_bool rightToLeft, le_uint16 theLookupFlags, LETag theFeatureTag, const LETag *theGlyphTags[],
    const GlyphDefinitionTableHeader *theGlyphDefinitionTableHeader)
  : direction(1), position(-1), nextLimit(theGlyphCount), prevLimit(-1),
    glyphs(theGlyphs), glyphPositionAdjustments(theGlyphPositionAdjustments), lookupFlags(theLookupFlags),
    featureTag(theFeatureTag), glyphTags(theGlyphTags),
    glyphClassDefinitionTable(NULL),
    markAttachClassDefinitionTable(NULL)

{
    if (theGlyphDefinitionTableHeader != NULL) {
        glyphClassDefinitionTable = theGlyphDefinitionTableHeader->getGlyphClassDefinitionTable();
        markAttachClassDefinitionTable = theGlyphDefinitionTableHeader->getMarkAttachClassDefinitionTable();
    }

    if (rightToLeft) {
        direction = -1;
        position = theGlyphCount;
        nextLimit = -1;
        prevLimit = theGlyphCount;
    }
}

GlyphIterator::GlyphIterator(GlyphIterator &that)
{
    direction = that.direction;
    position = that.position;
    nextLimit = that.nextLimit;
    prevLimit = that.prevLimit;

    glyphs = that.glyphs;
    glyphPositionAdjustments = that.glyphPositionAdjustments;
    lookupFlags = that.lookupFlags;
    featureTag = that.featureTag;
    glyphTags = that.glyphTags;
    glyphClassDefinitionTable = that.glyphClassDefinitionTable;
    markAttachClassDefinitionTable = that.markAttachClassDefinitionTable;
}

GlyphIterator::GlyphIterator(GlyphIterator &that, le_uint16 newLookupFlags)
{
    direction = that.direction;
    position = that.position;
    nextLimit = that.nextLimit;
    prevLimit = that.prevLimit;

    glyphs = that.glyphs;
    glyphPositionAdjustments = that.glyphPositionAdjustments;
    lookupFlags = newLookupFlags;
    featureTag = that.featureTag;
    glyphTags = that.glyphTags;
    glyphClassDefinitionTable = that.glyphClassDefinitionTable;
    markAttachClassDefinitionTable = that.markAttachClassDefinitionTable;
}

GlyphIterator::GlyphIterator()
{
};

GlyphIterator::~GlyphIterator()
{
}

le_int32 GlyphIterator::getCurrStreamPosition() const
{
    return position;
}

le_bool GlyphIterator::isRightToLeft() const
{
    return direction < 0;
}

le_bool GlyphIterator::ignoresMarks() const
{
    return (lookupFlags & lfIgnoreMarks) != 0;
}

LEGlyphID GlyphIterator::getCurrGlyphID() const
{
    if (direction < 0) {
        if (position <= nextLimit || position >= prevLimit) {
            return 0xFFFF;
        }
    } else {
        if (position <= prevLimit || position >= nextLimit) {
            return 0xFFFF;
        }
    }

    return glyphs[position];
}

void GlyphIterator::getCurrGlyphPositionAdjustment(GlyphPositionAdjustment &adjustment) const
{
    if (direction < 0)
    {
        if (position <= nextLimit || position >= prevLimit)
        {
            return;
        }
    } else {
        if (position <= prevLimit || position >= nextLimit) {
            return;
        }
    }

    adjustment = glyphPositionAdjustments[position];
}

void GlyphIterator::setCurrGlyphID(LEGlyphID glyphID)
{
    glyphs[position] = glyphID;
}

void GlyphIterator::setCurrStreamPosition(le_int32 newPosition)
{
    if (direction < 0) {
        if (newPosition >= prevLimit) {
            position = prevLimit;
            return;
        }

        if (newPosition <= nextLimit) {
            position = nextLimit;
            return;
        }
    } else {
        if (newPosition <= prevLimit) {
            position = prevLimit;
            return;
        }

        if (newPosition >= nextLimit) {
            position = nextLimit;
            return;
        }
    }

    position = newPosition - direction;
    next();
}

void GlyphIterator::setCurrGlyphPositionAdjustment(const GlyphPositionAdjustment *adjustment)
{
    if (direction < 0) {
        if (position <= nextLimit || position >= prevLimit) {
            return;
        }
    } else {
        if (position <= prevLimit || position >= nextLimit) {
            return;
        }
    }

    glyphPositionAdjustments[position] = *adjustment;
}

void GlyphIterator::adjustCurrGlyphPositionAdjustment(float xPlacementAdjust, float yPlacementAdjust,
                                                      float xAdvanceAdjust, float yAdvanceAdjust)
{
    if (direction < 0) {
        if (position <= nextLimit || position >= prevLimit) {
            return;
        }
    } else {
        if (position <= prevLimit || position >= nextLimit) {
            return;
        }
    }

    glyphPositionAdjustments[position].adjustXPlacement(xPlacementAdjust);
    glyphPositionAdjustments[position].adjustYPlacement(yPlacementAdjust);
    glyphPositionAdjustments[position].adjustXAdvance(xAdvanceAdjust);
    glyphPositionAdjustments[position].adjustYAdvance(yAdvanceAdjust);
}

le_bool GlyphIterator::filterGlyph(le_uint32 index) const
{
    LEGlyphID glyphID = (LEGlyphID) glyphs[index];
    le_int32 glyphClass = gcdNoGlyphClass;

    // FIXME: is this test really safe?
    if (glyphID >= 0xFFFE) {
        return true;
    }

    if (glyphClassDefinitionTable != NULL) {
        glyphClass = glyphClassDefinitionTable->getGlyphClass(glyphID);
    }

    switch (glyphClass)
    {
    case gcdNoGlyphClass:
        return false;

    case gcdSimpleGlyph:
        return (lookupFlags & lfIgnoreBaseGlyphs) != 0;

    case gcdLigatureGlyph:
        return (lookupFlags & lfIgnoreLigatures) != 0;

    case gcdMarkGlyph:
    {
        if ((lookupFlags & lfIgnoreMarks) != 0) {
            return true;
        }

        le_uint16 markAttachType = (lookupFlags & lfMarkAttachTypeMask) >> lfMarkAttachTypeShift;

        if ((markAttachType != 0) && (markAttachClassDefinitionTable != NULL)) {
            return markAttachClassDefinitionTable->getGlyphClass(glyphID) != markAttachType;
        }

        return false;
    }

    case gcdComponentGlyph:
        return (lookupFlags & lfIgnoreBaseGlyphs) != 0;

    default:
        return false;
    }
}

const LETag emptyTag = 0;
const LETag defaultTag = 0xFFFFFFFF;

le_bool GlyphIterator::hasFeatureTag() const
{
    if (featureTag == defaultTag || featureTag == emptyTag) {
        return true;
    }

	if (glyphTags != NULL) {
		const LETag *tagList = glyphTags[position];

		for (le_int32 tag = 0; tagList[tag] != emptyTag; tag += 1) {
			if (tagList[tag] == featureTag) {
				return true;
			}
		}
	}

    return false;
}

le_bool GlyphIterator::findFeatureTag()
{
    while (nextInternal()) {
        if (hasFeatureTag()) {
            prevInternal();
            return true;
        }
    }

    return false;
}


le_bool GlyphIterator::nextInternal(le_uint32 delta)
{
    le_int32 newPosition = position;

    while (newPosition != nextLimit && delta > 0) {
        do {
            newPosition += direction;
        } while (newPosition != nextLimit && filterGlyph(newPosition));

        delta -= 1;
    }

    position = newPosition;

    return position != nextLimit;
}

le_bool GlyphIterator::next(le_uint32 delta)
{
    return nextInternal(delta) && hasFeatureTag();
}

le_bool GlyphIterator::prevInternal(le_uint32 delta)
{
    le_int32 newPosition = position;

    while (newPosition != prevLimit && delta > 0) {
        do {
            newPosition -= direction;
        } while (newPosition != prevLimit && filterGlyph(newPosition));

        delta -= 1;
    }

    position = newPosition;

    return position != prevLimit;
}

le_bool GlyphIterator::prev(le_uint32 delta)
{
    return prevInternal(delta) && hasFeatureTag();
}

le_int32 GlyphIterator::getMarkComponent(le_int32 markPosition) const
{
    le_int32 component = 0;
    le_int32 posn, start = position, end = markPosition;

    if (markPosition < position) {
        start = markPosition;
        end = position;
        }

    for (posn = start; posn <= end; posn += 1) {
        if (glyphs[posn] == 0xFFFE) {
            component += 1;
		}
    }

    return component;
}

U_NAMESPACE_END
