// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/***********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2014, International Business Machines Corporation
 * and others. All Rights Reserved.
 ***********************************************************************/

/* Test Internationalized Calendars for C++ */

#include "unicode/utypes.h"
#include "cmemory.h"
#include "string.h"
#include "unicode/locid.h"
#include "japancal.h"
#include "unicode/localpointer.h"
#include "unicode/datefmt.h"
#include "unicode/smpdtfmt.h"
#include "unicode/dtptngen.h"

#if !UCONFIG_NO_FORMATTING

#include <stdio.h>
#include "caltest.h"

#define CHECK(status, msg) UPRV_BLOCK_MACRO_BEGIN { \
    if (U_FAILURE(status)) { \
        dataerrln((UnicodeString(u_errorName(status)) + UnicodeString(" : " ) )+ msg); \
        return; \
    } \
} UPRV_BLOCK_MACRO_END


static UnicodeString escape( const UnicodeString&src)
{
  UnicodeString dst;
    dst.remove();
    for (int32_t i = 0; i < src.length(); ++i) {
        char16_t c = src[i];
        if(c < 0x0080) 
            dst += c;
        else {
            dst += UnicodeString("[");
            char buf [8];
            snprintf(buf, sizeof(buf), "%#x", c);
            dst += UnicodeString(buf);
            dst += UnicodeString("]");
        }
    }

    return dst;
}


#include "incaltst.h"
#include "unicode/gregocal.h"
#include "unicode/smpdtfmt.h"
#include "unicode/simpletz.h"
 
// *****************************************************************************
// class IntlCalendarTest
// *****************************************************************************
//--- move to CalendarTest?

// Turn this on to dump the calendar fields 
#define U_DEBUG_DUMPCALS  


#define CASE(id,test) case id: name = #test; if (exec) { logln(#test "---"); logln((UnicodeString)""); test(); } break


void IntlCalendarTest::runIndexedTest( int32_t index, UBool exec, const char* &name, char* /*par*/ )
{
    if (exec) logln("TestSuite IntlCalendarTest");
    TESTCASE_AUTO_BEGIN;
    TESTCASE_AUTO(TestTypes);
    TESTCASE_AUTO(TestGregorian);
    TESTCASE_AUTO(TestBuddhist);
    TESTCASE_AUTO(TestBug21043Indian);
    TESTCASE_AUTO(TestBug21044Hebrew);
    TESTCASE_AUTO(TestBug21045Islamic);
    TESTCASE_AUTO(TestBug21046IslamicUmalqura);
    TESTCASE_AUTO(TestJapanese);
    TESTCASE_AUTO(TestBuddhistFormat);
    TESTCASE_AUTO(TestJapaneseFormat);
    TESTCASE_AUTO(TestJapanese3860);
    TESTCASE_AUTO(TestForceGannenNumbering);
    TESTCASE_AUTO(TestPersian);
    TESTCASE_AUTO(TestPersianJulianDayToYMD);
    TESTCASE_AUTO(TestPersianYMDToJulianDay);
    TESTCASE_AUTO(TestPersianJan1ToGregorian);
    TESTCASE_AUTO(TestGregorianToPersian);
    TESTCASE_AUTO(TestPersianFormat);
    TESTCASE_AUTO(TestTaiwan);
    TESTCASE_AUTO(TestConsistencyGregorian);
    TESTCASE_AUTO(TestConsistencyCoptic);
    TESTCASE_AUTO(TestConsistencyEthiopic);
    TESTCASE_AUTO(TestConsistencyROC);
    TESTCASE_AUTO(TestConsistencyChinese);
    TESTCASE_AUTO(TestConsistencyDangi);
    TESTCASE_AUTO(TestConsistencyBuddhist);
    TESTCASE_AUTO(TestConsistencyEthiopicAmeteAlem);
    TESTCASE_AUTO(TestConsistencyHebrew);
    TESTCASE_AUTO(TestConsistencyIndian);
    TESTCASE_AUTO(TestConsistencyIslamic);
    TESTCASE_AUTO(TestConsistencyIslamicCivil);
    TESTCASE_AUTO(TestConsistencyIslamicRGSA);
    TESTCASE_AUTO(TestConsistencyIslamicTBLA);
    TESTCASE_AUTO(TestConsistencyIslamicUmalqura);
    TESTCASE_AUTO(TestConsistencyPersian);
    TESTCASE_AUTO(TestConsistencyJapanese);
    TESTCASE_AUTO(TestIslamicUmalquraCalendarSlow);
    TESTCASE_AUTO(TestJapaneseLargeEra);
    TESTCASE_AUTO_END;
}

#undef CASE

// ---------------------------------------------------------------------------------


/**
 * Test various API methods for API completeness.
 */
void
IntlCalendarTest::TestTypes()
{
  Calendar *c = nullptr;
  UErrorCode status = U_ZERO_ERROR;
  int j;
  const char *locs [40] = { "en_US_VALLEYGIRL",     
                            "en_US_VALLEYGIRL@collation=phonebook;calendar=japanese",
                            "en_US_VALLEYGIRL@collation=phonebook;calendar=gregorian",
                            "ja_JP@calendar=japanese",   
                            "th_TH@calendar=buddhist", 
                            "th_TH_TRADITIONAL", 
                            "th_TH_TRADITIONAL@calendar=gregorian", 
                            "en_US",
                            "th_TH",    // Default calendar for th_TH is buddhist
                            "th",       // th's default region is TH and buddhist is used as default for TH
                            "en_TH",    // Default calendar for any locales with region TH is buddhist
                            "en-TH-u-ca-gregory",
                            nullptr };
  const char *types[40] = { "gregorian", 
                            "japanese",
                            "gregorian",
                            "japanese",
                            "buddhist",
                            "buddhist",           
                            "gregorian",
                            "gregorian",
                            "buddhist",           
                            "buddhist",           
                            "buddhist",           
                            "gregorian",
                            nullptr };

  for(j=0;locs[j];j++) {
    logln(UnicodeString("Creating calendar of locale ")  + locs[j]);
    status = U_ZERO_ERROR;
    c = Calendar::createInstance(locs[j], status);
    CHECK(status, "creating '" + UnicodeString(locs[j]) + "' calendar");
    if(U_SUCCESS(status)) {
      logln(UnicodeString(" type is ") + c->getType());
      if(strcmp(c->getType(), types[j])) {
        dataerrln(UnicodeString(locs[j]) + UnicodeString("Calendar type ") + c->getType() + " instead of " + types[j]);
      }
    }
    delete c;
  }
}



/**
 * Run a test of a quasi-Gregorian calendar.  This is a calendar
 * that behaves like a Gregorian but has different year/era mappings.
 * The int[] data array should have the format:
 * 
 * { era, year, gregorianYear, month, dayOfMonth, ...  ... , -1 }
 */
void IntlCalendarTest::quasiGregorianTest(Calendar& cal, const Locale& gcl, const int32_t *data) {
  UErrorCode status = U_ZERO_ERROR;
  // As of JDK 1.4.1_01, using the Sun JDK GregorianCalendar as
  // a reference throws us off by one hour.  This is most likely
  // due to the JDK 1.4 incorporation of historical time zones.
  //java.util.Calendar grego = java.util.Calendar.getInstance();
  Calendar *grego = Calendar::createInstance(gcl, status);
  if (U_FAILURE(status)) {
    dataerrln("Error calling Calendar::createInstance"); 
    return;
  }

  int32_t tz1 = cal.get(UCAL_ZONE_OFFSET,status);
  int32_t tz2 = grego -> get (UCAL_ZONE_OFFSET, status);
  if(tz1 != tz2) { 
    errln(UnicodeString("cal's tz ") + tz1 + " != grego's tz " + tz2);
  }

  for (int32_t i=0; data[i]!=-1; ) {
    int32_t era = data[i++];
    int32_t year = data[i++];
    int32_t gregorianYear = data[i++];
    int32_t month = data[i++];
    int32_t dayOfMonth = data[i++];
    
    grego->clear();
    grego->set(gregorianYear, month, dayOfMonth);
    UDate D = grego->getTime(status);
    
    cal.clear();
    cal.set(UCAL_ERA, era);
    cal.set(year, month, dayOfMonth);
    UDate d = cal.getTime(status);
#ifdef U_DEBUG_DUMPCALS
    logln(UnicodeString("cal  : ") + CalendarTest::calToStr(cal));
    logln(UnicodeString("grego: ") + CalendarTest::calToStr(*grego));
#endif
    if (d == D) {
      logln(UnicodeString("OK: ") + era + ":" + year + "/" + (month+1) + "/" + dayOfMonth +
            " => " + d + " (" + UnicodeString(cal.getType()) + ")");
    } else {
      errln(UnicodeString("Fail: (fields to millis)") + era + ":" + year + "/" + (month+1) + "/" + dayOfMonth +
            " => " + d + ", expected " + D + " (" + UnicodeString(cal.getType()) + "Off by: " + (d-D));
    }
    
    // Now, set the gregorian millis on the other calendar
    cal.clear();
    cal.setTime(D, status);
    int e = cal.get(UCAL_ERA, status);
    int y = cal.get(UCAL_YEAR, status);
#ifdef U_DEBUG_DUMPCALS
    logln(UnicodeString("cal  : ") + CalendarTest::calToStr(cal));
    logln(UnicodeString("grego: ") + CalendarTest::calToStr(*grego));
#endif
    if (y == year && e == era) {
      logln(UnicodeString("OK: ") + D + " => " + cal.get(UCAL_ERA, status) + ":" +
            cal.get(UCAL_YEAR, status) + "/" +
            (cal.get(UCAL_MONTH, status) + 1) + "/" + cal.get(UCAL_DATE, status) + " (" + UnicodeString(cal.getType()) + ")");
    } else {
      errln(UnicodeString("Fail: (millis to fields)") + D + " => " + cal.get(UCAL_ERA, status) + ":" +
            cal.get(UCAL_YEAR, status) + "/" +
            (cal.get(UCAL_MONTH, status)+1) + "/" + cal.get(UCAL_DATE, status) +
            ", expected " + era + ":" + year + "/" + (month+1) + "/" +
            dayOfMonth +  " (" + UnicodeString(cal.getType()));
    }
  }
  delete grego;
  CHECK(status, "err during quasiGregorianTest()");
}

// Verify that Gregorian works like Gregorian
void IntlCalendarTest::TestGregorian() { 
    UDate timeA = Calendar::getNow();
    int32_t data[] = { 
        GregorianCalendar::AD, 1868, 1868, UCAL_SEPTEMBER, 8,
        GregorianCalendar::AD, 1868, 1868, UCAL_SEPTEMBER, 9,
        GregorianCalendar::AD, 1869, 1869, UCAL_JUNE, 4,
        GregorianCalendar::AD, 1912, 1912, UCAL_JULY, 29,
        GregorianCalendar::AD, 1912, 1912, UCAL_JULY, 30,
        GregorianCalendar::AD, 1912, 1912, UCAL_AUGUST, 1,
        -1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1
    };
    
    Calendar *cal;
    UErrorCode status = U_ZERO_ERROR;
    cal = Calendar::createInstance(/*"de_DE", */ status);
    CHECK(status, UnicodeString("Creating de_CH calendar"));
    // Sanity check the calendar 
    UDate timeB = Calendar::getNow();
    UDate timeCal = cal->getTime(status);

    if(!(timeA <= timeCal) || !(timeCal <= timeB)) {
      errln(UnicodeString("Error: Calendar time ") + timeCal +
            " is not within sampled times [" + timeA + " to " + timeB + "]!");
    }
    // end sanity check

    // Note, the following is a good way to test the sanity of the constructed calendars,
    // using Collation as a delay-loop: 
    //
    // $ intltest  format/IntlCalendarTest  collate/G7CollationTest format/IntlCalendarTest

    quasiGregorianTest(*cal,Locale("fr_FR"),data);
    delete cal;
}

/**
 * Verify that BuddhistCalendar shifts years to Buddhist Era but otherwise
 * behaves like GregorianCalendar.
 */
void IntlCalendarTest::TestBuddhist() {
    // BE 2542 == 1999 CE
    UDate timeA = Calendar::getNow();

    int32_t data[] = {
        0,           // B. era   [928479600000]
        2542,        // B. year
        1999,        // G. year
        UCAL_JUNE,   // month
        4,           // day

        0,           // B. era   [-79204842000000]
        3,           // B. year
        -540,        // G. year
        UCAL_FEBRUARY, // month
        12,          // day

        0,           // test month calculation:  4795 BE = 4252 AD is a leap year, but 4795 AD is not.
        4795,        // BE [72018057600000]
        4252,        // AD
        UCAL_FEBRUARY,
        29,

        -1,-1,-1,-1,-1,-1,-1,-1,-1,-1
    };
    Calendar *cal;
    UErrorCode status = U_ZERO_ERROR;
    cal = Calendar::createInstance("th_TH@calendar=buddhist", status);
    CHECK(status, UnicodeString("Creating th_TH@calendar=buddhist calendar"));

    // Sanity check the calendar 
    UDate timeB = Calendar::getNow();
    UDate timeCal = cal->getTime(status);

    if(!(timeA <= timeCal) || !(timeCal <= timeB)) {
      errln(UnicodeString("Error: Calendar time ") + timeCal +
            " is not within sampled times [" + timeA + " to " + timeB + "]!");
    }
    // end sanity check


    quasiGregorianTest(*cal,Locale("th_TH@calendar=gregorian"),data);
    delete cal;
}


/**
 * Verify that TaiWanCalendar shifts years to Minguo Era but otherwise
 * behaves like GregorianCalendar.
 */
void IntlCalendarTest::TestTaiwan() {
    // MG 1 == 1912 AD
    UDate timeA = Calendar::getNow();
    
    // TODO port these to the data items
    int32_t data[] = {
        1,           // B. era   [928479600000]
        1,        // B. year
        1912,        // G. year
        UCAL_JUNE,   // month
        4,           // day

        1,           // B. era   [-79204842000000]
        3,           // B. year
        1914,        // G. year
        UCAL_FEBRUARY, // month
        12,          // day

        1,           // B. era   [-79204842000000]
        96,           // B. year
        2007,        // G. year
        UCAL_FEBRUARY, // month
        12,          // day

        -1,-1,-1,-1,-1,-1,-1,-1,-1,-1
    };
    Calendar *cal;
    UErrorCode status = U_ZERO_ERROR;
    cal = Calendar::createInstance("en_US@calendar=roc", status);
    CHECK(status, UnicodeString("Creating en_US@calendar=roc calendar"));

    // Sanity check the calendar 
    UDate timeB = Calendar::getNow();
    UDate timeCal = cal->getTime(status);

    if(!(timeA <= timeCal) || !(timeCal <= timeB)) {
      errln(UnicodeString("Error: Calendar time ") + timeCal +
            " is not within sampled times [" + timeA + " to " + timeB + "]!");
    }
    // end sanity check


    quasiGregorianTest(*cal,Locale("en_US"),data);
    delete cal;
}



/**
 * Verify that JapaneseCalendar shifts years to Japanese Eras but otherwise
 * behaves like GregorianCalendar.
 */
void IntlCalendarTest::TestJapanese() {
    UDate timeA = Calendar::getNow();
    
    /* Sorry.. japancal.h is private! */
#define JapaneseCalendar_MEIJI  232
#define JapaneseCalendar_TAISHO 233
#define JapaneseCalendar_SHOWA  234
#define JapaneseCalendar_HEISEI 235
    
    // BE 2542 == 1999 CE
    int32_t data[] = { 
        //       Jera         Jyr  Gyear   m             d
        JapaneseCalendar_MEIJI, 1, 1868, UCAL_SEPTEMBER, 8,
        JapaneseCalendar_MEIJI, 1, 1868, UCAL_SEPTEMBER, 9,
        JapaneseCalendar_MEIJI, 2, 1869, UCAL_JUNE, 4,
        JapaneseCalendar_MEIJI, 45, 1912, UCAL_JULY, 29,
        JapaneseCalendar_TAISHO, 1, 1912, UCAL_JULY, 30,
        JapaneseCalendar_TAISHO, 1, 1912, UCAL_AUGUST, 1,
        
        // new tests (not in java)
        JapaneseCalendar_SHOWA,     64,   1989,  UCAL_JANUARY, 7,  // Test current era transition (different code path than others)
        JapaneseCalendar_HEISEI,    1,   1989,  UCAL_JANUARY, 8,   
        JapaneseCalendar_HEISEI,    1,   1989,  UCAL_JANUARY, 9,
        JapaneseCalendar_HEISEI,    1,   1989,  UCAL_DECEMBER, 20,
        JapaneseCalendar_HEISEI,  15,  2003,  UCAL_MAY, 22,
        -1,-1,-1,-1,-1,-1,-1,-1,-1,-1
    };
    
    Calendar *cal;
    UErrorCode status = U_ZERO_ERROR;
    cal = Calendar::createInstance("ja_JP@calendar=japanese", status);
    CHECK(status, UnicodeString("Creating ja_JP@calendar=japanese calendar"));
    // Sanity check the calendar 
    UDate timeB = Calendar::getNow();
    UDate timeCal = cal->getTime(status);

    if(!(timeA <= timeCal) || !(timeCal <= timeB)) {
      errln(UnicodeString("Error: Calendar time ") + timeCal +
            " is not within sampled times [" + timeA + " to " + timeB + "]!");
    }
    // end sanity check
    quasiGregorianTest(*cal,Locale("ja_JP"),data);
    delete cal;
}



void IntlCalendarTest::TestBuddhistFormat() {
    UErrorCode status = U_ZERO_ERROR;
    
    // Test simple parse/format with adopt
    
    // First, a contrived English test..
    UDate aDate = 999932400000.0; 
    SimpleDateFormat fmt(UnicodeString("MMMM d, yyyy G"), Locale("en_US@calendar=buddhist"), status);
    CHECK(status, "creating date format instance");
    SimpleDateFormat fmt2(UnicodeString("MMMM d, yyyy G"), Locale("en_US@calendar=gregorian"), status);
    CHECK(status, "creating gregorian date format instance");
    UnicodeString str;
    fmt2.format(aDate, str);
    logln(UnicodeString() + "Test Date: " + str);
    str.remove();
    fmt.format(aDate, str);
    logln(UnicodeString() + "as Buddhist Calendar: " + escape(str));
    UnicodeString expected("September 8, 2544 BE");
    if(str != expected) {
        errln("Expected " + escape(expected) + " but got " + escape(str));
    }
    UDate otherDate = fmt.parse(expected, status);
    if(otherDate != aDate) { 
        UnicodeString str3;
        fmt.format(otherDate, str3);
        errln("Parse incorrect of " + escape(expected) + " - wanted " + aDate + " but got " +  otherDate + ", " + escape(str3));
    } else {
        logln("Parsed OK: " + expected);
    }
    
    CHECK(status, "Error occurred testing Buddhist Calendar in English ");
    
    status = U_ZERO_ERROR;
    // Now, try in Thai
    {
        UnicodeString expect = CharsToUnicodeString("\\u0E27\\u0E31\\u0E19\\u0E40\\u0E2A\\u0E32\\u0E23\\u0E4C\\u0E17\\u0E35\\u0E48"
            " 8 \\u0E01\\u0E31\\u0e19\\u0e22\\u0e32\\u0e22\\u0e19 \\u0e1e.\\u0e28. 2544");
        UDate         expectDate = 999932400000.0;
        Locale        loc("th_TH_TRADITIONAL"); // legacy
        
        simpleTest(loc, expect, expectDate, status);
    }
    status = U_ZERO_ERROR;
    {
        UnicodeString expect = CharsToUnicodeString("\\u0E27\\u0E31\\u0E19\\u0E40\\u0E2A\\u0E32\\u0E23\\u0E4C\\u0E17\\u0E35\\u0E48"
            " 8 \\u0E01\\u0E31\\u0e19\\u0e22\\u0e32\\u0e22\\u0e19 \\u0e1e.\\u0e28. 2544");
        UDate         expectDate = 999932400000.0;
        Locale        loc("th_TH@calendar=buddhist");
        
        simpleTest(loc, expect, expectDate, status);
    }
    status = U_ZERO_ERROR;
    {
        UnicodeString expect = CharsToUnicodeString("\\u0E27\\u0E31\\u0E19\\u0E40\\u0E2A\\u0E32\\u0E23\\u0E4C\\u0E17\\u0E35\\u0E48"
            " 8 \\u0E01\\u0E31\\u0e19\\u0e22\\u0e32\\u0e22\\u0e19 \\u0e04.\\u0e28. 2001");
        UDate         expectDate = 999932400000.0;
        Locale        loc("th_TH@calendar=gregorian");
        
        simpleTest(loc, expect, expectDate, status);
    }
    status = U_ZERO_ERROR;
    {
        UnicodeString expect = CharsToUnicodeString("\\u0E27\\u0E31\\u0E19\\u0E40\\u0E2A\\u0E32\\u0E23\\u0E4C\\u0E17\\u0E35\\u0E48"
            " 8 \\u0E01\\u0E31\\u0e19\\u0e22\\u0e32\\u0e22\\u0e19 \\u0e04.\\u0e28. 2001");
        UDate         expectDate = 999932400000.0;
        Locale        loc("th_TH_TRADITIONAL@calendar=gregorian");
        
        simpleTest(loc, expect, expectDate, status);
    }
}

// TaiwanFormat has been moved to testdata/format.txt


void IntlCalendarTest::TestJapaneseFormat() {
    LocalPointer<Calendar> cal;
    UErrorCode status = U_ZERO_ERROR;
    cal.adoptInstead(Calendar::createInstance("ja_JP@calendar=japanese", status));
    CHECK(status, UnicodeString("Creating ja_JP@calendar=japanese calendar"));
    
    LocalPointer<Calendar> cal2(cal->clone());
    cal.adoptInstead(nullptr);
    
    // Test simple parse/format with adopt
    
    UDate aDate = 999932400000.0; 
    SimpleDateFormat fmt(UnicodeString("MMMM d, yy G"), Locale("en_US@calendar=japanese"), status);
    SimpleDateFormat fmt2(UnicodeString("MMMM d, yyyy G"), Locale("en_US@calendar=gregorian"), status);
    CHECK(status, "creating date format instance");
    UnicodeString str;
    fmt2.format(aDate, str);
    logln(UnicodeString() + "Test Date: " + str);
    str.remove();
    fmt.format(aDate, str);
    logln(UnicodeString() + "as Japanese Calendar: " + str);
    UnicodeString expected("September 8, 13 Heisei");
    if(str != expected) {
        errln("Expected " + expected + " but got " + str);
    }
    UDate otherDate = fmt.parse(expected, status);
    if(otherDate != aDate) { 
        UnicodeString str3;
        ParsePosition pp;
        fmt.parse(expected, *cal2, pp);
        fmt.format(otherDate, str3);
        errln("Parse incorrect of " + expected + " - wanted " + aDate + " but got " +  " = " +   otherDate + ", " + str3 + " = " + CalendarTest::calToStr(*cal2) );

    } else {
        logln("Parsed OK: " + expected);
    }

    // Test parse with incomplete information
    SimpleDateFormat fmti(UnicodeString("G y"), Locale("en_US@calendar=japanese"), status);
    aDate = -3197117222000.0;
    CHECK(status, "creating date format instance");
    str.remove();
    fmt2.format(aDate, str);
    logln(UnicodeString() + "Test Date: " + str);
    str.remove();
    fmti.format(aDate, str);
    logln(UnicodeString() + "as Japanese Calendar: " + str);
    expected = u"Meiji 1";
    if(str != expected) {
        errln("Expected " + expected + " but got " + str);
    }
    otherDate = fmti.parse(expected, status);
    if(otherDate != aDate) { 
        UnicodeString str3;
        ParsePosition pp;
        fmti.parse(expected, *cal2, pp);
        fmti.format(otherDate, str3);
        errln("Parse incorrect of " + expected + " - wanted " + aDate + " but got " +  " = " +
                otherDate + ", " + str3 + " = " + CalendarTest::calToStr(*cal2) );
    } else {
        logln("Parsed OK: " + expected);
    }

    CHECK(status, "Error occurred");
    
    // Now, try in Japanese
    {
        UnicodeString expect = CharsToUnicodeString("\\u5e73\\u621013\\u5e749\\u67088\\u65e5\\u571f\\u66dc\\u65e5");
        UDate         expectDate = 999932400000.0; // Testing a recent date
        Locale        loc("ja_JP@calendar=japanese");
        
        status = U_ZERO_ERROR;
        simpleTest(loc, expect, expectDate, status);
    }
    {
        UnicodeString expect = CharsToUnicodeString("\\u5e73\\u621013\\u5e749\\u67088\\u65e5\\u571f\\u66dc\\u65e5");
        UDate         expectDate = 999932400000.0; // Testing a recent date
        Locale        loc("ja_JP@calendar=japanese");
        
        status = U_ZERO_ERROR;
        simpleTest(loc, expect, expectDate, status);
    }
    {
        // When era for 1776 is deleted, format result will be different
        UnicodeString expect = CharsToUnicodeString("\\u5b89\\u6c385\\u5e747\\u67084\\u65e5\\u6728\\u66dc\\u65e5");
        UDate         expectDate = -6106032422000.0; // 1776-07-04T00:00:00Z-075258
        Locale        loc("ja_JP@calendar=japanese");
        
        status = U_ZERO_ERROR;
        simpleTest(loc, expect, expectDate, status);    
        
    }
    {   // Jitterbug 1869 - this is an ambiguous era. (Showa 64 = Jan 6 1989, but Showa could be 2 other eras) )
        UnicodeString expect = CharsToUnicodeString("\\u662d\\u548c64\\u5e741\\u67086\\u65e5\\u91d1\\u66dc\\u65e5");
        UDate         expectDate = 600076800000.0;
        Locale        loc("ja_JP@calendar=japanese");
        
        status = U_ZERO_ERROR;
        simpleTest(loc, expect, expectDate, status);    
        
    }
    {   // 1989 Jan 9 Monday = Heisei 1; full is Gy年M月d日EEEE => 平成元年1月9日月曜日
        UnicodeString expect = CharsToUnicodeString("\\u5E73\\u6210\\u5143\\u5E741\\u67089\\u65E5\\u6708\\u66DC\\u65E5");
        UDate         expectDate = 600336000000.0;
        Locale        loc("ja_JP@calendar=japanese");
        
        status = U_ZERO_ERROR;
        simpleTest(loc, expect, expectDate, status);    
        
    }
    {   // This Feb 29th falls on a leap year by gregorian year, but not by Japanese year.
        // When era for 1456 is deleted, format result will be different
        UnicodeString expect = CharsToUnicodeString("\\u5EB7\\u6B632\\u5e742\\u670829\\u65e5\\u65e5\\u66dc\\u65e5");
        UDate         expectDate =  -16214400422000.0;  // 1456-03-09T00:00Z-075258
        Locale        loc("ja_JP@calendar=japanese");
        
        status = U_ZERO_ERROR;
        simpleTest(loc, expect, expectDate, status);    
        
    }
}

void IntlCalendarTest::TestJapanese3860()
{
    LocalPointer<Calendar> cal;
    UErrorCode status = U_ZERO_ERROR;
    cal.adoptInstead(Calendar::createInstance("ja_JP@calendar=japanese", status));
    CHECK(status, UnicodeString("Creating ja_JP@calendar=japanese calendar"));
    LocalPointer<Calendar> cal2(cal->clone());
    SimpleDateFormat fmt2(UnicodeString("HH:mm:ss.S MMMM d, yyyy G"), Locale("en_US@calendar=gregorian"), status);
    UnicodeString str;
    
    {
        // Test simple parse/format with adopt
        UDate aDate = 0; 

        // Test parse with missing era (should default to current era, heisei)
        // Test parse with incomplete information
        logln("Testing parse w/ missing era...");
        SimpleDateFormat fmt(UnicodeString("y/M/d"), Locale("ja_JP@calendar=japanese"), status);
        CHECK(status, "creating date format instance");
        UErrorCode s2 = U_ZERO_ERROR;
        cal2->clear();
        UnicodeString samplestr("1/5/9");
        logln(UnicodeString() + "Test Year: " + samplestr);
        aDate = fmt.parse(samplestr, s2);
        ParsePosition pp=0;
        fmt.parse(samplestr, *cal2, pp);
        CHECK(s2, "parsing the 1/5/9 string");
        logln("*cal2 after 159 parse:");
        str.remove();
        fmt2.format(aDate, str);
        logln(UnicodeString() + "as Gregorian Calendar: " + str);

        cal2->setTime(aDate, s2);
        int32_t gotYear = cal2->get(UCAL_YEAR, s2);
        int32_t gotEra = cal2->get(UCAL_ERA, s2);
        int32_t expectYear = 1;
        int32_t expectEra = JapaneseCalendar::getCurrentEra();
        if((gotYear!=1) || (gotEra != expectEra)) {
            errln(UnicodeString("parse "+samplestr+" of 'y/M/d' as Japanese Calendar, expected year ") + expectYear +
                    UnicodeString(" and era ") + expectEra +", but got year " + gotYear + " and era " + gotEra + " (Gregorian:" + str +")");
        } else {            
            logln(UnicodeString() + " year: " + gotYear + ", era: " + gotEra);
        }
    }
    
    {
        // Test simple parse/format with adopt
        UDate aDate = 0; 

        // Test parse with missing era (should default to current era, heisei)
        // Test parse with incomplete information
        logln("Testing parse w/ just year...");
        SimpleDateFormat fmt(UnicodeString("y"), Locale("ja_JP@calendar=japanese"), status);
        CHECK(status, "creating date format instance");
        UErrorCode s2 = U_ZERO_ERROR;
        cal2->clear();
        UnicodeString samplestr("1");
        logln(UnicodeString() + "Test Year: " + samplestr);
        aDate = fmt.parse(samplestr, s2);
        ParsePosition pp=0;
        fmt.parse(samplestr, *cal2, pp);
        CHECK(s2, "parsing the 1 string");
        logln("*cal2 after 1 parse:");
        str.remove();
        fmt2.format(aDate, str);
        logln(UnicodeString() + "as Gregorian Calendar: " + str);

        cal2->setTime(aDate, s2);
        int32_t gotYear = cal2->get(UCAL_YEAR, s2);
        int32_t gotEra = cal2->get(UCAL_ERA, s2);
        int32_t expectYear = 1;
        int32_t expectEra = JapaneseCalendar::getCurrentEra();
        if((gotYear!=1) || (gotEra != expectEra)) {
            errln(UnicodeString("parse "+samplestr+" of 'y' as Japanese Calendar, expected year ") + expectYear + 
                    UnicodeString(" and era ") + expectEra +", but got year " + gotYear + " and era " + gotEra + " (Gregorian:" + str +")");
        } else {            
            logln(UnicodeString() + " year: " + gotYear + ", era: " + gotEra);
        }
    }    
}

void IntlCalendarTest::TestForceGannenNumbering()
{
    UErrorCode status;
    const char* locID = "ja_JP@calendar=japanese";
    Locale loc(locID);
    UDate refDate = 600336000000.0; // 1989 Jan 9 Monday = Heisei 1
    UnicodeString patText(u"Gy年M月d日",-1);
    UnicodeString patNumr(u"GGGGGy/MM/dd",-1);
    UnicodeString skelText(u"yMMMM",-1);

    // Test Gannen year forcing
    status = U_ZERO_ERROR;
    LocalPointer<SimpleDateFormat> testFmt1(new SimpleDateFormat(patText, loc, status));
    LocalPointer<SimpleDateFormat> testFmt2(new SimpleDateFormat(patNumr, loc, status));
    if (U_FAILURE(status)) {
        dataerrln("Fail in new SimpleDateFormat locale %s: %s", locID, u_errorName(status));
    } else {
        UnicodeString testString1, testString2;
        testString1 = testFmt1->format(refDate, testString1);
        if (testString1.length() < 3 || testString1.charAt(2) != 0x5143) {
            errln(UnicodeString("Formatting year 1 in created text style, got " + testString1 + " but expected 3rd char to be 0x5143"));
        }
        testString2 = testFmt2->format(refDate, testString2);
        if (testString2.length() < 2 || testString2.charAt(1) != 0x0031) {
            errln(UnicodeString("Formatting year 1 in created numeric style, got " + testString2 + " but expected 2nd char to be 1"));
        }
        // Now switch the patterns and verify that Gannen use follows the pattern
        testFmt1->applyPattern(patNumr);
        testString1.remove();
        testString1 = testFmt1->format(refDate, testString1);
        if (testString1.length() < 2 || testString1.charAt(1) != 0x0031) {
            errln(UnicodeString("Formatting year 1 in applied numeric style, got " + testString1 + " but expected 2nd char to be 1"));
        }
        testFmt2->applyPattern(patText);
        testString2.remove();
        testString2 = testFmt2->format(refDate, testString2);
        if (testString2.length() < 3 || testString2.charAt(2) != 0x5143) {
            errln(UnicodeString("Formatting year 1 in applied text style, got " + testString2 + " but expected 3rd char to be 0x5143"));
        }
    }

    // Test disabling of Gannen year forcing
    status = U_ZERO_ERROR;
    LocalPointer<DateTimePatternGenerator> dtpgen(DateTimePatternGenerator::createInstance(loc, status));
    if (U_FAILURE(status)) {
        dataerrln("Fail in DateTimePatternGenerator::createInstance locale %s: %s", locID, u_errorName(status));
    } else {
        UnicodeString pattern = dtpgen->getBestPattern(skelText, status);
        if (U_FAILURE(status)) {
            dataerrln("Fail in DateTimePatternGenerator::getBestPattern locale %s: %s", locID, u_errorName(status));
        } else  {
            // Use override string of ""
            LocalPointer<SimpleDateFormat> testFmt3(new SimpleDateFormat(pattern, UnicodeString(""), loc, status));
            if (U_FAILURE(status)) {
                dataerrln("Fail in new SimpleDateFormat locale %s: %s", locID, u_errorName(status));
            } else {
                UnicodeString testString3;
                testString3 = testFmt3->format(refDate, testString3);
                if (testString3.length() < 3 || testString3.charAt(2) != 0x0031) {
                    errln(UnicodeString("Formatting year 1 with Gannen disabled, got " + testString3 + " but expected 3rd char to be 1"));
                }
            }
        }
    }
}

/**
 * Verify the Persian Calendar.
 */
void IntlCalendarTest::TestPersian() {
    UDate timeA = Calendar::getNow();
    
    Calendar *cal;
    UErrorCode status = U_ZERO_ERROR;
    cal = Calendar::createInstance("fa_IR@calendar=persian", status);
    CHECK(status, UnicodeString("Creating fa_IR@calendar=persian calendar"));
    // Sanity check the calendar 
    UDate timeB = Calendar::getNow();
    UDate timeCal = cal->getTime(status);

    if(!(timeA <= timeCal) || !(timeCal <= timeB)) {
      errln(UnicodeString("Error: Calendar time ") + timeCal +
            " is not within sampled times [" + timeA + " to " + timeB + "]!");
    }
    // end sanity check

    // Test various dates to be sure of validity
    int32_t data[] = { 
        1925, 4, 24, 1304, 2, 4,
        2011, 1, 11, 1389, 10, 21,
        1986, 2, 25, 1364, 12, 6, 
        1934, 3, 14, 1312, 12, 23,

        2090, 3, 19, 1468, 12, 29,
        2007, 2, 22, 1385, 12, 3,
        1969, 12, 31, 1348, 10, 10,
        1945, 11, 12, 1324, 8, 21,
        1925, 3, 31, 1304, 1, 11,

        1996, 3, 19, 1374, 12, 29,
        1996, 3, 20, 1375, 1, 1,
        1997, 3, 20, 1375, 12, 30,
        1997, 3, 21, 1376, 1, 1,

        2008, 3, 19, 1386, 12, 29,
        2008, 3, 20, 1387, 1, 1,
        2004, 3, 19, 1382, 12, 29,
        2004, 3, 20, 1383, 1, 1,

        2006, 3, 20, 1384, 12, 29,
        2006, 3, 21, 1385, 1, 1,

        2005, 4, 20, 1384, 1, 31,
        2005, 4, 21, 1384, 2, 1,
        2005, 5, 21, 1384, 2, 31,
        2005, 5, 22, 1384, 3, 1,
        2005, 6, 21, 1384, 3, 31,
        2005, 6, 22, 1384, 4, 1,
        2005, 7, 22, 1384, 4, 31,
        2005, 7, 23, 1384, 5, 1,
        2005, 8, 22, 1384, 5, 31,
        2005, 8, 23, 1384, 6, 1,
        2005, 9, 22, 1384, 6, 31,
        2005, 9, 23, 1384, 7, 1,
        2005, 10, 22, 1384, 7, 30,
        2005, 10, 23, 1384, 8, 1,
        2005, 11, 21, 1384, 8, 30,
        2005, 11, 22, 1384, 9, 1,
        2005, 12, 21, 1384, 9, 30,
        2005, 12, 22, 1384, 10, 1,
        2006, 1, 20, 1384, 10, 30,
        2006, 1, 21, 1384, 11, 1,
        2006, 2, 19, 1384, 11, 30,
        2006, 2, 20, 1384, 12, 1,
        2006, 3, 20, 1384, 12, 29,
        2006, 3, 21, 1385, 1, 1,

        // The 2820-year cycle arithmetical algorithm would fail this one.
        2025, 3, 21, 1404, 1, 1,
        
        -1,-1,-1,-1,-1,-1,-1,-1,-1,-1
    };

    Calendar *grego = Calendar::createInstance("fa_IR@calendar=gregorian", status);
    for (int32_t i=0; data[i]!=-1; ) {
        int32_t gregYear = data[i++];
        int32_t gregMonth = data[i++]-1;
        int32_t gregDay = data[i++];
        int32_t persYear = data[i++];
        int32_t persMonth = data[i++]-1;
        int32_t persDay = data[i++];
        
        // Test conversion from Persian dates
        grego->clear();
        grego->set(gregYear, gregMonth, gregDay);

        cal->clear();
        cal->set(persYear, persMonth, persDay);

        UDate persTime = cal->getTime(status);
        UDate gregTime = grego->getTime(status);

        if (persTime != gregTime) {
          errln(UnicodeString("Expected ") + gregTime + " but got " + persTime);
        }

        // Test conversion to Persian dates
        cal->clear();
        cal->setTime(gregTime, status);

        int32_t computedYear = cal->get(UCAL_YEAR, status);
        int32_t computedMonth = cal->get(UCAL_MONTH, status);
        int32_t computedDay = cal->get(UCAL_DATE, status);

        if ((persYear != computedYear) ||
            (persMonth != computedMonth) ||
            (persDay != computedDay)) {
          errln(UnicodeString("Expected ") + persYear + "/" + (persMonth+1) + "/" + persDay +
                " but got " +  computedYear + "/" + (computedMonth+1) + "/" + computedDay);
        }

    }

    delete cal;
    delete grego;
}

// Test data copy from
// https://github.com/unicode-org/icu4x/blob/main/components/calendar/src/persian.rs#L299
static struct PersianTestCase1 {
        int32_t rd;
        int32_t year;
        int32_t month;
        int32_t day;
    } persianTestCases1[]{
      {656786, 1178, 1, 1},
      {664224, 1198, 5, 10},
      {671401, 1218, 1, 7},
      {694799, 1282, 1, 29},
      {702806, 1304, 1, 1},
      {704424, 1308, 6, 3},
      {708842, 1320, 7, 7},
      {709409, 1322, 1, 29},
      {709580, 1322, 7, 14},
      {727274, 1370, 12, 27},
      {728714, 1374, 12, 6},
      {739330, 1403, 12, 30},
      {739331, 1404, 1, 1},
      {744313, 1417, 8, 19},
      {763436, 1469, 12, 30},
      {763437, 1470, 1, 1},
      {764652, 1473, 4, 28},
      {775123, 1501, 12, 29},
      {775488, 1502, 12, 29},
      {775487, 1502, 12, 28},
      {775488, 1502, 12, 29},
      {775489, 1503, 1, 1},
      {775490, 1503, 1, 2},
      {1317873, 2987, 12, 29},
      {1317874, 2988, 1, 1},
      {1317875, 2988, 1, 2},
    };

void IntlCalendarTest::TestPersianJulianDayToYMD() {
    UErrorCode status = U_ZERO_ERROR;
    std::unique_ptr<Calendar> cal(Calendar::createInstance(TimeZone::createTimeZone("Asia/Tehran"), "fa_IR@calendar=persian", status));

    for (const auto &testCase : persianTestCases1) {
        status = U_ZERO_ERROR;
        int32_t jday = testCase.rd + 1721425;
        cal->clear();
        cal->set(UCAL_JULIAN_DAY, jday);
        int32_t actualYear = cal->get(UCAL_YEAR, status);
        int32_t actualMonth = cal->get(UCAL_MONTH, status)+1;
        int32_t actualDay = cal->get(UCAL_DAY_OF_MONTH, status);
        if (actualYear != testCase.year || actualMonth != testCase.month || actualDay != testCase.day) {
            errln(UnicodeString("rd ") + testCase.rd + " = jday " + jday + " -> expect Persian(" +
                  testCase.year + "/" + testCase.month + "/" + testCase.day + ") " +
                  "actual Persian(" + actualYear + "/" + actualMonth + "/" + actualDay + ")");
        }
    }
}

void IntlCalendarTest::TestPersianYMDToJulianDay() {
    UErrorCode status = U_ZERO_ERROR;
    std::unique_ptr<Calendar> cal(Calendar::createInstance(TimeZone::createTimeZone("Asia/Tehran"), "fa_IR@calendar=persian", status));

    for (const auto &testCase : persianTestCases1) {
        status = U_ZERO_ERROR;
        cal->clear();
        cal->set(UCAL_YEAR, testCase.year);
        cal->set(UCAL_MONTH, testCase.month-1);
        cal->set(UCAL_DAY_OF_MONTH, testCase.day);
        int32_t actualJday = cal->get(UCAL_JULIAN_DAY, status);
        int32_t actualRD = actualJday - 1721425;
        if (actualRD != testCase.rd) {
            errln(UnicodeString("Persian(") + testCase.year + "/" + testCase.month + "/" + testCase.day + ") => "+
                  "expect rd " + testCase.rd + " but actual jd: " + actualJday + " = rd " + actualRD);
        }
    }
}

// Test data copy from
// https://github.com/unicode-org/icu4x/blob/main/components/calendar/src/persian.rs#L534
// From https://calendar.ut.ac.ir/Fa/News/Data/Doc/KabiseShamsi1206-1498-new.pdf
// Plain text version at https://github.com/roozbehp/persiancalendar/blob/main/kabise.txt
static struct PersianTestCase2 {
        int32_t pYear;
        bool pLeap;
        int32_t year;
        int32_t month;
        int32_t day;
    } persianTestCases2[]{
        {1206, false, 1827, 3, 22},
        {1207, false, 1828, 3, 21},
        {1208, false, 1829, 3, 21},
        {1209, false, 1830, 3, 21},
        {1210, true, 1831, 3, 21},
        {1211, false, 1832, 3, 21},
        {1212, false, 1833, 3, 21},
        {1213, false, 1834, 3, 21},
        {1214, true, 1835, 3, 21},
        {1215, false, 1836, 3, 21},
        {1216, false, 1837, 3, 21},
        {1217, false, 1838, 3, 21},
        {1218, true, 1839, 3, 21},
        {1219, false, 1840, 3, 21},
        {1220, false, 1841, 3, 21},
        {1221, false, 1842, 3, 21},
        {1222, true, 1843, 3, 21},
        {1223, false, 1844, 3, 21},
        {1224, false, 1845, 3, 21},
        {1225, false, 1846, 3, 21},
        {1226, true, 1847, 3, 21},
        {1227, false, 1848, 3, 21},
        {1228, false, 1849, 3, 21},
        {1229, false, 1850, 3, 21},
        {1230, true, 1851, 3, 21},
        {1231, false, 1852, 3, 21},
        {1232, false, 1853, 3, 21},
        {1233, false, 1854, 3, 21},
        {1234, true, 1855, 3, 21},
        {1235, false, 1856, 3, 21},
        {1236, false, 1857, 3, 21},
        {1237, false, 1858, 3, 21},
        {1238, true, 1859, 3, 21},
        {1239, false, 1860, 3, 21},
        {1240, false, 1861, 3, 21},
        {1241, false, 1862, 3, 21},
        {1242, false, 1863, 3, 21},
        {1243, true, 1864, 3, 20},
        {1244, false, 1865, 3, 21},
        {1245, false, 1866, 3, 21},
        {1246, false, 1867, 3, 21},
        {1247, true, 1868, 3, 20},
        {1248, false, 1869, 3, 21},
        {1249, false, 1870, 3, 21},
        {1250, false, 1871, 3, 21},
        {1251, true, 1872, 3, 20},
        {1252, false, 1873, 3, 21},
        {1253, false, 1874, 3, 21},
        {1254, false, 1875, 3, 21},
        {1255, true, 1876, 3, 20},
        {1256, false, 1877, 3, 21},
        {1257, false, 1878, 3, 21},
        {1258, false, 1879, 3, 21},
        {1259, true, 1880, 3, 20},
        {1260, false, 1881, 3, 21},
        {1261, false, 1882, 3, 21},
        {1262, false, 1883, 3, 21},
        {1263, true, 1884, 3, 20},
        {1264, false, 1885, 3, 21},
        {1265, false, 1886, 3, 21},
        {1266, false, 1887, 3, 21},
        {1267, true, 1888, 3, 20},
        {1268, false, 1889, 3, 21},
        {1269, false, 1890, 3, 21},
        {1270, false, 1891, 3, 21},
        {1271, true, 1892, 3, 20},
        {1272, false, 1893, 3, 21},
        {1273, false, 1894, 3, 21},
        {1274, false, 1895, 3, 21},
        {1275, false, 1896, 3, 20},
        {1276, true, 1897, 3, 20},
        {1277, false, 1898, 3, 21},
        {1278, false, 1899, 3, 21},
        {1279, false, 1900, 3, 21},
        {1280, true, 1901, 3, 21},
        {1281, false, 1902, 3, 22},
        {1282, false, 1903, 3, 22},
        {1283, false, 1904, 3, 21},
        {1284, true, 1905, 3, 21},
        {1285, false, 1906, 3, 22},
        {1286, false, 1907, 3, 22},
        {1287, false, 1908, 3, 21},
        {1288, true, 1909, 3, 21},
        {1289, false, 1910, 3, 22},
        {1290, false, 1911, 3, 22},
        {1291, false, 1912, 3, 21},
        {1292, true, 1913, 3, 21},
        {1293, false, 1914, 3, 22},
        {1294, false, 1915, 3, 22},
        {1295, false, 1916, 3, 21},
        {1296, true, 1917, 3, 21},
        {1297, false, 1918, 3, 22},
        {1298, false, 1919, 3, 22},
        {1299, false, 1920, 3, 21},
        {1300, true, 1921, 3, 21},
        {1301, false, 1922, 3, 22},
        {1302, false, 1923, 3, 22},
        {1303, false, 1924, 3, 21},
        {1304, true, 1925, 3, 21},
        {1305, false, 1926, 3, 22},
        {1306, false, 1927, 3, 22},
        {1307, false, 1928, 3, 21},
        {1308, false, 1929, 3, 21},
        {1309, true, 1930, 3, 21},
        {1310, false, 1931, 3, 22},
        {1311, false, 1932, 3, 21},
        {1312, false, 1933, 3, 21},
        {1313, true, 1934, 3, 21},
        {1314, false, 1935, 3, 22},
        {1315, false, 1936, 3, 21},
        {1316, false, 1937, 3, 21},
        {1317, true, 1938, 3, 21},
        {1318, false, 1939, 3, 22},
        {1319, false, 1940, 3, 21},
        {1320, false, 1941, 3, 21},
        {1321, true, 1942, 3, 21},
        {1322, false, 1943, 3, 22},
        {1323, false, 1944, 3, 21},
        {1324, false, 1945, 3, 21},
        {1325, true, 1946, 3, 21},
        {1326, false, 1947, 3, 22},
        {1327, false, 1948, 3, 21},
        {1328, false, 1949, 3, 21},
        {1329, true, 1950, 3, 21},
        {1330, false, 1951, 3, 22},
        {1331, false, 1952, 3, 21},
        {1332, false, 1953, 3, 21},
        {1333, true, 1954, 3, 21},
        {1334, false, 1955, 3, 22},
        {1335, false, 1956, 3, 21},
        {1336, false, 1957, 3, 21},
        {1337, true, 1958, 3, 21},
        {1338, false, 1959, 3, 22},
        {1339, false, 1960, 3, 21},
        {1340, false, 1961, 3, 21},
        {1341, false, 1962, 3, 21},
        {1342, true, 1963, 3, 21},
        {1343, false, 1964, 3, 21},
        {1344, false, 1965, 3, 21},
        {1345, false, 1966, 3, 21},
        {1346, true, 1967, 3, 21},
        {1347, false, 1968, 3, 21},
        {1348, false, 1969, 3, 21},
        {1349, false, 1970, 3, 21},
        {1350, true, 1971, 3, 21},
        {1351, false, 1972, 3, 21},
        {1352, false, 1973, 3, 21},
        {1353, false, 1974, 3, 21},
        {1354, true, 1975, 3, 21},
        {1355, false, 1976, 3, 21},
        {1356, false, 1977, 3, 21},
        {1357, false, 1978, 3, 21},
        {1358, true, 1979, 3, 21},
        {1359, false, 1980, 3, 21},
        {1360, false, 1981, 3, 21},
        {1361, false, 1982, 3, 21},
        {1362, true, 1983, 3, 21},
        {1363, false, 1984, 3, 21},
        {1364, false, 1985, 3, 21},
        {1365, false, 1986, 3, 21},
        {1366, true, 1987, 3, 21},
        {1367, false, 1988, 3, 21},
        {1368, false, 1989, 3, 21},
        {1369, false, 1990, 3, 21},
        {1370, true, 1991, 3, 21},
        {1371, false, 1992, 3, 21},
        {1372, false, 1993, 3, 21},
        {1373, false, 1994, 3, 21},
        {1374, false, 1995, 3, 21},
        {1375, true, 1996, 3, 20},
        {1376, false, 1997, 3, 21},
        {1377, false, 1998, 3, 21},
        {1378, false, 1999, 3, 21},
        {1379, true, 2000, 3, 20},
        {1380, false, 2001, 3, 21},
        {1381, false, 2002, 3, 21},
        {1382, false, 2003, 3, 21},
        {1383, true, 2004, 3, 20},
        {1384, false, 2005, 3, 21},
        {1385, false, 2006, 3, 21},
        {1386, false, 2007, 3, 21},
        {1387, true, 2008, 3, 20},
        {1388, false, 2009, 3, 21},
        {1389, false, 2010, 3, 21},
        {1390, false, 2011, 3, 21},
        {1391, true, 2012, 3, 20},
        {1392, false, 2013, 3, 21},
        {1393, false, 2014, 3, 21},
        {1394, false, 2015, 3, 21},
        {1395, true, 2016, 3, 20},
        {1396, false, 2017, 3, 21},
        {1397, false, 2018, 3, 21},
        {1398, false, 2019, 3, 21},
        {1399, true, 2020, 3, 20},
        {1400, false, 2021, 3, 21},
        {1401, false, 2022, 3, 21},
        {1402, false, 2023, 3, 21},
        {1403, true, 2024, 3, 20},
        {1404, false, 2025, 3, 21},
        {1405, false, 2026, 3, 21},
        {1406, false, 2027, 3, 21},
        {1407, false, 2028, 3, 20},
        {1408, true, 2029, 3, 20},
        {1409, false, 2030, 3, 21},
        {1410, false, 2031, 3, 21},
        {1411, false, 2032, 3, 20},
        {1412, true, 2033, 3, 20},
        {1413, false, 2034, 3, 21},
        {1414, false, 2035, 3, 21},
        {1415, false, 2036, 3, 20},
        {1416, true, 2037, 3, 20},
        {1417, false, 2038, 3, 21},
        {1418, false, 2039, 3, 21},
        {1419, false, 2040, 3, 20},
        {1420, true, 2041, 3, 20},
        {1421, false, 2042, 3, 21},
        {1422, false, 2043, 3, 21},
        {1423, false, 2044, 3, 20},
        {1424, true, 2045, 3, 20},
        {1425, false, 2046, 3, 21},
        {1426, false, 2047, 3, 21},
        {1427, false, 2048, 3, 20},
        {1428, true, 2049, 3, 20},
        {1429, false, 2050, 3, 21},
        {1430, false, 2051, 3, 21},
        {1431, false, 2052, 3, 20},
        {1432, true, 2053, 3, 20},
        {1433, false, 2054, 3, 21},
        {1434, false, 2055, 3, 21},
        {1435, false, 2056, 3, 20},
        {1436, true, 2057, 3, 20},
        {1437, false, 2058, 3, 21},
        {1438, false, 2059, 3, 21},
        {1439, false, 2060, 3, 20},
        {1440, false, 2061, 3, 20},
        {1441, true, 2062, 3, 20},
        {1442, false, 2063, 3, 21},
        {1443, false, 2064, 3, 20},
        {1444, false, 2065, 3, 20},
        {1445, true, 2066, 3, 20},
        {1446, false, 2067, 3, 21},
        {1447, false, 2068, 3, 20},
        {1448, false, 2069, 3, 20},
        {1449, true, 2070, 3, 20},
        {1450, false, 2071, 3, 21},
        {1451, false, 2072, 3, 20},
        {1452, false, 2073, 3, 20},
        {1453, true, 2074, 3, 20},
        {1454, false, 2075, 3, 21},
        {1455, false, 2076, 3, 20},
        {1456, false, 2077, 3, 20},
        {1457, true, 2078, 3, 20},
        {1458, false, 2079, 3, 21},
        {1459, false, 2080, 3, 20},
        {1460, false, 2081, 3, 20},
        {1461, true, 2082, 3, 20},
        {1462, false, 2083, 3, 21},
        {1463, false, 2084, 3, 20},
        {1464, false, 2085, 3, 20},
        {1465, true, 2086, 3, 20},
        {1466, false, 2087, 3, 21},
        {1467, false, 2088, 3, 20},
        {1468, false, 2089, 3, 20},
        {1469, true, 2090, 3, 20},
        {1470, false, 2091, 3, 21},
        {1471, false, 2092, 3, 20},
        {1472, false, 2093, 3, 20},
        {1473, false, 2094, 3, 20},
        {1474, true, 2095, 3, 20},
        {1475, false, 2096, 3, 20},
        {1476, false, 2097, 3, 20},
        {1477, false, 2098, 3, 20},
        {1478, true, 2099, 3, 20},
        {1479, false, 2100, 3, 21},
        {1480, false, 2101, 3, 21},
        {1481, false, 2102, 3, 21},
        {1482, true, 2103, 3, 21},
        {1483, false, 2104, 3, 21},
        {1484, false, 2105, 3, 21},
        {1485, false, 2106, 3, 21},
        {1486, true, 2107, 3, 21},
        {1487, false, 2108, 3, 21},
        {1488, false, 2109, 3, 21},
        {1489, false, 2110, 3, 21},
        {1490, true, 2111, 3, 21},
        {1491, false, 2112, 3, 21},
        {1492, false, 2113, 3, 21},
        {1493, false, 2114, 3, 21},
        {1494, true, 2115, 3, 21},
        {1495, false, 2116, 3, 21},
        {1496, false, 2117, 3, 21},
        {1497, false, 2118, 3, 21},
        {1498, true, 2119, 3, 21},
    };

void IntlCalendarTest::TestPersianJan1ToGregorian() {
    UErrorCode status = U_ZERO_ERROR;
    std::unique_ptr<Calendar> gcal(Calendar::createInstance(TimeZone::createTimeZone("Asia/Tehran"), "en", status));
    std::unique_ptr<Calendar> cal(Calendar::createInstance(TimeZone::createTimeZone("Asia/Tehran"), "fa_IR@calendar=persian", status));

    for (const auto &testCase : persianTestCases2) {
        status = U_ZERO_ERROR;
        cal->clear();
        cal->set(UCAL_YEAR, testCase.pYear);
        cal->set(UCAL_MONTH, 0);
        cal->set(UCAL_DAY_OF_MONTH, 1);
        gcal->setTime(cal->getTime(status), status);
        int32_t actualYear =  gcal->get(UCAL_YEAR, status);
        int32_t actualMonth =  gcal->get(UCAL_MONTH, status)+1;
        int32_t actualDay =  gcal->get(UCAL_DAY_OF_MONTH, status);
        if (actualYear != testCase.year || actualMonth != testCase.month || actualDay != testCase.day) {
            errln(UnicodeString("Persian(") + testCase.pYear + ", 1, 1) => " +
                  "expect Gregorian(" + testCase.year + "/" +  testCase.month + "/" + testCase.day + ") " +
                  "actual Gregorian(" + actualYear + "/" + actualMonth + "/" + actualDay + ")");
        }
    }
}

void IntlCalendarTest::TestGregorianToPersian() {
    UErrorCode status = U_ZERO_ERROR;
    std::unique_ptr<Calendar> gcal(Calendar::createInstance(TimeZone::createTimeZone("Asia/Tehran"), "en", status));
    std::unique_ptr<Calendar> cal(Calendar::createInstance(TimeZone::createTimeZone("Asia/Tehran"), "fa_IR@calendar=persian", status));

    for (const auto &testCase : persianTestCases2) {
        status = U_ZERO_ERROR;
        gcal->clear();
        gcal->set(UCAL_YEAR, testCase.year);
        gcal->set(UCAL_MONTH, testCase.month-1);
        gcal->set(UCAL_DAY_OF_MONTH, testCase.day);
        cal->setTime(gcal->getTime(status), status);
        int32_t persianYear =  cal->get(UCAL_YEAR, status);
        int32_t persianMonth =  cal->get(UCAL_MONTH, status)+1;
        int32_t persianDay =  cal->get(UCAL_DAY_OF_MONTH, status);
        if (persianYear != testCase.pYear || persianMonth != 1 || persianDay != 1) {
            errln(UnicodeString("Gregorian(") + testCase.year + "/" + testCase.month + "/" + testCase.day + ") "+
                  " => expect Persian(" + testCase.pYear + "/1/1) actual " +
                  "Persian(" + persianYear + "/" + persianMonth + "/" + persianDay + ")");
        }
    }
}

void IntlCalendarTest::TestPersianFormat() {
    UErrorCode status = U_ZERO_ERROR;
    SimpleDateFormat fmt(UnicodeString("MMMM d, yyyy G"), Locale(" en_US@calendar=persian"), status);
    CHECK(status, "creating date format instance");
    SimpleDateFormat fmt2(UnicodeString("MMMM d, yyyy G"), Locale("en_US@calendar=gregorian"), status);
    CHECK(status, "creating gregorian date format instance");
    UnicodeString gregorianDate("January 18, 2007 AD");
    UDate aDate = fmt2.parse(gregorianDate, status); 
    UnicodeString str;
    fmt.format(aDate, str);
    logln(UnicodeString() + "as Persian Calendar: " + escape(str));
    UnicodeString expected("Dey 28, 1385 AP");
    if(str != expected) {
        errln("Expected " + escape(expected) + " but got " + escape(str));
    }
    UDate otherDate = fmt.parse(expected, status); 
    if(otherDate != aDate) { 
        UnicodeString str3;
        fmt.format(otherDate, str3);
        errln("Parse incorrect of " + escape(expected) + " - wanted " + aDate + " but got " +  otherDate + ", " + escape(str3)); 
    } else {
        logln("Parsed OK: " + expected);
    }
    // Two digit year parsing problem #4732
    fmt.applyPattern("yy-MM-dd");
    str.remove();
    fmt.format(aDate, str);
    expected.setTo("85-10-28");
    if(str != expected) {
        errln("Expected " + escape(expected) + " but got " + escape(str));
    }
    otherDate = fmt.parse(expected, status);
    if (otherDate != aDate) {
        errln("Parse incorrect of " + escape(expected) + " - wanted " + aDate + " but got " + otherDate); 
    } else {
        logln("Parsed OK: " + expected);
    }

    CHECK(status, "Error occurred testing Persian Calendar in English "); 
}

void IntlCalendarTest::TestConsistencyGregorian() {
    checkConsistency("en@calendar=gregorian");
}
void IntlCalendarTest::TestConsistencyIndian() {
    checkConsistency("en@calendar=indian");
}
void IntlCalendarTest::TestConsistencyHebrew() {
    checkConsistency("en@calendar=hebrew");
}
void IntlCalendarTest::TestConsistencyIslamic() {
    checkConsistency("en@calendar=islamic");
}
void IntlCalendarTest::TestConsistencyIslamicRGSA() {
    checkConsistency("en@calendar=islamic-rgsa");
}
void IntlCalendarTest::TestConsistencyIslamicTBLA() {
    checkConsistency("en@calendar=islamic-tbla");
}
void IntlCalendarTest::TestConsistencyIslamicUmalqura() {
    checkConsistency("en@calendar=islamic-umalqura");
}
void IntlCalendarTest::TestConsistencyIslamicCivil() {
    checkConsistency("en@calendar=islamic-civil");
}
void IntlCalendarTest::TestConsistencyCoptic() {
    checkConsistency("en@calendar=coptic");
}
void IntlCalendarTest::TestConsistencyEthiopic() {
    checkConsistency("en@calendar=ethiopic");
}
void IntlCalendarTest::TestConsistencyROC() {
    checkConsistency("en@calendar=roc");
}
void IntlCalendarTest::TestConsistencyChinese() {
    checkConsistency("en@calendar=chinese");
}
void IntlCalendarTest::TestConsistencyDangi() {
    checkConsistency("en@calendar=dangi");
}
void IntlCalendarTest::TestConsistencyPersian() {
    checkConsistency("en@calendar=persian");
}
void IntlCalendarTest::TestConsistencyBuddhist() {
    checkConsistency("en@calendar=buddhist");
}
void IntlCalendarTest::TestConsistencyJapanese() {
    checkConsistency("en@calendar=japanese");
}
void IntlCalendarTest::TestConsistencyEthiopicAmeteAlem() {
    checkConsistency("en@calendar=ethiopic-amete-alem");
}
void IntlCalendarTest::checkConsistency(const char* locale) {
    // Check 2.5 years in quick mode and 6000 years in exhaustive mode.
    int32_t numOfDaysToTest = static_cast<int32_t>((quick ? 2.5 : 6000) * 365);
    constexpr int32_t msInADay = 1000*60*60*24;
    std::string msg("TestConsistency");
    IcuTestErrorCode status(*this, (msg + locale).c_str());
    // g is just for debugging messages.
    std::unique_ptr<Calendar> g(Calendar::createInstance("en", status));
    g->setTimeZone(*(TimeZone::getGMT()));
    std::unique_ptr<Calendar> base(Calendar::createInstance(locale, status));
    if (status.errIfFailureAndReset("Cannot create calendar %s", locale)) {
        return;
    }
    const char* type = base->getType();
    // Do not ignore in quick mode
    bool ignoreOrdinaryMonth12Bug = (!quick) && (strcmp("chinese", type) == 0 || strcmp("dangi", type) == 0);
    UDate test = Calendar::getNow();
    base->setTimeZone(*(TimeZone::getGMT()));
    int32_t j;
    int lastDay = 1;
    std::unique_ptr<Calendar> r(base->clone());
    for (j = 0; j < numOfDaysToTest; j++, test -= msInADay) {
        status.errIfFailureAndReset();
        g->setTime(test, status);
        if (status.errIfFailureAndReset("Cannot set time")) {
            return;
        }
        base->clear();
        base->setTime(test, status);
        if (status.errIfFailureAndReset("Cannot set time")) {
            return;
        }
        // First, we verify the date from base is decrease one day from the
        // last day unless the last day is 1.
        int32_t cday = base->get(UCAL_DATE, status);
        if (U_FAILURE(status)) {
           UErrorCode localStatus = U_ZERO_ERROR;
           if (status.errIfFailureAndReset(
               "Cannot get the %dth date for %f %d %d/%d/%d\n",
               j,
               test,
               g->get(UCAL_ERA, localStatus),
               g->get(UCAL_YEAR, localStatus),
               (g->get(UCAL_MONTH, localStatus) + 1),
               g->get(UCAL_DATE, localStatus))) {
               return;
           }
        }
        if (lastDay == 1) {
            lastDay = cday;
        } else {
            if (cday != lastDay-1) {
                // Ignore if it is the last day before Gregorian Calendar switch on
                // 1582 Oct 4
                if (g->get(UCAL_YEAR, status) == 1582 &&
                    (g->get(UCAL_MONTH, status) + 1) == 10 &&
                    g->get(UCAL_DATE, status) == 4) {
                    lastDay = 5;
                } else {
                    errln(UnicodeString(
                        "Day is not one less from previous date for "
                        "Gregorian(e=") + g->get(UCAL_ERA, status) + " " +
                        g->get(UCAL_YEAR, status) + "/" +
                        (g->get(UCAL_MONTH, status) + 1) + "/" +
                        g->get(UCAL_DATE, status) + ") " + locale + "(" +
                        base->get(UCAL_ERA, status) + " " +
                        base->get(UCAL_YEAR, status) + "/" +
                        (base->get(UCAL_MONTH, status) + 1 ) + "/" +
                        base->get(UCAL_DATE, status) + ")");
                    status.errIfFailureAndReset();
                    return;
                }
            }
            lastDay--;
        }
        // Second, we verify the month is in reasonale range.
        int32_t cmonth = base->get(UCAL_MONTH, status);
        if (cmonth < 0 || cmonth > 13) {
            errln(UnicodeString(
                "Month is out of range Gregorian(e=") +
                g->get(UCAL_ERA, status) + " " +
                g->get(UCAL_YEAR, status) + "/" +
                (g->get(UCAL_MONTH, status) + 1) + "/" +
                g->get(UCAL_DATE, status) + ") " + locale + "(" +
                base->get(UCAL_ERA, status) + " " +
                base->get(UCAL_YEAR, status) + "/" +
                (base->get(UCAL_MONTH, status) + 1 ) + "/" +
                base->get(UCAL_DATE, status) + ")");
            status.errIfFailureAndReset();
            return;
        }
        // Third, we verify the set function can round trip the time back.
        r->clear();
        for (int32_t f = 0; f < UCAL_FIELD_COUNT; f++) {
            UCalendarDateFields ut = static_cast<UCalendarDateFields>(f);
            r->set(ut, base->get(ut, status));
        }
        UDate result = r->getTime(status);
        if (status.errIfFailureAndReset("Cannot get time %s", locale)) {
            return;
        }
        if (test != result) {
            if (ignoreOrdinaryMonth12Bug &&
                base->get(UCAL_ORDINAL_MONTH, status) == 12) {
                logKnownIssue("ICU-22230", "Problem December in Leap Year");
                status.reset();
                continue;
            }
            int32_t year = base->get(UCAL_YEAR, status);
            int32_t month = base->get(UCAL_MONTH, status) + 1;
            int32_t date = base->get(UCAL_DATE, status);

            errln(UnicodeString("Round trip conversion produces different "
                  "time from ") + test + " to  " + result + " delta: " +
                  (result - test) +
                  " Gregorian(e=" + g->get(UCAL_ERA, status) + " " +
                  g->get(UCAL_YEAR, status) + "/" +
                  (g->get(UCAL_MONTH, status) + 1) + "/" +
                  g->get(UCAL_DATE, status) + ") \n" +
                  " Calendar[" + base->getType() +
                  "](e=" + base->get(UCAL_ERA, status) + " " +
                  year + "/" + month + "/" + date +
                  ") ordinalMonth=" +
                  base->get(UCAL_ORDINAL_MONTH, status));
            status.errIfFailureAndReset();
        }
    }
}

void IntlCalendarTest::TestIslamicUmalquraCalendarSlow() {
    IcuTestErrorCode status(*this, "TestIslamicUmalquraCalendarSlow");
    Locale l("th@calendar=islamic-umalqura");
    std::unique_ptr<Calendar> cal(
        Calendar::createInstance(l, status));
    cal->add(UCAL_YEAR, 1229080905, status);
    cal->roll(UCAL_WEEK_OF_MONTH, 1499050699, status);
    cal->fieldDifference(0.000000, UCAL_YEAR_WOY, status);
    // Ignore the error
    status.reset();
}

void IntlCalendarTest::TestJapaneseLargeEra() {
    IcuTestErrorCode status(*this, "TestJapaneseLargeEra");
    Locale l("ja@calendar=japanese");
    std::unique_ptr<Calendar> cal(
        Calendar::createInstance(l, status));
    cal->clear();
    cal->set(UCAL_ERA, 2139062143);
    cal->add(UCAL_YEAR, 1229539657, status);
    status.expectErrorAndReset(U_ILLEGAL_ARGUMENT_ERROR);
}

void IntlCalendarTest::simpleTest(const Locale& loc, const UnicodeString& expect, UDate expectDate, UErrorCode& status)
{
    UnicodeString tmp;
    UDate         d;
    DateFormat *fmt0 = DateFormat::createDateTimeInstance(DateFormat::kFull, DateFormat::kFull);

    logln("Try format/parse of " + UnicodeString(loc.getName()));
    DateFormat *fmt2 = DateFormat::createDateInstance(DateFormat::kFull, loc);
    if(fmt2) { 
        fmt2->format(expectDate, tmp);
        logln(escape(tmp) + " ( in locale " + loc.getName() + ")");
        if(tmp != expect) {
            errln(UnicodeString("Failed to format " ) + loc.getName() + " expected " + escape(expect) + " got " + escape(tmp) );
        }

        d = fmt2->parse(expect,status);
        CHECK(status, "Error occurred parsing " + UnicodeString(loc.getName()));
        if(d != expectDate) {
            fmt2->format(d,tmp);
            errln(UnicodeString("Failed to parse ") + escape(expect) + ", " + loc.getName() + " expect " + static_cast<double>(expectDate) + " got " + static_cast<double>(d) + " " + escape(tmp));
            logln( "wanted " + escape(fmt0->format(expectDate,tmp.remove())) + " but got " + escape(fmt0->format(d,tmp.remove())));
        }
        delete fmt2;
    } else {
        errln(UnicodeString("Can't create ") + loc.getName() + " date instance");
    }
    delete fmt0;
}

void IntlCalendarTest::TestBug21043Indian() {
    IcuTestErrorCode status(*this, "TestBug21043Indian");
    std::unique_ptr<Calendar> cal(
        Calendar::createInstance("en@calendar=indian", status));
    std::unique_ptr<Calendar> g(
        Calendar::createInstance("en@calendar=gregorian", status));
    // set to 10 BC
    g->set(UCAL_ERA, 0);
    g->set(UCAL_YEAR, 10);
    g->set(UCAL_MONTH, 1);
    g->set(UCAL_DATE, 1);
    cal->setTime(g->getTime(status), status);
    int32_t m = cal->get(UCAL_MONTH, status);
    if (m < 0 || m > 11) {
        errln(
              u"Month should be between 0 and 11 in India calendar");
    }
}

void IntlCalendarTest::TestBug21044Hebrew() {
    IcuTestErrorCode status(*this, "TestBug21044Hebrew");
    std::unique_ptr<Calendar> cal(
        Calendar::createInstance("en@calendar=hebrew", status));
    std::unique_ptr<Calendar> g(
        Calendar::createInstance("en@calendar=gregorian", status));
    // set to 3771/10/27 BC which is before 3760 BC.
    g->set(UCAL_ERA, 0);
    g->set(UCAL_YEAR, 3771);
    g->set(UCAL_MONTH, 9);
    g->set(UCAL_DATE, 27);
    cal->setTime(g->getTime(status), status);

    if (status.errIfFailureAndReset(
        "Cannot set date. Got error %s", u_errorName(status))) {
        return;
    }
    int32_t y = cal->get(UCAL_YEAR, status);
    int32_t m = cal->get(UCAL_MONTH, status);
    int32_t d = cal->get(UCAL_DATE, status);
    if (status.errIfFailureAndReset(
        "Cannot get date. Got error %s", u_errorName(status))) {
        return;
   }
    if (y > 0 || m < 0 || m > 12 || d < 0 || d > 32) {
        errln(UnicodeString("Out of rage!\nYear ") + y + " should be " +
              "negative number before 1AD.\nMonth " + m + " should " +
              "be between 0 and 12 in Hebrew calendar.\nDate " + d +
              " should be between 0 and 32 in Islamic calendar.");
    }
}

void IntlCalendarTest::TestBug21045Islamic() {
    IcuTestErrorCode status(*this, "TestBug21045Islamic");
    std::unique_ptr<Calendar> cal(
        Calendar::createInstance("en@calendar=islamic", status));
    std::unique_ptr<Calendar> g(
        Calendar::createInstance("en@calendar=gregorian", status));
    // set to 500 AD before 622 AD.
    g->set(UCAL_ERA, 1);
    g->set(UCAL_YEAR, 500);
    g->set(UCAL_MONTH, 1);
    g->set(UCAL_DATE, 1);
    cal->setTime(g->getTime(status), status);
    int32_t m = cal->get(UCAL_MONTH, status);
    if (m < 0 || m > 11) {
        errln(u"Month should be between 1 and 12 in Islamic calendar");
    }
}

void IntlCalendarTest::TestBug21046IslamicUmalqura() {
    IcuTestErrorCode status(*this, "TestBug21046IslamicUmalqura");
    std::unique_ptr<Calendar> cal(
        Calendar::createInstance("en@calendar=islamic-umalqura", status));
    std::unique_ptr<Calendar> g(
        Calendar::createInstance("en@calendar=gregorian", status));
    // set to 195366 BC
    g->set(UCAL_ERA, 0);
    g->set(UCAL_YEAR, 195366);
    g->set(UCAL_MONTH, 1);
    g->set(UCAL_DATE, 1);
    cal->setTime(g->getTime(status), status);
    int32_t y = cal->get(UCAL_YEAR, status);
    int32_t m = cal->get(UCAL_MONTH, status);
    int32_t d = cal->get(UCAL_DATE, status);
    if (y > 0 || m < 0 || m > 11 || d < 0 || d > 32) {
        errln(UnicodeString("Out of rage!\nYear ") + y + " should be " +
              "negative number before 1AD.\nMonth " + m + " should " +
              "be between 0 and 11 in Islamic calendar.\nDate " + d +
              " should be between 0 and 32 in Islamic calendar.");
    }
}
#undef CHECK

#endif /* #if !UCONFIG_NO_FORMATTING */

//eof
