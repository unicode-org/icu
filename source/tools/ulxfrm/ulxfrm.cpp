/*
 *
 *   Copyright (C) 1998-1999, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *
 * @version	1.0 06/19/98
 * @author	Helena Shih
 * Based on Taligent international support for C++
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>

#include "ucmp16.h"

#if U_IOSTREAM_SOURCE >= 199711
#include <iostream>
using namespace std;
#elif U_IOSTREAM_SOURCE >= 198506
#include <iostream.h>
#endif

CompactShortArray* ulxfrmArray = 0;

	enum	ECharTypeMapping {
		UNASSIGNED				= 0,
	UPPERCASE_LETTER		= 1,
	LOWERCASE_LETTER		= 2,
	TITLECASE_LETTER		= 3,
	MODIFIER_LETTER			= 4,
	OTHER_LETTER			= 5,
	NON_SPACING_MARK		= 6,
	ENCLOSING_MARK			= 7,
	COMBINING_SPACING_MARK	= 8,
	DECIMAL_DIGIT_NUMBER	= 9,
	LETTER_NUMBER			= 10,
	OTHER_NUMBER			= 11,
	SPACE_SEPARATOR			= 12,
	LINE_SEPARATOR			= 13,
	PARAGRAPH_SEPARATOR		= 14,
	CONTROL					= 15,
	FORMAT					= 16,
	PRIVATE_USE				= 17,
	SURROGATE				= 18,
	DASH_PUNCTUATION		= 19,
	START_PUNCTUATION		= 20,
	END_PUNCTUATION			= 21,
	CONNECTOR_PUNCTUATION	= 22,
	OTHER_PUNCTUATION		= 23,
	MATH_SYMBOL				= 24,
	CURRENCY_SYMBOL			= 25,
	MODIFIER_SYMBOL			= 26,
	OTHER_SYMBOL			= 27,
	INITIAL_PUNCTUATION     = 28,
	FINAL_PUNCTUATION       = 29
	};

static const UChar LAST_CHAR_CODE_IN_FILE = 0xFFFD;
const char tagStrings[] = "MnMcMeNdNlNoZsZlZpCcCfCsCoCnLuLlLtLmLoPcPdPsPePoSmScSkSoPiPf";
const int16_t tagValues[] =
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
int 
MakeProp(char* str) 
{
	int result = 0;
	char* matchPosition;
	
	matchPosition = strstr(tagStrings, str);
	if (matchPosition == 0) fprintf(stderr, "unrecognized type letter %s", str);
	else result = ((matchPosition - tagStrings) / 2);
	return result;
}

CompactShortArray*
getArray(FILE *input)
{
	if (ulxfrmArray == 0) {
		char	buffer[1000];
		char*	bufferPtr;
        int  set = FALSE;
        char type[3];

		try {
			ulxfrmArray = ucmp16_open((int16_t)0xffff);
			int32_t unicode, otherunicode, digit, i;
			while (TRUE) {
                otherunicode = 0xffff;
                digit = -1;
				bufferPtr = fgets(buffer, 999, input);
				if (bufferPtr == NULL) break;
				if (bufferPtr[0] == '#' || bufferPtr[0] == '\n' || bufferPtr[0] == 0) continue;
				sscanf(bufferPtr, "%X", &unicode);
				assert(0 <= unicode && unicode < 65536);
				bufferPtr = strchr(bufferPtr, ';');
				assert(bufferPtr != NULL);
                bufferPtr = strchr(bufferPtr + 1, ';');
				strncpy(type, ++bufferPtr, 2);	// go to start of third field
				assert(type != NULL);
				type[2] = 0;
  				int typeResult = tagValues[MakeProp(type)];
                // check for the decimal values
                bufferPtr++;
                for (i = 3; i < 8; i++) {
				    bufferPtr = strchr(bufferPtr, ';');
				    assert(bufferPtr != NULL);
				    bufferPtr++;
                }
    	    	sscanf(bufferPtr, "%X", &digit);
                if (((typeResult == DECIMAL_DIGIT_NUMBER) || (typeResult == OTHER_NUMBER)) &&
                    (digit >= 0 && digit <= 9)){
                    buffer[10];
                    sprintf(buffer, "0x%04X", unicode);
                    cout << "    { " << buffer << ", " << digit << "}, \n";
                }
                bufferPtr++;
                for (i = 8; i < 12; i++) {
				    bufferPtr = strchr(bufferPtr, ';');
				    assert(bufferPtr != NULL);
				    bufferPtr++;
                }
				sscanf(bufferPtr, "%X", &otherunicode);
                // the Unicode char has a equivalent uppercase
                if ((typeResult == LOWERCASE_LETTER) && (0 <= otherunicode && otherunicode < 65536)) { 
                    set = TRUE;
                } 
                if ((typeResult == UPPERCASE_LETTER) && !set) {
                    bufferPtr++;
			    	sscanf(bufferPtr, "%X", &otherunicode);
                    if (0 <= otherunicode && otherunicode < 65536) { 
                        set = TRUE;
                    } 
                }
				if ((set == TRUE) && (ucmp16_get(ulxfrmArray, (UChar)unicode) == (int16_t)0xffff))
					ucmp16_set(ulxfrmArray, (UChar)unicode, (int16_t)otherunicode);
                set = FALSE;
                }

			if (input) fclose(input);
			ucmp16_compact(ulxfrmArray);
		}
		catch (...) {
			fprintf(stderr, "Error Occured while parsing unicode data file.\n");
		}
	}
	return ulxfrmArray;
}

void 
writeArrays()
{
	const int16_t* values = ucmp16_getArray(ulxfrmArray);
	const uint16_t* indexes = ucmp16_getIndex(ulxfrmArray);
	int32_t i;
    int32_t cnt = ucmp16_getCount(ulxfrmArray);
    cout << "\nconst uint32_t Unicode::caseIndex[] = {\n    ";
    for (i = 0; i < ucmp16_getkIndexCount()-1; i++)
    {
        cout << "(uint16_t)" << ((indexes[i] >= 0) ? (int)indexes[i] : (int)(indexes[i]+ucmp16_getkUnicodeCount()))
                         << ", ";
        if (i != 0)
            if (i % 3 == 0)
                cout << "\n    ";
    }
    cout << "    (uint16_t)" << ((indexes[ucmp16_getkIndexCount()-1] >= 0) ? (int)indexes[i] : (int)(indexes[i]+ucmp16_getkUnicodeCount()))
                       << " };\n";
    cout << "\nconst int16_t Unicode::caseValues[] = {\n    ";
    for (i = 0; i < cnt-1; i++)
    {
        cout << "(int16_t)" << (int16_t)values[i] << ", ";
        if (i != 0)
            if (i % 5 == 0)
                cout << "\n    ";
    }
    cout << "    (char)" << (int16_t)values[cnt-1] << " }\n";
	cout << "const int32_t Unicode::caseCount = " << cnt << ";\n";
}
/**
 * The main function builds the CharType data array and prints it to System.out
 */
void main(int argc, char** argv)
{
	CompactShortArray* arrays = 0;
    FILE *input = 0;
    if (argc != 2) {
        printf("Usage : chartype filename\n\n");
        exit(1);
    }
    input = fopen(argv[1], "r");
    if (input == 0) {
        printf("Cannot open the input file: %s\n\n", argv[1]);
        exit(1);
    }
	arrays = getArray(input);
	writeArrays();
}

