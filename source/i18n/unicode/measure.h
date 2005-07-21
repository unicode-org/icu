/*
**********************************************************************
* Copyright (c) 2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Alan Liu
* Created: April 26, 2004
* Since: ICU 3.0
**********************************************************************
*/
#ifndef __MEASURE_H__
#define __MEASURE_H__

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/fmtable.h"

U_NAMESPACE_BEGIN

class MeasureUnit;

/**
 * An amount of a specified unit, consisting of a number and a Unit.
 * For example, a length measure consists of a number and a length
 * unit, such as feet or meters.  This is an abstract class.
 * Subclasses specify a concrete Unit type.
 *
 * <p>Measure objects are parsed and formatted by subclasses of
 * MeasureFormat.
 *
 * <p>Measure objects are immutable.
 *
 * <p>This is an abstract class.
 *
 * @author Alan Liu
 * @draft ICU 3.0
 */
class U_I18N_API Measure: public UObject {
 public:
    /**
     * Construct an object with the given numeric amount and the given
     * unit.  After this call, the caller must not delete the given
     * unit object.
     * @param number a numeric object; amount.isNumeric() must be TRUE
     * @param adoptedUnit the unit object, which must not be NULL
     * @param ec input-output error code. If the amount or the unit
     * is invalid, then this will be set to a failing value.
     * @draft ICU 3.0
     */
    Measure(const Formattable& number, MeasureUnit* adoptedUnit,
            UErrorCode& ec);

    /**
     * Copy constructor
     * @draft ICU 3.0
     */
    Measure(const Measure& other);

    /**
     * Assignment operator
     * @draft ICU 3.0
     */
    Measure& operator=(const Measure& other);

    /**
     * Return a polymorphic clone of this object.  The result will
     * have the same class as returned by getDynamicClassID().
     * @draft ICU 3.0
     */
    virtual UObject* clone() const = 0;

    /**
     * Destructor
     * @draft ICU 3.0
     */
    virtual ~Measure();
    
    /**
     * Equality operator.  Return true if this object is equal
     * to the given object.
     * @draft ICU 3.0
     */
    UBool operator==(const UObject& other) const;

    /**
     * Return a reference to the numeric value of this object.  The
     * numeric value may be of any numeric type supported by
     * Formattable.
     * @draft ICU 3.0
     */
    inline const Formattable& getNumber() const;

    /**
     * Return a reference to the unit of this object.
     * @draft ICU 3.0
     */
    inline const MeasureUnit& getUnit() const;

 protected:
    /**
     * Default constructor.
     * @draft ICU 3.0
     */
    Measure();

 private:
    /**
     * The numeric value of this object, e.g. 2.54 or 100.
     */
    Formattable number;

    /**
     * The unit of this object, e.g., "millimeter" or "JPY".  This is
     * owned by this object.
     */
    MeasureUnit* unit;
};

inline const Formattable& Measure::getNumber() const {
    return number;
}

inline const MeasureUnit& Measure::getUnit() const {
    return *unit;
}

U_NAMESPACE_END

#endif // !UCONFIG_NO_FORMATTING
#endif // __MEASURE_H__
