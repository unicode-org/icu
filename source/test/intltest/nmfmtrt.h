
/*
********************************************************************
* COPYRIGHT: 
* (C) Copyright International Business Machines Corporation, 1998
* Licensed Material - Program-Property of IBM - All Rights Reserved. 
* US Government Users Restricted Rights - Use, duplication, or disclosure 
* restricted by GSA ADP Schedule Contract with IBM Corp. 
*
********************************************************************
*/

#ifndef _NUMBERFORMATROUNDTRIPTEST_
#define _NUMBERFORMATROUNDTRIPTEST_
 
#include "utypes.h"
#include "intltest.h"

#include "fmtable.h"

class NumberFormat;

/** 
 * Performs round-trip tests for NumberFormat
 **/
class NumberFormatRoundTripTest : public IntlTest {    
    
    // IntlTest override
    void runIndexedTest( int32_t index, bool_t exec, char* &name, char* par );
public:

static bool_t verbose;
static bool_t STRING_COMPARE;
static bool_t EXACT_NUMERIC_COMPARE;
static bool_t DEBUG;
static double MAX_ERROR;
static double max_numeric_error;
static double min_numeric_error;


void start(void);

void test(NumberFormat *fmt);
void test(NumberFormat *fmt, double value);
void test(NumberFormat *fmt, int32_t value);
void test(NumberFormat *fmt, const Formattable& value);

static double randomDouble(double range);
static double proportionalError(const Formattable& a, const Formattable& b);
static UnicodeString& typeOf(const Formattable& n, UnicodeString& result);
static UnicodeString& escape(UnicodeString& s);

static bool_t
isDouble(const Formattable& n)
{ return (n.getType() == Formattable::kDouble); }

static bool_t
isLong(const Formattable& n)
{ return (n.getType() == Formattable::kLong); }

/*
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

protected:
    const char* errorName(UErrorCode code);
    bool_t failure(UErrorCode status, const char* msg);

};
 
#endif // _NUMBERFORMATROUNDTRIPTEST_
//eof
