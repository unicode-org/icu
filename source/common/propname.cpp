/*
**********************************************************************
* Copyright (c) 2002, International Business Machines
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

static UBool load() {
    if (!PNAME) {
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
    }
    return PNAME!=NULL;
}

//----------------------------------------------------------------------
// Public API implementation

// The C API is just a thin wrapper.  Each function obtains a pointer
// to the singleton PropertyAliases, and calls the appropriate method
// on it.  If it cannot obtain a pointer, because valid data is not
// available, then it returns NULL or UCHAR_INVALID_CODE.

// NOTE (ICU 2.4) For the 2.4 release it was decided late in the cycle
// to add a new enum to UProperty, UCHAR_GENERAL_CATEGORY_MASK.  This
// enum would specify UCharCategory mask values.  Because of time
// constraints, the underlying binary data and genprop scripts were
// not updated.  So the PNAME->... API takes UCHAR_GENERAL_CATEGORY
// and associates it with a MASK value.  We munge things to make this
// associate with a UCharCategory value, and we make
// UCHAR_GENERAL_CATEGORY_MASK correspond to the mask value.

// We add a synthetic (not in PropertyAliases.txt) pair of property
// names corresponding to UCHAR_GENERAL_CATEGORY_MASK:
// gcm       ; General_Category_Mask

// TODO: Remove the munge code, marked "//TODO:munge" below, after the
// script/binary data are updated (probably in ICU 2.6).

static const char SHORT_GCM_NAME[] = "gcm";
static const char LONG_GCM_NAME[]  = "General_Category_Mask";

U_CAPI const char* U_EXPORT2
u_getPropertyName(UProperty property,
                  UPropertyNameChoice nameChoice) {
    //TODO:munge
    if (property == UCHAR_GENERAL_CATEGORY_MASK) {
        switch (nameChoice) {
        case U_SHORT_PROPERTY_NAME:
            return SHORT_GCM_NAME;
        case U_LONG_PROPERTY_NAME:
            return LONG_GCM_NAME;
        default:
            return NULL;
        }
    }
    return load() ? PNAME->getPropertyName(property, nameChoice)
                  : NULL;
}

U_CAPI UProperty U_EXPORT2
u_getPropertyEnum(const char* alias) {
    UProperty p = load() ? (UProperty) PNAME->getPropertyEnum(alias)
                         : UCHAR_INVALID_CODE;
    //TODO:munge
    if (p == UCHAR_INVALID_CODE) {
        if (0 == uprv_comparePropertyNames(alias, SHORT_GCM_NAME) ||
            0 == uprv_comparePropertyNames(alias, LONG_GCM_NAME)) {
            p = UCHAR_GENERAL_CATEGORY_MASK;
        }
    }
    return p;
}

U_CAPI const char* U_EXPORT2
u_getPropertyValueName(UProperty property,
                       int32_t value,
                       UPropertyNameChoice nameChoice) {
    //TODO:munge
    switch (property) {
    case UCHAR_GENERAL_CATEGORY:
        value = (value < 32) ? U_MASK(value) : 0;
        break;
    case UCHAR_GENERAL_CATEGORY_MASK:
        property = UCHAR_GENERAL_CATEGORY;
        break;
    }
    return load() ? PNAME->getPropertyValueName(property, value, nameChoice)
                  : NULL;
}

U_CAPI int32_t U_EXPORT2
u_getPropertyValueEnum(UProperty property,
                       const char* alias) {
    //TODO:munge
    UProperty p = (property == UCHAR_GENERAL_CATEGORY_MASK) ?
                  UCHAR_GENERAL_CATEGORY : property;
    int32_t v = load() ? PNAME->getPropertyValueEnum(p, alias)
                       : UCHAR_INVALID_CODE;
    //TODO:munge
    if (property == UCHAR_GENERAL_CATEGORY) {
        int32_t gc = 0;
        for (;;) {
            if (v == 1) {
                return gc;
            }
            if ((v & 1) != 0) {
                // More than one bit is set; we can't map this mask to
                // a UCharCategory.
                return UCHAR_INVALID_CODE;
            }
            v >>= 1;
            gc += 1;
        }
    }
    return v;
}

//eof
