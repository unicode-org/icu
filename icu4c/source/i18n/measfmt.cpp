/*
**********************************************************************
* Copyright (c) 2004-2014, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Alan Liu
* Created: April 20, 2004
* Since: ICU 3.0
**********************************************************************
*/
#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/measfmt.h"
#include "unicode/numfmt.h"
#include "currfmt.h"
#include "unicode/localpointer.h"
#include "quantityformatter.h"
#include "unicode/plurrule.h"
#include "unicode/decimfmt.h"
#include "lrucache.h"
#include "uresimp.h"
#include "unicode/ures.h"
#include "cstring.h"
#include "mutex.h"
#include "ucln_in.h"
#include "unicode/listformatter.h"
#include "charstr.h"
#include "unicode/putil.h"
#include "unicode/smpdtfmt.h"

#include "sharedptr.h"

#define LENGTHOF(array) (int32_t)(sizeof(array)/sizeof((array)[0]))
#define MEAS_UNIT_COUNT 46

static icu::LRUCache *gCache = NULL;
static UMutex gCacheMutex = U_MUTEX_INITIALIZER;
static icu::UInitOnce gCacheInitOnce = U_INITONCE_INITIALIZER;

U_CDECL_BEGIN
static UBool U_CALLCONV measfmt_cleanup() {
    gCacheInitOnce.reset();
    if (gCache) {
        delete gCache;
        gCache = NULL;
    }
    return TRUE;
}
U_CDECL_END

U_NAMESPACE_BEGIN

class UnitFormatters : public UMemory {
public:
    UnitFormatters() { }
    QuantityFormatter formatters[MEAS_UNIT_COUNT][UMEASFMT_WIDTH_NARROW + 1];
private:
    UnitFormatters(const UnitFormatters &other);
    UnitFormatters &operator=(const UnitFormatters &other);
};

class NumericDateFormatters : public UMemory {
public:
    SimpleDateFormat hourMinute;
    SimpleDateFormat minuteSecond;
    SimpleDateFormat hourMinuteSecond;
    NumericDateFormatters(
            const UnicodeString &hm,
            const UnicodeString &ms,
            const UnicodeString &hms,
            UErrorCode &status) : 
            hourMinute(hm, status),
            minuteSecond(ms, status), 
            hourMinuteSecond(hms, status) {
        const TimeZone *gmt = TimeZone::getGMT();
        hourMinute.setTimeZone(*gmt);
        minuteSecond.setTimeZone(*gmt);
        hourMinuteSecond.setTimeZone(*gmt);
    }
private:
    NumericDateFormatters(const NumericDateFormatters &other);
    NumericDateFormatters &operator=(const NumericDateFormatters &other);
};

class MeasureFormatData : public SharedObject {
public:
    SharedPtr<UnitFormatters> unitFormatters;
    SharedPtr<PluralRules> pluralRules;
    SharedPtr<NumberFormat> numberFormat;
    SharedPtr<NumberFormat> currencyFormats[UMEASFMT_WIDTH_NARROW + 1];
    SharedPtr<NumericDateFormatters> numericDateFormatters;
    virtual ~MeasureFormatData();
private:
    MeasureFormatData &operator=(const MeasureFormatData& other);
};

MeasureFormatData::~MeasureFormatData() {
}

static int32_t widthToIndex(UMeasureFormatWidth width) {
    if (width > UMEASFMT_WIDTH_NARROW) {
        return UMEASFMT_WIDTH_NARROW;
    }
    return width;
}

static UBool isCurrency(const MeasureUnit &unit) {
    return (uprv_strcmp(unit.getType(), "currency") == 0);
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


static UBool load(
        const UResourceBundle *resource,
        UnitFormatters &unitFormatters,
        UErrorCode &status) {
    if (U_FAILURE(status)) {
        return FALSE;
    }
    static const char *widthPath[] = {"units", "unitsShort", "unitsNarrow"};
    MeasureUnit *units = NULL;
    int32_t unitCount = MeasureUnit::getAvailable(units, 0, status);
    while (status == U_BUFFER_OVERFLOW_ERROR) {
        status = U_ZERO_ERROR;
        delete [] units;
        units = new MeasureUnit[unitCount];
        if (units == NULL) {
            status = U_MEMORY_ALLOCATION_ERROR;
            return FALSE;
        }
        unitCount = MeasureUnit::getAvailable(units, unitCount, status);
    }
    for (int32_t currentWidth = 0; currentWidth <= UMEASFMT_WIDTH_NARROW; ++currentWidth) {
        // Be sure status is clear since next resource bundle lookup may fail.
        if (U_FAILURE(status)) {
            delete [] units;
            return FALSE;
        }
        LocalUResourceBundlePointer widthBundle(
                ures_getByKeyWithFallback(
                        resource, widthPath[currentWidth], NULL, &status));
        // We may not have data for all widths in all locales.
        if (status == U_MISSING_RESOURCE_ERROR) {
            status = U_ZERO_ERROR;
            continue;
        }
        for (int32_t currentUnit = 0; currentUnit < unitCount; ++currentUnit) {
            // Be sure status is clear next lookup may fail.
            if (U_FAILURE(status)) {
                delete [] units;
                return FALSE;
            }
            if (isCurrency(units[currentUnit])) {
                continue;
            }
            CharString pathBuffer;
            pathBuffer.append(units[currentUnit].getType(), status)
                    .append("/", status)
                    .append(units[currentUnit].getSubtype(), status);
            LocalUResourceBundlePointer unitBundle(
                    ures_getByKeyWithFallback(
                            widthBundle.getAlias(),
                            pathBuffer.data(),
                            NULL,
                            &status));
            // We may not have data for all units in all widths
            if (status == U_MISSING_RESOURCE_ERROR) {
                status = U_ZERO_ERROR;
                continue;
            }
            // We must have the unit bundle to proceed
            if (U_FAILURE(status)) {
                delete [] units;
                return FALSE;
            }
            int32_t size = ures_getSize(unitBundle.getAlias());
            for (int32_t plIndex = 0; plIndex < size; ++plIndex) {
                LocalUResourceBundlePointer pluralBundle(
                        ures_getByIndex(
                                unitBundle.getAlias(), plIndex, NULL, &status));
                if (U_FAILURE(status)) {
                    delete [] units;
                    return FALSE;
                }
                UnicodeString rawPattern;
                getString(pluralBundle.getAlias(), rawPattern, status);
                unitFormatters.formatters[units[currentUnit].getIndex()][currentWidth].add(
                        ures_getKey(pluralBundle.getAlias()),
                        rawPattern,
                        status);
            }
        }
    }
    delete [] units;
    return U_SUCCESS(status);
}

static UnicodeString loadNumericDateFormatterPattern(
        const UResourceBundle *resource,
        const char *pattern,
        UErrorCode &status) {
    UnicodeString result;
    if (U_FAILURE(status)) {
        return result;
    }
    CharString chs;
    chs.append("durationUnits", status)
            .append("/", status).append(pattern, status);
    LocalUResourceBundlePointer patternBundle(
            ures_getByKeyWithFallback(
                resource,
                chs.data(),
                NULL,
                &status));
    if (U_FAILURE(status)) {
        return result;
    }
    getString(patternBundle.getAlias(), result, status);
    // Replace 'h' with 'H'
    int32_t len = result.length();
    UChar *buffer = result.getBuffer(len);
    for (int32_t i = 0; i < len; ++i) {
        if (buffer[i] == 0x68) { // 'h'
            buffer[i] = 0x48; // 'H'
        }
    }
    result.releaseBuffer(len);
    return result;
}

static NumericDateFormatters *loadNumericDateFormatters(
        const UResourceBundle *resource,
        UErrorCode &status) {
    if (U_FAILURE(status)) {
        return NULL;
    }
    NumericDateFormatters *result = new NumericDateFormatters(
        loadNumericDateFormatterPattern(resource, "hm", status),
        loadNumericDateFormatterPattern(resource, "ms", status),
        loadNumericDateFormatterPattern(resource, "hms", status),
        status);
    if (U_FAILURE(status)) {
        delete result;
        return NULL;
    }
    return result;
}

static SharedObject *U_CALLCONV createData(
        const char *localeId, UErrorCode &status) {
    LocalUResourceBundlePointer topLevel(ures_open(NULL, localeId, &status));
    static UNumberFormatStyle currencyStyles[] = {
            UNUM_CURRENCY_PLURAL, UNUM_CURRENCY_ISO, UNUM_CURRENCY};
    if (U_FAILURE(status)) {
        return NULL;
    }
    LocalPointer<MeasureFormatData> result(new MeasureFormatData());
    LocalPointer<UnitFormatters> unitFormatters(new UnitFormatters());
    if (result.getAlias() == NULL
            || unitFormatters.getAlias() == NULL) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }
    if (!load(
            topLevel.getAlias(),
            *unitFormatters,
            status)) {
        return NULL;
    }
    if (!result->unitFormatters.reset(unitFormatters.orphan())) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }

    LocalPointer<NumericDateFormatters> ndf(
            loadNumericDateFormatters(topLevel.getAlias(), status));
    if (U_FAILURE(status)) {
        return NULL;
    }
    if (!result->numericDateFormatters.reset(ndf.orphan())) {
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

    for (int32_t i = 0; i <= UMEASFMT_WIDTH_NARROW; ++i) {
        LocalPointer<NumberFormat> cf(
                NumberFormat::createInstance(
                        localeId, currencyStyles[i], status));
        if (U_FAILURE(status)) {
            return NULL;
        }
        if (!result->currencyFormats[i].reset(cf.orphan())) {
            status = U_MEMORY_ALLOCATION_ERROR;
            return NULL;
        }
    }

    return result.orphan();
}

static void U_CALLCONV cacheInit(UErrorCode &status) {
    U_ASSERT(gCache == NULL);
    U_ASSERT(MeasureUnit::getIndexCount() == MEAS_UNIT_COUNT);
    ucln_i18n_registerCleanup(UCLN_I18N_MEASFMT, measfmt_cleanup);
    gCache = new SimpleLRUCache(100, &createData, status);
    if (U_FAILURE(status)) {
        delete gCache;
        gCache = NULL;
    }
}

static void getFromCache(
        const char *locale,
        const MeasureFormatData *&ptr,
        UErrorCode &status) {
    umtx_initOnce(gCacheInitOnce, &cacheInit, status);
    if (U_FAILURE(status)) {
        return;
    }
    Mutex lock(&gCacheMutex);
    gCache->get(locale, ptr, status);
}

static int32_t toHMS(
        const Measure *measures,
        int32_t measureCount,
        Formattable *hms,
        UErrorCode &status) {
    if (U_FAILURE(status)) {
        return 0;
    }
    int32_t result = 0;
    LocalPointer<MeasureUnit> hourUnit(MeasureUnit::createHour(status));
    LocalPointer<MeasureUnit> minuteUnit(MeasureUnit::createMinute(status));
    LocalPointer<MeasureUnit> secondUnit(MeasureUnit::createSecond(status));
    if (U_FAILURE(status)) {
        return 0;
    }
    int32_t count = 0;
    for (int32_t i = 0; i < measureCount; ++i) {
        if (measures[i].getUnit() == *hourUnit) {
            if ((result & 1) == 0) {
                ++count;
            } else {
                return 0;
            }
            hms[0] = measures[i].getNumber();
            result |= 1;
        } else if (measures[i].getUnit() == *minuteUnit) {
            if ((result & 2) == 0) {
                ++count;
            } else {
                return 0;
            }
            hms[1] = measures[i].getNumber();
            result |= 2;
        } else if (measures[i].getUnit() == *secondUnit) {
            if ((result & 4) == 0) {
                ++count;
            } else {
                return 0;
            }
            hms[2] = measures[i].getNumber();
            result |= 4;
        } else {
            return 0;
        }
    }
    return result;
}


MeasureFormat::MeasureFormat(
        const Locale &locale, UMeasureFormatWidth w, UErrorCode &status)
        : ptr(NULL), width(w) {
    initMeasureFormat(locale, w, status);
}

MeasureFormat::MeasureFormat(
        const Locale &locale,
        UMeasureFormatWidth w,
        NumberFormat *nfToAdopt,
        UErrorCode &status) 
        : ptr(NULL), width(w) {
    initMeasureFormat(locale, w, status);
    adoptNumberFormat(nfToAdopt, status);
}

MeasureFormat::MeasureFormat(const MeasureFormat &other)
        : Format(other), ptr(other.ptr), width(other.width) {
    ptr->addRef();
}

MeasureFormat &MeasureFormat::operator=(const MeasureFormat &other) {
    if (this == &other) {
        return *this;
    }
    Format::operator=(other);
    SharedObject::copyPtr(other.ptr, ptr);
    width = other.width;
    return *this;
}

MeasureFormat::MeasureFormat() : ptr(NULL), width(UMEASFMT_WIDTH_WIDE) {
}

MeasureFormat::~MeasureFormat() {
    if (ptr != NULL) {
        ptr->removeRef();
    }
}

UBool MeasureFormat::operator==(const Format &other) const {
    const MeasureFormat *rhs = dynamic_cast<const MeasureFormat *>(&other);
    if (rhs == NULL) {
        return FALSE;
    }
    // Same objects are equivalent
    if (this == rhs) {
        return TRUE;
    }
    // differing widths aren't equivalent
    if (width != rhs->width) {
        return FALSE;
    }
    // Width the same, same shared data -> equivlanet
    if (ptr == rhs->ptr) {
        return TRUE;
    }
    // Width same, but differing shred data. Depends on locale
    // and number format objects being the same.
    UErrorCode status = U_ZERO_ERROR;
    const char *localeId = getLocaleID(status);
    const char *rhsLocaleId = rhs->getLocaleID(status);
    if (U_FAILURE(status)) {
        // On failure, assume not equal
        return FALSE;
    }
    return (uprv_strcmp(localeId, rhsLocaleId) == 0
            && *ptr->numberFormat == *rhs->ptr->numberFormat);
}

Format *MeasureFormat::clone() const {
    return new MeasureFormat(*this);
}

UnicodeString &MeasureFormat::format(
        const Formattable &obj,
        UnicodeString &appendTo,
        FieldPosition &pos,
        UErrorCode &status) const {
    if (U_FAILURE(status)) return appendTo;
    if (obj.getType() == Formattable::kObject) {
        const UObject* formatObj = obj.getObject();
        const Measure* amount = dynamic_cast<const Measure*>(formatObj);
        if (amount != NULL) {
            return formatMeasure(*amount, appendTo, pos, status);
        }
    }
    status = U_ILLEGAL_ARGUMENT_ERROR;
    return appendTo;
}

void MeasureFormat::parseObject(
        const UnicodeString & /*source*/,
        Formattable & /*result*/,
        ParsePosition& /*pos*/) const {
    return;
}

UnicodeString &MeasureFormat::formatMeasures(
        const Measure *measures,
        int32_t measureCount,
        UnicodeString &appendTo,
        FieldPosition &pos,
        UErrorCode &status) const {
    static const char *listStyles[] = {"unit", "unit-short", "unit-narrow"};
    if (U_FAILURE(status)) {
        return appendTo;
    }
    if (measureCount == 0) {
        return appendTo;
    }
    if (measureCount == 1) {
        return formatMeasure(measures[0], appendTo, pos, status);
    }
    if (width == UMEASFMT_WIDTH_NUMERIC) {
        Formattable hms[3];
        int32_t bitMap = toHMS(measures, measureCount, hms, status);
        if (bitMap > 0) {
            return formatNumeric(hms, bitMap, appendTo, status);
        }
    }
    LocalPointer<ListFormatter> lf(
            ListFormatter::createInstance(
                    getLocale(status),
                    listStyles[widthToIndex(width)],
                    status));
    if (U_FAILURE(status)) {
        return appendTo;
    }
    if (pos.getField() != FieldPosition::DONT_CARE) {
        return formatMeasuresSlowTrack(
                measures, measureCount, *lf, appendTo, pos, status);
    }
    UnicodeString *results = new UnicodeString[measureCount];
    if (results == NULL) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return appendTo;
    }
    for (int32_t i = 0; i < measureCount; ++i) {
        formatMeasure(measures[i], results[i], pos, status);
    }
    lf->format(results, measureCount, appendTo, status);
    delete [] results; 
    return appendTo;
}

void MeasureFormat::initMeasureFormat(
        const Locale &locale, UMeasureFormatWidth w, UErrorCode &status) {
    if (U_FAILURE(status)) {
        return;
    }
    const char *name = locale.getName();
    setLocaleIDs(name, name);
    width = w;
    getFromCache(name, ptr, status);
}

void MeasureFormat::adoptNumberFormat(NumberFormat *nfToAdopt, UErrorCode &status) {
    if (U_FAILURE(status)) {
        return;
    }
    MeasureFormatData* wptr = SharedObject::copyOnWrite(ptr);
    if (wptr == NULL) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
    if (!wptr->numberFormat.reset(nfToAdopt)) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
}

UBool MeasureFormat::setMeasureFormatLocale(const Locale &locale, UErrorCode &status) {
    if (U_FAILURE(status) || locale == getLocale(status)) {
        return FALSE;
    }
    initMeasureFormat(locale, width, status);
    return U_SUCCESS(status);
} 

const NumberFormat &MeasureFormat::getNumberFormat() const {
    return *ptr->numberFormat;
}

const PluralRules &MeasureFormat::getPluralRules() const {
    return *ptr->pluralRules;
}

Locale MeasureFormat::getLocale(UErrorCode &status) const {
    return Format::getLocale(ULOC_VALID_LOCALE, status);
}

const char *MeasureFormat::getLocaleID(UErrorCode &status) const {
    return Format::getLocaleID(ULOC_VALID_LOCALE, status);
}

UnicodeString &MeasureFormat::formatMeasure(
        const Measure &measure,
        UnicodeString &appendTo,
        FieldPosition &pos,
        UErrorCode &status) const {
    if (U_FAILURE(status)) {
        return appendTo;
    }
    const Formattable& amtNumber = measure.getNumber();
    const MeasureUnit& amtUnit = measure.getUnit();
    if (isCurrency(amtUnit)) {
        UChar isoCode[4];
        u_charsToUChars(amtUnit.getSubtype(), isoCode, 4);
        return ptr->currencyFormats[widthToIndex(width)]->format(
                new CurrencyAmount(amtNumber, isoCode, status),
                appendTo,
                pos,
                status);
    }
    const QuantityFormatter *quantityFormatter = getQuantityFormatter(
            amtUnit.getIndex(), widthToIndex(width), status);
    if (U_FAILURE(status)) {
        return appendTo;
    }
    return quantityFormatter->format(
            amtNumber,
            *ptr->numberFormat,
            *ptr->pluralRules, appendTo,
            pos,
            status);
}

UnicodeString &MeasureFormat::formatNumeric(
        const Formattable *hms,  // always length 3
        int32_t bitMap,   // 1=hourset, 2=minuteset, 4=secondset
        UnicodeString &appendTo,
        UErrorCode &status) const {
    if (U_FAILURE(status)) {
        return appendTo;
    }
    UDate millis = 
        (UDate) (((hms[0].getDouble(status) * 60.0
             + hms[1].getDouble(status)) * 60.0
                  + hms[2].getDouble(status)) * 1000.0);
    switch (bitMap) {
    case 5: // hs
    case 7: // hms
        return formatNumeric(
                millis,
                ptr->numericDateFormatters->hourMinuteSecond,
                UDAT_SECOND_FIELD,
                hms[2],
                appendTo,
                status);
        break;
    case 6: // ms
        return formatNumeric(
                millis,
                ptr->numericDateFormatters->minuteSecond,
                UDAT_SECOND_FIELD,
                hms[2],
                appendTo,
                status);
        break;
    case 3: // hm
        return formatNumeric(
                millis,
                ptr->numericDateFormatters->hourMinute,
                UDAT_MINUTE_FIELD,
                hms[1],
                appendTo,
                status);
        break;
    default:
        status = U_INTERNAL_PROGRAM_ERROR;
        return appendTo;
        break;
    }
    return appendTo;
}

UnicodeString &MeasureFormat::formatNumeric(
        UDate date,
        const DateFormat &dateFmt,
        UDateFormatField smallestField,
        const Formattable &smallestAmount,
        UnicodeString &appendTo,
        UErrorCode &status) const {
    if (U_FAILURE(status)) {
        return appendTo;
    }
    UnicodeString smallestAmountFormatted;
    ptr->numberFormat->format(
            smallestAmount, smallestAmountFormatted, status);
    FieldPosition smallestFieldPosition(smallestField);
    UnicodeString draft;
    dateFmt.format(date, draft, smallestFieldPosition, status);
    if (smallestFieldPosition.getBeginIndex() != 0 ||
        smallestFieldPosition.getEndIndex() != 0) {
        appendTo.append(draft, 0, smallestFieldPosition.getBeginIndex());
        appendTo.append(smallestAmountFormatted);
        appendTo.append(
                draft,
                smallestFieldPosition.getEndIndex(),
                draft.length());
    } else {
        appendTo.append(draft);
    }
    return appendTo;
}

const QuantityFormatter *MeasureFormat::getQuantityFormatter(
        int32_t index,
        int32_t widthIndex,
        UErrorCode &status) const {
    if (U_FAILURE(status)) {
        return NULL;
    }
    const QuantityFormatter *formatters =
            ptr->unitFormatters->formatters[index];
    if (formatters[widthIndex].isValid()) {
        return &formatters[widthIndex];
    }
    if (formatters[UMEASFMT_WIDTH_SHORT].isValid()) {
        return &formatters[UMEASFMT_WIDTH_SHORT];
    }
    if (formatters[UMEASFMT_WIDTH_WIDE].isValid()) {
        return &formatters[UMEASFMT_WIDTH_WIDE];
    }
    status = U_MISSING_RESOURCE_ERROR;
    return NULL;
}

UnicodeString &MeasureFormat::formatMeasuresSlowTrack(
        const Measure *measures,
        int32_t measureCount,
        const ListFormatter& lf,
        UnicodeString& appendTo,
        FieldPosition& pos,
        UErrorCode& status) const {
    if (U_FAILURE(status)) {
        return appendTo;
    }
    FieldPosition dontCare(FieldPosition::DONT_CARE);
    FieldPosition fpos(pos.getField());
    UnicodeString *results = new UnicodeString[measureCount];
    int32_t fieldPositionFoundIndex = -1;
    for (int32_t i = 0; i < measureCount; ++i) {
        if (fieldPositionFoundIndex == -1) {
            formatMeasure(measures[i], results[i], fpos, status);
            if (U_FAILURE(status)) {
                delete [] results;
                return appendTo;
            }
            if (fpos.getBeginIndex() != 0 || fpos.getEndIndex() != 0) {
                fieldPositionFoundIndex = i;
            }
        } else {
            formatMeasure(measures[i], results[i], dontCare, status);
        }
    }
    int32_t offset;
    lf.format(
            results,
            measureCount,
            appendTo,
            fieldPositionFoundIndex,
            offset,
            status);
    if (U_FAILURE(status)) {
        delete [] results;
        return appendTo;
    }
    if (offset != -1) {
        pos.setBeginIndex(fpos.getBeginIndex() + offset);
        pos.setEndIndex(fpos.getEndIndex() + offset);
    }
    delete [] results;
    return appendTo;
}

MeasureFormat* U_EXPORT2 MeasureFormat::createCurrencyFormat(const Locale& locale,
                                                   UErrorCode& ec) {
    CurrencyFormat* fmt = NULL;
    if (U_SUCCESS(ec)) {
        fmt = new CurrencyFormat(locale, ec);
        if (U_FAILURE(ec)) {
            delete fmt;
            fmt = NULL;
        }
    }
    return fmt;
}

MeasureFormat* U_EXPORT2 MeasureFormat::createCurrencyFormat(UErrorCode& ec) {
    if (U_FAILURE(ec)) {
        return NULL;
    }
    return MeasureFormat::createCurrencyFormat(Locale::getDefault(), ec);
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */
