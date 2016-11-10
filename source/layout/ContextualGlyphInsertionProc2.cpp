/*
 *
 * Copyright (C) 2016 and later: Unicode, Inc. and others. License & terms of use: http://www.unicode.org/copyright.html
 *
 */

#include "LETypes.h"
#include "MorphTables.h"
#include "StateTables.h"
#include "MorphStateTables.h"
#include "SubtableProcessor2.h"
#include "StateTableProcessor2.h"
#include "ContextualGlyphInsertionProc2.h"
#include "LEGlyphStorage.h"
#include "LESwaps.h"

U_NAMESPACE_BEGIN

UOBJECT_DEFINE_RTTI_IMPLEMENTATION(ContextualGlyphInsertionProcessor2)

ContextualGlyphInsertionProcessor2::ContextualGlyphInsertionProcessor2(
         const LEReferenceTo<MorphSubtableHeader2> &morphSubtableHeader, LEErrorCode &success)
  : StateTableProcessor2(morphSubtableHeader, success)
{
  contextualGlyphHeader = LEReferenceTo<ContextualGlyphInsertionHeader2>(morphSubtableHeader, success);
  if(LE_FAILURE(success) || !contextualGlyphHeader.isValid()) return;
  le_uint32 insertionTableOffset = SWAPL(contextualGlyphHeader->insertionTableOffset);
  insertionTable = LEReferenceToArrayOf<le_uint16>(stHeader, success, insertionTableOffset, LE_UNBOUNDED_ARRAY);
  entryTable = LEReferenceToArrayOf<ContextualGlyphInsertionStateEntry2>(stHeader, success, entryTableOffset, LE_UNBOUNDED_ARRAY);
}

ContextualGlyphInsertionProcessor2::~ContextualGlyphInsertionProcessor2()
{
}

void ContextualGlyphInsertionProcessor2::beginStateTable()
{
    markGlyph = 0;
}

void ContextualGlyphInsertionProcessor2::doInsertion(LEGlyphStorage &glyphStorage,
                                                     le_int16 atGlyph,
                                                     le_int16 &index,
                                                     le_int16 count,
                                                     le_bool /* isKashidaLike */,
                                                     le_bool isBefore,
                                                     LEErrorCode &success) {
  LEGlyphID *insertGlyphs = glyphStorage.insertGlyphs(atGlyph, count + 1, success);
  
  if(LE_FAILURE(success) || insertGlyphs==NULL) {
    return;
  }

  // Note: Kashida vs Split Vowel seems to only affect selection and highlighting.
  // We note the flag, but do not layout different.
  // https://developer.apple.com/fonts/TTRefMan/RM06/Chap6mort.html

  le_int16 targetIndex = 0;
  if(isBefore) {
    // insert at beginning
    insertGlyphs[targetIndex++] = glyphStorage[atGlyph];
  } else {
    // insert at end
    insertGlyphs[count] = glyphStorage[atGlyph];
  }

  while(count--) {
    insertGlyphs[targetIndex++] = insertionTable.getObject(index++, success);
  }
  glyphStorage.applyInsertions();
}

le_uint16 ContextualGlyphInsertionProcessor2::processStateEntry(LEGlyphStorage &glyphStorage, le_int32 &currGlyph,
                                                                EntryTableIndex2 index, LEErrorCode &success)
{
    const ContextualGlyphInsertionStateEntry2 *entry = entryTable.getAlias(index, success);

    if(LE_FAILURE(success)) return 0; // TODO- which state?

    le_uint16 newState = SWAPW(entry->newStateIndex);
    le_uint16 flags = SWAPW(entry->flags);
    
    le_int16 markIndex = SWAPW(entry->markedInsertionListIndex);
    if (markIndex > 0) {
        le_int16 count = (flags & cgiMarkedInsertCountMask) >> 5;
        le_bool isKashidaLike = (flags & cgiMarkedIsKashidaLike);
        le_bool isBefore = (flags & cgiMarkInsertBefore);
        doInsertion(glyphStorage, markGlyph, markIndex, count, isKashidaLike, isBefore, success);
    }

    le_int16 currIndex = SWAPW(entry->currentInsertionListIndex);
    if (currIndex > 0) {
        le_int16 count = flags & cgiCurrentInsertCountMask;
        le_bool isKashidaLike = (flags & cgiCurrentIsKashidaLike);
        le_bool isBefore = (flags & cgiCurrentInsertBefore);
        doInsertion(glyphStorage, currGlyph, currIndex, count, isKashidaLike, isBefore, success);
    }

    if (flags & cgiSetMark) {
        markGlyph = currGlyph;
    }

    if (!(flags & cgiDontAdvance)) {
        currGlyph += dir;
    }

    return newState;
}

void ContextualGlyphInsertionProcessor2::endStateTable()
{
}

U_NAMESPACE_END
