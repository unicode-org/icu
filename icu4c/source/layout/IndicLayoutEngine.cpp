
/*
 * @(#)IndicLayoutEngine.cpp	1.3 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000, 2001 - All Rights Reserved
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

#include "IndicReordering.h"

IndicOpenTypeLayoutEngine::IndicOpenTypeLayoutEngine(const LEFontInstance *fontInstance, le_int32 scriptCode, le_int32 languageCode,
                    const GlyphSubstitutionTableHeader *gsubTable)
    : OpenTypeLayoutEngine(fontInstance, scriptCode, languageCode, gsubTable)
{
    // nothing else to do...
}

IndicOpenTypeLayoutEngine::IndicOpenTypeLayoutEngine(const LEFontInstance *fontInstance, le_int32 scriptCode, le_int32 languageCode)
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
                LEGlyphID *&glyphs, le_int32 *&charIndices, LEErrorCode &success)
{
	if (LE_FAILURE(success)) {
		return 0;
	}

	if (chars == NULL || offset < 0 || count < 0) {
		success = LE_ILLEGAL_ARGUMENT_ERROR;
		return 0;
	}

    le_int32 retCount = OpenTypeLayoutEngine::glyphProcessing(chars, offset, count, max, rightToLeft, featureTags, glyphs, charIndices, success);

	if (LE_FAILURE(success)) {
		return 0;
	}

    IndicReordering::adjustMPres(&chars[offset], count, glyphs, charIndices, fScriptCode);

    return retCount;
}

// Input: characters
// Output: characters, char indices, tags
// Returns: output character count
le_int32 IndicOpenTypeLayoutEngine::characterProcessing(const LEUnicode chars[], le_int32 offset, le_int32 count, le_int32 max, le_bool rightToLeft,
        LEUnicode *&outChars, le_int32 *&charIndices, const LETag **&featureTags, LEErrorCode &success)
{
	if (LE_FAILURE(success)) {
		return 0;
	}

    le_int32 worstCase = count * IndicReordering::getWorstCaseExpansion(fScriptCode);

    outChars = new LEUnicode[worstCase];

	if (outChars == NULL) {
		success = LE_MEMORY_ALLOCATION_ERROR;
		return 0;
	}

    charIndices = new le_int32[worstCase];
	if (charIndices == NULL) {
		delete[] outChars;
		success = LE_MEMORY_ALLOCATION_ERROR;
		return 0;
	}

    featureTags = new const LETag*[worstCase];

	if (featureTags == NULL) {
		delete[] charIndices;
		delete[] outChars;
		success = LE_MEMORY_ALLOCATION_ERROR;
		return 0;
	}

    // NOTE: assumes this allocates featureTags...
    // (probably better than doing the worst case stuff here...)
    return IndicReordering::reorder(&chars[offset], count, fScriptCode, outChars, charIndices, featureTags);
}

