/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1999-2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/************************************************************************
*   Date        Name        Description
*   12/14/99    Madhu        Creation.
************************************************************************/



#ifndef RBBIAPITEST_H
#define RBBIAPITEST_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_BREAK_ITERATION

#include "intltest.h"
#include "unicode/rbbi.h"

/**
 * API Test the RuleBasedBreakIterator class
 */
class RBBIAPITest: public IntlTest {
public:
   
    
    void runIndexedTest( int32_t index, UBool exec, const char* &name, char* par = NULL );
    /**
     * Tests Constructor behaviour of RuleBasedBreakIterator
     **/
   // void TestConstruction(void);   
    /**
     * Tests clone() and equals() methods of RuleBasedBreakIterator         
     **/
    void TestCloneEquals();
    /**
     * Tests toString() method of RuleBasedBreakIterator
     **/
    void TestgetRules();
    /**
     * Tests the method hashCode() of RuleBasedBreakIterator
     **/
    void TestHashCode();
     /**
      * Tests the methods getText() and setText() of RuleBasedBreakIterator
      **/
    void TestGetSetAdoptText();
     /**
      * Testing the methods first(), next(), next(int) and following() of RuleBasedBreakIterator
      **/
    void TestFirstNextFollowing(void);
    /**
     * Testing the methods lastt(), previous(), and preceding() of RuleBasedBreakIterator
     **/
   void TestLastPreviousPreceding(void);
    /**
     * Tests the method IsBoundary() of RuleBasedBreakIterator
     **/
   void TestIsBoundary(void);

    /**
     * Tests creating RuleBasedBreakIterator from rules strings.
     **/
   void TestBuilder(void);

    /**
     * Tests grouping effect of 'single quotes' in rules.
     **/
   void TestQuoteGrouping();

    /**
     *  Tests word break status returns.
     */
    void TestWordStatus();

    void TestBug2190();

    void TestBoilerPlate();

    void TestRegistration();

    /**
     *Internal subroutines
     **/
    /* Internal subroutine used by TestIsBoundary() */ 
    void doBoundaryTest(RuleBasedBreakIterator& bi, UnicodeString& text, int32_t *boundaries);

    /*Internal subroutine used for comparision of expected and acquired results */
    void doTest(UnicodeString& testString, int32_t start, int32_t gotoffset, int32_t expectedOffset, const char* expected);


};

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */

#endif
