
/*
 * %W% %E%
 *
 * (C) Copyright IBM Corp. 1998-2003 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "LEScripts.h"
#include "LELanguages.h"

#include "LayoutEngine.h"
#include "ArabicLayoutEngine.h"
#include "HanLayoutEngine.h"
#include "IndicLayoutEngine.h"
#include "ThaiLayoutEngine.h"
#include "GXLayoutEngine.h"
#include "ScriptAndLanguageTags.h"

#include "OpenTypeUtilities.h"
#include "GlyphSubstitutionTables.h"
#include "MorphTables.h"

#include "DefaultCharMapper.h"

U_NAMESPACE_BEGIN

#define ARRAY_SIZE(array) (sizeof array  / sizeof array[0])

const LEUnicode32 DefaultCharMapper::controlChars[] = {
    0x0009, 0x000A, 0x000D,
    /*0x200C, 0x200D,*/ 0x200E, 0x200F,
    0x2028, 0x2029, 0x202A, 0x202B, 0x202C, 0x202D, 0x202E,
    0x206A, 0x206B, 0x206C, 0x206D, 0x206E, 0x206F
};

const le_int32 DefaultCharMapper::controlCharsCount = ARRAY_SIZE(controlChars);

const LEUnicode32 DefaultCharMapper::mirroredChars[] = {
    0x0028, 0x0029, // ascii paired punctuation
    0x003c, 0x003e,
    0x005b, 0x005d,
    0x007b, 0x007d,
    0x2045, 0x2046, // math symbols (not complete)
    0x207d, 0x207e,
    0x208d, 0x208e,
    0x2264, 0x2265,
    0x3008, 0x3009, // chinese paired punctuation
    0x300a, 0x300b,
    0x300c, 0x300d,
    0x300e, 0x300f,
    0x3010, 0x3011,
    0x3014, 0x3015,
    0x3016, 0x3017,
    0x3018, 0x3019,
    0x301a, 0x301b
};

const le_int32 DefaultCharMapper::mirroredCharsCount = ARRAY_SIZE(mirroredChars);

LEUnicode32 DefaultCharMapper::mapChar(LEUnicode32 ch) const
{
    if (fFilterControls) {
        le_int32 index = OpenTypeUtilities::search((le_uint32)ch, (le_uint32 *)controlChars, controlCharsCount);

        if (controlChars[index] == ch) {
            return 0xFFFF;
        }
    }

    if (fMirror) {
        le_int32 index = OpenTypeUtilities::search((le_uint32) ch, (le_uint32 *)mirroredChars, mirroredCharsCount);

        if (mirroredChars[index] == ch) {
            le_int32 mirrorOffset = ((index & 1) == 0) ? 1 : -1;

            return mirroredChars[index + mirrorOffset];
        }
    }

    return ch;
}

const char LayoutEngine::fgClassID=0;

LayoutEngine::LayoutEngine(const LEFontInstance *fontInstance, le_int32 scriptCode, le_int32 languageCode)
    : fGlyphCount(0), fGlyphs(NULL), fCharIndices(NULL), fPositions(NULL),
      fFontInstance(fontInstance), fScriptCode(scriptCode), fLanguageCode(languageCode)
{
    // nothing else to do?
}

void LayoutEngine::getCharIndices(le_int32 charIndices[], le_int32 indexBase, LEErrorCode &success) const
{
    le_int32 i;

    if LE_FAILURE(success) {
        return;
    }

    if (charIndices == NULL) {
        success = LE_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    if (fCharIndices == NULL) {
        success = LE_NO_LAYOUT_ERROR;
        return;
    }

    for (i = 0; i < fGlyphCount; i += 1) {
        charIndices[i] = fCharIndices[i] + indexBase;
    }
}

void LayoutEngine::getCharIndices(le_int32 charIndices[], LEErrorCode &success) const
{
    if LE_FAILURE(success) {
      return;
    }
    
    if (charIndices == NULL) {
      success = LE_ILLEGAL_ARGUMENT_ERROR;
      return;
    }
    
    if (fCharIndices == NULL) {
      success = LE_NO_LAYOUT_ERROR;
      return;
    }
    
    LE_ARRAY_COPY(charIndices, fCharIndices, fGlyphCount);
}

// Copy the glyphs into caller's (32-bit) glyph array, OR in extraBits
void LayoutEngine::getGlyphs(le_uint32 glyphs[], le_uint32 extraBits, LEErrorCode &success) const
{
    le_int32 i;

    if (LE_FAILURE(success)) {
        return;
    }

    if (glyphs == NULL) {
        success = LE_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    if (fGlyphs == NULL) {
        success = LE_NO_LAYOUT_ERROR;
        return;
    }

    for (i = 0; i < fGlyphCount; i += 1) {
        glyphs[i] = fGlyphs[i] | extraBits;
    }
}

void LayoutEngine::getGlyphs(LEGlyphID glyphs[], LEErrorCode &success) const
{
    if (LE_FAILURE(success)) {
      return;
    }
    
    if (glyphs == NULL) {
      success = LE_ILLEGAL_ARGUMENT_ERROR;
      return;
    }
    
    if (fGlyphs == NULL) {
      success = LE_NO_LAYOUT_ERROR;
    }
    
    LE_ARRAY_COPY(glyphs, fGlyphs, fGlyphCount);
}


void LayoutEngine::getGlyphPositions(float positions[], LEErrorCode &success) const
{
    if LE_FAILURE(success) {
      return;
    }
  
    if (positions == NULL) {
      success = LE_ILLEGAL_ARGUMENT_ERROR;
      return;
    }
    
    if (fPositions == NULL) {
      success = LE_NO_LAYOUT_ERROR;
      return;
    }
    
    LE_ARRAY_COPY(positions, fPositions, fGlyphCount * 2 + 2);
}

void LayoutEngine::getGlyphPosition(le_int32 glyphIndex, float &x, float &y, LEErrorCode &success) const
{
    if (LE_FAILURE(success)) {
      return;
    }
    
    if (glyphIndex > fGlyphCount) {
      success = LE_INDEX_OUT_OF_BOUNDS_ERROR;
      return;
    }
    
    if (fPositions == NULL) {
      success = LE_NO_LAYOUT_ERROR;
      return;
    }
    
    x = fPositions[glyphIndex * 2];
    y = fPositions[glyphIndex * 2 + 1];
}


le_int32 LayoutEngine::computeGlyphs(const LEUnicode chars[], le_int32 offset, le_int32 count, le_int32 max, le_bool rightToLeft,
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

    return count;
}

// Input: glyphs
// Output: positions
void LayoutEngine::positionGlyphs(const LEGlyphID glyphs[], le_int32 glyphCount, float x, float y, float *&positions, LEErrorCode &success)
{
    if (LE_FAILURE(success)) {
        return;
    }

    if (glyphCount < 0) {
        success = LE_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    if (positions == NULL) {
        positions = LE_NEW_ARRAY(float, 2 * (glyphCount + 1));

        if (positions == NULL) {
            success = LE_MEMORY_ALLOCATION_ERROR;
            return;
        }
    }

    le_int32 i;

    for (i = 0; i < glyphCount; i += 1) {
        LEPoint advance;

        positions[i * 2] = x;
        positions[i * 2 + 1] = y;

        fFontInstance->getGlyphAdvance(glyphs[i], advance);
        x += advance.fX;
        y += advance.fY;
    }

    positions[glyphCount * 2] = x;
    positions[glyphCount * 2 + 1] = y;
}

void LayoutEngine::adjustMarkGlyphs(const LEGlyphID glyphs[], le_int32 glyphCount, le_bool reverse, LEGlyphFilter *markFilter,
                                    float positions[], LEErrorCode &success)
{
    float xAdjust = 0;
    le_int32 g = 0, direction = 1;
    le_int32 p;

    if (LE_FAILURE(success)) {
        return;
    }

    if (positions == NULL || markFilter == NULL) {
        success = LE_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    if (reverse) {
        g = glyphCount - 1;
        direction = -1;
    }

    for (p = 0; p < glyphCount; p += 1, g += direction) {
        float xAdvance = positions[(p + 1) * 2] - positions[p * 2];

        positions[p * 2] += xAdjust;

        if (markFilter->accept(glyphs[g])) {
            xAdjust -= xAdvance;
        }
    }

    positions[glyphCount * 2] += xAdjust;
}

const void *LayoutEngine::getFontTable(LETag tableTag) const
{
    return fFontInstance->getFontTable(tableTag);
}

void LayoutEngine::mapCharsToGlyphs(const LEUnicode chars[], le_int32 offset, le_int32 count, le_bool reverse, le_bool mirror,
                                    LEGlyphID *&glyphs, le_int32 *&charIndices, LEErrorCode &success)
{
    if (LE_FAILURE(success)) {
        return;
    }

    if (chars == NULL || offset < 0 || count < 0) {
        success = LE_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    if (glyphs == NULL) {
        glyphs = LE_NEW_ARRAY(LEGlyphID, count);

        if (glyphs == NULL) {
            success = LE_MEMORY_ALLOCATION_ERROR;
            return;
        }
    }

    if (charIndices == NULL) {
        le_int32 i, dir = 1, out = 0;

        if (reverse) {
            out = count - 1;
            dir = -1;
        }

        charIndices = LE_NEW_ARRAY(le_int32, count);

        if (charIndices == NULL) {
            success = LE_MEMORY_ALLOCATION_ERROR;
            return;
        }

        for (i = 0; i < count; i += 1, out += dir) {
            charIndices[out] = i;
        }
    }

    DefaultCharMapper charMapper(true, mirror);

    fFontInstance->mapCharsToGlyphs(chars, offset, count, reverse, &charMapper, glyphs);
}

// Input: characters, font?
// Output: glyphs, positions, char indices
// Returns: number of glyphs
le_int32 LayoutEngine::layoutChars(const LEUnicode chars[], le_int32 offset, le_int32 count, le_int32 max, le_bool rightToLeft,
                              float x, float y, LEErrorCode &success)
{
    if (LE_FAILURE(success)) {
        return 0;
    }

    if (chars == NULL || offset < 0 || count < 0 || max < 0 || offset >= max || offset + count > max) {
        success = LE_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    fGlyphCount = computeGlyphs(chars, offset, count, max, rightToLeft, fGlyphs, fCharIndices, success);
    positionGlyphs(fGlyphs, fGlyphCount, x, y, fPositions, success);
    adjustGlyphPositions(chars, offset, count, rightToLeft, fGlyphs, fGlyphCount, fPositions, success);

    return fGlyphCount;
}

void LayoutEngine::reset()
{
    fGlyphCount = 0;

    if (fGlyphs != NULL) {
        LE_DELETE_ARRAY(fGlyphs);
        fGlyphs = NULL;
    }

    if (fCharIndices != NULL) {
        LE_DELETE_ARRAY(fCharIndices);
        fCharIndices = NULL;
    }

    if (fPositions != NULL) {
        LE_DELETE_ARRAY(fPositions);
        fPositions = NULL;
    }
}
    
LayoutEngine *LayoutEngine::layoutEngineFactory(const LEFontInstance *fontInstance, le_int32 scriptCode, le_int32 languageCode, LEErrorCode &success)
{
    static le_uint32 gsubTableTag = LE_GSUB_TABLE_TAG;
    static le_uint32 mortTableTag = LE_MORT_TABLE_TAG;

    if (LE_FAILURE(success)) {
        return NULL;
    }

    const GlyphSubstitutionTableHeader *gsubTable = (const GlyphSubstitutionTableHeader *) fontInstance->getFontTable(gsubTableTag);
    LayoutEngine *result = NULL;
    LETag scriptTag   = 0x00000000;
    LETag languageTag = 0x00000000;

    if (gsubTable != NULL && gsubTable->coversScript(scriptTag = OpenTypeLayoutEngine::getScriptTag(scriptCode))) {
        switch (scriptCode) {
        case bengScriptCode:
        case devaScriptCode:
        case gujrScriptCode:
        case kndaScriptCode:
        case mlymScriptCode:
        case oryaScriptCode:
        case guruScriptCode:
        case tamlScriptCode:
        case teluScriptCode:
            result = new IndicOpenTypeLayoutEngine(fontInstance, scriptCode, languageCode, gsubTable);
            break;

        case arabScriptCode:
            result = new ArabicOpenTypeLayoutEngine(fontInstance, scriptCode, languageCode, gsubTable);
            break;

        case haniScriptCode:
            languageTag = OpenTypeLayoutEngine::getLangSysTag(languageCode);

            switch (languageCode) {
            case korLanguageCode:
            case janLanguageCode:
            case zhtLanguageCode:
            case zhsLanguageCode:
                if (gsubTable->coversScriptAndLanguage(scriptTag, languageTag)) {
                    result = new HanOpenTypeLayoutEngine(fontInstance, scriptCode, languageCode, gsubTable);
                    break;
                }

                // note: falling through to default case.
            default:
                result = new OpenTypeLayoutEngine(fontInstance, scriptCode, languageCode, gsubTable);
                break;
            }

            break;

        default:
            result = new OpenTypeLayoutEngine(fontInstance, scriptCode, languageCode, gsubTable);
            break;
        }
    } else {
        const MorphTableHeader *morphTable = (MorphTableHeader *) fontInstance->getFontTable(mortTableTag);

        if (morphTable != NULL) {
            result = new GXLayoutEngine(fontInstance, scriptCode, languageCode, morphTable);
        } else {
            switch (scriptCode) {
            case bengScriptCode:
            case devaScriptCode:
            case gujrScriptCode:
            case kndaScriptCode:
            case mlymScriptCode:
            case oryaScriptCode:
            case guruScriptCode:
            case tamlScriptCode:
            case teluScriptCode:
            {
                result = new IndicOpenTypeLayoutEngine(fontInstance, scriptCode, languageCode);
                break;
            }

            case arabScriptCode:
            case hebrScriptCode:
                result = new UnicodeArabicOpenTypeLayoutEngine(fontInstance, scriptCode, languageCode);
                break;

            //case hebrScriptCode:
            //    return new HebrewOpenTypeLayoutEngine(fontInstance, scriptCode, languageCode);

            case thaiScriptCode:
                result = new ThaiLayoutEngine(fontInstance, scriptCode, languageCode);
                break;

            default:
                result = new LayoutEngine(fontInstance, scriptCode, languageCode);
                break;
            }
        }
    }

    if (result == NULL) {
        success = LE_MEMORY_ALLOCATION_ERROR;
    }

    return result;
}

LayoutEngine::~LayoutEngine() {
    reset();
}

U_NAMESPACE_END
