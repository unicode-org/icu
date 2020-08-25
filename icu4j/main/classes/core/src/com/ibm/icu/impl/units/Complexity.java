package com.ibm.icu.impl.units;

/**
 * Enumeration for unit complexity. There are three levels:
 * <p>
 * - SINGLE: A single unit, optionally with a power and/or SI prefix. Examples: hectare,
 * square-kilometer, kilojoule, one-per-second.
 * - COMPOUND: A unit composed of the product of multiple single units. Examples:
 * meter-per-second, kilowatt-hour, kilogram-meter-per-square-second.
 * - SEQUENCE: A unit composed of the sum of multiple single units. Examples: foot+inch,
 * hour+minute+second, degree+arcminute+arcsecond.
 * <p>
 * The complexity determines which operations are available. For example, you cannot set the power
 * or SI prefix of a compound unit.
 *
 * @draft ICU 68
 */
public enum Complexity {
    /**
     * A single unit, like kilojoule.
     *
     * @draft ICU 68
     */
    SINGLE,

    /**
     * A compound unit, like meter-per-second.
     *
     * @draft ICU 68
     */
    COMPOUND,

    /**
     * A mixed unit, like hour+minute.
     *
     * @draft ICU 68
     */
    MIXED
}
