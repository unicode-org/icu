/*
*******************************************************************************
*   Copyright (C) 1996-2004, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*/

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/udat.h"

#include "unicode/uloc.h"
#include "unicode/datefmt.h"
#include "unicode/timezone.h"
#include "unicode/smpdtfmt.h"
#include "unicode/fieldpos.h"
#include "unicode/parsepos.h"
#include "unicode/calendar.h"
#include "unicode/numfmt.h"
#include "unicode/dtfmtsym.h"
#include "unicode/ustring.h"
#include "cpputils.h"

U_NAMESPACE_USE

U_CAPI UDateFormat* U_EXPORT2
udat_open(UDateFormatStyle  timeStyle,
          UDateFormatStyle  dateStyle,
          const char        *locale,
          const UChar       *tzID,
          int32_t           tzIDLength,
          const UChar       *pattern,
          int32_t           patternLength,
          UErrorCode        *status)
{

  if(U_FAILURE(*status)) 
  {
      return 0;
  }
  if(timeStyle != UDAT_IGNORE)
  {
      DateFormat *fmt;
      if(locale == 0)
        fmt = DateFormat::createDateTimeInstance((DateFormat::EStyle)dateStyle,
                             (DateFormat::EStyle)timeStyle);
      else
        fmt = DateFormat::createDateTimeInstance((DateFormat::EStyle)dateStyle,
                             (DateFormat::EStyle)timeStyle,
                                                 Locale(locale));
  
      if(fmt == 0) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        return 0;
      }

      if(tzID != 0) {
        TimeZone *zone = TimeZone::createTimeZone(UnicodeString((UBool)(tzIDLength == -1), tzID, tzIDLength));
        if(zone == 0) {
          *status = U_MEMORY_ALLOCATION_ERROR;
          delete fmt;
          return 0;
        }
        fmt->adoptTimeZone(zone);
      }
  
      return (UDateFormat*)fmt;
  }
  else
  {
      const UnicodeString pat = UnicodeString((UBool)(patternLength == -1), pattern, patternLength);
      UDateFormat *retVal = 0;

      if(locale == 0)
        retVal = (UDateFormat*)new SimpleDateFormat(pat, *status);
      else
        retVal = (UDateFormat*)new SimpleDateFormat(pat, Locale(locale), *status);

      if(retVal == 0) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        return 0;
      }
      return retVal;
  }
}


U_CAPI void U_EXPORT2
udat_close(UDateFormat* format)
{

  delete (DateFormat*)format;
}

U_CAPI UDateFormat* U_EXPORT2
udat_clone(const UDateFormat *fmt,
       UErrorCode *status)
{

  if(U_FAILURE(*status)) return 0;

  Format *res = ((SimpleDateFormat*)fmt)->clone();
  
  if(res == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return 0;
  }

  return (UDateFormat*) res;
}

U_CAPI int32_t U_EXPORT2
udat_format(    const    UDateFormat*    format,
        UDate           dateToFormat,
        UChar*          result,
        int32_t         resultLength,
        UFieldPosition* position,
        UErrorCode*     status)
{

  if(U_FAILURE(*status)) return -1;

  UnicodeString res;
  if(!(result==NULL && resultLength==0)) {
    // NULL destination for pure preflighting: empty dummy string
    // otherwise, alias the destination buffer
    res.setTo(result, 0, resultLength);
  }

  FieldPosition fp;
  
  if(position != 0)
    fp.setField(position->field);
  
  ((DateFormat*)format)->format(dateToFormat, res, fp);
  
  if(position != 0) {
    position->beginIndex = fp.getBeginIndex();
    position->endIndex = fp.getEndIndex();
  }
  
  return res.extract(result, resultLength, *status);
}

U_CAPI UDate U_EXPORT2
udat_parse(    const    UDateFormat*        format,
        const    UChar*          text,
        int32_t         textLength,
        int32_t         *parsePos,
        UErrorCode      *status)
{

  if(U_FAILURE(*status)) return (UDate)0;

  const UnicodeString src((UBool)(textLength == -1), text, textLength);
  ParsePosition pp;
  UDate res;

  if(parsePos != 0)
    pp.setIndex(*parsePos);

  res = ((DateFormat*)format)->parse(src, pp);

  if(parsePos != 0) {
    if(pp.getErrorIndex() == -1)
      *parsePos = pp.getIndex();
    else {
      *parsePos = pp.getErrorIndex();
      *status = U_PARSE_ERROR;
    }
  }
  
  return res;
}

U_CAPI void U_EXPORT2
udat_parseCalendar(const    UDateFormat*    format,
                            UCalendar*      calendar,
                   const    UChar*          text,
                            int32_t         textLength,
                            int32_t         *parsePos,
                            UErrorCode      *status)
{

  if(U_FAILURE(*status)) return;

  const UnicodeString src((UBool)(textLength == -1), text, textLength);
  ParsePosition pp;

  if(parsePos != 0)
    pp.setIndex(*parsePos);

  ((DateFormat*)format)->parse(src, *(Calendar*)calendar, pp);

  if(parsePos != 0) {
    if(pp.getErrorIndex() == -1)
      *parsePos = pp.getIndex();
    else {
      *parsePos = pp.getErrorIndex();
      *status = U_PARSE_ERROR;
    }
  }
  
  return;
}

U_CAPI UBool U_EXPORT2
udat_isLenient(const UDateFormat* fmt)
{

  return ((DateFormat*)fmt)->isLenient();
}

U_CAPI void U_EXPORT2
udat_setLenient(    UDateFormat*    fmt,
            UBool          isLenient)
{

  ((DateFormat*)fmt)->setLenient(isLenient);
}

U_CAPI const UCalendar* U_EXPORT2
udat_getCalendar(const UDateFormat* fmt)
{

  return (const UCalendar*) ((DateFormat*)fmt)->getCalendar();
}

U_CAPI void U_EXPORT2
udat_setCalendar(            UDateFormat*    fmt,
                    const   UCalendar*      calendarToSet)
{

  ((DateFormat*)fmt)->setCalendar(*((Calendar*)calendarToSet));
}

U_CAPI const UNumberFormat* U_EXPORT2
udat_getNumberFormat(const UDateFormat* fmt)
{

  return (const UNumberFormat*) ((DateFormat*)fmt)->getNumberFormat();
}

U_CAPI void U_EXPORT2
udat_setNumberFormat(            UDateFormat*    fmt,
                    const   UNumberFormat*  numberFormatToSet)
{

  ((DateFormat*)fmt)->setNumberFormat(*((NumberFormat*)numberFormatToSet));
}

U_CAPI const char* U_EXPORT2
udat_getAvailable(int32_t index)
{
  return uloc_getAvailable(index);
}

U_CAPI int32_t U_EXPORT2
udat_countAvailable()
{

  return uloc_countAvailable();
}

U_CAPI UDate U_EXPORT2
udat_get2DigitYearStart(    const   UDateFormat     *fmt,
                UErrorCode      *status)
{

  if(U_FAILURE(*status)) return (UDate)0;
  return ((SimpleDateFormat*)fmt)->get2DigitYearStart(*status);
}

U_CAPI void U_EXPORT2
udat_set2DigitYearStart(    UDateFormat     *fmt,
                UDate           d,
                UErrorCode      *status)
{

  if(U_FAILURE(*status)) return;
  ((SimpleDateFormat*)fmt)->set2DigitYearStart(d, *status);
}

U_CAPI int32_t U_EXPORT2
udat_toPattern(    const   UDateFormat     *fmt,
        UBool          localized,
        UChar           *result,
        int32_t         resultLength,
        UErrorCode      *status)
{

  if(U_FAILURE(*status)) return -1;

  UnicodeString res;
  if(!(result==NULL && resultLength==0)) {
    // NULL destination for pure preflighting: empty dummy string
    // otherwise, alias the destination buffer
    res.setTo(result, 0, resultLength);
  }

  if(localized)
    ((SimpleDateFormat*)fmt)->toLocalizedPattern(res, *status);
  else
    ((SimpleDateFormat*)fmt)->toPattern(res);

  return res.extract(result, resultLength, *status);
}

// TBD: should this take an UErrorCode?
U_CAPI void U_EXPORT2
udat_applyPattern(            UDateFormat     *format,
                    UBool          localized,
                    const   UChar           *pattern,
                    int32_t         patternLength)
{

  const UnicodeString pat((UBool)(patternLength == -1), pattern, patternLength);
  UErrorCode status = U_ZERO_ERROR;

  if(localized)
    ((SimpleDateFormat*)format)->applyLocalizedPattern(pat, status);
  else
    ((SimpleDateFormat*)format)->applyPattern(pat);
}

U_CAPI int32_t U_EXPORT2
udat_getSymbols(const   UDateFormat             *fmt,
        UDateFormatSymbolType   type,
        int32_t                 index,
        UChar                   *result,
        int32_t                 resultLength,
        UErrorCode              *status)
{

  if(U_FAILURE(*status)) return -1;

  const DateFormatSymbols *syms = 
    ((SimpleDateFormat*)fmt)->getDateFormatSymbols();
  int32_t count;
  const UnicodeString *res;

  switch(type) {
  case UDAT_ERAS:
    res = syms->getEras(count);
    if(index < count) {
      return res[index].extract(result, resultLength, *status);
    }
    break;

  case UDAT_MONTHS:
    res = syms->getMonths(count);
    if(index < count) {
      return res[index].extract(result, resultLength, *status);
    }
    break;

  case UDAT_SHORT_MONTHS:
    res = syms->getShortMonths(count);
    if(index < count) {
      return res[index].extract(result, resultLength, *status);
    }
    break;

  case UDAT_WEEKDAYS:
    res = syms->getWeekdays(count);
    if(index < count) {
      return res[index].extract(result, resultLength, *status);
    }
    break;

  case UDAT_SHORT_WEEKDAYS:
    res = syms->getShortWeekdays(count);
    if(index < count) {
      return res[index].extract(result, resultLength, *status);
    }
    break;

  case UDAT_AM_PMS:
    res = syms->getAmPmStrings(count);
    if(index < count) {
      return res[index].extract(result, resultLength, *status);
    }
    break;

  case UDAT_LOCALIZED_CHARS:
    {
      UnicodeString res1;
      if(!(result==NULL && resultLength==0)) {
        // NULL destination for pure preflighting: empty dummy string
        // otherwise, alias the destination buffer
        res1.setTo(result, 0, resultLength);
      }
      syms->getLocalPatternChars(res1);
      return res1.extract(result, resultLength, *status);
    }
  }
  
  return 0;
}

U_CAPI int32_t U_EXPORT2
udat_countSymbols(    const    UDateFormat                *fmt,
            UDateFormatSymbolType    type)
{

  const DateFormatSymbols *syms = 
    ((SimpleDateFormat*)fmt)->getDateFormatSymbols();
  int32_t count = 0;

  switch(type) {
  case UDAT_ERAS:
    syms->getEras(count);
    break;

  case UDAT_MONTHS:
    syms->getMonths(count);
    break;

  case UDAT_SHORT_MONTHS:
    syms->getShortMonths(count);
    break;

  case UDAT_WEEKDAYS:
    syms->getWeekdays(count);
    break;

  case UDAT_SHORT_WEEKDAYS:
    syms->getShortWeekdays(count);
    break;

  case UDAT_AM_PMS:
    syms->getAmPmStrings(count);
    break;

  case UDAT_LOCALIZED_CHARS:
    count = 1;
    break;
  }
  
  return count;
}

U_NAMESPACE_BEGIN

/*
 * This DateFormatSymbolsSingleSetter class is a friend of DateFormatSymbols
 * solely for the purpose of avoiding to clone the array of strings
 * just to modify one of them and then setting all of them back.
 * For example, the old code looked like this:
 *  case UDAT_MONTHS:
 *    res = syms->getMonths(count);
 *    array = new UnicodeString[count];
 *    if(array == 0) {
 *      *status = U_MEMORY_ALLOCATION_ERROR;
 *      return;
 *    }
 *    uprv_arrayCopy(res, array, count);
 *    if(index < count)
 *      array[index] = val;
 *    syms->setMonths(array, count);
 *    break;
 *
 * Even worse, the old code actually cloned the entire DateFormatSymbols object,
 * cloned one value array, changed one value, and then made the SimpleDateFormat
 * replace its DateFormatSymbols object with the new one.
 *
 * markus 2002-oct-14
 */
class DateFormatSymbolsSingleSetter /* not : public UObject because all methods are static */ {
public:
  static void
  setSymbol(UnicodeString *array, int32_t count, int32_t index,
            const UChar *value, int32_t valueLength, UErrorCode &errorCode) {
    if(array!=NULL) {
      if(index>=count) {
        errorCode=U_INDEX_OUTOFBOUNDS_ERROR;
      } else if(value==NULL) {
        errorCode=U_ILLEGAL_ARGUMENT_ERROR;
      } else {
        array[index].setTo(value, valueLength);
      }
    }
  }

  static void
  setEra(DateFormatSymbols *syms, int32_t index,
         const UChar *value, int32_t valueLength, UErrorCode &errorCode) {
    setSymbol(syms->fEras, syms->fErasCount, index, value, valueLength, errorCode);
  }

  static void
  setMonth(DateFormatSymbols *syms, int32_t index,
           const UChar *value, int32_t valueLength, UErrorCode &errorCode) {
    setSymbol(syms->fMonths, syms->fMonthsCount, index, value, valueLength, errorCode);
  }

  static void
  setShortMonth(DateFormatSymbols *syms, int32_t index,
                const UChar *value, int32_t valueLength, UErrorCode &errorCode) {
    setSymbol(syms->fShortMonths, syms->fShortMonthsCount, index, value, valueLength, errorCode);
  }

  static void
  setWeekday(DateFormatSymbols *syms, int32_t index,
             const UChar *value, int32_t valueLength, UErrorCode &errorCode) {
    setSymbol(syms->fWeekdays, syms->fWeekdaysCount, index, value, valueLength, errorCode);
  }

  static void
  setShortWeekday(DateFormatSymbols *syms, int32_t index,
                  const UChar *value, int32_t valueLength, UErrorCode &errorCode) {
    setSymbol(syms->fShortWeekdays, syms->fShortWeekdaysCount, index, value, valueLength, errorCode);
  }

  static void
  setAmPm(DateFormatSymbols *syms, int32_t index,
          const UChar *value, int32_t valueLength, UErrorCode &errorCode) {
    setSymbol(syms->fAmPms, syms->fAmPmsCount, index, value, valueLength, errorCode);
  }

  static void
  setLocalPatternChars(DateFormatSymbols *syms,
                       const UChar *value, int32_t valueLength, UErrorCode &errorCode) {
    setSymbol(&syms->fLocalPatternChars, 1, 0, value, valueLength, errorCode);
  }
};

U_NAMESPACE_END

U_CAPI void U_EXPORT2
udat_setSymbols(    UDateFormat             *format,
            UDateFormatSymbolType   type,
            int32_t                 index,
            UChar                   *value,
            int32_t                 valueLength,
            UErrorCode              *status)
{

  if(U_FAILURE(*status)) return;

  DateFormatSymbols *syms = (DateFormatSymbols *)((SimpleDateFormat *)format)->getDateFormatSymbols();

  switch(type) {
  case UDAT_ERAS:
    DateFormatSymbolsSingleSetter::setEra(syms, index, value, valueLength, *status);
    break;

  case UDAT_MONTHS:
    DateFormatSymbolsSingleSetter::setMonth(syms, index, value, valueLength, *status);
    break;

  case UDAT_SHORT_MONTHS:
    DateFormatSymbolsSingleSetter::setShortMonth(syms, index, value, valueLength, *status);
    break;

  case UDAT_WEEKDAYS:
    DateFormatSymbolsSingleSetter::setWeekday(syms, index, value, valueLength, *status);
    break;

  case UDAT_SHORT_WEEKDAYS:
    DateFormatSymbolsSingleSetter::setShortWeekday(syms, index, value, valueLength, *status);
    break;

  case UDAT_AM_PMS:
    DateFormatSymbolsSingleSetter::setAmPm(syms, index, value, valueLength, *status);
    break;

  case UDAT_LOCALIZED_CHARS:
    DateFormatSymbolsSingleSetter::setLocalPatternChars(syms, value, valueLength, *status);
    break;
  }
}

U_CAPI const char* U_EXPORT2
udat_getLocaleByType(const UDateFormat *fmt,
                     ULocDataLocaleType type,
                     UErrorCode* status)
{
    if (fmt == NULL) {
        if (U_SUCCESS(*status)) {
            *status = U_ILLEGAL_ARGUMENT_ERROR;
        }
        return NULL;
    }
    return ((Format*)fmt)->getLocaleID(type, *status);
}
#endif /* #if !UCONFIG_NO_FORMATTING */
