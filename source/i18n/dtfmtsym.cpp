/*
*******************************************************************************
* Copyright (C) 1997-2001, International Business Machines Corporation and    *
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
 
// *****************************************************************************
// class DateFormatSymbols
// *****************************************************************************

/**
 * These are static arrays we use only in the case where we have no
 * resource data.
 */

#define PATTERN_CHARS_LEN 20

/**
 * Unlocalized date-time pattern characters. For example: 'y', 'd', etc. All
 * locales use the same these unlocalized pattern characters.
 */
static const UChar gPatternChars[] = 
{
    0x47, 0x79, 0x4D, 0x64, 0x6B, 0x48, 0x6D, 0x73, 0x53, 0x45,
    0x44, 0x46, 0x77, 0x57, 0x61, 0x68, 0x4B, 0x7A, 0x59, 0x65, 0 /* "GyMdkHmsSEDFwWahKzYe" */
};

//------------------------------------------------------
// Strings of last resort.  These are only used if we have no resource
// files.  They aren't designed for actual use, just for backup.

// These are the month names and abbreviations of last resort.
static const UChar gLastResortMonthNames[13][3] =
{
    {0x0030, 0x0031, 0x0000}, /* "01" */
    {0x0030, 0x0032, 0x0000}, /* "02" */
    {0x0030, 0x0033, 0x0000}, /* "03" */
    {0x0030, 0x0034, 0x0000}, /* "04" */
    {0x0030, 0x0035, 0x0000}, /* "05" */
    {0x0030, 0x0036, 0x0000}, /* "06" */
    {0x0030, 0x0037, 0x0000}, /* "07" */
    {0x0030, 0x0038, 0x0000}, /* "08" */
    {0x0030, 0x0039, 0x0000}, /* "09" */
    {0x0031, 0x0030, 0x0000}, /* "10" */
    {0x0031, 0x0031, 0x0000}, /* "11" */
    {0x0031, 0x0032, 0x0000}, /* "12" */
    {0x0031, 0x0033, 0x0000}  /* "13" */
};

// These are the weekday names and abbreviations of last resort.
static const UChar gLastResortDayNames[8][2] =
{
    {0x0000, 0x0000}, /* "" */
    {0x0031, 0x0000}, /* "1" */
    {0x0032, 0x0000}, /* "2" */
    {0x0033, 0x0000}, /* "3" */
    {0x0034, 0x0000}, /* "4" */
    {0x0035, 0x0000}, /* "5" */
    {0x0036, 0x0000}, /* "6" */
    {0x0037, 0x0000}  /* "7" */
};

// These are the am/pm and BC/AD markers of last resort.
static const UChar gLastResortAmPmMarkers[2][3] =
{
    {0x0041, 0x004D, 0x0000}, /* "AM" */
    {0x0050, 0x004D, 0x0000}  /* "PM" */
};

static const UChar gLastResortEras[2][3] =
{
    {0x0042, 0x0043, 0x0000}, /* "BC" */
    {0x0041, 0x0044, 0x0000}  /* "AD" */
};


// These are the zone strings of last resort.
static const UChar gLastResortZoneStrings[5][4] =
{
    {0x0047, 0x004D, 0x0054, 0x0000}, /* "GMT" */
    {0x0047, 0x004D, 0x0054, 0x0000}, /* "GMT" */
    {0x0047, 0x004D, 0x0054, 0x0000}, /* "GMT" */
    {0x0047, 0x004D, 0x0054, 0x0000}, /* "GMT" */
    {0x0047, 0x004D, 0x0054, 0x0000}  /* "GMT" */
};

/**
 * These are the tags we expect to see in normal resource bundle files associated
 * with a locale.
 */
const char DateFormatSymbols::fgErasTag[]="Eras";
const char DateFormatSymbols::fgMonthNamesTag[]="MonthNames";
const char DateFormatSymbols::fgMonthAbbreviationsTag[]="MonthAbbreviations";
const char DateFormatSymbols::fgDayNamesTag[]="DayNames";
const char DateFormatSymbols::fgDayAbbreviationsTag[]="DayAbbreviations";
const char DateFormatSymbols::fgAmPmMarkersTag[]="AmPmMarkers";

/**
 * These are the tags we expect to see in time zone data resource bundle files
 * associated with a locale.
 */
const char DateFormatSymbols::fgZoneStringsTag[]="zoneStrings";
const char DateFormatSymbols::fgLocalPatternCharsTag[]="localPatternChars";

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
    copyData(other);
}

void
DateFormatSymbols::assignArray(UnicodeString*& dstArray,
                               int32_t& dstCount,
                               const UnicodeString* srcArray,
                               int32_t srcCount)
{
    // duplicates or aliases the source array, depending on the status of
    // the appropriate isOwned flag
    dstCount = srcCount;
    dstArray = new UnicodeString[srcCount];
    uprv_arrayCopy(srcArray, dstArray, srcCount);
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
 * Copy all of the other's data to this.
 */
void
DateFormatSymbols::copyData(const DateFormatSymbols& other) {
    assignArray(fEras, fErasCount, other.fEras, other.fErasCount);
    assignArray(fMonths, fMonthsCount, other.fMonths, other.fMonthsCount);
    assignArray(fShortMonths, fShortMonthsCount, other.fShortMonths, other.fShortMonthsCount);
    assignArray(fWeekdays, fWeekdaysCount, other.fWeekdays, other.fWeekdaysCount);
    assignArray(fShortWeekdays, fShortWeekdaysCount, other.fShortWeekdays, other.fShortWeekdaysCount);
    assignArray(fAmPms, fAmPmsCount, other.fAmPms, other.fAmPmsCount);

    fZoneStringsRowCount = other.fZoneStringsRowCount;
    fZoneStringsColCount = other.fZoneStringsColCount;
    createZoneStrings((const UnicodeString**)other.fZoneStrings);

    fLocalPatternChars = other.fLocalPatternChars;
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
    copyData(other);

    return *this;
}

DateFormatSymbols::~DateFormatSymbols()
{
    dispose();
}

void DateFormatSymbols::dispose()
{
    if (fEras)          delete[] fEras;
    if (fMonths)        delete[] fMonths;
    if (fShortMonths)   delete[] fShortMonths;
    if (fWeekdays)      delete[] fWeekdays;
    if (fShortWeekdays) delete[] fShortWeekdays;
    if (fAmPms)         delete[] fAmPms;

    disposeZoneStrings();
}

void DateFormatSymbols::disposeZoneStrings()
{
    if (fZoneStrings) {
        for (int32_t row=0; row<fZoneStringsRowCount; ++row)
            delete[] fZoneStrings[row];
        delete[] fZoneStrings;
    }
}

UBool
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

UBool
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
    if (fEras) delete[] fEras;

    // we always own the new list, which we create here (we duplicate rather
    // than adopting the list passed in)
    fEras = new UnicodeString[count];
    uprv_arrayCopy(erasArray,fEras,  count);
    fErasCount = count;
}

void
DateFormatSymbols::setMonths(const UnicodeString* monthsArray, int32_t count)
{
    // delete the old list if we own it
    if (fMonths) delete[] fMonths;

    // we always own the new list, which we create here (we duplicate rather
    // than adopting the list passed in)
    fMonths = new UnicodeString[count];
    uprv_arrayCopy( monthsArray,fMonths,count);
    fMonthsCount = count;
}

void
DateFormatSymbols::setShortMonths(const UnicodeString* shortMonthsArray, int32_t count)
{
    // delete the old list if we own it
    if (fShortMonths) delete[] fShortMonths;

    // we always own the new list, which we create here (we duplicate rather
    // than adopting the list passed in)
    fShortMonths = new UnicodeString[count];
    uprv_arrayCopy(shortMonthsArray,fShortMonths,  count);
    fShortMonthsCount = count;
}

void DateFormatSymbols::setWeekdays(const UnicodeString* weekdaysArray, int32_t count)
{
    // delete the old list if we own it
    if (fWeekdays) delete[] fWeekdays;

    // we always own the new list, which we create here (we duplicate rather
    // than adopting the list passed in)
    fWeekdays = new UnicodeString[count];
    uprv_arrayCopy(weekdaysArray,fWeekdays,count);
    fWeekdaysCount = count;
}

void
DateFormatSymbols::setShortWeekdays(const UnicodeString* shortWeekdaysArray, int32_t count)
{
    // delete the old list if we own it
    if (fShortWeekdays) delete[] fShortWeekdays;

    // we always own the new list, which we create here (we duplicate rather
    // than adopting the list passed in)
    fShortWeekdays = new UnicodeString[count];
    uprv_arrayCopy( shortWeekdaysArray,fShortWeekdays,count);
    fShortWeekdaysCount = count;
}

void
DateFormatSymbols::setAmPmStrings(const UnicodeString* amPmsArray, int32_t count)
{
    // delete the old list if we own it
    if (fAmPms) delete[] fAmPms;

    // we always own the new list, which we create here (we duplicate rather
    // than adopting the list passed in)
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
    fZoneStringsRowCount = rowCount;
    fZoneStringsColCount = columnCount;
    createZoneStrings((const UnicodeString**)strings);
}

//------------------------------------------------------

const UnicodeString&
DateFormatSymbols::getPatternChars(void)
{
    static const UnicodeString gPatternCharsStr(gPatternChars);
    return gPatternCharsStr;
}

//------------------------------------------------------

const UChar *
DateFormatSymbols::getPatternUChars(void)
{
    return gPatternChars;
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
DateFormatSymbols::initField(UnicodeString **field, int32_t& length, const ResourceBundle data, UErrorCode &status) {
    if (U_SUCCESS(status)) {
        length = data.getSize();
        *field = new UnicodeString[length];
        if (*field) {
            for(int32_t i = 0; i<length; i++) {
                *(*(field)+i) = data.getStringEx(i, status);
            }
        }
        else {
            length = 0;
            status = U_MEMORY_ALLOCATION_ERROR;
        }
    }
}

void
DateFormatSymbols::initField(UnicodeString **field, int32_t& length, const UChar *data, LastResortSize numStr, LastResortSize strLen, UErrorCode &status) {
    if (U_SUCCESS(status)) {
        length = numStr;
        *field = new UnicodeString[numStr];
        if (*field) {
            for(int32_t i = 0; i<length; i++) {
                *(*(field)+i) = data+(i*((int32_t)strLen));
            }
        }
        else {
            length = 0;
            status = U_MEMORY_ALLOCATION_ERROR;
        }
    }
}

void
DateFormatSymbols::initializeData(const Locale& locale, UErrorCode& status, UBool useLastResortData)
{
    int32_t i;
    if (U_FAILURE(status)) return;

    /**
     * Retrieve the string arrays we need from the resource bundle file.
     * We cast away const here, but that's okay; we won't delete any of
     * these.
     */
    ResourceBundle resource((char *)0, locale, status);

    if (U_FAILURE(status))
    {
        if (useLastResortData)
        {
            // Handle the case in which there is no resource data present.
            // We don't have to generate usable patterns in this situation;
            // we just need to produce something that will be semi-intelligible
            // in most locales.

            status = U_USING_FALLBACK_ERROR;

            initField(&fEras, fErasCount, (const UChar *)gLastResortEras, kEraNum, kEraLen, status);
            initField(&fMonths, fMonthsCount, (const UChar *)gLastResortMonthNames, kMonthNum, kMonthLen,  status);
            initField(&fShortMonths, fShortMonthsCount, (const UChar *)gLastResortMonthNames, kMonthNum, kMonthLen, status);
            initField(&fWeekdays, fWeekdaysCount, (const UChar *)gLastResortDayNames, kDayNum, kDayLen, status);
            initField(&fShortWeekdays, fShortWeekdaysCount, (const UChar *)gLastResortDayNames, kDayNum, kDayLen, status);
            initField(&fAmPms, fAmPmsCount, (const UChar *)gLastResortAmPmMarkers, kAmPmNum, kAmPmLen, status);

            fZoneStrings = new UnicodeString*[1];
            fZoneStringsRowCount = 1;
            initField(fZoneStrings, fZoneStringsColCount, (const UChar *)gLastResortZoneStrings, kZoneNum, kZoneLen, status);
            fLocalPatternChars = gPatternChars;
        }
        else {
            fEras = NULL;
            fMonths = NULL;
            fShortMonths = NULL;
            fWeekdays = NULL;
            fShortWeekdays = NULL;
            fAmPms = NULL;
            fZoneStrings = NULL;
        }
        return;
    }

    // if we make it to here, the resource data is cool, and we can get everything out
    // of it that we need except for the time-zone and localized-pattern data, which
    // are stoerd in a separate file
    initField(&fEras, fErasCount, resource.get(fgErasTag, status), status);
    initField(&fMonths, fMonthsCount, resource.get(fgMonthNamesTag, status), status);
    initField(&fShortMonths, fShortMonthsCount, resource.get(fgMonthAbbreviationsTag, status), status);
    initField(&fAmPms, fAmPmsCount, resource.get(fgAmPmMarkersTag, status), status);
    fLocalPatternChars = resource.getStringEx(fgLocalPatternCharsTag, status);

    ResourceBundle zoneArray = resource.get(fgZoneStringsTag, status);
    fZoneStringsRowCount = zoneArray.getSize();
    ResourceBundle zoneRow = zoneArray.get((int32_t)0, status);
    fZoneStringsColCount = zoneRow.getSize();
    fZoneStrings = new UnicodeString * [fZoneStringsRowCount];
    for(i = 0; i<fZoneStringsRowCount; i++) {
        *(fZoneStrings+i) = new UnicodeString[fZoneStringsColCount];
        zoneRow = zoneArray.get(i, status);
        for(int32_t j = 0; j<fZoneStringsColCount; j++) {
            fZoneStrings[i][j] = zoneRow.getStringEx(j, status);
        }
    }

    // {sfb} fixed to handle 1-based weekdays
    ResourceBundle weekdaysData = resource.get(fgDayNamesTag, status);
    fWeekdaysCount = weekdaysData.getSize();
    fWeekdays = new UnicodeString[fWeekdaysCount+1];
    fWeekdays[0] = UnicodeString();
    for(i = 0; i<fWeekdaysCount; i++) {
        fWeekdays[i+1] = weekdaysData.getStringEx(i, status);
    }

    ResourceBundle lsweekdaysData = resource.get(fgDayAbbreviationsTag, status);
    fShortWeekdaysCount = lsweekdaysData.getSize();
    fShortWeekdays = new UnicodeString[fShortWeekdaysCount+1];
    fShortWeekdays[0] = UnicodeString();
    for(i = 0; i<fShortWeekdaysCount; i++) {
        fShortWeekdays[i+1] = lsweekdaysData.getStringEx(i, status);
    }

    fWeekdaysCount = fShortWeekdaysCount = 8;
    
    // If the locale data does not include new pattern chars, use the defaults
    if (fLocalPatternChars.length() < PATTERN_CHARS_LEN) {
        fLocalPatternChars.append(&gPatternChars[fLocalPatternChars.length()]);
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
    int32_t result = _getZoneIndex(ID);
    if (result >= 0) {
        return result;
    }

    // Do a search through the equivalency group for the given ID
    int32_t n = TimeZone::countEquivalentIDs(ID);
    if (n > 1) {
        int32_t i;
        for (i=0; i<n; ++i) {
            UnicodeString equivID = TimeZone::getEquivalentID(ID, i);
            if (equivID != ID) {
                int32_t equivResult = _getZoneIndex(equivID);
                if (equivResult >= 0) {
                    return equivResult;
                }
            }
        }
    }

    return -1;
}

/**
 * Lookup the given ID.  Do NOT do an equivalency search.
 */
int32_t DateFormatSymbols::_getZoneIndex(const UnicodeString& ID) const
{
    // {sfb} kludge to support case-insensitive comparison
    UnicodeString lcaseID(ID);
    lcaseID.toLower();
    
    for(int32_t index = 0; index < fZoneStringsRowCount; index++) {
        UnicodeString lcase(fZoneStrings[index][0]);
        lcase.toLower();
        if (lcaseID == lcase) {
            return index;
        }
    }

    return -1;
}

//eof
