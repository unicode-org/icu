/*
*******************************************************************************
* Copyright (C) 1997-2004, International Business Machines Corporation and    *
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
 
#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/dtfmtsym.h"
#include "unicode/resbund.h"
#include "unicode/smpdtfmt.h"
#include "ucln_in.h"
#include "mutex.h"
#include "cmemory.h"
#include "cstring.h"
#include "locbased.h"
#include "gregoimp.h"
 
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
static const UChar gPatternChars[] = {
    // GyMdkHmsSEDFwWahKzYeugAZ
    0x47, 0x79, 0x4D, 0x64, 0x6B, 0x48, 0x6D, 0x73, 0x53, 0x45, 0x44,
    0x46, 0x77, 0x57, 0x61, 0x68, 0x4B, 0x7A, 0x59, 0x65, 0x75, 0x67,
    0x41, 0x5A, 0
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

U_NAMESPACE_BEGIN

UOBJECT_DEFINE_RTTI_IMPLEMENTATION(DateFormatSymbols)

/**
 * These are the tags we expect to see in normal resource bundle files associated
 * with a locale and calendar
 */
const char gErasTag[]="eras";
const char gMonthNamesTag[]="monthNames";
const char gDayNamesTag[]="dayNames";
const char gNamesWideTag[]="wide";
const char gNamesAbbrTag[]="abbreviated";
const char gAmPmMarkersTag[]="AmPmMarkers";

/**
 * These are the tags we expect to see in time zone data resource bundle files
 * associated with a locale.
 */
const char gZoneStringsTag[]="zoneStrings";
const char gLocalPatternCharsTag[]="localPatternChars";

/**
 * Jitterbug 2974: MSVC has a bug whereby new X[0] behaves badly.
 * Work around this.
 */
static inline UnicodeString* newUnicodeStringArray(size_t count) {
    return new UnicodeString[count ? count : 1];
}

//------------------------------------------------------

DateFormatSymbols::DateFormatSymbols(const Locale& locale,
                                     UErrorCode& status)
    : UObject()
{
  initializeData(locale, NULL,  status);
}

DateFormatSymbols::DateFormatSymbols(UErrorCode& status)
    : UObject()
{
  initializeData(Locale::getDefault(), NULL, status, TRUE);
}


DateFormatSymbols::DateFormatSymbols(const Locale& locale,
                                     const char *type,
                                     UErrorCode& status)
    : UObject()
{
  initializeData(locale, type,  status);
}

DateFormatSymbols::DateFormatSymbols(const char *type, UErrorCode& status)
    : UObject()
{
  initializeData(Locale::getDefault(), type, status, TRUE);
}

DateFormatSymbols::DateFormatSymbols(const DateFormatSymbols& other)
    : UObject(other)
{
    copyData(other);
}

void
DateFormatSymbols::assignArray(UnicodeString*& dstArray,
                               int32_t& dstCount,
                               const UnicodeString* srcArray,
                               int32_t srcCount)
{
    // assignArray() is only called by copyData(), which in turn implements the
    // copy constructor and the assignment operator.
    // All strings in a DateFormatSymbols object are created in one of the following
    // three ways that all allow to safely use UnicodeString::fastCopyFrom():
    // - readonly-aliases from resource bundles
    // - readonly-aliases or allocated strings from constants
    // - safely cloned strings (with owned buffers) from setXYZ() functions
    //
    // Note that this is true for as long as DateFormatSymbols can be constructed
    // only from a locale bundle or set via the cloning API,
    // *and* for as long as all the strings are in *private* fields, preventing
    // a subclass from creating these strings in an "unsafe" way (with respect to fastCopyFrom()).
    dstCount = srcCount;
    dstArray = newUnicodeStringArray(srcCount);
    if(dstArray != NULL) {
        int32_t i;
        for(i=0; i<srcCount; ++i) {
            dstArray[i].fastCopyFrom(srcArray[i]);
        }
    }
}

/**
 * Create a copy, in fZoneStrings, of the given zone strings array.  The
 * member variables fZoneStringsRowCount and fZoneStringsColCount should
 * be set already by the caller.
 */
void
DateFormatSymbols::createZoneStrings(const UnicodeString *const * otherStrings)
{
    int32_t row, col;

    fZoneStrings = (UnicodeString **)uprv_malloc(fZoneStringsRowCount * sizeof(UnicodeString *));
    for (row=0; row<fZoneStringsRowCount; ++row)
    {
        fZoneStrings[row] = newUnicodeStringArray(fZoneStringsColCount);
        for (col=0; col<fZoneStringsColCount; ++col) {
            // fastCopyFrom() - see assignArray comments
            fZoneStrings[row][col].fastCopyFrom(otherStrings[row][col]);
        }
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

    // fastCopyFrom() - see assignArray comments
    fLocalPatternChars.fastCopyFrom(other.fLocalPatternChars);
}

/**
 * Assignment operator.
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
        uprv_free(fZoneStrings);
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
    if (this == &other) {
        return TRUE;
    }
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
    fEras = newUnicodeStringArray(count);
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
    fMonths = newUnicodeStringArray(count);
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
    fShortMonths = newUnicodeStringArray(count);
    uprv_arrayCopy(shortMonthsArray,fShortMonths,  count);
    fShortMonthsCount = count;
}

void DateFormatSymbols::setWeekdays(const UnicodeString* weekdaysArray, int32_t count)
{
    // delete the old list if we own it
    if (fWeekdays) delete[] fWeekdays;

    // we always own the new list, which we create here (we duplicate rather
    // than adopting the list passed in)
    fWeekdays = newUnicodeStringArray(count);
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
    fShortWeekdays = newUnicodeStringArray(count);
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
    fAmPms = newUnicodeStringArray(count);
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

const UChar *
DateFormatSymbols::getPatternUChars(void)
{
    return gPatternChars;
}

//------------------------------------------------------

UnicodeString&
DateFormatSymbols::getLocalPatternChars(UnicodeString& result) const
{
    // fastCopyFrom() - see assignArray comments
    return result.fastCopyFrom(fLocalPatternChars);
}

//------------------------------------------------------

void
DateFormatSymbols::setLocalPatternChars(const UnicodeString& newLocalPatternChars)
{
    fLocalPatternChars = newLocalPatternChars;
}

//------------------------------------------------------

void
DateFormatSymbols::initField(UnicodeString **field, int32_t& length, const ResourceBundle &data, UErrorCode &status) {
    if (U_SUCCESS(status)) {
        length = data.getSize();
        *field = newUnicodeStringArray(length);
        if (*field) {
            for(int32_t i = 0; i<length; i++) {
                // fastCopyFrom() - see assignArray comments
                (*(field)+i)->fastCopyFrom(data.getStringEx(i, status));
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
        *field = newUnicodeStringArray((size_t)numStr);
        if (*field) {
            for(int32_t i = 0; i<length; i++) {
                // readonly aliases - all "data" strings are constant
                // -1 as length for variable-length strings (gLastResortDayNames[0] is empty)
                (*(field)+i)->setTo(TRUE, data+(i*((int32_t)strLen)), -1);
            }
        }
        else {
            length = 0;
            status = U_MEMORY_ALLOCATION_ERROR;
        }
    }
}

void
DateFormatSymbols::initializeData(const Locale& locale, const char *type, UErrorCode& status, UBool useLastResortData)
{
    int32_t i;

    /* In case something goes wrong, initialize all of the data to NULL. */
    fEras = NULL;
    fErasCount = 0;
    fMonths = NULL;
    fMonthsCount=0;
    fShortMonths = NULL;
    fShortMonthsCount=0;
    fWeekdays = NULL;
    fWeekdaysCount=0;
    fShortWeekdays = NULL;
    fShortWeekdaysCount=0;
    fAmPms = NULL;
    fAmPmsCount=0;
    fZoneStringsRowCount = 0;
    fZoneStringsColCount = 0;
    fZoneStrings = NULL;


    if (U_FAILURE(status)) return;

    /**
     * Retrieve the string arrays we need from the resource bundle file.
     * We cast away const here, but that's okay; we won't delete any of
     * these.
     */
    CalendarData calData(locale, type, status);
    ResourceBundle nonCalendarData((char*)0, locale, status);

    ResourceBundle data = calData.getBundleByKey(gErasTag, status); // load the first data item

    if (U_FAILURE(status))
    {
        if (useLastResortData)
        {
            // Handle the case in which there is no resource data present.
            // We don't have to generate usable patterns in this situation;
            // we just need to produce something that will be semi-intelligible
            // in most locales.

            status = U_USING_FALLBACK_WARNING;

            initField(&fEras, fErasCount, (const UChar *)gLastResortEras, kEraNum, kEraLen, status);
            initField(&fMonths, fMonthsCount, (const UChar *)gLastResortMonthNames, kMonthNum, kMonthLen,  status);
            initField(&fShortMonths, fShortMonthsCount, (const UChar *)gLastResortMonthNames, kMonthNum, kMonthLen, status);
            initField(&fWeekdays, fWeekdaysCount, (const UChar *)gLastResortDayNames, kDayNum, kDayLen, status);
            initField(&fShortWeekdays, fShortWeekdaysCount, (const UChar *)gLastResortDayNames, kDayNum, kDayLen, status);
            initField(&fAmPms, fAmPmsCount, (const UChar *)gLastResortAmPmMarkers, kAmPmNum, kAmPmLen, status);

            fZoneStrings = (UnicodeString **)uprv_malloc(sizeof(UnicodeString *));
            /* test for NULL */
            if (fZoneStrings == 0) {
                status = U_MEMORY_ALLOCATION_ERROR;
                return;
            }
            fZoneStringsRowCount = 1;
            initField(fZoneStrings, fZoneStringsColCount, (const UChar *)gLastResortZoneStrings, kZoneNum, kZoneLen, status);
            fLocalPatternChars = gPatternChars;
        }
        return;
    }

    // if we make it to here, the resource data is cool, and we can get everything out
    // of it that we need except for the time-zone and localized-pattern data, which
    // are stored in a separate file
    U_LOCALE_BASED(locBased, *this);
    locBased.setLocaleIDs(data.getLocale(ULOC_VALID_LOCALE, status).getName(),
                          data.getLocale(ULOC_ACTUAL_LOCALE, status).getName());
    initField(&fEras, fErasCount, data, status);
    initField(&fMonths, fMonthsCount, calData.getBundleByKey2(gMonthNamesTag, gNamesWideTag, status), status);
    initField(&fShortMonths, fShortMonthsCount, calData.getBundleByKey2(gMonthNamesTag, gNamesAbbrTag, status), status);
    initField(&fAmPms, fAmPmsCount, calData.getBundleByKey(gAmPmMarkersTag, status), status);

    // fastCopyFrom() - see assignArray comments
    fLocalPatternChars.fastCopyFrom(nonCalendarData.getStringEx(gLocalPatternCharsTag, status));

    ResourceBundle zoneArray(nonCalendarData.get(gZoneStringsTag, status));
    fZoneStringsRowCount = zoneArray.getSize();
    ResourceBundle zoneRow(zoneArray.get((int32_t)0, status));
    /* TODO: Fix the case where the zoneStrings is not a perfect square array of information. */
    fZoneStringsColCount = zoneRow.getSize();
    fZoneStrings = (UnicodeString **)uprv_malloc(fZoneStringsRowCount * sizeof(UnicodeString *));
    /* test for NULL */
    if (fZoneStrings == 0) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
    for(i = 0; i<fZoneStringsRowCount; i++) {
        *(fZoneStrings+i) = newUnicodeStringArray(fZoneStringsColCount);
        /* test for NULL */
        if ((*(fZoneStrings+i)) == 0) {
            status = U_MEMORY_ALLOCATION_ERROR;
            return;
        }
        zoneRow = zoneArray.get(i, status);
        for(int32_t j = 0; j<fZoneStringsColCount; j++) {
            // fastCopyFrom() - see assignArray comments
            fZoneStrings[i][j].fastCopyFrom(zoneRow.getStringEx(j, status));
        }
    }

    // {sfb} fixed to handle 1-based weekdays
    ResourceBundle weekdaysData(calData.getBundleByKey2(gDayNamesTag, gNamesWideTag, status));
    fWeekdaysCount = weekdaysData.getSize();
    fWeekdays = new UnicodeString[fWeekdaysCount+1];
    /* test for NULL */
    if (fWeekdays == 0) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
    // leave fWeekdays[0] empty
    for(i = 0; i<fWeekdaysCount; i++) {
        // fastCopyFrom() - see assignArray comments
        fWeekdays[i+1].fastCopyFrom(weekdaysData.getStringEx(i, status));
    }
    fWeekdaysCount++;

    ResourceBundle lsweekdaysData(calData.getBundleByKey2(gDayNamesTag, gNamesAbbrTag, status));
    fShortWeekdaysCount = lsweekdaysData.getSize();
    fShortWeekdays = new UnicodeString[fShortWeekdaysCount+1];
    /* test for NULL */
    if (fShortWeekdays == 0) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
    // leave fShortWeekdays[0] empty
    for(i = 0; i<fShortWeekdaysCount; i++) {
        // fastCopyFrom() - see assignArray comments
        fShortWeekdays[i+1].fastCopyFrom(lsweekdaysData.getStringEx(i, status));
    }
    fShortWeekdaysCount++;

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
    for(int32_t index = 0; index < fZoneStringsRowCount; index++) {
        if (0 == ID.caseCompare(fZoneStrings[index][0], 0)) {
            return index;
        }
    }

    return -1;
}

Locale 
DateFormatSymbols::getLocale(ULocDataLocaleType type, UErrorCode& status) const {
    U_LOCALE_BASED(locBased, *this);
    return locBased.getLocale(type, status);
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

//eof
