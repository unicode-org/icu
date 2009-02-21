/*
*******************************************************************************
* Copyright (C) 2007-2009, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "zonemeta.h"

#include "unicode/timezone.h"
#include "unicode/ustring.h"
#include "unicode/putil.h"

#include "umutex.h"
#include "uvector.h"
#include "cmemory.h"
#include "gregoimp.h"
#include "cstring.h"
#include "ucln_in.h"

// Metazone mapping tables
static UMTX gZoneMetaLock = NULL;
static UHashtable *gCanonicalMap = NULL;
static UHashtable *gOlsonToMeta = NULL;
static UHashtable *gMetaToOlson = NULL;
static UBool gCanonicalMapInitialized = FALSE;
static UBool gOlsonToMetaInitialized = FALSE;
static UBool gMetaToOlsonInitialized = FALSE;

U_CDECL_BEGIN
/**
 * Cleanup callback func
 */
static UBool U_CALLCONV zoneMeta_cleanup(void)
{
     umtx_destroy(&gZoneMetaLock);

    if (gCanonicalMap != NULL) {
        uhash_close(gCanonicalMap);
        gCanonicalMap = NULL;
    }
    gCanonicalMapInitialized = FALSE;

    if (gOlsonToMeta != NULL) {
        uhash_close(gOlsonToMeta);
        gOlsonToMeta = NULL;
    }
    gOlsonToMetaInitialized = FALSE;

    if (gMetaToOlson != NULL) {
        uhash_close(gMetaToOlson);
        gMetaToOlson = NULL;
    }
    gMetaToOlsonInitialized = FALSE;

    return TRUE;
}

/**
 * Deleter for UChar* string
 */
static void U_CALLCONV
deleteUCharString(void *obj) {
    UChar *entry = (UChar*)obj;
    uprv_free(entry);
}

/**
 * Deleter for UVector
 */
static void U_CALLCONV
deleteUVector(void *obj) {
   delete (U_NAMESPACE_QUALIFIER UVector*) obj;
}

/**
 * Deleter for CanonicalMapEntry
 */
static void U_CALLCONV
deleteCanonicalMapEntry(void *obj) {
    U_NAMESPACE_QUALIFIER CanonicalMapEntry *entry = (U_NAMESPACE_QUALIFIER CanonicalMapEntry*)obj;
    uprv_free(entry->id);
    uprv_free(entry);
}

/**
 * Deleter for OlsonToMetaMappingEntry
 */
static void U_CALLCONV
deleteOlsonToMetaMappingEntry(void *obj) {
    U_NAMESPACE_QUALIFIER OlsonToMetaMappingEntry *entry = (U_NAMESPACE_QUALIFIER OlsonToMetaMappingEntry*)obj;
    uprv_free(entry);
}

/**
 * Deleter for MetaToOlsonMappingEntry
 */
static void U_CALLCONV
deleteMetaToOlsonMappingEntry(void *obj) {
    U_NAMESPACE_QUALIFIER MetaToOlsonMappingEntry *entry = (U_NAMESPACE_QUALIFIER MetaToOlsonMappingEntry*)obj;
    uprv_free(entry->territory);
    uprv_free(entry);
}
U_CDECL_END

U_NAMESPACE_BEGIN

#define ZID_KEY_MAX 128
static const char gZoneStringsTag[]     = "zoneStrings";
static const char gUseMetazoneTag[]     = "um";

static const char gSupplementalData[]   = "supplementalData";
static const char gMapTimezonesTag[]    = "mapTimezones";
static const char gMetazonesTag[]       = "metazones";
static const char gZoneFormattingTag[]  = "zoneFormatting";
static const char gTerritoryTag[]       = "territory";
static const char gAliasesTag[]         = "aliases";
static const char gMultizoneTag[]       = "multizone";

static const char gMetazoneInfo[]       = "metazoneInfo";
static const char gMetazoneMappings[]   = "metazoneMappings";

#define MZID_PREFIX_LEN 5
static const char gMetazoneIdPrefix[]   = "meta:";

static const UChar gWorld[] = {0x30, 0x30, 0x31, 0x00}; // "001"

#define ASCII_DIGIT(c) (((c)>=0x30 && (c)<=0x39) ? (c)-0x30 : -1)

/*
 * Convert a date string used by metazone mappings to UDate.
 * The format used by CLDR metazone mapping is "yyyy-MM-dd HH:mm".
 */
static UDate
parseDate (const UChar *text, UErrorCode &status) {
    if (U_FAILURE(status)) {
        return 0;
    }
    int32_t len = u_strlen(text);
    if (len != 16 && len != 10) {
        // It must be yyyy-MM-dd HH:mm (length 16) or yyyy-MM-dd (length 10)
        status = U_INVALID_FORMAT_ERROR;
        return 0;
    }

    int32_t year = 0, month = 0, day = 0, hour = 0, min = 0, n;
    int32_t idx;

    // "yyyy" (0 - 3)
    for (idx = 0; idx <= 3 && U_SUCCESS(status); idx++) {
        n = ASCII_DIGIT(text[idx]);
        if (n >= 0) {
            year = 10*year + n;
        } else {
            status = U_INVALID_FORMAT_ERROR;
        }
    }
    // "MM" (5 - 6)
    for (idx = 5; idx <= 6 && U_SUCCESS(status); idx++) {
        n = ASCII_DIGIT(text[idx]);
        if (n >= 0) {
            month = 10*month + n;
        } else {
            status = U_INVALID_FORMAT_ERROR;
        }
    }
    // "dd" (8 - 9)
    for (idx = 8; idx <= 9 && U_SUCCESS(status); idx++) {
        n = ASCII_DIGIT(text[idx]);
        if (n >= 0) {
            day = 10*day + n;
        } else {
            status = U_INVALID_FORMAT_ERROR;
        }
    }
    if (len == 16) {
        // "HH" (11 - 12)
        for (idx = 11; idx <= 12 && U_SUCCESS(status); idx++) {
            n = ASCII_DIGIT(text[idx]);
            if (n >= 0) {
                hour = 10*hour + n;
            } else {
                status = U_INVALID_FORMAT_ERROR;
            }
        }
        // "mm" (14 - 15)
        for (idx = 14; idx <= 15 && U_SUCCESS(status); idx++) {
            n = ASCII_DIGIT(text[idx]);
            if (n >= 0) {
                min = 10*min + n;
            } else {
                status = U_INVALID_FORMAT_ERROR;
            }
        }
    }

    if (U_SUCCESS(status)) {
        UDate date = Grego::fieldsToDay(year, month - 1, day) * U_MILLIS_PER_DAY
            + hour * U_MILLIS_PER_HOUR + min * U_MILLIS_PER_MINUTE;
        return date;
    }
    return 0;
}

UHashtable*
ZoneMeta::createCanonicalMap(void) {
    UErrorCode status = U_ZERO_ERROR;

    UHashtable *canonicalMap = NULL;
    UResourceBundle *zoneFormatting = NULL;
    UResourceBundle *tzitem = NULL;
    UResourceBundle *aliases = NULL;

    StringEnumeration* tzenum = NULL;
    int32_t numZones;

    canonicalMap = uhash_open(uhash_hashUChars, uhash_compareUChars, NULL, &status);
    if (U_FAILURE(status)) {
        return NULL;
    }
    uhash_setKeyDeleter(canonicalMap, deleteUCharString);
    uhash_setValueDeleter(canonicalMap, deleteCanonicalMapEntry);

    zoneFormatting = ures_openDirect(NULL, gSupplementalData, &status);
    zoneFormatting = ures_getByKey(zoneFormatting, gZoneFormattingTag, zoneFormatting, &status);
    if (U_FAILURE(status)) {
        goto error_cleanup;
    }

    while (ures_hasNext(zoneFormatting)) {
        tzitem = ures_getNextResource(zoneFormatting, tzitem, &status);
        if (U_FAILURE(status)) {
            status = U_ZERO_ERROR;
            continue;
        }
        if (ures_getType(tzitem) != URES_TABLE) {
            continue;
        }

        int32_t territoryLen;
        const UChar *territory = ures_getStringByKey(tzitem, gTerritoryTag, &territoryLen, &status);
        if (U_FAILURE(status)) {
            territory = NULL;
            status = U_ZERO_ERROR;
        }

        int32_t tzidLen = 0;
        char tzid[ZID_KEY_MAX];
        const char *tzkey = ures_getKey(tzitem);
        uprv_strcpy(tzid, tzkey);
        // Replace ':' with '/'
        char *p = tzid;
        while (*p) {
            if (*p == ':') {
                *p = '/';
            }
            p++;
            tzidLen++;
        }
        tzidLen++; // Add one for NUL terminator

        // Create canonical map entry
        CanonicalMapEntry *entry = (CanonicalMapEntry*)uprv_malloc(sizeof(CanonicalMapEntry));
        if (entry == NULL) {
            status = U_MEMORY_ALLOCATION_ERROR;
            goto error_cleanup;
        }
        entry->id = (UChar*)uprv_malloc(tzidLen * sizeof(UChar));
        if (entry->id == NULL) {
            status = U_MEMORY_ALLOCATION_ERROR;
            uprv_free(entry);
            goto error_cleanup;
        }
        u_charsToUChars(tzid, entry->id, tzidLen);

        if (territory == NULL || u_strcmp(territory, gWorld) == 0) {
            entry->country = NULL;
        } else {
            entry->country = territory;
        }

        // Put this entry in the hashtable
        UChar *key = (UChar*)uprv_malloc(tzidLen * sizeof(UChar));
        if (key == NULL) {
            status = U_MEMORY_ALLOCATION_ERROR;
            deleteCanonicalMapEntry(entry);
            goto error_cleanup;
        }
        u_strncpy(key, entry->id, tzidLen);
        uhash_put(canonicalMap, key, entry, &status);
        if (U_FAILURE(status)) {
            goto error_cleanup;
        }

        // Get aliases
        aliases = ures_getByKey(tzitem, gAliasesTag, aliases, &status);
        if (U_FAILURE(status)) {
            // No aliases
            status = U_ZERO_ERROR;
            continue;
        }

        while (ures_hasNext(aliases)) {
            const UChar* alias = ures_getNextString(aliases, NULL, NULL, &status);
            if (U_FAILURE(status)) {
                status = U_ZERO_ERROR;
                continue;
            }
            // Create canonical map entry for this alias
            entry = (CanonicalMapEntry*)uprv_malloc(sizeof(CanonicalMapEntry));
            if (entry == NULL) {
                status = U_MEMORY_ALLOCATION_ERROR;
                goto error_cleanup;
            }
            entry->id = (UChar*)uprv_malloc(tzidLen * sizeof(UChar));
            if (entry->id == NULL) {
                status = U_MEMORY_ALLOCATION_ERROR;
                uprv_free(entry);
                goto error_cleanup;
            }
            u_charsToUChars(tzid, entry->id, tzidLen);

            if (territory  == NULL || u_strcmp(territory, gWorld) == 0) {
                entry->country = NULL;
            } else {
                entry->country = territory;
            }

            // Put this entry in the hashtable
            int32_t aliasLen = u_strlen(alias) + 1; // Add one for NUL terminator
            key = (UChar*)uprv_malloc(aliasLen * sizeof(UChar));
            if (key == NULL) {
                status = U_MEMORY_ALLOCATION_ERROR;
                deleteCanonicalMapEntry(entry);
                goto error_cleanup;
            }
            u_strncpy(key, alias, aliasLen);
            uhash_put(canonicalMap, key, entry, &status);
            if (U_FAILURE(status)) {
                goto error_cleanup;
            }
        }
    }

    // Some available Olson zones are not included in CLDR data (such as Asia/Riyadh87).
    // Also, when we update Olson tzdata, new zones may be added.
    // This code scans all available zones in zoneinfo.res, and if any of them are
    // missing, add them to the map.
    tzenum = TimeZone::createEnumeration();
    numZones = tzenum->count(status);
    if (U_SUCCESS(status)) {
        int32_t i;
        for (i = 0; i < numZones; i++) {
            const UnicodeString *zone = tzenum->snext(status);
            if (U_FAILURE(status)) {
                // We should not get here.
                status = U_ZERO_ERROR;
                continue;
            }
            UChar zoneUChars[ZID_KEY_MAX];
            int32_t zoneUCharsLen = zone->extract(zoneUChars, ZID_KEY_MAX, status) + 1; // Add one for NUL termination
            if (U_FAILURE(status) || status==U_STRING_NOT_TERMINATED_WARNING) {
                status = U_ZERO_ERROR;
                continue; // zone id is too long to extract
            }
            CanonicalMapEntry *entry = (CanonicalMapEntry*)uhash_get(canonicalMap, zoneUChars);
            if (entry) {
                // Already included in CLDR data
                continue;
            }
            // Not in CLDR data, but it could be new one whose alias is available
            // in CLDR.
            int32_t nTzdataEquivalent = TimeZone::countEquivalentIDs(*zone);
            int32_t j;
            for (j = 0; j < nTzdataEquivalent; j++) {
                UnicodeString alias = TimeZone::getEquivalentID(*zone, j);
                if (alias == *zone) {
                    continue;
                }
                UChar aliasUChars[ZID_KEY_MAX];
                alias.extract(aliasUChars, ZID_KEY_MAX, status);
                if (U_FAILURE(status) || status==U_STRING_NOT_TERMINATED_WARNING) {
                    status = U_ZERO_ERROR;
                    continue; // zone id is too long to extract
                }
                entry = (CanonicalMapEntry*)uhash_get(canonicalMap, aliasUChars);
                if (entry != NULL) {
                    break;
                }
            }
            // Create a new map entry
            CanonicalMapEntry* newEntry = (CanonicalMapEntry*)uprv_malloc(sizeof(CanonicalMapEntry));
            int32_t idLen;
            if (newEntry == NULL) {
                status = U_MEMORY_ALLOCATION_ERROR;
                goto error_cleanup;
            }
            if (entry == NULL) {
                // Set dereferenced zone ID as the canonical ID
                UnicodeString derefZone;
                TimeZone::dereferOlsonLink(*zone, derefZone);
                if (derefZone.length() == 0) {
                    // It should never happen.. but just in case
                    derefZone = *zone;
                }
                idLen = derefZone.length() + 1;
                newEntry->id = (UChar*)uprv_malloc(idLen * sizeof(UChar));
                if (newEntry->id == NULL) {
                    status = U_MEMORY_ALLOCATION_ERROR;
                    uprv_free(newEntry);
                    goto error_cleanup;
                }
                // Copy NULL terminated string
                derefZone.extract(newEntry->id, idLen, status);
                if (U_FAILURE(status)) {
                    uprv_free(newEntry->id);
                    uprv_free(newEntry);
                    goto error_cleanup;
                }
                // No territory information available
                newEntry->country = NULL;
            } else {
                // Use the canonical ID in the existing entry
                idLen = u_strlen(entry->id) + 1;
                newEntry->id = (UChar*)uprv_malloc(idLen * sizeof(UChar));
                if (newEntry->id == NULL) {
                    status = U_MEMORY_ALLOCATION_ERROR;
                    uprv_free(newEntry);
                    goto error_cleanup;
                }
                // Duplicate the entry
                u_strcpy(newEntry->id, entry->id);
                newEntry->country = entry->country;
            }

            // Put this entry in the hashtable
            UChar *key = (UChar*)uprv_malloc(zoneUCharsLen * sizeof(UChar));
            if (key == NULL) {
                status = U_MEMORY_ALLOCATION_ERROR;
                deleteCanonicalMapEntry(newEntry);
                goto error_cleanup;
            }
            u_strncpy(key, zoneUChars, zoneUCharsLen);
            uhash_put(canonicalMap, key, newEntry, &status);
            if (U_FAILURE(status)) {
                goto error_cleanup;
            }
        }
    }

normal_cleanup:
    ures_close(aliases);
    ures_close(tzitem);
    ures_close(zoneFormatting);
    delete tzenum;
    return canonicalMap;

error_cleanup:
    if (canonicalMap != NULL) {
        uhash_close(canonicalMap);
        canonicalMap = NULL;
    }
    goto normal_cleanup;
}

/*
 * Creating Olson tzid to metazone mappings from resource (3.8.1 and beyond)
 */
UHashtable*
ZoneMeta::createOlsonToMetaMap(void) {
    UErrorCode status = U_ZERO_ERROR;

    UHashtable *olsonToMeta = NULL;
    UResourceBundle *metazoneMappings = NULL;
    UResourceBundle *zoneItem = NULL;
    UResourceBundle *mz = NULL;
    StringEnumeration *tzids = NULL;

    olsonToMeta = uhash_open(uhash_hashUChars, uhash_compareUChars, NULL, &status);
    if (U_FAILURE(status)) {
        return NULL;
    }
    uhash_setKeyDeleter(olsonToMeta, deleteUCharString);
    uhash_setValueDeleter(olsonToMeta, deleteUVector);

    // Read metazone mappings from metazoneInfo bundle
    metazoneMappings = ures_openDirect(NULL, gMetazoneInfo, &status);
    metazoneMappings = ures_getByKey(metazoneMappings, gMetazoneMappings, metazoneMappings, &status);
    if (U_FAILURE(status)) {
        goto error_cleanup;
    }

    // Walk through all canonical tzids
    char zidkey[ZID_KEY_MAX];

    tzids = TimeZone::createEnumeration();
    const UnicodeString *tzid;
    while ((tzid = tzids->snext(status))) {
        if (U_FAILURE(status)) {
            goto error_cleanup;
        }
        // We may skip aliases, because the bundle
        // contains only canonical IDs.  For now, try
        // all of them.
        tzid->extract(0, tzid->length(), zidkey, sizeof(zidkey), US_INV);
        zidkey[sizeof(zidkey)-1] = 0; // NULL terminate just in case.

        // Replace '/' with ':'
        UBool foundSep = FALSE;
        char *p = zidkey;
        while (*p) {
            if (*p == '/') {
                *p = ':';
                foundSep = TRUE;
            }
            p++;
        }
        if (!foundSep) {
            // A valid time zone key has at least one separator
            continue;
        }

        zoneItem = ures_getByKey(metazoneMappings, zidkey, zoneItem, &status);
        if (U_FAILURE(status)) {
            status = U_ZERO_ERROR;
            continue;
        }

        UVector *mzMappings = NULL;
        while (ures_hasNext(zoneItem)) {
            mz = ures_getNextResource(zoneItem, mz, &status);
            const UChar *mz_name = ures_getStringByIndex(mz, 0, NULL, &status);
            const UChar *mz_from = ures_getStringByIndex(mz, 1, NULL, &status);
            const UChar *mz_to   = ures_getStringByIndex(mz, 2, NULL, &status);

            if(U_FAILURE(status)){
                status = U_ZERO_ERROR;
                continue;
            }
            // We do not want to use SimpleDateformat to parse boundary dates,
            // because this code could be triggered by the initialization code
            // used by SimpleDateFormat.
            UDate from = parseDate(mz_from, status);
            UDate to = parseDate(mz_to, status);
            if (U_FAILURE(status)) {
                status = U_ZERO_ERROR;
                continue;
            }

            OlsonToMetaMappingEntry *entry = (OlsonToMetaMappingEntry*)uprv_malloc(sizeof(OlsonToMetaMappingEntry));
            if (entry == NULL) {
                status = U_MEMORY_ALLOCATION_ERROR;
                break;
            }
            entry->mzid = mz_name;
            entry->from = from;
            entry->to = to;

            if (mzMappings == NULL) {
                mzMappings = new UVector(deleteOlsonToMetaMappingEntry, NULL, status);
                if (U_FAILURE(status)) {
                    delete mzMappings;
                    deleteOlsonToMetaMappingEntry(entry);
                    uprv_free(entry);
                    break;
                }
            }

            mzMappings->addElement(entry, status);
            if (U_FAILURE(status)) {
                break;
            }
        }

        if (U_FAILURE(status)) {
            if (mzMappings != NULL) {
                delete mzMappings;
            }
            goto error_cleanup;
        }
        if (mzMappings != NULL) {
            // Add to hashtable
            int32_t tzidLen = tzid->length() + 1; // Add one for NUL terminator
            UChar *key = (UChar*)uprv_malloc(tzidLen * sizeof(UChar));
            if (key == NULL) {
                status = U_MEMORY_ALLOCATION_ERROR;
                delete mzMappings;
                goto error_cleanup;
            }
            tzid->extract(key, tzidLen, status);
            uhash_put(olsonToMeta, key, mzMappings, &status);
            if (U_FAILURE(status)) {
                goto error_cleanup;
            }
        }
    }

normal_cleanup:
    if (tzids != NULL) {
        delete tzids;
    }
    ures_close(zoneItem);
    ures_close(mz);
    ures_close(metazoneMappings);
    return olsonToMeta;

error_cleanup:
    if (olsonToMeta != NULL) {
        uhash_close(olsonToMeta);
        olsonToMeta = NULL;
    }
    goto normal_cleanup;
}

UHashtable*
ZoneMeta::createMetaToOlsonMap(void) {
    UErrorCode status = U_ZERO_ERROR;

    UHashtable *metaToOlson = NULL;
    UResourceBundle *metazones = NULL;
    UResourceBundle *mz = NULL;

    metaToOlson = uhash_open(uhash_hashUChars, uhash_compareUChars, NULL, &status);
    if (U_FAILURE(status)) {
        return NULL;
    }
    uhash_setKeyDeleter(metaToOlson, deleteUCharString);
    uhash_setValueDeleter(metaToOlson, deleteUVector);

    metazones = ures_openDirect(NULL, gSupplementalData, &status);
    metazones = ures_getByKey(metazones, gMapTimezonesTag, metazones, &status);
    metazones = ures_getByKey(metazones, gMetazonesTag, metazones, &status);
    if (U_FAILURE(status)) {
        goto error_cleanup;
    }

    while (ures_hasNext(metazones)) {
        mz = ures_getNextResource(metazones, mz, &status);
        if (U_FAILURE(status)) {
            status = U_ZERO_ERROR;
            continue;
        }
        const char *mzkey = ures_getKey(mz);
        if (uprv_strncmp(mzkey, gMetazoneIdPrefix, MZID_PREFIX_LEN) == 0) {
            const char *mzid = mzkey + MZID_PREFIX_LEN;
            const char *territory = uprv_strrchr(mzid, '_');
            int32_t mzidLen = 0;
            int32_t territoryLen = 0;
            if (territory) {
                mzidLen = territory - mzid;
                territory++;
                territoryLen = uprv_strlen(territory);
            }
            if (mzidLen > 0 && territoryLen > 0) {
                int32_t tzidLen;
                const UChar *tzid = ures_getStringByIndex(mz, 0, &tzidLen, &status);
                if (U_SUCCESS(status)) {
                    // Create MetaToOlsonMappingEntry
                    MetaToOlsonMappingEntry *entry = (MetaToOlsonMappingEntry*)uprv_malloc(sizeof(MetaToOlsonMappingEntry));
                    if (entry == NULL) {
                        status = U_MEMORY_ALLOCATION_ERROR;
                        goto error_cleanup;
                    }
                    entry->id = tzid;
                    entry->territory = (UChar*)uprv_malloc((territoryLen + 1) * sizeof(UChar));
                    if (entry->territory == NULL) {
                        status = U_MEMORY_ALLOCATION_ERROR;
                        uprv_free(entry);
                        goto error_cleanup;
                    }
                    u_charsToUChars(territory, entry->territory, territoryLen + 1);

                    // Check if mapping entries for metazone is already available
                    if (mzidLen < ZID_KEY_MAX) {
                        UChar mzidUChars[ZID_KEY_MAX];
                        u_charsToUChars(mzid, mzidUChars, mzidLen);
                        mzidUChars[mzidLen++] = 0; // Add NUL terminator
                        UVector *tzMappings = (UVector*)uhash_get(metaToOlson, mzidUChars);
                        if (tzMappings == NULL) {
                            // Create new UVector and put it into the hashtable
                            tzMappings = new UVector(deleteMetaToOlsonMappingEntry, NULL, status);
                            if (U_FAILURE(status)) {
                                deleteMetaToOlsonMappingEntry(entry);
                                goto error_cleanup;
                            }
                            UChar *key = (UChar*)uprv_malloc(mzidLen * sizeof(UChar));
                            if (key == NULL) {
                                status = U_MEMORY_ALLOCATION_ERROR;
                                delete tzMappings;
                                deleteMetaToOlsonMappingEntry(entry);
                                goto error_cleanup;
                            }
                            u_strncpy(key, mzidUChars, mzidLen);
                            uhash_put(metaToOlson, key, tzMappings, &status);
                            if (U_FAILURE(status)) {
                                goto error_cleanup;
                            }
                        }
                        tzMappings->addElement(entry, status);
                        if (U_FAILURE(status)) {
                            goto error_cleanup;
                        }
                    } else {
                        deleteMetaToOlsonMappingEntry(entry);
                    }
                } else {
                    status = U_ZERO_ERROR;
                }
            }
        }
    }

normal_cleanup:
    ures_close(mz);
    ures_close(metazones);
    return metaToOlson;

error_cleanup:
    if (metaToOlson != NULL) {
        uhash_close(metaToOlson);
        metaToOlson = NULL;
    }
    goto normal_cleanup;
}

 /*
 * Initialize global objects
 */
void
ZoneMeta::initializeCanonicalMap(void) {
    UBool initialized;
    UMTX_CHECK(&gZoneMetaLock, gCanonicalMapInitialized, initialized);
    if (initialized) {
        return;
    }
    // Initialize hash table
    UHashtable *tmpCanonicalMap = createCanonicalMap();

    umtx_lock(&gZoneMetaLock);
    if (!gCanonicalMapInitialized) {
        gCanonicalMap = tmpCanonicalMap;
        tmpCanonicalMap = NULL;
        gCanonicalMapInitialized = TRUE;
    }
    umtx_unlock(&gZoneMetaLock);

    // OK to call the following multiple times with the same function
    ucln_i18n_registerCleanup(UCLN_I18N_ZONEMETA, zoneMeta_cleanup);
    if (tmpCanonicalMap != NULL) {
        uhash_close(tmpCanonicalMap);
    }
}

void
ZoneMeta::initializeOlsonToMeta(void) {
    UBool initialized;
    UMTX_CHECK(&gZoneMetaLock, gOlsonToMetaInitialized, initialized);
    if (initialized) {
        return;
    }
    // Initialize hash tables
    UHashtable *tmpOlsonToMeta = createOlsonToMetaMap();

    umtx_lock(&gZoneMetaLock);
    if (!gOlsonToMetaInitialized) {
        gOlsonToMeta = tmpOlsonToMeta;
        tmpOlsonToMeta = NULL;
        gOlsonToMetaInitialized = TRUE;
    }
    umtx_unlock(&gZoneMetaLock);
    
    // OK to call the following multiple times with the same function
    ucln_i18n_registerCleanup(UCLN_I18N_ZONEMETA, zoneMeta_cleanup);
    if (tmpOlsonToMeta != NULL) {
        uhash_close(tmpOlsonToMeta);
    }
}

void
ZoneMeta::initializeMetaToOlson(void) {
    UBool initialized;
    UMTX_CHECK(&gZoneMetaLock, gMetaToOlsonInitialized, initialized);
    if (initialized) {
        return;
    }
    // Initialize hash table
    UHashtable *tmpMetaToOlson = createMetaToOlsonMap();

    umtx_lock(&gZoneMetaLock);
    if (!gMetaToOlsonInitialized) {
        gMetaToOlson = tmpMetaToOlson;
        tmpMetaToOlson = NULL;
        gMetaToOlsonInitialized = TRUE;
    }
    umtx_unlock(&gZoneMetaLock);
    
    // OK to call the following multiple times with the same function
    ucln_i18n_registerCleanup(UCLN_I18N_ZONEMETA, zoneMeta_cleanup);
    if (tmpMetaToOlson != NULL) {
        uhash_close(tmpMetaToOlson);
    }
}

UnicodeString& U_EXPORT2
ZoneMeta::getCanonicalSystemID(const UnicodeString &tzid, UnicodeString &systemID, UErrorCode& status) {
    const CanonicalMapEntry *entry = getCanonicalInfo(tzid);
    if (entry != NULL) {
        systemID.setTo(entry->id);
    } else {
        status = U_ILLEGAL_ARGUMENT_ERROR;
    }
    return systemID;
}

UnicodeString& U_EXPORT2
ZoneMeta::getCanonicalCountry(const UnicodeString &tzid, UnicodeString &canonicalCountry) {
    const CanonicalMapEntry *entry = getCanonicalInfo(tzid);
    if (entry != NULL && entry->country != NULL) {
        canonicalCountry.setTo(entry->country);
    } else {
        // Use the input tzid
        canonicalCountry.remove();
    }
    return canonicalCountry;
}

const CanonicalMapEntry* U_EXPORT2
ZoneMeta::getCanonicalInfo(const UnicodeString &tzid) {
    initializeCanonicalMap();
    CanonicalMapEntry *entry = NULL;
    if (gCanonicalMap != NULL) {
        UErrorCode status = U_ZERO_ERROR;
        UChar tzidUChars[ZID_KEY_MAX];
        tzid.extract(tzidUChars, ZID_KEY_MAX, status);
        if (U_SUCCESS(status) && status!=U_STRING_NOT_TERMINATED_WARNING) {
            entry = (CanonicalMapEntry*)uhash_get(gCanonicalMap, tzidUChars);
        }
    }
    return entry;
}

UnicodeString& U_EXPORT2
ZoneMeta::getSingleCountry(const UnicodeString &tzid, UnicodeString &country) {
    UErrorCode status = U_ZERO_ERROR;

    // Get canonical country for the zone
    getCanonicalCountry(tzid, country);

    if (!country.isEmpty()) { 
        UResourceBundle *supplementalDataBundle = ures_openDirect(NULL, gSupplementalData, &status);
        UResourceBundle *zoneFormatting = ures_getByKey(supplementalDataBundle, gZoneFormattingTag, NULL, &status);
        UResourceBundle *multizone = ures_getByKey(zoneFormatting, gMultizoneTag, NULL, &status);

        if (U_SUCCESS(status)) {
            while (ures_hasNext(multizone)) {
                int32_t len;
                const UChar* multizoneCountry = ures_getNextString(multizone, &len, NULL, &status);
                if (country.compare(multizoneCountry, len) == 0) {
                    // Included in the multizone country list
                    country.remove();
                    break;
                }
            }
        }

        ures_close(multizone);
        ures_close(zoneFormatting);
        ures_close(supplementalDataBundle);
    }

    return country;
}

UnicodeString& U_EXPORT2
ZoneMeta::getMetazoneID(const UnicodeString &tzid, UDate date, UnicodeString &result) {
    UBool isSet = FALSE;
    const UVector *mappings = getMetazoneMappings(tzid);
    if (mappings != NULL) {
        for (int32_t i = 0; i < mappings->size(); i++) {
            OlsonToMetaMappingEntry *mzm = (OlsonToMetaMappingEntry*)mappings->elementAt(i);
            if (mzm->from <= date && mzm->to > date) {
                result.setTo(mzm->mzid, -1);
                isSet = TRUE;
                break;
            }
        }
    }
    if (!isSet) {
        result.remove();
    }
    return result;
}

const UVector* U_EXPORT2
ZoneMeta::getMetazoneMappings(const UnicodeString &tzid) {
    initializeOlsonToMeta();
    const UVector *result = NULL;
    if (gOlsonToMeta != NULL) {
        UErrorCode status = U_ZERO_ERROR;
        UChar tzidUChars[ZID_KEY_MAX];
        tzid.extract(tzidUChars, ZID_KEY_MAX, status);
        if (U_SUCCESS(status) && status!=U_STRING_NOT_TERMINATED_WARNING) {
            result = (UVector*)uhash_get(gOlsonToMeta, tzidUChars);
        }
    }
    return result;
}

UnicodeString& U_EXPORT2
ZoneMeta::getZoneIdByMetazone(const UnicodeString &mzid, const UnicodeString &region, UnicodeString &result) {
    initializeMetaToOlson();
    UBool isSet = FALSE;
    if (gMetaToOlson != NULL) {
        UErrorCode status = U_ZERO_ERROR;
        UChar mzidUChars[ZID_KEY_MAX];
        mzid.extract(mzidUChars, ZID_KEY_MAX, status);
        if (U_SUCCESS(status) && status!=U_STRING_NOT_TERMINATED_WARNING) {
            UVector *mappings = (UVector*)uhash_get(gMetaToOlson, mzidUChars);
            if (mappings != NULL) {
                // Find a preferred time zone for the given region.
                for (int32_t i = 0; i < mappings->size(); i++) {
                    MetaToOlsonMappingEntry *olsonmap = (MetaToOlsonMappingEntry*)mappings->elementAt(i);
                    if (region.compare(olsonmap->territory, -1) == 0) {
                        result.setTo(olsonmap->id);
                        isSet = TRUE;
                        break;
                    } else if (u_strcmp(olsonmap->territory, gWorld) == 0) {
                        result.setTo(olsonmap->id);
                        isSet = TRUE;
                    }
                }
            }
        }
    }
    if (!isSet) {
        result.remove();
    }
    return result;
}


U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */
