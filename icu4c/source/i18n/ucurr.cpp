/*
**********************************************************************
* Copyright (c) 2002, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
*/
#include "unicode/ucurr.h"
#include "unicode/locid.h"
#include "unicode/resbund.h"
#include "unicode/ustring.h"
#include "cstring.h"

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
 * Unfortunately, we have to convert the UChar* currency code to char*
 * to use it as a resource key.
 */
static inline char*
_16to8(char* resultOfLen4, const UChar* currency) {
    u_austrncpy(resultOfLen4, currency, 3);
    resultOfLen4[3] = 0;
    return resultOfLen4;
}

/**
 * Internal function to look up currency data.  Result is an array of
 * two integers.  The first is the fraction digits.  The second is the
 * rounding increment, or 0 if none.  The rounding increment is in
 * units of 10^(-fraction_digits).
 */
static const int32_t*
_findData(const UChar* currency) {

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
    char buf[4];
    ResourceBundle rb = currencyMeta.get(_16to8(buf, currency), ec);
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

U_CAPI const UChar* U_EXPORT2
ucurr_forLocale(const char* locale,
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
	UResourceBundle* rb = ures_open(NULL, locale, ec);
	UResourceBundle* ce = ures_getByKey(rb, CURRENCY_ELEMENTS, NULL, ec);
	int32_t len;
	const UChar* s = ures_getStringByIndex(ce, 1, &len, ec);
	ures_close(ce);
	ures_close(rb);
	// All resource data SHOULD be of length 3.  If it is not,
	// then the resource data is in error and we don't return it.
	if (U_SUCCESS(*ec) && len == 3) {
	    return s;
	}
    }

    return NULL;
}

U_CAPI const UChar* U_EXPORT2
ucurr_getSymbol(const UChar* currency,
                const char* locale,
		int32_t* len, // fillin
                UErrorCode* ec) {

    if (ec == NULL || U_FAILURE(*ec)) {
        return 0;
    }

    // Look up the Currencies resource for the given locale.  The
    // Currencies locale data looks like this:
    //|en {
    //|  Currencies { 
    //|    USD { "$" }
    //|    CHF { "sFr" }
    //|    //...
    //|  }
    //|}

    const UChar* s = NULL;
    char buf[4];
    UResourceBundle* rb = ures_open(NULL, locale, ec);
    UResourceBundle* rb_c = ures_getByKey(rb, CURRENCIES, NULL, ec);
    s = ures_getStringByKey(rb_c, _16to8(buf, currency), len, ec);
    ures_close(rb_c);
    UBool found = U_SUCCESS(*ec);

    if (!found) {
        // Since the Currencies resource is not fully populated yet,
        // check to see if we can find the currency in the
        // CurrencyElements resource.
        *ec = U_ZERO_ERROR;
	rb_c = ures_getByKey(rb, CURRENCY_ELEMENTS, NULL, ec);
	const UChar* elem1 = ures_getStringByIndex(rb_c, 1, len, ec);
	if (U_SUCCESS(*ec) &&  u_strcmp(elem1, currency) == 0) {
	    s = ures_getStringByIndex(rb_c, 0, len, ec);
	    found = U_SUCCESS(*ec);
	}
	ures_close(rb_c);

        if (!found) {
            // If we fail to find a match, use the full ISO code
            s = currency;
        }
    }

    ures_close(rb);
    return s;
}

U_CAPI int32_t U_EXPORT2
ucurr_getDefaultFractionDigits(const UChar* currency) {
    return (_findData(currency))[0];
}

U_CAPI double U_EXPORT2
ucurr_getRoundingIncrement(const UChar* currency) {
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
