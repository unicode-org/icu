/*
 ****************************************************************************
 * Copyright (c) 1997-2004, International Business Machines Corporation and *
 * others. All Rights Reserved.                                             *
 ****************************************************************************
 */

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/utmscale.h"

#include "cintltst.h"

#include <stdlib.h>
#include <time.h>

#define LOOP_COUNT 10000

static void TestAPI(void);
static void TestData(void);
static void TestMonkey(void);

void addUtmsTest(TestNode** root);

void addUtmsTest(TestNode** root)
{
    addTest(root, &TestAPI, "tsformat/utmstest/TestAPI");
    addTest(root, &TestData, "tsformat/utmstest/TestData");
    addTest(root, &TestMonkey, "tsformat/utmstest/TestMonkey");
}

/**
 * Return a random int64_t where U_INT64_MIN <= ran <= U_INT64_MAX.
 */
static uint64_t randomInt64(void)
{
    int64_t ran = 0;
    int32_t i;
    static UBool initialized = FALSE;

    if (!initialized) {
        srand((unsigned)time(NULL));
        initialized = TRUE;
    }

    /* Assume rand has at least 12 bits of precision */
    for (i = 0; i < sizeof(ran); i += 1) {
        ((char*)&ran)[i] = (char)((rand() & 0x0FF0) >> 4);
    }

    return ran;
}

static int64_t ranInt;
static int64_t ranMin;
static int64_t ranMax;

static void initRandom(int64_t min, int64_t max)
{
    uint64_t interval = max - min;
    
    ranMin = min;
    ranMax = max;
    ranInt = 0;
    
    if (interval < U_INT64_MIN) {
        ranInt = interval;
    }
}

static int64_t randomInRange(void)
{
    int64_t value;
    
    if (ranInt != 0) {
        value = randomInt64() % ranInt;
        
        if (value < 0) {
            value = -value;
        }
        
        value += ranMin;
    } else {
        do {
            value = randomInt64();
        } while (value < ranMin || value > ranMax);
    }
    
    return value;
}
    
static void roundTripTest(int64_t value, int32_t scale)
{
    UErrorCode status = U_ZERO_ERROR;
    int64_t rt = utmscale_toInt64(utmscale_fromInt64(value, scale, &status), scale, &status);
    
    if (rt != value) {
        log_err("Round-trip error: time scale = %d, value = %lld, round-trip = %lld.\n", scale, value, rt);
    }
}

static void toLimitTest(int64_t toLimit, int64_t fromLimit, int32_t scale)
{
    UErrorCode status = U_ZERO_ERROR;
    int64_t result = utmscale_toInt64(toLimit, scale, &status);
    
    if (result != fromLimit) {
        log_err("toLimit failure: scale = %d, toLimit = %lld , utmscale_toInt64(toLimit, scale, &status) = %lld, fromLimit = %lld.\n",
            scale, toLimit, result, fromLimit);
    }
}

static void epochOffsetTest(int64_t epochOffset, int64_t units, int32_t scale)
{
    UErrorCode status = U_ZERO_ERROR;
    int64_t universal = 0;
    int64_t universalEpoch = epochOffset * units;
    int64_t local = utmscale_toInt64(universalEpoch, scale, &status);
    
    if (local != 0) {
        log_err("utmscale_toInt64(epochOffset, scale, &status): scale = %d epochOffset = %lld, result = %lld.\n", scale, epochOffset, local);
    }
    
    local = utmscale_toInt64(0, scale, &status);
    
    if (local != -epochOffset) {
        log_err("utmscale_toInt64(0, scale): scale = %d, result = %lld.\n", scale, local);
    }
    
    universal = utmscale_fromInt64(-epochOffset, scale, &status);
    
    if (universal != 0) {
        log_err("from(-epochOffest, scale): scale = %d, epochOffset = %lld, result = %lld.\n", scale, epochOffset, universal);
    }
    
    universal = utmscale_fromInt64(0, scale, &status);
    
    if (universal != universalEpoch) {
        log_err("utmscale_fromInt64(0, scale): scale = %d, result = %lld.\n", scale, universal);
    }
}

static void TestEpochOffsets(void)
{
    UErrorCode status = U_ZERO_ERROR;
    int32_t scale;

    for (scale = 0; scale < UDTS_MAX_SCALE; scale += 1) {
        int64_t units       = utmscale_getTimeScaleValue(scale, UTSV_UNITS_VALUE, &status);
        int64_t epochOffset = utmscale_getTimeScaleValue(scale, UTSV_EPOCH_OFFSET_VALUE, &status);
        
        epochOffsetTest(epochOffset, units, scale);
    }
}

static void TestFromLimits(void)
{
    UErrorCode status = U_ZERO_ERROR;
    int32_t scale;

    for (scale = 0; scale < UDTS_MAX_SCALE; scale += 1) {
        int64_t fromMin = utmscale_getTimeScaleValue(scale, UTSV_FROM_MIN_VALUE, &status);
        int64_t fromMax = utmscale_getTimeScaleValue(scale, UTSV_FROM_MAX_VALUE, &status);
        
        roundTripTest(fromMin, scale);
        roundTripTest(fromMax, scale);
    }
}

static void TestToLimits(void)
{
    UErrorCode status = U_ZERO_ERROR;
    int32_t scale;

    for (scale = 0; scale < UDTS_MAX_SCALE; scale += 1) {
        int64_t fromMin = utmscale_getTimeScaleValue(scale, UTSV_FROM_MIN_VALUE, &status);
        int64_t fromMax = utmscale_getTimeScaleValue(scale, UTSV_FROM_MAX_VALUE, &status);
        int64_t toMin   = utmscale_getTimeScaleValue(scale, UTSV_TO_MIN_VALUE, &status);
        int64_t toMax   = utmscale_getTimeScaleValue(scale, UTSV_TO_MAX_VALUE, &status);
        
        toLimitTest(toMin, fromMin, scale);
        toLimitTest(toMax, fromMax, scale);
    }
}

static void TestFromInt64(void)
{
    int32_t scale;
    int64_t result;
    UErrorCode status = U_ZERO_ERROR;
    
    result = utmscale_fromInt64(0, -1, &status);
    if (status != U_ILLEGAL_ARGUMENT_ERROR) {
        log_err("utmscale_fromInt64(0, -1, status) did not set status to U_ILLEGAL_ARGUMENT_ERROR.\n");
    }
    
    for (scale = 0; scale < UDTS_MAX_SCALE; scale += 1) {
        int64_t fromMin, fromMax;
        
        status = U_ZERO_ERROR;
        fromMin = utmscale_getTimeScaleValue(scale, UTSV_FROM_MIN_VALUE, &status);
        fromMax = utmscale_getTimeScaleValue(scale, UTSV_FROM_MAX_VALUE, &status);
        
        status = U_ZERO_ERROR;
        result = utmscale_fromInt64(0, scale, &status);
        if (status == U_ILLEGAL_ARGUMENT_ERROR) {
            log_err("utmscale_fromInt64(0, %d, &status) generated U_ILLEGAL_ARGUMENT_ERROR.\n", scale);
        }
        
        status = U_ZERO_ERROR;
        result = utmscale_fromInt64(fromMin, scale, &status);
        if (status == U_ILLEGAL_ARGUMENT_ERROR) {
            log_err("utmscale_fromInt64(fromMin, %d, &status) generated U_ILLEGAL_ARGUMENT_ERROR.\n", scale);
        }
            
       if (fromMin > U_INT64_MIN) {
            status = U_ZERO_ERROR;
            result = utmscale_fromInt64(fromMin - 1, scale, &status);
            if (status != U_ILLEGAL_ARGUMENT_ERROR) {
                log_err("utmscale_fromInt64(fromMin - 1, %d, &status) did not generate U_ILLEGAL_ARGUMENT_ERROR.\n", scale);
            }
        }
            
        status = U_ZERO_ERROR;
        result = utmscale_fromInt64(fromMax, scale, &status);
        if (status == U_ILLEGAL_ARGUMENT_ERROR) {
            log_err("utmscale_fromInt64(fromMax, %d, &status) generated U_ILLEGAL_ARGUMENT_ERROR.\n", scale);
        }
            
        if (fromMax < U_INT64_MAX) {
            status = U_ZERO_ERROR;
            result = utmscale_fromInt64(fromMax + 1, scale, &status);
            if (status != U_ILLEGAL_ARGUMENT_ERROR) {
                log_err("utmscale_fromInt64(fromMax + 1, %d, &status) didn't generate U_ILLEGAL_ARGUMENT_ERROR.\n", scale);
            }
        }
    }
    
    status = U_ZERO_ERROR;
    result = utmscale_fromInt64(0, UDTS_MAX_SCALE, &status);
    if (status != U_ILLEGAL_ARGUMENT_ERROR) {
        log_err("utmscale_fromInt64(0, UDTS_MAX_SCALE, &status) did not generate U_ILLEGAL_ARGUMENT_ERROR.\n");
    }
}

static void TestToInt64(void)
{
    int32_t scale;
    int64_t result;
    UErrorCode status = U_ZERO_ERROR;
    
    result = utmscale_toInt64(0, -1, &status);
    if (status != U_ILLEGAL_ARGUMENT_ERROR) {
        log_err("utmscale_toInt64(0, -1, &status) did not generate U_ILLEGAL_ARGUMENT_ERROR.\n");
    }
    
    for (scale = 0; scale < UDTS_MAX_SCALE; scale += 1) {
        int64_t toMin, toMax;
        
        status = U_ZERO_ERROR;
        toMin = utmscale_getTimeScaleValue(scale, UTSV_TO_MIN_VALUE, &status);
        toMax = utmscale_getTimeScaleValue(scale, UTSV_TO_MAX_VALUE, &status);

        status = U_ZERO_ERROR;
        result = utmscale_toInt64(0, scale, &status);
        if (status == U_ILLEGAL_ARGUMENT_ERROR) {
            log_err("utmscale_toInt64(0, %d, &status) generated U_ILLEGAL_ARGUMENT_ERROR.\n", scale);
        }
        
        status = U_ZERO_ERROR;
        result = utmscale_toInt64(toMin, scale, &status);
        if (status == U_ILLEGAL_ARGUMENT_ERROR) {
            log_err("utmscale_toInt64(toMin, %d, &status) generated U_ILLEGAL_ARGUMENT_ERROR.\n", scale);
        }
            
        if (toMin > U_INT64_MIN) {
            status = U_ZERO_ERROR;
            result = utmscale_toInt64(toMin - 1, scale, &status);
            if (status != U_ILLEGAL_ARGUMENT_ERROR) {
                log_err("utmscale_toInt64(toMin - 1, %d, &status) did not generate U_ILLEGAL_ARGUMENT_ERROR.\n", scale);
            }
        }

            
        status = U_ZERO_ERROR;
        result = utmscale_toInt64(toMax, scale, &status);
        if (status == U_ILLEGAL_ARGUMENT_ERROR) {
            log_err("utmscale_toInt64(toMax, %d, &status) generated U_ILLEGAL_ARGUMENT_ERROR.\n", scale);
        }
            
        if (toMax < U_INT64_MAX) {
            status = U_ZERO_ERROR;
            result = utmscale_toInt64(toMax + 1, scale, &status);
            if (status != U_ILLEGAL_ARGUMENT_ERROR) {
                log_err("utmscale_toInt64(toMax + 1, %d, &status) did not generate U_ILLEGAL_ARGUMENT_ERROR.\n", scale);
            }
        }
    }
    
    status = U_ZERO_ERROR;
    result = utmscale_toInt64(0, UDTS_MAX_SCALE, &status);
    if (status != U_ILLEGAL_ARGUMENT_ERROR) {
        log_err("utmscale_toInt64(0, UDTS_MAX_SCALE, &status) did not generate U_ILLEGAL_ARGUMENT_ERROR.\n");
    }
}

static void TestAPI(void)
{
    TestFromInt64();
    TestToInt64();
}

static void TestData(void)
{
    TestEpochOffsets();
    TestFromLimits();
    TestToLimits();
}

static void TestMonkey(void)
{
    int32_t scale;
    UErrorCode status = U_ZERO_ERROR;
    
    for (scale = 0; scale < UDTS_MAX_SCALE; scale += 1) {
        int64_t fromMin = utmscale_getTimeScaleValue(scale, UTSV_FROM_MIN_VALUE, &status);
        int64_t fromMax = utmscale_getTimeScaleValue(scale, UTSV_FROM_MAX_VALUE, &status);
        int32_t i;
        
        initRandom(fromMin, fromMax);
        
        for (i = 0; i < LOOP_COUNT; i += 1) {
            int64_t value = randomInRange();
                            
            roundTripTest(value, scale);
        }
    }
}

#endif /* #if !UCONFIG_NO_FORMATTING */
