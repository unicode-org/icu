/*
*******************************************************************************
* Copyright (C) 2004-2006, International Business Machines Corporation and
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

static const int64_t timeScaleTable[UDTS_MAX_SCALE][UTSV_MAX_SCALE_VALUE] = {
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

U_CAPI int64_t U_EXPORT2
utmscale_getTimeScaleValue(UDateTimeScale timeScale, UTimeScaleValue value, UErrorCode *status)
{
    if (status == NULL || U_FAILURE(*status)) {
        return 0;
    }

    if (timeScale < UDTS_JAVA_TIME || UDTS_MAX_SCALE <= timeScale
        || value < UTSV_UNITS_VALUE || UTSV_MAX_SCALE_VALUE <= value)
    {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    return timeScaleTable[timeScale][value];
}

U_CAPI int64_t U_EXPORT2
utmscale_fromInt64(int64_t otherTime, UDateTimeScale timeScale, UErrorCode *status)
{
    const int64_t *data;
    
    if (status == NULL || U_FAILURE(*status)) {
        return 0;
    }

    if (timeScale < 0 || timeScale >= UDTS_MAX_SCALE) {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    data = (const int64_t *)(&timeScaleTable[timeScale]);

    if (otherTime < data[UTSV_FROM_MIN_VALUE] || otherTime > data[UTSV_FROM_MAX_VALUE]) {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }
    
    return (otherTime + data[UTSV_EPOCH_OFFSET_VALUE]) * data[UTSV_UNITS_VALUE];
}

U_CAPI int64_t U_EXPORT2
utmscale_toInt64(int64_t universalTime, UDateTimeScale timeScale, UErrorCode *status)
{
    const int64_t *data;
    
    if (status == NULL || U_FAILURE(*status)) {
        return 0;
    }

    if (timeScale < 0 || timeScale >= UDTS_MAX_SCALE) {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    data = (const int64_t *)(&timeScaleTable[timeScale]);

    if (universalTime < data[UTSV_TO_MIN_VALUE] || universalTime > data[UTSV_TO_MAX_VALUE]) {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }
    
    if (universalTime < 0) {
        if (universalTime < data[UTSV_MIN_ROUND_VALUE]) {
            return (universalTime + data[UTSV_UNITS_ROUND_VALUE]) / data[UTSV_UNITS_VALUE] - data[UTSV_EPOCH_OFFSET_PLUS_1_VALUE];
        }
        
        return (universalTime - data[UTSV_UNITS_ROUND_VALUE]) / data[UTSV_UNITS_VALUE] - data[UTSV_EPOCH_OFFSET_VALUE];
    }
    
    if (universalTime > data[UTSV_MAX_ROUND_VALUE]) {
        return (universalTime - data[UTSV_UNITS_ROUND_VALUE]) / data[UTSV_UNITS_VALUE] - data[UTSV_EPOCH_OFFSET_MINUS_1_VALUE];
    }
    
    return (universalTime + data[UTSV_UNITS_ROUND_VALUE]) / data[UTSV_UNITS_VALUE] - data[UTSV_EPOCH_OFFSET_VALUE];
}

#endif /* #if !UCONFIG_NO_FORMATTING */
