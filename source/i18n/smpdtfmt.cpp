/*
*******************************************************************************
* Copyright (C) 1997-1999, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* File SMPDTFMT.CPP
*
* Modification History:
*
*   Date        Name        Description
*   02/19/97    aliu        Converted from java.
*   03/31/97    aliu        Modified extensively to work with 50 locales.
*   04/01/97    aliu        Added support for centuries.
*   07/09/97    helena      Made ParsePosition into a class.
*   07/21/98    stephen     Added initializeDefaultCentury.
*                             Removed getZoneIndex (added in DateFormatSymbols)
*                             Removed subParseLong
*                             Removed chk
*  02/22/99     stephen     Removed character literals for EBCDIC safety
*   10/14/99    aliu        Updated 2-digit year parsing so that only "00" thru
*                           "99" are recognized. {j28 4182066}
*   11/15/99    weiv        Added support for week of year/day of week format
********************************************************************************
*/

#include "unicode/smpdtfmt.h"
#include "unicode/dtfmtsym.h"
#include "unicode/resbund.h"
#include "unicode/msgfmt.h"
#include "unicode/calendar.h"
#include "unicode/gregocal.h"
#include "unicode/timezone.h"
#include "unicode/decimfmt.h"
#include "unicode/dcfmtsym.h"
#include "mutex.h"
#include <float.h>

// *****************************************************************************
// class SimpleDateFormat
// *****************************************************************************

// For time zones that have no names, use strings GMT+minutes and
// GMT-minutes. For instance, in France the time zone is GMT+60.
// Also accepted are GMT+H:MM or GMT-H:MM.
const UnicodeString     SimpleDateFormat::fgGmt("GMT");
const UnicodeString     SimpleDateFormat::fgGmtPlus("GMT+");
const UnicodeString     SimpleDateFormat::fgGmtMinus("GMT-");

// This is a pattern-of-last-resort used when we can't load a usable pattern out
// of a resource.
const UnicodeString     SimpleDateFormat::fgDefaultPattern("yyMMdd hh:mm a");

/**
 * These are the tags we expect to see in normal resource bundle files associated
 * with a locale.
 */
const char *SimpleDateFormat::fgErasTag="Eras";
const char *SimpleDateFormat::fgMonthNamesTag="MonthNames";
const char *SimpleDateFormat::fgMonthAbbreviationsTag="MonthAbbreviations";
const char *SimpleDateFormat::fgDayNamesTag="DayNames";
const char *SimpleDateFormat::fgDayAbbreviationsTag="DayAbbreviations";
const char *SimpleDateFormat::fgAmPmMarkersTag="AmPmMarkers";
const char *SimpleDateFormat::fgDateTimePatternsTag="DateTimePatterns";

/**
 * These are the tags we expect to see in time zone data resource bundle files
 * associated with a locale.
 */
const char *SimpleDateFormat::fgZoneStringsTag="zoneStrings";
const char *SimpleDateFormat::fgLocalPatternCharsTag="localPatternChars";

char                    SimpleDateFormat::fgClassID = 0; // Value is irrelevant

/**
 * This value of defaultCenturyStart indicates that the system default is to be
 * used.
 */
const UDate              SimpleDateFormat::fgSystemDefaultCentury        = DBL_MIN;
const int32_t            SimpleDateFormat::fgSystemDefaultCenturyYear    = -1;

UDate                    SimpleDateFormat::fgSystemDefaultCenturyStart        = SimpleDateFormat::fgSystemDefaultCentury;
int32_t                 SimpleDateFormat::fgSystemDefaultCenturyStartYear    = SimpleDateFormat::fgSystemDefaultCenturyYear;

//----------------------------------------------------------------------

SimpleDateFormat::~SimpleDateFormat()
{
    delete fSymbols;
}

//----------------------------------------------------------------------

SimpleDateFormat::SimpleDateFormat(UErrorCode& status)
:   fSymbols(NULL),
    fDefaultCenturyStart(fgSystemDefaultCentury),
    fDefaultCenturyStartYear(fgSystemDefaultCenturyYear)
{
    construct(kShort, (EStyle) (kShort + kDateOffset), Locale::getDefault(), status);
}

//----------------------------------------------------------------------

SimpleDateFormat::SimpleDateFormat(const UnicodeString& pattern,
                                   UErrorCode &status)
:   fPattern(pattern),
    fSymbols(new DateFormatSymbols(status)),
    fDefaultCenturyStart(fgSystemDefaultCentury),
    fDefaultCenturyStartYear(fgSystemDefaultCenturyYear)
{
    initialize(Locale::getDefault(), status);
}

//----------------------------------------------------------------------

SimpleDateFormat::SimpleDateFormat(const UnicodeString& pattern,
                                   const Locale& locale,
                                   UErrorCode& status)
:   fPattern(pattern),
    fSymbols(new DateFormatSymbols(locale, status)),
    fDefaultCenturyStart(fgSystemDefaultCentury),
    fDefaultCenturyStartYear(fgSystemDefaultCenturyYear)
{
    initialize(locale, status);
}

//----------------------------------------------------------------------

SimpleDateFormat::SimpleDateFormat(const UnicodeString& pattern,
                                   DateFormatSymbols* symbolsToAdopt,
                                   UErrorCode& status)
:   fPattern(pattern),
    fSymbols(symbolsToAdopt),
    fDefaultCenturyStart(fgSystemDefaultCentury),
    fDefaultCenturyStartYear(fgSystemDefaultCenturyYear)
{
    initialize(Locale::getDefault(), status);
}

//----------------------------------------------------------------------

SimpleDateFormat::SimpleDateFormat(const UnicodeString& pattern,
                                   const DateFormatSymbols& symbols,
                                   UErrorCode& status)
:   fPattern(pattern),
    fSymbols(new DateFormatSymbols(symbols)),
    fDefaultCenturyStart(fgSystemDefaultCentury),
    fDefaultCenturyStartYear(fgSystemDefaultCenturyYear)
{
    initialize(Locale::getDefault(), status);
}

//----------------------------------------------------------------------

// Not for public consumption; used by DateFormat
SimpleDateFormat::SimpleDateFormat(EStyle timeStyle,
                                   EStyle dateStyle,
                                   const Locale& locale,
                                   UErrorCode& status)
:   fSymbols(NULL),
    fDefaultCenturyStart(fgSystemDefaultCentury),
    fDefaultCenturyStartYear(fgSystemDefaultCenturyYear)
{
    construct(timeStyle, dateStyle, locale, status);
}

//----------------------------------------------------------------------

/**
 * Not for public consumption; used by DateFormat.  This constructor
 * never fails.  If the resource data is not available, it uses the
 * the last resort symbols.
 */
SimpleDateFormat::SimpleDateFormat(const Locale& locale,
                                   UErrorCode& status)
:   fPattern(fgDefaultPattern),
    fSymbols(NULL),
    fDefaultCenturyStart(fgSystemDefaultCentury),
    fDefaultCenturyStartYear(fgSystemDefaultCenturyYear)
{
    if (U_FAILURE(status)) return;
    fSymbols = new DateFormatSymbols(locale, status);
    if (U_FAILURE(status))
    {
        status = U_ZERO_ERROR;
        delete fSymbols;
        // This constructor doesn't fail; it uses last resort data
        fSymbols = new DateFormatSymbols(status);
    }
    initialize(locale, status);
}

//----------------------------------------------------------------------

SimpleDateFormat::SimpleDateFormat(const SimpleDateFormat& other)
:   DateFormat(other),
    fSymbols(NULL),
    fDefaultCenturyStart(fgSystemDefaultCentury),
    fDefaultCenturyStartYear(fgSystemDefaultCenturyYear)
{
    *this = other;
}

//----------------------------------------------------------------------

SimpleDateFormat& SimpleDateFormat::operator=(const SimpleDateFormat& other)
{
    DateFormat::operator=(other);

    delete fSymbols;
    fSymbols = NULL;

    if (other.fSymbols)
        fSymbols = new DateFormatSymbols(*other.fSymbols);

    fDefaultCenturyStart         = other.fDefaultCenturyStart;
    fDefaultCenturyStartYear     = other.fDefaultCenturyStartYear;

    fPattern = other.fPattern;

    return *this;
}

//----------------------------------------------------------------------

Format*
SimpleDateFormat::clone() const
{
    return new SimpleDateFormat(*this);
}

//----------------------------------------------------------------------

bool_t
SimpleDateFormat::operator==(const Format& other) const
{
    if (DateFormat::operator==(other) &&
        other.getDynamicClassID() == getStaticClassID())
    {
        SimpleDateFormat* that = (SimpleDateFormat*)&other;
        return     (fPattern             == that->fPattern &&
                fSymbols             != NULL && // Check for pathological object
                that->fSymbols         != NULL && // Check for pathological object
                *fSymbols             == *that->fSymbols &&
                fDefaultCenturyStart == that->fDefaultCenturyStart);
    }
    return FALSE;
}

//----------------------------------------------------------------------

void SimpleDateFormat::construct(EStyle timeStyle,
                                 EStyle dateStyle,
                                 const Locale& locale,
                                 UErrorCode& status)
{
    // called by several constructors to load pattern data from the resources

    if (U_FAILURE(status)) return;

    // load up the DateTimePatters resource from the appropriate locale (throw
    // an error if for some weird reason the resource is malformed)
    ResourceBundle resources(u_getDataDirectory(), locale, status);
    int32_t dtCount;
    const UnicodeString *dateTimePatterns = resources.getStringArray(fgDateTimePatternsTag, dtCount, status);
    if (U_FAILURE(status)) return;
    if (dtCount <= kDateTime)
    {
        status = U_INVALID_FORMAT_ERROR;
        return;
    }

    // create a symbols object from the locale
    fSymbols = new DateFormatSymbols(locale, status);

    UnicodeString str;

    // Move dateStyle from the range [0, 3] to [4, 7] if necessary
    //if (dateStyle >= 0 && dateStyle < DATE_OFFSET) dateStyle = (EStyle)(dateStyle + DATE_OFFSET);

    // if the pattern should include both date and time information, use the date/time
    // pattern string as a guide to tell use how to glue together the appropriate date
    // and time pattern strings.  The actual gluing-together is handled by a convenience
    // method on MessageFormat.
    if ((timeStyle != kNone) &&
        (dateStyle != kNone))
    {
        //  Object[] dateTimeArgs = {
        //     dateTimePatterns[timeStyle], dateTimePatterns[dateStyle]
        //  };
        //  pattern = MessageFormat.format(dateTimePatterns[8], dateTimeArgs);

        Formattable *timeDateArray = new Formattable[2];
        timeDateArray[0].setString(dateTimePatterns[timeStyle]);
        timeDateArray[1].setString(dateTimePatterns[dateStyle]);

        MessageFormat::format(dateTimePatterns[kDateTime], timeDateArray, 2, fPattern, status);
        delete [] timeDateArray;
    }
    
    // if the pattern includes just time data or just date date, load the appropriate
    // pattern string from the resources
    else if (timeStyle != kNone) fPattern = dateTimePatterns[timeStyle];
    else if (dateStyle != kNone) fPattern = dateTimePatterns[dateStyle];
    
    // and if it includes _neither_, that's an error
    else status = U_INVALID_FORMAT_ERROR;

    // finally, finish initializing by creating a Calendar and a NumberFormat
    initialize(locale, status);
}

//----------------------------------------------------------------------

void
SimpleDateFormat::initialize(const Locale& locale,
                             UErrorCode& status)
{
    if (U_FAILURE(status)) return;

    // {sfb} should this be here?
    if (fSymbols->fZoneStringsColCount < 1)
    {
        status = U_INVALID_FORMAT_ERROR; // Check for bogus locale data
        return;
    }

    // We don't need to check that the row count is >= 1, since all 2d arrays have at
    // least one row
    fCalendar = Calendar::createInstance(TimeZone::createDefault(), locale, status);
    fNumberFormat = NumberFormat::createInstance(locale, status);
    if (fNumberFormat != NULL && U_SUCCESS(status))
    {
        // no matter what the locale's default number format looked like, we want
        // to modify it so that it doesn't use thousands separators, doesn't always
        // show the decimal point, and recognizes integers only when parsing

        fNumberFormat->setGroupingUsed(FALSE);
        if (fNumberFormat->getDynamicClassID() == DecimalFormat::getStaticClassID())
            ((DecimalFormat*)fNumberFormat)->setDecimalSeparatorAlwaysShown(FALSE);
        fNumberFormat->setParseIntegerOnly(TRUE);
        fNumberFormat->setMinimumFractionDigits(0); // To prevent "Jan 1.00, 1997.00"

        initializeDefaultCentury();
    }
    else if (U_SUCCESS(status))
    {
        status = U_MISSING_RESOURCE_ERROR;
    }
}

/* Initialize the fields we use to disambiguate ambiguous years. Separate
 * so we can call it from readObject().
 */
void SimpleDateFormat::initializeDefaultCentury() 
{
    fDefaultCenturyStart        = internalGetDefaultCenturyStart();
    fDefaultCenturyStartYear    = internalGetDefaultCenturyStartYear();

    UErrorCode status = U_ZERO_ERROR;
    fCalendar->setTime(fDefaultCenturyStart, status);
    // {sfb} throw away error
}

/* Define one-century window into which to disambiguate dates using
 * two-digit years. Make public in JDK 1.2.
 */
void SimpleDateFormat::parseAmbiguousDatesAsAfter(UDate startDate, UErrorCode& status) 
{
    if(U_FAILURE(status))
        return;
        
    fCalendar->setTime(startDate, status);
    if(U_SUCCESS(status)) {
        fDefaultCenturyStart = startDate;
        fDefaultCenturyStartYear = fCalendar->get(Calendar::YEAR, status);
    }
}
    
//----------------------------------------------------------------------

UnicodeString&
SimpleDateFormat::format(UDate date, UnicodeString& toAppendTo, FieldPosition& pos) const
{
    if (fCalendar == 0) {
        return toAppendTo;
    }

    UErrorCode status = U_ZERO_ERROR;
    pos.setBeginIndex(0);
    pos.setEndIndex(0);

    // load up our Calendar with the date/time we're formatting (the subroutines of this
    // function pick it up from there, since they need it anyway to split the value
    // into fields)
    fCalendar->setTime(date, status);

    bool_t inQuote = FALSE;
    UChar prevCh = 0;
    int32_t count = 0;
    UnicodeString str;
    
    // loop through the pattern string character by character
    for (int32_t i = 0; i < fPattern.length() && U_SUCCESS(status); ++i) {
        UChar ch = fPattern[i];
        
        // Use subFormat() to format a repeated pattern character
        // when a different pattern or non-pattern character is seen
        if (ch != prevCh && count > 0) {
            toAppendTo += subFormat(str, prevCh, count, toAppendTo.length(), pos, status);
            count = 0;
        }
        if (ch == 0x0027 /*'\''*/) {
            // Consecutive single quotes are a single quote literal,
            // either outside of quotes or between quotes
            if ((i+1) < fPattern.length() && fPattern[i+1] == 0x0027 /*'\''*/) {
                toAppendTo += (UChar)0x0027 /*'\''*/;
                ++i;
            } else {
                inQuote = ! inQuote;
            }
        } 
        else if ( ! inQuote && ((ch >= 0x0061 /*'a'*/ && ch <= 0x007A /*'z'*/) 
                    || (ch >= 0x0041 /*'A'*/ && ch <= 0x005A /*'Z'*/))) {
            // ch is a date-time pattern character to be interpreted
            // by subFormat(); count the number of times it is repeated
            prevCh = ch;
            ++count;
        }
        else {
            // Append quoted characters and unquoted non-pattern characters
            toAppendTo += ch;
        }
    }

    // Format the last item in the pattern, if any
    if (count > 0) {
        toAppendTo += subFormat(str, prevCh, count, toAppendTo.length(), pos, status);
    }

    // and if something failed (e.g., an invalid format character), reset our FieldPosition
    // to (0, 0) to show that
    // {sfb} look at this later- are these being set correctly?
    if (U_FAILURE(status)) {
        pos.setBeginIndex(0);
        pos.setEndIndex(0);
    }
    
    return toAppendTo;
}

UnicodeString&
SimpleDateFormat::format(const Formattable& obj, 
                         UnicodeString& toAppendTo, 
                         FieldPosition& pos,
                         UErrorCode& status) const
{
    // this is just here to get around the hiding problem
    // (the previous format() override would hide the version of
    // format() on DateFormat that this function correspond to, so we
    // have to redefine it here)
    return DateFormat::format(obj, toAppendTo, pos, status);
}

//----------------------------------------------------------------------

// Map index into pattern character string to Calendar field number.
const Calendar::EDateFields
SimpleDateFormat::fgPatternIndexToCalendarField[] =
{
    Calendar::ERA, Calendar::YEAR, Calendar::MONTH, Calendar::DATE, 
    Calendar::HOUR_OF_DAY, Calendar::HOUR_OF_DAY, Calendar::MINUTE, 
    Calendar::SECOND, Calendar::MILLISECOND, Calendar::DAY_OF_WEEK,
    Calendar::DAY_OF_YEAR, Calendar::DAY_OF_WEEK_IN_MONTH, 
    Calendar::WEEK_OF_YEAR, Calendar::WEEK_OF_MONTH, 
    Calendar::AM_PM, Calendar::HOUR, Calendar::HOUR, Calendar::ZONE_OFFSET,
    Calendar::YEAR_WOY, Calendar::DOW_LOCAL
};

// Map index into pattern character string to DateFormat field number
const DateFormat::EField
SimpleDateFormat::fgPatternIndexToDateFormatField[] = {
    DateFormat::kEraField, DateFormat::kYearField, DateFormat::kMonthField,
    DateFormat::kDateField, DateFormat::kHourOfDay1Field,
    DateFormat::kHourOfDay0Field, DateFormat::kMinuteField,
    DateFormat::kSecondField, DateFormat::kMillisecondField,
    DateFormat::kDayOfWeekField, DateFormat::kDayOfYearField,
    DateFormat::kDayOfWeekInMonthField, DateFormat::kWeekOfYearField,
    DateFormat::kWeekOfMonthField, DateFormat::kAmPmField,
    DateFormat::kHour1Field, DateFormat::kHour0Field,
    DateFormat::kTimezoneField, DateFormat::kYearWOYField, 
    DateFormat::kDOWLocalField
};


//----------------------------------------------------------------------

UnicodeString&
SimpleDateFormat::subFormat(UnicodeString& result,
                            UChar ch,
                            int32_t count,
                            int32_t beginOffset,
                            FieldPosition& pos,
                            UErrorCode& status) const
{
    // this function gets called by format() to produce the appropriate substitution
    // text for an individual pattern symbol (e.g., "HH" or "yyyy")

    EField patternCharIndex = (EField) -1;
    int32_t maxIntCount = 10;
    UnicodeString str; // Scratch
    result.remove();

    // if the pattern character is unrecognized, signal an error and dump out
    if ((patternCharIndex = (EField)DateFormatSymbols::fgPatternChars.indexOf(ch)) == (EField)-1)
    {
        status = U_INVALID_FORMAT_ERROR;
        return result;
    }

    Calendar::EDateFields field = fgPatternIndexToCalendarField[patternCharIndex];
    int32_t value = fCalendar->get(field, status);
    if (U_FAILURE(status)) return result;

    switch (patternCharIndex) {
    
    // for any "G" symbol, write out the appropriate era string
    case kEraField:
        result = fSymbols->fEras[value];
        break;

    // for "yyyy", write out the whole year; for "yy", write out the last 2 digits
    case kYearField:
    case kYearWOYField:
        if (count >= 4) 
            zeroPaddingNumber(result, value, 4, maxIntCount);
        else 
            zeroPaddingNumber(result, value, 2, 2);
        break;

    // for "MMMM", write out the whole month name, for "MMM", write out the month
    // abbreviation, for "M" or "MM", write out the month as a number with the
    // appropriate number of digits
    case kMonthField:
        if (count >= 4) 
            result = fSymbols->fMonths[value];
        else if (count == 3) 
            result = fSymbols->fShortMonths[value];
        else 
            zeroPaddingNumber(result, value + 1, count, maxIntCount);
        break;

    // for "k" and "kk", write out the hour, adjusting midnight to appear as "24"
    case kHourOfDay1Field:
        if (value == 0) 
            zeroPaddingNumber(result, fCalendar->getMaximum(Calendar::HOUR_OF_DAY) + 1, count, maxIntCount);
        else 
            zeroPaddingNumber(result, value, count, maxIntCount);
        break;

    // for "SS" and "S", we want to truncate digits so that you still see the MOST
    // significant digits rather than the LEAST (as is the case with the year)
    case kMillisecondField:
        if (count > 3) 
            count = 3;
        else if (count == 2) 
            value = value / 10;
        else if (count == 1) 
            value = value / 100;
        zeroPaddingNumber(result, value, count, maxIntCount);
        break;

    // for "EEEE", write out the day-of-the-week name; otherwise, use the abbreviation
    case kDayOfWeekField:
        if (count >= 4) 
            result = fSymbols->fWeekdays[value];
        else 
            result = fSymbols->fShortWeekdays[value];
        break;

    // for and "a" symbol, write out the whole AM/PM string
    case kAmPmField:
        result = fSymbols->fAmPms[value];
        break;

    // for "h" and "hh", write out the hour, adjusting noon and midnight to show up
    // as "12"
    case kHour1Field:
        if (value == 0) 
            zeroPaddingNumber(result, fCalendar->getLeastMaximum(Calendar::HOUR) + 1, count, maxIntCount);
        else 
            zeroPaddingNumber(result, value, count, maxIntCount);
        break;

    // for the "z" symbols, we have to check our time zone data first.  If we have a
    // localized name for the time zone, then "zzzz" is the whole name and anything
    // shorter is the abbreviation (we also have to check for daylight savings time
    // since the name will be different).  If we don't have a localized time zone name,
    // then the time zone shows up as "GMT+hh:mm" or "GMT-hh:mm" (where "hh:mm" is the
    // offset from GMT) regardless of how many z's were in the pattern symbol
    case kTimezoneField: {
        int32_t zoneIndex = fSymbols->getZoneIndex(fCalendar->getTimeZone().getID(str));
        if (zoneIndex == -1) {
            UnicodeString zoneString;

            value = fCalendar->get(Calendar::ZONE_OFFSET, status) +
                    fCalendar->get(Calendar::DST_OFFSET, status);

            if (value < 0) {
                zoneString += fgGmtMinus;
                value = -value; // suppress the '-' sign for text display.
            }
            else
                zoneString += fgGmtPlus;
            
            zoneString += zeroPaddingNumber(str, (int32_t)(value/U_MILLIS_PER_HOUR), 2, 2);
            zoneString += (UChar)0x003A /*':'*/;
            zoneString += zeroPaddingNumber(str, (int32_t)((value%U_MILLIS_PER_HOUR)/U_MILLIS_PER_MINUTE), 2, 2);
            
            result = zoneString;
        }
        else if (fCalendar->get(Calendar::DST_OFFSET, status) != 0) {
            if (count >= 4) 
                result = fSymbols->fZoneStrings[zoneIndex][3];
            else 
                result = fSymbols->fZoneStrings[zoneIndex][4];
        }
        else {
            if (count >= 4) 
                result = fSymbols->fZoneStrings[zoneIndex][1];
            else 
                result = fSymbols->fZoneStrings[zoneIndex][2];
        }
        }
        break;
    
    // all of the other pattern symbols can be formatted as simple numbers with
    // appropriate zero padding
    default:
    // case kDateField:
    // case kHourOfDay0Field:
    // case kMinuteField:
    // case kSecondField:
    // case kDayOfYearField:
    // case kDayOfWeekInMonthField:
    // case kWeekOfYearField:
    // case kWeekOfMonthField:
    // case kHour0Field:
    // case kDOWLocalField:
        zeroPaddingNumber(result, value, count, maxIntCount);
        break;
    }

    // if the field we're formatting is the one the FieldPosition says it's interested
    // in, fill in the FieldPosition with this field's positions
    if (pos.getField() == fgPatternIndexToDateFormatField[patternCharIndex]) {
        if (pos.getBeginIndex() == 0 && pos.getEndIndex() == 0) {
            pos.setBeginIndex(beginOffset);
            pos.setEndIndex(beginOffset + result.length());
        }
    }
    
    return result;
}

//----------------------------------------------------------------------

UnicodeString&
SimpleDateFormat::zeroPaddingNumber(UnicodeString& result, int32_t value, int32_t minDigits, int32_t maxDigits) const
{
    result.remove();
    fNumberFormat->setMinimumIntegerDigits(minDigits);
    fNumberFormat->setMaximumIntegerDigits(maxDigits);
    return fNumberFormat->format(value, result);
}

//----------------------------------------------------------------------

// {sfb} removed
/*
// this function will dump output to the console on a debug build when there's a parse error
#ifdef _DEBUG
void chk(ParsePosition& val, UChar ch, ParsePosition& start, int32_t count)
{
    if (val.getIndex() < 0)
    {
        cout << "[Parse failure on '" << (char)ch << "' x " << dec << count << " @ " << start.getIndex() << ']';
    }
}
#else
inline void chk(ParsePosition& val, UChar ch, ParsePosition& start, int32_t count)
{
}
#endif

inline Date
parseFailureResult(ParsePosition& pos, ParsePosition& oldStart, ParsePosition& failurePos)
{
    // Note: The C++ version currently supports the notion of returning zero
    // with a non-zero parse position, but only if this format is lenient.
    // The returned position in this case is the first un-parseable character.
    // This is useful, but is not present in the Java version, and causes a
    // DateFormat test to fail.
    
    // For now, I am removing this function.  It can be restored later.

    // if (!isLenient()) pos = oldStart;
    // else { pos = failurePos.getIndex(); if (pos.getIndex() < 0) pos = -pos.getIndex(); };
    pos = oldStart;
    return 0;
}
*/

UDate
SimpleDateFormat::parse(const UnicodeString& text, ParsePosition& pos) const
{
    int32_t start = pos.getIndex();
    int32_t oldStart = start;
    bool_t ambiguousYear[] = { FALSE };

    char s[100];
            s[text.extract(0, text.length(), s)]=0;

    fCalendar->clear();

    bool_t inQuote = FALSE;
    UChar prevCh = 0;
    int32_t count = 0;
    int32_t interQuoteCount = 1; // Number of chars between quotes

    // loop through the pattern string character by character, using it to control how
    // we match characters in the input
    for (int32_t i = 0; i < fPattern.length();++i) {
        UChar ch = fPattern[i];
        
        // if we're inside a quoted string, match characters exactly until we hit
        // another single quote (two single quotes in a row match one single quote
        // in the input)
        if (inQuote)
        {
            if (ch == 0x0027 /*'\''*/)
            {
                // ends with 2nd single quote
                inQuote = FALSE;
                // two consecutive quotes outside a quote means we have
                // a quote literal we need to match.
                if (count == 0)
                {
                    if(start > text.length() || ch != text[start])
                        {
                            pos.setIndex(oldStart);
                            pos.setErrorIndex(start);
                            // {sfb} what is the correct Date for failure?
                            return 0;
                        }
                        ++start;
                }
                count = 0;
                interQuoteCount = 0;
            }
            else
                {
                    // pattern uses text following from 1st single quote.
                    if (start >= text.length() || ch != text[start]) {
                        // Check for cases like: 'at' in pattern vs "xt"
                        // in time text, where 'a' doesn't match with 'x'.
                        // If fail to match, return null.
                        pos.setIndex(oldStart); // left unchanged
                        pos.setErrorIndex(start);
                        // {sfb} what is correct Date for failure?
                        return 0;
                    }
                    ++count;
                    ++start;
                }
        }

        // if we're not inside a quoted string...
        else {
            
            // ...a quote mark puts us into a quoted string (and we parse any pending
            // pattern symbols)
            if (ch == 0x0027 /*'\''*/) {
                inQuote = TRUE;
                if (count > 0) 
                {
                    int32_t startOffset = start;
                    start = subParse(text, start, prevCh, count, FALSE, ambiguousYear);
                    if ( start < 0 ) {
                        pos.setErrorIndex(startOffset);
                        pos.setIndex(oldStart);
                        // {sfb} correct Date
                        return 0;
                    }
                    count = 0;
                }

                    if (interQuoteCount == 0)
                    {
                        // This indicates two consecutive quotes inside a quote,
                        // for example, 'o''clock'.  We need to parse this as
                        // representing a single quote within the quote.
                        int32_t startOffset = start;
                        if (start >= text.length() ||  ch != text[start])
                        {
                            pos.setErrorIndex(startOffset);
                            pos.setIndex(oldStart);
                            // {sfb} correct Date
                            return 0;
                        }
                        ++start;
                        count = 1; // Make it look like we never left
                    }
            }
            
            // if we're on a letter, collect copies of the same letter to determine
            // the whole parse symbol.  when we hit a different character, parse the
            // input based on the resulting symbol
        else if ((ch >= 0x0061 /*'a'*/ && ch <= 0x007A /*'z'*/) 
             || (ch >= 0x0041 /*'A'*/ && ch <= 0x005A /*'Z'*/))
          {
                // ch is a date-time pattern
                if (ch != prevCh && count > 0) // e.g., yyyyMMdd
                {
                    int32_t startOffset = start;
                    // This is the only case where we pass in 'true' for
                    // obeyCount.  That's because the next field directly
                    // abuts this one, so we have to use the count to know when
                    // to stop parsing. [LIU]
                    start = subParse(text, start, prevCh, count, TRUE, ambiguousYear);
                    if (start < 0) {
                        pos.setErrorIndex(startOffset);
                        pos.setIndex(oldStart);
                        // {sfb} correct Date
                        return 0;
                    }
                    prevCh = ch;
                    count = 1;
                }
                else {
                    if (ch != prevCh) 
                        prevCh = ch;
                    count++;
                }
            }

            // if we're on a non-letter, parse based on any pending pattern symbols
            else if (count > 0) 
            {
                // handle cases like: MM-dd-yy, HH:mm:ss, or yyyy MM dd,
                // where ch = '-', ':', or ' ', repectively.
                int32_t startOffset = start;
                start = subParse( text, start, prevCh, count, FALSE, ambiguousYear);
                if ( start < 0 ) {
                    pos.setErrorIndex(startOffset);
                    pos.setIndex(oldStart);
                    // {sfb} correct Date?
                    return 0;
                }
                if (start >= text.length() || ch != text[start]) {
                    // handle cases like: 'MMMM dd' in pattern vs. "janx20"
                    // in time text, where ' ' doesn't match with 'x'.
                    pos.setErrorIndex(start);
                    pos.setIndex(oldStart);
                    // {sfb} correct Date?
                    return 0;
                }
                start++;
                count = 0;
                prevCh = 0;
            }

            // otherwise, match characters exactly
            else 
            {
                if (start >= text.length() || ch != text[start]) {
                    // handle cases like: 'MMMM   dd' in pattern vs.
                    // "jan,,,20" in time text, where "   " doesn't
                    // match with ",,,".

                    pos.setErrorIndex(start);
                    pos.setIndex(oldStart);
                    // {sfb} correct Date?
                    return 0;
                }
                start++;
            }

            ++interQuoteCount;
        }
    }

    // if we still have a pending pattern symbol after we're done looping through
    // characters in the pattern string, parse the input based on the final pending
    // pattern symbol
    if (count > 0) 
    {
        int32_t startOffset = start;
        start = subParse(text, start, prevCh, count, FALSE, ambiguousYear);
        if ( start < 0 ) {
            pos.setIndex(oldStart);
            pos.setErrorIndex(startOffset);
            // {sfb} correct Date?>
            return 0;
        }
    }

    // At this point the fields of Calendar have been set.  Calendar
    // will fill in default values for missing fields when the time
    // is computed.

    pos.setIndex(start);

    // This part is a problem:  When we call parsedDate.after, we compute the time.
    // Take the date April 3 2004 at 2:30 am.  When this is first set up, the year
    // will be wrong if we're parsing a 2-digit year pattern.  It will be 1904.
    // April 3 1904 is a Sunday (unlike 2004) so it is the DST onset day.  2:30 am
    // is therefore an "impossible" time, since the time goes from 1:59 to 3:00 am
    // on that day.  It is therefore parsed out to fields as 3:30 am.  Then we
    // add 100 years, and get April 3 2004 at 3:30 am.  Note that April 3 2004 is
    // a Saturday, so it can have a 2:30 am -- and it should. [LIU]
    /*
        UDate parsedDate = calendar.getTime();
        if( ambiguousYear[0] && !parsedDate.after(fDefaultCenturyStart) ) {
            calendar.add(Calendar.YEAR, 100);
            parsedDate = calendar.getTime();
        }
    */
    // Because of the above condition, save off the fields in case we need to readjust.
    // The procedure we use here is not particularly efficient, but there is no other
    // way to do this given the API restrictions present in Calendar.  We minimize
    // inefficiency by only performing this computation when it might apply, that is,
    // when the two-digit year is equal to the start year, and thus might fall at the
    // front or the back of the default century.  This only works because we adjust
    // the year correctly to start with in other cases -- see subParse().
    UErrorCode status = U_ZERO_ERROR;
    UDate parsedDate;
    if (ambiguousYear[0]) // If this is true then the two-digit year == the default start year
    {
        // We need a copy of the fields, and we need to avoid triggering a call to
        // complete(), which will recalculate the fields.  Since we can't access
        // the fields[] array in Calendar, we clone the entire object.  This will
        // stop working if Calendar.clone() is ever rewritten to call complete().
        Calendar *savedCalendar = fCalendar->clone();
        parsedDate = fCalendar->getTime(status);
        // {sfb} check internalGetDefaultCenturyStart
        if (parsedDate < internalGetDefaultCenturyStart())
        {
            // We can't use add here because that does a complete() first.
            savedCalendar->set(Calendar::YEAR, internalGetDefaultCenturyStartYear() + 100);
            parsedDate = savedCalendar->getTime(status);
        }
        delete savedCalendar;
    }
    else parsedDate = fCalendar->getTime(status);

    // If any Calendar calls failed, we pretend that we
    // couldn't parse the string, when in reality this isn't quite accurate--
    // we did parse it; the Calendar calls just failed.
    if (U_FAILURE(status)) { 
        pos.setErrorIndex(start);
        pos.setIndex(oldStart); 
        return 0; 
    }

    return parsedDate;
}

UDate
SimpleDateFormat::parse(const UnicodeString& text, UErrorCode& status) const
{
    // redefined here because the other parse() function hides this function's
    // ounterpart on DateFormat
    return DateFormat::parse(text, status);
}
//----------------------------------------------------------------------

int32_t SimpleDateFormat::matchString(const UnicodeString& text,
                              int32_t start,
                              Calendar::EDateFields field,
                              const UnicodeString* data,
                              int32_t dataCount) const
{
    int32_t i = 0;
    int32_t count = dataCount;

    if (field == Calendar::DAY_OF_WEEK) i = 1;

    // There may be multiple strings in the data[] array which begin with
    // the same prefix (e.g., Cerven and Cervenec (June and July) in Czech).
    // We keep track of the longest match, and return that.  Note that this
    // unfortunately requires us to test all array elements.
    int32_t bestMatchLength = 0, bestMatch = -1;

    // {sfb} kludge to support case-insensitive comparison
    UnicodeString lcaseText(text);
    lcaseText.toLower();

    for (; i < count; ++i)
    {
        int32_t length = data[i].length();
        // Always compare if we have no match yet; otherwise only compare
        // against potentially better matches (longer strings).

        UnicodeString lcase(data[i]);
        lcase.toLower();
                    
        if (length > bestMatchLength && (lcaseText.compareBetween(start, start + length, lcase, 0, length)) == 0)
        {
            bestMatch = i;
            bestMatchLength = length;
        }
    }
    if (bestMatch >= 0)
    {
        fCalendar->set(field, bestMatch);
        return start + bestMatchLength;
    }
    
    return -start;
}

//----------------------------------------------------------------------

void
SimpleDateFormat::set2DigitYearStart(UDate d, UErrorCode& status)
{
    parseAmbiguousDatesAsAfter(d, status);
}

/**
 * Parse the given text, at the given position, as a numeric value, using
 * this objects fNumberFormat. Return the corresponding long value in the
 * fill-in parameter 'value'. If the parse fails, this method leaves pos
 * unchanged and returns FALSE; otherwise it advances pos and
 * returns TRUE.
 */
// {sfb} removed
/*
bool_t
SimpleDateFormat::subParseLong(const UnicodeString& text, ParsePosition& pos, int32_t& value) const
{
    Formattable parseResult;
    ParsePosition posSave = pos;
    fNumberFormat->parse(text, parseResult, pos);
    if (pos != posSave && parseResult.getType() == Formattable::kLong)
    {
        value = parseResult.getLong();
        return TRUE;
    }
    pos = posSave;
    return FALSE;
}
*/

/**
 * Private member function that converts the parsed date strings into
 * timeFields. Returns -start (for ParsePosition) if failed.
 * @param text the time text to be parsed.
 * @param start where to start parsing.
 * @param ch the pattern character for the date field text to be parsed.
 * @param count the count of a pattern character.
 * @return the new start position if matching succeeded; a negative number
 * indicating matching failure, otherwise.
 */
int32_t SimpleDateFormat::subParse(const UnicodeString& text, int32_t& start, UChar ch, int32_t count,
                           bool_t obeyCount, bool_t ambiguousYear[]) const
{
    UErrorCode status = U_ZERO_ERROR;
    Formattable number;
    int32_t value = 0;
    int32_t i;
    ParsePosition pos(0);
    int32_t patternCharIndex = -1;
    
    if ((patternCharIndex = DateFormatSymbols::fgPatternChars.indexOf(ch)) == -1) 
        return -start;
    
    pos.setIndex(start);

    Calendar::EDateFields field = fgPatternIndexToCalendarField[patternCharIndex];

    // If there are any spaces here, skip over them.  If we hit the end
    // of the string, then fail.
    for (;;) {
        if (pos.getIndex() >= text.length()) 
            return -start;
        UChar c = text[pos.getIndex()];
        if (c != 0x0020 /*' '*/ && c != 0x0009 /*'\t'*/) 
            break;
        pos.setIndex(pos.getIndex() + 1);
    }

    // We handle a few special cases here where we need to parse
    // a number value.  We handle further, more generic cases below.  We need
    // to handle some of them here because some fields require extra processing on
    // the parsed value.
    if (patternCharIndex == kHourOfDay1Field /*HOUR_OF_DAY1_FIELD*/ ||
        patternCharIndex == kHour1Field /*HOUR1_FIELD*/ ||
        (patternCharIndex == kMonthField /*MONTH_FIELD*/ && count <= 2) ||
        patternCharIndex == kYearField /*YEAR*/ ||
        patternCharIndex == kYearWOYField)
    {
        int32_t parseStart = pos.getIndex(); // WORK AROUND BUG IN NUMBER FORMAT IN 1.2B3
        // It would be good to unify this with the obeyCount logic below,
        // but that's going to be difficult.
        if (obeyCount)
        {
            if ((start+count) > text.length()) 
                return -start;
            UnicodeString temp;
            text.extractBetween(0, start + count, temp);
            fNumberFormat->parse(temp, number, pos);
        }
        else 
            fNumberFormat->parse(text, number, pos);
        if (pos.getIndex() == parseStart)
            // WORK AROUND BUG IN NUMBER FORMAT IN 1.2B3
            return -start;
        value = number.getLong();
    }

    switch (patternCharIndex) {
    case kEraField:
        return matchString(text, start, Calendar::ERA, fSymbols->fEras, fSymbols->fErasCount);
    case kYearField:
        // If there are 3 or more YEAR pattern characters, this indicates
        // that the year value is to be treated literally, without any
        // two-digit year adjustments (e.g., from "01" to 2001).  Otherwise
        // we made adjustments to place the 2-digit year in the proper
        // century, for parsed strings from "00" to "99".  Any other string
        // is treated literally:  "2250", "-1", "1", "002".
        if (count <= 2 && (pos.getIndex() - start) == 2
            && Unicode::isDigit(text.charAt(start))
            && Unicode::isDigit(text.charAt(start+1)))
        {
            // Assume for example that the defaultCenturyStart is 6/18/1903.
            // This means that two-digit years will be forced into the range
            // 6/18/1903 to 6/17/2003.  As a result, years 00, 01, and 02
            // correspond to 2000, 2001, and 2002.  Years 04, 05, etc. correspond
            // to 1904, 1905, etc.  If the year is 03, then it is 2003 if the
            // other fields specify a date before 6/18, or 1903 if they specify a
            // date afterwards.  As a result, 03 is an ambiguous year.  All other
            // two-digit years are unambiguous.
            int32_t ambiguousTwoDigitYear = fDefaultCenturyStartYear % 100;
            ambiguousYear[0] = (value == ambiguousTwoDigitYear);
            value += (fDefaultCenturyStartYear/100)*100 +
                (value < ambiguousTwoDigitYear ? 100 : 0);
        }
        fCalendar->set(Calendar::YEAR, value);
        return pos.getIndex();
    case kYearWOYField:
        // Comment is the same as for kYearFiels - look above
        if (count <= 2 && (pos.getIndex() - start) == 2
            && Unicode::isDigit(text.charAt(start))
            && Unicode::isDigit(text.charAt(start+1)))
        {
            int32_t ambiguousTwoDigitYear = fDefaultCenturyStartYear % 100;
            ambiguousYear[0] = (value == ambiguousTwoDigitYear);
            value += (fDefaultCenturyStartYear/100)*100 +
                (value < ambiguousTwoDigitYear ? 100 : 0);
        }
        fCalendar->set(Calendar::YEAR_WOY, value);
        return pos.getIndex();
    case kMonthField:
        if (count <= 2) // i.e., M or MM.
        {
            // Don't want to parse the month if it is a string
            // while pattern uses numeric style: M or MM.
            // [We computed 'value' above.]
            fCalendar->set(Calendar::MONTH, value - 1);
            return pos.getIndex();
        }
        else
        {
            // count >= 3 // i.e., MMM or MMMM
            // Want to be able to parse both short and long forms.
            // Try count == 4 first:
            int32_t newStart = 0;
            if ((newStart = matchString(text, start, Calendar::MONTH,
                                      fSymbols->fMonths, fSymbols->fMonthsCount)) > 0)
                return newStart;
            else // count == 4 failed, now try count == 3
                return matchString(text, start, Calendar::MONTH,
                                   fSymbols->fShortMonths, fSymbols->fShortMonthsCount);
        }
    case kHourOfDay1Field:
        // [We computed 'value' above.]
        if (value == fCalendar->getMaximum(Calendar::HOUR_OF_DAY) + 1) 
            value = 0;
        fCalendar->set(Calendar::HOUR_OF_DAY, value);
        return pos.getIndex();
    case kDayOfWeekField:
        {
            // Want to be able to parse both short and long forms.
            // Try count == 4 (DDDD) first:
            int32_t newStart = 0;
            if ((newStart = matchString(text, start, Calendar::DAY_OF_WEEK,
                                      fSymbols->fWeekdays, fSymbols->fWeekdaysCount)) > 0)
                return newStart;
            else // DDDD failed, now try DDD
                return matchString(text, start, Calendar::DAY_OF_WEEK,
                                   fSymbols->fShortWeekdays, fSymbols->fShortWeekdaysCount);
        }
    case kAmPmField:
        return matchString(text, start, Calendar::AM_PM, fSymbols->fAmPms, fSymbols->fAmPmsCount);
    case kHour1Field:
        // [We computed 'value' above.]
        if (value == fCalendar->getLeastMaximum(Calendar::HOUR)+1) 
            value = 0;
        fCalendar->set(Calendar::HOUR, value);
        return pos.getIndex();
    case kTimezoneField:
        {
        // First try to parse generic forms such as GMT-07:00. Do this first
        // in case localized DateFormatZoneData contains the string "GMT"
        // for a zone; in that case, we don't want to match the first three
        // characters of GMT+/-HH:MM etc.
        int32_t sign = 0;
        int32_t offset;

        // For time zones that have no known names, look for strings
        // of the form:
        //    GMT[+-]hours:minutes or
        //    GMT[+-]hhmm or
        //    GMT.
        
        // {sfb} kludge for case-insensitive compare
        UnicodeString lcaseText(text);
        lcaseText.toLower();
        UnicodeString lcaseGMT(fgGmt);
        lcaseGMT.toLower();
        
        if ((text.length() - start) > fgGmt.length() &&
            (lcaseText.compare(start, lcaseGMT.length(), lcaseGMT, 0, lcaseGMT.length())) == 0)
        {
            fCalendar->set(Calendar::DST_OFFSET, 0);

            pos.setIndex(start + fgGmt.length());

            if( text[pos.getIndex()] == 0x002B /*'+'*/ )
                sign = 1;
            else if( text[pos.getIndex()] == 0x002D /*'-'*/ )
                sign = -1;
            else {
                fCalendar->set(Calendar::ZONE_OFFSET, 0 );
                return pos.getIndex();
            }

            // Look for hours:minutes or hhmm.
            pos.setIndex(pos.getIndex() + 1);
            // WORK AROUND BUG IN NUMBER FORMAT IN 1.2B3
            int32_t parseStart = pos.getIndex();
            Formattable tzNumber;
            fNumberFormat->parse(text, tzNumber, pos);
            if( pos.getIndex() == parseStart) {
                // WORK AROUND BUG IN NUMBER FORMAT IN 1.2B3
                return -start;
            }
            if( text[pos.getIndex()] == 0x003A /*':'*/ ) {
                // This is the hours:minutes case
                offset = tzNumber.getLong() * 60;
                pos.setIndex(pos.getIndex() + 1);
                // WORK AROUND BUG IN NUMBER FORMAT IN 1.2B3
                parseStart = pos.getIndex();
                fNumberFormat->parse(text, tzNumber, pos);
                if( pos.getIndex() == parseStart) {
                    // WORK AROUND BUG IN NUMBER FORMAT IN 1.2B3
                    return -start;
                }
                offset += tzNumber.getLong();
            }
            else {
                // This is the hhmm case.
                offset = tzNumber.getLong();
                if( offset < 24 )
                    offset *= 60;
                else
                    offset = offset % 100 + offset / 100 * 60;
            }

            // Fall through for final processing below of 'offset' and 'sign'.
        }
        else {
            // At this point, check for named time zones by looking through
            // the locale data from the DateFormatZoneData strings.
            // Want to be able to parse both short and long forms.
            for (i = 0; i < fSymbols->fZoneStringsRowCount; i++)
            {
                // Checking long and short zones [1 & 2],
                // and long and short daylight [3 & 4].
                int32_t j = 1;
                
                // {sfb} kludge for case-insensitive compare
                UnicodeString s1(text);
                s1.toLower();
                
                for (; j <= 4; ++j)
                {
                    UnicodeString s2(fSymbols->fZoneStrings[i][j]);
                    s2.toLower();
                
                    if ((s1.compare(start, s2.length(), s2, 0, s2.length())) == 0)
                        break;
                }
                if (j <= 4)
                {
                    TimeZone *tz = TimeZone::createTimeZone(fSymbols->fZoneStrings[i][0]);
                    fCalendar->set(Calendar::ZONE_OFFSET, tz->getRawOffset());
                    // Must call set() with something -- TODO -- Fix this to
                    // use the correct DST SAVINGS for the zone.
                    delete tz;
                    fCalendar->set(Calendar::DST_OFFSET, j >= 3 ? U_MILLIS_PER_HOUR : 0);
                    return (start + fSymbols->fZoneStrings[i][j].length());
                }
            }

            // As a last resort, look for numeric timezones of the form
            // [+-]hhmm as specified by RFC 822.  This code is actually
            // a little more permissive than RFC 822.  It will try to do
            // its best with numbers that aren't strictly 4 digits long.
            UErrorCode status = U_ZERO_ERROR;
            DecimalFormat *fmt = new DecimalFormat("+####;-####", status);
            if(U_FAILURE(status))
                return -start;
            fmt->setParseIntegerOnly(TRUE);
            // WORK AROUND BUG IN NUMBER FORMAT IN 1.2B3
            int32_t parseStart = pos.getIndex();
            Formattable tzNumber;
            fmt->parse( text, tzNumber, pos );
            if( pos.getIndex() == parseStart) {
                // WORK AROUND BUG IN NUMBER FORMAT IN 1.2B3
                return -start;   // Wasn't actually a number.
            }
            offset = tzNumber.getLong();
            sign = 1;
            if( offset < 0 ) {
                sign = -1;
                offset = -offset;
            }
            if( offset < 24 )
                offset = offset * 60;
            else
                offset = offset % 100 + offset / 100 * 60;

            // Fall through for final processing below of 'offset' and 'sign'.
        }

        // Do the final processing for both of the above cases.  We only
        // arrive here if the form GMT+/-... or an RFC 822 form was seen.
        if (sign != 0)
        {
            offset *= U_MILLIS_PER_MINUTE * sign;

            if (fCalendar->getTimeZone().useDaylightTime())
            {
                fCalendar->set(Calendar::DST_OFFSET, U_MILLIS_PER_HOUR);
                offset -= U_MILLIS_PER_HOUR;
            }
            fCalendar->set(Calendar::ZONE_OFFSET, offset);

            return pos.getIndex();
        }

        // All efforts to parse a zone failed.
        return -start;
        }
    default:
    // case 3: // 'd' - DATE
    // case 5: // 'H' - HOUR_OF_DAY:0-based.  eg, 23:59 + 1 hour =>> 00:59
    // case 6: // 'm' - MINUTE
    // case 7: // 's' - SECOND
    // case 8: // 'S' - MILLISECOND
    // case 10: // 'D' - DAY_OF_YEAR
    // case 11: // 'F' - DAY_OF_WEEK_IN_MONTH
    // case 12: // 'w' - WEEK_OF_YEAR
    // case 13: // 'W' - WEEK_OF_MONTH
    // case 16: // 'K' - HOUR: 0-based.  eg, 11PM + 1 hour =>> 0 AM
	// 'e' - DOW_LOCAL

        // WORK AROUND BUG IN NUMBER FORMAT IN 1.2B3
        int32_t parseStart = pos.getIndex();
        // Handle "generic" fields
        if (obeyCount)
        {
            if ((start+count) > text.length()) 
                return -start;
            UnicodeString s;
            // {sfb} old code had extract, make sure it works
            text.extractBetween(0, start + count, s);
            fNumberFormat->parse(s, number, pos);
        }
        else 
            fNumberFormat->parse(text, number, pos);
        if (pos.getIndex() != parseStart) {
            // WORK AROUND BUG IN NUMBER FORMAT IN 1.2B3
            fCalendar->set(field, number.getLong());
            return pos.getIndex();
        }
        return -start;
    }
}

//----------------------------------------------------------------------

void SimpleDateFormat::translatePattern(const UnicodeString& originalPattern,
                                        UnicodeString& translatedPattern,
                                        const UnicodeString& from,
                                        const UnicodeString& to,
                                        UErrorCode& status)
{
  // run through the pattern and convert any pattern symbols from the version
  // in "from" to the corresponding character ion "to".  This code takes
  // quoted strings into account (it doesn't try to translate them), and it signals
  // an error if a particular "pattern character" doesn't appear in "from".
  // Depending on the values of "from" and "to" this can convert from generic
  // to localized patterns or localized to generic.
  if (U_FAILURE(status)) 
    return;
  
  translatedPattern.remove();
  bool_t inQuote = FALSE;
  for (UTextOffset i = 0; i < originalPattern.length(); ++i) {
    UChar c = originalPattern[i];
    if (inQuote) {
      if (c == 0x0027 /*'\''*/) 
    inQuote = FALSE;
    }
    else {
      if (c == 0x0027 /*'\''*/) 
    inQuote = TRUE;
      else if ((c >= 0x0061 /*'a'*/ && c <= 0x007A) /*'z'*/ 
           || (c >= 0x0041 /*'A'*/ && c <= 0x005A /*'Z'*/)) {
    UTextOffset ci = from.indexOf(c);
    if (ci == -1) {
      status = U_INVALID_FORMAT_ERROR;
      return;
    }
    c = to[ci];
      }
    }
    translatedPattern += c;
  }
  if (inQuote) {
    status = U_INVALID_FORMAT_ERROR;
    return;
  }
}

//----------------------------------------------------------------------

UnicodeString&
SimpleDateFormat::toPattern(UnicodeString& result) const
{
    result = fPattern;
    return result;
}

//----------------------------------------------------------------------

UnicodeString&
SimpleDateFormat::toLocalizedPattern(UnicodeString& result,
                                     UErrorCode& status) const
{
    translatePattern(fPattern, result, DateFormatSymbols::fgPatternChars, fSymbols->fLocalPatternChars, status);
    return result;
}

//----------------------------------------------------------------------

void
SimpleDateFormat::applyPattern(const UnicodeString& pattern)
{
    fPattern = pattern;
}

//----------------------------------------------------------------------

void
SimpleDateFormat::applyLocalizedPattern(const UnicodeString& pattern,
                                        UErrorCode &status)
{
    translatePattern(pattern, fPattern, fSymbols->fLocalPatternChars, DateFormatSymbols::fgPatternChars, status);
}

//----------------------------------------------------------------------

const DateFormatSymbols*
SimpleDateFormat::getDateFormatSymbols() const
{
    return fSymbols;
}

//----------------------------------------------------------------------

void
SimpleDateFormat::adoptDateFormatSymbols(DateFormatSymbols* newFormatSymbols)
{
    delete fSymbols;
    fSymbols = newFormatSymbols;
}

//----------------------------------------------------------------------
void
SimpleDateFormat::setDateFormatSymbols(const DateFormatSymbols& newFormatSymbols)
{
    delete fSymbols;
    fSymbols = new DateFormatSymbols(newFormatSymbols);
}


//----------------------------------------------------------------------

// {sfb} removed
/*int32_t
SimpleDateFormat::getZoneIndex(const UnicodeString& ID) const
{
    // this function searches a time zone list for a time zone with the specified
    // ID.  It'll either return an apprpriate row number or -1 if the ID wasn't
    // found.
    int32_t index, col;

    for (col=0; col<=4 && col<fSymbols->fZoneStringsColCount; col+=2)
    {
        for (index = 0; index < fSymbols->fZoneStringsRowCount; index++)
        {
            if (fSymbols->fZoneStrings[index][col] == ID) return index;
        }
    }

    return - 1;
}*/

//----------------------------------------------------------------------

UDate
SimpleDateFormat::internalGetDefaultCenturyStart() const
{
    // lazy-evaluate systemDefaultCenturyStart
    if (fgSystemDefaultCenturyStart == fgSystemDefaultCentury)
        initializeSystemDefaultCentury();

    // use defaultCenturyStart unless it's the flag value;
    // then use systemDefaultCenturyStart
    return (fDefaultCenturyStart == fgSystemDefaultCentury) ?
        fgSystemDefaultCenturyStart : fDefaultCenturyStart;
}

int32_t
SimpleDateFormat::internalGetDefaultCenturyStartYear() const
{
    // lazy-evaluate systemDefaultCenturyStartYear
    if (fgSystemDefaultCenturyStart == fgSystemDefaultCentury)
        initializeSystemDefaultCentury();

    // use defaultCenturyStart unless it's the flag value;
    // then use systemDefaultCenturyStartYear
    //return (fDefaultCenturyStart == fgSystemDefaultCentury) ?
    return (fDefaultCenturyStartYear == fgSystemDefaultCenturyYear) ?
        fgSystemDefaultCenturyStartYear : fDefaultCenturyStartYear;
}

void
SimpleDateFormat::initializeSystemDefaultCentury()
{
    // initialize systemDefaultCentury and systemDefaultCenturyYear based
    // on the current time.  They'll be set to 80 years before
    // the current time.
    // No point in locking as it should be idempotent.
    if (fgSystemDefaultCenturyStart == fgSystemDefaultCentury)
    {
        UErrorCode status = U_ZERO_ERROR;
        Calendar *calendar = Calendar::createInstance(status);
        if (calendar != NULL && U_SUCCESS(status))
        {
            calendar->setTime(Calendar::getNow(), status);
            calendar->add(Calendar::YEAR, -80, status);
            fgSystemDefaultCenturyStart = calendar->getTime(status);
            fgSystemDefaultCenturyStartYear = calendar->get(Calendar::YEAR, status);
            delete calendar;
        }
        // We have no recourse upon failure unless we want to propagate the failure
        // out.
    }
}

//eof
