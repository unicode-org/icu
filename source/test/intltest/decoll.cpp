/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
#include "decoll.h"

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
    {0x47, 0x72, 0x00F6, 0x00DF, 0x65, 0},
    {0x61, 0x62, 0x63, 0},
    {0x54, 0x00F6, 0x6e, 0x65, 0},
    {0x54, 0x00F6, 0x6e, 0x65, 0},
    {0x54, 0x00F6, 0x6e, 0x65, 0},
    {0x61, 0x0308, 0x62, 0x63, 0},
    {0x00E4, 0x62, 0x63, 0},
    {0x00E4, 0x62, 0x63, 0},
    {0x53, 0x74, 0x72, 0x61, 0x00DF, 0x65, 0},
    {0x65, 0x66, 0x67, 0},
    {0x00E4, 0x62, 0x63, 0},
    {0x53, 0x74, 0x72, 0x61, 0x00DF, 0x65, 0}
};

const UChar CollationGermanTest::testTargetCases[][CollationGermanTest::MAX_TOKEN_LEN] =
{
    {0x47, 0x72, 0x6f, 0x73, 0x73, 0x69, 0x73, 0x74, 0},
    {0x61, 0x0308, 0x62, 0x63, 0},
    {0x54, 0x6f, 0x6e, 0},
    {0x54, 0x6f, 0x64, 0},
    {0x54, 0x6f, 0x66, 0x75, 0},
    {0x41, 0x0308, 0x62, 0x63, 0},
    {0x61, 0x0308, 0x62, 0x63, 0},
    {0x61, 0x65, 0x62, 0x63, 0},
    {0x53, 0x74, 0x72, 0x61, 0x73, 0x73, 0x65, 0},
    {0x65, 0x66, 0x67, 0},
    {0x61, 0x65, 0x62, 0x63, 0},
    {0x53, 0x74, 0x72, 0x61, 0x73, 0x73, 0x65, 0}
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


