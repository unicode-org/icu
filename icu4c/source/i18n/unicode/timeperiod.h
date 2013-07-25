/*
 *******************************************************************************
 * Copyright (C) 2013-2013, Google, International Business Machines Corporation
 * and others. All Rights Reserved.
 *******************************************************************************
 */

#ifndef __TIMEPERIOD_H__
#define __TIMEPERIOD_H__

// TODO:
// 1. Add size() method and test.
// 2. testEquals() -> testTimePeriodEquals()

#include "unicode/utypes.h"

/**
 * \file
 * \brief C++ API: Format and parse duration in single time unit
 */


#if !UCONFIG_NO_FORMATTING

#include "unicode/tmunit.h"
#include "unicode/tmutamt.h"

U_NAMESPACE_BEGIN

class U_I18N_API TimePeriod: public UObject {
public:
    // copy constructor.
    TimePeriod(const TimePeriod& other);

    /** 
     * Constructor.
     * @param timeUnitAmounts an array of TimeUnitAmounts pointers. TimePeriod copies the
     * data in this array. The caller is responsible for freeing the TimeUnitAmount objects
     * and the array.
     * @param length the number of timeUnitAmount pointers in timeUnitAmounts array.
     * @param status error returned here if timeUnitAmounts is empty;
     *    timeUnitAmounts has duplicate time units; or any timeUnitAmount except the 
     *    smallest has a non-integer value.
     * @draft ICU 52
     */  
    TimePeriod(
        const TimeUnitAmount * const *timeUnitAmounts, int32_t length, UErrorCode& status);

    /** 
     * Destructor
     * @draft ICU 52
     */  
    virtual ~TimePeriod();

    /** 
     * Returns true if the given TimePeriod objects are semantically equal.
     * For two TimePeriod objects to be equal, they must contain the same
     * units, and the amount for each unit much be equal.  For example,
     * 5 hours, 37 minutes == 37 minutes, 5 hours,
     * but 0 days, 5 hours != 5 hours.
     * @param that the TimePeriod to compare with. 
     * @return true if the given TimePeriod objects are semantically equal.
     * @draft ICU 52
     */  
    UBool operator==(const TimePeriod& that) const;

    /** 
     * Returns true if the given TimePeriod objects are semantically
     * unequal.
     * @param that the TimePeriod to compare with. 
     * @return true if the given TimePeriod objects are semantically
     * unequal.
     * @draft ICU 52
     */  
    UBool operator!=(const TimePeriod& that) const;

    /** 
     * Gets a specific field out of a time period.
     * @param field is the field to fetch
     * @return The desired field or NULL if it does not exist.
     */  
    const TimeUnitAmount* getAmount(TimeUnit::UTimeUnitFields field) const;

    /**
     * Returns the number of time units in this object.
     * @draft ICU 52
     */
    inline int32_t length() const {
        return fLength;
    }

private:
    int32_t fLength;
    TimeUnitAmount *fFields[TimeUnit::UTIMEUNIT_FIELD_COUNT];

    // Clients use forAmount and never use ctor directly.
    TimePeriod() {
    }   

    /** 
     * No assignment operator needed because class is immutable.
     */  
    TimePeriod& operator=(const TimePeriod& other);

    void validate(UErrorCode& status) const;
};

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif // __TMUTFMT_H__
//eof
