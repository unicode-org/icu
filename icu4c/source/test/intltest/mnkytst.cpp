
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

#ifndef _MNKYTST
#include "mnkytst.h"
#endif


#include <stdlib.h>
#include <stdio.h>
#include <time.h>

#ifndef MIN
#define MIN(x,y) ((x) < (y) ? (x) : (y))
#endif

#ifndef MAX
#define MAX(x,y) ((x) > (y) ? (x) : (y))
#endif

const UnicodeString CollationMonkeyTest::source("-abcdefghijklmnopqrstuvwxyz#&^$@");

CollationMonkeyTest::CollationMonkeyTest()
: myCollator(0)
{
    UErrorCode status = U_ZERO_ERROR;
    myCollator = Collator::createInstance(status);
}

CollationMonkeyTest::~CollationMonkeyTest()
{
    delete myCollator;
}


void 
CollationMonkeyTest::report(UnicodeString& s, UnicodeString& t, int32_t result, int32_t revResult)
{
    if (revResult != -result)
    {
        UnicodeString msg;

        msg += s; 
        msg += " and ";
        msg += t;
        msg += " round trip comparison failed";
        msg += (UnicodeString) " (result " + result + ", reverse Result " + revResult + ")"; 

        errln(msg);
    }
}

int32_t 
CollationMonkeyTest::checkValue(int32_t value)
{
    if (value < 0)
    {
        return -value;
    }

    return value;
}

void CollationMonkeyTest::TestCollationKey( char* par )
{
    if(source.length() == 0) {
        errln(UNICODE_STRING("CollationMonkeyTest::TestCollationKey(): source is empty - ICU_DATA not set or data missing?", 92));
        return;
    }

    srand( (unsigned)time( NULL ) );
    int32_t s = checkValue(rand() % source.length());
    int32_t t = checkValue(rand() % source.length());
    int32_t slen = checkValue((rand() - source.length()) % source.length());
    int32_t tlen = checkValue((rand() - source.length()) % source.length());
    UnicodeString subs, subt;

    source.extract(MIN(s, slen), MAX(s, slen), subs);
    source.extract(MIN(t, tlen), MAX(t, tlen), subt);

    CollationKey collationKey1, collationKey2;
    UErrorCode status1 = U_ZERO_ERROR, status2= U_ZERO_ERROR;

    myCollator->setStrength(Collator::TERTIARY);
    myCollator->getCollationKey(subs, collationKey1, status1);
    myCollator->getCollationKey(subt, collationKey2, status2);
    int32_t result = collationKey1.compareTo(collationKey2);  // Tertiary
    int32_t revResult = collationKey2.compareTo(collationKey1);  // Tertiary
    report( subs, subt, result, revResult);

    myCollator->setStrength(Collator::SECONDARY);
    myCollator->getCollationKey(subs, collationKey1, status1);
    myCollator->getCollationKey(subt, collationKey2, status2);
    result = collationKey1.compareTo(collationKey2);  // Secondary
    revResult = collationKey2.compareTo(collationKey1);   // Secondary
    report( subs, subt, result, revResult);

    myCollator->setStrength(Collator::PRIMARY);
    myCollator->getCollationKey(subs, collationKey1, status1);
    myCollator->getCollationKey(subt, collationKey2, status2);
    result = collationKey1.compareTo(collationKey2);  // Primary
    revResult = collationKey2.compareTo(collationKey1);   // Primary
    report(subs, subt, result, revResult);

    UnicodeString msg;
    UnicodeString addOne(subs);
    addOne += 0xE000;

    myCollator->getCollationKey(subs, collationKey1, status1);
    myCollator->getCollationKey(addOne, collationKey2, status2);
    result = collationKey1.compareTo(collationKey2);
    if (result != -1)
    {
        msg += "CollationKey(";
        msg += subs;
        msg += ") .LT. CollationKey(";
        msg += addOne;
        msg += ") Failed.";
        errln(msg);
    }

    msg.remove();
    result = collationKey2.compareTo(collationKey1);
    if (result != 1)
    {
        msg += "CollationKey(";
        msg += addOne;
        msg += ") .GT. CollationKey(";
        msg += subs;
        msg += ") Failed.";
        errln(msg);
    }
}

void 
CollationMonkeyTest::TestCompare(char* par)
{
    if(source.length() == 0) {
        errln(UNICODE_STRING("CollationMonkeyTest::TestCompare(): source is empty - ICU_DATA not set or data missing?", 87));
        return;
    }

    /* Seed the random-number generator with current time so that
     * the numbers will be different every time we run.
     */
    srand( (unsigned)time( NULL ) );
    int32_t s = checkValue(rand() % source.length());
    int32_t t = checkValue(rand() % source.length());
    int32_t slen = checkValue((rand() - source.length()) % source.length());
    int32_t tlen = checkValue((rand() - source.length()) % source.length());
    UnicodeString subs, subt;

    source.extract(MIN(s, slen), MAX(s, slen), subs);
    source.extract(MIN(t, tlen), MAX(t, tlen), subt);

    myCollator->setStrength(Collator::TERTIARY);
    int32_t result = myCollator->compare(subs, subt);  // Tertiary
    int32_t revResult = myCollator->compare(subt, subs);  // Tertiary
    report(subs, subt, result, revResult);

    myCollator->setStrength(Collator::SECONDARY);
    result = myCollator->compare(subs, subt);  // Secondary
    revResult = myCollator->compare(subt, subs);  // Secondary
    report(subs, subt, result, revResult);

    myCollator->setStrength(Collator::PRIMARY);
    result = myCollator->compare(subs, subt);  // Primary
    revResult = myCollator->compare(subt, subs);  // Primary
    report(subs, subt, result, revResult);

    UnicodeString msg;
    UnicodeString addOne(subs);
    addOne += 0xE000;

    result = myCollator->compare(subs, addOne);
    if (result != -1)
    {
        msg += "Test : ";
        msg += subs;
        msg += " .LT. ";
        msg += addOne;
        msg += " Failed.";
        errln(msg);
    }

    msg.remove();
    result = myCollator->compare(addOne, subs);
    if (result != 1)
    {
        msg += "Test : ";
        msg += addOne;
        msg += " .GT. ";
        msg += subs;
        msg += " Failed.";
        errln(msg);
    }
}

void CollationMonkeyTest::runIndexedTest( int32_t index, bool_t exec, char* &name, char* par )
{
    if (exec) logln("TestSuite CollationMonkeyTest: ");
    switch (index) {
        case 0: name = "TestCompare";   if (exec)   TestCompare( par ); break;
        case 1: name = "TestCollationKey"; if (exec)    TestCollationKey( par ); break;
        default: name = ""; break;
    }
}


