/*
*******************************************************************************
* Copyright (C) 1997-1999, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* File CALENDAR.CPP
*
* Modification History:
*
*   Date        Name        Description
*   02/03/97    clhuang     Creation.
*   04/22/97    aliu        Cleaned up, fixed memory leak, made 
*                           setWeekCountData() more robust.  
*                           Moved platform code to TPlatformUtilities.
*   05/01/97    aliu        Made equals(), before(), after() arguments const.
*   05/20/97    aliu        Changed logic of when to compute fields and time
*                           to fix bugs.
*   08/12/97    aliu        Added equivalentTo.  Misc other fixes.
*   07/28/98    stephen     Sync up with JDK 1.2
*   09/02/98    stephen     Sync with JDK 1.2 8/31 build (getActualMin/Max)
*   03/17/99    stephen     Changed adoptTimeZone() - now fAreFieldsSet is
*                           set to FALSE to force update of time.
*******************************************************************************
*/

#include "cpputils.h"
#include "resbund.h"
#include "gregocal.h"

// Resource bundle tags read by this class
const char* Calendar::kDateTimeElements = "DateTimeElements";

// Data flow in Calendar
// ---------------------

// The current time is represented in two ways by Calendar: as UTC
// milliseconds from the epoch start (1 January 1970 0:00 UTC), and as local
// fields such as MONTH, HOUR, AM_PM, etc.  It is possible to compute the
// millis from the fields, and vice versa.  The data needed to do this
// conversion is encapsulated by a TimeZone object owned by the Calendar.
// The data provided by the TimeZone object may also be overridden if the
// user sets the ZONE_OFFSET and/or DST_OFFSET fields directly. The class
// keeps track of what information was most recently set by the caller, and
// uses that to compute any other information as needed.

// If the user sets the fields using set(), the data flow is as follows.
// This is implemented by the Calendar subclass's computeTime() method.
// During this process, certain fields may be ignored.  The disambiguation
// algorithm for resolving which fields to pay attention to is described
// above.

//   local fields (YEAR, MONTH, DATE, HOUR, MINUTE, etc.)
//           |
//           | Using Calendar-specific algorithm
//           V
//   local standard millis
//           |
//           | Using TimeZone or user-set ZONE_OFFSET / DST_OFFSET
//           V
//   UTC millis (in time data member)

// If the user sets the UTC millis using setTime(), the data flow is as
// follows.  This is implemented by the Calendar subclass's computeFields()
// method.

//   UTC millis (in time data member)
//           |
//           | Using TimeZone getOffset()
//           V
//   local standard millis
//           |
//           | Using Calendar-specific algorithm
//           V
//   local fields (YEAR, MONTH, DATE, HOUR, MINUTE, etc.)

// In general, a round trip from fields, through local and UTC millis, and
// back out to fields is made when necessary.  This is implemented by the
// complete() method.  Resolving a partial set of fields into a UTC millis
// value allows all remaining fields to be generated from that value.  If
// the Calendar is lenient, the fields are also renormalized to standard
// ranges when they are regenerated.

// -------------------------------------

Calendar::Calendar(UErrorCode& success)
:   fTime(0),
    fIsTimeSet(FALSE),
    fAreFieldsSet(FALSE),
    fAreAllFieldsSet(FALSE),
    fLenient(TRUE),
    fZone(0),
    fNextStamp(kMinimumUserStamp)
{
    clear();
    fZone = TimeZone::createDefault();
    setWeekCountData(Locale::getDefault(), success);
}

// -------------------------------------

Calendar::Calendar(TimeZone* zone, const Locale& aLocale, UErrorCode& success)
:   fTime(0),
    fIsTimeSet(FALSE),
    fAreFieldsSet(FALSE),
    fAreAllFieldsSet(FALSE),
    fLenient(TRUE),
    fZone(0),
    fNextStamp(kMinimumUserStamp)
{
    if(zone == 0) {
        success = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    clear();    
    fZone = zone;
    
    setWeekCountData(aLocale, success);
}

// -------------------------------------

Calendar::Calendar(const TimeZone& zone, const Locale& aLocale, UErrorCode& success)
:   fTime(0),
    fIsTimeSet(FALSE),
    fAreFieldsSet(FALSE),
    fAreAllFieldsSet(FALSE),
    fLenient(TRUE),
    fZone(0),
    fNextStamp(kMinimumUserStamp)
{
    clear();
    fZone = zone.clone();
    setWeekCountData(aLocale, success);
}

// -------------------------------------

Calendar::~Calendar()
{
    delete fZone;
}

// -------------------------------------

Calendar::Calendar(const Calendar &source)
{
    fZone = 0;
    *this = source;
}

// -------------------------------------

Calendar &
Calendar::operator=(const Calendar &right)
{
    if (this != &right)
    {
        icu_arrayCopy(right.fFields, fFields, FIELD_COUNT);
        icu_arrayCopy(right.fIsSet, fIsSet, FIELD_COUNT);
        icu_arrayCopy(right.fStamp, fStamp, FIELD_COUNT);
        fTime                     = right.fTime;
        fIsTimeSet                 = right.fIsTimeSet;
        fAreAllFieldsSet         = right.fAreAllFieldsSet;
        fAreFieldsSet             = right.fAreFieldsSet;
        fLenient                 = right.fLenient;
        delete fZone;
        fZone                    = right.fZone->clone();
        fFirstDayOfWeek         = right.fFirstDayOfWeek;
        fMinimalDaysInFirstWeek = right.fMinimalDaysInFirstWeek;
        fNextStamp                 = right.fNextStamp;
    }
    
    return *this;
}

// -------------------------------------

Calendar*
Calendar::createInstance(UErrorCode& success)
{
    if (U_FAILURE(success)) return 0;
    // right now, createInstance will always return an instance of GregorianCalendar
    Calendar* c = new GregorianCalendar(success);
    if (U_FAILURE(success)) { delete c; c = 0; }
    return c;
}

// -------------------------------------

Calendar*
Calendar::createInstance(const TimeZone& zone, UErrorCode& success)
{
    if (U_FAILURE(success)) return 0;
    // since the Locale isn't specified, use the default locale
    Calendar* c = new GregorianCalendar(zone, Locale::getDefault(), success);
    if (U_FAILURE(success)) { delete c; c = 0; }
    return c;
}

// -------------------------------------

Calendar*
Calendar::createInstance(const Locale& aLocale, UErrorCode& success)
{
    if (U_FAILURE(success)) return 0;
    // since the TimeZone isn't specfied, use the default time zone
    Calendar* c = new GregorianCalendar(TimeZone::createDefault(), aLocale, success);
    if (U_FAILURE(success)) { delete c; c = 0; }
    return c;
}

// -------------------------------------

Calendar*
Calendar::createInstance(TimeZone* zone, const Locale& aLocale, UErrorCode& success)
{
    if (U_FAILURE(success)) {
        delete zone;
        return 0;
    }
    Calendar* c = new GregorianCalendar(zone, aLocale, success);
    if (c == 0) {
        success = U_MEMORY_ALLOCATION_ERROR;
        delete zone;
    } else if (U_FAILURE(success)) {
        delete c;
        c = 0;
    }
    return c;
}

// -------------------------------------

Calendar*
Calendar::createInstance(const TimeZone& zone, const Locale& aLocale, UErrorCode& success)
{
    if (U_FAILURE(success)) return 0;
    Calendar* c = new GregorianCalendar(zone, aLocale, success);
    if (U_FAILURE(success)) { delete c; c = 0; }
    return c;
}

// -------------------------------------

bool_t
Calendar::operator==(const Calendar& that) const
{
    UErrorCode status = U_ZERO_ERROR;
    // {sfb} is this correct? (Java equals)
    return (getDynamicClassID() == that.getDynamicClassID() && 
        getTimeInMillis(status) == that.getTimeInMillis(status) &&
        fLenient == that.fLenient &&
        fFirstDayOfWeek == that.fFirstDayOfWeek &&
        fMinimalDaysInFirstWeek == that.fMinimalDaysInFirstWeek &&
        *fZone == *(that.fZone));

    // As it stands, this is a very narrowly defined ==, since the
    // Calendars must not only represent the same time; they must
    // also be in exactly the same state.  This would be looser if
    // we forced field or fTime computation, and then did the comparison.
/*
    if (this == &that) return TRUE;
    for (int32_t i=0; i<FIELD_COUNT; ++i)
    {
        if (fFields[i] != that.fFields[i] ||
            fIsSet[i] != that.fIsSet[i]) return FALSE;
    }
    return (getDynamicClassID() == that.getDynamicClassID() &&
            fTime == that.fTime &&
            fIsTimeSet == that.fIsTimeSet &&
            fAreAllFieldsSet == that.fAreAllFieldsSet &&
            fAreFieldsSet == that.fAreFieldsSet &&
            fLenient == that.fLenient &&
            (!fIsSet[ZONE_OFFSET] || (fUserSetZoneOffset == that.fUserSetZoneOffset)) &&
            (!fIsSet[DST_OFFSET] || (fUserSetDSTOffset == that.fUserSetDSTOffset)) &&
            fFirstDayOfWeek == that.fFirstDayOfWeek &&
            fMinimalDaysInFirstWeek == that.fMinimalDaysInFirstWeek &&
            *fZone == *(that.fZone));
*/
}

// -------------------------------------

bool_t
Calendar::equals(const Calendar& when, UErrorCode& status) const
{
    return (this == &when ||
            getTime(status) == when.getTime(status));
}

// -------------------------------------

bool_t
Calendar::before(const Calendar& when, UErrorCode& status) const
{
    return (this != &when &&
            getTimeInMillis(status) < when.getTimeInMillis(status));
}

// -------------------------------------

bool_t
Calendar::after(const Calendar& when, UErrorCode& status) const
{
    return (this != &when &&
            getTimeInMillis(status) > when.getTimeInMillis(status));
}

// {sfb} not in Java API, but looks similar to operator==
bool_t 
Calendar::equivalentTo(const Calendar& other) const
{
    // Return true if another Calendar object is equivalent to this one.  An equivalent
    // Calendar will behave exactly as this one does (i.e., it will have be the same subclass
    // of Calendar, and have the same time zone, week-count values, and leniency level), 
    // but may be set to a different time.
    return getDynamicClassID() == other.getDynamicClassID() &&
        fLenient                == other.fLenient &&
        fFirstDayOfWeek         == other.fFirstDayOfWeek &&
        fMinimalDaysInFirstWeek == other.fMinimalDaysInFirstWeek &&
        *fZone                  == *other.fZone;
}

// -------------------------------------


const Locale*
Calendar::getAvailableLocales(int32_t& count)
{
    return Locale::getAvailableLocales(count);
}

// -------------------------------------

UDate
Calendar::getNow()
{
    return (UDate)icu_getUTCtime() * U_MILLIS_PER_SECOND; // return as milliseconds
}

// -------------------------------------

/**
 * Gets this Calendar's current time as a long.
 * @return the current time as UTC milliseconds from the epoch.
 */
double 
Calendar::getTimeInMillis(UErrorCode& status) const
{
    if(U_FAILURE(status)) 
        return 0.0;

    if ( ! fIsTimeSet) 
        ((Calendar*)this)->updateTime(status);
    return fTime;
}

// -------------------------------------

/**
 * Sets this Calendar's current time from the given long value.
 * @param date the new time in UTC milliseconds from the epoch.
 */
void 
Calendar::setTimeInMillis( double millis, UErrorCode& status ) {
    if(U_FAILURE(status)) 
        return;

    fIsTimeSet = TRUE;
    fTime = millis;

    fAreFieldsSet = FALSE;

    computeFields(status);
    fAreFieldsSet = TRUE;
    fAreAllFieldsSet = TRUE;
}

// -------------------------------------

int32_t
Calendar::get(EDateFields field, UErrorCode& status) const
{
    // field values are only computed when actually requested; for more on when computation
    // of various things happens, see the "data flow in Calendar" description at the top
    // of this file
    if (U_SUCCESS(status)) ((Calendar*)this)->complete(status); // Cast away const
    return U_SUCCESS(status) ? fFields[field] : 0;
}

// -------------------------------------

void
Calendar::set(EDateFields field, int32_t value)
{
    fIsTimeSet         = FALSE;
    fFields[field]     = value;
    fStamp[field]     = fNextStamp++;
    fAreFieldsSet     = FALSE;
    fIsSet[field]     = TRUE; // Remove later
}

// -------------------------------------

void
Calendar::set(int32_t year, int32_t month, int32_t date)
{
    set(YEAR, year);
    set(MONTH, month);
    set(DATE, date);
}

// -------------------------------------

void
Calendar::set(int32_t year, int32_t month, int32_t date, int32_t hour, int32_t minute)
{
    set(YEAR, year);
    set(MONTH, month);
    set(DATE, date);
    set(HOUR_OF_DAY, hour);
    set(MINUTE, minute);
}

// -------------------------------------

void
Calendar::set(int32_t year, int32_t month, int32_t date, int32_t hour, int32_t minute, int32_t second)
{
    set(YEAR, year);
    set(MONTH, month);
    set(DATE, date);
    set(HOUR_OF_DAY, hour);
    set(MINUTE, minute);
    set(SECOND, second);
}

// -------------------------------------

void
Calendar::clear()
{
    for (int32_t i=0; i<FIELD_COUNT; ++i) {
        fFields[i]     = 0; // Must do this; other code depends on it
        fIsSet[i]     = FALSE;
        fStamp[i]     = kUnset;
    }
    
    fAreFieldsSet         = FALSE;
    fAreAllFieldsSet    = FALSE;
    fIsTimeSet             = FALSE;
}

// -------------------------------------

void
Calendar::clear(EDateFields field)
{
    fFields[field]         = 0;
    fStamp[field]         = kUnset;
    fAreFieldsSet         = FALSE;
    fAreAllFieldsSet     = FALSE;
    fIsSet[field]         = FALSE; // Remove later
    fIsTimeSet             = FALSE;
}

// -------------------------------------

bool_t
Calendar::isSet(EDateFields field) const
{
    return fStamp[field] != kUnset;
}

// -------------------------------------

void
Calendar::complete(UErrorCode& status)
{
    if (!fIsTimeSet) 
        updateTime(status);
    if (!fAreFieldsSet) {
        computeFields(status); // fills in unset fields
        fAreFieldsSet         = TRUE;
        fAreAllFieldsSet     = TRUE;
    }
}

// -------------------------------------

void
Calendar::adoptTimeZone(TimeZone* zone)
{
    // Do nothing if passed-in zone is NULL
    if (zone == NULL) return;

    // fZone should always be non-null
    if (fZone != NULL) delete fZone;
    fZone = zone;

    // if the zone changes, we need to recompute the time fields
    fAreFieldsSet = FALSE;
}

// -------------------------------------
void
Calendar::setTimeZone(const TimeZone& zone)
{
    adoptTimeZone(zone.clone());
}

// -------------------------------------

const TimeZone&
Calendar::getTimeZone() const
{
    return *fZone;
}

// -------------------------------------

TimeZone*
Calendar::orphanTimeZone()
{
    TimeZone *z = fZone;
    // we let go of the time zone; the new time zone is the system default time zone
    fZone = TimeZone::createDefault();
    return z;
}

// -------------------------------------

void
Calendar::setLenient(bool_t lenient)
{
    fLenient = lenient;
}

// -------------------------------------

bool_t
Calendar::isLenient() const
{
    return fLenient;
}

// -------------------------------------

void
Calendar::setFirstDayOfWeek(EDaysOfWeek value)
{
    fFirstDayOfWeek = value;
}

// -------------------------------------

Calendar::EDaysOfWeek
Calendar::getFirstDayOfWeek() const
{
    return fFirstDayOfWeek;
}

// -------------------------------------

void
Calendar::setMinimalDaysInFirstWeek(uint8_t value)
{
    fMinimalDaysInFirstWeek = value;
}

// -------------------------------------

uint8_t
Calendar::getMinimalDaysInFirstWeek() const
{
    return fMinimalDaysInFirstWeek;
}

// -------------------------------------

int32_t
Calendar::getActualMinimum(EDateFields field, UErrorCode& status) const
{
    int32_t fieldValue = getGreatestMinimum(field);
    int32_t endValue = getMinimum(field);

    // if we know that the minimum value is always the same, just return it
    if (fieldValue == endValue) {
        return fieldValue;
    }

    // clone the calendar so we don't mess with the real one, and set it to
    // accept anything for the field values
    Calendar *work = (Calendar*)this->clone();
    work->setLenient(TRUE);

    // now try each value from getLeastMaximum() to getMaximum() one by one until
    // we get a value that normalizes to another value.  The last value that
    // normalizes to itself is the actual minimum for the current date
    int32_t result = fieldValue;

    do {
        work->set(field, fieldValue);
        if (work->get(field, status) != fieldValue) {
            break;
        } 
        else {
            result = fieldValue;
            fieldValue--;
        }
    } while (fieldValue >= endValue);

    delete work;
    return result;
}

// -------------------------------------

int32_t
Calendar::getActualMaximum(EDateFields field, UErrorCode& status) const
{
    int32_t fieldValue = getLeastMaximum(field);
    int32_t endValue = getMaximum(field);

    // if we know that the maximum value is always the same, just return it
    if (fieldValue == endValue) {
        return fieldValue;
    }

    // clone the calendar so we don't mess with the real one, and set it to
    // accept anything for the field values
    Calendar *work = (Calendar*)this->clone();
    work->setLenient(TRUE);

    // if we're counting weeks, set the day of the week to Sunday.  We know the
    // last week of a month or year will contain the first day of the week.
    if (field == WEEK_OF_YEAR || field == WEEK_OF_MONTH)
        work->set(DAY_OF_WEEK, fFirstDayOfWeek);

    // now try each value from getLeastMaximum() to getMaximum() one by one until
    // we get a value that normalizes to another value.  The last value that
    // normalizes to itself is the actual maximum for the current date
    int32_t result = fieldValue;

    do {
        work->set(field, fieldValue);
        if(work->get(field, status) != fieldValue) {
            break;
        } 
        else {
            result = fieldValue;
            fieldValue++;
        }
    } while (fieldValue <= endValue);

    delete work;
    return result;
}

// -------------------------------------

int32_t Calendar::stringToDayNumber(const UnicodeString& string, UErrorCode& status)
{
    // Convert a UnicodeString to a long integer, using the standard C library.
    // Return both the value obtained, and a UErrorCode indicating success or failure.
    // We fail if the string is zero length, of if strtol() does not parse all
    // of the characters in the string, or if the value is not in the range
    // 1..7.  (This is used to read the week-count data from the resource files;
    // ResourceBundle returns all data in string form, so we have to convert it here.)
    if (U_FAILURE(status)) return 0;

    int32_t len = string.length();
    char *number = new char[1 + len];
    if (number == 0) { status = U_MEMORY_ALLOCATION_ERROR; return 0; }
    char *end;

    string.extract(0, len, number);
    number[len] = 0;
    int32_t value = strtol(number, &end, 10); // Radix 10

    delete[] number;

    if (end-number != len || len == 0 || value < 1 || value > 7)
        status = U_INVALID_FORMAT_ERROR;

    return value;
}

// -------------------------------------

void
Calendar::setWeekCountData(const Locale& desiredLocale, UErrorCode& status)
{
    // Read the week count data from the resource bundle.  This should
    // have the form:
    //
    //   DateTimeElements {
    //      "1",    // first day of week
    //      "1"     // min days in week
    //   }

    const UnicodeString *dateTimeElements;
    int32_t count;

    if (U_FAILURE(status)) return;
    ResourceBundle resource(Locale::getDataDirectory(), desiredLocale, status);

    // If the resource data doesn't seem to be present at all, then use last-resort
    // hard-coded data.
    if (U_FAILURE(status))
    {
        status = U_USING_FALLBACK_ERROR;
        fFirstDayOfWeek = Calendar::SUNDAY;
        fMinimalDaysInFirstWeek = 1;
        return;
    }

    dateTimeElements = resource.getStringArray(kDateTimeElements, count, status);
    if (U_FAILURE(status)) return;
    if (count != 2)
    {
        status = U_INVALID_FORMAT_ERROR;
        return;
    }

    fFirstDayOfWeek = (Calendar::EDaysOfWeek)stringToDayNumber(dateTimeElements[0], status);
    fMinimalDaysInFirstWeek = (uint8_t)stringToDayNumber(dateTimeElements[1], status);
}

/**
 * Recompute the time and update the status fields isTimeSet
 * and areFieldsSet.  Callers should check isTimeSet and only
 * call this method if isTimeSet is false.
 */
void 
Calendar::updateTime(UErrorCode& status) 
{
    computeTime(status);
    if(U_FAILURE(status))
        return;
        
    // If we are lenient, we need to recompute the fields to normalize
    // the values.  Also, if we haven't set all the fields yet (i.e.,
    // in a newly-created object), we need to fill in the fields. [LIU]
    if (isLenient() || ! fAreAllFieldsSet) 
        fAreFieldsSet = FALSE;
    
    fIsTimeSet = TRUE;
}

//eof
