/*
 *******************************************************************************
 *
 *   Copyright (C) 1999-2003, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *
 *******************************************************************************
 *   file name:  letest.cpp
 *
 *   created on: 11/06/2000
 *   created by: Eric R. Mader
 */

#include "math.h"

#include "LETypes.h"
#include "LayoutEngine.h"
#include "PortableFontInstance.h"

#include "letest.h"

U_NAMESPACE_USE

le_bool compareResults(le_int32 testNumber, TestResult *expected, TestResult *actual)
{
    /* NOTE: we'll stop on the first failure 'cause once there's one error, it may cascade... */
    if (actual->glyphCount != expected->glyphCount) {
        printf("incorrect glyph count: exptected %d, got %d\n", expected->glyphCount, actual->glyphCount);
        return FALSE;
    }

    le_int32 i;

    for (i = 0; i < actual->glyphCount; i += 1) {
        if (actual->glyphs[i] != expected->glyphs[i]) {
            printf("incorrect id for glyph %d: expected %4X, got %4X\n", i, expected->glyphs[i], actual->glyphs[i]);
            return FALSE;
        }
    }

    for (i = 0; i < actual->glyphCount; i += 1) {
        if (actual->indices[i] != expected->indices[i]) {
            printf("incorrect index for glyph %d: expected %8X, got %8X\n", i, expected->indices[i], actual->indices[i]);
            return FALSE;
        }
    }

    for (i = 0; i <= actual->glyphCount; i += 1) {
        double xError = fabs(actual->positions[i * 2] - expected->positions[i * 2]);

        if (xError > 0.0001) {
            printf("incorrect x position for glyph %d: expected %f, got %f\n", i, expected->positions[i * 2], actual->positions[i * 2]);
            return FALSE;
        }

        double yError = fabs(actual->positions[i * 2 + 1] - expected->positions[i * 2 + 1]);

        if (yError < 0) {
            yError = -yError;
        }

        if (yError > 0.0001) {
            printf("incorrect y position for glyph %d: expected %f, got %f\n", i, expected->positions[i * 2 + 1], actual->positions[i * 2 + 1]);
            return FALSE;
        }
    }

    return TRUE;
}

int main(int argc, char *argv[])
{
    le_int32 failures = 0;

    for (le_int32 test = 0; test < testCount; test += 1) {
        LEErrorCode fontStatus = LE_NO_ERROR;

        printf("Test %d, font = %s... ", test, testInputs[test].fontName);

        PortableFontInstance fontInstance(testInputs[test].fontName, 12, fontStatus);

        if (LE_FAILURE(fontStatus)) {
            printf("could not open font.\n");
            continue;
        }

        LEErrorCode success = LE_NO_ERROR;
        LayoutEngine *engine = LayoutEngine::layoutEngineFactory(&fontInstance, testInputs[test].scriptCode, -1, success);
        le_int32 textLength = testInputs[test].textLength;
        le_bool result;
        TestResult actual;

        if (LE_FAILURE(success)) {
            // would be nice to print the script name here, but
            // don't want to maintain a table, and don't want to
            // require ICU just for the script name...
            printf("could not create a LayoutEngine.\n");
            continue;
        }

        actual.glyphCount = engine->layoutChars(testInputs[test].text, 0, textLength, textLength, testInputs[test].rightToLeft, 0, 0, success);

        actual.glyphs    = new LEGlyphID[actual.glyphCount];
        actual.indices   = new le_int32[actual.glyphCount];
        actual.positions = new float[actual.glyphCount * 2 + 2];

        engine->getGlyphs(actual.glyphs, success);
        engine->getCharIndices(actual.indices, success);
        engine->getGlyphPositions(actual.positions, success);

        result = compareResults(test, &testResults[test], &actual);

        if (result) {
            printf("passed.\n");
        } else {
            failures += 1;
            printf("failed.\n");
        }

        delete[] actual.positions;
        delete[] actual.indices;
        delete[] actual.glyphs;
        delete   engine;
    }

    return failures;
}
