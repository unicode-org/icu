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
        {0x61, 0x62, 0},
        {0x62, 0x6c, 0x61, 0x63, 0x6b, 0x2d, 0x62, 0x69, 0x72, 0x64, 0},
        {0x62, 0x6c, 0x61, 0x63, 0x6b, 0x20, 0x62, 0x69, 0x72, 0x64, 0},
        {0x62, 0x6c, 0x61, 0x63, 0x6b, 0x2d, 0x62, 0x69, 0x72, 0x64, 0},
        {0x48, 0x65, 0x6c, 0x6c, 0x6f, 0},
        {0x41, 0x42, 0x43, 0}, 
        {0x61, 0x62, 0x63, 0},
        {0x62, 0x6c, 0x61, 0x63, 0x6b, 0x62, 0x69, 0x72, 0x64, 0},
        {0x62, 0x6c, 0x61, 0x63, 0x6b, 0x2d, 0x62, 0x69, 0x72, 0x64, 0},
        {0x62, 0x6c, 0x61, 0x63, 0x6b, 0x2d, 0x62, 0x69, 0x72, 0x64, 0},
        {0x70, 0x00EA, 0x63, 0x68, 0x65, 0},                                            // 10
        {0x70, 0x00E9, 0x63, 0x68, 0x00E9, 0},
        {0x00C4, 0x42, 0x0308, 0x43, 0x0308, 0},
        {0x61, 0x0308, 0x62, 0x63, 0},
        {0x70, 0x00E9, 0x63, 0x68, 0x65, 0x72, 0},
        {0x72, 0x6f, 0x6c, 0x65, 0x73, 0},
        {0x61, 0x62, 0x63, 0},
        {0x41, 0},
        {0x41, 0},
        {0x61, 0x62, 0},                                                                // 20
        {0x74, 0x63, 0x6f, 0x6d, 0x70, 0x61, 0x72, 0x65, 0x70, 0x6c, 0x61, 0x69, 0x6e, 0},
        {0x61, 0x62, 0}, 
        {0x61, 0x23, 0x62, 0},
        {0x61, 0x23, 0x62, 0},
        {0x61, 0x62, 0x63, 0},
        {0x41, 0x62, 0x63, 0x64, 0x61, 0},
        {0x61, 0x62, 0x63, 0x64, 0x61, 0},
        {0x61, 0x62, 0x63, 0x64, 0x61, 0},
        {0x00E6, 0x62, 0x63, 0x64, 0x61, 0},
        {0x00E4, 0x62, 0x63, 0x64, 0x61, 0},                                            // 30
        {0x61, 0x62, 0x63, 0},
        {0x61, 0x62, 0x63, 0},
        {0x61, 0x62, 0x63, 0},
        {0x61, 0x62, 0x63, 0},
        {0x61, 0x62, 0x63, 0},
        {0x61, 0x63, 0x48, 0x63, 0},
        {0x61, 0x0308, 0x62, 0x63, 0},
        {0x74, 0x68, 0x69, 0x0302, 0x73, 0},
        {0x70, 0x00EA, 0x63, 0x68, 0x65},
        {0x61, 0x62, 0x63, 0},                                                         // 40
        {0x61, 0x62, 0x63, 0},
        {0x61, 0x62, 0x63, 0},
        {0x61, 0x00E6, 0x63, 0},
        {0x61, 0x62, 0x63, 0},
        {0x61, 0x62, 0x63, 0},
        {0x61, 0x00E6, 0x63, 0},
        {0x61, 0x62, 0x63, 0},
        {0x61, 0x62, 0x63, 0},               
        {0x70, 0x00E9, 0x63, 0x68, 0x00E9, 0}                                            // 49
};

const UChar CollationEnglishTest::testTargetCases[][CollationEnglishTest::MAX_TOKEN_LEN] = {
        {0x61, 0x62, 0x63, 0},
        {0x62, 0x6c, 0x61, 0x63, 0x6b, 0x62, 0x69, 0x72, 0x64, 0},
        {0x62, 0x6c, 0x61, 0x63, 0x6b, 0x2d, 0x62, 0x69, 0x72, 0x64, 0},
        {0x62, 0x6c, 0x61, 0x63, 0x6b, 0},
        {0x68, 0x65, 0x6c, 0x6c, 0x6f, 0},
        {0x41, 0x42, 0x43, 0},
        {0x41, 0x42, 0x43, 0},
        {0x62, 0x6c, 0x61, 0x63, 0x6b, 0x62, 0x69, 0x72, 0x64, 0x73, 0},
        {0x62, 0x6c, 0x61, 0x63, 0x6b, 0x62, 0x69, 0x72, 0x64, 0x73, 0},
        {0x62, 0x6c, 0x61, 0x63, 0x6b, 0x62, 0x69, 0x72, 0x64, 0},                             // 10
        {0x70, 0x00E9, 0x63, 0x68, 0x00E9, 0},
        {0x70, 0x00E9, 0x63, 0x68, 0x65, 0x72, 0},
        {0x00C4, 0x42, 0x0308, 0x43, 0x0308, 0},
        {0x41, 0x0308, 0x62, 0x63, 0},
        {0x70, 0x00E9, 0x63, 0x68, 0x65, 0},
        {0x72, 0x6f, 0x0302, 0x6c, 0x65, 0},
        {0x41, 0x00E1, 0x63, 0x64, 0},
        {0x41, 0x00E1, 0x63, 0x64, 0},
        {0x61, 0x62, 0x63, 0},
        {0x61, 0x62, 0x63, 0},                                                             // 20
        {0x54, 0x43, 0x6f, 0x6d, 0x70, 0x61, 0x72, 0x65, 0x50, 0x6c, 0x61, 0x69, 0x6e, 0},
        {0x61, 0x42, 0x63, 0},
        {0x61, 0x23, 0x42, 0},
        {0x61, 0x26, 0x62, 0},
        {0x61, 0x23, 0x63, 0},
        {0x61, 0x62, 0x63, 0x64, 0x61, 0},
        {0x00C4, 0x62, 0x63, 0x64, 0x61, 0},
        {0x00E4, 0x62, 0x63, 0x64, 0x61, 0},
        {0x00C4, 0x62, 0x63, 0x64, 0x61, 0},
        {0x00C4, 0x62, 0x63, 0x64, 0x61, 0},                                             // 30
        {0x61, 0x62, 0x23, 0x63, 0},
        {0x61, 0x62, 0x63, 0},
        {0x61, 0x62, 0x3d, 0x63, 0},
        {0x61, 0x62, 0x64, 0},
        {0x00E4, 0x62, 0x63, 0},
        {0x61, 0x43, 0x48, 0x63, 0},
        {0x00E4, 0x62, 0x63, 0},
        {0x74, 0x68, 0x00EE, 0x73, 0},
        {0x70, 0x00E9, 0x63, 0x68, 0x00E9, 0},
        {0x61, 0x42, 0x43, 0},                                                          // 40
        {0x61, 0x62, 0x64, 0},
        {0x00E4, 0x62, 0x63, 0},
        {0x61, 0x00C6, 0x63, 0},
        {0x61, 0x42, 0x64, 0},
        {0x00E4, 0x62, 0x63, 0},
        {0x61, 0x00C6, 0x63, 0},
        {0x61, 0x42, 0x64, 0},
        {0x00E4, 0x62, 0x63, 0},          
        {0x70, 0x00EA, 0x63, 0x68, 0x65, 0}                                                 // 49
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
    {0x61, 0},
    {0x41, 0},
    {0x65, 0},
    {0x45, 0},
    {0x00e9, 0},
    {0x00e8, 0},
    {0x00ea, 0},
    {0x00eb, 0},
    {0x65, 0x61, 0},
    {0x78, 0}
};

// 0x0300 is grave, 0x0301 is acute
// the order of elements in this array must be different than the order in CollationFrenchTest
const UChar CollationEnglishTest::testAcute[][CollationEnglishTest::MAX_TOKEN_LEN] = {
    {0x65, 0x65, 0},
    {0x65, 0x65, 0x0301, 0},
    {0x65, 0x65, 0x0301, 0x0300, 0},
    {0x65, 0x65, 0x0300, 0},
    {0x65, 0x65, 0x0300, 0x0301, 0},
    {0x65, 0x0301, 0x65, 0},
    {0x65, 0x0301, 0x65, 0x0301, 0},
    {0x65, 0x0301, 0x65, 0x0301, 0x0300, 0},
    {0x65, 0x0301, 0x65, 0x0300, 0},
    {0x65, 0x0301, 0x65, 0x0300, 0x0301, 0},
    {0x65, 0x0301, 0x0300, 0x65, 0},
    {0x65, 0x0301, 0x0300, 0x65, 0x0301, 0},
    {0x65, 0x0301, 0x0300, 0x65, 0x0301, 0x0300, 0},
    {0x65, 0x0301, 0x0300, 0x65, 0x0300, 0},
    {0x65, 0x0301, 0x0300, 0x65, 0x0300, 0x0301, 0},
    {0x65, 0x0300, 0x65, 0},
    {0x65, 0x0300, 0x65, 0x0301, 0},
    {0x65, 0x0300, 0x65, 0x0301, 0x0300, 0},
    {0x65, 0x0300, 0x65, 0x0300, 0},
    {0x65, 0x0300, 0x65, 0x0300, 0x0301, 0},
    {0x65, 0x0300, 0x0301, 0x65, 0},
    {0x65, 0x0300, 0x0301, 0x65, 0x0301, 0},
    {0x65, 0x0300, 0x0301, 0x65, 0x0301, 0x0300, 0},
    {0x65, 0x0300, 0x0301, 0x65, 0x0300, 0},
    {0x65, 0x0300, 0x0301, 0x65, 0x0300, 0x0301, 0}
};

static const UChar testMore[][CollationEnglishTest::MAX_TOKEN_LEN] = {
    {0x61, 0x65, 0},
    { 0x00E6, 0},
    { 0x00C6, 0},
    {0x61, 0x66, 0},
    {0x6f, 0x65, 0},
    { 0x0153, 0},
    { 0x0152, 0},
    {0x6f, 0x66, 0},
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

void CollationEnglishTest::TestTertiary(/* char* par */)
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
            if (i <  j)
                expected = Collator::LESS;
            else if (i == j)
                expected = Collator::EQUAL;
            else // (i >  j)
                expected = Collator::GREATER;
            doTest(testMore[i], testMore[j], expected );
        }
    }

}

void CollationEnglishTest::TestPrimary(/* char* par */)
{
    int32_t i;
    myCollation->setStrength(Collator::PRIMARY);
    for (i = 38; i < 43 ; i++)
    {
        doTest(testSourceCases[i], testTargetCases[i], results[i]);
    }
}

void CollationEnglishTest::TestSecondary(/* char* par */)
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
            if (i <  j)
                expected = Collator::LESS;
            else if (i == j)
                expected = Collator::EQUAL;
            else // (i >  j)
                expected = Collator::GREATER;
            doTest(testAcute[i], testAcute[j], expected );
        }
    }
}

void CollationEnglishTest::runIndexedTest( int32_t index, UBool exec, const char* &name, char* /*par*/ )
{
    if (exec) logln("TestSuite CollationEnglishTest: ");
    switch (index) {
        case 0: name = "TestPrimary";   if (exec)   TestPrimary(/* par */); break;
        case 1: name = "TestSecondary"; if (exec)   TestSecondary(/* par */); break;
        case 2: name = "TestTertiary";  if (exec)   TestTertiary(/* par */); break;
        default: name = ""; break;
    }
}


