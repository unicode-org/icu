/*
**********************************************************************
* Copyright (c) 2002, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* $Source: /xsrl/Nsvn/icu/icu/source/i18n/Attic/currency.cpp,v $ 
* $Date: 2002/05/14 23:24:24 $ 
* $Revision: 1.3 $
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
    char   code[4];
    int32_t fractionDigits;
    double  rounding;
};

/**
 * TEMPORARY Static data block giving currency fraction digits and
 * rounding increments.  The first entry with a NULL code gives
 * the default values for currencies not listed here.  The last
 * entry with a NULL code marks the end.
 */
static const UCurrencyData DATA[] = {
    // TODO Temporary implementation; Redo this
    // Code, Fraction digits, Rounding increment
    { ""   , 2, 0.0 }, // Default
    { "BYB", 0, 0.0 },
    { "CHF", 2, 0.25 },
    { "ESP", 0, 0.0 },
    { "IQD", 3, 0.0 },
    { "ITL", 0, 0.0 },
    { "JOD", 3, 0.0 },
    { "JPY", 0, 0.0 },
    { "KWD", 3, 0.0 },
    { "LUF", 0, 0.0 },
    { "LYD", 3, 0.0 },
    { "PTE", 0, 0.0 },
    { "PYG", 0, 0.0 },
    { "TND", 3, 0.0 },
    { "TRL", 0, 0.0 },
};

#define ARRAY_SIZE(array) (sizeof array  / sizeof array[0])

/**
 * TEMPORARY Internal function to look up currency data.
 */
static int32_t
_findData(const char* currency) {
    // TODO Temporary implementation; Redo this

    // Start from element 1
    for (int32_t i=1; i < (int32_t)ARRAY_SIZE(DATA); ++i) {
        int32_t c = uprv_stricmp(DATA[i].code, currency);
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
void
UCurrency::forLocale(const Locale& locale,
                     char* result,
                     UErrorCode& ec) {
    // Look up the CurrencyElements resource for this locale.
    // It contains: [0] = currency symbol, e.g. "$";
    // [1] = intl. currency symbol, e.g. "USD";
    // [2] = monetary decimal separator, e.g. ".".
    if (U_SUCCESS(ec)) {
        ResourceBundle rb((char*)0, locale, ec);
        UnicodeString s = rb.get("CurrencyElements", ec).getStringEx(1, ec);
        s.extract(0, 3, result, "");
    } else {
        result[0] = 0;
    }
}

/**
 * Returns the display string for this currency object in the
 * given locale.  For example, the display string for the USD
 * currency object in the en_US locale is "$".
 */
UnicodeString
UCurrency::getSymbol(const char* currency,
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

    ResourceBundle rb((char*)0, locale, ec);
    UnicodeString s = rb.get("Currencies", ec).getStringEx(currency, ec);
    if (U_SUCCESS(ec) && s.length() > 0) {
        return s;
    }

    // Since the Currencies resource is not fully populated yet,
    // check to see if we can find what we want in the CurrencyElements
    // resource.
    ec = U_ZERO_ERROR;
    ResourceBundle ce = rb.get("CurrencyElements", ec);
    s = UnicodeString(currency, "");
    if (ce.getStringEx(1, ec) == s) {
        return ce.getStringEx((int32_t)0, ec);
    }

    // If we fail to find a match, use the full ISO code
    return s;
}

/**
 * Returns the number of the number of fraction digits that should
 * be displayed for this currency.
 * @return a non-negative number of fraction digits to be
 * displayed
 */
int32_t
UCurrency::getDefaultFractionDigits(const char* currency) {
    // TODO Temporary implementation; Redo this
    return DATA[_findData(currency)].fractionDigits;
}

/**
 * Returns the rounding increment for this currency, or 0.0 if no
 * rounding is done by this currency.
 * @return the non-negative rounding increment, or 0.0 if none
 */
double
UCurrency::getRoundingIncrement(const char* currency) {
    // TODO Temporary implementation; Redo this
    return DATA[_findData(currency)].rounding;
}

U_NAMESPACE_END

//eof
