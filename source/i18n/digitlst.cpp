/*
**********************************************************************
*   Copyright (C) 1997-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*
* File DIGITLST.CPP
*
* Modification History:
*
*   Date        Name        Description
*   03/21/97    clhuang     Converted from java.
*   03/21/97    clhuang     Implemented with new APIs.
*   03/27/97    helena      Updated to pass the simple test after code review.
*   03/31/97    aliu        Moved isLONG_MIN to here, and fixed it.
*   04/15/97    aliu        Changed MAX_COUNT to DBL_DIG.  Changed Digit to char.
*                           Reworked representation by replacing fDecimalAt with
*                           fExponent.
*   04/16/97    aliu        Rewrote set() and getDouble() to use sprintf/atof
*                           to do digit conversion.
*   09/09/97    aliu        Modified for exponential notation support.
*    08/02/98    stephen        Added nearest/even rounding
*                            Fixed bug in fitsIntoLong
********************************************************************************
*/

#include "digitlst.h"
#include <stdlib.h>
#include <limits.h>
#include <string.h>
#include <stdio.h>

// *****************************************************************************
// class DigitList
// This class handles the transcoding between numeric values and strings of
//  characters.  Only handles as non-negative numbers.  
// *****************************************************************************

const char DigitList::kZero = '0';

char DigitList::LONG_MIN_REP[LONG_DIGITS];
int32_t  DigitList::LONG_MIN_REP_LENGTH = 0;

// -------------------------------------
// default constructor

DigitList::DigitList()
{
    clear();
}

// -------------------------------------

DigitList::~DigitList()
{
}

// -------------------------------------
// copy constructor

DigitList::DigitList(const DigitList &other)
{
    *this = other;
}

// -------------------------------------
// assignment operator

DigitList&
DigitList::operator=(const DigitList& other)
{
    if (this != &other)
    {
        fDecimalAt = other.fDecimalAt;
        fCount = other.fCount;
        strncpy(fDigits, other.fDigits, MAX_DIGITS);
    }
    return *this;
}

// -------------------------------------

bool_t
DigitList::operator==(const DigitList& that) const
{
    return ((this == &that) ||
            (fDecimalAt == that.fDecimalAt &&
             fCount == that.fCount &&
             0 == strncmp(fDigits, that.fDigits, fCount)));
}

// -------------------------------------
// Resets the digit list; sets all the digits to zero.

void
DigitList::clear()
{
    fDecimalAt = 0;
    fCount = 0;
    for (int32_t i=0; i<MAX_DIGITS; ++i) fDigits[i] = kZero;
}

// -------------------------------------
// Appends the digit to the digit list if it's not out of scope.
// Ignores the digit, otherwise.

void
DigitList::append(char digit)
{
    // Ignore digits which exceed the precision we can represent
    if (fCount < MAX_DIGITS) fDigits[fCount++] = digit;
}

// -------------------------------------

/**
 * Currently, getDouble() depends on atof() to do its conversion.
 */
double
DigitList::getDouble() const
{
    if (fCount == 0) return 0.0;

    // For the string "." + fDigits + "e" + fDecimalAt.
    char buffer[MAX_DIGITS+32];
    *buffer = '.';
    strncpy(buffer+1, fDigits, fCount);
    sprintf(buffer+fCount+1, "e%d", fDecimalAt);
    return atof(buffer);
}

// -------------------------------------

int32_t DigitList::getLong() const
{
    // This is 100% accurate in c++ because if we are representing
    // an integral value, we suffer nothing in the conversion to
    // double.  If we are to support 64-bit longs later, this method
    // must be rewritten. [LIU]
    return (int32_t)getDouble();
}

/**
 * Return true if the number represented by this object can fit into
 * a long.
 */
bool_t
DigitList::fitsIntoLong(bool_t isPositive, bool_t ignoreNegativeZero)
{
    // Figure out if the result will fit in a long.  We have to
    // first look for nonzero digits after the decimal point;
    // then check the size.  If the digit count is 18 or less, then
    // the value can definitely be represented as a long.  If it is 19
    // then it may be too large.

    // Trim trailing zeros.  This does not change the represented value.
    while (fCount > 0 && fDigits[fCount - 1] == '0') --fCount;

    if (fCount == 0) {
        // Positive zero fits into a long, but negative zero can only
        // be represented as a double. - bug 4162852
        return isPositive || ignoreNegativeZero;
    }
    
    initializeLONG_MIN_REP();

    // If the digit list represents a double or this number is too
    // big for a long.
    if (fDecimalAt < fCount || fDecimalAt > LONG_MIN_REP_LENGTH) return FALSE;

    // If number is small enough to fit in a long
    if (fDecimalAt < LONG_MIN_REP_LENGTH) return TRUE;

    // At this point we have fDecimalAt == fCount, and fCount == LONG_MIN_REP_LENGTH.
    // The number will overflow if it is larger than LONG_MAX
    // or smaller than LONG_MIN.
    for (int32_t i=0; i<fCount; ++i)
    {
        char dig = fDigits[i], max = LONG_MIN_REP[i];
        if (dig > max) return FALSE;
        if (dig < max) return TRUE;
    }
    
    // At this point the first count digits match.  If fDecimalAt is less
    // than count, then the remaining digits are zero, and we return true.
    if (fCount < fDecimalAt) return TRUE;

    // Now we have a representation of Long.MIN_VALUE, without the leading
    // negative sign.  If this represents a positive value, then it does
    // not fit; otherwise it fits.
    return !isPositive;
}

// -------------------------------------

/**
 * @param maximumDigits The maximum digits to be generated.  If zero,
 * there is no maximum -- generate all digits.
 */
void
DigitList::set(int32_t source, int32_t maximumDigits)
{
    // for now, simple implementation; later, do proper IEEE stuff
    //String stringDigits = Long.toString(source);
    char string [10 + 1];    // maximum digits for a 32-bit signed number is 10 + 1 for sign
    sprintf(string, "%d", source);

    char *stringDigits = string;
    // This method does not expect a negative number. However,
    // "source" can be a Long.MIN_VALUE (-9223372036854775808),
    // if the number being formatted is a Long.MIN_VALUE.  In that
    // case, it will be formatted as -Long.MIN_VALUE, a number
    // which is outside the legal range of a long, but which can
    // be represented by DigitList.
    if (stringDigits[0] == '-') 
        stringDigits++;

    fCount = fDecimalAt = strlen(stringDigits);
    
    // Don't copy trailing zeros
    while (fCount > 1 && stringDigits[fCount - 1] == '0') 
        --fCount;
    
    //for (int32_t i = 0; i < fCount; ++i)
    //    fDigits[i] = (char) stringDigits[i];
    strncpy(fDigits, stringDigits, fCount);

    if(maximumDigits > 0) 
        round(maximumDigits);

#if(0)
    // {sfb} old implementation, keep around for now

    // Handle the case in which source == LONG_MIN
    set((source >= 0 ? (double)source : -((double)source)),
        maximumDigits > 0 ? maximumDigits : MAX_DIGITS,
        maximumDigits == 0);
#endif
}

/**
 * Set the digit list to a representation of the given double value.
 * This method supports both fixed-point and exponential notation.
 * @param source Value to be converted; must not be Inf, -Inf, Nan,
 * or a value <= 0.
 * @param maximumDigits The most fractional or total digits which should
 * be converted.  If total digits, and the value is zero, then
 * there is no maximum -- generate all digits.
 * @param fixedPoint If true, then maximumDigits is the maximum
 * fractional digits to be converted.  If false, total digits.
 */
void
DigitList::set(double source, int32_t maximumDigits, bool_t fixedPoint)
{
    if(source == 0) source = 0;
    // Generate a representation of the form DDDDD, DDDDD.DDDDD, or
    // DDDDDE+/-DDDDD.
    //String rep = Double.toString(source);
    char rep[MAX_DIGITS + 7]; // Extra space for '.', e+NNN, and '\0' (actually +7 is enough)
    sprintf(rep, "%1.*e", MAX_DIGITS - 1, source);

    fDecimalAt             = -1;
    fCount                 = 0;
    int32_t exponent     = 0;
    // Number of zeros between decimal point and first non-zero digit after
    // decimal point, for numbers < 1.
    int32_t leadingZerosAfterDecimal = 0;
    bool_t nonZeroDigitSeen = FALSE;
    for (int32_t i=0; i < MAX_DIGITS + 7; ++i) {
        char c = rep[i];
        if (c == '.') {
            fDecimalAt = fCount;
        }
        else if (c == 'e' || c == 'E') {
            // Parse an exponent of the form /[eE][+-]?[0-9]*/
            //exponent = Integer.valueOf(rep.substring(i+1)).intValue();
            i += 1;                 // adjust for 'e'
            bool_t negExp = rep[i] == '-';
            if (negExp || rep[i] == '+') {
                ++i;
            }
            while ((c = rep[i++]) >= '0' && c <= '9') {
                exponent = 10*exponent + c - '0';
            }
            if (negExp) {
                exponent = -exponent;
            }
            break;
        }
        else if (fCount < MAX_DIGITS) {
            if ( ! nonZeroDigitSeen) {
                nonZeroDigitSeen = (c != '0');
                if ( ! nonZeroDigitSeen && fDecimalAt != -1) 
                    ++leadingZerosAfterDecimal;
            }
    
            if (nonZeroDigitSeen) 
                fDigits[fCount++] = (char)c;
        }
    }
    if (fDecimalAt == -1) 
        fDecimalAt = fCount;
    fDecimalAt += exponent - leadingZerosAfterDecimal;

    if (fixedPoint)
    {
        // The negative of the exponent represents the number of leading
        // zeros between the decimal and the first non-zero digit, for
        // a value < 0.1 (e.g., for 0.00123, -decimalAt == 2).  If this
        // is more than the maximum fraction digits, then we have an underflow
        // for the printed representation.
        if (-fDecimalAt > maximumDigits) {
            // Handle an underflow to zero when we round something like
            // 0.0009 to 2 fractional digits.
            fCount = 0;
            return;
        } else if (-fDecimalAt == maximumDigits) {
            // If we round 0.0009 to 3 fractional digits, then we have to
            // create a new one digit in the least significant location.
            if (shouldRoundUp(0)) {
                fCount = 1;
                ++fDecimalAt;
                fDigits[0] = (char)'1';
            } else {
                fCount = 0;
            }
            return;
        }
    }

    // Eliminate trailing zeros.
    while (fCount > 1 && fDigits[fCount - 1] == '0')
        --fCount;

    /*if (DEBUG)
    {
        System.out.print("Before rounding 0.");
        for (int i=0; i<fCount; ++i) System.out.print((char)digits[i]);
        System.out.println("x10^" + fDecimalAt);
    }*/

    // Eliminate digits beyond maximum digits to be displayed.
    // Round up if appropriate.  Do NOT round in the special
    // case where maximumDigits == 0 and fixedPoint is FALSE.
    if (fixedPoint || maximumDigits > 0) {
        round(fixedPoint ? (maximumDigits + fDecimalAt) : maximumDigits);
    }

    /*if (DEBUG)
    {
        System.out.print("After rounding 0.");
        for (int i=0; i<fCount; ++i) System.out.print((char)digits[i]);
        System.out.println("x10^" + fDecimalAt);
    }*/
}

// -------------------------------------

/**
 * Round the representation to the given number of digits.
 * @param maximumDigits The maximum number of digits to be shown.
 * Upon return, count will be less than or equal to maximumDigits.
 */
void 
DigitList::round(int32_t maximumDigits)
{
    // Eliminate digits beyond maximum digits to be displayed.
    // Round up if appropriate.
    if (maximumDigits >= 0 && maximumDigits < fCount)
    {
        if (shouldRoundUp(maximumDigits)) {
            // Rounding up involved incrementing digits from LSD to MSD.
            // In most cases this is simple, but in a worst case situation
            // (9999..99) we have to adjust the decimalAt value.
            for (;;)
            {
                --maximumDigits;
                if (maximumDigits < 0)
                {
                    // We have all 9's, so we increment to a single digit
                    // of one and adjust the exponent.
                    fDigits[0] = (char) '1';
                    ++fDecimalAt;
                    maximumDigits = 0; // Adjust the count
                    break;
                }

                ++fDigits[maximumDigits];
                if (fDigits[maximumDigits] <= '9') break;
                // fDigits[maximumDigits] = '0'; // Unnecessary since we'll truncate this
            }
            ++maximumDigits; // Increment for use as count
        }
        fCount = maximumDigits;

        // Eliminate trailing zeros.
        while (fCount > 1 && fDigits[fCount-1] == '0') {
            --fCount;
        }
    }
}

/**
 * Return true if truncating the representation to the given number
 * of digits will result in an increment to the last digit.  This
 * method implements half-even rounding, the default rounding mode.
 * [bnf]
 * @param maximumDigits the number of digits to keep, from 0 to
 * <code>count-1</code>.  If 0, then all digits are rounded away, and
 * this method returns true if a one should be generated (e.g., formatting
 * 0.09 with "#.#").
 * @return true if digit <code>maximumDigits-1</code> should be
 * incremented
 */
bool_t DigitList::shouldRoundUp(int32_t maximumDigits) {
    bool_t increment = FALSE;
    // Implement IEEE half-even rounding
    if (fDigits[maximumDigits] > '5') {
        return TRUE;
    } else if (fDigits[maximumDigits] == '5' ) {
        for (int i=maximumDigits+1; i<fCount; ++i) {
            if (fDigits[i] != '0') {
                return TRUE;
            }
        }
        return maximumDigits > 0 && (fDigits[maximumDigits-1] % 2 != 0);
    }
    return FALSE;
}

// -------------------------------------

// In the Java implementation, we need a separate set(long) because 64-bit longs
// have too much precision to fit into a 64-bit double.  In C++, longs can just
// be passed to set(double) as long as they are 32 bits in size.  We currently
// don't implement 64-bit longs in C++, although the code below would work for
// that with slight modifications. [LIU]

//  void
//  DigitList::set(long source)
//  {
//      // handle the special case of zero using a standard exponent of 0.
//      // mathematically, the exponent can be any value.
//      if (source == 0)
//      {
//          fcount = 0;
//          fDecimalAt = 0;
//          return;
//      }

//      // we don't accept negative numbers, with the exception of long_min.
//      // long_min is treated specially by being represented as long_max+1,
//      // which is actually an impossible signed long value, so there is no
//      // ambiguity.  we do this for convenience, so digitlist can easily
//      // represent the digits of a long.
//      bool islongmin = (source == long_min);
//      if (islongmin)
//      {
//          source = -(source + 1); // that is, long_max
//          islongmin = true;
//      }
//      sprintf(fdigits, "%d", source);

//      // now we need to compute the exponent.  it's easy in this case; it's
//      // just the same as the count.  e.g., 0.123 * 10^3 = 123.
//      fcount = strlen(fdigits);
//      fDecimalAt = fcount;

//      // here's how we represent long_max + 1.  note that we always know
//      // that the last digit of long_max will not be 9, because long_max
//      // is of the form (2^n)-1.
//      if (islongmin) ++fdigits[fcount-1];

//      // finally, we trim off trailing zeros.  we don't alter fDecimalAt,
//      // so this has no effect on the represented value.  we know the first
//      // digit is non-zero (see code above), so we only have to check down
//      // to fdigits[1].
//      while (fcount > 1 && fdigits[fcount-1] == kzero) --fcount;
//  }

/**
 * Return true if this object represents the value zero.  Anything with
 * no digits, or all zero digits, is zero, regardless of fDecimalAt.
 */
bool_t
DigitList::isZero() const
{
    for (int32_t i=0; i<fCount; ++i) if (fDigits[i] != kZero) return FALSE;
    return TRUE;
}

/**
 * We represent LONG_MIN internally as LONG_MAX + 1.  This is actually an impossible
 * value, for positive long integers, so we are safe in doing so.
 */
bool_t
DigitList::isLONG_MIN() const
{
    initializeLONG_MIN_REP();

    if (fCount != LONG_MIN_REP_LENGTH) return FALSE;

    for (int32_t i = 0; i < LONG_MIN_REP_LENGTH; ++i)
    {
        if (fDigits[i] != LONG_MIN_REP[i+1]) return FALSE;
    }

    return TRUE;
}

// Initialize the LONG_MIN representation buffer.  Note that LONG_MIN
// is stored as LONG_MAX+1 (LONG_MIN without the negative sign).

void
DigitList::initializeLONG_MIN_REP()
{
    if (LONG_MIN_REP_LENGTH == 0)
    {
        // THIS ASSUMES A 32-BIT LONG_MIN VALUE
        char buf[LONG_DIGITS];
        sprintf(buf, "%d", LONG_MIN);
        LONG_MIN_REP_LENGTH = strlen(buf) - 1;
        // assert(LONG_MIN_REP_LENGTH == LONG_DIGITS);
        for (int32_t i=1; i<=LONG_MIN_REP_LENGTH; ++i) LONG_MIN_REP[i-1] = buf[i];
    }
}

//eof
