/*
 *******************************************************************************
 * Copyright (C) 2004-2020, Google Inc, International Business Machines
 * Corporation and others. All Rights Reserved.
 *******************************************************************************
 */

package com.ibm.icu.impl.units;


import com.ibm.icu.util.MeasureUnit;
import com.sun.deploy.si.SingleInstanceManager;

import java.util.ArrayList;

public class MeasureUnitImpl {

    public MeasureUnitImpl() {
        singleUnits = new ArrayList<>();
    }

    public MeasureUnitImpl(MeasureUnitImpl other) {
        this.complexity = other.complexity;
        this.identifier = other.identifier;

        this.singleUnits = new ArrayList<>();
        for (SingleUnitImpl singleUnit :
                other.singleUnits) {
            this.appendSingleUnit(singleUnit);
        }

    }

    public MeasureUnitImpl(SingleUnitImpl singleUnit) {
        this.appendSingleUnit(singleUnit);
    }

    public static MeasureUnitImpl forMeasureUnitMaybeCopy(MeasureUnit inputUnit) {
        // TODO: implement
        return null;
    }

    /**
     * Returns the list of simple units.
     */
    public ArrayList<SingleUnitImpl> getSingleUnits() {
        return singleUnits;
    }

    public ArrayList<MeasureUnitImpl> getMeasureUnits() {
        ArrayList<MeasureUnitImpl> result = new ArrayList<MeasureUnitImpl>();
        if (this.getComplexity() == UMeasureUnitComplexity.UMEASURE_UNIT_MIXED) {
            for (SingleUnitImpl singleUnit :
                    this.getSingleUnits()) {
                result.add(new MeasureUnitImpl(singleUnit));
            }

            return result;
        }

        result.add(new MeasureUnitImpl(this));
        return result;
    }


    /**
     * Applies dimensionality to all the internal single units.
     * For example: `square-meter-per-second`, when we apply dimensionality -2, it will be `square-second-per-p4-meter`
     */
    public void applyDimensionality(int dimensionality) {
        for (SingleUnitImpl singleUnit :
                singleUnits) {
            singleUnit.setDimensionality(singleUnit.getDimensionality() * dimensionality);
        }
    }

    /**
     * Mutates this MeasureUnitImpl to append a single unit.
     *
     * @return true if a new item was added. If unit is the dimensionless unit,
     * it is never added: the return value will always be false.
     */
    public boolean appendSingleUnit(SingleUnitImpl singleUnit) {
        identifier = "";

        if (singleUnit.isDimensionless()) {
            // We don't append dimensionless units.
            return false;
        }

        // Find a similar unit that already exists, to attempt to coalesce
        SingleUnitImpl oldUnit = null;
        for (int i = 0, n = this.singleUnits.size(); i < n; i++) {
            SingleUnitImpl candidate = this.singleUnits.get(i);
            if (candidate.isCompatibleWith(singleUnit)) {
                oldUnit = candidate;
                break;
            }
        }

        if (oldUnit != null) {
            // Both dimensionalities will be positive, or both will be negative, by
            // virtue of isCompatibleWith().
            oldUnit.setDimensionality(oldUnit.getDimensionality() + singleUnit.getDimensionality());
            return false;
        }

        this.singleUnits.add(new SingleUnitImpl(singleUnit));

        // If the MeasureUnitImpl is `UMEASURE_UNIT_SINGLE` and after the appending the units, the singleUnits are more
        // than one singleUnit. thus means the complexity should be `UMEASURE_UNIT_SINGLE`
        if (this.singleUnits.size() > 1 && this.complexity == UMeasureUnitComplexity.UMEASURE_UNIT_SINGLE) {
            this.setComplexity(UMeasureUnitComplexity.UMEASURE_UNIT_COMPOUND);
        }

        return true;
    }

    /**
     * Extracts a `MeasureUnitImpl` from this MeasureUnitImpl , simplifying if possible.
     */
    public MeasureUnit build() {
        // TODO: implement
        return null;
    }

    public String getIdentifier() {
        // TODO: implement
        return identifier;
    }

    public UMeasureUnitComplexity getComplexity() {
        return complexity;
    }

    public void setComplexity(UMeasureUnitComplexity complexity) {
        this.complexity = complexity;
    }

    /**
     * The full unit identifier.  Owned by the MeasureUnitImpl.  Null if not computed.
     */
    private String identifier = null;


    /**
     * The complexity, either SINGLE, COMPOUND, or MIXED.
     */
    private UMeasureUnitComplexity complexity = UMeasureUnitComplexity.UMEASURE_UNIT_SINGLE;


    /**
     * the list of simple units.These may be summed or multiplied, based on the
     * value of the complexity field.
     * <p>
     * The "dimensionless" unit (SingleUnitImpl default constructor) must not be
     * added to this list.
     * <p>
     * The "dimensionless" `MeasureUnitImpl` has an empty `singleUnits`.
     */
    private ArrayList<SingleUnitImpl> singleUnits;

}
