/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright Taligent, Inc.,  1996                                       *
*   (C) Copyright International Business Machines Corporation,  1998-1999     *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
*******************************************************************************
*/

#include "ucal.h"

#include "uloc.h"
#include "calendar.h"
#include "timezone.h"
#include "ustring.h"
#include "cpputils.h"

CAPI const UChar*
ucal_getAvailableTZIDs(        int32_t         rawOffset,
                int32_t         index,
                UErrorCode*     status)
{
  if(FAILURE(*status)) return 0;
  
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

CAPI int32_t
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

CAPI UDate 
ucal_getNow()
{
  return Calendar::getNow();
}

// ignore type until we add more subclasses
CAPI UCalendar* 
ucal_open(    const    UChar*          zoneID,
            int32_t        len,
        const    char*       locale,
            UCalendarType     type,
            UErrorCode*    status)
{
  if(FAILURE(*status)) return 0;
  
  TimeZone *zone = 0;
  if(zoneID == 0) {
    zone = TimeZone::createDefault();
  }
  else {
    int32_t length = (len == -1 ? u_strlen(zoneID) : len);

    zone = TimeZone::createTimeZone(UnicodeString((UChar*)zoneID,
						  length, length));
  }
  if(zone == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return 0;
  }
  
  return (UCalendar*)Calendar::createInstance(zone, Locale().init(locale), *status);
}

CAPI void
ucal_close(UCalendar *cal)
{
  delete (Calendar*) cal;
}

CAPI void 
ucal_setTimeZone(    UCalendar*      cal,
            const    UChar*            zoneID,
            int32_t        len,
            UErrorCode *status)
{
  if(FAILURE(*status)) return;

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

CAPI int32_t
ucal_getTimeZoneDisplayName(    const     UCalendar*                 cal,
                    UCalendarDisplayNameType     type,
                const      char                     *locale,
                    UChar*                  result,
                    int32_t                 resultLength,
                    UErrorCode*             status)
{
  if(FAILURE(*status)) return -1;

  int32_t actLen;

  const TimeZone& tz = ((Calendar*)cal)->getTimeZone();
  UnicodeString id(result, 0, resultLength);

  switch(type) {
  case UCAL_STANDARD:
    tz.getDisplayName(FALSE, TimeZone::LONG, Locale().init(locale), id);
    break;

  case UCAL_SHORT_STANDARD:
    tz.getDisplayName(FALSE, TimeZone::SHORT, Locale().init(locale), id);
    break;

  case UCAL_DST:
    tz.getDisplayName(TRUE, TimeZone::LONG, Locale().init(locale), id);
    break;

  case UCAL_SHORT_DST:
    tz.getDisplayName(TRUE, TimeZone::SHORT, Locale().init(locale), id);
    break;
  }

  T_fillOutputParams(&id, result, resultLength, &actLen, status);
  return actLen;
}

CAPI bool_t 
ucal_inDaylightTime(    const    UCalendar*      cal, 
            UErrorCode*     status )
{
  if(FAILURE(*status)) return (bool_t) -1;
  return ((Calendar*)cal)->inDaylightTime(*status);
}

CAPI int32_t
ucal_getAttribute(    const    UCalendar*              cal,
            UCalendarAttribute      attr)
{
  switch(attr) {
  case UCAL_LENIENT:
    return ((Calendar*)cal)->isLenient();
    break;
    
  case UCAL_FIRST_DAY_OF_WEEK:
    return ((Calendar*)cal)->getFirstDayOfWeek();
    break;
      
  case UCAL_MINIMAL_DAYS_IN_FIRST_WEEK:
    return ((Calendar*)cal)->getMinimalDaysInFirstWeek();
    break;

  default:
    return -1;
    break;
  }
}

CAPI void
ucal_setAttribute(      UCalendar*              cal,
            UCalendarAttribute      attr,
            int32_t                 newValue)
{
  switch(attr) {
  case UCAL_LENIENT:
    ((Calendar*)cal)->setLenient((bool_t)newValue);
    break;
    
  case UCAL_FIRST_DAY_OF_WEEK:
    ((Calendar*)cal)->setFirstDayOfWeek((Calendar::EDaysOfWeek)newValue);
    break;
      
  case UCAL_MINIMAL_DAYS_IN_FIRST_WEEK:
    ((Calendar*)cal)->setMinimalDaysInFirstWeek((uint8_t)newValue);
    break;
  }
}

CAPI const char*
ucal_getAvailable(int32_t index)
{
  return uloc_getAvailable(index);
}

CAPI int32_t
ucal_countAvailable()
{
  return uloc_countAvailable();
}

CAPI UDate 
ucal_getMillis(    const    UCalendar*      cal,
        UErrorCode*     status)
{
  if(FAILURE(*status)) return (UDate) 0;

  return ((Calendar*)cal)->getTime(*status);
}

CAPI void 
ucal_setMillis(        UCalendar*      cal,
            UDate           dateTime,
            UErrorCode*     status )
{
  if(FAILURE(*status)) return;

  ((Calendar*)cal)->setTime(dateTime, *status);
}

// TBD: why does this take an UErrorCode?
CAPI void 
ucal_setDate(        UCalendar*        cal,
            int32_t            year,
            int32_t            month,
            int32_t            date,
            UErrorCode        *status)
{
  if(FAILURE(*status)) return;

  ((Calendar*)cal)->set(year, month, date);
}

// TBD: why does this take an UErrorCode?
CAPI void 
ucal_setDateTime(    UCalendar*        cal,
            int32_t            year,
            int32_t            month,
            int32_t            date,
            int32_t            hour,
            int32_t            minute,
            int32_t            second,
            UErrorCode        *status)
{
  if(FAILURE(*status)) return;

  ((Calendar*)cal)->set(year, month, date, hour, minute, second);
}

CAPI bool_t 
ucal_equivalentTo(    const UCalendar*      cal1,
            const UCalendar*      cal2)
{
  return ((Calendar*)cal1)->equivalentTo(*((Calendar*)cal2));
}

CAPI void 
ucal_add(    UCalendar*                cal,
        UCalendarDateFields        field,
        int32_t                    amount,
        UErrorCode*                status)
{
  if(FAILURE(*status)) return;

  ((Calendar*)cal)->add((Calendar::EDateFields)field, amount, *status);
}

CAPI void 
ucal_roll(        UCalendar*            cal,
            UCalendarDateFields field,
            int32_t                amount,
            UErrorCode*            status)
{
  if(FAILURE(*status)) return;

  ((Calendar*)cal)->roll((Calendar::EDateFields)field, amount, *status);
}

CAPI int32_t 
ucal_get(    const    UCalendar*                cal,
        UCalendarDateFields        field,
        UErrorCode*                status )
{
  if(FAILURE(*status)) return -1;

  return ((Calendar*)cal)->get((Calendar::EDateFields)field, *status);
}

CAPI void 
ucal_set(    UCalendar*                cal,
        UCalendarDateFields        field,
        int32_t                    value)
{
  ((Calendar*)cal)->set((Calendar::EDateFields)field, value);
}

CAPI bool_t 
ucal_isSet(    const    UCalendar*                cal,
        UCalendarDateFields        field)
{
  return ((Calendar*)cal)->isSet((Calendar::EDateFields)field);
}

CAPI void 
ucal_clearField(    UCalendar*            cal,
            UCalendarDateFields field)
{
  ((Calendar*)cal)->clear((Calendar::EDateFields)field);
}

CAPI void 
ucal_clear(UCalendar* calendar)
{
  ((Calendar*)calendar)->clear();
}

CAPI int32_t 
ucal_getLimit(    const    UCalendar*              cal,
            UCalendarDateFields     field,
            UCalendarLimitType      type,
            UErrorCode        *status)
{
  if(FAILURE(*status)) return -1;
  
  switch(type) {
  case UCAL_MINIMUM:
    return ((Calendar*)cal)->getMinimum((Calendar::EDateFields)field);
    break;

  case UCAL_MAXIMUM:
    return ((Calendar*)cal)->getMaximum((Calendar::EDateFields)field);
    break;

  case UCAL_GREATEST_MINIMUM:
    return ((Calendar*)cal)->getGreatestMinimum((Calendar::EDateFields)field);
    break;

  case UCAL_LEAST_MAXIMUM:
    return ((Calendar*)cal)->getLeastMaximum((Calendar::EDateFields)field);
    break;

  case UCAL_ACTUAL_MINIMUM:
    return ((Calendar*)cal)->getActualMinimum((Calendar::EDateFields)field,
                          *status);
    break;

  case UCAL_ACTUAL_MAXIMUM:
    return ((Calendar*)cal)->getActualMaximum((Calendar::EDateFields)field,
                          *status);
    break;

  default:
    return -1;
    break;
  }
}
