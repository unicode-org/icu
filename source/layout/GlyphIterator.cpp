/*
 *
 * (C) Copyright IBM Corp. 1998-2004 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "OpenTypeTables.h"
#include "GlyphDefinitionTables.h"
#include "GlyphPositionAdjustments.h"
#include "GlyphIterator.h"
#include "LEGlyphStorage.h"
#include "Lookups.h"
#include "LESwaps.h"

U_NAMESPACE_BEGIN

GlyphIterator::GlyphIterator(LEGlyphStorage &theGlyphStorage, GlyphPositionAdjustment *theGlyphPositionAdjustments, le_bool rightToLeft, le_uint16 theLookupFlags, LETag theFeatureTag,
    const GlyphDefinitionTableHeader *theGlyphDefinitionTableHeader)
  : direction(1), position(-1), nextLimit(-1), prevLimit(-1),
    cursiveFirstPosition(-1), cursiveLastPosition(-1), cursiveBaselineAdjustment(0),
    glyphStorage(theGlyphStorage), glyphPositionAdjustments(theGlyphPositionAdjustments),
    srcIndex(-1), destIndex(-1), lookupFlags(theLookupFlags), featureTag(theFeatureTag),
    glyphClassDefinitionTable(NULL), markAttachClassDefinitionTable(NULL)

{
    le_int32 glyphCount = glyphStorage.getGlyphCount();

    if (theGlyphDefinitionTableHeader != NULL) {
        glyphClassDefinitionTable = theGlyphDefinitionTableHeader->getGlyphClassDefinitionTable();
        markAttachClassDefinitionTable = theGlyphDefinitionTableHeader->getMarkAttachClassDefinitionTable();
    }

    nextLimit = glyphCount;

    if (rightToLeft) {
        direction = -1;
        position = glyphCount;
        nextLimit = -1;
        prevLimit = glyphCount;
    }
}

GlyphIterator::GlyphIterator(GlyphIterator &that)
  : glyphStorage(that.glyphStorage)
{
    direction    = that.direction;
    position     = that.position;
    nextLimit    = that.nextLimit;
    prevLimit    = that.prevLimit;

    cursiveFirstPosition = that.cursiveFirstPosition;
    cursiveLastPosition  = that.cursiveLastPosition;

    glyphPositionAdjustments = that.glyphPositionAdjustments;
    srcIndex = that.srcIndex;
    destIndex = that.destIndex;
    lookupFlags = that.lookupFlags;
    featureTag = that.featureTag;
    glyphClassDefinitionTable = that.glyphClassDefinitionTable;
    markAttachClassDefinitionTable = that.markAttachClassDefinitionTable;
}

GlyphIterator::GlyphIterator(GlyphIterator &that, LETag newFeatureTag)
  : glyphStorage(that.glyphStorage)
{
    direction    = that.direction;
    position     = that.position;
    nextLimit    = that.nextLimit;
    prevLimit    = that.prevLimit;

    cursiveFirstPosition = that.cursiveFirstPosition;
    cursiveLastPosition  = that.cursiveLastPosition;

    glyphPositionAdjustments = that.glyphPositionAdjustments;
    srcIndex = that.srcIndex;
    destIndex = that.destIndex;
    lookupFlags = that.lookupFlags;
    featureTag = newFeatureTag;
    glyphClassDefinitionTable = that.glyphClassDefinitionTable;
    markAttachClassDefinitionTable = that.markAttachClassDefinitionTable;
}

GlyphIterator::GlyphIterator(GlyphIterator &that, le_uint16 newLookupFlags)
  : glyphStorage(that.glyphStorage)
{
    direction    = that.direction;
    position     = that.position;
    nextLimit    = that.nextLimit;
    prevLimit    = that.prevLimit;


    cursiveFirstPosition = that.cursiveFirstPosition;
    cursiveLastPosition  = that.cursiveLastPosition;

    glyphPositionAdjustments = that.glyphPositionAdjustments;
    srcIndex = that.srcIndex;
    destIndex = that.destIndex;
    lookupFlags = newLookupFlags;
    featureTag = that.featureTag;
    glyphClassDefinitionTable = that.glyphClassDefinitionTable;
    markAttachClassDefinitionTable = that.markAttachClassDefinitionTable;
}

GlyphIterator::~GlyphIterator()
{
    // nothing to do, right?
}

void GlyphIterator::reset(le_uint16 newLookupFlags, LETag newFeatureTag)
{
    position    = prevLimit;
    featureTag  = newFeatureTag;
    lookupFlags = newLookupFlags;
}

LEGlyphID *GlyphIterator::insertGlyphs(le_int32 count)
{
    return glyphStorage.insertGlyphs(position, count);
}

le_int32 GlyphIterator::applyInsertions()
{
    le_int32 newGlyphCount = glyphStorage.applyInsertions();

    if (direction < 0) {
        prevLimit = newGlyphCount;
    } else {
        nextLimit = newGlyphCount;
    }

    return newGlyphCount;
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

le_bool GlyphIterator::baselineIsLogicalEnd() const
{
    return (lookupFlags & lfBaselineIsLogicalEnd) != 0;
}

le_bool GlyphIterator::hasCursiveFirstExitPoint() const
{
    return cursiveFirstPosition >= 0;
}

le_bool GlyphIterator::hasCursiveLastExitPoint() const
{
    return cursiveLastPosition >= 0;
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

    return glyphStorage[position];
}

LEGlyphID GlyphIterator::getCursiveLastGlyphID() const
{
    if (direction < 0) {
        if (cursiveLastPosition <= nextLimit || cursiveLastPosition >= prevLimit) {
            return 0xFFFF;
        }
    } else {
        if (cursiveLastPosition <= prevLimit || cursiveLastPosition >= nextLimit) {
            return 0xFFFF;
        }
    }

    return glyphStorage[cursiveLastPosition];
}

void GlyphIterator::getCursiveLastExitPoint(LEPoint &exitPoint) const
{
    if (cursiveLastPosition >= 0) {
        exitPoint = cursiveLastExitPoint;
    }
}

float GlyphIterator::getCursiveBaselineAdjustment() const
{
    return cursiveBaselineAdjustment;
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

void GlyphIterator::getCursiveLastPositionAdjustment(GlyphPositionAdjustment &adjustment) const
{
    if (direction < 0)
    {
        if (cursiveLastPosition <= nextLimit || cursiveLastPosition >= prevLimit)
        {
            return;
        }
    } else {
        if (cursiveLastPosition <= prevLimit || cursiveLastPosition >= nextLimit) {
            return;
        }
    }

    adjustment = glyphPositionAdjustments[cursiveLastPosition];
}

void GlyphIterator::setCurrGlyphID(TTGlyphID glyphID)
{
    LEGlyphID glyph = glyphStorage[position];

    glyphStorage[position] = LE_SET_GLYPH(glyph, glyphID);
}

void GlyphIterator::setCurrStreamPosition(le_int32 newPosition)
{
    cursiveFirstPosition      = -1;
    cursiveLastPosition       = -1;
    cursiveBaselineAdjustment =  0;

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

void GlyphIterator::setCurrGlyphBaseOffset(le_int32 baseOffset)
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

    glyphPositionAdjustments[position].setBaseOffset(baseOffset);
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

void GlyphIterator::setCursiveFirstExitPoint()
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

    cursiveFirstPosition = position;
}

void GlyphIterator::resetCursiveLastExitPoint()
{
    if ((lookupFlags & lfBaselineIsLogicalEnd) != 0 && cursiveFirstPosition >= 0 && cursiveLastPosition >= 0) {
        le_int32 savePosition = position, saveLimit = nextLimit;

        position  = cursiveFirstPosition - direction;
        nextLimit = cursiveLastPosition  + direction;

        while (nextInternal()) {
            glyphPositionAdjustments[position].adjustYPlacement(-cursiveBaselineAdjustment);
        }

        position  = savePosition;
        nextLimit = saveLimit;
    }

    cursiveLastPosition       = -1;
    cursiveFirstPosition      = -1;
    cursiveBaselineAdjustment =  0;
}

void GlyphIterator::setCursiveLastExitPoint(LEPoint &exitPoint)
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

    cursiveLastPosition  = position;
    cursiveLastExitPoint = exitPoint;

}

void GlyphIterator::setCursiveBaselineAdjustment(float adjustment)
{
    cursiveBaselineAdjustment = adjustment;
}

void GlyphIterator::adjustCursiveLastGlyphPositionAdjustment(float xPlacementAdjust, float yPlacementAdjust,
                                              float xAdvanceAdjust, float yAdvanceAdjust)
{
    if (direction < 0) {
        if (cursiveLastPosition <= nextLimit || cursiveLastPosition >= prevLimit) {
            return;
        }
    } else {
        if (cursiveLastPosition <= prevLimit || cursiveLastPosition >= nextLimit) {
            return;
        }
    }

    glyphPositionAdjustments[cursiveLastPosition].adjustXPlacement(xPlacementAdjust);
    glyphPositionAdjustments[cursiveLastPosition].adjustYPlacement(yPlacementAdjust);
    glyphPositionAdjustments[cursiveLastPosition].adjustXAdvance(xAdvanceAdjust);
    glyphPositionAdjustments[cursiveLastPosition].adjustYAdvance(yAdvanceAdjust);
}

le_bool GlyphIterator::filterGlyph(le_uint32 index) const
{
    LEGlyphID glyphID = glyphStorage[index];
    le_int32 glyphClass = gcdNoGlyphClass;

    if (LE_GET_GLYPH(glyphID) >= 0xFFFE) {
        return TRUE;
    }

    if (glyphClassDefinitionTable != NULL) {
        glyphClass = glyphClassDefinitionTable->getGlyphClass(glyphID);
    }

    switch (glyphClass)
    {
    case gcdNoGlyphClass:
        return FALSE;

    case gcdSimpleGlyph:
        return (lookupFlags & lfIgnoreBaseGlyphs) != 0;

    case gcdLigatureGlyph:
        return (lookupFlags & lfIgnoreLigatures) != 0;

    case gcdMarkGlyph:
    {
        if ((lookupFlags & lfIgnoreMarks) != 0) {
            return TRUE;
        }

        le_uint16 markAttachType = (lookupFlags & lfMarkAttachTypeMask) >> lfMarkAttachTypeShift;

        if ((markAttachType != 0) && (markAttachClassDefinitionTable != NULL)) {
            return markAttachClassDefinitionTable->getGlyphClass(glyphID) != markAttachType;
        }

        return FALSE;
    }

    case gcdComponentGlyph:
        return (lookupFlags & lfIgnoreBaseGlyphs) != 0;

    default:
        return FALSE;
    }
}

static const LETag emptyTag = 0;
static const LETag defaultTag = 0xFFFFFFFF;

le_bool GlyphIterator::hasFeatureTag() const
{
    if (featureTag == defaultTag || featureTag == emptyTag) {
        return TRUE;
    }

    LEErrorCode success = LE_NO_ERROR;
    const LETag *tagList = (const LETag *) glyphStorage.getAuxData(position, success);

    if (tagList != NULL) {
        for (le_int32 tag = 0; tagList[tag] != emptyTag; tag += 1) {
            if (tagList[tag] == featureTag) {
                return TRUE;
            }
        }
    }

    return FALSE;
}

le_bool GlyphIterator::findFeatureTag()
{
    while (nextInternal()) {
        if (hasFeatureTag()) {
            prevInternal();
            return TRUE;
        }
    }

    return FALSE;
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
    le_int32 posn;

    for (posn = position; posn != markPosition; posn += direction) {
        if (glyphStorage[posn] == 0xFFFE) {
            component += 1;
        }
    }

    return component;
}

// This is basically prevInternal except that it
// doesn't take a delta argument, and it doesn't
// filter out 0xFFFE glyphs.
le_bool GlyphIterator::findMark2Glyph()
{
    le_int32 newPosition = position;

    do {
        newPosition -= direction;
    } while (newPosition != prevLimit && glyphStorage[newPosition] != 0xFFFE && filterGlyph(newPosition));

    position = newPosition;

    return position != prevLimit;
}

U_NAMESPACE_END
