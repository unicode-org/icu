
/*
 * %W% %E%
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "LayoutEngine.h"
#include "ArabicLayoutEngine.h"
//#include "HebrewLayoutEngine.h"
#include "IndicLayoutEngine.h"
#include "ThaiLayoutEngine.h"
#include "GXLayoutEngine.h"
#include "ScriptAndLanguageTags.h"

#include "OpenTypeUtilities.h"
#include "GlyphSubstitutionTables.h"
#include "MorphTables.h"

#define ARRAY_SIZE(array) (sizeof array  / sizeof array[0])

class DefaultCharMapper : public LECharMapper
{
private:
    le_bool fFilterControls;
    le_bool fMirror;

    static LEUnicode32 controlChars[];

    static const le_int32 controlCharsCount;

    static LEUnicode32 mirroredChars[];

    static const le_int32 mirroredCharsCount;

public:
    DefaultCharMapper(le_bool filterControls, le_bool mirror)
        : fFilterControls(filterControls), fMirror(mirror)
    {
        // nothing
    };

    ~DefaultCharMapper()
    {
        // nada
    };

    LEUnicode32 mapChar(LEUnicode32 ch);
};

LEUnicode32 DefaultCharMapper::controlChars[] = {
    0x0009, 0x000A, 0x000D,
    /*0x200C, 0x200D,*/ 0x200E, 0x200F,
    0x2028, 0x2029, 0x202A, 0x202B, 0x202C, 0x202D, 0x202E,
    0x206A, 0x206B, 0x206C, 0x206D, 0x206E, 0x206F
};

const le_int32 DefaultCharMapper::controlCharsCount = ARRAY_SIZE(controlChars);

LEUnicode32 DefaultCharMapper::mirroredChars[] = {
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

LEUnicode32 DefaultCharMapper::mapChar(LEUnicode32 ch)
{
    if (fFilterControls) {
        le_int32 index = OpenTypeUtilities::search(ch, controlChars, controlCharsCount);

        if (controlChars[index] == ch) {
            return 0xFFFF;
        }
    }

    if (fMirror) {
        le_int32 index = OpenTypeUtilities::search(ch, mirroredChars, mirroredCharsCount);

        if (mirroredChars[index] == ch) {
            le_int32 mirrorOffset = ((index & 1) == 0) ? 1 : -1;

            return mirroredChars[index + mirrorOffset];
        }
    }

    return ch;
}

LayoutEngine::LayoutEngine(LEFontInstance *fontInstance, le_int32 scriptCode, le_int32 languageCode)
    : fGlyphCount(0), fGlyphs(NULL), fCharIndices(NULL), fPositions(NULL),
      fFontInstance(fontInstance), fScriptCode(scriptCode), fLanguageCode(languageCode)
{
    // nothing else to do?
}

void LayoutEngine::getCharIndices(le_int32 charIndices[], le_int32 indexBase)
{
	le_int32 i;

	for (i = 0; i < fGlyphCount; i += 1) {
		charIndices[i] = fCharIndices[i] + indexBase;
	}
}

// Copy the glyphs into caller's (32-bit) glyph array, OR in extraBits
void LayoutEngine::getGlyphs(le_uint32 glyphs[], le_uint32 extraBits)
{
    le_int32 i;
    
    for (i = 0; i < fGlyphCount; i += 1) {
        glyphs[i] = fGlyphs[i] | extraBits;
    }
};

le_int32 LayoutEngine::computeGlyphs(const LEUnicode chars[], le_int32 offset, le_int32 count, le_int32 max, le_bool rightToLeft,
                                            LEGlyphID *&glyphs, le_int32 *&charIndices)
{
    mapCharsToGlyphs(chars, offset, count, rightToLeft, rightToLeft, glyphs, charIndices);

    return count;
}

// Input: glyphs
// Output: positions
void LayoutEngine::positionGlyphs(const LEGlyphID glyphs[], le_int32 glyphCount, float x, float y, float *&positions)
{
    if (positions == NULL) {
        positions = new float[2 * (glyphCount + 1)];
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
                                    float positions[])
{
    float xAdjust = 0;
    le_int32 g = 0, direction = 1;
    le_int32 p;

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

void LayoutEngine::mapCharsToGlyphs(const LEUnicode chars[], le_int32 offset, le_int32 count, le_bool reverse, le_bool mirror,
                                    LEGlyphID *&glyphs, le_int32 *&charIndices)
{
    if (glyphs == NULL) {
        glyphs = new LEGlyphID[count];
    }

    if (charIndices == NULL) {
        le_int32 i, dir = 1, out = 0;

        if (reverse) {
            out = count - 1;
            dir = -1;
        }

        charIndices = new le_int32[count];

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
                              float x, float y)
{
    fGlyphCount = computeGlyphs(chars, offset, count, max, rightToLeft, fGlyphs, fCharIndices);
    positionGlyphs(fGlyphs, fGlyphCount, x, y, fPositions);
    adjustGlyphPositions(chars, offset, count, rightToLeft, fGlyphs, fGlyphCount, fPositions);

    return fGlyphCount;
}

void LayoutEngine::reset()
{
    fGlyphCount = 0;

    if (fGlyphs != NULL) {
        delete[] fGlyphs;
        fGlyphs = NULL;
    }

    if (fCharIndices != NULL) {
        delete[] fCharIndices;
        fCharIndices = NULL;
    }

    if (fPositions != NULL) {
        delete[] fPositions;
        fPositions = NULL;
    }
}
    
LayoutEngine *LayoutEngine::layoutEngineFactory(LEFontInstance *fontInstance, le_int32 scriptCode, le_int32 languageCode)
{
    static le_uint32 gsubTableTag = 0x47535542; // "GSUB"
    static le_uint32 mortTableTag = 0x6D6F7274; // 'mort'
    GlyphSubstitutionTableHeader *gsubTable = (GlyphSubstitutionTableHeader *) fontInstance->getFontTable(gsubTableTag);

    if (gsubTable != NULL && gsubTable->coversScript(OpenTypeLayoutEngine::getScriptTag(scriptCode))) {
        switch (scriptCode) {
        case bengScriptCode:
        case devaScriptCode:
        case gujrScriptCode:
        case kndaScriptCode:
        case mlymScriptCode:
        case oryaScriptCode:
        case punjScriptCode:
        case tamlScriptCode:
        case teluScriptCode:
            return new IndicOpenTypeLayoutEngine(fontInstance, scriptCode, languageCode, gsubTable);

        case arabScriptCode:
            return new ArabicOpenTypeLayoutEngine(fontInstance, scriptCode, languageCode, gsubTable);

        default:
			return new OpenTypeLayoutEngine(fontInstance, scriptCode, languageCode, gsubTable);
            break;
        }
    } else {
        MorphTableHeader *morphTable = (MorphTableHeader *) fontInstance->getFontTable(mortTableTag);

        if (morphTable != NULL) {
            return new GXLayoutEngine(fontInstance, scriptCode, languageCode, morphTable);
        } else {
            switch (scriptCode) {
            case bengScriptCode:
            case devaScriptCode:
            case gujrScriptCode:
            case kndaScriptCode:
            case mlymScriptCode:
            case oryaScriptCode:
            case punjScriptCode:
            case tamlScriptCode:
            case teluScriptCode:
            {
#if 0
                const CDACLayout::ScriptInfo *scriptInfo = CDACLayout::getCDACScriptInfo(fontInstance, scriptCode);

                if (scriptInfo != NULL) {
                    return new CDACOpenTypeLayoutEngine(fontInstance, scriptCode, languageCode, scriptInfo);
                } else {
                    return new IndicOpenTypeLayoutEngine(fontInstance, scriptCode, languageCode);
                }
#else
                return new IndicOpenTypeLayoutEngine(fontInstance, scriptCode, languageCode);
#endif
            }

            case arabScriptCode:
			case hebrScriptCode:
                return new UnicodeArabicOpenTypeLayoutEngine(fontInstance, scriptCode, languageCode);

			//case hebrScriptCode:
			//	return new HebrewOpenTypeLayoutEngine(fontInstance, scriptCode, languageCode);

            case thaiScriptCode:
                return new ThaiLayoutEngine(fontInstance, scriptCode, languageCode);

            default:
                break;
            }
        }
    }

    return new LayoutEngine(fontInstance, scriptCode, languageCode);
}

LayoutEngine::~LayoutEngine() {
    reset();
}

