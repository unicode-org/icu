/*
 * (C) Copyright IBM Corp. 1998 - All Rights Reserved
 * @version	1.0 06/19/98
 * @author	Helena Shih
 * Based on Taligent international support for C++
 */

#include <stdio.h>
#include <stdlib.h>
#include <iostream.h>
#include <string.h>
#include <assert.h>
#include "cmemory.h"
#include "ucmp8.h"
CompactByteArray* charDirArray = 0;

static const UChar LAST_CHAR_CODE_IN_FILE = 0xFFFD;
const char charDirStrings[] = "L  R  EN ES ET AN CS B  S  WS ON LRELROAL RLERLOPDFNSMBN ";

int tagValues[] = { 
	0,  // kLeftToRight              = 0, 
	1,  // kRightToLeft              = 1,
	2,  // kEuropeanNumber           = 2,
	3,  // kEuropeanNumberSeparator  = 3,
	4,  // kEuropeanNumberTerminator = 4,
	5,  // kArabicNumber             = 5,
	6,  // kCommonNumberSeparator    = 6,
    7,  // kParagraphSeparator       = 7,
	8,  // kSegmentSeparator         = 8,
	9,  // kWhiteSpaceNeutral        = 9, 
	10, // kOtherNeutral             = 10,
	11, // kLeftToRightEmbedding     = 11,
	12, // kLeftToRightOverride      = 12,
	13, // kRightToLeftArabic        = 13,
	14, // kRightToLeftEmbedding     = 14,
	15, // kRightToLeftOverride      = 15,
	16, // kPopDirectionalFormat     = 16,
	17, // kNonSpacingMark           = 17,
	18  // kBoundaryNeutral          = 18,
	
};

int MakeProp(char* str) 
{
	int result = 0;
	char* matchPosition;
	matchPosition = strstr(charDirStrings, str);
	if (matchPosition == 0) fprintf(stderr, "unrecognized type letter %s\n", str);
	else result = ((matchPosition - charDirStrings) / 3);
	return result;
}

CompactByteArray*
getArray(FILE *input)
{
	if (charDirArray == 0) {
		char	buffer[1000];
		char*	bufferPtr;
        int  set = FALSE;

		try {
			charDirArray = ucmp8_open(0);
			int32_t unicode;
            char *next;
            char dir[4];
			while (TRUE) {
                // Clear buffer first.
				bufferPtr = fgets(buffer, 999, input);
				if (bufferPtr == NULL) break;
				if (bufferPtr[0] == '#' || bufferPtr[0] == '\n' || bufferPtr[0] == 0) continue;
				sscanf(bufferPtr, "%X", &unicode);
				assert(0 <= unicode && unicode < 65536);
                for (int i = 0; i < 4; i++) {
				    bufferPtr = strchr(bufferPtr, ';');
				    assert(bufferPtr != NULL);
				    bufferPtr++;
                }
				assert(bufferPtr != NULL);
    		    next = strchr(bufferPtr, ';');
                *next = 0;
               /* for (int j = 0; j < 3; j++) {
				   if (bufferPtr+j!= next)
                        dir[j] = bufferPtr[j];
                   else
                        dir[j] = ' ';
                }*/
				for(int j=0; bufferPtr+j != next; j++)
					dir[j] = bufferPtr[j];
				while(j<3)
				{
					dir[j] = ' ';
					j++;
				}
				dir[3] = 0;
				ucmp8_set(charDirArray, (UChar)unicode, (int8_t)tagValues[MakeProp(dir)]);
                }

			if (input) fclose(input);
			ucmp8_compact(charDirArray, 1);
		}
		catch (...) {
			fprintf(stderr, "Error Occured while parsing unicode data file.\n");
		}
	}
	return charDirArray;
}

void 
writeArrays()
{
	const int8_t* values = ucmp8_getArray(charDirArray);
	const uint16_t* indexes = ucmp8_getIndex(charDirArray);
	int32_t i;
    int32_t cnt = ucmp8_getCount(charDirArray);
    cout << "\nconst t_uint32 Unicode::fCharDirIndices[] = {\n    ";
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
    cout << "\nconst int8_t Unicode::fCharDirValues[] = {\n    ";
    for (i = 0; i < cnt-1; i++)
    {
        cout << "(int8_t)" << (int)values[i] << ", ";
        if (i != 0)
            if (i % 5 == 0)
                cout << "\n    ";
    }
    cout << "    (int8_t)" << (int)values[cnt-1] << " }\n";
	cout << "const int32_t Unicode::fCharDirCount = " << cnt << ";\n";
}
/**
 * The main function builds the CharType data array and prints it to System.out
 */
void main(int argc, char** argv)
{
	CompactByteArray* arrays = 0;
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

