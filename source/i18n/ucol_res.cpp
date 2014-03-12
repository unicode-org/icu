/*
*******************************************************************************
*   Copyright (C) 1996-2014, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*   file name:  ucol_res.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
* Description:
* This file contains dependencies that the collation run-time doesn't normally
* need. This mainly contains resource bundle usage and collation meta information
*
* Modification history
* Date        Name      Comments
* 1996-1999   various members of ICU team maintained C API for collation framework
* 02/16/2001  synwee    Added internal method getPrevSpecialCE
* 03/01/2001  synwee    Added maxexpansion functionality.
* 03/16/2001  weiv      Collation framework is rewritten in C and made UCA compliant
* 12/08/2004  grhoten   Split part of ucol.cpp into ucol_res.cpp
* 2012-2014   markus    Rewritten in C++ again.
*/

#include "unicode/utypes.h"

#if !UCONFIG_NO_COLLATION

#include "unicode/coll.h"
#include "unicode/localpointer.h"
#include "unicode/locid.h"
#include "unicode/tblcoll.h"
#include "unicode/ucol.h"
#include "unicode/uloc.h"
#include "unicode/unistr.h"
#include "unicode/ures.h"
#include "cmemory.h"
#include "cstring.h"
#include "collationdatareader.h"
#include "collationroot.h"
#include "collationtailoring.h"
#include "putilimp.h"
#include "uassert.h"
#include "ucln_in.h"
#include "ucol_imp.h"
#include "uenumimp.h"
#include "ulist.h"
#include "umutex.h"
#include "uresimp.h"
#include "ustrenum.h"
#include "utracimp.h"

#define LENGTHOF(array) (int32_t)(sizeof(array)/sizeof((array)[0]))

U_NAMESPACE_BEGIN

namespace {

static const UChar *rootRules = NULL;
static int32_t rootRulesLength = 0;
static UResourceBundle *rootBundle = NULL;
static UInitOnce gInitOnce = U_INITONCE_INITIALIZER;

}  // namespace

U_CDECL_BEGIN

static UBool U_CALLCONV
ucol_res_cleanup() {
    rootRules = NULL;
    rootRulesLength = 0;
    ures_close(rootBundle);
    rootBundle = NULL;
    gInitOnce.reset();
    return TRUE;
}

U_CDECL_END

void
CollationLoader::loadRootRules(UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return; }
    rootBundle = ures_open(U_ICUDATA_COLL, kRootLocaleName, &errorCode);
    if(U_FAILURE(errorCode)) { return; }
    rootRules = ures_getStringByKey(rootBundle, "UCARules", &rootRulesLength, &errorCode);
    if(U_FAILURE(errorCode)) {
        ures_close(rootBundle);
        rootBundle = NULL;
        return;
    }
    ucln_i18n_registerCleanup(UCLN_I18N_UCOL_RES, ucol_res_cleanup);
}

void
CollationLoader::appendRootRules(UnicodeString &s) {
    UErrorCode errorCode = U_ZERO_ERROR;
    umtx_initOnce(gInitOnce, CollationLoader::loadRootRules, errorCode);
    if(U_SUCCESS(errorCode)) {
        s.append(rootRules, rootRulesLength);
    }
}

UnicodeString *
CollationLoader::loadRules(const char *localeID, const char *collationType, UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return NULL; }
    U_ASSERT(collationType != NULL && *collationType != 0);

    LocalUResourceBundlePointer bundle(ures_open(U_ICUDATA_COLL, localeID, &errorCode));
    LocalUResourceBundlePointer collations(
            ures_getByKey(bundle.getAlias(), "collations", NULL, &errorCode));
    LocalUResourceBundlePointer data(
            ures_getByKeyWithFallback(collations.getAlias(), collationType, NULL, &errorCode));
    int32_t length;
    const UChar *s =  ures_getStringByKey(data.getAlias(), "Sequence", &length, &errorCode);
    if(U_FAILURE(errorCode)) { return NULL; }

    // No string pointer aliasing so that we need not hold onto the resource bundle.
    UnicodeString *rules = new UnicodeString(s, length);
    if(rules == NULL) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }
    return rules;
}

const CollationTailoring *
CollationLoader::loadTailoring(const Locale &locale, Locale &validLocale, UErrorCode &errorCode) {
    const CollationTailoring *root = CollationRoot::getRoot(errorCode);
    if(U_FAILURE(errorCode)) { return NULL; }
    const char *name = locale.getName();
    if(*name == 0 || uprv_strcmp(name, "root") == 0) {
        validLocale = Locale::getRoot();
        return root;
    }

    LocalUResourceBundlePointer bundle(ures_open(U_ICUDATA_COLL, name, &errorCode));
    if(errorCode == U_MISSING_RESOURCE_ERROR) {
        errorCode = U_USING_DEFAULT_WARNING;
        validLocale = Locale::getRoot();
        return root;
    }
    const char *vLocale = ures_getLocaleByType(bundle.getAlias(), ULOC_ACTUAL_LOCALE, &errorCode);
    if(U_FAILURE(errorCode)) { return NULL; }
    validLocale = Locale(vLocale);

    // There are zero or more tailorings in the collations table.
    LocalUResourceBundlePointer collations(
            ures_getByKey(bundle.getAlias(), "collations", NULL, &errorCode));
    if(errorCode == U_MISSING_RESOURCE_ERROR) {
        errorCode = U_USING_DEFAULT_WARNING;
        return root;
    }
    if(U_FAILURE(errorCode)) { return NULL; }

    // Fetch the collation type from the locale ID and the default type from the data.
    char type[16];
    int32_t typeLength = locale.getKeywordValue("collation", type, LENGTHOF(type) - 1, errorCode);
    if(U_FAILURE(errorCode)) {
        errorCode = U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }
    type[typeLength] = 0;  // in case of U_NOT_TERMINATED_WARNING
    char defaultType[16];
    {
        UErrorCode internalErrorCode = U_ZERO_ERROR;
        LocalUResourceBundlePointer def(
                ures_getByKeyWithFallback(collations.getAlias(), "default", NULL,
                                          &internalErrorCode));
        int32_t length;
        const UChar *s = ures_getString(def.getAlias(), &length, &internalErrorCode);
        if(U_SUCCESS(internalErrorCode) && length < LENGTHOF(defaultType)) {
            u_UCharsToChars(s, defaultType, length + 1);
        } else {
            uprv_strcpy(defaultType, "standard");
        }
    }
    if(typeLength == 0 || uprv_strcmp(type, "default") == 0) {
        uprv_strcpy(type, defaultType);
    }

    // Load the collations/type tailoring, with type fallback.
    UBool typeFallback = FALSE;
    LocalUResourceBundlePointer data(
            ures_getByKeyWithFallback(collations.getAlias(), type, NULL, &errorCode));
    if(errorCode == U_MISSING_RESOURCE_ERROR &&
            typeLength > 6 && uprv_strncmp(type, "search", 6) == 0) {
        // fall back from something like "searchjl" to "search"
        typeFallback = TRUE;
        type[6] = 0;
        errorCode = U_ZERO_ERROR;
        data.adoptInstead(
            ures_getByKeyWithFallback(collations.getAlias(), type, NULL, &errorCode));
    }
    if(errorCode == U_MISSING_RESOURCE_ERROR && uprv_strcmp(type, defaultType) != 0) {
        // fall back to the default type
        typeFallback = TRUE;
        uprv_strcpy(type, defaultType);
        errorCode = U_ZERO_ERROR;
        data.adoptInstead(
            ures_getByKeyWithFallback(collations.getAlias(), type, NULL, &errorCode));
    }
    if(errorCode == U_MISSING_RESOURCE_ERROR && uprv_strcmp(type, "standard") != 0) {
        // fall back to the "standard" type
        typeFallback = TRUE;
        uprv_strcpy(type, "standard");
        errorCode = U_ZERO_ERROR;
        data.adoptInstead(
            ures_getByKeyWithFallback(collations.getAlias(), type, NULL, &errorCode));
    }
    if(errorCode == U_MISSING_RESOURCE_ERROR) {
        errorCode = U_USING_DEFAULT_WARNING;
        return root;
    }
    if(U_FAILURE(errorCode)) { return NULL; }

    LocalPointer<CollationTailoring> t(new CollationTailoring(root->settings));
    if(t.isNull() || t->isBogus()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }

    // Is this the same as the root collator? If so, then use that instead.
    const char *actualLocale = ures_getLocaleByType(data.getAlias(), ULOC_ACTUAL_LOCALE, &errorCode);
    if(U_FAILURE(errorCode)) { return NULL; }
    if((*actualLocale == 0 || uprv_strcmp(actualLocale, "root") == 0) &&
            uprv_strcmp(type, "standard") == 0) {
        if(typeFallback) {
            errorCode = U_USING_DEFAULT_WARNING;
        }
        return root;
    }
    t->actualLocale = Locale(actualLocale);

    // deserialize
    LocalUResourceBundlePointer binary(
            ures_getByKey(data.getAlias(), "%%CollationBin", NULL, &errorCode));
    // Note: U_MISSING_RESOURCE_ERROR --> The old code built from rules if available
    // but that created undesirable dependencies.
    int32_t length;
    const uint8_t *inBytes = ures_getBinary(binary.getAlias(), &length, &errorCode);
    if(U_FAILURE(errorCode)) { return NULL; }
    CollationDataReader::read(root, inBytes, length, *t, errorCode);
    // Note: U_COLLATOR_VERSION_MISMATCH --> The old code built from rules if available
    // but that created undesirable dependencies.
    if(U_FAILURE(errorCode)) { return NULL; }

    // Try to fetch the optional rules string.
    {
        UErrorCode internalErrorCode = U_ZERO_ERROR;
        int32_t length;
        const UChar *s = ures_getStringByKey(data.getAlias(), "Sequence", &length,
                                             &internalErrorCode);
        if(U_SUCCESS(errorCode)) {
            t->rules.setTo(TRUE, s, length);
        }
    }

    // Set the collation types on the informational locales,
    // except when they match the default types (for brevity and backwards compatibility).
    // For the valid locale, suppress the default type.
    if(uprv_strcmp(type, defaultType) != 0) {
        validLocale.setKeywordValue("collation", type, errorCode);
        if(U_FAILURE(errorCode)) { return NULL; }
    }

    // For the actual locale, suppress the default type *according to the actual locale*.
    // For example, zh has default=pinyin and contains all of the Chinese tailorings.
    // zh_Hant has default=stroke but has no other data.
    // For the valid locale "zh_Hant" we need to suppress stroke.
    // For the actual locale "zh" we need to suppress pinyin instead.
    if(uprv_strcmp(actualLocale, vLocale) != 0) {
        // Opening a bundle for the actual locale should always succeed.
        LocalUResourceBundlePointer actualBundle(
                ures_open(U_ICUDATA_COLL, actualLocale, &errorCode));
        if(U_FAILURE(errorCode)) { return NULL; }
        UErrorCode internalErrorCode = U_ZERO_ERROR;
        LocalUResourceBundlePointer def(
                ures_getByKeyWithFallback(actualBundle.getAlias(), "collations/default", NULL,
                                          &internalErrorCode));
        int32_t length;
        const UChar *s = ures_getString(def.getAlias(), &length, &internalErrorCode);
        if(U_SUCCESS(internalErrorCode) && length < LENGTHOF(defaultType)) {
            u_UCharsToChars(s, defaultType, length + 1);
        } else {
            uprv_strcpy(defaultType, "standard");
        }
    }
    if(uprv_strcmp(type, defaultType) != 0) {
        t->actualLocale.setKeywordValue("collation", type, errorCode);
        if(U_FAILURE(errorCode)) { return NULL; }
    }

    if(typeFallback) {
        errorCode = U_USING_DEFAULT_WARNING;
    }
    t->bundle = bundle.orphan();
    return t.orphan();
}

U_NAMESPACE_END

U_NAMESPACE_USE

U_CAPI UCollator*
ucol_open(const char *loc,
          UErrorCode *status)
{
    U_NAMESPACE_USE

    UTRACE_ENTRY_OC(UTRACE_UCOL_OPEN);
    UTRACE_DATA1(UTRACE_INFO, "locale = \"%s\"", loc);
    UCollator *result = NULL;

    Collator *coll = Collator::createInstance(loc, *status);
    if(U_SUCCESS(*status)) {
        result = coll->toUCollator();
    }
    UTRACE_EXIT_PTR_STATUS(result, *status);
    return result;
}


U_CAPI int32_t U_EXPORT2
ucol_getDisplayName(    const    char        *objLoc,
                    const    char        *dispLoc,
                    UChar             *result,
                    int32_t         resultLength,
                    UErrorCode        *status)
{
    U_NAMESPACE_USE

    if(U_FAILURE(*status)) return -1;
    UnicodeString dst;
    if(!(result==NULL && resultLength==0)) {
        // NULL destination for pure preflighting: empty dummy string
        // otherwise, alias the destination buffer
        dst.setTo(result, 0, resultLength);
    }
    Collator::getDisplayName(Locale(objLoc), Locale(dispLoc), dst);
    return dst.extract(result, resultLength, *status);
}

U_CAPI const char* U_EXPORT2
ucol_getAvailable(int32_t index)
{
    int32_t count = 0;
    const Locale *loc = Collator::getAvailableLocales(count);
    if (loc != NULL && index < count) {
        return loc[index].getName();
    }
    return NULL;
}

U_CAPI int32_t U_EXPORT2
ucol_countAvailable()
{
    int32_t count = 0;
    Collator::getAvailableLocales(count);
    return count;
}

#if !UCONFIG_NO_SERVICE
U_CAPI UEnumeration* U_EXPORT2
ucol_openAvailableLocales(UErrorCode *status) {
    U_NAMESPACE_USE

    // This is a wrapper over Collator::getAvailableLocales()
    if (U_FAILURE(*status)) {
        return NULL;
    }
    StringEnumeration *s = icu::Collator::getAvailableLocales();
    if (s == NULL) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }
    return uenum_openFromStringEnumeration(s, status);
}
#endif

// Note: KEYWORDS[0] != RESOURCE_NAME - alan

static const char RESOURCE_NAME[] = "collations";

static const char* const KEYWORDS[] = { "collation" };

#define KEYWORD_COUNT LENGTHOF(KEYWORDS)

U_CAPI UEnumeration* U_EXPORT2
ucol_getKeywords(UErrorCode *status) {
    UEnumeration *result = NULL;
    if (U_SUCCESS(*status)) {
        return uenum_openCharStringsEnumeration(KEYWORDS, KEYWORD_COUNT, status);
    }
    return result;
}

U_CAPI UEnumeration* U_EXPORT2
ucol_getKeywordValues(const char *keyword, UErrorCode *status) {
    if (U_FAILURE(*status)) {
        return NULL;
    }
    // hard-coded to accept exactly one collation keyword
    // modify if additional collation keyword is added later
    if (keyword==NULL || uprv_strcmp(keyword, KEYWORDS[0])!=0)
    {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }
    return ures_getKeywordValues(U_ICUDATA_COLL, RESOURCE_NAME, status);
}

static const UEnumeration defaultKeywordValues = {
    NULL,
    NULL,
    ulist_close_keyword_values_iterator,
    ulist_count_keyword_values,
    uenum_unextDefault,
    ulist_next_keyword_value,
    ulist_reset_keyword_values_iterator
};

#include <stdio.h>

U_CAPI UEnumeration* U_EXPORT2
ucol_getKeywordValuesForLocale(const char* /*key*/, const char* locale,
                               UBool /*commonlyUsed*/, UErrorCode* status) {
    /* Get the locale base name. */
    char localeBuffer[ULOC_FULLNAME_CAPACITY] = "";
    uloc_getBaseName(locale, localeBuffer, sizeof(localeBuffer), status);

    /* Create the 2 lists
     * -values is the temp location for the keyword values
     * -results hold the actual list used by the UEnumeration object
     */
    UList *values = ulist_createEmptyList(status);
    UList *results = ulist_createEmptyList(status);
    UEnumeration *en = (UEnumeration *)uprv_malloc(sizeof(UEnumeration));
    if (U_FAILURE(*status) || en == NULL) {
        if (en == NULL) {
            *status = U_MEMORY_ALLOCATION_ERROR;
        } else {
            uprv_free(en);
        }
        ulist_deleteList(values);
        ulist_deleteList(results);
        return NULL;
    }

    memcpy(en, &defaultKeywordValues, sizeof(UEnumeration));
    en->context = results;

    /* Open the resource bundle for collation with the given locale. */
    UResourceBundle bundle, collations, collres, defres;
    ures_initStackObject(&bundle);
    ures_initStackObject(&collations);
    ures_initStackObject(&collres);
    ures_initStackObject(&defres);

    ures_openFillIn(&bundle, U_ICUDATA_COLL, localeBuffer, status);

    while (U_SUCCESS(*status)) {
        ures_getByKey(&bundle, RESOURCE_NAME, &collations, status);
        ures_resetIterator(&collations);
        while (U_SUCCESS(*status) && ures_hasNext(&collations)) {
            ures_getNextResource(&collations, &collres, status);
            const char *key = ures_getKey(&collres);
            /* If the key is default, get the string and store it in results list only
             * if results list is empty.
             */
            if (uprv_strcmp(key, "default") == 0) {
                if (ulist_getListSize(results) == 0) {
                    char *defcoll = (char *)uprv_malloc(sizeof(char) * ULOC_KEYWORDS_CAPACITY);
                    int32_t defcollLength = ULOC_KEYWORDS_CAPACITY;

                    ures_getNextResource(&collres, &defres, status);
#if U_CHARSET_FAMILY==U_ASCII_FAMILY
			/* optimize - use the utf-8 string */
                    ures_getUTF8String(&defres, defcoll, &defcollLength, TRUE, status);
#else
                    {
                       const UChar* defString = ures_getString(&defres, &defcollLength, status);
                       if(U_SUCCESS(*status)) {
			   if(defcollLength+1 > ULOC_KEYWORDS_CAPACITY) {
				*status = U_BUFFER_OVERFLOW_ERROR;
			   } else {
                           	u_UCharsToChars(defString, defcoll, defcollLength+1);
			   }
                       }
                    }
#endif	

                    ulist_addItemBeginList(results, defcoll, TRUE, status);
                }
            } else {
                ulist_addItemEndList(values, key, FALSE, status);
            }
        }

        /* If the locale is "" this is root so exit. */
        if (uprv_strlen(localeBuffer) == 0) {
            break;
        }
        /* Get the parent locale and open a new resource bundle. */
        uloc_getParent(localeBuffer, localeBuffer, sizeof(localeBuffer), status);
        ures_openFillIn(&bundle, U_ICUDATA_COLL, localeBuffer, status);
    }

    ures_close(&defres);
    ures_close(&collres);
    ures_close(&collations);
    ures_close(&bundle);

    if (U_SUCCESS(*status)) {
        char *value = NULL;
        ulist_resetList(values);
        while ((value = (char *)ulist_getNext(values)) != NULL) {
            if (!ulist_containsString(results, value, (int32_t)uprv_strlen(value))) {
                ulist_addItemEndList(results, value, FALSE, status);
                if (U_FAILURE(*status)) {
                    break;
                }
            }
        }
    }

    ulist_deleteList(values);

    if (U_FAILURE(*status)){
        uenum_close(en);
        en = NULL;
    } else {
        ulist_resetList(results);
    }

    return en;
}

U_CAPI int32_t U_EXPORT2
ucol_getFunctionalEquivalent(char* result, int32_t resultCapacity,
                             const char* keyword, const char* locale,
                             UBool* isAvailable, UErrorCode* status)
{
    // N.B.: Resource name is "collations" but keyword is "collation"
    return ures_getFunctionalEquivalent(result, resultCapacity, U_ICUDATA_COLL,
        "collations", keyword, locale,
        isAvailable, TRUE, status);
}

#endif /* #if !UCONFIG_NO_COLLATION */
