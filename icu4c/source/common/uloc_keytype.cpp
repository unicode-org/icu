/*
**********************************************************************
*   Copyright (C) 2014, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*/
#include "unicode/utypes.h"

#include "cstring.h"
#include "uassert.h"
#include "ucln_cmn.h"
#include "uhash.h"
#include "umutex.h"
#include "uresimp.h"
#include "uvector.h"

static UHashtable* gLocExtKeyMap = NULL;
static icu::UInitOnce gLocExtKeyMapInitOnce = U_INITONCE_INITIALIZER;
static icu::UVector* gKeyTypeStringPool = NULL;
static icu::UVector* gLocExtKeyDataEntries = NULL;
static icu::UVector* gLocExtTypeEntries = NULL;

// bit flags for special types
typedef enum {
    SPECIALTYPE_NONE = 0,
    SPECIALTYPE_CODEPOINTS = 1,
    SPECIALTYPE_REORDER_CODE = 2
} SpecialType;

typedef struct LocExtKeyData {
    const char*     legacyId;
    const char*     bcpId;
    UHashtable*     typeMap;
    uint32_t        specialTypes;
} LocExtKeyData;

typedef struct LocExtType {
    const char*     legacyId;
    const char*     bcpId;
} LocExtType;

U_CDECL_BEGIN

static UBool U_CALLCONV
uloc_key_type_cleanup(void) {
    if (gLocExtKeyMap != NULL) {
        uhash_close(gLocExtKeyMap);
        gLocExtKeyMap = NULL;
    }

    delete gLocExtKeyDataEntries;
    gLocExtKeyDataEntries = NULL;

    delete gLocExtTypeEntries;
    gLocExtTypeEntries = NULL;

    delete gKeyTypeStringPool;
    gKeyTypeStringPool = NULL;

    gLocExtKeyMapInitOnce.reset();
    return TRUE;
}

static void U_CALLCONV
uloc_deleteKeyTypeStringPoolEntry(void* obj) {
    uprv_free(obj);
}

static void U_CALLCONV
uloc_deleteKeyDataEntry(void* obj) {
    LocExtKeyData* keyData = (LocExtKeyData*)obj;
    if (keyData->typeMap != NULL) {
        uhash_close(keyData->typeMap);
    }
    uprv_free(keyData);
}

static void U_CALLCONV
uloc_deleteTypeEntry(void* obj) {
    uprv_free(obj);
}

U_CDECL_END


static void U_CALLCONV
initFromResourceBundle(UErrorCode& sts) {
    ucln_common_registerCleanup(UCLN_COMMON_LOCALE_KEY_TYPE, uloc_key_type_cleanup);

    gLocExtKeyMap = uhash_open(uhash_hashIChars, uhash_compareIChars, NULL, &sts);
    if (U_FAILURE(sts)) {
        return;
    }

    UResourceBundle *keyTypeDataRes = NULL;
    UResourceBundle *keyMapRes = NULL;
    UResourceBundle *typeMapRes = NULL;
    UResourceBundle *typeAliasRes = NULL;
    UResourceBundle *bcpTypeAliasRes = NULL;

    keyTypeDataRes = ures_openDirect(NULL, "keyTypeData", &sts);
    keyMapRes = ures_getByKey(keyTypeDataRes, "keyMap", NULL, &sts);
    typeMapRes = ures_getByKey(keyTypeDataRes, "typeMap", NULL, &sts);

    UErrorCode tmpSts = U_ZERO_ERROR;
    typeAliasRes = ures_getByKey(keyTypeDataRes, "typeAlias", NULL, &tmpSts);
    if (U_FAILURE(tmpSts)) {
        typeAliasRes = NULL;
        tmpSts = U_ZERO_ERROR;
    }
    bcpTypeAliasRes = ures_getByKey(keyTypeDataRes, "bcpTypeAlias", NULL, &tmpSts);
    if (U_FAILURE(tmpSts)) {
        bcpTypeAliasRes = NULL;
        tmpSts = U_ZERO_ERROR;
    }

    // initialize vectors storing dynamically allocated objects
    gKeyTypeStringPool = new icu::UVector(uloc_deleteKeyTypeStringPoolEntry, NULL, sts);
    if (gKeyTypeStringPool == NULL || U_FAILURE(sts)) {
        goto close_bundles;
    }
    gLocExtKeyDataEntries = new icu::UVector(uloc_deleteKeyDataEntry, NULL, sts);
    if (gLocExtKeyDataEntries == NULL || U_FAILURE(sts)) {
        goto close_bundles;
    }
    gLocExtTypeEntries = new icu::UVector(uloc_deleteTypeEntry, NULL, sts);
    if (gLocExtTypeEntries == NULL || U_FAILURE(sts)) {
        goto close_bundles;
    }

    // iterate through keyMap resource
    UResourceBundle keyMapEntry;
    ures_initStackObject(&keyMapEntry);

    while (ures_hasNext(keyMapRes)) {
        ures_getNextResource(keyMapRes, &keyMapEntry, &sts);
        if (U_FAILURE(sts)) {
            break;
        }
        const char* legacyKeyId = ures_getKey(&keyMapEntry);
        int32_t bcpKeyIdLen = 0;
        const UChar* uBcpKeyId = ures_getString(&keyMapEntry, &bcpKeyIdLen, &sts);
        if (U_FAILURE(sts)) {
            break;
        }

        // empty value indicates that BCP key is same with the legacy key.
        const char* bcpKeyId = legacyKeyId;
        if (bcpKeyIdLen > 0) {
            char* bcpKeyIdBuf = (char*)uprv_malloc(bcpKeyIdLen + 1);
            if (bcpKeyIdBuf == NULL) {
                sts = U_MEMORY_ALLOCATION_ERROR;
                break;
            }
            u_UCharsToChars(uBcpKeyId, bcpKeyIdBuf, bcpKeyIdLen);
            bcpKeyIdBuf[bcpKeyIdLen] = 0;
            gKeyTypeStringPool->addElement(bcpKeyIdBuf, sts);
            if (U_FAILURE(sts)) {
                break;
            }
            bcpKeyId = bcpKeyIdBuf;
        }

        UBool isTZ = uprv_strcmp(legacyKeyId, "timezone") == 0;

        UHashtable* typeDataMap = uhash_open(uhash_hashIChars, uhash_compareIChars, NULL, &sts);
        if (U_FAILURE(sts)) {
            break;
        }
        uint32_t specialTypes = SPECIALTYPE_NONE;

        UResourceBundle* typeAliasResByKey = NULL;
        UResourceBundle* bcpTypeAliasResByKey = NULL;

        if (typeAliasRes != NULL) {
            typeAliasResByKey = ures_getByKey(typeAliasRes, legacyKeyId, NULL, &tmpSts);
            if (U_FAILURE(tmpSts)) {
                // only a few keys have type alias mapping
                typeAliasResByKey = NULL;
                tmpSts = U_ZERO_ERROR;
            }
        }
        if (bcpTypeAliasRes != NULL) {
            bcpTypeAliasResByKey = ures_getByKey(bcpTypeAliasRes, bcpKeyId, NULL, &tmpSts);
            if (U_FAILURE(tmpSts)) {
                // only a few keys have BCP type alias mapping
                bcpTypeAliasResByKey = NULL;
                tmpSts = U_ZERO_ERROR;
            }
        }

        // look up type map for the key, and walk through the mapping data
        UResourceBundle* typeMapResByKey = ures_getByKey(typeMapRes, legacyKeyId, NULL, &tmpSts);
        if (U_FAILURE(tmpSts)) {
            // type map for each key must exist
            U_ASSERT(FALSE);
            tmpSts = U_ZERO_ERROR;
        } else {
            UResourceBundle typeMapEntry;
            ures_initStackObject(&typeMapEntry);

            while (ures_hasNext(typeMapResByKey)) {
                ures_getNextResource(typeMapResByKey, &typeMapEntry, &sts);
                if (U_FAILURE(sts)) {
                    break;
                }
                const char* legacyTypeId = ures_getKey(&typeMapEntry);

                // special types
                if (uprv_strcmp(legacyTypeId, "CODEPOINTS") == 0) {
                    specialTypes |= SPECIALTYPE_CODEPOINTS;
                    continue;
                }
                if (uprv_strcmp(legacyTypeId, "REORDER_CODE") == 0) {
                    specialTypes |= SPECIALTYPE_REORDER_CODE;
                    continue;
                }

                if (isTZ) {
                    // a timezone key uses a colon instead of a slash in the resource.
                    // e.g. America:Los_Angeles
                    if (uprv_strchr(legacyTypeId, ':') != NULL) {
                        int32_t legacyTypeIdLen = uprv_strlen(legacyTypeId);
                        char* legacyTypeIdBuf = (char*)uprv_malloc(legacyTypeIdLen + 1);
                        if (legacyTypeIdBuf == NULL) {
                            sts = U_MEMORY_ALLOCATION_ERROR;
                            break;
                        }
                        const char* p = legacyTypeId;
                        char* q = legacyTypeIdBuf;
                        while (*p) {
                            if (*p == ':') {
                                *q++ = '/';
                            } else {
                                *q++ = *p;
                            }
                            p++;
                        }
                        *q = 0;

                        gKeyTypeStringPool->addElement(legacyTypeIdBuf, sts);
                        if (U_FAILURE(sts)) {
                            break;
                        }
                        legacyTypeId = legacyTypeIdBuf;
                    }
                }

                int32_t bcpTypeIdLen = 0;
                const UChar* uBcpTypeId = ures_getString(&typeMapEntry, &bcpTypeIdLen, &sts);
                if (U_FAILURE(sts)) {
                    break;
                }

                // empty value indicates that BCP type is same with the legacy type.
                const char* bcpTypeId = legacyTypeId;
                if (bcpTypeIdLen > 0) {
                    char* bcpTypeIdBuf = (char*)uprv_malloc(bcpTypeIdLen + 1);
                    if (bcpTypeIdBuf == NULL) {
                        sts = U_MEMORY_ALLOCATION_ERROR;
                        break;
                    }
                    u_UCharsToChars(uBcpTypeId, bcpTypeIdBuf, bcpTypeIdLen);
                    bcpTypeIdBuf[bcpTypeIdLen] = 0;
                    gKeyTypeStringPool->addElement(bcpTypeIdBuf, sts);
                    if (U_FAILURE(sts)) {
                        break;
                    }
                    bcpTypeId = bcpTypeIdBuf;
                }

                // Note: legacy type value should never be
                // equivalent to bcp type value of a different
                // type under the same key. So we use a single
                // map for lookup.
                LocExtType* t = (LocExtType*)uprv_malloc(sizeof(LocExtType));
                if (t == NULL) {
                    sts = U_MEMORY_ALLOCATION_ERROR;
                    break;
                }
                t->bcpId = bcpTypeId;
                t->legacyId = legacyTypeId;
                gLocExtTypeEntries->addElement((void*)t, sts);
                if (U_FAILURE(sts)) {
                    break;
                }

                uhash_put(typeDataMap, (void*)legacyTypeId, t, &sts);
                if (bcpTypeId != legacyTypeId) {
                    // different type value
                    uhash_put(typeDataMap, (void*)bcpTypeId, t, &sts);
                }
                if (U_FAILURE(sts)) {
                    break;
                }

                // also put aliases in the map
                if (typeAliasResByKey != NULL) {
                    UResourceBundle typeAliasDataEntry;
                    ures_initStackObject(&typeAliasDataEntry);

                    ures_resetIterator(typeAliasResByKey);
                    while (ures_hasNext(typeAliasResByKey) && U_SUCCESS(sts)) {
                        int32_t toLen;
                        ures_getNextResource(typeAliasResByKey, &typeAliasDataEntry, &sts);
                        const UChar* to = ures_getString(&typeAliasDataEntry, &toLen, &sts);
                        if (U_FAILURE(sts)) {
                            break;
                        }
                        // check if this is an alias of canoncal legacy type
                        if (uprv_compareInvAscii(NULL, legacyTypeId, -1, to, toLen) == 0) {
                            const char* from = ures_getKey(&typeAliasDataEntry);
                            if (isTZ) {
                                // replace colon with slash if necessary
                                if (uprv_strchr(from, ':') != NULL) {
                                    int32_t fromLen = uprv_strlen(from);
                                    char* fromBuf = (char*)uprv_malloc(fromLen + 1);
                                    if (fromBuf == NULL) {
                                        sts = U_MEMORY_ALLOCATION_ERROR;
                                        break;
                                    }
                                    const char* p = from;
                                    char* q = fromBuf;
                                    while (*p) {
                                        if (*p == ':') {
                                            *q++ = '/';
                                        } else {
                                            *q++ = *p;
                                        }
                                        p++;
                                    }
                                    *q = 0;

                                    gKeyTypeStringPool->addElement(fromBuf, sts);
                                    if (U_FAILURE(sts)) {
                                        break;
                                    }
                                    from = fromBuf;
                                }
                            }
                            uhash_put(typeDataMap, (void*)from, t, &sts);
                        }
                    }
                    ures_close(&typeAliasDataEntry);
                    if (U_FAILURE(sts)) {
                        break;
                    }
                }

                if (bcpTypeAliasResByKey != NULL) {
                    UResourceBundle bcpTypeAliasDataEntry;
                    ures_initStackObject(&bcpTypeAliasDataEntry);

                    ures_resetIterator(bcpTypeAliasResByKey);
                    while (ures_hasNext(bcpTypeAliasResByKey) && U_SUCCESS(sts)) {
                        int32_t toLen;
                        ures_getNextResource(bcpTypeAliasResByKey, &bcpTypeAliasDataEntry, &sts);
                        const UChar* to = ures_getString(&bcpTypeAliasDataEntry, &toLen, &sts);
                        if (U_FAILURE(sts)) {
                            break;
                        }
                        // check if this is an alias of bcp type
                        if (uprv_compareInvAscii(NULL, bcpTypeId, -1, to, toLen) == 0) {
                            const char* from = ures_getKey(&bcpTypeAliasDataEntry);
                            uhash_put(typeDataMap, (void*)from, t, &sts);
                        }
                    }
                    ures_close(&bcpTypeAliasDataEntry);
                    if (U_FAILURE(sts)) {
                        break;
                    }
                }
            }
            ures_close(&typeMapEntry);
        }
        ures_close(typeMapResByKey);
        ures_close(typeAliasResByKey);
        ures_close(bcpTypeAliasResByKey);
        if (U_FAILURE(sts)) {
            break;
        }

        LocExtKeyData* keyData = (LocExtKeyData*)uprv_malloc(sizeof(LocExtKeyData));
        if (keyData == NULL) {
            sts = U_MEMORY_ALLOCATION_ERROR;
            break;
        }
        keyData->bcpId = bcpKeyId;
        keyData->legacyId = legacyKeyId;
        keyData->specialTypes = specialTypes;
        keyData->typeMap = typeDataMap;

        gLocExtKeyDataEntries->addElement((void*)keyData, sts);
        if (U_FAILURE(sts)) {
            break;
        }

        uhash_put(gLocExtKeyMap, (void*)legacyKeyId, keyData, &sts);
        if (legacyKeyId != bcpKeyId) {
            // different key value
            uhash_put(gLocExtKeyMap, (void*)bcpKeyId, keyData, &sts);
        }
        if (U_FAILURE(sts)) {
            break;
        }
    }

    ures_close(&keyMapEntry);

close_bundles:
    ures_close(bcpTypeAliasRes);
    ures_close(typeAliasRes);
    ures_close(typeMapRes);
    ures_close(keyMapRes);
    ures_close(keyTypeDataRes);
}

static UBool
init() {
    UErrorCode sts = U_ZERO_ERROR;
    umtx_initOnce(gLocExtKeyMapInitOnce, &initFromResourceBundle, sts);
    if (U_FAILURE(sts)) {
        return FALSE;
    }
    return TRUE;
}

static UBool
isSpecialTypeCodepoints(const char* val) {
    int32_t subtagLen = 0;
    const char* p = val;
    while (*p) {
        if (*p == '-') {
            if (subtagLen < 4 || subtagLen > 6) {
                return FALSE;
            }
            subtagLen = 0;
        } else if (('0' <= *p && *p <= '9') ||
                    ('A' <= *p && *p <= 'F') || ('a' <= *p && *p <= 'f')) {
            subtagLen++;
        } else {
            return FALSE;
        }
        p++;
    }
    return (subtagLen >= 4 && subtagLen <= 6);
}

static UBool
isSpecialTypeReorderCode(const char* val) {
    int32_t subtagLen = 0;
    const char* p = val;
    while (*p) {
        if (*p == '-') {
            if (subtagLen < 3 || subtagLen > 8) {
                return FALSE;
            }
            subtagLen = 0;
        } else if (('A' <= *p && *p <= 'Z') || ('a' <= *p && *p <= 'z')) {
            subtagLen++;
        } else {
            return FALSE;
        }
        p++;
    }
    return (subtagLen >=3 && subtagLen <=8);
}

U_CFUNC const char*
ulocimp_toBcpKey(const char* key) {
    if (!init()) {
        return NULL;
    }

    LocExtKeyData* keyData = (LocExtKeyData*)uhash_get(gLocExtKeyMap, key);
    if (keyData != NULL) {
        return keyData->bcpId;
    }
    return NULL;
}

U_CFUNC const char*
ulocimp_toLegacyKey(const char* key) {
    if (!init()) {
        return NULL;
    }

    LocExtKeyData* keyData = (LocExtKeyData*)uhash_get(gLocExtKeyMap, key);
    if (keyData != NULL) {
        return keyData->legacyId;
    }
    return NULL;
}

U_CFUNC const char*
ulocimp_toBcpType(const char* key, const char* type, UBool* isKnownKey, UBool* isSpecialType) {
    if (isKnownKey != NULL) {
        *isKnownKey = FALSE;
    }
    if (isSpecialType != NULL) {
        *isSpecialType = FALSE;
    }

    if (!init()) {
        return NULL;
    }

    LocExtKeyData* keyData = (LocExtKeyData*)uhash_get(gLocExtKeyMap, key);
    if (keyData != NULL) {
        if (isKnownKey != NULL) {
            *isKnownKey = TRUE;
        }
        LocExtType* t = (LocExtType*)uhash_get(keyData->typeMap, type);
        if (t != NULL) {
            return t->bcpId;
        }
        if (keyData->specialTypes != SPECIALTYPE_NONE) {
            UBool matched = FALSE;
            if (keyData->specialTypes & SPECIALTYPE_CODEPOINTS) {
                matched = isSpecialTypeCodepoints(type);
            }
            if (!matched && keyData->specialTypes & SPECIALTYPE_REORDER_CODE) {
                matched = isSpecialTypeReorderCode(type);
            }
            if (matched) {
                if (isSpecialType != NULL) {
                    *isSpecialType = TRUE;
                }
                return type;
            }
        }
    }
    return NULL;
}


U_CFUNC const char*
ulocimp_toLegacyType(const char* key, const char* type, UBool* isKnownKey, UBool* isSpecialType) {
    if (isKnownKey != NULL) {
        *isKnownKey = FALSE;
    }
    if (isSpecialType != NULL) {
        *isSpecialType = FALSE;
    }

    if (!init()) {
        return NULL;
    }

    LocExtKeyData* keyData = (LocExtKeyData*)uhash_get(gLocExtKeyMap, key);
    if (keyData != NULL) {
        if (isKnownKey != NULL) {
            *isKnownKey = TRUE;
        }
        LocExtType* t = (LocExtType*)uhash_get(keyData->typeMap, type);
        if (t != NULL) {
            return t->legacyId;
        }
        if (keyData->specialTypes != SPECIALTYPE_NONE) {
            UBool matched = FALSE;
            if (keyData->specialTypes & SPECIALTYPE_CODEPOINTS) {
                matched = isSpecialTypeCodepoints(type);
            }
            if (!matched && keyData->specialTypes & SPECIALTYPE_REORDER_CODE) {
                matched = isSpecialTypeReorderCode(type);
            }
            if (matched) {
                if (isSpecialType != NULL) {
                    *isSpecialType = TRUE;
                }
                return type;
            }
        }
    }
    return NULL;
}

