/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 2003, International Business Machines Corporation 
 * and others. All Rights Reserved.
 ********************************************************************
 * Calendar Case Test is a type of CalendarTest which compares the 
 * behavior of a calendar to a certain set of 'test cases', involving
 * conversion between julian-day to fields and vice versa.
 ********************************************************************/

#include "calcasts.h"

#if !UCONFIG_NO_FORMATTING
// ======= 'Main' ===========================

#define CASE(id,test) case id: name = #test; if (exec) { logln(#test "---"); logln((UnicodeString)""); test(); } break


void CalendarCaseTest::runIndexedTest( int32_t index, UBool exec, const char* &name, char* /*par*/ )
{
    if (exec) logln("TestSuite CalendarCaseTest");
    switch (index) {
    CASE(0,IslamicCivil);
    default: name = ""; break;
    }
}

#undef CASE

// ======= Utility functions =================

void CalendarCaseTest::doTestCases(const TestCase *cases, Calendar *cal) {
  static const int32_t  ONE_SECOND = 1000;
  static const int32_t  ONE_MINUTE = 60*ONE_SECOND;
  static const int32_t  ONE_HOUR   = 60*ONE_MINUTE;
  static const double ONE_DAY    = 24*ONE_HOUR;
  static const double JULIAN_EPOCH = -210866760000000.;   // 1/1/4713 BC 12:00
  int32_t i;
  logln("hi\n");
  UErrorCode status = U_ZERO_ERROR;
  cal->adoptTimeZone(TimeZone::getGMT()->clone());
  for(i=0;cases[i].era>=0;i++) {
    logln(UnicodeString("Test case ") + i);
    UDate t = (JULIAN_EPOCH+(ONE_DAY*cases[i].julian));
    
    // Millis -> fields
    cal->setTime(t, status);

    checkField(cal, UCAL_ERA, cases[i].era, status);
    checkField(cal, UCAL_YEAR, cases[i].year,status);
    checkField(cal, UCAL_MONTH, cases[i].month - 1,status);
    checkField(cal, UCAL_DATE, cases[i].day,status);
    checkField(cal, UCAL_DAY_OF_WEEK, cases[i].dayOfWeek,status);
    checkField(cal, UCAL_HOUR, cases[i].hour,status);
    checkField(cal, UCAL_MINUTE, cases[i].min,status);
    checkField(cal, UCAL_SECOND, cases[i].sec,status);
    
    // Fields -> millis
    cal->clear();
    
    cal->set(UCAL_ERA, cases[i].era);
    cal->set(UCAL_YEAR, cases[i].year);
    cal->set(UCAL_MONTH, cases[i].month - 1);
    cal->set(UCAL_DATE, cases[i].day);
    cal->set(UCAL_DAY_OF_WEEK, cases[i].dayOfWeek);
    cal->set(UCAL_HOUR, cases[i].hour);
    cal->set(UCAL_MINUTE, cases[i].min);
    cal->set(UCAL_SECOND, cases[i].sec);

    UDate t2 = cal->getTime(status);
    
    if(t != t2) {
      errln("Field->millis: Expected %.0f but got %.0f\n", t, t2);
      logln(calToStr(*cal));
    }
  }
}

UBool CalendarCaseTest::checkField(Calendar *cal, UCalendarDateFields field, int32_t value, UErrorCode &status)
{
  if(U_FAILURE(status)) return FALSE;
  int32_t res = cal->get(field, status);
  if(U_FAILURE(status)) {
    errln((UnicodeString)"Checking field " + fieldName(field) + " and got " + u_errorName(status));
    return FALSE;
  }
  if(res != value) {
    errln((UnicodeString)"Checking field " + fieldName(field) + " expected " + value + " and got " + res + UnicodeString("\n"));
    return FALSE;
  }
  return TRUE;
}

// =========== Test Cases =====================
enum { SUN=UCAL_SUNDAY,
       MON=UCAL_MONDAY,
       TUE=UCAL_TUESDAY,
       WED=UCAL_WEDNESDAY, 
       THU=UCAL_THURSDAY,
       FRI=UCAL_FRIDAY,
       SAT=UCAL_SATURDAY};

void CalendarCaseTest::IslamicCivil()
{
  static const TestCase tests[] = {
    //
    // Most of these test cases were taken from the back of
    // "Calendrical Calculations", with some extras added to help
    // debug a few of the problems that cropped up in development.
    //
    // The months in this table are 1-based rather than 0-based,
    // because it's easier to edit that way.
    //                       Islamic
    //          Julian Day  Era  Year  Month Day  WkDay Hour Min Sec
    {1507231.5,  0, -1245,   12,   9,  SUN,   0,  0,  0},
    { 1660037.5,  0,  -813,    2,  23,  WED,   0,  0,  0},
    { 1746893.5,  0,  -568,    4,   1,  WED,   0,  0,  0},
    { 1770641.5,  0,  -501,    4,   6,  SUN,   0,  0,  0},
    { 1892731.5,  0,  -157,   10,  17,  WED,   0,  0,  0},
    { 1931579.5,  0,   -47,    6,   3,  MON,   0,  0,  0},
    { 1974851.5,  0,    75,    7,  13,  SAT,   0,  0,  0},
    { 2091164.5,  0,   403,   10,   5,  SUN,   0,  0,  0},
    { 2121509.5,  0,   489,    5,  22,  SUN,   0,  0,  0},
    { 2155779.5,  0,   586,    2,   7,  FRI,   0,  0,  0},
    { 2174029.5,  0,   637,    8,   7,  SAT,   0,  0,  0},
    { 2191584.5,  0,   687,    2,  20,  FRI,   0,  0,  0},
    { 2195261.5,  0,   697,    7,   7,  SUN,   0,  0,  0},
    { 2229274.5,  0,   793,    7,   1,  SUN,   0,  0,  0},
    { 2245580.5,  0,   839,    7,   6,  WED,   0,  0,  0},
    { 2266100.5,  0,   897,    6,   1,  SAT,   0,  0,  0},
    { 2288542.5,  0,   960,    9,  30,  SAT,   0,  0,  0},
    { 2290901.5,  0,   967,    5,  27,  SAT,   0,  0,  0},
    { 2323140.5,  0,  1058,    5,  18,  WED,   0,  0,  0},
    { 2334848.5,  0,  1091,    6,   2,  SUN,   0,  0,  0},
    { 2348020.5,  0,  1128,    8,   4,  FRI,   0,  0,  0},
    { 2366978.5,  0,  1182,    2,   3,  SUN,   0,  0,  0},
    { 2385648.5,  0,  1234,   10,  10,  MON,   0,  0,  0},
    { 2392825.5,  0,  1255,    1,  11,  WED,   0,  0,  0},
    { 2416223.5,  0,  1321,    1,  21,  SUN,   0,  0,  0},
    { 2425848.5,  0,  1348,    3,  19,  SUN,   0,  0,  0},
    { 2430266.5,  0,  1360,    9,   8,  MON,   0,  0,  0},
    { 2430833.5,  0,  1362,    4,  13,  MON,   0,  0,  0},
    { 2431004.5,  0,  1362,   10,   7,  THU,   0,  0,  0},
    { 2448698.5,  0,  1412,    9,  13,  TUE,   0,  0,  0},
    { 2450138.5,  0,  1416,   10,   5,  SUN,   0,  0,  0},
    { 2465737.5,  0,  1460,   10,  12,  WED,   0,  0,  0},
    { 2486076.5,  0,  1518,    3,   5,  SUN,   0,  0,  0},
    { -1,-1,-1,-1,-1,-1,-1,-1,-1 }
  };
  
  UErrorCode status = U_ZERO_ERROR;
  Calendar *c = Calendar::createInstance("@calendar=islamic-civil", status);
  c->setLenient(TRUE);
  doTestCases(tests, c);
  delete c;
}



#endif
