/*
*******************************************************************************
* Copyright (C) 2010, International Business Machines Corporation and         *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/

/**
 * \file 
 * \brief C API: Time Zone wrapper
 */

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/uobject.h"
#include "unicode/timezone.h"
#include "utimezone.h"
#include "cmemory.h"
#include "unicode/ustring.h"
#include "unicode/parsepos.h"

U_NAMESPACE_USE

U_DRAFT UTimeZone* U_EXPORT2
utimezone_createTimeZone(const UChar* ID, int32_t* IDLength) {
    UnicodeString uID(*IDLength==-1, ID, *IDLength);
    return (UTimeZone*) (TimeZone::createTimeZone(uID));
}

U_DRAFT UTimeZone* U_EXPORT2
utimezone_createDefault(void) {
    return (UTimeZone*) (TimeZone::createDefault());
}

U_DRAFT void U_EXPORT2
utimezone_close(UTimeZone* zone) {
    delete (TimeZone*)zone;
}

U_DRAFT UTimeZone* U_EXPORT2
utimezone_getGMT(UTimeZone* zone) {
    return (UTimeZone*) (((TimeZone*)zone)->TimeZone::getGMT());
}

U_DRAFT int32_t U_EXPORT2
utimezone_countEquivalentIDs(UTimeZone* zone, const UChar* ID, int32_t* IDLength) {
    UnicodeString uID(*IDLength==-1, ID, *IDLength);
    return ((TimeZone*)zone)->TimeZone::countEquivalentIDs(uID);
}

U_DRAFT void U_EXPORT2
utimezone_getEquivalentID(UTimeZone* zone, const UChar* id, int32_t* idLength, int32_t index,
                          UChar* equivId, int32_t* equivIdLength) {

    UnicodeString uID(*idLength==-1, id, *idLength);
    UnicodeString eID = ((TimeZone*)zone)->TimeZone::getEquivalentID(id, index);

    if (*equivIdLength > 0)
    {
        memcpy(equivId,eID.getBuffer(),*equivIdLength);
    }
    else
    {
        *equivIdLength = eID.length();
    }

    return;
}

U_DRAFT void U_EXPORT2
utimezone_adoptDefault(UTimeZone* zone, UTimeZone* dftZone) {
    return ((TimeZone*)zone)->TimeZone::adoptDefault((TimeZone*)dftZone);
}

U_DRAFT void U_EXPORT2
utimezone_setDefault(UTimeZone* zone, const UTimeZone* dftZone) {
    return ((TimeZone*)zone)->TimeZone::setDefault(*(TimeZone*)dftZone);
}

U_DRAFT const char* U_EXPORT2
utimezone_getTZDataVersion(UTimeZone* zone, UErrorCode* status) {
    return ((TimeZone*)zone)->TimeZone::getTZDataVersion(*status);
}

U_DRAFT void U_EXPORT2
utimezone_getCanonicalID(UTimeZone* zone, const UChar* id, int32_t* idLength,
                         UChar* canonicalID, int32_t* canonicalIDLen, UErrorCode* status) {

    UnicodeString uID(*idLength==-1, id, *idLength);
    UnicodeString canID = ((TimeZone*)zone)->TimeZone::getCanonicalID((const UnicodeString)uID, canID, *status);

    if (*canonicalIDLen > 0)
    {
        memcpy(canonicalID,canID.getBuffer(),*canonicalIDLen);
    }
    else
    {
        *canonicalIDLen = canID.length();
    }

    return;
}

U_DRAFT void U_EXPORT2
utimezone_getCanonicalIDSys(UTimeZone* zone, const UChar* id, int32_t* idLength, UChar* canonicalID, 
                         int32_t* canonicalIDLen, UBool* isSystemID, UErrorCode* status) {

    UnicodeString uID(*idLength==-1, id, *idLength);
    UnicodeString canID = ((TimeZone*)zone)->TimeZone::getCanonicalID((const UnicodeString)uID, canID, *isSystemID, *status);

    if (*canonicalIDLen > 0)
    {
        memcpy(canonicalID,canID.getBuffer(),*canonicalIDLen);
    }
    else
    {
        *canonicalIDLen = canID.length();
    }

    return;
}

U_DRAFT UBool U_EXPORT2
utimezone_equals(const UTimeZone* zone1, const UTimeZone* zone2) {
    return *(const TimeZone*)zone1 == *(const TimeZone*)zone2;
}

U_DRAFT void U_EXPORT2
utimezone_getOffset(UTimeZone* zone, UDate date, UBool local, int32_t* rawOffset, int32_t* dstOffset, UErrorCode* ec) {
    return ((TimeZone*)zone)->TimeZone::getOffset(date, local, *rawOffset, *dstOffset, *ec);
}

U_DRAFT void U_EXPORT2
utimezone_getID(UTimeZone* zone, UChar* ID, int32_t* IDLength) {

    UnicodeString s;
    s = ((TimeZone*)zone)->TimeZone::getID(s);

    if (*IDLength > 0)
    {
        memcpy(ID,s.getBuffer(),*IDLength);
    }
    else
    {
        *IDLength = s.length();
    }

    return;
}

U_DRAFT void U_EXPORT2
utimezone_setID(UTimeZone* zone, UChar* ID, int32_t* IDLength) {
    UnicodeString s(*IDLength==-1, ID, *IDLength);
    return ((TimeZone*)zone)->TimeZone::setID(s);
}


U_DRAFT UBool U_EXPORT2
utimezone_hasSameRules(UTimeZone* zone, UTimeZone* other) {
    return ((TimeZone*)zone)->TimeZone::hasSameRules(*(const TimeZone*)other);
}

#endif
