/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2003, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

/* Test Internationalized Calendars for C++ */

#include "unicode/utypes.h"
#include "string.h"
#include "unicode/locid.h"

#if !UCONFIG_NO_FORMATTING

#define CHECK(status, msg) \
    if (U_FAILURE(status)) { \
      errln((UnicodeString(u_errorName(status)) + UnicodeString("err " ) )+ msg); \
        return; \
    }


#include "incaltst.h"
#include "unicode/gregocal.h"
#include "unicode/smpdtfmt.h"
#include "unicode/simpletz.h"
 
// *****************************************************************************
// class IntlCalendarTest
// *****************************************************************************

static UnicodeString fieldName(UCalendarDateFields f);

static UnicodeString calToStr(const Calendar & cal)
{

  UnicodeString out;
  UErrorCode status;
  int i;
  for(i = 0;i<UCAL_FIELD_COUNT;i++) {
    out += (UnicodeString("+") + fieldName((UCalendarDateFields)i) + "=" +  cal.get((UCalendarDateFields)i, status) + UnicodeString(", "));
  }
  out += UnicodeString(cal.getType());

  out += cal.inDaylightTime(status)?UnicodeString("DAYLIGHT"):UnicodeString("NORMAL");

  UnicodeString str2;
  out += cal.getTimeZone().getDisplayName(str2);

  return out;
}

void IntlCalendarTest::runIndexedTest( int32_t index, UBool exec, const char* &name, char* /*par*/ )
{
    if (exec) logln("TestSuite IntlCalendarTest");
    switch (index) {
        case 0:
            name = "TestTypes";
            if (exec) {
                logln("TestTypes---"); logln("");
                TestTypes();
            }
            break;
        case 1:
            name = "TestBuddhist";
            if (exec) {
                logln("TestBuddhist---"); logln("");
                TestBuddhist();
            }
            break;
        default: name = ""; break;
    }
}

// ---------------------------------------------------------------------------------

static UnicodeString fieldName(UCalendarDateFields f) {
    switch (f) {
    case UCAL_ERA:           return "ERA";
    case UCAL_YEAR:          return "YEAR";
    case UCAL_MONTH:         return "MONTH";
    case UCAL_WEEK_OF_YEAR:  return "WEEK_OF_YEAR";
    case UCAL_WEEK_OF_MONTH: return "WEEK_OF_MONTH";
    case UCAL_DATE:			 return "DAY_OF_MONTH"; // DATE is synonym for DAY_OF_MONTH
    case UCAL_DAY_OF_YEAR:   return "DAY_OF_YEAR";
    case UCAL_DAY_OF_WEEK:   return "DAY_OF_WEEK";
    case UCAL_DAY_OF_WEEK_IN_MONTH: return "DAY_OF_WEEK_IN_MONTH";
    case UCAL_AM_PM:         return "AM_PM";
    case UCAL_HOUR:          return "HOUR";
    case UCAL_HOUR_OF_DAY:   return "HOUR_OF_DAY";
    case UCAL_MINUTE:        return "MINUTE";
    case UCAL_SECOND:        return "SECOND";
    case UCAL_MILLISECOND:   return "MILLISECOND";
    case UCAL_ZONE_OFFSET:   return "ZONE_OFFSET";
    case UCAL_DST_OFFSET:    return "DST_OFFSET";
    case UCAL_YEAR_WOY:      return "YEAR_WOY";
    case UCAL_DOW_LOCAL:     return "DOW_LOCAL";
    case UCAL_FIELD_COUNT:   return "FIELD_COUNT";
    default:
        return UnicodeString("") + ((int32_t)f);
    }
}

/**
 * Test various API methods for API completeness.
 */
void
IntlCalendarTest::TestTypes()
{
  Calendar *c = NULL;
  UErrorCode status = U_ZERO_ERROR;
  int j;
  const char *locs [40] = { "en_US_VALLEYGIRL",     
#if 0
                            "ja_JP_TRADITIONAL",   
#endif
                            "th_TH_TRADITIONAL", 
                            "en_US", NULL };
  const char *types[40] = { "gregorian", 
#if 0
                            "japanese",
#endif
                            "buddhist",           
                            "gregorian", NULL };

  for(j=0;locs[j];j++) {
    logln(UnicodeString("Creating calendar of locale ")  + locs[j]);
    status = U_ZERO_ERROR;
    c = Calendar::createInstance(locs[j], status);
    CHECK(status, "creating '" + UnicodeString(locs[j]) + "' calendar");
    if(U_SUCCESS(status)) {
      logln(UnicodeString(" type is ") + c->getType());
      if(strcmp(c->getType(), types[j])) {
        errln(UnicodeString(locs[j]) + UnicodeString("Calendar type ") + c->getType() + " instead of " + types[j]);
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

  int32_t tz1 = cal.get(UCAL_ZONE_OFFSET,status);
  int32_t tz2 = grego -> get (UCAL_ZONE_OFFSET, status);
  if(tz1 != tz2) { 
    errln((UnicodeString)"cal's tz " + tz1 + " != grego's tz " + tz2);
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
    //    logln((UnicodeString)"cal  : " + calToStr(cal));
    //    logln((UnicodeString)"grego: " + calToStr(*grego));
    if (d == D) {
      logln(UnicodeString("OK: ") + era + ":" + year + "/" + (month+1) + "/" + dayOfMonth +
            " => " + d + " (" + UnicodeString(cal.getType()));
    } else {
      errln(UnicodeString("Fail: ") + era + ":" + year + "/" + (month+1) + "/" + dayOfMonth +
            " => " + d + ", expected " + D + " (" + UnicodeString(cal.getType()) + "Off by: " + (d-D));
    }
    
    // Now, set the gregorian millis on the other calendar
    cal.clear();
    cal.setTime(D, status);
    int e = cal.get(UCAL_ERA, status);
    int y = cal.get(UCAL_YEAR, status);
    //logln((UnicodeString)"cal  : " + calToStr(cal));
    //logln((UnicodeString)"grego: " + calToStr(*grego));
    if (y == year && e == era) {
      logln((UnicodeString)"OK: " + D + " => " + cal.get(UCAL_ERA, status) + ":" +
            cal.get(UCAL_YEAR, status) + "/" +
            (cal.get(UCAL_MONTH, status)+1) + "/" + cal.get(UCAL_DATE, status) +  " (" + UnicodeString(cal.getType()));
    } else {
      errln((UnicodeString)"Fail: " + D + " => " + cal.get(UCAL_ERA, status) + ":" +
            cal.get(UCAL_YEAR, status) + "/" +
            (cal.get(UCAL_MONTH, status)+1) + "/" + cal.get(UCAL_DATE, status) +
            ", expected " + era + ":" + year + "/" + (month+1) + "/" +
            dayOfMonth +  " (" + UnicodeString(cal.getType()));
    }
  }
  CHECK(status, "err during quasiGregorianTest()");
}

/**
 * Verify that BuddhistCalendar shifts years to Buddhist Era but otherwise
 * behaves like GregorianCalendar.
 */
void IntlCalendarTest::TestBuddhist() {
  // BE 2542 == 1999 CE
  int32_t data[] = { 0,           // B. era
                     2542,        // B. year
                     1999,        // G. year
                     UCAL_JUNE,   // month
                     4,           // day

                     0,           // B. era
                     3,        // B. year
                     -540,        // G. year
                     UCAL_FEBRUARY,   // month
                     12,           // day
                     -1,-1,-1,-1,-1,-1,-1,-1,-1,-1 };
  Calendar *cal;
  UErrorCode status = U_ZERO_ERROR;
  cal = Calendar::createInstance("th_TH_TRADITIONAL", status);
  CHECK(status, UnicodeString("Creating th_TH_TRADITIONAL calendar"));
  quasiGregorianTest(*cal,Locale("th_TH"),data);

  // Test simple parse/format with adopt
  
  UDate aDate = 999932400000.0; 
  SimpleDateFormat *fmt = new SimpleDateFormat(UnicodeString("MMMM d, yyyy G"), Locale("en_US"), status);
  CHECK(status, "creating date format instance");
  if(!fmt) { 
    errln("Coudln't create en_US instance");
  } else {
    UnicodeString str;
    fmt->format(aDate, str);
    logln(UnicodeString() + "Test Date: " + str);
    str.remove();
    fmt->adoptCalendar(cal);
    cal = NULL;
    fmt->format(aDate, str);
    logln(UnicodeString() + "as Buddhist Calendar: " + str);
    UnicodeString expected("September 8, 2544 BE");
    if(str != expected) {
      errln("Expected " + expected + " but got " + str);
    }
    UDate otherDate = fmt->parse(expected, status);
    if(otherDate != aDate) { 
      UnicodeString str3;
      fmt->format(otherDate, str3);
      errln("Parse incorrect of " + expected + " - wanted " + aDate + " but got " +  otherDate + ", " + str3);
    } else {
      logln("Parsed OK: " + expected);
    }
    delete fmt;
  }
  CHECK(status, "Error occured");
}


#undef CHECK

#endif /* #if !UCONFIG_NO_FORMATTING */

//eof
