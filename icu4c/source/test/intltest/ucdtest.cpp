/*
*****************************************************************************************
*                                                                                       *
* COPYRIGHT:                                                                            *
*   (C) Copyright Taligent, Inc.,  1997                                                 *
*   (C) Copyright International Business Machines Corporation,  1997-1998               *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.                  *
*   US Government Users Restricted Rights - Use, duplication, or disclosure             *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                              *
*                                                                                       *
*****************************************************************************************
*/

#include "ucdtest.h"
#include "unicode/unicode.h"
#include <stdio.h>
#include <string.h>
#include <assert.h>
#include <iostream.h>

UnicodeTest::UnicodeTest()
{
}

UnicodeTest::~UnicodeTest()
{
}

void UnicodeTest::runIndexedTest( int32_t index, bool_t exec, char* &name, char* par )
{
    if (exec) logln("TestSuite UnicodeTest: ");
    switch (index) {
        case 0: name = "TestUpperLower"; if (exec) TestUpperLower(); break;
        case 1: name = "TestLetterNumber"; if (exec) TestLetterNumber(); break;
        case 2: name = "TestMisc"; if (exec) TestMisc(); break;
        case 3: name = "TestUnicodeData"; if (exec) TestUnicodeData(); break;

        default: name = ""; break; //needed to end loop
    }
}

//====================================================
// private data used by the tests
//====================================================

const UChar  LAST_CHAR_CODE_IN_FILE = 0xFFFD;
const char tagStrings[] = "MnMcMeNdNlNoZsZlZpCcCfCsCoCnLuLlLtLmLoPcPdPsPePoSmScSkSoPiPf";
const int32_t tagValues[] =
    {
    /* Mn */ Unicode::NON_SPACING_MARK,
	/* Mc */ Unicode::COMBINING_SPACING_MARK,
	/* Me */ Unicode::ENCLOSING_MARK,
	/* Nd */ Unicode::DECIMAL_DIGIT_NUMBER,
	/* Nl */ Unicode::LETTER_NUMBER,
	/* No */ Unicode::OTHER_NUMBER,
	/* Zs */ Unicode::SPACE_SEPARATOR,
	/* Zl */ Unicode::LINE_SEPARATOR,
	/* Zp */ Unicode::PARAGRAPH_SEPARATOR,
	/* Cc */ Unicode::CONTROL,
	/* Cf */ Unicode::FORMAT,
	/* Cs */ Unicode::SURROGATE,
	/* Co */ Unicode::PRIVATE_USE,
	/* Cn */ Unicode::UNASSIGNED,
	/* Lu */ Unicode::UPPERCASE_LETTER,
	/* Ll */ Unicode::LOWERCASE_LETTER,
	/* Lt */ Unicode::TITLECASE_LETTER,
	/* Lm */ Unicode::MODIFIER_LETTER,
	/* Lo */ Unicode::OTHER_LETTER,
	/* Pc */ Unicode::CONNECTOR_PUNCTUATION,
	/* Pd */ Unicode::DASH_PUNCTUATION,
	/* Ps */ Unicode::START_PUNCTUATION,
	/* Pe */ Unicode::END_PUNCTUATION,
	/* Po */ Unicode::OTHER_PUNCTUATION,
	/* Sm */ Unicode::MATH_SYMBOL,
	/* Sc */ Unicode::CURRENCY_SYMBOL,
	/* Sk */ Unicode::MODIFIER_SYMBOL,
	/* So */ Unicode::OTHER_SYMBOL,
	/* Pi */ Unicode::INITIAL_PUNCTUATION,
	/* Pf */ Unicode::FINAL_PUNCTUATION
    };
const char dirStrings[][5] = {
    "L",
    "R",
    "EN",
    "ES",
    "ET",   
    "AN",
    "CS",
    "B",
    "S",
    "WS",
    "ON",
    "LRE",
	"LRO",
	"AL",
	"RLE",
	"RLO",
	"PDF",
	"NSM",
	"BN"
};

//====================================================
// test toUpperCase() and toLowerCase()
//====================================================
void UnicodeTest::TestUpperLower()
{
    static char* upperTest = "abcdefg123hij.?:klmno";
    static char* lowerTest = "ABCDEFG123HIJ.?:KLMNO";
    uint16_t i;

//Checks LetterLike Symbols which were previously a source of confusion
//[Bertrand A. D. 02/04/98]
    for (i=0x2100;i<0x2138;i++)
    {
		if(i!=0x2126 && i!=0x212a && i!=0x212b)
		{
	    if (i != Unicode::toLowerCase(i)) // itself
            errln("Failed case conversion with itself: " + UCharToUnicodeString(i));
        if (i != Unicode::toUpperCase(i))
            errln("Failed case conversion with itself: " + UCharToUnicodeString(i));
		}
    }

    for (i = 0; i < 21; i++) {
//      logln((UnicodeString)"testing " + (int32_t)i + "...");
        if (Unicode::isLetter(upperTest[i]) && !Unicode::isLowerCase(upperTest[i]))
            errln("Failed isLowerCase test at " + UCharToUnicodeString(upperTest[i]));
        else if (Unicode::isLetter(lowerTest[i]) && !Unicode::isUpperCase(lowerTest[i]))
            errln("Failed isUpperCase test at " + UCharToUnicodeString(lowerTest[i]));
        else if (upperTest[i] != Unicode::toLowerCase(lowerTest[i]))
            errln("Failed case conversion : " + UCharToUnicodeString(upperTest[i]) + 
            " to " + UCharToUnicodeString(lowerTest[i]));
        else if (lowerTest[i] != Unicode::toUpperCase(upperTest[i]))
            errln("Failed case conversion : " + UCharToUnicodeString(upperTest[i]) +
            " to " + UCharToUnicodeString(lowerTest[i]));
        else if (upperTest[i] != Unicode::toLowerCase(upperTest[i])) // itself
            errln("Failed case conversion with itself: " + UCharToUnicodeString(upperTest[i]));
        else if (lowerTest[i] != Unicode::toUpperCase(lowerTest[i]))
            errln("Failed case conversion with itself: " + UCharToUnicodeString(lowerTest[i]));
    }
}

/* test isLetter() and isDigit() */
void UnicodeTest::TestLetterNumber()
{
    UChar i;

    for (i = 0x0041; i < 0x005B; i++) {
//      logln((UnicodeString)"testing " + (int32_t)i + "...");
        if (!Unicode::isLetter(i))
            errln("Failed isLetter test at " + UCharToUnicodeString(i));
    }
    for (i = 0x0660; i < 0x066A; i++) {
//      logln((UnicodeString)"testing " + (int32_t)i + "...");
        if (Unicode::isLetter(i))
            errln("Failed isLetter test with numbers at " + i);
    }
    for (i = 0x0660; i < 0x066A; i++) {
//      logln((UnicodeString)"testing " + (int32_t)i + "...");
        if (!Unicode::isDigit(i))
            errln("Failed isNumber test at " + i);
    }
}

/* Tests for isDefined(), isBaseForm(), isSpaceChar() and getCellWidth() */
void UnicodeTest::TestMisc()
{
    const UChar sampleSpaces[] = {0x0020, 0x00a0, 0x2000, 0x2001, 0x2005};
    const UChar sampleNonSpaces[] = {'a', 'b', 'c', 'd', 't'};
    const UChar sampleUndefined[] = {0xfff1, 0xfff7, 0xfa30};
    const UChar sampleDefined[] = {0x523E, 0x4f88, 0xfffd};
    const UChar sampleBase[] = {0x0061, 0x0031, 0x03d2};
    const UChar sampleNonBase[] = {0x002B, 0x0020, 0x203B};
    const UChar sampleChars[] = {0x000a, 0x0045, 0x4e00, 0xDC00};
    const UChar sampleDigits[]= {0x0030, 0x0662, 0x0F23, 0x0ED5};
    const UChar sampleNonDigits[] = {0x0010, 0x0041, 0x0122, 0x68FE};
    const int32_t sampleDigitValues[] = {0, 2, 3, 5};
    const uint16_t sampleCellWidth[] = {Unicode::ZERO_WIDTH, 
                                        Unicode::HALF_WIDTH, 
                                        Unicode::FULL_WIDTH, 
                                        Unicode::NEUTRAL};
    int32_t i;
    for (i = 0; i < 5; i++) {
//      logln((UnicodeString)"testing " + (int32_t)i + "...");
        if (!(Unicode::isSpaceChar(sampleSpaces[i])) ||
                (Unicode::isSpaceChar(sampleNonSpaces[i])))
            errln((UnicodeString)"Space char test error : " + (int32_t)sampleSpaces[i] +
            " or " + (int32_t)sampleNonSpaces[i]);
    }
    for (i = 0; i < 3; i++) {
//      logln((UnicodeString)"testing " + (int32_t)i + "...");
        if ((Unicode::isDefined(sampleUndefined[i])) ||
                !(Unicode::isDefined(sampleDefined[i])))
            errln((UnicodeString)"Undefined char test error : " +
            (int32_t)sampleUndefined[i] + " or " + (int32_t)sampleDefined[i]);
    }
    for (i = 0; i < 3; i++) {
//      logln((UnicodeString)"testing " + (int32_t)i + "...");
        if ((Unicode::isBaseForm(sampleNonBase[i])) ||
                !(Unicode::isBaseForm(sampleBase[i])))
            errln((UnicodeString)"Non-baseform char test error : " +
            (int32_t)sampleNonBase[i] + " or " + (int32_t)sampleBase[i]);
    }
    for (i = 0; i < 4; i++) {
//      logln((UnicodeString)"testing " + (int32_t)i + "...");
        if (Unicode::getCellWidth(sampleChars[i]) != sampleCellWidth[i])
            errln((UnicodeString)"Cell width char test error : " +
            (int32_t)sampleChars[i]);
    }
    for (i = 0; i < 4; i++) {
        if ((Unicode::isDigit(sampleDigits[i]) && 
            (Unicode::digitValue(sampleDigits[i])!= sampleDigitValues[i])) ||
            (Unicode::isDigit(sampleNonDigits[i]))) {
            errln((UnicodeString)"Digit char test error : " +
            (int32_t)sampleDigits[i] + " or " + (int32_t)sampleNonDigits[i]);
        }
    }
}

/* Tests for isControl() and isPrintable() */
void UnicodeTest::TestControlPrint()
{
    const UChar sampleControl[] = {0x001b, 0x0097, 0x0082};
    const UChar sampleNonControl[] = {'a', 0x0031, 0x00e2};
    const UChar samplePrintable[] = {0x0042, 0x005f, 0x2014};
    const UChar sampleNonPrintable[] = {0x200c, 0x009f, 0x001c};
    int32_t i;
    for (i = 0; i < 3; i++) {
//      logln((UnicodeString)"testing " + (int32_t)i + "...");
        if (!(Unicode::isControl(sampleControl[i])) ||
                (Unicode::isControl(sampleNonControl[i])))
            errln((UnicodeString)"Control char test error : " + (int32_t)sampleControl[i] +
            " or " + (int32_t)sampleNonControl[i]);
    }
    for (i = 0; i < 3; i++) {
//      logln((UnicodeString)"testing " + (int32_t)i + "...");
        if ((Unicode::isPrintable(samplePrintable[i])) ||
                !(Unicode::isPrintable(samplePrintable[i])))
            errln((UnicodeString)"Printable char test error : " +
            (int32_t)samplePrintable[i] + " or " + (int32_t)sampleNonPrintable[i]);
    }
}

/* Tests for isJavaIdentifierStart(), isJavaIdentifierPart(),
 * isUnicodeIdentifierStart() and isUnicodeIdentifierPart() */
void UnicodeTest::TestIdentifier()
{
    const UChar sampleJavaIDStart[] = {0x0071, 0x00e4, 0x005f};
    const UChar sampleNonJavaIDStart[] = {0x0020, 0x2030, 0x0082};
    const UChar sampleUnicodeIDStart[] = {0x0250, 0x00e2, 0x0061};
    const UChar sampleNonUnicodeIDStart[] = {0x2000, 0x000a, 0x2019};
    const UChar sampleJavaIDPart[] = {0x005f, 0x0032, 0x0045};
    const UChar sampleNonJavaIDPart[] = {0x007f, 0x2020, 0x0020};
    const UChar sampleUnicodeIDPart[] = {0x005f, 0x0032, 0x0045};
    const UChar sampleNonUnicodeIDPart[] = {0x007f, 0x00a3, 0x0020};
    const UChar sampleIDIgnore[] = {0x0006, 0x0010, 0x206b};
    const UChar sampleNonIDIgnore[] = {0x0075, 0x00a3, 0x0061};

    int32_t i;
    for (i = 0; i < 3; i++) {
//      logln((UnicodeString)"testing " + (int32_t)i + "...");
        if (!(Unicode::isJavaIdentifierStart(sampleJavaIDStart[i])) ||
                (Unicode::isJavaIdentifierStart(sampleNonJavaIDStart[i])))
            errln((UnicodeString)"Java ID Start char test error : " + (int32_t)sampleJavaIDStart[i] +
            " or " + (int32_t)sampleNonJavaIDStart[i]);
    }
    for (i = 0; i < 3; i++) {
//      logln((UnicodeString)"testing " + (int32_t)i + "...");
        if (!(Unicode::isJavaIdentifierPart(sampleJavaIDPart[i])) ||
                (Unicode::isJavaIdentifierPart(sampleNonJavaIDPart[i])))
            errln((UnicodeString)"Java ID Part char test error : " + (int32_t)sampleJavaIDPart[i] +
            " or " + (int32_t)sampleNonJavaIDPart[i]);
    }
    for (i = 0; i < 3; i++) {
//      logln((UnicodeString)"testing " + (int32_t)i + "...");
        if (!(Unicode::isUnicodeIdentifierStart(sampleUnicodeIDStart[i])) ||
                (Unicode::isUnicodeIdentifierStart(sampleNonUnicodeIDStart[i])))
            errln((UnicodeString)"Unicode ID Start char test error : " + (int32_t)sampleUnicodeIDStart[i] +
            " or " + (int32_t)sampleNonUnicodeIDStart[i]);
    }
    for (i = 0; i < 3; i++) {
//      logln((UnicodeString)"testing " + (int32_t)i + "...");
        if (!(Unicode::isUnicodeIdentifierPart(sampleUnicodeIDPart[i])) ||
                (Unicode::isUnicodeIdentifierPart(sampleNonUnicodeIDPart[i])))
            errln((UnicodeString)"Unicode ID Part char test error : " + (int32_t)sampleUnicodeIDPart[i] +
            " or " + (int32_t)sampleNonUnicodeIDPart[i]);
    }
    for (i = 0; i < 3; i++) {
//      logln((UnicodeString)"testing " + (int32_t)i + "...");
        if (!(Unicode::isIdentifierIgnorable(sampleIDIgnore[i])) ||
                (Unicode::isIdentifierIgnorable(sampleNonIDIgnore[i])))
            errln((UnicodeString)"ID ignorable char test error : " + (int32_t)sampleIDIgnore[i] +
            " or " + (int32_t)sampleNonIDIgnore[i]);
    }
}

/* tests for getType(), isTitleCase(), and toTitleCase() as well as characterDirection()*/
void UnicodeTest::TestUnicodeData()
{
    FILE*   input;
    char    buffer[1000];
    char*   bufferPtr, *dirPtr;
    char path[256];
    const char* datafile = "UnicodeData-3.0.0.txt";
  
    strcpy(path, u_getDataDirectory());
    strcat(path, datafile);
      
    input = fopen( path, "r");
    if (input == 0)
    {
        errln("Failed to open: " + UnicodeString(datafile));
        return;
    }

    int32_t unicode;
    for(;;) {
        bufferPtr = fgets(buffer, 999, input);
        if (bufferPtr == NULL) break;
        if (bufferPtr[0] == '#' || bufferPtr[0] == '\n' || bufferPtr[0] == 0) continue;
        sscanf(bufferPtr, "%X", &unicode);
        assert(0 <= unicode && unicode < 65536);
        if (unicode == LAST_CHAR_CODE_IN_FILE)
            break;
        bufferPtr = strchr(bufferPtr, ';');
        assert(bufferPtr != NULL);
        bufferPtr = strchr(bufferPtr + 1, ';'); // go to start of third field
        assert(bufferPtr != NULL);
        dirPtr = bufferPtr;
        dirPtr = strchr(dirPtr + 1, ';');
        assert(dirPtr != NULL);
        dirPtr = strchr(dirPtr + 1, ';');
        assert(dirPtr != NULL);
        bufferPtr++;
        bufferPtr[2] = 0;
//      logln((UnicodeString)"testing " + (int32_t)unicode + "...");
        if (Unicode::getType((UChar)unicode) != tagValues[MakeProp(bufferPtr)])
            errln("Unicode character type failed at " + unicode);
        // test title case
        if ((Unicode::toTitleCase((UChar)unicode) != Unicode::toUpperCase((UChar)unicode)) &&
            !(Unicode::isTitleCase(Unicode::toTitleCase((UChar)unicode))))
            errln("Title case test failed at " + unicode);
        bufferPtr = strchr(dirPtr + 1, ';');
        dirPtr++;
        bufferPtr[0] = 0;
        if (Unicode::characterDirection((UChar)unicode) != MakeDir(dirPtr))
            errln("Unicode character directionality failed at\n " + unicode);
    }

    if (input) fclose(input);

    // test Unicode::getCharName()
    // a more thorough test of u_charName() is in cintltst/cucdtst.c
    UTextOffset length=Unicode::getCharName(0x284, buffer, sizeof(buffer));

    // use invariant-character conversion to Unicode
    UnicodeString name(buffer, length, "");
    if(name!=UNICODE_STRING("LATIN SMALL LETTER DOTLESS J WITH STROKE AND HOOK", 49)) {
        errln("Unicode character name lookup failed\n");
    }
}

int32_t UnicodeTest::MakeProp(char* str) 
{
    int32_t result = 0;
    const char* matchPosition;
    
    matchPosition = strstr(tagStrings, str);
    if (matchPosition == 0) errln((UnicodeString)"unrecognized type letter " + str);
    else result = ((matchPosition - tagStrings) / 2);
    return result;
}

int32_t UnicodeTest::MakeDir(char* str) 
{
    int32_t pos = 0;
    for (pos = 0; pos < 19; pos++) {
        if (strcmp(str, dirStrings[pos]) == 0) {
            return pos;
        }
    }
    return -1;
}

