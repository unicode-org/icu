// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef __MEASUNIT_IMPL_H__
#define __MEASUNIT_IMPL_H__

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/measunit.h"
#include "cmemory.h"
#include "charstr.h"

U_NAMESPACE_BEGIN


struct TempSingleUnit : public UMemory {
    /**
     * Gets a single unit from the MeasureUnit. If there are multiple single units, sets an error
     * code and return the base dimensionless unit. Parses if necessary.
     */
    static TempSingleUnit forMeasureUnit(const MeasureUnit& measureUnit, UErrorCode& status);

    /** Transform this TemplSingleUnit into a MeasureUnit, simplifying if possible. */
    MeasureUnit build(UErrorCode& status);

    /** Compare this TempSingleUnit to another TempSingleUnit. */
    int32_t compareTo(const TempSingleUnit& other) const {
        if (dimensionality < 0 && other.dimensionality > 0) {
            // Positive dimensions first
            return 1;
        } else if (dimensionality > 0 && other.dimensionality < 0) {
            return -1;
        } else if (index < other.index) {
            return -1;
        } else if (index > other.index) {
            return 1;
        } else if (siPrefix < other.siPrefix) {
            return -1;
        } else if (siPrefix > other.siPrefix) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * Return whether this TempSingleUnit is compatible with another for the purpose of coalescing.
     *
     * Units with the same base unit and SI prefix should match, except that they must also have
     * the same dimensionality sign, such that we don't merge numerator and denominator.
     */
    bool isCompatibleWith(const TempSingleUnit& other) const {
        return (compareTo(other) == 0);
    }

    /** Simple unit index, unique for every simple unit. */
    int32_t index = 0;

    /** Simple unit identifier; memory not owned by the SimpleUnit. */
    StringPiece identifier;

    /** SI prefix. **/
    UMeasureSIPrefix siPrefix = UMEASURE_SI_PREFIX_ONE;
    
    /** Dimensionality. **/
    int32_t dimensionality = 1;
};


/**
 * Internal representation of measurement units. Capable of representing all complexities of units,
 * including sequence and compound units.
 */
struct MeasureUnitImpl : public UMemory {
    /** Extract the MeasureUnitImpl from a MeasureUnit. */
    static inline const MeasureUnitImpl* get(const MeasureUnit& measureUnit) {
        return measureUnit.fImpl;
    }

    /**
     * Parse a unit identifier into a MeasureUnitImpl.
     *
     * @param identifier The unit identifier string.
     * @param status Set if the identifier string is not valid.
     * @return A newly parsed value object.
     */
    static MeasureUnitImpl forIdentifier(StringPiece identifier, UErrorCode& status);

    /**
     * Extract the MeasureUnitImpl from a MeasureUnit, or parse if it is not present.
     * 
     * @param measureUnit The source MeasureUnit.
     * @param memory A place to write the new MeasureUnitImpl if parsing is required.
     * @param status Set if an error occurs.
     * @return A reference to either measureUnit.fImpl or memory.
     */
    static const MeasureUnitImpl& forMeasureUnit(
        const MeasureUnit& measureUnit, MeasureUnitImpl& memory, UErrorCode& status);

    /**
     * Extract the MeasureUnitImpl from a MeasureUnit, or parse if it is not present.
     *
     * @param measureUnit The source MeasureUnit.
     * @param status Set if an error occurs.
     * @return A value object, either newly parsed or copied from measureUnit.
     */
    static MeasureUnitImpl forMeasureUnitMaybeCopy(
        const MeasureUnit& measureUnit, UErrorCode& status);

    /**
     * Used for currency units.
     */
    static inline MeasureUnitImpl forCurrencyCode(StringPiece currencyCode) {
        MeasureUnitImpl result;
        UErrorCode localStatus = U_ZERO_ERROR;
        result.identifier.append(currencyCode, localStatus);
        // localStatus is not expected to fail since currencyCode should be 3 chars long
        return result;
    }

    /** Transform this MeasureUnitImpl into a MeasureUnit, simplifying if possible. */
    MeasureUnit build(UErrorCode& status) &&;

    /**
     * Create a copy of this MeasureUnitImpl. Don't use copy constructor to make this explicit.
     */
    inline MeasureUnitImpl copy(UErrorCode& status) const {
        MeasureUnitImpl result;
        result.complexity = complexity;
        result.units.appendAll(units, status);
        result.identifier.append(identifier, status);
        return result;
    }

    /** Mutates this MeasureUnitImpl to take the reciprocal. */
    void takeReciprocal(UErrorCode& status);

    /** Mutates this MeasureUnitImpl to append a single unit. */
    bool append(const TempSingleUnit& singleUnit, UErrorCode& status);

    /** The complexity, either SINGLE, COMPOUND, or SEQUENCE. */
    UMeasureUnitComplexity complexity = UMEASURE_UNIT_SINGLE;

    /**
     * The list of simple units. These may be summed or multiplied, based on the value of the
     * complexity field.
     */
    MaybeStackVector<TempSingleUnit> units;

    /**
     * The full unit identifier.  Owned by the MeasureUnitImpl.  Empty if not computed.
     */
    CharString identifier;
};


U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */
#endif //__MEASUNIT_IMPL_H__
