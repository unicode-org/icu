package com.ibm.icu.impl.units;

import com.ibm.icu.impl.Assert;

public enum UMeasureSIPrefix {

    /**
     * SI prefix: yotta, 10^24.
     *
     * @draft ICU 67
     */
    UMEASURE_SI_PREFIX_YOTTA(24),

    /**
     * SI prefix: zetta, 10^21.
     *
     * @draft ICU 67
     */
    UMEASURE_SI_PREFIX_ZETTA(21),

    /**
     * SI prefix: exa, 10^18.
     *
     * @draft ICU 67
     */
    UMEASURE_SI_PREFIX_EXA(18),

    /**
     * SI prefix: peta, 10^15.
     *
     * @draft ICU 67
     */
    UMEASURE_SI_PREFIX_PETA(15),

    /**
     * SI prefix: tera, 10^12.
     *
     * @draft ICU 67
     */
    UMEASURE_SI_PREFIX_TERA(12),

    /**
     * SI prefix: giga, 10^9.
     *
     * @draft ICU 67
     */
    UMEASURE_SI_PREFIX_GIGA(9),

    /**
     * SI prefix: mega, 10^6.
     *
     * @draft ICU 67
     */
    UMEASURE_SI_PREFIX_MEGA(6),

    /**
     * SI prefix: kilo, 10^3.
     *
     * @draft ICU 67
     */
    UMEASURE_SI_PREFIX_KILO(3),

    /**
     * SI prefix: hecto, 10^2.
     *
     * @draft ICU 67
     */
    UMEASURE_SI_PREFIX_HECTO(2),

    /**
     * SI prefix: deka, 10^1.
     *
     * @draft ICU 67
     */
    UMEASURE_SI_PREFIX_DEKA(1),

    /**
     * The absence of an SI prefix.
     *
     * @draft ICU 67
     */
    UMEASURE_SI_PREFIX_ONE(0),

    /**
     * SI prefix: deci, 10^-1.
     *
     * @draft ICU 67
     */
    UMEASURE_SI_PREFIX_DECI(-1),

    /**
     * SI prefix: centi, 10^-2.
     *
     * @draft ICU 67
     */
    UMEASURE_SI_PREFIX_CENTI(-2),

    /**
     * SI prefix: milli, 10^-3.
     *
     * @draft ICU 67
     */
    UMEASURE_SI_PREFIX_MILLI(-3),

    /**
     * SI prefix: micro, 10^-6.
     *
     * @draft ICU 67
     */
    UMEASURE_SI_PREFIX_MICRO(-6),

    /**
     * SI prefix: nano, 10^-9.
     *
     * @draft ICU 67
     */
    UMEASURE_SI_PREFIX_NANO(-9),

    /**
     * SI prefix: pico, 10^-12.
     *
     * @draft ICU 67
     */
    UMEASURE_SI_PREFIX_PICO(-12),

    /**
     * SI prefix: femto, 10^-15.
     *
     * @draft ICU 67
     */
    UMEASURE_SI_PREFIX_FEMTO(-15),

    /**
     * SI prefix: atto, 10^-18.
     *
     * @draft ICU 67
     */
    UMEASURE_SI_PREFIX_ATTO(-18),

    /**
     * SI prefix: zepto, 10^-21.
     *
     * @draft ICU 67
     */
    UMEASURE_SI_PREFIX_ZEPTO(-21),

    /**
     * SI prefix: yocto, 10^-24.
     *
     * @draft ICU 67
     */
    UMEASURE_SI_PREFIX_YOCTO(-24);

    private final int siPrefix;

    UMeasureSIPrefix(int siPrefix) {
        this.siPrefix = siPrefix;
    }

    public int getSiPrefix() {
        return siPrefix;
    }

    public int getTrieIndex() {
        return  siPrefix + Constants.kSIPrefixOffset;
    }

    public static UMeasureSIPrefix getSiPrefixFromTrieIndex(int trieIndex) {
        for (UMeasureSIPrefix element :
                values()) {
            if (element.getTrieIndex() == trieIndex)
                return element;
        }

        throw new java.lang.InternalError("Incorrect trieIndex");
    }

}