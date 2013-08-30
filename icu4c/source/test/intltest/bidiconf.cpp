/*
*******************************************************************************
*
*   Copyright (C) 2009-2013, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  bidiconf.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2009oct16
*   created by: Markus W. Scherer
*
*   BiDi conformance test, using the Unicode BidiTest.txt and BidiCharacterTest.txt files.
*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "unicode/utypes.h"
#include "unicode/ubidi.h"
#include "unicode/errorcode.h"
#include "unicode/localpointer.h"
#include "unicode/putil.h"
#include "unicode/unistr.h"
#include "intltest.h"
#include "uparse.h"

class BiDiConformanceTest : public IntlTest {
public:
    BiDiConformanceTest() :
        directionBits(0), lineNumber(0), levelsCount(0), orderingCount(0),
        errorCount(0) {}

    void runIndexedTest(int32_t index, UBool exec, const char *&name, char *par=NULL);

    void TestBidiTest();
    void TestBidiCharacterTest();
private:
    char *getUnidataPath(char path[]);

    UBool parseLevels(const char *&start);
    UBool parseOrdering(const char *start);
    UBool parseInputStringFromBiDiClasses(const char *&start, UBool parseChars);

    UBool checkLevels(const UBiDiLevel actualLevels[], int32_t actualCount);
    UBool checkOrdering(UBiDi *ubidi);

    void printErrorLine();

    char line[10000];
    UBiDiLevel levels[1000];
    uint32_t directionBits;
    int32_t ordering[1000];
    int32_t lineNumber;
    int32_t levelsCount;
    int32_t orderingCount;
    int32_t errorCount;
    UnicodeString inputString;
    const char *paraLevelName;
    char levelNameString[12];
};

extern IntlTest *createBiDiConformanceTest() {
    return new BiDiConformanceTest();
}

void BiDiConformanceTest::runIndexedTest(int32_t index, UBool exec, const char *&name, char * /*par*/) {
    if(exec) {
        logln("TestSuite BiDiConformanceTest: ");
    }
    TESTCASE_AUTO_BEGIN;
    TESTCASE_AUTO(TestBidiTest);
    TESTCASE_AUTO(TestBidiCharacterTest);
    TESTCASE_AUTO_END;
}

// TODO: Move to a common place (IntlTest?) to avoid duplication with UnicodeTest (ucdtest.cpp).
char *BiDiConformanceTest::getUnidataPath(char path[]) {
    IcuTestErrorCode errorCode(*this, "getUnidataPath");
    const int kUnicodeDataTxtLength=15;  // strlen("UnicodeData.txt")

    // Look inside ICU_DATA first.
    strcpy(path, pathToDataDirectory());
    strcat(path, "unidata" U_FILE_SEP_STRING "UnicodeData.txt");
    FILE *f=fopen(path, "r");
    if(f!=NULL) {
        fclose(f);
        *(strchr(path, 0)-kUnicodeDataTxtLength)=0;  // Remove the basename.
        return path;
    }

    // As a fallback, try to guess where the source data was located
    // at the time ICU was built, and look there.
#   ifdef U_TOPSRCDIR
        strcpy(path, U_TOPSRCDIR  U_FILE_SEP_STRING "data");
#   else
        strcpy(path, loadTestData(errorCode));
        strcat(path, U_FILE_SEP_STRING ".." U_FILE_SEP_STRING ".."
                     U_FILE_SEP_STRING ".." U_FILE_SEP_STRING ".."
                     U_FILE_SEP_STRING "data");
#   endif
    strcat(path, U_FILE_SEP_STRING);
    strcat(path, "unidata" U_FILE_SEP_STRING "UnicodeData.txt");
    f=fopen(path, "r");
    if(f!=NULL) {
        fclose(f);
        *(strchr(path, 0)-kUnicodeDataTxtLength)=0;  // Remove the basename.
        return path;
    }
    return NULL;
}

U_DEFINE_LOCAL_OPEN_POINTER(LocalStdioFilePointer, FILE, fclose);

UBool BiDiConformanceTest::parseLevels(const char *&start) {
    directionBits=0;
    levelsCount=0;
    while(*start!=0 && *(start=u_skipWhitespace(start))!=0 && *start!=';') {
        if(*start=='x') {
            levels[levelsCount++]=UBIDI_DEFAULT_LTR;
            ++start;
        } else {
            char *end;
            uint32_t value=(uint32_t)strtoul(start, &end, 10);
            if(end<=start || (!U_IS_INV_WHITESPACE(*end) && *end!=0 && *end!=';')
                          || value>(UBIDI_MAX_EXPLICIT_LEVEL+1)) {
                errln("\nError on line %d: Levels parse error at %s", (int)lineNumber, start);
                printErrorLine();
                return FALSE;
            }
            levels[levelsCount++]=(UBiDiLevel)value;
            directionBits|=(1<<(value&1));
            start=end;
        }
    }
    return TRUE;
}

UBool BiDiConformanceTest::parseOrdering(const char *start) {
    orderingCount=0;
    while(*start!=0 && *(start=u_skipWhitespace(start))!=0 && *start!=';') {
        char *end;
        uint32_t value=(uint32_t)strtoul(start, &end, 10);
        if(end<=start || (!U_IS_INV_WHITESPACE(*end) && *end!=0 && *end!=';') || value>=1000) {
            errln("\nError on line %d: Reorder parse error at %s", (int)lineNumber, start);
            printErrorLine();
            return FALSE;
        }
        ordering[orderingCount++]=(int32_t)value;
        start=end;
    }
    return TRUE;
}

static const UChar pseudoCharFromBiDiClass[U_CHAR_DIRECTION_COUNT]={
    0x6c,   // 'l' for L
    0x52,   // 'R' for R
    0x33,   // '3' for EN
    0x2d,   // '-' for ES
    0x25,   // '%' for ET
    0x39,   // '9' for AN
    0x2c,   // ',' for CS
    0x2f,   // '/' for B
    0x5f,   // '_' for S
    0x20,   // ' ' for WS
    0x3d,   // '=' for ON
    0x65,   // 'e' for LRE
    0x6f,   // 'o' for LRO
    0x41,   // 'A' for AL
    0x45,   // 'E' for RLE
    0x4f,   // 'O' for RLO
    0x2a,   // '*' for PDF
    0x60,   // '`' for NSM
    0x7c,   // '|' for BN
    // new in Unicode 6.3/ICU 52
    0x53,   // 'S' for FSI
    0x69,   // 'i' for LRI
    0x49,   // 'I' for RLI
    0x2e    // '.' for PDI
};

static const UChar realCharFromBiDiClass[U_CHAR_DIRECTION_COUNT]={
    0x006c,   // 'l' for L
    0x05d0,   // Hebrew Letter Alef for R
    0x0033,   // '3' for EN
    0x002d,   // '-' for ES
    0x0025,   // '%' for ET
    0x0669,   // Arabic-Indic '9' for AN
    0x002c,   // ',' for CS
    0x000d,   // CR  for B
    0x0009,   // Tab for S
    0x0020,   // ' ' for WS
    0x003d,   // '=' for ON
    0x202a,   // LRE
    0x202d,   // LRO
    0x0630,   // Arabic Letter Thal for AL
    0x202b,   // RLE
    0x202e,   // RLO
    0x202c,   // PDF
    0x05b9,   // Hebrew Point Holam for NSM
    0x00ad,   // Soft Hyphen for BN
    0x2068,   // FSI
    0x2066,   // LRI
    0x2067,   // RLI
    0x2069    // PDI
};

U_CDECL_BEGIN

static UCharDirection U_CALLCONV
biDiConfUBiDiClassCallback(const void * /*context*/, UChar32 c) {
    for(int i=0; i<U_CHAR_DIRECTION_COUNT; ++i) {
        if(c==pseudoCharFromBiDiClass[i]) {
            return (UCharDirection)i;
        }
    }
    // Character not in our hardcoded table.
    // Should not occur during testing.
    return U_BIDI_CLASS_DEFAULT;
}

U_CDECL_END

static int32_t hexdigit(char c) {
    if(c>='0' && c<='9')
        return c - '0';
    if(c>='A' && c<='F')
        return c - ('A'-10);
    if(c>='a' && c<='f')
        return c - ('a'-10);
    return -1;
}

static const int8_t biDiClassNameLengths[U_CHAR_DIRECTION_COUNT+1]={
    1, 1, 2, 2, 2, 2, 2, 1, 1, 2, 2, 3, 3, 2, 3, 3, 3, 3, 2, 3, 3, 3, 3, 0
};

UBool BiDiConformanceTest::parseInputStringFromBiDiClasses(const char *&start, UBool parseChars) {
    inputString.remove();
    /*
     * Lengthy but fast BiDi class parser.
     * A simple parser could terminate or extract the name string and use
     *   int32_t biDiClassInt=u_getPropertyValueEnum(UCHAR_BIDI_CLASS, bidiClassString);
     * but that makes this test take significantly more time.
     */
    while(*start!=0 && *(start=u_skipWhitespace(start))!=0 && *start!=';') {
        int32_t d1, d2, hexnum;
        // First look for an hexa value of at least 2 digits
        if(parseChars && (d1=hexdigit(start[0]))>=0 && (d2=hexdigit(start[1]))>=0) {
            const char *saveStart=start;
            hexnum=(d1<<4) + d2;
            start+=2;
            while((d1=hexdigit(start[0]))>=0) {
                hexnum=(hexnum<<4) + d1;
                start++;
            }
            if(hexnum<=0 || hexnum>0xffff ||
               (!U_IS_INV_WHITESPACE(start[0]) && start[0]!=';' && start[0]!=0)) {
                errln("\nError on line %d: Invalid hexa number at %s", (int)lineNumber, saveStart);
                return FALSE;
            }
            inputString.append(hexnum);
            continue;
        }
        UCharDirection biDiClass=U_CHAR_DIRECTION_COUNT;
        // Compare each character once until we have a match on
        // a complete, short BiDi class name.
        if(start[0]=='L') {
            if(start[1]=='R') {
                if(start[2]=='E') {
                    biDiClass=U_LEFT_TO_RIGHT_EMBEDDING;
                } else if(start[2]=='I') {
                    biDiClass=U_LEFT_TO_RIGHT_ISOLATE;
                } else if(start[2]=='O') {
                    biDiClass=U_LEFT_TO_RIGHT_OVERRIDE;
                }
            } else {
                biDiClass=U_LEFT_TO_RIGHT;
            }
        } else if(start[0]=='R') {
            if(start[1]=='L') {
                if(start[2]=='E') {
                    biDiClass=U_RIGHT_TO_LEFT_EMBEDDING;
                } else if(start[2]=='I') {
                    biDiClass=U_RIGHT_TO_LEFT_ISOLATE;
                } else if(start[2]=='O') {
                    biDiClass=U_RIGHT_TO_LEFT_OVERRIDE;
                }
            } else {
                biDiClass=U_RIGHT_TO_LEFT;
            }
        } else if(start[0]=='E') {
            if(start[1]=='N') {
                biDiClass=U_EUROPEAN_NUMBER;
            } else if(start[1]=='S') {
                biDiClass=U_EUROPEAN_NUMBER_SEPARATOR;
            } else if(start[1]=='T') {
                biDiClass=U_EUROPEAN_NUMBER_TERMINATOR;
            }
        } else if(start[0]=='A') {
            if(start[1]=='L') {
                biDiClass=U_RIGHT_TO_LEFT_ARABIC;
            } else if(start[1]=='N') {
                biDiClass=U_ARABIC_NUMBER;
            }
        } else if(start[0]=='C' && start[1]=='S') {
            biDiClass=U_COMMON_NUMBER_SEPARATOR;
        } else if(start[0]=='B') {
            if(start[1]=='N') {
                biDiClass=U_BOUNDARY_NEUTRAL;
            } else {
                biDiClass=U_BLOCK_SEPARATOR;
            }
        } else if(start[0]=='S') {
            biDiClass=U_SEGMENT_SEPARATOR;
        } else if(start[0]=='W' && start[1]=='S') {
            biDiClass=U_WHITE_SPACE_NEUTRAL;
        } else if(start[0]=='O' && start[1]=='N') {
            biDiClass=U_OTHER_NEUTRAL;
        } else if(start[0]=='P' && start[1]=='D') {
            if(start[2]=='F') {
                biDiClass=U_POP_DIRECTIONAL_FORMAT;
            } else if(start[2]=='I') {
                biDiClass=U_POP_DIRECTIONAL_ISOLATE;
            }
        } else if(start[0]=='N' && start[1]=='S' && start[2]=='M') {
            biDiClass=U_DIR_NON_SPACING_MARK;
        } else if(start[0]=='F' && start[1]=='S' && start[2]=='I') {
            biDiClass=U_FIRST_STRONG_ISOLATE;
        }
        // Now we verify that the class name is terminated properly,
        // and not just the start of a longer word.
        int8_t biDiClassNameLength=biDiClassNameLengths[biDiClass];
        char c=start[biDiClassNameLength];
        if(biDiClass<U_CHAR_DIRECTION_COUNT && (U_IS_INV_WHITESPACE(c) || c==';' || c==0)) {
            if(parseChars) {
                inputString.append(realCharFromBiDiClass[biDiClass]);
            } else {
                inputString.append(pseudoCharFromBiDiClass[biDiClass]);
            }
            start+=biDiClassNameLength;
            continue;
        }
#if 0
        // Accept any single character
        // Not currently supported:
        // This parser reads the .txt file as is, with the default charset.
        // We could at most support "invariant" characters,
        // and would have to convert them to Unicode using invariant-character functions.
        // If we need to support Unicode characters, then we would have to
        // rewrite the code for reading and parsing to read UTF-8.
        if(parseChars && (U_IS_INV_WHITESPACE(start[1]) || start[1]==';' || start[1]==0)) {
            inputString.append(start[0]);
            start++;
            continue;
        }
#endif
        errln("\nError on line %d: BiDi class string not recognized at %s", (int)lineNumber, start);
        printErrorLine();
        return FALSE;
    }
    return TRUE;
}

void BiDiConformanceTest::TestBidiTest() {
    IcuTestErrorCode errorCode(*this, "TestBidiTest");
    const char *sourceTestDataPath=getSourceTestData(errorCode);
    if(errorCode.logIfFailureAndReset("unable to find the source/test/testdata "
                                      "folder (getSourceTestData())")) {
        return;
    }
    char bidiTestPath[400];
    strcpy(bidiTestPath, sourceTestDataPath);
    strcat(bidiTestPath, "BidiTest.txt");
    LocalStdioFilePointer bidiTestFile(fopen(bidiTestPath, "r"));
    if(bidiTestFile.isNull()) {
        errln("unable to open %s", bidiTestPath);
        return;
    }
    LocalUBiDiPointer ubidi(ubidi_open());
    ubidi_setClassCallback(ubidi.getAlias(), biDiConfUBiDiClassCallback, NULL,
                           NULL, NULL, errorCode);
    if(errorCode.logIfFailureAndReset("ubidi_setClassCallback()")) {
        return;
    }
    lineNumber=0;
    levelsCount=0;
    orderingCount=0;
    errorCount=0;
    while(errorCount<10 && fgets(line, (int)sizeof(line), bidiTestFile.getAlias())!=NULL) {
        ++lineNumber;
        // Remove trailing comments and whitespace.
        char *commentStart=strchr(line, '#');
        if(commentStart!=NULL) {
            *commentStart=0;
        }
        u_rtrim(line);
        const char *start=u_skipWhitespace(line);
        if(*start==0) {
            continue;  // Skip empty and comment-only lines.
        }
        if(*start=='@') {
            ++start;
            if(0==strncmp(start, "Levels:", 7)) {
                start+=7;
                if(!parseLevels(start)) {
                    return;
                }
            } else if(0==strncmp(start, "Reorder:", 8)) {
                if(!parseOrdering(start+8)) {
                    return;
                }
            }
            // Skip unknown @Xyz: ...
        } else {
            if(!parseInputStringFromBiDiClasses(start, FALSE)) {
                return;
            }
            start=u_skipWhitespace(start);
            if(*start!=';') {
                errln("missing ; separator on input line %s", line);
                return;
            }
            start=u_skipWhitespace(start+1);
            char *end;
            uint32_t bitset=(uint32_t)strtoul(start, &end, 16);
            if(end<=start || (!U_IS_INV_WHITESPACE(*end) && *end!=';' && *end!=0)) {
                errln("input bitset parse error at %s", start);
                return;
            }
            // Loop over the bitset.
            static const UBiDiLevel paraLevels[]={ UBIDI_DEFAULT_LTR, 0, 1, UBIDI_DEFAULT_RTL };
            static const char *const paraLevelNames[]={ "auto/LTR", "LTR", "RTL", "auto/RTL" };
            for(int i=0; i<=3; ++i) {
                if(bitset&(1<<i)) {
                    ubidi_setPara(ubidi.getAlias(), inputString.getBuffer(), inputString.length(),
                                  paraLevels[i], NULL, errorCode);
                    const UBiDiLevel *actualLevels=ubidi_getLevels(ubidi.getAlias(), errorCode);
                    if(errorCode.logIfFailureAndReset("ubidi_setPara() or ubidi_getLevels()")) {
                        errln("Input line %d: %s", (int)lineNumber, line);
                        return;
                    }
                    paraLevelName=paraLevelNames[i];
                    if(!checkLevels(actualLevels, ubidi_getProcessedLength(ubidi.getAlias()))) {
                        // continue outerLoop;  does not exist in C++
                        // so just break out of the inner loop.
                        break;
                    }
                    if(!checkOrdering(ubidi.getAlias())) {
                        // continue outerLoop;  does not exist in C++
                        // so just break out of the inner loop.
                        break;
                    }
                }
            }
        }
    }
}

/*
*******************************************************************************
*
*   created on: 2013jul01
*   created by: Matitiahu Allouche

This function performs a conformance test for implementations of the
Unicode Bidirectional Algorithm, specified in UAX #9: Unicode
Bidirectional Algorithm, at http://www.unicode.org/unicode/reports/tr9/

Each test case is represented in a single line which is read from a file
named BidiCharacter.txt.  Empty, blank and comment lines may also appear
in this file.

The format of the test data is specified below.  Note that each test
case constitutes a single line of text; reordering is applied within a
single line and independently of a rendering engine, and rules L3 and L4
are out of scope.

The number sign '#' is the comment character: everything is ignored from
the occurrence of '#' until the end of the line,
Empty lines and lines containing only spaces and/or comments are ignored.

Lines which represent test cases consist of 4 or 5 fields separated by a
semicolon.  Each field consists of tokens separated by whitespace (space
or Tab).  Whitespace before and after semicolons is optional.

Field 0: A sequence of tokens where each token may be one of the following:
    - an hexadecimal number of at least 2 digits representing a code point
    - a bidi property value, which must be one of (case sensitive)
        L    (translated to 'l'),
        R    (translated to Hebrew Letter Alef),
        EN   (translated to '3'),
        ES   (translated to '-'),
        ET   (translated to '%'),
        AN   (translated to Arabic-Indic '9'),
        CS   (translated to ','),
        B    (translated to CR),
        S    (translated to Tab),
        WS   (translated to space),
        ON   (translated to '='),
        LRE, LRO,
        AL   (translated to Arabic Letter Thal),
        RLE, RLO, PDF,
        NSM  (translated to Hebrew Point Holam),
        BN   (translated to Soft Hyphen),
        FSI, LRI, RLI, PDI
    - a single character which represents itself

Field 1: A value representing the paragraph direction, as follows:
    - 0 represents left-to-right
    - 1 represents right-to-left
    - 2 represents auto-LTR according to rules P2 and P3 of the algorithm
    - 3 represents auto-RTL according to rules P2 and P3 of the algorithm
    - a negative number whose absolute value is taken as paragraph level;
      this may be useful to test cases where the embedding level approaches
      or exceeds the maximum embedding level.

Field 2: The resolved paragraph embedding level.  If the input (field 0)
         includes more than one paragraph, this field represents the
         resolved level of the first paragraph.

Field 3: An ordered list of resulting levels for each token in field 0
         (each token represents one source character).
         The UBA does not assign levels to certain characters (e.g. LRO);
         characters removed in rule X9 are indicated with an 'x'.

Field 4: An ordered list of indices showing the resulting visual ordering
         from left to right; characters with a resolved level of 'x' are
         skipped.  The number are zero-based.  Each index corresponds to
         a character in the reordered (visual) string. It represents the
         index of the source character in the input (field 0).
         This field is optional.  When it is absent, the visual ordering
         is not verified.

Examples:

# This is a comment line.
L L ON R ; 0 ; 0 ; 0 0 0 1 ; 0 1 2 3
L L ON R;0;0;0 0 0 1;0 1 2 3

# Note: in the next line, 'B' represents a block separator, not the letter 'B'.
LRE A B C PDF;2;0;x 2 0 0 x;1 2 3
# Note: in the next line, 'b' represents the letter 'b', not a block separator.
a b c 05d0 05d1 x ; 0 ; 0 ; 0 0 0 1 1 0 ; 0 1 2 4 3 5

a R R x ; 1 ; 1 ; 2 1 1 2
L L R R R B R R L L L B ON ON ; 3 ; 0 ; 0 0 1 1 1 0 1 1 2 2 2 1 1 1

*
*******************************************************************************
*/
void BiDiConformanceTest::TestBidiCharacterTest() {
    IcuTestErrorCode errorCode(*this, "TestBidiCharacterTest");
    const char *sourceTestDataPath=getSourceTestData(errorCode);
    if(errorCode.logIfFailureAndReset("unable to find the source/test/testdata "
                                      "folder (getSourceTestData())")) {
        return;
    }
    char bidiTestPath[400];
    strcpy(bidiTestPath, sourceTestDataPath);
    strcat(bidiTestPath, "BidiCharacterTest.txt");
    LocalStdioFilePointer bidiTestFile(fopen(bidiTestPath, "r"));
    if(bidiTestFile.isNull()) {
        errln("unable to open %s", bidiTestPath);
        return;
    }
    LocalUBiDiPointer ubidi(ubidi_open());
    lineNumber=0;
    levelsCount=0;
    orderingCount=0;
    errorCount=0;
    while(errorCount<20 && fgets(line, (int)sizeof(line), bidiTestFile.getAlias())!=NULL) {
        ++lineNumber;
        paraLevelName="N/A";
        inputString="N/A";
        // Remove trailing comments and whitespace.
        char *commentStart=strchr(line, '#');
        if(commentStart!=NULL) {
            *commentStart=0;
        }
        u_rtrim(line);
        const char *start=u_skipWhitespace(line);
        if(*start==0) {
            continue;  // Skip empty and comment-only lines.
        }
        if(!parseInputStringFromBiDiClasses(start, TRUE)) {
            continue;
        }
        start=u_skipWhitespace(start);
        if(*start!=';') {
            errorCount++;
            errln("\nError on line %d: Missing ; separator on line: %s", (int)lineNumber, line);
            continue;
        }
        start=u_skipWhitespace(start+1);
        char *end;
        int32_t paraDirection=(int32_t)strtol(start, &end, 10);
        UBiDiLevel paraLevel=UBIDI_MAX_EXPLICIT_LEVEL+2;
        if(paraDirection==0) {
            paraLevel=0;
            paraLevelName="LTR";
        }
        else if(paraDirection==1) {
            paraLevel=1;
            paraLevelName="RTL";
        }
        else if(paraDirection==2) {
            paraLevel=UBIDI_DEFAULT_LTR;
            paraLevelName="Auto/LTR";
        }
        else if(paraDirection==3) {
            paraLevel=UBIDI_DEFAULT_RTL;
            paraLevelName="Auto/RTL";
        }
        else if(paraDirection<0 && -paraDirection<=(UBIDI_MAX_EXPLICIT_LEVEL+1)) {
            paraLevel=(UBiDiLevel)(-paraDirection);
            sprintf(levelNameString, "%d", (int)paraLevel);
            paraLevelName=levelNameString;
        }
        if(end<=start || (!U_IS_INV_WHITESPACE(*end) && *end!=';' && *end!=0) ||
                         paraLevel==(UBIDI_MAX_EXPLICIT_LEVEL+2)) {
            errln("\nError on line %d: Input paragraph direction incorrect at %s", (int)lineNumber, start);
            printErrorLine();
            continue;
        }
        start=u_skipWhitespace(end);
        if(*start!=';') {
            errorCount++;
            errln("\nError on line %d: Missing ; separator on line: %s", (int)lineNumber, line);
            continue;
        }
        start++;
        uint32_t resolvedParaLevel=(uint32_t)strtoul(start, &end, 10);
        if(end<=start || (!U_IS_INV_WHITESPACE(*end) && *end!=';' && *end!=0) ||
           resolvedParaLevel>1) {
            errln("\nError on line %d: Resolved paragraph level incorrect at %s", (int)lineNumber, start);
            printErrorLine();
            continue;
        }
        start=u_skipWhitespace(end);
        if(*start!=';') {
            errorCount++;
            errln("\nError on line %d: Missing ; separator on line: %s", (int)lineNumber, line);
            return;
        }
        start++;
        if(!parseLevels(start)) {
            continue;
        }
        start=u_skipWhitespace(start);
        if(*start==';') {
            if(!parseOrdering(start+1)) {
                continue;
            }
        }
        else
            orderingCount=-1;

        ubidi_setPara(ubidi.getAlias(), inputString.getBuffer(), inputString.length(),
                      paraLevel, NULL, errorCode);
        const UBiDiLevel *actualLevels=ubidi_getLevels(ubidi.getAlias(), errorCode);
        if(errorCode.logIfFailureAndReset("ubidi_setPara() or ubidi_getLevels()")) {
            errln("Input line %d: %s", (int)lineNumber, line);
            continue;
        }
        UBiDiLevel actualLevel;
        if((actualLevel=ubidi_getParaLevel(ubidi.getAlias()))!=resolvedParaLevel) {
            printErrorLine();
            errln("\nError on line %d: Wrong resolved paragraph level; expected %d actual %d",
                   (int)lineNumber, resolvedParaLevel, actualLevel);
            continue;
        }
        if(!checkLevels(actualLevels, ubidi_getProcessedLength(ubidi.getAlias()))) {
            continue;
        }
        if(orderingCount>=0 && !checkOrdering(ubidi.getAlias())) {
            continue;
        }
    }
}

static UChar printLevel(UBiDiLevel level) {
    if(level<UBIDI_DEFAULT_LTR) {
        return 0x30+level;
    } else {
        return 0x78;  // 'x'
    }
}

static uint32_t getDirectionBits(const UBiDiLevel actualLevels[], int32_t actualCount) {
    uint32_t actualDirectionBits=0;
    for(int32_t i=0; i<actualCount; ++i) {
        actualDirectionBits|=(1<<(actualLevels[i]&1));
    }
    return actualDirectionBits;
}

UBool BiDiConformanceTest::checkLevels(const UBiDiLevel actualLevels[], int32_t actualCount) {
    UBool isOk=TRUE;
    if(levelsCount!=actualCount) {
        errln("\nError on line %d: Wrong number of level values; expected %d actual %d",
              (int)lineNumber, (int)levelsCount, (int)actualCount);
        isOk=FALSE;
    } else {
        for(int32_t i=0; i<actualCount; ++i) {
            if(levels[i]!=actualLevels[i] && levels[i]<UBIDI_DEFAULT_LTR) {
                if(directionBits!=3 && directionBits==getDirectionBits(actualLevels, actualCount)) {
                    // ICU used a shortcut:
                    // Since the text is unidirectional, it did not store the resolved
                    // levels but just returns all levels as the paragraph level 0 or 1.
                    // The reordering result is the same, so this is fine.
                    break;
                } else {
                    errln("\nError on line %d: Wrong level value at index %d; expected %d actual %d",
                          (int)lineNumber, (int)i, levels[i], actualLevels[i]);
                    isOk=FALSE;
                    break;
                }
            }
        }
    }
    if(!isOk) {
        printErrorLine();
        UnicodeString els("Expected levels:   ");
        int32_t i;
        for(i=0; i<levelsCount; ++i) {
            els.append((UChar)0x20).append(printLevel(levels[i]));
        }
        UnicodeString als("Actual   levels:   ");
        for(i=0; i<actualCount; ++i) {
            als.append((UChar)0x20).append(printLevel(actualLevels[i]));
        }
        errln(els);
        errln(als);
    }
    return isOk;
}

// Note: ubidi_setReorderingOptions(ubidi, UBIDI_OPTION_REMOVE_CONTROLS);
// does not work for custom BiDi class assignments
// and anyway also removes LRM/RLM/ZWJ/ZWNJ which is not desirable here.
// Therefore we just skip the indexes for BiDi controls while comparing
// with the expected ordering that has them omitted.
UBool BiDiConformanceTest::checkOrdering(UBiDi *ubidi) {
    UBool isOk=TRUE;
    IcuTestErrorCode errorCode(*this, "checkOrdering()");
    int32_t resultLength=ubidi_getResultLength(ubidi);  // visual length including BiDi controls
    int32_t i, visualIndex;
    // Note: It should be faster to call ubidi_countRuns()/ubidi_getVisualRun()
    // and loop over each run's indexes, but that seems unnecessary for this test code.
    for(i=visualIndex=0; i<resultLength; ++i) {
        int32_t logicalIndex=ubidi_getLogicalIndex(ubidi, i, errorCode);
        if(errorCode.logIfFailureAndReset("ubidi_getLogicalIndex()")) {
            errln("Input line %d: %s", (int)lineNumber, line);
            return FALSE;
        }
        if(levels[logicalIndex]>=UBIDI_DEFAULT_LTR) {
            continue;  // BiDi control, omitted from expected ordering.
        }
        if(visualIndex<orderingCount && logicalIndex!=ordering[visualIndex]) {
            errln("\nError on line %d: Wrong ordering value at visual index %d; expected %d actual %d",
                  (int)lineNumber, (int)visualIndex, ordering[visualIndex], logicalIndex);
            isOk=FALSE;
            break;
        }
        ++visualIndex;
    }
    // visualIndex is now the visual length minus the BiDi controls,
    // which should match the length of the BidiTest.txt ordering.
    if(isOk && orderingCount!=visualIndex) {
        errln("\nError on line %d: Wrong number of ordering values; expected %d actual %d",
              (int)lineNumber, (int)orderingCount, (int)visualIndex);
        isOk=FALSE;
    }
    if(!isOk) {
        printErrorLine();
        UnicodeString eord("Expected ordering: ");
        for(i=0; i<orderingCount; ++i) {
            eord.append((UChar)0x20).append((UChar)(0x30+ordering[i]));
        }
        UnicodeString aord("Actual   ordering: ");
        for(i=0; i<resultLength; ++i) {
            int32_t logicalIndex=ubidi_getLogicalIndex(ubidi, i, errorCode);
            if(levels[logicalIndex]<UBIDI_DEFAULT_LTR) {
                aord.append((UChar)0x20).append((UChar)(0x30+logicalIndex));
            }
        }
        errln(eord);
        errln(aord);
    }
    return isOk;
}

void BiDiConformanceTest::printErrorLine() {
    ++errorCount;
    errln("Input line %5d:   %s", (int)lineNumber, line);
    errln(UnicodeString("Input string:       ")+inputString);
    errln("Para level:         %s", paraLevelName);
}
