/*
********************************************************************************
*
*   Copyright (C) 1997-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
********************************************************************************
*
* File DIGITLST.H
*
* Modification History:
*
*   Date        Name        Description
*   02/25/97    aliu        Converted from java.
*   03/21/97    clhuang     Updated per C++ implementation.
*   04/15/97    aliu        Changed MAX_COUNT to DBL_DIG.  Changed Digit to char.
*   09/09/97    aliu        Adapted for exponential notation support.
*   08/02/98    stephen     Added nearest/even rounding
*   06/29/99    stephen     Made LONG_DIGITS a macro to satisfy SUN compiler
*   07/09/99    stephen     Removed kMaxCount (unused, for HP compiler)
*******************************************************************************
*/
 
#ifndef DIGITLST_H
#define DIGITLST_H
 
#include "unicode/utypes.h"
#include <float.h>

// Decimal digits in a 32-bit int
#define LONG_DIGITS 19 

/**
 * Digit List. Private to DecimalFormat.  Handles the transcoding
 * between numeric values and strings of characters.  Only handles
 * non-negative numbers.  The division of labor between DigitList and
 * DecimalFormat is that DigitList handles the radix 10 representation
 * issues; DecimalFormat handles the locale-specific issues such as
 * positive/negative, grouping, decimal point, currency, and so on.
 * <P>
 * A DigitList is really a representation of a floating point value.
 * It may be an integer value; we assume that a double has sufficient
 * precision to represent all digits of a long.
 * <P>
 * The DigitList representation consists of a string of characters,
 * which are the digits radix 10, from '0' to '9'.  It also has a radix
 * 10 exponent associated with it.  The value represented by a DigitList
 * object can be computed by mulitplying the fraction f, where 0 <= f < 1,
 * derived by placing all the digits of the list to the right of the
 * decimal point, by 10^exponent.
 */
class U_COMMON_API DigitList { // Declare external to make compiler happy
public:
    DigitList();
    ~DigitList();

    DigitList(const DigitList&); // copy constructor

    DigitList& operator=(const DigitList&);  // assignment operator

    /**
     * Return true if another object is semantically equal to this one.
     */
    bool_t operator==(const DigitList& other) const;

    /**
     * Return true if another object is semantically unequal to this one.
     */
    bool_t operator!=(const DigitList& other) const { return !operator==(other); }

    /**
     * Clears out the digits.
     * Use before appending them.
     * Typically, you set a series of digits with append, then at the point
     * you hit the decimal point, you set myDigitList.fDecimalAt = myDigitList.fCount;
     * then go on appending digits.
     */
    void clear(void);

    /**
     * Appends digits to the list. Ignores all digits beyond the first DBL_DIG,
     * since they are not significant for either longs or doubles.
     */
    void append(char digit);

    /**
     * Utility routine to get the value of the digit list
     * Returns 0.0 if zero length.
     */
    double getDouble(void) const;

    /**
     * Utility routine to get the value of the digit list
     * Returns 0 if zero length.
     */
    int32_t getLong(void) const;

    /**
     * Return true if the number represented by this object can fit into
     * a long.
     */
    bool_t fitsIntoLong(bool_t isPositive, bool_t ignoreNegativeZero);

    /**
     * Utility routine to set the value of the digit list from a double
     * Input must be non-negative, and must not be Inf, -Inf, or NaN.
     * The maximum fraction digits helps us round properly.
     */
    void set(double source, int32_t maximumDigits, bool_t fixedPoint = TRUE);

    /**
     * Utility routine to set the value of the digit list from a long.
     * If a non-zero maximumDigits is specified, no more than that number of
     * significant digits will be produced.
     */
    void set(int32_t source, int32_t maximumDigits = 0);

    /**
     * Return true if this is a representation of zero.
     */
    bool_t isZero(void) const;

    /**
     * Return true if this is a representation of LONG_MIN.  You must use
     * this method to determine if this is so; you cannot check directly,
     * because a special format is used to handle this.
     */
    bool_t isLONG_MIN(void) const;

    /**
     * This is the zero digit.  Array elements fDigits[i] have values from
     * kZero to kZero + 9.  Typically, this is '0'.
     */
    static const char kZero;

public:
    /**
     * These data members are intentionally public and can be set directly.
     *<P>
     * The value represented is given by placing the decimal point before
     * fDigits[fDecimalAt].  If fDecimalAt is < 0, then leading zeros between
     * the decimal point and the first nonzero digit are implied.  If fDecimalAt
     * is > fCount, then trailing zeros between the fDigits[fCount-1] and the
     * decimal point are implied.
     * <P>
     * Equivalently, the represented value is given by f * 10^fDecimalAt.  Here
     * f is a value 0.1 <= f < 1 arrived at by placing the digits in fDigits to
     * the right of the decimal.
     * <P>
     * DigitList is normalized, so if it is non-zero, fDigits[0] is non-zero.  We
     * don't allow denormalized numbers because our exponent is effectively of
     * unlimited magnitude.  The fCount value contains the number of significant
     * digits present in fDigits[].
     * <P>
     * Zero is represented by any DigitList with fCount == 0 or with each fDigits[i]
     * for all i <= fCount == '0'.
     */
    int32_t     fDecimalAt;
    int32_t     fCount;
private:
    enum      { MAX_DIGITS = DBL_DIG };
public:
    char        fDigits[MAX_DIGITS];

private:

    /**
     * Round the representation to the given number of digits.
     * @param maximumDigits The maximum number of digits to be shown.
     * Upon return, count will be less than or equal to maximumDigits.
     */
    void round(int32_t maximumDigits);

    /**
     * Initializes the buffer that records the mimimum long value.
     */
    static void initializeLONG_MIN_REP(void);

    bool_t shouldRoundUp(int32_t maximumDigits);

    static char LONG_MIN_REP[LONG_DIGITS];
    static int32_t  LONG_MIN_REP_LENGTH;
};
 
#endif // _DIGITLST
//eof



