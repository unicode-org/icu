/*
**********************************************************************
* Copyright (c) 2004-2014, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Alan Liu
* Created: April 26, 2004
* Since: ICU 3.0
**********************************************************************
*/
#ifndef __MEASUREUNIT_H__
#define __MEASUREUNIT_H__

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/unistr.h"

/**
 * \file 
 * \brief C++ API: A unit for measuring a quantity.
 */
 
U_NAMESPACE_BEGIN

class StringEnumeration;

/**
 * A unit such as length, mass, volume, currency, etc.  A unit is
 * coupled with a numeric amount to produce a Measure.
 *
 * @author Alan Liu
 * @stable ICU 3.0
 */
class U_I18N_API MeasureUnit: public UObject {
 public:

    /**
     * Default constructor.
     * @stable ICU 3.0
     */
    MeasureUnit() : fTypeId(0), fSubTypeId(0) { 
        fCurrency[0] = 0;
    }
    
    /**
     * Copy constructor.
     * @draft ICU 53
     */
    MeasureUnit(const MeasureUnit &other);
        
    /**
     * Assignment operator.
     * @draft ICU 53
     */
    MeasureUnit &operator=(const MeasureUnit &other);

    /**
     * Returns a polymorphic clone of this object.  The result will
     * have the same class as returned by getDynamicClassID().
     * @stable ICU 3.0
     */
    virtual UObject* clone() const;

    /**
     * Destructor
     * @stable ICU 3.0
     */
    virtual ~MeasureUnit();

    /**
     * Equality operator.  Return true if this object is equal
     * to the given object.
     * @stable ICU 3.0
     */
    virtual UBool operator==(const UObject& other) const;

#ifndef U_HIDE_DRAFT_API
    /**
     * Inequality operator.  Return true if this object is not equal
     * to the given object.
     * @draft ICU 53
     */
    UBool operator!=(const UObject& other) const {
        return !(*this == other);
    }

    /**
     * Get the type.
     * @draft ICU 53
     */
    const char *getType() const;

    /**
     * Get the sub type.
     * @draft ICU 53
     */
    const char *getSubtype() const;

    /**
     * getAvailable gets all of the available units.
     * If there are too many units to fit into destCapacity then the
     * error code is set to U_BUFFER_OVERFLOW_ERROR.
     *
     * @param destArray destination buffer.
     * @param destCapacity number of MeasureUnit instances available at dest.
     * @param errorCode ICU error code.
     * @return number of available units.
     * @draft ICU 53
     */
    static int32_t getAvailable(
            MeasureUnit *destArray,
            int32_t destCapacity,
            UErrorCode &errorCode);

    /**
     * getAvailable gets all of the available units for a specific type.
     * If there are too many units to fit into destCapacity then the
     * error code is set to U_BUFFER_OVERFLOW_ERROR.
     *
     * @param type the type
     * @param destArray destination buffer.
     * @param destCapacity number of MeasureUnit instances available at dest.
     * @param errorCode ICU error code.
     * @return number of available units for type.
     * @draft ICU 53
     */
    static int32_t getAvailable(
            const char *type,
            MeasureUnit *destArray,
            int32_t destCapacity,
            UErrorCode &errorCode);

    /**
     * getAvailableTypes gets all of the available types. Caller owns the
     * returned StringEnumeration and must delete it when finished using it.
     *
     * @param errorCode ICU error code.
     * @return the types.
     * @draft ICU 53
     */
    static StringEnumeration* getAvailableTypes(UErrorCode &errorCode);
#endif /* U_HIDE_DRAFT_API */

    /**
     * Return the class ID for this class. This is useful only for comparing to
     * a return value from getDynamicClassID(). For example:
     * <pre>
     * .   Base* polymorphic_pointer = createPolymorphicObject();
     * .   if (polymorphic_pointer->getDynamicClassID() ==
     * .       erived::getStaticClassID()) ...
     * </pre>
     * @return          The class ID for all objects of this class.
     * @draft ICU 53
     */
    static UClassID U_EXPORT2 getStaticClassID(void);

    /**
     * Returns a unique class ID POLYMORPHICALLY. Pure virtual override. This
     * method is to implement a simple version of RTTI, since not all C++
     * compilers support genuine RTTI. Polymorphic operator==() and clone()
     * methods call this method.
     *
     * @return          The class ID for this object. All objects of a
     *                  given class have the same class ID.  Objects of
     *                  other classes have different class IDs.
     * @draft ICU 53
     */
    virtual UClassID getDynamicClassID(void) const;

#ifndef U_HIDE_INTERNAL_API
    /**
     * ICU use only.
     * Returns associated array index for this measure unit. Only valid for
     * non-currency measure units.
     * @internal
     */
    int32_t getIndex() const;

    /**
     * ICU use only.
     * Returns maximum value from getIndex plus 1.
     * @internal
     */
    static int32_t getIndexCount();
#endif /* U_HIDE_INTERNAL_API */

// Start generated createXXX methods

#ifndef U_HIDE_DRAFT_API
    /**
     * Creates a unit of acceleration: g-force.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createGForce(UErrorCode &status);

    /**
     * Creates a unit of angle: arc-minute.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createArcMinute(UErrorCode &status);

    /**
     * Creates a unit of angle: arc-second.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createArcSecond(UErrorCode &status);

    /**
     * Creates a unit of angle: degree.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createDegree(UErrorCode &status);

    /**
     * Creates a unit of area: acre.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createAcre(UErrorCode &status);

    /**
     * Creates a unit of area: hectare.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createHectare(UErrorCode &status);

    /**
     * Creates a unit of area: square-foot.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createSquareFoot(UErrorCode &status);

    /**
     * Creates a unit of area: square-kilometer.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createSquareKilometer(UErrorCode &status);

    /**
     * Creates a unit of area: square-meter.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createSquareMeter(UErrorCode &status);

    /**
     * Creates a unit of area: square-mile.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createSquareMile(UErrorCode &status);

    /**
     * Creates a unit of duration: day.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createDay(UErrorCode &status);

    /**
     * Creates a unit of duration: hour.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createHour(UErrorCode &status);

    /**
     * Creates a unit of duration: millisecond.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createMillisecond(UErrorCode &status);

    /**
     * Creates a unit of duration: minute.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createMinute(UErrorCode &status);

    /**
     * Creates a unit of duration: month.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createMonth(UErrorCode &status);

    /**
     * Creates a unit of duration: second.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createSecond(UErrorCode &status);

    /**
     * Creates a unit of duration: week.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createWeek(UErrorCode &status);

    /**
     * Creates a unit of duration: year.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createYear(UErrorCode &status);

    /**
     * Creates a unit of length: centimeter.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createCentimeter(UErrorCode &status);

    /**
     * Creates a unit of length: foot.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createFoot(UErrorCode &status);

    /**
     * Creates a unit of length: inch.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createInch(UErrorCode &status);

    /**
     * Creates a unit of length: kilometer.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createKilometer(UErrorCode &status);

    /**
     * Creates a unit of length: light-year.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createLightYear(UErrorCode &status);

    /**
     * Creates a unit of length: meter.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createMeter(UErrorCode &status);

    /**
     * Creates a unit of length: mile.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createMile(UErrorCode &status);

    /**
     * Creates a unit of length: millimeter.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createMillimeter(UErrorCode &status);

    /**
     * Creates a unit of length: picometer.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createPicometer(UErrorCode &status);

    /**
     * Creates a unit of length: yard.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createYard(UErrorCode &status);

    /**
     * Creates a unit of mass: gram.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createGram(UErrorCode &status);

    /**
     * Creates a unit of mass: kilogram.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createKilogram(UErrorCode &status);

    /**
     * Creates a unit of mass: ounce.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createOunce(UErrorCode &status);

    /**
     * Creates a unit of mass: pound.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createPound(UErrorCode &status);

    /**
     * Creates a unit of power: horsepower.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createHorsepower(UErrorCode &status);

    /**
     * Creates a unit of power: kilowatt.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createKilowatt(UErrorCode &status);

    /**
     * Creates a unit of power: watt.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createWatt(UErrorCode &status);

    /**
     * Creates a unit of pressure: hectopascal.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createHectopascal(UErrorCode &status);

    /**
     * Creates a unit of pressure: inch-hg.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createInchHg(UErrorCode &status);

    /**
     * Creates a unit of pressure: millibar.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createMillibar(UErrorCode &status);

    /**
     * Creates a unit of speed: kilometer-per-hour.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createKilometerPerHour(UErrorCode &status);

    /**
     * Creates a unit of speed: meter-per-second.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createMeterPerSecond(UErrorCode &status);

    /**
     * Creates a unit of speed: mile-per-hour.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createMilePerHour(UErrorCode &status);

    /**
     * Creates a unit of temperature: celsius.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createCelsius(UErrorCode &status);

    /**
     * Creates a unit of temperature: fahrenheit.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createFahrenheit(UErrorCode &status);

    /**
     * Creates a unit of volume: cubic-kilometer.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createCubicKilometer(UErrorCode &status);

    /**
     * Creates a unit of volume: cubic-mile.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createCubicMile(UErrorCode &status);

    /**
     * Creates a unit of volume: liter.
     * Caller owns returned value and must free it.
     * @param status ICU error code.
     * @draft ICU 53
     */
    static MeasureUnit *createLiter(UErrorCode &status);
#endif /* U_HIDE_DRAFT_API */

 protected:

#ifndef U_HIDE_INTERNAL_API
    /**
     * For ICU use only.
     * @internal
     */
    void initTime(const char *timeId);

    /**
     * For ICU use only.
     * @internal
     */
    void initCurrency(const char *isoCurrency);

#endif

private:
    int32_t fTypeId;
    int32_t fSubTypeId;
    char fCurrency[4];

    MeasureUnit(int32_t typeId, int32_t subTypeId) : fTypeId(typeId), fSubTypeId(subTypeId) {
        fCurrency[0] = 0;
    }
    void setTo(int32_t typeId, int32_t subTypeId);
    int32_t getOffset() const;
    static MeasureUnit *create(int typeId, int subTypeId, UErrorCode &status);
};

U_NAMESPACE_END

#endif // !UNCONFIG_NO_FORMATTING
#endif // __MEASUREUNIT_H__
