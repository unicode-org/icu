/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1999-2000, International Business Machines Corporation and
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
#include "intltest.h"
#include "unicode/brkiter.h"

class Vector;
class Enumeration;

/**
 * Test the RuleBasedBreakIterator class giving different rules
 */
class RBBITest: public IntlTest {
public:
  
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
    void generalIteratorTest(RuleBasedBreakIterator& bi, Vector* expectedResult);
    /**
     * Internal method to perform iteration and test the first() and next() functions
     **/
    Vector* testFirstAndNext(RuleBasedBreakIterator& bi, UnicodeString& text);
    /**
     * Internal method to perform iteration and test the last() and previous() functions
     **/
    Vector* testLastAndPrevious(RuleBasedBreakIterator& bi, UnicodeString& text);
    /**
     * Internal method to perform iteration and test the following() function
     **/
    void testFollowing(RuleBasedBreakIterator& bi, UnicodeString& text, int32_t *boundaries);
    /**
     * Internal method to perform iteration and test the preceding() function
     **/
    void testPreceding(RuleBasedBreakIterator& bi, UnicodeString& text, int32_t *boundaries);
    /**
     * Internal method to perform iteration and test the isBoundary() function
     **/
    void testIsBoundary(RuleBasedBreakIterator& bi, UnicodeString& text, int32_t *boundaries);
    /** 
     * Internal method which does the comparision of expected and got results.
     **/
    void compareFragmentLists(UnicodeString& f1Name, UnicodeString& f2Name, Vector* f1, Vector* f2);
    /**
     * Internal method to perform tests of BreakIterator multiple selection functionality 
     * on different kinds of iterators (word, sentence, line and character)
     **/
    void doMultipleSelectionTest(RuleBasedBreakIterator& iterator, UnicodeString& testText);
    /**
     * Internal method to create test data string from an enumerator
     **/
    UnicodeString createTestData(Enumeration* e);

};


#endif
