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
#ifndef GREGOIMP_H
#define GREGOIMP_H
#if !UCONFIG_NO_FORMATTING

#include "unicode/utypes.h"

U_NAMESPACE_BEGIN

/**
 * A utility class providing mathematical functions used by time zone
 * and calendar code.  Do not instantiate.
 */
class U_I18N_API Math {
 public:
    /**
     * Divide two integers, returning the floor of the quotient.
     * Unlike the built-in division, this is mathematically
     * well-behaved.  E.g., <code>-1/4</code> => 0 but
     * <code>floorDivide(-1,4)</code> => -1.
     * @param numerator the numerator
     * @param denominator a divisor which must be != 0
     * @return the floor of the quotient
     */
    static int32_t floorDivide(int32_t numerator, int32_t denominator);

    /**
     * Divide two numbers, returning the floor of the quotient.
     * Unlike the built-in division, this is mathematically
     * well-behaved.  E.g., <code>-1/4</code> => 0 but
     * <code>floorDivide(-1,4)</code> => -1.
     * @param numerator the numerator
     * @param denominator a divisor which must be != 0
     * @return the floor of the quotient
     */
    static inline double floorDivide(double numerator, double denominator);

    /**
     * Divide two numbers, returning the floor of the quotient and
     * the modulus remainder.  Unlike the built-in division, this is
     * mathematically well-behaved.  E.g., <code>-1/4</code> => 0 and
     * <code>-1%4</code> => -1, but <code>floorDivide(-1,4)</code> =>
     * -1 with <code>remainder</code> => 3.  NOTE: If numerator is
     * too large, the returned quotient may overflow.
     * @param numerator the numerator
     * @param denominator a divisor which must be != 0
     * @param remainder output parameter to receive the
     * remainder. Unlike <code>numerator % denominator</code>, this
     * will always be non-negative, in the half-open range <code>[0,
     * |denominator|)</code>.
     * @return the floor of the quotient
     */
    static int32_t floorDivide(double numerator, int32_t denominator,
                               int32_t& remainder);
};

/**
 * A utility class providing proleptic Gregorian calendar functions
 * used by time zone and calendar code.  Do not instantiate.
 *
 * Note:  Unlike GregorianCalendar, all computations performed by this
 * class occur in the pure proleptic GregorianCalendar.
 */
class U_I18N_API Grego {
 public:
    /**
     * Return TRUE if the given year is a leap year.
     * @param year Gregorian year, with 0 == 1 BCE, -1 == 2 BCE, etc.
     * @return TRUE if the year is a leap year
     */
    static inline UBool isLeapYear(int32_t year);

    /**
     * Return the number of days in the given month.
     * @param year Gregorian year, with 0 == 1 BCE, -1 == 2 BCE, etc.
     * @param month 0-based month, with 0==Jan
     * @return the number of days in the given month
     */
    static inline int8_t monthLength(int32_t year, int32_t month);

    /**
     * Convert a year, month, and day-of-month, given in the proleptic
     * Gregorian calendar, to 1970 epoch days.
     * @param year Gregorian year, with 0 == 1 BCE, -1 == 2 BCE, etc.
     * @param month 0-based month, with 0==Jan
     * @param dom 1-based day of month
     * @return the day number, with day 0 == Jan 1 1970
     */
    static double fieldsToDay(int32_t year, int32_t month, int32_t dom);
    
    /**
     * Convert a 1970-epoch day number to proleptic Gregorian year,
     * month, day-of-month, and day-of-week.
     * @param day 1970-epoch day (integral value)
     * @param year output parameter to receive year
     * @param month output parameter to receive month (0-based, 0==Jan)
     * @param dom output parameter to receive day-of-month (1-based)
     * @param dow output parameter to receive day-of-week (1-based, 1==Sun)
     */
    static void dayToFields(double day, int32_t& year, int32_t& month,
                            int32_t& dom, int32_t& dow);

 private:
    static const int16_t DAYS_BEFORE[24];
    static const int8_t MONTH_LENGTH[24];
};

inline double Math::floorDivide(double numerator, double denominator) {
    return uprv_floor(numerator / denominator);
}

inline UBool Grego::isLeapYear(int32_t year) {
    // year&0x3 == year%4
    return ((year&0x3) == 0) && ((year%100 != 0) || (year%400 == 0));
}

inline int8_t
Grego::monthLength(int32_t year, int32_t month) {
    return MONTH_LENGTH[month + isLeapYear(year)?12:0];
}

U_NAMESPACE_END

#endif // !UCONFIG_NO_FORMATTING
#endif // GREGOIMP_H

//eof
