package com.ibm.icu.impl.units;

public enum UMeasureUnitComplexity {
    /**
     * A single unit, like kilojoule.
     *
     * @draft ICU 67
     */
    UMEASURE_UNIT_SINGLE,

    /**
     * A compound unit, like meter-per-second.
     *
     * @draft ICU 67
     */
    UMEASURE_UNIT_COMPOUND,

    /**
     * A mixed unit, like hour+minute.
     *
     * @draft ICU 67
     */
    UMEASURE_UNIT_MIXED
}
