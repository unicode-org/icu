/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
*
* File CREGRTST.H
*
* Modification History:
*        Name                     Description            
*     Madhu Katragadda            Converted to C, added extra functions and tests
*********************************************************************************
*/

/*C API functionality and regression test for BreakIterator*/

#ifndef _CBRKITREGTEST
#define _CBRKITREGTEST

#include "unicode/utypes.h"

#if !UCONFIG_NO_BREAK_ITERATION

#include "cintltst.h"


struct Vector1;
/* Internal functions used */
    void addElement(struct Vector1*, const char*);
    UChar * addElement2(struct Vector1*, const UChar*);
    void cleanupVector(struct Vector1*);
    int32_t Count(struct Vector1*);
    UChar* elementAt(struct Vector1*, int32_t);
/* Internal Functions used */
    UChar* extractBetween(int32_t start, int32_t end, UChar* text);
    UChar* CharsToUCharArray(const char*);
    UChar* UCharToUCharArray(const UChar uchar);

    void AllocateTextBoundary(void);
    void FreeTextBoundary(void);

/* The test functions */

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
     * Test sentence break using doForwardSelectionTest
     **/
     void TestForwardSentenceSelection(void);
    /**
     * Test sentence break using doBackwardSelectionTest
     **/
    void TestBackwardSentenceSelection(void);
    /**
     * Test sentence break using doFirstSelectionTest
     **/
    void TestFirstSentenceSelection(void);
    /**
     * Test sentence break using doLastSelectionTest
     **/
    void TestLastSentenceSelection(void); 
    /**
     * Test sentence break using doForwardIndexSelectionTest
    **/
    void TestForwardSentenceIndexSelection(void);
    /**
    * Test sentence break using doBackwardIndexSelectionTest
    **/
    void TestBackwardSentenceIndexSelection(void);  

    /**
     * Test line break using doForwardSelectionTest
     **/
    void TestForwardLineSelection(void);
    /**
     * Test line break using doBackwardSelectionTest
     **/
    void TestBackwardLineSelection(void); 
    /**
     * Test line break using doFirstSelectionTest
     **/
    void TestFirstLineSelection(void); 
    /**
     * Test line break using doLastSelectionTest
     **/
    void TestLastLineSelection(void); 
    /**
     * Test line break using doForwardIndexSelectionTest
     **/
    void TestForwardLineIndexSelection(void); 
    /**
     * Test line break using doBackwardIndexSelectionTest
     **/
    void TestBackwardLineIndexSelection(void);

    /**
     * Test character break using doForwardSelectionTest
     **/
    void TestForwardCharacterSelection(void);
    /**
     * Test character break using doBackwardSelectionTest
     **/
    void TestBackwardCharacterSelection(void); 
    /**
     * Test character break using doFirstSelectionTest
     **/
    void TestFirstCharacterSelection(void); 
    /**
     * Test character break using doLastSelectionTest
     **/
    void TestLastCharacterSelection(void); 
    /**
     * Test character break using doForwardIndexSelectionTest
     **/
    void TestForwardCharacterIndexSelection(void); 
    /**
     * Test character break using doBackwardIndexSelectionTest
     **/
    void TestBackwardCharacterIndexSelection(void); 


    /**
     * test methods ubrk_preceding(), ubrk_following() 
     **/
    void TestPreceding(void);
    void TestEndBehaviour(void);

    void TestWordInvariants(void);
    void TestSentenceInvariants(void);
    void TestCharacterInvariants(void);
    void TestLineInvariants(void);
/*-----------------*/
/* Internal functions to prepare test data */

    void addTestWordData(void);
    void addTestSentenceData(void);
    void addTestLineData(void);
    void addTestCharacterData(void);
    UChar* createTestData(struct Vector1*, int32_t);

/* Test Implementation routines*/

    /**
     * Perform tests of BreakIterator forward functionality 
     * on different kinds of iterators (word, sentence, line and character)
     **/
    void doForwardSelectionTest(UBreakIterator*,  UChar* testText, struct Vector1* result);
    /**
     * Perform tests of BreakIterator backward functionality 
     * on different kinds of iterators (word, sentence, line and character)
     **/
    void doBackwardSelectionTest(UBreakIterator*, UChar* testText, struct Vector1* result);
    /**
     * Perform tests of BreakIterator first selection functionality 
     * on different kinds of iterators (word, sentence, line and character)
     **/
    void doFirstSelectionTest(UBreakIterator* iterator, UChar* testText, struct Vector1* result);
    /**
     * Perform tests of BreakIterator last selection functionality 
     * on different kinds of iterators (word, sentence, line and character)
     **/
    void doLastSelectionTest(UBreakIterator* iterator, UChar* testText, struct Vector1* result);
    /**
     * Perform tests of BreakIterator backward index functionality 
     * on different kinds of iterators (word, sentence, line and character)
     **/
    void doForwardIndexSelectionTest(UBreakIterator* iterator, UChar* testText, struct Vector1* result);
    /**
     * Perform tests of BreakIterator multiple selection functionality 
     * on different kinds of iterators (word, sentence, line and character)
     **/
    void doBackwardIndexSelectionTest(UBreakIterator* iterator, UChar* testText, struct Vector1* result);
    
    void doBreakInvariantTest(UBreakIteratorType type, UChar* testChars);
    
    void doOtherInvariantTest(UBreakIteratorType type , UChar* testChars);
    /**
     * Perform tests with short sample code
     **/
    void sample(UBreakIterator* tb, UChar* text);


struct Vector1* wordSelectionData;
struct Vector1* sentenceSelectionData;
struct Vector1* lineSelectionData;
struct Vector1* characterSelectionData;

UChar* testWordText;
UChar* testSentenceText;
UChar* testLineText;
UChar* testCharacterText;


static UChar *cannedTestChars;

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */

#endif
