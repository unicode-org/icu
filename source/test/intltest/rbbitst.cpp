/********************************************************************
 * COPYRIGHT:
 * Copyright (c) 1999-2004, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/************************************************************************
*   Date        Name        Description
*   12/15/99    Madhu        Creation.
*   01/12/2000  Madhu        Updated for changed API and added new tests
************************************************************************/

#include "unicode/utypes.h"

#if !UCONFIG_NO_BREAK_ITERATION

#include "unicode/utypes.h"
#include "unicode/brkiter.h"
#include "unicode/rbbi.h"
#include "unicode/uchar.h"
#include "unicode/utf16.h"
#include "unicode/ucnv.h"
#include "unicode/schriter.h"
#include "unicode/uniset.h"
#include "unicode/regex.h"        // TODO: make conditional on regexp being built.
#include "unicode/ustring.h"

#include "intltest.h"
#include "rbbitst.h"
#include <string.h>
#include "uvector.h"
#include "uvectr32.h"
#include <string.h>
#include <stdio.h>
#include <stdlib.h>



//---------------------------------------------------------------------------
//
//   class BITestData   Holds a set of Break iterator test data and results
//                      Includes
//                         - the string data to be broken
//                         - a vector of the expected break positions.
//                         - a vector of source line numbers for the data,
//                               (to help see where errors occured.)
//                         - The expected break tag values.
//                         - Vectors of actual break positions and tag values.
//                         - Functions for comparing actual with expected and
//                            reporting errors.
//
//----------------------------------------------------------------------------
class BITestData {
public:
    UnicodeString    fDataToBreak;
    UVector          fExpectedBreakPositions;
    UVector          fExpectedTags;
    UVector          fLineNum;
    UVector          fActualBreakPositions;   // Test Results.
    UVector          fActualTags;

    BITestData(UErrorCode &status);
    void             addDataChunk(const char *data, int32_t tag, int32_t lineNum, UErrorCode status);
    void             checkResults(const char *heading, RBBITest *test);
    void             err(const char *heading, RBBITest *test, int32_t expectedIdx, int32_t actualIdx);
    void             clearResults();
};

//
// Constructor.
//
BITestData::BITestData(UErrorCode &status)
: fExpectedBreakPositions(status), fExpectedTags(status),  fLineNum(status), fActualBreakPositions(status),
  fActualTags(status)
{
};

//
// addDataChunk.   Add a section (non-breaking) piece if data to the test data.
//                 The macro form collects the line number, which is helpful
//                 when tracking down failures.
//
//                 A null data item is inserted at the start of each test's data
//                  to put the starting zero into the data list.  The position saved for
//                  each non-null item is its ending position.
//
#define ADD_DATACHUNK(td, data, tag, status)   td.addDataChunk(data, tag, __LINE__, status);
void BITestData::addDataChunk(const char *data, int32_t tag, int32_t lineNum, UErrorCode status) {
    if (U_FAILURE(status)) {return;}
    if (data != NULL) {
        fDataToBreak.append(CharsToUnicodeString(data));
    }
    fExpectedBreakPositions.addElement(fDataToBreak.length(), status);
    fExpectedTags.addElement(tag, status);
    fLineNum.addElement(lineNum, status);
};


//
//  checkResults.   Compare the actual and expected break positions, report any differences.
//
void BITestData::checkResults(const char *heading, RBBITest *test) {
    int32_t   expectedIndex = 0;
    int32_t   actualIndex = 0;

    for (;;) {
        // If we've run through both the expected and actual results vectors, we're done.
        //   break out of the loop.
        if (expectedIndex >= fExpectedBreakPositions.size() &&
            actualIndex   >= fActualBreakPositions.size()) {
            break;
        }


        if (expectedIndex >= fExpectedBreakPositions.size()) {
            err(heading, test, expectedIndex-1, actualIndex);
            actualIndex++;
            continue;
        }

        if (actualIndex >= fActualBreakPositions.size()) {
            err(heading, test, expectedIndex, actualIndex-1);
            expectedIndex++;
            continue;
        }

        if (fActualBreakPositions.elementAti(actualIndex) != fExpectedBreakPositions.elementAti(expectedIndex)) {
            err(heading, test, expectedIndex, actualIndex);
            // Try to resync the positions of the indices, to avoid a rash of spurious erros.
            if (fActualBreakPositions.elementAti(actualIndex) < fExpectedBreakPositions.elementAti(expectedIndex)) {
                actualIndex++;
            } else {
                expectedIndex++;
            }
            continue;
        }

        if (fActualTags.elementAti(actualIndex) != fExpectedTags.elementAti(expectedIndex)) {
            test->errln("%s, tag mismatch.  Test Line = %d, expected tag=%d, got %d",
                heading, fLineNum.elementAt(expectedIndex),
                fExpectedTags.elementAti(expectedIndex), fActualTags.elementAti(actualIndex));
        }

        actualIndex++;
        expectedIndex++;
    }
}

//
//  err   -  An error was found.  Report it, along with information about where the
//                                incorrectly broken test data appeared in the source file.
//
void    BITestData::err(const char *heading, RBBITest *test, int32_t expectedIdx, int32_t actualIdx)
{
    int32_t   expected = fExpectedBreakPositions.elementAti(expectedIdx);
    int32_t   actual   = fActualBreakPositions.elementAti(actualIdx);
    int32_t   o        = 0;
    int32_t   line     = fLineNum.elementAti(expectedIdx);
    if (expectedIdx > 0) {
        // The line numbers are off by one because a premature break occurs somewhere
        //    within the previous item, rather than at the start of the current (expected) item.
        //    We want to report the offset of the unexpected break from the start of
        //      this previous item.
        o    = actual - fExpectedBreakPositions.elementAti(expectedIdx-1);
    }
    if (actual < expected) {
        test->errln("%s unexpected break at offset %d in test item from line %d", heading, o, line);
    } else {
        test->errln("%s Failed to find break at end of item from line %d", heading, line);
    }
}


void BITestData::clearResults() {
    fActualBreakPositions.removeAllElements();
    fActualTags.removeAllElements();
}


//-----------------------------------------------------------------------------------
//
//    Cannned Test Characters
//
//-----------------------------------------------------------------------------------

static const UChar cannedTestArray[] = {
    0x0001, 0x0002, 0x0003, 0x0004, 0x0020, 0x0021, '\\', 0x0022, 0x0023, 0x0024, 0x0025, 0x0026, 0x0028, 0x0029, 0x002b, 0x002d, 0x0030, 0x0031,
    0x0032, 0x0033, 0x0034, 0x003c, 0x003d, 0x003e, 0x0041, 0x0042, 0x0043, 0x0044, 0x0045, 0x005b, 0x005d, 0x005e, 0x005f, 0x0060, 0x0061, 0x0062, 0x0063, 0x0064, 0x0065, 0x007b,
    0x007d, 0x007c, 0x002c, 0x00a0, 0x00a2,
    0x00a3, 0x00a4, 0x00a5, 0x00a6, 0x00a7, 0x00a8, 0x00a9, 0x00ab, 0x00ad, 0x00ae, 0x00af, 0x00b0, 0x00b2, 0x00b3,
    0x00b4, 0x00b9, 0x00bb, 0x00bc, 0x00bd, 0x02b0, 0x02b1, 0x02b2, 0x02b3, 0x02b4, 0x0300, 0x0301, 0x0302, 0x0303,
    0x0304, 0x05d0, 0x05d1, 0x05d2, 0x05d3, 0x05d4, 0x0903, 0x093e, 0x093f, 0x0940, 0x0949, 0x0f3a, 0x0f3b, 0x2000,
    0x2001, 0x2002, 0x200c, 0x200d, 0x200e, 0x200f, 0x2010, 0x2011, 0x2012, 0x2028, 0x2029, 0x202a, 0x203e, 0x203f,
    0x2040, 0x20dd, 0x20de, 0x20df, 0x20e0, 0x2160, 0x2161, 0x2162, 0x2163, 0x2164, 0x0000
};

static UnicodeString* cannedTestChars = 0;

#define  halfNA     "\\u0928\\u094d\\u200d"
#define  halfSA     "\\u0938\\u094d\\u200d"
#define  halfCHA    "\\u091a\\u094d\\u200d"
#define  halfKA     "\\u0915\\u094d\\u200d"
#define  deadTA     "\\u0924\\u094d"

//--------------------------------------------------------------------------------------
//
//    RBBITest    constructor and destructor
//
//--------------------------------------------------------------------------------------

RBBITest::RBBITest() {
    UnicodeString temp(cannedTestArray);
    cannedTestChars = new UnicodeString();
    *cannedTestChars += (UChar)0x0000;
    *cannedTestChars += temp;
}


RBBITest::~RBBITest() {
    delete cannedTestChars;
}


static const int T_NUMBER = 100;
static const int T_LETTER = 200;
static const int T_H_OR_K = 300;
static const int T_IDEO   = 400;






//--------------------------------------------------------------------
//Testing the BreakIterator for devanagari script
//--------------------------------------------------------------------

#define deadRA   "\\u0930\\u094d"         /*deadform RA = devanagari RA + virama*/
#define deadPHA  "\\u092b\\u094d"         /*deadform PHA = devanagari PHA + virama*/
#define deadTTHA "\\u0920\\u094d"
#define deadPA   "\\u092a\\u094d"
#define deadSA   "\\u0938\\u094d"
#define visarga  "\\u0903"                /*devanagari visarga looks like a english colon*/






//-----------------------------------------------------------------------------------
//
//   Test for status {tag} return value from break rules.
//        TODO:  a more thorough test.
//
//-----------------------------------------------------------------------------------
void RBBITest::TestStatusReturn() {
     UnicodeString rulesString1 = "$Letters = [:L:];\n"
                                  "$Numbers = [:N:];\n"
                                  "$Letters+{1};\n"
                                  "$Numbers+{2};\n"
                                  "Help\\ {4}/me\\!;\n"
                                  "[^$Letters $Numbers];\n"
                                  "!.*;\n";
     UnicodeString testString1  = "abc123..abc Help me Help me!";
                                // 01234567890123456789012345678
     int32_t bounds1[]   = {0, 3, 6, 7, 8, 11, 12, 16, 17, 19, 20, 25, 27, 28, -1};
     int32_t brkStatus[] = {0, 1, 2, 0, 0,  1,  0,  1,  0,  1,  0,  4,  1,  0, -1};

     UErrorCode status=U_ZERO_ERROR;
     UParseError    parseError;

     RuleBasedBreakIterator *bi = new RuleBasedBreakIterator(rulesString1, parseError, status);
     if(U_FAILURE(status)) {
         errln("FAIL : in construction");
     } else {
         int32_t  pos;
         int32_t  i = 0;
         bi->setText(testString1);
         for (pos=bi->first(); pos!= BreakIterator::DONE; pos=bi->next()) {
             if (pos != bounds1[i]) {
                 errln("FAIL:  expected break at %d, got %d\n", bounds1[i], pos);
                 break;
             }

             int tag = bi->getRuleStatus();
             if (tag != brkStatus[i]) {
                 errln("FAIL:  break at %d, expected tag %d, got tag %d\n", pos, brkStatus[i], tag);
                 break;
             }
             i++;
         }
     }
     delete bi;
}


static void printStringBreaks(UnicodeString ustr, int expected[],
                              int expectedcount)
{
    UErrorCode status = U_ZERO_ERROR;
    char name[100];
    printf("code    alpha extend alphanum type line name\n");
    int j;
    for (j = 0; j < ustr.length(); j ++) {
        if (expectedcount > 0) {
            int k;
            for (k = 0; k < expectedcount; k ++) {
                if (j == expected[k]) {
                    printf("------------------------------------------------ %d\n",
                           j);
                }
            }
        }
        UChar32 c = ustr.char32At(j);
        if (c > 0xffff) {
            j ++;
        }
        u_charName(c, U_UNICODE_CHAR_NAME, name, 100, &status);
        printf("%7x %5d %6d %8d %4s %4s %s\n", (int)c, 
                           u_isUAlphabetic(c), 
                           u_hasBinaryProperty(c, UCHAR_GRAPHEME_EXTEND),
                           u_isalnum(c), 
                           u_getPropertyValueName(UCHAR_GENERAL_CATEGORY, 
                                                  u_charType(c), 
                                                  U_SHORT_PROPERTY_NAME), 
                           u_getPropertyValueName(UCHAR_LINE_BREAK, 
                                                  u_getIntPropertyValue(c, 
                                                             UCHAR_LINE_BREAK), 
                                                  U_SHORT_PROPERTY_NAME),
                           name);
    }
}

void RBBITest::TestThaiLineBreak() {
    UErrorCode status = U_ZERO_ERROR;
    BITestData thaiLineSelection(status);

    // \u0e2f-- the Thai paiyannoi character-- isn't a letter.  It's a symbol that
    // represents elided letters at the end of a long word.  It should be bound to
    // the end of the word and not treated as an independent punctuation mark.


    ADD_DATACHUNK(thaiLineSelection, NULL, 0, status);           // Break at start of data
    ADD_DATACHUNK(thaiLineSelection, "\\u0e2a\\u0e16\\u0e32\\u0e19\\u0e35\\u0e2f", 0, status);
    ADD_DATACHUNK(thaiLineSelection, "\\u0e08\\u0e30", 0, status);
    ADD_DATACHUNK(thaiLineSelection, "\\u0e23\\u0e30\\u0e14\\u0e21", 0, status);
    ADD_DATACHUNK(thaiLineSelection, "\\u0e40\\u0e08\\u0e49\\u0e32", 0, status);
//        ADD_DATACHUNK(thaiLineSelection, "\\u0e2b\\u0e19\\u0e49\\u0e32", 0, status);
//        ADD_DATACHUNK(thaiLineSelection, "\\u0e17\\u0e35\\u0e48", 0, status);
    ADD_DATACHUNK(thaiLineSelection, "\\u0e2b\\u0e19\\u0e49\\u0e32\\u0e17\\u0e35\\u0e48", 0, status);
    // the commented-out lines (I think) are the preferred result; this line is what our current dictionary is giving us
    ADD_DATACHUNK(thaiLineSelection, "\\u0e2d\\u0e2d\\u0e01", 0, status);
    ADD_DATACHUNK(thaiLineSelection, "\\u0e21\\u0e32", 0, status);
    ADD_DATACHUNK(thaiLineSelection, "\\u0e40\\u0e23\\u0e48\\u0e07", 0, status);
    ADD_DATACHUNK(thaiLineSelection, "\\u0e23\\u0e30\\u0e1a\\u0e32\\u0e22", 0, status);
    ADD_DATACHUNK(thaiLineSelection, "\\u0e2d\\u0e22\\u0e48\\u0e32\\u0e07", 0, status);
    ADD_DATACHUNK(thaiLineSelection, "\\u0e40\\u0e15\\u0e47\\u0e21", 0, status);

    // the one time where the paiyannoi occurs somewhere other than at the end
    // of a word is in the Thai abbrevation for "etc.", which both begins and
    // ends with a paiyannoi
    ADD_DATACHUNK(thaiLineSelection, "\\u0e2f\\u0e25\\u0e2f", 0, status);
    ADD_DATACHUNK(thaiLineSelection, "\\u0e17\\u0e35\\u0e48", 0, status);
    ADD_DATACHUNK(thaiLineSelection, "\\u0e19\\u0e31\\u0e49\\u0e19", 0, status);

    RuleBasedBreakIterator* e = (RuleBasedBreakIterator *)BreakIterator::createLineInstance(
        Locale("th"), status);
    if (U_FAILURE(status))
    {
        errln("Failed to create the BreakIterator for Thai locale in TestThaiLineBreak.\n");
        return;
    }

    generalIteratorTest(*e, thaiLineSelection);
    delete e;
}



void RBBITest::TestMixedThaiLineBreak()
{
    UErrorCode   status = U_ZERO_ERROR;
    BITestData   thaiLineSelection(status);

    ADD_DATACHUNK(thaiLineSelection, NULL, 0, status);           // Break at start of data

    // Arabic numerals should always be separated from surrounding Thai text
/*
        ADD_DATACHUNK(thaiLineSelection, "\\u0e04\\u0e48\\u0e32", 0, status);
        ADD_DATACHUNK(thaiLineSelection, "\\u0e40\\u0e07\\u0e34\\u0e19", 0, status);
        ADD_DATACHUNK(thaiLineSelection, "\\u0e1a\\u0e32\\u0e17", 0, status);
        ADD_DATACHUNK(thaiLineSelection, "\\u0e41\\u0e15\\u0e30", 0, status);
        ADD_DATACHUNK(thaiLineSelection, "\\u0e23\\u0e30\\u0e14\\u0e31\\u0e1a", 0, status);
        thaiLineSelection->addElement("39");
        ADD_DATACHUNK(thaiLineSelection, "\\u0e1a\\u0e32\\u0e17 ", 0, status);

        // words in non-Thai scripts should always be separated from surrounding Thai text
        ADD_DATACHUNK(thaiLineSelection, "\\u0e17\\u0e14", 0, status);
        ADD_DATACHUNK(thaiLineSelection, "\\u0e2a\\u0e2d\\u0e1a", 0, status);
        thaiLineSelection->addElement("Java");
        ADD_DATACHUNK(thaiLineSelection, "\\u0e1a\\u0e19", 0, status);
        ADD_DATACHUNK(thaiLineSelection, "\\u0e40\\u0e04\\u0e23\\u0e37\\u0e48\\u0e2d\\u0e07", 0, status);
        ADD_DATACHUNK(thaiLineSelection, "\\u0e44\\u0e2d\\u0e1a\\u0e35\\u0e40\\u0e2d\\u0e47\\u0e21 ", 0, status);

        // Thai numerals should always be separated from the text surrounding them
        ADD_DATACHUNK(thaiLineSelection, "\\u0e04\\u0e48\\u0e32", 0, status);
        ADD_DATACHUNK(thaiLineSelection, "\\u0e40\\u0e07\\u0e34\\u0e19", 0, status);
        ADD_DATACHUNK(thaiLineSelection, "\\u0e1a\\u0e32\\u0e17", 0, status);
        ADD_DATACHUNK(thaiLineSelection, "\\u0e41\\u0e15\\u0e30", 0, status);
        ADD_DATACHUNK(thaiLineSelection, "\\u0e23\\u0e30\\u0e14\\u0e31\\u0e1a", 0, status);
        ADD_DATACHUNK(thaiLineSelection, "\\u0e53\\u0e59", 0, status);
        ADD_DATACHUNK(thaiLineSelection, "\\u0e1a\\u0e32\\u0e17 ", 0, status);

        // Thai text should interact correctly with punctuation and symbols
        ADD_DATACHUNK(thaiLineSelection, "\\u0e44\\u0e2d\\u0e1a\\u0e35\\u0e40\\u0e2d\\u0e47\\u0e21", 0, status);
//        ADD_DATACHUNK(thaiLineSelection, "(\\u0e1b\\u0e23\\u0e30\\u0e40\\u0e17\\u0e28", 0, status);
//        ADD_DATACHUNK(thaiLineSelection, "\\u0e44\\u0e17\\u0e22)", 0, status);
ADD_DATACHUNK(thaiLineSelection, "(\\u0e1b\\u0e23\\u0e30\\u0e40\\u0e17\\u0e28\\u0e44\\u0e17\\u0e22)", 0, status);
// I believe the commented-out reading above to be the correct one, but this is what passes with our current dictionary
        ADD_DATACHUNK(thaiLineSelection, "\\u0e08\\u0e33\\u0e01\\u0e31\\u0e14", 0, status);
        ADD_DATACHUNK(thaiLineSelection, "\\u0e40\\u0e1b\\u0e34\\u0e14", 0, status);
        ADD_DATACHUNK(thaiLineSelection, "\\u0e15\\u0e31\\u0e27\"", 0, status);
*/

    // The Unicode Linebreak TR says do not break before or after quotes.
    //    So this test is changed ot not break around the quote.
    //    TODO:  should Thai break around the around the quotes, like the original behavior here?
//    ADD_DATACHUNK(thaiLineSelection, "\\u0e2e\\u0e32\\u0e23\\u0e4c\\u0e14\\u0e14\\u0e34\\u0e2a\\u0e01\\u0e4c\"", 0, status);
//    ADD_DATACHUNK(thaiLineSelection, "\\u0e23\\u0e38\\u0e48\\u0e19", 0, status);
      ADD_DATACHUNK(thaiLineSelection, "\\u0e2e\\u0e32\\u0e23\\u0e4c\\u0e14\\u0e14\\u0e34\\u0e2a\\u0e01\\u0e4c\""
                                                         "\\u0e23\\u0e38\\u0e48\\u0e19", 0, status);

    ADD_DATACHUNK(thaiLineSelection, "\\u0e43\\u0e2b\\u0e21\\u0e48", 0, status);
    ADD_DATACHUNK(thaiLineSelection, "\\u0e40\\u0e14\\u0e37\\u0e2d\\u0e19\\u0e21\\u0e34.", 0, status);
    ADD_DATACHUNK(thaiLineSelection, "\\u0e22.", 0, status);
    ADD_DATACHUNK(thaiLineSelection, "\\u0e19\\u0e35\\u0e49", 0, status);
    ADD_DATACHUNK(thaiLineSelection, "\\u0e23\\u0e32\\u0e04\\u0e32", 0, status);
    ADD_DATACHUNK(thaiLineSelection, "$200", 0, status);
    ADD_DATACHUNK(thaiLineSelection, "\\u0e40\\u0e17\\u0e48\\u0e32", 0, status);
    ADD_DATACHUNK(thaiLineSelection, "\\u0e19\\u0e31\\u0e49\\u0e19 ", 0, status);
    ADD_DATACHUNK(thaiLineSelection, "(\"\\u0e2e\\u0e32\\u0e23\\u0e4c\\u0e14\\u0e14\\u0e34\\u0e2a\\u0e01\\u0e4c\").", 0, status);

    RuleBasedBreakIterator* e = (RuleBasedBreakIterator *)BreakIterator::createLineInstance(Locale("th"), status);
    if (U_FAILURE(status))
    {
        errln("Failed to create the BreakIterator for Thai locale in TestMixedThaiLineBreak.\n");
        return;
    }


    generalIteratorTest(*e, thaiLineSelection);
    delete e;
}


void RBBITest::TestMaiyamok()
{
    UErrorCode status = U_ZERO_ERROR;
    BITestData   thaiLineSelection(status);
    ADD_DATACHUNK(thaiLineSelection, NULL, 0, status);           // Break at start of data
    // the Thai maiyamok character is a shorthand symbol that means "repeat the previous
    // word".  Instead of appearing as a word unto itself, however, it's kept together
    // with the word before it
    ADD_DATACHUNK(thaiLineSelection, "\\u0e44\\u0e1b\\u0e46", 0, status);
    ADD_DATACHUNK(thaiLineSelection, "\\u0e21\\u0e32\\u0e46", 0, status);
    ADD_DATACHUNK(thaiLineSelection, "\\u0e23\\u0e30\\u0e2b\\u0e27\\u0e48\\u0e32\\u0e07", 0, status);
    ADD_DATACHUNK(thaiLineSelection, "\\u0e01\\u0e23\\u0e38\\u0e07\\u0e40\\u0e17\\u0e1e", 0, status);
    ADD_DATACHUNK(thaiLineSelection, "\\u0e41\\u0e25\\u0e30", 0, status);
    ADD_DATACHUNK(thaiLineSelection, "\\u0e40\\u0e03\\u0e35\\u0e22\\u0e07", 0, status);
    ADD_DATACHUNK(thaiLineSelection, "\\u0e43\\u0e2b\\u0e21\\u0e48", 0, status);

    RuleBasedBreakIterator* e = (RuleBasedBreakIterator *)BreakIterator::createLineInstance(
        Locale("th"), status);

    if (U_FAILURE(status))
    {
        errln("Failed to create the BreakIterator for Thai locale in TestMaiyamok.\n");
        return;
    }
    generalIteratorTest(*e, thaiLineSelection);
    delete e;
}

void RBBITest::TestThaiWordBreak() {
    UErrorCode status = U_ZERO_ERROR;
    BITestData   thaiWordSelection(status);

    ADD_DATACHUNK(thaiWordSelection, NULL, 0, status);           // Break at start of data
    ADD_DATACHUNK(thaiWordSelection, "\\u0E1A\\u0E17", 0, status); //2
    ADD_DATACHUNK(thaiWordSelection, "\\u0E17\\u0E35\\u0E48", 0, status); //5
    ADD_DATACHUNK(thaiWordSelection, "\\u0E51", 0, status); //6
    ADD_DATACHUNK(thaiWordSelection, "\\u0E1E\\u0E32\\u0E22\\u0E38", 0, status); //10
    ADD_DATACHUNK(thaiWordSelection, "\\u0E44\\u0E0B\\u0E42\\u0E04\\u0E25\\u0E19", 0, status); //16
    ADD_DATACHUNK(thaiWordSelection, "\\u000D\\u000A", 0, status); //18

    // This is the correct result
    //ADD_DATACHUNK(thaiWordSelection, "\\u0E42\\u0E14\\u0E42\\u0E23\\u0E18\\u0E35", 0, status); //24
    //ADD_DATACHUNK(thaiWordSelection, "\\u0E2D\\u0E32\\u0E28\\u0E31\\u0E22", 0, status); //29

    // and this is what the dictionary does...
    ADD_DATACHUNK(thaiWordSelection, "\\u0E42\\u0E14", 0, status); // 20
    ADD_DATACHUNK(thaiWordSelection, "\\u0E42\\u0E23\\u0E18\\u0E35\\u0E2D\\u0E32\\u0E28\\u0E31\\u0E22", 0, status); //29

    ADD_DATACHUNK(thaiWordSelection, "\\u0E2D\\u0E22\\u0E39\\u0E48", 0, status); //33

    // This is the correct result
    //ADD_DATACHUNK(thaiWordSelection, "\\u0E17\\u0E48\\u0E32\\u0E21", 0, status); //37
    //ADD_DATACHUNK(thaiWordSelection, "\\u0E01\\u0E25\\u0E32\\u0E07", 0, status); //41

    // and this is what the dictionary does
    ADD_DATACHUNK(thaiWordSelection, "\\u0E17\\u0E48\\u0E32\\u0E21\\u0E01\\u0E25\\u0E32\\u0E07", 0, status); //41

    ADD_DATACHUNK(thaiWordSelection, "\\u0E17\\u0E38\\u0E48\\u0E07", 0, status); //45
    ADD_DATACHUNK(thaiWordSelection, "\\u0E43\\u0E2B\\u0E0D\\u0E48", 0, status); //49
    ADD_DATACHUNK(thaiWordSelection, "\\u0E43\\u0E19", 0, status); //51

    // This is the correct result
    //ADD_DATACHUNK(thaiWordSelection, "\\u0E41\\u0E04\\u0E19\\u0E0B\\u0E31\\u0E2A", 0, status); //57
    //ADD_DATACHUNK(thaiWordSelection, "\\u0E01\\u0E31\\u0E1A", 0, status); //60

    // and this is what the dictionary does
    ADD_DATACHUNK(thaiWordSelection, "\\u0E41\\u0E04\\u0E19", 0, status); // 54
    ADD_DATACHUNK(thaiWordSelection, "\\u0E0B\\u0E31\\u0E2A\\u0E01\\u0E31\\u0E1A", 0, status); //60

    ADD_DATACHUNK(thaiWordSelection, "\\u0E25\\u0E38\\u0E07", 0, status); //63

    // This is the correct result
    //ADD_DATACHUNK(thaiWordSelection, "\\u0E40\\u0E2E\\u0E19\\u0E23\\u0E35", 0, status); //68
    //ADD_DATACHUNK(thaiWordSelection, "\\u0E0A\\u0E32\\u0E27", 0, status); //71
    //ADD_DATACHUNK(thaiWordSelection, "\\u0E44\\u0E23\\u0E48", 0, status); //74
    //ADD_DATACHUNK(thaiWordSelection, "\\u0E41\\u0E25\\u0E30", 0, status); //77

    // and this is what the dictionary does
    ADD_DATACHUNK(thaiWordSelection, "\\u0E40\\u0E2E", 0, status); // 65
    ADD_DATACHUNK(thaiWordSelection, "\\u0E19\\u0E23\\u0E35\\u0E0A\\u0E32\\u0E27\\u0E44\\u0E23\\u0E48\\u0E41\\u0E25\\u0E30", 0, status); //77

    RuleBasedBreakIterator* e = (RuleBasedBreakIterator *)BreakIterator::createWordInstance(
        Locale("th"), status);
    if (U_FAILURE(status))
    {
        errln("Failed to create the BreakIterator for Thai locale in TestThaiWordBreak.\n");
        return;
    }

    generalIteratorTest(*e, thaiWordSelection);
    delete e;
}


void RBBITest::TestBug3818() {
    UErrorCode  status = U_ZERO_ERROR;

    // Four Thai words...
    static const UChar thaiWordData[] = {  0x0E43,0x0E2B,0x0E0D,0x0E48, 0x0E43,0x0E2B,0x0E0D,0x0E48, 
                                           0x0E43,0x0E2B,0x0E0D,0x0E48, 0x0E43,0x0E2B,0x0E0D,0x0E48, 0 }; 
    UnicodeString  thaiStr(thaiWordData);

    RuleBasedBreakIterator* bi = 
        (RuleBasedBreakIterator *)BreakIterator::createWordInstance(Locale("th"), status);
    if (U_FAILURE(status) || bi == NULL) {
        errln("Fail at file %s, line %d, status = %s", __FILE__, __LINE__, u_errorName(status));
        return;
    }
    bi->setText(thaiStr);

    int32_t  startOfSecondWord = bi->following(1);
    if (startOfSecondWord != 4) {
        errln("Fail at file %s, line %d expected start of word at 4, got %d",
            __FILE__, __LINE__, startOfSecondWord);
    }
    startOfSecondWord = bi->following(0);
    if (startOfSecondWord != 4) {
        errln("Fail at file %s, line %d expected start of word at 4, got %d",
            __FILE__, __LINE__, startOfSecondWord);
    }
}

//---------------------------------------------
// runIndexedTest
//---------------------------------------------

void RBBITest::runIndexedTest( int32_t index, UBool exec, const char* &name, char* params )
{
    if (exec) logln("TestSuite RuleBasedBreakIterator: ");
    
    switch (index) {
        case 0: name = "TestBug4153072";
            if(exec) TestBug4153072();                         break;
        case 1: name = "TestJapaneseLineBreak";
            if(exec) TestJapaneseLineBreak();                 break;
        case 2: name = "TestStatusReturn";
            if(exec) TestStatusReturn();                       break;

        case 3: name = "TestLineBreakData";
            if(exec) TestLineBreakData();                      break;
        case 4: name = "TestEmptyString";
            if(exec) TestEmptyString();                        break;

        case 5: name = "TestGetAvailableLocales";
            if(exec) TestGetAvailableLocales();                break;

        case 6: name = "TestGetDisplayName";
            if(exec) TestGetDisplayName();                     break;

        case 7: name = "TestEndBehaviour";
            if(exec) TestEndBehaviour();                       break;
        case 8: name = "TestMixedThaiLineBreak";
             if(exec) TestMixedThaiLineBreak();                break;
        case 9: name = "TestThaiWordBreak";
             if(exec) TestThaiWordBreak();                     break;
        case 10: name = "TestThaiLineBreak";
             if(exec) TestThaiLineBreak();                     break;
        case 11: name = "TestMaiyamok";
             if(exec) TestMaiyamok();                          break;
        case 12: name = "TestWordBreaks";
             if(exec) TestWordBreaks();                        break;
        case 13: name = "TestWordBoundary";
             if(exec) TestWordBoundary();                      break;
        case 14: name = "TestLineBreaks";
             if(exec) TestLineBreaks();                        break;
        case 15: name = "TestSentBreaks";
             if(exec) TestSentBreaks();                        break;
        case 16: name = "TestExtended";
             if(exec) TestExtended();                          break;
        case 17: name = "TestMonkey";
             if(exec) {
 #if !UCONFIG_NO_REGULAR_EXPRESSIONS
               TestMonkey(params);
 #else
               logln("skipping TestMonkey (UCONFIG_NO_REGULAR_EXPRESSIONS)");
 #endif
             }
                                                               break;
        case 18: name = "TestBug3818";
            if(exec) TestBug3818();                            break;

        default: name = ""; break; //needed to end loop
    }
}


//----------------------------------------------------------------------------
//
// generalIteratorTest      Given a break iterator and a set of test data,
//                          Run the tests and report the results.
//
//----------------------------------------------------------------------------
void RBBITest::generalIteratorTest(RuleBasedBreakIterator& bi, BITestData &td)
{

    bi.setText(td.fDataToBreak);

    testFirstAndNext(bi, td);

    testLastAndPrevious(bi, td);

    testFollowing(bi, td);
    testPreceding(bi, td);
    testIsBoundary(bi, td);
    doMultipleSelectionTest(bi, td);
}


//
//   testFirstAndNext.   Run the iterator forwards in the obvious first(), next()
//                       kind of loop.
//
void RBBITest::testFirstAndNext(RuleBasedBreakIterator& bi, BITestData &td)
{
    UErrorCode  status = U_ZERO_ERROR;
    int32_t     p;
    int32_t     lastP = -1;
    int32_t     tag;

    logln("Test first and next");
    bi.setText(td.fDataToBreak);
    td.clearResults();

    for (p=bi.first(); p!=RuleBasedBreakIterator::DONE; p=bi.next()) {
        td.fActualBreakPositions.addElement(p, status);  // Save result.
        tag = bi.getRuleStatus();
        td.fActualTags.addElement(tag, status);
        if (p <= lastP) {
            // If the iterator is not making forward progress, stop.
            //  No need to raise an error here, it'll be detected in the normal check of results.
            break;
        }
        lastP = p;
    }
    td.checkResults("testFirstAndNext", this);
}


//
//  TestLastAndPrevious.   Run the iterator backwards, starting with last().
//
void  RBBITest::testLastAndPrevious(RuleBasedBreakIterator& bi,  BITestData &td)
{
    UErrorCode  status = U_ZERO_ERROR;
    int32_t     p;
    int32_t     lastP  = 0x7ffffffe;
    int32_t     tag;

    logln("Test first and next");
    bi.setText(td.fDataToBreak);
    td.clearResults();

    for (p=bi.last(); p!=RuleBasedBreakIterator::DONE; p=bi.previous()) {
        // Save break position.  Insert it at start of vector of results, shoving
        //    already-saved results further towards the end.
        td.fActualBreakPositions.insertElementAt(p, 0, status);
        // bi.previous();   // TODO:  Why does this fix things up????
        // bi.next();
        tag = bi.getRuleStatus();
        td.fActualTags.insertElementAt(tag, 0, status);
        if (p >= lastP) {
            // If the iterator is not making progress, stop.
            //  No need to raise an error here, it'll be detected in the normal check of results.
            break;
        }
        lastP = p;
    }
    td.checkResults("testLastAndPrevious", this);
}


void RBBITest::testFollowing(RuleBasedBreakIterator& bi, BITestData &td)
{
    UErrorCode  status = U_ZERO_ERROR;
    int32_t     p;
    int32_t     tag;
    int32_t     lastP  = -2;     // A value that will never be returned as a break position.
                                 //   cannot be -1; that is returned for DONE.
    int         i;

    logln("testFollowing():");
    bi.setText(td.fDataToBreak);
    td.clearResults();

    // Save the starting point, since we won't get that out of following.
    p = bi.first();
    td.fActualBreakPositions.addElement(p, status);  // Save result.
    tag = bi.getRuleStatus();
    td.fActualTags.addElement(tag, status);

    for (i = 0; i <= td.fDataToBreak.length()+1; i++) {
        p = bi.following(i);
        if (p != lastP) {
            if (p == RuleBasedBreakIterator::DONE) {
                break;
            }
            // We've reached a new break position.  Save it.
            td.fActualBreakPositions.addElement(p, status);  // Save result.
            tag = bi.getRuleStatus();
            td.fActualTags.addElement(tag, status);
            lastP = p;
        }
    }
    // The loop normally exits by means of the break in the middle.
    // Make sure that the index was at the correct position for the break iterator to have
    //   returned DONE.
    if (i != td.fDataToBreak.length()) {
        errln("testFollowing():  iterator returned DONE prematurely.");
    }

    // Full check of all results.
    td.checkResults("testFollowing", this);
}



void RBBITest::testPreceding(RuleBasedBreakIterator& bi,  BITestData &td) {
    UErrorCode  status = U_ZERO_ERROR;
    int32_t     p;
    int32_t     tag;
    int32_t     lastP  = 0x7ffffffe;
    int         i;

    logln("testPreceding():");
    bi.setText(td.fDataToBreak);
    td.clearResults();

    p = bi.last();
    td.fActualBreakPositions.addElement(p, status);
    tag = bi.getRuleStatus();
    td.fActualTags.addElement(tag, status);

    for (i = td.fDataToBreak.length(); i>=-1; i--) {
        p = bi.preceding(i);
        if (p != lastP) {
            if (p == RuleBasedBreakIterator::DONE) {
                break;
            }
            // We've reached a new break position.  Save it.
            td.fActualBreakPositions.insertElementAt(p, 0, status);
            lastP = p;
            tag = bi.getRuleStatus();
            td.fActualTags.insertElementAt(tag, 0, status);
        }
    }
    // The loop normally exits by means of the break in the middle.
    // Make sure that the index was at the correct position for the break iterator to have
    //   returned DONE.
    if (i != 0) {
        errln("testPreceding():  iterator returned DONE prematurely.");
    }

    // Full check of all results.
    td.checkResults("testPreceding", this);
}



void RBBITest::testIsBoundary(RuleBasedBreakIterator& bi,  BITestData &td) {
    UErrorCode  status = U_ZERO_ERROR;
    int         i;
    int32_t     tag;

    logln("testIsBoundary():");
    bi.setText(td.fDataToBreak);
    td.clearResults();

    for (i = 0; i <= td.fDataToBreak.length(); i++) {
        if (bi.isBoundary(i)) {
            td.fActualBreakPositions.addElement(i, status);  // Save result.
            tag = bi.getRuleStatus();
            td.fActualTags.addElement(tag, status);
        }
    }
    td.checkResults("testIsBoundary: ", this);
}



void RBBITest::doMultipleSelectionTest(RuleBasedBreakIterator& iterator, BITestData &td)
{
    iterator.setText(td.fDataToBreak);

    RuleBasedBreakIterator* testIterator =(RuleBasedBreakIterator*)iterator.clone();
    int32_t offset = iterator.first();
    int32_t testOffset;
    int32_t count = 0;

    logln("doMultipleSelectionTest text of length: %d", td.fDataToBreak.length());

    if (*testIterator != iterator)
        errln("clone() or operator!= failed: two clones compared unequal");

    do {
        testOffset = testIterator->first();
        testOffset = testIterator->next(count);
        if (offset != testOffset)
            errln(UnicodeString("next(n) and next() not returning consistent results: for step ") + count + ", next(n) returned " + testOffset + " and next() had " + offset);

        if (offset != RuleBasedBreakIterator::DONE) {
            count++;
            offset = iterator.next();

            if (offset != RuleBasedBreakIterator::DONE && *testIterator == iterator)
                errln("operator== failed: Two unequal iterators compared equal.");
        }
    } while (offset != RuleBasedBreakIterator::DONE);

    // now do it backwards...
    offset = iterator.last();
    count = 0;

    do {
        testOffset = testIterator->last();
        testOffset = testIterator->next(count);   // next() with a negative arg is same as previous
        if (offset != testOffset)
            errln(UnicodeString("next(n) and next() not returning consistent results: for step ") + count + ", next(n) returned " + testOffset + " and next() had " + offset);

        if (offset != RuleBasedBreakIterator::DONE) {
            count--;
            offset = iterator.previous();
        }
    } while (offset != RuleBasedBreakIterator::DONE);

    delete testIterator;
}



//--------------------------------------------------------------------------------------------
//
//    Break Iterator Invariants Tests
//
//--------------------------------------------------------------------------------------------

void RBBITest::TestCharacterInvariants()
{
    UErrorCode status = U_ZERO_ERROR;
    BreakIterator *e = BreakIterator::createCharacterInstance(Locale::getDefault(), status);
    if (U_FAILURE(status))
    {
        errln("Failed to create the BreakIterator for default locale in TestCharacterInvariants.\n");
        return;
    }
    UnicodeString s = *cannedTestChars + CharsToUnicodeString("\\u1100\\u1101\\u1102\\u1160\\u1161\\u1162\\u11a8\\u11a9\\u11aa");
    doBreakInvariantTest(*e, s);
    s = *cannedTestChars + CharsToUnicodeString("\\u1100\\u1101\\u1102\\u1160\\u1161\\u1162\\u11a8\\u11a9\\u11aa");
    doOtherInvariantTest(*e, s);
    delete e;
}


void RBBITest::TestWordInvariants()
{
    UErrorCode status = U_ZERO_ERROR;
    BreakIterator *e = BreakIterator::createWordInstance(Locale::getDefault(), status);
    if (U_FAILURE(status))
    {
        errln("Failed to create the BreakIterator for default locale in TestWordInvariants.\n");
        return;
    }
    UnicodeString s = *cannedTestChars + CharsToUnicodeString("\',.\\u3041\\u3042\\u3043\\u309b\\u309c\\u30a1\\u30a2\\u30a3\\u4e00\\u4e01\\u4e02");
    doBreakInvariantTest(*e, s);
    s = *cannedTestChars + CharsToUnicodeString("\',.\\u3041\\u3042\\u3043\\u309b\\u309c\\u30a1\\u30a2\\u30a3\\u4e00\\u4e01\\u4e02");
    doOtherInvariantTest(*e, s);
    delete e;
}


void RBBITest::TestSentenceInvariants()
{
    UErrorCode status = U_ZERO_ERROR;
    BreakIterator *e = BreakIterator::createSentenceInstance(Locale::getDefault(), status);
    if (U_FAILURE(status))
    {
        errln("Failed to create the BreakIterator for default locale in TestSentenceInvariant.\n");
        return;
    }
    UnicodeString s = *cannedTestChars + CharsToUnicodeString(".,\\u3001\\u3002\\u3041\\u3042\\u3043\\ufeff");
    doOtherInvariantTest(*e, s);
    delete e;
}




void RBBITest::doBreakInvariantTest(BreakIterator& tb, UnicodeString& testChars)
{
    UnicodeString work("aaa");
    int32_t errCount = 0, testCharsLen = testChars.length(), breaksLen;

    // a break should always occur after CR (unless followed by LF), LF, PS, and LS
    UnicodeString breaks = CharsToUnicodeString("\r\n\\u2029\\u2028");
    int32_t i, j;

    breaksLen = breaks.length();
    for (i = 0; i < breaksLen; i++) {
        UChar c1 = breaks[i];
        work.setCharAt(1, c1);
        for (j = 0; j < testCharsLen; j++) {
            UChar c0 = testChars[j];
            work.setCharAt(0, c0);
            int k;
            for (k = 0; k < testCharsLen; k++) {
                UChar c2 = testChars[k];
                work.setCharAt(2, c2);

                // if a cr is followed by lf, ps, ls or etx, don't do the check (that's
                // not supposed to work)
                if (c1 == '\r' && (c2 == '\n' || c2 == 0x2029
                        || c2 == 0x2028 || c2 == 0x0003))
                    continue;

                if (u_charType(c1) == U_CONTROL_CHAR &&
                    (u_charType(c2) == U_NON_SPACING_MARK ||
                     u_charType(c2) == U_ENCLOSING_MARK ||
                     u_charType(c2) == U_COMBINING_SPACING_MARK)
                    ) {
                    // Combining marks don't combine with controls.
                    //  TODO:  enhance test to verify that the break actually occurs,
                    //         not just ignore the case.
                    continue;
                }


                tb.setText(work);
                UBool seen2 = FALSE;
                int l;
                for (l = tb.first(); l != BreakIterator::DONE; l = tb.next()) {
                    if (l == 2) {
                        seen2 = TRUE;
                        break;
                    }
                }
                if (!seen2) {
                    printStringBreaks(work, NULL, 0); 
                    errln("No Break between \\U%04x and \\U%04x", c1, c2);
                    errCount++;
                    if (errCount >= 75)
                        return;
                }
            }
        }
    }
}



void RBBITest::doOtherInvariantTest(BreakIterator& tb, UnicodeString& testChars)
{
    UnicodeString work("a\r\na");
    int32_t errCount = 0, testCharsLen = testChars.length();
    int32_t i, j;
    int8_t type;

    // a break should never occur between CR and LF
    for (i = 0; i < testCharsLen; i++) {
        work.setCharAt(0, testChars[i]);
        for (j = 0; j < testCharsLen; j++) {
            work.setCharAt(3, testChars[j]);
            tb.setText(work);
            int32_t k;
            for (k = tb.first(); k != BreakIterator::DONE; k = tb.next())
                if (k == 2) {
                    errln("Break between CR and LF in string U\\%04x U\\%04x U\\%04x U\\%04x",
                        work[0], work[1], work[2], work[3]);
                    errCount++;
                    if (errCount >= 75)
                        return;
                }
        }
    }

    // a break should never occur before a non-spacing mark, unless the preceding
    // character is CR, LF, PS, or LS
    //   Or the general category == Control.
    work.remove();
    work += "aaaa";
    for (i = 0; i < testCharsLen; i++) {
        UChar c1 = testChars[i];
        if (c1 == '\n' || c1 == '\r' || c1 == 0x2029 || c1 == 0x2028 || c1 == 0x0003 ||
            u_charType(c1) == U_CONTROL_CHAR  ||  u_charType(c1) == U_FORMAT_CHAR) {
            continue;
        }
        work.setCharAt(1, c1);
        for (j = 0; j < testCharsLen; j++) {
            UChar c2 = testChars[j];
            type = u_charType(c2);
            if ((type != U_NON_SPACING_MARK) &&
                (type != U_ENCLOSING_MARK)) {
                continue;
            }
            work.setCharAt(2, c2);
            tb.setText(work);
            int k;
            for (k = tb.first(); k != BreakIterator::DONE; k = tb.next())
                if (k == 2) {
                    //errln("Break between U+" + UCharToUnicodeString(work[1])
                    //        + " and U+" + UCharToUnicodeString(work[2]));
                    errln("Unexpected Break between %6x and %6x", c1, c2);
                    errCount++;
                    if (errCount >= 75)
                        return;
                }
        }
    }
}




//---------------------------------------------
//
//     other tests
//
//---------------------------------------------
void RBBITest::TestEmptyString()
{
    UnicodeString text = "";
    UErrorCode status = U_ZERO_ERROR;

    BITestData x(status);
    ADD_DATACHUNK(x, "", 0, status);           // Break at start of data
    RuleBasedBreakIterator* bi = (RuleBasedBreakIterator *)BreakIterator::createLineInstance(Locale::getDefault(), status);
    if (U_FAILURE(status))
    {
        errln("Failed to create the BreakIterator for default locale in TestEmptyString.\n");
        return;
    }
    generalIteratorTest(*bi, x);
    delete bi;
}

void RBBITest::TestGetAvailableLocales()
{
    int32_t locCount = 0;
    const Locale* locList = BreakIterator::getAvailableLocales(locCount);

    if (locCount == 0)
        errln("getAvailableLocales() returned an empty list!");
    // Just make sure that it's returning good memory.
    int32_t i;
    for (i = 0; i < locCount; ++i) {
        logln(locList[i].getName());
    }
}

//Testing the BreakIterator::getDisplayName() function
void RBBITest::TestGetDisplayName()
{
    UnicodeString   result;

    BreakIterator::getDisplayName(Locale::getUS(), result);
    if (Locale::getDefault() == Locale::getUS() && result != "English (United States)")
        errln("BreakIterator::getDisplayName() failed: expected \"English (United States)\", got \""
                + result);

    BreakIterator::getDisplayName(Locale::getFrance(), Locale::getUS(), result);
    if (result != "French (France)")
        errln("BreakIterator::getDisplayName() failed: expected \"French (France)\", got \""
                + result);
}
/**
 * Test End Behaviour
 * @bug 4068137
 */
void RBBITest::TestEndBehaviour()
{
    UErrorCode status = U_ZERO_ERROR;
    UnicodeString testString("boo.");
    BreakIterator *wb = BreakIterator::createWordInstance(Locale::getDefault(), status);
    if (U_FAILURE(status))
    {
        errln("Failed to create the BreakIterator for default locale in TestEndBehaviour.\n");
        return;
    }
    wb->setText(testString);

    if (wb->first() != 0)
        errln("Didn't get break at beginning of string.");
    if (wb->next() != 3)
        errln("Didn't get break before period in \"boo.\"");
    if (wb->current() != 4 && wb->next() != 4)
        errln("Didn't get break at end of string.");
    delete wb;
}
/*
 * @bug 4153072
 */
void RBBITest::TestBug4153072() {
    UErrorCode status = U_ZERO_ERROR;
    BreakIterator *iter = BreakIterator::createWordInstance(Locale::getDefault(), status);
    if (U_FAILURE(status))
    {
        errln("Failed to create the BreakIterator for default locale in TestBug4153072\n");
        return;
    }
    UnicodeString str("...Hello, World!...");
    int32_t begin = 3;
    int32_t end = str.length() - 3;
    UBool dummy;

    StringCharacterIterator* textIterator = new StringCharacterIterator(str, begin, end, begin);
    iter->adoptText(textIterator);
    int index;
    for (index = -1; index < begin + 1; ++index) {
        dummy = iter->isBoundary(index);
        if (index < begin && dummy == TRUE) {
            errln((UnicodeString)"Didn't handle preceeding correctly with offset = " + index +
                            " and begin index = " + begin);
        }
    }
    delete iter;
}


/**
 * Test Japanese Line Break
 * @bug 4095322
 */
void RBBITest::TestJapaneseLineBreak()
{
#if 0
    // Test needs updating some more...   Dump it for now.


    // Change for Unicode TR 14:  Punctuation characters with categories Pi and Pf do not count
    //        as opening and closing punctuation for line breaking.
    //        Also, \u30fc and \u30fe are not counted as hyphens.   Remove these chars
    //        from these tests.    6-13-2002
    //
    UErrorCode status = U_ZERO_ERROR;
    UnicodeString testString = CharsToUnicodeString("\\u4e00x\\u4e8c");
    UnicodeString precedingChars = CharsToUnicodeString(
        //"([{\\u00ab$\\u00a5\\u00a3\\u00a4\\u2018\\u201a\\u201c\\u201e\\u201b\\u201f");
        "([{$\\u00a5\\u00a3\\u00a4\\u201a\\u201e");
    UnicodeString followingChars = CharsToUnicodeString(
        // ")]}\\u00bb!%,.\\u3001\\u3002\\u3063\\u3083\\u3085\\u3087\\u30c3\\u30e3\\u30e5\\u30e7\\u30fc"
        ")]}!%,.\\u3001\\u3002\\u3063\\u3083\\u3085\\u3087\\u30c3\\u30e3\\u30e5\\u30e7"
        // ":;\\u309b\\u309c\\u3005\\u309d\\u309e\\u30fd\\u30fe\\u2019\\u201d\\u00b0\\u2032\\u2033\\u2034"
        ":;\\u309b\\u309c\\u3005\\u309d\\u309e\\u30fd\\u00b0\\u2032\\u2033\\u2034"
        "\\u2030\\u2031\\u2103\\u2109\\u00a2\\u0300\\u0301\\u0302");
    BreakIterator *iter = BreakIterator::createLineInstance(Locale::getJapan(), status);

    int32_t i;
    if (U_FAILURE(status))
    {
        errln("Failed to create the BreakIterator for Japanese locale in TestJapaneseLineBreak.\n");
        return;
    }

    for (i = 0; i < precedingChars.length(); i++) {
        testString.setCharAt(1, precedingChars[i]);
        iter->setText(testString);
        int32_t j = iter->first();
        if (j != 0)
            errln("ja line break failure: failed to start at 0");
        j = iter->next();
        if (j != 1)
            errln("ja line break failure: failed to stop before '" + UCharToUnicodeString(precedingChars[i])
                        + "' (" + ((int)(precedingChars[i])) + ")");
        j = iter->next();
        if (j != 3)
            errln("ja line break failure: failed to skip position after '" + UCharToUnicodeString(precedingChars[i])
                        + "' (" + ((int)(precedingChars[i])) + ")");
    }

    for (i = 0; i < followingChars.length(); i++) {
        testString.setCharAt(1, followingChars[i]);
        iter->setText(testString);
        int j = iter->first();
        if (j != 0)
            errln("ja line break failure: failed to start at 0");
        j = iter->next();
        if (j != 2)
            errln("ja line break failure: failed to skip position before '" + UCharToUnicodeString(followingChars[i])
                        + "' (" + ((int)(followingChars[i])) + ")");
        j = iter->next();
        if (j != 3)
            errln("ja line break failure: failed to stop after '" + UCharToUnicodeString(followingChars[i])
                        + "' (" + ((int)(followingChars[i])) + ")");
    }
    delete iter;
#endif
}


//------------------------------------------------------------------------------
//
//   RBBITest::Extended    Run  RBBI Tests from an external test data file
//
//------------------------------------------------------------------------------

struct TestParams {
    BreakIterator   *bi;
    UnicodeString    dataToBreak;
    UVector32       *expectedBreaks;
    UVector32       *srcLine;
    UVector32       *srcCol;
};

void RBBITest::executeTest(TestParams *t) {
    int32_t    bp;
    int32_t    prevBP;
    int32_t    i;

    t->bi->setText(t->dataToBreak);
    //
    //  Run the iterator forward
    //
    prevBP = -1;
    for (bp = t->bi->first(); bp != BreakIterator::DONE; bp = t->bi->next()) {
        if (prevBP ==  bp) {
            // Fail for lack of forward progress.
            errln("Forward Iteration, no forward progress.  Break Pos=%4d  File line,col=%4d,%4d",
                bp, t->srcLine->elementAti(bp), t->srcCol->elementAti(bp));
            break;
        }

        // Check that there were we didn't miss an expected break between the last one
        //  and this one.
        for (i=prevBP+1; i<bp; i++) {
            if (t->expectedBreaks->elementAti(i) != 0) {
                int expected[] = {0, i};
                printStringBreaks(t->dataToBreak, expected, 2);
                errln("Forward Iteration, break expected, but not found.  Pos=%4d  File line,col= %4d,%4d",
                      i, t->srcLine->elementAti(i), t->srcCol->elementAti(i));
            }
        }

        // Check that the break we did find was expected
        if (t->expectedBreaks->elementAti(bp) == 0) {
            int expected[] = {0, bp};
            printStringBreaks(t->dataToBreak, expected, 2);
            errln("Forward Iteration, break found, but not expected.  Pos=%4d  File line,col= %4d,%4d",
                bp, t->srcLine->elementAti(bp), t->srcCol->elementAti(bp));
        } else {
            // The break was expected.
            //   Check that the {nnn} tag value is correct.
            int32_t expectedTagVal = t->expectedBreaks->elementAti(bp);
            if (expectedTagVal == -1) {
                expectedTagVal = 0;
            }
            int32_t rs = ((RuleBasedBreakIterator *)t->bi)->getRuleStatus();
            if (rs != expectedTagVal) {
                errln("Incorrect status for forward break.  Pos=%4d  File line,col= %4d,%4d.\n"
                      "          Actual, Expected status = %4d, %4d",
                    bp, t->srcLine->elementAti(bp), t->srcCol->elementAti(bp), rs, expectedTagVal);
            }
        }


        prevBP = bp;
    }

    // Verify that there were no missed expected breaks after the last one found
    for (i=prevBP+1; i<t->expectedBreaks->size(); i++) {
        if (t->expectedBreaks->elementAti(i) != 0) {
            errln("Forward Iteration, break expected, but not found.  Pos=%4d  File line,col= %4d,%4d",
                      i, t->srcLine->elementAti(i), t->srcCol->elementAti(i));
        }
    }

    //
    //  Run the iterator backwards, verify that the same breaks are found.
    //
    prevBP = t->dataToBreak.length()+2;  // start with a phony value for the last break pos seen.
    for (bp = t->bi->last(); bp != BreakIterator::DONE; bp = t->bi->previous()) {
        if (prevBP ==  bp) {
            // Fail for lack of progress.
            errln("Reverse Iteration, no progress.  Break Pos=%4d  File line,col=%4d,%4d",
                bp, t->srcLine->elementAti(bp), t->srcCol->elementAti(bp));
            break;
        }

        // Check that there were we didn't miss an expected break between the last one
        //  and this one.  (UVector returns zeros for index out of bounds.)
        for (i=prevBP-1; i>bp; i--) {
            if (t->expectedBreaks->elementAti(i) != 0) {
                errln("Reverse Itertion, break expected, but not found.  Pos=%4d  File line,col= %4d,%4d",
                      i, t->srcLine->elementAti(i), t->srcCol->elementAti(i));
            }
        }

        // Check that the break we did find was expected
        if (t->expectedBreaks->elementAti(bp) == 0) {
            errln("Reverse Itertion, break found, but not expected.  Pos=%4d  File line,col= %4d,%4d",
                   bp, t->srcLine->elementAti(bp), t->srcCol->elementAti(bp));
        } else {
            // The break was expected.
            //   Check that the {nnn} tag value is correct.
            int32_t expectedTagVal = t->expectedBreaks->elementAti(bp);
            if (expectedTagVal == -1) {
                expectedTagVal = 0;
            }
            int32_t rs = ((RuleBasedBreakIterator *)t->bi)->getRuleStatus();
            if (rs != expectedTagVal) {
                errln("Incorrect status for reverse break.  Pos=%4d  File line,col= %4d,%4d.\n"
                      "          Actual, Expected status = %4d, %4d",
                    bp, t->srcLine->elementAti(bp), t->srcCol->elementAti(bp), rs, expectedTagVal);
            }
        }

        prevBP = bp;
    }

    // Verify that there were no missed breaks prior to the last one found
    for (i=prevBP-1; i>=0; i--) {
        if (t->expectedBreaks->elementAti(i) != 0) {
            errln("Forward Itertion, break expected, but not found.  Pos=%4d  File line,col= %4d,%4d",
                      i, t->srcLine->elementAti(i), t->srcCol->elementAti(i));
        }
    }
}


void RBBITest::TestExtended() {
    UErrorCode      status  = U_ZERO_ERROR;
    Locale          locale   = Locale::getDefault();

    UnicodeString       rules;
    TestParams          tp;
    tp.bi             = NULL;
    tp.expectedBreaks = new UVector32(status);
    tp.srcLine        = new UVector32(status);
    tp.srcCol         = new UVector32(status);


    //
    //  Open and read the test data file.
    //
    const char *testDataDirectory = loadTestData(status);
    char testFileName[1000];
    if (strlen(testDataDirectory) >= sizeof(testFileName)) {
        errln("Can't open test data.  Path too long.");
        return;
    }
    strcpy(testFileName, testDataDirectory);
    char *p = strstr(testFileName, "/out/testdata");
    if (p == NULL) {
        p = strstr(testFileName, "\\out\\testdata");
        if (p == NULL) {
            errln("Can't open test data.  Bad test data directory path..");
            return;
        }
    }
    strcpy(p+1, "rbbitst.txt");

    int    len;
    UChar *testFile = ReadAndConvertFile(testFileName, len, status);
    if (U_FAILURE(status)) {
        return; /* something went wrong, error already output */
    }



    //
    //  Put the test data into a UnicodeString
    //
    UnicodeString testString(FALSE, testFile, len);

    enum EParseState{
        PARSE_COMMENT,
        PARSE_TAG,
        PARSE_DATA,
        PARSE_NUM
    }
    parseState = PARSE_TAG;

    EParseState savedState = PARSE_TAG;

    static const UChar CH_LF        = 0x0a;
    static const UChar CH_CR        = 0x0d;
    static const UChar CH_HASH      = 0x23;
    /*static const UChar CH_PERIOD    = 0x2e;*/
    static const UChar CH_LT        = 0x3c;
    static const UChar CH_GT        = 0x3e;
    static const UChar CH_BACKSLASH = 0x5c;
    static const UChar CH_BULLET    = 0x2022;

    int32_t    lineNum  = 1;
    int32_t    colStart = 0;
    int32_t    column   = 0;
    int32_t    charIdx  = 0;

    int32_t    tagValue = 0;       // The numeric value of a <nnn> tag.

    for (charIdx = 0; charIdx < len; ) {
        UChar  c = testString.charAt(charIdx);
        charIdx++;
        if (c == CH_CR && charIdx<len && testString.charAt(charIdx) == CH_LF) {
            // treat CRLF as a unit
            c = CH_LF;
            charIdx++;
        }
        if (c == CH_LF || c == CH_CR) {
            lineNum++;
            colStart = charIdx;
        }
        column = charIdx - colStart + 1;

        switch (parseState) {
        case PARSE_COMMENT:
            if (c == 0x0a || c == 0x0d) {
                parseState = savedState;
            }
            break;

        case PARSE_TAG:
            {
            if (c == CH_HASH) {
                parseState = PARSE_COMMENT;
                savedState = PARSE_TAG;
                break;
            }
            if (u_isUWhiteSpace(c)) {
                break;
            }
            if (testString.compare(charIdx-1, 6, "<word>") == 0) {
                delete tp.bi;
                tp.bi = BreakIterator::createWordInstance(locale,  status);
                charIdx += 5;
                break;
            }
            if (testString.compare(charIdx-1, 6, "<char>") == 0) {
                delete tp.bi;
                tp.bi = BreakIterator::createCharacterInstance(locale,  status);
                charIdx += 5;
                break;
            }
            if (testString.compare(charIdx-1, 6, "<line>") == 0) {
                delete tp.bi;
                tp.bi = BreakIterator::createLineInstance(locale,  status);
                charIdx += 5;
                break;
            }
            if (testString.compare(charIdx-1, 6, "<sent>") == 0) {
                delete tp.bi;
                tp.bi = BreakIterator::createSentenceInstance(locale,  status);
                charIdx += 5;
                break;
            }
            if (testString.compare(charIdx-1, 7, "<title>") == 0) {
                delete tp.bi;
                tp.bi = BreakIterator::createTitleInstance(locale,  status);
                charIdx += 6;
                break;
            }
            if (testString.compare(charIdx-1, 6, "<data>") == 0) {
                parseState = PARSE_DATA;
                charIdx += 5;
                tp.dataToBreak = "";
                tp.expectedBreaks->removeAllElements();
                tp.srcCol ->removeAllElements();
                tp.srcLine->removeAllElements();
                break;
            }

            errln("line %d: Tag expected in test file.", lineNum);
            goto end_test;
            parseState = PARSE_COMMENT;
            savedState = PARSE_DATA;
            }
            break;

        case PARSE_DATA:
            if (c == CH_BULLET) {
                int32_t  breakIdx = tp.dataToBreak.length();
                tp.expectedBreaks->setSize(breakIdx+1);
                tp.expectedBreaks->setElementAt(-1, breakIdx);
                tp.srcLine->setSize(breakIdx+1);
                tp.srcLine->setElementAt(lineNum, breakIdx);
                tp.srcCol ->setSize(breakIdx+1);
                tp.srcCol ->setElementAt(column, breakIdx);
                break;
            }

            if (testString.compare(charIdx-1, 7, "</data>") == 0) {
                // Add final entry to mappings from break location to source file position.
                //  Need one extra because last break position returned is after the
                //    last char in the data, not at the last char.
                tp.srcLine->addElement(lineNum, status);
                tp.srcCol ->addElement(column, status);

                parseState = PARSE_TAG;
                charIdx += 7;

                // RUN THE TEST!
                executeTest(&tp);
                break;
            }

            if (testString.compare(charIdx-1, 3, "\\N{") == 0) {
                // Named character, e.g. \N{COMBINING GRAVE ACCENT}
                // Get the code point from the name and insert it into the test data.
                //   (Damn, no API takes names in Unicode  !!!
                //    we've got to take it back to char *)
                int32_t nameEndIdx = testString.indexOf((UChar)0x7d/*'}'*/, charIdx);
                int32_t nameLength = nameEndIdx - (charIdx+2);
                char charNameBuf[200];
                UChar32 theChar = -1;
                if (nameEndIdx != -1) {
                    UErrorCode status = U_ZERO_ERROR;
                    testString.extract(charIdx+2, nameLength, charNameBuf, sizeof(charNameBuf));
                    charNameBuf[sizeof(charNameBuf)-1] = 0;
                    theChar = u_charFromName(U_UNICODE_CHAR_NAME, charNameBuf, &status);
                    if (U_FAILURE(status)) {
                        theChar = -1;
                    }
                }
                if (theChar == -1) {
                    errln("Error in named character in test file at line %d, col %d",
                        lineNum, column);
                } else {
                    // Named code point was recognized.  Insert it
                    //   into the test data.
                    tp.dataToBreak.append(theChar);
                    while (tp.dataToBreak.length() > tp.srcLine->size()) {
                        tp.srcLine->addElement(lineNum, status);
                        tp.srcCol ->addElement(column, status);
                    }
                }
                if (nameEndIdx > charIdx) {
                    charIdx = nameEndIdx+1;
                }
                break;
            }




            if (testString.compare(charIdx-1, 2, "<>") == 0) {
                charIdx++;
                int32_t  breakIdx = tp.dataToBreak.length();
                tp.expectedBreaks->setSize(breakIdx+1);
                tp.expectedBreaks->setElementAt(-1, breakIdx);
                tp.srcLine->setSize(breakIdx+1);
                tp.srcLine->setElementAt(lineNum, breakIdx);
                tp.srcCol ->setSize(breakIdx+1);
                tp.srcCol ->setElementAt(column, breakIdx);
                break;
            }

            if (c == CH_LT) {
                tagValue   = 0;
                parseState = PARSE_NUM;
                break;
            }

            if (c == CH_HASH && column==3) {   // TODO:  why is column off so far?
                parseState = PARSE_COMMENT;
                savedState = PARSE_DATA;
                break;
            }

            if (c == CH_BACKSLASH) {
                // Check for \ at end of line, a line continuation.
                //     Advance over (discard) the newline
                UChar32 cp = testString.char32At(charIdx);
                if (cp == CH_CR && charIdx<len && testString.charAt(charIdx+1) == CH_LF) {
                    // We have a CR LF
                    //  Need an extra increment of the input ptr to move over both of them
                    charIdx++;
                }
                if (cp == CH_LF || cp == CH_CR) {
                    lineNum++;
                    colStart = charIdx;
                    charIdx++;
                    break;
                }

                // Let unescape handle the back slash.
                cp = testString.unescapeAt(charIdx);
                if (cp != -1) {
                    // Escape sequence was recognized.  Insert the char
                    //   into the test data.
                    tp.dataToBreak.append(cp);
                    while (tp.dataToBreak.length() > tp.srcLine->size()) {
                        tp.srcLine->addElement(lineNum, status);
                        tp.srcCol ->addElement(column, status);
                    }
                    break;
                }


                // Not a recognized backslash escape sequence.
                // Take the next char as a literal.
                //  TODO:  Should this be an error?
                c = testString.charAt(charIdx);
                charIdx = testString.moveIndex32(charIdx, 1);
            }

            // Normal, non-escaped data char.
            tp.dataToBreak.append(c);

            // Save the mapping from offset in the data to line/column numbers in
            //   the original input file.  Will be used for better error messages only.
            //   If there's an expected break before this char, the slot in the mapping
            //     vector will already be set for this char; don't overwrite it.
            if (tp.dataToBreak.length() > tp.srcLine->size()) {
                tp.srcLine->addElement(lineNum, status);
                tp.srcCol ->addElement(column, status);
            }
            break;


        case PARSE_NUM:
            // We are parsing an expected numeric tag value, like <1234>,
            //   within a chunk of data.
            if (u_isUWhiteSpace(c)) {
                break;
            }

            if (c == CH_GT) {
                // Finished the number.  Add the info to the expected break data,
                //   and switch parse state back to doing plain data.
                parseState = PARSE_DATA;
                if (tagValue == 0) {
                    tagValue = -1;
                }
                int32_t  breakIdx = tp.dataToBreak.length();
                tp.expectedBreaks->setSize(breakIdx+1);
                tp.expectedBreaks->setElementAt(tagValue, breakIdx);
                tp.srcLine->setSize(breakIdx+1);
                tp.srcLine->setElementAt(lineNum, breakIdx);
                tp.srcCol ->setSize(breakIdx+1);
                tp.srcCol ->setElementAt(column, breakIdx);
                break;
            }

            if (u_isdigit(c)) {
                tagValue = tagValue*10 + u_charDigitValue(c);
                break;
            }

            errln("Syntax Error in test file at line %d, col %d",
                lineNum, column);
            goto end_test;
            parseState = PARSE_COMMENT;
            break;
        }


        if (U_FAILURE(status)) {
            errln("ICU Error %s while parsing test file at line %d.",
                u_errorName(status), lineNum);
            goto end_test;
            status = U_ZERO_ERROR;
        }

    }

end_test:
    delete tp.bi;
    delete tp.expectedBreaks;
    delete tp.srcLine;
    delete tp.srcCol;
    delete [] testFile;
}


//-------------------------------------------------------------------------------
//
//    ReadAndConvertFile   Read a text data file, convert it to UChars, and
//    return the datain one big UChar * buffer, which the caller must delete.
//
//    TODO:  This is a clone of RegexTest::ReadAndConvertFile.
//           Move this function to some common place.
//
//--------------------------------------------------------------------------------
UChar *RBBITest::ReadAndConvertFile(const char *fileName, int &ulen, UErrorCode &status) {
    UChar       *retPtr  = NULL;
    char        *fileBuf = NULL;
    UConverter* conv     = NULL;
    FILE        *f       = NULL;

    ulen = 0;
    if (U_FAILURE(status)) {
        return retPtr;
    }

    //
    //  Open the file.
    //
    f = fopen(fileName, "rb");
    if (f == 0) {
        errln("Error opening test data file %s\n", fileName);
        status = U_FILE_ACCESS_ERROR;
        return NULL;
    }
    //
    //  Read it in
    //
    int   fileSize;
    int   amt_read;

    fseek( f, 0, SEEK_END);
    fileSize = ftell(f);
    fileBuf = new char[fileSize];
    fseek(f, 0, SEEK_SET);
    amt_read = fread(fileBuf, 1, fileSize, f);
    if (amt_read != fileSize || fileSize <= 0) {
        errln("Error reading test data file.");
        goto cleanUpAndReturn;
    }

    //
    // Look for a Unicode Signature (BOM) on the data just read
    //
    int32_t        signatureLength;
    const char *   fileBufC;
    const char*    encoding;

    fileBufC = fileBuf;
    encoding = ucnv_detectUnicodeSignature(
        fileBuf, fileSize, &signatureLength, &status);
    if(encoding!=NULL ){
        fileBufC  += signatureLength;
        fileSize  -= signatureLength;
    }

    //
    // Open a converter to take the rule file to UTF-16
    //
    conv = ucnv_open(encoding, &status);
    if (U_FAILURE(status)) {
        goto cleanUpAndReturn;
    }

    //
    // Convert the rules to UChar.
    //  Preflight first to determine required buffer size.
    //
    ulen = ucnv_toUChars(conv,
        NULL,           //  dest,
        0,              //  destCapacity,
        fileBufC,
        fileSize,
        &status);
    if (status == U_BUFFER_OVERFLOW_ERROR) {
        // Buffer Overflow is expected from the preflight operation.
        status = U_ZERO_ERROR;

        retPtr = new UChar[ulen+1];
        ucnv_toUChars(conv,
            retPtr,       //  dest,
            ulen+1,
            fileBufC,
            fileSize,
            &status);
    }

cleanUpAndReturn:
    fclose(f);
    delete fileBuf;
    ucnv_close(conv);
    if (U_FAILURE(status)) {
        errln("ucnv_toUChars: ICU Error \"%s\"\n", u_errorName(status));
        delete retPtr;
        retPtr = 0;
        ulen   = 0;
    };
    return retPtr;
}


//--------------------------------------------------------------------------------------------
//
//     Exhaustive Tests, using Unicode Data Files.
//
//--------------------------------------------------------------------------------------------

//
//  Token level scanner for the Unicode Line Break Test Data file.
//      Return the next token, as follows:
//          >= 0:       a UChar32 character, scanned from hex in the file.
//          -1:         a break position, a division sign in the file.
//          -2:         end of rule.  A new line in the file.
//          -3:         end of file.  No more rules.
//          -4:         Error
//
//   The scanner
//       strips comments, ('#' to end of line)
//       Recognizes CR, CR/LF and LF as new lines.
//       Skips over spaces and  Xs (don't break here) in the data.
//
struct ScanState {
    int32_t     fPeekChar;
    UBool       fPeeked;
    int32_t     fLineNum;
    FILE        *fFile;
    ScanState() :fPeeked(FALSE), fLineNum(0), fFile(NULL) {};
};

//  Literal characters that are of interest.  In hex to keep EBCDIC based machines happy.
//  The data itself is latin-1 on all platforms.
static const int32_t chSpace  = 0x20;
static const int32_t chTab    = 0x09;
static const int32_t chCR     = 0x0D;
static const int32_t chLF     = 0x0A;
static const int32_t chHash   = 0x23;
static const int32_t chMult   = 0xD7;
static const int32_t chDivide = 0xF7;

static int32_t   nextLBDToken(ScanState *s) {
    int32_t     c;

    // Read  characters from the input file until we get something interesting
    //   to return.  The file is in latin-1 encoding.
    for (;;) {
        // Get the next character to look at,
        if (s->fPeeked) {
            c = s->fPeekChar;
            s->fPeeked = FALSE;
        } else {
            c = getc(s->fFile);
        }

        // EOF.  Return immediately.
        if (c == EOF) {
            return -3;
        }

        // Spaces.  Treat the multiply sign as a space - it indicates a no-break position
        //          in the data, and the test program doesn't want to see them.
        //          Continue the next char loop, looking for something significant.
        if (c == chSpace || c == chTab || c == chMult) {
            continue;
        }

        //  Divide sign.  Indicates an expected break position.
        if (c == chDivide) {
            return -1;
        }

        // New Line Handling.  Keep track of line number in the file, which in turn
        //   requires keeping track of CR/LF as a single new line.
        if (c == chCR) {
            s->fLineNum++;
            s->fPeekChar = getc(s->fFile);
            if (s->fPeekChar != chLF) {s->fPeeked = TRUE;};
            return -2;
        }
        if (c == chLF) {
            s->fLineNum++;
            return -2;
        }

        // Comments.  Consume everything up to the next new line.
        if (c == chHash) {
            do {
                c = getc(s->fFile);
            } while (!(c == EOF || c == chCR || c == chLF));
            s->fPeekChar = c;
            s->fPeeked = TRUE;
            return nextLBDToken(s);
        }

        // Scan a hex character (UChar32) value.
        if (u_digit(c, 16) >= 0) {
            int32_t   v = u_digit(c, 16);
            for (;;) {
                c = getc(s->fFile);
                if (u_digit(c, 16) < 0) {break;};
                v <<= 4;
                v += u_digit(c, 16);
            }
            s->fPeekChar = c;
            s->fPeeked   = TRUE;
            return v;
        }

        // Error.  Character was something unexpected.
        return -4;
    }
}



void RBBITest::TestLineBreakData() {

    UErrorCode      status = U_ZERO_ERROR;
    UnicodeString   testString;
    UVector         expectedBreaks(status);
    ScanState       ss;
    int32_t         tok;

    BreakIterator *bi = BreakIterator::createLineInstance(Locale::getDefault(), status);
    if (U_FAILURE(status)) {
        errln("Failure creating break iterator");
        return;
    }

    const char *    lbdfName = "LBTest.txt";

    // Open the test data file.
    //   TODO:  a proper way to handle this data.
    ss.fFile = fopen(lbdfName, "rb");
    if (ss.fFile == NULL) {
        logln("Unable to open Line Break Test Data file.  Skipping test.");
        delete bi;
        return;
    }

    // Loop once per line from the test data file.
    for (;;) {
        // Zero out test data from previous line.
        testString.truncate(0);
        expectedBreaks.removeAllElements();

        // Read one test's (line's) worth of data from the file.
        //   Loop once per token on the input file line.
        for(;;)  {
            tok = nextLBDToken(&ss);

            // If we scanned a character number in the file.
            //   save it in the test data array.
            if (tok >= 0) {
                testString.append((UChar32)tok);
                continue;
            }

            // If we scanned a break position in the data, record it.
            if (tok == -1) {
                expectedBreaks.addElement(testString.length(), status);
                continue;
            }

            // If we scanned a new line, or EOF
            //    drop out of scan loop and run the test case.
            if (tok == -2 || tok == -3) {break;};

            // None of above.  Error.
            errln("Failure:  Unrecognized data format,  test file line %d", ss.fLineNum);
            break;
        }

        // If this line from the test data file actually contained test data,
        //   run the test.
        if (testString.length() > 0) {
            int32_t pos;                 // Break Position in the test string
            int32_t expectedI = 0;       // Index of expected break position in vector of same.
            int32_t expectedPos;         // Expected break position (index into test string)

            bi->setText(testString);
            pos = bi->first();       // TODO:  break iterators always return a match at pos 0.
            pos = bi->next();        //        Line Break TR says no match at position 0.
                                     //        Resolve.

            for (; pos != BreakIterator::DONE; ) {
                expectedPos = expectedBreaks.elementAti(expectedI);
                if (pos < expectedPos) {
                    errln("Failure: Test file line %d, unexpected break found at position %d",
                        ss.fLineNum, pos);
                    break;
                }
                if (pos > expectedPos) {
                    errln("Failure: Test file line %d, failed to find break at position %d",
                        ss.fLineNum, expectedPos);
                    break;
                }
                pos = bi->next();
                expectedI++;
            }
        }

        // If we've hit EOF on the input file, we're done.
        if (tok == -3) {
            break;
        }

    }

    fclose(ss.fFile);
    delete bi;

}

#if !UCONFIG_NO_REGULAR_EXPRESSIONS

//---------------------------------------------------------------------------------------
//
//   classs RBBIMonkeyKind
//
//      Monkey Test for Break Iteration
//      Abstract interface class.   Concrete derived classes independently
//      implement the break rules for different iterator types.
//
//      The Monkey Test itself uses doesn't know which type of break iterator it is
//      testing, but works purely in terms of the interface defined here.
//
//---------------------------------------------------------------------------------------
class RBBIMonkeyKind {
public:
    // Return a UVector of UnicodeSets, representing the character classes used
    //   for this type of iterator.
    virtual  UVector  *charClasses() = 0;

    // Set the test text on which subsequent calls to next() will operate
    virtual  void      setText(const UnicodeString &s) = 0;

    // Find the next break postion, starting from the prev break position, or from zero.
    // Return -1 after reaching end of string.
    virtual  int32_t   next(int32_t i) = 0;

    virtual ~RBBIMonkeyKind();
    UErrorCode       deferredStatus;


protected:
    RBBIMonkeyKind();

private:
};

RBBIMonkeyKind::RBBIMonkeyKind() {
    deferredStatus = U_ZERO_ERROR;
}

RBBIMonkeyKind::~RBBIMonkeyKind() {
}


//----------------------------------------------------------------------------------------
//
//   Random Numbers.  Similar to standard lib rand() and srand()
//                    Not using library to
//                      1.  Get same results on all platforms.
//                      2.  Get access to current seed, to more easily reproduce failures.
//
//---------------------------------------------------------------------------------------
static uint32_t m_seed = 1;

static uint32_t m_rand()
{
    m_seed = m_seed * 1103515245 + 12345;
    return (uint32_t)(m_seed/65536) % 32768;
}


//------------------------------------------------------------------------------------------
//
//   class RBBICharMonkey      Character (Grapheme Cluster) specific implementation
//                             of RBBIMonkeyKind.
//
//------------------------------------------------------------------------------------------
class RBBICharMonkey: public RBBIMonkeyKind {
public:
    RBBICharMonkey();
    virtual          ~RBBICharMonkey();
    virtual  UVector *charClasses();
    virtual  void     setText(const UnicodeString &s);
    virtual  int32_t  next(int32_t i);
private:
    UVector   *fSets;

    UnicodeSet  *fCRLFSet;
    UnicodeSet  *fControlSet;
    UnicodeSet  *fExtendSet;
    UnicodeSet  *fHangulSet;
    UnicodeSet  *fAnySet;

    RegexMatcher  *fMatcher;
    const UnicodeString *fText;
};


RBBICharMonkey::RBBICharMonkey() {
    UErrorCode  status = U_ZERO_ERROR;

    fText = NULL;
    fMatcher = new RegexMatcher("\\X", 0, status);     // Pattern to match a grampheme cluster

    fCRLFSet    = new UnicodeSet("[\\r\\n]", status);
    fControlSet = new UnicodeSet("[[\\p{Zl}\\p{Zp}\\p{Cc}\\p{Cf}]-[\\n]-[\\r]-\\p{Grapheme_Extend}]", status);
    fExtendSet  = new UnicodeSet("[\\p{Grapheme_Extend}]", status);
    fHangulSet  = new UnicodeSet(
        "[\\p{Hangul_Syllable_Type=L}\\p{Hangul_Syllable_Type=L}\\p{Hangul_Syllable_Type=T}"
         "\\p{Hangul_Syllable_Type=LV}\\p{Hangul_Syllable_Type=LVT}]", status);
    fAnySet     = new UnicodeSet("[\\u0000-\\U0010ffff]", status);

    fSets       = new UVector(status);
    fSets->addElement(fCRLFSet,    status);
    fSets->addElement(fControlSet, status);
    fSets->addElement(fExtendSet,  status);
    fSets->addElement(fHangulSet,  status);
    fSets->addElement(fAnySet,     status);
    if (U_FAILURE(status)) {
        deferredStatus = status;
    }
};


void RBBICharMonkey::setText(const UnicodeString &s) {
    fText = &s;
    fMatcher->reset(s);
}


int32_t RBBICharMonkey::next(int32_t i) {
    UErrorCode status = U_ZERO_ERROR;
    int32_t  retVal = -1;

    if (fMatcher->find(i, status)) {
        retVal = fMatcher->end(status);
    }
    if (U_FAILURE(status)){
        retVal = -1;
    }
    return retVal;
}


UVector  *RBBICharMonkey::charClasses() {
    return fSets;
}


RBBICharMonkey::~RBBICharMonkey() {
    delete fSets;
    delete fCRLFSet;
    delete fControlSet;
    delete fExtendSet;
    delete fHangulSet;
    delete fAnySet;

    delete fMatcher;
}

//------------------------------------------------------------------------------------------
//
//   class RBBIWordMonkey      Word Break specific implementation
//                             of RBBIMonkeyKind.
//
//------------------------------------------------------------------------------------------
class RBBIWordMonkey: public RBBIMonkeyKind {
public:
    RBBIWordMonkey();
    virtual          ~RBBIWordMonkey();
    virtual  UVector *charClasses();
    virtual  void     setText(const UnicodeString &s);
    virtual int32_t   next(int32_t i);
private:
    UVector      *fSets;

    UnicodeSet  *fKatakanaSet;
    UnicodeSet  *fALetterSet;
    UnicodeSet  *fMidLetterSet;
    UnicodeSet  *fMidNumLetSet;
    UnicodeSet  *fMidNumSet;
    UnicodeSet  *fNumericSet;
    UnicodeSet  *fFormatSet;
    UnicodeSet  *fOtherSet;
    UnicodeSet  *fExtendSet;

    RegexMatcher  *fMatcher;

    const UnicodeString  *fText;

    RegexMatcher         *fGCFMatcher;
    RegexMatcher         *fGCMatcher;

};


RBBIWordMonkey::RBBIWordMonkey() : fGCFMatcher(0),
                                   fGCMatcher(0)
{
    UErrorCode  status = U_ZERO_ERROR;

    fSets          = new UVector(status);

    fKatakanaSet   = new UnicodeSet("[\\p{script=KATAKANA}\\u30fc\\uff70\\uff9e\\uff9f]", status);

    const UnicodeString ALetterStr( "[[\\p{Alphabetic}\\u05f3]-[\\p{Ideographic}]-[\\p{Script=Thai}]"
                                    "-[\\p{Script=Lao}]-[\\p{Script=Hiragana}]-"
                                    "[\\p{script=KATAKANA}\\u30fc\\uff70\\uff9e\\uff9f]]");

    fALetterSet    = new UnicodeSet(ALetterStr, status);
    fMidLetterSet  = new UnicodeSet("[\\u0027\\u00b7\\u05f4\\u2019\\u2027]", status);
    fMidNumLetSet  = new UnicodeSet("[\\u002e\\u003a]", status);
    fMidNumSet     = new UnicodeSet("[\\p{Line_Break=Infix_Numeric}]", status);
    fNumericSet    = new UnicodeSet("[\\p{Line_Break=Numeric}]", status);
    fFormatSet     = new UnicodeSet("[\\p{Format}-\\p{Grapheme_Extend}]", status);
    fExtendSet     = new UnicodeSet("[\\p{Grapheme_Extend}]", status);
    fOtherSet      = new UnicodeSet();
    if(U_FAILURE(status)) {
      deferredStatus = status;
      return;
    }

    fOtherSet->complement();
    fOtherSet->removeAll(*fKatakanaSet);
    fOtherSet->removeAll(*fALetterSet);
    fOtherSet->removeAll(*fMidLetterSet);
    fOtherSet->removeAll(*fMidNumLetSet);
    fOtherSet->removeAll(*fMidNumSet);
    fOtherSet->removeAll(*fNumericSet);

    fSets->addElement(fALetterSet,   status);
    fSets->addElement(fMidLetterSet, status);
    fSets->addElement(fMidNumLetSet, status);
    fSets->addElement(fMidNumSet,    status);
    fSets->addElement(fNumericSet,   status);
    fSets->addElement(fFormatSet,    status);
    fSets->addElement(fOtherSet,     status);


    fGCFMatcher = new RegexMatcher("\\X(?:[\\p{Format}-\\p{Grapheme_Extend}])*", 0, status);
    fGCMatcher  = new RegexMatcher("\\X", 0, status);

    if (U_FAILURE(status)) {
        deferredStatus = status;
    }
};

void RBBIWordMonkey::setText(const UnicodeString &s) {
    fText       = &s;
    fGCMatcher->reset(*fText);
    fGCFMatcher->reset(*fText);
}


int32_t RBBIWordMonkey::next(int32_t prevPos) {
    UErrorCode status = U_ZERO_ERROR;

    int    p0, p1, p2, p3;    // Indices of the significant code points around the 
                              //   break position being tested.  The candidate break
                              //   locatoin is before p2.

    int     breakPos = -1;

    UChar32 c0, c1, c2, c3;   // The code points at p0, p1, p2 & p3.

    // Prev break at end of string.  return DONE.
    if (prevPos >= fText->length()) {
        return -1;
    }
    p0 = p1 = p2 = p3 = prevPos;
    c3 =  fText->char32At(prevPos);
    c0 = c1 = c2 = 0;


    // Format char after prev break?  Special case, see last Note for Word Boundaries TR.
    //    break immdiately after the format char.
    if (fFormatSet->contains(c3)) {
        breakPos = fText->moveIndex32(prevPos, 1);
        return breakPos;
    }


    // Loop runs once per "significant" character position in the input text.
    for (;;) {
        // Move all of the positions forward in the input string.
        p0 = p1;  c0 = c1;
        p1 = p2;  c1 = c2;
        p2 = p3;  c2 = c3;
        // Advancd p3 by    (GC Format*)   Rules 3, 4
        status = U_ZERO_ERROR;
        if  (fGCFMatcher->find(p3, status) == FALSE) {
            p3 = fText->length();
            c3 = 0;
        } else {
            p3 = fGCFMatcher->end(0, status);
            U_ASSERT(U_SUCCESS(status));
            c3 = fText->char32At(p3);
        }
        
        if (p1 == p2) {
            // Still warming up the loop.  (won't work with zero length strings, but we don't care)
            continue;
        }
        if (p2 == fText->length()) {
            // Reached end of string.  Always a break position.
            break;
        }

        // Rule (5).   ALetter x ALetter
        if (fALetterSet->contains(c1) &&
            fALetterSet->contains(c2))  {
            continue;
        }

        // Rule (6)  ALetter  x  (MidLetter | MidNumLet) ALetter
        //
        //    Also incorporates rule 7 by skipping pos ahead to position of the
        //    terminating ALetter.
        if ( fALetterSet->contains(c1) &&
            (fMidLetterSet->contains(c2) || fMidNumLetSet->contains(c2)) &&
             fALetterSet->contains(c3)) {
            continue;
        }


        // Rule (7)  ALetter (MidLetter | MidNumLet)  x  ALetter
        if (fALetterSet->contains(c0) &&
            (fMidLetterSet->contains(c1) || fMidNumLetSet->contains(c1) ) &&
            fALetterSet->contains(c2)) {
            continue;
        }

        // Rule (8)    Numeric x Numeric
        if (fNumericSet->contains(c1) &&
            fNumericSet->contains(c2))  {
            continue;
        }

        // Rule (9)    ALetter x Numeric
        if (fALetterSet->contains(c1) &&
            fNumericSet->contains(c2))  {
            continue;
        }

        // Rule (10)    Numeric x ALetter
        if (fNumericSet->contains(c1) &&
            fALetterSet->contains(c2))  {
            continue;
        }

        // Rule (11)   Numeric (MidNum | MidNumLet)  x  Numeric
        if ( fNumericSet->contains(c0) &&
            (fMidNumSet->contains(c1) || fMidNumLetSet->contains(c1)) && 
            fNumericSet->contains(c2)) {
            continue;
        }

        // Rule (12)  Numeric x (MidNum | MidNumLet) Numeric
        if (fNumericSet->contains(c1) &&
            (fMidNumSet->contains(c2) || fMidNumLetSet->contains(c2)) &&
            fNumericSet->contains(c3)) {
            continue;
        }
        
        // Rule (13)  Katakana x Katakana
        if (fKatakanaSet->contains(c1) &&
            fKatakanaSet->contains(c2))  {
            continue;
        }

        // Rule 14.  Break found here.
        break;
    }


    //  Rule 4 fixup,  back up before any trailing
    //                 format characters at the end of the word.
    breakPos = p2;
    status = U_ZERO_ERROR;
    if  (fGCMatcher->find(p1, status)) {
        breakPos = fGCMatcher->end(0, status);
        U_ASSERT(U_SUCCESS(status));
    }
    return breakPos;
}


UVector  *RBBIWordMonkey::charClasses() {
    return fSets;
}


RBBIWordMonkey::~RBBIWordMonkey() {
    delete fSets;
    delete fKatakanaSet;
    delete fALetterSet;
    delete fMidLetterSet;
    delete fMidNumLetSet;
    delete fMidNumSet;
    delete fNumericSet;
    delete fFormatSet;
    delete fExtendSet;
    delete fOtherSet;

    delete fGCFMatcher;
    delete fGCMatcher;
}




//-------------------------------------------------------------------------------------------
//
//  RBBILineMonkey
//
//-------------------------------------------------------------------------------------------

class RBBILineMonkey: public RBBIMonkeyKind {
public:
    RBBILineMonkey();
    virtual          ~RBBILineMonkey();
    virtual  UVector *charClasses();
    virtual  void     setText(const UnicodeString &s);
    virtual  int32_t  next(int32_t i);
    virtual  void     rule67Adjust(int32_t pos, UChar32 *posChar, int32_t *nextPos, UChar32 *nextChar);
private:
    UVector      *fSets;

    UnicodeSet  *fBK;
    UnicodeSet  *fCR;
    UnicodeSet  *fLF;
    UnicodeSet  *fCM;
    UnicodeSet  *fNL;
    UnicodeSet  *fSG;
    UnicodeSet  *fWJ;
    UnicodeSet  *fZW;
    UnicodeSet  *fGL;
    UnicodeSet  *fCB;
    UnicodeSet  *fSP;
    UnicodeSet  *fB2;
    UnicodeSet  *fBA;
    UnicodeSet  *fBB;
    UnicodeSet  *fHY;
    UnicodeSet  *fCL;
    UnicodeSet  *fEX;
    UnicodeSet  *fIN;
    UnicodeSet  *fNS;
    UnicodeSet  *fOP;
    UnicodeSet  *fQU;
    UnicodeSet  *fIS;
    UnicodeSet  *fNU;
    UnicodeSet  *fPO;
    UnicodeSet  *fPR;
    UnicodeSet  *fSY;
    UnicodeSet  *fAI;
    UnicodeSet  *fAL;
    UnicodeSet  *fID;
    UnicodeSet  *fSA;
    UnicodeSet  *fXX;

    BreakIterator  *fCharBI;

    const UnicodeString  *fText;
    int32_t              *fOrigPositions;

    RegexMatcher         *fNumberMatcher;
    RegexMatcher         *fLB10Matcher;
    RegexMatcher         *fLB11Matcher;
};


RBBILineMonkey::RBBILineMonkey() 
{
    UErrorCode  status = U_ZERO_ERROR;

    fSets    = new UVector(status);

    fBK    = new UnicodeSet("[\\p{Line_Break=BK}]", status);
    fCR    = new UnicodeSet("[\\p{Line_break=CR}]", status);
    fLF    = new UnicodeSet("[\\p{Line_break=LF}]", status);
    fCM    = new UnicodeSet("[\\p{Line_break=CM}]", status);
    fNL    = new UnicodeSet("[\\p{Line_break=NL}]", status);
    fWJ    = new UnicodeSet("[\\p{Line_break=WJ}]", status);
    fZW    = new UnicodeSet("[\\p{Line_break=ZW}]", status);
    fGL    = new UnicodeSet("[\\p{Line_break=GL}]", status);
    fCB    = new UnicodeSet("[\\p{Line_break=CB}]", status);
    fSP    = new UnicodeSet("[\\p{Line_break=SP}]", status);
    fB2    = new UnicodeSet("[\\p{Line_break=B2}]", status);
    fBA    = new UnicodeSet("[\\p{Line_break=BA}]", status);
    fBB    = new UnicodeSet("[\\p{Line_break=BB}]", status);
    fHY    = new UnicodeSet("[\\p{Line_break=HY}]", status);
    fCL    = new UnicodeSet("[\\p{Line_break=CL}]", status);
    fEX    = new UnicodeSet("[\\p{Line_break=EX}]", status);
    fIN    = new UnicodeSet("[\\p{Line_break=IN}]", status);
    fNS    = new UnicodeSet("[\\p{Line_break=NS}]", status);
    fOP    = new UnicodeSet("[\\p{Line_break=OP}]", status);
    fQU    = new UnicodeSet("[\\p{Line_break=QU}]", status);
    fIS    = new UnicodeSet("[\\p{Line_break=IS}]", status);
    fNU    = new UnicodeSet("[\\p{Line_break=NU}]", status);
    fPO    = new UnicodeSet("[\\p{Line_break=PO}]", status);
    fPR    = new UnicodeSet("[\\p{Line_break=PR}]", status);
    fSY    = new UnicodeSet("[\\p{Line_break=SY}]", status);
    fAI    = new UnicodeSet("[\\p{Line_break=AI}]", status);
    fAL    = new UnicodeSet("[\\p{Line_break=AL}]", status);
    fID    = new UnicodeSet("[\\p{Line_break=ID}]", status);
    fSA    = new UnicodeSet("[\\p{Line_break=SA}]", status);
    fXX    = new UnicodeSet("[\\p{Line_break=XX}]", status);

    fAL->addAll(*fXX);     // Default behavior for XX is identical to AL
    fAL->addAll(*fAI);     // Default behavior for AI is identical to AL
    fAL->addAll(*fSA);     // Default behavior for SA is XX, which defaults to AL



    fSets->addElement(fBK, status);
    fSets->addElement(fCR, status);
    fSets->addElement(fLF, status);
    fSets->addElement(fCM, status);
    fSets->addElement(fNL, status);
    fSets->addElement(fWJ, status);
    fSets->addElement(fZW, status);
    fSets->addElement(fGL, status);
    fSets->addElement(fCB, status);
    fSets->addElement(fSP, status);
    fSets->addElement(fB2, status);
    fSets->addElement(fBA, status);
    fSets->addElement(fBB, status);
    fSets->addElement(fHY, status);
    fSets->addElement(fCL, status);
    fSets->addElement(fEX, status);
    fSets->addElement(fIN, status);
    fSets->addElement(fNS, status);
    fSets->addElement(fOP, status);
    fSets->addElement(fQU, status);
    fSets->addElement(fIS, status);
    fSets->addElement(fNU, status);
    fSets->addElement(fPO, status);
    fSets->addElement(fPR, status);
    fSets->addElement(fSY, status);
    fSets->addElement(fAI, status);
    fSets->addElement(fAL, status);
    fSets->addElement(fID, status);
    fSets->addElement(fWJ, status);
    fSets->addElement(fSA, status);
    // fSets->addElement(fXX, status);



    fNumberMatcher = new RegexMatcher(
        "(\\p{Line_Break=PR}\\p{Line_Break=CM}*)?"
        "((\\p{Line_Break=OP}|\\p{Line_Break=HY})\\p{Line_Break=CM}*)?"
        "\\p{Line_Break=NU}\\p{Line_Break=CM}*"
        "((\\p{Line_Break=NU}|\\p{Line_Break=IS}|\\p{Line_Break=SY})\\p{Line_Break=CM}*)*"
        "(\\p{Line_Break=CL}\\p{Line_Break=CM}*)?"
        "(\\p{Line_Break=PO}\\p{Line_Break=CM}*)?", 
        0, status);

    fLB10Matcher = new RegexMatcher(
        "\\p{Line_Break=QU}\\p{Line_Break=CM}*"
        "\\p{Line_Break=SP}*"
        "(\\p{Line_Break=OP})\\p{Line_Break=CM}*", 
        0, status);

    fLB11Matcher = new RegexMatcher(
        "\\p{Line_Break=CL}\\p{Line_Break=CM}*"
        "\\p{Line_Break=SP}*"
        "(\\p{Line_Break=NS})\\p{Line_Break=CM}*", 
        0, status);

    fCharBI = BreakIterator::createCharacterInstance(Locale::getEnglish(), status);

    if (U_FAILURE(status)) {
        deferredStatus = status;
    }
};


void RBBILineMonkey::setText(const UnicodeString &s) {
    fText       = &s;
    fCharBI->setText(s);
    fNumberMatcher->reset(s);
}

//
//  rule67Adjust
//     Line Break TR rules 6 and 7 implementation.
//     This deals with combining marks, Hangul Syllables, and other sequences that
//     that must be treated as if they were something other than what they actually are.
//
//     This is factored out into a separate function because it must be applied twice for
//     each potential break, once to the chars before the position being checked, then
//     again to the text following the possible break.
//
void RBBILineMonkey::rule67Adjust(int32_t pos, UChar32 *posChar, int32_t *nextPos, UChar32 *nextChar) {
    if (pos == -1) {
        // Invalid initial position.  Happens during the warmup iteration of the 
        //   main loop in next().
        return;
    }

    int32_t  nPos = *nextPos;
    
    // LB 6  Treat Korean Syllables as a single unit
    int32_t  hangultype = u_getIntPropertyValue(*posChar, UCHAR_HANGUL_SYLLABLE_TYPE);
    if (hangultype != U_HST_NOT_APPLICABLE) {
        nPos = fCharBI->following(pos);   // Advance by grapheme cluster, which
                                          //  contains the logic to locate Hangul syllables.
        // Grapheme Cluster Ugliness: some Grapheme_Extend chars, which are absorbed
        //   into a grapheme cluster, are NOT Line Break CM. (Some are GL, for example.)
        //   We don't want consume any of these.  The Approach is
        //      1.  Back nPos up, undoing the consumption of any
        //          Grapheme_Extend chars by the char break iterator.
        //      2.  Let the LB 7b logic below reconsume any Line Break CM chars.
        for (;;) {
            nPos = fText->moveIndex32(nPos, -1);
            UChar32 possiblyExtendChar = fText->char32At(nPos);
            if (fID->contains(possiblyExtendChar)) {
                // We hit into the Hangul Syllable itself, class is ID.
                nPos = fText->moveIndex32(nPos, +1);
                break;
            }
        }
    }
    
    // LB 7b  Keep combining sequences together.
    //  advance over any CM class chars.  (Line Break CM class is different from
    //    grapheme cluster CM, so we need to do this even for HangulSyllables.
    //    Line Break may eat additional stuff as combining, beyond what graphem cluster did.
    if (!(fBK->contains(*posChar) || fZW->contains(*posChar) || *posChar==0x0a 
        || *posChar==0x0d || *posChar==0x85)) {
        for (;;) {
            *nextChar = fText->char32At(nPos);
            if (!fCM->contains(*nextChar)) {
                break;
            }
            nPos = fText->moveIndex32(nPos, 1);
        }
    }
    
    
    // LB 7a In a SP CM* sequence, treat the SP as an ID
    if (nPos != *nextPos && fSP->contains(*posChar)) {
        *posChar = 0x4e00;   // 0x4e00 is a CJK Ideograph, linebreak type is ID.
    }
    
    // LB 7b Treat X CM* as if it were x.
    //       No explicit action required.  
    
    // LB 7c  Treat any remaining combining mark as AL
    if (fCM->contains(*posChar)) {
        *posChar = 0x41;   // thisChar = 'A';
    }

    // Push the updated nextPos and nextChar back to our caller.
    // This only makes a difference if posChar got bigger, by slurping up a
    // combining sequence or Hangul syllable.
    *nextPos  = nPos;
    *nextChar = fText->char32At(nPos);
}



int32_t RBBILineMonkey::next(int32_t startPos) {
    UErrorCode status = U_ZERO_ERROR;
    int32_t    pos;       //  Index of the char following a potential break position
    UChar32    thisChar;  //  Character at above position "pos"

    int32_t    prevPos;   //  Index of the char preceding a potential break position
    UChar32    prevChar;  //  Character at above position.  Note that prevChar
                          //   and thisChar may not be adjacent because combining
                          //   characters between them will be ignored.

    int32_t    nextPos;   //  Index of the next character following pos.
                          //     Usually skips over combining marks.
    int32_t    nextCPPos; //  Index of the code point following "pos."
                          //     May point to a combining mark.
    int32_t    tPos;      //  temp value.
    UChar32    c;

    if (startPos >= fText->length()) {
        return -1;
    }


    // Initial values for loop.  Loop will run the first time without finding breaks,
    //                           while the invalid values shift out and the "this" and
    //                           "prev" positions are filled in with good values.
    pos      = prevPos   = -1;    // Invalid value, serves as flag for initial loop iteration.
    thisChar = prevChar  = 0;
    nextPos  = nextCPPos = startPos;


    // Loop runs once per position in the test text, until a break position
    //  is found.
    for (;;) {
        prevPos   = pos;
        prevChar  = thisChar;

        pos       = nextPos;
        thisChar  = fText->char32At(pos);

        nextCPPos = fText->moveIndex32(pos, 1);
        nextPos   = nextCPPos;

        // Break at end of text.
        if (pos >= fText->length()) {
            break;
        }

        // LB 3a  Always break after hard line breaks,
        if (fBK->contains(prevChar)) {
            break;
        }

        // LB 3b  Break after CR, LF, NL, but not inside CR LF
        if (prevChar == 0x0d && thisChar == 0x0a) {
            continue;
        }
        if (prevChar == 0x0d ||
            prevChar == 0x0a ||
            prevChar == 0x85)  {
            break;
        }

        // LB 3c  Don't break before hard line breaks
        if (thisChar == 0x0d || thisChar == 0x0a || thisChar == 0x85 ||
            fBK->contains(thisChar)) {
                continue;
        }

        // LB 10    QU SP* x OP
        if (prevPos >= 0) {
            UnicodeString  subStr10(*fText, prevPos);
            fLB10Matcher->reset(subStr10);
            status = U_ZERO_ERROR;
            if (fLB10Matcher->lookingAt(status)) {  //   /QU CM* SP* (OP) CM*/;
                // TODO:  Check status codes
                pos      = prevPos + fLB10Matcher->start(1, status);
                nextPos  = prevPos + fLB10Matcher->end(0, status);
                thisChar = fText->char32At(pos);
                continue;
            }
        }

        // LB 11   CL SP* x NS
        if (prevPos >= 0) {
            UnicodeString  subStr11(*fText, prevPos);
            fLB11Matcher->reset(subStr11);
            status = U_ZERO_ERROR;
            if (fLB11Matcher->lookingAt(status)) {  //   /QU CM* SP* (OP) CM*/;
                // TODO:  Check status codes
                pos      = prevPos + fLB11Matcher->start(1, status);
                nextPos  = prevPos + fLB11Matcher->end(0, status);
                thisChar = fText->char32At(pos);
                continue;
            }
        }

        // LB 4  Don't break before spaces or zero-width space.
        if (fSP->contains(thisChar)) {
            continue;
        }

        if (fZW->contains(thisChar)) {
            continue;
        }

        // LB 5  Break after zero width space
        if (fZW->contains(prevChar)) {
            break;
        }

        // LB 6, LB 7
        /*int32_t oldpos = pos;*/
        rule67Adjust(prevPos, &prevChar, &pos,     &thisChar);
        
        nextCPPos = fText->moveIndex32(pos, 1);
        nextPos   = nextCPPos;
        c = fText->char32At(nextPos);
        // another percularity of LB 4 - Dont break before space
        if (fSP->contains(thisChar)) {
            continue;
        }
        rule67Adjust(pos,     &thisChar, &nextPos, &c);

        // If the loop is still warming up - if we haven't shifted the initial
        //   -1 positions out of prevPos yet - loop back to advance the
        //    position in the input without any further looking for breaks.
        if (prevPos == -1) {
            continue;
        }

        // Re-apply rules 3c, 4 because these could be affected by having
        //                      a new thisChar from doing rule 6 or 7.
        if (thisChar == 0x0d || thisChar == 0x0a || thisChar == 0x85 ||   // 3c
            fBK->contains(thisChar)) {
                continue;
        }
        if (fSP->contains(thisChar)) {    // LB 4
            continue;
        }
        if (fZW->contains(thisChar)) {    // LB 4
            continue;
        }


        // LB 8  Don't break before closings.
        //       NU x CL  and NU x IS are not matched here so that they will
        //       fall into LB 17 and the more general number regular expression.
        //
        if (!fNU->contains(prevChar) && fCL->contains(thisChar) ||
                                        fEX->contains(thisChar) ||
            !fNU->contains(prevChar) && fIS->contains(thisChar) ||
            !fNU->contains(prevChar) && fSY->contains(thisChar))    {
            continue;
        }

        // LB 9  Don't break after OP SP*
        //       Scan backwards, checking for this sequence.
        //       The OP char could include combining marks, so we acually check for
        //           OP CM* SP*
        //       Another Twist: The Rule 67 fixes may have changed a CP CM
        //       sequence into a ID char, so before scanning back through spaces,
        //       verify that prevChar is indeed a space.  The prevChar variable
        //       may differ from fText[prevPos]
        tPos = prevPos;
        if (fSP->contains(prevChar)) {
            while (tPos > 0 && fSP->contains(fText->char32At(tPos))) {
                tPos=fText->moveIndex32(tPos, -1);
            }
        }
        while (tPos > 0 && fCM->contains(fText->char32At(tPos))) {
            tPos=fText->moveIndex32(tPos, -1);
        }
        if (fOP->contains(fText->char32At(tPos))) {
            continue;
        }


        // LB 11a        B2 x B2
        if (fB2->contains(thisChar) && fB2->contains(prevChar)) {
            continue;
        }

        // LB 11b   
        //    x  GL
        //    GL  x
        if (fGL->contains(thisChar) || fGL->contains(prevChar)) {
            continue;
        }
        if (fWJ->contains(thisChar) || fWJ->contains(prevChar)) {
            continue;
        }

        // LB 12    break after space
        if (fSP->contains(prevChar)) {
            break;
        }

        // LB 14
        //    x   QU
        //    QU  x
        if (fQU->contains(thisChar) || fQU->contains(prevChar)) {
            continue;
        }

        // LB 14a  Break around a CB
        if (fCB->contains(thisChar) || fCB->contains(prevChar)) {
            break;
        }

        // LB 15 
        if (fBA->contains(thisChar) ||
            fHY->contains(thisChar) ||
            fNS->contains(thisChar) ||
            fBB->contains(prevChar) )   {
            continue;
        }

        // LB 16
        if (fAL->contains(prevChar) && fIN->contains(thisChar) ||
            fID->contains(prevChar) && fIN->contains(thisChar) ||
            fIN->contains(prevChar) && fIN->contains(thisChar) ||
            fNU->contains(prevChar) && fIN->contains(thisChar) )   {
            continue; 
        }


        // LB 17    ID x PO    (Note:  Leading CM behaves like ID)
        //          AL x NU
        //          NU x AL
        if (fID->contains(prevChar) && fPO->contains(thisChar) ||
            fCM->contains(prevChar) && fPO->contains(thisChar) || 
            fAL->contains(prevChar) && fNU->contains(thisChar) ||
            fNU->contains(prevChar) && fAL->contains(thisChar) )   {
            continue; 
        }

        // LB 18    Numbers
        UnicodeString  subStr18(*fText, prevPos);
        fNumberMatcher->reset(subStr18);
        if (fNumberMatcher->lookingAt(status)) {
            // TODO:  Check status codes
            // Matched a number.  But could have been just a single digit, which would
            //    not represent a "no break here" between prevChar and thisChar
            int32_t numEndIdx = prevPos + fNumberMatcher->end(status);  // idx of first char following num
            if (numEndIdx > pos) {
                // Number match includes at least our two chars being checked
                if (numEndIdx > nextPos) {
                    // Number match includes additional chars.  Update pos and nextPos
                    //   so that next loop iteration will continue at the end of the number,
                    //   checking for breaks between last char in number & whatever follows.
                    nextPos = numEndIdx;
                    pos = fCharBI->preceding(numEndIdx); 
                    thisChar = fText->char32At(pos);
                    while (fCM->contains(thisChar)) {
                        pos = fCharBI->preceding(pos);
                        thisChar = fText->char32At(pos);
                    }
                }
                continue;
            }
        }

        if (fPR->contains(prevChar) && fAL->contains(thisChar)) {
            continue;
        }

        if (fPR->contains(prevChar) && fID->contains(thisChar)) {
            continue;
        }

        // LB 18b
        if (fHY->contains(prevChar) || fBB->contains(thisChar)) {
            break;
        }

        // LB 19
        if (fAL->contains(prevChar) && fAL->contains(thisChar)) {
            continue;
        }

        // LB 19b
        if (fIS->contains(prevChar) && fAL->contains(thisChar)) {
            continue;
        }

        // LB 20    Break everywhere else
        break;
            
    }
    
    return pos;
}


UVector  *RBBILineMonkey::charClasses() {
    return fSets;
}


RBBILineMonkey::~RBBILineMonkey() {
    delete fSets;

    delete fBK;
    delete fCR;
    delete fLF;
    delete fCM;
    delete fNL;
    delete fWJ;
    delete fZW;
    delete fGL;
    delete fCB;
    delete fSP;
    delete fB2;
    delete fBA;
    delete fBB;
    delete fHY;
    delete fCL;
    delete fEX;
    delete fIN;
    delete fNS;
    delete fOP;
    delete fQU;
    delete fIS;
    delete fNU;
    delete fPO;
    delete fPR;
    delete fSY;
    delete fAI;
    delete fAL;
    delete fID;
    delete fSA;
    delete fXX;

    delete fCharBI;
    delete fNumberMatcher;
    delete fLB10Matcher;
    delete fLB11Matcher;
}


//-------------------------------------------------------------------------------------------
//
//   TestMonkey
//
//     params
//       seed=nnnnn        Random number starting seed.
//                         Setting the seed allows errors to be reproduced.
//       loop=nnn          Looping count.  Controls running time.
//                         -1:  run forever.
//                          0 or greater:  run length.
//
//       type = char | word | line | sent | title
//
//-------------------------------------------------------------------------------------------

static int32_t  getIntParam(UnicodeString name, UnicodeString &params, int32_t defaultVal) {
    int32_t val = defaultVal;
    name.append(" *= *(-?\\d+)");
    UErrorCode status = U_ZERO_ERROR;
    RegexMatcher m(name, params, 0, status);
    if (m.find()) {
        // The param exists.  Convert the string to an int.
        char valString[100];
        int32_t paramLength = m.end(1, status) - m.start(1, status);
        if (paramLength >= (int32_t)(sizeof(valString)-1)) {
            paramLength = (int32_t)(sizeof(valString)-2);
        }
        params.extract(m.start(1, status), paramLength, valString, sizeof(valString));
        val = strtol(valString,  NULL, 10);

        // Delete this parameter from the params string.
        m.reset();
        params = m.replaceFirst("", status);
    }
    U_ASSERT(U_SUCCESS(status));
    return val;
}
#endif

static void testBreakBoundPreceding(RBBITest *test, UnicodeString ustr, 
                                    BreakIterator *bi,
                                    int expected[], 
                                    int expectedcount)
{
    int count = 0;
    int i = 0;
    int forward[50];
    bi->setText(ustr);
    for (i = bi->first(); i != BreakIterator::DONE; i = bi->next()) {
        forward[count] = i;
        if (count < expectedcount && expected[count] != i) {
            test->errln("break forward test failed: expected %d but got %d", 
                        expected[count], i);
            break;
        }
        count ++;
    }
    if (count != expectedcount) {
        printStringBreaks(ustr, expected, expectedcount);
        test->errln("break test failed: missed %d match", 
                    expectedcount - count);
        return;
    }
    // testing boundaries
    for (i = 1; i < expectedcount; i ++) {
        int j = expected[i - 1];
        if (!bi->isBoundary(j)) {
            printStringBreaks(ustr, expected, expectedcount);
            test->errln("Expected boundary at position %d", j);
            return;
        }
        for (j = expected[i - 1] + 1; j < expected[i]; j ++) {
            if (bi->isBoundary(j)) {
                printStringBreaks(ustr, expected, expectedcount);
                test->errln("Not expecting boundary at position %d", j);
                return;
            }
        }
    }

    for (i = bi->last(); i != BreakIterator::DONE; i = bi->previous()) {
        count --;
        if (forward[count] != i) {
            test->errln("happy break test reverse failed: expected %d but got %d", 
                        forward[count], i);
            break;
        }
    }
    if (count != 0) {
        printStringBreaks(ustr, expected, expectedcount);
        test->errln("happy break test failed: missed a match");
        return;
    }

    // testing preceding
    for (i = 0; i < expectedcount - 1; i ++) {
        int j = expected[i] + 1;
        for (; j <= expected[i + 1]; j ++) {
            if (bi->preceding(j) != expected[i]) {
                printStringBreaks(ustr, expected, expectedcount);
                test->errln("Not expecting backwards boundary at position %d", j);
                return;
            }
        }
    }    
}

void RBBITest::TestWordBreaks(void)
{
#if !UCONFIG_NO_REGULAR_EXPRESSIONS

    // <data><>\u1d4a\u206e<?>\u0603\U0001d7ff<>\u2019<></data>
    Locale        locale("en");
    UErrorCode    status = U_ZERO_ERROR;
    // BreakIterator  *bi = BreakIterator::createCharacterInstance(locale, status);
    BreakIterator *bi = BreakIterator::createWordInstance(locale, status);
    UChar         str[300]; 
    static const char *strlist[] = 
    {
    "\\U000e0032\\u0097\\u0f94\\uc2d8\\u05f4\\U000e0031\\u060d",
    "\\U000e0037\\u4666\\u1202\\u003a\\U000e0031\\u064d\\u0bea\\u591c\\U000e0040\\u003b",
    "\\u0589\\u3e99\\U0001d7f3\\U000e0074\\u1810\\u200e\\U000e004b\\u179c\\u0027\\U000e0061\\u003a",
    "\\u398c\\U000104a5\\U0001d173\\u102d\\u002e\\uca3b\\u002e\\u002c\\u5622",
    "\\u90ca\\u3588\\u009c\\u0953\\u194b",
    "\\u200e\\U000e0072\\u0a4b\\U000e003f\\ufd2b\\u2027\\u002e\\u002e",
    "\\u0602\\u2019\\ua191\\U000e0063\\u0a4c\\u003a\\ub4b5\\u003a\\u827f\\u002e",
    "\\u7f1f\\uc634\\u65f8\\u0944\\u04f2\\uacdf\\u1f9c\\u05f4\\u002e",
    "\\U000e0042\\u002e\\u0fb8\\u09ef\\u0ed1\\u2044",
    "\\u003b\\u024a\\u102e\\U000e0071\\u0600",
    "\\u2027\\U000e0067\\u0a47\\u00b7",
    "\\u1fcd\\u002c\\u07aa\\u0027\\u11b0",
    "\\u002c\\U000e003c\\U0001d7f4\\u003a\\u0c6f\\u0027",
    "\\u0589\\U000e006e\\u0a42\\U000104a5",
    "\\u4f66\\ub523\\u003a\\uacae\\U000e0047\\u003a",
    "\\u003a\\u0f21\\u0668\\u0dab\\u003a\\u0655\\u00b7",
    "\\u0027\\u11af\\U000e0057\\u0602",
    "\\U0001d7f2\\U000e007\\u0004\\u0589",
    "\\U000e0022\\u003a\\u10b3\\u003a\\ua21b\\u002e\\U000e0058\\u1732\\U000e002b",
    "\\U0001d7f2\\U000e007d\\u0004\\u0589",
    "\\u82ab\\u17e8\\u0736\\u2019\\U0001d64d",
    "\\u0e01\\ub55c\\u0a68\\U000e0037\\u0cd6\\u002c\\ub959",
    "\\U000e0065\\u302c\\uc986\\u09ee\\U000e0068",
    "\\u0be8\\u002e\\u0c68\\u066e\\u136d\\ufc99\\u59e7",
    "\\u0233\\U000e0020\\u0a69\\u0d6a",
    "\\u206f\\u0741\\ub3ab\\u2019\\ubcac\\u2019",
    "\\u58f4\\U000e0049\\u20e7\\u2027",
    "\\ub315\\U0001d7e5\\U000e0073\\u0c47\\u06f2\\u0c6a\\u0037\\u10fe",
    "\\ua183\\u102d\\u0bec\\u003a",
    "\\u17e8\\u06e7\\u002e\\u096d\\u003b",
    "\\u003a\\u0e57\\u0fad\\u002e",
    "\\u002e\\U000e004c\\U0001d7ea\\u05bb\\ud0fd\\u02de",
    "\\u32e6\\U0001d7f6\\u0fa1\\u206a\\U000e003c\\u0cec\\u003a",
    "\\U000e005d\\u2044\\u0731\\u0650\\u0061",
    "\\u003a\\u0664\\u00b7\\u1fba",
    "\\u003b\\u0027\\u00b7\\u47a3",
    "\\u2027\\U000e0067\\u0a42\\u00b7\\ubddf\\uc26c\\u003a\\u4186\\u041b",
    "\\u0027\\u003a\\U0001d70f\\U0001d7df\\ubf4a\\U0001d7f5\\U0001d177\\u003a\\u0e51\\u1058\\U000e0058\\u00b7\\u0673",
    "\\uc30d\\u002e\\U000e002c\\u0c48\\u003a\\ub5a1\\u0661\\u002c",
    };
    int loop;
    for (loop = 0; loop < (int)(sizeof(strlist) / sizeof(char *)); loop ++) {
        // printf("looping %d\n", loop);
        u_unescape(strlist[loop], str, 25);
        UnicodeString ustr(str);
        // RBBICharMonkey monkey;
        RBBIWordMonkey monkey;

        int expected[50];
        int expectedcount = 0;

        monkey.setText(ustr);
        int i;
        for (i = 0; i != BreakIterator::DONE; i = monkey.next(i)) {
            expected[expectedcount ++] = i;
        }

        testBreakBoundPreceding(this, ustr, bi, expected, expectedcount);
    }
    delete bi;
#endif
}

void RBBITest::TestWordBoundary(void)
{
    // <data><>\u1d4a\u206e<?>\u0603\U0001d7ff<>\u2019<></data>
    Locale        locale("en");
    UErrorCode    status = U_ZERO_ERROR;
    // BreakIterator  *bi = BreakIterator::createCharacterInstance(locale, status);
    BreakIterator *bi = BreakIterator::createWordInstance(locale, status);
    UChar         str[50]; 
    static const char *strlist[] = 
    {
    "\\u200e\\U000e0072\\u0a4b\\U000e003f\\ufd2b\\u2027\\u002e\\u002e",
    "\\U000e0042\\u002e\\u0fb8\\u09ef\\u0ed1\\u2044",
    "\\u003b\\u024a\\u102e\\U000e0071\\u0600",
    "\\u2027\\U000e0067\\u0a47\\u00b7",
    "\\u1fcd\\u002c\\u07aa\\u0027\\u11b0",
    "\\u002c\\U000e003c\\U0001d7f4\\u003a\\u0c6f\\u0027",
    "\\u0589\\U000e006e\\u0a42\\U000104a5",
    "\\u4f66\\ub523\\u003a\\uacae\\U000e0047\\u003a",
    "\\u003a\\u0f21\\u0668\\u0dab\\u003a\\u0655\\u00b7",
    "\\u0027\\u11af\\U000e0057\\u0602",
    "\\U0001d7f2\\U000e007\\u0004\\u0589",
    "\\U000e0022\\u003a\\u10b3\\u003a\\ua21b\\u002e\\U000e0058\\u1732\\U000e002b",
    "\\U0001d7f2\\U000e007d\\u0004\\u0589",
    "\\u82ab\\u17e8\\u0736\\u2019\\U0001d64d",
    "\\u0e01\\ub55c\\u0a68\\U000e0037\\u0cd6\\u002c\\ub959",
    "\\U000e0065\\u302c\\uc986\\u09ee\\U000e0068",
    "\\u0be8\\u002e\\u0c68\\u066e\\u136d\\ufc99\\u59e7",
    "\\u0233\\U000e0020\\u0a69\\u0d6a",
    "\\u206f\\u0741\\ub3ab\\u2019\\ubcac\\u2019",
    "\\u58f4\\U000e0049\\u20e7\\u2027",
    "\\ub315\\U0001d7e5\\U000e0073\\u0c47\\u06f2\\u0c6a\\u0037\\u10fe",
    "\\ua183\\u102d\\u0bec\\u003a",
    "\\u17e8\\u06e7\\u002e\\u096d\\u003b",
    "\\u003a\\u0e57\\u0fad\\u002e",
    "\\u002e\\U000e004c\\U0001d7ea\\u05bb\\ud0fd\\u02de",
    "\\u32e6\\U0001d7f6\\u0fa1\\u206a\\U000e003c\\u0cec\\u003a",
    "\\ua2a5\\u0038\\u2044\\u002e\\u0c67\\U000e003c\\u05f4\\u2027\\u05f4\\u2019",
    "\\u003a\\u0664\\u00b7\\u1fba",
    "\\u003b\\u0027\\u00b7\\u47a3",
    };
    int loop;
    for (loop = 0; loop < (int)(sizeof(strlist) / sizeof(char *)); loop ++) {
        // printf("looping %d\n", loop);
        u_unescape(strlist[loop], str, 20);
        UnicodeString ustr(str);
        int forward[50];
        int count = 0;
        
        bi->setText(ustr);
        int prev = 0;
        int i;
        for (i = bi->first(); i != BreakIterator::DONE; i = bi->next()) {
            forward[count ++] = i;
            if (i > prev) {
                int j;
                for (j = prev + 1; j < i; j ++) {
                    if (bi->isBoundary(j)) {
                        printStringBreaks(ustr, forward, count);
                        errln("happy boundary test failed: expected %d not a boundary", 
                               j);
                        return;
                    }
                }
            }
            if (!bi->isBoundary(i)) {
                printStringBreaks(ustr, forward, count);
                errln("happy boundary test failed: expected %d a boundary", 
                       i);
                return;
            }
            prev = i;
        }
    }
    delete bi;
}

void RBBITest::TestLineBreaks(void)
{
#if !UCONFIG_NO_REGULAR_EXPRESSIONS
    Locale        locale("en");
    UErrorCode    status = U_ZERO_ERROR;
    BreakIterator *bi = BreakIterator::createLineInstance(locale, status);
    UChar         str[50]; 
    static const char *strlist[] = 
    {
     "\\u0668\\u192b\\u002f\\u2034\\ufe39\\u00b4\\u0cc8\\u2571\\u200b\\u003f",
     "\\ufeff\\ufffc\\u3289\\u0085\\u2772\\u0020\\U000e010a\\u0020\\u2025\\u000a\\U000e0123",
     "\\ufe3c\\u201c\\u000d\\u2025\\u2007\\u201c\\u002d\\u20a0\\u002d\\u30a7\\u17a4",
     "\\u2772\\u0020\\U000e010a\\u0020\\u2025\\u000a\\U000e0123",
     "\\u002d\\uff1b\\u02c8\\u2029\\ufeff\\u0f22\\u2044\\ufe09\\u003a\\u096d\\u2009\\u000a\\u06f7\\u02cc\\u1019\\u2060",
     "\\u1781\\u0b68\\u0f0c\\u3010\\u0085\\U00011f7a\\u0020\\u0dd6\\u200b\\U000e007a\\u000a\\u2060\\u2026\\u002f\\u2026\\u24dc\\u101e\\u2014\\u2007\\u30a5",
     "\\u2770\\u0020\\U000e010f\\u0020\\u2060\\u000a\\u02cc\\u0bcc\\u060d\\u30e7\\u0f3b\\u002f",
     "\\ufeff\\u0028\\u003b\\U00012fec\\u2010\\u0020\\u0004\\u200b\\u0020\\u275c\\u002f\\u17b1",
     "\\u20a9\\u2014\\u00a2\\u31f1\\u002f\\u0020\\u05b8\\u200b\\u0cc2\\u003b\\u060d\\u02c8\\ua4e8\\u002f\\u17d5",
     "\\u002d\\u136f\\uff63\\u0084\\ua933\\u2028\\u002d\\u431b\\u200b\\u20b0",
     "\\uade3\\u11d6\\u000a\\U0001107d\\u203a\\u201d\\ub070\\u000d\\u2024\\ufffc",
     "\\uff5b\\u101c\\u1806\\u002f\\u2213\\uff5f",
     "\\u2014\\u0a83\\ufdfc\\u003f\\u00a0\\u0020\\u000a\\u2991\\U0001d179\\u0020\\u201d\\U000125f6\\u0a67\\u20a7\\ufeff\\u043f",
     "\\u169b\\U000e0130\\u002d\\u1041\\u0f3d\\u0abf\\u00b0\\u31fb\\u00a0\\u002d\\u02c8\\u003b",
     "\\u2762\\u1680\\u002d\\u2028\\u0027\\u01dc\\ufe56\\u003a\\u000a\\uffe6\\u29fd\\u0020\\u30ee\\u007c\\U0001d178\\u0af1\\u0085",
     "\\u3010\\u200b\\u2029\\ufeff\\ufe6a\\u275b\\U000e013b\\ufe37\\u24d4\\u002d\\u1806\\u256a\\u1806\\u247c\\u0085\\u17ac",
     "\\u99ab\\u0027\\u003b\\u2026\\ueaf0\\u0020\\u0020\\u0313\\u0020\\u3099\\uff09\\u208e\\u2011\\u2007\\u2060\\u000a\\u0020\\u0020\\u300b\\u0bf9",
     "\\u1806\\u060d\\u30f5\\u00b4\\u17e9\\u2544\\u2028\\u2024\\u2011\\u20a3\\u002d\\u09cc\\u1782\\u000d\\uff6f\\u0025",
     "\\u002f\\uf22e\\u1944\\ufe3d\\u0020\\u206f\\u31b3\\u2014\\u002d\\u2025\\u0f0c\\u0085\\u2763",
     "\\u002f\\u2563\\u202f\\u0085\\u17d5\\u200b\\u0020\\U000e0043\\u2014\\u058a\\u3d0a\\ufe57\\u2035\\u2028\\u2029",
     "\\u20ae\\U0001d169\\u9293\\uff1f\\uff1f\\u0021\\u2012\\u2039\\u0085\\u02cc\\u00a2\\u0020\\U000e01ab\\u3085\\u0f3a\\u1806\\u0f0c\\u1945\\u000a\\U0001d7e7",
     "\\uffe6\\u00a0\\u200b\\u0085\\u2116\\u255b\\U0001d7f7\\u178c\\ufffc",
     "\\u02cc\\ufe6a\\u00a0\\u0021\\u002d\\u7490\\uec2e\\u200b\\u000a",
     "\\uec2e\\u200b\\u000a\\u0020\\u2028\\u2014\\u8945",
     "\\u7490\\uec2e\\u200b\\u000a\\u0020\\u2028\\u2014",
     "\\u0020\\u2028\\u2014\\u8945\\u002c\\u005b",
     "\\u000a\\ufe3c\\u201c\\u000d\\u2025\\u2007\\u201c\\u002d\\u20a0",
     "\\u2473\\u0e9d\\u0020\\u0085\\u000a\\ufe3c\\u201c\\u000d\\u2025",
     "\\U0001d16e\\ufffc\\u2025\\u0021\\u002d",
     "\\ufffc\\u301b\\u0fa5\\U000e0103\\u2060\\u208e\\u17d5\\u034f\\u1009\\u003a\\u180e\\u2009\\u3111",
     "\\u2014\\u0020\\u000a\\u17c5\\u24fc",
     "\\ufffc\\u0020\\u2116\\uff6c\\u200b\\u0ac3\\U0001028f",
     "\\uaeb0\\u0344\\u0085\\ufffc\\u073b\\u2010",
     "\\ufeff\\u0589\\u0085\\u0eb8\\u30fd\\u002f\\u003a\\u2014\\ufe43",
     "\\u09cc\\u256a\\u276d\\u002d\\u3085\\u000d\\u0e05\\u2028\\u0fbb",
     "\\u2034\\u00bb\\u0ae6\\u300c\\u0020\\u31f8\\ufffc",
     "\\u2116\\u0ed2\\uff64\\u02cd\\u2001\\u2060",
    };
    int loop;
    for (loop = 0; loop < (int)(sizeof(strlist) / sizeof(char *)); loop ++) {
        // printf("looping %d\n", loop);
        u_unescape(strlist[loop], str, 20);
        UnicodeString ustr(str);
        RBBILineMonkey monkey;

        int expected[50];
        int expectedcount = 0;

        monkey.setText(ustr);
        int i;
        for (i = 0; i != BreakIterator::DONE; i = monkey.next(i)) {
            expected[expectedcount ++] = i;
        }

        testBreakBoundPreceding(this, ustr, bi, expected, expectedcount);
    }
    delete bi;
#endif
}

void RBBITest::TestSentBreaks(void)
{
    Locale        locale("en");
    UErrorCode    status = U_ZERO_ERROR;
    BreakIterator *bi = BreakIterator::createSentenceInstance(locale, status);
    UChar         str[100]; 
    static const char *strlist[] = 
    {
     "Now\ris\nthe\r\ntime\n\rfor\r\r",
     "This\n",
     "Hello! how are you? I'am fine. Thankyou. How are you doing? This\n costs $20,00,000.",
     "\"Sentence ending with a quote.\" Bye.",
     "  (This is it).  Testing the sentence iterator. \"This isn't it.\"", 
     "Hi! This is a simple sample sentence. (This is it.) This is a simple sample sentence. \"This isn't it.\"",
     "Hi! This is a simple sample sentence. It does not have to make any sense as you can see. ",
     "Nel mezzo del cammin di nostra vita, mi ritrovai in una selva oscura. ",
     "Che la dritta via aveo smarrita. He said, that I said, that you said!! ",
     "Don't rock the boat.\\u2029Because I am the daddy, that is why. Not on my time (el timo.)!",
    };
    int loop;
    int forward[100];
	for (loop = 0; loop < (int)(sizeof(strlist) / sizeof(char *)); loop ++) {
        u_unescape(strlist[loop], str, 100);
        UnicodeString ustr(str);

        int count = 0;
        bi->setText(ustr);
        int i;
        for (i = bi->first(); i != BreakIterator::DONE; i = bi->next()) {
            forward[count ++] = i;
        }
        testBreakBoundPreceding(this, ustr, bi, forward, count);
    }
    delete bi;
}

void RBBITest::TestMonkey(char *params) {
#if !UCONFIG_NO_REGULAR_EXPRESSIONS

    UErrorCode     status    = U_ZERO_ERROR;
    int32_t        loopCount = 500;
    int32_t        seed      = 1;
    UnicodeString  breakType = "all";
    Locale         locale("en");

    if (quick == FALSE) {
        loopCount = 10000;
    }

    if (params) {
        UnicodeString p(params);
        loopCount = getIntParam("loop", p, loopCount);
        seed      = getIntParam("seed", p, seed);

        RegexMatcher m(" *type *= *(char|word|line|sent|title) *", p, 0, status);
        if (m.find()) {
            breakType = m.group(1, status);
            m.reset();
            p = m.replaceFirst("", status);
        }

        m.reset(p);
        if (RegexMatcher("\\S", p, 0, status).find()) {
            // Each option is stripped out of the option string as it is processed.
            // All options have been checked.  The option string should have been completely emptied..
            char buf[100];
            p.extract(buf, sizeof(buf), NULL, status);
            buf[sizeof(buf)-1] = 0;
            errln("Unrecognized or extra parameter:  %s\n", buf);
            return;
        }

    }

    if (breakType == "char" || breakType == "all") {
        RBBICharMonkey  m;
        BreakIterator  *bi = BreakIterator::createCharacterInstance(locale, status);
        RunMonkey(bi, m, "char", seed, loopCount);
        delete bi;
    }

    if (breakType == "word" || breakType == "all") {
        logln("Word Break Monkey Test");
        RBBIWordMonkey  m;
        BreakIterator  *bi = BreakIterator::createWordInstance(locale, status);
        RunMonkey(bi, m, "word", seed, loopCount);
        delete bi;
    }

    if (breakType == "line" || breakType == "all") {
        logln("Line Break Monkey Test");
        RBBILineMonkey  m;
        BreakIterator  *bi = BreakIterator::createLineInstance(locale, status);
        if (params == NULL) {
            loopCount = 50;
        }
        RunMonkey(bi, m, "line", seed, loopCount);
        delete bi;
    }


#endif
}

//
//  Run a RBBI monkey test.  Common routine, for all break iterator types.
//    Parameters:
//       bi      - the break iterator to use
//       mk      - MonkeyKind, abstraction for obtaining expected results
//       name    - Name of test (char, word, etc.) for use in error messages
//       seed    - Seed for starting random number generator (parameter from user)
//       numIterations
//
void RBBITest::RunMonkey(BreakIterator *bi, RBBIMonkeyKind &mk, const char *name, uint32_t  seed, int32_t numIterations) {

#if !UCONFIG_NO_REGULAR_EXPRESSIONS

    const int32_t    TESTSTRINGLEN = 500;
    UnicodeString    testText;
    int32_t          numCharClasses;
    UVector          *chClasses;
    int              expected[TESTSTRINGLEN*2 + 1];
    int              expectedCount = 0;
    char             expectedBreaks[TESTSTRINGLEN*2 + 1];
    char             forwardBreaks[TESTSTRINGLEN*2 + 1];
    char             reverseBreaks[TESTSTRINGLEN*2+1];
    char             isBoundaryBreaks[TESTSTRINGLEN*2+1];
    char             followingBreaks[TESTSTRINGLEN*2+1];
    char             precedingBreaks[TESTSTRINGLEN*2+1];
    int              i;
    int              loopCount = 0;

    m_seed = seed;

    numCharClasses = mk.charClasses()->size();
    chClasses      = mk.charClasses();

    // Check for errors that occured during the construction of the MonkeyKind object.
    //  Can't report them where they occured because errln() is a method coming from intlTest,
    //  and is not visible outside of RBBITest :-(
    if (U_FAILURE(mk.deferredStatus)) {
        errln("status of \"%s\" in creation of RBBIMonkeyKind.", u_errorName(mk.deferredStatus));
        return;
    }

    // Verify that the character classes all have at least one member.
    for (i=0; i<numCharClasses; i++) {
        UnicodeSet *s = (UnicodeSet *)chClasses->elementAt(i);
        if (s == NULL || s->size() == 0) {
            errln("Character Class #%d is null or of zero size.", i);
            return;
        }
    }

    while (loopCount < numIterations || numIterations == -1) {
        if (numIterations == -1 && loopCount % 10 == 0) {
            // If test is running in an infinite loop, display a periodic tic so
            //   we can tell that it is making progress.
            fprintf(stderr, ".");
        }
        // Save current random number seed, so that we can recreate the random numbers
        //   for this loop iteration in event of an error.
        seed = m_seed;

        // Populate a test string with data.
        testText.truncate(0);
        for (i=0; i<TESTSTRINGLEN; i++) {
            int32_t  aClassNum = m_rand() % numCharClasses;
            UnicodeSet *classSet = (UnicodeSet *)chClasses->elementAt(aClassNum);
            int32_t   charIdx = m_rand() % classSet->size();
            UChar32   c = classSet->charAt(charIdx);
            if (c < 0) {   // TODO:  deal with sets containing strings.
                errln("c < 0");
            }
            testText.append(c);
        }

        // Calculate the expected results for this test string.
        mk.setText(testText);
        memset(expectedBreaks, 0, sizeof(expectedBreaks));
        expectedBreaks[0] = 1;
        int32_t breakPos = 0;
        expectedCount = 0;
        for (;;) {
            breakPos = mk.next(breakPos);
            if (breakPos == -1) {
                break;
            }
            if (breakPos > testText.length()) {
                errln("breakPos > testText.length()");
            }
            expectedBreaks[breakPos] = 1;
            expected[expectedCount ++] = breakPos;
        }

        // Find the break positions using forward iteration
        memset(forwardBreaks, 0, sizeof(forwardBreaks));
        bi->setText(testText);
        for (i=bi->first(); i != BreakIterator::DONE; i=bi->next()) {
            if (i < 0 || i > testText.length()) {
                errln("%s break monkey test: Out of range value returned by breakIterator::next()", name);
                break;
            }
            forwardBreaks[i] = 1;
        }

        // Find the break positions using reverse iteration
        memset(reverseBreaks, 0, sizeof(reverseBreaks));
        for (i=bi->last(); i != BreakIterator::DONE; i=bi->previous()) {
            if (i < 0 || i > testText.length()) {
                errln("%s break monkey test: Out of range value returned by breakIterator::next()", name);
                break;
            }
            reverseBreaks[i] = 1;
        }

        // Find the break positions using isBoundary() tests.
        memset(isBoundaryBreaks, 0, sizeof(isBoundaryBreaks));
        U_ASSERT(sizeof(isBoundaryBreaks) > testText.length());
        for (i=0; i<=testText.length(); i++) {
            isBoundaryBreaks[i] = bi->isBoundary(i);
        }


        // Find the break positions using the following() function.
        // printf(".");
        memset(followingBreaks, 0, sizeof(followingBreaks));
        int32_t   lastBreakPos = 0;
        followingBreaks[0] = 1;
        for (i=0; i<testText.length(); i++) {
            breakPos = bi->following(i);
            if (breakPos <= i ||
                breakPos < lastBreakPos ||
                breakPos > testText.length() ||
                breakPos > lastBreakPos && lastBreakPos > i ) {
                errln("%s break monkey test: "
                    "Out of range value returned by BreakIterator::following().\n"
                    "Random seed=%d",  name, seed);
                break;
            }
            followingBreaks[breakPos] = 1;
            lastBreakPos = breakPos;
        }

        // Find the break positions using the preceding() function.
        memset(precedingBreaks, 0, sizeof(followingBreaks));
        lastBreakPos = testText.length();
        precedingBreaks[testText.length()] = 1;
        for (i=testText.length(); i>0; i--) {
            breakPos = bi->preceding(i);
            if (breakPos >= i ||
                breakPos > lastBreakPos ||
                breakPos < 0 ||
                breakPos < lastBreakPos && lastBreakPos < i ) {
                errln("%s break monkey test: "
                    "Out of range value returned by BreakIterator::preceding().\n"
                    "index=%d;  prev returned %d; lastBreak=%d" ,
                    name,  i, breakPos, lastBreakPos);
                precedingBreaks[i] = 2;   // Forces an error.
            } else {
                precedingBreaks[breakPos] = 1;
                lastBreakPos = breakPos;
            }
        }

        // Compare the expected and actual results.
        for (i=0; i<=testText.length(); i++) {
            const char *errorType = NULL;
            if  (forwardBreaks[i] != expectedBreaks[i]) {
                errorType = "next()";
            } else if (reverseBreaks[i] != forwardBreaks[i]) {
                errorType = "previous()";
            } else if (isBoundaryBreaks[i] != expectedBreaks[i]) {
                errorType = "isBoundary()";
            } else if (followingBreaks[i] != expectedBreaks[i]) {
                errorType = "following()";
            } else if (precedingBreaks[i] != expectedBreaks[i]) {
                errorType = "preceding()";
            }


            if (errorType != NULL) {
                // Format a range of the test text that includes the failure as
                //  a data item that can be included in the rbbi test data file.

                // Start of the range is the last point where expected and actual results
                //   both agreed that there was a break position.
                int startContext = i;
                int32_t count = 0;
                for (;;) {
                    if (startContext==0) { break; }
                    startContext --;
                    if (expectedBreaks[startContext] != 0) {
                        if (count == 2) break;
                        count ++;
                    }
                }

                // End of range is two expected breaks past the start position.
                int endContext = i + 1;
                int ci;
                for (ci=0; ci<2; ci++) {  // Number of items to include in error text.
                    for (;;) {
                        if (endContext >= testText.length()) {break;}
                        if (expectedBreaks[endContext-1] != 0) { 
                            if (count == 0) break;
                            count --;
                        }
                        endContext ++;
                    }
                }

                // Format looks like   "<data><>\uabcd\uabcd<>\U0001abcd...</data>"
                UnicodeString errorText = "<data>";
                /***if (strcmp(errorType, "next()") == 0) {
                    startContext = 0;
                    endContext = testText.length();
                   
                    printStringBreaks(testText, expected, expectedCount);
                }***/

                for (ci=startContext; ci<endContext;) {
                    UnicodeString hexChars("0123456789abcdef");
                    UChar32  c;
                    int      bn;
                    c = testText.char32At(ci);
                    if (ci == i) {
                        // This is the location of the error.
                        errorText.append("<?>");
                    } else if (expectedBreaks[ci] != 0) {
                        // This a non-error expected break position.
                        errorText.append("<>");
                    }
                    if (c < 0x10000) {
                        errorText.append("\\u");
                        for (bn=12; bn>=0; bn-=4) {
                            errorText.append(hexChars.charAt((c>>bn)&0xf));
                        }
                    } else {
                        errorText.append("\\U");
                        for (bn=28; bn>=0; bn-=4) {
                            errorText.append(hexChars.charAt((c>>bn)&0xf));
                        }
                    }
                    ci = testText.moveIndex32(ci, 1);
                }
                errorText.append("<>");
                errorText.append("</data>\n");

                // Output the error
                char  charErrorTxt[500];
                UErrorCode status = U_ZERO_ERROR;
                errorText.extract(charErrorTxt, sizeof(charErrorTxt), NULL, status);
                charErrorTxt[sizeof(charErrorTxt)-1] = 0;
                errln("%s break monkey test error.  %s. Operation = %s; Random seed = %d;  buf Idx = %d\n%s",
                    name, (expectedBreaks[i]? "break expected but not found" : "break found but not expected"),
                    errorType, seed, i, charErrorTxt);
                break;
            }
        }

        loopCount++;
    }
#endif
}


#endif /* #if !UCONFIG_NO_BREAK_ITERATION */
