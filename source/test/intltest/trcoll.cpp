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

#ifndef _TRCOLL
#include "trcoll.h"
#endif

CollationTurkishTest::CollationTurkishTest()
: myCollation(0)
{
    UErrorCode status = U_ZERO_ERROR;
    myCollation = Collator::createInstance(Locale("tr", "", ""),status);
}

CollationTurkishTest::~CollationTurkishTest()
{
    delete myCollation;
}

const UChar CollationTurkishTest::testSourceCases[][CollationTurkishTest::MAX_TOKEN_LEN] = {
    {0x73, 0x0327, 0},
    {0x76, 0x00E4, 0x74, 0},
    {0x6f, 0x6c, 0x64, 0},
    {0x00FC, 0x6f, 0x69, 0x64, 0},
    {0x68, 0x011E, 0x61, 0x6c, 0x74, 0},
    {0x73, 0x74, 0x72, 0x65, 0x73, 0x015E, 0},
    {0x76, 0x6f, 0x0131, 0x64, 0},
    {0x69, 0x64, 0x65, 0x61, 0},
    {0x00FC, 0x6f, 0x69, 0x64, 0},
    {0x76, 0x6f, 0x0131, 0x64, 0},
    {0x69, 0x64, 0x65, 0x61, 0}
};

const UChar CollationTurkishTest::testTargetCases[][CollationTurkishTest::MAX_TOKEN_LEN] = {
    {0x75, 0x0308, 0},
    {0x76, 0x62, 0x74, 0},
    {0x00D6, 0x61, 0x79, 0},
    {0x76, 0x6f, 0x69, 0x64, 0},
    {0x68, 0x61, 0x6c, 0x74, 0},
    {0x015E, 0x74, 0x72, 0x65, 0x015E, 0x73, 0},
    {0x76, 0x6f, 0x69, 0x64, 0},
    {0x49, 0x64, 0x65, 0x61, 0},
    {0x76, 0x6f, 0x69, 0x64, 0},
    {0x76, 0x6f, 0x69, 0x64, 0},
    {0x49, 0x64, 0x65, 0x61, 0}
};

const Collator::EComparisonResult CollationTurkishTest::results[] = {
    Collator::LESS,
    Collator::LESS,
    Collator::LESS,
    Collator::LESS,
    Collator::GREATER,
    Collator::LESS,
    Collator::LESS,
    Collator::GREATER,
    // test priamry > 8
    Collator::LESS,
    Collator::EQUAL,
    Collator::EQUAL
};

void CollationTurkishTest::doTest( UnicodeString source, UnicodeString target, Collator::EComparisonResult result)
{
    Collator::EComparisonResult compareResult = myCollation->compare(source, target);
    CollationKey sortKey1, sortKey2;
    UErrorCode key1status = U_ZERO_ERROR, key2status = U_ZERO_ERROR; //nos
    myCollation->getCollationKey(source, /*nos*/ sortKey1, key1status );
    myCollation->getCollationKey(target, /*nos*/ sortKey2, key2status );
    if (U_FAILURE(key1status) || U_FAILURE(key2status)) {
        errln("SortKey generation Failed.\n");
        return;
    }
    Collator::EComparisonResult keyResult = sortKey1.compareTo(sortKey2);
    reportCResult( source, target, sortKey1, sortKey2, compareResult, keyResult, result );
}

void CollationTurkishTest::TestTertiary( char* par )
{
    int32_t i = 0;
    myCollation->setStrength(Collator::TERTIARY);
    for (i = 0; i < 8 ; i++) {
        doTest(testSourceCases[i], testTargetCases[i], results[i]);
    }
}
void CollationTurkishTest::TestPrimary( char* par )
{
    int32_t i;
    myCollation->setStrength(Collator::PRIMARY);
    for (i = 8; i < 11; i++) {
        doTest(testSourceCases[i], testTargetCases[i], results[i]);
    }
}

void CollationTurkishTest::runIndexedTest( int32_t index, bool_t exec, char* &name, char* par )
{
    if (exec) logln("TestSuite CollationTurkishTest: ");
    switch (index) {
        case 0: name = "TestPrimary";   if (exec)   TestPrimary( par ); break;
        case 1: name = "TestTertiary";  if (exec)   TestTertiary( par ); break;
        default: name = ""; break;
    }
}


