
/*
 *
 * (C) Copyright IBM Corp. 1998-2004 - All Rights Reserved
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

#include "LEGlyphStorage.h"

#include "GDEFMarkFilter.h"

U_NAMESPACE_BEGIN

const char OpenTypeLayoutEngine::fgClassID=0;

const LETag emptyTag = 0x00000000;

const LETag ccmpFeatureTag = LE_CCMP_FEATURE_TAG;
const LETag ligaFeatureTag = LE_LIGA_FEATURE_TAG;
const LETag cligFeatureTag = LE_CLIG_FEATURE_TAG;
const LETag kernFeatureTag = LE_KERN_FEATURE_TAG;
const LETag markFeatureTag = LE_MARK_FEATURE_TAG;
const LETag mkmkFeatureTag = LE_MKMK_FEATURE_TAG;

const LETag defaultFeatures[] = {ccmpFeatureTag, ligaFeatureTag, cligFeatureTag, kernFeatureTag, markFeatureTag, mkmkFeatureTag, emptyTag};


OpenTypeLayoutEngine::OpenTypeLayoutEngine(const LEFontInstance *fontInstance, le_int32 scriptCode, le_int32 languageCode,
                        const GlyphSubstitutionTableHeader *gsubTable)
    : LayoutEngine(fontInstance, scriptCode, languageCode), fFeatureList(defaultFeatures), fFeatureOrder(NULL),
      fGSUBTable(gsubTable), fGDEFTable(NULL), fGPOSTable(NULL), fSubstitutionFilter(NULL)
{
    static le_uint32 gdefTableTag = LE_GDEF_TABLE_TAG;
    static le_uint32 gposTableTag = LE_GPOS_TABLE_TAG;
    const GlyphPositioningTableHeader *gposTable = (const GlyphPositioningTableHeader *) getFontTable(gposTableTag);

    setScriptAndLanguageTags();

    fGDEFTable = (const GlyphDefinitionTableHeader *) getFontTable(gdefTableTag);
    
    if (gposTable != NULL && gposTable->coversScriptAndLanguage(fScriptTag, fLangSysTag)) {
        fGPOSTable = gposTable;
    }
}

void OpenTypeLayoutEngine::reset()
{
    // NOTE: if we're called from
    // the destructor, LayoutEngine;:reset()
    // will have been called already by
    // LayoutEngine::~LayoutEngine()
    LayoutEngine::reset();
}

OpenTypeLayoutEngine::OpenTypeLayoutEngine(const LEFontInstance *fontInstance, le_int32 scriptCode, le_int32 languageCode)
    : LayoutEngine(fontInstance, scriptCode, languageCode), fFeatureOrder(NULL),
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

le_int32 OpenTypeLayoutEngine::characterProcessing(const LEUnicode chars[], le_int32 offset, le_int32 count, le_int32 max, le_bool rightToLeft,
                LEUnicode *&outChars, LEGlyphStorage &glyphStorage, LEErrorCode &success)
{
    if (LE_FAILURE(success)) {
        return 0;
    }

    if (offset < 0 || count < 0 || max < 0 || offset >= max || offset + count > max) {
        success = LE_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    le_int32 outCharCount = LayoutEngine::characterProcessing(chars, offset, count, max, rightToLeft, outChars, glyphStorage, success);

    if (LE_FAILURE(success)) {
        return 0;
    }

    glyphStorage.allocateGlyphArray(outCharCount, rightToLeft, success);
    glyphStorage.allocateAuxData(success);

    for (le_int32 i = 0; i < outCharCount; i += 1) {
        glyphStorage.setAuxData(i, (void *) fFeatureList, success);
    }

    return outCharCount;
}

// Input: characters, tags
// Output: glyphs, char indices
le_int32 OpenTypeLayoutEngine::glyphProcessing(const LEUnicode chars[], le_int32 offset, le_int32 count, le_int32 max, le_bool rightToLeft,
                                               LEGlyphStorage &glyphStorage, LEErrorCode &success)
{
    if (LE_FAILURE(success)) {
        return 0;
    }

    if (chars == NULL || offset < 0 || count < 0 || max < 0 || offset >= max || offset + count > max) {
        success = LE_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    mapCharsToGlyphs(chars, offset, count, rightToLeft, rightToLeft, glyphStorage, success);

    if (LE_FAILURE(success)) {
        return 0;
    }

    if (fGSUBTable != NULL) {
        count = fGSUBTable->process(glyphStorage, rightToLeft, fScriptTag, fLangSysTag, fGDEFTable, fSubstitutionFilter, fFeatureOrder);
    }

    return count;
}

le_int32 OpenTypeLayoutEngine::glyphPostProcessing(LEGlyphStorage &tempGlyphStorage, LEGlyphStorage &glyphStorage, LEErrorCode &success)
{
    if (LE_FAILURE(success)) {
        return 0;
    }

    glyphStorage.adoptGlyphArray(tempGlyphStorage);
    glyphStorage.adoptCharIndicesArray(tempGlyphStorage);
    glyphStorage.adoptAuxDataArray(tempGlyphStorage);
    glyphStorage.adoptGlyphCount(tempGlyphStorage);

    return glyphStorage.getGlyphCount();
}

le_int32 OpenTypeLayoutEngine::computeGlyphs(const LEUnicode chars[], le_int32 offset, le_int32 count, le_int32 max, le_bool rightToLeft, LEGlyphStorage &glyphStorage, LEErrorCode &success)
{
    LEUnicode *outChars = NULL;
    LEGlyphStorage fakeGlyphStorage;
    le_int32 outCharCount, outGlyphCount, fakeGlyphCount;

    if (LE_FAILURE(success)) {
        return 0;
    }

    if (chars == NULL || offset < 0 || count < 0 || max < 0 || offset >= max || offset + count > max) {
        success = LE_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    outCharCount = characterProcessing(chars, offset, count, max, rightToLeft, outChars, fakeGlyphStorage, success);

    if (outChars != NULL) {
        fakeGlyphCount = glyphProcessing(outChars, 0, outCharCount, outCharCount, rightToLeft, fakeGlyphStorage, success);
        LE_DELETE_ARRAY(outChars); // FIXME: a subclass may have allocated this, in which case this delete might not work...
        //adjustGlyphs(outChars, 0, outCharCount, rightToLeft, fakeGlyphs, fakeGlyphCount);
    } else {
        fakeGlyphCount = glyphProcessing(chars, offset, count, max, rightToLeft, fakeGlyphStorage, success);
        //adjustGlyphs(chars, offset, count, rightToLeft, fakeGlyphs, fakeGlyphCount);
    }

    outGlyphCount = glyphPostProcessing(fakeGlyphStorage, glyphStorage, success);

    return outGlyphCount;
}

// apply GPOS table, if any
void OpenTypeLayoutEngine::adjustGlyphPositions(const LEUnicode chars[], le_int32 offset, le_int32 count, le_bool reverse,
                                                LEGlyphStorage &glyphStorage, LEErrorCode &success)
{
    if (LE_FAILURE(success)) {
        return;
    }

    if (chars == NULL || offset < 0 || count < 0) {
        success = LE_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    le_int32 glyphCount = glyphStorage.getGlyphCount();

    if (glyphCount > 0 && fGPOSTable != NULL) {
        GlyphPositionAdjustment *adjustments = new GlyphPositionAdjustment[glyphCount];
        le_int32 i;

        if (adjustments == NULL) {
            success = LE_MEMORY_ALLOCATION_ERROR;
            return;
        }

#if 0
        // Don't need to do this if we allocate
        // the adjustments array w/ new...
        for (i = 0; i < glyphCount; i += 1) {
            adjustments[i].setXPlacement(0);
            adjustments[i].setYPlacement(0);

            adjustments[i].setXAdvance(0);
            adjustments[i].setYAdvance(0);

            adjustments[i].setBaseOffset(-1);
        }
#endif

        fGPOSTable->process(glyphStorage, adjustments, reverse, fScriptTag, fLangSysTag, fGDEFTable, fFontInstance, fFeatureOrder);

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

            xPlacement = fFontInstance->xUnitsToPoints(xPlacement);
            yPlacement = fFontInstance->yUnitsToPoints(yPlacement);
            glyphStorage.adjustPosition(i, xAdjust + xPlacement, -(yAdjust + yPlacement), success);

            xAdjust += fFontInstance->xUnitsToPoints(xAdvance);
            yAdjust += fFontInstance->yUnitsToPoints(yAdvance);
        }

        glyphStorage.adjustPosition(glyphCount, xAdjust, -yAdjust, success);

        delete[] adjustments;
    }

#if 0
    // Don't know why this is here...
    LE_DELETE_ARRAY(fFeatureTags);
    fFeatureTags = NULL;
#endif
}

U_NAMESPACE_END
