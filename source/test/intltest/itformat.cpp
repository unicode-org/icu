/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

/**
 * IntlTestFormat is the medium level test class for everything in the directory "format".
 */

#include "unicode/utypes.h"
#include "itformat.h"
#include "tsdate.h"
#include "tsnmfmt.h"
#include "caltest.h"
#include "callimts.h"
#include "tztest.h"
#include "tzbdtest.h"
#include "tsdcfmsy.h"       // DecimalFormatSymbols
#include "tchcfmt.h"
#include "tsdtfmsy.h"       // DateFormatSymbols
#include "dcfmapts.h"       // DecimalFormatAPI
#include "tfsmalls.h"       // Format Small Classes
#include "nmfmapts.h"       // NumberFormatAPI
#include "numfmtst.h"       // NumberFormatTest
#include "sdtfmtts.h"       // SimpleDateFormatAPI
#include "dtfmapts.h"       // DateFormatAPI
#include "dtfmttst.h"       // DateFormatTest
#include "tmsgfmt.h"        // TestMessageFormat
#include "dtfmrgts.h"       // DateFormatRegressionTest
#include "msfmrgts.h"       // MessageFormatRegressionTest
#include "miscdtfm.h"       // DateFormatMiscTests
#include "nmfmtrt.h"        // NumberFormatRoundTripTest
#include "numrgts.h"        // NumberFormatRegressionTest
#include "dtfmtrtts.h"      // DateFormatRoundTripTest
#include "pptest.h"         // ParsePositionTest
#include "calregts.h"       // CalendarRegressionTest
#include "tzregts.h"        // TimeZoneRegressionTest

void IntlTestFormat::runIndexedTest( int32_t index, UBool exec, char* &name, char* par )
{
    // for all format tests, always set default Locale and TimeZone to ENGLISH and PST.
    TimeZone* saveDefaultTimeZone;
    Locale  saveDefaultLocale = Locale::getDefault();
    if (exec) {
        saveDefaultTimeZone = TimeZone::createDefault();
        TimeZone *tz = TimeZone::createTimeZone("PST");
        TimeZone::setDefault(*tz);
        delete tz;
        UErrorCode status = U_ZERO_ERROR;
        Locale::setDefault( Locale::ENGLISH, status );
        if (U_FAILURE(status)) {
            errln("itformat: couldn't set default Locale to ENGLISH!");
        }
    }
    if (exec) logln("TestSuite Format: ");
    switch (index) {
        case 0: 
            name = "DateFormat"; 
            if (exec) {
                logln("DateFormat test---"); logln("");
                IntlTestDateFormat test;
                callTest( test, par );
            }
            break;

        case 1: 
            name = "NumberFormat"; 
            if (exec) {
                logln("NumberFormat test---"); logln("");
                IntlTestNumberFormat test;
                callTest( test, par );
            }
            break;

        case 2:
            name = "TestCalendar";
            if (exec) {
                logln("TestCalendar---"); logln("");
                CalendarTest test;
                callTest( test, par );
            }
            break;

        case 3:
            name = "TestCalendarLimit";
            if (exec) {
                logln("TestCalendarLimit---"); logln("");
                CalendarLimitTest test;
                callTest( test, par );
            }
            break;

        case 4:
            name = "TestTimeZone";
            if (exec) {
                logln("TestTimeZone---"); logln("");
                TimeZoneTest test;
                callTest( test, par );
            }
            break;

        case 5:
            name = "TestTimeZoneBoundary";
            if (exec) {
                logln("TestTimeZoneBoundary---"); logln("");
                TimeZoneBoundaryTest test;
                callTest( test, par );
            }
            break;

        case 6: name = "chc"; 
                if (exec) { 
                    logln("TestSuite Format/ChoiceFormat---"); logln();
                    TestChoiceFormat test;
                    callTest( test, par );
                }
                break;
        
        case 7: 
            name = "DecimalFormatSymbols"; 
            if (exec) {
                logln("DecimalFormatSymbols test---"); logln("");
                IntlTestDecimalFormatSymbols test;
                callTest( test, par );
            }
            break;

        case 8: 
            name = "DateFormatSymbols"; 
            if (exec) {
                logln("DateFormatSymbols test---"); logln("");
                IntlTestDateFormatSymbols test;
                callTest( test, par );
            }
            break;

        case 9: 
            name = "DecimalFormatAPI"; 
            if (exec) {
                logln("DecimalFormatAPI test---"); logln("");
                IntlTestDecimalFormatAPI test;
                callTest( test, par );
            }
            break;

        case 10: 
            name = "sma"; 
            if (exec) {
                logln("TestSuite Format/SmallClasses---"); logln("");
                TestFormatSmallClasses test;
                callTest( test, par );
            }
            break;

        case 11: 
            name = "NumberFormatAPI"; 
            if (exec) {
                logln("NumberFormatAPI test---"); logln("");
                IntlTestNumberFormatAPI test;
                callTest( test, par );
            }
            break;

        case 12: 
            name = "SimpleDateFormatAPI"; 
            if (exec) {
                logln("SimpleDateFormatAPI test---"); logln("");
                IntlTestSimpleDateFormatAPI test;
                callTest( test, par );
            }
            break;

        case 13: 
            name = "DateFormatAPI"; 
            if (exec) {
                logln("DateFormatAPI test---"); logln("");
                IntlTestDateFormatAPI test;
                callTest( test, par );
            }
            break;

        case 14: 
            name = "TestDateFormat"; 
            if (exec) {
                logln("TestDateFormat test---"); logln("");
                DateFormatTest test;
                callTest( test, par );
            }
            break;

        case 15:
            name = "msg"; 
            if (exec) {
                logln("TestMessageFormat test---"); logln("");
                TestMessageFormat test;
                callTest( test, par );
            }
            break;

        case 16: 
            name = "TestNumberFormat"; 
            if (exec) {
                logln("TestNumberFormat test---"); logln("");
                NumberFormatTest test;
                callTest( test, par );
            }
            break;

        case 17: 
            name = "DateFormatRegression"; 
            if (exec) {
                logln("DateFormatRegression test---"); logln("");
                DateFormatRegressionTest test;
                callTest( test, par );
            }
            break;

        case 18: 
            name = "MessageFormatRegression"; 
            if (exec) {
                logln("MessageFormatRegression test---"); logln("");
                MessageFormatRegressionTest test;
                callTest( test, par );
            }
            break;

        case 19: 
            name = "DateFormatMisc"; 
            if (exec) {
                logln("DateFormatMisc test---"); logln("");
                DateFormatMiscTests test;
                callTest( test, par );
            }
            break;

        case 20: 
            name = "NumberFormatRoundTrip"; 
            if (exec) {
                logln("NumberFormatRoundTrip test---"); logln("");
                NumberFormatRoundTripTest test;
                callTest( test, par );
            }
            break;

        case 21: 
            name = "NumberFormatRegression"; 
            if (exec) {
                logln("NumberFormatRegression test---"); logln("");
                NumberFormatRegressionTest test;
                callTest( test, par );
            }
            break;

        case 22: 
            name = "DateFormatRoundTrip"; 
            if (exec) {
                logln("DateFormatRoundTrip test---"); logln("");
                DateFormatRoundTripTest test;
                callTest( test, par );
            }
            break;

        case 23: 
            name = "ParsePosition"; 
            if (exec) {
                logln("ParsePosition test---"); logln("");
                ParsePositionTest test;
                callTest( test, par );
            }
            break;


        case 24: 
            name = "CalendarRegression"; 
            if (exec) {
                logln("CalendarRegression test---"); logln("");
                CalendarRegressionTest test;
                callTest( test, par );
            }
            break;

        case 25: 
            name = "TimeZoneRegression"; 
            if (exec) {
                logln("TimeZoneRegression test---"); logln("");
                TimeZoneRegressionTest test;
                callTest( test, par );
            }
            break;

        /*
        case 28: 
            name = "DateFormatSymbolsCAPI"; 
            if (exec) {
                logln("DateFormatSymbols C API test---"); logln("");
                IntlTestDateFormatSymbolsC test;
                callTest( test, par );
            }
            break;

        case 29: 
            name = "DecimalFormatSymbolsCAPI"; 
            if (exec) {
                logln("DecimalFormatSymbols C API test---"); logln("");
                IntlTestDecimalFormatSymbolsC test;
                callTest( test, par );
            }
            break;

        case 30: 
            name = "SimpleDateFormatCAPI"; 
            if (exec) {
                logln("SimpleDateFormat C API test---"); logln("");
                IntlTestSimpleDateFormatAPIC test;
                callTest( test, par );
            }
            break;

        case 31: 
            name = "DateFormatCAPI"; 
            if (exec) {
                logln("DateFormat C API test---"); logln("");
                IntlTestDateFormatAPIC test;
                callTest( test, par );
            }
            break;


        case 32: 
            name = "DecimalFormatCAPI"; 
            if (exec) {
                logln("Decimal Format C API test---"); logln("");
                IntlTestDecimalFormatAPIC test;
                callTest( test, par );
            }
            break;

        case 33: 
            name = "NumberFormatCAPI"; 
            if (exec) {
                logln("NumberFormat C API test---"); logln("");
                IntlTestNumberFormatAPIC test;
                callTest( test, par );
            }
            break;

        case 34:
            name = "NumberSpelloutFormatCAPI";
            if (exec) {
                logln("NumberSpelloutFormat C API test---"); logln("");
                CNumberSpelloutFormatTest test;
                callTest(test, par);
            }
            break;

        case 35:
            name = "NumberSpelloutFormatCRoundTrip";
            if (exec) {
                logln("NumberSpelloutFormat C Round Trip test---"); logln("");
                CNumberSpelloutFormatRoundTripTest test;
                callTest(test, par);
            }
            break;
        case 36:
            name = "FormatSmallClassesCAPI";
            if (exec) {
                logln("Format Small Classes C-API test---"); logln();
                TestCwrapperFormatSmallClasses test;
                callTest(test, par);
            }
            break;

        case 37:
            name = "MessageFormatCAPI";
            if (exec) {
                logln("MessageFormat C-API test---"); logln();
                TestCwrapperMessageFormat test;
                callTest(test, par);
            }
            break;

        case 38:
            name = "ChoiceFormatCAPI";
            if (exec) {
                logln("ChoiceFormat C-API test---"); logln();
                TestCwrapperChoiceFormat test;
                callTest(test, par);
            }
            break;

        case 39:
            name = "CalendarCAPI";
            if (exec) {
                logln("Calendar C-API test---"); logln();
                TestCwrapperCalendar test;
                callTest(test, par);
            }
            break;

        case 40:
            name = "TimeZoneCAPI";
            if (exec) {
                logln("TimeZone C-API test---"); logln();
                TestCwrapperTimeZone test;
                callTest(test, par);
            }
            break;
        */

        default: name = ""; break; //needed to end loop
    }
    if (exec) {
        // restore saved Locale and TimeZone
        TimeZone::adoptDefault(saveDefaultTimeZone);
        UErrorCode status = U_ZERO_ERROR;
        Locale::setDefault( saveDefaultLocale, status );
        if (U_FAILURE(status)) {
            errln("itformat: couldn't re-set default Locale!");
        }
    }
}


