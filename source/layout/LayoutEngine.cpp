
/*
 *
 * (C) Copyright IBM Corp. 1998-2004 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "LEScripts.h"
#include "LELanguages.h"

#include "LayoutEngine.h"
#include "ArabicLayoutEngine.h"
#include "CanonShaping.h"
#include "HanLayoutEngine.h"
#include "IndicLayoutEngine.h"
#include "ThaiLayoutEngine.h"
#include "GXLayoutEngine.h"
#include "ScriptAndLanguageTags.h"
#include "CharSubstitutionFilter.h"

#include "LEGlyphStorage.h"

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

// This is here to get it out of LEGlyphFilter.h.
// No particular reason to put it here, other than
// this is a good central location...
LEGlyphFilter::~LEGlyphFilter()
{
    // nothing to do
}

CharSubstitutionFilter::CharSubstitutionFilter(const LEFontInstance *fontInstance)
  : fFontInstance(fontInstance)
{
    // nothing to do
}

CharSubstitutionFilter::~CharSubstitutionFilter()
{
    // nothing to do
}


const char LayoutEngine::fgClassID=0;

const LETag emptyTag = 0x00000000;

const LETag ccmpFeatureTag = LE_CCMP_FEATURE_TAG;

const LETag canonFeatures[] = {ccmpFeatureTag, emptyTag};

LayoutEngine::LayoutEngine(const LEFontInstance *fontInstance, le_int32 scriptCode, le_int32 languageCode)
    : fGlyphStorage(NULL), fFontInstance(fontInstance), fScriptCode(scriptCode), fLanguageCode(languageCode)
{
    fGlyphStorage = new LEGlyphStorage();
}

le_int32 LayoutEngine::getGlyphCount() const
{
    return fGlyphStorage->getGlyphCount();
};

void LayoutEngine::getCharIndices(le_int32 charIndices[], le_int32 indexBase, LEErrorCode &success) const
{
    fGlyphStorage->getCharIndices(charIndices, indexBase, success);
}

void LayoutEngine::getCharIndices(le_int32 charIndices[], LEErrorCode &success) const
{
    fGlyphStorage->getCharIndices(charIndices, success);
}

// Copy the glyphs into caller's (32-bit) glyph array, OR in extraBits
void LayoutEngine::getGlyphs(le_uint32 glyphs[], le_uint32 extraBits, LEErrorCode &success) const
{
    fGlyphStorage->getGlyphs(glyphs, extraBits, success);
}

void LayoutEngine::getGlyphs(LEGlyphID glyphs[], LEErrorCode &success) const
{
    fGlyphStorage->getGlyphs(glyphs, success);
}


void LayoutEngine::getGlyphPositions(float positions[], LEErrorCode &success) const
{
    fGlyphStorage->getGlyphPositions(positions, success);
}

void LayoutEngine::getGlyphPosition(le_int32 glyphIndex, float &x, float &y, LEErrorCode &success) const
{
    fGlyphStorage->getGlyphPosition(glyphIndex, x, y, success);
}

le_int32 LayoutEngine::characterProcessing(const LEUnicode chars[], le_int32 offset, le_int32 count, le_int32 max, le_bool rightToLeft,
                LEUnicode *&outChars, LEGlyphStorage &glyphStorage, LEErrorCode &success)
{
    if (LE_FAILURE(success)) {
        return 0;
    }

    if (offset < 0 || count < 0 || max < 0 || offset >= max || offset + count > max) {
        success = LE_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    const GlyphSubstitutionTableHeader *canonGSUBTable = (GlyphSubstitutionTableHeader *) CanonShaping::glyphSubstitutionTable;
    LETag scriptTag  = OpenTypeLayoutEngine::getScriptTag(fScriptCode);
    LETag langSysTag = OpenTypeLayoutEngine::getLangSysTag(fLanguageCode);
    le_int32 i, dir = 1, out = 0, outCharCount = count;

    if (rightToLeft) {
        out = count - 1;
        dir = -1;
    }

    if (canonGSUBTable->coversScript(scriptTag)) {
        CharSubstitutionFilter *substitutionFilter = new CharSubstitutionFilter(fFontInstance);

        glyphStorage.allocateGlyphArray(count, rightToLeft, success);
        glyphStorage.allocateAuxData(success);

        if (LE_FAILURE(success)) {
            return 0;
        }

        for (i = 0; i < count; i += 1, out += dir) {
            glyphStorage[i] = (LEGlyphID) chars[offset + i];
            glyphStorage.setAuxData(i, (void *) canonFeatures, success);
        }

        outCharCount = canonGSUBTable->process(glyphStorage, rightToLeft, scriptTag, langSysTag, NULL, substitutionFilter, NULL);

        outChars = LE_NEW_ARRAY(LEUnicode, outCharCount);
        for (i = 0; i < outCharCount; i += 1) {
            outChars[i] = (LEUnicode) LE_GET_GLYPH(glyphStorage[i]);
        }

        delete substitutionFilter;
    }

    return outCharCount;
}

le_int32 LayoutEngine::computeGlyphs(const LEUnicode chars[], le_int32 offset, le_int32 count, le_int32 max, le_bool rightToLeft,
                                            LEGlyphStorage &glyphStorage, LEErrorCode &success)
{
    if (LE_FAILURE(success)) {
        return 0;
    }

    if (chars == NULL || offset < 0 || count < 0 || max < 0 || offset >= max || offset + count > max) {
        success = LE_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    LEUnicode *outChars = NULL;
    le_int32 outCharCount = characterProcessing(chars, offset, count, max, rightToLeft, outChars, glyphStorage, success);

    if (outChars != NULL) {
        mapCharsToGlyphs(outChars, 0, outCharCount, rightToLeft, rightToLeft, glyphStorage, success);
        LE_DELETE_ARRAY(outChars); // FIXME: a subclass may have allocated this, in which case this delete might not work...
    } else {
        mapCharsToGlyphs(chars, offset, count, rightToLeft, rightToLeft, glyphStorage, success);
    }

    return glyphStorage.getGlyphCount();
}

// Input: glyphs
// Output: positions
void LayoutEngine::positionGlyphs(LEGlyphStorage &glyphStorage, float x, float y, LEErrorCode &success)
{
    if (LE_FAILURE(success)) {
        return;
    }

    glyphStorage.allocatePositions(success);

    if (LE_FAILURE(success)) {
        return;
    }

    le_int32 i, glyphCount = glyphStorage.getGlyphCount();

    for (i = 0; i < glyphCount; i += 1) {
        LEPoint advance;

        glyphStorage.setPosition(i, x, y, success);

        fFontInstance->getGlyphAdvance(glyphStorage[i], advance);
        x += advance.fX;
        y += advance.fY;
    }

    glyphStorage.setPosition(glyphCount, x, y, success);
}

void LayoutEngine::adjustGlyphPositions(const LEUnicode chars[], le_int32 offset, le_int32 count, le_bool /*reverse*/,
                                        LEGlyphStorage &/*glyphStorage*/, LEErrorCode &success)
{
    if (LE_FAILURE(success)) {
        return;
    }

    if (chars == NULL || offset < 0 || count < 0) {
        success = LE_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    // default is no adjustments
    return;
}

void LayoutEngine::adjustMarkGlyphs(LEGlyphStorage &glyphStorage, LEGlyphFilter *markFilter, LEErrorCode &success)
{
    float xAdjust = 0;
    le_int32 p, glyphCount = glyphStorage.getGlyphCount();

    if (LE_FAILURE(success)) {
        return;
    }

    if (markFilter == NULL) {
        success = LE_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    float ignore, prev;

    glyphStorage.getGlyphPosition(0, prev, ignore, success);

    for (p = 0; p < glyphCount; p += 1) {
        float next, xAdvance;
        
        glyphStorage.getGlyphPosition(p + 1, next, ignore, success);

        xAdvance = next - prev;
        glyphStorage.adjustPosition(p, xAdjust, 0, success);

        if (markFilter->accept(glyphStorage[p])) {
            xAdjust -= xAdvance;
        }

        prev = next;
    }

    glyphStorage.adjustPosition(glyphCount, xAdjust, 0, success);
}

void LayoutEngine::adjustMarkGlyphs(const LEUnicode chars[], le_int32 charCount, le_bool reverse, LEGlyphStorage &glyphStorage, LEGlyphFilter *markFilter, LEErrorCode &success)
{
    float xAdjust = 0;
    le_int32 c = 0, direction = 1, p;
    le_int32 glyphCount = glyphStorage.getGlyphCount();

    if (LE_FAILURE(success)) {
        return;
    }

    if (markFilter == NULL) {
        success = LE_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    if (reverse) {
        c = glyphCount - 1;
        direction = -1;
    }

    float ignore, prev;

    glyphStorage.getGlyphPosition(0, prev, ignore, success);

    for (p = 0; p < charCount; p += 1, c += direction) {
        float next, xAdvance;
        
        glyphStorage.getGlyphPosition(p + 1, next, ignore, success);

        xAdvance = next - prev;
        glyphStorage.adjustPosition(p, xAdjust, 0, success);

        if (markFilter->accept(chars[c])) {
            xAdjust -= xAdvance;
        }

        prev = next;
    }

    glyphStorage.adjustPosition(glyphCount, xAdjust, 0, success);
}

const void *LayoutEngine::getFontTable(LETag tableTag) const
{
    return fFontInstance->getFontTable(tableTag);
}

void LayoutEngine::mapCharsToGlyphs(const LEUnicode chars[], le_int32 offset, le_int32 count, le_bool reverse, le_bool mirror,
                                    LEGlyphStorage &glyphStorage, LEErrorCode &success)
{
    if (LE_FAILURE(success)) {
        return;
    }

    glyphStorage.allocateGlyphArray(count, reverse, success);

    DefaultCharMapper charMapper(TRUE, mirror);

    fFontInstance->mapCharsToGlyphs(chars, offset, count, reverse, &charMapper, glyphStorage);
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

    le_int32 glyphCount;
    
    glyphCount = computeGlyphs(chars, offset, count, max, rightToLeft, *fGlyphStorage, success);
    positionGlyphs(*fGlyphStorage, x, y, success);
    adjustGlyphPositions(chars, offset, count, rightToLeft, *fGlyphStorage, success);

    return glyphCount;
}

void LayoutEngine::reset()
{
    fGlyphStorage->reset();
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
                if (gsubTable->coversScriptAndLanguage(scriptTag, languageTag, TRUE)) {
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
