
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

#ifndef _ESCOLL
#include "escoll.h"
#endif

CollationSpanishTest::CollationSpanishTest()
: myCollation(0)
{
    UErrorCode status = U_ZERO_ERROR;
    myCollation = Collator::createInstance(Locale("es", "ES", ""),status);
}

CollationSpanishTest::~CollationSpanishTest()
{
    delete myCollation;
}

const UChar CollationSpanishTest::testSourceCases[][CollationSpanishTest::MAX_TOKEN_LEN] = {
    {'a', 'l', 'i', 'a', 's', 0},
    {'E', 'l', 'l', 'i', 'o', 't', 0},
    {'H', 'e', 'l', 'l', 'o', 0},
    {'a', 'c', 'H', 'c', 0},
    {'a', 'c', 'c', 0},
    {'a', 'l', 'i', 'a', 's', 0},
    {'a', 'c', 'H', 'c', 0},
    {'a', 'c', 'c', 0},
    {'H', 'e', 'l', 'l', 'o', 0},
};

const UChar CollationSpanishTest::testTargetCases[][CollationSpanishTest::MAX_TOKEN_LEN] = {
    {'a', 'l', 'l', 'i', 'a', 's', 0},
    {'E', 'm', 'i', 'o', 't', 0},
    {'h', 'e', 'l', 'l', 'O', 0},
    {'a', 'C', 'H', 'c', 0},
    {'a', 'C', 'H', 'c', 0},
    {'a', 'l', 'l', 'i', 'a', 's', 0},
    {'a', 'C', 'H', 'c', 0},
    {'a', 'C', 'H', 'c', 0},
    {'h', 'e', 'l', 'l', 'O', 0},
};

const Collator::EComparisonResult CollationSpanishTest::results[] = {
    Collator::LESS,
    Collator::LESS,
    Collator::GREATER,
    Collator::LESS,
    Collator::LESS,
    // test primary > 5
    Collator::LESS,
    Collator::EQUAL,
    Collator::LESS,
    Collator::EQUAL
};

void CollationSpanishTest::doTest( UnicodeString source, UnicodeString target, Collator::EComparisonResult result)
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

void CollationSpanishTest::TestTertiary( char* par )
{
    int32_t i = 0;
    myCollation->setStrength(Collator::TERTIARY);
    for (i = 0; i < 5 ; i++) {
        doTest(testSourceCases[i], testTargetCases[i], results[i]);
    }
}
void CollationSpanishTest::TestPrimary( char* par )
{
    int32_t i;
    myCollation->setStrength(Collator::PRIMARY);
    for (i = 5; i < 9; i++) {
        doTest(testSourceCases[i], testTargetCases[i], results[i]);
    }
}

void CollationSpanishTest::runIndexedTest( int32_t index, bool_t exec, char* &name, char* par )
{
    if (exec) logln("TestSuite CollationSpanishTest: ");
    switch (index) {
        case 0: name = "TestPrimary";   if (exec)   TestPrimary( par ); break;
        case 1: name = "TestTertiary";  if (exec)   TestTertiary( par ); break;
        default: name = ""; break;
    }
}


