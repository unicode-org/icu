/*
*******************************************************************************
*   Copyright (C) 1996-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*/

#include "unicode/ucal.h"

#include "unicode/uloc.h"
#include "unicode/calendar.h"
#include "unicode/timezone.h"
#include "unicode/ustring.h"
#include "cpputils.h"

U_CAPI const UChar*
ucal_getAvailableTZIDs(        int32_t         rawOffset,
                int32_t         index,
                UErrorCode*     status)
{
  if(U_FAILURE(*status)) return 0;
  
  int32_t count = 0;
  const UChar *retVal = 0;
  
  const UnicodeString** const tzs = TimeZone::createAvailableIDs(rawOffset, 
                                 count);

  if(tzs == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return 0;
  }

  if(index < count)
    retVal = tzs[index]->getUChars();
  else
    *status = U_INDEX_OUTOFBOUNDS_ERROR;
  
  delete [] tzs;
  return retVal;
}

U_CAPI int32_t
ucal_countAvailableTZIDs(int32_t rawOffset)
{  
  int32_t count = 0;
  
  const UnicodeString** const tzs  = TimeZone::createAvailableIDs(rawOffset, 
                                  count);

  if(tzs == 0) {
    // TBD: U_MEMORY_ALLOCATION_ERROR
    return 0;
  }

  delete [] tzs;
  return count;
}

U_CAPI UDate 
ucal_getNow()
{
  return Calendar::getNow();
}

// ignore type until we add more subclasses
U_CAPI UCalendar* 
ucal_open(    const    UChar*          zoneID,
            int32_t        len,
        const    char*       locale,
            UCalendarType     /*type*/,
            UErrorCode*    status)
{
  if(U_FAILURE(*status)) return 0;
  
  TimeZone *zone = 0;
  if(zoneID == 0) {
    zone = TimeZone::createDefault();
  }
  else {
    int32_t length = (len == -1 ? u_strlen(zoneID) : len);

    zone = TimeZone::createTimeZone(UnicodeString(zoneID, length));
  }
  if(zone == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return 0;
  }
  
  return (UCalendar*)Calendar::createInstance(zone, Locale(locale), *status);
}

U_CAPI void
ucal_close(UCalendar *cal)
{
  delete (Calendar*) cal;
}

U_CAPI void 
ucal_setTimeZone(    UCalendar*      cal,
            const    UChar*            zoneID,
            int32_t        len,
            UErrorCode *status)
{
  if(U_FAILURE(*status)) return;

  TimeZone *zone = 0;
  if(zone == 0) {
    zone = TimeZone::createDefault();
  }
  else {
    int32_t length = (len == -1 ? u_strlen(zoneID) : len);
    zone = TimeZone::createTimeZone(UnicodeString((UChar*)zoneID, 
						  length, length));
  }
  if(zone == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return;
  }

  ((Calendar*)cal)->adoptTimeZone(zone);
}

U_CAPI int32_t
ucal_getTimeZoneDisplayName(    const     UCalendar*                 cal,
                    UCalendarDisplayNameType     type,
                const      char                     *locale,
                    UChar*                  result,
                    int32_t                 resultLength,
                    UErrorCode*             status)
{
  if(U_FAILURE(*status)) return -1;

  int32_t actLen;

  const TimeZone& tz = ((Calendar*)cal)->getTimeZone();
  UnicodeString id(result, 0, resultLength);

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

  T_fillOutputParams(&id, result, resultLength, &actLen, status);
  return actLen;
}

U_CAPI UBool 
ucal_inDaylightTime(    const    UCalendar*      cal, 
            UErrorCode*     status )
{
  if(U_FAILURE(*status)) return (UBool) -1;
  return ((Calendar*)cal)->inDaylightTime(*status);
}

U_CAPI int32_t
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

U_CAPI void
ucal_setAttribute(      UCalendar*              cal,
            UCalendarAttribute      attr,
            int32_t                 newValue)
{
  switch(attr) {
  case UCAL_LENIENT:
    ((Calendar*)cal)->setLenient((UBool)newValue);
    break;
    
  case UCAL_FIRST_DAY_OF_WEEK:
    ((Calendar*)cal)->setFirstDayOfWeek((Calendar::EDaysOfWeek)newValue);
    break;
      
  case UCAL_MINIMAL_DAYS_IN_FIRST_WEEK:
    ((Calendar*)cal)->setMinimalDaysInFirstWeek((uint8_t)newValue);
    break;
  }
}

U_CAPI const char*
ucal_getAvailable(int32_t index)
{
  return uloc_getAvailable(index);
}

U_CAPI int32_t
ucal_countAvailable()
{
  return uloc_countAvailable();
}

U_CAPI UDate 
ucal_getMillis(    const    UCalendar*      cal,
        UErrorCode*     status)
{
  if(U_FAILURE(*status)) return (UDate) 0;

  return ((Calendar*)cal)->getTime(*status);
}

U_CAPI void 
ucal_setMillis(        UCalendar*      cal,
            UDate           dateTime,
            UErrorCode*     status )
{
  if(U_FAILURE(*status)) return;

  ((Calendar*)cal)->setTime(dateTime, *status);
}

// TBD: why does this take an UErrorCode?
U_CAPI void 
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
U_CAPI void 
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

U_CAPI UBool 
ucal_equivalentTo(    const UCalendar*      cal1,
            const UCalendar*      cal2)
{
  return ((Calendar*)cal1)->equivalentTo(*((Calendar*)cal2));
}

U_CAPI void 
ucal_add(    UCalendar*                cal,
        UCalendarDateFields        field,
        int32_t                    amount,
        UErrorCode*                status)
{
  if(U_FAILURE(*status)) return;

  ((Calendar*)cal)->add((Calendar::EDateFields)field, amount, *status);
}

U_CAPI void 
ucal_roll(        UCalendar*            cal,
            UCalendarDateFields field,
            int32_t                amount,
            UErrorCode*            status)
{
  if(U_FAILURE(*status)) return;

  ((Calendar*)cal)->roll((Calendar::EDateFields)field, amount, *status);
}

U_CAPI int32_t 
ucal_get(    const    UCalendar*                cal,
        UCalendarDateFields        field,
        UErrorCode*                status )
{
  if(U_FAILURE(*status)) return -1;

  return ((Calendar*)cal)->get((Calendar::EDateFields)field, *status);
}

U_CAPI void 
ucal_set(    UCalendar*                cal,
        UCalendarDateFields        field,
        int32_t                    value)
{
  ((Calendar*)cal)->set((Calendar::EDateFields)field, value);
}

U_CAPI UBool 
ucal_isSet(    const    UCalendar*                cal,
        UCalendarDateFields        field)
{
  return ((Calendar*)cal)->isSet((Calendar::EDateFields)field);
}

U_CAPI void 
ucal_clearField(    UCalendar*            cal,
            UCalendarDateFields field)
{
  ((Calendar*)cal)->clear((Calendar::EDateFields)field);
}

U_CAPI void 
ucal_clear(UCalendar* calendar)
{
  ((Calendar*)calendar)->clear();
}

U_CAPI int32_t 
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
    return ((Calendar*)cal)->getMinimum((Calendar::EDateFields)field);

  case UCAL_MAXIMUM:
    return ((Calendar*)cal)->getMaximum((Calendar::EDateFields)field);

  case UCAL_GREATEST_MINIMUM:
    return ((Calendar*)cal)->getGreatestMinimum((Calendar::EDateFields)field);

  case UCAL_LEAST_MAXIMUM:
    return ((Calendar*)cal)->getLeastMaximum((Calendar::EDateFields)field);

  case UCAL_ACTUAL_MINIMUM:
    return ((Calendar*)cal)->getActualMinimum((Calendar::EDateFields)field,
                          *status);

  case UCAL_ACTUAL_MAXIMUM:
    return ((Calendar*)cal)->getActualMaximum((Calendar::EDateFields)field,
                          *status);

  default:
    break;
  }
  return -1;
}
