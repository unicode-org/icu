/*
*******************************************************************************
* Copyright (C) 1997-1999, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* File DECIMFMT.CPP
*
* Modification History:
*
*   Date        Name        Description
*   02/19/97    aliu        Converted from java.
*   03/20/97    clhuang     Implemented with new APIs.
*   03/31/97    aliu        Moved isLONG_MIN to DigitList, and fixed it.
*   04/3/97     aliu        Rewrote parsing and formatting completely, and
*                           cleaned up and debugged.  Actually works now.
*                           Implemented NAN and INF handling, for both parsing
*                           and formatting.  Extensive testing & debugging.
*   04/10/97    aliu        Modified to compile on AIX.
*   04/16/97    aliu        Rewrote to use DigitList, which has been resurrected.
*                           Changed DigitCount to int per code review.
*   07/09/97    helena      Made ParsePosition into a class.
*   08/26/97    aliu        Extensive changes to applyPattern; completely
*                           rewritten from the Java.
*   09/09/97    aliu        Ported over support for exponential formats.
*   07/20/98    stephen     JDK 1.2 sync up.
*                             Various instances of '0' replaced with 'NULL'
*                             Check for grouping size in subFormat()
*                             Brought subParse() in line with Java 1.2
*                             Added method appendAffix()
*   08/24/1998  srl         Removed Mutex calls. This is not a thread safe class!
*   02/22/99    stephen     Removed character literals for EBCDIC safety
*   06/24/99    helena      Integrated Alan's NF enhancements and Java2 bug fixes
*   06/28/99    stephen     Fixed bugs in toPattern().
*   06/29/99    stephen     Fixed operator= to copy fFormatWidth, fPad, 
*                             fPadPosition
********************************************************************************
*/
 
#include "unicode/decimfmt.h"
#include "digitlst.h"
#include "unicode/dcfmtsym.h"
#include "unicode/resbund.h"
#include "unicode/unicode.h"
#include "cmemory.h"
#include <float.h>
#include <limits.h>

// #define DEBUG

#ifdef DEBUG
#include <stdio.h>
static void debugout(UnicodeString s) {
    char buf[2000];
    s.extract((UTextOffset) 0, s.length(), buf);
    buf[s.length()] = 0;
    printf("%s", buf);
}
#define debug(x) printf("%s", x);
#else
#define debugout(x)
#define debug(x)
#endif

// *****************************************************************************
// class DecimalFormat
// *****************************************************************************

char DecimalFormat::fgClassID = 0; // Value is irrelevan

// Constants for characters used in programmatic (unlocalized) patterns.
const UChar DecimalFormat::kPatternZeroDigit           = 0x0030 /*'0'*/;
const UChar DecimalFormat::kPatternGroupingSeparator   = 0x002C /*','*/;
const UChar DecimalFormat::kPatternDecimalSeparator    = 0x002E /*'.'*/;
const UChar DecimalFormat::kPatternPerMill             = 0x2030;
const UChar DecimalFormat::kPatternPercent             = 0x0025 /*'%'*/;
const UChar DecimalFormat::kPatternDigit               = 0x0023 /*'#'*/;
const UChar DecimalFormat::kPatternSeparator           = 0x003B /*';'*/;
const UChar DecimalFormat::kPatternExponent            = 0x0045 /*'E'*/;
const UChar DecimalFormat::kPatternPlus                = 0x002B /*'+'*/;
const UChar DecimalFormat::kPatternMinus               = 0x002D /*'-'*/;
const UChar DecimalFormat::kPatternPadEscape           = 0x002A /*'*'*/;
const UChar DecimalFormat::kCurrencySign               = 0x00A4;
const UChar DecimalFormat::kQuote                      = 0x0027 /*'\''*/;

const int8_t DecimalFormat::fgMaxDigit                  = 9;

const int32_t DecimalFormat::kDoubleIntegerDigits  = 309;
const int32_t DecimalFormat::kDoubleFractionDigits = 340;

/**
 * These are the tags we expect to see in normal resource bundle files associated
 * with a locale.
 */
const char *DecimalFormat::fgNumberPatterns="NumberPatterns";

//------------------------------------------------------------------------------
// Constructs a DecimalFormat instance in the default locale.
 
DecimalFormat::DecimalFormat(UErrorCode& status)
: NumberFormat(), 
  fPosPrefixPattern(0), 
  fPosSuffixPattern(0), 
  fNegPrefixPattern(0), 
  fNegSuffixPattern(0),
  fSymbols(0)
{
    construct(status);
}

//------------------------------------------------------------------------------
// Constructs a DecimalFormat instance with the specified number format
// pattern in the default locale.

DecimalFormat::DecimalFormat(const UnicodeString& pattern,
                             UErrorCode& status)
: NumberFormat(),
  fPosPrefixPattern(0), 
  fPosSuffixPattern(0), 
  fNegPrefixPattern(0), 
  fNegSuffixPattern(0),
  fSymbols(0)
{
    construct(status, &pattern);
}

//------------------------------------------------------------------------------
// Constructs a DecimalFormat instance with the specified number format
// pattern and the number format symbols in the default locale.  The
// created instance owns the symbols.

DecimalFormat::DecimalFormat(const UnicodeString& pattern,
                             DecimalFormatSymbols* symbolsToAdopt,
                             UErrorCode& status)
: NumberFormat(),
  fPosPrefixPattern(0), 
  fPosSuffixPattern(0), 
  fNegPrefixPattern(0), 
  fNegSuffixPattern(0),
  fSymbols(0)
{
    if (symbolsToAdopt == NULL) status = U_ILLEGAL_ARGUMENT_ERROR;
    construct(status, &pattern, symbolsToAdopt);
}
 
//------------------------------------------------------------------------------
// Constructs a DecimalFormat instance with the specified number format
// pattern and the number format symbols in the default locale.  The
// created instance owns the clone of the symbols.
 
DecimalFormat::DecimalFormat(const UnicodeString& pattern,
                             const DecimalFormatSymbols& symbols,
                             UErrorCode& status)
: NumberFormat(),
  fPosPrefixPattern(0), 
  fPosSuffixPattern(0), 
  fNegPrefixPattern(0), 
  fNegSuffixPattern(0),
  fSymbols(0)
{
    construct(status, &pattern, new DecimalFormatSymbols(symbols));
}

//------------------------------------------------------------------------------
// Constructs a DecimalFormat instance with the specified number format
// pattern and the number format symbols in the desired locale.  The
// created instance owns the symbols.

void
DecimalFormat::construct(UErrorCode&             status,
                         const UnicodeString*   pattern,
                         DecimalFormatSymbols*  symbolsToAdopt,
                         const Locale&          locale)
{
    fSymbols = symbolsToAdopt; // Do this BEFORE aborting on status failure!!!
    fDigitList = new DigitList(); // Do this BEFORE aborting on status failure!!!
    fRoundingIncrement = NULL;
    fRoundingDouble = 0.0;
    fRoundingMode = kRoundHalfEven;
    fPad = kPatternPadEscape;
    fPadPosition = kPadBeforePrefix;
    if (U_FAILURE(status)) return;

    fPosPrefixPattern = fPosSuffixPattern = NULL;
    fNegPrefixPattern = fNegSuffixPattern = NULL;
    fMultiplier = 1;
    fGroupingSize = 3;
    fGroupingSize2 = 0;
    fDecimalSeparatorAlwaysShown = FALSE;
    fIsCurrencyFormat = FALSE;
    fUseExponentialNotation = FALSE;
    fMinExponentDigits = 0;

    if (fSymbols == NULL) fSymbols = new DecimalFormatSymbols(locale, status);

    UnicodeString str;
    // Uses the default locale's number format pattern if there isn't
    // one specified.
    if (pattern == NULL)
    {
        ResourceBundle resource((char *)0, Locale::getDefault(), status);

        str = resource.get(fgNumberPatterns, status).getStringEx((int32_t)0, status);
        pattern = &str;
    }

    if (U_FAILURE(status)) return;

    applyPattern(*pattern, FALSE /*not localized*/, status);
}

//------------------------------------------------------------------------------

DecimalFormat::~DecimalFormat()
{
    delete fSymbols;
    delete fDigitList;
    delete fPosPrefixPattern;
    delete fPosSuffixPattern;
    delete fNegPrefixPattern;
    delete fNegSuffixPattern;
    delete fRoundingIncrement;
}

//------------------------------------------------------------------------------
// copy constructor

DecimalFormat::DecimalFormat(const DecimalFormat &source)
:   NumberFormat(source),
    fSymbols(NULL),
    fDigitList(NULL),
    fPosPrefixPattern(NULL),
    fPosSuffixPattern(NULL),
    fNegPrefixPattern(NULL),
    fNegSuffixPattern(NULL),
    fRoundingIncrement(NULL)
{
    *this = source;
}

//------------------------------------------------------------------------------
// assignment operator
// Note that fDigitList is not considered a significant part of the
// DecimalFormat because it's used as a buffer to process the numbers.

static void _copy_us_ptr(UnicodeString** pdest, const UnicodeString* source) {
    if (source == NULL) {
        delete *pdest;
        *pdest = NULL;
    } else if (*pdest == NULL) {
        *pdest = new UnicodeString(*source);
    } else {
        **pdest  = *source;
    }
}

DecimalFormat&
DecimalFormat::operator=(const DecimalFormat& rhs)
{
  if(this != &rhs) {
    NumberFormat::operator=(rhs);
    fPositivePrefix = rhs.fPositivePrefix;
    fPositiveSuffix = rhs.fPositiveSuffix;
    fNegativePrefix = rhs.fNegativePrefix;
    fNegativeSuffix = rhs.fNegativeSuffix;
    _copy_us_ptr(&fPosPrefixPattern, rhs.fPosPrefixPattern);
    _copy_us_ptr(&fPosSuffixPattern, rhs.fPosSuffixPattern);
    _copy_us_ptr(&fNegPrefixPattern, rhs.fNegPrefixPattern);
    _copy_us_ptr(&fNegSuffixPattern, rhs.fNegSuffixPattern);
    if(rhs.fRoundingIncrement == NULL) {
      delete fRoundingIncrement;
      fRoundingIncrement = NULL;
    } 
    else if(fRoundingIncrement == NULL) {
      fRoundingIncrement = new DigitList(*rhs.fRoundingIncrement);
    }
    else {
      *fRoundingIncrement = *rhs.fRoundingIncrement;
    }
    fRoundingDouble = rhs.fRoundingDouble;
    fMultiplier = rhs.fMultiplier;
    fGroupingSize = rhs.fGroupingSize;
    fGroupingSize2 = rhs.fGroupingSize2;
    fDecimalSeparatorAlwaysShown = rhs.fDecimalSeparatorAlwaysShown;
    if(fSymbols == NULL) 
    fSymbols = new DecimalFormatSymbols(*rhs.fSymbols);
    else 
    *fSymbols = *rhs.fSymbols;
    fUseExponentialNotation = rhs.fUseExponentialNotation;
    /*Bertrand A. D. Update 98.03.17*/
    fIsCurrencyFormat = rhs.fIsCurrencyFormat;
    /*end of Update*/
    fMinExponentDigits = rhs.fMinExponentDigits;
    if (fDigitList == NULL) fDigitList = new DigitList();
    
    /* sfb 990629 */
    fFormatWidth = rhs.fFormatWidth;
    fPad = rhs.fPad;
    fPadPosition = rhs.fPadPosition;
    /* end sfb */
  }
  return *this;
}

//------------------------------------------------------------------------------

UBool
DecimalFormat::operator==(const Format& that) const
{
    if (this == &that)
        return TRUE;

    if (getDynamicClassID() != that.getDynamicClassID())
        return FALSE;

    const DecimalFormat* other = (DecimalFormat*)&that;

#if 0
    // This code makes it easy to determine why two format objects that should
    // be equal aren't.
    UBool first = TRUE;
    if (!NumberFormat::operator==(that)) {
        if (first) { printf("[ "); first = FALSE; } else { printf(", "); }
        debug("NumberFormat::!=");
    }
    if (!((fPosPrefixPattern == other->fPosPrefixPattern && // both null
              fPositivePrefix == other->fPositivePrefix)
           || (fPosPrefixPattern != 0 && other->fPosPrefixPattern != 0 &&
               *fPosPrefixPattern  == *other->fPosPrefixPattern))) {
        if (first) { printf("[ "); first = FALSE; } else { printf(", "); }
        debug("Pos Prefix !=");
    }
    if (!((fPosSuffixPattern == other->fPosSuffixPattern && // both null
           fPositiveSuffix == other->fPositiveSuffix)
          || (fPosSuffixPattern != 0 && other->fPosSuffixPattern != 0 &&
              *fPosSuffixPattern  == *other->fPosSuffixPattern))) {
        if (first) { printf("[ "); first = FALSE; } else { printf(", "); }
        debug("Pos Suffix !=");
    }
    if (!((fNegPrefixPattern == other->fNegPrefixPattern && // both null
           fNegativePrefix == other->fNegativePrefix)
          || (fNegPrefixPattern != 0 && other->fNegPrefixPattern != 0 &&
              *fNegPrefixPattern  == *other->fNegPrefixPattern))) {
        if (first) { printf("[ "); first = FALSE; } else { printf(", "); }
        debug("Neg Prefix ");
        if (fNegPrefixPattern == NULL) {
            debug("NULL(");
            debugout(fNegativePrefix);
            debug(")");
        } else {
            debugout(*fNegPrefixPattern);
        }
        debug(" != ");
        if (other->fNegPrefixPattern == NULL) {
            debug("NULL(");
            debugout(other->fNegativePrefix);
            debug(")");
        } else {
            debugout(*other->fNegPrefixPattern);
        }
    }
    if (!((fNegSuffixPattern == other->fNegSuffixPattern && // both null
           fNegativeSuffix == other->fNegativeSuffix)
          || (fNegSuffixPattern != 0 && other->fNegSuffixPattern != 0 &&
              *fNegSuffixPattern  == *other->fNegSuffixPattern))) {
        if (first) { printf("[ "); first = FALSE; } else { printf(", "); }
        debug("Neg Suffix ");
        if (fNegSuffixPattern == NULL) {
            debug("NULL(");
            debugout(fNegativeSuffix);
            debug(")");
        } else {
            debugout(*fNegSuffixPattern);
        }
        debug(" != ");
        if (other->fNegSuffixPattern == NULL) {
            debug("NULL(");
            debugout(other->fNegativeSuffix);
            debug(")");
        } else {
            debugout(*other->fNegSuffixPattern);
        }
    }
    if (!((fRoundingIncrement == other->fRoundingIncrement) // both null
          || (fRoundingIncrement != NULL &&
              other->fRoundingIncrement != NULL &&
              *fRoundingIncrement == *other->fRoundingIncrement))) {
        if (first) { printf("[ "); first = FALSE; } else { printf(", "); }
        debug("Rounding Increment !=");
              }
    if (fMultiplier != other->fMultiplier) {
        if (first) { printf("[ "); first = FALSE; }
        printf("Multiplier %ld != %ld", fMultiplier, other->fMultiplier);
    }
    if (fGroupingSize != other->fGroupingSize) {
        if (first) { printf("[ "); first = FALSE; } else { printf(", "); }
        printf("Grouping Size %ld != %ld", fGroupingSize, other->fGroupingSize);
    }
    if (fGroupingSize2 != other->fGroupingSize2) {
        if (first) { printf("[ "); first = FALSE; } else { printf(", "); }
        printf("Secondary Grouping Size %ld != %ld", fGroupingSize2, other->fGroupingSize2);
    }
    if (fDecimalSeparatorAlwaysShown != other->fDecimalSeparatorAlwaysShown) {
        if (first) { printf("[ "); first = FALSE; } else { printf(", "); }
        printf("Dec Sep Always %d != %d", fDecimalSeparatorAlwaysShown, other->fDecimalSeparatorAlwaysShown);
    }
    if (fUseExponentialNotation != other->fUseExponentialNotation) {
        if (first) { printf("[ "); first = FALSE; } else { printf(", "); }
        debug("Use Exp !=");
    }
    if (!(!fUseExponentialNotation ||
          fMinExponentDigits != other->fMinExponentDigits)) {
        if (first) { printf("[ "); first = FALSE; } else { printf(", "); }
        debug("Exp Digits !=");
    }
    if (*fSymbols != *(other->fSymbols)) {
        if (first) { printf("[ "); first = FALSE; } else { printf(", "); }
        debug("Symbols !=");
    }
    if (!first) { printf(" ]"); }
#endif

    return (NumberFormat::operator==(that) &&
            ((fPosPrefixPattern == other->fPosPrefixPattern && // both null
              fPositivePrefix == other->fPositivePrefix)
             || (fPosPrefixPattern != 0 && other->fPosPrefixPattern != 0 &&
                 *fPosPrefixPattern  == *other->fPosPrefixPattern)) &&
            ((fPosSuffixPattern == other->fPosSuffixPattern && // both null
              fPositiveSuffix == other->fPositiveSuffix)
             || (fPosSuffixPattern != 0 && other->fPosSuffixPattern != 0 &&
                 *fPosSuffixPattern  == *other->fPosSuffixPattern)) &&
            ((fNegPrefixPattern == other->fNegPrefixPattern && // both null
              fNegativePrefix == other->fNegativePrefix)
             || (fNegPrefixPattern != 0 && other->fNegPrefixPattern != 0 &&
                 *fNegPrefixPattern  == *other->fNegPrefixPattern)) &&
            ((fNegSuffixPattern == other->fNegSuffixPattern && // both null
              fNegativeSuffix == other->fNegativeSuffix)
             || (fNegSuffixPattern != 0 && other->fNegSuffixPattern != 0 &&
                 *fNegSuffixPattern  == *other->fNegSuffixPattern)) &&
            ((fRoundingIncrement == other->fRoundingIncrement) // both null
             || (fRoundingIncrement != NULL &&
                 other->fRoundingIncrement != NULL &&
                 *fRoundingIncrement == *other->fRoundingIncrement)) &&
        fMultiplier == other->fMultiplier &&
        fGroupingSize == other->fGroupingSize &&
        fGroupingSize2 == other->fGroupingSize2 &&
        fDecimalSeparatorAlwaysShown == other->fDecimalSeparatorAlwaysShown &&
        fUseExponentialNotation == other->fUseExponentialNotation &&
        (!fUseExponentialNotation ||
         fMinExponentDigits == other->fMinExponentDigits) &&
        *fSymbols == *(other->fSymbols));
}

//------------------------------------------------------------------------------

Format*
DecimalFormat::clone() const
{
    return new DecimalFormat(*this);
}

//------------------------------------------------------------------------------
 
UnicodeString&
DecimalFormat::format(int32_t number,
                      UnicodeString& result,
                      FieldPosition& fieldPosition) const
{
    // Clears field positions.
    fieldPosition.setBeginIndex(0);
    fieldPosition.setEndIndex(0);

    // If we are to do rounding, we need to move into the BigDecimal
    // domain in order to do divide/multiply correctly.
    if (fRoundingIncrement != NULL) {
        return format((double) number, result, fieldPosition);
    }

    UBool isNegative = (number < 0);
    if (isNegative) number = -number; // NOTE: number will still be negative if it is LONG_MIN

    // In general, long values always represent real finite numbers, so
    // we don't have to check for +/- Infinity or NaN.  However, there
    // is one case we have to be careful of:  The multiplier can push
    // a number near MIN_VALUE or MAX_VALUE outside the legal range.  We
    // check for this before multiplying, and if it happens we use doubles
    // instead, trading off accuracy for range.
    if (fMultiplier != 1 && fMultiplier != 0)
    {
        UBool useDouble = FALSE;

        if (number < 0) // This can only happen if number == Long.MIN_VALUE
        {
            int32_t cutoff = INT32_MIN / fMultiplier;
            useDouble = (number < cutoff);
        }
        else
        {
            int32_t cutoff = INT32_MAX / fMultiplier;
            useDouble = (number > cutoff);
        }
        // use double to format the number instead so we don't get out
        // of range errors.
        if (useDouble)
        {
            double dnumber = (double)(isNegative ? -number : number);
            return format(dnumber, result, fieldPosition);
        }
    }

    number *= fMultiplier;
    DecimalFormat *non_const = (DecimalFormat*)this;
    non_const->fDigitList->set(number, fUseExponentialNotation ?
                               getMinimumIntegerDigits() + getMaximumFractionDigits() : 0);

    return subformat(result, fieldPosition, isNegative, TRUE);
}
 
//------------------------------------------------------------------------------

UnicodeString&
DecimalFormat::format(  double number,
                        UnicodeString& result,
                        FieldPosition& fieldPosition) const
{
    // Clears field positions.
    fieldPosition.setBeginIndex(0);
    fieldPosition.setEndIndex(0);

    // Special case for NaN, sets the begin and end index to be the
    // the string length of localized name of NaN.
    if (uprv_isNaN(number))
    {
        if (fieldPosition.getField() == NumberFormat::kIntegerField)
            fieldPosition.setBeginIndex(result.length());

        UnicodeString nan;
        result += fSymbols->getNaN(nan);

        if (fieldPosition.getField() == NumberFormat::kIntegerField)
            fieldPosition.setEndIndex(result.length());

        addPadding(result, FALSE, FALSE /*ignored*/);
        return result;
    }

    /* Detecting whether a double is negative is easy with the exception of
     * the value -0.0.  This is a double which has a zero mantissa (and
     * exponent), but a negative sign bit.  It is semantically distinct from
     * a zero with a positive sign bit, and this distinction is important
     * to certain kinds of computations.  However, it's a little tricky to
     * detect, since (-0.0 == 0.0) and !(-0.0 < 0.0).  How then, you may
     * ask, does it behave distinctly from +0.0?  Well, 1/(-0.0) ==
     * -Infinity.  Proper detection of -0.0 is needed to deal with the
     * issues raised by bugs 4106658, 4106667, and 4147706.  Liu 7/6/98.
     */
    UBool isNegative = uprv_isNegative(number);
    if (isNegative) number = -number;

    // Do this BEFORE checking to see if value is infinite! Sets the
    // begin and end index to be length of the string composed of
    // localized name of Infinite and the positive/negative localized
    // signs.

    if (fMultiplier != 1) number *= fMultiplier;

    // Apply rounding after multiplier
    if (fRoundingIncrement != NULL) {
        number = fRoundingDouble
            * round(number / fRoundingDouble, fRoundingMode, isNegative);
    }

    // Special case for INFINITE,
    if (uprv_isInfinite(number))
    {
        result += (isNegative ? fNegativePrefix : fPositivePrefix);

        if (fieldPosition.getField() == NumberFormat::kIntegerField)
            fieldPosition.setBeginIndex(result.length());

        UnicodeString inf;
        result += fSymbols->getInfinity(inf);

        if (fieldPosition.getField() == NumberFormat::kIntegerField)
            fieldPosition.setEndIndex(result.length());

        result += (isNegative ? fNegativeSuffix : fPositiveSuffix);

        addPadding(result, TRUE, isNegative);
        return result;
    }

    // At this point we are guaranteed a nonnegative finite
    // number.
    DecimalFormat* non_const = (DecimalFormat*)this;
    // Sets up the digit list buffer with the number.
    // Please see digitlst.cpp for the details regarding DigitList.
    non_const->fDigitList->set(number, fUseExponentialNotation ?
                             getMinimumIntegerDigits() + getMaximumFractionDigits() :
                             getMaximumFractionDigits(),
                             !fUseExponentialNotation);

    return subformat(result, fieldPosition, isNegative, FALSE);
}
 
/**
 * Round a double value to the nearest integer according to the
 * given mode.
 * @param a the absolute value of the number to be rounded
 * @param mode a BigDecimal rounding mode
 * @param isNegative true if the number to be rounded is negative
 * @return the absolute value of the rounded result
 */
double DecimalFormat::round(double a, ERoundingMode mode, UBool isNegative) {
    switch (mode) {
    case kRoundCeiling:
        return isNegative ? uprv_floor(a) : uprv_ceil(a);
    case kRoundFloor:
        return isNegative ? uprv_ceil(a) : uprv_floor(a);
    case kRoundDown:
        return uprv_floor(a);
    case kRoundUp:
        return uprv_ceil(a);
    case kRoundHalfEven:
        {
            double f = uprv_floor(a);
            if ((a - f) != 0.5) {
                return uprv_floor(a + 0.5);
            }
            double g = f / 2.0;
            return (g == uprv_floor(g)) ? f : (f + 1.0);
        }
    case kRoundHalfDown:
        return ((a - uprv_floor(a)) <= 0.5) ? uprv_floor(a) : uprv_ceil(a);
    case kRoundHalfUp:
        return ((a - uprv_floor(a)) < 0.5) ? uprv_floor(a) : uprv_ceil(a);
    }
    return 1.0;
}

UnicodeString&
DecimalFormat::format(  const Formattable& obj,
                        UnicodeString& result,
                        FieldPosition& fieldPosition,
                        UErrorCode& status) const
{
    return NumberFormat::format(obj, result, fieldPosition, status);
}

/**
 * Return true if a grouping separator belongs at the given
 * position, based on whether grouping is in use and the values of
 * the primary and secondary grouping interval.
 * @param pos the number of integer digits to the right of
 * the current position.  Zero indicates the position after the
 * rightmost integer digit.
 * @return true if a grouping character belongs at the current
 * position.
 */
UBool DecimalFormat::isGroupingPosition(int32_t pos) const {
    UBool result = FALSE;
    if (isGroupingUsed() && (pos > 0) && (fGroupingSize > 0)) {
        if ((fGroupingSize2 > 0) && (pos > fGroupingSize)) {
            result = ((pos - fGroupingSize) % fGroupingSize2) == 0;
        } else {
            result = pos % fGroupingSize == 0;
        }
    }
    return result;
}

//------------------------------------------------------------------------------

/**
 * Complete the formatting of a finite number.  On entry, the fDigitList must
 * be filled in with the correct digits.
 */
UnicodeString&
DecimalFormat::subformat(UnicodeString& result,
                         FieldPosition& fieldPosition,
                         UBool         isNegative,
                         UBool         isInteger) const
{
    // Gets the localized zero Unicode character.
    UChar zero = fSymbols->getZeroDigit();
    int32_t zeroDelta = zero - '0'; // '0' is the DigitList representation of zero
    UChar grouping = fSymbols->getGroupingSeparator();
    UChar decimal = fIsCurrencyFormat ?
        fSymbols->getMonetaryDecimalSeparator() :
        fSymbols->getDecimalSeparator();
    int32_t maxIntDig = getMaximumIntegerDigits();
    int32_t minIntDig = getMinimumIntegerDigits();

    /* Per bug 4147706, DecimalFormat must respect the sign of numbers which
     * format as zero.  This allows sensible computations and preserves
     * relations such as signum(1/x) = signum(x), where x is +Infinity or
     * -Infinity.  Prior to this fix, we always formatted zero values as if
     * they were positive.  Liu 7/6/98.
     */
    if (fDigitList->isZero())
    {
        fDigitList->fDecimalAt = fDigitList->fCount = 0; // Normalize
    }

    // Appends the prefix.
    result += (isNegative ? fNegativePrefix : fPositivePrefix);

    if (fUseExponentialNotation)
    {
        // Record field information for caller.
        if (fieldPosition.getField() == NumberFormat::kIntegerField)
        {
            fieldPosition.setBeginIndex(result.length());
            fieldPosition.setEndIndex(-1);
        }
        else if (fieldPosition.getField() == NumberFormat::kFractionField)
        {
            fieldPosition.setBeginIndex(-1);
        }

        // Minimum integer digits are handled in exponential format by
        // adjusting the exponent.  For example, 0.01234 with 3 minimum
        // integer digits is "123.4E-4".

        // Maximum integer digits are interpreted as indicating the
        // repeating range.  This is useful for engineering notation, in
        // which the exponent is restricted to a multiple of 3.  For
        // example, 0.01234 with 3 maximum integer digits is "12.34e-3".
        // If maximum integer digits are defined and are larger than
        // minimum integer digits, then minimum integer digits are
        // ignored.
        int32_t exponent = fDigitList->fDecimalAt;
        if (maxIntDig > 1 && maxIntDig != minIntDig) {
            // A exponent increment is defined; adjust to it.
            exponent = (exponent > 0) ? (exponent - 1) / maxIntDig
                                      : (exponent / maxIntDig) - 1;
            exponent *= maxIntDig;
        } else {
            // No exponent increment is defined; use minimum integer digits.
            // If none is specified, as in "#E0", generate 1 integer digit.
            exponent -= (minIntDig > 0 || getMinimumFractionDigits() > 0)
                        ? minIntDig : 1;
        }

        // We now output a minimum number of digits, and more if there
        // are more digits, up to the maximum number of digits.  We
        // place the decimal point after the "integer" digits, which
        // are the first (decimalAt - exponent) digits.
        int32_t minimumDigits =  minIntDig + getMinimumFractionDigits();
        // The number of integer digits is handled specially if the number
        // is zero, since then there may be no digits.
        int32_t integerDigits = fDigitList->isZero() ? minIntDig :
            fDigitList->fDecimalAt - exponent;
        int32_t totalDigits = fDigitList->fCount;
        if (minimumDigits > totalDigits) totalDigits = minimumDigits;
        if (integerDigits > totalDigits) totalDigits = integerDigits;

        // totalDigits records total number of digits needs to be processed
        int32_t i;
        for (i=0; i<totalDigits; ++i)
        {
            if (i == integerDigits)
            {
                // Record field information for caller.
                if (fieldPosition.getField() == NumberFormat::kIntegerField)
                    fieldPosition.setEndIndex(result.length());

                result += (decimal);

                // Record field information for caller.
                if (fieldPosition.getField() == NumberFormat::kFractionField)
                    fieldPosition.setBeginIndex(result.length());
            }
            // Restores the digit character or pads the buffer with zeros.
            UChar c = ((i < fDigitList->fCount) ?
                          (UChar)(fDigitList->fDigits[i] + zeroDelta) :
                          zero);
            result += c;
        }

        // Record field information
        if (fieldPosition.getField() == NumberFormat::kIntegerField)
        {
            if (fieldPosition.getEndIndex() < 0)
                fieldPosition.setEndIndex(result.length());
        }
        else if (fieldPosition.getField() == NumberFormat::kFractionField)
        {
            if (fieldPosition.getBeginIndex() < 0)
                fieldPosition.setBeginIndex(result.length());
            fieldPosition.setEndIndex(result.length());
        }

        // The exponent is output using the pattern-specified minimum
        // exponent digits.  There is no maximum limit to the exponent
        // digits, since truncating the exponent would result in an
        // unacceptable inaccuracy.
        result += fSymbols->getExponentialSymbol();
        
        // For zero values, we force the exponent to zero.  We
        // must do this here, and not earlier, because the value
        // is used to determine integer digit count above.
        if (fDigitList->isZero()) exponent = 0;

        UBool negativeExponent = exponent < 0;
        if (negativeExponent) {
            exponent = -exponent;
            result += fSymbols->getMinusSign();
        } else if (fExponentSignAlwaysShown) {
            result += fSymbols->getPlusSign();
        }
        if (negativeExponent) exponent = -exponent;
        DecimalFormat* non_const = (DecimalFormat*)this;
        non_const->fDigitList->set(exponent);
        for (i=fDigitList->fDecimalAt; i<fMinExponentDigits; ++i)
            result += (zero);
        for (i=0; i<fDigitList->fDecimalAt; ++i)
        {
            UChar c = ((i < fDigitList->fCount) ?
                          (UChar)(fDigitList->fDigits[i] + zeroDelta) : zero);
            result += c;
        }
    }
    else  // Not using exponential notation
    {
        // Record field information for caller.
        if (fieldPosition.getField() == NumberFormat::kIntegerField)
            fieldPosition.setBeginIndex(result.length());

        // Output the integer portion.  Here 'count' is the total
        // number of integer digits we will display, including both
        // leading zeros required to satisfy getMinimumIntegerDigits,
        // and actual digits present in the number.
        int32_t count = minIntDig;
        int32_t digitIndex = 0; // Index into fDigitList->fDigits[]
        if (fDigitList->fDecimalAt > 0 && count < fDigitList->fDecimalAt)
            count = fDigitList->fDecimalAt;

        // Handle the case where getMaximumIntegerDigits() is smaller
        // than the real number of integer digits.  If this is so, we
        // output the least significant max integer digits.  For example,
        // the value 1997 printed with 2 max integer digits is just "97".

        if (count > maxIntDig)
        {
            count = maxIntDig;
            digitIndex = fDigitList->fDecimalAt - count;
        }

        int32_t sizeBeforeIntegerPart = result.length();

        int32_t i;
        for (i=count-1; i>=0; --i)
        {
            if (i < fDigitList->fDecimalAt && digitIndex < fDigitList->fCount)
            {
                // Output a real digit
                result += ((UChar)(fDigitList->fDigits[digitIndex++] + zeroDelta));
            }
            else
            {
                // Output a leading zero
                result += (zero);
            }

            // Output grouping separator if necessary.
            if (isGroupingPosition(i)) {
                result.append(grouping);
            }
        }

        // Record field information for caller.
        if (fieldPosition.getField() == NumberFormat::kIntegerField)
            fieldPosition.setEndIndex(result.length());

        // Determine whether or not there are any printable fractional
        // digits.  If we've used up the digits we know there aren't.
        UBool fractionPresent = (getMinimumFractionDigits() > 0) ||
            (!isInteger && digitIndex < fDigitList->fCount);

        // If there is no fraction present, and we haven't printed any
        // integer digits, then print a zero.  Otherwise we won't print
        // _any_ digits, and we won't be able to parse this string.
        if (!fractionPresent && result.length() == sizeBeforeIntegerPart)
            result += (zero);

        // Output the decimal separator if we always do so.
        if (fDecimalSeparatorAlwaysShown || fractionPresent)
            result += (decimal);

        // Record field information for caller.
        if (fieldPosition.getField() == NumberFormat::kFractionField)
            fieldPosition.setBeginIndex(result.length());

        for (i=0; i < getMaximumFractionDigits(); ++i)
        {
            // Here is where we escape from the loop.  We escape if we've output
            // the maximum fraction digits (specified in the for expression above).
            // We also stop when we've output the minimum digits and either:
            // we have an integer, so there is no fractional stuff to display,
            // or we're out of significant digits.
            if (i >= getMinimumFractionDigits() &&
                (isInteger || digitIndex >= fDigitList->fCount))
                break;

            // Output leading fractional zeros.  These are zeros that come after
            // the decimal but before any significant digits.  These are only
            // output if abs(number being formatted) < 1.0.
            if (-1-i > (fDigitList->fDecimalAt-1))
            {
                result += (zero);
                continue;
            }

            // Output a digit, if we have any precision left, or a
            // zero if we don't.  We don't want to output noise digits.
            if (!isInteger && digitIndex < fDigitList->fCount)
            {
                result += ((UChar)(fDigitList->fDigits[digitIndex++] + zeroDelta));
            }
            else
            {
                result += (zero);
            }
        }

        // Record field information for caller.
        if (fieldPosition.getField() == NumberFormat::kFractionField)
            fieldPosition.setEndIndex(result.length());
    }

    result += (isNegative ? fNegativeSuffix : fPositiveSuffix);

    addPadding(result, TRUE, isNegative);
    return result;
}

/**
 * Inserts the character fPad as needed to expand result to fFormatWidth.
 * @param result the string to be padded
 * @param hasAffixes if true, padding is positioned appropriately before or
 * after affixes.  If false, then isNegative is ignored, and there are only
 * two effective pad positions: kPadBeforePrefix/kPadAfterPrefix and
 * kPadBeforeSuffix/kPadAfterSuffix.
 * @param isNegative must be true if result contains a formatted negative
 * number, and false otherwise.  Ignored if hasAffixes is false.
 */
void DecimalFormat::addPadding(UnicodeString& result, UBool hasAffixes,
                               UBool isNegative) const {
    if (fFormatWidth > 0) {
        int32_t len = fFormatWidth - result.length();
        if (len > 0) {
            UChar* padding = (UChar*) uprv_malloc(sizeof(UChar) * len);
            for (int32_t i=0; i<len; ++i) {
                padding[i] = fPad;
            }
            switch (fPadPosition) {
            case kPadAfterPrefix:
                if (hasAffixes) {
                    result.insert(isNegative ? fNegativePrefix.length()
                                             : fPositivePrefix.length(),
                                  padding, len);
                    break;
                } // else fall through to next case
            case kPadBeforePrefix:
                result.insert(0, padding, len);
                break;
            case kPadBeforeSuffix:
                if (hasAffixes) {
                    result.insert(result.length() -
                                  (isNegative ? fNegativeSuffix.length()
                                              : fPositiveSuffix.length()),
                                  padding, len);
                    break;
                } // else fall through to next case
            case kPadAfterSuffix:
                result += padding;
                break;
            }
            uprv_free(padding);
        }
    }
}

//------------------------------------------------------------------------------
 
void
DecimalFormat::parse(const UnicodeString& text,
                     Formattable& result,
                     UErrorCode& status) const
{
    NumberFormat::parse(text, result, status);
}

const int32_t DecimalFormat::fgStatusInfinite    = 0;
const int32_t DecimalFormat::fgStatusPositive    = 1;
const int32_t DecimalFormat::fgStatusLength        = 2;

void
DecimalFormat::parse(const UnicodeString& text,
                     Formattable& result,
                     ParsePosition& parsePosition) const
{
    // Skip padding characters, if any
    int32_t backup = parsePosition.getIndex();
    int32_t i = backup;
    if (fFormatWidth > 0) {
        while (i < text.length() && text[(UTextOffset) i] == fPad) {
            ++i;
        }
        parsePosition.setIndex(i);
    }

    // special case NaN
    // If the text is composed of the representation of NaN, returns NaN.
    int32_t nanLen = fSymbols->compareNaN(text, parsePosition.getIndex());
    if (nanLen) {
        parsePosition.setIndex(parsePosition.getIndex() + nanLen);
        result.setDouble(uprv_getNaN());
        return;
    }

    // status is used to record whether a number is
    // infinite or positive.
    UBool status[fgStatusLength];

    if (!subparse(text, parsePosition, *fDigitList, FALSE, status)) {
        parsePosition.setIndex(backup);
        return;
    } else if (fFormatWidth < 0) {
        i = parsePosition.getIndex();
        while (i < text.length() && text[(UTextOffset) i] == fPad) {
            ++i;
        }
        parsePosition.setIndex(i);
    }

    // Handle infinity
    if (status[fgStatusInfinite]) {
        double inf = uprv_getInfinity();
        result.setDouble(status[fgStatusPositive] ? inf : -inf);
        return;
    }

    // Do as much of the multiplier conversion as possible without
    // losing accuracy.
    int32_t mult = fMultiplier; // Don't modify this.multiplier
    while (mult % 10 == 0) {
        --fDigitList->fDecimalAt;
        mult /= 10;
    }

    // Handle integral values
    if (fDigitList->fitsIntoLong(status[fgStatusPositive],
                                 isParseIntegerOnly())) {
        int32_t n = fDigitList->getLong();
        if (n % mult == 0) {
            n /= mult;
            result.setLong((status[fgStatusPositive] == n>=0) ? n : -n);
            return;
        }
    }

    // Handle non-integral values
    double a = fDigitList->getDouble();
    if (mult != 1) {
        a /= mult;
    }
    result.setDouble(status[fgStatusPositive] ? a : -a);
}

/**
 * Parse the given text into a number.  The text is parsed beginning at
 * parsePosition, until an unparseable character is seen.
 * @param text The string to parse.
 * @param parsePosition The position at which to being parsing.  Upon
 * return, the first unparseable character.
 * @param digits The DigitList to set to the parsed value.
 * @param isExponent If true, parse an exponent.  This means no
 * infinite values and integer only.
 * @param status Upon return contains boolean status flags indicating
 * whether the value was infinite and whether it was positive.
 */
UBool DecimalFormat::subparse(const UnicodeString& text, ParsePosition& parsePosition,
                               DigitList& digits, UBool isExponent,
                               UBool* status) const
{
    int32_t position = parsePosition.getIndex();
    int32_t oldStart = parsePosition.getIndex();
    int32_t backup;

    // check for positivePrefix; take longest
    UBool gotPositive = text.compare(position,fPositivePrefix.length(),fPositivePrefix,0,
                                      fPositivePrefix.length()) == 0;
    UBool gotNegative = text.compare(position,fNegativePrefix.length(),fNegativePrefix,0,
                                      fNegativePrefix.length()) == 0;
    // If the number is positive and negative at the same time,
    // 1. the number is positive if the positive prefix is longer
    // 2. the number is negative if the negative prefix is longer
    if (gotPositive && gotNegative) {
        if (fPositivePrefix.length() > fNegativePrefix.length())
            gotNegative = FALSE;
        else if (fPositivePrefix.length() < fNegativePrefix.length())
            gotPositive = FALSE;
    }
    if(gotPositive)
        position += fPositivePrefix.length();
    else if(gotNegative)
        position += fNegativePrefix.length();
    else {
        parsePosition.setErrorIndex(position);
        return FALSE;
    }
    // process digits or Inf, find decimal position
    status[fgStatusInfinite] = FALSE;
    int32_t infLen = fSymbols->compareInfinity(text, position);
    if (!isExponent && infLen)
    {
        // Found a infinite number.
        position += infLen;
        status[fgStatusInfinite] = TRUE;
    } else {
        // We now have a string of digits, possibly with grouping symbols,
        // and decimal points.  We want to process these into a DigitList.
        // We don't want to put a bunch of leading zeros into the DigitList
        // though, so we keep track of the location of the decimal point,
        // put only significant digits into the DigitList, and adjust the
        // exponent as needed.

        digits.fDecimalAt = digits.fCount = 0;
        UChar zero = fSymbols->getZeroDigit();
        UChar decimal = fIsCurrencyFormat ?
            fSymbols->getMonetaryDecimalSeparator() : fSymbols->getDecimalSeparator();
        UChar grouping = fSymbols->getGroupingSeparator();
        UChar exponentChar = fSymbols->getExponentialSymbol();
        UBool sawDecimal = FALSE;
        UBool sawExponent = FALSE;
        UBool sawDigit = FALSE;
        int32_t exponent = 0; // Set to the exponent value, if any
        UChar ch;
        int32_t digit;

        // We have to track digitCount ourselves, because digits.fCount will
        // pin when the maximum allowable digits is reached.
        int32_t digitCount = 0;

        backup = -1;
        for (; position < text.length(); ++position)
        {
            ch = text[(UTextOffset)position];

            /* We recognize all digit ranges, not only the Latin digit range
             * '0'..'9'.  We do so by using the Character.digit() method,
             * which converts a valid Unicode digit to the range 0..9.
             *
             * The character 'ch' may be a digit.  If so, place its value
             * from 0 to 9 in 'digit'.  First try using the locale digit,
             * which may or MAY NOT be a standard Unicode digit range.  If
             * this fails, try using the standard Unicode digit ranges by
             * calling Character.digit().  If this also fails, digit will
             * have a value outside the range 0..9.
             */
            digit = ch - zero;
            if (digit < 0 || digit > 9)
            {
                digit = Unicode::digitValue(ch);
            }

            if (digit > 0 && digit <= 9)
            {
                sawDigit = TRUE;
                // output a regular non-zero digit.
                ++digitCount;
                digits.append((char)(digit + '0'));

                // Cancel out backup setting (see grouping handler below)
                backup = -1;
            }
            else if (digit == 0)
            {
                // Cancel out backup setting (see grouping handler below)
                backup = -1; // Do this BEFORE continue statement below!!!
                sawDigit = TRUE;

                // Handle leading zeros
                if (digits.fCount == 0)
                {
                    if (sawDecimal)
                    {
                        // If we have seen the decimal, but no significant digits yet,
                        // then we account for leading zeros by decrementing the
                        // digits.fDecimalAt into negative values.
                        --digits.fDecimalAt;
                    }
                    // else ignore leading zeros in integer part of number.
                }
                else
                {
                    // output a regular zero digit.
                    ++digitCount;
                    digits.append((char)(digit + '0'));
                }
            }
            else if (!isExponent && ch == decimal)
            {
                // If we're only parsing integers, or if we ALREADY saw the
                // decimal, then don't parse this one.
                if (isParseIntegerOnly() || sawDecimal)
                    break;
                digits.fDecimalAt = digitCount; // Not digits.fCount!
                sawDecimal = TRUE;
            }
            else if (!isExponent && ch == grouping && isGroupingUsed())
            {
                // Ignore grouping characters, if we are using them, but require
                // that they be followed by a digit.  Otherwise we backup and
                // reprocess them.
                backup = position;
            }
            else if (!isExponent && ch == exponentChar && !sawExponent)
            {
                // Parse sign, if present
                UBool negExp = FALSE;
                int32_t pos = position + 1; // position + exponentSep.length();
                if (pos < text.length())
                {
                    ch = text[(UTextOffset) pos];
                    if (ch == fSymbols->getPlusSign())
                    {
                        ++pos;
                    }
                    else if (ch == fSymbols->getMinusSign())
                    {
                        ++pos;
                        negExp = TRUE;
                    }
                }

                DigitList exponentDigits;
                exponentDigits.fCount = 0;
                while (pos < text.length()) {
                    ch = text[(UTextOffset)pos];
                    digit = ch - zero;

                    if (digit < 0 || digit > 9) {
                        digit = Unicode::digitValue(ch);
                    }
                    if (digit >= 0 && digit <= 9) {
                        exponentDigits.append((char)(digit + '0'));
                        ++pos;
                    } else {
                        break;
                    }
                }

                if (exponentDigits.fCount > 0) {
                    exponentDigits.fDecimalAt = exponentDigits.fCount;
                    exponent = exponentDigits.getLong();
                    if (negExp) {
                        exponent = -exponent;
                    }
                    position = pos; // Advance past the exponent
                    sawExponent = TRUE;
                }

                break; // Whether we fail or succeed, we exit this loop
            }
            else
                break;
        }

        if (backup != -1)
        {
            position = backup;
        }

        // If there was no decimal point we have an integer
        if (!sawDecimal)
        {
            digits.fDecimalAt = digitCount; // Not digits.fCount!
        }

        // Adjust for exponent, if any
        digits.fDecimalAt += exponent;

        // If none of the text string was recognized.  For example, parse
        // "x" with pattern "#0.00" (return index and error index both 0)
        // parse "$" with pattern "$#0.00". (return index 0 and error index
        // 1).
        if (!sawDigit && digitCount == 0) {
            parsePosition.setIndex(oldStart);
            parsePosition.setErrorIndex(oldStart);
            return FALSE;
        }
    }

    // check for positiveSuffix
    if (gotPositive)
    {
        gotPositive = text.compare(position,fPositiveSuffix.length(),fPositiveSuffix,0,
                                   fPositiveSuffix.length()) == 0;
    }
    if (gotNegative)
    {
        gotNegative = text.compare(position,fNegativeSuffix.length(),fNegativeSuffix,0,
                                   fNegativeSuffix.length()) == 0;
    }

    // if both match, take longest
    if (gotPositive && gotNegative)
    {
        if (fPositiveSuffix.length() > fNegativeSuffix.length())
        {
            gotNegative = FALSE;
        }
        else if (fPositiveSuffix.length() < fNegativeSuffix.length())
        {
            gotPositive = FALSE;
        }
    }

    // fail if neither or both
    if (gotPositive == gotNegative)
    {
        parsePosition.setErrorIndex(position);
        return FALSE;
    }

    parsePosition.setIndex(position +
                           (gotPositive ? fPositiveSuffix.length() :
                            fNegativeSuffix.length())); // mark success!

    status[fgStatusPositive] = gotPositive;

    if(parsePosition.getIndex() == oldStart)
    {
        parsePosition.setErrorIndex(position);
        return FALSE;
    }
    return TRUE;
}

//------------------------------------------------------------------------------
// Gets the pointer to the localized decimal format symbols

const DecimalFormatSymbols*
DecimalFormat::getDecimalFormatSymbols() const
{
    return fSymbols;
}

//------------------------------------------------------------------------------
// De-owning the current localized symbols and adopt the new symbols.

void
DecimalFormat::adoptDecimalFormatSymbols(DecimalFormatSymbols* symbolsToAdopt)
{
    if (fSymbols != NULL)
        delete fSymbols;

    fSymbols = symbolsToAdopt;
}
//------------------------------------------------------------------------------
// Setting the symbols is equlivalent to adopting a newly created localized
// symbols.

void
DecimalFormat::setDecimalFormatSymbols(const DecimalFormatSymbols& symbols)
{
    adoptDecimalFormatSymbols(new DecimalFormatSymbols(symbols));
    expandAffixes();
}
 
//------------------------------------------------------------------------------
// Gets the positive prefix of the number pattern.
 
UnicodeString&
DecimalFormat::getPositivePrefix(UnicodeString& result) const
{
    result = fPositivePrefix;
    return result;
}
 
//------------------------------------------------------------------------------
// Sets the positive prefix of the number pattern.
 
void
DecimalFormat::setPositivePrefix(const UnicodeString& newValue)
{
    fPositivePrefix = newValue;
    delete fPosPrefixPattern;
    fPosPrefixPattern = 0;
}

//------------------------------------------------------------------------------
// Gets the negative prefix  of the number pattern.

UnicodeString&
DecimalFormat::getNegativePrefix(UnicodeString& result) const
{
    result = fNegativePrefix;
    return result;
}

//------------------------------------------------------------------------------
// Gets the negative prefix  of the number pattern.

void
DecimalFormat::setNegativePrefix(const UnicodeString& newValue)
{
    fNegativePrefix = newValue;
    delete fNegPrefixPattern;
    fNegPrefixPattern = 0;
}

//------------------------------------------------------------------------------
// Gets the positive suffix of the number pattern.

UnicodeString&
DecimalFormat::getPositiveSuffix(UnicodeString& result) const
{
    result = fPositiveSuffix;
    return result;
}

//------------------------------------------------------------------------------
// Sets the positive suffix of the number pattern.

void
DecimalFormat::setPositiveSuffix(const UnicodeString& newValue)
{
    fPositiveSuffix = newValue;
    delete fPosSuffixPattern;
    fPosSuffixPattern = 0;
}

//------------------------------------------------------------------------------
// Gets the negative suffix of the number pattern.

UnicodeString&
DecimalFormat::getNegativeSuffix(UnicodeString& result) const
{
    result = fNegativeSuffix;
    return result;
}

//------------------------------------------------------------------------------
// Sets the negative suffix of the number pattern.

void
DecimalFormat::setNegativeSuffix(const UnicodeString& newValue)
{
    fNegativeSuffix = newValue;
    delete fNegSuffixPattern;
    fNegSuffixPattern = 0;
}

//------------------------------------------------------------------------------
// Gets the multiplier of the number pattern.

int32_t DecimalFormat::getMultiplier() const
{
    return fMultiplier;
}

//------------------------------------------------------------------------------
// Sets the multiplier of the number pattern.

void
DecimalFormat::setMultiplier(int32_t newValue)
{
    // We should really take a UErrorCode and disallow values <= 0 - liu
    fMultiplier = newValue;
}

/**
 * Get the rounding increment.
 * @return A positive rounding increment, or 0.0 if rounding
 * is not in effect.
 * @see #setRoundingIncrement
 * @see #getRoundingMode
 * @see #setRoundingMode
 */
double DecimalFormat::getRoundingIncrement() {
    return fRoundingDouble;
}

/**
 * Set the rounding increment.  This method also controls whether
 * rounding is enabled.
 * @param newValue A positive rounding increment, or 0.0 to disable rounding.
 * Negative increments are equivalent to 0.0.
 * @see #getRoundingIncrement
 * @see #getRoundingMode
 * @see #setRoundingMode
 */
void DecimalFormat::setRoundingIncrement(double newValue) {
    if (newValue > 0.0) {
        if (fRoundingIncrement == NULL) {
            fRoundingIncrement = new DigitList();
        }
        fRoundingIncrement->set((long)newValue);
        fRoundingDouble = newValue;
    } else {
        delete fRoundingIncrement;
        fRoundingIncrement = NULL;
        fRoundingDouble = 0.0;
    }
}

/**
 * Get the rounding mode.
 * @return A rounding mode
 * @see #setRoundingIncrement
 * @see #getRoundingIncrement
 * @see #setRoundingMode
 */
DecimalFormat::ERoundingMode DecimalFormat::getRoundingMode() {
    return fRoundingMode;
}

/**
 * Set the rounding mode.  This has no effect unless the rounding
 * increment is greater than zero.
 * @param roundingMode A rounding mode
 * @see #setRoundingIncrement
 * @see #getRoundingIncrement
 * @see #getRoundingMode
 */
void DecimalFormat::setRoundingMode(ERoundingMode roundingMode) {
    fRoundingMode = roundingMode;
}

/**
 * Get the width to which the output of <code>format()</code> is padded.
 * @return the format width, or zero if no padding is in effect
 * @see #setFormatWidth
 * @see #getPadCharacter
 * @see #setPadCharacter
 * @see #getPadPosition
 * @see #setPadPosition
 */
int32_t DecimalFormat::getFormatWidth() {
    return fFormatWidth;
}

/**
 * Set the width to which the output of <code>format()</code> is padded.
 * This method also controls whether padding is enabled.
 * @param width the width to which to pad the result of
 * <code>format()</code>, or zero to disable padding.  A negative
 * width is equivalent to 0.
 * @see #getFormatWidth
 * @see #getPadCharacter
 * @see #setPadCharacter
 * @see #getPadPosition
 * @see #setPadPosition
 */
void DecimalFormat::setFormatWidth(int32_t width) {
    fFormatWidth = (width > 0) ? width : 0;
}

/**
 * Get the character used to pad to the format width.  The default is ' '.
 * @return the pad character
 * @see #setFormatWidth
 * @see #getFormatWidth
 * @see #setPadCharacter
 * @see #getPadPosition
 * @see #setPadPosition
 */
UnicodeString DecimalFormat::getPadCharacterString() {
    return fPad;
}

/**
 * Set the character used to pad to the format width.  This has no effect
 * unless padding is enabled.
 * @param padChar the pad character
 * @see #setFormatWidth
 * @see #getFormatWidth
 * @see #getPadCharacter
 * @see #getPadPosition
 * @see #setPadPosition
 */
void DecimalFormat::setPadCharacter(UnicodeString padChar) {
    fPad = padChar.length() > 0 ? padChar.charAt(0) : kPatternPadEscape;
}

/**
 * Get the position at which padding will take place.  This is the location
 * at which padding will be inserted if the result of <code>format()</code>
 * is shorter than the format width.
 * @return the pad position, one of <code>kPadBeforePrefix</code>,
 * <code>kPadAfterPrefix</code>, <code>kPadBeforeSuffix</code>, or
 * <code>kPadAfterSuffix</code>.
 * @see #setFormatWidth
 * @see #getFormatWidth
 * @see #setPadCharacter
 * @see #getPadCharacter
 * @see #setPadPosition
 * @see #kPadBeforePrefix
 * @see #kPadAfterPrefix
 * @see #kPadBeforeSuffix
 * @see #kPadAfterSuffix
 */
DecimalFormat::EPadPosition DecimalFormat::getPadPosition() {
    return fPadPosition;
}

/**
 * <strong><font face=helvetica color=red>NEW</font></strong>
 * Set the position at which padding will take place.  This is the location
 * at which padding will be inserted if the result of <code>format()</code>
 * is shorter than the format width.  This has no effect unless padding is
 * enabled.
 * @param padPos the pad position, one of <code>kPadBeforePrefix</code>,
 * <code>kPadAfterPrefix</code>, <code>kPadBeforeSuffix</code>, or
 * <code>kPadAfterSuffix</code>.
 * @see #setFormatWidth
 * @see #getFormatWidth
 * @see #setPadCharacter
 * @see #getPadCharacter
 * @see #getPadPosition
 * @see #kPadBeforePrefix
 * @see #kPadAfterPrefix
 * @see #kPadBeforeSuffix
 * @see #kPadAfterSuffix
 */
void DecimalFormat::setPadPosition(EPadPosition padPos) {
    fPadPosition = padPos;
}

/**
 * Return whether or not scientific notation is used.
 * @return TRUE if this object formats and parses scientific notation
 * @see #setScientificNotation
 * @see #getMinimumExponentDigits
 * @see #setMinimumExponentDigits
 * @see #isExponentSignAlwaysShown
 * @see #setExponentSignAlwaysShown
 */
UBool DecimalFormat::isScientificNotation() {
    return fUseExponentialNotation;
}

/**
 * Set whether or not scientific notation is used.
 * @param useScientific TRUE if this object formats and parses scientific
 * notation
 * @see #isScientificNotation
 * @see #getMinimumExponentDigits
 * @see #setMinimumExponentDigits
 * @see #isExponentSignAlwaysShown
 * @see #setExponentSignAlwaysShown
 */
void DecimalFormat::setScientificNotation(UBool useScientific) {
    fUseExponentialNotation = useScientific;
    if (fUseExponentialNotation && fMinExponentDigits < 1) {
        fMinExponentDigits = 1;
    }
}

/**
 * Return the minimum exponent digits that will be shown.
 * @return the minimum exponent digits that will be shown
 * @see #setScientificNotation
 * @see #isScientificNotation
 * @see #setMinimumExponentDigits
 * @see #isExponentSignAlwaysShown
 * @see #setExponentSignAlwaysShown
 */
int8_t DecimalFormat::getMinimumExponentDigits() {
    return fMinExponentDigits;
}

/**
 * Set the minimum exponent digits that will be shown.  This has no
 * effect unless scientific notation is in use.
 * @param minExpDig a value >= 1 indicating the fewest exponent digits
 * that will be shown.  Values less than 1 will be treated as 1.
 * @see #setScientificNotation
 * @see #isScientificNotation
 * @see #getMinimumExponentDigits
 * @see #isExponentSignAlwaysShown
 * @see #setExponentSignAlwaysShown
 */
void DecimalFormat::setMinimumExponentDigits(int8_t minExpDig) {
    fMinExponentDigits = (int8_t)((minExpDig > 0) ? minExpDig : 1);
}

/**
 * Return whether the exponent sign is always shown.
 * @return TRUE if the exponent is always prefixed with either the
 * localized minus sign or the localized plus sign, false if only negative
 * exponents are prefixed with the localized minus sign.
 * @see #setScientificNotation
 * @see #isScientificNotation
 * @see #setMinimumExponentDigits
 * @see #getMinimumExponentDigits
 * @see #setExponentSignAlwaysShown
 */
UBool DecimalFormat::isExponentSignAlwaysShown() {
    return fExponentSignAlwaysShown;
}

/**
 * Set whether the exponent sign is always shown.  This has no effect
 * unless scientific notation is in use.
 * @param expSignAlways TRUE if the exponent is always prefixed with either
 * the localized minus sign or the localized plus sign, false if only
 * negative exponents are prefixed with the localized minus sign.
 * @see #setScientificNotation
 * @see #isScientificNotation
 * @see #setMinimumExponentDigits
 * @see #getMinimumExponentDigits
 * @see #isExponentSignAlwaysShown
 */
void DecimalFormat::setExponentSignAlwaysShown(UBool expSignAlways) {
    fExponentSignAlwaysShown = expSignAlways;
}

//------------------------------------------------------------------------------
// Gets the grouping size of the number pattern.  For example, thousand or 10
// thousand groupings.
 
int32_t
DecimalFormat::getGroupingSize() const
{
    return fGroupingSize;
}
 
//------------------------------------------------------------------------------
// Gets the grouping size of the number pattern.
 
void
DecimalFormat::setGroupingSize(int32_t newValue)
{
    fGroupingSize = newValue;
}

//------------------------------------------------------------------------------

int32_t
DecimalFormat::getSecondaryGroupingSize() const
{
    return fGroupingSize2;
}

//------------------------------------------------------------------------------

void
DecimalFormat::setSecondaryGroupingSize(int32_t newValue)
{
    fGroupingSize2 = newValue;
}

//------------------------------------------------------------------------------
// Checks if to show the decimal separator.

UBool
DecimalFormat::isDecimalSeparatorAlwaysShown() const
{
    return fDecimalSeparatorAlwaysShown;
}

//------------------------------------------------------------------------------
// Sets to always show the decimal separator.

void
DecimalFormat::setDecimalSeparatorAlwaysShown(UBool newValue)
{
    fDecimalSeparatorAlwaysShown = newValue;
}

//------------------------------------------------------------------------------
// Emits the pattern of this DecimalFormat instance.

UnicodeString&
DecimalFormat::toPattern(UnicodeString& result) const
{
    return toPattern(result, FALSE);
}

//------------------------------------------------------------------------------
// Emits the localized pattern this DecimalFormat instance.

UnicodeString&
DecimalFormat::toLocalizedPattern(UnicodeString& result) const
{
    return toPattern(result, TRUE);
}

//------------------------------------------------------------------------------
/**
 * Expand the affix pattern strings into the expanded affix strings.  If any
 * affix pattern string is null, do not expand it.  This method should be
 * called any time the symbols or the affix patterns change in order to keep
 * the expanded affix strings up to date.
 */
void DecimalFormat::expandAffixes(void) {
    if (fPosPrefixPattern != 0) {
        expandAffix(*fPosPrefixPattern, fPositivePrefix);
    }
    if (fPosSuffixPattern != 0) {
        expandAffix(*fPosSuffixPattern, fPositiveSuffix);
    }
    if (fNegPrefixPattern != 0) {
        expandAffix(*fNegPrefixPattern, fNegativePrefix);
    }
    if (fNegSuffixPattern != 0) {
        expandAffix(*fNegSuffixPattern, fNegativeSuffix);
    }
#ifdef DEBUG
    UnicodeString s;
    s.append("[")
        .append(*fPosPrefixPattern).append("|").append(*fPosSuffixPattern)
        .append(";") .append(*fNegPrefixPattern).append("|").append(*fNegSuffixPattern)
        .append("]->[")
        .append(fPositivePrefix).append("|").append(fPositiveSuffix)
        .append(";") .append(fNegativePrefix).append("|").append(fNegativeSuffix)
        .append("]\n");
    debugout(s);
#endif
}

/**
 * Expand an affix pattern into an affix string.  All characters in the
 * pattern are literal unless prefixed by kQuote.  The following characters
 * after kQuote are recognized: PATTERN_PERCENT, PATTERN_PER_MILLE,
 * PATTERN_MINUS, and kCurrencySign.  If kCurrencySign is doubled (kQuote +
 * kCurrencySign + kCurrencySign), it is interpreted as an international
 * currency sign.  Any other character after a kQuote represents itself.
 * kQuote must be followed by another character; kQuote may not occur by
 * itself at the end of the pattern.
 *
 * @param pattern the non-null, fPossibly empty pattern
 * @param affix string to receive the expanded equivalent of pattern
 */
void DecimalFormat::expandAffix(const UnicodeString& pattern,
                                UnicodeString& affix) const {
    affix.remove();
    for (int i=0; i<pattern.length(); ) {
        UChar c = pattern.charAt(i++);
        if (c == kQuote) {
            c = pattern.charAt(i++);
            switch (c) {
            case kCurrencySign:
                {
                    UnicodeString s;
                    if (i<pattern.length() &&
                        pattern.charAt(i) == kCurrencySign) {
                        ++i;
                        affix += fSymbols->getInternationalCurrencySymbol(s);
                    } else {
                        affix += fSymbols->getCurrencySymbol(s);
                    }
                }
                continue;
            case kPatternPercent:
                c = fSymbols->getPercent();
                break;
            case kPatternPerMill:
                c = fSymbols->getPerMill();
                break;
            case kPatternPlus:
                c = fSymbols->getPlusSign();
                break;
            case kPatternMinus:
                c = fSymbols->getMinusSign();
                break;
            }
        }
        affix.append(c);
    }
}

/**
 * Appends an affix pattern to the given StringBuffer, quoting special
 * characters as needed.  Uses the internal affix pattern, if that exists,
 * or the literal affix, if the internal affix pattern is null.  The
 * appended string will generate the same affix pattern (or literal affix)
 * when passed to toPattern().
 *
 * @param buffer the affix string is appended to this
 * @param affixPattern a pattern such as fPosPrefixPattern; may be null
 * @param expAffix a corresponding expanded affix, such as fPositivePrefix.
 * Ignored unless affixPattern is null.  If affixPattern is null, then
 * expAffix is appended as a literal affix.
 * @param localized true if the appended pattern should contain localized
 * pattern characters; otherwise, non-localized pattern chars are appended
 */
void DecimalFormat::appendAffix(UnicodeString& buffer,
                                const UnicodeString* affixPattern,
                                const UnicodeString& expAffix,
                                UBool localized) const {
    if (affixPattern == 0) {
        appendAffix(buffer, expAffix, localized);
    } else {
        int i;
        for (int pos=0; pos<affixPattern->length(); pos=i) {
            i = affixPattern->indexOf(kQuote, pos);
            if (i < 0) {
                UnicodeString s;
                affixPattern->extractBetween(pos, affixPattern->length(), s);
                appendAffix(buffer, s, localized);
                break;
            }
            if (i > pos) {
                UnicodeString s;
                affixPattern->extractBetween(pos, i, s);
                appendAffix(buffer, s, localized);
            }
            UChar c = affixPattern->charAt(++i);
            ++i;
            if (c == kQuote) {
                buffer.append(c);
                // Fall through and append another kQuote below
            } else if (c == kCurrencySign &&
                       i<affixPattern->length() &&
                       affixPattern->charAt(i) == kCurrencySign) {
                ++i;
                buffer.append(c);
                // Fall through and append another kCurrencySign below
            } else if (localized) {
                switch (c) {
                case kPatternPercent:
                    c = fSymbols->getPercent();
                    break;
                case kPatternPerMill:
                    c = fSymbols->getPerMill();
                    break;
                case kPatternPlus:
                    c = fSymbols->getPlusSign();
                    break;
                case kPatternMinus:
                    c = fSymbols->getMinusSign();
                    break;
                }
            }
            buffer.append(c);
        }
    }
}

/**
 * Append an affix to the given StringBuffer, using quotes if
 * there are special characters.  Single quotes themselves must be
 * escaped in either case.
 */
void
DecimalFormat::appendAffix(    UnicodeString& buffer,
                            const UnicodeString& affix,
                            UBool localized) const {
    UBool needQuote;
    if(localized) {
        needQuote = affix.indexOf(fSymbols->getZeroDigit()) >= 0
            || affix.indexOf(fSymbols->getGroupingSeparator()) >= 0
            || affix.indexOf(fSymbols->getDecimalSeparator()) >= 0
            || affix.indexOf(fSymbols->getPercent()) >= 0
            || affix.indexOf(fSymbols->getPerMill()) >= 0
            || affix.indexOf(fSymbols->getDigit()) >= 0
            || affix.indexOf(fSymbols->getPatternSeparator()) >= 0
            || affix.indexOf(fSymbols->getPlusSign()) >= 0
            || affix.indexOf(fSymbols->getMinusSign()) >= 0
            || affix.indexOf(kCurrencySign) >= 0;
    }
    else {
        needQuote = affix.indexOf(kPatternZeroDigit) >= 0
            || affix.indexOf(kPatternGroupingSeparator) >= 0
            || affix.indexOf(kPatternDecimalSeparator) >= 0
            || affix.indexOf(kPatternPercent) >= 0
            || affix.indexOf(kPatternPerMill) >= 0
            || affix.indexOf(kPatternDigit) >= 0
            || affix.indexOf(kPatternSeparator) >= 0
            || affix.indexOf(kPatternExponent) >= 0
            || affix.indexOf(kPatternPlus) >= 0
            || affix.indexOf(kPatternMinus) >= 0
            || affix.indexOf(kCurrencySign) >= 0;
    }
    if (needQuote)
        buffer += (UChar)0x0027 /*'\''*/;
    if (affix.indexOf((UChar)0x0027 /*'\''*/) < 0)
        buffer += affix;
    else {
        for (int32_t j = 0; j < affix.length(); ++j) {
            UChar c = affix[j];
            buffer += c;
            if (c == 0x0027 /*'\''*/) buffer += c;
        }
    }
    if (needQuote) buffer += (UChar)0x0027 /*'\''*/;
}

//------------------------------------------------------------------------------

/* Tell the VC++ compiler not to spew out the warnings about integral size conversion */
#ifdef _WIN32
#pragma warning( disable : 4761 )
#endif

UnicodeString&
DecimalFormat::toPattern(UnicodeString& result, UBool localized) const
{
    result.remove();
    UChar zero = localized ? fSymbols->getZeroDigit() : kPatternZeroDigit;
    UChar digit = localized ? fSymbols->getDigit() : kPatternDigit;
    UChar group = localized ? fSymbols->getGroupingSeparator()
                           : kPatternGroupingSeparator;
    int32_t i;
    int32_t roundingDecimalPos = 0; // Pos of decimal in roundingDigits
    UnicodeString roundingDigits;
    int32_t padPos = (fFormatWidth > 0) ? fPadPosition : -1;
    UnicodeString padSpec;
    if(fFormatWidth > 0) {
      padSpec.append((UChar)(localized ? fSymbols->getPadEscape() : kPatternPadEscape)).
        append(fPad);
    }
    if(fRoundingIncrement != NULL) {
      for(i=0; i<fRoundingIncrement->fCount; ++i) {
        roundingDigits.append((UChar)fRoundingIncrement->fDigits[i]);
      }
      roundingDecimalPos = fRoundingIncrement->fDecimalAt;
    }
    for (int32_t part=0; part<2; ++part) {
        if (padPos == kPadBeforePrefix) {
            result.append(padSpec);
        }
        appendAffix(result,
                    (part==0 ? fPosPrefixPattern : fNegPrefixPattern),
                    (part==0 ? fPositivePrefix : fNegativePrefix),
                    localized);
        if (padPos == kPadAfterPrefix && ! padSpec.empty()) {
            result.append(padSpec);
        }
        int32_t sub0Start = result.length();
        int32_t g = isGroupingUsed() ? uprv_max(0, fGroupingSize) : 0;
        if (g > 0 && fGroupingSize2 > 0 && fGroupingSize2 != fGroupingSize) {
            g += fGroupingSize2;
        }
        int32_t maxIntDig = fUseExponentialNotation ? getMaximumIntegerDigits() :
          (uprv_max(uprv_max(g, getMinimumIntegerDigits()),
                   roundingDecimalPos) + 1);
        for (i = maxIntDig; i > 0; --i) {
            if (!fUseExponentialNotation && i<maxIntDig &&
                isGroupingPosition(i)) {
                result.append(group);
            }
            if (! roundingDigits.empty()) {
                int32_t pos = roundingDecimalPos - i;
                if (pos >= 0 && pos < roundingDigits.length()) {
                    result.append((UChar) (roundingDigits.charAt(pos) - kPatternZeroDigit + zero));
                    continue;
                }
            }
            result.append((UChar)(i<=getMinimumIntegerDigits() ? zero : digit));
        }
        if (getMaximumFractionDigits() > 0 || fDecimalSeparatorAlwaysShown) {
            result.append((UChar)(localized ? fSymbols->getDecimalSeparator() :
                kPatternDecimalSeparator));
        }
        int32_t pos = roundingDecimalPos;
        for (i = 0; i < getMaximumFractionDigits(); ++i) {
            if (! roundingDigits.empty() && pos < roundingDigits.length()) {
                result.append((UChar)(pos < 0 ? zero :
                  (UChar) (roundingDigits.charAt(pos) - kPatternZeroDigit + zero)));
                ++pos;
                continue;
            }
            result.append((UChar)(i<getMinimumFractionDigits() ? zero : digit));
        }
        if (fUseExponentialNotation) {
            result.append((UChar)(localized ? fSymbols->getExponentialSymbol() :
                          kPatternExponent));
            if (fExponentSignAlwaysShown) {
                result.append((UChar)(localized ? fSymbols->getPlusSign() :
                              kPatternPlus));
            }
            for (i=0; i<fMinExponentDigits; ++i) {
                result.append(zero);
            }
        }
        if (! padSpec.empty() && !fUseExponentialNotation) {
            int32_t add = fFormatWidth - result.length() + sub0Start
                - ((part == 0)
                   ? fPositivePrefix.length() + fPositiveSuffix.length()
                   : fNegativePrefix.length() + fNegativeSuffix.length());
            while (add > 0) {
                result.insert(sub0Start, digit);
                ++maxIntDig;
                --add;
                // Only add a grouping separator if we have at least
                // 2 additional characters to be added, so we don't
                // end up with ",###".
                if (add>1 && isGroupingPosition(maxIntDig)) {
                    result.insert(sub0Start, group);
                    --add;                        
                }
            }
        }
        if (fPadPosition == kPadBeforeSuffix && ! padSpec.empty()) {
            result.append(padSpec);
        }
        if (part == 0) {
            appendAffix(result, fPosSuffixPattern, fPositiveSuffix, localized);
            if (fPadPosition == kPadAfterSuffix && ! padSpec.empty()) {
                result.append(padSpec);
            }
            UBool isDefault = FALSE;
            if ((fNegSuffixPattern == fPosSuffixPattern && // both null
                 fNegativeSuffix == fPositiveSuffix)
                || (fNegSuffixPattern != 0 && fPosSuffixPattern != 0 &&
                    *fNegSuffixPattern == *fPosSuffixPattern)) {
                if (fNegPrefixPattern != NULL && fPosPrefixPattern != NULL) {
                    int32_t length = fPosPrefixPattern->length();
                    isDefault = fNegPrefixPattern->length() == (length+2) &&
                        (*fNegPrefixPattern)[(UTextOffset)0] == kQuote &&
                        (*fNegPrefixPattern)[(UTextOffset)1] == kPatternMinus &&
                        fNegPrefixPattern->compare(2, length, *fPosPrefixPattern, 0, length) == 0;
                }
                if (!isDefault &&
                    fNegPrefixPattern == NULL && fPosPrefixPattern == NULL) {
                    int32_t length = fPositivePrefix.length();
                    isDefault = fNegativePrefix.length() == (length+1) &&
                        fNegativePrefix[(UTextOffset)0] == fSymbols->getMinusSign() &&
                        fNegativePrefix.compare(1, length, fPositivePrefix, 0, length) == 0;
                }
            }
            if (isDefault) {
                break; // Don't output default negative subpattern
            } else {
                result.append((UChar)(localized ? fSymbols->getPatternSeparator() :
                              kPatternSeparator));
            }
        } else {
            appendAffix(result, fNegSuffixPattern, fNegativeSuffix, localized);
            if (fPadPosition == kPadAfterSuffix && ! padSpec.empty()) {
                result.append(padSpec);
            }
        }
    }

    return result;
}

//------------------------------------------------------------------------------

void
DecimalFormat::applyPattern(const UnicodeString& pattern, UErrorCode& status)
{
    applyPattern(pattern, FALSE, status);
}

//------------------------------------------------------------------------------

void
DecimalFormat::applyLocalizedPattern(const UnicodeString& pattern, UErrorCode& status)
{
    applyPattern(pattern, TRUE, status);
}

//------------------------------------------------------------------------------

void
DecimalFormat::applyPattern(const UnicodeString& pattern,
                            UBool localized,
                            UErrorCode& status)
{
    if (U_FAILURE(status)) return;
    // Set the significant pattern symbols
    UChar zeroDigit         = kPatternZeroDigit;
    UChar groupingSeparator = kPatternGroupingSeparator;
    UChar decimalSeparator  = kPatternDecimalSeparator;
    UChar percent           = kPatternPercent;
    UChar perMill           = kPatternPerMill;
    UChar digit             = kPatternDigit;
    UChar separator         = kPatternSeparator;
    UChar exponent          = kPatternExponent;
    UChar plus              = kPatternPlus;
    UChar minus             = kPatternMinus;
    UChar padEscape         = kPatternPadEscape;
    // Substitute with the localized symbols if necessary
    if (localized) {
        zeroDigit         = fSymbols->getZeroDigit();
        groupingSeparator = fSymbols->getGroupingSeparator();
        decimalSeparator  = fSymbols->getDecimalSeparator();
        percent           = fSymbols->getPercent();
        perMill           = fSymbols->getPerMill();
        digit             = fSymbols->getDigit();
        separator         = fSymbols->getPatternSeparator();
        exponent          = fSymbols->getExponentialSymbol();
        plus              = fSymbols->getPlusSign();
        minus             = fSymbols->getMinusSign();
        padEscape         = fSymbols->getPadEscape();
    }
    UChar nineDigit = (UChar)(zeroDigit + 9);

    int32_t pos = 0;
    // Part 0 is the positive pattern.  Part 1, if present, is the negative
    // pattern.
    for (int32_t part=0; part<2 && pos<pattern.length(); ++part) {
        // The subpart ranges from 0 to 4: 0=pattern proper, 1=prefix,
        // 2=suffix, 3=prefix in quote, 4=suffix in quote.  Subpart 0 is
        // between the prefix and suffix, and consists of pattern
        // characters.  In the prefix and suffix, percent, perMill, and
        // currency symbols are recognized and translated.
        int32_t subpart = 1, sub0Start = 0, sub0Limit = 0, sub2Limit = 0;

        // It's important that we don't change any fields of this object
        // prematurely.  We set the following variables for the multiplier,
        // grouping, etc., and then only change the actual object fields if
        // everything parses correctly.  This also lets us register
        // the data from part 0 and ignore the part 1, except for the
        // prefix and suffix.
        UnicodeString prefix;
        UnicodeString suffix;
        int32_t decimalPos = -1;
        int32_t multiplier = 1;
        int32_t digitLeftCount = 0, zeroDigitCount = 0, digitRightCount = 0;
        int8_t groupingCount = -1;
        int8_t groupingCount2 = -1;
        int32_t padPos = -1;
        UChar padChar = 0;
        int32_t roundingPos = -1;
        DigitList roundingInc;
        int8_t expDigits = -1;
        UBool expSignAlways = FALSE;
        UBool isCurrency = FALSE;
        
        // The affix is either the prefix or the suffix.
        UnicodeString* affix = &prefix;
        
        int32_t start = pos;
        UBool isPartDone = FALSE;

        for (; !isPartDone && pos < pattern.length(); ++pos) {
            UChar ch = pattern[(UTextOffset) pos];
            switch (subpart) {
            case 0: // Pattern proper subpart (between prefix & suffix)
                // Process the digits, decimal, and grouping characters.  We
                // record five pieces of information.  We expect the digits
                // to occur in the pattern ####00.00####, and we record the
                // number of left digits, zero (central) digits, and right
                // digits.  The position of the last grouping character is
                // recorded (should be somewhere within the first two blocks
                // of characters), as is the position of the decimal point,
                // if any (should be in the zero digits).  If there is no
                // decimal point, then there should be no right digits.
                if (ch == digit) {
                    if (zeroDigitCount > 0) {
                        ++digitRightCount;
                    } else {
                        ++digitLeftCount;
                    }
                    if (groupingCount >= 0 && decimalPos < 0) {
                        ++groupingCount;
                    }
                } else if (ch >= zeroDigit && ch <= nineDigit) {
                    if (digitRightCount > 0) {
                        // Unexpected '0'
                        debug("Unexpected '0'")
                        status = U_ILLEGAL_ARGUMENT_ERROR;
                        return;
                    }
                    ++zeroDigitCount;
                    if (groupingCount >= 0 && decimalPos < 0) {
                        ++groupingCount;
                    }
                    if (ch != zeroDigit && roundingPos < 0) {
                        roundingPos = digitLeftCount + zeroDigitCount;
                    }
                    if (roundingPos >= 0) {
                        roundingInc.append((char)(ch - zeroDigit + '0'));
                    }
                } else if (ch == groupingSeparator) {
                    if (decimalPos >= 0) {
                        // Grouping separator after decimal
                        debug("Grouping separator after decimal")
                        status = U_ILLEGAL_ARGUMENT_ERROR;
                        return;
                    }
                    groupingCount2 = groupingCount;
                    groupingCount = 0;
                } else if (ch == decimalSeparator) {
                    if (decimalPos >= 0) {
                        // Multiple decimal separators
                        debug("Multiple decimal separators")
                        status = U_ILLEGAL_ARGUMENT_ERROR;
                        return;
                    }
                    // Intentionally incorporate the digitRightCount,
                    // even though it is illegal for this to be > 0
                    // at this point.  We check pattern syntax below.
                    decimalPos = digitLeftCount + zeroDigitCount + digitRightCount;
                } else {
                    if (ch == exponent) {
                        if (expDigits >= 0) {
                            // Multiple exponential symbols
                            debug("Multiple exponential symbols")
                            status = U_ILLEGAL_ARGUMENT_ERROR;
                            return;
                        }
                        if (groupingCount >= 0) {
                            // Grouping separator in exponential pattern
                            debug("Grouping separator in exponential pattern")
                            status = U_ILLEGAL_ARGUMENT_ERROR;
                            return;
                        }
                        // Check for positive prefix
                        if ((pos+1) < pattern.length()
                            && pattern[(UTextOffset) (pos+1)] == plus) {
                            expSignAlways = TRUE;
                            ++pos;
                        }
                        // Use lookahead to parse out the exponential part of the
                        // pattern, then jump into suffix subpart.
                        expDigits = 0;
                        while (++pos < pattern.length() &&
                               pattern[(UTextOffset) pos] == zeroDigit) {
                            ++expDigits;
                        }

                        if ((digitLeftCount + zeroDigitCount) < 1 ||
                            expDigits < 1) {
                            // Malformed exponential pattern
                            debug("Malformed exponential pattern")
                            status = U_ILLEGAL_ARGUMENT_ERROR;
                            return;
                        }
                    }
                    // Transition to suffix subpart
                    subpart = 2; // suffix subpart
                    affix = &suffix;
                    sub0Limit = pos--;
                    continue;
                }
                break;
            case 1: // Prefix subpart
            case 2: // Suffix subpart
                // Process the prefix / suffix characters
                // Process unquoted characters seen in prefix or suffix
                // subpart.
                if (ch == digit ||
                    ch == groupingSeparator ||
                    ch == decimalSeparator ||
                    (ch >= zeroDigit && ch <= nineDigit)) {
                    // Any of these characters implicitly begins the
                    // next subpart if we are in the prefix
                    if (subpart == 1) { // prefix subpart
                        subpart = 0; // pattern proper subpart
                        sub0Start = pos--; // Reprocess this character
                        continue;
                    }
                    // Fall through to append(ch)
                } else if (ch == kCurrencySign) {
                    // Use lookahead to determine if the currency sign is
                    // doubled or not.
                    UBool doubled = (pos + 1) < pattern.length() &&
                        pattern[(UTextOffset) (pos+1)] == kCurrencySign;
                    affix->append(kQuote); // Encode currency
                    if (doubled) {
                        affix->append(kCurrencySign);
                        ++pos; // Skip over the doubled character
                    }
                    isCurrency = TRUE;
                    // Fall through to append(ch)
                } else if (ch == kQuote) {
                    // A quote outside quotes indicates either the opening
                    // quote or two quotes, which is a quote literal.  That is,
                    // we have the first quote in 'do' or o''clock.
                    if ((pos+1) < pattern.length() &&
                        pattern[(UTextOffset) (pos+1)] == kQuote) {
                        ++pos;
                        affix->append(kQuote); // Encode quote
                        // Fall through to append(ch)
                    } else {
                        subpart += 2; // open quote
                        continue;
                    }
                } else if (ch == separator) {
                    // Don't allow separators in the prefix, and don't allow
                    // separators in the second pattern (part == 1).
                    if (subpart == 1 || part == 1) {
                        // Unexpected separator
                        debug("Unexpected separator")
                        status = U_ILLEGAL_ARGUMENT_ERROR;
                        return;
                    }
                    sub2Limit = pos;
                    isPartDone = TRUE; // Go to next part
                    break;
                } else if (ch == percent || ch == perMill) {
                    // Next handle characters which are appended directly.
                    if (multiplier != 1) {
                        // Too many percent/perMill characters
                        debug("Too many percent/perMill characters")
                        status = U_ILLEGAL_ARGUMENT_ERROR;
                        return;
                    }
                    affix->append(kQuote); // Encode percent/perMill
                    if (ch == percent) {
                        multiplier = 100;
                        ch = kPatternPercent; // Use unlocalized pattern char
                    } else {
                        multiplier = 1000;
                        ch = kPatternPerMill; // Use unlocalized pattern char
                    }
                    // Fall through to append(ch)
                } else if (ch == padEscape) {
                    if (padPos >= 0 ||               // Multiple pad specifiers
                        (pos+1) == pattern.length()) { // Nothing after padEscape
                        debug("Multiple pad specifiers")
                        status = U_ILLEGAL_ARGUMENT_ERROR;
                        return;
                    }
                    padPos = pos;
                    padChar = pattern[(UTextOffset) ++pos];
                    continue;
                } else if (ch == minus) {
                    affix->append(kQuote); // Encode minus
                    ch = kPatternMinus;
                    // Fall through to append(ch)
                } else if (ch == plus) {
                    affix->append(kQuote); // Encode plus
                    ch = kPatternPlus;
                    // Fall through to append(ch)
                }
                // Unquoted, non-special characters fall through to here, as
                // well as other code which needs to append something to the
                // affix.
                affix->append(ch);
                break;
            case 3: // Prefix subpart, in quote
            case 4: // Suffix subpart, in quote
                // A quote within quotes indicates either the closing
                // quote or two quotes, which is a quote literal.  That is,
                // we have the second quote in 'do' or 'don''t'.
                if (ch == kQuote) {
                    if ((pos+1) < pattern.length() &&
                        pattern[(UTextOffset) (pos+1)] == kQuote) {
                        ++pos;
                        affix->append(kQuote); // Encode quote
                        // Fall through to append(ch)
                    } else {
                        subpart -= 2; // close quote
                        continue;
                    }
                }
                affix->append(ch);
                break;
            }
        }

        if (sub0Limit == 0) {
            sub0Limit = pattern.length();
        }

        if (sub2Limit == 0) {
            sub2Limit = pattern.length();
        }

        /* Handle patterns with no '0' pattern character.  These patterns
         * are legal, but must be recodified to make sense.  "##.###" ->
         * "#0.###".  ".###" -> ".0##".
         *
         * We allow patterns of the form "####" to produce a zeroDigitCount
         * of zero (got that?); although this seems like it might make it
         * possible for format() to produce empty strings, format() checks
         * for this condition and outputs a zero digit in this situation.
         * Having a zeroDigitCount of zero yields a minimum integer digits
         * of zero, which allows proper round-trip patterns.  We don't want
         * "#" to become "#0" when toPattern() is called (even though that's
         * what it really is, semantically).
         */
        if (zeroDigitCount == 0 && digitLeftCount > 0 && decimalPos >= 0) {
            // Handle "###.###" and "###." and ".###"
            int n = decimalPos;
            if (n == 0) ++n; // Handle ".###"
            digitRightCount = digitLeftCount - n;
            digitLeftCount = n - 1;
            zeroDigitCount = 1;
        }

        // Do syntax checking on the digits, decimal points, and quotes.
        if ((decimalPos < 0 && digitRightCount > 0) ||
            (decimalPos >= 0 &&
             (decimalPos < digitLeftCount ||
              decimalPos > (digitLeftCount + zeroDigitCount))) ||
            groupingCount == 0 || groupingCount2 == 0 ||
            subpart > 2) { // subpart > 2 == unmatched quote
            debug("Syntax error")
            status = U_ILLEGAL_ARGUMENT_ERROR;
            return;
        }

        // Make sure pad is at legal position before or after affix.
        if (padPos >= 0) {
            if (padPos == start) {
                padPos = kPadBeforePrefix;
            } else if (padPos+2 == sub0Start) {
                padPos = kPadAfterPrefix;
            } else if (padPos == sub0Limit) {
                padPos = kPadBeforeSuffix;
            } else if (padPos+2 == sub2Limit) {
                padPos = kPadAfterSuffix;
            } else {
                // Illegal pad position
                debug("Illegal pad position")
                status = U_ILLEGAL_ARGUMENT_ERROR;
                return;
            }
        }

        if (part == 0) {
            delete fPosPrefixPattern;
            delete fPosSuffixPattern;
            delete fNegPrefixPattern;
            delete fNegSuffixPattern;
            fPosPrefixPattern = new UnicodeString(prefix);
            fPosSuffixPattern = new UnicodeString(suffix);
            fNegPrefixPattern = 0;
            fNegSuffixPattern = 0;

            fUseExponentialNotation = (expDigits >= 0);
            if (fUseExponentialNotation) {
                fMinExponentDigits = expDigits;
                fExponentSignAlwaysShown = expSignAlways;
            }
            fIsCurrencyFormat = isCurrency;
            int digitTotalCount = digitLeftCount + zeroDigitCount + digitRightCount;
            // The effectiveDecimalPos is the position the decimal is at or
            // would be at if there is no decimal.  Note that if
            // decimalPos<0, then digitTotalCount == digitLeftCount +
            // zeroDigitCount.
            int effectiveDecimalPos = decimalPos >= 0 ? decimalPos : digitTotalCount;
            setMinimumIntegerDigits(effectiveDecimalPos - digitLeftCount);
            setMaximumIntegerDigits(fUseExponentialNotation
                    ? digitLeftCount + getMinimumIntegerDigits()
                    : kDoubleIntegerDigits);
            setMaximumFractionDigits(decimalPos >= 0
                    ? (digitTotalCount - decimalPos) : 0);
            setMinimumFractionDigits(decimalPos >= 0
                    ? (digitLeftCount + zeroDigitCount - decimalPos) : 0);
            setGroupingUsed(groupingCount > 0);
            fGroupingSize = (groupingCount > 0) ? groupingCount : 0;
            fGroupingSize2 = (groupingCount2 > 0 && groupingCount2 != groupingCount)
                ? groupingCount2 : 0;
            fMultiplier = multiplier;
            setDecimalSeparatorAlwaysShown(decimalPos == 0
                    || decimalPos == digitTotalCount);
            if (padPos >= 0) {
                fPadPosition = (EPadPosition) padPos;
                // To compute the format width, first set up sub0Limit -
                // sub0Start.  Add in prefix/suffix length later.

                // fFormatWidth = prefix.length() + suffix.length() +
                //    sub0Limit - sub0Start;
                fFormatWidth = sub0Limit - sub0Start;
                fPad = padChar;
            } else {
                fFormatWidth = 0;
            }
            if (roundingPos >= 0) {
                roundingInc.fDecimalAt = effectiveDecimalPos - roundingPos;
                if (fRoundingIncrement != NULL) {
                    *fRoundingIncrement = roundingInc;
                } else {
                    fRoundingIncrement = new DigitList(roundingInc);
                }
                fRoundingDouble = fRoundingIncrement->getDouble();
                fRoundingMode = kRoundHalfEven;
            } else {
                setRoundingIncrement(0.0);
            }
        } else {
            fNegPrefixPattern = new UnicodeString(prefix);
            fNegSuffixPattern = new UnicodeString(suffix);
        }
    }

    if (pattern.length() == 0) {
        delete fNegPrefixPattern;
        delete fNegSuffixPattern;
        fNegPrefixPattern = NULL;
        fNegSuffixPattern = NULL;
        if (fPosPrefixPattern != NULL) {
            fPosPrefixPattern->remove();
        } else {
            fPosPrefixPattern = new UnicodeString();
        }
        if (fPosSuffixPattern != NULL) {
            fPosSuffixPattern->remove();
        } else {
            fPosSuffixPattern = new UnicodeString();
        }

        setMinimumIntegerDigits(0);
        setMaximumIntegerDigits(kDoubleIntegerDigits);
        setMinimumFractionDigits(0);
        setMaximumFractionDigits(kDoubleFractionDigits);

        fUseExponentialNotation = FALSE;
        fIsCurrencyFormat = FALSE;
        setGroupingUsed(FALSE);
        fGroupingSize = 0;
        fGroupingSize2 = 0;
        fMultiplier = 1;
        setDecimalSeparatorAlwaysShown(FALSE);
        fFormatWidth = 0;
        setRoundingIncrement(0.0);
    }

    // If there was no negative pattern, or if the negative pattern is
    // identical to the positive pattern, then prepend the minus sign to the
    // positive pattern to form the negative pattern.
    if (fNegPrefixPattern == NULL ||
        (*fNegPrefixPattern == *fPosPrefixPattern
         && *fNegSuffixPattern == *fPosSuffixPattern)) {
        _copy_us_ptr(&fNegSuffixPattern, fPosSuffixPattern);
        if (fNegPrefixPattern == NULL) {
            fNegPrefixPattern = new UnicodeString();
        } else {
            fNegPrefixPattern->remove();
        }
        fNegPrefixPattern->append(kQuote).append(kPatternMinus)
            .append(*fPosPrefixPattern);
    }
#ifdef DEBUG
    UnicodeString s;
    s.append("\"").append(pattern).append("\"->");
    debugout(s);
#endif
    expandAffixes();
    if (fFormatWidth > 0) {
        // Finish computing format width (see above)
        fFormatWidth += fPositivePrefix.length() + fPositiveSuffix.length();
    }
}

/**
 * Sets the maximum number of digits allowed in the integer portion of a
 * number. This override limits the integer digit count to 309.
 * @see NumberFormat#setMaximumIntegerDigits
 */
void DecimalFormat::setMaximumIntegerDigits(int32_t newValue) {
    NumberFormat::setMaximumIntegerDigits(uprv_min(newValue, kDoubleIntegerDigits));
}

/**
 * Sets the minimum number of digits allowed in the integer portion of a
 * number. This override limits the integer digit count to 309.
 * @see NumberFormat#setMinimumIntegerDigits
 */
void DecimalFormat::setMinimumIntegerDigits(int32_t newValue) {
    NumberFormat::setMinimumIntegerDigits(uprv_min(newValue, kDoubleIntegerDigits));
}

/**
 * Sets the maximum number of digits allowed in the fraction portion of a
 * number. This override limits the fraction digit count to 340.
 * @see NumberFormat#setMaximumFractionDigits
 */
void DecimalFormat::setMaximumFractionDigits(int32_t newValue) {
    NumberFormat::setMaximumFractionDigits(uprv_min(newValue, kDoubleFractionDigits));
}

/**
 * Sets the minimum number of digits allowed in the fraction portion of a
 * number. This override limits the fraction digit count to 340.
 * @see NumberFormat#setMinimumFractionDigits
 */
void DecimalFormat::setMinimumFractionDigits(int32_t newValue) {
    NumberFormat::setMinimumFractionDigits(uprv_min(newValue, kDoubleFractionDigits));
}

//eof
