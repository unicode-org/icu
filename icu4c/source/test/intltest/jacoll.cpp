
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

#ifndef _JACOLL
#include "jacoll.h"
#endif

CollationKanaTest::CollationKanaTest()
: myCollation(0)
{
    UErrorCode status = U_ZERO_ERROR;
    myCollation = Collator::createInstance(Locale::JAPAN, status);
    myCollation->setDecomposition(Normalizer::DECOMP);
}

CollationKanaTest::~CollationKanaTest()
{
    delete myCollation;
}

const UChar CollationKanaTest::testSourceCases[][CollationKanaTest::MAX_TOKEN_LEN] = {
    {'A', 0x0300, 0x0301, 0},
    {'A', 0x0300, 0x0316, 0},
    {'A', 0x0300, 0},
    {0x00C0, 0x0301, 0},
    {0x00C0, 0x0316, 0},
    {0xff9E, 0},
    {0x3042, 0},
    {0x30A2, 0},
    {0x3042, 0x3042, 0},
    {0x30A2, 0x30FC, 0},
    {0x30A2, 0x30FC, 0x30C8, 0}                               // 11
};

const UChar CollationKanaTest::testTargetCases[][CollationKanaTest::MAX_TOKEN_LEN] = {
    {'A', 0x0301, 0x0300, 0},
    {'A', 0x0316, 0x0300, 0},
    {0x00C0, 0},
    {'A', 0x0301, 0x0300, 0},
    {'A', 0x0316, 0x0300, 0},
    {0xFF9F, 0},
    {0x30A2, 0},
    {0x3042, 0x3042, 0},
    {0x30A2, 0x30FC, 0},
    {0x30A2, 0x30FC, 0x30C8, 0},
    {0x3042, 0x3042, 0x3089, 0}                               // 11
};

const Collator::EComparisonResult CollationKanaTest::results[] = {
    Collator::GREATER,
    Collator::EQUAL,
    Collator::EQUAL,
    Collator::GREATER,
    Collator::EQUAL,
    Collator::LESS,
    Collator::LESS,
    Collator::LESS,
    Collator::LESS,
    Collator::LESS,
    Collator::LESS                                          // 11
};

void CollationKanaTest::doTest( UnicodeString source, UnicodeString target, Collator::EComparisonResult result)
{
    Collator::EComparisonResult compareResult = myCollation->compare(source, target);
    CollationKey sortKey1, sortKey2;
    UErrorCode key1status = U_ZERO_ERROR, key2status = U_ZERO_ERROR; //nos
    myCollation->getCollationKey(source, /*nos*/ sortKey1, key1status );
    myCollation->getCollationKey(target, /*nos*/ sortKey2, key2status );
    if (FAILURE(key1status) || FAILURE(key2status)) {
        errln("SortKey generation Failed.\n");
        return;
    }
    Collator::EComparisonResult keyResult = sortKey1.compareTo(sortKey2);
    reportCResult( source, target, sortKey1, sortKey2, compareResult, keyResult, result );
}

void CollationKanaTest::TestTertiary( char* par )
{
    int32_t i = 0;
    myCollation->setStrength(Collator::TERTIARY);
    for (i = 0; i < 11; i++) {
        doTest(testSourceCases[i], testTargetCases[i], results[i]);
    }
}

void CollationKanaTest::runIndexedTest( int32_t index, bool_t exec, char* &name, char* par )
{
    if (exec) logln("TestSuite CollationKanaTest: ");
    switch (index) {
        case 0: name = "TestTertiary";  if (exec)   TestTertiary( par ); break;
        default: name = ""; break;
    }
}


