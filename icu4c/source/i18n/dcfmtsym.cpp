/*
*******************************************************************************
* Copyright (C) 1997-1999, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* File DCFMTSYM.CPP
*
* Modification History:
*
*   Date        Name        Description
*   02/19/97    aliu        Converted from java.
*   03/18/97    clhuang     Implemented with C++ APIs.
*   03/27/97    helena      Updated to pass the simple test after code review.
*   08/26/97    aliu        Added currency/intl currency symbol support.
*    07/20/98    stephen        Slightly modified initialization of monetarySeparator
********************************************************************************
*/
 
#include "dcfmtsym.h"
#include "resbund.h"
#include "decimfmt.h"

// *****************************************************************************
// class DecimalFormatSymbols
// *****************************************************************************
 
const int32_t DecimalFormatSymbols::fgNumberElementsLength = 11;
const int32_t DecimalFormatSymbols::fgCurrencyElementsLength = 3;

/**
 * These are the tags we expect to see in normal resource bundle files associated
 * with a locale.
 */
const UnicodeString DecimalFormatSymbols::fgNumberElements("NumberElements");
const UnicodeString DecimalFormatSymbols::fgCurrencyElements("CurrencyElements");

// Because the C-compiler doesn't parse \u escape sequences, we encode the
// \u last resort strings as UChar arrays.
const UChar DecimalFormatSymbols::fgLastResortPerMill[]      = { 0x2030, 0 };
const UChar DecimalFormatSymbols::fgLastResortInfinity[]     = { 0x221E, 0 };
const UChar DecimalFormatSymbols::fgLastResortNaN[]          = { 0xFFFD, 0 };
const UChar DecimalFormatSymbols::fgLastResortCurrency[]     = { 0x00A4, 0 };
const UChar DecimalFormatSymbols::fgLastResortIntlCurrency[] = { 0x00A4, 0x00A4, 0 };

const UnicodeString DecimalFormatSymbols::fgLastResortNumberElements[] =
{
    ".",    // decimal separator
    "",     // group (thousands) separator
    ";",    // pattern separator
    "%",    // percent sign
    "0",    // native 0 digit
    "#",    // pattern digit
    "-",    // minus sign
    "E",    // exponential
    DecimalFormatSymbols::fgLastResortPerMill,  // per mill
    DecimalFormatSymbols::fgLastResortInfinity, // infinite
    DecimalFormatSymbols::fgLastResortNaN       // NaN
};

const UnicodeString DecimalFormatSymbols::fgLastResortCurrencyElements[] =
{
    DecimalFormatSymbols::fgLastResortCurrency,
    DecimalFormatSymbols::fgLastResortIntlCurrency,
    "."     // monetary decimal separator
};

// -------------------------------------
// Initializes this with the decimal format symbols in the default locale.
 
DecimalFormatSymbols::DecimalFormatSymbols(UErrorCode& status)
{
    initialize(Locale::getDefault(), status, TRUE);
}
 
// -------------------------------------
// Initializes this with the decimal format symbols in the desired locale.
 
DecimalFormatSymbols::DecimalFormatSymbols(const Locale& locale, UErrorCode& status)
{
    initialize(locale, status);
}
 
// -------------------------------------

DecimalFormatSymbols::~DecimalFormatSymbols()
{
}

// -------------------------------------
// copy constructor

DecimalFormatSymbols::DecimalFormatSymbols(const DecimalFormatSymbols &source)
    :   fZeroDigit(source.fZeroDigit),
        fGroupingSeparator(source.fGroupingSeparator),
        fDecimalSeparator(source.fDecimalSeparator),
        fPercent(source.fPercent),
        fPerMill(source.fPerMill),
        fDigit(source.fDigit),
        fPlusSign(source.fPlusSign),
        fMinusSign(source.fMinusSign),
        fExponential(source.fExponential),
        fPatternSeparator(source.fPatternSeparator),
        fInfinity(source.fInfinity),
        fNaN(source.fNaN),
        fCurrencySymbol(source.fCurrencySymbol),
        fIntlCurrencySymbol(source.fIntlCurrencySymbol),
        fMonetarySeparator(source.fMonetarySeparator),
        fPadEscape(source.fPadEscape)
{
}

// -------------------------------------
// assignment operator

DecimalFormatSymbols&
DecimalFormatSymbols::operator=(const DecimalFormatSymbols& rhs)
{
    if (this != &rhs)
    {
        fZeroDigit = rhs.fZeroDigit;
        fGroupingSeparator = rhs.fGroupingSeparator;
        fDecimalSeparator = rhs.fDecimalSeparator;
        fPercent = rhs.fPercent;
        fPerMill = rhs.fPerMill;
        fDigit = rhs.fDigit;
        fPlusSign = rhs.fPlusSign;
        fMinusSign = rhs.fMinusSign;
        fExponential = rhs.fExponential;
        fPatternSeparator = rhs.fPatternSeparator;
        fInfinity = rhs.fInfinity;
        fNaN = rhs.fNaN;
        fCurrencySymbol = rhs.fCurrencySymbol;
        fIntlCurrencySymbol = rhs.fIntlCurrencySymbol;
        fMonetarySeparator = rhs.fMonetarySeparator;
        fPadEscape = rhs.fPadEscape;
    }
    return *this;
}

// -------------------------------------

bool_t
DecimalFormatSymbols::operator==(const DecimalFormatSymbols& that) const
{
    if (this == &that) return TRUE;

    return (fZeroDigit == that.fZeroDigit &&
        fGroupingSeparator == that.fGroupingSeparator &&
        fDecimalSeparator == that.fDecimalSeparator &&
        fPercent == that.fPercent &&
        fPerMill == that.fPerMill &&
        fDigit == that.fDigit &&
        fPlusSign == that.fPlusSign &&
        fMinusSign == that.fMinusSign &&
        fExponential == that.fExponential &&
        fPatternSeparator == that.fPatternSeparator &&
        fInfinity == that.fInfinity &&
        fNaN == that.fNaN &&
        fCurrencySymbol == that.fCurrencySymbol &&
        fIntlCurrencySymbol == that.fIntlCurrencySymbol &&
        fMonetarySeparator == that.fMonetarySeparator &&
        fPadEscape == that.fPadEscape
        );
}
 
// -------------------------------------
 
void
DecimalFormatSymbols::initialize(const Locale& locale, UErrorCode& status,
                                 bool_t useLastResortData)
{
    if (U_FAILURE(status)) return;

    ResourceBundle resource(Locale::getDataDirectory(), locale, status);
    if (U_FAILURE(status))
    {
        // Initializes with last resort data if necessary.
        if (useLastResortData)
        {
            status = U_USING_FALLBACK_ERROR;
            initialize(fgLastResortNumberElements, fgLastResortCurrencyElements);
        }
        return;
    }

    int32_t numberElementsLength=0;
    // Gets the number element array.
    const UnicodeString* numberElements = resource.getStringArray(fgNumberElements, numberElementsLength, status);
    int32_t currencyElementsLength=0;
    // Gets the currency element array.
    const UnicodeString* currencyElements = resource.getStringArray(fgCurrencyElements, currencyElementsLength, status);
    if (U_FAILURE(status)) return;

    // If the array size is too small, something is wrong with the resource
    // bundle, returns the failure error code.
    if (numberElementsLength < fgNumberElementsLength ||
        currencyElementsLength < fgCurrencyElementsLength) {
        status = U_INVALID_FORMAT_ERROR;
        return;
    }

    initialize(numberElements, currencyElements);
}

// Initializes the DecimalFormatSymbol instance with the data obtained
// from ResourceBundle in the desired locale.

void
DecimalFormatSymbols::initialize(const UnicodeString* numberElements, const UnicodeString* currencyElements)
{
    fDecimalSeparator  = numberElements[0][(UTextOffset)0];
    fGroupingSeparator = numberElements[1][(UTextOffset)0];
    fPatternSeparator  = numberElements[2][(UTextOffset)0];
    fPercent           = numberElements[3][(UTextOffset)0];
    fZeroDigit         = numberElements[4][(UTextOffset)0];
    fDigit             = numberElements[5][(UTextOffset)0];
    fPlusSign          = 0x002B; // '+' Hard coded for now; get from resource later
    fMinusSign         = numberElements[6][(UTextOffset)0];
    fExponential       = numberElements[7][(UTextOffset)0];
    fPerMill           = numberElements[8][(UTextOffset)0];
    fInfinity          = numberElements[9];
    fNaN               = numberElements[10];
    fCurrencySymbol     = currencyElements[0];
    fIntlCurrencySymbol = currencyElements[1];
    fPadEscape         = 0x002A; // '*' Hard coded for now; get from resource later

    // if the resource data specified the empty string as the monetary decimal
    // separator, that means we should just use the regular separator as the
    // monetary separator
    if(currencyElements[2].size() == 0)
        fMonetarySeparator = fDecimalSeparator;
    else
        fMonetarySeparator = currencyElements[2][(UTextOffset)0];
}

//eof
