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

#ifndef _CURRCOLL
#include "currcoll.h"
#endif

#define ARRAY_LENGTH(array) (sizeof array / sizeof array[0])

CollationCurrencyTest::CollationCurrencyTest()
{
}

CollationCurrencyTest::~CollationCurrencyTest()
{
}

void CollationCurrencyTest::currencyTest(char *par)
{
    // All the currency symbols, in collation order
    static const UChar currency[] =
    {
        0x00a4, // generic currency
        0x0e3f, // baht
        0x00a2, // cent
        0x20a1, // colon
        0x20a2, // cruzeiro
        0x0024, // dollar
        0x20ab, // dong
        0x20ac, // euro
        0x20a3, // franc
        0x20a4, // lira
        0x20a5, // mill
        0x20a6, // naira
        0x20a7, // peseta
        0x00a3, // pound
        0x20a8, // rupee
        0x20aa, // shekel
        0x20a9, // won
        0x00a5  // yen
    };
    

    int32_t i, j;
    UErrorCode status = U_ZERO_ERROR;
    Collator::EComparisonResult expectedResult = Collator::EQUAL;
    RuleBasedCollator *c = (RuleBasedCollator *)Collator::createInstance(status);

    if (U_FAILURE(status))
    {
        errln ("Collator::createInstance() failed!");
        return;
    }

    // Compare each currency symbol against all the
    // currency symbols, including itself
    for (i = 0; i < ARRAY_LENGTH(currency); i += 1)
    {
        for (j = 0; j < ARRAY_LENGTH(currency); j += 1)
        {
            UnicodeString source(&currency[i], 1);
            UnicodeString target(&currency[j], 1);

            if (i < j)
            {
                expectedResult = Collator::LESS;
            }
            else if ( i == j)
            {
                expectedResult = Collator::EQUAL;
            }
            else
            {
                expectedResult = Collator::GREATER;
            }

            Collator::EComparisonResult compareResult = c->compare(source, target);

            CollationKey sourceKey, targetKey;
            UErrorCode status = U_ZERO_ERROR;

            c->getCollationKey(source, sourceKey, status);

            if (U_FAILURE(status))
            {
                errln("Couldn't get collationKey for source");
                continue;
            }

            c->getCollationKey(target, targetKey, status);

            if (U_FAILURE(status))
            {
                errln("Couldn't get collationKey for target");
                continue;
            }

            Collator::EComparisonResult keyResult = sourceKey.compareTo(targetKey);

            reportCResult(source, target, sourceKey, targetKey,
                          compareResult, keyResult, expectedResult);

        }
    }
}

void CollationCurrencyTest::runIndexedTest(int32_t index, bool_t exec, char* &name, char* par)
{
    if (exec)
    {
        logln("Collation Currency Tests: ");
    }

    switch (index)
    {
        case  0: name = "currencyTest"; if (exec) currencyTest(par); break;
        default: name = ""; break;
    }
}
