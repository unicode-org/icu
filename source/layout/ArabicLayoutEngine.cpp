
/*
 *
 * (C) Copyright IBM Corp. 1998-2004 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "LEScripts.h"
#include "LEGlyphFilter.h"
#include "LEGlyphStorage.h"
#include "LayoutEngine.h"
#include "OpenTypeLayoutEngine.h"
#include "ArabicLayoutEngine.h"
#include "ScriptAndLanguageTags.h"
#include "CharSubstitutionFilter.h"

#include "GlyphSubstitutionTables.h"
#include "GlyphDefinitionTables.h"
#include "GlyphPositioningTables.h"

#include "GDEFMarkFilter.h"

#include "ArabicShaping.h"
#include "HebrewShaping.h"

U_NAMESPACE_BEGIN

le_bool CharSubstitutionFilter::accept(LEGlyphID glyph) const
{
    return fFontInstance->canDisplay((LEUnicode) glyph);
}

const char ArabicOpenTypeLayoutEngine::fgClassID=0;

ArabicOpenTypeLayoutEngine::ArabicOpenTypeLayoutEngine(const LEFontInstance *fontInstance, le_int32 scriptCode, le_int32 languageCode,
                        const GlyphSubstitutionTableHeader *gsubTable)
    : OpenTypeLayoutEngine(fontInstance, scriptCode, languageCode, gsubTable)
{
    /**/ fFeatureOrder = ArabicShaping::getFeatureOrder();
}

ArabicOpenTypeLayoutEngine::ArabicOpenTypeLayoutEngine(const LEFontInstance *fontInstance, le_int32 scriptCode, le_int32 languageCode)
    : OpenTypeLayoutEngine(fontInstance, scriptCode, languageCode)
{
    // fFeatureOrder = ArabicShaping::getFeatureOrder();
}

ArabicOpenTypeLayoutEngine::~ArabicOpenTypeLayoutEngine()
{
    // nothing to do
}

// Input: characters
// Output: characters, char indices, tags
// Returns: output character count
le_int32 ArabicOpenTypeLayoutEngine::characterProcessing(const LEUnicode chars[], le_int32 offset, le_int32 count, le_int32 max, le_bool rightToLeft,
        LEUnicode *&/*outChars*/, LEGlyphStorage &glyphStorage, LEErrorCode &success)
{
    if (LE_FAILURE(success)) {
        return 0;
    }

    if (chars == NULL || offset < 0 || count < 0 || max < 0 || offset >= max || offset + count > max) {
        success = LE_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    glyphStorage.adoptGlyphCount(count);
    glyphStorage.allocateAuxData(success);

    if (LE_FAILURE(success)) {
        success = LE_MEMORY_ALLOCATION_ERROR;
        return 0;
    }

    switch (fScriptCode) {
    case arabScriptCode:
    {
        ArabicShaping::shape(chars, offset, count, max, rightToLeft, glyphStorage);
        break;
    }

    case hebrScriptCode:
        HebrewShaping::shape(chars, offset, count, max, rightToLeft, glyphStorage);
        break;
    }

    return count;
}

void ArabicOpenTypeLayoutEngine::adjustGlyphPositions(const LEUnicode chars[], le_int32 offset, le_int32 count, le_bool reverse,
                                                      LEGlyphStorage &glyphStorage, LEErrorCode &success)
{
    if (LE_FAILURE(success)) {
        return;
    }

    if (chars == NULL || offset < 0 || count < 0) {
        success = LE_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    if (fGPOSTable != NULL) {
        OpenTypeLayoutEngine::adjustGlyphPositions(chars, offset, count, reverse, glyphStorage, success);
    } else if (fGDEFTable != NULL) {
        GDEFMarkFilter filter(fGDEFTable);

        adjustMarkGlyphs(glyphStorage, &filter, success);
    } else {
        GlyphDefinitionTableHeader *gdefTable = (GlyphDefinitionTableHeader *) ArabicShaping::glyphDefinitionTable;
        GDEFMarkFilter filter(gdefTable);

        adjustMarkGlyphs(&chars[offset], count, reverse, glyphStorage, &filter, success);
    }
}

UnicodeArabicOpenTypeLayoutEngine::UnicodeArabicOpenTypeLayoutEngine(const LEFontInstance *fontInstance, le_int32 scriptCode, le_int32 languageCode)
    : ArabicOpenTypeLayoutEngine(fontInstance, scriptCode, languageCode)
{
    switch (scriptCode) {
    case arabScriptCode:
        fGSUBTable = (const GlyphSubstitutionTableHeader *) ArabicShaping::glyphSubstitutionTable;
        fGDEFTable = (const GlyphDefinitionTableHeader *) ArabicShaping::glyphDefinitionTable;
        break;

    case hebrScriptCode:
        fGSUBTable = (const GlyphSubstitutionTableHeader *) HebrewShaping::glyphSubstitutionTable;
        fGDEFTable = (const GlyphDefinitionTableHeader *) HebrewShaping::glyphDefinitionTable;
        break;
    }


    fSubstitutionFilter = new CharSubstitutionFilter(fontInstance);
}

UnicodeArabicOpenTypeLayoutEngine::~UnicodeArabicOpenTypeLayoutEngine()
{
    delete fSubstitutionFilter;
}

// "glyphs", "indices" -> glyphs, indices
le_int32 UnicodeArabicOpenTypeLayoutEngine::glyphPostProcessing(LEGlyphStorage &tempGlyphStorage, LEGlyphStorage &glyphStorage, LEErrorCode &success)
{
    if (LE_FAILURE(success)) {
        return 0;
    }

    // FIXME: we could avoid the memory allocation and copy if we
    // made a clone of mapCharsToGlyphs which took the fake glyphs
    // directly.
    le_int32 tempGlyphCount = tempGlyphStorage.getGlyphCount();
    LEUnicode *tempChars = LE_NEW_ARRAY(LEUnicode, tempGlyphCount);

    if (tempChars == NULL) {
        success = LE_MEMORY_ALLOCATION_ERROR;
        return 0;
    }

    for (le_int32 i = 0; i < tempGlyphCount; i += 1) {
        tempChars[i] = (LEUnicode) LE_GET_GLYPH(tempGlyphStorage[i]);
    }

    glyphStorage.adoptCharIndicesArray(tempGlyphStorage);

    ArabicOpenTypeLayoutEngine::mapCharsToGlyphs(tempChars, 0, tempGlyphCount, FALSE, TRUE, glyphStorage, success);

    LE_DELETE_ARRAY(tempChars);

    return tempGlyphCount;
}

void UnicodeArabicOpenTypeLayoutEngine::mapCharsToGlyphs(const LEUnicode chars[], le_int32 offset, le_int32 count, le_bool reverse, le_bool /*mirror*/, LEGlyphStorage &glyphStorage, LEErrorCode &success)
{
    if (LE_FAILURE(success)) {
        return;
    }

    if (chars == NULL || offset < 0 || count < 0) {
        success = LE_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    le_int32 i, dir = 1, out = 0;

    if (reverse) {
        out = count - 1;
        dir = -1;
    }

    glyphStorage.allocateGlyphArray(count, reverse, success);

    for (i = 0; i < count; i += 1, out += dir) {
        glyphStorage[out] = (LEGlyphID) chars[offset + i];
    }
}

void UnicodeArabicOpenTypeLayoutEngine::adjustGlyphPositions(const LEUnicode chars[], le_int32 offset, le_int32 count, le_bool reverse,
                                                      LEGlyphStorage &glyphStorage, LEErrorCode &success)
{
    if (LE_FAILURE(success)) {
        return;
    }

    if (chars == NULL || offset < 0 || count < 0) {
        success = LE_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    GDEFMarkFilter filter(fGDEFTable);

    adjustMarkGlyphs(&chars[offset], count, reverse, glyphStorage, &filter, success);
}

U_NAMESPACE_END

