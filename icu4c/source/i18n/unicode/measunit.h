// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
**********************************************************************
* Copyright (c) 2004-2016, International Business Machines
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
     * Populates the instance with the base dimensionless unit.
     * @stable ICU 3.0
     */
    MeasureUnit();
    
    /**
     * Copy constructor.
     * @stable ICU 3.0
     */
    MeasureUnit(const MeasureUnit &other);
        
    /**
     * Assignment operator.
     * @stable ICU 3.0
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

    /**
     * Inequality operator.  Return true if this object is not equal
     * to the given object.
     * @stable ICU 53
     */
    UBool operator!=(const UObject& other) const {
        return !(*this == other);
    }

    /**
     * Get the type.
     * @stable ICU 53
     */
    const char *getType() const;

    /**
     * Get the sub type.
     * @stable ICU 53
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
     * @stable ICU 53
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
     * @stable ICU 53
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
     * @stable ICU 53
     */
    static StringEnumeration* getAvailableTypes(UErrorCode &errorCode);

    /**
     * Return the class ID for this class. This is useful only for comparing to
     * a return value from getDynamicClassID(). For example:
     * <pre>
     * .   Base* polymorphic_pointer = createPolymorphicObject();
     * .   if (polymorphic_pointer->getDynamicClassID() ==
     * .       Derived::getStaticClassID()) ...
     * </pre>
     * @return          The class ID for all objects of this class.
     * @stable ICU 53
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
     * @stable ICU 53
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

    /**
     * ICU use only.
     * @return the unit.getIndex() of the unit which has this unit.getType() and unit.getSubtype(),
     *         or a negative value if there is no such unit
     * @internal
     */
    static int32_t internalGetIndexForTypeAndSubtype(const char *type, const char *subtype);

    /**
     * ICU use only.
     * @internal
     */
    static MeasureUnit resolveUnitPerUnit(
            const MeasureUnit &unit, const MeasureUnit &perUnit, bool* isResolved);
#endif /* U_HIDE_INTERNAL_API */

// All code between the "Start generated createXXX methods" comment and
// the "End generated createXXX methods" comment is auto generated code
// and must not be edited manually. For instructions on how to correctly
// update this code, refer to:
// http://site.icu-project.org/design/formatting/measureformat/updating-measure-unit
//
// Start generated createXXX methods

    /**
     * Returns by pointer, unit of acceleration: g-force.
     * Caller owns returned value and must free it.
     * Also see {@link #getGForce()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createGForce(UErrorCode &status);

    /**
     * Returns by value, unit of acceleration: g-force.
     * Also see {@link #createGForce()}.
     * @draft ICU 64
     */
    static MeasureUnit getGForce();

    /**
     * Returns by pointer, unit of acceleration: meter-per-second-squared.
     * Caller owns returned value and must free it.
     * Also see {@link #getMeterPerSecondSquared()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createMeterPerSecondSquared(UErrorCode &status);

    /**
     * Returns by value, unit of acceleration: meter-per-second-squared.
     * Also see {@link #createMeterPerSecondSquared()}.
     * @draft ICU 64
     */
    static MeasureUnit getMeterPerSecondSquared();

    /**
     * Returns by pointer, unit of angle: arc-minute.
     * Caller owns returned value and must free it.
     * Also see {@link #getArcMinute()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createArcMinute(UErrorCode &status);

    /**
     * Returns by value, unit of angle: arc-minute.
     * Also see {@link #createArcMinute()}.
     * @draft ICU 64
     */
    static MeasureUnit getArcMinute();

    /**
     * Returns by pointer, unit of angle: arc-second.
     * Caller owns returned value and must free it.
     * Also see {@link #getArcSecond()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createArcSecond(UErrorCode &status);

    /**
     * Returns by value, unit of angle: arc-second.
     * Also see {@link #createArcSecond()}.
     * @draft ICU 64
     */
    static MeasureUnit getArcSecond();

    /**
     * Returns by pointer, unit of angle: degree.
     * Caller owns returned value and must free it.
     * Also see {@link #getDegree()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createDegree(UErrorCode &status);

    /**
     * Returns by value, unit of angle: degree.
     * Also see {@link #createDegree()}.
     * @draft ICU 64
     */
    static MeasureUnit getDegree();

    /**
     * Returns by pointer, unit of angle: radian.
     * Caller owns returned value and must free it.
     * Also see {@link #getRadian()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createRadian(UErrorCode &status);

    /**
     * Returns by value, unit of angle: radian.
     * Also see {@link #createRadian()}.
     * @draft ICU 64
     */
    static MeasureUnit getRadian();

    /**
     * Returns by pointer, unit of angle: revolution.
     * Caller owns returned value and must free it.
     * Also see {@link #getRevolutionAngle()}.
     * @param status ICU error code.
     * @stable ICU 56
     */
    static MeasureUnit *createRevolutionAngle(UErrorCode &status);

    /**
     * Returns by value, unit of angle: revolution.
     * Also see {@link #createRevolutionAngle()}.
     * @draft ICU 64
     */
    static MeasureUnit getRevolutionAngle();

    /**
     * Returns by pointer, unit of area: acre.
     * Caller owns returned value and must free it.
     * Also see {@link #getAcre()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createAcre(UErrorCode &status);

    /**
     * Returns by value, unit of area: acre.
     * Also see {@link #createAcre()}.
     * @draft ICU 64
     */
    static MeasureUnit getAcre();

    /**
     * Returns by pointer, unit of area: hectare.
     * Caller owns returned value and must free it.
     * Also see {@link #getHectare()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createHectare(UErrorCode &status);

    /**
     * Returns by value, unit of area: hectare.
     * Also see {@link #createHectare()}.
     * @draft ICU 64
     */
    static MeasureUnit getHectare();

    /**
     * Returns by pointer, unit of area: square-centimeter.
     * Caller owns returned value and must free it.
     * Also see {@link #getSquareCentimeter()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createSquareCentimeter(UErrorCode &status);

    /**
     * Returns by value, unit of area: square-centimeter.
     * Also see {@link #createSquareCentimeter()}.
     * @draft ICU 64
     */
    static MeasureUnit getSquareCentimeter();

    /**
     * Returns by pointer, unit of area: square-foot.
     * Caller owns returned value and must free it.
     * Also see {@link #getSquareFoot()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createSquareFoot(UErrorCode &status);

    /**
     * Returns by value, unit of area: square-foot.
     * Also see {@link #createSquareFoot()}.
     * @draft ICU 64
     */
    static MeasureUnit getSquareFoot();

    /**
     * Returns by pointer, unit of area: square-inch.
     * Caller owns returned value and must free it.
     * Also see {@link #getSquareInch()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createSquareInch(UErrorCode &status);

    /**
     * Returns by value, unit of area: square-inch.
     * Also see {@link #createSquareInch()}.
     * @draft ICU 64
     */
    static MeasureUnit getSquareInch();

    /**
     * Returns by pointer, unit of area: square-kilometer.
     * Caller owns returned value and must free it.
     * Also see {@link #getSquareKilometer()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createSquareKilometer(UErrorCode &status);

    /**
     * Returns by value, unit of area: square-kilometer.
     * Also see {@link #createSquareKilometer()}.
     * @draft ICU 64
     */
    static MeasureUnit getSquareKilometer();

    /**
     * Returns by pointer, unit of area: square-meter.
     * Caller owns returned value and must free it.
     * Also see {@link #getSquareMeter()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createSquareMeter(UErrorCode &status);

    /**
     * Returns by value, unit of area: square-meter.
     * Also see {@link #createSquareMeter()}.
     * @draft ICU 64
     */
    static MeasureUnit getSquareMeter();

    /**
     * Returns by pointer, unit of area: square-mile.
     * Caller owns returned value and must free it.
     * Also see {@link #getSquareMile()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createSquareMile(UErrorCode &status);

    /**
     * Returns by value, unit of area: square-mile.
     * Also see {@link #createSquareMile()}.
     * @draft ICU 64
     */
    static MeasureUnit getSquareMile();

    /**
     * Returns by pointer, unit of area: square-yard.
     * Caller owns returned value and must free it.
     * Also see {@link #getSquareYard()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createSquareYard(UErrorCode &status);

    /**
     * Returns by value, unit of area: square-yard.
     * Also see {@link #createSquareYard()}.
     * @draft ICU 64
     */
    static MeasureUnit getSquareYard();

    /**
     * Returns by pointer, unit of concentr: karat.
     * Caller owns returned value and must free it.
     * Also see {@link #getKarat()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createKarat(UErrorCode &status);

    /**
     * Returns by value, unit of concentr: karat.
     * Also see {@link #createKarat()}.
     * @draft ICU 64
     */
    static MeasureUnit getKarat();

    /**
     * Returns by pointer, unit of concentr: milligram-per-deciliter.
     * Caller owns returned value and must free it.
     * Also see {@link #getMilligramPerDeciliter()}.
     * @param status ICU error code.
     * @stable ICU 57
     */
    static MeasureUnit *createMilligramPerDeciliter(UErrorCode &status);

    /**
     * Returns by value, unit of concentr: milligram-per-deciliter.
     * Also see {@link #createMilligramPerDeciliter()}.
     * @draft ICU 64
     */
    static MeasureUnit getMilligramPerDeciliter();

    /**
     * Returns by pointer, unit of concentr: millimole-per-liter.
     * Caller owns returned value and must free it.
     * Also see {@link #getMillimolePerLiter()}.
     * @param status ICU error code.
     * @stable ICU 57
     */
    static MeasureUnit *createMillimolePerLiter(UErrorCode &status);

    /**
     * Returns by value, unit of concentr: millimole-per-liter.
     * Also see {@link #createMillimolePerLiter()}.
     * @draft ICU 64
     */
    static MeasureUnit getMillimolePerLiter();

    /**
     * Returns by pointer, unit of concentr: part-per-million.
     * Caller owns returned value and must free it.
     * Also see {@link #getPartPerMillion()}.
     * @param status ICU error code.
     * @stable ICU 57
     */
    static MeasureUnit *createPartPerMillion(UErrorCode &status);

    /**
     * Returns by value, unit of concentr: part-per-million.
     * Also see {@link #createPartPerMillion()}.
     * @draft ICU 64
     */
    static MeasureUnit getPartPerMillion();

#ifndef U_HIDE_DRAFT_API
    /**
     * Returns by pointer, unit of concentr: percent.
     * Caller owns returned value and must free it.
     * Also see {@link #getPercent()}.
     * @param status ICU error code.
     * @draft ICU 63
     */
    static MeasureUnit *createPercent(UErrorCode &status);

    /**
     * Returns by value, unit of concentr: percent.
     * Also see {@link #createPercent()}.
     * @draft ICU 64
     */
    static MeasureUnit getPercent();
#endif /* U_HIDE_DRAFT_API */

#ifndef U_HIDE_DRAFT_API
    /**
     * Returns by pointer, unit of concentr: permille.
     * Caller owns returned value and must free it.
     * Also see {@link #getPermille()}.
     * @param status ICU error code.
     * @draft ICU 63
     */
    static MeasureUnit *createPermille(UErrorCode &status);

    /**
     * Returns by value, unit of concentr: permille.
     * Also see {@link #createPermille()}.
     * @draft ICU 64
     */
    static MeasureUnit getPermille();
#endif /* U_HIDE_DRAFT_API */

    /**
     * Returns by pointer, unit of consumption: liter-per-100kilometers.
     * Caller owns returned value and must free it.
     * Also see {@link #getLiterPer100Kilometers()}.
     * @param status ICU error code.
     * @stable ICU 56
     */
    static MeasureUnit *createLiterPer100Kilometers(UErrorCode &status);

    /**
     * Returns by value, unit of consumption: liter-per-100kilometers.
     * Also see {@link #createLiterPer100Kilometers()}.
     * @draft ICU 64
     */
    static MeasureUnit getLiterPer100Kilometers();

    /**
     * Returns by pointer, unit of consumption: liter-per-kilometer.
     * Caller owns returned value and must free it.
     * Also see {@link #getLiterPerKilometer()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createLiterPerKilometer(UErrorCode &status);

    /**
     * Returns by value, unit of consumption: liter-per-kilometer.
     * Also see {@link #createLiterPerKilometer()}.
     * @draft ICU 64
     */
    static MeasureUnit getLiterPerKilometer();

    /**
     * Returns by pointer, unit of consumption: mile-per-gallon.
     * Caller owns returned value and must free it.
     * Also see {@link #getMilePerGallon()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createMilePerGallon(UErrorCode &status);

    /**
     * Returns by value, unit of consumption: mile-per-gallon.
     * Also see {@link #createMilePerGallon()}.
     * @draft ICU 64
     */
    static MeasureUnit getMilePerGallon();

    /**
     * Returns by pointer, unit of consumption: mile-per-gallon-imperial.
     * Caller owns returned value and must free it.
     * Also see {@link #getMilePerGallonImperial()}.
     * @param status ICU error code.
     * @stable ICU 57
     */
    static MeasureUnit *createMilePerGallonImperial(UErrorCode &status);

    /**
     * Returns by value, unit of consumption: mile-per-gallon-imperial.
     * Also see {@link #createMilePerGallonImperial()}.
     * @draft ICU 64
     */
    static MeasureUnit getMilePerGallonImperial();

    /**
     * Returns by pointer, unit of digital: bit.
     * Caller owns returned value and must free it.
     * Also see {@link #getBit()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createBit(UErrorCode &status);

    /**
     * Returns by value, unit of digital: bit.
     * Also see {@link #createBit()}.
     * @draft ICU 64
     */
    static MeasureUnit getBit();

    /**
     * Returns by pointer, unit of digital: byte.
     * Caller owns returned value and must free it.
     * Also see {@link #getByte()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createByte(UErrorCode &status);

    /**
     * Returns by value, unit of digital: byte.
     * Also see {@link #createByte()}.
     * @draft ICU 64
     */
    static MeasureUnit getByte();

    /**
     * Returns by pointer, unit of digital: gigabit.
     * Caller owns returned value and must free it.
     * Also see {@link #getGigabit()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createGigabit(UErrorCode &status);

    /**
     * Returns by value, unit of digital: gigabit.
     * Also see {@link #createGigabit()}.
     * @draft ICU 64
     */
    static MeasureUnit getGigabit();

    /**
     * Returns by pointer, unit of digital: gigabyte.
     * Caller owns returned value and must free it.
     * Also see {@link #getGigabyte()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createGigabyte(UErrorCode &status);

    /**
     * Returns by value, unit of digital: gigabyte.
     * Also see {@link #createGigabyte()}.
     * @draft ICU 64
     */
    static MeasureUnit getGigabyte();

    /**
     * Returns by pointer, unit of digital: kilobit.
     * Caller owns returned value and must free it.
     * Also see {@link #getKilobit()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createKilobit(UErrorCode &status);

    /**
     * Returns by value, unit of digital: kilobit.
     * Also see {@link #createKilobit()}.
     * @draft ICU 64
     */
    static MeasureUnit getKilobit();

    /**
     * Returns by pointer, unit of digital: kilobyte.
     * Caller owns returned value and must free it.
     * Also see {@link #getKilobyte()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createKilobyte(UErrorCode &status);

    /**
     * Returns by value, unit of digital: kilobyte.
     * Also see {@link #createKilobyte()}.
     * @draft ICU 64
     */
    static MeasureUnit getKilobyte();

    /**
     * Returns by pointer, unit of digital: megabit.
     * Caller owns returned value and must free it.
     * Also see {@link #getMegabit()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createMegabit(UErrorCode &status);

    /**
     * Returns by value, unit of digital: megabit.
     * Also see {@link #createMegabit()}.
     * @draft ICU 64
     */
    static MeasureUnit getMegabit();

    /**
     * Returns by pointer, unit of digital: megabyte.
     * Caller owns returned value and must free it.
     * Also see {@link #getMegabyte()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createMegabyte(UErrorCode &status);

    /**
     * Returns by value, unit of digital: megabyte.
     * Also see {@link #createMegabyte()}.
     * @draft ICU 64
     */
    static MeasureUnit getMegabyte();

#ifndef U_HIDE_DRAFT_API
    /**
     * Returns by pointer, unit of digital: petabyte.
     * Caller owns returned value and must free it.
     * Also see {@link #getPetabyte()}.
     * @param status ICU error code.
     * @draft ICU 63
     */
    static MeasureUnit *createPetabyte(UErrorCode &status);

    /**
     * Returns by value, unit of digital: petabyte.
     * Also see {@link #createPetabyte()}.
     * @draft ICU 64
     */
    static MeasureUnit getPetabyte();
#endif /* U_HIDE_DRAFT_API */

    /**
     * Returns by pointer, unit of digital: terabit.
     * Caller owns returned value and must free it.
     * Also see {@link #getTerabit()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createTerabit(UErrorCode &status);

    /**
     * Returns by value, unit of digital: terabit.
     * Also see {@link #createTerabit()}.
     * @draft ICU 64
     */
    static MeasureUnit getTerabit();

    /**
     * Returns by pointer, unit of digital: terabyte.
     * Caller owns returned value and must free it.
     * Also see {@link #getTerabyte()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createTerabyte(UErrorCode &status);

    /**
     * Returns by value, unit of digital: terabyte.
     * Also see {@link #createTerabyte()}.
     * @draft ICU 64
     */
    static MeasureUnit getTerabyte();

    /**
     * Returns by pointer, unit of duration: century.
     * Caller owns returned value and must free it.
     * Also see {@link #getCentury()}.
     * @param status ICU error code.
     * @stable ICU 56
     */
    static MeasureUnit *createCentury(UErrorCode &status);

    /**
     * Returns by value, unit of duration: century.
     * Also see {@link #createCentury()}.
     * @draft ICU 64
     */
    static MeasureUnit getCentury();

    /**
     * Returns by pointer, unit of duration: day.
     * Caller owns returned value and must free it.
     * Also see {@link #getDay()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createDay(UErrorCode &status);

    /**
     * Returns by value, unit of duration: day.
     * Also see {@link #createDay()}.
     * @draft ICU 64
     */
    static MeasureUnit getDay();

    /**
     * Returns by pointer, unit of duration: hour.
     * Caller owns returned value and must free it.
     * Also see {@link #getHour()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createHour(UErrorCode &status);

    /**
     * Returns by value, unit of duration: hour.
     * Also see {@link #createHour()}.
     * @draft ICU 64
     */
    static MeasureUnit getHour();

    /**
     * Returns by pointer, unit of duration: microsecond.
     * Caller owns returned value and must free it.
     * Also see {@link #getMicrosecond()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createMicrosecond(UErrorCode &status);

    /**
     * Returns by value, unit of duration: microsecond.
     * Also see {@link #createMicrosecond()}.
     * @draft ICU 64
     */
    static MeasureUnit getMicrosecond();

    /**
     * Returns by pointer, unit of duration: millisecond.
     * Caller owns returned value and must free it.
     * Also see {@link #getMillisecond()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createMillisecond(UErrorCode &status);

    /**
     * Returns by value, unit of duration: millisecond.
     * Also see {@link #createMillisecond()}.
     * @draft ICU 64
     */
    static MeasureUnit getMillisecond();

    /**
     * Returns by pointer, unit of duration: minute.
     * Caller owns returned value and must free it.
     * Also see {@link #getMinute()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createMinute(UErrorCode &status);

    /**
     * Returns by value, unit of duration: minute.
     * Also see {@link #createMinute()}.
     * @draft ICU 64
     */
    static MeasureUnit getMinute();

    /**
     * Returns by pointer, unit of duration: month.
     * Caller owns returned value and must free it.
     * Also see {@link #getMonth()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createMonth(UErrorCode &status);

    /**
     * Returns by value, unit of duration: month.
     * Also see {@link #createMonth()}.
     * @draft ICU 64
     */
    static MeasureUnit getMonth();

    /**
     * Returns by pointer, unit of duration: nanosecond.
     * Caller owns returned value and must free it.
     * Also see {@link #getNanosecond()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createNanosecond(UErrorCode &status);

    /**
     * Returns by value, unit of duration: nanosecond.
     * Also see {@link #createNanosecond()}.
     * @draft ICU 64
     */
    static MeasureUnit getNanosecond();

    /**
     * Returns by pointer, unit of duration: second.
     * Caller owns returned value and must free it.
     * Also see {@link #getSecond()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createSecond(UErrorCode &status);

    /**
     * Returns by value, unit of duration: second.
     * Also see {@link #createSecond()}.
     * @draft ICU 64
     */
    static MeasureUnit getSecond();

    /**
     * Returns by pointer, unit of duration: week.
     * Caller owns returned value and must free it.
     * Also see {@link #getWeek()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createWeek(UErrorCode &status);

    /**
     * Returns by value, unit of duration: week.
     * Also see {@link #createWeek()}.
     * @draft ICU 64
     */
    static MeasureUnit getWeek();

    /**
     * Returns by pointer, unit of duration: year.
     * Caller owns returned value and must free it.
     * Also see {@link #getYear()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createYear(UErrorCode &status);

    /**
     * Returns by value, unit of duration: year.
     * Also see {@link #createYear()}.
     * @draft ICU 64
     */
    static MeasureUnit getYear();

    /**
     * Returns by pointer, unit of electric: ampere.
     * Caller owns returned value and must free it.
     * Also see {@link #getAmpere()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createAmpere(UErrorCode &status);

    /**
     * Returns by value, unit of electric: ampere.
     * Also see {@link #createAmpere()}.
     * @draft ICU 64
     */
    static MeasureUnit getAmpere();

    /**
     * Returns by pointer, unit of electric: milliampere.
     * Caller owns returned value and must free it.
     * Also see {@link #getMilliampere()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createMilliampere(UErrorCode &status);

    /**
     * Returns by value, unit of electric: milliampere.
     * Also see {@link #createMilliampere()}.
     * @draft ICU 64
     */
    static MeasureUnit getMilliampere();

    /**
     * Returns by pointer, unit of electric: ohm.
     * Caller owns returned value and must free it.
     * Also see {@link #getOhm()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createOhm(UErrorCode &status);

    /**
     * Returns by value, unit of electric: ohm.
     * Also see {@link #createOhm()}.
     * @draft ICU 64
     */
    static MeasureUnit getOhm();

    /**
     * Returns by pointer, unit of electric: volt.
     * Caller owns returned value and must free it.
     * Also see {@link #getVolt()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createVolt(UErrorCode &status);

    /**
     * Returns by value, unit of electric: volt.
     * Also see {@link #createVolt()}.
     * @draft ICU 64
     */
    static MeasureUnit getVolt();

    /**
     * Returns by pointer, unit of energy: calorie.
     * Caller owns returned value and must free it.
     * Also see {@link #getCalorie()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createCalorie(UErrorCode &status);

    /**
     * Returns by value, unit of energy: calorie.
     * Also see {@link #createCalorie()}.
     * @draft ICU 64
     */
    static MeasureUnit getCalorie();

    /**
     * Returns by pointer, unit of energy: foodcalorie.
     * Caller owns returned value and must free it.
     * Also see {@link #getFoodcalorie()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createFoodcalorie(UErrorCode &status);

    /**
     * Returns by value, unit of energy: foodcalorie.
     * Also see {@link #createFoodcalorie()}.
     * @draft ICU 64
     */
    static MeasureUnit getFoodcalorie();

    /**
     * Returns by pointer, unit of energy: joule.
     * Caller owns returned value and must free it.
     * Also see {@link #getJoule()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createJoule(UErrorCode &status);

    /**
     * Returns by value, unit of energy: joule.
     * Also see {@link #createJoule()}.
     * @draft ICU 64
     */
    static MeasureUnit getJoule();

    /**
     * Returns by pointer, unit of energy: kilocalorie.
     * Caller owns returned value and must free it.
     * Also see {@link #getKilocalorie()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createKilocalorie(UErrorCode &status);

    /**
     * Returns by value, unit of energy: kilocalorie.
     * Also see {@link #createKilocalorie()}.
     * @draft ICU 64
     */
    static MeasureUnit getKilocalorie();

    /**
     * Returns by pointer, unit of energy: kilojoule.
     * Caller owns returned value and must free it.
     * Also see {@link #getKilojoule()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createKilojoule(UErrorCode &status);

    /**
     * Returns by value, unit of energy: kilojoule.
     * Also see {@link #createKilojoule()}.
     * @draft ICU 64
     */
    static MeasureUnit getKilojoule();

    /**
     * Returns by pointer, unit of energy: kilowatt-hour.
     * Caller owns returned value and must free it.
     * Also see {@link #getKilowattHour()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createKilowattHour(UErrorCode &status);

    /**
     * Returns by value, unit of energy: kilowatt-hour.
     * Also see {@link #createKilowattHour()}.
     * @draft ICU 64
     */
    static MeasureUnit getKilowattHour();

    /**
     * Returns by pointer, unit of frequency: gigahertz.
     * Caller owns returned value and must free it.
     * Also see {@link #getGigahertz()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createGigahertz(UErrorCode &status);

    /**
     * Returns by value, unit of frequency: gigahertz.
     * Also see {@link #createGigahertz()}.
     * @draft ICU 64
     */
    static MeasureUnit getGigahertz();

    /**
     * Returns by pointer, unit of frequency: hertz.
     * Caller owns returned value and must free it.
     * Also see {@link #getHertz()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createHertz(UErrorCode &status);

    /**
     * Returns by value, unit of frequency: hertz.
     * Also see {@link #createHertz()}.
     * @draft ICU 64
     */
    static MeasureUnit getHertz();

    /**
     * Returns by pointer, unit of frequency: kilohertz.
     * Caller owns returned value and must free it.
     * Also see {@link #getKilohertz()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createKilohertz(UErrorCode &status);

    /**
     * Returns by value, unit of frequency: kilohertz.
     * Also see {@link #createKilohertz()}.
     * @draft ICU 64
     */
    static MeasureUnit getKilohertz();

    /**
     * Returns by pointer, unit of frequency: megahertz.
     * Caller owns returned value and must free it.
     * Also see {@link #getMegahertz()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createMegahertz(UErrorCode &status);

    /**
     * Returns by value, unit of frequency: megahertz.
     * Also see {@link #createMegahertz()}.
     * @draft ICU 64
     */
    static MeasureUnit getMegahertz();

    /**
     * Returns by pointer, unit of length: astronomical-unit.
     * Caller owns returned value and must free it.
     * Also see {@link #getAstronomicalUnit()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createAstronomicalUnit(UErrorCode &status);

    /**
     * Returns by value, unit of length: astronomical-unit.
     * Also see {@link #createAstronomicalUnit()}.
     * @draft ICU 64
     */
    static MeasureUnit getAstronomicalUnit();

    /**
     * Returns by pointer, unit of length: centimeter.
     * Caller owns returned value and must free it.
     * Also see {@link #getCentimeter()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createCentimeter(UErrorCode &status);

    /**
     * Returns by value, unit of length: centimeter.
     * Also see {@link #createCentimeter()}.
     * @draft ICU 64
     */
    static MeasureUnit getCentimeter();

    /**
     * Returns by pointer, unit of length: decimeter.
     * Caller owns returned value and must free it.
     * Also see {@link #getDecimeter()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createDecimeter(UErrorCode &status);

    /**
     * Returns by value, unit of length: decimeter.
     * Also see {@link #createDecimeter()}.
     * @draft ICU 64
     */
    static MeasureUnit getDecimeter();

    /**
     * Returns by pointer, unit of length: fathom.
     * Caller owns returned value and must free it.
     * Also see {@link #getFathom()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createFathom(UErrorCode &status);

    /**
     * Returns by value, unit of length: fathom.
     * Also see {@link #createFathom()}.
     * @draft ICU 64
     */
    static MeasureUnit getFathom();

    /**
     * Returns by pointer, unit of length: foot.
     * Caller owns returned value and must free it.
     * Also see {@link #getFoot()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createFoot(UErrorCode &status);

    /**
     * Returns by value, unit of length: foot.
     * Also see {@link #createFoot()}.
     * @draft ICU 64
     */
    static MeasureUnit getFoot();

    /**
     * Returns by pointer, unit of length: furlong.
     * Caller owns returned value and must free it.
     * Also see {@link #getFurlong()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createFurlong(UErrorCode &status);

    /**
     * Returns by value, unit of length: furlong.
     * Also see {@link #createFurlong()}.
     * @draft ICU 64
     */
    static MeasureUnit getFurlong();

    /**
     * Returns by pointer, unit of length: inch.
     * Caller owns returned value and must free it.
     * Also see {@link #getInch()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createInch(UErrorCode &status);

    /**
     * Returns by value, unit of length: inch.
     * Also see {@link #createInch()}.
     * @draft ICU 64
     */
    static MeasureUnit getInch();

    /**
     * Returns by pointer, unit of length: kilometer.
     * Caller owns returned value and must free it.
     * Also see {@link #getKilometer()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createKilometer(UErrorCode &status);

    /**
     * Returns by value, unit of length: kilometer.
     * Also see {@link #createKilometer()}.
     * @draft ICU 64
     */
    static MeasureUnit getKilometer();

    /**
     * Returns by pointer, unit of length: light-year.
     * Caller owns returned value and must free it.
     * Also see {@link #getLightYear()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createLightYear(UErrorCode &status);

    /**
     * Returns by value, unit of length: light-year.
     * Also see {@link #createLightYear()}.
     * @draft ICU 64
     */
    static MeasureUnit getLightYear();

    /**
     * Returns by pointer, unit of length: meter.
     * Caller owns returned value and must free it.
     * Also see {@link #getMeter()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createMeter(UErrorCode &status);

    /**
     * Returns by value, unit of length: meter.
     * Also see {@link #createMeter()}.
     * @draft ICU 64
     */
    static MeasureUnit getMeter();

    /**
     * Returns by pointer, unit of length: micrometer.
     * Caller owns returned value and must free it.
     * Also see {@link #getMicrometer()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createMicrometer(UErrorCode &status);

    /**
     * Returns by value, unit of length: micrometer.
     * Also see {@link #createMicrometer()}.
     * @draft ICU 64
     */
    static MeasureUnit getMicrometer();

    /**
     * Returns by pointer, unit of length: mile.
     * Caller owns returned value and must free it.
     * Also see {@link #getMile()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createMile(UErrorCode &status);

    /**
     * Returns by value, unit of length: mile.
     * Also see {@link #createMile()}.
     * @draft ICU 64
     */
    static MeasureUnit getMile();

    /**
     * Returns by pointer, unit of length: mile-scandinavian.
     * Caller owns returned value and must free it.
     * Also see {@link #getMileScandinavian()}.
     * @param status ICU error code.
     * @stable ICU 56
     */
    static MeasureUnit *createMileScandinavian(UErrorCode &status);

    /**
     * Returns by value, unit of length: mile-scandinavian.
     * Also see {@link #createMileScandinavian()}.
     * @draft ICU 64
     */
    static MeasureUnit getMileScandinavian();

    /**
     * Returns by pointer, unit of length: millimeter.
     * Caller owns returned value and must free it.
     * Also see {@link #getMillimeter()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createMillimeter(UErrorCode &status);

    /**
     * Returns by value, unit of length: millimeter.
     * Also see {@link #createMillimeter()}.
     * @draft ICU 64
     */
    static MeasureUnit getMillimeter();

    /**
     * Returns by pointer, unit of length: nanometer.
     * Caller owns returned value and must free it.
     * Also see {@link #getNanometer()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createNanometer(UErrorCode &status);

    /**
     * Returns by value, unit of length: nanometer.
     * Also see {@link #createNanometer()}.
     * @draft ICU 64
     */
    static MeasureUnit getNanometer();

    /**
     * Returns by pointer, unit of length: nautical-mile.
     * Caller owns returned value and must free it.
     * Also see {@link #getNauticalMile()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createNauticalMile(UErrorCode &status);

    /**
     * Returns by value, unit of length: nautical-mile.
     * Also see {@link #createNauticalMile()}.
     * @draft ICU 64
     */
    static MeasureUnit getNauticalMile();

    /**
     * Returns by pointer, unit of length: parsec.
     * Caller owns returned value and must free it.
     * Also see {@link #getParsec()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createParsec(UErrorCode &status);

    /**
     * Returns by value, unit of length: parsec.
     * Also see {@link #createParsec()}.
     * @draft ICU 64
     */
    static MeasureUnit getParsec();

    /**
     * Returns by pointer, unit of length: picometer.
     * Caller owns returned value and must free it.
     * Also see {@link #getPicometer()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createPicometer(UErrorCode &status);

    /**
     * Returns by value, unit of length: picometer.
     * Also see {@link #createPicometer()}.
     * @draft ICU 64
     */
    static MeasureUnit getPicometer();

    /**
     * Returns by pointer, unit of length: point.
     * Caller owns returned value and must free it.
     * Also see {@link #getPoint()}.
     * @param status ICU error code.
     * @stable ICU 59
     */
    static MeasureUnit *createPoint(UErrorCode &status);

    /**
     * Returns by value, unit of length: point.
     * Also see {@link #createPoint()}.
     * @draft ICU 64
     */
    static MeasureUnit getPoint();

    /**
     * Returns by pointer, unit of length: yard.
     * Caller owns returned value and must free it.
     * Also see {@link #getYard()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createYard(UErrorCode &status);

    /**
     * Returns by value, unit of length: yard.
     * Also see {@link #createYard()}.
     * @draft ICU 64
     */
    static MeasureUnit getYard();

    /**
     * Returns by pointer, unit of light: lux.
     * Caller owns returned value and must free it.
     * Also see {@link #getLux()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createLux(UErrorCode &status);

    /**
     * Returns by value, unit of light: lux.
     * Also see {@link #createLux()}.
     * @draft ICU 64
     */
    static MeasureUnit getLux();

    /**
     * Returns by pointer, unit of mass: carat.
     * Caller owns returned value and must free it.
     * Also see {@link #getCarat()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createCarat(UErrorCode &status);

    /**
     * Returns by value, unit of mass: carat.
     * Also see {@link #createCarat()}.
     * @draft ICU 64
     */
    static MeasureUnit getCarat();

    /**
     * Returns by pointer, unit of mass: gram.
     * Caller owns returned value and must free it.
     * Also see {@link #getGram()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createGram(UErrorCode &status);

    /**
     * Returns by value, unit of mass: gram.
     * Also see {@link #createGram()}.
     * @draft ICU 64
     */
    static MeasureUnit getGram();

    /**
     * Returns by pointer, unit of mass: kilogram.
     * Caller owns returned value and must free it.
     * Also see {@link #getKilogram()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createKilogram(UErrorCode &status);

    /**
     * Returns by value, unit of mass: kilogram.
     * Also see {@link #createKilogram()}.
     * @draft ICU 64
     */
    static MeasureUnit getKilogram();

    /**
     * Returns by pointer, unit of mass: metric-ton.
     * Caller owns returned value and must free it.
     * Also see {@link #getMetricTon()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createMetricTon(UErrorCode &status);

    /**
     * Returns by value, unit of mass: metric-ton.
     * Also see {@link #createMetricTon()}.
     * @draft ICU 64
     */
    static MeasureUnit getMetricTon();

    /**
     * Returns by pointer, unit of mass: microgram.
     * Caller owns returned value and must free it.
     * Also see {@link #getMicrogram()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createMicrogram(UErrorCode &status);

    /**
     * Returns by value, unit of mass: microgram.
     * Also see {@link #createMicrogram()}.
     * @draft ICU 64
     */
    static MeasureUnit getMicrogram();

    /**
     * Returns by pointer, unit of mass: milligram.
     * Caller owns returned value and must free it.
     * Also see {@link #getMilligram()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createMilligram(UErrorCode &status);

    /**
     * Returns by value, unit of mass: milligram.
     * Also see {@link #createMilligram()}.
     * @draft ICU 64
     */
    static MeasureUnit getMilligram();

    /**
     * Returns by pointer, unit of mass: ounce.
     * Caller owns returned value and must free it.
     * Also see {@link #getOunce()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createOunce(UErrorCode &status);

    /**
     * Returns by value, unit of mass: ounce.
     * Also see {@link #createOunce()}.
     * @draft ICU 64
     */
    static MeasureUnit getOunce();

    /**
     * Returns by pointer, unit of mass: ounce-troy.
     * Caller owns returned value and must free it.
     * Also see {@link #getOunceTroy()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createOunceTroy(UErrorCode &status);

    /**
     * Returns by value, unit of mass: ounce-troy.
     * Also see {@link #createOunceTroy()}.
     * @draft ICU 64
     */
    static MeasureUnit getOunceTroy();

    /**
     * Returns by pointer, unit of mass: pound.
     * Caller owns returned value and must free it.
     * Also see {@link #getPound()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createPound(UErrorCode &status);

    /**
     * Returns by value, unit of mass: pound.
     * Also see {@link #createPound()}.
     * @draft ICU 64
     */
    static MeasureUnit getPound();

    /**
     * Returns by pointer, unit of mass: stone.
     * Caller owns returned value and must free it.
     * Also see {@link #getStone()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createStone(UErrorCode &status);

    /**
     * Returns by value, unit of mass: stone.
     * Also see {@link #createStone()}.
     * @draft ICU 64
     */
    static MeasureUnit getStone();

    /**
     * Returns by pointer, unit of mass: ton.
     * Caller owns returned value and must free it.
     * Also see {@link #getTon()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createTon(UErrorCode &status);

    /**
     * Returns by value, unit of mass: ton.
     * Also see {@link #createTon()}.
     * @draft ICU 64
     */
    static MeasureUnit getTon();

    /**
     * Returns by pointer, unit of power: gigawatt.
     * Caller owns returned value and must free it.
     * Also see {@link #getGigawatt()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createGigawatt(UErrorCode &status);

    /**
     * Returns by value, unit of power: gigawatt.
     * Also see {@link #createGigawatt()}.
     * @draft ICU 64
     */
    static MeasureUnit getGigawatt();

    /**
     * Returns by pointer, unit of power: horsepower.
     * Caller owns returned value and must free it.
     * Also see {@link #getHorsepower()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createHorsepower(UErrorCode &status);

    /**
     * Returns by value, unit of power: horsepower.
     * Also see {@link #createHorsepower()}.
     * @draft ICU 64
     */
    static MeasureUnit getHorsepower();

    /**
     * Returns by pointer, unit of power: kilowatt.
     * Caller owns returned value and must free it.
     * Also see {@link #getKilowatt()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createKilowatt(UErrorCode &status);

    /**
     * Returns by value, unit of power: kilowatt.
     * Also see {@link #createKilowatt()}.
     * @draft ICU 64
     */
    static MeasureUnit getKilowatt();

    /**
     * Returns by pointer, unit of power: megawatt.
     * Caller owns returned value and must free it.
     * Also see {@link #getMegawatt()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createMegawatt(UErrorCode &status);

    /**
     * Returns by value, unit of power: megawatt.
     * Also see {@link #createMegawatt()}.
     * @draft ICU 64
     */
    static MeasureUnit getMegawatt();

    /**
     * Returns by pointer, unit of power: milliwatt.
     * Caller owns returned value and must free it.
     * Also see {@link #getMilliwatt()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createMilliwatt(UErrorCode &status);

    /**
     * Returns by value, unit of power: milliwatt.
     * Also see {@link #createMilliwatt()}.
     * @draft ICU 64
     */
    static MeasureUnit getMilliwatt();

    /**
     * Returns by pointer, unit of power: watt.
     * Caller owns returned value and must free it.
     * Also see {@link #getWatt()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createWatt(UErrorCode &status);

    /**
     * Returns by value, unit of power: watt.
     * Also see {@link #createWatt()}.
     * @draft ICU 64
     */
    static MeasureUnit getWatt();

#ifndef U_HIDE_DRAFT_API
    /**
     * Returns by pointer, unit of pressure: atmosphere.
     * Caller owns returned value and must free it.
     * Also see {@link #getAtmosphere()}.
     * @param status ICU error code.
     * @draft ICU 63
     */
    static MeasureUnit *createAtmosphere(UErrorCode &status);

    /**
     * Returns by value, unit of pressure: atmosphere.
     * Also see {@link #createAtmosphere()}.
     * @draft ICU 64
     */
    static MeasureUnit getAtmosphere();
#endif /* U_HIDE_DRAFT_API */

    /**
     * Returns by pointer, unit of pressure: hectopascal.
     * Caller owns returned value and must free it.
     * Also see {@link #getHectopascal()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createHectopascal(UErrorCode &status);

    /**
     * Returns by value, unit of pressure: hectopascal.
     * Also see {@link #createHectopascal()}.
     * @draft ICU 64
     */
    static MeasureUnit getHectopascal();

    /**
     * Returns by pointer, unit of pressure: inch-hg.
     * Caller owns returned value and must free it.
     * Also see {@link #getInchHg()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createInchHg(UErrorCode &status);

    /**
     * Returns by value, unit of pressure: inch-hg.
     * Also see {@link #createInchHg()}.
     * @draft ICU 64
     */
    static MeasureUnit getInchHg();

    /**
     * Returns by pointer, unit of pressure: millibar.
     * Caller owns returned value and must free it.
     * Also see {@link #getMillibar()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createMillibar(UErrorCode &status);

    /**
     * Returns by value, unit of pressure: millibar.
     * Also see {@link #createMillibar()}.
     * @draft ICU 64
     */
    static MeasureUnit getMillibar();

    /**
     * Returns by pointer, unit of pressure: millimeter-of-mercury.
     * Caller owns returned value and must free it.
     * Also see {@link #getMillimeterOfMercury()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createMillimeterOfMercury(UErrorCode &status);

    /**
     * Returns by value, unit of pressure: millimeter-of-mercury.
     * Also see {@link #createMillimeterOfMercury()}.
     * @draft ICU 64
     */
    static MeasureUnit getMillimeterOfMercury();

    /**
     * Returns by pointer, unit of pressure: pound-per-square-inch.
     * Caller owns returned value and must free it.
     * Also see {@link #getPoundPerSquareInch()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createPoundPerSquareInch(UErrorCode &status);

    /**
     * Returns by value, unit of pressure: pound-per-square-inch.
     * Also see {@link #createPoundPerSquareInch()}.
     * @draft ICU 64
     */
    static MeasureUnit getPoundPerSquareInch();

    /**
     * Returns by pointer, unit of speed: kilometer-per-hour.
     * Caller owns returned value and must free it.
     * Also see {@link #getKilometerPerHour()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createKilometerPerHour(UErrorCode &status);

    /**
     * Returns by value, unit of speed: kilometer-per-hour.
     * Also see {@link #createKilometerPerHour()}.
     * @draft ICU 64
     */
    static MeasureUnit getKilometerPerHour();

    /**
     * Returns by pointer, unit of speed: knot.
     * Caller owns returned value and must free it.
     * Also see {@link #getKnot()}.
     * @param status ICU error code.
     * @stable ICU 56
     */
    static MeasureUnit *createKnot(UErrorCode &status);

    /**
     * Returns by value, unit of speed: knot.
     * Also see {@link #createKnot()}.
     * @draft ICU 64
     */
    static MeasureUnit getKnot();

    /**
     * Returns by pointer, unit of speed: meter-per-second.
     * Caller owns returned value and must free it.
     * Also see {@link #getMeterPerSecond()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createMeterPerSecond(UErrorCode &status);

    /**
     * Returns by value, unit of speed: meter-per-second.
     * Also see {@link #createMeterPerSecond()}.
     * @draft ICU 64
     */
    static MeasureUnit getMeterPerSecond();

    /**
     * Returns by pointer, unit of speed: mile-per-hour.
     * Caller owns returned value and must free it.
     * Also see {@link #getMilePerHour()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createMilePerHour(UErrorCode &status);

    /**
     * Returns by value, unit of speed: mile-per-hour.
     * Also see {@link #createMilePerHour()}.
     * @draft ICU 64
     */
    static MeasureUnit getMilePerHour();

    /**
     * Returns by pointer, unit of temperature: celsius.
     * Caller owns returned value and must free it.
     * Also see {@link #getCelsius()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createCelsius(UErrorCode &status);

    /**
     * Returns by value, unit of temperature: celsius.
     * Also see {@link #createCelsius()}.
     * @draft ICU 64
     */
    static MeasureUnit getCelsius();

    /**
     * Returns by pointer, unit of temperature: fahrenheit.
     * Caller owns returned value and must free it.
     * Also see {@link #getFahrenheit()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createFahrenheit(UErrorCode &status);

    /**
     * Returns by value, unit of temperature: fahrenheit.
     * Also see {@link #createFahrenheit()}.
     * @draft ICU 64
     */
    static MeasureUnit getFahrenheit();

    /**
     * Returns by pointer, unit of temperature: generic.
     * Caller owns returned value and must free it.
     * Also see {@link #getGenericTemperature()}.
     * @param status ICU error code.
     * @stable ICU 56
     */
    static MeasureUnit *createGenericTemperature(UErrorCode &status);

    /**
     * Returns by value, unit of temperature: generic.
     * Also see {@link #createGenericTemperature()}.
     * @draft ICU 64
     */
    static MeasureUnit getGenericTemperature();

    /**
     * Returns by pointer, unit of temperature: kelvin.
     * Caller owns returned value and must free it.
     * Also see {@link #getKelvin()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createKelvin(UErrorCode &status);

    /**
     * Returns by value, unit of temperature: kelvin.
     * Also see {@link #createKelvin()}.
     * @draft ICU 64
     */
    static MeasureUnit getKelvin();

    /**
     * Returns by pointer, unit of volume: acre-foot.
     * Caller owns returned value and must free it.
     * Also see {@link #getAcreFoot()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createAcreFoot(UErrorCode &status);

    /**
     * Returns by value, unit of volume: acre-foot.
     * Also see {@link #createAcreFoot()}.
     * @draft ICU 64
     */
    static MeasureUnit getAcreFoot();

    /**
     * Returns by pointer, unit of volume: bushel.
     * Caller owns returned value and must free it.
     * Also see {@link #getBushel()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createBushel(UErrorCode &status);

    /**
     * Returns by value, unit of volume: bushel.
     * Also see {@link #createBushel()}.
     * @draft ICU 64
     */
    static MeasureUnit getBushel();

    /**
     * Returns by pointer, unit of volume: centiliter.
     * Caller owns returned value and must free it.
     * Also see {@link #getCentiliter()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createCentiliter(UErrorCode &status);

    /**
     * Returns by value, unit of volume: centiliter.
     * Also see {@link #createCentiliter()}.
     * @draft ICU 64
     */
    static MeasureUnit getCentiliter();

    /**
     * Returns by pointer, unit of volume: cubic-centimeter.
     * Caller owns returned value and must free it.
     * Also see {@link #getCubicCentimeter()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createCubicCentimeter(UErrorCode &status);

    /**
     * Returns by value, unit of volume: cubic-centimeter.
     * Also see {@link #createCubicCentimeter()}.
     * @draft ICU 64
     */
    static MeasureUnit getCubicCentimeter();

    /**
     * Returns by pointer, unit of volume: cubic-foot.
     * Caller owns returned value and must free it.
     * Also see {@link #getCubicFoot()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createCubicFoot(UErrorCode &status);

    /**
     * Returns by value, unit of volume: cubic-foot.
     * Also see {@link #createCubicFoot()}.
     * @draft ICU 64
     */
    static MeasureUnit getCubicFoot();

    /**
     * Returns by pointer, unit of volume: cubic-inch.
     * Caller owns returned value and must free it.
     * Also see {@link #getCubicInch()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createCubicInch(UErrorCode &status);

    /**
     * Returns by value, unit of volume: cubic-inch.
     * Also see {@link #createCubicInch()}.
     * @draft ICU 64
     */
    static MeasureUnit getCubicInch();

    /**
     * Returns by pointer, unit of volume: cubic-kilometer.
     * Caller owns returned value and must free it.
     * Also see {@link #getCubicKilometer()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createCubicKilometer(UErrorCode &status);

    /**
     * Returns by value, unit of volume: cubic-kilometer.
     * Also see {@link #createCubicKilometer()}.
     * @draft ICU 64
     */
    static MeasureUnit getCubicKilometer();

    /**
     * Returns by pointer, unit of volume: cubic-meter.
     * Caller owns returned value and must free it.
     * Also see {@link #getCubicMeter()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createCubicMeter(UErrorCode &status);

    /**
     * Returns by value, unit of volume: cubic-meter.
     * Also see {@link #createCubicMeter()}.
     * @draft ICU 64
     */
    static MeasureUnit getCubicMeter();

    /**
     * Returns by pointer, unit of volume: cubic-mile.
     * Caller owns returned value and must free it.
     * Also see {@link #getCubicMile()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createCubicMile(UErrorCode &status);

    /**
     * Returns by value, unit of volume: cubic-mile.
     * Also see {@link #createCubicMile()}.
     * @draft ICU 64
     */
    static MeasureUnit getCubicMile();

    /**
     * Returns by pointer, unit of volume: cubic-yard.
     * Caller owns returned value and must free it.
     * Also see {@link #getCubicYard()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createCubicYard(UErrorCode &status);

    /**
     * Returns by value, unit of volume: cubic-yard.
     * Also see {@link #createCubicYard()}.
     * @draft ICU 64
     */
    static MeasureUnit getCubicYard();

    /**
     * Returns by pointer, unit of volume: cup.
     * Caller owns returned value and must free it.
     * Also see {@link #getCup()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createCup(UErrorCode &status);

    /**
     * Returns by value, unit of volume: cup.
     * Also see {@link #createCup()}.
     * @draft ICU 64
     */
    static MeasureUnit getCup();

    /**
     * Returns by pointer, unit of volume: cup-metric.
     * Caller owns returned value and must free it.
     * Also see {@link #getCupMetric()}.
     * @param status ICU error code.
     * @stable ICU 56
     */
    static MeasureUnit *createCupMetric(UErrorCode &status);

    /**
     * Returns by value, unit of volume: cup-metric.
     * Also see {@link #createCupMetric()}.
     * @draft ICU 64
     */
    static MeasureUnit getCupMetric();

    /**
     * Returns by pointer, unit of volume: deciliter.
     * Caller owns returned value and must free it.
     * Also see {@link #getDeciliter()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createDeciliter(UErrorCode &status);

    /**
     * Returns by value, unit of volume: deciliter.
     * Also see {@link #createDeciliter()}.
     * @draft ICU 64
     */
    static MeasureUnit getDeciliter();

    /**
     * Returns by pointer, unit of volume: fluid-ounce.
     * Caller owns returned value and must free it.
     * Also see {@link #getFluidOunce()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createFluidOunce(UErrorCode &status);

    /**
     * Returns by value, unit of volume: fluid-ounce.
     * Also see {@link #createFluidOunce()}.
     * @draft ICU 64
     */
    static MeasureUnit getFluidOunce();

    /**
     * Returns by pointer, unit of volume: gallon.
     * Caller owns returned value and must free it.
     * Also see {@link #getGallon()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createGallon(UErrorCode &status);

    /**
     * Returns by value, unit of volume: gallon.
     * Also see {@link #createGallon()}.
     * @draft ICU 64
     */
    static MeasureUnit getGallon();

    /**
     * Returns by pointer, unit of volume: gallon-imperial.
     * Caller owns returned value and must free it.
     * Also see {@link #getGallonImperial()}.
     * @param status ICU error code.
     * @stable ICU 57
     */
    static MeasureUnit *createGallonImperial(UErrorCode &status);

    /**
     * Returns by value, unit of volume: gallon-imperial.
     * Also see {@link #createGallonImperial()}.
     * @draft ICU 64
     */
    static MeasureUnit getGallonImperial();

    /**
     * Returns by pointer, unit of volume: hectoliter.
     * Caller owns returned value and must free it.
     * Also see {@link #getHectoliter()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createHectoliter(UErrorCode &status);

    /**
     * Returns by value, unit of volume: hectoliter.
     * Also see {@link #createHectoliter()}.
     * @draft ICU 64
     */
    static MeasureUnit getHectoliter();

    /**
     * Returns by pointer, unit of volume: liter.
     * Caller owns returned value and must free it.
     * Also see {@link #getLiter()}.
     * @param status ICU error code.
     * @stable ICU 53
     */
    static MeasureUnit *createLiter(UErrorCode &status);

    /**
     * Returns by value, unit of volume: liter.
     * Also see {@link #createLiter()}.
     * @draft ICU 64
     */
    static MeasureUnit getLiter();

    /**
     * Returns by pointer, unit of volume: megaliter.
     * Caller owns returned value and must free it.
     * Also see {@link #getMegaliter()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createMegaliter(UErrorCode &status);

    /**
     * Returns by value, unit of volume: megaliter.
     * Also see {@link #createMegaliter()}.
     * @draft ICU 64
     */
    static MeasureUnit getMegaliter();

    /**
     * Returns by pointer, unit of volume: milliliter.
     * Caller owns returned value and must free it.
     * Also see {@link #getMilliliter()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createMilliliter(UErrorCode &status);

    /**
     * Returns by value, unit of volume: milliliter.
     * Also see {@link #createMilliliter()}.
     * @draft ICU 64
     */
    static MeasureUnit getMilliliter();

    /**
     * Returns by pointer, unit of volume: pint.
     * Caller owns returned value and must free it.
     * Also see {@link #getPint()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createPint(UErrorCode &status);

    /**
     * Returns by value, unit of volume: pint.
     * Also see {@link #createPint()}.
     * @draft ICU 64
     */
    static MeasureUnit getPint();

    /**
     * Returns by pointer, unit of volume: pint-metric.
     * Caller owns returned value and must free it.
     * Also see {@link #getPintMetric()}.
     * @param status ICU error code.
     * @stable ICU 56
     */
    static MeasureUnit *createPintMetric(UErrorCode &status);

    /**
     * Returns by value, unit of volume: pint-metric.
     * Also see {@link #createPintMetric()}.
     * @draft ICU 64
     */
    static MeasureUnit getPintMetric();

    /**
     * Returns by pointer, unit of volume: quart.
     * Caller owns returned value and must free it.
     * Also see {@link #getQuart()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createQuart(UErrorCode &status);

    /**
     * Returns by value, unit of volume: quart.
     * Also see {@link #createQuart()}.
     * @draft ICU 64
     */
    static MeasureUnit getQuart();

    /**
     * Returns by pointer, unit of volume: tablespoon.
     * Caller owns returned value and must free it.
     * Also see {@link #getTablespoon()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createTablespoon(UErrorCode &status);

    /**
     * Returns by value, unit of volume: tablespoon.
     * Also see {@link #createTablespoon()}.
     * @draft ICU 64
     */
    static MeasureUnit getTablespoon();

    /**
     * Returns by pointer, unit of volume: teaspoon.
     * Caller owns returned value and must free it.
     * Also see {@link #getTeaspoon()}.
     * @param status ICU error code.
     * @stable ICU 54
     */
    static MeasureUnit *createTeaspoon(UErrorCode &status);

    /**
     * Returns by value, unit of volume: teaspoon.
     * Also see {@link #createTeaspoon()}.
     * @draft ICU 64
     */
    static MeasureUnit getTeaspoon();


// End generated createXXX methods

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

    /**
     * For ICU use only.
     * @internal
     */
    void initNoUnit(const char *subtype);

#endif  /* U_HIDE_INTERNAL_API */

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
