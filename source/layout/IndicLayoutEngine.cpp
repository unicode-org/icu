
/*
 * @(#)IndicLayoutEngine.cpp	1.3 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "LayoutEngine.h"
#include "OpenTypeLayoutEngine.h"
#include "IndicLayoutEngine.h"
#include "ScriptAndLanguageTags.h"

#include "GlyphSubstitutionTables.h"
#include "GlyphDefinitionTables.h"
#include "GlyphPositioningTables.h"

#include "GDEFMarkFilter.h"

#include "CDACLayout.h"
#include "IndicReordering.h"

IndicOpenTypeLayoutEngine::IndicOpenTypeLayoutEngine(LEFontInstance *fontInstance, le_int32 scriptCode, le_int32 languageCode,
                    GlyphSubstitutionTableHeader *gsubTable)
    : OpenTypeLayoutEngine(fontInstance, scriptCode, languageCode, gsubTable)
{
    // nothing else to do...
}

IndicOpenTypeLayoutEngine::IndicOpenTypeLayoutEngine(LEFontInstance *fontInstance, le_int32 scriptCode, le_int32 languageCode)
    : OpenTypeLayoutEngine(fontInstance, scriptCode, languageCode)
{
    // nothing else to do...
}

IndicOpenTypeLayoutEngine::~IndicOpenTypeLayoutEngine()
{
    // nothing to do
}

// Input: characters, tags
// Output: glyphs, char indices
le_int32 IndicOpenTypeLayoutEngine::glyphProcessing(const LEUnicode chars[], le_int32 offset, le_int32 count, le_int32 max, le_bool rightToLeft, const LETag **featureTags,
                LEGlyphID *&glyphs, le_int32 *&charIndices)
{
    le_int32 retCount = OpenTypeLayoutEngine::glyphProcessing(chars, offset, count, max, rightToLeft, featureTags, glyphs, charIndices);

    IndicReordering::adjustMPres(&chars[offset], count, glyphs, charIndices, fScriptCode);

    return retCount;
}

// Input: characters
// Output: characters, char indices, tags
// Returns: output character count
le_int32 IndicOpenTypeLayoutEngine::characterProcessing(const LEUnicode chars[], le_int32 offset, le_int32 count, le_int32 max, le_bool rightToLeft,
        LEUnicode *&outChars, le_int32 *&charIndices, const LETag **&featureTags)
{
    le_int32 worstCase = count * IndicReordering::getWorstCaseExpansion(fScriptCode);

    outChars = new LEUnicode[worstCase];
    charIndices = new le_int32[worstCase];
    featureTags = new const LETag*[worstCase];

    // NOTE: assumes this allocates featureTags...
    // (probably better than doing the worst case stuff here...)
    return IndicReordering::reorder(&chars[offset], count, fScriptCode, outChars, charIndices, featureTags);
}

CDACOpenTypeLayoutEngine::CDACOpenTypeLayoutEngine(LEFontInstance *fontInstance, le_int32 scriptCode, le_int32 languageCode,
                                                   const CDACLayout::ScriptInfo *scriptInfo)
    : IndicOpenTypeLayoutEngine(fontInstance, scriptCode, languageCode), fScriptInfo(scriptInfo)
{
    fGSUBTable = (GlyphSubstitutionTableHeader *) fScriptInfo->glyphSubstitutionTable;
    fGDEFTable = (GlyphDefinitionTableHeader *) fScriptInfo->glyphDefinitionTable;
}

CDACOpenTypeLayoutEngine::~CDACOpenTypeLayoutEngine()
{
    reset();
}

// "glyph" -> char
// char -> glyph
le_int32 CDACOpenTypeLayoutEngine::glyphPostProcessing(LEGlyphID tempGlyphs[], le_int32 tempCharIndices[], le_int32 tempGlyphCount,
                LEGlyphID *&glyphs, le_int32 *&charIndices)
{
    LEUnicode *tempChars;
    le_int32 glyphCount = CDACLayout::countCDACGlyphs(fScriptInfo, tempGlyphs, tempGlyphCount);

    tempChars = new LEUnicode[glyphCount];
    glyphs = new LEGlyphID[glyphCount];
    charIndices = new le_int32[glyphCount];

    // NOTE: might want mapCDACGlyphsToChars to count output chars and allocate the array to hold them...
    // or it could make a guess and grow the array if the guess is too small...
    CDACLayout::mapCDACGlyphsToChars(fScriptInfo, tempGlyphs, tempCharIndices, tempGlyphCount, tempChars, charIndices);
    IndicOpenTypeLayoutEngine::mapCharsToGlyphs(tempChars, 0, glyphCount, false, false, glyphs, charIndices);

    delete[] tempChars;

    return glyphCount;
}

void CDACOpenTypeLayoutEngine::mapCharsToGlyphs(const LEUnicode chars[], le_int32 offset, le_int32 count, le_bool reverse, le_bool mirror, LEGlyphID *&glyphs, le_int32 *&charIndices)
{
    glyphs = new LEGlyphID[count];

    CDACLayout::initCDACGlyphs(fScriptInfo, chars, offset, count, glyphs);
}

