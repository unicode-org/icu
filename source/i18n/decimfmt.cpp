/*
*******************************************************************************
* Copyright (C) 1997-2003, International Business Machines Corporation and    *
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
 
#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/decimfmt.h"
#include "unicode/choicfmt.h"
#include "unicode/ucurr.h"
#include "unicode/ustring.h"
#include "unicode/dcfmtsym.h"
#include "unicode/resbund.h"
#include "unicode/uchar.h"
#include "uprops.h"
#include "digitlst.h"
#include "cmemory.h"
#include "cstring.h"
#include "umutex.h"
#include "uassert.h"

U_NAMESPACE_BEGIN

//#define FMT_DEBUG

#ifdef FMT_DEBUG
#include <stdio.h>
static void debugout(UnicodeString s) {
    char buf[2000];
    s.extract((int32_t) 0, s.length(), buf);
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

const char DecimalFormat::fgClassID = 0; // Value is irrelevant

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

//const int8_t DecimalFormat::fgMaxDigit                  = 9;

const int32_t DecimalFormat::kDoubleIntegerDigits  = 309;
const int32_t DecimalFormat::kDoubleFractionDigits = 340;

/**
 * These are the tags we expect to see in normal resource bundle files associated
 * with a locale.
 */
const char DecimalFormat::fgNumberPatterns[]="NumberPatterns";

static const UChar kDefaultPad = 0x0020; /* */

//------------------------------------------------------------------------------
// Constructs a DecimalFormat instance in the default locale.
 
DecimalFormat::DecimalFormat(UErrorCode& status)
: NumberFormat(), 
  fPosPrefixPattern(0), 
  fPosSuffixPattern(0), 
  fNegPrefixPattern(0), 
  fNegSuffixPattern(0),
  fCurrencyChoice(0),
  fMultiplier(0),
  fGroupingSize(0),
  fGroupingSize2(0),
  fSymbols(0),
  fMinExponentDigits(0),
  fRoundingIncrement(0),
  fPad(0),
  fFormatWidth(0)
{
    UParseError parseError;
    construct(status, parseError);
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
  fCurrencyChoice(0),
  fMultiplier(0),
  fGroupingSize(0),
  fGroupingSize2(0),
  fSymbols(0),
  fMinExponentDigits(0),
  fRoundingIncrement(0),
  fPad(0),
  fFormatWidth(0)
{
    UParseError parseError;
    construct(status, parseError, &pattern);
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
  fCurrencyChoice(0),
  fMultiplier(0),
  fGroupingSize(0),
  fGroupingSize2(0),
  fSymbols(0),
  fMinExponentDigits(0),
  fRoundingIncrement(0),
  fPad(0),
  fFormatWidth(0)
{
    UParseError parseError;
    if (symbolsToAdopt == NULL)
        status = U_ILLEGAL_ARGUMENT_ERROR;
    construct(status, parseError, &pattern, symbolsToAdopt);
}
 
DecimalFormat::DecimalFormat(  const UnicodeString& pattern,
                    DecimalFormatSymbols* symbolsToAdopt,
                    UParseError& parseErr,
                    UErrorCode& status)
: NumberFormat(),
  fPosPrefixPattern(0), 
  fPosSuffixPattern(0), 
  fNegPrefixPattern(0), 
  fNegSuffixPattern(0),
  fCurrencyChoice(0),
  fMultiplier(0),
  fGroupingSize(0),
  fGroupingSize2(0),
  fSymbols(0),
  fMinExponentDigits(0),
  fRoundingIncrement(0),
  fPad(0),
  fFormatWidth(0)
{
    if (symbolsToAdopt == NULL)
        status = U_ILLEGAL_ARGUMENT_ERROR;
    construct(status,parseErr, &pattern, symbolsToAdopt);
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
  fCurrencyChoice(0),
  fMultiplier(0),
  fGroupingSize(0),
  fGroupingSize2(0),
  fSymbols(0),
  fMinExponentDigits(0),
  fRoundingIncrement(0),
  fPad(0),
  fFormatWidth(0)
{
    UParseError parseError;
    construct(status, parseError, &pattern, new DecimalFormatSymbols(symbols));
}

//------------------------------------------------------------------------------
// Constructs a DecimalFormat instance with the specified number format
// pattern and the number format symbols in the desired locale.  The
// created instance owns the symbols.

void
DecimalFormat::construct(UErrorCode&             status,
                         UParseError&           parseErr,
                         const UnicodeString*   pattern,
                         DecimalFormatSymbols*  symbolsToAdopt)
{
    fSymbols = symbolsToAdopt; // Do this BEFORE aborting on status failure!!!
//    fDigitList = new DigitList(); // Do this BEFORE aborting on status failure!!!
    fRoundingIncrement = NULL;
    fRoundingDouble = 0.0;
    fRoundingMode = kRoundHalfEven;
    fPad = kPatternPadEscape;
    fPadPosition = kPadBeforePrefix;
    if (U_FAILURE(status))
        return;

    fPosPrefixPattern = fPosSuffixPattern = NULL;
    fNegPrefixPattern = fNegSuffixPattern = NULL;
    fMultiplier = 1;
    fGroupingSize = 3;
    fGroupingSize2 = 0;
    fDecimalSeparatorAlwaysShown = FALSE;
    fIsCurrencyFormat = FALSE;
    fUseExponentialNotation = FALSE;
    fMinExponentDigits = 0;

    if (fSymbols == NULL)
    {
        fSymbols = new DecimalFormatSymbols(Locale::getDefault(), status);
        /* test for NULL */
        if (fSymbols == 0) {
            status = U_MEMORY_ALLOCATION_ERROR;
            return;
        }
    }

    UnicodeString str;
    // Uses the default locale's number format pattern if there isn't
    // one specified.
    if (pattern == NULL)
    {
        ResourceBundle resource((char *)0, Locale::getDefault(), status);

        str = resource.get(fgNumberPatterns, status).getStringEx((int32_t)0, status);
        pattern = &str;
    }

    if (U_FAILURE(status))
    {
        return;
    }

    if (symbolsToAdopt == NULL) {
        setCurrencyForLocale(uloc_getDefault(), status);
    } else {
        setCurrencyForSymbols();
    }

    applyPattern(*pattern, FALSE /*not localized*/,parseErr, status);
}

/**
 * Sets our currency to be the default currency for the given locale.
 */
void DecimalFormat::setCurrencyForLocale(const char* locale, UErrorCode& ec) {
    const UChar* c = NULL;
    if (U_SUCCESS(ec)) {
        // Trap an error in mapping locale to currency.  If we can't
        // map, then don't fail and set the currency to "".
        UErrorCode ec2 = U_ZERO_ERROR;
        c = ucurr_forLocale(locale, &ec2);
    }
    setCurrency(c);
}

//------------------------------------------------------------------------------

DecimalFormat::~DecimalFormat()
{
//    delete fDigitList;
    delete fPosPrefixPattern;
    delete fPosSuffixPattern;
    delete fNegPrefixPattern;
    delete fNegSuffixPattern;
    delete fCurrencyChoice;
    delete fSymbols;
    delete fRoundingIncrement;
}

//------------------------------------------------------------------------------
// copy constructor

DecimalFormat::DecimalFormat(const DecimalFormat &source)
:   NumberFormat(source),
//    fDigitList(NULL),
    fPosPrefixPattern(NULL),
    fPosSuffixPattern(NULL),
    fNegPrefixPattern(NULL),
    fNegSuffixPattern(NULL),
    fCurrencyChoice(NULL),
    fSymbols(NULL),
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
    if (rhs.fCurrencyChoice == 0) {
        delete fCurrencyChoice;
        fCurrencyChoice = 0;
    } else {
        fCurrencyChoice = (ChoiceFormat*) rhs.fCurrencyChoice->clone();
    }
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
    fExponentSignAlwaysShown = rhs.fExponentSignAlwaysShown;
    /*Bertrand A. D. Update 98.03.17*/
    fIsCurrencyFormat = rhs.fIsCurrencyFormat;
    /*end of Update*/
    fMinExponentDigits = rhs.fMinExponentDigits;
//    if (fDigitList == NULL)
//        fDigitList = new DigitList();
    
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

#ifdef FMT_DEBUG
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
                      UnicodeString& appendTo,
                      FieldPosition& fieldPosition) const
{
    DigitList digits;

    // Clears field positions.
    fieldPosition.setBeginIndex(0);
    fieldPosition.setEndIndex(0);

    // If we are to do rounding, we need to move into the BigDecimal
    // domain in order to do divide/multiply correctly.
    // ||
    // In general, long values always represent real finite numbers, so
    // we don't have to check for +/- Infinity or NaN.  However, there
    // is one case we have to be careful of:  The multiplier can push
    // a number near MIN_VALUE or MAX_VALUE outside the legal range.  We
    // check for this before multiplying, and if it happens we use doubles
    // instead, trading off accuracy for range.
    if (fRoundingIncrement != NULL
        || (fMultiplier != 0 && (number > (INT32_MAX / fMultiplier)
                              || number < (INT32_MIN / fMultiplier))))
    {
        digits.set(((double)number) * fMultiplier,
                   fUseExponentialNotation ?
                            getMinimumIntegerDigits() + getMaximumFractionDigits() : 0,
                   !fUseExponentialNotation);
    }
    else
    {
        digits.set(number * fMultiplier,
                   fUseExponentialNotation ?
                            getMinimumIntegerDigits() + getMaximumFractionDigits() : 0);
    }

    return subformat(appendTo, fieldPosition, digits, TRUE);
}
 
//------------------------------------------------------------------------------

UnicodeString&
DecimalFormat::format(  double number,
                        UnicodeString& appendTo,
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
            fieldPosition.setBeginIndex(appendTo.length());

        appendTo += getConstSymbol(DecimalFormatSymbols::kNaNSymbol);

        if (fieldPosition.getField() == NumberFormat::kIntegerField)
            fieldPosition.setEndIndex(appendTo.length());

        addPadding(appendTo, fieldPosition, 0, 0);
        return appendTo;
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

    // Do this BEFORE checking to see if value is infinite! Sets the
    // begin and end index to be length of the string composed of
    // localized name of Infinite and the positive/negative localized
    // signs.

    number *= fMultiplier;

    // Apply rounding after multiplier
    if (fRoundingIncrement != NULL) {
        if (isNegative)     // For rounding in the correct direction
            number = -number;
        number = fRoundingDouble
            * round(number / fRoundingDouble, fRoundingMode, isNegative);
        if (isNegative)
            number = -number;
    }

    // Special case for INFINITE,
    if (uprv_isInfinite(number))
    {
        int32_t prefixLen = appendAffix(appendTo, number, isNegative, TRUE);

        if (fieldPosition.getField() == NumberFormat::kIntegerField)
            fieldPosition.setBeginIndex(appendTo.length());

        appendTo += getConstSymbol(DecimalFormatSymbols::kInfinitySymbol);

        if (fieldPosition.getField() == NumberFormat::kIntegerField)
            fieldPosition.setEndIndex(appendTo.length());

        int32_t suffixLen = appendAffix(appendTo, number, isNegative, FALSE);

        addPadding(appendTo, fieldPosition, prefixLen, suffixLen);
        return appendTo;
    }

    DigitList digits;

    // This detects negativity too.
    digits.set(number, fUseExponentialNotation ?
                             getMinimumIntegerDigits() + getMaximumFractionDigits() :
                             getMaximumFractionDigits(),
                             !fUseExponentialNotation);

    return subformat(appendTo, fieldPosition, digits, FALSE);
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
                        UnicodeString& appendTo,
                        FieldPosition& fieldPosition,
                        UErrorCode& status) const
{
    return NumberFormat::format(obj, appendTo, fieldPosition, status);
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
DecimalFormat::subformat(UnicodeString& appendTo,
                         FieldPosition& fieldPosition,
                         DigitList&     digits,
                         UBool         isInteger) const
{
    // Gets the localized zero Unicode character.
    UChar32 zero = getConstSymbol(DecimalFormatSymbols::kZeroDigitSymbol).char32At(0);
    int32_t zeroDelta = zero - '0'; // '0' is the DigitList representation of zero
    const UnicodeString *grouping = &getConstSymbol(DecimalFormatSymbols::kGroupingSeparatorSymbol);
    const UnicodeString *decimal;
    if(fIsCurrencyFormat) {
        decimal = &getConstSymbol(DecimalFormatSymbols::kMonetarySeparatorSymbol);
    } else {
        decimal = &getConstSymbol(DecimalFormatSymbols::kDecimalSeparatorSymbol);
    }
    int32_t maxIntDig = getMaximumIntegerDigits();
    int32_t minIntDig = getMinimumIntegerDigits();

    /* Per bug 4147706, DecimalFormat must respect the sign of numbers which
     * format as zero.  This allows sensible computations and preserves
     * relations such as signum(1/x) = signum(x), where x is +Infinity or
     * -Infinity.  Prior to this fix, we always formatted zero values as if
     * they were positive.  Liu 7/6/98.
     */
    if (digits.isZero())
    {
        digits.fDecimalAt = digits.fCount = 0; // Normalize
    }

    // Appends the prefix.
    double doubleValue = digits.getDouble();
    int32_t prefixLen = appendAffix(appendTo, doubleValue, !digits.fIsPositive, TRUE);

    if (fUseExponentialNotation)
    {
        // Record field information for caller.
        if (fieldPosition.getField() == NumberFormat::kIntegerField)
        {
            fieldPosition.setBeginIndex(appendTo.length());
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
        int32_t exponent = digits.fDecimalAt;
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
        int32_t integerDigits = digits.isZero() ? minIntDig :
            digits.fDecimalAt - exponent;
        int32_t totalDigits = digits.fCount;
        if (minimumDigits > totalDigits)
            totalDigits = minimumDigits;
        if (integerDigits > totalDigits)
            totalDigits = integerDigits;

        // totalDigits records total number of digits needs to be processed
        int32_t i;
        for (i=0; i<totalDigits; ++i)
        {
            if (i == integerDigits)
            {
                // Record field information for caller.
                if (fieldPosition.getField() == NumberFormat::kIntegerField)
                    fieldPosition.setEndIndex(appendTo.length());

                appendTo += *decimal;

                // Record field information for caller.
                if (fieldPosition.getField() == NumberFormat::kFractionField)
                    fieldPosition.setBeginIndex(appendTo.length());
            }
            // Restores the digit character or pads the buffer with zeros.
            UChar32 c = (UChar32)((i < digits.fCount) ?
                          (digits.fDigits[i] + zeroDelta) :
                          zero);
            appendTo += c;
        }

        // Record field information
        if (fieldPosition.getField() == NumberFormat::kIntegerField)
        {
            if (fieldPosition.getEndIndex() < 0)
                fieldPosition.setEndIndex(appendTo.length());
        }
        else if (fieldPosition.getField() == NumberFormat::kFractionField)
        {
            if (fieldPosition.getBeginIndex() < 0)
                fieldPosition.setBeginIndex(appendTo.length());
            fieldPosition.setEndIndex(appendTo.length());
        }

        // The exponent is output using the pattern-specified minimum
        // exponent digits.  There is no maximum limit to the exponent
        // digits, since truncating the exponent would appendTo in an
        // unacceptable inaccuracy.
        appendTo += getConstSymbol(DecimalFormatSymbols::kExponentialSymbol);

        // For zero values, we force the exponent to zero.  We
        // must do this here, and not earlier, because the value
        // is used to determine integer digit count above.
        if (digits.isZero())
            exponent = 0;

        if (exponent < 0) {
            appendTo += getConstSymbol(DecimalFormatSymbols::kMinusSignSymbol);
        } else if (fExponentSignAlwaysShown) {
            appendTo += getConstSymbol(DecimalFormatSymbols::kPlusSignSymbol);
        }

        DigitList expDigits;
        expDigits.set(exponent);
        for (i=expDigits.fDecimalAt; i<fMinExponentDigits; ++i)
            appendTo += (zero);
        for (i=0; i<expDigits.fDecimalAt; ++i)
        {
            UChar32 c = (UChar32)((i < expDigits.fCount) ?
                          (expDigits.fDigits[i] + zeroDelta) : zero);
            appendTo += c;
        }
    }
    else  // Not using exponential notation
    {
        // Record field information for caller.
        if (fieldPosition.getField() == NumberFormat::kIntegerField)
            fieldPosition.setBeginIndex(appendTo.length());

        // Output the integer portion.  Here 'count' is the total
        // number of integer digits we will display, including both
        // leading zeros required to satisfy getMinimumIntegerDigits,
        // and actual digits present in the number.
        int32_t count = minIntDig;
        int32_t digitIndex = 0; // Index into digits.fDigits[]
        if (digits.fDecimalAt > 0 && count < digits.fDecimalAt)
            count = digits.fDecimalAt;

        // Handle the case where getMaximumIntegerDigits() is smaller
        // than the real number of integer digits.  If this is so, we
        // output the least significant max integer digits.  For example,
        // the value 1997 printed with 2 max integer digits is just "97".

        if (count > maxIntDig)
        {
            count = maxIntDig;
            digitIndex = digits.fDecimalAt - count;
        }

        int32_t sizeBeforeIntegerPart = appendTo.length();

        int32_t i;
        for (i=count-1; i>=0; --i)
        {
            if (i < digits.fDecimalAt && digitIndex < digits.fCount)
            {
                // Output a real digit
                appendTo += ((UChar32)(digits.fDigits[digitIndex++] + zeroDelta));
            }
            else
            {
                // Output a leading zero
                appendTo += (zero);
            }

            // Output grouping separator if necessary.
            if (isGroupingPosition(i)) {
                appendTo.append(*grouping);
            }
        }

        // Record field information for caller.
        if (fieldPosition.getField() == NumberFormat::kIntegerField)
            fieldPosition.setEndIndex(appendTo.length());

        // Determine whether or not there are any printable fractional
        // digits.  If we've used up the digits we know there aren't.
        UBool fractionPresent = (getMinimumFractionDigits() > 0) ||
            (!isInteger && digitIndex < digits.fCount);

        // If there is no fraction present, and we haven't printed any
        // integer digits, then print a zero.  Otherwise we won't print
        // _any_ digits, and we won't be able to parse this string.
        if (!fractionPresent && appendTo.length() == sizeBeforeIntegerPart)
            appendTo += (zero);

        // Output the decimal separator if we always do so.
        if (fDecimalSeparatorAlwaysShown || fractionPresent)
            appendTo += *decimal;

        // Record field information for caller.
        if (fieldPosition.getField() == NumberFormat::kFractionField)
            fieldPosition.setBeginIndex(appendTo.length());

        int32_t maxFracDigits = getMaximumFractionDigits();
        int32_t negDecimalAt = -digits.fDecimalAt;
        for (i=0; i < maxFracDigits; ++i)
        {
            if (!isInteger && digitIndex < digits.fCount)
            {
                if (i >= negDecimalAt)
                {
                    // Output a digit
                    appendTo += ((UChar32)(digits.fDigits[digitIndex++] + zeroDelta));
                }
                else
                {
                    // Output leading fractional zeros.  These are zeros that come after
                    // the decimal but before any significant digits.  These are only
                    // output if abs(number being formatted) < 1.0.
                    appendTo += zero;
                }
            }
            else
            {
                // Here is where we escape from the loop.  We escape if we've output
                // the maximum fraction digits (specified in the for expression above).
                // We also stop when we've output the minimum digits and either:
                // we have an integer, so there is no fractional stuff to display,
                // or we're out of significant digits.
                if (i >= getMinimumFractionDigits())
                    break;

                // No precision is left.
                appendTo += zero;
            }
        }

        // Record field information for caller.
        if (fieldPosition.getField() == NumberFormat::kFractionField)
            fieldPosition.setEndIndex(appendTo.length());
    }

    int32_t suffixLen = appendAffix(appendTo, doubleValue, !digits.fIsPositive, FALSE);

    addPadding(appendTo, fieldPosition, prefixLen, suffixLen);
    return appendTo;
}

/**
 * Inserts the character fPad as needed to expand result to fFormatWidth.
 * @param result the string to be padded
 */
void DecimalFormat::addPadding(UnicodeString& appendTo,
                               FieldPosition& fieldPosition,
                               int32_t prefixLen,
                               int32_t suffixLen) const
{
    if (fFormatWidth > 0) {
        int32_t len = fFormatWidth - appendTo.length();
        if (len > 0) {
            UnicodeString padding;
            for (int32_t i=0; i<len; ++i) {
                padding += fPad;
            }
            switch (fPadPosition) {
            case kPadAfterPrefix:
                appendTo.insert(prefixLen, padding);
                break;
            case kPadBeforePrefix:
                appendTo.insert(0, padding);
                break;
            case kPadBeforeSuffix:
                appendTo.insert(appendTo.length() - suffixLen, padding);
                break;
            case kPadAfterSuffix:
                appendTo += padding;
                break;
            }
            if (fPadPosition == kPadBeforePrefix ||
                fPadPosition == kPadAfterPrefix) {
                fieldPosition.setBeginIndex(len + fieldPosition.getBeginIndex());
                fieldPosition.setEndIndex(len + fieldPosition.getEndIndex());
            }
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

void
DecimalFormat::parse(const UnicodeString& text,
                     Formattable& result,
                     ParsePosition& parsePosition) const
{
    int32_t backup;
    int32_t i = backup = parsePosition.getIndex();

    // Handle NaN as a special case:
    
    // Skip padding characters, if around prefix
    if (fFormatWidth > 0 && (fPadPosition == kPadBeforePrefix ||
                             fPadPosition == kPadAfterPrefix)) {
        i = skipPadding(text, i);
    }
    // If the text is composed of the representation of NaN, returns NaN.length
    const UnicodeString *nan = &getConstSymbol(DecimalFormatSymbols::kNaNSymbol);
    int32_t nanLen = (text.compare(i, nan->length(), *nan)
                      ? 0 : nan->length());
    if (nanLen) {
        i += nanLen;
        if (fFormatWidth > 0 && (fPadPosition == kPadBeforeSuffix ||
                                 fPadPosition == kPadAfterSuffix)) {
            i = skipPadding(text, i);
        }
        parsePosition.setIndex(i);
        result.setDouble(uprv_getNaN());
        return;
    }
    
    // NaN parse failed; start over
    i = backup;

    // status is used to record whether a number is infinite.
    UBool status[fgStatusLength];
    DigitList digits;

    if (!subparse(text, parsePosition, digits, status)) {
        parsePosition.setIndex(backup);
        return;
    }

    // Handle infinity
    if (status[fgStatusInfinite]) {
        double inf = uprv_getInfinity();
        result.setDouble(digits.fIsPositive ? inf : -inf);
        return;
    }

    // Do as much of the multiplier conversion as possible without
    // losing accuracy.
    int32_t mult = fMultiplier; // Don't modify this.multiplier
    while (mult % 10 == 0) {
        mult /= 10;
        --digits.fDecimalAt;
    }

    // Handle integral values.  We want to return the most
    // parsimonious type that will accommodate all of the result's
    // precision.  We therefore only return a long if the result fits
    // entirely within a long (taking into account the multiplier) --
    // otherwise we fall through and return a double.  When more
    // numeric types are supported by Formattable (e.g., 64-bit
    // integers, bignums) we will extend this logic to include them.
    if (digits.fitsIntoLong(isParseIntegerOnly())) {
        int32_t n = digits.getLong();
        if (n % mult == 0) {
            result.setLong(n / mult);
            return;
        }
        else {  // else handle the remainder
            result.setDouble(((double)n) / mult);
            return;
        }
    }
    else {
        // Handle non-integral or very large values
        // Dividing by one is okay and not that costly.
        result.setDouble(digits.getDouble() / mult);
        return;
    }
}


/*
This is an old implimentation that was preparing for 64-bit numbers in ICU.
It is very slow, and 64-bit numbers are not ANSI-C compatible. This code
is here if we change our minds.
*/
/**
 * Parse the given text into a number.  The text is parsed beginning at
 * parsePosition, until an unparseable character is seen.
 * @param text The string to parse.
 * @param parsePosition The position at which to being parsing.  Upon
 * return, the first unparseable character.
 * @param digits The DigitList to set to the parsed value.
 * @param isExponent If true, parse an exponent.  This means no
 * infinite values and integer only. By default it's really false.
 * @param status Upon return contains boolean status flags indicating
 * whether the value was infinite and whether it was positive.
 */
UBool DecimalFormat::subparse(const UnicodeString& text, ParsePosition& parsePosition,
                               DigitList& digits, UBool* status) const
{
    int32_t position = parsePosition.getIndex();
    int32_t oldStart = position;

    // Match padding before prefix
    if (fFormatWidth > 0 && fPadPosition == kPadBeforePrefix) {
        position = skipPadding(text, position);
    }

    // Match positive and negative prefixes; prefer longest match.
    int32_t posMatch = compareAffix(text, position, FALSE, TRUE);
    int32_t negMatch = compareAffix(text, position, TRUE, TRUE);
    if (posMatch >= 0 && negMatch >= 0) {
        if (posMatch > negMatch) {
            negMatch = -1;
        } else if (negMatch > posMatch) {
            posMatch = -1;
        }  
    }
    if (posMatch >= 0) {
        position += posMatch;
    } else if (negMatch >= 0) {
        position += negMatch;
    } else {
        parsePosition.setErrorIndex(position);
        return FALSE;
    }

    // Match padding before prefix
    if (fFormatWidth > 0 && fPadPosition == kPadAfterPrefix) {
        position = skipPadding(text, position);
    }

    // process digits or Inf, find decimal position
    const UnicodeString *inf = &getConstSymbol(DecimalFormatSymbols::kInfinitySymbol);
    int32_t infLen = (text.compare(position, inf->length(), *inf)
        ? 0 : inf->length());
    position += infLen; // infLen is non-zero when it does equal to infinity
    status[fgStatusInfinite] = (UBool)infLen;
    if (!infLen)
    {
        // We now have a string of digits, possibly with grouping symbols,
        // and decimal points.  We want to process these into a DigitList.
        // We don't want to put a bunch of leading zeros into the DigitList
        // though, so we keep track of the location of the decimal point,
        // put only significant digits into the DigitList, and adjust the
        // exponent as needed.

        digits.fDecimalAt = digits.fCount = 0;
        UChar32 zero = getConstSymbol(DecimalFormatSymbols::kZeroDigitSymbol).char32At(0);

        const UnicodeString *decimal;
        if(fIsCurrencyFormat) {
            decimal = &getConstSymbol(DecimalFormatSymbols::kMonetarySeparatorSymbol);
        } else {
            decimal = &getConstSymbol(DecimalFormatSymbols::kDecimalSeparatorSymbol);
        }
        const UnicodeString *grouping = &getConstSymbol(DecimalFormatSymbols::kGroupingSeparatorSymbol);
        const UnicodeString *exponentChar = &getConstSymbol(DecimalFormatSymbols::kExponentialSymbol);
        UBool sawDecimal = FALSE;
        UBool sawDigit = FALSE;
        int32_t backup = -1;
        int32_t digit;
        int32_t textLength = text.length(); // One less pointer to follow
        int32_t groupingLen = grouping->length();
        int32_t decimalLen = decimal->length();

        // We have to track digitCount ourselves, because digits.fCount will
        // pin when the maximum allowable digits is reached.
        int32_t digitCount = 0;

        for (; position < textLength; )
        {
            UChar32 ch = text.char32At(position);

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
                digit = u_charDigitValue(ch);
            }

            if (digit > 0 && digit <= 9)
            {
                // Cancel out backup setting (see grouping handler below)
                backup = -1;

                sawDigit = TRUE;
                // output a regular non-zero digit.
                ++digitCount;
                digits.append((char)(digit + '0'));
                position += U16_LENGTH(ch);
            }
            else if (digit == 0)
            {
                // Cancel out backup setting (see grouping handler below)
                backup = -1;
                sawDigit = TRUE;

                // Check for leading zeros
                if (digits.fCount != 0)
                {
                    // output a regular zero digit.
                    ++digitCount;
                    digits.append((char)(digit + '0'));
                }
                else if (sawDecimal)
                {
                    // If we have seen the decimal, but no significant digits yet,
                    // then we account for leading zeros by decrementing the
                    // digits.fDecimalAt into negative values.
                    --digits.fDecimalAt;
                }
                // else ignore leading zeros in integer part of number.
                position += U16_LENGTH(ch);
            }
            else if (!text.compare(position, groupingLen, *grouping) && isGroupingUsed())
            {
                // Ignore grouping characters, if we are using them, but require
                // that they be followed by a digit.  Otherwise we backup and
                // reprocess them.
                backup = position;
                position += groupingLen;
            }
            else if (!text.compare(position, decimalLen, *decimal) && !isParseIntegerOnly() && !sawDecimal)
            {
                // If we're only parsing integers, or if we ALREADY saw the
                // decimal, then don't parse this one.

                digits.fDecimalAt = digitCount; // Not digits.fCount!
                sawDecimal = TRUE;
                position += decimalLen;
            }
            else {
                const UnicodeString *tmp;
                tmp = &getConstSymbol(DecimalFormatSymbols::kExponentialSymbol);
                if (!text.caseCompare(position, tmp->length(), *tmp, U_FOLD_CASE_DEFAULT))    // error code is set below if !sawDigit
                {
                    // Parse sign, if present
                    int32_t pos = position + tmp->length();
                    DigitList exponentDigits;

                    if (pos < textLength)
                    {
                        tmp = &getConstSymbol(DecimalFormatSymbols::kPlusSignSymbol);
                        if (!text.compare(pos, tmp->length(), *tmp))
                        {
                            pos += tmp->length();
                        }
                        else {
                            tmp = &getConstSymbol(DecimalFormatSymbols::kMinusSignSymbol);
                            if (!text.compare(pos, tmp->length(), *tmp))
                            {
                                pos += tmp->length();
                                exponentDigits.fIsPositive = FALSE;
                            }
                        }
                    }

                    while (pos < textLength) {
                        ch = text[(int32_t)pos];
                        digit = ch - zero;

                        if (digit < 0 || digit > 9) {
                            digit = u_charDigitValue(ch);
                        }
                        if (0 <= digit && digit <= 9) {
                            ++pos;
                            exponentDigits.append((char)(digit + '0'));
                        } else {
                            break;
                        }
                    }

                    if (exponentDigits.fCount > 0) {
                        exponentDigits.fDecimalAt = exponentDigits.fCount;
                        digits.fDecimalAt += exponentDigits.getLong();
                        position = pos; // Advance past the exponent
                    }

                    break; // Whether we fail or succeed, we exit this loop
                }
                else {
                    break;
                }
            }
        }

        if (backup != -1)
        {
            position = backup;
        }

        // If there was no decimal point we have an integer
        if (!sawDecimal)
        {
            digits.fDecimalAt += digitCount; // Not digits.fCount!
        }

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

    // Match padding before suffix
    if (fFormatWidth > 0 && fPadPosition == kPadBeforeSuffix) {
        position = skipPadding(text, position);
    }

    // Match positive and negative suffixes; prefer longest match.
    if (posMatch >= 0) {
        posMatch = compareAffix(text, position, FALSE, FALSE);
    }
    if (negMatch >= 0) {
        negMatch = compareAffix(text, position, TRUE, FALSE);
    }
    if (posMatch >= 0 && negMatch >= 0) {
        if (posMatch > negMatch) {
            negMatch = -1;
        } else if (negMatch > posMatch) {
            posMatch = -1;
        }  
    }

    // Fail if neither or both
    if ((posMatch >= 0) == (negMatch >= 0)) {
        parsePosition.setErrorIndex(position);
        return FALSE;
    }

    position += (posMatch>=0 ? posMatch : negMatch);

    // Match padding before suffix
    if (fFormatWidth > 0 && fPadPosition == kPadAfterSuffix) {
        position = skipPadding(text, position);
    }

    parsePosition.setIndex(position);

    digits.fIsPositive = (posMatch >= 0);

    if(parsePosition.getIndex() == oldStart)
    {
        parsePosition.setErrorIndex(position);
        return FALSE;
    }
    return TRUE;
}

/**
 * Starting at position, advance past a run of pad characters, if any.
 * Return the index of the first character after position that is not a pad
 * character.  Result is >= position.
 */
int32_t DecimalFormat::skipPadding(const UnicodeString& text, int32_t position) const {
    int32_t padLen = U16_LENGTH(fPad);
    while (position < text.length() &&
           text.char32At(position) == fPad) {
        position += padLen;
    }
    return position;
}

/**
 * Return the length matched by the given affix, or -1 if none.
 * Runs of white space in the affix, match runs of white space in
 * the input.  Pattern white space and input white space are
 * determined differently; see code.
 * @param text input text
 * @param pos offset into input at which to begin matching
 * @param isNegative
 * @param isPrefix
 * @return length of input that matches, or -1 if match failure
 */
int32_t DecimalFormat::compareAffix(const UnicodeString& text,
                                    int32_t pos,
                                    UBool isNegative,
                                    UBool isPrefix) const {
    if (fCurrencyChoice != NULL) {
        if (isPrefix) {
            return compareComplexAffix(isNegative ? *fNegPrefixPattern : *fPosPrefixPattern,
                                       text, pos);
        } else {
            return compareComplexAffix(isNegative ? *fNegSuffixPattern : *fPosSuffixPattern,
                                       text, pos);
        }
    }
    
    if (isPrefix) {
        return compareSimpleAffix(isNegative ? fNegativePrefix : fPositivePrefix,
                                  text, pos);
    } else {
        return compareSimpleAffix(isNegative ? fNegativeSuffix : fPositiveSuffix,
                                  text, pos);
    }
}

/**
 * Return the length matched by the given affix, or -1 if none.
 * Runs of white space in the affix, match runs of white space in
 * the input.  Pattern white space and input white space are
 * determined differently; see code.
 * @param affix pattern string, taken as a literal
 * @param input input text
 * @param pos offset into input at which to begin matching
 * @return length of input that matches, or -1 if match failure
 */
int32_t DecimalFormat::compareSimpleAffix(const UnicodeString& affix,
                                          const UnicodeString& input,
                                          int32_t pos) {
    int32_t start = pos;
    for (int32_t i=0; i<affix.length(); ) {
        UChar32 c = affix.char32At(i);
        int32_t len = U16_LENGTH(c);
        if (uprv_isRuleWhiteSpace(c)) {
            // We may have a pattern like: \u200F \u0020
            //        and input text like: \u200F \u0020
            // Note that U+200F and U+0020 are RuleWhiteSpace but only
            // U+0020 is UWhiteSpace.  So we have to first do a direct
            // match of the run of RULE whitespace in the pattern,
            // then match any extra characters.
            UBool literalMatch = FALSE;
            while (pos < input.length() &&
                   input.char32At(pos) == c) {
                literalMatch = TRUE;
                i += len;
                pos += len;
                if (i == affix.length()) {
                    break;
                }
                c = affix.char32At(i);
                len = U16_LENGTH(c);
                if (!uprv_isRuleWhiteSpace(c)) {
                    break;
                }
            }

            // Advance over run in pattern
            i = skipRuleWhiteSpace(affix, i);

            // Advance over run in input text
            // Must see at least one white space char in input,
            // unless we've already matched some characters literally.
            int32_t s = pos;
            pos = skipUWhiteSpace(input, pos);
            if (pos == s && !literalMatch) {
                return -1;
            }
        } else {
            if (pos < input.length() &&
                input.char32At(pos) == c) {
                i += len;
                pos += len;
            } else {
                return -1;
            }
        }
    }
    return pos - start;
}

/**
 * Skip over a run of zero or more isRuleWhiteSpace() characters at
 * pos in text.
 */
int32_t DecimalFormat::skipRuleWhiteSpace(const UnicodeString& text, int32_t pos) {
    while (pos < text.length()) {
        UChar32 c = text.char32At(pos);
        if (!uprv_isRuleWhiteSpace(c)) {
            break;
        }
        pos += U16_LENGTH(c);
    }
    return pos;
}

/**
 * Skip over a run of zero or more isUWhiteSpace() characters at pos
 * in text.
 */
int32_t DecimalFormat::skipUWhiteSpace(const UnicodeString& text, int32_t pos) {
    while (pos < text.length()) {
        UChar32 c = text.char32At(pos);
        if (!u_isUWhiteSpace(c)) {
            break;
        }
        pos += U16_LENGTH(c);
    }
    return pos;
}

/**
 * Return the length matched by the given affix, or -1 if none.
 * @param affixPat pattern string
 * @param input input text
 * @param pos offset into input at which to begin matching
 * @return length of input that matches, or -1 if match failure
 */
int32_t DecimalFormat::compareComplexAffix(const UnicodeString& affixPat,
                                           const UnicodeString& text,
                                           int32_t pos) const {
    U_ASSERT(fCurrencyChoice != NULL);
    U_ASSERT(*getCurrency() != 0);

    for (int32_t i=0; i<affixPat.length() && pos >= 0; ) {
        UChar32 c = affixPat.char32At(i);
        i += U16_LENGTH(c);

        if (c == kQuote) {
            U_ASSERT(i <= affixPat.length());
            c = affixPat.char32At(i);
            i += U16_LENGTH(c);

            const UnicodeString* affix = NULL;

            switch (c) {
            case kCurrencySign: {
                UBool intl = i<affixPat.length() &&
                    affixPat.char32At(i) == kCurrencySign;
                if (intl) {
                    ++i;
                    pos = match(text, pos, getCurrency());
                } else {
                    ParsePosition ppos(pos);
                    Formattable result;
                    fCurrencyChoice->parse(text, result, ppos);
                    pos = (ppos.getIndex() == pos) ? -1 : ppos.getIndex();
                }
                continue;
            }
            case kPatternPercent:
                affix = &getConstSymbol(DecimalFormatSymbols::kPercentSymbol);
                break;
            case kPatternPerMill:
                affix = &getConstSymbol(DecimalFormatSymbols::kPerMillSymbol);
                break;
            case kPatternPlus:
                affix = &getConstSymbol(DecimalFormatSymbols::kPlusSignSymbol);
                break;
            case kPatternMinus:
                affix = &getConstSymbol(DecimalFormatSymbols::kMinusSignSymbol);
                break;
            default:
                // fall through to affix!=0 test, which will fail
                break;
            }

            if (affix != NULL) {
                pos = match(text, pos, *affix);
                continue;
            }
        }

        pos = match(text, pos, c);
        if (uprv_isRuleWhiteSpace(c)) {
            i = skipRuleWhiteSpace(affixPat, i);
        }
    }
    return pos;
}

/**
 * Match a single character at text[pos] and return the index of the
 * next character upon success.  Return -1 on failure.  If
 * isRuleWhiteSpace(ch) then match a run of white space in text.
 */
int32_t DecimalFormat::match(const UnicodeString& text, int32_t pos, UChar32 ch) {
    if (uprv_isRuleWhiteSpace(ch)) {
        // Advance over run of white space in input text
        // Must see at least one white space char in input
        int32_t s = pos;
        pos = skipUWhiteSpace(text, pos);
        if (pos == s) {
            return -1;
        }
        return pos;
    }
    return (pos >= 0 && text.char32At(pos) == ch) ?
        (pos + U16_LENGTH(ch)) : -1;
}

/**
 * Match a string at text[pos] and return the index of the next
 * character upon success.  Return -1 on failure.  Match a run of
 * white space in str with a run of white space in text.
 */
int32_t DecimalFormat::match(const UnicodeString& text, int32_t pos, const UnicodeString& str) {
    for (int32_t i=0; i<str.length() && pos >= 0; ) {
        UChar32 ch = str.char32At(i);
        i += U16_LENGTH(ch);
        if (uprv_isRuleWhiteSpace(ch)) {
            i = skipRuleWhiteSpace(str, i);
        }
        pos = match(text, pos, ch);
    }
    return pos;
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
    setCurrencyForSymbols();
    expandAffixes();
}
//------------------------------------------------------------------------------
// Setting the symbols is equlivalent to adopting a newly created localized
// symbols.

void
DecimalFormat::setDecimalFormatSymbols(const DecimalFormatSymbols& symbols)
{
    adoptDecimalFormatSymbols(new DecimalFormatSymbols(symbols));
}
 
/**
 * Update the currency object to match the symbols.  This method
 * is used only when the caller has passed in a symbols object
 * that may not be the default object for its locale.
 */
void
DecimalFormat::setCurrencyForSymbols() {
    /*Bug 4212072
      Update the affix strings accroding to symbols in order to keep
      the affix strings up to date.
      [Richard/GCL]
    */

    // With the introduction of the Currency object, the currency
    // symbols in the DFS object are ignored.  For backward
    // compatibility, we check any explicitly set DFS object.  If it
    // is a default symbols object for its locale, we change the
    // currency object to one for that locale.  If it is custom,
    // we set the currency to null.
    UErrorCode ec = U_ZERO_ERROR;
    DecimalFormatSymbols def(fSymbols->getLocale(), ec);

    if (getConstSymbol(DecimalFormatSymbols::kCurrencySymbol) ==
        def.getConstSymbol(DecimalFormatSymbols::kCurrencySymbol) &&
        getConstSymbol(DecimalFormatSymbols::kIntlCurrencySymbol) ==
        def.getConstSymbol(DecimalFormatSymbols::kIntlCurrencySymbol)
    ) {
        setCurrencyForLocale(fSymbols->getLocale().getName(), ec);
    } else {
        setCurrency(NULL); // Use DFS currency info
    }
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
    // This shouldn't be set to 0.
    // Due to compatibility with ICU4J we cannot set an error code and refuse 0.
    // So the rest of the code should ignore fMultiplier when it's 0. [grhoten]
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
        fRoundingIncrement->set((int32_t)newValue);
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
void DecimalFormat::setPadCharacter(const UnicodeString &padChar) {
    if (padChar.length() > 0) {
        fPad = padChar.char32At(0);
    }
    else {
        fPad = kDefaultPad;
    }
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
void DecimalFormat::expandAffixes() {
    if (fPosPrefixPattern != 0) {
        expandAffix(*fPosPrefixPattern, fPositivePrefix, 0, FALSE);
    }
    if (fPosSuffixPattern != 0) {
        expandAffix(*fPosSuffixPattern, fPositiveSuffix, 0, FALSE);
    }
    if (fNegPrefixPattern != 0) {
        expandAffix(*fNegPrefixPattern, fNegativePrefix, 0, FALSE);
    }
    if (fNegSuffixPattern != 0) {
        expandAffix(*fNegSuffixPattern, fNegativeSuffix, 0, FALSE);
    }
#ifdef FMT_DEBUG
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
 * This method is used in two distinct ways.  First, it is used to expand
 * the stored affix patterns into actual affixes.  For this usage, doFormat
 * must be false.  Second, it is used to expand the stored affix patterns
 * given a specific number (doFormat == true), for those rare cases in
 * which a currency format references a ChoiceFormat (e.g., en_IN display
 * name for INR).  The number itself is taken from digitList.
 *
 * When used in the first way, this method has a side effect: It sets
 * currencyChoice to a ChoiceFormat object, if the currency's display name
 * in this locale is a ChoiceFormat pattern (very rare).  It only does this
 * if currencyChoice is null to start with.
 *
 * @param pattern the non-null, fPossibly empty pattern
 * @param affix string to receive the expanded equivalent of pattern.
 * Previous contents are deleted.
 * @param doFormat if false, then the pattern will be expanded, and if a
 * currency symbol is encountered that expands to a ChoiceFormat, the
 * currencyChoice member variable will be initialized if it is null.  If
 * doFormat is true, then it is assumed that the currencyChoice has been
 * created, and it will be used to format the value in digitList.
 */
void DecimalFormat::expandAffix(const UnicodeString& pattern,
                                UnicodeString& affix,
                                double number,
                                UBool doFormat) const {
    affix.remove();
    for (int i=0; i<pattern.length(); ) {
        UChar32 c = pattern.char32At(i);
        i += U16_LENGTH(c);
        if (c == kQuote) {
            c = pattern.char32At(i);
            i += U16_LENGTH(c);
            switch (c) {
            case kCurrencySign: {
                // As of ICU 2.2 we use the currency object, and
                // ignore the currency symbols in the DFS, unless
                // we have a null currency object.  This occurs if
                // resurrecting a pre-2.2 object or if the user
                // sets a custom DFS.
                UBool intl = i<pattern.length() &&
                    pattern.char32At(i) == kCurrencySign;
                if (intl) {
                    ++i;
                }
                const UChar* currencyUChars = getCurrency();
                if (currencyUChars[0] != 0) {
                    UErrorCode ec = U_ZERO_ERROR;
                    if(intl) {
                        affix += currencyUChars;
                    } else {
                        int32_t len;
                        UBool isChoiceFormat;
                        const UChar* s = ucurr_getName(currencyUChars, fSymbols->getLocale().getName(),
                                                       UCURR_SYMBOL_NAME, &isChoiceFormat, &len, &ec);
                        if (isChoiceFormat) {
                            // Two modes here: If doFormat is false, we set up
                            // currencyChoice.  If doFormat is true, we use the
                            // previously created currencyChoice to format the
                            // value in digitList.
                            if (!doFormat) {
                                // If the currency is handled by a ChoiceFormat,
                                // then we're not going to use the expanded
                                // patterns.  Instantiate the ChoiceFormat and
                                // return.
                                if (fCurrencyChoice == NULL) {
                                    // TODO Replace double-check with proper thread-safe code
                                    ChoiceFormat* fmt = new ChoiceFormat(s, ec);
                                    if (U_SUCCESS(ec)) {
                                        umtx_lock(NULL);
                                        if (fCurrencyChoice == NULL) {
                                            // Cast away const
                                            ((DecimalFormat*)this)->fCurrencyChoice = fmt;
                                            fmt = NULL;
                                        }
                                        umtx_unlock(NULL);
                                        delete fmt;
                                    }
                                }
                                // We could almost return null or "" here, since the
                                // expanded affixes are almost not used at all
                                // in this situation.  However, one method --
                                // toPattern() -- still does use the expanded
                                // affixes, in order to set up a padding
                                // pattern.  We use the CURRENCY_SIGN as a
                                // placeholder.
                                affix.append(kCurrencySign);
                            } else {
                                if (fCurrencyChoice != NULL) {
                                    FieldPosition pos(0); // ignored
                                    if (number < 0) {
                                        number = -number;
                                    }
                                    fCurrencyChoice->format(number, affix, pos);
                                } else {
                                    // We only arrive here if the currency choice
                                    // format in the locale data is INVALID.
                                    affix += currencyUChars;
                                }
                            }
                            continue;
                        }
                        affix += UnicodeString(s, len);
                    }
                } else {
                    if(intl) {
                        affix += getConstSymbol(DecimalFormatSymbols::kIntlCurrencySymbol);
                    } else {
                        affix += getConstSymbol(DecimalFormatSymbols::kCurrencySymbol);
                    }
                }
                break;
            }
            case kPatternPercent:
                affix += getConstSymbol(DecimalFormatSymbols::kPercentSymbol);
                break;
            case kPatternPerMill:
                affix += getConstSymbol(DecimalFormatSymbols::kPerMillSymbol);
                break;
            case kPatternPlus:
                affix += getConstSymbol(DecimalFormatSymbols::kPlusSignSymbol);
                break;
            case kPatternMinus:
                affix += getConstSymbol(DecimalFormatSymbols::kMinusSignSymbol);
                break;
            default:
                affix.append(c);
                break;
            }
        }
        else {
            affix.append(c);
        }
    }
}

/**
 * Append an affix to the given StringBuffer.
 * @param buf buffer to append to
 * @param isNegative
 * @param isPrefix
 */
int32_t DecimalFormat::appendAffix(UnicodeString& buf, double number,
                                   UBool isNegative, UBool isPrefix) const {
    if (fCurrencyChoice != 0) {
        const UnicodeString* affixPat = 0;
        if (isPrefix) {
            affixPat = isNegative ? fNegPrefixPattern : fPosPrefixPattern;
        } else {
            affixPat = isNegative ? fNegSuffixPattern : fPosSuffixPattern;
        }
        UnicodeString affixBuf;
        expandAffix(*affixPat, affixBuf, number, TRUE);
        buf.append(affixBuf);
        return affixBuf.length();
    }
    
    const UnicodeString* affix = NULL;
    if (isPrefix) {
        affix = isNegative ? &fNegativePrefix : &fPositivePrefix;
    } else {
        affix = isNegative ? &fNegativeSuffix : &fPositiveSuffix;
    }
    buf.append(*affix);
    return affix->length();
}

/**
 * Appends an affix pattern to the given StringBuffer, quoting special
 * characters as needed.  Uses the internal affix pattern, if that exists,
 * or the literal affix, if the internal affix pattern is null.  The
 * appended string will generate the same affix pattern (or literal affix)
 * when passed to toPattern().
 *
 * @param appendTo the affix string is appended to this
 * @param affixPattern a pattern such as fPosPrefixPattern; may be null
 * @param expAffix a corresponding expanded affix, such as fPositivePrefix.
 * Ignored unless affixPattern is null.  If affixPattern is null, then
 * expAffix is appended as a literal affix.
 * @param localized true if the appended pattern should contain localized
 * pattern characters; otherwise, non-localized pattern chars are appended
 */
void DecimalFormat::appendAffixPattern(UnicodeString& appendTo,
                                       const UnicodeString* affixPattern,
                                       const UnicodeString& expAffix,
                                       UBool localized) const {
    if (affixPattern == 0) {
        appendAffixPattern(appendTo, expAffix, localized);
    } else {
        int i;
        for (int pos=0; pos<affixPattern->length(); pos=i) {
            i = affixPattern->indexOf(kQuote, pos);
            if (i < 0) {
                UnicodeString s;
                affixPattern->extractBetween(pos, affixPattern->length(), s);
                appendAffixPattern(appendTo, s, localized);
                break;
            }
            if (i > pos) {
                UnicodeString s;
                affixPattern->extractBetween(pos, i, s);
                appendAffixPattern(appendTo, s, localized);
            }
            UChar32 c = affixPattern->char32At(++i);
            ++i;
            if (c == kQuote) {
                appendTo.append(c).append(c);
                // Fall through and append another kQuote below
            } else if (c == kCurrencySign &&
                       i<affixPattern->length() &&
                       affixPattern->char32At(i) == kCurrencySign) {
                ++i;
                appendTo.append(c).append(c);
            } else if (localized) {
                switch (c) {
                case kPatternPercent:
                    appendTo += getConstSymbol(DecimalFormatSymbols::kPercentSymbol);
                    break;
                case kPatternPerMill:
                    appendTo += getConstSymbol(DecimalFormatSymbols::kPerMillSymbol);
                    break;
                case kPatternPlus:
                    appendTo += getConstSymbol(DecimalFormatSymbols::kPlusSignSymbol);
                    break;
                case kPatternMinus:
                    appendTo += getConstSymbol(DecimalFormatSymbols::kMinusSignSymbol);
                    break;
                default:
                    appendTo.append(c);
                }
            } else {
                appendTo.append(c);
            }
        }
    }
}

/**
 * Append an affix to the given StringBuffer, using quotes if
 * there are special characters.  Single quotes themselves must be
 * escaped in either case.
 */
void
DecimalFormat::appendAffixPattern(UnicodeString& appendTo,
                                  const UnicodeString& affix,
                                  UBool localized) const {
    UBool needQuote;
    if(localized) {
        needQuote = affix.indexOf(getConstSymbol(DecimalFormatSymbols::kZeroDigitSymbol)) >= 0
            || affix.indexOf(getConstSymbol(DecimalFormatSymbols::kGroupingSeparatorSymbol)) >= 0
            || affix.indexOf(getConstSymbol(DecimalFormatSymbols::kDecimalSeparatorSymbol)) >= 0
            || affix.indexOf(getConstSymbol(DecimalFormatSymbols::kPercentSymbol)) >= 0
            || affix.indexOf(getConstSymbol(DecimalFormatSymbols::kPerMillSymbol)) >= 0
            || affix.indexOf(getConstSymbol(DecimalFormatSymbols::kDigitSymbol)) >= 0
            || affix.indexOf(getConstSymbol(DecimalFormatSymbols::kPatternSeparatorSymbol)) >= 0
            || affix.indexOf(getConstSymbol(DecimalFormatSymbols::kPlusSignSymbol)) >= 0
            || affix.indexOf(getConstSymbol(DecimalFormatSymbols::kMinusSignSymbol)) >= 0
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
        appendTo += (UChar)0x0027 /*'\''*/;
    if (affix.indexOf((UChar)0x0027 /*'\''*/) < 0)
        appendTo += affix;
    else {
        for (int32_t j = 0; j < affix.length(); ) {
            UChar32 c = affix.char32At(j);
            j += U16_LENGTH(c);
            appendTo += c;
            if (c == 0x0027 /*'\''*/)
                appendTo += c;
        }
    }
    if (needQuote)
        appendTo += (UChar)0x0027 /*'\''*/;
}

//------------------------------------------------------------------------------

/* Tell the VC++ compiler not to spew out the warnings about integral size conversion */
/*
#ifdef _WIN32
#pragma warning( disable : 4761 )
#endif
*/

UnicodeString&
DecimalFormat::toPattern(UnicodeString& result, UBool localized) const
{
    result.remove();
    UChar32 zero;
    UnicodeString digit, group;
    int32_t i;
    int32_t roundingDecimalPos = 0; // Pos of decimal in roundingDigits
    UnicodeString roundingDigits;
    int32_t padPos = (fFormatWidth > 0) ? fPadPosition : -1;
    UnicodeString padSpec;

    if (localized) {
        digit.append(getConstSymbol(DecimalFormatSymbols::kDigitSymbol));
        group.append(getConstSymbol(DecimalFormatSymbols::kGroupingSeparatorSymbol));
        zero = getConstSymbol(DecimalFormatSymbols::kZeroDigitSymbol).char32At(0);
    }
    else {
        digit.append((UChar)kPatternDigit);
        group.append((UChar)kPatternGroupingSeparator);
        zero = (UChar32)kPatternZeroDigit;
    }
    if (fFormatWidth > 0) {
        if (localized) {
            padSpec.append(getConstSymbol(DecimalFormatSymbols::kPadEscapeSymbol));
        }
        else {
            padSpec.append((UChar)kPatternPadEscape);
        }
        padSpec.append(fPad);
    }
    if (fRoundingIncrement != NULL) {
        for(i=0; i<fRoundingIncrement->fCount; ++i) {
            roundingDigits.append((UChar)fRoundingIncrement->fDigits[i]);
        }
        roundingDecimalPos = fRoundingIncrement->fDecimalAt;
    }
    for (int32_t part=0; part<2; ++part) {
        if (padPos == kPadBeforePrefix) {
            result.append(padSpec);
        }
        appendAffixPattern(result,
                    (part==0 ? fPosPrefixPattern : fNegPrefixPattern),
                    (part==0 ? fPositivePrefix : fNegativePrefix),
                    localized);
        if (padPos == kPadAfterPrefix && ! padSpec.isEmpty()) {
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
            if (! roundingDigits.isEmpty()) {
                int32_t pos = roundingDecimalPos - i;
                if (pos >= 0 && pos < roundingDigits.length()) {
                    result.append((UChar) (roundingDigits.char32At(pos) - kPatternZeroDigit + zero));
                    continue;
                }
            }
            if (i<=getMinimumIntegerDigits()) {
                result.append(zero);
            }
            else {
                result.append(digit);
            }
        }
        if (getMaximumFractionDigits() > 0 || fDecimalSeparatorAlwaysShown) {
            if (localized) {
                result += getConstSymbol(DecimalFormatSymbols::kDecimalSeparatorSymbol);
            }
            else {
                result.append((UChar)kPatternDecimalSeparator);
            }
        }
        int32_t pos = roundingDecimalPos;
        for (i = 0; i < getMaximumFractionDigits(); ++i) {
            if (! roundingDigits.isEmpty() && pos < roundingDigits.length()) {
                if (pos < 0) {
                    result.append(zero);
                }
                else {
                    result.append((UChar)(roundingDigits.char32At(pos) - kPatternZeroDigit + zero));
                }
                ++pos;
                continue;
            }
            if (i<getMinimumFractionDigits()) {
                result.append(zero);
            }
            else {
                result.append(digit);
            }
        }
        if (fUseExponentialNotation) {
            if (localized) {
                result += getConstSymbol(DecimalFormatSymbols::kExponentialSymbol);
            }
            else {
                result.append((UChar)kPatternExponent);
            }
            if (fExponentSignAlwaysShown) {
                if (localized) {
                    result += getConstSymbol(DecimalFormatSymbols::kPlusSignSymbol);
                }
                else {
                    result.append((UChar)kPatternPlus);
                }
            }
            for (i=0; i<fMinExponentDigits; ++i) {
                result.append(zero);
            }
        }
        if (! padSpec.isEmpty() && !fUseExponentialNotation) {
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
        if (fPadPosition == kPadBeforeSuffix && ! padSpec.isEmpty()) {
            result.append(padSpec);
        }
        if (part == 0) {
            appendAffixPattern(result, fPosSuffixPattern, fPositiveSuffix, localized);
            if (fPadPosition == kPadAfterSuffix && ! padSpec.isEmpty()) {
                result.append(padSpec);
            }
            UBool isDefault = FALSE;
            if ((fNegSuffixPattern == fPosSuffixPattern && // both null
                 fNegativeSuffix == fPositiveSuffix)
                || (fNegSuffixPattern != 0 && fPosSuffixPattern != 0 &&
                    *fNegSuffixPattern == *fPosSuffixPattern))
            {
                if (fNegPrefixPattern != NULL && fPosPrefixPattern != NULL)
                {
                    int32_t length = fPosPrefixPattern->length();
                    isDefault = fNegPrefixPattern->length() == (length+2) &&
                        (*fNegPrefixPattern)[(int32_t)0] == kQuote &&
                        (*fNegPrefixPattern)[(int32_t)1] == kPatternMinus &&
                        fNegPrefixPattern->compare(2, length, *fPosPrefixPattern, 0, length) == 0;
                }
                if (!isDefault &&
                    fNegPrefixPattern == NULL && fPosPrefixPattern == NULL)
                {
                    int32_t length = fPositivePrefix.length();
                    isDefault = fNegativePrefix.length() == (length+1) &&
                        fNegativePrefix.compare(getConstSymbol(DecimalFormatSymbols::kMinusSignSymbol)) == 0 &&
                        fNegativePrefix.compare(1, length, fPositivePrefix, 0, length) == 0;
                }
            }
            if (isDefault) {
                break; // Don't output default negative subpattern
            } else {
                if (localized) {
                    result += getConstSymbol(DecimalFormatSymbols::kPatternSeparatorSymbol);
                }
                else {
                    result.append((UChar)kPatternSeparator);
                }
            }
        } else {
            appendAffixPattern(result, fNegSuffixPattern, fNegativeSuffix, localized);
            if (fPadPosition == kPadAfterSuffix && ! padSpec.isEmpty()) {
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
    UParseError parseError;
    applyPattern(pattern, FALSE, parseError, status);
}

//------------------------------------------------------------------------------

void
DecimalFormat::applyPattern(const UnicodeString& pattern,
                            UParseError& parseError, 
                            UErrorCode& status)
{
    applyPattern(pattern, FALSE, parseError, status);
}
//------------------------------------------------------------------------------

void
DecimalFormat::applyLocalizedPattern(const UnicodeString& pattern, UErrorCode& status)
{
    UParseError parseError;
    applyPattern(pattern, TRUE,parseError,status);
}

//------------------------------------------------------------------------------

void
DecimalFormat::applyLocalizedPattern(const UnicodeString& pattern,
                                     UParseError& parseError,
                                     UErrorCode& status)
{
    applyPattern(pattern, TRUE,parseError,status);
}

//------------------------------------------------------------------------------

void
DecimalFormat::applyPattern(const UnicodeString& pattern,
                            UBool localized,
                            UParseError& parseError,
                            UErrorCode& status)
{
    if (U_FAILURE(status))
    {
        return;
    }
    // Clear error struct
    parseError.offset = -1;
    parseError.preContext[0] = parseError.postContext[0] = (UChar)0;

    // Set the significant pattern symbols
    UChar32 zeroDigit               = kPatternZeroDigit;
    UnicodeString groupingSeparator ((UChar)kPatternGroupingSeparator);
    UnicodeString decimalSeparator  ((UChar)kPatternDecimalSeparator);
    UnicodeString percent           ((UChar)kPatternPercent);
    UnicodeString perMill           ((UChar)kPatternPerMill);
    UnicodeString digit             ((UChar)kPatternDigit);
    UnicodeString separator         ((UChar)kPatternSeparator);
    UnicodeString exponent          ((UChar)kPatternExponent);
    UnicodeString plus              ((UChar)kPatternPlus);
    UnicodeString minus             ((UChar)kPatternMinus);
    UnicodeString padEscape         ((UChar)kPatternPadEscape);
    // Substitute with the localized symbols if necessary
    if (localized) {
        zeroDigit = getConstSymbol(DecimalFormatSymbols::kZeroDigitSymbol).char32At(0);
        groupingSeparator.  remove().append(getConstSymbol(DecimalFormatSymbols::kGroupingSeparatorSymbol));
        decimalSeparator.   remove().append(getConstSymbol(DecimalFormatSymbols::kDecimalSeparatorSymbol));
        percent.            remove().append(getConstSymbol(DecimalFormatSymbols::kPercentSymbol));
        perMill.            remove().append(getConstSymbol(DecimalFormatSymbols::kPerMillSymbol));
        digit.              remove().append(getConstSymbol(DecimalFormatSymbols::kDigitSymbol));
        separator.          remove().append(getConstSymbol(DecimalFormatSymbols::kPatternSeparatorSymbol));
        exponent.           remove().append(getConstSymbol(DecimalFormatSymbols::kExponentialSymbol));
        plus.               remove().append(getConstSymbol(DecimalFormatSymbols::kPlusSignSymbol));
        minus.              remove().append(getConstSymbol(DecimalFormatSymbols::kMinusSignSymbol));
        padEscape.          remove().append(getConstSymbol(DecimalFormatSymbols::kPadEscapeSymbol));
    }
    UChar nineDigit = (UChar)(zeroDigit + 9);
    int32_t digitLen = digit.length();
    int32_t groupSepLen = groupingSeparator.length();
    int32_t decimalSepLen = decimalSeparator.length();

    int32_t pos = 0;
    int32_t patLen = pattern.length();
    // Part 0 is the positive pattern.  Part 1, if present, is the negative
    // pattern.
    for (int32_t part=0; part<2 && pos<patLen; ++part) {
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
        UChar32 padChar;
        int32_t roundingPos = -1;
        DigitList roundingInc;
        int8_t expDigits = -1;
        UBool expSignAlways = FALSE;
        UBool isCurrency = FALSE;
        
        // The affix is either the prefix or the suffix.
        UnicodeString* affix = &prefix;
        
        int32_t start = pos;
        UBool isPartDone = FALSE;
        UChar32 ch;

        for (; !isPartDone && pos < patLen; pos += UTF_NEED_MULTIPLE_UCHAR(ch)) {
            // Todo: account for surrogate pairs
            ch = pattern.char32At(pos);
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
                if (pattern.compare(pos, digitLen, digit) == 0) {
                    if (zeroDigitCount > 0) {
                        ++digitRightCount;
                    } else {
                        ++digitLeftCount;
                    }
                    if (groupingCount >= 0 && decimalPos < 0) {
                        ++groupingCount;
                    }
                    pos += digitLen;
                } else if (ch >= zeroDigit && ch <= nineDigit) {
                    if (digitRightCount > 0) {
                        // Unexpected '0'
                        debug("Unexpected '0'")
                        status = U_UNEXPECTED_TOKEN;
                        syntaxError(pattern,pos,parseError);
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
                    pos++;
                } else if (pattern.compare(pos, groupSepLen, groupingSeparator) == 0) {
                    if (decimalPos >= 0) {
                        // Grouping separator after decimal
                        debug("Grouping separator after decimal")
                        status = U_UNEXPECTED_TOKEN;
                        syntaxError(pattern,pos,parseError);
                        return;
                    }
                    groupingCount2 = groupingCount;
                    groupingCount = 0;
                    pos += groupSepLen;
                } else if (pattern.compare(pos, decimalSepLen, decimalSeparator) == 0) {
                    if (decimalPos >= 0) {
                        // Multiple decimal separators
                        debug("Multiple decimal separators")
                        status = U_MULTIPLE_DECIMAL_SEPARATORS;
                        syntaxError(pattern,pos,parseError);
                        return;
                    }
                    // Intentionally incorporate the digitRightCount,
                    // even though it is illegal for this to be > 0
                    // at this point.  We check pattern syntax below.
                    decimalPos = digitLeftCount + zeroDigitCount + digitRightCount;
                    pos += decimalSepLen;
                } else {
                    if (pattern.compare(pos, exponent.length(), exponent) == 0) {
                        if (expDigits >= 0) {
                            // Multiple exponential symbols
                            debug("Multiple exponential symbols")
                            status = U_MULTIPLE_EXPONENTIAL_SYMBOLS;
                            syntaxError(pattern,pos,parseError);
                            return;
                        }
                        if (groupingCount >= 0) {
                            // Grouping separator in exponential pattern
                            debug("Grouping separator in exponential pattern")
                            status = U_MALFORMED_EXPONENTIAL_PATTERN;
                            syntaxError(pattern,pos,parseError);
                            return;
                        }
                        // Check for positive prefix
                        if ((pos+1) < patLen
                            && pattern.compare((int32_t) (pos+1), plus.length(), plus) == 0)
                        {
                            expSignAlways = TRUE;
                            pos += plus.length();
                        }
                        // Use lookahead to parse out the exponential part of the
                        // pattern, then jump into suffix subpart.
                        expDigits = 0;
                        pos += exponent.length() - 1;
                        while (++pos < patLen &&
                               pattern[(int32_t) pos] == zeroDigit)
                        {
                            ++expDigits;
                        }

                        if ((digitLeftCount + zeroDigitCount) < 1 ||
                            expDigits < 1) {
                            // Malformed exponential pattern
                            debug("Malformed exponential pattern")
                            status = U_MALFORMED_EXPONENTIAL_PATTERN;
                            syntaxError(pattern,pos,parseError);
                            return;
                        }
                    }
                    // Transition to suffix subpart
                    subpart = 2; // suffix subpart
                    affix = &suffix;
                    sub0Limit = pos;
                    continue;
                }
                break;
            case 1: // Prefix subpart
            case 2: // Suffix subpart
                // Process the prefix / suffix characters
                // Process unquoted characters seen in prefix or suffix
                // subpart.
                if (pattern.compare(pos, digitLen, digit) == 0) {
                    // Any of these characters implicitly begins the
                    // next subpart if we are in the prefix
                    if (subpart == 1) { // prefix subpart
                        subpart = 0; // pattern proper subpart
                        sub0Start = pos; // Reprocess this character
                        continue;
                    }
                    pos += digitLen;
                    // Fall through to append(ch)
                } else if (pattern.compare(pos, groupSepLen, groupingSeparator) == 0) {
                    // Any of these characters implicitly begins the
                    // next subpart if we are in the prefix
                    if (subpart == 1) { // prefix subpart
                        subpart = 0; // pattern proper subpart
                        sub0Start = pos; // Reprocess this character
                        continue;
                    }
                    pos += groupSepLen;
                    // Fall through to append(ch)
                } else if (pattern.compare(pos, decimalSepLen, decimalSeparator) == 0) {
                    // Any of these characters implicitly begins the
                    // next subpart if we are in the prefix
                    if (subpart == 1) { // prefix subpart
                        subpart = 0; // pattern proper subpart
                        sub0Start = pos; // Reprocess this character
                        continue;
                    }
                    pos += decimalSepLen;
                    // Fall through to append(ch)
                } else if (ch >= zeroDigit && ch <= nineDigit) {
                    // Any of these characters implicitly begins the
                    // next subpart if we are in the prefix
                    if (subpart == 1) { // prefix subpart
                        subpart = 0; // pattern proper subpart
                        sub0Start = pos; // Reprocess this character
                        continue;
                    }
                    pos++;
                    // Fall through to append(ch)
                } else if (ch == kCurrencySign) {
                    // Use lookahead to determine if the currency sign is
                    // doubled or not.
                    pos++;
                    affix->append(kQuote); // Encode currency
                    if (pos < pattern.length() && pattern[pos] == kCurrencySign)
                    {
                        affix->append(kCurrencySign);
                        ++pos; // Skip over the doubled character
                    }
                    isCurrency = TRUE;
                    // Fall through to append(ch)
                } else if (ch == kQuote) {
                    // A quote outside quotes indicates either the opening
                    // quote or two quotes, which is a quote literal.  That is,
                    // we have the first quote in 'do' or o''clock.
                    ++pos;
                    if (pos < pattern.length() && pattern[pos] == kQuote) {
                        affix->append(kQuote); // Encode quote
                        ++pos;
                        // Fall through to append(ch)
                    } else {
                        subpart += 2; // open quote
                        continue;
                    }
                } else if (pattern.compare(pos, separator.length(), separator) == 0) {
                    // Don't allow separators in the prefix, and don't allow
                    // separators in the second pattern (part == 1).
                    if (subpart == 1 || part == 1) {
                        // Unexpected separator
                        debug("Unexpected separator")
                        status = U_UNEXPECTED_TOKEN;
                        syntaxError(pattern,pos,parseError);
                        return;
                    }
                    sub2Limit = pos;
                    isPartDone = TRUE; // Go to next part
                    pos += separator.length();
                    break;
                } else if (pattern.compare(pos, percent.length(), percent) == 0) {
                    // Next handle characters which are appended directly.
                    if (multiplier != 1) {
                        // Too many percent/perMill characters
                        debug("Too many percent characters")
                        status = U_MULTIPLE_PERCENT_SYMBOLS;
                        syntaxError(pattern,pos,parseError);
                        return;
                    }
                    affix->append(kQuote); // Encode percent/perMill
                    multiplier = 100;
                    ch = kPatternPercent; // Use unlocalized pattern char
                    pos += percent.length();
                    // Fall through to append(ch)
                } else if (pattern.compare(pos, perMill.length(), perMill) == 0) {
                    // Next handle characters which are appended directly.
                    if (multiplier != 1) {
                        // Too many percent/perMill characters
                        debug("Too many perMill characters")
                        status = U_MULTIPLE_PERMILL_SYMBOLS;
                        syntaxError(pattern,pos,parseError);
                        return;
                    }
                    affix->append(kQuote); // Encode percent/perMill
                    multiplier = 1000;
                    ch = kPatternPerMill; // Use unlocalized pattern char
                    pos += perMill.length();
                    // Fall through to append(ch)
                } else if (pattern.compare(pos, padEscape.length(), padEscape) == 0) {
                    if (padPos >= 0 ||               // Multiple pad specifiers
                        (pos+1) == pattern.length()) { // Nothing after padEscape
                        debug("Multiple pad specifiers")
                        status = U_MULTIPLE_PAD_SPECIFIERS;
                        syntaxError(pattern,pos,parseError);
                        return;
                    }
                    padPos = pos;
                    pos += padEscape.length();
                    padChar = pattern.char32At(pos);
                    pos += U16_LENGTH(padChar);
                    continue;
                } else if (pattern.compare(pos, minus.length(), minus) == 0) {
                    affix->append(kQuote); // Encode minus
                    ch = kPatternMinus;
                    pos += minus.length();
                    // Fall through to append(ch)
                } else if (pattern.compare(pos, plus.length(), plus) == 0) {
                    affix->append(kQuote); // Encode plus
                    ch = kPatternPlus;
                    pos += plus.length();
                    // Fall through to append(ch)
                } else {
                    pos++;
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
                pos++;
                if (ch == kQuote) {
                    if (pos < pattern.length() && pattern[pos] == kQuote) {
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
            if (n == 0)
                ++n; // Handle ".###"
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
            subpart > 2)
        { // subpart > 2 == unmatched quote
            debug("Syntax error")
            status = U_PATTERN_SYNTAX_ERROR;
            syntaxError(pattern,pos,parseError);
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
                status = U_ILLEGAL_PAD_POSITION;
                syntaxError(pattern,pos,parseError);
                return;
            }
        }

        if (part == 0) {
            delete fPosPrefixPattern;
            delete fPosSuffixPattern;
            delete fNegPrefixPattern;
            delete fNegSuffixPattern;
            fPosPrefixPattern = new UnicodeString(prefix);
            /* test for NULL */
            if (fPosPrefixPattern == 0) {
                status = U_MEMORY_ALLOCATION_ERROR;
                return;
            }
            fPosSuffixPattern = new UnicodeString(suffix);
            /* test for NULL */
            if (fPosSuffixPattern == 0) {
                status = U_MEMORY_ALLOCATION_ERROR;
                delete fPosPrefixPattern;
                return;
            }
            fNegPrefixPattern = 0;
            fNegSuffixPattern = 0;

            fUseExponentialNotation = (expDigits >= 0);
            if (fUseExponentialNotation) {
                fMinExponentDigits = expDigits;
            }
            fExponentSignAlwaysShown = expSignAlways;
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
                    /* test for NULL */
                    if (fRoundingIncrement == 0) {
                        status = U_MEMORY_ALLOCATION_ERROR;
                        delete fPosPrefixPattern;
                        delete fPosSuffixPattern;
                        return;
                    }
                }
                fRoundingDouble = fRoundingIncrement->getDouble();
                fRoundingMode = kRoundHalfEven;
            } else {
                setRoundingIncrement(0.0);
            }
        } else {
            fNegPrefixPattern = new UnicodeString(prefix);
            /* test for NULL */
            if (fNegPrefixPattern == 0) {
                status = U_MEMORY_ALLOCATION_ERROR;
                return;
            }
            fNegSuffixPattern = new UnicodeString(suffix);
            /* test for NULL */
            if (fNegSuffixPattern == 0) {
                delete fNegPrefixPattern;
                status = U_MEMORY_ALLOCATION_ERROR;
                return;
            }
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
            /* test for NULL */
            if (fPosPrefixPattern == 0) {
                status = U_MEMORY_ALLOCATION_ERROR;
                return;
            }
        }
        if (fPosSuffixPattern != NULL) {
            fPosSuffixPattern->remove();
        } else {
            fPosSuffixPattern = new UnicodeString();
            /* test for NULL */
            if (fPosSuffixPattern == 0) {
                delete fPosPrefixPattern;
                status = U_MEMORY_ALLOCATION_ERROR;
                return;
            }
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
            /* test for NULL */
            if (fNegPrefixPattern == 0) {
                status = U_MEMORY_ALLOCATION_ERROR;
                return;
            }
        } else {
            fNegPrefixPattern->remove();
        }
        fNegPrefixPattern->append(kQuote).append(kPatternMinus)
            .append(*fPosPrefixPattern);
    }
#ifdef FMT_DEBUG
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

/**
 * Sets the <tt>Currency</tt> object used to display currency
 * amounts.  This takes effect immediately, if this format is a
 * currency format.  If this format is not a currency format, then
 * the currency object is used if and when this object becomes a
 * currency format through the application of a new pattern.
 * @param theCurrency new currency object to use.  Must not be
 * null.
 * @since ICU 2.2
 */
void DecimalFormat::setCurrency(const UChar* theCurrency) {
    // If we are a currency format, then modify our affixes to
    // encode the currency symbol for the given currency in our
    // locale, and adjust the decimal digits and rounding for the
    // given currency.

    NumberFormat::setCurrency(theCurrency);

    if (fIsCurrencyFormat) {
        if (theCurrency && *theCurrency) {
            setRoundingIncrement(ucurr_getRoundingIncrement(theCurrency));
            
            int32_t d = ucurr_getDefaultFractionDigits(theCurrency);
            setMinimumFractionDigits(d);
            setMaximumFractionDigits(d);
        }

        expandAffixes();
    }
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

//eof
