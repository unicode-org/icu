/*
**********************************************************************
* Copyright (c) 2002, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* $Source: /xsrl/Nsvn/icu/icu/source/i18n/Attic/currency.cpp,v $ 
* $Date: 2002/05/13 19:32:16 $ 
* $Revision: 1.1 $
**********************************************************************
*/
#include "unicode/currency.h"
#include "cstring.h"
#include "unicode/locid.h"
#include "unicode/resbund.h"

U_NAMESPACE_BEGIN

/**
 * TEMPORARY data structure.
 */
struct UCurrencyData {
    char*   code;
    int32_t fractionDigits;
    double  rounding;
};

/**
 * TEMPORARY Static data block giving currency fraction digits and
 * rounding increments.  The first entry with a NULL code gives
 * the default values for currencies not listed here.  The last
 * entry with a NULL code marks the end.
 */
static UCurrencyData DATA[] = {
    // TODO Temporary implementation; Redo this
    // Code, Fraction digits, Rounding increment
    { NULL , 2, 0 }, // Default
    { "BYB", 0, 0 },
    { "CHF", 2, 0.25 },
    { "ESP", 0, 0 },
    { "IQD", 3, 0 },
    { "ITL", 0, 0 },
    { "JOD", 3, 0 },
    { "JPY", 0, 0 },
    { "KWD", 3, 0 },
    { "LUF", 0, 0 },
    { "LYD", 3, 0 },
    { "PTE", 0, 0 },
    { "PYG", 0, 0 },
    { "TND", 3, 0 },
    { "TRL", 0, 0 },
    { NULL , 0, 0 }, // END
};

/**
 * TEMPORARY Internal function to look up currency data.
 */
static int32_t
_findData(const UnicodeString& currency) {
    // TODO Temporary implementation; Redo this

    // Convert currency code to char*
    char isoCode[4];
    currency.extract(0, 3, isoCode, "");

    // Start from element 1
    for (int32_t i=1; DATA[i].code != NULL; ++i) {
        int32_t c = uprv_strcmp(DATA[i].code, isoCode);
        if (c == 0) {
            return i;
        } else if (c > 0) {
            break;
        }
    }
    return 0; // Return default entry
}

/**
 * Returns a currency object for the default currency in the given
 * locale.
 */
UnicodeString
UCurrency::forLocale(const Locale& locale,
                     UErrorCode& ec) {
    // Look up the CurrencyElements resource for this locale.
    // It contains: [0] = currency symbol, e.g. "$";
    // [1] = intl. currency symbol, e.g. "USD";
    // [2] = monetary decimal separator, e.g. ".".
    if (U_SUCCESS(ec)) {
        ResourceBundle rb((char*)0, locale, ec);
        return rb.get("CurrencyElements", ec).getStringEx(1, ec);
    }
    return "";
}

/**
 * Returns the display string for this currency object in the
 * given locale.  For example, the display string for the USD
 * currency object in the en_US locale is "$".
 */
UnicodeString
UCurrency::getSymbol(const UnicodeString& currency,
                     const Locale& locale) {
    // Look up the Currencies resource for the given locale.  The
    // Currencies locale looks like this in the original C
    // resource file:
    //|en {
    //|  Currencies { 
    //|    USD { "$" }
    //|    CHF { "sFr" }
    //|    //...
    //|  }
    //|}
    UErrorCode ec = U_ZERO_ERROR;

    // Convert currency code to char*
    char isoCode[4];
    currency.extract(0, 3, isoCode, "");

    ResourceBundle rb((char*)0, locale, ec);
    UnicodeString result = rb.get("Currencies", ec).getStringEx(isoCode, ec);
    if (U_SUCCESS(ec) && result.length() > 0) {
        return result;
    }

    // Since the Currencies resource is not fully populated yet,
    // check to see if we can find what we want in the CurrencyElements
    // resource.
    ec = U_ZERO_ERROR;
    ResourceBundle ce = rb.get("CurrencyElements", ec);
    if (ce.getStringEx(1, ec) == currency) {
        return ce.getStringEx((int32_t)0, ec);
    }

    // If we fail to find a match, use the full ISO code
    return currency;
}

/**
 * Returns the number of the number of fraction digits that should
 * be displayed for this currency.
 * @return a non-negative number of fraction digits to be
 * displayed
 */
int32_t
UCurrency::getDefaultFractionDigits(const UnicodeString& currency) {
    // TODO Temporary implementation; Redo this
    return DATA[_findData(currency)].fractionDigits;
}

/**
 * Returns the rounding increment for this currency, or 0.0 if no
 * rounding is done by this currency.
 * @return the non-negative rounding increment, or 0.0 if none
 */
double
UCurrency::getRoundingIncrement(const UnicodeString& currency) {
    // TODO Temporary implementation; Redo this
    return DATA[_findData(currency)].rounding;
}

U_NAMESPACE_END

//eof
