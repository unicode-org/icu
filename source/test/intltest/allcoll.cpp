/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

#ifndef _COLL
#include "unicode/coll.h"
#endif

#ifndef _TBLCOLL
#include "unicode/tblcoll.h"
#endif

#ifndef _UNISTR
#include "unicode/unistr.h"
#endif

#ifndef _SORTKEY
#include "unicode/sortkey.h"
#endif

#ifndef _ALLCOLL
#include "allcoll.h"
#endif

/*
 * Include callcoll.c to get the test data.
 * This helps maintain a single copy of the data.
 */
#define INCLUDE_CALLCOLL_C
#ifndef __OS400__
#include "../cintltst/callcoll.c"
#else
#include "cintltst/callcoll.c"
#endif

CollationDummyTest::CollationDummyTest()
: myCollation(0)
{
    UErrorCode status = U_ZERO_ERROR;
    UnicodeString rules(TRUE, DEFAULTRULEARRAY, sizeof(DEFAULTRULEARRAY)/sizeof(DEFAULTRULEARRAY[0]));
    UnicodeString newRules("& C < ch, cH, Ch, CH & Five, 5 & Four, 4 & one, 1 & Ampersand; '&' & Two, 2 ");
    rules += newRules;
    myCollation = new RuleBasedCollator(rules, status);
}

CollationDummyTest::~CollationDummyTest()
{
    delete myCollation;
}

const Collator::EComparisonResult CollationDummyTest::results[] = {
    Collator::LESS,
    Collator::GREATER,
    Collator::LESS,
    Collator::LESS,
    Collator::LESS,
    Collator::LESS,
    Collator::LESS,
    Collator::GREATER,
    Collator::GREATER,
    Collator::LESS,                                     // 10
    Collator::GREATER,
    Collator::LESS,
    Collator::GREATER,
    Collator::GREATER,
    Collator::LESS,
    Collator::LESS,
    Collator::LESS,
    // test primary > 17
    Collator::EQUAL,
    Collator::EQUAL,
    Collator::EQUAL,                                    // 20
    Collator::LESS,
    Collator::LESS,
    Collator::EQUAL,
    Collator::EQUAL,
    Collator::EQUAL,
    Collator::LESS,
    // test secondary > 26
    Collator::EQUAL,
    Collator::EQUAL,
    Collator::EQUAL,
    Collator::EQUAL,
    Collator::EQUAL,                                    // 30
    Collator::EQUAL,
    Collator::LESS,
    Collator::EQUAL,                                     // 34
    Collator::EQUAL,
    Collator::EQUAL,
    Collator::LESS                                        /* 37 */
};

void CollationDummyTest::doTest( UnicodeString source, UnicodeString target, Collator::EComparisonResult result)
{
    Collator::EComparisonResult compareResult = myCollation->compare(source, target);
    CollationKey sortKey1, sortKey2;
    UErrorCode key1status = U_ZERO_ERROR, key2status = U_ZERO_ERROR; //nos
    myCollation->getCollationKey(source, /*nos*/ sortKey1, key1status );
    myCollation->getCollationKey(target, /*nos*/ sortKey2, key2status );
    if (U_FAILURE(key1status) || U_FAILURE(key2status))
    {
        errln("SortKey generation Failed.\n");
        return;
    }

    Collator::EComparisonResult keyResult = sortKey1.compareTo(sortKey2);
    reportCResult( source, target, sortKey1, sortKey2, compareResult, keyResult, result );
}

void CollationDummyTest::TestTertiary(/* char* par */)
{
    int32_t i = 0;
    myCollation->setStrength(Collator::TERTIARY);
    for (i = 0; i < 17 ; i++)
    {
        doTest(testSourceCases[i], testTargetCases[i], results[i]);
    }
}
void CollationDummyTest::TestPrimary(/* char* par */)
{
    int32_t i;
    myCollation->setStrength(Collator::PRIMARY);
    for (i = 17; i < 26; i++)
    {
        doTest(testSourceCases[i], testTargetCases[i], results[i]);
    }
}

void CollationDummyTest::TestSecondary(/* char* par */)
{
    int32_t i;
    myCollation->setStrength(Collator::SECONDARY);
    for (i = 26; i < 34; i++)
    {
        doTest(testSourceCases[i], testTargetCases[i], results[i]);
    }
}

void CollationDummyTest::TestExtra(/* char* par */)
{
    int32_t i, j;
    myCollation->setStrength(Collator::TERTIARY);
    for (i = 0; i < COUNT_TEST_CASES-1; i++)
    {
        for (j = i + 1; j < COUNT_TEST_CASES; j += 1)
        {
            doTest(testCases[i], testCases[j], Collator::LESS);
        }
    }
}

void CollationDummyTest::runIndexedTest( int32_t index, UBool exec, const char* &name, char* /*par*/ )
{
    if (exec) logln("TestSuite CollationDummyTest: ");
    switch (index) {
        case 0: name = "TestPrimary";   if (exec)   TestPrimary(/* par */); break;
        case 1: name = "TestSecondary"; if (exec)   TestSecondary(/* par */); break;
        case 2: name = "TestTertiary";  if (exec)   TestTertiary(/* par */); break;
        case 3: name = "TestExtra";     if (exec)   TestExtra(/* par */); break;
        default: name = ""; break;
    }
}

