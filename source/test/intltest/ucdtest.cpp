/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

#include "ucdtest.h"
#include "unicode/unicode.h"
#include "unicode/ustring.h"
#include <stdio.h>
#include <string.h>
#include <assert.h>

UnicodeTest::UnicodeTest()
{
}

UnicodeTest::~UnicodeTest()
{
}

void UnicodeTest::runIndexedTest( int32_t index, UBool exec, char* &name, char* par )
{
    if (exec) logln("TestSuite UnicodeTest: ");
    switch (index) {
        case 0: name = "TestUpperLower"; if (exec) TestUpperLower(); break;
        case 1: name = "TestLetterNumber"; if (exec) TestLetterNumber(); break;
        case 2: name = "TestMisc"; if (exec) TestMisc(); break;
        case 3: name = "TestUnicodeData"; if (exec) TestUnicodeData(); break;
        case 4: name = "TestCodeUnit"; if(exec) TestCodeUnit(); break;
        case 5: name = "TestCodePoint"; if(exec) TestCodePoint(); break;
        case 6: name = "TestCharLength"; if(exec) TestCharLength(); break;
        case 7: name = "TestIdentifier"; if(exec) TestIdentifier(); break;  
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
    U_STRING_DECL(upperTest, "abcdefg123hij.?:klmno", 21);
    U_STRING_DECL(lowerTest, "ABCDEFG123HIJ.?:KLMNO", 21);
    uint16_t i;

    U_STRING_INIT(upperTest, "abcdefg123hij.?:klmno", 21);
    U_STRING_INIT(lowerTest, "ABCDEFG123HIJ.?:KLMNO", 21);

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
    const UChar sampleNonSpaces[] = {0x61, 0x62, 0x63, 0x64, 0x74};
    const UChar sampleWhiteSpaces[] = {0x2008, 0x2009, 0x200a, 0x001c, 0x000c};
    const UChar sampleNonWhiteSpaces[] = {0x61, 0x62, 0x3c, 0x28, 0x3f};
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
    for (i = 0; i < 5; i++) {
//      log_ln("Testing for isWhitespace and nonWhitespaces\n");
        if (!(Unicode::isWhitespace(sampleWhiteSpaces[i])) ||
                (Unicode::isWhitespace(sampleNonWhiteSpaces[i])))
        {
            errln((UnicodeString)"White Space char test error : " + (int32_t)sampleWhiteSpaces[i] +
                "or" + (int32_t)sampleNonWhiteSpaces[i]);
        }
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
    const UChar sampleNonControl[] = {0x61, 0x0031, 0x00e2};
    const UChar samplePrintable[] = {0x0042, 0x005f, 0x2014};
    const UChar sampleNonPrintable[] = {0x200c, 0x009f, 0x001b};
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
    const UChar sampleNonJavaIDPart[] = {0x2030, 0x2020, 0x0020};
    const UChar sampleUnicodeIDPart[] = {0x005f, 0x0032, 0x0045};
    const UChar sampleNonUnicodeIDPart[] = {0x2030, 0x00a3, 0x0020};
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
    int8_t type;
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

        /* we override the general category of some control characters */
        switch(unicode) {
        case 9:
        case 0xb:
        case 0x1f:
            type = U_SPACE_SEPARATOR;
            break;
        case 0xc:
            type = U_LINE_SEPARATOR;
            break;
        case 0xa:
        case 0xd:
        case 0x1c:
        case 0x1d:
        case 0x1e:
        case 0x85:
            type = U_PARAGRAPH_SEPARATOR;
            break;
        default:
            type = (int8_t)tagValues[MakeProp(bufferPtr)];
            break;
        }
        if (Unicode::getType((UChar)unicode) != type)
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

    // test Unicode::isMirrored() and charMirror()
    // see also cintltst/cucdtest.c
    if(!(Unicode::isMirrored(0x28) && Unicode::isMirrored(0xbb) && Unicode::isMirrored(0x2045) && Unicode::isMirrored(0x232a) &&
         !Unicode::isMirrored(0x27) && !Unicode::isMirrored(0x61) && !Unicode::isMirrored(0x284) && !Unicode::isMirrored(0x3400)
        )
    ) {
        errln("Unicode::isMirrored() does not work correctly\n");
    }

    if(!(Unicode::charMirror(0x3c)==0x3e && Unicode::charMirror(0x5d)==0x5b && Unicode::charMirror(0x208d)==0x208e && Unicode::charMirror(0x3017)==0x3016 &&
         Unicode::charMirror(0x2e)==0x2e && Unicode::charMirror(0x6f3)==0x6f3 && Unicode::charMirror(0x301c)==0x301c && Unicode::charMirror(0xa4ab)==0xa4ab
        )
    ) {
        errln("Unicode::charMirror() does not work correctly\n");
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
/*Tests added by Madhu*/

/* Tests for isSingle(), isLead(), isTrial(), isSurrogate */
void UnicodeTest::TestCodeUnit(){
	const UChar codeunit[]={0x0000,0xe065,0x20ac,0xd7ff,0xd800,0xd841,0xd905,0xdbff,0xdc00,0xdc02,0xddee,0xdfff,0};
	
	int16_t i;
	
	for(i=0; i<sizeof(codeunit)/sizeof(codeunit[0]); i++){
	    UChar c=codeunit[i];
		UnicodeString msg;
		msg.append((UChar32)c);
		logln((UnicodeString)"Testing code unit value of " + prettify(msg));
		if(i<4){
			if(!(Unicode::isSingle(c)) || (Unicode::isLead(c)) || (Unicode::isTrail(c)) ||(Unicode::isSurrogate(c))){
				errln((UnicodeString)"ERROR:" + prettify(msg) + " is a single");
			}
		
		}
		if(i >= 4 && i< 8){
			if(!(Unicode::isLead(c)) || Unicode::isSingle(c) || Unicode::isTrail(c) || !(Unicode::isSurrogate(c))){
				errln((UnicodeString)"ERROR:" + prettify(msg) + " is a first surrogate");
			}
		}
		if(i >= 8 && i< 12){
			if(!(Unicode::isTrail(c)) || Unicode::isSingle(c) || Unicode::isLead(c) || !(Unicode::isSurrogate(c))){
				errln((UnicodeString)"ERROR:" + prettify(msg) + " is a second surrogate");
			}
		}
	}

}
/* Tests for isSurrogate(), isUnicodeChar(), isError(), isValid() */
void UnicodeTest::TestCodePoint(){
	const UChar32 codePoint[]={
		//surrogate, notvalid(codepoint), not a UnicodeChar, not Error
		0xd800,
        0xdbff,
        0xdc00,
		0xdfff,
		0xdc04,
		0xd821,
	    //not a surrogate, valid, isUnicodeChar , not Error
		0x20ac,
		0xd7ff,
		0xe000,
		0xe123,
		0x0061,
		0xe065, 
	    0x20402,
        0x24506,
		0x23456,
		0x20402,
		0x10402,
		0x23456,
        //not a surrogate, not valid, isUnicodeChar, isError
		0x0015,
		0x009f,
		//not a surrogate, not valid, not isUnicodeChar, isError
		0xffff,
		0xfffe,
	};
    int16_t i;
	for(i=0; i<sizeof(codePoint)/sizeof(codePoint[0]); i++){
	    UChar32 c=codePoint[i];
		UnicodeString msg;
		msg.append(c);
		logln((UnicodeString)"Testing code Point value of " + prettify(msg));
		if(i<6){
			if(!Unicode::isSurrogate(c)){
				errln((UnicodeString)"ERROR: isSurrogate() failed for" + prettify(msg));
			}
			if(Unicode::isValid(c)){
                errln((UnicodeString)"ERROR: isValid() failed for "+ prettify(msg));
			}
			if(Unicode::isUnicodeChar(c)){
				errln((UnicodeString)"ERROR: isUnicodeChar() failed for "+ prettify(msg));
			}
			if(Unicode::isError(c)){
				errln((UnicodeString)"ERROR: isError() failed for "+ prettify(msg));
			}
		}else if(i >=6 && i<18){
			if(Unicode::isSurrogate(c)){
				errln((UnicodeString)"ERROR: isSurrogate() failed for" + prettify(msg));
			}
			if(!Unicode::isValid(c)){
                errln((UnicodeString)"ERROR: isValid() failed for "+ prettify(msg));
			}
			if(!Unicode::isUnicodeChar(c)){
				errln((UnicodeString)"ERROR: isUnicodeChar() failed for "+ prettify(msg));
			}
			if(Unicode::isError(c)){
				errln((UnicodeString)"ERROR: isError() failed for "+ prettify(msg));
			}
		}else if(i >=18 && i<20){
			if(Unicode::isSurrogate(c)){
				errln((UnicodeString)"ERROR: isSurrogate() failed for" + prettify(msg));
			}
			if(Unicode::isValid(c)){
                errln((UnicodeString)"ERROR: isValid() failed for "+ prettify(msg));
			}
			if(!Unicode::isUnicodeChar(c)){
				errln((UnicodeString)"ERROR: isUnicodeChar() failed for "+ prettify(msg));
			}
			if(!Unicode::isError(c)){
				errln((UnicodeString)"ERROR: isError() failed for "+ prettify(msg));
			}
		}
		else if(i >=18 && i<sizeof(codePoint)/sizeof(codePoint[0])){
		    if(Unicode::isSurrogate(c)){
				errln((UnicodeString)"ERROR: isSurrogate() failed for" + prettify(msg));
			}
			if(Unicode::isValid(c)){
                errln((UnicodeString)"ERROR: isValid() failed for "+ prettify(msg));
			}
			if(Unicode::isUnicodeChar(c)){
				errln((UnicodeString)"ERROR: isUnicodeChar() failed for "+ prettify(msg));
			}
			if(!Unicode::isError(c)){
				errln((UnicodeString)"ERROR: isError() failed for "+ prettify(msg));
			}
		}
	}
 
}
void UnicodeTest::TestCharLength()
{
	const int32_t codepoint[]={
		1, 0x0061,
        1, 0xe065,
		1, 0x20ac,
	    2, 0x20402,
		2, 0x23456,
		2, 0x24506,
		2, 0x20402,
		2, 0x10402,
		1, 0xd7ff,
		1, 0xe000
	};
	
	int16_t i;
	UBool multiple;
	for(i=0; i<sizeof(codepoint)/sizeof(codepoint[0]); i=i+2){
		UChar32 c=codepoint[i+1];
		UnicodeString msg;
		msg.append(c);
		if(Unicode::charLength(c) != codepoint[i]){
			errln((UnicodeString)"The no: of code units for" + prettify(msg)+
				":- Expected: " + (int32_t)codepoint[i] + " Got: " + Unicode::charLength(c));
		}else{
			logln((UnicodeString)"The no: of code units for" + prettify(msg) + " is " + Unicode::charLength(c)); 
		}
        multiple=codepoint[i] == 1 ? FALSE : TRUE;
		if(Unicode::needMultipleUChar(c) != multiple){
			  errln("ERROR: Unicode::needMultipleUChar() failed for" + prettify(msg));
		}
	}
}
