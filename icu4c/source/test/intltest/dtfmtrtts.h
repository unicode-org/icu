/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2003, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

#ifndef _DATEFORMATROUNDTRIPTEST_
#define _DATEFORMATROUNDTRIPTEST_
 
#include <stdlib.h>

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/unistr.h"
#include "unicode/datefmt.h"
#include "unicode/smpdtfmt.h"
#include "unicode/calendar.h"
#include "intltest.h"

/** 
 * Performs round-trip tests for DateFormat
 **/
class DateFormatRoundTripTest : public IntlTest {    
    
    // IntlTest override
    void runIndexedTest( int32_t index, UBool exec, const char* &name, char* par );

public:
    void TestDateFormatRoundTrip(void);
    void test(const Locale& loc);
    void test(DateFormat *fmt, const Locale &origLocale, UBool timeOnly = FALSE );
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
    uint32_t i;
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
    UBool failure(UErrorCode status, const char* msg);
    UBool failure(UErrorCode status, const char* msg, const UnicodeString& str);

private:

    static int32_t SPARSENESS;
    static int32_t TRIALS;
    static int32_t DEPTH;

    SimpleDateFormat *dateFormat;
    Calendar *getFieldCal;
};

#endif /* #if !UCONFIG_NO_FORMATTING */
 
#endif // _DATEFORMATROUNDTRIPTEST_
//eof
