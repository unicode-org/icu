/*
*******************************************************************************
* Copyright (C) 1997-2003, International Business Machines Corporation and    *
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

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/resbund.h"
#include "unicode/gregocal.h"
#include "buddhcal.h"
#include "japancal.h"
#include "unicode/calendar.h"
#include "cpputils.h"
#include "iculserv.h"
#include "ucln_in.h"
#include "cstring.h"

U_NAMESPACE_BEGIN

// ------------------------------------------
//
// Registration
//
//-------------------------------------------
//#define U_DEBUG_CALSVC 1
//

#ifdef U_DEBUG_CALSVC
#include <stdio.h>
#endif

static ICULocaleService* gService = NULL;

// -------------------------------------

/**
 * a Calendar Factory which creates the "basic" calendar types, that is, those 
 * shipped with ICU.
 */

class BasicCalendarFactory : public LocaleKeyFactory {
public:
  /**
   * @param calendarType static const string (caller owns storage - will be aliased) to calendar type
   */
  BasicCalendarFactory(const char *calendarType)
    : LocaleKeyFactory(LocaleKeyFactory::INVISIBLE), fType(calendarType), fID(calendarType,"")  { }
  
  virtual ~BasicCalendarFactory() {}

protected:
  virtual UBool isSupportedID( const UnicodeString& id, UErrorCode& /* status */) const { return (id == fID); }

  virtual void updateVisibleIDs(Hashtable& result, UErrorCode& status) const
  {
    if (U_SUCCESS(status)) {
      const UnicodeString& id = fID;
      result.put(id, (void*)this, status);
    }
  }

  virtual UObject* create(const ICUServiceKey& key, const ICUService* /*service*/, UErrorCode& status) const {
    const LocaleKey& lkey = (LocaleKey&)key;
    Locale curLoc;  // current locale
    Locale canLoc;  // Canonical locale

    lkey.currentLocale(curLoc);
    lkey.canonicalLocale(canLoc);

    UnicodeString str;
    key.currentID(str);

#ifdef U_DEBUG_CALSVC
    fprintf(stderr, "BasicCalendarFactory[%s] - cur %s, can %s\n", fType, (const char*)curLoc.getName(), (const char*)canLoc.getName());
#endif

    if(str != fID) {  // Do we handle this type?
#ifdef U_DEBUG_CALSVC
      fprintf(stderr, "BasicCalendarFactory[%s] - not handling %s.\n", fType, (const char*) curLoc.getName() );
#endif
      return NULL;
    }

#ifdef U_DEBUG_CALSVC
    fprintf(stderr, "BasicCalendarFactory %p: creating %s type for %s\n", 
            this, fType, (const char*)curLoc.getName());
    fflush(stderr);
#endif

  if(!fType || !*fType || !uprv_strcmp(fType,"gregorian")) {  // Gregorian (default)
    return new GregorianCalendar(canLoc, status);
  } else if(!uprv_strcmp(fType, "japanese")) {
    return new JapaneseCalendar(canLoc, status);
  } else if(!uprv_strcmp(fType, "buddhist")) {
    return new BuddhistCalendar(canLoc, status);
  } else { 
    status = U_UNSUPPORTED_ERROR;
    return NULL;
  }
 }
private:
  const char *fType;
  const UnicodeString fID;
};


/** 
 * A factory which looks up the DefaultCalendar resource to determine which class of calendar to use
 */

class DefaultCalendarFactory : public ICUResourceBundleFactory {
public:
  DefaultCalendarFactory():  ICUResourceBundleFactory() { } 
protected:
  virtual UObject* create(const ICUServiceKey& key, const ICUService* /*service*/, UErrorCode& status) const  {

  LocaleKey &lkey = (LocaleKey&)key;
  Locale loc;
  lkey.currentLocale(loc);

#ifdef U_DEBUG_CALSVC
  fprintf(stderr, "DefaultCalendar factory %p: looking up %s\n", 
          this, (const char*)loc.getName());
#endif

  UErrorCode resStatus = U_ZERO_ERROR;

  UResourceBundle *rb = ures_open(NULL, (const char*)loc.getName(), &resStatus);

#ifdef U_DEBUG_CALSVC
  fprintf(stderr, "... ures_open -> %s\n", u_errorName(resStatus));
#endif
  if(U_FAILURE(resStatus) || 
     (resStatus == U_USING_DEFAULT_WARNING) || (resStatus==U_USING_FALLBACK_WARNING)) { //Don't want to handle fallback data.
    ures_close(rb);
    status = resStatus; // propagate err back to caller
#ifdef U_DEBUG_CALSVC
    fprintf(stderr, "... exitting (NULL)\n");
#endif

    return NULL;
  }

  int32_t len = 0;

  UnicodeString myString = ures_getUnicodeStringByKey(rb, Calendar::kDefaultCalendar, &status);

#ifdef U_DEBUG_CALSVC
  UErrorCode debugStatus = U_ZERO_ERROR;
  const UChar *defCal = ures_getStringByKey(rb, Calendar::kDefaultCalendar, &len,  &debugStatus);
  fprintf(stderr, "... get string(%d) -> %s\n", len, u_errorName(debugStatus));
#endif

  ures_close(rb);
  
   if(U_FAILURE(status)) {
    return NULL;
  }
 

#ifdef U_DEBUG_CALSVC
   {
     char defCalStr[200];
     if(len > 199) {
       len = 199;
     }
     u_UCharsToChars(defCal, defCalStr, len);
     defCalStr[len]=0;
     fprintf(stderr, "DefaultCalendarFactory: looked up %s, got DefaultCalendar= %s\n",  (const char*)loc.getName(), defCalStr);
   }
#endif

   return myString.clone();
 }
};

// -------------------------------------
class CalendarService : public ICULocaleService {
public:
  CalendarService()
    : ICULocaleService("Calendar")
  {
    UErrorCode status = U_ZERO_ERROR;
    registerFactory(new DefaultCalendarFactory(), status);
  }

  virtual UObject* cloneInstance(UObject* instance) const {
    if(instance->getDynamicClassID() == UnicodeString::getStaticClassID()) {
      return ((UnicodeString*)instance)->clone(); 
    } else {
#ifdef U_DEBUG_CALSVC_F
      UErrorCode status2 = U_ZERO_ERROR;
      fprintf(stderr, "Cloning a %s calendar with tz=%ld\n", ((Calendar*)instance)->getType(), ((Calendar*)instance)->get(UCAL_ZONE_OFFSET, status2));
#endif
      return ((Calendar*)instance)->clone();
    }
  }

  virtual UObject* handleDefault(const ICUServiceKey& key, UnicodeString* /*actualID*/, UErrorCode& status) const {
	LocaleKey& lkey = (LocaleKey&)key;
	//int32_t kind = lkey.kind();

	Locale loc;
	lkey.canonicalLocale(loc);

#ifdef U_DEBUG_CALSVC
        Locale loc2;
        lkey.currentLocale(loc2);
    fprintf(stderr, "CalSvc:handleDefault for currentLoc %s, canloc %s\n", (const char*)loc.getName(),  (const char*)loc2.getName());
#endif
	Calendar *nc =  new GregorianCalendar(loc, status);

#ifdef U_DEBUG_CALSVC
        UErrorCode status2 = U_ZERO_ERROR;
        fprintf(stderr, "New default calendar has tz=%d\n", ((Calendar*)nc)->get(UCAL_ZONE_OFFSET, status2));
#endif
        return nc;
  }

  virtual UBool isDefault() const {
    return countFactories() == 1;
  }
};

// -------------------------------------

static ICULocaleService* 
getService(void)
{
  UBool needInit;
  {
    Mutex mutex;
    needInit = (UBool)(gService == NULL);
  }
  if (needInit) {
    UErrorCode status = U_ZERO_ERROR;
#ifdef U_DEBUG_CALSVC
    fprintf(stderr, "Spinning up Calendar Service\n");
#endif
    ICULocaleService * newservice = new CalendarService();
#ifdef U_DEBUG_CALSVC
    fprintf(stderr, "Registering classes..\n");
#endif

    // Register all basic instances. 
    newservice->registerFactory(new BasicCalendarFactory("japanese"),status);
    newservice->registerFactory(new BasicCalendarFactory("buddhist"),status);
    newservice->registerFactory(new BasicCalendarFactory("gregorian"),status);

#ifdef U_DEBUG_CALSVC
    fprintf(stderr, "Done..\n");
#endif

    if(U_FAILURE(status)) {
#ifdef U_DEBUG_CALSVC
      fprintf(stderr, "err (%s) registering classes, deleting service.....\n", u_errorName(status));
#endif
      delete newservice;
      newservice = NULL;
    }
    
    if (newservice) {
      Mutex mutex;
      if (gService == NULL) {
        gService = newservice;
        newservice = NULL;
      }
    }
    if (newservice) {
      delete newservice;
    } else {
      // we won the contention - we can register the cleanup.
      ucln_i18n_registerCleanup();
    }
  }
  return gService;
}

// -------------------------------------


// Resource bundle tags read by this class
const char Calendar::kDateTimeElements[] = "DateTimeElements";
const char Calendar::kDefaultCalendar[] = "DefaultCalendar";

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
:   UObject(),
    fIsTimeSet(FALSE),
    fAreFieldsSet(FALSE),
    fAreAllFieldsSet(FALSE),
    fNextStamp(kMinimumUserStamp),
    fTime(0),
    fLenient(TRUE),
    fZone(0)
{
    clear();
    fZone = TimeZone::createDefault();
    setWeekCountData(Locale::getDefault(), success);
}

// -------------------------------------

Calendar::Calendar(TimeZone* zone, const Locale& aLocale, UErrorCode& success)
:   UObject(),
    fIsTimeSet(FALSE),
    fAreFieldsSet(FALSE),
    fAreAllFieldsSet(FALSE),
    fNextStamp(kMinimumUserStamp),
    fTime(0),
    fLenient(TRUE),
    fZone(0)
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
:   UObject(),
    fIsTimeSet(FALSE),
    fAreFieldsSet(FALSE),
    fAreAllFieldsSet(FALSE),
    fNextStamp(kMinimumUserStamp),
    fTime(0),
    fLenient(TRUE),
    fZone(0)
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
:   UObject(source)
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
        uprv_arrayCopy(right.fFields, fFields, UCAL_FIELD_COUNT);
        uprv_arrayCopy(right.fIsSet, fIsSet, UCAL_FIELD_COUNT);
        uprv_arrayCopy(right.fStamp, fStamp, UCAL_FIELD_COUNT);
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
  return createInstance(TimeZone::createDefault(), Locale::getDefault(), success);
}

// -------------------------------------

Calendar*
Calendar::createInstance(const TimeZone& zone, UErrorCode& success)
{
  return createInstance(zone, Locale::getDefault(), success);
}

// -------------------------------------

Calendar*
Calendar::createInstance(const Locale& aLocale, UErrorCode& success)
{
  return createInstance(TimeZone::createDefault(), aLocale, success);
}

// ------------------------------------- Adopting 

// Note: this is the bottleneck that actually calls the service routines.

Calendar*
Calendar::createInstance(TimeZone* zone, const Locale& aLocale, UErrorCode& success)
{
  UObject* u = getService()->get(aLocale, LocaleKey::KIND_ANY, success);
  Calendar* c = NULL;

  if(U_FAILURE(success) || !u) {
    delete zone;
    if(U_SUCCESS(success)) { // Propagate some kind of err
      success = U_INTERNAL_PROGRAM_ERROR;
    }
    return NULL;
  }
  
  if(u->getDynamicClassID() == UnicodeString::getStaticClassID()) {
    // It's a unicode string telling us what type of calendar to load ("gregorian", etc)
    char tmp[200];
    const UnicodeString& str = *(UnicodeString*)u;
    // Extract a char* out of it..
    int32_t len = str.length();
    if(len > sizeof(tmp)-1) {
      len = sizeof(tmp)-1;
    }
    str.extract(0,len,tmp);
    tmp[len]=0;
    
#ifdef U_DEBUG_CALSVC
    // fprintf(stderr, "createInstance(%s) told to look at %s..\n", (const char*)aLocale.getName(), tmp);
#endif

    // Create a Locale over this string
    Locale l(tmp);

    delete u;
    u = NULL;
    
    c = (Calendar*)getService()->get(l, LocaleKey::KIND_ANY, success);

    if(U_FAILURE(success) || !c) {
      delete zone;
      if(U_SUCCESS(success)) { 
        success = U_INTERNAL_PROGRAM_ERROR; // Propagate some err
      }
      return NULL;
    }
    
    if(c->getDynamicClassID() == UnicodeString::getStaticClassID()) {
      // recursed! Second lookup returned a UnicodeString. 
      // Perhaps DefaultCalendar{} was set to another locale.
      success = U_MISSING_RESOURCE_ERROR;  // requested a calendar type which could NOT be found.
      delete c;
      delete zone;
      return NULL;
    }
#ifdef U_DEBUG_CALSVC
    fprintf(stderr, "setting to locale %s\n", (const char*)aLocale.getName());
#endif
    c->setWeekCountData(aLocale, success);  // set the correct locale (this was an indirected calendar)
  } else {
    // a calendar was returned - we assume the factory did the right thing.
    c = (Calendar*)u;
  }

  // Now, reset calendar to default state:
  c->adoptTimeZone(zone); //  Set the correct time zone
  c->setTimeInMillis(getNow(), success); // let the new calendar have the current time.
  return c;
}

// -------------------------------------

Calendar*
Calendar::createInstance(const TimeZone& zone, const Locale& aLocale, UErrorCode& success)
{
  Calendar* c = createInstance(aLocale, success);
  if(U_SUCCESS(success) && c) {
    c->setTimeZone(zone);
  }
  return c; 
}

// -------------------------------------

UBool
Calendar::operator==(const Calendar& that) const
{
    UErrorCode status = U_ZERO_ERROR;
    return isEquivalentTo(that) &&
        getTimeInMillis(status) == that.getTimeInMillis(status) &&
        U_SUCCESS(status);
}

UBool 
Calendar::isEquivalentTo(const Calendar& other) const
{
    return getDynamicClassID() == other.getDynamicClassID() &&
        fLenient                == other.fLenient &&
        fFirstDayOfWeek         == other.fFirstDayOfWeek &&
        fMinimalDaysInFirstWeek == other.fMinimalDaysInFirstWeek &&
        *fZone                  == *other.fZone;
}

// -------------------------------------

UBool
Calendar::equals(const Calendar& when, UErrorCode& status) const
{
    return (this == &when ||
            getTime(status) == when.getTime(status));
}

// -------------------------------------

UBool
Calendar::before(const Calendar& when, UErrorCode& status) const
{
    return (this != &when &&
            getTimeInMillis(status) < when.getTimeInMillis(status));
}

// -------------------------------------

UBool
Calendar::after(const Calendar& when, UErrorCode& status) const
{
    return (this != &when &&
            getTimeInMillis(status) > when.getTimeInMillis(status));
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
    return (UDate)uprv_getUTCtime() * U_MILLIS_PER_SECOND; // return as milliseconds
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

    /* Test for buffer overflows */
    if(U_FAILURE(status)) {
        return 0.0;
    }
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

    /* Test for buffer overflows */
    if(U_FAILURE(status)) {
        return;
    }
    fAreFieldsSet = TRUE;
    fAreAllFieldsSet = TRUE;
}

// -------------------------------------

int32_t
Calendar::get(UCalendarDateFields field, UErrorCode& status) const
{
    // field values are only computed when actually requested; for more on when computation
    // of various things happens, see the "data flow in Calendar" description at the top
    // of this file
    if (U_SUCCESS(status)) ((Calendar*)this)->complete(status); // Cast away const
    return U_SUCCESS(status) ? fFields[field] : 0;
}

// -------------------------------------

void
Calendar::set(UCalendarDateFields field, int32_t value)
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
    set(UCAL_YEAR, year);
    set(UCAL_MONTH, month);
    set(UCAL_DATE, date);
}

// -------------------------------------

void
Calendar::set(int32_t year, int32_t month, int32_t date, int32_t hour, int32_t minute)
{
    set(UCAL_YEAR, year);
    set(UCAL_MONTH, month);
    set(UCAL_DATE, date);
    set(UCAL_HOUR_OF_DAY, hour);
    set(UCAL_MINUTE, minute);
}

// -------------------------------------

void
Calendar::set(int32_t year, int32_t month, int32_t date, int32_t hour, int32_t minute, int32_t second)
{
    set(UCAL_YEAR, year);
    set(UCAL_MONTH, month);
    set(UCAL_DATE, date);
    set(UCAL_HOUR_OF_DAY, hour);
    set(UCAL_MINUTE, minute);
    set(UCAL_SECOND, second);
}

// -------------------------------------

void
Calendar::clear()
{
    for (int32_t i=0; i<UCAL_FIELD_COUNT; ++i) {
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
Calendar::clear(UCalendarDateFields field)
{
    fFields[field]         = 0;
    fStamp[field]         = kUnset;
    fAreFieldsSet         = FALSE;
    fAreAllFieldsSet     = FALSE;
    fIsSet[field]         = FALSE; // Remove later
    fIsTimeSet             = FALSE;
}

// -------------------------------------

UBool
Calendar::isSet(UCalendarDateFields field) const
{
    return fStamp[field] != kUnset;
}

// -------------------------------------

void
Calendar::complete(UErrorCode& status)
{
    if (!fIsTimeSet) {
        updateTime(status);
        /* Test for buffer overflows */
        if(U_FAILURE(status)) {
            return;
        }
    }
    if (!fAreFieldsSet) {
        computeFields(status); // fills in unset fields
        /* Test for buffer overflows */
        if(U_FAILURE(status)) {
            return;
        }
        fAreFieldsSet         = TRUE;
        fAreAllFieldsSet     = TRUE;
    }
}

// -------------------------------------
int32_t Calendar::fieldDifference(UDate when, EDateFields field, UErrorCode& status) {

	return fieldDifference(when, (UCalendarDateFields) field, status);

}


int32_t Calendar::fieldDifference(UDate targetMs, UCalendarDateFields field, UErrorCode& ec) {
    if (U_FAILURE(ec)) return 0;
    int32_t min = 0;
    double startMs = getTimeInMillis(ec);
    // Always add from the start millis.  This accomodates
    // operations like adding years from February 29, 2000 up to
    // February 29, 2004.  If 1, 1, 1, 1 is added to the year
    // field, the DOM gets pinned to 28 and stays there, giving an
    // incorrect DOM difference of 1.  We have to add 1, reset, 2,
    // reset, 3, reset, 4.
    if (startMs < targetMs) {
        int32_t max = 1;
        // Find a value that is too large
        while (U_SUCCESS(ec)) {
            setTimeInMillis(startMs, ec);
            add(field, max, ec);
            double ms = getTimeInMillis(ec);
            if (ms == targetMs) {
                return max;
            } else if (ms > targetMs) {
                break;
            } else {
                max <<= 1;
                if (max < 0) {
                    // Field difference too large to fit into int32_t
                    ec = U_ILLEGAL_ARGUMENT_ERROR;
                }
            }
        }
        // Do a binary search
        while ((max - min) > 1 && U_SUCCESS(ec)) {
            int32_t t = (min + max) / 2;
            setTimeInMillis(startMs, ec);
            add(field, t, ec);
            double ms = getTimeInMillis(ec);
            if (ms == targetMs) {
                return t;
            } else if (ms > targetMs) {
                max = t;
            } else {
                min = t;
            }
        }
    } else if (startMs > targetMs) {
        int32_t max = -1;
        // Find a value that is too small
        while (U_SUCCESS(ec)) {
            setTimeInMillis(startMs, ec);
            add(field, max, ec);
            double ms = getTimeInMillis(ec);
            if (ms == targetMs) {
                return max;
            } else if (ms < targetMs) {
                break;
            } else {
                max <<= 1;
                if (max == 0) {
                    // Field difference too large to fit into int32_t
                    ec = U_ILLEGAL_ARGUMENT_ERROR;
                }
            }
        }
        // Do a binary search
        while ((min - max) > 1 && U_SUCCESS(ec)) {
            int32_t t = (min + max) / 2;
            setTimeInMillis(startMs, ec);
            add(field, t, ec);
            double ms = getTimeInMillis(ec);
            if (ms == targetMs) {
                return t;
            } else if (ms < targetMs) {
                max = t;
            } else {
                min = t;
            }
        }
    }
    // Set calendar to end point
    setTimeInMillis(startMs, ec);
    add(field, min, ec);
    
    /* Test for buffer overflows */
    if(U_FAILURE(ec)) {
        return 0;
    }
    return min;
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
Calendar::setLenient(UBool lenient)
{
    fLenient = lenient;
}

// -------------------------------------

UBool
Calendar::isLenient() const
{
    return fLenient;
}

// -------------------------------------

void
Calendar::setFirstDayOfWeek(UCalendarDaysOfWeek value)
{
  if (fFirstDayOfWeek != value &&
        value >= UCAL_SUNDAY && value <= UCAL_SATURDAY) {
        fFirstDayOfWeek = value;
        fAreFieldsSet = FALSE;
    }
}

// -------------------------------------

Calendar::EDaysOfWeek
Calendar::getFirstDayOfWeek() const
{
    return (Calendar::EDaysOfWeek)fFirstDayOfWeek;
}

UCalendarDaysOfWeek
Calendar::getFirstDayOfWeek(UErrorCode & /*status*/) const
{
    return fFirstDayOfWeek;
}
// -------------------------------------

void
Calendar::setMinimalDaysInFirstWeek(uint8_t value)
{
    // Values less than 1 have the same effect as 1; values greater
    // than 7 have the same effect as 7. However, we normalize values
    // so operator== and so forth work.
    if (value < 1) {
        value = 1;
    } else if (value > 7) {
        value = 7;
    }
    if (fMinimalDaysInFirstWeek != value) {
        fMinimalDaysInFirstWeek = value;
        fAreFieldsSet = FALSE;
    }
}

// -------------------------------------

uint8_t
Calendar::getMinimalDaysInFirstWeek() const
{
    return fMinimalDaysInFirstWeek;
}

// -------------------------------------

int32_t
Calendar::getActualMinimum(UCalendarDateFields field, UErrorCode& status) const
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

    /* Test for buffer overflows */
    if(U_FAILURE(status)) {
        return 0;
    }
    return result;
}

// -------------------------------------

int32_t
Calendar::getActualMaximum(UCalendarDateFields field, UErrorCode& status) const
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
    if (field == UCAL_WEEK_OF_YEAR || field == UCAL_WEEK_OF_MONTH)
        work->set(UCAL_DAY_OF_WEEK, fFirstDayOfWeek);

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

    /* Test for buffer overflows */
    if(U_FAILURE(status)) {
        return 0;
    }

    return result;
}

// -------------------------------------

void
Calendar::setWeekCountData(const Locale& desiredLocale, UErrorCode& status)
{
    // Read the week count data from the resource bundle.  This should
    // have the form:
    //
    //   DateTimeElements:intvector {
    //      1,    // first day of week
    //      1     // min days in week
    //   }
    //   Both have a range of 1..7


    if (U_FAILURE(status)) return;

    fFirstDayOfWeek = UCAL_SUNDAY;
    fMinimalDaysInFirstWeek = 1;

    UResourceBundle *resource = ures_open(NULL, desiredLocale.getName(), &status);

    // If the resource data doesn't seem to be present at all, then use last-resort
    // hard-coded data.
    if (U_FAILURE(status))
    {
        status = U_USING_FALLBACK_WARNING;
        ures_close(resource);
        return;
    }

    //dateTimeElements = resource.getStringArray(kDateTimeElements, count, status);
    UResourceBundle *dateTimeElements = ures_getByKey(resource, kDateTimeElements, NULL, &status);
    if (U_SUCCESS(status)) {
        int32_t arrLen;
        const int32_t *dateTimeElementsArr = ures_getIntVector(dateTimeElements, &arrLen, &status);

        if(U_SUCCESS(status) && arrLen == 2
            && 1 <= dateTimeElementsArr[0] && dateTimeElementsArr[0] <= 7
            && 1 <= dateTimeElementsArr[1] && dateTimeElementsArr[1] <= 7)
        {
            fFirstDayOfWeek = (UCalendarDaysOfWeek)dateTimeElementsArr[0];
            fMinimalDaysInFirstWeek = (uint8_t)dateTimeElementsArr[1];
        }
        else {
            status = U_INVALID_FORMAT_ERROR;
        }
    }

    ures_close(dateTimeElements);
    ures_close(resource);
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



U_NAMESPACE_END

U_CFUNC UBool calendar_cleanup(void) {
  if (gService) {
    delete gService;
    gService = NULL;
  }
  return TRUE;
}


#endif /* #if !UCONFIG_NO_FORMATTING */

//eof
