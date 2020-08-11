/*
 *******************************************************************************
 * Copyright (C) 2004-2020, Google Inc, International Business Machines
 * Corporation and others. All Rights Reserved.
 *******************************************************************************
 */

package com.ibm.icu.impl.units;


import com.sun.deploy.si.SingleInstanceManager;

import java.util.ArrayList;

public class MeasureUnitImpl {

    public MeasureUnitImpl() {
        singleUnits = new ArrayList<SingleUnitImpl>();
    }

    /**
     * Returns the list of simple units.
     */
    public ArrayList<SingleUnitImpl> getSingleUnits() {
        return singleUnits;
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

        this.singleUnits.add(singleUnit);
        return true;

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
     * The full unit identifier.  Owned by the MeasureUnitImpl.  Empty if not computed.
     */
    private String identifier;


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
