/*
 *******************************************************************************
 *
 *   Copyright (C) 1999-2003, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *
 *******************************************************************************
 *   file name:  letest.h
 *
 *   created on: 11/06/2000
 *   created by: Eric R. Mader
 */

#include "LETypes.h"

#define ARRAY_LENGTH(array) (sizeof array / sizeof array[0])

struct TestInput
{
    char      *fontName;
    LEUnicode *text;
    le_int32   textLength;
    le_int32   scriptCode;
    le_bool    rightToLeft;
};

extern le_int32 testCount;

extern TestInput testInputs[];

struct TestResult
{
    le_int32   glyphCount;
    LEGlyphID *glyphs;
    le_int32  *indices;
    float     *positions;
};

extern TestResult testResults[];


