/*
**********************************************************************
* Copyright (c) 2002-2003, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Alan Liu
* Created: October 30 2002
* Since: ICU 2.4
**********************************************************************
*/
#include "propname.h"
#include "unicode/uchar.h"
#include "unicode/udata.h"
#include "umutex.h"

U_NAMESPACE_BEGIN

//----------------------------------------------------------------------
// PropertyAliases implementation

const char*
PropertyAliases::chooseNameInGroup(Offset offset,
                                   UPropertyNameChoice choice) const {
    int32_t c = choice;
    if (!offset || c < 0) {
        return NULL;
    }
    const Offset* p = (const Offset*) getPointer(offset);
    while (c-- > 0) {
        if (*p++ < 0) return NULL;
    }
    Offset a = *p;
    if (a < 0) a = -a;
    return (const char*) getPointerNull(a);
}

const ValueMap*
PropertyAliases::getValueMap(EnumValue prop) const {
    NonContiguousEnumToOffset* e2o = (NonContiguousEnumToOffset*) getPointer(enumToValue_offset);
    Offset a = e2o->getOffset(prop);
    return (const ValueMap*) (a ? getPointerNull(a) : NULL);
}

inline const char*
PropertyAliases::getPropertyName(EnumValue prop,
                                 UPropertyNameChoice choice) const {
    NonContiguousEnumToOffset* e2n = (NonContiguousEnumToOffset*) getPointer(enumToName_offset);
    return chooseNameInGroup(e2n->getOffset(prop), choice);
}

inline EnumValue
PropertyAliases::getPropertyEnum(const char* alias) const {
    NameToEnum* n2e = (NameToEnum*) getPointer(nameToEnum_offset);
    return n2e->getEnum(alias, *this);
}

inline const char*
PropertyAliases::getPropertyValueName(EnumValue prop,
                                      EnumValue value,
                                      UPropertyNameChoice choice) const {
    const ValueMap* vm = getValueMap(prop);
    if (!vm) return NULL;
    Offset a;
    if (vm->enumToName_offset) {
        a = ((EnumToOffset*) getPointer(vm->enumToName_offset))->
            getOffset(value);
    } else {
        a = ((NonContiguousEnumToOffset*) getPointer(vm->ncEnumToName_offset))->
            getOffset(value);
    }
    return chooseNameInGroup(a, choice);
}

inline EnumValue
PropertyAliases::getPropertyValueEnum(EnumValue prop,
                                      const char* alias) const {
    const ValueMap* vm = getValueMap(prop);
    if (!vm) return UCHAR_INVALID_CODE;
    NameToEnum* n2e = (NameToEnum*) getPointer(vm->nameToEnum_offset);
    return n2e->getEnum(alias, *this);
}

U_NAMESPACE_END

//----------------------------------------------------------------------
// UDataMemory structures

static const PropertyAliases* PNAME = NULL;
static UDataMemory* UDATA = NULL;

//----------------------------------------------------------------------
// UDataMemory loading/unloading

/**
 * udata callback to verify the zone data.
 */
U_CDECL_BEGIN
static UBool U_CALLCONV
isAcceptable(void* /*context*/,
             const char* /*type*/, const char* /*name*/,
             const UDataInfo* info) {
    return
        info->size >= sizeof(UDataInfo) &&
        info->isBigEndian == U_IS_BIG_ENDIAN &&
        info->charsetFamily == U_CHARSET_FAMILY &&
        info->dataFormat[0] == PNAME_SIG_0 &&
        info->dataFormat[1] == PNAME_SIG_1 &&
        info->dataFormat[2] == PNAME_SIG_2 &&
        info->dataFormat[3] == PNAME_SIG_3 &&
        info->formatVersion[0] == PNAME_FORMAT_VERSION;
}

UBool
pname_cleanup() {
    if (UDATA) {
        udata_close(UDATA);
        UDATA = NULL;
    }
    PNAME = NULL;
    return TRUE;
}
U_CDECL_END

/**
 * Load the property names data.  Caller should check that data is
 * not loaded BEFORE calling this function.  Returns TRUE if the load
 * succeeds.
 */
static UBool _load() {
    UErrorCode ec = U_ZERO_ERROR;
    UDataMemory* data =
        udata_openChoice(0, PNAME_DATA_TYPE, PNAME_DATA_NAME,
                         isAcceptable, 0, &ec);
    if (U_SUCCESS(ec)) {
        umtx_lock(NULL);
        if (UDATA == NULL) {
            UDATA = data;
            PNAME = (const PropertyAliases*) udata_getMemory(UDATA);
            data = NULL;
        }
        umtx_unlock(NULL);
    }
    if (data) {
        udata_close(data);
    }
    return PNAME!=NULL;
}

/**
 * Inline function that expands to code that does a lazy load of the
 * property names data.  If the data is already loaded, avoids an
 * unnecessary function call.  If the data is not loaded, call _load()
 * to load it, and return TRUE if the load succeeds.
 */
static inline UBool load() {
    umtx_lock(NULL);
    UBool f = (PNAME!=NULL);
    umtx_unlock(NULL);
    return f || _load();
}

//----------------------------------------------------------------------
// Public API implementation

// The C API is just a thin wrapper.  Each function obtains a pointer
// to the singleton PropertyAliases, and calls the appropriate method
// on it.  If it cannot obtain a pointer, because valid data is not
// available, then it returns NULL or UCHAR_INVALID_CODE.

U_CAPI const char* U_EXPORT2
u_getPropertyName(UProperty property,
                  UPropertyNameChoice nameChoice) {
    return load() ? PNAME->getPropertyName(property, nameChoice)
                  : NULL;
}

U_CAPI UProperty U_EXPORT2
u_getPropertyEnum(const char* alias) {
    UProperty p = load() ? (UProperty) PNAME->getPropertyEnum(alias)
                         : UCHAR_INVALID_CODE;
    return p;
}

U_CAPI const char* U_EXPORT2
u_getPropertyValueName(UProperty property,
                       int32_t value,
                       UPropertyNameChoice nameChoice) {
    return load() ? PNAME->getPropertyValueName(property, value, nameChoice)
                  : NULL;
}

U_CAPI int32_t U_EXPORT2
u_getPropertyValueEnum(UProperty property,
                       const char* alias) {
    return load() ? PNAME->getPropertyValueEnum(property, alias)
                  : UCHAR_INVALID_CODE;
}

//eof
