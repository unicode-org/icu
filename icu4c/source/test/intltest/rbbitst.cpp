/********************************************************************
 * COPYRIGHT:
 * Copyright (c) 1999-2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/************************************************************************
*   Date        Name        Description
*   12/15/99    Madhu        Creation.
*   01/12/2000  Madhu        Updated for changed API and added new tests
************************************************************************/


#include "intltest.h"
#include "unicode/utypes.h"
#include "unicode/brkiter.h"
#include "unicode/rbbi.h"
#include "unicode/unicode.h"
#include "unicode/uchar.h"
#include "unicode/utf16.h"
#include <stdio.h>
#include "rbbitst.h"
#include <string.h>
#include "unicode/schriter.h"
#include "uvector.h"



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
    int32_t   line     = fLineNum.elementAti(0);
    if (expectedIdx > 0) {
        // The line numbers are off by one because a premature break occurs somewhere
        //    within the previous item, rather than at the start of the current (expected) item.
        //    Similarly, we want to report the offset of the unexpected break from the start of
        //      this previous item.
        line = fLineNum.elementAti(expectedIdx-1);
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


//--------------------------------------------------------------------------------------
//
//    RBBITest 
//
//--------------------------------------------------------------------------------------

#define  halfNA     "\\u0928\\u094d\\u200d"
#define  halfSA     "\\u0938\\u094d\\u200d"
#define  halfCHA    "\\u091a\\u094d\\u200d"
#define  halfKA     "\\u0915\\u094d\\u200d"
#define  deadTA     "\\u0924\\u094d"



//--------------------------------------------------------------------
//tests default rules based character iteration
//--------------------------------------------------------------------
void RBBITest::TestDefaultRuleBasedCharacterIteration()
{
    //   RuleBasedBreakIterator* rbbi=(RuleBasedBreakIterator*)RuleBasedBreakIterator::createCharacterInstance();
    logln((UnicodeString)"Testing the RBBI for character iteration by using default rules");
    //fetch the rules used to create the above RuleBasedBreakIterator
    //    UnicodeString defaultRules=rbbi->getRules();
    //     RuleBasedCharacterIterator charIterDefault = new RuleBasedBreakIterator(defaultRules);

    UErrorCode status=U_ZERO_ERROR;
    RuleBasedBreakIterator* charIterDefault=(RuleBasedBreakIterator*)RuleBasedBreakIterator::createCharacterInstance(Locale::getDefault(), status);
    if(U_FAILURE(status)){
        errln("FAIL : in construction");
        return;
    }

    BITestData     chardata(status);

    ADD_DATACHUNK(chardata, NULL, 0, status);                       // Starting break
    ADD_DATACHUNK(chardata, "H", 0, status);
    ADD_DATACHUNK(chardata, "e", 0, status);
    ADD_DATACHUNK(chardata, "l", 0, status);
    ADD_DATACHUNK(chardata, "l", 0, status);
    ADD_DATACHUNK(chardata, "o", 0, status);
    ADD_DATACHUNK(chardata, "e\\u0301", 0, status);                   //acuteE
    ADD_DATACHUNK(chardata, "&", 0, status);
    ADD_DATACHUNK(chardata, "e\\u0303", 0, status);                   //tildaE
    //devanagiri characters for Hindi support
    ADD_DATACHUNK(chardata, "\\u0906", 0, status);                    //devanagiri AA
    //ADD_DATACHUNK(chardata, "\\u093e\\u0901", 0);              //devanagiri vowelsign AA+ chandrabindhu
    ADD_DATACHUNK(chardata, "\\u0906\\u0901", 0, status);              // Devanagari AA + chandrabindu
    ADD_DATACHUNK(chardata, "\\u0915\\u093e\\u0901", 0, status);       // Devanagari KA + AA vowelsign + chandrabindu

    ADD_DATACHUNK(chardata, "\\u0916\\u0947", 0, status);              //devanagiri KHA+vowelsign E
    ADD_DATACHUNK(chardata, "\\u0938\\u0941\\u0902", 0, status);        //devanagiri SA+vowelsign U + anusvara(bindu)
    ADD_DATACHUNK(chardata, "\\u0926", 0, status);                    //devanagiri consonant DA
    ADD_DATACHUNK(chardata, "\\u0930", 0, status);                    //devanagiri consonant RA
    ADD_DATACHUNK(chardata, "\\u0939\\u094c", 0, status);              //devanagiri HA+vowel sign AI
    ADD_DATACHUNK(chardata, "\\u0964", 0, status);                    //devanagiri danda
    //end hindi characters
    ADD_DATACHUNK(chardata, "A\\u0302", 0, status);                   //circumflexA
    ADD_DATACHUNK(chardata, "i\\u0301", 0, status);                   //acuteBelowI
    // conjoining jamo->..
    ADD_DATACHUNK(chardata, "\\u1109\\u1161\\u11bc", 0, status);
    ADD_DATACHUNK(chardata, "\\u1112\\u1161\\u11bc", 0, status);
    ADD_DATACHUNK(chardata, "\n", 0, status);
    ADD_DATACHUNK(chardata, "\r\n", 0, status);                      //keep CRLF sequences together
    ADD_DATACHUNK(chardata, "S\\u0300", 0, status);                   //graveS
    ADD_DATACHUNK(chardata, "i\\u0301", 0, status);                   //acuteBelowI
    ADD_DATACHUNK(chardata, "!", 0, status);

    // What follows is a string of Korean characters (I found it in the Yellow Pages
    // ad for the Korean Presbyterian Church of San Francisco, and I hope I transcribed
    // it correctly), first as precomposed syllables, and then as conjoining jamo.
    // Both sequences should be semantically identical and break the same way.
    // precomposed syllables...
    ADD_DATACHUNK(chardata, "\\uc0c1", 0, status);
    ADD_DATACHUNK(chardata, "\\ud56d", 0, status);
    ADD_DATACHUNK(chardata, " ", 0, status);
    ADD_DATACHUNK(chardata, "\\ud55c", 0, status);
    ADD_DATACHUNK(chardata, "\\uc778", 0, status);
    ADD_DATACHUNK(chardata, " ", 0, status);
    ADD_DATACHUNK(chardata, "\\uc5f0", 0, status);
    ADD_DATACHUNK(chardata, "\\ud569", 0, status);
    ADD_DATACHUNK(chardata, " ", 0, status);
    ADD_DATACHUNK(chardata, "\\uc7a5", 0, status);
    ADD_DATACHUNK(chardata, "\\ub85c", 0, status);
    ADD_DATACHUNK(chardata, "\\uad50", 0, status);
    ADD_DATACHUNK(chardata, "\\ud68c", 0, status);
    ADD_DATACHUNK(chardata, " ", 0, status);
    // conjoining jamo...
    ADD_DATACHUNK(chardata, "\\u1109\\u1161\\u11bc", 0, status);
    ADD_DATACHUNK(chardata, "\\u1112\\u1161\\u11bc", 0, status);
    ADD_DATACHUNK(chardata, " ", 0, status);
    ADD_DATACHUNK(chardata, "\\u1112\\u1161\\u11ab", 0, status);
    ADD_DATACHUNK(chardata, "\\u110b\\u1175\\u11ab", 0, status);
    ADD_DATACHUNK(chardata, " ", 0, status);
    ADD_DATACHUNK(chardata, "\\u110b\\u1167\\u11ab", 0, status);
    ADD_DATACHUNK(chardata, "\\u1112\\u1161\\u11b8", 0, status);
    ADD_DATACHUNK(chardata, " ", 0, status);
    ADD_DATACHUNK(chardata, "\\u110c\\u1161\\u11bc", 0, status);
    ADD_DATACHUNK(chardata, "\\u1105\\u1169", 0, status);
    ADD_DATACHUNK(chardata, "\\u1100\\u116d", 0, status);
    ADD_DATACHUNK(chardata, "\\u1112\\u116c", 0, status);

    // Surrogate pairs stay together
    ADD_DATACHUNK(chardata, "\\ud800\\udc00", 0, status);
    ADD_DATACHUNK(chardata, "\\udbff\\udfff", 0, status);
    ADD_DATACHUNK(chardata, "x", 0, status);

    // 0xffff is a legal character, and should not stop the break iterator early.
    //    (Requires special casing in implementation, which is why it gets a test.)
    ADD_DATACHUNK(chardata, "\\uffff", 0, status);
    ADD_DATACHUNK(chardata, "\\uffff", 0, status);
    ADD_DATACHUNK(chardata, " ", 0, status);
    ADD_DATACHUNK(chardata, "a", 0, status);

    // Regression test for bug 1889
    ADD_DATACHUNK(chardata, "\\u0f40\\u0f7d", 0, status);
    ADD_DATACHUNK(chardata, "\\u0000", 0, status);
    ADD_DATACHUNK(chardata, "\\u0f7e", 0, status);
    // \u0f7d\u0000\u0f7e

    if(U_FAILURE(status)){
        errln("FAIL : in BITestData construction");
        return;
    }
    // Run the test...
    generalIteratorTest(*charIterDefault, chardata);

    delete charIterDefault;
//   delete rbbi;
}

static const int T_NUMBER = 100;
static const int T_LETTER = 200;
static const int T_H_OR_K = 300;
static const int T_IDEO   = 400;

//--------------------------------------------------------------------
//tests default rules based word iteration
//--------------------------------------------------------------------
void RBBITest::TestDefaultRuleBasedWordIteration()
{
    logln((UnicodeString)"Testing the RBBI for word iteration using default rules");

    UErrorCode status=U_ZERO_ERROR;
    RuleBasedBreakIterator* wordIterDefault=(RuleBasedBreakIterator*)RuleBasedBreakIterator::createWordInstance(Locale::getDefault(), status);
    if(U_FAILURE(status)){
        errln("FAIL : in construction");
        return;
    }

    BITestData worddata(status);
    ADD_DATACHUNK(worddata, NULL, 0, status);
    ADD_DATACHUNK(worddata, "Write", T_LETTER, status);
    ADD_DATACHUNK(worddata, " ", 0, status);
    ADD_DATACHUNK(worddata, "wordrules", T_LETTER, status);
    ADD_DATACHUNK(worddata, ".", 0, status);
    ADD_DATACHUNK(worddata, " ", 0, status);
    ADD_DATACHUNK(worddata, "alpha\\u00adbeta\\u00adgamma", T_LETTER, status);
    ADD_DATACHUNK(worddata, " ", 0, status);
    ADD_DATACHUNK(worddata, "\\u092f\\u0939", T_LETTER, status);
    ADD_DATACHUNK(worddata, " ", 0, status);
    ADD_DATACHUNK(worddata, "\\u0939\\u093f" halfNA "\\u0926\\u0940", T_LETTER, status);
    ADD_DATACHUNK(worddata, " ", 0, status);
    ADD_DATACHUNK(worddata, "\\u0939\\u0948", T_LETTER, status);
    //ADD_DATACHUNK(worddata, "\\u0964", 0);   //danda followed by a space "\u0964->danda: hindi phrase seperator"
    ADD_DATACHUNK(worddata, " ", 0, status);
    ADD_DATACHUNK(worddata, "\\u0905\\u093e\\u092a", T_LETTER, status);
    ADD_DATACHUNK(worddata, " ", 0, status);
    ADD_DATACHUNK(worddata, "\\u0938\\u093f\\u0916\\u094b\\u0917\\u0947", T_LETTER, status);
    ADD_DATACHUNK(worddata, "?", 0, status);
    ADD_DATACHUNK(worddata, " ", 0, status);
    ADD_DATACHUNK(worddata, "\\u0968\\u0966.\\u0969\\u096f", T_NUMBER, status);            //hindi numbers
    ADD_DATACHUNK(worddata, " ", 0, status);
    ADD_DATACHUNK(worddata, "\\u0967\\u0966\\u0966.\\u0966\\u0966%", T_NUMBER, status);        //postnumeric
    ADD_DATACHUNK(worddata, " ", 0, status);
    ADD_DATACHUNK(worddata, "\\u20a8\\u0967,\\u0967\\u0966\\u0966.\\u0966\\u0966", T_NUMBER, status); //pre-number India currency symbol Rs->\\u20aD
    ADD_DATACHUNK(worddata, " ", 0, status);
    ADD_DATACHUNK(worddata, "\\u0905\\u092e\\u091c", T_LETTER, status);
    ADD_DATACHUNK(worddata, "\n", 0, status);
    ADD_DATACHUNK(worddata, halfSA  "\\u0935\\u0924\\u0902" deadTA "\\u0930", T_LETTER, status);
    ADD_DATACHUNK(worddata, "\r", 0, status);
    ADD_DATACHUNK(worddata, "It's", T_LETTER, status);
    ADD_DATACHUNK(worddata, " ", 0, status);
    ADD_DATACHUNK(worddata, "$30.10", T_NUMBER, status);
    ADD_DATACHUNK(worddata, " ", 0, status);
    ADD_DATACHUNK(worddata, "\\u00A2", 0, status); //cent sign
    ADD_DATACHUNK(worddata, "\\u00A3", 0, status); //pound sign
    ADD_DATACHUNK(worddata, "\\u00A4", 0, status); //currency sign
    ADD_DATACHUNK(worddata, "\\u00A5", 0, status); //yen sign
    ADD_DATACHUNK(worddata, "alpha\\u05f3beta\\u05f4gamma", T_LETTER, status);
    ADD_DATACHUNK(worddata, " ", 0, status);
    ADD_DATACHUNK(worddata, "Badges", T_LETTER, status);
    ADD_DATACHUNK(worddata, "?", 0, status);
    ADD_DATACHUNK(worddata, " ", 0, status);
    ADD_DATACHUNK(worddata, "BADGES", T_LETTER, status);
    ADD_DATACHUNK(worddata, "!", 0, status);
    ADD_DATACHUNK(worddata, "1000,233,456.000", T_NUMBER, status);
    ADD_DATACHUNK(worddata, " ", 0, status);
    ADD_DATACHUNK(worddata, "1,23.322%", T_NUMBER, status);
    ADD_DATACHUNK(worddata, " ", 0, status);
    ADD_DATACHUNK(worddata, "123.1222", T_NUMBER, status);
    ADD_DATACHUNK(worddata, " ", 0, status);
    ADD_DATACHUNK(worddata, "$123,000.20", T_NUMBER, status);
    ADD_DATACHUNK(worddata, " ", 0, status);
    ADD_DATACHUNK(worddata, "179.01%", T_NUMBER, status);
    ADD_DATACHUNK(worddata, "X", T_LETTER, status);
    ADD_DATACHUNK(worddata, " ", 0, status);
    ADD_DATACHUNK(worddata, "Now", T_LETTER, status);
    ADD_DATACHUNK(worddata, "\r", 0, status);
    ADD_DATACHUNK(worddata, "is", T_LETTER, status);
    ADD_DATACHUNK(worddata, "\n", 0, status);
    ADD_DATACHUNK(worddata, "the", T_LETTER, status);
    ADD_DATACHUNK(worddata, "\r\n", 0, status);
    ADD_DATACHUNK(worddata, "time", T_LETTER, status);
    ADD_DATACHUNK(worddata, " ", 0, status);
    ADD_DATACHUNK(worddata, "\\uc5f0\\ud569", T_LETTER, status);   // Hangul Syllables
    ADD_DATACHUNK(worddata, " ", 0, status);
    ADD_DATACHUNK(worddata, "\\uc7a5\\ub85c\\uad50\\ud68c", T_LETTER, status);  // Hangul
    ADD_DATACHUNK(worddata, " ", 0, status);
    // conjoining jamo...
    ADD_DATACHUNK(worddata, "\\u1109\\u1161\\u11bc\\u1112\\u1161\\u11bc", T_LETTER, status);
    ADD_DATACHUNK(worddata, " ", 0, status);
    ADD_DATACHUNK(worddata, "\\u1112\\u1161\\u11ab\\u110b\\u1175\\u11ab", T_LETTER, status);
    ADD_DATACHUNK(worddata, " ", 0, status);
    ADD_DATACHUNK(worddata, "Hello", T_LETTER, status);
    ADD_DATACHUNK(worddata, ",", 0, status);
    ADD_DATACHUNK(worddata, " ", 0, status);
    ADD_DATACHUNK(worddata, "how", T_LETTER, status);
    ADD_DATACHUNK(worddata, " ", 0, status);
    ADD_DATACHUNK(worddata, "are", T_LETTER, status);
    ADD_DATACHUNK(worddata, " ", 0, status);
    ADD_DATACHUNK(worddata, "you", T_LETTER, status);
    ADD_DATACHUNK(worddata, " ", 0, status);

    // Words containing non-BMP letters
    ADD_DATACHUNK(worddata, "abc\\U00010300", T_LETTER, status);
    ADD_DATACHUNK(worddata, " ", 0, status);
    ADD_DATACHUNK(worddata, "abc\\U0001044D", T_LETTER, status);
    ADD_DATACHUNK(worddata, " ", 0, status);
    ADD_DATACHUNK(worddata, "abc\\U0001D433", T_LETTER, status);  //MATHEMATICAL BOLD SMALL Z
    ADD_DATACHUNK(worddata, " ", 0, status);
    ADD_DATACHUNK(worddata, "abc\\U0001D7C9", T_LETTER, status);  //MATHEMATICAL SANS-SERIF BOLD ITALIC PI
    ADD_DATACHUNK(worddata, " ", 0, status);

    ADD_DATACHUNK(worddata, "abc", T_LETTER, status);  // same test outside of letter range.
    ADD_DATACHUNK(worddata, "\\U0001D800", 0, status);
    ADD_DATACHUNK(worddata, "def", T_LETTER, status);
    ADD_DATACHUNK(worddata, "\\U0001D3FF", 0, status);
    ADD_DATACHUNK(worddata, " ", 0, status);

    // Hiragana & Katakana stay together, but separates from each other and Latin.
    //   TODO:  Hira and Kata ranges from UnicodeSet differ slightly from
    //          what's in Unicode Scripts file.   Investigate.
    ADD_DATACHUNK(worddata, "abc", T_LETTER, status);
    ADD_DATACHUNK(worddata, "\\u3041\\u3094\\u309d\\u309e", T_H_OR_K, status);   // Hiragana
    ADD_DATACHUNK(worddata, "\\u30a1\\u30fd\\uff66\\uff9d", T_H_OR_K, status);  // Katakana
    ADD_DATACHUNK(worddata, "def", T_LETTER, status);

    if (U_FAILURE(status)){
        errln("FAIL : in BITestData construction");
        return;
    }

    generalIteratorTest(*wordIterDefault, worddata);

    delete wordIterDefault;
}



//--------------------------------------------------------------------
//tests default rules based sentence iteration
//--------------------------------------------------------------------
void RBBITest::TestDefaultRuleBasedSentenceIteration()
{
      logln((UnicodeString)"Testing the RBBI for sentence iteration using default rules");
     // RuleBasedBreakIterator *rbbi=(RuleBasedBreakIterator*)RuleBasedBreakIterator::createSentenceInstance();
      //fetch the rules used to create the above RuleBasedBreakIterator
    //  UnicodeString defaultRules=rbbi->getRules();
    //  RuleBasedBreakIterator sentIterDefault = new RuleBasedBreakIterator(defaultRules);
      UErrorCode status=U_ZERO_ERROR;
      RuleBasedBreakIterator* sentIterDefault=(RuleBasedBreakIterator*)RuleBasedBreakIterator::createSentenceInstance(Locale::getDefault(), status);
      if(U_FAILURE(status)){
          errln("FAIL : in construction");
          return;
      }
      BITestData sentdata(status);
      ADD_DATACHUNK(sentdata, NULL, 0, status);      // Mark start of data
      ADD_DATACHUNK(sentdata, "(This is it.) ", 0, status);
      ADD_DATACHUNK(sentdata, "Testing the sentence iterator. ", 0, status);
      ADD_DATACHUNK(sentdata, "\"This isn\'t it.\" ", 0, status);
      ADD_DATACHUNK(sentdata, "Hi! ", 0, status);
      //sentdata = new Vector();
      ADD_DATACHUNK(sentdata, "This is a simple sample sentence. ", 0, status);
      ADD_DATACHUNK(sentdata, "(This is it.) ", 0, status);
      ADD_DATACHUNK(sentdata, "This is a simple sample sentence. ", 0, status);
      ADD_DATACHUNK(sentdata, "\"This isn\'t it.\" ", 0, status);
      ADD_DATACHUNK(sentdata, "Hi! ", 0, status);
      ADD_DATACHUNK(sentdata, "This is a simple sample sentence. ", 0, status);
      ADD_DATACHUNK(sentdata, "It does not have to make any sense as you can see. ", 0, status);
      ADD_DATACHUNK(sentdata, "Nel mezzo del cammin di nostra vita, mi ritrovai in una selva oscura. ", 0, status);
      ADD_DATACHUNK(sentdata, "Che la dritta via aveo smarrita. ", 0, status);
      ADD_DATACHUNK(sentdata, "He said, that I said, that you said!! ", 0, status);

      ADD_DATACHUNK(sentdata, "Don't rock the boat.\\u2029", 0, status);    // Paragraph Separator

      ADD_DATACHUNK(sentdata, "Because I am the daddy, that is why. ", 0, status);
      ADD_DATACHUNK(sentdata, "Not on my time (el timo.)! ", 0, status);

      ADD_DATACHUNK(sentdata, "So what!!\\u2029", 0, status);              // Paragraph Separator

      ADD_DATACHUNK(sentdata, "\"But now,\" he said, \"I know!\" ", 0, status);
      ADD_DATACHUNK(sentdata, "Harris thumbed down several, including \"Away We Go\" (which became the huge success Oklahoma!). ", 0, status);
      ADD_DATACHUNK(sentdata, "One species, B. anthracis, is highly virulent.\n", 0, status);
      ADD_DATACHUNK(sentdata, "Wolf said about Sounder:\"Beautifully thought-out and directed.\" ", 0, status);
      ADD_DATACHUNK(sentdata, "Have you ever said, \"This is where\tI shall live\"? ", 0, status);
      ADD_DATACHUNK(sentdata, "He answered, \"You may not!\" ", 0, status);
      ADD_DATACHUNK(sentdata, "Another popular saying is: \"How do you do?\". ", 0, status);
      ADD_DATACHUNK(sentdata, "Yet another popular saying is: \'I\'m fine thanks.\' ", 0, status);
      ADD_DATACHUNK(sentdata, "What is the proper use of the abbreviation pp.? ", 0, status);
      ADD_DATACHUNK(sentdata, "Yes, I am definatelly 12\" tall!!", 0, status);
      // test for bug #4113835: \n and \r count as spaces, not as paragraph breaks
      ADD_DATACHUNK(sentdata, "Now\ris\nthe\r\ntime\n\rfor\r\rall\\u037e", 0, status);

    // test that it doesn't break sentences at the boundary between CJK
    // and other letters
      ADD_DATACHUNK(sentdata, "\\u5487\\u67ff\\ue591\\u5017\\u61b3\\u60a1\\u9510\\u8165:\"JAVA\\u821c"
           "\\u8165\\u7fc8\\u51ce\\u306d,\\u2494\\u56d8\\u4ec0\\u60b1\\u8560\\u51ba"
           "\\u611d\\u57b6\\u2510\\u5d46\".\\u2029", 0, status);
      ADD_DATACHUNK(sentdata, "\\u5487\\u67ff\\ue591\\u5017\\u61b3\\u60a1\\u9510\\u8165\\u9de8"
           "\\u97e4JAVA\\u821c\\u8165\\u7fc8\\u51ce\\u306d\\ue30b\\u2494\\u56d8\\u4ec0"
           "\\u60b1\\u8560\\u51ba\\u611d\\u57b6\\u2510\\u5d46\\u97e5\\u7751\\u3002", 0, status);
      ADD_DATACHUNK(sentdata, "\\u5487\\u67ff\\ue591\\u5017\\u61b3\\u60a1\\u9510\\u8165\\u9de8\\u97e4"
           "\\u6470\\u8790JAVA\\u821c\\u8165\\u7fc8\\u51ce\\u306d\\ue30b\\u2494\\u56d8"
           "\\u4ec0\\u60b1\\u8560\\u51ba\\u611d\\u57b6\\u2510\\u5d46\\u97e5\\u7751\\u2048", 0, status);
      ADD_DATACHUNK(sentdata, "He said, \"I can go there.\"\\u2029", 0, status);

      // Treat fullwidth variants of .!? the same as their
      // normal counterparts
#if 0   // Not according to TR29.  TODO:  what is the right thing for these chars?
      ADD_DATACHUNK(sentdata, "I know I'm right\\uff0e ", 0, status);
      ADD_DATACHUNK(sentdata, "Right\\uff1f ", 0, status);
      ADD_DATACHUNK(sentdata, "Right\\uff01 ", 0, status);
#endif

      // Don't break sentences at boundary between CJK and digits
      ADD_DATACHUNK(sentdata, "\\u5487\\u67ff\\ue591\\u5017\\u61b3\\u60a1\\u9510\\u8165\\u9de8"
                   "\\u97e48888\\u821c\\u8165\\u7fc8\\u51ce\\u306d\\ue30b\\u2494\\u56d8\\u4ec0"
                   "\\u60b1\\u8560\\u51ba\\u611d\\u57b6\\u2510\\u5d46\\u97e5\\u7751\\u3001", 0, status);

      // Break sentence between a sentence terminator and
      // opening punctuation
      ADD_DATACHUNK(sentdata, "How do you do?", 0, status);
      ADD_DATACHUNK(sentdata, "(fine).", 0, status);
      //sentence breaks for hindi which used Devanagari script
      //make sure there is sentence break after ?,danda(hindi phrase separator),fullstop followed by space and no break after \n \r
      ADD_DATACHUNK(sentdata,  "\\u0928\\u092e" halfSA
                                    "\\u0924\\u0947 "
                                    "\\u0930\\u092e\\u0947\\u0936, "
                                    "\\u0905\\u093e\\u092a"
                                    "\\u0915\\u0948\\u0938\\u0947 "
                                    "\\u0939\\u0948?", 0, status);
      ADD_DATACHUNK(sentdata,
              "\\u092e\\u0948 \\u0905"  halfCHA "\\u091b\\u093e \\u0939\\u0942\\u0901\\u0964 ", 0, status);
      ADD_DATACHUNK(sentdata, "\\u0905\\u093e\\u092a\r\n \\u0915\\u0948\\u0938\\u0947 \\u0939\\u0948?", 0, status);
      ADD_DATACHUNK(sentdata, "\\u0935\\u0939 " halfKA "\\u092f\\u093e\n \\u0939\\u0948?", 0, status);
      ADD_DATACHUNK(sentdata, "\\u092f\\u0939 \\u0905\\u093e\\u092e \\u0939\\u0948. ", 0, status);
      ADD_DATACHUNK(sentdata, "\\u092f\\u0939 means \"this\". ", 0, status);
      ADD_DATACHUNK(sentdata, "\"\\u092a\\u095d\\u093e\\u0908\" meaning \"education\" or \"studies\". ", 0, status);
      ADD_DATACHUNK(sentdata, "\\u0905\\u093e\\u091c("  halfSA "\\u0935\\u0924\\u0902"  deadTA "\\u0930 "
                                   "\\u0926\\u093f\\u0935\\u093e\\u0938) "
                                   "\\u0939\\u0948\\u0964 ", 0, status);
      ADD_DATACHUNK(sentdata, "Let's end here. ", 0, status);

      generalIteratorTest(*sentIterDefault, sentdata);

      delete sentIterDefault;
}


//--------------------------------------------------------------------
//tests default rules based line iteration
//--------------------------------------------------------------------
void RBBITest::TestDefaultRuleBasedLineIteration()
{
    UErrorCode status= U_ZERO_ERROR;
    RuleBasedBreakIterator* lineIterDefault=(RuleBasedBreakIterator*)RuleBasedBreakIterator::createLineInstance(Locale::getDefault(), status);
    if(U_FAILURE(status)){
          errln("FAIL : in construction");
          return;
      }
      BITestData linedata(status);
      ADD_DATACHUNK(linedata, NULL, 0, status);           // Break at start of data
      ADD_DATACHUNK(linedata, "Multi-", 0, status);
      ADD_DATACHUNK(linedata, "Level ", 0, status);
      ADD_DATACHUNK(linedata, "example ", 0, status);
      ADD_DATACHUNK(linedata, "of ", 0, status);
      ADD_DATACHUNK(linedata, "a ", 0, status);
      ADD_DATACHUNK(linedata, "semi-", 0, status);
      ADD_DATACHUNK(linedata, "idiotic ", 0, status);
      ADD_DATACHUNK(linedata, "non-", 0, status);
      ADD_DATACHUNK(linedata, "sensical ", 0, status);
      ADD_DATACHUNK(linedata, "(non-", 0, status);
      ADD_DATACHUNK(linedata, "important) ", 0, status);
      ADD_DATACHUNK(linedata, "sentence. ", 0, status);

      ADD_DATACHUNK(linedata, "Hi  ", 0, status);
      ADD_DATACHUNK(linedata, "Hello ", 0, status);
      ADD_DATACHUNK(linedata, "How\n", 0, status);
      ADD_DATACHUNK(linedata, "are\r", 0, status);
      ADD_DATACHUNK(linedata, "you\\u2028", 0, status);    // Line Separator
      ADD_DATACHUNK(linedata, "fine.\t", 0, status);
      ADD_DATACHUNK(linedata, "good.  ", 0, status);

      ADD_DATACHUNK(linedata, "Now\r", 0, status);
      ADD_DATACHUNK(linedata, "is\n", 0, status);
      ADD_DATACHUNK(linedata, "the\r\n", 0, status);
      ADD_DATACHUNK(linedata, "time\n", 0, status);
      ADD_DATACHUNK(linedata, "\r", 0, status);
      ADD_DATACHUNK(linedata, "for\r", 0, status);
      ADD_DATACHUNK(linedata, "\r", 0, status);
      ADD_DATACHUNK(linedata, "all", 0, status);

    // to test for bug #4068133
      ADD_DATACHUNK(linedata, "\\u96f6", 0, status);
      ADD_DATACHUNK(linedata, "\\u4e00\\u3002", 0, status);
      ADD_DATACHUNK(linedata, "\\u4e8c\\u3001", 0, status);
      ADD_DATACHUNK(linedata, "\\u4e09\\u3002\\u3001", 0, status);
      ADD_DATACHUNK(linedata, "\\u56db\\u3001\\u3002\\u3001", 0, status);
      ADD_DATACHUNK(linedata, "\\u4e94,", 0, status);
      ADD_DATACHUNK(linedata, "\\u516d.", 0, status);
      ADD_DATACHUNK(linedata, "\\u4e03.\\u3001,\\u3002", 0, status);
      ADD_DATACHUNK(linedata, "\\u516b", 0, status);

    // to test for bug #4086052
      ADD_DATACHUNK(linedata, "foo\\u00a0bar ", 0, status);
//          ADD_DATACHUNK(linedata, "foo\\ufeffbar", 0);

    // to test for bug #4097920
      ADD_DATACHUNK(linedata, "dog,", 0, status);
      ADD_DATACHUNK(linedata, "cat,", 0, status);
      ADD_DATACHUNK(linedata, "mouse ", 0, status);
      ADD_DATACHUNK(linedata, "(one)", 0, status);
      ADD_DATACHUNK(linedata, "(two)\n", 0, status);

    // to test for bug #4035266
      ADD_DATACHUNK(linedata, "The ", 0, status);
      ADD_DATACHUNK(linedata, "balance ", 0, status);
      ADD_DATACHUNK(linedata, "is ", 0, status);
      ADD_DATACHUNK(linedata, "$-23,456.78, ", 0, status);
      ADD_DATACHUNK(linedata, "not ", 0, status);
      // ADD_DATACHUNK(linedata, "-$32,456.78!\n", 0);    // Doesn't break this way according to TR29
      ADD_DATACHUNK(linedata, "-", 0, status);
      ADD_DATACHUNK(linedata, "$32,456.78!\n", 0, status);

    // to test for bug #4098467
    // What follows is a string of Korean characters (I found it in the Yellow Pages
    // ad for the Korean Presbyterian Church of San Francisco, and I hope I transcribed
    // it correctly), first as precomposed syllables, and then as conjoining jamo.
    // Both sequences should be semantically identical and break the same way.
    // precomposed syllables...

      // By TR14, precomposed Hangul syllables should not be grouped together.
#if 0
      ADD_DATACHUNK(linedata, "\\uc0c1\\ud56d ", 0, status);
      ADD_DATACHUNK(linedata, "\\ud55c\\uc778 ", 0, status);
      ADD_DATACHUNK(linedata, "\\uc5f0\\ud569 ", 0, status);
      ADD_DATACHUNK(linedata, "\\uc7a5\\ub85c\\uad50\\ud68c ", 0, status);
#endif
      ADD_DATACHUNK(linedata, "\\uc0c1", 0, status);
      ADD_DATACHUNK(linedata, "\\ud56d ", 0, status);
      ADD_DATACHUNK(linedata, "\\ud55c", 0, status);
      ADD_DATACHUNK(linedata, "\\uc778 ", 0, status);
      ADD_DATACHUNK(linedata, "\\uc5f0", 0, status);
      ADD_DATACHUNK(linedata, "\\ud569 ", 0, status);
      ADD_DATACHUNK(linedata, "\\uc7a5", 0, status);
      ADD_DATACHUNK(linedata, "\\ub85c", 0, status);
      ADD_DATACHUNK(linedata, "\\uad50", 0, status);
      ADD_DATACHUNK(linedata, "\\ud68c ", 0, status);

    // conjoining jamo...
      ADD_DATACHUNK(linedata, "\\u1109\\u1161\\u11bc", 0, status);
      ADD_DATACHUNK(linedata, "\\u1112\\u1161\\u11bc ", 0, status);
      ADD_DATACHUNK(linedata, "\\u1112\\u1161\\u11ab", 0, status);
      ADD_DATACHUNK(linedata, "\\u110b\\u1175\\u11ab ", 0, status);
      ADD_DATACHUNK(linedata, "\\u110b\\u1167\\u11ab", 0, status);
      ADD_DATACHUNK(linedata, "\\u1112\\u1161\\u11b8 ", 0, status);
      ADD_DATACHUNK(linedata, "\\u110c\\u1161\\u11bc", 0, status);
      ADD_DATACHUNK(linedata, "\\u1105\\u1169", 0, status);
      ADD_DATACHUNK(linedata, "\\u1100\\u116d", 0, status);
      ADD_DATACHUNK(linedata, "\\u1112\\u116c", 0, status);

    // to test for bug #4117554: Fullwidth .!? should be treated as postJwrd
      ADD_DATACHUNK(linedata, "\\u4e01\\uff0e", 0, status);
      ADD_DATACHUNK(linedata, "\\u4e02\\uff01", 0, status);
      ADD_DATACHUNK(linedata, "\\u4e03\\uff1f", 0, status);

    // Surrogate line break tests.
      ADD_DATACHUNK(linedata, "\\u4e01", 0, status);          // BMP ideograph
      ADD_DATACHUNK(linedata, "\\ud840\\udc01", 0, status);   // Extended ideograph
      ADD_DATACHUNK(linedata, "\\u4e02", 0, status);          // BMP Ideograph
      ADD_DATACHUNK(linedata, "abc", 0, status);              // latin
      ADD_DATACHUNK(linedata, "\\ue000", 0, status);          // PUA
      ADD_DATACHUNK(linedata, "\\udb80\\udc01", 0, status);   // Extended PUA.  Treated as ideograph.

      // Regression for bug 836
      ADD_DATACHUNK(linedata, "AAA", 0, status);
      ADD_DATACHUNK(linedata, "(AAA ", 0, status);

    generalIteratorTest(*lineIterDefault, linedata);

    delete lineIterDefault;
}


//--------------------------------------------------------------------
//Testing the BreakIterator for devanagari script
//--------------------------------------------------------------------

#define deadRA   "\\u0930\\u094d"         /*deadform RA = devanagari RA + virama*/
#define deadPHA  "\\u092b\\u094d"         /*deadform PHA = devanagari PHA + virama*/
#define deadTTHA "\\u0920\\u094d"
#define deadPA   "\\u092a\\u094d"
#define deadSA   "\\u0938\\u094d"
#define visarga  "\\u0903"                /*devanagari visarga looks like a english colon*/

void RBBITest::TestHindiCharacterBreak()
{
    UErrorCode status= U_ZERO_ERROR;
    BITestData hindicharData(status);
    ADD_DATACHUNK(hindicharData, NULL, 0, status);           // Break at start of data
    //devanagari characters for Hindi support
    ADD_DATACHUNK(hindicharData, "\\u0906", 0, status);                    //devanagari AA

    //hindi character break should make sure that it
    // doesn't break in-between a vowelsign and a chandrabindu
    // TODO:  Rules need some fixing.  As currently written, they'll correctly recognize this combination
    //        as part of a legit character, but not standalone.

    // ADD_DATACHUNK(hindicharData, "\\u093e\\u0901", 0);         //devanagari vowelsign AA+ chandrabindu
    ADD_DATACHUNK(hindicharData, "\\u0906\\u0901", 0, status);            // Devanagari AA + chandrabindu
    ADD_DATACHUNK(hindicharData, "\\u0915\\u093e\\u0901", 0, status);     // Devanagari KA + AA vowelsign + chandrabindu


    ADD_DATACHUNK(hindicharData, "\\u0916\\u0947", 0, status);              //devanagari KHA+vowelsign E
    ADD_DATACHUNK(hindicharData, "\\u0938\\u0941\\u0902", 0, status);        //devanagari SA+vowelsign U + anusvara(bindu)
    ADD_DATACHUNK(hindicharData, "\\u0926", 0, status);                    //devanagari consonant DA
    ADD_DATACHUNK(hindicharData, "\\u0930", 0, status);                    //devanagari consonant RA
    ADD_DATACHUNK(hindicharData, "\\u0939\\u094c", 0, status);              //devanagari consonant HA+dependent vowel sign AI
    ADD_DATACHUNK(hindicharData, "\\u0964", 0, status);                    //devanagari danda
    ADD_DATACHUNK(hindicharData, "\\u0950", 0, status);                    //devanagari OM
    ADD_DATACHUNK(hindicharData, "\\u0915\\u0943", 0, status);              //devanagari KA+dependent vowel RI->KRI

    //dependent half-forms
    ADD_DATACHUNK(hindicharData, halfSA "\\u0924", 0, status);             //halfSA+base consonant TA->STA
    ADD_DATACHUNK(hindicharData, halfSA "\\u0925", 0, status);             //halfSA+base consonant THA->STHA
    ADD_DATACHUNK(hindicharData, halfSA "\\u092e", 0, status);             //halfSA+base consonant MA->SMA
    ADD_DATACHUNK(hindicharData, halfCHA "\\u091b", 0, status);            //halfCHA+base consonant CHHA->CHHHA
    ADD_DATACHUNK(hindicharData, halfNA "\\u0917", 0, status);             //halfNA+base consonant GA->NGA
    ADD_DATACHUNK(hindicharData, "\\u092a\\u094d\\u200d\\u092f", 0, status);   //halfPA(PA+virama+zerowidthjoiner+base consonant YA->PYA


    //consonant RA rules ----------
    //if the dead consonant RA precedes either a consonant or an independent vowel,
    //then it is replaced by its superscript non-spacing mark
    ADD_DATACHUNK(hindicharData, deadRA  "\\u0915", 0, status);             //deadRA+devanagari consonant KA->KA+superRA
    ADD_DATACHUNK(hindicharData, deadRA  "\\u0923", 0, status);             //deadRA+devanagari consonant NNA->NNA+superRA
    ADD_DATACHUNK(hindicharData, deadRA  "\\u0917", 0, status);             //deadRA+devanagari consonant GA->GA+superRA
    //  ADD_DATACHUNK(hindicharData, deadRA+ "\\u0960", 0);           //deadRA+devanagari cosonant RRI->RRI+superRA

    //if any dead consonant(other than dead RA)precedes the consonant RA, then
    //it is replaced with its nominal forma nd RA is replaced by the subscript non-spacing mark.
    ADD_DATACHUNK(hindicharData, deadPHA  "\\u0930", 0, status);            //deadPHA+devanagari consonant RA->PHA+subRA
    ADD_DATACHUNK(hindicharData, deadPA  "\\u0930", 0, status);             //deadPA+devanagari consonant RA->PA+subRA
    ADD_DATACHUNK(hindicharData, deadTTHA  "\\u0930", 0, status);           //deadTTHA+devanagari consonant RA->TTHA+subRA
    ADD_DATACHUNK(hindicharData, deadTA  "\\u0930", 0, status);             //deadTA+RA->TRA
    ADD_DATACHUNK(hindicharData, "\\u0936\\u094d\\u0930", 0, status);         //deadSHA(SHA+virama)+RA->SHRA

    //conjuct ligatures
    ADD_DATACHUNK(hindicharData, "\\u0915\\u094d\\u0937", 0, status);         //deadKA(KA+virama) followed by SSHA wraps up into a single character KSSHA
    ADD_DATACHUNK(hindicharData, deadTA "\\u0924", 0, status);              //deadTA+TA wraps up into glyph TTHA
    ADD_DATACHUNK(hindicharData, "\\u0926\\u094d\\u0935", 0, status);         //deadDA(DA+virama)+VA wraps up into DVA
    ADD_DATACHUNK(hindicharData, "\\u091c\\u094d\\u091e", 0, status);         //deadJA(JA+virama)+NYA wraps up into JNYA

    RuleBasedBreakIterator *e=(RuleBasedBreakIterator*)RuleBasedBreakIterator::createCharacterInstance(Locale::getDefault(), status);
    if(U_FAILURE(status)){
        errln("FAIL : in construction");
        return;
    }
    generalIteratorTest(*e, hindicharData);
    delete e;
}

void RBBITest::TestHindiWordBreak()
{
    UErrorCode status= U_ZERO_ERROR;
    BITestData hindiWordData(status);

    //hindi
    ADD_DATACHUNK(hindiWordData, NULL, 0, status);           // Break at start of data
    ADD_DATACHUNK(hindiWordData, "\\u0917\\u092a\\u00ad\\u0936\\u092a", 200, status);
    ADD_DATACHUNK(hindiWordData, "!", 0, status);
    ADD_DATACHUNK(hindiWordData, "\\u092f\\u0939", 200, status);
    ADD_DATACHUNK(hindiWordData, " ", 0, status);
    ADD_DATACHUNK(hindiWordData, "\\u0939\\u093f" halfNA "\\u0926\\u0940", 200, status);
    ADD_DATACHUNK(hindiWordData, " ", 0, status);
    ADD_DATACHUNK(hindiWordData, "\\u0939\\u0948", 200, status);
    //danda is similar to full stop. danda is a hindi phrase seperator
    //Make sure it breaks before danda and after danda when it is followed by a space
    //ADD_DATACHUNK(hindiWordData, "\\u0964", 0);   //fails here doesn't break at danda
    ADD_DATACHUNK(hindiWordData, " ", 0, status);
    ADD_DATACHUNK(hindiWordData, "\\u0905\\u093e\\u092a", 200, status);
    ADD_DATACHUNK(hindiWordData, " ", 0, status);
    ADD_DATACHUNK(hindiWordData, "\\u0938\\u093f\\u0916\\u094b\\u0917\\u0947", 200, status);
    ADD_DATACHUNK(hindiWordData, "?", 0, status);
    ADD_DATACHUNK(hindiWordData, "\n", 0, status);
    ADD_DATACHUNK(hindiWordData, ":", 0, status);
    ADD_DATACHUNK(hindiWordData, deadPA "\\u0930\\u093e\\u092f" visarga, 200, status);    //no break before visarga
    ADD_DATACHUNK(hindiWordData, " ", 0, status);
    ADD_DATACHUNK(hindiWordData, "\\u0935" deadRA "\\u0937\\u093e", 200, status);
    ADD_DATACHUNK(hindiWordData, "\r\n", 0, status);
    ADD_DATACHUNK(hindiWordData, deadPA  "\\u0930\\u0915\\u093e\\u0936", 200, status);     //deadPA+RA+KA+vowel AA+SHA -> prakash
    ADD_DATACHUNK(hindiWordData, ",", 0, status);
    ADD_DATACHUNK(hindiWordData, "\\u0924\\u0941\\u092e\\u093e\\u0930\\u094b", 200, status);
    ADD_DATACHUNK(hindiWordData, " ", 0, status);
    ADD_DATACHUNK(hindiWordData, "\\u092e\\u093f" deadTA "\\u0930", 200, status);       //MA+vowel I+ deadTA + RA
    ADD_DATACHUNK(hindiWordData, " ", 0, status);
    ADD_DATACHUNK(hindiWordData, "\\u0915\\u093e", 200, status);
    ADD_DATACHUNK(hindiWordData, " ", 0, status);
    ADD_DATACHUNK(hindiWordData, "\\u092a" deadTA "\\u0930", 200, status);            //PA + deadTA + RA
    ADD_DATACHUNK(hindiWordData, " ", 0, status);
    ADD_DATACHUNK(hindiWordData, "\\u092a\\u095d\\u094b", 200, status);
    // ADD_DATACHUNK(hindiWordData, "\\u0964", 0); //fails here doesn't break at danda
    ADD_DATACHUNK(hindiWordData, " ", 0, status);
    ADD_DATACHUNK(hindiWordData, deadSA deadTA "\\u0930\\u093f", 200, status);       //deadSA+deadTA+RA+vowel I->sthri
    ADD_DATACHUNK(hindiWordData, ".", 0, status);
    ADD_DATACHUNK(hindiWordData, " ", 0, status);
    ADD_DATACHUNK(hindiWordData, "\\u0968\\u0966.\\u0969\\u096f", 100, status);            //hindi numbers
    ADD_DATACHUNK(hindiWordData, " ", 0, status);
    ADD_DATACHUNK(hindiWordData, "\\u0967\\u0966\\u0966.\\u0966\\u0966%", 100, status);     //postnumeric
    ADD_DATACHUNK(hindiWordData, " ", 0, status);
    ADD_DATACHUNK(hindiWordData, "\\u20a8\\u0967,\\u0967\\u0966\\u0966.\\u0966\\u0966", 100, status); //pre-number India currency symbol Rs.\\u20aD
    ADD_DATACHUNK(hindiWordData, " ", 0, status);
    ADD_DATACHUNK(hindiWordData, "\\u0905\\u092e\\u091c", 200, status);
    ADD_DATACHUNK(hindiWordData, "\n", 0, status);
    ADD_DATACHUNK(hindiWordData, halfSA "\\u0935\\u0924\\u0902" deadTA "\\u0930", 200, status);
    ADD_DATACHUNK(hindiWordData, "\r", 0, status);

    RuleBasedBreakIterator *e=(RuleBasedBreakIterator*)RuleBasedBreakIterator::createWordInstance(Locale::getDefault(), status);
    if(U_FAILURE(status)){
        errln("FAIL : in construction");
        return;
    }
    generalIteratorTest(*e, hindiWordData);
    delete e;
}


void RBBITest::TestTitleBreak()
{
    UErrorCode status= U_ZERO_ERROR;
    RuleBasedBreakIterator* titleI=(RuleBasedBreakIterator*)RuleBasedBreakIterator::createTitleInstance(Locale::getDefault(), status);
    if(U_FAILURE(status)){
          errln("FAIL : in construction");
          return;
    }

    BITestData titleData(status);
    ADD_DATACHUNK(titleData, NULL, 0, status);           // Break at start of data
    ADD_DATACHUNK(titleData, "   ", 0, status);
    ADD_DATACHUNK(titleData, "This ", 0, status);
    ADD_DATACHUNK(titleData, "is ", 0, status);
    ADD_DATACHUNK(titleData, "a ", 0, status);
    ADD_DATACHUNK(titleData, "simple ", 0, status);
    ADD_DATACHUNK(titleData, "sample ", 0, status);
    ADD_DATACHUNK(titleData, "sentence. ", 0, status);
    ADD_DATACHUNK(titleData, "This ", 0, status);

    generalIteratorTest(*titleI, titleData);
    delete titleI;
}


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

/*
//Bug: if there is no word break before and after danda when it is followed by a space
void RBBITest::TestDanda()
{
      Vector *hindiWordData = new Vector();
      //hindi
      ADD_DATACHUNK(hindiWordData, CharsToUnicodeString("\\u092f\\u0939"), 0, status);
      ADD_DATACHUNK(hindiWordData, " ", 0, status);
      //Danda is similar to full stop, danda is a hindi phrase seperator.
      //Make sure there is a word break before and after danda when it is followed by a space
     //following fail----
      ADD_DATACHUNK(hindiWordData, CharsToUnicodeString("\\u0939\\u0948"), 0, status);
    //  ADD_DATACHUNK(hindiWordData, CharsToUnicodeString("\\u0964"), 0);         // devanagari danda
      ADD_DATACHUNK(hindiWordData, " ", 0, status);
      ADD_DATACHUNK(hindiWordData, CharsToUnicodeString("\\u092f\\u0939"), 0, status);
  //    ADD_DATACHUNK(hindiWordData, CharsToUnicodeString("\\u0965"), 0);         //devanagari double danda
      ADD_DATACHUNK(hindiWordData, " ", 0, status);

      RuleBasedBreakIterator* e=(RuleBasedBreakIterator*)RuleBasedBreakIterator::createWordInstance();
      generalIteratorTest(*e, hindiWordData);
      delete e;
      delete hindiWordData;
}

//Make sure the character wrapping is done correctly
void RBBITest::TestHindiCharacterWrapping()
{
      Vector *hindicharData = new Vector();
      //if the dead consonant RA precedes either a consonant or an independent vowel,
      //then it is replaced by its superscript non-spacing mark
      ADD_DATACHUNK(hindicharData, deadRA+ CharsToUnicodeString("\\u0917"), 0, status); //deadRA+devanagari consonant GA->GA+superRA
      //following fail----
     // ADD_DATACHUNK(hindicharData, deadRA+ CharsToUnicodeString("\\u0960"), 0);   //deadRA+devanagari RRI->RRI+superRA

      RuleBasedBreakIterator* e=(RuleBasedBreakIterator*)RuleBasedBreakIterator::createCharacterInstance();
      generalIteratorTest(*e, hindicharData);
      delete e;
      delete hindicharData;

}*/




//----------------------------------------------------------------------------------
//adds rules for telugu support and tests the behaviour of chracterIterator of RBBI
//----------------------------------------------------------------------------------
/*void RBBITest::TestTeluguRuleBasedCharacterIteration()
{
     logln((UnicodeString)"Testing the RBBI by adding rules for Telugu(Indian Language) Support");
     //get the default rules
     RuleBasedBreakIterator *rb= (RuleBasedBreakIterator*)BreakIterator::createCharacterInstance();
     //additional rules for Telugu(Indian Language) support
     UnicodeString crules1 = rb->getRules()                                                 +  //default rules +
                      "<telvirama>=[\\u0c4d];"                                               +  //telugu virama
                      "<telVowelSign>=[\\u0c3e-\\u0c44\\u0c46\\u0c47\\u0c48\\u0c4a\\u0c4b\\u0c4c];" +  //telugu dependent vowel signs
                      "<telConsonant>=[\\u0c15-\\u0c28\\u0c2a-\\u0c33\\u0c35-\\u0c39];"           +  //telugu consonants
                      "<telCharEnd>=[\\u0c02\\u0c03\\u0c55\\u0c56];"                            +  //to create half forms and dead forms
                      "<telConjunct>=({<telConsonant><telvirama>{<zwj>}}<telConsonant>);"   +
                      "<telConjunct>{<telVowelSign>}{<telCharEnd>};";
      RuleBasedBreakIterator charIter=null;
      charIter   = new RuleBasedBreakIterator(crules1);

      Vector *chardata = new Vector();
      //behaviour of telugu characters from specified rules
      ADD_DATACHUNK(chardata, CharsToUnicodeString("\\u0c15"), 0, status);                    //telugu consonant KA
      ADD_DATACHUNK(chardata, CharsToUnicodeString("\\u0c30\\u0c47"), 0, status);              //telugu consonant RA+telugu dependent vowel EE
      ADD_DATACHUNK(chardata, CharsToUnicodeString("\\u0c1b\\u0c3e"), 0, status);              //telugu consonant CHA+telegu depenednt vowel AA
      ADD_DATACHUNK(chardata, CharsToUnicodeString("\\u0c17\\u0c48"), 0, status);              //telegu consonant GA+teleugu dependent vowel AI
      ADD_DATACHUNK(chardata, CharsToUnicodeString("\\u0c17\\u0c46\\u0c56"), 0, status);        //telugu consonant GA+telugu dependent vowel sign E+telugu AI length mark
      ADD_DATACHUNK(chardata, CharsToUnicodeString("\\u0c28\\u0c4d\\u200d\\u0c28"), 0, status);  //telugu consonant NA+telugu virama+zwj=>halfNA+NA->NNA(dependent half-form)
      ADD_DATACHUNK(chardata, CharsToUnicodeString("\\u0c17\\u0c4d\\u0c30"), 0, status);        //GA+deadRA(RA+telvirama)->GA+subRA->GRA
      ADD_DATACHUNK(chardata, CharsToUnicodeString("\\u0c66"), 0, status);                    //telugu digit
      ADD_DATACHUNK(chardata, CharsToUnicodeString("\\u0c37\\u0c4d\\u0c15"), 0, status);        //deadSSA(SSA+telvirama)+KA+subSSA->KSHA
      //behaviour of other characters from default rules
      ADD_DATACHUNK(chardata, "h", 0, status);
      ADD_DATACHUNK(chardata, CharsToUnicodeString("A\\u0302"), 0, status);                   // circumflexA
      ADD_DATACHUNK(chardata, CharsToUnicodeString("i\\u0301"), 0, status);                   // acuteBelowI
      ADD_DATACHUNK(chardata, CharsToUnicodeString("\\u1109\\u1161\\u11bc"), 0, status);
      ADD_DATACHUNK(chardata, CharsToUnicodeString("\\u1112\\u1161\\u11bc"), 0, status);
      ADD_DATACHUNK(chardata, "\n", 0, status);
      ADD_DATACHUNK(chardata, "\r\n", 0, status);

      generalIteratorTest(charIter, chardata);

      delete charIter;
      delete charData;
      delete rb;
}

//--------------------------------------------------------------------
//tests the behaviour of character iteration of RBBI with custom rules
//--------------------------------------------------------------------

void RBBITest::TestCustomRuleBasedCharacterIteration()
{
      logln((UnicodeString)"Testing the RBBI by using custom rules for character iteration");

      UnicodeString crules2="<ignore>=[e];"                         + //ignore the character "e"
                     ".;"                                           +
                     "<devVowelSign>=[\\u093e-\\u094c\\u0962\\u0963];"  +  //devanagiri vowel = \\u093e tp \\u094c and \\u0962.\\u0963
                     "<devConsonant>=[\\u0915-\\u0939];"              +  //devanagiri consonant = \\u0915 to \\u0939
                     "<devConsonant>{<devVowelSign>};" ;               //break at all places except the  following
                                                                       //devanagiri consonants+ devanagiri vowelsign

      RuleBasedCharacterIterator charIterCustom   = new RuleBasedBreakIterator(crules2);
      Vector *chardata = new Vector();
      ADD_DATACHUNK(chardata, "He", 0, status);              //ignores 'e'
      ADD_DATACHUNK(chardata, "l", 0, status);
      ADD_DATACHUNK(chardata, "l", 0, status);
      ADD_DATACHUNK(chardata, "oe", 0, status);              //ignores 'e' hence wraps it into 'o' instead of wrapping with
      ADD_DATACHUNK(chardata, CharsToUnicodeString("\\u0301"), 0, status);          //'\\u0301' to form 'acuteE '
      ADD_DATACHUNK(chardata, "&e", 0, status);              //ignores 'e' hence wraps it into '&' instead of wrapping with
      ADD_DATACHUNK(chardata, CharsToUnicodeString("\\u0303"), 0, status);          //'\\u0303 to form 'tildaE'
      //devanagiri characters
      ADD_DATACHUNK(chardata, CharsToUnicodeString("\\u0906"), 0, status);          //devanagiri AA
      ADD_DATACHUNK(chardata, CharsToUnicodeString("\\u093e"), 0, status);          //devanagiri vowelsign AA:--breaks at \\u0901 which is devanagiri
      ADD_DATACHUNK(chardata, CharsToUnicodeString("\\u0901"), 0, status);          //chandra bindhu since it is not mentioned in the rules
      ADD_DATACHUNK(chardata, CharsToUnicodeString("\\u0916\\u0947"), 0, status);    //devanagiri KHA+vowelsign E
      ADD_DATACHUNK(chardata, CharsToUnicodeString("\\u0938\\u0941"), 0, status);    //devanagiri SA+vowelsign U : - breaks at
      ADD_DATACHUNK(chardata, CharsToUnicodeString("\\u0902"), 0, status);          //\\u0902 devanagiri anusvara since it is not mentioned in the rules
      ADD_DATACHUNK(chardata, CharsToUnicodeString("\\u0926"), 0, status);          //devanagiri consonant DA
      ADD_DATACHUNK(chardata, CharsToUnicodeString("\\u0930"), 0, status);          //devanagiri consonant RA
      ADD_DATACHUNK(chardata, CharsToUnicodeString("\\u0939\\u094c"), 0, status);    //devanagiri HA+vowel sign AI
      ADD_DATACHUNK(chardata, CharsToUnicodeString("\\u0964"), 0, status);          //devanagiri danda
      // devanagiri chracters end
      ADD_DATACHUNK(chardata, "A", 0, status);               //breaks in between since it is not mentioned in the rules
      ADD_DATACHUNK(chardata, CharsToUnicodeString("\\u0302"), 0, status);          // circumflexA
      ADD_DATACHUNK(chardata, "i", 0, status);               //breaks in between since not mentioned in the rules
      ADD_DATACHUNK(chardata, CharsToUnicodeString("\\u0301"), 0, status);          // acuteBelowI
      //Rules don't support conjoining jamo->->..
      ADD_DATACHUNK(chardata, CharsToUnicodeString("\\u1109"), 0, status);          //break at every character since rules
      ADD_DATACHUNK(chardata, CharsToUnicodeString("\\u1161"), 0, status);          //don't support conjoining jamo
      ADD_DATACHUNK(chardata, CharsToUnicodeString("\\u11bc"), 0, status);
      ADD_DATACHUNK(chardata, CharsToUnicodeString("\\u1112"), 0, status);
      ADD_DATACHUNK(chardata, CharsToUnicodeString("\\u1161"), 0, status);
      ADD_DATACHUNK(chardata, CharsToUnicodeString("\\u11bc"), 0, status);
      ADD_DATACHUNK(chardata, "\n", 0, status);
      ADD_DATACHUNK(chardata, "\r", 0, status);             //doesn't keep CRLGF together since rules do not mention it
      ADD_DATACHUNK(chardata, "\n", 0, status);
      ADD_DATACHUNK(chardata, "S", 0, status);              //graveS
      ADD_DATACHUNK(chardata, CharsToUnicodeString("\\u0300"), 0, status);         //breaks in between since it is not mentioned in the rules
      ADD_DATACHUNK(chardata, "i", 0, status);              //acuteBelowI
      ADD_DATACHUNK(chardata, CharsToUnicodeString("\\u0301"), 0, status);         //breaks in between since it is not mentioned in the rules
      generalIteratorTest(charIterCustom, chardata);

      delete charIterCustom;
      delete chardata;
}*/
/*//--------------------------------------------------------------------
//tests custom rules based word iteration
//--------------------------------------------------------------------
void RBBITest::TestCustomRuleBasedWordIteration(){
      logln("(UnicodeString)Testing the RBBI by using custom rules for word iteration");
      UnicodeString wrules1="<ignore>=[:Mn::Me::Cf:];"                  + //ignore non-spacing marks, enclosing marks, and format characters,
                      "<danda>=[\\u0964\\u0965];"                       + //Hindi Phrase seperator
                      "<let>=[:L::Mc:];"                                + //uppercase(Lu), lowercase(Ll), titlecase(Lt), modifier(Lm) letters, Mc-combining space mark
                      "<mid-word>=[:Pd:\\\"\\\'\\.];"                   + //dashes, quotation, apostraphes, period
                      "<ls>=[\\n\\u000c\\u2028\\u2029];"                + //line separators:  LF, FF, PS, and LS
                      "<ws>=[:Zs:\\t];"                                 + //all space separators and the tab character
                      "<word>=((<let><let>*(<mid-word><let><let>*)*));" +
                      ".;"                                              + //break after every character, with the following exceptions
                      "{<word>};"                                       +
                      "<ws>*{\\r}{<ls>}{<danda>};" ;

      RuleBasedBreakIterator wordIterCustom   = new RuleBasedBreakIterator(wrules1);
      Vector *worddata = new Vector();
      ADD_DATACHUNK(worddata, "Write", 0, status);
      ADD_DATACHUNK(worddata, " ", 0, status);
      ADD_DATACHUNK(worddata, "wordrules", 0, status);
      ADD_DATACHUNK(worddata, ".", 0, status);
      ADD_DATACHUNK(worddata, " ", 0, status);
      //play with hindi
      ADD_DATACHUNK(worddata, CharsToUnicodeString("\\u092f\\u0939"), 0, status);
      ADD_DATACHUNK(worddata, " ", 0, status);
      ADD_DATACHUNK(worddata, CharsToUnicodeString("\\u0939\\u093f") + halfNA + CharsToUnicodeString("\\u0926\\u0940"), 0, status);
      ADD_DATACHUNK(worddata, " ", 0, status);
      ADD_DATACHUNK(worddata, CharsToUnicodeString("\\u0939\\u0948"), 0, status);
      ADD_DATACHUNK(worddata, CharsToUnicodeString("\\u0964"), 0, status);   //Danda is similar to full stop-> Danda followed by a space
      ADD_DATACHUNK(worddata, " ", 0, status);
      ADD_DATACHUNK(worddata, CharsToUnicodeString("\\u0905\\u093e\\u092a"), 0, status);
      ADD_DATACHUNK(worddata, " ", 0, status);
      ADD_DATACHUNK(worddata, CharsToUnicodeString("\\u0938\\u093f\\u0916\\u094b\\u0917\\u0947"), 0, status);
      ADD_DATACHUNK(worddata, "?", 0, status);
      ADD_DATACHUNK(worddata, " ", 0, status);
      ADD_DATACHUNK(worddata, "It's", 0, status);
      ADD_DATACHUNK(worddata, " ", 0, status);
      ADD_DATACHUNK(worddata, "$", 0, status);
      ADD_DATACHUNK(worddata, "3", 0, status);
      ADD_DATACHUNK(worddata, "0", 0, status);
      ADD_DATACHUNK(worddata, ".", 0, status);
      ADD_DATACHUNK(worddata, "1", 0, status);
      ADD_DATACHUNK(worddata, "0", 0, status);
      ADD_DATACHUNK(worddata, " ", 0, status);
      // ADD_DATACHUNK(worddata, " ", 0);
      generalIteratorTest(wordIterCustom, worddata);

      delete wordIterCustom;
      delete worddata;
}
//-------------------------------------------------------------------------------
//adds extra rules to deal with abbrevations(limited) and test the word Iteration
//-------------------------------------------------------------------------------
void RBBITest::TestAbbrRuleBasedWordIteration()
{
      logln((UnicodeString)"Testing the RBBI for word iteration by adding rules to support abbreviation");
      RuleBasedBreakIterator *rb =(RuleBasedBreakIterator*)BreakIterator::createWordInstance();

      UnicodeString wrules2="<abbr>=((Mr.)|(Mrs.)|(Ms.)|(Dr.)|(U.S.));" + // abbreviations.
                     rb->getRules()                               +
                     "{(<abbr><ws>)*<word>};";
      RuleBasedBreakIterator wordIter=null;
      //try{
      wordIter   = new RuleBasedBreakIterator(wrules2);
    //  }catch(IllegalArgumentException iae){
   //       errln("ERROR: failed construction illegal rules");
   //   }
      Vector *worddata = new Vector();
      ADD_DATACHUNK(worddata, "Mr. George", 0, status);
      ADD_DATACHUNK(worddata, " ", 0, status);
      ADD_DATACHUNK(worddata, "is", 0, status);
      ADD_DATACHUNK(worddata, " ", 0, status);
      ADD_DATACHUNK(worddata, "from", 0, status);
      ADD_DATACHUNK(worddata, " ", 0, status);
      ADD_DATACHUNK(worddata, "U.S. Navy", 0, status);
      ADD_DATACHUNK(worddata, ".", 0, status);
      ADD_DATACHUNK(worddata, " ", 0, status);
      ADD_DATACHUNK(worddata, "His", 0, status);
      ADD_DATACHUNK(worddata, "\n", 0, status);
      ADD_DATACHUNK(worddata, "friend", 0, status);
      ADD_DATACHUNK(worddata, "\t", 0, status);
      ADD_DATACHUNK(worddata, "Dr. Steven", 0, status);
      ADD_DATACHUNK(worddata, " ", 0, status);
      ADD_DATACHUNK(worddata, "married", 0, status);
      ADD_DATACHUNK(worddata, " ", 0, status);
      ADD_DATACHUNK(worddata, "Ms. Benneth", 0, status);
      ADD_DATACHUNK(worddata, "!", 0, status);
      ADD_DATACHUNK(worddata, " ", 0, status);
      ADD_DATACHUNK(worddata, "Mrs. Johnson", 0, status);
      ADD_DATACHUNK(worddata, "\r\n", 0, status);
      ADD_DATACHUNK(worddata, "paid", 0, status);
      ADD_DATACHUNK(worddata, " ", 0, status);
      ADD_DATACHUNK(worddata, "$2,400.00", 0, status);
      generalIteratorTest(wordIter, worddata);

      delete wordIter;
      delete worddata;
      delete rb;
} */

//---------------------------------------------
// runIndexedTest
//---------------------------------------------

void RBBITest::runIndexedTest( int32_t index, UBool exec, const char* &name, char* /*par*/ )
{
    if (exec) logln("TestSuite RuleBasedBreakIterator: ");
    switch (index) {

        case 0: name = "TestDefaultRuleBasedCharacterIteration";
            if(exec) TestDefaultRuleBasedCharacterIteration(); break;
        case 1: name = "TestDefaultRuleBasedWordIteration";
            if(exec) TestDefaultRuleBasedWordIteration();      break;
        case 2: name = "TestDefaultRuleBasedSentenceIteration";
            if(exec) TestDefaultRuleBasedSentenceIteration();  break;
        case 3: name = "TestDefaulRuleBasedLineIteration";
            if(exec) TestDefaultRuleBasedLineIteration();      break;
        case 4: name = "TestHindiCharacterBreak";
            if(exec) TestHindiCharacterBreak();                break;
        case 5: name = "TestHindiWordBreak";
            if(exec) TestHindiWordBreak();                     break;
        case 6: name = "TestTitleBreak";
            if(exec) TestTitleBreak();                         break;
        case 7: name = "TestStatusReturn";
            if(exec) TestStatusReturn();                       break;
        case 8: name = "TestLineBreakData";
            if(exec) TestLineBreakData();                       break;

//      case 6: name = "TestDanda()";
//           if(exec) TestDanda();                             break;
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
    int32_t     lastP  = -1;
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

    char *       lbdfName = "LBTest.txt";

    // Open the test data file.
    //   TODO:  a proper way to handle this data.
    ss.fFile = fopen(lbdfName, "rb");
    if (ss.fFile == NULL) {
        infoln("Unable to open Line Break Test Data file.  Skipping test.");
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

