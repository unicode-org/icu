/*
*******************************************************************************
* Copyright (C) 2007-2008, International Business Machines Corporation and
* others. All Rights Reserved.
********************************************************************************

* File PLURRULTS.cpp
*
********************************************************************************
*/

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "plurults.h"
#include "unicode/plurrule.h"



void setupResult(const int32_t testSource[], char result[], int32_t* max);
UBool checkEqual(PluralRules *test, char *result, int32_t max);
UBool testEquality(PluralRules *test);

// This is an API test, not a unit test.  It doesn't test very many cases, and doesn't
// try to test the full functionality.  It just calls each function in the class and
// verifies that it works on a basic level.

void PluralRulesTest::runIndexedTest( int32_t index, UBool exec, const char* &name, char* /*par*/ )
{
    if (exec) logln("TestSuite PluralRulesAPI");
    switch (index) {
        TESTCASE(0, testAPI);
        default: name = ""; break;
    }
}

#define PLURAL_TEST_NUM    13
/**
 * Test various generic API methods of PluralRules for API coverage.
 */
void PluralRulesTest::testAPI(/*char *par*/)
{
    UnicodeString pluralTestData[PLURAL_TEST_NUM] = {
            UNICODE_STRING_SIMPLE("a: n is 1"),
            UNICODE_STRING_SIMPLE("a: n mod 10 is 2"),
            UNICODE_STRING_SIMPLE("a: n is not 1"),
            UNICODE_STRING_SIMPLE("a: n mod 3 is not 1"),
            UNICODE_STRING_SIMPLE("a: n in 2..5"),
            UNICODE_STRING_SIMPLE("a: n not in 2..5"),
            UNICODE_STRING_SIMPLE("a: n mod 10 in 2..5"),
            UNICODE_STRING_SIMPLE("a: n mod 10 is 2 and n is not 12"),
            UNICODE_STRING_SIMPLE("a: n mod 10 in 2..3 or n mod 10 is 5"),
            UNICODE_STRING_SIMPLE("a: n is 1 or n is 4 or n is 23"),
            UNICODE_STRING_SIMPLE("a: n mod 2 is 1 and n is not 3 and n in 1..11"),
            UNICODE_STRING_SIMPLE("a: n mod 2 is 1 or n mod 5 is 1 and n is not 6"),
            "",
    };
    static const int32_t pluralTestResult[PLURAL_TEST_NUM][30] = {
        {1, 0},
        {2,12,22, 0},
        {0,2,3,4,5,0},
        {0,2,3,5,6,8,9,0},
        {2,3,4,5,0},
        {0,1,6,7,8, 0},
        {2,3,4,5,12,13,14,15,22,23,24,25,0},
        {2,22,32,42,0},
        {2,3,5,12,13,15,22,23,25,0},
        {1,4,23,0},
        {1,5,7,9,11,0},
        {1,3,5,7,9,11,13,15,16,0},
    };
    UErrorCode status = U_ZERO_ERROR;

    // ======= Test constructors
    logln("Testing PluralRules constructors");
    
        
    logln("\n start default locale test case ..\n");
        
    PluralRules defRule(status); 
    PluralRules* test=new PluralRules(status);
    PluralRules* newEnPlural= test->forLocale(Locale::getEnglish(), status);
    if(U_FAILURE(status)) {
        dataerrln("ERROR: Could not create PluralRules (default) - exitting");
        delete test;
        return;
    }
    delete newEnPlural;

    // ======= Test empty plural rules   
    logln("Testing Simple PluralRules");
      
    PluralRules* empRule = test->createRules(UNICODE_STRING_SIMPLE("a:n"), status);
    UnicodeString key;
    for (int32_t i=0; i<10; ++i) {
        key = empRule->select(i);
        if ( key.charAt(0)!='a' ) {
            errln("ERROR:  empty plural rules test failed! - exitting");
        }
    }
    if (empRule!=NULL) {
        delete empRule;
    }
    
    // ======= Test simple plural rules   
    logln("Testing Simple PluralRules");
        
    char result[100];
    int32_t max;
        
    for (int32_t i=0; i<PLURAL_TEST_NUM-1; ++i) {
       PluralRules *newRules = test->createRules(pluralTestData[i], status);
       setupResult(pluralTestResult[i], result, &max);
       if ( !checkEqual(newRules, result, max) ) {
            errln("ERROR:  simple plural rules failed! - exitting");
            delete test;
            return;
        }
       if (newRules!=NULL) {
           delete newRules;
       }
    }
       

    // ======= Test complex plural rules   
    logln("Testing Complex PluralRules");
    // TODO: the complex test data is hard coded. It's better to implement 
    // a parser to parse the test data.
    UnicodeString complexRule = UNICODE_STRING_SIMPLE("a: n in 2..5; b: n in 5..8; c: n mod 2 is 1"); 
    char cRuleResult[] = {
        'o','c','a','a','a','a','b','b','b','c',
        'o','c'};
    PluralRules *newRules = test->createRules(complexRule, status);
    if ( !checkEqual(newRules, cRuleResult, 12) ) {
         errln("ERROR:  complex plural rules failed! - exitting");
         delete test;
         return;
     }
    if (newRules!=NULL) {
        delete newRules;
        newRules=NULL;
    }
    
    // ======= Test Equality
    logln("Testing Equality of PluralRules");
    

    if ( !testEquality(test) ) {
         errln("ERROR:  complex plural rules failed! - exitting");
         delete test;
         return;
     }

  
// ======= Test getStaticClassID()
    logln("Testing getStaticClassID()");
    
    if(test->getDynamicClassID() != PluralRules::getStaticClassID()) {
        errln("ERROR: getDynamicClassID() didn't return the expected value");
    }
    delete test;
}

void setupResult(const int32_t testSource[], char result[], int32_t* max) {
    int32_t i=0;
    int32_t curIndex=0;
    
    do {
        while (curIndex < testSource[i]) {
            result[curIndex++]='o'; //other
        }
        result[curIndex++]='a';
        
    } while(testSource[++i]>0);
    *max=curIndex;
}


UBool checkEqual(PluralRules *test, char *result, int32_t max) {
    UnicodeString key;
    for (int32_t i=0; i<max; ++i) {
        key= test->select(i);
        if ( key.charAt(0)!=result[i] ) {
            return FALSE;
        }
    }
    return TRUE;
}

#define MAX_EQ_ROW  2
#define MAX_EQ_COL  5
UBool testEquality(PluralRules *test) {
    UnicodeString testEquRules[MAX_EQ_ROW][MAX_EQ_COL] = {
        {   UNICODE_STRING_SIMPLE("a: n in 2..3"),
            UNICODE_STRING_SIMPLE("a: n is 2 or n is 3"), 
            UNICODE_STRING_SIMPLE( "a:n is 3 and n in 2..5 or n is 2"),
            "",
        },
        {   UNICODE_STRING_SIMPLE("a: n is 12; b:n mod 10 in 2..3"),
            UNICODE_STRING_SIMPLE("b: n mod 10 in 2..3 and n is not 12; a: n in 12..12"),
            UNICODE_STRING_SIMPLE("b: n is 13; a: n in 12..13; b: n mod 10 is 2 or n mod 10 is 3"),
            "",
        }
    };
    UErrorCode status = U_ZERO_ERROR;
    UnicodeString key[MAX_EQ_COL];
    UBool ret=TRUE;
    for (int32_t i=0; i<MAX_EQ_ROW; ++i) {
        PluralRules* rules[MAX_EQ_COL];
        
        for (int32_t j=0; j<MAX_EQ_COL; ++j) {
            rules[j]=NULL;
        }
        int32_t totalRules=0;
        while((totalRules<MAX_EQ_COL) && (testEquRules[i][totalRules].length()>0) ) {
            rules[totalRules]=test->createRules(testEquRules[i][totalRules], status);
            totalRules++;
        }
        for (int32_t n=0; n<300 && ret ; ++n) {
            for(int32_t j=0; j<totalRules;++j) {
                key[j] = rules[j]->select(n);
            }
            for(int32_t j=0; j<totalRules-1;++j) {
                if (key[j]!=key[j+1]) {
                    ret= FALSE;
                    break;
                }
            }
            
        }
        for (int32_t j=0; j<MAX_EQ_COL; ++j) {
            if (rules[j]!=NULL) {
                delete rules[j];
            }
        }
    }
    
    return ret;
}
#endif /* #if !UCONFIG_NO_FORMATTING */

