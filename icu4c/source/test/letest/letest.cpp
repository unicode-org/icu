// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 *
 *   Copyright (C) 1999-2014, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *
 *******************************************************************************
 *   file name:  letest.cpp
 *
 *   created on: 11/06/2000
 *   created by: Eric R. Mader
 */

#include "unicode/utypes.h"
#include "unicode/uclean.h"
#include "unicode/uchar.h"
#include "unicode/uscript.h"
#include "unicode/putil.h"
#include "unicode/ctest.h"

#include "layout/LETypes.h"
#include "layout/LEScripts.h"
#include "layout/LayoutEngine.h"

#include "layout/ParagraphLayout.h"
#include "layout/RunArrays.h"

#include "SimpleFontInstance.h"

#include "letest.h"

#include "putilimp.h" // for uprv_getUTCtime()

#include <stdlib.h>
#include <string.h>

U_NAMESPACE_USE

U_CDECL_BEGIN

static void U_CALLCONV ParamTest()
{
    LEErrorCode status = LE_NO_ERROR;
    SimpleFontInstance *font = new SimpleFontInstance(12, status);
    LayoutEngine *engine = LayoutEngine::layoutEngineFactory(font, arabScriptCode, -1, status);
    LEGlyphID *glyphs    = nullptr;
    le_int32  *indices   = nullptr;
    float     *positions = nullptr;
    le_int32   glyphCount = 0;

    glyphCount = engine->getGlyphCount();
    if (glyphCount != 0) {
        log_err("Calling getGlyphCount() on an empty layout returned %d.\n", glyphCount);
    }

    glyphs    = NEW_ARRAY(LEGlyphID, glyphCount + 10);
    indices   = NEW_ARRAY(le_int32, glyphCount + 10);
    positions = NEW_ARRAY(float, glyphCount + 10);

    engine->getGlyphs(nullptr, status);

    if (status != LE_ILLEGAL_ARGUMENT_ERROR) {
        log_err("Calling getGlyphs(nullptr, status) did not return LE_ILLEGAL_ARGUMENT_ERROR.\n");
    }

    status = LE_NO_ERROR;
    engine->getGlyphs(glyphs, status);

    if (status != LE_NO_LAYOUT_ERROR) {
        log_err("Calling getGlyphs(glyphs, status) on an empty layout did not return LE_NO_LAYOUT_ERROR.\n");
    }

    status = LE_NO_ERROR;
    engine->getGlyphs(nullptr, 0xFF000000L, status);

    if (status != LE_ILLEGAL_ARGUMENT_ERROR) {
        log_err("Calling getGlyphs(nullptr, 0xFF000000L, status) did not return LE_ILLEGAL_ARGUMENT_ERROR.\n");
    }

    status = LE_NO_ERROR;
    engine->getGlyphs(glyphs, 0xFF000000L, status);

    if (status != LE_NO_LAYOUT_ERROR) {
        log_err("Calling getGlyphs(glyphs, 0xFF000000L, status) on an empty layout did not return LE_NO_LAYOUT_ERROR.\n");
    }

    status = LE_NO_ERROR;
    engine->getCharIndices(nullptr, status);

    if (status != LE_ILLEGAL_ARGUMENT_ERROR) {
        log_err("Calling getCharIndices(nullptr, status) did not return LE_ILLEGAL_ARGUMENT_ERROR.\n");
    }

    status = LE_NO_ERROR;
    engine->getCharIndices(indices, status);

    if (status != LE_NO_LAYOUT_ERROR) {
        log_err("Calling getCharIndices(indices, status) on an empty layout did not return LE_NO_LAYOUT_ERROR.\n");
    }

    status = LE_NO_ERROR;
    engine->getCharIndices(nullptr, 1024, status);

    if (status != LE_ILLEGAL_ARGUMENT_ERROR) {
        log_err("Calling getCharIndices(nullptr, 1024, status) did not return LE_ILLEGAL_ARGUMENT_ERROR.\n");
    }

    status = LE_NO_ERROR;
    engine->getCharIndices(indices, 1024, status);

    if (status != LE_NO_LAYOUT_ERROR) {
        log_err("Calling getCharIndices(indices, 1024, status) on an empty layout did not return LE_NO_LAYOUT_ERROR.\n");
    }

    status = LE_NO_ERROR;
    engine->getGlyphPositions(nullptr, status);

    if (status != LE_ILLEGAL_ARGUMENT_ERROR) {
        log_err("Calling getGlyphPositions(nullptr, status) did not return LE_ILLEGAL_ARGUMENT_ERROR.\n");
    }

    status = LE_NO_ERROR;
    engine->getGlyphPositions(positions, status);

    if (status != LE_NO_LAYOUT_ERROR) {
        log_err("Calling getGlyphPositions(positions, status) on an empty layout did not return LE_NO_LAYOUT_ERROR.\n");
    }

    DELETE_ARRAY(positions);
    DELETE_ARRAY(indices);
    DELETE_ARRAY(glyphs);

    status = LE_NO_ERROR;
    glyphCount = engine->layoutChars(nullptr, 0, 0, 0, false, 0.0, 0.0, status);

    if (status != LE_ILLEGAL_ARGUMENT_ERROR) {
        log_err("Calling layoutChars(nullptr, 0, 0, 0, false, 0.0, 0.0, status) did not fail w/ LE_ILLEGAL_ARGUMENT_ERROR.\n");
    }

    LEUnicode chars[] = {
        0x0045, 0x006E, 0x0067, 0x006C, 0x0069, 0x0073, 0x0068, 0x0020, // "English "
        0x0645, 0x0627, 0x0646, 0x062A, 0x0648, 0x0634,                 // MEM ALIF KAF NOON TEH WAW SHEEN
        0x0020, 0x0074, 0x0065, 0x0078, 0x0074, 0x02E                   // " text."
    };

    status = LE_NO_ERROR;
    glyphCount = engine->layoutChars(chars, -1, 6, 20, true, 0.0, 0.0, status);

    if (status != LE_ILLEGAL_ARGUMENT_ERROR) {
        log_err("Calling layoutChars(chars, -1, 6, 20, true, 0.0, 0.0, status) did not fail w/ LE_ILLEGAL_ARGUMENT_ERROR.\n");
    }

    status = LE_NO_ERROR;
    glyphCount = engine->layoutChars(chars, 8, -1, 20, true, 0.0, 0.0, status);

    if (status != LE_ILLEGAL_ARGUMENT_ERROR) {
        log_err("Calling layoutChars(chars, 8, -1, 20, true, 0.0, 0.0, status) did not fail w/ LE_ILLEGAL_ARGUMENT_ERROR.\n");
    }

    status = LE_NO_ERROR;
    glyphCount = engine->layoutChars(chars, 8, 6, -1, true, 0.0, 0.0, status);

    if (status != LE_ILLEGAL_ARGUMENT_ERROR) {
        log_err("Calling layoutChars((chars, 8, 6, -1, true, 0.0, 0.0, status) did not fail w/ LE_ILLEGAL_ARGUMENT_ERROR.\n");
    }

    status = LE_NO_ERROR;
    glyphCount = engine->layoutChars(chars, 8, 6, 10, true, 0.0, 0.0, status);

    if (status != LE_ILLEGAL_ARGUMENT_ERROR) {
        log_err("Calling layoutChars(chars, 8, 6, 10, true, 0.0, 0.0, status) did not fail w/ LE_ILLEGAL_ARGUMENT_ERROR.\n");
    }

    float x = 0.0, y = 0.0;

    status = LE_NO_ERROR;
    glyphCount = engine->layoutChars(chars, 8, 6, 20, true, 0.0, 0.0, status);

    if (LE_FAILURE(status)) {
        log_err("Calling layoutChars(chars, 8, 6, 20, true, 0.0, 0.0, status) failed.\n");
        goto bail;
    }

    engine->getGlyphPosition(-1, x, y, status);

    if (status != LE_INDEX_OUT_OF_BOUNDS_ERROR) {
        log_err("Calling getGlyphPosition(-1, x, y, status) did not fail w/ LE_INDEX_OUT_OF_BOUNDS_ERROR.\n");
    }

    status = LE_NO_ERROR;
    engine->getGlyphPosition(glyphCount + 1, x, y, status);

    if (status != LE_INDEX_OUT_OF_BOUNDS_ERROR) {
        log_err("Calling getGlyphPosition(glyphCount + 1, x, y, status) did not fail w/ LE_INDEX_OUT_OF_BOUNDS_ERROR.\n");
    }

bail:
    delete engine;
    delete font;
}
U_CDECL_END

U_CDECL_BEGIN
static void U_CALLCONV FactoryTest()
{
    LEErrorCode status = LE_NO_ERROR;
    SimpleFontInstance *font = new SimpleFontInstance(12, status);
    LayoutEngine *engine = nullptr;

    for(le_int32 scriptCode = 0; scriptCode < scriptCodeCount; scriptCode += 1) {
        status = LE_NO_ERROR;
        engine = LayoutEngine::layoutEngineFactory(font, scriptCode, -1, status);

        if (LE_FAILURE(status)) {
            log_err("Could not create a LayoutEngine for script \'%s\'.\n", uscript_getShortName((UScriptCode)scriptCode));
        }

        delete engine;
    }

    delete font;
}
U_CDECL_END

U_CDECL_BEGIN
static void U_CALLCONV AccessTest()
{
    LEErrorCode status = LE_NO_ERROR;
    SimpleFontInstance *font = new SimpleFontInstance(12, status);
    LayoutEngine *engine = LayoutEngine::layoutEngineFactory(font, arabScriptCode, -1, status);
    le_int32 glyphCount;
    LEGlyphID glyphs[6], extraBitGlyphs[6];
    le_int32 biasedIndices[6], indices[6], glyph;
    float positions[6 * 2 + 2];
    LEUnicode chars[] = {
        0x0045, 0x006E, 0x0067, 0x006C, 0x0069, 0x0073, 0x0068, 0x0020, // "English "
        0x0645, 0x0627, 0x0646, 0x062A, 0x0648, 0x0634,                 // MEM ALIF KAF NOON TEH WAW SHEEN
        0x0020, 0x0074, 0x0065, 0x0078, 0x0074, 0x02E                   // " text."
    };

    if (LE_FAILURE(status)) {
        log_err("Could not create LayoutEngine.\n");
        goto bail;
    }

    glyphCount = engine->layoutChars(chars, 8, 6, 20, true, 0.0, 0.0, status);

    if (LE_FAILURE(status) || glyphCount != 6) {
        log_err("layoutChars(chars, 8, 6, 20, true, 0.0, 0.0, status) failed.\n");
        goto bail;
    }

    engine->getGlyphs(glyphs, status);
    engine->getCharIndices(indices, status);
    engine->getGlyphPositions(positions, status);

    if (LE_FAILURE(status)) {
        log_err("Could not get glyph, indices and position arrays.\n");
        goto bail;
    }

    engine->getGlyphs(extraBitGlyphs, 0xFF000000L, status);

    if (LE_FAILURE(status)) {
        log_err("getGlyphs(extraBitGlyphs, 0xFF000000L, status); failed.\n");
    } else {
        for(glyph = 0; glyph < glyphCount; glyph += 1) {
            if (extraBitGlyphs[glyph] != (glyphs[glyph] | 0xFF000000L)) {
                log_err("extraBigGlyphs[%d] != glyphs[%d] | 0xFF000000L: %8X, %8X\n",
                    glyph, glyph, extraBitGlyphs[glyph], glyphs[glyph]);
                break;
            }
        }
    }

    status = LE_NO_ERROR;
    engine->getCharIndices(biasedIndices, 1024, status);

    if (LE_FAILURE(status)) {
        log_err("getCharIndices(biasedIndices, 1024, status) failed.\n");
    } else {
        for (glyph = 0; glyph < glyphCount; glyph += 1) {
            if (biasedIndices[glyph] != (indices[glyph] + 1024)) {
                log_err("biasedIndices[%d] != indices[%d] + 1024: %8X, %8X\n",
                    glyph, glyph, biasedIndices[glyph], indices[glyph]);
                break;
            }
        }
    }

    status = LE_NO_ERROR;
    for (glyph = 0; glyph <= glyphCount; glyph += 1) {
        float x = 0.0, y = 0.0;

        engine->getGlyphPosition(glyph, x, y, status);

        if (LE_FAILURE(status)) {
            log_err("getGlyphPosition(%d, x, y, status) failed.\n", glyph);
            break;
        }

        if (x != positions[glyph*2] || y != positions[glyph*2 + 1]) {
            log_err("getGlyphPosition(%d, x, y, status) returned bad position: (%f, %f) != (%f, %f)\n",
                glyph, x, y, positions[glyph*2], positions[glyph*2 + 1]);
            break;
        }
    }

bail:
    delete engine;
    delete font;
}
U_CDECL_END

U_CDECL_BEGIN
/*
 * From ticket:5923:
 *
 * Build a paragraph that contains a mixture of left to right and right to left text.
 * Break it into multiple lines and make sure that the glyphToCharMap for run in each
 * line is correct.
 *
 * Note: it might be a good idea to also check the glyphs and positions for each run,
 * that we get the expected number of runs per line and that the line breaks are where
 * we expect them to be. Really, it would be a good idea to make a whole test suite
 * for ParagraphLayout.
 */
static void U_CALLCONV GlyphToCharTest()
{
#if !UCONFIG_NO_BREAK_ITERATION
    LEErrorCode status = LE_NO_ERROR;
    LEFontInstance *font;
    FontRuns fontRuns(0);
    ParagraphLayout *paragraphLayout;
    const ParagraphLayout::Line *line;
    /*
     * This is the same text that's in <icu>/source/samples/layout/Sample.txt
     */
    LEUnicode chars[] = {
        /*BOM*/ 0x0054, 0x0068, 0x0065, 0x0020, 0x004c, 0x0061, 0x0079, 
        0x006f, 0x0075, 0x0074, 0x0045, 0x006e, 0x0067, 0x0069, 0x006e, 
        0x0065, 0x0020, 0x0064, 0x006f, 0x0065, 0x0073, 0x0020, 0x0061, 
        0x006c, 0x006c, 0x0020, 0x0074, 0x0068, 0x0065, 0x0020, 0x0077, 
        0x006f, 0x0072, 0x006b, 0x0020, 0x006e, 0x0065, 0x0063, 0x0065, 
        0x0073, 0x0073, 0x0061, 0x0072, 0x0079, 0x0020, 0x0074, 0x006f, 
        0x0020, 0x0064, 0x0069, 0x0073, 0x0070, 0x006c, 0x0061, 0x0079, 
        0x0020, 0x0055, 0x006e, 0x0069, 0x0063, 0x006f, 0x0064, 0x0065, 
        0x0020, 0x0074, 0x0065, 0x0078, 0x0074, 0x0020, 0x0077, 0x0072, 
        0x0069, 0x0074, 0x0074, 0x0065, 0x006e, 0x0020, 0x0069, 0x006e, 
        0x0020, 0x006c, 0x0061, 0x006e, 0x0067, 0x0075, 0x0061, 0x0067, 
        0x0065, 0x0073, 0x0020, 0x0077, 0x0069, 0x0074, 0x0068, 0x0020, 
        0x0063, 0x006f, 0x006d, 0x0070, 0x006c, 0x0065, 0x0078, 0x0020, 
        0x0077, 0x0072, 0x0069, 0x0074, 0x0069, 0x006e, 0x0067, 0x0020, 
        0x0073, 0x0079, 0x0073, 0x0074, 0x0065, 0x006d, 0x0073, 0x0020, 
        0x0073, 0x0075, 0x0063, 0x0068, 0x0020, 0x0061, 0x0073, 0x0020, 
        0x0048, 0x0069, 0x006e, 0x0064, 0x0069, 0x0020, 0x0028, 0x0939, 
        0x093f, 0x0928, 0x094d, 0x0926, 0x0940, 0x0029, 0x0020, 0x0054, 
        0x0068, 0x0061, 0x0069, 0x0020, 0x0028, 0x0e44, 0x0e17, 0x0e22, 
        0x0029, 0x0020, 0x0061, 0x006e, 0x0064, 0x0020, 0x0041, 0x0072, 
        0x0061, 0x0062, 0x0069, 0x0063, 0x0020, 0x0028, 0x0627, 0x0644, 
        0x0639, 0x0631, 0x0628, 0x064a, 0x0629, 0x0029, 0x002e, 0x0020, 
        0x0048, 0x0065, 0x0072, 0x0065, 0x0027, 0x0073, 0x0020, 0x0061, 
        0x0020, 0x0073, 0x0061, 0x006d, 0x0070, 0x006c, 0x0065, 0x0020, 
        0x006f, 0x0066, 0x0020, 0x0073, 0x006f, 0x006d, 0x0065, 0x0020, 
        0x0074, 0x0065, 0x0078, 0x0074, 0x0020, 0x0077, 0x0072, 0x0069, 
        0x0074, 0x0074, 0x0065, 0x006e, 0x0020, 0x0069, 0x006e, 0x0020, 
        0x0053, 0x0061, 0x006e, 0x0073, 0x006b, 0x0072, 0x0069, 0x0074, 
        0x003a, 0x0020, 0x0936, 0x094d, 0x0930, 0x0940, 0x092e, 0x0926, 
        0x094d, 0x0020, 0x092d, 0x0917, 0x0935, 0x0926, 0x094d, 0x0917, 
        0x0940, 0x0924, 0x093e, 0x0020, 0x0905, 0x0927, 0x094d, 0x092f, 
        0x093e, 0x092f, 0x0020, 0x0905, 0x0930, 0x094d, 0x091c, 0x0941, 
        0x0928, 0x0020, 0x0935, 0x093f, 0x0937, 0x093e, 0x0926, 0x0020, 
        0x092f, 0x094b, 0x0917, 0x0020, 0x0927, 0x0943, 0x0924, 0x0930, 
        0x093e, 0x0937, 0x094d, 0x091f, 0x094d, 0x0930, 0x0020, 0x0909, 
        0x0935, 0x093e, 0x091a, 0x0964, 0x0020, 0x0927, 0x0930, 0x094d, 
        0x092e, 0x0915, 0x094d, 0x0937, 0x0947, 0x0924, 0x094d, 0x0930, 
        0x0947, 0x0020, 0x0915, 0x0941, 0x0930, 0x0941, 0x0915, 0x094d, 
        0x0937, 0x0947, 0x0924, 0x094d, 0x0930, 0x0947, 0x0020, 0x0938, 
        0x092e, 0x0935, 0x0947, 0x0924, 0x093e, 0x0020, 0x092f, 0x0941, 
        0x092f, 0x0941, 0x0924, 0x094d, 0x0938, 0x0935, 0x0903, 0x0020, 
        0x092e, 0x093e, 0x092e, 0x0915, 0x093e, 0x0903, 0x0020, 0x092a, 
        0x093e, 0x0923, 0x094d, 0x0921, 0x0935, 0x093e, 0x0936, 0x094d, 
        0x091a, 0x0948, 0x0935, 0x0020, 0x0915, 0x093f, 0x092e, 0x0915, 
        0x0941, 0x0930, 0x094d, 0x0935, 0x0924, 0x0020, 0x0938, 0x0902, 
        0x091c, 0x092f, 0x0020, 0x0048, 0x0065, 0x0072, 0x0065, 0x0027, 
        0x0073, 0x0020, 0x0061, 0x0020, 0x0073, 0x0061, 0x006d, 0x0070, 
        0x006c, 0x0065, 0x0020, 0x006f, 0x0066, 0x0020, 0x0073, 0x006f, 
        0x006d, 0x0065, 0x0020, 0x0074, 0x0065, 0x0078, 0x0074, 0x0020, 
        0x0077, 0x0072, 0x0069, 0x0074, 0x0074, 0x0065, 0x006e, 0x0020, 
        0x0069, 0x006e, 0x0020, 0x0041, 0x0072, 0x0061, 0x0062, 0x0069, 
        0x0063, 0x003a, 0x0020, 0x0623, 0x0633, 0x0627, 0x0633, 0x064b, 
        0x0627, 0x060c, 0x0020, 0x062a, 0x062a, 0x0639, 0x0627, 0x0645, 
        0x0644, 0x0020, 0x0627, 0x0644, 0x062d, 0x0648, 0x0627, 0x0633, 
        0x064a, 0x0628, 0x0020, 0x0641, 0x0642, 0x0637, 0x0020, 0x0645, 
        0x0639, 0x0020, 0x0627, 0x0644, 0x0623, 0x0631, 0x0642, 0x0627, 
        0x0645, 0x060c, 0x0020, 0x0648, 0x062a, 0x0642, 0x0648, 0x0645, 
        0x0020, 0x0628, 0x062a, 0x062e, 0x0632, 0x064a, 0x0646, 0x0020, 
        0x0627, 0x0644, 0x0623, 0x062d, 0x0631, 0x0641, 0x0020, 0x0648, 
        0x0627, 0x0644, 0x0645, 0x062d, 0x0627, 0x0631, 0x0641, 0x0020, 
        0x0627, 0x0644, 0x0623, 0x062e, 0x0631, 0x0649, 0x0020, 0x0628, 
        0x0639, 0x062f, 0x0020, 0x0623, 0x0646, 0x0020, 0x062a, 0x064f, 
        0x0639, 0x0637, 0x064a, 0x0020, 0x0631, 0x0642, 0x0645, 0x0627, 
        0x0020, 0x0645, 0x0639, 0x064a, 0x0646, 0x0627, 0x0020, 0x0644, 
        0x0643, 0x0644, 0x0020, 0x0648, 0x0627, 0x062d, 0x062f, 0x0020, 
        0x0645, 0x0646, 0x0647, 0x0627, 0x002e, 0x0020, 0x0648, 0x0642, 
        0x0628, 0x0644, 0x0020, 0x0627, 0x062e, 0x062a, 0x0631, 0x0627, 
        0x0639, 0x0020, 0x0022, 0x064a, 0x0648, 0x0646, 0x0650, 0x0643, 
        0x0648, 0x062f, 0x0022, 0x060c, 0x0020, 0x0643, 0x0627, 0x0646, 
        0x0020, 0x0647, 0x0646, 0x0627, 0x0643, 0x0020, 0x0645, 0x0626, 
        0x0627, 0x062a, 0x0020, 0x0627, 0x0644, 0x0623, 0x0646, 0x0638, 
        0x0645, 0x0629, 0x0020, 0x0644, 0x0644, 0x062a, 0x0634, 0x0641, 
        0x064a, 0x0631, 0x0020, 0x0648, 0x062a, 0x062e, 0x0635, 0x064a, 
        0x0635, 0x0020, 0x0647, 0x0630, 0x0647, 0x0020, 0x0627, 0x0644, 
        0x0623, 0x0631, 0x0642, 0x0627, 0x0645, 0x0020, 0x0644, 0x0644, 
        0x0645, 0x062d, 0x0627, 0x0631, 0x0641, 0x060c, 0x0020, 0x0648, 
        0x0644, 0x0645, 0x0020, 0x064a, 0x0648, 0x062c, 0x062f, 0x0020, 
        0x0646, 0x0638, 0x0627, 0x0645, 0x0020, 0x062a, 0x0634, 0x0641, 
        0x064a, 0x0631, 0x0020, 0x0648, 0x0627, 0x062d, 0x062f, 0x0020, 
        0x064a, 0x062d, 0x062a, 0x0648, 0x064a, 0x0020, 0x0639, 0x0644, 
        0x0649, 0x0020, 0x062c, 0x0645, 0x064a, 0x0639, 0x0020, 0x0627, 
        0x0644, 0x0645, 0x062d, 0x0627, 0x0631, 0x0641, 0x0020, 0x0627, 
        0x0644, 0x0636, 0x0631, 0x0648, 0x0631, 0x064a, 0x0629, 0x0020, 
        0x0061, 0x006e, 0x0064, 0x0020, 0x0068, 0x0065, 0x0072, 0x0065, 
        0x0027, 0x0073, 0x0020, 0x0061, 0x0020, 0x0073, 0x0061, 0x006d, 
        0x0070, 0x006c, 0x0065, 0x0020, 0x006f, 0x0066, 0x0020, 0x0073, 
        0x006f, 0x006d, 0x0065, 0x0020, 0x0074, 0x0065, 0x0078, 0x0074, 
        0x0020, 0x0077, 0x0072, 0x0069, 0x0074, 0x0074, 0x0065, 0x006e, 
        0x0020, 0x0069, 0x006e, 0x0020, 0x0054, 0x0068, 0x0061, 0x0069, 
        0x003a, 0x0020, 0x0e1a, 0x0e17, 0x0e17, 0x0e35, 0x0e48, 0x0e51, 
        0x0e1e, 0x0e32, 0x0e22, 0x0e38, 0x0e44, 0x0e0b, 0x0e42, 0x0e04, 
        0x0e25, 0x0e19, 0x0e42, 0x0e14, 0x0e42, 0x0e23, 0x0e18, 0x0e35, 
        0x0e2d, 0x0e32, 0x0e28, 0x0e31, 0x0e22, 0x0e2d, 0x0e22, 0x0e39, 
        0x0e48, 0x0e17, 0x0e48, 0x0e32, 0x0e21, 0x0e01, 0x0e25, 0x0e32, 
        0x0e07, 0x0e17, 0x0e38, 0x0e48, 0x0e07, 0x0e43, 0x0e2b, 0x0e0d, 
        0x0e48, 0x0e43, 0x0e19, 0x0e41, 0x0e04, 0x0e19, 0x0e0b, 0x0e31, 
        0x0e2a, 0x0e01, 0x0e31, 0x0e1a, 0x0e25, 0x0e38, 0x0e07, 0x0e40, 
        0x0e2e, 0x0e19, 0x0e23, 0x0e35, 0x0e0a, 0x0e32, 0x0e27, 0x0e44, 
        0x0e23, 0x0e48, 0x0e41, 0x0e25, 0x0e30, 0x0e1b, 0x0e49, 0x0e32, 
        0x0e40, 0x0e2d, 0x0e47, 0x0e21, 0x0e20, 0x0e23, 0x0e23, 0x0e22, 
        0x0e32, 0x0e0a, 0x0e32, 0x0e27, 0x0e44, 0x0e23, 0x0e48, 0x0e1a, 
        0x0e49, 0x0e32, 0x0e19, 0x0e02, 0x0e2d, 0x0e07, 0x0e1e, 0x0e27, 
        0x0e01, 0x0e40, 0x0e02, 0x0e32, 0x0e2b, 0x0e25, 0x0e31, 0x0e07, 
        0x0e40, 0x0e25, 0x0e47, 0x0e01, 0x0e40, 0x0e1e, 0x0e23, 0x0e32, 
        0x0e30, 0x0e44, 0x0e21, 0x0e49, 0x0e2a, 0x0e23, 0x0e49, 0x0e32, 
        0x0e07, 0x0e1a, 0x0e49, 0x0e32, 0x0e19, 0x0e15, 0x0e49, 0x0e2d, 
        0x0e07, 0x0e02, 0x0e19, 0x0e21, 0x0e32, 0x0e14, 0x0e49, 0x0e27, 
        0x0e22, 0x0e40, 0x0e01, 0x0e27, 0x0e35, 0x0e22, 0x0e19, 0x0e40, 
        0x0e1b, 0x0e47, 0x0e19, 0x0e23, 0x0e30, 0x0e22, 0x0e30, 0x0e17, 
        0x0e32, 0x0e07, 0x0e2b, 0x0e25, 0x0e32, 0x0e22, 0x0e44, 0x0e21, 
        0x0e25, 0x0e4c
    };
    le_int32 charCount = LE_ARRAY_SIZE(chars);
    le_int32 charIndex = 0, lineNumber = 1;
    const float lineWidth = 600;

    font = new SimpleFontInstance(12, status);

    if (LE_FAILURE(status)) {
        goto finish;
    }

    fontRuns.add(font, charCount);

    paragraphLayout = new ParagraphLayout(chars, charCount, &fontRuns, nullptr, nullptr, nullptr, 0, false, status);

    if (LE_FAILURE(status)) {
        goto close_font;
    }

    paragraphLayout->reflow();
    while ((line = paragraphLayout->nextLine(lineWidth)) != nullptr) {
        le_int32 runCount = line->countRuns();

        for(le_int32 run = 0; run < runCount; run += 1) {
            const ParagraphLayout::VisualRun *visualRun = line->getVisualRun(run);
            le_int32 glyphCount = visualRun->getGlyphCount();
            const le_int32 *glyphToCharMap = visualRun->getGlyphToCharMap();

            if (visualRun->getDirection() == UBIDI_RTL) {
                /*
                 * For a right to left run, make sure that the character indices
                 * increase from the right most glyph to the left most glyph. If
                 * there are any one to many glyph substitutions, we might get several
                 * glyphs in a row with the same character index.
                 */
                for(le_int32 i = glyphCount - 1; i >= 0; i -= 1) {
                    le_int32 ix = glyphToCharMap[i];

                    if (ix != charIndex) {
                        if (ix != charIndex - 1) {
                            log_err("Bad glyph to char index for glyph %d on line %d: expected %d, got %d\n",
                                i, lineNumber, charIndex, ix);
                            goto close_paragraph; // once there's one error, we can't count on anything else...
                        }
                    } else {
                        charIndex += 1;
                    }
                }
            } else {
                /*
                 * We can't just check the order of the character indices
                 * for left to right runs because Indic text might have been
                 * reordered. What we can do is find the minimum and maximum
                 * character indices in the run and make sure that the minimum
                 * is equal to charIndex and then advance charIndex to the maximum.
                 */
                le_int32 minIndex = 0x7FFFFFFF, maxIndex = -1;

                for(le_int32 i = 0; i < glyphCount; i += 1) {
                    le_int32 ix = glyphToCharMap[i];

                    if (ix > maxIndex) {
                        maxIndex = ix;
                    }

                    if (ix < minIndex) {
                        minIndex = ix;
                    }
                }

                if (minIndex != charIndex) {
                    log_err("Bad minIndex for run %d on line %d: expected %d, got %d\n",
                        run, lineNumber, charIndex, minIndex);
                    goto close_paragraph; // once there's one error, we can't count on anything else...
                }

                charIndex = maxIndex + 1;
            }
        }

        lineNumber += 1;
    }
close_paragraph:
    delete paragraphLayout;

close_font:
    delete font;

finish:
    return;
#endif
}
U_CDECL_END

static void addAllTests(TestNode **root)
{
    addTest(root, &ParamTest,       "api/ParameterTest");
    addTest(root, &FactoryTest,     "api/FactoryTest");
    addTest(root, &AccessTest,      "layout/AccessTest");
    addTest(root, &GlyphToCharTest, "paragraph/GlyphToCharTest");
}

/* returns the path to icu/source/data/out */
static const char *ctest_dataOutDir()
{
    static const char *dataOutDir = nullptr;

    if(dataOutDir) {
        return dataOutDir;
    }

    /* U_TOPBUILDDIR is set by the makefiles on UNIXes when building cintltst and intltst
    //              to point to the top of the build hierarchy, which may or
    //              may not be the same as the source directory, depending on
    //              the configure options used.  At any rate,
    //              set the data path to the built data from this directory.
    //              The value is complete with quotes, so it can be used
    //              as-is as a string constant.
    */
#if defined (U_TOPBUILDDIR)
    {
        dataOutDir = U_TOPBUILDDIR "data" U_FILE_SEP_STRING "out" U_FILE_SEP_STRING;
    }
#else

    /* On Windows, the file name obtained from __FILE__ includes a full path.
     *             This file is "wherever\icu\source\test\cintltst\cintltst.c"
     *             Change to    "wherever\icu\source\data"
     */
    {
        static char p[sizeof(__FILE__) + 20];
        char *pBackSlash;
        int i;

        strcpy(p, __FILE__);
        /* We want to back over three '\' chars.                            */
        /*   Only Windows should end up here, so looking for '\' is safe.   */
        for (i=1; i<=3; i++) {
            pBackSlash = strrchr(p, U_FILE_SEP_CHAR);
            if (pBackSlash != nullptr) {
                *pBackSlash = 0;        /* Truncate the string at the '\'   */
            }
        }

        if (pBackSlash != nullptr) {
            /* We found and truncated three names from the path.
             *  Now append "source\data" and set the environment
             */
            strcpy(pBackSlash, U_FILE_SEP_STRING "data" U_FILE_SEP_STRING "out" U_FILE_SEP_STRING);
            dataOutDir = p;
        }
        else {
            /* __FILE__ on MSVC7 does not contain the directory */
            FILE *file = fopen(".." U_FILE_SEP_STRING ".." U_FILE_SEP_STRING "data" U_FILE_SEP_STRING "Makefile.in", "r");
            if (file) {
                fclose(file);
                dataOutDir = ".." U_FILE_SEP_STRING ".." U_FILE_SEP_STRING "data" U_FILE_SEP_STRING "out" U_FILE_SEP_STRING;
            }
            else {
                dataOutDir = ".." U_FILE_SEP_STRING".." U_FILE_SEP_STRING".." U_FILE_SEP_STRING "data" U_FILE_SEP_STRING "out" U_FILE_SEP_STRING;
            }
        }
    }
#endif

    return dataOutDir;
}

/*  ctest_setICU_DATA  - if the ICU_DATA environment variable is not already
 *                       set, try to deduce the directory in which ICU was built,
 *                       and set ICU_DATA to "icu/source/data" in that location.
 *                       The intent is to allow the tests to have a good chance
 *                       of running without requiring that the user manually set
 *                       ICU_DATA.  Common data isn't a problem, since it is
 *                       picked up via a static (build time) reference, but the
 *                       tests dynamically load some data.
 */
static void ctest_setICU_DATA() {

    /* No location for the data dir was identifiable.
     *   Add other fallbacks for the test data location here if the need arises
     */
    if (getenv("ICU_DATA") == nullptr) {
        /* If ICU_DATA isn't set, set it to the usual location */
        u_setDataDirectory(ctest_dataOutDir());
    }
}

int main(int argc, char* argv[])
{
    int32_t nerrors = 0;
    TestNode *root = nullptr;
    UErrorCode errorCode = U_ZERO_ERROR;
    UDate startTime, endTime;
    int32_t diffTime;

    startTime = uprv_getUTCtime();

    if (!initArgs(argc, argv, nullptr, nullptr)) {
        /* Error already displayed. */
        return -1;
    }

    /* Check whether ICU will initialize without forcing the build data directory into
    *  the ICU_DATA path.  Success here means either the data dll contains data, or that
    *  this test program was run with ICU_DATA set externally.  Failure of this check
    *  is normal when ICU data is not packaged into a shared library.
    *
    *  Whether or not this test succeeds, we want to cleanup and reinitialize
    *  with a data path so that data loading from individual files can be tested.
    */
    u_init(&errorCode);

    if (U_FAILURE(errorCode)) {
        fprintf(stderr,
            "#### Note:  ICU Init without build-specific setDataDirectory() failed.\n");
    }

    u_cleanup();
    errorCode = U_ZERO_ERROR;

    if (!initArgs(argc, argv, nullptr, nullptr)) {
        /* Error already displayed. */
        return -1;
    }
/* Initialize ICU */
    ctest_setICU_DATA();    /* u_setDataDirectory() must happen Before u_init() */
    u_init(&errorCode);

    if (U_FAILURE(errorCode)) {
        fprintf(stderr,
            "#### ERROR! %s: u_init() failed with status = \"%s\".\n" 
            "*** Check the ICU_DATA environment variable and \n"
            "*** check that the data files are present.\n", argv[0], u_errorName(errorCode));
        return 1;
    }

    addAllTests(&root);
    nerrors = runTestRequest(root, argc, argv);

    cleanUpTestTree(root);
    u_cleanup();

    endTime = uprv_getUTCtime();
    diffTime = static_cast<int32_t>(endTime - startTime);
    printf("Elapsed Time: %02d:%02d:%02d.%03d\n",
        (diffTime % U_MILLIS_PER_DAY) / U_MILLIS_PER_HOUR,
        (diffTime % U_MILLIS_PER_HOUR) / U_MILLIS_PER_MINUTE,
        (diffTime % U_MILLIS_PER_MINUTE) / U_MILLIS_PER_SECOND,
        diffTime % U_MILLIS_PER_SECOND);

    return nerrors;
}

