
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

#ifndef _G7COLL
#include "g7coll.h"
#endif

const Locale G7CollationTest::locales[8] = {
    Locale("en", "US", ""),
    Locale("en", "GB", ""),
    Locale("en", "CA", ""),
    Locale("fr", "FR", ""),
    Locale("fr", "CA", ""),
    Locale("de", "DE", ""),
    Locale("it", "IT", ""),
    Locale("ja", "JP", "")
};

const UChar G7CollationTest::testCases[][G7CollationTest::MAX_TOKEN_LEN] = {
    { 'b', 'l', 'a', 'c', 'k', '-', 'b', 'i', 'r', 'd', 's', 0},   // 0
    { 'P', 'a', 't', 0},                                           // 1
    { 'p', 0x00E9, 'c', 'h', 0x00E9, 0},                           // 2
    { 'p', 0x00EA, 'c', 'h', 'e', 0},                              // 3
    { 'p', 0x00E9, 'c', 'h', 'e', 'r', 0},                         // 4
    { 'p', 0x00EA, 'c', 'h', 'e', 'r', 0},                         // 5
    { 'T', 'o', 'd', 0},                                           // 6
    { 'T', 0x00F6, 'n', 'e', 0},                                   // 7
    { 'T', 'o', 'f', 'u', 0},                                      // 8
    { 'b', 'l', 'a', 'c', 'k', 'b', 'i', 'r', 'd', 's', 0},        // 9
    { 'T', 'o', 'n', 0},                                           // 10
    { 'P', 'A', 'T', 0},                                           // 11
    { 'b', 'l', 'a', 'c', 'k', 'b', 'i', 'r', 'd', 0},             // 12
    { 'b', 'l', 'a', 'c', 'k', '-', 'b', 'i', 'r', 'd', 0},        // 13
    { 'p', 'a', 't', 0},                                           // 14
    // Additional tests
    { 'c', 'z', 'a', 'r', 0 },                                     // 15
    { 'c', 'h', 'u', 'r', 'o', 0 },                                // 16
    { 'c', 'a', 't', 0 },                                          // 17
    { 'd', 'a', 'r', 'n', 0 },                                     // 18
    { '?', 0 },                                                    // 19
    { 'q', 'u', 'i', 'c', 'k', 0 },                                // 20
    { '#', 0 },                                                    // 21
    { '&', 0 },                                                    // 22
    { 'a', 'a', 'r', 'd', 'v', 'a', 'r', 'k', 0},                  // 23
    { 'a', '-', 'r', 'd', 'v', 'a', 'r', 'k', 0},                  // 24
    { 'a', 'b', 'b', 'o', 't', 0},                                 // 25
    { 'c', 'o', 'o', 'p', 0},                                      // 26
    { 'c', 'o', '-', 'p', 0},                                      // 27
    { 'c', 'o', 'p', 0},                                           // 28 
    { 'z', 'e', 'b', 'r', 'a', 0}                                  // 29
};

const int32_t G7CollationTest::results[G7CollationTest::TESTLOCALES][G7CollationTest::TOTALTESTSET] = {
    { 12, 13, 9, 0, 14, 1, 11, 2, 3, 4, 5, 6, 8, 10, 7, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31 }, // en_US
    { 12, 13, 9, 0, 14, 1, 11, 2, 3, 4, 5, 6, 8, 10, 7, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31 }, // en_GB
    { 12, 13, 9, 0, 14, 1, 11, 2, 3, 4, 5, 6, 8, 10, 7, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31 }, // en_CA
    { 12, 13, 9, 0, 14, 1, 11, 3, 2, 4, 5, 6, 8, 10, 7, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31 }, // fr_FR
    { 12, 13, 9, 0, 14, 1, 11, 3, 2, 4, 5, 6, 8, 10, 7, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31 }, // fr_CA
    { 12, 13, 9, 0, 14, 1, 11, 2, 3, 4, 5, 6, 8, 10, 7, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31 }, // de_DE
    { 12, 13, 9, 0, 14, 1, 11, 2, 3, 4, 5, 6, 8, 10, 7, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31 }, // it_IT
    { 12, 13, 9, 0, 14, 1, 11, 2, 3, 4, 5, 6, 8, 10, 7, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31 }, // ja_JP
    // new table collation with rules "& Z < p, P"  loop to FIXEDTESTSET
    { 12, 13, 9, 0, 6, 8, 10, 7, 14, 1, 11, 2, 3, 4, 5, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31 },
    // new table collation with rules "& C < ch , cH, Ch, CH " // loop to TOTALTESTSET
    { 19, 22, 21, 23, 25, 24, 12, 13, 9, 0, 17, 26, 28, 27, 15, 16, 18, 14, 1, 11, 2, 3, 4, 5, 20, 6, 8, 10, 7, 29 },
    // new table collation with rules "& Question-mark ; ? & Hash-mark ; # & Ampersand ; '&'  " // loop to TOTALTESTSET
    { 23, 25, 22, 24, 12, 13, 9, 0, 17, 16, 26, 28, 27, 15, 18, 21, 14, 1, 11, 2, 3, 4, 5, 19, 20, 6, 8, 10, 7, 29 },
    // analogous to Japanese rules " & aa ; a- & ee ; e- & ii ; i- & oo ; o- & uu ; u- "  // loop to TOTALTESTSET
    { 19, 22, 21, 23, 24, 25, 12, 13, 9, 0, 17, 16, 26, 27, 28, 15, 18, 14, 1, 11, 2, 3, 4, 5, 20, 6, 8, 10, 7, 29 }
};


void G7CollationTest::doTest( Collator* myCollation, UnicodeString source, UnicodeString target, Collator::EComparisonResult result)
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

void G7CollationTest::TestG7Locales( char* par )
{
    int32_t i;

    for (i = 0; i < 8; i++)
    {
        Collator *myCollation= 0;
        UnicodeString dispName;
        UErrorCode status = U_ZERO_ERROR;
        RuleBasedCollator* tblColl1 = 0;

        myCollation = Collator::createInstance(locales[i], status);
        if (U_FAILURE(status))
        {
            UnicodeString msg;

            msg += "Locale ";
            msg += locales[i].getDisplayName(dispName);
            msg += "creation failed.";

            errln(msg);
            continue;
        }

        const UnicodeString& defRules = ((RuleBasedCollator*)myCollation)->getRules();
        status = U_ZERO_ERROR;
        tblColl1 = new RuleBasedCollator(((RuleBasedCollator*)myCollation)->getRules(), status);
        if (U_FAILURE(status))
        {
            UnicodeString msg, name;

            msg += "Recreate ";
            msg += locales[i].getDisplayName(name);
            msg += "collation failed.";

            errln(msg);
            continue;
        }

        UnicodeString msg;

        msg += "Locale ";
        msg += locales[i].getDisplayName(dispName);
        msg += "tests start :";
        logln(msg);

        int32_t j = 0, n = 0;
        for (j = 0; j < FIXEDTESTSET; j++)
        {
            for (n = j+1; n < FIXEDTESTSET; n++)
            {
                doTest(tblColl1, testCases[results[i][j]], testCases[results[i][n]], Collator::LESS);
            }
        }

        delete myCollation;
        delete tblColl1;
    }
}

void G7CollationTest::TestDemo1( char* par )
{
    logln("Demo Test 1 : Create a new table collation with rules \"& Z < p, P\"");
    UErrorCode status = U_ZERO_ERROR;
    Collator *col = Collator::createInstance(status);
    const UnicodeString baseRules = ((RuleBasedCollator*)col)->getRules();
    UnicodeString newRules(" & Z < p, P");
    newRules.insert(0, baseRules);
    RuleBasedCollator *myCollation = new RuleBasedCollator(newRules, status);

    if (U_FAILURE(status))
    {
        errln( "Demo Test 1 Table Collation object creation failed.");
        return;
    }

    int32_t j = 0, n = 0;
    for (j = 0; j < FIXEDTESTSET; j++)
    {
        for (n = j+1; n < FIXEDTESTSET; n++)
        {
            doTest(myCollation, testCases[results[8][j]], testCases[results[8][n]], Collator::LESS);
        }
    }

    delete myCollation; 
    delete col;
}

void G7CollationTest::TestDemo2( char* par )
{
    logln("Demo Test 2 : Create a new table collation with rules \"& C < ch , cH, Ch, CH\"");
    UErrorCode status = U_ZERO_ERROR;
    Collator *col = Collator::createInstance(status);
    const UnicodeString baseRules = ((RuleBasedCollator*)col)->getRules();
    UnicodeString newRules("& C < ch , cH, Ch, CH");
    newRules.insert(0, baseRules);
    RuleBasedCollator *myCollation = new RuleBasedCollator(newRules, status);

    if (U_FAILURE(status))
    {
        errln("Demo Test 2 Table Collation object creation failed.");
        return;
    }

    int32_t j = 0;
    for (j; j < TOTALTESTSET; j++)
    {
        for (int32_t n = j+1; n < TOTALTESTSET; n++)
        {
            doTest(myCollation, testCases[results[9][j]], testCases[results[9][n]], Collator::LESS);
        }
    }
    
    delete myCollation; 
    delete col;
}

void G7CollationTest::TestDemo3( char* par )
{
    logln("Demo Test 3 : Create a new table collation with rules \"& Question'-'mark ; '?' & Hash'-'mark ; '#' & Ampersand ; '&'\"");
    UErrorCode status = U_ZERO_ERROR;
    Collator *col = Collator::createInstance(status);
    const UnicodeString baseRules = ((RuleBasedCollator*)col)->getRules();
    UnicodeString newRules = "& Question'-'mark ; '?' & Hash'-'mark ; '#' & Ampersand ; '&'";
    newRules.insert(0, baseRules);
    RuleBasedCollator *myCollation = new RuleBasedCollator(newRules, status);

    if (U_FAILURE(status))
    {
        errln("Demo Test 3 Table Collation object creation failed.");
        return;
    }

    int32_t j = 0;
    for (j = 0; j < TOTALTESTSET; j++)
    {
        for (int32_t n = j+1; n < TOTALTESTSET; n++)
        {
            doTest(myCollation, testCases[results[10][j]], testCases[results[10][n]], Collator::LESS);
        }
    }
    
    delete myCollation; 
    delete col;
}

void G7CollationTest::TestDemo4( char* par )
{
    logln("Demo Test 4 : Create a new table collation with rules \" & aa ; a'-' & ee ; e'-' & ii ; i'-' & oo ; o'-' & uu ; u'-' \"");
    UErrorCode status = U_ZERO_ERROR;
    Collator *col = Collator::createInstance(status);
    const UnicodeString baseRules = ((RuleBasedCollator*)col)->getRules();
    UnicodeString newRules = " & aa ; a'-' & ee ; e'-' & ii ; i'-' & oo ; o'-' & uu ; u'-' ";
    newRules.insert(0, baseRules);
    RuleBasedCollator *myCollation = new RuleBasedCollator(newRules, status);

    if (U_FAILURE(status))
    {
        errln( "Demo Test 4 Table Collation object creation failed." );
        return;
    }

    int32_t j;
    for (j = 0; j < TOTALTESTSET; j++)
    {
        for (int32_t n = j+1; n < TOTALTESTSET; n++)
        {
            doTest(myCollation, testCases[results[11][j]], testCases[results[11][n]], Collator::LESS);
        }
    }

    delete myCollation; 
    delete col;
}

void G7CollationTest::runIndexedTest( int32_t index, bool_t exec, char* &name, char* par )
{
    if (exec) logln("TestSuite G7CollationTest: ");
    switch (index) {
        case 0: name = "TestG7Locales"; if (exec)   TestG7Locales( par ); break;
        case 1: name = "TestDemo1"; if (exec)   TestDemo1( par ); break;
        case 2: name = "TestDemo2"; if (exec)   TestDemo2( par ); break;
        case 3: name = "TestDemo3"; if (exec)   TestDemo3( par ); break;
        case 4: name = "TestDemo4"; if (exec)   TestDemo4( par ); break;
        default: name = ""; break;
    }
}


