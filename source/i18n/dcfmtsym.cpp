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
    for(i = 0; i < (int)kFormatSymbolCount; ++i) {
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
        for(i = 0; i < (int)kFormatSymbolCount; ++i) {
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
    for(i = 0; i < (int)kFormatSymbolCount; ++i) {
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

    ResourceBundle resource((char *)0, locale, status);
    if (U_FAILURE(status))
    {
        // Initializes with last resort data if necessary.
        if (useLastResortData)
        {
            status = U_USING_FALLBACK_ERROR;
            initialize();
        }
        return;
    }

    // Gets the number element array.
    int32_t i = 0;
    ResourceBundle numberElementsRes = resource.get("NumberElements", status);
    int32_t numberElementsLength = numberElementsRes.getSize();
    UnicodeString* numberElements = new UnicodeString[numberElementsLength];
    for(i = 0; i<numberElementsLength; i++) {
        numberElements[i] = numberElementsRes.getStringEx(i, status);
    }

    // Gets the currency element array.
    ResourceBundle currencyElementsRes = resource.get("CurrencyElements", status);
    int32_t currencyElementsLength = currencyElementsRes.getSize();
    UnicodeString* currencyElements = new UnicodeString[currencyElementsLength];
    for(i = 0; i<currencyElementsLength; i++) {
        currencyElements[i] = currencyElementsRes.getStringEx(i, status);
    }

    if (U_FAILURE(status)) return;

    // If the array size is too small, something is wrong with the resource
    // bundle, returns the failure error code.
    if (numberElementsLength < 11 ||
        currencyElementsLength < 3) {
        status = U_INVALID_FORMAT_ERROR;
        return;
    }

    initialize(numberElements, currencyElements);

    delete[] numberElements;
    delete[] currencyElements;
}

// Initializes the DecimalFormatSymbol instance with the data obtained
// from ResourceBundle in the desired locale.

void
DecimalFormatSymbols::initialize(const UnicodeString* numberElements, const UnicodeString* currencyElements)
{
    fSymbols[kDecimalSeparatorSymbol] = numberElements[0];
    fSymbols[kGroupingSeparatorSymbol] = numberElements[1];
    fSymbols[kPatternSeparatorSymbol] = numberElements[2];
    fSymbols[kPercentSymbol] = numberElements[3];
    fSymbols[kZeroDigitSymbol] = numberElements[4];
    fSymbols[kDigitSymbol] = numberElements[5];
    fSymbols[kMinusSignSymbol] = numberElements[6];
    fSymbols[kPlusSignSymbol] = (UChar)0x002b; // '+' Hard coded for now; get from resource later
    fSymbols[kCurrencySymbol] = currencyElements[0];
    fSymbols[kIntlCurrencySymbol] = currencyElements[1];

    // if the resource data specified the empty string as the monetary decimal
    // separator, that means we should just use the regular separator as the
    // monetary separator
    fSymbols[kMonetarySeparatorSymbol] =
        currencyElements[2].length() > 0 ?
            currencyElements[2] :
            fSymbols[kDecimalSeparatorSymbol];

    fSymbols[kExponentialSymbol] = numberElements[7];
    fSymbols[kPermillSymbol] = numberElements[8];
    fSymbols[kPadEscapeSymbol] = (UChar)0x002a; // '*' Hard coded for now; get from resource later
    fSymbols[kInfinitySymbol] = numberElements[9];
    fSymbols[kNaNSymbol] = numberElements[10];
}

// initialize with default values
void
DecimalFormatSymbols::initialize() {
    /*
     * These strings used to be in static arrays, but the HP/UX aCC compiler
     * cannot initialize a static array with class constructors.
     *  markus 2000may25
     */
    fSymbols[kDecimalSeparatorSymbol] = (UChar)0x2e;    // '.' decimal separator
    fSymbols[kGroupingSeparatorSymbol].remove();        //     group (thousands) separator
    fSymbols[kPatternSeparatorSymbol] = (UChar)0x3b;    // ';' pattern separator
    fSymbols[kPercentSymbol] = (UChar)0x25;             // '%' percent sign
    fSymbols[kZeroDigitSymbol] = (UChar)0x30;           // '0' native 0 digit
    fSymbols[kDigitSymbol] = (UChar)0x23;               // '#' pattern digit
    fSymbols[kMinusSignSymbol] = (UChar)0x2d;           // '-' minus sign
    fSymbols[kPlusSignSymbol] = (UChar)0x002b;          // '+' plus sign
    fSymbols[kCurrencySymbol] = (UChar)0xa4;            // 'OX' currency symbol
    (fSymbols[kIntlCurrencySymbol] = (UChar)0xa4).append((UChar)0xa4);
    fSymbols[kMonetarySeparatorSymbol] = (UChar)0x2e;   // '.' monetary decimal separator
    fSymbols[kExponentialSymbol] = (UChar)0x45;         // 'E' exponential
    fSymbols[kPermillSymbol] = (UChar)0x2030;           // '%o' per mill
    fSymbols[kPadEscapeSymbol] = (UChar)0x2a;           // '*' pad escape symbol
    fSymbols[kInfinitySymbol] = (UChar)0x221e;          // 'oo' infinite
    fSymbols[kNaNSymbol] = (UChar)0xfffd;               // SUB NaN
}

//eof
