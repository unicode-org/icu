/*
******************************************************************************
* Copyright (C) 2014, International Business Machines Corporation and         
* others. All Rights Reserved.                                                
******************************************************************************
*                                                                             
* File RELDATEFMT.CPP                                                             
******************************************************************************
*/

#include "unicode/reldatefmt.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/localpointer.h"
#include "quantityformatter.h"
#include "unicode/plurrule.h"
#include "unicode/msgfmt.h"
#include "unicode/decimfmt.h"
#include "unicode/numfmt.h"
#include "lrucache.h"
#include "uresimp.h"
#include "unicode/ures.h"
#include "cstring.h"
#include "plurrule_impl.h"
#include "ucln_in.h"
#include "mutex.h"
#include "charstr.h"

#include "sharedptr.h"

// Copied from uscript_props.cpp
#define LENGTHOF(array) (int32_t)(sizeof(array)/sizeof((array)[0]))

static icu::LRUCache *gCache = NULL;
static UMutex gCacheMutex = U_MUTEX_INITIALIZER;
static icu::UInitOnce gCacheInitOnce = U_INITONCE_INITIALIZER;

U_CDECL_BEGIN
static UBool U_CALLCONV reldatefmt_cleanup() {
    gCacheInitOnce.reset();
    if (gCache) {
        delete gCache;
        gCache = NULL;
    }
    return TRUE;
}
U_CDECL_END

U_NAMESPACE_BEGIN

class QualitativeUnits : public UMemory {
public:
    QualitativeUnits() { }
    UnicodeString data[UDAT_ABSOLUTE_UNIT_COUNT][UDAT_DIRECTION_COUNT];
private:
    QualitativeUnits(const QualitativeUnits &other);
    QualitativeUnits &operator=(const QualitativeUnits& other);
};

class QuantitativeUnits : public UMemory {
public:
    QuantitativeUnits() { }
    QuantityFormatter data[UDAT_RELATIVE_UNIT_COUNT][2];
private:
    QuantitativeUnits(const QuantitativeUnits &other);
    QuantitativeUnits &operator=(const QuantitativeUnits& other);
};

class RelativeDateTimeData : public SharedObject {
public:
    SharedPtr<QualitativeUnits> qualitativeUnits;
    SharedPtr<QuantitativeUnits> quantitativeUnits;
    SharedPtr<MessageFormat> combinedDateAndTime;
    SharedPtr<PluralRules> pluralRules;
    SharedPtr<NumberFormat> numberFormat;
    virtual ~RelativeDateTimeData();
private:
    RelativeDateTimeData &operator=(const RelativeDateTimeData& other);
};

RelativeDateTimeData::~RelativeDateTimeData() {
}

static UBool getStringWithFallback(
        const UResourceBundle *resource, 
        const char *key,
        UnicodeString &result,
        UErrorCode &status) {
    int32_t len = 0;
    const UChar *resStr = ures_getStringByKeyWithFallback(
        resource, key, &len, &status);
    if (U_FAILURE(status)) {
        return FALSE;
    }
    result.setTo(TRUE, resStr, len);
    return TRUE;
}

static UBool getOptionalStringWithFallback(
        const UResourceBundle *resource, 
        const char *key,
        UnicodeString &result,
        UErrorCode &status) {
    if (U_FAILURE(status)) {
        return FALSE;
    }
    int32_t len = 0;
    const UChar *resStr = ures_getStringByKey(
        resource, key, &len, &status);
    if (status == U_MISSING_RESOURCE_ERROR) {
        result.remove();
        status = U_ZERO_ERROR;
        return TRUE;
    }
    if (U_FAILURE(status)) {
        return FALSE;
    }
    result.setTo(TRUE, resStr, len);
    return TRUE;
}

static UBool getString(
        const UResourceBundle *resource, 
        UnicodeString &result,
        UErrorCode &status) {
    int32_t len = 0;
    const UChar *resStr = ures_getString(resource, &len, &status);
    if (U_FAILURE(status)) {
        return FALSE;
    }
    result.setTo(TRUE, resStr, len);
    return TRUE;
}

static UBool getStringByIndex(
        const UResourceBundle *resource, 
        int32_t idx,
        UnicodeString &result,
        UErrorCode &status) {
    int32_t len = 0;
    const UChar *resStr = ures_getStringByIndex(
            resource, idx, &len, &status);
    if (U_FAILURE(status)) {
        return FALSE;
    }
    result.setTo(TRUE, resStr, len);
    return TRUE;
}

static void addQualitativeUnit(
            const UResourceBundle *resource,
            UDateAbsoluteUnit absoluteUnit,
            const UnicodeString &unitName,
            QualitativeUnits &qualitativeUnits,
            UErrorCode &status) {
    getStringWithFallback(
            resource, 
            "-1",
            qualitativeUnits.data[absoluteUnit][UDAT_DIRECTION_LAST],
            status);
    getStringWithFallback(
            resource, 
            "0",
            qualitativeUnits.data[absoluteUnit][UDAT_DIRECTION_THIS],
            status);
    getStringWithFallback(
            resource, 
            "1",
            qualitativeUnits.data[absoluteUnit][UDAT_DIRECTION_NEXT],
            status);
    getOptionalStringWithFallback(
            resource,
            "-2",
            qualitativeUnits.data[absoluteUnit][UDAT_DIRECTION_LAST_2],
            status);
    getOptionalStringWithFallback(
            resource,
            "2",
            qualitativeUnits.data[absoluteUnit][UDAT_DIRECTION_NEXT_2],
            status);
    qualitativeUnits.data[absoluteUnit][UDAT_DIRECTION_PLAIN] = unitName;
}

static void addTimeUnit(
        const UResourceBundle *resource,
        UDateRelativeUnit relativeUnit,
        int32_t pastOrFuture,
        QuantitativeUnits &quantitativeUnits,
        UErrorCode &status) {
    if (U_FAILURE(status)) {
        return;
    }
    int32_t size = ures_getSize(resource);
    for (int32_t i = 0; i < size; ++i) {
        LocalUResourceBundlePointer pluralBundle(
                ures_getByIndex(resource, i, NULL, &status));
        if (U_FAILURE(status)) {
            return;
        }
        UnicodeString rawPattern;
        if (!getString(pluralBundle.getAlias(), rawPattern, status)) {
            return;
        }
        if (!quantitativeUnits.data[relativeUnit][pastOrFuture]
                .add(
                        ures_getKey(pluralBundle.getAlias()),
                        rawPattern,
                        status)) {
            return;
        }
    }
}

static void addTimeUnit(
        const UResourceBundle *resource,
        UDateRelativeUnit relativeUnit,
        QuantitativeUnits &quantitativeUnits,
        UErrorCode &status) {
    LocalUResourceBundlePointer topLevel(
            ures_getByKeyWithFallback(
                    resource, "relativeTime", NULL, &status));
    if (U_FAILURE(status)) {
        return;
    }
    LocalUResourceBundlePointer futureBundle(ures_getByKeyWithFallback(
            topLevel.getAlias(), "future", NULL, &status));
    if (U_FAILURE(status)) {
        return;
    }
    addTimeUnit(
            futureBundle.getAlias(),
            relativeUnit,
            1,
            quantitativeUnits,
            status);
    LocalUResourceBundlePointer pastBundle(ures_getByKeyWithFallback(
            topLevel.getAlias(), "past", NULL, &status));
    if (U_FAILURE(status)) {
        return;
    }
    addTimeUnit(
            pastBundle.getAlias(),
            relativeUnit,
            0,
            quantitativeUnits,
            status);
}

static void addTimeUnit(
        const UResourceBundle *resource,
        const char *path,
        UDateRelativeUnit relativeUnit,
        QuantitativeUnits &quantitativeUnits,
        UErrorCode &status) {
    LocalUResourceBundlePointer topLevel(
            ures_getByKeyWithFallback(resource, path, NULL, &status));
    if (U_FAILURE(status)) {
        return;
    }
    addTimeUnit(topLevel.getAlias(), relativeUnit, quantitativeUnits, status);
}

static void addTimeUnit(
        const UResourceBundle *resource,
        const char *path,
        UDateRelativeUnit relativeUnit,
        UDateAbsoluteUnit absoluteUnit,
        QuantitativeUnits &quantitativeUnits,
        QualitativeUnits &qualitativeUnits,
        UErrorCode &status) {
    LocalUResourceBundlePointer topLevel(
            ures_getByKeyWithFallback(resource, path, NULL, &status));
    if (U_FAILURE(status)) {
        return;
    }
    addTimeUnit(topLevel.getAlias(), relativeUnit, quantitativeUnits, status);
    UnicodeString unitName;
    if (!getStringWithFallback(topLevel.getAlias(), "dn", unitName, status)) {
        return;
    }
    // TODO(Travis Keep): This is a hack to get around CLDR bug 6818.
    const char *localeId = ures_getLocaleByType(
            topLevel.getAlias(), ULOC_ACTUAL_LOCALE, &status);
    if (U_FAILURE(status)) {
        return;
    }
    Locale locale(localeId);
    if (uprv_strcmp("en", locale.getLanguage()) == 0) {
         unitName.toLower();
    }
    // end hack
    ures_getByKeyWithFallback(
            topLevel.getAlias(), "relative", topLevel.getAlias(), &status);
    if (U_FAILURE(status)) {
        return;
    }
    addQualitativeUnit(
            topLevel.getAlias(),
            absoluteUnit,
            unitName,
            qualitativeUnits,
            status);
}

static void readDaysOfWeek(
        const UResourceBundle *resource,
        const char *path,
        UnicodeString *daysOfWeek,
        UErrorCode &status) {
    LocalUResourceBundlePointer topLevel(
            ures_getByKeyWithFallback(resource, path, NULL, &status));
    if (U_FAILURE(status)) {
        return;
    }
    int32_t size = ures_getSize(topLevel.getAlias());
    if (size != 7) {
        status = U_INTERNAL_PROGRAM_ERROR;
        return;
    }
    for (int32_t i = 0; i < size; ++i) {
        if (!getStringByIndex(topLevel.getAlias(), i, daysOfWeek[i], status)) {
            return;
        }
    }
}

static void addWeekDay(
        const UResourceBundle *resource,
        const char *path,
        const UnicodeString *daysOfWeek,
        UDateAbsoluteUnit absoluteUnit,
        QualitativeUnits &qualitativeUnits,
        UErrorCode &status) {
    LocalUResourceBundlePointer topLevel(
            ures_getByKeyWithFallback(resource, path, NULL, &status));
    if (U_FAILURE(status)) {
        return;
    }
    addQualitativeUnit(
            topLevel.getAlias(),
            absoluteUnit,
            daysOfWeek[absoluteUnit - UDAT_ABSOLUTE_SUNDAY],
            qualitativeUnits,
            status);
}

static UBool load(
        const UResourceBundle *resource,
        QualitativeUnits &qualitativeUnits,
        QuantitativeUnits &quantitativeUnits,
        UErrorCode &status) {
    addTimeUnit(
            resource,
            "fields/day",
            UDAT_RELATIVE_DAYS,
            UDAT_ABSOLUTE_DAY,
            quantitativeUnits,
            qualitativeUnits,
            status);
    addTimeUnit(
            resource,
            "fields/week",
            UDAT_RELATIVE_WEEKS,
            UDAT_ABSOLUTE_WEEK,
            quantitativeUnits,
            qualitativeUnits,
            status);
    addTimeUnit(
            resource,
            "fields/month",
            UDAT_RELATIVE_MONTHS,
            UDAT_ABSOLUTE_MONTH,
            quantitativeUnits,
            qualitativeUnits,
            status);
    addTimeUnit(
            resource,
            "fields/year",
            UDAT_RELATIVE_YEARS,
            UDAT_ABSOLUTE_YEAR,
            quantitativeUnits,
            qualitativeUnits,
            status);
    addTimeUnit(
            resource,
            "fields/second",
            UDAT_RELATIVE_SECONDS,
            quantitativeUnits,
            status);
    addTimeUnit(
            resource,
            "fields/minute",
            UDAT_RELATIVE_MINUTES,
            quantitativeUnits,
            status);
    addTimeUnit(
            resource,
            "fields/hour",
            UDAT_RELATIVE_HOURS,
            quantitativeUnits,
            status);
    getStringWithFallback(
            resource,
            "fields/second/relative/0",
            qualitativeUnits.data[UDAT_ABSOLUTE_NOW][UDAT_DIRECTION_PLAIN],
            status);
    UnicodeString daysOfWeek[7];
    readDaysOfWeek(
            resource,
            "calendar/gregorian/dayNames/stand-alone/wide",
            daysOfWeek,
            status);
    addWeekDay(
            resource,
            "fields/mon/relative",
            daysOfWeek,
            UDAT_ABSOLUTE_MONDAY,
            qualitativeUnits,
            status);
    addWeekDay(
            resource,
            "fields/tue/relative",
            daysOfWeek,
            UDAT_ABSOLUTE_TUESDAY,
            qualitativeUnits,
            status);
    addWeekDay(
            resource,
            "fields/wed/relative",
            daysOfWeek,
            UDAT_ABSOLUTE_WEDNESDAY,
            qualitativeUnits,
            status);
    addWeekDay(
            resource,
            "fields/thu/relative",
            daysOfWeek,
            UDAT_ABSOLUTE_THURSDAY,
            qualitativeUnits,
            status);
    addWeekDay(
            resource,
            "fields/fri/relative",
            daysOfWeek,
            UDAT_ABSOLUTE_FRIDAY,
            qualitativeUnits,
            status);
    addWeekDay(
            resource,
            "fields/sat/relative",
            daysOfWeek,
            UDAT_ABSOLUTE_SATURDAY,
            qualitativeUnits,
            status);
    addWeekDay(
            resource,
            "fields/sun/relative",
            daysOfWeek,
            UDAT_ABSOLUTE_SUNDAY,
            qualitativeUnits,
            status);
    return U_SUCCESS(status);
}

static UBool getDateTimePattern(
        const UResourceBundle *resource,
        UnicodeString &result,
        UErrorCode &status) {
    UnicodeString defaultCalendarName;
    if (!getStringWithFallback(
            resource,
            "calendar/default",
            defaultCalendarName,
            status)) {
        return FALSE;
    }
    CharString pathBuffer;
    pathBuffer.append("calendar/", status)
            .appendInvariantChars(defaultCalendarName, status)
            .append("/DateTimePatterns", status);
    LocalUResourceBundlePointer topLevel(
            ures_getByKeyWithFallback(
                    resource, pathBuffer.data(), NULL, &status));
    if (U_FAILURE(status)) {
        return FALSE;
    }
    int32_t size = ures_getSize(topLevel.getAlias());
    if (size <= 8) {
        // Oops, size is to small to access the index that we want, fallback
        // to a hard-coded value.
        result = UNICODE_STRING_SIMPLE("{1} {0}");
        return TRUE;
    }
    return getStringByIndex(topLevel.getAlias(), 8, result, status);
}

static SharedObject *U_CALLCONV createData(
        const char *localeId, UErrorCode &status) {
    LocalUResourceBundlePointer topLevel(ures_open(NULL, localeId, &status));
    if (U_FAILURE(status)) {
        return NULL;
    }
    LocalPointer<RelativeDateTimeData> result(new RelativeDateTimeData());
    LocalPointer<QualitativeUnits> qualitativeUnits(new QualitativeUnits());
    LocalPointer<QuantitativeUnits> quantitativeUnits(new QuantitativeUnits());
    if (result.getAlias() == NULL
            || qualitativeUnits.getAlias() == NULL
            || quantitativeUnits.getAlias() == NULL) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }
    if (!load(
            topLevel.getAlias(),
            *qualitativeUnits,
            *quantitativeUnits,
            status)) {
        return NULL;
    }
    if (!result->qualitativeUnits.reset(qualitativeUnits.orphan())) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }
    if (!result->quantitativeUnits.reset(quantitativeUnits.orphan())) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }
        
    
    UnicodeString dateTimePattern;
    if (!getDateTimePattern(topLevel.getAlias(), dateTimePattern, status)) {
        return NULL;
    }
    LocalPointer<MessageFormat> mf(
            new MessageFormat(dateTimePattern, localeId, status));
    if (U_FAILURE(status)) {
        return NULL;
    }
    if (mf.getAlias() == NULL) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }
    if (!result->combinedDateAndTime.reset(mf.orphan())) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }
    LocalPointer<PluralRules> pr(PluralRules::forLocale(localeId, status));
    if (U_FAILURE(status)) {
        return NULL;
    }
    if (!result->pluralRules.reset(pr.orphan())) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }
    LocalPointer<NumberFormat> nf(
            NumberFormat::createInstance(localeId, status));
    if (U_FAILURE(status)) {
        return NULL;
    }
    if (!result->numberFormat.reset(nf.orphan())) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }
    return result.orphan();
}

static void U_CALLCONV cacheInit(UErrorCode &status) {
    U_ASSERT(gCache == NULL);
    ucln_i18n_registerCleanup(UCLN_I18N_RELDATEFMT, reldatefmt_cleanup);
    gCache = new SimpleLRUCache(100, &createData, status);
    if (U_FAILURE(status)) {
        delete gCache;
        gCache = NULL;
    }
}

static void getFromCache(
        const char *locale,
        const RelativeDateTimeData *&ptr,
        UErrorCode &status) {
    umtx_initOnce(gCacheInitOnce, &cacheInit, status);
    if (U_FAILURE(status)) {
        return;
    }
    Mutex lock(&gCacheMutex);
    gCache->get(locale, ptr, status);
}

RelativeDateTimeFormatter::RelativeDateTimeFormatter(UErrorCode& status)
        : ptr(NULL) {
    getFromCache(Locale::getDefault().getName(), ptr, status);
}

RelativeDateTimeFormatter::RelativeDateTimeFormatter(
        const Locale& locale, UErrorCode& status) : ptr(NULL) {
    getFromCache(locale.getName(), ptr, status);
}

RelativeDateTimeFormatter::RelativeDateTimeFormatter(
        const Locale& locale, NumberFormat *nfToAdopt, UErrorCode& status)
        : ptr(NULL) {
    getFromCache(locale.getName(), ptr, status);
    if (U_FAILURE(status)) {
        return;
    }
    RelativeDateTimeData* wptr = SharedObject::copyOnWrite(ptr);
    if (wptr == NULL) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
    if (!wptr->numberFormat.reset(nfToAdopt)) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
}

RelativeDateTimeFormatter::RelativeDateTimeFormatter(
        const RelativeDateTimeFormatter& other) : ptr(other.ptr) {
    ptr->addRef();
}

RelativeDateTimeFormatter& RelativeDateTimeFormatter::operator=(
        const RelativeDateTimeFormatter& other) {
    if (this != &other) {
        SharedObject::copyPtr(other.ptr, ptr);
    }
    return *this;
}

RelativeDateTimeFormatter::~RelativeDateTimeFormatter() {
    if (ptr != NULL) {
        ptr->removeRef();
    }
}

const NumberFormat& RelativeDateTimeFormatter::getNumberFormat() const {
    return *ptr->numberFormat;
}

UnicodeString& RelativeDateTimeFormatter::format(
        double quantity, UDateDirection direction, UDateRelativeUnit unit,
        UnicodeString& appendTo, UErrorCode& status) const {
    if (U_FAILURE(status)) {
        return appendTo;
    }
    if (direction != UDAT_DIRECTION_LAST && direction != UDAT_DIRECTION_NEXT) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return appendTo;
    }
    int32_t bFuture = direction == UDAT_DIRECTION_NEXT ? 1 : 0;
    return ptr->quantitativeUnits->data[unit][bFuture].format(
            quantity,
            *ptr->numberFormat,
            *ptr->pluralRules,
            appendTo,
            status);
}

UnicodeString& RelativeDateTimeFormatter::format(
        UDateDirection direction, UDateAbsoluteUnit unit,
        UnicodeString& appendTo, UErrorCode& status) const {
    if (U_FAILURE(status)) {
        return appendTo;
    }
    if (unit == UDAT_ABSOLUTE_NOW && direction != UDAT_DIRECTION_PLAIN) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return appendTo;
    }
    return appendTo.append(ptr->qualitativeUnits->data[unit][direction]);
}

UnicodeString& RelativeDateTimeFormatter::combineDateAndTime(
    const UnicodeString& relativeDateString, const UnicodeString& timeString,
    UnicodeString& appendTo, UErrorCode& status) const {
    Formattable args[2] = {timeString, relativeDateString};
    FieldPosition fpos(0);
    return ptr->combinedDateAndTime->format(args, 2, appendTo, fpos, status);
}

U_NAMESPACE_END

#endif /* !UCONFIG_NO_FORMATTING */

