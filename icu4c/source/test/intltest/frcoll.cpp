
/*
********************************************************************
* COPYRIGHT: 
* (C) Copyright Taligent, Inc., 1997
* (C) Copyright International Business Machines Corporation, 1997 - 1998
* Licensed Material - Program-Property of IBM - All Rights Reserved. 
* US Government Users Restricted Rights - Use, duplication, or disclosure 
* restricted by GSA ADP Schedule Contract with IBM Corp. 
*
********************************************************************
*/

#ifndef _COLL
#include "coll.h"
#endif

#ifndef _TBLCOLL
#include "tblcoll.h"
#endif

#ifndef _UNISTR
#include "unistr.h"
#endif

#ifndef _SORTKEY
#include "sortkey.h"
#endif

#ifndef _FRCOLL
#include "frcoll.h"
#endif

CollationFrenchTest::CollationFrenchTest()
: myCollation(0)
{
    UErrorCode status = U_ZERO_ERROR;
    myCollation = Collator::createInstance(Locale::FRANCE, status);
}

CollationFrenchTest::~CollationFrenchTest()
{
    delete myCollation;
}

const UChar CollationFrenchTest::testSourceCases[][CollationFrenchTest::MAX_TOKEN_LEN] =
{
    {'a', 'b', 'c', 0},
    {'C', 'O', 'T', 'E', 0},
    {'c', 'o', '-', 'o', 'p', 0},
    {'p', 0x00EA, 'c', 'h', 'e', 0},
    {'p', 0x00EA, 'c', 'h', 'e', 'r', 0},
    {'p', 0x00E9, 'c', 'h', 'e', 'r', 0},
    {'p', 0x00E9, 'c', 'h', 'e', 'r', 0},
    {'H', 'e', 'l', 'l', 'o', 0},
    {0x01f1, 0},
    {0xfb00, 0},
    {0x01fa, 0},
    {0x0101, 0}
};

const UChar CollationFrenchTest::testTargetCases[][CollationFrenchTest::MAX_TOKEN_LEN] =
{
    {'A', 'B', 'C', 0},
    {'c', 0x00f4, 't', 'e', 0},
    {'C', 'O', 'O', 'P', 0},
    {'p', 0x00E9, 'c', 'h', 0x00E9, 0},
    {'p', 0x00E9, 'c', 'h', 0x00E9, 0},
    {'p', 0x00EA, 'c', 'h', 'e', 0},
    {'p', 0x00EA, 'c', 'h', 'e', 'r', 0},
    {'h', 'e', 'l', 'l', 'O', 0},
    {0x01ee, 0},
    {0x25ca, 0},
    {0x00e0, 0},
    {0x01df, 0}
};

const Collator::EComparisonResult CollationFrenchTest::results[] =
{
    Collator::LESS,
    Collator::LESS,
    Collator::GREATER,
    Collator::LESS,
    Collator::GREATER,
    Collator::GREATER,
    Collator::LESS,
    Collator::GREATER,
    Collator::GREATER,
    Collator::GREATER,
    Collator::GREATER,
    Collator::GREATER
};

// 0x0300 is grave, 0x0301 is acute
// the order of elements in this array must be different than the order in CollationEnglishTest
const UChar CollationFrenchTest::testAcute[][CollationFrenchTest::MAX_TOKEN_LEN] =
{
    {'e', 'e', 0},
    {'e', 0x0301, 'e', 0},
    {'e', 0x0301, 0x0300, 'e', 0},
    {'e', 0x0300, 'e', 0},
    {'e', 0x0300, 0x0301, 'e', 0},
    {'e', 'e', 0x0301, 0},
    {'e', 0x0301, 'e', 0x0301, 0},
    {'e', 0x0301, 0x0300, 'e', 0x0301, 0},
    {'e', 0x0300, 'e', 0x0301, 0},
    {'e', 0x0300, 0x0301, 'e', 0x0301, 0},
    {'e', 'e', 0x0301, 0x0300, 0},
    {'e', 0x0301, 'e', 0x0301, 0x0300, 0},
    {'e', 0x0301, 0x0300, 'e', 0x0301, 0x0300, 0},
    {'e', 0x0300, 'e', 0x0301, 0x0300, 0},
    {'e', 0x0300, 0x0301, 'e', 0x0301, 0x0300, 0},
    {'e', 'e', 0x0300, 0},
    {'e', 0x0301, 'e', 0x0300, 0},
    {'e', 0x0301, 0x0300, 'e', 0x0300, 0},
    {'e', 0x0300, 'e', 0x0300, 0},
    {'e', 0x0300, 0x0301, 'e', 0x0300, 0},
    {'e', 'e', 0x0300, 0x0301, 0},
    {'e', 0x0301, 'e', 0x0300, 0x0301, 0},
    {'e', 0x0301, 0x0300, 'e', 0x0300, 0x0301, 0},
    {'e', 0x0300, 'e', 0x0300, 0x0301, 0},
    {'e', 0x0300, 0x0301, 'e', 0x0300, 0x0301, 0}
};

const UChar CollationFrenchTest::testBugs[][CollationFrenchTest::MAX_TOKEN_LEN] =
{
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

void CollationFrenchTest::doTest( UnicodeString source, UnicodeString target, Collator::EComparisonResult result)
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

void CollationFrenchTest::TestTertiary( char* par )
{
    int32_t i = 0;
    myCollation->setStrength(Collator::TERTIARY);
    for (i = 0; i < 12 ; i++)
    {
        doTest(testSourceCases[i], testTargetCases[i], results[i]);
    }
}

void CollationFrenchTest::TestSecondary( char* par )
{
    //test acute and grave ordering
    int32_t i = 0;
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

void CollationFrenchTest::TestExtra( char* par )
{
    int32_t i, j;
    myCollation->setStrength(Collator::TERTIARY);
    for (i = 0; i < 9 ; i++)
    {
        for (j = i + 1; j < 10; j += 1)
        {
            doTest(testBugs[i], testBugs[j], Collator::LESS);
        }
    }
}

void CollationFrenchTest::runIndexedTest( int32_t index, bool_t exec, char* &name, char* par )
{
    if (exec) logln("TestSuite CollationFrenchTest: ");
    switch (index) {
        case 0: name = "TestSecondary"; if (exec)   TestSecondary( par ); break;
        case 1: name = "TestTertiary";  if (exec)   TestTertiary( par ); break;
        case 2: name = "TestExtra";     if (exec)   TestExtra( par ); break;
        default: name = ""; break;
    }
}


