/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

#ifndef _DATEFORMATROUNDTRIPTEST_
#define _DATEFORMATROUNDTRIPTEST_
 
#include "unicode/utypes.h"
#include "intltest.h"
#include "locale.h"
#include "unicode/unistr.h"

class DateFormat;
class SimpleDateFormat;
class Calendar;

/** 
 * Performs round-trip tests for DateFormat
 **/
class DateFormatRoundTripTest : public IntlTest {    
    
    // IntlTest override
    void runIndexedTest( int32_t index, bool_t exec, char* &name, char* par );

public:
    void TestDateFormatRoundTrip(void);
    void test(const Locale& loc);
    void test(DateFormat *fmt, bool_t timeOnly = FALSE );
    int32_t getField(UDate d, int32_t f);
    UnicodeString& escape(const UnicodeString& src, UnicodeString& dst);
    UDate generateDate(void); 


//============================================================
// statics
//============================================================

/**
 * Return a random uint32_t
 **/
static uint32_t randLong()
{
    // Assume 8-bit (or larger) rand values.  Also assume
    // that the system rand() function is very poor, which it always is.
    uint32_t d;
    int32_t i;
    for (i=0; i < sizeof(uint32_t); ++i)
    {
        char* poke = (char*)&d;
        poke[i] = (rand() & 0xFF);
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

/**
 * Return a random value from -range..+range.
 **/
static double randDouble(double range)
{
    double a = randFraction();
    //while(TPlatformUtilities::isInfinite(a) || TPlatformUtilities::isNaN(a))
    //    a = randFraction();
    return (2.0 * range * a) - range;
}

protected:
    bool_t failure(UErrorCode status, const char* msg);

private:

    static bool_t INFINITE;
    static int32_t SPARSENESS;
    static int32_t TRIALS;
    static int32_t DEPTH;

    SimpleDateFormat *dateFormat;
    Calendar *getFieldCal;
};
 
#endif // _DATEFORMATROUNDTRIPTEST_
//eof
