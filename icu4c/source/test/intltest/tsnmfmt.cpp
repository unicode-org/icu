/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

#include "unicode/utypes.h"
#include "tsnmfmt.h"

#include "unicode/decimfmt.h"

#include <math.h>
#include <float.h>
#include <limits.h>

/**
 * This test does round-trip testing (format -> parse -> format -> parse -> etc.) of
 * NumberFormat.
 */
void IntlTestNumberFormat::runIndexedTest( int32_t index, UBool exec, char* &name, char* par )
{

    if (exec) logln((UnicodeString)"TestSuite NumberFormat");
    switch (index) {
        case 0: name = "createInstance"; 
            if (exec)
            {
                logln(name);
                fStatus = U_ZERO_ERROR;
                fFormat = NumberFormat::createInstance(fStatus);
                testFormat(par);
            }
            break;

        case 1: name = "Default Locale";
            if (exec) testLocale(par, Locale::getDefault(), "Default Locale");
            break;

        case 2: name = "Determine Available Locales"; 
            if (exec) {
                logln(name);
                testAvailableLocales(par);
            }
            break;

        case 3: name = "Test Available Locales"; 
            if (exec) {
                logln(name);
                monsterTest(par);
            }
            break;

        default: name = ""; break;
    }
}

void
IntlTestNumberFormat::testLocale(char* par, const Locale& locale, const UnicodeString& localeName)
{
    const char* name;
    
    name = "Number test";
    logln((UnicodeString)name + " (" + localeName + ")");
    fStatus = U_ZERO_ERROR;
    fFormat = NumberFormat::createInstance(locale, fStatus);
    testFormat(par);

    name = "Currency test";
    logln((UnicodeString)name + " (" + localeName + ")");
    fStatus = U_ZERO_ERROR;
    fFormat = NumberFormat::createCurrencyInstance(locale, fStatus);
    testFormat(par);

    name = "Percent test";
    logln((UnicodeString)name + " (" + localeName + ")");
    fStatus = U_ZERO_ERROR;
    fFormat = NumberFormat::createPercentInstance(locale, fStatus);
    testFormat(par);
}

void
IntlTestNumberFormat::testFormat(char *par)
{
    if (U_FAILURE(fStatus))
    { 
        errln((UnicodeString)"********** FAIL: createXxxInstance failed.");
        if (fFormat != 0) errln("********** FAIL: Non-null format returned by createXxxInstance upon failure.");
        delete fFormat;
        fFormat = 0;
        return;
    }
                    
    if (fFormat == 0)
    {
        errln((UnicodeString)"********** FAIL: Null format returned by createXxxInstance.");
        return;
    }

    UnicodeString str;

    // Assume it's a DecimalFormat and get some info
    DecimalFormat *s = (DecimalFormat*)fFormat;
    logln((UnicodeString)"  Pattern " + s->toPattern(str));

    if (1)
    {
#ifdef OS390
        tryIt(-2.02147304840132e-68);
        tryIt(3.88057859588817e-68); // Test rounding when only some digits are shown because exponent is close to -maxfrac
        tryIt(-2.64651110485945e+65); // Overflows to +INF when shown as a percent
        tryIt(9.29526819488338e+64); // Ok -- used to fail?
#else
        tryIt(-2.02147304840132e-100);
        tryIt(3.88057859588817e-096); // Test rounding when only some digits are shown because exponent is close to -maxfrac
        tryIt(-2.64651110485945e+306); // Overflows to +INF when shown as a percent
        tryIt(9.29526819488338e+250); // Ok -- used to fail?
#endif
    }

    if (1)
    {
        // These PASS now, with the sprintf/atof based format-parse.

        // These fail due to round-off
        // The least significant digit drops by one during each format-parse cycle.
        // Both numbers DON'T have a round-off problem when multiplied by 100! (shown as %)
#ifdef OS390
        tryIt(-9.18228054496402e+64);
        tryIt(-9.69413034454191e+64);
#else
        tryIt(-9.18228054496402e+255);
        tryIt(-9.69413034454191e+273);
#endif
    }

    if (1)
    {
        tryIt((int32_t)251887531);
#ifndef OS390
        tryIt(-2.3e-168);

        tryIt(uprv_getNaN());
        tryIt(uprv_getInfinity());
        tryIt(-uprv_getInfinity());
#endif

        tryIt(5e-20 / 9);
        tryIt(5e20 / 9);
#ifndef OS390
        tryIt(1.234e-200);
#endif
        tryIt(1.234e-50);
        tryIt(9.99999999999996);
        tryIt(9.999999999999996);

        tryIt((int32_t)INT32_MIN);
        tryIt((int32_t)INT32_MAX);
        tryIt((double)LONG_MIN);
        tryIt((double)LONG_MAX);
        tryIt((double)LONG_MIN - 1.0);
        tryIt((double)LONG_MAX + 1.0);

        tryIt(5.0 / 9.0 * 1e-20);
        tryIt(4.0 / 9.0 * 1e-20);
        tryIt(5.0 / 9.0 * 1e+20);
        tryIt(4.0 / 9.0 * 1e+20);

        tryIt(2147483647.);
        tryIt((int32_t)0);
        tryIt(0.0);
        tryIt((int32_t)1);
        tryIt((int32_t)10);
        tryIt((int32_t)100);
        tryIt((int32_t)-1);
        tryIt((int32_t)-10);
        tryIt((int32_t)-100);
    }

    if (1)
    {
        for (int32_t z=0; z<10; ++z)
        {
            double d = randFraction() * 2e10 - 1e10;
            tryIt(d);
        }

        double it = randDouble() * 10000;
        tryIt(0.0);
        tryIt(it);
        tryIt((int32_t)0);
        tryIt((int32_t)uprv_floor(it));

        // try again
        it = randDouble() * 10;
        tryIt(it);
        tryIt((int32_t)uprv_floor(it));

#ifndef OS390
        // try again with very larget numbers
        it = randDouble() * 10000000000.0;
        tryIt(it);
        tryIt((int32_t)uprv_floor(it));
#endif
    }

    delete fFormat;
}

void 
IntlTestNumberFormat::tryIt(double aNumber)
{
    const int32_t DEPTH = 10;
    Formattable number[DEPTH];
    UnicodeString string[DEPTH];

    int32_t numberMatch = 0;
    int32_t stringMatch = 0;
    UBool dump = FALSE;
    int32_t i;
    for (i=0; i<DEPTH; ++i)
    {
        UErrorCode status = U_ZERO_ERROR;
        if (i == 0) number[i].setDouble(aNumber);
        else fFormat->parse(string[i-1], number[i], status);
        if (U_FAILURE(status))
        {
            errln("********** FAIL: Parse of " + string[i-1] + " failed.");
            dump = TRUE;
            break;
        }
        // Convert from long to double
        if (number[i].getType() == Formattable::kLong) number[i].setDouble(number[i].getLong());
        else if (number[i].getType() != Formattable::kDouble)
        {
            errln("********** FAIL: Parse of " + string[i-1] + " returned non-numeric Formattable.");
            dump = TRUE;
            break;
        }
        fFormat->format(number[i].getDouble(), string[i]);
        if (i > 0)
        {
            if (numberMatch == 0 && number[i] == number[i-1]) numberMatch = i;
            else if (numberMatch > 0 && number[i] != number[i-1])
            {
                errln("********** FAIL: Numeric mismatch after match.");
                dump = TRUE;
                break;
            }
            if (stringMatch == 0 && string[i] == string[i-1]) stringMatch = i;
            else if (stringMatch > 0 && string[i] != string[i-1])
            {
                errln("********** FAIL: String mismatch after match.");
                dump = TRUE;
                break;
            }
        }
        if (numberMatch > 0 && stringMatch > 0) break;
    }
    if (i == DEPTH) --i;

    if (stringMatch > 2 || numberMatch > 2)
    {
        errln("********** FAIL: No string and/or number match within 2 iterations.");
        dump = TRUE;
    }

    if (dump)
    {
        for (int32_t k=0; k<=i; ++k)
        {
            logln((UnicodeString)"" + k + ": " + number[k].getDouble() + " F> " +
                  string[k] + " P> ");
        }
    }
}

void 
IntlTestNumberFormat::tryIt(int32_t aNumber)
{
    const int32_t DEPTH = 10;
    Formattable number[DEPTH];
    UnicodeString string[DEPTH];

    int32_t numberMatch = 0;
    int32_t stringMatch = 0;
    UBool dump = FALSE;
    int32_t i;
    for (i=0; i<DEPTH; ++i)
    {
        UErrorCode status = U_ZERO_ERROR;
        if (i == 0) number[i].setLong(aNumber);
        else fFormat->parse(string[i-1], number[i], status);
        if (U_FAILURE(status))
        {
            errln("********** FAIL: Parse of " + string[i-1] + " failed.");
            dump = TRUE;
            break;
        }
        if (number[i].getType() != Formattable::kLong)
        {
            errln("********** FAIL: Parse of " + string[i-1] + " returned non-long Formattable.");
            dump = TRUE;
            break;
        }
        fFormat->format(number[i].getLong(), string[i]);
        if (i > 0)
        {
            if (numberMatch == 0 && number[i] == number[i-1]) numberMatch = i;
            else if (numberMatch > 0 && number[i] != number[i-1])
            {
                errln("********** FAIL: Numeric mismatch after match.");
                dump = TRUE;
                break;
            }
            if (stringMatch == 0 && string[i] == string[i-1]) stringMatch = i;
            else if (stringMatch > 0 && string[i] != string[i-1])
            {
                errln("********** FAIL: String mismatch after match.");
                dump = TRUE;
                break;
            }
        }
        if (numberMatch > 0 && stringMatch > 0) break;
    }
    if (i == DEPTH) --i;

    if (stringMatch > 2 || numberMatch > 2)
    {
        errln("********** FAIL: No string and/or number match within 2 iterations.");
        dump = TRUE;
    }

    if (dump)
    {
        for (int32_t k=0; k<=i; ++k)
        {
            logln((UnicodeString)"" + k + ": " + number[k].getLong() + " F> " +
                  string[k] + " P> ");
        }
    }
}

void IntlTestNumberFormat::testAvailableLocales(char *par)
{
    int32_t count = 0;
    const Locale* locales = NumberFormat::getAvailableLocales(count);
    logln((UnicodeString)"" + count + " available locales");
    if (locales && count)
    {
        UnicodeString name;
        UnicodeString all;
        for (int32_t i=0; i<count; ++i)
        {
            if (i!=0) all += ", ";
            all += locales[i].getName();
        }
        logln(all);
    }
    else errln((UnicodeString)"********** FAIL: Zero available locales or null array pointer");
}

void IntlTestNumberFormat::monsterTest(char *par)
{
    const char *SEP = "============================================================\n";
    int32_t count;
    const Locale* locales = NumberFormat::getAvailableLocales(count);
    if (locales && count)
    {
        if (quick && count > 2) {
            logln("quick test: testing just 2 locales!");
            count = 2;
        }
        for (int32_t i=0; i<count; ++i)
        {
            UnicodeString name(locales[i].getName(), "");
            logln(SEP);
            testLocale(par, locales[i], name);
        }
    }

    logln(SEP);
}
