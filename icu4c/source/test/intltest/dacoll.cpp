
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

#ifndef _DACOLL
#include "dacoll.h"
#endif

CollationDanishTest::CollationDanishTest()
: myCollation(0)
{
    UErrorCode status = U_ZERO_ERROR;
    myCollation = Collator::createInstance(Locale("da", "DK", ""),status);
}

CollationDanishTest::~CollationDanishTest()
{
    delete myCollation;
}

const UChar CollationDanishTest::testSourceCases[][CollationDanishTest::MAX_TOKEN_LEN] = {
    {'L', 'u', 'c', 0},
    {'l', 'u', 'c', 'k', 0},
    {'L', 0x00FC, 'b', 'e', 'c', 'k', 0},
    {'L', 0x00E4, 'v', 'i', 0},
    {'L', 0x00F6, 'w', 'w', 0},
    {'L', 'v', 'i', 0},
    {'L', 0x00E4, 'v', 'i', 0},
    {'L', 0x00FC, 'b', 'e', 'c', 'k', 0}
};

const UChar CollationDanishTest::testTargetCases[][CollationDanishTest::MAX_TOKEN_LEN] = {
    {'l', 'u', 'c', 'k', 0},
    {'L', 0x00FC, 'b', 'e', 'c', 'k', 0},
    {'l', 'y', 'b', 'e', 'c', 'k', 0},
    {'L', 0x00F6, 'w', 'e', 0},
    {'m', 'a', 's', 't', 0},
    {'L', 'w', 'i', 0},
    {'L', 0x00F6, 'w', 'i', 0},
    {'L', 'y', 'b', 'e', 'c', 'k', 0}
};

const Collator::EComparisonResult CollationDanishTest::results[] = {
    Collator::LESS,
    Collator::LESS,
    Collator::GREATER,
    Collator::LESS,
    Collator::LESS,
    // test primary > 5
    Collator::EQUAL,
    Collator::LESS,
    Collator::EQUAL
};

const UChar CollationDanishTest::testBugs[][CollationDanishTest::MAX_TOKEN_LEN] = {
    {'A', '/', 'S', 0},
    {'A', 'N', 'D', 'R', 'E', 0},
    {'A', 'N', 'D', 'R', 0x00C9, 0},
    {'A', 'N', 'D', 'R', 'E', 'A', 'S', 0},
    {'A', 'S', 0},
    {'C', 'A', 0},
    {0x00C7, 'A', 0},
    {'C', 'B', 0},
    {0x00C7, 'C', 0},
    {'D', '.', 'S', '.', 'B', '.', 0},
    {'D', 'A', 0},                                                                           // 10
    {'D', 'B', 0},
    {'D', 'S', 'B', 0},
    {'D', 'S', 'C', 0},
    {'E', 'K', 'S', 'T', 'R', 'A', '_', 'A', 'R', 'B', 'E', 'J', 'D', 'E', 0},
    {'E', 'K', 'S', 'T', 'R', 'A', 'B', 'U', 'D', 0},
    {'H', 0x00D8, 'S', 'T', 0},  // could the 0x00D8 be 0x2205?
    {'H', 'A', 'A', 'G', 0},                                                                 // 20
    {'H', 0x00C5, 'N', 'D', 'B', 'O', 'G', 0},
    {'H', 'A', 'A', 'N', 'D', 'V', 0x00C6, 'R', 'K', 'S', 'B', 'A', 'N', 'K', 'E', 'N', 0},
    {'k', 'a', 'r', 'l', 0},
    {'K', 'a', 'r', 'l', 0},
    {'N', 'I', 'E', 'L', 'S', 'E', 'N', 0},
    {'N', 'I', 'E', 'L', 'S', ' ', 'J', 0x00D8, 'R', 'G', 'E', 'N', 0},
    {'N', 'I', 'E', 'L', 'S', '-', 'J', 0x00D8, 'R', 'G', 'E', 'N', 0},
    {'R', 0x00C9, 'E', ',', ' ', 'A', 0},
    {'R', 'E', 'E', ',', ' ', 'B', 0},
    {'R', 0x00C9, 'E', ',', ' ', 'L', 0},                                                    // 30
    {'R', 'E', 'E', ',', ' ', 'V', 0},
    {'S', 'C', 'H', 'Y', 'T', 'T', ',', ' ', 'B', 0},
    {'S', 'C', 'H', 'Y', 'T', 'T', ',', ' ', 'H', 0},
    {'S', 'C', 'H', 0x00DC, 'T', 'T', ',', ' ', 'H', 0},
    {'S', 'C', 'H', 'Y', 'T', 'T', ',', ' ', 'L', 0},
    {'S', 'C', 'H', 0x00DC, 'T', 'T', ',', ' ', 'M', 0},
    {'S', 'S', 0},
    {0x00DF, 0},
    {'S', 'S', 'A', 0},
    {'S', 'T', 'O', 'R', 'E', 'K', 0x00C6, 'R', 0},
    {'S', 'T', 'O', 'R', 'E', ' ', 'V', 'I', 'L', 'D', 'M', 'O', 'S', 'E', 0},               // 40
    {'S', 'T', 'O', 'R', 'M', 'L', 'Y', 0},
    {'S', 'T', 'O', 'R', 'M', ' ', 'P', 'E', 'T', 'E', 'R', 'S', 'E', 'N', 0},
    {'T', 'H', 'O', 'R', 'V', 'A', 'L', 'D', 0},
    {'T', 'H', 'O', 'R', 'V', 'A', 'R', 'D', 'U', 'R', 0},
    {0x00FE, 'O', 'R', 'V', 'A', 'R', 0x0110, 'U', 'R', 0},
    {'T', 'H', 'Y', 'G', 'E', 'S', 'E', 'N', 0},
    {'V', 'E', 'S', 'T', 'E', 'R', 'G', 0x00C5, 'R', 'D', ',', ' ', 'A', 0},
    {'V', 'E', 'S', 'T', 'E', 'R', 'G', 'A', 'A', 'R', 'D', ',', ' ', 'A', 0},
    {'V', 'E', 'S', 'T', 'E', 'R', 'G', 0x00C5, 'R', 'D', ',', ' ', 'B', 0},                 // 50
    {0x00C6, 'B', 'L', 'E', 0},
    {0x00C4, 'B', 'L', 'E', 0},
    {0x00D8, 'B', 'E', 'R', 'G', 0},
    {0x00D6, 'B', 'E', 'R', 'G', 0},
    {0x0110, 'A', 0},
    {0x0110, 'C', 0}                                                                         // 54
};

const UChar CollationDanishTest::testNTList[][CollationDanishTest::MAX_TOKEN_LEN] = {
    {'a', 'n', 'd', 'e', 'r', 'e', 0},
    {'c', 'h', 'a', 'q', 'u', 'e', 0},
    {'c', 'h', 'e', 'm', 'i', 'n', 0},
    {'c', 'o', 't', 'e', 0},
    {'c', 'o', 't', 0x00e9, 0},
    {'c', 0x00f4, 't', 'e', 0},
    {'c', 0x00f4, 't', 0x00e9, 0},
    {0x010d, 'u', 0x010d, 0x0113, 't', 0},
    {'C', 'z', 'e', 'c', 'h', 0},
    {'h', 'i', 0x0161, 'a', 0},
    {'i', 'r', 'd', 'i', 's', 'c', 'h', 0},
    {'l', 'i', 'e', 0},
    {'l', 'i', 'r', 'e', 0},
    {'l', 'l', 'a', 'm', 'a', 0},
    {'l', 0x00f5, 'u', 'g', 0},
    {'l', 0x00f2, 'z', 'a', 0},
    {'l', 'u', 0x010d, 0},                                
    {'l', 'u', 'c', 'k', 0},
    {'L', 0x00fc, 'b', 'e', 'c', 'k', 0},
    {'l', 'y', 'e', 0},                               /* 20 */
    {'l', 0x00e4, 'v', 'i', 0},
    {'L', 0x00f6, 'w', 'e', 'n', 0},
    {'m', 0x00e0, 0x0161, 't', 'a', 0},
    {'m', 0x00ee, 'r', 0},
    {'m', 'y', 'n', 'd', 'i', 'g', 0},
    {'M', 0x00e4, 'n', 'n', 'e', 'r', 0},
    {'m', 0x00f6, 'c', 'h', 't', 'e', 'n', 0},
    {'p', 'i', 0x00f1, 'a', 0},
    {'p', 'i', 'n', 't', 0},
    {'p', 'y', 'l', 'o', 'n', 0},
    {0x0161, 0x00e0, 'r', 'a', 'n', 0},
    {'s', 'a', 'v', 'o', 'i', 'r', 0},
    {0x0160, 'e', 'r', 'b', 0x016b, 'r', 'a', 0},
    {'S', 'i', 'e', 't', 'l', 'a', 0},
    {0x015b, 'l', 'u', 'b', 0},
    {'s', 'u', 'b', 't', 'l', 'e', 0},
    {'s', 'y', 'm', 'b', 'o', 'l', 0},
    {'s', 0x00e4, 'm', 't', 'l', 'i', 'c', 'h', 0},
    {'w', 'a', 'f', 'f', 'l', 'e', 0},
    {'v', 'e', 'r', 'k', 'e', 'h', 'r', 't', 0},
    {'w', 'o', 'o', 'd', 0},
    {'v', 'o', 'x', 0},                                 /* 40 */
    {'v', 0x00e4, 'g', 'a', 0},
    {'y', 'e', 'n', 0},
    {'y', 'u', 'a', 'n', 0},
    {'y', 'u', 'c', 'c', 'a', 0},
    {0x017e, 'a', 'l', 0},
    {0x017e, 'e', 'n', 'a', 0},
    {0x017d, 'e', 'n', 0x0113, 'v', 'a', 0},
    {'z', 'o', 'o', 0},
    {'Z', 'v', 'i', 'e', 'd', 'r', 'i', 'j', 'a', 0},
    {'Z', 0x00fc, 'r', 'i', 'c', 'h', 0},
    {'z', 'y', 's', 'k', 0},             
    {0x00e4, 'n', 'd', 'e', 'r', 'e', 0}                  /* 53 */
};
void CollationDanishTest::doTest( UnicodeString source, UnicodeString target, Collator::EComparisonResult result)
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

void CollationDanishTest::TestTertiary( char* par )
{
    int32_t i = 0;
    myCollation->setStrength(Collator::TERTIARY);
    for (i = 0; i < 5 ; i++) {
        doTest(testSourceCases[i], testTargetCases[i], results[i]);
    }
    int32_t j = 0;
    logln("Test internet data list : ");
    for (i = 0; i < 53; i++) {
        for (j = i+1; j < 54; j++) {
            doTest(testBugs[i], testBugs[j], Collator::LESS);
        }
    }
    logln("Test NT data list : ");
    for (i = 0; i < 52; i++) {
        for (j = i+1; j < 53; j++) {
            doTest(testNTList[i], testNTList[j], Collator::LESS);
        }
    }
}
void CollationDanishTest::TestPrimary( char* par )
{
    int32_t i;
    myCollation->setStrength(Collator::PRIMARY);
    for (i = 5; i < 8; i++) {
        doTest(testSourceCases[i], testTargetCases[i], results[i]);
    }
}

void CollationDanishTest::runIndexedTest( int32_t index, bool_t exec, char* &name, char* par )
{
    if (exec) logln("TestSuite CollationDanishTest: ");
    switch (index) {
        case 0: name = "TestPrimary";   if (exec)   TestPrimary( par ); break;
        case 1: name = "TestTertiary";  if (exec)   TestTertiary( par ); break;
        default: name = ""; break;
    }
}


