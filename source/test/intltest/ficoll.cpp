
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

#ifndef _FICOLL
#include "ficoll.h"
#endif

CollationFinnishTest::CollationFinnishTest()
: myCollation(0)
{
    UErrorCode status = ZERO_ERROR;
    myCollation = Collator::createInstance(Locale("fi", "FI", ""),status);
}

CollationFinnishTest::~CollationFinnishTest()
{
    delete myCollation;
}

const UChar CollationFinnishTest::testSourceCases[][CollationFinnishTest::MAX_TOKEN_LEN] = {
    {'w', 'a', 't', 0},
    {'v', 'a', 't', 0},
    {'a', 0x00FC, 'b', 'e', 'c', 'k', 0},
    {'L', 0x00E5, 'v', 'i', 0},
    {'w', 'a', 't', 0}
};

const UChar CollationFinnishTest::testTargetCases[][CollationFinnishTest::MAX_TOKEN_LEN] = {
    {'v', 'a', 't', 0},
    {'w', 'a', 'y', 0},
    {'a', 'x', 'b', 'e', 'c', 'k', 0},
    {'L', 0x00E4, 'w', 'e', 0},
    {'v', 'a', 't', 0}
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
    UErrorCode key1status = ZERO_ERROR, key2status = ZERO_ERROR; //nos
    myCollation->getCollationKey(source, /*nos*/ sortKey1, key1status );
    myCollation->getCollationKey(target, /*nos*/ sortKey2, key2status );
    if (FAILURE(key1status) || FAILURE(key2status)) {
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

void CollationFinnishTest::runIndexedTest( int32_t index, bool_t exec, char* &name, char* par )
{
    if (exec) logln("TestSuite CollationFinnishTest: ");
    switch (index) {
        case 0: name = "TestPrimary";   if (exec)   TestPrimary( par ); break;
        case 1: name = "TestTertiary";  if (exec)   TestTertiary( par ); break;
        default: name = ""; break;
    }
}


