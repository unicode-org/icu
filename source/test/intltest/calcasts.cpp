/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 2003-2004, International Business Machines Corporation 
 * and others. All Rights Reserved.
 ********************************************************************
 * Calendar Case Test is a type of CalendarTest which compares the 
 * behavior of a calendar to a certain set of 'test cases', involving
 * conversion between julian-day to fields and vice versa.
 ********************************************************************/

#include "calcasts.h"

#if !UCONFIG_NO_FORMATTING
// ======= 'Main' ===========================

#include "hebrwcal.h" // for Eras

#define CASE(id,test) case id: name = #test; if (exec) { logln(#test "---"); logln((UnicodeString)""); test(); } break


void CalendarCaseTest::runIndexedTest( int32_t index, UBool exec, const char* &name, char* /*par*/ )
{
    if (exec) logln("TestSuite CalendarCaseTest");
    switch (index) {
    CASE(0,IslamicCivil);
    CASE(1,Hebrew);
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
  UErrorCode status = U_ZERO_ERROR;
  cal->adoptTimeZone(TimeZone::getGMT()->clone());
  for(i=0;cases[i].era>=0;i++) {
    UDate t = (JULIAN_EPOCH+(ONE_DAY*cases[i].julian));

    logln("Test case %d:  julianday%f -> date %f\n", i, cases[i].julian, t);

    // Millis -> fields
    cal->setTime(t, status);

    logln(calToStr(*cal));

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
    errln((UnicodeString)"FAIL: Checking field " + fieldName(field) + " expected " + value + " and got " + res + UnicodeString("\n"));
    return FALSE;
  } else {
    logln((UnicodeString)"Checking field " + fieldName(field) + " == " + value + UnicodeString("\n"));
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
  Calendar *c = Calendar::createInstance("ar@calendar=islamic-civil", status);
  c->setLenient(TRUE);
  doTestCases(tests, c);
  delete c;
}

void CalendarCaseTest::Hebrew() {
  static const int32_t TISHRI  = HebrewCalendar::TISHRI;
//     static const int32_t HESHVAN = HebrewCalendar::HESHVAN;
//     static const int32_t KISLEV  = HebrewCalendar::KISLEV;
//     static const int32_t TEVET   = HebrewCalendar::TEVET;
//     static const int32_t SHEVAT  = HebrewCalendar::SHEVAT;
//     static const int32_t ADAR_1  = HebrewCalendar::ADAR_1;
//     static const int32_t ADAR    = HebrewCalendar::ADAR;
//     static const int32_t NISAN   = HebrewCalendar::NISAN;
//     static const int32_t IYAR    = HebrewCalendar::IYAR;
//     static const int32_t SIVAN   = HebrewCalendar::SIVAN;
//     static const int32_t TAMUZ   = HebrewCalendar::TAMUZ;
    static const int32_t AV      = HebrewCalendar::AV;
    static const int32_t ELUL    = HebrewCalendar::ELUL;

  static const TestCase tests[] = {
    //
    // Most of these test cases were taken from the back of
    // "Calendrical Calculations", with some extras added to help
    // debug a few of the problems that cropped up in development.
    //
    // The months in this table are 1-based rather than 0-based,
    // because it's easier to edit that way.
    //
    //         Julian Day  Era  Year  Month Day  WkDay Hour Min Sec
    {1507231.5,  0,  3174,   12,  10,  SUN,   0,  0,  0},
     {1660037.5,  0,  3593,    3,  25,  WED,   0,  0,  0},
     {1746893.5,  0,  3831,    1,   3,  WED,   0,  0,  0},
     {1770641.5,  0,  3896,    1,   9,  SUN,   0,  0,  0},
     {1892731.5,  0,  4230,    4,  18,  WED,   0,  0,  0},
     {1931579.5,  0,  4336,   10,   4,  MON,   0,  0,  0},
     {1974851.5,  0,  4455,    2,  13,  SAT,   0,  0,  0},
     {2091164.5,  0,  4773,    9,   6,  SUN,   0,  0,  0},
     {2121509.5,  0,  4856,    9,  23,  SUN,   0,  0,  0},
     {2155779.5,  0,  4950,    8,   7,  FRI,   0,  0,  0},
     {2174029.5,  0,  5000,    7,   8,  SAT,   0,  0,  0},
     {2191584.5,  0,  5048,    8,  21,  FRI,   0,  0,  0},
     {2195261.5,  0,  5058,    9,   7,  SUN,   0,  0,  0},
     {2229274.5,  0,  5151,   11,   1,  SUN,   0,  0,  0},
     {2245580.5,  0,  5196,    5,   7,  WED,   0,  0,  0},
     {2266100.5,  0,  5252,    8,   3,  SAT,   0,  0,  0},
     {2288542.5,  0,  5314,    1,   1,  SAT,   0,  0,  0},
     {2290901.5,  0,  5320,    6,  27,  SAT,   0,  0,  0},
     {2323140.5,  0,  5408,   10,  20,  WED,   0,  0,  0},
     {2334551.5,  0,  5440,    1,   1,  THU,   0,  0,  0},
     {2334581.5,  0,  5440,    2,   1,  SAT,   0,  0,  0},
     {2334610.5,  0,  5440,    3,   1,  SUN,   0,  0,  0},
     {2334639.5,  0,  5440,    4,   1,  MON,   0,  0,  0},
     {2334668.5,  0,  5440,    5,   1,  TUE,   0,  0,  0},
     {2334698.5,  0,  5440,    6,   1,  THU,   0,  0,  0},
     {2334728.5,  0,  5440,    7,   1,  SAT,   0,  0,  0},
     {2334757.5,  0,  5440,    8,   1,  SUN,   0,  0,  0},
     {2334787.5,  0,  5440,    9,   1,  TUE,   0,  0,  0},
     {2334816.5,  0,  5440,   10,   1,  WED,   0,  0,  0},
     {2334846.5,  0,  5440,   11,   1,  FRI,   0,  0,  0},
     {2334848.5,  0,  5440,   11,   3,  SUN,   0,  0,  0},
     {2334934.5,  0,  5441,    1,   1,  TUE,   0,  0,  0},
     {2348020.5,  0,  5476,   12,   5,  FRI,   0,  0,  0},
     {2366978.5,  0,  5528,   11,   4,  SUN,   0,  0,  0},
     {2385648.5,  0,  5579,   12,  11,  MON,   0,  0,  0},
     {2392825.5,  0,  5599,    8,  12,  WED,   0,  0,  0},
     {2416223.5,  0,  5663,    8,  22,  SUN,   0,  0,  0},
     {2425848.5,  0,  5689,   12,  19,  SUN,   0,  0,  0},
     {2430266.5,  0,  5702,    1,   8,  MON,   0,  0,  0},
     {2430833.5,  0,  5703,    8,  14,  MON,   0,  0,  0},
     {2431004.5,  0,  5704,    1,   8,  THU,   0,  0,  0},
     {2448698.5,  0,  5752,    7,  12,  TUE,   0,  0,  0},
     {2450138.5,  0,  5756,    7,   5,  SUN,   0,  0,  0},
     {2465737.5,  0,  5799,    2,  12,  WED,   0,  0,  0},
    {2486076.5,  0,  5854,   12,   5,  SUN,   0,  0,  0},

    // Test cases taken from a table of 14 "year types" in the Help file
    // of the application "Hebrew Calendar"
    {2456187.5,  0,  5773,    1,   1,  MON,   0,  0,  0},
     {2459111.5,  0,  5781,    1,   1,  SAT,   0,  0,  0},
     {2453647.5,  0,  5766,    1,   1,  TUE,   0,  0,  0},
     {2462035.5,  0,  5789,    1,   1,  THU,   0,  0,  0},
     {2458756.5,  0,  5780,    1,   1,  MON,   0,  0,  0},
     {2460586.5,  0,  5785,    1,   1,  THU,   0,  0,  0},
     {2463864.5,  0,  5794,    1,   1,  SAT,   0,  0,  0},
     {2463481.5,  0,  5793,    1,   1,  MON,   0,  0,  0},
     {2470421.5,  0,  5812,    1,   1,  THU,   0,  0,  0},
     {2460203.5,  0,  5784,    1,   1,  SAT,   0,  0,  0},
     {2459464.5,  0,  5782,    1,   1,  TUE,   0,  0,  0},
     {2467142.5,  0,  5803,    1,   1,  MON,   0,  0,  0},
    {2455448.5,  0,  5771,    1,   1,  THU,   0,  0,  0},
    // Test cases for JB#2327        
    // http://www.fourmilab.com/documents/calendar/
    // http://www.calendarhome.com/converter/
    //      2452465.5, 2002, JULY, 10, 5762, AV, 1,
    //      2452494.5, 2002, AUGUST, 8, 5762, AV, 30,
    //      2452495.5, 2002, AUGUST, 9, 5762, ELUL, 1,
    //      2452523.5, 2002, SEPTEMBER, 6, 5762, ELUL, 29,
    //      2452524.5, 2002, SEPTEMBER, 7, 5763, TISHRI, 1,
    //         Julian Day  Era  Year  Month Day  WkDay Hour Min Sec
    {2452465.5,  0,  5762,    AV+1,  1,  WED,   0,  0,  0},
    {2452494.5,  0,  5762,    AV+1, 30,  THU,   0,  0,  0},
    {2452495.5,  0,  5762,  ELUL+1,  1,  FRI,   0,  0,  0},
    {2452523.5,  0,  5762,  ELUL+1, 29,  FRI,   0,  0,  0},
    {2452524.5,  0,  5763, TISHRI+1,  1,  SAT,   0,  0,  0},
    { -1,-1,-1,-1,-1,-1,-1,-1,-1 }
  };
  
  UErrorCode status = U_ZERO_ERROR;
  Calendar *c = Calendar::createInstance("he_HE@calendar=hebrew", status);
  c->setLenient(TRUE);
  doTestCases(tests, c);


    // Additional test cases for bugs found during development
    //           G.YY/MM/DD  Era  Year  Month Day  WkDay Hour Min Sec
  //{1013, 9, 8, 0,  4774,    1,   1,  TUE,   0,  0,  0},
  //{1239, 9, 1, 0,  5000,    1,   1,  THU,   0,  0,  0},
  //{1240, 9,18, 0,  5001,    1,   1,  TUE,   0,  0,  0},


  delete c;
}


#endif
