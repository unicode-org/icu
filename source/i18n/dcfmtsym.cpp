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
 
#include "unicode/dcfmtsym.h"
#include "unicode/resbund.h"
#include "unicode/decimfmt.h"

// *****************************************************************************
// class DecimalFormatSymbols
// *****************************************************************************
 
const int32_t DecimalFormatSymbols::fgNumberElementsLength = 11;
const int32_t DecimalFormatSymbols::fgCurrencyElementsLength = 3;

/**
 * These are the tags we expect to see in normal resource bundle files associated
 * with a locale.
 */
const char *DecimalFormatSymbols::fgNumberElements="NumberElements";
const char *DecimalFormatSymbols::fgCurrencyElements="CurrencyElements";

// Because the C-compiler doesn't parse \u escape sequences, we encode the
// \u last resort strings as UChar arrays.
const UChar DecimalFormatSymbols::fgLastResortPermill[]      = { 0x2030, 0 };
const UChar DecimalFormatSymbols::fgLastResortInfinity[]     = { 0x221E, 0 };
const UChar DecimalFormatSymbols::fgLastResortNaN[]          = { 0xFFFD, 0 };
const UChar DecimalFormatSymbols::fgLastResortCurrency[]     = { 0x00A4, 0 };
const UChar DecimalFormatSymbols::fgLastResortIntlCurrency[] = { 0x00A4, 0x00A4, 0 };

const UnicodeString DecimalFormatSymbols::fgLastResortNumberElements[] =
{
    UNICODE_STRING(".", 1),    // decimal separator
    UnicodeString(),           // group (thousands) separator
    UNICODE_STRING(";", 1),    // pattern separator
    UNICODE_STRING("%", 1),    // percent sign
    UNICODE_STRING("0", 1),    // native 0 digit
    UNICODE_STRING("#", 1),    // pattern digit
    UNICODE_STRING("-", 1),    // minus sign
    UNICODE_STRING("E", 1),    // exponential
    DecimalFormatSymbols::fgLastResortPermill,  // per mill
    DecimalFormatSymbols::fgLastResortInfinity, // infinite
    DecimalFormatSymbols::fgLastResortNaN       // NaN
};

const UnicodeString DecimalFormatSymbols::fgLastResortCurrencyElements[] =
{
    DecimalFormatSymbols::fgLastResortCurrency,
    DecimalFormatSymbols::fgLastResortIntlCurrency,
    UNICODE_STRING(".", 1)     // monetary decimal separator
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

DecimalFormatSymbols::DecimalFormatSymbols(const DecimalFormatSymbols &source) {
    int i;
    for(i = 0; i < (int)ENumberFormatSymbol::kCount; ++i) {
        fSymbols[(ENumberFormatSymbol)i] = source.fSymbols[(ENumberFormatSymbol)i];
    }
}

// -------------------------------------
// assignment operator

DecimalFormatSymbols&
DecimalFormatSymbols::operator=(const DecimalFormatSymbols& rhs)
{
    if (this != &rhs)
    {
        int i;
        for(i = 0; i < (int)ENumberFormatSymbol::kCount; ++i) {
            fSymbols[(ENumberFormatSymbol)i] = rhs.fSymbols[(ENumberFormatSymbol)i];
        }
    }
    return *this;
}

// -------------------------------------

UBool
DecimalFormatSymbols::operator==(const DecimalFormatSymbols& that) const
{
    if (this == &that) {
        return TRUE;
    }

    int i;
    for(i = 0; i < (int)ENumberFormatSymbol::kCount; ++i) {
        if(fSymbols[(ENumberFormatSymbol)i] != that.fSymbols[(ENumberFormatSymbol)i]) {
            return FALSE;
        }
    }
    return TRUE;
}
 
// -------------------------------------
 
void
DecimalFormatSymbols::initialize(const Locale& locale, UErrorCode& status,
                                 UBool useLastResortData)
{
    if (U_FAILURE(status)) return;

    /*ResourceBundle resource(Locale::getDataDirectory(), locale, status);*/
    ResourceBundle resource(NULL, locale, status);
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

    // Gets the number element array.
    int32_t i = 0;
    ResourceBundle numberElementsRes = resource.get(fgNumberElements, status);
    int32_t numberElementsLength = numberElementsRes.getSize();
    UnicodeString* numberElements = new UnicodeString[numberElementsLength];
    for(i = 0; i<numberElementsLength; i++) {
        numberElements[i] = numberElementsRes.getStringEx(i, status);
    }

    // Gets the currency element array.
    ResourceBundle currencyElementsRes = resource.get(fgCurrencyElements, status);
    int32_t currencyElementsLength = currencyElementsRes.getSize();
    UnicodeString* currencyElements = new UnicodeString[currencyElementsLength];
    for(i = 0; i<currencyElementsLength; i++) {
        currencyElements[i] = currencyElementsRes.getStringEx(i, status);
    }

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
    fSymbols[ENumberFormatSymbol::kDecimalSeparator] = numberElements[0];
    fSymbols[ENumberFormatSymbol::kGroupingSeparator] = numberElements[1];
    fSymbols[ENumberFormatSymbol::kPatternSeparator] = numberElements[2];
    fSymbols[ENumberFormatSymbol::kPercent] = numberElements[3];
    fSymbols[ENumberFormatSymbol::kZeroDigit] = numberElements[4];
    fSymbols[ENumberFormatSymbol::kDigit] = numberElements[5];
    fSymbols[ENumberFormatSymbol::kMinusSign] = numberElements[6];
    fSymbols[ENumberFormatSymbol::kPlusSign] = (UChar)0x002b; // '+' Hard coded for now; get from resource later
    fSymbols[ENumberFormatSymbol::kCurrency] = currencyElements[0];
    fSymbols[ENumberFormatSymbol::kIntlCurrency] = currencyElements[1];

    // if the resource data specified the empty string as the monetary decimal
    // separator, that means we should just use the regular separator as the
    // monetary separator
    fSymbols[ENumberFormatSymbol::kMonetarySeparator] =
        currencyElements[2].length() > 0 ?
            currencyElements[2] :
            fSymbols[ENumberFormatSymbol::kDecimalSeparator];

    fSymbols[ENumberFormatSymbol::kExponential] = numberElements[7];
    fSymbols[ENumberFormatSymbol::kPermill] = numberElements[8];
    fSymbols[ENumberFormatSymbol::kPadEscape] = (UChar)0x002a; // '*' Hard coded for now; get from resource later
    fSymbols[ENumberFormatSymbol::kInfinity] = numberElements[9];
    fSymbols[ENumberFormatSymbol::kNaN] = numberElements[10];
}

//eof
