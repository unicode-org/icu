/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

#ifndef _INTLTESTNUMBERFORMAT
#define _INTLTESTNUMBERFORMAT


#include "unicode/utypes.h"
#include "intltest.h"

#include "unicode/fmtable.h"

class NumberFormat;

/**
 * This test does round-trip testing (format -> parse -> format -> parse -> etc.) of
 * NumberFormat.
 */
class IntlTestNumberFormat: public IntlTest {
    void runIndexedTest( int32_t index, UBool exec, char* &name, char* par = NULL );  

private:

    /**
     *  call tryIt with many variations, called by testLocale
     **/
    void testFormat(char *par);
    /**
     *  perform tests using aNumber and fFormat, called in many variations
     **/
    void tryIt(double aNumber);
    /**
     *  perform tests using aNumber and fFormat, called in many variations
     **/
    void tryIt(int32_t aNumber);
    /**
     *  test NumberFormat::getAvailableLocales
     **/
    void testAvailableLocales(char *par);
    /**
     *  call testLocale for all locales
     **/
    void monsterTest(char *par);
    /**
     *  call testFormat for currency, percent and plain number instances
     **/
    void testLocale(char *par, const Locale& locale, const UnicodeString& localeName);

    NumberFormat*   fFormat;
    UErrorCode       fStatus;

public:

/*
 * Return a random double
 **/
static double randDouble()
{
    // Assume 8-bit (or larger) rand values.  Also assume
    // that the system rand() function is very poor, which it always is.
    double d;
    int32_t i;
    char* poke = (char*)&d;
    for (i=0; i < sizeof(double); ++i)
    {
        poke[i] = (char)(rand() & 0xFF);
    }
    return d;
}

/*
 * Return a random uint32_t
 **/
static uint32_t randLong()
{
    // Assume 8-bit (or larger) rand values.  Also assume
    // that the system rand() function is very poor, which it always is.
    uint32_t d;
    int32_t i;
    char* poke = (char*)&d;
    for (i=0; i < sizeof(uint32_t); ++i)
    {
        poke[i] = (char)(rand() & 0xFF);
    }
    return d;
}

/**
 * Return a random double 0 <= x < 1.0
 **/
static double randFraction()
{
    return (double)randLong() / (double)0xFFFFFFFF;
}

};

#endif
