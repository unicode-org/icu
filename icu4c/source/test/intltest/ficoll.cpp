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

#ifndef _FICOLL
#include "ficoll.h"
#endif

CollationFinnishTest::CollationFinnishTest()
: myCollation(0)
{
    UErrorCode status = U_ZERO_ERROR;
    myCollation = Collator::createInstance(Locale("fi", "FI", ""),status);
}

CollationFinnishTest::~CollationFinnishTest()
{
    delete myCollation;
}

const UChar CollationFinnishTest::testSourceCases[][CollationFinnishTest::MAX_TOKEN_LEN] = {
    {0x77, 0x61, 0x74, 0},
    {0x76, 0x61, 0x74, 0},
    {0x61, 0x00FC, 0x62, 0x65, 0x63, 0x6b, 0},
    {0x4c, 0x00E5, 0x76, 0x69, 0},
    {0x77, 0x61, 0x74, 0}
};

const UChar CollationFinnishTest::testTargetCases[][CollationFinnishTest::MAX_TOKEN_LEN] = {
    {0x76, 0x61, 0x74, 0},
    {0x77, 0x61, 0x79, 0},
    {0x61, 0x78, 0x62, 0x65, 0x63, 0x6b, 0},
    {0x4c, 0x00E4, 0x77, 0x65, 0},
    {0x76, 0x61, 0x74, 0}
};

const Collator::EComparisonResult CollationFinnishTest::results[] = {
    Collator::GREATER,
    Collator::LESS,
    Collator::GREATER,
    Collator::LESS,
    // test primary > 4
    Collator::EQUAL,
};

void CollationFinnishTest::doTest( UnicodeString source, UnicodeString target, Collator::EComparisonResult result)
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

void CollationFinnishTest::TestTertiary( char* par )
{
    int32_t i = 0;
    myCollation->setStrength(Collator::TERTIARY);
    for (i = 0; i < 4 ; i++) {
        doTest(testSourceCases[i], testTargetCases[i], results[i]);
    }
}
void CollationFinnishTest::TestPrimary( char* par )
{
    int32_t i;
    myCollation->setStrength(Collator::PRIMARY);
    for (i = 4; i < 5; i++) {
        doTest(testSourceCases[i], testTargetCases[i], results[i]);
    }
}

void CollationFinnishTest::runIndexedTest( int32_t index, UBool exec, char* &name, char* par )
{
    if (exec) logln("TestSuite CollationFinnishTest: ");
    switch (index) {
        case 0: name = "TestPrimary";   if (exec)   TestPrimary( par ); break;
        case 1: name = "TestTertiary";  if (exec)   TestTertiary( par ); break;
        default: name = ""; break;
    }
}


