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


#ifndef RBBITEST_H
#define RBBITEST_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_BREAK_ITERATION

#include "intltest.h"
#include "unicode/brkiter.h"

class  Vector;
class  Enumeration;
class  BITestData;
struct TestParams;
class  RBBIMonkeyKind;

/**
 * Test the RuleBasedBreakIterator class giving different rules
 */
class RBBITest: public IntlTest {
public:
  
    RBBITest();
    virtual ~RBBITest();

    void runIndexedTest( int32_t index, UBool exec, const char* &name, char* par = NULL );
 
    /**
     * Tests rule status return values
     **/  
    void TestStatusReturn();

    /**
     * Run the Unicode Line Break test data.
     **/  
    void TestLineBreakData();

    /**
     * Run tests from external test data file.
     */

    void TestSentenceInvariants();
    void TestCharacterInvariants();
    void TestWordInvariants();
    void TestEmptyString();
    void TestGetAvailableLocales();
    void TestGetDisplayName();
    void TestEndBehaviour();
    void TestBug4153072();
    void TestJapaneseLineBreak();
    void TestThaiLineBreak();
    void TestMixedThaiLineBreak(); 
    void TestMaiyamok(); 
    void TestThaiWordBreak();
    void TestMonkey(char *params);

    void TestExtended();
    UChar *ReadAndConvertFile(const char *fileName, int &ulen, UErrorCode &status);
    void executeTest(TestParams *);

    void TestWordBreaks();
    void TestWordBoundary();
    void TestLineBreaks();
    void TestSentBreaks();
    void TestBug3818();
    
    
/***********************/
private:
    /**
     * internal methods to prepare test data
     **/
   
    /**
     * Perform tests of BreakIterator forward and backward functionality 
     * on different kinds of iterators (word, sentence, line and character).
     * It tests the methods first(), next(), current(), preceding(), following()
     * previous() and isBoundary().
     * It makes use of internal functions to achieve this.
     **/
    void generalIteratorTest(RuleBasedBreakIterator& bi, BITestData  &td);
    /**
     * Internal method to perform iteration and test the first() and next() functions
     **/
    void testFirstAndNext(RuleBasedBreakIterator& bi, BITestData &td);
    /**
     * Internal method to perform iteration and test the last() and previous() functions
     **/
    void testLastAndPrevious(RuleBasedBreakIterator& bi, BITestData &td);
    /**
     * Internal method to perform iteration and test the following() function
     **/
    void testFollowing(RuleBasedBreakIterator& bi, BITestData &td);
    /**
     * Internal method to perform iteration and test the preceding() function
     **/
    void testPreceding(RuleBasedBreakIterator& bi, BITestData &td);
    /**
     * Internal method to perform iteration and test the isBoundary() function
     **/
    void testIsBoundary(RuleBasedBreakIterator& bi, BITestData &td);
    /**
     * Internal method to perform tests of BreakIterator multiple selection functionality 
     * on different kinds of iterators (word, sentence, line and character)
     **/
    void doMultipleSelectionTest(RuleBasedBreakIterator& iterator, BITestData &td);

    void doBreakInvariantTest(BreakIterator& tb, UnicodeString& testChars);
    void doOtherInvariantTest(BreakIterator& tb, UnicodeString& testChars);

    void RunMonkey(BreakIterator *bi, RBBIMonkeyKind &mk, const char *name, uint32_t  seed, int32_t loopCount);

};

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */

#endif
