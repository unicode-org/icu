/*
*******************************************************************************
* Copyright (C) 2004, International Business Machines Corporation and
* others. All Rights Reserved.
*******************************************************************************
*/

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/utmscale.h"

#define ticks        INT64_C(1)
#define microseconds (ticks * 10)
#define milliseconds (microseconds * 1000)
#define seconds      (milliseconds * 1000)
#define minutes      (seconds * 60)
#define hours        (minutes * 60)
#define days         (hours * 24)

#define TIME_SCALE_CHECK(scale,status) \
    if(scale < 0 || scale >= UDTS_MAX_SCALE) { \
        *status = U_ILLEGAL_ARGUMENT_ERROR; \
        return 0; \
    }

typedef struct
{
    int64_t units;
    int64_t epochOffset;

    int64_t fromMin;
    int64_t fromMax;
    int64_t toMin;
    int64_t toMax;

    int64_t epochOffsetP1;
    int64_t epochOffsetM1;
    int64_t unitsRound;
    int64_t minRound;
    int64_t maxRound;
} InternalTimeScaleData;

static const InternalTimeScaleData timeScaleTable[] = {
  /*    units             epochOffset                     fromMin                        fromMax                            toMin                        toMax                    epochOffsetP1                epochOffsetM1              unitsRound                    minRound                     maxRound          */
    {milliseconds, INT64_C(62135769600000),     INT64_C(-984472973285477),     INT64_C(860201434085477),     INT64_C(-9223372036854774999), INT64_C(9223372036854774999), INT64_C(62135769600001),     INT64_C(62135769599999),     INT64_C(5000),         INT64_C(-9223372036854770808), INT64_C(9223372036854770807)},
    {seconds,      INT64_C(62135769600),        INT64_C(-984472973285),        INT64_C(860201434085),        U_INT64_MIN,                   INT64_C(9223372036854775807), INT64_C(62135769601),        INT64_C(62135769599),        INT64_C(5000000),      INT64_C(-9223372036849775808), INT64_C(9223372036849775807)},
    {milliseconds, INT64_C(62135769600000),     INT64_C(-984472973285477),     INT64_C(860201434085477),     INT64_C(-9223372036854774999), INT64_C(9223372036854774999), INT64_C(62135769600001),     INT64_C(62135769599999),     INT64_C(5000),         INT64_C(-9223372036854770808), INT64_C(9223372036854770807)},
    {ticks,        INT64_C(504912960000000000), U_INT64_MIN,                   INT64_C(8718459076854775807), INT64_C(-8718459076854775808), INT64_C(9223372036854775807), INT64_C(504912960000000000), INT64_C(504912960000000000), INT64_C(0),            U_INT64_MIN,                   INT64_C(9223372036854775807)},
    {ticks,        INT64_C(0),                  U_INT64_MIN,                   INT64_C(9223372036854775807), U_INT64_MIN,                   INT64_C(9223372036854775807), INT64_C(0),                  INT64_C(0),                  INT64_C(0),            U_INT64_MIN,                   INT64_C(9223372036854775807)},
    {seconds,      INT64_C(60052924800),        INT64_C(-982390128485),        INT64_C(862284278885),        U_INT64_MIN,                   INT64_C(9223372036854775807), INT64_C(60052924801),        INT64_C(60052924799),        INT64_C(5000000),      INT64_C(-9223372036849775808), INT64_C(9223372036849775807)},
    {seconds,      INT64_C(63114076800),        INT64_C(-985451280485),        INT64_C(859223126885),        U_INT64_MIN,                   INT64_C(9223372036854775807), INT64_C(63114076801),        INT64_C(63114076799),        INT64_C(5000000),      INT64_C(-9223372036849775808), INT64_C(9223372036849775807)},
    {days,         INT64_C(693596),             INT64_C(-11368795),            INT64_C(9981603),             U_INT64_MIN,                   INT64_C(9223372036854775807), INT64_C(693597),             INT64_C(693595),             INT64_C(432000000000), INT64_C(-9223371604854775808), INT64_C(9223371604854775807)},
    {days,         INT64_C(693596),             INT64_C(-11368795),            INT64_C(9981603),             U_INT64_MIN,                   INT64_C(9223372036854775807), INT64_C(693597),             INT64_C(693595),             INT64_C(432000000000), INT64_C(-9223371604854775808), INT64_C(9223371604854775807)},
};

U_CAPI void U_EXPORT2
utmscale_getTimeScaleData(UDateTimeScale timeScale, UTimeScaleData *data, UErrorCode *status)
{
    const InternalTimeScaleData *internalData;

    if (status == NULL || U_FAILURE(*status)) {
        return;
    }

    if (timeScale < 0 || timeScale >= UDTS_MAX_SCALE || data == NULL) {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    internalData      = &timeScaleTable[timeScale];

    data->units       = internalData->units;
    data->epochOffset = internalData->epochOffset;
    data->fromMin     = internalData->fromMin;
    data->fromMax     = internalData->fromMax;
    data->toMin       = internalData->toMin;
    data->toMax       = internalData->toMax;
}

U_CAPI int64_t U_EXPORT2
utmscale_fromDouble(double otherTime, UDateTimeScale timeScale, UErrorCode *status)
{
    /*
     * NOTE: fromMin and fromMax are marked "volatile" because the
     * with the gcc compiler, the code which compares otherTime to
     * fromMin and fromMax seems to fail if data->fromMax is U_INT64_MAX.
     */
    const InternalTimeScaleData *data;
    volatile double fromMin, fromMax;

    if (status == NULL || U_FAILURE(*status)) {
        return 0;
    }

    if (timeScale < 0 || timeScale >= UDTS_MAX_SCALE) {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    data = &timeScaleTable[timeScale];
    fromMin = (double) data->fromMin;
    fromMax = (double) data->fromMax;

    if (!(otherTime >= fromMin && otherTime <= fromMax)) {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }
    
    return ((int64_t)otherTime + data->epochOffset) * data->units;
}

U_CAPI int64_t U_EXPORT2
utmscale_fromInt64(int64_t otherTime, UDateTimeScale timeScale, UErrorCode *status)
{
    const InternalTimeScaleData *data;
    
    if (status == NULL || U_FAILURE(*status)) {
        return 0;
    }

    if (timeScale < 0 || timeScale >= UDTS_MAX_SCALE) {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    data = &timeScaleTable[timeScale];

    if (otherTime < data->fromMin || otherTime > data->fromMax) {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }
    
    return (otherTime + data->epochOffset) * data->units;
}

U_CAPI double U_EXPORT2
utmscale_toDouble(int64_t universalTime, UDateTimeScale timeScale, UErrorCode *status)
{
    const InternalTimeScaleData *data;
    
    if (status == NULL || U_FAILURE(*status)) {
        return 0;
    }

    if (timeScale < 0 || timeScale >= UDTS_MAX_SCALE) {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    data = &timeScaleTable[timeScale];

    if (universalTime < data->toMin || universalTime > data->toMax) {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }
    
    if (universalTime < 0) {
        if (universalTime < data->minRound) {
            return (double) (universalTime + data->unitsRound) / data->units - data->epochOffsetP1;
        }
        
        return (double) (universalTime - data->unitsRound) / data->units - data->epochOffset;
    }
    
    if (universalTime > data->maxRound) {
        return (double) (universalTime - data->unitsRound) / data->units - data->epochOffsetM1;
    }
    
    return (double) (universalTime + data->unitsRound) / data->units - data->epochOffset;
}
    
U_CAPI int64_t U_EXPORT2
utmscale_toInt64(int64_t universalTime, UDateTimeScale timeScale, UErrorCode *status)
{
    const InternalTimeScaleData *data;
    
    if (status == NULL || U_FAILURE(*status)) {
        return 0;
    }

    if (timeScale < 0 || timeScale >= UDTS_MAX_SCALE) {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    data = &timeScaleTable[timeScale];

    if (universalTime < data->toMin || universalTime > data->toMax) {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }
    
    if (universalTime < 0) {
        if (universalTime < data->minRound) {
            return (universalTime + data->unitsRound) / data->units - data->epochOffsetP1;
        }
        
        return (universalTime - data->unitsRound) / data->units - data->epochOffset;
    }
    
    if (universalTime > data->maxRound) {
        return (universalTime - data->unitsRound) / data->units - data->epochOffsetM1;
    }
    
    return (universalTime + data->unitsRound) / data->units - data->epochOffset;
}

#endif /* #if !UCONFIG_NO_FORMATTING */
