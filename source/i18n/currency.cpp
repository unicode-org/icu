/*
**********************************************************************
* Copyright (c) 2002, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
*/
#include "unicode/currency.h"
#include "cstring.h"
#include "unicode/locid.h"
#include "unicode/resbund.h"

//------------------------------------------------------------
// Constants

// Default currency meta data of last resort.  We try to use the
// defaults encoded in the meta data resource bundle.  If there is a
// configuration/build error and these are not available, we use these
// hard-coded defaults (which should be identical).
static const int32_t LAST_RESORT_DATA[] = { 2, 0 };

// POW10[i] = 10^i, i=0..MAX_POW10
static const int32_t POW10[] = { 1, 10, 100, 1000, 10000, 100000,
                                 1000000, 10000000, 100000000, 1000000000 };

static const int32_t MAX_POW10 = (sizeof(POW10)/sizeof(POW10[0])) - 1;

//------------------------------------------------------------
// Resource tags

// Tag for meta-data, in root.
static const char CURRENCY_META[] = "CurrencyMeta";

// Tag for default meta-data, in CURRENCY_META
static const char DEFAULT_META[] = "DEFAULT";

// Tag for legacy currency elements data
static const char CURRENCY_ELEMENTS[] = "CurrencyElements";

// Tag for localized display names (symbols) of currencies
static const char CURRENCIES[] = "Currencies";

//------------------------------------------------------------
// Code

/**
 * Internal function to look up currency data.  Result is an array of
 * two integers.  The first is the fraction digits.  The second is the
 * rounding increment, or 0 if none.  The rounding increment is in
 * units of 10^(-fraction_digits).
 */
static const int32_t*
_findData(const char* currency) {

    // Get CurrencyMeta resource out of root locale file.  [This may
    // move out of the root locale file later; if it does, update this
    // code.]
    UErrorCode ec = U_ZERO_ERROR;
    ResourceBundle currencyMeta =
        ResourceBundle((char*)0, Locale(""), ec).get(CURRENCY_META, ec);
    
    if (U_FAILURE(ec)) {
        // Config/build error; return hard-coded defaults
        return LAST_RESORT_DATA;
    }

    // Look up our currency, or if that's not available, then DEFAULT
    ResourceBundle rb = currencyMeta.get(currency, ec);
    if (U_FAILURE(ec)) {
        rb = currencyMeta.get(DEFAULT_META, ec);
        if (U_FAILURE(ec)) {
            // Config/build error; return hard-coded defaults
            return LAST_RESORT_DATA;
        }
    }

    int32_t len;
    const int32_t *data = rb.getIntVector(len, ec);
    if (U_FAILURE(ec) || len < 2) {
        // Config/build error; return hard-coded defaults
        return LAST_RESORT_DATA;
    }

    return data;
}

U_CAPI void U_EXPORT2
ucurr_forLocale(const char* locale,
                char*       result,
                UErrorCode* ec) {

    // TODO: ? Establish separate resource for locale->currency mapping
    //       ? <IF> we end up deleting the CurrencyElements resource.
    //       ? In the meantime the CurrencyElements tag has exactly the
    //       ? data we want.

    // Look up the CurrencyElements resource for this locale.
    // It contains: [0] = currency symbol, e.g. "$";
    //              [1] = intl. currency symbol, e.g. "USD";
    //              [2] = monetary decimal separator, e.g. ".".

    if (ec != NULL && U_SUCCESS(*ec)) {
        ResourceBundle rb((char*)0, Locale(locale), *ec);
        UnicodeString s = rb.get(CURRENCY_ELEMENTS, *ec).getStringEx(1, *ec);
        if (U_SUCCESS(*ec)) {
            s.extract(0, 3, result, "");
            return;
        }
    }

    result[0] = 0;
}

U_NAMESPACE_BEGIN
/**
 * Rather than convert a UnicodeString to a UChar array and then back
 * to a UnicodeString, possibly with allocation of UChar space on the
 * heap, we use this function in DecimalFormat.
 *
 * This is internal to ICU, but exported by this file.  Note that it
 * is C++ API even though it's named like a C function.
 *
 * @internal
 */
UnicodeString
ucurr_getSymbolAsUnicodeString(const char* currency,
                               const Locale& locale) {

    // Look up the Currencies resource for the given locale.  The
    // Currencies locale data looks like this:
    //|en {
    //|  Currencies { 
    //|    USD { "$" }
    //|    CHF { "sFr" }
    //|    //...
    //|  }
    //|}

    UErrorCode ec = U_ZERO_ERROR;
    ResourceBundle rb((char*)0, locale, ec);
    UnicodeString s = rb.get(CURRENCIES, ec).getStringEx(currency, ec);
    UBool found = U_SUCCESS(ec);

    if (!found) {
        // Since the Currencies resource is not fully populated yet,
        // check to see if we can find the currency in the
        // CurrencyElements resource.
        ec = U_ZERO_ERROR;
        ResourceBundle ce = rb.get(CURRENCY_ELEMENTS, ec);
        UnicodeString curr(currency, "");
        if (ce.getStringEx(1, ec) == curr) {
            s = ce.getStringEx((int32_t)0, ec);
            found = U_SUCCESS(ec);
        }

        if (!found) {
            // If we fail to find a match, use the full ISO code
            s = curr;
        }
    }

    return s;
}
U_NAMESPACE_END

U_CAPI int32_t U_EXPORT2
ucurr_getSymbol(const char* currency,
                UChar*      result,
                int32_t     resultCapacity,
                const char* locale,
                UErrorCode* ec) {

    if (ec == NULL || U_FAILURE(*ec)) {
        return 0;
    }

    UnicodeString s = ucurr_getSymbolAsUnicodeString(currency, Locale(locale));

    return s.extract(result, resultCapacity, *ec);
}

U_CAPI int32_t U_EXPORT2
ucurr_getDefaultFractionDigits(const char* currency) {
    return (_findData(currency))[0];
}

U_CAPI double U_EXPORT2
ucurr_getRoundingIncrement(const char* currency) {
    const int32_t *data = _findData(currency);

    // If there is no rounding, or if the meta data is invalid,
    // return 0.0 to indicate no rounding.
    if (data[1] == 0 || data[0] < 0 || data[0] > MAX_POW10) {
        return 0.0;
    }

    // Return data[1] / 10^(data[0]).  The only actual rounding data,
    // as of this writing, is CHF { 2, 25 }.
    return double(data[1]) / POW10[data[0]];
}

//eof
