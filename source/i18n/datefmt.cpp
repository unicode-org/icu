/*
********************************************************************************
*                                                                              *
* COPYRIGHT:                                                                   *
*   (C) Copyright Taligent, Inc.,  1997                                        *
*   (C) Copyright International Business Machines Corporation,  1997-1998      *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.         *
*   US Government Users Restricted Rights - Use, duplication, or disclosure    *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                     *
*                                                                              *
********************************************************************************
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
*    07/20/98    stephen        Changed ParsePosition initialization
********************************************************************************
*/

#include "datefmt.h"
#include "smpdtfmt.h"
#include "mutex.h"
#include "resbund.h"

// *****************************************************************************
// class DateFormat
// *****************************************************************************

int32_t         DateFormat::fgLocalesCount;
const Locale*   DateFormat::fgLocales = 0;

DateFormat::DateFormat()
:   fCalendar(0),
    fNumberFormat(0)
{
}

//----------------------------------------------------------------------

DateFormat::DateFormat(const DateFormat& other)
:   fCalendar(0),
    fNumberFormat(0)
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
        fCalendar = other.fCalendar->clone();
        fNumberFormat = (NumberFormat*)other.fNumberFormat->clone();
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

bool_t
DateFormat::operator==(const Format& other) const
{
    // This protected comparison operator should only be called by subclasses
    // which have confirmed that the other object being compared against is
    // an instance of a sublcass of DateFormat.  THIS IS IMPORTANT.

    // We only dereference this pointer after we have confirmed below that
    // 'other' is a DateFormat subclass.
    DateFormat* fmt = (DateFormat*)&other;

    return (this == fmt) ||
        ((getDynamicClassID() == other.getDynamicClassID()) &&
         fCalendar->equivalentTo(*fmt->fCalendar) &&
         (*fNumberFormat == *fmt->fNumberFormat));
}

//----------------------------------------------------------------------

UnicodeString&
DateFormat::format(const Formattable& obj,
                   UnicodeString& toAppendTo,
                   FieldPosition& fieldPosition,
                   UErrorCode& status) const
{
    if (FAILURE(status)) return toAppendTo;

    // if the type of the Formattable is double or long, treat it as if it were a Date
    switch (obj.getType())
    {
    case Formattable::kDate:
        format(obj.getDate(), toAppendTo, fieldPosition);
        break;
    case Formattable::kDouble:
        format((UDate)obj.getDouble(), toAppendTo, fieldPosition);
        break;
    case Formattable::kLong:
        format((UDate)obj.getLong(), toAppendTo, fieldPosition);
        break;
    default:
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return toAppendTo;
    }

    // Is this right?
    //if (fieldPosition.getBeginIndex() == fieldPosition.getEndIndex())
    //  status = U_ILLEGAL_ARGUMENT_ERROR;

    return toAppendTo;
}

//----------------------------------------------------------------------

UnicodeString&
DateFormat::format(UDate date, UnicodeString& result) const
{
    // Note that any error information is just lost.  That's okay
    // for this convenience method.
    FieldPosition fpos(0);
    format(date, result, fpos);
    return result;
}

//----------------------------------------------------------------------

UDate
DateFormat::parse(const UnicodeString& text,
                  UErrorCode& status) const
{
    if (FAILURE(status)) return 0;

    ParsePosition pos(0);
    UDate result = parse(text, pos);
    if (pos.getIndex() == 0) status = U_ILLEGAL_ARGUMENT_ERROR;
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

DateFormat*
DateFormat::createTimeInstance(DateFormat::EStyle style,
                               const Locale& aLocale)
{
    return create(style, kNone, aLocale);
}

//----------------------------------------------------------------------

DateFormat*
DateFormat::createDateInstance(DateFormat::EStyle style,
                               const Locale& aLocale)
{
    // +4 to set the correct index for getting data out of
    // LocaleElements.
    return create(kNone, (EStyle) (style + kDateOffset), aLocale);
}

//----------------------------------------------------------------------

DateFormat*
DateFormat::createDateTimeInstance(EStyle dateStyle,
                                   EStyle timeStyle,
                                   const Locale& aLocale)
{
    return create(timeStyle, (EStyle) (dateStyle + kDateOffset), aLocale);
}

//----------------------------------------------------------------------

DateFormat*
DateFormat::createInstance()
{
    return create(kShort, (EStyle) (kShort + kDateOffset), Locale::getDefault());
}

//----------------------------------------------------------------------

DateFormat*
DateFormat::create(EStyle timeStyle, EStyle dateStyle, const Locale& locale)
{
    // Try to create a SimpleDateFormat of the desired style.
    UErrorCode status = U_ZERO_ERROR;
    SimpleDateFormat *f = new SimpleDateFormat(timeStyle, dateStyle, locale, status);
    if (SUCCESS(status)) return f;
    delete f;

    // If that fails, try to create a format using the default pattern and
    // the DateFormatSymbols for this locale.
    status = U_ZERO_ERROR;
    f = new SimpleDateFormat(locale, status);
    if (SUCCESS(status)) return f;
    delete f;

    // This should never really happen, because the preceding constructor
    // should always succeed.  If the resource data is unavailable, a last
    // resort object should be returned.
    return 0;
}

//----------------------------------------------------------------------

const Locale*
DateFormat::getAvailableLocales(int32_t& count)
{
    //  return LocaleData.getAvailableLocales("DateTimePatterns");


    // We only do this computation once.  Thereafter, we keep it in the static
    // fgLocalesArray.  What we do is iterate over all the locales, and try to
    // load a specific resource from them which indicates that they have
    // date format data.

    if (!fgLocales)
    {
        // This is going to load all the locale resource bundles into the
        // cache!
        int32_t localesCount;
        const Locale* locales = Locale::getAvailableLocales(localesCount);
        Locale* temp = new Locale[localesCount];
        int32_t newLocalesCount = 0;
        int32_t i;
        for (i=0; i<localesCount; ++i)
        {
            UErrorCode status = U_ZERO_ERROR;
            ResourceBundle resource(Locale::getDataDirectory(), locales[i], status);
            int32_t ignoredCount;
            resource.getStringArray(SimpleDateFormat::fgDateTimePatternsTag, ignoredCount, status);
            if (SUCCESS(status))
            {
                temp[newLocalesCount++] = locales[i];
            }
        }
        Locale *newLocales = new Locale[newLocalesCount];
        for (i=0; i<newLocalesCount; ++i) ((Locale*)newLocales)[i] = temp[i]; // Cast away const
        delete[] temp;


        Mutex lock;
        if(!fgLocales)
        {
            fgLocalesCount = newLocalesCount;
            fgLocales = newLocales;
        }
        else
        {
            delete [] newLocales; // *shrug*
        }
    }

    count = fgLocalesCount;
    return fgLocales;
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
    adoptCalendar(newCalendar.clone());
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
}
//----------------------------------------------------------------------

void
DateFormat::setNumberFormat(const NumberFormat& newNumberFormat)
{
    adoptNumberFormat((NumberFormat*)newNumberFormat.clone());
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
    fCalendar->adoptTimeZone(zone);
}
//----------------------------------------------------------------------

void
DateFormat::setTimeZone(const TimeZone& zone)
{
    fCalendar->setTimeZone(zone);
}

//----------------------------------------------------------------------

const TimeZone&
DateFormat::getTimeZone() const
{
    return fCalendar->getTimeZone();
}

//----------------------------------------------------------------------

void
DateFormat::setLenient(bool_t lenient)
{
    fCalendar->setLenient(lenient);
}

//----------------------------------------------------------------------

bool_t
DateFormat::isLenient() const
{
    return fCalendar->isLenient();
}

//eof
