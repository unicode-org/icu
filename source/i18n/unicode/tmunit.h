/*
 *******************************************************************************
 * Copyright (C) 2008, Google, International Business Machines Corporation and *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

#ifndef __TMUNIT_H__
#define __TMUNIT_H__


/**
 * \file
 * \brief C++ API: time unit object
 */


#if !UCONFIG_NO_FORMATTING

#include "unicode/measunit.h"

U_NAMESPACE_BEGIN

/**
 * Measurement unit for time units.
 * @see TimeUnitAmount
 * @see TimeUnit
 * @draft ICU 4.2
 */
class U_I18N_API TimeUnit: public MeasureUnit {
public:
    /**
     * Constants for all the time units we supported.
     * @draft ICU 4.2
     */
    enum UTimeUnitFields {
        UTIMEUNIT_YEAR,
        UTIMEUNIT_MONTH,
        UTIMEUNIT_DAY,
        UTIMEUNIT_WEEK,
        UTIMEUNIT_HOUR,
        UTIMEUNIT_MINUTE,
        UTIMEUNIT_SECOND,
        UTIMEUNIT_FIELD_COUNT
    };

    /**
     * Create Instance.
     * @param timeUnitField  time unit field based on which the instance 
     *                       is created.
     * @param status         input-output error code. 
     *                       If the timeUnitField is invalid,
     *                       then this will be set to U_ILLEGAL_ARGUMENT_ERROR.
     * @return               a TimeUnit instance
     * @draft ICU 4.2 
     */
    static TimeUnit* U_EXPORT2 createInstance(UTimeUnitFields timeUnitField,
                                              UErrorCode& status);


    /**
     * Override clone.
     * @draft ICU 4.2 
     */
    virtual UObject* clone() const;

    /**
     * Copy operator.
     * @draft ICU 4.2 
     */
    TimeUnit(const TimeUnit& other);

    /**
     * Assignment operator.
     * @draft ICU 4.2 
     */
    TimeUnit& operator=(const TimeUnit& other);

    /**
     * Equality operator. 
     * @return true if 2 objects are the same.
     * @draft ICU 4.2 
     */
    virtual UBool operator==(const UObject& other) const;

    /**
     * Non-Equality operator. 
     * @return true if 2 objects are not the same.
     * @draft ICU 4.2 
     */
    UBool operator!=(const UObject& other) const;

    /**
     * Returns a unique class ID for this object POLYMORPHICALLY.
     * This method implements a simple form of RTTI used by ICU.
     * @return The class ID for this object. All objects of a given
     * class have the same class ID.  Objects of other classes have
     * different class IDs.
     * @draft ICU 4.2 
     */
    virtual UClassID getDynamicClassID() const;

    /**
     * Returns the class ID for this class. This is used to compare to
     * the return value of getDynamicClassID().
     * @return The class ID for all objects of this class.
     * @draft ICU 4.2 
     */
    static UClassID U_EXPORT2 getStaticClassID();


    /**
     * Get time unit field.
     * @return time unit field.
     * @draft ICU 4.2 
     */
    UTimeUnitFields getTimeUnitField() const;

    /**
     * Destructor.
     * @draft ICU 4.2 
     */
    virtual ~TimeUnit();

private:
    UTimeUnitFields fTimeUnitField;

    /**
     * Constructor
     * @internal ICU 4.2 
     */
    TimeUnit(UTimeUnitFields timeUnitField);

};


inline UBool 
TimeUnit::operator!=(const UObject& other) const {
    return !operator==(other);
}


U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif // __TMUNIT_H__
//eof
//
