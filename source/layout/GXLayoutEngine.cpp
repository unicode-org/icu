
/*
 * @(#)GXLayoutEngine.cpp	1.5 00/03/15
 *
 * (C) Copyright IBM Corp. 1998-2003 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "LayoutEngine.h"
#include "GXLayoutEngine.h"

#include "MorphTables.h"

U_NAMESPACE_BEGIN

const char GXLayoutEngine::fgClassID=0;

GXLayoutEngine::GXLayoutEngine(const LEFontInstance *fontInstance, le_int32 scriptCode, le_int32 languageCode, const MorphTableHeader *morphTable) 
    : LayoutEngine(fontInstance, scriptCode, languageCode), fMorphTable(morphTable)
{
    // nothing else to do?
}

GXLayoutEngine::~GXLayoutEngine()
{
    reset();
}

// apply 'mort' table
le_int32 GXLayoutEngine::computeGlyphs(const LEUnicode chars[], le_int32 offset, le_int32 count, le_int32 max, le_bool rightToLeft, LEGlyphID *&glyphs, le_int32 *&charIndices, LEErrorCode &success)
{
    if (LE_FAILURE(success)) {
        return 0;
    }

    if (chars == NULL || offset < 0 || count < 0 || max < 0 || offset >= max || offset + count > max) {
        success = LE_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    mapCharsToGlyphs(chars, offset, count, false, rightToLeft, glyphs, charIndices, success);

    if (LE_FAILURE(success)) {
        return 0;
    }

    fMorphTable->process(glyphs, charIndices, count);

    return count;
}

// apply positional tables
void GXLayoutEngine::adjustGlyphPositions(const LEUnicode chars[], le_int32 offset, le_int32 count, le_bool /*reverse*/, LEGlyphID glyphs[], le_int32 glyphCount, float positions[], LEErrorCode &success)
{
    if (LE_FAILURE(success)) {
        return;
    }

    if (chars == NULL || glyphs == NULL || positions == NULL || offset < 0 || count < 0 || glyphCount < 0) {
        success = LE_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    // FIXME: no positional processing yet...
}

U_NAMESPACE_END
