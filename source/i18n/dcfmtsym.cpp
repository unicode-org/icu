/*
*******************************************************************************
* Copyright (C) 1997-2004, International Business Machines Corporation and    *
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
*   07/20/98    stephen     Slightly modified initialization of monetarySeparator
********************************************************************************
*/
 
#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/dcfmtsym.h"
#include "unicode/resbund.h"
#include "unicode/decimfmt.h"
#include "unicode/ucurr.h"
#include "unicode/choicfmt.h"
#include "ucurrimp.h"
#include "cstring.h"
#include "locbased.h"

// *****************************************************************************
// class DecimalFormatSymbols
// *****************************************************************************
 
U_NAMESPACE_BEGIN

UOBJECT_DEFINE_RTTI_IMPLEMENTATION(DecimalFormatSymbols)

static const char gNumberElements[] = "NumberElements";

static const UChar INTL_CURRENCY_SYMBOL_STR[] = {0xa4, 0xa4, 0};

// -------------------------------------
// Initializes this with the decimal format symbols in the default locale.
 
DecimalFormatSymbols::DecimalFormatSymbols(UErrorCode& status)
    : UObject(),
    locale()
{
    initialize(locale, status, TRUE);
}
 
// -------------------------------------
// Initializes this with the decimal format symbols in the desired locale.
 
DecimalFormatSymbols::DecimalFormatSymbols(const Locale& loc, UErrorCode& status)
    : UObject(),
    locale(loc)
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
    : UObject(source)
{
    *this = source;
}

// -------------------------------------
// assignment operator

DecimalFormatSymbols&
DecimalFormatSymbols::operator=(const DecimalFormatSymbols& rhs)
{
    if (this != &rhs) {
        for(int32_t i = 0; i < (int32_t)kFormatSymbolCount; ++i) {
            // fastCopyFrom is safe, see docs on fSymbols
            fSymbols[(ENumberFormatSymbol)i].fastCopyFrom(rhs.fSymbols[(ENumberFormatSymbol)i]);
        }
        locale = rhs.locale;
        uprv_strcpy(validLocale, rhs.validLocale);
        uprv_strcpy(actualLocale, rhs.actualLocale);
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
    for(int32_t i = 0; i < (int32_t)kFormatSymbolCount; ++i) {
        if(fSymbols[(ENumberFormatSymbol)i] != that.fSymbols[(ENumberFormatSymbol)i]) {
            return FALSE;
        }
    }
    return locale == that.locale &&
        uprv_strcmp(validLocale, that.validLocale) == 0 &&
        uprv_strcmp(actualLocale, that.actualLocale) == 0;
}
 
// -------------------------------------
 
void
DecimalFormatSymbols::initialize(const Locale& loc, UErrorCode& status,
                                 UBool useLastResortData)
{
    *validLocale = *actualLocale = 0;

    if (U_FAILURE(status))
        return;

    const char* locStr = loc.getName();
    UResourceBundle *resource = ures_open((char *)0, locStr, &status);
    UResourceBundle *numberElementsRes = ures_getByKey(resource, gNumberElements, resource, &status);
    if (U_FAILURE(status))
    {
        // Initializes with last resort data if necessary.
        if (useLastResortData)
        {
            status = U_USING_FALLBACK_WARNING;
            initialize();
        }
    }
    else {
        // Gets the number element array.
        int32_t numberElementsLength = ures_getSize(numberElementsRes);

        if (numberElementsLength > (int32_t)kFormatSymbolCount) {
            /* Warning: Invalid format. Array too large. */
            numberElementsLength = (int32_t)kFormatSymbolCount;
        }
        // If the array size is too small, something is wrong with the resource
        // bundle, returns the failure error code.
        if (numberElementsLength < 11 || U_FAILURE(status)) {
            status = U_INVALID_FORMAT_ERROR;
        }
        else {
            UnicodeString numberElements[kFormatSymbolCount];
            int32_t i = 0;
            for(i = 0; i<numberElementsLength; i++) {
                int32_t len = 0;
                const UChar *resUChars = ures_getStringByIndex(numberElementsRes, i, &len, &status);
                numberElements[i].setTo(TRUE, resUChars, len); /* This setTo does aliasing */
            }

            if (U_SUCCESS(status)) {
                initialize(numberElements, numberElementsLength);

                // Obtain currency data from the currency API.  This is strictly
                // for backward compatibility; we don't use DecimalFormatSymbols
                // for currency data anymore.
                UErrorCode internalStatus = U_ZERO_ERROR; // don't propagate failures out
                UChar curriso[4];
                ucurr_forLocale(locStr, curriso, 4, &internalStatus);

                // Reuse numberElements[0] as a temporary buffer
                uprv_getStaticCurrencyName(curriso, locStr, numberElements[0], internalStatus);
                if (U_SUCCESS(internalStatus)) {
                    fSymbols[kIntlCurrencySymbol] = curriso;
                    fSymbols[kCurrencySymbol] = numberElements[0];
                }
                /* else use the default values. */
            }

            U_LOCALE_BASED(locBased, *this);
            locBased.setLocaleIDs(ures_getLocaleByType(numberElementsRes,
                                       ULOC_VALID_LOCALE, &status),
                                  ures_getLocaleByType(numberElementsRes,
                                       ULOC_ACTUAL_LOCALE, &status));
        }
    }
    ures_close(numberElementsRes);
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
    fSymbols[kExponentialSymbol].fastCopyFrom(numberElements[7]);
    fSymbols[kPerMillSymbol].fastCopyFrom(numberElements[8]);
    fSymbols[kPadEscapeSymbol] = (UChar)0x002a; // TODO: '*' Hard coded for now; get from resource later
    fSymbols[kInfinitySymbol].fastCopyFrom(numberElements[9]);
    fSymbols[kNaNSymbol].fastCopyFrom(numberElements[10]);

    // If there is a currency decimal, use it.
    fSymbols[kMonetarySeparatorSymbol].fastCopyFrom(numberElements[numberElementsLength >= 12 ? 11 : 0]);
    if (numberElementsLength >= 13) {
        fSymbols[kPlusSignSymbol].fastCopyFrom(numberElements[12]);
    }
    else {
        /* This locale really needs to be updated. This locale is out of date. */
        fSymbols[kPlusSignSymbol] = (UChar)0x002B;  /* + */
    }

    // Default values until it's set later on.
    fSymbols[kCurrencySymbol] = (UChar)0xa4;            // 'OX' currency symbol
    fSymbols[kIntlCurrencySymbol] = INTL_CURRENCY_SYMBOL_STR;
    // TODO: read from locale data, if this makes it into CLDR
    fSymbols[kSignificantDigitSymbol] = (UChar)0x0040;  // '@' significant digit
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
    fSymbols[kPlusSignSymbol] = (UChar)0x002b;          // '+' plus sign
    fSymbols[kMinusSignSymbol] = (UChar)0x2d;           // '-' minus sign
    fSymbols[kCurrencySymbol] = (UChar)0xa4;            // 'OX' currency symbol
    fSymbols[kIntlCurrencySymbol] = INTL_CURRENCY_SYMBOL_STR;
    fSymbols[kMonetarySeparatorSymbol] = (UChar)0x2e;   // '.' monetary decimal separator
    fSymbols[kExponentialSymbol] = (UChar)0x45;         // 'E' exponential
    fSymbols[kPerMillSymbol] = (UChar)0x2030;           // '%o' per mill
    fSymbols[kPadEscapeSymbol] = (UChar)0x2a;           // '*' pad escape symbol
    fSymbols[kInfinitySymbol] = (UChar)0x221e;          // 'oo' infinite
    fSymbols[kNaNSymbol] = (UChar)0xfffd;               // SUB NaN
    fSymbols[kSignificantDigitSymbol] = (UChar)0x0040;  // '@' significant digit
}

Locale 
DecimalFormatSymbols::getLocale(ULocDataLocaleType type, UErrorCode& status) const {
    U_LOCALE_BASED(locBased, *this);
    return locBased.getLocale(type, status);
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

//eof
