/*
*******************************************************************************
* Copyright (C) 1997-2003, International Business Machines Corporation and    *
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
 
#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/dcfmtsym.h"
#include "unicode/resbund.h"
#include "unicode/decimfmt.h"
#include "unicode/ucurr.h"
#include "unicode/choicfmt.h"

// *****************************************************************************
// class DecimalFormatSymbols
// *****************************************************************************
 
U_NAMESPACE_BEGIN

const char DecimalFormatSymbols::fgClassID=0;

const char DecimalFormatSymbols::fgNumberElements[] = "NumberElements";

static const UChar INTL_CURRENCY_SYMBOL_STR[] = {0xa4, 0xa4, 0};

// -------------------------------------
// Initializes this with the decimal format symbols in the default locale.
 
DecimalFormatSymbols::DecimalFormatSymbols(UErrorCode& status)
    : UObject()
{
    initialize(Locale::getDefault(), status, TRUE);
}
 
// -------------------------------------
// Initializes this with the decimal format symbols in the desired locale.
 
DecimalFormatSymbols::DecimalFormatSymbols(const Locale& loc, UErrorCode& status)
    : UObject()
{
    initialize(loc, status);
}
 
// -------------------------------------

DecimalFormatSymbols::~DecimalFormatSymbols()
{
}

// -------------------------------------
// copy constructor

DecimalFormatSymbols::DecimalFormatSymbols(const DecimalFormatSymbols &source)
    : UObject(source)
{
    int i;
    for(i = 0; i < (int)kFormatSymbolCount; ++i) {
        // fastCopyFrom is safe, see docs on fSymbols
        fSymbols[(ENumberFormatSymbol)i].fastCopyFrom(source.fSymbols[(ENumberFormatSymbol)i]);
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
            // fastCopyFrom is safe, see docs on fSymbols
            fSymbols[(ENumberFormatSymbol)i].fastCopyFrom(rhs.fSymbols[(ENumberFormatSymbol)i]);
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
DecimalFormatSymbols::initialize(const Locale& loc, UErrorCode& status,
                                 UBool useLastResortData)
{
    if (U_FAILURE(status)) return;

    this->locale = loc;

    ResourceBundle resource((char *)0, loc, status);
    if (U_FAILURE(status))
    {
        // Initializes with last resort data if necessary.
        if (useLastResortData)
        {
            status = U_USING_FALLBACK_WARNING;
            initialize();
        }
        return;
    }

    // Gets the number element array.
    int32_t i = 0;
    ResourceBundle numberElementsRes = resource.get(fgNumberElements, status);
    int32_t numberElementsLength = numberElementsRes.getSize();

    // If the array size is too small, something is wrong with the resource
    // bundle, returns the failure error code.
    if (numberElementsLength < 11) {
        status = U_INVALID_FORMAT_ERROR;
        return;
    }

    UnicodeString numberElements[kFormatSymbolCount];
    for(i = 0; i<numberElementsLength; i++) {
        numberElements[i].fastCopyFrom(numberElementsRes.getStringEx(i, status));
    }

    if (U_FAILURE(status)) {
        return;
    }

    initialize(numberElements, numberElementsLength);


    // Obtain currency data from the currency API.  This is strictly
    // for backward compatibility; we don't use DecimalFormatSymbols
    // for currency data anymore.
    UErrorCode ec = U_ZERO_ERROR; // don't propagate failures out
    const char* l = loc.getName();
    const UChar* curriso = ucurr_forLocale(l, &ec);
    UBool isChoiceFormat;
    int32_t len;
    const UChar* currname = ucurr_getName(curriso, l, UCURR_SYMBOL_NAME,
                                          &isChoiceFormat, &len, &ec);
    if (U_SUCCESS(ec)) {
        fSymbols[kIntlCurrencySymbol] = curriso;

        // If this is a ChoiceFormat currency, then format an
        // arbitrary value; pick something != 1; more common.
        fSymbols[kCurrencySymbol].truncate(0);
        if (isChoiceFormat) {
            ChoiceFormat f(currname, ec);
            if (U_SUCCESS(ec)) {
                f.format(2.0, fSymbols[kCurrencySymbol]);
            } else {
                fSymbols[kCurrencySymbol] = fSymbols[kIntlCurrencySymbol];
            }
        } else {
            fSymbols[kCurrencySymbol] = UnicodeString(currname);
        }
    }
    /* else use the default values. */
}

// Initializes the DecimalFormatSymbol instance with the data obtained
// from ResourceBundle in the desired locale.

void
DecimalFormatSymbols::initialize(const UnicodeString* numberElements, int32_t numberElementsLength)
{
    fSymbols[kDecimalSeparatorSymbol].fastCopyFrom(numberElements[0]);
    fSymbols[kGroupingSeparatorSymbol].fastCopyFrom(numberElements[1]);
    fSymbols[kPatternSeparatorSymbol].fastCopyFrom(numberElements[2]);
    fSymbols[kPercentSymbol].fastCopyFrom(numberElements[3]);
    fSymbols[kZeroDigitSymbol].fastCopyFrom(numberElements[4]);
    fSymbols[kDigitSymbol].fastCopyFrom(numberElements[5]);
    fSymbols[kMinusSignSymbol].fastCopyFrom(numberElements[6]);
    fSymbols[kPlusSignSymbol] = (UChar)0x002b; // '+' Hard coded for now; get from resource later
    fSymbols[kExponentialSymbol].fastCopyFrom(numberElements[7]);
    fSymbols[kPerMillSymbol].fastCopyFrom(numberElements[8]);
    fSymbols[kPadEscapeSymbol] = (UChar)0x002a; // '*' Hard coded for now; get from resource later
    fSymbols[kInfinitySymbol].fastCopyFrom(numberElements[9]);
    fSymbols[kNaNSymbol].fastCopyFrom(numberElements[10]);

    // If there is a currency decimal, use it.
    fSymbols[kMonetarySeparatorSymbol].fastCopyFrom(numberElements[numberElementsLength >= 12 ? 11 : 0]);

    // Default values until it's set later on.
    fSymbols[kCurrencySymbol] = (UChar)0xa4;            // 'OX' currency symbol
    fSymbols[kIntlCurrencySymbol] = INTL_CURRENCY_SYMBOL_STR;
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
    fSymbols[kIntlCurrencySymbol] = INTL_CURRENCY_SYMBOL_STR;
    fSymbols[kMonetarySeparatorSymbol] = (UChar)0x2e;   // '.' monetary decimal separator
    fSymbols[kExponentialSymbol] = (UChar)0x45;         // 'E' exponential
    fSymbols[kPerMillSymbol] = (UChar)0x2030;           // '%o' per mill
    fSymbols[kPadEscapeSymbol] = (UChar)0x2a;           // '*' pad escape symbol
    fSymbols[kInfinitySymbol] = (UChar)0x221e;          // 'oo' infinite
    fSymbols[kNaNSymbol] = (UChar)0xfffd;               // SUB NaN
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

//eof
