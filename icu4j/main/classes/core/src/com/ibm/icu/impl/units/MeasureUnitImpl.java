/*
 *******************************************************************************
 * Copyright (C) 2004-2020, Google Inc, International Business Machines
 * Corporation and others. All Rights Reserved.
 *******************************************************************************
 */

package com.ibm.icu.impl.units;

import com.ibm.icu.util.MeasureUnit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MeasureUnitImpl {

    /**
     * The full unit identifier. Owned by the MeasureUnitImpl.  Null if not computed.
     */
    private String identifier = null;
    /**
     * The complexity, either SINGLE, COMPOUND, or MIXED.
     */
    private MeasureUnit.Complexity complexity = MeasureUnit.Complexity.SINGLE;
    /**
     * the list of simple units.These may be summed or multiplied, based on the
     * value of the complexity field.
     * <p>
     * The "dimensionless" unit (SingleUnitImpl default constructor) must not be
     * added to this list.
     * <p>
     * The "dimensionless" <code>MeasureUnitImpl</code> has an empty <code>singleUnits</code>.
     */
    private ArrayList<SingleUnitImpl> singleUnits;

    public MeasureUnitImpl() {
        singleUnits = new ArrayList<>();
    }

    public MeasureUnitImpl(SingleUnitImpl singleUnit) {
        this();
        this.appendSingleUnit(singleUnit);
    }

    /**
     * Parse a unit identifier into a MeasureUnitImpl.
     *
     * @param identifier The unit identifier string.
     * @return A newly parsed value object.
     * @throws <code>InternalError</code> in case of incorrect/non-parsed identifier.
     */
    public static MeasureUnitImpl forIdentifier(String identifier) {
        return UnitsParser.parseForIdentifier(identifier);
    }

    /**
     * Used for currency units.
     */
    public static MeasureUnitImpl forCurrencyCode(String currencyCode) {
        MeasureUnitImpl result = new MeasureUnitImpl();
        result.identifier = currencyCode;
        return result;
    }

    public MeasureUnitImpl clone() {
        MeasureUnitImpl result = new MeasureUnitImpl();
        result.complexity = this.complexity;
        result.identifier = this.identifier;
        result.singleUnits = (ArrayList<SingleUnitImpl>) this.singleUnits.clone();
        return result;
    }

    /**
     * Returns the list of simple units.
     */
    public ArrayList<SingleUnitImpl> getSingleUnits() {
        return singleUnits;
    }

    /**
     * Mutates this MeasureUnitImpl to take the reciprocal.
     */
    public void takeReciprocal() {
        this.identifier = null;
        for (SingleUnitImpl singleUnit :
                this.singleUnits) {
            singleUnit.setDimensionality(singleUnit.getDimensionality() * -1);
        }
    }

    /**
     * Extracts the list of all the individual units inside the `MeasureUnitImpl`.
     * For example:
     * -   if the <code>MeasureUnitImpl</code> is <code>foot-per-hour</code>
     * it will return a list of 1 <code>{foot-per-hour}</code>
     * -   if the <code>MeasureUnitImpl</code> is <code>foot-and-inch</code>
     * it will return a list of 2 <code>{ foot, inch}</code>
     *
     * @return a list of <code>MeasureUnitImpl</code>
     */
    public ArrayList<MeasureUnitImpl> extractIndividualUnits() {
        ArrayList<MeasureUnitImpl> result = new ArrayList<MeasureUnitImpl>();
        if (this.getComplexity() == MeasureUnit.Complexity.MIXED) {
            // In case of mixed units, each single unit can be considered as a stand alone MeasureUnitImpl.
            for (SingleUnitImpl singleUnit :
                    this.getSingleUnits()) {
                result.add(new MeasureUnitImpl(singleUnit));
            }

            return result;
        }

        result.add(this.clone());
        return result;
    }

    /**
     * Applies dimensionality to all the internal single units.
     * For example: <b>square-meter-per-second</b>, when we apply dimensionality -2, it will be <b>square-second-per-p4-meter</b>
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
        identifier = null;

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

        // TODO: shall we just add singleUnit instead of creating a copy ??
        this.singleUnits.add(singleUnit.clone());

        // If the MeasureUnitImpl is `UMEASURE_UNIT_SINGLE` and after the appending a unit, the singleUnits are more
        // than one singleUnit. thus means the complexity should be `UMEASURE_UNIT_COMPOUND`
        if (this.singleUnits.size() > 1 && this.complexity == MeasureUnit.Complexity.SINGLE) {
            this.setComplexity(MeasureUnit.Complexity.COMPOUND);
        }

        return true;
    }

    /**
     * Transform this MeasureUnitImpl into a MeasureUnit, simplifying if possible.
     */
    public MeasureUnit build() {
        return new MeasureUnit(this);
    }

    /**
     * @return SingleUnitImpl
     * @throws UnsupportedOperationException if the object could not be converted to SingleUnitImpl.
     */
    public SingleUnitImpl getSingleUnitImpl() {
        if (this.singleUnits.size() == 0) {
            return new SingleUnitImpl();
        }
        if (this.singleUnits.size() == 1) {
            return this.singleUnits.get(0).clone();
        }

        throw new UnsupportedOperationException();
    }

    public String getIdentifier() {
        if (this.identifier != null) {
            return this.identifier;
        }

        this.serialize();
        return identifier;
    }

    public MeasureUnit.Complexity getComplexity() {
        return complexity;
    }

    public void setComplexity(MeasureUnit.Complexity complexity) {
        this.complexity = complexity;
    }

    /**
     * Normalizes the MeasureUnitImpl and generates the identifier string in place.
     */
    private void serialize() {
        if (this.getSingleUnits().size() == 0) {
            // Dimensionless, constructed by the default constructor: no appending
            // to this.result, we wish it to contain the zero-length string.
            return;
        }
        if (this.complexity == MeasureUnit.Complexity.COMPOUND) {
            // Note: don't sort a MIXED unit
            Collections.sort(this.getSingleUnits(), new SingleUnitComparator());
        }

        StringBuilder result = new StringBuilder();
        boolean beforePre = true;
        boolean firstTimeNegativeDimension = false;
        for (SingleUnitImpl singleUnit :
                this.getSingleUnits()) {
            if (beforePre && singleUnit.getDimensionality() < 0) {
                beforePre = false;
                firstTimeNegativeDimension = true;
            } else if (singleUnit.getDimensionality() < 0) {
                firstTimeNegativeDimension = false;
            }

            String singleUnitIdentifier = singleUnit.getNeutralIdentifier();
            if (this.getComplexity() == MeasureUnit.Complexity.MIXED) {
                if (result.length() != 0) {
                    result.append("-and-");
                }
            } else {
                if (firstTimeNegativeDimension) {
                    if (result.length() == 0) {
                        result.append("per-");
                    } else {
                        result.append("-per-");
                    }
                } else {
                    if (result.length() != 0) {
                        result.append("-");
                    }
                }
            }

            result.append(singleUnitIdentifier);
        }

        this.identifier = result.toString();
    }

    class SingleUnitComparator implements Comparator<SingleUnitImpl> {
        @Override
        public int compare(SingleUnitImpl o1, SingleUnitImpl o2) {
            return o1.compareTo(o2);
        }
    }
}
