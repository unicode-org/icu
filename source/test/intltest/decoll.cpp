
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
#include "decoll.h"

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

#ifndef _DECOLL
#include "decoll.h"
#endif

CollationGermanTest::CollationGermanTest()
: myCollation(0)
{
    UErrorCode status = U_ZERO_ERROR;
    myCollation = Collator::createInstance(Locale::GERMANY, status);
}

CollationGermanTest::~CollationGermanTest()
{
    delete myCollation;
}

const UChar CollationGermanTest::testSourceCases[][CollationGermanTest::MAX_TOKEN_LEN] =
{
    {'G', 'r', 0x00F6, 0x00DF, 'e', 0},
    {'a', 'b', 'c', 0},
    {'T', 0x00F6, 'n', 'e', 0},
    {'T', 0x00F6, 'n', 'e', 0},
    {'T', 0x00F6, 'n', 'e', 0},
    {'a', 0x0308, 'b', 'c', 0},
    {0x00E4, 'b', 'c', 0},
    {0x00E4, 'b', 'c', 0},
    {'S', 't', 'r', 'a', 0x00DF, 'e', 0},
    {'e', 'f', 'g', 0},
    {0x00E4, 'b', 'c', 0},
    {'S', 't', 'r', 'a', 0x00DF, 'e', 0}
};

const UChar CollationGermanTest::testTargetCases[][CollationGermanTest::MAX_TOKEN_LEN] =
{
    {'G', 'r', 'o', 's', 's', 'i', 's', 't', 0},
    {'a', 0x0308, 'b', 'c', 0},
    {'T', 'o', 'n', 0},
    {'T', 'o', 'd', 0},
    {'T', 'o', 'f', 'u', 0},
    {'A', 0x0308, 'b', 'c', 0},
    {'a', 0x0308, 'b', 'c', 0},
    {'a', 'e', 'b', 'c', 0},
    {'S', 't', 'r', 'a', 's', 's', 'e', 0},
    {'e', 'f', 'g', 0},
    {'a', 'e', 'b', 'c', 0},
    {'S', 't', 'r', 'a', 's', 's', 'e', 0}
};

const Collator::EComparisonResult CollationGermanTest::results[][2] =
{
      //  Primary                Tertiary
        { Collator::LESS,        Collator::LESS },
        { Collator::EQUAL,        Collator::LESS },
        { Collator::GREATER,    Collator::GREATER },
        { Collator::GREATER,    Collator::GREATER },
        { Collator::GREATER,    Collator::GREATER },
        { Collator::EQUAL,        Collator::LESS },
        { Collator::EQUAL,        Collator::EQUAL },
        { Collator::LESS,        Collator::LESS },
        { Collator::EQUAL,        Collator::GREATER },
        { Collator::EQUAL,        Collator::EQUAL },
        { Collator::LESS,        Collator::LESS },
        { Collator::EQUAL,        Collator::GREATER }
};

void CollationGermanTest::doTest( UnicodeString source, UnicodeString target, Collator::EComparisonResult result)
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

void CollationGermanTest::TestTertiary( char* par )
{
    int32_t i = 0;
    myCollation->setStrength(Collator::TERTIARY);
    for (i = 0; i < 12 ; i++)
    {
        doTest(testSourceCases[i], testTargetCases[i], results[i][1]);
    }
}
void CollationGermanTest::TestPrimary( char* par )
{
    int32_t i;
    myCollation->setStrength(Collator::PRIMARY);
    for (i = 0; i < 12 ; i++)
    {
        doTest(testSourceCases[i], testTargetCases[i], results[i][0]);
    }
}

void CollationGermanTest::runIndexedTest( int32_t index, bool_t exec, char* &name, char* par )
{
    if (exec) logln("TestSuite CollationGermanTest: ");
    switch (index)
    {
        case 0: name = "TestPrimary";   if (exec)   TestPrimary( par ); break;
        case 1: name = "TestTertiary";  if (exec)   TestTertiary( par ); break;
        default: name = ""; break;
    }
}


