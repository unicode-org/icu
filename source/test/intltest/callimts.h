/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
 
#ifndef __CalendarLimitTest__
#define __CalendarLimitTest__
 
#include "unicode/utypes.h"
#include "caltztst.h"
class Calendar;
class DateFormat;

/**
 * This test verifies the behavior of Calendar around the very earliest limits
 * which it can handle.  It also verifies the behavior for large values of millis.
 *
 * Bug ID 4033662.
 */
class CalendarLimitTest: public CalendarTimeZoneTest {
    // IntlTest override
    void runIndexedTest( int32_t index, bool_t exec, char* &name, char* par );
public: // package
    static const UDate EARLIEST_SUPPORTED_MILLIS;
    static const UDate LATEST_SUPPORTED_MILLIS;

    //test routine used by TestCalendarLimit
    virtual void test(UDate millis, Calendar *cal, DateFormat *fmt);

    static double nextDouble(double a);
    static double previousDouble(double a);
    static bool_t withinErr(double a, double b, double err);

public:
    // test behaviour and error reporting at boundaries of defined range
    virtual void TestCalendarLimit(void);
 
public: // package
    /**
     * Locate the earliest limits which are correctly handled.
     * Used by TestCalendarLimit
     */
    virtual void explore2(UDate expectedEarlyLimit);
    virtual void explore3(UDate expectedLateLimit);
    static UDate gregorianCutover;

    static const int32_t     kEpochStartAsJulianDay; // January 1, 1970 (Gregorian)
    static const UDate        kPapalCutover;
    static const int32_t    kJan1_1JulianDay;
    static const int32_t    kNumDays[];
    static const int32_t    kLeapNumDays[];
    static const int32_t    kMonthLength[];
    static const int32_t    kLeapMonthLength[];

    static double millisToJulianDay(UDate millis);

    static double floorDivide(double numerator, double denominator);
    static int32_t floorDivide(int32_t numerator, int32_t denominator);
    static int32_t floorDivide(int32_t numerator, int32_t denominator, int32_t remainder[]);
    static int32_t floorDivide(double numerator, int32_t denominator, int32_t remainder[]);

       static int32_t julianDayOffset;
    static int32_t millisPerDay;
    static int32_t YEAR;
    static int32_t MONTH;
    static int32_t DATE;

    static bool_t timeToFields(UDate millis, int32_t *fields);
};
 
#endif // __CalendarLimitTest__
