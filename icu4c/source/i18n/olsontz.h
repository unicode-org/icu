/*
**********************************************************************
* Copyright (c) 2003, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Alan Liu
* Created: July 21 2003
* Since: ICU 2.8
**********************************************************************
*/
#ifndef OLSONTZ_H
#define OLSONTZ_H
#if !UCONFIG_NO_FORMATTING

#include "unicode/utypes.h"
#include "unicode/timezone.h"

struct UResourceBundle;

U_NAMESPACE_BEGIN

class SimpleTimeZone;

class U_I18N_API OlsonTimeZone: public TimeZone {
 public:
    /**
     * Construct from a resource bundle.
     * @param top the top-level zoneinfo resource bundle.  This is used
     * to lookup the rule that `res' may refer to, if there is one.
     * @param res the resource bundle of the zone to be constructed
     * @param ec input-output error code
     */
    OlsonTimeZone(const UResourceBundle* top,
                  const UResourceBundle* res, UErrorCode& ec);

    /**
     * Copy constructor
     */
    OlsonTimeZone(const OlsonTimeZone& other);

    /**
     * Destructor
     */
    virtual ~OlsonTimeZone();

    /**
     * Assignment operator
     */
    OlsonTimeZone& operator=(const OlsonTimeZone& other);

    /**
     * Returns true if the two TimeZone objects are equal.
     */
    virtual UBool operator==(const TimeZone& other) const;

    /**
     * TimeZone API.
     */
    virtual TimeZone* clone() const;

    /**
     * TimeZone API.
     */
    static UClassID getStaticClassID();

    /**
     * TimeZone API.
     */
    virtual UClassID getDynamicClassID() const;
    
    /**
     * TimeZone API.  Do not call this; prefer getOffset(UDate,...).
     */
    virtual int32_t getOffset(uint8_t era, int32_t year, int32_t month,
                              int32_t day, uint8_t dayOfWeek,
                              int32_t millis, UErrorCode& ec) const;

    /**
     * TimeZone API.  Do not call this; prefer getOffset(UDate,...).
     */
    virtual int32_t getOffset(uint8_t era, int32_t year, int32_t month,
                              int32_t day, uint8_t dayOfWeek,
                              int32_t millis, int32_t monthLength,
                              UErrorCode& ec) const;

    /**
     * TimeZone API.
     */
    virtual void getOffset(UDate date, UBool local, int32_t& rawOffset,
                   int32_t& dstOffset, UErrorCode& ec) const;

    /**
     * TimeZone API.  This method has no effect since objects of this
     * class are quasi-immutable (the base class allows the ID to be
     * changed).
     */
    virtual void setRawOffset(int32_t offsetMillis);

    /**
     * TimeZone API.  For a historical zone, the raw offset can change
     * over time, so this API is not useful.  In order to approximate
     * expected behavior, this method returns the raw offset for the
     * current moment in time.
     */
    virtual int32_t getRawOffset() const;

    /**
     * TimeZone API.  For a historical zone, whether DST is used or
     * not varies over time.  In order to approximate expected
     * behavior, this method returns TRUE if DST is observed at any
     * point in the current year.
     */
    virtual UBool useDaylightTime() const;

    /**
     * TimeZone API.
     */
    virtual UBool inDaylightTime(UDate date, UErrorCode& ec) const;

 protected:
    /**
     * Default constructor.  Creates a time zone with an empty ID and
     * a fixed GMT offset of zero.
     */
    OlsonTimeZone();

 private:

    void constructEmpty();

    int16_t findTransition(double time, UBool local) const;

    int32_t zoneOffset(int16_t index) const;
    int32_t rawOffset(int16_t index) const;
    int32_t dstOffset(int16_t index) const;

    /**
     * Number of transitions, 0..~370
     */
    int16_t transitionCount;

    /**
     * Number of types, 1..255
     */
    int16_t typeCount;

    /**
     * Time of each transition in seconds from 1970 epoch.
     * Length is transitionCount int32_t's.
     */
    const int32_t *transitionTimes; // alias into res; do not delete

    /**
     * Offset from GMT in seconds for each type.
     * Length is typeCount int32_t's.
     */
    const int32_t *typeOffsets; // alias into res; do not delete

    /**
     * Type description data, consisting of transitionCount uint8_t
     * type indices (from 0..typeCount-1).
     * Length is transitionCount int8_t's.
     */
    const uint8_t *typeData; // alias into res; do not delete

    /**
     * The last year for which the transitions data are to be used
     * rather than the finalZone.  If there is no finalZone, then this
     * is set to INT32_MAX.
     */
    int32_t finalYear;

    /**
     * A SimpleTimeZone that governs the behavior for years > finalYear.
     * If and only if finalYear == INT32_MAX then finalZone == 0.
     */
    SimpleTimeZone *finalZone; // owned, may be NULL

};

inline int32_t
OlsonTimeZone::zoneOffset(int16_t index) const {
    index <<= 1;
    return typeOffsets[index] + typeOffsets[index+1];
}

inline int32_t
OlsonTimeZone::rawOffset(int16_t index) const {
    return typeOffsets[index << 1];
}

inline int32_t
OlsonTimeZone::dstOffset(int16_t index) const {
    return typeOffsets[(index << 1) + 1];
}

U_NAMESPACE_END

#endif // !UCONFIG_NO_FORMATTING
#endif // OLSONTZ_H

//eof
