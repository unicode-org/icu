/*
*******************************************************************************
* Copyright (C) 1997-1999, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* File NUMFMT.CPP
*
* Modification History:
*
*   Date        Name        Description
*   02/19/97    aliu        Converted from java.
*   03/18/97    clhuang     Implemented with C++ APIs.
*   04/17/97    aliu        Enlarged MAX_INTEGER_DIGITS to fully accomodate the
*                           largest double, by default.
*                           Changed DigitCount to int per code review.
*    07/20/98    stephen        Changed operator== to check for grouping
*                            Changed setMaxIntegerDigits per Java implementation.
*                            Changed setMinIntegerDigits per Java implementation.
*                            Changed setMinFractionDigits per Java implementation.
*                            Changed setMaxFractionDigits per Java implementation.
********************************************************************************
*/
 
#include "unicode/numfmt.h"
#include "unicode/locid.h"
#include "unicode/resbund.h"
#include "unicode/dcfmtsym.h"
#include "unicode/decimfmt.h"
#include <float.h>

// *****************************************************************************
// class NumberFormat
// *****************************************************************************
 
char NumberFormat::fgClassID = 0; // Value is irrelevant

// If the maximum base 10 exponent were 4, then the largest number would 
// be 99,999 which has 5 digits.
const int32_t NumberFormat::fgMaxIntegerDigits = DBL_MAX_10_EXP + 1; // Should be ~40 ? --srl
const int32_t NumberFormat::fgMinIntegerDigits = 127;

const int32_t NumberFormat::fgNumberPatternsCount = 3;

// If no number pattern can be located for a locale, this is the last
// resort.
const UnicodeString NumberFormat::fgLastResortNumberPatterns[] =
{
    UNICODE_STRING("#0.###;-#0.###", 14),   // decimal pattern
    UNICODE_STRING("$#0.00;($#0.00)", 15),  // currency pattern
    UNICODE_STRING("#0%", 3),               // percent pattern
	UNICODE_STRING("#E0", 3)                // scientific pattern
};

// -------------------------------------
// default constructor

NumberFormat::NumberFormat()
:   fGroupingUsed(TRUE),
    fMaxIntegerDigits(fgMaxIntegerDigits),
    fMinIntegerDigits(1),
    fMaxFractionDigits(3), // invariant, >= minFractionDigits
    fMinFractionDigits(0),
    fParseIntegerOnly(FALSE)
{
}

// -------------------------------------

NumberFormat::~NumberFormat()
{
}

// -------------------------------------
// copy constructor

NumberFormat::NumberFormat(const NumberFormat &source)
{
    *this = source;
}

// -------------------------------------
// assignment operator

NumberFormat&
NumberFormat::operator=(const NumberFormat& rhs)
{
    if (this != &rhs)
    {
        fGroupingUsed = rhs.fGroupingUsed;
        fMaxIntegerDigits = rhs.fMaxIntegerDigits;
        fMinIntegerDigits = rhs.fMinIntegerDigits;
        fMaxFractionDigits = rhs.fMaxFractionDigits;
        fMinFractionDigits = rhs.fMinFractionDigits;
        fParseIntegerOnly = rhs.fParseIntegerOnly;
    }
    return *this;
}

// -------------------------------------
 
UBool
NumberFormat::operator==(const Format& that) const
{
    NumberFormat* other = (NumberFormat*)&that;

    return ((this == &that) ||
            ((Format::operator==(that) &&
              getDynamicClassID()== that.getDynamicClassID() &&
              fMaxIntegerDigits == other->fMaxIntegerDigits &&
              fMinIntegerDigits == other->fMinIntegerDigits &&
              fMaxFractionDigits == other->fMaxFractionDigits &&
              fMinFractionDigits == other->fMinFractionDigits &&
              fGroupingUsed == other->fGroupingUsed &&
              fParseIntegerOnly == other->fParseIntegerOnly)));
}

// -------------------------------------
// Formats the number object and save the format
// result in the toAppendTo string buffer.
 
UnicodeString&
NumberFormat::format(const Formattable& obj,
                        UnicodeString& toAppendTo,
                        FieldPosition& pos,
                        UErrorCode& status) const
{
    if (U_FAILURE(status)) return toAppendTo;

    if (obj.getType() == Formattable::kDouble) {
        return format(obj.getDouble(), toAppendTo, pos);
    }
    else if (obj.getType() == Formattable::kLong) {
        return format(obj.getLong(), toAppendTo, pos);
    }
    // can't try to format a non-numeric object
    else {
        status = U_INVALID_FORMAT_ERROR;
        return toAppendTo; 
    }
}
  
// -------------------------------------
// Parses the string and save the result object as well
// as the final parsed position.

void
NumberFormat::parseObject(const UnicodeString& source,
                             Formattable& result,
                             ParsePosition& parse_pos) const
{
    parse(source, result, parse_pos);
}
 
// -------------------------------------
// Formats a double number and save the result in a string.
 
UnicodeString&
NumberFormat::format(double number, UnicodeString& toAppendTo) const
{
    FieldPosition pos(0);
    UErrorCode status = U_ZERO_ERROR;
    return format(Formattable(number), toAppendTo, pos, status);
}
 
// -------------------------------------
// Formats a long number and save the result in a string.
 
UnicodeString&
NumberFormat::format(int32_t number, UnicodeString& toAppendTo) const
{
    FieldPosition pos(0);
    UErrorCode status = U_ZERO_ERROR;
    return format(Formattable(number), toAppendTo, pos, status);
}
 
// -------------------------------------
// Parses the text and save the result object.  If the returned
// parse position is 0, that means the parsing failed, the status
// code needs to be set to failure.  Ignores the returned parse 
// position, otherwise.
 
void
NumberFormat::parse(const UnicodeString& text,
                        Formattable& result,
                        UErrorCode& status) const
{
    if (U_FAILURE(status)) return;

    ParsePosition parsePosition(0);
    parse(text, result, parsePosition);
    if (parsePosition.getIndex() == 0) {
        status = U_INVALID_FORMAT_ERROR;
    }
}
 
// -------------------------------------
// Sets to only parse integers.
 
void
NumberFormat::setParseIntegerOnly(UBool value)
{
    fParseIntegerOnly = value;
}
 
// -------------------------------------
// Create a number style NumberFormat instance with the default locale.
 
NumberFormat*
NumberFormat::createInstance(UErrorCode& status)
{
    return createInstance(Locale::getDefault(), kNumberStyle, status);
}
 
// -------------------------------------
// Create a number style NumberFormat instance with the inLocale locale.
 
NumberFormat*
NumberFormat::createInstance(const Locale& inLocale, UErrorCode& status)
{
    return createInstance(inLocale, kNumberStyle, status);
}
 
// -------------------------------------
// Create a currency style NumberFormat instance with the default locale.
 
NumberFormat*
NumberFormat::createCurrencyInstance(UErrorCode& status)
{
    return createInstance(Locale::getDefault(), kCurrencyStyle, status);
}
 
// -------------------------------------
// Create a currency style NumberFormat instance with the inLocale locale.
 
NumberFormat*
NumberFormat::createCurrencyInstance(const Locale& inLocale, UErrorCode& status)
{
    return createInstance(inLocale, kCurrencyStyle, status);
}
 
// -------------------------------------
// Create a percent style NumberFormat instance with the default locale.
 
NumberFormat*
NumberFormat::createPercentInstance(UErrorCode& status)
{
    return createInstance(Locale::getDefault(), kPercentStyle, status);
}
 
// -------------------------------------
// Create a percent style NumberFormat instance with the inLocale locale.
 
NumberFormat*
NumberFormat::createPercentInstance(const Locale& inLocale, UErrorCode& status)
{
    return createInstance(inLocale, kPercentStyle, status);
}
 
// -------------------------------------
// Create a scientific style NumberFormat instance with the default locale.
 
NumberFormat*
NumberFormat::createScientificInstance(UErrorCode& status)
{
    return createInstance(Locale::getDefault(), kScientificStyle, status);
}
 
// -------------------------------------
// Create a scientific style NumberFormat instance with the inLocale locale.
 
NumberFormat*
NumberFormat::createScientificInstance(const Locale& inLocale, UErrorCode& status)
{
    return createInstance(inLocale, kScientificStyle, status);
}
 
// -------------------------------------
 
const Locale*
NumberFormat::getAvailableLocales(int32_t& count)
{
    return Locale::getAvailableLocales(count);
}
 
// -------------------------------------
// Checks if the thousand/10 thousand grouping is used in the 
// NumberFormat instance.

UBool
NumberFormat::isGroupingUsed() const
{
    return fGroupingUsed;
}
 
// -------------------------------------
// Sets to use the thousand/10 thousand grouping in the 
// NumberFormat instance.
 
void
NumberFormat::setGroupingUsed(UBool newValue)
{
    fGroupingUsed = newValue;
}
 
// -------------------------------------
// Gets the maximum number of digits for the integral part for
// this NumberFormat instance.
 
int32_t NumberFormat::getMaximumIntegerDigits() const
{
    return fMaxIntegerDigits;
}
 
// -------------------------------------
// Sets the maximum number of digits for the integral part for
// this NumberFormat instance.
 
void
NumberFormat::setMaximumIntegerDigits(int32_t newValue)
{
    fMaxIntegerDigits = uprv_max(0, uprv_min(newValue, fgMaxIntegerDigits));
    if(fMinIntegerDigits > fMaxIntegerDigits)
        fMinIntegerDigits = fMaxIntegerDigits;
}
 
// -------------------------------------
// Gets the minimum number of digits for the integral part for
// this NumberFormat instance.
 
int32_t
NumberFormat::getMinimumIntegerDigits() const
{
    return fMinIntegerDigits;
}
 
// -------------------------------------
// Sets the minimum number of digits for the integral part for
// this NumberFormat instance.
 
void
NumberFormat::setMinimumIntegerDigits(int32_t newValue)
{
    fMinIntegerDigits = uprv_max(0, uprv_min(newValue, fgMinIntegerDigits));
    if(fMinIntegerDigits > fMaxIntegerDigits)
        fMaxIntegerDigits = fMinIntegerDigits;
}
 
// -------------------------------------
// Gets the maximum number of digits for the fractional part for
// this NumberFormat instance.
 
int32_t
NumberFormat::getMaximumFractionDigits() const
{
    return fMaxFractionDigits;
}
 
// -------------------------------------
// Sets the maximum number of digits for the fractional part for
// this NumberFormat instance.
 
void
NumberFormat::setMaximumFractionDigits(int32_t newValue)
{
    fMaxFractionDigits = uprv_max(0, uprv_min(newValue, fgMaxIntegerDigits));
    if(fMaxFractionDigits < fMinFractionDigits)
        fMinFractionDigits = fMaxFractionDigits;
}
 
// -------------------------------------
// Gets the minimum number of digits for the fractional part for
// this NumberFormat instance.
 
int32_t
NumberFormat::getMinimumFractionDigits() const
{
    return fMinFractionDigits;
}
 
// -------------------------------------
// Sets the minimum number of digits for the fractional part for
// this NumberFormat instance.
 
void
NumberFormat::setMinimumFractionDigits(int32_t newValue)
{
    fMinFractionDigits = uprv_max(0, uprv_min(newValue, fgMinIntegerDigits));
    if (fMaxFractionDigits < fMinFractionDigits)
        fMaxFractionDigits = fMinFractionDigits;
}
 
// -------------------------------------
// Creates the NumberFormat instance of the specified style (number, currency,
// or percent) for the desired locale.
 
NumberFormat*
NumberFormat::createInstance(const Locale& desiredLocale, 
                             EStyles style, 
                             UErrorCode& status)
{
    if (U_FAILURE(status)) return NULL;

    if (style < 0 || style >= kStyleCount) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }

    /*ResourceBundle resource(Locale::getDataDirectory(), desiredLocale, status);*/
    ResourceBundle resource(NULL, desiredLocale, status);

    if (U_FAILURE(status))
    {
        // We don't appear to have resource data available -- use the last-resort data
        status = U_USING_FALLBACK_ERROR;
        
        // Use the DecimalFormatSymbols constructor which uses last-resort data
        DecimalFormatSymbols* symbolsToAdopt = new DecimalFormatSymbols(status);
        if (U_FAILURE(status)) { delete symbolsToAdopt; return NULL; } // This should never happen

        // Creates a DecimalFormat instance with the last resort number patterns.
        NumberFormat* f = new DecimalFormat(fgLastResortNumberPatterns[style], symbolsToAdopt, status);
        if (U_FAILURE(status)) { delete f; f = NULL; }
        return f;
    }

    //int32_t patternCount=0;
    //const UnicodeString* numberPatterns = resource.getStringArray(DecimalFormat::fgNumberPatterns,
    //                                                              patternCount, status);

    ResourceBundle numberPatterns = resource.get(DecimalFormat::fgNumberPatterns, status);
    // If not all the styled patterns exists for the NumberFormat in this locale,
    // sets the status code to failure and returns nil.
    //if (patternCount < fgNumberPatternsCount) status = U_INVALID_FORMAT_ERROR;
    if (numberPatterns.getSize() < fgNumberPatternsCount) status = U_INVALID_FORMAT_ERROR;
    if (U_FAILURE(status)) return NULL;

    // If the requested style doesn't exist, use a last-resort style.
    // This is to support scientific styles before we have all the
    // resource data in place.
    //const UnicodeString& pattern = style < patternCount ?
    //    numberPatterns[style] : fgLastResortNumberPatterns[style];
    const UnicodeString& pattern = style < numberPatterns.getSize()?
        numberPatterns.getStringEx(style, status) : fgLastResortNumberPatterns[style];

    // Loads the decimal symbols of the desired locale.
    DecimalFormatSymbols* symbolsToAdopt = new DecimalFormatSymbols(desiredLocale, status);
    if (U_FAILURE(status)) { delete symbolsToAdopt; return NULL; }

    // Creates the specified decimal format style of the desired locale.
    NumberFormat* f = new DecimalFormat(pattern, symbolsToAdopt, status);
    if (U_FAILURE(status)) { delete f; f = NULL; }
    return f;
}

//eof
