
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

U_NAMESPACE_BEGIN

const char IndicOpenTypeLayoutEngine::fgClassID=0;

IndicOpenTypeLayoutEngine::IndicOpenTypeLayoutEngine(const LEFontInstance *fontInstance, le_int32 scriptCode, le_int32 languageCode,
                    const GlyphSubstitutionTableHeader *gsubTable)
    : OpenTypeLayoutEngine(fontInstance, scriptCode, languageCode, gsubTable)
{
    fFeatureOrder = IndicReordering::getFeatureOrder();
}

IndicOpenTypeLayoutEngine::IndicOpenTypeLayoutEngine(const LEFontInstance *fontInstance, le_int32 scriptCode, le_int32 languageCode)
    : OpenTypeLayoutEngine(fontInstance, scriptCode, languageCode)
{
    fFeatureOrder = IndicReordering::getFeatureOrder();
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

    outChars = (LEUnicode *)uprv_malloc(worstCase * sizeof(LEUnicode));

    if (outChars == NULL) {
        success = LE_MEMORY_ALLOCATION_ERROR;
        return 0;
    }

    charIndices = (le_int32 *)uprv_malloc(worstCase * sizeof(le_int32));
    if (charIndices == NULL) {
        uprv_free(outChars);
        success = LE_MEMORY_ALLOCATION_ERROR;
        return 0;
    }

    featureTags = (const LETag **)uprv_malloc(worstCase * sizeof(const LETag *));

    if (featureTags == NULL) {
        uprv_free(charIndices);
        uprv_free(outChars);
        success = LE_MEMORY_ALLOCATION_ERROR;
        return 0;
    }

    // NOTE: assumes this allocates featureTags...
    // (probably better than doing the worst case stuff here...)
    return IndicReordering::reorder(&chars[offset], count, fScriptCode, outChars, charIndices, featureTags);
}

U_NAMESPACE_END
