/*
**********************************************************************
* Copyright (c) 2003-2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Alan Liu
* Created: July 21 2003
* Since: ICU 2.8
**********************************************************************
*/

#include "olsontz.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/ures.h"
#include "unicode/simpletz.h"
#include "unicode/gregocal.h"
#include "gregoimp.h"
#include "cmemory.h"
#include "uassert.h"
#include <float.h> // DBL_MAX

#ifdef U_DEBUG_TZ
# include <stdio.h>
# include "uresimp.h" // for debugging

static void debug_tz_loc(const char *f, int32_t l)
{
  fprintf(stderr, "%s:%d: ", f, l);
}

static void debug_tz_msg(const char *pat, ...)
{
  va_list ap;
  va_start(ap, pat);
  vfprintf(stderr, pat, ap);
  fflush(stderr);
}
// must use double parens, i.e.:  U_DEBUG_TZ_MSG(("four is: %d",4));
#define U_DEBUG_TZ_MSG(x) {debug_tz_loc(__FILE__,__LINE__);debug_tz_msg x;}
#else
#define U_DEBUG_TZ_MSG(x)
#endif

U_NAMESPACE_BEGIN

#define SECONDS_PER_DAY (24*60*60)

static const int32_t ZEROS[] = {0,0};

UOBJECT_DEFINE_RTTI_IMPLEMENTATION(OlsonTimeZone)

/**
 * Default constructor.  Creates a time zone with an empty ID and
 * a fixed GMT offset of zero.
 */
OlsonTimeZone::OlsonTimeZone() : finalYear(INT32_MAX), finalMillis(DBL_MAX), finalZone(0) {
    constructEmpty();
}

/**
 * Construct a GMT+0 zone with no transitions.  This is done when a
 * constructor fails so the resultant object is well-behaved.
 */
void OlsonTimeZone::constructEmpty() {
    transitionCount = 0;
    typeCount = 1;
    transitionTimes = typeOffsets = ZEROS;
    typeData = (const uint8_t*) ZEROS;
}

/**
 * Construct from a resource bundle
 * @param top the top-level zoneinfo resource bundle.  This is used
 * to lookup the rule that `res' may refer to, if there is one.
 * @param res the resource bundle of the zone to be constructed
 * @param ec input-output error code
 */
OlsonTimeZone::OlsonTimeZone(const UResourceBundle* top,
                             const UResourceBundle* res,
                             UErrorCode& ec) :
  finalYear(INT32_MAX), finalMillis(DBL_MAX), finalZone(0)
{
    U_DEBUG_TZ_MSG(("OlsonTimeZone(%s)\n", ures_getKey((UResourceBundle*)res)));
    if ((top == NULL || res == NULL) && U_SUCCESS(ec)) {
        ec = U_ILLEGAL_ARGUMENT_ERROR;
    }
    if (U_SUCCESS(ec)) {
        // TODO -- clean up -- Doesn't work if res points to an alias
        //        // TODO remove nonconst casts below when ures_* API is fixed
        //        setID(ures_getKey((UResourceBundle*) res)); // cast away const

        // Size 1 is an alias TO another zone (int)
        // HOWEVER, the caller should dereference this and never pass it in to us
        // Size 3 is a purely historical zone (no final rules)
        // Size 4 is like size 3, but with an alias list at the end
        // Size 5 is a hybrid zone, with historical and final elements
        // Size 6 is like size 5, but with an alias list at the end
        int32_t size = ures_getSize((UResourceBundle*) res); // cast away const
        if (size < 3 || size > 6) {
            ec = U_INVALID_FORMAT_ERROR;
        }

        // Transitions list may be empty
        int32_t i;
        UResourceBundle* r = ures_getByIndex(res, 0, NULL, &ec);
        transitionTimes = ures_getIntVector(r, &i, &ec);
        ures_close(r);
        if ((i<0 || i>0x7FFF) && U_SUCCESS(ec)) {
            ec = U_INVALID_FORMAT_ERROR;
        }
        transitionCount = (int16_t) i;
        
        // Type offsets list must be of even size, with size >= 2
        r = ures_getByIndex(res, 1, NULL, &ec);
        typeOffsets = ures_getIntVector(r, &i, &ec);
        ures_close(r);
        if ((i<2 || i>0x7FFE || ((i&1)!=0)) && U_SUCCESS(ec)) {
            ec = U_INVALID_FORMAT_ERROR;
        }
        typeCount = (int16_t) i >> 1;

        // Type data must be of the same size as the transitions list        
        r = ures_getByIndex(res, 2, NULL, &ec);
        int32_t len;
        typeData = ures_getBinary(r, &len, &ec);
        ures_close(r);
        if (len != transitionCount && U_SUCCESS(ec)) {
            ec = U_INVALID_FORMAT_ERROR;
        }

#if defined (U_DEBUG_TZ)
        U_DEBUG_TZ_MSG(("OlsonTimeZone(%s) - size = %d, typecount %d transitioncount %d - err %s\n", ures_getKey((UResourceBundle*)res), size, typeCount,  transitionCount, u_errorName(ec)));
        if(U_SUCCESS(ec)) {
          int32_t jj;
          for(jj=0;jj<transitionCount;jj++) {
            U_DEBUG_TZ_MSG(("   Transition %d:  time %d, typedata%d\n", jj, transitionTimes[jj], typeData[jj]));
          }
          for(jj=0;jj<transitionCount;jj++) {
            U_DEBUG_TZ_MSG(("   Type %d:  offset%d\n", jj, typeOffsets[jj]));
          }
        }
#endif

        // Process final rule and data, if any
        if (size >= 5) {
            int32_t ruleidLen = 0;
            const UChar* idUStr = ures_getStringByIndex(res, 3, &ruleidLen, &ec);
            UnicodeString ruleid(TRUE, idUStr, ruleidLen);
            r = ures_getByIndex(res, 4, NULL, &ec);
            const int32_t* data = ures_getIntVector(r, &len, &ec);
#if defined U_DEBUG_TZ
            const char *rKey = ures_getKey(r);
            const char *zKey = ures_getKey((UResourceBundle*)res);
#endif
            ures_close(r);
            if (U_SUCCESS(ec)) {
                if (data != 0 && len == 2) {
                    int32_t rawOffset = data[0] * U_MILLIS_PER_SECOND;
                    // Subtract one from the actual final year; we
                    // actually store final year - 1, and compare
                    // using > rather than >=.  This allows us to use
                    // INT32_MAX as an exclusive upper limit for all
                    // years, including INT32_MAX.
                    U_ASSERT(data[1] > INT32_MIN);
                    finalYear = data[1] - 1;
                    // Also compute the millis for Jan 1, 0:00 GMT of the
                    // finalYear.  This reduces runtime computations.
                    finalMillis = Grego::fieldsToDay(data[1], 0, 1) * U_MILLIS_PER_DAY;
                    U_DEBUG_TZ_MSG(("zone%s|%s: {%d,%d}, finalYear%d, finalMillis%.1lf\n",
                                    zKey,rKey, data[0], data[1], finalYear, finalMillis));
                    r = TimeZone::loadRule(top, ruleid, NULL, ec);
                    if (U_SUCCESS(ec)) {
                        // 3, 1, -1, 7200, 0, 9, -31, -1, 7200, 0, 3600
                        data = ures_getIntVector(r, &len, &ec);
                        if (U_SUCCESS(ec) && len == 11) {
                            U_DEBUG_TZ_MSG(("zone%s, rule%s: {%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d}", zKey, ures_getKey(r), 
                                          data[0], data[1], data[2], data[3], data[4], data[5], data[6], data[7], data[8], data[9], data[10]));
                            finalZone = new SimpleTimeZone(rawOffset, "",
                                (int8_t)data[0], (int8_t)data[1], (int8_t)data[2],
                                data[3] * U_MILLIS_PER_SECOND,
                                (SimpleTimeZone::TimeMode) data[4],
                                (int8_t)data[5], (int8_t)data[6], (int8_t)data[7],
                                data[8] * U_MILLIS_PER_SECOND,
                                (SimpleTimeZone::TimeMode) data[9],
                                data[10] * U_MILLIS_PER_SECOND, ec);
                        } else {
                            ec = U_INVALID_FORMAT_ERROR;
                        }
                    }
                    ures_close(r);
                } else {
                    ec = U_INVALID_FORMAT_ERROR;
                }
            }
        }
    }

    if (U_FAILURE(ec)) {
        constructEmpty();
    }
}

/**
 * Copy constructor
 */
OlsonTimeZone::OlsonTimeZone(const OlsonTimeZone& other) :
    TimeZone(other), finalZone(0) {
    *this = other;
}

/**
 * Assignment operator
 */
OlsonTimeZone& OlsonTimeZone::operator=(const OlsonTimeZone& other) {
    transitionCount = other.transitionCount;
    typeCount = other.typeCount;
    transitionTimes = other.transitionTimes;
    typeOffsets = other.typeOffsets;
    typeData = other.typeData;
    finalYear = other.finalYear;
    finalMillis = other.finalMillis;
    delete finalZone;
    finalZone = (other.finalZone != 0) ?
        (SimpleTimeZone*) other.finalZone->clone() : 0;
    return *this;
}

/**
 * Destructor
 */
OlsonTimeZone::~OlsonTimeZone() {
    delete finalZone;
}

/**
 * Returns true if the two TimeZone objects are equal.
 */
UBool OlsonTimeZone::operator==(const TimeZone& other) const {
    const OlsonTimeZone* z = (const OlsonTimeZone*) &other;

    return TimeZone::operator==(other) &&
        // [sic] pointer comparison: typeData points into
        // memory-mapped or DLL space, so if two zones have the same
        // pointer, they are equal.
        (typeData == z->typeData ||
         // If the pointers are not equal, the zones may still
         // be equal if their rules and transitions are equal
         (finalYear == z->finalYear &&
          // Don't compare finalMillis; if finalYear is ==, so is finalMillis
          ((finalZone == 0 && z->finalZone == 0) ||
           (finalZone != 0 && z->finalZone != 0 &&
            *finalZone == *z->finalZone)) &&
          transitionCount == z->transitionCount &&
          typeCount == z->typeCount &&
          uprv_memcmp(transitionTimes, z->transitionTimes,
                      sizeof(transitionTimes[0]) * transitionCount) == 0 &&
          uprv_memcmp(typeOffsets, z->typeOffsets,
                      (sizeof(typeOffsets[0]) * typeCount) << 1) == 0 &&
          uprv_memcmp(typeData, z->typeData,
                      (sizeof(typeData[0]) * typeCount)) == 0
          ));
}

/**
 * TimeZone API.
 */
TimeZone* OlsonTimeZone::clone() const {
    return new OlsonTimeZone(*this);
}

/**
 * TimeZone API.
 */
int32_t OlsonTimeZone::getOffset(uint8_t era, int32_t year, int32_t month,
                                 int32_t dom, uint8_t dow,
                                 int32_t millis, UErrorCode& ec) const {
    if (month < UCAL_JANUARY || month > UCAL_DECEMBER) {
        if (U_SUCCESS(ec)) {
            ec = U_ILLEGAL_ARGUMENT_ERROR;
        }
        return 0;
    } else {
        return getOffset(era, year, month, dom, dow, millis,
                         Grego::monthLength(year, month),
                         ec);
    }
}

/**
 * TimeZone API.
 */
int32_t OlsonTimeZone::getOffset(uint8_t era, int32_t year, int32_t month,
                                 int32_t dom, uint8_t dow,
                                 int32_t millis, int32_t monthLength,
                                 UErrorCode& ec) const {
    if (U_FAILURE(ec)) {
        return 0;
    }

    if ((era != GregorianCalendar::AD && era != GregorianCalendar::BC)
        || month < UCAL_JANUARY
        || month > UCAL_DECEMBER
        || dom < 1
        || dom > monthLength
        || dow < UCAL_SUNDAY
        || dow > UCAL_SATURDAY
        || millis < 0
        || millis >= U_MILLIS_PER_DAY
        || monthLength < 28
        || monthLength > 31) {
        ec = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    if (era == GregorianCalendar::BC) {
        year = -year;
    }

    if (year > finalYear) { // [sic] >, not >=; see above
        U_ASSERT(finalZone != 0);
        return finalZone->getOffset(era, year, month, dom, dow,
                                    millis, monthLength, ec);
    }

    // Compute local epoch seconds from input fields
    double time = Grego::fieldsToDay(year, month, dom) * SECONDS_PER_DAY +
        uprv_floor(millis / (double) U_MILLIS_PER_SECOND);

    return zoneOffset(findTransition(time, TRUE)) * U_MILLIS_PER_SECOND;
}

/**
 * TimeZone API.
 */
void OlsonTimeZone::getOffset(UDate date, UBool local, int32_t& rawoff,
                              int32_t& dstoff, UErrorCode& ec) const {
    if (U_FAILURE(ec)) {
        return;
    }

    // The check against finalMillis will suffice most of the time, except
    // for the case in which finalMillis == DBL_MAX, date == DBL_MAX,
    // and finalZone == 0.  For this case we add "&& finalZone != 0".
    if (date >= finalMillis && finalZone != 0) {
        int32_t year, month, dom, dow;
        double millis;
        double days = Math::floorDivide(date, (double)U_MILLIS_PER_DAY, millis);
        
        Grego::dayToFields(days, year, month, dom, dow);

        rawoff = finalZone->getRawOffset();

        if (!local) {
            // Adjust from GMT to local
            date += rawoff;
            double days2 = Math::floorDivide(date, (double)U_MILLIS_PER_DAY, millis);
            if (days2 != days) {
                Grego::dayToFields(days2, year, month, dom, dow);
            }
        }

        dstoff = finalZone->getOffset(
            GregorianCalendar::AD, year, month,
            dom, (uint8_t) dow, (int32_t) millis, ec) - rawoff;
        return;
    }

    double secs = uprv_floor(date / U_MILLIS_PER_SECOND);
    int16_t i = findTransition(secs, local);
    rawoff = rawOffset(i) * U_MILLIS_PER_SECOND;
    dstoff = dstOffset(i) * U_MILLIS_PER_SECOND;
}

/**
 * TimeZone API.
 */
void OlsonTimeZone::setRawOffset(int32_t /*offsetMillis*/) {
    // We don't support this operation, since OlsonTimeZones are
    // immutable (except for the ID, which is in the base class).

    // Nothing to do!
}

/**
 * TimeZone API.
 */
int32_t OlsonTimeZone::getRawOffset() const {
    UErrorCode ec = U_ZERO_ERROR;
    int32_t raw, dst;
    getOffset((double) uprv_getUTCtime() * U_MILLIS_PER_SECOND,
              FALSE, raw, dst, ec);
    return raw;
}

/**
 * Find the smallest i (in 0..transitionCount-1) such that time >=
 * transition(i), where transition(i) is either the GMT or the local
 * transition time, as specified by `local'.
 * @param time epoch seconds, either GMT or local wall
 * @param local if TRUE, `time' is in local wall units, otherwise it
 * is GMT
 * @return an index i, where 0 <= i < transitionCount, and
 * transition(i) <= time < transition(i+1), or i == 0 if
 * transitionCount == 0 or time < transition(0).
 */
int16_t OlsonTimeZone::findTransition(double time, UBool local) const {
    int16_t i = 0;
    
    if (transitionCount != 0) {
        // Linear search from the end is the fastest approach, since
        // most lookups will happen at/near the end.
        for (i = transitionCount - 1; i > 0; --i) {
            int32_t transition = transitionTimes[i];
            if (local) {
                transition += zoneOffset(typeData[i]);
            }
            if (time >= transition) {
                break;
            }
        }

        U_ASSERT(i>=0 && i<transitionCount);

        // Check invariants for GMT times; if these pass for GMT times
        // the local logic should be working too.
        U_ASSERT(local || time < transitionTimes[0] || time >= transitionTimes[i]);
        U_ASSERT(local || i == transitionCount-1 || time < transitionTimes[i+1]);

        i = typeData[i];
    }

    U_ASSERT(i>=0 && i<typeCount);
    
    return i;
}

/**
 * TimeZone API.
 */
UBool OlsonTimeZone::useDaylightTime() const {
    // If DST was observed in 1942 (for example) but has never been
    // observed from 1943 to the present, most clients will expect
    // this method to return FALSE.  This method determines whether
    // DST is in use in the current year (at any point in the year)
    // and returns TRUE if so.

    int32_t days = Math::floorDivide(uprv_getUTCtime(), SECONDS_PER_DAY); // epoch days

    int32_t year, month, dom, dow;
    
    Grego::dayToFields(days, year, month, dom, dow);

    if (year > finalYear) { // [sic] >, not >=; see above
        U_ASSERT(finalZone != 0 && finalZone->useDaylightTime());
        return TRUE;
    }

    // Find start of this year, and start of next year
    int32_t start = (int32_t) Grego::fieldsToDay(year, 0, 1) * SECONDS_PER_DAY;    
    int32_t limit = (int32_t) Grego::fieldsToDay(year+1, 0, 1) * SECONDS_PER_DAY;    

    // Return TRUE if DST is observed at any time during the current
    // year.
    for (int16_t i=0; i<transitionCount; ++i) {
        if (transitionTimes[i] >= limit) {
            break;
        }
        if (transitionTimes[i] >= start &&
            dstOffset(typeData[i]) != 0) {
            return TRUE;
        }
    }
    return FALSE;
}

/**
 * TimeZone API.
 */
UBool OlsonTimeZone::inDaylightTime(UDate date, UErrorCode& ec) const {
    int32_t raw, dst;
    getOffset(date, FALSE, raw, dst, ec);
    return dst != 0;
}

U_NAMESPACE_END

#endif // !UCONFIG_NO_FORMATTING

//eof
