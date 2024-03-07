// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.impl.units;

import com.ibm.icu.util.MeasureUnit;

// TODO: revisit documentation in this file. E.g. we don't do dimensionless
// units in Java? We use null instead.

/**
 * A class representing a single unit (optional SI or binary prefix, and dimensionality).
 */
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
    private String simpleUnitID = "";
    /**
     * Determine the power of the {@code SingleUnit}. For example, for "square-meter", the dimensionality will be {@code 2}.
     * <p>
     * NOTE:
     * Default dimensionality is 1.
     */
    private int dimensionality = 1;
    /**
     * SI or binary prefix.
     */
    private MeasureUnit.MeasurePrefix unitPrefix = MeasureUnit.MeasurePrefix.ONE;

    public SingleUnitImpl copy() {
        SingleUnitImpl result = new SingleUnitImpl();
        result.index = this.index;
        result.dimensionality = this.dimensionality;
        result.simpleUnitID = this.simpleUnitID;
        result.unitPrefix = this.unitPrefix;

        return result;
    }

    public MeasureUnit build() {
        MeasureUnitImpl measureUnit = new MeasureUnitImpl(this);
        return measureUnit.build();
    }

    /**
     * Generates a neutral identifier string for a single unit which means we do not include the dimension signal.
     */
    public String getNeutralIdentifier() {
        StringBuilder result = new StringBuilder();
        int absPower = Math.abs(this.getDimensionality());

        assert absPower > 0 : "this function does not support the dimensionless single units";

        if (absPower == 1) {
            // no-op
        } else if (absPower == 2) {
            result.append("square-");
        } else if (absPower == 3) {
            result.append("cubic-");
        } else if (absPower <= 15) {
            result.append("pow");
            result.append(absPower);
            result.append('-');
        } else {
            throw new IllegalArgumentException("Unit Identifier Syntax Error");
        }

        result.append(this.getPrefix().getIdentifier());
        result.append(this.getSimpleUnitID());

        return result.toString();
    }

    /**
     * Compare this SingleUnitImpl to another SingleUnitImpl for the sake of
     * sorting and coalescing.
     * <p>
     * Sort order of units is specified by UTS #35
     * (https://unicode.org/reports/tr35/tr35-info.html#Unit_Identifier_Normalization).
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
        // Sort by official quantity order
        int thisCategoryIndex = UnitsData.getCategoryIndexOfSimpleUnit(index);
        int otherCategoryIndex = UnitsData.getCategoryIndexOfSimpleUnit(other.index);
        if (thisCategoryIndex < otherCategoryIndex) {
            return -1;
        }
        if (thisCategoryIndex > otherCategoryIndex) {
            return 1;
        }
        // If quantity order didn't help, then we go by index.
        if (index < other.index) {
            return -1;
        }
        if (index > other.index) {
            return 1;
        }

        // When comparing binary prefixes vs SI prefixes, instead of comparing the actual values, we can
        // multiply the binary prefix power by 3 and compare the powers. if they are equal, we can can
        // compare the bases.
        // NOTE: this methodology will fail if the binary prefix more than or equal 98.
        int unitBase = this.unitPrefix.getBase();
        int otherUnitBase = other.unitPrefix.getBase();
        // Values for comparison purposes only.
        int unitPowerComp =
                unitBase == 1024 /* Binary Prefix */ ? this.unitPrefix.getPower() * 3
                        : this.unitPrefix.getPower();
        int otherUnitPowerComp =
                otherUnitBase == 1024 /* Binary Prefix */ ? other.unitPrefix.getPower() * 3
                        : other.unitPrefix.getPower();

        if (unitPowerComp < otherUnitPowerComp) {
            return 1;
        }
        if (unitPowerComp > otherUnitPowerComp) {
            return -1;
        }

        if (unitBase < otherUnitBase) {
            return 1;
        }
        if (unitBase > otherUnitBase) {
            return -1;
        }

        return 0;
    }

    /**
     * Checks whether this SingleUnitImpl is compatible with another for the purpose of coalescing.
     * <p>
     * Units with the same base unit and SI or binary prefix should match, except that they must also
     * have the same dimensionality sign, such that we don't merge numerator and denominator.
     */
    boolean isCompatibleWith(SingleUnitImpl other) {
        return (compareTo(other) == 0);
    }

    public String getSimpleUnitID() {
        return simpleUnitID;
    }

    public void setSimpleUnit(int simpleUnitIndex, String[] simpleUnits) {
        this.index = simpleUnitIndex;
        this.simpleUnitID = simpleUnits[simpleUnitIndex];
    }

    public int getDimensionality() {
        return dimensionality;
    }

    public void setDimensionality(int dimensionality) {
        this.dimensionality = dimensionality;
    }

    public MeasureUnit.MeasurePrefix getPrefix() {
        return unitPrefix;
    }

    public void setPrefix(MeasureUnit.MeasurePrefix unitPrefix) {
        this.unitPrefix = unitPrefix;
    }

    // TODO: unused? Delete?
    public int getIndex() {
        return index;
    }

}
