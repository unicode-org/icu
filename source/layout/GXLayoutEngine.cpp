
/*
 * @(#)GXLayoutEngine.cpp	1.5 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "LayoutEngine.h"
#include "GXLayoutEngine.h"

#include "MorphTables.h"

GXLayoutEngine::GXLayoutEngine(LEFontInstance *fontInstance, le_int32 scriptCode, le_int32 languageCode, MorphTableHeader *morphTable) 
    : LayoutEngine(fontInstance, scriptCode, languageCode), fMorphTable(morphTable)
{
    // nothing else to do?
}

GXLayoutEngine::~GXLayoutEngine()
{
    reset();
}

// apply 'mort' table
le_int32 GXLayoutEngine::computeGlyphs(const LEUnicode chars[], le_int32 offset, le_int32 count, le_int32 max, le_bool rightToLeft, LEGlyphID *&glyphs, le_int32 *&charIndices)
{
    mapCharsToGlyphs(chars, offset, count, false, rightToLeft, glyphs, charIndices);
    fMorphTable->process(glyphs, charIndices, count);

    return count;
}

// apply positional tables
void GXLayoutEngine::adjustGlyphPositions(const LEUnicode chars[], le_int32 offset, le_int32 count, le_bool reverse, LEGlyphID glyphs[], le_int32 glyphCount, float positions[])
{
    // FIXME: no positional processing yet...
}

