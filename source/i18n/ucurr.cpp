/*
**********************************************************************
* Copyright (c) 2002-2003, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
*/

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/ucurr.h"
#include "unicode/locid.h"
#include "unicode/resbund.h"
#include "unicode/ustring.h"
#include "cmemory.h"
#include "cstring.h"
#include "uassert.h"
#include "iculserv.h"
#include "ucln_in.h"

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

#define ISO_COUNTRY_CODE_LENGTH 3

//------------------------------------------------------------
// Resource tags

// Tag for meta-data, in root.
static const char CURRENCY_META[] = "CurrencyMeta";

// Tag for map from countries to currencies, in root.
static const char CURRENCY_MAP[] = "CurrencyMap";

// Tag for default meta-data, in CURRENCY_META
static const char DEFAULT_META[] = "DEFAULT";

// Variant for legacy pre-euro mapping in CurrencyMap
static const char VAR_PRE_EURO[] = "PREEURO";

// Variant for legacy euro mapping in CurrencyMap
static const char VAR_EURO[] = "EURO";

// Variant delimiter
static const char VAR_DELIM[] = "_";

// Tag for localized display names (symbols) of currencies
static const char CURRENCIES[] = "Currencies";

// Marker character indicating that a display name is a ChoiceFormat
// pattern.  Strings that start with one mark are ChoiceFormat
// patterns.  Strings that start with 2 marks are static strings, and
// the first mark is deleted.
static const UChar CHOICE_FORMAT_MARK = 0x003D; // Equals sign

//------------------------------------------------------------
// Code

/**
 * Unfortunately, we have to convert the UChar* currency code to char*
 * to use it as a resource key.
 */
static inline char*
myUCharsToChars(char* resultOfLen4, const UChar* currency) {
    u_UCharsToChars(currency, resultOfLen4, ISO_COUNTRY_CODE_LENGTH);
    resultOfLen4[ISO_COUNTRY_CODE_LENGTH] = 0;
    return resultOfLen4;
}

/**
 * Internal function to look up currency data.  Result is an array of
 * two integers.  The first is the fraction digits.  The second is the
 * rounding increment, or 0 if none.  The rounding increment is in
 * units of 10^(-fraction_digits).
 */
static const int32_t*
_findMetaData(const UChar* currency) {

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
    char buf[ISO_COUNTRY_CODE_LENGTH+1];
    ResourceBundle rb = currencyMeta.get(myUCharsToChars(buf, currency), ec);
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


// ------------------------------------------
//
// Registration
//
//-------------------------------------------

// don't use ICUService since we don't need fallback

struct CReg;

/* Remember to call umtx_init(&gCRegLock) before using it! */
static UMTX gCRegLock = 0;
static CReg* gCRegHead = 0;

struct CReg : public UMemory {
    CReg *next;
    UChar iso[ISO_COUNTRY_CODE_LENGTH+1];
    char  id[ULOC_FULLNAME_CAPACITY];
    
    CReg(const UChar* _iso, const char* _id)
        : next(0)
    {
        int32_t len = uprv_strlen(_id);
        if (len > (int32_t)(sizeof(id)-1)) {
            len = (sizeof(id)-1);
        }
        uprv_strncpy(id, _id, len);
        id[len] = 0;
        uprv_memcpy(iso, _iso, ISO_COUNTRY_CODE_LENGTH * sizeof(const UChar));
        iso[ISO_COUNTRY_CODE_LENGTH] = 0;
    }
    
    static UCurrRegistryKey reg(const UChar* _iso, const char* _id, UErrorCode* status)
    {
        if (status && U_SUCCESS(*status) && _iso && _id) {
            CReg* n = new CReg(_iso, _id);
            if (n) {
                umtx_init(&gCRegLock);
                Mutex mutex(&gCRegLock);
                if (!gCRegHead) {
                    ucln_i18n_registerCleanup();
                }
                n->next = gCRegHead;
                gCRegHead = n;
                return n;
            }
            *status = U_MEMORY_ALLOCATION_ERROR;
        }
        return 0;
    }
    
    static UBool unreg(UCurrRegistryKey key) {
        umtx_init(&gCRegLock);
        Mutex mutex(&gCRegLock);
        if (gCRegHead == key) {
            gCRegHead = gCRegHead->next;
            delete (CReg*)key;
            return TRUE;
        }
        
        CReg* p = gCRegHead;
        while (p) {
            if (p->next == key) {
                p->next = ((CReg*)key)->next;
                delete (CReg*)key;	
                return TRUE;
            }
            p = p->next;
        }
        
        return FALSE;
    }
    
    static const UChar* get(const char* id) {
        umtx_init(&gCRegLock);
        Mutex mutex(&gCRegLock);
        CReg* p = gCRegHead;
        while (p) {
            if (uprv_strcmp(id, p->id) == 0) {
                return p->iso;
            }
            p = p->next;
        }
        return NULL;
    }

    /* This doesn't need to be thread safe. It's for u_cleanup only. */
    static void cleanup(void) {
        while (gCRegHead) {
            CReg* n = gCRegHead;
            gCRegHead = gCRegHead->next;
            delete n;
        }
        umtx_destroy(&gCRegLock);
    }
};

// -------------------------------------

static void
idForLocale(const char* locale, char* buffer, int capacity, UErrorCode* ec)
{
    // !!! this is internal only, assumes buffer is not null and capacity is sufficient
    // Extract the country name and variant name.  We only
    // recognize two variant names, EURO and PREEURO.
    char variant[ULOC_FULLNAME_CAPACITY];
    uloc_getCountry(locale, buffer, capacity, ec);
    uloc_getVariant(locale, variant, sizeof(variant), ec);
    if (0 == uprv_strcmp(variant, VAR_PRE_EURO) ||
        0 == uprv_strcmp(variant, VAR_EURO))
    {
        uprv_strcat(buffer, VAR_DELIM);
        uprv_strcat(buffer, variant);
    }
}

// -------------------------------------

U_CAPI UCurrRegistryKey U_EXPORT2
ucurr_register(const UChar* isoCode, const char* locale, UErrorCode *status) 
{
    if (status && U_SUCCESS(*status)) {
        char id[ULOC_FULLNAME_CAPACITY];
        idForLocale(locale, id, sizeof(id), status);
        return CReg::reg(isoCode, id, status);
    }
    return NULL;
}

// -------------------------------------

U_CAPI UBool U_EXPORT2
ucurr_unregister(UCurrRegistryKey key, UErrorCode* status) 
{
    if (status && U_SUCCESS(*status)) {
        return CReg::unreg(key);
    }
    return FALSE;
}

// -------------------------------------

U_CAPI const UChar* U_EXPORT2
ucurr_forLocale(const char* locale, UErrorCode* ec) {
    if (ec != NULL && U_SUCCESS(*ec)) {
        char id[ULOC_FULLNAME_CAPACITY];
        idForLocale(locale, id, sizeof(id), ec);
        if (U_FAILURE(*ec)) {
            return NULL;
        }
        
        const UChar* result = CReg::get(id);
        if (result) {
            return result;
        }
        
        // Look up the CurrencyMap element in the root bundle.
        UResourceBundle* rb = ures_open(NULL, "", ec);
        UResourceBundle* cm = ures_getByKey(rb, CURRENCY_MAP, NULL, ec);
        int32_t len;
        const UChar* s = ures_getStringByKey(cm, id, &len, ec);
        ures_close(cm);
        ures_close(rb);
        
        if (U_SUCCESS(*ec)) {
            return s;
        }
    }
    return NULL;
}

// end registration

/**
 * Modify the given locale name by removing the rightmost _-delimited
 * element.  If there is none, empty the string ("" == root).
 * NOTE: The string "root" is not recognized; do not use it.
 * @return TRUE if the fallback happened; FALSE if locale is already
 * root ("").
 */
static UBool fallback(char *loc) {
    if (!*loc) {
        return FALSE;
    }
    UErrorCode status = U_ZERO_ERROR;
    uloc_getParent(loc, loc, uprv_strlen(loc), &status);
 /*
    char *i = uprv_strrchr(loc, '_');
    if (i == NULL) {
        i = loc;
    }
    *i = 0;
 */
    return TRUE;
}


U_CAPI const UChar* U_EXPORT2
ucurr_getName(const UChar* currency,
              const char* locale,
              UCurrNameStyle nameStyle,
              UBool* isChoiceFormat, // fillin
              int32_t* len, // fillin
              UErrorCode* ec) {

    // Look up the Currencies resource for the given locale.  The
    // Currencies locale data looks like this:
    //|en {
    //|  Currencies { 
    //|    USD { "US$", "US Dollar" }
    //|    CHF { "Sw F", "Swiss Franc" }
    //|    INR { "=0#Rs|1#Re|1<Rs", "=0#Rupees|1#Rupee|1<Rupees" }
    //|    //...
    //|  }
    //|}

    if (ec == NULL || U_FAILURE(*ec)) {
        return 0;
    }

    int32_t choice = (int32_t) nameStyle;
    if (choice < 0 || choice > 1) {
        *ec = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }
    
    // In the future, resource bundles may implement multi-level
    // fallback.  That is, if a currency is not found in the en_US
    // Currencies data, then the en Currencies data will be searched.
    // Currently, if a Currencies datum exists in en_US and en, the
    // en_US entry hides that in en.

    // We want multi-level fallback for this resource, so we implement
    // it manually.

    // Use a separate UErrorCode here that does not propagate out of
    // this function.
    UErrorCode ec2 = U_ZERO_ERROR;

    char loc[ULOC_FULLNAME_CAPACITY];
    uloc_getName(locale, loc, sizeof(loc), &ec2);
    if (U_FAILURE(ec2) || ec2 == U_STRING_NOT_TERMINATED_WARNING) {
        *ec = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    char buf[ISO_COUNTRY_CODE_LENGTH+1];
    myUCharsToChars(buf, currency);

    const UChar* s = NULL;

    // Multi-level resource inheritance fallback loop
    for (;;) {
        ec2 = U_ZERO_ERROR;
        UResourceBundle* rb = ures_open(NULL, loc, &ec2);
        UResourceBundle* curr = ures_getByKey(rb, CURRENCIES, NULL, &ec2);
        UResourceBundle* names = ures_getByKey(curr, buf, NULL, &ec2);
        s = ures_getStringByIndex(names, choice, len, &ec2);
        ures_close(names);
        ures_close(curr);
        ures_close(rb);

        // If we've succeeded we're done.  Otherwise, try to fallback.
        // If that fails (because we are already at root) then exit.
        if (U_SUCCESS(ec2) || !fallback(loc)) {
            break;
        }
    }

    // Determine if this is a ChoiceFormat pattern.  One leading mark
    // indicates a ChoiceFormat.  Two indicates a static string that
    // starts with a mark.  In either case, the first mark is ignored,
    // if present.  Marks in the rest of the string have no special
    // meaning.
    *isChoiceFormat = FALSE;
    if (U_SUCCESS(ec2)) {
        U_ASSERT(s != NULL);
        int32_t i=0;
        while (i < *len && s[i] == CHOICE_FORMAT_MARK && i < 2) {
            ++i;
        }
        *isChoiceFormat = (i == 1);
        if (i != 0) ++s; // Skip over first mark
        return s;
    }

    // If we fail to find a match, use the ISO 4217 code
    *len = u_strlen(currency); // Should == ISO_COUNTRY_CODE_LENGTH, but maybe not...?
    return currency;
}

//!// This API is now redundant.  It predates ucurr_getName, which
//!// replaces and extends it.
//!U_CAPI const UChar* U_EXPORT2
//!ucurr_getSymbol(const UChar* currency,
//!                const char* locale,
//!    int32_t* len, // fillin
//!                UErrorCode* ec) {
//!    UBool isChoiceFormat;
//!    const UChar* s = ucurr_getName(currency, locale, UCURR_SYMBOL_NAME,
//!                                   &isChoiceFormat, len, ec);
//!    if (isChoiceFormat) {
//!        // Don't let ChoiceFormat patterns out through this API
//!        *len = u_strlen(currency); // Should == 3, but maybe not...?
//!        return currency;
//!    }
//!    return s;
//!}

U_CAPI int32_t U_EXPORT2
ucurr_getDefaultFractionDigits(const UChar* currency) {
    return (_findMetaData(currency))[0];
}

U_CAPI double U_EXPORT2
ucurr_getRoundingIncrement(const UChar* currency) {
    const int32_t *data = _findMetaData(currency);

    // If there is no rounding, or if the meta data is invalid,
    // return 0.0 to indicate no rounding.  A rounding value
    // (data[1]) of 0 or 1 indicates no rounding.
    if (data[1] < 2 || data[0] < 0 || data[0] > MAX_POW10) {
        return 0.0;
    }

    // Return data[1] / 10^(data[0]).  The only actual rounding data,
    // as of this writing, is CHF { 2, 5 }.
    return double(data[1]) / POW10[data[0]];
}

/**
 * Release all static memory held by currency.
 */
U_CFUNC UBool currency_cleanup(void) {
    CReg::cleanup();
    return TRUE;
}

#endif /* #if !UCONFIG_NO_FORMATTING */

//eof
