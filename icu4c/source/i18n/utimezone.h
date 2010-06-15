/*
*******************************************************************************
* Copyright (C) 2010, International Business Machines Corporation and         *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/

/**
* \file
* \brief C API: RFC2445 VTIMEZONE support
*
* <p>This is a C wrapper around the C++ VTimeZone class.</p>
*/

#ifndef __UTIMEZONE_H
#define __UTIMEZONE_H

#include "unicode/utypes.h"

#ifndef UCNV_H
struct UTimeZone;
/**
 * Use the utimezone_* API to manipulate.  Create with
 * utimezone_open*, and destroy with utimezone_close.
 * @draft ICU 4.6
 */
typedef struct UTimeZone UTimeZone;
#endif

/**
 * Creates a TimeZone for the given ID.
 * @param ID: the ID for a TimeZone, such as "America/Los_Angeles",
 * or a custom ID such as "GMT-8:00".
 * @param IDLength: the length of the ID
 * @return the specified TimeZone, or the GMT zone if the given ID
 * cannot be understood.  Return result guaranteed to be non-null.  If you
 * require that the specific zone asked for be returned, check the ID of the
 * return result.
 * @draft ICU 4.6
 */
U_DRAFT UTimeZone* U_EXPORT2
utimezone_createTimeZone(const UChar* ID,
                         int32_t* IDLength);


/**
 * Creates a new copy of the default TimeZone for this host. Unless the default time
 * zone has already been set using adoptDefault() or setDefault(), the default is
 * determined by querying the system using methods in TPlatformUtilities. If the
 * system routines fail, or if they specify a TimeZone or TimeZone offset which is not
 * recognized, the TimeZone indicated by the ID kLastResortID is instantiated
 * and made the default.
 *
 * @return   A default TimeZone. Clients are responsible for deleting the time zone
 *           object returned.
 * @draft ICU 4.6
 */
U_DRAFT UTimeZone* U_EXPORT2
utimezone_createDefault(void);


/**
 * Disposes of the storage used by a UTimeZone object.  This function should
 * be called exactly once for objects returned by utimezone_open*.
 * @param zone: the object to dispose of
 * @draft ICU 4.6
 */
U_DRAFT void U_EXPORT2
utimezone_close(UTimeZone* zone);

/**
 * The GMT time zone has a raw offset of zero and does not use daylight
 * savings time. This is a commonly used time zone.
 * @param: zone: the time zone to use
 * @return the GMT time zone.
 * @draft ICU 4.6
 */
U_DRAFT UTimeZone* U_EXPORT2
utimezone_getGMT(UTimeZone* zone);


/**
 * Returns the number of IDs in the equivalency group that
 * includes the given ID.  An equivalency group contains zones
 * that have the same GMT offset and rules.
 *
 * <p>The returned count includes the given ID; it is always >= 1.
 * The given ID must be a system time zone.  If it is not, returns
 * zero.
 *
 * @param: zone: the time zone to use
 * @param id: a system time zone ID
 * @param idLength: length of the id
 * @return the number of zones in the equivalency group containing
 * 'id', or zero if 'id' is not a valid system ID
 * @see #getEquivalentID
 * @draft ICU 4.6
 */
U_DRAFT int32_t U_EXPORT2
utimezone_countEquivalentIDs(UTimeZone* zone,
                             const UChar* ID,
                             int32_t* IDLength);


/**
 * Returns an ID in the equivalency group that
 * includes the given ID.  An equivalency group contains zones
 * that have the same GMT offset and rules.
 *
 * <p>The given index must be in the range 0..n-1, where n is the
 * value returned by <code>countEquivalentIDs(id)</code>.  For
 * some value of 'index', the returned value will be equal to the
 * given id.  If the given id is not a valid system time zone, or
 * if 'index' is out of range, then returns an empty string.
 *
 * @param: zone: the time zone to use
 * @param id: a system time zone ID
 * @param idLength: the length of the id
 * @param index a value from 0 to n-1, where n is the value
 * returned by <code>countEquivalentIDs(id)</code>
 * @param: equivId: the returned id of the index-th zone in the 
 * equivalency group containing 'id', or an empty string if 'id' is not a valid
 * system ID or 'index' is out of range
 * @param: equivId: the length of the retuned equiv id.  If equivId is null,
 * this will contain the number of bytes to contain the equivId.
 *
 * @see #countEquivalentIDs
 * @draft ICU 4.6
 */
U_DRAFT void U_EXPORT2
utimezone_getEquivalentID(UTimeZone* zone,
                          const UChar* id,
                          int32_t* idLength,
                          int32_t index,
                          UChar* equivId,
                          int32_t* equivIdLength);


/**
 * Sets the default time zone (i.e., what's returned by createDefault()) to be the
 * specified time zone.  If NULL is specified for the time zone, the default time
 * zone is set to the default host time zone.  This call adopts the TimeZone object
 * passed in; the clent is no longer responsible for deleting it.
 *
 * @param zone: the time zone to use
 * @param dftZone:  A pointer to the new TimeZone object to use as the default.
 * @draft ICU 4.6
 */
U_DRAFT void U_EXPORT2
utimezone_adoptDefault(UTimeZone* zone,
                       UTimeZone* dftZone);



/**
 * Same as adoptDefault(), except that the TimeZone object passed in is NOT adopted;
 * the caller remains responsible for deleting it.
 *
 * @param zone: the time zone to use
 * @param dftZone: a pointer to the TimeZone object to use as the default.
 * @system
 * @draft ICU 4.6
 */
U_DRAFT void U_EXPORT2
utimezone_setDefault(UTimeZone* zone,
                     const UTimeZone* dftZone);


/**
 * Returns the timezone data version currently used by ICU.
 * @param zone: the time zone to use
 * @param status: Output param to filled in with a success or an error. 
 * @return the data version
 * @draft ICU 4.6
 */
U_DRAFT const char* U_EXPORT2
utimezone_getTZDataVersion(UTimeZone* zone, 
                           UErrorCode* status);


/**
 * Returns the canonical system timezone ID or the normalized
 * custom time zone ID for the given time zone ID.
 * @param zone: the time zone to use
 * @param id            The input time zone ID to be canonicalized.
 * @param idLength:     The length of the id
 * @param canonicalID   Receives the canonical system time zone ID
 *                      or the custom time zone ID in normalized format.
 * @param canonicalIDLen: Length of the canonicalID.  If the canonicalID is
 *                      null this will contain the number of bytes needed
 *                      to hold the canonicalID
 * @param status        Recevies the status.  When the given time zone ID
 *                      is neither a known system time zone ID nor a
 *                      valid custom time zone ID, U_ILLEGAL_ARGUMENT_ERROR
 *                      is set.
 * @draft ICU 4.6
 */
U_DRAFT void U_EXPORT2
utimezone_getCanonicalID(UTimeZone* zone,
                         const UChar* id,
                         int32_t* idLength,
                         UChar* canonicalID, 
                         int32_t* canonicalIDLen,
                         UErrorCode* status);


/**
 * Returns the canonical system time zone ID or the normalized
 * custom time zone ID for the given time zone ID.
 * @param zone: the time zone to use
 * @param id            The input time zone ID to be canonicalized.
 * @param idLength:     The length of the id
 * @param canonicalID   Receives the canonical system time zone ID
 *                      or the custom time zone ID in normalized format.
 * @param canonicalIDLen: Length of the canonicalID.  If the canonicalID is
 *                      null this will contain the number of bytes needed
 *                      to hold the canonicalID
 * @param isSystemID    Receives if the given ID is a known system
 *                      time zone ID.
 * @param status        Recevies the status.  When the given time zone ID
 *                      is neither a known system time zone ID nor a
 *                      valid custom time zone ID, U_ILLEGAL_ARGUMENT_ERROR
 *                      is set.
 * @draft ICU 4.6
 */
U_DRAFT void U_EXPORT2
utimezone_getCanonicalIDSys(UTimeZone* zone,
                         const UChar* id,
                         int32_t* idLength,
                         UChar* canonicalID, 
                         int32_t* canonicalIDLen,
                         UBool* isSystemID,
                         UErrorCode* status);


/**
 * Returns true if the two TimeZones are equal.  (The TimeZone version only compares
 * IDs, but subclasses are expected to also compare the fields they add.)
 *
 * @param zone1: the time zone to be checked for equality
 * @param zone2: the time zone to be checked for equality
 * @return true if the test condition is met
 * @draft ICU 4.6
 */
U_DRAFT UBool U_EXPORT2
utimezone_equals(const UTimeZone* zone1, 
                 const UTimeZone* zone2);


/**
 * Returns the time zone raw and GMT offset for the given moment
 * in time.  Upon return, local-millis = GMT-millis + rawOffset +
 * dstOffset.  All computations are performed in the proleptic
 * Gregorian calendar.  The default implementation in the TimeZone
 * class delegates to the 8-argument getOffset().
 *
 * @param: zone: the time zone to use
 * @param date moment in time for which to return offsets, in
 * units of milliseconds from January 1, 1970 0:00 GMT, either GMT
 * time or local wall time, depending on `local'.
 * @param local if true, `date' is local wall time; otherwise it
 * is in GMT time.
 * @param rawOffset output parameter to receive the raw offset, that
 * is, the offset not including DST adjustments
 * @param dstOffset output parameter to receive the DST offset,
 * that is, the offset to be added to `rawOffset' to obtain the
 * total offset between local and GMT time. If DST is not in
 * effect, this value is zero; otherwise it is a positive value,
 * typically one hour.
 * @param ec input-output error code
 *
 * @draft ICU 4.6
 */
U_DRAFT void U_EXPORT2
utimezone_getOffset(UTimeZone* zone,
                    UDate date, 
                    UBool local, 
                    int32_t* rawOffset,
                    int32_t* dstOffset, 
                    UErrorCode* ec);

/**
 * Fills in "ID" with the TimeZone's ID.
 *
 * @param zone: the time zone to use
 * @param ID  Receives this TimeZone's ID.
 * @param IDLength: the length of ID.  If ID is null
 * this will return the number of bytes needed for ID
 * @draft ICU 4.6
 */
U_DRAFT void U_EXPORT2
utimezone_getID(UTimeZone* zone,
                UChar* ID,
                int32_t* IDLength);


/**
 * Sets the TimeZone's ID to the specified value.  This doesn't affect any other
 * fields.
 *
 * @param zone: the time zone to use
 * @param ID  The new time zone ID.
 * @param IDLength: the length of ID.
 * @draft ICU 4.6
 */
U_DRAFT void U_EXPORT2
utimezone_setID(UTimeZone* zone,
                UChar* ID,
                int32_t* IDLength);

/**
 * Returns true if this zone has the same rule and offset as another zone.
 * That is, if this zone differs only in ID, if at all.
 * @param zone: the time zone to use
 * @param other the <code>TimeZone</code> object to be compared with
 * @return true if the given zone is the same as this one,
 * with the possible exception of the ID
 * @draft ICU 4.6
 */
U_DRAFT UBool U_EXPORT2
utimezone_hasSameRules(UTimeZone* zone,
                       UTimeZone* other);
  
#endif // __UTIMEZONE_H

