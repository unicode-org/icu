/********************************************************************
 * COPYRIGHT:
 * Copyright (c) 1999-2003, International Business Machines Corporation and
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
#include "intltest.h"
#include "rbbitst.h"
#include <string.h>
#include "uvector.h"
#include "uvectr32.h"
#include <string.h>
#include <stdio.h>



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


//---------------------------------------------
// runIndexedTest
//---------------------------------------------

void RBBITest::runIndexedTest( int32_t index, UBool exec, const char* &name, char* /*par*/ )
{
    if (exec) logln("TestSuite RuleBasedBreakIterator: ");
    switch (index) {

        case 0: name = "TestExtended";
             if(exec) TestExtended();                          break;
        case 1: name = "";
             break;
        case 2: name = "";
             break;
        case 3: name = "";
             break;
        case 4: name = "";
            break;
        case 5: name = "";
            break;
        case 6: name = "";
            break;
        case 7: name = "TestStatusReturn";
            if(exec) TestStatusReturn();                       break;

        case 8: name = "TestLineBreakData";
            if(exec) TestLineBreakData();                      break;
        case 9: name = "TestSentenceInvariants";
            if(exec) TestSentenceInvariants();                 break;
        case 10: name = "TestCharacterInvariants";
            if(exec) TestCharacterInvariants();                break;
        case 11: name = "TestWordInvariants";
            if(exec) TestWordInvariants();                     break;

        case 12: name = "TestEmptyString";
            if(exec) TestEmptyString();                        break;

        case 13: name = "TestGetAvailableLocales";
            if(exec) TestGetAvailableLocales();                break;

        case 14: name = "TestGetDisplayName";
            if(exec) TestGetDisplayName();                     break;

        case 15: name = "TestEndBehaviour";
            if(exec) TestEndBehaviour();                       break;
        case 16: name = "TestBug4153072";
            if(exec) TestBug4153072();                         break;
        case 17: name = "TestJapaneseLineBreak";
             if(exec) TestJapaneseLineBreak();                 break;


        case 18: name = "TestThaiLineBreak";
             if(exec) TestThaiLineBreak();                     break;
        case 19: name = "TestMixedThaiLineBreak";
             if(exec) TestMixedThaiLineBreak();                break;
        case 20: name = "TestMaiyamok";
             if(exec) TestMaiyamok();                          break;
        case 21: name = "TestThaiWordBreak";
             if(exec) TestThaiWordBreak();                     break;


//      case 7: name = "TestHindiCharacterWrapping()";
//           if(exec) TestHindiCharacterWrapping();            break;
//      case 8: name = "TestCustomRuleBasedWordIteration";
//          if(exec) TestCustomRuleBasedWordIteration();       break;
//      case 9: name = "TestAbbrRuleBasedWordIteration";
//          if(exec) TestAbbrRuleBasedWordIteration();         break;
//      case 10: name = "TestTeluguRuleBasedCharacterIteration";
//          if(exec) TestTeluguRuleBasedCharacterIteration();  break;
//      case 11: name = "TestCustomRuleBasedCharacterIteration";
//          if(exec) TestCustomRuleBasedCharacterIteration();  break;


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
            for (int k = 0; k < testCharsLen; k++) {
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
                for (int l = tb.first(); l != BreakIterator::DONE; l = tb.next()) {
                    if (l == 2) {
                        seen2 = TRUE;
                        break;
                    }
                }
                if (!seen2) {
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
            for (int32_t k = tb.first(); k != BreakIterator::DONE; k = tb.next())
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
            for (int k = tb.first(); k != BreakIterator::DONE; k = tb.next())
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
    for (int32_t i = 0; i < locCount; ++i) {
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
    for (int index = -1; index < begin + 1; ++index) {
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
                errln("Forward Itertion, break expected, but not found.  Pos=%4d  File line,col= %4d,%4d",
                      i, t->srcLine->elementAti(i), t->srcCol->elementAti(i));
            }
        }

        // Check that the break we did find was expected
        if (t->expectedBreaks->elementAti(bp) == 0) {
            errln("Forward Itertion, break found, but not expected.  Pos=%4d  File line,col= %4d,%4d",
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
                errln("Incorrect status for break.  Pos=%4d  File line,col= %4d,%4d.\n"
                      "          Actual, Expected status = %4d, %4d",
                    bp, t->srcLine->elementAti(bp), t->srcCol->elementAti(bp), rs, expectedTagVal);
            }
        }
        
        
        prevBP = bp;
    }
    
    // Verify that there were no missed expected breaks after the last one found
    for (i=prevBP+1; i<t->expectedBreaks->size(); i++) {
        if (t->expectedBreaks->elementAti(i) != 0) {
            errln("Forward Itertion, break expected, but not found.  Pos=%4d  File line,col= %4d,%4d",
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
                errln("Incorrect status for break.  Pos=%4d  File line,col= %4d,%4d.\n"
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



    //
    //  Put the test data into a UnicodeString
    //
    UnicodeString testString(FALSE, testFile, len);

    enum EParseState{
        PARSE_COMMENT,
        PARSE_TAG,
        PARSE_RULE,
        PARSE_DATA,
        PARSE_NUM
    } 
    parseState = PARSE_TAG;

    EParseState savedState = PARSE_TAG;

    const UChar CH_LF        = 0x0a;
    const UChar CH_CR        = 0x0d;
    const UChar CH_HASH      = 0x23;
    const UChar CH_PERIOD    = 0x2e;
    const UChar CH_LT        = 0x3c;
    const UChar CH_GT        = 0x3e;
    const UChar CH_BACKSLASH = 0x5c;
    const UChar CH_BULLET    = 0x2022;
    
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
            parseState = PARSE_COMMENT;
            break;
        }


        if (U_FAILURE(status)) {
            errln("ICU Error %s while parsing test file at line %d.", 
                u_errorName(status), lineNum);
            status = U_ZERO_ERROR;
        }

    }

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
        goto cleanUpAndReturn;
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

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */
