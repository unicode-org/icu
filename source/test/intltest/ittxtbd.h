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


#ifndef _INTLTESTTEXTBOUNDARY
#define _INTLTESTTEXTBOUNDARY


#include "unicode/utypes.h"
#include "intltest.h"
#include "unicode/brkiter.h"

class Vector;
class Enumeration;

/**
 * Test the BreakIterator class and indirectly all related classes
 */
class IntlTestTextBoundary: public IntlTest {
public:
    IntlTestTextBoundary();
    ~IntlTestTextBoundary();
    
    void runIndexedTest( int32_t index, bool_t exec, char* &name, char* par = NULL );

    /**
     * Test sentence break using doForwardSelectionTest
     **/
    void TestForwardSentenceSelection(void);
    /**
     * Test sentence break using doFirstSelectionTest
     **/
    void TestFirstSentenceSelection(void);
    /**
     * Test sentence break using doLastSelectionTest
     **/
    void TestLastSentenceSelection(void);
    /**
     * Test sentence break using doBackwardSelectionTest
     **/
    void TestBackwardSentenceSelection(void);
    /**
     * Test sentence break using doForwardIndexSelectionTest
     **/
    void TestForwardSentenceIndexSelection(void);
    /**
     * Test sentence break using doBackwardIndexSelectionTest
     **/
    void TestBackwardSentenceIndexSelection(void);
    /**
     * Test sentence break using doMultipleSelectionTest
     **/
    void TestSentenceMultipleSelection(void);
    /**
     * Test word break using doForwardSelectionTest
     **/
    void TestForwardWordSelection(void);
    /**
     * Test word break using doFirstSelectionTest
     **/
    void TestFirstWordSelection(void);
    /**
     * Test word break using doLastSelectionTest
     **/
    void TestLastWordSelection(void);
    /**
     * Test word break using doBackwardSelectionTest
     **/
    void TestBackwardWordSelection(void);
    /**
     * Test word break using doForwardIndexSelectionTest
     **/
    void TestForwardWordIndexSelection(void);
    /**
     * Test word break using doBackwardIndexSelectionTest
     **/
    void TestBackwardWordIndexSelection(void);
    /**
     * Test word break using doMultipleSelectionTest
     **/
    void TestWordMultipleSelection(void);
    /**
     * Test line break using doLastSelectionTest
     **/
    void TestForwardLineSelection(void);
    /**
     * Test line break using doFirstSelectionTest
     **/
    void TestFirstLineSelection(void);
    /**
     * Test line break using doLastSelectionTest
     **/
    void TestLastLineSelection(void);
    /**
     * Test line break using doBackwardSelectionTest
     **/
    void TestBackwardLineSelection(void);
    /**
     * Test line break using doForwardIndexSelectionTest
     **/
    void TestForwardLineIndexSelection(void);
    /**
     * Test line break using doBackwardIndexSelectionTest
     **/
    void TestBackwardLineIndexSelection(void);
    /**
     * Test line break using doMultipleSelectionTest
     **/
    void TestLineMultipleSelection(void);
    /**
     * Test word break using doForwardIndexSelectionTest
     **/
    void TestForwardCharacterSelection(void);
    /**
     * Test character break using doFirstSelectionTest
     **/
    void TestFirstCharacterSelection(void);
    /**
     * Test character break using doLastSelectionTest
     **/
    void TestLastCharacterSelection(void);
    /**
     * Test character break using doBackwardSelectionTest
     **/
    void TestBackwardCharacterSelection(void);
    /**
     * Test character break using doForwardIndexSelectionTest
     **/
    void TestForwardCharacterIndexSelection(void);
    /**
     * Test character break using doBackwardIndexSelectionTest
     **/
    void TestBackwardCharacterIndexSelection(void);
    /**
     * Test character break using doMultipleSelectionTest
     **/
    void TestCharacterMultipleSelection(void);
    /**
     * test behaviour of BrakIteraor on an empty string
     **/
    void TestEmptyString(void);
    /**
     * Test BreakIterator::getAvailableLocales
     **/
    void TestGetAvailableLocales(void);
    /**
     * Test BreakIterator::getDisplayName
     **/
    void TestGetDisplayName(void);
    /**
     * test methods preceding, following and isBoundary
     **/
    void TestPreceding(void);

    void TestJapaneseLineBreak(void);

    void TestBug4153072(void);

    void TestEndBehavior(void);

    void TestSentenceInvariants(void);

    void TestWordInvariants(void);
    
    void TestLineInvariants(void);

    void TestCharacterInvariants(void);

private:
    /**
     * internal methods to prepare test data
     **/
    void addTestWordData(void);
    void addTestSentenceData(void);
    void addTestLineData(void);
    void addTestCharacterData(void);

    UnicodeString createTestData(Enumeration* e);


    /**
     * Perform tests of BreakIterator forward functionality 
     * on different kinds of iterators (word, sentence, line and character)
     **/
    void doForwardSelectionTest(BreakIterator& iterator, UnicodeString& testText, Vector* result);
    /**
     * Perform tests of BreakIterator backward functionality 
     * on different kinds of iterators (word, sentence, line and character)
     **/
    void doBackwardSelectionTest(BreakIterator& iterator, UnicodeString& testText, Vector* result);
    /**
     * Perform tests of BreakIterator first selection functionality 
     * on different kinds of iterators (word, sentence, line and character)
     **/
    void doFirstSelectionTest(BreakIterator& iterator, UnicodeString& testText, Vector* result);
    /**
     * Perform tests of BreakIterator last selection functionality 
     * on different kinds of iterators (word, sentence, line and character)
     **/
    void doLastSelectionTest(BreakIterator& iterator, UnicodeString& testText, Vector* result);
    /**
     * Perform tests of BreakIterator forward index functionality 
     * on different kinds of iterators (word, sentence, line and character)
     **/
    void doForwardIndexSelectionTest(BreakIterator& iterator, UnicodeString& testText, Vector* result);
    /**
     * Perform tests of BreakIterator backward index functionality 
     * on different kinds of iterators (word, sentence, line and character)
     **/
    void doBackwardIndexSelectionTest(BreakIterator& iterator, UnicodeString& testText, Vector* result);
    /**
     * Perform tests of BreakIterator multiple selection functionality 
     * on different kinds of iterators (word, sentence, line and character)
     **/
    void doMultipleSelectionTest(BreakIterator& iterator, UnicodeString& testText);
    /**
     * Perform tests with short sample code
     **/
    void sample(BreakIterator& tb, UnicodeString& text, UnicodeString& title);

    void doBreakInvariantTest(BreakIterator& tb, UnicodeString& testChars);

    void doOtherInvariantTest(BreakIterator& tb, UnicodeString& testChars);

    Vector* lineSelectionData;
    UnicodeString testLineText;
    Vector* sentenceSelectionData;
    UnicodeString testSentenceText;
    Vector* wordSelectionData;
    UnicodeString testWordText;
    Vector* characterSelectionData;
    UnicodeString testCharacterText;
    static const UChar cannedTestArray[];
    static UnicodeString *cannedTestChars;
};


#endif
