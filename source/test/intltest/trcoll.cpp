
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
    {'s', 0x0327, 0},
    {'v', 0x00E4, 't', 0},
    {'o', 'l', 'd', 0},
    {0x00FC, 'o', 'i', 'd', 0},
    {'h', 0x011E, 'a', 'l', 't', 0},
    {'s', 't', 'r', 'e', 's', 0x015E, 0},
    {'v', 'o', 0x0131, 'd', 0},
    {'i', 'd', 'e', 'a', 0},
    {0x00FC, 'o', 'i', 'd', 0},
    {'v', 'o', 0x0131, 'd', 0},
    {'i', 'd', 'e', 'a', 0}
};

const UChar CollationTurkishTest::testTargetCases[][CollationTurkishTest::MAX_TOKEN_LEN] = {
    {'u', 0x0308, 0},
    {'v', 'b', 't', 0},
    {0x00D6, 'a', 'y', 0},
    {'v', 'o', 'i', 'd', 0},
    {'h', 'a', 'l', 't', 0},
    {0x015E, 't', 'r', 'e', 0x015E, 's', 0},
    {'v', 'o', 'i', 'd', 0},
    {'I', 'd', 'e', 'a', 0},
    {'v', 'o', 'i', 'd', 0},
    {'v', 'o', 'i', 'd', 0},
    {'I', 'd', 'e', 'a', 0}
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


