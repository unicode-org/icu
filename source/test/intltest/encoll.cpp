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

#ifndef _ENCOLL
#include "encoll.h"
#endif

CollationEnglishTest::CollationEnglishTest()
: myCollation(0)
{
    UErrorCode status = U_ZERO_ERROR;
    myCollation = Collator::createInstance(Locale::ENGLISH, status);
}

CollationEnglishTest::~CollationEnglishTest()
{
    delete myCollation;
}

const UChar CollationEnglishTest::testSourceCases[][CollationEnglishTest::MAX_TOKEN_LEN] = {
        {'a', 'b', 0},
        {'b', 'l', 'a', 'c', 'k', '-', 'b', 'i', 'r', 'd', 0},
        {'b', 'l', 'a', 'c', 'k', ' ', 'b', 'i', 'r', 'd', 0},
        {'b', 'l', 'a', 'c', 'k', '-', 'b', 'i', 'r', 'd', 0},
        {'H', 'e', 'l', 'l', 'o', 0},
        {'A', 'B', 'C', 0}, 
        {'a', 'b', 'c', 0},
        {'b', 'l', 'a', 'c', 'k', 'b', 'i', 'r', 'd', 0},
        {'b', 'l', 'a', 'c', 'k', '-', 'b', 'i', 'r', 'd', 0},
        {'b', 'l', 'a', 'c', 'k', '-', 'b', 'i', 'r', 'd', 0},
        {'p', 0x00EA, 'c', 'h', 'e', 0},                                            // 10
        {'p', 0x00E9, 'c', 'h', 0x00E9, 0},
        {0x00C4, 'B', 0x0308, 'C', 0x0308, 0},
        {'a', 0x0308, 'b', 'c', 0},
        {'p', 0x00E9, 'c', 'h', 'e', 'r', 0},
        {'r', 'o', 'l', 'e', 's', 0},
        {'a', 'b', 'c', 0},
        {'A', 0},
        {'A', 0},
        {'a', 'b', 0},                                                                // 20
        {'t', 'c', 'o', 'm', 'p', 'a', 'r', 'e', 'p', 'l', 'a', 'i', 'n', 0},
        {'a', 'b', 0}, 
        {'a', '#', 'b', 0},
        {'a', '#', 'b', 0},
        {'a', 'b', 'c', 0},
        {'A', 'b', 'c', 'd', 'a', 0},
        {'a', 'b', 'c', 'd', 'a', 0},
        {'a', 'b', 'c', 'd', 'a', 0},
        {0x00E6, 'b', 'c', 'd', 'a', 0},
        {0x00E4, 'b', 'c', 'd', 'a', 0},                                            // 30
        {'a', 'b', 'c', 0},
        {'a', 'b', 'c', 0},
        {'a', 'b', 'c', 0},
        {'a', 'b', 'c', 0},
        {'a', 'b', 'c', 0},
        {'a', 'c', 'H', 'c', 0},
        {'a', 0x0308, 'b', 'c', 0},
        {'t', 'h', 'i', 0x0302, 's', 0},
        {'p', 0x00EA, 'c', 'h', 'e'},
        {'a', 'b', 'c', 0},                                                         // 40
        {'a', 'b', 'c', 0},
        {'a', 'b', 'c', 0},
        {'a', 0x00E6, 'c', 0},
        {'a', 'b', 'c', 0},
        {'a', 'b', 'c', 0},
        {'a', 0x00E6, 'c', 0},
        {'a', 'b', 'c', 0},
        {'a', 'b', 'c', 0},               
        {'p', 0x00E9, 'c', 'h', 0x00E9, 0}                                            // 49
};

const UChar CollationEnglishTest::testTargetCases[][CollationEnglishTest::MAX_TOKEN_LEN] = {
        {'a', 'b', 'c', 0},
        {'b', 'l', 'a', 'c', 'k', 'b', 'i', 'r', 'd', 0},
        {'b', 'l', 'a', 'c', 'k', '-', 'b', 'i', 'r', 'd', 0},
        {'b', 'l', 'a', 'c', 'k', 0},
        {'h', 'e', 'l', 'l', 'o', 0},
        {'A', 'B', 'C', 0},
        {'A', 'B', 'C', 0},
        {'b', 'l', 'a', 'c', 'k', 'b', 'i', 'r', 'd', 's', 0},
        {'b', 'l', 'a', 'c', 'k', 'b', 'i', 'r', 'd', 's', 0},
        {'b', 'l', 'a', 'c', 'k', 'b', 'i', 'r', 'd', 0},                             // 10
        {'p', 0x00E9, 'c', 'h', 0x00E9, 0},
        {'p', 0x00E9, 'c', 'h', 'e', 'r', 0},
        {0x00C4, 'B', 0x0308, 'C', 0x0308, 0},
        {'A', 0x0308, 'b', 'c', 0},
        {'p', 0x00E9, 'c', 'h', 'e', 0},
        {'r', 'o', 0x0302, 'l', 'e', 0},
        {'A', 0x00E1, 'c', 'd', 0},
        {'A', 0x00E1, 'c', 'd', 0},
        {'a', 'b', 'c', 0},
        {'a', 'b', 'c', 0},                                                             // 20
        {'T', 'C', 'o', 'm', 'p', 'a', 'r', 'e', 'P', 'l', 'a', 'i', 'n', 0},
        {'a', 'B', 'c', 0},
        {'a', '#', 'B', 0},
        {'a', '&', 'b', 0},
        {'a', '#', 'c', 0},
        {'a', 'b', 'c', 'd', 'a', 0},
        {0x00C4, 'b', 'c', 'd', 'a', 0},
        {0x00E4, 'b', 'c', 'd', 'a', 0},
        {0x00C4, 'b', 'c', 'd', 'a', 0},
        {0x00C4, 'b', 'c', 'd', 'a', 0},                                             // 30
        {'a', 'b', '#', 'c', 0},
        {'a', 'b', 'c', 0},
        {'a', 'b', '=', 'c', 0},
        {'a', 'b', 'd', 0},
        {0x00E4, 'b', 'c', 0},
        {'a', 'C', 'H', 'c', 0},
        {0x00E4, 'b', 'c', 0},
        {'t', 'h', 0x00EE, 's', 0},
        {'p', 0x00E9, 'c', 'h', 0x00E9, 0},
        {'a', 'B', 'C', 0},                                                          // 40
        {'a', 'b', 'd', 0},
        {0x00E4, 'b', 'c', 0},
        {'a', 0x00C6, 'c', 0},
        {'a', 'B', 'd', 0},
        {0x00E4, 'b', 'c', 0},
        {'a', 0x00C6, 'c', 0},
        {'a', 'B', 'd', 0},
        {0x00E4, 'b', 'c', 0},          
        {'p', 0x00EA, 'c', 'h', 'e', 0}                                                 // 49
};

const Collator::EComparisonResult CollationEnglishTest::results[] = {
        Collator::LESS, 
        Collator::GREATER,
        Collator::LESS,
        Collator::GREATER,
        Collator::GREATER,
        Collator::EQUAL,
        Collator::LESS,
        Collator::LESS,
        Collator::LESS,
        Collator::GREATER,                                                          // 10
        Collator::GREATER,
        Collator::LESS,
        Collator::EQUAL,
        Collator::LESS,
        Collator::GREATER,
        Collator::GREATER,
        Collator::GREATER,
        Collator::LESS,
        Collator::LESS,
        Collator::LESS,                                                             // 20
        Collator::LESS,
        Collator::LESS,
        Collator::LESS,
        Collator::GREATER,
        Collator::GREATER,
        Collator::GREATER,
        // Test Tertiary  > 26
        Collator::LESS,
        Collator::LESS,
        Collator::GREATER,
        Collator::LESS,                                                             // 30
        Collator::GREATER,
        Collator::EQUAL,
        Collator::GREATER,
        Collator::LESS,
        Collator::LESS,
        Collator::LESS,
        // test identical > 36
        Collator::EQUAL,
        Collator::EQUAL,
        // test primary > 38
        Collator::EQUAL,
        Collator::EQUAL,                                                            // 40
        Collator::LESS,
        Collator::EQUAL,
        Collator::EQUAL,
        // test secondary > 43
        Collator::LESS,
        Collator::LESS,
        Collator::EQUAL,
        Collator::LESS,
        Collator::LESS, 
        Collator::LESS                                                                 // 49
};

const UChar CollationEnglishTest::testBugs[][CollationEnglishTest::MAX_TOKEN_LEN] = {
    {'a', 0},
    {'A', 0},
    {'e', 0},
    {'E', 0},
    {0x00e9, 0},
    {0x00e8, 0},
    {0x00ea, 0},
    {0x00eb, 0},
    {'e', 'a', 0},
    {'x', 0}
};

// 0x0300 is grave, 0x0301 is acute
// the order of elements in this array must be different than the order in CollationFrenchTest
const UChar CollationEnglishTest::testAcute[][CollationEnglishTest::MAX_TOKEN_LEN] = {
    {'e', 'e', 0},
    {'e', 'e', 0x0301, 0},
    {'e', 'e', 0x0301, 0x0300, 0},
    {'e', 'e', 0x0300, 0},
    {'e', 'e', 0x0300, 0x0301, 0},
    {'e', 0x0301, 'e', 0},
    {'e', 0x0301, 'e', 0x0301, 0},
    {'e', 0x0301, 'e', 0x0301, 0x0300, 0},
    {'e', 0x0301, 'e', 0x0300, 0},
    {'e', 0x0301, 'e', 0x0300, 0x0301, 0},
    {'e', 0x0301, 0x0300, 'e', 0},
    {'e', 0x0301, 0x0300, 'e', 0x0301, 0},
    {'e', 0x0301, 0x0300, 'e', 0x0301, 0x0300, 0},
    {'e', 0x0301, 0x0300, 'e', 0x0300, 0},
    {'e', 0x0301, 0x0300, 'e', 0x0300, 0x0301, 0},
    {'e', 0x0300, 'e', 0},
    {'e', 0x0300, 'e', 0x0301, 0},
    {'e', 0x0300, 'e', 0x0301, 0x0300, 0},
    {'e', 0x0300, 'e', 0x0300, 0},
    {'e', 0x0300, 'e', 0x0300, 0x0301, 0},
    {'e', 0x0300, 0x0301, 'e', 0},
    {'e', 0x0300, 0x0301, 'e', 0x0301, 0},
    {'e', 0x0300, 0x0301, 'e', 0x0301, 0x0300, 0},
    {'e', 0x0300, 0x0301, 'e', 0x0300, 0},
    {'e', 0x0300, 0x0301, 'e', 0x0300, 0x0301, 0}
};

static const UChar testMore[][CollationEnglishTest::MAX_TOKEN_LEN] = {
    {'a', 'e', 0},
    { 0x00E6, 0},
    { 0x00C6, 0},
    {'a', 'f', 0},
    {'o', 'e', 0},
    { 0x0153, 0},
    { 0x0152, 0},
    {'o', 'f', 0},
};

void CollationEnglishTest::doTest( UnicodeString source, UnicodeString target, Collator::EComparisonResult result)
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

void CollationEnglishTest::TestTertiary( char* par )
{
    int32_t i = 0;
    myCollation->setStrength(Collator::TERTIARY);
    for (i = 0; i < 38 ; i++)
    {
        doTest(testSourceCases[i], testTargetCases[i], results[i]);
    }

    int32_t j = 0;
    for (i = 0; i < 10; i++)
    {
        for (j = i+1; j < 10; j++)
        {
            doTest(testBugs[i], testBugs[j], Collator::LESS);
        }
    }

    //test more interesting cases
    Collator::EComparisonResult expected;
    const int32_t testMoreSize = sizeof(testMore) / sizeof(testMore[0]);
    for (i = 0; i < testMoreSize; i++)
    {
        for (j = 0; j < testMoreSize; j++)
        {
            if (i <  j) expected = Collator::LESS;
            if (i == j) expected = Collator::EQUAL;
            if (i >  j) expected = Collator::GREATER;
            doTest(testMore[i], testMore[j], expected );
        }
    }

}

void CollationEnglishTest::TestPrimary( char* par )
{
    int32_t i;
    myCollation->setStrength(Collator::PRIMARY);
    for (i = 38; i < 43 ; i++)
    {
        doTest(testSourceCases[i], testTargetCases[i], results[i]);
    }
}

void CollationEnglishTest::TestSecondary( char* par )
{
    int32_t i;
    myCollation->setStrength(Collator::SECONDARY);
    for (i = 43; i < 49 ; i++)
    {
        doTest(testSourceCases[i], testTargetCases[i], results[i]);
    }

    //test acute and grave ordering (compare to french collation)
    int32_t j;
    Collator::EComparisonResult expected;
    const int32_t testAcuteSize = sizeof(testAcute) / sizeof(testAcute[0]);
    for (i = 0; i < testAcuteSize; i++)
    {
        for (j = 0; j < testAcuteSize; j++)
        {
            if (i <  j) expected = Collator::LESS;
            if (i == j) expected = Collator::EQUAL;
            if (i >  j) expected = Collator::GREATER;
            doTest(testAcute[i], testAcute[j], expected );
        }
    }
}

void CollationEnglishTest::runIndexedTest( int32_t index, bool_t exec, char* &name, char* par )
{
    if (exec) logln("TestSuite CollationEnglishTest: ");
    switch (index) {
        case 0: name = "TestPrimary";   if (exec)   TestPrimary( par ); break;
        case 1: name = "TestSecondary"; if (exec)   TestSecondary( par ); break;
        case 2: name = "TestTertiary";  if (exec)   TestTertiary( par ); break;
        default: name = ""; break;
    }
}


