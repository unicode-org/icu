
/*
 * %W% %W%
 *
 * (C) Copyright IBM Corp. 1998-2003 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "LEScripts.h"
#include "LELanguages.h"

#include "LayoutEngine.h"
#include "OpenTypeLayoutEngine.h"
#include "ScriptAndLanguageTags.h"

#include "GlyphSubstitutionTables.h"
#include "GlyphDefinitionTables.h"
#include "GlyphPositioningTables.h"

#include "GDEFMarkFilter.h"

U_NAMESPACE_BEGIN

const char OpenTypeLayoutEngine::fgClassID=0;

OpenTypeLayoutEngine::OpenTypeLayoutEngine(const LEFontInstance *fontInstance, le_int32 scriptCode, le_int32 languageCode,
                        const GlyphSubstitutionTableHeader *gsubTable)
    : LayoutEngine(fontInstance, scriptCode, languageCode), fFeatureTags(NULL), fFeatureOrder(NULL),
      fGSUBTable(gsubTable), fSubstitutionFilter(NULL)
{
    static le_uint32 gdefTableTag = LE_GDEF_TABLE_TAG;
    static le_uint32 gposTableTag = LE_GPOS_TABLE_TAG;

    fGDEFTable = (const GlyphDefinitionTableHeader *) getFontTable(gdefTableTag);
    fGPOSTable = (const GlyphPositioningTableHeader *) getFontTable(gposTableTag);

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
        LE_DELETE_ARRAY(fFeatureTags);
        fFeatureTags = NULL;
    }
}

OpenTypeLayoutEngine::OpenTypeLayoutEngine(const LEFontInstance *fontInstance, le_int32 scriptCode, le_int32 languageCode)
    : LayoutEngine(fontInstance, scriptCode, languageCode), fFeatureTags(NULL), fFeatureOrder(NULL),
      fGSUBTable(NULL), fGDEFTable(NULL), fGPOSTable(NULL), fSubstitutionFilter(NULL)
{
    setScriptAndLanguageTags();
}

OpenTypeLayoutEngine::~OpenTypeLayoutEngine()
{
    reset();
}

LETag OpenTypeLayoutEngine::getScriptTag(le_int32 scriptCode)
{
    if (scriptCode < 0 || scriptCode >= scriptCodeCount) {
        return 0xFFFFFFFF;
    }

    return scriptTags[scriptCode];
}

LETag OpenTypeLayoutEngine::getLangSysTag(le_int32 languageCode)
{
    if (languageCode < 0 || languageCode >= languageCodeCount) {
        return 0xFFFFFFFF;
    }

    return languageTags[languageCode];
}

void OpenTypeLayoutEngine::setScriptAndLanguageTags()
{
    fScriptTag  = getScriptTag(fScriptCode);
    fLangSysTag = getLangSysTag(fLanguageCode);
}

// Input: characters, tags
// Output: glyphs, char indices
le_int32 OpenTypeLayoutEngine::glyphProcessing(const LEUnicode chars[], le_int32 offset, le_int32 count, le_int32 max, le_bool rightToLeft, const LETag **featureTags,
                LEGlyphID *&glyphs, le_int32 *&charIndices, LEErrorCode &success)
{
    if (LE_FAILURE(success)) {
        return 0;
    }

    if (chars == NULL || offset < 0 || count < 0 || max < 0 || offset >= max || offset + count > max) {
        success = LE_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    mapCharsToGlyphs(chars, offset, count, rightToLeft, rightToLeft, glyphs, charIndices, success);

    if (LE_FAILURE(success)) {
        return 0;
    }

    if (fGSUBTable != NULL) {
        fGSUBTable->process(glyphs, featureTags, count, rightToLeft, fScriptTag, fLangSysTag, fGDEFTable, fSubstitutionFilter, fFeatureOrder);
    }

    return count;
}

le_int32 OpenTypeLayoutEngine::computeGlyphs(const LEUnicode chars[], le_int32 offset, le_int32 count, le_int32 max, le_bool rightToLeft, LEGlyphID *&glyphs, le_int32 *&charIndices, LEErrorCode &success)
{
    LEUnicode *outChars = NULL;
    LEGlyphID *fakeGlyphs = NULL;
    le_int32 *tempCharIndices = NULL;
    le_int32 outCharCount, outGlyphCount, fakeGlyphCount;

    if (LE_FAILURE(success)) {
        return 0;
    }

    if (chars == NULL || offset < 0 || count < 0 || max < 0 || offset >= max || offset + count > max) {
        success = LE_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    outCharCount   = characterProcessing(chars, offset, count, max, rightToLeft, outChars, tempCharIndices, fFeatureTags, success);

    if (outChars != NULL) {
        fakeGlyphCount = glyphProcessing(outChars, 0, outCharCount, outCharCount, rightToLeft, fFeatureTags, fakeGlyphs, tempCharIndices, success);
        //adjustGlyphs(outChars, 0, outCharCount, rightToLeft, fakeGlyphs, fakeGlyphCount);
    } else {
        fakeGlyphCount = glyphProcessing(chars, offset, count, max, rightToLeft, fFeatureTags, fakeGlyphs, tempCharIndices, success);
        //adjustGlyphs(chars, offset, count, rightToLeft, fakeGlyphs, fakeGlyphCount);
    }

    outGlyphCount  = glyphPostProcessing(fakeGlyphs, tempCharIndices, fakeGlyphCount, glyphs, charIndices, success);

    if (outChars != chars) {
        LE_DELETE_ARRAY(outChars);
    }

    if (fakeGlyphs != glyphs) {
        LE_DELETE_ARRAY(fakeGlyphs);
    }

    if (tempCharIndices != charIndices) {
        LE_DELETE_ARRAY(tempCharIndices);
    }

    return outGlyphCount;
}

// apply GPOS table, if any
void OpenTypeLayoutEngine::adjustGlyphPositions(const LEUnicode chars[], le_int32 offset, le_int32 count, le_bool reverse,
                                                LEGlyphID glyphs[], le_int32 glyphCount, float positions[], LEErrorCode &success)
{
    if (LE_FAILURE(success)) {
        return;
    }

    if (chars == NULL || glyphs == NULL || positions == NULL || offset < 0 || count < 0) {
        success = LE_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    if (glyphCount > 0 && fGPOSTable != NULL) {
        GlyphPositionAdjustment *adjustments = LE_NEW_ARRAY(GlyphPositionAdjustment, glyphCount);
        le_int32 i;

        if (adjustments == NULL) {
            success = LE_MEMORY_ALLOCATION_ERROR;
            return;
        }

        for (i = 0; i < glyphCount; i += 1) {
            adjustments[i].setXPlacement(0);
            adjustments[i].setYPlacement(0);

            adjustments[i].setXAdvance(0);
            adjustments[i].setYAdvance(0);

            adjustments[i].setBaseOffset(-1);
        }

        fGPOSTable->process(glyphs, adjustments, fFeatureTags, glyphCount, reverse, fScriptTag, fLangSysTag, fGDEFTable, fFontInstance, fFeatureOrder);

        float xAdjust = 0, yAdjust = 0;

        for (i = 0; i < glyphCount; i += 1) {
            float xAdvance   = adjustments[i].getXAdvance();
            float yAdvance   = adjustments[i].getYAdvance();
            float xPlacement = 0;
            float yPlacement = 0;


#if 0
            // This is where separate kerning adjustments
            // should get applied.
            xAdjust += xKerning;
            yAdjust += yKerning;
#endif

            for (le_int32 base = i; base >= 0; base = adjustments[base].getBaseOffset()) {
                xPlacement += adjustments[base].getXPlacement();
                yPlacement += adjustments[base].getYPlacement();
            }

            positions[i*2]   += xAdjust + fFontInstance->xUnitsToPoints(xPlacement);
            positions[i*2+1] -= yAdjust + fFontInstance->yUnitsToPoints(yPlacement);

            xAdjust += fFontInstance->xUnitsToPoints(xAdvance);
            yAdjust += fFontInstance->yUnitsToPoints(yAdvance);
        }

        positions[glyphCount*2]   += xAdjust;
        positions[glyphCount*2+1] -= yAdjust;

        LE_DELETE_ARRAY(adjustments);
    }

    LE_DELETE_ARRAY(fFeatureTags);
    fFeatureTags = NULL;
}

U_NAMESPACE_END
