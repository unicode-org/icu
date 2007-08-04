/*
*******************************************************************************
* Copyright (C) 2007, International Business Machines Corporation and
* others. All Rights Reserved.
*******************************************************************************
*
* File DTPTNGEN.CPP
*
*******************************************************************************
*/

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/datefmt.h"
#include "unicode/decimfmt.h"
#include "unicode/dtfmtsym.h"
#include "unicode/dtptngen.h"
#include "unicode/msgfmt.h"
#include "unicode/smpdtfmt.h"
#include "unicode/udat.h"
#include "unicode/udatpg.h"
#include "unicode/uniset.h"
#include "unicode/ures.h"
#include "unicode/rep.h"
#include "cpputils.h"
#include "ucln_in.h"
#include "mutex.h"
#include "cmemory.h"
#include "cstring.h"
#include "locbased.h"
#include "gregoimp.h"
#include "hash.h"
#include "uresimp.h"
#include "dtptngen_impl.h"

#if defined U_DEBUG_DTPTN
#include <stdio.h>
#endif

#define LENGTHOF(array) (int32_t)(sizeof(array)/sizeof((array)[0]))

U_NAMESPACE_BEGIN

// *****************************************************************************
// class DateTimePatternGenerator
// *****************************************************************************
static const UChar Canonical_Items[] = {
    // GyQMwWedDFHmsSv
    CAP_G, LOW_Y, CAP_Q, CAP_M, LOW_W, CAP_W, LOW_E, LOW_D, CAP_D, CAP_F,
    CAP_H, LOW_M, LOW_S, CAP_S, LOW_V, 0
};

static const dtTypeElem dtTypes[] = {
    // patternChar, field, type, minLen, weight
    {CAP_G, UDATPG_ERA_FIELD, DT_SHORT, 1, 3,},
    {CAP_G, UDATPG_ERA_FIELD, DT_LONG, 4, 0},
    {LOW_Y, UDATPG_YEAR_FIELD, DT_NUMERIC, 1, 20},
    {CAP_Y, UDATPG_YEAR_FIELD, DT_NUMERIC + DT_DELTA, 1, 20},
    {LOW_U, UDATPG_YEAR_FIELD, DT_NUMERIC + 2*DT_DELTA, 1, 20},
    {CAP_Q, UDATPG_QUARTER_FIELD, DT_NUMERIC, 1, 2},
    {CAP_Q, UDATPG_QUARTER_FIELD, DT_SHORT, 3, 0},
    {CAP_Q, UDATPG_QUARTER_FIELD, DT_LONG, 4, 0},
    {CAP_M, UDATPG_MONTH_FIELD, DT_NUMERIC, 1, 2},
    {CAP_M, UDATPG_MONTH_FIELD, DT_SHORT, 3, 0},
    {CAP_M, UDATPG_MONTH_FIELD, DT_LONG, 4, 0},
    {CAP_M, UDATPG_MONTH_FIELD, DT_NARROW, 5, 0},
    {CAP_L, UDATPG_MONTH_FIELD, DT_NUMERIC + DT_DELTA, 1, 2},
    {CAP_L, UDATPG_MONTH_FIELD, DT_SHORT - DT_DELTA, 3, 0},
    {CAP_L, UDATPG_MONTH_FIELD, DT_LONG - DT_DELTA, 4, 0},
    {CAP_L, UDATPG_MONTH_FIELD, DT_NARROW - DT_DELTA, 5, 0},
    {LOW_W, UDATPG_WEEK_OF_YEAR_FIELD, DT_NUMERIC, 1, 2},
    {CAP_W, UDATPG_WEEK_OF_MONTH_FIELD, DT_NUMERIC + DT_DELTA, 1, 0},
    {LOW_E, UDATPG_WEEKDAY_FIELD, DT_NUMERIC + DT_DELTA, 1, 2},
    {LOW_E, UDATPG_WEEKDAY_FIELD, DT_SHORT - DT_DELTA, 3, 0},
    {LOW_E, UDATPG_WEEKDAY_FIELD, DT_LONG - DT_DELTA, 4, 0},
    {LOW_E, UDATPG_WEEKDAY_FIELD, DT_NARROW - DT_DELTA, 5, 0},
    {CAP_E, UDATPG_WEEKDAY_FIELD, DT_SHORT, 1, 3},
    {CAP_E, UDATPG_WEEKDAY_FIELD, DT_LONG, 4, 0},
    {CAP_E, UDATPG_WEEKDAY_FIELD, DT_NARROW, 5, 0},
    {LOW_C, UDATPG_WEEKDAY_FIELD, DT_NUMERIC + 2*DT_DELTA, 1, 2},
    {LOW_C, UDATPG_WEEKDAY_FIELD, DT_SHORT - 2*DT_DELTA, 3, 0},
    {LOW_C, UDATPG_WEEKDAY_FIELD, DT_LONG - 2*DT_DELTA, 4, 0},
    {LOW_C, UDATPG_WEEKDAY_FIELD, DT_NARROW - 2*DT_DELTA, 5, 0},
    {LOW_D, UDATPG_DAY_FIELD, DT_NUMERIC, 1, 2},
    {CAP_D, UDATPG_DAY_OF_YEAR_FIELD, DT_NUMERIC + DT_DELTA, 1, 3},
    {CAP_F, UDATPG_DAY_OF_WEEK_IN_MONTH_FIELD, DT_NUMERIC + 2*DT_DELTA, 1, 0},
    {LOW_G, UDATPG_DAY_FIELD, DT_NUMERIC + 3*DT_DELTA, 1, 20}, // really internal use, so we d'ont care
    {LOW_A, UDATPG_DAYPERIOD_FIELD, DT_SHORT, 1, 0},
    {CAP_H, UDATPG_HOUR_FIELD, DT_NUMERIC + 10*DT_DELTA, 1, 2}, // 24 hour
    {LOW_K, UDATPG_HOUR_FIELD, DT_NUMERIC + 11*DT_DELTA, 1, 2},
    {LOW_H, UDATPG_HOUR_FIELD, DT_NUMERIC, 1, 2}, // 12 hour
    {LOW_K, UDATPG_HOUR_FIELD, DT_NUMERIC + DT_DELTA, 1, 2},
    {LOW_M, UDATPG_MINUTE_FIELD, DT_NUMERIC, 1, 2},
    {LOW_S, UDATPG_SECOND_FIELD, DT_NUMERIC, 1, 2},
    {CAP_S, UDATPG_FRACTIONAL_SECOND_FIELD, DT_NUMERIC + DT_DELTA, 1, 1000},
    {CAP_A, UDATPG_SECOND_FIELD, DT_NUMERIC + 2*DT_DELTA, 1, 1000},
    {LOW_V, UDATPG_ZONE_FIELD, DT_SHORT - 2*DT_DELTA, 1, 0},
    {LOW_V, UDATPG_ZONE_FIELD, DT_LONG - 2*DT_DELTA, 4, 0},
    {LOW_Z, UDATPG_ZONE_FIELD, DT_SHORT, 1, 3},
    {LOW_Z, UDATPG_ZONE_FIELD, DT_LONG, 4, 0},
    {CAP_Z, UDATPG_ZONE_FIELD, DT_SHORT - DT_DELTA, 1, 3},
    {CAP_Z, UDATPG_ZONE_FIELD, DT_LONG - DT_DELTA, 4, 0},
    {0, UDATPG_FIELD_COUNT, 0, 0, 0} , // last row of dtTypes[] 
 };

static const char* CLDR_FIELD_APPEND[] = {
    "Era", "Year", "Quarter", "Month", "Week", "*", "Day-Of-Week", "Day", "*", "*", "*",
    "Hour", "Minute", "Second", "*", "Timezone"
};

static const char* CLDR_FIELD_NAME[] = {
    "era", "year", "quarter", "month", "week", "*", "weekday", "day", "*", "*", "dayperiod",
    "hour", "minute", "second", "*", "zone"
};

static const char* Resource_Fields[] = {
    "day", "dayperiod", "era", "hour", "minute", "month", "second", "week",
    "weekday", "year", "zone" };

static const char* CLDR_AVAILABLE_FORMAT[MAX_AVAILABLE_FORMATS] = {
        "Ed", "EMMMd", "H", "HHmm", "HHmmss", "Md", "MMMMd", "MMyy", "Qyy",
        "mmss", "yyMMM", "yyyy",
};  // binary ascending order

static const UnicodeString quotingPattern= UNICODE_STRING_SIMPLE("[[[:script=Latin:][:script=Cyrl:]]&[[:L:][:M:]]]");

UOBJECT_DEFINE_RTTI_IMPLEMENTATION(DateTimePatternGenerator)
UOBJECT_DEFINE_RTTI_IMPLEMENTATION(PtnSkeleton)
UOBJECT_DEFINE_RTTI_IMPLEMENTATION(PtnElem)
UOBJECT_DEFINE_RTTI_IMPLEMENTATION(DateTimeMatcher)
UOBJECT_DEFINE_RTTI_IMPLEMENTATION(PatternMap)
UOBJECT_DEFINE_RTTI_IMPLEMENTATION(PatternMapIterator)
UOBJECT_DEFINE_RTTI_IMPLEMENTATION(FormatParser)
UOBJECT_DEFINE_RTTI_IMPLEMENTATION(DistanceInfo)
UOBJECT_DEFINE_RTTI_IMPLEMENTATION(DTSkeletonEnumeration)
UOBJECT_DEFINE_RTTI_IMPLEMENTATION(DTRedundantEnumeration)

DateTimePatternGenerator*  U_EXPORT2
DateTimePatternGenerator::createInstance(UErrorCode& status) {
    return createInstance(Locale::getDefault(), status);
}

DateTimePatternGenerator* U_EXPORT2
DateTimePatternGenerator::createInstance(const Locale& locale, UErrorCode& status) {
    return new DateTimePatternGenerator(locale, status);
}

DateTimePatternGenerator*  U_EXPORT2
DateTimePatternGenerator::createEmptyInstance(UErrorCode& status) {
    return new DateTimePatternGenerator(status);
}

DateTimePatternGenerator::DateTimePatternGenerator(UErrorCode &status) : UObject() {
    fStatus = U_ZERO_ERROR;
    skipMatcher = NULL;
    fAvailableFormatKeyHash=NULL;
    fp = new FormatParser();
    dtMatcher = new DateTimeMatcher();
    distanceInfo = new DistanceInfo();
    patternMap = new PatternMap();
}

DateTimePatternGenerator::DateTimePatternGenerator(const Locale& locale, UErrorCode &status) : UObject() {
    fp = new FormatParser();
    dtMatcher = new DateTimeMatcher();
    distanceInfo = new DistanceInfo();
    patternMap = new PatternMap();
    initData(locale);
    status = getStatus();
}

DateTimePatternGenerator::DateTimePatternGenerator(const DateTimePatternGenerator& other) : UObject() {
    fStatus = U_ZERO_ERROR;
    skipMatcher = NULL;
    fAvailableFormatKeyHash=NULL;
    fp = new FormatParser();
    dtMatcher = new DateTimeMatcher(); 
    distanceInfo = new DistanceInfo();
    patternMap = new PatternMap();
    *this=other;
}

DateTimePatternGenerator&
DateTimePatternGenerator::operator=(const DateTimePatternGenerator& other) {
    fStatus = U_ZERO_ERROR;
    pLocale = other.pLocale;
    *fp = *(other.fp);
    fp->setFilter(fStatus);
    dtMatcher->copyFrom(other.dtMatcher->skeleton);
    *distanceInfo = *(other.distanceInfo);
    dateTimeFormat = other.dateTimeFormat;
    decimal = other.decimal;
    // NUL-terminate for the C API.
    dateTimeFormat.getTerminatedBuffer();
    decimal.getTerminatedBuffer();
    skipMatcher = other.skipMatcher;
    for (int32_t i=0; i< UDATPG_FIELD_COUNT; ++i ) {
        appendItemFormats[i] = other.appendItemFormats[i];
        appendItemNames[i] = other.appendItemNames[i];
        // NUL-terminate for the C API.
        appendItemFormats[i].getTerminatedBuffer();
        appendItemNames[i].getTerminatedBuffer();
    }
    patternMap->copyFrom(*other.patternMap, fStatus);
    fAvailableFormatKeyHash=NULL;
    copyHashtable(other.fAvailableFormatKeyHash);
    return *this;
}


UBool
DateTimePatternGenerator::operator==(const DateTimePatternGenerator& other) const {
    if (this == &other) {
        return TRUE;
    }
    if ((pLocale==other.pLocale) && (patternMap->equals(*other.patternMap))) {
        return TRUE;
    }
    else {
        return FALSE;
    }
}

DateTimePatternGenerator::~DateTimePatternGenerator() {
    if (fAvailableFormatKeyHash!=NULL) {
        delete fAvailableFormatKeyHash;
        fAvailableFormatKeyHash=NULL;
    }
    
    if (fp != NULL) delete fp; 
    if (dtMatcher != NULL) delete dtMatcher;
    if (distanceInfo != NULL) delete distanceInfo;
    if (patternMap != NULL) delete patternMap;
    
}

void
DateTimePatternGenerator::initData(const Locale& locale) {
    fStatus = U_ZERO_ERROR;
    skipMatcher = NULL;
    fAvailableFormatKeyHash=NULL;

    addCanonicalItems();
    addICUPatterns(locale);
    addCLDRData(locale);
    setDateTimeFromCalendar(locale);
    setDecimalSymbols(locale, fStatus);
} // DateTimePatternGenerator::initData

UnicodeString
DateTimePatternGenerator::getSkeleton(const UnicodeString& pattern, UErrorCode& status) {
    PtnSkeleton *ptrSkeleton;
    UnicodeString result;
    
    dtMatcher->set(pattern, fp);
    ptrSkeleton=dtMatcher->getSkeletonPtr();
    result = ptrSkeleton->getSkeleton();
    return result;
}

UnicodeString
DateTimePatternGenerator::getBaseSkeleton(const UnicodeString& pattern, UErrorCode& status) {
    PtnSkeleton *ptrSkeleton;
    UnicodeString result;
    
    dtMatcher->set(pattern, fp);
    ptrSkeleton=dtMatcher->getSkeletonPtr();
    for (int32_t i=0; i<UDATPG_FIELD_COUNT; ++i ) {
        if (ptrSkeleton->baseOriginal[i].length()!=0) {
            result += ptrSkeleton->baseOriginal[i];
        }
    }
    return result;
}

void
DateTimePatternGenerator::addICUPatterns(const Locale& locale) {
    UnicodeString dfPattern, newPattern;
    UnicodeString conflictingString;
    UDateTimePatternConflict conflictingStatus;
    SimpleDateFormat* df;
    UErrorCode status = U_ZERO_ERROR;
    
    // Load with ICU patterns
    for (DateFormat::EStyle i=DateFormat::kFull; i<=DateFormat::kShort; i=static_cast<DateFormat::EStyle>(i+1)) {
        if ((df = (SimpleDateFormat*)DateFormat::createDateInstance(i, locale))!= NULL) {
            newPattern=df->toPattern(dfPattern);
            conflictingStatus = addPattern(df->toPattern(dfPattern), FALSE, conflictingString, status);
            delete df;
        }

        if ((df = (SimpleDateFormat*)DateFormat::createTimeInstance(i, locale)) != NULL) {
            conflictingStatus = addPattern(df->toPattern(dfPattern), FALSE, conflictingString, status);
            newPattern=df->toPattern(dfPattern);
            if (U_FAILURE(status)) {
                delete df;
                return;
            }
            // HACK for hh:ss
            if ( i==DateFormat::kMedium ) {
                hackPattern = df->toPattern(hackPattern);
            }
            delete df;
        }
    }

}

void
DateTimePatternGenerator::hackTimes(UnicodeString& hackPattern, UErrorCode& status)  {
    UDateTimePatternConflict conflictingStatus;
    UnicodeString conflictingString;
    
    fp->set(hackPattern);
    UnicodeString mmss;
    UBool gotMm=FALSE;
    for (int32_t i=0; i<fp->itemNumber; ++i) {
        UnicodeString field = fp->items[i];
        if ( fp->isQuoteLiteral(field) ) {
            if ( gotMm ) {
               UnicodeString quoteLiteral;
               fp->getQuoteLiteral(quoteLiteral, &i);
               mmss += quoteLiteral;
            }
        }
        else {
            if (fp->isPatternSeparator(field) && gotMm) {
                mmss+=field;
            }
            else {
                UChar ch=field.charAt(0);
                if (ch==LOW_M) {
                    gotMm=TRUE;
                    mmss+=field;
                }
                else {
                    if (ch==LOW_S) {
                        if (!gotMm) {
                            break;
                        }
                        mmss+= field;
                        conflictingStatus = addPattern(mmss, FALSE, conflictingString, status);
                        break;
                    }
                    else {
                        if (gotMm || ch==LOW_Z || ch==CAP_Z || ch==LOW_V || ch==CAP_V) {
                            break;
                        }
                    }
                }
            }
        }
    }
}

void
DateTimePatternGenerator::addCLDRData(const Locale& locale) {
    UErrorCode err = U_ZERO_ERROR;
    UResourceBundle *rb, *gregorianBundle, *calBundle;
    UResourceBundle *patBundle, *fieldBundle, *fBundle;
    UnicodeString rbPattern, value, field;
    UnicodeString conflictingPattern;
    UDateTimePatternConflict conflictingStatus;
    const char *key=NULL;

    // Initialize appendItems
    static const UChar itemFormat[]= {0x7B, 0x30, 0x7D, 0x20, 0x251C, 0x7B, 0x32, 0x7D, 0x3A,
        0x20, 0x7B, 0x31, 0x7D, 0x2524, 0};  // {0} \u251C{2}: {1}\u2524
    UnicodeString defaultItemFormat(TRUE, itemFormat, LENGTHOF(itemFormat)-1);  // Read-only alias.

    for (int32_t i=0; i<UDATPG_FIELD_COUNT; ++i ) {
        appendItemNames[i]=CAP_F;
        if (i<10) {
            appendItemNames[i]+=(i+0x30); 
        }
        else {
            appendItemNames[i]+=0x31;
            appendItemNames[i]+=(i-10 + 0x30);
        }
        // NUL-terminate for the C API.
        appendItemNames[i].getTerminatedBuffer();
    }

    rb = ures_open(NULL, locale.getName(), &err);
    calBundle = ures_getByKey(rb, "calendar", NULL, &err);
    gregorianBundle = ures_getByKey(calBundle, "gregorian", NULL, &err);

    key=NULL;
    int32_t dtCount=0;
    patBundle = ures_getByKeyWithFallback(gregorianBundle, "DateTimePatterns", NULL, &err);
    while (U_SUCCESS(err)) {
        rbPattern = ures_getNextUnicodeString(patBundle, &key, &err);
        dtCount++;
        if (rbPattern.length()==0 ) {
            break;  // no more pattern
        }
        else {
            if (dtCount==9) {
                setDateTimeFormat(rbPattern);
            }
        }
    };
    ures_close(patBundle);
    
    err = U_ZERO_ERROR;
    patBundle = ures_getByKeyWithFallback(gregorianBundle, "appendItems", NULL, &err);
    key=NULL;
    UnicodeString itemKey;
    while (U_SUCCESS(err)) {
        rbPattern = ures_getNextUnicodeString(patBundle, &key, &err);
        if (rbPattern.length()==0 ) {
            break;  // no more pattern
        }
        else {
            setAppendItemFormat(getAppendFormatNumber(key), rbPattern);
        }
    }
    ures_close(patBundle);
    
    key=NULL;
    err = U_ZERO_ERROR;
    fBundle = ures_getByKeyWithFallback(gregorianBundle, "fields", NULL, &err);
    for (int32_t i=0; i<MAX_RESOURCE_FIELD; ++i) {
        err = U_ZERO_ERROR;
        patBundle = ures_getByKeyWithFallback(fBundle, Resource_Fields[i], NULL, &err);
        fieldBundle = ures_getByKeyWithFallback(patBundle, "dn", NULL, &err);
        rbPattern = ures_getNextUnicodeString(fieldBundle, &key, &err);
        ures_close(fieldBundle);
        ures_close(patBundle);
        if (rbPattern.length()==0 ) {
            continue;  
        }
        else {
            setAppendItemName(getAppendNameNumber(Resource_Fields[i]), rbPattern);
        }
    }
    ures_close(fBundle);

    // add available formats
    err = U_ZERO_ERROR;
    initHashtable(err);
    patBundle = ures_getByKeyWithFallback(gregorianBundle, "availableFormats", NULL, &err);
    if (U_SUCCESS(err)) {
        int32_t numberKeys = ures_getSize(patBundle);
        //printf ("\n available formats from current locale:%s", locale.getName());
        int32_t len;
        const UChar *retPattern;
        key=NULL;
        for(int32_t i=0; i<numberKeys; ++i) {
            retPattern=ures_getNextString(patBundle, &len, &key, &err);
            UnicodeString format=UnicodeString(retPattern);
            UnicodeString retKey=UnicodeString(key);
            setAvailableFormat(key, err);
            conflictingStatus = addPattern(format, FALSE, conflictingPattern, err);
        }
    }
    ures_close(patBundle);
    ures_close(gregorianBundle);
    ures_close(calBundle);
    ures_close(rb);
    
    err = U_ZERO_ERROR;
    char parentLocale[50];
    const char *curLocaleName=locale.getName();
    int32_t localeNameLen=0;
    uprv_strcpy(parentLocale, curLocaleName);
    while((localeNameLen=uloc_getParent(parentLocale, parentLocale, 50, &err))>=0 ) {
        rb = ures_open(NULL, parentLocale, &err);
        calBundle = ures_getByKey(rb, "calendar", NULL, &err);
        gregorianBundle = ures_getByKey(calBundle, "gregorian", NULL, &err);
        patBundle = ures_getByKeyWithFallback(gregorianBundle, "availableFormats", NULL, &err);
        if (U_SUCCESS(err)) {
            int32_t numberKeys = ures_getSize(patBundle);
            int32_t len;
            const UChar *retPattern;
            key=NULL;

            //printf ("\n available formats from parent locale:%s", parentLocale);
            for(int32_t i=0; i<numberKeys; ++i) {
                retPattern=ures_getNextString(patBundle, &len, &key, &err);
                UnicodeString format=UnicodeString(retPattern);
                UnicodeString retKey=UnicodeString(key);
                if ( !isAvailableFormatSet(key) ) {
                    setAvailableFormat(key, err);
                    conflictingStatus = addPattern(format, FALSE, conflictingPattern, err);
                }
            }
        }
        ures_close(patBundle);
        ures_close(gregorianBundle);
        ures_close(calBundle);
        ures_close(rb);
        if (localeNameLen==0) {
            break;
        }
    }

    if (hackPattern.length()>0) {
        hackTimes(hackPattern, err);
    }
}

void
DateTimePatternGenerator::initHashtable(UErrorCode& err) {
    if (fAvailableFormatKeyHash!=NULL) {
        return;
    }
    if ((fAvailableFormatKeyHash = new Hashtable(FALSE, err))!=NULL) {
        return;
    }
}


void
DateTimePatternGenerator::setAppendItemFormat(UDateTimePatternField field, const UnicodeString& value) {
    appendItemFormats[field] = value;
    // NUL-terminate for the C API.
    appendItemFormats[field].getTerminatedBuffer();
}

const UnicodeString&
DateTimePatternGenerator::getAppendItemFormat(UDateTimePatternField field) const {
    return appendItemFormats[field];
}

void
DateTimePatternGenerator::setAppendItemName(UDateTimePatternField field, const UnicodeString& value) {
    appendItemNames[field] = value;
    // NUL-terminate for the C API.
    appendItemNames[field].getTerminatedBuffer();
}

const UnicodeString&
DateTimePatternGenerator:: getAppendItemName(UDateTimePatternField field) const {
    return appendItemNames[field];
}

void
DateTimePatternGenerator::getAppendName(UDateTimePatternField field, UnicodeString& value) {
    value = SINGLE_QUOTE;
    value += appendItemNames[field];
    value += SINGLE_QUOTE;
}

UnicodeString
DateTimePatternGenerator::getBestPattern(const UnicodeString& patternForm, UErrorCode& status) {
    const UnicodeString *bestPattern=NULL;
    UnicodeString dtFormat;
    UErrorCode err = U_ZERO_ERROR;
    UnicodeString resultPattern;

    int32_t dateMask=(1<<UDATPG_DAYPERIOD_FIELD) - 1;
    int32_t timeMask=(1<<UDATPG_FIELD_COUNT) - 1 - dateMask;

    resultPattern="";
    dtMatcher->set(patternForm, fp);
    bestPattern=getBestRaw(*dtMatcher, -1, distanceInfo);
    if ( distanceInfo->missingFieldMask==0 && distanceInfo->extraFieldMask==0 ) {
        resultPattern = adjustFieldTypes(*bestPattern, FALSE);

        return resultPattern;
    }
    int32_t neededFields = dtMatcher->getFieldMask();
    UnicodeString datePattern=getBestAppending(neededFields & dateMask);
    UnicodeString timePattern=getBestAppending(neededFields & timeMask);
    if (datePattern.length()==0) {
        if (timePattern.length()==0) {
            resultPattern="";
            return resultPattern;
        }
        else {
            resultPattern=timePattern;
            return resultPattern;
        }
    }
    if (timePattern.length()==0) {
        resultPattern=datePattern;
        return resultPattern;
    }
    resultPattern="";
    dtFormat=getDateTimeFormat();
    Formattable dateTimeObject[] = { datePattern, timePattern };
    resultPattern = MessageFormat::format(dtFormat, dateTimeObject, 2, resultPattern, err );
    return resultPattern;
}

UnicodeString
DateTimePatternGenerator::replaceFieldTypes(const UnicodeString& pattern, 
                                            const UnicodeString& skeleton, 
                                            UErrorCode& status) {
    dtMatcher->set(skeleton, fp);
    UnicodeString result = adjustFieldTypes(pattern, FALSE);
    return result;
}

void
DateTimePatternGenerator::setDecimal(const UnicodeString& decimal) {
    this->decimal = decimal;
    // NUL-terminate for the C API.
    this->decimal.getTerminatedBuffer();
}

const UnicodeString&
DateTimePatternGenerator::getDecimal() const {
    return decimal;
}

void
DateTimePatternGenerator::addCanonicalItems() {
    UnicodeString  conflictingPattern;
    UDateTimePatternConflict conflictingStatus;
    UErrorCode status = U_ZERO_ERROR;

    for (int32_t i=0; i<UDATPG_FIELD_COUNT; i++) {
        conflictingStatus = addPattern(UnicodeString(Canonical_Items[i]), FALSE, conflictingPattern, status);
    }
}

void
DateTimePatternGenerator::setDateTimeFormat(const UnicodeString& dtFormat) {
    dateTimeFormat = dtFormat;
    // NUL-terminate for the C API.
    dateTimeFormat.getTerminatedBuffer();
}

const UnicodeString&
DateTimePatternGenerator::getDateTimeFormat() const {
    return dateTimeFormat;
}

void
DateTimePatternGenerator::setDateTimeFromCalendar(const Locale& locale) {
    UErrorCode err=U_ZERO_ERROR;
    UResourceBundle *patBundle;
    UnicodeString rbPattern;
    const char *key=NULL;

     // Set the datetime pattern
    CalendarData calData(locale, NULL, err );

    // load the first data item
    patBundle = calData.getByKey("DateTimePatterns", err);
    UnicodeString dtFormat;
    while (U_SUCCESS(err)) {
        dtFormat = ures_getNextUnicodeString(patBundle, &key, &err);
        if (rbPattern.length()==0 ) {
            break;  // no more pattern
        }
        setDateTimeFormat(dtFormat);
    };
    //ures_close(patBundle);
}

void
DateTimePatternGenerator::setDecimalSymbols(const Locale& locale, UErrorCode& status) {
    DecimalFormatSymbols dfs = DecimalFormatSymbols(locale, status);
    if(U_SUCCESS(status)) {
        decimal = dfs.getSymbol(DecimalFormatSymbols::kDecimalSeparatorSymbol);
        // NUL-terminate for the C API.
        decimal.getTerminatedBuffer();
    }
}

UDateTimePatternConflict
DateTimePatternGenerator::addPattern(
    const UnicodeString& pattern,
    UBool override,
    UnicodeString &conflictingPattern,
    UErrorCode& status) {

    UnicodeString basePattern;
    PtnSkeleton   skeleton;
    UDateTimePatternConflict conflictingStatus = UDATPG_NO_CONFLICT;

    DateTimeMatcher matcher;
    matcher.set(pattern, fp, skeleton);
    matcher.getBasePattern(basePattern);
    const UnicodeString *duplicatePattern = patternMap->getPatternFromBasePattern(basePattern);
    if (duplicatePattern != NULL ) {
        conflictingStatus = UDATPG_BASE_CONFLICT;
        conflictingPattern = *duplicatePattern;
        if (!override) {
            return conflictingStatus;
        }
    }
    duplicatePattern = patternMap->getPatternFromSkeleton(skeleton);
    if (duplicatePattern != NULL ) {
        conflictingStatus = UDATPG_CONFLICT;
        conflictingPattern = *duplicatePattern;
        if (!override) {
            return conflictingStatus;
        }
    }
    patternMap->add(basePattern, skeleton, pattern, status);
    if(U_FAILURE(status)) {
        return conflictingStatus;
    }
    
    return UDATPG_NO_CONFLICT;
}


UDateTimePatternField
DateTimePatternGenerator::getAppendFormatNumber(const char* field) {
    for (int32_t i=0; i<UDATPG_FIELD_COUNT; ++i ) {
        if (uprv_strcmp(CLDR_FIELD_APPEND[i], field)==0) {
            return (UDateTimePatternField)i;
        }
    }
    return UDATPG_FIELD_COUNT;
}

UDateTimePatternField
DateTimePatternGenerator::getAppendNameNumber(const char* field) {
    for (int32_t i=0; i<UDATPG_FIELD_COUNT; ++i ) {
        if (uprv_strcmp(CLDR_FIELD_NAME[i],field)==0) {
            return (UDateTimePatternField)i;
        }
    }
    return UDATPG_FIELD_COUNT;
}

const UnicodeString*
DateTimePatternGenerator::getBestRaw(DateTimeMatcher& source,
                                     int32_t includeMask,
                                     DistanceInfo* missingFields) {
    int32_t bestDistance = 0x7fffffff;
    DistanceInfo tempInfo;
    const UnicodeString *bestPattern=NULL;

    PatternMapIterator it;
    for (it.set(*patternMap); it.hasNext(); ) {
        DateTimeMatcher trial = it.next();
        if (trial.equals(skipMatcher)) {
            continue;
        }
        int32_t distance=source.getDistance(trial, includeMask, tempInfo);
        if (distance<bestDistance) {
            bestDistance=distance;
            bestPattern=patternMap->getPatternFromSkeleton(*trial.getSkeletonPtr());
            missingFields->setTo(tempInfo);
            if (distance==0) {
                break;
            }
        }
    }

    return bestPattern;
}

UnicodeString
DateTimePatternGenerator::adjustFieldTypes(const UnicodeString& pattern,
                                           UBool fixFractionalSeconds) {
    UnicodeString newPattern="";
    fp->set(pattern);
    for (int32_t i=0; i < fp->itemNumber; i++) {
        UnicodeString field = fp->items[i];
        if ( fp->isQuoteLiteral(field) ) {

            UnicodeString quoteLiteral="";
            fp->getQuoteLiteral(quoteLiteral, &i);
            newPattern += quoteLiteral;
        }
        else {

            int32_t canonicalIndex = fp->getCanonicalIndex(field);
            if (canonicalIndex < 0) {
                if (fp->isPatternSeparator(field)) {
                    newPattern+=field;
                }
                continue; //don't adjust
            }
            const dtTypeElem *row = &dtTypes[canonicalIndex];
            int32_t typeValue = row->field;
            if (fixFractionalSeconds && typeValue == UDATPG_SECOND_FIELD) {
                UnicodeString newField=dtMatcher->skeleton.original[UDATPG_FRACTIONAL_SECOND_FIELD];
                field = field + decimal + newField;
            }
            else {
                if (dtMatcher->skeleton.type[typeValue]!=0) {
                    UnicodeString newField=dtMatcher->skeleton.original[typeValue];
                    if (typeValue!= UDATPG_HOUR_FIELD) {
                        field=newField;
                    }
                    else {
                        if (field.length()!=newField.length()) {
                            UChar c=field.charAt(0);
                            field="";
                            for (int32_t i=newField.length(); i>0; --i) {
                                field+=c;
                            }
                        }
                    }
                }
                newPattern+=field;
            }
        }
    }
    return newPattern;
}

UnicodeString
DateTimePatternGenerator::getBestAppending(int32_t missingFields) {
    UnicodeString  resultPattern, tempPattern, formattedPattern;
    UErrorCode err=U_ZERO_ERROR;
    int32_t lastMissingFieldMask=0;
    if (missingFields!=0) {
        resultPattern=UnicodeString("");
        tempPattern = *getBestRaw(*dtMatcher, missingFields, distanceInfo);
        resultPattern = adjustFieldTypes(tempPattern, FALSE);
        if ( distanceInfo->missingFieldMask==0 ) {
            return resultPattern;
        }
        while (distanceInfo->missingFieldMask!=0) { // precondition: EVERY single field must work!
            if ( lastMissingFieldMask == distanceInfo->missingFieldMask ) {
                break;  // cannot find the proper missing field
            }
            if (((distanceInfo->missingFieldMask & UDATPG_SECOND_AND_FRACTIONAL_MASK)==UDATPG_FRACTIONAL_MASK) &&
                ((missingFields & UDATPG_SECOND_AND_FRACTIONAL_MASK) == UDATPG_SECOND_AND_FRACTIONAL_MASK)) {
                resultPattern = adjustFieldTypes(resultPattern, FALSE);
                resultPattern = tempPattern;
                distanceInfo->missingFieldMask &= ~UDATPG_FRACTIONAL_MASK;
                continue;
            }
            int32_t startingMask = distanceInfo->missingFieldMask;
            tempPattern = *getBestRaw(*dtMatcher, distanceInfo->missingFieldMask, distanceInfo);
            tempPattern = adjustFieldTypes(tempPattern, FALSE);
            int32_t foundMask=startingMask& ~distanceInfo->missingFieldMask;
            int32_t topField=getTopBitNumber(foundMask);
            UnicodeString appendName;
            getAppendName((UDateTimePatternField)topField, appendName);
            const Formattable formatPattern[] = {
                resultPattern,
                tempPattern,
                appendName
            };
            formattedPattern = MessageFormat::format(appendItemFormats[topField], formatPattern, 3, resultPattern, err);
            lastMissingFieldMask = distanceInfo->missingFieldMask;
        }
    }
    return formattedPattern;
}

int32_t
DateTimePatternGenerator::getTopBitNumber(int32_t foundMask) {
    if ( foundMask==0 ) {
        return 0;
    }
    int32_t i=0;
    while (foundMask!=0) {
        foundMask >>=1;
        ++i;
    }
    if (i-1 >UDATPG_ZONE_FIELD) {
        return UDATPG_ZONE_FIELD;
    }
    else 
        return i-1;
}

void
DateTimePatternGenerator::setAvailableFormat(const char* key, UErrorCode& err) {

    fAvailableFormatKeyHash->puti(key, 1, err);
}

UBool
DateTimePatternGenerator::isAvailableFormatSet(const char* key) {
    int32_t i=0;

    i=fAvailableFormatKeyHash->geti(key);
    if ( i==1 ) {
        return TRUE;
    }
    else{
        return FALSE;
    }
}

void
DateTimePatternGenerator::copyHashtable(Hashtable *other) {

    if (fAvailableFormatKeyHash !=NULL) {
        delete fAvailableFormatKeyHash;
    }
    if (other == NULL) {
        fAvailableFormatKeyHash = NULL;
        return;
    }
    initHashtable(fStatus);
    if(U_FAILURE(fStatus)){
        return;
    }
    int32_t pos = -1;
    const UHashElement* elem = NULL;
    // walk through the hash table and create a deep clone
    while((elem = other->nextElement(pos))!= NULL){
        const UHashTok otherKeyTok = elem->key;
        UnicodeString* otherKey = (UnicodeString*)otherKeyTok.pointer;
        UnicodeString *key = new UnicodeString(*otherKey);
        fAvailableFormatKeyHash->puti(*key, 1, fStatus);
        if(U_FAILURE(fStatus)){
            return;
        }
    }
}

StringEnumeration*
DateTimePatternGenerator::getSkeletons(UErrorCode& status) const {
    StringEnumeration* skeletonEnumerator = new DTSkeletonEnumeration(*patternMap, DT_SKELETON, status);
    return skeletonEnumerator;
}

const UnicodeString&
DateTimePatternGenerator::getPatternForSkeleton(const UnicodeString& skeleton) const {
    UChar baseChar;
    PtnElem *curElem;
    
    if (skeleton.length() ==0) {
        return emptyString;
    }  

    baseChar = skeleton.charAt(0);

    // the baseChar must be A-Z or a-z
    if ( (baseChar >= CAP_A) && (baseChar <= CAP_Z) ) {
        curElem = patternMap->boot[26 + (baseChar-CAP_A)];
    }
    else {
        if ( (baseChar >=LOW_A) && (baseChar <= LOW_Z) ) {
            curElem = patternMap->boot[baseChar-LOW_A];
        }
        else {
            return emptyString;
        }
    }
    while ( curElem != NULL ) {
        if ( curElem->skeleton->getSkeleton()==skeleton ) {
            return curElem->pattern;
        }
        curElem=curElem->next;
    }
    return emptyString;
}

StringEnumeration*
DateTimePatternGenerator::getBaseSkeletons(UErrorCode& status) const {
    StringEnumeration* baseSkeletonEnumerator = new DTSkeletonEnumeration(*patternMap, DT_BASESKELETON, status);
    return baseSkeletonEnumerator;
}

StringEnumeration*
DateTimePatternGenerator::getRedundants(UErrorCode& status) {
    StringEnumeration* output = new DTRedundantEnumeration();
    const UnicodeString *pattern;

    PatternMapIterator it;
    for (it.set(*patternMap); it.hasNext(); ) {
        DateTimeMatcher current = it.next();
        pattern = patternMap->getPatternFromSkeleton(*(it.getSkeleton()));
        if ( isCanonicalItem(*pattern) ) {
            continue;
        }
        skipMatcher = &current;
        UnicodeString trial = getBestPattern(current.getPattern(), status);
        if (trial == *pattern) {   
            ((DTRedundantEnumeration *)output)->add(*pattern, status);
        }
        if (current.equals(skipMatcher)) {
            continue;
        }
    }
    return output;
}

UBool
DateTimePatternGenerator::isCanonicalItem(const UnicodeString& item) {
    if ( item.length() != 1 ) {
        return FALSE;
    }
    for (int32_t i=0; i<UDATPG_FIELD_COUNT; ++i) {
        if (item.charAt(0)==Canonical_Items[i]) {
            return TRUE;
        }
    }
    return FALSE;
}


DateTimePatternGenerator*
DateTimePatternGenerator::clone() const {
    return new DateTimePatternGenerator(*this);
}

PatternMap::PatternMap() {
   for (int32_t i=0; i < MAX_PATTERN_ENTRIES; ++i ) {
      boot[i]=NULL;
   }
   isDupAllowed = TRUE;
}

void
PatternMap::copyFrom(const PatternMap& other, UErrorCode& status) {
    this->isDupAllowed = other.isDupAllowed;
    for (int32_t bootIndex=0; bootIndex<MAX_PATTERN_ENTRIES; ++bootIndex ) {
        PtnElem *curElem, *otherElem, *prevElem=NULL;
        otherElem = other.boot[bootIndex];
        while (otherElem!=NULL) {
            curElem = new PtnElem(otherElem->basePattern, otherElem->pattern);
            if ((this->boot[bootIndex]= curElem) == NULL ) {
                // out of memory
                status = U_MEMORY_ALLOCATION_ERROR;
                return;
            }

            if ((curElem->skeleton=new PtnSkeleton(*(otherElem->skeleton))) == NULL ) {
                // out of memory
                status = U_MEMORY_ALLOCATION_ERROR;
                return;
            }

            if (prevElem!=NULL) {
                prevElem->next=curElem;
            }
            curElem->next=NULL;
            prevElem = curElem;
            otherElem = otherElem->next;
        }

    }
}

PatternMap::~PatternMap() {
   for (int32_t i=0; i < MAX_PATTERN_ENTRIES; ++i ) {
       if (boot[i]!=NULL ) {
           delete boot[i];
           boot[i]=NULL;
       }
   }
}  // PatternMap destructor

void
PatternMap::add(const UnicodeString& basePattern,
                const PtnSkeleton& skeleton,
                const UnicodeString& value,// mapped pattern value
                UErrorCode &status) {
    UChar baseChar = basePattern.charAt(0);
    PtnElem *curElem, *baseElem;
    status = U_ZERO_ERROR;

    // the baseChar must be A-Z or a-z
    if ((baseChar >= CAP_A) && (baseChar <= CAP_Z)) {
        baseElem = boot[baseChar-CAP_A];
    }
    else {
        if ((baseChar >=LOW_A) && (baseChar <= LOW_Z)) {
            baseElem = boot[26+baseChar-LOW_A];
         }
         else {
             status = U_ILLEGAL_CHARACTER;
             return;
         }
    }

    if (baseElem == NULL) {
        if ((curElem = new PtnElem(basePattern, value)) == NULL ) {
            // out of memory
            status = U_MEMORY_ALLOCATION_ERROR;
            return;
        }
        if (baseChar >= LOW_A) {
            Mutex mutex;
            if ( boot[26 + (baseChar-LOW_A)]==NULL ) {
                boot[26 + (baseChar-LOW_A)] = curElem;
            }
            else {
                uprv_free(curElem);
                curElem=NULL;
                baseElem = boot[26 + (baseChar-LOW_A)];
            }
        }
        else {
            Mutex mutex;
            if (boot[baseChar-CAP_A] == NULL ) {
                boot[baseChar-CAP_A] = curElem;
            }
            else {
                uprv_free(curElem);
                curElem=NULL;
                baseElem = boot[baseChar-CAP_A];
            }
        }
        if ( curElem != NULL ) {
            PtnSkeleton* pNewSkeleton = curElem->skeleton = new PtnSkeleton;

            for (int32_t i=0; i<UDATPG_FIELD_COUNT; ++i ) {
                pNewSkeleton->type[i] = skeleton.type[i];
                pNewSkeleton->original[i] = skeleton.original[i];
                pNewSkeleton->baseOriginal[i] = skeleton.baseOriginal[i];
            }
            curElem->next = NULL;
        }
    }
    if ( baseElem != NULL ) {
        curElem = getDuplicateElem(basePattern, skeleton, baseElem);

        if (curElem == NULL) {
            // add new element to the list.
            curElem = baseElem;
            {
                Mutex mutex;
                while( curElem -> next != NULL )
                {
                    curElem = curElem->next;
                }
                if ((curElem->next = new PtnElem(basePattern, value)) == NULL ) {
                    // out of memory
                    status = U_MEMORY_ALLOCATION_ERROR;
                    return;
                }
                curElem=curElem->next;
            }
            PtnSkeleton* pNewSkeleton = curElem->skeleton = new PtnSkeleton;
            for (int32_t i=0; i<UDATPG_FIELD_COUNT; ++i ) {
                pNewSkeleton->type[i] = skeleton.type[i];
                pNewSkeleton->original[i] = skeleton.original[i];
                pNewSkeleton->baseOriginal[i] = skeleton.baseOriginal[i];
            }
            curElem->next = NULL;
        }
        else {
            // Pattern exists in the list already.
            if ( !isDupAllowed ) {
                return;
            }
            // Overwrite the value.
            curElem->pattern = value;
        }
    }
}  // PatternMap::add

// Find the pattern from the given basePattern string.
const UnicodeString *
PatternMap::getPatternFromBasePattern(UnicodeString& basePattern) { // key to search for
   UChar baseChar = basePattern.charAt(0);
   PtnElem *curElem;

   // the baseChar must be A-Z or a-z
   if ( (baseChar >= CAP_A) && (baseChar <= CAP_Z) ) {
     curElem = boot[26 + (baseChar-CAP_A)];
   }
   else {
      if ( (baseChar >=LOW_A) && (baseChar <= LOW_Z) ) {
        curElem = boot[baseChar-LOW_A];
      }
      else
         return NULL;
   }

   if ( curElem == NULL ) {
     return NULL;  // no match
   }

   do  {
     if ( basePattern.compare(curElem->basePattern)==0 ) {
        return &(curElem->pattern);
     }
     curElem=curElem->next;
   }while (curElem != NULL);

   return NULL;
}  // PatternMap::getFromBasePattern


// Find the pattern from the given skeleton.
const UnicodeString *
PatternMap::getPatternFromSkeleton(PtnSkeleton& skeleton) { // key to search for
   PtnElem *curElem;

   // find boot entry
   UChar baseChar='\0';
   for (int32_t i=0; i<UDATPG_FIELD_COUNT; ++i) {
       if (skeleton.baseOriginal[i].length() !=0 ) {
           baseChar = skeleton.baseOriginal[i].charAt(0);
           break;
       }
   }

   // the baseChar must be A-Z or a-z
   if ( (baseChar >= CAP_A) && (baseChar <= CAP_Z) ) {
     curElem = boot[baseChar-CAP_A];
   }
   else {
      if ( (baseChar >=LOW_A) && (baseChar <= LOW_Z) ) {
        curElem = boot[26+baseChar-LOW_A];
      }
      else
         return NULL;
   }

   if ( curElem == NULL ) {
     return NULL;  // no match
   }

   do  {
       int32_t i=0;
       for (i=0; i<UDATPG_FIELD_COUNT; ++i) {
           if (curElem->skeleton->baseOriginal[i].compare(skeleton.baseOriginal[i]) != 0 )
           {
               break;
           }
       }
       if (i == UDATPG_FIELD_COUNT) {
           return &(curElem->pattern);
       }
       curElem=curElem->next;
   }while (curElem != NULL);

   return NULL;
}  

UBool
PatternMap::equals(const PatternMap& other) {
    if ( this==&other ) {
        return TRUE;
    }
    for (int32_t bootIndex=0; bootIndex<MAX_PATTERN_ENTRIES; ++bootIndex ) {
        if ( boot[bootIndex]==other.boot[bootIndex] ) {
            continue;
        }
        if ( (boot[bootIndex]==NULL)||(other.boot[bootIndex]==NULL) ) {
            return FALSE;
        }
        PtnElem *otherElem = other.boot[bootIndex];
        PtnElem *myElem = boot[bootIndex];
        while ((otherElem!=NULL) || (myElem!=NULL)) {
            if ( myElem == otherElem ) {
                break;
            }
            if ((otherElem==NULL) || (myElem==NULL)) {
                return FALSE;
            }
            if ( (myElem->basePattern != otherElem->basePattern) ||
                 (myElem->pattern != otherElem->pattern) ) {
                return FALSE;
            }
            if ((myElem->skeleton!=otherElem->skeleton)&&
                !myElem->skeleton->equals(*(otherElem->skeleton))) {
                return FALSE;
            }
            myElem = myElem->next;
            otherElem=otherElem->next;
        }
    }
    return TRUE;
}

// find any key existing in the mapping table already.
// return TRUE if there is an existing key, otherwise return FALSE.
PtnElem*
PatternMap::getDuplicateElem(
            const UnicodeString &basePattern,
            const PtnSkeleton &skeleton,
            PtnElem *baseElem)  {
   PtnElem *curElem;

   if ( baseElem == (PtnElem *)NULL )  {
         return (PtnElem*)NULL;
   }
   else {
         curElem = baseElem;
   }
   do {
     if ( basePattern.compare(curElem->basePattern)==0 ) {
        UBool isEqual=TRUE;
        for (int32_t i=0; i<UDATPG_FIELD_COUNT; ++i) {
            if (curElem->skeleton->type[i] != skeleton.type[i] ) {
                isEqual=FALSE;
                break;
            }
        }
        if (isEqual) {
            return curElem;
        }
     }
     curElem = curElem->next;
   } while( curElem != (PtnElem *)NULL );

   // end of the list
   return (PtnElem*)NULL;

}  // PatternMap::getDuplicateElem

DateTimeMatcher::DateTimeMatcher(void) {
}


void
DateTimeMatcher::set(const UnicodeString& pattern, FormatParser* fp) {
    PtnSkeleton skeleton;
    return set(pattern, fp, skeleton);
}

void
DateTimeMatcher::set(const UnicodeString& pattern, FormatParser* fp, PtnSkeleton& skeleton) {

    const UChar repeatedPatterns[6]={CAP_G, CAP_E, LOW_Z, LOW_V, CAP_Q, 0}; // "GEzvQ"
    UnicodeString repeatedPattern=UnicodeString(repeatedPatterns);

    for (int32_t i=0; i<UDATPG_FIELD_COUNT; ++i) {
        skeleton.type[i]=NONE;
    }
    fp->set(pattern);
    for (int32_t i=0; i < fp->itemNumber; i++) {
        UnicodeString field = fp->items[i];
        if ( field.charAt(0) == LOW_A ) {
            continue;  // skip 'a'
        }

        if ( fp->isQuoteLiteral(field) ) {
            UnicodeString quoteLiteral="";
            fp->getQuoteLiteral(quoteLiteral, &i);
            continue;
        }
        int32_t canonicalIndex = fp->getCanonicalIndex(field);
        if (canonicalIndex < 0 ) {
            continue;
        }
        const dtTypeElem *row = &dtTypes[canonicalIndex];
        int32_t typeValue = row->field;
        skeleton.original[typeValue]=field;
        UChar repeatChar = row->patternChar;
        int32_t repeatCount = row->minLen > 3 ? 3: row->minLen;
        while (repeatCount-- > 0) {
            skeleton.baseOriginal[typeValue] += repeatChar;
        }
        int16_t subTypeValue = row->type;
        if ( row->type > 0) {
            subTypeValue += field.length();
        }
        skeleton.type[typeValue] = (char)subTypeValue;
    }
    {
        Mutex mutex;
        for (int32_t i=0; i<UDATPG_FIELD_COUNT; ++i) {
            this->skeleton.type[i] = skeleton.type[i];
            this->skeleton.baseOriginal[i] =  skeleton.baseOriginal[i];
            this->skeleton.original[i] = skeleton.original[i];
        }
    }

    return;
}

void
DateTimeMatcher::getBasePattern(UnicodeString &result ) {
    result.remove(); // Reset the result first.
    for (int32_t i=0; i<UDATPG_FIELD_COUNT; ++i ) {
        if (skeleton.baseOriginal[i].length()!=0) {
            result += skeleton.baseOriginal[i];
        }
    }
    return;
}

UnicodeString
DateTimeMatcher::getPattern() {
    UnicodeString result;
    
    for (int32_t i=0; i<UDATPG_FIELD_COUNT; ++i ) {
        if (skeleton.original[i].length()!=0) {
            result += skeleton.original[i];
        }
    }
    return result;
}

int32_t
DateTimeMatcher::getDistance(DateTimeMatcher& other, int32_t includeMask, DistanceInfo& distanceInfo) {
    int32_t result=0;
    distanceInfo.clear();
    for (int32_t i=0; i<UDATPG_FIELD_COUNT; ++i ) {
        int32_t myType = (includeMask&(1<<i))==0 ? 0 : skeleton.type[i];
        int32_t otherType = other.skeleton.type[i];
        if (myType==otherType) {
            continue;
        }
        if (myType==0) {// and other is not
            result += EXTRA_FIELD;
            distanceInfo.addExtra(i);
        }
        else {
            if (otherType==0) {
                result += MISSING_FIELD;
                distanceInfo.addMissing(i);
            }
            else {
                result += abs(myType - otherType);
            }
        }

    }
    return result;
}

void
DateTimeMatcher::copyFrom(PtnSkeleton& skeleton) {
    for (int32_t i=0; i<UDATPG_FIELD_COUNT; ++i) {
        this->skeleton.type[i]=skeleton.type[i];
        this->skeleton.original[i]=skeleton.original[i];
        this->skeleton.baseOriginal[i]=skeleton.baseOriginal[i];
    }
    return;
}

void
DateTimeMatcher::copyFrom() {
    // same as clear
    for (int32_t i=0; i<UDATPG_FIELD_COUNT; ++i) {
        this->skeleton.type[i]=0;
        this->skeleton.original[i]="";
        this->skeleton.baseOriginal[i]="";
    }
    return;
}

UBool
DateTimeMatcher::equals(DateTimeMatcher* other) {
    if (other==NULL) {
        return FALSE;
    }
    for (int32_t i=0; i<UDATPG_FIELD_COUNT; ++i) {
        if (this->skeleton.original[i]!=other->skeleton.original[i] ) {
            return FALSE;
        }
    }
    return TRUE;
}

int32_t
DateTimeMatcher::getFieldMask() {
    int32_t result=0;

    for (int32_t i=0; i<UDATPG_FIELD_COUNT; ++i) {
        if (skeleton.type[i]!=0) {
            result |= (1<<i);
        }
    }
    return result;
}

PtnSkeleton*
DateTimeMatcher::getSkeletonPtr() {
    return &skeleton;
}

FormatParser::FormatParser () {
    status = START;
    itemNumber=0;
    quoteFilter = NULL;
}


FormatParser::~FormatParser () {
    delete quoteFilter;
}


// Find the next token with the starting position and length
// Note: the startPos may
FormatParser::TokenStatus
FormatParser::setTokens(const UnicodeString& pattern, int32_t startPos, int32_t *len) {
    int32_t  curLoc = startPos;
    if ( curLoc >= pattern.length()) {
        return DONE;
    }
    // check the current char is between A-Z or a-z
    do {
        UChar c=pattern.charAt(curLoc);
        if ( (c>=CAP_A && c<=CAP_Z) || (c>=LOW_A && c<=LOW_Z) ) {
           curLoc++;
        }
        else {
               startPos = curLoc;
               *len=1;
               return ADD_TOKEN;
        }

        if ( pattern.charAt(curLoc)!= pattern.charAt(startPos) ) {
            break;  // not the same token
        }
    } while(curLoc <= pattern.length());
    *len = curLoc-startPos;
    return ADD_TOKEN;
}

void
FormatParser::set(const UnicodeString& pattern) {
    int32_t startPos=0;
    TokenStatus result=START;
    int32_t len=0;
    itemNumber =0;

    do {
        result = setTokens( pattern, startPos, &len );
        if ( result == ADD_TOKEN )
        {
            items[itemNumber++] = UnicodeString(pattern, startPos, len );
            startPos += len;
        }
        else {
            break;
        }
    } while (result==ADD_TOKEN && itemNumber < MAX_DT_TOKEN);
}

int
FormatParser::getCanonicalIndex(const UnicodeString& s) {
    int32_t len = s.length();
    UChar ch = s.charAt(0);
    int32_t i=0;

    while (dtTypes[i].patternChar!='\0') {
        if ( dtTypes[i].patternChar!=ch ) {
            ++i;
            continue;
        }
        if (dtTypes[i].patternChar!=dtTypes[i+1].patternChar) {
            return i;
        }
        if (dtTypes[i+1].minLen <= len) {
            ++i;
            continue;
        }
        return i;
    }
    return -1;
}

UBool
FormatParser::isQuoteLiteral(UnicodeString s) {
    if ((s.charAt(0)==SINGLE_QUOTE)||(s.charAt(0)==FORWARDSLASH)||(s.charAt(0)==BACKSLASH) ||
        (s.charAt(0)==SPACE) ||(s.charAt(0)==COMMA) ||(s.charAt(0)==HYPHEN) ||(s.charAt(0)==DOT) ) {
        return TRUE;
    }
    else {
        return FALSE;
    }
}

// This function aussumes the current itemIndex points to the quote literal.
// Please call isQuoteLiteral prior to this function.
void
FormatParser::getQuoteLiteral(UnicodeString& quote, int32_t *itemIndex) {
    UErrorCode status = U_ZERO_ERROR;
    int32_t i=*itemIndex;
    
    if ( quoteFilter == NULL ) {
        setFilter(status);
    }
    if ((items[i].charAt(0)==FORWARDSLASH) || (items[i].charAt(0)==BACKSLASH) || (items[i].charAt(0)==SPACE) ||
        (items[i].charAt(0)==COMMA) ||(items[i].charAt(0)==HYPHEN) ||(items[i].charAt(0)==DOT)) {
        quote += items[i];
        return;
    }
    if (items[i].charAt(0)==SINGLE_QUOTE) {
        quote += items[i];
        ++i;
    }
    while ( i < itemNumber ) {
        if ( items[i].charAt(0)==SINGLE_QUOTE ) {
            if ( items[i+1].charAt(0)==SINGLE_QUOTE ) {
                // two single quotes e.g. 'o''clock'
                quote += items[++i];
                continue;
            }
            else {
                // TODO turn off the filter 
                //if ( quoteFilter->contains(items[i].charAt(0)) ) {
                    // TODO add error checking here
                    quote += items[i];
                //}
                break;
            }

        }
        else {
            quote += items[i];
        }
        ++i;
    }
    *itemIndex=i;
    return;
}

UBool
FormatParser::isPatternSeparator(UnicodeString& field) {
    for (int32_t i=0; i<field.length(); ++i ) {
        UChar c= field.charAt(i);
        if ( (c==SINGLE_QUOTE) || (c==BACKSLASH) || (c==SPACE) || (c==COLON) ||
             (c==QUOTATION_MARK) || (c==COMMA) || (c==HYPHEN) ) {
            continue;
        }
        else {
            return FALSE;
        }
    }
    return TRUE;
}

void
FormatParser::setFilter(UErrorCode &status) { 
    quoteFilter = new UnicodeSet(quotingPattern, status);
}

void
DistanceInfo::setTo(DistanceInfo &other) {
    missingFieldMask = other.missingFieldMask;
    extraFieldMask= other.extraFieldMask;
    return;
}

PatternMapIterator::PatternMapIterator() {
    bootIndex = 0;
    nodePtr = NULL;
    patternMap=NULL;
    matcher= new DateTimeMatcher();
}


PatternMapIterator::~PatternMapIterator() {
    delete matcher;
}

void
PatternMapIterator::set(PatternMap& patternMap) {
    /*
    for (int32_t i=0; i<MAX_PATTERN_ENTRIES; ++i ) {
        this->boot[i]=patternMap->boot[i];
    }
    */
    this->patternMap=&patternMap;
}

PtnSkeleton* 
PatternMapIterator::getSkeleton() {
    if ( nodePtr == NULL ) {
        return NULL;
    }
    else {
        return nodePtr->skeleton;
    }
}

UBool
PatternMapIterator::hasNext() {
    int32_t headIndex=bootIndex;
    PtnElem *curPtr=nodePtr;

    if (patternMap==NULL) {
        return FALSE;
    }
    while ( headIndex < MAX_PATTERN_ENTRIES ) {
        if ( curPtr != NULL ) {
            if ( curPtr->next != NULL ) {
                return TRUE;
            }
            else {
                headIndex++;
                curPtr=NULL;
                continue;
            }
        }
        else {
            if ( patternMap->boot[headIndex] != NULL ) {
                return TRUE;
            }
            else {
                headIndex++;
                continue;
            }
        }

    }
    return FALSE;
}

DateTimeMatcher&
PatternMapIterator::next() {
    while ( bootIndex < MAX_PATTERN_ENTRIES ) {
        if ( nodePtr != NULL ) {
            if ( nodePtr->next != NULL ) {
                nodePtr = nodePtr->next;
                break;
            }
            else {
                bootIndex++;
                nodePtr=NULL;
                continue;
            }
        }
        else {
            if ( patternMap->boot[bootIndex] != NULL ) {
                nodePtr = patternMap->boot[bootIndex];
                break;
            }
            else {
                bootIndex++;
                continue;
            }
        }
    }
    if (nodePtr!=NULL) {
        matcher->copyFrom(*nodePtr->skeleton);
    }
    else {
        matcher->copyFrom();
    }
    return *matcher;
}

PtnSkeleton::PtnSkeleton() : UObject() {
}


PtnSkeleton::PtnSkeleton(PtnSkeleton& other) : UObject() {
    for (int32_t i=0; i<UDATPG_FIELD_COUNT; ++i) {
        this->type[i]=other.type[i];
        this->original[i]=other.original[i];
        this->baseOriginal[i]=other.baseOriginal[i];
    }
}

UBool
PtnSkeleton::equals(PtnSkeleton& other)  {
    for (int32_t i=0; i<UDATPG_FIELD_COUNT; ++i) {
        if ( (type[i]!= other.type[i]) ||
             (original[i]!=other.original[i]) ||
             (baseOriginal[i]!=other.baseOriginal[i]) ) {
            return FALSE;
        }
    }
    return TRUE;
}

UnicodeString
PtnSkeleton::getSkeleton() {
    UnicodeString result;

    for(int32_t i=0; i< UDATPG_FIELD_COUNT; ++i) {
        if (original[i].length()!=0) {
            result += original[i];
        }
    }
    return result;
}

PtnSkeleton::~PtnSkeleton() {
}

PtnElem::PtnElem(const UnicodeString &basePat, const UnicodeString &pat) : 
basePattern(basePat),
pattern(pat)
{
    skeleton=NULL;
    next=NULL;
}

PtnElem::~PtnElem() {

    if (next!=NULL) {
        delete next;
    }
    delete skeleton;
}

DTSkeletonEnumeration::DTSkeletonEnumeration(PatternMap &patternMap, dtStrEnum type, UErrorCode& status) {
    PtnElem  *curElem;
    PtnSkeleton *curSkeleton;
    UnicodeString s;
    int32_t bootIndex;

    pos=0;
    fSkeletons = new UVector(status);
    if (U_FAILURE(status)) {
        delete fSkeletons;
        return;
    }
    for (bootIndex=0; bootIndex<MAX_PATTERN_ENTRIES; ++bootIndex ) {
        curElem = patternMap.boot[bootIndex];
        while (curElem!=NULL) {
            switch(type) {
                case DT_BASESKELETON:
                    s=curElem->basePattern;
                    break;
                case DT_PATTERN:
                    s=curElem->pattern;
                    break;
                case DT_SKELETON:
                    curSkeleton=curElem->skeleton;
                    s=curSkeleton->getSkeleton();
                    break;
            }
            if ( !isCanonicalItem(s) ) {
                fSkeletons->addElement(new UnicodeString(s), status);
                if (U_FAILURE(status)) {
                    delete fSkeletons;
                    fSkeletons = NULL;
                    return;
                }
            }
            curElem = curElem->next;
        }
    }
    if ((bootIndex==MAX_PATTERN_ENTRIES) && (curElem!=NULL) ) {
        status = U_BUFFER_OVERFLOW_ERROR;
    }
}

const UnicodeString*
DTSkeletonEnumeration::snext(UErrorCode& status) {
    if (U_SUCCESS(status) && pos < fSkeletons->size()) {
        return (const UnicodeString*)fSkeletons->elementAt(pos++);
    }
    return NULL;
}

void
DTSkeletonEnumeration::reset(UErrorCode& status) {
    pos=0;
}

int32_t
DTSkeletonEnumeration::count(UErrorCode& status) const {
       return (fSkeletons==NULL) ? 0 : fSkeletons->size();
   }
UBool
DTSkeletonEnumeration::isCanonicalItem(const UnicodeString& item) {
    if ( item.length() != 1 ) {
        return FALSE;
    }
    for (int32_t i=0; i<UDATPG_FIELD_COUNT; ++i) {
        if (item.charAt(0)==Canonical_Items[i]) {
            return TRUE;
        }
    }
    return FALSE;
}

DTSkeletonEnumeration::~DTSkeletonEnumeration() {
    UnicodeString *s;
    for (int32_t i=0; i<fSkeletons->size(); ++i) {
        if ((s=(UnicodeString *)fSkeletons->elementAt(i))!=NULL) {
            delete s;
        }
    }
    delete fSkeletons;
}

DTRedundantEnumeration::DTRedundantEnumeration() {
    pos=0;
    fPatterns = NULL;
}

void
DTRedundantEnumeration::add(const UnicodeString& pattern, UErrorCode& status) {
    if ( (fPatterns == NULL) && U_SUCCESS(status) ) {
        fPatterns = new UVector(status);
        if (U_FAILURE(status)) {
            delete fPatterns;
            fPatterns = NULL;
            return;
       }
    }
    fPatterns->addElement(new UnicodeString(pattern), status);
    if (U_FAILURE(status)) {
        delete fPatterns;
        fPatterns = NULL;
        return;
    }
}

const UnicodeString*
DTRedundantEnumeration::snext(UErrorCode& status) {
    if (U_SUCCESS(status) && pos < fPatterns->size()) {
        return (const UnicodeString*)fPatterns->elementAt(pos++);
    }
    return NULL;
}

void
DTRedundantEnumeration::reset(UErrorCode& status) {
    pos=0;
}

int32_t
DTRedundantEnumeration::count(UErrorCode& status) const {
       return (fPatterns==NULL) ? 0 : fPatterns->size();
}

UBool
DTRedundantEnumeration::isCanonicalItem(const UnicodeString& item) {
    if ( item.length() != 1 ) {
        return FALSE;
    }
    for (int32_t i=0; i<UDATPG_FIELD_COUNT; ++i) {
        if (item.charAt(0)==Canonical_Items[i]) {
            return TRUE;
        }
    }
    return FALSE;
}

DTRedundantEnumeration::~DTRedundantEnumeration() {
    UnicodeString *s;
    for (int32_t i=0; i<fPatterns->size(); ++i) {
        if ((s=(UnicodeString *)fPatterns->elementAt(i))!=NULL) {
            delete s;
        }
    }
    delete fPatterns;
}

U_NAMESPACE_END


#endif /* #if !UCONFIG_NO_FORMATTING */

//eof
