/*
*******************************************************************************
* Copyright (C) 1997-1999, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* File DTFMTSYM.CPP
*
* Modification History:
*
*   Date        Name        Description
*   02/19/97    aliu        Converted from java.
*   07/21/98    stephen     Added getZoneIndex
*                            Changed weekdays/short weekdays to be one-based
*   06/14/99    stephen     Removed SimpleDateFormat::fgTimeZoneDataSuffix
*   11/16/99    weiv        Added 'Y' and 'e' to fgPatternChars
*   03/27/00    weiv        Keeping resource bundle around!
*******************************************************************************
*/
 
#include "unicode/dtfmtsym.h"
#include "unicode/resbund.h"
#include "unicode/smpdtfmt.h"
#include "mutex.h"
 
// *****************************************************************************
// class DateFormatSymbols
// *****************************************************************************

#define PATTERN_CHARS_LEN 20

// generic date-format pattern symbols.  For their meanings, see class docs
// for SimpleDateFormat
UnicodeString DateFormatSymbols::fgPatternChars = UNICODE_STRING("GyMdkHmsSEDFwWahKzYe", PATTERN_CHARS_LEN);

//------------------------------------------------------
// Strings of last resort.  These are only used if we have no resource
// files.  They aren't designed for actual use, just for backup.

//============================================================
// To make the VC++ compiler happy these must occur before the
// sizeof operations on the arrays occur below.
//============================================================

// These are the month names and abbreviations of last resort.
const UnicodeString DateFormatSymbols::fgLastResortMonthNames[] =
{
    UNICODE_STRING("01", 2), UNICODE_STRING("02", 2), UNICODE_STRING("03", 2), UNICODE_STRING("04", 2),
    UNICODE_STRING("05", 2), UNICODE_STRING("06", 2), UNICODE_STRING("07", 2), UNICODE_STRING("08", 2),
    UNICODE_STRING("09", 2), UNICODE_STRING("10", 2), UNICODE_STRING("11", 2), UNICODE_STRING("12", 2),
    UNICODE_STRING("13", 2)
};

// These are the weekday names and abbreviations of last resort.
const UnicodeString DateFormatSymbols::fgLastResortDayNames[] =
{
    UnicodeString(), UNICODE_STRING("1", 1), UNICODE_STRING("2", 1), UNICODE_STRING("3", 1),
    UNICODE_STRING("4", 1), UNICODE_STRING("5", 1), UNICODE_STRING("6", 1), UNICODE_STRING("7", 1)
};

// These are the am/pm and BC/AD markers of last resort.
const UnicodeString DateFormatSymbols::fgLastResortAmPmMarkers[] =
{
    UNICODE_STRING("AM", 2), UNICODE_STRING("PM", 2)
};

const UnicodeString DateFormatSymbols::fgLastResortEras[] =
{
    UNICODE_STRING("BC", 2), UNICODE_STRING("AD", 2)
};

// These are the zone strings of last resort.
UnicodeString** DateFormatSymbols::fgLastResortZoneStringsH = 0;

const UnicodeString DateFormatSymbols::fgLastResortZoneStrings[] =
{
    UNICODE_STRING("GMT", 3), UNICODE_STRING("GMT", 3), UNICODE_STRING("GMT", 3), UNICODE_STRING("GMT", 3), UNICODE_STRING("GMT", 3)
};

//------------------------------------------------------

DateFormatSymbols::DateFormatSymbols(const Locale& locale,
                                     UErrorCode& status)
{
    initializeData(locale, status);
}

DateFormatSymbols::DateFormatSymbols(UErrorCode& status)
{
    initializeData(Locale::getDefault(), status, TRUE);
}

DateFormatSymbols::DateFormatSymbols(const DateFormatSymbols& other)
{
    fIsOwned = 0; // We own nothing (ignore existing pointers)
    *this = other;
}

void
DateFormatSymbols::assignArray(UnicodeString*& dstArray,
                               int32_t& dstCount,
                               const UnicodeString* srcArray,
                               int32_t srcCount,
                               const DateFormatSymbols& other,
                               int32_t which)
{
    // duplicates or aliases the source array, depending on the status of
    // the appropriate isOwned flag
    bool_t owned = other.isOwned(which);
    setIsOwned(which, owned);
    dstCount = srcCount;
    if (owned)
    {
        dstArray = new UnicodeString[srcCount];
        uprv_arrayCopy(srcArray, dstArray, srcCount);
    }
    else
    {
        dstArray = (UnicodeString*)srcArray; // Compiler requires cast
    }
}

/**
 * Create a copy, in fZoneStrings, of the given zone strings array.  The
 * member variables fZoneStringsRowCount and fZoneStringsColCount should
 * be set already by the caller.  The fIsOwned flags are not checked or set
 * by this method; that is the caller's responsibility.
 */
void
DateFormatSymbols::createZoneStrings(const UnicodeString *const * otherStrings)
{
    fZoneStrings = new UnicodeString*[fZoneStringsRowCount];
    for (int32_t row=0; row<fZoneStringsRowCount; ++row)
    {
        fZoneStrings[row] = new UnicodeString[fZoneStringsColCount];
        uprv_arrayCopy(otherStrings[row], fZoneStrings[row], fZoneStringsColCount);
    }
}

/**
 * Assignment operator.  A bit messy because the other object may or may not
 * own each of its arrays.  We then alias or copy those arrays as appropriate.
 * Arrays that aren't owned are assumed to be permanently "around," which is
 * true, since they are owned by the ResourceBundle cache.
 */
DateFormatSymbols& DateFormatSymbols::operator=(const DateFormatSymbols& other)
{
    dispose();
    assignArray(fEras, fErasCount, other.fEras, other.fErasCount, other, kEras);
    assignArray(fMonths, fMonthsCount, other.fMonths, other.fMonthsCount, other, kMonths);
    assignArray(fShortMonths, fShortMonthsCount, other.fShortMonths, other.fShortMonthsCount, other, kShortMonths);
    assignArray(fWeekdays, fWeekdaysCount, other.fWeekdays, other.fWeekdaysCount, other, kWeekdays);
    assignArray(fShortWeekdays, fShortWeekdaysCount, other.fShortWeekdays, other.fShortWeekdaysCount, other, kShortWeekdays);
    assignArray(fAmPms, fAmPmsCount, other.fAmPms, other.fAmPmsCount, other, kAmPms);

    bool_t owned = other.isOwned(kZoneStrings);
    setIsOwned(kZoneStrings, owned);
    fZoneStringsRowCount = other.fZoneStringsRowCount;
    fZoneStringsColCount = other.fZoneStringsColCount;
    if (owned) createZoneStrings((const UnicodeString**)other.fZoneStrings);
        else fZoneStrings = other.fZoneStrings;

    fLocalPatternChars = other.fLocalPatternChars;

    return *this;
}

DateFormatSymbols::~DateFormatSymbols()
{
    dispose();
}

void DateFormatSymbols::dispose()
{
    // Delete those items which we have marked as owned

    if (isOwned(kEras))             delete[] fEras;
    if (isOwned(kMonths))           delete[] fMonths;
    if (isOwned(kShortMonths))      delete[] fShortMonths;
    if (isOwned(kWeekdays))         delete[] fWeekdays;
    if (isOwned(kShortWeekdays))    delete[] fShortWeekdays;
    if (isOwned(kAmPms))            delete[] fAmPms;
    
    disposeZoneStrings();

    fIsOwned = 0; // Indicate that we no longer need to delete anything
}

void DateFormatSymbols::disposeZoneStrings()
{
    if (isOwned(kZoneStrings))
    {
        for (int32_t row=0; row<fZoneStringsRowCount; ++row)
            delete[] fZoneStrings[row];
        delete[] fZoneStrings;
    }
}

bool_t
DateFormatSymbols::arrayCompare(const UnicodeString* array1,
                                const UnicodeString* array2,
                                int32_t count)
{
    if (array1 == array2) return TRUE;
    while (count>0)
    {
        --count;
        if (array1[count] != array2[count]) return FALSE;
    }
    return TRUE;
}

bool_t
DateFormatSymbols::operator==(const DateFormatSymbols& other) const
{
    // First do cheap comparisons
    if (fErasCount == other.fErasCount &&
        fMonthsCount == other.fMonthsCount &&
        fShortMonthsCount == other.fShortMonthsCount &&
        fWeekdaysCount == other.fWeekdaysCount &&
        fShortWeekdaysCount == other.fShortWeekdaysCount &&
        fAmPmsCount == other.fAmPmsCount &&
        fZoneStringsRowCount == other.fZoneStringsRowCount &&
        fZoneStringsColCount == other.fZoneStringsColCount)
    {
        // Now compare the arrays themselves
        if (arrayCompare(fEras, other.fEras, fErasCount) &&
            arrayCompare(fMonths, other.fMonths, fMonthsCount) &&
            arrayCompare(fShortMonths, other.fShortMonths, fShortMonthsCount) &&
            arrayCompare(fWeekdays, other.fWeekdays, fWeekdaysCount) &&
            arrayCompare(fShortWeekdays, other.fShortWeekdays, fShortWeekdaysCount) &&
            arrayCompare(fAmPms, other.fAmPms, fAmPmsCount))
        {
            if (fZoneStrings == other.fZoneStrings) return TRUE;

            for (int32_t row=0; row<fZoneStringsRowCount; ++row)
            {
                if (!arrayCompare(fZoneStrings[row], other.fZoneStrings[row], fZoneStringsColCount))
                    return FALSE;
            }
            return TRUE;
        }
    }
    return FALSE;
}

//------------------------------------------------------

const UnicodeString*
DateFormatSymbols::getEras(int32_t &count) const
{
    count = fErasCount;
    return fEras;
}

const UnicodeString*
DateFormatSymbols::getMonths(int32_t &count) const
{
    count = fMonthsCount;
    return fMonths;
}

const UnicodeString*
DateFormatSymbols::getShortMonths(int32_t &count) const
{
    count = fShortMonthsCount;
    return fShortMonths;
}

const UnicodeString*
DateFormatSymbols::getWeekdays(int32_t &count) const
{
    count = fWeekdaysCount;
    return fWeekdays;
}

const UnicodeString*
DateFormatSymbols::getShortWeekdays(int32_t &count) const
{
    count = fShortWeekdaysCount;
    return fShortWeekdays;
}

const UnicodeString*
DateFormatSymbols::getAmPmStrings(int32_t &count) const
{
    count = fAmPmsCount;
    return fAmPms;
}

//------------------------------------------------------

void
DateFormatSymbols::setEras(const UnicodeString* erasArray, int32_t count)
{
    // delete the old list if we own it
    if (isOwned(kEras)) delete[] fEras;

    // we always own the new list, which we create here (we duplicate rather
    // than adopting the list passed in)
    setIsOwned(kEras, TRUE);
    fEras = new UnicodeString[count];
    uprv_arrayCopy(erasArray,fEras,  count);
    fErasCount = count;
}

void
DateFormatSymbols::setMonths(const UnicodeString* monthsArray, int32_t count)
{
    // delete the old list if we own it
    if (isOwned(kMonths)) delete[] fMonths;

    // we always own the new list, which we create here (we duplicate rather
    // than adopting the list passed in)
    setIsOwned(kMonths, TRUE);
    fMonths = new UnicodeString[count];
    uprv_arrayCopy( monthsArray,fMonths,count);
    fMonthsCount = count;
}

void
DateFormatSymbols::setShortMonths(const UnicodeString* shortMonthsArray, int32_t count)
{
    // delete the old list if we own it
    if (isOwned(kShortMonths)) delete[] fShortMonths;

    // we always own the new list, which we create here (we duplicate rather
    // than adopting the list passed in)
    setIsOwned(kShortMonths, TRUE);
    fShortMonths = new UnicodeString[count];
    uprv_arrayCopy(shortMonthsArray,fShortMonths,  count);
    fShortMonthsCount = count;
}

void DateFormatSymbols::setWeekdays(const UnicodeString* weekdaysArray, int32_t count)
{
    // delete the old list if we own it
    if (isOwned(kWeekdays)) delete[] fWeekdays;

    // we always own the new list, which we create here (we duplicate rather
    // than adopting the list passed in)
    setIsOwned(kWeekdays, TRUE);
    fWeekdays = new UnicodeString[count];
    uprv_arrayCopy(weekdaysArray,fWeekdays,count);
    fWeekdaysCount = count;
}

void
DateFormatSymbols::setShortWeekdays(const UnicodeString* shortWeekdaysArray, int32_t count)
{
    // delete the old list if we own it
    if (isOwned(kShortWeekdays)) delete[] fShortWeekdays;

    // we always own the new list, which we create here (we duplicate rather
    // than adopting the list passed in)
    setIsOwned(kShortWeekdays, TRUE);
    fShortWeekdays = new UnicodeString[count];
    uprv_arrayCopy( shortWeekdaysArray,fShortWeekdays,count);
    fShortWeekdaysCount = count;
}

void
DateFormatSymbols::setAmPmStrings(const UnicodeString* amPmsArray, int32_t count)
{
    // delete the old list if we own it
    if (isOwned(kAmPms)) delete[] fAmPms;

    // we always own the new list, which we create here (we duplicate rather
    // than adopting the list passed in)
    setIsOwned(kAmPms, TRUE);
    fAmPms = new UnicodeString[count];
    uprv_arrayCopy(amPmsArray,fAmPms,count);
    fAmPmsCount = count;
}

//------------------------------------------------------

const UnicodeString**
DateFormatSymbols::getZoneStrings(int32_t& rowCount, int32_t& columnCount) const
{
    rowCount = fZoneStringsRowCount;
    columnCount = fZoneStringsColCount;
    return (const UnicodeString**)fZoneStrings; // Compiler requires cast
}

void
DateFormatSymbols::setZoneStrings(const UnicodeString* const *strings, int32_t rowCount, int32_t columnCount)
{
    // since deleting a 2-d array is a pain in the butt, we offload that task to
    // a separate function
    disposeZoneStrings();

    // we always own the new list, which we create here (we duplicate rather
    // than adopting the list passed in)
    setIsOwned(kZoneStrings, TRUE);
    fZoneStringsRowCount = rowCount;
    fZoneStringsColCount = columnCount;
    createZoneStrings((const UnicodeString**)strings);
}

//------------------------------------------------------

UnicodeString&
DateFormatSymbols::getLocalPatternChars(UnicodeString& result) const
{
    result = fLocalPatternChars;
    return result;
}

//------------------------------------------------------

void
DateFormatSymbols::setLocalPatternChars(const UnicodeString& newLocalPatternChars)
{
    fLocalPatternChars = newLocalPatternChars;
}

//------------------------------------------------------

void
DateFormatSymbols::initField(UnicodeString **field, int32_t& length, const ResourceBundle data, uint8_t ownfield, UErrorCode &status) {
    //ResourceBundle data = source->getByKey(tag, status);
    length = data.getSize();
    *field = new UnicodeString[length];
    for(int32_t i = 0; i<length; i++) {
        *(*(field)+i) = data.getStringEx(i, status);
    }
    setIsOwned(ownfield, TRUE);
}

void
DateFormatSymbols::initializeData(const Locale& locale, UErrorCode& status, bool_t useLastResortData)
{
    if (U_FAILURE(status)) return;

    fIsOwned = 0; // Nothing is owned

    /**
     * Retrieve the string arrays we need from the resource bundle file.
     * We cast away const here, but that's okay; we won't delete any of
     * these.
     */
    /*ResourceBundle resource(Locale::getDataDirectory(), locale, status);*/
    ResourceBundle resource(NULL, locale, status);
    /*resource.open(UnicodeString(""), locale, status);*/
    if (U_FAILURE(status))
    {
        if (useLastResortData)
        {
            // Handle the case in which there is no resource data present.
            // We don't have to generate usable patterns in this situation;
            // we just need to produce something that will be semi-intelligible
            // in most locales.

            status = U_USING_FALLBACK_ERROR;

            fEras = (UnicodeString*)fgLastResortEras;
            fErasCount = sizeof(fgLastResortEras[0]) / sizeof(fgLastResortEras[0]);
            fMonths = fShortMonths = (UnicodeString*)fgLastResortMonthNames;
            fMonthsCount = fShortMonthsCount = sizeof(fgLastResortMonthNames) / sizeof(fgLastResortMonthNames[0]);
            fWeekdays = fShortWeekdays = (UnicodeString*)fgLastResortDayNames;
            fWeekdaysCount = fShortWeekdaysCount = sizeof(fgLastResortDayNames) / sizeof(fgLastResortDayNames[0]);
            fAmPms = (UnicodeString*)fgLastResortAmPmMarkers;
            fAmPmsCount = sizeof(fgLastResortAmPmMarkers) / sizeof(fgLastResortAmPmMarkers[0]);
            if (fgLastResortZoneStringsH == 0)
            {
                // Initialize this -- the compiler doesn't like to do so at static init time
                UnicodeString **tempH = fgLastResortZoneStringsH = new UnicodeString*[1];

                Mutex lock; // This could be optimized but it's not worth it -- exceptional case
                *fgLastResortZoneStringsH = (UnicodeString*)fgLastResortZoneStrings;
            }
            fZoneStrings = fgLastResortZoneStringsH;
            fZoneStringsRowCount = 1;
            fZoneStringsColCount = sizeof(fgLastResortZoneStrings);
            fLocalPatternChars = fgPatternChars;
        }
        return;
    }

    // if we make it to here, the resource data is cool, and we can get everything out
    // of it that we need except for the time-zone and localized-pattern data, which
    // are stoerd in a separate file
    initField(&fEras, fErasCount, resource.get(SimpleDateFormat::fgErasTag, status), kEras, status);
    initField(&fMonths, fMonthsCount, resource.get(SimpleDateFormat::fgMonthNamesTag, status), kMonths, status);
    initField(&fShortMonths, fShortMonthsCount, resource.get(SimpleDateFormat::fgMonthAbbreviationsTag, status), kShortMonths, status);
    initField(&fAmPms, fAmPmsCount, resource.get(SimpleDateFormat::fgAmPmMarkersTag, status), kAmPms, status);
    fLocalPatternChars = resource.getStringEx(SimpleDateFormat::fgLocalPatternCharsTag, status);


    //fEras           = (UnicodeString*)resource.getStringArray(SimpleDateFormat::fgErasTag, fErasCount, status);
    //fMonths         = (UnicodeString*)resource.getStringArray(SimpleDateFormat::fgMonthNamesTag, fMonthsCount, status);
    //fShortMonths    = (UnicodeString*)resource.getStringArray(SimpleDateFormat::fgMonthAbbreviationsTag, fShortMonthsCount, status);
    //fAmPms          = (UnicodeString*)resource.getStringArray(SimpleDateFormat::fgAmPmMarkersTag, fAmPmsCount, status);
    //fLocalPatternChars = *resource.getString(SimpleDateFormat::fgLocalPatternCharsTag, status);

    ResourceBundle zoneArray = resource.get(SimpleDateFormat::fgZoneStringsTag, status);
    fZoneStringsRowCount = zoneArray.getSize();
    ResourceBundle zoneRow = zoneArray.get((int32_t)0, status);
    fZoneStringsColCount = zoneRow.getSize();
    fZoneStrings = new UnicodeString * [fZoneStringsRowCount];
    for(int32_t i = 0; i<fZoneStringsRowCount; i++) {
        *(fZoneStrings+i) = new UnicodeString[fZoneStringsColCount];
        zoneRow = zoneArray.get(i, status);
        for(int32_t j = 0; j<fZoneStringsColCount; j++) {
            fZoneStrings[i][j] = zoneRow.getStringEx(j, status);
        }
    }
    setIsOwned(kZoneStrings, TRUE);

/*
    ResourceBundle data = resource.getByKey(SimpleDateFormat::fgErasTag, status);
    fErasCount = data.getSize();
    fEras = new UnicodeString[fErasCount];
    for(int32_t i = 0; i<fErasCount; i++) {
        fEras[i] = data.getStringByIndex(i, status);
    }
    setIsOwned(kEras, TRUE);
    data = resource.getByKey(SimpleDateFormat::fgMonthNamesTag, status);
    fMonthsCount = data.getSize();
    fMonths = new UnicodeString[fMonthsCount];
    for(int32_t i = 0; i<fMonthsCount; i++) {
        fMonths[i] = data.getStringByIndex(i, status);
    }
    setIsOwned(kMonths, TRUE);
*/
    // {sfb} fixed to handle 1-based weekdays
    ResourceBundle weekdaysData = resource.get(SimpleDateFormat::fgDayNamesTag, status);
    fWeekdaysCount = weekdaysData.getSize();
    fWeekdays = new UnicodeString[fWeekdaysCount+1];
    fWeekdays[0] = UnicodeString();
    for(i = 0; i<fWeekdaysCount; i++) {
        fWeekdays[i+1] = weekdaysData.getStringEx(i, status);
    }
    setIsOwned(kWeekdays, TRUE);

    ResourceBundle lsweekdaysData = resource.get(SimpleDateFormat::fgDayAbbreviationsTag, status);
    fShortWeekdaysCount = lsweekdaysData.getSize();
    fShortWeekdays = new UnicodeString[fShortWeekdaysCount+1];
    fShortWeekdays[0] = UnicodeString();
    for(i = 0; i<fShortWeekdaysCount; i++) {
        fShortWeekdays[i+1] = lsweekdaysData.getStringEx(i, status);
    }
    setIsOwned(kShortWeekdays, TRUE);

    fWeekdaysCount = fShortWeekdaysCount = 8;
    
    // If the locale data does not include new pattern chars, use the defaults
    if (fLocalPatternChars.length() < PATTERN_CHARS_LEN) {
        UnicodeString str;
        fgPatternChars.extractBetween(fLocalPatternChars.length(), PATTERN_CHARS_LEN, str);
        fLocalPatternChars.append(str);
    }
}

/**
 * Package private: used by SimpleDateFormat
 * Gets the index for the given time zone ID to obtain the timezone
 * strings for formatting. The time zone ID is just for programmatic
 * lookup. NOT LOCALIZED!!!
 * @param ID the given time zone ID.
 * @return the index of the given time zone ID.  Returns -1 if
 * the given time zone ID can't be located in the DateFormatSymbols object.
 * @see java.util.SimpleTimeZone
 */
int32_t DateFormatSymbols::getZoneIndex(const UnicodeString& ID) const
{
    // {sfb} kludge to support case-insensitive comparison
    UnicodeString lcaseID(ID);
    lcaseID.toLower();
    
    for(int32_t index = 0; index < fZoneStringsRowCount; index++) {
        UnicodeString lcase(fZoneStrings[index][0]);
        lcase.toLower();
        if (lcaseID == lcase) 
            return index;
    }

    return -1;
}

//eof
