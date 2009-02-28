/*
**********************************************************************
* Copyright (c) 2002-2009, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
*/

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/ucurr.h"
#include "unicode/locid.h"
#include "unicode/ures.h"
#include "unicode/ustring.h"
#include "unicode/choicfmt.h"
#include "unicode/parsepos.h"
#include "ustr_imp.h"
#include "cmemory.h"
#include "cstring.h"
#include "uassert.h"
#include "umutex.h"
#include "ucln_in.h"
#include "uenumimp.h"
#include "uhash.h"
#include "uresimp.h"

//#define UCURR_DEBUG 1
#ifdef UCURR_DEBUG
#include "stdio.h"
#endif

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
//

static const char CURRENCY_DATA[] = "supplementalData";
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
static const char VAR_DELIM = '_';
static const char VAR_DELIM_STR[] = "_";

// Variant for legacy euro mapping in CurrencyMap
//static const char VAR_DELIM_EURO[] = "_EURO";

#define VARIANT_IS_EMPTY    0
#define VARIANT_IS_EURO     0x1
#define VARIANT_IS_PREEURO  0x2

// Tag for localized display names (symbols) of currencies
static const char CURRENCIES[] = "Currencies";
static const char CURRENCYPLURALS[] = "CurrencyPlurals";

// Marker character indicating that a display name is a ChoiceFormat
// pattern.  Strings that start with one mark are ChoiceFormat
// patterns.  Strings that start with 2 marks are static strings, and
// the first mark is deleted.
static const UChar CHOICE_FORMAT_MARK = 0x003D; // Equals sign

static const UChar EUR_STR[] = {0x0045,0x0055,0x0052,0};

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
_findMetaData(const UChar* currency, UErrorCode& ec) {

    if (currency == 0 || *currency == 0) {
        if (U_SUCCESS(ec)) {
            ec = U_ILLEGAL_ARGUMENT_ERROR;
        }
        return LAST_RESORT_DATA;
    }

    // Get CurrencyMeta resource out of root locale file.  [This may
    // move out of the root locale file later; if it does, update this
    // code.]
    UResourceBundle* currencyData = ures_openDirect(NULL, CURRENCY_DATA, &ec);
    UResourceBundle* currencyMeta = ures_getByKey(currencyData, CURRENCY_META, currencyData, &ec);

    if (U_FAILURE(ec)) {
        ures_close(currencyMeta);
        // Config/build error; return hard-coded defaults
        return LAST_RESORT_DATA;
    }

    // Look up our currency, or if that's not available, then DEFAULT
    char buf[ISO_COUNTRY_CODE_LENGTH+1];
    UErrorCode ec2 = U_ZERO_ERROR; // local error code: soft failure
    UResourceBundle* rb = ures_getByKey(currencyMeta, myUCharsToChars(buf, currency), NULL, &ec2);
      if (U_FAILURE(ec2)) {
        ures_close(rb);
        rb = ures_getByKey(currencyMeta,DEFAULT_META, NULL, &ec);
        if (U_FAILURE(ec)) {
            ures_close(currencyMeta);
            ures_close(rb);
            // Config/build error; return hard-coded defaults
            return LAST_RESORT_DATA;
        }
    }

    int32_t len;
    const int32_t *data = ures_getIntVector(rb, &len, &ec);
    if (U_FAILURE(ec) || len != 2) {
        // Config/build error; return hard-coded defaults
        if (U_SUCCESS(ec)) {
            ec = U_INVALID_FORMAT_ERROR;
        }
        ures_close(currencyMeta);
        ures_close(rb);
        return LAST_RESORT_DATA;
    }

    ures_close(currencyMeta);
    ures_close(rb);
    return data;
}

// -------------------------------------

/**
 * @see VARIANT_IS_EURO
 * @see VARIANT_IS_PREEURO
 */
static uint32_t
idForLocale(const char* locale, char* countryAndVariant, int capacity, UErrorCode* ec)
{
    uint32_t variantType = 0;
    // !!! this is internal only, assumes buffer is not null and capacity is sufficient
    // Extract the country name and variant name.  We only
    // recognize two variant names, EURO and PREEURO.
    char variant[ULOC_FULLNAME_CAPACITY];
    uloc_getCountry(locale, countryAndVariant, capacity, ec);
    uloc_getVariant(locale, variant, sizeof(variant), ec);
    if (variant[0] != 0) {
        variantType = (0 == uprv_strcmp(variant, VAR_EURO))
                   | ((0 == uprv_strcmp(variant, VAR_PRE_EURO)) << 1);
        if (variantType)
        {
            uprv_strcat(countryAndVariant, VAR_DELIM_STR);
            uprv_strcat(countryAndVariant, variant);
        }
    }
    return variantType;
}

// ------------------------------------------
//
// Registration
//
//-------------------------------------------

// don't use ICUService since we don't need fallback

#if !UCONFIG_NO_SERVICE
U_CDECL_BEGIN
static UBool U_CALLCONV currency_cleanup(void);
U_CDECL_END
struct CReg;

/* Remember to call umtx_init(&gCRegLock) before using it! */
static UMTX gCRegLock = 0;
static CReg* gCRegHead = 0;

struct CReg : public U_NAMESPACE_QUALIFIER UMemory {
    CReg *next;
    UChar iso[ISO_COUNTRY_CODE_LENGTH+1];
    char  id[ULOC_FULLNAME_CAPACITY];

    CReg(const UChar* _iso, const char* _id)
        : next(0)
    {
        int32_t len = (int32_t)uprv_strlen(_id);
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
                umtx_lock(&gCRegLock);
                if (!gCRegHead) {
                    /* register for the first time */
                    ucln_i18n_registerCleanup(UCLN_I18N_CURRENCY, currency_cleanup);
                }
                n->next = gCRegHead;
                gCRegHead = n;
                umtx_unlock(&gCRegLock);
                return n;
            }
            *status = U_MEMORY_ALLOCATION_ERROR;
        }
        return 0;
    }

    static UBool unreg(UCurrRegistryKey key) {
        UBool found = FALSE;
        umtx_init(&gCRegLock);
        umtx_lock(&gCRegLock);

        CReg** p = &gCRegHead;
        while (*p) {
            if (*p == key) {
                *p = ((CReg*)key)->next;
                delete (CReg*)key;
                found = TRUE;
                break;
            }
            p = &((*p)->next);
        }

        umtx_unlock(&gCRegLock);
        return found;
    }

    static const UChar* get(const char* id) {
        const UChar* result = NULL;
        umtx_init(&gCRegLock);
        umtx_lock(&gCRegLock);
        CReg* p = gCRegHead;

        /* register cleanup of the mutex */
        ucln_i18n_registerCleanup(UCLN_I18N_CURRENCY, currency_cleanup);
        while (p) {
            if (uprv_strcmp(id, p->id) == 0) {
                result = p->iso;
                break;
            }
            p = p->next;
        }
        umtx_unlock(&gCRegLock);
        return result;
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

/**
 * Release all static memory held by currency.
 */
U_CDECL_BEGIN
static UBool U_CALLCONV currency_cleanup(void) {
#if !UCONFIG_NO_SERVICE
    CReg::cleanup();
#endif
    return TRUE;
}
U_CDECL_END

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
#endif /* UCONFIG_NO_SERVICE */

// -------------------------------------

U_CAPI int32_t U_EXPORT2
ucurr_forLocale(const char* locale,
                UChar* buff,
                int32_t buffCapacity,
                UErrorCode* ec)
{
    int32_t resLen = 0;
    const UChar* s = NULL;
    if (ec != NULL && U_SUCCESS(*ec)) {
        if ((buff && buffCapacity) || !buffCapacity) {
            UErrorCode localStatus = U_ZERO_ERROR;
            char id[ULOC_FULLNAME_CAPACITY];
            if ((resLen = uloc_getKeywordValue(locale, "currency", id, ULOC_FULLNAME_CAPACITY, &localStatus))) {
                // there is a currency keyword. Try to see if it's valid
                if(buffCapacity > resLen) {
                    u_charsToUChars(id, buff, resLen);
                }
            } else {
                // get country or country_variant in `id'
                uint32_t variantType = idForLocale(locale, id, sizeof(id), ec);

                if (U_FAILURE(*ec)) {
                    return 0;
                }

#if !UCONFIG_NO_SERVICE
                const UChar* result = CReg::get(id);
                if (result) {
                    if(buffCapacity > u_strlen(result)) {
                        u_strcpy(buff, result);
                    }
                    return u_strlen(result);
                }
#endif
                // Remove variants, which is only needed for registration.
                char *idDelim = strchr(id, VAR_DELIM);
                if (idDelim) {
                    idDelim[0] = 0;
                }

                // Look up the CurrencyMap element in the root bundle.
                UResourceBundle *rb = ures_openDirect(NULL, CURRENCY_DATA, &localStatus);
                UResourceBundle *cm = ures_getByKey(rb, CURRENCY_MAP, rb, &localStatus);
                UResourceBundle *countryArray = ures_getByKey(rb, id, cm, &localStatus);
                UResourceBundle *currencyReq = ures_getByIndex(countryArray, 0, NULL, &localStatus);
                s = ures_getStringByKey(currencyReq, "id", &resLen, &localStatus);

                /*
                Get the second item when PREEURO is requested, and this is a known Euro country.
                If the requested variant is PREEURO, and this isn't a Euro country, assume
                that the country changed over to the Euro in the future. This is probably
                an old version of ICU that hasn't been updated yet. The latest currency is
                probably correct.
                */
                if (U_SUCCESS(localStatus)) {
                    if ((variantType & VARIANT_IS_PREEURO) && u_strcmp(s, EUR_STR) == 0) {
                        currencyReq = ures_getByIndex(countryArray, 1, currencyReq, &localStatus);
                        s = ures_getStringByKey(currencyReq, "id", &resLen, &localStatus);
                    }
                    else if ((variantType & VARIANT_IS_EURO)) {
                        s = EUR_STR;
                    }
                }
                ures_close(countryArray);
                ures_close(currencyReq);

                if ((U_FAILURE(localStatus)) && strchr(id, '_') != 0)
                {
                    // We don't know about it.  Check to see if we support the variant.
                    uloc_getParent(locale, id, sizeof(id), ec);
                    *ec = U_USING_FALLBACK_WARNING;
                    return ucurr_forLocale(id, buff, buffCapacity, ec);
                }
                else if (*ec == U_ZERO_ERROR || localStatus != U_ZERO_ERROR) {
                    // There is nothing to fallback to. Report the failure/warning if possible.
                    *ec = localStatus;
                }
                if (U_SUCCESS(*ec)) {
                    if(buffCapacity > resLen) {
                        u_strcpy(buff, s);
                    }
                }
            }
            return u_terminateUChars(buff, buffCapacity, resLen, ec);
        } else {
            *ec = U_ILLEGAL_ARGUMENT_ERROR;
        }
    }
    return resLen;
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
    uloc_getParent(loc, loc, (int32_t)uprv_strlen(loc), &status);
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

    if (U_FAILURE(*ec)) {
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
    ec2 = U_ZERO_ERROR;
    UResourceBundle* rb = ures_open(NULL, loc, &ec2);

    rb = ures_getByKey(rb, CURRENCIES, rb, &ec2);

    // Fetch resource with multi-level resource inheritance fallback
    rb = ures_getByKeyWithFallback(rb, buf, rb, &ec2);

    s = ures_getStringByIndex(rb, choice, len, &ec2);
    ures_close(rb);

    // If we've succeeded we're done.  Otherwise, try to fallback.
    // If that fails (because we are already at root) then exit.
    if (U_SUCCESS(ec2)) {
        if (ec2 == U_USING_DEFAULT_WARNING
            || (ec2 == U_USING_FALLBACK_WARNING && *ec != U_USING_DEFAULT_WARNING)) {
            *ec = ec2;
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
    *ec = U_USING_DEFAULT_WARNING;
    return currency;
}

U_CAPI const UChar* U_EXPORT2
ucurr_getPluralName(const UChar* currency,
                    const char* locale,
                    UBool* isChoiceFormat,
                    const char* pluralCount,
                    int32_t* len, // fillin
                    UErrorCode* ec) {
    // Look up the Currencies resource for the given locale.  The
    // Currencies locale data looks like this:
    //|en {
    //|  CurrencyPlurals {
    //|    USD{
    //|      one{"US dollar"}
    //|      other{"US dollars"}
    //|    }
    //|  }
    //|}

    if (U_FAILURE(*ec)) {
        return 0;
    }

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
    ec2 = U_ZERO_ERROR;
    UResourceBundle* rb = ures_open(NULL, loc, &ec2);

    rb = ures_getByKey(rb, CURRENCYPLURALS, rb, &ec2);

    // Fetch resource with multi-level resource inheritance fallback
    rb = ures_getByKeyWithFallback(rb, buf, rb, &ec2);

    s = ures_getStringByKeyWithFallback(rb, pluralCount, len, &ec2);
    if (U_FAILURE(ec2)) {
        //  fall back to "other"
        ec2 = U_ZERO_ERROR;
        s = ures_getStringByKeyWithFallback(rb, "other", len, &ec2);     
        if (U_FAILURE(ec2)) {
            ures_close(rb);
            // fall back to long name in Currencies
            return ucurr_getName(currency, locale, UCURR_LONG_NAME, 
                                 isChoiceFormat, len, ec);
        }
    }
    ures_close(rb);

    // If we've succeeded we're done.  Otherwise, try to fallback.
    // If that fails (because we are already at root) then exit.
    if (U_SUCCESS(ec2)) {
        if (ec2 == U_USING_DEFAULT_WARNING
            || (ec2 == U_USING_FALLBACK_WARNING && *ec != U_USING_DEFAULT_WARNING)) {
            *ec = ec2;
        }
        U_ASSERT(s != NULL);
        return s;
    }

    // If we fail to find a match, use the ISO 4217 code
    *len = u_strlen(currency); // Should == ISO_COUNTRY_CODE_LENGTH, but maybe not...?
    *ec = U_USING_DEFAULT_WARNING;
    return currency;
}


//========================================================================
// Following are structure and function for parsing currency names

#define NEED_TO_BE_DELETED 0x1

typedef struct {
    const char* IsoCode;  // key
    UChar* currencyName;  // value
    int32_t currencyNameLen;  // value length
    int32_t flag;  // flags
} CurrencyNameStruct;


#define MIN(a,b) (((a)<(b)) ? (a) : (b))

// Comparason function used in quick sort.
static int currencyNameComparator(const void* a, const void* b) {
    const CurrencyNameStruct* currName_1 = (const CurrencyNameStruct*)a;
    const CurrencyNameStruct* currName_2 = (const CurrencyNameStruct*)b;
    for (int32_t i = 0; 
         i < MIN(currName_1->currencyNameLen, currName_2->currencyNameLen);
         ++i) {
        if (currName_1->currencyName[i] < currName_2->currencyName[i]) {
            return -1;
        }
        if (currName_1->currencyName[i] > currName_2->currencyName[i]) {
            return 1;
        }
    }
    if (currName_1->currencyNameLen < currName_2->currencyNameLen) {
        return -1;
    } else if (currName_1->currencyNameLen > currName_2->currencyNameLen) {
        return 1;
    }
    return 0;
}


// Give a locale, return the maximum number of currency names associated with
// this locale.
// It gets currency names from resource bundles using fallback.
// It is the maximum number because in the fallback chain, some of the 
// currency names are duplicated.
// For example, given locale as "en_US", the currency names get from resource
// bundle in "en_US" and "en" are duplicated. The fallback mechanism will count
// all currency names in "en_US" and "en".
static int32_t
getCurrencyNameCount(const char* loc) {
    int32_t total_currency_count = 0;
    const UChar* s = NULL;
    char locale[ULOC_FULLNAME_CAPACITY];
    uprv_strcpy(locale, loc);
    for (;;) {
        UErrorCode ec2 = U_ZERO_ERROR;
        // TODO: ures_openDirect?
        UResourceBundle* rb = ures_open(NULL, locale, &ec2);
        UResourceBundle* curr = ures_getByKey(rb, CURRENCIES, NULL, &ec2);
        int32_t n = ures_getSize(curr);
        for (int32_t i=0; i<n; ++i) {
            UResourceBundle* names = ures_getByIndex(curr, i, NULL, &ec2);
            int32_t len;
            s = ures_getStringByIndex(names, UCURR_SYMBOL_NAME, &len, &ec2);
            UBool isChoice = FALSE;
            if (len > 0 && s[0] == CHOICE_FORMAT_MARK) {
                ++s;
                --len;
                if (len > 0 && s[0] != CHOICE_FORMAT_MARK) {
                    isChoice = TRUE;
                }
            }
            if (isChoice) {
                ChoiceFormat fmt(s, ec2);
                int32_t fmt_count;
                fmt.getFormats(fmt_count);
                total_currency_count += fmt_count;
            } else {
                ++total_currency_count;  // currency symbol
            }

            total_currency_count += 2; // long name and iso code
            ures_close(names);
        }

        // currency plurals
        UErrorCode ec3 = U_ZERO_ERROR;
        UResourceBundle* curr_p = ures_getByKey(rb, CURRENCYPLURALS, NULL, &ec3);
        n = ures_getSize(curr_p);
        for (int32_t i=0; i<n; ++i) {
            UResourceBundle* names = ures_getByIndex(curr_p, i, NULL, &ec3);
            total_currency_count += ures_getSize(names);
            ures_close(names);
        }
        ures_close(curr_p);
        ures_close(curr);
        ures_close(rb);

        if (!fallback(locale)) {
            break;
        }
    }
    return total_currency_count;
}


// Collect all available currency names associated with the give locale
// (enable fallback chain).
// Read currenc names defined in resource bundle "Currencies" and
// "CurrencyPlural", enable fallback chain.
// return the malloc-ed currency name arrays and the total number of currency
// names in the array.
static CurrencyNameStruct*
collectCurrencyNames(const char* locale, int32_t* total_currency_count, 
                     UErrorCode& ec) {
    // Look up the Currencies resource for the given locale.
    UErrorCode ec2 = U_ZERO_ERROR;

    char loc[ULOC_FULLNAME_CAPACITY];
    uloc_getName(locale, loc, sizeof(loc), &ec2);
    if (U_FAILURE(ec2) || ec2 == U_STRING_NOT_TERMINATED_WARNING) {
        ec = U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }

    // Get maximum currency name count first.
    int32_t max_currency_count = getCurrencyNameCount(loc);

    CurrencyNameStruct* currencyNames = (CurrencyNameStruct*)uprv_malloc
        (sizeof(CurrencyNameStruct) * (max_currency_count));

    const UChar* s = NULL;  // currency name
    char* iso = NULL;  // currency ISO code

    *total_currency_count = 0;

    UErrorCode ec3 = U_ZERO_ERROR;
    UErrorCode ec4 = U_ZERO_ERROR;

    // Using hash to remove duplicates caused by locale fallback
    UHashtable* currencyIsoCodes = uhash_open(uhash_hashChars, uhash_compareChars, NULL, &ec3);
    UHashtable* currencyPluralIsoCodes = uhash_open(uhash_hashChars, uhash_compareChars, NULL, &ec4);
    for (int32_t localeLevel = 0; ; ++localeLevel) {
        ec2 = U_ZERO_ERROR;
        // TODO: ures_openDirect
        UResourceBundle* rb = ures_open(NULL, loc, &ec2);
        UResourceBundle* curr = ures_getByKey(rb, CURRENCIES, NULL, &ec2);
        int32_t n = ures_getSize(curr);
        for (int32_t i=0; i<n; ++i) {
            UResourceBundle* names = ures_getByIndex(curr, i, NULL, &ec2);
            int32_t len;
            s = ures_getStringByIndex(names, UCURR_SYMBOL_NAME, &len, &ec2);
            // TODO: uhash_put wont change key/value?
            iso = (char*)ures_getKey(names);
            if (localeLevel == 0) {
                uhash_put(currencyIsoCodes, iso, iso, &ec3); 
            } else {
                if (uhash_get(currencyIsoCodes, iso) != NULL) {
                    ures_close(names);
                    continue;
                } else {
                    uhash_put(currencyIsoCodes, iso, iso, &ec3); 
                }
            }
            UBool isChoice = FALSE;
            if (len > 0 && s[0] == CHOICE_FORMAT_MARK) {
                ++s;
                --len;
                if (len > 0 && s[0] != CHOICE_FORMAT_MARK) {
                    isChoice = TRUE;
                }
            }
            if (isChoice) {
                ChoiceFormat fmt(s, ec2);
                int32_t fmt_count;
                const UnicodeString* formats = fmt.getFormats(fmt_count);
                for (int i = 0; i < fmt_count; ++i) {
                    // put iso, formats[i]; into array
                    int32_t length = formats[i].length();
                    UChar* name = (UChar*)uprv_malloc(sizeof(UChar)*length);
                    formats[i].extract(0, length, name);
                    currencyNames[*total_currency_count].IsoCode = iso;
                    currencyNames[*total_currency_count].currencyName = name;
                    currencyNames[*total_currency_count].flag = NEED_TO_BE_DELETED;
                    currencyNames[(*total_currency_count)++].currencyNameLen = length;
                }
            } else {
                // Add currency symbol.
                currencyNames[*total_currency_count].IsoCode = iso;
                currencyNames[*total_currency_count].currencyName = (UChar*)s;
                currencyNames[*total_currency_count].flag = 0;
                currencyNames[(*total_currency_count)++].currencyNameLen = len;
            }

            // Add currency long name.
            s = ures_getStringByIndex(names, UCURR_LONG_NAME, &len, &ec2);
            currencyNames[*total_currency_count].IsoCode = iso;
            currencyNames[*total_currency_count].currencyName = (UChar*)s;
            currencyNames[*total_currency_count].flag = 0;
            currencyNames[(*total_currency_count)++].currencyNameLen = len;

            // put (iso, 3, and iso) in to array
            // Add currency ISO code.
            currencyNames[*total_currency_count].IsoCode = iso;
            currencyNames[*total_currency_count].currencyName = (UChar*)uprv_malloc(sizeof(UChar)*3);
            currencyNames[*total_currency_count].currencyName[0] = iso[0];
            currencyNames[*total_currency_count].currencyName[1] = iso[1];
            currencyNames[*total_currency_count].currencyName[2] = iso[2];
            currencyNames[*total_currency_count].flag = NEED_TO_BE_DELETED;
            currencyNames[(*total_currency_count)++].currencyNameLen = 3;

            ures_close(names);
        }

        // currency plurals
        UErrorCode ec3 = U_ZERO_ERROR;
        UResourceBundle* curr_p = ures_getByKey(rb, CURRENCYPLURALS, NULL, &ec3);
        n = ures_getSize(curr_p);
        for (int32_t i=0; i<n; ++i) {
            UResourceBundle* names = ures_getByIndex(curr_p, i, NULL, &ec3);
            iso = (char*)ures_getKey(names);
            // Using hash to remove duplicated ISO codes in fallback chain.
            if (localeLevel == 0) {
                uhash_put(currencyPluralIsoCodes, iso, iso, &ec4); 
            } else {
                if (uhash_get(currencyPluralIsoCodes, iso) != NULL) {
                    ures_close(names);
                    continue;
                } else {
                    uhash_put(currencyPluralIsoCodes, iso, iso, &ec4); 
                }
            }
            int32_t num = ures_getSize(names);
            int32_t len;
            for (int32_t j = 0; j < num; ++j) {
                // TODO: remove duplicates between singular name and 
                // currency long name?
                s = ures_getStringByIndex(names, j, &len, &ec3);
                currencyNames[*total_currency_count].IsoCode = iso;
                currencyNames[*total_currency_count].currencyName = (UChar*)s;
                currencyNames[*total_currency_count].flag = 0;
                currencyNames[(*total_currency_count)++].currencyNameLen = len;
            }
            ures_close(names);
        }
        ures_close(curr_p);
        ures_close(curr);
        ures_close(rb);

        if (!fallback(loc)) {
            break;
        }
    }

    uhash_close(currencyIsoCodes);
    uhash_close(currencyPluralIsoCodes);

    // quick sort the struct
    qsort(currencyNames, *total_currency_count, sizeof(CurrencyNameStruct),
          currencyNameComparator);

#ifdef UCURR_DEBUG
    for (int32_t index = 0; index < *total_currency_count; ++index) {
        printf("index: %d\n", index);
        printf("iso: %s\n", currencyNames[index].IsoCode);
        printf("currencyName:");
        for (int32_t i = 0; i < currencyNames[index].currencyNameLen; ++i) {
            printf("%c", (unsigned char)currencyNames[index].currencyName[i]);
        }
        printf("\n");
        printf("len: %d\n", currencyNames[index].currencyNameLen);
    }
    printf("currency count: %d\n", *total_currency_count);
#endif

    return currencyNames;
}

// @param  currencyNames: currency names array
// @param  indexInCurrencyNames: the index of the character in currency names 
//         array against which the comparison is done
// @param  text: input text to compare against
// @param  pos: the position of character in input text to compare against
// @param  begin(IN/OUT): the begin index of matching range in currency names array
// @param  end(IN/OUT): the end index of matching range in currency names array.
static int32_t
binarySearch(const CurrencyNameStruct* currencyNames, 
             int32_t indexInCurrencyNames,
             const UnicodeString* text, int32_t pos,
             int32_t* begin, int32_t* end) {
#ifdef UCURR_DEBUG
    printf("pos = %d\n", pos);
#endif
   UChar key = text->charAt(pos); 
   int32_t first = *begin;
   int32_t last = *end;
   while (first <= last) {
       int32_t mid = (first + last) / 2;  // compute mid point.
       if (indexInCurrencyNames >= currencyNames[mid].currencyNameLen) {
           first = mid + 1;
       } else {
           if (key > currencyNames[mid].currencyName[indexInCurrencyNames]) {
               first = mid + 1;
           }
           else if (key < currencyNames[mid].currencyName[indexInCurrencyNames]) {
               last = mid - 1;
           }
           else {
                // Find a match, and looking for ranges
                // Now do two more binary searches. First, on the left side for
                // the greatest L such that CurrencyNameStruct[L] < key.
                int32_t L = *begin;
                int32_t R = mid;

#ifdef UCURR_DEBUG
                printf("mid = %d\n", mid);
#endif
                while (L < R) {
                    int32_t M = (L + R) / 2;
#ifdef UCURR_DEBUG
                    printf("L = %d, R = %d, M = %d\n", L, R, M);
#endif
                    if (indexInCurrencyNames >= currencyNames[M].currencyNameLen) {
                        L = M + 1;
                    } else {
                        if (currencyNames[M].currencyName[indexInCurrencyNames] < key) {
                            L = M + 1;
                        } else {
#ifdef UCURR_DEBUG
                            U_ASSERT(currencyNames[M].currencyName[indexInCurrencyNames] == key);
#endif
                            R = M;
                        }
                    }
                }
#ifdef UCURR_DEBUG
                U_ASSERT(L == R);
#endif
                *begin = L;
#ifdef UCURR_DEBUG
                printf("begin = %d\n", *begin);
                U_ASSERT(currencyNames[*begin].currencyName[indexInCurrencyNames] == key);
#endif

                // Now for the second search, finding the least R such that
                // key < CurrencyNameStruct[R].
                L = mid;
                R = *end;
                while (L < R) {
                    int32_t M = (L + R) / 2;
#ifdef UCURR_DEBUG
                    printf("L = %d, R = %d, M = %d\n", L, R, M);
#endif
                    if (currencyNames[M].currencyNameLen < indexInCurrencyNames) {
                        L = M + 1;
                    } else {
                        if (currencyNames[M].currencyName[indexInCurrencyNames] > key) {
                            R = M;
                        } else {
#ifdef UCURR_DEBUG
                            U_ASSERT(currencyNames[M].currencyName[indexInCurrencyNames] == key);
#endif
                            L = M + 1;
                        }
                    }
                }
#ifdef UCURR_DEBUG
                U_ASSERT(L == R);
#endif
                if (currencyNames[R].currencyName[indexInCurrencyNames] > key) {
                    *end = R - 1;
                } else {
                    *end = R;
                }
#ifdef UCURR_DEBUG
                printf("end = %d\n", *end);
#endif

                // now, found the range. check whether there is exact match
                if (currencyNames[*begin].currencyNameLen == indexInCurrencyNames + 1) {
                    return *begin;  // find range and exact match.
                }
                return -1;  // find range, but no exact match.
           }
       }
   }
   *begin = -1;
   *end = -1;
   return -1;    // failed to find range.
}


// Linear search "text" in "currencyNames".
// @param  begin, end: the begin and end index in currencyNames, within which
//         range should the search be performed.
// @param  startPos: the comparison start position in text
// @param  maxMatchLen(IN/OUT): passing in the computed max matching length
//                              pass out the new max  matching length
// @param  maxMatchIndex: the index in currencyName which has the longest
//                        match with input text.
static void
linearSearch(const CurrencyNameStruct* currencyNames, 
             int32_t begin, int32_t end,
             const UnicodeString* text, int32_t startPos,
             int32_t *maxMatchLen, int32_t* maxMatchIndex) {
    for (int32_t index = begin; index <= end; ++index) {
        int32_t len = currencyNames[index].currencyNameLen;
        // TODO: case in-sensitve? but case-sensitive for ISO code /symbol?
        if (len > *maxMatchLen &&
            text->compare(startPos, len, currencyNames[index].currencyName) == 0) {
            *maxMatchIndex = index;
            *maxMatchLen = len;
#ifdef UCURR_DEBUG
            printf("maxMatchIndex = %d, maxMatchLen = %d\n",
                   *maxMatchIndex, *maxMatchLen);
#endif
        }
    }
}

#define LINEAR_SEARCH_THRESHOLD 10

// Find longest match between "text" and currency names in "currencyNames".
// @param  total_currency_count: total number of currency names in CurrencyNames.
// @param  start: the comparison start position in text
// @param  maxMatchLen: passing in the computed max matching length
//                              pass out the new max  matching length
// @param  maxMatchIndex: the index in currencyName which has the longest
//                        match with input text.
static void
searchCurrencyName(const CurrencyNameStruct* currencyNames, 
                   int32_t total_currency_count,
                   const UnicodeString* text, int32_t start, 
                   int32_t* maxMatchLen, int32_t* maxMatchIndex) {
    *maxMatchIndex = -1;
    *maxMatchLen = 0;
    int32_t matchIndex = -1;
    int32_t binarySearchBegin = 0;
    int32_t binarySearchEnd = total_currency_count - 1;
    // It is a variant of binary search.
    // For example, given the currency names in currencyNames array are:
    // A AB ABC AD AZ B BB BBEX BBEXYZ BS C D E....
    // and the input text is BBEXST
    // The first round binary search search "B" in the text against
    // the first char in currency names, and find the first char matching range
    // to be "B BB BBEX BBEXYZ BS" (and the maximum matching "B").
    // The 2nd round binary search search the second "B" in the text against
    // the 2nd char in currency names, and narrow the matching range to
    // "BB BBEX BBEXYZ" (and the maximum matching "BB").
    // The 3rd round returnes the range as "BBEX BBEXYZ" (without changing
    // maximum matching).
    // The 4th round returns the same range (the maximum matching is "BBEX").
    // The 5th round returns no matching range.
    for (int32_t index = start; index < text->length(); ++index) {
        // matchIndex saves the one with exact match till the current point.
        // [binarySearchBegin, binarySearchEnd] saves the matching range.
        matchIndex = binarySearch(currencyNames, index - start,
                                  text, index,
                                  &binarySearchBegin, &binarySearchEnd);
        if (binarySearchBegin == -1) { // did not find the range
            break;
        }
        if (matchIndex != -1) { 
            // find an exact match for text from text[start] to text[index] 
            // in currencyNames array.
            *maxMatchLen = index - start + 1;
            *maxMatchIndex = matchIndex;
        }
        if (binarySearchEnd - binarySearchBegin < LINEAR_SEARCH_THRESHOLD) {
            // linear search if within threshold.
            linearSearch(currencyNames, binarySearchBegin, binarySearchEnd,
                         text, start,
                         maxMatchLen, maxMatchIndex);
            break;
        }
    }
    return;
}

//========================= currency name cache =====================
typedef struct {
    char locale[ULOC_FULLNAME_CAPACITY];  //key
    CurrencyNameStruct* currencyNames;  // value
    int32_t totalCurrencyNameCount;  // currency name count
    // reference count.
    // reference count is set to 1 when an entry is put to cache.
    // it increases by 1 before accessing, and decreased by 1 after accessing.
    // The entry is deleted when ref count is zero, which means 
    // the entry is replaced out of cache and no process is accessing it.
    int32_t refCount;
} CurrencyNameCacheEntry;


#define CURRENCY_NAME_CACHE_NUM 10

// Reserve 10 cache entries.
static CurrencyNameCacheEntry* currCache[CURRENCY_NAME_CACHE_NUM] = {NULL};
// Using an index to indicate which entry to be replaced when cache is full.
// It is a simple round-robin replacement strategy.
static int8_t currentCacheEntryIndex = 0;

// Cache deletion
static void
deleteCurrencyNames(CurrencyNameStruct* currencyNames, int32_t count) {
    for (int32_t index = 0; index < count; ++index) {
        if ( (currencyNames[index].flag & NEED_TO_BE_DELETED) ) {
            uprv_free(currencyNames[index].currencyName);
        }
    }
    uprv_free(currencyNames);
}


static void
deleteCacheEntry(CurrencyNameCacheEntry* entry) {
    deleteCurrencyNames(entry->currencyNames, entry->totalCurrencyNameCount);
    uprv_free(entry);
}


// Cache clean up
static UBool U_CALLCONV
currency_cache_cleanup(void) {
    for (int32_t i = 0; i < CURRENCY_NAME_CACHE_NUM; ++i) {
        if (currCache[i]) {
            deleteCacheEntry(currCache[i]);
            currCache[i] = 0;
        }
    }
    return TRUE;
}


U_CFUNC void
uprv_parseCurrency(const char* locale,
                   const U_NAMESPACE_QUALIFIER UnicodeString& text,
                   U_NAMESPACE_QUALIFIER ParsePosition& pos,
                   UChar* result,
                   UErrorCode& ec)
{
    U_NAMESPACE_USE

    if (U_FAILURE(ec)) {
        return;
    }

    int32_t total_currency_count = 0;
    CurrencyNameStruct* currencyNames = NULL;
    CurrencyNameCacheEntry* cacheEntry = NULL;

    umtx_lock(NULL);
    // in order to handle racing correctly,
    // not putting 'search' in a separate function and using UMTX.
    int8_t  found = -1;
    for (int8_t i = 0; i < CURRENCY_NAME_CACHE_NUM; ++i) {
        if (currCache[i]!= NULL &&
            uprv_strcmp(locale, currCache[i]->locale) == 0) {
            found = i;
            break;
        }
    }
    if (found != -1) {
        cacheEntry = currCache[found];
        currencyNames = cacheEntry->currencyNames;
        total_currency_count = cacheEntry->totalCurrencyNameCount;
        ++(cacheEntry->refCount);
    }
    umtx_unlock(NULL);
    if (found == -1) {
        currencyNames = collectCurrencyNames(locale, &total_currency_count, ec);
        if (U_FAILURE(ec)) {
            return;
        }
        umtx_lock(NULL);
        // check again.
        int8_t  found = -1;
        for (int8_t i = 0; i < CURRENCY_NAME_CACHE_NUM; ++i) {
            if (currCache[i]!= NULL &&
                uprv_strcmp(locale, currCache[i]->locale) == 0) {
                found = i;
                break;
            }
        }
        if (found == -1) {
            // insert new entry to 
            // currentCacheEntryIndex % CURRENCY_NAME_CACHE_NUM
            // and remove the existing entry 
            // currentCacheEntryIndex % CURRENCY_NAME_CACHE_NUM
            // from cache.
            cacheEntry = currCache[currentCacheEntryIndex];
            if (cacheEntry) {
                --(cacheEntry->refCount);
                // delete if the ref count is zero
                if (cacheEntry->refCount == 0) {
                    deleteCacheEntry(cacheEntry);
                }
            }
            cacheEntry = (CurrencyNameCacheEntry*)uprv_malloc(sizeof(CurrencyNameCacheEntry));
            currCache[currentCacheEntryIndex] = cacheEntry;
            uprv_strcpy(cacheEntry->locale, locale);
            cacheEntry->currencyNames = currencyNames;
            cacheEntry->totalCurrencyNameCount = total_currency_count;
            cacheEntry->refCount = 2; // one for cache, one for reference
            currentCacheEntryIndex = (currentCacheEntryIndex + 1) % CURRENCY_NAME_CACHE_NUM;
            ucln_i18n_registerCleanup(UCLN_I18N_CURRENCY, currency_cache_cleanup);
            
        } else {
            uprv_free(currencyNames);
            cacheEntry = currCache[found];
            currencyNames = cacheEntry->currencyNames;
            total_currency_count = cacheEntry->totalCurrencyNameCount;
            ++(cacheEntry->refCount);
        }
        umtx_unlock(NULL);
    }

    int32_t max = 0;
    int32_t matchIndex = -1;
    int32_t start = pos.getIndex();
    searchCurrencyName(currencyNames, total_currency_count, 
                       &text, start, &max, &matchIndex);

    if (matchIndex != -1) {
        u_charsToUChars(currencyNames[matchIndex].IsoCode, result, 4);
    }

    // decrease reference count
    umtx_lock(NULL);
    --(cacheEntry->refCount);
    if (cacheEntry->refCount == 0) {  // remove 
        deleteCacheEntry(cacheEntry);
    }
    umtx_unlock(NULL);
    
    pos.setIndex(start + max);
}


/**
 * Internal method.  Given a currency ISO code and a locale, return
 * the "static" currency name.  This is usually the same as the
 * UCURR_SYMBOL_NAME, but if the latter is a choice format, then the
 * format is applied to the number 2.0 (to yield the more common
 * plural) to return a static name.
 *
 * This is used for backward compatibility with old currency logic in
 * DecimalFormat and DecimalFormatSymbols.
 */
U_CFUNC void
uprv_getStaticCurrencyName(const UChar* iso, const char* loc,
                           U_NAMESPACE_QUALIFIER UnicodeString& result, UErrorCode& ec)
{
    U_NAMESPACE_USE

    UBool isChoiceFormat;
    int32_t len;
    const UChar* currname = ucurr_getName(iso, loc, UCURR_SYMBOL_NAME,
                                          &isChoiceFormat, &len, &ec);
    if (U_SUCCESS(ec)) {
        // If this is a ChoiceFormat currency, then format an
        // arbitrary value; pick something != 1; more common.
        result.truncate(0);
        if (isChoiceFormat) {
            ChoiceFormat f(currname, ec);
            if (U_SUCCESS(ec)) {
                f.format(2.0, result);
            } else {
                result = iso;
            }
        } else {
            result = currname;
        }
    }
}

U_CAPI int32_t U_EXPORT2
ucurr_getDefaultFractionDigits(const UChar* currency, UErrorCode* ec) {
    return (_findMetaData(currency, *ec))[0];
}

U_CAPI double U_EXPORT2
ucurr_getRoundingIncrement(const UChar* currency, UErrorCode* ec) {
    const int32_t *data = _findMetaData(currency, *ec);

    // If the meta data is invalid, return 0.0.
    if (data[0] < 0 || data[0] > MAX_POW10) {
        if (U_SUCCESS(*ec)) {
            *ec = U_INVALID_FORMAT_ERROR;
        }
        return 0.0;
    }

    // If there is no rounding, return 0.0 to indicate no rounding.  A
    // rounding value (data[1]) of 0 or 1 indicates no rounding.
    if (data[1] < 2) {
        return 0.0;
    }

    // Return data[1] / 10^(data[0]).  The only actual rounding data,
    // as of this writing, is CHF { 2, 5 }.
    return double(data[1]) / POW10[data[0]];
}

U_CDECL_BEGIN

typedef struct UCurrencyContext {
    uint32_t currType; /* UCurrCurrencyType */
    uint32_t listIdx;
} UCurrencyContext;

/*
Please keep this list in alphabetical order.
You can look at the CLDR supplemental data or ISO-4217 for the meaning of some
of these items.
ISO-4217: http://www.iso.org/iso/en/prods-services/popstds/currencycodeslist.html
*/
static const struct CurrencyList {
    const char *currency;
    uint32_t currType;
} gCurrencyList[] = {
    {"ADP", UCURR_COMMON|UCURR_DEPRECATED},
    {"AED", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"AFA", UCURR_COMMON|UCURR_DEPRECATED},
    {"AFN", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"ALK", UCURR_COMMON|UCURR_DEPRECATED},
    {"ALL", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"AMD", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"ANG", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"AOA", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"AOK", UCURR_COMMON|UCURR_DEPRECATED},
    {"AON", UCURR_COMMON|UCURR_DEPRECATED},
    {"AOR", UCURR_COMMON|UCURR_DEPRECATED},
    {"ARA", UCURR_COMMON|UCURR_DEPRECATED},
    {"ARP", UCURR_COMMON|UCURR_DEPRECATED},
    {"ARS", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"ATS", UCURR_COMMON|UCURR_DEPRECATED},
    {"AUD", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"AWG", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"AZM", UCURR_COMMON|UCURR_DEPRECATED},
    {"AZN", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"BAD", UCURR_COMMON|UCURR_DEPRECATED},
    {"BAM", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"BBD", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"BDT", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"BEC", UCURR_UNCOMMON|UCURR_DEPRECATED},
    {"BEF", UCURR_COMMON|UCURR_DEPRECATED},
    {"BEL", UCURR_UNCOMMON|UCURR_DEPRECATED},
    {"BGL", UCURR_COMMON|UCURR_DEPRECATED},
    {"BGM", UCURR_COMMON|UCURR_DEPRECATED},
    {"BGN", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"BHD", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"BIF", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"BMD", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"BND", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"BOB", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"BOP", UCURR_COMMON|UCURR_DEPRECATED},
    {"BOV", UCURR_UNCOMMON|UCURR_NON_DEPRECATED},
    {"BRB", UCURR_COMMON|UCURR_DEPRECATED},
    {"BRC", UCURR_COMMON|UCURR_DEPRECATED},
    {"BRE", UCURR_COMMON|UCURR_DEPRECATED},
    {"BRL", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"BRN", UCURR_COMMON|UCURR_DEPRECATED},
    {"BRR", UCURR_COMMON|UCURR_DEPRECATED},
    {"BSD", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"BTN", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"BUK", UCURR_COMMON|UCURR_DEPRECATED},
    {"BWP", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"BYB", UCURR_COMMON|UCURR_DEPRECATED},
    {"BYR", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"BZD", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"CAD", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"CDF", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"CHE", UCURR_UNCOMMON|UCURR_NON_DEPRECATED},
    {"CHF", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"CHW", UCURR_UNCOMMON|UCURR_NON_DEPRECATED},
    {"CLF", UCURR_UNCOMMON|UCURR_NON_DEPRECATED},
    {"CLP", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"CNX", UCURR_UNCOMMON|UCURR_DEPRECATED},
    {"CNY", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"COP", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"COU", UCURR_UNCOMMON|UCURR_NON_DEPRECATED},
    {"CRC", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"CSD", UCURR_COMMON|UCURR_DEPRECATED},
    {"CSK", UCURR_COMMON|UCURR_DEPRECATED},
    {"CUP", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"CVE", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"CYP", UCURR_COMMON|UCURR_DEPRECATED},
    {"CZK", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"DDM", UCURR_COMMON|UCURR_DEPRECATED},
    {"DEM", UCURR_COMMON|UCURR_DEPRECATED},
    {"DJF", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"DKK", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"DOP", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"DZD", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"ECS", UCURR_COMMON|UCURR_DEPRECATED},
    {"ECV", UCURR_UNCOMMON|UCURR_DEPRECATED},
    {"EEK", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"EGP", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"EQE", UCURR_COMMON|UCURR_DEPRECATED},
    {"ERN", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"ESA", UCURR_UNCOMMON|UCURR_DEPRECATED},
    {"ESB", UCURR_UNCOMMON|UCURR_DEPRECATED},
    {"ESP", UCURR_COMMON|UCURR_DEPRECATED},
    {"ETB", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"EUR", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"FIM", UCURR_COMMON|UCURR_DEPRECATED},
    {"FJD", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"FKP", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"FRF", UCURR_COMMON|UCURR_DEPRECATED},
    {"GBP", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"GEK", UCURR_COMMON|UCURR_DEPRECATED},
    {"GEL", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"GHC", UCURR_COMMON|UCURR_DEPRECATED},
    {"GHS", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"GIP", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"GMD", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"GNF", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"GNS", UCURR_COMMON|UCURR_DEPRECATED},
    {"GQE", UCURR_COMMON|UCURR_DEPRECATED},
    {"GRD", UCURR_COMMON|UCURR_DEPRECATED},
    {"GTQ", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"GWE", UCURR_COMMON|UCURR_DEPRECATED},
    {"GWP", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"GYD", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"HKD", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"HNL", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"HRD", UCURR_COMMON|UCURR_DEPRECATED},
    {"HRK", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"HTG", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"HUF", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"IDR", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"IEP", UCURR_COMMON|UCURR_DEPRECATED},
    {"ILP", UCURR_COMMON|UCURR_DEPRECATED},
    {"ILS", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"INR", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"IQD", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"IRR", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"ISK", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"ITL", UCURR_COMMON|UCURR_DEPRECATED},
    {"JMD", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"JOD", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"JPY", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"KES", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"KGS", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"KHR", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"KMF", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"KPW", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"KRW", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"KWD", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"KYD", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"KZT", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"LAK", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"LBP", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"LKR", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"LRD", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"LSL", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"LSM", UCURR_COMMON|UCURR_DEPRECATED},
    {"LTL", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"LTT", UCURR_COMMON|UCURR_DEPRECATED},
    {"LUC", UCURR_UNCOMMON|UCURR_DEPRECATED},
    {"LUF", UCURR_COMMON|UCURR_DEPRECATED},
    {"LUL", UCURR_UNCOMMON|UCURR_DEPRECATED},
    {"LVL", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"LVR", UCURR_COMMON|UCURR_DEPRECATED},
    {"LYD", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"MAD", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"MAF", UCURR_COMMON|UCURR_DEPRECATED},
    {"MDL", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"MGA", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"MGF", UCURR_COMMON|UCURR_DEPRECATED},
    {"MKD", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"MLF", UCURR_COMMON|UCURR_DEPRECATED},
    {"MMK", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"MNT", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"MOP", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"MRO", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"MTL", UCURR_COMMON|UCURR_DEPRECATED},
    {"MTP", UCURR_COMMON|UCURR_DEPRECATED},
    {"MUR", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"MVR", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"MWK", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"MXN", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"MXP", UCURR_COMMON|UCURR_DEPRECATED},
    {"MXV", UCURR_UNCOMMON|UCURR_NON_DEPRECATED},
    {"MYR", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"MZE", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"MZM", UCURR_COMMON|UCURR_DEPRECATED},
    {"MZN", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"NAD", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"NGN", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"NIC", UCURR_COMMON|UCURR_DEPRECATED},
    {"NIO", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"NLG", UCURR_COMMON|UCURR_DEPRECATED},
    {"NOK", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"NPR", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"NZD", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"OMR", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"PAB", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"PEI", UCURR_COMMON|UCURR_DEPRECATED},
    {"PEN", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"PES", UCURR_COMMON|UCURR_DEPRECATED},
    {"PGK", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"PHP", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"PKR", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"PLN", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"PLZ", UCURR_COMMON|UCURR_DEPRECATED},
    {"PTE", UCURR_COMMON|UCURR_DEPRECATED},
    {"PYG", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"QAR", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"RHD", UCURR_COMMON|UCURR_DEPRECATED},
    {"ROL", UCURR_COMMON|UCURR_DEPRECATED},
    {"RON", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"RSD", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"RUB", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"RUR", UCURR_COMMON|UCURR_DEPRECATED},
    {"RWF", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"SAR", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"SBD", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"SCR", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"SDD", UCURR_COMMON|UCURR_DEPRECATED},
    {"SDG", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"SDP", UCURR_COMMON|UCURR_DEPRECATED},
    {"SEK", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"SGD", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"SHP", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"SIT", UCURR_COMMON|UCURR_DEPRECATED},
    {"SKK", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"SLL", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"SOS", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"SRD", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"SRG", UCURR_COMMON|UCURR_DEPRECATED},
    {"STD", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"SUR", UCURR_COMMON|UCURR_DEPRECATED},
    {"SVC", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"SYP", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"SZL", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"THB", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"TJR", UCURR_COMMON|UCURR_DEPRECATED},
    {"TJS", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"TMM", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"TND", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"TOP", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"TPE", UCURR_COMMON|UCURR_DEPRECATED},
    {"TRL", UCURR_COMMON|UCURR_DEPRECATED},
    {"TRY", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"TTD", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"TWD", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"TZS", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"UAH", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"UAK", UCURR_COMMON|UCURR_DEPRECATED},
    {"UGS", UCURR_COMMON|UCURR_DEPRECATED},
    {"UGX", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"USD", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"USN", UCURR_UNCOMMON|UCURR_NON_DEPRECATED},
    {"USS", UCURR_UNCOMMON|UCURR_NON_DEPRECATED},
    {"UYP", UCURR_COMMON|UCURR_DEPRECATED},
    {"UYI", UCURR_UNCOMMON|UCURR_NON_DEPRECATED},
    {"UYU", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"UZS", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"VEB", UCURR_COMMON|UCURR_DEPRECATED},
    {"VEF", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"VND", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"VUV", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"WST", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"XAF", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"XAG", UCURR_UNCOMMON|UCURR_NON_DEPRECATED},
    {"XAU", UCURR_UNCOMMON|UCURR_NON_DEPRECATED},
    {"XBA", UCURR_UNCOMMON|UCURR_NON_DEPRECATED},
    {"XBB", UCURR_UNCOMMON|UCURR_NON_DEPRECATED},
    {"XBC", UCURR_UNCOMMON|UCURR_NON_DEPRECATED},
    {"XBD", UCURR_UNCOMMON|UCURR_NON_DEPRECATED},
    {"XCD", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"XDR", UCURR_UNCOMMON|UCURR_NON_DEPRECATED},
    {"XEU", UCURR_UNCOMMON|UCURR_DEPRECATED},
    {"XFO", UCURR_UNCOMMON|UCURR_NON_DEPRECATED},
    {"XFU", UCURR_UNCOMMON|UCURR_NON_DEPRECATED},
    {"XOF", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"XPD", UCURR_UNCOMMON|UCURR_NON_DEPRECATED},
    {"XPF", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"XPT", UCURR_UNCOMMON|UCURR_NON_DEPRECATED},
    {"XRE", UCURR_UNCOMMON|UCURR_NON_DEPRECATED},
    {"XTS", UCURR_UNCOMMON|UCURR_NON_DEPRECATED},
    {"XXX", UCURR_UNCOMMON|UCURR_NON_DEPRECATED},
    {"YDD", UCURR_COMMON|UCURR_DEPRECATED},
    {"YER", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"YUD", UCURR_COMMON|UCURR_DEPRECATED},
    {"YUM", UCURR_COMMON|UCURR_DEPRECATED},
    {"YUN", UCURR_COMMON|UCURR_DEPRECATED},
    {"ZAL", UCURR_UNCOMMON|UCURR_NON_DEPRECATED},
    {"ZAR", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"ZMK", UCURR_COMMON|UCURR_NON_DEPRECATED},
    {"ZRN", UCURR_COMMON|UCURR_DEPRECATED},
    {"ZRZ", UCURR_COMMON|UCURR_DEPRECATED},
    {"ZWD", UCURR_COMMON|UCURR_NON_DEPRECATED},
    { NULL, 0 } // Leave here to denote the end of the list.
};

#define UCURR_MATCHES_BITMASK(variable, typeToMatch) \
    ((typeToMatch) == UCURR_ALL || ((variable) & (typeToMatch)) == (typeToMatch))

static int32_t U_CALLCONV
ucurr_countCurrencyList(UEnumeration *enumerator, UErrorCode * /*pErrorCode*/) {
    UCurrencyContext *myContext = (UCurrencyContext *)(enumerator->context);
    uint32_t currType = myContext->currType;
    int32_t count = 0;

    /* Count the number of items matching the type we are looking for. */
    for (int32_t idx = 0; gCurrencyList[idx].currency != NULL; idx++) {
        if (UCURR_MATCHES_BITMASK(gCurrencyList[idx].currType, currType)) {
            count++;
        }
    }
    return count;
}

static const char* U_CALLCONV
ucurr_nextCurrencyList(UEnumeration *enumerator,
                        int32_t* resultLength,
                        UErrorCode * /*pErrorCode*/)
{
    UCurrencyContext *myContext = (UCurrencyContext *)(enumerator->context);

    /* Find the next in the list that matches the type we are looking for. */
    while (myContext->listIdx < (sizeof(gCurrencyList)/sizeof(gCurrencyList[0]))-1) {
        const struct CurrencyList *currItem = &gCurrencyList[myContext->listIdx++];
        if (UCURR_MATCHES_BITMASK(currItem->currType, myContext->currType))
        {
            if (resultLength) {
                *resultLength = 3; /* Currency codes are only 3 chars long */
            }
            return currItem->currency;
        }
    }
    /* We enumerated too far. */
    if (resultLength) {
        *resultLength = 0;
    }
    return NULL;
}

static void U_CALLCONV
ucurr_resetCurrencyList(UEnumeration *enumerator, UErrorCode * /*pErrorCode*/) {
    ((UCurrencyContext *)(enumerator->context))->listIdx = 0;
}

static void U_CALLCONV
ucurr_closeCurrencyList(UEnumeration *enumerator) {
    uprv_free(enumerator->context);
    uprv_free(enumerator);
}

static const UEnumeration gEnumCurrencyList = {
    NULL,
    NULL,
    ucurr_closeCurrencyList,
    ucurr_countCurrencyList,
    uenum_unextDefault,
    ucurr_nextCurrencyList,
    ucurr_resetCurrencyList
};
U_CDECL_END

U_CAPI UEnumeration * U_EXPORT2
ucurr_openISOCurrencies(uint32_t currType, UErrorCode *pErrorCode) {
    UEnumeration *myEnum = NULL;
    UCurrencyContext *myContext;

    myEnum = (UEnumeration*)uprv_malloc(sizeof(UEnumeration));
    if (myEnum == NULL) {
        *pErrorCode = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }
    uprv_memcpy(myEnum, &gEnumCurrencyList, sizeof(UEnumeration));
    myContext = (UCurrencyContext*)uprv_malloc(sizeof(UCurrencyContext));
    if (myContext == NULL) {
        *pErrorCode = U_MEMORY_ALLOCATION_ERROR;
        uprv_free(myEnum);
        return NULL;
    }
    myContext->currType = currType;
    myContext->listIdx = 0;
    myEnum->context = myContext;
    return myEnum;
}

U_CAPI int32_t U_EXPORT2
ucurr_countCurrencies(const char* locale, 
                 UDate date, 
                 UErrorCode* ec)
{
    int32_t currCount = 0;
    int32_t resLen = 0;
    const UChar* s = NULL;

    if (ec != NULL && U_SUCCESS(*ec)) 
    {
        // local variables
        UErrorCode localStatus = U_ZERO_ERROR;
        char id[ULOC_FULLNAME_CAPACITY];
        resLen = uloc_getKeywordValue(locale, "currency", id, ULOC_FULLNAME_CAPACITY, &localStatus);
        // get country or country_variant in `id'
        uint32_t variantType = idForLocale(locale, id, sizeof(id), ec);

        if (U_FAILURE(*ec))
        {
            return 0;
        }

        // Remove variants, which is only needed for registration.
        char *idDelim = strchr(id, VAR_DELIM);
        if (idDelim)
        {
            idDelim[0] = 0;
        }

        // Look up the CurrencyMap element in the root bundle.
        UResourceBundle *rb = ures_openDirect(NULL, CURRENCY_DATA, &localStatus);
        UResourceBundle *cm = ures_getByKey(rb, CURRENCY_MAP, rb, &localStatus);

        // Using the id derived from the local, get the currency data
        UResourceBundle *countryArray = ures_getByKey(rb, id, cm, &localStatus);

        // process each currency to see which one is valid for the given date
        if (U_SUCCESS(localStatus))
        {
            for (int32_t i=0; i<ures_getSize(countryArray); i++)
            {
                // get the currency resource
                UResourceBundle *currencyRes = ures_getByIndex(countryArray, i, NULL, &localStatus);
                s = ures_getStringByKey(currencyRes, "id", &resLen, &localStatus);

                // get the from date
                int32_t fromLength = 0;
                UResourceBundle *fromRes = ures_getByKey(currencyRes, "from", NULL, &localStatus);
                const int32_t *fromArray = ures_getIntVector(fromRes, &fromLength, &localStatus);

                int64_t currDate64 = (int64_t)fromArray[0] << 32;
                currDate64 |= ((int64_t)fromArray[1] & (int64_t)INT64_C(0x00000000FFFFFFFF));
                UDate fromDate = (UDate)currDate64;

                if (ures_getSize(currencyRes)> 2)
                {
                    int32_t toLength = 0;
                    UResourceBundle *toRes = ures_getByKey(currencyRes, "to", NULL, &localStatus);
                    const int32_t *toArray = ures_getIntVector(toRes, &toLength, &localStatus);

                    currDate64 = (int64_t)toArray[0] << 32;
                    currDate64 |= ((int64_t)toArray[1] & (int64_t)INT64_C(0x00000000FFFFFFFF));
                    UDate toDate = (UDate)currDate64;

                    if ((fromDate <= date) && (date < toDate))
                    {
                        currCount++;
                    }

                    ures_close(toRes);
                }
                else
                {
                    if (fromDate <= date)
                    {
                        currCount++;
                    }
                }

                // close open resources
                ures_close(currencyRes);
                ures_close(fromRes);

            } // end For loop
        } // end if (U_SUCCESS(localStatus))

        ures_close(countryArray);

        // Check for errors
        if (*ec == U_ZERO_ERROR || localStatus != U_ZERO_ERROR)
        {
            // There is nothing to fallback to. 
            // Report the failure/warning if possible.
            *ec = localStatus;
        }

        if (U_SUCCESS(*ec))
        {
            // no errors
            return currCount;
        }

    }

    // If we got here, either error code is invalid or
    // some argument passed is no good.
    return 0;
}

U_CAPI int32_t U_EXPORT2 
ucurr_forLocaleAndDate(const char* locale, 
                UDate date, 
                int32_t index,
                UChar* buff, 
                int32_t buffCapacity, 
                UErrorCode* ec)
{
    int32_t resLen = 0;
	int32_t currIndex = 0;
    const UChar* s = NULL;

    if (ec != NULL && U_SUCCESS(*ec))
    {
        // check the arguments passed
        if ((buff && buffCapacity) || !buffCapacity )
        {
            // local variables
            UErrorCode localStatus = U_ZERO_ERROR;
            char id[ULOC_FULLNAME_CAPACITY];
            resLen = uloc_getKeywordValue(locale, "currency", id, ULOC_FULLNAME_CAPACITY, &localStatus);

            // get country or country_variant in `id'
            uint32_t variantType = idForLocale(locale, id, sizeof(id), ec);
            if (U_FAILURE(*ec))
            {
                return 0;
            }

            // Remove variants, which is only needed for registration.
            char *idDelim = strchr(id, VAR_DELIM);
            if (idDelim)
            {
                idDelim[0] = 0;
            }

            // Look up the CurrencyMap element in the root bundle.
            UResourceBundle *rb = ures_openDirect(NULL, CURRENCY_DATA, &localStatus);
            UResourceBundle *cm = ures_getByKey(rb, CURRENCY_MAP, rb, &localStatus);

            // Using the id derived from the local, get the currency data
            UResourceBundle *countryArray = ures_getByKey(rb, id, cm, &localStatus);

            // process each currency to see which one is valid for the given date
            bool matchFound = false;
            if (U_SUCCESS(localStatus))
            {
                if ((index <= 0) || (index> ures_getSize(countryArray)))
                {
                    // requested index is out of bounds
                    ures_close(countryArray);
                    return 0;
                }

                for (int32_t i=0; i<ures_getSize(countryArray); i++)
                {
                    // get the currency resource
                    UResourceBundle *currencyRes = ures_getByIndex(countryArray, i, NULL, &localStatus);
                    s = ures_getStringByKey(currencyRes, "id", &resLen, &localStatus);

                    // get the from date
                    int32_t fromLength = 0;
                    UResourceBundle *fromRes = ures_getByKey(currencyRes, "from", NULL, &localStatus);
                    const int32_t *fromArray = ures_getIntVector(fromRes, &fromLength, &localStatus);

                    int64_t currDate64 = (int64_t)fromArray[0] << 32;
                    currDate64 |= ((int64_t)fromArray[1] & (int64_t)INT64_C(0x00000000FFFFFFFF));
                    UDate fromDate = (UDate)currDate64;

                    if (ures_getSize(currencyRes)> 2)
                    {
                        int32_t toLength = 0;
                        UResourceBundle *toRes = ures_getByKey(currencyRes, "to", NULL, &localStatus);
                        const int32_t *toArray = ures_getIntVector(toRes, &toLength, &localStatus);

                        currDate64 = (int64_t)toArray[0] << 32;
                        currDate64 |= ((int64_t)toArray[1] & (int64_t)INT64_C(0x00000000FFFFFFFF));
                        UDate toDate = (UDate)currDate64;

                        if ((fromDate <= date) && (date < toDate))
                        {
                            currIndex++;
                            if (currIndex == index)
                            {
                                matchFound = true;
                            }
                        }

                        ures_close(toRes);
                    }
                    else
                    {
                        if (fromDate <= date)
                        {
                            currIndex++;
                            if (currIndex == index)
                            {
                                matchFound = true;
                            }
                        }
                    }

                    // close open resources
                    ures_close(currencyRes);
                    ures_close(fromRes);

                    // check for loop exit
                    if (matchFound)
                    {
                        break;
                    }

                } // end For loop
            }

            ures_close(countryArray);

            // Check for errors
            if (*ec == U_ZERO_ERROR || localStatus != U_ZERO_ERROR)
            {
                // There is nothing to fallback to. 
                // Report the failure/warning if possible.
                *ec = localStatus;
            }

            if (U_SUCCESS(*ec))
            {
                // no errors
                if((buffCapacity> resLen) && matchFound)
                {
                    // write out the currency value
                    u_strcpy(buff, s);
                }
                else
                {
                    return 0;
                }
            }

            // return null terminated currency string
            return u_terminateUChars(buff, buffCapacity, resLen, ec);
        }
        else
        {
            // illegal argument encountered
            *ec = U_ILLEGAL_ARGUMENT_ERROR;
        }

    }

    // If we got here, either error code is invalid or
    // some argument passed is no good.
    return resLen;
}

#endif /* #if !UCONFIG_NO_FORMATTING */

//eof
