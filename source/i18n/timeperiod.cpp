/*
 *******************************************************************************
 * Copyright (C) 2013-2013, Google, International Business Machines Corporation
 * and others. All Rights Reserved.
 *******************************************************************************
 */

#include <math.h>
#include "unicode/timeperiod.h"
#include "putilimp.h"

#if !UCONFIG_NO_FORMATTING

U_NAMESPACE_BEGIN

TimePeriod::TimePeriod(const TimePeriod& other) : fLength(other.fLength) {
    for (int32_t i = 0; i < sizeof(fFields) / sizeof(fFields[0]); ++i) {
        if (other.fFields[i] == NULL) {
            fFields[i] = NULL;
        } else {
            fFields[i] = (TimeUnitAmount *) other.fFields[i]->clone();
        }
    }
}


TimePeriod::TimePeriod(
        const TimeUnitAmount * const *timeUnitAmounts,
        int32_t length,
        UErrorCode& status) : fLength(0) {
    int32_t fieldCount = sizeof(fFields) / sizeof(fFields[0]);
    for (int32_t i = 0; i < fieldCount; ++i) {
        fFields[i] = NULL;
    }
    if (U_FAILURE(status)) {
        return;
    }
    for (int32_t i = 0; i < length; ++i) {
        const TimeUnitAmount *tua = timeUnitAmounts[i];
        int32_t idx = tua->getTimeUnitField();
        if (fFields[idx] != NULL) {
            status = U_ILLEGAL_ARGUMENT_ERROR;
            return;
        }
        fFields[idx] = (TimeUnitAmount *) tua->clone();
        ++fLength;
    }
    validate(status);
    if (U_FAILURE(status)) {
        return;
    }
}

void TimePeriod::validate(UErrorCode& status) const {
    if (U_FAILURE(status)) {
        return;
    }
    if (fLength == 0) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }  
    int32_t fieldCount = sizeof(fFields) / sizeof(fFields[0]);
    UBool fractionalFieldEncountered = FALSE;
    for (int32_t i = 0; i < fieldCount; ++i) {
        if (fFields[i] == NULL) {
             continue;
        }
        if (fractionalFieldEncountered) {
            status = U_ILLEGAL_ARGUMENT_ERROR;
            return;
        }
        UErrorCode doubleStatus = U_ZERO_ERROR;
        double value = fFields[i]->getNumber().getDouble(doubleStatus);
        if (value != uprv_floor(value)) {
            fractionalFieldEncountered = TRUE;
        }
    }
}

TimePeriod::~TimePeriod() {
    int32_t fieldCount = sizeof(fFields) / sizeof(fFields[0]);
    for (int32_t i = 0; i < fieldCount; ++i) {
        delete fFields[i];
    }
}

UBool TimePeriod::operator==(const TimePeriod& other) const {
    // If same object, they are equal
    if (this == &other) {
        return TRUE;
    }
    int32_t fieldCount = sizeof(fFields) / sizeof(fFields[0]);
    for (int32_t i = 0; i < fieldCount; ++i) {
        if (fFields[i] == other.fFields[i]) {
            continue;
        }    
        // One is NULL, the other isn't.
        if (fFields[i] == NULL || other.fFields[i] == NULL) {
            return FALSE;
        }
        if (*fFields[i] != *other.fFields[i]) {
            return FALSE;
        }
    }
    return TRUE;
}
    
UBool TimePeriod::operator!=(const TimePeriod& other) const {
    return !(*this == other);
}

const TimeUnitAmount *TimePeriod::getAmount(
        TimeUnit::UTimeUnitFields field) const {
    return fFields[field];
}

U_NAMESPACE_END

#endif
