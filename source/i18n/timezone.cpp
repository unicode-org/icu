/*
*******************************************************************************
* Copyright (C) 1997-2003, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* File TIMEZONE.CPP
*
* Modification History:
*
*   Date        Name        Description
*   12/05/96    clhuang     Creation.
*   04/21/97    aliu        General clean-up and bug fixing.
*   05/08/97    aliu        Fixed Hashtable code per code review.
*   07/09/97    helena      Changed createInstance to createDefault.
*   07/29/97    aliu        Updated with all-new list of 96 UNIX-derived
*                           TimeZones.  Changed mechanism to load from static
*                           array rather than resource bundle.
*   07/07/1998  srl         Bugfixes from the Java side: UTC GMT CAT NST
*                           Added getDisplayName API
*                           going to add custom parsing.
*
*                           ISSUES:
*                               - should getDisplayName cache something?
*                               - should custom time zones be cached? [probably]
*  08/10/98     stephen     Brought getDisplayName() API in-line w/ conventions
*  08/19/98     stephen     Changed createTimeZone() to never return 0
*  09/02/98     stephen     Added getOffset(monthLen) and hasSameRules()
*  09/15/98     stephen     Added getStaticClassID()
*  02/22/99     stephen     Removed character literals for EBCDIC safety
*  05/04/99     stephen     Changed initDefault() for Mutex issues
*  07/12/99     helena      HPUX 11 CC Port.
*  12/03/99     aliu        Moved data out of static table into icudata.dll.
*                           Substantial rewrite of zone lookup, default zone, and
*                           available IDs code.  Misc. cleanup.
*********************************************************************************/

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/simpletz.h"
#include "unicode/smpdtfmt.h"
#include "unicode/calendar.h"
#include "unicode/gregocal.h"
#include "unicode/ures.h"
#include "uresimp.h" // struct UResourceBundle
#include "olsontz.h"
#include "mutex.h"
#include "unicode/udata.h"
#include "tzdat.h"
#include "ucln_in.h"
#include "cstring.h"
#include "cmemory.h"
#include "unicode/strenum.h"
#include "uassert.h"

#define ZONEINFO "zoneinfo"

// Static data and constants

static const UChar         GMT_ID[] = {0x47, 0x4D, 0x54, 0x00}; /* "GMT" */
static const int32_t       GMT_ID_LENGTH = 3;
static const UChar         CUSTOM_ID[] = 
{
    0x43, 0x75, 0x73, 0x74, 0x6F, 0x6D, 0x00 /* "Custom" */
};

static UMTX                LOCK;
static TimeZone*           DEFAULT_ZONE = NULL;
static TimeZone*           _GMT = NULL; // cf. TimeZone::GMT
const char                 TimeZone::fgClassID = 0; // Value is irrelevant

// #ifdef ICU_TIMEZONE_USE_DEPRECATES
static UnicodeString* OLSON_IDS = 0;
// #endif

UBool timeZone_cleanup()
{
// #ifdef ICU_TIMEZONE_USE_DEPRECATES
    delete []OLSON_IDS;
    OLSON_IDS = 0;
// #endif

    delete DEFAULT_ZONE;
    DEFAULT_ZONE = NULL;

    delete _GMT;
    _GMT = NULL;

    if (LOCK) {
        umtx_destroy(&LOCK);
        LOCK = NULL;
    }

    return TRUE;
}

U_NAMESPACE_BEGIN

/**
 * The Olson data is stored the "zoneinfo" resource bundle.
 * Sub-resources are organized into three ranges of data: Zones, final
 * rules, and country tables.  There is also a meta-data resource
 * which has 3 integers: The number of zones, rules, and countries,
 * respectively.  The country count includes the non-country '%' and
 * the rule count includes the non-rule '_' (the metadata).
 */
static int32_t OLSON_ZONE_START = -1; // starting index of zones
static int32_t OLSON_ZONE_COUNT = 0;  // count of zones

/* We could compute all of the following, but we don't need them
static int32_t OLSON_RULE_START = 0;  // starting index of rules
static int32_t OLSON_RULE_COUNT = 0;  // count of zones
static int32_t OLSON_COUNTRY_START = 0; // starting index of countries
static int32_t OLSON_COUNTRY_COUNT = 0; // count of zones
*/

/**
 * Given a pointer to an open "zoneinfo" resource, load up the Olson
 * meta-data. Return TRUE if successful.
 */
static UBool getOlsonMeta(const UResourceBundle* top) {
    if (OLSON_ZONE_START < 0) {
        UErrorCode ec = U_ZERO_ERROR;
        UResourceBundle res;
        ures_initStackObject(&res);
        ures_getByKey(top, "_", &res, &ec);
        int32_t len;
        const int32_t* v = ures_getIntVector(&res, &len, &ec);
        if (U_SUCCESS(ec) && len == 3) {
            OLSON_ZONE_COUNT = v[0];
            // Note: Remove the following `int32_t' declarations if you
            //       uncomment the declarations above!
            int32_t OLSON_RULE_COUNT = v[1];
            int32_t OLSON_COUNTRY_COUNT = v[2];

            // Try to figure out how strcmp in genrb is going to sort
            // things.  This has to be done here, dynamically, rather
            // than at build time.
            char zoneCh='A', ruleCh='_', countryCh='%'; // [sic]
            OLSON_ZONE_START = ((ruleCh < zoneCh) ? OLSON_RULE_COUNT : 0) +
                               ((countryCh < zoneCh) ? OLSON_COUNTRY_COUNT : 0);

            /* We could compute the following, but we don't need them -- yet
            OLSON_RULE_START = ((zoneCh < ruleCh) ? OLSON_ZONE_COUNT : 0) +
                               ((countryCh < ruleCh) ? OLSON_COUNTRY_COUNT : 0);
            OLSON_COUNTRY_START = ((ruleCh < countryCh) ? OLSON_RULE_COUNT : 0) +
                                  ((zoneCh < countryCh) ? OLSON_ZONE_COUNT : 0);
            */
        }
        ures_close(&res);
    }
    return (OLSON_ZONE_START >= 0);
}

/**
 * Load up the Olson meta-data. Return TRUE if successful.
 */
static UBool getOlsonMeta() {
    if (OLSON_ZONE_START < 0) {
        UErrorCode ec = U_ZERO_ERROR;
        UResourceBundle *top = ures_openDirect(0, ZONEINFO, &ec);
        if (U_SUCCESS(ec)) {
            getOlsonMeta(top);
        }
        ures_close(top);
    }
    return (OLSON_ZONE_START >= 0);
}

/**
 * Given an ID, open the appropriate resource for the given time zone.
 * Dereference aliases if necessary.
 * @param id zone id
 * @param res resource, which must be ready for use (initialized but not open)
 * @param ec input-output error code
 * @return top-level resource bundle
 */
static UResourceBundle* openOlsonResource(const UnicodeString& id,
                                          UResourceBundle& res,
                                          UErrorCode& ec) {
    char buf[128];
    id.extract(0, sizeof(buf)-1, buf, sizeof(buf), "");
    UResourceBundle *top = ures_openDirect(0, ZONEINFO, &ec);
    ures_getByKey(top, buf, &res, &ec);
    // Dereference if this is an alias.  Docs say result should be 1
    // but it is 0 in 2.8 (?).
    if (ures_getSize(&res) <= 1 && getOlsonMeta(top)) {
        int32_t deref = ures_getInt(&res, &ec) + OLSON_ZONE_START;
        ures_close(&res);
        ures_getByIndex(top, deref, &res, &ec);
    }
    return top;
}

// TODO: #ifdef out this code after 8-Nov-2003
// #ifdef ICU_TIMEZONE_USE_DEPRECATES

/**
 * Load all the ids from the "zoneinfo" resource bundle into a static
 * array that we hang onto.  This is _only_ used to implement the
 * deprecated createAvailableIDs() API.
 */
static UBool loadOlsonIDs() {
    if (OLSON_IDS != 0) {
        return TRUE;
    }

    UErrorCode ec = U_ZERO_ERROR;
    UnicodeString* ids = 0;
    int32_t count = 0;
    UResourceBundle *top = ures_openDirect(0, ZONEINFO, &ec);
    if (U_SUCCESS(ec)) {
        getOlsonMeta(top);
        int32_t start = OLSON_ZONE_START;
        count = OLSON_ZONE_COUNT;
        ids = new UnicodeString[(count > 0) ? count : 1];
        UResourceBundle res;
        ures_initStackObject(&res);
        for (int32_t i=0; i<count; ++i) {
            ures_getByIndex(top, start+i, &res, &ec);
            if (U_FAILURE(ec)) {
                ures_close(&res);
                break;
            }
            const char* key = ures_getKey(&res);
            U_ASSERT(*key != '_' && *key != '%');
            int32_t len = uprv_strlen(key);
            ids[i] = UnicodeString(key, len+1, "");
            ids[i].truncate(len); // add a NUL but don't count it so that
                                  // getBuffer() gets a terminated string
            ures_close(&res);
        }
    }
    ures_close(top);

    if (U_FAILURE(ec)) {
        delete[] ids;
        return FALSE;
    }

    // Keep mutexed operations as short as possible by doing all
    // computations first, then doing pointer copies within the mutex.
    umtx_lock(&LOCK);
    if (OLSON_IDS == 0) {
        OLSON_IDS = ids;
        ids = 0;
    }
    umtx_unlock(&LOCK);

    // If another thread initialized the statics first, then delete
    // our unused data.
    delete[] ids;
    return TRUE;
}

// #endif //ICU_TIMEZONE_USE_DEPRECATES

// -------------------------------------

const TimeZone*
TimeZone::getGMT(void)
{
    umtx_init(&LOCK);   /* This is here to prevent race conditions. */
    Mutex lock(&LOCK);
    // Initialize _GMT independently of other static data; it should
    // be valid even if we can't load the time zone UDataMemory.
    if (_GMT == 0) {
        _GMT = new SimpleTimeZone(0, UnicodeString(GMT_ID, GMT_ID_LENGTH));
    }
    return _GMT;
}

// *****************************************************************************
// class TimeZone
// *****************************************************************************

TimeZone::TimeZone()
    :   UObject(), fID()
{
}

// -------------------------------------

TimeZone::TimeZone(const UnicodeString &id)
    :   UObject(), fID(id)
{
}

// -------------------------------------

TimeZone::~TimeZone()
{
}

// -------------------------------------

TimeZone::TimeZone(const TimeZone &source)
    :   UObject(source), fID(source.fID)
{
}

// -------------------------------------

TimeZone &
TimeZone::operator=(const TimeZone &right)
{
    if (this != &right) fID = right.fID;
    return *this;
}

// -------------------------------------

UBool
TimeZone::operator==(const TimeZone& that) const
{
    return getDynamicClassID() == that.getDynamicClassID() &&
        fID == that.fID;
}

// -------------------------------------

TimeZone*
TimeZone::createTimeZone(const UnicodeString& ID)
{
    /* We first try to lookup the zone ID in our system list.  If this
     * fails, we try to parse it as a custom string GMT[+-]hh:mm.  If
     * all else fails, we return GMT, which is probably not what the
     * user wants, but at least is a functioning TimeZone object.
     *
     * We cannot return NULL, because that would break compatibility
     * with the JDK.
     */
    TimeZone* result = createSystemTimeZone(ID);

    if (result == 0) {
        result = createCustomTimeZone(ID);
    }
    if (result == 0) {
        result = getGMT()->clone();
    }
    return result;
}

/**
 * Lookup the given name in our system zone table.  If found,
 * instantiate a new zone of that name and return it.  If not
 * found, return 0.
 */
TimeZone*
TimeZone::createSystemTimeZone(const UnicodeString& id) {
    TimeZone* z = 0;
    UErrorCode ec = U_ZERO_ERROR;
    UResourceBundle res;
    ures_initStackObject(&res);
    UResourceBundle *top = openOlsonResource(id, res, ec);
    if (U_SUCCESS(ec)) {
        z = new OlsonTimeZone(top, &res, ec);
        if (z) z->setID(id);      // Hmm. cleanup? TODO
    }
    ures_close(&res);
    ures_close(top);
    if (U_FAILURE(ec)) {
        delete z;
        z = 0;
    }
    return z;
}

// -------------------------------------

/**
 * Initialize DEFAULT_ZONE from the system default time zone.  The
 * caller should confirm that DEFAULT_ZONE is NULL before calling.
 * Upon return, DEFAULT_ZONE will not be NULL, unless operator new()
 * returns NULL.
 *
 * Must be called OUTSIDE mutex.
 */
void
TimeZone::initDefault()
{ 
    // We access system timezone data through TPlatformUtilities,
    // including tzset(), timezone, and tzname[].
    int32_t rawOffset = 0;
    const char *hostID;

    // First, try to create a system timezone, based
    // on the string ID in tzname[0].
    {
        // NOTE: Global mutex here; TimeZone mutex above
        // mutexed to avoid threading issues in the platform fcns.
        // Some of the locale/timezone OS functions may not be thread safe, 
        //  so the intent is that any setting from anywhere within ICU 
        //  happens with the ICU global mutex held.
        Mutex lock; 
        uprv_tzset(); // Initialize tz... system data
        
        // Get the timezone ID from the host.  This function should do
        // any required host-specific remapping; e.g., on Windows this
        // function maps the Date and Time control panel setting to an
        // ICU timezone ID.
        hostID = uprv_tzname(0);
        
        // Invert sign because UNIX semantics are backwards
        rawOffset = uprv_timezone() * -U_MILLIS_PER_SECOND;
    }

    TimeZone* default_zone = NULL;

    default_zone = createSystemTimeZone(hostID);

#if 0
    // NOTE: As of ICU 2.8, we no longer have an offsets table, since
    // historical zones can change offset over time.  If we add
    // build-time heuristics to infer the "most frequent" raw offset
    // of a zone, we can build tables and institute defaults, as done
    // in ICU <= 2.6.

    // If we couldn't get the time zone ID from the host, use
    // the default host timezone offset.  Further refinements
    // to this include querying the host to determine if DST
    // is in use or not and possibly using the host locale to
    // select from multiple zones at a the same offset.  We
    // don't do any of this now, but we could easily add this.
    if (default_zone == NULL) {
        // Use the designated default in the time zone list that has the
        // appropriate GMT offset, if there is one.

        const OffsetIndex* index = INDEX_BY_OFFSET;

        for (;;) {
            if (index->gmtOffset > rawOffset) {
                // Went past our desired offset; no match found
                break;
            }
            if (index->gmtOffset == rawOffset) {
                // Found our desired offset
                default_zone = createSystemTimeZone(ZONE_IDS[index->defaultZone]);
                break;
            }
            // Compute the position of the next entry.  If the delta value
            // in this entry is zero, then there is no next entry.
            uint16_t delta = index->nextEntryDelta;
            if (delta == 0) {
                break;
            }
            index = (const OffsetIndex*)((int8_t*)index + delta);
        }
    }
#endif

    // Construct a fixed standard zone with the host's ID
    // and raw offset.
    if (default_zone == NULL) {
        default_zone =
            new SimpleTimeZone(rawOffset, UnicodeString(hostID, (char*)0));
    }

    // If we _still_ don't have a time zone, use GMT.
    if (default_zone == NULL) {
        default_zone = getGMT()->clone();
    }

    // If DEFAULT_ZONE is still NULL, set it up.
    umtx_lock(&LOCK);
    if (DEFAULT_ZONE == NULL) {
        DEFAULT_ZONE = default_zone;
        default_zone = NULL;
    }
    umtx_unlock(&LOCK);

    delete default_zone;
}

// -------------------------------------

TimeZone*
TimeZone::createDefault()
{
    umtx_init(&LOCK);   /* This is here to prevent race conditions. */
    umtx_lock(&LOCK);
    UBool f = (DEFAULT_ZONE != 0);
    umtx_unlock(&LOCK);
    if (!f) {
        initDefault();
    }

    Mutex lock(&LOCK); // In case adoptDefault is called
    return DEFAULT_ZONE->clone();
}

// -------------------------------------

void
TimeZone::adoptDefault(TimeZone* zone)
{
    if (zone != NULL)
    {
        TimeZone* old = NULL;

        umtx_init(&LOCK);   /* This is here to prevent race conditions. */
        umtx_lock(&LOCK);
        old = DEFAULT_ZONE;
        DEFAULT_ZONE = zone;
        umtx_unlock(&LOCK);

        delete old;
    }
}
// -------------------------------------

void
TimeZone::setDefault(const TimeZone& zone)
{
    adoptDefault(zone.clone());
}

//----------------------------------------------------------------------
// Support methods for getOffset(millis...)

// TODO clean up; consolidate with GregorianCalendar, TimeZone, StandarTimeZone

/**
 * Return floor(numerator/denominator); handles extreme values correctly.
 */
static int32_t floorDivide(int32_t numerator, int32_t denominator) {
    return (numerator >= 0) ?
        numerator / denominator : ((numerator + 1) / denominator) - 1;
}

static int32_t floorDivide(int32_t numerator, int32_t denominator,
                           int32_t& remainder) {
    int32_t quotient = floorDivide(numerator, denominator);
    remainder = numerator - (quotient * denominator);
    return quotient;
}

static double floorDivide(double numerator, double denominator,
                          double& remainder) {
    double quotient = uprv_floor(numerator / denominator);
    remainder = numerator - (quotient * denominator);
    return quotient;
}

static int32_t floorDivide(double numerator, int32_t denominator,
                           int32_t& remainder) {
    double quotient, rem;
    quotient = floorDivide(numerator, denominator, rem);
    remainder = (int32_t) rem;
    return (int32_t) quotient;
}

static const int32_t JULIAN_1_CE    = 1721426; // January 1, 1 CE Gregorian
static const int32_t JULIAN_1970_CE = 2440588; // January 1, 1970 CE Gregorian

/**
 * The number of days before the given month.  There are two sets of
 * values for Jan..Dec; one set for non-leap years followed by another
 * set for leap years.
 */
static const int16_t DAYS_BEFORE[] =
    {0,31,59,90,120,151,181,212,243,273,304,334,
     0,31,60,91,121,152,182,213,244,274,305,335};

static const int8_t MONTH_LENGTH[] =
    {31,28,31,30,31,30,31,31,30,31,30,31,
     31,29,31,30,31,30,31,31,30,31,30,31};

static UBool isLeapYear(int year) {
    return (year%4 == 0) && ((year%100 != 0) || (year%400 == 0));
}

/**
 * Convert a 1970-epoch day number to proleptic Gregorian year, month,
 * day-of-month, and day-of-week.
 * @param day 1970-epoch day (integral value)
 * @param year output parameter to receive year
 * @param month output parameter to receive month (0-based, 0==Jan)
 * @param dom output parameter to receive day-of-month (1-based)
 * @param dow output parameter to receive day-of-week (1-based, 1==Sun)
 */
static void dayToFields(double day, int32_t& year, int32_t& month,
                        int32_t& dom, int32_t& dow) {
    int32_t doy;

    // Convert from 1970 CE epoch to 1 CE epoch (Gregorian calendar)
    day += JULIAN_1970_CE - JULIAN_1_CE;

    int32_t n400 = floorDivide(day, 146097, doy); // 400-year cycle length
    int32_t n100 = floorDivide(doy, 36524, doy); // 100-year cycle length
    int32_t n4   = floorDivide(doy, 1461, doy); // 4-year cycle length
    int32_t n1   = floorDivide(doy, 365, doy);
    year = 400*n400 + 100*n100 + 4*n4 + n1;
    if (n100 == 4 || n1 == 4) {
        doy = 365; // Dec 31 at end of 4- or 400-year cycle
    } else {
        ++year;
    }
    
    UBool isLeap = isLeapYear(year);
    
    // Gregorian day zero is a Monday.
    dow = (int32_t) uprv_fmod(day + 1, 7);
    dow += (dow < 0) ? (UCAL_SUNDAY + 7) : UCAL_SUNDAY;

    // Common Julian/Gregorian calculation
    int32_t correction = 0;
    int32_t march1 = isLeap ? 60 : 59; // zero-based DOY for March 1
    if (doy >= march1) {
        correction = isLeap ? 1 : 2;
    }
    month = (12 * (doy + correction) + 6) / 367; // zero-based month
    dom = doy - DAYS_BEFORE[month + (isLeap ? 12 : 0)] + 1; // one-based DOM
}

//----------------------------------------------------------------------

/**
 * This is the default implementation for subclasses that do not
 * override this method.  This implementation calls through to the
 * 8-argument getOffset() method after suitable computations, and
 * correctly adjusts GMT millis to local millis when necessary.
 */
void TimeZone::getOffset(UDate date, UBool local, int32_t& rawOffset,
                         int32_t& dstOffset, UErrorCode& ec) const {
    if (U_FAILURE(ec)) {
        return;
    }

    rawOffset = getRawOffset();

    // Convert to local wall millis if necessary
    if (!local) {
        date += rawOffset; // now in local standard millis
    }

    // When local==FALSE, we might have to recompute. This loop is
    // executed once, unless a recomputation is required; then it is
    // executed twice.
    for (int32_t pass=0; ; ++pass) {
        int32_t year, month, dom, dow;
        double day = uprv_floor(date / U_MILLIS_PER_DAY);
        int32_t millis = (int32_t) (date - day * U_MILLIS_PER_DAY);
        
        dayToFields(day, year, month, dom, dow);
        
        dstOffset = getOffset(GregorianCalendar::AD, year, month, dom,
                              (uint8_t) dow, millis,
                              MONTH_LENGTH[month + isLeapYear(year)?12:0],
                              ec) - rawOffset;

        // Recompute if local==FALSE, dstOffset!=0, and addition of
        // the dstOffset puts us in a different day.
        if (pass!=0 || local || dstOffset==0) {
            break;
        }
        date += dstOffset;
        if (uprv_floor(date / U_MILLIS_PER_DAY) == day) {
            break;
        }
    }
}

// -------------------------------------

// New available IDs API as of ICU 2.4.  Uses StringEnumeration API.

class TZEnumeration : public StringEnumeration {
    // Map into to zones.  Our results are zone[map[i]] for
    // i=0..len-1, where zone[i] is the i-th Olson zone.  If map==NULL
    // then our results are zone[i] for i=0..len-1.  Len will be zero
    // iff the zone data could not be loaded.
    int32_t* map;
    int32_t  len;
    int32_t  pos;
    void*    _bufp;
    int32_t  _buflen;
    UnicodeString id;

public:
    TZEnumeration() {
        map = NULL;
        _bufp = NULL;
        len = pos = _buflen = 0;
        if (getOlsonMeta()) {
            len = OLSON_ZONE_COUNT;
        }
    }

    TZEnumeration(int32_t rawOffset) {
        map = NULL;
        _bufp = NULL;
        len = pos = _buflen = 0;
        if (!getOlsonMeta()) {
            return;
        }

        // Allocate more space than we'll need.  The end of the array will
        // be blank.
        map = (int32_t*)uprv_malloc(OLSON_ZONE_COUNT * sizeof(int32_t));
        if (map == 0) {
            return;
        }

        uprv_memset(map, 0, sizeof(int32_t) * OLSON_ZONE_COUNT);

        UnicodeString s;
        for (int32_t i=0; i<OLSON_ZONE_COUNT; ++i) {
            if (getID(i)) {
                // This is VERY inefficient.
                TimeZone* z = TimeZone::createTimeZone(id);
                // Make sure we get back the ID we wanted (if the ID is
                // invalid we get back GMT).
                if (z != 0 && z->getID(s) == id &&
                    z->getRawOffset() == rawOffset) {
                    map[len++] = i;
                }
                delete z;
            }
        }
    }

    TZEnumeration(const char* country) {
        map = NULL;
        _bufp = NULL;
        len = pos = _buflen = 0;
        if (!getOlsonMeta()) {
            return;
        }

        char key[4] = {'%', 0, 0, 0}; // e.g., "%US", or "%" for no country
        if (country) uprv_strncat(key, country, 2);

        UErrorCode ec = U_ZERO_ERROR;
        UResourceBundle *top = ures_openDirect(0, ZONEINFO, &ec);
        if (U_SUCCESS(ec)) {
            UResourceBundle res;
            ures_initStackObject(&res);
            ures_getByKey(top, key, &res, &ec);
            // The list of zones is a list of integers, from 0..n-1,
            // where n is the total number of system zones.
            const int32_t* v = ures_getIntVector(&res, &len, &ec);
            if (U_SUCCESS(ec)) {
                U_ASSERT(len > 0);
                map = (int32_t*)uprv_malloc(sizeof(int32_t) * len);
                if (map != 0) {
                    for (uint16_t i=0; i<len; ++i) {
                        U_ASSERT(v[i] >= 0 && v[i] < OLSON_ZONE_COUNT);
                        map[i] = v[i];
                    }
                }
            }
            ures_close(&res);
        }
        ures_close(top);
    }

    virtual ~TZEnumeration() {
        uprv_free(map);
        uprv_free(_bufp);
    }

    int32_t count(UErrorCode& status) const {
        return U_FAILURE(status) ? 0 : len;
    }

    const char* next(int32_t* resultLength, UErrorCode& status) {
        // TODO: Later a subclass of StringEnumeration will be available
        // that implements next() and unext() in terms of snext().
        // Inherit from that class when available and remove this method
        // (and its declaration).
        const UnicodeString* us = snext(status);
        int32_t newlen;
        if (us != NULL && ensureCapacity((newlen=us->length()) + 1)) {
            us->extract(0, INT32_MAX, (char*) _bufp, "");
            if (resultLength) {
                resultLength[0] = newlen;
            }
            return (const char*)_bufp;
        }
        return NULL;
    }

    const UChar* unext(int32_t* resultLength, UErrorCode& status) {
        const UnicodeString* us = snext(status);
        if (us != NULL) {
          if (resultLength) {
            resultLength[0] = us->length();
          }
          U_ASSERT(us == &id); // A little ugly...
          return id.getTerminatedBuffer(); // ...but works
        }
        return NULL;
    }

    const UnicodeString* snext(UErrorCode& status) {
        if (U_SUCCESS(status) && pos < len) {
            getID((map == 0) ? pos : map[pos]);
            ++pos;
            return &id;
        }
        return 0;
    }

    void reset(UErrorCode& /*status*/) {
        pos = 0;
    }

private:

    UBool getID(int32_t i) {
        UErrorCode ec = U_ZERO_ERROR;
        UResourceBundle *top = ures_openDirect(0, ZONEINFO, &ec);
        UResourceBundle res;
        ures_initStackObject(&res);
        ures_getByIndex(top, OLSON_ZONE_START + i, &res, &ec);
        if (U_SUCCESS(ec)) {
            const char* key = ures_getKey(&res);
            id = UnicodeString(key, "");
        } else {
            id.truncate(0);
        }
        ures_close(&res);
        ures_close(top);
        return U_SUCCESS(ec);
    }

    static const char fgClassID;

public:
    static inline UClassID getStaticClassID(void) { return (UClassID)&fgClassID; }
    virtual UClassID getDynamicClassID(void) const { return getStaticClassID(); }
private:
    /**
     * Guarantee that _bufp is allocated to include _buflen characters
     * where _buflen >= minlen.  Return TRUE if successful, FALSE
     * otherwise.
     */
    UBool ensureCapacity(int32_t minlen) {
        if (_bufp != NULL && _buflen >= minlen) {
            return TRUE;
        }
        _buflen = minlen + 8; // add 8 to prevent thrashing
        _bufp = (_bufp == NULL) ? uprv_malloc(_buflen)
                                : uprv_realloc(_bufp, _buflen);
        return _bufp != NULL;
    }
};

const char TZEnumeration::fgClassID = '\0';

StringEnumeration*
TimeZone::createEnumeration() {
    return new TZEnumeration();
}

StringEnumeration*
TimeZone::createEnumeration(int32_t rawOffset) {
    return new TZEnumeration(rawOffset);
}

StringEnumeration*
TimeZone::createEnumeration(const char* country) {
    return new TZEnumeration(country);
}

// -------------------------------------

// TODO: #ifdef out this code after 8-Nov-2003
// #ifdef ICU_TIMEZONE_USE_DEPRECATES

const UnicodeString** 
TimeZone::createAvailableIDs(int32_t rawOffset, int32_t& numIDs)
{
    // We are creating a new array to existing UnicodeString pointers.
    // The caller will delete the array when done, but not the pointers
    // in the array.

    numIDs = 0;
    if (!loadOlsonIDs()) {
        return 0;
    }

    // Allocate more space than we'll need.  The end of the array will
    // be blank.
    const UnicodeString** ids =
        (const UnicodeString** )uprv_malloc(OLSON_ZONE_COUNT * sizeof(UnicodeString *));
    if (ids == 0) {
        return 0;
    }

    uprv_memset(ids, 0, sizeof(UnicodeString*) * OLSON_ZONE_COUNT);

    UnicodeString s;
    for (int32_t i=0; i<OLSON_ZONE_COUNT; ++i) {
        // This is VERY inefficient.
        TimeZone* z = TimeZone::createTimeZone(OLSON_IDS[i]);
        // Make sure we get back the ID we wanted (if the ID is
        // invalid we get back GMT).
        if (z != 0 && z->getID(s) == OLSON_IDS[i] &&
            z->getRawOffset() == rawOffset) {
            ids[numIDs++] = &OLSON_IDS[i]; // [sic]
        }
        delete z;
    }

    return ids;
}

// -------------------------------------

const UnicodeString** 
TimeZone::createAvailableIDs(const char* country, int32_t& numIDs) {

    // We are creating a new array to existing UnicodeString pointers.
    // The caller will delete the array when done, but not the pointers
    // in the array.

    numIDs = 0;
    if (!loadOlsonIDs()) {
        return 0;
    }
    
    char key[4] = {'%', 0, 0, 0}; // e.g., "%US", or "%" for non-country zones
    if (country) uprv_strncat(key, country, 2);

    const UnicodeString** ids = 0;

    UErrorCode ec = U_ZERO_ERROR;
    UResourceBundle *top = ures_openDirect(0, ZONEINFO, &ec);
    if (U_SUCCESS(ec)) {
        getOlsonMeta(top);
        UResourceBundle res;
        ures_initStackObject(&res);
        ures_getByKey(top, key, &res, &ec);
        if (U_SUCCESS(ec)) {
            /* The list of zones is a list of integers, from 0..n-1,
             * where n is the total number of system zones.  The
             * numbering corresponds exactly to the ordering of
             * OLSON_IDS.
             */
            const int32_t* v = ures_getIntVector(&res, &numIDs, &ec);
            ids = (const UnicodeString**)
                uprv_malloc(numIDs * sizeof(UnicodeString*));
            if (ids == 0) {
                numIDs = 0;
            } else {
                for (int32_t i=0; i<numIDs; ++i) {
                    ids[i] = &OLSON_IDS[v[i]]; // [sic]
                }
            }
        }
        ures_close(&res);
    }
    ures_close(top);

    return ids;
}

// -------------------------------------

const UnicodeString** 
TimeZone::createAvailableIDs(int32_t& numIDs)
{
    // We are creating a new array to existing UnicodeString pointers.
    // The caller will delete the array when done, but not the pointers
    // in the array.
    numIDs = 0;
    if (!loadOlsonIDs()) {
        return 0;
    }
    
    const UnicodeString** ids =
        (const UnicodeString** )uprv_malloc(OLSON_ZONE_COUNT * sizeof(UnicodeString *));
    if (ids != 0) {
        numIDs = OLSON_ZONE_COUNT;
        for (int32_t i=0; i<numIDs; ++i) {
            ids[i] = &OLSON_IDS[i];
        }
    }

    return ids;
}

// ICU_TIMEZONE_USE_DEPRECATES
// #endif
// see above

// ---------------------------------------

int32_t
TimeZone::countEquivalentIDs(const UnicodeString& id) {
    int32_t result = 0;
    UErrorCode ec = U_ZERO_ERROR;
    UResourceBundle res;
    ures_initStackObject(&res);
    UResourceBundle *top = openOlsonResource(id, res, ec);
    if (U_SUCCESS(ec)) {
        int32_t size = ures_getSize(&res);
        if (size == 4 || size == 6) {
            UResourceBundle r;
            ures_initStackObject(&r);
            ures_getByIndex(&res, size-1, &r, &ec);
            // result = ures_getSize(&r); // doesn't work
            ures_getIntVector(&r, &result, &ec);
            ures_close(&r);
        }
    }
    ures_close(&res);
    ures_close(top);
    return result;
}

// ---------------------------------------

const UnicodeString
TimeZone::getEquivalentID(const UnicodeString& id, int32_t index) {
    UnicodeString result;
    UErrorCode ec = U_ZERO_ERROR;
    UResourceBundle res;
    ures_initStackObject(&res);
    UResourceBundle *top = openOlsonResource(id, res, ec);
    int32_t zone = -1;
    if (U_SUCCESS(ec)) {
        int32_t size = ures_getSize(&res);
        if (size == 4 || size == 6) {
            UResourceBundle r;
            ures_initStackObject(&r);
            ures_getByIndex(&res, size-1, &r, &ec);
            const int32_t* v = ures_getIntVector(&r, &size, &ec);
            if (index >= 0 && index < size && getOlsonMeta()) {
                zone = v[index] + OLSON_ZONE_START;
            }
            ures_close(&r);
        }
    }
    ures_close(&res);
    if (zone >= 0) {
        ures_getByIndex(top, zone, &res, &ec);
        if (U_SUCCESS(ec)) {
            const char* key = ures_getKey(&res);
            result = UnicodeString(key, "");
        }
        ures_close(&res);
    }
    ures_close(top);
    return result;
}

// ---------------------------------------


UnicodeString&
TimeZone::getDisplayName(UnicodeString& result) const
{
    return getDisplayName(FALSE,LONG,Locale::getDefault(), result);
}

UnicodeString&
TimeZone::getDisplayName(const Locale& locale, UnicodeString& result) const
{
    return getDisplayName(FALSE, LONG, locale, result);
}

UnicodeString&
TimeZone::getDisplayName(UBool daylight, EDisplayType style, UnicodeString& result)  const
{
    return getDisplayName(daylight,style, Locale::getDefault(), result);
}

UnicodeString&
TimeZone::getDisplayName(UBool daylight, EDisplayType style, const Locale& locale, UnicodeString& result) const
{
    // SRL TODO: cache the SDF, just like java.
    UErrorCode status = U_ZERO_ERROR;

    SimpleDateFormat format(style == LONG ? "zzzz" : "z",locale,status);

    if(!U_SUCCESS(status))
    {
        // *** SRL what do I do here?!!
        return result.remove();
    }

    // Create a new SimpleTimeZone as a stand-in for this zone; the
    // stand-in will have no DST, or all DST, but the same ID and offset,
    // and hence the same display name.
    // We don't cache these because they're small and cheap to create.
    UnicodeString tempID;
    SimpleTimeZone *tz =  daylight ?
        // For the pure-DST zone, we use JANUARY and DECEMBER

        new SimpleTimeZone(getRawOffset(), getID(tempID),
                           UCAL_JANUARY , 1, 0, 0,
                           UCAL_DECEMBER , 31, 0, U_MILLIS_PER_DAY, status) :
        new SimpleTimeZone(getRawOffset(), getID(tempID));

    format.applyPattern(style == LONG ? "zzzz" : "z");
    Calendar *myCalendar = (Calendar*)format.getCalendar();
    myCalendar->setTimeZone(*tz); // copy
    
    delete tz;

    FieldPosition pos(FieldPosition::DONT_CARE);
    return format.format(UDate(196262345678.), result, pos); // Must use a valid date here.
}


/**
 * Parse a custom time zone identifier and return a corresponding zone.
 * @param id a string of the form GMT[+-]hh:mm, GMT[+-]hhmm, or
 * GMT[+-]hh.
 * @return a newly created SimpleTimeZone with the given offset and
 * no Daylight Savings Time, or null if the id cannot be parsed.
*/
TimeZone*
TimeZone::createCustomTimeZone(const UnicodeString& id)
{
    static const int32_t         kParseFailed = -99999;

    NumberFormat* numberFormat = 0;
    
    UnicodeString idUppercase = id;
    idUppercase.toUpper();

    if (id.length() > GMT_ID_LENGTH &&
        idUppercase.startsWith(GMT_ID))
    {
        ParsePosition pos(GMT_ID_LENGTH);
        UBool negative = FALSE;
        int32_t offset;

        if (id[pos.getIndex()] == 0x002D /*'-'*/)
            negative = TRUE;
        else if (id[pos.getIndex()] != 0x002B /*'+'*/)
            return 0;
        pos.setIndex(pos.getIndex() + 1);

        UErrorCode success = U_ZERO_ERROR;
        numberFormat = NumberFormat::createInstance(success);
        numberFormat->setParseIntegerOnly(TRUE);

    
        // Look for either hh:mm, hhmm, or hh
        int32_t start = pos.getIndex();
        
        Formattable n(kParseFailed);

        numberFormat->parse(id, n, pos);
        if (pos.getIndex() == start) {
            delete numberFormat;
            return 0;
        }
        offset = n.getLong();

        if (pos.getIndex() < id.length() &&
            id[pos.getIndex()] == 0x003A /*':'*/)
        {
            // hh:mm
            offset *= 60;
            pos.setIndex(pos.getIndex() + 1);
            int32_t oldPos = pos.getIndex();
            n.setLong(kParseFailed);
            numberFormat->parse(id, n, pos);
            if (pos.getIndex() == oldPos) {
                delete numberFormat;
                return 0;
            }
            offset += n.getLong();
        }
        else 
        {
            // hhmm or hh

            // Be strict about interpreting something as hh; it must be
            // an offset < 30, and it must be one or two digits. Thus
            // 0010 is interpreted as 00:10, but 10 is interpreted as
            // 10:00.
            if (offset < 30 && (pos.getIndex() - start) <= 2)
                offset *= 60; // hh, from 00 to 29; 30 is 00:30
            else
                offset = offset % 100 + offset / 100 * 60; // hhmm
        }

        if(negative)
            offset = -offset;

        delete numberFormat;
        return new SimpleTimeZone(offset * 60000, CUSTOM_ID);
    }
    return 0;
}


UBool 
TimeZone::hasSameRules(const TimeZone& other) const
{
    return (getRawOffset() == other.getRawOffset() && 
            useDaylightTime() == other.useDaylightTime());
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

//eof
