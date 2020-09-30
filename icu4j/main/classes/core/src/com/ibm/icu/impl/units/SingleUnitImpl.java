// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.impl.units;

import com.ibm.icu.util.MeasureUnit;

public class SingleUnitImpl {
    /**
     * Simple unit index, unique for every simple unit, -1 for the dimensionless
     * unit. This is an index into a string list in unit.txt {ConversionUnits}.
     * <p>
     * The default value is -1, meaning the dimensionless unit:
     * isDimensionless() will return true, until index is changed.
     */
    private int index = -1;
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
    private MeasureUnit.SIPrefix siPrefix = MeasureUnit.SIPrefix.ONE;

    public SingleUnitImpl copy() {
        SingleUnitImpl result = new SingleUnitImpl();
        result.index = this.index;
        result.dimensionality = this.dimensionality;
        result.simpleUnit = this.simpleUnit;
        result.siPrefix = this.siPrefix;

        return result;
    }

    public MeasureUnit build() {
        MeasureUnitImpl measureUnit = new MeasureUnitImpl(this);
        return measureUnit.build();
    }

    /**
     * Generates an neutral identifier string for a single unit which means we do not include the dimension signal.
     */
    public String getNeutralIdentifier() {
        StringBuilder result = new StringBuilder();
        int posPower = Math.abs(this.getDimensionality());

        assert posPower > 0 : "getIdentifier does not support the dimensionless";

        if (posPower == 1) {
            // no-op
        } else if (posPower == 2) {
            result.append("square-");
        } else if (posPower == 3) {
            result.append("cubic-");
        } else if (posPower <= 15) {
            result.append("pow");
            result.append(posPower);
            result.append('-');
        } else {
            // TODO: IllegalArgumentException might not be appropriate here
            throw new IllegalArgumentException("Unit Identifier Syntax Error");
        }

        result.append(this.getSiPrefix().getIdentifier());
        result.append(this.getSimpleUnit());

        return result.toString();
    }

    /**
     * Compare this SingleUnitImpl to another SingleUnitImpl for the sake of
     * sorting and coalescing.
     * <p>
     * Takes the sign of dimensionality into account, but not the absolute
     * value: per-meter is not considered the same as meter, but meter is
     * considered the same as square-meter.
     * <p>
     * The dimensionless unit generally does not get compared, but if it did, it
     * would sort before other units by virtue of index being < 0 and
     * dimensionality not being negative.
     */
    int compareTo(SingleUnitImpl other) {
        if (dimensionality < 0 && other.dimensionality > 0) {
            // Positive dimensions first
            return 1;
        }
        if (dimensionality > 0 && other.dimensionality < 0) {
            return -1;
        }
        if (index < other.index) {
            return -1;
        }
        if (index > other.index) {
            return 1;
        }
        if (this.getSiPrefix().getPower() < other.getSiPrefix().getPower()) {
            return -1;
        }
        if (this.getSiPrefix().getPower() > other.getSiPrefix().getPower()) {
            return 1;
        }
        return 0;
    }

    /**
     * Checks whether this SingleUnitImpl is compatible with another for the purpose of coalescing.
     * <p>
     * Units with the same base unit and SI prefix should match, except that they must also have
     * the same dimensionality sign, such that we don't merge numerator and denominator.
     */
    boolean isCompatibleWith(SingleUnitImpl other) {
        return (compareTo(other) == 0);
    }

    public String getSimpleUnit() {
        return simpleUnit;
    }

    public void setSimpleUnit(int simpleUnitIndex, String[] simpleUnits) {
        this.index = simpleUnitIndex;
        this.simpleUnit = simpleUnits[simpleUnitIndex];
    }

    public int getDimensionality() {
        return dimensionality;
    }

    public void setDimensionality(int dimensionality) {
        this.dimensionality = dimensionality;
    }

    public MeasureUnit.SIPrefix getSiPrefix() {
        return siPrefix;
    }

    public void setSiPrefix(MeasureUnit.SIPrefix siPrefix) {
        this.siPrefix = siPrefix;
    }

    public int getIndex() {
        return index;
    }

}
