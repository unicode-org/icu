/*
 * %W% %E%
 *
 * (C) Copyright IBM Corp. 1998-2003 - All Rights Reserved
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

const char InsertionList::fgClassID = 0;

InsertionList::InsertionList(le_bool rightToLeft)
: head(NULL), tail(NULL), growAmount(0), append(rightToLeft)
{
	tail = (InsertionRecord *) &head;
}

InsertionList::~InsertionList()
{
	reset();
}

void InsertionList::reset()
{
	while (head != NULL) {
		InsertionRecord *record = head;

		head = head->next;
		LE_DELETE_ARRAY(record);
	}

	tail = (InsertionRecord *) &head;
	growAmount = 0;
}

le_int32 InsertionList::getGrowAmount()
{
	return growAmount;
}

LEGlyphID *InsertionList::insert(le_int32 position, le_int32 count)
{
	InsertionRecord *insertion = (InsertionRecord *) LE_NEW_ARRAY(char, sizeof(InsertionRecord) + (count - ANY_NUMBER) * sizeof (LEGlyphID));

	insertion->position = position;
	insertion->count = count;

	growAmount += count - 1;

	if (append) {
		// insert on end of list...
		insertion->next = NULL;
		tail->next = insertion;
		tail = insertion;
	} else {
		// insert on front of list...
		insertion->next = head;
		head = insertion;
	}

	return insertion->glyphs;
}

le_bool InsertionList::applyInsertions(InsertionCallback *callback)
{
	for (InsertionRecord *rec = head; rec != NULL; rec = rec->next) {
		if (callback->applyInsertion(rec->position, rec->count, rec->glyphs)) {
			return true;
		}
	}

	return false;
}

GlyphIterator::GlyphIterator(LEGlyphID *&theGlyphs, GlyphPositionAdjustment *theGlyphPositionAdjustments, le_int32 *&theCharIndices, le_int32 theGlyphCount,
    le_bool rightToLeft, le_uint16 theLookupFlags, LETag theFeatureTag, const LETag **&theGlyphTags,
    const GlyphDefinitionTableHeader *theGlyphDefinitionTableHeader)
  : direction(1), position(-1), nextLimit(theGlyphCount), prevLimit(-1),
    cursiveFirstPosition(-1), cursiveLastPosition(-1), cursiveBaselineAdjustment(0),
    glyphsRef(&theGlyphs), glyphs(theGlyphs), glyphPositionAdjustments(theGlyphPositionAdjustments),
	charIndicesRef(&theCharIndices), charIndices(theCharIndices), glyphCount(theGlyphCount), insertionList(NULL), ownInsertionList(true), srcIndex(-1), destIndex(-1),
	lookupFlags(theLookupFlags), featureTag(theFeatureTag), glyphTagsRef(&theGlyphTags), glyphTags(theGlyphTags),
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

	insertionList = new InsertionList(rightToLeft);
}

GlyphIterator::GlyphIterator(GlyphIterator &that)
  : InsertionCallback()
{
    direction    = that.direction;
    position     = that.position;
    nextLimit    = that.nextLimit;
    prevLimit    = that.prevLimit;

    cursiveFirstPosition = that.cursiveFirstPosition;
    cursiveLastPosition  = that.cursiveLastPosition;

    glyphsRef = that.glyphsRef;
    glyphs = that.glyphs;
    glyphPositionAdjustments = that.glyphPositionAdjustments;
    charIndicesRef = that.charIndicesRef;
    charIndices = that.charIndices;
    glyphCount = that.glyphCount;
    insertionList = that.insertionList;
    ownInsertionList = false;
    srcIndex = that.srcIndex;
    destIndex = that.destIndex;
    lookupFlags = that.lookupFlags;
    featureTag = that.featureTag;
    glyphTagsRef = that.glyphTagsRef;
    glyphTags = that.glyphTags;
    glyphClassDefinitionTable = that.glyphClassDefinitionTable;
    markAttachClassDefinitionTable = that.markAttachClassDefinitionTable;
}

GlyphIterator::GlyphIterator(GlyphIterator &that, LETag newFeatureTag)
{
    direction    = that.direction;
    position     = that.position;
    nextLimit    = that.nextLimit;
    prevLimit    = that.prevLimit;

    cursiveFirstPosition = that.cursiveFirstPosition;
    cursiveLastPosition  = that.cursiveLastPosition;

	glyphsRef = that.glyphsRef;
	glyphs = that.glyphs;
    glyphPositionAdjustments = that.glyphPositionAdjustments;
	charIndicesRef = that.charIndicesRef;
	charIndices = that.charIndices;
	glyphCount = that.glyphCount;
	insertionList = that.insertionList;
	ownInsertionList = false;
	srcIndex = that.srcIndex;
	destIndex = that.destIndex;
    lookupFlags = that.lookupFlags;
    featureTag = newFeatureTag;
	glyphTagsRef = that.glyphTagsRef;
    glyphTags = that.glyphTags;
    glyphClassDefinitionTable = that.glyphClassDefinitionTable;
    markAttachClassDefinitionTable = that.markAttachClassDefinitionTable;
}

GlyphIterator::GlyphIterator(GlyphIterator &that, le_uint16 newLookupFlags)
{
    direction    = that.direction;
    position     = that.position;
    nextLimit    = that.nextLimit;
    prevLimit    = that.prevLimit;


    cursiveFirstPosition = that.cursiveFirstPosition;
    cursiveLastPosition  = that.cursiveLastPosition;

	glyphsRef = that.glyphsRef;
    glyphs = that.glyphs;
    glyphPositionAdjustments = that.glyphPositionAdjustments;
	charIndicesRef = that.charIndicesRef;
	charIndices = that.charIndices;
	glyphCount = that.glyphCount;
	insertionList = that.insertionList;
	ownInsertionList = false;
	srcIndex = that.srcIndex;
	destIndex = that.destIndex;
    lookupFlags = newLookupFlags;
    featureTag = that.featureTag;
	glyphTagsRef = that.glyphTagsRef;
    glyphTags = that.glyphTags;
    glyphClassDefinitionTable = that.glyphClassDefinitionTable;
    markAttachClassDefinitionTable = that.markAttachClassDefinitionTable;
}

GlyphIterator::GlyphIterator()
{
};

GlyphIterator::~GlyphIterator()
{
	if (ownInsertionList) {
		delete insertionList;
	}
}

void GlyphIterator::reset(le_uint16 newLookupFlags, LETag newFeatureTag)
{
	position    = prevLimit;
	featureTag  = newFeatureTag;
	lookupFlags = newLookupFlags;
}

LEGlyphID *GlyphIterator::insertGlyphs(le_int32 count)
{
	return insertionList->insert(position, count);
}

le_int32 GlyphIterator::applyInsertions()
{
	le_int32 growAmount = insertionList->getGrowAmount();

	if (growAmount == 0) {
		return glyphCount;
	}

	le_int32 newGlyphCount = glyphCount + growAmount;

	*glyphsRef      = glyphs      = (LEGlyphID *)    LE_GROW_ARRAY(glyphs,      newGlyphCount);
	*glyphTagsRef   = glyphTags   = (const LETag **) LE_GROW_ARRAY(glyphTags,   newGlyphCount);
	*charIndicesRef = charIndices = (le_int32 *)     LE_GROW_ARRAY(charIndices, newGlyphCount);

	srcIndex  = glyphCount - 1;
	destIndex = newGlyphCount - 1;

	// If the current position is at the end of the array
	// update it to point to the end of the new array. The
	// insertion callback will handle all other cases.
	if (position == glyphCount) {
		position = newGlyphCount;
	}

	insertionList->applyInsertions(this);

	insertionList->reset();

	if (direction < 0) {
		prevLimit = newGlyphCount;
	} else {
		nextLimit = newGlyphCount;
	}

	return glyphCount = newGlyphCount;
}

le_bool GlyphIterator::applyInsertion(le_int32 atPosition, le_int32 count, LEGlyphID newGlyphs[])
{
	// if the current position is within the block we're shifting
	// it needs to be updated to the current glyph's
	// new location.
	if (position >= atPosition && position <= srcIndex) {
		position += destIndex - srcIndex;
	}

	while (srcIndex > atPosition) {
		glyphs[destIndex]      = glyphs[srcIndex];
		glyphTags[destIndex]   = glyphTags[srcIndex];
		charIndices[destIndex] = charIndices[srcIndex];

		destIndex -= 1;
		srcIndex  -= 1;
	}

	for (le_int32 i = count - 1; i >= 0; i -= 1) {
		glyphs[destIndex]      = newGlyphs[i];
		glyphTags[destIndex]   = glyphTags[atPosition];
		charIndices[destIndex] = charIndices[atPosition];

		destIndex -= 1;
	}

	// the source glyph we're pointing at
	// just got replaced by the insertion
	srcIndex -= 1;

	return false;
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

    return glyphs[position];
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

    return glyphs[cursiveLastPosition];
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
    glyphs[position] = LE_SET_GLYPH(glyphs[position], glyphID);
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
    LEGlyphID glyphID = glyphs[index];
    le_int32 glyphClass = gcdNoGlyphClass;

    if (LE_GET_GLYPH(glyphID) >= 0xFFFE) {
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
    le_int32 posn;

    for (posn = position; posn != markPosition; posn += direction) {
        if (glyphs[posn] == 0xFFFE) {
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
    } while (newPosition != prevLimit && glyphs[newPosition] != 0xFFFE && filterGlyph(newPosition));

    position = newPosition;

    return position != prevLimit;
}

U_NAMESPACE_END
