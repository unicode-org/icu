
/*
 * %W% %E%
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
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
    LEFontInstance *fFontInstance;

public:
    CharSubstitutionFilter(LEFontInstance *fontInstance);
    le_bool accept(LEGlyphID glyph);
};

CharSubstitutionFilter::CharSubstitutionFilter(LEFontInstance *fontInstance)
  : fFontInstance(fontInstance)
{
    // nothing to do
}

le_bool CharSubstitutionFilter::accept(LEGlyphID glyph)
{
    return fFontInstance->canDisplay((LEUnicode) glyph);
}

ArabicOpenTypeLayoutEngine::ArabicOpenTypeLayoutEngine(LEFontInstance *fontInstance, le_int32 scriptCode, le_int32 languageCode,
                        GlyphSubstitutionTableHeader *gsubTable)
    : OpenTypeLayoutEngine(fontInstance, scriptCode, languageCode, gsubTable)
{
    // nothing else to do...
}

ArabicOpenTypeLayoutEngine::ArabicOpenTypeLayoutEngine(LEFontInstance *fontInstance, le_int32 scriptCode, le_int32 languageCode)
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
        LEUnicode *&outChars, le_int32 *&charIndices, const LETag **&featureTags)
{
    featureTags = new const LETag*[count];

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
                                                      LEGlyphID glyphs[], le_int32 glyphCount, float positions[])
{
    if (fGPOSTable != NULL) {
        OpenTypeLayoutEngine::adjustGlyphPositions(chars, offset, count, reverse, glyphs, glyphCount, positions);
    } else if (fGDEFTable != NULL) {
        GDEFMarkFilter filter(fGDEFTable);

        adjustMarkGlyphs(glyphs, glyphCount, false, &filter, positions);
    } else {
        GlyphDefinitionTableHeader *gdefTable = (GlyphDefinitionTableHeader *) ArabicShaping::glyphDefinitionTable;
        GDEFMarkFilter filter(gdefTable);

        // this won't work if LEGlyphID and LEUnicode aren't the same size...
        adjustMarkGlyphs((const LEGlyphID *) &chars[offset], count, reverse, &filter, positions);
    }
}

UnicodeArabicOpenTypeLayoutEngine::UnicodeArabicOpenTypeLayoutEngine(LEFontInstance *fontInstance, le_int32 scriptCode, le_int32 languageCode)
    : ArabicOpenTypeLayoutEngine(fontInstance, scriptCode, languageCode)
{
	switch (scriptCode) {
	case arabScriptCode:
		fGSUBTable = (GlyphSubstitutionTableHeader *) ArabicShaping::glyphSubstitutionTable;
		fGDEFTable = (GlyphDefinitionTableHeader *) ArabicShaping::glyphDefinitionTable;
		break;

	case hebrScriptCode:
		fGSUBTable = (GlyphSubstitutionTableHeader *) HebrewShaping::glyphSubstitutionTable;
		fGDEFTable = (GlyphDefinitionTableHeader *) HebrewShaping::glyphDefinitionTable;
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
                LEGlyphID *&glyphs, le_int32 *&charIndices)
{
    charIndices = tempCharIndices;

    // NOTE: need to copy tempGlyphs to an LEUnicode array if LEGlyphID and LEUnicode aren't the same size...
    ArabicOpenTypeLayoutEngine::mapCharsToGlyphs((LEUnicode *) tempGlyphs, 0, tempGlyphCount, false, true, glyphs, charIndices);

    return tempGlyphCount;
}

void UnicodeArabicOpenTypeLayoutEngine::mapCharsToGlyphs(const LEUnicode chars[], le_int32 offset, le_int32 count, le_bool reverse, le_bool mirror, LEGlyphID *&glyphs, le_int32 *&charIndices)
{
    le_int32 i, dir, out;

    out = 0;
    dir = 1;

    if (reverse) {
        out = count - 1;
        dir = -1;
    }

    glyphs = new LEGlyphID[count];
    charIndices = new le_int32[count];

    for (i = 0; i < count; i += 1, out += dir) {
        glyphs[out] = (LEGlyphID) chars[offset + i];
        charIndices[out] = i;
    }
}

void UnicodeArabicOpenTypeLayoutEngine::adjustGlyphPositions(const LEUnicode chars[], le_int32 offset, le_int32 count, le_bool reverse,
                                                      LEGlyphID glyphs[], le_int32 glyphCount, float positions[])
{
    GDEFMarkFilter filter(fGDEFTable);

    // this won't work if LEGlyphID and LEUnicode aren't the same size...
    adjustMarkGlyphs((const LEGlyphID *) &chars[offset], count, reverse, &filter, positions);
}



