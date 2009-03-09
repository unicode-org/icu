/*
*******************************************************************************
*   Copyright (C) 1996-2009, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*/

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/ucal.h"
#include "unicode/uloc.h"
#include "unicode/calendar.h"
#include "unicode/timezone.h"
#include "unicode/gregocal.h"
#include "unicode/simpletz.h"
#include "unicode/ustring.h"
#include "unicode/strenum.h"
#include "cmemory.h"
#include "cstring.h"
#include "ustrenum.h"
#include "uenumimp.h"
#include "ulist.h"

U_NAMESPACE_USE

static TimeZone*
_createTimeZone(const UChar* zoneID, int32_t len, UErrorCode* ec) {
    TimeZone* zone = NULL;
    if (ec!=NULL && U_SUCCESS(*ec)) {
        // Note that if zoneID is invalid, we get back GMT. This odd
        // behavior is by design and goes back to the JDK. The only
        // failure we will see is a memory allocation failure.
        int32_t l = (len<0 ? u_strlen(zoneID) : len);
        UnicodeString zoneStrID;
        zoneStrID.setTo((UBool)(len < 0), zoneID, l); /* temporary read-only alias */
        zone = TimeZone::createTimeZone(zoneStrID);
        if (zone == NULL) {
            *ec = U_MEMORY_ALLOCATION_ERROR;
        }
    }
    return zone;
}

U_CAPI UEnumeration* U_EXPORT2
ucal_openTimeZones(UErrorCode* ec) {
    return uenum_openStringEnumeration(TimeZone::createEnumeration(), ec);
}

U_CAPI UEnumeration* U_EXPORT2
ucal_openCountryTimeZones(const char* country, UErrorCode* ec) {
    return uenum_openStringEnumeration(TimeZone::createEnumeration(country), ec);
}

U_CAPI int32_t U_EXPORT2
ucal_getDefaultTimeZone(UChar* result, int32_t resultCapacity, UErrorCode* ec) {
    int32_t len = 0;
    if (ec!=NULL && U_SUCCESS(*ec)) {
        TimeZone* zone = TimeZone::createDefault();
        if (zone == NULL) {
            *ec = U_MEMORY_ALLOCATION_ERROR;
        } else {
            UnicodeString id;
            zone->getID(id);
            delete zone;
            len = id.extract(result, resultCapacity, *ec);
        }
    }
    return len;
}

U_CAPI void U_EXPORT2
ucal_setDefaultTimeZone(const UChar* zoneID, UErrorCode* ec) {
    TimeZone* zone = _createTimeZone(zoneID, -1, ec);
    if (zone != NULL) {
        TimeZone::adoptDefault(zone);
    }
}

U_CAPI int32_t U_EXPORT2
ucal_getDSTSavings(const UChar* zoneID, UErrorCode* ec) {
    int32_t result = 0;
    TimeZone* zone = _createTimeZone(zoneID, -1, ec);
    if (U_SUCCESS(*ec)) {
        if (zone->getDynamicClassID() == SimpleTimeZone::getStaticClassID()) {
            result = ((SimpleTimeZone*) zone)->getDSTSavings();
        } else {
            // Since there is no getDSTSavings on TimeZone, we use a
            // heuristic: Starting with the current time, march
            // forwards for one year, looking for DST savings.
            // Stepping by weeks is sufficient.
            UDate d = Calendar::getNow();
            for (int32_t i=0; i<53; ++i, d+=U_MILLIS_PER_DAY*7.0) {
                int32_t raw, dst;
                zone->getOffset(d, FALSE, raw, dst, *ec);
                if (U_FAILURE(*ec)) {
                    break;
                } else if (dst != 0) {
                    result = dst;
                    break;
                }
            }
        }
    }
    delete zone;
    return result;
}

U_CAPI UDate  U_EXPORT2
ucal_getNow()
{

  return Calendar::getNow();
}

#define ULOC_LOCALE_IDENTIFIER_CAPACITY (ULOC_FULLNAME_CAPACITY + 1 + ULOC_KEYWORD_AND_VALUES_CAPACITY)

U_CAPI UCalendar*  U_EXPORT2
ucal_open(  const UChar*  zoneID,
            int32_t       len,
            const char*   locale,
            UCalendarType caltype,
            UErrorCode*   status)
{

  if(U_FAILURE(*status)) return 0;
  
  TimeZone* zone = (zoneID==NULL) ? TimeZone::createDefault()
      : _createTimeZone(zoneID, len, status);

  if (U_FAILURE(*status)) {
      return NULL;
  }

  if ( caltype == UCAL_GREGORIAN ) {
      char  localeBuf[ULOC_LOCALE_IDENTIFIER_CAPACITY];
      uprv_strncpy(localeBuf, locale, ULOC_LOCALE_IDENTIFIER_CAPACITY);
      uloc_setKeywordValue("calendar", "gregorian", localeBuf, ULOC_LOCALE_IDENTIFIER_CAPACITY, status);
      if (U_FAILURE(*status)) {
          return NULL;
      }
      return (UCalendar*)Calendar::createInstance(zone, Locale(localeBuf), *status);
  }
  return (UCalendar*)Calendar::createInstance(zone, Locale(locale), *status);
}

U_CAPI void U_EXPORT2
ucal_close(UCalendar *cal)
{

  delete (Calendar*) cal;
}

U_CAPI UCalendar* U_EXPORT2 
ucal_clone(const UCalendar* cal,
           UErrorCode*      status)
{
  if(U_FAILURE(*status)) return 0;
  
  Calendar* res = ((Calendar*)cal)->clone();

  if(res == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return 0;
  }

  return (UCalendar*) res;
}

U_CAPI void  U_EXPORT2
ucal_setTimeZone(    UCalendar*      cal,
            const    UChar*            zoneID,
            int32_t        len,
            UErrorCode *status)
{

  if(U_FAILURE(*status))
    return;

  TimeZone* zone = (zoneID==NULL) ? TimeZone::createDefault()
      : _createTimeZone(zoneID, len, status);

  if (zone != NULL) {
      ((Calendar*)cal)->adoptTimeZone(zone);
  }
}

U_CAPI int32_t U_EXPORT2
ucal_getTimeZoneDisplayName(const     UCalendar*                 cal,
                    UCalendarDisplayNameType     type,
                    const char             *locale,
                    UChar*                  result,
                    int32_t                 resultLength,
                    UErrorCode*             status)
{

    if(U_FAILURE(*status)) return -1;

    const TimeZone& tz = ((Calendar*)cal)->getTimeZone();
    UnicodeString id;
    if(!(result==NULL && resultLength==0)) {
        // NULL destination for pure preflighting: empty dummy string
        // otherwise, alias the destination buffer
        id.setTo(result, 0, resultLength);
    }

    switch(type) {
  case UCAL_STANDARD:
      tz.getDisplayName(FALSE, TimeZone::LONG, Locale(locale), id);
      break;

  case UCAL_SHORT_STANDARD:
      tz.getDisplayName(FALSE, TimeZone::SHORT, Locale(locale), id);
      break;

  case UCAL_DST:
      tz.getDisplayName(TRUE, TimeZone::LONG, Locale(locale), id);
      break;

  case UCAL_SHORT_DST:
      tz.getDisplayName(TRUE, TimeZone::SHORT, Locale(locale), id);
      break;
    }

    return id.extract(result, resultLength, *status);
}

U_CAPI UBool  U_EXPORT2
ucal_inDaylightTime(    const    UCalendar*      cal, 
                    UErrorCode*     status )
{

    if(U_FAILURE(*status)) return (UBool) -1;
    return ((Calendar*)cal)->inDaylightTime(*status);
}

U_CAPI void U_EXPORT2
ucal_setGregorianChange(UCalendar *cal, UDate date, UErrorCode *pErrorCode) {
    if(U_FAILURE(*pErrorCode)) {
        return;
    }
    Calendar *cpp_cal = (Calendar *)cal;
    if(cpp_cal->getDynamicClassID() != GregorianCalendar::getStaticClassID()) {
        *pErrorCode = U_UNSUPPORTED_ERROR;
        return;
    }
    ((GregorianCalendar *)cpp_cal)->setGregorianChange(date, *pErrorCode);
}

U_CAPI UDate U_EXPORT2
ucal_getGregorianChange(const UCalendar *cal, UErrorCode *pErrorCode) {
    if(U_FAILURE(*pErrorCode)) {
        return (UDate)0;
    }
    Calendar *cpp_cal = (Calendar *)cal;
    if(cpp_cal->getDynamicClassID() != GregorianCalendar::getStaticClassID()) {
        *pErrorCode = U_UNSUPPORTED_ERROR;
        return (UDate)0;
    }
    return ((GregorianCalendar *)cpp_cal)->getGregorianChange();
}

U_CAPI int32_t U_EXPORT2
ucal_getAttribute(    const    UCalendar*              cal,
                  UCalendarAttribute      attr)
{

    switch(attr) {
  case UCAL_LENIENT:
      return ((Calendar*)cal)->isLenient();

  case UCAL_FIRST_DAY_OF_WEEK:
      return ((Calendar*)cal)->getFirstDayOfWeek();

  case UCAL_MINIMAL_DAYS_IN_FIRST_WEEK:
      return ((Calendar*)cal)->getMinimalDaysInFirstWeek();

  default:
      break;
    }
    return -1;
}

U_CAPI void U_EXPORT2
ucal_setAttribute(      UCalendar*              cal,
                  UCalendarAttribute      attr,
                  int32_t                 newValue)
{

    switch(attr) {
  case UCAL_LENIENT:
      ((Calendar*)cal)->setLenient((UBool)newValue);
      break;

  case UCAL_FIRST_DAY_OF_WEEK:
      ((Calendar*)cal)->setFirstDayOfWeek((UCalendarDaysOfWeek)newValue);
      break;

  case UCAL_MINIMAL_DAYS_IN_FIRST_WEEK:
      ((Calendar*)cal)->setMinimalDaysInFirstWeek((uint8_t)newValue);
      break;
    }
}

U_CAPI const char* U_EXPORT2
ucal_getAvailable(int32_t index)
{

    return uloc_getAvailable(index);
}

U_CAPI int32_t U_EXPORT2
ucal_countAvailable()
{

    return uloc_countAvailable();
}

U_CAPI UDate  U_EXPORT2
ucal_getMillis(    const    UCalendar*      cal,
               UErrorCode*     status)
{

    if(U_FAILURE(*status)) return (UDate) 0;

    return ((Calendar*)cal)->getTime(*status);
}

U_CAPI void  U_EXPORT2
ucal_setMillis(        UCalendar*      cal,
               UDate           dateTime,
               UErrorCode*     status )
{
    if(U_FAILURE(*status)) return;

    ((Calendar*)cal)->setTime(dateTime, *status);
}

// TBD: why does this take an UErrorCode?
U_CAPI void  U_EXPORT2
ucal_setDate(        UCalendar*        cal,
             int32_t            year,
             int32_t            month,
             int32_t            date,
             UErrorCode        *status)
{

    if(U_FAILURE(*status)) return;

    ((Calendar*)cal)->set(year, month, date);
}

// TBD: why does this take an UErrorCode?
U_CAPI void  U_EXPORT2
ucal_setDateTime(    UCalendar*        cal,
                 int32_t            year,
                 int32_t            month,
                 int32_t            date,
                 int32_t            hour,
                 int32_t            minute,
                 int32_t            second,
                 UErrorCode        *status)
{
    if(U_FAILURE(*status)) return;

    ((Calendar*)cal)->set(year, month, date, hour, minute, second);
}

U_CAPI UBool  U_EXPORT2
ucal_equivalentTo(    const UCalendar*      cal1,
                  const UCalendar*      cal2)
{

    return ((Calendar*)cal1)->isEquivalentTo(*((Calendar*)cal2));
}

U_CAPI void  U_EXPORT2
ucal_add(    UCalendar*                cal,
         UCalendarDateFields        field,
         int32_t                    amount,
         UErrorCode*                status)
{

    if(U_FAILURE(*status)) return;

    ((Calendar*)cal)->add(field, amount, *status);
}

U_CAPI void  U_EXPORT2
ucal_roll(        UCalendar*            cal,
          UCalendarDateFields field,
          int32_t                amount,
          UErrorCode*            status)
{

    if(U_FAILURE(*status)) return;

    ((Calendar*)cal)->roll(field, amount, *status);
}

U_CAPI int32_t  U_EXPORT2
ucal_get(    const    UCalendar*                cal,
         UCalendarDateFields        field,
         UErrorCode*                status )
{

    if(U_FAILURE(*status)) return -1;

    return ((Calendar*)cal)->get(field, *status);
}

U_CAPI void  U_EXPORT2
ucal_set(    UCalendar*                cal,
         UCalendarDateFields        field,
         int32_t                    value)
{

    ((Calendar*)cal)->set(field, value);
}

U_CAPI UBool  U_EXPORT2
ucal_isSet(    const    UCalendar*                cal,
           UCalendarDateFields        field)
{

    return ((Calendar*)cal)->isSet(field);
}

U_CAPI void  U_EXPORT2
ucal_clearField(    UCalendar*            cal,
                UCalendarDateFields field)
{

    ((Calendar*)cal)->clear(field);
}

U_CAPI void  U_EXPORT2
ucal_clear(UCalendar* calendar)
{

    ((Calendar*)calendar)->clear();
}

U_CAPI int32_t  U_EXPORT2
ucal_getLimit(    const    UCalendar*              cal,
              UCalendarDateFields     field,
              UCalendarLimitType      type,
              UErrorCode        *status)
{

    if(status==0 || U_FAILURE(*status)) {
        return -1;
    }

    switch(type) {
  case UCAL_MINIMUM:
      return ((Calendar*)cal)->getMinimum(field);

  case UCAL_MAXIMUM:
      return ((Calendar*)cal)->getMaximum(field);

  case UCAL_GREATEST_MINIMUM:
      return ((Calendar*)cal)->getGreatestMinimum(field);

  case UCAL_LEAST_MAXIMUM:
      return ((Calendar*)cal)->getLeastMaximum(field);

  case UCAL_ACTUAL_MINIMUM:
      return ((Calendar*)cal)->getActualMinimum(field,
          *status);

  case UCAL_ACTUAL_MAXIMUM:
      return ((Calendar*)cal)->getActualMaximum(field,
          *status);

  default:
      break;
    }
    return -1;
}

U_CAPI const char * U_EXPORT2
ucal_getLocaleByType(const UCalendar *cal, ULocDataLocaleType type, UErrorCode* status) 
{
    if (cal == NULL) {
        if (U_SUCCESS(*status)) {
            *status = U_ILLEGAL_ARGUMENT_ERROR;
        }
        return NULL;
    }
    return ((Calendar*)cal)->getLocaleID(type, *status);
}

U_CAPI const char * U_EXPORT2
ucal_getTZDataVersion(UErrorCode* status)
{
    return TimeZone::getTZDataVersion(*status);
}

U_CAPI int32_t U_EXPORT2
ucal_getCanonicalTimeZoneID(const UChar* id, int32_t len,
                            UChar* result, int32_t resultCapacity, UBool *isSystemID, UErrorCode* status) {
    if(status == 0 || U_FAILURE(*status)) {
        return 0;
    }
    if (isSystemID) {
        *isSystemID = FALSE;
    }
    if (id == 0 || len == 0 || result == 0 || resultCapacity <= 0) {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }
    int32_t reslen = 0;
    UnicodeString canonical;
    UBool systemID = FALSE;
    TimeZone::getCanonicalID(UnicodeString(id, len), canonical, systemID, *status);
    if (U_SUCCESS(*status)) {
        if (isSystemID) {
            *isSystemID = systemID;
        }
        reslen = canonical.extract(result, resultCapacity, *status);
    }
    return reslen;
}

U_CAPI const char * U_EXPORT2
ucal_getType(const UCalendar *cal, UErrorCode* status)
{
    if (U_FAILURE(*status)) {
        return NULL;
    }
    return ((Calendar*)cal)->getType();
}

static const UEnumeration defaultKeywordValues = {
    NULL,
    NULL,
    ulist_close_keyword_values_iterator,
    ulist_count_keyword_values,
    uenum_unextDefault,
    ulist_next_keyword_value, 
    ulist_reset_keyword_values_iterator
};

static const char * const CAL_TYPES[] = {
        "gregorian",
        "japanese",
        "buddhist",
        "roc",
        "persian",
        "islamic-civil",
        "islamic",
        "hebrew",
        "chinese",
        "indian",
        "coptic",
        "ethiopic",
        "ethiopic-amete-alem",
        NULL
};

#define CALPREF_LENGTH 39
#define CALPREF_MAX_NUM_KEYWORDS 4

static const char * const CALPREF[CALPREF_LENGTH][CALPREF_MAX_NUM_KEYWORDS+1] = {
        { "001",    "gregorian", NULL, NULL, NULL },
        { "AE",     "gregorian", "islamic", "islamic-civil", NULL },
        { "AF",     "gregorian", "islamic", "islamic-civil", "persian" },
        { "BH",     "gregorian", "islamic", "islamic-civil", NULL },
        { "CN",     "gregorian", "chinese", NULL, NULL },
        { "CX",     "gregorian", "chinese", NULL, NULL },
        { "DJ",     "gregorian", "islamic", "islamic-civil", NULL },
        { "DZ",     "gregorian", "islamic", "islamic-civil", NULL },
        { "EG",     "gregorian", "islamic", "islamic-civil", "coptic" },
        { "EH",     "gregorian", "islamic", "islamic-civil", NULL },
        { "ER",     "gregorian", "islamic", "islamic-civil", NULL },
        { "ET",     "gregorian", "ethiopic", "ethiopic-amete-alem", NULL },
        { "HK",     "gregorian", "chinese", NULL, NULL },
        { "IL",     "gregorian", "hebrew", NULL, NULL },
        { "IL",     "gregorian", "islamic", "islamic-civil", NULL },
        { "IN",     "gregorian", "indian", NULL, NULL },
        { "IQ",     "gregorian", "islamic", "islamic-civil", NULL },
        { "IR",     "gregorian", "islamic", "islamic-civil", "persian" },
        { "JO",     "gregorian", "islamic", "islamic-civil", NULL },
        { "JP",     "gregorian", "japanese", NULL, NULL },
        { "KM",     "gregorian", "islamic", "islamic-civil", NULL },
        { "KW",     "gregorian", "islamic", "islamic-civil", NULL },
        { "LB",     "gregorian", "islamic", "islamic-civil", NULL },
        { "LY",     "gregorian", "islamic", "islamic-civil", NULL },
        { "MA",     "gregorian", "islamic", "islamic-civil", NULL },
        { "MO",     "gregorian", "chinese", NULL, NULL },
        { "MR",     "gregorian", "islamic", "islamic-civil", NULL },
        { "OM",     "gregorian", "islamic", "islamic-civil", NULL },
        { "PS",     "gregorian", "islamic", "islamic-civil", NULL },
        { "QA",     "gregorian", "islamic", "islamic-civil", NULL },
        { "SA",     "gregorian", "islamic", "islamic-civil", NULL },
        { "SD",     "gregorian", "islamic", "islamic-civil", NULL },
        { "SG",     "gregorian", "chinese", NULL, NULL },
        { "SY",     "gregorian", "islamic", "islamic-civil", NULL },
        { "TD",     "gregorian", "islamic", "islamic-civil", NULL },
        { "TH",     "buddhist", "gregorian", NULL, NULL },
        { "TN",     "gregorian", "islamic", "islamic-civil", NULL },
        { "TW",     "gregorian", "roc", "chinese", NULL },
        { "YE",     "gregorian", "islamic", "islamic-civl", NULL }
};

#define MAX_LOC_SIZE_KEYWORD_VALUES 64
#define MAX_LENGTH_KEYWORD_VALUE 64

U_CAPI UEnumeration* U_EXPORT2
ucal_getKeywordValuesForLocale(const char *key, const char* locale, UBool commonlyUsed, UErrorCode *status) {
    // Resolve region
    char prefRegion[MAX_LOC_SIZE_KEYWORD_VALUES];
    int32_t prefRegionLength = 0;
    prefRegionLength = uloc_getCountry(locale, prefRegion, MAX_LOC_SIZE_KEYWORD_VALUES, status);
    if (prefRegionLength == 0) {
        char loc[MAX_LOC_SIZE_KEYWORD_VALUES];
        int32_t locLength = 0;
        locLength = uloc_addLikelySubtags(locale, loc, MAX_LOC_SIZE_KEYWORD_VALUES, status);
        
        prefRegionLength = uloc_getCountry(loc, prefRegion, MAX_LOC_SIZE_KEYWORD_VALUES, status);
    }
    
    // Read preferred calendar values from supplementalData calendarPreference
    UList *values = ulist_createEmptyList(status);
    UEnumeration *en = (UEnumeration *)uprv_malloc(sizeof(UEnumeration));
    if (U_FAILURE(*status) || en == NULL) {
        if (en == NULL) {
            *status = U_MEMORY_ALLOCATION_ERROR;
        } else {
            uprv_free(en);
        }
        ulist_deleteList(values);
        return NULL;
    }
    memcpy(en, &defaultKeywordValues, sizeof(UEnumeration));
    en->context = values;
    
    int32_t preferences = 0;
    for (int32_t i = 0; i < CALPREF_LENGTH; i++) {
        if (uprv_strcmp(prefRegion, CALPREF[i][0]) == 0) {
            preferences = i;
            break;
        }
    }
    for (int32_t i = 1; CALPREF[preferences][i] != NULL && i <= CALPREF_MAX_NUM_KEYWORDS; i++) {
        if (!ulist_containsString(values, CALPREF[preferences][i], uprv_strlen(CALPREF[preferences][i]))) {
            ulist_addItemEndList(values, CALPREF[preferences][i], FALSE, status);
            if (U_FAILURE(*status)) {
                break;
            }
        }
    }
    
    if (U_SUCCESS(*status) && !commonlyUsed) {
        // If not commonlyUsed, add other available values
        for (int32_t i = 0; CAL_TYPES[i] != NULL; i++) {
            if (!ulist_containsString(values, CAL_TYPES[i], uprv_strlen(CAL_TYPES[i]))) {
                ulist_addItemEndList(values, CAL_TYPES[i], FALSE, status);
                if (U_FAILURE(*status)) {
                    break;
                }
            }
        }
    }
    
    if (U_FAILURE(*status)) {
        uenum_close(en);
        return NULL;
    }
    
    ulist_resetList(values);
    
    return en;
}

#endif /* #if !UCONFIG_NO_FORMATTING */
