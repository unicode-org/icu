
/*
 * %W% %E%
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000, 2001 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "LEGlyphFilter.h"
#include "LayoutEngine.h"
#include "OpenTypeLayoutEngine.h"
#include "ArabicLayoutEngine.h"
#include "ScriptAndLanguageTags.h"

#include "GlyphSubstitutionTables.h"
#include "GlyphDefinitionTables.h"
#include "GlyphPositioningTables.h"

#include "GDEFMarkFilter.h"

#include "ArabicShaping.h"
#include "HebrewShaping.h"

class CharSubstitutionFilter : public LEGlyphFilter
{
private:
    const LEFontInstance *fFontInstance;

public:
    CharSubstitutionFilter(const LEFontInstance *fontInstance);
    le_bool accept(LEGlyphID glyph) const;
};

CharSubstitutionFilter::CharSubstitutionFilter(const LEFontInstance *fontInstance)
  : fFontInstance(fontInstance)
{
    // nothing to do
}

le_bool CharSubstitutionFilter::accept(LEGlyphID glyph) const
{
    return fFontInstance->canDisplay((LEUnicode) glyph);
}

ArabicOpenTypeLayoutEngine::ArabicOpenTypeLayoutEngine(const LEFontInstance *fontInstance, le_int32 scriptCode, le_int32 languageCode,
                        const GlyphSubstitutionTableHeader *gsubTable)
    : OpenTypeLayoutEngine(fontInstance, scriptCode, languageCode, gsubTable)
{
    // nothing else to do...
}

ArabicOpenTypeLayoutEngine::ArabicOpenTypeLayoutEngine(const LEFontInstance *fontInstance, le_int32 scriptCode, le_int32 languageCode)
    : OpenTypeLayoutEngine(fontInstance, scriptCode, languageCode)
{
    // nothing else to do...
}

ArabicOpenTypeLayoutEngine::~ArabicOpenTypeLayoutEngine()
{
    // nothing to do
}

// Input: characters
// Output: characters, char indices, tags
// Returns: output character count
le_int32 ArabicOpenTypeLayoutEngine::characterProcessing(const LEUnicode chars[], le_int32 offset, le_int32 count, le_int32 max, le_bool rightToLeft,
        LEUnicode *&outChars, le_int32 *&charIndices, const LETag **&featureTags, LEErrorCode &success)
{
	if (LE_FAILURE(success)) {
		return 0;
	}

	if (chars == NULL || offset < 0 || count < 0 || max < 0 || offset >= max || offset + count > max) {
		success = LE_ILLEGAL_ARGUMENT_ERROR;
		return 0;
	}

    featureTags = new const LETag*[count];

	if (featureTags == NULL) {
		success = LE_MEMORY_ALLOCATION_ERROR;
		return 0;
	}

	switch (fScriptCode) {
	case arabScriptCode:
	{
		GlyphShaper shaper(featureTags);

		// NOTE: may not need seperate shaper if always use tags...
		// NOTE: shaper could allocate the feature tags...
		ArabicShaping::shape(chars, offset, count, max, rightToLeft, shaper);
		break;
	}

	case hebrScriptCode:
		HebrewShaping::shape(chars, offset, count, max, rightToLeft, featureTags);
		break;
	}

    return count;
}

void ArabicOpenTypeLayoutEngine::adjustGlyphPositions(const LEUnicode chars[], le_int32 offset, le_int32 count, le_bool reverse,
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
        OpenTypeLayoutEngine::adjustGlyphPositions(chars, offset, count, reverse, glyphs, glyphCount, positions, success);
    } else if (fGDEFTable != NULL) {
        GDEFMarkFilter filter(fGDEFTable);

        adjustMarkGlyphs(glyphs, glyphCount, false, &filter, positions, success);
    } else {
        GlyphDefinitionTableHeader *gdefTable = (GlyphDefinitionTableHeader *) ArabicShaping::glyphDefinitionTable;
        GDEFMarkFilter filter(gdefTable);

        // this won't work if LEGlyphID and LEUnicode aren't the same size...
        adjustMarkGlyphs((const LEGlyphID *) &chars[offset], count, reverse, &filter, positions, success);
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
le_int32 UnicodeArabicOpenTypeLayoutEngine::glyphPostProcessing(LEGlyphID tempGlyphs[], le_int32 tempCharIndices[], le_int32 tempGlyphCount,
                LEGlyphID *&glyphs, le_int32 *&charIndices, LEErrorCode &success)
{
	if (LE_FAILURE(success)) {
		return 0;
	}

	if (tempGlyphs == NULL || tempCharIndices == NULL ||tempGlyphCount < 0) {
		success = LE_ILLEGAL_ARGUMENT_ERROR;
		return 0;
	}

    charIndices = tempCharIndices;

    // NOTE: need to copy tempGlyphs to an LEUnicode array if LEGlyphID and LEUnicode aren't the same size...
    ArabicOpenTypeLayoutEngine::mapCharsToGlyphs((LEUnicode *) tempGlyphs, 0, tempGlyphCount, false, true, glyphs, charIndices, success);

    return tempGlyphCount;
}

void UnicodeArabicOpenTypeLayoutEngine::mapCharsToGlyphs(const LEUnicode chars[], le_int32 offset, le_int32 count, le_bool reverse, le_bool mirror, LEGlyphID *&glyphs, le_int32 *&charIndices, LEErrorCode &success)
{
	if (LE_FAILURE(success)) {
		return;
	}

	if (chars == NULL || offset < 0 || count < 0) {
		success = LE_ILLEGAL_ARGUMENT_ERROR;
		return;
	}

    le_int32 i, dir, out;

    out = 0;
    dir = 1;

    if (reverse) {
        out = count - 1;
        dir = -1;
    }

    glyphs = new LEGlyphID[count];

	if (glyphs == NULL) {
		success = LE_MEMORY_ALLOCATION_ERROR;
		return;
	}

    charIndices = new le_int32[count];

	if (charIndices == NULL) {
		delete [] glyphs;
		success = LE_MEMORY_ALLOCATION_ERROR;
		return;
	}

    for (i = 0; i < count; i += 1, out += dir) {
        glyphs[out] = (LEGlyphID) chars[offset + i];
        charIndices[out] = i;
    }
}

void UnicodeArabicOpenTypeLayoutEngine::adjustGlyphPositions(const LEUnicode chars[], le_int32 offset, le_int32 count, le_bool reverse,
                                                      LEGlyphID glyphs[], le_int32 glyphCount, float positions[], LEErrorCode &success)
{
	if (LE_FAILURE(success)) {
		return;
	}

	if (chars == NULL || glyphs == NULL || positions == NULL || offset < 0 || count < 0) {
		success = LE_ILLEGAL_ARGUMENT_ERROR;
		return;
	}

    GDEFMarkFilter filter(fGDEFTable);

    // this won't work if LEGlyphID and LEUnicode aren't the same size...
    adjustMarkGlyphs((const LEGlyphID *) &chars[offset], count, reverse, &filter, positions, success);
}



