
/*
 * %W% %W%
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000, 2001 - All Rights Reserved
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

OpenTypeLayoutEngine::OpenTypeLayoutEngine(const LEFontInstance *fontInstance, le_int32 scriptCode, le_int32 languageCode,
                        const GlyphSubstitutionTableHeader *gsubTable)
    : LayoutEngine(fontInstance, scriptCode, languageCode), fFeatureTags(NULL), fGSUBTable(gsubTable), fSubstitutionFilter(NULL)
{
    static le_uint32 gdefTableTag = 0x47444546; // "GDEF"
    static le_uint32 gposTableTag = 0x47504F53; // "GPOS"

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
        delete[] fFeatureTags;
        fFeatureTags = NULL;
    }
}

OpenTypeLayoutEngine::OpenTypeLayoutEngine(const LEFontInstance *fontInstance, le_int32 scriptCode, le_int32 languageCode)
    : LayoutEngine(fontInstance, scriptCode, languageCode), fFeatureTags(NULL), fGSUBTable(NULL), fGDEFTable(NULL), fGPOSTable(NULL),
      fSubstitutionFilter(NULL)
{
    setScriptAndLanguageTags();
}

OpenTypeLayoutEngine::~OpenTypeLayoutEngine()
{
    reset();
}

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
                LEGlyphID *&glyphs, le_int32 *&charIndices, LEErrorCode &success)
{
	if (LE_FAILURE(success)) {
		return 0;
	}

	if (chars == NULL || offset < 0 || count < 0) {
		success = LE_ILLEGAL_ARGUMENT_ERROR;
		return 0;
	}

    mapCharsToGlyphs(chars, offset, count, rightToLeft, rightToLeft, glyphs, charIndices, success);

	if (LE_FAILURE(success)) {
		return 0;
	}

    if (fGSUBTable != NULL) {
        fGSUBTable->process(glyphs, featureTags, count, rightToLeft, fScriptTag, fLangSysTag, fGDEFTable, fSubstitutionFilter);
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
                                                LEGlyphID glyphs[], le_int32 glyphCount, float positions[], LEErrorCode &success)
{
	if (LE_FAILURE(success)) {
		return;
	}

	if (chars == NULL || glyphs == NULL || positions == NULL || offset < 0 || count < 0) {
		success = LE_ILLEGAL_ARGUMENT_ERROR;
		return;
	}

    if (fGPOSTable != NULL) {
        GlyphPositionAdjustment *adjustments = new GlyphPositionAdjustment[glyphCount];

		if (adjustments == NULL) {
			success = LE_MEMORY_ALLOCATION_ERROR;
			return;
		}

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

