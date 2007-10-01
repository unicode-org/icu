/*
*******************************************************************************
* Copyright (C) 1997-2007, International Business Machines Corporation and    *
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
*   06/30/05    emmons      Added eraNames, narrow month/day, standalone context
*   10/12/05    emmons      Added setters for eraNames, month/day by width/context
*******************************************************************************
*/
#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING
#include "unicode/ustring.h"
#include "unicode/dtfmtsym.h"
#include "unicode/smpdtfmt.h"
#include "unicode/msgfmt.h"
#include "cpputils.h"
#include "ucln_in.h"
#include "umutex.h"
#include "cmemory.h"
#include "cstring.h"
#include "locbased.h"
#include "gregoimp.h"
#include "hash.h"
#include "uresimp.h" 

// *****************************************************************************
// class DateFormatSymbols
// *****************************************************************************
/**
 * These are static arrays we use only in the case where we have no
 * resource data.
 */

#define PATTERN_CHARS_LEN 30

/**
 * Unlocalized date-time pattern characters. For example: 'y', 'd', etc. All
 * locales use the same these unlocalized pattern characters.
 */
static const UChar gPatternChars[] = {
    // GyMdkHmsSEDFwWahKzYeugAZvcLQqV
    0x47, 0x79, 0x4D, 0x64, 0x6B, 0x48, 0x6D, 0x73, 0x53, 0x45,
    0x44, 0x46, 0x77, 0x57, 0x61, 0x68, 0x4B, 0x7A, 0x59, 0x65,
    0x75, 0x67, 0x41, 0x5A, 0x76, 0x63, 0x4c, 0x51, 0x71, 0x56, 0
};

/* length of an array */
#define ARRAY_LENGTH(array) (sizeof(array)/sizeof(array[0]))

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
    {0x0030, 0x0000}, /* "0" */
    {0x0031, 0x0000}, /* "1" */
    {0x0032, 0x0000}, /* "2" */
    {0x0033, 0x0000}, /* "3" */
    {0x0034, 0x0000}, /* "4" */
    {0x0035, 0x0000}, /* "5" */
    {0x0036, 0x0000}, /* "6" */
    {0x0037, 0x0000}  /* "7" */
};

// These are the quarter names and abbreviations of last resort.
static const UChar gLastResortQuarters[4][2] =
{
    {0x0031, 0x0000}, /* "1" */
    {0x0032, 0x0000}, /* "2" */
    {0x0033, 0x0000}, /* "3" */
    {0x0034, 0x0000}, /* "4" */
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
static const UChar gLastResortZoneStrings[7][4] =
{
    {0x0047, 0x004D, 0x0054, 0x0000}, /* "GMT" */
    {0x0047, 0x004D, 0x0054, 0x0000}, /* "GMT" */
    {0x0047, 0x004D, 0x0054, 0x0000}, /* "GMT" */
    {0x0047, 0x004D, 0x0054, 0x0000}, /* "GMT" */
    {0x0047, 0x004D, 0x0054, 0x0000}, /* "GMT" */
    {0x0047, 0x004D, 0x0054, 0x0000}, /* "GMT" */
    {0x0047, 0x004D, 0x0054, 0x0000}  /* "GMT" */
};

/* Sizes for the last resort string arrays */
typedef enum LastResortSize {
    kMonthNum = 13,
    kMonthLen = 3,

    kDayNum = 8,
    kDayLen = 2,

    kAmPmNum = 2,
    kAmPmLen = 3,

    kQuarterNum = 4,
    kQuarterLen = 2,

    kEraNum = 2,
    kEraLen = 3,

    kZoneNum = 5,
    kZoneLen = 4
} LastResortSize;

U_NAMESPACE_BEGIN

UOBJECT_DEFINE_RTTI_IMPLEMENTATION(DateFormatSymbols)

#define kSUPPLEMENTAL "supplementalData"

/**
 * These are the tags we expect to see in normal resource bundle files associated
 * with a locale and calendar
 */
static const char gErasTag[]="eras";
static const char gAbbreviatedTag[] = "abbreviated";
static const char gMonthNamesTag[]="monthNames";
static const char gDayNamesTag[]="dayNames";
static const char gNamesWideTag[]="wide";
static const char gNamesAbbrTag[]="abbreviated";
static const char gNamesNarrowTag[]="narrow";
static const char gNamesStandaloneTag[]="stand-alone";
static const char gAmPmMarkersTag[]="AmPmMarkers";
static const char gQuartersTag[]="quarters";
static const char gMaptimezonesTag[]="mapTimezones";
static const char gMetazonesTag[]="metazones";
static const char gTerritoryTag[]="territory";
static const char gCountriesTag[]="Countries";
static const char gZoneFormattingTag[]="zoneFormatting";
static const char gMultizoneTag[]="multizone";
static const char gRegionFormatTag[]="zoneStrings/regionFormat";
static const char gFallbackFormatTag[]="zoneStrings/fallbackFormat";

/**
 * These are the tags we expect to see in time zone data resource bundle files
 * associated with a locale.
 */
static const char gZoneStringsTag[]="zoneStrings";
static const char gLocalPatternCharsTag[]="localPatternChars";

static UMTX LOCK;

/*
 * Keep this variable in synch with max length of display strings
 */
#define ZID_KEY_MAX 128
#define UTZ_MAX_DISPLAY_STRINGS_LENGTH 7
#define UTZ_SHORT_GENERIC   "sg"
#define UTZ_SHORT_STANDARD  "ss"
#define UTZ_SHORT_DAYLIGHT  "sd"
#define UTZ_LONG_GENERIC    "lg"
#define UTZ_LONG_STANDARD   "ls"
#define UTZ_LONG_DAYLIGHT   "ld"
#define UTZ_EXEMPLAR_CITY   "ec"
#define UTZ_USES_METAZONE   "um"
#define UTZ_COMMONLY_USED   "cu"

/**
 * Jitterbug 2974: MSVC has a bug whereby new X[0] behaves badly.
 * Work around this.
 */
static inline UnicodeString* newUnicodeStringArray(size_t count) {
    return new UnicodeString[count ? count : 1];
}

U_CDECL_BEGIN
static void deleteUnicodeStringArray(void* obj) {
    delete[] (UnicodeString*)obj;
}
U_CDECL_END

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
    assignArray(fEraNames, fEraNamesCount, other.fEraNames, other.fEraNamesCount);
    assignArray(fMonths, fMonthsCount, other.fMonths, other.fMonthsCount);
    assignArray(fShortMonths, fShortMonthsCount, other.fShortMonths, other.fShortMonthsCount);
    assignArray(fNarrowMonths, fNarrowMonthsCount, other.fNarrowMonths, other.fNarrowMonthsCount);
    assignArray(fStandaloneMonths, fStandaloneMonthsCount, other.fStandaloneMonths, other.fStandaloneMonthsCount);
    assignArray(fStandaloneShortMonths, fStandaloneShortMonthsCount, other.fStandaloneShortMonths, other.fStandaloneShortMonthsCount);
    assignArray(fStandaloneNarrowMonths, fStandaloneNarrowMonthsCount, other.fStandaloneNarrowMonths, other.fStandaloneNarrowMonthsCount);
    assignArray(fWeekdays, fWeekdaysCount, other.fWeekdays, other.fWeekdaysCount);
    assignArray(fShortWeekdays, fShortWeekdaysCount, other.fShortWeekdays, other.fShortWeekdaysCount);
    assignArray(fNarrowWeekdays, fNarrowWeekdaysCount, other.fNarrowWeekdays, other.fNarrowWeekdaysCount);
    assignArray(fStandaloneWeekdays, fStandaloneWeekdaysCount, other.fStandaloneWeekdays, other.fStandaloneWeekdaysCount);
    assignArray(fStandaloneShortWeekdays, fStandaloneShortWeekdaysCount, other.fStandaloneShortWeekdays, other.fStandaloneShortWeekdaysCount);
    assignArray(fStandaloneNarrowWeekdays, fStandaloneNarrowWeekdaysCount, other.fStandaloneNarrowWeekdays, other.fStandaloneNarrowWeekdaysCount);
    assignArray(fAmPms, fAmPmsCount, other.fAmPms, other.fAmPmsCount);
    assignArray(fQuarters, fQuartersCount, other.fQuarters, other.fQuartersCount);
    assignArray(fShortQuarters, fShortQuartersCount, other.fShortQuarters, other.fShortQuartersCount);
    assignArray(fStandaloneQuarters, fStandaloneQuartersCount, other.fStandaloneQuarters, other.fStandaloneQuartersCount);
    assignArray(fStandaloneShortQuarters, fStandaloneShortQuartersCount, other.fStandaloneShortQuarters, other.fStandaloneShortQuartersCount);
    // the zoneStrings data is initialized on demand
    //fZoneStringsRowCount = other.fZoneStringsRowCount;
    //fZoneStringsColCount = other.fZoneStringsColCount;
    //createZoneStrings((const UnicodeString**)other.fZoneStrings);
    // initialize on demand
    fZoneStringsHash = NULL;
    fZoneIDEnumeration = NULL;
    fZoneStrings = NULL;
    fZoneStringsColCount = 0;
    fZoneStringsRowCount = 0;
    fResourceBundle = NULL;
    fCountry = other.fCountry;
    if(other.fZoneStringsHash!=NULL){
        fZoneStringsHash = createZoneStringsHash(other.fZoneStringsHash);
        fZoneIDEnumeration = other.fZoneIDEnumeration->clone();
    }else{
        UErrorCode status =U_ZERO_ERROR;
        fResourceBundle = ures_clone(other.fResourceBundle, &status);
        // TODO: what should be done in case of error?
    }

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
    if (fEras)                     delete[] fEras;
    if (fEraNames)                 delete[] fEraNames;
    if (fMonths)                   delete[] fMonths;
    if (fShortMonths)              delete[] fShortMonths;
    if (fNarrowMonths)             delete[] fNarrowMonths;
    if (fStandaloneMonths)         delete[] fStandaloneMonths;
    if (fStandaloneShortMonths)    delete[] fStandaloneShortMonths;
    if (fStandaloneNarrowMonths)   delete[] fStandaloneNarrowMonths;
    if (fWeekdays)                 delete[] fWeekdays;
    if (fShortWeekdays)            delete[] fShortWeekdays;
    if (fNarrowWeekdays)           delete[] fNarrowWeekdays;
    if (fStandaloneWeekdays)       delete[] fStandaloneWeekdays;
    if (fStandaloneShortWeekdays)  delete[] fStandaloneShortWeekdays;
    if (fStandaloneNarrowWeekdays) delete[] fStandaloneNarrowWeekdays;
    if (fAmPms)                    delete[] fAmPms;
    if (fQuarters)                 delete[] fQuarters;
    if (fShortQuarters)            delete[] fShortQuarters;
    if (fStandaloneQuarters)       delete[] fStandaloneQuarters;
    if (fStandaloneShortQuarters)  delete[] fStandaloneShortQuarters;

    disposeZoneStrings();
}

void DateFormatSymbols::disposeZoneStrings()
{
    if (fZoneStrings) {
        for (int32_t row=0; row<fZoneStringsRowCount; ++row)
            delete[] fZoneStrings[row];
        uprv_free(fZoneStrings);
    } 
    if(fZoneStringsHash){
        delete fZoneStringsHash;
        fZoneStringsHash =  NULL;
    }
    if(fZoneIDEnumeration){
        delete fZoneIDEnumeration; 
        fZoneIDEnumeration = NULL;
    }
    if (fResourceBundle){
        ures_close(fResourceBundle);
        fResourceBundle = NULL;
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
        fEraNamesCount == other.fEraNamesCount &&
        fMonthsCount == other.fMonthsCount &&
        fShortMonthsCount == other.fShortMonthsCount &&
        fNarrowMonthsCount == other.fNarrowMonthsCount &&
        fStandaloneMonthsCount == other.fStandaloneMonthsCount &&
        fStandaloneShortMonthsCount == other.fStandaloneShortMonthsCount &&
        fStandaloneNarrowMonthsCount == other.fStandaloneNarrowMonthsCount &&
        fWeekdaysCount == other.fWeekdaysCount &&
        fShortWeekdaysCount == other.fShortWeekdaysCount &&
        fNarrowWeekdaysCount == other.fNarrowWeekdaysCount &&
        fStandaloneWeekdaysCount == other.fStandaloneWeekdaysCount &&
        fStandaloneShortWeekdaysCount == other.fStandaloneShortWeekdaysCount &&
        fStandaloneNarrowWeekdaysCount == other.fStandaloneNarrowWeekdaysCount &&
        fAmPmsCount == other.fAmPmsCount &&
        fQuartersCount == other.fQuartersCount &&
        fShortQuartersCount == other.fShortQuartersCount &&
        fStandaloneQuartersCount == other.fStandaloneQuartersCount &&
        fStandaloneShortQuartersCount == other.fStandaloneShortQuartersCount)
    {
        // Now compare the arrays themselves
        if (arrayCompare(fEras, other.fEras, fErasCount) &&
            arrayCompare(fEraNames, other.fEraNames, fEraNamesCount) &&
            arrayCompare(fMonths, other.fMonths, fMonthsCount) &&
            arrayCompare(fShortMonths, other.fShortMonths, fShortMonthsCount) &&
            arrayCompare(fNarrowMonths, other.fNarrowMonths, fNarrowMonthsCount) &&
            arrayCompare(fStandaloneMonths, other.fStandaloneMonths, fStandaloneMonthsCount) &&
            arrayCompare(fStandaloneShortMonths, other.fStandaloneShortMonths, fStandaloneShortMonthsCount) &&
            arrayCompare(fStandaloneNarrowMonths, other.fStandaloneNarrowMonths, fStandaloneNarrowMonthsCount) &&
            arrayCompare(fWeekdays, other.fWeekdays, fWeekdaysCount) &&
            arrayCompare(fShortWeekdays, other.fShortWeekdays, fShortWeekdaysCount) &&
            arrayCompare(fNarrowWeekdays, other.fNarrowWeekdays, fNarrowWeekdaysCount) &&
            arrayCompare(fStandaloneWeekdays, other.fStandaloneWeekdays, fStandaloneWeekdaysCount) &&
            arrayCompare(fStandaloneShortWeekdays, other.fStandaloneShortWeekdays, fStandaloneShortWeekdaysCount) &&
            arrayCompare(fStandaloneNarrowWeekdays, other.fStandaloneNarrowWeekdays, fStandaloneNarrowWeekdaysCount) &&
            arrayCompare(fAmPms, other.fAmPms, fAmPmsCount) &&
            arrayCompare(fQuarters, other.fQuarters, fQuartersCount) &&
            arrayCompare(fShortQuarters, other.fShortQuarters, fShortQuartersCount) &&
            arrayCompare(fStandaloneQuarters, other.fStandaloneQuarters, fStandaloneQuartersCount) &&
            arrayCompare(fStandaloneShortQuarters, other.fStandaloneShortQuarters, fStandaloneShortQuartersCount))
        {
            
            if(fZoneStringsHash == NULL || other.fZoneStringsHash == NULL){
                // fZoneStringsHash is not initialized compare the resource bundles
                if(ures_equal(fResourceBundle, other.fResourceBundle)== FALSE){
                    return FALSE;
                }
            }else{
                if(fZoneStringsHash->equals(*other.fZoneStringsHash) == FALSE){
                    return FALSE;
                }
                // we always make sure that we update the enumeration when the hash is
                // updated. So we can be sure that once we compare the hashes  the 
                // enumerations are also equal
            }
            // since fZoneStrings data member is deprecated .. and may not be initialized
            // so don't compare them
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
DateFormatSymbols::getEraNames(int32_t &count) const
{
    count = fEraNamesCount;
    return fEraNames;
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
DateFormatSymbols::getMonths(int32_t &count, DtContextType context, DtWidthType width ) const
{
    UnicodeString *returnValue = NULL;

    switch (context) {
    case FORMAT :
        switch(width) {
        case WIDE :
            count = fMonthsCount;
            returnValue = fMonths;
            break;
        case ABBREVIATED :
            count = fShortMonthsCount;
            returnValue = fShortMonths;
            break;
        case NARROW :
            count = fNarrowMonthsCount;
            returnValue = fNarrowMonths;
            break;
        case DT_WIDTH_COUNT :
            break;
        }
        break;
    case STANDALONE :
        switch(width) {
        case WIDE :
            count = fStandaloneMonthsCount;
            returnValue = fStandaloneMonths;
            break;
        case ABBREVIATED :
            count = fStandaloneShortMonthsCount;
            returnValue = fStandaloneShortMonths;
            break;
        case NARROW :
            count = fStandaloneNarrowMonthsCount;
            returnValue = fStandaloneNarrowMonths;
            break;
        case DT_WIDTH_COUNT :
            break;
        }
        break;
    case DT_CONTEXT_COUNT :
        break;
    }
    return returnValue;
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
DateFormatSymbols::getWeekdays(int32_t &count, DtContextType context, DtWidthType width) const
{
    UnicodeString *returnValue = NULL;
    switch (context) {
    case FORMAT :
        switch(width) {
            case WIDE :
                count = fWeekdaysCount;
                returnValue = fWeekdays;
                break;
            case ABBREVIATED :
                count = fShortWeekdaysCount;
                returnValue = fShortWeekdays;
                break;
            case NARROW :
                count = fNarrowWeekdaysCount;
                returnValue = fNarrowWeekdays;
                break;
            case DT_WIDTH_COUNT :
                break;
        }
        break;
    case STANDALONE :
        switch(width) {
            case WIDE :
                count = fStandaloneWeekdaysCount;
                returnValue = fStandaloneWeekdays;
                break;
            case ABBREVIATED :
                count = fStandaloneShortWeekdaysCount;
                returnValue = fStandaloneShortWeekdays;
                break;
            case NARROW :
                count = fStandaloneNarrowWeekdaysCount;
                returnValue = fStandaloneNarrowWeekdays;
                break;
            case DT_WIDTH_COUNT :
                break;
        }
        break;
    case DT_CONTEXT_COUNT :
        break;
    }
    return returnValue;
}

const UnicodeString*
DateFormatSymbols::getQuarters(int32_t &count, DtContextType context, DtWidthType width ) const
{
    UnicodeString *returnValue = NULL;

    switch (context) {
    case FORMAT :
        switch(width) {
        case WIDE :
            count = fQuartersCount;
            returnValue = fQuarters;
            break;
        case ABBREVIATED :
            count = fShortQuartersCount;
            returnValue = fShortQuarters;
            break;
        case NARROW :
            count = 0;
            returnValue = NULL;
            break;
        case DT_WIDTH_COUNT :
            break;
        }
        break;
    case STANDALONE :
        switch(width) {
        case WIDE :
            count = fStandaloneQuartersCount;
            returnValue = fStandaloneQuarters;
            break;
        case ABBREVIATED :
            count = fStandaloneShortQuartersCount;
            returnValue = fStandaloneShortQuarters;
            break;
        case NARROW :
            count = 0;
            returnValue = NULL;
            break;
        case DT_WIDTH_COUNT :
            break;
        }
        break;
    case DT_CONTEXT_COUNT :
        break;
    }
    return returnValue;
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
    if (fEras)
        delete[] fEras;

    // we always own the new list, which we create here (we duplicate rather
    // than adopting the list passed in)
    fEras = newUnicodeStringArray(count);
    uprv_arrayCopy(erasArray,fEras,  count);
    fErasCount = count;
}

void
DateFormatSymbols::setEraNames(const UnicodeString* eraNamesArray, int32_t count)
{
    // delete the old list if we own it
    if (fEraNames)
        delete[] fEraNames;

    // we always own the new list, which we create here (we duplicate rather
    // than adopting the list passed in)
    fEraNames = newUnicodeStringArray(count);
    uprv_arrayCopy(eraNamesArray,fEraNames,  count);
    fEraNamesCount = count;
}

void
DateFormatSymbols::setMonths(const UnicodeString* monthsArray, int32_t count)
{
    // delete the old list if we own it
    if (fMonths)
        delete[] fMonths;

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
    if (fShortMonths)
        delete[] fShortMonths;

    // we always own the new list, which we create here (we duplicate rather
    // than adopting the list passed in)
    fShortMonths = newUnicodeStringArray(count);
    uprv_arrayCopy(shortMonthsArray,fShortMonths,  count);
    fShortMonthsCount = count;
}

void
DateFormatSymbols::setMonths(const UnicodeString* monthsArray, int32_t count, DtContextType context, DtWidthType width)
{
    // delete the old list if we own it
    // we always own the new list, which we create here (we duplicate rather
    // than adopting the list passed in)

    switch (context) {
    case FORMAT :
        switch (width) {
        case WIDE :
            if (fMonths)
                delete[] fMonths;
            fMonths = newUnicodeStringArray(count);
            uprv_arrayCopy( monthsArray,fMonths,count);
            fMonthsCount = count;
            break;
        case ABBREVIATED :
            if (fShortMonths)
                delete[] fShortMonths;
            fShortMonths = newUnicodeStringArray(count);
            uprv_arrayCopy( monthsArray,fShortMonths,count);
            fShortMonthsCount = count;
            break;
        case NARROW :
            if (fNarrowMonths)
                delete[] fNarrowMonths;
            fNarrowMonths = newUnicodeStringArray(count);
            uprv_arrayCopy( monthsArray,fNarrowMonths,count);
            fNarrowMonthsCount = count;
            break; 
        case DT_WIDTH_COUNT :
            break;
        }
        break;
    case STANDALONE :
        switch (width) {
        case WIDE :
            if (fStandaloneMonths)
                delete[] fStandaloneMonths;
            fStandaloneMonths = newUnicodeStringArray(count);
            uprv_arrayCopy( monthsArray,fStandaloneMonths,count);
            fStandaloneMonthsCount = count;
            break;
        case ABBREVIATED :
            if (fStandaloneShortMonths)
                delete[] fStandaloneShortMonths;
            fStandaloneShortMonths = newUnicodeStringArray(count);
            uprv_arrayCopy( monthsArray,fStandaloneShortMonths,count);
            fStandaloneShortMonthsCount = count;
            break;
        case NARROW :
           if (fStandaloneNarrowMonths)
                delete[] fStandaloneNarrowMonths;
            fStandaloneNarrowMonths = newUnicodeStringArray(count);
            uprv_arrayCopy( monthsArray,fStandaloneNarrowMonths,count);
            fStandaloneNarrowMonthsCount = count;
            break; 
        case DT_WIDTH_COUNT :
            break;
        }
        break;
    case DT_CONTEXT_COUNT :
        break;
    }
}

void DateFormatSymbols::setWeekdays(const UnicodeString* weekdaysArray, int32_t count)
{
    // delete the old list if we own it
    if (fWeekdays)
        delete[] fWeekdays;

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
    if (fShortWeekdays)
        delete[] fShortWeekdays;

    // we always own the new list, which we create here (we duplicate rather
    // than adopting the list passed in)
    fShortWeekdays = newUnicodeStringArray(count);
    uprv_arrayCopy(shortWeekdaysArray, fShortWeekdays, count);
    fShortWeekdaysCount = count;
}

void
DateFormatSymbols::setWeekdays(const UnicodeString* weekdaysArray, int32_t count, DtContextType context, DtWidthType width)
{
    // delete the old list if we own it
    // we always own the new list, which we create here (we duplicate rather
    // than adopting the list passed in)

    switch (context) {
    case FORMAT :
        switch (width) {
        case WIDE :
            if (fWeekdays)
                delete[] fWeekdays;
            fWeekdays = newUnicodeStringArray(count);
            uprv_arrayCopy(weekdaysArray, fWeekdays, count);
            fWeekdaysCount = count;
            break;
        case ABBREVIATED :
            if (fShortWeekdays)
                delete[] fShortWeekdays;
            fShortWeekdays = newUnicodeStringArray(count);
            uprv_arrayCopy(weekdaysArray, fShortWeekdays, count);
            fShortWeekdaysCount = count;
            break;
        case NARROW :
            if (fNarrowWeekdays)
                delete[] fNarrowWeekdays;
            fNarrowWeekdays = newUnicodeStringArray(count);
            uprv_arrayCopy(weekdaysArray, fNarrowWeekdays, count);
            fNarrowWeekdaysCount = count;
            break; 
        case DT_WIDTH_COUNT :
            break;
        }
        break;
    case STANDALONE :
        switch (width) {
        case WIDE :
            if (fStandaloneWeekdays)
                delete[] fStandaloneWeekdays;
            fStandaloneWeekdays = newUnicodeStringArray(count);
            uprv_arrayCopy(weekdaysArray, fStandaloneWeekdays, count);
            fStandaloneWeekdaysCount = count;
            break;
        case ABBREVIATED :
            if (fStandaloneShortWeekdays)
                delete[] fStandaloneShortWeekdays;
            fStandaloneShortWeekdays = newUnicodeStringArray(count);
            uprv_arrayCopy(weekdaysArray, fStandaloneShortWeekdays, count);
            fStandaloneShortWeekdaysCount = count;
            break;
        case NARROW :
            if (fStandaloneNarrowWeekdays)
                delete[] fStandaloneNarrowWeekdays;
            fStandaloneNarrowWeekdays = newUnicodeStringArray(count);
            uprv_arrayCopy(weekdaysArray, fStandaloneNarrowWeekdays, count);
            fStandaloneNarrowWeekdaysCount = count;
            break; 
        case DT_WIDTH_COUNT :
            break;
        }
        break;
    case DT_CONTEXT_COUNT :
        break;
    }
}

void
DateFormatSymbols::setQuarters(const UnicodeString* quartersArray, int32_t count, DtContextType context, DtWidthType width)
{
    // delete the old list if we own it
    // we always own the new list, which we create here (we duplicate rather
    // than adopting the list passed in)

    switch (context) {
    case FORMAT :
        switch (width) {
        case WIDE :
            if (fQuarters)
                delete[] fQuarters;
            fQuarters = newUnicodeStringArray(count);
            uprv_arrayCopy( quartersArray,fQuarters,count);
            fQuartersCount = count;
            break;
        case ABBREVIATED :
            if (fShortQuarters)
                delete[] fShortQuarters;
            fShortQuarters = newUnicodeStringArray(count);
            uprv_arrayCopy( quartersArray,fShortQuarters,count);
            fShortQuartersCount = count;
            break;
        case NARROW :
        /*
            if (fNarrowQuarters)
                delete[] fNarrowQuarters;
            fNarrowQuarters = newUnicodeStringArray(count);
            uprv_arrayCopy( quartersArray,fNarrowQuarters,count);
            fNarrowQuartersCount = count;
        */
            break; 
        case DT_WIDTH_COUNT :
            break;
        }
        break;
    case STANDALONE :
        switch (width) {
        case WIDE :
            if (fStandaloneQuarters)
                delete[] fStandaloneQuarters;
            fStandaloneQuarters = newUnicodeStringArray(count);
            uprv_arrayCopy( quartersArray,fStandaloneQuarters,count);
            fStandaloneQuartersCount = count;
            break;
        case ABBREVIATED :
            if (fStandaloneShortQuarters)
                delete[] fStandaloneShortQuarters;
            fStandaloneShortQuarters = newUnicodeStringArray(count);
            uprv_arrayCopy( quartersArray,fStandaloneShortQuarters,count);
            fStandaloneShortQuartersCount = count;
            break;
        case NARROW :
        /*
           if (fStandaloneNarrowQuarters)
                delete[] fStandaloneNarrowQuarters;
            fStandaloneNarrowQuarters = newUnicodeStringArray(count);
            uprv_arrayCopy( quartersArray,fStandaloneNarrowQuarters,count);
            fStandaloneNarrowQuartersCount = count;
        */
            break; 
        case DT_WIDTH_COUNT :
            break;
        }
        break;
    case DT_CONTEXT_COUNT :
        break;
    }
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
    umtx_lock(&LOCK);
    UErrorCode status = U_ZERO_ERROR;
    if(fZoneStrings==NULL){
        // cast away const to get around the problem for lazy initialization
        ((DateFormatSymbols*)this)->initZoneStringsArray(status);
    }
    rowCount = fZoneStringsRowCount;
    columnCount = fZoneStringsColCount;
    umtx_unlock(&LOCK);
    if(U_FAILURE(status)){
        rowCount = 0;
        columnCount = 0;
        return NULL;
    }
    return (const UnicodeString**)fZoneStrings; // Compiler requires cast
}

void
DateFormatSymbols::setZoneStrings(const UnicodeString* const *strings, int32_t rowCount, int32_t columnCount)
{
    // since deleting a 2-d array is a pain in the butt, we offload that task to
    // a separate function
    disposeZoneStrings();
    UErrorCode status = U_ZERO_ERROR;
    // we always own the new list, which we create here (we duplicate rather
    // than adopting the list passed in)
    fZoneStringsRowCount = rowCount;
    fZoneStringsColCount = columnCount;
    createZoneStrings((const UnicodeString**)strings);
    initZoneStrings((const UnicodeString**)strings, rowCount,columnCount, status);
}

//------------------------------------------------------

const UChar * U_EXPORT2
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

static void
initField(UnicodeString **field, int32_t& length, const UResourceBundle *data, UErrorCode &status) {
    if (U_SUCCESS(status)) {
        int32_t strLen = 0;
        length = ures_getSize(data);
        *field = newUnicodeStringArray(length);
        if (*field) {
            for(int32_t i = 0; i<length; i++) {
                const UChar *resStr = ures_getStringByIndex(data, i, &strLen, &status);
                // setTo() - see assignArray comments
                (*(field)+i)->setTo(TRUE, resStr, strLen);
            }
        }
        else {
            length = 0;
            status = U_MEMORY_ALLOCATION_ERROR;
        }
    }
}

static void
initField(UnicodeString **field, int32_t& length, const UChar *data, LastResortSize numStr, LastResortSize strLen, UErrorCode &status) {
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
    int32_t len = 0;
    const UChar *resStr;
    /* In case something goes wrong, initialize all of the data to NULL. */
    fEras = NULL;
    fErasCount = 0;
    fEraNames = NULL;
    fEraNamesCount = 0;
    fMonths = NULL;
    fMonthsCount=0;
    fShortMonths = NULL;
    fShortMonthsCount=0;
    fNarrowMonths = NULL;
    fNarrowMonthsCount=0;
    fStandaloneMonths = NULL;
    fStandaloneMonthsCount=0;
    fStandaloneShortMonths = NULL;
    fStandaloneShortMonthsCount=0;
    fStandaloneNarrowMonths = NULL;
    fStandaloneNarrowMonthsCount=0;
    fWeekdays = NULL;
    fWeekdaysCount=0;
    fShortWeekdays = NULL;
    fShortWeekdaysCount=0;
    fNarrowWeekdays = NULL;
    fNarrowWeekdaysCount=0;
    fStandaloneWeekdays = NULL;
    fStandaloneWeekdaysCount=0;
    fStandaloneShortWeekdays = NULL;
    fStandaloneShortWeekdaysCount=0;
    fStandaloneNarrowWeekdays = NULL;
    fStandaloneNarrowWeekdaysCount=0;
    fAmPms = NULL;
    fAmPmsCount=0;
    fQuarters = NULL;
    fQuartersCount = 0;
    fShortQuarters = NULL;
    fShortQuartersCount = 0;
    fStandaloneQuarters = NULL;
    fStandaloneQuartersCount = 0;
    fStandaloneShortQuarters = NULL;
    fStandaloneShortQuartersCount = 0;
    fZoneStringsRowCount = 0;
    fZoneStringsColCount = 0;
    fZoneStrings = NULL;
    fZoneStringsHash = NULL;
    fZoneIDEnumeration = NULL;
    fResourceBundle   = NULL;
    fCountry = NULL;
    
      
    if (U_FAILURE(status)) return;

    /**
     * Retrieve the string arrays we need from the resource bundle file.
     * We cast away const here, but that's okay; we won't delete any of
     * these.
     */
    CalendarData calData(locale, type, status);
    fResourceBundle = ures_open(NULL, locale.getName(), &status);
    fCountry = locale.getCountry();

    // load the first data item
    UResourceBundle *erasMain = calData.getByKey(gErasTag, status);
    UResourceBundle *eras = ures_getByKeyWithFallback(erasMain, gAbbreviatedTag, NULL, &status);
    UErrorCode oldStatus = status;
    UResourceBundle *eraNames = ures_getByKeyWithFallback(erasMain, gNamesWideTag, NULL, &status);
    if ( status == U_MISSING_RESOURCE_ERROR ) { // Workaround because eras/wide was omitted from CLDR 1.3
       status = oldStatus;
       eraNames = ures_getByKeyWithFallback(erasMain, gAbbreviatedTag, NULL, &status);
    }

    UResourceBundle *lsweekdaysData = NULL; // Data closed by calData
    UResourceBundle *weekdaysData = NULL; // Data closed by calData
    UResourceBundle *narrowWeekdaysData = NULL; // Data closed by calData
    UResourceBundle *standaloneWeekdaysData = NULL; // Data closed by calData
    UResourceBundle *standaloneShortWeekdaysData = NULL; // Data closed by calData
    UResourceBundle *standaloneNarrowWeekdaysData = NULL; // Data closed by calData

    U_LOCALE_BASED(locBased, *this);
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
            initField(&fEraNames, fEraNamesCount, (const UChar *)gLastResortEras, kEraNum, kEraLen, status);
            initField(&fMonths, fMonthsCount, (const UChar *)gLastResortMonthNames, kMonthNum, kMonthLen,  status);
            initField(&fShortMonths, fShortMonthsCount, (const UChar *)gLastResortMonthNames, kMonthNum, kMonthLen, status);
            initField(&fNarrowMonths, fNarrowMonthsCount, (const UChar *)gLastResortMonthNames, kMonthNum, kMonthLen, status);
            initField(&fStandaloneMonths, fStandaloneMonthsCount, (const UChar *)gLastResortMonthNames, kMonthNum, kMonthLen,  status);
            initField(&fStandaloneShortMonths, fStandaloneShortMonthsCount, (const UChar *)gLastResortMonthNames, kMonthNum, kMonthLen, status);
            initField(&fStandaloneNarrowMonths, fStandaloneNarrowMonthsCount, (const UChar *)gLastResortMonthNames, kMonthNum, kMonthLen, status);
            initField(&fWeekdays, fWeekdaysCount, (const UChar *)gLastResortDayNames, kDayNum, kDayLen, status);
            initField(&fShortWeekdays, fShortWeekdaysCount, (const UChar *)gLastResortDayNames, kDayNum, kDayLen, status);
            initField(&fNarrowWeekdays, fNarrowWeekdaysCount, (const UChar *)gLastResortDayNames, kDayNum, kDayLen, status);
            initField(&fStandaloneWeekdays, fStandaloneWeekdaysCount, (const UChar *)gLastResortDayNames, kDayNum, kDayLen, status);
            initField(&fStandaloneShortWeekdays, fStandaloneShortWeekdaysCount, (const UChar *)gLastResortDayNames, kDayNum, kDayLen, status);
            initField(&fStandaloneNarrowWeekdays, fStandaloneNarrowWeekdaysCount, (const UChar *)gLastResortDayNames, kDayNum, kDayLen, status);
            initField(&fAmPms, fAmPmsCount, (const UChar *)gLastResortAmPmMarkers, kAmPmNum, kAmPmLen, status);
            initField(&fQuarters, fQuartersCount, (const UChar *)gLastResortQuarters, kQuarterNum, kQuarterLen, status);
            initField(&fShortQuarters, fShortQuartersCount, (const UChar *)gLastResortQuarters, kQuarterNum, kQuarterLen, status);
            initField(&fStandaloneQuarters, fStandaloneQuartersCount, (const UChar *)gLastResortQuarters, kQuarterNum, kQuarterLen, status);
            initField(&fStandaloneShortQuarters, fStandaloneShortQuartersCount, (const UChar *)gLastResortQuarters, kQuarterNum, kQuarterLen, status);
            fLocalPatternChars.setTo(TRUE, gPatternChars, PATTERN_CHARS_LEN);
        }
        goto cleanup;
    }

    // if we make it to here, the resource data is cool, and we can get everything out
    // of it that we need except for the time-zone and localized-pattern data, which
    // are stored in a separate file
    locBased.setLocaleIDs(ures_getLocaleByType(eras, ULOC_VALID_LOCALE, &status),
                          ures_getLocaleByType(eras, ULOC_ACTUAL_LOCALE, &status));

    initField(&fEras, fErasCount, eras, status);
    initField(&fEraNames, fEraNamesCount, eraNames, status);

    initField(&fMonths, fMonthsCount, calData.getByKey2(gMonthNamesTag, gNamesWideTag, status), status);
    initField(&fShortMonths, fShortMonthsCount, calData.getByKey2(gMonthNamesTag, gNamesAbbrTag, status), status);

    initField(&fNarrowMonths, fNarrowMonthsCount, calData.getByKey2(gMonthNamesTag, gNamesNarrowTag, status), status);
    if(status == U_MISSING_RESOURCE_ERROR) {
        status = U_ZERO_ERROR;
        initField(&fNarrowMonths, fNarrowMonthsCount, calData.getByKey3(gMonthNamesTag, gNamesStandaloneTag, gNamesNarrowTag, status), status);
    }
    if ( status == U_MISSING_RESOURCE_ERROR ) { /* If format/narrow not available, use format/abbreviated */
       status = U_ZERO_ERROR;
       initField(&fNarrowMonths, fNarrowMonthsCount, calData.getByKey2(gMonthNamesTag, gNamesAbbrTag, status), status);
    }

    initField(&fStandaloneMonths, fStandaloneMonthsCount, calData.getByKey3(gMonthNamesTag, gNamesStandaloneTag, gNamesWideTag, status), status);
    if ( status == U_MISSING_RESOURCE_ERROR ) { /* If standalone/wide not available, use format/wide */
       status = U_ZERO_ERROR;
       initField(&fStandaloneMonths, fStandaloneMonthsCount, calData.getByKey2(gMonthNamesTag, gNamesWideTag, status), status);
    }
    initField(&fStandaloneShortMonths, fStandaloneShortMonthsCount, calData.getByKey3(gMonthNamesTag, gNamesStandaloneTag, gNamesAbbrTag, status), status);
    if ( status == U_MISSING_RESOURCE_ERROR ) { /* If standalone/abbreviated not available, use format/abbreviated */
       status = U_ZERO_ERROR;
       initField(&fStandaloneShortMonths, fStandaloneShortMonthsCount, calData.getByKey2(gMonthNamesTag, gNamesAbbrTag, status), status);
    }
    initField(&fStandaloneNarrowMonths, fStandaloneNarrowMonthsCount, calData.getByKey3(gMonthNamesTag, gNamesStandaloneTag, gNamesNarrowTag, status), status);
    if ( status == U_MISSING_RESOURCE_ERROR ) { /* if standalone/narrow not availabe, try format/narrow */
       status = U_ZERO_ERROR;
       initField(&fStandaloneNarrowMonths, fStandaloneNarrowMonthsCount, calData.getByKey2(gMonthNamesTag, gNamesNarrowTag, status), status);
       if ( status == U_MISSING_RESOURCE_ERROR ) { /* if still not there, use format/abbreviated */
          status = U_ZERO_ERROR;
          initField(&fStandaloneNarrowMonths, fStandaloneNarrowMonthsCount, calData.getByKey2(gMonthNamesTag, gNamesAbbrTag, status), status);
       }
    }
    initField(&fAmPms, fAmPmsCount, calData.getByKey(gAmPmMarkersTag, status), status);

    initField(&fQuarters, fQuartersCount, calData.getByKey2(gQuartersTag, gNamesWideTag, status), status);
    initField(&fShortQuarters, fShortQuartersCount, calData.getByKey2(gQuartersTag, gNamesAbbrTag, status), status);

    initField(&fStandaloneQuarters, fStandaloneQuartersCount, calData.getByKey3(gQuartersTag, gNamesStandaloneTag, gNamesWideTag, status), status);
    if(status == U_MISSING_RESOURCE_ERROR) {
        status = U_ZERO_ERROR;
        initField(&fStandaloneQuarters, fStandaloneQuartersCount, calData.getByKey2(gQuartersTag, gNamesWideTag, status), status);
    }

    initField(&fStandaloneShortQuarters, fStandaloneShortQuartersCount, calData.getByKey3(gQuartersTag, gNamesStandaloneTag, gNamesAbbrTag, status), status);
    if(status == U_MISSING_RESOURCE_ERROR) {
        status = U_ZERO_ERROR;
        initField(&fStandaloneShortQuarters, fStandaloneShortQuartersCount, calData.getByKey2(gQuartersTag, gNamesAbbrTag, status), status);
    }
    // ICU 3.8 or later version no longer uses localized date-time pattern characters by default (ticket#5597)
    /*
    // fastCopyFrom()/setTo() - see assignArray comments
    resStr = ures_getStringByKey(fResourceBundle, gLocalPatternCharsTag, &len, &status);
    fLocalPatternChars.setTo(TRUE, resStr, len);
    // If the locale data does not include new pattern chars, use the defaults
    // TODO: Consider making this an error, since this may add conflicting characters.
    if (len < PATTERN_CHARS_LEN) {
        fLocalPatternChars.append(UnicodeString(TRUE, &gPatternChars[len], PATTERN_CHARS_LEN-len));
    }
    */
    fLocalPatternChars.setTo(TRUE, gPatternChars, PATTERN_CHARS_LEN);

    // {sfb} fixed to handle 1-based weekdays
    weekdaysData = calData.getByKey2(gDayNamesTag, gNamesWideTag, status);
    fWeekdaysCount = ures_getSize(weekdaysData);
    fWeekdays = new UnicodeString[fWeekdaysCount+1];
    /* pin the blame on system. If we cannot get a chunk of memory .. the system is dying!*/
    if (fWeekdays == NULL) {
        status = U_MEMORY_ALLOCATION_ERROR;
        goto cleanup;
    }
    // leave fWeekdays[0] empty
    for(i = 0; i<fWeekdaysCount; i++) {
        resStr = ures_getStringByIndex(weekdaysData, i, &len, &status);
        // setTo() - see assignArray comments
        fWeekdays[i+1].setTo(TRUE, resStr, len);
    }
    fWeekdaysCount++;

    lsweekdaysData = calData.getByKey2(gDayNamesTag, gNamesAbbrTag, status);
    fShortWeekdaysCount = ures_getSize(lsweekdaysData);
    fShortWeekdays = new UnicodeString[fShortWeekdaysCount+1];
    /* test for NULL */
    if (fShortWeekdays == 0) {
        status = U_MEMORY_ALLOCATION_ERROR;
        goto cleanup;
    }
    // leave fShortWeekdays[0] empty
    for(i = 0; i<fShortWeekdaysCount; i++) {
        resStr = ures_getStringByIndex(lsweekdaysData, i, &len, &status);
        // setTo() - see assignArray comments
        fShortWeekdays[i+1].setTo(TRUE, resStr, len);
    }
    fShortWeekdaysCount++;

    narrowWeekdaysData = calData.getByKey2(gDayNamesTag, gNamesNarrowTag, status);
    if(status == U_MISSING_RESOURCE_ERROR) {
        status = U_ZERO_ERROR;
        narrowWeekdaysData = calData.getByKey3(gDayNamesTag, gNamesStandaloneTag, gNamesNarrowTag, status);
    }
    if ( status == U_MISSING_RESOURCE_ERROR ) {
       status = U_ZERO_ERROR;
       narrowWeekdaysData = calData.getByKey2(gDayNamesTag, gNamesAbbrTag, status);
    }
    fNarrowWeekdaysCount = ures_getSize(narrowWeekdaysData);
    fNarrowWeekdays = new UnicodeString[fNarrowWeekdaysCount+1];
    /* test for NULL */
    if (fNarrowWeekdays == 0) {
        status = U_MEMORY_ALLOCATION_ERROR;
        goto cleanup;
    }
    // leave fNarrowWeekdays[0] empty
    for(i = 0; i<fNarrowWeekdaysCount; i++) {
        resStr = ures_getStringByIndex(narrowWeekdaysData, i, &len, &status);
        // setTo() - see assignArray comments
        fNarrowWeekdays[i+1].setTo(TRUE, resStr, len);
    }
    fNarrowWeekdaysCount++;

    standaloneWeekdaysData = calData.getByKey3(gDayNamesTag, gNamesStandaloneTag, gNamesWideTag, status);
    if ( status == U_MISSING_RESOURCE_ERROR ) {
       status = U_ZERO_ERROR;
       standaloneWeekdaysData = calData.getByKey2(gDayNamesTag, gNamesWideTag, status);
    }
    fStandaloneWeekdaysCount = ures_getSize(standaloneWeekdaysData);
    fStandaloneWeekdays = new UnicodeString[fStandaloneWeekdaysCount+1];
    /* test for NULL */
    if (fStandaloneWeekdays == 0) {
        status = U_MEMORY_ALLOCATION_ERROR;
        goto cleanup;
    }
    // leave fStandaloneWeekdays[0] empty
    for(i = 0; i<fStandaloneWeekdaysCount; i++) {
        resStr = ures_getStringByIndex(standaloneWeekdaysData, i, &len, &status);
        // setTo() - see assignArray comments
        fStandaloneWeekdays[i+1].setTo(TRUE, resStr, len);
    }
    fStandaloneWeekdaysCount++;

    standaloneShortWeekdaysData = calData.getByKey3(gDayNamesTag, gNamesStandaloneTag, gNamesAbbrTag, status);
    if ( status == U_MISSING_RESOURCE_ERROR ) {
       status = U_ZERO_ERROR;
       standaloneShortWeekdaysData = calData.getByKey2(gDayNamesTag, gNamesAbbrTag, status);
    }
    fStandaloneShortWeekdaysCount = ures_getSize(standaloneShortWeekdaysData);
    fStandaloneShortWeekdays = new UnicodeString[fStandaloneShortWeekdaysCount+1];
    /* test for NULL */
    if (fStandaloneShortWeekdays == 0) {
        status = U_MEMORY_ALLOCATION_ERROR;
        goto cleanup;
    }
    // leave fStandaloneShortWeekdays[0] empty
    for(i = 0; i<fStandaloneShortWeekdaysCount; i++) {
        resStr = ures_getStringByIndex(standaloneShortWeekdaysData, i, &len, &status);
        // setTo() - see assignArray comments
        fStandaloneShortWeekdays[i+1].setTo(TRUE, resStr, len);
    }
    fStandaloneShortWeekdaysCount++;

    standaloneNarrowWeekdaysData = calData.getByKey3(gDayNamesTag, gNamesStandaloneTag, gNamesNarrowTag, status);
    if ( status == U_MISSING_RESOURCE_ERROR ) {
       status = U_ZERO_ERROR;
       standaloneNarrowWeekdaysData = calData.getByKey2(gDayNamesTag, gNamesNarrowTag, status);
       if ( status == U_MISSING_RESOURCE_ERROR ) {
          status = U_ZERO_ERROR;
          standaloneNarrowWeekdaysData = calData.getByKey2(gDayNamesTag, gNamesAbbrTag, status);
       }
    }
    fStandaloneNarrowWeekdaysCount = ures_getSize(standaloneNarrowWeekdaysData);
    fStandaloneNarrowWeekdays = new UnicodeString[fStandaloneNarrowWeekdaysCount+1];
    /* test for NULL */
    if (fStandaloneNarrowWeekdays == 0) {
        status = U_MEMORY_ALLOCATION_ERROR;
        goto cleanup;
    }
    // leave fStandaloneNarrowWeekdays[0] empty
    for(i = 0; i<fStandaloneNarrowWeekdaysCount; i++) {
        resStr = ures_getStringByIndex(standaloneNarrowWeekdaysData, i, &len, &status);
        // setTo() - see assignArray comments
        fStandaloneNarrowWeekdays[i+1].setTo(TRUE, resStr, len);
    }
    fStandaloneNarrowWeekdaysCount++;

cleanup:
    ures_close(eras);
    ures_close(eraNames);
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

class TimeZoneKeysEnumeration : public StringEnumeration {
private:
    UnicodeString* strings;
    int32_t length;
    int32_t current;
    int32_t capacity;
    TimeZoneKeysEnumeration(UnicodeString* oldStrs, int32_t count){
        strings = newUnicodeStringArray(count);
        if(strings==NULL){
            return;
        }
        capacity = count;
        current = 0;
        for(length = 0; length<capacity; length++){
            strings[length].setTo(oldStrs[length]);
        }
    }    
public:
    static UClassID U_EXPORT2 getStaticClassID(void);
    virtual UClassID getDynamicClassID(void) const;

    TimeZoneKeysEnumeration(int32_t count, UErrorCode status){
        strings = newUnicodeStringArray(count);
        if(strings == NULL){
            status = U_MEMORY_ALLOCATION_ERROR;
        }
        length = 0; 
        current = 0;
        capacity = count;
    }

    void put(const UnicodeString& str, UErrorCode& status){
        if(length < capacity){
            strings[length++].setTo(str);
        }else{
            status = U_INDEX_OUTOFBOUNDS_ERROR;
        }
    }
    virtual ~TimeZoneKeysEnumeration() {
        delete[] strings;
    }

    virtual StringEnumeration * clone() const
    {
        return new TimeZoneKeysEnumeration(strings, length);
    }

    virtual int32_t count(UErrorCode &/*status*/) const {
        return length;
    }
    virtual const UChar* unext(int32_t *resultLength, UErrorCode& /*status*/){
        if(current < length){
            const UChar* ret = strings[current].getBuffer();
            *resultLength = strings[current].length();
            current++;
            return ret;
        }
        return NULL;
    }

    virtual const UnicodeString* snext(UErrorCode& status) {
        if(U_FAILURE(status)){
            return NULL;
        }
        if(current < length){
            return &strings[current++];
        }
        return NULL;
    }
    /* this method is for thread safe iteration */
    const UnicodeString* snext(int32_t& pos, UErrorCode& status)const {
        if(U_FAILURE(status)){
            return NULL;
        }
        if(pos < length){
            return &strings[pos++];
        }
        return NULL;
    }

    virtual void reset(UErrorCode& /*status*/) {
        current = 0;

    }
private:
    UBool equals(const StringEnumeration& other) const{
        if (other.getDynamicClassID() != TimeZoneKeysEnumeration::getStaticClassID()) {
            return FALSE;
        }
        TimeZoneKeysEnumeration& enum2 =  (TimeZoneKeysEnumeration&)(other);
        UErrorCode status = U_ZERO_ERROR;

        int32_t count1 = count(status);
        int32_t count2 = other.count(status);
        if(count1 != count2){
            return FALSE;
        }
        int32_t pos1 = 0; 
        int32_t pos2 = 0;
        const UnicodeString* str1 = NULL;
        const UnicodeString* str2 = NULL;

        while((str1 = snext(pos1, status))!=NULL){ 
            str2 = enum2.snext(pos2, status);
            if(U_FAILURE(status)){
                return FALSE;
            }
            if(*str1 != *str2){
                // bail out at the first failure
                return FALSE;
            }
            
        }
        // if we reached here that means that the enumerations are equal
        return TRUE;
    }
public:
    virtual UBool operator==(const StringEnumeration& that)const{
        return ((this == &that) ||
            (getDynamicClassID() == that.getDynamicClassID() &&
            StringEnumeration::operator==(that) &&
            equals(that)));
    }
};

UOBJECT_DEFINE_RTTI_IMPLEMENTATION(TimeZoneKeysEnumeration)

void
DateFormatSymbols::initZoneStringsArray(UErrorCode& status){
    if(fZoneStringsHash == NULL){
        initZoneStrings(status);
    }
    if(U_FAILURE(status)){
        return;
    }
    fZoneStringsRowCount = fZoneIDEnumeration->count(status);
    fZoneStringsColCount = 8;
    fZoneStrings = (UnicodeString **)uprv_malloc(fZoneStringsRowCount * sizeof(UnicodeString *));
    /* if we can't get a chunk of heap then the system is going down. Pin the blame on system*/
    if (fZoneStrings == NULL) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
    const UnicodeString *zid = NULL;
    TimeZoneKeysEnumeration *keys = (TimeZoneKeysEnumeration*) fZoneIDEnumeration;
    int32_t pos = 0;
    int32_t i = 0;
    while((zid=keys->snext(pos,status))!=NULL){
        *(fZoneStrings+i) = newUnicodeStringArray(fZoneStringsColCount);
        /* test for NULL */
        if ((*(fZoneStrings+i)) == 0) {
            status = U_MEMORY_ALLOCATION_ERROR;
            return;
        }
        UnicodeString* strings = (UnicodeString*)fZoneStringsHash->get(*zid);
        fZoneStrings[i][0].setTo(*zid);
        fZoneStrings[i][1].setTo(strings[TIMEZONE_LONG_STANDARD]);
        fZoneStrings[i][2].setTo(strings[TIMEZONE_SHORT_STANDARD]);
        fZoneStrings[i][3].setTo(strings[TIMEZONE_LONG_DAYLIGHT]);
        fZoneStrings[i][4].setTo(strings[TIMEZONE_SHORT_DAYLIGHT]);
        fZoneStrings[i][5].setTo(strings[TIMEZONE_EXEMPLAR_CITY]);
        fZoneStrings[i][6].setTo(strings[TIMEZONE_LONG_GENERIC]);
        fZoneStrings[i][7].setTo(strings[TIMEZONE_SHORT_GENERIC]);
        i++;
    }
}

U_CDECL_BEGIN
static UBool U_CALLCONV 
compareTZHashValues(const UHashTok val1, const UHashTok val2){

    const UnicodeString* array1 = (UnicodeString*) val1.pointer;
    const UnicodeString* array2 = (UnicodeString*) val2.pointer;
    if(array1==array2){
        return TRUE;
    }
    if(array1==NULL || array2==NULL){
        return FALSE;
    }
    for(int32_t j=0; j< UTZ_MAX_DISPLAY_STRINGS_LENGTH; j++){
        if(array1[j] != array2[j]){
            return FALSE;
        }
    }
    return TRUE;
}
U_CDECL_END

void
DateFormatSymbols::initZoneStrings(UErrorCode &status){
    if(U_FAILURE(status)){
        return;
    }  

    if(fZoneStringsHash != NULL){
        return;
    }
    int32_t i;

    fZoneStringsHash = new Hashtable(uhash_compareUnicodeString, compareTZHashValues, status);
    if(fZoneStringsHash==NULL){
        status = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
    fZoneStringsHash->setValueDeleter(deleteUnicodeStringArray);

    if(fResourceBundle != NULL){
        UnicodeString solidus = UNICODE_STRING_SIMPLE("/");
        UnicodeString colon = UNICODE_STRING_SIMPLE(":");
        UResourceBundle *zoneArray, *zoneItem;
        for(const UResourceBundle* rb = fResourceBundle; rb!=NULL; rb=ures_getParentBundle(rb)){
            zoneArray = ures_getByKey(rb, gZoneStringsTag, NULL, &status);
            if(U_FAILURE(status)){
                break;
            }
            while(ures_hasNext(zoneArray)){
                UErrorCode tempStatus = U_ZERO_ERROR;
                zoneItem = ures_getNextResource(zoneArray, NULL, &status);
                UnicodeString key(ures_getKey(zoneItem), -1, US_INV);
                if (key.indexOf(colon) == -1) {
                    ures_close(zoneItem);
                    continue;
                }
                UnicodeString* strArray = newUnicodeStringArray(UTZ_MAX_DISPLAY_STRINGS_LENGTH);
                key.findAndReplace(colon, solidus);
                int32_t len = 0;
                //fetch the strings with fine grained fallback
                const UChar* str = ures_getStringByKeyWithFallback(zoneItem,UTZ_SHORT_STANDARD, &len, &tempStatus);
                if(U_SUCCESS(tempStatus)){
                    strArray[TIMEZONE_SHORT_STANDARD].setTo(TRUE, str, len);
                }else{
                    tempStatus = U_ZERO_ERROR;
                }
                str = ures_getStringByKeyWithFallback(zoneItem,UTZ_SHORT_GENERIC, &len, &tempStatus);
                if(U_SUCCESS(tempStatus)){
                    strArray[TIMEZONE_SHORT_GENERIC].setTo(TRUE, str, len);
                }else{
                    tempStatus = U_ZERO_ERROR;
                }                
                str = ures_getStringByKeyWithFallback(zoneItem,UTZ_SHORT_DAYLIGHT, &len, &tempStatus);
                if(U_SUCCESS(tempStatus)){
                    strArray[TIMEZONE_SHORT_DAYLIGHT].setTo(TRUE, str, len);
                }else{
                    tempStatus = U_ZERO_ERROR;
                }
                str = ures_getStringByKeyWithFallback(zoneItem,UTZ_LONG_STANDARD, &len, &tempStatus);
                if(U_SUCCESS(tempStatus)){
                    strArray[TIMEZONE_LONG_STANDARD].setTo(TRUE, str, len);
                }else{
                    tempStatus = U_ZERO_ERROR;
                }
                str = ures_getStringByKeyWithFallback(zoneItem,UTZ_LONG_GENERIC, &len, &tempStatus);
                if(U_SUCCESS(tempStatus)){
                    strArray[TIMEZONE_LONG_GENERIC].setTo(TRUE, str, len);
                }else{
                    tempStatus = U_ZERO_ERROR;
                }                
                str = ures_getStringByKeyWithFallback(zoneItem,UTZ_LONG_DAYLIGHT, &len, &tempStatus);
                if(U_SUCCESS(tempStatus)){
                    strArray[TIMEZONE_LONG_DAYLIGHT].setTo(TRUE, str, len);
                }else{
                    tempStatus = U_ZERO_ERROR;
                }
                str = ures_getStringByKeyWithFallback(zoneItem,UTZ_EXEMPLAR_CITY, &len, &tempStatus);
                if(U_SUCCESS(tempStatus)){
                    strArray[TIMEZONE_EXEMPLAR_CITY].setTo(TRUE, str, len);
                }else{
                    tempStatus = U_ZERO_ERROR;
                }
                // store the strings in hash
                fZoneStringsHash->put(key, strArray, status);
                ures_close(zoneItem);
            }

            ures_close(zoneArray);
        }

        // Need to make sure that all zoneStrings in root are covered as well, otherwise metazone lookups won't
        // work properly
        UResourceBundle* root_res = ures_open(NULL, "", &status);
        zoneArray = ures_getByKey(root_res, gZoneStringsTag, NULL, &status);
        if (U_SUCCESS(status)) {
            while(ures_hasNext(zoneArray)){
                UErrorCode tempStatus = U_ZERO_ERROR;
                zoneItem = ures_getNextResource(zoneArray, NULL, &status);
                UnicodeString key(ures_getKey(zoneItem), -1, US_INV);
                if ( key.indexOf(colon) == -1 ) {
                    ures_close(zoneItem);
                    continue;
                }
                key.findAndReplace(colon, solidus);

                // Don't step on anything that is already there
                UnicodeString* existingArray = (UnicodeString*)fZoneStringsHash->get(key);
                if(existingArray != NULL){
                    ures_close(zoneItem);
                    continue;
                }
                UnicodeString* strArray = newUnicodeStringArray(UTZ_MAX_DISPLAY_STRINGS_LENGTH);
                int32_t len = 0;

                const UChar *str = ures_getStringByKeyWithFallback(zoneItem,UTZ_EXEMPLAR_CITY, &len, &tempStatus);
                if(U_SUCCESS(tempStatus)){
                    strArray[TIMEZONE_EXEMPLAR_CITY].setTo(TRUE, str, len);
                }else{
                    tempStatus = U_ZERO_ERROR;
                }
                // store the strings in hash
                fZoneStringsHash->put(key, strArray, status);
                ures_close(zoneItem);
            }
            ures_close(zoneArray);
            ures_close(root_res);
        }

        int32_t length = fZoneStringsHash->count();
        TimeZoneKeysEnumeration* keysEnum = new TimeZoneKeysEnumeration(length, status);
        fZoneIDEnumeration = keysEnum;
        if(fZoneIDEnumeration==NULL){
            delete fZoneStringsHash;
            fZoneStringsHash = NULL;
            status = U_MEMORY_ALLOCATION_ERROR;
            return;
        }
        int32_t pos=-1;
        const UnicodeString* key; 
        const UHashElement* elem = NULL;
        while((elem = fZoneStringsHash->nextElement(pos))!= NULL){  
            const UHashTok keyTok = elem->key;
            key = (const UnicodeString*)keyTok.pointer;
            keysEnum->put(*key, status);
        }
    }else{
        //last resort strings
        UnicodeString* array = newUnicodeStringArray(UTZ_MAX_DISPLAY_STRINGS_LENGTH);
        if(array==NULL){
            delete fZoneStringsHash;
            status = U_MEMORY_ALLOCATION_ERROR;
            return;
        }
        int32_t length = ARRAY_LENGTH(gLastResortZoneStrings);
        UnicodeString key(gLastResortZoneStrings[0]);
        TimeZoneKeysEnumeration* keysEnum = new TimeZoneKeysEnumeration(length, status);
        fZoneIDEnumeration = keysEnum;
        if(fZoneIDEnumeration==NULL){
            delete fZoneStringsHash;
            delete[] array;
            fZoneStringsHash = NULL;
            status = U_MEMORY_ALLOCATION_ERROR;
            return;
        }
        keysEnum->put(key, status);
        int32_t j=1;
        for(i=0; i< length; ){
            array[i++].setTo(gLastResortZoneStrings[j++]);
        }
        fZoneStringsHash->put(key, array, status);
    }
}
void 
DateFormatSymbols::initZoneStrings(const UnicodeString** strings, int32_t rowCount, int32_t columnCount, UErrorCode& status){
    if(strings==NULL || rowCount<0 || columnCount<0){
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    TimeZoneKeysEnumeration* keysEnum = new TimeZoneKeysEnumeration(rowCount, status);
    fZoneIDEnumeration = keysEnum;
    if(U_FAILURE(status)){
        return;
    }
    if(fZoneIDEnumeration==NULL){
        status = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
    fZoneStringsHash = new Hashtable(uhash_compareUnicodeString, compareTZHashValues, status);
    if(U_FAILURE(status)){
        return;
    }
    if(fZoneStringsHash==NULL){
        status = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
    fZoneStringsHash->setValueDeleter(deleteUnicodeStringArray);
    for (int32_t row=0; row<rowCount; ++row){
        // the first string in the array is the key.
        UnicodeString key = strings[row][0];
        keysEnum->put(key, status);
        UnicodeString* array = newUnicodeStringArray(UTZ_MAX_DISPLAY_STRINGS_LENGTH);
        if(array==NULL){
            status = U_MEMORY_ALLOCATION_ERROR;
            return;
        }
        for (int32_t col=1; col<columnCount; ++col) {
            // fastCopyFrom() - see assignArray comments
            switch (col){
                case 1:
                    array[TIMEZONE_LONG_STANDARD].setTo(strings[row][col]);
                    break;
                case 2:
                    array[TIMEZONE_SHORT_STANDARD].setTo(strings[row][col]);
                    break;
                case 3:
                    array[TIMEZONE_LONG_DAYLIGHT].setTo(strings[row][col]);
                    break;
                case 4:
                     array[TIMEZONE_LONG_DAYLIGHT].setTo(strings[row][col]);
                     break;
                case 5:
                     array[TIMEZONE_EXEMPLAR_CITY].setTo(strings[row][col]);
                     break;
                case 6:
                     array[TIMEZONE_LONG_GENERIC].setTo(strings[row][col]);
                     break; 
                case 7:
                     array[TIMEZONE_SHORT_GENERIC].setTo(strings[row][col]);
                     break;
                default:
                    status = U_ILLEGAL_ARGUMENT_ERROR;
            }
            // populate the hash table
            fZoneStringsHash->put(strings[row][0], array, status);
        }
    }

}

UnicodeString&
DateFormatSymbols::getZoneString(const UnicodeString &zid, const TimeZoneTranslationType type, 
                                 UnicodeString &result, UErrorCode &status){

    if(fZoneStringsHash == NULL){
        //lazy initialization
        initZoneStrings(status);
    }
    if(U_FAILURE(status)){
        return result;
    }

    UnicodeString* stringsArray = (UnicodeString*)fZoneStringsHash->get(zid);
    if(stringsArray != NULL){
        result.setTo(stringsArray[type],0);
    }
    return result;
}

UnicodeString
DateFormatSymbols::getMetazoneString(const UnicodeString &zid, const TimeZoneTranslationType type, Calendar &cal,
                                 UnicodeString &result, UErrorCode &status)
{
    UErrorCode tempStatus = U_ZERO_ERROR;
    int32_t len;
    UnicodeString mzid(UNICODE_STRING_SIMPLE("meta/"));

    // Get the appropriate metazone mapping from the resource bundles

    char usesMetazoneKey[ZID_KEY_MAX];
    char zidkey[ZID_KEY_MAX];

    uprv_strcpy(usesMetazoneKey,gZoneStringsTag);
    uprv_strcat(usesMetazoneKey,"/");

    len = zid.length();
    len = (len >= (ZID_KEY_MAX-1) ? ZID_KEY_MAX-1 : len);
    u_UCharsToChars(zid.getBuffer(), zidkey, len);
    zidkey[len] = 0; // NULL terminate

    // Replace / with : for zid
    len = (int32_t)uprv_strlen(zidkey);
    for (int i = 0; i < len; i++) {
        if (zidkey[i] == '/') {
            zidkey[i] = ':';
        }
    }

    uprv_strcat(usesMetazoneKey,zidkey);
    uprv_strcat(usesMetazoneKey,"/");
    uprv_strcat(usesMetazoneKey,UTZ_USES_METAZONE);

    UResourceBundle *um = ures_getByKeyWithFallback(fResourceBundle, usesMetazoneKey, NULL, &tempStatus);
    if (U_FAILURE(tempStatus)) {
        return result;
    }

    UnicodeString* stringsArray = (UnicodeString*)fZoneStringsHash->get(zid);

    if(stringsArray != NULL){
        SimpleDateFormat df(UNICODE_STRING_SIMPLE("yyyy-MM-dd HH:mm"), Locale(""),tempStatus);
        TimeZone *tz = TimeZone::createTimeZone(UNICODE_STRING_SIMPLE("Etc/GMT"));
        df.setTimeZone(*tz);
        delete tz;
        UnicodeString theTime;
        df.format(cal.getTime(tempStatus),theTime);

        while (ures_hasNext(um)) {
            UResourceBundle *mz = ures_getNextResource(um,NULL,&status);
            const UChar *mz_name = ures_getStringByIndex(mz,0,&len,&status);
            const UChar *mz_from = ures_getStringByIndex(mz,1,&len,&status);
            const UChar *mz_to   = ures_getStringByIndex(mz,2,&len,&status);
            ures_close(mz);
            if(U_FAILURE(status)){
                break;
            }

            if (mz_name[0] != 0 &&
                UnicodeString(TRUE, mz_from, -1) <= theTime &&
                UnicodeString(TRUE, mz_to, -1) > theTime )
            {
                mzid += mz_name;
                getZoneString(mzid,type,result,status);
                break;
            }
        }
    } 
    ures_close(um);
    if ( mzid.length() > 5 ) {
        return mzid;
    }
    return result;
}

UnicodeString&
DateFormatSymbols::getFallbackString(const UnicodeString &zid, UnicodeString &result, UErrorCode &status)
{
    UnicodeString exemplarCity;
    char zidkey[ZID_KEY_MAX];
    char zoneTerritoryChars[ULOC_COUNTRY_CAPACITY];
    UnicodeString displayCountry;
    UnicodeString solidus = UNICODE_STRING_SIMPLE("/");
    UnicodeString und = UNICODE_STRING_SIMPLE("_");
    UnicodeString spc = UNICODE_STRING_SIMPLE(" ");
    const UChar* aZone = NULL;
    UBool IsMultiZone = FALSE;

   
    int32_t len = zid.length();
    len = (len >= (ZID_KEY_MAX-1) ? ZID_KEY_MAX-1 : len);
    u_UCharsToChars(zid.getBuffer(), zidkey, len);
    zidkey[len] = 0; // NULL terminate

    // Replace / with : for zid
    len = (int32_t)uprv_strlen(zidkey);
    for (int i = 0; i < len; i++) {
        if (zidkey[i] == '/') {
            zidkey[i] = ':';
        }
    }

    result.remove();

    UResourceBundle* supplementalDataBundle = ures_openDirect(NULL, kSUPPLEMENTAL, &status);
    if (U_FAILURE(status) || fResourceBundle == NULL ) {
        return result;
    }
       
    UResourceBundle* zoneFormatting = ures_getByKey(supplementalDataBundle, gZoneFormattingTag, NULL, &status);
    UResourceBundle* thisZone = ures_getByKey(zoneFormatting, zidkey, NULL, &status);
    if (U_FAILURE(status)) {
        ures_close(zoneFormatting);
        ures_close(supplementalDataBundle);
        return result; 
    }

    UResourceBundle* multiZone = ures_getByKey(zoneFormatting, gMultizoneTag, NULL, &status);
    const UChar *zoneTerritory = ures_getStringByKey(thisZone,gTerritoryTag,&len,&status);
    u_UCharsToChars(zoneTerritory, zoneTerritoryChars, u_strlen(zoneTerritory));
    zoneTerritoryChars[u_strlen(zoneTerritory)] = 0; // NULL terminate

    UResourceBundle* countries = ures_getByKey(fResourceBundle, gCountriesTag, NULL, &status);
    if ( u_strlen(zoneTerritory) > 0 && countries != NULL ) {
        displayCountry = ures_getStringByKeyWithFallback(countries,zoneTerritoryChars,&len,&status);
    }

    if ( U_FAILURE(status) ) {
        status = U_ZERO_ERROR;
        displayCountry = UnicodeString(zoneTerritory);
    }

    while ( ures_hasNext(multiZone) ) {
        aZone = ures_getNextString(multiZone,&len,NULL,&status);
        if ( u_strcmp(aZone,zoneTerritory) == 0 ) {
            IsMultiZone = TRUE;
            continue;
        }
    }
    
    if ( IsMultiZone ) {
        getZoneString(zid, TIMEZONE_EXEMPLAR_CITY, exemplarCity, status);
        if ( exemplarCity.length()==0 ) {
	    exemplarCity.setTo(UnicodeString(zid,zid.lastIndexOf(solidus)+1));
            exemplarCity.findAndReplace(und,spc);
        }
        Formattable cityCountryArray[2];
        UnicodeString pattern = UnicodeString(ures_getStringByKeyWithFallback(fResourceBundle,gFallbackFormatTag,&len,&status));
        if ( U_FAILURE(status) ) {
            pattern = UNICODE_STRING_SIMPLE("{1} ({0})");
            status = U_ZERO_ERROR;
        }
        cityCountryArray[0].adoptString(new UnicodeString(exemplarCity));
        cityCountryArray[1].adoptString(new UnicodeString(displayCountry));
        MessageFormat::format(pattern,cityCountryArray, 2, result, status);
    } else {
        Formattable countryArray[1];
        UnicodeString pattern = UnicodeString(ures_getStringByKeyWithFallback(fResourceBundle,gRegionFormatTag,&len,&status));
        if ( U_FAILURE(status) ) {
            pattern = UNICODE_STRING_SIMPLE("{0}");
            status = U_ZERO_ERROR;
        }
        countryArray[0].adoptString(new UnicodeString(displayCountry));
        MessageFormat::format(pattern,countryArray, 1, result, status);
    }
    
    ures_close(thisZone);
    ures_close(zoneFormatting);
    ures_close(supplementalDataBundle);
    ures_close(countries);
    ures_close(multiZone);

    return result;
}

UBool
DateFormatSymbols::isCommonlyUsed(const UnicodeString &zid){
    UErrorCode status=U_ZERO_ERROR;
    UResourceBundle *zoneArray, *zoneItem, *cuRes;
    UnicodeString solidus = UNICODE_STRING_SIMPLE("/");
    UnicodeString colon = UNICODE_STRING_SIMPLE(":");
    UnicodeString key(zid);
    char keychars[ZID_KEY_MAX+1];

    key.findAndReplace(solidus,colon);

    for(const UResourceBundle* rb = fResourceBundle; rb!=NULL; rb=ures_getParentBundle(rb)){
        zoneArray = ures_getByKey(rb, gZoneStringsTag, NULL, &status);
        if(U_FAILURE(status)){
            status = U_ZERO_ERROR;
            continue;
        }
        int32_t len = key.length();
        u_UCharsToChars(key.getBuffer(), keychars, len);
        keychars[len] = 0; // NULL terminate
        zoneItem = ures_getByKey(zoneArray,keychars,NULL, &status);
        if(U_FAILURE(status)){
            ures_close(zoneArray);
            status = U_ZERO_ERROR;
            continue;
        }

        cuRes = ures_getByKey(zoneItem,UTZ_COMMONLY_USED,NULL,&status);
        if(U_FAILURE(status)){
            ures_close(zoneItem);
            ures_close(zoneArray);
            status = U_ZERO_ERROR;
            continue;
        }
        int32_t cuValue = ures_getInt(cuRes,&status);

        ures_close(cuRes);
        ures_close(zoneItem);
        ures_close(zoneArray);

        if(U_FAILURE(status)){
            status = U_ZERO_ERROR;
            continue;
        }

        if ( cuValue == 1 ) {
            return TRUE;
        }
    }
    return FALSE;
}

StringEnumeration* 
DateFormatSymbols::createZoneStringIDs(UErrorCode &status){
    if(U_FAILURE(status)){
        return NULL;
    }
    if(fZoneStringsHash == NULL){
        //lazy initialization
        initZoneStrings(status);
    }
    return fZoneIDEnumeration->clone();
}

/**
 * Sets timezone strings.
 * @draft ICU 3.6
 */
void 
DateFormatSymbols::setZoneString(const UnicodeString &zid, const TimeZoneTranslationType type,
                                 const UnicodeString &value, UErrorCode &status){
    if(fZoneStringsHash == NULL){
        //lazy initialization
        initZoneStrings(status);
    }
    if(U_FAILURE(status)){
        return;
    }
    UnicodeString* stringsArray = (UnicodeString*)fZoneStringsHash->get(zid);
    if(stringsArray != NULL){
        stringsArray[type].setTo(value);
    }else{
        stringsArray = newUnicodeStringArray(UTZ_MAX_DISPLAY_STRINGS_LENGTH); 
        if(stringsArray==NULL){
            status = U_MEMORY_ALLOCATION_ERROR;
            return;
        }
        stringsArray[type].setTo(value);
        fZoneStringsHash->put(zid, stringsArray, status);
        TimeZoneKeysEnumeration* keys = (TimeZoneKeysEnumeration*) fZoneIDEnumeration;
        keys->put(zid, status);
    }
}

Hashtable* 
DateFormatSymbols::createZoneStringsHash(const Hashtable* otherHash){
    UErrorCode status = U_ZERO_ERROR;
    Hashtable* hash = new Hashtable(uhash_compareUnicodeString, compareTZHashValues, status);
    if(hash==NULL){
        return NULL;
    }
    if(U_FAILURE(status)){
        return NULL;
    }
    hash->setValueDeleter(deleteUnicodeStringArray);
    int32_t pos = -1;
    const UHashElement* elem = NULL;
    // walk through the hash table and create a deep clone 
    while((elem = otherHash->nextElement(pos))!= NULL){
        const UHashTok otherKeyTok = elem->key;
        const UHashTok otherValueTok = elem->value;
        UnicodeString* otherKey = (UnicodeString*)otherKeyTok.pointer;
        UnicodeString* otherArray = (UnicodeString*)otherValueTok.pointer;
        UnicodeString* array = newUnicodeStringArray(UTZ_MAX_DISPLAY_STRINGS_LENGTH);
        if(array==NULL){
            return NULL;
        }
        UnicodeString key(*otherKey);
        for(int32_t i=0; i<UTZ_MAX_DISPLAY_STRINGS_LENGTH; i++){
            array[i].setTo(otherArray[i]);
        }
        hash->put(key, array, status);
        if(U_FAILURE(status)){
            delete[] array;
            return NULL;
        }
    } 
    return hash;
}


UnicodeString&
DateFormatSymbols::getZoneID(const UnicodeString& zid, UnicodeString& result, UErrorCode& status){
    if(fZoneStringsHash == NULL){
        initZoneStrings(status); 
    }
    if(U_FAILURE(status)){
        return result;
    }
    UnicodeString* strings = (UnicodeString*)fZoneStringsHash->get(zid);
    if (strings != NULL) {
        return result.setTo(zid,0);
    }

    // Do a search through the equivalency group for the given ID
    int32_t n = TimeZone::countEquivalentIDs(zid);
    if (n > 1) {
        int32_t i;
        for (i=0; i<n; ++i) {
            UnicodeString equivID = TimeZone::getEquivalentID(zid, i);
            if (equivID != zid) {
                strings = (UnicodeString*)fZoneStringsHash->get(equivID);
                if (strings != NULL) {
                    return result.setTo(equivID,0);
                }
            }
        }
    }else{
        result.setTo(zid);
    }
    return result;
}

void
DateFormatSymbols::getZoneType(const UnicodeString& zid, const UnicodeString& text, int32_t start, 
                               TimeZoneTranslationType& type, UnicodeString& value, UErrorCode& status){
    if(fZoneStringsHash == NULL){
        initZoneStrings(status);
    }
    if(U_FAILURE(status)){
        return;
    }
    type = TIMEZONE_COUNT;
    UnicodeString* strings = (UnicodeString*)fZoneStringsHash->get(zid);
    if(strings != NULL){
        for(int32_t j=0; j<UTZ_MAX_DISPLAY_STRINGS_LENGTH; j++){
            if(strings[j].length() >0 && text.caseCompare(start, strings[j].length(), strings[j], 0)==0){
                type = (TimeZoneTranslationType)j;
                value.setTo(strings[j]);
                return;
            }
        }
    }
}
void
DateFormatSymbols::findZoneIDTypeValue( UnicodeString& zid, const UnicodeString& text, int32_t start, 
                                        TimeZoneTranslationType& type, UnicodeString& value,
                                        UErrorCode& status){
    if(fZoneStringsHash == NULL){
        initZoneStrings(status);
    }
    if(U_FAILURE(status)){
        return;
    }
    const UnicodeString* myKey = NULL;
    int32_t pos = 0;
    TimeZoneKeysEnumeration *keys = (TimeZoneKeysEnumeration*)fZoneIDEnumeration;
    while( (myKey=keys->snext(pos, status))!= NULL){
        UnicodeString* strings = (UnicodeString*)fZoneStringsHash->get(*myKey);
        if(strings != NULL){
            for(int32_t j=0; j<UTZ_MAX_DISPLAY_STRINGS_LENGTH; j++){
                if(strings[j].length()>0 && text.caseCompare(start, strings[j].length(), strings[j], 0)==0){
                    type = (TimeZoneTranslationType)j;
                    value.setTo(strings[j]);
                    if (myKey->startsWith(UNICODE_STRING_SIMPLE("meta"))) {
                       zid.setTo(resolveParsedMetazone(*myKey));
                    }
                    else {
                       zid.setTo(*myKey);
                    }
                    return;
                }
            }
        }
    }

    // Check for generic tz fallback strings if we have gone through all zone strings and haven't found
    // anything.  

    UnicodeString fbString;
    StringEnumeration *tzKeys = TimeZone::createEnumeration();

    while( (myKey=tzKeys->snext(status))!= NULL){
        status = U_ZERO_ERROR;
        this->getFallbackString(*myKey,fbString,status);
        if ( U_FAILURE(status) ) {
           status = U_ZERO_ERROR;
           continue;
        }
        
        if(fbString.length()>0 && text.compare(start, fbString.length(), fbString)==0){
            type = (TimeZoneTranslationType) TIMEZONE_LONG_GENERIC;
            value.setTo(fbString);
            zid.setTo(*myKey);
            break;
        }
    }
    delete tzKeys;
}

UnicodeString
DateFormatSymbols::resolveParsedMetazone( const UnicodeString& zid ) {

    UErrorCode status = U_ZERO_ERROR;

    UResourceBundle* supplementalDataBundle = ures_openDirect(NULL, kSUPPLEMENTAL, &status);
    UResourceBundle* mapTz = ures_getByKey(supplementalDataBundle, gMaptimezonesTag, NULL, &status);
    if(U_FAILURE(status)){
        ures_close(supplementalDataBundle);
        return UNICODE_STRING_SIMPLE("Etc/GMT");
    }

    UResourceBundle* metazoneMap = ures_getByKey(mapTz, gMetazonesTag, NULL, &status);
    char mzMapKey[ZID_KEY_MAX+4];

    int32_t len = zid.length();
    len = (len >= (ZID_KEY_MAX-1) ? ZID_KEY_MAX-1 : len);
    u_UCharsToChars(zid.getBuffer(), mzMapKey, len);
    mzMapKey[len] = 0; // NULL terminate

    for (int i = 0; i < len; i++) {
        if (mzMapKey[i] == '/') {
            mzMapKey[i] = ':';
        }
    }

    uprv_strcat(mzMapKey,"_");
    uprv_strcat(mzMapKey,fCountry);

    int32_t len2;
    const UChar* resStr = ures_getStringByKey(metazoneMap, mzMapKey, &len2, &status);

    // If we can't find a territory-specific metazone mapping, then use the generic one
    // which is the metazone name followed by _001

    if(U_FAILURE(status)){
        status = U_ZERO_ERROR;
        mzMapKey[len] = 0;
        uprv_strcat(mzMapKey,"_001");
        resStr = ures_getStringByKey(metazoneMap, mzMapKey, &len2, &status);
    }

    ures_close(metazoneMap);
    ures_close(mapTz);
    ures_close(supplementalDataBundle);

    if(U_SUCCESS(status)){
        return resStr;
    }
    else {
        return UNICODE_STRING_SIMPLE("Etc/GMT");
    }

}
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

//eof
