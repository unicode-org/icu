/*
 *
 *   Copyright (C) 1996-1999, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *
 * @version	1.0 12/12/96
 * @author	Helena Shih
 * Based on Taligent international support for C++
 */

#include <stdio.h>
#include <stdlib.h>
#include <iostream.h>
#include <string.h>
#include <assert.h>

#ifndef _CHARTBLD
#include "chartbld.h"
#endif

const char CharTypeBuilder::tagStrings[] = "MnMcMeNdNlNoZsZlZpCcCfCsCoCnLuLlLtLmLoPcPdPsPePoSmScSkSoPiPf";
const int16_t CharTypeBuilder::tagValues[] =
	{
	/* Mn */ (int16_t)NON_SPACING_MARK,
	/* Mc */ (int16_t)COMBINING_SPACING_MARK,
	/* Me */ (int16_t)ENCLOSING_MARK,
	/* Nd */ (int16_t)DECIMAL_DIGIT_NUMBER,
	/* Nl */ (int16_t)LETTER_NUMBER,
	/* No */ (int16_t)OTHER_NUMBER,
	/* Zs */ (int16_t)SPACE_SEPARATOR,
	/* Zl */ (int16_t)LINE_SEPARATOR,
	/* Zp */ (int16_t)PARAGRAPH_SEPARATOR,
	/* Cc */ (int16_t)CONTROL,
	/* Cf */ (int16_t)FORMAT,
	/* Cs */ (int16_t)SURROGATE,
	/* Co */ (int16_t)PRIVATE_USE,
	/* Cn */ (int16_t)UNASSIGNED,
	/* Lu */ (int16_t)UPPERCASE_LETTER,
	/* Ll */ (int16_t)LOWERCASE_LETTER,
	/* Lt */ (int16_t)TITLECASE_LETTER,
	/* Lm */ (int16_t)MODIFIER_LETTER,
	/* Lo */ (int16_t)OTHER_LETTER,
	/* Pc */ (int16_t)CONNECTOR_PUNCTUATION,
	/* Pd */ (int16_t)DASH_PUNCTUATION,
	/* Ps */ (int16_t)START_PUNCTUATION,
	/* Pe */ (int16_t)END_PUNCTUATION,
	/* Po */ (int16_t)OTHER_PUNCTUATION,
	/* Sm */ (int16_t)MATH_SYMBOL,
	/* Sc */ (int16_t)CURRENCY_SYMBOL,
	/* Sk */ (int16_t)MODIFIER_SYMBOL,
	/* So */ (int16_t)OTHER_SYMBOL,
	/* Pi */ (int16_t)INITIAL_PUNCTUATION,
	/* Pf */ (int16_t)FINAL_PUNCTUATION
	};

	const UChar CharTypeBuilder:: LAST_CHAR_CODE_IN_FILE = 0xFFFD;

CompactByteArray* CharTypeBuilder::charTypeArray = 0;
int 
CharTypeBuilder::MakeProp(char* str) 
{
	int result = 0;
	char* matchPosition;
	
	matchPosition = strstr(tagStrings, str);
	if (matchPosition == 0) fprintf(stderr, "unrecognized type letter %s\n", str);
	else result = ((matchPosition - tagStrings) / 2);
	return result;
}

CompactByteArray*
CharTypeBuilder::getByteArray(FILE* input)
{
	if (charTypeArray == 0) {
		char	buffer[1000];
		char*	bufferPtr;

		try {
			charTypeArray = ucmp8_open((int8_t)CharTypeBuilder::UNASSIGNED);
			int32_t unicode;
			while (TRUE) {
				bufferPtr = fgets(buffer, 999, input);
				if (bufferPtr == NULL) break;
				if (bufferPtr[0] == '#' || bufferPtr[0] == '\n' || bufferPtr[0] == 0) continue;
				sscanf(bufferPtr, "%X", &unicode);
				assert(0 <= unicode && unicode < 65536);
				bufferPtr = strchr(bufferPtr, ';');
				assert(bufferPtr != NULL);
				bufferPtr = strchr(bufferPtr + 1, ';');	// go to start of third field
				assert(bufferPtr != NULL);
				bufferPtr++;
				bufferPtr[2] = 0;
				ucmp8_set(charTypeArray, (UChar)unicode, (int8_t)tagValues[MakeProp(bufferPtr)]);
				if (unicode == LAST_CHAR_CODE_IN_FILE)
					break;
			}
            /* Check the database to see if this needs to be updated!!! */
			ucmp8_setRange(charTypeArray, 0x3401, 0x4db4, ucmp8_get(charTypeArray, 0x3400));
			ucmp8_setRange(charTypeArray, 0x4e01, 0x9fa4, ucmp8_get(charTypeArray, 0x4e00));
			ucmp8_setRange(charTypeArray, 0xac01, 0xd7a2, ucmp8_get(charTypeArray, 0xac00));
			ucmp8_setRange(charTypeArray, 0xd801, 0xdb7e, ucmp8_get(charTypeArray, 0xd800));
			ucmp8_setRange(charTypeArray, 0xdb81, 0xdbfe, ucmp8_get(charTypeArray, 0xdb80));
			ucmp8_setRange(charTypeArray, 0xdc01, 0xdffe, ucmp8_get(charTypeArray, 0xdc00));
			ucmp8_setRange(charTypeArray, 0xe001, 0xf8fe, ucmp8_get(charTypeArray, 0xe000));

			if (input) fclose(input);
			ucmp8_compact(charTypeArray, 1);
		}
		catch (...) {
			fprintf(stderr, "Error Occured while parsing unicode data file.\n");
		}
	}
	return charTypeArray;
}

void 
CharTypeBuilder::writeByteArrays()
{
	const int8_t* values = ucmp8_getArray(charTypeArray);
	const uint16_t* indexes = ucmp8_getIndex(charTypeArray);
	int32_t i;
    int32_t cnt = ucmp8_getCount(charTypeArray);
    cout << "\nconst unsigned short Unicode::indicies[] = {\n    ";
    for (i = 0; i < ucmp8_getkIndexCount()-1; i++)
    {
        cout << "(uint16_t)" << ((indexes[i] >= 0) ? (int)indexes[i] : (int)(indexes[i]+ucmp8_getkUnicodeCount()))
                         << ", ";
        if (i != 0)
            if (i % 3 == 0)
                cout << "\n    ";
    }
    cout << "    (uint16_t)" << ((indexes[ucmp8_getkIndexCount()-1] >= 0) ? (int)indexes[i] : (int)(indexes[i]+ucmp8_getkUnicodeCount()))
                       << " };\n";
    cout << "\nconst char Unicode::values[] = {\n    ";
    for (i = 0; i < cnt-1; i++)
    {
        cout << "(int8_t)" << (int)values[i] << ", ";
        if (i != 0)
            if (i % 5 == 0)
                cout << "\n    ";
    }
    cout << "    (int8_t)" << (int)values[cnt-1] << " }\n";
	cout << "const short Unicode::offsetCount = " << cnt << ";\n";
}
/**
 * The main function builds the CharType data array and prints it to System.out
 */
void main(int argc, char** argv)
{
    if (argc != 2) {
        printf("Usage : chartype filename\n\n");
        exit(1);
    }
    FILE *input = fopen(argv[1], "r");
    if (input == 0) {
        printf("Cannot open the input file: %s\n\n", argv[1]);
        exit(1);
    }
	CompactByteArray* arrays = CharTypeBuilder::getByteArray(input);
	CharTypeBuilder::writeByteArrays();
}

