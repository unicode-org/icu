package com.ibm.icu.impl.units;

public class SingleUnitImpl {
    public SingleUnitImpl() {
    }

    public SingleUnitImpl(SingleUnitImpl other) {
        this.dimensionality = other.dimensionality;
        this.simpleUnit = other.getSimpleUnit();
        this.siPrefix = other.siPrefix;
    }


    /**
     * Returns true if this unit is the "dimensionless base unit", as produced
     * by the MeasureUnit() default constructor.
     * <p>
     * NOTE:
     * (This does not include the likes of concentrations or angles.)
     */
    public boolean isDimensionless() {
        return simpleUnit.isEmpty();
    }

    public String getSimpleUnit() {
        return simpleUnit;
    }

    public void setSimpleUnit(String simpleUnit) {
        this.simpleUnit = simpleUnit;
    }

    /**
     * Checks whether this SingleUnitImpl is compatible with another for the purpose of coalescing.
     * <p>
     * Units with the same base unit and SI prefix should match, except that they must also have
     * the same dimensionality sign, such that we don't merge numerator and denominator.
     */
    boolean isCompatibleWith(SingleUnitImpl other) {
        if (this.simpleUnit == other.simpleUnit && this.siPrefix == other.siPrefix) {
            if (this.getDimensionality() > 0 && other.getDimensionality() > 0
                    || this.getDimensionality() < 0 && other.getDimensionality() < 0) {
                return true;
            }
        }

        return false;
    }

    public int getDimensionality() {
        return dimensionality;
    }

    public void setDimensionality(int dimensionality) {
        this.dimensionality = dimensionality;
    }

    public UMeasureSIPrefix getSiPrefix() {
        return siPrefix;
    }

    public void setSiPrefix(UMeasureSIPrefix siPrefix) {
        this.siPrefix = siPrefix;
    }


    /**
     * SimpleUnit is the simplest form of a Unit. For example, for "square-millimeter", the simple unit would be "meter"Ò
     * <p>
     * The default value is "", meaning the dimensionless unit:
     * isDimensionless() will return true, until index is changed.
     */
    private String simpleUnit = "";

    /**
     * Determine the power of the `SingleUnit`. For example, for "square-meter", the dimensionality will be `2`.
     * <p>
     * NOTE:
     * Default dimensionality is 1.
     */
    private int dimensionality = 1;

    /**
     * SI Prefix
     */
    private UMeasureSIPrefix siPrefix = UMeasureSIPrefix.UMEASURE_SI_PREFIX_ONE;

}
