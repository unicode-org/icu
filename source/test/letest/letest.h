/*
 *******************************************************************************
 *
 *   Copyright (C) 1999-2000, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *
 *******************************************************************************
 *   file name:  letest.h
 *
 *   created on: 11/06/2000
 *   created by: Eric R. Mader
 */

#include "unicode/utypes.h"
#include "unicode/unicode.h"

#define ARRAY_LENGTH(array) (sizeof array / sizeof array[0])

struct TestInput
{
	char *fontName;
	UChar *text;
	int32_t textLength;
	Unicode::EUnicodeScript scriptCode;
	UBool rightToLeft;
};

extern int32_t testCount;

extern TestInput testInputs[];

struct TestResult
{
	int32_t glyphCount;
	uint16_t *glyphs;
	int32_t *indices;
	float *positions;
};

extern TestResult testResults[];


