/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/


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
    virtual ~IntlTestTextBoundary();
    
    void runIndexedTest( int32_t index, UBool exec, const char* &name, char* par = NULL );
    /**
     * Test sentence break using generalIteratorTest()
     **/
    void TestSentenceIteration(void);
    /**
     * Test word break using generalIteratorTest()
     **/
    void TestWordIteration(void);
    /**
     * Test line break using generalIteratorTest()
     **/ 
    void TestLineIteration(void);
    /**
     * Test character break using generalIteratorTest()
     **/
    void TestCharacterIteration(void);
    /**
     * Test sentence break using ()
     **/
    void TestSentenceInvariants(void);
     /**
     * Test sentence break Invariants using generalIteratorTest()
     **/ 
    void TestWordInvariants(void);
     /**
     * Test sentence break Invariants using generalIteratorTest()
     **/
    void TestLineInvariants(void);
     /**
     * Test sentence break Invariants using generalIteratorTest()
     **/
    void TestCharacterInvariants(void);
     /**
     * Test Japanese line break Invariants using generalIteratorTest()
     **/
    void TestJapaneseLineBreak(void);
     /**
     * Test Thai line break using generalIteratorTest()
     **/
    void TestThaiLineBreak(void);
     /**
     * Test Mixed Thai (thai with other languages like english)line break using generalIteratorTest()
     **/
    void TestMixedThaiLineBreak(void);
    /**
     * Test Thai Line break with Maiyamok using generalIteratorTest()
     * The Thai maiyamok character is a shorthand symbol that means "repeat the previous
     * word".  Instead of appearing as a word unto itself, however, it's kept together
     * with the word before it
     **/
    void TestMaiyamok(void);
    /**
     * test behaviour of BreakIterator on an empty string
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

    void TestBug4153072(void);
    /**
     * Test End Behaviour
     * @bug 4068137
     **/
    void TestEndBehaviour(void);

/***********************/
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
     * Perform tests of BreakIterator forward and backward functionality 
     * on different kinds of iterators (word, sentence, line and character).
     * It tests the methods first(), next(), current(), preceding(), following()
     * previous() and isBoundary().
     * It makes use of internal functions to achieve this.
     **/
    void generalIteratorTest(BreakIterator& bi, Vector* expectedResult);
    /**
     * Internal method to perform iteration and test the first() and next() functions
     **/
    Vector* testFirstAndNext(BreakIterator& bi, UnicodeString& text);
    /**
     * Internal method to perform iteration and test the last() and previous() functions
     **/
    Vector* testLastAndPrevious(BreakIterator& bi, UnicodeString& text);
    /**
     * Internal method to perform iteration and test the following() function
     **/
    void testFollowing(BreakIterator& bi, UnicodeString& text, int32_t *boundaries);
    /**
     * Internal method to perform iteration and test the preceding() function
     **/
    void testPreceding(BreakIterator& bi, UnicodeString& text, int32_t *boundaries);
    /**
     * Internal method to perform iteration and test the isBoundary() function
     **/
    void testIsBoundary(BreakIterator& bi, UnicodeString& text, int32_t *boundaries);
    /** 
     * Internal method which does the comparision of expected and got results.
     **/
    void compareFragmentLists(UnicodeString& f1Name, UnicodeString& f2Name, Vector* f1, Vector* f2);
    /**
     * Internal method to perform tests of BreakIterator multiple selection functionality 
     * on different kinds of iterators (word, sentence, line and character)
     **/
    void doMultipleSelectionTest(BreakIterator& iterator, UnicodeString& testText);
    /**
     * Internal method to perform tests of BreakIterator break Invariants 
     * on different kinds of iterators (word, sentence, line and character)
     **/
    void doBreakInvariantTest(BreakIterator& tb, UnicodeString& testChars);
    /**
     * Internal method to perform tests of BreakIterator other invariants 
     * on different kinds of iterators (word, sentence, line and character)
     **/
    void doOtherInvariantTest(BreakIterator& tb, UnicodeString& testChars);
    /**
     * Perform tests with short sample code
     **/ 
    void sample(BreakIterator& tb, UnicodeString& text, UnicodeString& title);
    /**
     * The vectors holding test data for testing 
     * different kinds of iterators( word, sentence, line and character)
     **/
    Vector* lineSelectionData;
    Vector* sentenceSelectionData;
    Vector* wordSelectionData;
    Vector* characterSelectionData;

    static const UChar cannedTestArray[];
    static UnicodeString *cannedTestChars;
};


#endif
