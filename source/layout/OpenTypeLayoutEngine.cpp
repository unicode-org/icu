
/*
 * @(#)OpenTypeLayoutEngine.cpp	1.3 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "LayoutEngine.h"
#include "OpenTypeLayoutEngine.h"
#include "ScriptAndLanguageTags.h"

#include "GlyphSubstitutionTables.h"
#include "GlyphDefinitionTables.h"
#include "GlyphPositioningTables.h"

#include "GDEFMarkFilter.h"

OpenTypeLayoutEngine::OpenTypeLayoutEngine(LEFontInstance *fontInstance, le_int32 scriptCode, le_int32 languageCode,
                        GlyphSubstitutionTableHeader *gsubTable)
    : LayoutEngine(fontInstance, scriptCode, languageCode), fFeatureTags(NULL), fGSUBTable(gsubTable), fSubstitutionFilter(NULL)
{
    static le_uint32 gdefTableTag = 0x47444546; // "GDEF"
    static le_uint32 gposTableTag = 0x47504F53; // "GPOS"

    fGDEFTable = (GlyphDefinitionTableHeader *) getFontTable(gdefTableTag);
    fGPOSTable = (GlyphPositioningTableHeader *) getFontTable(gposTableTag);

    setScriptAndLanguageTags();
}

void OpenTypeLayoutEngine::reset()
{
    // NOTE: if we're called from
    // the destructor, LayoutEngine;:reset()
    // will have been called already by
    // LayoutEngine::~LayoutEngine()
    LayoutEngine::reset();

    // The double call could be avoided by
    // puting the following into a private
    // method that's called from here and
    // from our destructor
    if (fFeatureTags != NULL) {
        delete[] fFeatureTags;
        fFeatureTags = NULL;
    }
}

OpenTypeLayoutEngine::OpenTypeLayoutEngine(LEFontInstance *fontInstance, le_int32 scriptCode, le_int32 languageCode)
    : LayoutEngine(fontInstance, scriptCode, languageCode), fFeatureTags(NULL), fGSUBTable(NULL), fGDEFTable(NULL), fGPOSTable(NULL),
      fSubstitutionFilter(NULL)
{
    setScriptAndLanguageTags();
}

OpenTypeLayoutEngine::~OpenTypeLayoutEngine()
{
    reset();
}

LETag scriptTags[] = {
    neutScriptTag, // 'NEUT'
    latnScriptTag, // 'latn'
    grekScriptTag, // 'grek'
    cyrlScriptTag, // 'cyrl'
    armnScriptTag, // 'armn' **** Armenian: no MS definition ****
    hebrScriptTag, // 'hebr'
    arabScriptTag, // 'arab'
    devaScriptTag, // 'deva'
    bengScriptTag, // 'beng'
    punjScriptTag, // 'punj' punjabi == gurmukhi
    gujrScriptTag, // 'gujr'
    oryaScriptTag, // 'orya'
    tamlScriptTag, // 'taml'
    teluScriptTag, // 'telu'
    kndaScriptTag, // 'knda'
    mlymScriptTag, // 'mlym'
    thaiScriptTag, // 'thai'
    laoScriptTag,  // 'lao ' **** Lao: no MS definition ****
    tibtScriptTag, // 'tibt'
    grgnScriptTag, // 'grgn' **** Georgian: no MS definition ****
    hangScriptTag, // 'hang'
    kanaScriptTag, // 'kana'
    bpmfScriptTag, // 'bpmf' **** Bopomofo: no MS definition ****
    knbnScriptTag, // 'knbn' **** Kanbun: no MS definition ****
    haniScriptTag, // 'hani'
    surrScriptTag, // 'SURR'
    puseScriptTag, // 'PUSE'
    spclScriptTag  // 'SPCL'
};

LETag OpenTypeLayoutEngine::getScriptTag(le_int32 scriptCode)
{
    if (scriptCode == -1) {
        return -1;
    }

    return scriptTags[scriptCode];
}

LETag OpenTypeLayoutEngine::getLangSysTag(le_int32 languageCode)
{
    // FIXME: do this for real some day (soon?)
    return -1;
}

void OpenTypeLayoutEngine::setScriptAndLanguageTags()
{
    fScriptTag  = getScriptTag(fScriptCode);
    fLangSysTag = getLangSysTag(fLanguageCode);
}

// Input: characters, tags
// Output: glyphs, char indices
le_int32 OpenTypeLayoutEngine::glyphProcessing(const LEUnicode chars[], le_int32 offset, le_int32 count, le_int32 max, le_bool rightToLeft, const LETag **featureTags,
                LEGlyphID *&glyphs, le_int32 *&charIndices)
{
    mapCharsToGlyphs(chars, offset, count, rightToLeft, rightToLeft, glyphs, charIndices);

    if (fGSUBTable != NULL) {
        fGSUBTable->process(glyphs, featureTags, count, rightToLeft, fScriptTag, fLangSysTag, fGDEFTable, fSubstitutionFilter);
    }

    return count;
}

le_int32 OpenTypeLayoutEngine::computeGlyphs(const LEUnicode chars[], le_int32 offset, le_int32 count, le_int32 max, le_bool rightToLeft, LEGlyphID *&glyphs, le_int32 *&charIndices)
{
    LEUnicode *outChars = NULL;
    LEGlyphID *fakeGlyphs = NULL;
    le_int32 *tempCharIndices = NULL;
    le_int32 outCharCount, outGlyphCount, fakeGlyphCount;

    outCharCount   = characterProcessing(chars, offset, count, max, rightToLeft, outChars, tempCharIndices, fFeatureTags);

    if (outChars != NULL) {
        fakeGlyphCount = glyphProcessing(outChars, 0, outCharCount, outCharCount, rightToLeft, fFeatureTags, fakeGlyphs, tempCharIndices);
        //adjustGlyphs(outChars, 0, outCharCount, rightToLeft, fakeGlyphs, fakeGlyphCount);
    } else {
        fakeGlyphCount = glyphProcessing(chars, offset, count, max, rightToLeft, fFeatureTags, fakeGlyphs, tempCharIndices);
        //adjustGlyphs(chars, offset, count, rightToLeft, fakeGlyphs, fakeGlyphCount);
    }

    outGlyphCount  = glyphPostProcessing(fakeGlyphs, tempCharIndices, fakeGlyphCount, glyphs, charIndices);

    if (outChars != chars) {
        delete[] outChars;
    }

    if (fakeGlyphs != glyphs) {
        delete[] fakeGlyphs;
    }

    if (tempCharIndices != charIndices) {
        delete[] tempCharIndices;
    }

    return outGlyphCount;
}

// apply GPOS table, if any
void OpenTypeLayoutEngine::adjustGlyphPositions(const LEUnicode chars[], le_int32 offset, le_int32 count, le_bool reverse,
                                                LEGlyphID glyphs[], le_int32 glyphCount, float positions[])
{
    if (fGPOSTable != NULL) {
        GlyphPositionAdjustment *adjustments = new GlyphPositionAdjustment[glyphCount];

        fGPOSTable->process(glyphs, adjustments, fFeatureTags, glyphCount, reverse, fScriptTag, fLangSysTag, fGDEFTable, fFontInstance);

        float xAdjust = 0, yAdjust = 0;
        le_int32 i;

        for (i = 0; i < glyphCount; i += 1) {
            float xPlacement = fFontInstance->xUnitsToPoints(adjustments[i].getXPlacement());
            float xAdvance   = fFontInstance->xUnitsToPoints(adjustments[i].getXAdvance());
            float yPlacement = fFontInstance->yUnitsToPoints(adjustments[i].getYPlacement());
            float yAdvance   = fFontInstance->yUnitsToPoints(adjustments[i].getYAdvance());

            xAdjust += xPlacement;
            yAdjust += yPlacement;

            positions[i*2] += xAdjust;
            positions[i*2+1] += yAdjust;

            xAdjust += xAdvance;
            yAdjust += yAdvance;
        }

        positions[glyphCount*2] += xAdjust;
        positions[glyphCount*2+1] += yAdjust;

        delete[] adjustments;
    }

    delete[] fFeatureTags;
    fFeatureTags = NULL;
}

