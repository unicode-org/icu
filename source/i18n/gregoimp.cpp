/*
**********************************************************************
* Copyright (c) 2003, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Alan Liu
* Created: September 2 2003
* Since: ICU 2.8
**********************************************************************
*/
#include "gregoimp.h"
#include "unicode/ucal.h"

int32_t Math::floorDivide(int32_t numerator, int32_t denominator) {
    return (numerator >= 0) ?
        numerator / denominator : ((numerator + 1) / denominator) - 1;
}

int32_t Math::floorDivide(double numerator, int32_t denominator,
                          int32_t& remainder) {
    double quotient;
    quotient = uprv_floor(numerator / denominator);
    remainder = (int32_t) (numerator - (quotient * denominator));
    return (int32_t) quotient;
}

const int32_t JULIAN_1_CE    = 1721426; // January 1, 1 CE Gregorian
const int32_t JULIAN_1970_CE = 2440588; // January 1, 1970 CE Gregorian

const int16_t Grego::DAYS_BEFORE[24] =
    {0,31,59,90,120,151,181,212,243,273,304,334,
     0,31,60,91,121,152,182,213,244,274,305,335};

const int8_t Grego::MONTH_LENGTH[24] =
    {31,28,31,30,31,30,31,31,30,31,30,31,
     31,29,31,30,31,30,31,31,30,31,30,31};

double Grego::fieldsToDay(int32_t year, int32_t month, int32_t dom) {

    int32_t y = year - 1;

    double julian = 365 * y + Math::floorDivide(y, 4) + (JULIAN_1_CE - 3) + // Julian cal
        Math::floorDivide(y, 400) - Math::floorDivide(y, 100) + 2 + // => Gregorian cal
        DAYS_BEFORE[month + (isLeapYear(year) ? 12 : 0)] + dom; // => month/dom

    return julian - JULIAN_1970_CE; // JD => epoch day
}

void Grego::dayToFields(double day, int32_t& year, int32_t& month,
                        int32_t& dom, int32_t& dow, int32_t& doy) {

    // Convert from 1970 CE epoch to 1 CE epoch (Gregorian calendar)
    day += JULIAN_1970_CE - JULIAN_1_CE;

    // Convert from the day number to the multiple radix
    // representation.  We use 400-year, 100-year, and 4-year cycles.
    // For example, the 4-year cycle has 4 years + 1 leap day; giving
    // 1461 == 365*4 + 1 days.
    int32_t n400 = Math::floorDivide(day, 146097, doy); // 400-year cycle length
    int32_t n100 = Math::floorDivide(doy, 36524, doy); // 100-year cycle length
    int32_t n4   = Math::floorDivide(doy, 1461, doy); // 4-year cycle length
    int32_t n1   = Math::floorDivide(doy, 365, doy);
    year = 400*n400 + 100*n100 + 4*n4 + n1;
    if (n100 == 4 || n1 == 4) {
        doy = 365; // Dec 31 at end of 4- or 400-year cycle
    } else {
        ++year;
    }
    
    UBool isLeap = isLeapYear(year);
    
    // Gregorian day zero is a Monday.
    dow = (int32_t) uprv_fmod(day + 1, 7);
    dow += (dow < 0) ? (UCAL_SUNDAY + 7) : UCAL_SUNDAY;

    // Common Julian/Gregorian calculation
    int32_t correction = 0;
    int32_t march1 = isLeap ? 60 : 59; // zero-based DOY for March 1
    if (doy >= march1) {
        correction = isLeap ? 1 : 2;
    }
    month = (12 * (doy + correction) + 6) / 367; // zero-based month
    dom = doy - DAYS_BEFORE[month + (isLeap ? 12 : 0)] + 1; // one-based DOM
    doy++; // one-based doy
}

//eof
