/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2003, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "tsdtfmsy.h"

#include "unicode/dtfmtsym.h"


void IntlTestDateFormatSymbols::runIndexedTest( int32_t index, UBool exec, const char* &name, char* /*par*/ )
{
    if (exec) logln("TestSuite DateFormatSymbols");
    switch (index) {
        case 0: name = "DateFormatSymbols test"; 
                if (exec) {
                    logln("DateFormatSymbols test---"); logln("");
                    testSymbols(/*par*/);
                }
                break;

        case 1: name = "TestGetMonths"; 
                if (exec) {
                    logln("TestGetMonths test---"); logln("");
                    TestGetMonths();
                }
                break;

        default: name = ""; break;
    }
}

/**
 * Test getMonths.
 */
void IntlTestDateFormatSymbols::TestGetMonths()
{
    UErrorCode  status = U_ZERO_ERROR;
    int32_t cnt;
    const UnicodeString* month;
    DateFormatSymbols *symbol;

    symbol=new DateFormatSymbols(Locale::getDefault(), status);

    month=symbol->getMonths(cnt);

    logln((UnicodeString)"size = " + cnt);

    for (int32_t i=0; i<cnt; ++i)
    {
        logln(month[i]);
    }

    delete symbol;
}

/**
 * Test the API of DateFormatSymbols; primarily a simple get/set set.
 */
void IntlTestDateFormatSymbols::testSymbols(/* char *par */)
{
    UErrorCode status = U_ZERO_ERROR;

    DateFormatSymbols fr(Locale::getFrench(), status);
    if(U_FAILURE(status)) {
        errln("ERROR: Couldn't create French DateFormatSymbols " + (UnicodeString)u_errorName(status));
    }

    status = U_ZERO_ERROR;
    DateFormatSymbols en(Locale::getEnglish(), status);
    if(U_FAILURE(status)) {
        errln("ERROR: Couldn't create English DateFormatSymbols " + (UnicodeString)u_errorName(status));
    }

    if(en == fr || ! (en != fr) ) {
        errln("ERROR: English DateFormatSymbols equal to French");
    }

    // just do some VERY basic tests to make sure that get/set work

    int32_t count = 0;
    const UnicodeString *eras = en.getEras(count);
    if(count == 0) {
      errln("ERROR: 0 english eras.. exitting..\n");
      return;
    }

    fr.setEras(eras, count);
    if( *en.getEras(count) != *fr.getEras(count)) {
      errln("ERROR: setEras() failed");
    }

    const UnicodeString *months = en.getMonths(count);
    fr.setMonths(months, count);
    if( *en.getMonths(count) != *fr.getMonths(count)) {
        errln("ERROR: setMonths() failed");
    }

    const UnicodeString *shortMonths = en.getShortMonths(count);
    fr.setShortMonths(shortMonths, count);
    if( *en.getShortMonths(count) != *fr.getShortMonths(count)) {
        errln("ERROR: setShortMonths() failed");
    }

    const UnicodeString *weekdays = en.getWeekdays(count);
    fr.setWeekdays(weekdays, count);
    if( *en.getWeekdays(count) != *fr.getWeekdays(count)) {
        errln("ERROR: setWeekdays() failed");
    }

    const UnicodeString *shortWeekdays = en.getShortWeekdays(count);
    fr.setShortWeekdays(shortWeekdays, count);
    if( *en.getShortWeekdays(count) != *fr.getShortWeekdays(count)) {
        errln("ERROR: setShortWeekdays() failed");
    }

    const UnicodeString *ampms = en.getAmPmStrings(count);
    fr.setAmPmStrings(ampms, count);
    if( *en.getAmPmStrings(count) != *fr.getAmPmStrings(count)) {
        errln("ERROR: setAmPmStrings() failed");
    }

    int32_t rowCount = 0, columnCount = 0;
    const UnicodeString **strings = en.getZoneStrings(rowCount, columnCount);
    fr.setZoneStrings(strings, rowCount, columnCount);
    const UnicodeString **strings1 = fr.getZoneStrings(rowCount, columnCount);
    for(int32_t i = 0; i < rowCount; i++) {
        for(int32_t j = 0; j < columnCount; j++) {
            if( strings[i][j] != strings1[i][j] ) {
                errln("ERROR: setZoneStrings() failed");
            }

        }
    }

    UnicodeString localPattern, pat1, pat2;
    localPattern = en.getLocalPatternChars(localPattern);
    fr.setLocalPatternChars(localPattern);
    if( en.getLocalPatternChars(pat1) != fr.getLocalPatternChars(pat2)) {
        errln("ERROR: setLocalPatternChars() failed");
    }


    status = U_ZERO_ERROR;
    DateFormatSymbols foo(status);
    DateFormatSymbols bar(foo);

    en = fr;

    if(en != fr || foo != bar) {
        errln("ERROR: Copy Constructor or Assignment failed");
    }
}

#endif /* #if !UCONFIG_NO_FORMATTING */
