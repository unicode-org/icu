/*
 *******************************************************************************
 * Copyright (C) 1997-2014, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * File DATEFMT.CPP
 *
 * Modification History:
 *
 *   Date        Name        Description
 *   02/19/97    aliu        Converted from java.
 *   03/31/97    aliu        Modified extensively to work with 50 locales.
 *   04/01/97    aliu        Added support for centuries.
 *   08/12/97    aliu        Fixed operator== to use Calendar::equivalentTo.
 *   07/20/98    stephen     Changed ParsePosition initialization
 ********************************************************************************
 */

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/ures.h"
#include "unicode/datefmt.h"
#include "unicode/smpdtfmt.h"
#include "unicode/dtptngen.h"
#include "unicode/udisplaycontext.h"
#include "reldtfmt.h"
#include "shareddatefmt.h"
#include "shareddatetimepatterngenerator.h"
#include "unifiedcache.h"

#include "cstring.h"
#include "windtfmt.h"

#if defined( U_DEBUG_CALSVC ) || defined (U_DEBUG_CAL)
#include <stdio.h>
#endif

// *****************************************************************************
// class DateFormat
// *****************************************************************************

U_NAMESPACE_BEGIN

SharedDateFormat::~SharedDateFormat() {
    delete ptr;
}

// We must fully define LocaleCacheKey<SharedDateFormat>
template<> U_I18N_API
const SharedDateFormat *LocaleCacheKey<SharedDateFormat>::createObject(
        const void * /*creationContext*/, UErrorCode &status) const {
    status = U_UNSUPPORTED_ERROR;
    return NULL;
}

class U_I18N_API DateFmtKeyByStyle : public LocaleCacheKey<SharedDateFormat> {
 private:
   DateFormat::EStyle fDateStyle;
   DateFormat::EStyle fTimeStyle;
 public:
   DateFmtKeyByStyle(
           const Locale &loc,
           DateFormat::EStyle dateStyle,
           DateFormat::EStyle timeStyle)
           : LocaleCacheKey<SharedDateFormat>(loc),
           fDateStyle(dateStyle),
           fTimeStyle(timeStyle) { }
   DateFmtKeyByStyle(const DateFmtKeyByStyle &other) :
           LocaleCacheKey<SharedDateFormat>(other),
           fDateStyle(other.fDateStyle),
           fTimeStyle(other.fTimeStyle) { }
   virtual ~DateFmtKeyByStyle();
   virtual int32_t hashCode() const {
       int32_t hash = 37 * LocaleCacheKey<SharedDateFormat>::hashCode() + fDateStyle;
       hash = 37 * hash + fTimeStyle;
       return hash;
   }
   virtual UBool operator==(const CacheKeyBase &other) const {
       // reflexive
       if (this == &other) {
           return TRUE;
       }
       if (!LocaleCacheKey<SharedDateFormat>::operator==(other)) {
           return FALSE;
       }
       // We know that this an other are of same class if we get this far.
       const DateFmtKeyByStyle *realOther =
               static_cast<const DateFmtKeyByStyle *>(&other);
       return (realOther->fDateStyle == fDateStyle &&
               realOther->fTimeStyle == fTimeStyle);
   }
   virtual CacheKeyBase *clone() const {
       return new DateFmtKeyByStyle(*this);
   }
   virtual const SharedDateFormat *createObject(
           const void * /*creationContext*/, UErrorCode &status) const {
       DateFormat::EStyle dateStyle = fDateStyle;
       if(dateStyle != DateFormat::kNone)
       {
           dateStyle = (DateFormat::EStyle) (dateStyle + DateFormat::kDateOffset);
       }
       DateFormat *fmt = DateFormat::create(fTimeStyle, dateStyle, fLoc);
       if (fmt == NULL) {
           status = U_MEMORY_ALLOCATION_ERROR;
           return NULL;
       }
       SharedDateFormat *result = new SharedDateFormat(fmt);
       if (result == NULL) {
           delete fmt;
           status = U_MEMORY_ALLOCATION_ERROR;
           return NULL;
       }
       result->addRef();
       return result;
   }
};

DateFmtKeyByStyle::~DateFmtKeyByStyle() {
}

class U_I18N_API DateFmtKeyBySkeleton : public LocaleCacheKey<SharedDateFormat> {
 private:
    UnicodeString fSkeleton;
 public:
   DateFmtKeyBySkeleton(const Locale &loc, const UnicodeString &skeleton) :
           LocaleCacheKey<SharedDateFormat>(loc),
           fSkeleton(skeleton) { }
   DateFmtKeyBySkeleton(const DateFmtKeyBySkeleton &other) :
           LocaleCacheKey<SharedDateFormat>(other),
           fSkeleton(other.fSkeleton) { }
   virtual ~DateFmtKeyBySkeleton();
   virtual int32_t hashCode() const {
       return 37 * LocaleCacheKey<SharedDateFormat>::hashCode() + fSkeleton.hashCode();
   }
   virtual UBool operator==(const CacheKeyBase &other) const {
       // reflexive
       if (this == &other) {
           return TRUE;
       }
       if (!LocaleCacheKey<SharedDateFormat>::operator==(other)) {
           return FALSE;
       }
       // We know that this an other are of same class if we get this far.
       const DateFmtKeyBySkeleton *realOther =
               static_cast<const DateFmtKeyBySkeleton *>(&other);
       return (realOther->fSkeleton == fSkeleton);
   }
   virtual CacheKeyBase *clone() const {
       return new DateFmtKeyBySkeleton(*this);
   }
   virtual const SharedDateFormat *createObject(
           const void *creationContext, UErrorCode &status) const {
       void *mutableCreationContext = const_cast<void *>(creationContext);
       DateTimePatternGenerator *ownedDtpg = NULL;
       DateTimePatternGenerator *dtpg =
               static_cast<DateTimePatternGenerator *>(mutableCreationContext);
       if (dtpg == NULL) {
           ownedDtpg = DateTimePatternGenerator::createInstance(fLoc, status);
           if (U_FAILURE(status)) {
               return NULL;
           }
           dtpg = ownedDtpg;
       } 
       DateFormat *fmt = new SimpleDateFormat(
               dtpg->getBestPattern(fSkeleton, status),
               fLoc,
               status);
       delete ownedDtpg;
       if (fmt == NULL) {
           status = U_MEMORY_ALLOCATION_ERROR;
           return NULL;
       }
       if (U_FAILURE(status)) {
           delete fmt;
           return NULL;
       }
       SharedDateFormat *result = new SharedDateFormat(fmt);
       if (result == NULL) {
           delete fmt;
           status = U_MEMORY_ALLOCATION_ERROR;
           return NULL;
       }
       result->addRef();
       return result;
   }
};

DateFmtKeyBySkeleton::~DateFmtKeyBySkeleton() {
}

static DateFormat *createFromCache(
        const CacheKey<SharedDateFormat> &key,
        const void *context,
        UErrorCode &status) {
    const UnifiedCache *cache = UnifiedCache::getInstance(status);
    if (U_FAILURE(status)) {
        return NULL;
    }
    const SharedDateFormat *ptr = NULL;
    cache->get(key, context, ptr, status);
    if (U_FAILURE(status)) {
        return NULL;
    }
    DateFormat *result = static_cast<DateFormat *>((*ptr)->clone());
    ptr->removeRef();
    if (result == NULL) {
        status = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

DateFormat::DateFormat()
:   fCalendar(0),
    fNumberFormat(0),
    fCapitalizationContext(UDISPCTX_CAPITALIZATION_NONE)
{
}

//----------------------------------------------------------------------

DateFormat::DateFormat(const DateFormat& other)
:   Format(other),
    fCalendar(0),
    fNumberFormat(0),
    fCapitalizationContext(UDISPCTX_CAPITALIZATION_NONE)
{
    *this = other;
}

//----------------------------------------------------------------------

DateFormat& DateFormat::operator=(const DateFormat& other)
{
    if (this != &other)
    {
        delete fCalendar;
        delete fNumberFormat;
        if(other.fCalendar) {
          fCalendar = other.fCalendar->clone();
        } else {
          fCalendar = NULL;
        }
        if(other.fNumberFormat) {
          fNumberFormat = (NumberFormat*)other.fNumberFormat->clone();
        } else {
          fNumberFormat = NULL;
        }
        fBoolFlags = other.fBoolFlags;
        fCapitalizationContext = other.fCapitalizationContext;
    }
    return *this;
}

//----------------------------------------------------------------------

DateFormat::~DateFormat()
{
    delete fCalendar;
    delete fNumberFormat;
}

//----------------------------------------------------------------------

UBool
DateFormat::operator==(const Format& other) const
{
    // This protected comparison operator should only be called by subclasses
    // which have confirmed that the other object being compared against is
    // an instance of a sublcass of DateFormat.  THIS IS IMPORTANT.

    // Format::operator== guarantees that this cast is safe
    DateFormat* fmt = (DateFormat*)&other;

    return (this == fmt) ||
        (Format::operator==(other) &&
         fCalendar&&(fCalendar->isEquivalentTo(*fmt->fCalendar)) &&
         (fNumberFormat && *fNumberFormat == *fmt->fNumberFormat) &&
         (fCapitalizationContext == fmt->fCapitalizationContext) );
}

//----------------------------------------------------------------------

UnicodeString&
DateFormat::format(const Formattable& obj,
                   UnicodeString& appendTo,
                   FieldPosition& fieldPosition,
                   UErrorCode& status) const
{
    if (U_FAILURE(status)) return appendTo;

    // if the type of the Formattable is double or long, treat it as if it were a Date
    UDate date = 0;
    switch (obj.getType())
    {
    case Formattable::kDate:
        date = obj.getDate();
        break;
    case Formattable::kDouble:
        date = (UDate)obj.getDouble();
        break;
    case Formattable::kLong:
        date = (UDate)obj.getLong();
        break;
    default:
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return appendTo;
    }

    // Is this right?
    //if (fieldPosition.getBeginIndex() == fieldPosition.getEndIndex())
    //  status = U_ILLEGAL_ARGUMENT_ERROR;

    return format(date, appendTo, fieldPosition);
}

//----------------------------------------------------------------------

UnicodeString&
DateFormat::format(const Formattable& obj,
                   UnicodeString& appendTo,
                   FieldPositionIterator* posIter,
                   UErrorCode& status) const
{
    if (U_FAILURE(status)) return appendTo;

    // if the type of the Formattable is double or long, treat it as if it were a Date
    UDate date = 0;
    switch (obj.getType())
    {
    case Formattable::kDate:
        date = obj.getDate();
        break;
    case Formattable::kDouble:
        date = (UDate)obj.getDouble();
        break;
    case Formattable::kLong:
        date = (UDate)obj.getLong();
        break;
    default:
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return appendTo;
    }

    // Is this right?
    //if (fieldPosition.getBeginIndex() == fieldPosition.getEndIndex())
    //  status = U_ILLEGAL_ARGUMENT_ERROR;

    return format(date, appendTo, posIter, status);
}

//----------------------------------------------------------------------

// Default implementation for backwards compatibility, subclasses should implement.
UnicodeString&
DateFormat::format(Calendar& /* unused cal */,
                   UnicodeString& appendTo,
                   FieldPositionIterator* /* unused posIter */,
                   UErrorCode& status) const {
    if (U_SUCCESS(status)) {
        status = U_UNSUPPORTED_ERROR;
    }
    return appendTo;
}

//----------------------------------------------------------------------

UnicodeString&
DateFormat::format(UDate date, UnicodeString& appendTo, FieldPosition& fieldPosition) const {
    if (fCalendar != NULL) {
        // Use a clone of our calendar instance
        Calendar* calClone = fCalendar->clone();
        if (calClone != NULL) {
            UErrorCode ec = U_ZERO_ERROR;
            calClone->setTime(date, ec);
            if (U_SUCCESS(ec)) {
                format(*calClone, appendTo, fieldPosition);
            }
            delete calClone;
        }
    }
    return appendTo;
}

//----------------------------------------------------------------------

UnicodeString&
DateFormat::format(UDate date, UnicodeString& appendTo, FieldPositionIterator* posIter,
                   UErrorCode& status) const {
    if (fCalendar != NULL) {
        Calendar* calClone = fCalendar->clone();
        if (calClone != NULL) {
            calClone->setTime(date, status);
            if (U_SUCCESS(status)) {
               format(*calClone, appendTo, posIter, status);
            }
            delete calClone;
        }
    }
    return appendTo;
}

//----------------------------------------------------------------------

UnicodeString&
DateFormat::format(UDate date, UnicodeString& appendTo) const
{
    // Note that any error information is just lost.  That's okay
    // for this convenience method.
    FieldPosition fpos(0);
    return format(date, appendTo, fpos);
}

//----------------------------------------------------------------------

UDate
DateFormat::parse(const UnicodeString& text,
                  ParsePosition& pos) const
{
    UDate d = 0; // Error return UDate is 0 (the epoch)
    if (fCalendar != NULL) {
        Calendar* calClone = fCalendar->clone();
        if (calClone != NULL) {
            int32_t start = pos.getIndex();
            calClone->clear();
            parse(text, *calClone, pos);
            if (pos.getIndex() != start) {
                UErrorCode ec = U_ZERO_ERROR;
                d = calClone->getTime(ec);
                if (U_FAILURE(ec)) {
                    // We arrive here if fCalendar => calClone is non-lenient and
                    // there is an out-of-range field.  We don't know which field
                    // was illegal so we set the error index to the start.
                    pos.setIndex(start);
                    pos.setErrorIndex(start);
                    d = 0;
                }
            }
            delete calClone;
        }
    }
    return d;
}

//----------------------------------------------------------------------

UDate
DateFormat::parse(const UnicodeString& text,
                  UErrorCode& status) const
{
    if (U_FAILURE(status)) return 0;

    ParsePosition pos(0);
    UDate result = parse(text, pos);
    if (pos.getIndex() == 0) {
#if defined (U_DEBUG_CAL)
      fprintf(stderr, "%s:%d - - failed to parse  - err index %d\n"
              , __FILE__, __LINE__, pos.getErrorIndex() );
#endif
      status = U_ILLEGAL_ARGUMENT_ERROR;
    }
    return result;
}

//----------------------------------------------------------------------

void
DateFormat::parseObject(const UnicodeString& source,
                        Formattable& result,
                        ParsePosition& pos) const
{
    result.setDate(parse(source, pos));
}

//----------------------------------------------------------------------

DateFormat* U_EXPORT2
DateFormat::createTimeInstance(DateFormat::EStyle style,
                               const Locale& aLocale)
{
    DateFmtKeyByStyle key(aLocale, kNone, style);
    UErrorCode status = U_ZERO_ERROR;
    return createFromCache(key, NULL, status);
}

//----------------------------------------------------------------------

DateFormat* U_EXPORT2
DateFormat::createDateInstance(DateFormat::EStyle style,
                               const Locale& aLocale)
{
    DateFmtKeyByStyle key(aLocale, style, kNone);
    UErrorCode status = U_ZERO_ERROR;
    return createFromCache(key, NULL, status);
}

//----------------------------------------------------------------------

DateFormat* U_EXPORT2
DateFormat::createDateTimeInstance(EStyle dateStyle,
                                   EStyle timeStyle,
                                   const Locale& aLocale)
{
    DateFmtKeyByStyle key(aLocale, dateStyle, timeStyle);
    UErrorCode status = U_ZERO_ERROR;
    return createFromCache(key, NULL, status);
}

//----------------------------------------------------------------------

DateFormat* U_EXPORT2
DateFormat::createInstance()
{
    DateFmtKeyByStyle key(Locale::getDefault(), kShort, kShort);
    UErrorCode status = U_ZERO_ERROR;
    return createFromCache(key, NULL, status);
}

//----------------------------------------------------------------------

DateFormat* U_EXPORT2
DateFormat::createInstanceForSkeleton(
        Calendar *calendarToAdopt,
        const UnicodeString& skeleton,
        const Locale &locale,
        UErrorCode &status) {
    LocalPointer<Calendar> calendar(calendarToAdopt);
    if (U_FAILURE(status)) {
        return NULL;
    }
    if (calendar.isNull()) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }
    DateFmtKeyBySkeleton key(locale, skeleton);
    DateFormat *result = createFromCache(key, NULL, status);
    if (U_FAILURE(status)) {
        return NULL;
    }
    result->adoptCalendar(calendar.orphan());
    return result;
}

DateFormat* U_EXPORT2
DateFormat::createInstanceForSkeleton(
        const UnicodeString& skeleton,
        const Locale &locale,
        UErrorCode &status) {
    if (U_FAILURE(status)) {
        return NULL;
    }
    DateFmtKeyBySkeleton key(locale, skeleton);
    return createFromCache(key, NULL, status);
}

DateFormat* U_EXPORT2
DateFormat::createInstanceForSkeleton(
        const UnicodeString& skeleton,
        UErrorCode &status) {
    if (U_FAILURE(status)) {
        return NULL;
    }
    DateFmtKeyBySkeleton key(Locale::getDefault(), skeleton);
    return createFromCache(key, NULL, status);
}

DateFormat* U_EXPORT2
DateFormat::internalCreateInstanceForSkeleton(
        const UnicodeString& skeleton,
        const Locale &locale,
        DateTimePatternGenerator &gen,
        UErrorCode &status) {
    if (U_FAILURE(status)) {
        return NULL;
    }
    DateFmtKeyBySkeleton key(locale, skeleton);
    return createFromCache(key, &gen, status);
}

//----------------------------------------------------------------------

DateFormat* U_EXPORT2
DateFormat::create(EStyle timeStyle, EStyle dateStyle, const Locale& locale)
{
    UErrorCode status = U_ZERO_ERROR;
#if U_PLATFORM_HAS_WIN32_API
    char buffer[8];
    int32_t count = locale.getKeywordValue("compat", buffer, sizeof(buffer), status);

    // if the locale has "@compat=host", create a host-specific DateFormat...
    if (count > 0 && uprv_strcmp(buffer, "host") == 0) {
        Win32DateFormat *f = new Win32DateFormat(timeStyle, dateStyle, locale, status);

        if (U_SUCCESS(status)) {
            return f;
        }

        delete f;
    }
#endif

    // is it relative?
    if(/*((timeStyle!=UDAT_NONE)&&(timeStyle & UDAT_RELATIVE)) || */((dateStyle!=kNone)&&((dateStyle-kDateOffset) & UDAT_RELATIVE))) {
        RelativeDateFormat *r = new RelativeDateFormat((UDateFormatStyle)timeStyle, (UDateFormatStyle)(dateStyle-kDateOffset), locale, status);
        if(U_SUCCESS(status)) return r;
        delete r;
        status = U_ZERO_ERROR;
    }

    // Try to create a SimpleDateFormat of the desired style.
    SimpleDateFormat *f = new SimpleDateFormat(timeStyle, dateStyle, locale, status);
    if (U_SUCCESS(status)) return f;
    delete f;

    // If that fails, try to create a format using the default pattern and
    // the DateFormatSymbols for this locale.
    status = U_ZERO_ERROR;
    f = new SimpleDateFormat(locale, status);
    if (U_SUCCESS(status)) return f;
    delete f;

    // This should never really happen, because the preceding constructor
    // should always succeed.  If the resource data is unavailable, a last
    // resort object should be returned.
    return 0;
}

//----------------------------------------------------------------------

const Locale* U_EXPORT2
DateFormat::getAvailableLocales(int32_t& count)
{
    // Get the list of installed locales.
    // Even if root has the correct date format for this locale,
    // it's still a valid locale (we don't worry about data fallbacks).
    return Locale::getAvailableLocales(count);
}

//----------------------------------------------------------------------

void
DateFormat::adoptCalendar(Calendar* newCalendar)
{
    delete fCalendar;
    fCalendar = newCalendar;
}

//----------------------------------------------------------------------
void
DateFormat::setCalendar(const Calendar& newCalendar)
{
    Calendar* newCalClone = newCalendar.clone();
    if (newCalClone != NULL) {
        adoptCalendar(newCalClone);
    }
}

//----------------------------------------------------------------------

const Calendar*
DateFormat::getCalendar() const
{
    return fCalendar;
}

//----------------------------------------------------------------------

void
DateFormat::adoptNumberFormat(NumberFormat* newNumberFormat)
{
    delete fNumberFormat;
    fNumberFormat = newNumberFormat;
    newNumberFormat->setParseIntegerOnly(TRUE);
}
//----------------------------------------------------------------------

void
DateFormat::setNumberFormat(const NumberFormat& newNumberFormat)
{
    NumberFormat* newNumFmtClone = (NumberFormat*)newNumberFormat.clone();
    if (newNumFmtClone != NULL) {
        adoptNumberFormat(newNumFmtClone);
    }
}

//----------------------------------------------------------------------

const NumberFormat*
DateFormat::getNumberFormat() const
{
    return fNumberFormat;
}

//----------------------------------------------------------------------

void
DateFormat::adoptTimeZone(TimeZone* zone)
{
    if (fCalendar != NULL) {
        fCalendar->adoptTimeZone(zone);
    }
}
//----------------------------------------------------------------------

void
DateFormat::setTimeZone(const TimeZone& zone)
{
    if (fCalendar != NULL) {
        fCalendar->setTimeZone(zone);
    }
}

//----------------------------------------------------------------------

const TimeZone&
DateFormat::getTimeZone() const
{
    if (fCalendar != NULL) {
        return fCalendar->getTimeZone();
    }
    // If calendar doesn't exists, create default timezone.
    // fCalendar is rarely null
    return *(TimeZone::createDefault());
}

//----------------------------------------------------------------------

void
DateFormat::setLenient(UBool lenient)
{
    if (fCalendar != NULL) {
        fCalendar->setLenient(lenient);
    }
    UErrorCode status = U_ZERO_ERROR;
    setBooleanAttribute(UDAT_PARSE_ALLOW_WHITESPACE, lenient, status);
    setBooleanAttribute(UDAT_PARSE_ALLOW_NUMERIC, lenient, status);
}

//----------------------------------------------------------------------

UBool
DateFormat::isLenient() const
{
    UBool lenient = TRUE;
    if (fCalendar != NULL) {
        lenient = fCalendar->isLenient();
    }
    UErrorCode status = U_ZERO_ERROR;
    return lenient
        && getBooleanAttribute(UDAT_PARSE_ALLOW_WHITESPACE, status)
        && getBooleanAttribute(UDAT_PARSE_ALLOW_NUMERIC, status);
}

void
DateFormat::setCalendarLenient(UBool lenient)
{
    if (fCalendar != NULL) {
        fCalendar->setLenient(lenient);
    }
}

//----------------------------------------------------------------------

UBool
DateFormat::isCalendarLenient() const
{
    if (fCalendar != NULL) {
        return fCalendar->isLenient();
    }
    // fCalendar is rarely null
    return FALSE;
}


//----------------------------------------------------------------------


void DateFormat::setContext(UDisplayContext value, UErrorCode& status)
{
    if (U_FAILURE(status))
        return;
    if ( (UDisplayContextType)((uint32_t)value >> 8) == UDISPCTX_TYPE_CAPITALIZATION ) {
        fCapitalizationContext = value;
    } else {
        status = U_ILLEGAL_ARGUMENT_ERROR;
   }
}


//----------------------------------------------------------------------


UDisplayContext DateFormat::getContext(UDisplayContextType type, UErrorCode& status) const
{
    if (U_FAILURE(status))
        return (UDisplayContext)0;
    if (type != UDISPCTX_TYPE_CAPITALIZATION) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return (UDisplayContext)0;
    }
    return fCapitalizationContext;
}


//----------------------------------------------------------------------


DateFormat& 
DateFormat::setBooleanAttribute(UDateFormatBooleanAttribute attr,
    									UBool newValue,
    									UErrorCode &status) {
    if(!fBoolFlags.isValidValue(newValue)) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
    } else {
        fBoolFlags.set(attr, newValue);
    }

    return *this;
}

//----------------------------------------------------------------------

UBool 
DateFormat::getBooleanAttribute(UDateFormatBooleanAttribute attr, UErrorCode &/*status*/) const {

    return fBoolFlags.get(attr);
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

//eof
