/*
 * HanLayoutEngine.cpp: OpenType processing for Han fonts.
 *
 * (C) Copyright IBM Corp. 1998-2004 - All Rights Reserved.
 */

#include "LETypes.h"
#include "LEScripts.h"
#include "LELanguages.h"

#include "LayoutEngine.h"
#include "OpenTypeLayoutEngine.h"
#include "HanLayoutEngine.h"
#include "ScriptAndLanguageTags.h"
#include "LEGlyphStorage.h"

U_NAMESPACE_BEGIN

const char HanOpenTypeLayoutEngine::fgClassID=0;

HanOpenTypeLayoutEngine::HanOpenTypeLayoutEngine(const LEFontInstance *fontInstance, le_int32 scriptCode, le_int32 languageCode,
                        const GlyphSubstitutionTableHeader *gsubTable)
    : OpenTypeLayoutEngine(fontInstance, scriptCode, languageCode, gsubTable)
{
    // nothing else to do...
}

HanOpenTypeLayoutEngine::~HanOpenTypeLayoutEngine()
{
    // nothing to do
}

const LETag emptyTag = 0x00000000;

const LETag loclFeatureTag = LE_LOCL_FEATURE_TAG;
const LETag smplFeatureTag = LE_SMPL_FEATURE_TAG;
const LETag tradFeatureTag = LE_TRAD_FEATURE_TAG;

const LETag features[] = {loclFeatureTag, emptyTag};

le_int32 HanOpenTypeLayoutEngine::characterProcessing(const LEUnicode chars[], le_int32 offset, le_int32 count, le_int32 max, le_bool /*rightToLeft*/,
        LEUnicode *&/*outChars*/, LEGlyphStorage &glyphStorage, LEErrorCode &success)
{
    if (LE_FAILURE(success)) {
        return 0;
    }

    if (chars == NULL || offset < 0 || count < 0 || max < 0 || offset >= max || offset + count > max) {
        success = LE_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    glyphStorage.allocateGlyphArray(count, FALSE, success);
    glyphStorage.allocateAuxData(success);

    if (LE_FAILURE(success)) {
        return 0;
    }

    // FIXME: do we want to add the 'trad' feature for 'ZHT' and the
    // 'smpl' feature for 'ZHS'? If we do this, we can remove the exact
    // flag from the language tag lookups, so we can use these features
    // with the default LangSys...
    for (le_int32 i = 0; i < count; i += 1) {
        glyphStorage.setAuxData(i, (void *) features, success);
    }

    return count;
}

U_NAMESPACE_END
