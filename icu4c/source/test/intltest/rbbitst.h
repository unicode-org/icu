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


#ifndef RBBITEST_H
#define RBBITEST_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_BREAK_ITERATION

#include "intltest.h"
#include "unicode/brkiter.h"

class Vector;
class Enumeration;
class BITestData;

/**
 * Test the RuleBasedBreakIterator class giving different rules
 */
class RBBITest: public IntlTest {
public:
  
    RBBITest();
    virtual ~RBBITest();

    void runIndexedTest( int32_t index, UBool exec, const char* &name, char* par = NULL );
    /**
     * Tests default rules based character iteration
     **/ 
    void TestDefaultRuleBasedCharacterIteration(void);
     /**
     * Tests default rules based word iteration
     **/ 
    void TestDefaultRuleBasedWordIteration(void);
    /**
     * Tests default rules based word iteration
     **/
    void TestDefaultRuleBasedSentenceIteration(void);
    /**
     * Tests default rules based word iteration
     **/
    void TestDefaultRuleBasedLineIteration(void);
    /**
     * Tests Hindi(Devanagiri) character iteration
     **/  
    void TestHindiCharacterBreak(void);
    /**
     * Tests Hindi(Devanagiri) word iteration
     **/  
    void TestHindiWordBreak(void);
    /**
     * Tests Title Case break iteration
     **/  
    void TestTitleBreak(void);

    /**
     * Tests rule status return values
     **/  
    void TestStatusReturn();

    /**
     * Run the Unicode Line Break test data.
     **/  
    void TestLineBreakData();

    void TestSentenceInvariants();
    void TestCharacterInvariants();
    void TestWordInvariants();
    void TestLineInvariants();
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
    
    
    /**
    * Test Hindi Danda i.e make sure we have a break point before and after danda 
    **/ 
//    void TestDanda(void); 
    /**
    *  Test Hindi Charactet Wrapping behaviour
    **/
//    void TestHindiCharacterWrapping(void);
       

    /**
     * Adds rules for telugu support and tests the behaviour of chracterIterator of RBBI 
     **/
//    void TestTeluguRuleBasedCharacterIteration(void)
    /**
     * Tests the behaviour of character iteration of RBBI with custom rules
     **/
//    void TestCustomRuleBasedCharacterIteration(void);
    /**
     * Tests custom rules based word iteration
     **/
//    void TestCustomRuleBasedWordIteration(void);
    /**
     * Adds extra rules to deal with abbrevations(limited) and test the word Iteration
     **/
//  void TestAbbrRuleBasedWordIteration(void);
   
    
    
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

};

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */

#endif
